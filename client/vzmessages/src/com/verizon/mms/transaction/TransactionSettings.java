/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.transaction;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.internal.telephony.Phone;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.util.SqliteWrapper;

/**
 * Container of transaction settings. Instances of this class are contained within Transaction instances to
 * allow overriding of the default APN settings or of the MMS Client.
 */
public class TransactionSettings {

    private String mServiceCenter;
    private String mProxyAddress;
    private int mProxyPort = -1;

    private static final String[] APN_PROJECTION  = {
        Telephony.Carriers.TYPE,     // 0
        Telephony.Carriers.MMSC,     // 1
        Telephony.Carriers.MMSPROXY, // 2
        Telephony.Carriers.MMSPORT   // 3
    };
    private static final int COLUMN_TYPE     = 0;
    private static final int COLUMN_MMSC     = 1;
    private static final int COLUMN_MMSPROXY = 2;
    private static final int COLUMN_MMSPORT  = 3;

    /**
     * Constructor that uses the default settings of the MMS Client.
     * 
     * @param context
     *            The context of the MMS Client
     */
    public TransactionSettings(Context context, String apnName) {
        
        
        if(ApplicationSettings.isVZMMSLabEnabled()){
            mServiceCenter = ApplicationSettings.getInstance().getMMSCHostURL();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "MMSC Server :" + mServiceCenter);
            }
            return;
        }
        

    	// Default setting if VZ config is enabled. Why waste time querying. Also note that sometimes this query happens on
    	// main thread
    	if (MmsConfig.getEnableVzwAppApn()) {
    		mServiceCenter = ApplicationSettings.getInstance().getMMSCHostURL();
        	//mServiceCenter = "http://vzpix.com/servlets/mms";
    		 if (Logger.IS_DEBUG_ENABLED) {
                 Logger.debug(getClass(), "MMSC Server :" + mServiceCenter);
             }      
    		// these were used in IOT testing
    		//mServiceCenter = "http://mms.ftw.nsn-rdnet.net/servlets/mms";
    		//mServiceCenter = "http://MMS.FTW.NSN-RDNET.NET/mmsc/MMS";    		
    		return;
    	}
    	
    	// these URLs are for VZ lab
    	//mServiceCenter = "http://vzpix.com/servlets/mms";
    	//return;
    	// if (0) {

		String selection = Telephony.Carriers.CURRENT + " IS NOT NULL";
        String[] selectionArgs = null;
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "APN requested is " + apnName);
        }
        if (apnName != null) {
            selection += " AND " + Telephony.Carriers.APN + "=?";
            selectionArgs = new String[] { apnName.trim() };
        }

        final ContentResolver res = context.getContentResolver();
        Cursor cursor = null;
        try {
        	cursor = SqliteWrapper.query(context, res, VZUris.getTelephonyCarriers(), APN_PROJECTION, selection, selectionArgs, null);

	        if (Logger.IS_DEBUG_ENABLED) {
	        	Logger.debug(getClass(), "looking for apn: " + selection + ", returned: "
	                + (cursor == null ? "null cursor" : (cursor.getCount() + " hits")));
	        }
	
	        // if we do not get any current APN then we just use the default. We found on some devices that the VZ APN was not even
	        // defined. Therefore we force use of default in such situations
	        if ((cursor == null) || (cursor.getCount() == 0)) {
	            if (Logger.IS_WARNING_ENABLED) {
	                Logger.debug(getClass(), "Null cursor querying APNs - using default APN");
	            }
	            return;
	        }
	
	        // If VZ APP APN is enabled (means we are running on VZ network then we dont need to go through all APNs to find one. We use default
	        if (!MmsConfig.getEnableVzwAppApn() && (cursor.getCount() == 0)) {
	        	// XXX no APN is marked as current: if there is only one APN defined then use it
	        	// since some devices don't set the current field
	        	cursor.close();
	            cursor = SqliteWrapper.query(context, res, VZUris.getTelephonyCarriers(), APN_PROJECTION, null, null, null);
	            if (cursor != null) {
	            	final int count = cursor.getCount();
	                if (count != 1) {
	                    if (Logger.IS_WARNING_ENABLED) {
	                        Logger.debug(getClass(), count + " APNs defined"
	                                + (count == 0 ? "" : " but no current - still proceed"));
	                    }
	            	}
	            }
	            else {
	                if (Logger.IS_WARNING_ENABLED) {
	                    Logger.debug(getClass(), "Null cursor querying APNs - using default APN");
	                }
	                return;
	            }
	        }
	
	        boolean sawValidApn = false;
	        boolean foundMmsc = false;
	        try {
	            while (cursor.moveToNext() && !foundMmsc) { // && TextUtils.isEmpty(mServiceCenter)) {
	                // Read values from APN settings
	                if (isValidApnType(cursor.getString(COLUMN_TYPE), Phone.APN_TYPE_MMS)) {
	                    if (Logger.IS_WARNING_ENABLED) {
	                        Logger.debug(getClass(), "Saw valid APN Type = " + cursor.getString(COLUMN_TYPE));
	                    }
	                	sawValidApn = true;
	                    try {
	                    	String tempServiceCenter = trimV4AddrZeros(cursor.getString(COLUMN_MMSC).trim());
	                        if (!TextUtils.isEmpty(tempServiceCenter)) {
	                        	mServiceCenter = tempServiceCenter;
	                        	foundMmsc = true;
	                        }
	                        if (Logger.IS_WARNING_ENABLED) {
	                            Logger.debug(getClass(), "Reading APNs...: SC: = " + mServiceCenter);
	                        }
	                        mProxyAddress = trimV4AddrZeros(cursor.getString(COLUMN_MMSPROXY));
	                        if (Logger.IS_WARNING_ENABLED) {
	                            Logger.debug(getClass(), "Reading APNs...: PA: = " + mProxyAddress);
	                        }
	                    } catch (Exception e) {
	                    	Logger.debug(getClass(), "Exception reading service Center and Proxy");                    	
	                    }
	                    if (isProxySet()) {
	                        String portString = cursor.getString(COLUMN_MMSPORT);
	                        if(Logger.IS_WARNING_ENABLED){
	                        Logger.debug(getClass(), "Reading APNs...: Port: = " + portString);
	                        }
	                        try {
	                            mProxyPort = Integer.parseInt(portString);
	                        } catch (NumberFormatException e) {
	                            if (TextUtils.isEmpty(portString)) {
	                                if (Logger.IS_DEBUG_ENABLED) {
	                                	Logger.debug(getClass(), "mms port not set!");
	                                }
	                            } else {
	                                if (Logger.IS_WARNING_ENABLED) {
	                                    Logger.debug(getClass(), "Bad port number format: " + portString);
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        } catch (Exception e) {
	            if (Logger.IS_WARNING_ENABLED) {
	                Logger.debug(getClass(), "Exception reading the APN table - use default", e);
	            }
	        }

	        if (sawValidApn && TextUtils.isEmpty(mServiceCenter)) {
	            if (Logger.IS_WARNING_ENABLED) {
	                Logger.debug(getClass(), "Invalid APN setting: MMSC is empty");
	            }
	        }
        }
        finally {
        	if (cursor != null) {
        		cursor.close();
        	}
        }

        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "APN setting: MMSC = " + mServiceCenter);
        }
    }

    /**
     * Constructor that overrides the default settings of the MMS Client.
     * 
     * @param mmscUrl
     *            The MMSC URL
     * @param proxyAddr
     *            The proxy address
     * @param proxyPort
     *            The port used by the proxy address immediately start a SendTransaction upon completion of a
     *            NotificationTransaction, false otherwise.
     */
    public TransactionSettings(String mmscUrl, String proxyAddr, int proxyPort) {
        mServiceCenter = mmscUrl != null ? mmscUrl.trim() : null;
        mProxyAddress = proxyAddr;
        mProxyPort = proxyPort;

        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "TransactionSettings: " + mServiceCenter + " proxyAddress: " + mProxyAddress
                + " proxyPort: " + mProxyPort);
        }
    }

    public String getMmscUrl() {
        return mServiceCenter;
    }

    public String getProxyAddress() {
        return mProxyAddress;
    }

    public int getProxyPort() {
        return mProxyPort;
    }

    public boolean isProxySet() {
        return (mProxyAddress != null) && (mProxyAddress.trim().length() != 0);
    }

    static private boolean isValidApnType(String types, String requestType) {
        // If APN type is unspecified, assume APN_TYPE_ALL.
        if (TextUtils.isEmpty(types)) {
            return true;
        }

        // added check for vzwapp because on some phone such as LG Revolution mmsc url is defined by vzwapp type
        for (String t : types.split(",")) {
            if (t.equals(requestType) || t.equals(Phone.APN_TYPE_ALL) || t.equals("vzwapp")) {
                return true;
            }
        }
        return false;
    }

    public static String trimV4AddrZeros(String addr) {
        if (addr == null)
            return null;
        String[] octets = addr.split("\\.");
        if (octets.length != 4)
            return addr;
        StringBuilder builder = new StringBuilder(16);
        String result = null;
        for (int i = 0; i < 4; i++) {
            try {
                if (octets[i].length() > 3)
                    return addr;
                builder.append(Integer.parseInt(octets[i]));
            } catch (NumberFormatException e) {
                return addr;
            }
            if (i < 3)
                builder.append('.');
        }
        result = builder.toString();
        return result;
    }
}