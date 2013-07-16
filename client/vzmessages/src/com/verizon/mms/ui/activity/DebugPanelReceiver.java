/**
 * DebugPanelReceiver.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.strumsoft.android.commons.logger.Logger;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jun 6, 2012
 */
public class DebugPanelReceiver extends BroadcastReceiver {
    private static final String DEBUG_CODE = "#33284896#";

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(DebugPanelReceiver.class, "Debug panel activity ");
        }
        String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        if (DEBUG_CODE.equalsIgnoreCase(phoneNumber)) {
            Intent rintent = new Intent(context, DebugPanelActivity.class);
            rintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(rintent);
            setResultData(null);
        }

    }

}
