/**
 * SyncController.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.sync;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.VMAProvision;

/**
 * <P>
 * This class is responsible for starting and stopping the service. It can be a singleton class with methods
 * that can be called to start/stop the service and its job is to send intents to the SyncService.
 * Addiytionally it listens to background & foreground events and attempts to start/stop the service in those
 * scenarios. If the app goes to the background it posts a Handler event after a 60 second delay to stop the
 * service. If the app comes back into foreground within 60 seconds it cancels the shutdown and does nothing.
 * Otherwise it posts an intent to start the sync service.
 * 
 * </P>
 * 
 * @author Jegadeesan
 * @Since Jan 16, 2013
 */
public class SyncController {

    /**
	 * 
	 */
    private static final int DEFAULT_DELAY = 180000;
    private static final int SCREEN_LOCK_DELAY = 60000;
    private static final int ONDEMAND_SYNC_DELAY = 5000;

    private static SyncController instance;
    private Context context;
    private AppSettings settings;
    private SyncServiceStopHandler syncStopHandler;
    private OnDemandSyncDelayHandler onDemandSyncHandler;
    private final static int STOP_SERVICE = 1;
    private final static int START_ONDEMAND_SYNC = 2;
    private PowerManager pm;

    private SyncController(AppSettings settings) {
        this.context = settings.getContext();
        this.settings = settings;
        this.syncStopHandler = new SyncServiceStopHandler();
        this.onDemandSyncHandler = new OnDemandSyncDelayHandler();
    }

    /**
     * This Method is used to get the {@link SyncController}
     * 
     * @return {@link SyncController}
     */
    public static SyncController getInstance() {
        return instance;
    }

    /**
     * This Method is used to get the {@link SyncController}
     * 
     * @param context
     * @return {@link SyncController}
     */
    public static SyncController getInstance(AppSettings settings) {
        if (instance == null) {
            instance = new SyncController(settings);
        }
        return instance;
    }

    /**
     * This Method is used to start the sync vma Sync
     * 
     * @param selfStop
     */
    public synchronized void startVMASync(boolean onDemand) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("SyncController.startVMASync():isApplicationInBackground="
                    + settings.isApplicationInBackground() + ",onDemand=" + onDemand + ",keyguard locked="
                    + settings.isKeyguardGuardLocked());
        }

        if (settings.isProvisioned()) {
//            if (settings.isKeyguardGuardLocked()) {
//                if (Logger.IS_DEBUG_ENABLED) {
//                    Logger.debug("SyncController.startVMASync():device locked ignoring the ondemand sync");
//                }
//                return;
//            }
            pushGCMToken();
            if (onDemand) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("GCM/ONDemand Sync delayed for 5 sec");
                }
                onDemandSyncHandler.removeMessages(START_ONDEMAND_SYNC);
                onDemandSyncHandler.sendEmptyMessageDelayed(START_ONDEMAND_SYNC, ONDEMAND_SYNC_DELAY);
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Ignoring GCM/ONDemandSync notification stopItself=" + onDemand);
                }
            }

        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(),
                        "startVMASync(): Devices is not provisioned or .isApplicationInBackground="
                                + settings.isApplicationInBackground() + ",onDemand=" + onDemand);
            }
        }
    }

    /**
     * This Method
     */
    private void pushGCMToken() {
        String newRegId = settings.getStringSettings(ApplicationSettings.KEY_PUSH_GCM_TOKEN_TOSEND);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Updating the GCM token on server: newRegId=" + newRegId);
        }
        if (VZUris.isTabletDevice() && !TextUtils.isEmpty(newRegId)) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("New GCM token=" + newRegId);
            }
            Intent intent = new Intent(SyncManager.ACTION_START_PROVISIONING);
            intent.putExtra(SyncManager.EXTRA_ACTION, VMAProvision.ACTION_PUSH_GCMID_TO_VMASERVER);
            intent.putExtra(SyncManager.EXTRA_GCM_REGISTRATION_ID, newRegId);
            context.startService(intent);
        }
    }

    /**
     * This Method is used to start the VMA Sync
     */
    public synchronized void startVMASync() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "startVMASync()");
        }
        if (settings.isProvisioned()) {
            pushGCMToken();
            syncStopHandler.removeMessages(STOP_SERVICE);
            context.startService(new Intent(SyncManager.ACTION_START_VMA_SYNC));
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Devices is not provisioned. Ignoring the start.");
            }
        }
    }

    /**
     * This Method is used to stop the VMA Sync
     */
    public synchronized void stopVMASync() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Stop VMA sync");
        }
        context.startService(new Intent(SyncManager.ACTION_STOP_VMA_SYNC));
    }

    public void retryStopLater() {
        if (settings.isApplicationInBackground() || settings.isKeyguardGuardLocked()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("retryStopLater() : Sync stop");
            }
            syncStopHandler.removeMessages(STOP_SERVICE);
            syncStopHandler.sendEmptyMessageDelayed(STOP_SERVICE, DEFAULT_DELAY);
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("retryStopLater(). Application in foreground.ignoring the retry.");
            }
        }

    }

    /**
     * This Method is used
     */
    public synchronized void onAppStatusChanged(boolean isAppInBackground, boolean screenLocked) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(),
                    "onAppStatusChanged():isAppInBackground=" + isAppInBackground + ",screenLocked="
                            + screenLocked + ",keyguard locked=" + settings.isKeyguardGuardLocked());
        }
        if (isAppInBackground || screenLocked) {
            // App in background
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("activityStatusChanged():App in background: stop the sync 180 sec");
            }
            // Remove the unprocessed the message
            syncStopHandler.removeMessages(STOP_SERVICE);
            syncStopHandler.sendEmptyMessageDelayed(STOP_SERVICE, DEFAULT_DELAY);
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("activityStatusChanged():App in foreground. start the sync service.");
            }
            // Remove the unprocessed the message
            syncStopHandler.removeMessages(STOP_SERVICE);
            startVMASync();
        }
    }

    private class SyncServiceStopHandler extends Handler {

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Executing delay Handler");
            }
            stopVMASync();
        }
    }

    /**
     * This class used to schedule the ondemand sync.
     * 
     * @author Jegadeesan M
     * @Since Apr 8, 2013
     */
    private class OnDemandSyncDelayHandler extends Handler {

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Firing onDemand Sync");
            }
            Intent intent = new Intent(SyncManager.ACTION_START_VMA_SYNC);
            intent.putExtra(SyncManager.EXTRA_SYNC_TYPE, SyncManager.SYNC_ON_DEMAND);
            context.startService(intent);
            
            if(settings.isApplicationInBackground()){
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("App in background: stop the vma sync service after 180 sec.");
                }
                retryStopLater();
            }
        }
    }

}
