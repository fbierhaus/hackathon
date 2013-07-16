/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.verizon.mms;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.internal.telephony.Phone;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.mms.DeviceConfig.Device;
import com.verizon.mms.DeviceConfig.OEM;

public class MmsConfig {
    // private static final String TAG = "MmsConfig";
	
	private static final String  DEFAULT_HTTP_KEY_X_WAP_PROFILE    = "x-wap-profile";
    public static String BUILD_VERSION = "3.1.20";
    
	// introduced in Android ICS
	public static final int TYPE_MOBILE_CBS = 12;
		
	// this must be set to true before doing production build but for test builds should be false
	private static final boolean ENABLE_VZWAPPAPN = ApplicationSettings.getInstance().isVZWAPPAPNEnabled(); // false for test builds. Else it requires signature file from VZ

	// Should be either Phone.FEATURE_ENABLE_MMS or Phone.FEATURE_ENABLE_HIPRI
	// We found that HIPRI does not work on LTE devices. So we only use MOBILE_MMS. VZ has verified that when we use MOBILE_MMS
	// that the VZApp APN is being used properly
	private static String mPhoneFeatureMMS = Phone.FEATURE_ENABLE_MMS; 
	private static int mNetworkConnectivityMode = ConnectivityManager.TYPE_MOBILE_MMS;//ConnectivityManager.TYPE_MOBILE_HIPRI;
	private static boolean mIsLte = false;

    // For Verizon Android-Mms/2.0 UA does not work. So for non-VZ devices (for test) we set it as Android-Mms/2.0 else a default VZ one

    //private static final String  DEFAULT_USER_AGENT                = "Android-Mms/2.0";
    public static String  DEFAULT_USER_AGENT                = ENABLE_VZWAPPAPN ? "sami515" : "Android-Mms/2.0";

    private static final int     MAX_IMAGE_HEIGHT                  = 480;
    private static final int     MAX_IMAGE_WIDTH                   = 640;

    /**
     * Whether to hide MMS functionality from the user (i.e. SMS only).
     */
    private static boolean       mTransIdEnabled                   = false;
    private static int           mMmsEnabled                       = 1;                          
    private static int           mMaxMessageSize                   = 1258291;  // 1.2MB is max size of MMS
    private static int           mMaxTimeOutMessageSize            = 512000;  // 500KB is max size of timed out MMS
    private static String        mUserAgent                        = DEFAULT_USER_AGENT;
    private static String        mUaProfTagName                    = DEFAULT_HTTP_KEY_X_WAP_PROFILE;
    private static String        mUaProfUrl                        = null;
    private static String        mHttpParams                       = null;
    private static String        mHttpParamsLine1Key               = null;
    private static String        mEmailGateway                     = null;
    private static int           mMaxImageHeight                   = MAX_IMAGE_HEIGHT;           
    private static int           mMaxImageWidth                    = MAX_IMAGE_WIDTH;            
    private static int           mRecipientLimit                   = Integer.MAX_VALUE;          
    private static int           mDefaultMessagesPerThread         = 200;                             
    private static int           mMinMessageCountPerThread         = 2;                                
    private static int           mMaxMessageCountPerThread         = 5000;
    private static int 			 mRecyclerInterval				   = 24*60*60*1000;   // set for 1 day
    private static int 			 mRecyclerDelay			   = 15*60*1000;   	  // set for 15 mins
                                                                                                        
    private static int           mHttpSocketTimeout                = 30 * 1000;    // changed to 30 sec
    public static int           mHttpReadTimeout                  = 240 * 1000;    // set at 240 seconds
    private static int           mMinimumSlideElementDuration      = 7;            // 7 sec      
    private static boolean       mNotifyWapMMSC                    = true;
    private static boolean       mAllowAttachAudio                 = true;
    
    private static boolean       mMonitorAsyncQuery                = false;
    private static int           mMonitorAsyncQueryTimeout		   = 10;
    
    // This is the max amount of storage multiplied by mMaxMessageSize that we
    // allow of unsent messages before blocking the user from sending any more
    // MMS's.
    private static int           mMaxSizeScaleForPendingMmsAllowed = 8;  // was 4                                

    // Email gateway alias support, including the master switch and different rules
    private static boolean       mAliasEnabled                     = false;
    private static int           mAliasRuleMinChars                = 2;
    private static int           mAliasRuleMaxChars                = 48;

    private static Device        mDevice                           = null;
    private static String        mUserAgentFromDevices             = null;
    
    //initialized with values in the resources
    //if it is a tablet the value is picked from values-sw600dp
    //else it is picked from values folder
    private static boolean       mIsTabletResource                  = false;
    private static boolean       mVzmRTLEnabled                     = false;
    private static boolean       mFMSBEnabled                     	= true;
    private static long 		 mFMSBInterval 						= 5000;// in mili-seconds
	private static long          maxNativeHeap;
	private static int           maxTextLength                      = 2000;
	private static boolean       dieOnOom                           = false;
		//Enable Emojis



	public static final boolean enableEmojis = true;
	
	
	//Allow disk caching of images in Conversation View
	private static boolean		 allowConvDiskCache					= true;
	
	// Enable ImageEditor
    private static boolean       vzmImageEditorEnabled              = false;

    public static void init(Context context, int devResId, int mmsRevId, int boolResID) {
        DeviceConfig.init(context, devResId);
        loadMmsSettings(context, mmsRevId);

        // we load this to check if a device is tablet
		mIsTabletResource = context.getResources().getBoolean(boolResID);

		if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(MmsConfig.class, "Device model=" + OEM.deviceModel + ", manufacturer="
                    + OEM.deviceManufacturer);
        }
        // look for config
        mDevice = DeviceConfig.getDeviceByModel(context, OEM.deviceModel.toLowerCase(), mIsTabletResource);
        try {
        	PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        	BUILD_VERSION = info.versionName;
        } catch (Exception e) {
        	Logger.debug(MmsConfig.class, "Logger.error: error getting package info: " + e);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(MmsConfig.class, "Device from DeviceConfigs by model=" + mDevice);
        	Logger.debug(MmsConfig.class, "Running app " + BUILD_VERSION);
        }

        // Why do we need mUserAgentFromWebSettings - commenting that out
        //mUserAgentFromWebSettings = new WebView(context).getSettings().getUserAgentString();
        //log.info("#==> mUserAgentFromWebSettings ==> {}", mUserAgentFromWebSettings);

        mUserAgentFromDevices = (null != mDevice) ? mDevice.getmUserAgent() : null;

        
        // from web settings
        // mUserAgent = mUserAgentFromWebSettings;
        // from device list
        mUserAgent = (null != mUserAgentFromDevices) ? mUserAgentFromDevices : DEFAULT_USER_AGENT;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(MmsConfig.class, "mUserAgent=" + mUserAgent);
        }
        PackageManager pm = context.getPackageManager();
    	mIsLte = pm.hasSystemFeature("com.verizon.hardware.telephony.lte") ||
    				pm.hasSystemFeature("com.vzw.hardware.lte");

    	/*// if not LTE device then set max message size to 500KB
    	if (!mIsLte && !isTabletDevice()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(MmsConfig.class, "#==>  Setting Max MMS to 500KB ==> ");
            }
    		mMaxMessageSize = 512000;
    	}*/
    	
		//if (getEnableVzwAppApn() && mIsLte) {
		//	mPhoneFeatureMMS = Phone.FEATURE_ENABLE_HIPRI;
		//	mNetworkConnectivityMode = ConnectivityManager.TYPE_MOBILE_HIPRI;
        //    if (Logger.IS_DEBUG_ENABLED) {
        //        Logger.debug(MmsConfig.class,
        //                "#==> LTE device and ENABLING VZWAPPAPN so enabling HIPRI. Should only be done for prod or vz signed builds");
        //    }
        //}

    	maxNativeHeap = mDevice.getMaxNativeHeap();
    }

    public static boolean getMmsEnabled() {
        return mMmsEnabled == 1 ? true : false;
    }

    public static int getMaxMessageSize() {
        return mMaxMessageSize;
    }
    
    public static int getMaxTimedOutMessageSize() {
        return mMaxTimeOutMessageSize;
    }
    /**
     * This function returns the value of "enabledTransID" present in mms_config file. In case of single
     * segment wap push message, this "enabledTransID" indicates whether TransactionID should be appended to
     * URI or not.
     */
    public static boolean getTransIdEnabled() {
        return mTransIdEnabled;
    }

    public static String getUserAgent() {
        return mUserAgent;
    }

    public static String getUaProfTagName() {
        return mUaProfTagName;
    }

    public static String getUaProfUrl() {
        return mUaProfUrl;
    }

    public static boolean isLTE() {
    	if (Logger.IS_DEBUG_ENABLED)
    		Logger.debug("Device is LTE");
    	
    	return mIsLte;
    }

    /**
     * Returns the Value of the mDevice
     * @return the  {@link Device}
     */
    public static boolean isTabletDevice() {
    	return (mDevice != null) ? mDevice.isTablet() : mIsTabletResource;
    }
    public static boolean isNotificationLedSupported() {
        return (mDevice != null) ? mDevice.isNotificationLedSupported() : false;
    }

    public static String getHttpParams() {
        return mHttpParams;
    }

    public static String getHttpParamsLine1Key() {
        return mHttpParamsLine1Key;
    }

    public static String getEmailGateway() {
        return mEmailGateway;
    }

    public static int getMaxImageHeight() {
        return mMaxImageHeight;
    }

    public static int getMaxImageWidth() {
        return mMaxImageWidth;
    }

    public static int getRecipientLimit() {
        return mRecipientLimit;
    }

    public static int getDefaultMessagesPerThread() {
        return mDefaultMessagesPerThread;
    }

    public static int getMinMessageCountPerThread() {
        return mMinMessageCountPerThread;
    }

    public static int getMaxMessageCountPerThread() {
        return mMaxMessageCountPerThread;
    }
    
    public static int getRecyclerInterval() {
    	return mRecyclerInterval;
    }
    
    public static int getRecyclerDelay() {
    	return mRecyclerDelay;
    }

    public static int getHttpSocketTimeout() {
        return mHttpSocketTimeout;
    }

    public static int getHttpReadTimeout() {
        return mHttpReadTimeout;
    }

    public static int getMinimumSlideElementDuration() {
        return mMinimumSlideElementDuration;
    }

    public static int getMonitorAsyncQueryTimeout() {
        return mMonitorAsyncQueryTimeout;
    }

    public static boolean getNotifyWapMMSC() {
        return mNotifyWapMMSC;
    }

    public static int getMaxSizeScaleForPendingMmsAllowed() {
        return mMaxSizeScaleForPendingMmsAllowed;
    }

    public static boolean isAliasEnabled() {
        return mAliasEnabled;
    }

    public static int getAliasMinChars() {
        return mAliasRuleMinChars;
    }

    public static int getAliasMaxChars() {
        return mAliasRuleMaxChars;
    }

    public static boolean getAllowAttachAudio() {
        return mAllowAttachAudio;
    }

    public static boolean getMonitorAsyncQuery() {
        return mMonitorAsyncQuery;
    }

    public static boolean getEnableVzwAppApn() {
        return ENABLE_VZWAPPAPN;
    }

    public static String getPhoneFeatureMms() {
        return mPhoneFeatureMMS;
    }
    
    public static boolean isVzmRTLEnabled() {
        return mVzmRTLEnabled;
    }
    
    public static boolean isFMSBEnabled() {
        return mFMSBEnabled;
    }

    public static long getFMSBInterval() {
        return mFMSBInterval;
    }

    public static long getMaxNativeHeap() {
    	return maxNativeHeap;
    }

	public static int getMaxTextLength() {
		return maxTextLength;
	}

    public static boolean dieOnOom() {
    	return dieOnOom;
    }
    

    public static boolean allowConvDiskCache() {
    	return allowConvDiskCache;
    }
    
    public static boolean getVzmImageEditorEnabled() {
        return vzmImageEditorEnabled;
    }

    public static void setPhoneFeatureMms(String newPhoneFeature) {
        mPhoneFeatureMMS = newPhoneFeature;
    }
    
    public static int getNetworkConnectivityMode() {
        return mNetworkConnectivityMode;
    }
    
    public static void setNetworkConnectivityMode(int newConnMode) {
        mNetworkConnectivityMode = newConnMode;
    }

    public static final void beginDocument(XmlPullParser parser, String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            ;
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " + parser.getName()
                    + ", expected " + firstElementName);
        }
    }

    public static final void nextElement(XmlPullParser parser) throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
            ;
        }
    }

    private static void loadMmsSettings(Context context, int resID) {
        XmlResourceParser parser = context.getResources().getXml(resID);

        try {
            beginDocument(parser, "mms_config");

            while (true) {
                nextElement(parser);
                String tag = parser.getName();
                if (tag == null) {
                    break;
                }
                String name = parser.getAttributeName(0);
                String value = parser.getAttributeValue(0);
                String text = null;
                if (parser.next() == XmlPullParser.TEXT) {
                    text = parser.getText();
                }

//                log.debug("#=> MmsInfo ==> tag={}, name={}, text={}", new Object[] { tag, value, text });

                if ("name".equalsIgnoreCase(name)) {
                    if ("bool".equals(tag)) {
                        // bool config tags go here
                        if ("enabledMMS".equalsIgnoreCase(value)) {
                            mMmsEnabled = "true".equalsIgnoreCase(text) ? 1 : 0;
                        } else if ("enabledTransID".equalsIgnoreCase(value)) {
                            mTransIdEnabled = "true".equalsIgnoreCase(text);
                        } else if ("enabledNotifyWapMMSC".equalsIgnoreCase(value)) {
                            mNotifyWapMMSC = "true".equalsIgnoreCase(text);
                        } else if ("aliasEnabled".equalsIgnoreCase(value)) {
                            mAliasEnabled = "true".equalsIgnoreCase(text);
                        } else if ("allowAttachAudio".equalsIgnoreCase(value)) {
                            mAllowAttachAudio = "true".equalsIgnoreCase(text);
                        } else if ("monitorAsyncQuery".equalsIgnoreCase(value)) {
                            mMonitorAsyncQuery = "true".equalsIgnoreCase(text);
                        }
                    } else if ("int".equals(tag)) {
                        // int config tags go here
                        if ("maxMessageSize".equalsIgnoreCase(value)) {
                            mMaxMessageSize = Integer.parseInt(text);
                        } else if ("maxTimedOutMessageSize".equalsIgnoreCase(value)) {
                            mMaxTimeOutMessageSize = Integer.parseInt(text);
                        } else if ("maxImageHeight".equalsIgnoreCase(value)) {
                            mMaxImageHeight = Integer.parseInt(text);
                        } else if ("maxImageWidth".equalsIgnoreCase(value)) {
                            mMaxImageWidth = Integer.parseInt(text);
                        } else if ("defaultMessagesPerThread".equalsIgnoreCase(value)) {
                            mDefaultMessagesPerThread = Integer.parseInt(text);
                        } else if ("minMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMinMessageCountPerThread = Integer.parseInt(text);
                        } else if ("maxMessageCountPerThread".equalsIgnoreCase(value)) {
                            mMaxMessageCountPerThread = Integer.parseInt(text);
                        } else if ("recyclerInterval".equalsIgnoreCase(value)) {
                            mRecyclerInterval = Integer.parseInt(text);
                        } else if ("recyclerDelayInterval".equalsIgnoreCase(value)) {
                            mRecyclerDelay = Integer.parseInt(text);
                        }else if ("recipientLimit".equalsIgnoreCase(value)) {
                            mRecipientLimit = Integer.parseInt(text);
                            if (mRecipientLimit < 0) {
                                mRecipientLimit = Integer.MAX_VALUE;
                            }
                        } else if ("httpSocketTimeout".equalsIgnoreCase(value)) {
                            mHttpSocketTimeout = Integer.parseInt(text);
                        } else if ("httpReadTimeout".equalsIgnoreCase(value)) {
                            mHttpReadTimeout = Integer.parseInt(text);
                        } else if ("minimumSlideElementDuration".equalsIgnoreCase(value)) {
                            mMinimumSlideElementDuration = Integer.parseInt(text);
                        } else if ("maxSizeScaleForPendingMmsAllowed".equalsIgnoreCase(value)) {
                            mMaxSizeScaleForPendingMmsAllowed = Integer.parseInt(text);
                        } else if ("aliasMinChars".equalsIgnoreCase(value)) {
                            mAliasRuleMinChars = Integer.parseInt(text);
                        } else if ("aliasMaxChars".equalsIgnoreCase(value)) {
                            mAliasRuleMaxChars = Integer.parseInt(text);
                        } else if ("monitorAsyncQueryTimeout".equalsIgnoreCase(value)) {
                            mMonitorAsyncQueryTimeout = Integer.parseInt(text);
                        } 
                    } else if ("string".equals(tag)) {
                        // string config tags go here
                        if ("userAgent".equalsIgnoreCase(value)) {
                            mUserAgent = text;
                        } else if ("uaProfTagName".equalsIgnoreCase(value)) {
                            mUaProfTagName = text;
                        } else if ("uaProfUrl".equalsIgnoreCase(value)) {
                            mUaProfUrl = text;
                        } else if ("httpParams".equalsIgnoreCase(value)) {
                            mHttpParams = text;
                        } else if ("httpParamsLine1Key".equalsIgnoreCase(value)) {
                            mHttpParamsLine1Key = text;
                        } else if ("emailGatewayNumber".equalsIgnoreCase(value)) {
                            mEmailGateway = text;
                        }
                    }
                }
            }
        } catch (Exception e) {
        	Logger.error(MmsConfig.class, e, "loadMmsSettings caught");
        } finally {
            parser.close();
        }

        String errorStr = null;

        if (getMmsEnabled() && mUaProfUrl == null) {
            errorStr = "uaProfUrl";
        }

        if (errorStr != null) {
            String err = String.format("MmsConfig.loadMmsSettings mms_config.xml missing %s setting",
                    errorStr);
            Logger.error(MmsConfig.class, err);
            throw new ContentRestrictionException(err);
        }
    }

}
