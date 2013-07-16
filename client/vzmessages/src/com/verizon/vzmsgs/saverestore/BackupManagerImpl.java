package com.verizon.vzmsgs.saverestore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.xml.sax.SAXException;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.ui.RestoreConversationListActivity.ParsePreviewListener;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.VMASyncHook;

/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since  Sep 4, 2012
 */
public class BackupManagerImpl implements BackupManager {
   
	private Context mContext;
	XmlParser parser;
	private MessagesDaoImpl daoImpl;
	private ArrayList<BackUpMessage> messages;
	private int restoreStatus = 0;
	private int restoreCount = -1;
	private int progressCount =  0;
	private boolean isOldVersion;
	private HashMap<String, List<Object>> restoreItems;
	public  static final int SUCEEDDED = 1;
	public static final int EMPTY_FILE = 2;
	public static final int ALREADYEXISTS = 3;
	public static final int FAILED = 4;
	public static final int SD_CARD_UNMOUNTED = 5;
	
	public static final int SAVE = 1;
	public static final int RESTORE = 2;
 	
 	//Sync status variables
 	public static final int BSYNC_SAVE_CANCELLED = 100;
 	public static final int BSYNC_SAVING_CHANGES =  101;
 	public static final int BSYNC_RESTORING_CHANGES =  102;
 	public static final int BSYNC_SAVED =  103;
 	public static final int BSYNC_RESTORED =  104;
 	public static final int BSYNC_RESTORE_FAILED =  106;
  	public static final int BSYNC_ALREADYEXISTS =  107;
 	public static final int BSYNC_EMPTY_FILE =  108;
 	public static final int BSYNC_RESTORE_CANCELLED = 109;
	public static final int BSYNC_SD_CARD_UNMOUNTED = 110;
	public static final int BSYNC_SD_CARD_INSUFFICIENT_SPACE = 111;
 	//Sync action intent
 	public static final String BACKUP_SYNC = "vzm.backup.status";
 	public static final String BACKUP_XTRA_STATUS = "x-status";
 	public static final String BACKUP_COUNT = "x-count";
 	public static final String BACKUP_TOTAL_COUNT = "x-total";
 	public static final String EXTRA_ACTION = "x-action";
 	
 	
 	
 	
 	
	
	public BackupManagerImpl(Context context) {
	     mContext = context;
	     parser = new XmlParser(mContext);
	     daoImpl = new MessagesDaoImpl(mContext);
	     restoreItems = new HashMap<String, List<Object>>();
	}
	
	
	@Override
	public void getConversations(String filePath, ParsePreviewListener mParsePreviewListener, String recipients ,boolean fromConversation) throws
					OutOfMemoryError, ParserConfigurationException, SAXException, IOException, Exception {
     
		ArrayList<String> recipientList = new ArrayList<String>();
		recipientList.add(recipients);
		
			messages = parser.parseXML(filePath, fromConversation, recipientList, mParsePreviewListener, false);
			restoreStatus = Integer.valueOf(parser.getParseStatus()) == 0 ? 2 : 0;
		
		
    }

	
	
	@Override
	public ArrayList<BackUpMessage> getMessages(String recipients) {
		if (Logger.IS_DEBUG_ENABLED) {
	         Logger.debug(getClass(), "Getting messages of recipients :: " + recipients);
	    }
		restoreStatus = EMPTY_FILE;
		ArrayList<BackUpMessage> sortedMsgs = new ArrayList<BackUpMessage>();
		Set<String> keySet = restoreItems.keySet();
		Iterator<String> iterator = keySet.iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (key.equalsIgnoreCase(recipients)) {
				for (Object index : restoreItems.get(key)) {
					for (int count = 0; count < messages.size(); count++) {
						BackUpMessage message = (BackUpMessage) messages
								.get(count);
						if (isOldVersion) {
							if (message.getPduData().get("date").equalsIgnoreCase((String)index)) {
								sortedMsgs.add(message);
								break;
							}
						} else {
							if (message.getMsgIndex() == (Integer)index) {
								sortedMsgs.add(message);
								break;
							}	
						}
						
					}
				}
			}
		}
		
		if (sortedMsgs.size() > 0) {
			restoreStatus = SUCEEDDED;
		}
		return sortedMsgs;
	}
	

	@Override
	public void restoreConversation(ArrayList<String> recipientsList, final BackUpStatusListener listener, String filePath) throws OutOfMemoryError, ParserConfigurationException, SAXException, IOException, Exception {
		ArrayList<BackUpMessage> messages =  new ArrayList<BackUpMessage>();
		messages = parser.parseXML(filePath, false, recipientsList, new ParsePreviewListener() {
			
			@Override
			public void updatePreviewMessageList(BackUpMessage message) {
				try {
					restoreData(message, listener);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OperationApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			@Override
			public void updatePreviewListArrayList(BackUpMessage msgThreads) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean isCancelled() {
				// TODO Auto-generated method stub
				return false;
			}
		}, true);
		/*for (String recipients : recipientsList) {
			 for(BackUpMessage message : getMessages(recipients)) {
				 messages.add(message);
			 }
		}*/
		
		if (messages.isEmpty()) {
			return;
		}
		//restoreData(messages, listener);
		
		
	}

  
    @Override
	public void saveConversations(ArrayList<Long[]> msgIDs, String filePath, BackUpStatusListener listener, boolean supportMMS) 
				throws FileNotFoundException, IOException, JSONException, SDCardUnmountException, Exception {
		    parser.createXMLFromData(msgIDs, filePath, listener);
    	   //TODO : CSV FileSave -  parser.createCSVFileData(msgIDs, filePath, listener);
	}

	@Override
	public void saveMessage(long msgID, long threadID, boolean isSMS, String filePath, BackUpStatusListener listener) 
			throws FileNotFoundException, IOException, JSONException, SDCardUnmountException, Exception  {
		parser.createXMLFromData(msgID, isSMS, filePath, listener); 
		
	}

	@Override
	public void restoreAll(String filePath, final BackUpStatusListener listener) throws 
					ParserConfigurationException, SAXException, IOException, OutOfMemoryError, Exception{
		
		messages = parser.parseXML(filePath, false, null, new ParsePreviewListener() {
			
			@Override
			public void updatePreviewMessageList(BackUpMessage message) {
				try {
					restoreData(message, listener);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OperationApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			@Override
			public void updatePreviewListArrayList(BackUpMessage msgThreads) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean isCancelled() {
				// TODO Auto-generated method stub
				return false;
			}
		}, true);
		//restoreStatus = Integer.valueOf(parser.getParseStatus()) == 0 ? 2 : 0;
		if (messages.isEmpty()) {
			return;
		}
       // restoreData(messages, listener);
		
    }
	
	public int getRestoreStatus() {
		return restoreStatus;
	}


	/* (non-Javadoc)
	 * @see com.verizon.vzmsgs.saverestore.BackUpData#restoreMessages(java.util.ArrayList)
	 */
	@Override
	public void restoreMessages(ArrayList<BackUpMessage> messages, BackUpStatusListener listener) throws RemoteException, JSONException, OperationApplicationException, Exception{
		restoreStatus = EMPTY_FILE;
		if (messages.isEmpty()) {
			return;
		}
		restoreData(messages, listener);
		
	}

	public void restoreData(ArrayList<BackUpMessage> messages, BackUpStatusListener listener) throws RemoteException, JSONException, OperationApplicationException, Exception {
		int total = messages.size();
		int progressCount = 0;
		long threadID = -1;
		long prevThreadID = -1;
		for (BackUpMessage message : messages) {
			if (listener.isTaskCancelled()) {
				restoreStatus = FAILED;
				return;
			}
			long msgID = -1;

			if (message.isSms()) {
				if (!daoImpl.doesSMSExists(message)) {
					listener.updateStatus(++progressCount, total);
					threadID = getThreadId(message.getRecipients());
					msgID = daoImpl.addSMS(message, threadID);
					if (prevThreadID == -1) {
						prevThreadID = threadID;
					} else if (prevThreadID != threadID) {
						fixTimeStampIssue(prevThreadID);
						prevThreadID = threadID;
					}
					VMASyncHook.syncSendingSMS(mContext, msgID);
					ConversationDataObserver.onNewMessageAdded(threadID, msgID, ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
					++restoreCount;
					restoreStatus = SUCEEDDED;
				} else {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.warn("Restored SMS failed. duplicate !!!");
					}
					listener.updateStatus(-1, total);
					restoreStatus = ALREADYEXISTS;
				}

			} else {
				
					if (!daoImpl.doesMMSExists(message)) {
						listener.updateStatus(++progressCount, total);
						threadID = getThreadId(message.getRecipients());
						msgID = daoImpl.addMMS(message, threadID);
						if (prevThreadID == -1) {
							prevThreadID = threadID;
						} else if (prevThreadID != threadID) {
							fixTimeStampIssue(prevThreadID);
							prevThreadID = threadID;
						}
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.info("Restored MMS. mmsid=" + msgID);
						}
						VMASyncHook.syncSendingMMS(mContext, msgID);
						ConversationDataObserver.onNewMessageAdded(threadID, msgID, ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
						++restoreCount;
						restoreStatus = SUCEEDDED;
					} else {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.warn("Restored MMS failed. duplicate !!!");
						}
						listener.updateStatus(-1, total);
						restoreStatus = ALREADYEXISTS;
					}
				
			}
		}
		if (restoreCount != -1) {
			restoreStatus = SUCEEDDED;
			restoreCount = -1;
		} else {
			restoreStatus = ALREADYEXISTS;
		}

	}

	public void restoreData(BackUpMessage message, BackUpStatusListener listener) throws RemoteException, JSONException, OperationApplicationException, Exception {
		//int total = messages.size();
		//int progressCount = 0;
		long threadID = -1;
		long prevThreadID = -1;
		/*for (BackUpMessage message : messages) {*/
			if (listener.isTaskCancelled()) {
				restoreStatus = FAILED;
				return;
			}
			long msgID = -1;

			if (message.isSms()) {
				if (!daoImpl.doesSMSExists(message)) {
					progressCount = progressCount + 1;
					listener.updateStatus(progressCount, 0);//total);
					threadID = getThreadId(message.getRecipients());
					msgID = daoImpl.addSMS(message, threadID);
					if (prevThreadID == -1) {
						prevThreadID = threadID;
					} else if (prevThreadID != threadID) {
						fixTimeStampIssue(prevThreadID);
						prevThreadID = threadID;
					}
					VMASyncHook.syncSendingSMS(mContext, msgID);
					ConversationDataObserver.onNewMessageAdded(threadID, msgID, ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
					++restoreCount;
					restoreStatus = SUCEEDDED;
				} else {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.warn("Restored SMS failed. duplicate !!!");
					}
					listener.updateStatus(-1, 0);//total);
					restoreStatus = ALREADYEXISTS;
				}

			} else {
				
					if (!daoImpl.doesMMSExists(message)) {
						progressCount = progressCount + 1;
						listener.updateStatus(progressCount, 0);// total);
						threadID = getThreadId(message.getRecipients());
						msgID = daoImpl.addMMS(message, threadID);
						if (prevThreadID == -1) {
							prevThreadID = threadID;
						} else if (prevThreadID != threadID) {
							fixTimeStampIssue(prevThreadID);
							prevThreadID = threadID;
						}
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.info("Restored MMS. mmsid=" + msgID);
						}
						VMASyncHook.syncSendingMMS(mContext, msgID);
						ConversationDataObserver.onNewMessageAdded(threadID, msgID, ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
						++restoreCount;
						restoreStatus = SUCEEDDED;
					} else {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.warn("Restored MMS failed. duplicate !!!");
						}
						listener.updateStatus(-1, 0);//total);
						restoreStatus = ALREADYEXISTS;
					}
				
			}
		//}
		if (restoreCount != -1) {
			restoreStatus = SUCEEDDED;
			restoreCount = -1;
		} else {
			restoreStatus = ALREADYEXISTS;
		}

	}
	
	
	public interface BackUpStatusListener {
		public void updateStatus(int progress, int count);
		public boolean isTaskCancelled();
	}

	private void fixTimeStampIssue(long threadId) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("fixing TimeStampIssue . inserting and deleting dummy messages");
		}
		if (threadId > 0) {
			ContentValues values = new ContentValues(7);
			values.put(Sms.ADDRESS, "unknown");
			values.put(Sms.READ, 1);
			values.put(Sms.BODY, "unknown");
			values.put(Sms.THREAD_ID, threadId);
			values.put(Sms.TYPE, Sms.MESSAGE_TYPE_ALL);
			long id = insert(VZUris.getSmsUri(), values);
			String where = Sms._ID + "=" + id;
			Uri uri = Uri.withAppendedPath(VZUris.getSmsConversations(),
					String.valueOf(threadId));
			int count = delete(uri, where);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("Added empty msg:" + id);
				Logger.debug("Delete empty msg:" + count + "where=" + where);
			}
		}

	}
	
	public long getThreadId(String recep) {
		if (recep != null) {
			String[] recipientsMDN = recep.split(";");
			HashSet<String> recipients = new HashSet<String>();
			for (String recipient : recipientsMDN) {
				recipients.add(recipient);
			}
			return Threads.getOrCreateThreadId(mContext, recipients);
		}
		return 0;
	}

	private long insert(Uri uri, ContentValues values) {
		final Uri u = insertForUri(uri, values);
		return (u != null) ? ContentUris.parseId(u) : -1;
	}
	
	private Uri insertForUri(Uri uri, ContentValues values) {
		final long start;
		if (Logger.IS_DEBUG_ENABLED) {
			start = SystemClock.uptimeMillis();
		}

		final Uri u = mContext.getContentResolver().insert(uri, values);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(),
					"insert: time = " + (SystemClock.uptimeMillis() - start)
							+ "ms, uri = " + uri + ", values = " + values
							+ ", returning " + u);
		}
		return u;
	}

	private int delete(Uri uri, String where) {
		final long start;
		if (Logger.IS_DEBUG_ENABLED) {
			start = SystemClock.uptimeMillis();
		}

		final int rows =mContext.getContentResolver().delete(uri, where, null);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(),
					"delete: time = " + (SystemClock.uptimeMillis() - start)
							+ "ms, uri = " + uri + ", where = " + where
							+ ", returning " + rows);
		}
		return rows;
	}
	public void setRestoreStatus(int status) {
		restoreStatus = status;
	}

}
