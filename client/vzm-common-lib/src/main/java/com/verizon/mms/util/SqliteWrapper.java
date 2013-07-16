/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.util;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.RemoteException;
import android.os.SystemClock;

import com.strumsoft.android.commons.logger.Logger;

public final class SqliteWrapper {
    private static final String SQLITE_EXCEPTION_DETAIL_MESSAGE = "unable to open database file";

    private SqliteWrapper() {
        // Forbidden being instantiated.
    }

    // FIXME: need to optimize this method.
    public static boolean isLowMemory(SQLiteException e) {
        return e.getMessage().equals(SQLITE_EXCEPTION_DETAIL_MESSAGE);
    }

    public static void checkSQLiteException(Context context, SQLiteException e) {
        if (isLowMemory(e)) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("Low Memory ", e);
            }
            // Toast.makeText(context, context.getString(R.string.low_memory), Toast.LENGTH_SHORT).show();
        } else {
            throw e;
        }
    }

    public static Cursor query(Context context, ContentResolver resolver, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {

        final long start;
        if (Logger.IS_DEBUG_ENABLED) {
            start = SystemClock.uptimeMillis();
            Logger.debug("SqliteWrapper.query: uri = " + uri + ", cols = " + Arrays.toString(projection) + ", selection = <"
                    + selection + ">, args = " + Arrays.toString(selectionArgs) );
            // if (Util.onMainThread()) {
            // Logger.warn("SqliteWrapper.query: on main thread: uri = " + uri, Util.getStackTrace());
            // }
        }

        try {
            final Cursor cursor = resolver != null ? resolver.query(uri, projection, selection,
                    selectionArgs, sortOrder) : null;

            if (Logger.IS_DEBUG_ENABLED) {
            	long deltaTime = (SystemClock.uptimeMillis() - start);
                Logger.debug("SqliteWrapper.query: time = " + (deltaTime)
                        + "ms, uri = " + uri + ", cols = " + Arrays.toString(projection) + ", selection = <"
                        + selection + ">, args = " + Arrays.toString(selectionArgs) + ", returning "
                        + (cursor == null ? "null" : cursor.getCount()));
                if (Logger.TRACE_LONG_QUERIES) {
                	try {               
                		if (deltaTime > 1000) {
                			throw new Exception("temp");
                		}
                	} catch (Exception e) {
                		Logger.error("SqliteLongQuery", e);
                	}
                }
            }
            
            return cursor;
        } catch (SQLiteException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error(true, SqliteWrapper.class, "query exception:", e);
            }
            checkSQLiteException(context, e);
            return null;
        }
    }

    public static Cursor queryOrThrow(Context context, ContentResolver resolver, Uri uri,
            String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        final long start;
        if (Logger.IS_DEBUG_ENABLED) {
            start = SystemClock.uptimeMillis();
            Logger.debug("SqliteWrapper.query: uri = "
                    + uri + ", cols = " + Arrays.toString(projection) + ", selection = <" + selection
                    + ">, args = " + Arrays.toString(selectionArgs));
            // if (Util.onMainThread()) {
            // Logger.warn("SqliteWrapper.query: on main thread: uri = " + uri, Util.getStackTrace());
            // }
        }

        final Cursor cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("SqliteWrapper.query: time = " + (SystemClock.uptimeMillis() - start) + "ms, uri = "
                    + uri + ", cols = " + Arrays.toString(projection) + ", selection = <" + selection
                    + ">, args = " + Arrays.toString(selectionArgs) + ", returning "
                    + (cursor == null ? "null" : cursor.getCount()));
        }

        return cursor;
    }

    public static boolean requery(Context context, Cursor cursor) {
        try {
            return cursor.requery();
        } catch (SQLiteException e) {
            Logger.error(SqliteWrapper.class, "requery exception:", e);
            checkSQLiteException(context, e);
            return false;
        }
    }

    public static int update(Context context, ContentResolver resolver, Uri uri, ContentValues values,
            String where, String[] selectionArgs) {

        final long start;
        if (Logger.IS_DEBUG_ENABLED) {
            start = SystemClock.uptimeMillis();
            // if (Util.onMainThread()) {
            // Logger.warn("SqliteWrapper.update: on main thread: uri = " + uri, Util.getStackTrace());
            // }
            Logger.debug("SqliteWrapper.update: uri = " + uri + ", where = <" + where + ">, args = "
                    + Arrays.toString(selectionArgs) + ", values = " + values);
        }

        try {
            final int rows = resolver.update(uri, values, where, selectionArgs);

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("SqliteWrapper.update: time = " + (SystemClock.uptimeMillis() - start)
                        + "ms, uri = " + uri + ", where = <" + where + ">, args = "
                        + Arrays.toString(selectionArgs) + ", values = " + values + ", returning " + rows);
            }

            return rows;

        } catch (SQLiteException e) {
            Logger.error(SqliteWrapper.class, "update exception:", e);
            checkSQLiteException(context, e);
            return -1;
        }
    }

    public static int delete(Context context, ContentResolver resolver, Uri uri, String where,
            String[] selectionArgs) {
        final long start;
        if (Logger.IS_DEBUG_ENABLED) {
            start = SystemClock.uptimeMillis();
            // if (Util.onMainThread()) {
            // Logger.warn("SqliteWrapper.delete: on main thread: uri = " + uri, Util.getStackTrace());
            // }
            Logger.debug("SqliteWrapper.delete: uri = " + uri + ", where = <" + where + ">, args = "
                    + Arrays.toString(selectionArgs));
        }

        try {
            final int rows = resolver.delete(uri, where, selectionArgs);

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("SqliteWrapper.delete: time = " + (SystemClock.uptimeMillis() - start)
                        + "ms, uri = " + uri + ", where = <" + where + ">, args = "
                        + Arrays.toString(selectionArgs) + ", returning " + rows);
            }

            return rows;

        } catch (SQLiteException e) {
            Logger.error(SqliteWrapper.class, "delete exception:", e);
            checkSQLiteException(context, e);
            return -1;
        }
    }

    public static Uri insert(Context context, ContentResolver resolver, Uri uri, ContentValues values) {
        final long start;
        if (Logger.IS_DEBUG_ENABLED) {
            start = SystemClock.uptimeMillis();
            // if (Util.onMainThread()) {
            // Logger.warn("SqliteWrapper.insert: on main thread: uri = " + uri, Util.getStackTrace());
            // }
            Logger.debug("SqliteWrapper.insert: time = " + (SystemClock.uptimeMillis() - start)
                    + "ms, uri = " + uri + ", values = " + values);
        }

        try {
            final Uri ret = resolver.insert(uri, values);

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("SqliteWrapper.insert: time = " + (SystemClock.uptimeMillis() - start)
                        + "ms, uri = " + uri + ", values = " + values + ", returning " + ret);
            }

            return ret;

        } catch (SQLiteException e) {
            Logger.error(SqliteWrapper.class, "insert exception:", e);
            checkSQLiteException(context, e);
            return null;
        }
    }

    public static Uri insert(Context context, Uri uri, ContentValues values) {
        return insert(context, context.getContentResolver(), uri, values);
    }

    public static int delete(Context context, Uri uri, String where, String[] selectionArgs) {
        return delete(context, context.getContentResolver(), uri, where, selectionArgs);
    }

    public static int update(Context context, Uri uri, ContentValues values, String where,
            String[] selectionArgs) {
        return update(context, context.getContentResolver(), uri, values, where, selectionArgs);
    }

    public static Cursor query(Context context, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return query(context, context.getContentResolver(), uri, projection, selection, selectionArgs,
                sortOrder);
    }

    /**
     * This Method
     * 
     * @param authority
     * @param ops
     * @return
     * @throws OperationApplicationException
     * @throws RemoteException
     */
    public static ContentProviderResult[] applyBatch(Context context, String authority,
            ArrayList<ContentProviderOperation> ops) throws RemoteException, OperationApplicationException {
        return applyBatch(context, context.getContentResolver(), authority, ops);
    }

    public static ContentProviderResult[] applyBatch(Context context, ContentResolver resolver,
            String authority, ArrayList<ContentProviderOperation> ops) throws RemoteException,
            OperationApplicationException {
        try {
            long start;
            if (Logger.IS_DEBUG_ENABLED) {
                start = SystemClock.uptimeMillis();
                // if (Util.onMainThread()) {
                // Logger.warn("SqliteWrapper.insert: on main thread: uri = " + uri, Util.getStackTrace());
                // }
                Logger.debug("SqliteWrapper.applyBatch: time = " + (SystemClock.uptimeMillis() - start)
                        + "ms, uri = " + authority + ", ops = " + ops);
            }
            ContentProviderResult[] result = resolver.applyBatch(authority, ops);

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("SqliteWrapper.applyBatch: time = " + (SystemClock.uptimeMillis() - start)
                        + "ms, uri = " + authority + ", ops = " + ops + ", returning " + result);
            }
            return result;
        } catch (SQLiteException e) {
            checkSQLiteException(context, e);
            return null;
        }
    }

}
