/**
 * SendAction.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.action;

import java.io.IOException;

import javax.mail.MessagingException;

import android.app.Activity;
import android.telephony.SmsManager;

import com.strumsoft.android.commons.logger.Logger;
import com.sun.mail.iap.ProtocolException;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.messaging.vzmsgs.provider.dao.MapperDao;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.mms.pdu.PduHeaders;
import com.vzw.vma.common.message.MessageSourceEnum;
import com.vzw.vma.common.message.MessageTypeEnum;
import com.vzw.vma.common.message.VMAMessage;
import com.vzw.vma.message.VMAStore;
import com.vzw.vma.sync.refactor.PDUDao;
import com.vzw.vma.sync.refactor.impl.MapperDaoImpl;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Feb 22, 2013
 */
public class SendAction {

    private VMAStore store;
    private VMAEventHandler eventHandler;
    private PDUDao pduDao;
    protected MapperDao mapper;
    private static SendAction instance;

    public synchronized static SendAction getInstance(VMAStore store, VMAEventHandler eventHandler,
            PDUDao pduDao) {
        if (instance == null) {
            instance = new SendAction(store, eventHandler, pduDao);
        } else {
            instance.updateOldReference(store, eventHandler, pduDao);
        }
        return instance;
    }

    /**
     * This Method
     * 
     * @param store
     * @param eventHandler
     * @param pduDao
     */
    private void updateOldReference(VMAStore store, VMAEventHandler eventHandler, PDUDao pduDao) {
        this.store = store;
        this.eventHandler = eventHandler;
        this.pduDao = pduDao;
        // XXX: TEMP HACK
        this.mapper = new MapperDaoImpl(ApplicationSettings.getInstance().getContext());
    }

    /**
     * 
     * Constructor
     */
    private SendAction(VMAStore store, VMAEventHandler eventHandler, PDUDao pduDao) {
        this.store = store;
        this.eventHandler = eventHandler;
        this.pduDao = pduDao;
        // XXX: TEMP HACK
        this.mapper = new MapperDaoImpl(ApplicationSettings.getInstance().getContext());
    }

    /**
     * This Method is used to send the SMS
     * 
     * @param item
     * @return
     * @throws ProtocolException
     * @throws IOException
     * @throws MessagingException
     */
    public boolean sendSMS(SyncItem item) throws ProtocolException, IOException, MessagingException {
        if (Logger.IS_INFO_ENABLED) {
            Logger.info("Sending SMS luid=" + item.itemId);
        }
        VMAMessage msg = pduDao.getSMS(item.itemId);
        if (msg != null) {

            long luid = item.itemId;

            // XXX: TEMP HACK
            throwExceptionIfMappingExists(luid, VMAMapping.TYPE_SMS);

            // Temporary unique message id to avoid duplicates on send. the will used to detect
            msg.setMessageId(getTempSendMessageId(item, msg));
            // Message Type
            msg.setMessageType(MessageTypeEnum.SMS);
            // Source is IMAP
            msg.setMessageSource(MessageSourceEnum.IMAP);

            pduDao.moveSMStoOutbox(item.itemId);
            // String toAddress = ApplicationSettings.parseAdddressForChecksum(msg.getToAddrs().get(0));
            // if (Logger.IS_DEBUG_ENABLED) {
            // Logger.info("Sending SMS address=" + toAddress + ",original=" + msg.getToAddrs().get(0));
            // }

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("Sending SMS address=" + msg.getToAddrs()+",msg="+ msg.getMessageText());
            }
            String messageId = store.sendSMS(msg);
            // String messageId = store.sendSMS(msg.getMessageText(), toAddress);
            if (messageId != null) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.info("SMS sent messageId=" + messageId);
                }
                long threadId = msg.getLocalThreadId();
                String body = msg.getMessageText();
                String mdn = msg.getToAddrs().get(0) + "";
                long timestamp = System.currentTimeMillis(); // XXX: we dont have timestamp
                // msg.getDate().getTime();
                eventHandler.vmaSendSMS(luid, threadId, body, mdn, timestamp, messageId);
                pduDao.moveSMStoSent(luid, Activity.RESULT_OK);
                return true;
            }
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("SMS send failed.returing false.");
            }
            pduDao.moveSMStoSendFailed(item.itemId, SmsManager.RESULT_ERROR_NO_SERVICE);
            return false;
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.info("SMS not found on database. returing true");
        }
        return true;
    }

    // XXX: TEMP HACK to prevent duplicate messages
    protected void throwExceptionIfMappingExists(long luid, int type) {
        VMAMapping mapping = mapper.findMappingByPduLuid(luid, type);
        if (mapping != null) {
            Logger.error("Error: Mapping exists for luid " + luid + " of type " + type);
            throw new RuntimeException("Error: Mapping exists for luid " + luid + " of type " + type);
        }
    }

    /**
     * This Method is used to send MMS
     * 
     * @param item
     * @return
     * @throws ProtocolException
     * @throws IOException
     * @throws MessagingException
     */
    public boolean sendMMS(SyncItem item) throws ProtocolException, IOException, MessagingException {
        if (Logger.IS_INFO_ENABLED) {
            Logger.info("Sending MMS luid=" + item.itemId);
        }
        VMAMessage message = pduDao.getMms(item.itemId);
        if (message != null) {
            long luid = item.itemId;

            // XXX: TEMP HACK
            throwExceptionIfMappingExists(luid, VMAMapping.TYPE_MMS);

            // Temporary unique message id to avoid duplicates on send. the will used to detect
            message.setMessageId(getTempSendMessageId(item, message));
            // Message Type
            message.setMessageType(MessageTypeEnum.MMS);
            // Source is IMAP
            message.setMessageSource(MessageSourceEnum.IMAP);

            pduDao.moveMMStoOutbox(item.itemId);
            // boolean needAttachmentLock = message.getAttachments().size() > 0;
            String messageId = null;

            messageId = store.sendMMS(message);
            // }
            if (messageId != null) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.info("MMS sent. messageId=" + messageId);
                }

                long threadId = message.getLocalThreadId();
                long timestamp = message.getMessageTime().getTime();
                eventHandler.vmaSendMMS(luid, threadId, messageId, timestamp);
                pduDao.moveMMStoSent(luid, messageId, PduHeaders.RESPONSE_STATUS_OK);
                return true;
            }
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("MMS send failed.returing false.messageId=" + messageId);
            }
            pduDao.moveMMStoSendFailed(item.itemId, PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_FAILURE);
            return false;
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.info("MMS not found.returing true.");
        }
        return true;
    }

    /**
     * This Method
     * 
     * @param item
     * @param message
     * @return
     */
    private String getTempSendMessageId(SyncItem item, VMAMessage message) {
        return item.itemId + "-" + item.type + "-" + message.getMessageTime().getTime();
    }

    /**
     * This Method
     * 
     * @return
     */
    public synchronized static SendAction getInstance() {
        return instance;
    }

}
