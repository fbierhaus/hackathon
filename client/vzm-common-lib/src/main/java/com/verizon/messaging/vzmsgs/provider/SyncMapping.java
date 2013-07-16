/**
 * LocalMapping.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs.provider;

import android.net.Uri;

/**
 * This class is used to map the local database and server columns.
 * 
 * @author Jegadeesan M
 * @Since Dec 5, 2012
 */
public class SyncMapping {
    public static final String TABLE_NAME = "local_mapping";
    public static final Uri CONTENT_URI = Uri.parse("content://" + ApplicationProvider.AUTHORITY + "/"
            + TABLE_NAME);
    // local Mapping info
    // local unique id
    public static final String _LUID = "luid";
    // Message type
    public static final String _TYPE = "type";
    // Conversation id
    public static final String _THREAD_ID = "threadId";
    //
    public static final String _SMSC_CHECKSUM = "smsc_checksum";
    public static final String _SMSC_TIMESTAMP = "smsc_timestamp";
    // Server Mapping info
    public static final String _UID = "uid";
    public static final String _VMA_ID = "vma_Id";
    public static final String _VMA_CHECKSUM = "vma_checksum";
    public static final String _VMA_TIMESTAMP = "vma_timestamp";

    // Common columns
    public static final String _DELETED = "deleted";
    public static final String _SOURCE = "src";

    public enum MessageType {
        UNKNOWN,SMS, MMS, CONVERSATION
    }

    public enum MessageSource {
        PHONE, VMA, UNKNOWN
    }

    public int luid;
    public MessageType type;
    public int threadId;
    public int smschecksum;
    public int smstimestamp;

    public int uid;
    public String vmaId;
    public int vmachecksum;
    public int vmatimestamp;

    public MessageSource source;
    public boolean deleted;

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("luid=" + luid);
        builder.append(",threadId=" + threadId);
        builder.append(",uid=" + uid);
        builder.append(",vmaId=" + vmaId);
        builder.append(",type=" + type);
        builder.append(",smschecksum=" + smschecksum);
        builder.append(",smstimestamp=" + smstimestamp);
        builder.append(",vmachecksum=" + vmachecksum);
        builder.append(",vmatimestamp=" + vmatimestamp);
        builder.append(",src=" + source);
        builder.append(",deleted=" + deleted);
        return builder.toString();
    }

}
