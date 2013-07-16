package com.vzw.vma.sync.refactor;

import com.verizon.messaging.vzmsgs.provider.VMAMapping;

public interface VMAUiEventHandler {
	
	public VMAMapping uiSMSDelete(long luid);
	
	public VMAMapping uiSMSRead(long luid);
	
	public VMAMapping uiMMSDelete(long luid);
	
	public VMAMapping uiMMSRead(long luid);
	
	public void conversationRead(long threadId);
	
	public void conversationDelete(long threadId);
	
}
