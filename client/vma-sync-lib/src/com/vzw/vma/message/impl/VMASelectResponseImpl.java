package com.vzw.vma.message.impl;

import com.vzw.vma.message.VmaSelectResponse;

public class VMASelectResponseImpl implements VmaSelectResponse {

	protected long uidValidity;
	protected long lastUid;
//	protected long highestModSeq;
	protected long unreadCount;
	protected long idleTimeout;
	protected long autoReplyChangeCount;
	protected long autoForwardChangeCount;

	protected long highestPMcr;
	protected long highestSMcr;
//    @Deprecated
//	public VMASelectResponseImpl(long valid, long uid, long seq, long unr, long idle, long ar, long af) {
//		uidValidity = valid;
//		lastUid = uid;
//		highestModSeq = seq;
//		unreadCount = unr;
//		idleTimeout = idle;
//		autoReplyChangeCount = ar;
//		autoForwardChangeCount = af;
//	}
	
	
	public VMASelectResponseImpl(long valid, long uid, long pMcr,long sMcr, long unr, long idle, long ar, long af) {
		uidValidity = valid;
		lastUid = uid;
		unreadCount = unr;
		idleTimeout = idle;
		autoReplyChangeCount = ar;
		autoForwardChangeCount = af;
		highestPMcr=pMcr;
		highestSMcr=sMcr;
	}
	
	@Override
	public long getUidValidity() {
		return uidValidity;
	}

	@Override
	public long getLastUid() {
		return lastUid;
	}

//	@Override
//	public long getHighestModSeq() {
//		return highestModSeq;
//	}

	@Override
	public long getUnreadCount() {
		return unreadCount;
	}

	@Override
	public long getIdleTimeout() {
		return idleTimeout;
	}
	
	public String toString() {
		return "idleTimeout=" + idleTimeout + " UidValidity=" + uidValidity + " lastUid=" + lastUid + " highest pMcr=" + highestPMcr+" sMcr="+ highestSMcr + " unreadCount=" + unreadCount + " af=" + autoForwardChangeCount + " ar=" + autoReplyChangeCount;
	}

	@Override
	public long getAutoReplyChangeCount() {
		// TODO Auto-generated method stub
		return autoReplyChangeCount;
	}

	@Override
	public long getAutoForwardChangeCount() {
		// TODO Auto-generated method stub
		return autoForwardChangeCount;
	}

	@Override
	public long getPrimaryHighestMCR() {
		return highestPMcr;
	}

	@Override
	public long getSecondaryHighestMCR() {
		return highestSMcr;
	}
}
