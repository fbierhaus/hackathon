package com.verizon.mms.data;

import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF;
import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_SEND_REQ;
import static com.verizon.mms.pdu.PduHeaders.MESSAGE_TYPE_SEND_REQ_X;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_ID;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_DATE;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_MESSAGE_TYPE;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_TRANSACTION_ID;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MSG_TYPE;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_SMS_DATE;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_SMS_TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Mms.Addr;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.provider.Telephony.Threads;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.DeviceConfig.OEM;
import com.verizon.mms.MediaSyncHelper;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.transaction.MessagingNotification;
import com.verizon.mms.ui.MMSReadReport;
import com.verizon.mms.ui.MessageItem;
import com.verizon.mms.ui.MessageItem.DeliveryStatus;
import com.verizon.mms.ui.MessageItem.GroupMode;
import com.verizon.mms.ui.MessageListAdapter;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.util.DraftCache;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.VZTelephony;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.SyncController;
import com.verizon.sync.SyncManager;
import com.verizon.sync.VMASyncHook;

/**
 * An interface for finding information about conversations and/or creating new ones.
 */
public class Conversation {
    private static String notDownloaded;

    public static final Uri sAllThreadsUri = VZUris.getMmsSmsConversationUri().buildUpon()
            .appendQueryParameter("simple", "true").build();

    // filter out temporary threads created for drafts etc and threads with id -1.
	private static final String ALL_THREADS_WHERE = Threads.RECIPIENT_IDS + " != '' and " + Threads._ID + " != -1";

    private static final String[] ALL_THREADS_PROJECTION = { Threads._ID, Threads.DATE,
            Threads.MESSAGE_COUNT, Threads.RECIPIENT_IDS, Threads.SNIPPET, Threads.SNIPPET_CHARSET,
            Threads.READ, Threads.ERROR };

    public static final int ID = 0;
    public static final int DATE = 1;
    public static final int MESSAGE_COUNT = 2;
    public static final int RECIPIENT_IDS = 3;
    public static final int SNIPPET = 4;
    public static final int SNIPPET_CS = 5;
    public static final int READ = 6;
    public static final int ERROR = 7;

    private static final String[] COUNT_PROJECTION = { "COUNT(*) AS count" };

    private static final String[] UNREAD_PROJECTION = { Threads._ID, Threads.READ };

    private static final String UNREAD_SELECTION = "(read=0 OR seen=0)";
      
    private static final String[] SEEN_PROJECTION = new String[] { "seen" };
    
    private static final String[] SEEN_MSG_PROJECTION = new String[] { "seen" , "_id"};
    
    private static final String UNREAD_MMS_SELECTION = "(" + Mms.MESSAGE_TYPE + "="
            + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF + " OR " + Mms.MESSAGE_TYPE + "="
            + PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND + ") AND " + Mms.READ + "=0 AND " + Mms.THREAD_ID
            + "=?";

    private static final String UNREAD_MMS_SELECTION_WIDGET = "(" + Mms.MESSAGE_TYPE + "="
            + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF + " OR " + Mms.MESSAGE_TYPE + "="
            + PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND + ") AND " + Mms.READ + "=0";

    private static final String OUTGOING_SMS_SELECTION = Sms.THREAD_ID + " = ? AND (" + Sms.TYPE + "="
            + Sms.MESSAGE_TYPE_SENT + " OR " + Sms.TYPE + "=" + Sms.MESSAGE_TYPE_QUEUED + " OR " + Sms.TYPE
            + "=" + Sms.MESSAGE_TYPE_OUTBOX + " OR " + Sms.TYPE + "=" + Sms.MESSAGE_TYPE_FAILED + ")";
    private static final String OUTGOING_MMS_SELECTION = Mms.THREAD_ID + " = ? AND " + Mms.MESSAGE_TYPE + "="
            + MESSAGE_TYPE_SEND_REQ;
	private static final String UNREAD_SMS_QUERY_SELECTION = "read=0 and thread_id != -1 and thread_id in (select _id from threads)";
	
	private static final String UNREAD_MMS_QUERY_SELECTION =  "("
			+ Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF + " OR "
			+ Mms.MESSAGE_TYPE + "=" + PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND + ") AND "
			+ Mms.READ + "=0 and thread_id != -1 and thread_id in (select _id from threads)";
	
    private final Context mContext;

    // The thread ID of this conversation. Can be zero in the case of a
    // new conversation where the recipient set is changing as the user
    // types and we have not hit the database yet to create a thread.
    private long mThreadId;
    // The previous threadId used to trigger onThreadChanged when ever a conversations threadId is changed
    private long mLastThreadId;

    private ContactList mRecipients; // The current set of recipients.
    private long mDate; // The last update time.
    private int mMessageCount; // Number of messages.
    private String mSnippet; // Text of the most recent message.
    private boolean mHasUnreadMessages; // True if there are unread messages.
    private boolean mHasError; // True if any message is in an error state.
    private static ContentValues mReadContentValues;
    private boolean mMarkAsReadBlocked;
    private Object mMarkAsBlockedSyncer = new Object();
    private HashMap<Long, Integer> mRecipientsMap;
    private ContentResolver mResolver;

    private List<ThreadChangeListener> mThreadChangeListener = null;
    private static boolean isRecreateNotification = false; // dont recreate the failure Notification

    private static boolean useNewUnreadQuery = true;

    public static class MessageData {
        public CharSequence subject;
        public int attType;
        public boolean isQueued;

		public MessageData(CharSequence subject, int attType, DeliveryStatus msgStatus) {
			this.subject = subject;
			this.attType = attType;
			this.isQueued = msgStatus == DeliveryStatus.SENDING ? true : false;
		}
	}
 private static final int MAX_UNREAD_CHECK = 5; // max number of incoming message to check for an unread message
 private static final int MAX_INCOMING_XIDS = 5; // max number of incoming msgs to collect data for
    private static final int MAX_INCOMING_MSGS = 100; // max number of incoming msgs to scan


    public static class IncomingMessageData {
        public int pos; // position of most recently received message
        public long lastId; // id of most recently received message
        public long lastMmsId; // id of most recently received MMS message
        public long lastSmsId; // id of most recently received SMS message
        public boolean lastIsMms; // type of most recently received message
        public Set<String> receivedXids; // xids of recently received messages

        private IncomingMessageData(int pos, long lastId, long lastMmsId, long lastSmsId,
        		boolean lastIsMms, Set<String> receivedXids) {
            this.pos = pos;
            this.lastId = lastId;
            this.lastMmsId = lastMmsId;
            this.lastSmsId = lastSmsId;
            this.lastIsMms = lastIsMms;
            this.receivedXids = receivedXids;
        }

        @Override
        public String toString() {
            return super.toString() + ": pos = " + pos + ", lastId = " + lastId + ", mmsId = " + lastMmsId +
            	", smsId = " + lastSmsId + ", mms = " + lastIsMms + ", xids = " + receivedXids;
        }
    }

    public static class OutgoingMessageData {
        public int pos;
        public GroupMode groupMode;
        public long time;

		private OutgoingMessageData(int pos, GroupMode groupMode, long time) {
			this.pos = pos;
			this.groupMode = groupMode;
			this.time = time;
		}
    }

    public interface ThreadChangeListener {
        public void onThreadChanged(long oldThreadId, long newThreadId);
    }

    private Conversation(Context context) {
        mContext = context;
        init();
        mRecipients = new ContactList();
        mLastThreadId = mThreadId = 0;
    }

    public Conversation(Context context, long threadId, boolean allowQuery) {
        mContext = context;
        init();
        if (!loadFromThreadId(threadId, allowQuery)) {
            mRecipients = new ContactList();
            mLastThreadId = mThreadId = 0;
        }
    }

    private Conversation(Context context, Cursor cursor, boolean allowQuery) {
        mContext = context;
        init();
        fillFromCursor(context, this, cursor, allowQuery);
    }

    public void registerThreadChangeListener(ThreadChangeListener listener) {
        if (!mThreadChangeListener.contains(listener)) {
            mThreadChangeListener.add(listener);
        }
    }

    public void unregisterThreadChangeListener(ThreadChangeListener listener) {
        mThreadChangeListener.remove(listener);
    }

    private void init() {
        mRecipientsMap = new HashMap<Long, Integer>();
        mResolver = mContext.getContentResolver();
        mThreadChangeListener = new ArrayList<ThreadChangeListener>();
    }

    /**
     * Create a new conversation with no recipients. {@link #setRecipients} can be called as many times as you
     * like; the conversation will not be created in the database until {@link #ensureThreadId} is called.
     */
    public static Conversation createNew(Context context) {
        return new Conversation(context);
    }

    /**
     * Find the conversation matching the provided thread ID.
     */
    public static Conversation get(Context context, long threadId, boolean allowQuery) {
        Conversation conv = Cache.get(threadId);
        if (conv != null)
            return conv;

        conv = new Conversation(context, threadId, allowQuery);
        try {
            Cache.put(conv);
        } catch (IllegalStateException e) {
            Logger.error("Tried to add duplicate Conversation to Cache");
        }
        return conv;
    }

    /**
     * Find the conversation matching the provided recipient set. When called with an empty recipient list,
     * equivalent to {@link #createNew}.
     */
    public static Conversation get(Context context, ContactList recipients, boolean allowQuery) {
        // If there are no recipients in the list, make a new conversation.
        if (recipients.size() < 1) {
            return createNew(context);
        }

        Conversation conv = Cache.get(recipients);
        if (conv != null)
            return conv;

        long threadId = getOrCreateThreadId(context, recipients);
        conv = new Conversation(context, threadId, allowQuery);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Conversation.get: created new conversation " + conv);
        }
        /*
         * Bug_id371 and 451 conversation failed to load with the threadid then give the recipients to be
         * loaded as from the intent fetched.
         */
        if (DeviceConfig.OEM.isHTC && recipients != null & !recipients.isEmpty()) {
            if (conv.getThreadId() != threadId) {
                conv.setRecipients(recipients);
            }
        }

        if (!conv.getRecipients().equals(recipients)) {
            Logger.error(Conversation.class,
                    "Conversation.get: new conv's recipients don't match input recpients " + recipients);
        }

        try {
            Cache.put(conv);
        } catch (IllegalStateException e) {
            Logger.error("Tried to add duplicate Conversation to Cache");
        }

        return conv;
    }

    /**
     * Find the conversation matching in the specified Uri. Example forms: {@value
     * content://mms-sms/conversations/3} or {@value sms:+12124797990}. When called with a null Uri,
     * equivalent to {@link #createNew}.
     */
    public static Conversation get(Context context, Uri uri, boolean allowQuery) {
        if (uri == null) {
            return createNew(context);
        }

        if (Logger.IS_DEBUG_ENABLED)
            Logger.debug(Conversation.class, "Conversation get URI: " + uri);

        // Handle a conversation URI
        if (uri.getPathSegments().size() >= 2) {
            try {
                long threadId = Long.parseLong(uri.getPathSegments().get(1));
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(Conversation.class, "Conversation get threadId: " + threadId);
                }
                return get(context, threadId, allowQuery);
            } catch (NumberFormatException exception) {
                Logger.error("Invalid URI: " + uri);
            }
        }

        String recipient = uri.getSchemeSpecificPart();
        return get(context, ContactList.getByNumbers(recipient, allowQuery, true), allowQuery);
    }

    /**
     * Returns true if the recipient in the uri matches the recipient list in this conversation.
     */
    public boolean sameRecipient(Uri uri) {
        synchronized (this) {
            int size = mRecipients.size();
            if (size > 1) {
                return false;
            }
            if (uri == null) {
                return size == 0;
            }
        }
        if (uri.getPathSegments().size() >= 2) {
            return false; // it's a thread id for a conversation
        }
        String recipient = uri.getSchemeSpecificPart();
        ContactList incomingRecipient = ContactList
                .getByNumbers(recipient, false /* don't block */, false /* don't replace number */);
        synchronized (this) {
            return mRecipients.equals(incomingRecipient);
        }
    }

    /**
     * Returns a temporary Conversation (not representing one on disk) wrapping the contents of the provided
     * cursor. The cursor should be the one returned to your AsyncQueryHandler passed in to
     * {@link #startQueryForAll}. The recipient list of this conversation can be empty if the results were not
     * in cache.
     */
    public static Conversation from(Context context, Cursor cursor) {
        // First look in the cache for the Conversation and return that one. That way, all the
        // people that are looking at the cached copy will get updated when fillFromCursor() is
        // called with this cursor.
        long threadId = cursor.getLong(ID);
        if (threadId > 0) {
            Conversation conv = Cache.get(threadId);
            if (conv != null) {
                fillFromCursor(context, conv, cursor, false); // update the existing conv in-place
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(Conversation.class, "from: returning cached conv: " + conv);
                }
                return conv;
            }
        }
        Conversation conv = new Conversation(context, cursor, false);
        try {
            Cache.put(conv);
        } catch (IllegalStateException e) {
            Logger.error("Tried to add duplicate Conversation to Cache");
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(Conversation.class, "from: returning new conv: " + conv);
        }
        return conv;
    }

    private static synchronized void buildReadContentValues() {
        if (mReadContentValues == null) {
            mReadContentValues = new ContentValues(2);
            mReadContentValues.put("read", 1);
            mReadContentValues.put("seen", 1);
        }
    }

   
    public static int markMsgAsRead(Context context, long threadId) {
    	Uri threadUri = VZUris.getMmsSmsConversationUri().buildUpon().appendPath(String.valueOf(threadId)).build();
    	boolean needUpdate = false;
    	int updateCount = 0;
    	if (threadUri != null) {
    	       
               ContentResolver resolver = context.getContentResolver();
    		   Cursor c = SqliteWrapper.query(context, resolver, threadUri, UNREAD_PROJECTION, UNREAD_SELECTION, null, null);
               if (c != null) {
                   try {
                       needUpdate = c.getCount() > 0;
                   } finally {
                       c.close();
                   }
               }
               if (needUpdate) {
            	   VMASyncHook.markVMAMessageAsRead(context, threadUri);
                   if (Logger.IS_DEBUG_ENABLED) {
                       Logger.debug("Converation.markMsgAsRead markAsRead: update read/seen for thread uri: "
                               + threadUri);
                   }
            	   buildReadContentValues();
                   SqliteWrapper.update(context, resolver, threadUri, mReadContentValues,
                           UNREAD_SELECTION, null);
            	   
                   //ConversationDataObserver.onMessageStatusChanged(threadId, -1, -1, ConversationDataObserver.MSG_SRC_TELEPHONY);
               }
       }
       return updateCount;
    }
    
    public void markAsRead() {
    	markAsRead(false);
    }
    
    /**
     * Marks all messages in this conversation as read and updates relevant notifications. This method returns
     * immediately; work is dispatched to a background thread.
     * if the addDelay variable is true the notification is reloaded after a gap of 2 secs which is required
     * in case we receive an mms and that conversation is open there are chaces that we might miss the notification 
     * all together since after persisting an mms we go thru few steps such as sending the sendNotifyRespInd status
     */
    public void markAsRead(final boolean addDelay) {
        // If we have no Uri to mark (as in the case of a conversation that
        // has not yet made its way to disk), there's nothing to do.
        final Uri threadUri = getUri();

        new Thread(new Runnable() {
            public void run() {
                synchronized (mMarkAsBlockedSyncer) {
                    if (mMarkAsReadBlocked) {
                        try {
                            mMarkAsBlockedSyncer.wait();
                        } catch (InterruptedException e) {
                        }
                    }

                    if (threadUri != null) {
                        // Check the read flag first. It's much faster to do a query than
                        // to do an update. Timing this function show it's about 10x faster to
                        // do the query compared to the update, even when there's nothing to
                        // update.
                        boolean needUpdate = true;

                        final Context context = mContext;
                        final ContentResolver resolver = mResolver;
                        Cursor c = SqliteWrapper.query(context, resolver, threadUri, UNREAD_PROJECTION,
                                UNREAD_SELECTION, null, null);
                        if (c != null) {
                            try {
                                needUpdate = c.getCount() > 0;
                            } finally {
                                c.close();
                            }
                        }

                        if (needUpdate) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(getClass(), "VMa-Hook: markAsRead: uri = " + threadUri);
                            }
                            VMASyncHook.markVMAMessageAsRead(context, threadUri);
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(getClass(), "markAsRead: update read/seen for thread uri: "
                                        + threadUri);
                            }
                            buildReadContentValues();
                            SqliteWrapper.update(context, resolver, threadUri, mReadContentValues,
                                    UNREAD_SELECTION, null);
                            
                            //ConversationDataObserver.onMessageStatusChanged(Long.valueOf(threadUri.getLastPathSegment()), -1, -1, ConversationDataObserver.MSG_SRC_TELEPHONY);
                        }

                        setHasUnreadMessages(false);
                    }
                }

            	if (addDelay) {
            		try {
						Thread.sleep(1500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
                
                // Always update notifications regardless of the read state.
                MessagingNotification.blockingUpdateAllNotifications(mContext, isRecreateNotification);
            }
        }).start();
    }

    public void blockMarkAsRead(boolean block) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("blockMarkAsRead: " + block);
        }

        synchronized (mMarkAsBlockedSyncer) {
            if (block != mMarkAsReadBlocked) {
                mMarkAsReadBlocked = block;
                if (!mMarkAsReadBlocked) {
                    mMarkAsBlockedSyncer.notifyAll();
                }
            }

        }
    }

    /**
     * Returns a content:// URI referring to this conversation, or null if it does not exist on disk yet.
     */
    public synchronized Uri getUri() {
        if (mThreadId <= 0)
            return null;

        return ContentUris.withAppendedId(VZUris.getMmsSmsConversationUri(), mThreadId);
    }

    /**
     * Return the Uri for all messages in the given thread ID.
     */
    public static Uri getUri(long threadId) {
        return ContentUris.withAppendedId(VZUris.getMmsSmsConversationUri(), threadId);
    }

    /**
     * Returns the thread ID of this conversation. Can be zero if {@link #ensureThreadId} has not been called
     * yet.
     */
    public synchronized long getThreadId() {
        return mThreadId;
    }

    /**
     * Guarantees that the conversation has been created in the database. This will make a blocking database
     * call if it hasn't.
     * 
     * @return The thread ID of this conversation in the database
     */
    public synchronized long ensureThreadId() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ": ensureThreadId before: " + mThreadId);
        }
        if (mThreadId <= 0) {
            long lastThreadId = mLastThreadId;
            mThreadId = getOrCreateThreadId(mContext, mRecipients);
            // update conversation state from thread
            loadFromThreadId(mThreadId, true);

            if (mThreadId != lastThreadId) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "ensureThreadId: lastThreadId: " + lastThreadId
                            + " current threadId " + mThreadId);
                }
                for (ThreadChangeListener listener : mThreadChangeListener) {
                    listener.onThreadChanged(lastThreadId, mThreadId);
                }
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "ensureThreadId after: " + mThreadId);
        }

        return mThreadId;
    }

    public synchronized void clearThreadId() {
        // remove ourself from the cache
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("clearThreadId old threadId was: " + mThreadId + " now zero");
        }
        Cache.remove(mThreadId);

        mThreadId = 0;
    }

    /**
     * Sets the list of recipients associated with this conversation. If called, {@link #ensureThreadId} must
     * be called before the next operation that depends on this conversation existing in the database (e.g.
     * storing a draft message to it).
     */
    public void setRecipients(ContactList list) {
        setRecipients(list, true);
    }

    private synchronized void setRecipients(ContactList list, boolean clearThreadId) {
        mRecipients = list;

        ContactList sortedList = new ContactList();
        for (Contact contact : list) {
            sortedList.add(contact);
        }

        // sort the contact list to ensure that the colors assigned for
        // the recipients are same in both Tablet and Handset
        Collections.sort(sortedList, Contact.getComparator());

        // map recipient numbers to their offset in the list
        // note that we assume that the list is in the canonical order and it shouldn't change
        final HashMap<Long, Integer> map = mRecipientsMap;
        map.clear();
        final int num = sortedList.size();
        for (int i = 0; i < num; ++i) {
            final Contact contact = sortedList.get(i);
            map.put(contact.getRecipientId(), i);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ", recipientsMap = " + map);
        }

        if (clearThreadId) {
            // Invalidate thread ID because the recipient set has changed.
            mThreadId = 0;
        }
    }

    /**
     * Returns the recipient set of this conversation.
     */
    public synchronized ContactList getRecipients() {
        return mRecipients;
    }

    /**
     * Returns the offset of the contact in the recipient list, based on the contact's canonical ID.
     * 
     * @return Offset of contact in the conversation's recipient list or -1 if not present
     */
    public synchronized int getRecipientNum(Long id) {
        final Integer offset = mRecipientsMap.get(id);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "getRecipientNum: id = " + id + ", offset = " + offset + ", map = "
                    + mRecipientsMap);
        }
        return offset != null ? offset.intValue() : -1;
    }

    /**
     * Returns true if a draft message exists in this conversation.
     */
    public synchronized boolean hasDraft() {
        if (mThreadId <= 0)
            return false;

        return DraftCache.getInstance().hasDraft(mThreadId);
    }

    /**
     * Sets whether or not this conversation has a draft message.
     */
    public synchronized void setDraftState(boolean hasDraft) {
        if (mThreadId <= 0)
            return;

        DraftCache.getInstance().setDraftState(mThreadId, hasDraft);
    }

    /**
     * Returns the time of the last update to this conversation in milliseconds, on the
     * {@link System#currentTimeMillis} timebase.
     */
    public synchronized long getDate() {
        return mDate;
    }

    /**
     * Returns the number of messages in this conversation, excluding the draft (if it exists).
     */
    public synchronized int getMessageCount() {
        return mMessageCount;
    }

    /**
     * Returns a snippet of text from the most recent message in the conversation.
     */
    public synchronized String getSnippet() {
        return mSnippet;
    }

    /**
     * Returns true if there are any unread messages in the conversation.
     */
    public boolean hasUnreadMessages() {
        synchronized (this) {
            return mHasUnreadMessages;
        }
    }

    private void setHasUnreadMessages(boolean flag) {
        synchronized (this) {
            mHasUnreadMessages = flag;
        }
    }

    /**
     * Returns true if any messages in the conversation are in an error state.
     */
    public synchronized boolean hasError() {
        return mHasError;
    }

    private static long getOrCreateThreadId(Context context, ContactList list) {
        HashSet<String> recipients = new HashSet<String>();
        Contact cacheContact = null;
        for (Contact c : list) {
            cacheContact = Contact.get(c.getNumber(), false);
            if (cacheContact != null) {
                recipients.add(cacheContact.getNumber());
            } else {
                recipients.add(c.getNumber());
            }
        }
        long retVal = VZTelephony.getOrCreateThreadId(context, recipients);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Conversation: getOrCreateThreadId for " + recipients + " returned " + retVal);
        }

        return retVal;
    }

    /*
     * The primary key of a conversation is its recipient set; override equals() and hashCode() to just pass
     * through to the internal recipient sets.
     */
    @Override
    public synchronized boolean equals(Object obj) {
        try {
            Conversation other = (Conversation) obj;
            return (mRecipients.equals(other.mRecipients));
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public synchronized int hashCode() {
        return mRecipients.hashCode();
    }

    @Override
    public synchronized String toString() {
        return super.toString() + ": tid " + mThreadId + ": recipients = " + mRecipients + ", map = "
                + mRecipientsMap;
    }

    /**
     * Remove any obsolete conversations sitting around on disk. Obsolete threads are threads that aren't
     * referenced by any message in the pdu or sms tables.
     */
    public static void asyncDeleteObsoleteThreads(AsyncQueryHandler handler, int token) {
        handler.startDelete(token, null, VZUris.getObsoleteThreadsUri(), null, null);
    }

    /**
     * Start a query for all conversations in the database on the specified AsyncQueryHandler.
     * 
     * @param handler
     *            An AsyncQueryHandler that will receive onQueryComplete upon completion of the query
     * @param token
     *            The token that will be passed to onQueryComplete
     */
    public static void startQueryForAll(AsyncQueryHandler handler, int token, Object cookie) {
        handler.cancelOperation(token);

        // This query looks like this in the log:
        // I/Database( 147): elapsedTime4Sql|/data/data/com.android.providers.telephony/databases/
        // mmssms.db|2.253 ms|SELECT _id, date, message_count, recipient_ids, snippet, snippet_cs,
        // read, error, has_attachment FROM threads ORDER BY date DESC

        handler.startQuery(token, cookie, sAllThreadsUri, ALL_THREADS_PROJECTION, ALL_THREADS_WHERE, null,
                Conversations.DEFAULT_SORT_ORDER);
    }

    /**
     * Start a delete of the conversation with the specified thread ID.
     * 
     * @param handler
     *            An AsyncQueryHandler that will receive onDeleteComplete upon completion of the conversation
     *            being deleted
     * @param token
     *            The token that will be passed to onDeleteComplete
     * @param deleteAll
     *            Delete the whole thread including locked messages
     * @param threadId
     *            Thread ID of the conversation to be deleted
     */
    public static void startDelete(Context context, AsyncQueryHandler handler, int token, boolean deleteAll,
            long threadId) {
        Cache.remove(threadId);
        Uri uri = ContentUris.withAppendedId(VZUris.getMmsSmsConversationUri(), threadId);
        String selection = deleteAll ? null : "locked=0";
        // Bug 1166 - There's a crash, when user deletes all the conversations.
        // TODO [Ishaque/Peter]. I am not sure which context object is going to pass for this class, so i am
        // passing the context as parameter. if it is wrong please update the code.
        // if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Keys.IS_PAIRED, false)) {
        // if (AndroidUtil.isPaired()) {
        // if (Logger.IS_DEBUG_ENABLED) {
        // Logger.debug("Wifi-Hook:Deleting Conversation:threadId="+threadId);
        // }
        // WifiSyncHelper.markConversationAsDelete(context, threadId);
        //
        // }
        if(ApplicationSettings.getInstance().isProvisioned()){
            // VMA Hook
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("VMA-Hook:Mark a Conversation as deleted:" + uri);
            }
//            Need to call from worker thread 
//            VMAEventHandler vmaHandler = ApplicationSettings.getInstance().getVMAEventHandler();
//            vmaHandler.conversationDelete(threadId);
//            SyncController.getInstance().startVMASync();
            Intent intent = new Intent(SyncManager.ACTION_XCONV_DELETED);
            intent.putExtra(SyncManager.EXTRA_URI, uri);
            context.startService(intent);
        }

        // media cache
        MediaSyncHelper.onThreadDelete(context, threadId);
        // ReadReport 3.1
        MMSReadReport.handleReadReport(context, null, threadId,
                PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ);
        handler.startDelete(token, null, uri, selection, null);
        MessagingNotification.blockingUpdateAllNotifications(context, false);
    }

    /**
     * Start deleting all conversations in the database.
     * 
     * @param mContext2
     * 
     * @param handler
     *            An AsyncQueryHandler that will receive onDeleteComplete upon completion of all conversations
     *            being deleted
     * @param token
     *            The token that will be passed to onDeleteComplete
     * @param deleteAll
     *            Delete the whole thread including locked messages
     */
    public static void startDeleteAll(final Context context, final AsyncQueryHandler handler,
            final int token, boolean deleteAll) {
        final String selection = deleteAll ? null : "locked=0";
        new AsyncTask<Void, Void, Exception>() {

            @Override
            protected Exception doInBackground(Void... params) {
                // Bug 1166 - There's a crash, when user deletes all the conversations.
                // TODO [Ishaque/Peter]. I am not sure which context object is going to pass for this class,
                // so i am passing the context as parameter. if it is wrong please update the code.
                // if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Keys.IS_PAIRED,
                // false)) {
                // if (AndroidUtil.isPaired()) {
                // if (Logger.IS_DEBUG_ENABLED) {
                // Logger.debug("Wifi-Hook:Deleting All Conversation:");
                // }
                // WifiSyncHelper.markAllConversationAsDelete(context);
                //
                // // media cache
                // }
                if(ApplicationSettings.getInstance().isProvisioned()){
                    Uri uri = ContentUris.withAppendedId(VZUris.getMmsSmsConversationUri(), -1);
                    // VMA Hook
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "VMA-Hook:Marks all conversation as deleted:" + uri);
                    }
                    Intent intent = new Intent(SyncManager.ACTION_XCONV_DELETED);
                    intent.putExtra(SyncManager.EXTRA_URI, uri);
                    context.startService(intent);
                }
                MediaSyncHelper.onAllThreadsDelete(context);
                return null;
            }

            protected void onPostExecute(Exception result) {
                // ReadReport 3.1
                MMSReadReport.handleReadReport(context, null, -1,
                        PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ);
                handler.startDelete(token, null, VZUris.getMmsSmsConversationUri(), selection, null);
                MessagingNotification.blockingUpdateAllNotifications(context, false);
            };

        }.execute();

    }

    /**
     * Start deleting selected conversations in the database.
     * 
     * @param handler
     *            An Handler that will receive message upon completion of all conversations being deleted
     * @param syncToke
     *            The token that will be passed to handler on completing sync
     * @param deleteToken
     *            The token that will be passed to handler on completing delete
     * @param threads
     *            The list of threads that will be deleted
     */
    public static void asyncDeleteSelected(final Context context, final Handler handler,
            final int deleteToken, final int syncToken, final ArrayList<Long> threads) {
        // perform the db operation in background thread and notify the UI

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("asyncDeleteSelected entered with threads " + threads);
        }
        // ReadReport 3.1
        MMSReadReport.handleReadReport(context, threads, PduHeaders.READ_STATUS__DELETED_WITHOUT_BEING_READ);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri uri = null;
                Long[] threadArray = null;
                // WifiSyncHelper.markConversationsAsDelete(context, threads);

                // media-cache
                if (null != threads && !threads.isEmpty()) {
                	threadArray = threads.toArray(new Long[threads.size()]);
                } else {
					handler.sendEmptyMessage(deleteToken);
                	return;
                }

                MediaSyncHelper.onThreadDelete(context, threadArray);
                
                if (syncToken != -1) {
                    handler.sendEmptyMessage(syncToken);
                }
                VMAEventHandler vmaHandler = ApplicationSettings.getInstance().getVMAEventHandler();
                for (Long thread : threadArray) {
                    uri = ContentUris.withAppendedId(VZUris.getMmsSmsConversationUri(), thread);
                    try {
                        context.getContentResolver().delete(uri, null, null);
                    } catch (RuntimeException e) {
                        if (Logger.IS_ERROR_ENABLED) {
                            Logger.warn("Unable to delete the selected conversation:" + e.getMessage());
                            Logger.error(e);
                        }
                    }
                    if(ApplicationSettings.getInstance().isProvisioned()){
                        // VMA Hook
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(), "VMA-Hook:Marks XCONV as deleted:" + uri);
                        }
                        vmaHandler.conversationDelete(thread.longValue());
                    }
                    
                    ConversationDataObserver.onMessageDeleted(thread, -1, -1, ConversationDataObserver.MSG_SRC_TELEPHONY);
                }
                MessagingNotification.blockingUpdateAllNotifications(context, false);
                SyncController.getInstance().startVMASync();
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), " XCONV as deleted:ids=" + threads);
                }
                handler.sendEmptyMessage(deleteToken);
            }
        }, "Worker-Conversation").start();
    }

    /**
     * Check for locked messages in all threads or a specified thread.
     * 
     * @param handler
     *            An AsyncQueryHandler that will receive onQueryComplete upon completion of looking for locked
     *            messages
     * @param threadId
     *            The threadId of the thread to search. -1 means all threads
     * @param token
     *            The token that will be passed to onQueryComplete
     */
    public static void startQueryHaveLockedMessages(AsyncQueryHandler handler, long threadId, int token) {
        handler.cancelOperation(token);
        Uri uri = VZUris.getMmsSmsLocked();
        if (threadId != -1) {
            uri = ContentUris.withAppendedId(uri, threadId);
        }
        handler.startQuery(token, new Long(threadId), uri, ALL_THREADS_PROJECTION, null, null,
                Conversations.DEFAULT_SORT_ORDER);
    }

    /**
     * Fill the specified conversation with the values from the specified cursor, possibly setting recipients
     * to empty if {@value allowQuery} is false and the recipient IDs are not in cache. The cursor should be
     * one made via {@link #startQueryForAll}.
     */
    private static void fillFromCursor(Context context, Conversation conv, Cursor c, boolean allowQuery) {
        synchronized (conv) {
            conv.mThreadId = c.getLong(ID);
            conv.mLastThreadId = conv.mThreadId;
            conv.mDate = c.getLong(DATE);
            conv.mMessageCount = c.getInt(MESSAGE_COUNT);

            // Replace the snippet with a default value if it's empty.
            String snippet = MessageUtils.extractEncStrFromCursor(c, SNIPPET, SNIPPET_CS);
            if (TextUtils.isEmpty(snippet)) {
                snippet = context.getString(R.string.no_subject_view);
            }
            conv.mSnippet = snippet;

            conv.setHasUnreadMessages(c.getInt(READ) == 0);
            conv.mHasError = (c.getInt(ERROR) != 0);
        }
        // Fill in as much of the conversation as we can before doing the slow stuff of looking
        // up the contacts associated with this conversation.
        String recipientIds = c.getString(RECIPIENT_IDS);
        ContactList recipients = ContactList.getByIds(recipientIds, allowQuery);
        synchronized (conv) {
            conv.setRecipients(recipients, false);
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(conv + ": fillFromCursor: recipientIds = " + recipientIds);
        }
    }

    /**
     * Private cache for the use of the various forms of Conversation.get.
     */
    private static class Cache {
        private static Cache sInstance = new Cache();
        private final HashSet<Conversation> mCache;

        private Cache() {
            mCache = new HashSet<Conversation>(10);
        }

        /**
         * Return the conversation with the specified thread ID, or null if it's not in cache.
         */
        static Conversation get(final long threadId) {
            /*synchronized (sInstance) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Conversation.Cache.get: threadId = " + threadId);
                }
                final HashSet<Conversation> cache = sInstance.mCache;
                for (Conversation c : cache) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Conversation.Cache.get: cached = " + c.getThreadId());
                    }
                    if (c.getThreadId() == threadId) {
                        return c;
                    }
                }
            }*/
            return null;
        }

        /**
         * Return the conversation with the specified recipient list, or null if it's not in cache.
         */
        static Conversation get(final ContactList list) {
            /*synchronized (sInstance) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Conversation.Cache.get: contactList = " + list);
                }
                final HashSet<Conversation> cache = sInstance.mCache;
                for (Conversation c : cache) {
                    if (c.getRecipients().equals(list)) {
                        return c;
                    }
                }
            }*/
            return null;
        }

        /**
         * Put the specified conversation in the cache. The caller should not place an already-existing
         * conversation in the cache, but rather update it in place.
         * 
         * TODO limit cache size
         */
        static void put(Conversation c) {
            /*synchronized (sInstance) {
                // We update cache entries in place so people with long-held references get updated.
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Conversation.Cache.put: conv = " + c + ", hash = " + c.hashCode());
                }

                if (sInstance.mCache.contains(c)) {
                    throw new IllegalStateException("cache already contains " + c + ", threadId: "
                            + c.mThreadId);
                }
                sInstance.mCache.add(c);
            }*/
        }

        static void remove(long threadId) {
            /*synchronized (sInstance) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Conversation.Cache.remove threadid: " + threadId);
                    dumpCache();
                }
                final HashSet<Conversation> cache = sInstance.mCache;
                for (Conversation c : cache) {
                    if (c.getThreadId() == threadId) {
                        cache.remove(c);
                        return;
                    }
                }
            }*/
        }

        private static void clear() {
            synchronized (sInstance) {
                sInstance.mCache.clear();
            }
        }

        static void dumpCache() {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Conversation.Cache.dumpCache: ");
                for (Conversation c : sInstance.mCache) {
                    Logger.debug("   conv: " + c.toString() + " hash: " + c.hashCode());
                }
            }
        }
    }

    /**
     * Clear the conversation cache.
     */
    public static void init(final Context context) {
        Cache.clear();
        final Resources res = context.getResources();
        notDownloaded = res.getString(R.string.not_downloaded);
    }

    public static void markAllConversationsAsSeen(final Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Conversation.markAllConversationsAsSeen");
        }

        new Thread(new Runnable() {
            public void run() {
                blockingMarkAllSmsMessagesAsSeen(context);
                blockingMarkAllMmsMessagesAsSeen(context);

                // Always update notifications regardless of the read state.
                MessagingNotification.blockingUpdateAllNotifications(context, isRecreateNotification);
            }
        }).start();
    }

    private static void blockingMarkAllSmsMessagesAsSeen(final Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = SqliteWrapper.query(context, resolver, VZUris.getSmsInboxUri(), SEEN_PROJECTION,
                "seen=0", null, null);

        int count = 0;

        if (cursor != null) {
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        }

        if (count == 0) {
            return;
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(Conversation.class, "mark " + count + " SMS msgs as seen");
        }

        ContentValues values = new ContentValues(1);
        values.put("seen", 1);

        SqliteWrapper.update(context, resolver, VZUris.getSmsInboxUri(), values, "seen=0", null);
    }
    
    public static int markMsgAsSeen(final Context context, String msgId, boolean isSMS) {
    	ContentResolver resolver = context.getContentResolver();
    	Cursor cursor = null;
    	int updateCount = 0;
    	if (isSMS) {
    		cursor = SqliteWrapper.query(context, resolver , VZUris.getSmsInboxUri(), SEEN_MSG_PROJECTION, "seen=0" + " AND " + "_id = " + msgId, null, null);	
    	} else {
    		cursor = SqliteWrapper.query(context, resolver , VZUris.getMmsInboxUri(), SEEN_MSG_PROJECTION, "seen=0" + " AND " + "_id = " + msgId, null, null);
    	}
    	
    	int count = 0;
    	if (cursor != null) {
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        }

        if (count == 0) {
            return updateCount;
        }
        
        ContentValues values = new ContentValues(1);
        values.put("seen", 1);
        updateCount = SqliteWrapper.update(context, resolver, VZUris.getSmsInboxUri(), values, "seen=0" + " AND " + " _id = " + msgId, null);
        return updateCount;
    }

    public static ArrayList<Long> getThreadIDs(final Context context) {
        ArrayList<Long> threadIds = new ArrayList<Long>();
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), sAllThreadsUri, null,
                null, null, Conversations.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                while (cursor.isAfterLast() == false) {
                    threadIds.add(cursor.getLong(ID));
                    cursor.moveToNext();
                }
                cursor.close();
            }
        }
        return threadIds;
    }

    private static void blockingMarkAllMmsMessagesAsSeen(final Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = SqliteWrapper.query(context, resolver, VZUris.getMmsInboxUri(), SEEN_PROJECTION,
                "seen=0", null, null);

        int count = 0;

        if (cursor != null) {
            try {
                count = cursor.getCount();
            } finally {
                cursor.close();
            }
        }

        if (count == 0) {
            return;
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(Conversation.class, "mark " + count + " MMS msgs as seen");
        }

        ContentValues values = new ContentValues(1);
        values.put("seen", 1);

        SqliteWrapper.update(context, resolver, VZUris.getMmsInboxUri(), values, "seen=0", null);

    }

    private boolean loadFromThreadId(long threadId, boolean allowQuery) {
    	boolean success = loadFromThreadIdRegularQuery(threadId, allowQuery);
    	
    	if (!success && OEM.isHTC) {
    		success = loadFromThreadIdHtcQuery(threadId, allowQuery);
    	}
    	
    	return success;
    }
    
    /**
     * for some htc devices the threads query does not consider "args" parameter so append the threadid in 
     * where itself
     * @param threadId
     * @param allowQuery
     * @return
     */
    private boolean loadFromThreadIdHtcQuery(long threadId, boolean allowQuery) {
    	boolean success = false;
    	Cursor c = null;
        final String where = Threads._ID + " = " + threadId;
        try {
    		c = SqliteWrapper.query(mContext, mResolver, sAllThreadsUri, ALL_THREADS_PROJECTION, where, null, null);
            if (c != null) {
            	if (c.moveToFirst()) {
	                fillFromCursor(mContext, this, c, allowQuery);
	                success = true;
	
	                if (threadId != mThreadId) {
	                    final String msg = "fillFromCursor returned different thread_id: " + threadId + " / " + mThreadId;
	                    Logger.error(getClass(), msg);
	                    if (Logger.IS_DEBUG_ENABLED) {
	                    	throw new RuntimeException(msg);
	                    }
	                }
            	}
            }
            else {
           		Logger.error(getClass(), "loadFromThreadId: null cursor for thread " + threadId);
            }
    	}
    	catch (Exception e) {
    		Logger.error(getClass(), "loadFromThreadId: exception with thread " + threadId, e);
        }
    	finally {
        	if (c != null) {
        		c.close();
        	}
        }

		return success;
    }
    
    private boolean loadFromThreadIdRegularQuery(long threadId, boolean allowQuery) {
    	boolean success = false;
    	Cursor c = null;
        final String where = Threads._ID + "=?";
        final String[] args = { Long.toString(threadId) };
    	try {
    		c = SqliteWrapper.query(mContext, mResolver, sAllThreadsUri, ALL_THREADS_PROJECTION, where, args, null);
            if (c != null) {
            	if (c.moveToFirst()) {
	                fillFromCursor(mContext, this, c, allowQuery);
	                success = true;
	
	                if (threadId != mThreadId) {
	                    final String msg = "fillFromCursor returned different thread_id: " + threadId + " / " + mThreadId;
	                    Logger.error(getClass(), msg);
	                    if (Logger.IS_DEBUG_ENABLED) {
	                    	throw new RuntimeException(msg);
	                    }
	                }
            	}
            }
            else {
           		Logger.error(getClass(), "loadFromThreadId: null cursor for thread " + threadId);
            }
    	}
    	catch (Exception e) {
    		Logger.error(getClass(), "loadFromThreadId: exception with thread " + threadId, e);
        }
    	finally {
        	if (c != null) {
        		c.close();
        	}
        }

		return success;
    }

    public static int getUnread(Context context, Long threadId) {
        return getUnreadMms(context, threadId) + getUnreadSms(context, threadId);
    }

    private static int getUnreadSms(Context context, Long threadId) {
        Cursor cursor = null;
        try {
            cursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getSmsInboxUri(),
                    COUNT_PROJECTION, "read=0 AND thread_id=?", new String[] { threadId.toString() }, null);
            if (null != cursor && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return 0;
    }

    private static int getUnreadMms(Context context, Long threadId) {
        Cursor cursor = null;
        try {
            final String where = UNREAD_MMS_SELECTION;
            cursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getMmsUri(),
                    COUNT_PROJECTION, where, new String[] { threadId.toString() }, null);
            if (null != cursor && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return 0;
    }

    public static boolean hasSMSMessages(Context context, long threadID) {
        final String[] cols = { Sms._ID };
        final String where = Sms.THREAD_ID + "=" + threadID;
        final Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getSmsUri(),
                cols, where, null, null);
        if (cursor != null) {
            try {
                if (cursor.getCount() > 0) {
                    return true;
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    public static int getUnreadSmsFromInbox(Context context) {
        Cursor cursor = null;

        // dont query using the new query if some exception occured in the old call
        // and we had set the useNewUnreadQuery to false
        if (useNewUnreadQuery) {
            Cursor newCursor = null;
            try {
                newCursor = SqliteWrapper.query(context, context.getContentResolver(),
                        VZUris.getSmsInboxUri(), new String[] { "count (*) AS count" },
                        UNREAD_SMS_QUERY_SELECTION, null, null);
                if (null != newCursor && newCursor.moveToFirst()) {
                    return newCursor.getInt(0);
                }
            } catch (Exception e) {
                Logger.error("getUnreadSmsFromInbox using new query ", e);
                useNewUnreadQuery = false;
            } finally {
                if (null != newCursor) {
                    newCursor.close();
                } else {
                    useNewUnreadQuery = false;
                }
            }
        }

        try {
            cursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getSmsInboxUri(),
                    new String[] { "count (*) AS count" }, "read=0", null, null);
            if (null != cursor && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return 0;
    }

    public static int getUnreadMmsFromInbox(Context context) {
        Cursor cursor = null;

        // dont query using the new query if some exception occured in the old call
        // and we had set the useNewUnreadQuery to false
        if (useNewUnreadQuery) {
            Cursor newCursor = null;
            try {
                newCursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getMmsUri(),
                        new String[] { "count (*) AS count" }, UNREAD_MMS_QUERY_SELECTION, null, null);
                if (null != newCursor && newCursor.moveToFirst()) {
                    return newCursor.getInt(0);
                }
            } catch (Exception e) {
                Logger.error("getUnreadMmsFromInbox using new query ", e);
                useNewUnreadQuery = false;
            } finally {
                if (null != newCursor) {
                    newCursor.close();
                } else {
                    useNewUnreadQuery = false;
                }
            }
        }

        try {
            cursor = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getMmsUri(),
                    new String[] { "count (*) AS count" }, UNREAD_MMS_SELECTION_WIDGET, null, null);
            if (null != cursor && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } catch (Exception e) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return 0;
    }

    /**
     * Returns the subject and attachment type for the last message in the thread or null on error.
     */
    public static MessageData getLastMessageData(Context context, Long threadId) {
        MessageData ret = null;

        final Uri uri = getUri(threadId);
        Cursor cursor = null;
        try {
            cursor = SqliteWrapper.query(context, context.getContentResolver(), uri,
                    MessageListAdapter.PROJECTION, null, null, null);
            if (cursor != null) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Conversation.getLastMessageData: threadId=" + threadId + ", cursor count="
                            + cursor.getCount());
                }
                if (cursor.moveToLast()) {
                    try {
                        final MessageItem item = new MessageItem(context, cursor, null, false, true);
                        ret = new MessageData(getSubject(item), item.getAttachmentType(), DeliveryStatus.NONE);
                    } catch (Exception e) {
                        Logger.error("error getting data for uri " + uri, e);
                    }
                }
            } else {
                Logger.error(MessageItem.class, "null cursor for " + uri);
            }
        } catch (Exception e) {
            Logger.error("error querying uri " + uri, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (ret == null) {
            // create a blank response
            ret = new MessageData("", WorkingMessage.NONE, DeliveryStatus.NONE);
        }

        return ret;
    }

    private static CharSequence getSubject(MessageItem item) {
        final CharSequence ret;
        if (item.isDownloaded()) {
            // if it has a subject then use it
            final String subject = item.getSubject();
            if (subject != null && subject.length() != 0) {
                ret = MessageUtils.parseEmoticons(subject);
            } else {
                // try to use the body
                final String body = item.getBody();
                if (body != null) {
                    ret = MessageUtils.parseEmoticons(body);
                } else {
                    ret = "";
                }
            }
        } else {
            ret = notDownloaded;
        }
        return ret;
    }

    /**
     * Get data about the most recently received messages in the conversation.
     */
    public static IncomingMessageData getLastIncomingData(Cursor cursor) {
        IncomingMessageData data = null;
        try {
            if (cursor != null) {
                // save position
                final int curPos = cursor.getPosition();

                // get position of last incoming message
                if (cursor.moveToLast()) {
                    final HashSet<String> xids = new HashSet<String>(MAX_INCOMING_XIDS);
                    int pos = -1;
                    long lastId = -1;
                    long lastMmsId = -1;
                    long lastSmsId = -1;
                    boolean lastIsMms = false;
                    int numMsgs = 0;
                    int numIncoming = 0;
                    do {
                        final String type = cursor.getString(COLUMN_MSG_TYPE);
                        final boolean msgMms = type != null && type.charAt(0) == 'm';
                        final boolean incoming = msgMms ?
                        	cursor.getInt(COLUMN_MMS_MESSAGE_TYPE) == MESSAGE_TYPE_RETRIEVE_CONF :
                            cursor.getInt(COLUMN_SMS_TYPE) == Sms.MESSAGE_TYPE_INBOX;
                        if (incoming) {
                            final long id = cursor.getLong(COLUMN_ID);
                            if (numIncoming++ == 0) {
                                // get data for last incoming message
                                pos = cursor.getPosition();
                                lastId = id;
                                lastIsMms = msgMms;
                            }
                            if (msgMms) {
                            	if (lastMmsId == -1) {
                            		lastMmsId = id;
                            	}
                                final String xid = cursor.getString(COLUMN_MMS_TRANSACTION_ID);
                                if (xid != null) {
                                    xids.add(xid);
                                }
                            }
                            else if (lastSmsId == -1) {
                            	lastSmsId = id;
                            }
                        }
                    } while (cursor.moveToPrevious() &&
                   		(numIncoming < MAX_INCOMING_XIDS || lastMmsId == -1 || lastSmsId == -1) &&
                   		++numMsgs < MAX_INCOMING_MSGS);

                    if (numIncoming > 0) {
                        data = new IncomingMessageData(pos, lastId, lastMmsId, lastSmsId, lastIsMms, xids);
                    }

                    // restore position
                    cursor.moveToPosition(curPos);
                }
            }
        } catch (Exception e) {
            Logger.error(Conversation.class, e);
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(Conversation.class, "getLastIncomingData: returning " + data);
        }

        return data;
    }
	
	public static boolean hasUnreadMessage(Cursor cursor) {
		boolean hasUnread = false;
		try {
			if (cursor != null) {
				// save position
				final int curPos = cursor.getPosition();

				// get position of last incoming message
				if (cursor.moveToLast()) {
					int numScanned = 0;
					do {
						final String type = cursor.getString(COLUMN_MSG_TYPE);
						final boolean msgMms = type != null && type.charAt(0) == 'm';
						final boolean incoming = msgMms ?
							cursor.getInt(COLUMN_MMS_MESSAGE_TYPE) == MESSAGE_TYPE_RETRIEVE_CONF :
							cursor.getInt(COLUMN_SMS_TYPE) == Sms.MESSAGE_TYPE_INBOX;
						
						if (incoming) {
							hasUnread = msgMms ?
									cursor.getInt(MessageListAdapter.COLUMN_MMS_READ) == 0 :
									cursor.getInt(MessageListAdapter.COLUMN_SMS_READ) == 0;
							break;
						}
					} while (cursor.moveToPrevious() && ++numScanned < MAX_UNREAD_CHECK);

					// restore position
					cursor.moveToPosition(curPos);
				}
			}
		}
		catch (Exception e) {
			Logger.error(Conversation.class, e);
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(Conversation.class, "hasUnreadMessage: returning " + hasUnread);
		}
		return hasUnread;
	}

    /**
     * Get the position of the first outgoing message in the conversation.
     */
    public static int getFirstOutgoingPos(Cursor cursor) {
        int pos = -1;
        try {
            if (cursor != null) {
                // save position
                final int curPos = cursor.getPosition();

                // get position of first outgoing message
                if (cursor.moveToFirst()) {
                    do {
                        final String type = cursor.getString(COLUMN_MSG_TYPE);
                        final boolean mms = type != null && type.charAt(0) == 'm';
                        if (isOutgoing(cursor, mms)) {
                            pos = cursor.getPosition();
                            break;
                        }
                    } while (cursor.moveToNext());

                    // restore position
                    cursor.moveToPosition(curPos);
                }
            }
        } catch (Exception e) {
            Logger.error(e);
        }
        return pos;
    }

    /**
     * Get data from the last outgoing message in the conversation.
     */
    public static OutgoingMessageData getLastOutgoingData(Context context, Cursor cursor, boolean group) {
        OutgoingMessageData data = null;
        try {
            if (cursor != null) {
                // save position
                final int curPos = cursor.getPosition();

                // get position of last outgoing message
                if (cursor.moveToLast()) {
                    do {
                        final String type = cursor.getString(COLUMN_MSG_TYPE);
                        final boolean mms = type != null && type.charAt(0) == 'm';
                        if (isOutgoing(cursor, mms)) {
                            final int pos = cursor.getPosition();
                            final GroupMode mode = group ? getGroupMode(context, cursor, mms) : null;
                            final long time;
							if (mms) {
								time = cursor.getLong(COLUMN_MMS_DATE) * 1000;
							}
							else {
								time = cursor.getLong(COLUMN_SMS_DATE);
							}
							
							data = new OutgoingMessageData(pos, mode, time);
                            break;
                        }
                    } while (cursor.moveToPrevious());

                    // restore position
                    cursor.moveToPosition(curPos);
                }
            }
        } catch (Exception e) {
            Logger.error(Conversation.class, e);
        }
        return data;
    }

    public static OutgoingMessageData getLastOutgoingData(Context context, long threadId, boolean group) {
        final Uri uri = getUri(threadId);
        Cursor cursor = null;
        try {
            cursor = SqliteWrapper.query(context, context.getContentResolver(), uri,
                    MessageListAdapter.PROJECTION, null, null, null);
            if (cursor != null) {
                return getLastOutgoingData(context, cursor, group);
            } else {
                Logger.error(Conversation.class, "null cursor for " + uri);
            }
        } catch (Exception e) {
            Logger.error("error querying uri " + uri, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static long getLastOutgoingId(Context context, long threadId, boolean mms) {
        long id = -1;
        try {
            final String[] cols = { "MAX(_id) AS _id" };
            final String[] args = { Long.toString(threadId) };
            final Uri uri;
            final String where;
            if (mms) {
                uri = VZUris.getMmsUri();
                where = OUTGOING_MMS_SELECTION;
            } else {
                uri = VZUris.getSmsUri();
                where = OUTGOING_SMS_SELECTION;
            }
            final Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), uri, cols,
                    where, args, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    id = cursor.getLong(0);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Logger.error(Conversation.class, e);
        }
        return id;
    }

    public static List<Long> getMessagesSince(Cursor cursor, long lastId, long lastDate, boolean mms) {
        final ArrayList<Long> ids = new ArrayList<Long>(0);
        try {
            if (lastId != -1 && cursor != null) {
                // save position
                final int curPos = cursor.getPosition();

                if (cursor.moveToLast()) {
                    // count number of outgoing messages of the given type since the given id
                    final char typec;
                    final int dateCol;
                    if (mms) {
                        typec = 'm';
                        dateCol = COLUMN_MMS_DATE;
                    } else {
                        typec = 's';
                        dateCol = COLUMN_SMS_DATE;
                    }

                    do {
                        final String type = cursor.getString(COLUMN_MSG_TYPE);
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Conversation.getMessagesSince: id = " + cursor.getLong(COLUMN_ID)
                                    + ", type = " + type + ", outgoing = " + isOutgoing(cursor, mms)
                                    + ", date = " + cursor.getLong(dateCol));
                        }
                        if (type != null && type.charAt(0) == typec && isOutgoing(cursor, mms)) {
                            final long id = cursor.getLong(COLUMN_ID);
                            if (id > lastId) {
                                ids.add(id);
                            } else if (cursor.getLong(dateCol) <= lastDate) {
                                break;
                            }
                        }
                    } while (cursor.moveToPrevious());

                    // restore position
                    cursor.moveToPosition(curPos);
                }
            }
        } catch (Exception e) {
            Logger.error("Conversation.getMessagesSince:", e);
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Conversation.getMessagesSince: lastId = " + lastId + ", lastDate = " + lastDate
                    + ", ids = " + ids);
        }

        return ids;
    }

    /**
     * Returns the group delivery mode of the previous outbound message. NB assumes the message is a group
     * message.
     */
    public static GroupMode getPreviousGroupMode(Context context, Cursor cursor, int curPos) {
        GroupMode mode = null;
        try {
            while (cursor.moveToPrevious()) {
                final String type = cursor.getString(COLUMN_MSG_TYPE);
                final boolean mms = type != null && type.charAt(0) == 'm';
                if (isOutgoing(cursor, mms)) {
                    mode = getGroupMode(context, cursor, mms);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(Conversation.class,
                                "getPreviousGroupMode: msg " + cursor.getString(COLUMN_ID) + ", mode = "
                                        + mode);
                    }
                    break;
                }
            }
            cursor.moveToPosition(curPos);
        } catch (Exception e) {
            Logger.error(Conversation.class, e);
        }
        return mode;
    }

    private static GroupMode getGroupMode(Context context, Cursor cursor, boolean mms) {
        GroupMode mode;
        if (mms) {
            // need to check address headers for BCC
            mode = GroupMode.GROUP;
            final String msgId = cursor.getString(COLUMN_ID);
            final Uri uri = VZUris.getMmsUri().buildUpon().appendPath(msgId).appendPath("addr").build();
            final String[] cols = { Addr.TYPE };
            final String where =Addr.TYPE+"="+PduHeaders.BCC;   
            final Cursor addrCursor = SqliteWrapper.query(context, context.getContentResolver(), uri, cols,
                    where, null, null);
            if (addrCursor != null) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(Conversation.class, "getGroupMode: BCC count = " + addrCursor.getCount());
                }
                if (addrCursor.getCount() > 0) {
                    mode = GroupMode.SENDER;
                }
                // while (addrCursor.moveToNext()) {
                // final int hdr = addrCursor.getInt(0);
                // if (Logger.IS_DEBUG_ENABLED) {
                // Logger.debug(Conversation.class, "getGroupMode: hdr = " + hdr);
                // }
                // if (hdr == PduHeaders.BCC) {
                // mode = GroupMode.SENDER;
                // break;
                // }
                // }
                addrCursor.close();
            }
        } else {
            mode = GroupMode.SENDER;
        }
        return mode;
    }

    private static boolean isOutgoing(Cursor cursor, boolean mms) {
        final boolean outgoing;
        if (mms) {
            final int type = cursor.getInt(COLUMN_MMS_MESSAGE_TYPE);
            outgoing = type == MESSAGE_TYPE_SEND_REQ || type == MESSAGE_TYPE_SEND_REQ_X;
        } else {
            outgoing = Sms.isOutgoingFolder(cursor.getInt(COLUMN_SMS_TYPE));
        }
        return outgoing;
    }

    public static int getThreadCount(Context context) {
        int count = -1;
        final ContentResolver res = context.getContentResolver();
        try {
            final Cursor cursor = SqliteWrapper.query(context, res, sAllThreadsUri, COUNT_PROJECTION,
                    ALL_THREADS_WHERE, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Logger.error(e);
        }
        return count;
    }
}
