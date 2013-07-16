/**
 * PDUDao.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.refactor;

import java.util.List;
import java.util.Map;

import android.net.Uri;

import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.mms.MmsException;
import com.vzw.vma.common.message.VMAAttachment;
import com.vzw.vma.common.message.VMAMessage;
import com.vzw.vma.message.VMAMessageResponse;

/**
 * This interface is used to interact with native SMS and MMS database
 * 
 * @author Jegadeesan M
 * @Since Feb 12, 2013
 */
public interface PDUDao {

    /**
     * This Method
     * 
     * @param msg
     * @return
     */
    public long createNewSms(VMAMessageResponse msg);

    /**
     * This Method
     * 
     * @param msg
     * @return
     * @throws MmsException
     */
    public long createNewMms(VMAMessageResponse msg, String mdn) throws MmsException;

    /**
     * This Method
     * 
     * @param attachment
     * @param luid
     * @throws MmsException
     */
    public void persistPart(VMAAttachment attachment, long luid) throws MmsException;

    /**
     * This Method
     * 
     * @param luid
     * @return
     */
    public VMAMessage getSMS(long luid);

    /**
     * This Method
     * 
     * @param luid
     * @return
     */
    public VMAMessage getMms(long luid);
    
    public VMAMessage getMms(String msgId);
    
    public int markSMSAsRead(long luid);
    
    public int markMMSAsRead(long luid);
    
    public boolean isSMSRead(long luid);
    
    public boolean isMMSRead(long luid);
    
    public boolean isSMSDelivered(long luid);
    
    public boolean isMMSDelivered(long luid);
    
    public int markSMSAsDelivered(long luid);
    
    public int deleteSMS(long luid);
        
    public int deleteMMS(long luid);
    
    public int moveSMStoSent(long luid , int status);
    
    public int moveSMStoSendFailed(long luid, int status);
    
    public boolean moveSMStoOutbox(long luid);
    

    public int moveMMStoSent(long luid ,String messageId, int responseStatus);
    
    public boolean moveMMStoOutbox(long luid);
    
    public int moveMMStoSendFailed(long luid , int responseStatus);
    
    public void applyDeliveryReports( Map<String, String> reports, String messageId, long threadId, long luid, boolean isSMS ,long deliveredDate);
    
    public boolean mmsHasAttachement(Uri uri);
    
    public boolean mmsHasAttachement(long luid);
    
    public VMAMessage getSMSForMapping(long luid);
    
    public VMAMessage getMMSForMapping(long luid);
    
    public void persistPart(List<VMAAttachment> attachments, long luid, String mdn) throws MmsException;

    /**
     * This Method 
     * @param tempPartUri
     */
    public int deletePart(Uri tempPartUri);


    /**
     * This Method 
     * @param luid
     * @param vmaTimeStamp
     */
    public void updateSMSReceivedTimeOnHandset(long luid, long vmaTimeStamp);

    /**
     * This Method 
     * @param luid
     * @param vmaTimeStamp
     */
    public void updateMMSReceivedTimeOnHandset(long luid, long vmaTimeStamp);

    /**
     * This Method is used to get the message time 
     * @param mapping
     * @return
     */
    public long getMessageTime(VMAMapping mapping);
    

    

}
