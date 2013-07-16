package com.verizon.report;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.util.EntityUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.util.Util;
import com.verizon.report.http.HttpManager;
import com.vzw.anm.ANMLibrary;

/**
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * 
 */
public class ReportService extends Service {
	private static final String PREFS_NAME = "vzm-report-prefs";

	private static final int EVENT_CONNECTED = 1;
	private static final int EVENT_DISCONNECTED = 2;
	private static final int EVENT_NEW_INTENT = 3;
	private static final int EVENT_REPORT_SUCCESS = 4;
	private static final int EVENT_REPORT_FAILURE = 5;
	private static final int EVENT_CHECK_CONNECTIVITY = 6;
	private static final int EVENT_QUIT = 100;

	private static final int CHECK_CONNECTIVITY_WAIT = 1 * 5000;
	private static final int MAX_CHECK_CONNECTIVITY_COUNT = 20;

	private ServiceHandler mServiceHandler;
	private Looper mServiceLooper;

	private ConnectivityManager mConnMgr;
	private ConnectivityBroadcastReceiver mReceiver;

	// use connectivity mode as set by MmsConfig
	private final int networkType = MmsConfig.getNetworkConnectivityMode(); 
	private final String phoneFeature = MmsConfig.getPhoneFeatureMms();

	private AtomicBoolean inProgress = new AtomicBoolean(false);
	private Context ctx;

	private final class ServiceHandler extends Handler {
		private int checkConnectionCount = 0;

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
			} else if (msg.what == EVENT_REPORT_FAILURE) {
				return "EVENT_REPORT_FAILURE";
			} else if (msg.what == EVENT_REPORT_SUCCESS) {
				return "EVENT_REPORT_SUCCESS";
			}
			return "unknown message.what";
		}

		@Override
		public void handleMessage(Message msg) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "==>> handleMessage: msg = " + msg
						+ ", decoded = " + decodeMessage(msg));
			}

			switch (msg.what) {
			case EVENT_QUIT:
				getLooper().quit();
				return;

			case EVENT_NEW_INTENT:
				// 1. Has report sent for this month? ==> return
				// 2. Is report being sent? ==> return
				// 3. If not ==> sent now
				if (!MmsConfig.isLTE()) {
					stopSelf(msg.arg1);
					return;
				}
				
				if (inProgress.get()) {
					stopSelf(msg.arg1);
					return;
				}

				if (hasReportSentForThisMonth()) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(ReportService.class,
								"==>> report is already sent for this month.");

					}
					stopSelf(msg.arg1);
					return;
				}

				inProgress.set(onNewIntent((Intent) msg.obj, msg.arg1));
				break;
			case EVENT_CHECK_CONNECTIVITY:
				if (isNetworkConnected()) {
					// Send connected event
					mServiceHandler.sendMessage(mServiceHandler.obtainMessage(
							EVENT_CONNECTED, msg.arg1));
				} else {

					checkConnectionCount++;
					if (checkConnectionCount > MAX_CHECK_CONNECTIVITY_COUNT) {
						// Assume failure
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(ReportService.class,
									"==>> Checked connectivity for "
											+ checkConnectionCount
											+ " times. Timing out now.");
						}
						// Send failure event
						mServiceHandler.sendMessage(mServiceHandler
								.obtainMessage(EVENT_REPORT_FAILURE, msg.arg1));
					} else {

						// Set a timer to check connectivity
						mServiceHandler.sendMessageDelayed(mServiceHandler
								.obtainMessage(EVENT_CHECK_CONNECTIVITY,
										msg.arg1), CHECK_CONNECTIVITY_WAIT);
					}
				}
				break;

			case EVENT_CONNECTED:

				// cancel timer
				mServiceHandler.removeMessages(EVENT_CHECK_CONNECTIVITY);

				// unregister broadcast listeners
				ReportSystemEventReceiver
						.unRegisterForConnectionStateChanges(getApplicationContext());
				unregisterForConnectivityStateChanges();

				// Report already sent?
				if (hasReportSentForThisMonth()) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(ReportService.class,
								"==>> report already sent for this month.");
					}
					return;
				}

				// Submit report
				boolean success = false;
				try {
					success = submitReport();
				} catch (IOException e) {
					if (Logger.IS_ERROR_ENABLED) {
						Logger.error(ReportService.class, "====>> error", e);
					}
				}
				if (success) {
					// Send success event
					mServiceHandler.sendMessage(mServiceHandler.obtainMessage(
							EVENT_REPORT_SUCCESS, msg.arg1));
				} else {
					// Send failure event
					mServiceHandler.sendMessage(mServiceHandler.obtainMessage(
							EVENT_REPORT_FAILURE, msg.arg1));
				}

				break;

			case EVENT_DISCONNECTED:
				inProgress.set(false);
				// Stop now
				stopSelf(msg.arg1);

				break;

			case EVENT_REPORT_SUCCESS:
				// Set flag
				setReportSentForThisMonthToTrue();
				// Schedule for next month
				scheduleNextMonth();
				// stop connectivity
				endConnectivity(msg.arg1);
				break;

			case EVENT_REPORT_FAILURE:
				// retry :(
				scheduleRetry();
				// stop connectivity
				endConnectivity(msg.arg1);
				break;
			}
		}
	}

	private boolean onNewIntent(Intent intent, int serviceId) {
		mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		boolean noNetwork = !isNetworkAvailable();

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(
					getClass(),
					"onNewIntent: serviceId = " + serviceId + ", noNetwork = "
							+ noNetwork + ", intent = "
							+ Util.dumpIntent(intent, "  "));
		}

		if (noNetwork) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "==>> network unavailable: sid = "
						+ serviceId);
			}
			ReportSystemEventReceiver
					.registerForConnectionStateChanges(getApplicationContext());
			return false;
		} else {
			// begin connectivity
			try {
				//crashes on some device LG VS950
				beginConnectivity(serviceId);
			} catch (Throwable th) {
				if (Logger.IS_ERROR_ENABLED) {
					Logger.error(true, getClass(), "beginConnectivity: error = ", th);
				}
				return false;
			}
			return true;
		}
	}

	private void registerForConnectivityStateChanges(int serviceId) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==> Registering for connectivity changes");
		}

		if (null != mReceiver) {
			ctx.unregisterReceiver(mReceiver);
			mReceiver = new ConnectivityBroadcastReceiver(serviceId);
		}

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		ctx.registerReceiver(mReceiver, intentFilter);
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

	private boolean isNetworkAvailable() {
		NetworkInfo ni = mConnMgr.getNetworkInfo(networkType);
		return (ni == null ? false : ni.isAvailable());
	}

	private boolean isNetworkConnected() {
		NetworkInfo ni = mConnMgr.getNetworkInfo(networkType);
		return (ni == null ? false : ni.isConnected());
	}

	private int beginConnectivity(int serviceId) {
		registerForConnectivityStateChanges(serviceId);
		int result = mConnMgr.startUsingNetworkFeature(networkType,
					phoneFeature);
	
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==>> beginConnectivity - asking for: " + networkType + "-" + phoneFeature + ": result = "
					+ result);
		}

		// Set a timer to check connectivity
		mServiceHandler.sendMessageDelayed(mServiceHandler.obtainMessage(
				EVENT_CHECK_CONNECTIVITY, serviceId), CHECK_CONNECTIVITY_WAIT);

		return result;			
	}

	private void endConnectivity(int serviceId) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==>> endConnectivity");
		}

		// cancel timer
		mServiceHandler.removeMessages(EVENT_CHECK_CONNECTIVITY);

		try {
			// Stop network
			if (mConnMgr != null) {
				mConnMgr.stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
						MmsConfig.getPhoneFeatureMms());
			}
		} catch (Throwable th) {
			if (Logger.IS_ERROR_ENABLED) {
				Logger.error(true, getClass(), "exception in stopUsingNetworkFeature:"  + th);
			}
		}
		// Send disconnected event
		mServiceHandler.sendMessage(mServiceHandler.obtainMessage(
				EVENT_DISCONNECTED, serviceId));
	}

	@Override
	public void onCreate() {
		super.onCreate();
		ctx = this;

		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.
		HandlerThread thread = new HandlerThread("ReportService");
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(ReportService.class, "***********************");
			Logger.debug(ReportService.class, "==>> onCreate()");
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
			Message msg = mServiceHandler.obtainMessage(EVENT_NEW_INTENT);
			msg.arg1 = startId;
			msg.obj = intent;
			mServiceHandler.sendMessage(msg);
		}
		return Service.START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterForConnectivityStateChanges();
		mServiceHandler.sendEmptyMessage(EVENT_QUIT);
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(ReportService.class, "==>> onDestroy()");
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void scheduleRetry() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==>> scheduleRetry()");
		}

		// Schedule randomly between next 2 hours ==> 120 mins.
		int min = 1, max = 120;
		int ramdomMinute = new Random().nextInt(max - min + 1) + min;

		// Calendar
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, ramdomMinute);

		// schedule
		schedule(getApplicationContext(), cal);
	}

	private void scheduleNextMonth() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==>> scheduleNextMonth()");
		}

		// Next month
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, 1);
		cal.set(Calendar.DAY_OF_MONTH, 1);

		// Add random days between 10 days.
		int min = 2, max = 10;
		int randonDay = new Random().nextInt(max - min + 1) + min;
		cal.add(Calendar.DAY_OF_MONTH, randonDay);

		// Add random min between next 2 hours
		min = 1;
		max = 120;
		int ramdomMinute = new Random().nextInt(max - min + 1) + min;
		cal.add(Calendar.MINUTE, ramdomMinute);

		// schedule
		schedule(getApplicationContext(), cal);
	}

	public static void schedule(Context context, Calendar cal) {
		// Intent
		final int request_code = 192837;
		PendingIntent intent = PendingIntent.getBroadcast(context,
				request_code, new Intent(context, ReportAlarmReceiver.class),
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Set Alarm
		AlarmManager am = (AlarmManager) context
				.getSystemService(Service.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), intent);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(ReportService.class,
					"==>> ReportService scheduled at = " + cal.getTime());
		}
	}

	private boolean hasReportSentForThisMonth() {
		String key = getKeyForCurrentMonth();
		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		boolean sent = prefs.getBoolean(key, false);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "==>> hasReportSentForThisMonth key="
					+ key + ", value=" + sent);
		}
		return sent;
		// return false;
	}

	private void setReportSentForThisMonthToTrue() {
		String key = getKeyForCurrentMonth();
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(),
					"==>> setReportSentForThisMonthToTrue() ==> key=" + key);
		}

		SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(key, true);
		editor.commit();
	}

	private String getKeyForCurrentMonth() {
		Calendar cal = Calendar.getInstance();
		return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH);
	}

	private boolean submitReport() throws IOException {
		boolean success = false;
		HttpGet httpGet = new HttpGet(getApiUrl());
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(ReportService.class, "==>> [request] GET => url="
					+ httpGet.getURI());
		}

		HttpResponse response = HttpManager.execute(httpGet);

		StatusLine status = response.getStatusLine();
		String responseBody = null;
		HttpEntity entity = null;
		HttpEntity temp = response.getEntity();
		if (temp != null && status != null) {
			entity = new BufferedHttpEntity(temp);
			responseBody = EntityUtils.toString(entity);
			if (status.getStatusCode() == 200
					&& responseBody.equalsIgnoreCase("{\"OK\"}")) {
				success = true;
			}
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(ReportService.class, "==> [response] status=" + status
					+ ", body=" + responseBody);
			Logger.debug(ReportService.class, "==> submitReport ==> success="
					+ success);
		}

		return success;
	}

	private String getApiUrl() throws UnsupportedEncodingException {
		// ?make={1}&amp;model={2}&amp;mdn={3}&amp;version={4}
		String fmt;
		if (Logger.IS_DEBUG_ENABLED) {
			fmt = (String) getResources().getText(R.string.vz_report_qa_api_url);						
		} else {
			fmt = (String) getResources().getText(R.string.vz_report_api_url);			
		}
		String uri = MessageFormat.format(fmt,
				URLEncoder.encode(getMake(), "UTF-8"), // make
				URLEncoder.encode(getModel(), "UTF-8"), // model
				URLEncoder.encode(getMdn(), "UTF-8"), // mdn
				URLEncoder.encode(getAppVersion(), "UTF-8") // version
				);
		return uri;
	}

	private String getMake() {
		return DeviceConfig.OEM.deviceManufacturer;
	}

	private String getModel() {
		return DeviceConfig.OEM.deviceModel;
	}

	private String getMdn() {
		try {
			String tok = ANMLibrary.getToken(ctx);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "Token to be passed for reporting: " + tok);
			}
			return tok;
		} catch (Exception e) {
			if (Logger.IS_ERROR_ENABLED) {
				Logger.error(ReportService.class, e);
			}
			return "unknown";
		}
	}

	private String getAppVersion() {
		return MmsConfig.BUILD_VERSION;
	}

	private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
		private int serviceId;

		public ConnectivityBroadcastReceiver(int serviceId) {
			this.serviceId = serviceId;
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

			// Check availability of the mobile network.
			if ((networkInfo == null || !networkInfo.isConnected())
					|| (networkInfo.getType() != networkType)) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(ConnectivityBroadcastReceiver.class,
							"==>> networkInfo=" + networkInfo);
				}
				return;
			}

			// Send connect event
			mServiceHandler.sendMessage(mServiceHandler.obtainMessage(
					EVENT_CONNECTED, serviceId));
		}
	};

}