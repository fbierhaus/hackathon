/**
 * PDUDaoImpl.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.refactor.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Addr;
import android.provider.Telephony.Mms.Part;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.mms.ContentType;
import com.verizon.mms.MmsException;
import com.verizon.mms.pdu.DeliveryInd;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduContentTypes;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPart;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.VMAPduParser;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.VZTelephony;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.UiNotification;
import com.vzw.vma.common.message.MSAMessage;
import com.vzw.vma.common.message.MessageSourceEnum;
import com.vzw.vma.common.message.MessageTypeEnum;
import com.vzw.vma.common.message.StringUtils;
import com.vzw.vma.common.message.VMAAttachment;
import com.vzw.vma.common.message.VMAMessage;
import com.vzw.vma.message.VMAMessageResponse;
import com.vzw.vma.sync.refactor.PDUDao;
import com.vzw.vma.sync.util.MMSUtil;
import com.vzw.vma.sync.util.MMSUtil.MMSMessage;

/**
 * This class/interface
 * 
 * @author Jegadeesan M
 * @Since Feb 12, 2013
 */
public class PDUDaoImpl implements PDUDao {

    private ContentResolver resolver;
    private Context context;
    private PduPersister persister;
    private static final String[] SEND_PROJECTION = new String[] { Sms._ID, // 0
            Sms.THREAD_ID, // 1
            Sms.ADDRESS, // 2
            Sms.BODY, // 3
            Sms.STATUS, // 4
            Sms.DATE, // 5

    };

    private static final String[] SMS_CHECKSUM_PROJECTION = new String[] { Sms.THREAD_ID, Sms.DATE,
            Sms.ADDRESS, Sms.BODY, Sms.TYPE };
    private static final String[] MMS_VMAMAPPING_PROJECTION = new String[] { Mms.THREAD_ID, Mms.DATE,
            Mms.MESSAGE_ID, Mms.MESSAGE_BOX };

    // This must match SEND_PROJECTION.
    private static final int SEND_COLUMN_ID = 0;
    private static final int SEND_COLUMN_THREAD_ID = 1;
    private static final int SEND_COLUMN_ADDRESS = 2;
    private static final int SEND_COLUMN_BODY = 3;
    // private static final int SEND_COLUMN_STATUS = 4;
    private static final int SEND_COLUMN_DATE = 5;
    private UiNotification uiNotifier;

    /**
     * 
     * Constructor
     */
    public PDUDaoImpl(Context context, UiNotification uiNotifier) {
        this.context = context;
        this.uiNotifier = uiNotifier;
        this.resolver = context.getContentResolver();
        persister = PduPersister.getPduPersister(context);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#createNewSms(com.vzw.vma.message.VMAMessageResponse)
     */
    @Override
    public long createNewSms(VMAMessageResponse msg) {

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("VMAMesgDAO.addSMS called");
        }
        // SMS Table Schema
        // CREATE TABLE sms (_id INTEGER PRIMARY KEY,
        // thread_id INTEGER,
        // address TEXT,
        // person INTEGER,
        // date INTEGER,
        // protocol INTEGER,
        // read INTEGER DEFAULT 0,
        // status INTEGER DEFAULT -1,
        // type INTEGER,
        // reply_path_present INTEGER,
        // subject TEXT,
        // body TEXT,service_center TEXT,
        // locked INTEGER DEFAULT 0,
        // error_code INTEGER DEFAULT 0,
        // seen INTEGER DEFAULT 0);

        VMAMessage message = msg.getVmaMessage();

        ContentValues values = new ContentValues();

        Map<String, String> reports = msg.getDeliveryReports();
        boolean failedSms = false;
        for (String key : reports.keySet()) {
            String value = reports.get(key);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("Delivery :recipient=" + key + ",status=" + value);
            }
            if ("DELIVERED".equalsIgnoreCase(value)) {
                values.put(Sms.STATUS, Sms.STATUS_COMPLETE);
            } else if ("FAILED".equalsIgnoreCase(value)) {
                values.put(Sms.STATUS, 64);
                values.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
                values.put(Sms.ERROR_CODE, SmsManager.RESULT_ERROR_NO_SERVICE);
                failedSms = true;
            } else if ("READ".equalsIgnoreCase(value)) {
                values.put(Sms.READ, 1);
            }
        }

        if (!failedSms) {
            // error_code INTEGER DEFAULT 0,
            values.put(Sms.ERROR_CODE, 0);
        }

        HashSet<String> recipients = new HashSet<String>();
        if (message.isFlagSent()) {
            // Sent from tablet/mobile/webportal
            ArrayList<String> tos = message.getToAddrs();
            String to = TextUtils.join(";", tos);
            recipients.add(to);
        } else {
            // Received from third party
            ArrayList<String> addresses = message.getFromList();
            for (String address : addresses) {
                recipients.add(address);
            }
        }
        long threadId = VZTelephony.getOrCreateThreadId(context, recipients);
        message.setLocalThreadId(threadId);

        // date INTEGER,
        values.put(Sms.DATE, message.getMessageTime().getTime());
        // protocol INTEGER,

        // read INTEGER DEFAULT 0,
        // seen INTEGER DEFAULT 0);

        values.put(Sms.SEEN, message.isFlagSeen() ? 1 : 0);
        values.put(Sms.READ, message.isFlagSeen() ? 1 : 0);
        // status INTEGER DEFAULT -1,

        // type INTEGER,

        // reply_path_present INTEGER,

        // Logger.error("DATE:---" + message.getDate());
        // Logger.error("DATE:---" + message.getDate().getTime());

        // subject TEXT,
        values.put(Sms.DATE, message.getMessageTime().getTime());
        // body TEXT,
        values.put(Sms.BODY, message.getMessageText());

        // service_center TEXT,

        // locked INTEGER DEFAULT 0,

        Uri res = null;
        for (String address : recipients) {
            values.put(Sms.ADDRESS, address);
            break;
        }
        Uri uri = null;

        if (failedSms) {
            Logger.debug("FailedSMS:getSmsUri" + failedSms);
            uri = VZUris.getSmsUri();
        } else if (message.isFlagSent()) {
            Logger.debug("FailedSMS:isFalgSent:getSmsSentUri");
            uri = VZUris.getSmsSentUri();
        } else {
            Logger.debug("FailedSMS:getSmsInboxUri");
            uri = VZUris.getSmsInboxUri();
        }
        /*
         * if (message.isFlagSent()) { uri = VZUris.getSmsSentUri(); } else { uri = VZUris.getSmsInboxUri(); }
         */
        res = SqliteWrapper.insert(context, resolver, uri, values);
        long luid = ContentUris.parseId(res);
        // Mandatory
        msg.getVmaMessage().setLuid(luid);
        msg.getVmaMessage().setLocalThreadId(threadId);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("VMAMesgDAO.addSMS returning " + res);
        }

        ConversationDataObserver.onNewMessageAdded(threadId, luid, ConversationDataObserver.MSG_TYPE_SMS,
                ConversationDataObserver.MSG_SRC_TELEPHONY);

        return ContentUris.parseId(res);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#createNewMms(com.vzw.vma.message.VMAMessageResponse)
     */
    @Override
    public long createNewMms(VMAMessageResponse msg, String mdn) throws MmsException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createNewMms called");
        }
        VMAPduParser parser = new VMAPduParser(context, msg.getVmaMessage(), mdn);
        Uri uri = parser.savePdu();
        long id = ContentUris.parseId(uri);
        // Mandatory
        msg.getVmaMessage().setLuid(id);
        msg.getVmaMessage().setLocalThreadId(parser.getThreadId());
        // Check for delivery report
        Map<String, String> reports = msg.getDeliveryReports();
        for (String reciption : reports.keySet()) {
            String status = reports.get(reciption);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("MMS Delivery :recipient=" + reciption + ",status=" + status);
            }
            if ("DELIVERED".equalsIgnoreCase(status)) {
                long date = (msg.getVmaMessage().getMessageTime().getTime() / 1000L);
                persisitDeliveryInd(msg.getVmaMessage().getMessageId(), parser.getThreadId(), reciption, date);
            } else if ("FAILED".equalsIgnoreCase(status)) {
            } else if ("READ".equalsIgnoreCase(status)) {
            }

        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createNewMms returning " + id);
        }

        return id;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#persistPart(com.vzw.vma.common.message.VMAAttachment, long)
     */
    @Override
    public void persistPart(VMAAttachment attachment, long luid) throws MmsException {

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("persistPart : for " + luid);
        }
        PduPart part = new PduPart();
        String contentType = attachment.getMimeType();
        if (contentType.contains("\n")) {
            // Logger.error("Mime time : has new line ");
            String[] contentTypes = contentType.split("\n", 2);
            contentType = contentTypes[0];
        }
        if (contentType.contains(";")) {
            // Logger.error("Mime time : has semicolon ");
            String[] contentTypes = contentType.split(";", 2);
            contentType = contentTypes[0];
        }
        if (contentType != null) {
            part.setContentType(contentType.getBytes());
        } else {
            part.setContentType((PduContentTypes.contentTypes[0]).getBytes());
        }

        String name = attachment.getAttachmentName();
        if (null != name) {
            part.setName(name.getBytes());
        }

        /*
         * get charset parameter String charSet = attachment.getCh; int charset = getCharset(charSet); if (-1
         * != charset) { part.setCharset(charset); }
         */

        part.setContentId(attachment.getAttachmentId().getBytes());
        part.setContentDisposition(attachment.getXSectionId().getBytes()); // XXX : or FROM_DATA ?
        part.setFilename(attachment.getAttachmentName().getBytes());
        part.setContentTransferEncoding("binary".getBytes());

        /*
         * FIXME: check content-id, name, filename and content location, if not set anyone of them, generate a
         * default content-location
         */
        if ((null == part.getContentLocation()) && (null == part.getName()) && (null == part.getFilename())
                && (null == part.getContentId())) {
            part.setContentLocation(Long.toOctalString(System.currentTimeMillis()).getBytes());
        }
        // OOM Issue Fix
        // byte[] partData = attachment.getData();
        // part.setData(partData);
        part.setTempPartUri(attachment.getTempPartUri());

        // int count = deleteThumbnailPart(luid, contentType);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("deleteThumbnailPart : for " + luid + " type=" + contentType);
        }
        String where = Part.CONTENT_DISPOSITION + "='" + MSAMessage.THUMBNAIL_SECTION + "'";
        int count = SqliteWrapper.delete(context, resolver, VZUris.getMmsPartsUri(luid), where, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.info("Thumbnail part deleted.count=" + count);
        }
        PduPersister.getPduPersister(context).persistPart(part, luid);
        uiNotifier.purge(ContentUris.withAppendedId(VZUris.getMmsUri(), luid), true);

        ConversationDataObserver.onMessageStatusChanged(findMmsThreadId(context, luid), luid,
                ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#getSMS(long)
     */
    @Override
    public VMAMessage getSMS(long luid) {
        VMAMessage message = null;
        try {
            Cursor c = SqliteWrapper.query(context, resolver,
                    ContentUris.withAppendedId(VZUris.getSmsUri(), luid), SEND_PROJECTION, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    // String msgText = c.getString(SEND_COLUMN_BODY);
                    // String address = c.getString(SEND_COLUMN_ADDRESS);
                    // int threadId = c.getInt(SEND_COLUMN_THREAD_ID);
                    // int msgId = c.getInt(SEND_COLUMN_ID);
                    message = new VMAMessage();
                    message.setMessageText(c.getString(SEND_COLUMN_BODY));
                    ArrayList<String> to = new ArrayList<String>();
                    to.add(ApplicationSettings.parseAdddressForChecksum(c.getString(SEND_COLUMN_ADDRESS)));
                    message.setToAddrs(to);
                    message.setLocalThreadId(c.getLong(SEND_COLUMN_THREAD_ID));
                    message.setMessageTime(new Date(c.getLong(SEND_COLUMN_DATE)));
                    message.setLuid(luid);
                }
                c.close();
            }
            return message;
        } catch (SQLException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error("unable to get SMS :got error - " + e.getMessage());
            }
        }
        return null;

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#getMms(long)
     */
    @Override
    public VMAMessage getMms(long luid) {

        VMAMessage msg = new VMAMessage();
        // populate PDU HEADERS
        populatePDU(luid, msg);
        // msg.setMessageTime(new Date());

        // msg.setMdn(mdn);
        // msg.setSourceAddr(mdn);

        // ArrayList<String> toList = getMmsRecepients(luid);
        // msg.setToAddrs(toList);
        populateAdr(luid, msg);

        // msg.setMessageId(StringUtils.getRandomString(15));
        // msg.setMessageType(MessageTypeEnum.MMS);
        // msg.setMessageSource(MessageSourceEnum.IMAP);

        // Message Body
        msg.setMessageText(getMmsMessage(luid));

        // ClassLoader loader = MSAMessage.class.getClassLoader();
        // MimetypesFileTypeMap mimeMap = new MimetypesFileTypeMap(loader.getResourceAsStream("mime.type"));

        populateAttachements(luid, msg);

        return msg;

    }

    private ArrayList<String> populatePDU(long luid, VMAMessage message) {
        ArrayList<String> recipients = new ArrayList<String>();
        Uri uri = ContentUris.withAppendedId(VZUris.getMmsUri(), luid);
        String[] projection = new String[] { Mms.THREAD_ID, Mms.SUBJECT, Mms.MESSAGE_BOX, Mms.DATE };
        Cursor c = SqliteWrapper.query(context, resolver, uri, projection, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                message.setLuid(luid);
                // LOCAL THREAD ID
                message.setLocalThreadId(c.getLong(0));
                // SUBJECT
                message.setMessageSubject(c.getString(1));
                // MESSAGE BOX
                boolean isSent = (c.getInt(2) == Mms.MESSAGE_BOX_SENT) ? true : false;
                message.setFlagSent(isSent);
                // TIME
                message.setMessageTime(new Date(c.getLong(3)));

            }
            c.close();
        }
        return recipients;
    }

    /**
     * This Method
     * 
     * @param luid
     * @return
     */
    private synchronized String getMmsMessage(long luid) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getMmsMessage : for " + luid);
        }
        String message = null;
        Uri uri = Uri.parse("content://" + VZUris.getMmsAuthority() + "/" + luid + "/part");
        String where = Part.CONTENT_TYPE + "= '" + ContentType.TEXT_PLAIN + "'";
        Cursor c = SqliteWrapper.query(context, resolver, uri, new String[] { Part.TEXT }, where, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                message = c.getString(0);
            }
            c.close();

        }
        return message;
    }

    private ArrayList<String> getMmsRecepients(long luid) {
        ArrayList<String> recipients = new ArrayList<String>();
        Uri uri = Uri.parse("content://" + VZUris.getMmsAuthority() + "/" + luid + "/addr");
        String where = Addr.TYPE + "!=" + PduHeaders.FROM;
        Cursor c = SqliteWrapper.query(context, resolver, uri, null, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                recipients.add(c.getString(c.getColumnIndex(Addr.ADDRESS)));
            }
            c.close();
        }
        return recipients;
    }

    private String getMmsSender(long luid) {
        String sender = null;
        Uri uri = Uri.parse("content://" + VZUris.getMmsAuthority() + "/" + luid + "/addr");
        String where = Addr.TYPE + "=" + PduHeaders.FROM;
        Cursor c = SqliteWrapper.query(context, resolver, uri, null, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                sender = c.getString(c.getColumnIndex(Addr.ADDRESS));
            }
            c.close();
        }
        return sender;
    }

    /**
     * This Method
     * 
     * @param luid
     * @return
     */
    private void populateAttachements(long luid, VMAMessage message) {
        ArrayList<VMAAttachment> attachments = new ArrayList<VMAAttachment>();
        Uri uri = Uri.parse("content://" + VZUris.getMmsAuthority() + "/" + luid + "/part");
        // String where = Part.CONTENT_TYPE + " != '" + ContentType.TEXT_PLAIN + "' AND " + Part.CONTENT_TYPE
        // + " != '" + ContentType.APP_SMIL + "'";
        String where = Part.CONTENT_TYPE + " != '" + ContentType.APP_SMIL + "'";

        Cursor c = SqliteWrapper.query(context, resolver, uri, new String[] { Part._ID,
                Part.CONTENT_LOCATION, Part.CONTENT_TYPE, Part.TEXT, Part.FILENAME }, where, null, null);
        if (c != null) {
            String contentType = null;
            while (c.moveToNext()) {
                contentType = c.getString(2);
                if (ContentType.TEXT_PLAIN.equalsIgnoreCase(contentType)) {
                    message.setMessageText(c.getString(3));
                    continue;
                }
                VMAAttachment attachment = new VMAAttachment();
                String partId = c.getString(0);
                String contentlocation = c.getString(1);
                String fn = c.getString(4);
                String fileName = contentlocation;
                fileName = (!TextUtils.isEmpty(contentlocation) ? contentlocation : fn);
                if (TextUtils.isEmpty(fileName)) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.error("File name ............" + fileName);
                    }
                    // fileName="text.txt";
                    // XXX: prevent NPE
                    fileName = "NoFname" + System.currentTimeMillis();
                }
                if (fileName.startsWith(".")) {
                    fileName = fileName.substring(1);
                }
                String attachID;
                if (fileName.indexOf(".") >= 0) {
                    attachID = fileName.substring(0, fileName.indexOf("."));
                } else {
                    attachID = fileName;
                }
                attachment.setAttachmentName(attachID);
                attachment.setMimeType(contentType);
                // attachment.setDataURI(c.getString(3));
                attachment.setDataURI("content://" + VZUris.getMmsAuthority() + "/part/" + partId);
                attachments.add(attachment);
            }
            c.close();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Attachments count=" + attachments.size());
            }
            if (!attachments.isEmpty()) {
                message.setAttachments(attachments);
            }
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#markSMSAsRead(long)
     */
    @Override
    public int markSMSAsRead(long luid) {
        ContentValues values = new ContentValues(2);
        values.put(Sms.SEEN, 1);
        values.put(Sms.READ, 1);
        String where = Sms._ID + "=" + luid + " AND " + Sms.READ + "= 0";
        int count = SqliteWrapper.update(context, resolver, VZUris.getSmsUri(), values, where, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("markSMSAsRead luid=" + luid + ",count=" + count);
        }
        if (count > 0) {
            uiNotifier.clearReadMessageNotification(ContentUris.withAppendedId(VZUris.getSmsUri(), luid));
        }
        return count;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#markMMSAsRead(long)
     */
    @Override
    public int markMMSAsRead(long luid) {
        ContentValues values = new ContentValues(2);
        values.put(Mms.SEEN, 1);
        values.put(Mms.READ, 1);
        String where = Mms._ID + "=" + luid + " AND " + Mms.READ + "= 0";
        int count = SqliteWrapper.update(context, resolver, VZUris.getMmsUri(), values, where, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("markMMSAsRead luid=" + luid + ",count=" + count);
        }
        if (count > 0) {
            uiNotifier.clearReadMessageNotification(ContentUris.withAppendedId(VZUris.getMmsUri(), luid));
        }
        return count;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#isSMSRead(long)
     */
    @Override
    public boolean isSMSRead(long luid) {
        String selection = Sms._ID + "=" + luid + " AND " + Sms.READ + "=1";
        // default projection
        Cursor c = SqliteWrapper.query(context, resolver, VZUris.getSmsUri(), null, selection, null, null);
        boolean result = false;
        if (c != null) {
            result = c.getCount() > 0;
            c.close();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("isSMSRead: luid=" + luid + ",result=" + result);
        }
        return result;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#isMMSRead(long)
     */
    @Override
    public boolean isMMSRead(long luid) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("isMMSRead: luid=" + luid);
        }
        String selection = Mms._ID + "=" + luid + " AND " + Mms.READ + "> 0";
        // default projection
        Cursor c = SqliteWrapper.query(context, resolver, VZUris.getMmsUri(), null, selection, null, null);
        boolean result = false;
        if (c != null) {
            result = c.getCount() > 0;
            c.close();
        }
        return result;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#isSMSDelivered(long)
     */
    @Override
    public boolean isSMSDelivered(long luid) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("isSMSDelivered: luid=" + luid);
        }
        String selection = Sms._ID + "=" + luid + " AND " + Sms.STATUS + "= " + Sms.STATUS_COMPLETE;
        // default projection
        Cursor c = SqliteWrapper.query(context, resolver, VZUris.getSmsUri(), null, selection, null, null);
        boolean result = false;
        if (c != null) {
            result = c.getCount() > 0;
            c.close();
        }
        return result;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#isMMSDelivered(long)
     */
    @Override
    public boolean isMMSDelivered(long luid) {

        return false;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#markSMSAsDelivered(long)
     */
    @Override
    public int markSMSAsDelivered(long luid) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("SMS Delivery :luid" + luid);
        }

        ContentValues values = new ContentValues();
        values.put(Sms.ERROR_CODE, Sms.STATUS_COMPLETE);
        values.put(Sms.STATUS, Sms.STATUS_COMPLETE);
        int count = SqliteWrapper.update(context, resolver,
                ContentUris.withAppendedId(VZUris.getSmsUri(), luid), values, null, null);

        ConversationDataObserver.onMessageStatusChanged(findSmsThreadId(context, luid), luid,
                ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
        return count;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#deleteSMS(long)
     */
    @Override
    public int deleteSMS(long luid) {
        long threadId = findSmsThreadId(context, luid);
        Uri deleteUri = ContentUris.withAppendedId(VZUris.getSmsUri(), luid);
        int count = SqliteWrapper.delete(context, resolver, deleteUri, null, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("deleteSMS(), luid=" + luid + ",count=" + count);
        }
        if (count > 0) {
            // Clear the unread notification , incase of it is received SMS/MMS
            uiNotifier.clearReadMessageNotification(deleteUri);
            // Sending provide notification to clear the PDUCache.
            uiNotifier.purge(deleteUri, true);

            ConversationDataObserver.onMessageDeleted(threadId, luid, ConversationDataObserver.MSG_TYPE_SMS,
                    ConversationDataObserver.MSG_SRC_TELEPHONY);
        }
        return count;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#deleteMMS(long)
     */
    @Override
    public int deleteMMS(long luid) {
        long threadId = findMmsThreadId(context, luid);
        Uri deleteUri = ContentUris.withAppendedId(VZUris.getMmsUri(), luid);
        int count = SqliteWrapper.delete(context, resolver, VZUris.getMmsUri(), Mms._ID + "=" + luid, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("deleteMMS(), luid=" + luid + ",count=" + count);
        }
        if (count > 0) {
            // Clear the unread notification , incase of it is received SMS/MMS
            uiNotifier.clearReadMessageNotification(deleteUri);
            // Sending provide notification to clear the PDUCache.
            uiNotifier.purge(deleteUri, true);

            ConversationDataObserver.onMessageDeleted(threadId, luid, ConversationDataObserver.MSG_TYPE_MMS,
                    ConversationDataObserver.MSG_SRC_TELEPHONY);
        }
        return count;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#moveSMStoSent(long, int)
     */
    @Override
    public int moveSMStoSent(long luid, int status) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("moveSMStoSent : for " + luid + ",status=" + status);
        }
        long threadId = findSmsThreadId(context, luid);
        ContentValues values = new ContentValues(2);
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_SENT);
        values.put(Sms.ERROR_CODE, status);
        int ret = SqliteWrapper.update(context, resolver,
                ContentUris.withAppendedId(VZUris.getSmsUri(), luid), values, null, null);

        ConversationDataObserver.onMessageStatusChanged(threadId, luid,
                ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);

        return ret;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#moveSMStoSendFailed(long, int)
     */
    @Override
    public int moveSMStoSendFailed(long luid, int error) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("moveSMStoSendFailed : for " + luid + ",error=" + error);
        }
        long threadId = findSmsThreadId(context, luid);
        ContentValues values = new ContentValues(2);
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
        values.put(Sms.ERROR_CODE, error);
        int ret = SqliteWrapper.update(context, resolver,
                ContentUris.withAppendedId(VZUris.getSmsUri(), luid), values, null, null);

        ConversationDataObserver.onMessageStatusChanged(threadId, luid,
                ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
        return ret;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#moveMMStoSent(long, int)
     */
    @Override
    public int moveMMStoSent(long luid, String messageId, int responseStatus) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("moveMMStoSent : luid=" + luid + ",responseStatus=" + responseStatus);
        }
        long threadId = findMmsThreadId(context, luid);
        ContentValues values = new ContentValues(2);
        values.put(Mms.MESSAGE_ID, messageId);
        values.put(Mms.RESPONSE_STATUS, responseStatus);
        values.put(Mms.MESSAGE_BOX, Mms.MESSAGE_BOX_SENT);
        int ret = SqliteWrapper.update(context, resolver,
                ContentUris.withAppendedId(VZUris.getMmsUri(), luid), values, null, null);

        ConversationDataObserver.onMessageStatusChanged(threadId, luid,
                ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
        return ret;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#moveMMStoSendFailed(long, int)
     */
    @Override
    public int moveMMStoSendFailed(long luid, int error) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("markMmsSendFailed : for " + luid + " error=" + error);
        }
        long threadId = findMmsThreadId(context, luid);
        ContentValues values = new ContentValues(1);
        values.put(Mms.RESPONSE_STATUS, error);
        int ret = SqliteWrapper.update(context, resolver,
                ContentUris.withAppendedId(VZUris.getMmsUri(), luid), values, null, null);

        ConversationDataObserver.onMessageStatusChanged(threadId, luid,
                ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
        return ret;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#getMms(java.lang.String)
     */
    @Override
    public VMAMessage getMms(String msgId) {
        VMAMessage vmaMessage = null;
        String where = Mms.MESSAGE_ID + "='" + msgId + "' AND (" + Mms.MESSAGE_TYPE + "="
                + PduHeaders.MESSAGE_TYPE_SEND_REQ + " OR " + Mms.MESSAGE_TYPE + "="
                + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF + ")";
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getMms : where =" + where);
        }

        String[] projection = new String[] { Mms._ID, Mms.THREAD_ID, Mms.MESSAGE_TYPE };
        Cursor cursor = SqliteWrapper.query(context, resolver, VZUris.getMmsUri(), projection, where, null,
                null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                vmaMessage = new VMAMessage();
                vmaMessage.setLuid(cursor.getLong(0));
                // Setting the conversation id
                vmaMessage.setLocalThreadId(cursor.getLong(1));
            }
            cursor.close();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getMmsId : returning ");
        }
        return vmaMessage;
    }

    public void applyDeliveryReports(Map<String, String> reports, String messageId, long threadId, long luid,
            boolean isSMS, long deliveryTime) {
        if (isSMS) {
            if (!isSMSDelivered(luid)) {
                applySMSDelivery(reports, luid);

                ConversationDataObserver.onMessageStatusChanged(threadId, luid,
                        ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("SMS already delivered. no need update again");
                }
            }
        } else {
            // Grep the already delivered receptions and compare with vma reports
            List<String> deliveredReceptions = getDeliveredReceptions(messageId);
            if (deliveredReceptions == null) {
                deliveredReceptions = new ArrayList<String>();
            }
            if (deliveredReceptions.size() < reports.size()) {
                applyMMSDelivery(reports, deliveredReceptions, messageId, luid, (deliveryTime / 1000L));

                ConversationDataObserver.onMessageStatusChanged(threadId, luid,
                        ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("MMS already delivered. no need update again");
                }
            }

        }
    }

    /**
     * This Method
     * 
     * @param reports
     * @param luid
     */
    private void applyMMSDelivery(Map<String, String> reports, List<String> deliveredReceptions,
            String messageId, long threadId, long deliveryTimeInSec) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("applyMMSDelivery:messageId=" + messageId + ",threadId=" + threadId + ",reports="
                    + reports + ",deliveredReceptions=" + deliveredReceptions);
        }
        for (String reception : reports.keySet()) {
            String value = reports.get(reception);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("MMS Delivery :reception=" + reception + ",status=" + value);
            }
            if ("DELIVERED".equalsIgnoreCase(value)) {
                if (!deliveredReceptions.contains(reception)) {
                    persisitDeliveryInd(messageId, threadId, reception, deliveryTimeInSec);
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("MMS already updated. reception=" + reception);
                    }
                }
            } else if ("FAILED".equalsIgnoreCase(value)) {

            } else if ("READ".equalsIgnoreCase(value)) {

            }
        }
    }

    /**
     * This Method
     * 
     * @param messageId
     * @param reciption
     * @return
     */
    private ArrayList<String> getDeliveredReceptions(String messageId) {
        String where = Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_DELIVERY_IND + " AND "
                + Mms.MESSAGE_ID + "='" + messageId + "'";
        String[] projection = new String[] { Mms._ID };
        Cursor cursor = SqliteWrapper.query(context, resolver, VZUris.getMmsUri(), projection, where, null,
                null);
        ArrayList<String> reciptions = null;
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                reciptions = new ArrayList<String>();
            }
            while (cursor.moveToNext()) {
                reciptions.addAll(getMmsRecepients(cursor.getLong(0)));
            }
            cursor.close();
        }
        return reciptions;
    }

    /**
     * This Method
     * 
     * @param reports
     */
    private void applySMSDelivery(Map<String, String> reports, long luid) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("applySMSDelivery:luid=" + luid);
        }
        for (String reciptions : reports.keySet()) {
            String value = reports.get(reciptions);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("SMS Delivery :recipient=" + reciptions + ",status=" + value);
            }
            if ("DELIVERED".equalsIgnoreCase(value)) {
                persistSMSDelivery(luid);
            } else if ("FAILED".equalsIgnoreCase(value)) {
                persistSMSDeliveryFailed(luid);
            } else if ("READ".equalsIgnoreCase(value)) {

            }
        }

    }

    /**
     * This Method
     * 
     * @param info
     */
    public void persistSMSDelivery(long luid) {
        ContentValues values = new ContentValues();
        values.put(Sms.ERROR_CODE, Sms.STATUS_COMPLETE);
        values.put(Sms.STATUS, Sms.STATUS_COMPLETE);
        int count = SqliteWrapper.update(context, resolver,
                ContentUris.withAppendedId(VZUris.getSmsUri(), luid), values, null, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("SMS Delivery :update count=" + count);
        }
    }

    public void persistSMSDeliveryFailed(long luid) {
        ContentValues values = new ContentValues(3);
        values.put(Sms.STATUS, 64);
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_FAILED);
        values.put(Sms.ERROR_CODE, SmsManager.RESULT_ERROR_NO_SERVICE);
        int count = SqliteWrapper.update(context, resolver,
                ContentUris.withAppendedId(VZUris.getSmsUri(), luid), values, null, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Marked SMS sent failed:update count=" + count +" luid="+luid);
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#mmsHasAttachement(android.net.Uri)
     */
    @Override
    public boolean mmsHasAttachement(Uri uri) {
        return mmsHasAttachement(ContentUris.parseId(uri));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#mmsHasAttachement(long)
     */
    @Override
    public boolean mmsHasAttachement(long luid) {
        Uri uri = VZUris.getMmsPartsUri(luid);
        String selection = Part.CONTENT_TYPE + " != '" + ContentType.TEXT_PLAIN + "' OR " + Part.CONTENT_TYPE
                + " != '" + ContentType.TEXT_HTML + "' OR " + Part.CONTENT_TYPE + " != '"
                + ContentType.APP_SMIL + "'";
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("mmsHasAttachement: uri=" + uri + ",where=" + selection);
        }
        // default projection
        Cursor c = SqliteWrapper.query(context, resolver, uri, new String[] { " COUNT (*) AS count " },
                selection, null, null);
        boolean result = false;
        if (c != null) {
            if (c.moveToFirst()) {
                result = c.getLong(0) > 0;
            }
            c.close();
        }
        return result;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#moveSMStoOutbox(long)
     */
    @Override
    public boolean moveSMStoOutbox(long luid) {
        String selection = Sms._ID + "=" + luid + " AND " + Sms.TYPE + "!=" + Sms.MESSAGE_TYPE_OUTBOX;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("moveSMStoOutbox: luid=" + luid);
        }
        ContentValues values = new ContentValues(2);
        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_OUTBOX);
        values.put(Sms.ERROR_CODE, 0);
        int count = SqliteWrapper.update(context, resolver, VZUris.getSmsUri(), values, selection, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("moveSMStoOutbox: return=" + count);
        }

        long threadId = findSmsThreadId(context, luid);
        ConversationDataObserver.onMessageStatusChanged(threadId, luid,
                ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
        return count > 0;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#moveMMStoOutbox(long)
     */
    @Override
    public boolean moveMMStoOutbox(long luid) {
        String selection = Mms._ID + "=" + luid + " AND " + Mms.RESPONSE_STATUS + "!=0";
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("moveMMStoOutbox : luid=" + luid);
        }
        ContentValues values = new ContentValues(2);
        values.put(Mms.DATE, (System.currentTimeMillis() / 1000L));
        values.put(Mms.RESPONSE_STATUS, 0);
        int count = SqliteWrapper.update(context, resolver, VZUris.getMmsUri(), values, selection, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("moveMMStoOutbox : return=" + count);
        }

        long threadId = findMmsThreadId(context, luid);
        ConversationDataObserver.onMessageStatusChanged(threadId, luid,
                ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
        return count > 0;
    }

    private Uri persisitDeliveryInd(String messageId, long thread, String reciption, long deliveredDate) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("persisitDeliveryInd: reciptions=" + reciption);
        }
        try {

            DeliveryInd delivery = new DeliveryInd();
            // Success
            delivery.setStatus(PduHeaders.STATUS_RETRIEVED);
            // threadId
            delivery.setThreadId(thread);
            // date .. need to get the timstamp from server
            delivery.setDate(deliveredDate);
            // MessageId
            delivery.setMessageId(messageId.getBytes());
            // To Address
            EncodedStringValue evalue = new EncodedStringValue(reciption);
            delivery.setTo(new EncodedStringValue[] { evalue });
            // Persister
            Uri uri = persister.persist(delivery, VZUris.getMmsInboxUri());
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("persisitDeliveryInd: uri=" + uri);
            }

            ConversationDataObserver.onNewMessageAdded(thread, ContentUris.parseId(uri),
                    ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);

            return uri;
        } catch (Exception e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Failed to persist Delivery Ind", e);
            }
        }
        return null;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#getLazyPDUMessage(long)
     */
    @Override
    public VMAMessage getSMSForMapping(long luid) {
        VMAMessage message = null;
        try {

            Cursor c = SqliteWrapper.query(context, resolver,
                    ContentUris.withAppendedId(VZUris.getSmsUri(), luid), SMS_CHECKSUM_PROJECTION, null,
                    null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    message = new VMAMessage();
                    message.setLuid(luid);
                    message.setLocalThreadId(c.getLong(0));
                    message.setMessageTime(new Date(c.getLong(1)));
                    message.setLocalParticipantId(c.getString(2));
                    message.setMessageText(c.getString(3));
                    int msgBox = (c.getInt(4) == Sms.MESSAGE_TYPE_SENT) ? VMAMapping.MSGBOX_SENT
                            : VMAMapping.MSGBOX_RECEIVED;
                    message.setLocalMessageBox(msgBox);
                }
                c.close();
            }
            return message;
        } catch (SQLException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error("unable to get SMS :got error - " + e.getMessage());
            }
        }
        return null;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#getMMSForMapping(long)
     */
    @Override
    public VMAMessage getMMSForMapping(long luid) {
        VMAMessage message = null;
        try {
            Cursor c = SqliteWrapper.query(context, resolver,
                    ContentUris.withAppendedId(VZUris.getMmsUri(), luid), MMS_VMAMAPPING_PROJECTION, null,
                    null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    message = new VMAMessage();
                    message.setLuid(luid);
                    message.setLocalThreadId(c.getLong(0));
                    message.setMessageTime(new Date(c.getLong(1)));
                    message.setMessageId(c.getString(2));
                    int msgBox = (c.getInt(3) == Mms.MESSAGE_BOX_SENT) ? VMAMapping.MSGBOX_SENT
                            : VMAMapping.MSGBOX_RECEIVED;
                    message.setLocalMessageBox(msgBox);
                }
                c.close();
            }
            return message;
        } catch (SQLException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error("unable to get MMS :got error - " + e.getMessage());
            }
        }
        return null;
    }

    /*
     * public void persistPart(List<VMAAttachment> attachments, long luid, String mdn) throws MmsException {
     * if (Logger.IS_DEBUG_ENABLED) { Logger.debug("persistPart : for " + luid); }
     * 
     * int count = attachments.size(); if (count == 1) { persistPart(attachment, luid); } else { VMAMessage
     * message = getMMSPduHeaders(luid); ArrayList<VMAAttachment> attachmentList = new
     * ArrayList<VMAAttachment>(); attachmentList.add(attachment); message.setAttachments(attachmentList); //
     * Need to fix // createNewMms(msg, mdn); VMAPduParser parser = new VMAPduParser(context, message, null,
     * mdn); parser.savePdu(); } } }
     */

    public void persistPart(List<VMAAttachment> attachments, long luid, String mdn) throws MmsException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("persistPart : for " + luid);
        }

        int count = attachments.size();

        if (count == 1) {
            Logger.debug("persistPart : only one attachment present no need to split it");

            persistPart(attachments.get(0), luid);
        } else {
            List<MMSUtil.MMSMessage> msgs = MMSUtil.splitMessages(attachments);

            int i = 0;
            Uri uri = null;
            for (MMSMessage msg : msgs) {
                ArrayList<VMAAttachment> msgAttachments = msg.getAttachment();

                if (i == 0) {
                    for (VMAAttachment attachment : msgAttachments) {
                        persistPart(attachment, luid);
                    }
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("persistPart : first message as parts instead of creating new message");
                    }
                } else {
                    VMAMessage message = getMMSPduHeaders(luid);
                    String sender = getMmsSender(luid);
                    if (!TextUtils.isEmpty(sender)) {
                        message.setMdn(sender);
                        message.setSourceAddr(sender);
                    } else {
                        message.setMdn(mdn);
                        message.setSourceAddr(mdn);
                    }
                    message.setAttachments(msg.getAttachment());
                    message.setMessageText("");
                    // Need to fix
                    // createNewMms(msg, mdn);

                    VMAPduParser parser = new VMAPduParser(context, message, mdn);
                    // no need to delete thumbnail. it has only original attachment
                    // deleteThumbnail(luid);
                    uri = parser.savePdu();
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("persistPart : uri for part number " + i + " saved as new message "
                                + uri);
                    }
                }
                i++;
            }
            if (i > 1) {
                // Clearing the media sync cache
                uiNotifier.purge(ContentUris.withAppendedId(VZUris.getMmsUri(), luid), true);
            }
        }
        long threadId = findMmsThreadId(context, luid);
        ConversationDataObserver.onMessageStatusChanged(threadId, luid,
                ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
    }

    /**
     * This Method
     */
    private void deleteThumbnail(long luid) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("deleteThumbnailPart : for " + luid);
        }
        String where = Part.CONTENT_DISPOSITION + "='" + MSAMessage.THUMBNAIL_SECTION + "'";
        int count = SqliteWrapper.delete(context, resolver, VZUris.getMmsPartsUri(luid), where, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.info("Thumbnail part deleted.count=" + count);
        }
    }

    /**
     * This Method
     * 
     * @param luid
     * @return
     */
    private VMAMessage getMMSPduHeaders(long luid) {
        VMAMessage msg = new VMAMessage();

        // msg.setMdn(mdn);
        // msg.setSourceAddr(mdn);

        ArrayList<String> toList = getMmsRecepients(luid);
        msg.setToAddrs(toList);
        msg.setMessageTime(getMmsMessageTime(luid));
        msg.setMessageId(StringUtils.getRandomString(15));
        msg.setMessageType(MessageTypeEnum.MMS);
        msg.setMessageSource(MessageSourceEnum.IMAP);
        msg.setMessageText(getMmsMessage(luid));
        // populate PDU
        populatePDU(luid, msg);

        if (!msg.isFlagSent()) {
            msg.setFlagSeen(true);
        }
        // populate delivery reports
        // populatedeliveryreports(VMAMessage msg);
        return msg;
    }

    private Date getMmsMessageTime(long luid) {
        Cursor c = SqliteWrapper.query(context, resolver,
                ContentUris.withAppendedId(VZUris.getMmsUri(), luid), new String[] { Mms.DATE }, null, null,
                null);
        Date date = null;
        if (c != null) {
            while (c.moveToNext()) {
                long time = c.getLong(0) * 1000;

                date = new Date(time);
                break;
            }
            c.close();
        }
        return date;
    }

    /**
     * This Method
     * 
     * @param luid
     * @param msg
     */
    private void populateAdr(long luid, VMAMessage msg) {
        ArrayList<String> toList = getMmsRecepients(luid, PduHeaders.TO);
        msg.setToAddrs(toList);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("populateAdr():TO Address:" + toList);
        }
        ArrayList<String> ccList = getMmsRecepients(luid, PduHeaders.CC);
        msg.setCcAddrs(ccList);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("populateAdr():CC Address:" + ccList);
        }

        ArrayList<String> bccList = getMmsRecepients(luid, PduHeaders.BCC);
        msg.setBccAddrs(bccList);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("populateAdr():BCC Address:" + bccList);
        }
    }

    private ArrayList<String> getMmsRecepients(long luid, int addressType) {
        ArrayList<String> recipients = new ArrayList<String>();
        Uri uri = Uri.parse("content://" + VZUris.getMmsAuthority() + "/" + luid + "/addr");
        // String where = Addr.TYPE + "!=" + PduHeaders.FROM ;
        String where = Addr.TYPE + "=" + addressType;
        Cursor c = resolver.query(uri, null, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                String address = c.getString(c.getColumnIndex(Addr.ADDRESS));
                recipients.add(ApplicationSettings.parseAdddressForChecksum(address));
            }
            c.close();
        }
        return recipients;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#deleteTempPart(android.net.Uri)
     */
    @Override
    public int deletePart(Uri tempPartUri) {
        return SqliteWrapper.delete(context, tempPartUri, null, null);
    }

    public static long findMmsThreadId(Context context, long id) {
        StringBuilder sb = new StringBuilder('(');
        sb.append(Mms._ID);
        sb.append('=');
        sb.append(id);

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getMmsUri(),
                new String[] { Mms.THREAD_ID }, sb.toString(), null, null);
        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        return -1;
    }

    public static long findMmsRowId(Context context, long id) {
        StringBuilder sb = new StringBuilder('(');
        sb.append(Mms.MESSAGE_ID);
        sb.append('=');
        sb.append(id);

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getMmsUri(),
                new String[] { Mms._ID }, sb.toString(), null, null);
        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    return cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        return -1;
    }

    public static long findSmsThreadId(Context context, long id) {
        long threadId = -1;
        StringBuilder sb = new StringBuilder('(');
        sb.append(Sms._ID);
        sb.append('=');
        sb.append(id);

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getSmsUri(),
                new String[] { Sms.THREAD_ID }, sb.toString(), null, null);
        try {
            if ((cursor != null) && (cursor.moveToFirst())) {
                threadId = cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return threadId;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#updateSMSReceivedTimeOnHandset(long, long)
     */
    @Override
    public void updateSMSReceivedTimeOnHandset(long luid, long vmaTimeStamp) {
        ContentValues values = new ContentValues(1);
        values.put(Sms.DATE, vmaTimeStamp);
        SqliteWrapper.update(context, ContentUris.withAppendedId(VZUris.getSmsUri(), luid), values, null,
                null);

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.PDUDao#updateMMSReceivedTimeOnHandset(long, long)
     */
    @Override
    public void updateMMSReceivedTimeOnHandset(long luid, long vmaTimeStamp) {
        ContentValues values = new ContentValues(1);
        values.put(Mms.DATE, vmaTimeStamp);
        SqliteWrapper.update(context, ContentUris.withAppendedId(VZUris.getMmsUri(), luid), values, null,
                null);
    }

    /* Overriding method 
     * (non-Javadoc)
     * @see com.vzw.vma.sync.refactor.PDUDao#getMessageTime(com.verizon.messaging.vzmsgs.provider.VMAMapping)
     */
    @Override
    public long getMessageTime(VMAMapping mapping) {
        Uri uri = null;
        String[] projection = null;
        if (mapping.isMMS()) {
            uri = ContentUris.withAppendedId(VZUris.getMmsUri(), mapping.getLuid());
            projection = new String[] { Mms.DATE };
        } else if (mapping.isSMS()) {
            uri = ContentUris.withAppendedId(VZUris.getSmsUri(), mapping.getLuid());
            projection = new String[] { Sms.DATE };
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("getMessageTime(): Unknown messagge type");
            }
        }
        long time = 0;
        if (uri != null && projection != null) {
            Cursor c = SqliteWrapper.query(context, uri, projection, null, null, null);
            if (c != null) {
                if (c.moveToNext()) {
                    time = c.getLong(0);
                }
                c.close();
            }
        }
        return time;
    }
}
