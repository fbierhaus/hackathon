package com.vzw.hackathon;

public class Member extends User {
	private MemberStatus	status = null;
	
	
	

	public MemberStatus getStatus() {
		return status;
	}
	public void setStatus(MemberStatus status) {
		this.status = status;
	}
	@Override
	public String toString() {
		return "Member [status=" + status + ", getMdn()=" + getMdn()
				+ ", getChannelId()=" + getChannelId() + ", getName()="
				+ getName() + ", getLastChannelId()=" + getLastChannelId()
				+ "]";
	}




	
}
