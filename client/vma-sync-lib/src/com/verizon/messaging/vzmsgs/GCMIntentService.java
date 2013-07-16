/**
 * GCMIntentService.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gcm.GCMBaseIntentService;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.sync.SyncController;
import com.verizon.sync.SyncManager;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jul 18, 2012
 */
public class GCMIntentService extends GCMBaseIntentService {

    /**
     * @param senderId
     *            Constructor
     */
    public GCMIntentService() {
        super(VMAProvision.SENDER_ID);

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.google.android.gcm.GCMBaseIntentService#onError(android.content.Context, java.lang.String)
     */
    @Override
    protected void onError(Context arg0, String arg1) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("GCMIntentService:onError()" + arg1);
        }
        // Toast.makeText(arg0, "GCMIntentService:onError()", Toast.LENGTH_LONG).show();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.google.android.gcm.GCMBaseIntentService#onMessage(android.content.Context,
     * android.content.Intent)
     */
    @Override
    protected void onMessage(Context context, Intent arg1) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.info("Got GCM Notification: intent=" + arg1);
        }
        // Toast.makeText(context, "GCMIntentService:onMessage()", Toast.LENGTH_SHORT).show();
        Bundle extras = arg1.getExtras();
        String unreadMsgCount = "0";
        if (extras != null) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("GCM: extras=" + extras);
            }
            unreadMsgCount = extras.getString("unread");
        }
        // Check for app visibility.If running in background then update the unread message count
        // else start the sync service
//        if (isApplicationInBackground(context)) {
//            if (Logger.IS_DEBUG_ENABLED) {
//                Logger.info("GCM Notification: App is running in background -.Updating unread message count.");
//            }
//            // Send broadcast to update the app icon with unread message count.
//            Intent intent = new Intent();
//            intent.setAction(Sync.MESSAGE_COUNT_CHANGED);
//            intent.putExtra(Sync.UNREAD_MESSAGE_COUNT, Integer.parseInt(unreadMsgCount));
//            context.sendBroadcast(intent);
//
//        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("GCM Notification: App is in foreground -Firing  vma sync.");
            }
//            Intent intent =new Intent(SyncManager.ACTION_START_VMA_SYNC);
//            intent.putExtra(SyncManager.EXTRA_SYNC_TYPE, SyncManager.SYNC_ON_DEMAND);
//            startService(intent);
            SyncController.getInstance().startVMASync(true);
//        }
        // // Temporary fix ,
        // Intent t = new Intent(Sync.INTENT_SYNC_STATUS);
        // t.putExtra(SyncManager.EXTRA_STATUS, Sync.SYNC_NEW_MESSAGE_ARRIVED);
        // context.sendBroadcast(t);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.google.android.gcm.GCMBaseIntentService#onRegistered(android.content.Context,
     * java.lang.String)
     */
    @Override
    protected void onRegistered(Context arg0, String newRegId) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("onRegistered()" + newRegId);
        }
        Intent intent = new Intent(SyncManager.ACTION_START_PROVISIONING);
        intent.putExtra(SyncManager.EXTRA_ACTION,VMAProvision.ACTION_PUSH_GCMID_TO_VMASERVER);
        intent.putExtra(SyncManager.EXTRA_GCM_REGISTRATION_ID, newRegId);
        startService(intent);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.google.android.gcm.GCMBaseIntentService#onUnregistered(android.content.Context,
     * java.lang.String)
     */
    @Override
    protected void onUnregistered(Context arg0, String arg1) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("GCMIntentService:onUnregistered()" + arg1);
        }
        // Toast.makeText(arg0, "GCMIntentService:onUnregistered()", Toast.LENGTH_LONG).show();

        stopSelf();
    }
//
//    /*
//     * Checks if the application is being sent in the background (i.e behind another application's Activity).
//     * 
//     * @param context the context
//     * 
//     * @return <code>true</code> if another application will be above this one.
//     */
//    public static boolean isApplicationInBackground(Context context) {
////        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
////        List<RunningTaskInfo> tasks = am.getRunningTasks(1);
////        if (!tasks.isEmpty()) {
////            ComponentName topActivity = tasks.get(0).topActivity;
////            if (!topActivity.getPackageName().equals(context.getPackageName())) {
////                return true;
////            }
////        }
//
//        return ApplicationSettings.getInstance().isApplicationInBackground();
//    }
}
