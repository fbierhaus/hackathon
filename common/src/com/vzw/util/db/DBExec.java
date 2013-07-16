/**
 * 
 */
package com.vzw.util.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * @author hud
 * 
 * This class wraps ResultSet and Statement object
 *
 */
public class DBExec {
	
	private static Logger		logger = Logger.getLogger(DBExec.class);
	
	private ResultSet				resultSet = null;
	private Statement				statement = null;

	/**
	 * 
	 */
	public DBExec() {
		// TODO Auto-generated constructor stub
	}
	
	public DBExec(Statement statement) {
		this.statement = statement;
	}
	
	public void close() {
		DBManager.release(resultSet, statement);
	}
	
	public ResultSet exec() throws SQLException {
		PreparedStatement pstmt = null;
		if (statement instanceof PreparedStatement) {
			pstmt = (PreparedStatement)statement;
			resultSet = pstmt.executeQuery();
			return resultSet;
		}
		else {
			return null;
		}
	}
	
	public ResultSet exec(String sql) throws SQLException {
		if (statement != null) {
			resultSet = statement.executeQuery(sql);
			return resultSet;
		}
		else {
			return null;
		}
	}

	/**
	 * @return the resultSet
	 */
	public ResultSet getResultSet() {
		return resultSet;
	}

	/**
	 * @param resultSet the resultSet to set
	 */
	public void setResultSet(ResultSet resultSet) {
		this.resultSet = resultSet;
	}

	/**
	 * @return the statement
	 */
	public Statement getStatement() {
		return statement;
	}

	/**
	 * @param statement the statement to set
	 */
	public void setStatement(Statement statement) {
		this.statement = statement;
	}
	
	/**
	 * @return prepared statement
	 * NOTE: type check is not made here, this method is just for
	 * convenience
	 */
	public PreparedStatement getPs() {
		return (PreparedStatement)statement;
	}

}
