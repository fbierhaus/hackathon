package com.vzw.vma.sync.refactor;

import com.verizon.messaging.vzmsgs.provider.VMAMapping;

public interface VMATelephonyEventHandler {
	
	public VMAMapping telephonyMMSReceive(long luid, long threadId, String messageId, long timestamp);
	
	public VMAMapping telephonyMMSSend(long luid, long threadId, String messageId, long timestamp);
	
	public VMAMapping telephonySMSReceive(long luid, long threadId, String body, String mdn, long smscGatewayTime , long msgSavedTime);
	
	public VMAMapping telephonySMSSend(long luid, long threadId, String body, String mdn, long timestamp);
	
	public VMAMapping telephonySMSReceive(long luid, long threadId, String body, String mdn, long timestamp);
}
