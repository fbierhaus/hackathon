/**
 * AdvancePreferenceActivity.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.rocketmobile.asimov.Asimov;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.util.Recycler;
import com.verizon.mms.util.RecyclerScheduleReceiver;

public class AdvancePreferenceActivity extends VZMPreferenceActivity implements OnPreferenceClickListener{

    // Menu entries
    private static final int   MENU_RESTORE_DEFAULTS               = 1;
    // Symbolic names for the keys used for preference lookup
    public static final String WIFI_PROBE                             = "pref_key_wifi_probe";
    public static final String CONV_DELETE_LIMIT                      = "pref_key_all_conv_delete_limit";
    public static final String MMS_SETTINGS                           = "pref_key_mms_settings";
    public static final String STORAGE_SETTINGS                       = "pref_key_storage_settings";
    public static final String DELIVERY_REPORT_MODE                   = "pref_key_delivery_reports";
    public static final String AUTO_RETRIEVAL                         = "pref_key_mms_auto_retrieval";
    public static final String RETRIEVAL_DURING_ROAMING               = "pref_key_mms_retrieval_during_roaming";
    public static final String NOTIFICATION_DISABLE_DURING_PHONE_CALL = "pref_key_notifications_during_phone_call";
    public static final String WEBLINK_PREVIEW						  = "pref_key_weblick_preview";
    	
    // default values--must match the defaults in preferences xml file(s)
    public static final boolean DELIVERY_REPORT_MODE_DEFAULT          = true;
    public static final boolean WEBLINK_PREVIEW_DEFAULT               = true;

    private Preference         mMsgLimitPref;
    private CheckBoxPreference mWifiProbe;// deliveryReport;//, readReportAutoResponse;//readReport
    private Recycler           mMessageRecycler;
   // ListPreference listPref ;
    AlertDialog.Builder builder = null;
    AlertDialog alert;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.advancepreferences);
        initView();
        
        if (!MmsConfig.getMmsEnabled()) {
            // No Mms, remove all the mms-related preferences
            PreferenceCategory mmsOptions = (PreferenceCategory) findPreference(MMS_SETTINGS);
            getPreferenceScreen().removePreference(mmsOptions);

            PreferenceCategory storageOptions = (PreferenceCategory) findPreference(STORAGE_SETTINGS);
            storageOptions.removePreference(findPreference(CONV_DELETE_LIMIT));
        }
        
        final CheckBoxPreference prefs = (CheckBoxPreference)findPreference("pref_key_auto_delete");
        prefs.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean oldValue = prefs.isChecked();
				boolean isChecked = (Boolean)newValue;
				
				if (oldValue != isChecked) {
					if (isChecked) {
						Context context = Asimov.getApplication();
						RecyclerScheduleReceiver.scheduleRecycler(context, 
								SystemClock.elapsedRealtime() + 25000
								, MmsConfig.getRecyclerInterval());
						
						
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AdvancePreferenceActivity.this);
						Editor editor = prefs.edit();
						editor.putLong(RecyclerScheduleReceiver.PREV_RECYCLE_TIME, 0);
						editor.commit();
					} else {
						//RecyclerScheduleReceiver.stopRecycler(AdvancePreferenceActivity.this);
					}
				}
				prefs.setChecked(isChecked);
				return true;
			}
		});
        
        // Hide the menus on tablet 
        if (MmsConfig.isTabletDevice()) {
            ((PreferenceGroup) findPreference("pref_key_application_settings"))
                    .removePreference(findPreference("pref_key_delivery_reports"));
            ((PreferenceGroup) findPreference("pref_key_application_settings"))
                    .removePreference(findPreference("pref_key_notifications_during_phone_call"));
            ((PreferenceScreen) findPreference("pref_screen_settings"))
                    .removePreference(findPreference("pref_key_mms_settings"));
            ((PreferenceGroup) findPreference("pref_key_application_settings"))
                .removePreference(findPreference("pref_key_auto_delete"));
            ((PreferenceGroup) findPreference("pref_key_application_settings"))
                .removePreference(findPreference("pref_key_all_conv_delete_limit"));

        }
    }

    
   /* @Override
    protected void onStart() {
    	Asimov.activityStarted();
    	super.onStart();
    }
    
    @Override
    protected void onStop() {
    	Asimov.activityStoped();
    	super.onStop();
    }*/
    
    private void initView() {
    	 setTitle(R.string.advance_preferences_title);
    	 mMsgLimitPref = findPreference(CONV_DELETE_LIMIT);
         // Wifi Sync Probe for Location Setting
         mWifiProbe      = (CheckBoxPreference) findPreference(WIFI_PROBE);
         mWifiProbe.setOnPreferenceClickListener(this);
         // ReadReport 3.1
   //      deliveryReport = (CheckBoxPreference) findPreference("pref_key_delivery_reports");
  //       deliveryReport.setOnPreferenceClickListener(this);
//         readReport =  (CheckBoxPreference) findPreference(MMSReadReportPref.MMS_READ_REPORT);
//         readReport.setOnPreferenceClickListener(this);
         
        // if(readReport.isChecked()) {
     	//	deliveryReport.setChecked(true);
      //   }
         
         SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
//       readReportAutoResponse =(CheckBoxPreference) findPreference(MMSReadReportPref.MMS_READ_REPORT_AUTO_RESPOND);
//         readReportAutoResponse.setOnPreferenceClickListener(this);
//         if(readReportAutoResponse.isChecked()) {
//	         if(pref.getInt(MMSReadReportPref.MMS_READ_REPORT_OPTIONS,MMSReadReportPref.ALWAYS_RESPOND) == MMSReadReportPref.ALWAYS_RESPOND) {
//	        	 readReportAutoResponse.setSummary(R.string.pref_summary_readreport_always);
//	         } else {
//	        	 readReportAutoResponse.setSummary(R.string.pref_summary_readreport_never);
//	         }
//         } else {
//        	 readReportAutoResponse.setSummary(R.string.pref_summary_readreport_disabled);
//         }
                    
                 
         if (mWifiProbe.isChecked()) {
             mWifiProbe.setSummary(getString(R.string.wifi_probe_on));
         } else {
             mWifiProbe.setSummary(getString(R.string.wifi_probe_off));
         }
         
         mMessageRecycler = Recycler.getMessageRecycler();
         // Fix up the recycler's summary with the correct values
         if(!MmsConfig.isTabletDevice()){
        	 setMsgDisplayLimit();
         }

	}

	private void setMsgDisplayLimit() {
        mMsgLimitPref.setSummary(getString(R.string.pref_summary_delete_limit,
                mMessageRecycler.getMessageLimit(this)));
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

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mMsgLimitPref) {
            new NumberPickerDialog(this, mMsgLimitListener, mMessageRecycler.getMessageLimit(this),
                    mMessageRecycler.getMessageMinLimit(), mMessageRecycler.getMessageMaxLimit(),
                    R.string.pref_title_all_conv_delete).show();
        } 
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void restoreDefaultPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit();
        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.advancepreferences);
        initView();
    }

    NumberPickerDialog.OnNumberSetListener mMsgLimitListener = new NumberPickerDialog.OnNumberSetListener() {
                                                                 public void onNumberSet(int limit) {
                                                                     mMessageRecycler
                                                                             .setMessageLimit(
                                                                                     AdvancePreferenceActivity.this,
                                                                                     limit);
                                                                     setMsgDisplayLimit();
                                                                 }
                                                             };
    @Override
    public boolean onPreferenceClick(Preference preference) {

        final SharedPreferences.Editor editPrefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        // ReadReport 3.1
//        if(preference.getKey().equals(MMSReadReportPref.MMS_READ_REPORT)) {
//        	if(readReport.isChecked()) {
//           			editPrefs.putBoolean("pref_key_delivery_reports", true);
//               	    editPrefs.commit();
//        			deliveryReport.setChecked(true);
//           	}
//	       return true;
//        } else if(preference.getKey().equals("pref_key_delivery_reports")) {
//        	if(!deliveryReport.isChecked()){
//        		editPrefs.putBoolean(MMSReadReportPref.MMS_READ_REPORT, false);
//        		editPrefs.commit();
//        		readReport.setChecked(false);
//           	    
//        	}
//        	return true;
//        } else if(preference.getKey().equals(MMSReadReportPref.MMS_READ_REPORT_AUTO_RESPOND)) {
//        	if(readReportAutoResponse.isChecked()) {
//        		editPrefs.putBoolean(MMSReadReportPref.FIRST_READ_REPORT_LAUNCH, false);
//           	    editPrefs.commit();
//           	    autoReadReportDialog().show();
//        	} else {
//        		readReportAutoResponse.setSummary(R.string.pref_summary_readreport_disabled);
//        	}
//        	return true;
//        }
        if (mWifiProbe.isChecked()) {
        	final Dialog d = new AppAlignedDialog(AdvancePreferenceActivity.this,
    				R.drawable.dialog_alert, 
    				R.string.wifi_probe_title, 
    				R.string.wifi_probe_dialog);
    		Button positiveButton = (Button) d.findViewById(R.id.positive_button);
    		positiveButton.setText(R.string.wifi_probe_accept);
    		positiveButton.setOnClickListener(new View.OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				 editPrefs.putBoolean(WIFI_PROBE, true);
                     editPrefs.commit();
                     mWifiProbe.setSummary(getString(R.string.wifi_probe_on));
                     d.dismiss();
    			}
    		});
    		
    		Button negativeButton = (Button) d.findViewById(R.id.negative_button);
    		negativeButton.setVisibility(View.VISIBLE);
    		negativeButton.setText(R.string.wifi_probe_decline);
    		negativeButton.setOnClickListener(new View.OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				editPrefs.putBoolean(WIFI_PROBE, false);
                    editPrefs.commit();
                    mWifiProbe.setSummary(R.string.wifi_probe_off);
                    mWifiProbe.setChecked(false);
    				d.dismiss();
    			}
    		});
    		d.show(); 
        } else {
            editPrefs.putBoolean(WIFI_PROBE, false);
            editPrefs.commit();
            mWifiProbe.setSummary(getString(R.string.wifi_probe_off));
        }
        
        return true;
    }
 // ReadReport 3.1
//    AlertDialog autoReadReportDialog() {
//    	SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
//    	 final SharedPreferences.Editor editPref =pref.edit();
//        
//         builder = new AlertDialog.Builder(this);
//         builder.setTitle("Auto-respond");
//         builder.setCancelable(false);
//         builder.setSingleChoiceItems(MMSReadReportPref.AUTO_RESPOND_OPTIONS, 0, new DialogInterface.OnClickListener(){
//             public void onClick(DialogInterface dialogInterface, int option) {
//            	 editPref.putInt(MMSReadReportPref.MMS_READ_REPORT_OPTIONS, option);
//            	 if(option == MMSReadReportPref.ALWAYS_RESPOND)
//            		 readReportAutoResponse.setSummary(R.string.pref_summary_readreport_always);
//            	 else
//            		 readReportAutoResponse.setSummary(R.string.pref_summary_readreport_never);
//            	 editPref.commit();
//            	 alert.dismiss();
//             }
//         });
//         alert = builder.create();
//         return alert;
//    }
    
    
    
}

