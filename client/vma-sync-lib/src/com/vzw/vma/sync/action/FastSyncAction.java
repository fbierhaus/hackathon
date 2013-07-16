/**
 * FastSyncAction.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.strumsoft.android.commons.logger.Logger;
import com.sun.mail.iap.ProtocolException;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemAction;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemType;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.vzw.vma.message.VMAChangedSinceResponse;
import com.vzw.vma.message.VMAStore;

/**
 * This class is used to fetch the partial sync changes
 * 
 * @author Jegadeesan M
 * @Since Feb 22, 2013
 */
public class FastSyncAction {

    private static FastSyncAction instance;
    private AppSettings settings;
    private SyncItemDao syncItemDao;
    private VMAEventHandler handler;
    private VMAStore store;
    protected UpdatedItemsCache cache;

    protected Comparator<VMAChangedSinceResponse> comparator = new Comparator<VMAChangedSinceResponse>() {

        @Override
        public int compare(VMAChangedSinceResponse lhs, VMAChangedSinceResponse rhs) {
            // if(lhs.getModSeq() == rhs.getModSeq()) {
            // return 0;
            // }
            // // Sort in descending order
            // return lhs.getModSeq() < rhs.getModSeq() ? 1 : -1;
            //
            if (lhs.getPrimaryMCR() == rhs.getPrimaryMCR()) {
                return 0;
            }
            // Sort in descending order
            return lhs.getPrimaryMCR() < rhs.getPrimaryMCR() ? 1 : -1;
        }
    };

    public synchronized static FastSyncAction getInstance() {
        return instance;
    }

    public synchronized static FastSyncAction getInstance(AppSettings settings, VMAStore store,
            SyncItemDao syncItemDao, VMAEventHandler handler) {
        if (instance == null) {
            instance = new FastSyncAction(settings, store, syncItemDao, handler);
        } else {
            instance.updateOldReference(settings, store, syncItemDao, handler);
        }
        return instance;
    }

    /**
     * This Method
     * 
     * @param settings
     * @param store
     * @param syncItemDao
     * @param handler
     */
    private void updateOldReference(AppSettings settings, VMAStore store, SyncItemDao syncItemDao,
            VMAEventHandler handler) {
        this.settings = settings;
        this.syncItemDao = syncItemDao;
        this.handler = handler;
        this.store = store;
        this.cache = UpdatedItemsCache.getInstance();

    }

    /**
     * 
     * Constructor
     */
    private FastSyncAction(AppSettings settings, VMAStore store, SyncItemDao syncItemDao,
            VMAEventHandler handler) {
        this.settings = settings;
        this.syncItemDao = syncItemDao;
        this.handler = handler;
        this.store = store;
        this.cache = UpdatedItemsCache.getInstance();
    }

    public boolean fetchChanges(long oldMaxMCR) throws ProtocolException, IOException {
        boolean processedChanges = false;

        while (true) {
            long newMaxMCR = fetchChangesOnce(oldMaxMCR);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("After fetchChangesOnce newMaxMCR=" + newMaxMCR + " oldMaxMCR=" + oldMaxMCR);
            }
            if (newMaxMCR == oldMaxMCR) {
                break;
            }
            if (Logger.IS_DEBUG_ENABLED) {
                // since the server lets us know on first change, even though there may be more
                Logger.debug("We had changes checking if there are more.");
            }
            processedChanges = true;
            oldMaxMCR = newMaxMCR;
        }

        return processedChanges;

    }

    public void fetchChanges() throws ProtocolException, IOException {
        while (true) {
            // We should not pause -1 , because server will send bad response. 
            long ourMaxPMCR = settings.getLongSetting(AppSettings.KEY_OUR_MAX_PMCR, 0);
            long ourMaxSMCR = settings.getLongSetting(AppSettings.KEY_OUR_MAX_SMCR, 0);
            long ourMaxMcr = (ourMaxPMCR >= ourMaxSMCR) ? ourMaxPMCR : ourMaxSMCR;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("fetchChanges(): Our MAX pMCR=" + ourMaxPMCR + ",sMCR=" + ourMaxSMCR
                        + " ourMaxMcr=" + ourMaxMcr);
            }

            //
            List<VMAChangedSinceResponse> changesList = store.getChangedSince(ourMaxPMCR, ourMaxSMCR);

            if (changesList != null && !changesList.isEmpty()) {

                // Sorting the items
                ArrayList<SyncItem> items = new ArrayList<SyncItem>();
                Collections.sort(changesList, comparator);

                int changesCount = changesList.size();
                long pMCr = 0;
                long sMCr = 0;
                long tempMcr = 0;

                for (int i = (changesCount - 1); i >= 0; i--) {
                    VMAChangedSinceResponse changedItem = changesList.get(i);

                    tempMcr = changedItem.getPrimaryMCR();
                    pMCr = (tempMcr > ourMaxPMCR) ? tempMcr : ourMaxPMCR;

                    tempMcr = changedItem.getSecondaryMCR();
                    sMCr = (tempMcr > ourMaxSMCR) ? tempMcr : ourMaxSMCR;
                    
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("MCR: ourMaxPMCR="+ourMaxPMCR+" ourMaxSMCR="+ourMaxSMCR +" Item="+changedItem );
                    }
                    
                    long uid = changedItem.getUID();
                    long hasPrevMCR = cache.getMCR(uid);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("fetch of UID=" + uid + " cached mcr=" + hasPrevMCR);
                    }

                    if (hasPrevMCR > 0 && hasPrevMCR == pMCr) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Skipping fetch of UID=" + uid
                                    + " since its last MCR was updated by us, MCR=" + pMCr);
                        }
                    } else {
                        SyncItem item = new SyncItem();
                        item.type = ItemType.VMA_MSG;
                        item.itemId = uid;
                        item.priority = ItemPriority.ONDEMAND_CRITICAL;
                        if (handler.hasExistingMapping(changedItem.getUID()) != null) {
                            item.action = ItemAction.FETCH_MESSAGE_HEADERS;
                        } else {
                            item.action = ItemAction.FETCH_MESSAGE;
                        }
                        items.add(item);
                    }
                }
                int result = syncItemDao.addEventInBatch(items);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("fetchChanges():enqueued id=" + result);
                }
                // Updating the settings after adding events bz if any SQL
                // failure we may lost the changes.
                settings.put(AppSettings.KEY_OUR_MAX_PMCR, pMCr);
                settings.put(AppSettings.KEY_OUR_MAX_SMCR, sMCr);
                
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("No more changes found.setting connection to idle");
                }
                break;
            }

        }
    }

    /**
     * This Method
     * 
     * @param syncItem
     * @return
     * @throws IOException
     * @throws ProtocolException
     */
    private long fetchChangesOnce(long ourMaxXmcr) throws ProtocolException, IOException {
        long highestMCR = ourMaxXmcr;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("fetchChanges(): ourMaxXmcr=" + ourMaxXmcr);
        }

        @SuppressWarnings("deprecation")
        List<VMAChangedSinceResponse> changesList = store.getChangedSince(ourMaxXmcr);
        if (changesList != null && !changesList.isEmpty()) {

            // If changes > 25 , we have to insert in batch mode
            ArrayList<SyncItem> items = new ArrayList<SyncItem>();
            Collections.sort(changesList, comparator);

            int changesCount = changesList.size();

            for (int i = (changesCount - 1); i >= 0; i--) {
                VMAChangedSinceResponse changedItem = changesList.get(i);
                long itemMCR = changedItem.getPrimaryMCR();
                // long itemMCR = changedItem.getModSeq();
                if ((itemMCR == 0) || (itemMCR <= highestMCR)) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.postErrorToAcraIfDebug("Ignoring the XMCR clientHighest XMCR=" + highestMCR
                                + ",itemMCR=" + itemMCR);
                    }
                    continue;
                } else {
                    highestMCR = itemMCR;
                }

                settings.put(AppSettings.KEY_OUR_MAX_XMCR, highestMCR);

                long uid = changedItem.getUID();
                long hasPrevMCR = cache.getMCR(uid);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("fetch of UID=" + uid + " cached mcr=" + hasPrevMCR);
                }

                if (hasPrevMCR > 0 && hasPrevMCR == highestMCR) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Skipping fetch of UID=" + uid
                                + " since its last MCR was updated by us, MCR=" + highestMCR);
                    }
                } else {
                    SyncItem item = new SyncItem();
                    item.type = ItemType.VMA_MSG;
                    item.itemId = uid;
                    item.priority = ItemPriority.ONDEMAND_CRITICAL;
                    if (handler.hasExistingMapping(changedItem.getUID()) != null) {
                        item.action = ItemAction.FETCH_MESSAGE_HEADERS;
                    } else {
                        item.action = ItemAction.FETCH_MESSAGE;
                    }
                    items.add(item);
                }

            }
            int result = syncItemDao.addEventInBatch(items);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("fetchChanges():enqueued id=" + result);
            }
        }

        return highestMCR;
    }

}
