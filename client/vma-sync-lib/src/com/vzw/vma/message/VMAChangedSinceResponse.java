package com.vzw.vma.message;

public interface VMAChangedSinceResponse extends VMAResponse {

	public long getUID();
	
//	@Deprecated
//	public long getModSeq();
	
	public String getVMAId();
	
	
	// MSA FAIL OVER changes 
	
	public long getPrimaryMCR();
	
	public long getSecondaryMCR();
	
}
