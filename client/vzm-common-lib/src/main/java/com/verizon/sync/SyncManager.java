/**
 * SyncManager.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.sync;

import android.os.Binder;

import com.verizon.common.VZUris;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jul 3, 2012
 */
public abstract class SyncManager extends Binder {

    public static final String ACTION_SYNC_STATUS = "vzm.sync.STATUS";

    public static final String ACTION_START_VMA_SYNC = "vzm.sync.START";
//    public static final String ACTION_APP_ON_FOREGROUND = "vzm.sync.APP_ON_FOREGROUND";
    public static final String ACTION_STOP_VMA_SYNC = "vzm.sync.STOP";
    public static final String ACTION_DELETE_OLD_MESSAGE = "vzm.sync.DELETE_OLD_MESSAGES";
    public static final String ACTION_SEND_MSG = "vzm.msg.SEND";
    public static final String ACTION_UPDATE_SETTINGS = "vzm.sync.update.settings";

    public static final String ACTION_SMS_SENT = "vzm.msg.sms.SENT";
    public static final String ACTION_MMS_SENT = "vzm.msg.mms.SENT";
    public static final String ACTION_MSG_DELETED = "vzm.msg.DELETED";
    public static final String ACTION_XCONV_DELETED = "vzm.xconv.DELETED";
    public static final String ACTION_MARK_AS_READ = "vzm.mark.as.READ";
    public static final String ACTION_START_PROVISIONING = "vzm.provisioning.START";
    public static final String ACTION_REGISTER_GCM_SERVICE = "vzm.vma.publish.GCM_TOKEN";

    public static final String MESSAGE_COUNT_CHANGED = "com.verizon.widget.MESSAGE_COUNT_CHANGED";
    public static final String UNREAD_MESSAGE_COUNT = "com.verizon.widget.UNREAD_MESSAGE_COUNT";

    public static final String ACTION_VMA_SYNC = "vzm.sync.START";



    /**
     * The MSA allows messages that belong to additional groups to exist in the mailbox but only supports
     * retrieval of the most recent 2000. These participant groups are retrieved in maximum number of 50 per
     * request.
     */
    protected static final String SENDER_ID = "135713301837";

    public static final String C2DM_KEY = "c2dmKey";

    public interface StatusListner {
        public void updateStatus(int status);
    }

    public static final int NO_WIFI_CONNECTION = 108;
    public static final int NO_DATA_CONNECTION = 109;


    // Sync completed
    public static final int SYNC_STATUS_SYNC_COMPLETED = 11;
    public static final int SYNC_STATUS_FAILED = 12;
    public static final int SYNC_STATUS_LOGIN_FAILED = 13;
    public static final int SYNC_STATUS_PROVISIONING_RESULT = 14;
    public static final int SYNC_STATUS_GCM_TOKEN_FAILED = 15;
    public static final int SYNC_SHOW_NEW_NOTIFICATION = 9;
    public static final int CLEAR_SHOWN_NOTIFICATION = 10;
    public static final int SYNC_SHOW_NONBLOCKING_NOTIFICATION = 16;
    public static final int SYNC_PROVISIONING_COMPLETED = 17;
    public static final int FINISH_POP_UP = 18;
    public static final int HANDSET_PROVISIONING_BY_INTERCEPTING_MSG = 1800;
    
    /**
     * VMA Server errorcodes
     */


    /**
     * Provi
     */

    public static final String EXTRA_URI = "vma.uri";
    public static final String EXTRA_ERROR_CODE = "errorCode";
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_MESSAGE_SENT_SEND_NEXT = "SendNextMsg";

    public static final String EXTRA_XCONV_COUNT = "x-conv-count";
    public static final String EXTRA_XCONV_RECEIVED_COUNT = "x-conv-received-count";

    public static final String EXTRA_RECEIVING_COUNT = "x-receiving";
    public static final String EXTRA_SENDING_COUNT = "x-sending";
    public static final String EXTRA_TOTAL_COUNT = "x-total";
    public static final String EXTRA_STATUS = "x-status";
    public static final String EXTRA_KEY = "x-key";
    public static final String EXTRA_ACTION = "x-action";
    public static final String EXTRA_PAIR_RESULT = "x-pair-result";
    public static final String EXTRA_INSERT_URI = "x-insert-uri";

    // Mapping table
    public static final String EXTRA_LUID = "x-luid";
    public static final String EXTRA_MOD_TYPE = "x-modtype";
    public static final String EXTRA_WIFI_NETWORK_NAME = "x-networkname";

    // Flag to notify observers after a cache flush
    public static final String EXTRA_NOTIFY = "vzm-x-notify";
    public static final String EXTRA_GCM_REGISTRATION_ID = "x-gcm-regId";
    public static final String EXTRA_SYNC_TYPE = "x-sync-type";
    public static final int SYNC_NORMAL = 0;
    public static final int SYNC_ON_DEMAND = 1;
    public static final int SYNC_MANUAL = 2;

    public static final int CLEAR_NOTIFICATION = 99999;

    public static final String ACTION_CLEAR_READ_MSG_POPUP = "com.verizon.mms.ui.popup.CLEAR_READ_MSG";
    
    
 
    protected boolean isTabletDevice;
    protected String mdn;
    protected String authToken;

    /**
     * 
     * Constructor
     */
    public SyncManager() {
        isTabletDevice = VZUris.isTabletDevice();
    }

    public abstract void destroy();

    protected abstract void doSync(boolean onDemand);

    public abstract void reset();

    public final boolean isDryRun() {
        return false;
        // return true;
    }

    protected final boolean isTablet() {
        return VZUris.isTabletDevice();
    }

}
