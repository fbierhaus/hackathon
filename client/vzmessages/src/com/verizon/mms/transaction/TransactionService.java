/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.transaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import com.verizon.mms.MmsException;
import android.text.TextUtils;
import android.widget.Toast;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.internal.telephony.Phone;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.pdu.GenericPdu;
import com.verizon.mms.pdu.NotificationInd;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduParser;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.util.DownloadManager;
import com.verizon.mms.util.RateController;
import com.verizon.mms.util.Util;
import com.verizon.sync.SyncController;
import com.verizon.sync.VMASyncHook;


/**
 * The TransactionService of the MMS Client is responsible for handling requests to initiate
 * client-transactions sent from:
 * <ul>
 * <li>The Proxy-Relay (Through Push messages)</li>
 * <li>The composer/viewer activities of the MMS Client (Through intents)</li>
 * </ul>
 * The TransactionService runs locally in the same process as the application. It contains a HandlerThread to
 * which messages are posted from the intent-receivers of this application.
 * <p/>
 * <b>IMPORTANT</b>: This is currently the only instance in the system in which simultaneous connectivity to
 * both the mobile data network and a Wi-Fi network is allowed. This makes the code for handling network
 * connectivity somewhat different than it is in other applications. In particular, we want to be able to send
 * or receive MMS messages when a Wi-Fi connection is active (which implies that there is no connection to the
 * mobile data network). This has two main consequences:
 * <ul>
 * <li>Testing for current network connectivity ({@link android.net.NetworkInfo#isConnected()} is not
 * sufficient. Instead, the correct test is for network availability (
 * {@link android.net.NetworkInfo#isAvailable()}).</li>
 * <li>If the mobile data network is not in the connected state, but it is available, we must initiate setup
 * of the mobile data connection, and defer handling the MMS transaction until the connection is established.</li>
 * </ul>
 */
public class TransactionService extends Service implements Observer {
	/**
     * Used to identify notification intents broadcasted by the TransactionService when a Transaction is
     * completed.
	 */
	public static final String TRANSACTION_COMPLETED_ACTION = "verizon.intent.action.TRANSACTION_COMPLETED_ACTION";

	/**
	 * Action for the Intent which is sent by Alarm service to launch TransactionService.
	 */
	public static final String ACTION_ONALARM = "verizon.intent.action.ACTION_ONALARM";
	
	/**
	 * We fudge the DUE_TIME of pending messages with this extra delta (24 hours) so as to prevent the native app 
	 * from picking up these messages. 
	 */
	public static final long DELTA_TIME_TO_PREVENT_NATIVE =  86400000;
	
	/**
     * Used as extra key in notification intents broadcasted by the TransactionService when a Transaction is
     * completed (TRANSACTION_COMPLETED_ACTION intents). Allowed values for this key are:
     * TransactionState.INITIALIZED, TransactionState.SUCCESS, TransactionState.FAILED.
	 */
	public static final String STATE = "state";

	/**
     * Used as extra key in notification intents broadcasted by the TransactionService when a Transaction is
     * completed (TRANSACTION_COMPLETED_ACTION intents). Allowed values for this key are any valid content
     * uri.
	 */
	public static final String STATE_URI = "uri";

	private static final int EVENT_TRANSACTION_REQUEST = 1;
	private static final int EVENT_CONTINUE_MMS_CONNECTIVITY = 3;
	private static final int EVENT_HANDLE_NEXT_PENDING_TRANSACTION = 4;
	private static final int EVENT_NEW_INTENT = 5;
	// this event is needed so we periodically check when connectivity has been established. When using VZAPPAPN 
	// we cannot rely on the BroadcastReceiver being called
	private static final int EVENT_CHECK_CONNECTIVITY = 6; 
	private static final int EVENT_QUIT = 100;

	private static final int TOAST_MSG_QUEUED = 1;
	private static final int TOAST_DOWNLOAD_LATER = 2;
	private static final int TOAST_NONE = -1;


	// How often to extend the use of the MMS APN while a transaction
	// is still being processed. VZM MMS APN stays up for 1 min. So we renew after 45 sec
	private static final int APN_EXTENSION_WAIT = 45 * 1000;
	private static long lastToastTime = 0;

	private static final int CHECK_CONNECTIVITY_WAIT = 1 * 1000;
	private int checkConnectivityCounter = 0;
	private int startUsingNetworkCounter = 0;
	private static final int MAX_CHECK_CONNECTIVITY_COUNT = 90;
	private static final int MAX_START_USING_NETWORK_COUNT = 5;
	
	private int mUpdateInProgress = 0;
	private ServiceHandler mServiceHandler;
	private Looper mServiceLooper;
	private final ArrayList<Transaction> mProcessing = new ArrayList<Transaction>();
	private final ArrayList<Transaction> mPending = new ArrayList<Transaction>();
	private ConnectivityManager mConnMgr;
	private ConnectivityBroadcastReceiver mReceiver;

	private PowerManager.WakeLock mWakeLock;
	
	private ThreadPoolExecutor transactionExecutor;
	
	private PreviewPrefetcher mLinkPreviewPrefetcher;

	public Handler mToastHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			String str = null;

			if (msg.what == TOAST_MSG_QUEUED) {
				str = getString(R.string.message_queued);
			}
			else if (msg.what == TOAST_DOWNLOAD_LATER) {
				str = getString(R.string.download_later);
			}

			// dont show toast again if shown within last 3 mins
			if (str != null) {
				long currTime = System.currentTimeMillis();
				if ((currTime - lastToastTime) > 300000) {
					lastToastTime = currTime;				
					Toast.makeText(TransactionService.this, str, Toast.LENGTH_LONG).show();
				}
			}
		}
	};

	@Override
	public void onCreate() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onCreate");
		}
		
		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.
		HandlerThread thread = new HandlerThread("TransactionService");
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		mReceiver = new ConnectivityBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mReceiver, intentFilter);
		
		
		// transactionExecutor
		transactionExecutor = new ThreadPoolExecutor(1, // default pool size
				2, // max pool size (1 extra)
				300000, // timeout
				TimeUnit.SECONDS, // seconds
				new LinkedBlockingQueue<Runnable>(), // Requests to be queued - unbounded
				new RejectedExecutionHandler() {
					@Override
					public void rejectedExecution(Runnable runnable,
							ThreadPoolExecutor executor) {
						Logger.postErrorToAcra(
								TransactionService.class,
								"====>>>> can not process any more transactions now. active threads=",
								executor.getActiveCount()
										+ ", rejected runnable="
										+ runnable.getClass());
					}
				});

		mLinkPreviewPrefetcher = new PreviewPrefetcher(this);
	}

	// moved from Android ICS code
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onStartCommand: startId = " + startId + ", intent = " + Util.dumpIntent(intent, "  "));
		}			
		if (intent != null) {
			Message msg = mServiceHandler.obtainMessage(EVENT_NEW_INTENT);
			msg.arg1 = startId;
			msg.obj = intent;
			mServiceHandler.sendMessage(msg);
		}
		return Service.START_NOT_STICKY;
	}
	
	public void onNewIntent(Intent intent, int serviceId) {
		mConnMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		boolean noNetwork = !isNetworkAvailable();

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onNewIntent: serviceId = " + serviceId + ", noNetwork = " + noNetwork +
				", intent = " + Util.dumpIntent(intent, "  "));
		}

		// This is done to prevent the native app from picking up the pending messages. Added this back as HTC is creating it
		//PduPersister.getPduPersister(this).fixDueTimestampOfPendingMessage();
		boolean xnCompleted = TRANSACTION_COMPLETED_ACTION.equals(intent.getAction());
		
		if (ACTION_ONALARM.equals(intent.getAction()) || (intent.getExtras() == null) || xnCompleted) {
			// Scan database to find all pending operations.
			long dueTimeToCheck;
			Cursor cursor = null;
			dueTimeToCheck = System.currentTimeMillis(); //+DELTA_TIME_TO_PREVENT_NATIVE;

			if (Logger.IS_DEBUG_ENABLED) {
				Util.dumpPendingTable(this);
			}

			if (!xnCompleted) {
				cursor = PduPersister.getPduPersister(this).getPendingMessages(dueTimeToCheck);
			} else {
				boolean lastActiveXn = false;
				synchronized (mProcessing) {
					if (mProcessing.size() == 0 && mPending.size() == 0) {
						lastActiveXn = true;
					}
				}
				if (lastActiveXn) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "onNewIntent: last xnComplete so checking for pending messages");
					}
					cursor = PduPersister.getPduPersister(this).getAllPendingMessages();
					//cursor = PduPersister.getPduPersister(this).getFuturePendingMessages(dueTimeToCheck);
				} else {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "onNewIntent: xnComplete but still have mProcessing:" 
								+ mProcessing + " mPending: " + mPending);
					}
				}
			}
			if (cursor != null) {
				try {
					int count = cursor.getCount();

					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "onNewIntent: pendingMessage cursor.count = " + count);
					}

					if (count == 0) {
						RetryScheduler.setRetryAlarm(this);
						stopSelfIfIdle(serviceId);
						return;
					}

					int columnIndexOfMsgId = cursor.getColumnIndexOrThrow(PendingMessages.MSG_ID);
					int columnIndexOfMsgType = cursor.getColumnIndexOrThrow(PendingMessages.MSG_TYPE);
					
					while (cursor.moveToNext()) {
						int msgType = cursor.getInt(columnIndexOfMsgType);

						int transactionType = getTransactionType(msgType);
						if (noNetwork) {
							onNetworkUnavailable(serviceId, transactionType);
							return;
						}
						switch (transactionType) {
							case -1:
								break;
							case Transaction.RETRIEVE_TRANSACTION:
								// If it's a transiently failed transaction,
								// we should retry it in spite of current
								// downloading mode. 
								
								// Above comment is from base code. Its wrong on two grounds. First if the user has now set
								// Downloading mode to not automatic then it should be honored. Second, the base code approach
								// does not handle the case where one gets an MMS but it could not be downloaded because of say
								// no network and then when network comes (or if the phone is rebooted in the middle) the 
								// retrieval will not be automatically done. Our changes are to handle such cases.
								int failureType = cursor.getInt(cursor.getColumnIndexOrThrow(PendingMessages.ERROR_TYPE));
								if (!(DownloadManager.getInstance().isAuto()) || isPermanentFailure(failureType)) { 
//										|| (failureType == MmsSms.NO_ERROR)) {
									if (Logger.IS_DEBUG_ENABLED) {
										Logger.debug(getClass(), "onNewIntent: RETRIEVE TRANSACTION but wont try: failure:" + failureType);
									}
									break;
								} 
								if (Logger.IS_DEBUG_ENABLED) {
									Logger.debug(getClass(), "onNewIntent:  RETRIEVE TRANSACTION will try transient failure: " + failureType);
								}
								// fall-through
							default:
								Uri uri = ContentUris.withAppendedId(VZUris.getMmsUri(),
										cursor.getLong(columnIndexOfMsgId));
								TransactionBundle args = new TransactionBundle(transactionType, uri.toString());
								// FIXME: We use the same startId for all MMs.
								launchTransaction(serviceId, args, false);
								break;
						}
					}
				}
				finally {
					cursor.close();
				}
			}
			else {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "onNewIntent: null pending cursor");
				}
				RetryScheduler.setRetryAlarm(this);
				stopSelfIfIdle(serviceId);
			}
		}
		else {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onNewIntent: got intent - launching transaction");
			}
			// For launching NotificationTransaction and test purpose.
			TransactionBundle args = new TransactionBundle(intent.getExtras());
			launchTransaction(serviceId, args, noNetwork);
		}
	}

	private void stopSelfIfIdle(int startId) {
		synchronized (mProcessing) {
			if ((mUpdateInProgress == 0) && mProcessing.isEmpty() && mPending.isEmpty()) {
				// Make sure we're no longer listening for connection state changes.
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "stopSelfIfIdle: stopping");
				}

				// if MMS network is not available then dont unregister from MmsSystemEventReceiver
				if (isNetworkAvailable()) {
					MmsSystemEventReceiver.unRegisterForConnectionStateChanges(getApplicationContext());
				}
				stopSelf(startId);
			}
		}
	}

	private static boolean isPermanentFailure(int type) {
		// instead of isTransientFailure we use isPermanentFailure
		return (type >= MmsSms.ERR_TYPE_GENERIC_PERMANENT);
	}

	private boolean isNetworkAvailable() {
		NetworkInfo ni = mConnMgr.getNetworkInfo(MmsConfig.getNetworkConnectivityMode());
		return (ni == null ? false : ni.isAvailable());
	}

	private int getTransactionType(int msgType) {
		switch (msgType) {
			case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
			case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X:
				return Transaction.RETRIEVE_TRANSACTION;
			case PduHeaders.MESSAGE_TYPE_READ_REC_IND:
			case PduHeaders.MESSAGE_TYPE_READ_REC_IND_X:
				return Transaction.READREC_TRANSACTION;
			case PduHeaders.MESSAGE_TYPE_SEND_REQ:
			case PduHeaders.MESSAGE_TYPE_SEND_REQ_X:
				return Transaction.SEND_TRANSACTION;
			default:
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "getTransactionType: unrecognized msgType: " + msgType);
				}
				return -1;
		}
	}

	private void launchTransaction(int serviceId, TransactionBundle txnBundle, boolean noNetwork) {
		if (noNetwork) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "launchTransaction: no network");
			}
			onNetworkUnavailable(serviceId, txnBundle.getTransactionType());
			updateMessageStatus(txnBundle, false);  // update the UI
			return;
		}
		Message msg = mServiceHandler.obtainMessage(EVENT_TRANSACTION_REQUEST);
		msg.arg1 = serviceId;
		msg.obj = txnBundle;

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "launchTransaction: sending message = " + msg);
		}
		mServiceHandler.sendMessage(msg);
	}

	/*
	 * If a messenger is present then update it with status
	 */
	void updateMessageStatus(TransactionBundle txnBundle, boolean fatal) {
		if (txnBundle != null) {
			final Bundle messengerBundle = txnBundle.getBundle();
			if (messengerBundle != null) {
				final Messenger statusHandler = (Messenger)messengerBundle.get(TransactionBundle.STATUS_HANDLER);
				if (statusHandler != null) {
					final Message msg = Message.obtain();
					msg.arg1 = txnBundle.getTransactionType();
					msg.arg2 = fatal ? 1 : 0;
					try {
						statusHandler.send(msg);
					}
					catch (Exception e) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(e);
						}
					}
				}
			}
		}
	}

	private void onNetworkUnavailable(int serviceId, int transactionType) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onNetworkUnavailable: sid = " + serviceId + ", type = " + transactionType);
		}

		// register so that we get notified when network comes back up
		MmsSystemEventReceiver.registerForConnectionStateChanges(getApplicationContext());

		int toastType = TOAST_NONE;
		if ((transactionType == Transaction.RETRIEVE_TRANSACTION) || (transactionType == Transaction.NOTIFICATION_TRANSACTION)) {
			toastType = TOAST_DOWNLOAD_LATER;
		}
		else if (transactionType == Transaction.SEND_TRANSACTION) {
			toastType = TOAST_MSG_QUEUED;
		}
		if (toastType != TOAST_NONE) {
			mToastHandler.sendEmptyMessage(toastType);
		}
		// keep stopSelf here as we don't want to unregister the system listener
		stopSelf(serviceId);
	}

	private void onNetworkNotConnecting() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onNetworkNotConnecting: mProcessing = " + mProcessing.size() + ", mPending = " + mPending.size());
		}
		endMmsConnectivity();
		
		// register so that we get notified when network comes back up
		MmsSystemEventReceiver.registerForConnectionStateChanges(getApplicationContext());

		int transactionType = Transaction.SEND_TRANSACTION; // let send xn be the default
		int toastType = TOAST_NONE;
		if (mPending.size() > 0) {
			Transaction xn = mPending.get(0);
			if (xn != null) {
				transactionType = xn.getType();
			}
		} else if (mProcessing.size() > 0) {
			Transaction xn = mProcessing.get(0);
			if (xn != null) {
				transactionType = xn.getType();
			} 
		}
		if ((transactionType == Transaction.RETRIEVE_TRANSACTION) || (transactionType == Transaction.NOTIFICATION_TRANSACTION)) {
			toastType = TOAST_DOWNLOAD_LATER;
		}
		else if (transactionType == Transaction.SEND_TRANSACTION) {
			toastType = TOAST_MSG_QUEUED;
		}
		if (toastType != TOAST_NONE) {
			mToastHandler.sendEmptyMessage(toastType);
		}
	}

	// found network so no need to check further connectivity
	private void foundNetworkConnected() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "FOUND NETWORK - " + MmsConfig.getNetworkConnectivityMode() + "-" + 
					MmsConfig.getPhoneFeatureMms() + " took " + checkConnectivityCounter + " tries");
		}
		// cancel all checks for network
		mServiceHandler.removeMessages(EVENT_CHECK_CONNECTIVITY);
		checkConnectivityCounter = 0;
		renewMmsConnectivity();
	}



	@Override
	public void onDestroy() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onDestroy");
			boolean noNetwork = !isNetworkAvailable();

			// this is possible in no network situation
			if ((!noNetwork) && (!mPending.isEmpty() || !mProcessing.isEmpty())) {
				Logger.postErrorToAcra(getClass(), "TransactionService exiting with transaction still pending: " +  
						mPending.size() + " or processing: " + mProcessing.size());
			}
		}
		endMmsConnectivity();

		unregisterReceiver(mReceiver);

		mServiceHandler.sendEmptyMessage(EVENT_QUIT);
		
		// transactionExecutor
		if (null != transactionExecutor) {
			transactionExecutor.shutdown();
			transactionExecutor = null;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * Handle status change of Transaction (The Observable).
	 */
	public void update(Observable observable) {
		Transaction transaction = (Transaction)observable;
		int serviceId = transaction.getServiceId();
		boolean dontNeedMmsConnAnymore = true;
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "update: serviceId = " + serviceId);
		}

		try {
			synchronized (mProcessing) {
				mUpdateInProgress++;
				mProcessing.remove(transaction);
				if (mPending.size() > 0) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "update: handle next pending transaction...");
					}

					Message msg = mServiceHandler.obtainMessage(EVENT_HANDLE_NEXT_PENDING_TRANSACTION,
							transaction.getConnectionSettings());
					mServiceHandler.sendMessage(msg);
				}
			}

			Intent intent = new Intent(TRANSACTION_COMPLETED_ACTION);
			TransactionState state = transaction.getState();
			int result = state.getState();
			intent.putExtra(STATE, result);

			switch (result) {
				case TransactionState.SUCCESS:
					Uri uri = state.getContentUri();
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "update: transaction success: " + serviceId + ", uri = " + uri + 
								" type:" + transaction.getType());
					}

					intent.putExtra(STATE_URI, uri);

					// Notify user in the system-wide notification area.
					switch (transaction.getType()) {
						case Transaction.NOTIFICATION_TRANSACTION:
						case Transaction.RETRIEVE_TRANSACTION:
							// We're already in a non-UI thread called from
							// NotificationTransacation.run(), so ok to block here.
						    
							MessagingNotification.blockingUpdateNewMessageIndicator(this, true, false, uri);
							MessagingNotification.updateDownloadFailedNotification(this);
							
		                    // prefetch links and image preview
	                    	Uri contentUri = state.getContentUri();
		                    if (transaction.getType() == Transaction.NOTIFICATION_TRANSACTION) {
		                    	if (Logger.IS_DEBUG_ENABLED) {
		                    		Logger.info("TransactionService:NOTIFICATION_TRANSACTION, Uri = " + contentUri);
		                    	}

		                    	mLinkPreviewPrefetcher.processLinksInMms(contentUri);
		                    }
							
							if (Logger.IS_DEBUG_ENABLED) {
							    Logger.info("TransactionService:VMA-Hook:MMS Received:uri="+state.getContentUri());
			                }
		                    long msgId = ContentUris.parseId(state.getContentUri());
		                    ApplicationSettings.getInstance().createOrUpdateMSAMmsMapping(state.getContentUri());
		                    if(Logger.IS_DEBUG_ENABLED){
		                        Logger.debug("New MMS Message Received: Firing VMA Sync.");
		                    }
//		                    Intent vmaIntent =new Intent(SyncManager.ACTION_START_VMA_SYNC);
//		                    vmaIntent.putExtra(SyncManager.EXTRA_SYNC_TYPE, SyncManager.SYNC_ON_DEMAND);
//		                    vmaIntent.putExtra(SyncManager.EXTRA_STOPITSELF, SyncManager.SYNC_ON_DEMAND);
//		                    startService(vmaIntent);
		                    SyncController.getInstance().startVMASync(true);

		                    
							break;
						case Transaction.SEND_TRANSACTION:
							RateController.getInstance().update();
							break;
					}
					
					// if just completed a transaction successfully then see if any transactions that had failed earlier can be redone
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "Completed one transaction successfully. Seeing if others are in DB");
					}
					dontNeedMmsConnAnymore = false;
					Message msg = mServiceHandler.obtainMessage(EVENT_NEW_INTENT);
					msg.arg1 = serviceId;
					Intent newI = new Intent();
					// setting action as ACTION_ONALARM even though it is not alarm
					newI.setAction(TRANSACTION_COMPLETED_ACTION);
					msg.obj = intent;
					mServiceHandler.sendMessage(msg);
					
					break;

				case TransactionState.FAILED:
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "update: transaction failed: " + serviceId);
						Logger.info("TransactionService:Wifi-Hook:MMS Send Failed:uri="+state.getContentUri());
					}
                    final boolean fatal = state.getError() >= PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_FAILURE;
                    // we do not retry ReadRec Transaction and we do not want to generate notification for ReadRec failure
                    if (transaction.getType() != Transaction.READREC_TRANSACTION) {
                    	updateMessageStatus(transaction.getArgs(), fatal);
                    	// we will let RetryScheduler retry xn or we will let SystemMmsListener wake us up if connectivity is established
    					//dontNeedMmsConnAnymore = false;
                    }
					if(transaction.getType() == Transaction.SEND_TRANSACTION){
					    // No need to send the download failure notification
					    VMASyncHook.markMMSSendFailed(this, ContentUris.parseId(state.getContentUri()));
					} 
					break;

				default:
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "update: transaction state unknown: serviceId = " + serviceId + ", result = " + result);
					}
					break;
			}

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "update: broadcasting result = " + result);
			}
			// Broadcast the result of the transaction.
			sendBroadcast(intent);
		}
		finally {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "update: detaching transaction and unregistering systemlistener");
			}
			transaction.detach(this);
			synchronized (mProcessing) {
				mUpdateInProgress--;
			}

			// it seems safer to call stopSelfIfIdle instead of stopSelf - also it keeps unregister functionality at a common place
			//MmsSystemEventReceiver.unRegisterForConnectionStateChanges(getApplicationContext());
			//stopSelf(serviceId);
			// if onNewIntent is called from here then it will call stopSelf
			if (dontNeedMmsConnAnymore) {
				stopSelfIfIdle(serviceId);
			}
		}
	}

	private synchronized void createWakeLock() {
		// Create a new wake lock if we haven't made one yet.
		if (mWakeLock == null) {
			PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MMS Connectivity");
			mWakeLock.setReferenceCounted(false);
		}
	}

	private void acquireWakeLock() {
		// It's okay to double-acquire this because we are not using it
		// in reference-counted mode.
		mWakeLock.acquire();
	}

	private void releaseWakeLock() {
		// Don't release the wake lock if it hasn't been created and acquired.
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}

	protected int beginMmsConnectivity() throws IOException {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "asking beginMmsConnectivity - have asked CHECK_CONNECTIVITY  " + checkConnectivityCounter + " times");
		}
		// Take a wake lock so we don't fall asleep before the message is downloaded.
		createWakeLock();

		int result = -1; //default should be error and not already established (0)
		try {
			result = mConnMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, MmsConfig.getPhoneFeatureMms()); 
		} catch (NullPointerException e) {
			// should not happen
			if (Logger.IS_DEBUG_ENABLED) {
				throw new RuntimeException("beginMmsConnectivity returned NullPointerException", e);
			}
			
			if (MmsConfig.getNetworkConnectivityMode() == ConnectivityManager.TYPE_MOBILE_HIPRI) { 
				if (Logger.IS_DEBUG_ENABLED)
					Logger.debug(getClass(), "got NullPointerException and so reverting to MOBILE_MMS");
				MmsConfig.setPhoneFeatureMms(Phone.FEATURE_ENABLE_MMS);
				MmsConfig.setNetworkConnectivityMode(ConnectivityManager.TYPE_MOBILE_MMS); 
				result = mConnMgr.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, MmsConfig.getPhoneFeatureMms());
			}
		} catch (Exception e) {
			if (Logger.IS_ERROR_ENABLED) {
				Logger.error(true, getClass(), "exception while using startUsingNetwork" + e);
			}
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "beginMmsConnectivity: result = " + result);
		}

		switch (result) {
			// if request is started then we should keep the asking for MMS connectivity if we dont get it in some time period
			// this is to handle situation where we ask for MMS connectivity and then the network drops and we dont get any response
			// in that case we need to be persistent in our request!
			case Phone.APN_REQUEST_STARTED:
				// Set a timer to keep asking if we dont get the MMS connection
				renewMmsConnectivity();

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "beginMmsConnectivity: waiting for connection...");
				}

				mServiceHandler.sendMessageDelayed(mServiceHandler.obtainMessage(EVENT_CHECK_CONNECTIVITY),
						CHECK_CONNECTIVITY_WAIT);

				acquireWakeLock();
				return result;
				
			case Phone.APN_ALREADY_ACTIVE:
				foundNetworkConnected();
				acquireWakeLock();
				return result;
		}

		// someone tried to set up MMS connectivity but we got an error so before we go back we should register for system event
		// so we will be notified when connectivity is established and will restart transaction service and finish what was left off
		// Otherwise we are not notified that the network is back on
		MmsSystemEventReceiver.registerForConnectionStateChanges(getApplicationContext());

		throw new IOException("Cannot establish MMS connectivity");
	}

	protected void endMmsConnectivity() {
		synchronized (mProcessing) {
			if (!mProcessing.isEmpty()) {
				// Make sure we're no longer listening for connection state changes.
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "endMmsConnectivity - mProcessing is there so dont end");
				}
				return;
			}
		}
		
		try {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "Doing endMmsConnectivity");
			}
			// cancel timer for renewal of lease
			mServiceHandler.removeMessages(EVENT_CONTINUE_MMS_CONNECTIVITY);
			mServiceHandler.removeMessages(EVENT_CHECK_CONNECTIVITY);
			checkConnectivityCounter = 0;
			
			if (mConnMgr != null) {
				mConnMgr.stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, MmsConfig.getPhoneFeatureMms());
			}
		}
		catch (Throwable th) {
			if (Logger.IS_ERROR_ENABLED) {
				Logger.error(true, getClass(), "exception in endMmsConnnectivity" + th);
			}
		}
		finally {
			releaseWakeLock();
		}
	}

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		private String decodeMessage(Message msg) {
			if (msg.what == EVENT_QUIT) {
				return "EVENT_QUIT";
			} else if (msg.what == EVENT_CONTINUE_MMS_CONNECTIVITY) {
				return "EVENT_CONTINUE_MMS_CONNECTIVITY";
			} else if (msg.what == EVENT_TRANSACTION_REQUEST) {
				return "EVENT_TRANSACTION_REQUEST";
			} else if (msg.what == EVENT_HANDLE_NEXT_PENDING_TRANSACTION) {
				return "EVENT_HANDLE_NEXT_PENDING_TRANSACTION";
			} else if (msg.what == EVENT_NEW_INTENT)  {
				return "EVENT_NEW_INTENT";
			} else if (msg.what == EVENT_CHECK_CONNECTIVITY)  {
				return "EVENT_CHECK_CONNECTIVITY";
			}
			return "unknown message.what";
		}

		private String decodeTransactionType(int transactionType) {
			if (transactionType == Transaction.NOTIFICATION_TRANSACTION) {
				return "NOTIFICATION_TRANSACTION";
			} else if (transactionType == Transaction.RETRIEVE_TRANSACTION) {
				return "RETRIEVE_TRANSACTION";
			} else if (transactionType == Transaction.SEND_TRANSACTION) {
				return "SEND_TRANSACTION";
			} else if (transactionType == Transaction.READREC_TRANSACTION) {
				return "READREC_TRANSACTION";
			}
			return "invalid transaction type";
		}

		/**
         * Handle incoming transaction requests. The incoming requests are initiated by the MMSC Server or by
         * the MMS Client itself.
		 */
		@Override
		public void handleMessage(Message msg) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "handleMessage: msg = " + msg + ", decoded = " + decodeMessage(msg));
			}

			Transaction transaction = null;
			
			String uri = null;
			switch (msg.what) {
				case EVENT_NEW_INTENT:
					onNewIntent((Intent)msg.obj, msg.arg1);
				break;

				case EVENT_QUIT:
					synchronized (mProcessing) {
						if (mProcessing.isEmpty() && mPending.isEmpty()) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "Got EVENT_QUIT and no processing or pending so quiting");
							}
							getLooper().quit();
						}
					}
					return;

				case EVENT_CHECK_CONNECTIVITY:
					checkConnectivityCounter++;
					// check to see if network interface is up
					if (mConnMgr.getNetworkInfo(MmsConfig.getNetworkConnectivityMode()).isConnected()) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "handleMessage: have network - " + checkConnectivityCounter 
									+ " tries - processing pending transaction");
						}
						foundNetworkConnected();

						TransactionSettings settings = new TransactionSettings(TransactionService.this, null);

						mServiceHandler.processPendingTransaction(null, settings);

					} else {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "handleMessage: no network - " + checkConnectivityCounter + "tries");
						}

						if (checkConnectivityCounter <= MAX_CHECK_CONNECTIVITY_COUNT) {
							// Set a timer to check the MMS connection 
							if (!mServiceHandler.hasMessages(EVENT_CHECK_CONNECTIVITY)) {
								mServiceHandler.sendMessageDelayed(mServiceHandler.obtainMessage(EVENT_CHECK_CONNECTIVITY),
										CHECK_CONNECTIVITY_WAIT);
							}
						} else {
							// have been waiting for network to come up for a long time - put a toast and register system listener
							onNetworkNotConnecting();
						}
					}
					return;

				case EVENT_CONTINUE_MMS_CONNECTIVITY:
					// added an extra check for mPending - if mPending is not empty then we may be waiting for
					// MMS interface to become available. In case that earlier request was lost (say network became unavailable)
					// so ask for it again
					synchronized (mProcessing) {
						if (mProcessing.isEmpty() && mPending.isEmpty()) {
							return;
						}
					}

					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "handleMessage: EVENT_CONTINUE_MMS_CONNECTIVITY");
					}

					try {
						int result = beginMmsConnectivity();
						if (result != Phone.APN_ALREADY_ACTIVE) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "handleMessage: result = " + result + " instead of APN_ALREADY_ACTIVE");
							}
							// Just wait for connectivity startup without
							// any new request of APN switch.
							return;
						} else {
							startUsingNetworkCounter = 0;
						}
					}
					catch (IOException e) {
						Logger.error(false, getClass(), "handleMessage: beginMmsConnectivity failed");
						// fall through so we can try renewMmsConnectivity again. Earlier were returning from here
						// at some point if the transaction fails then it will stop renewing MMS connectivity
					}

					// Restart timer
					renewMmsConnectivity();
					return;

				case EVENT_TRANSACTION_REQUEST:
					int serviceId = msg.arg1;
					try {
						TransactionBundle args = (TransactionBundle)msg.obj;
						TransactionSettings transactionSettings;

						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "handleMessage: EVENT_TRANSACTION_REQUEST: MmscUrl = " + args.getMmscUrl() +
								", proxy port = " + args.getProxyAddress());
						}

						// Set the connection settings for this transaction.
						// If these have not been set in args, load the default settings.
						String mmsc = args.getMmscUrl();
						if (mmsc != null) {
							transactionSettings = new TransactionSettings(mmsc, args.getProxyAddress(),
									args.getProxyPort());
						}
						else {
							transactionSettings = new TransactionSettings(TransactionService.this, null);
						}

						int transactionType = args.getTransactionType();

						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "handleMessage: EVENT_TRANSACTION_REQUEST: transactionType = " + transactionType
								+ ", decoded = " + decodeTransactionType(transactionType));
						}

						// Create appropriate transaction
						switch (transactionType) {
							case Transaction.NOTIFICATION_TRANSACTION:
								uri = args.getUri();
								if (uri != null) {
									transaction = new NotificationTransaction(TransactionService.this, serviceId,
											transactionSettings, uri);
								}
								else {
									Logger.postErrorToAcra(getClass(), "TransactionService - should not be here in production code");
									// Now it's only used for test purpose.
									byte[] pushData = args.getPushData();
									PduParser parser = new PduParser(pushData);
									GenericPdu ind = parser.parse();

									int type = PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
									if ((ind != null) && (ind.getMessageType() == type)) {
										transaction = new NotificationTransaction(TransactionService.this, serviceId,
												transactionSettings, (NotificationInd)ind);
									}
									else {
										Logger.error(getClass(), "Invalid PUSH data.");
										transaction = null;
										return;
									}
								}
								break;
							case Transaction.RETRIEVE_TRANSACTION:
								uri = args.getUri();
								transaction = new RetrieveTransaction(TransactionService.this, serviceId,
										transactionSettings, uri, args);
								break;
							case Transaction.SEND_TRANSACTION:
								uri = args.getUri();
								transaction = new SendTransaction(TransactionService.this, serviceId,
										transactionSettings, uri);
								break;
							case Transaction.READREC_TRANSACTION:
								uri = args.getUri();
								transaction = new ReadRecTransaction(TransactionService.this, serviceId,
										transactionSettings, uri);
								break;
							default:
								Logger.error(getClass(), "Invalid transaction type: "+serviceId);
								transaction = null;
								return;
						}

						if (!processTransaction(transaction)) {
							transaction = null;
							return;
						}

						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "handleMessage: Transaction has been kicked off based on msg: " + msg);
						}
					}
					catch (Throwable ex) {
						if (Logger.IS_ERROR_ENABLED) {
							Logger.error(getClass(), "Exception occurred while handling message " + msg, ex);
						}

						if (ex instanceof MmsException) {
							if (uri != null) {
								if (Logger.IS_ERROR_ENABLED) {
									Logger.debug(true, getClass(), "MMS exception - deleting pending table entry: " + uri);
								}
								Transaction.deletePendingTableEntry(TransactionService.this, Uri.parse(uri));
							}
						}
						if (transaction != null) {
							try {
								transaction.detach(TransactionService.this);
								if (mProcessing.contains(transaction)) {
									synchronized (mProcessing) {
										mProcessing.remove(transaction);
									}
								}
							}
							catch (Throwable t) {
								Logger.error(getClass(), "Unexpected Throwable=", t);
							}
							finally {
								// Set transaction to null to allow stopping the
								// transaction service.
								transaction = null;
							}
						}
					}
					finally {
						if (transaction == null) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "handleMessage: transaction was null:" + serviceId);
							}
							stopSelfIfIdle(serviceId);
						}
					}
					return;
				case EVENT_HANDLE_NEXT_PENDING_TRANSACTION:
					processPendingTransaction(transaction, (TransactionSettings)msg.obj);
					return;
				default:
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "handleMessage: unknown msg");
					}
					return;
			}
		}

		public void processPendingTransaction(Transaction transaction, TransactionSettings settings) {

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "processPendingTxn: transaction="+ transaction + " mPending.size="+mPending.size());
			}

			int numProcessTransaction = 0;
			synchronized (mProcessing) {
				if (mPending.size() != 0) {
					transaction = mPending.remove(0);
				}
				numProcessTransaction = mProcessing.size();
			}

			if (transaction != null) {
				if (settings != null) {
					transaction.setConnectionSettings(settings);
				}

				/*
				 * Process deferred transaction
				 */
				try {
					int serviceId = transaction.getServiceId();

					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "processPendingTxn: "+ transaction.toString()); 
					}
					if (processTransaction(transaction)) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "processPendingTxn: started deferred processing of transaction: "+ transaction);
						}
					}
					else {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "processPendingTxn: discarding deferred processing of transaction: "+ transaction);
						}
						transaction = null;
						stopSelfIfIdle(serviceId);
					}
				}
				catch (IOException e) {
					Logger.error(getClass(), "processPendingTxn:", e);
				}
			}
			else {
				if (numProcessTransaction == 0) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "processPendingTxn: no more transaction, endMmsConnectivity");
					}
					// we do not want to end MMS connectivity here because have seen situation where there is a 
					// xn starting in parallel and this runs in the main thread
					//endMmsConnectivity();
				}
			}
		}

		/**
		 * Internal method to begin processing a transaction.
		 * 
		 * @param transaction
		 *            the transaction. Must not be {@code null}.
         * @return {@code true} if process has begun or will begin. {@code false} if the transaction should be
         *         discarded.
		 * @throws IOException
		 *             if connectivity for MMS traffic could not be established.
		 */
		private boolean processTransaction(Transaction transaction) throws IOException {
			if (transaction == null) {
				return false;
			}
			
			// Check if transaction already processing
			synchronized (mProcessing) {
				for (Transaction t : mPending) {
					try {
						if ((t != null) && (t.isEquivalent(transaction))) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "processTransaction: transaction already pending: "+ transaction.getServiceId());
							}
							return true;
						}
					} catch (Exception e) {
						Logger.error("Exception in Transaction.isEquivalent");
					}
				}
				for (Transaction t : mProcessing) {
					try {
						if ((t != null) && (t.isEquivalent(transaction))) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "processTransaction: duplicated transaction: "+ transaction.getServiceId());
							}
							return true;
						}
					} catch (Exception e) {
						Logger.error("Exception in Transaction.isEquivalent");						
					}
				}

				/*
				 * Make sure that the network connectivity necessary for MMS traffic is enabled. If it is not,
				 * we need to defer processing the transaction until connectivity is established.
				 */
                try {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "processTransaction: call beginMmsConnectivity...");
                    }
                    int connectivityResult = beginMmsConnectivity();
					if (connectivityResult == Phone.APN_REQUEST_STARTED) {
						mPending.add(transaction);
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "processTransaction: APN_REQUEST_STARTED, defer transaction pending MMS connectivity");
						}
						return true;
					}
				} catch (IOException e) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "processTransaction: IOException - not marking an error");
					}
					throw new IOException("Cannot establish MMS connectivity");
				}
					
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "processTransaction: adding transaction to 'mProcessing' list: "+ transaction);
				}
				mProcessing.add(transaction);				
			}

			// Set a timer to keep renewing our "lease" on the MMS connection - first cancel any messages in queue
			renewMmsConnectivity();
			
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "processTransaction: starting transaction "+ transaction);
			}

			// Attach to transaction and process it
			transaction.attach(TransactionService.this);
			final ThreadPoolExecutor exec = transactionExecutor;
			if (exec != null) {
				transaction.process(transactionExecutor); // Process with executor
			}
			else {
				Logger.error(getClass(), "processTransaction: shut down while processing " + transaction);
			}

			dumpExecutorState();
			return true;
		}
	}
	
	private void dumpExecutorState() {
		if (Logger.IS_DEBUG_ENABLED) {
			final ThreadPoolExecutor exec = transactionExecutor;
			Logger.debug(TransactionService.class, exec == null ? "shut down" :
				("pool = " + exec.getPoolSize() + "/" + exec.getCorePoolSize() +
				", active = " + exec.getActiveCount() + ", submittedtasks = " + exec.getTaskCount() +
				", completedtasks = " + exec.getCompletedTaskCount()));
		}
	}

	private void renewMmsConnectivity() {
		// Set a timer to keep renewing our "lease" on the MMS connection 
		// instead of removing messages and adding - better to not add if exists because then that message will fire at 
		// whatever time it was set - else it could keep getting pushed into future
		//mServiceHandler.removeMessages(EVENT_CONTINUE_MMS_CONNECTIVITY);
		if (!mServiceHandler.hasMessages(EVENT_CONTINUE_MMS_CONNECTIVITY)) {
			mServiceHandler.sendMessageDelayed(mServiceHandler.obtainMessage(EVENT_CONTINUE_MMS_CONNECTIVITY),
				APN_EXTENSION_WAIT);
		}
	}

	private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onReceive: intent = " + Util.dumpIntent(intent, "  "));
			}

			if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				return;
			}

			//boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
			
			NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "onReceive: networkInfo = "+ networkInfo);
			}

			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm == null)
				return;
			
			boolean foundConnectedMmsNetwork = false;
			
			NetworkInfo nw = cm.getNetworkInfo(MmsConfig.getNetworkConnectivityMode());
			
			if (null != nw && nw.isConnected()) {
				foundConnectedMmsNetwork = true;
			}

			if (foundConnectedMmsNetwork) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "onReceive: looking for " + MmsConfig.getNetworkConnectivityMode() +
							" and found it connected");
				}
			}
			
//			NetworkInfo[] ni = cm.getAllNetworkInfo();
//			for (int i = 0; i<ni.length; i++) {
//				if (Logger.IS_DEBUG_ENABLED)
//					Logger.debug(getClass(), "#===> MmsConnect - networkInfo:" + ni[i]);
//				if ((MmsConfig.getPhoneFeatureMms() == Phone.FEATURE_ENABLE_MMS) && 
//						(ni[i].getType() == ConnectivityManager.TYPE_MOBILE_MMS) && 
//						(ni[i].isConnected())) {
//					foundConnectedMmsNetwork = true;
//					if (Logger.IS_DEBUG_ENABLED)
//						Logger.debug(getClass(), "#===> MmsConnect - Looking for mobile_mms and found it connected");
//					
//					break;
//				}
//				
//				if ((MmsConfig.getPhoneFeatureMms() == Phone.FEATURE_ENABLE_HIPRI) && 
//						(ni[i].getType() == ConnectivityManager.TYPE_MOBILE_HIPRI) && 
//						(ni[i].isConnected())) {
//					foundConnectedMmsNetwork = true;
//					if (Logger.IS_DEBUG_ENABLED)
//						Logger.debug(getClass(), "#===> MmsConnect - Looking for mobile_hipri and found it connected");
//					break;
//				}
//			}
			

			/*
			 * If we are being informed that connectivity has been established to allow MMS traffic, then
			 * proceed with processing the pending transaction, if any.
			 */
			if (!foundConnectedMmsNetwork) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "onReceive: no connected network: mode is: " + MmsConfig.getPhoneFeatureMms());
				}
				// This is a very specific fix to handle the case where the phone receives an
				// incoming call during the time we're trying to setup the mms connection.
				// When the call ends, restart the process of mms connectivity.
				
				// We need a more general case to ask for MMS again. If the network got disconnected and is brought back now
				//if (networkInfo != null && Phone.REASON_VOICE_CALL_ENDED.equals(networkInfo.getReason())) {
				//	if (Logger.IS_DEBUG_ENABLED)
				//		Logger.debug(getClass(), "MmsConnect - retrying mms connectivity. reason is: "+ Phone.REASON_VOICE_CALL_ENDED);
				//	renewMmsConnectivity();
				//}
				synchronized (mProcessing) {
					if (!mProcessing.isEmpty() || !mPending.isEmpty()) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "onReceive: retrying mms connectivity: mProcessing = " +
								mProcessing.size() + ", mPending = " + mPending.size());
						}
						renewMmsConnectivity();
					}
				}
				return;
			}

			TransactionSettings settings = new TransactionSettings(TransactionService.this, networkInfo.getExtraInfo());

			// If this APN doesn't have an MMSC, wait for one that does.
			if (TextUtils.isEmpty(settings.getMmscUrl())) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "onReceive: empty MMSC url, bail");
				}
				return;
			}

			// remove if any messages are going on to check for connectivity because we already have connectivity
			foundNetworkConnected();
			
			mServiceHandler.processPendingTransaction(null, settings);
		}
	};
}