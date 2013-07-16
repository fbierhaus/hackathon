/**
 * CustomListPreference.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.widget;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.verizon.mms.data.Contact;
import com.verizon.mms.ui.MessagingPreferenceActivity;
/*
 * LexaSG:Change for localization, customized list preference for language selection. 
 */
public class LanguageListPreference extends ListPreference
 {
    Context mContext;
    String mClickedDialogEntry; 
    public LanguageListPreference(Context context, AttributeSet attrs)
     {
        super(context, attrs);
        this.mContext=context;
        
     }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) 
     {
        super.onPrepareDialogBuilder(builder);
        SharedPreferences myPreference=PreferenceManager.getDefaultSharedPreferences(mContext);
        String langcode = myPreference.getString(MessagingPreferenceActivity.LANGUAGE_CODE, mContext.getResources().getConfiguration().locale.getLanguage());
        CharSequence[] values=getEntryValues();
        int selectedentry=0;
        for(;selectedentry<values.length;selectedentry++)
            if(langcode.equals(values[selectedentry]))
                break;
        
        mClickedDialogEntry=langcode; 
        builder.setSingleChoiceItems(getEntries(),selectedentry,new OnClickListener()
          {
            @Override
            public void onClick(DialogInterface dialog, int which) 
              {
                // TODO Auto-generated method stub
                CharSequence[] values=getEntryValues();
                mClickedDialogEntry=(String) values[which];
               }
           });
        
		builder.setPositiveButton(mContext.getString(android.R.string.yes),
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences myPreference = PreferenceManager
								.getDefaultSharedPreferences(mContext);
						SharedPreferences.Editor editor = myPreference.edit();
						editor.putString(MessagingPreferenceActivity.LANGUAGE_CODE,mClickedDialogEntry);
						editor.commit();
						// to retain the application setting window
						Intent intent = new Intent(getContext(), MessagingPreferenceActivity.class);
						mContext.startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
						MessagingPreferenceActivity.setLocale(mContext);
						Contact.onAppLocaleChanged(mContext);
					}

				});
        
        builder.setNegativeButton(mContext.getString(android.R.string.no), null);
     }
    
 }

