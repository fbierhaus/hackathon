/**
 * AdressListTabActivity.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui;

import android.app.TabActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmobile.asimov.Asimov;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.VZActivityHelper.ActivityState;

/**
 * This class/interface
 * 
 * @author Imthiaz
 * @Since Apr 11, 2012
 */
public class PlacesTabActivity extends TabActivity implements OnTouchListener, ActivityState {
	int state = ActivityState.ACTIVITY_STATE_NONE;

	private View recent;
	private View favorite;
	private ImageView recentWhiteBar;
	private ImageView favoriteWhiteBar;
    private TabHost         tabHost;
    // URIs
    public static final Uri CACHE_URI = Uri.parse("content://address-cache/address");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VZActivityHelper.activityCreated(this);
        
        tabHost = getTabHost();
        int tabIndexValue = getIntent().getIntExtra("TAB_INDEX", 0);
        LayoutInflater li = getLayoutInflater();
        recent = li.inflate(R.layout.recentlyusedandfavoritestab, null);
        favorite = li.inflate(R.layout.recentlyusedandfavoritestab, null);
        recentWhiteBar = (ImageView) recent.findViewById(R.id.whitebar);
        favoriteWhiteBar = (ImageView) favorite.findViewById(R.id.whitebar);
        if (tabIndexValue == 0) {
        	recentWhiteBar.setVisibility(View.VISIBLE);
        }
        else {
        	favoriteWhiteBar.findViewById(R.id.whitebar).setVisibility(View.VISIBLE);
        }
        recent.setOnTouchListener(this);
        favorite.setOnTouchListener(this);
        ((TextView) favorite.findViewById(R.id.text)).setText(getString(R.string.attach_favorites));
        try {
            tabHost.addTab(tabHost
                    .newTabSpec("A")
                    .setIndicator(recent)
                    .setContent(new Intent(this, RecentlyUsedListActivity.class)));
            tabHost.addTab(tabHost
                    .newTabSpec("B")
                    .setIndicator(favorite)
                    .setContent(new Intent(this, FavoritesListActivity.class)));
        } catch (Exception e) {
        	Toast.makeText(this, getString(R.string.unable_to_start_activity), Toast.LENGTH_LONG).show();
        	finish();
        }

        tabHost.setCurrentTab(tabIndexValue);

    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v != null) {
			TextView title = (TextView) v.findViewById(R.id.text);
			if (title != null) {
				if (title.getText().equals(getString(R.string.attach_favorites))) {
					favoriteWhiteBar.setVisibility(View.VISIBLE);
					recentWhiteBar.setVisibility(View.GONE);
				}
				else {
					favoriteWhiteBar.setVisibility(View.GONE);
					recentWhiteBar.setVisibility(View.VISIBLE);
				}
			}
		}
		return false;
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
