/**
 * 
 */
package com.vzw.util.db;

import com.vzw.util.LogUtil;
import com.vzw.util.config.AbstractProperties;
import com.vzw.util.config.AsyncExecutor;
import com.vzw.util.IServerHook;
import com.vzw.util.ShutdownThread;
import java.lang.reflect.Constructor;
import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;


/**
 * @author hud
 * 
 * This is the database pool wrapper for jakarta dbcp
 * 
 * We only maintain one connection pool per JVM context
 * The dbpool URL is obtained through the static "init" method
 *
 */
public class DBManager implements IServerHook {
	
	private final static Logger logger = Logger.getLogger(DBManager.class);
	
	
	/**
	 * Added to support one local, multiple remote for future
	 * This defines the key for local and remote for convenience
	 * 
	 * Pool names should be defined in vma.properties
	 * and in env.properties
	 * 
	 * By default, there are 2 pools:
	 * LOCAL: dbpool
	 * REMOTE: dbpool_remote
	 * 
	 * See property: 
	 */
	protected static enum DbLocation {
		LOCAL,
		REMOTE
	}
	
	
	/**============================================
	 * constants
	 */
	
	/**
	 * The pooling driver
	 */
	private static final String		POOLINGDRIVER		= "org.apache.commons.dbcp.PoolingDriver";
	
	
	/**
	 *
	 */
	public static final String		BASEPOOLURL			= "jdbc:apache:commons:dbcp:/";
	
	
	public static final String		DBPOOL_DEFAULT;
	
	public static final String		DBPOOL_REMOTE;
	
	
	
	
	
	
	
	/**
	 * singleton
	 * always instantiate during class loading
	 */
	private static final DBManager	instance;


	

	/**
	 * map of the pools
	 */
	private Map<String, DBPool>			poolMap	= new HashMap<String, DBPool>();
	
	
	/**
	 * Multi-value map for local and remote
	 */
	protected MultiValueMap				poolLocationMap = new MultiValueMap();
		
		
		
	

	@Override
	public void shutdown() {
		destroyAll();
	}




	static class MonitorProperties extends AbstractProperties {
		private int monitorPeriodSeconds;
		private int connHeldWarnThresholdSeconds;
		private int connCloseAfterHeldSeconds;
		private boolean enableStackTrace;
		private boolean enabled = false;


		private MonitorProperties() throws Exception {
			super("dbmonitor.properties");
		}

		@Override
		synchronized protected void refresh(boolean bFirstLoad) throws Exception {
			if (bFirstLoad) {
				monitorPeriodSeconds = getInt("dbmonitor.period_seconds", 60);
			}
			else {
				int _monitorPeriodSeconds = getInt("dbmonitor.period_seconds", 60);


				if (_monitorPeriodSeconds != monitorPeriodSeconds) {
					// reschedule the timer
					monitorPeriodSeconds = _monitorPeriodSeconds;
					DBManager.getInstance().rescheduleMonitor(monitorPeriodSeconds);
				}
			}
			connHeldWarnThresholdSeconds = getInt("dbmonitor.conn_held_warn_threshold_seconds", 300);
			connCloseAfterHeldSeconds = getInt("dbmonitor.conn_close_after_warn_count", 5)*connHeldWarnThresholdSeconds+60;
			enableStackTrace = getBoolean("dbmonitor.enable_stack_trace", false);
			enabled = getBoolean("dbmonitor.enabled", true);

		}


		synchronized public int getMonitorPeriodSeconds() {
			return monitorPeriodSeconds;
		}

		synchronized public int getConnHeldWarnThresholdSeconds() {
			return connHeldWarnThresholdSeconds;
		}

		synchronized public int getConnCloseAfterHeldSeconds() {
			return connCloseAfterHeldSeconds;
		}

		synchronized public boolean isEnableStackTrace() {
			return enableStackTrace;
		}

		synchronized public boolean isEnabled() {
			return enabled;
		}

		

	}

	private MonitorProperties monitorProperties = null;
	
	
	
	static {
		// initialilze local and remote db pool name (default)
		//VMAPropertiesFile props = VMAPropertiesFile.getInstance();
		DBPOOL_DEFAULT = "dbpool";//props.getString("db.pool.local");
		//String[] remotePoolNames = props.getStringArray("db.pool.remote");
		DBPOOL_REMOTE = DBPOOL_DEFAULT; //remotePoolNames[0];
		
		DBManager _i = null;
		try {
			
			
			// Use system properties dbManager.class to find out correct
			// DBManager class here
			String dbManagerClassName = System.getProperty("dbManager.class");
			if (dbManagerClassName == null) {
				_i = new DBManager();
			}
			else {
				Class<? extends DBManager> cls = (Class<? extends DBManager>)Thread.currentThread().getContextClassLoader().loadClass(dbManagerClassName);
				Constructor<? extends DBManager> csr = cls.getConstructor();
				_i = csr.newInstance();
			}
			ShutdownThread.addShutdownHook(_i, logger);
			
			
			// initialize the dbpool map
			_i.initPoolMap();

		}
		catch (Exception e) {
			logger.error("Unable to initialize DBManager", e);
		}
		
		instance = _i;
	}
	
	/**
	 * Initialize the pool multi-value map
	 * may be overridden 
	 */
	protected void initPoolMap() {
		// local
		poolLocationMap.put(DbLocation.LOCAL, _getDBPool(DBPOOL_DEFAULT));
		
		//String[] remotePoolNames = props.getStringArray("db.pool.remote");
		//for (String name : remotePoolNames) {
		//	poolLocationMap.put(DbLocation.REMOTE, _getDBPool(name));
		//}
	}
	
	/**
	 * Convenient method to get local pool
	 * no need to synchronize as it's already been initialized
	 * @return 
	 */
	public DBPool getLocalPool() {
		Collection<DBPool> coll = poolLocationMap.getCollection(DbLocation.LOCAL);
		
		// we should only have one here
		return coll.iterator().next();
	}
	
	/**
	 * Only get remote pools
	 * @return 
	 */
	public Collection<DBPool> getRemotePools() {
		return (Collection<DBPool>)poolLocationMap.getCollection(DbLocation.REMOTE);
	}
	
	/**
	 * Get all the pools including local and remote ones
	 * @return 
	 */
	public Collection<DBPool> getAllPools() {
		return (Collection<DBPool>)poolLocationMap.values();
	}
	
	/**
	 * get the instance, no need synchronization as it's initialized in static block
	 * @return
	 */
	public static DBManager getInstance() {
		return instance;
	}
	
	/**
	 * default to get local pool
	 * @return
	 */
	synchronized public static DBPool getDBPool() {
		//return instance._getDBPool(null);
		return instance.getLocalPool();
	}
	
	/**
	 * If poolName matches local or remote, call location version instead
	 * otherwise, call _getDBPool directly with a particular pool name
	 * @param poolName
	 * @return
	 */
	synchronized public static DBPool getDBPool(String poolName) {
		if (StringUtils.equals(DBPOOL_DEFAULT, poolName)) {
			return getDBPool();	// local
		}
		else if (StringUtils.equals(DBPOOL_REMOTE, poolName)) {
			return instance.getRemotePools().iterator().next();
		}
		else {
			return instance._getDBPool(poolName);
		}
	}
	
	
	/**
	 * 
	 * @return 
	 */
	public boolean isMonitorEnabled() {
		return monitorProperties.isEnabled();
	}
	
	/**
	 * This method creates db pool and places it in hash map
	 * @param poolName
	 * @return 
	 */
	protected DBPool _getDBPool(String poolName) {
		
		DBPool pool;
		if (poolName == null) {
			// always return the first one (default)
			//if (poolMap.size() > 0) {
			//	return poolMap.values().iterator().next();
			//}
			//else {
				// use "dbpool" as default name
				poolName = DBPOOL_DEFAULT;
			//}
		}
			
		pool = poolMap.get(poolName);
		if (pool == null) {
			pool = new DBPool(poolName, monitorProperties);
			poolMap.put(poolName, pool);
		}
		
		return pool;
	}

	/**
	 * this has to be a singleton
	 */
	protected DBManager() throws Exception {
		// load up the connection pool
		try {
			// initialize properties
			monitorProperties = new MonitorProperties();

			// load the class using context class loader to avoid multiple class loader issue.
			Class.forName(POOLINGDRIVER, true, Thread.currentThread().getContextClassLoader());
			
			
		}
		catch (Exception e) {
			logger.error("Unable to load dbcp pooling driver", e);
			throw e;
		}
		
	}

	/**
	 * 
	 * @param periodSeconds
	 */
	private void rescheduleMonitor(int periodSeconds) {

		// we need to do async call for this
		final int p = periodSeconds;
		AsyncExecutor.execute(new Runnable() {
			@Override
			public void run() {
				for (DBPool dbPool : poolMap.values()) {
					dbPool.rescheduleMonitor(p);
				}
			}
		});

	}


    /**
     * clean up in static
     */
    public static void destroyAll() {
        if (getInstance() != null) {
            getInstance().destroy();
        }
    }


	/**
	 * cleanup
	 */
	public void destroy() {
		for (DBPool dbPool : poolMap.values()) {
			dbPool.destroy();
		}

		monitorProperties.destroy();
	}
	
	/**
	 * This is the main method that obtains connection from the pool
	 * 
	 * @return
	 * @throws Exception
	 */
	Connection getConnection(String poolUrl) throws SQLException {
		
		Connection conn = null;
		
		try {
			conn = DriverManager.getConnection(poolUrl);
			if (conn == null) {
			}
			
			//logger.debug("Obtained connection from pool [" + poolUrl + "]");
		}
		catch (SQLException e) {
			LogUtil.errorAndThrow(logger, new SQLException("Unable to borrow db connection from " + poolUrl));
		}
		
		return conn;
	}
	

	
	///////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////
	// helpers
	/**
	 * objects could be connection, statements or resultsets
	 * 
	 * IMPORTANT Note that the objects are released in order
	 *
	 * @param dbObjects
	 */
	public static void release(Object...dbObjects) {
		
		for (Object dbObject : dbObjects) {
			if (dbObject != null) {
				try {
					if (dbObject instanceof Connection) {
						Connection conn = (Connection) dbObject;
						if (!conn.isClosed()) {
							conn.close();
						}
					}
					else if (dbObject instanceof Statement) {
						Statement stmt = (Statement) dbObject;
						stmt.close();
					}
					else if (dbObject instanceof ResultSet) {
						ResultSet rs = (ResultSet)dbObject;
						rs.close();
					}
					else if (dbObject instanceof DBExec) {
						DBExec dbExec = (DBExec)dbObject;
						dbExec.close();
					}
					else {
						throw new SQLException("Invalid db object: " + dbObject);
					}
				}
				catch (SQLException e) {
				    if (!"Already closed".equals(e.getMessage())) {
				        logger.warn("Unable to release object: " + dbObject, e);
				    }
				}
			}
		}
		
	}
	
	
	/**
	 * for rollback
	 * 
	 * it often happens in catch block while we have to enclose another
	 * try/catch which makes code redundant. So created wrapper here
	 *
	 * @param conn
	 */
	public static void rollback(Connection conn) {
		if (conn != null) {
			try {
				conn.rollback();
			}
			catch (SQLException e) {
				LogUtil.error(logger, e, "Unable to rollback.");
			}
		}
	}
	
	
	/**
	 * 
	 * @param conn
	 * @param autoCommit 
	 */
	public static void setAutoCommit(Connection conn, boolean autoCommit) {
		if (conn != null) {
			try {
				conn.setAutoCommit(autoCommit);
			}
			catch (SQLException e) {
				LogUtil.error(logger, e, "Unable to set autocommit to {0}", autoCommit);
					
			}
		}
	}
	
	
	
	///////////////////////////////////////////////////////
	///////////////////////////////////////////////////////
	///////////////////////////////////////////////////////
	///////////////////////////////////////////////////////
	// for backwards compatibility only

	
	
	
	/**
	 * testing
	 * args[0]:		command
	 * args[1..]:	parameters
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			logger.error("Missing argumenets.");
			return;
		}
		
		try {
			if (args[0].equals("ping")) {
				testPing(args);
			}
			else {
				logger.error("Invalid command.");
			}
		}
		catch (Throwable t) {
			logger.error(t);
		}
	}
	
	/**
	 * args[1]: 	interval
	 * @param args
	 */
	private static void testPing(String[] args) throws Throwable {
		if (args.length != 3) {
			logger.error("Invalid command.");
			return;
		}
		
		int intervalMs = Integer.parseInt(args[1]) * 1000;
		
		DBPool dbPool = DBManager.getDBPool();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = args[2];
		
		logger.info("SQL = " + sql);
		try {
			while (true) {
				conn = dbPool.getConnection();
				logger.info("Got connection.");
				
				ps = conn.prepareStatement(sql);
				logger.info("Prepared statement.");
				
				rs = ps.executeQuery();
				logger.info("Query successfully executed.");
				
				logger.info("Sleeping for " + (intervalMs/1000) + " seconds.");
				
				Thread.sleep(intervalMs);
			}
		}
		catch (InterruptedException ie) {
			logger.info("Interrupted.");
		}
		finally {
			DBManager.release(rs, ps, conn);
		}
	}
}
