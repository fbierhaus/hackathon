/**
 * AvatarClickListener.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.util;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.QuickContact;
import android.provider.Telephony.Mms;
import android.view.View;

import com.verizon.mms.data.Contact;
import com.verizon.mms.data.RecipientIdCache;
import com.verizon.mms.data.RecipientIdCache.Entry;

/**
 * This class/interface   
 * @author Imthiaz
 * @Since  Jun 13, 2012
 */
/**
 * This class helps in ContactImage On-click events
 */
public class AvatarClickListener implements View.OnClickListener {
    private String recipients;
    
    private Context mContext;

    public AvatarClickListener(Context context, String recipients) {
        this.recipients = recipients;
        this.mContext = context;
    }
    
    public void setRecipients(String recipients) {
    	this.recipients = recipients;
    }

    @Override
    public void onClick(View view) {
    	List<Entry> entries = RecipientIdCache.getAddresses(recipients);
    	// don't handle onClick for group mms
    	if (entries.size() == 1) {
    		final RecipientIdCache.Entry entry = entries.get(0);
    		final Contact contact = Contact.get(entry.number, true);
    		final String lookUpKey = contact.getLookUpKey();
    		if (lookUpKey != null) {
    			Uri lookUpUri = Contacts.getLookupUri(contact.getContactId(), lookUpKey);
    			QuickContact.showQuickContact(mContext, view, lookUpUri, ContactsContract.QuickContact.MODE_MEDIUM,
    					new String[] { Contacts.CONTENT_ITEM_TYPE });
    		}
    		else {
    			final Uri createUri;
    			if (Mms.isEmailAddress(entry.number)) {
    				createUri = Uri.fromParts("mailto", entry.number, null);
    			}
    			else {
    				createUri = Uri.fromParts("tel", entry.number, null);
    			}
    			final Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT, createUri);
    			mContext.startActivity(intent);
    			contact.markAsStale();
    		}
    	}
    }
}

