/**
 * VZTelephoney.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.util;

import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.MmsConfig;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jul 9, 2012
 */
public class VZTelephony {
    private static final String[] ID_PROJECTION = { BaseColumns._ID };

	public static long getOrCreateThreadId(Context context, String recipient) {
		Set<String> recipients = new HashSet<String>();
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(VZTelephony.class, "in getOrCreateThreadId - recipient = " + recipient);
		}
		recipients.add(recipient);
		long threadid = -1;
		if (VZUris.isTabletDevice()) {
			threadid = getOrCreateThreadIdForTablet(context, recipients);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(VZTelephony.class, "in getOrCreateThreadId - recipient = " + recipient + "got threadid = " + threadid);
			}
			return threadid;
		} else {
			try {
				threadid = android.provider.Telephony.Threads.getOrCreateThreadId(context, recipients);
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(VZTelephony.class, "recipient = " + recipient + "got threadid = " + threadid);
				}			
				return threadid;
			} catch (Exception e) {
				boolean recp_present = recipient != null; 
				if(recp_present) {
					Logger.postErrorToAcra("Recipient Present : "+recp_present + " : "+e);
        		}
				throw new IllegalArgumentException(e);
			}
		}
	}

    /**
     * Given the recipients list and subject of an unsaved message, return its thread ID. If the message
     * starts a new thread, allocate a new thread ID. Otherwise, use the appropriate existing thread ID.
     * 
     * Find the thread ID of the same set of recipients (in any order, without any additions). If one is
     * found, return it. Otherwise, return a unique thread ID.
     */
    public static long getOrCreateThreadId(Context context, Set<String> recipients) {
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(VZTelephony.class, "in getOrCreateThreadId - recipients = " + recipients);
        }
        long threadid = -1;
        if (VZUris.isTabletDevice()) {
            threadid = getOrCreateThreadIdForTablet(context, recipients);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(VZTelephony.class, "in getOrCreateThreadId - recipients = " + recipients + "got threadid = " + threadid);
			}
			return threadid;            
        } else {
        	
        	try {
        		threadid = android.provider.Telephony.Threads.getOrCreateThreadId(context, recipients);
    			if (Logger.IS_DEBUG_ENABLED) {
    				Logger.debug(VZTelephony.class, "in getOrCreateThreadId - recipients = " + recipients + "got threadid = " + threadid);
    			}
    			return threadid;            
        	} catch (Exception e) {
    			boolean recp_present = recipients != null && !recipients.isEmpty(); 
        		if(recp_present) {
        			Logger.postErrorToAcra("Recipient Present : "+recp_present + " : "+e);
        		}
        		throw new IllegalArgumentException(e);
        	}
        	
        }
    }

    /**
     * Given the recipients list and subject of an unsaved message, return its thread ID. If the message
     * starts a new thread, allocate a new thread ID. Otherwise, use the appropriate existing thread ID.
     * 
     * Find the thread ID of the same set of recipients (in any order, without any additions). If one is
     * found, return it. Otherwise, return a unique thread ID.
     */
    public static long getOrCreateThreadIdForTablet(Context context, Set<String> recipients) {
        Uri.Builder uriBuilder = VZUris.getThreadIdUri().buildUpon();

        for (String recipient : recipients) {
            if (Mms.isEmailAddress(recipient)) {
                recipient = Mms.extractAddrSpec(recipient);
            }

            uriBuilder.appendQueryParameter("recipient", recipient);
        }

        Uri uri = uriBuilder.build();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(VZTelephony.class, "getOrCreateThreadId uri: " + uri);
        }
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), uri, ID_PROJECTION, null,
                null, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(VZTelephony.class, "getOrCreateThreadId cursor cnt: " + cursor.getCount());
        }
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0);
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(VZTelephony.class, "getOrCreateThreadId returned no rows!");
                    }
                }
            } finally {
                cursor.close();
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(VZTelephony.class, "getOrCreateThreadId failed with uri " + uri.toString());
        }
        throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
    }

	public static Uri addMessageToUri(ContentResolver resolver, Uri uri,
			String address, String body, String subject, Long date,
			boolean read, boolean deliveryReport, long threadId) {

		if (MmsConfig.isTabletDevice()) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("VZTelephony addMessageToUri, is a tablet device, insert sms manualy");
			}
			ContentValues values = new ContentValues(7);

			values.put(Telephony.Sms.ADDRESS, address);
			if (date != null) {
				values.put(Telephony.Sms.DATE, date);
			}
			values.put(Telephony.Sms.READ,
					read ? Integer.valueOf(1) : Integer.valueOf(0));
			values.put(Telephony.Sms.SUBJECT, subject);
			values.put(Telephony.Sms.BODY, body);
			if (deliveryReport) {
				values.put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_PENDING);
			}
			if (threadId != -1L) {
				values.put(Telephony.Sms.THREAD_ID, threadId);
			}
			return resolver.insert(uri, values);
		} else {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("VZTelephony addMessageToUri, is a mobile");
			}
			return Sms.addMessageToUri(resolver, uri, address, body, subject,
					date, read, deliveryReport, threadId);
		}
	}
}
