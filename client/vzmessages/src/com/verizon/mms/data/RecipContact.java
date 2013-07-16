package com.verizon.mms.data;

import android.text.TextUtils;

import com.verizon.mms.ui.MessageUtils;


public class RecipContact {
    private String  name;
    private long    contactId;
    private String  key;
    private boolean isInAddressBook = false;

    public RecipContact(String name, String key, long contactId, String label) {
    	this.contactId = contactId;
    	this.key = MessageUtils.getParsedKey(key);
    	
    	if (contactId != -1) {
    		isInAddressBook = true;
    		this.name = name;
    	} else {
    		this.name = this.key;
    	}
    	if (!TextUtils.isEmpty(label)) {
    		this.name += " (" + label.charAt(0) + ")";
    	}
    }

	public String getName() {
        return name;
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public String getKey() {
        return key;
    }

    public boolean isInAddressBook() {
        return isInAddressBook;
    }

    public void setInAddressBook(boolean isInAddressBook) {
        this.isInAddressBook = isInAddressBook;
    }
}