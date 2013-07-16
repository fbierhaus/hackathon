/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.verizon.mms.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.provider.Telephony.Threads;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.MmsConfig;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.VMASyncHook;

/**
 * The recycler is responsible for deleting old messages.
 */
public class Recycler {
    private static final String AUTO_DELETE = "pref_key_auto_delete";
    // Default preference values
    private static final boolean DEFAULT_AUTO_DELETE = false;

    private static final String[] ALL_THREADS_PROJECTION = { Telephony.Threads._ID,
            Telephony.Threads.MESSAGE_COUNT };

    // columns for quering threads table with ALL_THREADS_PROJECTION
    private static final int THREAD_ID = 0;
    private static final int MSG_COUNT = 1;

    private static final String ALL_THREADS_WHERE = Threads.RECIPIENT_IDS + " != '' and " + Threads._ID
            + " != -1";

    private final String MAX_MESSAGES_PER_THREAD = "MaxMessagesPerThread";

    private static Recycler sRecycler;

    private static final String[] PROJECTION = new String[] { MmsSms.TYPE_DISCRIMINATOR_COLUMN,
            BaseColumns._ID, Sms.DATE,
            // For MMS
            Mms.DATE };

    private final int COL_MSG_TYPE = 0;
    private final int COL_ID = 1;
    private final int COL_SMS_DATE = 2;
    private final int COL_MMS_DATE = 3;

    public static Recycler getMessageRecycler() {
        if (sRecycler == null) {
            sRecycler = new Recycler();
        }
        return sRecycler;
    }

    public static boolean checkForThreadsOverLimit(Context context) {
        Recycler recycler = getMessageRecycler();

        return recycler.anyThreadOverLimit(context);
    }

    private void deleteOldMessagesInThread(Context context, long threadId) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "Recycler.deleteOldMessagesInThread this: " + this + " threadId: "
                    + threadId);
        }
        ContentResolver resolver = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(VZUris.getMmsSmsConversationUri(), threadId);

        Cursor conversation = resolver.query(uri, PROJECTION, null, null, null);

        if (conversation != null) {
            int msgLimit = getMessageLimit(context);
            int count = conversation.getCount() - 1;
            long smsDate = 0;
            long mmsDate = 0;

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), " total message count " + count + " messages allowed in a thread "
                        + msgLimit);
            }
            if (count >= msgLimit) {
                conversation.moveToPosition(count - msgLimit);

                final String prevMsgType = conversation.getString(COL_MSG_TYPE);
                if (prevMsgType.charAt(0) == 'm') {
                    mmsDate = conversation.getLong(COL_MMS_DATE);
                    smsDate = mmsDate * 1000;
                } else {
                    smsDate = conversation.getLong(COL_SMS_DATE);
                    mmsDate = smsDate / 1000;
                }
            }

            conversation.close();

            deleteMessagesForThreadId(context, threadId, smsDate, mmsDate);
        }
    }

    private void deleteMessagesForThreadId(Context context, long threadId, long smsDate, long mmsDate) {
        if (smsDate > 0) {
            deleteSMSOlderThanDate(context, threadId, smsDate);
        }
        if (mmsDate > 0) {
            deleteMMSOlderThanDate(context, threadId, mmsDate);
        }
    }

    public static boolean isAutoDeleteEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(AUTO_DELETE, DEFAULT_AUTO_DELETE);
    }

    public int getMessageMinLimit() {
        return MmsConfig.getMinMessageCountPerThread();
    }

    public int getMessageMaxLimit() {
        return MmsConfig.getMaxMessageCountPerThread();
    }

    private boolean anyThreadOverLimit(Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "Recycler: anyThreadOverLimit");
        }

        Cursor cursor = getAllThreads(context);
        int limit = getMessageLimit(context);
        try {
            while (cursor.moveToNext()) {
                int count = cursor.getInt(MSG_COUNT);
                if (count >= limit) {
                    return true;
                }
            }
        } finally {
            cursor.close();
        }
        return false;
    }

    public int getMessageLimit(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(MAX_MESSAGES_PER_THREAD, MmsConfig.getDefaultMessagesPerThread());
    }

    public void setMessageLimit(Context context, int limit) {
        SharedPreferences.Editor editPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editPrefs.putInt(MAX_MESSAGES_PER_THREAD, limit);
        editPrefs.commit();
    }

    private Cursor getAllThreads(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = SqliteWrapper.query(context, resolver, VZUris.getMmsSmsConversationUri().buildUpon()
                .appendQueryParameter("simple", "true").build(), ALL_THREADS_PROJECTION, ALL_THREADS_WHERE,
                null, Conversations.DEFAULT_SORT_ORDER);

        return cursor;
    }

    public void deleteMsgsOverLimit(Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this, "deleteMsgsOverLimit()");
        }
        if (!isAutoDeleteEnabled(context)) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this, "Auto Delete is not Enabled.");
            }
            return;
        }
        Cursor threads = getAllThreads(context);
        if (threads != null) {
            while (threads.moveToNext()) {
                deleteOldMessagesInThread(context, threads.getLong(THREAD_ID));
            }
            threads.close();
        }
    }

    private void deleteMMSOlderThanDate(Context context, long threadId, long latestDate) {
        VMASyncHook.markVMAMmsAsDeleteOlderThanDate(context, threadId, latestDate);
        long cntDeleted = SqliteWrapper.delete(context, context.getContentResolver(), VZUris.getMmsUri(),
                "thread_id=" + threadId + " AND date<=" + latestDate, null);

        ConversationDataObserver.onMessageDeleted(threadId, -1, -1, ConversationDataObserver.MSG_SRC_TELEPHONY);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "deleteMMSOlderThanDate date: " + latestDate + " cntDeleted: "
                    + cntDeleted);
        }
    }

    private void deleteSMSOlderThanDate(Context context, long threadId, long latestDate) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("deleteSMSOlderThanDate() threadId=" + threadId + ",latestDate=" + latestDate);
        }
        VMASyncHook.markVMASmsAsDeleteOlderThanDate(context, threadId, latestDate);
        
        long cntDeleted = SqliteWrapper.delete(context, context.getContentResolver(), VZUris.getSmsUri(),
                "thread_id=" + threadId + " AND date<=" + latestDate, null);

        ConversationDataObserver.onMessageDeleted(threadId, -1, -1, ConversationDataObserver.MSG_SRC_TELEPHONY);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "deleteSMSOlderThanDate date: " + latestDate + " cntDeleted: "
                    + cntDeleted);
        }
    }
}