/**
 * 
 */
package com.vzw.util.config;

import com.vzw.util.LogUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 * @author hud
 * 
 * This class implements automatically reloading
 * resources
 * 
 * No configuration is needed. 
 * 
 * Resources
 * 
 * Necessary properties need to be set in
 * the watched resource
 * 
 * == Properties file:
 * 	hot_reload.check_interval = xx
 * 		unit: seconds
 * 
 * 	hot_reload.force_interval = xx
 * 		unit: seconds
 * 		force the file to be reloaded.
 * 		if <= 0, ignored
 * 
 * == URL provided by properties file:
 * 	<url_property_key>.hot_reload.check_interval = xx	
 * 	<url_property_key>.hot_reload.force_interval = xx 
 * 
 */

abstract public class ResourceWatcher {
	
	public static final String			KEY_HOTRELOAD_CHECKINTERVAL	= "hot_reload.check_interval";
	public static final String			KEY_HOTRELOAD_FORCEINTERVAL	= "hot_reload.force_interval";
	
	// in seconds
	public static final int				DEFAULT_CHECKINTERVAL		= 30;
	public static final int				DEFAULT_FORCEINTERVAL		= 0;
	
	
	private static final Logger			logger = Logger.getLogger(ResourceWatcher.class);
	
	// by default, any of "-1" means no watcher 
	// if 0, use default value
	//private	int		checkInterval;	//0 -> use default
	//private int		forceInterval; 	//0 -> do not force reloading
	
	/**
	 * @hud added to clean up timers
	 * 
	 * A list of resource watcher objects is held here
	 * Upon application exit, a destroy method must be called to release all
	 * the resources
	 */
	protected final static List<ResourceWatcher>		resourceWatcherList = new ArrayList<ResourceWatcher>();
	
	private ScheduledExecutorService	checkService;
	private ScheduledExecutorService	forceService;
	
	
	static {
		Runtime r = Runtime.getRuntime();
		r.addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				destroyAll();
			}
			
		}));
	}
	
	
	
	/**
	 * timer task
	 */
	private class WatcherRunnable implements Runnable {
		
		private boolean forceRefresh = false;
		//@Override
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {

				if (forceRefresh) {
					synchronized (this) {
						reload();
					}					
				}
				else {
					synchronized (this) {
						if (requireRefresh()) {
							reload();
						}
					}
				}
			}
			catch (Exception e) {
				LogUtil.error(logger, e, "Failed to execute WatcherRunnable, forceRefresh = {0}", forceRefresh);
				//e.printStackTrace();
			}
			
		}
		
		WatcherRunnable(boolean forceRefresh) {
			this.forceRefresh = forceRefresh;
		}
		
	}
	
	
	protected ResourceWatcher() {
		
	}
	
	
	/**
	 * destroy all the resource watchers
	 * use with caution, should only get called 
	 * during application exit
	 */
	public static void destroyAll() {
		for (ResourceWatcher rw : resourceWatcherList) {
			rw.destroy();
		}
	}
	/**
	 * give external process a chance to destroy it
	 */
	public void destroy() {
		if (checkService != null) {
			checkService.shutdownNow();
            checkService = null;
		}
		
		if (forceService != null) {
			forceService.shutdownNow();
            forceService = null;
		}
	}
	
	
	/**
	 * initialize watcher
	 */
	final public void initWatcher() {
		// check whether watcher is needed on this property
		try {
			
			// get parameters
			int checkInterval = getCheckInterval();
			int forceInterval = getForceInterval();
			
			if (checkInterval == 0) {
				checkInterval = DEFAULT_CHECKINTERVAL;
			}
			if (checkInterval > 0) {
				checkService = Executors.newSingleThreadScheduledExecutor();
				checkService.scheduleAtFixedRate(new WatcherRunnable(false), getCheckDelay(), checkInterval, TimeUnit.SECONDS);
				//checkTimer = createTimer(getCheckDelay(), checkInterval, false);
			}
			
			if (forceInterval == 0) {
				// do not create timer for 0 force interval
				forceInterval = DEFAULT_FORCEINTERVAL;
			}
			if (forceInterval > 0)	{
				forceService = Executors.newSingleThreadScheduledExecutor();
				forceService.scheduleAtFixedRate(new WatcherRunnable(true), getForceDelay(), forceInterval, TimeUnit.SECONDS);
				//forceTimer = createTimer(getForceDelay(), forceInterval, true);
			}
			
			
			
		}
		catch (Exception e) {
			LogUtil.error(logger, e, "Failed to initialize resource watcher");
			//e.printStackTrace();
		}
		
		// add to list
		synchronized (resourceWatcherList) {
			resourceWatcherList.add(this);
		}
	}
	
	
	
	/**
	 * abstract methods
	 * @return
	 */
	abstract protected int getCheckInterval();
	abstract protected int getCheckDelay();
	abstract protected int getForceInterval();
	abstract protected int getForceDelay();
	
	/**
	 * The deriving class must implement this to 
	 * determine whether a refresh is needed
	 * 
	 * @return
	 */
	abstract protected boolean requireRefresh();

	/**
	 * reload the data
	 */
	abstract protected void reload() throws Exception;	


}
