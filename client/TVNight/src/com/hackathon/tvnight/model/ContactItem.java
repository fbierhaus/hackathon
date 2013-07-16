package com.hackathon.tvnight.model;

import android.graphics.Bitmap;

public class ContactItem {

	private String name;
	private String number;
	private String numberType;
	private Bitmap icon;
	
	public ContactItem() { }
	
	public ContactItem(String name, String number, String numberType, Bitmap icon) {
		this.name = name;
		this.number = number;
		this.numberType = numberType;
		this.icon = icon;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getNumberType() {
		return numberType;
	}

	public void setNumberType(String numberType) {
		this.numberType = numberType;
	}

	public Bitmap getIcon() {
		return icon;
	}

	public void setIcon(Bitmap icon) {
		this.icon = icon;
	}
	
}
