/**
 * SystemEventReceiver.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.vzmsgs.receiver;

import java.util.Locale;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.mms.ui.MessagingPreferenceActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class/interface
 * 
 * @author jegadeesan
 * @Since Jun 27, 2013
 */
public class SystemEventReceiver extends BroadcastReceiver {

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onReceive() intent=" + Logger.dumpIntent(intent, null));
        }
        String action = intent.getAction();

        if (Intent.ACTION_LOCALE_CHANGED.equalsIgnoreCase(action)) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Locale changed Action=" + action + ", locale="
                        + Locale.getDefault().getDisplayLanguage());
            }
           // ApplicationSettings.getInstance(context).setLocaleChange(Locale.getDefault());
            MessagingPreferenceActivity.setLocale(context);

        }

    }

}
