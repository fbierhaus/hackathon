/**
 * SyncConstants.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.common;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jun 17, 2012
 */
public final class SyncConstants {
    /**
     * Used to notify the new messages from vma server
     */
    public static final String INTENT_WIFI_SYNC_SERVICE = "vzm.wifi.sync.START";


    public static final String INTENT_VMA_SYNC_SERVICE = "vzm.wifi.sync.start";
    
    /*Adding Transaction service related constants here since they are genric*/
    public static final String TRANSACTION_TYPE = "type";
    public static final String URI = "uri";
    public static final int NOTIFICATION_TRANSACTION = 0;
    
    public static final String INTENT_START_PARING_SERVER = "vzm.wifi.pairing.server.START";
    public static final String INTENT_STOP_PARING_SERVER = "vzm.wifi.pairing.server.STOP";
    public static final String INTENT_PARING_STATUS = "vzm.wifi.pairing.STATUS";
    

    public static String DELETION_THREAD_ID = "delete_id";



    public static final String KEY_VMA_USERNAME = "vma.mdn";
    public static final String KEY_VMA_PASSWORD = "vma.pwd";

}
