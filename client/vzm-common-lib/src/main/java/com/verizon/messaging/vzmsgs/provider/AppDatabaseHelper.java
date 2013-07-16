/**
 * DatabaseHelper.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

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
 * @author Jegadeesan M
 * @Since Nov 6, 2012
 */
public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static AppDatabaseHelper instance;

    public static synchronized AppDatabaseHelper getInstance(Context context, String name,
            CursorFactory factory, int version) {
        if (instance == null) {
            instance = new AppDatabaseHelper(context, name, factory, version);
        }
        return instance;
    }

    /**
     * @param context
     * @param name
     * @param factory
     * @param version
     *            Constructor
     */
    private AppDatabaseHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        if (Logger.IS_INFO_ENABLED) {
            Logger.info(AppDatabaseHelper.class, " Creating a tables");
        }
        // App settings problem 
        db.execSQL(createSettingsTable());
        db.execSQL(createACRATable());
        
        db.execSQL(createVmaDevicesTable());
        db.execSQL(createVmaFwdAddrTable());
        db.execSQL(createVmaReplyMsgTable());

        // VMA Sync Table
        db.execSQL(createVmaSyncMappingTable());
        db.execSQL(createSyncItemsTable());
        db.execSQL(createSyncStatusTable());
        db.execSQL(createIndex());

    }
    
    
    /**
     * This Method 
     * @param db
     */
    private void dropAllTable(SQLiteDatabase db) {
        if (Logger.IS_INFO_ENABLED) {
            Logger.info(AppDatabaseHelper.class, " drop All tables.");
        }
        db.execSQL("DROP TABLE IF EXISTS "+ Settings.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS "+ LinkedVMADevices.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS "+ RecentlyUsedFwdAddr.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS "+ RecentlyUsedReplyMsg.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS "+ VMAMapping.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS "+ SyncItem.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS "+ SyncStatusTable.TABLE_NAME);
        
    }

    private String createVmaReplyMsgTable() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE TABLE " + RecentlyUsedReplyMsg.TABLE_NAME + " (");
        buffer.append("'" + AppSettings.KEY_VMA_AUTOREPLYUSEDMSGS + "' VARCHAR)");
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(AppDatabaseHelper.class, " Creating Vma Reply Message table:" + buffer.toString());
        }
        return buffer.toString();
    }

    private String createVmaFwdAddrTable() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE TABLE " + RecentlyUsedFwdAddr.TABLE_NAME + " (");
        buffer.append("'" + AppSettings.KEY_VMA_AUTOFORWARDUSEDADDR + "' VARCHAR)");
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(AppDatabaseHelper.class, " Creating Vma Forward Address table:" + buffer.toString());
        }
        return buffer.toString();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int,
     * int)
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            if (Logger.IS_INFO_ENABLED) {
                Logger.info(AppDatabaseHelper.class, "Updating the table from " + newVersion + " to "
                        + oldVersion);
            }
            // 4.0.0 release  database version is 1 
            if(oldVersion==1){
                db.execSQL(upgradeVMAMappingTableQuery(db));
                db.execSQL(createACRATable());
            }else if (oldVersion == 2){
                db.execSQL(createACRATable());
            }
            
        } else {
            if (Logger.IS_INFO_ENABLED) {
                Logger.info(AppDatabaseHelper.class, "Downgrading the table from " + newVersion + " to "
                        + oldVersion);
            }
            dropAllTable(db);
            onCreate(db);
        }
    }



    /**
     * This Method 
     * @param db
     */
    private String upgradeVMAMappingTableQuery(SQLiteDatabase db) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("ALTER TABLE " + VMAMapping.TABLE_NAME + " ADD COLUMN ");
        buffer.append("'"+ VMAMapping._OLD_LUID + "' INTEGER DEFAULT 0 ;");
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(AppDatabaseHelper.class, " alterMappingTableQuery:" + buffer.toString());
        }
        return buffer.toString();
    }

    /**
     * This Method is used to create the Sync Status table Query.
     * 
     * @return {@link String}
     */
    private String createSyncStatusTable() {
        // CREATE TABLE "main"."sync_status" ("min_uid" INTEGER, "min_mod_seq" INTEGER, "max_uid" INTEGER,
        // "max_mod_seq" INTEGER, "sync_mod" INTEGER)
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE  TABLE " + SyncStatusTable.TABLE_NAME + " (");
        buffer.append("'" + SyncStatusTable.MIN_UID + "' INTEGER,");
        buffer.append("'" + SyncStatusTable.MIN_MOD_SEQUENCE + "' INTEGER,");
        buffer.append("'" + SyncStatusTable.MAX_UID + "' INTEGER,");
        buffer.append("'" + SyncStatusTable.MAX_MOD_SEQUENCE + "' INTEGER,");
        buffer.append("'" + SyncStatusTable.SYNC_MODE + "' INTEGER DEFAULT -1,");
        buffer.append("'" + SyncStatusTable.PROCESSED_MAX_MODSEQ + "' INTEGER DEFAULT -1)");
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(AppDatabaseHelper.class, " Creating Vma SyncStatusTable table:" + buffer.toString());
        }
        return buffer.toString();
    }

    /**
     * This Method is used to create the MsgInfoTable table for the Vma Sync . Its holds the local LUID and
     * remote UID.
     * 
     * @return {@link String}
     */
    private String createVmaSyncMappingTable() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE TABLE " + VMAMapping.TABLE_NAME + " (");
        buffer.append("'" + VMAMapping._ID + "' INTEGER PRIMARY KEY AUTOINCREMENT  NOT NULL,");
        buffer.append("'" + VMAMapping._TIME_CREATED + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._TIME_UPDATED + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._SOURCE_CREATED_FROM + "' INTEGER DEFAULT 0, ");
        buffer.append("'" + VMAMapping._TYPE + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._LUID + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._THREAD_ID + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._MSGID + "'  VARCHAR(128),");
        buffer.append("'" + VMAMapping._UID + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._TIMEOFMESSAGE + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._SMSCHECKSUM + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._VMAFLAGS + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._PENDING_UI_EVENTS + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._SOURCE + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._MSG_BOX + "' INTEGER DEFAULT 0,");
        buffer.append("'" + VMAMapping._OLD_LUID + "' INTEGER DEFAULT 0,");
        buffer.append(" UNIQUE ('" + VMAMapping._UID + "','" + VMAMapping._LUID + "','");
        buffer.append(VMAMapping._MSGID + "','" + VMAMapping._TYPE + "'");
        buffer.append(" ) ON CONFLICT REPLACE )");
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(AppDatabaseHelper.class, " Creating Vma Sync Mapping table:" + buffer.toString());
        }
        return buffer.toString();
    }

    private String createVmaDevicesTable() {
        // CREATE TABLE "main"."sync_status" ("min_uid" INTEGER, "min_mod_seq" INTEGER, "max_uid" INTEGER,
        // "max_mod_seq" INTEGER, "sync_mod" INTEGER)
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE TABLE " + LinkedVMADevices.TABLE_NAME + " (");
        buffer.append("'" + LinkedVMADevices._DEVICE_ID + "' VARCHAR,");
        buffer.append("'" + LinkedVMADevices._NAME + "' VARCHAR,");
        buffer.append("'" + LinkedVMADevices._TIME + "' INTEGER,");
        buffer.append(" UNIQUE ('" + LinkedVMADevices._DEVICE_ID + "','" + LinkedVMADevices._NAME + "','");
        buffer.append(LinkedVMADevices._TIME + "'");
        buffer.append(" ) ON CONFLICT REPLACE )");
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(AppDatabaseHelper.class, " Creating Vma Linked device table:" + buffer.toString());
        }
        return buffer.toString();
    }

    private String createSyncItemsTable() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE TABLE " + SyncItem.TABLE_NAME + " (");
        buffer.append("'" + SyncItem._ID + "' INTEGER PRIMARY KEY AUTOINCREMENT  NOT NULL,");
        buffer.append("'" + SyncItem._ITEM_ID + "' INTEGER,");
        buffer.append("'" + SyncItem._TYPE + "' INTEGER,");
        buffer.append("'" + SyncItem._ACTION + "' INTEGER ,");
        buffer.append("'" + SyncItem._PRIORITY + "' INTEGER ,");
        buffer.append("'" + SyncItem._LAST_PRIORITY + "' INTEGER ,");
        buffer.append("'" + SyncItem._RETRY_COUNT + "' INTEGER ,");
        buffer.append(" UNIQUE ('" + SyncItem._ITEM_ID + "','" + SyncItem._TYPE + "','" + SyncItem._PRIORITY);
        buffer.append("','" + SyncItem._ACTION + "'");
        buffer.append(" ) ON CONFLICT REPLACE )");
        // }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(AppDatabaseHelper.class, " Creating sync items table:" + buffer.toString());
        }
        return buffer.toString();
    }

    private String createIndex() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE INDEX IDXMAP_LUID ON " + VMAMapping.TABLE_NAME + "( " + VMAMapping._LUID
                + " );");
        buffer.append("CREATE INDEX IDXMAP_UID ON " + VMAMapping.TABLE_NAME + "( " + VMAMapping._UID
                + " );");
        buffer.append("CREATE INDEX IDXMAP_MSGID ON " + VMAMapping.TABLE_NAME + "( " + VMAMapping._MSGID
                + " );");
        buffer.append("CREATE INDEX IDXMAP_CHECKSUM ON " + VMAMapping.TABLE_NAME + "( "
                + VMAMapping._SMSCHECKSUM + " );");
        

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(AppDatabaseHelper.class, " Creating indexing for mapping table:" + buffer.toString());
        }
        return buffer.toString();
    }

    private String createSettingsTable() {
        // CREATE TABLE "app_settings" ("_id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL , "key" VARCHAR(25)
        // NOT NULL , "value" VARCHAR(255))
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE TABLE " + Settings.TABLE_NAME + "(");
        buffer.append("'" + Settings._ID + "' INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL ,");
        buffer.append("'" + Settings._KEY + "'  VARCHAR(25) NOT NULL ,");
        buffer.append("'" + Settings._VALUE + "'  VARCHAR(255) ,");
        buffer.append("UNIQUE ('" + Settings._KEY + "') ON CONFLICT REPLACE )");

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(AppDatabaseHelper.class, "Settings :" + buffer.toString());
        }
        return buffer.toString();
    }
    
    
    private String createACRATable() {
        // CREATE TABLE "app_settings" ("_id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL , "key" VARCHAR(25)
        // NOT NULL , "value" VARCHAR(255))
        StringBuffer buffer = new StringBuffer();
        buffer.append("CREATE TABLE " + AcraReports.TABLE_NAME + "(");
        buffer.append("'" + AcraReports._ID + "' INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL ,");
        buffer.append("'" + AcraReports._CHECK_SUM + "'  INTEGER DEFAULT 0,");
        buffer.append("'" + AcraReports._REPORT_DATETIME + "'  INTEGER DEFAULT 0,");
        buffer.append("'" + AcraReports._REPORT_STATUS + "'  INTEGER DEFAULT 0,");
        buffer.append("'" + AcraReports._ERROR_MSG + "'  VARCHAR,");
        buffer.append("UNIQUE ('" + AcraReports._CHECK_SUM + "') ON CONFLICT REPLACE )");
        
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(AppDatabaseHelper.class, "ACRA :" + buffer.toString());
        }
        return buffer.toString();
    }


}
