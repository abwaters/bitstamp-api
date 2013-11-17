package com.abwaters.bitstamp.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.junit.Before;

import com.abwaters.bitstamp.Bitstamp;

public class Bitstamp_Test {

	private Bitstamp bitstamp ;
	
	private static Properties load(File pfile) throws Exception {
		FileInputStream pfs = new FileInputStream(pfile.getAbsoluteFile()) ;
		Properties properties = new Properties() ;
		properties.load(pfs) ;
		return properties ;
	}
	
	@Before
	public void setUp() throws Exception {
		// Note: Keys below do not have trade or withdraw permissions...only info
		String userdir = System.getProperty("user.dir") ;
		Properties p = load(new File(userdir,"config.properties")) ;
		String key = p.getProperty("bitstamp.key") ;
		String secret = p.getProperty("bitstamp.secret") ;
		String clientid = p.getProperty("bitstamp.clientid") ;
		int request_limit = Integer.parseInt(p.getProperty("bitstamp.request_limit")) ;
		int auth_request_limit = Integer.parseInt(p.getProperty("bitstamp.auth_request_limit")) ;
		bitstamp = new Bitstamp() ;
		bitstamp.setAuthKeys(key, secret,clientid) ;
		bitstamp.setAuthRequestLimit(auth_request_limit) ;
		bitstamp.setRequestLimit(request_limit) ;
	}

}
