package com.vzw.vma.common.message;

public enum MessageSourceEnum {
	WEB(0, "WEB"),
	IMAP(1, "IMAP"),
	PHONE(2, "PHONE");
	
	private int code;
	private String desc;
	
	private MessageSourceEnum(int code, String desc){
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
