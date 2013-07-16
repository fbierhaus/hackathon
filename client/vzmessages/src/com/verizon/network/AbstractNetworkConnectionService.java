package com.verizon.network;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.util.Util;

/**
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * 
 */
public abstract class AbstractNetworkConnectionService extends Service {
	public static enum ErrorType {
		NO_NETWORK, CONNECTION_FAILURE, EXECUTE_FAILURE
	}

	private static final int EVENT_QUIT = 101;
	private static final int EVENT_NEW_INTENT = 104;
	private static final int EVENT_CHECK_CONNECTIVITY = 105;
	private static final int EVENT_CONNECTED = 102;
	private static final int EVENT_DISCONNECTED = 103;
	private static final int EVENT_CONNECT_FAILURE = 106;

	private static final int CHECK_CONNECTIVITY_WAIT = 1 * 5000;
	private static final int MAX_CHECK_CONNECTIVITY_COUNT = 5;

	private ServiceHandler mServiceHandler;
	private Looper mServiceLooper;

	private ConnectivityManager mConnMgr;
	private ConnectivityBroadcastReceiver mReceiver;

	protected Context ctx;

	/**
	 * The Class ServiceHandler.
	 */
	private final class ServiceHandler extends Handler {
		// private int checkConnectionCount = 0;

		public ServiceHandler(Looper looper) {
			super(looper);
		}

		private String decodeMessage(Message msg) {
			if (msg.what == EVENT_QUIT) {
				return "EVENT_QUIT";
			} else if (msg.what == EVENT_CHECK_CONNECTIVITY) {
				return "EVENT_CHECK_CONNECTIVITY";
			} else if (msg.what == EVENT_CONNECTED) {
				return "EVENT_CONNECTED";
			} else if (msg.what == EVENT_DISCONNECTED) {
				return "EVENT_DISCONNECTED";
			} else if (msg.what == EVENT_NEW_INTENT) {
				return "EVENT_NEW_INTENT";
			} else if (msg.what == EVENT_CONNECT_FAILURE) {
				return "EVENT_CONNECT_FAILURE";
			}
			return "EVENT_UNKNOWN";
		}

		@Override
		public void handleMessage(Message msg) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(),
						"==>> handleMessage: " + decodeMessage(msg)
								+ ", msg = " + Util.dumpMessage(msg));
			}

			switch (msg.what) {
			// quit?
			case EVENT_QUIT:
				getLooper().quit();
				return;

			case EVENT_NEW_INTENT:
				// Begin using n/w feature
				beginConnectivity(msg);

				// send a delayed message to check for connectivity

				// Set a timer to check connectivity
				mServiceHandler.sendMessage(copyMessage(msg,
						EVENT_CHECK_CONNECTIVITY));
				break;

			// check connection
			case EVENT_CHECK_CONNECTIVITY:
				// is connected?
				if (isNetworkConnected()) {
					// Send connected event
					mServiceHandler.sendMessage(copyMessage(msg,
							EVENT_CONNECTED));
				}
				// Not yet connected?
				else {
					int counter = getCheckIntervalCounter(msg);
					setCheckIntervalCounter(msg, (counter + 1));
					// checkConnectionCount++;
					if (counter > checkConnectionMaxCount()) {
						// Assume failure
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(),
									"==>> Checked connectivity for " + counter
											+ " times. Timing out now.");
						}
						// Send failure event
						mServiceHandler.sendMessage(copyMessage(msg,
								EVENT_CONNECT_FAILURE));
					} else {
						// Set a timer to check connectivity
						mServiceHandler.sendMessageDelayed(
								copyMessage(msg, EVENT_CHECK_CONNECTIVITY),
								checkConnectionInterval());
					}
				}
				break;

			case EVENT_CONNECTED:
				// unregister broadcast listeners
				// ReportSystemEventReceiver.unRegisterForConnectionStateChanges(getApplicationContext());
				unregisterForConnectivityStateChanges();

				// Execute
				boolean success = false;
				Throwable error = null;
				try {
					success = execute(getStartId(msg), msg.getData());
				} catch (Exception e) {
					error = e;
					if (Logger.IS_ERROR_ENABLED) {
						Logger.error(getClass(), "====>> error", e);
					}
				}
				if (success) {
					onExecuteSuccess(getStartId(msg), msg.getData());
				} else {
					onExecuteFailure(ErrorType.EXECUTE_FAILURE, error,
							getStartId(msg), msg.getData());
				}

				// stop service now
				stopSelf(getStartId(msg));
				break;

			case EVENT_DISCONNECTED:
				// Stop now
				// stopSelf(getStartId(msg));
				break;

			case EVENT_CONNECT_FAILURE:
				// stop connectivity
				onExecuteFailure(ErrorType.CONNECTION_FAILURE, null,
						getStartId(msg), msg.getData());
				if (mServiceHandler.hasMessages(EVENT_CHECK_CONNECTIVITY)
						|| mServiceHandler.hasMessages(EVENT_CONNECTED)
						|| mServiceHandler.hasMessages(EVENT_NEW_INTENT)) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(
								getClass(),
								"==> deferring to end connectivity. looks like there are some pending messages to be processed!");
					}
				} else {
					endConnectivity(getStartId(msg));
				}

				// stop service now
				stopSelf(getStartId(msg));

				break;
			}
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		ctx = this;
		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.
		HandlerThread thread = new HandlerThread(getServiceName());
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		if (Logger.IS_DEBUG_ENABLED) {

			Logger.debug(getClass(), "==>> onCreate()");
		}
	}

	@Override
	public int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==>> onStartCommand: startId = "
					+ startId + ", intent = " + Util.dumpIntent(intent, "  "));
		}

		if (intent != null) {

			Bundle bundle = intent.getExtras();
			if (preExecute(startId, bundle)) {
				start(startId, bundle);
			}
		}
		return Service.START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterForConnectivityStateChanges();
		mServiceHandler.sendEmptyMessage(EVENT_QUIT);
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==>> onDestroy()");
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public boolean isNetworkAvailable() {
		NetworkInfo ni = mConnMgr.getNetworkInfo(getNetworkType());
		return (ni == null ? false : ni.isAvailable());
	}

	public boolean isNetworkConnected() {
		NetworkInfo ni = mConnMgr.getNetworkInfo(getNetworkType());
		return (ni == null ? false : ni.isConnected());
	}

	private void unregisterForConnectivityStateChanges() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(),
					"==> Unregistering for connectivity changes");
		}
		if (null != mReceiver) {
			unregisterReceiver(mReceiver);
		}
	}

	private int beginConnectivity(Message msg) {
		// register for connectivity state change
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==> Registering for connectivity changes");
		}

		if (null != mReceiver) {
			ctx.unregisterReceiver(mReceiver);
			mReceiver = new ConnectivityBroadcastReceiver(copyMessage(msg,
					EVENT_CONNECTED));
		}

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		ctx.registerReceiver(mReceiver, intentFilter);

		int result = mConnMgr.startUsingNetworkFeature(getNetworkType(),
				getPhoneFeature());

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(),
					"==>> beginConnectivity - looking for: type="
							+ getNetworkType() + ", feature="
							+ getPhoneFeature() + ": result = " + result);
		}
		return result;
	}

	private void endConnectivity(int serviceId) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==>> endConnectivity");
		}

		// cancel timer
		// mServiceHandler.removeMessages(EVENT_CHECK_CONNECTIVITY);

		// Stop network
		if (mConnMgr != null) {
			mConnMgr.stopUsingNetworkFeature(getNetworkType(),
					getPhoneFeature());
		}

		// Send disconnected event
		mServiceHandler.sendMessage(mServiceHandler.obtainMessage(
				EVENT_DISCONNECTED, serviceId));
	}

	public boolean start(int startId, Bundle bundle) {
		mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		boolean noNetwork = !isNetworkAvailable();

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "start: startId = " + startId
					+ ", noNetwork = " + noNetwork + ", Bundle = " + bundle);
		}

		if (noNetwork) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "==>> network unavailable: sid = "
						+ startId);
			}
			// ReportSystemEventReceiver.registerForConnectionStateChanges(getApplicationContext());
			onExecuteFailure(ErrorType.NO_NETWORK, null, startId, bundle);
			return false;
		} else {
			// begin connectivity
			Message msg = mServiceHandler.obtainMessage(EVENT_NEW_INTENT);
			msg.setData(bundle);
			setStartId(msg, startId);
			setCheckIntervalCounter(msg, 0);
			mServiceHandler.sendMessage(msg);
			return true;
		}
	}

	private Message copyMessage(Message orig, int what) {
		Message msg = Message.obtain(orig);
		msg.what = what;
		msg.setData(orig.getData());
		return msg;
	}

	private int getStartId(Message msg) {
		return msg.arg1;
	}

	private void setStartId(Message msg, int startId) {
		msg.arg1 = startId;
	}

	private int getCheckIntervalCounter(Message msg) {
		return msg.arg2;
	}

	private void setCheckIntervalCounter(Message msg, int counter) {
		msg.arg2 = counter;
	}

	public String getServiceName() {
		return getClass().getName();
	}

	public int checkConnectionInterval() {
		return CHECK_CONNECTIVITY_WAIT;
	}

	public int checkConnectionMaxCount() {
		return MAX_CHECK_CONNECTIVITY_COUNT;
	}

	public abstract String getPhoneFeature();

	public abstract int getNetworkType();

	/**
	 * Runs on UI thread
	 * 
	 * @param bundle
	 * @return false: stop processing
	 */
	public abstract boolean preExecute(int startId, Bundle bundle);

	/**
	 * Runs on worker thread
	 * 
	 * @param bundle
	 * @return false: error in processing
	 * @throws Exception
	 */
	public abstract boolean execute(int startId, Bundle bundle)
			throws Exception;

	/**
	 * Runs on worker thread
	 * 
	 * @param bundle
	 */
	public abstract void onExecuteFailure(ErrorType type, Throwable th,
			int startId, Bundle bundle);

	/**
	 * Runs on worker thread
	 * 
	 * @param bundle
	 */
	public abstract void onExecuteSuccess(int startId, Bundle bundle);

	private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
		private Message msg;

		public ConnectivityBroadcastReceiver(Message msg) {
			this.msg = msg;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(ConnectivityBroadcastReceiver.class,
						"!! ==>> onReceive() action=" + action);
			}

			if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				return;
			}

			NetworkInfo networkInfo = (NetworkInfo) intent
					.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

			// Check availability of the network.
			if ((networkInfo == null || !networkInfo.isConnected())
					|| (networkInfo.getType() != getNetworkType())) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(ConnectivityBroadcastReceiver.class,
							"==>> networkInfo=" + networkInfo);
				}
				return;
			}

			// Send connect event
			mServiceHandler.sendMessage(msg);
		}
	};

}