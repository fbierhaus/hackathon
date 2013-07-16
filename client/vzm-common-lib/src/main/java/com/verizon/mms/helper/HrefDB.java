package com.verizon.mms.helper;

import java.util.ArrayList;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.helper.db.CommonDB;
import com.verizon.mms.helper.db.DatabaseHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/**
 */
public class HrefDB extends CommonDB implements HrefDBConstant {
	private static final String TAG = HrefDB.class.getSimpleName();
	
	private static final String DATABASE_NAME = "link_details.db";
	private static final int DATABASE_VERSION = 3;
	
	private int mRecordCount = -1;			// keep track of the counter internal rather than have to do sql request.

	/**
	 * List of table names for batch processing.
	 */
	private static final String[] TABLE_LIST = {
		TABLE_DETAILS
	};
	
	/**
	 * Constructor.
	 */
	public HrefDB(Context context) {
		mContext = context;
		
		// create a new database helper
		mHelper = new DatabaseHelper(mContext, this, DATABASE_NAME, DATABASE_VERSION);
	}
	
	@Override
	public boolean openRead() {
		boolean ret = super.openRead();
		if (mRecordCount < 0) {
			mRecordCount = getTableCount(TABLE_DETAILS);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "Current database size: " + mRecordCount);
			}
		}
		return ret;
	}

	
	@Override
	public boolean openReadWrite() {
		boolean ret = super.openReadWrite();
		if (mRecordCount < 0) {
			mRecordCount = getTableCount(TABLE_DETAILS);
		}
		return ret;
	}

//	public int getLinkDetailsCount() {
//		return getTableCount(TABLE_DETAILS);
//	}
	
	public boolean hasLinkDetail(String url) {
		boolean ret = false;
		
		if (mDatabase != null) {
			String query = COUNT_TABLE + TABLE_DETAILS + " where url=" + url;
			SQLiteStatement countStmt = mDatabase.compileStatement(query);
			long count = countStmt.simpleQueryForLong();
			countStmt.close();
			ret = (count > 0);					
		}
		return ret;
	}
	
	/**
	 * Update a link detail in the table.
	 */
	public boolean updateLinkDetailLastRead(String url, long lastRead) {
		boolean ret = false;
		
		if (mDatabase != null) {
			try {
				ContentValues args = new ContentValues();
				args.put("last_read", lastRead);

				int count = mDatabase.update(TABLE_DETAILS, args, "url='" + url +"'", null);
				if (count != 1) {
					if (Logger.IS_WARNING_ENABLED) {
						Logger.warn(getClass(), "Error in updating Link Details last read, url=" + url);
					}
				}
				else {
					ret = true;
				}
			}
			catch (Exception e) {
				if (Logger.IS_ERROR_ENABLED) {
					Logger.error(getClass(), "Exception in Updating Link Details last read: url=" + url, e);
				}
			}
		}
		return ret;		
	}
	
	/**
	 * Update a link detail in the table.
	 */
	public boolean updateLinkDetail(String url,
								 String contentType,
								 String title,
								 String description,
								 String imageUrl,
								 int responseCode,
								 String error,
								 long lastUpdate) {
		boolean ret = false;
		
		if (mDatabase != null) {
			try {
				ContentValues args = new ContentValues();
				args.put("content_type", safeString(contentType));
				args.put("title", safeString(title));
				args.put("description", safeString(description));
				args.put("image_url", safeString(imageUrl));
				args.put("response_code", responseCode);
				args.put("error", safeString(error));
				args.put("last_update", lastUpdate);
				args.put("last_read", lastUpdate);

				int count = mDatabase.update(TABLE_DETAILS, args, "url='" + url +"'", null);
				if (count != 1) {
					if (Logger.IS_WARNING_ENABLED) {
						Logger.warn(getClass(), "Error in updating Link Details, url=" + url);
					}
				}
				else {
					ret = true;
				}
			}
			catch (Exception e) {
				if (Logger.IS_ERROR_ENABLED) {
					Logger.error(getClass(), "Exception in Updating Link Details: url=" + url, e);
				}
			}
		}
		return ret;		
	}

	/**
	 * Insert a link detail to the table.
	 */
	public long insertLinkDetail(String url,
								 String contentType,
								 String title,
								 String description,
								 String imageUrl,
								 int responseCode,
								 String error,
								 long lastUpdate) {

		SQLiteStatement insertStmt;
		insertStmt = mDatabase.compileStatement(INSERT_DETAILS);
		long rowId = 0;
		boolean inserted = false;

		try {
			insertStmt.bindString(1, url);
			insertStmt.bindString(2, safeString(contentType));
			insertStmt.bindString(3, safeString(title));
			insertStmt.bindString(4, safeString(description));
			insertStmt.bindString(5, safeString(imageUrl));
			insertStmt.bindLong(6, responseCode);
			insertStmt.bindString(7, safeString(error));
			insertStmt.bindLong(8, lastUpdate);
			insertStmt.bindLong(9, lastUpdate);	// last read
			insertStmt.bindString(10, "");	// not used

			rowId = insertStmt.executeInsert();			
			inserted = true;			
		}
		catch (Exception e) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "Exception in Inserting Link Details, try update: url=" + url + "e=" , e.getMessage());
			}
			
			// try update
			updateLinkDetail(url, contentType, title, description, imageUrl, responseCode, error, lastUpdate);
		}
		finally {
			insertStmt.close();
		}

		if (inserted == true) {
			// insertion succeeded, increment counter
			mRecordCount++;
			// Logger.debug(getClass(), "Href Record inserted: " + url + " rowId:" + rowId + " total:" + mRecordCount);

			if (mRecordMaxCount >= 0 && mRecordCount > mRecordMaxCount) {
				purgeLRU();
			}
		}

		return rowId;
	}
	
	public LinkDetail getLinkDetail(String url) {
		LinkDetail model = null;
		
		Cursor cursor = mDatabase.query(TABLE_DETAILS,
			new String[] {
				"content_type",
				"title",
				"description",
				"image_url",
				"response_code",
				"error",
				"last_update",
				"last_read",
				"image_cache"
			},
			"url=?",
			new String[] { url },
			null,
			null,
			null);
		
		if (cursor.getCount() != 1) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), TAG, "More than 1 entry or no entry found: " + cursor.getCount());
			}
		}

		if (cursor.moveToFirst()) {
			model = new LinkDetail();
			model.contentType = emptyStringToNull(cursor.getString(0));
			model.title = emptyStringToNull(cursor.getString(1));
			model.description = emptyStringToNull(cursor.getString(2));
			model.linkImage = emptyStringToNull(cursor.getString(3));
			model.responseCode = cursor.getInt(4);
			model.error = emptyStringToNull(cursor.getString(5));
			model.lastUpdate = cursor.getLong(6);
			model.lastRead = cursor.getLong(7);
			model.imagePath = emptyStringToNull(cursor.getString(8));
			
			model.link = url;
			
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "Database cache found: " + url);
			}
		}

		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		
		if (model != null) {
			// update the last used time
			updateLinkDetailLastRead(url, System.currentTimeMillis());
		}
		
		return model;
	}
	
	private int purgeLRU() {
		int count = 0;
		Cursor cursor = null;
	
		if (mRecordPurgeCount >= 0) {
			try {
				// get the column last read
				cursor = mDatabase.query(TABLE_DETAILS,
						new String[] { "last_read" },
						null, null,	null, null,
						"last_read DESC");

				ArrayList<Long> list = new ArrayList<Long>();
				if (cursor.moveToFirst()) {
					do {
						long time = cursor.getLong(0);
						list.add(new Long(time));
					} while (cursor.moveToNext());
				}

				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}

				if (list.size() > mRecordPurgeCount) {
					// get the time of last remaining record
					long time = list.get(mRecordPurgeCount-1).longValue();

					// if we have keep time defined, use the earliest timestamp
					if (mRecordKeepTime >= 0) {
						long lastKeep = System.currentTimeMillis() - mRecordKeepTime;
						time = Math.min(time,  lastKeep);
					}

					// delete any entry that is older than this time
					count = mDatabase.delete(TABLE_DETAILS, "last_read < " + time, null);

					// update record count
					mRecordCount = list.size() - count;			
				}
			}
			catch (Exception e) {
				if (Logger.IS_WARNING_ENABLED) {
					Logger.warn(getClass(), "Failed to purge Href DB: ", e);
				}
			}
			finally {
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			}
		}
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "Record purged: " + count + " Current: " + mRecordCount);
		}
		
		return count;
	}
	
	@Override
	public void createTables(SQLiteDatabase db) {
		db.execSQL(CREATE_DETAILS);
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "Table " + TABLE_DETAILS + "created");
		}
	}

	/**
	 * Testing.
	 */
	public void dumpInfo() {
		for (String table : TABLE_LIST) {
			String query = "SELECT COUNT (*) FROM " + table;
			
			try {
				SQLiteStatement stmt;
				stmt = mDatabase.compileStatement(query);
				long status = stmt.simpleQueryForLong(); 
				stmt.close();

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "Table " + table + "count=" + status);
				}
			}
			catch (Exception e) {
				if (Logger.IS_ERROR_ENABLED) {
					Logger.error(getClass(), "Table " + table + "error: {}", e.getMessage());
				}
			}
		}
	}

//	/**
//	 * Purge all events older than a given time.
//	 * 
//	 * @param id
//	 * @return
//	 */
//	public int purgeByTime(long time) {
//		int count = -1;
//		
//		if (mDatabase != null) {
//			count = mDatabase.delete(TABLE_DETAILS, "time < " + time, null);
//			if (count > 0) {
//				Logger.debug(getClass(), "Table " + TABLE_DETAILS + "purage rows " + count);
//			}
//		}
//		return count;
//	}

//	/**
//	 * Purge all events older than a given time.
//	 * 
//	 * @param id
//	 * @return
//	 */
//	public int purgeByCount(int max) {
//		int count = -1;
//		
////		if (mDatabase != null) {
////			count = mDatabase.delete(TABLE_DETAILS, "time < " + time, null);
////			if (count > 0) {
////				Log.v(TAG, "Table " + TABLE_DETAILS + " purged rows: " + count);
////			}
////		}		
//		return count;
//	}

	@Override
	public void deleteTables(SQLiteDatabase db) {
		for (String table : TABLE_LIST) {
			try {
				db.execSQL("DROP TABLE IF EXISTS " + table);
			}
			catch (Exception e) {
				if (Logger.IS_ERROR_ENABLED) {
					Logger.error("Failed to delete table " + table + ":" , e.getMessage());
				}
			}
		}
	}	
}
