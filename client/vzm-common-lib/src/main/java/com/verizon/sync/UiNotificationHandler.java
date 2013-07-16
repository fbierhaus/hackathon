/**
 * VMASyncStatusNotification.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.sync;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony.Mms;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemType;
import com.verizon.mms.MediaSyncHelper;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Feb 21, 2013
 */
public class UiNotificationHandler implements UiNotification {

    private Intent statusIntent;
    private Context context;

    /**
     * 
     * Constructor
     */
    public UiNotificationHandler(Context context) {
        this.context = context;
        statusIntent = new Intent(SyncManager.ACTION_SYNC_STATUS);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.sync.SyncNotification#newMessage(android.net.Uri)
     */
    @Override
    public void newMessage(Uri uri) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(" New unread message. uri=" + uri);
        }
        statusIntent.putExtra(SyncManager.EXTRA_STATUS, SyncManager.SYNC_SHOW_NEW_NOTIFICATION);
        statusIntent.putExtra(SyncManager.EXTRA_URI, uri);
        context.sendBroadcast(statusIntent);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.sync.SyncNotification#clearReadMessageNotification(android.net.Uri)
     */
    @Override
    public void clearReadMessageNotification(Uri uri) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Message read in remote device. clearing notification.");
        }
        Intent intent = new Intent(SyncManager.ACTION_SYNC_STATUS);
        intent.putExtra(SyncManager.EXTRA_STATUS, SyncManager.CLEAR_SHOWN_NOTIFICATION);
        intent.putExtra(SyncManager.EXTRA_URI, uri);
        context.sendBroadcast(intent);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.sync.SyncNotification#syncstatus(int)
     */
    @Override
    public synchronized void syncStatus(int state) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Sending status broadcast.status=" + state);
        }
        Intent intent = new Intent(SyncManager.ACTION_SYNC_STATUS);
        intent.putExtra(SyncManager.EXTRA_STATUS, state);
        context.sendBroadcast(intent);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.sync.SyncNotification#sendingMessage(int, int, int)
     */
    @Override
    public void sendingMessage(int progress, int total, int type) {
        // TODO Auto-generated method stub

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.sync.SyncNotification#fetchingMessage(int, int, int)
     */
    @Override
    public void fetchingMessage(int progress, int total, int type) {
        // TODO Auto-generated method stub

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.sync.SyncNotification#fetchingAttachements(int, int, int)
     */
    @Override
    public void fetchingAttachements(int progress, int total, int type) {
        // TODO Auto-generated method stub

    }
    
    /**
     * Send a broadcast to force listeners to purge their cache of the message, and optionally notify cursor
     * observers.
     */
    public synchronized void purge(Uri msgUri, boolean notify) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("purge : for " + msgUri);
        }
        // clear the media cache
        MediaSyncHelper.onMessageDelete(context ,msgUri);
        final Intent intent = new Intent(Mms.Intents.CONTENT_CHANGED_ACTION);
        intent.putExtra(Mms.Intents.DELETED_CONTENTS, msgUri);
        if (notify) {
            intent.putExtra(SyncManager.EXTRA_NOTIFY, true);
        }
        context.sendBroadcast(intent);
    }
    

}
