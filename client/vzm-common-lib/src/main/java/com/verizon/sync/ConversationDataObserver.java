package com.verizon.sync;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import com.strumsoft.android.commons.logger.Logger;

/**
 *This class is used to register and notify for any changes in the Coversations
 */
public class ConversationDataObserver {
	public static final int MSG_SRC_TELEPHONY = 1;
	public static final int MSG_SRC_RCS = 2;
	public static final int MSG_SRC_CHAT = 3;
	
	public static final int MSG_TYPE_SMS = 1;
	public static final int MSG_TYPE_MMS = 2;

	private static ArrayList<DatasetChangeListener> datasetListener = new ArrayList<DatasetChangeListener>();
	
	public static void addConvDataListener(DatasetChangeListener listener) {
		synchronized(datasetListener) {
			if (!datasetListener.contains(listener)) {
				datasetListener.add(listener);
			}
		}
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("ConversationDataObserver.addConvDataListener Listeners registered for Conversation data set changes " + datasetListener + " threadId = " + listener.getRegisteredThreadId());;
		}
	}
	
	public static void removeConvDataListener(DatasetChangeListener listener) {
		synchronized(datasetListener) {
			datasetListener.remove(listener);
		}
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("ConversationDataObserver.removeConvDataListener Listeners registered for Conversation data set changes " + datasetListener);;
		}
	}
	
	public static void onNewMessageAdded(long threadId, long msgId, int msgType, long msgSource) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("ConversationDataObserver.onNewMessageAdded threadId = " + threadId + " msgId = " + msgId
					+ " msgType = " + msgType + " msgSource = " + msgSource);
		}
		try {
			final ArrayList<DatasetChangeListener> toNotify;
            synchronized (datasetListener) {
				toNotify = (ArrayList<DatasetChangeListener>)datasetListener.clone();
            }
            for (DatasetChangeListener listener : toNotify) {
            	if (threadId == listener.getRegisteredThreadId()) {
            		listener.onMessageAdded(threadId, msgId, msgType, msgSource);
            	}
            }
		} catch (Exception e) {
			if (Logger.IS_ACRA_ERROR_REPORT_ENABLED) {
				Logger.postErrorToAcraIfDebug("Caught exception in onNewMessageAdded ", e);
			}
		}
	}
	
	public static void onMessageDeleted(long threadId, long msgId, int msgType, long msgSource) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("ConversationDataObserver.onMessageDeleted threadId = " + threadId + " msgId = " + msgId
					+ " msgType = " + msgType + " msgSource = " + msgSource);
		}
		try {
			final ArrayList<DatasetChangeListener> toNotify;
            synchronized (datasetListener) {
				toNotify = (ArrayList<DatasetChangeListener>)datasetListener.clone();
            }
            for (DatasetChangeListener listener : toNotify) {
            	if (threadId == listener.getRegisteredThreadId()) {
            		listener.onMesssageDeleted(threadId, msgId, msgType, msgSource);
            	}
            }
		} catch (Exception e) {
			if (Logger.IS_ACRA_ERROR_REPORT_ENABLED) {
				Logger.postErrorToAcraIfDebug("Caught exception in onMessageDeleted ", e);
			}
		}
	}
	
	public static void onMessageStatusChanged(long threadId, long msgId, int msgType, long msgSource) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("ConversationDataObserver.onMessageStatusChanged threadId = " + threadId + " msgId = " + msgId
					+ " msgType = " + msgType + " msgSource = " + msgSource);
		}
		try {
			final ArrayList<DatasetChangeListener> toNotify;
			synchronized (datasetListener) {
				toNotify = (ArrayList<DatasetChangeListener>)datasetListener.clone();
			}
			for (DatasetChangeListener listener : toNotify) {
				if (threadId == listener.getRegisteredThreadId()) {
					listener.onMesssageStatusChanged(threadId, msgId, msgType, msgSource);
				}
			}
		} catch (Exception e) {
			if (Logger.IS_ACRA_ERROR_REPORT_ENABLED) {
				Logger.postErrorToAcraIfDebug("Caught exception in onMessageStatusChanged ", e);
			}
		}
	}
}
