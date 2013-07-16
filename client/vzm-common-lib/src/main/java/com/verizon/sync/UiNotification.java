/**
 * SyncStatus.java
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
 * @author Jegadeesan
 * @Since  Feb 21, 2013
 */
public interface UiNotification {
    
    /**
     * This Method is used to update the new messages
     * 
     * @param uri
     */
    public void newMessage(Uri uri);

    /**
     * This Method is used to notify the read message to UI to clear the notification.
     * 
     * @param uri
     */
    public void clearReadMessageNotification(Uri uri);

    /**
     * This Method is used to update the sync status
     * 
     * @param state
     */
    public void syncStatus(int state);

    /**
     * This Method is used to update the sending status
     * 
     * @param progress
     * @param total
     * @param type
     */
    public void sendingMessage(int progress, int total, int type) ;

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
     * Send a broadcast to force listeners to purge their cache of the message, and optionally notify cursor
     * observers.
     */
    public void purge(Uri msgUri, boolean notify);

}
