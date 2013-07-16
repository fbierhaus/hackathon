package com.verizon.mms.helper.db;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.helper.DBContext;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class for creating the database.
 *
 * The default SQLiteOpenHelper does not allow using external storage
 **/
public class DatabaseHelper extends SQLiteOpenHelper {
	private CommonDB mParentDB = null;
	
	/**
	 * Constructor
	 * 
	 * @param context
	 */
	public DatabaseHelper(Context context, CommonDB db, String databaseName, int version) {
		// override the Context to allow using external storage
		super(new DBContext(context), databaseName, null, version);
		
		mParentDB = db;
	}

	/**
	 * Create database
	 * 
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		mParentDB.createTables(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Logger.debug("Upgrading database, this will drop tables and recreate.");
		mParentDB.deleteTables(db);
		// recreate the databases
		onCreate(db);
	}
}

