package com.vzw.vma.message;

import java.util.List;
import java.util.Map;

import com.vzw.vma.common.message.VMAMessage;

public interface VMAMessageResponse extends VMAResponse {

	
    public static final int TYPE_SMS=1;
    public static final int TYPE_MMS=2;
       
    public boolean isSeen();
    
    public boolean isDeleted();
    
    public boolean isSent();
    
	public List<VMAFlags> getFlags();
	
	public long getUID();
	
//	@Deprecated
//	public long getModSeq();
	
	public VMAMessage getVmaMessage();
	
	/*
	 * NONE, DELIVERED, FAILED, READ
	 */
	public Map<String, String> getDeliveryReports();
	
	// Changes of MSA FAIL OVER , 2.0  
	
	public long getPrimaryMCR();


	public long getSecondaryMCR();
	
}
