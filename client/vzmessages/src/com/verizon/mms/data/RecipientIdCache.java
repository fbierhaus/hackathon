package com.verizon.mms.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.provider.Telephony.CanonicalAddressesColumns;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.util.SqliteWrapper;


public class RecipientIdCache {
    private static final String TAG = "Mms/cache";

    private static Uri sAllCanonical = VZUris.getMmsSmsCanonicalAddresses();
    
    private static Uri sSingleCanonicalAddressUri = VZUris.getMmsSmsCanonical();

    private static RecipientIdCache sInstance;
    private static boolean mLoadingAddress;
    static RecipientIdCache getInstance() { return sInstance; }
    private final Map<Long, String> mCache;
    private final Context mContext;

    public static class Entry {
        public long id;
        public String number;

        public Entry(long id, String number) {
            this.id = id;
            this.number = number;
        }
    };

	public static long getRecipientId(String number) {
		synchronized (sInstance) {
			final Map<Long, String> cache = sInstance.mCache;
			// check to verify if its only a number with allowed characters or
			// email address.
			boolean isOnlyNumber = Pattern.matches("[0-9,),(,-]+",
					number.replaceAll("\\s+", ""));

			for (Long id : cache.keySet()) {
				if (isOnlyNumber) {
					if (PhoneNumberUtils.compare(number, cache.get(id))) {
						return id;
					}
				} else {
					// using string comparison in case of non number item .ie
					// email addresses.
					if (number.equalsIgnoreCase(cache.get(id))) {
						return id;
					}
				}

			}
		}
		return -1;
	}
    
    static void init(Context context) {
        sInstance = new RecipientIdCache(context);

        fill();
        //load the recipeint cache
        /*new Thread(new Runnable() {
            public void run() {
                fill();
            }
        }).start();*/
    }

    RecipientIdCache(Context context) {
        mCache = new HashMap<Long, String>();
        mContext = context;
    }

    public static void fill() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("[RecipientIdCache] fill: begin");
        }

        synchronized (sInstance) {
            if (mLoadingAddress) {
                return;
            }
            mLoadingAddress = true;
        }
        
        Context context = sInstance.mContext;
        Cursor c = null;
        try {
        	c = SqliteWrapper.query(context, context.getContentResolver(),
        			sAllCanonical, null, null, null, null);
        } catch (IllegalArgumentException e) {
        	if (Logger.IS_ERROR_ENABLED) {
        		Logger.error("Got exception in RecipientIdCache.fill querying " + sAllCanonical, e);
        	}
        }
        if (c == null) {
         if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(TAG, "null Cursor in fill()");
        	}
            return;
        }

        try {
            synchronized (sInstance) {
                // Technically we don't have to clear this because the stupid
                // canonical_addresses table is never GC'ed.
                sInstance.mCache.clear();
                while (c.moveToNext()) {
                    // TODO: don't hardcode the column indices
                    long id = c.getLong(0);
                    String number = c.getString(1);
                    sInstance.mCache.put(id, number);
                }
            }
        } finally {
        	if (c != null) {
        		c.close();
        	}
            
            synchronized (sInstance) {
                mLoadingAddress = false;
            }
        }
    }
    
    public static List<Entry> getAddresses(String spaceSepIds) {
        synchronized (sInstance) {
            final Map<Long, String> cache = sInstance.mCache;
            final String[] ids = spaceSepIds == null ? new String[0] : spaceSepIds.split(" ");
            final List<Entry> numbers = new ArrayList<Entry>(ids.length);
            for (String id : ids) {
                final long longId;
                try {
                    longId = Long.parseLong(id);
                } catch (NumberFormatException ex) {
                    if(Logger.IS_ERROR_ENABLED){
                        Logger.error(false,RecipientIdCache.class ,ex);  // XXX
                    }
                    continue;
                }

                String number = cache.get(longId);
                if (number == null) {
                	// get it from the provider
                    final Uri uri = ContentUris.withAppendedId(sSingleCanonicalAddressUri, longId);
                    final Context context = sInstance.mContext;
                    final String[] cols = new String[] { CanonicalAddressesColumns.ADDRESS };
                    final Cursor c = SqliteWrapper.query(context, context.getContentResolver(), uri, cols, null, null, null);
                    if (c != null) {
                    	try {
	                    	if (c.getCount() > 0) {
		                    	c.moveToFirst();
		                        number = c.getString(0);
		                        // TODO limit cache size
		                        cache.put(longId, number);
	                    	}
	                        else {
	                            if(Logger.IS_ERROR_ENABLED){
	                                Logger.error(false,RecipientIdCache.class,"RecipientIdCache.getAddresses: empty cursor for " + longId);
	                            }
	                        }
                    	}
                    	catch (Exception e) {
                    	    if(Logger.IS_ERROR_ENABLED){
                    	        Logger.error(true,RecipientIdCache.class,e);
                    	    }
                    	}
                    	finally {
                    		c.close();
                    	}
                    }
                    else {
                        if(Logger.IS_ERROR_ENABLED){
                            Logger.error(false, RecipientIdCache.class, "RecipientIdCache.getAddresses: null cursor for " + longId);
                        }
                    }
                }

                if (TextUtils.isEmpty(number)) {
                    if(Logger.IS_ERROR_ENABLED){
                        Logger.error(false,RecipientIdCache.class, "RecipientIdCache.getAddresses: " + longId + " has empty number");
                    }
                } else {
                    numbers.add(new Entry(longId, number));
                }
            }
            return numbers;
        }
    }

    public static void updateNumbers(long threadId, ContactList contacts) {
        long recipientId = 0;

        for (Contact contact : contacts) {
            if (contact.isNumberModified()) {
                contact.setIsNumberModified(false);
            } else {
                // if the contact's number wasn't modified, don't bother.
                continue;
            }

            recipientId = contact.getRecipientId();
            if (recipientId == 0) {
                continue;
            }

            String number1 = contact.getNumber();
            String number2 = sInstance.mCache.get(recipientId);

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(RecipientIdCache.class, "[RecipientIdCache] updateNumbers: comparing " + number1
                        + " with " + number2);
            }

            // if the numbers don't match, let's update the RecipientIdCache's number
            // with the new number in the contact.
            if (!Contact.equalsIgnoreSeperators(number1, number2)) {
                sInstance.mCache.put(recipientId, number1);
                sInstance.updateCanonicalAddressInDb(recipientId, number1);
            }
        }
    }

    private void updateCanonicalAddressInDb(long id, String number) throws UnsupportedOperationException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "[RecipientIdCache] updateCanonicalAddressInDb: id=" + id + ", number="
                    + number);
        }

        ContentValues values = new ContentValues();
        values.put(Telephony.CanonicalAddressesColumns.ADDRESS, number);

        StringBuilder buf = new StringBuilder(Telephony.CanonicalAddressesColumns._ID);
        buf.append('=').append(id);

        Uri uri = ContentUris.withAppendedId(sSingleCanonicalAddressUri, id);
        
       try {
       	   mContext.getContentResolver().update(uri, values, buf.toString(), null);
       } catch(UnsupportedOperationException e){
           Logger.error(getClass(), "Failed to update CanonicalAddress In Db : ", e);
       }
       
    }

    public static void dump() {
        // Only dump user private data if we're in special debug mode
        if (Logger.IS_DEBUG_ENABLED) {
            synchronized (sInstance) {
            	Logger.debug(RecipientIdCache.class, "*** Recipient ID cache dump ***");
            	for (Long id : sInstance.mCache.keySet()) {
            		Logger.debug(RecipientIdCache.class, id + ": " + sInstance.mCache.get(id));
            	}
            }
        }
    }
}
