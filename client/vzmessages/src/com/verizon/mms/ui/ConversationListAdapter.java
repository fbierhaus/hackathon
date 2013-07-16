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

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.data.Conversation.MessageData;
import com.verizon.mms.data.WorkingMessage;
import com.verizon.mms.ui.ListDataWorker.ListDataJob;
import com.verizon.mms.ui.adapter.FastCursorAdapter;
import com.verizon.mms.ui.widget.ResizeAnimation;
import com.verizon.mms.util.AvatarClickListener;
import com.verizon.mms.util.DraftCache;
import com.verizon.mms.util.MemoryCacheMap;


/**
 * The back-end data adapter for ConversationList.
 */
public class ConversationListAdapter extends FastCursorAdapter {
	private Context context;
	private ListView listView;
	private final LayoutInflater mFactory;
	private final DraftCache draftCache;
	private static Drawable defaultImage;
	private static Drawable[] defaultImages;
	private static Drawable errorIcon;
	//private final Drawable pendingIcon;
	private static Drawable imageIcon;
	private static Drawable mediaIcon;
	private static Drawable locationIcon;
	private static Drawable vCardIcon;
	private static Drawable audioIcon;
	   //icones for selected;
    private static Drawable imageIconSelected;
    private static Drawable mediaIconSelected;
    private static Drawable locationIconSelected;
    private static Drawable vCardIconSelected;
    private static Drawable audioIconSelected;
    private static Drawable errorIconSelected;
    
	private static String loading;
	private OnContentChangedListener mOnContentChangedListener;
	private MemoryCacheMap<String, ContactDetails> contactDetailsCache;
	
	private MemoryCacheMap<Long, Integer> unreadCache;
	private MemoryCacheMap<Long, MessageData> lastMessageDataCache;
	
	private static MemoryCacheMap<String, ContactDetails> oldContactDetailsCache;
	private static MemoryCacheMap<Long, Integer> oldUnreadCache;
	private static MemoryCacheMap<Long, MessageData> oldLastMessageDataCache;
	private static int lastCacheSize;
	private ListDataWorker lastDataWorker;
	private ListDataWorker contactDetailWorker;
	private int numViews;
	private int queueSize = MIN_QUEUE_SIZE;
	private static Typeface fromType;
	private boolean batchMode = false;
	private BatchModeThreadsController batchThreads;
    private long selectedThreadID = INVALID_THREAD;
    private long lastTopId = -1;
	private int animatedPos = -1;
	private boolean animating;
    private static int listSelectedTextColour;
    private static int listUnselectedFromColor;
    private static int listUnselectedSubjectColor;
    private static int listUnselectedDateColor;
    
	private static final float VIEW_QUEUE_FACTOR = 2.5f;  // size of queue relative to number of views
	private static final int MIN_VIEWS = 10;
	private static final int MIN_QUEUE_SIZE = (int)(MIN_VIEWS * VIEW_QUEUE_FACTOR);

	private static final float CACHE_PERCENT = 0.1f;
	private static final int MAX_CACHE_SIZE = 200;
	private static final int MIN_CACHE_SIZE = MIN_QUEUE_SIZE * 3;

	private static final int QUEUE_CONTACT_DETAILS = 0;
	private static final int QUEUE_LAST_MESSAGE_DATA = 0;
	private static final int QUEUE_UNREAD = 1;
	
	private static final int MSG_CONTACT_DETAILS = 1;
	private static final int MSG_CONTACT_NAMES = 2;
	private static final int MSG_UNREAD = 3;
	private static final int MSG_LAST_MESSAGE_DATA = 4;

	private static final long INVALID_THREAD = -90;

	//flag that says if we are in process of deleting more than one thread
	private boolean mDeleteInProgress = false;
	
	private static class ViewData {
		private View unreadBox;
		private TextView unreadNumber;
		private TextView subject;
		private FromTextView from;
		private TextView draft;
		private TextView date;
		private ImageView icon;
		private ContactImage avatar;
		private CheckBox checkBox;
		private boolean hasError;
		private long threadId;
		private boolean subjectLoading;
		private AvatarClickListener avatarClickListener;
		public CharSequence snippet;
		public ViewAnimation anim;

		public String toString() {
			return "tid " + threadId + ": " + date.getText() + " error = " + hasError + ", draft = " +
				TextUtils.isEmpty(draft.getText()) + ", from = " + from.getText() + ", subject = " + subject.getText();
		}
	}


	static class ContactNameData {
		String names;
		int num;

		ContactNameData(String names, int num) {
			this.names = names;
			this.num = num;
		}
	}


	static class ContactDetails {
		ContactNameData contactNameData;
		Drawable[] images;
		ContactList contactList;
		int pos;
		
		public ContactDetails (String names, int num, Drawable[] images, ContactList contactList, int pos) {
			contactNameData = new ContactNameData(names, num);
			this.images = images;
			this.contactList = contactList;
			this.pos = pos;
		}
	}


	private class ViewAnimation {
		private View view;
		private Animation anim;

		private static final long ANIM_BUBBLE_DURATION = 1000;
		private static final long ANIM_DONE_DELAY = ANIM_BUBBLE_DURATION + 500;


		private ViewAnimation(View view) {
			this.view = view;
		}

		private void clearAnimation() {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(ConversationListAdapter.this + ".clearAnimation: view = " + view);
			}
			anim.setAnimationListener(null);
			view.clearAnimation();
			animating = false;
			animatedPos = -1;
		}

		public void startAnimation() {
			// set top-down stretch and fade in
			final AnimationSet anim = new AnimationSet(false);
			anim.setDuration(ANIM_BUBBLE_DURATION);

			anim.addAnimation(new ResizeAnimation(view, -1, 0));

			final AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
			alpha.setInterpolator(new AccelerateInterpolator(1.0f));
			anim.addAnimation(alpha);

			anim.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(ConversationListAdapter.this + ".onAnimationStart: view = " + view);
					}
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(ConversationListAdapter.this + ".onAnimationEnd: view = " + view);
					}
					clearAnimation();
					handler.removeMessages(0);
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}
			});

			// make sure the view's animation gets cleared after a delay to allow for messages that
			// are never animated (e.g. because they aren't visible on the screen)
			//
			handler.sendEmptyMessageDelayed(0, ANIM_DONE_DELAY);

			this.anim = anim;
        	animating = true;
        	view.getLayoutParams().height = 0;
			view.startAnimation(anim);
		}

		private Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(ConversationListAdapter.this + ".handleMessage: view = " + view);
				}
				clearAnimation();
			}
		};
	}


	public ConversationListAdapter(Context context, ListView listView, Cursor cursor, 
	        BatchModeThreadsController batchThreads) {
		super(context, cursor, false);
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".init");
		}
		this.context = context;
		this.listView = listView;
		this.batchThreads = batchThreads;
		setChangeListener(changeListener);
		mFactory = LayoutInflater.from(context);
		draftCache = DraftCache.getInstance();

		//pendingIcon = res.getDrawable(R.drawable.ic_email_pending);
		if (defaultImage == null) {
			final Resources res = context.getResources();
			
			defaultImage = res.getDrawable(R.drawable.ic_contact_picture);
			defaultImages = new Drawable[] { defaultImage };
			errorIcon = res.getDrawable(R.drawable.ico_error);
			imageIcon = res.getDrawable(R.drawable.ico_image);
			mediaIcon = res.getDrawable(R.drawable.ico_media);
			locationIcon = res.getDrawable(R.drawable.ico_location);
			audioIcon = res.getDrawable(R.drawable.ico_audio);
			vCardIcon = res.getDrawable(R.drawable.ico_vcard);
			//selected resources selected
			imageIconSelected = res.getDrawable(R.drawable.ico_picture_selected);
			mediaIconSelected = res.getDrawable(R.drawable.ico_media_selected);
			locationIconSelected = res.getDrawable(R.drawable.ico_pin_selected);
			audioIconSelected = res.getDrawable(R.drawable.ico_audio_selected);
			vCardIconSelected = res.getDrawable(R.drawable.ico_vcard_selected);
			errorIconSelected = res.getDrawable(R.drawable.ico_error_selected);
			
			listSelectedTextColour = res.getColor(R.color.text_color_list_selected);
	        listUnselectedFromColor = res.getColor(R.drawable.text_color_black);
	        listUnselectedDateColor = res.getColor(R.drawable.text_date);
	        listUnselectedSubjectColor = res.getColor(R.drawable.text_subject);
	        
			loading = res.getString(R.string.loading);
		}
		
		createCaches(cursor);
		createQueues();
	}

	private void createCaches(Cursor cursor) {
		// scale cache size to number of items
		if (cursor != null) {
			int size = (int)(cursor.getCount() * CACHE_PERCENT);
			if (size > MAX_CACHE_SIZE) {
				size = MAX_CACHE_SIZE;
			}
			else if (size < MIN_CACHE_SIZE) {
				size = MIN_CACHE_SIZE;
			}
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".createCaches: count = " + cursor.getCount() +
					", size = " + size + ", last = " + lastCacheSize +
					", prev = " + (unreadCache == null ? -1 : unreadCache.size()) +
					", old = " + (oldUnreadCache == null ? -1 : oldUnreadCache.size()));
			}

			// init the static caches
			if (oldUnreadCache == null) {
				oldUnreadCache = new MemoryCacheMap<Long, Integer>(size);
				oldLastMessageDataCache = new MemoryCacheMap<Long, MessageData>(size);
				oldContactDetailsCache = new MemoryCacheMap<String, ContactDetails>(size);
			}
			else if (size > lastCacheSize) {
				oldUnreadCache.setCacheSize(size);
				oldLastMessageDataCache.setCacheSize(size);
				oldContactDetailsCache.setCacheSize(size);
			}
			lastCacheSize = size;

			// init the current caches
			unreadCache = new MemoryCacheMap<Long, Integer>(size);
			lastMessageDataCache = new MemoryCacheMap<Long, MessageData>(size);
			contactDetailsCache = new MemoryCacheMap<String, ContactDetails>(size);
		}
	}

	private void createQueues() {
		lastDataWorker = new ListDataWorker();
		lastDataWorker.addQueue(handler, QUEUE_LAST_MESSAGE_DATA, MSG_LAST_MESSAGE_DATA, MIN_QUEUE_SIZE, null);
		lastDataWorker.addQueue(handler, QUEUE_UNREAD, MSG_UNREAD, MIN_QUEUE_SIZE, null);
		lastDataWorker.start();
		
		contactDetailWorker = new ListDataWorker();
		contactDetailWorker.addQueue(handler, QUEUE_CONTACT_DETAILS, MSG_CONTACT_DETAILS, MIN_QUEUE_SIZE, null);
		contactDetailWorker.start();
	}

	private void resizeQueues(int newQueueSize) {
		queueSize = newQueueSize;
		lastDataWorker.resizeQueues(newQueueSize);
		contactDetailWorker.resizeQueues(newQueueSize);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final ViewData data = (ViewData)view.getTag();
		final int pos = cursor.getPosition();
		final String recipients = cursor.getString(Conversation.RECIPIENT_IDS);
		
		final ContactDetails contactDetails = getContactDetails(recipients, pos);
		
		data.from.setNames(contactDetails.contactNameData);

		final long threadId = data.threadId = cursor.getLong(Conversation.ID);
		final boolean hasDraft = threadId > 0 && draftCache.hasDraft(threadId);
		data.draft.setVisibility(hasDraft ? View.VISIBLE : View.GONE);
		data.draft.setText(R.string.has_draft);

		final long date = cursor.getLong(Conversation.DATE);
		data.date.setText(MessageUtils.formatTimeStampString(date, false));

		data.snippet = getSnippet(cursor);
		
		//fetch the message data only if it is a group message
		if (contactDetails != null && contactDetails.contactList.size() > 1) {
			MessageData msgData = getLastMessageData(pos, threadId);

			data.subject.setText(getSubject(msgData, data, pos));
		} else {
			data.subject.setText(data.snippet);
		}

		final ImageView icon = (ImageView)data.icon;
		Drawable iconImage = null;
		final boolean hasError = data.hasError = cursor.getInt(Conversation.ERROR) != 0;
        final boolean isSelected = (threadId == selectedThreadID);
		if (hasError) {
            iconImage = isSelected ? errorIconSelected : errorIcon;
		}
		/*else if (getMessageQueuedStatus(pos, threadId)) {
			iconImage = pendingIcon;
		}*/
		/*else {
			final int attType = getAttachmentType(msgData, pos, threadId);
			if (attType != WorkingMessage.NONE) {
				iconImage = getAttachmentIcon(attType, isSelected);
			}
			else {
				iconImage = null;
			}
		}*/
		if (iconImage != null) {
			icon.setImageDrawable(iconImage);
			icon.setVisibility(View.VISIBLE);
		}
		else {
			icon.setVisibility(View.GONE);
		}

		final ContactImage avatar = data.avatar;
		avatar.setImages(getContactImages(pos, recipients, contactDetails), defaultImage);
		data.avatarClickListener.setRecipients(recipients);
		avatar.setOnClickListener(data.avatarClickListener);

        if (isSelected) {
            putUnread(threadId, 0);
            setUnread(data, 0);
        } else {
            setUnread(data, getUnread(pos, cursor, threadId));
        }
        
		if (batchMode) {
			final CheckBox checkBox = data.checkBox;
			checkBox.setVisibility(View.VISIBLE);
			checkBox.setTag(threadId);
			final ArrayList<Long> threads = batchThreads.getBatchedThreads();
			checkBox.setChecked(threads.contains(threadId));
		}
		else {
			data.checkBox.setVisibility(View.GONE);
		}

        if (isSelected) {
            view.setBackgroundResource(R.drawable.conversation_item_background_open);
            data.subject.setTextColor(listSelectedTextColour);
            data.from.setTextColor(listSelectedTextColour);
            data.date.setTextColor(listSelectedTextColour);
        } else if (selectedThreadID != INVALID_THREAD) {  // don't change bg for handsets
            view.setBackgroundResource(R.drawable.conversation_item_background_shaded);
            data.subject.setTextColor(listUnselectedSubjectColor);
            data.from.setTextColor(listUnselectedFromColor);
            data.date.setTextColor(listUnselectedDateColor);
        }

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".bindView: pos " + pos + ": animatedPos = " + animatedPos +
				", animating = " + animating + ", view = " + view + ", data = " + data);
		}

//		if (pos == animatedPos) {
//			if (view.getAnimation() == null) {
//				final ViewAnimation anim = data.anim = new ViewAnimation(view);
//				anim.startAnimation();
//			}
//        }
//		else {
//			if (data.anim != null) {
//				data.anim.clearAnimation();
//				data.anim = null;
//			}
//			else {
//				view.clearAnimation();
//			}
//		}
	}

	public void toggleCheckBox(View view) {
        final Object tag = view.getTag();
	    if (tag instanceof ViewData) {
	        ((ViewData)tag).checkBox.toggle();
	    }
	}

    public void setSelected(long threadID) {
        selectedThreadID = threadID;
    }
    
    @Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		// ensure we have enough queue space for the number of views
		numViews = listView.getChildCount() + 1;
		final int minQueueSize = (int)(numViews * VIEW_QUEUE_FACTOR);
		if (minQueueSize > queueSize) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".newView: numViews = " + numViews + ", queueSize = " + queueSize);
			}
			resizeQueues(minQueueSize);
		}

		final View view = mFactory.inflate(R.layout.conversation_list_item, parent, false);
		final ViewData data = new ViewData();
		data.from = (FromTextView)findView(view, R.id.from);
		data.unreadBox = findView(view, R.id.mybox);
		data.unreadNumber = (TextView)findView(data.unreadBox, R.id.unreadcount);
		data.subject = (TextView)findView(view, R.id.subject);
		data.draft = (TextView)findView(view, R.id.draft);
		data.date = (TextView)findView(view, R.id.date);
		data.icon = (ImageView)findView(view, R.id.icon);
		data.avatar = (ContactImage)findView(view, R.id.avatar);
		if (fromType == null) {
			fromType = data.from.getTypeface();
		}
		data.checkBox = (CheckBox)findView(view, R.id.checkBox);
		data.checkBox.setOnCheckedChangeListener(checkedChangeListener);
		
		data.avatarClickListener = new AvatarClickListener(mContext, null);
		view.setTag(data);
		return view;
	}

	private View findView(View view, int id) {
		final View ret = view.findViewById(id);
		if (ret == null) {
			throw new RuntimeException("Missing view " + Integer.toHexString(id));
		}
		return ret;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		if (lastDataWorker != null) {
			lastDataWorker.exit();
			lastDataWorker = null;
		}
		if (contactDetailWorker != null) {
			contactDetailWorker.exit();
			contactDetailWorker = null;
		}
	}
	
	private void setUnread(final ViewData data, final Integer unread) {
		if (unread != null && unread != 0) {
			data.unreadNumber.setText(Integer.toString(unread));
			data.unreadBox.setVisibility(View.VISIBLE);
			data.from.setTypeface(fromType, Typeface.BOLD);
		}
		else {
			data.unreadBox.setVisibility(View.GONE);
			data.from.setTypeface(fromType, Typeface.NORMAL);
		}
	}

	private ContactDetails getContactDetails(String ids, int pos) {
		ContactDetails contactDetails = null;
		ContactDetails oldContactDetails = null;
		synchronized (contactDetailsCache) {
			contactDetails = contactDetailsCache.get(ids);
			oldContactDetails = oldContactDetailsCache.get(ids);
		}

		if (contactDetails == null) {
			ContactList contacts = ContactList.getByIds(ids, false, ids, false);
			int num = contacts.size();
			String names;
			
			if (oldContactDetails != null) {
				ContactList oldContactList = oldContactDetails.contactList;
				num = oldContactDetails.contactNameData.num;
				if (num == 1) {
					names = oldContactList.get(0).getName();
				}
				else if (num > 1) {
					names = oldContactList.formatNames();
				}
				else {
					names = "";
					Logger.error(this + ".fetchContactNames: no contacts for " + ids);
				}
			} else {
				if (num == 1) {
					names = contacts.get(0).getName();
				}
				else if (num > 1) {
					names = contacts.formatNames();
				}
				else {
					names = "";
					Logger.error(this + ".fetchContactNames: no contacts for " + ids);
				}
			}

			/*Drawable[] images = contacts.getImages(context, defaultImage, false);
			if (images == null) {
				images = defaultImages;
			}*/

			contactDetails = new ContactDetails(names, num, null, contacts, pos);
			synchronized (contactDetailsCache){
				contactDetailsCache.put(ids, contactDetails);
			}
		}
		return contactDetails;
	}
	
	private CharSequence getSnippet(Cursor cursor) {
		CharSequence snippet = MessageUtils.extractEncStrFromCursor(cursor, Conversation.SNIPPET, Conversation.SNIPPET_CS);
		if (snippet.length() != 0) {
			// parse emoticons
			snippet = MessageUtils.parseEmoticons(snippet);
		}
		else {
			snippet = "";
		}
		return snippet;
	}

	private CharSequence getSubject(MessageData data, ViewData vdata, int pos) {
		CharSequence subject = null;

		boolean hasData = data != null;
		if (hasData) {
			subject = data.subject;
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".getSubject: pos " + pos + ": got from data: " + subject);
			}
		}
		if (subject == null || subject.length() == 0) {
			// try the snippet from the cursor
			subject = vdata.snippet;
			if (subject != null) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(this + ".getSubject: pos " + pos + ": got from snippet: " + subject);
				}
			}
			else {
				// let the empty subject stand if we have the last data
				subject = hasData ? "" : null;
			}
		}

		// if we dint find any set it to "" and wait for data to come in
		if (!hasData && subject == null) {
			subject = "";
			vdata.subjectLoading = true;
		}

		return subject;
	}

	private int getAttachmentType(MessageData data, int pos, long threadId) {
		return data != null ? data.attType : WorkingMessage.NONE;
	}

	private MessageData getLastMessageData(int pos, long threadId) {
		synchronized (lastMessageDataCache) {
			MessageData data = lastMessageDataCache.get(threadId);
			if (data == null) {
				lastDataWorker.request(QUEUE_LAST_MESSAGE_DATA, pos, lastMessageDataJob, threadId);

				// if the old cache has an entry temporarily update the new one
				if (data == null) {
					data = oldLastMessageDataCache.get(threadId);
					if (data != null) {
						lastMessageDataCache.put(threadId, data);
					}
				}
			}
			return data;
		}
	}

	private ListDataJob lastMessageDataJob = new ListDataJob() {
		public Object run(int pos, Object jobData) {
			final long threadId = (Long)jobData;
			final MessageData data = Conversation.getLastMessageData(context, threadId);
			synchronized (lastMessageDataCache) {
				lastMessageDataCache.put(threadId, data);
				oldLastMessageDataCache.put(threadId, data);
			}
			return data;
		}
	};
	
	   private void putUnread( long threadId, Integer count) {
	        synchronized (unreadCache) {
	            // try the local cache
	            unreadCache.put(threadId, count);
	
	        }
	    }

	private Integer getUnread(int pos, Cursor cursor, long threadId) {
		synchronized (unreadCache) {
			Integer unread = 0;

			// try the local cache
			unread = unreadCache.get(threadId);
			if (unread == null) {
				boolean hasUnread = true;
				//fetch the unread count every time since there seems to be an bug in the 
				//threads table which on some occassion does not update the read flag properly
				//cursor.getInt(Conversation.READ) == 0;
				//query for unread only if there is any or if it is a tablet 
				if (hasUnread || MmsConfig.isTabletDevice()) {
					lastDataWorker.request(QUEUE_UNREAD, pos, unreadJob, threadId);
					// if the old cache has an entry temporarily update the new one
					unread = oldUnreadCache.get(threadId);
					if (unread != null) {
						unreadCache.put(threadId, unread);
					}
				} else {
					unreadCache.put(threadId, 0);
					oldUnreadCache.put(threadId, 0);
				}
			}
			return unread;
		}
	}

	private ListDataJob unreadJob = new ListDataJob() {
		public Object run(int pos, Object data) {
			final long threadId = (Long)data;
			final Integer unread = Integer.valueOf(Conversation.getUnread(context, threadId));
			synchronized (unreadCache) {
				unreadCache.put(threadId, unread);
				oldUnreadCache.put(threadId, unread);
			}
			return unread;
		}
	};

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			// update the item if it is visible
			final ListView listView = ConversationListAdapter.this.listView;
			final int first = listView.getFirstVisiblePosition();
			final int last = listView.getLastVisiblePosition();
			final int pos = msg.arg1;

			if (pos >= first && pos <= last) {
				try {
					final View view = listView.getChildAt(pos - first);
					final ViewData data = (ViewData)view.getTag();

					switch (msg.what) {
					case MSG_CONTACT_NAMES:
						final ContactNameData names = (ContactNameData)msg.obj;
						data.from.setNames(names);
						break;

					case MSG_UNREAD:
						final Integer unread = (Integer)msg.obj;
						setUnread(data, unread);
						break;

					case MSG_LAST_MESSAGE_DATA:
						final MessageData mdata = (MessageData)msg.obj;

						// cache and set the subject if we got one from the last message, or the subject
						// is currently "Loading...", or we have no subject from the thread's snippet
						//
						final CharSequence subject = mdata.subject;
						final boolean hasSubject = subject != null && subject.length() != 0;

						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(ConversationListAdapter.this + ".handler: pos " + pos +
									", tid " + data.threadId + ": loading = " + data.subjectLoading +
									", snippet = " + data.snippet + ", subject = " + subject);
						}

						if (hasSubject || data.subjectLoading || data.snippet == null) {
							data.subject.setText(subject);
							data.subjectLoading = false;
						}
						Drawable image = null;
						// check if we should set the icon
						if (data.hasError) {
							image = (data.threadId == selectedThreadID) ? errorIconSelected : errorIcon;
						}
						if (image != null) {
							final ImageView icon = data.icon;
							icon.setVisibility(View.VISIBLE);
							icon.setImageDrawable(image);
						}
						break;

					case MSG_CONTACT_DETAILS:
						final ContactDetails contactDetails = (ContactDetails)msg.obj;
						final ContactNameData contactNameData = contactDetails.contactNameData;
						final Drawable[] contactImages = contactDetails.images;
						
						data.from.setNames(contactNameData);
						data.avatar.setImages(contactImages, defaultImage);
					}
				}
				catch (Exception e) {
					Logger.error(e);
				}
			}
		}
	};

	/**
     * Returns the attachment icon based on the type of attachment.
     */
    private Drawable getAttachmentIcon(int attachmentType, boolean isSelected) {
        final Drawable iconImage;

        switch (attachmentType) {
        case WorkingMessage.IMAGE:
        case WorkingMessage.SLIDESHOW:
            iconImage = isSelected ? imageIconSelected : imageIcon;
            break;

        case WorkingMessage.VIDEO:
            iconImage = isSelected ? mediaIconSelected : mediaIcon;
            break;

        case WorkingMessage.AUDIO:
            iconImage = isSelected ? audioIconSelected : audioIcon;
            break;

        case WorkingMessage.LOCATION:
            iconImage = isSelected ? locationIconSelected : locationIcon;
            break;

        case WorkingMessage.VCARD:
            iconImage = isSelected ? vCardIconSelected : vCardIcon;
            break;

        default:
            iconImage = null;
            break;
        }
        return iconImage;
    }

	public interface OnContentChangedListener {
		void onContentChanged(ConversationListAdapter adapter);
	}

	public void setOnContentChangedListener(OnContentChangedListener l) {
		mOnContentChangedListener = l;
	}

	private ChangeListener changeListener = new ChangeListener() {

		public void onContentChanged() {
			if (mOnContentChangedListener != null) {
				mOnContentChangedListener.onContentChanged(ConversationListAdapter.this);
			}
		}

		public Cursor onCursorChanging(Cursor newCursor) {
			createCaches(newCursor);
			if (lastDataWorker != null) {
				lastDataWorker.clear();
			}
			if (contactDetailWorker != null) {
				contactDetailWorker.clear();
			}

			// set up to animate the top view if its thread has changed
//			if (newCursor != null && newCursor.moveToFirst()) {
//				try {
//					final long id = newCursor.getLong(Conversation.ID);
//
//					if (Logger.IS_DEBUG_ENABLED) {
//						Logger.debug(ConversationListAdapter.this +
//							".onCursorChanging: top id = " + id + ", last = " + lastTopId);
//					}
//
//					if (id != lastTopId && lastTopId != -1) {
//						animatedPos = 0;
//					}
//					lastTopId = id;
//				}
//				catch (Exception e) {
//					Logger.error(getClass(), e);
//				}
//			}
//			else {
//				animatedPos = -1;
//				lastTopId = -1;
//			}

			return newCursor;
		}
	};

	public void setBatchMode(boolean batchMode) {
	    this.batchMode = batchMode;
	    
	    //refresh the view once we have toggled the batchmode
	    notifyDataSetChanged();
	}
	
	private OnCheckedChangeListener checkedChangeListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton paramCompoundButton, boolean isChecked) {
            if (paramCompoundButton.getTag() instanceof Long) {
                Long threadId = (Long)paramCompoundButton.getTag();
                ArrayList<Long> selectedThreads = batchThreads.getBatchedThreads();
                boolean notify = false;
                
                if (selectedThreads.size() == 0 || selectedThreads.size() == 1) {
                    //notify only when we have to change the state of action button 
                    //in conversation screen
                    notify = true;
                }
                
                int location = selectedThreads.indexOf(threadId);
                if (isChecked) {
                    if (location == -1) {
                        selectedThreads.add(threadId);
                    }
                } else {
                    if (location != -1) {
                        selectedThreads.remove(location);
                    }
                }
                
                if (notify) {
                    batchThreads.onThreadsUpdated();
                }
            }
        }
    };

	@Override
	protected long getContentChangeDelay() {
		// if we are animating (or about to) then defer the update
//		if (animatedPos >= 0) {
//			return -1;
//		}

		//if we are in process of deleting more than one thread
		//return maximum content change delay as it would result in aggressively refreshing the lists
		//which could result in ANR
		if (mDeleteInProgress) {
			return DEFAULT_CONTENT_CHANGE_DELAY;
		}
//		// if the number of threads has changed then update immediately
//		final Cursor cursor = getCursor();
//		if (cursor != null) {
//			final int oldCount = cursor.getCount();
//			final int newCount = Conversation.getThreadCount(context);
//			if (Logger.IS_DEBUG_ENABLED) {
//				Logger.debug(this + ".getContentChangeDelay: oldCount = " + oldCount + ", newCount = " + newCount);
//			}
//			if (newCount != oldCount) {
//				return MINIMUM_CONTENT_CHANGE_DELAY;
//			}
//		}

		return super.getContentChangeDelay();
	}

	private Drawable[] getContactImages(int pos, String ids, ContactDetails contactDetails2) {
		ContactDetails contactDetails = null;
		ContactDetails oldcontactDetails = null;
		Drawable[] images = defaultImages;
		synchronized (contactDetailsCache) {
			contactDetails = contactDetailsCache.get(ids);
			oldcontactDetails = oldContactDetailsCache.get(ids);
		}
		
		if (contactDetails != null) {
			// try the local cache
			images = contactDetails.images;
			if (images == null) {
				if (oldcontactDetails != null) {
					images = oldcontactDetails.images;
				}
				contactDetailWorker.request(QUEUE_CONTACT_DETAILS, pos, contactDetailsJob, ids);
				
				if (images == null) {
					images = defaultImages;
				}
			}
		}
		return images;
	}
	
	private ListDataJob contactDetailsJob = new ListDataJob() {
		public Object run(int pos, Object data) {
			ContactDetails contactDetails = null;
			Drawable[] images = defaultImages;
			String ids = (String)data;

			synchronized (contactDetailsCache) {
				contactDetails = contactDetailsCache.get(ids);
				oldContactDetailsCache.put(ids, contactDetails);
			}

			if (contactDetails != null) {

				ContactList contacts = ContactList.getByIds(ids, true, ids, true);

				int num = contacts.size();
				String names;


				if (num == 1) {
					names = contacts.get(0).getName();
				}
				else if (num > 1) {
					names = contacts.formatNames();
				}
				else {
					names = "";
					Logger.error(this + ".fetchContactNames: no contacts for " + ids);
				}

				contactDetails.contactNameData = new ContactNameData(names, num);
				contactDetails.contactList = contacts;

				images = contactDetails.contactList.getImages(context, defaultImage, false);
				if (images == null) {
					images = defaultImages;
				}
				contactDetails.images = images;
			}
			return contactDetails;
		}
	};

	public void resetUnreadCount(long threadId) {
		synchronized (unreadCache) {
			unreadCache.put(threadId, 0);
			oldUnreadCache.put(threadId, 0);
		}
	}
	
	public void setDeleteInProgress(boolean flag) {
		mDeleteInProgress = flag;
	}
}
