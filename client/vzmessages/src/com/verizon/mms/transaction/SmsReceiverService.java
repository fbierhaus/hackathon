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

import static android.content.Intent.ACTION_BOOT_COMPLETED;
import static android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;

import java.sql.Date;

import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import android.provider.Telephony.Sms.Intents;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.widget.Toast;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.internal.telephony.TelephonyIntents;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.VMAProvision;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.Contact;
import com.verizon.mms.ui.ClassZeroActivity;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.util.SaveGetOrCreateThreadIDFailure;
import com.verizon.mms.util.SendingProgressTokenManager;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.Util;
import com.verizon.mms.util.VZTelephony;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.SyncController;
import com.verizon.sync.SyncManager;
import com.verizon.sync.VMASyncHook;

/**
 * This service essentially plays the role of a "worker thread", allowing us to store incoming messages to the
 * database, update notifications, etc. without blocking the main thread that SmsReceiver runs on.
 */
public class SmsReceiverService extends Service {
    private static final String TAG = "SmsReceiverService";

    // protected VMATelephonyEventHandler eventHandler;

    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;
    private boolean mSending;

    public static final String MESSAGE_SENT_ACTION = "com.android.mms.transaction.MESSAGE_SENT";

    // Indicates next message can be picked up and sent out.
    public static final String EXTRA_MESSAGE_SENT_SEND_NEXT = "SendNextMsg";

    public static final String ACTION_SEND_MESSAGE = "com.verizon.sms.transaction.SEND_MESSAGE";

    // This must match the column IDs below.
    private static final String[] SEND_PROJECTION = new String[] { Sms._ID, // 0
            Sms.THREAD_ID, // 1
            Sms.ADDRESS, // 2
            Sms.BODY, // 3
            Sms.STATUS, // 4

    };

    public Handler mToastHandler = new Handler();

    // This must match SEND_PROJECTION.
    private static final int SEND_COLUMN_ID = 0;
    private static final int SEND_COLUMN_THREAD_ID = 1;
    private static final int SEND_COLUMN_ADDRESS = 2;
    private static final int SEND_COLUMN_BODY = 3;
    private static final int SEND_COLUMN_STATUS = 4;

    private int mResultCode;

    private PreviewPrefetcher mLinkPreviewPrefetcher;

    @Override
    public void onCreate() {
        // Temporarily removed for this duplicate message track down.
        // if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
        // Log.v(TAG, "onCreate");
        // }

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mLinkPreviewPrefetcher = new PreviewPrefetcher(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Temporarily removed for this duplicate message track down.
        // if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
        // Log.v(TAG, "onStart: #" + startId + ": " + intent.getExtras());
        // }
        if (Logger.IS_WARNING_ENABLED) {
            Logger.debug(getClass(), "onStart: #" + startId + ": " + intent.getExtras());
        }
        mResultCode = intent != null ? intent.getIntExtra("result", 0) : 0;

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        // Temporarily removed for this duplicate message track down.
        // if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
        // Log.v(TAG, "onDestroy");
        // }
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        /**
         * Handle incoming transaction requests. The incoming requests are initiated by the MMSC Server or by
         * the MMS Client itself.
         */
        @Override
        public void handleMessage(Message msg) {
            int serviceId = msg.arg1;
            Intent intent = (Intent) msg.obj;
            if (intent != null) {
                String action = intent.getAction();

                int error = intent.getIntExtra("errorCode", 0);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "handleMessage: intent = " + Util.dumpIntent(intent, "  "));
                }

                if (MESSAGE_SENT_ACTION.equals(intent.getAction())
                        || SyncManager.ACTION_SMS_SENT.equals(intent.getAction())) {
                    handleSmsSent(intent, error);
                } else if (SMS_RECEIVED_ACTION.equals(action)) {
                    handleSmsReceived(intent, error);
                } else if (ACTION_BOOT_COMPLETED.equals(action)) {
                    handleBootCompleted();
                } else if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(action)) {
                    handleServiceStateChanged(intent);
                } else if (ACTION_SEND_MESSAGE.endsWith(action)) {
                    handleSendMessage();
                }
            }
            // NOTE: We MUST not call stopSelf() directly, since we need to
            // make sure the wake lock acquired by AlertReceiver is released.
            SmsReceiver.finishStartingService(SmsReceiverService.this, serviceId);
        }
    }

    private void handleServiceStateChanged(Intent intent) {
        // If service just returned, start sending out the queued messages
        ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());

        if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
            sendFirstQueuedMessage();
        }
    }

    private void handleSendMessage() {
        if (!mSending) {
            sendFirstQueuedMessage();
        }
    }

    public synchronized void sendFirstQueuedMessage() {
        boolean success = true;
        // get all the queued messages from the database
        final Uri uri = VZUris.getSmsQueuedUri();
        ContentResolver resolver = getContentResolver();
        // date ASC so we send out in same order the user tried to send messages.
        Cursor c = SqliteWrapper.query(this, resolver, uri, SEND_PROJECTION, null, null, "date ASC");
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    String msgText = c.getString(SEND_COLUMN_BODY);
                    String address = c.getString(SEND_COLUMN_ADDRESS);
                    int threadId = c.getInt(SEND_COLUMN_THREAD_ID);
                    int status = c.getInt(SEND_COLUMN_STATUS);

                    int msgId = c.getInt(SEND_COLUMN_ID);
                    Uri msgUri = ContentUris.withAppendedId(VZUris.getSmsUri(), msgId);

                    SmsMessageSender sender = new SmsSingleRecipientSender(this, address, msgText, threadId,
                            msgUri);

                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "sendFirstQueuedMessage " + msgUri + ", address: " + address
                                + ", threadId: " + threadId + ", status: " + status + ", body: " + msgText);
                    }
                    try {
                        sender.sendMessage(SendingProgressTokenManager.NO_TOKEN);
                        mSending = true;
                        ConversationDataObserver.onMessageStatusChanged(threadId, ContentUris.parseId(msgUri), ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
                    } catch (MmsException e) {
                        Logger.error(getClass(), "sendFirstQueuedMessage: failed to send message " + msgUri
                                + ", caught ", e);
                        mSending = false;
                        messageFailedToSend(msgUri, SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                        success = false;
                    }
                }
            } finally {
                c.close();
            }
        }
        if (success) {
            // We successfully sent all the messages in the queue. We don't need to
            // be notified of any service changes any longer.
            unRegisterForServiceStateChanges();
        }
    }

    private void handleSmsSent(Intent intent, int error) {
        Uri uri = null;
        if (intent.getAction().equalsIgnoreCase(SyncManager.ACTION_SMS_SENT)) {
            uri = intent.getParcelableExtra(SyncManager.EXTRA_URI);
        } else {
            uri = intent.getData();
        }

        mSending = false;
        boolean sendNextMsg = intent.getBooleanExtra(EXTRA_MESSAGE_SENT_SEND_NEXT, false);

        if (mResultCode == Activity.RESULT_OK) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(SmsReceiverService.class, "handleSmsSent sending uri: " + uri);
            }
            ApplicationSettings.getInstance().createOrUpdateMSASMSMapping(uri);
            if (!Sms.moveMessageToFolder(this, uri, Sms.MESSAGE_TYPE_SENT, error)) {
                Logger.error(SmsReceiverService.class, "handleSmsSent: failed to move message " + uri
                        + " to sent folder");
            }
            // Wifi Sync Helper hook
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(SmsReceiverService.class, "Wifi-Hook:SMS: Sent:uri=" + uri);
            }
            long msgId = ContentUris.parseId(uri);
            VMASyncHook.syncSentSMS(this, msgId);
            ConversationDataObserver.onMessageStatusChanged(MessageUtils.findSmsThreadId(this, msgId), msgId, ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
            
            if (sendNextMsg) {
                sendFirstQueuedMessage();
            }

            // Update the notification for failed messages since they may be deleted.
            MessagingNotification.updateSendFailedNotification(this, true);
        } else if ((mResultCode == SmsManager.RESULT_ERROR_RADIO_OFF)
                || (mResultCode == SmsManager.RESULT_ERROR_NO_SERVICE)) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(SmsReceiverService.class, "handleSmsSent: no service, queuing message w/ uri: "
                        + uri);
            }

            // We got an error with no service or no radio. Register for state changes so
            // when the status of the connection/radio changes, we can try to send the
            // queued up messages.
            registerForServiceStateChanges();
            // We couldn't send the message, put in the queue to retry later.
            Sms.moveMessageToFolder(this, uri, Sms.MESSAGE_TYPE_QUEUED, error);
            
            long msgId = ContentUris.parseId(uri);
            ConversationDataObserver.onMessageStatusChanged(MessageUtils.findSmsThreadId(getApplicationContext(), msgId), msgId, ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
            mToastHandler.post(new Runnable() {
                public void run() {
                    Toast.makeText(SmsReceiverService.this, getString(R.string.message_queued),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            messageFailedToSend(uri, error);
            if (sendNextMsg) {
                sendFirstQueuedMessage();
            }
        }
    }

    private void messageFailedToSend(Uri uri, int error) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(SmsReceiverService.class, "messageFailedToSend: uri = " + uri + ", error = " + error);
        }
        Sms.moveMessageToFolder(this, uri, Sms.MESSAGE_TYPE_FAILED, error);

        // Wifi Sync Helper hook
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(SmsReceiverService.class, "Wifi-Hook:SMS:send failed:uri=" + uri);
        }
        // Wifi Sync Hook
        VMASyncHook.syncSendFailedSMS(this, ContentUris.parseId(uri));

        MessagingNotification.notifySendFailed(getApplicationContext(), true);
        
        long msgId = ContentUris.parseId(uri);
        ConversationDataObserver.onMessageStatusChanged(MessageUtils.findSmsThreadId(getApplicationContext(), msgId), msgId, ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
    }

    private void handleSmsReceived(Intent intent, int error) {
        SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
        Uri messageUri = insertMessage(this, msgs, error);
        SmsMessage sms = msgs[0];
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "handleSmsReceived" + (sms.isReplace() ? "(replace)" : "")
                    + " messageUri: " + messageUri + ", address: " + sms.getOriginatingAddress() + ", body: "
                    + sms.getMessageBody() + "Smsc timestamp:" + sms.getTimestampMillis());
        }
        if (messageUri != null) {
            // Called off of the UI thread so ok to block.
            MessagingNotification.blockingUpdateNewMessageIndicator(this, true, false, messageUri);
            
            long msgId = ContentUris.parseId(messageUri);
            ConversationDataObserver.onNewMessageAdded(MessageUtils.findSmsThreadId(getApplicationContext(), msgId), msgId, ConversationDataObserver.MSG_TYPE_SMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("New SMS Message Received: Firing VMA Sync.");
        }
        // Intent vmaIntent =new Intent(SyncManager.ACTION_START_VMA_SYNC);
        // vmaIntent.putExtra(SyncManager.EXTRA_SYNC_TYPE, SyncManager.SYNC_ON_DEMAND);
        // vmaIntent.putExtra(SyncManager.EXTRA_STOPITSELF, SyncManager.SYNC_ON_DEMAND);
        // startService(vmaIntent);
        if (ApplicationSettings.getInstance().isProvisioned()) {
            SyncController.getInstance().startVMASync(true);
        }

    }

    private void handleBootCompleted() {
        moveOutboxMessagesToQueuedBox();
        sendFirstQueuedMessage();

        // Called off of the UI thread so ok to block.
        MessagingNotification.blockingUpdateNewMessageIndicator(this, true, false, null);
    }

    private void moveOutboxMessagesToQueuedBox() {
        ContentValues values = new ContentValues(1);

        values.put(Sms.TYPE, Sms.MESSAGE_TYPE_QUEUED);

        SqliteWrapper.update(getApplicationContext(), getContentResolver(), VZUris.getSmsOutboxUri(), values,
                "type = " + Sms.MESSAGE_TYPE_OUTBOX, null);
    }

    public static final String CLASS_ZERO_BODY_KEY = "CLASS_ZERO_BODY";

    // This must match the column IDs below.
    private final static String[] REPLACE_PROJECTION = new String[] { Sms._ID, Sms.ADDRESS, Sms.PROTOCOL };

    // This must match REPLACE_PROJECTION.
    private static final int REPLACE_COLUMN_ID = 0;

    /**
     * If the message is a class-zero message, display it immediately and return null. Otherwise, store it
     * using the <code>ContentResolver</code> and return the <code>Uri</code> of the thread containing this
     * message so that we can use it for notification.
     */
    private Uri insertMessage(Context context, SmsMessage[] msgs, int error) {
        // Build the helper classes to parse the messages.
        SmsMessage sms = msgs[0];

        // process links in sms
        mLinkPreviewPrefetcher.processLinksInSms(sms);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("SMS :" + Util.dumpSms(sms));
        }

        if (sms.getMessageClass() == SmsMessage.MessageClass.CLASS_0) {
            displayClassZeroMessage(context, sms);
            return null;
        } else if (sms.isReplace()) {
            return replaceMessage(context, msgs, error);
        } else {
            return storeMessage(context, msgs, error);
        }
    }

    /**
     * This method is used if this is a "replace short message" SMS. We find any existing message that matches
     * the incoming message's originating address and protocol identifier. If there is one, we replace its
     * fields with those of the new message. Otherwise, we store the new message as usual.
     * 
     * See TS 23.040 9.2.3.9.
     */
    private Uri replaceMessage(Context context, SmsMessage[] msgs, int error) {
        SmsMessage sms = msgs[0];
        ContentValues values = extractContentValues(sms);

        values.put(Inbox.BODY, sms.getMessageBody());
        values.put(Sms.ERROR_CODE, error);

        ContentResolver resolver = context.getContentResolver();
        String originatingAddress = sms.getOriginatingAddress();
        int protocolIdentifier = sms.getProtocolIdentifier();
        String selection = Sms.ADDRESS + " = ? AND " + Sms.PROTOCOL + " = ?";
        String[] selectionArgs = new String[] { originatingAddress, Integer.toString(protocolIdentifier) };

        Cursor cursor = SqliteWrapper.query(context, resolver, VZUris.getSmsInboxUri(), REPLACE_PROJECTION,
                selection, selectionArgs, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    long messageId = cursor.getLong(REPLACE_COLUMN_ID);
                    Uri messageUri = ContentUris.withAppendedId(VZUris.getSmsUri(), messageId);

                    SqliteWrapper.update(context, resolver, messageUri, values, null, null);
                    return messageUri;
                }
            } finally {
                cursor.close();
            }
        }
        return storeMessage(context, msgs, error);
    }

    private Uri storeMessage(Context context, SmsMessage[] msgs, int error) {
        SmsMessage sms = msgs[0];

        // Store the message in the content provider.
        ContentValues values = extractContentValues(sms);
        values.put(Sms.ERROR_CODE, error);
        int pduCount = msgs.length;

        // Build up the body from the parts.
        StringBuilder bodyBuilder = new StringBuilder();
        if (pduCount == 1) {
            // There is only one part, so grab the body directly.
        	bodyBuilder.append(sms.getDisplayMessageBody());
        } else {
            for (int i = 0; i < pduCount; i++) {
                sms = msgs[i];
                bodyBuilder.append(sms.getDisplayMessageBody());
            }
        }
        String smsBody = bodyBuilder.toString();
        
        String address = values.getAsString(Sms.ADDRESS);
        // coming as VTEXT unverified sender
        if (AppSettings.VZW_VTEXT_UNVERIFIED_SENDER.equalsIgnoreCase(address)) {
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "Recognized as unverified sender - address = " + address + " body = " + smsBody);
            }
            //extract address from the body
            String[] bodySplit = smsBody.split(" ", 2);
            if (bodySplit.length > 1) {
            	address = bodySplit[0];
            	smsBody = bodySplit[1];
            }
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "Unverified sender modified - address = " + address + " body = " + smsBody);
            }
            values.put(Inbox.ADDRESS, address);
        }
        values.put(Inbox.BODY, smsBody);

        // Make sure we've got a thread id so after the insert we'll be able to delete
        // excess messages.
        Long threadId = values.getAsLong(Sms.THREAD_ID);
        Contact cacheContact = Contact.get(address, true);
        if (cacheContact != null) {
            address = cacheContact.getNumber();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "address = " + address + " and threadId = " + threadId);
        }
        if (((threadId == null) || (threadId == 0)) && (address != null)) {
            try {
                try {
                    threadId = VZTelephony.getOrCreateThreadId(context, address);
                } catch (IllegalArgumentException iae) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        ;
                    }
                    threadId = VZTelephony.getOrCreateThreadId(context, address);
                }
            } catch (IllegalArgumentException iae) {
                try {
                    SaveGetOrCreateThreadIDFailure.getInstance(context).saveReceivedSMS(values);
                } catch (Exception e) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.error(SmsReceiver.class, e);
                    }
                }
                return null;
            }
            values.put(Sms.THREAD_ID, threadId);
        }

        ContentResolver resolver = context.getContentResolver();

        Uri insertedUri = SqliteWrapper.insert(context, resolver, VZUris.getSmsInboxUri(), values);

        long msgID = ContentUris.parseId(insertedUri);

        // VMA Mapping
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "VMA-Hook: SMS Received: threadId = " + threadId + ", msgId = " + msgID);
        }
        // add checksum and mapping
        String msgBody = values.getAsString(Sms.BODY);
        String checksumSource = ApplicationSettings.parseAdddressForChecksum(address)
                + msgBody;
        ApplicationSettings settings = ApplicationSettings.getInstance();
        boolean isServiceMessage = (AppSettings.VZW_SERVICEMSG_SENDER_NO.equals(address) || AppSettings.VZW_SERVICEMSG_SENDER_NO_QA
                .equalsIgnoreCase(address) || AppSettings.VZW_SERVICEMSG_SENDER_TEXT.equals(address) );
        if (!settings.isProvisioned() && isServiceMessage) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "VZW Service message: address=" + address);
            }
            // changed logic to only check startsWith rather than exact match and also to do this even if a new device was registered
            if (msgBody.startsWith(AppSettings.VMA_ACTIVATION_WELCOME_MESSAGE) ||
            		msgBody.startsWith(AppSettings.VMA_DEVICE_WELCOME_MESSAGE)) { 
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "Found VMA Service message. firing autoprovisioning."
                            + checksumSource);
                }
//                Intent interceptIntent = new Intent(context, SingleDialogActivity.class);
//                interceptIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                interceptIntent.putExtra(SingleDialogActivity.INTERCEPT_DIALOG, true);
//                startActivity(interceptIntent);
                Intent intent = new Intent(SyncManager.ACTION_START_PROVISIONING);
                intent.putExtra(SyncManager.EXTRA_ACTION, VMAProvision.ACTION_AUTO_PROVISION_HANDSET);
                startService(intent);
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "Not a VMA service message=" + checksumSource);
                }
            }
        }else{
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Not a VZW service message: address=" + address);
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("storeMessage: Calling createOrUpdateMSASMSMapping, locked");
        }
        String body = values.getAsString(Sms.BODY);
        String mdn = ApplicationSettings.parseAdddressForChecksum(address);
        long luid = ContentUris.parseId(insertedUri);
        if(Logger.IS_DEBUG_ENABLED){
            Logger.debug("SMS Gateway time: sms.getTimestampMillis()="+ new Date(sms.getTimestampMillis()).toGMTString());
            Logger.debug("SMS Stored  time: "+ new Date(values.getAsLong(Sms.DATE)).toGMTString());
        }
        
        ApplicationSettings.getInstance().getVMAEventHandler()
                .telephonySMSReceive(luid, threadId, body, mdn, sms.getTimestampMillis() , values.getAsLong(Sms.DATE));
        // Now make sure we're not over the limit in stored messages
        // Recycler.getMessageRecycler().deleteOldMessagesByThreadId(getApplicationContext(), threadId);

        return insertedUri;
    }

    /**
     * Extract all the content values except the body from an SMS message.
     */
    private ContentValues extractContentValues(SmsMessage sms) {
        // Store the message in the content provider.
        ContentValues values = new ContentValues();

        String smsAddress = sms.getDisplayOriginatingAddress();
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "Address from SMS: " + smsAddress);
        }
        values.put(Inbox.ADDRESS, smsAddress);

        // Use now for the timestamp to avoid confusion with clock
        // drift between the handset and the SMSC.
        values.put(Inbox.DATE, new Long(System.currentTimeMillis()));
        values.put(Inbox.PROTOCOL, sms.getProtocolIdentifier());
        values.put(Inbox.READ, 0);
        values.put(Inbox.SEEN, 0);
        if (sms.getPseudoSubject().length() > 0) {
            values.put(Inbox.SUBJECT, sms.getPseudoSubject());
        }
        values.put(Inbox.REPLY_PATH_PRESENT, sms.isReplyPathPresent() ? 1 : 0);
        values.put(Inbox.SERVICE_CENTER, sms.getServiceCenterAddress());
        return values;
    }

    /**
     * Displays a class-zero message immediately in a pop-up window with the number from where it received the
     * Notification with the body of the message
     * 
     */
    private void displayClassZeroMessage(Context context, SmsMessage sms) {
        // Using NEW_TASK here is necessary because we're calling
        // startActivity from outside an activity.
        Intent smsDialogIntent = new Intent(context, ClassZeroActivity.class).putExtra("pdu", sms.getPdu())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        context.startActivity(smsDialogIntent);
    }

    private void registerForServiceStateChanges() {
        Context context = getApplicationContext();
        unRegisterForServiceStateChanges();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "registerForServiceStateChanges");
        }

        context.registerReceiver(SmsReceiver.getInstance(), intentFilter);
    }

    private void unRegisterForServiceStateChanges() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "unRegisterForServiceStateChanges");
        }
        try {
            Context context = getApplicationContext();
            context.unregisterReceiver(SmsReceiver.getInstance());
        } catch (IllegalArgumentException e) {
            // Allow un-matched register-unregister calls
        }
    }
}
