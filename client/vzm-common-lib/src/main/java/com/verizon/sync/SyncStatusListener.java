/**
 * SyncStatusListener.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.sync;

import android.net.Uri;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Jan 16, 2013
 */
public interface SyncStatusListener {

    // FETCH STATUS
//    public static final int FETCHING_CONVERSATION = 9200;
//    public static final int FETCHING_ATTACHEMENTS = 9201;
//    public static final int FETCHING_MESSAGE = 9202;
//    public static final int FETCHING_CHANGES = 9203;
//    public static final int NEW_MESSAGE = 9204;
//    public static final int SYNC_PREPARING = 9205;
//
//    // SENDING STATUS
//    public static final int SENDING_MESSAGE = 9300;
//    public static final int SENDING_READ = 9301;
//    public static final int SENDING_DELETE = 9302;
//
//    // CONNECTION STATUS
//    public static final int FOREGROUND_CONNECTION_IDLE = 9401;
//    public static final int FOREGROUND_CONNECTION_WAIT = 9402;
//    public static final int BACKGROUND_CONNECTION_WAIT = 9403;
//    public static final int VMA_CONNECTION_SIGNOUT     = 9403;
//
//    public static final int NO_NETWORK = 9410;
//    public static final int NO_WIFI_NETWORK = 9411;
//    public static final int FOREGROUND_LOGIN = 9412;
//    public static final int BACKGROUND_LOGIN = 9413;
//    public static final int AUTHENTICATION_FAILED = 9414;
//
//    // KNOW ERROR FROM SERVER
//    public static final int MSA_RESPONSE_ILLEGAL_ARGUMENTS = 1400;
//    public static final int MSA_RESPONSE_INVALID_LOGIN = 1401;
//    public static final int MSA_RESPONSE_WRONG_STATE = 1402;
//    public static final int MSA_RESPONSE_HAS_SMS_BLOCKING = 1403;
//    public static final int MSA_RESPONSE_NOT_A_VMA_SUBSCRIBER = 1404;
//    public static final int MSA_RESPONSE_SUSPENDED = 1405;
//    public static final int MSA_RESPONSE_ISTD_NOT_SUPPORTED = 1406;
//    // Connected State
//    public static final int SYNC_PROVISIONING = 9104;
//    public static final int SIGNOUT_SEND_CONNECTION = 91005;
//    public static final int SIGNOUT_FETCH_CONNECTION = 91006;
//    public static final int UPDATE_AUTO_REPLY_OR_FORWARD = 91007;
    

//    public enum SyncAgentType {
//    	FOREGROUND,
//    	BACKGROUND
//    };
    
    /**
     * This Method is used to update the sending status
     * 
     * @param progress
     * @param total
     * @param type
     */
    public void sendingMessage(int progress, int total, int type);

    /**
     * This Method is used to update received message status.
     * 
     * @param progress
     * @param total
     * @param type
     */
    public void fetchingMessage(int progress, int total, int type);

    /**
     * This Method is used to fetch attachements
     * 
     * @param progress
     * @param total
     * @param type
     */
    public void fetchingAttachements(int progress, int total, int type);

    /**
     * This Method is used to update the sync status
     * 
     * @param state
     */

    /**
     * This Method is used to update the new messages
     * 
     * @param uri
     */
    public void newMessage(Uri uri);
    
    /**
     * This Method is used to notify the read message to UI to clear the notification.
     * @param uri
     */
    public void clearReadMessageNotification(Uri uri);

    /**
     * This Method
     */
    public void wakeUpBackgroundSync();

}
