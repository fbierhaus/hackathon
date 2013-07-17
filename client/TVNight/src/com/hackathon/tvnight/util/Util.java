package com.hackathon.tvnight.util;

import android.content.Context;
import android.telephony.TelephonyManager;

public class Util {
    /**
     * Get phone number.
     * 
     * @param context
     * @return
     */
    public static String getPhoneNumber(Context context) {   	
    	TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); 
    	String phoneNumber = telephonyManager.getLine1Number();
    	return phoneNumber;
    }
}
