package com.vzw.vma.message;

public interface VmaSelectResponse extends VMAResponse{
	
	public long getUidValidity();
	
	public long getLastUid();
//	@Deprecated
//	public long getHighestModSeq();
	
	public long getUnreadCount();

	public long getIdleTimeout();
	
	public long getAutoReplyChangeCount();
	
	public long getAutoForwardChangeCount();
	
	public long getPrimaryHighestMCR();
	
	public long getSecondaryHighestMCR();
	
	
}
