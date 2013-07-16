/**
 * 
 */
package com.vzw.util.db;

import com.vzw.util.LogUtil;
import java.io.PrintWriter;
import java.sql.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.apache.log4j.Logger;


/**
 * @author hud
 *
 */
public class DBPool implements DataSource {
	
	private static Logger		logger = Logger.getLogger(DBPool.class);
	
	/**
	 * For debug purpose
	 */
	private boolean				enableMonitor = false;
	

	/**
	 * 5 minutes
	 * if a connection was held for more than this value
	 * a warning message will be logged
	 */
	//private static final long	CONN_HOLD_WARN_THRESHOLD = 3000000;
	
	
	private String 				poolUrl	= null;
	private String				dbcpPoolName = null;
	
	//private DBManager			dbManager = DBManager.getInstance();

	private boolean 			closeLongHeld = true;

	/**
	 * Holds a set of connection objects for monitoring
	 */
	private final Set<WrappedConnection>	connSet = new HashSet<WrappedConnection>();


	/**
	 * timer to display current states
	 */
	private ScheduledThreadPoolExecutor		monitorExecutor = null;
	private ScheduledFuture<?>				monitorFuture = null;

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isCloseLongHeld() {
		return closeLongHeld;
	}

	public void setCloseLongHeld(boolean closeLongHeld) {
		this.closeLongHeld = closeLongHeld;
	}

	private class MonitorRunnable implements Runnable {

		@Override
		public void run() {

			try {
				LogUtil.tdlog("DBMONITOR::RUN::ENTER");

				// for synchronization
				Set<WrappedConnection> connSetWork;
				synchronized (connSet) {
					connSetWork = new HashSet<WrappedConnection>(connSet);//Set<WrappedConnection>)((HashSet<WrappedConnection>)connSet).clone();
				}

				LogUtil.dlog("DBMONITOR[{0}]: borrowed connections: {1}", dbcpPoolName, connSetWork.size());

				Date curTime = new Date();
				long diffTime;
				for (WrappedConnection conn : connSetWork) {
					
					LogUtil.dlog("DBMONITOR[{0}|{1}]: checking connection", dbcpPoolName, conn.toString());
					
					if (conn.isReturned()) {
						LogUtil.dlog("DBMONITOR[{0}|{1}]: connection has already been returned, skipping", dbcpPoolName, conn.toString());
						continue;
					}

					
					diffTime = curTime.getTime() - conn.getBorrowDate().getTime();
					if (diffTime > monitorProperties.getConnHeldWarnThresholdSeconds() * 1000) {
						logWarn(conn, diffTime);
						if(closeLongHeld && diffTime > monitorProperties.getConnCloseAfterHeldSeconds() * 1000) {
							logger.warn("DBMONITOR: (warn) closing [" + dbcpPoolName + "] connection (" + conn + ") has been held for " +
									diffTime / 1000 + " seconds");
							LogUtil.dlog("DBMONITOR[{0}|{1}]: force closing the connection.", dbcpPoolName, conn.toString());
							conn.close();
						}

					}
					else {
						if (logger.isDebugEnabled()) {
							//logger.debug("DBMONITOR: (normal) [" + dbcpPoolName + "] connection (" + conn + ") has been held for " +
							//		diffTime / 1000 + " seconds");
							LogUtil.dlog("DBMONITOR[{0}|{1}]: Connection has been held for {2} seconds.", 
									dbcpPoolName,
									conn.toString(),
									TimeUnit.MILLISECONDS.toSeconds(diffTime));
						}
					}
				}

				// clear it for garbage collection (not necessary sometimes)
				//connSetWork = null;
			}
			catch (Throwable t) {
				LogUtil.error(logger, t, "MONITORDB[{0}]: failed.", dbcpPoolName);
			}
			finally {
				LogUtil.tdlog("DBMONITOR::RUN::LEAVE");
			}


		}


		private void logWarn(WrappedConnection conn, long diffTime) {
			LogUtil.warn(logger, "DBMONITOR[{0}|{1}]: Connection has been held for {2} seconds.", 
					dbcpPoolName,
					conn.toString(),
					TimeUnit.MILLISECONDS.toSeconds(diffTime));
			
			/*
			StringBuilder sb = new StringBuilder("DBMONITOR: (warn) [");
			sb.append(dbcpPoolName)
			  .append("] connection (")
			  .append(conn)
			  .append(") has been held for ")
			  .append(diffTime / 1000)
			  .append(" seconds.");
			 */

			if (conn.getStackTraceElementArray()  != null) {
				StringBuilder sb = new StringBuilder();

				sb.append(MessageFormat.format("DBMONITOR[{0}|{1}]: Stack Trace: \n", dbcpPoolName, conn.toString()));


				for (StackTraceElement ste : conn.getStackTraceElementArray()) {
					sb.append(ste.toString()).append("\n");
				}
				LogUtil.warn(logger, sb.toString());
			}
			
		}

	}

	private MonitorRunnable					monitorRunnable = null;
	
	DBManager.MonitorProperties				monitorProperties = null;

	/**
	 * 
	 */
	DBPool(String poolName, DBManager.MonitorProperties monitorProperties) {
		poolUrl = DBManager.BASEPOOLURL + poolName;
		dbcpPoolName = "/" + poolName;

		this.monitorProperties = monitorProperties;
		this.enableMonitor = monitorProperties.isEnabled();

		// for monitoring
		if (enableMonitor) {
			monitorExecutor = new ScheduledThreadPoolExecutor(1);
			monitorRunnable = new MonitorRunnable();

			// every 60 seconds, print out the information as needed
			// (use info)
			monitorFuture = monitorExecutor.scheduleAtFixedRate(
					monitorRunnable, 0, monitorProperties.getMonitorPeriodSeconds(), TimeUnit.SECONDS);
		}
	}


	/**
	 *
	 * @param periodSeconds
	 */
	void rescheduleMonitor(int periodSeconds) {
		// try here
		try {
			if (enableMonitor) {
				if (monitorExecutor != null) {
					monitorExecutor.shutdown();
					monitorExecutor.awaitTermination(500, TimeUnit.SECONDS);

					// create a new one
					monitorExecutor = new ScheduledThreadPoolExecutor(1);
					monitorFuture = monitorExecutor.scheduleAtFixedRate(
							monitorRunnable, 0, periodSeconds, TimeUnit.SECONDS);
				}
			}
		}
		catch (InterruptedException e) {
			logger.error(e);
		}
	}


	/**
	 * clean up
	 */
	public void destroy() {
		// stop the monitoring thread
		if (enableMonitor) {
			if (monitorExecutor != null) {
				monitorExecutor.shutdown();
			}
		}
	}

	/**
	 *
	 * @return
	 * @throws java.sql.SQLException
	 */
	@Override
	public Connection getConnection() throws SQLException {

		//LogUtil.tdlog("DBMONITOR[{0}]::GET_CONN::ENTER", dbcpPoolName);
		LogUtil.debug(logger, "Get connection start: {0}", poolUrl);

		WrappedConnection conn = null;
		try {

			Connection _conn = DBManager.getInstance().getConnection(poolUrl);
			LogUtil.debug(logger, "Got connection from pool: {0}", poolUrl);
			
			conn = new WrappedConnection(_conn, this);

			// add to the set
			if (enableMonitor) {
				synchronized (connSet) {
					connSet.add(conn);
				}

				if (monitorProperties.isEnableStackTrace()) {
					LogUtil.debug(logger, "Get connection: stack trace. {0}", poolUrl);
					conn.setStackTraceElementArray(Thread.currentThread().getStackTrace());
				}
			}

		}
		finally {
			//LogUtil.tdlog("DBMONITOR[{0}]::GET_CONN::LEAVE: conn={1}", dbcpPoolName, conn == null ? "null" : conn.toString());
			LogUtil.debug(logger, "Get connection end: {0}", poolUrl);
		}
		
		return conn;

	}


	void releaseConnection(WrappedConnection conn) {
		if (enableMonitor) {
			synchronized (connSet) {
				connSet.remove(conn);
			}
		}
	}

	//-----------------------------------------------------------
	/** Get a Vector of StringArrays from the specified sql.
	 *  Each Vector element represents a row, each array element a field value
	 *  This is restricted to an upper limit of maxCount
	 *
	 * @param sql
	 * @return
	 * @throws SQLException
	 * @deprecated 
	 */
	public Vector<String[]> getSQLResults(String sql) throws SQLException {
		return getSQLResults(sql, -1);
	}
	
	/**
	 *
	 * @param sql
	 * @param maxCount
	 * @return
	 * @throws java.sql.SQLException
	 * @deprecated 
	 */
	public Vector<String[]> getSQLResults(String sql, int maxCount) throws SQLException {
		
		Vector<String[]> rows = new Vector<String[]>();
		Statement stmt = null;
		ResultSet rs = null;
		ResultSetMetaData md = null;
		Connection conn = null;
				
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			md = rs.getMetaData();
			int numCols = md.getColumnCount();

			//iterate over every row
			int currRow = 0;
			while (rs.next()) {
				String vals[] = new String[numCols];
				for (int c = 0; c < numCols; c++) {
					String val = rs.getString(c + 1);
					if (val == null) val = "";
					vals[c] = val;
				}
				rows.addElement(vals);
				vals = null;
				currRow ++;
				
				if (currRow >= maxCount && maxCount >= 0) {
					break;
				}
			}

		} 
		finally {
			DBManager.release(rs, stmt, conn);
			
			md = null;
			rs = null;
			stmt = null;
			conn = null;
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("[getSQLResults] - Total Rows:" + rows.size());
		}
		
		return rows;
	}
	
	
	
	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	//@hud VMEP2 an improved version of get sql results
	
	/**
	 *
	 * @param sql
	 * @return
	 * @throws java.lang.Throwable
	 */
	public List<List<Object>> getResultAsList(String sql) throws Throwable {
		return getResultAsList(sql, -1);
	}	
	/**
	 * 
	 *
	 * @param sql
	 * @param maxCount
	 * @return
	 * @throws Throwable
	 */
	public List<List<Object>> getResultAsList(String sql, int maxCount) throws Throwable {
		
		Statement stmt = null;
		ResultSet rs = null;
		ResultSetMetaData md = null;
		Connection conn = null;
				
		List<List<Object>> resList = null; 
		try {
			
			resList = new ArrayList<List<Object>>();
			
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			md = rs.getMetaData();
			int numCols = md.getColumnCount();

			//iterate over every row
			int currRow = 0;
			List<Object> vals = null;
			while (rs.next()) {
				vals = new ArrayList<Object>();
				for (int c = 1; c <= numCols; c++) {
					vals.add(rs.getObject(c));
				}
				resList.add(vals);
				currRow ++;
				
				if (currRow >= maxCount && maxCount >= 0) {
					break;
				}
			}

		} 
		finally {
			DBManager.release(rs, stmt, conn);
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("[getSQLResults] - Total Rows:" + resList.size());
		}
		
		return resList;
	}

	
	
	/**
	 *
	 * @param sql
	 * @param typeMap
	 * @return
	 * @throws java.lang.Throwable
	 */
	public List<List<Object>> getResultAsList(String sql, Map<String, Class<?>> typeMap) throws Throwable {
		return getResultAsList(sql, typeMap, -1);
	}	
	/**
	 * 
	 *
	 * @param sql
	 * @param typeMap
	 * @param maxCount
	 * @return
	 * @throws Throwable
	 */
	public List<List<Object>> getResultAsList(String sql, Map<String, Class<?>> typeMap, int maxCount) throws Throwable {
		
		Statement stmt = null;
		ResultSet rs = null;
		ResultSetMetaData md;
		Connection conn = null;
				
		List<List<Object>> resList = null; 
		try {
			
			resList = new ArrayList<List<Object>>();
			
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			md = rs.getMetaData();
			int numCols = md.getColumnCount();

			//iterate over every row
			int currRow = 0;
			List<Object> vals;
			while (rs.next()) {
				vals = new ArrayList<Object>();
				for (int c = 1; c <= numCols; c++) {
					vals.add(rs.getObject(c, typeMap));
				}
				resList.add(vals);
				currRow ++;
				
				if (currRow >= maxCount && maxCount >= 0) {
					break;
				}
			}

		} 
		finally {
			DBManager.release(rs, stmt, conn);
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug("[getSQLResults] - Total Rows:" + resList.size());
		}
		
		return resList;
	}
	
	
	
	/**
	 * This is more generic version of getting SQL result
	 *
	 * @param sql
	 */
	public void execSql(String sql) {
		//TODO
	}

	public String getPoolUrl() {
		return poolUrl;
	}

	public String getDbcpPoolName() {
		return dbcpPoolName;
	}
	
	
}
