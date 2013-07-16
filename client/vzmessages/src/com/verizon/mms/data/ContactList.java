package com.verizon.mms.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ui.ContactImage;
import com.verizon.mms.ui.MessageUtils;

public class ContactList extends ArrayList<Contact>  {
    private static final long serialVersionUID = 1L;
    private static final Pattern alphaPat = Pattern.compile("\\p{Alpha}");

    private static final String COMMA_SEPARATOR = ", ";

    public static ContactList getByNumbers(Iterable<String> numbers, boolean canBlock) {
        ContactList list = new ContactList();
        for (String number : numbers) {
            if (!TextUtils.isEmpty(number)) {
                list.add(Contact.get(number, canBlock));
            }
        }
        return list;
    }

    public static ContactList getByNumbers(String semiSepNumbers,
                                           boolean canBlock,
                                           boolean replaceNumber) {
        ContactList list = new ContactList();
        if (semiSepNumbers != null) {
	        for (String number : semiSepNumbers.split(";")) {
	            if (!TextUtils.isEmpty(number)) {
	                Contact contact = Contact.get(number, canBlock);
	                //bug http://50.17.243.155/bugzilla/show_bug.cgi?id=3473
					//use the parsed number as it avoids creation of duplicate entry in canonical address table
	                /*if (replaceNumber) {
	                    contact.setNumber(number);
	                }*/
	                list.add(contact);
	            }
	        }
        }
        return list;
    }

    
    public static ContactList getByIds(String spaceSepIds, boolean canBlock) {
    	return getByIds(spaceSepIds, canBlock, null);
    }
    
    /**
     * Returns a ContactList for the corresponding recipient ids passed in. This method will
     * create the contact if it doesn't exist, and would inject the recipient id into the contact.
     */
    public static ContactList getByIds(String spaceSepIds, boolean canBlock, Object cookie) {
        return getByIds(spaceSepIds, canBlock, cookie, true);
    }
    
    public static ContactList getByIds(String spaceSepIds, boolean canBlock, Object cookie, boolean update) {
        ContactList list = new ContactList();
        for (RecipientIdCache.Entry entry : RecipientIdCache.getAddresses(spaceSepIds)) {
            if (entry != null && !TextUtils.isEmpty(entry.number)) {
                Contact contact = Contact.get(entry.number, canBlock, update, cookie);
                contact.setRecipientId(entry.id);
                list.add(contact);
            }
        }
        return list;
    }

    public int getPresenceResId() {
        // We only show presence for single contacts.
        if (size() != 1)
            return 0;

        return get(0).getPresenceResId();
    }

    public String formatNames() {
    	return formatNames(COMMA_SEPARATOR, true);
    }

    public String formatNames(String separator, boolean firstNames) {
    	final String ret;
    	final int size = size();
    	if (size == 1) {
    		ret = get(0).getName();
    	}
    	else if (size == 0) {
    		ret = "";
    	}
    	else {
	        final ArrayList<String> names = new ArrayList<String>(size);
	        for (Contact c : this) {
	           	String name = c.getName();
	           	if (firstNames) {
	           		// simplistic parsing of first space-separated substring
	           		final int i = name.indexOf(' ');
	           		if (i > 0) {
	           			// only accept strings that have at least one alpha character
	           			final String first = name.substring(0, i);
	           			final Matcher matcher = alphaPat.matcher(first);
	           			if (matcher.find()) {
	           				name = first;
	           			}
	           		}
	           	}
	           	names.add(name);
	        }
	        ret = TextUtils.join(separator, names);
    	}
    	return ret;
    }

    public String formatNamesAndNumbers(String separator) {
        String[] nans = new String[size()];
        int i = 0;
        for (Contact c : this) {
            nans[i++] = c.getNameAndNumber();
        }
        return TextUtils.join(separator, nans);
    }

    public String serialize() {
        return TextUtils.join(";", getNumbers());
    }

    public boolean containsEmail() {
        for (Contact c : this) {
            if (c.isEmail()) {
                return true;
            }
        }
        return false;
    }

    public String[] getNumbers() {
        return getNumbers(false /* don't scrub for MMS address */);
    }

    static public String getDelimSeperatedNumbers(ContactList contacts, String delim) {
        StringBuilder numberList = new StringBuilder();
        
        for (Contact contact : contacts) {
           numberList.append(contact.getNumber());
           numberList.append(delim);
        }
        return numberList.toString();
    }
    
    public String[] getNumbers(boolean scrubForMmsAddress) {
        List<String> numbers = new ArrayList<String>();
        String number;
        for (Contact c : this) {
            number = c.getNumber();

            if (scrubForMmsAddress) {
                // parse/scrub the address for valid MMS address. The returned number
                // could be null if it's not a valid MMS address. We don't want to send
                // a message to an invalid number, as the network may do its own stripping,
                // and end up sending the message to a different number!
                number = MessageUtils.parseMmsAddress(number);
            }

            // Don't add duplicate numbers. This can happen if a contact name has a comma.
            // Since we use a comma as a delimiter between contacts, the code will consider
            // the same recipient has been added twice. The recipients UI still works correctly.
            // It's easiest to just make sure we only send to the same recipient once.
            if (!TextUtils.isEmpty(number) && !numbers.contains(number)) {
                numbers.add(number);
            }
        }
        return numbers.toArray(new String[numbers.size()]);
    }
    
    public List<String> getNumbersList() {
        List<String> numbers = new ArrayList<String>();
        String number;
        for (Contact c : this) {
            number = c.getNumber();

            // Don't add duplicate numbers. This can happen if a contact name has a comma.
            // Since we use a comma as a delimiter between contacts, the code will consider
            // the same recipient has been added twice. The recipients UI still works correctly.
            // It's easiest to just make sure we only send to the same recipient once.
            if (!TextUtils.isEmpty(number) && !numbers.contains(number)) {
                numbers.add(number);
            }
        }
        return numbers;
    }

    /**
     * Gets an array of images for the contact list suitable for display in a ContactImage.  Note that
     * the array has a maximum size of ContactImage.NUM_VIEWS, so it won't necessary contain images for
     * all contacts in the list.
     * @param context
     * @param defaultImage The default image to use if the list is empty or an image isn't available
     * @param noNullReturn If true, return an array with a single default image in it if the list is empty,
     * otherwise return null
     * @return An array of contact images (or default ones), or null if the list is empty and noNullReturn is false
     */
	public Drawable[] getImages(final Context context, final Drawable defaultImage, boolean noNullReturn) {
		final Drawable[] images;
		int size = size();
		if (size == 1) {
			images = new Drawable[] { get(0).getAvatar(context, defaultImage) };
		}
		else if (size > 1) {
			// build image array
			if (size > ContactImage.NUM_VIEWS) {
				size = ContactImage.NUM_VIEWS;
			}
			images = new Drawable[size];
			for (int i = 0; i < size; ++i) {
				images[i] = get(i).getAvatar(context, defaultImage);
			}
		}
		else if (noNullReturn) {
			images = new Drawable[] { defaultImage };
		}
		else {
			images = null;
		}
		return images;
	}

    @Override
    public boolean equals(Object obj) {
        try {
            ContactList other = (ContactList)obj;
            // If they're different sizes, the contact
            // set is obviously different.
            if (size() != other.size()) {
                return false;
            }

            // Make sure all the individual contacts are the same.
            for (Contact c : this) {
                if (!other.contains(c)) {
                    return false;
                }
            }

            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    private void log(String msg) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(Contact.class, "[ContactList] " + msg);
        }
	}
}
