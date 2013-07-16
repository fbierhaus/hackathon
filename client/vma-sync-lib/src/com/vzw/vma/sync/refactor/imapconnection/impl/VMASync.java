/**
 * VMASync.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.refactor.imapconnection.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.MessagingException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.PowerManager;
import android.telephony.SmsManager;

import com.google.android.gcm.GCMRegistrar;
import com.strumsoft.android.commons.logger.Logger;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.VMAProvision;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemAction;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.messaging.vzmsgs.sync.AppErrorCodes;
import com.verizon.messaging.vzmsgs.sync.SyncStatusCode;
import com.verizon.mms.MmsException;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.sync.SyncManager;
import com.verizon.sync.UiNotification;
import com.vzw.vma.message.VMAStore;
import com.vzw.vma.message.VmaSelectResponse;
import com.vzw.vma.message.impl.VMAStoreJavaMailImpl;
import com.vzw.vma.sync.SyncClient;
import com.vzw.vma.sync.action.FastSyncAction;
import com.vzw.vma.sync.action.FecthAction;
import com.vzw.vma.sync.action.FullSyncAction;
import com.vzw.vma.sync.action.SendAction;
import com.vzw.vma.sync.action.UpdateAction;
import com.vzw.vma.sync.refactor.PDUDao;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Feb 12, 2013
 */
public class VMASync implements SyncClient {

    /**
     * 
     */
    private static final int MAX_DB_QUERY_SIZE = 50;
    private static final int MAX_ITEM_RETRY = 3;
    private static final int MAX_QUEUE_SIZE = 1;
    public static final int ITEM_SUCCESS = 0;
    public static final int ITEM_TEMPORARY_FAILURE = 1;
    public static final int ITEM_PERMANENT_FAILURE = 2;

    private static final int ACCOUNT_SUSPENDED = 3;
    private static final int ACCOUNT_INVALID = 4;
    private static final int INVALID_SESSION_RELOGIN = 5;
    private static final int ACCOUNT_INSUFFICIENT_FUNDS = 6;
    private Context context;
    private boolean isRunning;
    private boolean isConnected;
    private boolean queuedItemToSend;
    private boolean queueditemToFetch;
    private boolean isShutdown;
    private VMAStore store;
    private SyncItemDao syncDao;
    private PDUDao pduDao;
    private VMAEventHandler eventHandler;
    private AppSettings settings;
    private VMASyncController syncController;
    private VMAConnectionType connectionType;
    private Object waitLock = new Object();
    private Thread thread;
    // IDLE variables
    boolean idleAbortedByTimer = false;
    private long fullsyncLastUid = -1;
    private boolean idling;
    private Timer timer = null;
    private static int idleSelfCount;
    private static int fecthSelfCount;
    private static int sendSelfCount;
    private static int fullsyncSelfCount;

    private boolean isTablet;
    private boolean imapConnectionIssue;
    private boolean idleSleep;
    private ContentResolver resolver;
    // Verizon retry scheme needs to be 30 sec, 3 min, 5 min, and 8 min
    private final int[] DEFAULT_RETRY_SCHEME;
    // private static final int[] DEFAULT_RETRY_SCHEME = { 1 * 1000, 1 * 30 * 1000, 1 * 60 * 1000,
    // 3 * 60 * 1000, 5 * 60 * 1000, 8 * 60 * 1000 };

    private int connectionFailureRetry;
    private final int MAX_IMAP_CONNECTION_RETRY_COUNT;;

    public enum VMAConnectionType {
        IDLE, SEND, FETCH, FETCH_ATTACHEMENT, FULLSYNC
    }

    private String threadName;
    private String mdn;
    private UiNotification uiNotification;
    PowerManager pm;

    /**
     * 
     * Constructor
     */
    public VMASync(VMASyncController syncController, VMAConnectionType connectionType) {
        if (connectionType == VMAConnectionType.IDLE) {
            DEFAULT_RETRY_SCHEME = new int[] { 1 * 1000, 1 * 30 * 1000, 1 * 60 * 1000 };
        } else {
            DEFAULT_RETRY_SCHEME = new int[] { 1 * 1000, 1 * 30 * 1000, 1 * 60 * 1000, 3 * 60 * 1000,
                    5 * 60 * 1000, 8 * 60 * 1000 };
        }
        MAX_IMAP_CONNECTION_RETRY_COUNT = DEFAULT_RETRY_SCHEME.length;
        this.syncController = syncController;
        threadName = connectionType.name();
        this.isTablet = syncController.isTablet();
        this.connectionType = connectionType;
        this.context = syncController.getContext();
        settings = syncController.getSettings();
        syncDao = syncController.getSyncItemDao();
        pduDao = syncController.getPduDao();
        eventHandler = syncController.getVmaEventHandler();
        resolver = context.getContentResolver();
        uiNotification = syncController.getUiNotification();
        mdn = settings.getMDN();
        pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    @Override
    public String getThreadName() {
        return threadName;
    }

    private void createNewThread() {
        isShutdown = false;
        createIMAPConnection();
        thread = new Thread(this);
        if (connectionType == VMAConnectionType.IDLE) {
            threadName = connectionType + "-" + (++idleSelfCount);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
        } else if (connectionType == VMAConnectionType.FETCH) {
            threadName = connectionType + "-" + (++fecthSelfCount);
            thread.setPriority(Thread.MIN_PRIORITY);
        } else if (connectionType == VMAConnectionType.FULLSYNC) {
            threadName = connectionType + "-" + (++fullsyncSelfCount);
            thread.setPriority(isTablet ? Thread.MIN_PRIORITY : Thread.NORM_PRIORITY - 1);
        } else if (connectionType == VMAConnectionType.SEND) {
            threadName = connectionType + "-" + (++sendSelfCount);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
        }
        thread.setName(threadName);
        thread.start();
    }

    public synchronized final void startOrWakeUp() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.info("startOrWakeUp(): " + threadName);
        }
        if (thread == null && !isShutdown) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("startOrWakeUp(): " + threadName + "-Creating new thread");
            }
            createNewThread();
        } else {
            synchronized (waitLock) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.info("startOrWakeUp(): " + threadName + "-Notifying wait");
                }
                waitLock.notify();
            }
        }
    }

    /**
     * This Method
     */
    private void createIMAPConnection() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createIMAPConnection:" + connectionType + " mdn=" + mdn + ",token="
                    + settings.getDecryptedLoginToken());
        }
        String loginMDN = mdn;

        if (VMAConnectionType.SEND == connectionType) {
            loginMDN = mdn + "_SEND-ONLY";
        }

        String actionPrefix = "A";
        if (isTablet) {
            actionPrefix = VMAStore.IMAP_TABLET_ACTION_TAG + VMAStore.IMAP_SERVER_API_VERSION;
        } else {
            actionPrefix = VMAStore.IMAP_HANDSET_ACTION_TAG + VMAStore.IMAP_SERVER_API_VERSION;
        }

        store = new VMAStoreJavaMailImpl(context, settings.getImapHost(), settings.getImapPort(),
                settings.isSSLEnabled(), mdn, loginMDN, settings.getDecryptedLoginToken(),
                settings.isIMAPLogEnabled(), actionPrefix);

        // Need to handle here , else we will use the wrong syncsession object
        if (connectionType == VMAConnectionType.IDLE) {
            FastSyncAction.getInstance(settings, store, syncDao, eventHandler);
        } else if (connectionType == VMAConnectionType.FETCH) {
            FecthAction.getInstance(store, eventHandler, pduDao, syncDao, uiNotification, settings);
            UpdateAction.getInstance(syncDao, store);
        } else if (connectionType == VMAConnectionType.FETCH_ATTACHEMENT) {
            FecthAction.getInstance(store, eventHandler, pduDao, syncDao, uiNotification, settings);
            UpdateAction.getInstance(syncDao, store);
        } else if (connectionType == VMAConnectionType.FULLSYNC) {
            FullSyncAction.getInstance(settings, store, syncDao, context);
        } else if (connectionType == VMAConnectionType.SEND) {
            SendAction.getInstance(store, eventHandler, pduDao);
        }
    }

    /**
     * Returns the Value of the isConnected
     * 
     * @return the {@link boolean}
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Returns the Value of the isRunning
     * 
     * @return the {@link boolean}
     */
    public synchronized boolean isRunning() {
        return isRunning;
    }

    /**
     * Returns the Value of the isShutdown
     * 
     * @return the {@link boolean}
     */
    public boolean isShutdown() {
        return isShutdown;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.DataConnectivityListener#onConnectionChanged(boolean, int)
     */
    @Override
    public void onConnectionChanged(boolean isConnected, int type) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("onConnectionChanged: " + isConnected + " for type = " + type);
        }
        this.isConnected = isConnected;
        if (isConnected) {
            synchronized (waitLock) {
                waitLock.notifyAll();
            }
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("invoked run()");
        }
        while (!isShutdown) {
            try {
                synchronized (waitLock) {

                    isRunning = true;
                }
                execute();
                synchronized (waitLock) {
                    isRunning = false;
                }
                if (connectionType == VMAConnectionType.IDLE && !isConnected) {
                    // If idle wait or network error , we are sending the connection drop notification to ui
                    // syncController.syncstatus(SyncStatusListener.FOREGROUND_CONNECTION_WAIT);
                    uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_IDLE_NO_NETWORK);
                } else {
                    // sending other threads logout status
                    // if (connectionType != VMAConnectionType.FULLSYNC) {
                    // syncController.syncstatus(SyncStatusListener.VMA_CONNECTION_SIGNOUT);
                    if (connectionType == VMAConnectionType.IDLE) {
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_IDLE_LOGOUT);
                    } else if (connectionType == VMAConnectionType.FETCH) {
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FETCH_LOGOUT);
                    } else if (connectionType == VMAConnectionType.SEND) {
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_SEND_LOGOUT);
                    } else if (connectionType == VMAConnectionType.FULLSYNC) {
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FULLSYNC_LOGOUT);
                    }
                    // }
                }
                synchronized (waitLock) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("inside run(): waiting. isConnected=" + isConnected);
                        dumpPendingItems();
                    }
                    if (isShutdown) {
                        break;
                    }

                    // if any IMAP connection issue , we have to retry the connection based on VZW retry
                    // schema.
                    if (idleSleep) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("inside infinitie wait. isConnected=" + isConnected + ",idleSleep="
                                    + idleSleep);
                            dumpPendingItems();
                        }
                        waitLock.wait();
                    } else if (imapConnectionIssue && isConnected) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.info("got IO exception. waiting to retry. waittime="
                                    + DEFAULT_RETRY_SCHEME[connectionFailureRetry] + "ms");
                        }
                        waitLock.wait(DEFAULT_RETRY_SCHEME[connectionFailureRetry]);

                        if (connectionFailureRetry < (MAX_IMAP_CONNECTION_RETRY_COUNT - 1)) {
                            connectionFailureRetry++;
                        } else {
                            // Failure after , 5 retry
                            uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_IDLE_RELEASE);
                        }
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("inside infinitie wait. isConnected=" + isConnected
                                    + ",connectionFailureRetry=" + connectionFailureRetry);
                            dumpPendingItems();
                        }
                        waitLock.wait();
                    }

                }
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("inside run(): released. isConnected=" + isConnected);
                }
                // } catch (SQLiteDiskIOException e) {
                // if (Logger.IS_DEBUG_ENABLED) {
                // Logger.debug("inside run(): SQLiteDiskIOException. isConnected=" + isConnected);
                // }
                //
                // } catch (SQLiteDatabaseCorruptException e) {
                // if (Logger.IS_DEBUG_ENABLED) {
                // Logger.debug("inside run(): SQLiteDatabaseCorruptException. isConnected=" + isConnected);
                // }
            } catch (SQLiteException e) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("inside run(): SQLiteException. isConnected=" + isConnected);
                }
                if (SqliteWrapper.isLowMemory(e)) {
                    uiNotification.syncStatus(AppErrorCodes.VMA_LOW_MEMORY);
                    syncController.setLowMemory(true);
                    isShutdown = true;
                    context.startService(new Intent(SyncManager.ACTION_STOP_VMA_SYNC));
                    break;
                }
                Logger.postErrorToAcraIfDebug(threadName + " inside run():", e);
            } catch (Exception e) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error("inside run():", e);
                }
                Logger.postErrorToAcraIfDebug(threadName + " inside run():", e);
            } finally {
                synchronized (waitLock) {
                    isRunning = false;
                }
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("shutdown() done." + connectionType + ":"
                    + ((thread != null) ? thread.getName() : connectionType) + "isConnected=" + isConnected
                    + ",isShutdown=" + isShutdown);
        }
    }

    /**
     * This Method
     */
    private void dumpPendingItems() {
        String sendPending = SyncItem._PRIORITY + "<=" + ItemPriority.SEND_MAX.getValue() + " AND "
                + SyncItem._PRIORITY + ">" + ItemPriority.SEND_MIN.getValue();

        String fetch = SyncItem._PRIORITY + "<=" + ItemPriority.ONDEMAND_MAX.getValue() + " AND "
                + SyncItem._PRIORITY + ">" + ItemPriority.FULLSYNC_MIN.getValue();

        String fetchDeffered = SyncItem._PRIORITY + "==" + ItemPriority.DEFFERED.getValue();

        Logger.debug("==============================================");
        Logger.debug("Send items");
        Logger.debug("Send Pending  :" + getCount(SyncItem.CONTENT_URI, sendPending));
        Logger.debug("Fetch Pending :" + getCount(SyncItem.CONTENT_URI, fetch));
        Logger.debug("Deffered      :" + getCount(SyncItem.CONTENT_URI, fetchDeffered));
        Logger.debug("==============================================");

    }

    /**
     * This Method
     * 
     * @param pendingSyncItems
     * @param selection
     * @param sortOrder
     * @return
     */
    private int getCount(Uri uri, String selection) {
        int res = 0;
        Cursor c = resolver.query(SyncItem.CONTENT_URI, null, selection, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                res = c.getCount();
            }
            c.close();
        }
        return res;
    }

    private void processSyncItems(List<SyncItem> items) throws IOException, ProtocolException {
        boolean isDone = false;
        if (items == null) {
            return;
        }
        List<SyncItem> itemsToRemove = new ArrayList<SyncItem>();

        do {
            if (items.isEmpty() || !isConnected || isShutdown) {
                break;
            }
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("processSyncItems: number to process = " + items.size());
            }
            for (SyncItem syncItem : items) {
                int error = ITEM_SUCCESS;
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("processSyncItems: item=" + syncItem);
                }
                if (!isConnected || isShutdown) {
                    break;
                }
                try {
                    switch (syncItem.action) {
                    case SEND_SMS:
                        // uiNotification.syncStatus(SyncStatusListener.SENDING_MESSAGE);
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_SEND_SMS);
                        error = SendAction.getInstance().sendSMS(syncItem) ? ITEM_SUCCESS
                                : ITEM_TEMPORARY_FAILURE;
                        break;
                    case SEND_MMS:
                        // uiNotification.syncStatus(SyncStatusListener.SENDING_MESSAGE);
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_SEND_MMS);
                        error = SendAction.getInstance().sendMMS(syncItem) ? ITEM_SUCCESS
                                : ITEM_TEMPORARY_FAILURE;
                        break;
                    case READ:
                        // uiNotification.syncStatus(SyncStatusListener.SENDING_READ);
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_SEND_READ);
                        error = UpdateAction.getInstance().readMessages(syncItem) ? ITEM_SUCCESS
                                : ITEM_TEMPORARY_FAILURE;
                        break;
                    case DELETE:
                        // uiNotification.syncStatus(SyncStatusListener.SENDING_DELETE);
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_SEND_DELETE);
                        error = UpdateAction.getInstance().deletedMessage(syncItem) ? ITEM_SUCCESS
                                : ITEM_TEMPORARY_FAILURE;
                        break;
                    case FETCH_MESSAGE_HEADERS:
                        // uiNotification.syncStatus(SyncStatusListener.FETCHING_MESSAGE);
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FETCH_MSG);

                        error = FecthAction.getInstance().fetchMessageHeaders(syncItem) ? ITEM_SUCCESS
                                : ITEM_TEMPORARY_FAILURE;
                        break;
                    case FETCH_MESSAGE:
                        // uiNotification.syncStatus(SyncStatusListener.FETCHING_MESSAGE);
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FETCH_MSG);
                        error = fetchMessage(syncItem) ? ITEM_SUCCESS : ITEM_TEMPORARY_FAILURE;
                        break;
                    case FETCH_ATTACHMENT:
                        // uiNotification.syncStatus(SyncStatusListener.FETCHING_ATTACHEMENTS);
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FETCH_ATTACHEMENT);
                        error = FecthAction.getInstance().fetchAttachment(syncItem, settings.getMDN());
                        break;
                    case FETCH_CHANGES:
                        // uiNotification.syncStatus(SyncStatusListener.FETCHING_CHANGES);
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FETCH_CHANGES);
                        error = FastSyncAction.getInstance().fetchChanges(0) ? ITEM_SUCCESS
                                : ITEM_TEMPORARY_FAILURE;
                        break;
                    case FULLSYNC:
                        // Full sync
                        // uiNotification.syncStatus(SyncStatusListener.FETCHING_CONVERSATION);
                        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FETCH_CONVERSATION);
                        error = FullSyncAction.getInstance().doXConvFetch(this) ? ITEM_SUCCESS
                                : ITEM_TEMPORARY_FAILURE;
                        break;
                    default:
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.warn("processSyncItems: Unknown sync action.");
                        }
                        break;
                    }
                } catch (ProtocolException e) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.error(false, "inside processSyncItems:ProtocolException:", e);
                    }
                    error = handleProtocolException(e, syncItem);
                } catch (IOException e) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.error(false, "inside processSyncItems:IOException", e);
                    }
                    // execute method will handle the IOException
                    // error = ITEM_TEMPORARY_FAILURE;
                    throw e;
                } catch (MessagingException e) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.error(false, "inside processSyncItems:MessagingException:", e);
                    }
                    if (syncItem.retryCount >= MAX_ITEM_RETRY) {
                        error = ITEM_PERMANENT_FAILURE;
                    } else {
                        error = ITEM_TEMPORARY_FAILURE;
                    }
                } catch (RuntimeException e) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.error(false, "inside processSyncItems:RuntimeException:", e);
                    }
                    if (syncItem.retryCount >= MAX_ITEM_RETRY) {
                        error = ITEM_PERMANENT_FAILURE;
                    } else {
                        error = ITEM_TEMPORARY_FAILURE;
                    }
                } catch (Exception e) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.error(false, "inside processSyncItems:Exception:", e);
                    }
                    if (syncItem.retryCount >= MAX_ITEM_RETRY) {
                        error = ITEM_PERMANENT_FAILURE;
                    } else {
                        error = ITEM_TEMPORARY_FAILURE;
                    }
                }
                // queuing remove items
                itemsToRemove.add(syncItem);

                if (error == ITEM_SUCCESS) {
                    int count = syncDao.deleteEvent(syncItem.id);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("processed:" + count + " event deleted: " + syncItem);
                    }
                } else {
                    // Failure
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("failed to process. item=" + syncItem + ",failure code= " + error);
                    }
                    if (error == ITEM_TEMPORARY_FAILURE) {
                        syncItem.retryCount = syncItem.retryCount + 1;
                        if (syncItem.retryCount >= MAX_ITEM_RETRY) {
                            // Find the exact priority using item action
                            syncItem.lastPriority = syncItem.priority;
                            // we don't have to update the priority for full sync item.
                            if (syncItem.action != ItemAction.FULLSYNC) {
                                syncItem.priority = ItemPriority.DEFFERED;
                            }
                        }
                        markPDUItemAsFailed(syncItem);
                    } else if (error == ITEM_PERMANENT_FAILURE) {
                        markPDUItemAsFailed(syncItem);
                        syncItem.retryCount = syncItem.retryCount + 1;
                        syncItem.priority = ItemPriority.PERMANENT_FAILURE;
                    } else if (error == ACCOUNT_INSUFFICIENT_FUNDS) {
                        // mark all send items as deffered
                        items.clear();
                        break;
                    } else if (error == ACCOUNT_SUSPENDED) {
                        markPDUItemAsFailed(syncItem);
                        handleAccountError(error);
                        items.clear();
                        break;
                    } else if (error == ACCOUNT_INVALID) {
                        items.clear();
                        // this method will send the shutdown intent , so we need handle other case before
                        // calling
                        handleAccountError(error);
                        break;
                    } else if (error == INVALID_SESSION_RELOGIN) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Sessing in wrong state relogging.item=" + syncItem);
                        }
                        items.clear();
                        break;
                    } else {

                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("failure default else block.");
                        }
                    }
                    int count = syncDao.updateSyncItem(syncItem);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("updated failed item=" + syncItem + "updatecount=" + count);
                    }
                }
            }
            for (SyncItem syncItem : itemsToRemove) {
                items.remove(syncItem);
            }
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("items: processed=" + itemsToRemove.size() + ", pending=" + items.size());
            }
            itemsToRemove.clear();

        } while (!isDone);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("processSyncItems() done.");
        }

    }

    /**
     * This Method
     */
    @Deprecated
    private void markAllSendItemsAsFailed() {
        // TEMP fix, // TODO proper fix on connection changes
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("markAllSendItemsAsFailed()");
        }
        while (true) {
            List<SyncItem> items = syncDao.getSendItems(MAX_DB_QUERY_SIZE);
            if (items != null && !items.isEmpty()) {
                for (SyncItem syncItem : items) {
                    markAsDeffered(syncItem);
                }
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("No more send items found.");
                }
                break;
            }
        }
    }

    @Deprecated
    private void markSmsItemsAsBlocked() {
        // TEMP fix, // TODO proper fix on connection changes
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("markAllSendItemsAsFailed()");
        }
        while (true) {
            List<SyncItem> items = syncDao.getSendMmsItems(MAX_DB_QUERY_SIZE);
            if (items != null && !items.isEmpty()) {
                for (SyncItem syncItem : items) {
                    // Permanent failure
                    markItemAsFailed(syncItem, ItemPriority.PERMANENT_FAILURE);
                }
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("No more sms send items found.");
                }
                break;
            }

        }
    }

    @Deprecated
    private void markMmsItemsAsBlocked() {
        // TEMP fix, // TODO proper fix on connection changes
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("markMmsItemsAsBlocked()");
        }
        while (true) {
            List<SyncItem> items = syncDao.getSendSmsItems(MAX_DB_QUERY_SIZE);
            if (items != null && !items.isEmpty()) {
                for (SyncItem syncItem : items) {
                    // Permanent failure
                    markItemAsFailed(syncItem, ItemPriority.PERMANENT_FAILURE);
                }
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("No more mms send items found.");
                }
                break;
            }

        }
    }

    /**
     * This Method
     * 
     * @param syncItem
     */
    @Deprecated
    private void markAsDeffered(SyncItem syncItem) {
        // TEMP fix, // TODO proper fix on connection changes
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("markAsDeffered.item=" + syncItem);
        }
        markItemAsFailed(syncItem, ItemPriority.DEFFERED);
    }

    private void markItemAsFailed(SyncItem syncItem, ItemPriority priority) {
        markPDUItemAsFailed(syncItem);
        syncItem.lastPriority = syncItem.priority;
        syncItem.priority = priority;
        syncDao.updateSyncItem(syncItem);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("marked as failed.item=" + syncItem);
        }
    }

    /**
     * This Method
     * 
     * @param syncItem
     */
    private void markPDUItemAsFailed(SyncItem syncItem) {
        if (syncItem.isSend()) {
            int count = 0;
            if (syncItem.isSMS()) {
                count = pduDao.moveSMStoSendFailed(syncItem.itemId, SmsManager.RESULT_ERROR_NO_SERVICE);
            } else if (syncItem.isMMS()) {
                count = pduDao.moveMMStoSendFailed(syncItem.itemId,
                        PduHeaders.RESPONSE_STATUS_ERROR_UNSPECIFIED);
            }
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("markItemAsFailed() count=." + count);
            }
        }
    }

    private boolean fetchMessage(SyncItem syncItem) throws ProtocolException, IOException,
            MessagingException, MmsException {
        if (isTablet) {
            return FecthAction.getInstance().fetchMessageTablet(syncItem, settings.getMDN());
        } else {
            return FecthAction.getInstance().fetchMessageHandset(syncItem, settings.getMDN());
        }
    }

    private void execute() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Inside execute method. network=" + isConnected + ",shutdown=" + isShutdown);
        }
        try {
            // 1.) Login
            VmaSelectResponse mailBox = null;
            if (!store.isConnected()) {
                if (connectionType == VMAConnectionType.IDLE) {
                    uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_IDLE_LOGIN);
                } else if (connectionType == VMAConnectionType.FETCH) {
                    uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FETCH_LOGIN);
                } else if (connectionType == VMAConnectionType.FULLSYNC) {
                    uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FULLSYNC_LOGIN);
                } else if (connectionType == VMAConnectionType.SEND) {
                    uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_SEND_LOGIN);
                }
                mailBox = store.login();
                if (connectionType != VMAConnectionType.SEND && mailBox != null) {
                    updateAutoForwardAndReplychanges(mailBox);
                }
                // GOT IMAP Connection back
                imapConnectionIssue = false;
                // RESET the retry count.
                connectionFailureRetry = 0;
                clearAccountSuspendFlag();
            }
            // Processing the class based on connection type.
            switch (connectionType) {
            case IDLE:
                executeIdle(mailBox);
                break;
            case SEND:
                sendPriorityItems();
                break;
            case FETCH:
                fetchPriorityItems();
                break;
            case FETCH_ATTACHEMENT:
                fetchPriorityAttachements();
                break;
            case FULLSYNC:
                startFullSync();
                break;
            default:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(" Unknown action.");
                }
                break;
            }
        } catch (IOException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error(false, e);
            }
            imapConnectionIssue = true;
        } catch (ProtocolException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error(false, e);
            }
            int error = handleProtocolException(e);
            if (error == ACCOUNT_INVALID || error == ACCOUNT_SUSPENDED) {
                handleAccountError(error);
            }
        } finally {
            if (connectionType != VMAConnectionType.IDLE || idleSleep) {
                store.signout();
            } else {
                // Temp fix
                imapConnectionIssue = true;
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("execute() done. network=" + isConnected + ",shutdown=" + isShutdown);
        }

    }

    /**
     * This Method
     * 
     * @param mailBox
     */
    private void updateAutoForwardAndReplychanges(VmaSelectResponse mailBox) {
        long serverFwdcAnconr = mailBox.getAutoForwardChangeCount();
        long serverReplyAnchor = mailBox.getAutoReplyChangeCount();
        long ourFwdAnchor = settings.getLongSetting(AppSettings.KEY_AUTOFORWARD_SYNC_ANCHOR, 0);
        long ourReplyAnchor = settings.getLongSetting(AppSettings.KEY_AUTOREPLY_SYNC_ANCHOR, 0);
        if (ourFwdAnchor != serverFwdcAnconr || ourReplyAnchor != serverReplyAnchor) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Changes found on auto reply settings. Sync anchor: our=." + ourReplyAnchor
                        + ",server=" + serverReplyAnchor);
                Logger.debug("Changes found on auto forward settings. Sync anchor: our=." + ourFwdAnchor
                        + ",server=" + serverFwdcAnconr);
            }
            Intent intent = new Intent(SyncManager.ACTION_START_PROVISIONING);
            intent.putExtra(SyncManager.EXTRA_ACTION, VMAProvision.ACTION_ASSISTANT_QUERY);
            intent.putExtra(AppSettings.EXTRA_AUTO_REPLY_SYNCANCHOR, serverReplyAnchor);
            intent.putExtra(AppSettings.EXTRA_AUTO_FORWARD_SYNCANCHOR, serverFwdcAnconr);
            context.startService(intent);
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("No changes found on auto reply settings. Sync anchor: our=." + ourReplyAnchor
                        + ",server=" + serverReplyAnchor);
                Logger.debug("No changes found on auto forward settings. Sync anchor: our=." + ourFwdAnchor
                        + ",server=" + serverFwdcAnconr);
            }
        }
    }

    /**
     * This Method
     */
    private void clearAccountSuspendFlag() {
        if (settings.getBooleanSetting(AppSettings.KEY_VMA_ACCOUNT_SUSPENDED, false)) {
            // Account suspend ,Resetting the suspend mode.
            settings.put(AppSettings.KEY_VMA_ACCOUNT_SUSPENDED, false);
        }
    }

    /**
     * This Method is used to send the messages out
     * 
     * @throws ProtocolException
     * @throws IOException
     */
    private void sendPriorityItems() throws IOException, ProtocolException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("sendPriorityItems(). queuedItemToSend=" + queuedItemToSend + ",shutdown="
                    + isShutdown);
        }
        // syncDao.moveSendDefferedItemsBackToQueue();
        syncDao.updateDefferedItemsBackToQueue();
        List<SyncItem> items = null;
        queuedItemToSend = true;
        // syncController.syncstatus(SyncStatusListener.SENDING_MESSAGE);
        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_SEND_SMS);
        // syncController.sendingMessage(progress, total, type)
        boolean isDone = false;
        while (!isDone && isConnected && !isShutdown) {
            items = syncDao.getSendItems(MAX_QUEUE_SIZE);
            if (items != null && !items.isEmpty()) {
                processSyncItems(items);
            } else {
                isDone = true;
            }
        }
        queuedItemToSend = false;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("sendPriorityItems() done. queuedItemToSend=" + queuedItemToSend + ",shutdown="
                    + isShutdown);
        }

    }

    /**
     * This Method
     * 
     * @param mailBox
     * @throws MessagingException
     */
    private void startFullSync() throws ProtocolException, IOException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("startFullSync().");
        }
        List<SyncItem> items = null;
        // syncController.syncstatus(SyncStatusListener.FETCHING_CONVERSATION);
        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FETCH_CONVERSATION);
        // syncController.sendingMessage(progress, total, type)
        boolean isDone = false;
        while (!isDone && isConnected && !isShutdown) {
            items = syncDao.getFullSyncItems();
            if (items != null && !items.isEmpty()) {
                processSyncItems(items);
            } else {
                isDone = true;
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("startFullSync(). done shutdown=" + isShutdown);
        }

    }

    // private int doXConvFetch() throws IOException, ProtocolException {
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug("doXConvFetch().");
    // }
    // syncController.syncstatus(SyncStatusListener.SYNC_PREPARING);
    // FullSync fetchUids = new FullSync(settings, store, syncDao,context);
    // fetchUids.fetchUids();
    // int messageTotalCount = fetchUids.getMsgChangesCount();
    // settings.put(AppSettings.KEY_FULLSYNC_COMPLETED, true);
    // settings.put(AppSettings.KEY_FULLSYNC_MSG_COUNT, messageTotalCount);
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug("doXConvFetch(). done");
    // }
    // isShutdown = true;
    // return ITEM_SUCCESS;
    // }

    /**
     * This Method
     */
    private void fetchPriorityAttachements() throws ProtocolException, IOException {
        List<SyncItem> items = null;
        boolean isDone = false;
        // syncController.syncstatus(SyncStatusListener.FETCHING_MESSAGE);
        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FETCH_MSG);
        while (!isDone && isConnected && !isShutdown) {
            items = syncDao.getFetchAttachementsItems(MAX_QUEUE_SIZE);
            if (items != null && !items.isEmpty()) {
                processSyncItems(items);
            } else {
                isDone = true;
            }
        }
    }

    /**
     * This Method
     * 
     * @param mailBox
     * @throws ProtocolException
     * @throws IOException
     */
    private void fetchPriorityItems() throws IOException, ProtocolException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("fetchPriorityItems()");
        }
        List<SyncItem> items = null;
        // syncController.syncstatus(SyncStatusListener.FETCHING_MESSAGE);
        uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_FETCH_MSG);
        // syncDao.moveFetchDefferedItemsBackToQueue();
        syncDao.updateDefferedItemsBackToQueue();
        queueditemToFetch = true;
        boolean isDone = false;
        while (!isDone && isConnected && !isShutdown) {
            items = syncDao.getFetchItems(1);
            if (items != null && !items.isEmpty()) {
                processSyncItems(items);
            } else {
                isDone = true;
            }
        }
        queueditemToFetch = true;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("fetchPriorityItems() done. queueditemToFetch=" + queueditemToFetch + ",shutdown="
                    + isShutdown + ",isConnected=" + isConnected);
        }
    }

    /**
     * This Method
     * 
     * @param mailBox
     * @throws IOException
     * @throws ProtocolException
     */
    private void executeIdle(VmaSelectResponse mailBox) throws ProtocolException, IOException {
        boolean firstPass = true;
        long wlTime = 0;
        long iters = 0;
        do {
            iters++;
            try {
                long ourPMcr = settings.getLongSetting(AppSettings.KEY_OUR_MAX_PMCR, -1);
                long ourSMcr = settings.getLongSetting(AppSettings.KEY_OUR_MAX_SMCR, -1);
                if (ourPMcr < 0 && ourSMcr < 0) {
                    // First Launch
//                    ourMaxXmcr = mailBox.getHighestModSeq();
                	ourPMcr = mailBox.getPrimaryHighestMCR();
                    fullsyncLastUid = mailBox.getLastUid();
                    settings.put(AppSettings.KEY_OUR_MAX_PMCR,mailBox.getPrimaryHighestMCR());
        			settings.put(AppSettings.KEY_OUR_MAX_SMCR,mailBox.getSecondaryHighestMCR());
//                    settings.put(AppSettings.KEY_OUR_MAX_XMCR, ourMaxXmcr);
                    settings.put(AppSettings.KEY_FULLSYNC_LAST_UID, fullsyncLastUid);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("First launch of IDLE, waking other threads (fullsync) after setting up initialMCR="
                                + ourPMcr + " fullsyncLastUid=" + fullsyncLastUid);
                    }
                    syncDao.addFullSyncEvent();
                }
                if (!idleAbortedByTimer || firstPass) {
                    firstPass = false;
//                    FastSyncAction.getInstance().fetchChanges(ourMaxXmcr);
                      FastSyncAction.getInstance().fetchChanges();
                }
                synchronized (waitLock) {
                    if (settings.isApplicationInBackground()) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("App in background. put the idle connection on wait state.");
                        }
                        idleSleep = true;
                        break;
                    } else {
                        idleSleep = false;
                    }
                }
                timer = new Timer();
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("New timer name=" + timer.toString());
                }
                idleAbortedByTimer = false;

                long startT = System.currentTimeMillis();

                // PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VMA Idle Task"
                // + this.getThreadName());
                //
                // wl.acquire(store.getIdleTimeout()); // Wake lock will expire after timer fires

                long endT = System.currentTimeMillis();
                wlTime += (endT - startT);

                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        synchronized (waitLock) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("Timer Sending done command to prevent idle timeout.");
                            }
                            store.abortIdle();
                            idleAbortedByTimer = true;
                            timer = null;
                        }
                    }
                }, store.getIdleTimeout() - 5000);
                // syncController.syncstatus(SyncStatusListener.FOREGROUND_CONNECTION_IDLE);
                synchronized (waitLock) {
                    idling = true;
                }
                uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_IDLING);
                int idleStatus = store.startIdle();
                synchronized (waitLock) {
                    idling = false;
                }
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Woken up idlestatus = " + idleStatus);
                }
                if (idleStatus == VMAStore.IDLE_STATUS_XUPDATE) {
//                    ourMaxXmcr = settings.getLongSetting(AppSettings.KEY_OUR_MAX_XMCR);
//                    FastSyncAction.getInstance().fetchChanges(ourMaxXmcr);
                      FastSyncAction.getInstance().fetchChanges();
                } else if (idleStatus == VMAStore.IDLE_NO_VALID_SESSION) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("No session found. release the sleepoOrIdle to create new session.");
                    }
                    break;
                } else if (idleStatus == VMAStore.IDLE_STATUS_ABORT) {

                    if (!idleAbortedByTimer) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Idle abort by manual/retry " + idleAbortedByTimer);
                        }
                        timer.cancel();
                        idleAbortedByTimer = false;
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Idle abort by idleAbortedByTimer=" + idleAbortedByTimer);
                        }
                    }
                    continue;
                } else if (idleStatus == VMAStore.IDLE_STATUS_LOGIN_FAILED) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Login Failure");
                    }
                    isShutdown = true;
                    break;
                } else if (idleStatus == VMAStore.IDLE_STATUS_EXCEPTION) {
                	// here we got an exception in IDLE - it is good to set the network status icon so that the user knows
                	// there is some network problem. In run() the network error is set ONLY if there is no network connectivity
                	// not on timeouts or SSL errors etc
                    uiNotification.syncStatus(SyncStatusCode.VMA_SYNC_IDLE_NO_NETWORK);
                	
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.warn("Idle() got exception");
                    }
                    break;
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.warn("Idle():Could not found the expected use case");
                    }
                }
                // No need to catch execute method will take care,

            } finally {
                synchronized (waitLock) {
                    if (timer != null) {
                        timer.cancel();
                    }
                    idling = false;
                }
            }

        } while (isConnected && !isShutdown);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Time spent acquiring waitlock = " + wlTime + " iters=" + iters);
        }
    }

    /**
     * This Method
     */
    public synchronized void shutdown() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("shutdown(): " + ((threadName != null) ? threadName : connectionType));
        }
        synchronized (waitLock) {
            isShutdown = true;
            if (connectionType == VMAConnectionType.IDLE) {
                if (idling) {
                    abortIdle();
                }
            }
            waitLock.notifyAll();
        }
    }

    private int handleProtocolException(ProtocolException e) {
        return handleProtocolException(e, null);
    }

    /**
     * This Method is used to handle the protocol exception. 0 Temporary failure 1 Permanent failure 2
     * Shutdown
     * 
     * @param e
     */
    private int handleProtocolException(ProtocolException e, SyncItem syncItem) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("handleProtocolException() :"
                    + ((e.getResponse() != null) ? e.getResponse().getRest() : "null"));
        }
        if (e.getResponse() == null) {
            return ITEM_TEMPORARY_FAILURE;
        }
        if (e.getResponse().isOK()) {
            return handleOKResponse(e.getResponse());
        } else if (e.getResponse().isNO()) {
            return handleNoResponse(e.getResponse(), syncItem);
        } else if (e.getResponse().isBAD()) {
            return handleBADResponse(e.getResponse());
        } else if (e.getResponse().isBYE()) {
            return handleByeResponse(e.getResponse());
        } else {
            return ITEM_TEMPORARY_FAILURE;
        }
    }

    private int handleNoResponse(Response response, SyncItem syncItem) {
        if (response == null) {
            return ITEM_TEMPORARY_FAILURE;
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("handleNoResponse():" + response.toString());
        }
        int failure = ITEM_TEMPORARY_FAILURE;
        // A0 NO LOGIN failed.
        if (VMAStore.LOGIN_FAILED.equalsIgnoreCase(response.getRest().trim())) {
            // isLoginFailed = true;
            // isShutdown = true;
            failure = ACCOUNT_INVALID;
            uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_LOGIN_FAILED);
        } else {
            int responseCode = store.getStatusCode(response.getRest());
            switch (responseCode) {
            case VMAStore.MSA_OTHER_PERMANENT_FAILURE:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse(): [499] Other permanent failure, IDLE command not active");
                }
                // isLoginFailed = true;
                // isShutdown = true;
                uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_OTHER_PERMANENT_FAILURE);
                failure = ACCOUNT_INVALID;
                break;
            case VMAStore.MSA_MAILBOX_DOES_NOT_EXIST:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse():NO SELECT [408] Mailbox does not exist.");
                }
                // isLoginFailed = true;
                // isShutdown = true;
                uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_MAILBOX_DOES_NOT_EXIST);
                failure = ACCOUNT_INVALID;
                break;

            case VMAStore.MSA_NOT_A_VMA_SUBSCRIBER:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse(): NO [450] Subscriber not found with that MDN");
                }
                // isShutdown = true;
                uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_NOT_A_VMA_SUBSCRIBER);
                failure = ACCOUNT_INVALID;
                break;
            case VMAStore.MSA_MISSING_LOGIN_OR_PASSWORD:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse():LOGIN [400] Missing login/password.");
                }
                // isShutdown = true;
                uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_MISSING_LOGIN_OR_PASSWORD);
                failure = ACCOUNT_INVALID;
                break;

            // Account suspend
            case VMAStore.MSA_ACCOUNT_SUSPENDED:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse():NO [452] Subscriber is suspended.");
                }
                // isShutdown = true;
                uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_ACCOUNT_SUSPENDED);
                failure = ACCOUNT_SUSPENDED;
                break;
            case VMAStore.MSA_HAS_SMS_BLOCKING:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse():NO [451] Subscriber has SMS blocking");
                }
                // isShutdown = true;
                // TEMP fix, // TODO proper fix on connection changes
                markSmsItemsAsBlocked();
                uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_HAS_SMS_BLOCKING);
                failure = ACCOUNT_SUSPENDED;
                break;
            case VMAStore.MSA_SUBSCRIBER_HAS_MMS_BLOCKING:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse():NO [453] Subscriber has MMS blocking");
                }
                // isShutdown = true;
                // TEMP fix, // TODO proper fix on connection changes
                failure = ACCOUNT_SUSPENDED;
                markMmsItemsAsBlocked();
                uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_SUBSCRIBER_HAS_MMS_BLOCKING);
                break;
            case VMAStore.MSA_SUBSCRIBER_HAS_INSUFFICIENT_FUNDS:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse():NO [458] Subscriber has insufficient funds");
                }
                // TEMP fix, // TODO proper fix on connection changes
                // isShutdown = true;
                markAllSendItemsAsFailed();
                failure = ACCOUNT_INSUFFICIENT_FUNDS;
                uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_SUBSCRIBER_HAS_INSUFFICIENT_FUNDS);
                break;
            case VMAStore.MSA_SESSION_WRONG_STATE:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse():LOGIN [402] Session in wrong state.");
                }
                // ReLogin ,
                store.signout();
                failure = INVALID_SESSION_RELOGIN;
                break;

            case VMAStore.MSA_FAILED_ANTISPAM_CHECK:
                // NO [454] Failed anti-spam check
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse():NO [454] Failed anti-spam check");
                }
                failure = ITEM_PERMANENT_FAILURE;
                uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_FAILED_ANTISPAM_CHECK);
                break;
            case VMAStore.MSA_ANOTHER_COMMAND_ALREADY_IN_PROGRESS:
                // NO [403] Another command already in progress
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("handleNoResponse():LOGIN [403] Another command already in progress"
                            + store.isIdling());
                }
                store.signout();
                failure = INVALID_SESSION_RELOGIN;
                break;
            case VMAStore.MSA_ISTD_NOT_SUPPORTED:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("International destination numbers are not supported." + response.getRest());
                }
                failure = ITEM_PERMANENT_FAILURE;
                // syncController.syncstatus(SyncStatusListener.MSA_RESPONSE_ISTD_NOT_SUPPORTED);
                uiNotification.syncStatus(AppErrorCodes.VMA_SYNC_ISTD_NOT_SUPPORTED);
                break;

            default:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Unknown error " + response.getRest());
                }
                break;
            }
        }

        return failure;
    }

    /**
     * This Method
     * 
     * @param response
     */
    private int handleOKResponse(Response response) {
        return ITEM_TEMPORARY_FAILURE;
    }

    /**
     * This Method is used to handle the bye response from server
     * 
     * @param e
     */
    private int handleByeResponse(Response e) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("handleByeResponse() :" + ((e != null) ? e.getRest() : "Null"));
        }
        return ITEM_TEMPORARY_FAILURE;
    }

    /**
     * This Method is used to handle the BAD response from server
     * 
     * @param response
     */
    private int handleBADResponse(Response e) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("handleBADResponse() :" + ((e != null) ? e.getRest() : "Null"));
        }
        return ITEM_PERMANENT_FAILURE;
    }

    private void handleAccountError(int errorCode) {
        if (errorCode == ACCOUNT_INVALID) {
            handleLoginFailure();
            context.startService(new Intent(SyncManager.ACTION_STOP_VMA_SYNC));
        } else if (errorCode == ACCOUNT_SUSPENDED) {
            isShutdown = true;
            if (connectionType != VMAConnectionType.IDLE) {
                // Stop the idle connection from worker
                // Bug : incase if we get any account error from worker thread.we are not stopping the it is
                // taking time to shutdown. so we are shutdown before calling the stop service.
                syncController.stopIdleConnection();
            }
            settings.put(AppSettings.KEY_VMA_ACCOUNT_SUSPENDED, true);
            context.startService(new Intent(SyncManager.ACTION_STOP_VMA_SYNC));
        }
    }

    private void handleLoginFailure() {
        isShutdown = true;
        if (connectionType != VMAConnectionType.IDLE) {
            // Stop the idle connection from worker
            // Bug : incase if we get any account error from worker thread.we are not stopping the it is
            // taking time to shutdown. so we are shutdown before calling the stop service.
            syncController.stopIdleConnection();
        }
        // Shutdown the worker threads by deleting pending events
        int count = syncDao.deleteAllPendingEvents();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Account invalid. permanent failure deleting all pending events. count" + count);
        }
        store.clearCache();

        if (isTablet) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Account invalid. Unregister the GCM registration token");
            }
            try {
                // GCMRegistrar.unregister(context);
                GCMRegistrar.onDestroy(context);
            } catch (Exception e) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error("Account invalid. Unregister the GCM failed ");
                }
            }
        }
        context.startService(new Intent(SyncManager.ACTION_STOP_VMA_SYNC));
    }

    /**
     * Returns the Value of the idling
     * 
     * @return the {@link boolean}
     */
    public synchronized boolean isIdling() {
        return idling;
    }

    /**
     * This Method is used to abort the idle connection to start the syncing.
     */
    public final void abortIdle() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("abortIdle() invoked:");
        }
        if (isIdling()) {
            store.abortIdle();
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.SyncClient#isMine(com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority)
     */
    @Override
    public boolean isMine(ItemPriority priority) {
        if (connectionType == VMAConnectionType.IDLE) {
            return false;
        } else if (connectionType == VMAConnectionType.FETCH) {
            return ItemPriority.isFetchPriority(priority);
        } else if (connectionType == VMAConnectionType.FETCH_ATTACHEMENT) {
            return ItemPriority.isFetchPriority(priority);
        } else if (connectionType == VMAConnectionType.FULLSYNC) {
            return ItemPriority.isInitialSyncPriority(priority);
        } else if (connectionType == VMAConnectionType.SEND) {
            return ItemPriority.isSendPriority(priority);
        }
        return false;
    }

    @Override
    public boolean isMine(Set<ItemPriority> priorities) {
        for (ItemPriority priority : priorities) {
            if (isMine(priority)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.SyncClient#getConnectionType()
     */
    @Override
    public VMAConnectionType getConnectionType() {
        return connectionType;
    }

}
