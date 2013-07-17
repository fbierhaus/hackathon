package com.vzw.hackathon;

public class Member {
	private String			mdn = null;
	private MemberStatus	status = null;
	private String			lastChannelId = null;
	
	public String getMdn() {
		return mdn;
	}
	public void setMdn(String mdn) {
		this.mdn = mdn;
	}
	public MemberStatus getStatus() {
		return status;
	}
	public void setStatus(MemberStatus status) {
		this.status = status;
	}

	public String getLastChannelId() {
		return lastChannelId;
	}
	public void setLastChannelId(String lastChannelId) {
		this.lastChannelId = lastChannelId;
	}
	

	
}
