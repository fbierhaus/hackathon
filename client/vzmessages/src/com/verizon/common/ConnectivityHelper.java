package com.verizon.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;

import com.strumsoft.android.commons.logger.Logger;

public class ConnectivityHelper {
	// Make it empty after debugging
	private static final String TAG = ">>>>>>>>>>>>>> ConnectivityHelper >>>>>>>>>>>>>> ";

	public static final int EVENT_ON_CONNECT = 1;
	public static final int EVENT_ON_DISCONNECT = 2;
	public static final int EVENT_ON_CONNECT_START = 3;

	private ConnectivityManager mConnMgr;
	private ConnectivityBroadcastReceiver mReceiver;
	private Handler mHandler;
	private Context mContext;

	private final int networkType;
	private final String feature;

	public ConnectivityHelper(Context ctx, Handler handler, int networkType,
			String feature) {
		this.mContext = ctx;
		this.mHandler = handler;
		this.networkType = networkType;
		this.feature = feature;
	}

	public int beginConnectivity() {
		// Receiver
		mReceiver = new ConnectivityBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mContext.registerReceiver(mReceiver, intentFilter);

		// Connection Manager
		mConnMgr = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return mConnMgr.startUsingNetworkFeature(networkType, feature);
	}

	public void endConnectivity(int networkType, String feature) {
		if (null != mReceiver) {
//			mContext.unregisterReceiver(mReceiver);
		}

		if (null != mConnMgr) {
			mConnMgr.stopUsingNetworkFeature(networkType, feature);

			// Send Message
			Message msg = mHandler.obtainMessage(EVENT_ON_DISCONNECT);
			mHandler.sendMessage(msg);
		}
	}

	private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(ConnectivityBroadcastReceiver.class, TAG + "onReceive() action=" + action);
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
                    Logger.debug(ConnectivityBroadcastReceiver.class, TAG + "networkInfo=" + networkInfo);
                }
				return;
			}

			sendNetworkEstablishedEvent(networkInfo);
		}
	};

	public boolean checkIfConnected() {
		boolean connected = mConnMgr.getNetworkInfo(networkType).isConnected();
		// send event if connected?
		if (connected) {
			sendNetworkEstablishedEvent(mConnMgr.getNetworkInfo(networkType));
		}
		return connected;
	}

	private void sendNetworkEstablishedEvent(NetworkInfo networkInfo) {
		// Send Message
		Message msg = mHandler.obtainMessage(EVENT_ON_CONNECT);
		msg.obj = networkInfo;
		mHandler.sendMessage(msg);
	}
}