/**
 * VMAProvider.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs.provider;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.AcraReports;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.Settings;
import com.verizon.messaging.vzmsgs.provider.Vma.LinkedVMADevices;
import com.verizon.messaging.vzmsgs.provider.Vma.RecentlyUsedFwdAddr;
import com.verizon.messaging.vzmsgs.provider.Vma.RecentlyUsedReplyMsg;
import com.verizon.messaging.vzmsgs.provider.Vma.SyncStatusTable;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jun 13, 2012
 */
public class ApplicationProvider extends ContentProvider {

    public static final String AUTHORITY = "vma";
    private AppDatabaseHelper helper;
    private static final String DATABASE_NAME = "vma.db";
    private static final int DATABASE_VERSION = 3;

    private static final UriMatcher uriMatcher;
    private static final int SETTINGS_MATCH_INDEX = 1;
    private static final int VMA_SYNC_STATUS = 2;
    private static final int VMA_SYNC_STATUS_ID = 5;
    private static final int VMA_LINKED_DEVICES = 8;
    private static final int VMA_EVENTS = 9;
    private static final int VMA_EVENTS_ID = 10;
    private static final int VMA_REPLY_MSG = 11;
    private static final int VMA_FWD_ADDR = 12;

    private static final int VMA_SYNC_MAPPING = 13;
    private static final int VMA_SYNC_MAPPING_ID = 14;
    private static final int VMA_SYNC_MAPPING_UID = 15;

    private static final int ACRA_REPORTS = 16;
    private static final int ACRA_REPORTS_ID = 17;

    private static final HashMap<String, String> syncStatusProjection;
    private static final HashMap<String, String> vmaEventsProjection;
    private static final HashMap<String, String> settingDefaultProjection;
    private static final HashMap<String, String> deviceProjection;
    private static final HashMap<String, String> autoReplyProjection;
    private static final HashMap<String, String> autoFwdProjection;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        // Settings
        uriMatcher.addURI(AUTHORITY, Settings.TABLE_NAME, SETTINGS_MATCH_INDEX);

        uriMatcher.addURI(AUTHORITY, SyncStatusTable.TABLE_NAME, VMA_SYNC_STATUS);

        uriMatcher.addURI(AUTHORITY, SyncStatusTable.TABLE_NAME + "/#", VMA_SYNC_STATUS_ID);

        uriMatcher.addURI(AUTHORITY, LinkedVMADevices.TABLE_NAME, VMA_LINKED_DEVICES);
        uriMatcher.addURI(AUTHORITY, RecentlyUsedFwdAddr.TABLE_NAME, VMA_FWD_ADDR);
        uriMatcher.addURI(AUTHORITY, RecentlyUsedReplyMsg.TABLE_NAME, VMA_REPLY_MSG);

        uriMatcher.addURI(AUTHORITY, SyncItem.TABLE_NAME, VMA_EVENTS);
        uriMatcher.addURI(AUTHORITY, SyncItem.TABLE_NAME + "/#", VMA_EVENTS_ID);

        uriMatcher.addURI(AUTHORITY, VMAMapping.TABLE_NAME, VMA_SYNC_MAPPING);
        uriMatcher.addURI(AUTHORITY, VMAMapping.TABLE_NAME + "/#", VMA_SYNC_MAPPING_ID);
        uriMatcher.addURI(AUTHORITY, VMAMapping.TABLE_NAME + "/uid/#", VMA_SYNC_MAPPING_UID);

        uriMatcher.addURI(AUTHORITY, AcraReports.TABLE_NAME, ACRA_REPORTS);
        uriMatcher.addURI(AUTHORITY, AcraReports.TABLE_NAME + "/#", ACRA_REPORTS_ID);

        // Sync Status
        syncStatusProjection = new HashMap<String, String>();
        syncStatusProjection.put(SyncStatusTable.MAX_UID, SyncStatusTable.MAX_UID);
        syncStatusProjection.put(SyncStatusTable.MAX_MOD_SEQUENCE, SyncStatusTable.MAX_MOD_SEQUENCE);
        syncStatusProjection.put(SyncStatusTable.MIN_UID, SyncStatusTable.MIN_UID);
        syncStatusProjection.put(SyncStatusTable.MIN_MOD_SEQUENCE, SyncStatusTable.MIN_MOD_SEQUENCE);
        syncStatusProjection.put(SyncStatusTable.SYNC_MODE, SyncStatusTable.SYNC_MODE);
        syncStatusProjection.put(SyncStatusTable.PROCESSED_MAX_MODSEQ, SyncStatusTable.PROCESSED_MAX_MODSEQ);

        // Setting default projection
        settingDefaultProjection = new HashMap<String, String>();
        settingDefaultProjection.put(Settings._ID, Settings._ID);
        settingDefaultProjection.put(Settings._KEY, Settings._KEY);
        settingDefaultProjection.put(Settings._VALUE, Settings._VALUE);

        // Sync Events
        vmaEventsProjection = new HashMap<String, String>();
        vmaEventsProjection.put(SyncItem._ID, SyncItem._ID);
        vmaEventsProjection.put(SyncItem._ITEM_ID, SyncItem._ITEM_ID);
        vmaEventsProjection.put(SyncItem._TYPE, SyncItem._TYPE);
        vmaEventsProjection.put(SyncItem._ACTION, SyncItem._ACTION);
        vmaEventsProjection.put(SyncItem._PRIORITY, SyncItem._PRIORITY);
        vmaEventsProjection.put(SyncItem._RETRY_COUNT, SyncItem._RETRY_COUNT);
        vmaEventsProjection.put(SyncItem._LAST_PRIORITY, SyncItem._LAST_PRIORITY);

        deviceProjection = new HashMap<String, String>();
        deviceProjection.put(LinkedVMADevices._DEVICE_ID, LinkedVMADevices._DEVICE_ID);
        deviceProjection.put(LinkedVMADevices._NAME, LinkedVMADevices._NAME);
        deviceProjection.put(LinkedVMADevices._TIME, LinkedVMADevices._TIME);

        autoFwdProjection = new HashMap<String, String>();
        autoFwdProjection.put(AppSettings.KEY_VMA_AUTOFORWARDUSEDADDR,
                AppSettings.KEY_VMA_AUTOFORWARDUSEDADDR);

        autoReplyProjection = new HashMap<String, String>();
        autoReplyProjection.put(AppSettings.KEY_VMA_AUTOREPLYUSEDMSGS, AppSettings.KEY_VMA_AUTOREPLYUSEDMSGS);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate() {
        helper = AppDatabaseHelper.getInstance(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
        return false;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long rowId;
        Uri result = null;
        switch (uriMatcher.match(uri)) {
        case SETTINGS_MATCH_INDEX:
            rowId = db.insert(Settings.TABLE_NAME, null, values);
            if (rowId > 0) {
                Uri noteUri = ContentUris.withAppendedId(Settings.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(noteUri, null);
                return noteUri;
            }
            break;
        case VMA_SYNC_STATUS:
            rowId = db.insert(SyncStatusTable.TABLE_NAME, null, values);
            if (rowId > 0) {
                result = ContentUris.withAppendedId(SyncStatusTable.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(result, null);
            }
            break;
        case VMA_SYNC_MAPPING:
            if (!values.containsKey(VMAMapping._TIME_CREATED)) {
                values.put(VMAMapping._TIME_CREATED, System.currentTimeMillis());
            }
            rowId = db.insert(VMAMapping.TABLE_NAME, null, values);
            if (rowId > 0) {
                result = ContentUris.withAppendedId(VMAMapping.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(result, null);
            }
            break;
        case VMA_EVENTS:
            rowId = db.insert(SyncItem.TABLE_NAME, null, values);
            if (rowId > 0) {
                result = ContentUris.withAppendedId(SyncItem.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(result, null);
            }
            break;
        case ACRA_REPORTS:
            rowId = db.insert(AcraReports.TABLE_NAME, null, values);
            if (rowId > 0) {
                result = ContentUris.withAppendedId(AcraReports.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(result, null);
            }
            break;
        case VMA_LINKED_DEVICES:
            rowId = db.insert(LinkedVMADevices.TABLE_NAME, null, values);
            if (rowId > 0) {
                result = ContentUris.withAppendedId(LinkedVMADevices.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(result, null);
            }
            break;
        case VMA_FWD_ADDR:
            rowId = db.insert(RecentlyUsedFwdAddr.TABLE_NAME, null, values);
            if (rowId > 0) {
                result = ContentUris.withAppendedId(RecentlyUsedFwdAddr.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(result, null);
            }
            break;
        case VMA_REPLY_MSG:
            rowId = db.insert(RecentlyUsedReplyMsg.TABLE_NAME, null, values);
            if (rowId > 0) {
                result = ContentUris.withAppendedId(RecentlyUsedReplyMsg.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(result, null);
            }
            break;

        default:
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(ApplicationProvider.class, "Unknow URI .... throwing IllegalArgumentException");
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return result;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
        case SETTINGS_MATCH_INDEX:
            if (selection == null) {
                count = db.delete(Settings.TABLE_NAME, null, null);
            } else {
                count = db.delete(Settings.TABLE_NAME, selection, selectionArgs);

            }
            break;
        case VMA_SYNC_STATUS:
            count = db.delete(SyncStatusTable.TABLE_NAME, selection, selectionArgs);
            break;
        case VMA_SYNC_STATUS_ID:
            count = db.delete(SyncStatusTable.TABLE_NAME, selection, selectionArgs);
            break;
        case VMA_SYNC_MAPPING:
            count = db.delete(VMAMapping.TABLE_NAME, selection, selectionArgs);
            break;
        case VMA_LINKED_DEVICES:
            count = db.delete(LinkedVMADevices.TABLE_NAME, selection, selectionArgs);
            break;
        case VMA_REPLY_MSG:
            count = db.delete(RecentlyUsedReplyMsg.TABLE_NAME, selection, selectionArgs);
            break;
        case VMA_FWD_ADDR:
            count = db.delete(RecentlyUsedFwdAddr.TABLE_NAME, selection, selectionArgs);
            break;
        case VMA_SYNC_MAPPING_ID:
            selection = VMAMapping._ID + "=" + uri.getPathSegments().get(1);
            count = db.delete(VMAMapping.TABLE_NAME, selection, selectionArgs);
            break;
        case VMA_EVENTS:
            count = db.delete(SyncItem.TABLE_NAME, selection, selectionArgs);
            break;
        case ACRA_REPORTS:
            count = db.delete(AcraReports.TABLE_NAME, selection, selectionArgs);
            break;
        case VMA_EVENTS_ID:
            selection = SyncItem._ID + "=" + uri.getPathSegments().get(1);
            count = db.delete(SyncItem.TABLE_NAME, selection, selectionArgs);
            break;
        case ACRA_REPORTS_ID:
            selection = AcraReports._ID + "=" + uri.getPathSegments().get(1);
            count = db.delete(AcraReports.TABLE_NAME, selection, selectionArgs);
            break;
        default:
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(ApplicationProvider.class, "Unknow URI .... throwing IllegalArgumentException");
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String,
     * java.lang.String[], java.lang.String)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (uriMatcher.match(uri)) {
        case SETTINGS_MATCH_INDEX:
            qb.setTables(Settings.TABLE_NAME);
            if (projection == null) {
                // Setting default projection
                qb.setProjectionMap(settingDefaultProjection);
            }
            break;
        case ACRA_REPORTS:
            qb.setTables(AcraReports.TABLE_NAME);
            if (projection == null) {
                // Setting default projection
                qb.setProjectionMap(AcraReports.DEFAULT_PROJECTION);
            }
            break;
        case ACRA_REPORTS_ID:
            selection = AcraReports._ID + "=" + uri.getPathSegments().get(1);
            qb.setTables(AcraReports.TABLE_NAME);
            // Setting default projection
            if (projection == null) {
                qb.setProjectionMap(AcraReports.DEFAULT_PROJECTION);
            }
            break;
        case VMA_SYNC_STATUS:
            qb.setTables(SyncStatusTable.TABLE_NAME);
            if (projection == null) {
                // Setting default projection
                qb.setProjectionMap(syncStatusProjection);
            }
            break;
        case VMA_SYNC_MAPPING:
            qb.setTables(VMAMapping.TABLE_NAME);
            // Setting default projection
            if (projection == null) {
                qb.setProjectionMap(VMAMapping.defaultProjection);
            }
            break;
        case VMA_SYNC_MAPPING_ID:
            selection = VMAMapping._ID + "=" + uri.getPathSegments().get(1);
            qb.setTables(VMAMapping.TABLE_NAME);
            // Setting default projection
            if (projection == null) {
                qb.setProjectionMap(VMAMapping.defaultProjection);
            }
            break;
        case VMA_SYNC_MAPPING_UID:
            selection = VMAMapping._UID + "=" + uri.getPathSegments().get(1);
            qb.setTables(VMAMapping.TABLE_NAME);
            // Setting default projection
            if (projection == null) {
                qb.setProjectionMap(VMAMapping.defaultProjection);
            }
            break;
        case VMA_EVENTS:
            qb.setTables(SyncItem.TABLE_NAME);
            // Setting default projection
            if (projection == null) {
                qb.setProjectionMap(vmaEventsProjection);
            }
            break;
        case VMA_LINKED_DEVICES:
            qb.setTables(LinkedVMADevices.TABLE_NAME);
            // Setting default projection
            if (projection == null) {
                qb.setProjectionMap(deviceProjection);
            }
            break;
        case VMA_FWD_ADDR:
            qb.setTables(RecentlyUsedFwdAddr.TABLE_NAME);
            // Setting default projection
            if (projection == null) {
                qb.setProjectionMap(autoFwdProjection);
            }
            break;
        case VMA_REPLY_MSG:
            qb.setTables(RecentlyUsedReplyMsg.TABLE_NAME);
            // Setting default projection
            if (projection == null) {
                qb.setProjectionMap(autoReplyProjection);
            }
            break;
        default:
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(ApplicationProvider.class, "Unknow URI .... Retunring Null");
            }
            return null;
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues,
     * java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int count;
        switch (uriMatcher.match(uri)) {
        case SETTINGS_MATCH_INDEX:
            count = db.update(Settings.TABLE_NAME, values, selection, selectionArgs);
            break;
        case ACRA_REPORTS:
            count = db.update(AcraReports.TABLE_NAME, values, selection, selectionArgs);
            break;
        case ACRA_REPORTS_ID:
            selection = AcraReports._ID + "=" + uri.getPathSegments().get(1);
            count = db.update(AcraReports.TABLE_NAME, values, selection, selectionArgs);
            break;
        case VMA_SYNC_STATUS:
            count = db.update(SyncStatusTable.TABLE_NAME, values, selection, selectionArgs);
            break;
        case VMA_SYNC_MAPPING:
            // if(!values.containsKey(VMAMapping._TIME_UPDATED)){
            values.put(VMAMapping._TIME_UPDATED, System.currentTimeMillis());
            // }
            count = db.update(VMAMapping.TABLE_NAME, values, selection, selectionArgs);
            break;
        case VMA_SYNC_MAPPING_ID:
            selection = VMAMapping._ID + "=" + uri.getPathSegments().get(1);
            if (!values.containsKey(VMAMapping._TIME_UPDATED)) {
                values.put(VMAMapping._TIME_UPDATED, System.currentTimeMillis());
            }
            count = db.update(VMAMapping.TABLE_NAME, values, selection, selectionArgs);
            break;
        case VMA_EVENTS:
            count = db.update(SyncItem.TABLE_NAME, values, selection, selectionArgs);
            break;
        case VMA_EVENTS_ID:
            selection = SyncItem._ID + "=" + uri.getPathSegments().get(1);
            count = db.update(SyncItem.TABLE_NAME, values, selection, selectionArgs);
            break;
        case VMA_LINKED_DEVICES:
            count = db.update(LinkedVMADevices.TABLE_NAME, values, selection, selectionArgs);
            break;
        case VMA_FWD_ADDR:
            count = db.update(RecentlyUsedFwdAddr.TABLE_NAME, values, selection, selectionArgs);
            break;
        case VMA_REPLY_MSG:
            count = db.update(RecentlyUsedReplyMsg.TABLE_NAME, values, selection, selectionArgs);
            break;
        default:
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(ApplicationProvider.class, "Unknow URI .... throwing IllegalArgumentException");
            }
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

}
