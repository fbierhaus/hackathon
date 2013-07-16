package com.verizon.mms.ui;

public class ConversationSearchItem {

	private long threadId;
	private String snippet;
	private String timeStamp;
	private String msgSource;
	private long msgId;
	public ConversationSearchItem() {
		super();
	}

	public ConversationSearchItem(long threadId,long msgId, String snippet,
			String timeStamp, String msgSource) {
		super();
		this.threadId = threadId;
		this.msgId=msgId;
		this.snippet = snippet;
		this.timeStamp = timeStamp;
		this.msgSource = msgSource;
	}

	public long getThreadId() {
		return threadId;
	}

	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}

	
	public long getMsgId() {
		return msgId;
	}

	public void setMsgId(long msgId) {
		this.msgId = msgId;
	}

	public String getSnippet() {
		return snippet;
	}

	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}

	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getMsgSource() {
		return msgSource;
	}

	public void setMsgSource(String msgSource) {
		this.msgSource = msgSource;
	}

}
