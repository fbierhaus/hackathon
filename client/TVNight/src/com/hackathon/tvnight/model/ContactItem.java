package com.hackathon.tvnight.model;

import android.graphics.Bitmap;

public class ContactItem {

	private String name;
	private String number;
	private String numberType;
	private Bitmap icon;
	private boolean isChecked;
	
	public ContactItem() { }
	
	public ContactItem(String name, String number, String numberType, Bitmap icon, boolean isChecked) {
		this.name = name;
		this.isChecked = isChecked;
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
	
	public void setChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}
	
	public boolean getChecked() {
		return isChecked;
	}
	
}
