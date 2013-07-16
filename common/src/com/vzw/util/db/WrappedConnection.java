/**
 * 
 */
package com.vzw.util.db;

import com.vzw.util.LogUtil;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.log4j.Logger;

/**
 * @author hud
 * 
 * This is a delegation of Connection for better control
 *
 */
public class WrappedConnection implements Connection {
	
	private static final Logger				logger = Logger.getLogger(WrappedConnection.class);
	
	/**
	 * internal connection
	 */
	private Connection	internalConn = null;

	private Date		borrowDate = null;
	DBPool				dbPool = null;


	/**
	 * for monitoring purpose
	 * only used when dbmonitor.
	 */
	StackTraceElement[]	stackTraceElementArray = null;
	
	/**
	 * for auto commit flag
	 * 
	 * -1: not set
	 * 0: false
	 * 1: true
	 */
	private int			prevAutoCommit = -1;

	/**
	 * we could use isClosed(), but for safer implementation, define it here
	 */
	private boolean		returned = false;

	/**
	 * 
	 *
	 * @param conn
	 * @param dbPool
	 */
	public WrappedConnection(Connection conn, DBPool dbPool) {
		internalConn = conn;
		this.dbPool = dbPool;
		borrowDate = new Date();
		LogUtil.dlog("DBMONITOR[{0}|{1}]: Created WrappedConnection.", dbPool.getDbcpPoolName(), toString());
	}

	/**
	 * get borrow date for calculating how long it has been borrowed
	 * @return
	 */
	public Date getBorrowDate() {
		return borrowDate;
	}

	/**
	 * @throws SQLException
	 * @see java.sql.Connection#close()
	 * 
	 * Modified to make sure it is released to the connection pool
	 */
	@Override
	public void close() throws SQLException {
		LogUtil.tdlog("DBMONITOR[{0}|{1}]::CLOSE::ENTER", dbPool.getDbcpPoolName(), toString());

		try {
		
			//@hud
			if (prevAutoCommit >= 0) {
				try {
					internalConn.setAutoCommit(prevAutoCommit == 1);
				}
				catch (SQLException e) {
					StringBuilderWriter stWriter = new StringBuilderWriter();
					e.printStackTrace(new PrintWriter(stWriter));
					LogUtil.dlog("DBMONITOR[{0}|{1}]::RESET_AUTOCOMMIT: Unable to set back AutoCommit flag: {2}, e={3}, \nstackTrace:{4}}", 
							dbPool.getDbcpPoolName(), toString(), prevAutoCommit == 1, e.getMessage(), 
							stWriter.toString());
				}
			}

			try {
				internalConn.close();
			}
			catch (SQLException e) {
				StringBuilderWriter stWriter = new StringBuilderWriter();
				e.printStackTrace(new PrintWriter(stWriter));
				LogUtil.dlog("DBMONITOR[{0}|{1}]: Unable to return connection to dbcp pool. e={2}, \nstackTrace:{3}", 
						dbPool.getDbcpPoolName(), toString(),
						e.getMessage(),
						stWriter.toString());
			}

			// for monitoring
			dbPool.releaseConnection(this);
			LogUtil.dlog("DBMONITOR[{0}|{1}]: Closed WrappedConnection.", dbPool.getDbcpPoolName(), toString());

		}
		finally {
			LogUtil.tdlog("DBMONITOR[{0}|{1}]::CLOSE::LEAVE", dbPool.getDbcpPoolName(), toString());
			
			returned = true;

			dbPool = null;
			internalConn = null;	// for garbage collection
			stackTraceElementArray = null;
			borrowDate = null;
		}

	}


	/**
	 *
	 * @return
	 */
	public DBPool getDBPool() {
		return dbPool;
	}

	/**
	 * 
	 * @return
	 */
	public StackTraceElement[] getStackTraceElementArray() {
		return stackTraceElementArray;
	}

	/**
	 *
	 * @param stackTraceElementArray
	 */
	public void setStackTraceElementArray(StackTraceElement[] stackTraceElementArray) {
		this.stackTraceElementArray = stackTraceElementArray;
	}


	
	/**
	 * facility to get internal connection object
	 * @return
	 */
	public Connection getInternalConnection() {
		return internalConn;
	}

	/**
	 * @throws SQLException
	 * @see java.sql.Connection#clearWarnings()
	 */
	@Override
	public void clearWarnings() throws SQLException {
		internalConn.clearWarnings();
	}



	/**
	 * @throws SQLException
	 * @see java.sql.Connection#commit()
	 */
	@Override
	public void commit() throws SQLException {
		internalConn.commit();
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#createStatement()
	 */
	@Override
	public Statement createStatement() throws SQLException {
		return internalConn.createStatement();
	}

	/**
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param resultSetHoldability
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#createStatement(int, int, int)
	 */
	@Override
	public Statement createStatement(int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return internalConn.createStatement(resultSetType,
				resultSetConcurrency, resultSetHoldability);
	}

	/**
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#createStatement(int, int)
	 */
	@Override
	public Statement createStatement(int resultSetType, int resultSetConcurrency)
			throws SQLException {
		return internalConn
				.createStatement(resultSetType, resultSetConcurrency);
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#getAutoCommit()
	 */
	@Override
	public boolean getAutoCommit() throws SQLException {
		return internalConn.getAutoCommit();
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#getCatalog()
	 */
	@Override
	public String getCatalog() throws SQLException {
		return internalConn.getCatalog();
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#getHoldability()
	 */
	@Override
	public int getHoldability() throws SQLException {
		return internalConn.getHoldability();
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#getMetaData()
	 */
	@Override
	public DatabaseMetaData getMetaData() throws SQLException {
		return internalConn.getMetaData();
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#getTransactionIsolation()
	 */
	@Override
	public int getTransactionIsolation() throws SQLException {
		return internalConn.getTransactionIsolation();
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#getTypeMap()
	 */
	@Override
	public Map<String, Class<?>> getTypeMap() throws SQLException {
		return internalConn.getTypeMap();
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#getWarnings()
	 */
	@Override
	public SQLWarning getWarnings() throws SQLException {
		return internalConn.getWarnings();
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#isClosed()
	 */
	@Override
	public boolean isClosed() throws SQLException {
		return internalConn.isClosed();
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#isReadOnly()
	 */
	@Override
	public boolean isReadOnly() throws SQLException {
		return internalConn.isReadOnly();
	}

	/**
	 * @param sql
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#nativeSQL(java.lang.String)
	 */
	@Override
	public String nativeSQL(String sql) throws SQLException {
		return internalConn.nativeSQL(sql);
	}

	/**
	 * @param sql
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param resultSetHoldability
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#prepareCall(java.lang.String, int, int, int)
	 */
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return internalConn.prepareCall(sql, resultSetType,
				resultSetConcurrency, resultSetHoldability);
	}

	/**
	 * @param sql
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#prepareCall(java.lang.String, int, int)
	 */
	@Override
	public CallableStatement prepareCall(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		return internalConn.prepareCall(sql, resultSetType,
				resultSetConcurrency);
	}

	/**
	 * @param sql
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#prepareCall(java.lang.String)
	 */
	@Override
	public CallableStatement prepareCall(String sql) throws SQLException {
		return internalConn.prepareCall(sql);
	}

	/**
	 * @param sql
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @param resultSetHoldability
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int, int, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency, int resultSetHoldability)
			throws SQLException {
		return internalConn.prepareStatement(sql, resultSetType,
				resultSetConcurrency, resultSetHoldability);
	}

	/**
	 * @param sql
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		return internalConn.prepareStatement(sql, resultSetType,
				resultSetConcurrency);
	}

	/**
	 * @param sql
	 * @param autoGeneratedKeys
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
			throws SQLException {
		return internalConn.prepareStatement(sql, autoGeneratedKeys);
	}

	/**
	 * @param sql
	 * @param columnIndexes
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#prepareStatement(java.lang.String, int[])
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
			throws SQLException {
		return internalConn.prepareStatement(sql, columnIndexes);
	}

	/**
	 * @param sql
	 * @param columnNames
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#prepareStatement(java.lang.String, java.lang.String[])
	 */
	@Override
	public PreparedStatement prepareStatement(String sql, String[] columnNames)
			throws SQLException {
		return internalConn.prepareStatement(sql, columnNames);
	}

	/**
	 * @param sql
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#prepareStatement(java.lang.String)
	 */
	@Override
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return internalConn.prepareStatement(sql);
	}

	/**
	 * @param savepoint
	 * @throws SQLException
	 * @see java.sql.Connection#releaseSavepoint(java.sql.Savepoint)
	 */
	@Override
	public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		internalConn.releaseSavepoint(savepoint);
	}

	/**
	 * @throws SQLException
	 * @see java.sql.Connection#rollback()
	 */
	@Override
	public void rollback() throws SQLException {
		internalConn.rollback();
	}

	/**
	 * @param savepoint
	 * @throws SQLException
	 * @see java.sql.Connection#rollback(java.sql.Savepoint)
	 */
	@Override
	public void rollback(Savepoint savepoint) throws SQLException {
		internalConn.rollback(savepoint);
	}

	/**
	 * @param autoCommit
	 * @throws SQLException
	 * @see java.sql.Connection#setAutoCommit(boolean)
	 */
	@Override
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		
		//@hud 
		if (prevAutoCommit < 0) {
			prevAutoCommit = (getAutoCommit() ? 1 : 0);
		}
		internalConn.setAutoCommit(autoCommit);
	}

	/**
	 * @param catalog
	 * @throws SQLException
	 * @see java.sql.Connection#setCatalog(java.lang.String)
	 */
	@Override
	public void setCatalog(String catalog) throws SQLException {
		internalConn.setCatalog(catalog);
	}

	/**
	 * @param holdability
	 * @throws SQLException
	 * @see java.sql.Connection#setHoldability(int)
	 */
	@Override
	public void setHoldability(int holdability) throws SQLException {
		internalConn.setHoldability(holdability);
	}

	/**
	 * @param readOnly
	 * @throws SQLException
	 * @see java.sql.Connection#setReadOnly(boolean)
	 */
	@Override
	public void setReadOnly(boolean readOnly) throws SQLException {
		internalConn.setReadOnly(readOnly);
	}

	/**
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#setSavepoint()
	 */
	@Override
	public Savepoint setSavepoint() throws SQLException {
		return internalConn.setSavepoint();
	}

	/**
	 * @param name
	 * @return
	 * @throws SQLException
	 * @see java.sql.Connection#setSavepoint(java.lang.String)
	 */
	@Override
	public Savepoint setSavepoint(String name) throws SQLException {
		return internalConn.setSavepoint(name);
	}

	/**
	 * @param level
	 * @throws SQLException
	 * @see java.sql.Connection#setTransactionIsolation(int)
	 */
	@Override
	public void setTransactionIsolation(int level) throws SQLException {
		internalConn.setTransactionIsolation(level);
	}

	/**
	 * @param map
	 * @throws SQLException
	 * @see java.sql.Connection#setTypeMap(java.util.Map)
	 */
	@Override
	public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		internalConn.setTypeMap(map);
	}

	/**
	 * 
	 * @return
	 */
	public boolean isReturned() {
		return returned;
	}

	@Override
	public Clob createClob() throws SQLException {
		return internalConn.createClob();
	}

	@Override
	public Blob createBlob() throws SQLException {
		return internalConn.createBlob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		return internalConn.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		return internalConn.createSQLXML();
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		return internalConn.isValid(timeout);
	}

	@Override
	public void setClientInfo(String name, String value) throws SQLClientInfoException {
		internalConn.setClientInfo(name, value);
	}

	@Override
	public void setClientInfo(Properties properties) throws SQLClientInfoException {
		internalConn.setClientInfo(properties);
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		return internalConn.getClientInfo(name);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		return internalConn.getClientInfo();
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
		return internalConn.createArrayOf(typeName, elements);
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
		return internalConn.createStruct(typeName, attributes);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return internalConn.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return internalConn.isWrapperFor(iface);
	}
	
	

}
