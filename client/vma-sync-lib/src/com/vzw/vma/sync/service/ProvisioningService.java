/**

o * ProvisioningService.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.service;

import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.handler.HandlerTask;
import com.verizon.messaging.vma.provision.ProvisionManager;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.VMAProvision;
import com.verizon.messaging.vzmsgs.sync.AppErrorCodes;
import com.verizon.messaging.vzmsgs.sync.SyncStatusCode;
import com.verizon.sync.SyncManager;

import static com.verizon.messaging.vzmsgs.AppSettings.EXTRA_AUTO_FORWARD_SYNCANCHOR;
import static com.verizon.messaging.vzmsgs.AppSettings.EXTRA_AUTO_REPLY_SYNCANCHOR;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_HANDSET_PROVISIONING_IN_BACKROUND;

public class ProvisioningService extends Service {

    private static final int PROVISION_HANDLER = 1001;
    private ProvisionHandler handler;
    private ProvisionManager manager;
    private AppSettings settings;
    private ArrayList<HandlerTask> processingQueue = new ArrayList<HandlerTask>();

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ProvisioningService.class, " onCreate() ");
        }
        settings = ApplicationSettings.getInstance(this);
        // Looper
        HandlerThread thread = new HandlerThread("ProvisionService");
        thread.start();
        handler = new ProvisionHandler(thread.getLooper());
        manager = new ProvisionManager(this);

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ProvisioningService.class, " onBind() ");
        }
        return manager;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        handler.getLooper().quit();
        super.onDestroy();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onDestroy  done");
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onStartCommand: startId = " + startId + ", intent = ");
        }
        if (intent != null) {
            Message msg = handler.obtainMessage(PROVISION_HANDLER);
            msg.arg1 = startId;
            msg.obj = intent;
            handler.sendMessage(msg);
        }
        return Service.START_NOT_STICKY;
    }

    public final class ProvisionHandler extends Handler {
        /**
         * @param looper
         *            Constructor
         */
        public ProvisionHandler(Looper looper) {
            super(looper);
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message msg) {
            HandlerTask task = new HandlerTask(msg);
            synchronized (processingQueue) {
                processingQueue.add(task);
            }
            try {
                execute(task);
            } catch (Exception e) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error(true, getClass(), e);
                }
            } finally {
                stopIfIdle(task);
            }
        }

        /**
         * This Method
         * 
         * @param intent
         */
        private void execute(HandlerTask task) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("execute() " + task.intent.getAction());
            }
            int action = 0;
            if (task.intent.hasExtra(SyncManager.EXTRA_ACTION)) {
                action = task.intent.getIntExtra(SyncManager.EXTRA_ACTION, 0);
            }
            if (action == VMAProvision.ACTION_ASSISTANT_QUERY) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("firing the message assistant sync");
                }
                long autoForwardSyncAnchor = -1;
                if (task.intent.hasExtra(EXTRA_AUTO_FORWARD_SYNCANCHOR)) {
                    autoForwardSyncAnchor = task.intent.getLongExtra(EXTRA_AUTO_FORWARD_SYNCANCHOR, -1);
                }
                long autoReplySyncAnchor = -1;
                if (task.intent.hasExtra(EXTRA_AUTO_REPLY_SYNCANCHOR)) {
                    autoReplySyncAnchor = task.intent.getLongExtra(EXTRA_AUTO_REPLY_SYNCANCHOR, -1);
                }
                manager.queryMessagingAssistantfeatures(autoForwardSyncAnchor, autoReplySyncAnchor);
                // } else if (action == VMAProvision.ACTION_REGISTER_GCM) {
                // manager.registerGCMToken();
            } else if (action == VMAProvision.ACTION_PUSH_GCMID_TO_VMASERVER) {
                if (task.intent.hasExtra(SyncManager.EXTRA_GCM_REGISTRATION_ID)) {
                    manager.registerGCMToken(task.intent
                            .getStringExtra(SyncManager.EXTRA_GCM_REGISTRATION_ID));
                } else {
                    // manager.registerGCMToken();
                }
            } else if (action == VMAProvision.ACTION_AUTO_PROVISION_HANDSET) {
                if (settings.getBooleanSetting(KEY_VMA_HANDSET_PROVISIONING_IN_BACKROUND, false)) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Already provisioning in progress. ignoring action" + action);
                    }
                    return;
                }
                settings.put(KEY_VMA_HANDSET_PROVISIONING_IN_BACKROUND, true);
                String mdn = ApplicationSettings.getInstance().getLocalPhoneNumber();
                String deviceModel = Build.MANUFACTURER + "-" + Build.MODEL;
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("doHandsetProvisioning : mdn: " + mdn + "deviceModel: " + deviceModel);
                }
                int result = manager.doHandsetProvisioning(mdn, deviceModel);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Server response for handset is :" + result);
                }
                
                int status = (result == AppErrorCodes.VMA_PROVISION_OK) ? SyncStatusCode.VMA_AUTO_PROVISION_SUCCESS
                        : SyncStatusCode.VMA_AUTO_PROVISION_FAILED;
                
                settings.put(KEY_VMA_HANDSET_PROVISIONING_IN_BACKROUND, false);
                Intent intent = new Intent(SyncManager.ACTION_SYNC_STATUS);
                intent.putExtra(SyncManager.EXTRA_STATUS, status);
                sendBroadcast(intent);
                
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Unknown action =" + action);
                }
            }

        }

    }

    /**
     * This Method
     */
    private synchronized void stopIfIdle(HandlerTask task) {
        // Remove the current task and stop the service
        processingQueue.remove(task);
        stopIfIdle(task.serviceStartId);
    }

    private synchronized void stopIfIdle(int startId) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Pending task." + processingQueue.size());
        }
        if (processingQueue.isEmpty()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Calling stopSelf()");
            }
            stopSelf(startId);
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Pending task found ignoring the stop.");
            }
        }

    }

}
