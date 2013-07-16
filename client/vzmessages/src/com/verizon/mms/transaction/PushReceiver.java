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

import static android.provider.Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION;
import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_DELIVERY_IND;
import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X;
import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_READ_ORIG_IND;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.Telephony.Mms;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.ContentType;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.pdu.DeliveryInd;
import com.verizon.mms.pdu.GenericPdu;
import com.verizon.mms.pdu.NotificationInd;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduParser;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.ReadOrigInd;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.Util;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.VMASyncHook;

/**
 * Receives Intent.WAP_PUSH_RECEIVED_ACTION intents and starts the
 * TransactionService by passing the push-data to it.
 */
public class PushReceiver extends BroadcastReceiver {
    private class ReceivePushTask extends AsyncTask<Intent,Void,Void> {
        private Context mContext;
        public ReceivePushTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Intent... intents) {
            Intent intent = intents[0];

            // Get raw PDU push-data from the message and parse it
            byte[] pushData = intent.getByteArrayExtra("data");
            PduParser parser = new PduParser(pushData);
            GenericPdu pdu = parser.parse();

            if (null == pdu) {
                if(Logger.IS_ERROR_ENABLED){
                    Logger.error(PushReceiver.class, "Invalid PUSH data");
                }
                return null;
            }

            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "PushReceiver.doInBackground: pdu = " + Util.dumpPdu(pdu));
            }

            PduPersister p = PduPersister.getPduPersister(mContext);
            ContentResolver cr = mContext.getContentResolver();
            int type = pdu.getMessageType();
            long threadId = -1;

            try {
                switch (type) {
                    case MESSAGE_TYPE_DELIVERY_IND:
                    case MESSAGE_TYPE_READ_ORIG_IND: {
                    	long[] ids = findMessageIds(mContext, pdu, type);;
                        threadId = ids[0];
                        
                        if (Logger.IS_DEBUG_ENABLED) {
                        	Logger.debug(getClass(), "Delivery IND: threadId " + threadId + ", type=" + type);
                        }
                        if (threadId == -1) {
                            // The associated SendReq isn't found, therefore skip
                            // processing this PDU.
                            break;
                        }

                        Uri uri = p.persist(pdu, VZUris.getMmsInboxUri());
                        // Update thread ID for ReadOrigInd & DeliveryInd.
                        ContentValues values = new ContentValues(1);
                        values.put(Mms.THREAD_ID, threadId);
                        SqliteWrapper.update(mContext, cr, uri, values, null, null);
                        // sync both Delivery Indication and Read Receipt
                        if(Logger.IS_DEBUG_ENABLED) {
                        	Logger.debug(getClass(), "Wifi-Hook:MMS Delivery IND: threadId " + threadId + ", uri=" + uri);
                        }
                        MessagingNotification.showMMSDeliveryStatus(mContext,uri,pdu);
                        
                        long msgId = ContentUris.parseId(uri);
                        ConversationDataObserver.onMessageStatusChanged(threadId, ids[1], ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
                        VMASyncHook.syncMMSDelivered(mContext, msgId);
                        break;
                    }
                    case MESSAGE_TYPE_NOTIFICATION_IND: 
                    case MESSAGE_TYPE_NOTIFICATION_IND_X: 
                    {
                        NotificationInd nInd = (NotificationInd) pdu;
                        if (Logger.IS_DEBUG_ENABLED) {
                        	Logger.debug(getClass(), "got NOTIF_IND");
                        }

                        if (MmsConfig.getTransIdEnabled()) {
                            byte [] contentLocation = nInd.getContentLocation();
                            if (Logger.IS_DEBUG_ENABLED) {
                            	Logger.debug(PushReceiver.class, "NOTIF_IND: xnIdEnabled - contentLoc: " + contentLocation);
                            }
                            if ('=' == contentLocation[contentLocation.length - 1]) {
                                byte [] transactionId = nInd.getTransactionId();
                                byte [] contentLocationWithId = new byte [contentLocation.length
                                                                          + transactionId.length];
                                System.arraycopy(contentLocation, 0, contentLocationWithId,
                                        0, contentLocation.length);
                                System.arraycopy(transactionId, 0, contentLocationWithId,
                                        contentLocation.length, transactionId.length);
                                nInd.setContentLocation(contentLocationWithId);
                                if (Logger.IS_DEBUG_ENABLED) {
                                	Logger.debug(PushReceiver.class, "NOTIF_IND: contentLocation: " + contentLocationWithId);
                                }
                            }
                        }

                        if (!isDuplicateNotification(mContext, nInd)) {
                            if (Logger.IS_DEBUG_ENABLED) {
                            	Logger.debug(PushReceiver.class, "NOTIF_IND: not a duplicate so persist");
                            }
                            try {
                            	pdu.fixNotificationMessageType();
                            } catch (Exception e) {
                            	if (Logger.IS_DEBUG_ENABLED) {
                            		Logger.error(getClass(), "Exception while trying to fix Message Type in Push Receiver");
                            	}
                            }                            
                            Uri uri = p.persist(pdu, VZUris.getMmsInboxUri());
                            p.fixMessageTypeToMmsNotification(uri);
                            Util.addPendingTableEntry(mContext, uri, PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
                            
                            long msgId = ContentUris.parseId(uri);
                            ConversationDataObserver.onNewMessageAdded(MessageUtils.findMmsThreadId(mContext, msgId), msgId, ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
                            
                            // Start service to finish the notification transaction.
                            Intent svc = new Intent(mContext, TransactionService.class);
                            svc.putExtra(TransactionBundle.URI, uri.toString());
                            svc.putExtra(TransactionBundle.TRANSACTION_TYPE,
                                    Transaction.NOTIFICATION_TRANSACTION);
                            mContext.startService(svc);
                        } else if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(PushReceiver.class, "Skip downloading duplicate message: " + new String(nInd.getContentLocation()));
                        }
                        break;
                    }
                    default:
                        Logger.error(PushReceiver.class, "Received unrecognized PDU.");
                }
            } catch (MmsException e) {
                Logger.error(PushReceiver.class,  e ,"Failed to save the data from PUSH: type=" + type);
            } catch (RuntimeException e) {
                Logger.error(PushReceiver.class,  e ,"Unexpected RuntimeException.");
            }

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(PushReceiver.class, "PUSH Intent processed.");
            }

            return null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	if (MmsConfig.isTabletDevice()) {
			if (Logger.IS_ACRA_ENABLED) {
				Logger.debug(getClass(), "got incoming Push notification on tablet - dont handle it");
			}
			return;
    	}
        if (intent.getAction().equals(WAP_PUSH_RECEIVED_ACTION) && ContentType.MMS_MESSAGE.equals(intent.getType())) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(PushReceiver.class, "received PUSH intent: " + Util.dumpIntent(intent, "    "));
            }

            // Hold a wake lock for 5 seconds, enough to give any
            // services we start time to take their own wake locks.
            PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                            "MMS PushReceiver");
            wl.acquire(5000);
            new ReceivePushTask(context).execute(intent);

            if (isOrderedBroadcast()) {
            	 if (Logger.IS_DEBUG_ENABLED) {
                     Logger.debug(PushReceiver.class, "aborting Broadcast: ");
                 }            	
                abortBroadcast();
            }
        }
    }

    /**
     * This function returns the MMS and thread to which the delivery/read recipts belongs to
     *  
     * @param context
     * @param pdu
     * @param type
     * @return long array with threadId in 0th position and MessageId in 1st position
     */
    private static long[] findMessageIds(Context context, GenericPdu pdu, int type) {
        String messageId;
        long[] ids = new long[] {-1, -1};
        if (type == MESSAGE_TYPE_DELIVERY_IND) {
            messageId = new String(((DeliveryInd) pdu).getMessageId());
        } else {
            messageId = new String(((ReadOrigInd) pdu).getMessageId());
        }

        StringBuilder sb = new StringBuilder('(');
        sb.append(Mms.MESSAGE_ID);
        sb.append('=');
        sb.append(DatabaseUtils.sqlEscapeString(messageId));
        sb.append(" AND ");
        sb.append(Mms.MESSAGE_TYPE);
        sb.append('=');
        sb.append(PduHeaders.MESSAGE_TYPE_SEND_REQ);
        // TODO ContentResolver.query() appends closing ')' to the selection argument
        // sb.append(')');

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
        		VZUris.getMmsUri(), new String[] { Mms.THREAD_ID, Mms._ID },
                            sb.toString(), null, null);
        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    ids[0] = cursor.getLong(0); //threadId
                    ids[1] = cursor.getLong(1); //message id
                }
            } finally {
                cursor.close();
            }
        }

        return ids;
    }

    private static boolean isDuplicateNotification(
            Context context, NotificationInd nInd) {
        byte[] rawLocation = nInd.getContentLocation();
   	 	if (Logger.IS_DEBUG_ENABLED) {
   	 		Logger.debug(PushReceiver.class, "isDup: rawLoc: " + rawLocation);
   	 	}            	
        if (rawLocation != null) {
            String location = new String(rawLocation);
            String selection = Mms.CONTENT_LOCATION + " = ?";
            String[] selectionArgs = new String[] { location };
            Cursor cursor = SqliteWrapper.query(
                    context, context.getContentResolver(),
                    VZUris.getMmsUri(), new String[] { Mms._ID },
                    selection, selectionArgs, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        // We already received the same notification before.
                        return true;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return false;
    }
}
