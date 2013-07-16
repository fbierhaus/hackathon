/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.verizon.mms.util;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.telephony.ServiceState;
import android.widget.Toast;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.internal.telephony.TelephonyIntents;
import com.verizon.internal.telephony.TelephonyProperties;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsException;
import com.verizon.mms.pdu.NotificationInd;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.ui.AdvancePreferenceActivity;

public class DownloadManager {
    private static final int DEFERRED_MASK           = 0x04;

    public static final int STATE_UNSTARTED         = 0x80;
    public static final int STATE_DOWNLOADING       = 0x81;
    public static final int STATE_TRANSIENT_FAILURE = 0x82;
    public static final int STATE_PERMANENT_FAILURE = 0x87;

    private final Context mContext;
    private final Handler mHandler;
    private final SharedPreferences mPreferences;
    private boolean mAutoDownload;

    private final OnSharedPreferenceChangeListener mPreferencesChangeListener =
        new OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (AdvancePreferenceActivity.AUTO_RETRIEVAL.equals(key)
                    || AdvancePreferenceActivity.RETRIEVAL_DURING_ROAMING.equals(key)) {
               		if (Logger.IS_DEBUG_ENABLED) {
                	Logger.debug(getClass(), "Preferences updated.");
                }

                synchronized (sInstance) {
                    mAutoDownload = getAutoDownloadState(prefs);
                   		if (Logger.IS_DEBUG_ENABLED) {
                    	Logger.debug(getClass(), "mAutoDownload ------> " + mAutoDownload);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mRoamingStateListener =
        new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(intent.getAction())) {
               		if (Logger.IS_DEBUG_ENABLED) {
                	Logger.debug(getClass(), "Service state changed: " + intent.getExtras());
                }

                ServiceState state = ServiceState.newFromBundle(intent.getExtras());
                boolean isRoaming = state.getRoaming();
               		if (Logger.IS_DEBUG_ENABLED) {
                	Logger.debug(getClass(), "roaming ------> " + isRoaming);
                }
                synchronized (sInstance) {
                    mAutoDownload = getAutoDownloadState(mPreferences, isRoaming);
                   		if (Logger.IS_DEBUG_ENABLED) {
                    	Logger.debug(getClass(), "mAutoDownload ------> " + mAutoDownload);
                    }
                }
            }
        }
    };

    private static DownloadManager sInstance;

    private DownloadManager(Context context) {
        mContext = context;
        mHandler = new Handler();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesChangeListener);

        context.registerReceiver(
                mRoamingStateListener,
                new IntentFilter(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED));

        mAutoDownload = getAutoDownloadState(mPreferences);
       		if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "mAutoDownload ------> " + mAutoDownload);
        }
    }

    public boolean isAuto() {
        return mAutoDownload;
    }

    public static void init(Context context) {
       		if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(DownloadManager.class, "DownloadManager.init()");
        }

        if (sInstance != null) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(DownloadManager.class, "Already initialized.");
            }
        }
        sInstance = new DownloadManager(context);
    }

    public static DownloadManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Uninitialized.");
        }
        return sInstance;
    }

    static boolean getAutoDownloadState(SharedPreferences prefs) {
        return getAutoDownloadState(prefs, isRoaming());
    }

    static boolean getAutoDownloadState(SharedPreferences prefs, boolean roaming) {
        boolean autoDownload = prefs.getBoolean(
                AdvancePreferenceActivity.AUTO_RETRIEVAL, true);

       		if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(DownloadManager.class, "auto download without roaming -> " + autoDownload);
        }

        if (autoDownload) {
            boolean alwaysAuto = prefs.getBoolean(
                    AdvancePreferenceActivity.RETRIEVAL_DURING_ROAMING, false);

           		if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(DownloadManager.class, "auto download during roaming -> " + alwaysAuto);
            }

            if (!roaming || alwaysAuto) {
                return true;
            }
        }
        return false;
    }

    static boolean isRoaming() {
        // TODO: fix and put in Telephony layer
        String roaming = SystemProperties.get(
                TelephonyProperties.PROPERTY_OPERATOR_ISROAMING, null);
       		if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(DownloadManager.class, "roaming ------> " + roaming);
        }
        return "true".equals(roaming);
    }

    public void markState(final Uri uri, int state) {
    	markState(uri, state, false);
    }

    public void markState(final Uri uri, int state, boolean fatal) {
        // Notify user if the message has expired.
        try {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(DownloadManager.class, "markState: uri = " + uri + ", state = " + state + ", fatal = " + fatal);
        	}
            NotificationInd nInd = (NotificationInd) PduPersister.getPduPersister(mContext).load(uri);
            if (fatal || (nInd.getExpiry() < (System.currentTimeMillis() / 1000L) && state == STATE_DOWNLOADING)) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Toast.makeText(mContext, R.string.download_unavailable,
                                Toast.LENGTH_LONG).show();
                    }
                });
                SqliteWrapper.delete(mContext, mContext.getContentResolver(), uri, null, null);
                return;
            }
        } catch(MmsException e) {
        	Logger.error(DownloadManager.class, e.getMessage(), e);
            return;
        }

        // Notify user if downloading permanently failed.
        if (state == STATE_PERMANENT_FAILURE) {
            mHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, R.string.download_unavailable,
                            Toast.LENGTH_LONG).show();
                }
            });
        } else if (!mAutoDownload) {
            state |= DEFERRED_MASK;
        }

        // Use the STATUS field to store the state of downloading process
        // because it's useless for M-Notification.ind.
        ContentValues values = new ContentValues(1);
        values.put(Mms.STATUS, state);
        SqliteWrapper.update(mContext, mContext.getContentResolver(),
                    uri, values, null, null);
    }

    public void showErrorCodeToast(int errorStr) {
        final int errStr = errorStr;
        mHandler.post(new Runnable() {
            public void run() {
                try {
                    Toast.makeText(mContext, errStr, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                	Logger.error(DownloadManager.class, "Caught an exception in showErrorCodeToast");
                }
            }
        });
    }

	public static int getState(int status) {
        return status &~ DEFERRED_MASK;
	}
}
