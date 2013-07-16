package com.verizon.mms.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.Button;

import com.rocketmobile.asimov.Asimov;
import com.verizon.messaging.vzmsgs.R;

public class CustomizeConActivity extends VZMPreferenceActivity {
	
	Preference mResetDefault;
	Preference mCustomizeBgCol;
	Preference mCustomizeBubble;
	
	public static final String DEFAULT_KEY = "pref_key_reset_default";
	public static final String CUSTOMIZE_BG="pref_key_customize_bg_colr";
	public static final String CUSTOMIZE_BUBBLE = "pref_key_customize_bubble_color";
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, true);        
        
        ConversationResHelper.init(this);
        addPreferencesFromResource(R.xml.cutomize_conv_pref);
        initView();
    }
    
    private void initView(){
    	mResetDefault = findPreference(DEFAULT_KEY);
    	mCustomizeBgCol = findPreference(CUSTOMIZE_BG);
    	mCustomizeBubble  = findPreference(CUSTOMIZE_BUBBLE);
    }
	
    public  void confirmResetDialog () {
        final Dialog d = new AppAlignedDialog(CustomizeConActivity.this,
				R.drawable.dialog_alert, 
				R.string.reset_to_default, 
				R.string.reset_to_default_confirmation_dialog);
        
		Button deleteButton = (Button) d.findViewById(R.id.positive_button);
		deleteButton.setText(R.string.yes);
		deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ConversationResHelper.resetToDefault(CustomizeConActivity.this);
				d.dismiss();
			}
		});
		Button noButton = (Button) d.findViewById(R.id.negative_button);
		noButton.setVisibility(View.VISIBLE);
		noButton.setText(R.string.no);
		noButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
			}
		});
		d.show(); 
    }

	@Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    	if (preference == mResetDefault){
    		confirmResetDialog();
        } 
    	else if(preference == mCustomizeBgCol){
    		Intent intent = new Intent(CustomizeConActivity.this, CustomizeBackgroundActivity.class);
			startActivity(intent);
    	}
    	else if(preference == mCustomizeBubble){
    		Intent i = new Intent(CustomizeConActivity.this, CustomizeBubblesActivity.class);
    		startActivity(i);
    	}
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
