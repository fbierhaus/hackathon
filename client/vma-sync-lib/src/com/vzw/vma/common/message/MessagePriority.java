package com.vzw.vma.common.message;

public enum MessagePriority {

	NORMAL(0,"NORMAL"),
	URGENT(2, "URGENT");
	
	private int code;
	private String desc;
	
	private MessagePriority(int code, String desc) {
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
