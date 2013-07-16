package com.vzw.vma.message.impl;

import com.vzw.vma.message.VMAXconvFetchResponse;

public class VMAXconvFetchResponseImpl implements VMAXconvFetchResponse {

	protected String conversationThreadId;
	protected long uid;
	protected long modSeq;
	
	public VMAXconvFetchResponseImpl(String conversationThreadId, long uid, long modSeq) {
		this.conversationThreadId = conversationThreadId;
		this.uid = uid;
		this.modSeq = modSeq;
	}
	
	@Override
	public String getConversationThreadId() {
		return conversationThreadId;
	}

	@Override
	public long getUid() {
		return uid;
	}

	@Override
	public long getModseq() {
		return modSeq;
	}

	public String toString() {
		return "XCONV FETCH: conversationThreadId=" + conversationThreadId + " uid=" + uid + " modSeq=" + modSeq;
	}
}
