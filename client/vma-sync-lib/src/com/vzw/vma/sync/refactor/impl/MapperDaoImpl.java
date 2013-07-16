/**
 * MapperDaoImpl.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.refactor.impl;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.messaging.vzmsgs.provider.dao.MapperDao;
import com.verizon.mms.util.SqliteWrapper;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Feb 12, 2013
 */
public class MapperDaoImpl implements MapperDao {

    private ContentResolver resolver;
    private Context context;

    /**
     * 
     * Constructor
     */
    public MapperDaoImpl(Context context) {
        this.context = context;
        this.resolver = context.getContentResolver();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.MapperDao#findMappingByMessageId(java.lang.String, int)
     */
    @Override
    public VMAMapping findMappingByMessageId(String messageId, int msgtype) {

        String selection = VMAMapping._MSGID + "='" + messageId + "' AND " + VMAMapping._TYPE + "=" + msgtype;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByMessageId: selection=" + selection);
        }
        // default projection
        Cursor c = SqliteWrapper
                .query(context, resolver, VMAMapping.CONTENT_URI, null, selection, null, null);
        VMAMapping mapping = null;
        if (c != null) {
            while (c.moveToNext()) {
                mapping = new VMAMapping();
                populateVMAMappingDefaultProjection(c, mapping);
                break;
            }
            c.close();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByMessageId: mapping=" + mapping);
        }
        return mapping;
    }

    /**
     * This Method
     * 
     * @param c
     * @param mapping
     */
    private void populateVMAMappingDefaultProjection(Cursor c, VMAMapping mapping) {

        mapping.setId(c.getLong(c.getColumnIndex(VMAMapping._ID)));
        mapping.setTimeCreated(c.getLong(c.getColumnIndex(VMAMapping._TIME_CREATED)));
        mapping.setTimeUpdated(c.getLong(c.getColumnIndex(VMAMapping._TIME_UPDATED)));
        mapping.setSourceCreatedFrom(c.getInt(c.getColumnIndex(VMAMapping._SOURCE_CREATED_FROM)));
        mapping.setType(c.getInt(c.getColumnIndex(VMAMapping._TYPE)));
        mapping.setLuid(c.getLong(c.getColumnIndex(VMAMapping._LUID)));
        mapping.setThreadId(c.getLong(c.getColumnIndex(VMAMapping._THREAD_ID)));
        mapping.setMsgid(c.getString(c.getColumnIndex(VMAMapping._MSGID)));
        mapping.setUid(c.getLong(c.getColumnIndex(VMAMapping._UID)));
        mapping.setTimeofmessage(c.getLong(c.getColumnIndex(VMAMapping._TIMEOFMESSAGE)));
        mapping.setSmschecksum(c.getLong(c.getColumnIndex(VMAMapping._SMSCHECKSUM)));
        mapping.setVmaflags(c.getInt(c.getColumnIndex(VMAMapping._VMAFLAGS)));
        mapping.setPendingUievents(c.getInt(c.getColumnIndex(VMAMapping._PENDING_UI_EVENTS)));
        mapping.setSource(c.getInt(c.getColumnIndex(VMAMapping._SOURCE)));
        mapping.setMessageBox(c.getInt(c.getColumnIndex(VMAMapping._MSG_BOX)));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see
     * com.vzw.vma.sync.refactor.MapperDao#createMapping(com.verizon.messaging.vzmsgs.provider.VMAMapping)
     */
    @Override
    public VMAMapping createMapping(VMAMapping inputmap) {

        if (inputmap.getMessageBox() == 0) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("ERRRRRRRRRRRRRRROR - messagebox should be non zero >>>>>>>>>> Throwing RuntimeException.");
                throw new RuntimeException("messageBox value is 0");
            }
        }
        ContentValues values = new ContentValues(14);
        values.put(VMAMapping._TIME_CREATED, inputmap.getTimeCreated());
        values.put(VMAMapping._TIME_UPDATED, inputmap.getTimeUpdated());
        values.put(VMAMapping._SOURCE_CREATED_FROM, inputmap.getSourceCreatedFrom());
        values.put(VMAMapping._TYPE, inputmap.getType());
        values.put(VMAMapping._LUID, inputmap.getLuid());
        values.put(VMAMapping._THREAD_ID, inputmap.getThreadId());
        values.put(VMAMapping._MSGID, inputmap.getMsgid());
        values.put(VMAMapping._UID, inputmap.getUid());
        values.put(VMAMapping._TIMEOFMESSAGE, inputmap.getTimeofmessage());
        values.put(VMAMapping._SMSCHECKSUM, inputmap.getSmschecksum());
        values.put(VMAMapping._VMAFLAGS, inputmap.getVmaflags());
        values.put(VMAMapping._PENDING_UI_EVENTS, inputmap.getPendingUievents());
        values.put(VMAMapping._SOURCE, inputmap.getSource());
        values.put(VMAMapping._MSG_BOX, inputmap.getMessageBox());
        Uri insertUri = SqliteWrapper.insert(context, resolver, VMAMapping.CONTENT_URI, values);
        inputmap.setId(ContentUris.parseId(insertUri));
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createMapping: created mapping=" + inputmap);
        }
        return inputmap;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.MapperDao#findMappingByPduLuid(long, int)
     */
    @Override
    public VMAMapping findMappingByPduLuid(long luid, int typeSms) {

        String selection = VMAMapping._LUID + "=" + luid + " AND " + VMAMapping._TYPE + "=" + typeSms;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByPduLuid: selection=" + selection);
        }
        // default projection
        Cursor c = SqliteWrapper
                .query(context, resolver, VMAMapping.CONTENT_URI, null, selection, null, null);
        VMAMapping mapping = null;
        if (c != null) {
            while (c.moveToNext()) {
                mapping = new VMAMapping();
                populateVMAMappingDefaultProjection(c, mapping);
                break;
            }
            c.close();
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByPduLuid:  mapping=" + mapping);
        }
        return mapping;

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.MapperDao#findMappingByChecksum(long, long)
     */
    @Override
    public List<VMAMapping> findAllMappingsByChecksum(long checksum, CallerSource src, int messageBox) {

        ArrayList<VMAMapping> mappings = new ArrayList<VMAMapping>();

        String idToExamine = src == CallerSource.TELEPHONY ? VMAMapping._LUID : VMAMapping._UID;
        String[] projection = new String[] { idToExamine, VMAMapping._TIMEOFMESSAGE };
        String where = VMAMapping._SMSCHECKSUM + "=" + checksum + " AND " + idToExamine + " =0 AND "
                + VMAMapping._MSG_BOX + " = " + messageBox;

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByChecksum : where=" + where + " for source=" + src);
        }

        String sortOrder = VMAMapping._TIMEOFMESSAGE + " DESC ";
        Cursor c = SqliteWrapper.query(context, resolver, VMAMapping.CONTENT_URI, null, where, null,
                sortOrder);
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    VMAMapping mapping = new VMAMapping();
                    populateVMAMappingDefaultProjection(c, mapping);
                    mappings.add(mapping);
                }
            } finally {
                c.close();
            }
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByChecksum : returning mapping=" + mappings);
        }
        return mappings;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.MapperDao#findMappingByChecksum(long, long)
     */
    @Override
    public VMAMapping findMappingByChecksum(long checksum, long srcTime, CallerSource src) {

        VMAMapping mapping = null;

        String idToExamine = src == CallerSource.TELEPHONY ? VMAMapping._LUID : VMAMapping._UID;
        String[] projection = new String[] { idToExamine, VMAMapping._TIMEOFMESSAGE };
        String where = VMAMapping._SMSCHECKSUM + "=" + checksum + " AND " + idToExamine + " <=0 ";

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByChecksum : where=" + where + " for source=" + src);
        }
        // In this case. if the conversation have same message/checksum we are filtering message based on the
        // SMSC timestamp to pick the least one.
        String sortOrder = VMAMapping._TIMEOFMESSAGE + " DESC ";
        Cursor c = SqliteWrapper.query(context, resolver, VMAMapping.CONTENT_URI, null, where, null,
                sortOrder);
        int position = -1;
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    // populateVMAMappingDefaultProjection(c, mapping);
                    position = c.getPosition();
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Found " + c.getCount() + " matches for checksum=" + checksum);
                    }
                    if (c.getCount() > 1) {
                        long oldTime = Long.MAX_VALUE;
                        do {
                            if (c.getLong(1) <= srcTime) {
                                long newTime = c.getLong(1);
                                long timeDiff = srcTime - newTime;
                                long oldTimeDiff = oldTime - srcTime;
                                if (Logger.IS_DEBUG_ENABLED) {
                                    Logger.debug("getMappedSmsLuid :inside loop " + c.getLong(0)
                                            + "total count" + c.getCount() + "newTime" + newTime + "timeDiff"
                                            + timeDiff + "oldTimeDiff" + oldTimeDiff);
                                }
                                if (timeDiff < oldTimeDiff) {
                                    position = c.getPosition();
                                }
                                break;
                            }
                            // populateVMAMappingDefaultProjection(c, mapping);
                            position = c.getPosition();
                            oldTime = c.getLong(1);
                        } while (c.moveToNext());
                    } else {

                    }
                }
                if (position != -1) {
                    mapping = new VMAMapping();
                    c.moveToPosition(position);
                    populateVMAMappingDefaultProjection(c, mapping);
                }
            } finally {
                c.close();
            }

        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByChecksum : returning mapping=" + mapping);
        }
        return mapping;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.MapperDao#findMappingByUid(long)
     */
    @Override
    public VMAMapping findMappingByUid(long uid) {
        String selection = VMAMapping._UID + "=" + uid;
        // default projection
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByUid: selection=" + selection);
        }
        Cursor c = SqliteWrapper
                .query(context, resolver, VMAMapping.CONTENT_URI, null, selection, null, null);
        VMAMapping mapping = null;
        if (c != null) {
            while (c.moveToNext()) {
                mapping = new VMAMapping();
                populateVMAMappingDefaultProjection(c, mapping);
                break;
            }
            c.close();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByUid : returning mapping=" + mapping);
        }
        return mapping;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.MapperDao#updateMappingWithluid(long, long, long)
     */
    @Override
    public int updateMappingWithluid(long id, long luid, long threadId) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("updateMappingWithluid: id=" + id + " luid=" + luid + " threadId=" + threadId);
        }
        ContentValues values = new ContentValues(2);
        values.put(VMAMapping._LUID, luid);
        values.put(VMAMapping._THREAD_ID, threadId);
        return SqliteWrapper.update(context, resolver, VMAMapping.CONTENT_URI, values, VMAMapping._ID + "="
                + id, null);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.MapperDao#updateMessageIdAndFlags(long, java.lang.String, int)
     */
    @Override
    public int updateMessageIdAndFlags(long id, String msgId, int vmaflags) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("updateMessageIdAndFlags: id=" + id + " msgId=" + msgId + " vmaflags=" + vmaflags);
        }
        ContentValues values = new ContentValues(2);
        values.put(VMAMapping._MSGID, msgId);
        values.put(VMAMapping._VMAFLAGS, vmaflags);
        return SqliteWrapper.update(context, resolver, VMAMapping.CONTENT_URI, values, VMAMapping._ID + "="
                + id, null);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.MapperDao#addPendingUiEvent(long, int)
     */
    @Override
    public int addPendingUiEvent(long id, int code) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addPendingUiEvent: id=" + id + " code=" + code);
        }
        ContentValues values = new ContentValues(1);
        values.put(VMAMapping._PENDING_UI_EVENTS, code);
        return SqliteWrapper.update(context, resolver, VMAMapping.CONTENT_URI, values, VMAMapping._ID + "="
                + id, null);
    }
    /* Overriding method 
     * (non-Javadoc)
     * @see com.verizon.messaging.vzmsgs.provider.dao.MapperDao#addPendingUiEvent(long, long, long, int)
     */
    @Override
    public int addPendingUiEvent(long id,long tempLuid, long oldLuid, int code) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("addPendingUiEvent: id=" + id + " code=" + code);
        }
        ContentValues values = new ContentValues(1);
        values.put(VMAMapping._PENDING_UI_EVENTS, code);
        values.put(VMAMapping._LUID, tempLuid);
        values.put(VMAMapping._OLD_LUID, oldLuid);        
        return SqliteWrapper.update(context, resolver, VMAMapping.CONTENT_URI, values, VMAMapping._ID + "="
                + id, null);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.MapperDao#updateUidMessageIdAndFlags(long, long, java.lang.String, int)
     */
    @Override
    public int updateUidMessageIdSrcAndFlags(long id, long uid, String msgId, int vmaflags, int srcCode) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("updateMessageIdAndFlags: id=" + id + " uid=" + uid + " vmaflags=" + vmaflags);
        }
        ContentValues values = new ContentValues(3);
        values.put(VMAMapping._UID, uid);
        values.put(VMAMapping._MSGID, msgId);
        values.put(VMAMapping._VMAFLAGS, vmaflags);
        values.put(VMAMapping._SOURCE, srcCode);
        return SqliteWrapper.update(context, resolver, VMAMapping.CONTENT_URI, values, VMAMapping._ID + "="
                + id, null);
    }

    @Override
    public int updateUidTimestampAndFlags(long id, long uid, int source, long timestamp, int vmaflags) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("updateMessageIdAndFlags: id=" + id + " uid=" + uid + " vmaflags=" + vmaflags);
        }
        ContentValues values = new ContentValues(3);
        values.put(VMAMapping._UID, uid);
        values.put(VMAMapping._SOURCE, source);
        values.put(VMAMapping._TIMEOFMESSAGE, timestamp);
        values.put(VMAMapping._VMAFLAGS, vmaflags);
        return SqliteWrapper.update(context, resolver, VMAMapping.CONTENT_URI, values, VMAMapping._ID + "="
                + id, null);
    }

    @Override
    public int deleteMapping(long id) {
        return SqliteWrapper.delete(context, resolver, VMAMapping.CONTENT_URI, VMAMapping._ID + "=" + id,
                null);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.provider.dao.MapperDao#findMappingByPduThreadId(long)
     */
    @Override
    public List<VMAMapping> findMappingByPduThreadId(long threadId) {
        String selection = VMAMapping._THREAD_ID;
        ;
        if (threadId > 0) {
            selection += "=" + threadId;
        } else {
            // Deleting all conversation.
            selection += ">" + threadId;
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByPduThreadId: selection=" + selection);
        }
        ArrayList<VMAMapping> mappings = new ArrayList<VMAMapping>();
        ;
        // default projection
        Cursor c = SqliteWrapper
                .query(context, resolver, VMAMapping.CONTENT_URI, null, selection, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                VMAMapping mapping = new VMAMapping();
                populateVMAMappingDefaultProjection(c, mapping);
                mappings.add(mapping);
            }
            c.close();
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByPduThreadId:  mapping=" + mappings);
        }
        return mappings;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see
     * com.verizon.messaging.vzmsgs.provider.dao.MapperDao#findMappingByMessageIdAndBoxType(java.lang.String,
     * int, int)
     */
    @Override
    public VMAMapping findMappingByMessageIdAndBoxType(String messageId, int msgtype, int boxType) {
        String selection = VMAMapping._MSGID + "='" + messageId + "' AND " + VMAMapping._TYPE + "=" + msgtype
                + " AND " + VMAMapping._MSG_BOX + "=" + boxType;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByMessageIdAndBoxType: selection=" + selection);
        }
        // default projection
        Cursor c = SqliteWrapper
                .query(context, resolver, VMAMapping.CONTENT_URI, null, selection, null, null);
        VMAMapping mapping = null;
        if (c != null) {
            while (c.moveToNext()) {
                mapping = new VMAMapping();
                populateVMAMappingDefaultProjection(c, mapping);
                break;
            }
            c.close();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("findMappingByMessageIdAndBoxType: mapping=" + mapping);
        }
        return mapping;
    }

}
