/**
 * SyncStatusCode.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs.sync;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Apr 1, 2013
 */
public interface SyncStatusCode {


    /**
     * Provisioning status
     * */

    public static final int VMA_PROVISION_GENERATE_PIN = 8400;
    public static final int VMA_PROVISION_VALIDATE_PIN = 8401;
    public static final int VMA_PROVISION_USER_QUERY = 8402;
    public static final int VMA_PROVISION_CREATE_MAILBOX = 8403;
    public static final int VMA_PROVISION_SYNC_ASSISTANT_SETTINGS = 8404;

    public static final int VMA_PROVISION_SUCCESS = 8405;
    public static final int VMA_PROVISION_FAILED = 8406;

    public static final int VMA_UNSUBSCRIBE = 8420;
    public static final int VMA_UNSUBSCRIBE_FAILED = 8421;
    
    public static final int VMA_AUTO_PROVISION_FAILED = 8422;
    public static final int VMA_AUTO_PROVISION_SUCCESS = 8423;

    /**
     * Notification updates
     */

    public static final int UPDATE_AUTO_REPLY_OR_FORWARD = 8500;

    public static final int VMA_SYNC_IDLE_LOGIN = 8510;
    public static final int VMA_SYNC_IDLING = 8511;
    public static final int VMA_SYNC_IDLE_RENEW = 8512;
    public static final int VMA_SYNC_IDLE_RELEASE = 8513;
    public static final int VMA_SYNC_IDLE_NO_NETWORK = 8514;
    public static final int VMA_SYNC_IDLE_LOGOUT = 8515;

    public static final int VMA_SYNC_FETCH_LOGIN = 8520;
    public static final int VMA_SYNC_FETCH_MSG = 8521;
    public static final int VMA_SYNC_FETCH_ATTACHEMENT = 8522;
    public static final int VMA_SYNC_FETCH_RELEASE = 8523;
    public static final int VMA_SYNC_FETCH_NO_NETWORK = 8524;
    public static final int VMA_SYNC_FETCH_LOGOUT = 8525;
    public static final int VMA_SYNC_FULLSYNC_LOGIN = 8526;
    public static final int VMA_SYNC_FULLSYNC_LOGOUT = 8527;

    public static final int VMA_SYNC_SEND_LOGIN = 8530;
    public static final int VMA_SYNC_SEND_SMS = 8531;
    public static final int VMA_SYNC_SEND_MMS = 8532;
    public static final int VMA_SYNC_SEND_READ = 8533;
    public static final int VMA_SYNC_SEND_DELETE = 8534;    
    public static final int VMA_SYNC_SEND_NO_NETWORK = 8535;
    public static final int VMA_SYNC_SEND_LOGOUT = 8536;

    public static final int VMA_SYNC_FETCH_CONVERSATION = 8540;
    public static final int VMA_SYNC_FETCH_UIDS = 8541;
    public static final int VMA_SYNC_FETCH_CHANGES = 8542;
    public static final int VMA_SYNC_NEW_MESSAGE = 8543;
    public static final int VMA_SYNC_FETCHING_ATTACHEMENTS = 8544;
    
    public static final int VMA_SYNC_CHECKSUM_BUILDER_START = 8545;
    public static final int VMA_SYNC_CHECKSUM_BUILDER_STOP = 8545;

}
