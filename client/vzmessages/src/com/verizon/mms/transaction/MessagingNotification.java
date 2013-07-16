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

package com.verizon.mms.transaction;

import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;
import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ContentType;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.data.RecipientIdCache;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.GenericPdu;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.ui.AdvancePreferenceActivity;
import com.verizon.mms.ui.ComposeMessageFragment;
import com.verizon.mms.ui.MessageItem;
import com.verizon.mms.ui.MessageItem.ReportStatus;
import com.verizon.mms.ui.MessagingPreferenceActivity;
import com.verizon.mms.ui.PopUpNotificationActivity;
import com.verizon.mms.ui.VZActivityHelper;
import com.verizon.mms.util.AddressUtils;
import com.verizon.mms.util.DownloadManager;
import com.verizon.mms.util.EmojiParser;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.Util;

/**
 * This class is used to update the notification indicator. It will check whether there are unread messages.
 * If yes, it would show the notification indicator, otherwise, hide the indicator.
 */
public class MessagingNotification {
	private static final ArrayList<NotificationListener> listeners = new ArrayList<NotificationListener>(2);
    private static final long NOTIFICATION_DURATION = 3000;
    private static final int NOTIFICATION_ID = 123;
    public static final int MESSAGE_FAILED_NOTIFICATION_ID = 789;
    public static final int DOWNLOAD_FAILED_NOTIFICATION_ID = 531;

    // This must be consistent with the column constants below.
    private static final String[] MMS_STATUS_PROJECTION = new String[] { Mms.THREAD_ID, Mms.DATE, Mms._ID,
            Mms.SUBJECT, Mms.SUBJECT_CHARSET };

    // This must be consistent with the column constants below.
    private static final String[] SMS_STATUS_PROJECTION = new String[] { Sms.THREAD_ID, Sms.DATE,
            Sms.ADDRESS, Sms.SUBJECT, Sms.BODY, Sms._ID };

    // These must be consistent with MMS_STATUS_PROJECTION and
    // SMS_STATUS_PROJECTION.
    private static final int COLUMN_THREAD_ID = 0;
    private static final int COLUMN_DATE = 1;
    private static final int COLUMN_MMS_ID = 2;
    private static final int COLUMN_SMS_ADDRESS = 2;
    private static final int COLUMN_SUBJECT = 3;
    private static final int COLUMN_SUBJECT_CS = 4;
    private static final int COLUMN_SMS_BODY = 4;
    // [JEGA] For WIFI SYNC
    private static final int COLUMN_SMS_MSG_ID = 5;

    private static final String NEW_INCOMING_SM_CONSTRAINT = "(" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_INBOX
            + " AND " + Sms.SEEN + " = 0)";

    private static final String NEW_DELIVERY_SM_CONSTRAINT = "(" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_SENT
            + " AND " + Sms.STATUS + " = " + Sms.STATUS_COMPLETE + ")";

    private static final String NEW_INCOMING_MM_CONSTRAINT = "(" + Mms.MESSAGE_BOX + "="
            + Mms.MESSAGE_BOX_INBOX + " AND " + Mms.SEEN + "=0" + " AND (" + Mms.MESSAGE_TYPE + "="
            + MESSAGE_TYPE_NOTIFICATION_IND + " OR " + Mms.MESSAGE_TYPE + "=" + MESSAGE_TYPE_RETRIEVE_CONF
            + "))";
    
    private static final String NEW_INCOMING_MM_DOWNLOADED_CONSTRAINT = "(" + Mms.MESSAGE_BOX + "="
            + Mms.MESSAGE_BOX_INBOX + " AND " + Mms.READ + "=0 and thread_id != -1" + " AND (" 
            + Mms.MESSAGE_TYPE + "=" + MESSAGE_TYPE_RETRIEVE_CONF
            + "))";
    
    private static final String NEW_INCOMING_SM_UNREAD_CONSTRAINT = "(" + Sms.TYPE + " = " + Sms.MESSAGE_TYPE_INBOX
          + " AND " + Sms.READ + " = 0 and thread_id != -1)";
    
    private static final String RECIP_ID_PROJECTION[] = new String[]{Threads.RECIPIENT_IDS};
    
    private static final MmsSmsNotificationInfoComparator INFO_COMPARATOR = new MmsSmsNotificationInfoComparator();
    
    private static final PopUpInfoComparator POPUP_TIME_COMPARATOR = new PopUpInfoComparator();

    private final static String NOTIFICATION_DELETED_ACTION = "com.android.mms.NOTIFICATION_DELETED_ACTION";

    public static class OnDeletedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "[MessagingNotification] clear notification: mark all msgs seen");
            }

            Conversation.markAllConversationsAsSeen(context);
        }
    };

    private static OnDeletedReceiver sNotificationDeletedReceiver = new OnDeletedReceiver();
    private static Intent sNotificationOnDeleteIntent;
   // private static Handler mToastHandler = new Handler();
    private static ToastHandler mToastHandler = new ToastHandler();

    public static final int GREEN_LED = 0xFF00FF00;
    public static final int RED_LED = 0xFFFF0000;
    public static final int ORANGE_LED = 0xFFFF7F00;
    public static final int YELLOW_LED = 0xFFFFFF00;
    public static final int BLUE_LED = 0xFF0000FF;
    public static final int INDIGO_LED = 0xFF7F00FF;


    public interface NotificationListener {
    	public void onMessagesReceived(Map<Long, List<Uri>> threadData);
    }


    private MessagingNotification() {
    }

    public static void init(Context context) {
        // set up the intent filter for notification deleted action
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NOTIFICATION_DELETED_ACTION);
        context.registerReceiver(sNotificationDeletedReceiver, intentFilter);

        // initialize the notification deleted action
        sNotificationOnDeleteIntent = new Intent(NOTIFICATION_DELETED_ACTION);
    }

    public static void nonBlockingUpdateNewMessageIndicator(final Context context, final boolean isNew,
            final boolean isStatusMessage, final Uri messageUri) {
    	nonBlockingUpdateNewMessageIndicator(context, isNew, isStatusMessage, messageUri, true);
    }
    /**
     * Checks to see if there are any "unseen" messages or delivery reports. Shows the most recent
     * notification if there is one. Does its work and query in a worker thread.
     * 
     * @param context
     *            the context to use
     */
    public static void nonBlockingUpdateNewMessageIndicator(final Context context, final boolean isNew,
            final boolean isStatusMessage, final Uri messageUri, final boolean showPopUp) {
        new Thread(new Runnable() {
            public void run() {
                blockingUpdateNewMessageIndicator(context, isNew, isStatusMessage, messageUri, showPopUp);
            }
        }).start();
    }

    @SuppressWarnings("unchecked")
	public static void blockingUpdateNewMessageIndicator(Context context, boolean isNew,
            boolean isStatusMessage, Uri messageUri) {
    	blockingUpdateNewMessageIndicator(context, isNew, isStatusMessage, messageUri, true);
    }
    /**
     * Checks to see if there are any "unseen" messages or delivery reports. Shows the most recent
     * notification if there is one.
     * 
     * @param context
     *            the context to use
     * @param isNew
     *            if notify a new message comes, it should be true, otherwise, false.
     *@param updatePopUp used to specify if the popup has to be updated
     */
    @SuppressWarnings("unchecked")
	public static void blockingUpdateNewMessageIndicator(Context context, boolean isNew,
            boolean isStatusMessage, Uri messageUri, boolean updatePopUp) {
        SortedSet<MmsSmsNotificationInfo> accumulator = new TreeSet<MmsSmsNotificationInfo>(INFO_COMPARATOR);
        SortedSet<PopUpInfo> popUpInfos = new TreeSet<PopUpInfo>(POPUP_TIME_COMPARATOR);
        MmsSmsDeliveryInfo delivery = null;
        HashMap<Long, List<Uri>> threads = new HashMap<Long, List<Uri>>(4);

        int count = 0;
        if (isStatusMessage) {
	        delivery = getSmsNewDeliveryInfo(context,messageUri);
	        if (delivery != null) {
	            delivery.deliver(context, isStatusMessage);
	        }
	     
        } else {   
	        count += accumulateNotificationInfo(accumulator, getMmsNewMessageNotificationInfo(context, threads));
	        count += accumulateNotificationInfo(accumulator, getSmsNewMessageNotificationInfo(context, threads));
	       	cancelNotification(context, NOTIFICATION_ID);
	        if (!accumulator.isEmpty()) {
	            // if (Log.isLoggable(LogTag.APP, Log.VERBOSE)) {
	            if (Logger.IS_DEBUG_ENABLED) {
	                Logger.debug(MessagingNotification.class, "blockingUpdateNewMessageIndicator: count=" + count
	                        + ", isNew=" + isNew);
	            }
	            MmsSmsNotificationInfo element = accumulator.first();
	            element.deliver(context, isNew, count, threads.keySet().size());

	            // notify listeners
	            final ArrayList<NotificationListener> toNotify;
	            synchronized (listeners) {
					toNotify = (ArrayList<NotificationListener>)listeners.clone();
	            }
	            if (Logger.IS_DEBUG_ENABLED) {
	            	Logger.debug(MessagingNotification.class, "blockingUpdateNewMessageIndicator: notifying: " +
	            		toNotify + ", threadData = " + threads);
	            }
	            for (NotificationListener listener : toNotify) {
	            	listener.onMessagesReceived(threads);
	            }
	            
	            // show a popup if none of our activities are visible and the user wants them
	            if (!VZActivityHelper.isActivityOnTop() && updatePopUp) {
		            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
	            	if (sp.getBoolean(MessagingPreferenceActivity.POPUP_ENABLED, false)) {
			            if (messageUri != null) {
			            	 String msgUriAuthority = messageUri.getAuthority();
			            	 String msgID = messageUri.getLastPathSegment();
			 	             if (VZUris.getMmsAuthority().equalsIgnoreCase(msgUriAuthority)) {
			 	            	Cursor cursor = context.getContentResolver().query(VZUris.getMmsInboxUri(), new String[] { Mms._ID ,  Mms.MESSAGE_TYPE }, Mms._ID + " = " + msgID + " AND " + Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF , null, Mms.DATE + " DESC");
			 	            	if (cursor != null) {
			 	            		int msgCount = cursor.getCount();
			 	            		
			 	            		cursor.close();
			 	            		
			 	            		if (msgCount == 0) {
			 	            			return;
			 	            		}
			 	            	}
			 	             }
			            }
			           
			            
						popUpInfos = getAllUnReadMsgsInfo(popUpInfos, context);
						if (popUpInfos != null) {
							if (popUpInfos.size() > 0) {
								PopUpNotificationActivity
										.showPopUp(context, popUpInfos);
							}
						}
	            	}
	            }
	        }
        }  
	    // And deals with delivery reports (which use Toasts). It's safe to call in a worker
        // thread because the toast will eventually get posted to a handler.       
    }

    /**
     * Updates all pending notifications, clearing or updating them as necessary.
     */
   
    public static void blockingUpdateAllNotifications(final Context context, boolean isRecreateNotification) {
        nonBlockingUpdateNewMessageIndicator(context, false, false, null);
        updateSendFailedNotification(context,isRecreateNotification);
        updateDownloadFailedNotification(context);
    }
    //
    public static void blockingUpdateAllNotificationsWifiSync(final Context context, boolean isNew, boolean isRecreateNotification) {
        nonBlockingUpdateNewMessageIndicator(context, isNew, isRecreateNotification, null);
        updateSendFailedNotification(context,isRecreateNotification);
        updateDownloadFailedNotification(context);
    }

    private static final int accumulateNotificationInfo(SortedSet<MmsSmsNotificationInfo> set, MmsSmsNotificationInfo info) {
        if (info != null) {
            set.add(info);

            return info.mCount;
        }

        return 0;
    }

    private static final class MmsSmsDeliveryInfo {
        public CharSequence mTicker;
        public long mTimeMillis;

        public MmsSmsDeliveryInfo(CharSequence ticker, long timeMillis) {
            mTicker = ticker;
            mTimeMillis = timeMillis;
        }

        public void deliver(Context context, boolean isStatusMessage) {
            updateDeliveryNotification(context, isStatusMessage, mTicker, mTimeMillis);
        }
    }

    private static final class MmsSmsNotificationInfo {
        public Intent mClickIntent;
        public String mDescription;
        public int mIconResourceId;
        public CharSequence mTicker;
        public long mTimeMillis;
        public String mTitle;
        public int mCount;

        public MmsSmsNotificationInfo(Intent clickIntent, String description, int iconResourceId,
                CharSequence ticker, long timeMillis, String title, int count) {
            mClickIntent = clickIntent;
            mDescription = description;
            mIconResourceId = iconResourceId;
            mTicker = ticker;
            mTimeMillis = timeMillis;
            mTitle = title;
            mCount = count;
        }

        public void deliver(Context context, boolean isNew, int count, int uniqueThreads) {
        	// only display the ticker if the message is new
        	final CharSequence ticker = (isNew ? mTicker : null);
            updateNotification(context, mClickIntent, mDescription, mIconResourceId, isNew, ticker,
                    mTimeMillis, mTitle, count, uniqueThreads);
        }

        public long getTime() {
            return mTimeMillis;
        }
    }

    private static final class MmsSmsNotificationInfoComparator implements Comparator<MmsSmsNotificationInfo> {
        public int compare(MmsSmsNotificationInfo info1, MmsSmsNotificationInfo info2) {
            return Long.signum(info2.getTime() - info1.getTime());
        }
    }
    
    public static final class PopUpInfoComparator implements Comparator<PopUpInfo> {

		
		@Override
		public int compare(PopUpInfo pUI1, PopUpInfo pUI2) {
			// TODO Auto-generated method stub
			return Long.signum(pUI2.getDate() - pUI1.getDate());
		}
    	
    }

    private static final MmsSmsNotificationInfo getMmsNewMessageNotificationInfo(Context context,
            Map<Long, List<Uri>> threads) {
        ContentResolver resolver = context.getContentResolver();

        // This query looks like this when logged:
        // I/Database( 147): elapsedTime4Sql|/data/data/com.android.providers.telephony/databases/
        // mmssms.db|0.362 ms|SELECT thread_id, date, _id, sub, sub_cs FROM pdu WHERE ((msg_box=1
        // AND seen=0 AND (m_type=130 OR m_type=132))) ORDER BY date desc

        final Uri mmsUri = VZUris.getMmsUri();
        Cursor cursor = SqliteWrapper.query(context, resolver, mmsUri, MMS_STATUS_PROJECTION,
                NEW_INCOMING_MM_CONSTRAINT, null, Mms.DATE + " desc");
        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            long msgId = cursor.getLong(COLUMN_MMS_ID);
            Uri msgUri = mmsUri.buildUpon().appendPath(Long.toString(msgId)).build();
            String address = AddressUtils.getFrom(context, msgUri);
            String subject = getMmsSubject(cursor.getString(COLUMN_SUBJECT), cursor.getInt(COLUMN_SUBJECT_CS));
            String body = null;
            long threadId = cursor.getLong(COLUMN_THREAD_ID);
            long timeMillis = cursor.getLong(COLUMN_DATE) * 1000;
            String selection = Mms.Part.CONTENT_TYPE + "='" + ContentType.TEXT_PLAIN + "'";
            
            Cursor query = resolver.query(VZUris.getMmsPartsUri(msgId), new String[] { Mms.Part.TEXT }, selection, null, null);
            if (query != null) {
            	while (query.moveToNext()) {
            		body = query.getString(0);
            	}
            	query.close();
            }
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(MessagingNotification.class, "getMmsNewMessageNotificationInfo: count=" + cursor.getCount() + ", first addr = "
                        + address + ", thread_id=" + threadId);
            }
            if (body == null && subject != null) {
            	body = subject;
            }
            MmsSmsNotificationInfo info = getNewMessageNotificationInfo(address, body, context,
                    R.drawable.stat_notify_mms, null, threadId, timeMillis, cursor.getCount());

            // add thread(s) and their message uri(s) to the map
            boolean fetch = false;
            do {
            	if (fetch) {
            		threadId = cursor.getLong(COLUMN_THREAD_ID);
                    msgId = cursor.getLong(COLUMN_MMS_ID);
                    msgUri = ContentUris.withAppendedId(mmsUri, msgId);
            	}
                List<Uri> msgUris = threads.get(threadId);
                if (msgUris == null) {
                	msgUris = new ArrayList<Uri>();
                	threads.put(threadId, msgUris);
                }
                msgUris.add(msgUri);
            } while (fetch = cursor.moveToNext());

            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(MessagingNotification.class, "getMmsNewMessageNotificationInfo: threads = " + threads);
            }

            return info;
        } finally {
            cursor.close();
        }
    }
    
    private static final MmsSmsDeliveryInfo getSmsNewDeliveryInfo(Context context, Uri messageUri) {
        ContentResolver resolver = context.getContentResolver();
        Uri msgUri =  VZUris.getSmsUri(); 
       
        if (messageUri != null)
        	msgUri = messageUri; 
        
        Cursor cursor = SqliteWrapper.query(context, resolver, msgUri, SMS_STATUS_PROJECTION,
                NEW_DELIVERY_SM_CONSTRAINT, null, Sms.DATE + " desc");

        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            
            String address = cursor.getString(COLUMN_SMS_ADDRESS);
            long timeMillis = 3000;
            String name = Contact.get(address, true, false).getName();
            return new MmsSmsDeliveryInfo(String.format(context.getString(R.string.delivery_toast_body),
                    name), timeMillis);

        } finally {
        	if (cursor != null) {
        		cursor.close();
        	}
        }
    }

    public static class PopUpInfo implements Parcelable {
    	private long threadId;
    	private long msgId;
    	private boolean isSMS;
    	private long date;
    	private String recipientId;
    	
		public PopUpInfo(long threadId, long msgId, boolean isSms, long date, String recipIds) {
			this.threadId = threadId;
			this.msgId = msgId;
			this.isSMS = isSms;
			this.date = date;
			this.recipientId = recipIds;
		}
		
		private PopUpInfo(Parcel in) {
			Bundle bundle = in.readBundle();
			
			threadId = bundle.getLong("thread_id");
			msgId = bundle.getLong("msg_id");
			isSMS = bundle.getBoolean("msg_state");
			date = bundle.getLong("msg_date");
			recipientId = bundle.getString("recipId");
			
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("PopUp ThreadID :: " + threadId
						+ " :: MsgId" + msgId
						+ " :: isSms" + isSMS
						+ " :: recipientId" + recipientId
						+ " :: date " + date);
			}
		}
		
		public PopUpInfo() {
			
		}
		
		public long getThreadId() {
			return threadId;
		}
		
		public long getMsgId() {
			return msgId;
		}
		
		public boolean isSms() {
			return isSMS;
		}
		
		public long getDate() {
			return date;
		}
		
		public String getRecipientId() {
			return recipientId;
		}
		
		/*public void setThreadId(Long tId) {
			this.threadId = tId;
		}
		
		public void setMsgId(String msgId) {
			this.msgId = msgId;
		}
		
		public void setMsgState(int value) {
			this.isSMS = value;
		}
		
		public void setMsgDate(long value) {
			this.date = value;
		}*/

		@Override
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug("PopUp :: smsState" + isSMS + " :: msgID" + msgId + " :: threadId" + threadId);	
        	}
        	Bundle bundle = new Bundle();
        	bundle.putLong("msg_id", msgId);
            bundle.putLong("thread_id", threadId);
            bundle.putBoolean("msg_state", isSMS);
            bundle.putLong("msg_date", date);
            bundle.putString("recipId", recipientId);
            parcel.writeBundle(bundle);
		}
		

        /*
         * Parcelable interface must also have a static field called CREATOR,
         * which is an object implementing the Parcelable.Creator interface.
         * Used to un-marshal or de-serialize object from Parcel.
         */
        public static final Parcelable.Creator<PopUpInfo> CREATOR =
                new Parcelable.Creator<PopUpInfo>() {
            public PopUpInfo createFromParcel(Parcel in) {
                return new PopUpInfo(in);
            }
     
            public PopUpInfo[] newArray(int size) {
                return new PopUpInfo[size];
            }
        };
	}
    
	/*public static class ParcelablePopUpInfo implements Parcelable {
		private PopUpInfo info;

		public PopUpInfo getPopUpInfo() {
			return info;
		}

		public ParcelablePopUpInfo(PopUpInfo info) {
			super();
			this.info = info;
		}

		private ParcelablePopUpInfo(Parcel in) {
			Bundle bundle = in.readBundle();
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("PopUp ThreadID :: " + bundle.getLong("thread_id")
						+ " :: MsgId" + bundle.getString("msg_id")
						+ " :: isSms" + bundle.getInt("msg_state")
						+ " :: date " + bundle.getLong("msg_date"));
			}
			info = new PopUpInfo();
			info.setMsgId(bundle.getString("msg_id"));
			info.setThreadId(bundle.getLong("thread_id"));
			info.setMsgState(bundle.getInt("msg_state"));
            info.setMsgDate(bundle.getLong("msg_date"));
		}
     
      
        @Override
        public int describeContents() {
            return 0;
        }
     
        
         * Actual object Serialization/flattening happens here. You need to
         * individually Parcel each property of your object.
         
        @Override
        public void writeToParcel(Parcel parcel, int flags) {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug("PopUp :: smsState" + info.isSms() + " :: msgID" + info.getMsgId() + " :: threadId" + info.getThreadId());	
        	}
        	Bundle bundle = new Bundle();
        	bundle.putString("msg_id", info.getMsgId());
            bundle.putLong("thread_id", info.getThreadId());
            bundle.putInt("msg_state",info.isSms());
            bundle.putLong("msg_date", info.getDate());
            parcel.writeBundle(bundle);
            
        }
     
        
         * Parcelable interface must also have a static field called CREATOR,
         * which is an object implementing the Parcelable.Creator interface.
         * Used to un-marshal or de-serialize object from Parcel.
         
        public static final Parcelable.Creator<ParcelablePopUpInfo> CREATOR =
                new Parcelable.Creator<ParcelablePopUpInfo>() {
            public ParcelablePopUpInfo createFromParcel(Parcel in) {
                return new ParcelablePopUpInfo(in);
            }
     
            public ParcelablePopUpInfo[] newArray(int size) {
                return new ParcelablePopUpInfo[size];
            }
        };
    }
*/



    
    
    
    
	private static SortedSet<PopUpInfo> getAllUnReadMsgsInfo(SortedSet<PopUpInfo> popUpInfoAcc, Context context) {
		ContentResolver resolver = context.getContentResolver();
		Cursor mmsCursor = null;
		Cursor smsCursor = SqliteWrapper.query(context, resolver, VZUris.getSmsUri(), SMS_STATUS_PROJECTION,
				NEW_INCOMING_SM_UNREAD_CONSTRAINT, null, Sms.DATE + " desc");
		try {
			HashMap<Long, String> recipIdMapping = new HashMap<Long, String>();
			
			if (smsCursor != null) {
				while(smsCursor.moveToNext()) {
					long threadId = smsCursor.getLong(COLUMN_THREAD_ID);
					long msgId = smsCursor.getLong(COLUMN_SMS_MSG_ID);
					long timeMillis = smsCursor.getLong(COLUMN_DATE);
					
					String recipId = recipIdMapping.get(threadId);
					if (recipId == null) {
						recipId = getRecipIds(threadId, context);
						recipIdMapping.put(threadId, recipId);
					}
					
					popUpInfoAcc.add(new PopUpInfo(threadId, msgId, true, timeMillis, recipId));
				}
			}   

			//Only Downloaded Messages are to be shown
			mmsCursor = SqliteWrapper.query(context, resolver, VZUris.getMmsUri(), MMS_STATUS_PROJECTION,
					NEW_INCOMING_MM_DOWNLOADED_CONSTRAINT, null, Mms.DATE + " desc");
			if (mmsCursor != null) {
				while(mmsCursor.moveToNext()) {
					long msgId = mmsCursor.getLong(COLUMN_MMS_ID);
					long threadId = mmsCursor.getLong(COLUMN_THREAD_ID);
					long timeMillis = mmsCursor.getLong(COLUMN_DATE) * 1000;
					
					String recipId = recipIdMapping.get(threadId);
					if (recipId == null) {
						recipId = getRecipIds(threadId, context);
						recipIdMapping.put(threadId, recipId);
					}
					popUpInfoAcc.add(new PopUpInfo(threadId, msgId, false, timeMillis, recipId));
				}
			}   


		} finally  {
			if (smsCursor != null) {
				smsCursor.close();   
			}

			if (mmsCursor != null) {
				mmsCursor.close();
			}
		}

		return popUpInfoAcc;	   
	}
    
    /*
     * This method is used to fetch the recipientIds which will be used by the PopUpNotification 
     * since getting the contacts from threadid every time requires to make some db calls
     */
    private static String getRecipIds(long threadId, Context context) {
		String recipId = null;
		
		Cursor cursor = SqliteWrapper.query(context, Conversation.sAllThreadsUri, RECIP_ID_PROJECTION, Threads._ID + "=" + threadId, null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				recipId = cursor.getString(0); 
			}
		}
		
		return recipId;
	}

	private static final MmsSmsNotificationInfo getSmsNewMessageNotificationInfo(Context context,
            Map<Long, List<Uri>> threads) {
        ContentResolver resolver = context.getContentResolver();
        final Uri smsUri = VZUris.getSmsUri();
        Cursor cursor = SqliteWrapper.query(context, resolver, smsUri, SMS_STATUS_PROJECTION,
                NEW_INCOMING_SM_CONSTRAINT, null, Sms.DATE + " desc");

        if (cursor == null) {
            return null;
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }

            String address = cursor.getString(COLUMN_SMS_ADDRESS);
            String body = cursor.getString(COLUMN_SMS_BODY);
            long threadId = cursor.getLong(COLUMN_THREAD_ID);
            long timeMillis = cursor.getLong(COLUMN_DATE);
            long msgId = cursor.getLong(COLUMN_SMS_MSG_ID);

            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(MessagingNotification.class, "getSmsNewMessageNotificationInfo: count=" + cursor.getCount() + ", first addr="
                        + address + ", thread_id=" + threadId + ",msgId=" + msgId);
            }

            MmsSmsNotificationInfo info = getNewMessageNotificationInfo(address, body, context,
                    R.drawable.stat_notify_sms, null, threadId, timeMillis, cursor.getCount());

            // add thread(s) and their message uri(s) to the map
            boolean fetch = false;
            do {
            	if (fetch) {
                    threadId = cursor.getLong(COLUMN_THREAD_ID);
                    msgId = cursor.getLong(COLUMN_SMS_MSG_ID);
            	}
                final Uri msgUri = ContentUris.withAppendedId(smsUri, msgId);
                List<Uri> msgUris = threads.get(threadId);
                if (msgUris == null) {
                	msgUris = new ArrayList<Uri>();
                	threads.put(threadId, msgUris);
                }
                msgUris.add(msgUri);
            } while (fetch = cursor.moveToNext());

            return info;
        } finally {
        	if (cursor != null) {
        		cursor.close();
        	}
        }
    }
  
    private static final MmsSmsNotificationInfo getNewMessageNotificationInfo(String address, String body,
            Context context, int iconResourceId, String subject, long threadId, long timeMillis, int count) {
        Intent clickIntent = ConversationListActivity.getNotificationIntentFromParent(context, threadId, false);
        int flag = clickIntent.getFlags();
        clickIntent.setFlags(flag | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        clickIntent.putExtra(ComposeMessageFragment.INTENT_FROM_NOTIFICATION, true);

        //bug 2881:need to show multiple recipient count if it is group sms/mms in notification area.
        String senderInfoNames=getSenderInfoNames(context,threadId,address);
        CharSequence ticker = buildTickerMessage(context, address, subject, body);
        return new MmsSmsNotificationInfo(clickIntent, body, iconResourceId, ticker, timeMillis,
        		senderInfoNames, count);
    }
    
    
	private static String getSenderInfoNames(Context context,long threadId,String address) {
		Cursor c = null;
		final String where = Threads._ID + "=?";
		final String[] args = { Long.toString(threadId) };
		String recipientIds=null;
		StringBuilder sb=new StringBuilder();
		Uri sAllThreadsUri = VZUris.getMmsSmsConversationUri().buildUpon()
				.appendQueryParameter("simple", "true").build();
		String[] ALL_THREADS_PROJECTION = {Threads.RECIPIENT_IDS};
		try{
			 String senderInfo = buildTickerMessage(context, address, null, null).toString();
			 sb.append(senderInfo.substring(0, senderInfo.length() - 2));
			 c = SqliteWrapper.query(context, context.getContentResolver(), sAllThreadsUri, ALL_THREADS_PROJECTION, where, args,
					null);       
				 if (c != null) {
					if (c.moveToFirst()) {
						 recipientIds = c.getString(0);
					}
					}
			
				 if(0<(RecipientIdCache.getAddresses(recipientIds).size()-1)){
					sb.append(" ").append("+").append(Integer.toString(RecipientIdCache.getAddresses(recipientIds).size()-1));
				 }
		}catch(Exception e){
			Logger.error(e);
		}finally{
			 if (c != null) {
				 c.close();
			 } 
		}
		Logger.debug("senderInfoNames::"+sb.toString());
		return sb.toString();
	}
	
    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        nm.cancel(notificationId);
    }
    
    public static void clearAllNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        nm.cancel(NOTIFICATION_ID);
        nm.cancel(MESSAGE_FAILED_NOTIFICATION_ID);
        nm.cancel(DOWNLOAD_FAILED_NOTIFICATION_ID);
    }

    private static void updateDeliveryNotification(final Context context, boolean isStatusMessage,
            final CharSequence message, final long timeMillis) {
        if (!isStatusMessage) {
            return;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (!sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true)) {
            return;
        }
        //now the toast will not be on the UI thread. We have a new thread to show the toast messages.
        mToastHandler.queueToast(context,message,timeMillis);

    }

    private static void updateNotification(Context context, Intent clickIntent, String description,
            int iconRes, boolean isNew, CharSequence ticker, long timeMillis, String title, int messageCount,
            int uniqueThreadCount) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("In updateNotification: isNew:" + isNew + " uniqueThreadCount:" + uniqueThreadCount + " messageCount:"
            		+ messageCount);
        }
        if (!sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true)) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("In updateNotification but not enabled");
            }
            return;
        }

        Notification notification = new Notification(iconRes, ticker, timeMillis);
        
        
        // If we have more than one unique thread, change the title (which would
        // normally be the contact who sent the message) to a generic one that
        // makes sense for multiple senders, and change the Intent to take the
        // user to the conversation list instead of the specific thread.
        if (uniqueThreadCount > 1) {
            title = context.getString(R.string.notification_multiple_title);
            clickIntent = new Intent(context, ConversationListActivity.class);

            clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            clickIntent.putExtra(ConversationListActivity.EXTRA_FROM_NOTIFICATION, true);
            //clickIntent.setType("vnd.android-dir/mms-sms");
        }

        // If there is more than one message, change the description (which
        // would normally be a snippet of the individual message text) to
        // a string indicating how many "unseen" messages there are.
        if (messageCount > 1) {
            description = context.getString(R.string.notification_multiple, Integer.toString(messageCount));
            
            notification.number = messageCount;
        }

        PendingIntent pendingIntent = null;
        
        if(!ApplicationSettings.getInstance().getBooleanSetting(AppSettings.KEY_VMA_ACCEPT_TERMS, false)) {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(MessagingNotification.class, "update Notification - T&C false");
        	}
        	Intent i = new Intent(context, ConversationListActivity.class);
        	i.putExtra(ConversationListActivity.EXTRA_FROM_NOTIFICATION, true);
        	
        	pendingIntent = PendingIntent.getActivity(context, 0, i,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(MessagingNotification.class, "update Notification - T&C true");
        	}
        	// Make a startActivity() PendingIntent for the notification.
        	pendingIntent = PendingIntent.getActivity(context, 0, clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
        

        // Update the notification.
        notification.setLatestEventInfo(context, title, description, pendingIntent);
        boolean disableIfOnCall = true;
        boolean onCall = false;
        
        if (isNew) {
            disableIfOnCall = sp.getBoolean(
                    AdvancePreferenceActivity.NOTIFICATION_DISABLE_DURING_PHONE_CALL, true);
            onCall = Util.isOnPhoneCall(context);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("In updateNotification - isNew; onCall: " + onCall + " disableIfOnCall: " + disableIfOnCall);
            }
            
            /*
             * The function isOnPhoneCall() will give false for EHRPD or LTE even if we are in a call
             * and we actually need to block sound/vibrate if we're on a call and disableIfOnCall is true.
             */
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            int state = tm.getCallState();
            if ( state == TelephonyManager.CALL_STATE_OFFHOOK || state == TelephonyManager.CALL_STATE_RINGING )
            {
                onCall = true;
            }
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("In updateNotification - onCall again: " + onCall + " disableIfOnCall: " + disableIfOnCall);
            }
            
            

//            if ((!onCall || !disableIfOnCall) || (onCall && !disableIfOnCall)) {
//
//                String vibrateWhen;
//                if (sp.contains(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN)) {
//                    vibrateWhen = sp.getString(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN, null);
//                } else {
//                    vibrateWhen = context.getString(R.string.prefDefault_vibrateWhen);
//                }
//
//                boolean vibrateAlways = vibrateWhen.equals("always");
//                boolean vibrateSilent = vibrateWhen.equals("silent");
//                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
//                int ringerMode = audioManager.getRingerMode();
//                boolean nowSilent = ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_SILENT;
//
//                if (Logger.IS_DEBUG_ENABLED) {
//                    Logger.debug("In updateNotification - vibrateAlways: " + vibrateAlways + " vibrateSilent: " + vibrateSilent 
//                    		+ " nowSilent: " + nowSilent);
//                }
//                if (vibrateAlways || (vibrateSilent && nowSilent)) {
//                    notification.defaults |= Notification.DEFAULT_VIBRATE;
//                  
//                    if (onCall || (ringerMode == AudioManager.RINGER_MODE_SILENT)) {
//                    	long[] vibrate_pattern = {0, 1000};
//                    	Vibrator vibe = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
//                    	vibe.vibrate(vibrate_pattern, -1);
//                    }
//                }
//            }
            
//            boolean onCallStatus = onCall && disableIfOnCall;
//            String ringtoneStr = sp.getString(MessagingPreferenceActivity.NOTIFICATION_RINGTONE, null);
//            if (Logger.IS_DEBUG_ENABLED) {
//                Logger.debug("In updateNotification - ringtoneStr: " + ringtoneStr + " onCallStatus: " + onCallStatus); 
//            }
//            if( !onCallStatus ) {
//                notification.sound = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
//            }
            
            
            boolean onCallStatus = onCall && disableIfOnCall;

            String vibrateWhen="";
            if (sp.contains(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN)) {
                vibrateWhen = sp.getString(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN, null);
            } else {
                vibrateWhen = context.getString(R.string.prefDefault_vibrateWhen);
            }


            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int ringerMode = audioManager.getRingerMode();

            
            int osVibrateSetting =  audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
            boolean osVibrateAlways=  (osVibrateSetting == AudioManager.VIBRATE_SETTING_ON);
            boolean osVibrateOnSilent=  (osVibrateSetting == AudioManager.VIBRATE_SETTING_ONLY_SILENT);
            boolean appVibrateAlways=  vibrateWhen.equals("always");
            boolean appVibrateOnSilent=  vibrateWhen.equals("silent");

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Notification Setting: Vibrate() : osVibrateSetting=" + osVibrateSetting + ",osVibrateAlways="
                        + osVibrateAlways + " ,osVibrateOnSilent=" + osVibrateOnSilent + ",appVibrateAlways="
                        + appVibrateAlways + ",appVibrateOnSilent=" + appVibrateOnSilent+"");
            }
            
            
            
            
            boolean silentMode = (ringerMode == AudioManager.RINGER_MODE_SILENT ||  
                    ringerMode == AudioManager.RINGER_MODE_VIBRATE);

            // Vibrate/silent/on call. we have to disable the sound
            boolean enableSound = (onCallStatus) ? false : ((silentMode)?false:true);

            
            // always or silent
            // The below flag are not need ,  this is temporary fix to override the platform or app settings
            boolean alwaysVibrate= (osVibrateAlways==appVibrateAlways)?osVibrateAlways:appVibrateAlways;
            boolean vibrateOnSilent=(osVibrateOnSilent==appVibrateOnSilent)?appVibrateOnSilent:appVibrateOnSilent;
            
            boolean vibrate =(onCallStatus) ?false:(appVibrateAlways==appVibrateOnSilent) ? false:(appVibrateAlways || (silentMode && vibrateOnSilent));
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Notification Setting: Silent mode=" + silentMode + " onCallStatus="
                        + onCallStatus + " enableSound=" + enableSound + " ringerMode=" + ringerMode
                        + " alwaysVibrate=" + alwaysVibrate + " vibrateOnSilent=" + vibrateOnSilent +" vibrate="+vibrate);

            }
            
                                                            
            if (vibrate) {
                long[] vibrate_pattern = { 0, 1000 };
                Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                vibe.vibrate(vibrate_pattern, -1);
            }
            
            if (enableSound) {
                String ringtoneStr = sp.getString(MessagingPreferenceActivity.NOTIFICATION_RINGTONE, null);
                notification.sound = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
            } else {
                notification.sound = null;
            }
        }
        

        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.ledARGB =  getNotificationColour(sp);
        notification.ledOnMS = 500;
        notification.ledOffMS = 2000;

        // set up delete intent
        notification.deleteIntent = PendingIntent.getBroadcast(context, 0, sNotificationOnDeleteIntent, 0);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        nm.notify(NOTIFICATION_ID, notification);
        
        // if we dont explicitly play a tone then sounds within calls are disabled
        if (onCall && !disableIfOnCall) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("In updateNotification - explicitly generating tone as on call"); 
            }

        	ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        	tg.startTone(ToneGenerator.TONE_PROP_BEEP2); 
        	try {
        		// beep for 200 msec
        		Thread.sleep(300);
        	} catch (InterruptedException e) {
        		if (Logger.IS_DEBUG_ENABLED) {
        			Logger.debug(MessagingNotification.class, "Trying to play tone and got interrupted");
        		}
        	}
        	tg.stopTone();
        }
    }
/**
* Returns the colour of the notification to be used
*/

    public static int getNotificationColour( SharedPreferences sp){

        String ledPrefence = sp.getString(MessagingPreferenceActivity.NOTIFICATION_LED_COLOUR, null);
        if (null == ledPrefence) {
            return  MessagingNotification.GREEN_LED;
        } else {
            switch (Integer.parseInt(ledPrefence)) {
            case 0:
                return GREEN_LED;
            case 1:
                return RED_LED;
            case 2:
                return ORANGE_LED;
            case 3:
                return YELLOW_LED;
            case 4:
                return BLUE_LED;
            case 5:
                return INDIGO_LED;
            default:
                return GREEN_LED;
            }

        }
    }

    protected static CharSequence buildTickerMessage(Context context, String address, String subject,
            String body) {
        String displayAddress = Contact.get(address, true).getName();

        StringBuilder buf = new StringBuilder(displayAddress == null ? "" : displayAddress.replace('\n', ' ')
                .replace('\r', ' '));
        buf.append(':').append(' ');

        int offset = buf.length();
        if (!TextUtils.isEmpty(subject)) {
            subject = subject.replace('\n', ' ').replace('\r', ' ');
            buf.append(subject);
            buf.append(' ');
        }

        if (!TextUtils.isEmpty(body)) {
            body = body.replace('\n', ' ').replace('\r', ' ');
            buf.append(body);
        }

        SpannableString spanText = new SpannableString(buf.toString());
        spanText.setSpan(new StyleSpan(Typeface.BOLD), 0, offset, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spanText;       
    }

    private static String getMmsSubject(String sub, int charset) {
        return TextUtils.isEmpty(sub) ? "" : new EncodedStringValue(charset, PduPersister.getBytes(sub))
                .getString();
    }

    public static void notifyDownloadFailed(Context context, long threadId) {
        notifyFailed(context, true, threadId, false);
    }

    public static void notifySendFailed(Context context) {
        notifyFailed(context, false, 0, false);
    }

    public static void notifySendFailed(Context context, boolean noisy) {
        notifyFailed(context, false, 0, noisy);
    }

    private static void notifyFailed(Context context, boolean isDownload, long threadId, boolean noisy) {
        // TODO factor out common code for creating notifications
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        boolean enabled = sp.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true);
        if (!enabled) {
            return;
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Strategy:
        // a. If there is a single failure notification, tapping on the notification goes
        // to the compose view.
        // b. If there are two failure it stays in the thread view. Selecting one undelivered
        // thread will dismiss one undelivered notification but will still display the
        // notification.If you select the 2nd undelivered one it will dismiss the notification.

        long[] msgThreadId = { 0 };
        int totalFailedCount = getUndeliveredMessageCount(context, msgThreadId);

        Intent failedIntent;
        Notification notification = new Notification();
        String title;
        String description;
        if (totalFailedCount > 1) {
            description = context.getString(R.string.notification_failed_multiple,
                    Integer.toString(totalFailedCount));
            title = context.getString(R.string.notification_failed_multiple_title);

            failedIntent = new Intent(context, ConversationListActivity.class);
        } else {
            title = isDownload ? context.getString(R.string.message_download_failed_title) : context
                    .getString(R.string.message_send_failed_title);

            description = context.getString(R.string.message_failed_body);
            failedIntent = ConversationListActivity.getNotificationIntentFromParent(context, 0, false);
            if (isDownload) {
                // When isDownload is true, the valid threadId is passed into this function.
                failedIntent.putExtra("failed_download_flag", true);
            } else {
                threadId = (msgThreadId[0] != 0 ? msgThreadId[0] : 0);
                failedIntent.putExtra("undelivered_flag", true);
            }
            failedIntent.putExtra("thread_id", threadId);
        }

        failedIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        failedIntent.putExtra(ComposeMessageFragment.INTENT_FROM_NOTIFICATION, true);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, failedIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        notification.icon = R.drawable.stat_notify_sms_failed;

        notification.tickerText = title;
        //Auto cancel for notification Bug 1190
        notification.flags |= Notification.FLAG_AUTO_CANCEL; 
        CharSequence desc=EmojiParser.getInstance().addEmojiSpans(description, false);
        notification.setLatestEventInfo(context, title, desc, pendingIntent);

        if (noisy) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
            String ringtoneStr = sp.getString(MessagingPreferenceActivity.NOTIFICATION_RINGTONE, null);
            notification.sound = TextUtils.isEmpty(ringtoneStr) ? null : Uri.parse(ringtoneStr);
        }

        if (isDownload) {
            nm.notify(DOWNLOAD_FAILED_NOTIFICATION_ID, notification);
        } else {
            nm.notify(MESSAGE_FAILED_NOTIFICATION_ID, notification);
        }
    }

    // threadIdResult[0] contains the thread id of the first message.
    // threadIdResult[1] is nonzero if the thread ids of all the messages are the same.
    // You can pass in null for threadIdResult.
    // You can pass in a threadIdResult of size 1 to avoid the comparison of each thread id.
    private static int getUndeliveredMessageCount(Context context, long[] threadIdResult) {
        Cursor undeliveredCursor = SqliteWrapper.query(context, context.getContentResolver(),
                VZUris.getUndelivered(), new String[] { Mms.THREAD_ID }, "read=0", null, null);
        if (undeliveredCursor == null) {
            return 0;
        }
        
        int count = 0;
        try {
            //get valid threads from the cursor
            ArrayList<Long> threads = new ArrayList<Long>();
            if (undeliveredCursor.moveToFirst()) {
                do {
                    long threadId = undeliveredCursor.getLong(0);
                    
                    if (threadId > 0 && threadId < Long.MAX_VALUE) {
                        threads.add(threadId);
                    }
                } while (undeliveredCursor.moveToNext());
            }
         
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(MessagingNotification.class, "Failed Unread thread Ids " + threads);
            }
            
            count = threads.size();
            if (threadIdResult != null && count > 0) {
                threadIdResult[0] = threads.get(0);

                if (threadIdResult.length >= 2) {
                    // Test to see if all the undelivered messages belong to the same thread.
                    long firstId = threadIdResult[0];
                    for (long thread : threads) {
                        if (thread != firstId) {
                            firstId = 0;
                            break;
                        }
                    }
                    threadIdResult[1] = firstId; // non-zero if all ids are the same
                }
            }
        } finally {
            undeliveredCursor.close();
        }
        return count;
    }

    public static void updateSendFailedNotification(Context context, boolean isRecreateNotification) {
        if (getUndeliveredMessageCount(context, null) < 1) {
            cancelNotification(context, MESSAGE_FAILED_NOTIFICATION_ID);
        }// Dont recreate the failure Notification as request by Sanjeev
        else {
        	if(isRecreateNotification)
        		notifySendFailed(context); // rebuild and adjust the message count if necessary.
        }
    }

    /**
     * If all the undelivered messages belong to "threadId", cancel the notification.
     */
    public static void updateSendFailedNotificationForThread(Context context, long threadId) {
        long[] msgThreadId = { 0, 0 };
        if (getUndeliveredMessageCount(context, msgThreadId) > 0 && msgThreadId[0] == threadId
                && msgThreadId[1] != 0) {
            cancelNotification(context, MESSAGE_FAILED_NOTIFICATION_ID);
        }
    }

    private static int getDownloadFailedMessageCount(Context context) {
        // Look for any messages in the MMS Inbox that are of the type
        // NOTIFICATION_IND (i.e. not already downloaded) and in the
        // permanent failure state. If there are none, cancel any
        // failed download notification.
        Cursor c = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getMmsInboxUri(), null,
                Mms.MESSAGE_TYPE + "=" + String.valueOf(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) + " AND "
                        + Mms.STATUS + "=" + String.valueOf(DownloadManager.STATE_PERMANENT_FAILURE), null,
                null);
        if (c == null) {
            return 0;
        }
        int count = c.getCount();
        c.close();
        return count;
    }

    public static void updateDownloadFailedNotification(Context context) {
        if (getDownloadFailedMessageCount(context) < 1) {
            cancelNotification(context, DOWNLOAD_FAILED_NOTIFICATION_ID);
        }
    }

    public static boolean isFailedToDeliver(Intent intent) {
        return (intent != null) && intent.getBooleanExtra("undelivered_flag", false);
    }

    public static boolean isFailedToDownload(Intent intent) {
        return (intent != null) && intent.getBooleanExtra("failed_download_flag", false);
    }
   
	// Bugid 1324 Toast message for MMS
	/*
	 * Get the delivery report address from the PDU . 
	 * Do a extra check to see if the message is delivered, for the address got from PDU .
	 * getMmsRecipients == getMmsReportStatus  : show the MMS toast
	 */
	static public void showMMSDeliveryStatus(final Context context, Uri uri, GenericPdu pdu) {
		// get the recipients from the pdu
		final List<String> addrs = MessageItem.getMmsRecipients(pdu, false);

		// get delivery reports for the message
		final String msgId = Long.toString(ContentUris.parseId(uri));
		final HashMap<String, ReportStatus> stat = MessageItem.getMmsReportStatus(context, msgId);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MessagingNotification.class, "showMMSDeliveryStatus: recipients = " + addrs + ", status = " + stat);
		}

		// show a notification for any recipients that are delivered
		for (String addr : addrs) {
			final ReportStatus report = stat.get(addr);
			if (Logger.IS_DEBUG_ENABLED) {
				if(report != null)
				Logger.debug(MessagingNotification.class, "showMMSDeliveryStatus of : ReportStatus = " + addrs + ", status = " + report);
			}
			if (report != null && report.delivered && !report.read && !report.deleted ) {
				final String msg = context.getString(R.string.delivery_toast_body,
					Contact.get(addr, true, false).getName());
				new MmsSmsDeliveryInfo(msg, NOTIFICATION_DURATION).deliver(context, true);
			} else if (report != null && report.delivered && report.read && !report.deleted) {
				final String msg = context.getString(R.string.read_toast_body,
						Contact.get(addr, true, false).getName());
					new MmsSmsDeliveryInfo(msg, NOTIFICATION_DURATION).deliver(context, true);
			} else if (report != null && report.delivered && report.deleted) {
				 final String msg = context.getString(R.string.deleted_toast_body,
							Contact.get(addr, true, false).getName());
						new MmsSmsDeliveryInfo(msg, NOTIFICATION_DURATION).deliver(context, true);
			}
		}
	}

	public static void addListener(NotificationListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public static void removeListener(NotificationListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}
}
