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

package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.Browser;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.widget.AbsListView.RecyclerListener;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ContentType;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.data.Conversation.IncomingMessageData;
import com.verizon.mms.data.Conversation.OutgoingMessageData;
import com.verizon.mms.data.WorkingMessage;
import com.verizon.mms.helper.BitmapManager;
import com.verizon.mms.helper.BitmapManager.BitmapEntry;
import com.verizon.mms.helper.Cache;
import com.verizon.mms.helper.HrefManager;
import com.verizon.mms.helper.LinkDetail;
import com.verizon.mms.model.ImageModel;
import com.verizon.mms.model.MediaModel;
import com.verizon.mms.model.SlideModel;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.pdu.CharacterSets;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.transaction.MessagingNotification;
import com.verizon.mms.transaction.MessagingNotification.NotificationListener;
import com.verizon.mms.transaction.Transaction;
import com.verizon.mms.transaction.TransactionBundle;
import com.verizon.mms.transaction.TransactionService;
import com.verizon.mms.ui.ListDataWorker.ListDataJob;
import com.verizon.mms.ui.ListDataWorker.ListDataOwner;
import com.verizon.mms.ui.MessageItem.DeliveryStatus;
import com.verizon.mms.ui.MessageItem.GroupMode;
import com.verizon.mms.ui.MessageItem.OnLoadedListener;
import com.verizon.mms.ui.MessageListAdapter.MessageState.State;
import com.verizon.mms.ui.adapter.FastCursorAdapter;
import com.verizon.mms.ui.adapter.UrlAdapter;
import com.verizon.mms.util.BitmapManager.BitmapUser;
import com.verizon.mms.util.DownloadManager;
import com.verizon.mms.util.EmojiParser;
import com.verizon.mms.util.Prefs;
import com.verizon.mms.util.SizeCache;
import com.verizon.mms.util.SmileyParser;
import com.verizon.mms.util.Util;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.DatasetChangeListener;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;
import com.vzw.vma.common.message.MSAMessage;


/**
 * The back-end data adapter of a message list.
 * 
 * The mms messages is loaded in two ways based on the LOAD_TEXT_ON_UI flag
 * 
 * -) MMS is loaded in two stages
 *    1) We fetch the mms from db and load only the text part
 *    2) We fetch the attachments in the mms in background thread
 *    
 * -) Entire MMS is loaded in background keeping the UI thread jitter free
 * 
 * This adapter does not listen to the changes in the DataBase thru cursor instead it registers itself to 
 * ConversationDataObserver and refreshes the cursor
 */
public class MessageListAdapter extends FastCursorAdapter implements ListDataOwner, RecyclerListener, BitmapUser {
	private static final boolean LOAD_TEXT_ON_UI = false;
	
	protected LayoutInflater mInflater;
	private final MessageListView mListView;
	private ItemCache messageItemCache;
	private static ItemCache oldMessageItemCache;
	private static long cacheSize;
	private OnContentChangedListener mOnDataSetChangedListener;
	private Handler mMsgListItemHandler;
	private Pattern mHighlight;
	private Context mContext;
	private Conversation mConversation;
	private boolean mFullWidthLayout;
	private boolean groupThread;
	private GroupMode groupMode;
	private int lastPos = -1;
	private long lastIncomingAnimationId = -1;
	private int lastIncomingPos = -1;
	private int lastOutgoingPos = -1;
	private int prevOutgoingPos;
	private long lastOutGoingMessageTime = -1;
	private int firstOutgoingPos = -1;
	private boolean messageAdded;
	private static long lastThreadId;
	private boolean threadChanged = false; //flag used to recreate the caches if there was a change in the cursor
	private long mHighLightedMsgId = 0;
	private Cursor baseCursor;
	private Object cursorLock = new Object();
	private HashMap<Integer, ViewType> viewTypeByPos;
	private ListDataWorker loadWorker;
	private int numViews;
	private MessageState receivingMsg;
	private Set<String> receivedXids;
	private final SendingState smsSendingState = new SendingState(false);
	private final SendingState mmsSendingState = new SendingState(true);
	private final SendingState[] sendingStates = { smsSendingState, mmsSendingState };
	private final Map<Long, SendingMessage> sendingMsgs;
	private final Map<Long, SendingMessage> sentMmsMsgs;
	private volatile boolean haveSending;
	private boolean haveSent;
	private boolean sendingMessages;
    private ArrayList<Cursor> cursors;
	private Cursor deferredCursor;
	private int queueSize = MIN_QUEUE_SIZE;
	private static final int numViewTypes = ViewType.values().length;
	private static HashMap<Long, Status> deliveryStatus;
	private static int errors;
	private boolean mEnableUrlPreview; 
	private Resources res;
	private int messageTopBorder;
	private int messageBottomBorder;
	private int messageLeftRightBorder;
	private int sendingTopBorder;
	private int senderTopBorder;
	private int senderTopBorderWithText;
	private int minMessageWidth;
	private static int[] minMessageWidths = new int[2];
	private int maxMessageBorder;
	private int minImageMessageHeight;
	private int maxImageMessageHeight;
	private Rect imageDimensions;
	private int minImageBorder;
	private int previewTextWidth;
	private int minPreviewImage;
	private OnGlobalLayoutListener listLayoutListener;
	private Bitmap missingPicture;
	private Bitmap missingVideo;
	private Bitmap missingAudio;
	private ColorDrawable blackDrawable;
	private com.verizon.mms.util.BitmapManager bitmapMgr;
	private final HashSet<Cursor> observedCursors;
	private Map<Long, List<Uri>> lastMessagesReceived;
	private long lastReceivedMmsId;
	private long lastReceivedSmsId;
	private TextEntryStateProvider textEntryStateProvider;

	//customization of conversation related variables
	private boolean mFillBubble;
	private int mBackgroundColor;
	private int mTimeStampColor;
	private int mWhiteColor;
	private int mGreyHighlightColor;
	private int mErrorIconId;

	// layout ratios per the iPad visual design guide spec
	private static final float MIN_MESSAGE_WIDTH_RATIO = 295f / 614f;  // min message width
	private static final float IMAGE_WIDTH_HEIGHT_RATIO = 1.5f;        // ratio of min/max image height to width
	private static final float IMAGE_SCALE_LIMIT = 4.0f;               // max to scale up small images

    private static final boolean SHOW_PREVIEW_ERROR = false;

    private DatasetChangeListener contentChangeListener = null; 
    
	// for preview caching
    private static final int DETAIL_CACHE_SIZE = 10;
    private static final int BITMAP_CACHE_SIZE = 5;
    private Cache<String, LinkDetail> detailCache;
    private Cache<String, BitmapEntry> bitmapCache;

   private static final float OLD_CACHE_PERCENT = 0.2f;    // percent of cache to allocate for old cache

	private static final int[] leftBubbles = {
		R.drawable.chat_bubble2_left,
		R.drawable.chat_bubble3_left,
		R.drawable.chat_bubble4_left,
		R.drawable.chat_bubble5_left,
		R.drawable.chat_bubble6_left,
		R.drawable.chat_bubble7_left,
		R.drawable.chat_bubble8_left,
		R.drawable.chat_bubble9_left,
		R.drawable.chat_bubble10_left,
		R.drawable.chat_bubble11_left,
		R.drawable.chat_bubble12_left,
		R.drawable.chat_bubble13_left,
		R.drawable.chat_bubble14_left,
		R.drawable.chat_bubble15_left,
		R.drawable.chat_bubble16_left,
		R.drawable.chat_bubble17_left,
		R.drawable.chat_bubble18_left,
		R.drawable.chat_bubble19_left,
		R.drawable.chat_bubble20_left,
		R.drawable.chat_bubble21_left
	};


	public interface TextEntryStateProvider {
		public long getLastTextEntryTime();
	}


	private static class SendingState {
    	private boolean mms;
    	private long lastId;
    	private long lastDate;
    	private int numSending;

    	public SendingState(boolean mms) {
    		this.mms = mms;
    	}

    	public String toString() {
    		return "SendingState: mms = " + mms + ", lastId = " + lastId +
    			", lastDate = " + lastDate + ", numSending = " + numSending;
    	}
    }


	public static class MessageState {
		protected boolean mms;
		protected long id;
		protected State state;
		protected boolean sent;
		View view;

		public enum State {
			INITIAL,    // created
			IN_CURSOR,  // added to sending cursor, or initial state for receiving
			ANIMATING,  // in process of animatng
			FINISHED,   // done animating
			SENT        // known to be persisted
		}

		public MessageState(boolean mms, long id) {
			this.mms = mms;
			this.id = id;
			state = State.IN_CURSOR;
		}

		@Override
		public String toString() {
			return Integer.toHexString(hashCode()) + ": mms = " + mms + ", id = " + id + ", state = " + state;
		}
	}


	public static class SendingMessage extends MessageState {
		private long tempId;
		private String body;
		private String subject;
		private SlideshowModel model;
		private long date;
		private int error;

		/**
		 * Constructor for SMS messages.
		 */
		public SendingMessage(long id, String body) {
			super(false, id);
			this.body = body == null ? null : body.trim();
			init();
		}

		/**
		 * Constructor for MMS messages.
		 */
		public SendingMessage(long id, String subject, SlideshowModel model) {
			super(true, id);
			this.subject = subject == null ? null : subject.trim();
			this.model = model;
			init();
		}

		private void init() {
			tempId = id;
			state = State.INITIAL;
			date = System.currentTimeMillis();
		}

		public void setUri(Uri uri) {
			try {
				id = ContentUris.parseId(uri);
			}
			catch (Exception e) {
				if (Logger.IS_ERROR_ENABLED) {
					Logger.error(getClass(), "setId: bad uri " + uri, e);
				}
			}
		}

		public void setError(int error) {
			this.error = error;
		}

		/**
		 * Returns an appropriate cursor row with this message's data.
		 */
		private Object[] getRow() {
			final int len = PROJECTION.length;
			final Object[] vals = new Object[len];
			System.arraycopy(sendingRow, 0, vals, 0, len);
			vals[COLUMN_MSG_TYPE] = mms ? "mms" : "sms";
			vals[COLUMN_ID] = id;
			if (mms) {
				vals[COLUMN_MMS_DATE] = date / 1000;
				if (subject != null) {
					vals[COLUMN_MMS_SUBJECT] = subject;
				}
			}
			else {
				vals[COLUMN_SMS_DATE] = date;
				if (body != null) {
					vals[COLUMN_SMS_BODY] = body;
				}
			}
			return vals;
		}

		@Override
		public String toString() {
			return Integer.toHexString(hashCode()) + ": mms = " + mms + ", id = " + id + ", tempId = " + tempId +
				", state = " + state + ", error = " + error + ", sent = " + sent + ", model = " +  model +
				", body = " + (body == null ? "null" : body.substring(0, Math.min(body.length(), 80)));
		}
	}

    private static class Status {
		private DeliveryStatus status;
		private boolean failed;

		public Status(DeliveryStatus status, boolean failed) {
			this.status = status;
			this.failed = failed;
		}
	}


	private static class ItemCache extends SizeCache<Long, MessageItem> implements OnLoadedListener {
		private static final int INITIAL_CAPACITY = 40;
		private static final int ITEM_SIZE = 100;    // order-of-magnitude estimate of overhead for item objects
		private static final int LARGE_ITEM = 2000;  // limit between "small" and "large" items, e.g. text and media


		public ItemCache(String tag, long maxCacheSize) {
			super(tag, INITIAL_CAPACITY, maxCacheSize, null, LARGE_ITEM, ITEM_SIZE);
		}

		@Override
		public MessageItem put(Long key, MessageItem item) {
			// set the listener for unloaded items so that we can update its cached size when loaded
			if (!item.mLoaded || !item.mMediaLoaded) {
				item.setOnLoadedListener(this);
			}
			return super.put(key, item);
		}

		@Override
		public void onMessageLoaded(MessageItem item) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".onMessageLoaded: item = " + item);
			}
			synchronized (this) {
				updateSize();
				item.setOnLoadedListener(null);
			}
		}

		@Override
		protected void removeItem(MessageItem item, int size) {
			item.setOnLoadedListener(null);
			super.removeItem(item, size);
		}
	}

	// we buffer content change notifications to reduce thrashing, but need to be
	// responsive to status changes when too long a delay might be objectionable;
	// since we can't tell if delivery reports are pending for SMS messages we need
	// to have a shorter delay for them, and we ignore pending reports for visible
	// messages if they are older than MAX_CONTENT_CHANGE_MESSAGE_AGE
	//
	private static final long SMALL_CONTENT_CHANGE_DELAY = 500;
	private static final long DEFAULT_SMS_CONTENT_CHANGE_DELAY = 2000;
	private static final long DEFAULT_MMS_CONTENT_CHANGE_DELAY = 2000;
	private static final long MAX_CONTENT_CHANGE_MESSAGE_AGE = 10 * 60 * 1000;
	private static final long MAX_TEXT_ENTRY_DELAY = 15 * 1000;
	private static final long ANIM_BUBBLE_DURATION = 1000;
	private static final long ANIM_DONE_DELAY = 4000;
	private static final long CHANGE_CURSOR_DELAY = 300;

    private static final long TIMESTAMP_GAP_MAX = 300000;

	private static final float VIEW_QUEUE_FACTOR = 2.5f;  // size of queue relative to number of views
	private static final int MIN_VIEWS = 10;
	private static final int MIN_QUEUE_SIZE = (int)(MIN_VIEWS * VIEW_QUEUE_FACTOR);

	private static final int QUEUE_LOAD = 0;

	private static final int MSG_LOADED = 1;
	private static final int MSG_CHANGE_CURSOR = 2;
	private static final int MSG_BLANK_STATUS = 3;
	private static final int MSG_ANIM_DONE = 4;
	private static final int MSG_UNREGISTER = 5;

	private static final int MAX_ERROR_RETRIES = 3;

	static final int MSG_LIST_ERROR = 1;
	static final int MSG_LIST_MENU = 2;

	// template for the dummy cursor rows for in-process sending messages
	private static final Object[] sendingRow = {
		null,
		0L,
		null,
		"",
		0L,
		1,
		Sms.MESSAGE_TYPE_QUEUED,
		Sms.STATUS_NONE,
		0,
		0,
		"",
		CharacterSets.UTF_8,
		0L,
		1,
		PduHeaders.MESSAGE_TYPE_SEND_REQ,
		Mms.MESSAGE_BOX_OUTBOX,
		PduHeaders.VALUE_NO,
		PduHeaders.VALUE_NO,
		MmsSms.NO_ERROR,
		0,
		null,
		PduHeaders.RESPONSE_STATUS_OK,
		null
	};

	public static final String[] PROJECTION = new String[] {
		MmsSms.TYPE_DISCRIMINATOR_COLUMN,
		BaseColumns._ID,
		// For SMS
		Sms.ADDRESS,
		Sms.BODY,
		Sms.DATE,
		Sms.READ,
		Sms.TYPE,
		Sms.STATUS,
		Sms.LOCKED,
		Sms.ERROR_CODE,
		// For MMS
		Mms.SUBJECT,
		Mms.SUBJECT_CHARSET,
		Mms.DATE,
		Mms.READ,
		Mms.MESSAGE_TYPE,
		Mms.MESSAGE_BOX,
		Mms.DELIVERY_REPORT,
		Mms.READ_REPORT,
		PendingMessages.ERROR_TYPE,
		Mms.LOCKED,
		Mms.STATUS,
		Mms.RESPONSE_STATUS,
		Mms.TRANSACTION_ID
	};

	// The indexes of the default columns which must be consistent with above PROJECTIONs
	public static final int COLUMN_MSG_TYPE            = 0;
	public static final int COLUMN_ID                  = 1;
	public static final int COLUMN_SMS_ADDRESS         = 2;
	public static final int COLUMN_SMS_BODY            = 3;
	public static final int COLUMN_SMS_DATE            = 4;
	public static final int COLUMN_SMS_READ            = 5;
	public static final int COLUMN_SMS_TYPE            = 6;
	public static final int COLUMN_SMS_STATUS          = 7;
	public static final int COLUMN_SMS_LOCKED          = 8;
	public static final int COLUMN_SMS_ERROR_CODE      = 9;
	public static final int COLUMN_MMS_SUBJECT         = 10;
	public static final int COLUMN_MMS_SUBJECT_CHARSET = 11;
	public static final int COLUMN_MMS_DATE            = 12;
	public static final int COLUMN_MMS_READ            = 13;
	public static final int COLUMN_MMS_MESSAGE_TYPE    = 14;
	public static final int COLUMN_MMS_MESSAGE_BOX     = 15;
	public static final int COLUMN_MMS_DELIVERY_REPORT = 16;
	public static final int COLUMN_MMS_READ_REPORT     = 17;
	public static final int COLUMN_MMS_ERROR_TYPE      = 18;
	public static final int COLUMN_MMS_LOCKED          = 19;
	public static final int COLUMN_MMS_STATUS          = 20;
	public static final int COLUMN_MMS_RESPONSE_STATUS = 21;
	public static final int COLUMN_MMS_TRANSACTION_ID  = 22;


	// there are several different types of views depending on the message properties
	private enum ViewType {
		TEXT,      // text-only message
		MEDIA,     // MMS message with media attachments
		MMS,       // MMS message that isn't yet loaded
		DOWNLOAD;  // undownloaded MMS message (notification indicator)
	}


	abstract class ItemView {
		// set once per view in newView()
		protected ViewGroup content;
		private TextView timestamp;
		private TextView groupModeChange;
		protected View message;
		private View bubble;
		private View highlight;
		private TextView sender;
		private TextView status;
		private View error;
		// set in each bindView()
		private View view;
		protected MessageItem msgItem;
		protected MessageItem newItem;
		protected SendingMessage sendingMsg;
		protected boolean outgoing;
		private int curPos;

		private static final long BLANK_STATUS_LONG_DELAY = 2500;
		private static final long BLANK_STATUS_SHORT_DELAY = 1000;


		protected abstract int getContentLayout();


		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			// inflate the main message item view
			final LayoutInflater inflater = mInflater;
			final View view = inflater.inflate(R.layout.msg_item, parent, false);

			// add the specific view type's layout to the item's content
			final ViewGroup content = this.content = (ViewGroup)view.findViewById(R.id.content);
			inflater.inflate(getContentLayout(), content, true);
			content.setMinimumWidth(minMessageWidth);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(MessageListAdapter.this + ".newView: set min width of " + content + " to " + minMessageWidth);
			}

			// set up the data for bindView
			timestamp = (TextView)view.findViewById(R.id.timestamp);
			groupModeChange = (TextView)view.findViewById(R.id.groupModeChange);
			message = view.findViewById(R.id.message);
			message.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onItemClick(v);
				}
			});
			message.setOnLongClickListener(msgMenuListener);
			message.setTag(this);
			bubble = view.findViewById(R.id.bubble);
			highlight = view.findViewById(R.id.highlightBackground);
			sender = (TextView)view.findViewById(R.id.sender);
			status = (TextView)view.findViewById(R.id.status);
			error = view.findViewById(R.id.error);
			((ImageView)error).setImageResource(mErrorIconId);
			view.setTag(this);
			return view;
		}

		public OnLongClickListener msgMenuListener = new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				final Handler handler = mMsgListItemHandler;
				if (handler != null) {
					final Message msg = Message.obtain(handler, MSG_LIST_MENU, msgItem);
					handler.sendMessage(msg);
				}
				return true;
			}
		};

		protected void setContentLayout(DeliveryStatus stat) {
		}

		public void bindView(View view, Context context, Cursor cursor) {
			synchronized (cursorLock) {
				this.view = view;
				final int curPos = this.curPos = cursor.getPosition();
				final boolean sending = curPos > lastPos;

				// get any sent or sending message for this id and fetch the item
				final long key = getIdKey(cursor);
				sendingMsg = haveSending ? sendingMsgs.get(key) : null;
				if (haveSent) {
					final SendingMessage msg = sentMmsMsgs.get(key);
					if (msg != null) {
						sendingMsg = msg;
					}
				}
				final MessageItem msgItem = getMessage(cursor, key, sending);

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(MessageListAdapter.this + ".bindView: pos = " + curPos + ", itemView = " + this +
						", sendingMsg = " + sendingMsg + ", receivingMsg = " + receivingMsg + ", item = " + msgItem +
						", newItem = " + newItem);
				}

				if (msgItem != null) {
					// once an MMS message is loaded we can remove the cached sent message
					if (sendingMsg != null && !msgItem.mSms && msgItem.mLoaded) {
						sentMmsMsgs.remove(key);
						haveSent = sentMmsMsgs.size() != 0;
					}

					// hide messages that are being received until they're loaded and we can animate them
					final boolean receiving = receivingMsg != null && receivingMsg.id == msgItem.mMsgId;
					if (receiving && !msgItem.mLoaded) {
						hideMessage();
					}
					else {
						final Contact contact;
						final boolean outgoing;
						final boolean highlight;
						final String sender;
						final boolean animate;

						if (!sending) {
							// normal message
							contact = msgItem.mRawContact;
							outgoing = this.outgoing = contact == null;
							//highlight = msgItem.mMsgId == mHighLightedMsgId;
                            highlight = isHighLightRequired(msgItem);
							// show sender if this is an incoming group message
							sender = outgoing ? null : groupThread ? msgItem.mContact : null;
							animate = receiving;
						}
						else {
							// dummy item for in-process sending message
							contact = null;
							outgoing = this.outgoing = true;
							highlight = false;
							sender = null;
							animate = true;
						}

						setMessageLayout(outgoing, contact, highlight, animate, msgItem);
						final DeliveryStatus stat = setStatus(outgoing);
						setContentLayout(stat);
						setSender(sender);
						setTimestamp(cursor);
						setGroupModeChange(outgoing, cursor);
						
						if (highlight) {
							msgItem.mTextColor = -1;
							msgItem.mRecipColor = -1;
						}
					}
				}
				else {
					// invalid item in the cursor
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(MessageListAdapter.this + ".bindView: invalid item at pos " + curPos);
					}
					invalidCursor();
				}
			}
		}

		
        protected void hideMessage() {
			message.setVisibility(View.GONE);
			timestamp.setVisibility(View.GONE);
			groupModeChange.setVisibility(View.GONE);
		}


		protected MessageItem getMessage(Cursor cursor, long key, boolean sending) {
			MessageItem item = msgItem = getMessageItem(cursor, key, false);
			
			if (item != null && (!item.mLoaded || !item.mMediaLoaded) && !sending) {
				// load it in background
				newItem = item;
				loadWorker.request(QUEUE_LOAD, cursor.getPosition(), loadJob, this);

				// use stale item for now if one exists
				final MessageItem oldItem = getMessageItem(cursor, key, true);
				if (oldItem != null) {
					item = msgItem = oldItem;
				}
			}
			else {
				newItem = null;
			}

			return item;
		}

		private void setTimestamp(Cursor cursor) {
			// show the timestamp if this is the first message or it's > TIMESTAMP_GAP_MAX from the previous one
			final MessageItem msgItem = this.msgItem;
			boolean showTimestamp = false;
			long time = msgItem.mMsgTime;
			if (time == 0) {
				time = System.currentTimeMillis();
			}
			final int curPos = this.curPos;
			if (curPos == 0) {
				showTimestamp = true;
			}
			else {
				if (cursor.moveToPrevious()) {
					final long prevMsgTime;
					final String prevMsgType = cursor.getString(COLUMN_MSG_TYPE);
					if (prevMsgType.equals("mms")) {
						prevMsgTime = cursor.getLong(COLUMN_MMS_DATE) * 1000;
					}
					else {
						prevMsgTime = cursor.getLong(COLUMN_SMS_DATE);
					}

					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(MessageListAdapter.this + ".setTimestamp: pos = " + curPos + ", id = " + msgItem.mType + "/" +
							msgItem.mMsgId + ", time = " + new Date(msgItem.mMsgTime) + ", prev = " + new Date(prevMsgTime));
					}

					if (prevMsgTime != 0 && time > (prevMsgTime + TIMESTAMP_GAP_MAX)) {
						showTimestamp = true;
					}
				}
				cursor.moveToPosition(curPos);
			}

			final TextView ts = timestamp;
			if (showTimestamp) {
				String timeString = msgItem.getTimestamp();
				if (timeString == null || timeString.length() == 0) {
					timeString = MessageUtils.formatTimeStampString(time, true);
				}
				ts.setVisibility(View.VISIBLE);
				ts.setText(timeString);
				
				ts.setTextColor(mTimeStampColor);
			}
			else {
				ts.setVisibility(View.GONE);
			}
		}

		private void setGroupModeChange(boolean outgoing, Cursor cursor) {
			String msg = null;
			if (groupThread && outgoing && curPos <= lastOutgoingPos) {
				// show the group mode change indicator if the previous outgoing message was sent
				// with a different mode or this is the first outgoing message in the thread
				final GroupMode itemMode = msgItem.mGroupMode;
				if (itemMode != null) {
					final Context context = mContext;
					if (curPos == firstOutgoingPos) {
						msg = itemMode.getMessage(context, true);
					}
					else {
						//commented out to avoid query on UI thread
						/*final GroupMode prevMode = Conversation.getPreviousGroupMode(context, cursor, curPos);
						if (prevMode != null && prevMode != itemMode) {
							msg = itemMode.getMessage(context, false);
						}*/
					}
				}
			}
			if (msg != null) {
				groupModeChange.setText(msg);
				groupModeChange.setTextColor(mTimeStampColor);
				groupModeChange.setVisibility(View.VISIBLE);
			}
			else {
				groupModeChange.setVisibility(View.GONE);
			}
		}

		private void setMessageLayout(boolean outgoing, Contact contact, boolean isHighLight, boolean animate, MessageItem msgItem) {
			message.setVisibility(View.VISIBLE);

			// set bubble background
			Drawable background;
			int color;
			if (outgoing) {
				background = res.getDrawable(R.drawable.chat_bubble_adi_right_gradient);
				color = ConversationResHelper.getRightBubbleColor();
			}
			else {
				background = res.getDrawable(R.drawable.chat_bubble_adi_left_gradient);
				if (groupThread && mConversation != null) {
					// get the appropriate bubble resource for this recipient
					final int recipNum = mConversation.getRecipientNum(contact.getRecipientId());
					if (recipNum >= 0) {
						color = ConversationResHelper.getLeftBubbleColor(recipNum % leftBubbles.length);
					}
					else {
						color = Color.LTGRAY;
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(MessageListAdapter.this + ": no recipient for " + contact + ", conv = " + mConversation);
						}
					}
				}
				else {
					color = ConversationResHelper.getLeftBubbleColor(0);
				}
			}
            if (isHighLight) {
                color = ConversationResHelper.getMsgHightlightColor();
            }	
			background.setFilterBitmap(true);
			background.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
			
			bubble.setBackgroundDrawable(background);

			// if animation is desired and not already set for this view then start it
			final MessageState msg = sendingMsg != null ? sendingMsg : receivingMsg;

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(MessageListAdapter.this + ".setMessageLayout: pos " + curPos +
					": animate = " + animate + ", msg = " + msg);
			}

			if (animate && msg != null) {
				final State state = msg.state;
				if (state == State.IN_CURSOR) {
					final boolean hasAnimation = bubble.getAnimation() != null;
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(MessageListAdapter.this + ".setMessageLayout: pos " + curPos + ": hasAnimation = " + hasAnimation);
					}
					if (!hasAnimation) {
						msg.state = State.ANIMATING;
						msg.view = bubble;
						final float xpivot = outgoing ? 1 : 0;
						final ScaleAnimation anim = new ScaleAnimation(0, 1, 0, 1,
							Animation.RELATIVE_TO_SELF, xpivot, Animation.RELATIVE_TO_SELF, 1);
						anim.setDuration(ANIM_BUBBLE_DURATION);
						anim.setAnimationListener(animListener);
						bubble.startAnimation(anim);
					}
				}
				else if (state != State.ANIMATING) {
					bubble.clearAnimation();
					msg.view = null;
				}
			}

			// set highlight background as appropriate
			/*if (isHighLight) {
				//TODO: dont hardcode
				color = mGreyHighlightColor;
				if (outgoing) {
					highlight.setBackgroundResource(R.drawable.chat_bubble_gray_right);
				}
				else {
					highlight.setBackgroundResource(R.drawable.chat_bubble_gray_left);
				}
			} else*/ if (!mFillBubble && !isHighLight) {
				//TODO: dont hardcode
				color = mWhiteColor;
				if (outgoing) {
					highlight.setBackgroundResource(R.drawable.chat_bubble_white_right);
				}
				else {
					highlight.setBackgroundResource(R.drawable.chat_bubble_white_left);
				}
			} else {
				highlight.setBackgroundDrawable(null);
			}
			
			if (msgItem.mTextColor == -1) {
				msgItem.mTextColor = ConversationResHelper.getBubbleTextColor(color);
				msgItem.mRecipColor = ConversationResHelper.getTimeStampColor(color);
			}

			// set appropriate message alignment and padding
			final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)message.getLayoutParams();
			if (outgoing) {
				params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
				message.setPadding(maxMessageBorder, 0, 0, 0);
			}
			else {
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
				params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
				message.setPadding(0, 0, maxMessageBorder, 0);
			}
		}

		private AnimationListener animListener = new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				if (Logger.IS_DEBUG_ENABLED) {
					final MessageState msg = sendingMsg != null ? sendingMsg : receivingMsg;
					Logger.debug(MessageListAdapter.this + ".onAnimationStart: msg = " + msg);
				}
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				synchronized (cursorLock) {
					final MessageState msg = sendingMsg != null ? sendingMsg : receivingMsg;
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(MessageListAdapter.this + ".onAnimationEnd: msg = " + msg);
					}
					animation.setAnimationListener(null);
					animationDone(msg);
				}
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}
		};

		/**
		 * Set the error icon and delivery status fields.
		 * @return the message status or null if none available
		 */
		private DeliveryStatus setStatus(boolean outgoing) {
			DeliveryStatus stat = null;
			if (outgoing && msgItem.mMediaLoaded) {
				boolean failed = false;
				boolean save = false;
				if (curPos <= lastOutgoingPos) {
					// existing message: check if we have delivery status
					final MessageItem msgItem = this.msgItem;
					stat = msgItem.getDeliveryStatus(false);
					if (stat == null) {
						// check cache for previous status
						final long key = getIdKey(msgItem);
						final Status cached = deliveryStatus.get(key);
						if (cached != null) {
							stat = cached.status;
							failed = cached.failed;
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(MessageListAdapter.this + ".setStatus: got cached for " + key + ": stat = " + stat + ", failed = " + failed);
							}
						}
						else if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(MessageListAdapter.this + ".setStatus: messageAdded = " + messageAdded + ", cur / last = " + curPos + " / " + lastOutgoingPos);
						}
					}
	
					// check if this message has just been added
					if (messageAdded && curPos == lastOutgoingPos) {
						messageAdded = false;
						if (stat == null) {
							// assume its status is sending until we're updated
							stat = DeliveryStatus.SENDING;
							save = true;
						}
					}
				}
				else {
					// sending message
					stat = DeliveryStatus.SENDING;
				}

				// set status as appropriate
				if (stat != null) {
					setStatus(stat, failed, save);
				}
				else {
					status.setVisibility(View.GONE);
					error.setVisibility(View.GONE);
				}
			}
			else {
				// incoming message or status is not yet loaded
				status.setVisibility(View.GONE);
				error.setVisibility(View.GONE);
			}

			return stat;
		}


		/**
		 * Set the error icon and delivery status fields.  This assumes that stat is valid and
		 * that this is an outgoing message.
		 */
		private void setStatus(DeliveryStatus stat, boolean failed, boolean save) {
			final TextView status = this.status;
			final View error = this.error;
			final MessageItem msgItem = this.msgItem;
			final long key = getIdKey(msgItem);

			// get previous message status and update with current
			final Status prevCached = deliveryStatus.get(key);
			final DeliveryStatus prevStat = prevCached == null ? null : prevCached.status;

			// display status if we're the most recent outgoing message, status isn't terminal, or it just became terminal
			final boolean prevWasTerminal = prevStat == null || prevStat.isTerminal();
			final boolean isTerminal = stat.isTerminal();
			final boolean forceStatus = !sendingMessages && curPos == lastOutgoingPos;
			final boolean display = !isTerminal || !prevWasTerminal || forceStatus;

			if (display && stat != DeliveryStatus.NONE) {
				status.setText(stat.getStringId());
				status.setVisibility(View.VISIBLE);

				// blank status after a delay if not being forced and it just became terminal
				if (!prevWasTerminal && isTerminal && !forceStatus) {
					final long delay = stat == DeliveryStatus.DELIVERED ? BLANK_STATUS_SHORT_DELAY : BLANK_STATUS_LONG_DELAY;
					viewHandler.sendEmptyMessageDelayed(MSG_BLANK_STATUS, delay);
				}
			}
			else {
				status.setVisibility(View.GONE);
			}

			// set error indicator as appropriate
			if (!failed) {
				// check for updated status
				failed = msgItem.isFailedMessage(stat);
			}
			if (failed) {
				error.setVisibility(View.VISIBLE);
				setErrorClickListener(msgItem);
			}
			else {
				error.setVisibility(View.GONE);
			}

			if (save) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(MessageListAdapter.this + ".setStatus: caching for " + key + ": stat = " + stat + ", failed = " + failed);
				}
				deliveryStatus.put(key, new Status(stat, failed));
			}
		}

		private Handler viewHandler = new Handler() {
			public void handleMessage(Message msg) {
				final int what = msg.what;
				if (what == MSG_BLANK_STATUS) {
					// blank status field
					status.setVisibility(View.GONE);
				}
			}
		};

		protected void setStatusLayout(boolean below) {
			final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)status.getLayoutParams();
			if (below) {
				// put status below content
				params.addRule(RelativeLayout.BELOW, R.id.content);
				params.addRule(RelativeLayout.ALIGN_BOTTOM, 0);
			}
			else {
				// put status inside content
				params.addRule(RelativeLayout.BELOW, 0);
				params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.content);
			}
		}

		private void setErrorClickListener(final MessageItem msgItem) {
			error.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					final Handler handler = mMsgListItemHandler;
					if (handler != null) {
						final Message msg = Message.obtain(handler, MSG_LIST_ERROR, msgItem);
						handler.sendMessage(msg);
					}
				}
			});
		}

		private ListDataJob loadJob = new ListDataJob() {
			public Object run(int pos, Object obj) {
				ItemView itemView = (ItemView)obj;
				MessageItem msgItem = itemView.newItem;
				final MessageItem oldMessageItem = itemView.msgItem;
				if (msgItem == null) {
					msgItem = itemView.msgItem;
				}
				
				if (msgItem != null) {
					try {
						msgItem.loadMms(mContext);

						if (msgItem.mSlideshow != null) {
							//if the media in the old model is the same use the old media itself instead of loading a new one
							if (itemView.msgItem != null && itemView.msgItem != msgItem && SlideshowModel.isSame(itemView.msgItem.mSlideshow, msgItem.mSlideshow)) {
								msgItem.loadSlideshow(mContext, itemView.msgItem.mSlideshow);
							}
							msgItem.mSlideshow.loadFirstSlide(imageDimensions);
						}
					} catch (Exception e) {
						msgItem.mLoaded = false;
						msgItem.mStatusLoaded = false;
						msgItem.mMediaLoaded = false;
						//some exception occured, may be the message was deleted by the sync, mark loaded a false so that
						//we can retrieve it in next iteration or the cursor would be refreshed by then
						Logger.error("loadJob " + e);
					}
				}
				return obj;
			}
		};

		protected void setSender(String senderName) {
			final TextView sender = this.sender;
			if (senderName != null) {
				sender.setText(senderName);
				sender.setTextColor(msgItem.mRecipColor);
				sender.setVisibility(View.VISIBLE);
			}
			else {
				sender.setVisibility(View.GONE);
			}
		}

		protected void setSenderMargin(boolean hasText) {
			final MarginLayoutParams params = (MarginLayoutParams)sender.getLayoutParams();
			params.topMargin = hasText ? senderTopBorderWithText : senderTopBorder;
		}

		/**
		 * Called when the user clicks on the item view.  Default implementation does nothing.
		 */
		protected void onClick() {
		}
	}

	private static final int MSG_LINK_DETAIL = 1;
	private static final int MSG_IMAGE_DONE = 2;

	/**
	 * Handler for preview message.
	 */
	private Handler mPreviewHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_LINK_DETAIL: {
					LinkDetail detail = (LinkDetail)msg.obj;
					if (detail != null) {
						// add to local cache
						detailCache.putToCache(detail.getLink(), detail);
						try {
							mListView.setPosition();
							int size = mListView.getChildCount();
							
							for (int i = 0; i < size; i++) {
								View child = (View)mListView.getChildAt(i);
								View preview = child.findViewById(R.id.preview);
								
								if (preview != null) {
									TextItemView tiv = (TextItemView)preview.getTag();
									
									if (tiv != null) {
										if (!TextUtils.isEmpty(tiv.url) && tiv.url.equalsIgnoreCase(detail.getLink())) {
											tiv.setPreview(tiv.url, false);
										}
									}
								}
							}
						} catch (Exception e) {
							if (Logger.IS_ERROR_ENABLED) {
								Logger.error(MessageListAdapter.class, "handleMessage" + e);
							}
						}
						//commented out notifyDataSetChanged as it causes the list to scroll to bottom
						//notifyDataSetChanged(); // notify adaptor
					}
					break;
				}

				case MSG_IMAGE_DONE: {
                	int result = msg.arg1;
					BitmapEntry entry = (BitmapEntry)msg.obj;
					String url = null;
					
					// add to local cache
					if (entry != null) {			
                        url = entry.url;
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(this.getClass(), "Receive callback for " + url);
                        }
						if (entry != null && url != null) {
							if (result == BitmapManager.OK || result == BitmapManager.NOT_FOUND) {
								bitmapCache.putToCache(url, entry);
							}
						}
					}

					// use this must enable getting from database cache in getBitmap() function
					// also it seems like it might reset the view to the bottom of the list after relayout
//					notifyDataSetChanged(); // notify adaptor
					
                	// look for the View that has the request url
                	int size = mListView.getChildCount();
                	View textItem = null;
                	ImageView linkImage = null;

               		for (int i=0; i<size; i++) {
               			View child = (View)mListView.getChildAt(i);
               			ImageView image = (ImageView)child.findViewById(R.id.linkImage);
               			if (image != null) {
               				LinkTag tag = (LinkTag)image.getTag();
               				if (tag != null) {
               					if (tag.url.equals(url)) {
               						linkImage = image;
               						textItem = child;

               						if (linkImage != null) {
               							linkImage.setTag(null);

               							// same url
               							boolean set = false;
               							if (result == BitmapManager.OK) {
               								if (setImageBitmap(linkImage, entry.bitmap, minPreviewImage)) {
               									set = true;
               								}
               							}

               							// if there are no preview elements then remove it
               							if (!set) {
               								TextView linkTitle = (TextView)textItem.findViewById(R.id.linkTitle);
               								TextView linkDesc = (TextView)textItem.findViewById(R.id.linkDesc);
               								if (linkTitle != null && linkDesc!= null) {
               									if (linkTitle.getVisibility() == View.GONE &&
               											linkDesc.getVisibility() == View.GONE) {
               										// duplicate of TextItemView.removePreview()
               										removePreview(textItem);
               									}
               								}
               							}
               						}
               					}
               				}
               			}
               		}
					break;
				}
			}
		}		
	};
	
	private static void removePreview(View view) {
		View preview = view.findViewById(R.id.preview);
		preview.setVisibility(View.GONE);
		
		TextView text = (TextView)view.findViewById(R.id.text);
		final MarginLayoutParams params = (MarginLayoutParams)text.getLayoutParams();
		params.width = LayoutParams.WRAP_CONTENT;
		params.bottomMargin = 0;
		text.requestLayout();
		
	}

	private class TextItemView extends ItemView {
		private View layout;
		private TextView text;
		private View preview;
		private TextView linkTitle;
		private ImageView linkImage;
		private TextView linkDesc;
		private View loading;
		private View failed;
		private View refresh;
		protected boolean hasText;
		private String url;

		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = super.newView(context, cursor, parent);

			// set up the view type specific data for bindView
			layout = view.findViewById(R.id.textLayout);
			text = (TextView)view.findViewById(R.id.text);
			preview = view.findViewById(R.id.preview);
			linkTitle = (TextView)view.findViewById(R.id.linkTitle);
			linkImage = (ImageView)view.findViewById(R.id.linkImage);
			linkDesc = (TextView)view.findViewById(R.id.linkDesc);
			loading = view.findViewById(R.id.loading);
			failed = view.findViewById(R.id.failed);
			refresh = view.findViewById(R.id.refresh);

			return view;
		}

		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);

			if (msgItem != null) {
				final TextView text = this.text;
				int width = LayoutParams.WRAP_CONTENT;
				int bottom = 0;
				String previewUrl = null;
				final CharSequence body = formatMessage();
				if (hasText = body.length() > 0) {
					setTextHighLight(text);		
					text.setText(body);
					layout.setVisibility(View.VISIBLE);
	
					if (mEnableUrlPreview == true) {
						// check for urls in the body
						final URLSpan[] urls = text.getUrls();
						final int len = urls.length;
						if (len > 0) {
							// display a preview of the first http or https url
							for (int i = 0; i < len; i++) {
								final String url = urls[i].getURL();
								if (url != null && url.length() > 0 && (url.startsWith("http:") || url.startsWith("https:"))) {
									this.url = previewUrl = url;
									width = previewTextWidth;
									bottom = messageBottomBorder;
									break;
								}
							}
						}
					}
	
					// set text layout params
					final MarginLayoutParams params = (MarginLayoutParams)text.getLayoutParams();
					params.width = width;
					params.bottomMargin = bottom;
					layout.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
					setSenderMargin(true);
				}
				else {
					// no text or preview
					layout.setVisibility(View.GONE);
					setSenderMargin(false);  // presumably there is media etc.
				}
	
				setPreview(previewUrl, false);
			}
		}

        private void setTextHighLight(TextView text) {
            final MessageItem msg = msgItem;
            if (isHighLightRequired(msg)) {
                text.setTextColor(Color.WHITE);
            } else {
                text.setTextAppearance(mContext, R.style.TextAppearance_Conv_Body);
                text.setTextColor(msgItem.mTextColor);
            }
        }

        protected void setTextLayoutWidth(int width) {
			final MarginLayoutParams params = (MarginLayoutParams)layout.getLayoutParams();
			params.width = width - params.leftMargin - params.rightMargin;
			//Bug 2432
			if (text.getVisibility() == View.VISIBLE) {
                final MarginLayoutParams paramsText = (MarginLayoutParams) text.getLayoutParams();
                paramsText.width= LayoutParams.WRAP_CONTENT;
            }
		}

		protected CharSequence formatMessage() {
			final MessageItem msg = msgItem;
			return msg == null ? "" : formatMessage(msg.mBody, msg.mSubject, msg.mHighlight, msg.mTextContentType);
		}

		protected CharSequence formatMessage(String body, String subject, Pattern highlight, String contentType) {
			final SpannableStringBuilder buf = new SpannableStringBuilder();
			final SmileyParser parser = SmileyParser.getInstance();
           EmojiParser emojiParser = EmojiParser.getInstance();
			boolean hasSubject = false;
			if (subject != null) {
				final int len = subject.length();
				if (len != 0) {
					hasSubject = true;
					if (MmsConfig.enableEmojis) {
						buf.append(emojiParser.addEmojiSpans(subject, false));
					} else {
						buf.append(parser.addSmileySpans(subject, false));
					}
					buf.setSpan(new StyleSpan(Typeface.BOLD), 0, len, 0);
				}
			}

			if (body != null) {
				body = body.trim();
				if (body.length() != 0) {
					if (hasSubject) {
						buf.append("\n");
					}
					// convert from html if appropriate
					if (contentType != null && ContentType.TEXT_HTML.equals(contentType)) {
						if(MmsConfig.enableEmojis) {
							buf.append(emojiParser.addEmojiSpans(body, false));
						} 
						else{
							buf.append(parser.addSmileySpans(Html.fromHtml(body), false));
						}
					}
					else {
						if(MmsConfig.enableEmojis) {
							buf.append(emojiParser.addEmojiSpans(body, false));
						}
						else{
							buf.append(parser.addSmileySpans(body, false));
						}
					}
				}
			}

            if (mHighLightedMsgId != 0 && highlight != null) {
				final Matcher m = highlight.matcher(buf.toString());
				while (m.find()) {
					buf.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(), 0);
				}
			}

			return buf;
		}

		private void setPreview(String url, boolean force) {
			if (url != null) {
				final Context context = mContext;
				final HrefManager href = HrefManager.INSTANCE;
				LinkDetail linkDetail = null;
				
				if (!force) {
					// check local memory cache
					linkDetail = detailCache.getFromCache(url);
					if (linkDetail == null) {
						// check global cache
						linkDetail = href.getLink(context, url);
						if (linkDetail != null) {
							// add to local cache
							detailCache.putToCache(url, linkDetail);
						}
					}
				}
				
				if (force || linkDetail == null) {
					loading.setVisibility(View.VISIBLE);
					preview.setVisibility(View.GONE);
					preview.setTag(this);
					failed.setVisibility(View.GONE);
					// BZ#767 use force as parameter so it will force refresh
					// when user presses the refresh button
					href.loadLink(context, url, mPreviewHandler, MSG_LINK_DETAIL, force);
				}
				else {					
					preview.setTag(null);
					populatePreview(linkDetail);
				}
			}
			else {
				preview.setTag(null);
				loading.setVisibility(View.GONE);
				preview.setVisibility(View.GONE);
				failed.setVisibility(View.GONE);
			}
		}

		private void populatePreview(LinkDetail linkDetail) {
			loading.setVisibility(View.GONE);
			final String error = linkDetail.getError();
			if (error != null) {
				// 2012-08-12 disable error BZ#1276
				if (SHOW_PREVIEW_ERROR) {
					failed.setVisibility(View.VISIBLE);
					final String url = linkDetail.getLink();
					refresh.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							setPreview(url, true);
						}
					});
				}
				else {
					failed.setVisibility(View.GONE);
				}
				preview.setVisibility(View.GONE);
			}
			else {
				// successful fetch
				failed.setVisibility(View.GONE);

				final String title = linkDetail.getTitle();
				final String image = linkDetail.getLinkImage();
				final String desc = linkDetail.getDescription();

				if (title != null || desc != null || image != null) {
					preview.setVisibility(View.VISIBLE);

					// title
					final TextView linkTitle = this.linkTitle;
					if (title != null) {
						linkTitle.setText(Html.fromHtml(title));
						linkTitle.setVisibility(View.VISIBLE);
						// set bottom margin
						final MarginLayoutParams params = (MarginLayoutParams)linkTitle.getLayoutParams();
						params.bottomMargin = image == null && desc == null ? 0 : messageBottomBorder;
						//wrap the content :2874
						params.width = LayoutParams.WRAP_CONTENT;
					}
					else {
						linkTitle.setVisibility(View.GONE);
					}

					// image
					ImageView linkImage = this.linkImage;
					
		            LinkTag oldLinkTag = (LinkTag)linkImage.getTag();
	            	linkImage.setTag(null);

					if (image != null) {
						// try to get the bitmap from the cache
						final BitmapManager mgr = BitmapManager.INSTANCE;
						final Context context = mContext;
						boolean hasImage = false;
						
						linkImage.setImageDrawable(null);
						
						// check local cache
						BitmapEntry bitmapEntry = bitmapCache.getFromCache(image);						
						if (bitmapEntry != null) {
							// we found it in local cache
							if (setImageBitmap(linkImage, bitmapEntry.bitmap, minPreviewImage)) {
								hasImage = true;
							}
						}
						else {
							// check global cache
							// calculate the pixel size we want
							int pixel = BitmapManager.dipToPixel(mContext, R.dimen.linkPreviewImageSize);
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(this.getClass(), "Getting memory cache for " + image);
							}
							BitmapEntry result = mgr.getBitmap(context, image, pixel, pixel);
							
							if (result != null) {
								if (Logger.IS_DEBUG_ENABLED) {
									Logger.debug(this.getClass(), "Result found memory cache for " + image);
								}
								
								// found in cache
								if (result.bitmap != null) {
									// bitmap can be null, e.g. if result is 404 NOT FOUND
									if (setImageBitmap(linkImage, result.bitmap, minPreviewImage)) {
										hasImage = true;
									}
								}
								else {
		            				// result found but not bitmap, so previous download didn't get a bitmap
		            				// TODO check response code to reload or not
								}
							}
							else {
								if (Logger.IS_DEBUG_ENABLED) {
									Logger.debug(this.getClass(), "Result NOT found memory cache for " + image);
								}
								
								// if not in the cache then try to download it
		            			boolean download = true;            			
		            			if (oldLinkTag != null) {
		            				if (oldLinkTag.url.equals(image)) {
		            					// still is queue, no need to reschedule
		            					linkImage.setTag(oldLinkTag);
		            					download = false;
		            					oldLinkTag = null;
		            				}
		            			}
		            			
		            			if (download) {
		            				boolean success = mgr.loadBitmap(context, image,
		            						pixel, pixel, minPreviewImage,
		            						mPreviewHandler, MSG_IMAGE_DONE);
		            				if (success) {
		            					// create a new link tag
		            					LinkTag tag = new LinkTag();
		            					tag.url = image;
		            					linkImage.setTag(tag);
		            				}
		            				else {
		            					linkImage.setTag(null);
		            				}
		            			}
							}
						}

						if (hasImage == false) {
							linkImage.setVisibility(View.GONE);
						}

						// set bottom margin
						final MarginLayoutParams params = (MarginLayoutParams)linkImage.getLayoutParams();
						params.bottomMargin = desc == null ? 0 : messageBottomBorder;
					}
					else {
						linkImage.setVisibility(View.GONE);
					}

					// description
					final TextView linkDesc = this.linkDesc;
					if (desc != null) {
						linkDesc.setText(Html.fromHtml(desc));
						linkDesc.setVisibility(View.VISIBLE);
					}
					else {
						linkDesc.setVisibility(View.GONE);
					}

					if (oldLinkTag != null) {
						// cancel previous task and reset the tag
						BitmapManager.INSTANCE.cancelRequest(mPreviewHandler, oldLinkTag.url);
					}
				}
				else {
					// no preview to display
					removePreview();
				}
			}
		}

		/**
		 * Remove preview layout from the list item.
		 */
		private void removePreview() {
			preview.setVisibility(View.GONE);
			preview.setTag(null);
			final MarginLayoutParams params = (MarginLayoutParams)text.getLayoutParams();
			params.width = LayoutParams.WRAP_CONTENT;
			params.bottomMargin = 0;
		}
		
		protected void onClick() {
			final Context context = mContext;
			final URLSpan[] spans = text.getUrls();
			if (spans.length == 1) {
				// single url: display it
				showUrl(context, Uri.parse(spans[0].getURL()));
			}
			else if (spans.length != 0) {
				// multiple urls: have user choose one
				final java.util.ArrayList<String> urls = MessageUtils.extractUrisForMessageClick(spans);
				if(urls.size() == 0){
					return;
				}
				final UrlAdapter adapter = new UrlAdapter(context, urls);

				final DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
					public final void onClick(DialogInterface dialog, int which) {
						if (which >= 0) {
							showUrl(context, Uri.parse(urls.get(which)));
						}
					}
				};

				final AlertDialog.Builder b = new AlertDialog.Builder(context);
				b.setTitle(R.string.select_link_title);
				b.setCancelable(true);
				b.setAdapter(adapter, click);
				b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					public final void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				b.show();
			}
		}

		protected void showUrl(Context context, Uri uri) {
			if (uri.toString().startsWith(UrlAdapter.TEL_PREFIX)) {
				if(MmsConfig.isTabletDevice()){
					if(Logger.IS_DEBUG_ENABLED){
						Logger.debug("Not launching Phone App from Tablet for uri :"+ uri);
					}
					return;
				}
			}
			final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			context.startActivity(intent);
		}

		protected int getContentLayout() {
			return R.layout.msg_item_text;
		}

		public String getUrl() {
			return url;
		}
	}


	private class DownloadItemView extends TextItemView {
		private Button button;
		private TextView message;
		private TextView showVMA;
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = super.newView(context, cursor, parent);

			// set up the view type specific data for bindView
			button = (Button) view.findViewById(R.id.downloadButton);
			message = (TextView)view.findViewById(R.id.downloadMessage);
			showVMA = (TextView)view.findViewById(R.id.show_getStarted);
			showVMA.setVisibility(View.GONE);
			return view;
		}

		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);

			if (msgItem != null) {
				message.setSingleLine(true);
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(MessageListAdapter.this + ".bindView: xid = " + msgItem.mXid +
						", status = " + msgItem.mMmsStatus);
				}

				// don't display the superfluous notification ind if we have a recent receive conf for this xid
				if (receivedXids.contains(msgItem.mXid)) {
					hideMessage();
				}
				// otherwise set button and message state based on the message's download status
				else if (DownloadManager.getState(msgItem.mMmsStatus) == DownloadManager.STATE_DOWNLOADING) {
					message.setVisibility(View.VISIBLE);
					button.setVisibility(View.GONE);
				}
				else {
					message.setVisibility(View.GONE);
					button.setText(context.getString(R.string.download));
					button.setVisibility(View.VISIBLE);
					button.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							message.setVisibility(View.VISIBLE);
							button.setVisibility(View.GONE);
							final Context context = mContext;
							Intent intent = new Intent(context, TransactionService.class);
							// add handler for download error updates
							Messenger msg = new Messenger(downloadStatus);
							intent.putExtra(TransactionBundle.STATUS_HANDLER, msg);
							intent.putExtra(TransactionBundle.URI, msgItem.mMessageUri.toString());
							intent.putExtra(TransactionBundle.TRANSACTION_TYPE, Transaction.RETRIEVE_TRANSACTION);
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(MessageListAdapter.this + ".bindView: starting TransactionService to download MMS");
							}
							context.startService(intent);
						}
					});
					button.setLongClickable(true);
                    }
                }
				message.setTextColor(msgItem.mTextColor);
			}

		/*
		 * Called when message download fails: if fatal then so indicate, otherwise
		 * restore download button.
		 */
		Handler downloadStatus = new Handler() {
			public void handleMessage(Message msg) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(MessageListAdapter.this + ".downloadStatus: msg = " + msg);
				}
				if (msg.arg1 == Transaction.RETRIEVE_TRANSACTION) {
					// if it's a fatal error then so indicate, otherwise restore button
					final boolean fatal = msg.arg2 != 0;
					if (fatal) {
						message.setVisibility(View.VISIBLE);
						message.setText(R.string.download_error);
						button.setVisibility(View.GONE);
					}
					else {
						message.setVisibility(View.GONE);
						button.setVisibility(View.VISIBLE);
					}
				}
			}
		};

		@Override
		protected CharSequence formatMessage() {
			final MessageItem msg = msgItem;
			return formatMessage(null, msg.mSubject, msg.mHighlight, msg.mTextContentType);
		}

		protected int getContentLayout() {
			return R.layout.msg_item_download;
		}
	}


	private class MediaItemView extends TextItemView implements SlideViewInterface {
		private ImageView image;
		private View imageLayout;
		protected View progress;
		private View play;
		private View vcard;
		private ImageView vcardImage;
		private TextView vcardText;
		private View location;
		private ImageView locationImage;
		private TextView locationText;
		private TextView thumbnailStatus;


		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final View view = super.newView(context, cursor, parent);

			// set up the view type specific data for bindView
			image = (ImageView)view.findViewById(R.id.mediaImage);
			image.setOnClickListener(playListener);
			image.setOnLongClickListener(msgMenuListener);
			imageLayout = view.findViewById(R.id.mediaImageLayout);
			progress = view.findViewById(R.id.progress);
			play = view.findViewById(R.id.playButton);
			play.setOnClickListener(playListener);
			play.setOnLongClickListener(msgMenuListener);
			vcard = view.findViewById(R.id.vcard);
			vcardText = (TextView)view.findViewById(R.id.vcardText);
			vcardImage = (ImageView)view.findViewById(R.id.vcardImage);
			location = view.findViewById(R.id.location);
			locationText = (TextView)view.findViewById(R.id.locationText);
			locationImage = (ImageView)view.findViewById(R.id.locationImage);
			thumbnailStatus = (TextView) view.findViewById(R.id.thubmailStatus);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);
			thumbnailStatus.setVisibility(View.GONE);
			if (msgItem != null) {
				// if a media attachment is present then show it
				final SlideshowModel model = msgItem.mSlideshow;
				boolean loadtext = true;
				if (model != null && MessageUtils.getAttachmentType(model) != WorkingMessage.TEXT) {
					if (this.msgItem.mMediaLoaded) {
						MmsThumbnailPresenter presenter = new MmsThumbnailPresenter(mContext, this, model);
						presenter.present();
						loadtext = false;
						if (progress != null) {
							progress.setVisibility(View.GONE);
						}
					} else {
						//loadWorker.request(QUEUE_LOAD, cursor.getPosition(), loadJob, this);
						if (progress != null) {
							progress.setVisibility(View.VISIBLE);
						}
					}
				}
				
				if (loadtext) {
					// text-only or loading message
					setImageBitmap(null, false);
					play.setVisibility(View.GONE);
					vcard.setVisibility(View.GONE);
					location.setVisibility(View.GONE);
					setStatusLayout(hasText || !msgItem.mLoaded);
				}
			}
		}

		public void setAudio(Uri audio, String name, Map<String, ?> extras) {
			if (missingAudio == null) {
				try {
					missingAudio = bitmapMgr.decodeResource(res,
						R.drawable.ic_missing_thumbnail_audio);
				}
				catch (Throwable t) {
					Logger.error(MessageListAdapter.this, t);
				}
			}
			
			image.setOnClickListener(playListener);
			setImageBitmap(missingAudio, false);
			play.setVisibility(View.VISIBLE);
			vcard.setVisibility(View.GONE);
			location.setVisibility(View.GONE);
			setStatusLayout(true);
		}

		public void setImage(String name, Bitmap bitmap) {
			boolean mediaImage = true;
			if (bitmap == null) {
				if (missingPicture == null) {
					try {
						missingPicture = bitmapMgr.decodeResource(res,
							R.drawable.ic_missing_thumbnail_picture);
					}
					catch (Throwable t) {
						Logger.error(MessageListAdapter.this, t);
					}
				}
				bitmap = missingPicture;
				mediaImage = false;
			}
			setImageBitmap(bitmap, mediaImage);
			
			if (msgItem.mAttachmentType == WorkingMessage.SLIDESHOW) {
			    play.setVisibility(View.VISIBLE);
			} else {
			    play.setVisibility(View.GONE);
			}
            
            String contentDisposoition = msgItem.mContentDisposoition;
           
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("setImage msgItem.mContentDisposoition:" + contentDisposoition);
            }
            if (!TextUtils.isEmpty(contentDisposoition) && MSAMessage.THUMBNAIL_SECTION.equalsIgnoreCase(contentDisposoition)) {
                thumbnailStatus.setText("Thumbnail");
                thumbnailStatus.setVisibility(View.VISIBLE);
            }
            else
            {
                thumbnailStatus.setVisibility(View.GONE);
            }
			image.setOnClickListener(playListener);
			vcard.setVisibility(View.GONE);
			location.setVisibility(View.GONE);
			setStatusLayout(hasText);
		}

		public void setVideo(String name, Uri video) {
			setVideo(name, VideoAttachmentView.createVideoThumbnail(mContext, video));
		}

		public void setVideo(String name, Bitmap bitmap) {
			boolean mediaImage = true;
            if (bitmap == null) {
                if (missingVideo == null) {
                    try {
                        missingVideo = bitmapMgr.decodeResource(res,
                            R.drawable.ic_missing_thumbnail_video);
                    }
                    catch (Throwable t) {
                        Logger.error(MessageListAdapter.this, t);
                    }
                }
                bitmap = missingVideo;
                mediaImage = false;
            }
            image.setOnClickListener(playListener);
            setImageBitmap(bitmap, mediaImage);
            play.setVisibility(View.VISIBLE);
            vcard.setVisibility(View.GONE);
            location.setVisibility(View.GONE);
            setStatusLayout(hasText);
        }

		public void setLocation(Bitmap bitmap, String loc) {
			final View clickView;
			if (bitmap == null) {
				// use default image if no location image is available
				locationImage.setImageResource(R.drawable.attach_location);
				setImageBitmap(null, false);
				clickView = locationImage;
				final ViewGroup.LayoutParams params = locationText.getLayoutParams();
				params.height = params.width = LinearLayout.LayoutParams.WRAP_CONTENT;
				locationImage.setVisibility(View.VISIBLE);
			}
			else {
				locationImage.setVisibility(View.GONE);
				setImageBitmap(bitmap, true);
				clickView = image;
				final MarginLayoutParams params = (MarginLayoutParams)locationText.getLayoutParams();
				params.width = minMessageWidth - params.leftMargin - params.rightMargin;
			}

			clickView.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {

                    if (DeviceConfig.OEM.isNbiLocationDisabled) {
                        final Context context = mContext;
                        final MessageItem item = msgItem;
                        Intent goToGallery = new Intent(context, GalleryActivity.class);
                        goToGallery.putExtra("itemtogo", "content://" + VZUris.getMmsUri().getAuthority()
                                + "/" + item.mMsgId);
                        goToGallery.putExtra("threadid", mConversation.getThreadId());
                        goToGallery.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(goToGallery);
                        return;
                    }
                    MessageUtils.viewMmsMessageAttachment(mContext, null, msgItem.mSlideshow);
                }
			});
			clickView.setOnLongClickListener(msgMenuListener);
			if (msgItem.mBody.contains(loc)) {
				locationText.setVisibility(View.GONE);
			} else {
				locationText.setText(loc);
				locationText.setTextColor(msgItem.mTextColor);
				locationText.setVisibility(View.VISIBLE);
			}
			play.setVisibility(View.GONE);
			vcard.setVisibility(View.GONE);
			location.setVisibility(View.VISIBLE);
			setStatusLayout(true);
		}

		public void setVCard(Uri uri, String name, Bitmap bitmap) {
			if (bitmap == null) {
				vcardImage.setImageResource(R.drawable.list_namecard);
			}
			else {
				vcardImage.setImageBitmap(bitmap);
			}
			vcard.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					MessageUtils.viewMmsMessageAttachment(mContext, null, msgItem.mSlideshow);
				}
			});
			vcard.setOnLongClickListener(msgMenuListener);
			SpannableString msg = new SpannableString(name);
			if(MmsConfig.isTabletDevice()){
				Linkify.addLinks(msg, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS | Linkify.MAP_ADDRESSES);
			    
			}
			else
			{
				Linkify.addLinks(msg, Linkify.ALL);
			}
			vcardText.setText(msg);
			vcardText.setLinksClickable(true);
			vcardText.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					URLSpan[] spans = vcardText.getUrls();
					if (spans.length == 1) {
						showUrl(mContext, Uri.parse(spans[0].getURL()));
					}
				}
			});
			vcardText.setMovementMethod(LinkMovementMethod.getInstance());
			setImageBitmap(null, false);
			play.setVisibility(View.GONE);
			vcard.setVisibility(View.VISIBLE);
			location.setVisibility(View.GONE);
			setStatusLayout(true);
		}

		@Override
		public void setSlideShow() {
		    if (missingVideo == null) {
		        try {
		            missingVideo = bitmapMgr.decodeResource(res,
		                    R.drawable.ic_missing_thumbnail_video);
		        }
		        catch (Throwable t) {
		            Logger.error(MessageListAdapter.this, t);
		        }
		    }

		    setImageBitmap(missingVideo, false);
		    play.setVisibility(View.VISIBLE);
		    vcard.setVisibility(View.GONE);
		    location.setVisibility(View.GONE);
		    setStatusLayout(hasText);
		}

		private void setImageBitmap(Bitmap bitmap, boolean mediaImage) {
			final ImageView image = this.image;

			// check if the bitmap is being changed
			final Drawable d = image.getDrawable();
			if (d instanceof BitmapDrawable) {
				final Bitmap old = ((BitmapDrawable)d).getBitmap();
				if (old != bitmap) {
					// size of item is changing, need to update the cache
					messageItemCache.updateSize();
				}
			}
			image.setImageBitmap(bitmap);
            
			if (bitmap != null) {
				final boolean adjustViewBounds;
				final ScaleType scaleType;
				int width;
				int height;
				final int minHeight;
				final int maxWidth;
				final Drawable background;

				if (mediaImage) {
					adjustViewBounds = true;
					scaleType = ScaleType.CENTER_CROP;
					maxWidth = Integer.MAX_VALUE;
					minHeight = minImageMessageHeight;

					// if the image is smaller than the message then scale it up and center it on a black background
					height = bitmap.getHeight();
					width = bitmap.getWidth();
					if (width < minMessageWidth && height < minImageMessageHeight) {
						final float aspect = (float)width / height;
						width *= IMAGE_SCALE_LIMIT;
						if (width > minMessageWidth - minImageBorder) {
							width = minMessageWidth;
						}
						height = Math.round(width / aspect);
						background = blackDrawable;
					}
					else {
						// center image and crop to the view
						width = minMessageWidth;
						height = LayoutParams.WRAP_CONTENT;
						background = null;
					}
				}
				else {
					// icons/placeholder images are centered on a transparent background
					adjustViewBounds = false;
					scaleType = ScaleType.CENTER;
					maxWidth = minMessageWidth;
					width = minMessageWidth;
					height = LayoutParams.WRAP_CONTENT;
					background = null;
					minHeight = 0;
				}

				// set text and image layout widths equal
				setTextLayoutWidth(minMessageWidth);
				image.setAdjustViewBounds(adjustViewBounds);
				image.setScaleType(scaleType);
				final ViewGroup.LayoutParams params = image.getLayoutParams();
				params.width = width;
				params.height = height;
				image.setMinimumHeight(minHeight);
				image.setMaxWidth(maxWidth);
				image.setMaxHeight(maxImageMessageHeight);
				image.setVisibility(View.VISIBLE);
				imageLayout.setMinimumHeight(minHeight);
				imageLayout.setBackgroundDrawable(background);
			}
			else {
				image.setVisibility(View.GONE);
				imageLayout.setBackgroundDrawable(null);
				imageLayout.setMinimumHeight(0);
			}

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + "setImageBitmap: set " + image + " with " + bitmap);
			}
		}

		public Rect getImageDimensions() {
			return imageDimensions;
		}

		private OnClickListener playListener = new OnClickListener() {
			public void onClick(View v) {
				final Context context = mContext;
				final MessageItem item = msgItem;

				switch (item.mAttachmentType) {
				    case WorkingMessage.SLIDESHOW:
					case WorkingMessage.AUDIO:
						MessageUtils.viewMmsMessageAttachment(context, item.mMessageUri, item.mSlideshow);
						break;

					case WorkingMessage.IMAGE:
					case WorkingMessage.VIDEO:
						final Intent i = new Intent(context, MediaShoeboxInterceptorActivity.class);
						i.putExtra("threadid", mConversation.getThreadId());
						i.putExtra("itemtogo", item.mMessageUri.toString());
						i.putExtra("id", item.mMsgId);
						if (item.mBody != null) {
						    i.putExtra("text", item.mBody);
						}
						String subjectText = item.isFailedMessage(true) ? "" : mContext.getString(R.string.forward_prefix);
			            if (item.mSubject != null) {
			                subjectText += item.mSubject;
			            }
			            i.putExtra("subjectText", subjectText);
						context.startActivity(i);
						break;
				}
			}
		};

		@Override
		protected void setContentLayout(DeliveryStatus stat) {
			super.setContentLayout(stat);
			if ((msgItem.mLoaded && msgItem.mMediaLoaded) || sendingMsg != null) {
				content.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
				content.setPadding(0, 0, 0, 0);
			}
			else {
				// loading spinner
				content.getLayoutParams().width = minMessageWidth;
				final int bottom = stat == null ? messageBottomBorder + sendingTopBorder : messageBottomBorder;
				content.setPadding(messageLeftRightBorder, messageTopBorder, messageLeftRightBorder, bottom);
			}
		}

		protected int getContentLayout() {
			return R.layout.msg_item_media;
		}

		public void startAudio() {
		}

		public void startVideo() {
		}

		public void setImageRegionFit(String fit) {
		}

		public void setImageVisibility(boolean visible) {
		}

		public void setText(String name, String text) {
		}

		public void setTextVisibility(boolean visible) {
		}

		public void setVideoVisibility(boolean visible) {
		}

		public void stopAudio() {
		}

		public void stopVideo() {
		}

		public void reset() {
		}

		public void pauseAudio() {
		}

		public void pauseVideo() {
		}

		public void seekAudio(int seekTo) {
		}

		public void seekVideo(int seekTo) {
		}

		public void showVCard(ContactStruct contact) {
		}

		public int getHeight() {
			return 0;
		}
	}


	private class MmsItemView extends MediaItemView {
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			super.bindView(view, context, cursor);

			if (msgItem != null) {
				if (msgItem.mLoaded && msgItem.mMediaLoaded) {
					// normal MMS message
					progress.setVisibility(View.GONE);
					message.setLongClickable(true);
				}
				else {
					// sending or still loading: if not sending then show the progress spinner
					progress.setVisibility(sendingMsg == null ? View.VISIBLE : View.GONE);
					message.setLongClickable(false);
					setSender(null);
				}
			} else {
				progress.setVisibility(View.VISIBLE);
				message.setLongClickable(false);
				setSender(null);
			}
		}

		@Override
		protected MessageItem getMessage(Cursor cursor, long key, boolean sending) {
			// if the item isn't loaded and we have a SendingMessage then mock up the item content from it
			final MessageItem item = super.getMessage(cursor, key, sending);
			if (item != null && (!item.mLoaded || !item.mMediaLoaded)) {
				final SendingMessage msg = sendingMsg;
				if (msg != null) {
					item.mSubject = msg.subject;
					item.loadSlideshow(mContext, msg.model);
					item.mMediaLoaded = true;
				}
			}
			return item;
		}
	}


	public MessageListAdapter(Context context, Cursor c, MessageListView listView, Pattern highlight,
			Conversation conv, long highLightedMsgid, boolean fullWidthLayout, TextEntryStateProvider textEntryStateProvider) {
		super(context, c, false);
		setChangeListener(changeListener);

		mContext = context;
		mHighlight = highlight;
		mHighLightedMsgId = highLightedMsgid;
		mFullWidthLayout = fullWidthLayout;
		//this.textEntryStateProvider = textEntryStateProvider;

		mConversation = conv;
		groupThread = conv.getRecipients().size() > 1;

		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mListView = listView;
		listView.setRecyclerListener(this);
		bitmapMgr = com.verizon.mms.util.BitmapManager.INSTANCE;
		bitmapMgr.addBitmapUser(this);
		viewTypeByPos = new HashMap<Integer, ViewType>();
		sendingMsgs = Collections.synchronizedMap(new HashMap<Long, SendingMessage>());
		sentMmsMsgs = Collections.synchronizedMap(new HashMap<Long, SendingMessage>());
		observedCursors = new HashSet<Cursor>(2);
		createCaches(c);

		// preview cache
        detailCache = new Cache<String, LinkDetail>(DETAIL_CACHE_SIZE);
        bitmapCache = new Cache<String, BitmapEntry>(BITMAP_CACHE_SIZE);

		loadWorker = new ListDataWorker();
		loadWorker.addQueue(handler, QUEUE_LOAD, MSG_LOADED, MIN_QUEUE_SIZE, this);
		loadWorker.start();

		// allocate or clear delivery status cache
		updateThreadId(mConversation.getThreadId());

		// get the url preview preference and register the listener
        Prefs.registerPrefsListener(mPreferencesChangeListener);
		mEnableUrlPreview = Prefs.getBoolean(AdvancePreferenceActivity.WEBLINK_PREVIEW,
			AdvancePreferenceActivity.WEBLINK_PREVIEW_DEFAULT);

		res = mContext.getResources();
		onConfigChanged(res.getConfiguration());

		blackDrawable = new ColorDrawable(Color.BLACK);

		mWhiteColor = res.getColor(R.color.white);
		mBackgroundColor = ConversationResHelper.getBGColor();
		mTimeStampColor = ConversationResHelper.getTimeStampColor(mBackgroundColor);
		mGreyHighlightColor = res.getColor(R.color.highlightgrey);
		mFillBubble = ConversationResHelper.fillBubble();
		
		if (ConversationResHelper.isBrightColor(ConversationResHelper.getBGColor())) {
			mErrorIconId = R.drawable.ico_error_darkgray;
		} else {
			mErrorIconId = R.drawable.ico_error_white;
		}

		MessagingNotification.addListener(notificationListener);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ": created");
		}
	}

	public void onConfigChanged(Configuration config) {
		// (re)init config-specific data
		messageTopBorder = res.getDimensionPixelSize(R.dimen.messageTopBorder);
		messageBottomBorder = res.getDimensionPixelSize(R.dimen.messageBottomBorder);
		messageLeftRightBorder = res.getDimensionPixelSize(R.dimen.messageLeftRightBorder);
		sendingTopBorder = res.getDimensionPixelSize(R.dimen.sendingTopBorder);
		senderTopBorder = res.getDimensionPixelSize(R.dimen.senderTopBorder);
		senderTopBorderWithText = res.getDimensionPixelSize(R.dimen.senderTopBorderWithText);
		previewTextWidth = res.getDimensionPixelSize(R.dimen.previewTextWidth);
		minPreviewImage = (int)res.getDimension(R.dimen.linkPreviewMinImageSizeConversation);
		minImageBorder = (int)res.getDimension(R.dimen.minImageBorder);
		maxMessageBorder =
			res.getDimensionPixelSize(R.dimen.maxMessageBorder) +
			res.getDimensionPixelSize(R.dimen.messageBubbleBorder);

		// if this is a full-width layout then we can base the min message width on the display width,
		// otherwise we need to wait until after the first layout is complete after a config change,
		// since we don't know the width of the list before its layout is done
		//
		if (mFullWidthLayout) {
			final int dispWidth = res.getDisplayMetrics().widthPixels;
			setMessageSize(Math.round(dispWidth * MIN_MESSAGE_WIDTH_RATIO));

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".onGlobalLayout: disp width = " + dispWidth +
					", min width = " + minMessageWidth);
			}
		}
		else {
			// if the width for this orientation has been set then use it, otherwise set the
			// view listener and let it set the width when the list layout is done
			//
			// NB we assume the list width for each orientation won't change while the app is running;
			// if this isn't true (e.g. if the conv list can be closed in the tablet layout) then we
			// need to check for that
			//
			final int width = minMessageWidths[config.orientation == Configuration.ORIENTATION_PORTRAIT ? 0 : 1];
			if (width != 0) {
				setMessageSize(width);
			}
			else if (listLayoutListener == null) {
				listLayoutListener = new OnGlobalLayoutListener() {
					public void onGlobalLayout() {
						// set the width for this orientation
						final ListView list = mListView;
						final int listWidth = list.getMeasuredWidth();
						final int width = Math.round(listWidth * MIN_MESSAGE_WIDTH_RATIO);
						setMessageSize(width);

						final boolean port = res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
						minMessageWidths[port ? 0 : 1] = width;

						// set the min widths of existing views and re-layout
						final int count = list.getChildCount();
						for (int i = 0; i < count; ++i) {
							final View view = list.getChildAt(i);
							((ItemView)view.getTag()).content.setMinimumWidth(width);
						}
						list.invalidateViews();

						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(MessageListAdapter.this + ".onGlobalLayout: list width = " + listWidth +
								", portrait = " + port + ", min width = " + minMessageWidth);
						}

						// remove ourselves
						try {
							mListView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
						}
						catch (Exception e) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.error(e);
							}
						}
						listLayoutListener = null;
					}
				};

				mListView.getViewTreeObserver().addOnGlobalLayoutListener(listLayoutListener);
			}
		}
	}

	private void setMessageSize(int width) {
		minMessageWidth = width;
		minImageMessageHeight = Math.round(width / IMAGE_WIDTH_HEIGHT_RATIO);
		maxImageMessageHeight = Math.round(width * IMAGE_WIDTH_HEIGHT_RATIO);
		imageDimensions = new Rect(0, 0, minMessageWidth, minImageMessageHeight);
	}

	private void createCaches(Cursor cursor) {
		if (cursor != null/* && threadChanged || lastThreadId == 0 || messageItemCache == null*/) {
			if (cacheSize == 0) {
				cacheSize = bitmapMgr.getUserCacheSize("MessageItem");
			}
			if (oldMessageItemCache == null) {
				oldMessageItemCache = new ItemCache("MessageItem-old", (long)(cacheSize * OLD_CACHE_PERCENT));
			}
			// free old cache items if any
			if (messageItemCache != null) {
				messageItemCache.shutdown();
			}
			messageItemCache = new ItemCache("MessageItem-new", (long)(cacheSize * (1f - OLD_CACHE_PERCENT)));
			
			if (threadChanged) {
				if (contentChangeListener != null) {
					ConversationDataObserver.removeConvDataListener(contentChangeListener);
				}

				contentChangeListener = new DatasetChangeListener(lastThreadId) {
					@Override
					public void onMesssageStatusChanged(long threadId, long msgId, int msgType,
							long msgSource) {
						synchronized (messageItemCache) {
							long id = getIdKey(msgId, msgType == ConversationDataObserver.MSG_TYPE_MMS);

							Logger.debug("removind key " + id);

							deliveryStatus.remove(id);

							/*if (msgType == ConversationDataObserver.MSG_TYPE_SMS)*/ {
								messageItemCache.remove(id);
							} /*else {
							MessageItem msgItem = messageItemCache.get(id);
							if (msgItem != null) {
								msgItem.resetDeliveryStatus();
							}
						}*/
						}
						if (getCursor() != null) {
							onConversationContentChanged();
						}
					}

					@Override
					public void onMesssageDeleted(long threadId, long msgId, int msgType, long msgSource) {
						synchronized (messageItemCache) {
							long key = getIdKey(msgId, msgType == ConversationDataObserver.MSG_TYPE_MMS);
							deliveryStatus.remove(key);
							messageItemCache.remove(key);
							oldMessageItemCache.remove(key);
						}
						if (getCursor() != null) {
							onConversationContentChanged();
						}
					}

					@Override
					public void onMessageAdded(long threadId, long msgId, int msgType,
							long msgSource) {
						if (getCursor() != null) {
							onConversationContentChanged();
						}
					}
				};

				ConversationDataObserver.addConvDataListener(contentChangeListener);
			}
			threadChanged = false;
		}
	}

	@Override
	public void shutdown() {
		if (contentChangeListener != null) {
			ConversationDataObserver.removeConvDataListener(contentChangeListener);
			contentChangeListener = null;
		}
		
		synchronized (cursorLock) {
			super.shutdown();
			handler.removeCallbacksAndMessages(null);

			if (loadWorker != null) {
				loadWorker.exit();
				loadWorker = null;
			}

			if (deferredCursor != null) {
				try {
					deferredCursor.close();
					deferredCursor = null;
				}
				catch (Exception e) {
					Logger.error(e);
				}
			}
			
			if (messageItemCache != null) {
				messageItemCache.shutdown();
			}

			bitmapCache.clear();
		}

		bitmapMgr.removeBitmapUser(this);
		
		// remove preferences listener
		Prefs.unregisterPrefsListener(mPreferencesChangeListener);

		MessagingNotification.removeListener(notificationListener);
	}

	private final OnSharedPreferenceChangeListener mPreferencesChangeListener =
			new OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			// check if weblink preview preference is changed
			if (AdvancePreferenceActivity.WEBLINK_PREVIEW.equals(key)) {
				// get the new setting
				mEnableUrlPreview = prefs.getBoolean(AdvancePreferenceActivity.WEBLINK_PREVIEW,
						AdvancePreferenceActivity.WEBLINK_PREVIEW_DEFAULT);
			}
		}
	};	

	@Override
	public int getItemViewType(int position) {
		return getViewType(position).ordinal();
	}

	@Override
	public int getViewTypeCount() {
		return numViewTypes;
	}

	private ViewType getViewType(int pos) {
		synchronized (cursorLock) {
			ViewType vtype = viewTypeByPos.get(pos);
			if (vtype == null) {
				final Cursor cursor = getCursor();
				if (cursor != null && cursor.moveToPosition(pos)) {
					final long key = getIdKey(cursor);
					boolean load = LOAD_TEXT_ON_UI;
					if ((sendingMsgs.size() > 0 && sendingMsgs.get(key) != null) || (sentMmsMsgs != null && sentMmsMsgs.get(key) != null)) {
						load = false;
					}
					final MessageItem msgItem = getMessageItem(cursor, key, false, load);
					if (msgItem != null) {
						if (!msgItem.mSms) {
							if (msgItem.mMessageType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND ||
									msgItem.mMessageType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X) {
								vtype = ViewType.DOWNLOAD;
							} else if (msgItem.mLoaded && msgItem.mMediaLoaded) {
								if (msgItem.mAttachmentType != WorkingMessage.TEXT && msgItem.mAttachmentType != WorkingMessage.NONE) {
									vtype = ViewType.MEDIA;
								}
							}
							else {
								vtype = ViewType.MMS;
							}
						}
					}
					else {
						// invalid item in the cursor: force a re-query
						invalidCursor();
					}
				}
				else {
					Logger.error(this + ".getViewType: failed to move to " + pos + ", count = " +
						(cursor == null ? -1 : cursor.getCount()));
				}
				if (vtype == null) {
					vtype = ViewType.TEXT;
				}
				viewTypeByPos.put(pos, vtype);
			}
			return vtype;
		}
	}

	private ItemView getItemView(ViewType vtype) {
		switch (vtype) {
			case TEXT:
				return new TextItemView();
			case MEDIA:
				return new MediaItemView();
			case MMS:
				return new MmsItemView();
			case DOWNLOAD:
				return new DownloadItemView();
			default:
				throw new UnsupportedOperationException("No class for view type " + vtype);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// ensure we have enough queue space for the number of views
		numViews = mListView.getChildCount() + 1;
		final int minQueueSize = (int)(numViews * VIEW_QUEUE_FACTOR);
		if (minQueueSize > queueSize) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".newView: numViews = " + numViews + ", queueSize = " + queueSize);
			}
			queueSize = minQueueSize;
			loadWorker.resizeQueues(minQueueSize);
		}

		// get specific view type object and get a new view from it
		final ViewType vtype = getViewType(cursor.getPosition());
		return getItemView(vtype).newView(context, cursor, parent);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// delegate to the view type
		final ItemView itemView = (ItemView)view.getTag();
		itemView.bindView(view, context, cursor);

		if (Logger.IS_DEBUG_ENABLED) {
			if (lastThreadId != 0 && mConversation.getThreadId() != lastThreadId) {
				Logger.debug(this + ".bindView: last thread id = " + lastThreadId + ", conv = " + mConversation.getThreadId());
//				throw new RuntimeException("last thread id = " + lastThreadId + ", conv = " + mConversation.getThreadId());
			}
		}
	}

	@Override
	public void onMovedToScrapHeap(View view) {
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(this + ".onMovedToScrapHeap: view = " + view);
//		}
//
//		cancelAllPreviewTask();
		
		// free resources that can be reclaimed
		if (view instanceof ViewGroup) {
			freeViewResources((ViewGroup)view);
		}
	}

	private void freeViewResources(ViewGroup vg) {
		vg.destroyDrawingCache();
		final int num = vg.getChildCount();
		for (int i = 0; i < num; ++i) {
			final View view = vg.getChildAt(i);
			view.destroyDrawingCache();
			if (view instanceof ViewGroup) {
				freeViewResources((ViewGroup)view);
			}
			else if (view instanceof ImageView && view.getId() == R.id.mediaImage) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(this + ".freeViewResources: freeing bitmap from view = " + view);
				}
				((ImageView)view).setImageDrawable(null);
			}
		}
	}

	@Override
	public void freeBitmapMemory() {
		// free bitmap refs from the ImageModels in the sending and sent messages
		for (SendingMessage msg : sendingMsgs.values()) {
			if (msg.mms) {
				freeBitmapMemory(msg);
			}
		}
		for (SendingMessage msg : sentMmsMsgs.values()) {
			freeBitmapMemory(msg);
		}
	}

	@Override
	public void debugDump() {
		Logger.debug(MessageListAdapter.class, "----DebugBump - MLA ----");
		for (SendingMessage msg : sendingMsgs.values()) {
			if (msg.mms) {
				Logger.debug(MessageListAdapter.class, msg.toString());
			}
		}
		for (SendingMessage msg : sentMmsMsgs.values()) {
			if (msg.mms) {
				Logger.debug(MessageListAdapter.class, msg.toString());
			}
		}
	}

	private void freeBitmapMemory(SendingMessage msg) {
		final SlideshowModel model = msg.model;
		if (model != null) {
			for (SlideModel slide : model) {
				for (MediaModel media : slide.getMedia()) {
					if (media instanceof ImageModel) {
						((ImageModel)media).freeBitmapMemory();
					}
				}
			}
		}
	}

	private void invalidCursor() {
		if (errors++ < MAX_ERROR_RETRIES) {
			changeListener.onContentChanged();
		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			final int what = msg.what;
			if (what == MSG_LOADED) {
				// update the item if it is still valid
				synchronized (messageItemCache) {
					final ItemView itemView = (ItemView)msg.obj;
					final int pos = msg.arg1;
					if (isValidRequest(pos, itemView)) {
						try {
							final Cursor cursor = getCursor();
							if (cursor != null) {
								cursor.moveToPosition(pos);
							}

							// if stale item was being used then switch it with the newly loaded one
							MessageItem msgItem = itemView.newItem;
							if (msgItem != null) {
								itemView.msgItem = msgItem;
								itemView.newItem = null;
							}
							else {
								msgItem = itemView.msgItem;
							}
							oldMessageItemCache.put(getIdKey(msgItem), msgItem);

							itemView.setStatus(msgItem.mDeliveryStatus, false, true);
							bindView(itemView.view, mContext, cursor);
						}
						catch (Exception e) {
							Logger.error(MessageListAdapter.this, e);
						}
					}
				}
			}

			else if (what == MSG_CHANGE_CURSOR) {
				synchronized (cursorLock) {
					Cursor cursor = (Cursor)msg.obj;
					final boolean force = msg.arg1 != 0;
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(MessageListAdapter.this + ".handleMessage: cursor = " + cursor + ", force = " + force);
					}
					if (cursor == null || !cursor.isClosed()) {
						updateCursor(cursor, force);
					}
				}
			}

			else if (what == MSG_UNREGISTER) {
				final CursorObserver obs = (CursorObserver)msg.obj;
				try {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(MessageListAdapter.this + ".handleMessage: unregistering " + obs.cursor);
					}
					obs.cursor.unregisterDataSetObserver(obs);
				}
				catch (Exception e) {
					Logger.error(getClass(), e);
				}
			}
		}
	};

	private void updateCursor(Cursor cursor, boolean force) {
		// if this is a forced update then first try the deferred and then the last base cursor,
		// creating an empty one if neither exists
		//
		if (force) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".updateCursor: cursor = " + cursor +
					", deferred = " + deferredCursor + ", base = " + baseCursor);
			}
			if (cursor == null) {
				cursor = deferredCursor;
				if (cursor == null) {
					cursor = baseCursor;
					if (cursor == null) {
						cursor = new MatrixCursor(PROJECTION);
					}
				}
			}
		}

		changeCursor(cursor, force);
	}

	public boolean isValidRequest(int pos, Object data) {
		// return true if the item at pos is still valid
		final ItemView itemView = (ItemView)data;
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".isValidRequest: pos / item = " + pos + " / " + itemView.curPos + ", id = " + itemView.msgItem.mMsgId);
		}
		return itemView.curPos == pos;
	}

	public long getIdKey(MessageItem msgItem) {
		return getIdKey(msgItem.mMsgId, !msgItem.mSms);
	}

	public long getIdKey(Cursor cursor) {
		final long id = cursor.getLong(COLUMN_ID);
		final String type = cursor.getString(COLUMN_MSG_TYPE);
		return getIdKey(id, type.charAt(0) == 'm');
	}

	private long getIdKey(long id, boolean mms) {
		// sms and mms have overlapping id spaces so we need to distinguish them
		return mms ? -id : id;
	}

	public MessageItem getSelectedMessageItem() {
		return getMessageItem(mListView.getSelectedView());
	}

	public MessageItem getMessageItem(View view) {
		if (view != null) {
			final Object o = view.getTag();
			if (o instanceof ItemView) {
				return ((ItemView)o).msgItem;
			}
		}
		Logger.error(this + ".getMessageItem: no item view for " + view, Util.getStackTrace());
		return null;
	}

	/**
	 * Return the first url in the text of the message in the item view, or null if none exists
	 */
	public String getSelectedUrl(View view) {
		String url = null;
		// if it's a text view then get its url, if any
		if (view != null) {
			final Object o = view.getTag();
			if (o instanceof TextItemView) {
				url = ((TextItemView)o).getUrl();
			}
		}
		return url;
	}

	/**
	 * Return the group mode of the last outgoing message or null if not a group thread.
	 */
	public GroupMode getGroupMode() {
		return groupMode;
	}

	void onItemClick(View view) {
		final Object o = view.getTag();
		if (o instanceof ItemView) {
			((ItemView)o).onClick();
		}
		else {
			Logger.error(this + ".onItemClick: tag = " + o);
		}
	}

	public interface OnContentChangedListener {
		void onContentChanged(MessageListAdapter adapter);
		
		long getThreadId();
	}

	public void setOnDataSetChangedListener(OnContentChangedListener l) {
		mOnDataSetChangedListener = l;
	}

	public void setMsgListItemHandler(Handler handler) {
		mMsgListItemHandler = handler;
	}

	@Override
	protected void changeCursor(final Cursor newCursor, boolean force) {
		synchronized (cursorLock) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".changeCursor: deferredCursor = " + deferredCursor +
					", newCursor = " + newCursor);
			}

			// close the existing deferred cursor if it's not in use
			final Cursor deferred = deferredCursor;
			if (deferred != null && deferred != newCursor) {
				final Cursor current = getCursor();
				if (deferred != current) {
					// make sure it's not contained in a sending cursor
					if (!(current instanceof SendingCursor) || deferred != ((SendingCursor)current).getCursor(0)) {
						try {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(this + ".changeCursor: closing " + deferred);
							}
							deferred.close();
						}
						catch (Exception e) {
							Logger.error(this + ".changeCursor", e);
						}
					}
				}
			}

			// make sure we don't update the cursor while messages are being sent
			if (force || !processingMessages()) {
				deferredCursor = null;
				try {
					super.changeCursor(newCursor, force);
				}
				catch (Exception e) {
					Logger.error(this + ".changeCursor:", e);
				}
			}
			else {
				// schedule a callback 
				deferredCursor = newCursor;
				final Message msg = Message.obtain(handler, MSG_CHANGE_CURSOR, newCursor);
				handler.sendMessageDelayed(msg, CHANGE_CURSOR_DELAY);

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(this + ".changeCursor: deferred");
				}
			}

			if (newCursor != null) {
				// register an observer on the cursor to make sure we don't try to use it once it's closed
				if (!observedCursors.contains(newCursor)) {
					try {
						newCursor.registerDataSetObserver(new CursorObserver(newCursor));
						observedCursors.add(newCursor);
					}
					catch (IllegalStateException e) {
					}
					catch (Exception e) {
						Logger.error(e);
					}
				}
			}
		}
	}


	private class CursorObserver extends DataSetObserver {
		private Cursor cursor;

		public CursorObserver(Cursor cursor) {
			this.cursor = cursor;
		}

		@Override
		public void onInvalidated() {
			synchronized (cursorLock) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(this + ".onInvalidated: cursor = " + cursor + ", closed = " + cursor.isClosed() +
						", observedCursors = " + observedCursors);
				}
				if (cursor.isClosed()) {
					if (deferredCursor == cursor) {
						deferredCursor = null;
					}
					if (baseCursor == cursor) {
						baseCursor = null;
					}
					handler.removeMessages(MSG_CHANGE_CURSOR, cursor);
					observedCursors.remove(cursor);

					// unregister ourselves on a different stack to prevent concurrent modification
					final Message msg = Message.obtain(handler, MSG_UNREGISTER, this);
					handler.sendMessage(msg);
				}
			}
		}
	}


	@Override
	public void changeCursor(Cursor newCursor) {
		synchronized (cursorLock) {
			super.changeCursor(newCursor);
		}
	}

	private ChangeListener changeListener = new ChangeListener() {

		public void onContentChanged() {
			if (mOnDataSetChangedListener != null) {
				mOnDataSetChangedListener.onContentChanged(MessageListAdapter.this);
			}
		}

		public Cursor onCursorChanging(Cursor newCursor) {
			synchronized (cursorLock) {
				closeOnChange = true;
				if (newCursor != null) {
					if (Logger.IS_DEBUG_ENABLED) {
						if (newCursor instanceof SendingCursor) {
							throw new RuntimeException(this + ".onCursorChanging: changing to SendingCursor");
						}
					}

					// check if the cursor has a newly received message
					final IncomingMessageData inData = Conversation.getLastIncomingData(newCursor);
					if (inData != null) {
						final int pos = inData.pos;
						if (lastIncomingPos != -1 && pos > lastIncomingPos && lastIncomingAnimationId != inData.lastId) {
							receivingMsg = new MessageState(inData.lastIsMms, inData.lastId);
							lastIncomingAnimationId = inData.lastId;

							// make sure it gets cleared after a delay to allow for messages that
							// are never animated (e.g. because they aren't visible on the screen)
							//
							final Message msg = Message.obtain(animHandler, MSG_ANIM_DONE, receivingMsg);
							animHandler.sendMessageDelayed(msg, ANIM_DONE_DELAY);
						}
						else {
							receivingMsg = null;
						}
						lastIncomingPos = pos;
						receivedXids = inData.receivedXids;

						// if we've been notified of received messages and they're in the new cursor then
						// clear their state since they are no longer pending
						//
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(this + ".onCursorChanging: lastReceivedMmsId = " + lastReceivedMmsId +
								", lastReceivedSmsId = " + lastReceivedSmsId);
						}
						if (lastReceivedMmsId != 0 && inData.lastMmsId >= lastReceivedMmsId) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(this + ".onCursorChanging: set lastReceivedMmsId to 0");
							}
							lastReceivedMmsId = 0;
						}
						if (lastReceivedSmsId != 0 && inData.lastSmsId >= lastReceivedSmsId) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(this + ".onCursorChanging: set lastReceivedSmsId to 0");
							}
							lastReceivedSmsId = 0;
						}
					}
					else {
						receivingMsg = null;
						lastIncomingPos = -1;
						receivedXids = new HashSet<String>(0);
					}

					// if the last outgoing message id has been bumped then reset the changing flag
					final OutgoingMessageData outData = Conversation.getLastOutgoingData(mContext, newCursor, groupThread && groupMode == null);
					if (outData != null) {
						if (groupMode == null) {
							groupMode = outData.groupMode;
						}
						lastOutgoingPos = outData.pos;
					}
					else {
						groupMode = null;
						lastOutgoingPos = -1;
					}
					
					//if we dont have a new message received or a message sent then dont scroll the list to the bottom
					if (((outData != null && outData.time <= lastOutGoingMessageTime) || outData == null) 
							&& receivingMsg == null && !sendingMessages) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(this + ".onCursorChanging: dont scroll - recvMsg:" + receivingMsg + 
									" sendMsg:" + sendingMessages);
						}
						cursorChanging = false;
						messageAdded = false;
						mListView.ignoreTouchOnCursorChange();
						prevOutgoingPos = lastOutgoingPos;
					} else {
						mListView.cancelIgnoreTouchOnCursorChange();
					}
					
					if (outData != null) {
						lastOutGoingMessageTime = outData.time;
					}
					
					if (lastOutgoingPos != prevOutgoingPos) {
						cursorChanging = false;
						messageAdded = lastOutgoingPos > prevOutgoingPos;
						prevOutgoingPos = lastOutgoingPos;
					}
					prevOutgoingPos = lastOutgoingPos;
					
					// don't close the old cursor if it is being reused
					final Cursor oldCursor = getCursor();
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(this + ".onCursorChanging: oldCursor = " + oldCursor);
					}
					if (oldCursor != null) {
						if (oldCursor instanceof SendingCursor) {
							// get the base cursor from the merge cursor and check it
							final SendingCursor oldSendingCursor = (SendingCursor)oldCursor;
							final Cursor oldBaseCursor = oldSendingCursor.getCursor(0);
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(this + ".onCursorChanging: oldBaseCursor = " + oldBaseCursor +
									", newCursor = " + newCursor);
							}
							if (newCursor == oldBaseCursor) {
								// prevent the merge cursor from closing the old cursor when it's closed
								oldSendingCursor.clear(0);
							}
						}
						else if (newCursor == oldCursor) {
							// prevent the old cursor from being closed
							closeOnChange = false;
						}
					}

					baseCursor = newCursor;
					lastPos = newCursor.getCount() - 1;
					firstOutgoingPos = Conversation.getFirstOutgoingPos(newCursor);

					// check if we need to merge any sending messages with the new cursor
					if (sendingMessages) {
						newCursor = mergeSendingMessages(newCursor);
					}
				}

				else {  // null cursor
					baseCursor = null;
					firstOutgoingPos = lastOutgoingPos = lastPos = -1;
				}

				deferredCursor = null;
				
				if (threadChanged) {
					lastThreadId = mOnDataSetChangedListener.getThreadId();
				} else {
					long curThreadId = mOnDataSetChangedListener.getThreadId();
					threadChanged = lastThreadId != mOnDataSetChangedListener.getThreadId();
					lastThreadId = curThreadId;
				}
				createCaches(newCursor);
				viewTypeByPos.clear();
				if (loadWorker != null) {
					loadWorker.clear();
				}

				return newCursor;
			}
		}
	};

	/**
	 * Merge in dummy items for sending messages if needed; assumes caller is holding cursorLock.
	 * @param newCursor Base cursor with real messages
	 * @return A merged cursor with dummy rows for sending messages or newCursor if no sending messages
	 */
	private Cursor mergeSendingMessages(Cursor newCursor) {
		// check each possible type of sending messages
		int stillSending = 0;
		for (final SendingState state : sendingStates) {
			int sending = state.numSending;
			if (sending > 0) {
				// get the ids of messages sent since the checkpointed id and date
				final long lastId = state.lastId;
				if (Logger.IS_DEBUG_ENABLED && lastId == -1) {
					throw new RuntimeException(this + ": bad id for " + state);
				}

				final boolean mms = state.mms;
				final List<Long> ids = Conversation.getMessagesSince(newCursor, lastId, state.lastDate, mms);
				final int sent = ids.size();

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(this + ".mergeSendingMessages: was sending " + sending +
						(mms ? " mms" : " sms") + ", have sent " + ids + " since id " + lastId);
				}

				// remove sent messages from sending messages map
				for (long id : ids) {
					removeSendingMessage(id, mms);
				}

				// update the count of messages to send
				sending -= sent;
				if (sending > 0) {
					// still some to send
					stillSending += sending;
				}
				else {
					if (sending < 0) {  // shouldn't happen
						Logger.error(this + ".mergeSendingMessages: sending underflow: sending = " + sending +
							", sent = " + sent + ", state = " + state);
					}

					// reset sending state
					state.numSending = 0;
					state.lastId = -1;
					state.lastDate = 0;
				}
			}
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".mergeSendingMessages: stillSending = " + stillSending +
				", sendingMsgs = " + dumpSendingMsgs());
		}

		// if we still have any sending messages then add dummy items for them to a merge cursor
		if (stillSending > 0) {
			final MatrixCursor sendingCursor = new MatrixCursor(PROJECTION, stillSending);
			final ArrayList<SendingMessage> added = new ArrayList<SendingMessage>(sendingMsgs.size());
			for (SendingMessage smsg : sendingMsgs.values()) {
				if (!added.contains(smsg)) {
					added.add(smsg);

					// create dummy row from sending message data
					sendingCursor.addRow(smsg.getRow());
	
					if (smsg.state == State.INITIAL) {
						smsg.state = State.IN_CURSOR;
		
						// make sure it gets marked as finished after a delay to allow for messages that
						// are never animated (e.g. because they aren't visible on the screen)
						//
						final Message msg = Message.obtain(animHandler, MSG_ANIM_DONE, smsg);
						animHandler.sendMessageDelayed(msg, ANIM_DONE_DELAY);

						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(MessageListAdapter.this + ".mergeSendingMessages: set " + smsg);
						}
					}
				}
			}

			// create merge cursor containing the real items followed by the dummy items
			try {
				newCursor = new SendingCursor(new Cursor[] { newCursor, sendingCursor });
			}
			catch (Exception e) {
				Logger.error(e);
			}
		}
		else {
			sendingMessages = false;
		}

		return newCursor;
	}

	private void animationDone(MessageState msg) {
		if (msg != null) {
			// set the message state and clear the receiving message if appropriate
			msg.state = msg.sent ? State.SENT : State.FINISHED;
			if (msg == receivingMsg) {
				receivingMsg = null;
			}

			// clear the view's animation
			final View view = msg.view;
			if (view != null) {
				view.clearAnimation();
				msg.view = null;
			}

			animHandler.removeMessages(MSG_ANIM_DONE, msg);
		}
		else if (Logger.IS_ERROR_ENABLED) {
			Logger.error(MessageListAdapter.this + ".animationDone: no msg");
		}

		// if no other animations are running and we have pending animations then
		// force a cursor update, which will add the messages to be animated
		//
		if (animationReady()) {
			updateCursor(null, true);
		}
	}

	private Handler animHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(MessageListAdapter.this + ".animHandler: msg = " + msg);
			}
			if (msg.what == MSG_ANIM_DONE) {
				synchronized (cursorLock) {
					animationDone((MessageState)msg.obj);
				}
			}
		}
	};

	private class SendingCursor extends MergeCursor {
		private final Cursor[] cursors;
		private final int length;

		public SendingCursor(Cursor[] cursors) {
			super(cursors);
			this.cursors = cursors;
			this.length = cursors.length;
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".init: cursors = " + Arrays.asList(cursors));
			}
		}

		public Cursor getCursor(int which) {
			return valid(which) ? cursors[which] : null;
		}

		public void clear(int which) {
			if (valid(which)) {
				cursors[which] = null;
			}
		}

		private boolean valid(int which) {
			if (which >= 0 && which < length && cursors[which] != null && !cursors[which].isClosed()) {
				return true;
			}
			else {
				Logger.error(MessageListAdapter.this + ".SendingCursor.valid: invalid which " +
					which + ", cursors = " + Arrays.asList(cursors));
			}
			return false;
		}

		public void close() {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".close: cursors = " + Arrays.asList(cursors));
			}
			super.close();
		}
	}

	/**
	 * Notification that the given number of messages of the given type are about to be sent.
	 * @param sending The number of messages being sent; can be > 1 e.g. for group messages,
	 *                or < 0 e.g. to adjust the count if an error occurs in sending
	 * @param mms True if the message(s) being sent are MMS
	 * @param last Last outgoing ID in thread or -1 if unknown
	 */
	public void onSendingMessages(List<SendingMessage> msgs, boolean mms, long last) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".onSendingMessages: mms = " + mms + ", last = " + last + ", msgs = " + msgs);
		}
		synchronized (cursorLock) {
			// add to sending messages map
			for (SendingMessage msg : msgs) {
				sendingMsgs.put(getIdKey(msg.id, msg.mms), msg);
			}

			// if there were no prior sending messages of this type then checkpoint the last outgoing id
			final SendingState state = mms ? mmsSendingState : smsSendingState;
			final int sending = msgs.size();
			haveSending = sending != 0;
			if (sending > 0 && state.numSending == 0) {
				if (last == -1) {
					last = Conversation.getLastOutgoingId(mContext, mConversation.getThreadId(), mms);
				}
				state.lastId = last;
				final long now = System.currentTimeMillis();
				state.lastDate = (mms ? now / 1000 : now) - 1;
			}

			// update the count of sending messages for this message type
			adjustSendingMessages(state, sending);

			// if we're not currently animating then force a cursor change, which will add
			// dummy items for the sending messages to the cursor
			//
			if (!animating()) {
				handler.removeMessages(MSG_CHANGE_CURSOR);
				final Message msg = Message.obtain(handler, MSG_CHANGE_CURSOR);
				msg.arg1 = 1;  // force
				handler.sendMessage(msg);
			}
		}
	}

	public void onMessagesSent(List<SendingMessage> msgs) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".onMessagesSent: msgs = " + msgs + ", sending = " + dumpSendingMsgs());
		}
		synchronized (cursorLock) {
			for (SendingMessage msg : msgs) {
				// put it in the sending map under its valid id
				final boolean mms = msg.mms;
				final long key = getIdKey(msg.id, mms);
				sendingMsgs.put(key, msg);

				// update the message state
				msg.sent = true;
				if (msg.state == State.FINISHED) {
					msg.state = State.SENT;
				}

				// save MMS messages
				if (mms) {
					sentMmsMsgs.put(key, msg);
				}

				// if the message had a send error then update the count of sending messages for
				// its message type
				//
				if (msg.error != 0) {
					final SendingState state = mms ? mmsSendingState : smsSendingState;
					adjustSendingMessages(state, -1);
				}
			}

			haveSending = msgs.size() != 0;
			haveSent = sentMmsMsgs.size() != 0;
		}
	}

	private void adjustSendingMessages(SendingState state, int sending) {
		// update the count of sending messages for this message type
		int totalSending = sending + state.numSending;

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".adjustSendingMessages: was sending " + state.numSending +
				(state.mms ? " mms" : " sms") + ", now sending " + totalSending + ": " + dumpSendingMsgs());
		}

		state.numSending = totalSending;
		if (totalSending > 0) {
			sendingMessages = true;
			cursorChanging = true;
		}
		else if (totalSending == 0) {
			// clear sending state and checkpointed id
			sendingMessages = false;
			state.lastId = -1;
			state.lastDate = 0;
		}
		else {
			// shouldn't happen
			totalSending = 0;
			Logger.error(this + ".adjustSendingMessages: sending underflow: sending = " + totalSending + ", mms = " + state.mms);
		}
	}

	private void removeSendingMessage(long id, boolean mms) {
		// try to remove the sending message with this id
		long key = getIdKey(id, mms);
		final SendingMessage msg = sendingMsgs.remove(key);
		if (msg != null) {
			// remove its entry under its temp id
			key = getIdKey(msg.tempId, mms);
			if (sendingMsgs.remove(key) == null) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.error(this + ".removeSendingMessage: msg not found under temp id: " + msg);
				}
			}

			final MessageItem item = messageItemCache.remove(key);

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".removeSendingMessage: " + msg +
					(item == null ? ": not" : ":") + " removed from cache");
			}
		}
		else {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.error(this + ".removeSendingMessage: msg not found for key " + key);
			}
		}

		haveSending = sendingMsgs.size() != 0;
	}

	/**
	 * Returns true if any sent messages are waiting to be animated or are about
	 * to be (in the SendingCursor).
	 */
	private boolean animating() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".animating: receivingMsg = " + receivingMsg +
				". sendingMsgs = " + dumpSendingMsgs());
		}
		for (SendingMessage msg : sendingMsgs.values()) {
			if (msg.state == State.IN_CURSOR || msg.state == State.ANIMATING) {
				return true;
			}
		}
		if (receivingMsg != null && receivingMsg.state == State.ANIMATING) {
			return true;
		}
		return false;
	}

	/**
	 * Returns true if there are any messages that are in the process of being
	 * sent or received.
	 */
	private boolean processingMessages() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".processingMessages: receivingMsg = " + receivingMsg +
				", sendingMsgs = " + dumpSendingMsgs());
		}
		for (SendingMessage msg : sendingMsgs.values()) {
			if (msg.state != State.SENT) {
				return true;
			}
		}
		return receivingMsg != null;
	}

	/**
	 * Returns true if any sent messages are waiting to be animated and none are currently
	 * being animated or are about to be (in the SendingCursor).
	 */
	private boolean animationReady() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".animationReady: sendingMsgs = " + dumpSendingMsgs());
		}
		boolean pending = false;
		for (SendingMessage msg : sendingMsgs.values()) {
			if (msg.state == State.IN_CURSOR || msg.state == State.ANIMATING) {
				return false;
			}
			if (msg.state == State.INITIAL) {
				pending = true;
			}
		}
		return pending;
	}

	private final NotificationListener notificationListener = new NotificationListener() {
		public void onMessagesReceived(Map<Long, List<Uri>> threadData) {
			lastMessagesReceived = threadData;
			checkReceivedMessages();
		}
	};

	private void checkReceivedMessages() {
		// if there are any received messages for this thread then get the id of the last one
		if (lastMessagesReceived != null) {
			final List<Uri> uris = lastMessagesReceived.get(lastThreadId);
			if (uris != null) {
				try {
					long maxMmsId = 0;
					long maxSmsId = 0;
					for (Uri uri : uris) {
						final long id = ContentUris.parseId(uri);
						if (uri.toString().contains("/mms/")) {
							if (id > maxMmsId) {
								maxMmsId = id;
							}
						}
						else if (id > maxSmsId) {
							maxSmsId = id;
						}
					}
					if (maxMmsId != 0) {
						lastReceivedMmsId = maxMmsId;
					}
					if (maxSmsId != 0) {
						lastReceivedSmsId = maxSmsId;
					}
				}
				catch (Exception e) {
					Logger.error(getClass(), "checkReceivedMessages", e);
				}
			}
		}
	}
	
	
	@Override
	public void onContentChanged() {
		
	}
	
	
	public void onConversationContentChanged() {
		super.onContentChanged();
	}

	@Override
	protected long getContentChangeDelay() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".getContentChangeDelay: cursorChanging = " + cursorChanging +
				", lastReceivedSmsId = " + lastReceivedSmsId + ", lastReceivedMmsId = " + lastReceivedMmsId);
		}
		synchronized (cursorLock) {
			long delay;
			if (processingMessages()) {
				// we have animations running and/or pending so delay until they're done
				delay = -1;
			}
			else if (cursorChanging || lastReceivedSmsId != 0 || lastReceivedMmsId != 0) {
				// the cursor is expected to change due to a message being sent, received or deleted
				// so return a minimal delay so that we update the display quickly
				//
				delay = MINIMUM_CONTENT_CHANGE_DELAY;
			}
			else if (textEntryStateProvider != null &&
				SystemClock.uptimeMillis() - textEntryStateProvider.getLastTextEntryTime() < MAX_TEXT_ENTRY_DELAY) {
					// the user has entered text recently so defer the cursor update to prevent
					// updates from causing text entry to stutter
					//
					delay = -1;
			}
			else {
				// if any of the visible and recent outgoing messages have non-terminal delivery status then return
				// a small delay so that we display status updates quickly but don't thrash too much
				//
				delay = -1;
				final long now = System.currentTimeMillis();
				final ListView list = mListView;
				boolean smsVisible = false;
				for (int i = list.getChildCount() - 1; i >= 0; --i) {
					final View view = list.getChildAt(i);
                    if (view != null) {
					    final Object itemView = view.getTag();
	                    if (itemView instanceof ItemView) {
	                        final MessageItem item = ((ItemView) itemView).msgItem;
	                        if (Logger.IS_DEBUG_ENABLED) {
	                            Logger.debug(this + ".getContentChangeDelay: item = " + item);
	                        }
	                        if (item != null) {
	                            if (item.isOutboundMessage()
	                                    && now - item.mMsgTime <= MAX_CONTENT_CHANGE_MESSAGE_AGE
	                                    && !item.getDeliveryStatus(true).isTerminal()) {
	                                delay = SMALL_CONTENT_CHANGE_DELAY;
	                                break;
	                            }
	                            smsVisible = item.isSms();
	                        }
                        }
                    }
				}
				
				if (delay == -1) {
					delay = smsVisible ? DEFAULT_SMS_CONTENT_CHANGE_DELAY : DEFAULT_MMS_CONTENT_CHANGE_DELAY;
				}
			}

			return delay;
		}
	}

	@Override
	public void setCursorChanging(boolean cursorChanging) {
		super.setCursorChanging(cursorChanging);
		prevOutgoingPos = lastOutgoingPos;
	}

	private MessageItem getMessageItem(Cursor cursor, long key, boolean old) {
		return getMessageItem(cursor, key, old, LOAD_TEXT_ON_UI);
	}
	
	private MessageItem getMessageItem(Cursor cursor, long key, boolean old, boolean load) {
		synchronized (messageItemCache) {
			final ItemCache cache = old ? oldMessageItemCache : messageItemCache;
			MessageItem item = cache.get(key);

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".getMessageItem: " + (old ? "old" : "new") + " cache " +
					(item == null ? "miss" : "hit") + " for key " + key);
			}

			if (item == null && isCursorValid(cursor)) {
				try {
					item = new MessageItem(mContext, cursor, mHighlight, true, load);
					if (!old) {
						cache.put(key, item);
					}
				}
				catch (Exception e) {
					Logger.error(this + ": failed to get message for key " + key, e);
				}
			}
			return item;
		}
	}

	private boolean isCursorValid(Cursor cursor) {
		return cursor != null && !cursor.isClosed() && !cursor.isBeforeFirst() && !cursor.isAfterLast();
	}

	public boolean sameItem(View view1, View view2) {
		final MessageItem item1 = getMessageItem(view1);
		final MessageItem item2 = getMessageItem(view2);
		return item1 != null && item2 != null && item1.mSms == item2.mSms && item1.mMsgId == item2.mMsgId;
	}

	private String dumpSendingMsgs() {
		final Map<Long, SendingMessage> sendingMsgs = this.sendingMsgs;
		final Set<Long> keySet = sendingMsgs.keySet();
		final List<Long> keys = Arrays.asList(keySet.toArray(new Long[keySet.size()]));
		Collections.sort(keys, new Comparator<Long>() {
			@Override
			public int compare(Long lhs, Long rhs) {
				return (int)(lhs - rhs);
			}
		});
		final StringBuilder sb = new StringBuilder("[");
		for (Long key : keys) {
			sb.append("\n    ");
			sb.append(key);
			sb.append(" = ");
			sb.append(sendingMsgs.get(key));
		}
		if (keys.size() != 0) {
			sb.append("\n");
		}
		sb.append("]");
		return sb.toString();
	}
	
    /**
    * Set the bitmap to use in an ImageView.
    * 
    * Bitmap is set only if any side of the bitmap is larger than the minimum dimension required.
    * 
    * @param imageView
    * @param bitmap
    * @param minDimension
    * @return
    */
	private boolean setImageBitmap(ImageView imageView, Bitmap bitmap, int minDimension) {
		if (minDimension > 0 && bitmap != null && 
				(bitmap.getWidth() >= minDimension || bitmap.getHeight() >= minDimension)) {
			imageView.setImageBitmap(bitmap);
			imageView.setVisibility(View.VISIBLE);
			imageView.requestLayout();
			return true;
		}    	
		return false;
	}
	
	/**
	 * Object to be set as tag in the preivew ImageView
	 */
	private class LinkTag {
		String url = null;;
	};
	
	public void cancelAllPreviewTask() {
		if (mListView != null) {
        	// look for the View that has the request Id
			int size = mListView.getChildCount();
			for (int i=0; i<size; i++) {
				View child = (View)mListView.getChildAt(i);
				if (child != null) {
					ImageView linkImage = (ImageView)child.findViewById(R.id.linkImage);;
					if (linkImage != null) {
						LinkTag tag = (LinkTag)linkImage.getTag();
						
						if (tag != null) {
							linkImage.setTag(null);
							BitmapManager.INSTANCE.cancelRequest(mPreviewHandler, tag.url);
						}
					}
				}
			}
		}
	}
	
	public void updateThreadMode() {
		if (mConversation != null) {
			groupThread = mConversation.getRecipients().size() > 1;
     Logger.debug("Getting recipients--------"+mConversation.getRecipients());
		}
	}
	
	public void updateBackGroundColor() {
		mBackgroundColor = ConversationResHelper.getBGColor();
		mFillBubble = ConversationResHelper.fillBubble();
		mTimeStampColor = ConversationResHelper.getTimeStampColor(mBackgroundColor);
		
		if (ConversationResHelper.isBrightColor(ConversationResHelper.getBGColor())) {
			mErrorIconId = R.drawable.ico_error_darkgray;
		} else {
			mErrorIconId = R.drawable.ico_error_white;
		}
	}

	
	public void updateThreadId(long threadId) {
		if (threadId != lastThreadId || deliveryStatus == null) {
			final Cursor cursor = getCursor();
			final int numMessages = cursor == null ? 256 : cursor.getCount();
			deliveryStatus = new HashMap<Long, Status>(numMessages);
			errors = 0;
		}

		threadChanged = true;

		lastThreadId = threadId;
		checkReceivedMessages();
	}

	public void setGroupMode(GroupMode mode) {
		groupMode = mode;
	}
	
    public void highLightMsg(long newHighLightedMsgId) {
        this.mHighLightedMsgId = newHighLightedMsgId;
    }

    private boolean isHighLightRequired(MessageItem msg) {
        //if both MsgId's are same , then check r both sms or mms types  same?.
        if (mHighLightedMsgId != 0 && msg.mMsgId == Math.abs(mHighLightedMsgId)) {
            // if msgId is <0 then treat its an mms as  we are passing -ve for mms from search screen.
            if (mHighLightedMsgId < 0) {
                return msg.isMms() ? true : false;
            } else {
                return msg.isSms() ? true : false;
            }
        }
        return false;
    }
    
    public void registerContentChangeObserver() {
    	if (contentChangeListener != null) {
			ConversationDataObserver.addConvDataListener(contentChangeListener);
		}
    }
    
    public void unregisterContentChangeObserver() {
    	if (contentChangeListener != null) {
			ConversationDataObserver.removeConvDataListener(contentChangeListener);
		}
    }
}
