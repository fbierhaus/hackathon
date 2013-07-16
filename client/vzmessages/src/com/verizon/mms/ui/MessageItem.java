/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.ui;

import static com.verizon.mms.pdu.PduHeaders.BCC;
import static com.verizon.mms.pdu.PduHeaders.CC;
import static com.verizon.mms.pdu.PduHeaders.FROM;
import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_DELIVERY_IND;
import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_READ_ORIG_IND;
import static com.verizon.mms.pdu.PduHeaders.READ_STATUS_READ;
import static com.verizon.mms.pdu.PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ;
import static com.verizon.mms.pdu.PduHeaders.STATUS_FORWARDED;
import static com.verizon.mms.pdu.PduHeaders.STATUS_RETRIEVED;
import static com.verizon.mms.pdu.PduHeaders.TO;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_ID;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_DATE;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_DELIVERY_REPORT;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_ERROR_TYPE;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_LOCKED;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_MESSAGE_BOX;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_MESSAGE_TYPE;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_READ_REPORT;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_RESPONSE_STATUS;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_STATUS;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_SUBJECT;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_SUBJECT_CHARSET;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_TRANSACTION_ID;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_SMS_ADDRESS;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_SMS_BODY;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_SMS_DATE;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_SMS_ERROR_CODE;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_SMS_LOCKED;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_SMS_STATUS;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_SMS_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Addr;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.text.TextUtils;

import com.rocketmobile.asimov.Asimov;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.mms.InvalidMessageException;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.WorkingMessage;
import com.verizon.mms.model.ImageModel;
import com.verizon.mms.model.SlideModel;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.model.TextModel;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.GenericPdu;
import com.verizon.mms.pdu.MultimediaMessagePdu;
import com.verizon.mms.pdu.NotificationInd;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.RetrieveConf;
import com.verizon.mms.pdu.SendReq;
import com.verizon.mms.util.AddressUtils;
import com.verizon.mms.util.DownloadManager;
import com.verizon.mms.util.MemoryItem;
import com.verizon.mms.util.SqliteWrapper;


/**
 * Mostly immutable model for an SMS/MMS message.
 * 
 * <p>
 * The only mutable field is the cached formatted message member, the formatting of which is done outside this
 * model in MessageListItem.
 */
public class MessageItem implements MemoryItem {

   
	/*
	 * Outgoing messages go from sending to sent if delivery reports are off, or to pending if they're on,
	 * then to delivered, delivered to some (if multiple recipients), or failed.
	 */
	public enum DeliveryStatus {
		NONE(R.string.status_none, false),
		DRAFT(R.string.status_draft, false),
		SENDING(R.string.status_sending, false),                      // in process of being sent
		SENT(R.string.status_sent, true),                             // sent and no reports requested
		PENDING(R.string.status_sent, false),                         // sent and waiting for reports
		DELIVERED_TO_SOME(R.string.status_delivered_to_some, false),  // delivered to some recipients
		DELIVERED(R.string.status_delivered, true),                   // delivered to all recipients
		FAILED(R.string.status_failed, true),                         // failed to all
		UNREAD(R.string.status_delivered, true),                      // delivered to all and waiting for read reports
		READ_BY_SOME(R.string.status_read_by_some, true),             // read by some
		READ(R.string.status_read, true),                             // read by all
		DELETED(R.string.status_deleted, true),                       // deleted before being read by all
		RECEIVED(R.string.status_received, true);                     // inbound message

		private int stringId;
		private boolean terminal;

		private DeliveryStatus(int stringId, boolean terminal) {
			this.stringId = stringId;
			this.terminal = terminal;
		}

		public int getStringId() {
			return stringId;
		}

		public boolean isTerminal() {
			return terminal;
		}
	}

	public enum GroupMode {
		GROUP(R.string.group_mode_group),    // all recipients get replies (mms with to/cc)
		SENDER(R.string.group_mode_sender);  // only sender sees replies (mms with bcc or sms)

		private int message;

		private GroupMode(int message) {
			this.message = message;
		}

		public String getMessage(Context context, boolean first) {
			final int resid = first ? R.string.group_set : R.string.group_change;
			return context.getString(resid, context.getString(message));
		}
	}

	public static class ReportStatus {
		public boolean delivered;
		public boolean read;
		public boolean deleted;
		public boolean failed;

		public String toString() {
			return "delivered = " + delivered + ", read = " + read +
				", deleted = " + deleted + ", failed = " + failed;
		}
	}


	final Context mContext;
	String mType;
	final long mMsgId;
	int mBoxId;

	DeliveryStatus mDeliveryStatus;
	boolean mSms;
	private final static String self;
	private boolean mDeliveryReportRequested;
	private boolean mReadReportRequested;
	boolean mLocked; // locked to prevent auto-deletion

	private String mTimestamp;
	long mMsgTime;
	String mAddress;
	Contact mRawContact; // null for outgoing messages
	String mContact; // display format of contact, null for outgoing messages
	String mBody; // Body of SMS, first text of MMS.
	String mTextContentType; // ContentType of text of MMS.
	Pattern mHighlight; // portion of message to highlight (from search)

	// Fields for MMS only.
	Uri mMessageUri;
	int mMessageType;
	int mAttachmentType = WorkingMessage.NONE;
	/**
	 * This will check whether message is thubmail
	 */
	String mContentDisposoition;
	String mSubject;
	volatile SlideshowModel mSlideshow;
	volatile int mMessageSize;
	int mErrorType;
	int mErrorCode;
	int mMmsStatus;
	int mMmsResponseStatus;
	GroupMode mGroupMode;
	private HashMap<String, ReportStatus> mReportStatus;
	private GenericPdu mPdu;
	volatile boolean mLoaded;
	volatile boolean mMediaLoaded = false;  //indicates if the first slide that has to be displayed from the slideshow is loaded
	volatile boolean mStatusLoaded = false; //indicates if the status of an mms isloaded
	String mXid;
	int mTextColor = -1;
	int mRecipColor = -1;
	private volatile OnLoadedListener onLoadedListener;

	private static final String[] MMS_REPORT_COLS = {
		Mms._ID,
		Mms.MESSAGE_TYPE,
		Mms.STATUS,
		Mms.READ_STATUS
	};

	private static final int REPORT_COLUMN_ID = 0;
	private static final int REPORT_COLUMN_TYPE = 1;
	private static final int REPORT_COLUMN_STATUS = 2;
	private static final int REPORT_COLUMN_READ_STATUS = 3;

	// query for delivery and read reports for a given message id
	private static final String MMS_REPORT_WHERE =
		Mms.MESSAGE_ID + "= ? AND (" +
		Mms.MESSAGE_TYPE + "=" + MESSAGE_TYPE_DELIVERY_IND + " OR " +
		Mms.MESSAGE_TYPE + "=" + MESSAGE_TYPE_READ_ORIG_IND + ")";

	private static final String[] ADDR_ADDRESS_COL = { Addr.ADDRESS };

	private static final String ADDR_WHERE = Addr.TYPE + "=?";

	private static final String[] MMS_MESSAGE_ID_COL = { Mms.MESSAGE_ID };

	private static final int[] TO_HEADERS = { TO, CC, BCC };

	// SMS status values (see TP-Status section 9.2.3.15)
	private static final int SMS_STATUS_NONE = -1;  // default when not set
	public static final int SMS_STATUS_MASK = 0x7f;  // status is low 7 bits
	private static final int SMS_STATUS_DELIVERY_MAX = 0x02;
	private static final int SMS_STATUS_TEMP_ERROR_BIT = 0x20;
	private static final int SMS_STATUS_PERM_ERROR_BIT = 0x40;
	private static final int SMS_STATUS_PENDING = 0x40;  // some devices set this while sending if delivery reports are enabled

	public static class MMSData {
	    public int mAttachmentType;
	    public String mBody;
	    
	    public MMSData(int attachType, String body) {
	        mAttachmentType = attachType;
	        mBody = body;
	    }
	}


	public interface OnLoadedListener {
		public void onMessageLoaded(MessageItem item);
	}


	static {
		self = Asimov.getApplication().getString(R.string.me);
	}


	public MessageItem(Context context, Cursor cursor, Pattern highlight, boolean loadSlideShow, boolean loadMms) throws MmsException {
		mContext = context;
		mMsgId = cursor.getLong(COLUMN_ID);
		mHighlight = highlight;
		mType = cursor.getString(MessageListAdapter.COLUMN_MSG_TYPE);
		mSms = mType.equals("sms");

		if (mSms) {
			mBoxId = cursor.getInt(COLUMN_SMS_TYPE);

			final int status = cursor.getInt(COLUMN_SMS_STATUS);
			mDeliveryStatus = getSmsStatus(mBoxId, status);

			mMessageUri = ContentUris.withAppendedId(VZUris.getSmsUri(), mMsgId);
			// Set contact and message body
			mAddress = cursor.getString(COLUMN_SMS_ADDRESS);
			if (!Sms.isOutgoingFolder(mBoxId)) {
				// For incoming messages, the ADDRESS field contains the sender.
				setContact(mAddress);
			}
			mBody = cursor.getString(COLUMN_SMS_BODY);
			if (mBody != null) {
				mMessageSize = mBody.length();
			}
			mMsgTime = cursor.getLong(COLUMN_SMS_DATE);
			mLocked = cursor.getInt(COLUMN_SMS_LOCKED) != 0;
			mErrorCode = cursor.getInt(COLUMN_SMS_ERROR_CODE);
			if (isOutboundMessage()) {
				mGroupMode = GroupMode.SENDER;
			}
			mLoaded = true;
			mMediaLoaded = true;
			mStatusLoaded = true;
		}
		else { // mms
			mMessageUri = ContentUris.withAppendedId(VZUris.getMmsUri(), mMsgId);
			mBoxId = cursor.getInt(COLUMN_MMS_MESSAGE_BOX);
			mMessageType = cursor.getInt(COLUMN_MMS_MESSAGE_TYPE);
			mErrorType = cursor.getInt(COLUMN_MMS_ERROR_TYPE);
			mMmsStatus = cursor.getInt(COLUMN_MMS_STATUS);
			mMmsResponseStatus = cursor.getInt(COLUMN_MMS_RESPONSE_STATUS);
			String subject = cursor.getString(COLUMN_MMS_SUBJECT);
			if (!TextUtils.isEmpty(subject)) {
				final EncodedStringValue v = new EncodedStringValue(cursor.getInt(COLUMN_MMS_SUBJECT_CHARSET),
					PduPersister.getBytes(subject));
				mSubject = v.getString();
			}
			mLocked = cursor.getInt(COLUMN_MMS_LOCKED) != 0;
			mMsgTime = cursor.getLong(COLUMN_MMS_DATE) * 1000;
			mXid = cursor.getString(COLUMN_MMS_TRANSACTION_ID);

			// get read and delivery report requests
			mDeliveryReportRequested = getReport(cursor.getString(COLUMN_MMS_DELIVERY_REPORT));
			mReadReportRequested = getReport(cursor.getString(COLUMN_MMS_READ_REPORT));

			if (loadMms) {
				loadMms(context, loadSlideShow);
			}
			else {
				mBody = "";
				if ((mMessageType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) 
					|| (mMessageType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X)) {
					mDeliveryStatus = DeliveryStatus.NONE;
					setContact("");
				}
				else if (mMessageType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) {
					setContact("");
				}
				else {  // SEND_REQ
					// use self string for outgoing messages
					mAddress = self;
				}
			}
			mLoaded = loadMms;
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "new item = " + this);
		}
	}

	public void loadMms(Context context) {
		if (mSms) {
			throw new UnsupportedOperationException("Calling loadMms on an SMS message");
		}
		synchronized (this) {
			if (!mLoaded || !mStatusLoaded) {
				try {
					if (!mLoaded) {
						loadMms(context, true);
					}
					if (!mStatusLoaded || !mLoaded) {
						getMmsStatus();
					}
					mStatusLoaded = mMediaLoaded = mLoaded = true;
					if (onLoadedListener != null) {
						onLoadedListener.onMessageLoaded(this);
					}
				}
				catch(Exception e) {
					Logger.error(getClass(), "Error loading MMS:", e);
				}
			}
		}
	}

	private void loadMms(Context context, boolean loadSlideshow) throws MmsException {
		final PduPersister p = PduPersister.getPduPersister(context);
		if ((mMessageType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND)
				|| (mMessageType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X)) {
			mDeliveryStatus = DeliveryStatus.NONE;
			mMediaLoaded = true;
			try {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "loadMms - uri:" + mMessageUri);
				}
				final NotificationInd notifInd = (NotificationInd)p.load(mMessageUri);
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "loadMms - loaded");
				}
				mPdu = notifInd;
				interpretFrom(notifInd.getFrom(), mMessageUri);
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "loadMms - interpretFrom returned");
				}
				// Borrow the mBody to hold the URL of the message.
				mBody = new String(notifInd.getContentLocation());
				mMessageSize = (int)notifInd.getMessageSize();
			}
			catch (InvalidMessageException e) {
				// notification messages get deleted once they're downloaded, so assume that's the
				// case and dummy up a downloading notification item until the cursor gets refreshed
				//
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "loadMms - got Exception");
				}
				mPdu = new NotificationInd();
				mMmsStatus = DownloadManager.STATE_DOWNLOADING;
				mAddress = mContext.getString(R.string.unknown_sender);
				setContact(mAddress);
			}
		}
		else if (loadSlideshow) {
			final MultimediaMessagePdu msg = (MultimediaMessagePdu)p.load(mMessageUri);
			mPdu = msg;
			loadSlideshow(context, SlideshowModel.createConversationSlideShowFromPduBody(context, msg.getBody()));

			if (mMessageType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) {
				final RetrieveConf retrieveConf = (RetrieveConf)msg;
				interpretFrom(retrieveConf.getFrom(), mMessageUri);
			}
			else {
				// use self string for outgoing messages
				mAddress = self;

				// if there were BCC recipients then the group mode is SENDER, otherwise GROUP
				if (msg instanceof SendReq) {
					final EncodedStringValue[] values = ((SendReq)msg).getBcc();
					if (values != null && values.length > 0) {
						mGroupMode = GroupMode.SENDER;
					}
					else {
						mGroupMode = GroupMode.GROUP;
					}
				}
			}
		}
		else {
			MMSData mmsData = SlideshowModel.getBodyAttachmentType(context, p, mMessageUri);
			mAttachmentType = mmsData.mAttachmentType;
			mBody = mmsData.mBody;
		}
	}

	public void loadSlideshow(Context context, SlideshowModel slideshow) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "loadSlideshow: slideshow = " + slideshow);
		}
		
		if (slideshow == null) {
			return;
		}

		mAttachmentType = MessageUtils.getAttachmentType(slideshow);

		final SlideModel slide = slideshow.get(0);
		if ((slide != null) && slide.hasText()) {
			TextModel tm = slide.getText();
			if (tm.isDrmProtected()) {
				mBody = context.getString(R.string.drm_protected_text);
			}
			else {
				final String body = tm.getText();
				// when there is no text in the text part the cursor tends to return
				// a new line character which makes the UI look unpleasant
				if (body.length() == 1) {
					mBody = body.trim();
				}
				else {
					mBody = body;
				}
			}
			mTextContentType = tm.getContentType();
			
		}
		else {
			mBody = "";
		}
		
		// Extract the content disposition to show if it is thumbnail
		if(slide!= null && slide.hasImage()){
		    ImageModel im = slide.getImage();
		    mContentDisposoition = im.getContentDisposition();
		}
		
		mMessageSize = slideshow.getActualMessageSize();
		if (mSubject != null) {
			mMessageSize += mSubject.length();
		}

		mSlideshow = slideshow;
		if (slideshow.isSlideShowLoaded(null) && mStatusLoaded) {
			mMediaLoaded = true;
		}
	}

	/**
	 * @return the amount of memory in bytes currently used by the message's content
	 */
	@Override
	public int getMemorySize() {
		int size;
		final SlideshowModel model;
		if (mSms || (model = mSlideshow) == null) {
			size = mMessageSize * 2;
		}
		else {
			// loaded MMS message
			size = model.getMemorySize();
			if (mSubject != null) {
				size += mSubject.length() * 2;
			}
		}
		return size;
	}

	private boolean getReport(String report) {
		if (report != null && report.length() != 0) {
			try {
				return Integer.parseInt(report) == PduHeaders.VALUE_YES;
			}
			catch (Exception e) {
				Logger.error(getClass(),e);
			}
		}
		return false;
	}

	public static DeliveryStatus getSmsStatus(int boxId, int status) {
		final DeliveryStatus stat;
		if (status == SMS_STATUS_NONE) {
			stat = getStatusFromBox(true, boxId, false);
		}
		else if (status == SMS_STATUS_PENDING && boxId == Sms.MESSAGE_TYPE_QUEUED) {
			// some devices set the status to this when first sending if delivery reports are enabled
			stat = DeliveryStatus.SENDING;
		}
		else {
			status = normalizeStatus(status);
			if (status <= SMS_STATUS_DELIVERY_MAX) {
				stat = DeliveryStatus.DELIVERED;
			}
			else if ((status & SMS_STATUS_PERM_ERROR_BIT) != 0) {
				stat = DeliveryStatus.FAILED;
			}
			else if ((status & SMS_STATUS_TEMP_ERROR_BIT) != 0) {
				if (boxId == Sms.MESSAGE_TYPE_QUEUED || boxId == Sms.MESSAGE_TYPE_OUTBOX) {
					stat = DeliveryStatus.SENDING;
				}
				else {
					stat = DeliveryStatus.PENDING;
				}
			}
			else {
				stat = getStatusFromBox(true, boxId, false);
			}
		}

		return stat;
	}

	public static int normalizeStatus(int status) {
		// some devices move the status to the upper 16 bits so check them
		final int upper = (status >> 16) & SMS_STATUS_MASK;
		if (upper != 0) {
			status = upper;
		}
		else {
			status &= SMS_STATUS_MASK;
		}
		return status;
	}

	private DeliveryStatus getMmsStatus() {
		DeliveryStatus stat = null;

		// if the overall message is marked as failed then that takes priority
		if (isFailedMessage(null)) {
			stat = DeliveryStatus.FAILED;
		}
		else {
			if (isOutboundMessage()) {
				// check if there are any status reports for this message
				final HashMap<String, ReportStatus> reportStatus = getMmsReportStatus();
	
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "getMmsStatus: " + this + ": report status = " + reportStatus);
				}

				final int recipients = reportStatus.size();
				if (recipients != 0) {
					int delivered = 0;
					int read = 0;
					int deleted = 0;
					int failed = 0;
					for (ReportStatus status : reportStatus.values()) {
						if (status.read) {
							++read;
						}
						else if (status.deleted) {
							++deleted;
						}
						else if (status.delivered) {
							++delivered;
						}
						else if (status.failed) {
							++failed;
						}
					}
					if (failed >= recipients) {
						stat = DeliveryStatus.FAILED;
					}
					else if (read >= recipients) {
						stat = DeliveryStatus.READ;
					}
					else if (read != 0) {
						stat = DeliveryStatus.READ_BY_SOME;
					}
					else if (deleted >= recipients) {
						stat = DeliveryStatus.DELETED;
					}
					else if (delivered >= recipients) {
						if (mReadReportRequested) {
							// waiting for read reports
							stat = DeliveryStatus.UNREAD;
						}
						else {
							stat = DeliveryStatus.DELIVERED;
						}
					}
					else if (delivered != 0) {
						stat = DeliveryStatus.DELIVERED_TO_SOME;
					}
				}
			}

			if (stat == null) {
				// failures are sometimes reported in the response status
				final int status = mMmsResponseStatus;
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "getMmsStatus: response status for " + mMsgId + " = " + status);
				}
				if (status != 0 && status != PduHeaders.RESPONSE_STATUS_OK) {
					stat = DeliveryStatus.FAILED;
				}
			}
	
			if (stat == null) {
				final boolean reportRequested = mDeliveryReportRequested || mReadReportRequested;
				stat = getStatusFromBox(false, mBoxId, reportRequested);
			}
		}

		return mDeliveryStatus = stat;
	}
	
	public void resetDeliveryStatus() {
		mDeliveryStatus = null;
	}

	public HashMap<String, ReportStatus> getMmsReportStatus() {
		if (mReportStatus == null) {
			// get the recipients and status for this message
			final List<String> addrs = getMmsRecipients();
			final HashMap<String, ReportStatus> stat = getMmsReportStatus(mContext, Long.toString(mMsgId));

			// add entries for recipients with no status reports
			for (String addr : addrs) {
				if (!stat.containsKey(addr)) {
					stat.put(addr, new ReportStatus());
				}
			}

			mReportStatus = stat;
		}

		return mReportStatus;
	}

	public static HashMap<String, ReportStatus> getMmsReportStatus(Context context, String msgId) {
		final ContentResolver res = context.getContentResolver();
		final HashMap<String, ReportStatus> stat = new HashMap<String, ReportStatus>();

		// get the m_id from the sent message
		final Uri mmsUri = VZUris.getMmsUri();
		final Uri msgUri = Uri.withAppendedPath(mmsUri, msgId);
		String mid = null;
		Cursor cursor = null;
		try {
			cursor = SqliteWrapper.query(context, res, msgUri, MMS_MESSAGE_ID_COL, null, null, null);
			if (cursor != null && cursor.moveToNext()) {
				mid = cursor.getString(0);
			}
		}
		catch (Exception e) {
			Logger.error(e);
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MessageItem.class, "getMmsReportStatus: mid for " + msgId + " = " + mid);
		}

		if (mid != null) {
			// get the ids and status of any received reports for this message
			final String[] mmsArgs = { mid };
			cursor = null;
			try {
				cursor = SqliteWrapper.query(context, res, mmsUri, MMS_REPORT_COLS, MMS_REPORT_WHERE, mmsArgs, null);
				if (cursor != null) {
					while (cursor.moveToNext()) {
						final String rptMsgId = cursor.getString(REPORT_COLUMN_ID);
						final int type = cursor.getInt(REPORT_COLUMN_TYPE);
						final int col = type == MESSAGE_TYPE_DELIVERY_IND ? REPORT_COLUMN_STATUS : REPORT_COLUMN_READ_STATUS;
						final String statStr = cursor.getString(col);
						int status;
						try {
							status = Integer.parseInt(statStr);
						}
						catch (Exception e) {
							Logger.error(MessageItem.class, "error converting status <" + statStr + ">:", e);
							status = 0;
						}

						// get sender addr of this report status message
						Cursor addrCursor = null;
						try {
							final Uri addrUri = mmsUri.buildUpon().appendPath(rptMsgId).appendPath("addr").build();
							final int header = type == MESSAGE_TYPE_DELIVERY_IND ? TO : FROM;
							final String[] args = { Integer.toString(header) };
							addrCursor = SqliteWrapper.query(context, res, addrUri, ADDR_ADDRESS_COL, ADDR_WHERE, args, null);
							if (addrCursor != null) {
								while (addrCursor.moveToNext()) {
									// get address and get or create a report status object for it
									final String addr = MessageUtils.normalizeMmsAddress(addrCursor.getString(0));
									ReportStatus rstat = stat.get(addr);
									if (rstat == null) {
										rstat = new ReportStatus();
										stat.put(addr, rstat);
									}

									// set status fields
									if (type == MESSAGE_TYPE_DELIVERY_IND) {
										if (status == STATUS_RETRIEVED || status == STATUS_FORWARDED) {
											rstat.delivered = true;
										}
										else if (status != 0) {
											rstat.failed = true;
										}
									}
									else {  // MESSAGE_TYPE_READ_ORIG_IND
										if (status == READ_STATUS_READ) {
											rstat.read = true;
										}
										else if (status == READ_STATUS__DELETED_WITHOUT_BEING_READ) {
											rstat.deleted = true;
										}
									}

									if (Logger.IS_DEBUG_ENABLED) {
										Logger.debug(MessageItem.class, "getMmsReportStatus: addr = " + addr +
											", type = " + type + ", status = " + status);
									}
								}
							}
						}
						catch (Exception e) {
							Logger.error(e);
						}
						finally {
							if (addrCursor != null) {
								addrCursor.close();
							}
						}
					}
				}
			}
			catch (Exception e) {
				Logger.error(e);
			}
			finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}

		return stat;
	}

	public List<String> getMmsRecipients() {
		return getMmsRecipients(mPdu, !isOutboundMessage());
	}

	public static List<String> getMmsRecipients(GenericPdu pdu, boolean addLocal) {
		final ArrayList<String> addrs = new ArrayList<String>();

		if (pdu != null) {
			// don't check for the local address if caller doesn't want it
			boolean hasLocal = !addLocal;

			for (String headerAddr : getMmsRecipientAddrs(pdu)) {
				final String addr = MessageUtils.normalizeMmsAddress(headerAddr);
				addrs.add(addr);

				// check if the local number is in the headers
				if (!hasLocal) {
					hasLocal = MessageUtils.isLocalNumber(addr);
				}
			}

			// add local number if desired and it wasn't in the headers
			if (addLocal && !hasLocal) {
				final String local = MessageUtils.getLocalNumber();
				if (local != null && local.length() != 0) {
					addrs.add(local);
				}
			}
		}

		return addrs;
	}

	/**
	 * Get the original recipient address(es) for this message.
	 */
	private static List<String> getMmsRecipientAddrs(GenericPdu pdu) {
		final ArrayList<String> addrs = new ArrayList<String>();

		if (pdu.getMessageType() == MESSAGE_TYPE_READ_ORIG_IND) {
			// the from header contains the original message recipient
			final EncodedStringValue value = pdu.getFrom();
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(MessageItem.class, "getMmsRecipientAddrs: from = " + value);
			}
			if (value != null) {
				addrs.add(value.toString());
			}
		}
		else {
			for (int header : TO_HEADERS) {
				final EncodedStringValue[] values = pdu.getHeader(header);
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(MessageItem.class, "getMmsRecipientAddrs: header " + header + " = " +
						(values == null ? "null" : Arrays.asList(values)));
				}
				if (values != null) {
					for (EncodedStringValue value : values) {
						addrs.add(value.toString());
					}
				}
			}
		}

		return addrs;
	}

	public static DeliveryStatus getStatusFromBox(boolean sms, int boxId, boolean reportRequested) {
		final DeliveryStatus ret;
		if (sms) {
			switch (boxId) {
				case Sms.MESSAGE_TYPE_SENT:
					ret = DeliveryStatus.SENT;
					break;
				case Sms.MESSAGE_TYPE_INBOX:
					ret = DeliveryStatus.RECEIVED;
					break;
				case Sms.MESSAGE_TYPE_FAILED:
					ret = DeliveryStatus.FAILED;
					break;
				case Sms.MESSAGE_TYPE_QUEUED:
				case Sms.MESSAGE_TYPE_OUTBOX:
					ret = DeliveryStatus.SENDING;
					break;
				case Sms.MESSAGE_TYPE_DRAFT:
					ret = DeliveryStatus.DRAFT;
					break;
				default:
					ret = DeliveryStatus.NONE;
					Logger.error(MessageItem.class,"MessageItem.getStatusFromBox: no delivery status for sms box " + boxId);
					break;
			}
		}
		else {
			switch (boxId) {
				case Mms.MESSAGE_BOX_SENT:
					ret = reportRequested ? DeliveryStatus.PENDING : DeliveryStatus.SENT;
					break;
				case Mms.MESSAGE_BOX_INBOX:
					ret = DeliveryStatus.RECEIVED;
					break;
				case Mms.MESSAGE_BOX_ALL:
				case Mms.MESSAGE_BOX_OUTBOX:
					ret = DeliveryStatus.SENDING;
					break;
				case Mms.MESSAGE_BOX_DRAFTS:
					ret = DeliveryStatus.DRAFT;
					break;
				default:
					ret = DeliveryStatus.NONE;
					Logger.error(MessageItem.class,"MessageItem.getStatusFromBox: no delivery status for mms box " + boxId);
					break;
			}
		}
		return ret;
	}

	public String getStatusText(Context context, DeliveryStatus stat) {
		final int resid;
		if (stat == DeliveryStatus.FAILED) {
			switch (mMmsResponseStatus) {
				case PduHeaders.RESPONSE_STATUS_ERROR_UNSPECIFIED:
					resid = R.string.mms_error_unspecified;
					break;
				case PduHeaders.RESPONSE_STATUS_ERROR_SERVICE_DENIED:
					resid = R.string.mms_error_service_denied;
					break;
				case PduHeaders.RESPONSE_STATUS_ERROR_MESSAGE_FORMAT_CORRUPT:
					resid = R.string.mms_error_message_format_corrupt;
					break;
				case PduHeaders.RESPONSE_STATUS_ERROR_SENDING_ADDRESS_UNRESOLVED:
					resid = R.string.mms_error_sending_address_unresolved;
					break;
				case PduHeaders.RESPONSE_STATUS_ERROR_MESSAGE_NOT_FOUND:
					resid = R.string.mms_error_message_not_found;
					break;
				case PduHeaders.RESPONSE_STATUS_ERROR_NETWORK_PROBLEM:
					resid = R.string.mms_error_network_problem;
					break;
				case PduHeaders.RESPONSE_STATUS_ERROR_CONTENT_NOT_ACCEPTED:
					resid = R.string.mms_error_content_not_accepted;
					break;
				case PduHeaders.RESPONSE_STATUS_ERROR_UNSUPPORTED_MESSAGE:
					resid = R.string.mms_error_unsupported_message;
					break;
				case PduHeaders.RESPONSE_STATUS_ERROR_PERMANENT_MESSAGE_NOT_FOUND:
					resid = R.string.mms_error_expired_or_unavailable;
					break;
				default:
					resid = stat.getStringId();
					break;
			}
		}
		else {
			resid = stat.getStringId();
		}

		return context.getString(resid);
	}

	private void interpretFrom(EncodedStringValue from, Uri messageUri) {
		if (from != null) {
			mAddress = from.getString();
		}
		else {
			// In the rare case when getting the "from" address from the pdu fails,
			// (e.g. from == null) fall back to a slower, yet more reliable method of
			// getting the address from the "addr" table. This is what the Messaging
			// notification system uses.
			mAddress = AddressUtils.getFrom(mContext, messageUri);
		}
		if (mAddress == null || mAddress.length() == 0) {
			setContact("");
		}
		else {
			setContact(mAddress);
		}
	}

	private void setContact(String address) {
		final Contact contact = mRawContact = Contact.get(address, false);
		mContact = contact.getName();
	}

	public boolean isMms() {
		return !mSms;
	}

	public boolean isSms() {
		return mSms;
	}

	public boolean isDownloaded() {
		return ((mMessageType != PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) 
				&& (mMessageType != PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X));
	}

	/**
	 * Returns true if the message is in the process of being sent or has failed.
	 */
	public boolean isOutgoingMessage() {
		final int box = mBoxId;
        return mSms ?
        	box == Sms.MESSAGE_TYPE_FAILED || box == Sms.MESSAGE_TYPE_OUTBOX || box == Sms.MESSAGE_TYPE_QUEUED :
        	box == Mms.MESSAGE_BOX_OUTBOX;
	}

	/**
	 * Returns true if the message is being or has been sent.
	 */
	public boolean isOutboundMessage() {
		return mRawContact == null;
	}

	public boolean isFailedMessage(boolean canBlock) {
		return isFailedMessage(getDeliveryStatus(canBlock));
	}

	public boolean isFailedMessage(DeliveryStatus status) {
		final boolean failed;
		if (mSms) {
	        failed = status == DeliveryStatus.FAILED || mBoxId == Sms.MESSAGE_TYPE_FAILED;
		}
		else {
			failed = status == DeliveryStatus.FAILED || isFailedMmsMessage();
		}
		return failed;
	}

	/**
	 * @return true if there is a global error for the message
	 */
	public boolean isFailedMmsMessage() {
		final int status = mMmsResponseStatus;
		return (status != 0 && status != PduHeaders.RESPONSE_STATUS_OK) ||
			mErrorType >= MmsSms.ERR_TYPE_GENERIC_PERMANENT;
	}

	public int getBoxId() {
		return mBoxId;
	}

	public String getSubject() {
		return mSubject;
	}

	public String getBody() {
		return mBody;
	}

	public int getAttachmentType() {
		return mAttachmentType;
	}

	public DeliveryStatus getDeliveryStatus(boolean canBlock) {
		DeliveryStatus status = mDeliveryStatus;
		if (status == null) {
			// mms status is deferred since it requires a DB lookup; sms status should always be defined
			if (mSms) {
				status = DeliveryStatus.NONE;
			}
			else if (canBlock) {
				status = getMmsStatus();
			}
		}
		return status;
	}

	public String getTimestamp() {
		if (mTimestamp == null && mMsgTime != 0) {
			mTimestamp = MessageUtils.formatTimeStampString(mMsgTime, true);
		}
		return mTimestamp;
	}

	public void setOnLoadedListener(OnLoadedListener onLoadedListener) {
		this.onLoadedListener = onLoadedListener;
	}

	public String getAddress() {
		return mAddress;
	}

	@Override
	public String toString() {
        return super.toString() + ": type = " + mType + ", slides = " + (mSlideshow != null) + " mLoaded " + mLoaded + " mMediaLoaded " + mMediaLoaded
        	+ ", box = " + mBoxId + ", uri = " + mMessageUri +
        	", address = " + mAddress + ", contact = " + mContact + ", readReq = " + mReadReportRequested +
        	", delReq = " + mDeliveryReportRequested + ", time = " + mMsgTime + " = " + getTimestamp() +
        	", delStat = " + mDeliveryStatus + ", msgSize = " + mMessageSize + ", memSize = " + getMemorySize() +
        	", status = " + mStatusLoaded + ", xid = " + mXid
        	+",ContentDisposoition = "+ mContentDisposoition;
	}


	public int getMessageSource() {
	       String where = VMAMapping._LUID + "=" + mMsgId +" AND "+ VMAMapping._TYPE+"="+(mSms?1:2);
           String[] projection = new String[] {VMAMapping._SOURCE };
           Cursor cursor = SqliteWrapper.query(mContext,VMAMapping.CONTENT_URI, projection, where, null, null);
           int  msgSource= -1;
           if (cursor != null) {
               while (cursor.moveToNext()) {
                 msgSource=cursor.getInt(0);
               }
               cursor.close();
           }
           return msgSource;

    }
	
    /**
     * This Method 
     * @param msgId
     * @return
     */
    public String[] getVMAUidAndMessageId() {
            String where = VMAMapping._LUID + "=" + mMsgId +" AND "+ VMAMapping._TYPE+"="+(mSms?1:2);
            String[] projection = new String[] { VMAMapping._UID , VMAMapping._MSGID ,VMAMapping._SOURCE };
            Cursor cursor = SqliteWrapper.query(mContext,VMAMapping.CONTENT_URI, projection, where, null, null);
            String[] msg= new String[3];
            if (cursor != null) {
                while (cursor.moveToNext()) {
                  msg[0]=cursor.getString(0);
                  msg[1]=cursor.getString(1);
                  msg[2]=cursor.getString(2);
                }
                cursor.close();
            }
            return msg;
    }
    
}
