/**
 * AlarmReceiver.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.report;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class/interface
 * 
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * @Since July 4, 2012
 */
public class ReportAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO - Animesh
		context.startService(new Intent(ReportService.class.getName()));
	}
}