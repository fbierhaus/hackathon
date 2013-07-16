package com.vzw.vma.message;

public interface VMAXconvListResponse extends VMAResponse {

	public String getConversationThreadId();

	public long getHighestUid();

	public int getUnreadCount();
	

}
