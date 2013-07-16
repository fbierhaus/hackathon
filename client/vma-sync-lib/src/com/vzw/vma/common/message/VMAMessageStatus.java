package com.vzw.vma.common.message;

public enum VMAMessageStatus {

	SUBMITTED(0, "SUBMITTED"),
	DELIVERED(1, "DELIVERED"),
	FAILED(2, "FAILED"),
	READ(3, "READ"),
	UNREAD(4, "UNREAD");
	
	private int code;
	private String desc;
	
	private VMAMessageStatus(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public int getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}
	
	
	
}
