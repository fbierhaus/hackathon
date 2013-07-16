/**
o * ProvisionManager.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vma.provision;

import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOFORWARDADDR;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOFORWARDENDDATE;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOREPLYENDDATE;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOREPLYMSG;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOREPLYSTATUS;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOREPLYUSEDMSGS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.os.RemoteException;
import android.provider.Settings.Secure;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsMessage;

import com.google.android.gcm.GCMRegistrar;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.TransportManager;
import com.verizon.common.VZUris;
import com.verizon.common.security.AESEncryption;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.AppSettings.AutoReplyStatus;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.VMAProvision;
import com.verizon.messaging.vzmsgs.provider.ApplicationProvider;
import com.verizon.messaging.vzmsgs.provider.Vma.LinkedVMADevices;
import com.verizon.messaging.vzmsgs.provider.Vma.RecentlyUsedReplyMsg;
import com.verizon.messaging.vzmsgs.sync.AppErrorCodes;
import com.verizon.messaging.vzmsgs.sync.SyncStatusCode;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.sync.SyncManager;
import com.verizon.sync.SyncStatusListener;
import com.vzw.vma.common.message.StringUtils;
import com.vzw.vma.message.VMAStore;
import com.vzw.vma.sync.SmsChecksumBuilder;

/**
 * This class/interface
 * 
 * @author Jegadeesan M
 * @Since Sep 30, 2012
 */
public class ProvisionManager extends Binder implements VMAProvision, AppErrorCodes {

    private static final String UNABLE_TO_REMOVE_DEVICE = "Unable to remove device";
    private static final String JSON_MESSAGE = "message";
    private static final String JSON_NO = "NO";
    private static final String JSON_YES = "YES";
    private Context context;
    private static final String BASE_URI = "services";
    private static final String QUERY_URI = BASE_URI + "/" + "vma_user_query";
    private static final String LOGINTOKEN_URI = BASE_URI + "/" + "loginToken";
    private static final String LOGINPIN_TOKEN_URI = BASE_URI + "/" + "loginPINToken";
    private static final String PUSHID_URI = BASE_URI + "/" + "pushId";
    private static final String PROVISIONING_URI = BASE_URI + "/" + "vmaProvisioning";
    private static final String ASSISTANTAUTOFWD_URI = BASE_URI + "/" + "AssistantAutoFwd";
    private static final String ASSISTANTAUTOFWDDISABLE_URI = BASE_URI + "/" + "AssistantAutoFwdDisable";
    private static final String ASSISTANTAUTOREPLYDISABLE_URI = BASE_URI + "/" + "AssistantAutoReplyDisable";
    private static final String ASSISTANTAUTOREPLY_URI = BASE_URI + "/" + "AssistantAutoReply";
    private static final String ASSISTANTQUERY_URI = BASE_URI + "/" + "AssistantQuery";
    private static final String GENERATEPIN_URI = BASE_URI + "/" + "generatePIN";
    private static final String DEPROVISIONING_URI = BASE_URI + "/" + "vmaDeProvisioning";
    private static final String LINKEDDEVICES_URI = BASE_URI + "/" + "linkedDevices";
    private static final String REMOVEDEVICE_URI = BASE_URI + "/" + "removeDevice";
    private static final String REMOVESECONDARYDEVICES_URI = BASE_URI + "/" + "removeSecondaryDevices";
    private static final String WELCOMEMESSAGE_URI = BASE_URI + "/" + "welcomeMessage";

    private static final String JSON_STATUS = "status";
    private static final String JSON_STATUSINFO = "statusInfo";
    private static final String JSON_OK = "OK";

    private static final String JSON_ERROR = "ERROR";
    private static final String JSON_FAIL = "FAIL";

    private static final String JSON_INTERNAL_ERROR = "Internal Error";
    private static final String JSON_LOGIN_FAIL = "Login Fail";

    // error codes

    private static final String JSON_VBLOCK = "VBLOCK";
    private static final String JSON_SUSPENDED = "SUSPENDED";
    private static final String JSON_NOTELIGIBLE = "NOTELIGIBLE";
    private static final String JSON_NOTVZWMDN = "NOTVZWMDN";
    private static final String JSON_NOTEXT = "NOTEXT";
    private static final String JSON_NO_MMS = "NOMMS";
    private static final String JSON_OVERLIMIT = "OVERLIMIT";
    private static final String JSON_EXCEEDDEVICELIMIT = "EXCEEDDEVICELIMIT";

    private static final String JSON_INVALID_MDN = "INVALID_MDN";
    private static final String JSON_DUPLICATE_DESTINATION = "DUPLICATE_DESTINATION";
    private static final String JSON_ALREADY_AUTO_FORWARDED = "ALREADY_AUTO_FORWARDED";
    private static final String JSON_INVALID_END_DATE = "INVALID_END_DATE";

    private TransportManager manager;
    private String baseUrl;
    private String mdn;
    private String loginToken;
    private String pin;
    private ApplicationSettings settings;
    private Object lock;
    private boolean isPinReceived = false;
    private final SimpleDateFormat SDF_LINKED_DEVICE;

    public ProvisionManager(Context context) {
        this.context = context;
        settings = ApplicationSettings.getInstance();
        manager = new TransportManager(context);
        SDF_LINKED_DEVICE = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        baseUrl = ((settings.isSSLEnabled()) ? "https://" : "http://") + settings.getVMAServiceHost() + ":"
                + settings.getVMAServicePort();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#isVMASubscriber(java.lang.String)
     */
    @Override
    public int isVMASubscriber(String mdn) {
        try {
            sendStatus(SyncStatusCode.VMA_PROVISION_USER_QUERY);
            loginToken = settings.getDecryptedLoginToken();
            String url = baseUrl + "/" + QUERY_URI + "?mdn=" + mdn + "&loginToken=" + loginToken;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("isVMASubscriber():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            String response = new String(out.toByteArray()).trim();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Response:res=" + response);
            }
            if (JSON_YES.equalsIgnoreCase(response)) {
                settings.put(ApplicationSettings.KEY_VMA_PROVISIONED, String.valueOf(1));
                settings.put(ApplicationSettings.SHOW_WELCOME_SCREEN, true);
                startSync();
                return VMA_PROVISION_VMA_SUBSCRIBER;
            } else if (JSON_NO.equalsIgnoreCase(response)) {
                settings.put(ApplicationSettings.KEY_VMA_PROVISIONED, String.valueOf(0));
                if (!VZUris.isTabletDevice()) {
                    // if(settings.getBooleanSetting(AppSettings.KEY_VMA_ACCEPT_TERMS, false)){
                    // doProvision(mdn, loginToken);
                    // }
                    // else{
                    // settings.put(ApplicationSettings.RETRY_AGAIN_CREATE_MAILBOX, true);
                    // }
                    // getWelcomeMessage();
                    // int result =getWelcomeMessage();
                    // if(VMA_PROVISION_OK !=result){
                    // return result;
                    // }

                    // Add a welcome message
                    // ContentValues values = new ContentValues();
                    // values.put(Sms.ADDRESS, "8888");
                    // values.put(Sms.BODY, AppSettings.VMA_CUSTOM_WELCOME_MSG);
                    // values.put(Sms.TYPE, Sms.MESSAGE_TYPE_INBOX);
                    // values.put(Sms.DATE, System.currentTimeMillis());
                    // context.getContentResolver().insert(VZUris.getSmsInboxUri(), values);
                }
                return VMA_PROVISION_NOT_VMA_SUBCRIBER;
            } else {
                settings.removeSettings(ApplicationSettings.KEY_VMA_TOKEN);
                settings.put(ApplicationSettings.KEY_VMA_PROVISIONED, String.valueOf(0));
                return handleKnownServerError(response);
            }

        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("isVMASubscriber() failed:", e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        }

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#generatePIN(java.lang.String, java.lang.String,
     * boolean)
     */
    @Override
    public int generatePIN(String mdn, String deviceModel, boolean isPrimary) {
        try {
            sendStatus(SyncStatusCode.VMA_PROVISION_GENERATE_PIN);
            if (mdn == null) {
                return VMA_PROVISION_ERROR;
            }
            deviceModel = ((deviceModel != null) ? deviceModel : Build.MANUFACTURER + "-" + Build.MODEL);

            String url = baseUrl + "/" + GENERATEPIN_URI + "?mdn=" + URLEncoder.encode(mdn) + "&isPrimary="
                    + isPrimary + "&deviceType=ANDROID&deviceName=" + URLEncoder.encode(deviceModel);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("generatePIN():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            String response = new String(out.toByteArray());
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Response:res=" + response);
            }
            JSONObject res = new JSONObject(response);
            response = res.getString(JSON_STATUS);
            if (JSON_OK.equals(response)) {
                return VMA_PROVISION_OK;
            } else {
                if (res.has("statusinfo")) {
                    response = res.getString("statusinfo");
                }
                return handleKnownServerError(response);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.debug("generatePIN() failed:", e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.debug("generatePIN() failed:", e);
            }
        }
        return VMA_PROVISION_ERROR;
    }

    private void sendStatus(int status) {
        Intent intent = new Intent(SyncManager.ACTION_SYNC_STATUS);
        intent.putExtra(SyncManager.EXTRA_STATUS, status);
        context.sendBroadcast(intent);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#isValidLoginPIN(java.lang.String,
     * java.lang.String)
     */
    @Override
    public int isValidLoginPIN(String mdn, String password) {
        try {
            sendStatus(SyncStatusCode.VMA_PROVISION_VALIDATE_PIN);
            if (mdn == null || password == null) {
                return VMA_PROVISION_ERROR;
            }
            // {"status":"OK","mdn":"9253243014","loginToken":"PIN_5530ef86740f2_16afe93cfb"}
            //
            // // Debug if QA Server


            String url = baseUrl + "/" + LOGINPIN_TOKEN_URI + "?mdn=" + URLEncoder.encode(mdn) + "&pin="
                    + URLEncoder.encode(password);
            // if (VZUris.isTabletDevice()) { -- TEMP Workaround for server bug
            String deviceId = Secure.getString(settings.getContentResolver(), Secure.ANDROID_ID);
            if (deviceId == null) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("ANDROID_ID not found. Generating the UUID.ANDROID_ID=" + deviceId);
                }
                deviceId = UUID.randomUUID().toString();
            }
            url += "&deviceId=" + URLEncoder.encode(deviceId);
            // }
            String old = settings.getStringSettings(ApplicationSettings.KEY_OLDLOGINTOKEN);
            if (old != null) {
                url += "&oldLoginToken=" + old;
            }

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("isValidLoginPIN():url=" + url);
            }
            if ("000000".equalsIgnoreCase(password) && Logger.IS_DEBUG_ENABLED && !settings.isSSLEnabled()) {

                String loginToken = password;
                String key = getKey();
                loginToken = AESEncryption.encrypt(key.getBytes(), loginToken);

                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Login token: key=" + key + ",encryptedToken=" + loginToken
                            + ", serverToken=" + password);
                }
                settings.put(ApplicationSettings.KEY_VMA_KEY, key);
                settings.put(ApplicationSettings.KEY_VMA_TOKEN, loginToken);
                settings.put(ApplicationSettings.KEY_VMA_MDN, mdn);
                settings.put(ApplicationSettings.KEY_DEVICE_ID, deviceId);
                // settings.setFirstLaunch(false);
                return VMA_PROVISION_OK;
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            String response = new String(out.toByteArray());
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("isValidLoginPIN():response=" + response);
            }
            JSONObject object = new JSONObject(response);
            String status = object.getString(JSON_STATUS);
            if (JSON_OK.equalsIgnoreCase(status)) {
                String loginToken = object.getString("loginToken");
                String key = getKey();
                loginToken = AESEncryption.encrypt(key.getBytes(), loginToken);

                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Login token: key=" + key + ",encryptedToken=" + loginToken
                            + ", serverToken=" + object.getString("loginToken"));
                }
                settings.put(ApplicationSettings.KEY_VMA_KEY, key);
                settings.put(ApplicationSettings.KEY_VMA_TOKEN, loginToken);
                settings.put(ApplicationSettings.KEY_VMA_MDN, object.getString("mdn"));
                settings.put(ApplicationSettings.KEY_DEVICE_ID, deviceId);
                // settings.setFirstLaunch(false);
                return VMA_PROVISION_OK;
            } else {
                if (object.has("statusinfo")) {
                    status = object.getString("statusinfo");
                }
                return handleKnownServerError(status);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.debug(e.getMessage(), e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.debug(e.getMessage(), e);
            }
        } catch (Exception e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error("Failed to encrypt the " + e.getMessage(), e);
            }
        }
        return VMA_PROVISION_ERROR;
    }

    /**
     * This Method
     * 
     * @return
     */
    private String getKey() {
        String key = UUID.randomUUID().toString();
        key = key.replaceAll("-", "");
        key = key.substring(0, 16);
        return key;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#registerGCMToken(java.lang.String)
     */
    @Override
    public int registerGCMToken(String registrationId) {
        // for retrying purpose incase of failure.
        settings.put(ApplicationSettings.KEY_PUSH_GCM_TOKEN_TOSEND, registrationId);
        if (Logger.IS_DEBUG_ENABLED) {
            String oldId = settings.getStringSettings(ApplicationSettings.KEY_LAST_PUSH_ID);
            Logger.debug("OLD GCM Id:" + oldId);
            Logger.debug("NEW GCM Id:" + registrationId);
            Logger.debug("Both regid are same =" + ((registrationId.equals(oldId)) ? true : false));
        }

        try {

            loginToken = settings.getDecryptedLoginToken();
            mdn = settings.getMDN();

            String url = baseUrl + "/" + PUSHID_URI + "?type=G&mdn=" + URLEncoder.encode(mdn)
                    + "&registrationId=" + URLEncoder.encode(registrationId) + "&loginToken="
                    + URLEncoder.encode(loginToken);
            String old = settings.getStringSettings(ApplicationSettings.KEY_LAST_PUSH_ID);
            if (old != null) {
                url += "&oldRegistrationId=" + old;
            } else {
                url += "&oldRegistrationId=" + registrationId;
            }
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("registerGCMToken():url=" + url);
            }
            if (StringUtils.isNotEmpty(registrationId)) {
                ByteArrayOutputStream out = manager.makePostRequest(url);
                JSONArray array = new JSONArray(new String(out.toByteArray()));
                JSONObject object = array.getJSONObject(0);
                if (object.has(JSON_STATUS)) {
                    String status = object.getString(JSON_STATUS);
                    if (JSON_OK.equalsIgnoreCase(status)) {
                        //
                        settings.removeSettings(ApplicationSettings.KEY_PUSH_GCM_TOKEN_TOSEND);
                        settings.put(ApplicationSettings.KEY_LAST_PUSH_ID, registrationId);
//                        no need to start the vma sync , it may already started 
//                        context.startService(new Intent(SyncManager.ACTION_START_VMA_SYNC));
                        return VMA_PROVISION_OK;
                    } else {
                        Intent gcmFailedIntent = new Intent(SyncManager.ACTION_SYNC_STATUS);
                        gcmFailedIntent.putExtra(SyncManager.EXTRA_STATUS,
                                SyncManager.SYNC_STATUS_GCM_TOKEN_FAILED);
                        context.sendBroadcast(gcmFailedIntent);
                        if (object.has("statusinfo")) {
                            status = object.getString("statusinfo");
                        }
                        return handleKnownServerError(status);
                    }
                }
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("unable to register the C2DM :error=" + e.getMessage(), e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("unable to register the GCM :error=" + e.getMessage(), e);
            }
        }

        return VMA_PROVISION_ERROR;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#doProvision(java.lang.String, java.lang.String)
     */
    @Override
    public int doProvision() {
        int result = VMA_PROVISION_ERROR;
        try {
            sendStatus(SyncStatusCode.VMA_PROVISION_CREATE_MAILBOX);
            this.mdn = settings.getMDN();
            this.loginToken = settings.getDecryptedLoginToken();
            String url = baseUrl + "/" + PROVISIONING_URI + "?mdn=" + mdn + "&loginToken=" + loginToken;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("doProvision():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            // {"status":"ERROR","statusinfo":"Internal Error"}

            JSONObject object = new JSONObject(new String(out.toByteArray()));
            String status = object.getString(JSON_STATUS);
            if (JSON_OK.equalsIgnoreCase(status)) {
                // Query the account settings.
                settings.put(ApplicationSettings.KEY_VMA_PROVISIONED, String.valueOf(1));
                syncMessagingAssistantsSettings();
                // registerGCMToken();
                startSync();
                settings.put(ApplicationSettings.SHOW_WELCOME_SCREEN, true);
                result = VMA_PROVISION_OK;
            } else {
                settings.put(ApplicationSettings.KEY_VMA_PROVISIONED, String.valueOf(0));
                if (object.has("statusinfo")) {
                    status = object.getString("statusinfo");
                }
                result = handleKnownServerError(status);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("doProvision()=" + e.getMessage(), e);
            }
            result = VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private void broadCastProvisionCompleted() {
        Intent intent = new Intent(SyncManager.ACTION_SYNC_STATUS);
        intent.putExtra(SyncManager.EXTRA_STATUS, SyncManager.SYNC_PROVISIONING_COMPLETED);
        context.sendBroadcast(intent);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#getMessagingAssistantsSettings()
     */
    @Override
    public int syncMessagingAssistantsSettings() throws IOException {
        try {
            mdn = settings.getMDN();
            // loginToken = settings.getSettings(AppSettings.KEY_VMA_TOKEN);
            loginToken = settings.getDecryptedLoginToken();
            String url = baseUrl + "/" + ASSISTANTQUERY_URI + "?mdn=" + mdn + "&loginToken=" + loginToken;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("getMessagingAssistantsSettings():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            JSONArray array = new JSONArray(new String(out.toByteArray()));
            // [{"status":"OK","statusInfo":"Success","autoForwardStatus":"PENDING","autoForwardAddr":"vzmjega@gmail.com",
            // "autoForwardUsedAddr":["vzmjega@gmail.com"],"autoForwardEndDate":"11/18/2012",
            // "autoReplyStatus":"ACTIVE","autoReplyMsg":"Testing Messaging Assistant Query API \n\nsent from VMA account  J",
            // "autoReplyEndDate":"11/18/2012","autoReplyUsedMsgs":["Testing Messaging Assistant Query API \n\nsent from VMA account  J"]}]''

            // int length = array.length();
            JSONObject response = array.getJSONObject(0);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("getMessagingAssistantsSettings():url response=" + response.toString());
            }
            String status = response.getString(JSON_STATUS);
            if (JSON_OK.equals(status)) {
                // Remove this settings before adding new values
                // settings.remove(KEY_VMA_AUTOFORWARDSTATUS);
                // settings.remove(KEY_VMA_AUTOFORWARDADDR);
                // settings.remove(KEY_VMA_AUTOFORWARDENDDATE);
                settings.removeSettings(KEY_VMA_AUTOREPLYSTATUS);
                settings.removeSettings(KEY_VMA_AUTOREPLYMSG);
                settings.removeSettings(KEY_VMA_AUTOREPLYENDDATE);

                /*
                 * if (response.has(KEY_VMA_AUTOFORWARDSTATUS)) { int forwardStatus =
                 * AutoForwardStatus.valueOf( response.getString(KEY_VMA_AUTOFORWARDSTATUS)).ordinal();
                 * settings.put(KEY_VMA_AUTOFORWARDSTATUS, forwardStatus); if (forwardStatus ==
                 * AutoForwardStatus.ACTIVE.ordinal() || forwardStatus == AutoForwardStatus.PENDING.ordinal())
                 * { settings.put(AppSettings.KEY_VMA_AUTOFWD, true); } else {
                 * settings.put(AppSettings.KEY_VMA_AUTOFWD, false); } } else {
                 * settings.put(AppSettings.KEY_VMA_AUTOFWD, false); } if
                 * (response.has(KEY_VMA_AUTOFORWARDADDR)) { settings.put(KEY_VMA_AUTOFORWARDADDR,
                 * response.getString(KEY_VMA_AUTOFORWARDADDR)); } if
                 * (response.has(KEY_VMA_AUTOFORWARDENDDATE)) { settings.put(KEY_VMA_AUTOFORWARDENDDATE,
                 * response.getString(KEY_VMA_AUTOFORWARDENDDATE)); }
                 */
                if (response.has(KEY_VMA_AUTOREPLYSTATUS)) {
                    int replyStatus = AutoReplyStatus.valueOf(response.getString(KEY_VMA_AUTOREPLYSTATUS))
                            .ordinal();
                    settings.put(KEY_VMA_AUTOREPLYSTATUS, replyStatus);
                    if (replyStatus == AutoReplyStatus.ACTIVE.ordinal()) {
                        settings.put(AppSettings.KEY_VMA_AUTO_REPLY, true);
                    } else {
                        settings.put(AppSettings.KEY_VMA_AUTO_REPLY, false);
                    }
                } else {
                    settings.put(AppSettings.KEY_VMA_AUTO_REPLY, false);
                }
                if (response.has(KEY_VMA_AUTOREPLYMSG)) {
                    settings.put(KEY_VMA_AUTOREPLYMSG, response.getString(KEY_VMA_AUTOREPLYMSG));
                }
                if (response.has(KEY_VMA_AUTOREPLYENDDATE)) {
                    settings.put(KEY_VMA_AUTOREPLYENDDATE, response.getString(KEY_VMA_AUTOREPLYENDDATE));
                }

                if (response.has(KEY_VMA_AUTOREPLYUSEDMSGS)) {
                    JSONArray autoReplyarray = response.getJSONArray(KEY_VMA_AUTOREPLYUSEDMSGS);
                    int length = autoReplyarray.length();
                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    Builder b = null;
                    for (int i = 0; i < length; i++) {
                        b = ContentProviderOperation.newInsert(RecentlyUsedReplyMsg.CONTENT_URI);
                        b.withValue(KEY_VMA_AUTOREPLYUSEDMSGS, autoReplyarray.getString(i));
                        operations.add(b.build());
                    }
                    if (!operations.isEmpty()) {
                        // Delete all previous data
                        context.getContentResolver().delete(RecentlyUsedReplyMsg.CONTENT_URI, null, null);
                        // clear all the devices
                        context.getContentResolver().applyBatch(ApplicationProvider.AUTHORITY, operations);
                    }
                }
                /*
                 * if (response.has(KEY_VMA_AUTOFORWARDUSEDADDR)) { JSONArray autoFwdarray =
                 * response.getJSONArray(KEY_VMA_AUTOFORWARDUSEDADDR); int length = autoFwdarray.length();
                 * ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                 * Builder b = null; for (int i = 0; i < length; i++) { b =
                 * ContentProviderOperation.newInsert(RecentlyUsedFwdAddr.CONTENT_URI);
                 * b.withValue(KEY_VMA_AUTOFORWARDUSEDADDR, autoFwdarray.getString(i));
                 * operations.add(b.build()); } if (!operations.isEmpty()) { // Delete all previous data
                 * context.getContentResolver().delete(RecentlyUsedFwdAddr.CONTENT_URI, null, null); // clear
                 * all the devices context.getContentResolver().applyBatch(ApplicationProvider.AUTHORITY,
                 * operations); } }
                 */
                return VMA_PROVISION_OK;
            } else {
                if (response.has("statusinfo")) {
                    status = response.getString("statusinfo");
                }
                return handleKnownServerError(status);
            }

        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("syncMessagingAssistantsSettings()=" + e.getMessage(), e);
            }
            throw e;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("syncMessagingAssistantsSettings()=" + e.getMessage(), e);
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return VMA_PROVISION_ERROR;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#enableAutoforward(java.lang.String,
     * java.lang.String)
     */
    @Override
    public int enableAutoforward(String address, String endDate) {
        try {
            String mdn = settings.getMDN();
            // String loginToken = settings.getSettings(ApplicationSettings.KEY_VMA_TOKEN);
            String loginToken = settings.getDecryptedLoginToken();

            String url = baseUrl + "/" + ASSISTANTAUTOFWD_URI + "?mdn=" + mdn + "&loginToken=" + loginToken
                    + "&autoForwardAddr=" + URLEncoder.encode(address) + "&autoForwardEndDate="
                    + URLEncoder.encode(endDate);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("enableAutoforward():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            // {"status":"ERROR","statusinfo":"Internal Error"}

            JSONArray array = new JSONArray(new String(out.toByteArray()));
            JSONObject object = array.getJSONObject(0);
            String status = object.getString(JSON_STATUS);
            if (JSON_OK.equalsIgnoreCase(status)) {
                settings.put(KEY_VMA_AUTOFORWARDADDR, address);
                settings.put(KEY_VMA_AUTOFORWARDENDDATE, endDate);
                settings.put(AppSettings.KEY_VMA_AUTOFWD, true);
                return VMA_PROVISION_OK;
            } else if (JSON_FAIL.equalsIgnoreCase(status)) {
                String message = "Unknown";
                if (object.has("statusInfo")) {
                    message = object.getString("statusInfo");
                }
                throw new IllegalArgumentException(message);
            } else {
                if (object.has("statusinfo")) {
                    status = object.getString("statusinfo");
                }
                return handleKnownServerError(status);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("enableAutoForward()=" + e.getMessage(), e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("enableAutoForward()=" + e.getMessage(), e);
            }
        }
        return VMA_PROVISION_ERROR;

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#enableAutoReply(java.lang.String,
     * java.lang.String)
     */
    @Override
    public int enableAutoReply(String message, String endDate) {
        try {
            String mdn = settings.getMDN();
            // String loginToken = settings.getSettings(ApplicationSettings.KEY_VMA_TOKEN);
            String loginToken = settings.getDecryptedLoginToken();

            String url = baseUrl + "/" + ASSISTANTAUTOREPLY_URI + "?mdn=" + mdn + "&loginToken=" + loginToken
                    + "&autoReplyMsg=" + URLEncoder.encode(message) + "&autoReplyEndDate="
                    + URLEncoder.encode(endDate);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("enableAutoReply():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            // {"status":"ERROR","statusinfo":"Internal Error"}

            JSONArray array = new JSONArray(new String(out.toByteArray()));
            JSONObject object = array.getJSONObject(0);
            String status = object.getString(JSON_STATUS);
            if (JSON_OK.equalsIgnoreCase(status)) {
                settings.put(KEY_VMA_AUTOREPLYMSG, message);
                settings.put(KEY_VMA_AUTOREPLYENDDATE, endDate);
                settings.put(AppSettings.KEY_VMA_AUTO_REPLY, true);
                return VMA_PROVISION_OK;
            } else if (JSON_FAIL.equalsIgnoreCase(status)) {
                String error = "Unknown";
                if (object.has("statusInfo")) {
                    error = object.getString("statusInfo");
                }
                throw new IllegalArgumentException(error);
            } else {
                if (object.has("statusinfo")) {
                    status = object.getString("statusinfo");
                }
                return handleKnownServerError(status);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("enableAutoReply()=" + e.getMessage(), e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("enableAutoReply()=" + e.getMessage(), e);
            }
        }
        return VMA_PROVISION_ERROR;

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#disableAutoForward()
     */

    @Override
    public int disableAutoForward() {
        try {
            String mdn = settings.getMDN();
            // String loginToken = settings.getSettings(ApplicationSettings.KEY_VMA_TOKEN);
            String loginToken = settings.getDecryptedLoginToken();

            String url = baseUrl + "/" + ASSISTANTAUTOFWDDISABLE_URI + "?mdn=" + mdn + "&loginToken="
                    + loginToken;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("disableAutoForward():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            // {"status":"ERROR","statusinfo":"Internal Error"}
            JSONArray array = new JSONArray(new String(out.toByteArray()));
            JSONObject object = array.getJSONObject(0);
            String status = object.getString(JSON_STATUS);
            if (JSON_OK.equalsIgnoreCase(status)) {
                settings.removeSettings(KEY_VMA_AUTOFORWARDADDR);
                settings.removeSettings(KEY_VMA_AUTOFORWARDENDDATE);
                settings.removeSettings(AppSettings.KEY_VMA_AUTOFWD);
                return VMA_PROVISION_OK;
            } else {
                if (object.has("statusinfo")) {
                    status = object.getString("statusinfo");
                }
                return handleKnownServerError(status);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("disableAutoForward()=" + e.getMessage(), e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("disableAutoForward()=" + e.getMessage(), e);
            }
        }
        return VMA_PROVISION_ERROR;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#disableAutoReply()
     */
    @Override
    public int disableAutoReply() {
        try {
            String mdn = settings.getMDN();
            // String loginToken = settings.getSettings(ApplicationSettings.KEY_VMA_TOKEN);
            String loginToken = settings.getDecryptedLoginToken();

            String url = baseUrl + "/" + ASSISTANTAUTOREPLYDISABLE_URI + "?mdn=" + mdn + "&loginToken="
                    + loginToken;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("disableAutoReply():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            // [{"status":"ERROR","statusinfo":"Internal Error"}]
            // [{"status":"OK","statusInfo":"Success"}]

            JSONArray array = new JSONArray(new String(out.toByteArray()));
            JSONObject object = array.getJSONObject(0);
            String status = object.getString(JSON_STATUS);
            if (JSON_OK.equalsIgnoreCase(status)) {
                settings.removeSettings(KEY_VMA_AUTOREPLYMSG);
                settings.removeSettings(KEY_VMA_AUTOREPLYENDDATE);
                settings.removeSettings(AppSettings.KEY_VMA_AUTO_REPLY);
                return VMA_PROVISION_OK;
            } else {
                if (object.has("statusinfo")) {
                    status = object.getString("statusinfo");
                }
                return handleKnownServerError(status);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("disableAutoReply()=" + e.getMessage(), e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("disableAutoReply()=" + e.getMessage(), e);
            }
        }
        return VMA_PROVISION_ERROR;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vma.provision.VMAProvision#deleteVMAAccount()
     */
    @Override
    public int deleteVMAAccount() {
        try {
            context.stopService(new Intent(SyncManager.ACTION_STOP_VMA_SYNC));
            String mdn = settings.getMDN();
            // String loginToken = settings.getSettings(ApplicationSettings.KEY_VMA_TOKEN);
            String loginToken = settings.getDecryptedLoginToken();
            String url = baseUrl + "/" + DEPROVISIONING_URI + "?mdn=" + mdn + "&loginToken=" + loginToken;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("deProvisioning():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            // {"status":"ERROR","statusinfo":"Internal Error"}

            JSONObject object = new JSONObject(new String(out.toByteArray()));
            String status = object.getString(JSON_STATUS);
            if (JSON_OK.equalsIgnoreCase(status)) {
                settings.resetHandset();
                return VMA_PROVISION_OK;
            } else {
                if (object.has("statusinfo")) {
                    status = object.getString("statusinfo");
                }
                return handleKnownServerError(status);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("deleteVMAAccount()=" + e.getMessage(), e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("deleteVMAAccount()=" + e.getMessage(), e);
            }
        }
        return VMA_PROVISION_ERROR;

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.VMAProvision#getLinkedDevices()
     */
    @Override
    public ArrayList<LinkedVMADevices> syncLinkedDevices() {
        ArrayList<LinkedVMADevices> devicesList = new ArrayList<LinkedVMADevices>();
        try {
            String mdn = settings.getMDN();
            // String loginToken = settings.getSettings(ApplicationSettings.KEY_VMA_TOKEN);
            String loginToken = settings.getDecryptedLoginToken();

            String url = baseUrl + "/" + LINKEDDEVICES_URI + "?mdn=" + mdn + "&loginToken=" + loginToken;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("getLinkedDevices():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);

            // [{"status":"OK","statusInfo":"Success","devices":[{"deviceId":"815d2699035a1","deviceName":"Motorola-MZ609","createTime":"Fri, 9 Nov 2012 13:03:53 +0000 (GMT)"},{"deviceId":"f706e05d0cdd4","deviceName":"Motorola-MZ609","createTime":"Sat, 10 Nov 2012 07:08:32 +0000 (GMT)"}]}]
            JSONArray jsonArray = new JSONArray(new String(out.toByteArray()));
            JSONObject object = jsonArray.getJSONObject(0);
            String status = object.getString(JSON_STATUS);
            String statusInfo = null;
            if (object.has(JSON_STATUSINFO)) {
                statusInfo = object.getString(JSON_STATUSINFO);
            }
            if (JSON_OK.equalsIgnoreCase(status)) {
                if (object.has("devices")) {
                    JSONArray array = object.getJSONArray("devices");
                    int length = array.length();

                    Builder b = null;
                    LinkedVMADevices device = null;
                    for (int i = 0; i < length; i++) {
                        device = new LinkedVMADevices(array.getJSONObject(i));
                        try {
                            Date date = SDF_LINKED_DEVICE.parse(device.createTime);
                            device.createTime = String.valueOf(date.getTime());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        devicesList.add(device);
                    }
                }
                return devicesList;

            } else if (JSON_FAIL.equalsIgnoreCase(status)) {
                handleKnownServerError(status, statusInfo);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("getLinkedDevices()=" + e.getMessage(), e);
            }
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("getLinkedDevices()=" + e.getMessage(), e);
            }
        }
        return devicesList;

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.VMAProvision#deleteLinkedDevice(java.lang.String)
     */
    @Override
    public int deleteLinkedDevice(String deviceId) {
        try {
            String mdn = settings.getMDN();
            // String loginToken = settings.getSettings(ApplicationSettings.KEY_VMA_TOKEN);
            String loginToken = settings.getDecryptedLoginToken();

            String url = baseUrl + "/" + REMOVEDEVICE_URI + "?mdn=" + mdn + "&loginToken=" + loginToken
                    + "&&deviceId=" + deviceId;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("deleteLinkedDevice():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            // {"status":"ERROR","statusinfo":"Internal Error"}

            JSONArray array = new JSONArray(new String(out.toByteArray()));
            JSONObject object = array.getJSONObject(0);
            String status = object.getString(JSON_STATUS);
            if (JSON_OK.equalsIgnoreCase(status)) {
                context.getContentResolver().delete(LinkedVMADevices.CONTENT_URI,
                        LinkedVMADevices._DEVICE_ID + " = ?", new String[] { deviceId });
                return VMA_PROVISION_OK;
            } else if (JSON_ERROR.equalsIgnoreCase(status)) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("ignoring the VMA_REMOVE_DEVICE_ERROR and deleting deviceId:"+deviceId);
                }
                context.getContentResolver().delete(LinkedVMADevices.CONTENT_URI,
                        LinkedVMADevices._DEVICE_ID + " = ?", new String[] { deviceId });
                return VMA_PROVISION_OK;
               // return VMA_REMOVE_DEVICE_ERROR;
            } else {
                if (object.has("statusinfo")) {
                    status = object.getString("statusinfo");
                }	
                return handleKnownServerError(status);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("deleteLinkedDevice()=" + e.getMessage(), e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return VMA_REMOVE_DEVICE_ERROR;
    }

    private int handleKnownServerError(String status, String statusInfo) {
        if (JSON_LOGIN_FAIL.equalsIgnoreCase(status)) {
            if ("Invalid MDN or token".equalsIgnoreCase(statusInfo)) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Login  failure. stoping the sync service.");
                }
                context.stopService(new Intent(SyncManager.ACTION_VMA_SYNC));
            }
            return VMA_PROVISION_ERROR;
        } else {
            return handleKnownServerError(status);
        }
    }

    private int handleKnownServerError(String status) {
        if (JSON_LOGIN_FAIL.equalsIgnoreCase(status)) {
            return VMA_PROVISION_ERROR;
        } else if (JSON_INTERNAL_ERROR.equalsIgnoreCase(status)) {
            return VMA_PROVISION_INTERNAL_SERVER_ERROR;
        } else if (JSON_FAIL.equalsIgnoreCase(status)) {
            return VMA_PROVISION_FAIL;
        } else if (JSON_VBLOCK.equalsIgnoreCase(status)) {
            return VMA_PROVISION_VBLOCK;
        } else if (JSON_SUSPENDED.equalsIgnoreCase(status)) {
            return VMA_PROVISION_SUSPENDED;
        } else if (JSON_INVALID_MDN.equalsIgnoreCase(status)) {
            return VMA_PROVISION_INVALID_MDN;
        } else if (JSON_INVALID_END_DATE.equalsIgnoreCase(status)) {
            return VMA_PROVISION_INVALID_END_DATE;
        } else if (JSON_ALREADY_AUTO_FORWARDED.equalsIgnoreCase(status)) {
            return VMA_PROVISION_ALREADY_AUTO_FORWARDED;
        } else if (JSON_DUPLICATE_DESTINATION.equalsIgnoreCase(status)) {
            return VMA_PROVISION_DUPLICATE_DESTINATION;
        } else if (JSON_NOTELIGIBLE.equalsIgnoreCase(status)) {
            settings.put(AppSettings.KEY_VMA_NOTELIGIBLE, true);
            return VMA_PROVISION_NOTELIGIBLE;
        } else if (JSON_NOTVZWMDN.equalsIgnoreCase(status)) {
            settings.put(AppSettings.KEY_VMA_NOTVZWMDN, true);
            return VMA_PROVISION_NOTVZWMDN;
        } else if (JSON_EXCEEDDEVICELIMIT.equalsIgnoreCase(status)) {
            return VMA_PROVISION_EXCEEDDEVICELIMIT;
        } else if (JSON_NOTEXT.equalsIgnoreCase(status)) {
            return VMA_PROVISION_NOTEXT;
        } else if (JSON_NO_MMS.equalsIgnoreCase(status)) {
            return VMA_PROVISION_NOMMS;
        } else if (JSON_OVERLIMIT.equalsIgnoreCase(status)) {
            return VMA_PROVISION_OVERLIMIT;
        } else if (JSON_ERROR.equalsIgnoreCase(status)) {
            return VMA_PROVISION_ERROR;
        } else {
            return VMA_PROVISION_UNKNOWN;
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.VMAProvision#deleteAllLinkedDevices()
     */
    @Override
    public int deleteAllLinkedDevices() {
        try {
            String mdn = settings.getMDN();
            // String loginToken = settings.getSettings(ApplicationSettings.KEY_VMA_TOKEN);
            String loginToken = settings.getDecryptedLoginToken();

            String url = baseUrl + "/" + REMOVESECONDARYDEVICES_URI + "?mdn=" + mdn + "&loginToken="
                    + loginToken;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("deleteAllLinkedDevices():url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            // {"status":"ERROR","statusinfo":"Internal Error"}

            JSONArray array = new JSONArray(new String(out.toByteArray()));
            JSONObject object = array.getJSONObject(0);
            String status = object.getString(JSON_STATUS);
            if (status.equalsIgnoreCase(JSON_OK)) {
                context.getContentResolver().delete(LinkedVMADevices.CONTENT_URI, null, null);
                return VMA_PROVISION_OK;
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("deleteAllLinkedDevices()=" + e.getMessage(), e);
            }
            return VMA_PROVISION_NETWORK_ERROR;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("deleteAllLinkedDevices()=" + e.getMessage(), e);
            }
        }
        return VMA_PROVISION_ERROR;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.VMAProvision#registerGCMToken()
     */
    @Override
    public void registerGCMToken1() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("registerGCMToken");
        }
        try {

            GCMRegistrar.checkDevice(context);
            GCMRegistrar.checkManifest(context);
            final String regId = GCMRegistrar.getRegistrationId(context);
            if (regId.equals("")) {
                GCMRegistrar.register(context, VMAProvision.SENDER_ID);
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("GCM Token Already registered");
                }
            }
        } catch (Exception e) {
            // We get unsupportedoperationexception from checkdevice
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Not registering gcm." + e);
            }
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.VMAProvision#doHandsetProvisioning()
     */
    @Override
    public int doHandsetProvisioning(String mdn, String deviceModel) {
        boolean doRetry = false;
        int response = VMA_PROVISION_ERROR;
        int count = 0;
        do {

            PINIntercepters intercepters = new PINIntercepters();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("doHandsetProvisioning:mdn=" + mdn);
            }
            // Request PIN
            lock = new Object();

            IntentFilter filter = new IntentFilter(Intents.SMS_RECEIVED_ACTION);
            // filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            // GOSMS app is using Integer.MAX_VALUE as the priority, we hve to set more
            filter.setPriority(Integer.MAX_VALUE);
            context.registerReceiver(intercepters, filter);
            // 1. Generate PIN
            if ((response = generatePIN(mdn, deviceModel, true)) == VMA_PROVISION_OK) {
                isPinReceived = false;
                // wait for 2 minutes
                synchronized (lock) {
                    try {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Waiting for Silent SMS PIN");
                        }
                        // two minutes time out
                        lock.wait(120000);
                        // To abort the broadcast.
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }

                }
                // 2. Validating PIN
                if (isPinReceived && (response = isValidLoginPIN(mdn, pin)) == VMA_PROVISION_OK) {
                    // 3.) VMA Subscription
                    response = isVMASubscriber(mdn);
                    if (response == VMA_PROVISION_VMA_SUBSCRIBER) {
                        if (!ApplicationSettings.getInstance().getBooleanSetting(
                                AppSettings.KEY_SMS_CHECKSUM_COMPLETED)) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("Handset was already a VMA sub, starting thread to do indexing old messages");
                            }
                            SmsChecksumBuilder builder = new SmsChecksumBuilder(settings, getMaxSmsId());
                            // We are already in the worker thread , we don't have to start a new thread again
                            builder.run();
                        }
                        response = VMA_PROVISION_OK;
                    } else if (response == VMA_PROVISION_NOT_VMA_SUBCRIBER) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Handset was not a VMA sub, no need to index old messages since they are not going to be on VMA");
                        }
                        settings.put(AppSettings.KEY_SMS_CHECKSUM_COMPLETED, true);
                        response = VMA_PROVISION_OK;
                    }
                } else {
                    response = VMA_PROVISION_PIN_RETRIEVAL_FAILED;
                }

            }
            context.unregisterReceiver(intercepters);
            if (response != VMA_PROVISION_OK) {
                count++;
                if (count == 2) {
                    doRetry = true;
                }
            } else {
                doRetry = true;
            }
        } while (!doRetry);
        broadCastProvisionCompleted();
        return response;
    }

    /**
     * This Method
     */
    @Deprecated
    public void startSMSChecksumBuilder() {
        SmsChecksumBuilder builder = new SmsChecksumBuilder(settings, getMaxSmsId());
        builder.start();
    }

    public int getMaxSmsId() {
        int maxLuid = 0;
        String[] projection = new String[] { "MAX(" + Sms._ID + ")" };
        Cursor c = settings.getContentResolver().query(VZUris.getSmsUri(), projection, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                maxLuid = c.getInt(0);
            }
            c.close();
        }
        return maxLuid;
    }

    class PINIntercepters extends BroadcastReceiver {

        /**
         * 
         */
        private static final String SILENT_PIN_SENDER_ADDRESS = "900080004102";

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            // SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(PINIntercepters.class, "PINIntercepters.onReceive(Context, Intent)");
            }
            if (Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(PINIntercepters.class, "body :" + msgs[0].getDisplayMessageBody());
                    Logger.debug(PINIntercepters.class, "sender :" + msgs[0].getDisplayOriginatingAddress());
                }
                String sender = msgs[0].getDisplayOriginatingAddress();
                String message = msgs[0].getDisplayMessageBody();
                if (SILENT_PIN_SENDER_ADDRESS.equalsIgnoreCase(sender)) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(PINIntercepters.class, "Service Messaage ");
                    }
                    if (message.startsWith("VZ Messages PIN:") || message.startsWith(":")
                            || message.lastIndexOf(":") > 0) {
                        String[] pins = message.split(":", 2);
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(PINIntercepters.class, "found silent pin=" + pins[1].trim());
                        }
                        pin = pins[1].trim();
                        isPinReceived = true;
                        if (isOrderedBroadcast()) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(PINIntercepters.class, "aborting Order Broadcast.");
                            }
                            abortBroadcast();
                        } else {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(PINIntercepters.class, "aborting non-Order Broadcast.");
                            }
                            setResult(Activity.RESULT_CANCELED, null, null);
                            clearAbortBroadcast();
                        }
                        // Need notify after unblock.
                        synchronized (lock) {
                            lock.notify();
                        }

                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(PINIntercepters.class, "Not a service messaage. no need to process");
                        }
                    }
                }
            }

        }

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.VMAProvision#syncMessagingAssistantsSettings(boolean)
     */
    @Override
    public int syncMessagingAssistantsSettings(boolean isReply) throws IOException {
        try {
            mdn = settings.getMDN();
            // loginToken = settings.getSettings(AppSettings.KEY_VMA_TOKEN);
            loginToken = settings.getDecryptedLoginToken();
            String url = baseUrl + "/" + ASSISTANTQUERY_URI + "?mdn=" + mdn + "&loginToken=" + loginToken;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("syncMessagingAssistantsSettings(boolean isReply):url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            JSONArray array = new JSONArray(new String(out.toByteArray()));
            // [{"status":"OK","statusInfo":"Success","autoForwardStatus":"PENDING","autoForwardAddr":"vzmjega@gmail.com",
            // "autoForwardUsedAddr":["vzmjega@gmail.com"],"autoForwardEndDate":"11/18/2012",
            // "autoReplyStatus":"ACTIVE","autoReplyMsg":"Testing Messaging Assistant Query API \n\nsent from VMA account  J",
            // "autoReplyEndDate":"11/18/2012","autoReplyUsedMsgs":["Testing Messaging Assistant Query API \n\nsent from VMA account  J"]}]''

            // int length = array.length();
            JSONObject response = array.getJSONObject(0);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("syncMessagingAssistantsSettings(boolean isReply):url response="
                        + response.toString());
            }
            String status = response.getString(JSON_STATUS);
            if (JSON_OK.equals(status)) {
                // Remove this settings before adding new values
                if (!isReply) {/*
                                * settings.remove(KEY_VMA_AUTOFORWARDSTATUS);
                                * settings.remove(KEY_VMA_AUTOFORWARDADDR);
                                * settings.remove(KEY_VMA_AUTOFORWARDENDDATE);
                                * 
                                * if (response.has(KEY_VMA_AUTOFORWARDSTATUS)) { int forwardStatus =
                                * AutoForwardStatus.valueOf(
                                * response.getString(KEY_VMA_AUTOFORWARDSTATUS)).ordinal();
                                * settings.put(KEY_VMA_AUTOFORWARDSTATUS, forwardStatus); if (forwardStatus ==
                                * AutoForwardStatus.ACTIVE.ordinal() || forwardStatus ==
                                * AutoForwardStatus.PENDING.ordinal()) {
                                * settings.put(AppSettings.KEY_VMA_AUTOFWD, true); } else {
                                * settings.put(AppSettings.KEY_VMA_AUTOFWD, false); } } else {
                                * settings.put(AppSettings.KEY_VMA_AUTOFWD, false); } if
                                * (response.has(KEY_VMA_AUTOFORWARDADDR)) {
                                * settings.put(KEY_VMA_AUTOFORWARDADDR,
                                * response.getString(KEY_VMA_AUTOFORWARDADDR)); } if
                                * (response.has(KEY_VMA_AUTOFORWARDENDDATE)) {
                                * settings.put(KEY_VMA_AUTOFORWARDENDDATE,
                                * response.getString(KEY_VMA_AUTOFORWARDENDDATE)); } if
                                * (response.has(KEY_VMA_AUTOFORWARDUSEDADDR)) { JSONArray autoFwdarray =
                                * response.getJSONArray(KEY_VMA_AUTOFORWARDUSEDADDR); int length =
                                * autoFwdarray.length(); ArrayList<ContentProviderOperation> operations = new
                                * ArrayList<ContentProviderOperation>(); Builder b = null; for (int i = 0; i <
                                * length; i++) { b =
                                * ContentProviderOperation.newInsert(RecentlyUsedFwdAddr.CONTENT_URI);
                                * b.withValue(KEY_VMA_AUTOFORWARDUSEDADDR, autoFwdarray.getString(i));
                                * operations.add(b.build()); } if (!operations.isEmpty()) { // Delete all
                                * previous data
                                * context.getContentResolver().delete(RecentlyUsedFwdAddr.CONTENT_URI, null,
                                * null); // clear all the devices context.getContentResolver()
                                * .applyBatch(ApplicationProvider.AUTHORITY, operations); } }
                                */
                } else {
                    settings.removeSettings(KEY_VMA_AUTOREPLYSTATUS);
                    settings.removeSettings(KEY_VMA_AUTOREPLYMSG);
                    settings.removeSettings(KEY_VMA_AUTOREPLYENDDATE);

                    if (response.has(KEY_VMA_AUTOREPLYSTATUS)) {
                        int replyStatus = AutoReplyStatus
                                .valueOf(response.getString(KEY_VMA_AUTOREPLYSTATUS)).ordinal();
                        settings.put(KEY_VMA_AUTOREPLYSTATUS, replyStatus);
                        if (replyStatus == AutoReplyStatus.ACTIVE.ordinal()) {
                            settings.put(AppSettings.KEY_VMA_AUTO_REPLY, true);
                        } else {
                            settings.put(AppSettings.KEY_VMA_AUTO_REPLY, false);
                        }
                    } else {
                        settings.put(AppSettings.KEY_VMA_AUTO_REPLY, false);
                    }
                    if (response.has(KEY_VMA_AUTOREPLYMSG)) {
                        settings.put(KEY_VMA_AUTOREPLYMSG, response.getString(KEY_VMA_AUTOREPLYMSG));
                    }
                    if (response.has(KEY_VMA_AUTOREPLYENDDATE)) {
                        settings.put(KEY_VMA_AUTOREPLYENDDATE, response.getString(KEY_VMA_AUTOREPLYENDDATE));
                    }

                    if (response.has(KEY_VMA_AUTOREPLYUSEDMSGS)) {
                        JSONArray autoReplyarray = response.getJSONArray(KEY_VMA_AUTOREPLYUSEDMSGS);
                        int length = autoReplyarray.length();
                        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                        Builder b = null;
                        for (int i = 0; i < length; i++) {
                            b = ContentProviderOperation.newInsert(RecentlyUsedReplyMsg.CONTENT_URI);
                            b.withValue(KEY_VMA_AUTOREPLYUSEDMSGS, autoReplyarray.getString(i));
                            operations.add(b.build());
                        }
                        if (!operations.isEmpty()) {
                            // Delete all previous data
                            context.getContentResolver().delete(RecentlyUsedReplyMsg.CONTENT_URI, null, null);
                            // clear all the devices
                            context.getContentResolver()
                                    .applyBatch(ApplicationProvider.AUTHORITY, operations);
                        }
                    }
                }

                return VMA_PROVISION_OK;
            } else {
                if (response.has("statusinfo")) {
                    status = response.getString("statusinfo");
                }
                return handleKnownServerError(status);
            }

        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("syncMessagingAssistantsSettings(boolean isReply)=" + e.getMessage(), e);
            }
            throw e;
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("syncMessagingAssistantsSettings(boolean isReply)=" + e.getMessage(), e);
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return VMA_PROVISION_ERROR;
    }

    private void startSync() {
        try {
            if(Logger.IS_DEBUG_ENABLED){
                Logger.debug(getClass() , "startSync()");
            }
            context.startService(new Intent(SyncManager.ACTION_START_VMA_SYNC));
            syncMessagingAssistantsSettings();
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("startSync", e);
            }
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.VMAProvision#queryMessagingAssistantfeatures(int, int)
     */
    @Override
    public int queryMessagingAssistantfeatures(long autoForwardSyncAnchor, long autoReplySyncAnchor) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("queryMessagingAssistantfeatures(): autoForwardSyncAnchor=" + autoForwardSyncAnchor
                    + ",autoReplySyncAnchor=" + autoReplySyncAnchor);
        }
        // Sample response
        // [{"status":"OK","statusInfo":"Success","autoForwardStatus":"PENDING","autoForwardAddr":"vzmjega@gmail.com",
        // "autoForwardUsedAddr":["vzmjega@gmail.com"],"autoForwardEndDate":"11/18/2012",
        // "autoReplyStatus":"ACTIVE","autoReplyMsg":"Testing Messaging Assistant Query API \n\nsent from VMA account  J",
        // "autoReplyEndDate":"11/18/2012","autoReplyUsedMsgs":["Testing Messaging Assistant Query API \n\nsent from VMA account  J"]}]''

        try {
            mdn = settings.getMDN();
            loginToken = settings.getDecryptedLoginToken();
            String url = baseUrl + "/" + ASSISTANTQUERY_URI + "?mdn=" + mdn + "&loginToken=" + loginToken;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("MessagingAssistant:url=" + url);
            }
            ByteArrayOutputStream out = manager.makePostRequest(url);
            JSONArray array = new JSONArray(new String(out.toByteArray()));

            // int length = array.length();
            JSONObject response = array.getJSONObject(0);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("MessagingAssistant: response=" + response.toString());
            }
            String status = response.getString(JSON_STATUS);
            if (JSON_OK.equals(status)) {
                // Remove this settings before adding new values
                settings.removeSettings(KEY_VMA_AUTOREPLYSTATUS);
                settings.removeSettings(KEY_VMA_AUTOREPLYMSG);
                settings.removeSettings(KEY_VMA_AUTOREPLYENDDATE);
                if (response.has(KEY_VMA_AUTOREPLYSTATUS)) {
                    int replyStatus = AutoReplyStatus.valueOf(response.getString(KEY_VMA_AUTOREPLYSTATUS))
                            .ordinal();
                    settings.put(KEY_VMA_AUTOREPLYSTATUS, replyStatus);
                    if (replyStatus == AutoReplyStatus.ACTIVE.ordinal()) {
                        settings.put(AppSettings.KEY_VMA_AUTO_REPLY, true);
                    } else {
                        settings.put(AppSettings.KEY_VMA_AUTO_REPLY, false);
                    }
                } else {
                    settings.put(AppSettings.KEY_VMA_AUTO_REPLY, false);
                }
                if (response.has(KEY_VMA_AUTOREPLYMSG)) {
                    settings.put(KEY_VMA_AUTOREPLYMSG, response.getString(KEY_VMA_AUTOREPLYMSG));
                }
                if (response.has(KEY_VMA_AUTOREPLYENDDATE)) {
                    settings.put(KEY_VMA_AUTOREPLYENDDATE, response.getString(KEY_VMA_AUTOREPLYENDDATE));
                }

                if (response.has(KEY_VMA_AUTOREPLYUSEDMSGS)) {
                    JSONArray autoReplyarray = response.getJSONArray(KEY_VMA_AUTOREPLYUSEDMSGS);
                    int length = autoReplyarray.length();
                    ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                    Builder b = null;
                    for (int i = 0; i < length; i++) {
                        b = ContentProviderOperation.newInsert(RecentlyUsedReplyMsg.CONTENT_URI);
                        b.withValue(KEY_VMA_AUTOREPLYUSEDMSGS, autoReplyarray.getString(i));
                        operations.add(b.build());
                    }
                    if (!operations.isEmpty()) {
                        // Delete all previous data
                        SqliteWrapper.delete(context, RecentlyUsedReplyMsg.CONTENT_URI, null, null);
                        // clear all the devices
                        SqliteWrapper.applyBatch(context, ApplicationProvider.AUTHORITY, operations);
                    }
                }
                // updating the sync anchor
                settings.put(AppSettings.KEY_AUTOFORWARD_SYNC_ANCHOR, autoForwardSyncAnchor);
                settings.put(AppSettings.KEY_AUTOREPLY_SYNC_ANCHOR, autoReplySyncAnchor);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("MessagingAssistant:sending UI notification to update the UI");
                }
                Intent intent = new Intent(SyncManager.ACTION_SYNC_STATUS);
                intent.putExtra(SyncManager.EXTRA_STATUS, SyncStatusCode.UPDATE_AUTO_REPLY_OR_FORWARD);
                context.sendBroadcast(intent);
                return VMA_PROVISION_OK;
            } else {
                if (response.has("statusinfo")) {
                    status = response.getString("statusinfo");
                }
                return handleKnownServerError(status);
            }
        } catch (IOException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("MessagingAssistant:", e);
            }
        } catch (JSONException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("MessagingAssistant:", e);
            }
        } catch (RemoteException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("MessagingAssistant:", e);
            }

        } catch (OperationApplicationException e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error("MessagingAssistant:", e);
            }

        }
        return VMA_PROVISION_ERROR;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.verizon.messaging.vzmsgs.VMAProvision#doHandsetProvisioning(java.lang.String,
     * java.lang.String, com.verizon.messaging.vzmsgs.VMAProvision.ProvisionStatusListener)
     */
    @Override
    public int registerHandset(String mdn, String deviceModel, ProvisionStatusListener listener) {
        int response = VMA_PROVISION_ERROR;
        try {
            // Generate PIN
            // Intercept the pin
            // check the subscription using vma_query user
            // create a mailbox if need

            sendStatus(SyncStatusCode.VMA_PROVISION_GENERATE_PIN);

            PINIntercepters intercepters = new PINIntercepters();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("doHandsetProvisioning:mdn=" + mdn);
            }
            // Request PIN
            lock = new Object();

            IntentFilter filter = new IntentFilter(Intents.SMS_RECEIVED_ACTION);
            // filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            // GOSMS app is using Integer.MAX_VALUE as the priority, we hve to set more
            filter.setPriority(Integer.MAX_VALUE);
            context.registerReceiver(intercepters, filter);
            // 1. Generate PIN
            if ((response = generatePIN(mdn, deviceModel, true)) == VMA_PROVISION_OK) {
                isPinReceived = false;
                // wait for 2 minutes
                synchronized (lock) {
                    try {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Waiting for Silent SMS PIN");
                        }
                        // two minutes time out
                        lock.wait(120000);
                        // To abort the broadcast.
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }

                }
                // 2. Validating PIN
                if (isPinReceived && (response = isValidLoginPIN(mdn, pin)) == VMA_PROVISION_OK) {
                    // 3.) VMA Subscription
                    response = isVMASubscriber(mdn);
                    if (response == VMA_PROVISION_VMA_SUBSCRIBER) {
                        if (!ApplicationSettings.getInstance().getBooleanSetting(
                                AppSettings.KEY_SMS_CHECKSUM_COMPLETED)) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("Handset was already a VMA sub, starting thread to do indexing old messages");
                            }
                            SmsChecksumBuilder builder = new SmsChecksumBuilder(settings, getMaxSmsId());
                            // We are already in the worker thread , we don't have to start a new thread again
                            builder.run();
                        }
                        response = VMA_PROVISION_OK;
                    } else if (response == VMA_PROVISION_NOT_VMA_SUBCRIBER) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Handset was not a VMA sub, no need to index old messages since they are not going to be on VMA");
                        }
                        settings.put(AppSettings.KEY_SMS_CHECKSUM_COMPLETED, true);
                        response = VMA_PROVISION_OK;
                    }
                } else {
                    response = VMA_PROVISION_PIN_RETRIEVAL_FAILED;
                }

            }
            context.unregisterReceiver(intercepters);

        } catch (Exception e) {

        }

        return 0;
    }

}
