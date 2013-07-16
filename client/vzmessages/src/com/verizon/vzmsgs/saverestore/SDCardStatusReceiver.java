package com.verizon.vzmsgs.saverestore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.strumsoft.android.commons.logger.Logger;

public class SDCardStatusReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		
		  String action = intent.getAction();
          if(action.equalsIgnoreCase(Intent.ACTION_MEDIA_REMOVED)
                  || action.equalsIgnoreCase(Intent.ACTION_MEDIA_UNMOUNTED)
                  || action.equalsIgnoreCase(Intent.ACTION_MEDIA_BAD_REMOVAL)
                  || action.equalsIgnoreCase(Intent.ACTION_MEDIA_EJECT)) {
        	  
        	  new Thread(new Runnable() {
				@Override
				public void run() {
					
					HashMap<String, SDCardStatusContainer> runningTask =  SDCardStatus.getRunningTasks();
					if(Logger.IS_DEBUG_ENABLED) {
						Logger.debug(SDCardStatusReceiver.class,"Currently Running task : "+ runningTask.keySet());
			      	}
					 Set<Entry<String, SDCardStatusContainer>> set = runningTask.entrySet();
					 Iterator<Entry<String, SDCardStatusContainer>> iter = set.iterator();
					 boolean processStopped = false;
					 	while (iter.hasNext()) {
						 	try {
						        Map.Entry entry = (Map.Entry) iter.next();
						      	SDCardStatusContainer taskRunning = (SDCardStatusContainer) entry.getValue();
						      	taskRunning.getBackManager().parser.setSDCardMounted(false);
						      	processStopped = taskRunning.getRunningTask().cancel(true);
						      	taskRunning.getBackManager().setRestoreStatus(BackupManagerImpl.SD_CARD_UNMOUNTED);
						    }
					 		catch(Exception e) {
					 			if(Logger.IS_DEBUG_ENABLED) {
						      		Logger.debug(SDCardStatusReceiver.class,"Failed to treminate - SDCard Ejected");
						      	}
					 		}
					      	
					      	if(Logger.IS_DEBUG_ENABLED) {
					      		Logger.debug(SDCardStatusReceiver.class,"Running process treminated as SDCard Ejected"+processStopped);
					      	}
					    }
			}
          }).start();   
         }
	}

}
