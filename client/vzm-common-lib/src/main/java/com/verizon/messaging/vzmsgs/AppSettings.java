/**
 * AppSettings.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs;

import java.util.HashSet;

import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;

import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.sync.UiNotification;

/**
 * This class/interface
 * 
 * @author Jegadeesan M
 * @Since Oct 1, 2012
 */
public interface AppSettings {
	// on QA VMA is using 8888 number
    public static final String VZW_SERVICEMSG_SENDER_NO_QA = "8888";
    public static final String VZW_SERVICEMSG_SENDER_NO = "5555";
    public static final String VZW_VTEXT_UNVERIFIED_SENDER = "6245";
    //Some android 2.2 devices(Droid pro) will not send the sender id as 5555.
    public static final String VZW_SERVICEMSG_SENDER_TEXT = "VZW";
    public static HashSet<String> SEND_VMA_MSG_ID_CACHE= new HashSet<String>();

    public static final String KEY_VMA_NOTVZWMDN = "vma.status.notvzwmdn";
    public static final String KEY_VMA_NOTELIGIBLE = "vma.status.notvzEligible";
    public static final String KEY_VMA_PROVISIONED = "vma.status.provision";
    public static final String KEY_APP_FIRST_LAUNCH = "app.first.launch";
    public static final String KEY_OLDLOGINTOKEN = "vma.last.loginToken";
    public static final String KEY_LAST_PUSH_ID = "vma.last.gcmid";
    public static final String KEY_VMA_TOKEN = "vma.login.token";
    public static final String KEY_VMA_KEY = "vma.login.key";
    public static final String KEY_VMA_MDN = "vma.mdn";
    public static final String KEY_DEVICE_ID= "vma.key.device.id";
    /**
     * BZ 3584, mdn last entered was null when relaunching the Provisiong Screen after some time so storing
     * last entered MDN in case mdn is null
     */
    public static final String KEY_VMA_ENTERD_MDN = "vma.mdn.editTextEntered";
    public static final String KEY_VMA_AUTOFWD = "vma.autoForward";
    public static final String KEY_VMA_AUTO_REPLY = "vma.autoReply";
    public static final String KEY_VMA_SYNC_OVER_WIFI = "vma.usewifi";
    public static final String KEY_VMA_DONT_SHOW_DIALOG = "vma.dialog.integratedMessaging";
    public static final String KEY_VMA_HANDSET_PROVISIONING_IN_BACKROUND = "vma.handset.provision_inBackground";
    public static final String KEY_VMA_AUTOFORWARDADDR = "autoForwardAddr";
    public static final String KEY_VMA_AUTOFORWARDUSEDADDR = "autoForwardUsedAddr";
    public static final String KEY_VMA_AUTOFORWARDENDDATE = "autoForwardEndDate";
    public static final String KEY_VMA_AUTOREPLYSTATUS = "autoReplyStatus";
    public static final String KEY_VMA_AUTOREPLYMSG = "autoReplyMsg";
    public static final String KEY_VMA_AUTOREPLYENDDATE = "autoReplyEndDate";
    public static final String KEY_VMA_AUTOREPLYUSEDMSGS = "autoReplyUsedMsgs";
    public static final String KEY_VMA_AUTOFORWARDSTATUS = "autoForwardStatus";
    public static final String KEY_VMA_SERVER_TYPE = "vma.sever.type";
    public static final String KEY_WELCOME_MSG = "vma.welcome.msg";
    public static final String KEY_CANT_SEND_MESSAGE = "vma.cantSendMsg";
    public static final String KEY_VMA_SHOW_PROVISION_SERVICE = "vma.provisionServiceHandset";

    public static final String KEY_VMA_LAST_BUILD_NUMBER = "vma.last.build.number";
    public static final String KEY_VMA_CURRENT_BUILD_NUMBER = "vma.current.build.number";

    public static final Uri SETTINGS_URI = Uri.parse("content://vma/settings");
    public static final int BUILD_QA = 0;
    public static final int BUILD_PROD_IMAP_DEBUG = 1;
    public static final int BUILD_PROD = 2;
    public static final int BUILD_RELEASE = 3;
    public static final int VMA_SUBSCRIBED = 1;
    public static final int VMA_NOT_PROVISIONED = 1;

    public static final String KEY_FULLSYNC_COMPLETED = "vma.fullsync.completed";
    public static final String KEY_SMS_CHECKSUM_COMPLETED = "vma.sms.checksum.completed";

    public static final String KEY_FULLSYNC_LAST_UID = "vma.fullsync.last.uid";
    public static final String KEY_FASTSYNC_LAST_MAX_XMCR = "vma.fastsync.maxuid";
    public static final String KEY_FULLSYNC_MSG_COUNT = "vma.fullsync.msgcount";
    public static final String KEY_VMA_TAB_OFFLINE_MODE = "vma.offline.mode";
    public static final String VMA_CUSTOM_WELCOME_MSG = "Welcome message for VMA service. Click on Subscribe button above to register for VMA service";
    public static final String KEY_APP_INSTALL_TIME = "vzm.install.time";
    public static final String SHOW_WELCOME_SCREEN = "vma.show.welcomeScreen";
    public static final String KEY_VMA_ACCEPT_TERMS = "vz.accept_tnc2";
    @Deprecated
    public static final String KEY_OUR_MAX_XMCR = "vma.ourmax.xmcr";

    public static final String KEY_OUR_MAX_PMCR = "vma.ourmax.xmcr";
    public static final String KEY_OUR_MAX_SMCR = "vma.ourmax.smcr";

    public static final String KEY_FULLSYNC_DUMP_UIDS = "vma.dump.fullsync.uids";

    public static final String KEY_VMA_ACCOUNT_SUSPENDED = "vma.account.suspended";

    public static final String KEY_AUTOFORWARD_SYNC_ANCHOR = "vma.auto.forward.syncanchor";
    public static final String KEY_AUTOREPLY_SYNC_ANCHOR = "vma.auto.reply.syncanchor";

    public static final String EXTRA_AUTO_FORWARD_SYNCANCHOR = "vma.extra.fwd.syncanchor";
    public static final String EXTRA_AUTO_REPLY_SYNCANCHOR = "vma.extra.reply.syncanchor";

    public static final String KEY_DEBUG_VMA_STATUSBAR_NOTIFICATION = "vma.debug.enable.notification";

    public static final String VMA_ACTIVATION_WELCOME_MESSAGE = "VZW Free Msg: Welcome to Integrated Messaging! Starting now,";
    public static final String VMA_DEVICE_WELCOME_MESSAGE = "VZW Free Msg: Your device has been successfully registered with your Integrated Messaging account";
    
//    public static final String VMA_ACTIVATION_WELCOME_MESSAGE_02 = "VZW Free Msg: Welcome to Integrated Messaging! Starting now, your messages will be synced across your Verizon Messages devices-including phone, web & tablet access! Visit www.vzw.com/vzmessages for more info.";
    // Android 2.2 devices will remove the prefix in SMS for VZW service message the above one is original messgae 
//    public static final String VMA_ACTIVATION_WELCOME_MESSAGE_02 = "VZW Free Msg: Welcome to Integrated Messaging! Starting now, your messages will be synced across your Verizon Messages devices-including phone, web & tablet access! Visit www.vzw.com/vzmessages for more info.";
    
    
    public static final String KEY_PUSH_GCM_TOKEN_TOSEND = "vma.push.gcm.token.tosend";
    // debug 
    public static final String KEY_VMA_SIMULATE_ERROR = "vma.simulate.error";
    public static final String KEY_VMA_SIMULATE_PROVISIONING_ERROR = "vma.simulate.provision.error";
    

    public enum AutoForwardStatus {
        PENDING, ACTIVE, INACTIVE, EXPIRED, STOPPED
    };

    public enum AutoReplyStatus {
        ACTIVE, INACTIVE, EXPIRED, STOPPED
    }

    public int resetTablet();

    public int resetVMASettings();

    public String getLocalPhoneNumber();

    public void createOrUpdateMSASMSMapping(Uri smsUri);

    public String getMDN();

    public String getDecryptedLoginToken();

    /**
     * This Method
     * 
     * @return
     */
    public Context getContext();

    /**
     * This Method
     * 
     * @return
     */
    public boolean isSyncOverWifiEnabled();

    public boolean isProductionBuild();

    public boolean isAppFirstLaunch();

    public void setAppFirstLaunch(boolean isFirstLaunch);

    public void setTabletInOfflineMode(boolean offlinemode);

    public boolean isProvisioned();

    public boolean isIMAPLogEnabled();

    public String getImapHost();

    public int getImapPort();

    public boolean isSSLEnabled();
//
//    /**
//     * This Method is used to identify the application foreground or background.
//     * 
//     * @return {@link Boolean}
//     */
    public boolean isApplicationInBackground();
    
    
//
//    /**
//     * This Method is used to update the visible activity count of this application.
//     */
//    public void activityStarted();
//
//    /**
//     * This Method is used to update the visible activity count of this application.
//     */
//    public void activityStopped();


    public VMAEventHandler getVMAEventHandler();

    public void setVMAEventHandler(VMAEventHandler vmaEventHandler);

    public SyncItemDao getSyncItemDao();

    public void setSyncItemDao(SyncItemDao syncItemDao);

    public void setUiNotificationHandler(UiNotification notification);

    public UiNotification getUiNotificationHandler();

    public void resetHandset() ;


    /**
     * This Method
     * 
     * @param mmsUri
     */
    public void createOrUpdateMSAMmsMapping(Uri mmsUri);

    public int getIntSetting(String key);

    public long getLongSetting(String key);

    public boolean getBooleanSetting(String key);

    public String getStringSettings(String key);

    public int getIntSetting(String key, int defaultValue);

    public long getLongSetting(String key, long defaultValue);

    public boolean getBooleanSetting(String key, boolean defaultValue);

    public String getStringSettings(String key, String defaultValue);

    public void put(String key, String value);

    public void put(String key, long value);

    public void put(String key, int value);

    public void put(String key, boolean value);

    public void removeSettings(String key);
    
    public boolean isPhoneLocked();
    
    public boolean isKeyguardGuardLocked();
    
    public boolean isRunningOnLowBattery();
    
    public PowerManager getPowerManager();

    /**
     * This Method 
     * @return
     */
    public boolean isMdnChanged();

    /**
     * This Method 
     * @return
     */
    public boolean isVMAErrorSimulationEnabled();
    
    
    public boolean isVMAAccountSuspended();
    
    public void setVMAAccountSuspended(boolean suspended);
    
    public boolean isVMALoginFailed();
    
    public void setVMALoginFailed(boolean loginFailed);
    
    /**
     * This Method is used to know the status of the screen to start or stop the sync  
     * @param isForeground true  if app in foreground 
     * @param screenLocked  true  if  activity has focus
     */
    public void appStatusChanged(boolean isForeground, boolean screenLocked);
    
    

}
