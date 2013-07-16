package com.verizon.mms.ui;

import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.ApplicationSettings;

/*
 * Class used to keep track of the Applications UI state 
 */
public class VZActivityHelper {
	/*
	 * Interface the activity has to implement to hold the state of the 
	 * activities
	 */
	public interface ActivityState {
		int ACTIVITY_STATE_NONE = -1;
		int ACTIVITY_START = 0;
		int ACTIVITY_PAUSED = 1;
		int ACTIVITY_RESUMED = 2;
		int ACTIVITY_STOP = 3;
		
		public int getActivityState();
		public void finish();
	}
	
	//List of activities currently in the application stack
	private static CopyOnWriteArrayList<ActivityState> runningActivities = new CopyOnWriteArrayList<ActivityState>();
	
	private static KeyguardManager keyguardManager;
	
	private static Handler handler;
	private static Context context;
	private static final int MSG_UPDATE_ACTIVITY_STATUS = 1;
	//delay to avoid volatile app states update i.e when we move from an activity to another 
	//within the app we get activitystopped and activitystarted so to avoid this we check
	//the state of the activity only after the MAC_DELAY time
	private static final long MAX_DELAY = 1000;
	private static long maxDelayTime;
	
	//used to state wether this is the first time the UI has run
	private static boolean firstRun = true;
	//used to hold the previsous states of the application to avoid calling the "appStatusChanged" if 
	//there was no changed in the state
	private static boolean prevStateVisible = false;
	private static boolean prevKeyGuardOn = false;
	
	public static void init(Context context) {
		VZActivityHelper.context = context;
		
		handler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if (msg.what == MSG_UPDATE_ACTIVITY_STATUS) {
					boolean stateVisible = !isActivityVisible();
					boolean keyGuardOn = isKeyguardGuardLocked(VZActivityHelper.context);
					
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug("VZActivityHelper.handleMessage stateVisible = " + stateVisible + "keyguardOn = " + keyGuardOn +
								"prevstateVisible = " + prevStateVisible + "prevkeyguardOn = " + prevKeyGuardOn);
					}
					
					if (firstRun) {
						firstRun = false;
					} else if (prevStateVisible == stateVisible && prevKeyGuardOn == keyGuardOn){
						//update the status only if there is a change in the application state
						//Ex if we move to conversation view from conversation list there is no need
						//to send the application in foreground again
						return;
					}
					
					prevStateVisible = stateVisible;
					prevKeyGuardOn = keyGuardOn;
					
					ApplicationSettings.getInstance().appStatusChanged(stateVisible, keyGuardOn);
				}
			}
		};
	}
	
	protected static void activityCreated(ActivityState activity) {
		synchronized (runningActivities) {
			runningActivities.add(activity);
		}
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("VZActivityHelper.activityCreated activities list " + runningActivities);
		}
	}
	
	protected static void activityDestroyed(ActivityState activity) {
		synchronized (runningActivities) {
			runningActivities.remove(activity);
		}
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("VZActivityHelper.activityDestroyed activities list " + runningActivities);
		}
	}
	
	public static boolean isActivityVisible() {
	    boolean visible = false;
	    
	    synchronized (runningActivities) {
	    	if (runningActivities.size() > 0) {
	    		for (ActivityState state : runningActivities) {
	    			int activityState = state.getActivityState();

	    			visible = activityState == ActivityState.ACTIVITY_RESUMED;
	    			
	    			if (visible) {
	    				break;
	    			}
	    		}
	    	}
	    }
	    if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("VZActivityHelper.isActivityVisible() " + visible);
		}
	    
	    return visible;
	}  
	
	public static boolean isActivityOnTop() {
	    boolean visible = false;
	    
	    synchronized (runningActivities) {
	    	if (runningActivities.size() > 0) {
	    		for (ActivityState state : runningActivities) {
	    			int activityState = state.getActivityState();

	    			visible = activityState >= ActivityState.ACTIVITY_START && activityState < ActivityState.ACTIVITY_STOP;
	    			
	    			if (visible) {
	    				break;
	    			}
	    		}
	    	}
	    }
	    if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("VZActivityHelper.isActivityVisible() " + visible);
		}
	    
	    return visible;
	}
	
	public static boolean isAppInBackground(Context context) {
		KeyguardManager manager = (KeyguardManager)context.getSystemService(Activity.KEYGUARD_SERVICE);
		
		return !isActivityVisible() || manager.inKeyguardRestrictedInputMode(); 
	}  

	public static void activityStarted(Context context) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("VZActivityHelper.activityStarted");
		}
		
		postAppStatusUpdate();
	}

	public static void activityStoped(Context context) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("VZActivityHelper.activityStoped");
		}
		
		postAppStatusUpdate();
	} 
	
	private static boolean isKeyguardGuardLocked(Context context) {
        if (keyguardManager == null) {
            keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        }
        boolean locked = keyguardManager.inKeyguardRestrictedInputMode();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Phone lock state from KEYGUARD_SERVICE: Current state:"
                    + (locked ? "LOCKED" : "UNLOCKED"));
        }
        return locked;
    }
	
	private static void postAppStatusUpdate() {
		final long curTime = SystemClock.uptimeMillis();
		long delay = MAX_DELAY;
		
		if (maxDelayTime != 0) {
			// make sure we don't go past the max delay time
			long delta = maxDelayTime - curTime;
			
			if (delay >= delta) {
				delay = delta;
			}
		} else {
			maxDelayTime = curTime + MAX_DELAY;
		}
		
		if (delay <= 0) {
			delay = MAX_DELAY;
			maxDelayTime = 0;
		}
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("VZActivityHelper.postAppStatusUpdate, curTime = " + curTime + ", delay = " + delay);
		}
		
		handler.removeMessages(MSG_UPDATE_ACTIVITY_STATUS);
		handler.sendMessageDelayed(handler.obtainMessage(MSG_UPDATE_ACTIVITY_STATUS), delay);
	}
	
	public static void closePausedActivityOnStack(){
	    try {
            int size = runningActivities.size();
            int i = 0;
	        for (ActivityState state : runningActivities) {
	            if (i < size - 1) {
	                state.finish();
	            }
	            i++;
	        }
	        
        } catch (Exception e) {
            if(Logger.IS_ERROR_ENABLED){
                Logger.error("VZActivityHelper :closeAllActivity" ,e);
            }
        }
	}
	
}
