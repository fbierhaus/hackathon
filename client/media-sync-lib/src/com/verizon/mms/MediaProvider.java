package com.verizon.mms;

import java.util.ArrayList;
import java.util.HashMap;

import com.strumsoft.android.commons.logger.Logger;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * @author animeshkumar
 * 
 */
public class MediaProvider extends ContentProvider {
    // Database name
    private static final String DATABASE_NAME = "vz-cahce.db";
    // Version
    private static final int DATABASE_VERSION = 101;
    // Authority
    public static final String AUTHORITY = "verizon-cache";

    private static final int MMS_META_DIR = 100;

    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        final UriMatcher matcher = uriMatcher;
        matcher.addURI(AUTHORITY, "media", MMS_META_DIR); // All
    }

    // Helper
    private DatabaseHelper dbHelper;
    // Databse
    private SQLiteDatabase sqliteDb;

    public SQLiteDatabase getDb() {
        return sqliteDb;
    }

    @Override
    public boolean onCreate() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "## Creating Verizon Cache Provider");
        }
        dbHelper = new DatabaseHelper(getContext());
        sqliteDb = dbHelper.getWritableDatabase();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "#==> DB path=" + sqliteDb.getPath());
        }
        return (sqliteDb != null);
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "=====> Starting transaction");
        }
        ContentProviderResult[] result;
        try {
            sqliteDb.beginTransaction();
            result = super.applyBatch(operations);
            sqliteDb.setTransactionSuccessful();

            // send notifications
            getContext().getContentResolver().notifyChange(BASE_URI, null);

        } catch (OperationApplicationException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "<===== Aborting transaction (Failure)");
            }
            throw e;
        } finally {
            sqliteDb.endTransaction();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "<===== Ending transaction (Success)");
        }
        return result;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] cvs) {
        return -1;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        long row = 0L;

        switch (uriMatcher.match(uri)) {
        // all
        case MMS_META_DIR:
            try {
                if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Delete: table= " + Helper.TABLE_NAME + " selection= " + selection + " selectionArgs= "+ selectionArgs);
                }
                row = sqliteDb.delete(Helper.TABLE_NAME, selection, selectionArgs);
            } catch (SQLException e) {
                Logger.error(getClass(), "error=", e);
            }
            break;
        default:
            throw new IllegalArgumentException("Unsupported Operation for Uri=" + uri);
        }

        // if successful
        if (row > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return (int) row;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues cv) {
        long row = 0L;

        switch (uriMatcher.match(uri)) {
        // all
        case MMS_META_DIR:
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Insert: " + uri + "=>" + cv);
            }
            row = getDb().insert(Helper.TABLE_NAME, "", cv);
            break;
        default:
            throw new IllegalArgumentException("Unsupported Operation for Uri=" + uri);
        }

        // if successful
        if (row > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(uri, row);
        }
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "## Query for uri=" + uri + ", projection=" + projection + ", selection="
                    + selection + ", selectionArgs=" + selectionArgs + ", sortOrder=" + sortOrder);
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
        // All
        case MMS_META_DIR:
            qb.setTables(Helper.TABLE_NAME);
            qb.setProjectionMap(Helper.projectionMap);
            break;
        default:
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        Cursor cur = qb.query(getDb(), projection, selection, selectionArgs, null, null, sortOrder);
        return cur;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Nothing to do here.
        return 0;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "### Creating table...");
                Logger.debug(getClass(), "query=" + Helper.CREATE_SQL);
            }
            db.execSQL(Helper.CREATE_SQL);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "### Dropping table...");
                // Flocks table
                Logger.debug(getClass(), "query=" + Helper.DROP_SQL);
            }
            db.execSQL(Helper.DROP_SQL);
            onCreate(db);
        }
    }

    public static class Helper {
        /* Table */
        private static String TABLE_NAME = "mms_cache";

        /* Columns */
        public static String _ID = "_id";
        public static String M_ID = "m_id";
        public static String THREAD_ID = "thread_id";
        public static String M_PART_ID = "m_part_id";
        public static String M_CT = "m_ct";
        public static String M_PART_CT = "m_part_ct";
        public static String TEXT = "m_text";
        public static String M_TYPE = "m_type";
        public static String M_READ = "m_read";
        public static String DATE = "date";
        public static String ADDRESS = "address";
        public static String WIDTH = "width";
        public static String HEIGHT = "height";
        /* Projection Map ==> columns to be picked by select queries */
        public static HashMap<String, String> projectionMap;

        static {
            projectionMap = new HashMap<String, String>();
            projectionMap.put(_ID, _ID);
            projectionMap.put(M_ID, M_ID);
            projectionMap.put(THREAD_ID, THREAD_ID);
            projectionMap.put(M_PART_ID, M_PART_ID);
            projectionMap.put(M_CT, M_CT);
            projectionMap.put(M_PART_CT, M_PART_CT);
            projectionMap.put(TEXT, TEXT);
            projectionMap.put(M_TYPE, M_TYPE);
            projectionMap.put(M_READ, M_READ);
            projectionMap.put(DATE, DATE);
            projectionMap.put(ADDRESS, ADDRESS);
            projectionMap.put(WIDTH, WIDTH);
            projectionMap.put(HEIGHT, HEIGHT);
        }

        /* Create Table Query */
        public static String CREATE_SQL = "CREATE TABLE " + TABLE_NAME + " ( "
                + _ID
                + " INTEGER PRIMARY KEY AUTOINCREMENT, " // _id
                + M_ID
                + " INTEGER, " // mms id
                + THREAD_ID
                + " INTEGER, " // thread id
                + M_PART_ID
                + " INTEGER, " // mms part id
                + M_CT
                + " TEXT, " // mms ct
                + M_PART_CT
                + " TEXT, " // mms part ct
                + TEXT
                + " TEXT, " // mms text
                + M_TYPE
                + " INTEGER, " // mms type
                + M_READ
                + " INTEGER, " // mms read?
                + DATE
                + " INTEGER, " // mms date
                + ADDRESS
                + " TEXT, " // sender/receiver
                + WIDTH
                + " INTEGER, " // sender/receiver
                + HEIGHT
                + " INTEGER, " // sender/receiver
                + "UNIQUE(" + M_ID + ", " + THREAD_ID + ", " + M_PART_ID + ", " + M_CT
                + ") ON CONFLICT REPLACE" + ");";

        /* Drop Table Query */
        public static String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static ContentValues toContentValues(Media mms) {
            ContentValues cv = new ContentValues();
            cv.put(M_ID, mms.getMId());
            cv.put(THREAD_ID, mms.getThreadId());
            cv.put(M_PART_ID, mms.getmPartId());
            cv.put(M_CT, mms.getmCt());
            cv.put(M_PART_CT, mms.getmPartCt());
            cv.put(TEXT, mms.getText());
            cv.put(M_TYPE, mms.getmType());
            cv.put(M_READ, mms.getmRead());
            cv.put(DATE, mms.getDate());
            cv.put(ADDRESS, mms.getAddress());
            if (mms.getWidth() > 0 && mms.getHeight() > 0) {
                cv.put(WIDTH, mms.getWidth());
                cv.put(HEIGHT, mms.getHeight());
            }
            return cv;
        }

        public static Media fromCursorCurentPosition(Cursor cur) {
            int mId = cur.getInt(cur.getColumnIndex(M_ID));
            int threadId = cur.getInt(cur.getColumnIndex(THREAD_ID));
            int mPartId = cur.getInt(cur.getColumnIndex(M_PART_ID));
            String mCt = cur.getString(cur.getColumnIndex(M_CT));
            String mPartCt = cur.getString(cur.getColumnIndex(M_PART_CT));
            String text = cur.getString(cur.getColumnIndex(TEXT));
            int mType = cur.getInt(cur.getColumnIndex(M_TYPE));
            int mRead = cur.getInt(cur.getColumnIndex(M_READ));
            long date = cur.getLong(cur.getColumnIndex(DATE));
            String address = cur.getString(cur.getColumnIndex(ADDRESS));
            int width = cur.getInt(cur.getColumnIndex(WIDTH));
            int height = cur.getInt(cur.getColumnIndex(HEIGHT));
            return new Media(threadId, mId, mPartId, address, mCt, mPartCt, text, mType, mRead, date, width, height);
        }
    }
}