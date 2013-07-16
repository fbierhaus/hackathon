package com.vzw.vma.message;

public interface VmaConnection {

	public String getConnectionId();
	
	public VmaSelectResponse getInboxSelect();
	
}
