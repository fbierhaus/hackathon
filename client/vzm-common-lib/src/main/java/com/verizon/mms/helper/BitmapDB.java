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
public class BitmapDB extends CommonDB implements BitmapDBConstant {
	private static final String TAG = BitmapDB.class.getSimpleName();
	
	private static final String DATABASE_NAME = "bitmap_index.db";
	private static final int DATABASE_VERSION = 4;
	
	private int mRecordCount = -1;			// keep track of the counter internal rather than have to do sql request.

	public class BitmapRecord {
		public String url;
		public String contentType;
		public int responseCode;
		public String error;
		public byte[] bitmap;				// compressed JPEG
		public long lastUpdate;
		public long lastRead;
	};

	/**
	 * List of table names for batch processing.
	 */
	private static final String[] TABLE_LIST = {
		TABLE_BITMAP
	};
	
	/**
	 * Constructor.
	 */
	public BitmapDB(Context context) {
		mContext = context;
		
		// create a new database helper
		mHelper = new DatabaseHelper(mContext, this, DATABASE_NAME, DATABASE_VERSION);
	}

	@Override
	public boolean openRead() {
		boolean ret = super.openRead();
		if (mRecordCount < 0) {
			mRecordCount = getTableCount(TABLE_BITMAP);
		}
		return ret;
	}

	
	@Override
	public boolean openReadWrite() {
		boolean ret = super.openReadWrite();
		if (mRecordCount < 0) {
			mRecordCount = getTableCount(TABLE_BITMAP);
		}
		return ret;
	}

	/**
	 * Update a link detail in the table.
	 */
	public boolean updateRecordLastRead(String url, long lastRead) {
		boolean ret = false;
		
		if (mDatabase != null) {
			try {
				ContentValues args = new ContentValues();
				args.put("last_read", lastRead);

				int count = mDatabase.update(TABLE_BITMAP, args, "url='" + url +"'", null);
				if (count != 1) {
					if (Logger.IS_WARNING_ENABLED) {
						Logger.warn(getClass(), "Error in updating bitmap last read, url=" + url);
					}
				}
				else {
					ret = true;
				}
			}
			catch (Exception e) {
				if (Logger.IS_ERROR_ENABLED) {
					Logger.error(getClass(), "Exception in Updating bitmap last read: url=" + url, e);
				}
			}
		}
		return ret;		
	}
	
	/**
	 * Update an event to the event table.
	 */
	public boolean updateRecord(String url,
								 String contentType,
								 int responseCode,
								 byte[] bitmap,
								 String error,
								 long lastUpdate) {
		boolean ret = false;

		if (bitmap == null) {
			bitmap = new byte[0];			
		}

		if (mDatabase != null) {
			try {
				ContentValues args = new ContentValues();
				args.put("content_type", safeString(contentType));
				args.put("response_code", responseCode);
				args.put("bitmap", bitmap);
				args.put("bitmap_size", bitmap.length);
				args.put("error", safeString(error));
				args.put("last_update", lastUpdate);
				args.put("last_read", lastUpdate);

				int count = mDatabase.update(TABLE_BITMAP, args, "url='" + url + "'", null);
				if (count != 1) {
					if (Logger.IS_ERROR_ENABLED) {
						Logger.error(getClass(), "Error in updating Link Details, url=" + url);
					}
				}
				else {
					ret = true;
				}
			}
			catch (Exception e) {
				if (Logger.IS_ERROR_ENABLED) {
					Logger.error(getClass(), "Exception in Updating Bitmap: url=" + url, e);
				}
			}
		}
		return ret;		
	}
	
	/**
	 * Insert an event to the event table.
	 */
	public boolean updateBitmap(String url,
								byte[] bitmap) {
		boolean ret = false;
		
		if (bitmap == null) {
			bitmap = new byte[0];			
		}
		
		if (mDatabase != null) {
			ContentValues args = new ContentValues();
			if (bitmap != null) {
				args.put("bitmap", bitmap);
				args.put("bitmap_size", bitmap.length);
			}

			int count = mDatabase.update(TABLE_BITMAP, args, "url='" + url +"'", null);
			if (count != 1) {
				if (Logger.IS_ERROR_ENABLED) {
					Logger.error(getClass(), "Error in updating Bitmap, url=" + url);
				}
			}
			else {
				ret = true;
			}
		}
		return ret;		
	}

	/**
	 * Insert an event to the event table.
	 */
	public long insertRecord(String url,
							 String contentType,
							 int responseCode,
							 byte[] bitmap,
							 String error,
							 long lastUpdate) {
		SQLiteStatement insertStmt;
		insertStmt = mDatabase.compileStatement(INSERT_BITMAP);
		long rowId = 0;
		boolean inserted = false;
		
		if (bitmap == null) {
			bitmap = new byte[0];			
		}

		try {
			insertStmt.bindString(1, url);
			insertStmt.bindString(2, safeString(contentType));
			insertStmt.bindLong(3, responseCode);
			insertStmt.bindBlob(4, bitmap);
			insertStmt.bindLong(5, bitmap.length);
			insertStmt.bindString(6, safeString(error));
			insertStmt.bindLong(7, lastUpdate);
			insertStmt.bindLong(8, lastUpdate);	// last read

			rowId = insertStmt.executeInsert();
			inserted = true;
		}
		catch (Exception e) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "Exception in Inserting Bitmap, try update: url=" + url + "e=" + e.getMessage());
			}
			
			// try update
			updateRecord(url, contentType, responseCode, bitmap, error, lastUpdate);
		}
		finally {
			insertStmt.close();
		}
		
		if (inserted == true) {
			// insertion succeeded, increment counter
			mRecordCount++;
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "Bitmap Record inserted: " + url + " rowId:" + rowId + " total:" + mRecordCount);
			}

			if (mRecordMaxCount >= 0 && mRecordCount > mRecordMaxCount) {
				purgeLRU();
			}
		}
		
		return rowId;
	}
	
	public BitmapRecord getBitmap(String url) {
		BitmapRecord model = null;
		
		Cursor cursor = mDatabase.query(TABLE_BITMAP,
			new String[] {
				"content_type",
				"response_code",
				"error",
				"bitmap",
				"last_update",
				"last_read"
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
			model = new BitmapRecord();
			model.contentType = emptyStringToNull(cursor.getString(0));
			model.responseCode = cursor.getInt(1);
			model.error = emptyStringToNull(cursor.getString(2));
			model.bitmap = cursor.getBlob(3);
			model.lastUpdate = cursor.getLong(4);
			model.lastRead = cursor.getLong(5);			
			model.url = url;
			
			// replace 0 length bitmap with null
			if (model.bitmap.length == 0) {
				model.bitmap = null;
			}
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "Database cache found: "+ url);
			}
		}

		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		
		if (model != null) {
			// update the last used time
			updateRecordLastRead(url, System.currentTimeMillis());
		}
		
		return model;
	}
	
	private int purgeLRU() {
		int count = 0;
		Cursor cursor = null;
		
		if (mRecordPurgeCount >= 0) {
			try {
				// get the column last read
				cursor = mDatabase.query(TABLE_BITMAP,
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
					count = mDatabase.delete(TABLE_BITMAP, "last_read < " + time, null);

					// update record count
					mRecordCount = list.size() - count;	
				}
			} catch (Exception e) {
				if (Logger.IS_WARNING_ENABLED) {
					Logger.warn(getClass(), "Failed to purge Bitmap DB: ", e);
				}
			} finally {
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
		db.execSQL(CREATE_EVENT);
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "Table " + TABLE_BITMAP + "created");
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
//				log.info("Table {} purage rows {}", TABLE_DETAILS, count);
//			}
//		}
//		return count;
//	}
//
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
					Logger.error(getClass(), "Failed to delete table " + table + ":" , e.getMessage());
				}
			}
		}
	}	
}
