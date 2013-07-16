/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vzw.util.db;

import com.vzw.util.CleanupUtil;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import javax.sql.DataSource;
import org.apache.commons.dbutils.*;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

/**
 *
 * @author hud
 *
 * Database Utilities
 */
public class DBUtil {
	
	/**
	 * For SQL failure
	 */
	public static interface ExceptionHandler {
		public void handle(Exception e) throws Exception;
	}
	
	public static class ThrowHandler implements ExceptionHandler {
		@Override
		public void handle(Exception e) throws Exception {
			throw e;
		}
	}
	
	public static final ThrowHandler THROW_HANDLER = new ThrowHandler();
	

	public static class BeanProcessEx extends BeanProcessor {
		@Override
		protected Object processColumn(
				ResultSet rs, int index, Class<?> propType
		) throws SQLException {
			if (propType.equals(Date.class)) {
				Timestamp ts = rs.getTimestamp(index);
				return ts == null ? null : new Date(ts.getTime());
			}
			else {
				return super.processColumn(rs, index, propType);
			}
		}
	}


	public static class BeanHandlerEx<T> extends BeanHandler<T> {
		public BeanHandlerEx(Class<T> type) {
			super(type, new BasicRowProcessor(new BeanProcessEx()));
		}

		public BeanHandlerEx(Class<T> type, RowProcessor rp) {
			super(type, rp);
		}
	}
	
	
	public static class BeanListHandlerEx<T> extends BeanListHandler<T>  {
		public BeanListHandlerEx(Class<T> type) {
			super(type, new BasicRowProcessor(new BeanProcessEx()));
		}
		
		public BeanListHandlerEx(Class<T> type, RowProcessor rp) {
			super(type, rp);
		}
	}


	/**
	 * Wrapper to dbutils QueryRunner: do not skip oracle incompatibility
	 * @return
	 */
	public static QueryRunner qr() {
		return new QueryRunner(false);
	}

	public static QueryRunner qr(DataSource ds) {
		return new QueryRunner(ds, false);
	}
	
	
	public static <T> T query(
			DataSource				ds,
			String					sql,
			ResultSetHandler<T>		rsh,
			ExceptionHandler		eh,
			Object...params
	) throws Exception {
		T ret = null;
		
		try {
			ret = qr(ds).query(sql, rsh, params);
		}
		catch (SQLException e) {
			if (eh != null) {
				eh.handle(e);
			}
		}
		
		
		return ret;
	}
	
	
	public static int update(
			DataSource				ds, 
			String					sql,
			ExceptionHandler		eh,
			Object...params
	) throws Exception	{
		
		int ret = 0;
		Connection conn = null;
		
		try {
			conn = ds.getConnection();
			conn.setAutoCommit(false);
			
			ret = qr().update(conn, sql,	params);
			
			conn.commit();
		}
		catch (SQLException e) {
			DBManager.rollback(conn);
			if (eh != null) {
				eh.handle(e);
			}
		}
		finally {
			CleanupUtil.release(conn);
		}
		
		return ret;
	}

}
