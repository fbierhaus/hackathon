/**
 * Vma.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs.provider;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

/**
 * This is the constant class for VMA.
 * 
 * @author Jegadeesan.M
 * @Since Jun 13, 2012
 */
public final class Vma {
    public final static class SyncStatusTable {

        public static final String TABLE_NAME = "syncstatus";
        public static final Uri CONTENT_URI = Uri.parse("content://" + ApplicationProvider.AUTHORITY + "/"
                + TABLE_NAME);

        /**
         * This column is used to identified the last synced UID;
         */
        public static final String MIN_UID = "min_uid";
        public static final String MIN_MOD_SEQUENCE = "min_mod_seq";
        public static final String MAX_UID = "max_uid";
        public static final String MAX_MOD_SEQUENCE = "max_mod_seq";
        public static final String SYNC_MODE = "syncmode";
        public static final String PROCESSED_MAX_MODSEQ = "procesed_maxmodseq";

    }
    
    public final static class RecentlyUsedFwdAddr{
    	 public static final String TABLE_NAME = "fwd_address";
         public static final Uri CONTENT_URI = Uri.parse("content://" + ApplicationProvider.AUTHORITY + "/"
                 + TABLE_NAME);
    }
    
    public final static class RecentlyUsedReplyMsg{
   	    public static final String TABLE_NAME = "reply_address";
        public static final Uri CONTENT_URI = Uri.parse("content://" + ApplicationProvider.AUTHORITY + "/"
                + TABLE_NAME);
   }

    public final static class LinkedVMADevices {
        public static final String TABLE_NAME = "linked_devices";
        public static final Uri CONTENT_URI = Uri.parse("content://" + ApplicationProvider.AUTHORITY + "/"
                + TABLE_NAME);
        public static final String _DEVICE_ID = "id";
        public static final String _NAME = "name";
        public static final String _TIME = "time";
        private static final String JKEY_CREATE_TIME = "createTime";
        private static final String JKEY_DEVICE_ID = "deviceId";
        private static final String JKEY_DEVICE_NAME = "deviceName";
        // {"deviceId":"815d2699035a1","deviceName":"Motorola-MZ609","createTime":"Fri, 9 Nov 2012 13:03:53 +0000 (GMT)"},
        public String deviceId;
        public String deviceName;
        public String createTime;

        /**
         * 
         * Constructor
         * 
         * @throws JSONException
         */
        public LinkedVMADevices(JSONObject object) throws JSONException {
            if (object.has(JKEY_DEVICE_ID)) {
                deviceId = object.getString(JKEY_DEVICE_ID);
            }
            if (object.has(JKEY_DEVICE_NAME)) {
                deviceName = object.getString(JKEY_DEVICE_NAME);
            }
            if (object.has(JKEY_CREATE_TIME)) {
                createTime = object.getString(JKEY_CREATE_TIME);
            }
        }

		public LinkedVMADevices(String device_id, String device_Name,
				String createdTime) {
			deviceId = device_id ;
			deviceName = device_Name ;
			createTime = createdTime;
		}
    }

}
