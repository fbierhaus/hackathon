/**
 * AppErrorCodes.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs.sync;

/**
 * This interface has the supported error codes on UI.
 * 
 * @author Jegadeesan M 
 * @Since Apr 1, 2013
 */
public interface AppErrorCodes {

    // VMA Error codes should be greater than 9xxx

    //VMA Provisioning error codes
    public static final int VMA_PROVISION_UNKNOWN = 9000;
    public static final int VMA_PROVISION_OK = 9001;
    public static final int VMA_PROVISION_NOTVZWMDN = 9002;
    public static final int VMA_PROVISION_NOTELIGIBLE = 9003;
    public static final int VMA_PROVISION_OVERLIMIT = 9004;
    public static final int VMA_PROVISION_ERROR = 9005;
    public static final int VMA_PROVISION_FAIL = 9006;
    public static final int VMA_PROVISION_SUSPENDED = 9007;
    public static final int VMA_PROVISION_VBLOCK = 9008;
    public static final int VMA_PROVISION_EXCEEDDEVICELIMIT = 9009;
    public static final int VMA_PROVISION_VMA_SUBSCRIBER = 9010;
    public static final int VMA_PROVISION_NOT_VMA_SUBCRIBER = 9011;
    public static final int VMA_PROVISION_INVALID_MDN = 9012;
    public static final int VMA_PROVISION_DUPLICATE_DESTINATION = 9013;
    public static final int VMA_PROVISION_ALREADY_AUTO_FORWARDED = 9014;
    public static final int VMA_PROVISION_INVALID_END_DATE = 9015;
    public static final int VMA_PROVISION_NETWORK_ERROR = 9016;
    public static final int VMA_PROVISION_INTERNAL_SERVER_ERROR = 9017;
    public static final int VMA_PROVISION_PIN_RETRIEVAL_FAILED = 9018;
    public static final int VMA_PROVISION_LOGIN_FAIL = 9019;
    public static final int VMA_PROVISION_UNSUBSCRIBE_ERROR = 9020;
    public static final int VMA_PROVISION_NOTEXT = 9021;
    public static final int VMA_PROVISION_NOMMS = 9022;
    public static final int VMA_REMOVE_DEVICE_ERROR = 9023;
    

    // VMA Sync Error codes
    public static final int VMA_SYNC_MISSING_LOGIN_OR_PASSWORD = 9400;
    public static final int VMA_SYNC_LOGIN_FAILED = 9401;
    public static final int VMA_SYNC_SESSION_WRONG_STATE = 9402;
    // NO IDLE [403] Another command already in progress
    public static final int VMA_SYNC_ANOTHER_COMMAND_ALREADY_IN_PROGRESS = 9403;
    // NO SELECT [408] Mailbox does not exist
    public static final int VMA_SYNC_MAILBOX_DOES_NOT_EXIST = 9408;
    public static final int VMA_SYNC_OTHER_PERMANENT_FAILURE = 9499;
    public static final int VMA_SYNC_NOT_A_VMA_SUBSCRIBER = 9450;
    public static final int VMA_SYNC_HAS_SMS_BLOCKING = 9451;
    public static final int VMA_SYNC_ACCOUNT_SUSPENDED = 9452;
    public static final int VMA_SYNC_FAILED_ANTISPAM_CHECK = 9454;
    public static final int VMA_SYNC_ISTD_NOT_SUPPORTED = 9460;
    // NO [455] All destination MDNs blocked by Usage Control
    public static final int VMA_SYNC_ALLDESTINATION_MDNS_BLOCKED_BY_USAGE_CONTROL = 9455;
    // NO [456] UC limit reached
    public static final int VMA_SYNC_UC_LIMIT_REACHED = 9456;
    // NO [453] Subscriber has MMS blocking
    public static final int VMA_SYNC_SUBSCRIBER_HAS_MMS_BLOCKING = 9453;
    // NO [457] UC system error
    public static final int VMA_SYNC_UC_SYSTEM_ERROR = 9457;
    // NO [458] Subscriber has insufficient funds
    public static final int VMA_SYNC_SUBSCRIBER_HAS_INSUFFICIENT_FUNDS = 9458;
    
    public static final int VMA_LOW_MEMORY = 9900;
    

    // TELEPHONEY error codes
    // RCS Error codes

}
