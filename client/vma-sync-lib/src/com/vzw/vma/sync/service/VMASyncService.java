/**
 * VMAService.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemAction;
import com.verizon.sync.SyncController;
import com.verizon.sync.SyncManager;
import com.vzw.vma.sync.refactor.imapconnection.impl.VMASyncController;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jun 14, 2012
 */
public class VMASyncService extends Service {

    private VMASyncController controller;
    private SyncHandler syncHandler;
    private AppSettings settings;

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(VMASyncService.class, " onCreate() ");
        }
        settings = ApplicationSettings.getInstance(this);
        HandlerThread thread = new HandlerThread("VMASyncService");
        thread.start();
        syncHandler = new SyncHandler(thread.getLooper());
        controller = new VMASyncController(this);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(VMASyncService.class, " onBind() ");
        }
        return null;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.content.ContextWrapper#stopService(android.content.Intent)
     */
    @Override
    public boolean stopService(Intent name) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("com.vzw.vma.sync.service.VMASyncService.stopService(Intent): action="
                    + name.getAction());
        }
        return super.stopService(name);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("com.vzw.vma.sync.service.VMASyncService.onDestroy()");
        }
        new Thread(new Runnable() {

            @Override
            public void run() {
                controller.shutdown();
            }
        }, "VMASyncService.onDestroy").start();
        syncHandler.getLooper().quit();
        super.onDestroy();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!settings.isProvisioned()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("VMA service is not activated. firing selfstop.startId=" + startId);
            }
            stopSelf(startId);
        } else {
            if (intent != null) {
                // in Samsung Galaxy we are getting Null Intent.
                Message msg = syncHandler.obtainMessage(SyncHandler.EVENT_NEW_INTENT);
                msg.arg1 = startId;
                msg.obj = intent;
                syncHandler.sendMessage(msg);

            }
        }
        return START_NOT_STICKY;
    }

    protected void onHandleIntent(Intent intent) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(VMASyncService.class, " onHandleIntent() :-" + intent.getAction());
        }
        Uri uri = null;
        if (intent.hasExtra(SyncManager.EXTRA_URI)) {
            uri = intent.getExtras().getParcelable(SyncManager.EXTRA_URI);
        }
        boolean isOnDemandSync = false;
        if (intent.hasExtra(SyncManager.EXTRA_SYNC_TYPE)) {
            int syncType = intent.getIntExtra(SyncManager.EXTRA_SYNC_TYPE, SyncManager.SYNC_NORMAL);
            isOnDemandSync = (syncType == SyncManager.SYNC_ON_DEMAND)
                    || (syncType == SyncManager.SYNC_MANUAL);
        }

        if (SyncManager.ACTION_SEND_MSG.equalsIgnoreCase(intent.getAction())) {
            controller.enqueue(uri, ItemAction.SEND);
        } else if (SyncManager.ACTION_MSG_DELETED.equalsIgnoreCase(intent.getAction())) {
            controller.enqueue(uri, ItemAction.DELETE);
        } else if (SyncManager.ACTION_MARK_AS_READ.equalsIgnoreCase(intent.getAction())) {
            controller.enqueue(uri, ItemAction.READ);
        } else if (SyncManager.ACTION_XCONV_DELETED.equalsIgnoreCase(intent.getAction())) {
            controller.enqueue(uri, ItemAction.DELETE);
        } else if (SyncManager.ACTION_DELETE_OLD_MESSAGE.equalsIgnoreCase(intent.getAction())) {
            // syncManager.deleteOldConversations();
        } else if (SyncManager.ACTION_STOP_VMA_SYNC.equalsIgnoreCase(intent.getAction())) {

            boolean canShutdown = false;
            canShutdown = controller.canTheServiceBeShutdown();
            if (canShutdown) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("No Pending items to sync. firing shutdown");
                }
                controller.shutdown();
                stopSelf();
            } else {
                // syncManager.stopIdle();
                retryShutdownAfterDelay();
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("pending items found. Ignoring shutdown");
                }
            }
        } else if (SyncManager.ACTION_START_VMA_SYNC.equalsIgnoreCase(intent.getAction())) {
            if (isOnDemandSync) {
                controller.startOndemandSync();
            } else {
                controller.startSync();
            }
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("VMASyncService.onIntentHandler: Ignoring unknown action : "
                        + intent.getAction());
            }
        }
    }

    private void retryShutdownAfterDelay() {
        SyncController.getInstance().retryStopLater();
    }

    public class SyncHandler extends Handler {
        public final static int EVENT_QUIT = 0;
        public final static int EVENT_NEW_INTENT = 1;

        SyncHandler(Looper looper) {
            super(looper);
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            Intent intent = (Intent) msg.obj;
            if (msg.what == EVENT_QUIT) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Stopping the VMA sync service.");
                }
                stopSelf();
            } else if (msg.what == EVENT_NEW_INTENT) {
                onHandleIntent(intent);
            }
        }
    }

}
