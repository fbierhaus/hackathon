/**
 * SmsChecksumBuilder.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.Telephony.Sms;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.messaging.vzmsgs.sync.SyncStatusCode;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.sync.SyncManager;
import com.verizon.sync.SyncStatusListener;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Jan 23, 2013
 */
public class SmsChecksumBuilder extends Thread {

    private Context context;
    private ContentResolver resolver;
    private AppSettings settings;
    private int maxId;
    private static int threadCount;
    private VMAEventHandler vmaEventHandler;

    /**
     * 
     * Constructor
     */
    public SmsChecksumBuilder(AppSettings settings, int maxLuid) {
        this.context=settings.getContext();
        this.resolver = settings.getContext().getContentResolver();
        this.maxId = maxLuid;
        this.settings = settings;
        this.setName("SmsChecksumBuilder-" + (++threadCount));
        this.setPriority(Thread.MIN_PRIORITY);
        vmaEventHandler = ApplicationSettings.getInstance().getVMAEventHandler();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        long start = System.currentTimeMillis();
        int count = 0;
        synchronized (SmsChecksumBuilder.class) {
            try {
                if (!ApplicationSettings.getInstance().getBooleanSetting(
                        AppSettings.KEY_SMS_CHECKSUM_COMPLETED)) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Building checksum for previously received SMS");
                    }
                    String[] projection = new String[] { Sms._ID, Sms.THREAD_ID, Sms.DATE, Sms.ADDRESS,
                            Sms.BODY, Sms.TYPE };
                    String where = "(" + Sms.TYPE + "=" + Sms.MESSAGE_TYPE_INBOX + " OR " + Sms.TYPE + "="
                            + Sms.MESSAGE_TYPE_SENT + ")" + " AND " + Sms._ID + " <= " + maxId;
                    Cursor cursor = SqliteWrapper.query(context,resolver,VZUris.getSmsUri(), projection, where, null, null);
                    if (cursor != null) {
                        long threadId = 0;
                        long date = 0;
                        String address = null;
                        String body = null;
                        long luid = 0;
                        int msgBox = 0;
                        Intent statusIntent = new Intent(SyncManager.ACTION_SYNC_STATUS);
                        statusIntent.putExtra(SyncManager.EXTRA_STATUS, SyncStatusCode.VMA_SYNC_CHECKSUM_BUILDER_START);
                        settings.getContext().sendBroadcast(statusIntent);
                        int notificationCount = 0;
                        while (cursor.moveToNext()) {
                            count++;
                            luid = cursor.getLong(0);
                            threadId = cursor.getLong(1);
                            date = cursor.getLong(2);
                            address = ApplicationSettings.parseAdddressForChecksum(cursor.getString(3));
                            body = cursor.getString(4);
                            msgBox = cursor.getInt(5);
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("Building checksum : id=" + luid + ",box=" + msgBox
                                        + ",threadId=" + threadId + ",date=" + date + ",address=" + address
                                        + ",msg=" + body);
                            }
                            int messageBox = VMAMapping.MSGBOX_RECEIVED;
                            if (msgBox == Sms.MESSAGE_TYPE_SENT) {
                                messageBox = VMAMapping.MSGBOX_SENT;
                            }
                            vmaEventHandler.checksumBuilderAddSMS(luid, threadId, body, address, date,
                                    messageBox);
                            // Sending notification for every 100 messages.
                            notificationCount++;
                            if (notificationCount == 100) {
                                settings.getContext().sendBroadcast(statusIntent);
                                notificationCount = 0;
                            }
                        }
                        cursor.close();
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("No messages found in sms db");
                        }
                    }
                    ApplicationSettings.getInstance().put(AppSettings.KEY_SMS_CHECKSUM_COMPLETED, true);
                    Intent statusIntent = new Intent(SyncManager.ACTION_SYNC_STATUS);
                    statusIntent.putExtra(SyncManager.EXTRA_STATUS, SyncStatusCode.VMA_SYNC_CHECKSUM_BUILDER_STOP);
                    settings.getContext().sendBroadcast(statusIntent);
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Already Indexed. Skipping indexing.");
                    }
                }
            } catch (Exception e) {
                Logger.error("SmsChecksumBuilder: got exception", e);
            }
        }
        long end = System.currentTimeMillis();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Done building checksum for previously received SMS. count=" + count + " time="
                    + (end - start));
        }

    }

}
