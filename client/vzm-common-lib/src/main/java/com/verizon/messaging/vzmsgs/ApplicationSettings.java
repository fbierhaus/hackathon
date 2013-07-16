/**
 * BuildConfig.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs;

import java.io.IOException;
import java.util.Hashtable;
import java.util.zip.CRC32;

import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.common.security.AESEncryption;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.util.RecipientAddressUtil;
import com.verizon.mms.util.RecipientAddressUtil.Flag;
import com.verizon.mms.util.RecipientAddressUtil.RecipientAddress;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.sync.SyncController;
import com.verizon.sync.SyncManager;
import com.verizon.sync.UiNotification;

/**
 * This class is used to configure the build settings.
 * 
 * @author Jegadeesan.M
 * @Since Sep 21, 2012
 */
public class ApplicationSettings implements AppSettings {

    private static final String META_VZMSGS_BUILD_TYPE = "com.vzmsgs.build";
    private static final String META_VZW_APP_APN_ENABLED = "vzw.app.apn.enabled";
    public static final String[] VALUE_PROJECTION = new String[] { Settings._VALUE };

    // Manifest value
    private static boolean USE_PROD_CONFIG;
    private int buildType;
    private boolean isVzwappapnEnabled;
    private boolean vmaAccountSuspended;
    private boolean vmaLoginFailed;
    private boolean applicationInBackground;

    // Phone status
    private boolean phoneLocked;
    private boolean lowBattery;

    private static ApplicationSettings instance;
    private Context context;
    private ContentResolver resolver;
    private SharedPreferences preferences;

    private boolean enableImapLog;
    private int visibleActivityCount;
    private static RecipientAddressUtil recipientAddressUtil;
    private static String areaCode = null;
    private long currentBuildNo;
    private long lastBuildNo;
    private long manifestBuildNo;

    private VMAEventHandler vmaEventHandler;
    private UiNotification notification;
    private SyncItemDao syncItemDao;
    private Object pduDao;
    private Hashtable<String, String> cache;
    // private String privateKey;
    private PowerManager powerManager;

    private KeyguardManager keyguardManager;
    private TelephonyManager telephonyManager;
    private SyncController syncController;
    
    
    public Object getPduDao() {
        return pduDao;
    }

    public void setPduDao(Object pduDao) {
        this.pduDao = pduDao;
    }

    public static ApplicationSettings getInstance(Context context) {
        if (instance == null) {
            try {
                instance = new ApplicationSettings(context);
            } catch (Exception e) {
                Logger.error(e);
            }
        }
        return instance;
    }

    public static ApplicationSettings getInstance() {
        return instance;
    }

    /**
     * 
     * Constructor
     * 
     * @throws NameNotFoundException
     */
    private ApplicationSettings(Context context) throws NameNotFoundException {
        // Read from manifest meta data file
        this.context = context;
        cache = new Hashtable<String, String>();
        resolver = context.getContentResolver();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                PackageManager.GET_META_DATA);
        buildType = BUILD_RELEASE;
        buildType = ai.metaData.getInt(META_VZMSGS_BUILD_TYPE);
        // privateKey = ai.metaData.getString(META_PRIVATE_KEY);
        isVzwappapnEnabled = ai.metaData.getBoolean(META_VZW_APP_APN_ENABLED);

        if (buildType == BUILD_QA) {
            USE_PROD_CONFIG = getBooleanSetting(KEY_VMA_SERVER_TYPE, false);
            enableImapLog = true;
        } else if (buildType == BUILD_PROD_IMAP_DEBUG) {
            USE_PROD_CONFIG = getBooleanSetting(KEY_VMA_SERVER_TYPE, true);
            enableImapLog = true;
        } else if (buildType == BUILD_PROD) {
            // Default , relasee build
            enableImapLog = false;
            USE_PROD_CONFIG = true;
        } else {
            // Default , relasee build
            enableImapLog = false;
            USE_PROD_CONFIG = true;
        }
        initDefaultSettings();
        recipientAddressUtil = RecipientAddressUtil.getInstance();
    }

    /**
     * This Method
     */
    private void initDefaultSettings() {
        loadSettingOnCache();
        syncController = SyncController.getInstance(this);
        // verifying the build no
        try {
            // 4.0-20130222-0736-prod-148
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (info.versionName != null) {
                String[] value = info.versionName.split("-", 5);
                manifestBuildNo = Long.valueOf(value[4]);
                // for production we have to hardcode the number here
            	//manifestBuildNo = 257;
            }
        } catch (Exception e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error(e);
            }
        }

        lastBuildNo = getLongSetting(KEY_VMA_LAST_BUILD_NUMBER, 0);
        currentBuildNo = getLongSetting(KEY_VMA_CURRENT_BUILD_NUMBER, 0);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(" Manifest Build No: " + manifestBuildNo);
            Logger.debug(" Current Build  No: " + currentBuildNo);
            Logger.debug(" Last Build     No: " + lastBuildNo);
        }
        if (manifestBuildNo != currentBuildNo) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Application upgraded : updating the old build no.");
            }
            put(KEY_VMA_LAST_BUILD_NUMBER, currentBuildNo);
            put(KEY_VMA_CURRENT_BUILD_NUMBER, manifestBuildNo);
            currentBuildNo = manifestBuildNo;
        }

    }

    /**
     * This Method
     * 
     */
    public void dumpSettings() {
        Cursor c = SqliteWrapper.query(context, resolver, SETTINGS_URI, null, null, null, null);
        if (c != null) {
            try {
                Log.d("VZMLogger", "=======================================");
                Log.d("VZMLogger", "dumping uri=" + SETTINGS_URI);
                while (c.moveToNext()) {
                    Log.d("VZMLogger",
                            "Settings:k=" + c.getString(c.getColumnIndex("key")) + ",v="
                                    + c.getString(c.getColumnIndex("value")));
                }
                Log.d("VZMLogger", "=======================================");
            } finally {
                c.close();
            }
        }
    }

    public boolean isProvisioned() {
        return getIntSetting(KEY_VMA_PROVISIONED) == 1 && !getBooleanSetting(KEY_VMA_TAB_OFFLINE_MODE, false);
    }

    /**
     * Set the Value of the field context
     * 
     * @param context
     *            the context to set
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * This Method
     * 
     * @param userName
     * @param password
     */

    public void switchServer(boolean useProduction) {
        context.stopService(new Intent(SyncManager.ACTION_STOP_VMA_SYNC));
        SqliteWrapper.delete(context, resolver, SETTINGS_URI, null, null);
        USE_PROD_CONFIG = useProduction;
        put(KEY_VMA_SERVER_TYPE, useProduction);
        System.exit(1);
    }

    public int getVMAServicePort() {
        return USE_PROD_CONFIG ? 8443 : 80;
    }

    public String getVMAServiceHost() {
        return USE_PROD_CONFIG ? "web.vma.vzw.com" : "vmaqaui.pdi.vzw.com";
    }

    /**
     * Returns the Value of the resolver
     * 
     * @return the {@link ContentResolver}
     */
    public ContentResolver getContentResolver() {
        return resolver;
    }

    public String getMMSCHostURL() {
        return USE_PROD_CONFIG ? "http://mms.vtext.com/servlets/mms" : "http://vzpix.com/servlets/mms";
    }

    public String getImapHost() {
        return USE_PROD_CONFIG ? "imap.vma.vzw.com" : "vmaqaui.pdi.vzw.com";
    }

    public int getImapPort() {
        return USE_PROD_CONFIG ? 993 : 8143;
    }

    public boolean isSSLEnabled() {
        return USE_PROD_CONFIG ? true : false;
    }

    public boolean isVZWAPPAPNEnabled() {
        return USE_PROD_CONFIG ? true : false;
    }

    /**
     * This Method
     * 
     * @return
     */
    public static boolean isVZMMSLabEnabled() {
        return USE_PROD_CONFIG ? false : true;
    }

    /**
     * Returns the Value of the enableImapLog
     * 
     * @return the {@link boolean}
     */
    public boolean isIMAPLogEnabled() {
        return enableImapLog;
    }

    /**
     * Returns the Value of the currentBuildNo
     * 
     * @return the {@link long}
     */
    public long getCurrentBuildNo() {
        return currentBuildNo;
    }

    /**
     * Returns the Value of the lastBuildNo
     * 
     * @return the {@link long}
     */
    public long getLastBuildNo() {
        return lastBuildNo;
    }

    /**
     * Returns the Value of the lastBuildNo
     * 
     * @return the {@link long}
     */
    public long getManifestBuildNo() {
        return manifestBuildNo;
    }

    /**
     * Returns the Value of the phoneNumber
     * 
     * @return the {@link String}
     */
    public String getLocalPhoneNumber() {
        if (telephonyManager == null) {
            telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        }
        return telephonyManager.getLine1Number();
    }

    /**
     * This API is used to set Auto-Forward feature. URL http://vmadevui.pdi.vzw.com/services/AssistantAutoFwd
     * (lab) Method GET Parameter mdn loginToken autoForwardAddr Auto-forward address autoForwardEndDate
     * Auto-forward end date Result (JSON) status OK provisioning succeeds, FAIL login fails, VBLOCK user is
     * blocked from using this service, ERROR error statusInfo optional, detailed info about status: Invalid
     * Address auto-forward address is invalid Invalid End Date end date is invalid
     */
    public boolean isAutoForwardEnabled() {
        return getBooleanSetting(AppSettings.KEY_VMA_AUTOFWD, false);
    }

    /**
     * This API is used to set Auto-Forward feature. URL
     * http://vmadevui.pdi.vzw.com/services/AssistantAutoReply (lab) Method GET Parameter mdn loginToken
     * autoReplyMsg Auto-reply message autoReplyEndDate Auto-reply end date Result (JSON) status OK
     * provisioning succeeds, FAIL login fails, VBLOCK user is blocked from using this service, ERROR error
     * statusInfo optional, detailed info about status: Message Too Long message is longer than 160 characters
     * Invalid End Date end date is invalid
     * 
     * @return {@link Boolean}
     */
    public boolean isAutoReplyEnabled() {
        return getBooleanSetting(AppSettings.KEY_VMA_AUTO_REPLY, false);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#createSMSCMapping(android.net.Uri)
     */
    @Override
    public void createOrUpdateMSASMSMapping(Uri smsUri) {
        synchronized (SyncController.getInstance()) {
            String[] projection = new String[] { Sms.THREAD_ID, Sms.DATE, Sms.ADDRESS, Sms.BODY };
            Cursor cursor = SqliteWrapper.query(context, resolver, smsUri, projection, null, null, null);
            if (cursor != null) {
                try {
                    long threadId = 0;
                    long date = 0;
                    String address = null;
                    String body = null;
                    if (cursor.moveToFirst()) {
                        threadId = cursor.getLong(0);
                        date = cursor.getLong(1);
                        address = ApplicationSettings.parseAdddressForChecksum(cursor.getString(2));
                        body = cursor.getString(3);
                        long luid = ContentUris.parseId(smsUri);
                        vmaEventHandler.telephonySMSSend(luid, threadId, body, address, date);
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#updateMmsVMAMapping(android.net.Uri)
     */
    @Override
    public void createOrUpdateMSAMmsMapping(Uri mmsUri) {
        String[] projection = new String[] { Mms.THREAD_ID, Mms.DATE, Mms.MESSAGE_ID };
        Cursor cursor = SqliteWrapper.query(context, resolver, mmsUri, projection, null, null, null);
        if (cursor != null) {
            try {
                long threadId = 0;
                long date = 0;
                String msgId = null;
                if (cursor.moveToFirst()) {
                    threadId = cursor.getLong(0);
                    date = cursor.getLong(1);
                    msgId = cursor.getString(2);
                    vmaEventHandler.telephonyMMSReceive(ContentUris.parseId(mmsUri), threadId, msgId, date);
                }
            } finally {
                cursor.close();
            }
        }
    }

    public static long computeCheckSum(byte[] buf) throws IOException {
        long checkSum = -1;
        CRC32 crc32 = new CRC32();
        crc32.update(buf);
        checkSum = crc32.getValue();
        return checkSum;
    }

    /**
     * This Method
     * 
     * @return
     */
    public String getMDN() {
        return getStringSettings(KEY_VMA_MDN);
    }

    /**
     * This Method is used to get the decrypted login token.
     * 
     * @return
     * @throws Exception
     */
    public String getDecryptedLoginToken() {
        try {
            String key = getStringSettings(KEY_VMA_KEY);
            return AESEncryption.decrypt(key.getBytes(), getStringSettings(KEY_VMA_TOKEN));
        } catch (Exception e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("faild to getDecryptedLoginToken:", e);
            }
        }
        return null;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getContext()
     */
    @Override
    public Context getContext() {
        return context;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isSyncOverWifiEnabled()
     */
    @Override
    public boolean isSyncOverWifiEnabled() {
        return getBooleanSetting(KEY_VMA_SYNC_OVER_WIFI, false);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isProductionBuild()
     */
    @Override
    public boolean isProductionBuild() {
        return USE_PROD_CONFIG;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#setTabletInOfflineMode(boolean)
     */
    @Override
    public void setTabletInOfflineMode(boolean offlinemode) {
        put(KEY_VMA_TAB_OFFLINE_MODE, offlinemode);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isAppFirstLaunch()
     */
    @Override
    public boolean isAppFirstLaunch() {
        return preferences.getBoolean(KEY_APP_FIRST_LAUNCH, true);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#setAppFirstLaunch(boolean)
     */
    @Override
    public void setAppFirstLaunch(boolean isFirstLaunch) {
        preferences.edit().putBoolean(KEY_APP_FIRST_LAUNCH, isFirstLaunch).commit();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isApplicationInBackground()
     */
    @Override
    public synchronized boolean isApplicationInBackground() {
        return applicationInBackground;
    }

    //
    // /*
    // * Overriding method (non-Javadoc)
    // *
    // * @see com.verizon.messaging.vzmsgs.AppSettings#activityStarted()
    // */
    // @Override
    // public void activityStarted() {
    // visibleActivityCount++;
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug("activityStarted():Activity visible count=" + visibleActivityCount);
    // }
    // if (visibleActivityCount > 0) {
    // syncController.setAppInBackground(false);
    // if (!receiversRegistered) {
    // registerReceivers();
    // }
    // }
    // }

    // /*
    // * Overriding method (non-Javadoc)
    // *
    // * @see com.verizon.messaging.vzmsgs.AppSettings#activityStoped()
    // */
    // @Override
    // public void activityStopped() {
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug("Activity visible count=" + visibleActivityCount);
    // }
    // visibleActivityCount--;
    // if (visibleActivityCount == 0) {
    // syncController.setAppInBackground(true);
    // if (receiversRegistered) {
    // unregisterReceivers();
    // }
    // } else {
    // syncController.setAppInBackground(false);
    // }
    // }

    /*
     * Parses the adress and builds the checksum based on this algorithm 1. Strip off all non digits other
     * than the first '+' . Then modify the remaining string using the following algorithm: 1. 0111xxxxxxxxxx
     * to xxxxxxxxxx (remove leading 0111, treat it as domestic/north America number) 2. 011byyyyy to
     * 011byyyyy (b is not “1”, number of y’s may change. No conversion) 3. +1xxxxxxxxxx to xxxxxxxxxx (remove
     * leading +1, treat it as domestic/north America number) 4. +byyyyy to 011byyyyy (b is not “1”, number of
     * y’s may change. Add “011” in the front) 5. 1xxxxxxxxxx to xxxxxxxxxx (remove leading “1”) 6. xxxxxxx to
     * aaaxxxxxxx (where “aaa” is subscriber mdn’s area code) 7. 3,4,5,6,12 digit number to (long code, no
     * conversation) 8. Invalid recipient address
     */
    public static String parseAdddressForChecksum(String address) {
        String retAdd = address;

        RecipientAddress recipAddress = recipientAddressUtil.parse(address, Flag.ALL, true, getAreaCode());
        if (recipAddress.isValid()) {
            retAdd = recipAddress.getNormalized();
        }

        Logger.debug("parseAddressFroChecksum old address " + address + " parsed address" + retAdd);
        return retAdd;
    }

    public static String getAreaCode() {
        if (areaCode == null) {
            String mdn = instance.getMDN();

            if (mdn == null && !MmsConfig.isTabletDevice()) {
                // mdn might not have been saved into settings yet so fetch it from telephony manager only
                // for handsets
                mdn = instance.getLocalPhoneNumber();
            }
            if (!TextUtils.isEmpty(mdn)) {
                Logger.debug("ApplicationSettings.getAreaCode(): original mdn: " + mdn);
                if (mdn.length() > 10) {
                    RecipientAddress recipAddress = recipientAddressUtil
                            .parse(mdn, Flag.MDN_ONLY, true, null);

                    if (recipAddress.isValid()) {
                        mdn = recipAddress.getNormalized();
                        Logger.debug("ApplicationSettings.getAreaCode():  processed mdn " + mdn);
                    }
                }
                // parsed address has to be 10 digits
                areaCode = mdn.substring(0, 3);
            } else {
                Logger.debug("ApplicationSettings.getAreaCode():  no mdn value present");
                areaCode = null;
            }
        }

        return areaCode;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getVMAEventHandler()
     */
    @Override
    public VMAEventHandler getVMAEventHandler() {
        return vmaEventHandler;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see
     * com.verizon.messaging.vzmsgs.AppSettings#setVMAEventHandler(com.verizon.messaging.vzmsgs.provider.dao
     * .VMAEventHandler)
     */
    @Override
    public void setVMAEventHandler(VMAEventHandler vmaEventHandler) {
        this.vmaEventHandler = vmaEventHandler;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getSyncItemDao()
     */
    @Override
    public SyncItemDao getSyncItemDao() {
        return syncItemDao;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#setSyncItemDao(com.verizon.messaging.vzmsgs.provider.dao.
     * SyncItemDao)
     */
    @Override
    public void setSyncItemDao(SyncItemDao syncItemDao) {
        this.syncItemDao = syncItemDao;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#setUiNotificationHandler(com.verizon.sync.UINotification)
     */
    @Override
    public void setUiNotificationHandler(UiNotification notification) {
        this.notification = notification;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getUiNotificationHandler()
     */
    @Override
    public UiNotification getUiNotificationHandler() {
        return notification;
    }

    /**
     * This Method
     */
    public void resetHandset() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ApplicationSettings.class, "resetHandset()");
        }
        SyncController.getInstance().stopVMASync();
        resetVMAMapping();
        resetCache();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#resetVMAHandset()
     */
    @Override
    public int resetVMASettings() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ApplicationSettings.class, "resetVMASettings()");
        }
        String oldGCMId = getStringSettings(KEY_LAST_PUSH_ID);
        int count = resetVMAMapping();
        SyncController.getInstance().stopVMASync();
        resetCache();
        if (oldGCMId != null) {
            put(KEY_LAST_PUSH_ID, oldGCMId);
        }
        vmaAccountSuspended = false;
        vmaLoginFailed = false;
        return count;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#resetTablet()
     */
    @Override
    public int resetTablet() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ApplicationSettings.class, "resetTablet()");
        }
        String oldGCMId = getStringSettings(KEY_LAST_PUSH_ID);
        String selection = " locked >= 0 ";
        int count = SqliteWrapper.delete(context, resolver, VZUris.getMmsSmsConversationUri(), selection,
                null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ApplicationSettings.class, " Deleted all the conversation:count=" + count);
        }
        selection = null;
        count = resetVMAMapping();
        // Unregister the GCM receiver. To clean up the old tokens
        GCMRegistrar.unregister(context);

        SyncController.getInstance().stopVMASync();

        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
        
        resetCache();

        if (oldGCMId != null) {
            put(KEY_LAST_PUSH_ID, oldGCMId);
        }
        return count;
    }

    /**
     * This Method
     * 
     * @param selection
     * @return
     */
    private int resetVMAMapping() {
        int count = 0;
        // SyncItem tables
        count = SqliteWrapper.delete(context, resolver, SyncItem.CONTENT_URI, SyncItem._ID + " >=0", null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ApplicationSettings.class, " Cleared the pending items:count=" + count);
        }
        // clearing settings table
        count = SqliteWrapper.delete(context, resolver, SETTINGS_URI, null, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ApplicationSettings.class, "Cleared the settings: count=" + count);
        }

        // New VMAMapping
        count = SqliteWrapper.delete(context, resolver, VMAMapping.CONTENT_URI, null, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ApplicationSettings.class, "Cleared the mapping:count=" + count);
        }
        // Sync Status
        count = SqliteWrapper.delete(context, resolver, Uri.parse("content://vma/syncstatus"), null, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ApplicationSettings.class, "Cleared the fullsync status:count=" + count);
        }
        // RESET Send Message ID Cache 
        SEND_VMA_MSG_ID_CACHE.clear();

        return count;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#put(java.lang.String, java.lang.String)
     */
    @Override
    public void put(String key, String value) {
        saveSettings(key, String.valueOf(value));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#put(java.lang.String, long)
     */
    @Override
    public void put(String key, long value) {
        saveSettings(key, String.valueOf(value));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#put(java.lang.String, int)
     */
    @Override
    public void put(String key, int value) {
        saveSettings(key, String.valueOf(value));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#put(java.lang.String, boolean)
     */
    @Override
    public void put(String key, boolean value) {
        saveSettings(key, String.valueOf(value));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getIntSetting(java.lang.String)
     */
    @Override
    public int getIntSetting(String key) {
        return Integer.valueOf(getSettings(key, String.valueOf(0)));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getLongSetting(java.lang.String)
     */
    @Override
    public long getLongSetting(String key) {
        return Long.valueOf(getSettings(key, String.valueOf(0)));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getBooleanSetting(java.lang.String)
     */
    @Override
    public boolean getBooleanSetting(String key) {
        return Boolean.valueOf(getSettings(key, String.valueOf(false)));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getStringSettings(java.lang.String)
     */
    @Override
    public String getStringSettings(String key) {
        return getSettings(key, null);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getIntSetting(java.lang.String, int)
     */
    @Override
    public int getIntSetting(String key, int defaultValue) {
        return Integer.valueOf(getSettings(key, String.valueOf(defaultValue)));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getLongSetting(java.lang.String, long)
     */
    @Override
    public long getLongSetting(String key, long defaultValue) {
        return Long.valueOf(getSettings(key, String.valueOf(defaultValue)));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getBooleanSetting(java.lang.String, boolean)
     */
    @Override
    public boolean getBooleanSetting(String key, boolean defaultValue) {
        return Boolean.valueOf(getSettings(key, String.valueOf(defaultValue)));
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getStringSettings(java.lang.String, java.lang.String)
     */
    @Override
    public String getStringSettings(String key, String defaultValue) {
        return getSettings(key, defaultValue);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#removeSettings(java.lang.String)
     */
    @Override
    public synchronized void removeSettings(String key) {
        SqliteWrapper.delete(context, resolver, SETTINGS_URI, Settings._KEY + "=?", new String[] { key });
        cache.remove(key);
    }

    private synchronized String getSettings(String key, String defaultValue) {
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        return defaultValue;
    }

    private synchronized void saveSettings(String key, String value) {
        boolean updateRecord = cache.containsKey(key);
        // persisting on database
        if (updateRecord) {
            ContentValues values = new ContentValues(1);
            values.put(Settings._VALUE, value);
            SqliteWrapper.update(context, resolver, SETTINGS_URI, values, Settings._KEY + "=?",
                    new String[] { key });
        } else {
            ContentValues values = new ContentValues(2);
            values.put(Settings._VALUE, value);
            values.put(Settings._KEY, key);
            SqliteWrapper.insert(context, resolver, SETTINGS_URI, values);
        }
        // Loading to local cache
        cache.put(key, value);
    }

    /**
     * This Method
     */
    private synchronized void resetCache() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("resetCache():");
        }
        cache.clear();
        loadSettingOnCache();
    }

    /**
     * This Method
     */
    private synchronized void loadSettingOnCache() {
        // Loading the setting
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("loadSettingOnCache():");
        }
        Cursor c = SqliteWrapper.query(context, resolver, SETTINGS_URI, null, null, null, null);
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    String key = c.getString(c.getColumnIndex("key"));
                    String value = c.getString(c.getColumnIndex("value"));
                    cache.put(key, value);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Settings:k=" + key + ",v=" + value);
                    }
                }
            } finally {
                c.close();
            }
        }

    }

    private void onScreenOnOrOff(boolean locked) {
        // if (keyguardManager == null) {
        // keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        // }
        // boolean locked = keyguardManager.inKeyguardRestrictedInputMode();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Phone lock state from KEYGUARD_SERVICE: Current state:"
                    + (locked ? "LOCKED" : "UNLOCKED"));
        }
        // if (locked) {
        // // locked
        // if (!phoneLocked) {
        // // we dnt have to stop the vma sync again and aging before unlocking
        // syncController.setScreenMode(true);
        // }
        // } else {
        // // unlocked
        // if (phoneLocked) {
        // // phone previously locked and activity in foreground
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.debug("Phone was locked eairler. resuming the vma sync");
        // }
        // syncController.setScreenMode(false);
        // }
        // }
        phoneLocked = locked;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isPhoneLocked()
     */
    @Override
    public boolean isPhoneLocked() {
        return phoneLocked;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isRunningOnLowBattery()
     */
    @Override
    public boolean isRunningOnLowBattery() {
        return lowBattery;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#getPowerManager()
     */
    @Override
    public PowerManager getPowerManager() {
        if (powerManager == null) {
            powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            // boolean isScreenOn = powerManager.isScreenOn();
        }
        return powerManager;
    }

    // private class ScreenLockOrUnlockReceiver extends BroadcastReceiver {
    //
    // /*
    // * Overriding method (non-Javadoc)
    // *
    // * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
    // */
    // @Override
    // public void onReceive(Context context, Intent intent) {
    // if (Logger.IS_INFO_ENABLED) {
    // Logger.info(ScreenLockOrUnlockReceiver.class, " action=" + intent.getAction());
    // }
    // if (Intent.ACTION_SCREEN_ON.equalsIgnoreCase(intent.getAction())) {
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug(ScreenLockOrUnlockReceiver.class, "Screen ON");
    // }
    // // onScreenOnOrOff();
    // } else if (Intent.ACTION_SCREEN_OFF.equalsIgnoreCase(intent.getAction())) {
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug(ScreenLockOrUnlockReceiver.class, "Screen OFF");
    // }
    // onScreenOnOrOff(true);
    // } else if (Intent.ACTION_USER_PRESENT.equalsIgnoreCase(intent.getAction())) {
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug(ScreenLockOrUnlockReceiver.class, "ACTION_USER_PRESENT");
    // }
    // onScreenOnOrOff(false);
    // } else {
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug(ScreenLockOrUnlockReceiver.class, "Ignoring the action");
    // }
    //
    // }
    // }
    // }

    /**
     * This Method
     */
    private void registerReceivers() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "registerReceivers()");
        }
        /**
         * <receiver android:name="com.verizon.common.receiver.SystemEventReceiver" android:enabled="false">
         * <intent-filter> <action android:name="android.intent.action.BOOT_COMPLETED"/> <action
         * android:name="android.intent.action.BATTERY_LOW"/> <action
         * android:name="android.intent.action.BATTERY_OKAY"/> <action
         * android:name="android.intent.action.FACTORY_TEST"/> <action
         * android:name="android.intent.action.MEDIA_REMOVED"/> <action
         * android:name="android.intent.action.MEDIA_UNMOUNTED"/> <action
         * android:name="android.intent.action.PACKAGE_DATA_CLEARED"/> <action
         * android:name="android.intent.action.SCREEN_OFF"/> <action
         * android:name="android.intent.action.SCREEN_ON"/> <action
         * android:name="android.intent.action.LOCALE_CHANGED"/> <action
         * android:name="android.intent.action.AIRPLANE_MODE"/> </intent-filter> </receiver>
         **/
        // IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        // filter.addAction(Intent.ACTION_SCREEN_ON);
        // filter.addAction(Intent.ACTION_USER_PRESENT);
        // context.registerReceiver(screenLockOrUnlockReceiver, filter);
        // receiversRegistered = true;
    }

    // /**
    // * This Method
    // */
    // private void unregisterReceivers() {
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug(getClass(), "unregisterReceivers()");
    // }
    // try {
    // context.unregisterReceiver(screenLockOrUnlockReceiver);
    // } catch (Exception e) {
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.error(getClass(), "unregisterReceiver()", e);
    // }
    // } finally {
    // receiversRegistered = false;
    // }
    //
    // }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isMdnChanged()
     */
    @Override
    public boolean isMdnChanged() {
        if (isProvisioned()) {
            String localMdn = getLocalPhoneNumber();
            String vmaMDN = getMDN();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("isMdnChanged():Local mdn=" + localMdn + ",vma mdn=" + vmaMDN);
            }

            /*
             * Sometimes if telephony manager is not initialized we can get a null returned, so we cannot
             * depend on it to be sure that the number changed.
             * 
             * Hence we ignore the null case as a false positive.
             */
            if (!TextUtils.isEmpty(localMdn)) {
                localMdn = parseAdddressForChecksum(localMdn);
                if (vmaMDN != null) {
                    vmaMDN = parseAdddressForChecksum(vmaMDN);
                }
                if (!localMdn.equals(vmaMDN)) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("MDN changed returning true local=" + localMdn + " vma=" + vmaMDN);
                        Logger.postErrorToAcra("Handset MDN changed returning true,  local=" + localMdn
                                + " vma=" + vmaMDN);
                    }
                    return true;
                }
            }

        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("No changes in mdn returning false, either same or new is null.");
        }
        return false;
    }

    public boolean isReleaseBuild() {
        return buildType >= 3;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isVMAErrorSimulationEnabled()
     */
    @Override
    public boolean isVMAErrorSimulationEnabled() {
        return getIntSetting(KEY_VMA_SIMULATE_ERROR, 0) > 0;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isKeyguardGuardLocked()
     */
    @Override
    public boolean isKeyguardGuardLocked() {
        if (keyguardManager == null) {
            keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        }
        boolean locked = keyguardManager.inKeyguardRestrictedInputMode();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Phone lock state from KEYGUARD_SERVICE: Current state:"
                    + (locked ? "LOCKED" : "UNLOCKED"));
        }
        return locked;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isVMAAccountSuspended()
     */
    @Override
    public synchronized boolean isVMAAccountSuspended() {
        return vmaAccountSuspended;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#setVMAAccountSuspended(boolean)
     */
    @Override
    public synchronized void setVMAAccountSuspended(boolean suspended) {
        this.vmaAccountSuspended = suspended;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#isVMALoginFailed()
     */
    @Override
    public synchronized boolean isVMALoginFailed() {
        return vmaLoginFailed;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#setVMALoginFailed(boolean)
     */
    @Override
    public synchronized void setVMALoginFailed(boolean loginFailed) {
        this.vmaLoginFailed = loginFailed;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.AppSettings#activityOnInForeground(boolean, boolean)
     */
    @Override
    public void appStatusChanged(boolean isAppInBackground, boolean screenLocked) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("activityStatusChanged:isAppInBackground=" + isAppInBackground + ",appHasFocus="
                    + screenLocked + ",locked=" + isKeyguardGuardLocked());
        }
        synchronized (this) {
        	applicationInBackground = isAppInBackground;
        }
        SyncController.getInstance(this).onAppStatusChanged(isAppInBackground, screenLocked);
    }
}
