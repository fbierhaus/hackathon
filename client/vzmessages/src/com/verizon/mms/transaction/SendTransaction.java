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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.TimeOutException;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduComposer;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduParser;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.SendConf;
import com.verizon.mms.pdu.SendReq;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.util.RateController;
import com.verizon.mms.util.SendingProgressTokenManager;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.VMASyncHook;

/**
 * The SendTransaction is responsible for sending multimedia messages
 * (M-Send.req) to the MMSC server.  It:
 *
 * <ul>
 * <li>Loads the multimedia message from storage (Outbox).
 * <li>Packs M-Send.req and sends it.
 * <li>Retrieves confirmation data from the server  (M-Send.conf).
 * <li>Parses confirmation message and handles it.
 * <li>Moves sent multimedia message from Outbox to Sent.
 * <li>Notifies the TransactionService about successful completion.
 * </ul>
 */
public class SendTransaction extends Transaction implements Runnable {
    private Thread mThread;
    private final Uri mSendReqURI;

    public SendTransaction(Context context,
            int transId, TransactionSettings connectionSettings, String uri) {
        super(context, transId, connectionSettings);
        mSendReqURI = Uri.parse(uri);
        mId = uri;

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    /*
     * (non-Javadoc)
     * @see com.android.mms.Transaction#process()
     */
    @Override
    public void process() {
        mThread = new Thread(this);
        mThread.start();
    }

    @Override
    public void process(ThreadPoolExecutor executor) {
    	executor.submit(this);
    }

    public void run() {
    	
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "run: processing transaction: type=" + getType()
				+ ", serviceId=" + getServiceId() + ", mId=" + mId);
		}

        try {
            RateController rateCtlr = RateController.getInstance();
            if (rateCtlr.isLimitSurpassed() && !rateCtlr.isAllowedByUser()) {
            	Logger.error(getClass(), "Sending rate limit surpassed.");
                return;
            }

            // Load M-Send.req from outbox
            PduPersister persister = PduPersister.getPduPersister(mContext);
            SendReq sendReq = (SendReq) persister.load(mSendReqURI);
            
            // Update the 'date' field of the PDU right before sending it.
            long date = System.currentTimeMillis() / 1000L;
            sendReq.setDate(date);
            
            // Persist the new date value into database.
            ContentValues values = new ContentValues(1);
            values.put(Mms.DATE, date);
            SqliteWrapper.update(mContext, mContext.getContentResolver(),
                                 mSendReqURI, values, null, null);

            // do we want to fix the duetime here too?
            //if (Logger.IS_DEBUG_ENABLED)
            //	Logger.debug(TransactionService.class, "FixDueTimestamp from SendTransaction");
            //persister.fixDueTimestampOfPendingMessage();

            // fix bug 2100169: insert the 'from' address per spec
            String lineNumber = MessageUtils.getLocalNumber();
            if (!TextUtils.isEmpty(lineNumber)) {
                sendReq.setFrom(new EncodedStringValue(lineNumber));
            }

            // Pack M-Send.req, send it, retrieve confirmation data, and parse it
            long tokenKey = ContentUris.parseId(mSendReqURI);
            byte[] response = sendPdu(SendingProgressTokenManager.get(tokenKey),
                                      new PduComposer(mContext, sendReq).make());
            SendingProgressTokenManager.remove(tokenKey);

            if (Logger.IS_DEBUG_ENABLED) {
                String respStr = new String(response);
                Logger.debug(getClass(), "run: send mms msg (" + mId + "), resp=" + respStr);
            }

            SendConf conf = (SendConf) new PduParser(response).parse();
            if (conf == null) {
            	Logger.error(getClass(), "No M-Send.conf received.");
            }

            // Check whether the responding Transaction-ID is consistent
            // with the sent one.
            byte[] reqId = sendReq.getTransactionId();
            byte[] confId = conf.getTransactionId();
            if (!Arrays.equals(reqId, confId)) {
            	Logger.error(getClass(), "Inconsistent Transaction-ID: req="
                        + new String(reqId) + ", conf=" + new String(confId));
                return;
            }

            // From now on, we won't save the whole M-Send.conf into
            // our database. Instead, we just save some interesting fields
            // into the related M-Send.req.
            values = new ContentValues(2);
            int respStatus = conf.getResponseStatus();
            values.put(Mms.RESPONSE_STATUS, respStatus);

            if (respStatus != PduHeaders.RESPONSE_STATUS_OK) {
                SqliteWrapper.update(mContext, mContext.getContentResolver(),
                                     mSendReqURI, values, null, null);
                if(Logger.IS_DEBUG_ENABLED){
                // report status to error for now. But before production change it to false. Getting 132 (sending address unresolved)
                    Logger.debug(getClass(), "Server returned an error code: " + respStatus);
                    //Logger.info("SendTransaction:Wifi-Hook:MMS Send failed:uri="+mSendReqURI);
                }
                // no reason to inform the other side of MMS send failuer as it will be retried. Only when it fails completed 
                // should we inform the other side. That happens from RetryScheduler where it determines if its fatal error
                //WifiSyncHelper.markMMSSendFailed(mContext, ContentUris.parseId(mSendReqURI));
                return;
            }

            String messageId = PduPersister.toIsoString(conf.getMessageId());
            values.put(Mms.MESSAGE_ID, messageId);
            SqliteWrapper.update(mContext, mContext.getContentResolver(),
                                 mSendReqURI, values, null, null);

            // Move M-Send.req from Outbox into Sent. 
            Uri uri = persister.move(mSendReqURI, VZUris.getMmsSentUri());
            if(Logger.IS_DEBUG_ENABLED){
                Logger.debug(getClass(), "run: MMS sent: uri = " + uri + ", mSendReqURI = " + mSendReqURI);
            }

            deletePendingTableEntry(mContext, mSendReqURI);

            // Wifi Hook for sent MMS 
            VMASyncHook.syncSentMMS(mContext, ContentUris.parseId(uri));
            mTransactionState.setState(TransactionState.SUCCESS);
            mTransactionState.setContentUri(uri);
        } catch (TimeOutException e) {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.error("TimeOut occured. Resize Media and try again");
        	}
            try {
                //On time out resize the media to less than MmsConfig.getMaxTimedOutMessageSize()
                SlideshowModel slideShowModel = SlideshowModel.createFromMessageUri(mContext, mSendReqURI);
                slideShowModel.finalResize(mSendReqURI, MmsConfig.getMaxTimedOutMessageSize());
            } catch (MmsException ex) {
                Logger.error(ex);
            }
        } catch (IOException e) {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.error(false, getClass(), "Error sending:", e);
        	}
        } catch (OutOfMemoryError t) {
        	Logger.error(false, getClass(), t);
        	
        } catch (Throwable t) {
        	Logger.error(false, getClass(), t);
        	
        } finally {
        	long id = ContentUris.parseId(mSendReqURI);
        	ConversationDataObserver.onMessageStatusChanged(MessageUtils.findMmsThreadId(mContext, id), id, ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mSendReqURI);
                Logger.error(false, getClass(), "Delivery failed.");
            }
            notifyObservers();
        }
    }

    @Override
    public int getType() {
        return SEND_TRANSACTION;
    }
}
