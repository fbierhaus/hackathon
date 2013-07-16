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

package com.verizon.mms.ui;

import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_SYNC_OVER_WIFI;

import java.util.Locale;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rocketmobile.asimov.Asimov;
import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vma.provision.ProvisionManager;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.sync.AppErrorCodes;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.ui.activity.ErrorHandler;
import com.verizon.mms.ui.activity.Provisioning;
import com.verizon.sync.SyncManager;

/**
 * With this activity, users can set preferences for MMS and SMS and can access and manipulate SMS messages
 * stored on the SIM.
 */
public class MessagingPreferenceActivity extends VZMPreferenceActivity implements OnPreferenceClickListener,
        OnPreferenceChangeListener {

    // Symbolic names for the keys used for preference lookup
    public static final String EXPIRY_TIME = "pref_key_mms_expiry";
    public static final String PRIORITY = "pref_key_mms_priority";
    public static final String NOTIFICATION_ENABLED = "pref_key_enable_notifications";
    public static final String POPUP_ENABLED = "pref_key_enable_popup";
    public static final String MARK_AS_READ = "pref_key_markas_readmsg";
    public static final String NOTIFICATION_VIBRATE_WHEN = "pref_key_vibrateWhen";
    public static final String APP_FONT_SUPPORT = "pref_key_fontsupport";
    public static final String NOTIFICATION_RINGTONE = "pref_key_ringtone";
    public static final String NOTIFICATION_LED_COLOUR = "pref_key_led_colour";
    public static final String AUTO_DELETE = "pref_key_auto_delete";
    private static final String KEY_ENABLE_WIFI_SYNC = "pref_key_enable_wifi";
    public static final String AUTO_SIGNATURE = "pref_key_auto_signature";
    public static final String AUTO_SIGNATURE_ENABLED = "pref_key_auto_signature_enable";
    private static final String ADVANCE = "pref_key_advance";
    public static final String LANGUAGE_CODE = "pref_key_language_change";
    public static final String HANDSET_LANGUAGE_CODE = "pref_handset_language_code";
    public static final String COUNTRY = "pref_country";
    public static final String COUNTRY_CODE = "pref_country_code";
    public static final String CUSTOMIZE_CONV = "pref_key_conversation_view";
    public static final String VMA_SETTING = "pref_key_vma";

    public static final String APP_FONT_SUPPORT_DEFAULT = "System Default";
    public static final String APP_FONT_SUPPORT_NORMAL = "Normal Text";
    public static final String APP_FONT_SUPPORT_LARGE = "Large Text";
    private boolean mAppFontChanged = false;
    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS = 1;
    // Symbolic names for the keys used for preference lookup
    // public static final String VMA_FORWARD_SETTING = "pref_key_auto_forward";
    public static final String VMA_REPLY_SETTING = "pref_key_auto_reply";
    public static final String VMA_MANAGE_DEVICE = "pref_key_manage_device";
    public static final String KEY_VMA_DEPROVISION = "vma.deprovision";
    public static final String KEY_VMA_FEATURES = "pref_key_vma_features";
    public static final String KEY_VMA_ASSISTANT = "pref_key_messaging_assistant";
    public static final String KEY_VMA_MESSAGE_SETTING = "pref_key_message_setting";
    public static final String KEY_VMA_PAIRED_ACCOUNT = "pref_key_paired_account";
    public static final String KEY_VMA_DEREGISTER_DEVICE = "pref_key_deregister";

    private ListPreference mLanguagePref;
    private RingtonePreference mRingtonePref;

    private Preference pairedAccount;
    private Preference deregisterDevice;
    private Preference manageLinkedDevices;
    private Preference subscribeOrUnsubscribe;
    // private Preference mForwardSetting;
    private Preference autoReplySetting;
    private CheckBoxPreference syncOverWifiCheckBox;
    private Context context;
    private ApplicationSettings settings;
    private ProvisionManager provMng;

    private ListPreference mVibrateWhenPref;
    private Preference mAdvanceSetting;
    private ListPreference mfontSupportPref;
    private Preference mCustomizeConv;
    private Preference mAutoSignaturePref;
    private SharedPreferences.Editor editPrefs;
    private SharedPreferences sharedPreferences;
    private static CheckBoxPreference mAutoSignatureEnable;
    private boolean isTablet;
    private SyncStatusReceiver mSyncStatusReceiver;

    public static final int REQUEST_WIFI_SYNC_PAIRING = 10101;
    public static final int RESULT_WIFI_SYNC_PAIRING_FAILED = 10102;
    public static final int RESULT_WIFI_SYNC_PAIRING_SUCCESS = 10103;

    public static final String EXTRA_FONT_CHANGED = "font_changed";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.preferences);
        context = this;
        settings = ApplicationSettings.getInstance();
        provMng = new ProvisionManager(this);
        isTablet = MmsConfig.isTabletDevice();
        if (isTablet) {
            ((PreferenceGroup) findPreference("pref_key_application_settings"))
                    .removePreference(findPreference("pref_key_mms_auto_response_read_reports"));
        }
        if (!MmsConfig.isNotificationLedSupported()) {
            ((PreferenceGroup) findPreference("pref_key_application_settings"))
                    .removePreference(findPreference(NOTIFICATION_LED_COLOUR));
        }

        initViews();
        // If needed, migrate vibration setting from a previous version

        if (!sharedPreferences.contains(NOTIFICATION_VIBRATE_WHEN)) {
            mVibrateWhenPref.setValue(getString(R.string.prefDefault_vibrateWhen));
        }
        mSyncStatusReceiver = new SyncStatusReceiver();
        registerReceiver(mSyncStatusReceiver, new IntentFilter(SyncManager.ACTION_SYNC_STATUS));
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

    @Override
    protected void onResume() {
        initVMAView();
        super.onResume();
    }

    /*
     * @Override protected void onStart() { super.onStart(); Asimov.activityStarted(); }
     * 
     * @Override protected void onStop() { Asimov.activityStoped(); super.onStop(); }
     */

    private String getRingtone(String ring) {
        Uri mUri = Uri.parse(ring.toString());
        String filename = null;
        String[] projection = { MediaStore.Images.Media.TITLE };
        if (ring.length() == 0) {
            return getString(R.string.ringtone_silent);
        }
        try {

            Cursor cursor = getContentResolver().query(mUri, projection, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
                cursor.moveToFirst();
                filename = cursor.getString(column_index);
                cursor.close();
            }
        } catch (Exception e) {
            return getString(R.string.ringtone_default);
        }
        if (filename != null) {
            return filename;
        }
        return "";
    }

    private void initViews() {
        setTitle(R.string.preferences_title);
        mAdvanceSetting = findPreference(ADVANCE);
        mCustomizeConv = findPreference(CUSTOMIZE_CONV);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mRingtonePref = (RingtonePreference) findPreference(NOTIFICATION_RINGTONE);
        mRingtonePref.setSummary(getRingtone(sharedPreferences.getString(NOTIFICATION_RINGTONE, "")));
        mRingtonePref.setOnPreferenceChangeListener(this);
        mVibrateWhenPref = (ListPreference) findPreference(NOTIFICATION_VIBRATE_WHEN);
        mVibrateWhenPref.setSummary(mVibrateWhenPref.getEntry());
        mVibrateWhenPref.setOnPreferenceChangeListener(this);
        mfontSupportPref = (ListPreference) findPreference(APP_FONT_SUPPORT);
        mfontSupportPref.setOnPreferenceChangeListener(this);
        mfontSupportPref.setSummary(mfontSupportPref.getEntry());
        mLanguagePref = (ListPreference) findPreference(LANGUAGE_CODE);
        if (mLanguagePref.getEntry() != null) {
            mLanguagePref.setSummary(mLanguagePref.getEntry());
        } else {
            mLanguagePref.setSummary(setDefaultLanguageSummery(this));
        }
        mAutoSignaturePref = findPreference(AUTO_SIGNATURE);
        mAutoSignatureEnable = (CheckBoxPreference) findPreference(AUTO_SIGNATURE_ENABLED);
        editPrefs = PreferenceManager.getDefaultSharedPreferences(this).edit();

        mAutoSignatureEnable.setOnPreferenceChangeListener(this);

        setSignatureDisplay();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // mForwardSetting = findPreference(VMA_FORWARD_SETTING);
        autoReplySetting = findPreference(VMA_REPLY_SETTING);

        pairedAccount = findPreference(KEY_VMA_PAIRED_ACCOUNT);
        pairedAccount.setSummary(ApplicationSettings.getInstance().getMDN());
        deregisterDevice = findPreference(KEY_VMA_DEREGISTER_DEVICE);
        manageLinkedDevices = findPreference(VMA_MANAGE_DEVICE);
        subscribeOrUnsubscribe = findPreference(KEY_VMA_DEPROVISION);
        syncOverWifiCheckBox = (CheckBoxPreference) findPreference(KEY_VMA_SYNC_OVER_WIFI);
        syncOverWifiCheckBox.setOnPreferenceClickListener(this);

    }

    private void setSignatureDisplay() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String summery = sharedPreferences.getString(AUTO_SIGNATURE,
                this.getString(R.string.pref_summary_manage_auto_signature));
        if (summery.length() > 0) {
            mAutoSignaturePref.setSummary(summery);
        } else {
            mAutoSignaturePref.setSummary(R.string.pref_summary_manage_auto_signature);
        }
    }

    private void initVMAView() {
        PreferenceScreen screen = (PreferenceScreen) findPreference("preference_screen");
        if (settings.getBooleanSetting(AppSettings.KEY_VMA_NOTVZWMDN)) {
            PreferenceCategory vmaFeaturesCategory = (PreferenceCategory) findPreference(KEY_VMA_FEATURES);

            if (vmaFeaturesCategory != null) {
                screen.removePreference(vmaFeaturesCategory);
            }
        } else {
            if (isTablet && !settings.isProvisioned()) {
                PreferenceCategory vmaFeaturesCategory = (PreferenceCategory) findPreference(KEY_VMA_FEATURES);
                if (vmaFeaturesCategory != null) {
                    screen.removePreference(vmaFeaturesCategory);
                }
                return;
            }
            if (isTablet) {
                PreferenceCategory vmaFeaturesCategory = (PreferenceCategory) findPreference(KEY_VMA_FEATURES);
                vmaFeaturesCategory.removePreference(subscribeOrUnsubscribe);
                vmaFeaturesCategory.removePreference(manageLinkedDevices);
                vmaFeaturesCategory.removePreference(syncOverWifiCheckBox);
                return;
            } else {
                PreferenceCategory vmaFeaturesCategory = (PreferenceCategory) findPreference(KEY_VMA_FEATURES);
                vmaFeaturesCategory.removePreference(pairedAccount);
                vmaFeaturesCategory.removePreference(deregisterDevice);
            }

            if (!settings.isProvisioned()) {
                PreferenceCategory vmaFeaturesCategory = (PreferenceCategory) findPreference(KEY_VMA_FEATURES);
                // vmaFeaturesCategory.removePreference(mForwardSetting);
                vmaFeaturesCategory.removePreference(autoReplySetting);
                vmaFeaturesCategory.removePreference(manageLinkedDevices);
                vmaFeaturesCategory.removePreference(syncOverWifiCheckBox);

            }
            if (settings.isSyncOverWifiEnabled() && !isTablet && syncOverWifiCheckBox != null) {
                syncOverWifiCheckBox.setChecked(true);
            } else {
                syncOverWifiCheckBox.setChecked(false);
            }
            if (subscribeOrUnsubscribe != null) {
                if (settings.getBooleanSetting(AppSettings.KEY_VMA_HANDSET_PROVISIONING_IN_BACKROUND, false)
                        && !settings.isProvisioned()) {
                    subscribeOrUnsubscribe.setEnabled(false);
                }
                if (settings.isProvisioned()) {
                    subscribeOrUnsubscribe.setTitle(R.string.vma_delete_account);
                    subscribeOrUnsubscribe.setSummary(R.string.vma_deprovision_summary);
                } else {
                    subscribeOrUnsubscribe.setTitle(R.string.vma_subscribe);
                    subscribeOrUnsubscribe.setSummary(R.string.vma_request_subscription);
                }
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_RESTORE_DEFAULTS:
            restoreDefaultPreferences();
            item.setTitle(getString(R.string.restore_default));
            return true;
        }
        return false;
    }

    private void showAddSignatureDialog(int title) {
        final Dialog d = new AppAlignedDialog(this, getString(R.string.pref_title_manage_auto_signature),
                getString(R.string.pref_summary_enable_auto_signature));
        final EditText input = (EditText) d.findViewById(R.id.edit_signature);
        final TextView counter = (TextView) d.findViewById(R.id.counterTextView);

        InputFilter maxLengthFilter = new InputFilter.LengthFilter(32);
        input.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setFilters(new InputFilter[] { maxLengthFilter });
        input.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (count <= 32) {
                    counter.setText((32 - input.getText().length()) + "/32");
                } else {
                    input.setText(s.toString().substring(0, 31));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Button button = (Button) d.findViewById(R.id.positive_button);
                String countCharacters = input.getText().toString();
                if (countCharacters.length() > 1) {
                    button.setEnabled(true);
                } else {

                    button.setEnabled(false);
                }

                for (int i = s.length(); i > 0; i--) {

                    if (s.subSequence(i - 1, i).toString().equals("\n"))
                        s.replace(i - 1, i, "");
                }
            }
        });

        input.setText(sharedPreferences.getString(AUTO_SIGNATURE, ""));

        Button setButton = (Button) d.findViewById(R.id.positive_button);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreferences.edit().putString(AUTO_SIGNATURE, input.getText().toString().trim())
                        .commit();
                setSignatureDisplay();
                d.dismiss();
            }
        });
        Button cancelButton = (Button) d.findViewById(R.id.negative_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                d.dismiss();
            }
        });
        d.show();
    }

    public static String getSignature(Context mContext, CharSequence text) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        if (sharedPreferences.getBoolean(AUTO_SIGNATURE_ENABLED, false)) {
            String signature = sharedPreferences.getString(AUTO_SIGNATURE, "");
            if (signature.length() > 0) {
                if (text.toString().endsWith("\n" + signature)) {
                    return text.toString();
                } else {
                    return text + "\n" + signature;
                }
            }

        }
        return text.toString();

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onPreferenceTreeClick() : key=" + preference.getKey() + ",screen"
                    + ((preferenceScreen != null) ? preferenceScreen.getKey() : null));
        }
        if (preference == mAdvanceSetting) {
            Intent advanceIntent = new Intent(this, AdvancePreferenceActivity.class);
            startActivity(advanceIntent);
        } else if (preference == mCustomizeConv) {
            Intent intent = new Intent(this, CustomizeConActivity.class);
            startActivity(intent);
        } else if (preference == mAutoSignaturePref) {
            showAddSignatureDialog(R.string.pref_title_manage_auto_signature);
        } else if (preference == manageLinkedDevices) {
            showManageDevice();
        } else if (KEY_VMA_DEREGISTER_DEVICE.equals(preference.getKey())) {

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "KEY_VMA_DEREGISTER_DEVICE is Called");
            }
            showDeregisteringDialog();
        } else if (KEY_VMA_DEPROVISION.equals(preference.getKey())) {
            if (settings.getBooleanSetting(AppSettings.KEY_VMA_NOTELIGIBLE)) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "The devices is not eligible for VMA service");
                }
                showMessage(getString(R.string.vma_server_error), getString(R.string.vma_account_noteligible));
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "if setting is Provisioned \t" + settings.isProvisioned());
                }
                if (settings.isProvisioned()) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "Device already registered. deprovisioning");
                    }
                    showDeProvisioningDialog();
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "Not registered yet closing activity.");
                    }
                    settings.put(AppSettings.KEY_VMA_SHOW_PROVISION_SERVICE, true);
                    Intent intent = null;
                    if (isTablet) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(),
                                    "starting Provisioning activity from MessagingPreference");
                        }
                        intent = new Intent(context, Provisioning.class);
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(),
                                    "starting ConversationList activity from MessagingPreference");
                        }
                        intent = new Intent(context, ConversationListActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            }
        }
        /*
         * else if(preference == mForwardSetting){ startReplyForwardActivity(ReplyForwardActivity.FWD_INTENT);
         * }
         */
        else if (preference == autoReplySetting) {
            startReplyForwardActivity(ReplyForwardActivity.REPLY_INTENT);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void startReplyForwardActivity(String extra) {
        Intent intent = new Intent(context, ReplyForwardActivity.class);
        intent.putExtra(extra, true);
        startActivity(intent);
    }

    private void showManageDevice() {
        startActivity(new Intent(context, ManageDeviceList.class));
    }

    private void showDeregisteringDialog() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("showDeregisteringDialog()");
        }
        final AppAlignedDialog dialog = new AppAlignedDialog(context, R.drawable.dialog_alert,
                R.string.vma_disconnect_tablet, R.string.vma_disconnect_confirmation);
        Button okButton = (Button) dialog.findViewById(R.id.positive_button);
        okButton.setText(R.string.vma_disconnect_ok_button_label);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("User accepted to deregister");
                }
                VmaDeregisterTask deregister = new VmaDeregisterTask();
                deregister.execute();
                dialog.dismiss();
            }
        });
        Button cancelButton = (Button) dialog.findViewById(R.id.negative_button);
        cancelButton.setText(R.string.cancel);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void showDeProvisioningDialog() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("showDeProvisioningDialog()");
        }
        final AppAlignedDialog dialog = new AppAlignedDialog(context, R.drawable.dialog_alert,
                R.string.vma_unsubscribe_handset, R.string.vma_deprovisioning_confirmation);
        Button okButton = (Button) dialog.findViewById(R.id.positive_button);
        okButton.setText(R.string.vma_unsubscribe_handset);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("User accepted to unsubcribe");
                }
                VmaUnsubscribeTask unsubscribe = new VmaUnsubscribeTask();
                unsubscribe.execute();
                dialog.dismiss();
            }
        });
        Button cancelButton = (Button) dialog.findViewById(R.id.negative_button);
        cancelButton.setText(R.string.no);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private class VmaDeregisterTask extends AsyncTask<Void, String, Integer> {
        private ProgressDialog dialog;

        public VmaDeregisterTask() {
        }

        protected void onPreExecute() {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "preparing to delete the vma account");
            }
            dialog = ProgressDialog.show(context, null, getString(R.string.vma_disconnect_tablet_msg));
        }

        @Override
        protected Integer doInBackground(Void... params) {
            ProvisionManager pMgr = new ProvisionManager(context);
            String deviceId = settings.getStringSettings(AppSettings.KEY_DEVICE_ID);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "pref deviceId:" + deviceId);
            }

            int result = pMgr.deleteLinkedDevice(deviceId);
            if (result == AppErrorCodes.VMA_PROVISION_OK) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "resetTablet():disconnect");
                }
                settings.resetTablet();
            }
            return result;
        };

        protected void onPostExecute(Integer result) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "result Code:" + result.intValue());
            }
            if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                settings.put(ApplicationSettings.KEY_VMA_PROVISIONED, String.valueOf(0));
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "calling VZActivityHelper closePausedActivityHelper....");
                }
                VZActivityHelper.closePausedActivityOnStack();
                settings.put(AppSettings.KEY_VMA_ACCEPT_TERMS, false);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "starting Provisioning activity from VmaDeregisterTask");
                }
                Intent intent = new Intent(context, Provisioning.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                dialog.dismiss();
                finish();
            } else {
                ErrorHandler handler = new ErrorHandler(MessagingPreferenceActivity.this);
                handler.showAlertDialog(result.intValue());
                dialog.dismiss();
            }
        }
    }

    private class VmaUnsubscribeTask extends AsyncTask<Void, String, Integer> {
        private ProgressDialog dialog;

        public VmaUnsubscribeTask() {
        }

        protected void onPreExecute() {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "preparing to delete the vma account");
            }
            dialog = ProgressDialog.show(context, null, getString(R.string.vma_deactive));
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "deleting vma account");
            }
            int result = provMng.deleteVMAAccount();
            return result;
        };

        protected void onPostExecute(Integer result) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "result of deprovising" + result);
            }
            if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                settings.put(AppSettings.KEY_VMA_DONT_SHOW_DIALOG, true);
                showMessage(getString(R.string.vma_server_error), getString(R.string.vma_account_deactivated));
                // initialize the value again as we will reset setting
                settings.put(AppSettings.KEY_VMA_ACCEPT_TERMS, true);
                PreferenceCategory vmaFeaturesCategory = (PreferenceCategory) findPreference(KEY_VMA_FEATURES);
                // vmaFeaturesCategory.removePreference(mForwardSetting);
                if (isTablet) {
                    vmaFeaturesCategory.removePreference(pairedAccount);
                }
                vmaFeaturesCategory.removePreference(autoReplySetting);
                vmaFeaturesCategory.removePreference(manageLinkedDevices);
                vmaFeaturesCategory.removePreference(syncOverWifiCheckBox);
                subscribeOrUnsubscribe.setTitle(R.string.vma_subscribe);
                subscribeOrUnsubscribe.setSummary(R.string.vma_request_subscription);
            } else {
                // handleError(result);
                ErrorHandler handler = new ErrorHandler(MessagingPreferenceActivity.this);
                handler.showAlertDialog(result.intValue());
            }
            dialog.dismiss();
        }

    }

    private void restoreDefaultPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit();
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.preferences);
        initViews();
        initVMAView();
    }

    public static void setLocale(Context context) {
        // make sure the handset locale is saved correctly
        // initLocale(context);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(MessagingPreferenceActivity.class, "setLocale():");
        }

        Locale locale = getLocale(context);
        if (locale != null) {
            Locale.setDefault(locale);

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(MessagingPreferenceActivity.class, "setLocale(): locale=" + locale.getLanguage()
                        + " ,System Locale=" + Locale.getDefault().getDisplayName());
            }
            Configuration config = new Configuration();
            config.locale = locale;
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
            MessageUtils.init(context);
        }
    }

    // public static void initLocale(Context context) {
    // SharedPreferences myPreference = PreferenceManager.getDefaultSharedPreferences(context);
    // String langcode = myPreference.getString(LANGUAGE_CODE, "en");
    //
    // // if the user has not set the language in the app, save the handset language in
    // // a preference. Then when "Restore Default settings" is called, we can restore the language
    // // to the handset location.
    // //
    // if (langcode.equals("")) {
    // // if the user has not set the language, get the default handset locale and save it
    // Locale locale = Locale.getDefault();
    //
    // SharedPreferences.Editor editPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    // .edit();
    // editPrefs.putString(HANDSET_LANGUAGE_CODE, locale.getLanguage());
    // editPrefs.commit();
    // }
    //
    // }

    private String setDefaultLanguageSummery(Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("setDefaultLanguageSummery():");
        }
        SharedPreferences myPreference = PreferenceManager.getDefaultSharedPreferences(context);
        String langcode = myPreference.getString(LANGUAGE_CODE, null);

        if ("default".equals(langcode) || TextUtils.isEmpty(langcode)) {
            langcode = Locale.getDefault().getLanguage();
        }
        String[] langcode_array = context.getResources().getStringArray(R.array.language_codes);
        for (int i = 0; i < langcode_array.length; i++) {
            if (langcode.equals(langcode_array[i])) {
                return context.getResources().getStringArray(R.array.language_names)[i];
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("setDefaultLanguageSummery(): return system default locale=" + langcode);
        }
        return langcode;
    }

    private static Locale getLocale(Context context) {
        SharedPreferences myPreference = PreferenceManager.getDefaultSharedPreferences(context);
        String langcode = myPreference.getString(LANGUAGE_CODE, null);

        // user has not set any preference.. just default to the language of the handset
        if ("default".equals(langcode) || TextUtils.isEmpty(langcode)) {
            langcode = Locale.getDefault().getLanguage();
        }
        boolean isSupported = false;
        String[] langcode_array = context.getResources().getStringArray(R.array.language_codes);
        for (int i = 0; i < langcode_array.length; i++) {
            if (langcode.equals(langcode_array[i])) {
                isSupported = true;
                break;
            }
        }

        if (!isSupported) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("getLocale(): langcode is not supported. setting eng as locale.");
            }
            langcode = "en";
        }

        Locale locale = new Locale(langcode);
        return locale;
    }

    //
    // always return a locale - never null; this is used when formatting dates/times
    //
    public static Locale getCurrentLocale(Context context) {
        Locale locale = getLocale(context);
        if (locale == null) {
            return Locale.getDefault();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("getCurrentLocale(): locale=" + locale);
        }
        return locale;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see
     * android.preference.Preference.OnPreferenceClickListener#onPreferenceClick(android.preference.Preference
     * )
     */

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference.getKey().equals(KEY_ENABLE_WIFI_SYNC)) {
            // if (wifiSyncCheckBox.isChecked()) {
            //
            // editPrefs.putBoolean(SyncManager.WIFI_SYNC_ENABLED, true);
            // editPrefs.commit();
            // wifiSyncCheckBox.setChecked(true);
            // //TODO :Add intent to restart the stopped service (Wifi-Sync)
            // } else {
            // //editPrefs.putBoolean(SyncManager.WIFI_SYNC_ENABLED, false);
            // //editPrefs.commit();
            // wifiSyncCheckBox.setChecked(false);
            // //TODO : Add stopservice intent (Wifi-Sync
            // }
        }
        if (preference.getKey().equals(KEY_VMA_SYNC_OVER_WIFI)) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Sync OverWifi options changed: checked=" + syncOverWifiCheckBox.isChecked());
            }
            if (syncOverWifiCheckBox.isChecked()) {
                settings.put(KEY_VMA_SYNC_OVER_WIFI, true);
                syncOverWifiCheckBox.setChecked(true);
            } else {
                settings.put(KEY_VMA_SYNC_OVER_WIFI, false);
                syncOverWifiCheckBox.setChecked(false);
            }
            context.sendBroadcast(new Intent(SyncManager.ACTION_UPDATE_SETTINGS));
        }
        return true;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.preference.PreferenceActivity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("onActivityResult:requestCode=" + requestCode + ",aresultCode=" + resultCode
                    + ",data=" + data);
        }

    }

    /**
     * This Method show single button Alert dialog
     * 
     * @param title
     * @param message
     */
    public void showMessage(String title, String message) {

        final Dialog d = new AppAlignedDialog(this, R.drawable.launcher_home_icon, title, message);
        d.setCancelable(false);
        Button cancelButton = (Button) d.findViewById(R.id.positive_button);
        cancelButton.setText(R.string.ok);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settings.put(AppSettings.KEY_VMA_DONT_SHOW_DIALOG, false);
                d.dismiss();
            }
        });
        d.show();
    }

    @Override
    public void onBackPressed() {
        if (mAppFontChanged) {
            Intent intent = new Intent(this, ConversationListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(EXTRA_FONT_CHANGED, true);
            startActivity(intent);
            mAppFontChanged = false;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mfontSupportPref) {
            editPrefs.putString(APP_FONT_SUPPORT, newValue.toString()).commit();
            mAppFontChanged = true;
            // set the result code so that if this was started from
            setResult(RESULT_OK);
            // reset the theme
            MessageUtils.resetTheme();
            mfontSupportPref.setValue((String) newValue);
            mfontSupportPref.setSummary(mfontSupportPref.getEntry());
            return true;
        } else if (preference == mRingtonePref) {
            mRingtonePref.setSummary(getRingtone((String) newValue));
            return true;
        } else if (preference == mVibrateWhenPref) {
            mVibrateWhenPref.setValue((String) newValue);
            mVibrateWhenPref.setSummary(mVibrateWhenPref.getEntry());
            return true;
        } else if (preference == mAutoSignatureEnable) {
            editPrefs.putBoolean(AUTO_SIGNATURE_ENABLED, (Boolean) newValue).commit();
            return true;
        }
        return false;
    }

    private class SyncStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(SyncManager.EXTRA_STATUS, 0);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this.getClass(), "onReceiver received status in MessagingPreferencesActivity: "
                        + status);
            }
            switch (status) {
            case SyncManager.SYNC_PROVISIONING_COMPLETED:
                if (isTablet) {
                    break;
                }
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(this.getClass(),
                            "refreshing the preferences view as provisioning status has been changed");
                }
                restoreDefaultPreferences();
                if (subscribeOrUnsubscribe != null)
                    subscribeOrUnsubscribe.setEnabled(true);

                break;
            case SyncManager.SYNC_STATUS_PROVISIONING_RESULT:
                if (subscribeOrUnsubscribe != null)
                    subscribeOrUnsubscribe.setEnabled(true);

                break;
            default:
                break;
            }

        }
    }
}
