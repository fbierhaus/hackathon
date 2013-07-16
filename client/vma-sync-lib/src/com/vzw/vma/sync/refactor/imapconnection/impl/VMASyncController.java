/**
 * SyncController.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.refactor.imapconnection.impl;

import java.util.ArrayList;
import java.util.Set;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemAction;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemType;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao.SyncItemListener;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.messaging.vzmsgs.sync.SyncStatusCode;
import com.verizon.sync.SyncManager;
import com.verizon.sync.SyncStatusListener;
import com.verizon.sync.UiNotification;
import com.vzw.vma.sync.ConnectionManager;
import com.vzw.vma.sync.SyncClient;
import com.vzw.vma.sync.action.FecthAction;
import com.vzw.vma.sync.refactor.PDUDao;
import com.vzw.vma.sync.refactor.imapconnection.impl.VMASync.VMAConnectionType;

/**
 * This class is used control to send or receive the messages from VMA system.
 * 
 * @author Jegadeesan M
 * @Since Feb 13, 2013
 */
public class VMASyncController implements SyncItemListener {

    private Context context;
    private AppSettings settings;
    private SyncItemDao syncItemDao;
    private PDUDao pduDao;
    private VMAEventHandler vmaEventHandler;
    private VMASync idleConnection;
    // private VMASync fetchAttachementConnection;
    private ConnectionManager connectionManager;
    private boolean isTablet;
    private Intent statusIntent;
    private final Object attachementLock = new Object();
    protected ArrayList<SyncClient> workerThreads;
    private UiNotification uiNotification;
    private boolean loginFailed;
    private boolean isLowMemory;
    

    /**
     * Returns the Value of the isLowMemory
     * @return the  {@link boolean}
     */
    public boolean isLowMemory() {
        return isLowMemory;
    }

    /**
     * Set the Value of the field isLowMemory
     *
     * @param isLowMemory the isLowMemory to set
     */
    public void setLowMemory(boolean isLowMemory) {
        this.isLowMemory = isLowMemory;
    }

    /**
     * 
     * Constructor
     */
    public VMASyncController(Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("VMASyncController.ctor()");
        }
        workerThreads = new ArrayList<SyncClient>();

        VMASync sendConnection;
        VMASync fullsyncConnection;
        VMASync fetchConnection;

        this.context = context;
        this.settings = ApplicationSettings.getInstance(context);
        connectionManager = new ConnectionManager(settings);

        isTablet = VZUris.isTabletDevice();

        /*
         * XXX: Temp hack
         */
        ApplicationSettings as = (ApplicationSettings) settings;
        pduDao = (PDUDao) as.getPduDao();

        settings.getUiNotificationHandler();
        syncItemDao = settings.getSyncItemDao();
        syncItemDao.setSyncItemChangesListener(this);
        vmaEventHandler = settings.getVMAEventHandler();
        uiNotification = settings.getUiNotificationHandler();

        idleConnection = new VMASync(this, VMAConnectionType.IDLE);
        connectionManager.addListener(idleConnection);

        fetchConnection = new VMASync(this, VMAConnectionType.FETCH);
        connectionManager.addListener(fetchConnection);
        workerThreads.add(fetchConnection);

        if (isTablet) {
            sendConnection = new VMASync(this, VMAConnectionType.SEND);
            connectionManager.addListener(sendConnection);
            workerThreads.add(sendConnection);
        }

        if (settings.getBooleanSetting(AppSettings.KEY_FULLSYNC_DUMP_UIDS, true)) {
            fullsyncConnection = new VMASync(this, VMAConnectionType.FULLSYNC);
            connectionManager.addListener(fullsyncConnection);
            workerThreads.add(fullsyncConnection);
        }

        // fetchAttachementConnection = new VMASync(this,VMAConnectionType.FETCH_ATTACHEMENT);
        // connectionManager.addListener(fetchAttachementConnection);

        statusIntent = new Intent(SyncManager.ACTION_SYNC_STATUS);

    }

    public synchronized void startOndemandSync() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("startOndemandSync(): Manual, Retry or GCM.");
        }
        if (idleConnection.isIdling()) {
            idleConnection.abortIdle();
        } else {
            idleConnection.startOrWakeUp();
        }
        wakeUpWorkerThreads(syncItemDao.getUniqueItemPriorities());
    }

    private void wakeUpWorkerThreads(Set<ItemPriority> priorities) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Db has priority events of : " + priorities);
        }
        if (!priorities.isEmpty()) {
            for (SyncClient workerThread : workerThreads) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Checking if we need to wakeup - " + workerThread.getThreadName());
                }
                if (workerThread.isMine(priorities)) {
                    if (!workerThread.isRunning()) {
                        workerThread.startOrWakeUp();
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Thread is running not waking it up - "
                                    + workerThread.getThreadName());
                        }
                    }
                }
            }
        }
    }

    private void wakeUpWorkerThreads(ItemPriority priority) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Wakeup thread that can process " + priority);
        }
        String claimThread = null;
        boolean started = false;
        for (SyncClient workerThread : workerThreads) {

            if (workerThread.isMine(priority)) {
                claimThread = workerThread.getThreadName();
                if (!workerThread.isRunning()) {
                    workerThread.startOrWakeUp();
                    started = true;
                } else {
                }
                break;
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            // claimThread should not be null
            Logger.debug("Item claimed by " + claimThread
                    + " restarted (if claimThread != null, false means it was already running)=" + started);
        }

    }

    /**
     * This Method
     */
    public void startSync() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("startSync(): Triggering Sync");
        }
        idleConnection.startOrWakeUp();
        wakeUpWorkerThreads(syncItemDao.getUniqueItemPriorities());
    }

    /**
     * This Method is used to update the sync status
     * 
     * @param state
     */
    // public void syncstatus(int state) {
    //
    // switch (state) {
    // // case SyncStatusListener.FETCHING_MESSAGE:
    // // case SyncStatusListener.FETCHING_CHANGES:
    // // case SyncStatusListener.FETCHING_CONVERSATION:
    // // case SyncStatusListener.FETCHING_ATTACHEMENTS:
    // // case SyncStatusListener.SENDING_DELETE:
    // // case SyncStatusListener.SENDING_MESSAGE:
    // // case SyncStatusListener.SENDING_READ:
    // // case SyncStatusListener.SYNC_PREPARING:
    // // case SyncStatusListener.SYNC_PROVISIONING:
    // // case SyncStatusListener.BACKGROUND_LOGIN:
    // // case SyncStatusListener.FOREGROUND_LOGIN:
    // // case SyncStatusListener.NEW_MESSAGE:
    // // statusIntent.putExtra(EXTRA_STATUS, state);
    // // context.sendBroadcast(statusIntent);
    // // break;
    // case SyncStatusCode.:
    // // case SyncStatusListener.BACKGROUND_CONNECTION_WAIT:
    // case SyncStatusListener.VMA_CONNECTION_SIGNOUT:
    // boolean isAnyWorkerThreadRunning = false;
    // // sync with shutdown
    // synchronized(workerThreads) {
    // for (SyncClient myThread : workerThreads) {
    // if (myThread.isRunning()) {
    // isAnyWorkerThreadRunning = true;
    // break;
    // }
    // }
    // }
    // if (!isAnyWorkerThreadRunning) {
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug("Sending status broadcast.status=" + state);
    // }
    // settings.getUiNotificationHandler().syncStatus(state);
    // }
    // break;
    // default:
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug("Sending status broadcast.status=" + state);
    // }
    // settings.getUiNotificationHandler().syncStatus(state);
    // break;
    // }
    //
    // }

    /**
     * This Method is used to update the sending status
     * 
     * @param progress
     * @param total
     * @param type
     */
    public void sendingMessage(int progress, int total, int type) {

    }

    /**
     * This Method is used to update received message status.
     * 
     * @param progress
     * @param total
     * @param type
     */
    public void fetchingMessage(int progress, int total, int type) {

    }

    /**
     * This Method is used to fetch attachements
     * 
     * @param progress
     * @param total
     * @param type
     */
    public void fetchingAttachements(int progress, int total, int type) {

    }

    /**
     * Returns the Value of the context
     * 
     * @return the {@link Context}
     */
    public Context getContext() {
        return context;
    }

    /**
     * Returns the Value of the settings
     * 
     * @return the {@link AppSettings}
     */
    public AppSettings getSettings() {
        return settings;
    }

    /**
     * Returns the Value of the syncItemDao
     * 
     * @return the {@link SyncItemDao}
     */
    public SyncItemDao getSyncItemDao() {
        return syncItemDao;
    }

    /**
     * Returns the Value of the pduDao
     * 
     * @return the {@link PDUDao}
     */
    public PDUDao getPduDao() {
        return pduDao;
    }

    /**
     * Returns the Value of the vmaEventHandler
     * 
     * @return the {@link VMAEventHandler}
     */
    public VMAEventHandler getVmaEventHandler() {
        return vmaEventHandler;
    }

    /**
     * This Method
     * 
     * @param b
     */
    public void setLoginFailed() {

    }

    /**
     * This Method
     */
    public synchronized void shutdown() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("VMASyncController: shutdown()");
        }
        // Shutdown the connection manager
        connectionManager.shutdown();
        syncItemDao.setSyncItemChangesListener(null);

        idleConnection.shutdown();

        for (SyncClient myThread : workerThreads) {
            myThread.shutdown();
        }
        // Clearing from local cache, could probably skip it too
        synchronized (workerThreads) {
            workerThreads.clear();
        }
    }

    public synchronized void enqueue(Uri uri, ItemAction action) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("VMASyncController: enqueue: calling enq uri - " + uri);
        }
        if (uri == null) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("VMASyncController: Null uri ignoring enqueue");
            }
            return;
        }
        SyncItem item = new SyncItem();
        item.itemId = ContentUris.parseId(uri);
        item.action = action;
        item.type = getMesasgeType(uri);

        if (action == SyncItem.ItemAction.DELETE) {
            if (item.type == SyncItem.ItemType.SMS) {
                vmaEventHandler.uiSMSDelete(item.itemId);
            } else if (item.type == SyncItem.ItemType.MMS) {
                vmaEventHandler.uiMMSDelete(item.itemId);
            } else if (item.type == SyncItem.ItemType.CONVERSATION) {
                vmaEventHandler.conversationDelete(item.itemId);
            }
        } else if (action == SyncItem.ItemAction.READ) {
            if (item.type == SyncItem.ItemType.SMS) {
                vmaEventHandler.uiSMSRead(item.itemId);
            } else if (item.type == SyncItem.ItemType.MMS) {
                vmaEventHandler.uiMMSRead(item.itemId);
            } else if (item.type == SyncItem.ItemType.CONVERSATION) {
                vmaEventHandler.conversationRead(item.itemId);
            }
        } else if (action == SyncItem.ItemAction.SEND) {
            if (item.type == SyncItem.ItemType.SMS) {
                syncItemDao.addSendSmsEvent(item.itemId);
            } else if (item.type == SyncItem.ItemType.MMS) {
                syncItemDao.addSendMmsEvent(item.itemId, pduDao.mmsHasAttachement(item.itemId));
            }
        }

        if (Logger.IS_INFO_ENABLED) {
            Logger.info(getClass(), "Changes found : enqueued: uri=" + uri + ",item=" + item);
            // dao.dumpMapping();
        }
    }

    private ItemType getMesasgeType(Uri uri) {
        if (VZUris.getSmsAuthority().equalsIgnoreCase(uri.getAuthority())) {
            return ItemType.SMS;
        } else if (VZUris.getMmsAuthority().equalsIgnoreCase(uri.getAuthority())) {
            return ItemType.MMS;
        } else if (VZUris.getMmsSmsAuthority().equalsIgnoreCase(uri.getAuthority())) {
            return ItemType.CONVERSATION;
        }
        return ItemType.UNKNOWN;
    }

    /**
     * This Method
     * 
     * @return
     */
    public boolean isTablet() {
        return isTablet;
    }

    /**
     * Returns the Value of the attachementLock
     * 
     * @return the {@link Object}
     */
    public final Object getAttachementDownloadOrUploadLock() {
        return attachementLock;
    }

    /**
     * This Method
     * 
     * @return
     */
    public boolean hasPendingItemsToSync() {
        return (syncItemDao.hasPendingFetchItems() || syncItemDao.hasPendingSendItems());
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see
     * com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao.SyncItemListener#newSyncItem(com.verizon.messaging
     * .vzmsgs.provider.SyncItem.ItemPriority)
     */
    @Override
    public void onNewSyncItem(ItemPriority itemPriority) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Added new item in queue.staritng sync." + itemPriority);
        }
        wakeUpWorkerThreads(itemPriority);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao.SyncItemListener#newSyncItems(java.util.Set)
     */
    @Override
    public void newSyncItems(Set<ItemPriority> itemPriorities) {
        wakeUpWorkerThreads(itemPriorities);
    }

    public boolean canTheServiceBeShutdown() {
        return isLowMemory || !settings.isProvisioned() || !hasPendingItemsToSync()
                || settings.getBooleanSetting(AppSettings.KEY_VMA_ACCOUNT_SUSPENDED, false);
    }

    /**
     * Returns the Value of the uiNotification
     * 
     * @return the {@link UiNotification}
     */
    public UiNotification getUiNotification() {
        return settings.getUiNotificationHandler();
    }

    /**
     * This Method 
     */
    public void stopIdleConnection() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Account error : stopIdleConnection() ");
        }
        idleConnection.shutdown();
    }

}
