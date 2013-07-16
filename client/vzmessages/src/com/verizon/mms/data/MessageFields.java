/**
 * MessageFields.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.data;

/**
 * This class/interface   
 * @author Imthiaz
 * @Since  May 30, 2012
 */
public interface MessageFields {
    
    public abstract String getAddress();
   
    public abstract String getBody();
    
    public abstract long getDate();
    
    public abstract int getRead();
    
    public abstract int getType();
    
    public abstract int getStatus();
    
    public abstract int getLocked();
    
    public static final int MESSAGE_FIELDS_SMS_MESSAGE_TYPE_ALL = 0;
    public static final int MESSAGE_FIELDS_SMS_MESSAGE_TYPE_INBOX = 1;
    public static final int MESSAGE_FIELDS_SMS_MESSAGE_TYPE_SENT = 2;
    public static final int MESSAGE_FIELDS_SMS_MESSAGE_TYPE_DRAFT = 3;
    public static final int MESSAGE_FIELDS_SMS_MESSAGE_TYPE_OUTBOX = 4;
    public static final int MESSAGE_FIELDS_SMS_MESSAGE_TYPE_FAILED = 5;
    public static final int MESSAGE_FIELDS_SMS_MESSAGE_TYPE_QUEUED = 6;
    public static final int MESSAGE_FIELDS_SMS_MESSAGE_TYPE_RM_QUEUED = 135;
}
