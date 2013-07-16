package com.vzw.vma.sync.refactor.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.widget.AutoCompleteTextView.Validator;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority;
import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.messaging.vzmsgs.provider.dao.MapperDao;
import com.verizon.messaging.vzmsgs.provider.dao.MapperDao.CallerSource;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.vzw.vma.common.message.MSAMessage;
import com.vzw.vma.common.message.VMAMessage;
import com.vzw.vma.sync.refactor.PDUDao;

public class VMAEventHandlerImpl implements VMAEventHandler {

    protected MapperDao mapper;
    protected SyncItemDao syncer;
    protected PDUDao pdu;

    /**
     * 
     * Constructor
     */
    public VMAEventHandlerImpl(MapperDao mapper, SyncItemDao syncer, PDUDao pdu) {
        this.mapper = mapper;
        this.syncer = syncer;
        this.pdu = pdu;
    }

    protected long computeChecksum(String address, String msgBody) {
        String checksumSource = ApplicationSettings.parseAdddressForChecksum(address) + msgBody;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Building Checksum source=" + checksumSource);
        }
        long checksum = 0;
        try {
            // checksum = ApplicationSettings.computeCheckSum(new
            // ByteArrayInputStream(checksumSource.getBytes()));
            checksum = ApplicationSettings.computeCheckSum(checksumSource.getBytes());
        } catch (IOException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Exception in Building Checksum source=" + e);
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Return checksum=" + checksum);
        }
        return checksum;
    }

    private VMAMapping getOrCreateMMSMappingForVMA(long luid, long threadId, long uid, String messageId,
            long timestamp, Source src, List<Flags> flags, int messageBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getOrCreateMMSMappingForVMA: " + messageId);
        }
        VMAMapping mapping = mapper.findMappingByMessageId(messageId, VMAMapping.TYPE_MMS);
        if (mapping == null) {
            mapping = createNewMMSMappingFromVMA(luid, threadId, uid, messageId, timestamp, src, flags,
                    messageBox);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getOrCreateMMSMappingForVMA: mapping=" + mapping);
        }
        return mapping;
    }

    private VMAMapping createNewMMSMappingFromVMA(long luid, long threadId, long uid, String messageId,
            long timestamp, Source src, List<Flags> flags, int messageBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createNewMMSMappingFromVMA: " + messageId);
        }
        VMAMapping inputmap = new VMAMapping();
        inputmap.setLuid(luid);
        inputmap.setMsgid(messageId);
        inputmap.setPendingUievents(0);
        inputmap.setSmschecksum(0);
        inputmap.setSource(src.getCode());
        inputmap.setSourceCreatedFrom(VMAMapping.SOURCECREATEDFROM_VMA);
        inputmap.setThreadId(threadId);
        long curtime = System.currentTimeMillis();
        inputmap.setTimeCreated(curtime);
        inputmap.setTimeofmessage(timestamp);
        inputmap.setTimeUpdated(curtime);
        inputmap.setType(VMAMapping.TYPE_MMS);
        inputmap.setUid(uid);
        inputmap.setVmaflags(collapseFlags(flags));
        inputmap.setMessageBox(messageBox);

        VMAMapping mapping = mapper.createMapping(inputmap);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createNewMMSMappingFromVMA: created " + mapping);
        }
        return mapping;
    }

    private long getCheckSumFromMsgId(String msgId) {
        long checksum = -1;

        if (msgId.contains("-")) {
            String[] extract = msgId.split("-", 2);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("VMA:checksum=" + Arrays.toString(extract));
            }
            checksum = Long.valueOf(extract[1]);
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Returning checksum = " + checksum + " from msgid=" + msgId);
        }
        return checksum;
    }

    protected long fullsyncLastUid = -1;

    private boolean isPriorMessage(long uid) {
        if (fullsyncLastUid == -1) {
            this.fullsyncLastUid = ApplicationSettings.getInstance().getLongSetting(
                    AppSettings.KEY_FULLSYNC_LAST_UID, -1);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("VMAEventHandler.isPriorMessage():msgUid=" + uid + ", fullsyncLastUid="
                    + fullsyncLastUid);
        }
        return uid <= fullsyncLastUid;
    }

    private VMAMapping getOrCreateSMSMappingForVMA(long luid, long threadId, long uid, String body,
            String mdn, long timestamp, String msgId, Source src, List<Flags> flags, int messageBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getOrCreateSMSMappingForVMA: uid=" + uid);
        }
        VMAMapping mapping = mapper.findMappingByUid(uid);
        if (mapping == null) {
            long checksum = getCheckSumFromMsgId(msgId);
            if (checksum == -1) {
                // Fore SMS messages that are sent from a paired device/web and for which we have a prior
                // message on the handset we want to match that to a existing message on the phone
                checksum = computeChecksum(mdn, body);
            }
            mapping = findMappingByChecksum(checksum, timestamp, CallerSource.VMA, messageBox,
                    VMAMapping.SOURCECREATEDFROM_VMA, uid);

            if (mapping == null) {
                mapping = createNewSMSMappingFromVMA(luid, threadId, uid, checksum, timestamp, msgId, src,
                        flags, messageBox);
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getOrCreateSMSMappingForVMA: returning " + mapping);
        }
        return mapping;
    }

    private VMAMapping createNewSMSMappingFromVMA(long luid, long threadId, long uid, long checksum,
            long timestamp, String msgId, Source src, List<Flags> flags, int messageBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createNewSMSMappingFromVMA: uid=" + uid);
        }
        VMAMapping inputmap = new VMAMapping();
        inputmap.setLuid(luid);
        inputmap.setMsgid(msgId);
        inputmap.setPendingUievents(0);
        inputmap.setSmschecksum(checksum);
        inputmap.setSource(src.getCode());
        inputmap.setSourceCreatedFrom(VMAMapping.SOURCECREATEDFROM_VMA);
        inputmap.setThreadId(threadId);
        long curtime = System.currentTimeMillis();
        inputmap.setTimeCreated(curtime);
        inputmap.setTimeofmessage(timestamp);
        inputmap.setTimeUpdated(curtime);
        inputmap.setType(VMAMapping.TYPE_SMS);
        inputmap.setUid(uid);
        inputmap.setVmaflags(collapseFlags(flags));
        inputmap.setMessageBox(messageBox);
        VMAMapping mapping = mapper.createMapping(inputmap);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createNewSMSMappingFromVMA: returning " + mapping);
        }
        return mapping;
    }

    protected VMAMapping getOrCreateMMSMappingForTelephony(long luid, long threadId, String messageId,
            long timestamp, int messageBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getOrCreateMMSMappingForTelephony: luid=" + luid);
        }
        /*
         * 
         * Search using messageId, if not found create it
         */
        VMAMapping mapping = mapper.findMappingByMessageId(messageId, MapperDao.MMS);
        if (mapping == null) {
            mapping = createNewMMSMappingForTelephony(luid, threadId, messageId, timestamp, messageBox);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getOrCreateMMSMappingForTelephony: returning " + mapping);
        }
        return mapping;
    }

    protected VMAMapping createNewMMSMappingForTelephony(long luid, long threadId, String messageId,
            long timestamp, int messageBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createNewMMSMappingForTelephony: luid=" + luid + " threadId=" + threadId
                    + " messageId=" + messageId + "tmestamp=" + timestamp + " messageBox=" + messageBox);
        }
        VMAMapping inputmap = new VMAMapping();
        inputmap.setLuid(luid);
        inputmap.setMsgid(messageId);
        inputmap.setPendingUievents(0);
        inputmap.setSmschecksum(0);
        inputmap.setSource(Source.PHONE.getCode());
        inputmap.setSourceCreatedFrom(VMAMapping.SOURCECREATEDFROM_TELEPHONY);
        inputmap.setThreadId(threadId);
        long curtime = System.currentTimeMillis();
        inputmap.setTimeCreated(curtime);
        inputmap.setTimeofmessage(timestamp);
        inputmap.setTimeUpdated(curtime);
        inputmap.setType(VMAMapping.TYPE_MMS);
        inputmap.setUid(0);
        inputmap.setMessageBox(messageBox);
        VMAMapping mapping = mapper.createMapping(inputmap);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createNewMMSMappingForTelephony: returning " + mapping);
        }
        return mapping;

    }

    protected VMAMapping createNewSMSMappingForTelephonyInternal(long luid, long threadId, String body,
            String mdn, long timestamp, int createdFromSrc, int msgBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createNewSMSMappingForTelephonyInternal: luid=" + luid + " threadId=" + threadId
                    + " mdn=" + mdn + " body=" + body + " timestamp=" + timestamp + " messageBox=" + msgBox);
        }
        long checksum = computeChecksum(mdn, body);
        VMAMapping inputmap = new VMAMapping();
        inputmap.setLuid(luid);
        inputmap.setMsgid("");
        inputmap.setPendingUievents(0);
        inputmap.setSmschecksum(checksum);
        inputmap.setSource(Source.PHONE.getCode());
        inputmap.setSourceCreatedFrom(createdFromSrc);
        inputmap.setThreadId(threadId);
        long curtime = System.currentTimeMillis();
        inputmap.setTimeCreated(curtime);
        inputmap.setTimeofmessage(timestamp);
        inputmap.setTimeUpdated(curtime);
        inputmap.setType(VMAMapping.TYPE_SMS);
        inputmap.setUid(0);
        inputmap.setMessageBox(msgBox);
        VMAMapping mapping = mapper.createMapping(inputmap);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("createNewSMSMappingForTelephonyInternal: returning " + mapping);
        }
        return mapping;
    }

    protected static final long FIVE_DAYS = 86400 * 5 * 1000L;

    protected VMAMapping findMappingByChecksum(long checksum, long timestamp, CallerSource vmaOrTelephony,
            int messageBox, int mappingInitiatedFrom, long uid) {
        VMAMapping mapping = null; // = mapper.findMappingByChecksum(checksum, timestamp, vmaOrTelephony);
        /*
         * 
         * Rules are: 1. Any unmapped messages need to be in the same messageBox if we are attempting to map a
         * sent SMS, it needs to map an existing mapping of SENT type 2. If we are attempting to map a message
         * that we received from VMAMapping.SOURCECREATEDFROM_TELEPHONY, then its matched_UID cannot be
         * isPriorMessage(matched_UID) 3. If we are attempting to map a message with UID that is coming from
         * VMAMapping.SOURCECREATEDFROM_VMA and !isPriorMessage(UID) then it has to map a
         * VMAMapping.SOURCECREATEDFROM_TELEPHONY & not VMAMapping.SOURCECREATEDFROM_CKSUMMAPPER 4. If we are
         * attempting to map a message from VMAMapping.SOURCECREATEDFROM_CKSUMMAPPER then it has to match
         * VMAMapping.SOURCECREATEDFROM_VMA with isPriorMessage(UID) true
         * 
         * If all the above are true then we filter based on time.
         * 
         * 5. Discard if the time difference of the two messages is > 5 days ? 6. Find the one closest in time
         */

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Find mapping for: checksum=" + checksum + " timestamp=" + timestamp + " "
                    + " from: " + vmaOrTelephony
                    + (messageBox == VMAMapping.MSGBOX_RECEIVED ? "Received" : "Sent") + " fromsrc="
                    + mappingInitiatedFrom + " uid=" + uid);
        }
        List<VMAMapping> mappings = mapper.findAllMappingsByChecksum(checksum, vmaOrTelephony, messageBox);

        long lastMatchDifference = Long.MAX_VALUE;
        if (mappings.size() > 0) {

            boolean isPrior = (uid > 0) ? isPriorMessage(uid) : false;
            for (VMAMapping candidate : mappings) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Examining: " + candidate);
                }

                if (uid > 0) {
                    // Message we are trying to match is coming from VMA, only that will have UID > 0
                    if (!isPrior) {
                        if (candidate.getSourceCreatedFrom() == VMAMapping.SOURCECREATEDFROM_CKSUMMAPPER) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("Skipping candidate since it is a new message from VMA which got to VMA after we installed, and we cannot match that to something that was already in the phone db.");
                            }
                            continue;
                        }
                    }
                } else {
                    // Message we are trying to match is coming either telephony or cksummmapper
                    long candidateuid = candidate.getUid();
                    // We should not get here where candidate uid == 0 since the mapper dao query should have
                    // filtered that out
                    boolean candidateIsPrior = isPriorMessage(candidateuid);
                    if (mappingInitiatedFrom == VMAMapping.SOURCECREATEDFROM_TELEPHONY) {
                        if (candidateIsPrior) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("Skipping candidate since we have a new message from telephony and we cannot match that to something that was on vam before we installed.");
                            }
                            continue;
                        }
                    } else if (mappingInitiatedFrom == VMAMapping.SOURCECREATEDFROM_CKSUMMAPPER) {
                        if (!candidateIsPrior) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("Skipping candidate since we are matching an existing message on phone and matcing it to something that did not exist on VMA does not make sense.");
                            }
                            continue;
                        }
                    }
                }
                long candidateTime = candidate.getTimeofmessage();
                long diff = timestamp - candidateTime;
                if (diff > 0) {
                    if (diff > FIVE_DAYS) {
                        // We have exhausted all candidates, since candidates are in DESC order of timestamp
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Breaking out since we are more than 5 days newer than current candidate.");
                        }
                        break;
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Potential candidate that is  older than us:" + candidate);
                        }
                        if (diff < lastMatchDifference) {
                            lastMatchDifference = diff;
                            mapping = candidate;
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("So far this is our best candidate with a time diff of " + diff);
                            }
                        }
                    }
                } else {
                    diff = -diff;
                    if (diff > FIVE_DAYS) {
                        // skip this candidates, and see if next one is within window
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Skip this candidate since we are more than 5 days older than current candidate.");
                        }
                        continue;
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Potential candidate that is newer than us:" + candidate);
                        }
                        if (diff < lastMatchDifference) {
                            lastMatchDifference = diff;
                            mapping = candidate;
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("So far this is our best candidate with a time diff of " + diff);
                            }
                        }
                    }
                }
            }
        }

        if (mapping != null) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Best match had a difference of " + lastMatchDifference);
            }
        }
        return mapping;

    }

    /*
     * Called when we get a sms from telephony to look up our mapping tables
     */
    protected VMAMapping getOrCreateSMSMappingForTelephony(long luid, long threadId, String body, String mdn,
            long timestamp, int createdFromSrc, int messageBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getOrCreateSMSMappingForTelephony: luid=" + luid);
        }
        VMAMapping mapping = mapper.findMappingByPduLuid(luid, VMAMapping.TYPE_SMS);
        if (mapping == null) {
            long checksum = computeChecksum(mdn, body);
            mapping = findMappingByChecksum(checksum, timestamp, CallerSource.TELEPHONY, messageBox,
                    createdFromSrc, 0);
            if (mapping == null) {
                mapping = createNewSMSMappingForTelephonyInternal(luid, threadId, body, mdn, timestamp,
                        createdFromSrc, messageBox);
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getOrCreateSMSMappingForTelephony: returning " + mapping);
        }
        return mapping;
    }

    /*
     * Called when there is an existing mapping and we get a corresponding new message from telephony.
     * 
     * This method will update the
     */
    protected void pairExistingMappingWithTelephony(VMAMapping mapping, long luid, long threadId) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("pairVMAMappingWithTelephony: luid=" + luid + " threadId=" + threadId);
        }
        /*
         * 1. Updated mapping table with luid & threaid
         * 
         * 2. If any pending vma flags apply that to the sync items
         * 
         * Note delivery reports need not be applied here, since they will always come from VMA after we have
         * a local message and would be applied as part of the sent message if the message was sent from a
         * tablet, and from telephony if it was sent from the phone.
         */

        mapper.updateMappingWithluid(mapping.getId(), luid, threadId);

        int flags = mapping.getVmaflags();
        if (flags > 0) {
            if ((Flags.DELETE.getCode() & flags) == Flags.DELETE.getCode()) {
                if (mapping.getType() == MapperDao.MMS) {
                    pdu.deleteMMS(luid);
                } else {
                    pdu.deleteSMS(luid);
                }
            } else if ((Flags.READ.getCode() & flags) == Flags.READ.getCode()) {
                if (mapping.getType() == MapperDao.MMS) {
                    pdu.markMMSAsRead(luid);
                } else {
                    pdu.markSMSAsRead(luid);
                }
            }
        }

    }

    private int collapseFlags(List<Flags> flags) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("collapseFlags: flags=" + flags);
        }
        int collapsed = 0;
        if (flags != null) {
            for (Flags f : flags) {
                collapsed |= f.getCode();
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("collapseFlags: collapsed=" + collapsed);
        }
        return collapsed;
    }

    protected void sendDeleteToVMA(long uid) {
        /*
         * First delete all other notifications to VMA bout this uid
         */
        ArrayList<Long> uids = new ArrayList<Long>();
        uids.add(uid);
        syncer.deleteEventsWithUids(uids);

        /*
         * Next queue up a delete to the server
         */
        syncer.addDeleteEvent(uid, ItemPriority.ONDEMAND_CRITICAL);
    }

    /*
     * Called when there is an existing mapping and we get a corresponding new message from VMA.
     * 
     * This method will check if the msgId & flags need updating and update them
     * 
     * The caller is responsible for any pdu interaction
     */
    private void pairExistingMappingWithVMA(VMAMapping mapping, long uid, String msgId, List<Flags> flags,
            Source src) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("pairExistingMappingWithVMA: called=" + mapping + " uid=" + uid + " msgId=" + msgId
                    + " flags=" + flags);
        }
        int collapsed = collapseFlags(flags);
        if (mapping.getUid() == 0 || !msgId.equals(mapping.getMsgid())
                || (mapping.getVmaflags() != collapsed)) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Calling updateMessageIdAndFlags " + mapping.getId() + "  " + msgId + " "
                        + collapsed + "uid=" + uid);
            }
            mapper.updateUidMessageIdSrcAndFlags(mapping.getId(), uid, msgId, collapsed, src.getCode());
            mapping.setUid(uid);
            mapping.setMsgid(msgId);
            mapping.setVmaflags(collapsed);
        }
        int pendingUiEvents = mapping.getPendingUievents();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("pairVMAMappingWithTelephony: pending events=" + pendingUiEvents);
        }
        if (pendingUiEvents != 0) {
            if ((pendingUiEvents & Flags.READ.getCode()) == Flags.READ.getCode()) {
                syncer.addReadEvent(uid, ItemPriority.ONDEMAND_CRITICAL);
            }
            if ((pendingUiEvents & Flags.DELETE.getCode()) == Flags.DELETE.getCode()) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("pairExistingMappingWithVMA: deleting mapping entry & adding deleted event for uid="
                            + uid);
                }
                sendDeleteToVMA(uid);
                mapper.deleteMapping(mapping.getId());
            }
        }
    }

    protected VMAMapping uiSMSDeleteCommon(long luid, int msgType) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("uiSMSDeleteCommon: called luid=" + luid + " msgType=" + msgType);
        }
        VMAMapping mapping = mapper.findMappingByPduLuid(luid, msgType);
        if (mapping != null) {
            long uid = mapping.getUid();
            if (uid > 0) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("uiSMSDeleteCommon:  deleting mapping entry & adding deleted event for uid="
                            + uid);
                }
                sendDeleteToVMA(uid);
                mapper.deleteMapping(mapping.getId());
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("uiSMSDeleteCommon: adding pending delete ui event for luid which does not have a uid");
                }
                // Negative timestamp
                long tempLuid = -System.currentTimeMillis();
                mapper.addPendingUiEvent(mapping.getId(), tempLuid, luid, Flags.DELETE.getCode());
                mapping.setPendingUievents(mapping.getPendingUievents() | Flags.DELETE.getCode());
                mapping.setLuid(tempLuid);
                mapping.setOldLuid(luid);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("uiSMSDeleteCommon: marking luid to negative number to avoid conflict"
                            + mapping);
                }
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("uiSMSDeleteCommon: returning=" + mapping);
        }
        return mapping;
    }

    protected VMAMapping uiSMSReadCommon(long luid, int msgType) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("uiSMSReadCommon: called luid=" + luid + " msgType=" + msgType);
        }
        VMAMapping mapping = mapper.findMappingByPduLuid(luid, msgType);
        if (mapping != null) {
            long uid = mapping.getUid();
            if (uid > 0) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("uiSMSReadCommon: adding read event for uid=" + uid);
                }
                syncer.addReadEvent(uid, ItemPriority.ONDEMAND_CRITICAL);
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("uiSMSReadCommon: adding pending read ui event for luid which does not have a uid");
                }
                mapper.addPendingUiEvent(mapping.getId(), Flags.READ.getCode());
                mapping.setPendingUievents(mapping.getPendingUievents() | Flags.READ.getCode());
            }
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Don't have a mapping yet, likely the read notification from UI came before the telephony, happens when conv is open");
            }
            mapping = createNewMappingFromPdu(luid, msgType);
            if (mapping != null) {
                long uid = mapping.getUid();
                if (uid > 0) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("uiSMSReadCommon: adding read event for uid=" + uid);
                    }
                    syncer.addReadEvent(uid, ItemPriority.ONDEMAND_CRITICAL);
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("uiSMSReadCommon: adding pending read ui event for luid which does not have a uid");
                    }
                    mapper.addPendingUiEvent(mapping.getId(), Flags.READ.getCode());
                    mapping.setPendingUievents(mapping.getPendingUievents() | Flags.READ.getCode());
                }
            }

        }
        return mapping;
    }

    /*
     * Called when we get a notification from the UI before we get one from the telephony hooks.
     * SmsReceiverService first persists the SMS in PDU tables, the UI picks it up and then if the
     * conversation is open it sends a read notification to the vma code.
     * 
     * In the meanwhile the SmsReceiverservice goes ahead and attempts to create the mapping. If the mapping
     * get created after the read hook is fired from the UI then the message never gets updated
     */
    private VMAMapping createNewMappingFromPdu(long luid, int msgType) {

        VMAMapping mapping = null;
        VMAMessage msg = null;
        if (msgType == VMAMapping.TYPE_MMS) {
            msg = pdu.getMMSForMapping(luid);
            if (msg != null) {
                long threadId = msg.getLocalThreadId();
                String messageId = msg.getMessageId();
                long timestamp = msg.getMessageTime().getTime();
                int messageBox = msg.getLocalMessageBox();
                // Simulate receive from telephony
                mapping = telephonyMMSReceive(luid, threadId, messageId, timestamp);
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Could not locate mms pdu for luid=" + luid);
                }
            }
        } else {
            msg = pdu.getSMSForMapping(luid);
            if (msg != null) {
                String mdn = msg.getLocalParticipantId();
                int messageBox = msg.getLocalMessageBox();
                // VMAMapping.MSGBOX_RECEIVED
                // VMAMapping.MSGBOX_SENT
                String body = msg.getMessageText();
                long checksum = computeChecksum(mdn, body);
                long timestamp = msg.getMessageTime().getTime();
                long threadId = msg.getLocalThreadId();
                // Simulate receive from telephony
                mapping = telephonySMSReceive(luid, threadId, body, mdn, timestamp);
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Could not locate sms pdu for luid=" + luid);
                }
            }
        }

        return mapping;
    }

    @Override
    public synchronized VMAMapping telephonyMMSReceive(long luid, long threadId, String messageId,
            long timestamp) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("telephonyMMSReceive: called: luid=" + luid + " threadId=" + threadId
                    + " messageId=" + messageId + " timestamp=" + timestamp);
        }
        VMAMapping mapping = getOrCreateMMSMappingForTelephony(luid, threadId, messageId, timestamp,
                VMAMapping.MSGBOX_RECEIVED);
        if (mapping.getLuid() == 0) {
            /*
			 * 
			 */
            pairExistingMappingWithTelephony(mapping, luid, threadId);
            // If the Telephony message received time and VMA message received time difference is greater than
            // a
            // minute we have to update VMA timestamp on Handset to avoid the received message getting messed
            // up
            // on the ui.
            updateHandsetMsgTimeFromTelephony(timestamp, luid, mapping);
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("telephonyMMSReceive: returning : " + mapping);
        }
        return mapping;
    }

    @Override
    public synchronized VMAMapping telephonyMMSSend(long luid, long threadId, String messageId, long timestamp) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("telephonyMMSSend: called luid=" + luid);
        }
        VMAMapping mapping = createNewMMSMappingForTelephony(luid, threadId, messageId, timestamp,
                VMAMapping.MSGBOX_SENT);
        return mapping;
    }

    @Override
    public synchronized VMAMapping telephonySMSReceive(long luid, long threadId, String body, String mdn,
            long msgTime) {
        return telephonySMSReceive(luid, threadId, body, mdn, msgTime, 0);
    }

    @Override
    public synchronized VMAMapping telephonySMSReceive(long luid, long threadId, String body, String mdn,
            long smscTime, long msgSavedTime) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("telephonySMSReceive: called luid=" + luid + " smscTime=" + smscTime
                    + " msgSavedTime=" + msgSavedTime);
        }
        VMAMapping mapping = getOrCreateSMSMappingForTelephony(luid, threadId, body, mdn, smscTime,
                VMAMapping.SOURCECREATEDFROM_TELEPHONY, VMAMapping.MSGBOX_RECEIVED);
        if (mapping.getLuid() == 0) {
            pairExistingMappingWithTelephony(mapping, luid, threadId);
            // If the Telephony message received time and VMA message received time difference is greater than
            // a
            // minute we have to update VMA timestamp on Handset to avoid the received message getting messed
            // up
            // on the ui.
            updateHandsetMsgTimeFromTelephony(((msgSavedTime > 0) ? msgSavedTime : smscTime), luid, mapping);
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("telephonySMSReceive: returning : " + mapping);
        }
        return mapping;
    }

    @Override
    public synchronized VMAMapping telephonySMSSend(long luid, long threadId, String body, String mdn,
            long timestamp) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("telephonySMSSend: called luid=" + luid);
        }
        VMAMapping mapping = createNewSMSMappingForTelephonyInternal(luid, threadId, body, mdn, timestamp,
                VMAMapping.SOURCECREATEDFROM_TELEPHONY, VMAMapping.MSGBOX_SENT);
        return mapping;
    }

    @Override
    public synchronized VMAMapping vmaReceiveSMSHandset(long luid, long threadId, long uid, String body,
            String mdn, String msgId, long timestamp, Source src, List<Flags> flags, int messageBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaSMSReceiveHandset: called for : uid=" + uid + " time=" + timestamp);
        }
        VMAMapping mapping = getOrCreateSMSMappingForVMA(luid, threadId, uid, body, mdn, timestamp, msgId,
                src, flags, messageBox);
        if (mapping.getUid() == 0) {
            pairExistingMappingWithVMA(mapping, uid, msgId, flags, src);
            // If the Telephony message received time and VMA message received time difference is greater than
            // a
            // minute we have to update VMA timestamp on Handset to avoid the received message getting messed
            // up
            // on the ui.
            updateHandsetMsgTimeFromVMA(timestamp, mapping);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaSMSReceiveHandset: returning=" + mapping);
        }
        return mapping;
    }

    @Override
    public synchronized VMAMapping vmaReceiveMMSHandset(long luid, long threadId, long uid, String messageId,
            long timestamp, Source src, List<Flags> flags, int messageBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaMMSReceiveHandset: called for : uid=" + uid);
        }
        VMAMapping mapping = getOrCreateMMSMappingForVMA(luid, threadId, uid, messageId, timestamp, src,
                flags, messageBox);
        if (mapping.getUid() == 0) {
            pairExistingMappingWithVMA(mapping, uid, messageId, flags, src);
            // If the Telephony message received time and VMA message received time difference is greater than
            // a
            // minute we have to update VMA timestamp on Handset to avoid the received message getting messed
            // up
            // on the ui.
            updateHandsetMsgTimeFromVMA(timestamp, mapping);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaMMSReceiveHandset: returning=" + mapping);
        }
        return mapping;
    }

    private void updateHandsetMsgTimeFromVMA(long vmaTime, VMAMapping mapping) {
        // Telephony message received before vma
        if (mapping.isReceivedMessage()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("updateHandsetMsgTimeFromVMA() vmaTime=" + vmaTime + " mapping=" + mapping);
            }
            if (mapping.getLuid() > 0) {
                long msgTime = pdu.getMessageTime(mapping);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("updateHandsetMsgTimeFromVMA() msgSavedTime:" + new Date(msgTime).toGMTString());
                }
                updateTelephonyReceivedMsgTime(vmaTime, msgTime, mapping.getLuid(),
                        mapping.getType());
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("updateHandsetMsgTimeFromVMA()  no LUID found ");
                }
            }
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("updateHandsetMsgTimeFromVMA() is an sent msg skipping time update.");
            }
        }
    }

    private void updateHandsetMsgTimeFromTelephony(long telephonyTime, long luid, VMAMapping mapping) {
        // VMA message received before Telephony
        if (mapping.isReceivedMessage()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("updateHandsetMsgTimeFromTelephony() telephonyTime=" + telephonyTime + " luid="
                        + luid + " mapping=" + mapping);
            }
            if (mapping.getUid() > 0) {
                // Message already mapped with VMA
                if (Logger.IS_DEBUG_ENABLED) {
                    if (telephonyTime == mapping.getTimeofmessage()) {
                        Logger.warn("updateHandsetMsgTimeFromTelephony() : SMS Gateway time and VMA time are same. ");
                    }
                }
                updateTelephonyReceivedMsgTime(mapping.getTimeofmessage(), telephonyTime, luid,
                        mapping.getType());
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("updateHandsetMsgTimeFromTelephony()  No UID found ");
                }
            }
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("updateHandsetMsgTimeFromTelephony()  sent message skiping time update");
            }
        }
    }

    /**
     * This Method
     */
    private void updateTelephonyReceivedMsgTime(long vmaTime, long telephonyTime, long luid, int msgType) {

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("updateTelephonyReceivedMsgTime()  vmaTime=" + new Date(vmaTime)
                    + ", telephonyTime =" + new Date(telephonyTime) + ", luid=" + luid + " msgType="
                    + msgType);
        }
        // Telephony message received before VMA Sync. Updating the timestamp if the Telephony time > (VMA
        // time + 1 minute)

        long differnce = (telephonyTime > vmaTime) ? telephonyTime - vmaTime : vmaTime - telephonyTime;
        // long milliseconds= differnce / 1000 % 60;

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("updateTelephonyReceivedMsgTime() TIME DIFFERENCE = " + differnce);
        }

        if (differnce >= (1000 * 60)) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("updateTelephonyReceivedMsgTime() Telephony SMS/MMS time has more than a minute difference. updating the vmatmestamp.");
            }
            if (msgType == VMAMapping.TYPE_SMS) {
                pdu.updateSMSReceivedTimeOnHandset(luid, vmaTime);
            } else if (msgType == VMAMapping.TYPE_MMS) {
                // 
                long timeInSeconds = (vmaTime< Integer.MAX_VALUE)? vmaTime: (vmaTime / 1000);
                pdu.updateMMSReceivedTimeOnHandset(luid, timeInSeconds);
            }
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("updateTelephonyReceivedMsgTime() Telephony SMS/MMS time has less than a minute difference. skpping the vma timestamp update.");
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vzw.vma.sync.refactor.VMAEventHandler#vmaSMSSend(long, java.lang.String, java.lang.String,
     * long, java.lang.String)
     */
    @Override
    public synchronized VMAMapping vmaSendSMS(long luid, long threadId, String body, String mdn,
            long timestamp, String msgId) {
        // msgId is vma_XXX temp msg id
        // Only called from tablet
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaSMSSend: called for : luid=" + luid);
        }
        VMAMapping mapping = mapper.findMappingByPduLuid(luid, VMAMapping.TYPE_SMS);
        if (mapping != null) {
            Logger.error("Error: Mapping exists for luid " + luid + " of type " + VMAMapping.TYPE_SMS);
            throw new RuntimeException("Error: Mapping exists for luid " + luid + " of type "
                    + VMAMapping.TYPE_SMS);
        }
        long checksum = computeChecksum(mdn, body);
        VMAMapping inputmap = new VMAMapping();
        inputmap.setLuid(luid);
        inputmap.setMsgid(msgId);
        inputmap.setPendingUievents(0);
        inputmap.setSmschecksum(checksum);
        inputmap.setSource(Source.IMAP.getCode());
        // XXX: We should probably create a new type for TABLET
        inputmap.setSourceCreatedFrom(VMAMapping.SOURCECREATEDFROM_CKSUMMAPPER);
        inputmap.setThreadId(threadId);
        long curtime = System.currentTimeMillis();
        inputmap.setTimeCreated(curtime);
        inputmap.setTimeofmessage(timestamp);
        inputmap.setTimeUpdated(curtime);
        inputmap.setType(VMAMapping.TYPE_SMS);
        inputmap.setUid(0);
        inputmap.setMessageBox(VMAMapping.MSGBOX_SENT);
        mapping = mapper.createMapping(inputmap);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaSMSSend: returning mapping=" + mapping);
        }
        return mapping;
    }

    @Override
    public synchronized VMAMapping vmaReceiveMMSTablet(long luid, long threadId, long uid, String messageId,
            long timestamp, Source src, List<Flags> flags, int messageBox) {
        return vmaReceiveTabletInternal(luid, threadId, uid, messageId, timestamp, src, flags,
                VMAMapping.TYPE_MMS, messageBox);
    }

    @Override
    public VMAMapping vmaReceiveSMSTablet(long luid, long threadId, long uid, String messageId,
            long timestamp, Source src, List<Flags> flags, int messageBox) {
        return vmaReceiveTabletInternal(luid, threadId, uid, messageId, timestamp, src, flags,
                VMAMapping.TYPE_SMS, messageBox);
    }

    protected synchronized VMAMapping vmaReceiveTabletInternal(long luid, long threadId, long uid,
            String messageId, long timestamp, Source src, List<Flags> flags, int msgType, int messageBox) {

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaReceiveTabletInternal: called for : uid=" + uid + " type=" + msgType);
        }
        VMAMapping mapping = null;
        // VMAMapping existing = mapper.findMappingByMessageId(messageId, msgType);
        VMAMapping existing = mapper.findMappingByMessageIdAndBoxType(messageId, msgType, messageBox);
        if (existing == null) {
            /*
             * 
             * This only gets called once the caller has checked using messageId for existence and failed
             */
            VMAMapping inputmap = new VMAMapping();
            inputmap.setLuid(luid);
            inputmap.setMsgid(messageId);
            inputmap.setPendingUievents(0);
            inputmap.setSmschecksum(0);
            inputmap.setSource(src.getCode());
            inputmap.setSourceCreatedFrom(VMAMapping.SOURCECREATEDFROM_VMA);
            inputmap.setThreadId(threadId);
            long curtime = System.currentTimeMillis();
            inputmap.setTimeCreated(curtime);
            inputmap.setTimeofmessage(timestamp);
            inputmap.setTimeUpdated(curtime);
            inputmap.setType(msgType);
            inputmap.setUid(uid);
            inputmap.setVmaflags(collapseFlags(flags));
            inputmap.setMessageBox(messageBox);
            mapping = mapper.createMapping(inputmap);

        } else {
            if (existing.getUid() == 0) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("On tablet mapping with uid=0. Can happen if the mapping got created because the UI observer tried to mark message as read before we get here");
                }
                mapping = existing;
                pairExistingMappingWithVMA(existing, uid, messageId, flags, src);
                mapping.setUid(uid);
            } else {
                mapping = vmaReceiveFlags(uid, flags);
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaReceiveTabletInternal: returning mapping=" + mapping);
        }
        return mapping;
    }

    @Override
    public synchronized void vmaSendUpdateWithUidOnTablet(long id, long uid, long timestamp, Source src,
            List<Flags> flags) {
        int collapsed = this.collapseFlags(flags);
        mapper.updateUidTimestampAndFlags(id, uid, src.getCode(), timestamp, collapsed);
    }

    @Override
    public synchronized VMAMapping vmaSendMMS(long luid, long threadId, String messageId, long timestamp) {
        // msgId is vma_XXX temp msg id
        // Only called from tablet
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaMMSSend: called for : luid=" + luid);
        }
        VMAMapping mapping = mapper.findMappingByPduLuid(luid, VMAMapping.TYPE_MMS);
        if (mapping != null) {
            Logger.error("Error: Mapping exists for luid " + luid + " of type " + VMAMapping.TYPE_MMS);
            throw new RuntimeException("Error: Mapping exists for luid " + luid + " of type "
                    + VMAMapping.TYPE_MMS);
        }
        long checksum = 0;
        VMAMapping inputmap = new VMAMapping();
        inputmap.setLuid(luid);
        inputmap.setMsgid(messageId);
        inputmap.setPendingUievents(0);
        inputmap.setSmschecksum(checksum);
        inputmap.setSource(Source.IMAP.getCode());
        // XXX: We should probably create a new type for TABLET
        inputmap.setSourceCreatedFrom(VMAMapping.SOURCECREATEDFROM_CKSUMMAPPER);
        inputmap.setThreadId(threadId);
        long curtime = System.currentTimeMillis();
        inputmap.setTimeCreated(curtime);
        inputmap.setTimeofmessage(timestamp);
        inputmap.setTimeUpdated(curtime);
        inputmap.setType(VMAMapping.TYPE_MMS);
        inputmap.setUid(0);
        inputmap.setMessageBox(VMAMapping.MSGBOX_SENT);
        mapping = mapper.createMapping(inputmap);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaMMSSend: returning mapping=" + mapping);
        }
        return mapping;
    }

    @Override
    public synchronized VMAMapping vmaReceiveFlags(long uid, List<Flags> flags) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaReceiveFlags: called for : uid=" + uid + " flags=" + flags);
        }
        VMAMapping mapping = mapper.findMappingByUid(uid);
        if (mapping == null) {
            // This has to be logic error
            Logger.error("vmaReceiveFlags: called for : uid="
                    + uid
                    + " when there was no existing mapping. Ignoring, this should not happen unless we failed earlier with a fetch of real message.");
        } else {
            int collapsed = this.collapseFlags(flags);
            if (mapping != null && mapping.getVmaflags() != collapsed) {
                mapper.updateMessageIdAndFlags(mapping.getId(), mapping.getMsgid(), collapsed);
                mapping.setVmaflags(collapsed);
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("vmaReceiveFlags: return mapping=" + mapping);
        }
        return mapping;
    }

    @Override
    public synchronized VMAMapping checksumBuilderAddSMS(long luid, long threadId, String body, String mdn,
            long timestamp, int messageBox) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("checksumBuilderAddSMS: called for : luid=" + luid);
        }
        VMAMapping mapping = getOrCreateSMSMappingForTelephony(luid, threadId, body, mdn, timestamp,
                VMAMapping.SOURCECREATEDFROM_CKSUMMAPPER, messageBox);
        if (mapping.getLuid() == 0) {
            pairExistingMappingWithTelephony(mapping, luid, threadId);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("checksumBuilderAddSMS: return mapping=" + mapping);
        }
        return mapping;
    }

    @Override
    public synchronized VMAMapping hasExistingMapping(long uid) {
        return mapper.findMappingByUid(uid);
    }

    @Override
    public VMAMapping hasExistingMapping(long uid, String msgId, int msgType) {
        VMAMapping mapping = mapper.findMappingByUid(uid);
        if (mapping == null) {
            mapping = mapper.findMappingByMessageId(msgId, msgType);
        }
        return mapping;
    }

    @Override
    public synchronized VMAMapping uiSMSDelete(long luid) {
        return uiSMSDeleteCommon(luid, VMAMapping.TYPE_SMS);
    }

    @Override
    public synchronized VMAMapping uiSMSRead(long luid) {
        return uiSMSReadCommon(luid, VMAMapping.TYPE_SMS);
    }

    @Override
    public synchronized VMAMapping uiMMSDelete(long luid) {
        return uiSMSDeleteCommon(luid, VMAMapping.TYPE_MMS);
    }

    @Override
    public synchronized VMAMapping uiMMSRead(long luid) {
        return uiSMSReadCommon(luid, VMAMapping.TYPE_MMS);
    }

    @Override
    public synchronized void conversationRead(long threadId) {
        List<VMAMapping> mappings = mapper.findMappingByPduThreadId(threadId);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("conversationRead: msg found=" + ((mappings != null) ? mappings.size() : 0));
        }
        if (mappings != null) {
            for (VMAMapping vmaMapping : mappings) {
                if (vmaMapping.getLuid() > 0) {
                    if (vmaMapping.isSMS()) {
                        uiSMSRead(vmaMapping.getLuid());
                    } else {
                        uiMMSRead(vmaMapping.getLuid());
                    }
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.warn("Luid not found. mapping=" + vmaMapping);
                    }
                }
            }
        }
    }

    @Override
    public synchronized void conversationDelete(long threadId) {
        List<VMAMapping> mappings = mapper.findMappingByPduThreadId(threadId);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("conversationDelete: msg found=" + ((mappings != null) ? mappings.size() : 0));
        }
        if (mappings != null) {
            for (VMAMapping vmaMapping : mappings) {
                if (vmaMapping.getLuid() > 0) {
                    if (vmaMapping.isSMS()) {
                        uiSMSDelete(vmaMapping.getLuid());
                    } else {
                        uiMMSDelete(vmaMapping.getLuid());
                    }
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.warn("Luid not found. mapping=" + vmaMapping);
                    }
                }
            }
        }
    }

    @Override
    public void deleteMapping(long id) {
        mapper.deleteMapping(id);
    }

}
