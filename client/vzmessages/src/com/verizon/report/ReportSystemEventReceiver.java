package com.verizon.report;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.internal.telephony.Phone;
import com.verizon.internal.telephony.TelephonyIntents;
import com.verizon.mms.util.Util;

public class ReportSystemEventReceiver extends BroadcastReceiver {
	private static ReportSystemEventReceiver systemEventReceiver;

	private static void wakeUpService(Context context) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(ReportSystemEventReceiver.class,
					"==>> wakeUpService: start report service ...");
		}
		context.startService(new Intent(context, ReportService.class));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.debug(ReportService.class,
				"!*!=================================");
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(),
					"==>> received intent: " + Util.dumpIntent(intent, "    "));
		}

		if (intent.getAction().equals(
				TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
			String state = intent.getStringExtra(Phone.STATE_KEY);

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "==>> ANY_DATA_STATE event received: "
						+ state);
			}

			if (state.equals("CONNECTED")) {
				wakeUpService(context);
			}
		}
	}

	public static void registerForConnectionStateChanges(Context context) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(ReportSystemEventReceiver.class,
					"==> Registering for system network changes");
		}

		// Unregister
		if (systemEventReceiver != null) {
			try {
				context.unregisterReceiver(systemEventReceiver);
			} catch (IllegalArgumentException e) {
				// Allow un-matched register-unregister calls
			}
		}

		// Register
		IntentFilter filter = new IntentFilter(
				TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
		if (systemEventReceiver == null) {
			systemEventReceiver = new ReportSystemEventReceiver();
		}
		context.registerReceiver(systemEventReceiver, filter);
	}

	public static void unRegisterForConnectionStateChanges(Context context) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(ReportSystemEventReceiver.class,
					"==> Unregistering for system network changes");
		}

		if (systemEventReceiver != null) {
			try {
				context.unregisterReceiver(systemEventReceiver);
			} catch (IllegalArgumentException e) {
				// Allow un-matched register-unregister calls
			}
		}
	}
}
