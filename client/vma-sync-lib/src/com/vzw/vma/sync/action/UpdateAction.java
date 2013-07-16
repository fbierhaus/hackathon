/**
 * UpdateAction.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.strumsoft.android.commons.logger.Logger;
import com.sun.mail.iap.ProtocolException;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.vzw.vma.message.VMAMarkMessageResponse;
import com.vzw.vma.message.VMAStore;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Feb 22, 2013
 */
public class UpdateAction {

    private static UpdateAction instance;
    private SyncItemDao syncItemDao;
    private VMAStore store;
    
    
    protected UpdatedItemsCache cache;

    /**
     * 
     * Constructor
     */
    private UpdateAction(SyncItemDao syncItemDao, VMAStore store) {
        this.syncItemDao = syncItemDao;
        this.store = store;
        cache = UpdatedItemsCache.getInstance();
    }

    /**
     * This Method is used to send read message
     * 
     * @param item
     * @return {@link Boolean}
     * @throws IOException
     * @throws ProtocolException
     * 
     */
    public boolean readMessages(SyncItem item) throws ProtocolException, IOException {
        boolean done = false;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Sending read mesages: item=" + item);
        }

        List<SyncItem> uidsyncItems = syncItemDao.getPendingReadMessages(50);
        if (uidsyncItems != null && !uidsyncItems.isEmpty()) {
            List<Long> uids = new ArrayList<Long>();
            List<Long> itemsToDelete = new ArrayList<Long>();
            for (SyncItem sync : uidsyncItems) {
                if (sync.itemId > 0) {
                    uids.add(sync.itemId);
                }
                itemsToDelete.add(sync.id);
            }
            List<VMAMarkMessageResponse> response = store.markMessageRead(uids);
            if (response != null) {
                done = true;
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Sent read flags: uids=" + uids);
                }
                syncItemDao.deleteEventsWithIds(itemsToDelete);
                for(VMAMarkMessageResponse res : response) {
                	if(!res.isDeleted()) {
                		// If the item happened to get deleted by someone else, we should let the 
                		// update come in through the other channel
                		if(Logger.IS_DEBUG_ENABLED) {
                			Logger.debug("UpdateAction: Adding to cache uid=" + res.getUID() + " mcr=" + res.getModSeq());
                		}
                		cache.addMCR(res.getUID(), res.getModSeq());
                	}
                }
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Unable to send read flags: uids=" + uids);
                }
            }
        }
        return done;
    }

    /**
     * This Method is used to send read message
     * 
     * @param item
     * @return {@link Boolean}
     * @throws IOException
     * @throws ProtocolException
     * 
     */
    public boolean deletedMessage(SyncItem item) throws ProtocolException, IOException {
        boolean done = false;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Sending deleted mesages: item=" + item);
        }
        List<SyncItem> uidsyncItems = syncItemDao.getPendingDeletedMessages(50);
        if (uidsyncItems != null && !uidsyncItems.isEmpty()) {
            List<Long> uids = new ArrayList<Long>();
            List<Long> itemsToDelete = new ArrayList<Long>();
            for (SyncItem sync : uidsyncItems) {
                if (sync.itemId > 0) {
                    uids.add(sync.itemId);
                }
                itemsToDelete.add(sync.id);
            }
            List<VMAMarkMessageResponse> response = store.markMessageDeleted(uids);
            if (response != null) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Sent deleted flags: uids=" + uids);
                }
                syncItemDao.deleteEventsWithIds(itemsToDelete);
                for(VMAMarkMessageResponse res : response) {
            		if(Logger.IS_DEBUG_ENABLED) {
            			Logger.debug("UpdateAction: Adding to cache uid=" + res.getUID() + " mcr=" + res.getModSeq());
            		}
                	cache.addMCR(res.getUID(), res.getModSeq());
                }
                done = true;
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Unable to send deleted flags: uids=" + uids);
                }
            }
        }
        return done;
    }

    /**
     * This Method
     * 
     * @return
     */
    public synchronized static UpdateAction getInstance(SyncItemDao syncItemDao, VMAStore store) {
        if (instance == null) {
            instance = new UpdateAction(syncItemDao, store);
        }else{
            instance.updateOldReference(syncItemDao, store);
        }
        return instance;
    }
    
    /**
     * This Method 
     * @param syncItemDao
     * @param store
     */
    private void updateOldReference(SyncItemDao syncItemDao, VMAStore store) {
        this.syncItemDao = syncItemDao;
        this.store = store;
        cache = UpdatedItemsCache.getInstance();
        
    }

    public synchronized static UpdateAction getInstance() {
        return instance;
    }

}
