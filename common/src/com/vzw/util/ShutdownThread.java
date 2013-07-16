package com.vzw.util;

import org.apache.log4j.Logger;

public class ShutdownThread extends Thread {
	private IServerHook server;
	private Logger logger;

	public ShutdownThread(IServerHook srvr, Logger logger) {
		this.server = srvr;
		this.logger = logger;
	}

	@Override
	public void run() {
		logger.warn("Shutting down server");

	    if (server != null) {
	    	try {
	    		server.shutdown();
	    		server = null;
	    	} catch (Exception e) {
	    		logger.warn(e);
	    	}
	    }

	    logger.info("Shutdown complete");

	}
	
	
	public static void addShutdownHook(IServerHook sh, Logger logger) {
		Runtime.getRuntime().addShutdownHook(new ShutdownThread(sh, logger));
	}
}
