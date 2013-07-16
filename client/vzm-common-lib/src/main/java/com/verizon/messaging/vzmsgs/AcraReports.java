/**
 * Settings.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs;

import java.util.Hashtable;

import com.verizon.messaging.vzmsgs.provider.ApplicationProvider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Apr 27, 2012
 */
public class AcraReports implements BaseColumns {

    public static final int REPORT_UNKNOWN = 0;
    public static final int REPORT_SENT = 1;
    public static final int REPORT_SEND_FAILED = 2;
    
    public static final String TABLE_NAME = "acrareports";
    public static String _CHECK_SUM = "checksum";
    public static String _REPORT_DATETIME = "report_datetime";
    public static String _ERROR_MSG = "error_msg";
    public static String _REPORT_STATUS = "report_status";
    public static final Uri CONTENT_URI = Uri.parse("content://" + ApplicationProvider.AUTHORITY + "/"
            + TABLE_NAME);
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/acra_report";
    public static Hashtable<String, String> DEFAULT_PROJECTION = new Hashtable<String, String>();
    static {
        DEFAULT_PROJECTION.put(_ID, _ID);
        DEFAULT_PROJECTION.put(_CHECK_SUM, _CHECK_SUM);
        DEFAULT_PROJECTION.put(_REPORT_DATETIME, _REPORT_DATETIME);
        DEFAULT_PROJECTION.put(_ERROR_MSG, _ERROR_MSG);
        DEFAULT_PROJECTION.put(_REPORT_STATUS, _REPORT_STATUS);
    }
}
