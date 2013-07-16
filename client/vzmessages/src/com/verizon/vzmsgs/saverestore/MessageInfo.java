package com.verizon.vzmsgs.saverestore;

public class MessageInfo {

	private long id;
	private boolean isSMS;
	
	MessageInfo(long _id, boolean _isSMS) {
		this.id = _id;
		this.isSMS = _isSMS;
	}
	
	public long getId() {
		return id;
	}
	
	public boolean isSMS() {
		return isSMS;
	}
}
