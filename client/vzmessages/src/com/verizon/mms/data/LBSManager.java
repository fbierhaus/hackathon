package com.verizon.mms.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nbi.common.NBIContext;
import com.nbi.location.LocationConfig;
import com.nbi.location.LocationListener;
import com.nbi.location.LocationProvider;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ui.AdvancePreferenceActivity;

public class LBSManager {
	private static NBIContext nbiContext;
	private LocationProvider locationProvider = null;

	public static final String API_KEY = "6VAQTsY6v7YVJ0EX7nT7GTABvedpYAC2Avvip9M4";


	public LBSManager(Context context) {
		try {
			if (nbiContext == null) {
				nbiContext = new NBIContext(context, API_KEY, null);
			}
            LocationConfig config = LocationConfig.createLocationConfig();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean(AdvancePreferenceActivity.WIFI_PROBE, false) == true) {
            	// IMPORTANT!!!!!
                // setting this parameter will enable wi-fi probe collection
                config.setCollectWiFiProbes(true);
            }
            locationProvider = LocationProvider.getInstance(nbiContext, config);	

		} catch (Exception e) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.error(getClass(), e);
			}
		}
	}

	public NBIContext getNBIContext() {
		return nbiContext;
	}

	public LocationProvider getLocationManager() {
		return locationProvider;
	}

	public void destroy(LocationListener listener) {
		try {
			if (locationProvider != null) {
				locationProvider.cancelGetLocation(listener);
				locationProvider.stopReceivingFixes(listener);
				locationProvider.onDestroy();
				locationProvider = null;
			}
			if (nbiContext != null) {
				nbiContext.clearCache();
				nbiContext.destroy();
				nbiContext = null;
			}
		}
		catch (Exception e) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.error(getClass(), e);
			}
		}
	}
}