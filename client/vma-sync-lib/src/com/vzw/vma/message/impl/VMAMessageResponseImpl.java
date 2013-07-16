package com.vzw.vma.message.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vzw.vma.common.message.VMAMessage;
import com.vzw.vma.message.VMAFlags;
import com.vzw.vma.message.VMAMessageResponse;

public class VMAMessageResponseImpl implements VMAMessageResponse {

	protected VMAMessage msam;
	protected List<VMAFlags> flags = new ArrayList<VMAFlags>();
	protected long uid;
	protected long pMCR;
	protected long sMCR;
	protected boolean seen;
	protected boolean deleted;
	protected boolean sent;
	protected HashMap<String, String> deliveryReports = new HashMap<String, String>();
	
//	@Deprecated
//	public VMAMessageResponseImpl(VMAMessage msam, List<VMAFlags> flags, long uid, long modSeq, HashMap<String, String> reports) {
//		this.flags = flags;
//		this.uid = uid;
////		this.modSeq = modSeq;
//		this.msam = msam;
//		if(flags != null) {
//			for(VMAFlags flag : flags) {
//				if(flag == VMAFlags.DELETED) {
//					deleted = true;
//				} else if (flag == VMAFlags.SEEN) {
//					seen = true;
//				} else if (flag == VMAFlags.SENT) {
//					sent = true;
//				} 
//			}
//		}
//		if(reports != null) {
//			deliveryReports = reports;
//		}
//	}
	
	public VMAMessageResponseImpl(VMAMessage msam, List<VMAFlags> flags, long uid, long pMcr,long sMcr, HashMap<String, String> reports) {
		this.flags = flags;
		this.uid = uid;
		this.pMCR=pMcr;
		this.sMCR=sMcr;
		this.msam = msam;
		if(flags != null) {
			for(VMAFlags flag : flags) {
				if(flag == VMAFlags.DELETED) {
					deleted = true;
				} else if (flag == VMAFlags.SEEN) {
					seen = true;
				} else if (flag == VMAFlags.SENT) {
					sent = true;
				} 
			}
		}
		if(reports != null) {
			deliveryReports = reports;
		}
	}
	


	public VMAMessage getVmaMessage() {
		return msam;
	}
	
	@Override
	public List<VMAFlags> getFlags() {
		return flags;
	}

	@Override
	public long getUID() {
		return uid;
	}
	

	public String toString() {
		return "VMAMessageResponse: flags=" + flags + " uid=" + uid +" pMCR="+pMCR +" sMCR="+sMCR +" subject=" + msam.getMessageSubject() ; 
	}

	@Override
	public boolean isSeen() {

		return seen;
	}

	@Override
	public boolean isDeleted() {
		return deleted;
	}

	@Override
	public boolean isSent() {
		return sent;
	}



	@Override
	public Map<String, String> getDeliveryReports() {
		return deliveryReports;
	}



	@Override
	public long getPrimaryMCR() {
		return pMCR;
	}



	@Override
	public long getSecondaryMCR() {
		return sMCR;
	}   
}
