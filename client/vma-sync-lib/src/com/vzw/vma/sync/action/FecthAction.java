/**
 * FecthAction.java
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
import java.util.Map;

import javax.mail.MessagingException;

import android.content.ContentUris;

import com.strumsoft.android.commons.logger.Logger;
import com.sun.mail.iap.ProtocolException;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.messaging.vzmsgs.provider.dao.MapperDao;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler.Flags;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler.Source;
import com.verizon.mms.MmsException;
import com.verizon.sync.UiNotification;
import com.vzw.vma.common.message.MessageSourceEnum;
import com.vzw.vma.common.message.MessageTypeEnum;
import com.vzw.vma.common.message.VMAAttachment;
import com.vzw.vma.common.message.VMAMessage;
import com.vzw.vma.message.VMAMessageResponse;
import com.vzw.vma.message.VMAStore;
import com.vzw.vma.sync.refactor.PDUDao;
import com.vzw.vma.sync.refactor.imapconnection.impl.VMASync;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Feb 22, 2013
 */
public class FecthAction {

    private VMAStore store;
    private VMAEventHandler eventHandler;
    private PDUDao pduDao;
    private SyncItemDao syncItemDao;
    private UiNotification uiNotification;
    private long fullsyncLastUid = -1;
    private AppSettings settings;
    private static FecthAction instance;

    public synchronized static FecthAction getInstance() {
        return instance;
    }

    public synchronized static FecthAction getInstance(VMAStore store, VMAEventHandler eventHandler,
            PDUDao pduDao, SyncItemDao syncItemDao, UiNotification uiNotification, AppSettings settings) {
        if (instance == null) {
            instance = new FecthAction(store, eventHandler, pduDao, syncItemDao, uiNotification, settings);
        } else {
            instance.updateOldReference(store, eventHandler, pduDao, syncItemDao, uiNotification, settings);
        }
        return instance;
    }

    /**
     * This Method
     * 
     * @param store
     * @param eventHandler
     * @param pduDao
     * @param syncItemDao
     * @param uiNotification
     * @param settings
     */
    private void updateOldReference(VMAStore store, VMAEventHandler eventHandler, PDUDao pduDao,
            SyncItemDao syncItemDao, UiNotification uiNotification, AppSettings settings) {
        this.store = store;
        this.eventHandler = eventHandler;
        this.pduDao = pduDao;
        this.syncItemDao = syncItemDao;
        this.uiNotification = uiNotification;
        this.settings = settings;
    }

    /**
     * 
     * Constructor
     */
    private FecthAction(VMAStore store, VMAEventHandler eventHandler, PDUDao pduDao, SyncItemDao syncItemDao,
            UiNotification uiNotification, AppSettings settings) {
        this.store = store;
        this.eventHandler = eventHandler;
        this.pduDao = pduDao;
        this.syncItemDao = syncItemDao;
        this.uiNotification = uiNotification;
        this.settings = settings;
    }

    /**
     * This Method
     * 
     * @param syncItem
     * @param store
     * @param eventHandler
     * @param pduDao
     * @param mdn
     * @return
     * @throws ProtocolException
     * @throws IOException
     * @throws MessagingException
     * @throws MmsException
     */
    public int fetchAttachment(SyncItem syncItem, String mdn) throws ProtocolException, IOException,
            MessagingException, MmsException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Sending Attachement fecth request item=" + syncItem);
        }

        VMAMapping mapping = eventHandler.hasExistingMapping(syncItem.itemId);
        if (mapping == null || mapping.getLuid() == 0) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("No uid or luid found for attachement. marking as permanent failure." + syncItem);
            }
            return VMASync.ITEM_PERMANENT_FAILURE;
        }

        List<VMAAttachment> attachments = store.getAttachments(syncItem.itemId);
        mapping = eventHandler.hasExistingMapping(syncItem.itemId);
        if (mapping == null || mapping.getLuid() == 0) {
            // Need to check again incase it may deleted while downloading.
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("No mapping, or luid found when fetching attachment for syncItem " + syncItem);
            }
            for (VMAAttachment vmaAttachment : attachments) {
                if (Logger.IS_ERROR_ENABLED) {
                    Logger.debug("Deleting downloaded attachement:" + vmaAttachment.getTempPartUri());
                }
                pduDao.deletePart(vmaAttachment.getTempPartUri());
            }
        } else {
            if (attachments != null && !attachments.isEmpty()) {
                pduDao.persistPart(attachments, mapping.getLuid(), mdn);
                return VMASync.ITEM_SUCCESS;
            }
        }
        return VMASync.ITEM_TEMPORARY_FAILURE;
    }

    public boolean fetchMessageTablet(SyncItem syncItem, String mdn) throws ProtocolException, IOException,
            MessagingException, MmsException {
        // TEMP FIX: Fetch the message header if message type is MMS we will fetch message again with
        // thumbnail even
        // for flag updates.
        // TODO[JEGA] we have fetch the thumbnail only for new items/no mapped items.

        boolean processed = false;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("fetchMessageTablet(): item=" + syncItem);
        }
        long uid = syncItem.itemId;
        VMAMessageResponse msg = store.getUid(uid);

        if (msg != null) {
            VMAMessage vmamsg = msg.getVmaMessage();
            String msgId = msg.getVmaMessage().getMessageId();

            long timestamp = vmamsg.getMessageTime().getTime();
            MessageSourceEnum source = vmamsg.getMessageSource();
            Source src = Source.valueOf(source.getDesc());
            List<Flags> flags = getFlagsFromMessage(vmamsg);

            VMAMapping mapping;

            if (msg.getVmaMessage().isFlagSent()
                    && msg.getVmaMessage().getMessageSource() == MessageSourceEnum.IMAP) {
                int type = msg.getVmaMessage().getMessageType() == MessageTypeEnum.SMS ? MapperDao.SMS
                        : MapperDao.MMS;
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Tablet sent message, looking up using either uid=" + uid + " or msgid="
                            + msgId + " type=" + type);
                }
                mapping = eventHandler.hasExistingMapping(uid, msgId, type);
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Looking up using uid=" + uid);
                }
                mapping = eventHandler.hasExistingMapping(uid);
            }

            if (mapping == null) {
                /*
                 * 
                 * Pdu method will apply all the flags and delivery reports
                 */
                if (!msg.getVmaMessage().isFlagDeleted()) {
                    if (msg.getVmaMessage().isSMS()) {
                        pduDao.createNewSms(msg);

                    } else {
                        pduDao.createNewMms(msg, mdn);
                        if (vmamsg.getAttachments() != null && vmamsg.getAttachments().size() > 0) {
                            syncItemDao.addFetchAttachmentEvent(uid,
                                    syncItem.priority.getPriorityForAttachment());
                        }
                    }

                    long threadId = vmamsg.getLocalThreadId();
                    long luid = vmamsg.getLuid();

                    int messageBox = vmamsg.isFlagSent() ? VMAMapping.MSGBOX_SENT
                            : VMAMapping.MSGBOX_RECEIVED;

                    if (msg.getVmaMessage().isSMS()) {
                        eventHandler.vmaReceiveSMSTablet(luid, threadId, uid, msgId, timestamp, src, flags,
                                messageBox);
                    } else {
                        eventHandler.vmaReceiveMMSTablet(luid, threadId, uid, msgId, timestamp, src, flags,
                                messageBox);
                    }

                    // No need to call updatePdu on this path since the createNew methods take care of
                    // updating the
                    // flags & delivery reports

                    // Notifying the new messages
                    if (!syncItem.isFullSyncItem()) {
                        publishUnreadMessageNotificataton(luid, msg);
                    }
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Ignoring Deleted message on tablet");
                    }
                    
                    // DEL THUMBNAIL
//                    deleteThumbnail(msg);
                }

            } else {
                if (mapping.getUid() == 0) {
                    /*
                     * 
                     * Update the uid, timestamp, src and flags from VMA when we have a sent MMS/SMS from the
                     * tablet
                     */
                    eventHandler.vmaSendUpdateWithUidOnTablet(mapping.getId(), uid, timestamp, src, flags);

                } else {
                    eventHandler.vmaReceiveFlags(uid, flags);
                }
                updatePdu(mapping, msg.getDeliveryReports(), msg.getVmaMessage().getMessageId(),
                        msg.isSeen(), msg.isDeleted(), msg.getVmaMessage().isSMS(),msg.getVmaMessage().getMessageTime().getTime());

//                deleteThumbnail(msg);
            }
            processed = true;
        }else{
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("fetchMessageTablet():empty response " + msg);
            }
            return true;
        }
        return processed;
    }

    public boolean fetchMessageHeaders(SyncItem syncItem) throws ProtocolException, IOException,
            MessagingException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Sending Fecth message header request item=" + syncItem);
        }
        List<VMAMessageResponse> msgList = store.getUidsNoThumbnail(new long[] { syncItem.itemId });
//        List<VMAMessageResponse> msgList = store.getUidsHeaders(new long[] { syncItem.itemId });
        for (VMAMessageResponse vmaMsg : msgList) {
            List<Flags> f = getFlagsFromMessage(vmaMsg.getVmaMessage());
            VMAMapping mapping = eventHandler.vmaReceiveFlags(vmaMsg.getUID(), f);
            updatePdu(mapping, vmaMsg.getDeliveryReports(), vmaMsg.getVmaMessage().getMessageId(),
                    vmaMsg.isSeen(), vmaMsg.isDeleted(), vmaMsg.getVmaMessage().isSMS(),vmaMsg.getVmaMessage().getMessageTime().getTime());
        }
        

        return true;
    }

    private void updatePdu(VMAMapping mapping, Map<String, String> deliveryReports, String msgId,
            boolean isSeen, boolean isDeleted, boolean isSMS,long deliveredTime) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("updatePdu for " + mapping);
        }
        long luid = mapping.getLuid();
        if (luid > 0) {
            if (isDeleted) {
                if (isSMS) {
                    pduDao.deleteSMS(luid);
                } else {
                    pduDao.deleteMMS(luid);
                }
                eventHandler.deleteMapping(mapping.getId());
                syncItemDao.deleteEventsWithLuid(luid);
                if (mapping.getUid() > 0) {
                    ArrayList<Long> uids = new ArrayList<Long>();
                    uids.add(mapping.getUid());
                    syncItemDao.deleteEventsWithUids(uids);
                }
            } else if (isSeen) {
                if (isSMS) {
                    if (!pduDao.isSMSRead(luid)) {
                        pduDao.markSMSAsRead(luid);
                    }
                } else {
                    if (!pduDao.isMMSRead(luid)) {
                        pduDao.markMMSAsRead(luid);
                    }
                }
            }
            if (!isDeleted) {
                pduDao.applyDeliveryReports(deliveryReports, msgId, mapping.getThreadId(), luid, isSMS,deliveredTime);
            }
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("updatePdu: no luid, skipping till we get the message from telephony");
            }
        }
    }

    private List<Flags> getFlagsFromMessage(VMAMessage vmamsg) {
        List<Flags> flags = new ArrayList<VMAEventHandler.Flags>();
        if (vmamsg.isFlagDeleted()) {
            flags.add(Flags.DELETE);
        }
        if (vmamsg.isFlagSeen()) {
            flags.add(Flags.READ);
        }
        if (vmamsg.isFlagSent()) {
            flags.add(Flags.SENT);
        }
        return flags;
    }

    private void publishUnreadMessageNotificataton(long luid, VMAMessageResponse msg) {
        if (!msg.getVmaMessage().isFlagSent() && !msg.getVmaMessage().isFlagSeen()) {
            if (msg.getVmaMessage().isSMS()) {
                uiNotification.newMessage(ContentUris.withAppendedId(VZUris.getSmsUri(), luid));
            } else {
                uiNotification.newMessage(ContentUris.withAppendedId(VZUris.getMmsUri(), luid));
            }
        }
    }

    private VMAMessageResponse getWithThumbnailIfRequired(VMAMessageResponse msg) throws ProtocolException,
            IOException, MessagingException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getWithThumbnailIfRequired()");
        }
        if (msg.getVmaMessage().isMMS()) {
            VMAMessage vmamsg = msg.getVmaMessage();
            boolean shouldWeRefetch = (!isPriorMessage(msg.getUID())
                    && (vmamsg.getMessageSource().equals(MessageSourceEnum.WEB) || vmamsg.getMessageSource()
                            .equals(MessageSourceEnum.IMAP)) && vmamsg.isFlagSent());
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Should we refetch=" + shouldWeRefetch + " isPrior="
                        + isPriorMessage(msg.getUID()) + " Source=" + vmamsg.getMessageSource() + " Sent="
                        + vmamsg.isFlagSent() + " Deleted=" + vmamsg.isFlagDeleted());
            }
            if (shouldWeRefetch) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("getWithThumbnailIfRequired() : Refetching the uid with  thumbnail.");
                }
                return store.getUid(msg.getUID());
            }
        }
        return msg;

    }

    private boolean isPriorMessage(long uid) {
        if (fullsyncLastUid == -1) {
            this.fullsyncLastUid = settings.getLongSetting(AppSettings.KEY_FULLSYNC_LAST_UID, -1);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("isPriorMessage():msgUid=" + uid + ", fullsyncLastUid=" + fullsyncLastUid);
        }
        return uid <= fullsyncLastUid;
    }

    /**
     * This Method
     * 
     * @param syncItem
     * @return
     * @throws MessagingException
     * @throws IOException
     * @throws ProtocolException
     * @throws MmsException
     * 
     * 
     */
    public boolean fetchMessageHandset(SyncItem syncItem, String mdn) throws ProtocolException, IOException,
            MessagingException, MmsException {
        boolean processed = false;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Sending Fecth message request item=" + syncItem);
        }
        long uid = syncItem.itemId;
        VMAMessageResponse msg = null;
        List<VMAMessageResponse> msgList = store.getUidsNoThumbnail(new long[] { uid });
        if (msgList != null && !msgList.isEmpty()) {
            msg = msgList.get(0);
            if (msg != null) {
                msg = getWithThumbnailIfRequired(msg);
            }
        } else {

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("fetchMessageHandset(): empty response="+msgList);
            }
            return true;
        }

        if (msg != null) {
            VMAMapping mapping = eventHandler.hasExistingMapping(uid);
            VMAMessage vmamsg = msg.getVmaMessage();
            String msgId = msg.getVmaMessage().getMessageId();
            if (mapping == null) {
                /*
                 * 
                 * Pdu method will apply all the flags and delivery reports
                 * 
                 * On handset we need to do the following.
                 * 
                 * 1. Only messages that are sent from a paired device are persisted on the handset unless the
                 * message retrieved from VMA is older than from when we provsioned this handset with VMA.
                 */
                boolean shouldPersist = (!isPriorMessage(uid)
                        && (vmamsg.getMessageSource().equals(MessageSourceEnum.WEB) || vmamsg
                                .getMessageSource().equals(MessageSourceEnum.IMAP)) && vmamsg.isFlagSent());
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Should we persist=" + shouldPersist + " isPrior=" + isPriorMessage(uid)
                            + " Source=" + vmamsg.getMessageSource() + " Sent=" + vmamsg.isFlagSent()
                            + " Deleted=" + vmamsg.isFlagDeleted());
                }
                long luid = 0;
                long threadId = 0;
                if (shouldPersist) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Persisting message with PDU : " + uid);
                    }
                    if (!vmamsg.isFlagDeleted()) {
                        if (msg.getVmaMessage().isSMS()) {
                            pduDao.createNewSms(msg);
                        } else {
                            pduDao.createNewMms(msg, mdn);
                            if (vmamsg.getAttachments() != null && vmamsg.getAttachments().size() > 0) {
                                syncItemDao.addFetchAttachmentEvent(uid,
                                        syncItem.priority.getPriorityForAttachment());
                            }
                        }
                        threadId = vmamsg.getLocalThreadId();
                        luid = vmamsg.getLuid();
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Got a deleted message from VMA that was sent from a paired device, not persisting it");
                        }
                     // DEL THUMBNAIL
//                        deleteThumbnail(msg);
                    }
                } else {
                    if (msg.getVmaMessage().isMMS()) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Checking if as an mms meesage we can pair up with an existing luid");
                        }
                        VMAMessage mmsmsg = pduDao.getMms(msgId);
                        if (mmsmsg != null) {
                            threadId = mmsmsg.getLocalThreadId();
                            luid = mmsmsg.getLuid();
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("Found: luid=" + luid + " threadId=" + threadId);
                            }
                        }
                    }
                }

                long timestamp = vmamsg.getMessageTime().getTime();

                MessageSourceEnum source = vmamsg.getMessageSource();
                Source src = Source.valueOf(source.getDesc());
                List<Flags> flags = getFlagsFromMessage(vmamsg);
                String body = vmamsg.getMessageText();

                boolean isSent = vmamsg.isFlagSent();
                int messageBox = (isSent) ? VMAMapping.MSGBOX_SENT : VMAMapping.MSGBOX_RECEIVED;
                if (msg.getVmaMessage().isSMS()) {
                    String address = null;
                    if (isSent) {
                        // Sent from tablet/mobile/webportal
                        address = vmamsg.getToAddrs().get(0);
                    } else {
                        // Received from third party
                        address = vmamsg.getFromList().get(0);
                    }
                    mapping = eventHandler.vmaReceiveSMSHandset(luid, threadId, uid, body, address, msgId,
                            timestamp, src, flags, messageBox);
                } else {
                    mapping = eventHandler.vmaReceiveMMSHandset(luid, threadId, uid, msgId, timestamp, src,
                            flags, messageBox);
                }

                if (mapping != null && mapping.getLuid() > 0) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Applying vma flags to the previously received luid we matched - "
                                + mapping.getLuid());
                    }
                    updatePdu(mapping, msg.getDeliveryReports(), msg.getVmaMessage().getMessageId(),
                            msg.isSeen(), msg.isDeleted(), msg.getVmaMessage().isSMS(),msg.getVmaMessage().getMessageTime().getTime());

                    // DEL THUMBNAIL
//                    deleteThumbnail(msg);
                    
                } else {
                    if (mapping == null) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Why did we not get a mapping back for this message, shouldn't we have created one ?");
                        }
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Appears this is a case where we got informed by VMA before telephony, so we will wait for telephony - "
                                    + uid);
                        }
                    }
                }
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Only applying flags for this message.");
                }
                List<Flags> flags = getFlagsFromMessage(vmamsg);
                // If luid > 0 in the mapping apply the flags to the PDU
                updatePdu(mapping, msg.getDeliveryReports(), msg.getVmaMessage().getMessageId(),
                        msg.isSeen(), msg.isDeleted(), msg.getVmaMessage().isSMS(),msg.getVmaMessage().getMessageTime().getTime());
                // Delivery reports do not need to be saved off ever, since there should always be a valid
                // luid for the report
                eventHandler.vmaReceiveFlags(uid, flags);
            }

            processed = true;
        }

        return processed;
    }

    /**
     * This Method 
     * @param msg
     */
//    private void deleteThumbnail(VMAMessageResponse msg) {
//        if (msg.getVmaMessage().isMMS()) {
//            List<VMAAttachment> attachments = msg.getVmaMessage().getAttachments();
//            if (attachments != null && !attachments.isEmpty()) {
//                for (VMAAttachment vmaAttachment : attachments) {
//                    // Deleting thumbnail part
//                    int c = pduDao.deletePart(vmaAttachment.getTempPartUri());
//                    if (Logger.IS_DEBUG_ENABLED) {
//                        Logger.debug("Update call , deleting the downloaded thumbnail.count=" + c);
//                    }
//                }
//            }
//        }
//    }

}
