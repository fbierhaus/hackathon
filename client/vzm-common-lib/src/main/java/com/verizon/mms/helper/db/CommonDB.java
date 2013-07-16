package com.verizon.mms.helper.db;


import com.strumsoft.android.commons.logger.Logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/**
 * Abstract class for Database classes
 * 
 * Provide some common functions for the subclasses.
 */
public abstract class CommonDB {
	// for controlling the cache size
	protected int mRecordMaxCount = -1;			// maximum number to store
	protected int mRecordPurgeCount = -1;			// when purge, cut it down to this number, purge must be <= max
	protected long mRecordKeepTime = -1;			// longest time to keep since last read

	/**
	 * Create all tables of the database
	 * 
	 * @param db
	 */
	public abstract void createTables(SQLiteDatabase db);
	
	/**
	 * Delete all tables of the database
	 * 
	 * @param db
	 */
	public abstract void deleteTables(SQLiteDatabase db);

	protected Context mContext;
	
	/**
	 * SQLiteDatabase object after it's opened (read-only or read/write)
	 */
	protected SQLiteDatabase mDatabase;
	
	/**
	 * Helper object for simplify some actions.
	 */
	protected DatabaseHelper mHelper;
	
	/**
	 * SQL statement to get size of a table.
	 */
	protected static final String COUNT_TABLE = "select count(*) from ";

	/**
	 * Open a read/write database.
	 */
	public boolean openReadWrite() {
		// close the database if one is opened
//		close();
		
		if (mDatabase == null) {
			try {
				mDatabase = mHelper.getWritableDatabase();
			}
			catch (Exception e) {
				Logger.debug("Failed to open writable database: " + e.getMessage());
			}
		}
		return (mDatabase != null);		
	}

	/**
	 * Open a read only database
	 */
	public boolean openRead() {
		// close the database if one is opened
//		close();
		
		if (mDatabase == null) {
			try {
				mDatabase = mHelper.getReadableDatabase();
			}
			catch (Exception e) {
				Logger.debug("Failed to open readable database: " + e.getMessage());
			}
		}
		return (mDatabase != null);
	}
	
	/**
	 * Close the database.
	 */
	public void close() {
		if (mDatabase != null) {
			mDatabase.close();
			mDatabase = null;
		}
	}
	
	/**
	 * Get the database's path
	 * 
	 * @return
	 */
	public String getPath() {
		return mDatabase.getPath();		
	}

	/**
	 * Get size of a table
	 * 
	 * @param table
	 * @return
	 */
	protected int getTableCount(String table) {
		String query = COUNT_TABLE + table;
		SQLiteStatement countStmt = mDatabase.compileStatement(query);		
		long count = countStmt.simpleQueryForLong();
		countStmt.close();
		return (int)count;		
	}
	
	/**
	 * Helper function to convert int to boolean
	 * 
	 * @param n
	 * @return
	 */
	protected boolean intToBool(int n) {
		return n == 0? false : true;
	}
	
	protected String safeString(String str) {
		if (str == null) {
			str = "";
		}
		return str;
	}
	
	protected String emptyStringToNull(String str) {
		if (str != null) {
			if (str.length() == 0) {
				str = null;
			}
		}
		return str;
	}
	
	public void setStorageCount(int max, int purge, long keepTime) {
		mRecordMaxCount = max;
		mRecordPurgeCount = purge;
		mRecordKeepTime = keepTime;
	}
}
