package com.verizon.mms.helper;

public class HrefEntity {
	public String tag;
	public String attrPropertyName;
	public String attrPropertyValue;
	public String attrValueName;
//	public String attrValueValue;
	public int type;
	
	public HrefEntity(String tag,
				  String attrPropertyName, String attrPropertyValue,
				  String attrValueName,
				  int type) {
		this.tag = tag;
		this.attrPropertyName = attrPropertyName;
		this.attrPropertyValue = attrPropertyValue;
		this.attrValueName = attrValueName;
		this.type = type;
	}
};
