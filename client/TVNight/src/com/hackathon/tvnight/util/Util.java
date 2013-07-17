package com.hackathon.tvnight.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

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
    
    public static final String md5(final String s) {
    	return new String(Hex.encodeHex(org.apache.commons.codec.digest.DigestUtils.md5(s)));
    }
    
//    public static final String md5(final String s) {
//        try {
//            // Create MD5 Hash
//            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
//            digest.update(s.getBytes());
//            byte messageDigest[] = digest.digest();
//
//            // Create Hex String
//            StringBuffer hexString = new StringBuffer();
//            for (int i = 0; i < messageDigest.length; i++) {
//                String h = Integer.toHexString(0xFF & messageDigest[i]);
//                while (h.length() < 2)
//                    h = "0" + h;
//                hexString.append(h);
//            }
//            return hexString.toString();
//
//        }
//        catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        return "";
//    }
    
}
