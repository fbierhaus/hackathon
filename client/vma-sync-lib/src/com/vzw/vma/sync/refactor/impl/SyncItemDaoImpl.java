/**
 * SyncItemDaoImpl.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.refactor.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.provider.ApplicationProvider;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemAction;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemType;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.verizon.mms.util.SqliteWrapper;

/**
 * This class is used to do CURD operations on SyncItem table.
 * 
 * @author Jegadeesan M
 * @Since Feb 12, 2013
 */
public class SyncItemDaoImpl implements SyncItemDao {

    private ContentResolver resolver;
    private SyncItemListener syncItemListener;
    private Context context;

    /**
     * 
     * Constructor
     */
    public SyncItemDaoImpl(Context context) {
        this.context = context;
        this.resolver = context.getContentResolver();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#addReadEvent(long,
     * com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority)
     */
    @Override
    public long addReadEvent(long uid, ItemPriority priority) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addReadEvent :uid=" + uid + ",priority=" + priority);
        }
        if (uid <= 0) {
            Logger.error("Error uid to read/delete is 0. Critical error");
            throw new RuntimeException("Error uid to read/delete is 0. Critical error");
        }
        ContentValues values = new ContentValues();
        values.put(SyncItem._ACTION, ItemAction.READ.getValue());
        values.put(SyncItem._PRIORITY, priority.getValue());
        values.put(SyncItem._ITEM_ID, uid);
        long id = ContentUris.parseId(SqliteWrapper.insert(context, resolver, SyncItem.CONTENT_URI, values));
        // long id = ContentUris.parseId(resolver.insert(SyncItem.CONTENT_URI, values));
        if (syncItemListener != null) {
            syncItemListener.onNewSyncItem(priority);
        }
        return id;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#addFullSyncEvent()
     */
    @Override
    public long addFullSyncEvent() {
        ContentValues values = new ContentValues();
        values.put(SyncItem._ACTION, ItemAction.FULLSYNC.getValue());
        values.put(SyncItem._ITEM_ID, Long.MAX_VALUE);
        values.put(SyncItem._PRIORITY, ItemPriority.INITIALSYNC_CRITICAL.getValue());
        long id = ContentUris.parseId(SqliteWrapper.insert(context, resolver, SyncItem.CONTENT_URI, values));
        // long id = ContentUris.parseId(resolver.insert(SyncItem.CONTENT_URI, values));
        if (syncItemListener != null) {
            syncItemListener.onNewSyncItem(ItemPriority.INITIALSYNC_CRITICAL);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("added FullSync Event id=" + id);
        }
        return id;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#addSendSmsEvent(long)
     */
    @Override
    public long addSendSmsEvent(long luid) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addSendSmsEvent :luid=" + luid);
        }
        if (luid <= 0) {
            Logger.error("Error uid to read/delete is 0. Critical error");
            throw new RuntimeException("Error uid to read/delete is 0. Critical error");
        }
        ContentValues values = new ContentValues();
        values.put(SyncItem._ACTION, ItemAction.SEND_SMS.getValue());
        values.put(SyncItem._TYPE, ItemType.SMS.getValue());
        values.put(SyncItem._PRIORITY, ItemPriority.SENDSMS_CRITICAL.getValue());
        values.put(SyncItem._ITEM_ID, luid);
        // long id = ContentUris.parseId(resolver.insert(SyncItem.CONTENT_URI, values));
        long id = ContentUris.parseId(SqliteWrapper.insert(context, resolver, SyncItem.CONTENT_URI, values));
        if (syncItemListener != null) {
            syncItemListener.onNewSyncItem(ItemPriority.SENDSMS_CRITICAL);
        }
        return id;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#addSendMmsEvent(long)
     */
    @Override
    public long addSendMmsEvent(long luid, boolean hasAttachement) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addSendMmsEvent :luid=" + luid);
        }
        if (luid <= 0) {
            Logger.error("Error uid to read/delete is 0. Critical error");
            throw new RuntimeException("Error uid to read/delete is 0. Critical error");
        }
        ContentValues values = new ContentValues();
        values.put(SyncItem._ACTION, ItemAction.SEND_MMS.getValue());
        values.put(SyncItem._TYPE, ItemType.MMS.getValue());
        if (hasAttachement) {
            values.put(SyncItem._PRIORITY, ItemPriority.SENDMMS_ATTACHEMENT.getValue());
        } else {
            values.put(SyncItem._PRIORITY, ItemPriority.SENDMMS_CRITICAL.getValue());
        }
        values.put(SyncItem._ITEM_ID, luid);
        // long id = ContentUris.parseId(resolver.insert(SyncItem.CONTENT_URI, values));
        long id = ContentUris.parseId(SqliteWrapper.insert(context, resolver, SyncItem.CONTENT_URI, values));
        if (syncItemListener != null) {
            syncItemListener.onNewSyncItem(ItemPriority.SENDMMS_CRITICAL);
        }
        return id;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#addDeleteEvent(long,
     * com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority)
     */
    @Override
    public long addDeleteEvent(long uid, ItemPriority priority) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addDeleteEvent :uid=" + uid + ",priority=" + priority);
        }
        if (uid <= 0) {
            Logger.error("Error uid to read/delete is 0. Critical error");
            throw new RuntimeException("Error uid to read/delete is 0. Critical error");
        }
        ContentValues values = new ContentValues();
        values.put(SyncItem._ACTION, ItemAction.DELETE.getValue());
        values.put(SyncItem._PRIORITY, priority.getValue());
        values.put(SyncItem._ITEM_ID, uid);
        // long id = ContentUris.parseId(resolver.insert(SyncItem.CONTENT_URI, values));
        long id = ContentUris.parseId(SqliteWrapper.insert(context, resolver, SyncItem.CONTENT_URI, values));
        if (syncItemListener != null) {
            syncItemListener.onNewSyncItem(priority);
        }
        return id;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#addFetchMessageEvent(long,
     * com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority)
     */
    @Override
    public long addFetchMessageEvent(long uid, ItemPriority priority) {

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addFetchMessageEvent :uid=" + uid + ",priority=" + priority);
        }
        if (uid <= 0) {
            Logger.error("Error uid to read/delete is 0. Critical error");
            throw new RuntimeException("Error uid to read/delete is 0. Critical error");
        }
        ContentValues values = new ContentValues();
        values.put(SyncItem._ACTION, ItemAction.FETCH_MESSAGE.getValue());
        values.put(SyncItem._PRIORITY, priority.getValue());
        values.put(SyncItem._ITEM_ID, uid);
        // long id = ContentUris.parseId(resolver.insert(SyncItem.CONTENT_URI, values));
        long id = ContentUris.parseId(SqliteWrapper.insert(context, resolver, SyncItem.CONTENT_URI, values));
        if (syncItemListener != null) {
            syncItemListener.onNewSyncItem(priority);
        }
        return id;

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#addFetchHeaderEvent(long,
     * com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority)
     */
    @Override
    public long addFetchHeaderEvent(long uid, ItemPriority priority) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addFetchHeaderEvent :uid=" + uid + ",priority=" + priority);
        }
        if (uid <= 0) {
            Logger.error("Error uid to read/delete is 0. Critical error");
            throw new RuntimeException("Error uid to read/delete is 0. Critical error");
        }
        ContentValues values = new ContentValues();
        values.put(SyncItem._ACTION, ItemAction.FETCH_MESSAGE_HEADERS.getValue());
        values.put(SyncItem._PRIORITY, priority.getValue());
        values.put(SyncItem._ITEM_ID, uid);
        // long id = ContentUris.parseId(resolver.insert(SyncItem.CONTENT_URI, values));
        long id = ContentUris.parseId(SqliteWrapper.insert(context, resolver, SyncItem.CONTENT_URI, values));
        if (syncItemListener != null) {
            syncItemListener.onNewSyncItem(priority);
        }
        return id;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#addFetchAttachmentEvent(long,
     * com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority)
     */
    @Override
    public long addFetchAttachmentEvent(long uid, ItemPriority priority) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addFetchAttachmentEvent :uid=" + uid + ",priority=" + priority);
        }
        if (uid <= 0) {
            Logger.error("Error uid to read/delete is 0. Critical error");
            throw new RuntimeException("Error uid to read/delete is 0. Critical error");
        }
        ContentValues values = new ContentValues();
        values.put(SyncItem._ACTION, ItemAction.FETCH_ATTACHMENT.getValue());
        values.put(SyncItem._PRIORITY, priority.getValue());
        values.put(SyncItem._ITEM_ID, uid);
        long id = ContentUris.parseId(SqliteWrapper.insert(context, resolver, SyncItem.CONTENT_URI, values));
        if (syncItemListener != null) {
            syncItemListener.onNewSyncItem(priority);
        }
        return id;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#addEventInBatch(java.util.List)
     */
    @Override
    public int addEventInBatch(List<SyncItem> syncItems) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addEventInBatch :syncItems=" + syncItems);
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        Set<ItemPriority> updateItems = new HashSet<SyncItem.ItemPriority>();
        for (SyncItem syncItem : syncItems) {
            Builder b = ContentProviderOperation.newInsert(SyncItem.CONTENT_URI);
            b.withValues(toValues(syncItem));
            ops.add(b.build());
            updateItems.add(syncItem.priority);
        }
        try {
            ContentProviderResult[] result = SqliteWrapper.applyBatch(context, resolver,
                    ApplicationProvider.AUTHORITY, ops);
            // ContentProviderResult[] result = resolver.applyBatch(ApplicationProvider.AUTHORITY, ops);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("addEventInBatch :returns=" + result.length);
            }
            if (syncItemListener != null) {
                syncItemListener.newSyncItems(updateItems);
            }
            return result.length;
        } catch (RemoteException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error(false, e);
            }
        } catch (OperationApplicationException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error(false, e);
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addEventInBatch :returns=-1");
        }
        return -1;
    }

    /**
     * This Method
     * 
     * @param syncItem
     * @return
     */
    private ContentValues toValues(SyncItem syncItem) {
        ContentValues values = new ContentValues();
        values.put(SyncItem._ACTION, syncItem.action.getValue());

        values.put(SyncItem._PRIORITY, syncItem.priority.getValue());

        if (syncItem.lastPriority != null) {
            values.put(SyncItem._LAST_PRIORITY, syncItem.lastPriority.getValue());
        }
        values.put(SyncItem._ITEM_ID, syncItem.itemId);
        values.put(SyncItem._TYPE, syncItem.type.getValue());
        values.put(SyncItem._RETRY_COUNT, syncItem.retryCount);
        return values;
    }

    /**
     * This Method
     * 
     * @param c
     * @param item
     */
    private void populateSyncItem(Cursor c, SyncItem item) {
        item.id = c.getLong(c.getColumnIndex(SyncItem._ID));

        // XXX: SANDEEP WHY ARE WE POPULATING FROM LUID LETS FIX THIS ASAP
        // IF WE NEED TO LETS KEEP BOTH A UID & A LUID FIELD IN THE DB OR
        // RENAME uid/luid in the OBJECT TO BE itemid so that either LUID OR UID CAN GO IN IT

        item.itemId = c.getLong(c.getColumnIndex(SyncItem._ITEM_ID));

        item.itemId = c.getLong(c.getColumnIndex(SyncItem._ITEM_ID));
        item.retryCount = c.getInt(c.getColumnIndex(SyncItem._RETRY_COUNT));
        item.priority = ItemPriority.toEnum(c.getInt(c.getColumnIndex(SyncItem._PRIORITY)));
        item.lastPriority = ItemPriority.toEnum(c.getInt(c.getColumnIndex(SyncItem._LAST_PRIORITY)));
        item.action = ItemAction.toEnum(c.getInt(c.getColumnIndex(SyncItem._ACTION)));
        item.type = ItemType.toEnum(c.getInt(c.getColumnIndex(SyncItem._TYPE)));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#getFetchItems(int)
     */
    @Override
    public List<SyncItem> getFetchItems(int maxQueueSize) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getFetchItems :maxQueueSize=" + maxQueueSize);
        }
        String selection = SyncItem._PRIORITY + "<=" + ItemPriority.ONDEMAND_MAX.getValue() + " AND "
                + SyncItem._PRIORITY + ">=" + ItemPriority.FULLSYNC_MIN.getValue();
        String sortOrder = SyncItem._PRIORITY + " DESC LIMIT " + maxQueueSize;
        return getPendingItems(selection, sortOrder);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#getPendingReadMessages(int)
     */
    @Override
    public List<SyncItem> getPendingReadMessages(int maxQueueSize) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getSendItems :maxQueueSize=" + maxQueueSize);
        }
        String selection = SyncItem._ACTION + "=" + ItemAction.READ.getValue();
        String sortOrder = SyncItem._PRIORITY + " DESC LIMIT " + maxQueueSize;
        return getPendingItems(selection, sortOrder);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#getFullSyncItems()
     */
    @Override
    public List<SyncItem> getFullSyncItems() {
        String selection = SyncItem._ACTION + "=" + ItemAction.FULLSYNC.getValue();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getFullSyncItems()");
        }
        return getPendingItems(selection, null);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#getSendItems(int)
     */
    @Override
    public List<SyncItem> getSendItems(int maxQueueSize) {
        String selection = SyncItem._PRIORITY + "<=" + ItemPriority.SEND_MAX.getValue() + " AND "
                + SyncItem._PRIORITY + ">=" + ItemPriority.SEND_MIN.getValue();
        String sortOrder = SyncItem._PRIORITY + " DESC ," + SyncItem._RETRY_COUNT + " ASC LIMIT "
                + maxQueueSize;
        List<SyncItem> items = getPendingItems(selection, sortOrder);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getSendItems :items=" + items);
        }
        return items;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#getFetchAttachementsItems(int)
     */
    @Override
    public List<SyncItem> getFetchAttachementsItems(int i) {

        throw new UnsupportedOperationException();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#getPendingDeletedMessages()
     */
    @Override
    public List<SyncItem> getPendingDeletedMessages(int maxQueueSize) {
        String selection = SyncItem._ACTION + "=" + ItemAction.DELETE.getValue();
        // String sortOrder = SyncItem._PRIORITY + " DESC LIMIT " + maxQueueSize;
        String sortOrder = SyncItem._PRIORITY + " DESC LIMIT " + maxQueueSize;
        List<SyncItem> items = getPendingItems(selection, sortOrder);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getPendingDeletedMessages() : items=" + items);
        }
        return items;
    }

    /**
     * This Method
     * 
     * @param selection
     * @param items
     * @param sortOrder
     * @return
     */
    private List<SyncItem> getPendingItems(String selection, String sortOrder) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Querying pending items: selection=" + selection + ",sort=" + sortOrder);
        }
        List<SyncItem> items = null;
        Cursor c = SqliteWrapper.query(context, resolver, SyncItem.CONTENT_URI, null, selection, null,
                sortOrder);
        // Cursor c = resolver.query(SyncItem.CONTENT_URI, null, selection, null, sortOrder);
        if (c != null) {
            if (c.getCount() > 0) {
                items = new ArrayList<SyncItem>();
                while (c.moveToNext()) {
                    SyncItem item = new SyncItem();
                    populateSyncItem(c, item);
                    items.add(item);
                }
            }
            c.close();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("pending items count=" + ((items != null) ? items.size() : 0));
        }
        return items;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#deleteEventsWithUid(java.util.List)
     */
    @Override
    public int deleteEventsWithUids(List<Long> uids) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("deleteEventsWithUid :uids=" + uids);
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (long uid : uids) {
            Builder b = ContentProviderOperation.newDelete(SyncItem.CONTENT_URI);
            b.withSelection(SyncItem._ITEM_ID + "=" + uid, null);
            ops.add(b.build());
        }
        try {
            ContentProviderResult[] result = SqliteWrapper.applyBatch(context, resolver,
                    ApplicationProvider.AUTHORITY, ops);
            // ContentProviderResult[] result = resolver.applyBatch(ApplicationProvider.AUTHORITY, ops);
            if (result != null) {
                return result.length;
            }
        } catch (RemoteException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error(false, e);
            }
        } catch (OperationApplicationException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error(false, e);
            }
        }
        return -1;
    }

    @Override
    public int deleteEventsWithLuid(long luid) {
        // Check that event action type is SEND what is supported by luid
        // return resolver.delete(SyncItem.CONTENT_URI, SyncItem._ITEM_ID + "=" + luid, null);
        return SqliteWrapper.delete(context, resolver, SyncItem.CONTENT_URI, SyncItem._ITEM_ID + "=" + luid,
                null);
    }

    @Override
    public int deleteEventsWithIds(List<Long> ids) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("deleteEventsWithUid :uids=" + ids);
        }
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for (long id : ids) {
            Builder b = ContentProviderOperation.newDelete(SyncItem.CONTENT_URI);
            b.withSelection(SyncItem._ID + "=" + id, null);
            ops.add(b.build());
        }
        try {
            ContentProviderResult[] result = SqliteWrapper.applyBatch(context, resolver,
                    ApplicationProvider.AUTHORITY, ops);
            // ContentProviderResult[] result = resolver.applyBatch(ApplicationProvider.AUTHORITY, ops);
            if (result != null) {
                return result.length;
            }
        } catch (RemoteException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error(false, e);
            }
        } catch (OperationApplicationException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error(false, e);
            }
        }
        return -1;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#hasPendingSendItems()
     */
    @Override
    public boolean hasPendingSendItems() {
        String selection = SyncItem._PRIORITY + "<=" + ItemPriority.SEND_MAX.getValue() + " AND "
                + SyncItem._PRIORITY + ">=" + ItemPriority.SEND_MIN.getValue();
        return getCount(selection) > 0;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#hasPendingItems()
     */
    @Override
    public boolean hasPendingItems() {
        String selection = SyncItem._PRIORITY + ">" + ItemPriority.DEFFERED.getValue();
        boolean result = getCount(selection) > 0;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("hasPendingSendItems: result=" + result);
        }
        return result;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#hasPendingFullSyncItems()
     */
    @Override
    public boolean hasPendingFullSyncItems() {
        String selection = SyncItem._ACTION + "=" + ItemAction.FULLSYNC.getValue();
        boolean result = getCount(selection) > 0;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("hasPendingFullSyncItems: result=" + result);
        }
        return result;
    }

    /**
     * This Method
     * 
     * @param selection
     * @return
     */
    private long getCount(String selection) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getSyncItemsCount: selection=" + selection);
        }
        Cursor c = SqliteWrapper.query(context, resolver, SyncItem.CONTENT_URI,
                new String[] { "COUNT(*) AS count" }, selection,
                // Cursor c = resolver.query(SyncItem.CONTENT_URI, new String[] { "COUNT(*) AS count" },
                // selection,
                null, null);
        long result = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                result = c.getLong(0);
            }
            c.close();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getSyncItemsCount: result=" + result);
        }
        return result;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#hasPendingFetchItems()
     */
    @Override
    public boolean hasPendingFetchItems() {
        String selection = SyncItem._PRIORITY + "<=" + ItemPriority.ONDEMAND_MAX.getValue() + " AND "
                + SyncItem._PRIORITY + ">" + ItemPriority.FULLSYNC_MIN.getValue();
        return getCount(selection) > 0;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.SyncItemDao#deleteEvent(long)
     */
    @Override
    public int deleteEvent(long id) {
        return SqliteWrapper.delete(context, resolver, SyncItem.CONTENT_URI, SyncItem._ID + "=" + id, null);
        // return resolver.delete(SyncItem.CONTENT_URI, SyncItem._ID + "=" + id, null);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see
     * com.vzw.vma.sync.refactor.SyncItemDao#updateSyncItem(com.verizon.messaging.vzmsgs.provider.SyncItem)
     */
    @Override
    public int updateSyncItem(SyncItem syncItem) {
        if (syncItem.itemId <= 0) {
            Logger.debug("=====> Check why we are updating syncItem value to something that has itemid <=0"
                    + syncItem);
        }
        return SqliteWrapper.update(context, resolver, SyncItem.CONTENT_URI, toValues(syncItem), SyncItem._ID
                + "=" + syncItem.id, null);
        // return resolver.update(SyncItem.CONTENT_URI, toValues(syncItem), SyncItem._ID + "=" + syncItem.id,
        // null);
    }

    private int commonUpdateDefferedPriority(String selection) {
        int count = -1;
        ArrayList<ContentProviderOperation> operations = null;
        Cursor c = SqliteWrapper.query(context, resolver, SyncItem.CONTENT_URI, new String[] { SyncItem._ID,
                SyncItem._LAST_PRIORITY }, selection, null, null);
        // Cursor c = resolver.query(SyncItem.CONTENT_URI, new String[]{SyncItem._ID
        // ,SyncItem._LAST_PRIORITY}, selection, null, null);
        if (c != null) {
            count = c.getCount();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Found deffered items=" + count + ",selection=" + selection);
            }
            if (c.getCount() > 0) {
                operations = new ArrayList<ContentProviderOperation>();
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    long lastPriority = c.getLong(1);
                    Builder b = ContentProviderOperation.newUpdate(ContentUris.withAppendedId(
                            SyncItem.CONTENT_URI, id));
                    b.withValue(SyncItem._PRIORITY, lastPriority);
                    b.withValue(SyncItem._LAST_PRIORITY, 0);
                    b.withValue(SyncItem._RETRY_COUNT, 0);
                    operations.add(b.build());
                }
            }
            c.close();
        }
        if (operations != null && !operations.isEmpty()) {
            try {
                return SqliteWrapper.applyBatch(context, resolver, ApplicationProvider.AUTHORITY, operations).length;
            } catch (RemoteException e) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error("Batch execution failed.selection=" + selection, e);
                }
                return -1;
            } catch (OperationApplicationException e) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error("Batch execution failed.selection=" + selection, e);
                }
                return -1;
            }
        }
        return count;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#deleteFullSyncEvent()
     */
    @Override
    public int deleteFullSyncEvent() {
        return SqliteWrapper.delete(context, resolver, SyncItem.CONTENT_URI, SyncItem._ACTION + "="
                + ItemAction.FULLSYNC.getValue(), null);
        // return resolver.delete(SyncItem.CONTENT_URI, SyncItem._ACTION + "=" +
        // ItemAction.FULLSYNC.getValue(),
        // null);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see
     * com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#setSyncItemChangesListener(com.verizon.messaging
     * .vzmsgs.provider.dao.SyncItemDao.SyncItemListener)
     */
    @Override
    public void setSyncItemChangesListener(SyncItemListener listener) {
        this.syncItemListener = listener;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#getQueuedPriorities()
     */
    @Override
    public Set<ItemPriority> getUniqueItemPriorities() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getQueuedPriorities:");
        }
        String[] projecction = new String[] { " DISTINCT " + SyncItem._PRIORITY };
        Cursor c = SqliteWrapper
                .query(context, resolver, SyncItem.CONTENT_URI, projecction, null, null, null);
        // Cursor c = resolver.query(SyncItem.CONTENT_URI, projecction, null, null, null);
        Set<ItemPriority> priorities = new HashSet<SyncItem.ItemPriority>();
        long result = 0;
        if (c != null) {
            while (c.moveToNext()) {
                priorities.add(ItemPriority.toEnum(c.getInt(0)));
            }
            c.close();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getQueuedPriorities: result=" + result);
        }
        return priorities;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#deleteAllPendingEvents()
     */
    @Override
    public int deleteAllPendingEvents() {
        return SqliteWrapper.delete(context, resolver, SyncItem.CONTENT_URI, SyncItem._ID + "> 0", null);
        // return resolver.delete(SyncItem.CONTENT_URI, SyncItem._ID + "> 0", null);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#updateDefferedItemsBackToQueue()
     */
    @Override
    public synchronized int updateDefferedItemsBackToQueue() {
        int updateCount = commonUpdateDefferedPriority(SyncItem._PRIORITY + "="
                + ItemPriority.DEFFERED.getValue());
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("updateDefferedItemsBackToQueue(): updatecount=" + updateCount);
        }
        return updateCount;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#getSendSmsItems(int)
     */
    @Override
    public List<SyncItem> getSendSmsItems(int maxDbQuerySize) {
        String selection = SyncItem._PRIORITY + "<=" + ItemPriority.SENDSMS_CRITICAL.getValue() + " AND "
                + SyncItem._PRIORITY + ">=" + ItemPriority.SEND_MIN.getValue() + " AND " + SyncItem._ACTION
                + "=" + ItemAction.SEND_SMS.getValue();
        String sortOrder = SyncItem._PRIORITY + " DESC LIMIT " + maxDbQuerySize;
        List<SyncItem> items = getPendingItems(selection, sortOrder);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getSendItems :items=" + items);
        }
        return items;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao#getSendMmsItems(int)
     */
    @Override
    public List<SyncItem> getSendMmsItems(int maxDbQuerySize) {
        String selection = SyncItem._PRIORITY + "<=" + ItemPriority.SENDMMS_CRITICAL.getValue() + " AND "
                + SyncItem._PRIORITY + ">=" + ItemPriority.SEND_MIN.getValue() + " AND " + SyncItem._ACTION
                + "=" + ItemAction.SEND_MMS.getValue();
        String sortOrder = SyncItem._PRIORITY + " DESC LIMIT " + maxDbQuerySize;
        List<SyncItem> items = getPendingItems(selection, sortOrder);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getSendItems :items=" + items);
        }
        return items;
    }

}
