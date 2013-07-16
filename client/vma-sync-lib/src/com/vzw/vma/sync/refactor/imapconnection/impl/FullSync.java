package com.vzw.vma.sync.refactor.imapconnection.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.mail.MessagingException;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.strumsoft.android.commons.logger.Logger;
import com.sun.mail.iap.ProtocolException;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemAction;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemType;
import com.verizon.messaging.vzmsgs.provider.Vma.SyncStatusTable;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.vzw.vma.message.VMAStore;
import com.vzw.vma.message.VMAXconvFetchResponse;
import com.vzw.vma.message.VMAXconvListResponse;
import com.vzw.vma.sync.SyncClient;
import com.vzw.vma.sync.SyncStatus;

public class FullSync {

    private VMAStore vmastore;
    private SyncItemDao syncItemDao;;
    private AppSettings settings;
    private SyncClient syncClient;
    public SyncStatus syncstat;

    private int msgReceivedCount;
    private int msgChangesCount;
    private int xConvReceivedCount;
    private int xConvCount;

    public static final int MAX_CONV_PER_QUERY = 50;
    public static final int MAX_CONV_PER_HIGH_PRI = 10;

    private static HashSet<String> convIdCache = new HashSet<String>();

    protected long fullsyncLastUid = -1;
    ContentResolver resolver;

    /**
     * This Method
     * 
     * @return
     */
    private synchronized SyncStatus getSyncStatus() {
        SyncStatus status = null;
        Cursor c = resolver.query(SyncStatusTable.CONTENT_URI, null, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                status = new SyncStatus();
                status.minUid = c.getLong(c.getColumnIndex(SyncStatusTable.MIN_UID));
                status.minModSeq = c.getLong(c.getColumnIndex(SyncStatusTable.MIN_MOD_SEQUENCE));
                status.maxUid = c.getLong(c.getColumnIndex(SyncStatusTable.MAX_UID));
                status.maxModSeq = c.getLong(c.getColumnIndex(SyncStatusTable.MIN_MOD_SEQUENCE));
                status.syncCompleted = (c.getInt(c.getColumnIndex(SyncStatusTable.SYNC_MODE)) <= 0) ? false
                        : true;
                status.partialSyncModSeq = c.getLong(c.getColumnIndex(SyncStatusTable.PROCESSED_MAX_MODSEQ));
                break;
            }
            c.close();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(FullSync.class, "getSyncStatus=" + status);
        }
        return status;
    }

    public synchronized int updateSyncStatus(SyncStatus syncstat) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(FullSync.class, "updateSyncStatus=" + syncstat);
        }
        ContentValues values = new ContentValues();
        values.put(SyncStatusTable.MAX_UID, syncstat.maxUid);
        values.put(SyncStatusTable.MIN_UID, syncstat.minUid);
        values.put(SyncStatusTable.MAX_MOD_SEQUENCE, syncstat.maxModSeq);
        values.put(SyncStatusTable.MIN_MOD_SEQUENCE, syncstat.minModSeq);
        values.put(SyncStatusTable.SYNC_MODE, (syncstat.syncCompleted) ? 1 : 0);
        values.put(SyncStatusTable.PROCESSED_MAX_MODSEQ, syncstat.partialSyncModSeq);
        int count = resolver.update(SyncStatusTable.CONTENT_URI, values, null, null);
        if (count == 0) {
            // Fresh Insert
            Uri uri = resolver.insert(SyncStatusTable.CONTENT_URI, values);
            long ll = ContentUris.parseId(uri);
            return 1;
        } else {
            return count;
        }

    }

    private boolean isPriorMessage(long uid) {
        if (fullsyncLastUid == -1) {
            this.fullsyncLastUid = ApplicationSettings.getInstance().getLongSetting(
                    AppSettings.KEY_FULLSYNC_LAST_UID, -1);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("VMAEventHandler.isPriorMessage():msgUid=" + uid + ", fullsyncLastUid="
                    + fullsyncLastUid);
        }
        return uid <= fullsyncLastUid;
    }

    /**
     * 
     * Constructor
     */
    public FullSync(AppSettings settings, VMAStore store, SyncItemDao syncItemDao, Context context,SyncClient syncClient) {
        this.settings = settings;
        this.syncItemDao = syncItemDao;
        this.vmastore = store;
        this.syncClient=syncClient;
        resolver = context.getContentResolver();
    }

    /**
     * 
     * Called by the Sync Manager to initiate a full sync.
     * 
     * @throws ProtocolException
     * @throws MessagingException
     * 
     */
    public void fetchUids() throws IOException, ProtocolException {
        try {
            boolean processedConversations = true;
            // Reset the count for notification
            msgReceivedCount = 0;
            msgChangesCount = 0;
            xConvReceivedCount = 0;
            xConvCount = 0;

            syncstat = getSyncStatus();

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Attempting Fullsync. syncstat=" + syncstat);
            }
            if (syncstat != null && syncstat.syncCompleted) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(this.getClass(), "FullSync is already completed.");
                }
                return;
            }

            /*
             * If we died mid way during the last sync re-start from there else re-start from the top again.
             * IF sync was completed sync complete flag would be true
             */
            if (syncstat == null) {
                // initial sync
                syncstat = new SyncStatus();
            }

            long minUid = syncstat.minUid;
            if (minUid == 0) {
                minUid = VMAStore.ANY;
            }

            boolean firstPass = true;
            while (processedConversations) {
                processedConversations = syncConversationsFrom(minUid);

                if(syncClient.isShutdown()){
                    if(Logger.IS_DEBUG_ENABLED){
                        Logger.debug("return from syncConversationsFrom:shutdown=true.");
                    }
                    return;
                }
                /*
                 * Keep doing the loop until we do not get any more to sync
                 */
                if (firstPass && minUid != VMAStore.ANY) {
                    /*
                     * If we did a restart and did not process any more, still make another pass starting
                     * again from the top with MAX UID
                     */
                    processedConversations = true;
                    firstPass = false;
                }
                minUid = VMAStore.ANY;

            }
            syncstat.syncCompleted = true;
            syncstat.partialSyncModSeq = syncstat.maxModSeq;
            // if (msgReceivedCount > 0) {
            // dao.fixTimeStampIssue(dao.getLastThreadId());
            // dao.setLastThreadId(0);
            // }
            updateSyncStatus(syncstat);
            logSyncStatus(null);
            // Bydefault this flag is true , now we are updating to false.
            settings.put(AppSettings.KEY_FULLSYNC_DUMP_UIDS, false);
        } finally {

        }
    }

    /**
     * This Method
     * 
     * @param e
     */
    public void logSyncStatus(Exception e) {
        if (Logger.IS_INFO_ENABLED) {
            Logger.debug("<===================Sync Status==============================>");
            if (e != null) {
                Logger.warn("Sync failed : error:" + e.getMessage());
            } else {
                Logger.debug("Sync completed successfully");
            }
            Logger.debug(" FullSync: Conversation: " + xConvReceivedCount + " of " + xConvReceivedCount);
            Logger.debug(" FullSync: Message " + msgReceivedCount + " of " + msgChangesCount);
            Logger.debug(" FullSync: Next modSequence= " + syncstat);
            Logger.debug("<===================Sync Status==============================>");

        }
    }

    /**
     * 
     * This method will recursively walk down the VMA XCONV LIST starting from uid, finding new threads to
     * sync till the time it finds threads that are fully synced.
     * 
     * @param uid
     *            Starting uid of the conversation list whose maxuid is < uid
     * 
     * @return true if it processed any conversation list
     * @throws ProtocolException
     * @throws MessagingException
     * 
     */
    public boolean syncConversationsFrom(long uid) throws IOException, ProtocolException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this.getClass(),
                    "syncConversationsFrom: Attempting to get conversation threads below : " + uid);
        }
        if(syncClient.isShutdown()){
            if(Logger.IS_DEBUG_ENABLED){
                Logger.debug("return from syncConversationsFrom:shutdown=true.");
            }
            return true;
        }
        List<VMAXconvListResponse> convs = vmastore.getConversationLists(uid, MAX_CONV_PER_QUERY);
        boolean didWork = false;
        long prevLow = syncstat.minUid;
        long prevHigh = syncstat.maxUid;
        if (convs != null && !convs.isEmpty()) {
            xConvCount = convs.size();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this.getClass(), "syncConversationsFrom: Will process num convs = " + xConvCount);
            }
            long lastMaxUid = Long.MAX_VALUE;
            // for (VMAXconvListResponse conv : convs) {
            // if (Logger.IS_DEBUG_ENABLED) {
            // Logger.debug(this.getClass(), "syncConversationsFrom: Got conversations from server="
            // + conv.getConversationThreadId() + " with maxuid=" + conv.getHighestUid());
            // }
            // }
            for (VMAXconvListResponse conv : convs) {
                if(syncClient.isShutdown()){
                    break;
                }
                long maxUid = conv.getHighestUid();
                lastMaxUid = maxUid;
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(
                            this.getClass(),
                            "syncConversationsFrom: Processing conversation "
                                    + conv.getConversationThreadId() + " with maxuid=" + conv.getHighestUid());
                }
                /*
                 * 
                 * If this conversation is already processed then skip it
                 */
                if (conv.getHighestUid() < prevLow || conv.getHighestUid() > prevHigh) {

                    didWork = true;
                    String conversationId = conv.getConversationThreadId();
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(
                                this.getClass(),
                                "syncConversationsFrom: Starting sync for conv="
                                        + conv.getConversationThreadId() + " starting uid="
                                        + conv.getHighestUid() + " have already processed through "
                                        + syncstat.minUid);
                    }
                    syncConversation(conversationId, maxUid);
                    syncstat.maxUid = (maxUid > syncstat.maxUid || syncstat.maxUid == 0) ? maxUid
                            : syncstat.maxUid;
                    syncstat.minUid = (maxUid < syncstat.minUid || syncstat.minUid == 0) ? maxUid
                            : syncstat.minUid;
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Updating syncstats with: " + syncstat);
                    }
                    /*
                     * Update sync status values only when a conversation is fully processed.
                     */
                    updateSyncStatus(syncstat);

                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(this.getClass(),
                                "syncConversationsFrom: skipping already processed conversation ");
                    }
                }

            }
            long uidFrom = lastMaxUid - 1;
            if (uidFrom < 0) {
                uidFrom = 0;
            }
            if(!syncClient.isShutdown()){
                boolean worked = syncConversationsFrom(uidFrom);
            }
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this.getClass(), "syncConversationsFrom: No work, either null or zero. convs= "
                        + convs);
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this.getClass(), "syncConversationsFrom: " + uid + " returning: " + didWork);
        }
        return didWork;
    }

    /**
     * 
     * Recursively fetch all messages in a conversation and store them in our local database
     * 
     * @param conversationId
     *            Conversation Id for which the messages need to be fetched
     * @param maxUid
     *            UID from where to start to fetch older messages.
     * @throws ProtocolException
     * @throws MessagingException
     * 
     */
    public void syncConversation(String conversationId, long maxUid) throws IOException, ProtocolException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this.getClass(), "syncConversation: Attempting to sync conv id: " + conversationId
                    + " starting from uid : " + maxUid);
        }
        List<VMAXconvFetchResponse> messages = vmastore.getConversation(conversationId, maxUid,
                MAX_CONV_PER_QUERY);

        if (messages != null && !messages.isEmpty()) {
            xConvReceivedCount++;
            // manager.sendConversationStatus(xConvReceivedCount, xConvCount);
            long leastUid = Long.MAX_VALUE;
            int index = 0;
            int messageCount = messages.size();
            msgChangesCount += messageCount;
            boolean wakeUpBackground = false;
            ArrayList<SyncItem> items = new ArrayList<SyncItem>();
            for (VMAXconvFetchResponse message : messages) {
                if(syncClient.isShutdown()){
                    if(Logger.IS_DEBUG_ENABLED){
                        Logger.debug("return from syncConversation() : shutdown=true");
                    }
                    return;
                }
                long uid = message.getUid();
                ++index;
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(this.getClass(), "syncing message: " + (index) + "/" + messageCount
                            + " ,uid=" + uid);
                }
                SyncItem item = new SyncItem();
                item.type = ItemType.VMA_MSG;
                item.action = ItemAction.FETCH_MESSAGE;
                item.itemId = uid;
                // Fetching only first 25 messages in a conversation other message will be fetched later.
                if (index < MAX_CONV_PER_HIGH_PRI && !convIdCache.contains(conversationId)) {
                    // syncController.getSyncItemDao().addFetchMessageEvent(uid,
                    // ItemPriority.FULLSYNC_CRITICAL);
                    item.priority = ItemPriority.FULLSYNC_CRITICAL;
                } else {
                    convIdCache.add(conversationId);
                    // syncController.getSyncItemDao().addFetchMessageEvent(uid,ItemPriority.FULLSYNCOLDER_MESSAGES);
                    item.priority = ItemPriority.FULLSYNCOLDER_MESSAGES;
                }
                if (isPriorMessage(uid)) {
                    items.add(item);
                    wakeUpBackground = true;
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Skipping fetching for UID that is post initial sync, since this should be fetched via a partial sync. Uid="
                                + uid);
                    }
                }
                leastUid = uid;
            }
            int count = syncItemDao.addEventInBatch(items);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Enqueued XCONV messages ids:msgcount=" + count);
            }
            // if (wakeUpBackground) {
            // syncController.newChangesFound();
            // }
            long uidFrom = leastUid - 1;
            if (uidFrom < 0) {
                uidFrom = 0;
            }
            syncConversation(conversationId, uidFrom);
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this.getClass(), "syncConversation: No work for : " + conversationId
                        + " either null or empty : " + messages);
            }
        }
    }

    /**
     * Returns the Value of the msgReceivedCount
     * 
     * @return the {@link int}
     */
    public int getMsgReceivedCount() {
        return msgReceivedCount;
    }

    /**
     * Returns the Value of the msgChangesCount
     * 
     * @return the {@link int}
     */
    public int getMsgChangesCount() {
        return msgChangesCount;
    }

    /**
     * Returns the Value of the xConvReceivedCount
     * 
     * @return the {@link int}
     */
    public int getxConvReceivedCount() {
        return xConvReceivedCount;
    }

    /**
     * Returns the Value of the xConvCount
     * 
     * @return the {@link int}
     */
    public int getxConvCount() {
        return xConvCount;
    }

}
