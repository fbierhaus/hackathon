package com.verizon.messaging.vzmsgs.provider.dao;

import java.util.HashMap;
import java.util.List;

import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.vzw.vma.sync.refactor.VMATelephonyEventHandler;
import com.vzw.vma.sync.refactor.VMAUiEventHandler;

public interface VMAEventHandler extends VMATelephonyEventHandler, VMAUiEventHandler {
	

	public enum Source {
		WEB(0, "WEB"),
		IMAP(1, "IMAP"),
		PHONE(2, "PHONE");
		
		private int code;
		private String desc;
		
		private Source(int code, String desc){
			this.code = code;
			this.desc = desc;
		}

		public int getCode(){
			return code;
		}

		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}		
	}
	
	public enum Flags {
		
		NONE(0, "NONE"),
		SENT(1, "SENT"),
		READ(2, "READ"),
		DELETE(4, "DELETE");
		
		private int code;
		private String desc;
		
		private Flags(int code, String desc){
			this.code = code;
			this.desc = desc;
		}

		public int getCode(){
			return code;
		}

		public String getDesc() {
			return desc;
		}

		public void setDesc(String desc) {
			this.desc = desc;
		}		
	}
	
	public VMAMapping vmaReceiveSMSHandset(long luid, long threadId, long uid, String body, String mdn, String msgId,  long timestamp, Source src, List<Flags> flags, int messageBox );
	
	public VMAMapping vmaReceiveMMSHandset(long luid, long threadId, long uid,  String messageId, long timestamp, Source src, List<Flags> flags, int messageBox);

	public VMAMapping vmaSendSMS(long luid, long threadId, String body, String mdn, long timestamp, String msgId);
	
	public VMAMapping vmaReceiveMMSTablet(long luid, long threadId, long uid, String messageId, long timestamp, Source src, List<Flags> flags, int messageBox);
	
	public VMAMapping vmaReceiveSMSTablet(long luid, long threadId, long uid, String messageId, long timestamp, Source src, List<Flags> flags, int messageBox);
	
	
	public VMAMapping vmaSendMMS(long luid, long threadId, String messageId, long timestamp);
	
	/*
	 * Called when we already had a mapping for the message with UID filled in, only need to either 
	 * update Delivery reports or status
	 * 
	 */
	public VMAMapping vmaReceiveFlags(long uid, List<Flags> flags);
	
	public VMAMapping checksumBuilderAddSMS(long luid, long threaId, String body, String mdn, long timestamp,int messageBox);
	
	
	public VMAMapping hasExistingMapping(long uid);
	
	   /*
     * Has a mapping for either uid  or msgid ( in case of a send )
     */ 
    public VMAMapping hasExistingMapping(long uid, String msgId, int msgType);
    
    
    
    public  void  vmaSendUpdateWithUidOnTablet(long id, long uid,  long timestamp, Source src, List<Flags> flags);

    public void deleteMapping(long id);
    
}
