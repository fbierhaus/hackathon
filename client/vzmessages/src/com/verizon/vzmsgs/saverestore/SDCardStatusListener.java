package com.verizon.vzmsgs.saverestore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class SDCardStatusListener {
	
	BroadcastReceiver mSDCardStateChangeListener = null;
	Context mContext = null;
	public SDCardStatusListener(Context context,final SDCardStatus sdCardStatus) {
		mContext = context;
		 mSDCardStateChangeListener = new BroadcastReceiver() {
	         @Override
	           public void onReceive(Context context, Intent intent) {
	               String action = intent.getAction();
	               if(action.equalsIgnoreCase(Intent.ACTION_MEDIA_REMOVED)
	                       || action.equalsIgnoreCase(Intent.ACTION_MEDIA_UNMOUNTED)
	                       || action.equalsIgnoreCase(Intent.ACTION_MEDIA_BAD_REMOVAL)
	                       || action.equalsIgnoreCase(Intent.ACTION_MEDIA_EJECT)) {
	            	   sdCardStatus.status(Intent.ACTION_MEDIA_UNMOUNTED);
	               }               
	           }
	       };
	      
	      
	}
	public void unRegisterSDCardStatusListener() {
		mContext.unregisterReceiver(mSDCardStateChangeListener);
	}
	public void registerSDCardStastusListener() {
		 mContext.registerReceiver(mSDCardStateChangeListener,getSDCardIntentFilter());
	}
	
	private IntentFilter getSDCardIntentFilter() {
		IntentFilter filter = new IntentFilter();
	    filter.addAction(Intent.ACTION_MEDIA_REMOVED);
	    filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
	    filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
	    filter.addAction(Intent.ACTION_MEDIA_EJECT);
	    filter.addDataScheme("file");
	    return filter;
	}
	
	

}

