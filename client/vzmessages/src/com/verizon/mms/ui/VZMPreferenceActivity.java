package com.verizon.mms.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.verizon.mms.ui.VZActivityHelper.ActivityState;

public class VZMPreferenceActivity extends PreferenceActivity  implements ActivityState {
	int state = ActivityState.ACTIVITY_STATE_NONE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		onCreate(savedInstanceState, false);
	}
	
	protected void onCreate(Bundle savedInstanceState, boolean setTheme) {
		super.onCreate(savedInstanceState);
		
		if (setTheme) {
			MessageUtils.setTheme(this);
		}
		
		VZActivityHelper.activityCreated(this);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		state = ActivityState.ACTIVITY_START;
		VZActivityHelper.activityStarted(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		state = ActivityState.ACTIVITY_STOP;
		VZActivityHelper.activityStoped(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		state = ActivityState.ACTIVITY_RESUMED;
		VZActivityHelper.activityStarted(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		state = ActivityState.ACTIVITY_PAUSED;
		VZActivityHelper.activityStoped(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		VZActivityHelper.activityDestroyed(this);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			VZActivityHelper.activityStarted(this);
		}
	}

	@Override
	public int getActivityState() {
		return state;
	}
}
