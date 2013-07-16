package com.verizon.mms.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.mms.MmsConfig;

public class RecyclerScheduleReceiver extends BroadcastReceiver {
    public static final String PREV_RECYCLE_TIME = "recycle_time";
    private static final int MINIMUM_DELTA_DELAY = 20000;
    
    private static int selfCount;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onReceive:");
        }
        
        ApplicationSettings settings = ApplicationSettings.getInstance();
        
        if (settings != null && !settings.isApplicationInBackground()) {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(getClass(), " defering the sync since application is in forerground by " + MmsConfig.getRecyclerDelay());
        	}
            long nxtScheduleTime = SystemClock.elapsedRealtime() + MmsConfig.getRecyclerDelay();
            scheduleRecycler(context, nxtScheduleTime, MmsConfig.getRecyclerInterval());
            return;
        }
        
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lastRecycleTime = prefs.getLong(PREV_RECYCLE_TIME, 0);
        long timeInterval = System.currentTimeMillis() - lastRecycleTime;
        long recyclerInterval = MmsConfig.getRecyclerInterval();
        long delta = timeInterval - recyclerInterval;

        // go for another recycler session only if we have already passed the RecyclerInterval
        // or if there is still MINIMUM_DELTA_DELAY time left
        if (delta > 0 || delta + MINIMUM_DELTA_DELAY > 0) {
        	new RecyclerThread(context, "Recycler-" + (++selfCount)).start();
        } else {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(getClass(), " defering the sync for " + delta);
        	}
            long nxtScheduleTime = SystemClock.elapsedRealtime() + recyclerInterval - timeInterval;
            scheduleRecycler(context, nxtScheduleTime, MmsConfig.getRecyclerInterval());
        }
    }

    public static void scheduleRecycler(Context context, long startTime, int interval) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("RecyclerScheduleReceiver.scheduleRecycler startTime " + startTime + " interval " + interval);
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RecyclerScheduleReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME, startTime, interval, pi);
    }

    public static void stopRecycler(Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("RecyclerScheduleReceiver.stopRecycler()");
        }
        Intent intent = new Intent(context, RecyclerScheduleReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
 
    public static void scheduleOnLaunchAlarm(Context context) {
    	final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lastRecycleTime = prefs.getLong(PREV_RECYCLE_TIME, 0);
        long recyclerInterval = MmsConfig.getRecyclerInterval();
        long initialTriggertime = recyclerInterval;
        
        if (lastRecycleTime > 0) {
        	long timeInterval = System.currentTimeMillis() - lastRecycleTime;
        	long delta = timeInterval - recyclerInterval;
        	
        	if (delta > 0 || delta + MINIMUM_DELTA_DELAY > 0) {
        		initialTriggertime = MINIMUM_DELTA_DELAY;
        	} else {
        		initialTriggertime = recyclerInterval - timeInterval;
        	}
        	
        }
        
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug("RecyclerScheduleReceiver.scheduleOnLaunchAlarm scheduling the alarm after " + initialTriggertime);
        }
        scheduleRecycler(context, SystemClock.elapsedRealtime() + initialTriggertime
				, MmsConfig.getRecyclerInterval());
    }
    
    class RecyclerThread extends Thread {
    	Context mContext;
    	
    	public RecyclerThread(Context context, String threadName) {
    		setName(threadName);
    		setPriority(NORM_PRIORITY - 1);
    		mContext = context;
    	}

    	@Override
    	public void run() {
    		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    		final Recycler recycler = Recycler.getMessageRecycler();
    		
    		recycler.deleteMsgsOverLimit(mContext);

    		Editor editor = prefs.edit();
    		editor.putLong(PREV_RECYCLE_TIME, System.currentTimeMillis());
    		editor.commit();
    	}
    }
}