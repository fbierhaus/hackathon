package com.verizon.mms.data;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContactsEntity;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Threads;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.DeviceConfig.OEM;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.Util;

public class Contact {
	private static final boolean V = false;
	private static ContactsCache sContactCache;
	private final static HashSet<UpdateListener> mListeners = new HashSet<UpdateListener>();
	private String mNumber;
	private String mName;
	private String mNameAndNumber; // for display, e.g. Fred Flintstone <670-782-1123>
	private boolean mNumberIsModified; // true if the number is modified
	private long mRecipientId; // used to find the Recipient cache entry
	private String mLabel;
	private String mPrefix; 
	private int    mChannelType; // gives the channel type
	private long mPersonId;
	private long mContactPictureId;
	private int mPresenceResId; // TODO: make this a state instead of a res ID
	private String mPresenceText;
	private int mAvatarHashCode = 0; //used to compare two byte array's of avatar to see if the image changed
	private SoftReference<BitmapDrawable> mAvatarCache = new SoftReference<BitmapDrawable>(null);
	//private byte[] mAvatarData;
	private boolean mIsStale;
	private boolean mQueryPending;
	private long mLastInvalidatedTime;
	
	private static Context mContext;
	private static ContentResolver mResolver;
	private static String mPhoneType;
	private static String mEmailType;
	private static Comparator<Contact> mNumberComparator = null;
	private static boolean mLoadingThreads;

	private static final int ADDR_TYPE_NONE = 0;
	private static final int ADDR_TYPE_EMAIL = 1;
	private static final int ADDR_TYPE_NUMBER = 2;
	

	private static final Uri sAllThreadsUri =
		VZUris.getMmsSmsConversationUri().buildUpon().appendQueryParameter("simple", "true").build();

	// filter out temporary threads created for drafts etc and threads with id -1.
	private static final String ALL_THREADS_WHERE = Threads.RECIPIENT_IDS + " != '' and " + Threads._ID + " != -1";
		
	private static final String[] ALL_THREADS_PROJECTION = {
		Threads._ID, Threads.RECIPIENT_IDS};

	private static final int ID             = 0;
	private static final int RECIPIENT_IDS  = 1;

	private static final int INVALIDATE_MIN_TIME = 300000; //5 mins minimum time to invalidate again

	private static boolean mUseRawContactQuery = true;
	private static final Contact VZ_CONTACT;

	private static final int CACHE_THREAD_LIMIT = 10;
	
	public interface UpdateListener {
		public void onUpdate(Contact updated, Object cookie);
	}
	static {
	    // No context object so we are hard coding 
        VZ_CONTACT = new Contact(AppSettings.VZW_SERVICEMSG_SENDER_NO);
        VZ_CONTACT.mName = "Verizon Messages";
        VZ_CONTACT.mNumber=AppSettings.VZW_SERVICEMSG_SENDER_NO;
    }
	private static final ContentObserver sContactsObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfUpdate) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("contact changed, invalidate cache");
			}
			invalidateCache();
		}
	};
	
	/*
	 * Make a basic contact object with a phone number.
	 */
	private Contact(String number) {
		mName = "";
		mChannelType = ADDR_TYPE_NONE;
		setNumber(number);
		mNumberIsModified = false;
		mLabel = "";
		mPersonId = 0;
		mPresenceResId = 0;
		mIsStale = true;
		mPrefix = "";
	}
	
	private Contact(String number, String prefix, int type) {
	    this(number);
	    
	    this.mPrefix = prefix;
	    this.mChannelType = type;
	}

	@Override
	public String toString() {
		return String.format("{ number=%s, name=%s, nameAndNumber=%s, label=%s, personId=%d, recipientId=%d, hash=%d }",
				(mNumber != null ? mNumber : "null"), (mName != null ? mName : "null"),
				(mNameAndNumber != null ? mNameAndNumber : "null"), (mLabel != null ? mLabel : "null"), mPersonId,
				mRecipientId, hashCode());
	}

	private static void logWithTrace(String msg, Object... format) {
		Thread current = Thread.currentThread();
		StackTraceElement[] stack = current.getStackTrace();

		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(current.getId());
		sb.append("] ");
		sb.append(String.format(msg, format));

		sb.append(" <- ");
		int stop = stack.length > 7 ? 7 : stack.length;
		for (int i = 3; i < stop; i++) {
			String methodName = stack[i].getMethodName();
			sb.append(methodName);
			if ((i + 1) != stop) {
				sb.append(" <- ");
			}
		}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(Contact.class, sb.toString());
		}
	}

	public static Contact get(String number, boolean canBlock) {
		return sContactCache.get(number, canBlock, true);
	}

	public static Contact get(String number, boolean canBlock, boolean update) {
		return sContactCache.get(number, canBlock, update);
	}

	public static Contact get(String number, boolean canBlock, boolean update, Object cookie) {
		return sContactCache.get(number, canBlock, update, cookie);
	}
	
	public static void invalidateCache() {
		if (Logger.IS_DEBUG_ENABLED) {
			log("invalidateCache");
		}

		// While invalidating our local Cache doesn't remove the contacts, it will mark them
		// stale so the next time we're asked for a particular contact, we'll return that
		// stale contact and at the same time, fire off an asyncUpdateContact to update
		// that contact's info in the background. UI elements using the contact typically
		// call addListener() so they immediately get notified when the contact has been
		// updated with the latest info. They redraw themselves when we call the
		// listener's onUpdate().
		sContactCache.invalidate();
	}

	private static String emptyIfNull(String s) {
		return (s != null ? s : "");
	}

	public static String formatNameAndNumber(String name, String number) {
		// Format like this: Mike Cleron <(650) 555-1234>
		// Erick Tseng <(650) 555-1212>
		// Tutankhamun <tutank1341@gmail.com>
		// (408) 555-1289
		String formattedNumber = number;
		if (!Mms.isEmailAddress(number)) {
			formattedNumber = PhoneNumberUtils.formatNumber(number);
		}

		if (!TextUtils.isEmpty(name) && !name.equals(number)) {
			return name + " <" + formattedNumber + ">";
		} else {
			return formattedNumber;
		}
	}

	public synchronized void reload() {
		mIsStale = true;
		mLastInvalidatedTime = 0;
		sContactCache.get(mNumber, false, true);
	}
	
	public synchronized void markAsStale() {
		mIsStale = true;
		mLastInvalidatedTime = 0;
	}

	public String getLookUpKey() {
	    if (mName == null || mName.length() == 0) {
	        return null;
	    }
	    
	    Uri lkup = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(mName));
	    String lookupKey = null;
	    String tokenWhere = ContactsContract.Contacts.DISPLAY_NAME + " = ?";
	    String[] tokenWhereParams = new String[]{String.valueOf(mName)};
	    Cursor idCursor = SqliteWrapper.query(mContext, mResolver, lkup,
	    	new String[]{ContactsContract.Contacts.DISPLAY_NAME,ContactsContract.Contacts.LOOKUP_KEY },
	    	tokenWhere, tokenWhereParams, null);
	    
	    if (idCursor != null) {
	        if (idCursor.moveToFirst()) {
	            lookupKey = idCursor.getString(idCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
	        }
	        idCursor.close();
	    } 
	    
	    if (lookupKey != null) {
	       return lookupKey;
	    }
	    
	    return null;
	}
	
	public synchronized String getNumber() {
		return mNumber;
	}

	public synchronized void setNumber(String number) {
		mNumber = number;
		notSynchronizedUpdateNameAndNumber();
		mNumberIsModified = true;
	}

	public boolean isNumberModified() {
		return mNumberIsModified;
	}

	public void setIsNumberModified(boolean flag) {
		mNumberIsModified = flag;
	}

	public synchronized String getName() {
		return getName(true);
	}

	public synchronized String getName(boolean checkNumber) {
		if (TextUtils.isEmpty(mName)) {
			return checkNumber ? mNumber : "";
		} else {
			return mName;
		}
	}

	public synchronized String getNameAndNumber() {
		return mNameAndNumber;
	}

	private void notSynchronizedUpdateNameAndNumber() {
		mNameAndNumber = formatNameAndNumber(mName, mNumber);
	}

	public synchronized long getRecipientId() {
		return mRecipientId;
	}

	public synchronized void setRecipientId(long id) {
		mRecipientId = id;
	}
	
	public String getPrefix() {
		String prefix = mPrefix;
		
		if (prefix == null) {
			if (mChannelType == ADDR_TYPE_EMAIL) {
				prefix = mEmailType;
			} else if (mChannelType == ADDR_TYPE_NUMBER) {
				prefix = mPhoneType;
			}
		}
	    return prefix;
	}

	public synchronized String getLabel() {
		return mLabel;
	}

	public synchronized Uri getUri() {
		return ContentUris.withAppendedId(Contacts.CONTENT_URI, mPersonId);
	}

	public synchronized int getPresenceResId() {
		return mPresenceResId;
	}

	public synchronized boolean existsInDatabase() {
		return (mPersonId > 0);
	}

	public static void addListener(UpdateListener l) {
		synchronized (mListeners) {
			mListeners.add(l);
		}
	}

	public static void removeListener(UpdateListener l) {
		synchronized (mListeners) {
			mListeners.remove(l);
		}
	}

	public static synchronized void dumpListeners() {
		int i = 0;
		if (Logger.IS_DEBUG_ENABLED) {
		Logger.debug(Contact.class, "[Contact] dumpListeners; size=" + mListeners.size());
		
		}for (UpdateListener listener : mListeners) {
			if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(Contact.class, "[" + (i++) + "]" + listener);
			}
		}
	}

	public synchronized boolean isEmail() {
		return Mms.isEmailAddress(mNumber);
	}

	public String getPresenceText() {
		return mPresenceText;
	}

	public synchronized Drawable getAvatar(Context context, Drawable defaultValue) {
		BitmapDrawable bm = mAvatarCache.get();
		if (mAvatarHashCode != 0 && bm == null) {
			long id = mContactPictureId != 0 ? mContactPictureId : mPersonId;
			bm = Util.loadAvatarImage(context, id);
			
			if (bm != null) {
				mAvatarCache = new SoftReference<BitmapDrawable>(bm);
			}
		}
		return bm != null ? bm : defaultValue;
	}
	
	public static void init(final Context context) {
		sContactCache = new ContactsCache(context);
        mContext = context;
        mResolver = context.getContentResolver();
		RecipientIdCache.init(context);

		mPhoneType = context.getString(R.string.type_phone_number);
		mEmailType = context.getString(R.string.type_email);
		// it maybe too aggressive to listen for *any* contact changes, and rebuild MMS contact
		// cache each time that occurs. Unless we can get targeted updates for the contacts we
		// care about(which probably won't happen for a long time), we probably should just
		// invalidate cache peoridically, or surgically.
		
		 context.getContentResolver().registerContentObserver(
		  Contacts.CONTENT_URI, true, sContactsObserver);
		 
		new Thread(new Runnable() {
            public void run() {
                cacheAllThreadContacts(context);
            }
        }).start();
	}
	
	//update the email and mobile strings when the locale of 
	//the app is changed through applications settings
	public static void onAppLocaleChanged(final Context context) {
		mPhoneType = context.getString(R.string.type_phone_number);
		mEmailType = context.getString(R.string.type_email);
	}

	//load all the contacts present in the threads
	protected static void cacheAllThreadContacts(Context context) {
		if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("[Contact] cacheAllThreadContacts: begin");
        }
        synchronized (sContactCache) {
            if (mLoadingThreads) {
                return;
            }
            mLoadingThreads = true;
        }

        // Query for all conversations.
        Cursor c = null;
        try {
        	c = context.getContentResolver().query(sAllThreadsUri,
                    ALL_THREADS_PROJECTION, ALL_THREADS_WHERE, null, null);
            if (c != null) {
            	int i = 0;
                while (c.moveToNext()) {
                	String recipId = c.getString(Contact.RECIPIENT_IDS);
                    ContactList.getByIds(recipId, true);
                    i++;
                    
                    if (i >= CACHE_THREAD_LIMIT) {
                    	if (Logger.IS_DEBUG_ENABLED) {
                    	Logger.debug("Contact.cacheAllThreads cached " + CACHE_THREAD_LIMIT + " contacts ");
                    	}
                    	break;
                    }
                }
            }
        } catch (Exception e) {
        	//catch all the exception there is no reson to allow the app the crash if 
        	// this thread fails
        	Logger.error("Contact.cacheAllThreads ", e);
        }
        finally {
            if (c != null) {
                c.close();
            }
            synchronized (sContactCache) {
                mLoadingThreads = false;
            }
        }
	}
	
	public static boolean isLoadingContacts() {
		return mLoadingThreads;
	}

	public static void dump() {
		sContactCache.dump();
	}

	public static class ContactsCache {
		private final TaskStack mTaskQueue = new TaskStack();

		//raw phone mime type selection
		private static final String RAW_PHONE_MIME_SELECTION = ContactsContract.RawContactsEntity.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
				
		private static final String[] DEEP_ENTRY_PROJECTION = new String[] {
					RawContactsEntity._ID,
					RawContactsEntity.CONTACT_ID,
					RawContactsEntity.DATA1,
					RawContactsEntity.DATA2,
					RawContactsEntity.DELETED
		};
				
		private static final int DEEP_RAW_CONTACT_ID_COLUMN = 0;
		private static final int DEEP_CONTACT_ID_COLUMN = 1;
		private static final int DEEP_DATA_COLUMN = 2;
		private static final int DEEP_DATA_TYPE_COLUMN = 3;
		private static final int DEEP_DELETED_COLUMN = 4;
				
		//raw email mime type selection
		private static final String RAW_EMAIL_MIME_SELECTION = ContactsContract.RawContactsEntity.MIMETYPE + "='" + CommonDataKinds.Email.CONTENT_ITEM_TYPE + "'";
		
		private static final String RAW_CALLER_ID_SELECTION  = RAW_PHONE_MIME_SELECTION + ") AND ("+RawContactsEntity.DELETED+"=0 AND PHONE_NUMBERS_EQUAL(" + RawContactsEntity.DATA1 + ", ?)) union SELECT " +
				TextUtils.join(" , ", DEEP_ENTRY_PROJECTION)+
				" FROM contact_entities_view WHERE (1) AND (" + RAW_PHONE_MIME_SELECTION + ") AND ("+RawContactsEntity.DELETED+"=0 AND PHONE_NUMBERS_EQUAL(" + RawContactsEntity.DATA1 + ", ?)";
		
		private static final String RAW_EMAIL_SELECTION = RAW_EMAIL_MIME_SELECTION + ") AND ("+RawContactsEntity.DELETED+"=0 AND UPPER(" + RawContactsEntity.DATA1 + ")=?) union SELECT " +
				TextUtils.join(" , ", DEEP_ENTRY_PROJECTION)+
				" FROM contact_entities_view WHERE (1) AND (" + RAW_EMAIL_MIME_SELECTION + ") AND ("+RawContactsEntity.DELETED+"=0 AND UPPER(" + RawContactsEntity.DATA1 + ")=?";
		
		// query params for caller id lookup
		// Now it checks 
		private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER + ",?) AND "
				+ Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'" + " OR " + Data.RAW_CONTACT_ID + " IN "
				+ "(SELECT raw_contact_id " + " FROM phone_lookup" + " WHERE normalized_number GLOB('+*'))";
		
		//private static final String CALLER_ID_SELECTION = " Data._ID IN "+ " (SELECT DISTINCT lookup.data_id "+ " FROM "+ " (SELECT data_id, normalized_number, length(normalized_number) as len "+ " FROM phone_lookup "+ " WHERE min_match = ?) AS lookup "+ " WHERE "+ " (lookup.len <= ? AND "+ " substr(?, ? - lookup.len + 1) = lookup.normalized_number))";

		// Utilizing private API
		private static final Uri PHONES_WITH_PRESENCE_URI = Data.CONTENT_URI;

		private static final String[] CALLER_ID_PROJECTION = new String[] { Phone.NUMBER, // 0
		        Phone.LABEL, // 1
		        Phone.DISPLAY_NAME, // 2
				Phone.CONTACT_ID, // 3
				Phone.CONTACT_PRESENCE, // 4
				Phone.CONTACT_STATUS, // 5
				Phone.TYPE//6
		};

		private static final int PHONE_NUMBER_COLUMN = 0;
		private static final int PHONE_LABEL_COLUMN = 1;
		private static final int CONTACT_NAME_COLUMN = 2;
		private static final int CONTACT_ID_COLUMN = 3;
		private static final int CONTACT_PRESENCE_COLUMN = 4;
		private static final int CONTACT_STATUS_COLUMN = 5;
		private static final int PHONE_TYPE = 6;

		private static final String CALLER_ID_SELECTION_ICS = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER + ",?) AND "
				+ Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'" + " AND " + Data.RAW_CONTACT_ID + " IN "
				+ "(SELECT raw_contact_id " + " FROM phone_lookup" + " WHERE normalized_number GLOB('+*'))";
		
		private static final String CALLER_ID_PROJ_ICS[] = {
			PhoneLookup.NUMBER, PhoneLookup.LABEL, PhoneLookup.DISPLAY_NAME, PhoneLookup._ID, PhoneLookup.PHOTO_ID, PhoneLookup.LOOKUP_KEY, PhoneLookup.TYPE
	    };

		private static final int PHONE_NUMBER_COLUMN_ICS = 0;
		private static final int PHONE_LABEL_COLUMN_ICS = 1;
		private static final int CONTACT_NAME_COLUMN_ICS = 2;
		private static final int CONTACT_ID_COLUMN_ICS = 3;
		private static final int CONTACT_PHOTO_ID_ICS = 4;
		private static final int CONTACT_LOOKUP_KEY_ICS = 5;
		private static final int PHONE_TYPE_ICS = 6;
		
		// query params for contact lookup by email
		private static final Uri EMAIL_WITH_PRESENCE_URI = Data.CONTENT_URI;

		private static final String EMAIL_SELECTION = "UPPER(" + Email.DATA + ")=? AND " + Data.MIMETYPE + "='"
				+ Email.CONTENT_ITEM_TYPE + "'";

		private static final String[] EMAIL_PROJECTION = new String[] { Email.DISPLAY_NAME, // 0
				Email.CONTACT_PRESENCE, // 1
				Email.CONTACT_ID, // 2
				Phone.DISPLAY_NAME, //
		};
		private static final int EMAIL_NAME_COLUMN = 0;
		private static final int EMAIL_STATUS_COLUMN = 1;
		private static final int EMAIL_ID_COLUMN = 2;
		private static final int EMAIL_CONTACT_NAME_COLUMN = 3;

		private final HashMap<String, ArrayList<Contact>> mContactsHash = new HashMap<String, ArrayList<Contact>>();


		private ContactsCache(Context context) {
		}

		void dump() {
			synchronized (ContactsCache.this) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(Contact.class, "**** Contact cache dump ****");
                }
				for (String key : mContactsHash.keySet()) {
					ArrayList<Contact> alc = mContactsHash.get(key);
					for (Contact c : alc) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(Contact.class, key + " ==> " + c.toString());
                        }
					}
				}
			}
		}

		private static class TaskStack {
			Thread mWorkerThread;
			private final ArrayList<Runnable> mThingsToLoad;

			public TaskStack() {
				mThingsToLoad = new ArrayList<Runnable>();
				mWorkerThread = new Thread(new Runnable() {
					public void run() {
						while (true) {
							Runnable r = null;
							synchronized (mThingsToLoad) {
								if (mThingsToLoad.size() == 0) {
									try {
										mThingsToLoad.wait();
									} catch (InterruptedException ex) {
										// nothing to do
									}
								}
								//process in LIFO
								int size = mThingsToLoad.size();
								if (size > 0) {
									r = mThingsToLoad.remove(size - 1);
								}
							}
							if (r != null) {
								r.run();
							}
						}
					}
				});
				mWorkerThread.start();
			}

			public void push(Runnable r) {
				synchronized (mThingsToLoad) {
					mThingsToLoad.add(r);
					mThingsToLoad.notify();
				}
			}
		}

		public void pushTask(Runnable r) {
			mTaskQueue.push(r);
		}

		private Contact get(String number, boolean canBlock, boolean update) {
			return get(number, canBlock, update, null);
		}
		
		private Contact get(String numberAddress, boolean canBlock, boolean update, final Object object) {
			if (V)
				logWithTrace("get(%s, %s)", numberAddress, canBlock);
			String number = numberAddress;
			if (VZ_CONTACT.mNumber.equals(number)) {
                return VZ_CONTACT;
            }
			if (TextUtils.isEmpty(numberAddress)) {
				
				number = ""; // In some places (such as Korea), it's possible to receive
								// a message without the sender's address. In this case,
								// all such anonymous messages will get added to the same
								// thread.
			} else {
				// before stripping check if it is a valid phone number 
				// since there are chances of messages coming in as SBI23XM
				if (!Mms.isEmailAddress(numberAddress) && MessageUtils.isValidMmsAddress(numberAddress)) {
		    		number = PhoneNumberUtils.stripSeparators(numberAddress);
		    	}
			}

			// Always return a Contact object, if if we don't have an actual contact
			// in the contacts db.			
			Contact contact = get(number);

			if (update) {
				Runnable r = null;
	
				synchronized (contact) {
					// If there's a query pending and we're willing to block then
					// wait here until the query completes.
					while (canBlock && contact.mQueryPending) {
						try {
							contact.wait();
						} catch (InterruptedException ex) {
							// try again by virtue of the loop unless mQueryPending is false
						}
					}
	
					long time = SystemClock.elapsedRealtime();
					// If we're stale and we have exceeded 5 mins since we last invalidated the cache and 
					//we haven't already kicked off a query then kick
					// it off here.
					if (contact.mIsStale /*&& (contact.mLastInvalidatedTime + INVALIDATE_MIN_TIME < time || 
							contact.mLastInvalidatedTime == 0)*/ 
							&& !contact.mQueryPending) {
						contact.mIsStale = false;

                        if (Logger.IS_DEBUG_ENABLED) {
                            log("async update for " + contact.toString() + " canBlock: " + canBlock
                                    + " isStale: " + contact.mIsStale);
                        }
	
						final Contact c = contact;
						r = new Runnable() {
							public void run() {
								updateContact(c, object);
							}
						};
	
						// set this to true while we have the lock on contact since we will
						// either run the query directly (canBlock case) or push the query
						// onto the queue. In either case the mQueryPending will get set
						// to false via updateContact.
						contact.mQueryPending = true;
					}
				}
				// do this outside of the synchronized so we don't hold up any
				// subsequent calls to "get" on other threads
				if (r != null) {
					if (canBlock) {
						r.run();
					} else {
						pushTask(r);
					}
				}
			}

			return contact;
		}

		private boolean contactChanged(Contact orig, Contact newContactData) {
			// The phone number should never change, so don't bother checking.
			// TODO: Maybe update it if it has gotten longer, i.e. 650-234-5678 -> +16502345678?

			String oldName = emptyIfNull(orig.mName);
			String newName = emptyIfNull(newContactData.mName);
			if (!oldName.equals(newName)) {
                if (V) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(Contact.class, String.format("name changed: %s -> %s", oldName, newName));
                    }
                }
					return true;
			}

			String oldLabel = emptyIfNull(orig.mLabel);
			String newLabel = emptyIfNull(newContactData.mLabel);
			if (!oldLabel.equals(newLabel)) {
                if (V) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(Contact.class,
                                String.format("label changed: %s -> %s", oldLabel, newLabel));
                    }
                }
					return true;
			}
			
			String oldPrefix = emptyIfNull(orig.mPrefix);
			String newPrefix = emptyIfNull(newContactData.mPrefix);
			if (!oldPrefix.equals(newPrefix)) {
                if (V) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(Contact.class,
                                String.format("prefix changed: %s -> %s", oldPrefix, newPrefix));
                    }
                }
					return true;
			}

			if (orig.mPersonId != newContactData.mPersonId) {
                if (V) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(Contact.class, "person id changed");
                    }
                }
				return true;
			}

			if (orig.mPresenceResId != newContactData.mPresenceResId) {
                if (V) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(Contact.class, "presence changed");
                    }
                }
				return true;
			}

			if (orig.mAvatarHashCode != newContactData.mAvatarHashCode) {
                if (V) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(Contact.class, "avatar changed");
                    }
                }
				return true;
			}

			return false;
		}

		private void updateContact(final Contact c, final Object cookie) {
			if (c == null) {
				return;
			}
           
			Contact entry = getContactInfo(c.mNumber);
			
			synchronized (c) {
				c.mLastInvalidatedTime = SystemClock.elapsedRealtime();
				
				if (entry == null) {
					//if entry was not returned then return
					c.mQueryPending = false;
					c.notifyAll();
					return;
				}
				
				if (contactChanged(c, entry)) {
					if (Logger.IS_DEBUG_ENABLED) {
						log("updateContact: contact changed for " + entry.mName);
					}
					
					c.mNumber = entry.mNumber;
					c.mLabel = entry.mLabel;
					c.mPersonId = entry.mPersonId;
					c.mPresenceResId = entry.mPresenceResId;
					c.mPresenceText = entry.mPresenceText;
					c.mContactPictureId = entry.mContactPictureId;
					//c.mAvatarData = entry.mAvatarData;
					c.mAvatarHashCode = entry.mAvatarHashCode;
					c.mAvatarCache = entry.mAvatarCache;
					c.mPrefix = entry.mPrefix;
					c.mChannelType = entry.mChannelType;
					// Check to see if this is the local ("me") number and update the name.
					if (MessageUtils.isLocalNumber(c.mNumber)) {
						c.mName = mContext.getString(R.string.me);
					} else {
						c.mName = entry.mName;
					}

					c.notSynchronizedUpdateNameAndNumber();

					// clone the list of listeners in case the onUpdate call turns around and
					// modifies the list of listeners
					// access to mListeners is synchronized on ContactsCache
					HashSet<UpdateListener> iterator;
					synchronized (mListeners) {
						iterator = (HashSet<UpdateListener>) Contact.mListeners.clone();
					}
					for (UpdateListener l : iterator) {
                        if (V) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(Contact.class, "updating " + l);
                            }
                        }
						l.onUpdate(c, cookie);
					}
				}
				c.mPrefix = entry.mPrefix;
				c.mChannelType = entry.mChannelType;
				synchronized (c) {
					c.mQueryPending = false;
					c.notifyAll();
				}
			}
		}

		/**
		 * Returns the caller info in Contact.
		 */
		public Contact getContactInfo(String numberOrEmail) {
			if (Mms.isEmailAddress(numberOrEmail)) {
				return getContactInfoForEmailAddress(numberOrEmail);
			} else {
				/*if (!OEM.isIceCreamSandwich) {
					return getContactInfoForPhoneNumber(numberOrEmail);
				} else */{
					//use lookup query
					return getContactInfoForPhoneNumberICS(numberOrEmail);
				}
			}
		}

		/**
		 * Queries the caller id info with the phone number.
		 * 
		 * @return a Contact containing the caller id info corresponding to the number.
		 */
		private Contact getContactInfoForPhoneNumber(String number) {
			boolean found = false;
			Contact entry = null;
			number = PhoneNumberUtils.stripSeparators(number);
			if (number.length() != 0) {
				entry = new Contact(number, null, ADDR_TYPE_NUMBER);
	
				// if (LOCAL_DEBUG) log("queryContactInfoByNumber: number=" + number);
	
				// We need to include the phone number in the selection string itself rather then
				// selection arguments, because SQLite needs to see the exact pattern of GLOB
				// to generate the correct query plan
	
				String selection = CALLER_ID_SELECTION.replace("+", number);
				
				Cursor cursor = SqliteWrapper.query(mContext, mResolver, PHONES_WITH_PRESENCE_URI, CALLER_ID_PROJECTION,
						selection, new String[] { number }, null);
				
				if (cursor == null) {
					Logger.warn(getClass(), "queryContactInfoByNumber(" + number + ") returned NULL cursor!" + " contact uri used "
							+ PHONES_WITH_PRESENCE_URI);
				}
				try {
				    if (cursor != null && cursor.moveToFirst()) {
				        do {
				            String numberoremail = cursor.getString(PHONE_NUMBER_COLUMN);
				            //consider this contact only if it matches properly
				            //else ignore it to avoid wrong contact name being displayed
				            	if (PhoneNumberUtils.compare(numberoremail, number)) {

				            	found = true;
				                synchronized (entry) {
				                    entry.mLabel = cursor.getString(PHONE_LABEL_COLUMN);
				                    entry.mName = cursor.getString(CONTACT_NAME_COLUMN);
				                    entry.mPersonId = cursor.getLong(CONTACT_ID_COLUMN);
				                    entry.mPresenceResId = getPresenceIconResourceId(cursor.getInt(CONTACT_PRESENCE_COLUMN));
				                    entry.mPresenceText = cursor.getString(CONTACT_STATUS_COLUMN);
				                    
				                    int type = cursor.getInt(PHONE_TYPE);
				                    
				                    entry.mPrefix = Phone.getTypeLabel(mContext.getResources(),
				                            type, entry.mLabel).toString();
				                    if (V) {
				                        log("queryContactInfoByNumber: name=" + entry.mName + ", number=" + number + ", presence="
				                                + entry.mPresenceResId);
				                    }
				                }
				                byte[] data = loadAvatarData(entry);
	
		                        synchronized (entry) {
		                        	if (data != null) {
		                        		entry.mAvatarHashCode = Arrays.hashCode(data);
		                        	}
		                            //entry.mAvatarData = data;
		                        }
		                        break;
				            	}
				           // }  
				        }while (cursor.moveToNext());
				    }
				}catch (Exception e) {
					Logger.debug("getContactInfoForPhoneNumber(" + number + ")", e);
				}
				
				if (cursor != null) {
					cursor.close();
				}
			} 

			if (!found && mUseRawContactQuery) {
				entry = getRawContactInfoForPhoneNumber(number);
			}
			
			return entry;
		}
		
		
		private Contact getContactInfoForPhoneNumberICS(String number) {
			boolean contactResolved = false;
			Contact entry = null;
			
			number = PhoneNumberUtils.stripSeparators(number);
			if (number.length() != 0) {
				entry = new Contact(number, null, ADDR_TYPE_NUMBER);
	
				Cursor cursor = null;
				
				try {
					Uri uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
					cursor = SqliteWrapper.query(mContext, mResolver, uri, CALLER_ID_PROJ_ICS,
							null, null, null);
					
				    if (cursor != null && cursor.moveToFirst()) {
				    	boolean contactFound = false;
				    	long contactId = 0;
				        do {
				            String numberoremail = cursor.getString(PHONE_NUMBER_COLUMN_ICS);
				            //consider this contact only if it matches properly
				            //else ignore it to avoid wrong contact name being displayed
				            if (PhoneNumberUtils.compare(numberoremail, number)) {
				            	contactResolved = true;
				            	
				            	contactId = cursor.getLong(CONTACT_ID_COLUMN_ICS);
				            	
				            	if (!contactFound) {
				            		synchronized (entry) {
				            			contactFound = true;
				            			entry.mLabel = cursor.getString(PHONE_LABEL_COLUMN_ICS);
				            			entry.mName = cursor.getString(CONTACT_NAME_COLUMN_ICS);
				            			entry.mPersonId = contactId;
				            			entry.mContactPictureId = contactId;
				            			int type = cursor.getInt(PHONE_TYPE_ICS);

				            			entry.mPrefix = Phone.getTypeLabel(mContext.getResources(),
				            					type, entry.mLabel).toString();
				            			if (V) {
				            				log("queryContactInfoByNumber: name=" + entry.mName + ", number=" + number + ", presence="
				            						+ entry.mPresenceResId);
				            			}
				            		}
				            	}
				                byte[] data = loadAvatarData(contactId);
	
		                        synchronized (entry) {
		                        	if (data != null) {
		                        		entry.mContactPictureId = contactId;
		                        		entry.mAvatarHashCode = Arrays.hashCode(data);
		                        	}
		                        }
		                        //iterate for the image
		                        if (data != null && contactFound) {
		                        	break;
		                        }
				            }
				        }while (cursor.moveToNext());
				    }
				}catch (Exception e) {
					Logger.debug("getContactInfoForPhoneNumber(" + number + ")", e);
				}
				
				if (cursor != null) {
					cursor.close();
				}
				
				if (!contactResolved && mUseRawContactQuery) {
					entry = getRawContactInfoForPhoneNumber(number);
				}
			} 

			return entry;
		}
		
		/**
		 * Queries the caller id info with the phone number using RawContactsEntity.CONTENT_URI.
		 * 
		 * @return a Contact containing the caller id info corresponding to the number.
		 */
		private Contact getRawContactInfoForPhoneNumber(String number) {
			number = PhoneNumberUtils.stripSeparators(number);
			Contact entry = new Contact(number, null, ADDR_TYPE_NUMBER);
			
			Cursor dataCursor = null;
			Cursor rawCursor = null;
			
			if (!OEM.isIceCreamSandwich) {
		    	try {
		    		//try the raw query
		    		String rawselection  = RAW_CALLER_ID_SELECTION;
		    		
		    		rawCursor = SqliteWrapper.queryOrThrow(mContext, mResolver, RawContactsEntity.CONTENT_URI, DEEP_ENTRY_PROJECTION,
		    				rawselection, new String[] {number, number}, "contact_id");
		    		
		    		if (rawCursor != null && rawCursor.moveToFirst()) {
		    			do {
		    				String numberoremail = rawCursor.getString(DEEP_DATA_COLUMN);
				            //consider this contact only if it matches properly
				            //else ignore it to avoid wrong contact name being displayed
				            if (PhoneNumberUtils.compare(numberoremail, number)) {
				            	//found the number use the contact_id to retrieve required information
				            	int type = rawCursor.getInt(DEEP_DATA_TYPE_COLUMN);
				            	long contactId = rawCursor.getLong(DEEP_CONTACT_ID_COLUMN);
				            	byte [] imageBytes = null;
				        		String name = null;
				            	
				            	Uri contentUri = Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, contactId + "/data");
				        		
				            	String unionSelect = " 1 ) union all select data1, mimetype, data15 from view_data where (contact_id=" + contactId +" AND ("+
				        		ContactsContract.Contacts.Data.MIMETYPE+" == '"+ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "' " +
				        				"OR " +  ContactsContract.Contacts.Data.MIMETYPE+" == '"+CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'))";
				        		
				        		dataCursor = SqliteWrapper.query(mContext, mResolver, contentUri,
				        				new String[]{ "data1", "mimetype", "data15" }, unionSelect + "/*", null, "*/");
				        		
				        		if (dataCursor != null && dataCursor.moveToFirst()) {
				        			do {
				        				String mimeType = dataCursor.getString(1);
				        				if (mimeType.equals(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
				        					name = dataCursor.getString(0);
				        				} else if (imageBytes == null && mimeType.equals(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)) {
				        					if (!dataCursor.isNull(2))
				        					{
				        						imageBytes = dataCursor.getBlob(2);
				        					}
				        				}
				        				
				        			} while (dataCursor.moveToNext());
				        			
				        			synchronized (entry) {
					                    entry.mLabel = null;
					                    entry.mName = name;
					                    entry.mPersonId = contactId;
					                    entry.mPresenceResId = getPresenceIconResourceId(StatusUpdates.OFFLINE);
					                    entry.mPresenceText = null;
					                    entry.mPrefix = Phone.getTypeLabel(mContext.getResources(),
					                            type, entry.mLabel).toString();
					                    
					                    //entry.mAvatarData = imageBytes;
					                    
					                    if (imageBytes != null) {
					                    	entry.mAvatarHashCode = Arrays.hashCode(imageBytes);
					                    }
					                    if (V) {
					                        log("queryContactInfoByNumber: name=" + entry.mName + ", number=" + number + ", presence="
					                                + entry.mPresenceResId);
					                    }
					                }
				        		}
				        		
				            	break;
				            }
		    			} while (rawCursor.moveToNext());
		    		}
		    	} catch (Exception e) {
		    		if (Logger.IS_DEBUG_ENABLED) {
		    			Logger.debug("getRawContactInfoForPhoneNumber(" + number + ") got an exception so avoid making raw queries", e);
		    		}
		    		//if this query results in an exception, then raw queries are not 
					//supported on this device so avoid using them
					//mUseRawContactQuery = false;
		    	}
		    }
			
			if (dataCursor != null) {
				dataCursor.close();
			}
			if (rawCursor != null) {
				rawCursor.close();
			}
			
			return entry;
		}

		/*
		 * Load the avatar data from the cursor into memory. Don't decode the data
		 * until someone calls for it (see getAvatar). Hang onto the raw data so that
		 * we can compare it when the data is reloaded.
		 * TODO: consider comparing a checksum so that we don't have to hang onto
		 * the raw bytes after the image is decoded.
		 */
		private byte[] loadAvatarData(Contact entry) {
			return loadAvatarData(entry.mPersonId);
		}
		
		
		private byte[] loadAvatarData(long contactId) {
			byte[] data = null;
			InputStream avatarDataStream = null;
			Cursor photoCursor = null;

			if (contactId == 0) {
				return null;
			}

			try {
				Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

				avatarDataStream = Contacts.openContactPhotoInputStream(mContext.getContentResolver(),
						contactUri);

				if (avatarDataStream != null) {
					data = new byte[avatarDataStream.available()];
					avatarDataStream.read(data, 0, data.length);
				}
				
				
				/*
				 * ... got null. This means there was no photo, or it was a Facebook contact, try the raw query to get the contact.
				 * this raw query is not supported on Ice Cream Sandwich, so skip if that OS is the case.
				 */
				if (data == null && !OEM.isIceCreamSandwich) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug("loadAvatarData no picture found trying query to get fb contact image");
					}
					// Create a URI request from the content table
					Uri contentUri = Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, contactId + "/data");
					String unionSelect = " 1 ) union all select data15 from view_data where (contact_id=" + contactId +" AND "+
							ContactsContract.Contacts.Data.MIMETYPE+" == '"+ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "' )";

					photoCursor = SqliteWrapper.query(mContext, mResolver, contentUri,
							new String[]{ "data15" }, unionSelect + "/*", null, "*/");

					if(photoCursor != null) {
						while(data == null && photoCursor.moveToNext()) {
							if (!photoCursor.isNull(0)) {
								data = photoCursor.getBlob(0);
								break;
							}
						}
					}
				}
			} catch (Exception ex) {
				if (Logger.IS_DEBUG_ENABLED) {
	    			Logger.debug("loadAvatarData " + contactId, ex);
				}
			} finally {
				if (photoCursor != null) {
					photoCursor.close();
				}
				try {
					if (avatarDataStream != null) {
						avatarDataStream.close();
					}
				} catch (IOException e) {
				}
			}
			
			return data;
		}
		
		
		private int getPresenceIconResourceId(int presence) {
			// TODO: must fix for SDK
			if (presence != StatusUpdates.OFFLINE) {
				return StatusUpdates.getPresenceIconResourceId(presence);
			}

			return 0;
		}

		/**
		 * Query the contact email table to get the name of an email address.
		 */
		private Contact getContactInfoForEmailAddress(String email) {
			Contact entry = new Contact(email, null, ADDR_TYPE_EMAIL);
			boolean found = false;
			String upperEmail = email.toUpperCase();
			Cursor cursor = SqliteWrapper.query(mContext, mResolver, EMAIL_WITH_PRESENCE_URI,
					EMAIL_PROJECTION, EMAIL_SELECTION, new String[] { upperEmail }, null);

			if (cursor != null) {
				try {
					while (cursor.moveToNext()) {
						found = false;

						synchronized (entry) {
							entry.mPresenceResId = getPresenceIconResourceId(cursor.getInt(EMAIL_STATUS_COLUMN));
							entry.mPersonId = cursor.getLong(EMAIL_ID_COLUMN);

							String name = cursor.getString(EMAIL_NAME_COLUMN);
							if (TextUtils.isEmpty(name)) {
								name = cursor.getString(EMAIL_CONTACT_NAME_COLUMN);
							}
							if (!TextUtils.isEmpty(name)) {
								entry.mName = name;
								if (V) {
									log("getContactInfoForEmailAddress: name=" + entry.mName + ", email=" + email
											+ ", presence=" + entry.mPresenceResId);
								}
								found = true;
							}
						}

						if (found) {
							byte[] data = loadAvatarData(entry);
							synchronized (entry) {
								if (data != null) {
									entry.mAvatarHashCode = Arrays.hashCode(data);
								}
								//entry.mAvatarData = data;
							}

							break;
						}
					}
				} catch (Exception e) {
					
				}
				
				cursor.close();
				
				if (!found && mUseRawContactQuery) {
					entry = getRawContactInfoForEmailAddress(email);
				}
				
			}
			return entry;
		}
		
		/**
		 * Query the contact email table to get the name of an email address.
		 */
		private Contact getRawContactInfoForEmailAddress(String email) {
			Contact entry = new Contact(email, null, ADDR_TYPE_EMAIL);
			
			Cursor dataCursor = null;
			Cursor rawCursor = null;
			
			if (!OEM.isIceCreamSandwich) {
				String rawselection  = RAW_EMAIL_SELECTION;
				
				try {
					String upperEmail = email.toUpperCase();
					rawCursor = SqliteWrapper.query(mContext, mResolver, RawContactsEntity.CONTENT_URI, DEEP_ENTRY_PROJECTION,
							rawselection, new String[] {upperEmail, upperEmail}, "contact_id");

					if (rawCursor != null && rawCursor.moveToFirst()) {
						//found the contact_id of the email address now query to fetch 
						//required information
						long contactId = rawCursor.getLong(DEEP_CONTACT_ID_COLUMN);
						byte [] imageBytes = null;
						String name = null;

						Uri contentUri = Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, contactId + "/data");

						String unionSelect = " 1 ) union all select data1, mimetype, data15 from view_data where (contact_id=" + contactId +" AND ("+
								ContactsContract.Contacts.Data.MIMETYPE+" == '"+ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "' " +
								"OR " +  ContactsContract.Contacts.Data.MIMETYPE+" == '"+CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'))";

						dataCursor = SqliteWrapper.queryOrThrow(mContext, mResolver, contentUri,
								new String[]{ "data1", "mimetype", "data15" }, unionSelect + "/*", null, "*/");
						if (dataCursor.moveToFirst()) {
							do {
								String mimeType = dataCursor.getString(1);
								if (mimeType.equals(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
									name = dataCursor.getString(0);
								} else if (imageBytes == null && mimeType.equals(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)) {
									if (!dataCursor.isNull(2))
									{
										imageBytes = dataCursor.getBlob(2);
									}
								}

							} while (dataCursor.moveToNext());

							synchronized (entry) {
								entry.mLabel = null;
								entry.mName = name;
								entry.mPersonId = contactId;
								entry.mPresenceResId = getPresenceIconResourceId(StatusUpdates.OFFLINE);
								entry.mPresenceText = null;
								if (imageBytes != null) {
									entry.mAvatarHashCode = Arrays.hashCode(imageBytes);
								}
								//entry.mAvatarData = imageBytes;
								if (V) {
									log("queryContactInfoByNumber: name=" + entry.mName + ", number=" + email + ", presence="
											+ entry.mPresenceResId);
								}
							}
						}
					}
				} catch (Exception e) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug("getRawContactInfoForEmailAddress(" + email + ") got an exception avoid making raw queries", e);
					}
					//if this query results in an exception, then raw queries are not 
					//supported on this device so avoid using them
					//mUseRawContactQuery = false;
		    	}

			}
			if (rawCursor != null) {
				rawCursor.close();
			}
			if (dataCursor != null) {
				dataCursor.close();
			}
			
			return entry;
		}

		// Invert and truncate to five characters the phoneNumber so that we
		// can use it as the key in a hashtable. We keep a mapping of this
		// key to a list of all contacts which have the same key.
		private String key(String phoneNumber, CharBuffer keyBuffer) {
			keyBuffer.clear();
			keyBuffer.mark();

			int position = phoneNumber.length();
			int resultCount = 0;
			while (--position >= 0) {
				char c = phoneNumber.charAt(position);
				if (Character.isDigit(c)) {
					keyBuffer.put(c);
					if (++resultCount == STATIC_KEY_BUFFER_MAXIMUM_LENGTH) {
						break;
					}
				}
			}
			keyBuffer.reset();
			if (resultCount > 0) {
				return keyBuffer.toString();
			} else {
				// there were no usable digits in the input phoneNumber
				return phoneNumber;
			}
		}

		// Reuse this so we don't have to allocate each time we go through this
		// "get" function.
		static final int STATIC_KEY_BUFFER_MAXIMUM_LENGTH = 5;
		static CharBuffer sStaticKeyBuffer = CharBuffer.allocate(STATIC_KEY_BUFFER_MAXIMUM_LENGTH);

		public Contact get(String numberOrEmail) {
			synchronized (ContactsCache.this) {
				// See if we can find "number" in the hashtable.
				// If so, just return the result.
				final boolean isNotRegularPhoneNumber = Mms.isEmailAddress(numberOrEmail)
						|| MessageUtils.isAlias(numberOrEmail);
				final String key = isNotRegularPhoneNumber ? numberOrEmail : key(numberOrEmail, sStaticKeyBuffer);

				ArrayList<Contact> candidates = mContactsHash.get(key);
				if (candidates != null) {
					int length = candidates.size();
					for (int i = 0; i < length; i++) {
						Contact c = candidates.get(i);
						if (isNotRegularPhoneNumber) {
							if (numberOrEmail.equals(c.mNumber)) {
								return c;
							}
						} else {
							if (PhoneNumberUtils.compare(numberOrEmail, c.mNumber)) {
								return c;
							}
						}
					}
				} else {
					candidates = new ArrayList<Contact>();
					// call toString() since it may be the static CharBuffer
					mContactsHash.put(key, candidates);
				}
				Contact c = new Contact(numberOrEmail);
				candidates.add(c);
				return c;
			}
		}

		void invalidate() {
			// Don't remove the contacts. Just mark them stale so we'll update their
			// info, particularly their presence.
			synchronized (ContactsCache.this) {
				for (ArrayList<Contact> alc : mContactsHash.values()) {
					for (Contact c : alc) {
						synchronized (c) {
							c.mIsStale = true;
						}
					}
				}
			}
		}
	}

	private static void log(String msg) {

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(Contact.class, msg);
        }
    }

    public synchronized long getContactId() {
       
        return mPersonId;
    }

    /**
     * This Method compares two numbers by removing the seperators in them if it is a phonenumber
     * @param number2
     * @return
     */
    public static boolean equalsIgnoreSeperators(String number1, String number2) {
        String stripedNumber1 = number1;
        String stripedNumber2 = number2;
        
        if (number1 == null || number2 == null) {
            return false;
        }
        
        if (!Mms.isEmailAddress(stripedNumber1) && !Mms.isEmailAddress(stripedNumber2)) {
            stripedNumber1 = PhoneNumberUtils.stripSeparators(number1);
            stripedNumber2 = PhoneNumberUtils.stripSeparators(number2);
            return PhoneNumberUtils.compare(stripedNumber1,stripedNumber2);
            
        } else if(Mms.isEmailAddress(stripedNumber1) && Mms.isEmailAddress(stripedNumber2)) {
           	return stripedNumber1.trim().equalsIgnoreCase(stripedNumber2.trim());
        }
        return false;
       
        
    }
    
    public static Comparator<Contact> getComparator() {
        if (mNumberComparator == null) {
            mNumberComparator = new Comparator<Contact>() {
                @Override
                public int compare(Contact lhs, Contact rhs) {
                    String lhsNumber = lhs.getNumber();
                    if (!lhs.isEmail()) {
                        lhsNumber = PhoneNumberUtils.stripSeparators(lhsNumber);
                        //there are chances where tablet has the country code appended to a number
                        //but handset wont have the country code so lets compare last 10 digits
                        if (lhsNumber.length() > 10) {
                        	lhsNumber = lhsNumber.substring(lhsNumber.length() - 10);
                        }
                    }
                    
                    String rhsNumber = rhs.getNumber();
                    if (!rhs.isEmail()) {
                        rhsNumber = PhoneNumberUtils.stripSeparators(rhsNumber);
                        //there are chances where tablet has the country code appended to a number
                        //but handset wont have the country code so lets compare last 10 digits
                        if (rhsNumber.length() > 10) {
                        	rhsNumber = rhsNumber.substring(rhsNumber.length() - 10);
                        }
                    }
                    
                    return lhsNumber.compareToIgnoreCase(rhsNumber);
                }
            };
        }
        
        return mNumberComparator;
    }
}
