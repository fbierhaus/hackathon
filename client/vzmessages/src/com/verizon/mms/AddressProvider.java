/**
 * AddressProvider.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.model.AddressModel;

/**
 * This class/interface
 * 
 * @author Imthiaz
 * @Since Apr 11, 2012
 */
public class AddressProvider extends ContentProvider {
	// Database name
	private static final String DATABASE_NAME = "vz-locations.db";
	// Version
	private static final int DATABASE_VERSION = 200;
	// Authority
	public static final String AUTHORITY = "address-cache";

	private static final int LOC_META_DIR = 200;

	public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

	private static final UriMatcher uriMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);

	static {
		final UriMatcher matcher = uriMatcher;
		matcher.addURI(AUTHORITY, "address", LOC_META_DIR);
	}

	// Helper
	private DatabaseHelper helper;
	// DB
	private SQLiteDatabase sqliteDB;

	public SQLiteDatabase getDb() {
		return sqliteDB;
	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#delete(android.net.Uri,
	 * java.lang.String, java.lang.String[])
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		long row = 0L;

		try {
		    if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "Delete: table='" + AddressHelper.TABLE_NAME
					+ "', selection='" + selection + "', selectionArgs="
					+ selectionArgs);
		    }
			row = sqliteDB.delete(AddressHelper.TABLE_NAME, selection,
					selectionArgs);
		} catch (SQLException e) {
			Logger.error(AddressProvider.class, e);
		}
		if (row > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return (int) row;
	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues cv) {

		long row = 0L;
		 if (Logger.IS_DEBUG_ENABLED) {
		Logger.debug(getClass(), "Insert : " + uri + " => " + cv);
		 }
		row = getDb().insert(AddressHelper.TABLE_NAME, "", cv);
		if (row > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
			return ContentUris.withAppendedId(uri, row);
		}
		return null;
	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#onCreate()
	 */
	@Override
	public boolean onCreate() {
		helper = new DatabaseHelper(getContext());
		sqliteDB = helper.getWritableDatabase();
		return (sqliteDB != null);
	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 * java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(AddressHelper.TABLE_NAME);
		if (projection == null) {

			qb.setProjectionMap(AddressHelper.projectionMap);
		}
		Cursor cur = qb.query(getDb(), projection, selection, selectionArgs,
				null, null, sortOrder);

		return cur;
	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 * android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		int count = sqliteDB.update(AddressHelper.TABLE_NAME, values,
				selection, selectionArgs);
		// send notifications
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/*
		 * Overriding method (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database
		 * .sqlite.SQLiteDatabase)
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(AddressHelper.CREATE_SQL);
		}

		/*
		 * Overriding method (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database
		 * .sqlite.SQLiteDatabase, int, int)
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			db.execSQL(AddressHelper.DROP_SQL);
			onCreate(db);
		}

	}

	public static class AddressHelper implements BaseColumns {

		/* Table */
		private static String TABLE_NAME = "loc_cache";

		/* Drop Table Query */
		public static String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME;

		// Columns
		public static String FAVORITE = "favorite";
		public static String ADDRESS = "address";
		public static String CITYSTATEZIP = "citystatezip";
		public static String LAT = "latitude";
		public static String LON = "longitude";
		public static String DATE = "date";
		public static String NAME = "title";

		public static HashMap<String, String> projectionMap;

		static {
			projectionMap = new HashMap<String, String>();
			projectionMap.put(_ID, _ID);
			projectionMap.put(FAVORITE, FAVORITE);
			projectionMap.put(LAT, LAT);
			projectionMap.put(LON, LON);
			projectionMap.put(NAME, NAME);
			projectionMap.put(ADDRESS, ADDRESS);
			projectionMap.put(CITYSTATEZIP, CITYSTATEZIP);
			projectionMap.put(DATE, DATE);
		}

		/* Create Table Query */
		public static String CREATE_SQL = "CREATE TABLE " + TABLE_NAME
				+ " ( "
				+ _ID
				+ " INTEGER PRIMARY KEY AUTOINCREMENT, " // _id
				+ FAVORITE + " INTEGER, " + DATE + " INTEGER, " + LAT
				+ " FLOAT, " + LON + " FLOAT, " + CITYSTATEZIP + " TEXT, "
				+ ADDRESS + " TEXT, " + NAME + " TEXT " + ");";

		public static ContentValues toContentValues(AddressModel loc) {
			ContentValues cv = new ContentValues();

			cv.put(LAT, loc.getLatitude());
			cv.put(LON, loc.getLongitude());
			cv.put(ADDRESS, loc.getAddressText());
			cv.put(FAVORITE, loc.isFavoriteState());
			cv.put(NAME, loc.getPlaceName());
			cv.put(DATE, loc.getCurrentTime());
			cv.put(CITYSTATEZIP, loc.getAddressContext());
			return cv;
		}

		public static AddressModel fromCursor(Cursor cursor) {
			int id = cursor.getInt(0); // To get the auto incremented ID value
			boolean favState = (cursor.getInt(cursor.getColumnIndex(FAVORITE)) == 1) ? true
					: false;
			long date = cursor.getLong(cursor.getColumnIndex(DATE));
			String address = cursor.getString(cursor.getColumnIndex(ADDRESS));
			double lat = cursor.getDouble(cursor.getColumnIndex(LAT));
			double lon = cursor.getDouble(cursor.getColumnIndex(LON));
			String context = cursor.getString(cursor
					.getColumnIndex(CITYSTATEZIP));
			String name = cursor.getString(cursor.getColumnIndex(NAME));
			AddressModel loc = new AddressModel(id, address, date, favState,
					lat, lon, context, name);
			return loc;
		}

	}

}
