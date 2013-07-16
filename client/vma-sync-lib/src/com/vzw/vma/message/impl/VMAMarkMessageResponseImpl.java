package com.vzw.vma.message.impl;

import com.vzw.vma.message.VMAMarkMessageResponse;

public class VMAMarkMessageResponseImpl implements VMAMarkMessageResponse {

	long uid;
	long pMcr;;
	boolean isSeen;
	boolean isSent;
	boolean isDeleted;
	
	public VMAMarkMessageResponseImpl(long uid, long pMCR, boolean seen, boolean deleted, boolean sent) {
		this.uid = uid;
		this.pMcr = pMCR;
		isSeen = seen;
		isDeleted = deleted;
		isSent = sent;
	}
	@Override
	public long getUID() {
		return uid;
	}

	@Override
	public long getModSeq() {
		return pMcr;
	}

	@Override
	public boolean isSeen() {
		return isSeen;
	}

	@Override
	public boolean isDeleted() {
		return isDeleted;
	}

	@Override
	public boolean isSent() {
		return isSent;
	}
	
	@Override
	public String toString() {
		return "VMAMarkMessageResponseImpl: uid=" + uid + " pMCR=" + pMcr + " seen=" + isSeen + " deleted=" + isDeleted + " sent=" + isSent;
	}

}
