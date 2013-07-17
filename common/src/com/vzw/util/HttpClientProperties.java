package com.vzw.util;





import org.apache.log4j.Logger;

import com.vzw.util.config.AbstractProperties;

public class HttpClientProperties extends AbstractProperties {
	private static final Logger logger = Logger.getLogger(HttpClientProperties.class);
	
	private static HttpClientProperties		instance = null;
	
	static {
		try {
			instance = new HttpClientProperties();
		}
		catch (Exception e) {
			logger.error("Failed to load SchedulerProperties", e);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static HttpClientProperties getInstance() {
		return instance;
	}

	private HttpClientProperties() throws Exception {
		super("httpcomponent.properties");
		// TODO Auto-generated constructor stub
	}



	@Override
	protected void refresh(boolean bFirstLoad) throws Exception {
		// TODO Auto-generated method stub

	}

}
