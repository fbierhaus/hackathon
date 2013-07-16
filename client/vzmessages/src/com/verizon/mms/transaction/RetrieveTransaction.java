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
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.http.HttpStatus;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.util.Log;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.model.SmilHelper;
import com.verizon.mms.pdu.AcknowledgeInd;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduBody;
import com.verizon.mms.pdu.PduComposer;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduParser;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.RetrieveConf;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.util.DownloadManager;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.sync.ConversationDataObserver;

/**
 * The RetrieveTransaction is responsible for retrieving multimedia
 * messages (M-Retrieve.conf) from the MMSC server.  It:
 *
 * <ul>
 * <li>Sends a GET request to the MMSC server.
 * <li>Retrieves the binary M-Retrieve.conf data and parses it.
 * <li>Persists the retrieve multimedia message.
 * <li>Determines whether an acknowledgement is required.
 * <li>Creates appropriate M-Acknowledge.ind and sends it to MMSC server.
 * <li>Notifies the TransactionService about succesful completion.
 * </ul>
 */
public class RetrieveTransaction extends Transaction implements Runnable {
    private final Uri mUri;
    private final String mContentLocation;
    private boolean mLocked;
    private boolean threadIDfailed = false;
    static final String[] PROJECTION = new String[] {
        Mms.CONTENT_LOCATION,
        Mms.LOCKED
    };

    // The indexes of the columns which must be consistent with above PROJECTION.
    static final int COLUMN_CONTENT_LOCATION      = 0;
    static final int COLUMN_LOCKED                = 1;

    public RetrieveTransaction(Context context, int serviceId,
            TransactionSettings connectionSettings, String uri, TransactionBundle args)
            throws MmsException {
        super(context, serviceId, connectionSettings, args);

        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(TransactionService.class, "RetrieveTransaction constructed: uri:"+ uri + " serviceId:"+serviceId);
        }
        if (uri.startsWith("content://")) {
            mUri = Uri.parse(uri); // The Uri of the M-Notification.ind
            mId = mContentLocation = getContentLocation(context, mUri);
            if(Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "X-Mms-Content-Location: " + mContentLocation);
            }
            if ((mContentLocation == null) || (mContentLocation == "")) {
                throw new MmsException("X-Mms-Content-Location null or empty for : " + mUri);            	
            }

        } else {
            throw new IllegalArgumentException(
                    "Initializing from X-Mms-Content-Location is abandoned!");
        }

        // Attach the transaction to the instance of RetryScheduler.
        attach(RetryScheduler.getInstance(context));
    }

    private String getContentLocation(Context context, Uri uri)
            throws MmsException {
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                            uri, PROJECTION, null, null, null);
        mLocked = false;

        if (cursor != null) {
            try {
                if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                    // Get the locked flag from the M-Notification.ind so it can be transferred
                    // to the real message after the download.
                    mLocked = cursor.getInt(COLUMN_LOCKED) == 1;
                    return cursor.getString(COLUMN_CONTENT_LOCATION);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see com.android.mms.transaction.Transaction#process()
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
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(RetrieveTransaction.class,
					new Date() + "===>> Processing transaction ==> type=" + getType()
							+ ", serviceId=" + getServiceId() + ", mId=" + mId);
		}

        try {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(TransactionService.class, "Running RetrieveTransaction");
        	}
            // Change the downloading state of the M-Notification.ind.
            DownloadManager.getInstance().markState(
                    mUri, DownloadManager.STATE_DOWNLOADING);

            // Send GET request to MMSC and retrieve the response data.
            byte[] resp = getPdu(mContentLocation);

            // Parse M-Retrieve.conf
            RetrieveConf retrieveConf = (RetrieveConf) new PduParser(resp).parse();
            if (null == retrieveConf) {
                throw new MmsException("Invalid M-Retrieve.conf PDU.");
            }

            Uri msgUri = null;
            if (isDuplicateMessage(mContext, retrieveConf)) {
            	if (Logger.IS_DEBUG_ENABLED) {
            		Logger.debug(TransactionService.class, "This MMS was already retrieved so mark this transaction as failed");
            	}
                // Mark this transaction as failed to prevent duplicate
                // notification to user.
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mUri);
            } else {
            	// set date to time received rather than sent
            	retrieveConf.setDate(System.currentTimeMillis() / 1000);

            	// Store M-Retrieve.conf into Inbox
                PduPersister persister = PduPersister.getPduPersister(mContext);
                boolean persisted = false;
                PduBody body = retrieveConf.getBody();
                
                try {
                	List<PduBody> pb = SmilHelper.createPartsFromPduBody(mContext, body);
                	
                	//if the pd.size is greater than one then we have a slideshow
                	//split it and save each slide as an individual mms
                	if (pb != null && pb.size() > 1) {
                		int i = 0;
                		int lastIndex = pb.size() - 1;
                		for (PduBody pdu : pb) {
                			retrieveConf.setBody(pdu);
							//apart from first pdu mark the rest as read
                			msgUri = persister.persist(retrieveConf, VZUris.getMmsInboxUri(), i != lastIndex);
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
                } catch(IllegalArgumentException e) {
                	threadIDfailed = true;
                }
                catch (Exception e) {
                	Logger.error("Could not split the slideshow into seperate parts and persist it ", e);
                }
                
                if (!persisted) {
                	retrieveConf.setBody(body);
                	try {
                		msgUri = persister.persist(retrieveConf, VZUris.getMmsInboxUri());
                		threadIDfailed = false;
                	} catch(IllegalArgumentException e) {
                		threadIDfailed = true;
                	}
                }

            	long oldThread = 0;
                if (Logger.IS_DEBUG_ENABLED) {
                	// before deleting M-NotifyResp.ind get the conversation thread from it
                	oldThread = NotificationTransaction.getMMSThreadId(mContext, ContentUris.parseId(mUri));
                }
                // We have successfully downloaded the new MM. Delete the M-NotifyResp.ind from Inbox.    
                if (!threadIDfailed) {
                    // In case if the notification pdu got deleted.the mms provider will return the same uri/Id
                    // for last persisted message. so we are checking type before delete. 
                    String selection = Mms.MESSAGE_TYPE+"="+ PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
	                SqliteWrapper.delete(mContext, mContext.getContentResolver(), mUri, selection, null);
	                deletePendingTableEntry(mContext, mUri);
	
	                if (Logger.IS_DEBUG_ENABLED) {
	                    long newThread = NotificationTransaction.getMMSThreadId(mContext, ContentUris.parseId(msgUri));
	                    Logger.debug(getClass(), "New MMS received: old threadId = " + oldThread +
	                    	", new = " + newThread + ", uri = " + msgUri);
	                }
	
	                
	                // The M-Retrieve.conf has been successfully downloaded.
	                mTransactionState.setState(TransactionState.SUCCESS);
	                mTransactionState.setContentUri(msgUri);
	                // Remember the location the message was downloaded from.
	                // Since it's not critical, it won't fail the transaction.
	                // Copy over the locked flag from the M-Notification.ind in case
	                // the user locked the message before activating the download.
	                updateContentLocation(mContext, msgUri, mContentLocation, mLocked);
	                
	                ConversationDataObserver.onMessageDeleted(oldThread, ContentUris.parseId(mUri), ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
                } else {
                	  mTransactionState.setState(TransactionState.FAILED);
                   	  threadIDfailed = false;
                }
            }

            if (msgUri != null) {
            	long msgId = ContentUris.parseId(msgUri);
            	ConversationDataObserver.onNewMessageAdded(MessageUtils.findMmsThreadId(mContext, msgId), -1, ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
            }
            // remove from here and only do in success case
            // Delete the corresponding M-Notification.ind.
            //SqliteWrapper.delete(mContext, mContext.getContentResolver(),
            //                     mUri, null, null);

//            if (msgUri != null) {
//                // Have to delete messages over limit *after* the delete above. Otherwise,
//                // it would be counted as part of the total.
//                Recycler.getMessageRecycler().deleteOldMessagesInSameThreadAsMessage(mContext, msgUri);
//            }

            // Send ACK to the Proxy-Relay to indicate we have fetched the
            // MM successfully.
            // Don't mark the transaction as failed if we failed to send it.
            sendAcknowledgeInd(retrieveConf);

        } catch (Throwable t) {
        	checkError(mTransactionState, t);

        	// no need to report to ACRA as transactions do fail sometimes
        	Logger.error(false, getClass(), Log.getStackTraceString(t));

        } finally {
            if (mTransactionState.getState() != TransactionState.SUCCESS) {
                mTransactionState.setState(TransactionState.FAILED);
                mTransactionState.setContentUri(mUri);
                
                long msgId = ContentUris.parseId(mUri);
                ConversationDataObserver.onMessageStatusChanged(MessageUtils.findMmsThreadId(mContext, msgId), msgId, ConversationDataObserver.MSG_TYPE_MMS, ConversationDataObserver.MSG_SRC_TELEPHONY);
            	// no need to report to ACRA as transactions do fail sometimes
                if(Logger.IS_ERROR_ENABLED){
                    Logger.error(false, getClass(), "Retrieval failed.uri="+mUri);
                }
            }
            notifyObservers();
        }
    }

    static void checkError(TransactionState state, Throwable t) {
    	// if there is an HTTPException in the chain then check its status code
    	for (Throwable e = t; e != null; e = e.getCause()) {
    		if (e instanceof HTTPException) {
    			final int statusCode = ((HTTPException)e).getStatusCode();
    			if (statusCode == HttpStatus.SC_NOT_FOUND) {
        			state.setError(PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_MESSAGE_NOT_FOUND);
    			}
    			break;
    		}
    	}
	}

	private static boolean isDuplicateMessage(Context context, RetrieveConf rc) {
        byte[] rawMessageId = rc.getMessageId();
        if (rawMessageId != null) {
            String messageId = new String(rawMessageId);
            String selection = "(" + Mms.MESSAGE_ID + " = ? AND "
                                   + Mms.MESSAGE_TYPE + " = ?)";
            String[] selectionArgs = new String[] { messageId,
                    String.valueOf(PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) };
            Cursor cursor = SqliteWrapper.query(
                    context, context.getContentResolver(),
                    VZUris.getMmsUri(), new String[] { Mms._ID },
                    selection, selectionArgs, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() > 0) {
                        // We already received the same message before.
                        return true;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return false;
    }

    private void sendAcknowledgeInd(RetrieveConf rc ) throws MmsException, IOException {
        // Send M-Acknowledge.ind to MMSC if required.
        // If the Transaction-ID isn't set in the M-Retrieve.conf, it means
        // the MMS proxy-relay doesn't require an ACK.
        byte[] tranId = rc.getTransactionId();
        if (tranId != null) {
            // Create M-Acknowledge.ind
            AcknowledgeInd acknowledgeInd = new AcknowledgeInd(
                    PduHeaders.CURRENT_MMS_VERSION, tranId);
                     // insert the 'from' address per spec
            String lineNumber = MessageUtils.getLocalNumber();
            if (lineNumber != null && lineNumber.length() != 0) {
            	acknowledgeInd.setFrom(new EncodedStringValue(lineNumber));
            }

            // Pack M-Acknowledge.ind and send it
            if(MmsConfig.getNotifyWapMMSC()) {
                sendPdu(new PduComposer(mContext, acknowledgeInd).make(), mContentLocation);
            } else {
                sendPdu(new PduComposer(mContext, acknowledgeInd).make());
            }
        }
    }

    private static void updateContentLocation(Context context, Uri uri,
                                              String contentLocation,
                                              boolean locked) {
        ContentValues values = new ContentValues(2);
        values.put(Mms.CONTENT_LOCATION, contentLocation);
        values.put(Mms.LOCKED, locked);     // preserve the state of the M-Notification.ind lock.
        SqliteWrapper.update(context, context.getContentResolver(),
                             uri, values, null, null);
    }

    @Override
    public int getType() {
        return RETRIEVE_TRANSACTION;
    }
    
    @Override
    public boolean isEquivalent(Transaction transaction) {
        boolean isEq = (getClass().equals(transaction.getClass()) || NotificationTransaction.class.equals(transaction.getClass()))
                && mId.equals(transaction.mId);
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "Same: " + isEq + " Comparing me: " + toString() + " to: " + transaction.toString());
    	}
        return isEq;
    }
}
