package com.verizon.mms.util;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

import com.rocketmobile.asimov.Asimov;


public class Prefs {
	private static SharedPreferences prefs;

	private static final String SHOW_PREFIX = "show-";


	public static SharedPreferences getPrefs() {
		if (prefs == null) {
			prefs = PreferenceManager.getDefaultSharedPreferences(Asimov.getApplication());
		}
		return prefs;
	}

	public static Editor getPrefsEditor() {
		return getPrefs().edit();
	}

	public static void registerPrefsListener(OnSharedPreferenceChangeListener listener) {
		getPrefs().registerOnSharedPreferenceChangeListener(listener);
	}

	public static void unregisterPrefsListener(OnSharedPreferenceChangeListener listener) {
		getPrefs().unregisterOnSharedPreferenceChangeListener(listener);
	}

	public static void setString(String key, String val) {
		final Editor editor = getPrefsEditor();
		editor.putString(key, val);
		editor.commit();
	}

	public static void setFloat(String key, float val) {
		final Editor editor = getPrefsEditor();
		editor.putFloat(key, val);
		editor.commit();
	}
	
	public static void setInt(String key, int val) {
		final Editor editor = getPrefsEditor();
		editor.putInt(key, val);
		editor.commit();
	}

	public static void setLong(String key, long val) {
		final Editor editor = getPrefsEditor();
		editor.putLong(key, val);
		editor.commit();
	}

	public static void setBoolean(String key, boolean val) {
		final Editor editor = getPrefsEditor();
		editor.putBoolean(key, val);
		editor.commit();
	}

	public static void remove(String key) {
		final Editor editor = getPrefsEditor();
		editor.remove(key);
		editor.commit();
	}

	public static String getString(String key, String def) {
		return getPrefs().getString(key, def);
	}

	public static int getInt(String key, int def) {
		return getPrefs().getInt(key, def);
	}

	public static long getLong(String key, long def) {
		return getPrefs().getLong(key, def);
	}

	public static float getFloat(String key, float def) {
		return getPrefs().getFloat(key, def);
	}

	public static boolean getBoolean(String key, boolean def) {
		return getPrefs().getBoolean(key, def);
	}

	public static boolean contains(String key) {
		return getPrefs().contains(key);
	}

	/**
	 * Sets a boolean preference for the given preference key that indicates whether to prompt again for it.
	 * Use shouldShow() to query it.
	 * @param key
	 * @param show True if we should prompt the user again for the key
	 */
	public static void setShow(String key, boolean show) {
		setBoolean(SHOW_PREFIX + key, show);
	}

	/**
	 * Checks whether we should prompt the user for the given preference key again.
	 * @param key
	 * @return True if we should prompt the user for the key
	 */
	public static boolean shouldShow(String key) {
		return getBoolean(SHOW_PREFIX + key, true);
	}
}
