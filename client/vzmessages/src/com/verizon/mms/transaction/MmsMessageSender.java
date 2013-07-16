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

package com.verizon.mms.transaction;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.InvalidHeaderValueException;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.GenericPdu;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.ReadRecInd;
import com.verizon.mms.pdu.SendReq;
import com.verizon.mms.ui.AdvancePreferenceActivity;
import com.verizon.mms.ui.MessagingPreferenceActivity;
import com.verizon.mms.util.SendingProgressTokenManager;
import com.verizon.mms.util.Util;
import com.verizon.sync.VMASyncHook;
import com.verizon.sync.SyncManager;

public class MmsMessageSender implements MessageSender {
    private final Context mContext;
    private final Uri mMessageUri;
    private final long mMessageSize;

    // Default preference values // ReadReport 3.1 
    private static final boolean DEFAULT_DELIVERY_REPORT_MODE  = false;
    public static final boolean DEFAULT_READ_REPORT_MODE      = false;
    private static final long DEFAULT_EXPIRY_TIME = 7 * 24 * 60 * 60;
    private static final int DEFAULT_PRIORITY = PduHeaders.PRIORITY_NORMAL;
    private static final String DEFAULT_MESSAGE_CLASS = PduHeaders.MESSAGE_CLASS_PERSONAL_STR;

    public MmsMessageSender(Context context, Uri location, long messageSize) {
        mContext = context;
        mMessageUri = location;
        mMessageSize = messageSize;

        if (mMessageUri == null) {
            throw new IllegalArgumentException("Null message URI.");
        }
    }

    public Uri[] sendMessage(long token) throws MmsException {
        
        // Load the MMS from the message uri
        PduPersister p = PduPersister.getPduPersister(mContext);

        GenericPdu pdu = p.load(mMessageUri);

        if (pdu.getMessageType() != PduHeaders.MESSAGE_TYPE_SEND_REQ) {
            throw new MmsException("Invalid message: " + pdu.getMessageType());
        }

        SendReq sendReq = (SendReq) pdu;

        // Update headers.
        updatePreferencesHeaders(sendReq);

        // MessageClass.
        sendReq.setMessageClass(DEFAULT_MESSAGE_CLASS.getBytes());

        // Update the 'date' field of the message before sending it.
        sendReq.setDate(System.currentTimeMillis() / 1000L);

        sendReq.setMessageSize(mMessageSize);

        // true argument says to fix the message type to _X
        p.updateHeaders(mMessageUri, sendReq, true, false);

        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "sendMessage: fixed type of PDU: " + mMessageUri);
        }
        
        //[Note] We should not move the messages to outbox to prevent native database trigger execution.  
        // Move the message into MMS Outbox
        //p.move(mMessageUri, VZUris.getMmsOutboxUri());
//        try {
//        	pdu.fixMessageType();
//        } catch (Exception e) {
//        	if (Logger.IS_DEBUG_ENABLED) {
//        		Logger.error(getClass(), "Exception while trying to fix Message Type in MmsMessageSender");
//        	}
//        }                            
        
        Uri newMessageUri = p.move(mMessageUri, VZUris.getMmsOutboxUri());
        //Uri newMessageUri = p.move(mMessageUri, VZUris.getMmsOutboxUri());
        // This is done to prevent the native app from picking up the pending messages
        // In HTC we are noticing that pending table entry is created even though we tried to skirt around it
        // so we retain this code

        //p.fixDueTimestampOfPendingMessage();

		p.fixMessageTypeToMmsSend(newMessageUri);
		
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "sendMessage: send message type fixed to regular send");
        }

        // This is done to prevent the native app from picking up the pending messages
        // In HTC we are noticing that pending table entry is created even though we tried to skirt around it
        // so we retain this code
		//p.fixDueTimestampOfPendingMessage();
        //ContentValues values = new ContentValues();
        //values.put(Mms.MESSAGE_BOX, Mms.MESSAGE_BOX_ALL);
        //int count= mContext.getContentResolver().update(mMessageUri, values, null, null);

        
  
        // update resp_status value to a non-permanent failure
//        ContentValues respvalue = new ContentValues(2);
//        int respStatus = MmsSms.ERR_TYPE_MMS_PROTO_TRANSIENT; //using temporary value
//        respvalue.put(Mms.RESPONSE_STATUS, respStatus);
//
//        SqliteWrapper.update(mContext, mContext.getContentResolver(),
//        		newMessageUri, respvalue, null, null);
 
        if(MmsConfig.isTabletDevice()){
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Sending mms using vma. uri=" + (newMessageUri));
            }
            Intent sendMsg = new Intent(SyncManager.ACTION_SEND_MSG);
            sendMsg.putExtra(SyncManager.EXTRA_URI, newMessageUri);
            mContext.startService(sendMsg);
    
        }else{
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "sendMessage: new uri is: " + newMessageUri);
            }
            Util.addPendingTableEntry(mContext, newMessageUri, PduHeaders.MESSAGE_TYPE_SEND_REQ);
            
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "sendMessage: sending " + newMessageUri);
            }
            
            // Start MMS transaction service
            SendingProgressTokenManager.put(ContentUris.parseId(newMessageUri), token);
            mContext.startService(new Intent(mContext, TransactionService.class));
        }

        return new Uri[] { newMessageUri };
    }

    // Update the headers which are stored in SharedPreferences.
    private void updatePreferencesHeaders(SendReq sendReq) throws MmsException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Expiry.
        sendReq.setExpiry(prefs.getLong(MessagingPreferenceActivity.EXPIRY_TIME, DEFAULT_EXPIRY_TIME));

        // Priority.
        sendReq.setPriority(prefs.getInt(MessagingPreferenceActivity.PRIORITY, DEFAULT_PRIORITY));

        // Delivery report.
        boolean dr = prefs.getBoolean(AdvancePreferenceActivity.DELIVERY_REPORT_MODE,
                AdvancePreferenceActivity.DELIVERY_REPORT_MODE_DEFAULT);
        sendReq.setDeliveryReport(dr ? PduHeaders.VALUE_YES : PduHeaders.VALUE_NO);

        // ReadReport 3.1 
        boolean rr = prefs.getBoolean(AdvancePreferenceActivity.DELIVERY_REPORT_MODE,AdvancePreferenceActivity.DELIVERY_REPORT_MODE_DEFAULT);
        sendReq.setReadReport(rr?PduHeaders.VALUE_YES:PduHeaders.VALUE_NO);
    }

    public static void sendReadRec(Context context, String to, String messageId, int status) {
        EncodedStringValue[] sender = new EncodedStringValue[1];
        sender[0] = new EncodedStringValue(to);

        try {
            final ReadRecInd readRec = new ReadRecInd(new EncodedStringValue(
                    PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.getBytes()), messageId.getBytes(),
                    PduHeaders.CURRENT_MMS_VERSION, status, sender);

            readRec.setDate(System.currentTimeMillis() / 1000);

            Uri  uri= PduPersister.getPduPersister(context).persist(readRec, VZUris.getMmsOutboxUri());
            if(!MmsConfig.isTabletDevice()){
            	context.startService(new Intent(context, TransactionService.class));
            }else{
            	if(Logger.IS_DEBUG_ENABLED){
            		Logger.debug("WifiHook: sending read receipt:uri="+uri);
            	}
            	VMASyncHook.sendReadReceipt(context , ContentUris.parseId(uri));
            }
        } catch (InvalidHeaderValueException e) {
            Logger.error(MmsMessageSender.class, "Invalid header value", e);
        } catch (MmsException e) {
        	Logger.error(MmsMessageSender.class, "Persist message failed", e);
        }
    }
}
