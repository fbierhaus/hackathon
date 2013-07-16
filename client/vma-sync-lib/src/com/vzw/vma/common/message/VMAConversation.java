package com.vzw.vma.common.message;

import java.util.Date;

//import org.apache.log4j.Logger;


public class VMAConversation {
	//private static Logger		logger = Logger.getLogger(VMAConversation.class);

	private String mdn;
	private String participants;
	private int numberOfMessages;
	private int numberOfUnreadMessages;
	private String lastMessageText;
	private Date lastMessageTime; 
	private String lastMessageUid;
	private boolean failedMessage;
	

	public String getLastMessageUid() {
		return lastMessageUid;
	}
	public void setLastMessageUid(String lastMessageUid) {
		this.lastMessageUid = lastMessageUid;
	}
	public String getMdn() {
		return mdn;
	}
	public void setMdn(String mdn) {
		this.mdn = mdn;
	}
	public String getParticipants() {
		return participants;
	}
	public void setParticipants(String participants) {
		this.participants = participants;
	}
	public int getNumberOfMessages() {
		return numberOfMessages;
	}
	public void setNumberOfMessages(int numberOfMessages) {
		this.numberOfMessages = numberOfMessages;
	}
	public int getNumberOfUnreadMessages() {
		return numberOfUnreadMessages;
	}
	public void setNumberOfUnreadMessages(int numberOfUnreadMessages) {
		this.numberOfUnreadMessages = numberOfUnreadMessages;
	}
	public String getLastMessageText() {
		return lastMessageText;
	}
	public void setLastMessageText(String lastMessageText) {
		this.lastMessageText = lastMessageText;
	}

	public Date getLastMessageTime() {
		return lastMessageTime;
	}

	public void setLastMessageTime(Date lastMessageTime) {
		this.lastMessageTime = lastMessageTime;
	}
	

	
	

	public boolean isFailedMessage() {
		return failedMessage;
	}
	public void setFailedMessage(boolean failedMessage) {
		this.failedMessage = failedMessage;
	}
	@Override
	public String toString() {
		return "WebConversation [failedMessage=" + failedMessage + ", lastMessageText=" + lastMessageText + ", lastMessageTime=" + lastMessageTime + ", numberOfMessages=" + numberOfMessages
				+ ", numberOfUnreadMessages=" + numberOfUnreadMessages + ", participants=" + participants + "]";
	}
	
	
}
