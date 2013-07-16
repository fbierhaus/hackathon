/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.Telephony.Sms;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.VZTelephony;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.SyncManager;

public class SmsMessageSender implements MessageSender {
    protected final Context mContext;
    protected final int mNumberOfDests;
    private final String[] mDests;
    protected final String mMessageText;
    protected final String mServiceCenter;
    protected final long mThreadId;
    protected long mTimestamp;


    private static final String[] SERVICE_CENTER_PROJECTION = new String[] {
            Sms.Conversations.REPLY_PATH_PRESENT, Sms.Conversations.SERVICE_CENTER, };

    private static final int COLUMN_REPLY_PATH_PRESENT = 0;
    private static final int COLUMN_SERVICE_CENTER = 1;

    public SmsMessageSender(Context context, String[] dests, String msgText, long threadId) {
        mContext = context;
        mMessageText = msgText;
        if (dests != null) {
            mNumberOfDests = dests.length;
            mDests = new String[mNumberOfDests];
            System.arraycopy(dests, 0, mDests, 0, mNumberOfDests);
        } else {
            mNumberOfDests = 0;
            mDests = null;
        }
        mTimestamp = System.currentTimeMillis();
        mThreadId = threadId;
        mServiceCenter = getOutgoingServiceCenter(mThreadId);
    }

    public Uri[] sendMessage(long token) throws MmsException {
        // In order to send the message one by one, instead of sending now, the message will split,
        // and be put into the queue along with each destinations
        return queueMessage(token);
    }

    private Uri[] queueMessage(long token) throws MmsException {
        if ((mMessageText == null) || (mNumberOfDests == 0)) {
            // Don't try to send an empty message.
            throw new MmsException("Null message body or dest.");
        }

        final Uri[] uris = new Uri[mNumberOfDests];
        for (int i = 0; i < mNumberOfDests; i++) {
            try {
                Uri smsUri = VZTelephony.addMessageToUri(mContext.getContentResolver(), VZUris.getSmsQueuedUri(),
                        mDests[i], mMessageText, null, mTimestamp,  true, false, mThreadId);
                uris[i] = smsUri;

                ConversationDataObserver.onMessageStatusChanged(mThreadId, ContentUris.parseId(smsUri), ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "queueMessage: tid = " + mThreadId + ", dest = " + mDests[i] + ", uri = " + smsUri +"==="+ VZUris.getSmsQueuedUri());
                }
                // VMA SYNC 
                if(MmsConfig.isTabletDevice()){
                    if (Logger.IS_INFO_ENABLED) {
                        Logger.info(SmsMessageSender.class, "Sending sms using vma. uri="+ smsUri);
                    }
                    if(smsUri!=null){
                        Intent sendMsg = new Intent(SyncManager.ACTION_SEND_MSG);
                        sendMsg.putExtra(SyncManager.EXTRA_URI, smsUri);
                        mContext.startService(sendMsg);
                    }else{
                        if (Logger.IS_INFO_ENABLED) {
                            Logger.info(SmsMessageSender.class, "Sms URI is null. ignoring the send via vma. uri="+smsUri);
                        }    
                    }
                }
                

            } catch (SQLiteException e) {
                SqliteWrapper.checkSQLiteException(mContext, e);
            }
        }

        if (!MmsConfig.isTabletDevice()) {
            // Notify the SmsReceiverService to send the message out
            mContext.sendBroadcast(new Intent(SmsReceiverService.ACTION_SEND_MESSAGE, null, mContext,
                    SmsReceiver.class));
        }
        return uris;
    }

    /**
     * Get the service center to use for a reply.
     * 
     * The rule from TS 23.040 D.6 is that we send reply messages to the service center of the message to
     * which we're replying, but only if we haven't already replied to that message and only if
     * <code>TP-Reply-Path</code> was set in that message.
     * 
     * Therefore, return the service center from the most recent message in the conversation, but only if it
     * is a message from the other party, and only if <code>TP-Reply-Path</code> is set. Otherwise, return
     * null.
     */
    private String getOutgoingServiceCenter(long threadId) {
        Cursor cursor = null;

        try {
            cursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), VZUris.getSmsUri(),
                    SERVICE_CENTER_PROJECTION, "thread_id = " + threadId, null, "date DESC");

            if ((cursor == null) || !cursor.moveToFirst()) {
                return null;
            }

            boolean replyPathPresent = (1 == cursor.getInt(COLUMN_REPLY_PATH_PRESENT));
            return replyPathPresent ? cursor.getString(COLUMN_SERVICE_CENTER) : null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
