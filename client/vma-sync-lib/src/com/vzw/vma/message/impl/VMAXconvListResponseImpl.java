package com.vzw.vma.message.impl;

import com.vzw.vma.message.VMAXconvListResponse;

public class VMAXconvListResponseImpl implements VMAXconvListResponse {

	protected int unreadCount;
	protected String conversationThreadId;
	protected long highestUid;
	
	public VMAXconvListResponseImpl(String conversationThreadId, long highestUid, int unreadCount) {
		this.highestUid = highestUid;
		this.unreadCount = unreadCount;
		this.conversationThreadId = conversationThreadId;
	}
	
	@Override
	public String getConversationThreadId() {
		// TODO Auto-generated method stub
		return conversationThreadId;
	}

	@Override
	public long getHighestUid() {
		// TODO Auto-generated method stub
		return highestUid;
	}

	@Override
	public int getUnreadCount() {
		// TODO Auto-generated method stub
		return unreadCount;
	}

	public String toString() {
		return "XCONV LIST: conversationThreadId=" + conversationThreadId + " highestUid=" + highestUid + " unreadCount=" + unreadCount; 
	}
}
