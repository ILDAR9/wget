package com.java_core.wget;

import java.net.MalformedURLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainActivity {
	private static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

	public static void main(String[] args) {
		if (args.length > 0) {
			String urlString = args[0];
			try {
				Wget wget = new Wget(urlString);
				wget.downloadPage();
			} catch (MalformedURLException ex) {
				logger.info("{} is not valid url address", urlString);
				logger.error("MalformedURLException exception caught while attepting to create instance of WGET utility",ex);
			} catch(NullPointerException ex){
				logger.error("NullPointerException exception caught while attepting to create instance of WGET utility",ex);
			}
		} else {
			logger.info("usage: wget [url]");
		}
		
	}
}
