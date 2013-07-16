package com.vzw.vma.common.message;

public enum MessageTypeEnum {
	SMS(0, "SMS"),
	MMS(1, "MMS");
	
	private int code;
	private String desc;
	
	private MessageTypeEnum(int code, String desc){
		this.code = code;
		this.desc = desc;
	}

	public int getCode(){
		return code;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	

}
