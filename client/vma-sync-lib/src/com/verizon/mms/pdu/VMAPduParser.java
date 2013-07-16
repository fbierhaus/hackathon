package com.verizon.mms.pdu;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.ContentType;
import com.verizon.mms.InvalidHeaderValueException;
import com.verizon.mms.MmsException;
import com.verizon.sync.ConversationDataObserver;
import com.vzw.vma.common.message.VMAAttachment;
import com.vzw.vma.common.message.VMAMessage;

public class VMAPduParser {

    private PduHeaders headers = new PduHeaders();
    private PduBody body = new PduBody();
    private VMAMessage vma;
    private Context context;
    private boolean hasAttachements;
    private String localMdn;
    private long threadId;

    /**
     * Returns the Value of the hasAttachements
     * 
     * @return the {@link boolean}
     */
    public boolean hasAttachements() {
        return hasAttachements;
    }

    public VMAPduParser(Context con, VMAMessage msg, String localMdn) {
        vma = msg;
        context = con;
        this.localMdn = localMdn;
    }

    public Uri savePdu() throws MmsException {

        saveHeaders();
        saveBody();
        try {
            PduPersister persister = PduPersister.getPduPersister(context);
            Uri res = null;
            if (vma.isFlagSent()) {
                SendReq pdu = new SendReq(headers, body);
                pdu.setMessageType(PduHeaders.MESSAGE_TYPE_SEND_REQ);
                // log(pdu);
                res = persister.persist(pdu, VZUris.getMmsSentUri(), true, localMdn);
                threadId = pdu.getThreadId();
                
                ConversationDataObserver.onNewMessageAdded(threadId, ContentUris.parseId(res), ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
                return res;
            } else {
                RetrieveConf pdu = new RetrieveConf(headers, body);
                pdu.setMessageType(PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF);
                // log(pdu);
                res = persister.persist(pdu, VZUris.getMmsInboxUri(), vma.isFlagSeen(), localMdn);
                threadId = pdu.getThreadId();
                
                ConversationDataObserver.onNewMessageAdded(threadId, ContentUris.parseId(res), ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
                return res;
            }
        } catch (MmsException e) {
            throw e;
        }

    }

    // /**
    // * This Method
    // *
    // * @param pdu
    // */
    // private void log(MultimediaMessagePdu pdu) {
    // Logger.info("Saving MMS : subject=" + pdu.getSubject());
    // Logger.info("Saving MMS : partcount=" + pdu.getBody().getPartsNum());
    // Logger.info("Saving MMS : bodytext=" + pdu.getBody().getPartsNum());
    // Logger.info("Saving MMS : sent=" + vma.isFlagSent());
    // }

    public void saveBody() {
        // Saving Message body
        Logger.info("Text body=" + vma.getMessageText());
        if (vma.getMessageText() != null) {

            // SANDY , this should be a vma attachment
            PduPart mmsBodyText = new PduPart();
            mmsBodyText.setData(vma.getMessageText().getBytes());
            mmsBodyText.setCharset(CharacterSets.UTF_8);
            mmsBodyText.setContentType(ContentType.TEXT_PLAIN.getBytes());
            mmsBodyText.setContentLocation("text.txt".getBytes());
            mmsBodyText.setContentDisposition("text.txt".getBytes());
            mmsBodyText.setFilename("text.txt".getBytes());
            mmsBodyText.setName("text".getBytes());
            mmsBodyText.setContentId("text.txt".getBytes());
            body.addPart(mmsBodyText);

            // PduPart smilDoc = new PduPart();
            // smilDoc.setData(body.toSMILPart().getBytes());
            // smilDoc.setCharset(CharacterSets.UTF_8);
            // smilDoc.setContentType(ContentType.APP_SMIL.getBytes());
            // smilDoc.setContentLocation("smil.xml".getBytes());
            // smilDoc.setContentDisposition("smil.xml".getBytes());
            // smilDoc.setFilename("smil.xml".getBytes());
            // smilDoc.setName("smil.xml".getBytes());
            // smilDoc.setContentId("smil.xml".getBytes());
        }
        ArrayList<VMAAttachment> attachments = vma.getAttachments();
        if (attachments != null) {
            for (VMAAttachment attachment : attachments) {
                savePart(attachment);
            }
        }
    }

    public void saveHeaders() throws InvalidHeaderValueException {
        // PDU Columns
        // _id|thread_id|date|msg_box|read|m_id|sub|sub_cs|ct_t|ct_l|exp|m_cls|m_type|v|m_size|pri|rr|rpt_a|resp_st|st|tr_id|retr_st|retr_txt|retr_txt_cs|read_status|ct_cls|resp_txt|d_tm|d_rpt|locked|seen|sort_index
        // 46|2|1348654772|4|1||Hhggjjjkkjhg|106|application/vnd.wap.multipart.related||604800|personal|128|18|8|129|129||||T13a02168400||||||||128|0|1|1348654728956

        // date
        if (vma.getMessageTime() != null) {
            headers.setLongInteger(vma.getMessageTime().getTime() / 1000, PduHeaders.DATE);
        }
        // PDU Table Mandatory headers
        // default
        headers.setOctet(PduHeaders.CURRENT_MMS_VERSION, PduHeaders.MMS_VERSION);

        // Message Type == received MMS
        // m_type
        // if(msaMessage.isSent()){
        // headers.setOctet(PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF, PduHeaders.MESSAGE_TYPE);
        // }else{
        // headers.setOctet(PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF, PduHeaders.MESSAGE_TYPE);
        // }

        // m_id
        if (vma.getMessageId() != null) {
            headers.setTextString(vma.getMessageId().getBytes(), PduHeaders.MESSAGE_ID);
            headers.setTextString(vma.getMessageId().getBytes(), PduHeaders.TRANSACTION_ID);
        }
        // ct_t /application/vnd.wap.multipart.related
        headers.setTextString(ContentType.MULTIPART_MIXED.getBytes(), PduHeaders.CONTENT_TYPE);

        // Reaad

        String subject = vma.getMessageSubject();
        if (!TextUtils.isEmpty(subject)) {
            // saveAsEncodedString("utf-8", subject, PduHeaders.SUBJECT);
            headers.setEncodedStringValue(new EncodedStringValue(subject), PduHeaders.SUBJECT);

        }

        // m_cls
        // response
        headers.setOctet(PduHeaders.RESPONSE_STATUS_OK, PduHeaders.RESPONSE_STATUS);

        // st
        headers.setOctet(PduHeaders.STATUS_RETRIEVED, PduHeaders.STATUS);
        //
        headers.setLongInteger(0, PduHeaders.MESSAGE_SIZE);

        // ADDR table entry
        // FROM address entry
        if (vma.isFlagSent()) {
            // UI will take the
            EncodedStringValue from = new EncodedStringValue(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR);
            headers.setEncodedStringValue(from, PduHeaders.FROM);
        } else {
            EncodedStringValue from = new EncodedStringValue(vma.getSourceAddr());
            headers.setEncodedStringValue(from, PduHeaders.FROM);

            // TO address will be the logged MDN.
        }

        // TO Address Entry
        // TO address
        ArrayList<String> toAddress = vma.getToAddrs();
        if (toAddress != null) {
            for (String to : toAddress) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("MMS TO address :" + to);
                }
                EncodedStringValue value = new EncodedStringValue(to);
                headers.appendEncodedStringValue(value, PduHeaders.TO);
            }
        }

        // CC address
        ArrayList<String> ccAddress = vma.getCcAddrs();
        if (ccAddress != null) {
            for (String cc : ccAddress) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("MMS CC address :" + cc);
                }
                EncodedStringValue value = new EncodedStringValue(cc);
                headers.appendEncodedStringValue(value, PduHeaders.CC);
            }
        }
        // BCC address
        ArrayList<String> bccAddress = vma.getBccAddrs();
        if (bccAddress != null) {
            for (String bcc : bccAddress) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("MMS BCC address :" + bcc);
                }
                EncodedStringValue value = new EncodedStringValue(bcc);
                headers.appendEncodedStringValue(value, PduHeaders.BCC);
            }
        }
    }

    protected int getCharset(String charSet) {
        /* get charset parameter */
        int charset = -1;
        if (charSet != null) {
            try {
                charset = CharacterSets.getMibEnumValue(charSet);
            } catch (UnsupportedEncodingException e) {
            }
        }
        return charset;
    }

    public void savePart(VMAAttachment attachment) {
        // The attachment data will be saved by MSA message
        // com.vzw.vma.common.message.MSAMessage.saveAttachment(BodyPart) . here we are going update
        // attachment dummy Id to real messageID
        //
        PduPart part = new PduPart();
        part.setTempPartUri(attachment.getTempPartUri());
        body.addPart(part);
        // saving only thumb image.
        hasAttachements = true;
    }

    public void saveAsEncodedString(String charSet, String textString, int field) {
        EncodedStringValue returnValue = null;
        int charset = getCharset(charSet);
        if (charset != -1) {
            returnValue = new EncodedStringValue(charset, textString.getBytes());
        } else {
            returnValue = new EncodedStringValue(textString.getBytes());
        }
        headers.appendEncodedStringValue(returnValue, field);
    }

    /**
     * Returns the Value of the threadId
     * 
     * @return the {@link long}
     */
    public long getThreadId() {
        return threadId;
    }

}
