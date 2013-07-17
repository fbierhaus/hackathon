package com.vzw.hackathon;

public class Member extends User {
	private MemberStatus	status = null;
	private String			lastChannelId = null;
	
	

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
	@Override
	public String toString() {
		return "Member [status=" + status + ", lastChannelId=" + lastChannelId
				+ ", getMdn()=" + getMdn() + ", getChannelId()="
				+ getChannelId() + ", getName()=" + getName() + "]";
	}


	
}
