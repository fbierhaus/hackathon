/**
 * WifiSyncHelper.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.sync;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.mms.util.SqliteWrapper;

/**
 * This class is used to trigger the UI changes to VMA Server.
 * 
 * @author Jegadeesan.M
 * @Since May 25, 2012
 */
public class VMASyncHook {

    private static Intent intent;
    // private static SharedPreferences prefs;
    private static String[] ID_MSG_TYPE_PROJECTON = new String[] { MmsSms._ID,
            MmsSms.TYPE_DISCRIMINATOR_COLUMN };

    /**
     * This Method is used to notify the SMS changes to the WIFI Sync Service
     * 
     * @param context
     * @param threadId
     * @param luid
     */
    private static void sendBroadcast(Context context, long luid, int modType) {
        // if (!isDevicePaired(context)) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.warn("device is not paired");
        // }
        // return;
        // }
        // intent = new Intent(SyncManager.ACTION_SYNC_CHANGES);
        // intent.putExtra(SyncManager.EXTRA_LUID, luid);
        // intent.putExtra(SyncManager.EXTRA_MOD_TYPE, modType);
        // context.startService(intent);

    }

    public static void syncMMSDelivered(Context context, long luid) {
        // sendBroadcast(context, luid, MMS_DELIVERED);
    }

    public static void syncReceivedSMS(Context context, long luid) {
        // sendBroadcast(context, luid, RECEIVED_SMS);
    }

    public static void syncSMSDelivered(Context context, long luid) {
        // sendBroadcast(context, luid, SMS_DELIVERED);
    }

    public static void syncSMSDelete(Context context, long luid) {
        // sendBroadcast(context, luid, SMS_DELETED);
    }

    public static void syncSendFailedSMS(Context context, long luid) {
        //
        // if (!MmsConfig.isTabletDevice()) {
        // sendBroadcast(context, luid, SMS_SEND_FAILED);
        // } else {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.warn(WifiSyncHelper.class, "Device is tablet : Ignoring send failed. ");
        // }
        // }
    }

    /**
     * This Method
     * 
     * @param mContext
     * @param parseId
     */
    public static void markMMSSendFailed(Context context, long luid) {
        // if (!MmsConfig.isTabletDevice()) {
        // sendBroadcast(context, luid, MMS_SEND_FAILED);
        // } else {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.warn(WifiSyncHelper.class, "Device is tablet : Ignoring send failed. ");
        // }
        // }
    }

    /**
     * This Method
     * 
     * @param smsReceiverService
     * @param parseId
     */
    public static void syncSentSMS(Context context, long luid) {
        // if (!MmsConfig.isTabletDevice()) {
        // sendBroadcast(context, luid, SMS_SENT);
        // } else {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.warn(WifiSyncHelper.class, "Device is tablet : Ignoring sent failed. ");
        // }
        // }
    }

    public static void syncSendingSMS(Context context, long luid) {
        // sendBroadcast(context, luid, SENDING_SMS);
    }

    public static void syncMMSDelete(Context context, long luid) {
        // sendBroadcast(context, luid, MMS_DELETED);
    }

    public static void sendReceivedMMS(Context context, long luid) {
        // if (ApplicationSettings.isVMASyncEnabled()
        // && ApplicationSettings.getInstance(context).isProvisioned()) {
        // //
        // String[] projection = new String[] { Mms.MESSAGE_ID, Mms.THREAD_ID };
        // Cursor cursor = context.getContentResolver().query(
        // ContentUris.withAppendedId(VZUris.getMmsUri(), luid), projection, null, null, null);
        // if (cursor != null) {
        // while (cursor.moveToNext()) {
        // String msgId = cursor.getString(0);
        // long threadId = cursor.getLong(0);
        // // update the received mms id in mapping table
        // String where = MsgInfoTable.VMA_MSG_ID + "='" + msgId + "'";
        // ContentValues values = new ContentValues();
        // values.put(MsgInfoTable.THREAD_ID, threadId);
        // values.put(MsgInfoTable.LUID, luid);
        // int count = context.getContentResolver().update(MsgInfoTable.CONTENT_URI, values, where,
        // null);
        // if (Logger.IS_INFO_ENABLED) {
        // Logger.info("Updated mmsc id to vma mapping:count=" + count);
        // }
        // }
        // cursor.close();
        // }
        //
        // }
        // // sendBroadcast(context, luid, RECEIVED_MMS);
    }

    public static void syncSendingMMS(Context context, long luid) {
        // sendBroadcast(context, luid, SENDING_MMS);
    }

    /**
     * This Method
     * 
     * @param mmsReceiverService
     * @param parseId
     */
    public static void syncSentMMS(Context context, long luid) {

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("syncSentMMS called luid=" + luid);
        }
        if (!ApplicationSettings.getInstance().isProvisioned()) {
            return;
        }

        VMAEventHandler vmaHandler = ApplicationSettings.getInstance().getVMAEventHandler();

        String[] projection = new String[] { Mms.THREAD_ID, Mms.DATE, Mms.MESSAGE_ID };
        Cursor cursor = context.getContentResolver().query(
                ContentUris.withAppendedId(VZUris.getMmsUri(), luid), projection, null, null, null);
        if (cursor != null) {
            try {
                long threadId = 0;
                long date = 0;
                String msgId = null;
                if (cursor.moveToFirst()) {
                    threadId = cursor.getLong(0);
                    date = cursor.getLong(1);
                    msgId = cursor.getString(2);
                        // vmaEventHandler.telephonyMMSReceive(ContentUris.parseId(mmsUri), threadId, msgId,
                        // date);
                        vmaHandler.telephonyMMSSend(luid, threadId, msgId, date);
                }
            } finally {
                cursor.close();
            }
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("syncSentMMS starting vma sync with " + SyncManager.ACTION_START_VMA_SYNC);
        }
        Intent intent = new Intent(SyncManager.ACTION_START_VMA_SYNC);
        context.startService(intent);
    }

    /**
     * This Method
     * 
     * @param context
     */
    public static void markAllConversationAsDelete(Context context) {
        // if (!isDevicePaired(context)) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.warn("device is not paired");
        // }
        // return;
        // }
        // intent = new Intent(SyncManager.ACTION_SYNC_CHANGES);
        // intent.putExtra(SyncManager.EXTRA_MOD_TYPE, MessageEvents.DELETE_ALL_CONVERSATION);
        // context.startService(intent);
    }

    public static void markConversationsAsDelete(Context context, List<Long> threadIds) {
        // try {
        //
        // if (!isDevicePaired(context)) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.warn("device is not paired");
        // }
        // return;
        // }
        // ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        // for (long threadId : threadIds) {
        // Uri uri = ContentUris.withAppendedId(VZUris.getMmsSmsConversationUri(), threadId);
        // Cursor cursor = context.getContentResolver().query(uri, ID_MSG_TYPE_PROJECTON, null, null,
        // null);
        // // we need to send the deleted meesage ids
        // if (cursor != null) {
        // Builder builder = null;
        // while (cursor.moveToNext()) {
        // String msgType = cursor.getString(cursor
        // .getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN));
        // long luid = cursor.getLong(cursor.getColumnIndex(MmsSms._ID));
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.debug("adding delete event:luid=" + luid + ",type=" + msgType);
        // }
        // if (msgType.contains("sms")) {
        // builder = ContentProviderOperation.newInsert(MessageEvents.CONTENT_URI);
        // builder.withValue(MessageEvents.LUID, luid);
        // builder.withValue(MessageEvents.MSG_TYPE, MessageEvents.TYPE_SMS);
        // builder.withValue(MessageEvents.MOD_TYPE, MessageEvents.SMS_DELETED);
        // operations.add(builder.build());
        // } else if (msgType.contains("mms")) {
        // builder = ContentProviderOperation.newInsert(MessageEvents.CONTENT_URI);
        // builder.withValue(MessageEvents.LUID, luid);
        // builder.withValue(MessageEvents.MSG_TYPE, MessageEvents.TYPE_MMS);
        // builder.withValue(MessageEvents.MOD_TYPE, MessageEvents.MMS_DELETED);
        // operations.add(builder.build());
        // }
        // }
        // cursor.close();
        // }
        // }
        //
        // if (!operations.isEmpty()) {
        // try {
        // context.getContentResolver().applyBatch(SQLiteProvider.AUTHORITY, operations);
        // } catch (RemoteException e) {
        // if (Logger.IS_ERROR_ENABLED) {
        // Logger.error(false, "unable to delete :", e);
        // }
        // } catch (OperationApplicationException e) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.error(false, "unable to delete :", e);
        // }
        // }
        // intent = new Intent(SyncManager.ACTION_SYNC_CHANGES);
        // intent.putExtra(SyncManager.EXTRA_MOD_TYPE, MessageEvents.CONVERSATION_DELETE);
        // context.startService(intent);
        // }
        //
        // } catch (SQLException e) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.error(false, "Duplicate entry: ", e.getMessage());
        // }
        // }

    }

    public static void markConversationAsDelete(Context context, long threadId) {
        // try {
        //
        // if (!isDevicePaired(context)) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.warn("device is not paired");
        // }
        // return;
        // }
        // Uri uri = ContentUris.withAppendedId(VZUris.getMmsSmsConversationUri(), threadId);
        // ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        // Cursor cursor = context.getContentResolver().query(uri, ID_MSG_TYPE_PROJECTON, null, null, null);
        // // we need to send the deleted meesage ids
        // if (cursor != null) {
        // Builder builder = null;
        // while (cursor.moveToNext()) {
        // String msgType = cursor
        // .getString(cursor.getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN));
        // long luid = cursor.getLong(cursor.getColumnIndex(MmsSms._ID));
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.debug("adding delete event:luid=" + luid + ",type=" + msgType);
        // }
        // if (msgType.contains("sms")) {
        // builder = ContentProviderOperation.newInsert(MessageEvents.CONTENT_URI);
        // builder.withValue(MessageEvents.LUID, luid);
        // builder.withValue(MessageEvents.MSG_TYPE, MessageEvents.TYPE_SMS);
        // builder.withValue(MessageEvents.MOD_TYPE, MessageEvents.SMS_DELETED);
        // operations.add(builder.build());
        // } else if (msgType.contains("mms")) {
        // builder = ContentProviderOperation.newInsert(MessageEvents.CONTENT_URI);
        // builder.withValue(MessageEvents.LUID, luid);
        // builder.withValue(MessageEvents.MSG_TYPE, MessageEvents.TYPE_MMS);
        // builder.withValue(MessageEvents.MOD_TYPE, MessageEvents.MMS_DELETED);
        // operations.add(builder.build());
        // }
        // }
        // cursor.close();
        // if (!operations.isEmpty()) {
        // try {
        // ContentProviderResult[] results = context.getContentResolver().applyBatch(
        // SQLiteProvider.AUTHORITY, operations);
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.debug("Message size:" + ((results != null) ? results.length : 0));
        // for (ContentProviderResult contentProviderResult : results) {
        // Logger.debug("Result:" + contentProviderResult.toString());
        // }
        // }
        // } catch (RemoteException e) {
        // if (Logger.IS_ERROR_ENABLED) {
        // Logger.error(false, "unable to delete :", e);
        // }
        // } catch (OperationApplicationException e) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.error(false, "unable to delete :", e);
        // }
        // }
        // }
        // }
        // intent = new Intent(SyncManager.ACTION_SYNC_CHANGES);
        // intent.putExtra(SyncManager.EXTRA_MOD_TYPE, MessageEvents.CONVERSATION_DELETE);
        // context.startService(intent);
        // } catch (SQLException e) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.error(false, "Duplicate entry: ", e.getMessage());
        // }
        // }
    }

    /**
     * This Method
     * 
     * @param context
     */
    public static boolean isDevicePaired(Context context) {
        // prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // return prefs.getBoolean(Keys.IS_PAIRED, false);
        // return AndroidUtil.isPaired();

        return false;
    }

    /**
     * This Method
     * 
     * @param context
     * @param threadUri
     */
    public static void markConversationAsRead(Context context, Uri threadUri) {
        // try {
        //
        // // TODO , check the MMS type 128/132
        // if (!isDevicePaired(context)) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.warn("device is not paired");
        // }
        // return;
        // }
        // ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        // Cursor cursor = context.getContentResolver().query(threadUri, ID_MSG_TYPE_PROJECTON,
        // "(read=0 OR seen=0)", null, null);
        // if (cursor != null) {
        // Builder builder = null;
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.info("Unread Messages Count:" + cursor.getCount());
        // }
        // while (cursor.moveToNext()) {
        // String msgType = cursor
        // .getString(cursor.getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN));
        // long luid = cursor.getLong(cursor.getColumnIndex(MmsSms._ID));
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.debug("adding msg read event:luid=" + luid + ",type=" + msgType);
        // }
        // if (msgType.contains("sms")) {
        // builder = ContentProviderOperation.newInsert(MessageEvents.CONTENT_URI);
        // builder.withValue(MessageEvents.LUID, luid);
        // builder.withValue(MessageEvents.MSG_TYPE, MessageEvents.TYPE_SMS);
        // builder.withValue(MessageEvents.MOD_TYPE, MessageEvents.SMS_READ);
        // operations.add(builder.build());
        // } else if (msgType.contains("mms")) {
        // builder = ContentProviderOperation.newInsert(MessageEvents.CONTENT_URI);
        // builder.withValue(MessageEvents.LUID, luid);
        // builder.withValue(MessageEvents.MSG_TYPE, MessageEvents.TYPE_MMS);
        // builder.withValue(MessageEvents.MOD_TYPE, MessageEvents.MMS_READ);
        // operations.add(builder.build());
        // }
        // }
        // cursor.close();
        // }
        // if (!operations.isEmpty()) {
        // try {
        // ContentProviderResult[] results = context.getContentResolver().applyBatch(
        // SQLiteProvider.AUTHORITY, operations);
        // intent = new Intent(SyncManager.ACTION_SYNC_CHANGES);
        // intent.putExtra(SyncManager.EXTRA_MOD_TYPE, MessageEvents.CONVERSATION_READ);
        // context.startService(intent);
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.debug("Message size:" + ((results != null) ? results.length : 0));
        // for (ContentProviderResult contentProviderResult : results) {
        // Logger.debug("Result:" + contentProviderResult.toString());
        // }
        // }
        // } catch (RemoteException e) {
        // if (Logger.IS_ERROR_ENABLED) {
        // Logger.error(false, "unable to delete :", e);
        // }
        // } catch (OperationApplicationException e) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.error(false, "unable to delete :", e);
        // }
        // }
        // }
        //
        // } catch (SQLException e) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.error(false, "Duplicate entry: ", e.getMessage());
        // }
        // }
    }

    public static void sendReadReceipt(Context context, long luid) {
        // sendBroadcast(context,luid , SEND_READ_REPORT);
    }

    /**
     * This Method
     * 
     * @param context
     * @param threadUri
     */
    public static void markVMAMessageAsRead(Context context, Uri threadUri) {
        if (!ApplicationSettings.getInstance().isProvisioned()) {
            return;
        }
        boolean startService = false;
        // TODO Auto-generated method stub
        Cursor cursor = context.getContentResolver().query(threadUri, ID_MSG_TYPE_PROJECTON, "(read=0)",
                null, null);

        if (cursor != null) {
            try {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("markVMAMessageAsRead: on threadId, VMAHook :Unread Messages Count:"
                            + cursor.getCount());
                }
                while (cursor.moveToNext()) {
                    String msgType = cursor
                            .getString(cursor.getColumnIndex(MmsSms.TYPE_DISCRIMINATOR_COLUMN));
                    long luid = cursor.getLong(cursor.getColumnIndex(MmsSms._ID));
                    Uri uri = null;
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("markVMAMessageAsRead: adding msg read event:luid=" + luid + ",type="
                                + msgType);
                    }

                    VMAEventHandler vmaHandler = ApplicationSettings.getInstance().getVMAEventHandler();
                    if (msgType.contains("sms")) {
                        uri = ContentUris.withAppendedId(VZUris.getSmsUri(), luid);
                        vmaHandler.uiSMSRead(luid);
                        startService = true;
                    } else if (msgType.contains("mms")) {
                        uri = ContentUris.withAppendedId(VZUris.getMmsUri(), luid);
                        vmaHandler.uiMMSRead(luid);
                        startService = true;
                    }

                }
            } finally {
                cursor.close();
            }
        }

        if (startService) {
            Intent intent = new Intent(SyncManager.ACTION_START_VMA_SYNC);
            context.startService(intent);
        }
    }

    public static void markVMASmsAsDeleteOlderThanDate(Context context, long threadId, long latestDate) {
        if (!ApplicationSettings.getInstance().isProvisioned()) {
            return;
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("VMA Hook: attemting to delete from recycler.threadId=" + threadId + ", latestDate="
                    + latestDate);
        }
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getSmsUri(),
                new String[] { Sms._ID }, "thread_id=" + threadId + " AND date<=" + latestDate, null, null);

        List<Long> luids = new ArrayList<Long>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                luids.add(cursor.getLong(0));
            }
        }
        int count = luids.size();
        if (count > 0) {
            VMAEventHandler vmaHandler = ApplicationSettings.getInstance().getVMAEventHandler();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("VMA Hook: attemting to delete from recycler. items found=" + luids.size());
            }
            for (long luid : luids) {
                vmaHandler.uiSMSDelete(luid);
            }
            Intent intent = new Intent(SyncManager.ACTION_START_VMA_SYNC);
            context.startService(intent);
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("VMA Hook: attemting to delete from recycler. not items found");
            }
        }
    }

    public static void markVMAMmsAsDeleteOlderThanDate(Context context, long threadId, long latestDate) {
        if (!ApplicationSettings.getInstance().isProvisioned()) {
            return;
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("VMA Hook: attemting to delete from recycler.threadId=" + threadId + ", latestDate="
                    + latestDate);
        }
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getMmsUri(),
                new String[] { Mms._ID }, "thread_id=" + threadId + " AND date<=" + latestDate, null, null);

        List<Long> luids = new ArrayList<Long>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                luids.add(cursor.getLong(0));
            }
        }
        int count = luids.size();
        if (count > 0) {
            VMAEventHandler vmaHandler = ApplicationSettings.getInstance().getVMAEventHandler();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("VMA Hook: attemting to delete from recycler. items found=" + luids.size());
            }
            for (long luid : luids) {
                vmaHandler.uiMMSDelete(luid);
            }
            Intent intent = new Intent(SyncManager.ACTION_START_VMA_SYNC);
            context.startService(intent);
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("VMA Hook: attemting to delete from recycler. not items found=");
            }
        }
    }
}
