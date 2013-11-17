package com.abwaters.bitstamp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Bitstamp {
	private static final String USER_AGENT = "Mozilla/5.0 (compatible; BTCE-API/1.0; MSIE 6.0 compatible; +https://github.com/abwaters/bitstamp-api)" ;
	private static final String API_URL = "https://www.bitstamp.net/api/" ;

	private static long auth_last_request = 0 ;
	private static long auth_request_limit = 1000 ;	// request limit in milliseconds
	private static long last_request = 0 ;
	private static long request_limit = 1000 ;	// request limit in milliseconds for non-auth calls...defaults to 1 seconds
	private static long nonce = 0, last_nonce = 0 ;
	
	private boolean initialized = false;
	private String secret, key, clientid ;
	private Mac mac ;
	private Gson gson ;

	/**
	 * Constructor
	 */
	public Bitstamp() {
		GsonBuilder gson_builder = new GsonBuilder();
		gson = gson_builder.create() ;
		if( nonce == 0 ) nonce = System.currentTimeMillis()/1000 ; 
	}
	
	/*
	 * Get the ticker.
	 */
	public String getTicker() throws BitstampException {
		return request("ticker",null) ;
	}
	
	/*
	 * Gets the order book.
	 * 
	 * @param group group orders with the same price.
	 * 
	 */
	public String getOrderBook(boolean group) throws BitstampException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("group",group?"1":"0") ;
		return request("order_book",args) ;
	}
	
	/**
	 * Time Frame helper class.
	 */
	public static final class TimeFrame {
		public static final String MINUTE = "minute" ;
		public static final String HOUR = "hour" ;
	}
	
	/*
	 * Gets the transactions according to the specified time frame.
	 * 
	 * @param time
	 */
	public String getTransactions(String time) throws BitstampException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("time",time) ;
		return request("transactions",args) ;
	}
	
	/*
	 * Get the EUR/USD conversion rate for BitStamp.
	 */
	public String getConversionRate() throws BitstampException {
		return request("eur_usd",null) ;
	}

	// authorized requests
	
	/*
	 * Gets the balances for your account.
	 * 
	 */
	public String getBalance() throws BitstampException {
		return authrequest("balance",null) ;
	}
	
	public String getUserTransactions(int offset,int limit,boolean sort_asc) throws BitstampException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("offset",Integer.toString(offset)) ;
		args.put("limit",Integer.toString(limit)) ;
		args.put("sort",sort_asc?"asc":"desc") ;
		return authrequest("user_transctions",null) ;
	}
	
	public String getOpenOrders() throws BitstampException {
		return authrequest("open_orders",null) ;
	}
	
	public String cancelOrder(int orderid) throws BitstampException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("id",Integer.toString(orderid)) ;
		return authrequest("cancel_order",args) ;
	}
	
	public String buyLimitOrder(double amount,double price) throws BitstampException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("amount",Double.toString(amount)) ;
		args.put("price",Double.toString(price)) ;
		return authrequest("buy",args) ;
	}
	
	public String sellLimitOrder(double amount,double price) throws BitstampException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("amount",Double.toString(amount)) ;
		args.put("price",Double.toString(price)) ;
		return authrequest("sell",args) ;
	}
	
	public String checkCode(String code) throws BitstampException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("code",code) ;
		return authrequest("check_code",args) ;
	}
	
	public String redeemCode(String code) throws BitstampException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("code",code) ;
		return authrequest("redeem_code",args) ;
	}
	
	public String getWithdrawalRequests() throws BitstampException {
		return authrequest("withdrawal_requests",null) ;
	}
	
	public String withdrawBitcoin(double amount,String address) throws BitstampException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("amount",Double.toString(amount)) ;
		args.put("address",address) ;
		return authrequest("bitcoin_withdrawal",args) ;
	}
	
	public String getBitcoinDeposits() throws BitstampException {
		return authrequest("unconfirmed_btc",null) ;
	}
	
	public String withdrawRipple(double amount,String address,String currency) throws BitstampException {
		Map<String,String> args = new HashMap<String,String>() ;
		args.put("amount",Double.toString(amount)) ;
		args.put("address",address) ;
		args.put("currency",currency) ;
		return authrequest("ripple_withdrawal",args) ;
	}
	
	public String getRippleDepositAddress() throws BitstampException {
		return authrequest("ripple_address",null) ;
	}
	
	/*
	public String getOpenOrders() throws BitstampException {
		return authrequest("open_orders",null) ;
	}
	
	public String getOpenOrders() throws BitstampException {
		return authrequest("open_orders",null) ;
	}
	
	public String getOpenOrders() throws BitstampException {
		return authrequest("open_orders",null) ;
	}
	*/
	
	/**
	 * Limits how frequently calls to the open API for trade history and tickers can be made.  
	 * If calls are attempted more frequently, the thread making the call is put to sleep for 
	 * the duration of the time left before the limit is reached.
	 *  
	 * @param request_limit call limit in milliseconds
	 */
	public void setRequestLimit(long request_limit) {
		Bitstamp.request_limit = request_limit ; 
	}
	
	/**
	 * Limits how frequently calls to the authenticated Bitstamp can be made.  If calls are attempted 
	 * more frequently, the thread making the call is put to sleep for the duration of the time 
	 * left before the limit is reached.
	 * 
	 * @param auth_request_limit call limit in milliseconds
	 */
	public void setAuthRequestLimit(long auth_request_limit) {
		Bitstamp.auth_request_limit = auth_request_limit ; 
	}
	
	/**
	 * Sets the account API keys to use for calling methods that require access to a Bitstamp account.
	 * 
	 * @param key the key obtained from Profile->API Keys in your BTC-E account.
	 * @param secret the secret obtained from Profile->API Keys in your BTC-E account.
	 * @param clientid the account clientid for your Bitstamp account
	 */
	public void setAuthKeys(String key,String secret,String clientid) throws BitstampException {
		this.key = key ;
		this.secret = secret ;
		this.clientid = clientid ;
		SecretKeySpec keyspec = null ;
		try {
			keyspec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256") ;
		} catch (UnsupportedEncodingException uee) {
			throw new BitstampException("HMAC-SHA256 doesn't seem to be installed",uee) ;
		}

		try {
			mac = Mac.getInstance("HmacSHA256") ;
		} catch (NoSuchAlgorithmException nsae) {
			throw new BitstampException("HMAC-SHA256 doesn't seem to be installed",nsae) ;
		}

		try {
			mac.init(keyspec) ;
		} catch (InvalidKeyException ike) {
			throw new BitstampException("Invalid key for signing request",ike) ;
		}
		
		initialized = true ;
	}

	private final void preCall() {
		while(nonce==last_nonce) nonce++ ;
		long elapsed = System.currentTimeMillis()-last_request ;
		if( elapsed < request_limit ) {
			try {
				Thread.currentThread().sleep(request_limit-elapsed) ;
			} catch (InterruptedException e) {
				
			}
		}
		last_request = System.currentTimeMillis() ;
	}
	
	private final String request(String method,Map<String,String> args) throws BitstampException {
		
		// handle precall logic
		preCall() ;

		// prepare args
		if (args == null) args = new HashMap<String,String>() ;
		String queryString = "" ;
		for (Iterator<String> iter = args.keySet().iterator(); iter.hasNext();) {
			String arg = iter.next() ;
			if (queryString.length() > 0) queryString += "&" ;
			queryString += arg + "=" + URLEncoder.encode(args.get(arg)) ;
		}

		// create connection
		URLConnection conn = null ;
		StringBuffer response = new StringBuffer() ;
		try {
			URL url = new URL(API_URL+method+"/?"+queryString);
			conn = url.openConnection() ;
			conn.setUseCaches(false) ;
			conn.setRequestProperty("User-Agent",USER_AGENT) ;
		
			// read response
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = null ;
			while ((line = in.readLine()) != null)
				response.append(line) ;
			in.close() ;
		} catch (MalformedURLException e) {
			throw new BitstampException("Internal error.",e) ;
		} catch (IOException e) {
			throw new BitstampException("Error connecting to BitStamp.",e) ;
		}
		return response.toString() ;
	}
	
	private final void preAuth() {
		while(nonce==last_nonce) nonce++ ;
		long elapsed = System.currentTimeMillis()-auth_last_request ;
		if( elapsed < auth_request_limit ) {
			try {
				Thread.currentThread().sleep(auth_request_limit-elapsed) ;
			} catch (InterruptedException e) {
				
			}
		}
		auth_last_request = System.currentTimeMillis() ;
	}
	
	private final String authrequest(String method, Map<String,String> args) throws BitstampException {
		if( !initialized ) throw new BitstampException("BitStamp not initialized.") ;
		
		// prep the call
		preAuth() ;

		// add method and nonce to args
		if (args == null) args = new HashMap<String,String>() ;
		args.put("nonce",Long.toString(nonce)) ;
		args.put("key",key) ;
		last_nonce = nonce ;
		
		// create url form encoded post data
		String postData = "" ;
		for (Iterator<String> iter = args.keySet().iterator(); iter.hasNext();) {
			String arg = iter.next() ;
			if (postData.length() > 0) postData += "&" ;
			postData += arg + "=" + URLEncoder.encode(args.get(arg)) ;
		}
		
		// create connection
		URLConnection conn = null ;
		StringBuffer response = new StringBuffer() ;
		try {
			URL url = new URL(API_URL+method+"/");
			conn = url.openConnection() ;
			conn.setUseCaches(false) ;
			conn.setDoOutput(true) ;
			
			mac.update(Long.toString(nonce).getBytes()) ;
		    mac.update(clientid.getBytes()) ;
		    mac.update(key.getBytes()) ;
			postData += "&signature="+String.format("%064x", new BigInteger(1, mac.doFinal())).toUpperCase() ;
			conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded") ;
			conn.setRequestProperty("User-Agent",USER_AGENT) ;
		
			// write post data
			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
			out.write(postData) ;
			out.close() ;
	
			// read response
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = null ;
			while ((line = in.readLine()) != null)
				response.append(line) ;
			in.close() ;
		} catch (MalformedURLException e) {
			throw new BitstampException("Internal error.",e) ;
		} catch (IOException e) {
			throw new BitstampException("Error connecting to BitStamp.",e) ;
		}
		return response.toString() ;
	}
	
	private String toHex(byte[] b) throws UnsupportedEncodingException {
	    return String.format("%040x", new BigInteger(1,b));
	}
	
	/**
	 * An exception class specifically for the BitStamp API.  The goal here is to provide a specific exception class for this API while
	 * not losing any of the details of the inner exceptions.
	 * <p>  
	 * This class is just a wrapper for the Exception class.
	 */
	public class BitstampException extends Exception {
		
		private static final long serialVersionUID = 1L;
		
		public BitstampException(String msg) {
			super(msg) ;
		}
		
		public BitstampException(String msg, Throwable e) {
			super(msg,e) ;
		}
	}
}
