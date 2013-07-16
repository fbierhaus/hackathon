package com.vzw.vma.message.impl;

import com.vzw.vma.message.VMAChangedSinceResponse;

public class VMAChangedSinceResponseImpl implements VMAChangedSinceResponse {

	protected long uid;
//	protected long modSeq;
	protected String vmaId;
	protected long pMCR;
	protected long sMCR;
	
//	@Deprecated
//	 public VMAChangedSinceResponseImpl(long uid, long modSeq, String vmaId) {
//		this.uid = uid;
//		this.modSeq = modSeq;
//		this.vmaId = vmaId;
//	}
	
	public VMAChangedSinceResponseImpl(long uid, long pMCR,long sMCR, String vmaId) {
		this.uid = uid;
		this.pMCR=pMCR;
		this.sMCR=sMCR;
		this.vmaId = vmaId;
	}
	
	@Override
	public long getUID() {
		return uid;
	}

//	@Override @Deprecated
//	public long getModSeq() {
//		return modSeq;
//	}

	@Override
	public String getVMAId() {
		return vmaId;
	}

	public String toString() {
		return "VMAChangedSinceResponseImpl: uid=" + uid +" vmaId=" + vmaId+" pMcr="+pMCR+" sMCR="+sMCR;
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
