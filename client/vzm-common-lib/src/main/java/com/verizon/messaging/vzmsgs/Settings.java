/**
 * Settings.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs;

import com.verizon.messaging.vzmsgs.provider.ApplicationProvider;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Apr 27, 2012
 */
public class Settings implements BaseColumns {

    public static final String TABLE_NAME = "settings";
    public static String _KEY = "key";
    public static String _VALUE = "value";
    public static final Uri CONTENT_URI = Uri.parse("content://" + ApplicationProvider.AUTHORITY + "/"
            + TABLE_NAME);
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/";
}
