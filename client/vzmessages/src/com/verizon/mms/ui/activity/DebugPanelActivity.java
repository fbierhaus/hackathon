/**
 * DebugPanelActivity.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;

import nbisdk.va;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.support.v4.view.ViewPager.LayoutParams;
import android.text.Html;
import android.text.TextUtils;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.strumsoft.android.commons.logger.Logger;
import com.sun.mail.iap.ProtocolException;
import com.verizon.common.VZUris;
import com.verizon.common.security.AESEncryption;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority;
import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.messaging.vzmsgs.provider.dao.MapperDao;
import com.verizon.mms.ContentType;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.transaction.TransactionSettings;
import com.verizon.mms.ui.VZMPreferenceActivity;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.sync.SyncManager;
import com.vzw.vma.message.VMAChangedSinceResponse;
import com.vzw.vma.message.VMAMessageResponse;
import com.vzw.vma.message.impl.VMAStoreJavaMailImpl;
import com.vzw.vma.sync.refactor.impl.MapperDaoImpl;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jun 6, 2012
 */
public class DebugPanelActivity extends VZMPreferenceActivity implements OnPreferenceClickListener,
        OnPreferenceChangeListener {
    private static final String KEY_VALIDATE_MSG = "validate_msg";
    private static final String KEY_CLEAR_REPORTS = "pdu_clear_reports";
    private static final String KEY_CLEAR_PDU_PARTS = "pdu_clear_parts";
    private static final String KEY_ENABLE_NOTIFICATION = "key_enable_notification";
    private static final String SIMULATE_ERROR_CODES = "simulate_error_codes";
    private static final String KEY_VMA_UPDATE_CREDENTIALS = "pref_key_update_credentials";
    private static final String SIMULATE_SYNC_ERRORS = "pref_key_serverError";
    private static final String SIMULATE_PROVISIONING_ERRORS = "pref_key_serverErrors";
    
    private static final String KEY_VMA_MDN= "pref_key_vma_mdn";
    private static final String KEY_VMA_TOKEN = "pref_key_vma_token";
    private static final String KEY_FIX_MMS_TIMESTAMP_ISSUE = "pref_fix_mms_time_issue";

    private Preference clearSyncData;
    private Preference clearPairingData;
    private Preference deleteAll;
    private Preference cleanUpDeliveryReports;
    private Preference cleanUpPDUParts;
    private Preference clearMmsTime;
    private Preference fetchitems;
    private Preference senditems;
    private Preference exportLogs;
    private Preference exportInMail;
    private Preference smsCount;
    private Preference mmsCount;
    private Preference deliveryCount;
    private Preference validateMsg;
    private EditTextPreference smsGenCount;
    private ProgressDialog dialog;
    private ListPreference syncErrorList;
    private ListPreference provisionErrorList;

    private Preference maxMemory;
    private Preference totalMemory;
    private Preference freeMemory;
    private Preference usedMemory;
    private Preference unreadsmsCount;
    private Preference unreadmmsCount;
    private Preference oldunreadsmsCount;
    private Preference oldunreadmmsCount;
    private Preference mapping;
    private Preference mapping1;
    private Preference mapping2;
    private Preference updateCredentials;
    private Preference vmaMdn;
    private Preference vmaToken;

    private CheckBoxPreference enableNotification;
    private CheckBoxPreference simulateerror;

    private SyncStatusReceiver mSyncStatusReceiver;
    private TextView syncProgressStatus;
    private TextView syncNonProgressStatus;
    public static final String MESSAGE_TYPE = "m_type";
    public static final String READ = "read";
    public static final String SEEN = "seen";

    public static final int MESSAGE_TYPE_NOTIFICATION_IND = 0x82;
    public static final int MESSAGE_TYPE_NOTIFYRESP_IND = 0x83;
    public static final int MESSAGE_TYPE_RETRIEVE_CONF = 0x84;

    private static final String NEW_UNREAD_MMS_SELECTION_WIDGET = "(" + MESSAGE_TYPE + "="
            + MESSAGE_TYPE_RETRIEVE_CONF + " OR " + MESSAGE_TYPE + "=" + MESSAGE_TYPE_NOTIFICATION_IND
            + ") AND " + READ + "=0 and thread_id in (select _id from threads)";

    private static final String UNREAD_MMS_SELECTION_WIDGET = "(" + MESSAGE_TYPE + "="
            + MESSAGE_TYPE_RETRIEVE_CONF + " OR " + MESSAGE_TYPE + "=" + MESSAGE_TYPE_NOTIFICATION_IND
            + ") AND " + READ + "=0";

    private AppSettings settings;

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = ApplicationSettings.getInstance(this);
        addPreferencesFromResource(R.xml.debug_panel);
        setContentView(R.layout.debug_panel_layout);
        clearSyncData = (Preference) findPreference("wifi_clear_sync_data");
        clearPairingData = (Preference) findPreference("wifi_clear_pair_data");
        deleteAll = (Preference) findPreference("delete_all");
        senditems = (Preference) findPreference("senditems");
        fetchitems = (Preference) findPreference("fetchitems");
        enableNotification = (CheckBoxPreference) findPreference(KEY_ENABLE_NOTIFICATION);
        simulateerror = (CheckBoxPreference) findPreference(SIMULATE_ERROR_CODES);
        mapping = (Preference) findPreference("vmamapping");
        mapping1 = (Preference) findPreference("vmamapping1");
        mapping2 = (Preference) findPreference("vmamapping2");
        exportLogs = (Preference) findPreference("export_logs");
        exportInMail = (Preference) findPreference("send_email");
        smsGenCount = (EditTextPreference) findPreference("generate_sms");
        updateCredentials = (Preference) findPreference("pref_key_update_credentials");
        smsGenCount.setOnPreferenceChangeListener(this);
        smsCount = (Preference) findPreference("sms_count");
        mmsCount = (Preference) findPreference("mms_count");
        unreadsmsCount = (Preference) findPreference("newunreadsms_count");
        unreadmmsCount = (Preference) findPreference("newunreadmms_count");
        oldunreadsmsCount = (Preference) findPreference("unreadsms_count");
        oldunreadmmsCount = (Preference) findPreference("unreadmms_count");
        deliveryCount = (Preference) findPreference("delivery_count");
        cleanUpDeliveryReports = (Preference) findPreference(KEY_CLEAR_REPORTS);
        cleanUpPDUParts = (Preference) findPreference(KEY_CLEAR_PDU_PARTS);
        clearMmsTime = (Preference) findPreference(KEY_FIX_MMS_TIMESTAMP_ISSUE);
        validateMsg = (Preference) findPreference(KEY_VALIDATE_MSG);
        
        updateCredentials = (Preference) findPreference(KEY_VMA_UPDATE_CREDENTIALS);
        vmaMdn = (Preference) findPreference(KEY_VMA_MDN);
        vmaToken = (Preference) findPreference(KEY_VMA_TOKEN);
        
        vmaMdn.setTitle("MDN : "+settings.getMDN());
        vmaMdn.setSummary("MDN used for VMA Sync");
        vmaToken.setTitle("TOKEN : "+settings.getDecryptedLoginToken());
        vmaToken.setSummary("Token used for VMA Sync");
        
        syncErrorList = (ListPreference) findPreference(SIMULATE_SYNC_ERRORS);
        provisionErrorList = (ListPreference) findPreference(SIMULATE_PROVISIONING_ERRORS);
        smsCount.setSummary("" + getSMSCount());
        mmsCount.setSummary("" + getMMSCount());
        oldunreadsmsCount.setSummary("" + getUnreadSmsFromInbox(this));
        oldunreadmmsCount.setSummary("" + getUnreadMmsFromInbox(this));
        unreadsmsCount.setSummary("" + getNewUnreadSmsFromInbox(this));
        unreadmmsCount.setSummary("" + getNewUnreadMmsFromInbox(this));
        deliveryCount.setSummary("" + getMMSDeliveryCount());

        try {
            // PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            // versionName.setTitle("Version Name :" + info.versionName);
            // versionCode.setTitle("Version Code :" + info.versionCode);
            // if (info.versionName != null) {
            // String[] value = info.versionName.split("-", 2);
            // senditems.setTitle("Version :" + value[0]);
            // fetchitems.setTitle("Build No:" + value[1]);
            // }

            String sendPending = SyncItem._PRIORITY + "<=" + ItemPriority.SEND_MAX.getValue() + " AND "
                    + SyncItem._PRIORITY + ">=" + ItemPriority.SEND_MIN.getValue();

            String sendDeffered = SyncItem._PRIORITY + "=" + ItemPriority.DEFFERED.getValue() + " AND ("
                    + SyncItem._LAST_PRIORITY + "<=" + ItemPriority.SEND_MAX.getValue() + " AND "
                    + SyncItem._LAST_PRIORITY + ">=" + ItemPriority.SEND_MIN.getValue() + ")";

            String send = "SEND: Pending: " + getCount(SyncItem.CONTENT_URI, sendPending) + ", deffered: "
                    + getCount(SyncItem.CONTENT_URI, sendDeffered);

            senditems.setTitle(send);

            senditems.setSummary("Items in sending queued.");

            String fetch = SyncItem._PRIORITY + "<=" + ItemPriority.ONDEMAND_MAX.getValue() + " AND "
                    + SyncItem._PRIORITY + ">" + ItemPriority.FULLSYNC_MIN.getValue();

            String fetchDeffered = SyncItem._PRIORITY + "==" + ItemPriority.DEFFERED.getValue() + " AND ("
                    + SyncItem._LAST_PRIORITY + "<=" + ItemPriority.ONDEMAND_MAX.getValue() + " AND "
                    + SyncItem._LAST_PRIORITY + ">" + ItemPriority.FULLSYNC_MIN.getValue() + ")";
            String fetchMsg = "FETCH:Pending: " + getCount(SyncItem.CONTENT_URI, fetch) + " ,deffered: "
                    + getCount(SyncItem.CONTENT_URI, fetchDeffered);
            fetchitems.setTitle(fetchMsg);

            int fullsyncAttachments = getCount(SyncItem.CONTENT_URI, SyncItem._PRIORITY + "="
                    + ItemPriority.FULLSYNC_ATTACHMENT.getValue() + " OR " + SyncItem._PRIORITY + "="
                    + ItemPriority.FULLSYNCOLDER_ATTACHMENT.getValue());

            int fullMsgCount = getCount(SyncItem.CONTENT_URI, SyncItem._PRIORITY + "="
                    + ItemPriority.FULLSYNCOLDER_MESSAGES.getValue() + " OR " + SyncItem._PRIORITY + "="
                    + ItemPriority.FULLSYNC_CRITICAL.getValue());

            int fastSyncAttachments = getCount(SyncItem.CONTENT_URI, SyncItem._PRIORITY + "="
                    + ItemPriority.ONDEMAND_ATTACHMENT.getValue());

            int fastMsgCount = getCount(SyncItem.CONTENT_URI, SyncItem._PRIORITY + "="
                    + ItemPriority.ONDEMAND_CRITICAL.getValue());

            // PENDING MSG= ATTACHEMENTS=

            String fetchSummary = "Fullsync Msg :" + (fullMsgCount) + " ,Attachements :"
                    + (fullsyncAttachments);
            fetchSummary += "\n Fastsync Msg :" + (fastMsgCount) + " ,Attachements :" + (fastSyncAttachments);

            fetchitems.setSummary(fetchSummary);

            String mappedWhere = VMAMapping._UID + "> 0 AND " + VMAMapping._LUID + ">0";
            String unmappedWhere = VMAMapping._UID + "<= 0 OR " + VMAMapping._LUID + "<=0";
            String pendingEvents = VMAMapping._PENDING_UI_EVENTS + "> 0 ";

            mapping.setTitle("Mapping: mapped=" + getCount(VMAMapping.CONTENT_URI, mappedWhere));
            mapping1.setTitle("Mapping: unmapped=" + getCount(VMAMapping.CONTENT_URI, unmappedWhere));
            mapping2.setTitle("Mapping: waiting to pair=" + getCount(VMAMapping.CONTENT_URI, pendingEvents));

        } catch (Exception e) {
            Logger.error(e);
        }
        clearSyncData.setOnPreferenceClickListener(this);
        clearPairingData.setOnPreferenceClickListener(this);
        deleteAll.setOnPreferenceClickListener(this);
        exportLogs.setOnPreferenceClickListener(this);
        exportInMail.setOnPreferenceClickListener(this);
        syncErrorList.setOnPreferenceChangeListener(this);
        provisionErrorList.setOnPreferenceChangeListener(this);
        updateCredentials .setOnPreferenceClickListener(this);
        validateMsg.setOnPreferenceClickListener(this);
        cleanUpDeliveryReports.setOnPreferenceClickListener(this);

        cleanUpPDUParts.setOnPreferenceClickListener(this);
        clearMmsTime.setOnPreferenceClickListener(this);
        String dummy = getDummyPartsCount(getMaxMmsId())
                + " Dummy thumnail attachement found. Tap here to delete the records";
        cleanUpPDUParts.setSummary(dummy);

        TransactionSettings settings = new TransactionSettings(this, null);
        clearSyncData.setSummary(settings.getMmscUrl());
        clearPairingData.setSummary(ApplicationSettings.getInstance().getImapHost());
        // AndroidUtil.init(this);
        // Memory
        maxMemory = (Preference) findPreference("max_memory");
        totalMemory = (Preference) findPreference("total_memory");
        freeMemory = (Preference) findPreference("free_memory");
        usedMemory = (Preference) findPreference("used_memory");

        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        totalMemory.setSummary(" Total " + (runtime.totalMemory() / 1048576F) + " Mb");
        freeMemory.setSummary(" Free  " + (runtime.freeMemory() / 1048576F) + " Mb");
        maxMemory.setSummary(" Max   " + (runtime.maxMemory() / 1048576F) + " Mb");
        usedMemory.setSummary(" Usage " + (memoryUsed / (1048576F)) + "Mb");
        // register the syncstatus receiver
        mSyncStatusReceiver = new SyncStatusReceiver();
        syncProgressStatus = (TextView) findViewById(R.id.progressStatus);
        syncNonProgressStatus = (TextView) findViewById(R.id.nonProgressStatus);
        enableNotification.setOnPreferenceClickListener(this);

        simulateerror.setOnPreferenceClickListener(this);

        registerReceiver(mSyncStatusReceiver, new IntentFilter(SyncManager.ACTION_SYNC_STATUS));
    }

    /**
     * This Method
     * 
     * @param pendingSyncItems
     * @param selection
     * @param sortOrder
     * @return
     */
    private int getCount(Uri uri, String selection) {
        int res = 0;
        Cursor c = getContentResolver().query(uri, null, selection, null, null);
        if (c != null) {
            if (c.getCount() > 0) {
                res = c.getCount();
            }
            c.close();
        }
        return res;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.preference.Preference.OnPreferenceClickListener#onPreferenceClick
     * (android.preference.Preference )
     */
    @Override
    public boolean onPreferenceClick(Preference preference) {
        Logger.debug("onPreferenceClick():Preference key=" + preference.getKey());
        if (preference.getKey().equalsIgnoreCase("export_logs")) {
            new ExportLogs(false).execute();
        } else if (preference.getKey().equalsIgnoreCase("send_email")) {
            new ExportLogs(true).execute();
        } else if (preference.getKey().equalsIgnoreCase("delete_all")) {

        } else if (preference.getKey().equalsIgnoreCase("wifi_clear_sync_data")) {
        }

        else if (preference.getKey().equalsIgnoreCase(KEY_ENABLE_NOTIFICATION)) {
            Logger.debug("Enable Notification :" + enableNotification.isChecked());
            ApplicationSettings.getInstance().put(AppSettings.KEY_DEBUG_VMA_STATUSBAR_NOTIFICATION,
                    enableNotification.isChecked());

        } else if (preference.getKey().equalsIgnoreCase(KEY_CLEAR_PDU_PARTS)) {
            Logger.debug("Clearing PDU parts ");
            clearPDUParts();
        } else if (preference.getKey().equalsIgnoreCase(KEY_FIX_MMS_TIMESTAMP_ISSUE)) {
            Logger.debug("Clearing Invalid MMS  time");
            clearMMSTime();
        } else if (preference.getKey().equalsIgnoreCase(KEY_CLEAR_REPORTS)) {
            Logger.debug("Clearing delivery reports");
            String where = Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_DELIVERY_IND + " AND "
                    + Mms.THREAD_ID + "=" + Long.MAX_VALUE;
            int count = getContentResolver().delete(VZUris.getMmsUri(), where, null);
            Logger.debug(count + " delivery reports got deleted.");
            Toast.makeText(this, count + " delivery reports got deleted.", Toast.LENGTH_SHORT).show();
        } else if (preference.getKey().equalsIgnoreCase(KEY_VALIDATE_MSG)) {
            Logger.debug("", "innn validate_msg");
            doValidate();
        } else if (preference.getKey().equalsIgnoreCase(SIMULATE_ERROR_CODES)) {
            Logger.debug("Enable errorcodes :" + simulateerror.isChecked());

            if (!simulateerror.isChecked()) {
                // remove simulate
                settings.removeSettings(AppSettings.KEY_VMA_SIMULATE_ERROR);
                settings.removeSettings(AppSettings.KEY_VMA_SIMULATE_PROVISIONING_ERROR);
            }
        } else if (preference.getKey().equalsIgnoreCase(KEY_VMA_UPDATE_CREDENTIALS)) {
            Logger.debug("Updating credentials");
            showUpdateCredentialsDialog(R.string.enter_credentials);
        }

        return false;
    }

    /**
     * This Method 
     */
    private void clearMMSTime() {

        new AsyncTask<Void, String, Exception>() {
            private ProgressDialog dialog;
            int count = 0;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(DebugPanelActivity.this, null, "Clearing Invalid MMS time ...");
            };

            /*
             * Overriding method (non-Javadoc)
             * 
             * @see android.os.AsyncTask#onProgressUpdate(Progress[])
             */
            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);
                dialog.setMessage(values[0]);
            }

            protected void onPostExecute(Exception result) {
                dialog.dismiss();
                String msg = count + " MMS  time update to seconds.";
                if (result != null) {
                    msg = "failed to update MMS time to seconds";
                }
                Toast.makeText(DebugPanelActivity.this, msg, Toast.LENGTH_SHORT).show();

            };

            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    
                    
                    long maxId = 0;
                    String[] projection = new String[] { Mms._ID ,Mms.DATE};
                    HashMap<Long, Long> invalidIds =new HashMap<Long, Long>();
                    Cursor c = getContentResolver().query(VZUris.getMmsUri(), projection, null, null, null);
                    if (c != null) {
                        while (c.moveToNext()) {
                            maxId = c.getLong(1);
                            if(maxId > Integer.MAX_VALUE){
                                invalidIds.put(c.getLong(0), (maxId/1000));
                            }
                        }
                        c.close();
                    }
                    long size = invalidIds.size();
                    count=(int)size;
                    Logger.debug("Invalid timestamp found: "+ size);
                    Iterator<Long> iterator = invalidIds.keySet().iterator();
                    int i=0;
                    while (iterator.hasNext()) {
                        long id = (Long) iterator.next();
                        ContentValues values = new ContentValues(1);
                        values.put(Mms.DATE, invalidIds.get(id)); 
                        SqliteWrapper.update(DebugPanelActivity.this,ContentUris.withAppendedId(VZUris.getMmsUri(), id), values,null, null);
                        publishProgress("Processing " + (++i)+" of "+  size);
                    }
                    
                    publishProgress(size + " MMS time updated to seconds");
                    
                    
                    
                } catch (Exception e) {
                    return e;
                }
                return null;
            };

        }.execute();

            
    }

    private void showUpdateCredentialsDialog(int title) {
        
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText mdnEditText = new EditText(this);
        final EditText pwdEditText = new EditText(this);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView t1 = new TextView(this);
        t1.setText("MDN");
        TextView t2 = new TextView(this);
        t2.setText("Login Token");
        
        layout.addView(t1);
        layout.addView(mdnEditText);
        layout.addView(t2);
        layout.addView(pwdEditText);
        alert.setMessage("Enter credentials");
        alert.setView(layout);
        
        if(settings.isProvisioned()){
            mdnEditText.setText(settings.getMDN());
            pwdEditText.setText(settings.getDecryptedLoginToken());
        }
        
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String mdn = mdnEditText.getText().toString();
              String loginToken = pwdEditText.getText().toString();
              if (!TextUtils.isEmpty(mdn) && !TextUtils.isEmpty(loginToken)) {
                  Logger.debug("Updating logintoken and mdn.mdn=" + mdn + ",token =" + loginToken);
                  String key = getKey();
                  try {
                      loginToken = AESEncryption.encrypt(key.getBytes(), loginToken);
                      Logger.debug("Login token: key=" + key + ",encryptedToken=" + loginToken
                              + ", serverToken=" + loginToken);
                      settings.put(ApplicationSettings.KEY_VMA_KEY, key);
                      settings.put(ApplicationSettings.KEY_VMA_TOKEN, loginToken);
                      settings.put(ApplicationSettings.KEY_VMA_MDN, mdn);
                      // Force restarting the application 
                      System.exit(1);
                      
                  } catch (Exception e) {
                      e.printStackTrace();
                      Logger.debug("Unable to simulate error codes");
                  }
              }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        alert.show();  
        
        
//
//        final Provisioningdialog dialog = new Provisioningdialog(this, R.layout.tabletlogin);
//        final EditText mdnEditText = (EditText) dialog.findViewById(R.id.vzm_dialog_mdn);
//        final EditText pwdEditText = (EditText) dialog.findViewById(R.id.vzm_dialog_username);
//        final Button saveButton = (Button) dialog.findViewById(R.id.send_mdn);
//
//        if (settings.isProvisioned()) {
//            pwdEditText.setText(settings.getMDN());
//            mdnEditText.setText(settings.getDecryptedLoginToken());
//        }
//
//        saveButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//            }
//        });
//        dialog.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //
//                String mdn = mdnEditText.getText().toString();
//                String loginToken = pwdEditText.getText().toString();
//                if (!TextUtils.isEmpty(mdn) && !TextUtils.isEmpty(loginToken)) {
//                    Logger.debug("Updating logintoken and mdn.mdn=" + mdn + ",token =" + loginToken);
//                    String key = getKey();
//                    try {
//                        loginToken = AESEncryption.encrypt(key.getBytes(), loginToken);
//                        Logger.debug("Login token: key=" + key + ",encryptedToken=" + loginToken
//                                + ", serverToken=" + loginToken);
//                        settings.put(ApplicationSettings.KEY_VMA_KEY, key);
//                        settings.put(ApplicationSettings.KEY_VMA_TOKEN, loginToken);
//                        settings.put(ApplicationSettings.KEY_VMA_MDN, mdn);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        Logger.debug("Unable to simulate error codes");
//                    }
//                }
//                dialog.dismiss();
//            }
//        });
//        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//
//            @Override
//            public void onCancel(DialogInterface dialog) {
//                dialog.dismiss();
//            }
//        });
//
//        dialog.show();
    }

    private class Provisioningdialog extends Dialog {

        public Provisioningdialog(Context context, int layoutId) {
            super(context, R.style.ThemeDialog);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(layoutId);
            getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        }
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

    /**
     * This Method
     */
    private void clearPDUParts() {
        new AsyncTask<Void, String, Exception>() {
            private ProgressDialog dialog;
            int count = 0;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(DebugPanelActivity.this, null, "Clearing dummy thumbmail ...");
            };

            /*
             * Overriding method (non-Javadoc)
             * 
             * @see android.os.AsyncTask#onProgressUpdate(Progress[])
             */
            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);
                dialog.setMessage(values[0]);
            }

            protected void onPostExecute(Exception result) {
                dialog.dismiss();
                String msg = count + " dummy thumbnail parts deleted.";
                if (result != null) {
                    msg = "failed to delete the dummy thumbmails";
                }
                Toast.makeText(DebugPanelActivity.this, msg, Toast.LENGTH_SHORT).show();

            };

            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    long maxid = getMaxMmsId();
                    long fakeCount = getDummyPartsCount(maxid);
                    Logger.debug("MAX MMSID=" + maxid);
                    Logger.debug("Dummpy message Id =" + fakeCount);
                    String where = Mms.Part.MSG_ID + " > " + maxid;
                    long maxCount = getDummyPartsCount(maxid);
                    String[] projection = new String[] { Mms.Part._ID, Mms.Part.MSG_ID };
                    int count = 0;
                    Cursor c = getContentResolver().query(VZUris.getMmsPartUri(), projection, where, null,
                            null);
                    if (c != null) {
                        int i = 0;
                        while (c.moveToNext()) {
                            long id = c.getLong(0);
                            Uri uri = ContentUris.withAppendedId(VZUris.getMmsPartUri(), id);
                            publishProgress("deleting " + (++i) + " of " + maxCount);
                            count = SqliteWrapper.delete(DebugPanelActivity.this, getContentResolver(), uri,
                                    where, null);
                        }
                        c.close();
                    }
                    Logger.debug("Dummy parts deleted : count =" + count);

                } catch (Exception e) {
                    return e;
                }
                return null;
            };

        }.execute();

    }

    private long getDummyPartsCount(long maxMsgId) {
        String[] projection = new String[] { Mms.Part._ID, Mms.Part.MSG_ID };
        String where = Mms.Part.MSG_ID + " > " + maxMsgId;
        int count = 0;
        Cursor c = getContentResolver().query(VZUris.getMmsPartUri(), projection, where, null, null);
        if (c != null) {
            count = c.getCount();
            c.close();
        }
        Logger.debug("DEBUG ==============Dummy parts count=" + count);
        return count;
    }

    /**
     * This Method
     * 
     * @param maxUid
     * @return
     */
    private long getMaxMmsId() {

        long maxId = 0;
        String[] projection = new String[] { "MAX(_id) as maxMMSID" };
        Cursor c = getContentResolver().query(VZUris.getMmsUri(), projection, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                maxId = c.getLong(0);
            }
            c.close();
        }
        Logger.debug("DEBUG ==============MAX mms id=" + maxId);
        return maxId;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.preference.PreferenceActivity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        unregisterReceiver(mSyncStatusReceiver);
        super.onDestroy();
    }

    class ExportLogs extends AsyncTask<Void, Void, Exception> {
        boolean asEmailAttachement;
        String logs;

        /**
         * 
         * Constructor
         */
        public ExportLogs(boolean asEmailAttachement) {
            this.asEmailAttachement = asEmailAttachement;
            String path = null;
            if (getExternalCacheDir() != null) {
                path = getExternalCacheDir().getAbsolutePath();
            } else {
                Toast.makeText(DebugPanelActivity.this,
                        "External directory not found. saving on phone memory.path=:" + logs,
                        Toast.LENGTH_SHORT).show();
                path = getCacheDir().getAbsolutePath();
            }
            logs = path + "/vzm-logs.zip";

        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(DebugPanelActivity.this, null, "Exporting Logs");
            super.onPreExecute();
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Exception doInBackground(Void... params) {
            Logger.getInstance().exportLog(logs);
            return null;
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Exception result) {
            super.onPostExecute(result);
            dialog.dismiss();
            if (asEmailAttachement) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType(ContentType.TEXT_HTML);
                i.putExtra(Intent.EXTRA_EMAIL, new String[] { "jegadeesan@strumsoft.com" });
                i.putExtra(Intent.EXTRA_SUBJECT, Build.MANUFACTURER + "-" + Build.MODEL + "logs-"
                        + new Date());
                StringBuilder builder = new StringBuilder();
                builder.append("<P>");
                builder.append("================DEVICE INFO ================<Br>");
                builder.append("Model        :" + Build.MODEL + "<Br>");
                builder.append("MANUFACTURER :" + Build.MANUFACTURER + "<Br>");
                builder.append("PRODUCT      :" + Build.PRODUCT + "<Br>");
                builder.append("SDK          :" + Build.VERSION.SDK + "<Br>");
                builder.append("SDK INT      :" + Build.VERSION.SDK_INT + "<Br>");
                builder.append("CODE NAME    :" + Build.VERSION.CODENAME + "<Br>");
                builder.append("OS VERSION   :" + Build.VERSION.INCREMENTAL + "<Br>");
                builder.append("Log file     :" + logs + "<Br>");
                builder.append("============================================<Br>");
                i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(builder.toString()));
                i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + logs));
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(DebugPanelActivity.this, "There are no email clients installed.",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(DebugPanelActivity.this, "Logs exported path=:" + logs, Toast.LENGTH_SHORT)
                        .show();
            }

        }

    }

    public long getMMSCount() {
        long count = 0;
        String[] projection = new String[] { "COUNT(*) as mmscount" };
        String where = Mms.MESSAGE_TYPE + "=" + 0x80 + " OR " + Mms.MESSAGE_TYPE + "=" + 0x84;
        Cursor c = getContentResolver().query(VZUris.getMmsUri(), projection, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                count = c.getLong(0);
            }
            c.close();
        }
        return count;
    }

    public long getMMSDeliveryCount() {
        long count = 0;
        String[] projection = new String[] { "COUNT(*) as mmscount" };
        String where = Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_DELIVERY_IND;
        Cursor c = getContentResolver().query(VZUris.getMmsUri(), projection, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                count = c.getLong(0);
            }
            c.close();
        }
        return count;
    }

    public long getSMSCount() {
        long count = 0;
        String[] projection = new String[] { "COUNT(*) as smscount" };
        String where = Sms.TYPE + "!=" + Sms.MESSAGE_TYPE_DRAFT;
        Cursor c = getContentResolver().query(VZUris.getSmsUri(), projection, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                count = c.getLong(0);
            }
            c.close();
        }
        return count;
    }

    public void generateSMS(int count) {
        ContentValues values = new ContentValues();
        long time = System.currentTimeMillis();
        for (int i = 1; i <= count; i++) {
            if ((i % 2) != 0) {
                values.put(Sms.ADDRESS, "9258081885");
                values.put(Sms.BODY, "Hello Buddy " + (i) + "</body>");
                values.put(Sms.DATE, (time += 345515));
                values.put(Sms.READ, 1);
                values.put(Sms.TYPE, 1);
                values.put(Sms.STATUS, -1);
                values.put(Sms.LOCKED, 0);
                getContentResolver().insert(VZUris.getSmsUri(), values);
            } else {
                values.put(Sms.ADDRESS, "9258081885");
                values.put(Sms.BODY, "Reply " + (i));
                values.put(Sms.DATE, (time += 345515));
                values.put(Sms.READ, 1);
                values.put(Sms.TYPE, 2);
                values.put(Sms.STATUS, -1);
                values.put(Sms.LOCKED, 0);
                getContentResolver().insert(VZUris.getSmsUri(), values);
            }

        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.preference.Preference.OnPreferenceChangeListener#onPreferenceChange
     * (android.preference.Preference , java.lang.Object)
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {

        if (preference.getKey().endsWith("generate_sms")) {
            String count = smsGenCount.getEditText().getText().toString();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("SMS count :" + count);
            }
            new GenerateSMS(Integer.parseInt(count)).execute();

        } else if (preference.getKey().equalsIgnoreCase(SIMULATE_SYNC_ERRORS)) {

            int errorCode = Integer.parseInt((String) newValue);
            Logger.debug("Simulating error codes :" + errorCode);
            settings.put(AppSettings.KEY_VMA_SIMULATE_ERROR, errorCode);

        } else if (preference.getKey().equalsIgnoreCase(SIMULATE_PROVISIONING_ERRORS)) {

            int errorCode = Integer.parseInt((String) newValue);
            Logger.debug("Simulating provisioning error codes :" + errorCode);
            settings.put(AppSettings.KEY_VMA_SIMULATE_PROVISIONING_ERROR, errorCode);

            //
            // Intent syncFailedIntent = new Intent(SyncManager.ACTION_SYNC_STATUS);
            // syncFailedIntent.putExtra(SyncManager.EXTRA_STATUS, SyncManager.SYNC_STATUS_FAILED);
            // syncFailedIntent.putExtra(SyncManager.EXTRA_ERROR_CODE, errorCode);
            // sendBroadcast(syncFailedIntent);
        }
        return false;
    }

    class GenerateSMS extends AsyncTask<Void, Integer, Exception> {
        int count = 0;
        ProgressDialog dialog;

        /**
         * 
         * Constructor
         */
        public GenerateSMS(int count) {
            this.count = count;
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = ProgressDialog.show(DebugPanelActivity.this, null, "Generating SMS");
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#onProgressUpdate(Progress[])
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int progress = values[0];
            dialog.setMessage("Creating SMS " + progress + " of " + count);
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                ContentValues values = new ContentValues();
                long time = System.currentTimeMillis();
                for (int i = 1; i <= count; i++) {
                    if ((i % 2) != 0) {
                        values.put(Sms.ADDRESS, "9258081885");
                        values.put(Sms.BODY, "Hello Buddy " + (i) + "</body>");
                        values.put(Sms.DATE, (time += 345515));
                        values.put(Sms.READ, 1);
                        values.put(Sms.TYPE, 1);
                        values.put(Sms.STATUS, -1);
                        values.put(Sms.LOCKED, 0);
                        getContentResolver().insert(VZUris.getSmsUri(), values);
                        publishProgress(i);
                    } else {
                        values.put(Sms.ADDRESS, "9258081885");
                        values.put(Sms.BODY, "Reply " + (i));
                        values.put(Sms.DATE, (time += 345515));
                        values.put(Sms.READ, 1);
                        values.put(Sms.TYPE, 2);
                        values.put(Sms.STATUS, -1);
                        values.put(Sms.LOCKED, 0);
                        getContentResolver().insert(VZUris.getSmsUri(), values);
                        publishProgress(i);
                    }

                }

            } catch (Exception e) {
                return e;
            }
            return null;
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Exception result) {
            super.onPostExecute(result);
            dialog.dismiss();
        }

    }

    public static int getNewUnreadSmsFromInbox(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(VZUris.getSmsInboxUri(),
                    new String[] { "count (*) AS count" },
                    "read=0 and thread_id in (select _id from threads)", null, null);
            if (null != cursor && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return -1;
    }

    public static int getNewUnreadMmsFromInbox(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(VZUris.getMmsUri(),
                    new String[] { "count (*) AS count" }, NEW_UNREAD_MMS_SELECTION_WIDGET, null, null);
            if (null != cursor && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return -1;
    }

    public static Integer getUnreadSmsFromInbox(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(VZUris.getSmsInboxUri(),
                    new String[] { "count (*) AS count" }, "read=0", null, null);
            if (null != cursor && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return null;
    }

    public static Integer getUnreadMmsFromInbox(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(VZUris.getMmsUri(),
                    new String[] { "count (*) AS count" }, UNREAD_MMS_SELECTION_WIDGET, null, null);
            if (null != cursor && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return null;
    }

    /*@Override
    protected void onStart() {
        Asimov.activityStarted();
        super.onStart();
    }

    @Override
    protected void onStop() {
        Asimov.activityStoped();
        super.onStop();
    }*/

    private class SyncStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(SyncManager.EXTRA_STATUS, 0);

            if (status == 0) {
                // it could be the pair status
                status = intent.getIntExtra(SyncManager.EXTRA_ACTION, 0);

                // if (status == SyncManager.UNPAIR) {
                // // dismiss all the sync related notifications
                // SyncNotification.clearSyncNotification(mActivity);
                // if (MmsConfig.isTabletDevice()) {
                // MessagingNotification.clearAllNotification(mActivity);
                // }
                // }
            }
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this.getClass(), "onReceiver received status in DebugPanelActivity: " + status);
            }
            long sendingCount;
            long receivedCount;
            long total;
            boolean nonProgressStatus = false;
            String statusMsg = null;
            switch (status) {
            case SyncManager.NO_WIFI_CONNECTION:
                statusMsg = context.getString(R.string.vma_sync_failed) + " "
                        + context.getString(R.string.vma_no_wifi_connection);
                nonProgressStatus = true;
                break;
            case SyncManager.NO_DATA_CONNECTION:
                statusMsg = context.getString(R.string.vma_sync_failed) + " "
                        + context.getString(R.string.vma_no_data_connection);
                nonProgressStatus = true;
                break;
            case SyncManager.SYNC_STATUS_FAILED:
                statusMsg = context.getString(R.string.vma_sync_failed);
                nonProgressStatus = true;
                break;
            case SyncManager.SYNC_STATUS_LOGIN_FAILED:
                statusMsg = context.getString(R.string.vma_fails);
                nonProgressStatus = true;
                break;
            // case SyncStatusListener.SIGNOUT_SEND_CONNECTION:
            // statusMsg = "Sync is Shutdown";
            // nonProgressStatus = true;
            // break;
            // case SyncStatusListener.SYNC_PREPARING:
            // statusMsg = "Preparing for Event";
            // nonProgressStatus = true;
            // break;
            // case SyncStatusListener.FETCHING_CHANGES:
            // statusMsg = context.getString(R.string.sync_connected);
            // sendingCount = intent.getLongExtra(SyncManager.EXTRA_SENDING_COUNT, 0);
            // total = intent.getLongExtra(SyncManager.EXTRA_TOTAL_COUNT, 0);
            // if (sendingCount > 0) {
            // statusMsg = context.getString(R.string.sync_status_sending, sendingCount, total);
            // }
            // break;
            // case SyncStatusListener.FETCHING_MESSAGE:
            // int count = intent.getIntExtra(SyncManager.EXTRA_TOTAL_COUNT, 0);
            // receivedCount = intent.getIntExtra(SyncManager.EXTRA_RECEIVING_COUNT, 0);
            // statusMsg = context.getString(R.string.sync_receiving_messages, receivedCount, count);
            // break;
            // case SyncStatusListener.FETCHING_ATTACHEMENTS:
            // int acount = intent.getIntExtra(SyncManager.EXTRA_TOTAL_COUNT, 1);
            // receivedCount = intent.getIntExtra(SyncManager.EXTRA_RECEIVING_COUNT, 1);
            // statusMsg = context.getString(R.string.vma_download_attachements, receivedCount, acount);
            // break;
            //
            // case SyncStatusListener.FETCHING_CONVERSATION:
            // int changesCount = intent.getIntExtra(SyncManager.EXTRA_TOTAL_COUNT, 1);
            // receivedCount = intent.getIntExtra(SyncManager.EXTRA_RECEIVING_COUNT, 1);
            // if (intent.hasExtra(SyncManager.EXTRA_XCONV_COUNT)) {
            // // full sync
            // int xConvReceivedCount = intent.getIntExtra(SyncManager.EXTRA_XCONV_RECEIVED_COUNT, 1);
            // int xConvCount = intent.getIntExtra(SyncManager.EXTRA_XCONV_COUNT, 1);
            // if (xConvCount > 0) {
            // statusMsg = context.getString(R.string.sync_conversations, xConvReceivedCount,
            // xConvCount);
            //
            // } else {
            // statusMsg = context.getString(R.string.sync_receiving_changes, receivedCount,
            // changesCount);
            // }
            // } else {
            // // fast sync
            // statusMsg = context.getString(R.string.sync_receiving_changes);
            //
            // }
            // break;
            }
            if (TextUtils.isEmpty(statusMsg)) {
                return;
            } else {
                if (nonProgressStatus) {
                    syncNonProgressStatus.setText(statusMsg);
                } else {
                    syncProgressStatus.setText(statusMsg);
                }
            }

        }
    }

    public void doValidate() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Validating Fullsync");
        }
        new ValidateMessage().execute();
    }

    class ValidateMessage extends AsyncTask<Void, Integer, Exception> {

        ProgressDialog dialog;

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = ProgressDialog.show(DebugPanelActivity.this, null, "Validating fullsync..");
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#onProgressUpdate(Progress[])
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Exception result) {
            super.onPostExecute(result);
            dialog.dismiss();
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Exception doInBackground(Void... params) {

            MapperDao dao = new MapperDaoImpl(DebugPanelActivity.this);
            // VMAMessagesDAOImpl dao = new VMAMessagesDAOImpl(DebugPanelActivity.this);
            try {
                VMAStoreJavaMailImpl store = new VMAStoreJavaMailImpl(DebugPanelActivity.this,
                        ApplicationSettings.getInstance(), "A");
                store.selectInbox();
                List<VMAChangedSinceResponse> changes = store.getChangedSince(1);
                ArrayList<VMAChangedSinceResponse> missingMessages = new ArrayList<VMAChangedSinceResponse>();
                ArrayList<VMAChangedSinceResponse> trueUnmatched = new ArrayList<VMAChangedSinceResponse>();
                ArrayList<VMAChangedSinceResponse> matched = new ArrayList<VMAChangedSinceResponse>();
                for (VMAChangedSinceResponse resp : changes) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        long uid = resp.getUID();
                        VMAMapping info = dao.findMappingByUid(uid);
                        if (info == null) {
                            Logger.debug("doValidate: Did not find message, could possibly be deleted,  for for uid="
                                    + uid);
                            missingMessages.add(resp);
                        } else {
                            Logger.debug("doValidate: Found message=" + info.getLuid() + " for uid=" + uid);
                            matched.add(resp);
                        }
                    }
                }

                for (VMAChangedSinceResponse missing : missingMessages) {
                    long uid = missing.getUID();
                    try {
                        VMAMessageResponse resp = store.getUid(uid);
                        if (resp != null) {
                            if (resp.isDeleted()) {
                                Logger.debug("doValidate: uid is deleted from vma and not in our local db "
                                        + uid);
                            } else {
                                Logger.debug("doValidate: ERROR uid exists in VMA but not locally " + uid);
                                trueUnmatched.add(missing);
                            }
                        }
                    } catch (ProtocolException e) {
                        Logger.debug("doValidate: Proto exception " + e + " " + uid);
                    } catch (IOException e) {
                        Logger.debug("doValidate: IO exception " + e + " " + uid);
                        e.printStackTrace();
                    } catch (MessagingException e) {
                        Logger.debug("doValidate: Mess exception " + e + " " + uid);
                    }
                }

                store.signout();

                StringBuilder builder = new StringBuilder();
                builder.append("\n===============================\n");

                if (trueUnmatched.isEmpty()) {
                    builder.append("trueUnmatched uids = 0 \n");
                } else {
                    builder.append("trueUnmatched uids = " + trueUnmatched.size() + "\n");
                    for (VMAChangedSinceResponse vmaChangedSinceResponse : trueUnmatched) {
                        builder.append(vmaChangedSinceResponse + "\n");
                    }
                    builder.append("-------------------------------\n");
                }
                if (missingMessages.isEmpty()) {
                    builder.append("Missing messages =0 \n");

                } else {
                    builder.append("Missing messages uids = " + trueUnmatched.size() + "\n");
                    for (VMAChangedSinceResponse missingMsg : missingMessages) {
                        builder.append(missingMsg + "\n");
                    }
                }
                builder.append("-------------------------------\n");
                if (matched.isEmpty()) {
                    builder.append("Matched messages =0 \n");

                } else {
                    builder.append("Matched messages uids = " + matched.size() + "\n");
                    for (VMAChangedSinceResponse msg : matched) {
                        builder.append(msg + "\n");
                    }
                }

                builder.append("===============================\n");

                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(" Mail Text :" + builder.toString());
                }

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType(ContentType.TEXT_HTML);
                i.putExtra(Intent.EXTRA_EMAIL, new String[] { "jegadeesan@strumsoft.com",
                        "sandeep@strumsoft.com" });
                i.putExtra(Intent.EXTRA_SUBJECT, Build.MANUFACTURER + "-" + Build.MODEL + "-" + new Date());
                i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(builder.toString()));
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(DebugPanelActivity.this, "There are no email clients installed.",
                            Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                return e;
            }
            return null;

        }

    }
}
