/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.transaction;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Sms;
import android.telephony.SmsMessage;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ui.MessageItem;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.VMASyncHook;

/**
 * Service that gets started by the MessageStatusReceiver when a message status report is
 * received.
 */
public class MessageStatusService extends IntentService {
    private static final String[] ID_PROJECTION = new String[] { Sms._ID, Sms.THREAD_ID };
    private static final Uri STATUS_URI = Uri.parse("content://sms/status");

    public MessageStatusService() {
        // Class name will be the thread name.
        super(MessageStatusService.class.getName());

        // Intent should be redelivered if the process gets killed before completing the job.
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // This method is called on a worker thread.

        Uri messageUri = intent.getData();
        byte[] pdu = intent.getByteArrayExtra("pdu");
        String format = intent.getStringExtra("format");
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "updateMessageStatus: onHandleIntent");
        }

        boolean isStatusMessage = updateMessageStatus(this, messageUri, pdu);

        // Called on a background thread, so it's OK to block.
        /*
         * Fixed Issues Bug 944 . Mass Text message and the delivery report order is uncertain. So querying 
         * by the last send SMS goes wrong. So Used the Uri to identify who the message was delivered to.
         */
       MessagingNotification.blockingUpdateNewMessageIndicator(this,
               true, isStatusMessage,messageUri);
    }

    private boolean updateMessageStatus(Context context, Uri messageUri, byte[] pdu) {
        // Create a "status/#" URL and use it to update the
        // message's status in the database.
        boolean isStatusReport = false;
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            messageUri, ID_PROJECTION, null, null, null);
        try {
            if ((cursor != null) && (cursor.moveToFirst())) {
                int messageId = cursor.getInt(0);
                long threadId = cursor.getLong(1);

                Uri updateUri = ContentUris.withAppendedId(STATUS_URI, messageId);
                SmsMessage message = SmsMessage.createFromPdu(pdu);
                int status = message.getStatus();
                isStatusReport = message.isStatusReportMessage();
                ContentValues contentValues = new ContentValues(1);

                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "updateMessageStatus: msgUrl=" + messageUri + ", status=" + status +
                            ", isStatusReport=" + isStatusReport);
                }

                contentValues.put(Sms.STATUS, MessageItem.normalizeStatus(status));
                SqliteWrapper.update(context, context.getContentResolver(),
                                    updateUri, contentValues, null, null);
                
                /* Wifi Sync HookUps */
                if(Logger.IS_DEBUG_ENABLED){
                    Logger.debug(getClass(), "Wifi-Hook:SMS: delivered - uri="+ messageUri);
                }
                long id = ContentUris.parseId(messageUri);
                ConversationDataObserver.onMessageStatusChanged(threadId, id, ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
                VMASyncHook.syncSMSDelivered(context, ContentUris.parseId(messageUri));
            } else {
                error("Can't find message for status update: " + messageUri);
            }
        } finally {
        	if (cursor != null) {
        		cursor.close();
        	}
        }
        return isStatusReport;
    }

    private void error(String message) {
    	Logger.error(getClass(), "[MessageStatusService] " + message);
    }
}
