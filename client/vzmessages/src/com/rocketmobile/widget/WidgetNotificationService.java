package com.rocketmobile.widget;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.data.Conversation;

/**
 * Service to update HomeScreenWidget
 */
public class WidgetNotificationService extends Service {
    private ContentObserver observer;
	private Looper looper;

    @Override
    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // start observer thread
        final HandlerThread thread = new HandlerThread("Widget", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // register observer thread to receive MMS/SMS content change notifications
        looper = thread.getLooper();
        observer = new CacheObserver(this, new Handler(looper));
        getContentResolver().registerContentObserver(VZUris.getMmsSmsConversationUri(), false, observer);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != observer) {
            getContentResolver().unregisterContentObserver(observer);
            observer = null;
            looper.quit();
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        return Service.START_STICKY;
    }

    private static int getUnreadMessageCountFromDB(Context context) {
        final int mms = Conversation.getUnreadMmsFromInbox(context);
        final int sms = Conversation.getUnreadSmsFromInbox(context);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(WidgetNotificationService.class, "getUnreadMessageCountFromDB: sms = " + sms + ", mms = " + mms);
        }
        return sms + mms;
    }


    class CacheObserver extends ContentObserver {
        private Context context;
		private Handler handler;
        private int lastCount = 0;
        private long lastCheck;

        private static final long CHECK_INTERVAL = 5000;

		public CacheObserver(Context context, Handler handler) {
			super(handler);
			this.context = context;
			this.handler = handler;
			// post initial check
    		handler.post(callback);
		}

        public void onChange(boolean selfChange) {
        	// only perform check periodically
    		handler.removeCallbacks(callback);
        	final long now = SystemClock.uptimeMillis();
        	final long delta = now - lastCheck;

        	if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(WidgetNotificationService.class, "onChange: delta = " + delta);
            }

        	if (delta > CHECK_INTERVAL) {
        		lastCheck = now;
	        	int newCount = getUnreadMessageCountFromDB(context);
	        	
	            // update only when there is some change!
	            if (lastCount != newCount) {
	                HomeScreenWidget.updateWidget(context, newCount);
	                lastCount = newCount;
	            }
	
	            if (Logger.IS_DEBUG_ENABLED) {
	            	Logger.debug(WidgetNotificationService.class, "onChange: count = " + newCount +
	            		" in " + (SystemClock.uptimeMillis() - now) + "ms");
	            }
        	}
        	else {
        		// post a callback
        		handler.postDelayed(callback, CHECK_INTERVAL - delta);
        	}
        }

        private Runnable callback = new Runnable() {
			@Override
			public void run() {
				onChange(false);
			}
        };
    }
}