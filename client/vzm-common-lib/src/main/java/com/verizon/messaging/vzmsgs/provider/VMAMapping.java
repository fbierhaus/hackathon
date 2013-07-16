/**
 * LocalMapping.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs.provider;

import java.util.HashMap;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * This class is used to map the local database and server columns.
 * 
 * @author Jegadeesan M
 * @Since Dec 5, 2012
 */
public class VMAMapping implements BaseColumns {
    public static final String TABLE_NAME = "vma_sync_mapping";
    public static final Uri CONTENT_URI = Uri.parse("content://" + ApplicationProvider.AUTHORITY + "/"
            + TABLE_NAME);

    public static final int TYPE_SMS = 1;
    public static final int TYPE_MMS = 2;
    
    public static final int SOURCECREATEDFROM_TELEPHONY = 1;
    public static final int SOURCECREATEDFROM_VMA = 2;
    public static final int SOURCECREATEDFROM_CKSUMMAPPER = 3;
    
    public static final int MSGBOX_SENT = 1;
    public static final int MSGBOX_RECEIVED= 2;
    
    public static final String _TIME_CREATED = "time_created";
    public static final String _TIME_UPDATED = "time_updated";
    public static final String _SOURCE_CREATED_FROM = "source_created_from";
    public static final String _TYPE = "type";
    public static final String _LUID = "luid";
    public static final String _THREAD_ID = "tid";
    public static final String _MSGID = "msgid";
    public static final String _UID = "uid";
    public static final String _TIMEOFMESSAGE = "timeofmessage";
    public static final String _SMSCHECKSUM = "smschecksum";
    public static final String _VMAFLAGS = "vmaflags";
    public static final String _PENDING_UI_EVENTS = "pending_ui_events";
    public static final String _SOURCE = "source";
    public static final String _MSG_BOX = "msgbox";
    public static final String _OLD_LUID = "oldLuid";

    public static final HashMap<String, String> defaultProjection;
    private static final int SMS = 1;
    private static final int MMS = 2;
    static {
        defaultProjection = new HashMap<String, String>();
        defaultProjection.put(_ID, _ID);
        defaultProjection.put(_TIME_CREATED, _TIME_CREATED);
        defaultProjection.put(_TIME_UPDATED, _TIME_UPDATED);
        defaultProjection.put(_SOURCE_CREATED_FROM, _SOURCE_CREATED_FROM);
        defaultProjection.put(_TYPE, _TYPE);
        defaultProjection.put(_LUID, _LUID);
        defaultProjection.put(_THREAD_ID, _THREAD_ID);
        defaultProjection.put(_MSGID, _MSGID);
        defaultProjection.put(_UID, _UID);
        defaultProjection.put(_TIMEOFMESSAGE, _TIMEOFMESSAGE);
        defaultProjection.put(_SMSCHECKSUM, _SMSCHECKSUM);
        defaultProjection.put(_VMAFLAGS, _VMAFLAGS);
        defaultProjection.put(_PENDING_UI_EVENTS, _PENDING_UI_EVENTS);
        defaultProjection.put(_SOURCE, _SOURCE);
        defaultProjection.put(_MSG_BOX, _MSG_BOX);
        defaultProjection.put(_OLD_LUID, _OLD_LUID);
    }

    private long id;
    private long timeCreated;
    private long timeUpdated;
    private int sourceCreatedFrom;
    private int type;
    private long luid;
    private long threadId;
    private String msgid;
    private long uid;
    private long timeofmessage;
    private long smschecksum;
    private int vmaflags;
    private int pendingUievents;
    private int source;
    private int messageBox;
    private long oldLuid;

    /**
     * Returns the Value of the id
     * 
     * @return the {@link long}
     */
    public long getId() {
        return id;
    }

    /**
     * Set the Value of the field id
     * 
     * @param id
     *            the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns the Value of the timeCreated
     * 
     * @return the {@link long}
     */
    public long getTimeCreated() {
        return timeCreated;
    }

    /**
     * Set the Value of the field timeCreated
     * 
     * @param timeCreated
     *            the timeCreated to set
     */
    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    /**
     * Returns the Value of the timeUpdated
     * 
     * @return the {@link long}
     */
    public long getTimeUpdated() {
        return timeUpdated;
    }

    /**
     * Set the Value of the field timeUpdated
     * 
     * @param timeUpdated
     *            the timeUpdated to set
     */
    public void setTimeUpdated(long timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    /**
     * Returns the Value of the sourceCreatedFrom
     * 
     * @return the {@link int}
     */
    public int getSourceCreatedFrom() {
        return sourceCreatedFrom;
    }

    /**
     * Set the Value of the field sourceCreatedFrom
     * 
     * @param sourceCreatedFrom
     *            the sourceCreatedFrom to set
     */
    public void setSourceCreatedFrom(int sourceCreatedFrom) {
        this.sourceCreatedFrom = sourceCreatedFrom;
    }

    /**
     * Returns the Value of the type
     * 
     * @return the {@link int}
     */
    public int getType() {
        return type;
    }

    /**
     * Set the Value of the field type
     * 
     * @param type
     *            the type to set
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Returns the Value of the luid
     * 
     * @return the {@link long}
     */
    public long getLuid() {
        return luid;
    }

    /**
     * Set the Value of the field luid
     * 
     * @param luid
     *            the luid to set
     */
    public void setLuid(long luid) {
        this.luid = luid;
    }

    /**
     * Returns the Value of the threadId
     * 
     * @return the {@link long}
     */
    public long getThreadId() {
        return threadId;
    }

    /**
     * Set the Value of the field threadId
     * 
     * @param threadId
     *            the threadId to set
     */
    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    /**
     * Returns the Value of the msgid
     * 
     * @return the {@link String}
     */
    public String getMsgid() {
        return msgid;
    }

    /**
     * Set the Value of the field msgid
     * 
     * @param msgid
     *            the msgid to set
     */
    public void setMsgid(String msgid) {
        this.msgid = msgid;
    }

    /**
     * Returns the Value of the uid
     * 
     * @return the {@link int}
     */
    public long getUid() {
        return uid;
    }

    /**
     * Set the Value of the field uid
     * 
     * @param uid
     *            the uid to set
     */
    public void setUid(long uid) {
        this.uid = uid;
    }

    /**
     * Returns the Value of the timeofmessage
     * 
     * @return the {@link long}
     */
    public long getTimeofmessage() {
        return timeofmessage;
    }

    /**
     * Set the Value of the field timeofmessage
     * 
     * @param timeofmessage
     *            the timeofmessage to set
     */
    public void setTimeofmessage(long timeofmessage) {
        this.timeofmessage = timeofmessage;
    }

    /**
     * Returns the Value of the smschecksum
     * 
     * @return the {@link long}
     */
    public long getSmschecksum() {
        return smschecksum;
    }

    /**
     * Set the Value of the field smschecksum
     * 
     * @param smschecksum
     *            the smschecksum to set
     */
    public void setSmschecksum(long smschecksum) {
        this.smschecksum = smschecksum;
    }

    /**
     * Returns the Value of the vmaflags
     * 
     * @return the {@link int}
     */
    public int getVmaflags() {
        return vmaflags;
    }

    /**
     * Set the Value of the field vmaflags
     * 
     * @param vmaflags
     *            the vmaflags to set
     */
    public void setVmaflags(int vmaflags) {
        this.vmaflags = vmaflags;
    }

    /**
     * Returns the Value of the pendingUievents
     * 
     * @return the {@link int}
     */
    public int getPendingUievents() {
        return pendingUievents;
    }

    /**
     * Set the Value of the field pendingUievents
     * 
     * @param pendingUievents
     *            the pendingUievents to set
     */
    public void setPendingUievents(int pendingUievents) {
        this.pendingUievents = pendingUievents;
    }

    /**
     * Returns the Value of the source
     * 
     * @return the {@link int}
     */
    public int getSource() {
        return source;
    }

    /**
     * Set the Value of the field source
     * 
     * @param source
     *            the source to set
     */
    public void setSource(int source) {
        this.source = source;
    }

    /**
     * Returns the Value of the oldLuid
     * @return the  {@link long}
     */
    public long getOldLuid() {
        return oldLuid;
    }

    /**
     * Set the Value of the field oldLuid
     *
     * @param oldLuid the oldLuid to set
     */
    public void setOldLuid(long oldLuid) {
        this.oldLuid = oldLuid;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("id=" + id);
        builder.append(",timeCreated=" + timeCreated);
        builder.append(",timeUpdated=" + timeUpdated);
        builder.append(",sourceCreatedFrom=" + sourceCreatedFrom);
        builder.append(",type=" + type);
        builder.append(",luid=" + luid);
        builder.append(",threadId=" + threadId);
        builder.append(",msgid=" + msgid);
        builder.append(",uid=" + uid);
        builder.append(",timeofmessage=" + timeofmessage);
        builder.append(",smschecksum=" + smschecksum);
        builder.append(",vmaflags=" + vmaflags);
        builder.append(",pendingUievents=" + pendingUievents);
        builder.append(",source=" + source);
        builder.append(",box=" + messageBox);
        builder.append(",oldLuid=" + oldLuid);
        return builder.toString();
    }

    /**
     * This Method 
     * @return
     */
    public boolean isSMS() {
        return type== SMS;
    }

    /**
     * Returns the Value of the messageBox
     * @return the  {@link int}
     */
    public int getMessageBox() {
        return messageBox;
    }

    /**
     * Set the Value of the field messageBox
     *
     * @param messageBox the messageBox to set
     */
    public void setMessageBox(int messageBox) {
        this.messageBox = messageBox;
    }

    /**
     * This Method 
     * @return
     */
    public boolean isMMS() {
        return type == MMS;
    }

    /**
     * This Method 
     * @return
     */
    public boolean isSentMessage() {
        return messageBox ==  MSGBOX_SENT;
    }
    
    /**
     * This Method 
     * @return
     */
    public boolean isReceivedMessage() {
        return messageBox ==  MSGBOX_RECEIVED;
    }
}
