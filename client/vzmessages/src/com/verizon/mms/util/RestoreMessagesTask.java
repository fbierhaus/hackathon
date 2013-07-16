/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since  Sep 9, 2012
 */
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
import com.verizon.mms.ui.RestoreConversationListActivity.ParsePreviewListener;
import com.verizon.vzmsgs.saverestore.BackUpMessage;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl.BackUpStatusListener;
import com.verizon.vzmsgs.saverestore.PopUpUtil;
import com.verizon.vzmsgs.saverestore.SDCardStatus;
import com.verizon.vzmsgs.saverestore.SDCardStatusContainer;

/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since  Sep 9, 2012
 */
public class RestoreMessagesTask extends AsyncTask<Void, Integer, Integer> {

	private PopUpUtil util;
	private Context mContext;
	private String restoreXMLPath;
	private BackupManagerImpl manager;
	private ArrayList<BackUpMessage> objects;
	private SharedPreferences prefs;
	private Button restoreButton;
	private Editor edit;
	
		
	/**
	 * Constructor 
	 * 
	 *  Activity context
	 *  String filePath
	 */
	public RestoreMessagesTask(Context context, String filePath) {
		if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreMessagesTask.class,"RestoreMessagesTask created 1");
		}
		mContext = context;
		restoreXMLPath = filePath;
		util = new PopUpUtil(mContext, false);
		manager = new BackupManagerImpl(mContext);
		this.restoreButton = null;
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		edit = prefs.edit();
		
		
	}
	
	public RestoreMessagesTask(Context context, Button restoreButton, ArrayList<BackUpMessage> messages,BackupManagerImpl mgr) {
		 if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreMessagesTask.class,"RestoreMessagesTask created 2");
		 }
		 mContext = context;
	     this.objects = messages;
	     util = new PopUpUtil(mContext, true);
	     manager = mgr;
	     restoreXMLPath = null;
	     this.restoreButton = restoreButton;
	     messages = new ArrayList<BackUpMessage>();
	 	 prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		 edit = prefs.edit();
		
		 
	}
	
	
	@Override 
	protected void onPreExecute() {
		super.onPreExecute();
		if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreMessagesTask.class,"onPreExecute()");
		}
		edit.putInt(util.PROGRESS_KEY, 2);
		edit.commit();
		SDCardStatusContainer runningProcess = new SDCardStatusContainer(this,manager);
		SDCardStatus.addRunningTask(runningProcess, RestoreMessagesTask.class.toString());
		
		if (restoreButton != null) {
			restoreButton.setEnabled(false);
		}
		
		util.showNotification(BackupManagerImpl.BSYNC_RESTORING_CHANGES, 0, -1);
		
  	}

	@Override
	protected Integer doInBackground(Void... params) {
		
		if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreMessagesTask.class,"doInBackground()");
		}
        if (restoreXMLPath != null) {

        	try {
        		if	(Logger.IS_DEBUG_ENABLED) {
       			 	Logger.debug(RestoreMessagesTask.class,"restoreAll()");
        		}
				manager.restoreAll(restoreXMLPath, new BackUpStatusListener() {
					
					@Override
					public void updateStatus(int progress, int count) {
						publishProgress(progress, count);
						
					}

					@Override
					public boolean isTaskCancelled() {
						// TODO Auto-generated method stub
						return false;
					}
				});
			} catch (OutOfMemoryError e) {
				if (Logger.IS_DEBUG_ENABLED) {
					 Logger.debug(RestoreMessagesTask.class,"OutOfMemoryError" +e);
				}
				manager.setRestoreStatus(BackupManagerImpl.FAILED);
				updatedRestoreState();
				
			} catch (ParserConfigurationException e) {
				if (Logger.IS_DEBUG_ENABLED) {
					 Logger.debug(RestoreMessagesTask.class,"ParserConfigurationException" +e);
				}
				manager.setRestoreStatus(BackupManagerImpl.FAILED);
				updatedRestoreState();
			} catch (SAXException e) {
				if (Logger.IS_DEBUG_ENABLED) {
					 Logger.debug(RestoreMessagesTask.class,"SAXException" +e);
				}
				manager.setRestoreStatus(BackupManagerImpl.SUCEEDDED);
				//manager.setRestoreStatus(BackupManagerImpl.FAILED);
				updatedRestoreState();
			} catch (IOException e) {
				if (Logger.IS_DEBUG_ENABLED) {
					 Logger.debug(RestoreMessagesTask.class,"IOException" +e);
				}
				manager.setRestoreStatus(BackupManagerImpl.FAILED);
				updatedRestoreState();
			} catch (Exception e) {
				if (Logger.IS_DEBUG_ENABLED) {
					 Logger.debug(RestoreMessagesTask.class,"Exception" +e);
				}
				manager.setRestoreStatus(BackupManagerImpl.FAILED);
				updatedRestoreState();
			}
        	finally {
        		SDCardStatus.removeRunningTask(RestoreMessagesTask.class.toString());
        	}
        	
        	
        } else {
        	if	(Logger.IS_DEBUG_ENABLED) {
   			 	Logger.debug(RestoreMessagesTask.class,"restoreMessages()");
    		}
        		try {
					manager.restoreMessages(objects, new BackUpStatusListener() {
						
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
					});
				} catch (RemoteException e) {
					if (Logger.IS_DEBUG_ENABLED) {
						 Logger.debug(RestoreMessagesTask.class,"RemoteException" +e);
					}
					manager.setRestoreStatus(BackupManagerImpl.FAILED);
					updatedRestoreState();
				} catch (JSONException e) {
					if (Logger.IS_DEBUG_ENABLED) {
						 Logger.debug(RestoreMessagesTask.class,"JSONException" +e);
					}
					manager.setRestoreStatus(BackupManagerImpl.FAILED);
					updatedRestoreState();
				} catch (OperationApplicationException e) {
					if (Logger.IS_DEBUG_ENABLED) {
						 Logger.debug(RestoreMessagesTask.class,"OperationApplicationException" +e);
					}
					manager.setRestoreStatus(BackupManagerImpl.FAILED);
					updatedRestoreState();
				} catch (Exception e) {
					if (Logger.IS_DEBUG_ENABLED) {
						 Logger.debug(RestoreMessagesTask.class,"Exception" +e);
					}
					manager.setRestoreStatus(BackupManagerImpl.FAILED);
					updatedRestoreState();
				}
        		finally{
        			SDCardStatus.removeRunningTask(RestoreMessagesTask.class.toString());
        		}
        		 
        } 
       
        return manager.getRestoreStatus();
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
        if (values[0] != 1) {
        	 if	(Logger.IS_DEBUG_ENABLED) {
     			 Logger.debug(RestoreMessagesTask.class,"RestoreMessagesTask Progress "+values[0]+ " : " + values[1]);
     		 }
     		 util.showNotification(BackupManagerImpl.BSYNC_RESTORING_CHANGES, values[0], values[1]);
        	 
        } else {
        	if (values[0] != -1) {
        		util.showNotification(BackupManagerImpl.BSYNC_RESTORING_CHANGES, values[0], values[1]);
        	}
        	Intent intent = new Intent(mContext, ConversationListActivity.class);
   		    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
   		    mContext.startActivity(intent);
   		    ((Activity)mContext).finish();
        }
      
		
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		SDCardStatus.removeRunningTask(RestoreMessagesTask.class.toString());
		edit.putInt(util.PROGRESS_KEY, 0);
		edit.commit();
		if (restoreButton != null) {
			restoreButton.setEnabled(true);
		}
		util.clearSyncNotification();
		notifyStatus(manager.getRestoreStatus());
		
	}
	@Override
	protected void onCancelled() {
		super.onCancelled();
		 manager.setRestoreStatus(BackupManagerImpl.SD_CARD_UNMOUNTED);
		 if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreMessagesTask.class,"onCancelled");
		 }
		SDCardStatus.removeRunningTask(RestoreConversationTask.class.toString());
		notifyStatus(manager.getRestoreStatus());
		updatedRestoreState();
	}
	void updatedRestoreState() {
		edit.putInt(util.PROGRESS_KEY, 0);
		edit.commit();
	}
	void notifyStatus(int result) {
		 if	(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(RestoreMessagesTask.class,"result "+result);
		 }
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

}
