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

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony.Mms;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.internal.telephony.Phone;
import com.verizon.internal.telephony.TelephonyIntents;
import com.verizon.mms.util.PduCache;
import com.verizon.mms.util.Util;
import com.verizon.sync.SyncManager;

/**
 * MmsSystemEventReceiver receives the
 * {@link android.content.intent.ACTION_BOOT_COMPLETED},
 * {@link com.verizon.internal.telephony.TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED}
 * and performs a series of operations which may include:
 * <ul>
 * <li>Show/hide the icon in notification area which is used to indicate
 * whether there is new incoming message.</li>
 * <li>Resend the MM's in the outbox.</li>
 * </ul>
 */
public class MmsSystemEventReceiver extends BroadcastReceiver {
    private static final String TAG = "MmsSystemEventReceiver";
    private static MmsSystemEventReceiver sMmsSystemEventReceiver;

    private static void wakeUpService(Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(MmsSystemEventReceiver.class, "wakeUpService: start transaction service ...");
        }

        context.startService(new Intent(context, TransactionService.class));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "received intent: " + Util.dumpIntent(intent, "    "));
    	}

        String action = intent.getAction();
        if (action.equals(Mms.Intents.CONTENT_CHANGED_ACTION)) {
        	// some devices give us Uris, some give us arrays of IDs
            final PduCache cache = PduCache.getInstance();
        	final Bundle b = intent.getExtras();
        	if (b != null) {
        		final Object o = b.get(Mms.Intents.DELETED_CONTENTS);
        		if (o != null) {
	        		if (o instanceof Uri) {
	        			cache.purge((Uri)o);
	        		}
	        		else if (o instanceof long[]) {
	        			// XXX assume it's inbox message IDs
	        			for (long id : (long[])o) {
	        				cache.purge(ContentUris.withAppendedId(VZUris.getMmsInboxUri(), id));
	        			}
	        		}
	        		else if (Logger.IS_DEBUG_ENABLED) {
	        			Logger.error(getClass(), "unknown deleted object type " , o);
	        		}

	        		// check if we're supposed to notify cursor observers as well
	        		if (b.getBoolean(SyncManager.EXTRA_NOTIFY)) {
	        			if (Logger.IS_DEBUG_ENABLED) {
	        				Logger.debug(getClass(), "onReceive: notifying observers");
	        			}
	        			context.getContentResolver().notifyChange(VZUris.getMmsSmsUri(), null);
	        		}
        		}
        	}
        } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
            String state = intent.getStringExtra(Phone.STATE_KEY);

            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(MmsSystemEventReceiver.class, "ANY_DATA_STATE event received: " + state);
            }

            if (state.equals("CONNECTED")) {
                wakeUpService(context);
            }
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // We should check whether there are unread incoming
            // messages in the Inbox and then update the notification icon.
            // Called on the UI thread so don't block.
            MessagingNotification.nonBlockingUpdateNewMessageIndicator(context, false, false,null);
        }
    }

    public static void registerForConnectionStateChanges(Context context) {
        unRegisterForConnectionStateChanges(context);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(MmsSystemEventReceiver.class, "registerForConnectionStateChanges");
        }
        if (sMmsSystemEventReceiver == null) {
            sMmsSystemEventReceiver = new MmsSystemEventReceiver();
        }

        context.registerReceiver(sMmsSystemEventReceiver, intentFilter);
    }

    public static void unRegisterForConnectionStateChanges(Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(MmsSystemEventReceiver.class, "unRegisterForConnectionStateChanges");
        }
        if (sMmsSystemEventReceiver != null) {
            try {
                context.unregisterReceiver(sMmsSystemEventReceiver);
            } catch (IllegalArgumentException e) {
                // Allow un-matched register-unregister calls
            }
        }
    }
}
