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
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.net.Uri;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.MmsException;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduComposer;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.ReadRecInd;
import com.verizon.mms.ui.MessageUtils;

/**
 * The ReadRecTransaction is responsible for sending read report
 * notifications (M-read-rec.ind) to clients that have requested them.
 * It:
 *
 * <ul>
 * <li>Loads the read report indication from storage (Outbox).
 * <li>Packs M-read-rec.ind and sends it.
 * <li>Notifies the TransactionService about successful completion.
 * </ul>
 */
public class ReadRecTransaction extends Transaction implements Runnable {
    private static final String TAG = "ReadRecTransaction";

    private final Uri mReadReportURI;

    public ReadRecTransaction(Context context,
            int transId,
            TransactionSettings connectionSettings,
            String uri) {
        super(context, transId, connectionSettings);
        mReadReportURI = Uri.parse(uri);
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
    	new Thread(this).start();
    }
    
    @Override
    public void process(ThreadPoolExecutor executor) {
    	executor.submit(this);
    }

    @Override
    public int getType() {
        return READREC_TRANSACTION;
    }

	@Override
	public void run() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(ReadRecTransaction.class,
					new Date() + "===>> Processing transaction ==> type=" + getType()
							+ ", serviceId=" + getServiceId() + ", mId=" + mId);
		}

        //remove before going to production
        //Bug 804
		if (Logger.IS_DEBUG_ENABLED) {
	        try {
	            Logger.debug("********IMP********* Sending Read Report****");
	        } catch (Exception e) {
	            
	        }
		}
        
        PduPersister persister = PduPersister.getPduPersister(mContext);
        try {
            // Load M-read-rec.ind from outbox
            ReadRecInd readRecInd = (ReadRecInd) persister.load(mReadReportURI);

            // insert the 'from' address per spec
            String lineNumber = MessageUtils.getLocalNumber();
            if (lineNumber != null && lineNumber.length() != 0) {
            	readRecInd.setFrom(new EncodedStringValue(lineNumber));
            }

            // Pack M-read-rec.ind and send it
            byte[] postingData = new PduComposer(mContext, readRecInd).make();
            sendPdu(postingData);
            mTransactionState.setState(TransactionState.SUCCESS);
        } catch (IOException e) {
            if(Logger.IS_DEBUG_ENABLED){
            	Logger.debug(getClass(), "Failed to send M-Read-Rec.Ind.", e);
            }
        } catch (MmsException e) {
            if(Logger.IS_DEBUG_ENABLED){
            	Logger.debug(getClass(), "Failed to load message from Outbox.", e);
            }
        } catch (RuntimeException e) {
            if(Logger.IS_ERROR_ENABLED){
            	Logger.error(getClass(), "Unexpected RuntimeException.", e);
            }
        } finally {
        	try {
        		Uri uri = persister.move(mReadReportURI, VZUris.getMmsSentUri());
                mTransactionState.setContentUri(uri);
        	} catch (Exception e) {
                if(Logger.IS_ERROR_ENABLED){
                	Logger.error(getClass(), "Exception while moving Read-Rec xn to Sent Folder", e);
                }        		
        	}
            deletePendingTableEntry(mContext, mReadReportURI);

            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mReadReportURI);
            }
            notifyObservers();
        }
	}
}
