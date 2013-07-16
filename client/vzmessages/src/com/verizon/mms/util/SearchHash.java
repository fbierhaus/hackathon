package com.verizon.mms.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.data.Contact;

public class SearchHash {
	private HashMap<Character, HashMap<Contact, Integer>> searchList;
		
	public SearchHash() {
		searchList = new HashMap<Character, HashMap<Contact, Integer>>();
	}
	
	/**
	 * Creates an index using the letter of the contact name.
	 * Assumption is that contact.getName() is not empty 
	 * @param contact
	 */
	public void addSearchItem(Contact contact) {
		String name = contact.getName();
		
		name = name.toLowerCase();
		int size = name.length();
		HashMap<Contact, Integer> map;
		char ch;
		for (int i = 0; i < size; i++) {
			ch = name.charAt(i);
			map = searchList.get(ch);
			
			if (map == null) {
				map = new HashMap<Contact, Integer>();
				searchList.put(ch, map);
			}
			
			if (!map.containsKey(contact)) {
				map.put(contact, i);
			}
		}
	}
	
	public List<Contact> getMatchingContactList(String searchString, int resultsLimit) {
		List<Contact> matchedList = new ArrayList<Contact>();

		HashMap<Contact, Integer> contactList = null;
		String name = searchString.toLowerCase();
		contactList = searchList.get(name.charAt(0));
		
		if (contactList == null) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("getMatchingContactList no matching contacts found");
			}
			return matchedList;
		}

		int length = searchString.length();
		Contact contact = null;
		int limit = 0;
		int value = 0;
		String contactName;
		
		for (Entry<Contact, Integer> entry : contactList.entrySet()) {
			contact = entry.getKey();
			contactName = contact.getName().toLowerCase();
			value = entry.getValue();
			
			if (length == 1) {
				matchedList.add(contact);
				limit++;
			} else if (contactName.substring(value).contains(name)){
				matchedList.add(contact);
				limit++;
			}
			
			if (resultsLimit != -1 && limit == resultsLimit) {
				break;
			}
		}

		return matchedList;
	}
}
