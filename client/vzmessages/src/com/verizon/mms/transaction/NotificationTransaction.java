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

import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;
import static com.verizon.mms.pdu.PduHeaders.STATUS_DEFERRED;
import static com.verizon.mms.pdu.PduHeaders.STATUS_RETRIEVED;
import static com.verizon.mms.pdu.PduHeaders.STATUS_UNRECOGNIZED;
import static com.verizon.mms.transaction.TransactionState.FAILED;
import static com.verizon.mms.transaction.TransactionState.INITIALIZED;
import static com.verizon.mms.transaction.TransactionState.SUCCESS;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.rocketmobile.asimov.Asimov;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.model.SmilHelper;
import com.verizon.mms.pdu.GenericPdu;
import com.verizon.mms.pdu.NotificationInd;
import com.verizon.mms.pdu.NotifyRespInd;
import com.verizon.mms.pdu.PduBody;
import com.verizon.mms.pdu.PduComposer;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduParser;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.RetrieveConf;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.util.DownloadManager;
import com.verizon.mms.util.Recycler;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.Util;
import com.verizon.sync.ConversationDataObserver;

/**
 * The NotificationTransaction is responsible for handling multimedia
 * message notifications (M-Notification.ind).  It:
 *
 * <ul>
 * <li>Composes the notification response (M-NotifyResp.ind).
 * <li>Sends the notification response to the MMSC server.
 * <li>Stores the notification indication.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 *
 * NOTE: This MMS client handles all notifications with a <b>deferred
 * retrieval</b> response.  The transaction service, upon succesful
 * completion of this transaction, will trigger a retrieve transaction
 * in case the client is in immediate retrieve mode.
 */
public class NotificationTransaction extends Transaction implements Runnable {
    private Uri mUri;
    private NotificationInd mNotificationInd;
    private String mContentLocation;
    private boolean threadIDfailed = false;
    public NotificationTransaction(
            Context context, int serviceId,
            TransactionSettings connectionSettings, String uriString)   throws MmsException {
        super(context, serviceId, connectionSettings);

        mUri = Uri.parse(uriString);

        try {
            mNotificationInd = (NotificationInd)
                    PduPersister.getPduPersister(context).load(mUri);
        } catch (MmsException e) {
        	Logger.error(true, getClass(), "Failed to load NotificationInd from: " + uriString, e);
            throw new IllegalArgumentException();
        }

        // Strumsoft - to allow us to decide whether a notification transaction is equivalent to a retrieve xn
        // we keep their ids consistent. Dont see any reason why the ids should be different forms
        //mId = new String(mNotificationInd.getTransactionId());
        mId = mContentLocation = new String(mNotificationInd.getContentLocation());
        if(Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "X-Mms-Content-Location: " + mContentLocation);
        }

        if ((mId == null) || (mContentLocation == null)) {
            throw new MmsException("X-Mms-Content-Location null or empty for : " + mUri);            	
        }

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    /**
     * This constructor is only used for test purposes.
     */
    public NotificationTransaction(
            Context context, int serviceId,
            TransactionSettings connectionSettings, NotificationInd ind)   throws MmsException {
        super(context, serviceId, connectionSettings);

        try {
            mUri = PduPersister.getPduPersister(context).persist(
                        ind, VZUris.getMmsInboxUri());
        } catch (MmsException e) {
        	Logger.error(true, getClass(), "Failed to save NotificationInd in constructor.", e);
            throw new IllegalArgumentException();
        }

        mNotificationInd = ind;
        mId = new String(ind.getTransactionId());
        if (mId == null) {
            throw new MmsException("X-Mms-Content-Location null or empty for : " + mUri);            	
        }
    }

    /*
     * (non-Javadoc)
     * @see com.google.android.mms.pdu.Transaction#process()
     */
    @Override
    public void process() {
        new Thread(this).start();
    }

    @Override
    public void process(ThreadPoolExecutor executor) {
    	executor.submit(this);
    }

    public void run() {
        DownloadManager downloadManager = DownloadManager.getInstance();
        boolean autoDownload = downloadManager.isAuto();
        boolean dataSuspended = (Asimov.getApplication().getTelephonyManager().getDataState() ==
                TelephonyManager.DATA_SUSPENDED);
        try {
            if(Logger.IS_DEBUG_ENABLED){
            	Logger.debug(getClass(), "Notification transaction launched: " + this + " autoDownload:" + autoDownload);
            }

            // By default, we set status to STATUS_DEFERRED because we
            // should response MMSC with STATUS_DEFERRED when we cannot
            // download a MM immediately.
            int status = STATUS_DEFERRED;
            // Don't try to download when data is suspended, as it will fail, so defer download
            if (!autoDownload || dataSuspended) {
                if(Logger.IS_DEBUG_ENABLED){
                	Logger.debug(getClass(), "Not autoDownload so postpone it");
                }
                downloadManager.markState(mUri, DownloadManager.STATE_UNSTARTED);
                sendNotifyRespInd(status);
                return;
            }

            downloadManager.markState(mUri, DownloadManager.STATE_DOWNLOADING);

            if(Logger.IS_DEBUG_ENABLED){
            	Logger.debug(getClass(), "Content-Location: " + mContentLocation);
            }

            byte[] retrieveConfData = null;
            // We should catch exceptions here to response MMSC
            // with STATUS_DEFERRED.
            try {
                retrieveConfData = getPdu(mContentLocation);
            } catch (IOException e) {
                mTransactionState.setState(FAILED);
            	RetrieveTransaction.checkError(mTransactionState, e);
            }

            if (retrieveConfData != null) {
                GenericPdu pdu = new PduParser(retrieveConfData).parse();
                if(Logger.IS_DEBUG_ENABLED){
                	Logger.debug(getClass(), "pdu = " + Util.dumpPdu(pdu));
                }
                if ((pdu == null) || (pdu.getMessageType() != MESSAGE_TYPE_RETRIEVE_CONF)) {
                	Logger.error(getClass(), "Invalid M-RETRIEVE.CONF PDU.");
                    mTransactionState.setState(FAILED);
                    status = STATUS_UNRECOGNIZED;
                } else {
                	// set date to time received rather than sent
                	((RetrieveConf)pdu).setDate(System.currentTimeMillis() / 1000);

                	// Save the received PDU (must be a M-RETRIEVE.CONF).
                    PduPersister p = PduPersister.getPduPersister(mContext);
                    boolean persisted = false;
                    RetrieveConf retrieveConf = ((RetrieveConf)pdu);
                    PduBody body = retrieveConf.getBody();
                    Uri uri = null;
                    
                    try {
                    	List<PduBody> pb = SmilHelper.createPartsFromPduBody(mContext, retrieveConf.getBody());
                    	
                    	//if the pd.size is greater than one then we have a slideshow
                    	//split it and save each slide as an individual mms
                    	if (pb != null && pb.size() > 1) {
                    		int i = 0;
                    		int lastIndex = pb.size() - 1;
							for (PduBody pduBody : pb) {
                    			retrieveConf.setBody(pduBody);
								//apart from first pdu mark the rest as read
                    			uri = p.persist(retrieveConf, VZUris.getMmsInboxUri(), i != lastIndex);
                    			
                    			i++;
                    		}
                    		persisted = true;
                    		
                    		if (Logger.IS_DEBUG_ENABLED) {
                    			Logger.debug("Persisted the parts by splitting the slideshow");
                    		}
                    	} else {
                    		if (Logger.IS_DEBUG_ENABLED) {
                    			Logger.debug("No need to split the mms");
                    		}
                    	}
                    	
                    }catch(IllegalArgumentException i) {
                    	threadIDfailed = true;
                    }
                     catch (Exception e) {
                    	Logger.error("Could not split the slideshow into seperate parts and persist it ", e);
                    }
                    
                    if (!persisted) {
                    	retrieveConf.setBody(body);
                    	try {
                    		uri = p.persist(pdu, VZUris.getMmsInboxUri());
                    		threadIDfailed = false;
                    	} catch(IllegalArgumentException i) {
                        	threadIDfailed = true;
                        }
                    	catch(Exception e) {
                        	e.printStackTrace();
                        }
                    }
                    if(!threadIDfailed) {
                    // before deleting M-NotifyResp.ind get the conversation thread from it
	                    long oldThread = getMMSThreadId(mContext, ContentUris.parseId(mUri));
	
	                    // We have successfully downloaded the new MM. Delete the M-NotifyResp.ind from Inbox.
	                    // In case if the notification pdu got deleted.the mms provider will return the same uri/Id
	                    // for last persisted message. so we are checking type before delete. 
	                    String selection = Mms.MESSAGE_TYPE+"="+ PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
	                    SqliteWrapper.delete(mContext, mContext.getContentResolver(), mUri, selection, null);
	                    
	                    deletePendingTableEntry(mContext, mUri);
	                    
	                    mUri = uri;
	
	                    if (Logger.IS_DEBUG_ENABLED) {
	                        long newThread = getMMSThreadId(mContext, ContentUris.parseId(mUri));
	                        Logger.debug(getClass(), "New MMS received: old threadId = " + oldThread +
	                        	", new = " + newThread + ", uri = " + uri);
	                    }
	
	                    // delete drafts from the old thread to fix up the timestamp
	                    final Uri delUri = ContentUris.withAppendedId(VZUris.getSmsConversations(), oldThread);
	                    final String where = Sms.TYPE + "=" + Sms.MESSAGE_TYPE_DRAFT;
	                    SqliteWrapper.delete(mContext, mContext.getContentResolver(), delUri, where, null);

	                    ConversationDataObserver.onMessageDeleted(oldThread, ContentUris.parseId(mUri), ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
	                    if (uri != null) {
	                    	long msgId = ContentUris.parseId(uri);
	                    	ConversationDataObserver.onNewMessageAdded(MessageUtils.findMmsThreadId(mContext, msgId), ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
	                    }
	//                  NotificationHelper.broadcastNewMMS(mContext, threadId, msgId);
	                    status = STATUS_RETRIEVED;
                    } else {
                    	status = STATUS_DEFERRED;
                    	mTransactionState.setState(FAILED);
                    }
                }
            }

            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "finishing with status = 0x" + Integer.toHexString(status));
            }

            // Check the status and update the result state of this Transaction.
            switch (status) {
                case STATUS_RETRIEVED:
                    mTransactionState.setState(SUCCESS);
                    /* Wifi Sync HookUps */
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "NotificationTransaction:Wifi-Hook: MMS Received:" + mUri);
                    }
                    break;
                case STATUS_DEFERRED:
                    // STATUS_DEFERRED, may be a failed immediate retrieval.
                    if (mTransactionState.getState() == INITIALIZED) {
                        mTransactionState.setState(SUCCESS);
                    }
                    break;
            }

            sendNotifyRespInd(status);
        	threadIDfailed = false;
            // Make sure this thread isn't over the limits in message count.
//            Recycler.getMessageRecycler().deleteOldMessagesInSameThreadAsMessage(mContext, mUri);
        } catch (IOException e) {            
        	Logger.error(false, getClass(), Log.getStackTraceString(e));
        } catch (Throwable t) {
        	Logger.error(getClass(), Log.getStackTraceString(t));
        } finally {
            mTransactionState.setContentUri(mUri);
            if (!autoDownload || dataSuspended) {
                // Always mark the transaction successful for deferred
                // download since any error here doesn't make sense.
                mTransactionState.setState(SUCCESS);
            }
            if (mTransactionState.getState() != SUCCESS) {
                mTransactionState.setState(FAILED);
                if (mUri != null) {
                	long msgId = ContentUris.parseId(mUri);
                	ConversationDataObserver.onMessageStatusChanged(MessageUtils.findMmsThreadId(mContext, msgId), msgId, ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
                }
                Logger.error(false, getClass(), "NotificationTransaction failed.");
            }
            notifyObservers();
        }
    }

    private void sendNotifyRespInd(int status) throws MmsException, IOException {
        // Create the M-NotifyResp.ind
        NotifyRespInd notifyRespInd = new NotifyRespInd(
                PduHeaders.CURRENT_MMS_VERSION,
                mNotificationInd.getTransactionId(),
                status);

        // Pack M-NotifyResp.ind and send it
        if(MmsConfig.getNotifyWapMMSC()) {
            sendPdu(new PduComposer(mContext, notifyRespInd).make(), mContentLocation);
        } else {
            sendPdu(new PduComposer(mContext, notifyRespInd).make());
        }
    }

    @Override
    public int getType() {
        return NOTIFICATION_TRANSACTION;
    }
    
    @Override
    public boolean isEquivalent(Transaction transaction) {
        boolean isEq = (getClass().equals(transaction.getClass()) || RetrieveTransaction.class.equals(transaction.getClass()))
                && mId.equals(transaction.mId);
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "Same: " + isEq + " Comparing me: " + toString() + " to: " + toString());
    	}
        return isEq;
    }


    
	public static long getMMSThreadId(Context context, long msgId) {
		long threadId = 0;
		String where = Mms._ID + "=" + msgId;
		String[] projection = new String[] { Mms.THREAD_ID };
		Cursor c = SqliteWrapper.query(context,VZUris.getMmsUri(), projection, where, null, null);
		if (c != null) {
			try {
				if (c.moveToNext()) {
					threadId = c.getLong(c.getColumnIndex(Mms.THREAD_ID));
				}
			} finally {
				c.close();
			}
		}
		return threadId;
	}
}
