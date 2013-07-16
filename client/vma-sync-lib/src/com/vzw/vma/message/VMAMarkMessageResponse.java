package com.vzw.vma.message;

public interface VMAMarkMessageResponse  extends VMAResponse {

	public long getUID();
	
	public long getModSeq();
		
    public boolean isSeen();
    
    public boolean isDeleted();
    
    public boolean isSent();
	
}
