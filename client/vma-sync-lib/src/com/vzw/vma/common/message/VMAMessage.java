package com.vzw.vma.common.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

//import org.apache.log4j.Logger;


public class VMAMessage implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 9085816487732871711L;

	//private static Logger		logger = Logger.getLogger(VMAMessage.class);

	public final static int STATUS_SUBMITTED = 0;
	public final static int STATUS_DELIVERED = 1;
	public final static int STATUS_FAILED = 2;

	private String uid;
	private String messageId;
	private String mdn;
	private String sourceAddr;
	private ArrayList<String> destAddrs = new ArrayList<String>(); 
	private ArrayList<VMAAttachment> attachments = new ArrayList<VMAAttachment>(); 
	private ArrayList<String> toAddrs;
	private ArrayList<String> ccAddrs;
	private ArrayList<String> bccAddrs;
	private ArrayList<String> fromList;
	private Date messageTime;
	private String messageSubject;
	private String messageText;
	private MessagePriority priority;
	private long messageSize;
	private boolean groupMessage;
	private MessageSourceEnum messageSource;
	private String XmsgId;
	private String debitAmount;
	private String contentType;
	private String conversationId;
	private boolean flagSent;
	private boolean flagDeleted;
	private boolean flagSeen;
//	private Date date;
	private long localThreadId;
	private long luid;
	
	
	private MessageTypeEnum messageType;
	private VMAMessageStatus messageStatus;
	protected int retryCount=0;
	protected String receivedQueue;

    private int localMsgBox;

    private String localParticipantAddress;

	
	public boolean isFlagSent() {
		return flagSent;
	}
	public void setFlagSent(boolean flagSent) {
		this.flagSent = flagSent;
	}
	public boolean isFlagDeleted() {
		return flagDeleted;
	}
	public void setFlagDeleted(boolean flagDeleted) {
		this.flagDeleted = flagDeleted;
	}
	public boolean isFlagSeen() {
		return flagSeen;
	}
	public void setFlagSeen(boolean flagSeen) {
		this.flagSeen = flagSeen;
	}
	
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public String getXmsgId() {
		return XmsgId;
	}
	public void setXmsgId(String xmsgId) {
		XmsgId = xmsgId;
	}
	public MessageSourceEnum getMessageSource() {
		return messageSource;
	}
	public void setMessageSource(MessageSourceEnum messageSource) {
		this.messageSource = messageSource;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public String getMdn() {
		return mdn;
	}
	public void setMdn(String mdn) {
		this.mdn = mdn;
	}
	public String getSourceAddr() {
		return sourceAddr;
	}
	public void setSourceAddr(String sourceAddr) {
		this.sourceAddr = sourceAddr;
	}
	public ArrayList<String> getDestAddrs() {
		return destAddrs;
	}
	public void setDestAddrs(ArrayList<String> destAddrs) {
		this.destAddrs = destAddrs;
	}
	
	public Date getMessageTime() {
		return messageTime;
	}
	
	public void setMessageTime(Date messageTime) {
		this.messageTime = messageTime;
	}

	public String getMessageSubject() {
		return messageSubject;
	}
	public void setMessageSubject(String messageSubject) {
		this.messageSubject = messageSubject;
	}
	public String getMessageText() {
		return messageText;
	}
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}
	public MessagePriority getPriority() {
		return priority;
	}
	public void setPriority(MessagePriority priority) {
		this.priority = priority;
	}
	public long getMessageSize() {
		return messageSize;
	}
	public void setMessageSize(long messageSize) {
		this.messageSize = messageSize;
	}
	public boolean isGroupMessage() {
		return groupMessage;
	}
	public void setGroupMessage(boolean groupMessage) {
		this.groupMessage = groupMessage;
	}
	public ArrayList<VMAAttachment> getAttachments() {
		return attachments;
	}
	public void setAttachments(ArrayList<VMAAttachment> attachments) {
		this.attachments = attachments;
	}
	public MessageTypeEnum getMessageType() {
		return messageType;
	}
	public void setMessageType(MessageTypeEnum messageType) {
		this.messageType = messageType;
	}
	public VMAMessageStatus getMessageStatus() {
		return messageStatus;
	}
	public void setMessageStatus(VMAMessageStatus messageStatus) {
		this.messageStatus = messageStatus;
	}
	public static int getStatusSubmitted() {
		return STATUS_SUBMITTED;
	}
	public static int getStatusDelivered() {
		return STATUS_DELIVERED;
	}
	public static int getStatusFailed() {
		return STATUS_FAILED;
	}
	@Override
	public String toString() {
		return "VMAMessage [XmsgId=" + XmsgId + ", attachments=" + attachments + ", bccAddrs=" + bccAddrs + ", ccAddrs=" + ccAddrs + ", destAddrs=" + destAddrs + ", groupMessage=" + groupMessage
				+ ", mdn=" + mdn + ", messageId=" + messageId + ", messageSize=" + messageSize + ", messageSource=" + messageSource + ", messageStatus=" + messageStatus + ", messageSubject="
				+ messageSubject + ", messageText=" + messageText + ", messageTime=" + messageTime + ", messageType=" + messageType + ", priority=" + priority + ", receivedQueue=" + receivedQueue
				+ ", retryCount=" + retryCount + ", sourceAddr=" + sourceAddr + ", toAddrs=" + toAddrs + ", uid=" + uid + "]";
	}
		
	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public String getReceivedQueue() {
		return receivedQueue;
	}

	public void setReceivedQueue(String receivedQueue) {
		this.receivedQueue = receivedQueue;
	}
	
	public ArrayList<String> getToAddrs() {
		return toAddrs;
	}
	
	public void setToAddrs(ArrayList<String> toAddrs) {
		this.toAddrs = toAddrs;
		for (String addr : toAddrs) {
			addDest(addr);
		}
	}
	
	public ArrayList<String> getCcAddrs() {
		return ccAddrs;
	}
	
	public void setCcAddrs(ArrayList<String> ccAddrs) {
		this.ccAddrs = ccAddrs;
		for (String addr : ccAddrs) {
			addDest(addr);
		}
	}
	
	public ArrayList<String> getBccAddrs() {
		return bccAddrs;
	}
	
	public void setBccAddrs(ArrayList<String> bccAddrs) {
		this.bccAddrs = bccAddrs;
		for (String addr : bccAddrs) {
			addDest(addr);
		}
	}

	private void addDest(String addr) {
		if (!destAddrs.contains(addr)) {
			destAddrs.add(addr);
		}
	}
	
	public String getDebitAmount() {
		return debitAmount;
	}
	
	public void setDebitAmount(String debitAmount) {
		this.debitAmount = debitAmount;
	}
	
	public void setConversationId(String convId) {
		conversationId = convId;
	}

	public String getConversationId() {
		return conversationId;
	}
	
	public boolean isMMS(){
		return getMessageType().equals(MessageTypeEnum.MMS);
	}

	public boolean isSMS(){
		return !isMMS();
	}
//	@Deprecated
//	private void setDate(Date d) {
//		date = d;
//	}
//	@Deprecated
//	private Date getDate() {
//		return date;
//	}
	public void setLocalThreadId(long threadId) {
		localThreadId = threadId;	
	}
	
	public long getLocalThreadId() {
		return localThreadId;	
	}
	/**
     * Returns the Value of the luid
     * @return the  {@link long}
     */
    public long getLuid() {
        return luid;
    }
    /**
     * Set the Value of the field luid
     *
     * @param luid the luid to set
     */
    public void setLuid(long luid) {
        this.luid = luid;
    }
    public void setFromList(ArrayList<String> fromList) {
		this.fromList = fromList;		
	}
	
	public ArrayList<String> getFromList () {
		return fromList;
		
	}
    /**
     * This Method 
     * @param msgBox
     */
    public void setLocalMessageBox(int msgBox) {
            localMsgBox=msgBox;
    }
    /**
     * This Method 
     * @param msgBox
     */
    public int getLocalMessageBox() {
        return localMsgBox;
    }
    /**
     * This Method 
     * @param string
     */
    public void setLocalParticipantId(String address) {
        localParticipantAddress=address;
    }
    /**
     * This Method 
     * @param string
     */
    public String getLocalParticipantId() {
        return localParticipantAddress;
    }
   
	
}
