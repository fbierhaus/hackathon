package com.verizon.mms.util;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.widget.Button;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl.BackUpStatusListener;
import com.verizon.vzmsgs.saverestore.PopUpUtil;
import com.verizon.vzmsgs.saverestore.SDCardStatus;
import com.verizon.vzmsgs.saverestore.SDCardStatusContainer;

public class RestoreConversationTask extends
		AsyncTask<Void, Integer, Integer> {

	
	private PopUpUtil util;
	private Context mContext;
	private BackupManagerImpl manager;
	private ArrayList<String> objects;
	private SharedPreferences prefs;
	private Button restoreButton;
	private String filePath;
	private Editor edit;
	
	
	public RestoreConversationTask(Context context, Button restoreButton, ArrayList<String> objects, String filePath, BackupManagerImpl mgr) {
	     mContext = context;
	     this.objects = objects;
	     this.filePath = filePath;
	     util = new PopUpUtil(mContext, true);
	     manager = mgr;
	     this.restoreButton = restoreButton;
	     
	 	 prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		 edit = prefs.edit();
		 if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreConversationTask.class,"RestoreConversationTask created");
		 }
		 
	}
	
	@Override 
	protected void onPreExecute() {
		super.onPreExecute();
		 if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreConversationTask.class,"RestoreConversationTask PreExecute");
		 }
		SDCardStatusContainer runningProcess = new SDCardStatusContainer(this,manager);
		SDCardStatus.addRunningTask(runningProcess, RestoreConversationTask.class.toString());
		
		edit.putInt(util.PROGRESS_KEY, 2);
		edit.commit();
		if (restoreButton != null) {
			restoreButton.setEnabled(false);
		}
		util.showNotification(BackupManagerImpl.BSYNC_RESTORING_CHANGES, 0, -1);
		
  	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
        if (values[0] == 1) {
        	 Intent intent = new Intent(mContext, ConversationListActivity.class);
    		 intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    		 mContext.startActivity(intent);
    		 ((Activity)mContext).finish();
        }
        if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreConversationTask.class,"RestoreConversationTask Progress "+values[0]+ " : " + values[1]);
		 }
		util.showNotification(BackupManagerImpl.BSYNC_RESTORING_CHANGES, values[0], values[1]);
		super.onProgressUpdate(values);
	}
	
	@Override
	protected Integer doInBackground(Void... params) {
		try {
			if	(Logger.IS_DEBUG_ENABLED) {
				 Logger.debug(RestoreConversationTask.class,"RestoreConversationTask doInBackground");
			 }
			manager.restoreConversation(objects, new BackUpStatusListener() {
				
				@Override
				public void updateStatus(int progress, int count) {
					
					publishProgress(progress, count);
				}

				@Override
				public boolean isTaskCancelled() {
					if (isCancelled()) {
						return true;
					} else {
						return false;	
					}
				}
			}, filePath);
		} catch (OutOfMemoryError e) {
			if	(Logger.IS_DEBUG_ENABLED) {
				 Logger.debug(RestoreConversationTask.class,"OutOfMemoryError" +e);
			}
			manager.setRestoreStatus(BackupManagerImpl.FAILED);
			updatedRestoreState();
		} catch (ParserConfigurationException e) {
			if	(Logger.IS_DEBUG_ENABLED) {
				 Logger.debug(RestoreConversationTask.class,"ParserConfigurationException" +e);
				 manager.setRestoreStatus(BackupManagerImpl.FAILED);
				 updatedRestoreState();
			 }
		} catch (SAXException e) {
			if	(Logger.IS_DEBUG_ENABLED) {
				 Logger.debug(RestoreConversationTask.class,"SAXException" +e);
		    }
		    //manager.setRestoreStatus(BackupManagerImpl.FAILED);
			manager.setRestoreStatus(BackupManagerImpl.SUCEEDDED); //TODO : Need to be verified
			updatedRestoreState();
		} catch (IOException e) {
			if	(Logger.IS_DEBUG_ENABLED) {
				 Logger.debug(RestoreConversationTask.class,"IOException" +e);
			 }
			 manager.setRestoreStatus(BackupManagerImpl.FAILED);
			 updatedRestoreState();
		} catch (Exception e) {
			if	(Logger.IS_DEBUG_ENABLED) {
				 Logger.debug(RestoreConversationTask.class,"Exception" +e);
			 }
			 manager.setRestoreStatus(BackupManagerImpl.FAILED);
			 updatedRestoreState();
		}
		finally {
			SDCardStatus.removeRunningTask(RestoreConversationTask.class.toString());
		}
	
	  return manager.getRestoreStatus();
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreConversationTask.class,"onPostExecute result" +result);
		 }
		SDCardStatus.removeRunningTask(RestoreConversationTask.class.toString());
			
		if (restoreButton != null) {
			restoreButton.setEnabled(true);
		}
		util.clearSyncNotification();
		notifyStatus(result);
		updatedRestoreState();
		super.onPostExecute(result);
	}
	@Override
	protected void onCancelled() {
		super.onCancelled();
		 manager.setRestoreStatus(BackupManagerImpl.SD_CARD_UNMOUNTED);
		if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreConversationTask.class,"onCancelled");
		 }
		SDCardStatus.removeRunningTask(RestoreConversationTask.class.toString());
		notifyStatus(manager.getRestoreStatus());
		updatedRestoreState();
		
	}
	
	void notifyStatus(int result) {
	
		switch (result) {
		case BackupManagerImpl.SUCEEDDED:
			util.showNotification(BackupManagerImpl.BSYNC_RESTORED, 0, 0);
			break;
		case BackupManagerImpl.EMPTY_FILE:
			util.fileEmptyPopup(mContext);
			break;
		case BackupManagerImpl.ALREADYEXISTS:
			util.showNotification(BackupManagerImpl.BSYNC_RESTORE_FAILED, 0, 0);
			util.show(mContext);
			break;
		case BackupManagerImpl.FAILED:
			util.showNotification(BackupManagerImpl.BSYNC_RESTORE_CANCELLED, 0, 0);
			break;
		case BackupManagerImpl.SD_CARD_UNMOUNTED:
			util.showNotification(BackupManagerImpl.BSYNC_SD_CARD_UNMOUNTED, 0, 0);
			break;
		default:
			break;
		}
	}
	void updatedRestoreState() {
		edit.putInt(util.PROGRESS_KEY, 0);
		edit.commit();
	}
	
}
