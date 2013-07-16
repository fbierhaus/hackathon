package com.verizon.mms.transaction;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.telephony.SmsManager;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.ui.AdvancePreferenceActivity;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.util.Prefs;

public class SmsSingleRecipientSender extends SmsMessageSender {

    private final boolean mRequestDeliveryReport;
    private String mDest;
    private Uri mUri;

    public SmsSingleRecipientSender(Context context, String dest, String msgText, long threadId, Uri uri) {
        super(context, null, msgText, threadId);
        // request delivery report if the user pref is set
        mRequestDeliveryReport = Prefs.getBoolean(AdvancePreferenceActivity.DELIVERY_REPORT_MODE,
        		AdvancePreferenceActivity.DELIVERY_REPORT_MODE_DEFAULT);
        mDest = dest;
        mUri = uri;
    }

    public Uri[] sendMessage(long token) throws MmsException {
        if (mMessageText == null) {
            // Don't try to send an empty message, and destination should be just
            // one.
            throw new MmsException("Null message body or have multiple destinations.");
        }
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> messages = null;
        if ((MmsConfig.getEmailGateway() != null) &&
                (Mms.isEmailAddress(mDest) || MessageUtils.isAlias(mDest))) {
            String msgText;
            msgText = mDest + " " + mMessageText;
            mDest = MmsConfig.getEmailGateway();
            messages = smsManager.divideMessage(msgText);
        } else {
           messages = smsManager.divideMessage(mMessageText);
           // remove spaces from destination number (e.g. "801 555 1212" -> "8015551212")
           mDest = mDest.replaceAll(" ", "");
        }
        int messageCount = messages.size();

        if (messageCount == 0) {
            // Don't try to send an empty message.
            throw new MmsException("SmsMessageSender.sendMessage: divideMessage returned " +
                    "empty messages. Original message is \"" + mMessageText + "\"");
        }

        boolean moved = Sms.moveMessageToFolder(mContext, mUri, Sms.MESSAGE_TYPE_OUTBOX, 0);
        if (!moved) {
            throw new MmsException("SmsMessageSender.sendMessage: couldn't move message " +
                    "to outbox: " + mUri);
        }
        
        

        ArrayList<PendingIntent> deliveryIntents =  new ArrayList<PendingIntent>(messageCount);
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(messageCount);
        for (int i = 0; i < messageCount; i++) {
            if (mRequestDeliveryReport) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(SmsReceiverService.class, "SMS: asking for delivery report");
                }

                // TODO: Fix: It should not be necessary to
                // specify the class in this intent.  Doing that
                // unnecessarily limits customizability.
                deliveryIntents.add(PendingIntent.getBroadcast(
                        mContext, 0,
                        new Intent(
                                MessageStatusReceiver.MESSAGE_STATUS_RECEIVED_ACTION,
                                mUri,
                                mContext,
                                MessageStatusReceiver.class),
                        0));
            }
            Intent intent  = new Intent(SmsReceiverService.MESSAGE_SENT_ACTION,
                    mUri,
                    mContext,
                    SmsReceiver.class);
            if (i == messageCount -1) {
                intent.putExtra(SmsReceiverService.EXTRA_MESSAGE_SENT_SEND_NEXT, true);
            }
            sentIntents.add(PendingIntent.getBroadcast(
                    mContext, 0, intent, 0));
        }
        try {
            if(!MmsConfig.isTabletDevice()){
                smsManager.sendMultipartTextMessage(mDest, mServiceCenter, messages, sentIntents, deliveryIntents);
            }
        } catch (Exception ex) {
            throw new MmsException("SmsMessageSender.sendMessage: caught " + ex +
                    " from SmsManager.sendTextMessage()");
        }
        if (Logger.IS_DEBUG_ENABLED) {
            log("sendMessage: address=" + mDest + ", threadId=" + mThreadId +
                    ", uri=" + mUri + ", msgs.count=" + messageCount);
        }
        return new Uri[] { mUri };
    }

    private void log(String msg) {
        if (Logger.IS_WARNING_ENABLED) {
            Logger.debug(getClass(), "[SmsSingleRecipientSender] " + msg);
        }
    }
}
