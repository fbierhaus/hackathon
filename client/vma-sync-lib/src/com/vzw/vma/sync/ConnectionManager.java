/**
 * ConnectionManager.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.sync.SyncManager;

/**
 * This class/interface
 * 
 * @author Jegadeesan M
 * @Since Nov 13, 2012
 */
public class ConnectionManager {

	private Context context;
	private ArrayList<DataConnectivityListener> listeners;
	private ConnectivityMonitor receiver;
	private ConnectivityManager dataConnectivity;
	private AppSettings settings;
	private boolean isShutdown = false;

	public ConnectionManager(AppSettings settings) {
		this.context = settings.getContext();
		this.settings = settings;
		listeners = new ArrayList<DataConnectivityListener>();
		receiver = new ConnectivityMonitor();
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(SyncManager.ACTION_UPDATE_SETTINGS);
		context.registerReceiver(receiver, filter);
		dataConnectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		isShutdown = false;
	}

	public void shutdown() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "shutdown() called - " + isShutdown);
		}
		synchronized(this) {
			if(!isShutdown) {
				listeners.clear();
				context.unregisterReceiver(receiver);
				isShutdown = true;
			} else {
				Logger.error("Shutdown called twice");
			}
		}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "shutdown() done - " + isShutdown);
		}
	}

	private void updateCurrentConnectionStatus(DataConnectivityListener listener) {

		NetworkInfo appropriateNetwork = isAppropriateNetworkAvaliable();
		boolean hasNetwork;
		int networkType;
		if(appropriateNetwork != null) {
			hasNetwork = true;
			networkType = appropriateNetwork.getType();
		} else {
			hasNetwork = false;
			networkType = -1;
		}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("ConnectivityMonitor :updateCurrentConnectionStatus=" + hasNetwork);
		}
		if(listener != null) {
			listener.onConnectionChanged(hasNetwork, networkType);
		}

	}

	public void addListener(DataConnectivityListener listener) {
		synchronized(this) {
			if (!listeners.contains(listener)) {
				listeners.add(listener);
				updateCurrentConnectionStatus(listener);
			}
		}
	}

	public void removeListener(DataConnectivityListener listener) {
		synchronized(this) {
			listeners.remove(listener);
		}
	}

	final class ConnectivityMonitor extends BroadcastReceiver {

		/*
		 * Overriding method (non-Javadoc)
		 * 
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("ConnectivityMonitor :" + intent.getAction());
			}
			boolean hasNetwork = false;
			if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
				NetworkInfo appropriateNetwork = isAppropriateNetworkAvaliable();
				int networkType = -1;
				if(appropriateNetwork != null) {
					hasNetwork = true;
					networkType = appropriateNetwork.getType();
				} else {
					hasNetwork = false;
					networkType = -1;
				}

				synchronized(ConnectionManager.this) {
					if (!listeners.isEmpty()) {
						for (DataConnectivityListener listener : listeners) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug("ConnectivityMonitor :isNetworkAvaliable=" + hasNetwork + ",type="+ networkType);
							}
							listener.onConnectionChanged(hasNetwork, networkType);
						}
					}
				}

			} else if (SyncManager.ACTION_UPDATE_SETTINGS.equals(intent.getAction())) {
				if (!listeners.isEmpty()) {
					NetworkInfo appropriateNetwork = isAppropriateNetworkAvaliable();
					int networkType = -1;
					if(appropriateNetwork != null) {
						hasNetwork = true;
						networkType = appropriateNetwork.getType();
					} else {
						hasNetwork = false;
						networkType = -1;
					}
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug("ConnectivityMonitor :dataConnection=" + hasNetwork);
					}
					synchronized(ConnectionManager.this) {
						if (!listeners.isEmpty()) {
							for (DataConnectivityListener listener : listeners) {
								if (Logger.IS_DEBUG_ENABLED) {
									Logger.debug("ConnectivityMonitor :isNetworkAvaliable=" + hasNetwork + ",type=" + networkType);
								}
								listener.onConnectionChanged(hasNetwork, networkType);
							}
						}
					}
				}
			}

		}

	}

	private boolean isWifi(NetworkInfo in) {
		return in.getType() == ConnectivityManager.TYPE_WIFI;
	}

	private boolean isMobileOrWifi(NetworkInfo in) {
		return (in.getType() == ConnectivityManager.TYPE_WIFI || in.getType() == ConnectivityManager.TYPE_MOBILE);
	}    

	private NetworkInfo isNetworkAvailable(boolean needWifi) {

		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("Connectivity change, finding active network : wifirequired = " + needWifi);
		}
		NetworkInfo[] infos = dataConnectivity.getAllNetworkInfo();
		if(infos != null) {
			for(NetworkInfo in : infos) {
				if(isMobileOrWifi(in) && in.isAvailable() && in.isConnected()) {
					if(Logger.IS_DEBUG_ENABLED) {
						Logger.debug("isNetworkAvailable: wifi/mobile network available : " + in);
					}		
					if(!needWifi || isWifi(in)) {
						if(Logger.IS_DEBUG_ENABLED) {
							Logger.debug("isNetworkAvailable: appropriate network available : " + in);
						}
						return in;
					} 
				}
			}
		}
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("isNetworkAvailable: returning : no valid network");
		}
		return null;
	}
	/**
	 * This Method
	 * 
	 * @param networkInfo
	 * @return
	 */
	protected NetworkInfo isAppropriateNetworkAvaliable() {
		return isNetworkAvailable(settings.isSyncOverWifiEnabled());
	}
}
