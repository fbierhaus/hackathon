package com.verizon.messaging.vzmsgs.provider.dao;

import java.util.List;
import java.util.Set;

import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority;

public interface SyncItemDao {
    
    public interface SyncItemListener {
        public void onNewSyncItem(ItemPriority itemPriority);
        public void newSyncItems(Set<ItemPriority> itemPriorities);
    }
    
    public void setSyncItemChangesListener(SyncItemListener listener); 

    public long addReadEvent(long uid, ItemPriority priority);

    public long addDeleteEvent(long uid, ItemPriority priority);


    /**
     * This Method
     * 
     * @param item
     * @return
     */
    public long addFetchMessageEvent(long uid, ItemPriority priority);

    /**
     * This Method
     * 
     * @param uid
     */
    public long addFetchHeaderEvent(long uid, ItemPriority priority);

    public long addFetchAttachmentEvent(long uid, ItemPriority priority);

    public int addEventInBatch(List<SyncItem> syncItems);

    /**
     * This Method
     * 
     * @param maxQueueSize
     * @return
     */
    public List<SyncItem> getSendItems(int maxQueueSize);

    /**
     * This Method
     * 
     * @param maxQueueSize
     * @return
     */

    public List<SyncItem> getFetchItems(int maxQueueSize);

    /**
     * This Method
     * 
     * @return
     */
    public List<SyncItem> getPendingReadMessages(int maxQueueSize);

    /**
     * This Method
     * 
     * @param uids
     */
    public int deleteEventsWithUids(List<Long> uids);
    
    public int deleteEventsWithLuid(long luid);
    
    public int deleteEventsWithIds(List<Long> ids);
    
    
    /**
     * This Method
     * 
     * @return
     */
    public List<SyncItem> getPendingDeletedMessages(int maxQueueSize);

    /**
     * This Method
     * 
     * @param luid
     */
    public long addSendSmsEvent(long luid);

    /**
     * This Method
     * 
     * @param luid
     */
    public long addSendMmsEvent(long luid, boolean hasAttachemen);

    /**
     * This Method
     * 
     * @param i
     * @return
     */
    public List<SyncItem> getFetchAttachementsItems(int i);

    /**
     * This Method
     * 
     * @return
     */
    public boolean hasPendingSendItems();

    /**
     * This Method
     * 
     * @return
     */
    public boolean hasPendingFetchItems();

    /**
     * This Method
     * 
     * @param id
     */
    public int deleteEvent(long id);

    /**
     * This Method
     * 
     * @param syncItem
     * @return
     */
    public int updateSyncItem(SyncItem syncItem);
    
    
    public  int updateDefferedItemsBackToQueue();
    
    public boolean hasPendingItems();

    /**
     * This Method 
     * @return
     */
    public boolean hasPendingFullSyncItems();

    /**
     * This Method 
     */
    public long addFullSyncEvent();
    
    public int deleteFullSyncEvent();

    /**
     * This Method 
     * @return
     */
    public List<SyncItem> getFullSyncItems();
    
    public Set<ItemPriority> getUniqueItemPriorities();

    /**
     * This Method 
     */
    public int deleteAllPendingEvents();

    /**
     * This Method 
     * @param maxDbQuerySize
     * @return
     */
    public List<SyncItem> getSendSmsItems(int maxDbQuerySize);

    /**
     * This Method 
     * @param maxDbQuerySize
     * @return
     */
    public List<SyncItem> getSendMmsItems(int maxDbQuerySize);


}
