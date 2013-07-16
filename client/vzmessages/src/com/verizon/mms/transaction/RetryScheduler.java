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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.util.DownloadManager;
import com.verizon.mms.util.SqliteWrapper;

public class RetryScheduler implements Observer {
    private final Context mContext;
    private final ContentResolver mContentResolver;

    private RetryScheduler(Context context) {
        mContext = context;
        mContentResolver = context.getContentResolver();
    }

    private static RetryScheduler sInstance;
    public static RetryScheduler getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RetryScheduler(context);
        }
        return sInstance;
    }

    private boolean shouldRetry() {
    	// vz changed name from isConnected to shouldRetry 
    	// return true if either MOBILE_MMS is available or if TYPE_MOBILE is connected
    	// because even if MMS is not connected but available we should try to reconnect it
        ConnectivityManager mConnMgr = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = mConnMgr.getNetworkInfo(MmsConfig.getNetworkConnectivityMode());
        if ((ni != null) && (ni.isAvailable())) {
        	return true;
        } else {
        	ni = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        	return (ni == null ? false : ni.isConnected());
        }
    }


    public void update(Observable observable) {
        try {
            Transaction t = (Transaction) observable;

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "update: " + observable);
            }
            
            // We are only supposed to handle M-Notification.ind, M-Send.req
            // and M-ReadRec.ind.
            // Removed retry for M-ReadRec
            if ((t instanceof NotificationTransaction)
                    || (t instanceof RetrieveTransaction)
                    //|| (t instanceof ReadRecTransaction)
                    || (t instanceof SendTransaction)) {
                try {
                    TransactionState state = t.getState();
                    if (state.getState() == TransactionState.FAILED) {
                        Uri uri = state.getContentUri();
                        if (uri != null) {
                            scheduleRetry(uri, state);
                        }
                    }
                } finally {
                    t.detach(this);
                }
            }
        } finally {
        	// always set up retry - on retry it will check - if no network it will register listener
//              if (shouldRetry()) {
        	setRetryAlarm(mContext);
//            } else {
//            	// VZ there is no network and so we are not setting the alaram to retry transaction
//            	// There we rely on the system connectivity listener detecting that connection has been established and
//            	// hence waking up the transaction service
//        		MmsSystemEventReceiver.registerForConnectionStateChanges(mContext);
//            }
        }
    }

    private void scheduleRetry(Uri uri, TransactionState state) {
        long msgId = ContentUris.parseId(uri);

        Uri.Builder uriBuilder = VZUris.getMmsSmsPendingUri().buildUpon();
        uriBuilder.appendQueryParameter("protocol", "mms");
        uriBuilder.appendQueryParameter("message", String.valueOf(msgId));

        Cursor cursor = SqliteWrapper.query(mContext, mContentResolver,
                uriBuilder.build(), null, null, null, null);

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    int msgType = cursor.getInt(cursor.getColumnIndexOrThrow(
                            PendingMessages.MSG_TYPE));

                    int retryIndex = cursor.getInt(cursor.getColumnIndexOrThrow(
                            PendingMessages.RETRY_INDEX)) + 1; // Count this time.

                    // TODO Should exactly understand what was happened.
                    int errorType = MmsSms.ERR_TYPE_GENERIC;

                    DefaultRetryScheme scheme = new DefaultRetryScheme(mContext, retryIndex);

                    ContentValues values = new ContentValues(4);
                    long current = System.currentTimeMillis();
                    boolean isRetryDownloading =
                            (msgType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
                    boolean retry = true;
                    int respStatus = getResponseStatus(msgId);
                    if (respStatus == PduHeaders.RESPONSE_STATUS_ERROR_SENDING_ADDRESS_UNRESOLVED) {
                        DownloadManager.getInstance().showErrorCodeToast(R.string.invalid_destination);
                        retry = false;
                    }

                    final boolean fatal = state.getError() >= PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_FAILURE;
                    if (Logger.IS_DEBUG_ENABLED) {
                    	Logger.debug(getClass(), "error is: " + state.getError() + " isFatal: " + fatal);
                    }
                    if (!fatal && (retryIndex < scheme.getRetryLimit()) && retry) {
                        long retryAt = current + scheme.getWaitingInterval();

                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(), "scheduleRetry: retry for " + uri + " is scheduled at "
                                    + (retryAt - System.currentTimeMillis()) + "ms from now");
                        }

                        long retryAtWithDelta = retryAt; // + TransactionService.DELTA_TIME_TO_PREVENT_NATIVE;
                        values.put(PendingMessages.DUE_TIME, retryAtWithDelta);

                        if (isRetryDownloading) {
                            // Downloading process is transiently failed.
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(getClass(), "scheduleRetry: download process transiently failed ");
                            }
                            DownloadManager.getInstance().markState(
                                    uri, DownloadManager.STATE_TRANSIENT_FAILURE);
                        }
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                        	Logger.debug(getClass(), "marking error as permanent - isRetryDownloading: " + isRetryDownloading);
                        }

                        errorType = MmsSms.ERR_TYPE_GENERIC_PERMANENT;
                        if (isRetryDownloading) {
                            Cursor c = SqliteWrapper.query(mContext, mContext.getContentResolver(), uri,
                                    new String[] { Mms.THREAD_ID }, null, null, null);
                            
                            long threadId = -1;
                            if (c != null) {
                                try {
                                    if (c.moveToFirst()) {
                                        threadId = c.getLong(0);
                                    }
                                } finally {
                                    c.close();
                                }
                            }

                            if (threadId != -1) {
                                // Downloading process is permanently failed.
                                MessagingNotification.notifyDownloadFailed(mContext, threadId);
                                if (Logger.IS_DEBUG_ENABLED) {
                                    Logger.debug(getClass(), "scheduleRetry: download process permanently failed ");
                                }
                            }

                            DownloadManager.getInstance().markState(uri, DownloadManager.STATE_PERMANENT_FAILURE, fatal);
                        } else {
                            // create an entry in the native pending tables and mark it as failed
                        	MessageUtils.markMmsMessageWithError(mContext, mContext.getContentResolver(), uri, true);

                        	// Mark the failed message as unread.
                            ContentValues readValues = new ContentValues(1);
                            readValues.put(Mms.READ, 0);
                            SqliteWrapper.update(mContext, mContext.getContentResolver(),
                                    uri, readValues, null, null);
                            MessagingNotification.notifySendFailed(mContext, true);
                        }
                    }

                    values.put(PendingMessages.ERROR_TYPE,  errorType);
                    values.put(PendingMessages.RETRY_INDEX, retryIndex);
                    values.put(PendingMessages.LAST_TRY,    current);

                    int columnIndex = cursor.getColumnIndexOrThrow(
                            PendingMessages._ID);
                    long id = cursor.getLong(columnIndex);
                    SqliteWrapper.update(mContext, mContentResolver,
                    		VZUris.getMmsSmsPendingUri(),
                            values, PendingMessages._ID + "=" + id, null);

                } else if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "scheduleRetry: cannot find correct pending status for: " + msgId);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private int getResponseStatus(long msgID) {
        int respStatus = 0;
        Cursor cursor = SqliteWrapper.query(mContext, mContentResolver,
        		VZUris.getMmsOutboxUri(), null, Mms._ID + "=" + msgID, null, null);
        try {
            if (cursor.moveToFirst()) {
                respStatus = cursor.getInt(cursor.getColumnIndexOrThrow(Mms.RESPONSE_STATUS));
            }
        } finally {
            cursor.close();
        }
        if (respStatus != 0) {
        	if (Logger.IS_DEBUG_ENABLED)
        		Logger.debug(getClass(), "Response status is: " + respStatus);
        }
        return respStatus;
    }

    public static void setRetryAlarm(Context context) {
        Cursor cursor = PduPersister.getPduPersister(context).getPendingMessages(
                Long.MAX_VALUE);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    // The result of getPendingMessages() is order by due time.
                    long msgDueTime = cursor.getLong(cursor.getColumnIndexOrThrow(PendingMessages.DUE_TIME));
                    long retryAt = msgDueTime; // - TransactionService.DELTA_TIME_TO_PREVENT_NATIVE;
                    if (retryAt < System.currentTimeMillis()) {
                    	if (Logger.IS_DEBUG_ENABLED) {
                			int columnIndex = cursor.getColumnIndexOrThrow(PendingMessages._ID);
                			long id = cursor.getLong(columnIndex);
            				int columnIndexOfMsgId = cursor.getColumnIndexOrThrow(PendingMessages.MSG_ID);
            				int columnIndexOfMsgType = cursor.getColumnIndexOrThrow(PendingMessages.MSG_TYPE);
            				int msgType = cursor.getInt(columnIndexOfMsgType);					
            				long msgId = cursor.getLong(columnIndexOfMsgId);
            				String msgProto = cursor.getString(cursor.getColumnIndexOrThrow(PendingMessages.PROTO_TYPE));
            				String msgErrType = cursor.getString(cursor.getColumnIndexOrThrow(PendingMessages.ERROR_TYPE));
            				String msgErrCode = cursor.getString(cursor.getColumnIndexOrThrow(PendingMessages.ERROR_CODE));
            				String msgRetryIndex = cursor.getString(cursor.getColumnIndexOrThrow(PendingMessages.RETRY_INDEX));

            				Logger.debug("RetryScheduler: alarm in the past - msgId:" + msgId + " msgType:" + msgType 
            						+ " rowId:" + id + " dueTime:" +  msgDueTime + " proto:" + msgProto + " errType:" + msgErrType 
            						+ " errCode:" + msgErrCode + " retryIndex:" + msgRetryIndex);
                    	}
                    	continue;
                    }
                    
                    Intent service = new Intent(TransactionService.ACTION_ONALARM,
                                        null, context, TransactionService.class);
                    PendingIntent operation = PendingIntent.getService(
                            context, 0, service, PendingIntent.FLAG_ONE_SHOT);
                    AlarmManager am = (AlarmManager) context.getSystemService(
                            Context.ALARM_SERVICE);
                    am.set(AlarmManager.RTC, retryAt, operation);

                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("RetryScheduler: next retry is scheduled at"
                                + (retryAt - System.currentTimeMillis()) + "ms from now");
                    }
                    break;
                }
            } finally {
                cursor.close();
            }
        }
    }
}
