package com.vzw.hackthon.scheduler;





import org.apache.log4j.Logger;

import com.vzw.util.config.AbstractProperties;

public class SchedulerProperties extends AbstractProperties {
	private static final Logger logger = Logger.getLogger(SchedulerProperties.class);
	
	private static SchedulerProperties		instance = null;
	
	static {
		try {
			instance = new SchedulerProperties();
		}
		catch (Exception e) {
			logger.error("Failed to load SchedulerProperties", e);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static SchedulerProperties getInstance() {
		return instance;
	}

	private SchedulerProperties() throws Exception {
		super("scheduler.properties");
		// TODO Auto-generated constructor stub
	}



	@Override
	protected void refresh(boolean bFirstLoad) throws Exception {
		// TODO Auto-generated method stub

	}

}
