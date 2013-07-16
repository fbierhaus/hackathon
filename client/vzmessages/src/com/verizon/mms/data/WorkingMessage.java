/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.verizon.mms.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.ContentType;
import com.verizon.mms.ExceedMessageSizeException;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.ResolutionException;
import com.verizon.mms.UnsupportContentTypeException;
import com.verizon.mms.model.AudioModel;
import com.verizon.mms.model.ImageModel;
import com.verizon.mms.model.LocationModel;
import com.verizon.mms.model.MediaModel;
import com.verizon.mms.model.SlideModel;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.model.TextModel;
import com.verizon.mms.model.VCardModel;
import com.verizon.mms.model.VideoModel;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduBody;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.SendReq;
import com.verizon.mms.transaction.MessageSender;
import com.verizon.mms.transaction.MmsMessageSender;
import com.verizon.mms.transaction.SmsMessageSender;
import com.verizon.mms.ui.MessageListAdapter.SendingMessage;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.ui.MessagingPreferenceActivity;
import com.verizon.mms.ui.SlideshowEditor;
import com.verizon.mms.util.Recycler;
import com.verizon.mms.util.SqliteWrapper;


/**
 * Contains all state related to a message being edited by the user.
 */
public class WorkingMessage {
	/**
     * 
     */
    private static final String SEND_SMS_THREAD_NAME = "SendSmsWorker";

    /**
     * 
     */
    private static final String SEND_MMS_THREAD_NAME = "SendMmsWorker";

    // Public intents
	public static final String ACTION_SENDING_SMS = "android.intent.action.SENDING_SMS";

	// Intent extras
	public static final String EXTRA_SMS_MESSAGE = "android.mms.extra.MESSAGE";
	public static final String EXTRA_SMS_RECIPIENTS = "android.mms.extra.RECIPIENTS";
	public static final String EXTRA_SMS_THREAD_ID = "android.mms.extra.THREAD_ID";

	// Database access stuff
	private final Context mContext;
	private final ContentResolver mContentResolver;

	// States that can require us to save or send a message as MMS.
	private static final int RECIPIENTS_REQUIRE_MMS = (1 << 0); // 1
	private static final int HAS_SUBJECT = (1 << 1); // 2
	private static final int HAS_ATTACHMENT = (1 << 2); // 4
	public static final int LENGTH_REQUIRES_MMS = (1 << 3); // 8
	private static final int FORCE_MMS = (1 << 4); // 16
	public  static final int GROUP_MMS = (1 << 5); // 32

	// A bitmap of the above indicating different properties of the message;
	// any bit set will require the message to be sent via MMS.
	private int mMmsState;

	// Errors from setAttachment()
	public static final int OK = 0;
	public static final int UNKNOWN_ERROR = -1;
	public static final int MESSAGE_SIZE_EXCEEDED = -2;
	public static final int UNSUPPORTED_TYPE = -3;
	public static final int IMAGE_TOO_LARGE = -4;
	public static final int SECURITY_EXCEPTION = -5;

	// Attachment types
	public static final int NONE = -1;
	public static final int TEXT = 0;
	public static final int IMAGE = 1;
	public static final int VIDEO = 2;
	public static final int AUDIO = 3;
	public static final int SLIDESHOW = 4;
	public static final int LOCATION = 5;
	public static final int VCARD = 6;
	public static final int DRM_TYPE = 7;// tells that the attachment is DRM Protected

	// Current attachment type of the message; one of the above values.
	private int mAttachmentType;

	// Conversation this message is targeting.
	private Conversation mConversation;

	// Text of the message.
	private CharSequence mText;
	// Slideshow for this message, if applicable. If it's a simple attachment,
	// i.e. not SLIDESHOW, it will contain only one slide.
	private SlideshowModel mSlideshow;
	// Data URI of an MMS message if we have had to save it.
	private Uri mMessageUri;
	// MMS subject line for this message
	private CharSequence mSubject;

	// Set to true if this message has been discarded.
	private boolean mDiscarded = false;

	// Cached value of mms enabled flag
	private static boolean sMmsEnabled = MmsConfig.getMmsEnabled();

	// Files data directory path
	private static String mFilesDirPath;
	
	// Our callback interface
	private final MessageStatusListener mStatusListener;
	private List<String> mWorkingRecipients;
	private long mDraftThreadId;
	private static AtomicLong sendingId = new AtomicLong(1000000000);

	// Message sizes in Outbox
	private static final String[] MMS_OUTBOX_PROJECTION = {
		Mms._ID,         // 0
		Mms.MESSAGE_SIZE // 1
	};

	private static final int MMS_MESSAGE_SIZE_INDEX = 1;

	/**
	 * Callback interface for communicating important state changes back to
	 * ComposeMessageActivity.
	 */
	public interface MessageStatusListener {
		/**
		 * Called when the protocol for sending the message changes from SMS
		 * to MMS, and vice versa.
		 * 
		 * @param protocolChanged True if the message sending protocol changed
		 * @param mms True if the message will be sent with MMS instead of SMS
		 * @param messageChanged True if the message properties changed, as opposed to group mode or other factors
		 * @param messageMms True if the message properties will cause the message to be sent with MMS instead of SMS
		 */
		void onMessageChanged(boolean protocolChanged, boolean mms, boolean messageChanged, boolean messageMms);

		/**
		 * Called when an attachment on the message has changed.
		 */
		void onAttachmentChanged();

		/**
		 * Called when the message is being prepared to be sent.
		 */
		void onPreMessageSent();

		/**
		 * Called just before sending messages.
		 *
		 * @param msgs The list of messages being sent, with temporary ids
		 * @param mms True if they are MMS messages
		 * @param lastId Last outgoing ID in thread or -1 if unknown
		 */
		void onSendingMessages(List<SendingMessage> msgs, boolean mms, long lastId);

		/**
		 * Called once the process of sending a message, triggered by {@link send} has completed. This doesn't mean the
		 * send succeeded, just that it has been dispatched to the network.
		 *
		 * @param msgs The list of messages sent, updated with actual ids and errors if any
		 */
		void onMessagesSent(List<SendingMessage> msgs);

		/**
		 * Called if the message has a sending error.
		 */
		void onSendError();

		/**
		 * Called if there are too many unsent messages in the queue and we're not allowing
		 * any more Mms's to be sent.
		 */
		void onMaxPendingMessagesReached();

		/**
		 * Called if there's an attachment error while resizing the images just before sending.
		 */
		void onAttachmentError(int error);
		
		/**
		 * Get the context from the listener
		 */
		Context getContext();
		
		
	}

	private WorkingMessage(MessageStatusListener listener) {
		mContext = listener.getContext();
		mContentResolver = mContext.getContentResolver();
		mStatusListener = listener;
		mAttachmentType = TEXT;
		mText = "";
		mFilesDirPath = mContext.getFilesDir().getAbsolutePath();
	}

	/**
	 * Creates a new working message.
	 */
	public static WorkingMessage createEmpty(MessageStatusListener listener) {
		// Make a new empty working message.
		WorkingMessage msg = new WorkingMessage(listener);
		return msg;
	}

	/**
	 * Create a new WorkingMessage from the specified data URI, which typically
	 * contains an MMS message.
	 */
	public static WorkingMessage load(MessageStatusListener listener, Uri uri) {
		// If the message is not already in the draft box, move it there.
		if (!uri.toString().startsWith(VZUris.getMmsDraftsUri().toString())) {
			PduPersister persister = PduPersister.getPduPersister(listener.getContext());
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(WorkingMessage.class, "load: moving to drafts: " + uri);
			}
			try {
				uri = persister.move(uri, VZUris.getMmsDraftsUri());
			} catch (MmsException e) {
				Logger.error(WorkingMessage.class, "Error moving to drafts: " + uri,  e);
				return null;
			}
		}

		WorkingMessage msg = new WorkingMessage(listener);
		if (msg.loadFromUri(uri)) {
			return msg;
		}

		return null;
	}

	public SlideModel getSlideModel(){
	    return mSlideshow.get(0); //Helps in retrieving the recent last attached model
	}
	
	private void correctAttachmentState(boolean notify) {
		int slideCount = mSlideshow.size();

		// If we get an empty slideshow, tear down all MMS
		// state and discard the unnecessary message Uri.
		if (slideCount == 0) {
			mAttachmentType = TEXT;
			mSlideshow = null;
			if (mMessageUri != null) {
				asyncDelete(mMessageUri, null, null);
				mMessageUri = null;
			}
		} else if (slideCount > 1) {
			mAttachmentType = SLIDESHOW;
		} else {
			SlideModel slide = mSlideshow.get(0);
			if (slide.hasLocation()) {
	               mAttachmentType = LOCATION;
	        } else if (slide.hasImage()) {
				mAttachmentType = IMAGE;
			} else if (slide.hasVideo()) {
				mAttachmentType = VIDEO;
			} else if (slide.hasAudio()) {
				mAttachmentType = AUDIO;
			} else if (slide.hasVCard()) {
			    mAttachmentType = VCARD;
			}
		}

		updateState(HAS_ATTACHMENT, hasAttachment(), notify);
	}

	private boolean loadFromUri(Uri uri) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "loadFromUri: uri = " + uri);
		}
		try {
			mSlideshow = SlideshowModel.createFromMessageUri(mContext, uri);
			SlideshowModel.reorderSlideShow(mSlideshow);
		} catch (MmsException e) {
			Logger.error(getClass(), "Error loading: " + uri, e);
			return false;
		}

		mMessageUri = uri;

		// Make sure all our state is as expected.
		syncTextFromSlideshow();
		correctAttachmentState(false);

		return true;
	}

	/**
	 * Load the draft message for the specified conversation, or a new empty message if
	 * none exists.
	 */
	public static WorkingMessage loadDraft(MessageStatusListener listener, Conversation conv) {
		WorkingMessage msg = new WorkingMessage(listener);
		if (msg.loadFromConversation(conv)) {
			return msg;
		} else {
			return createEmpty(listener);
		}
	}

	private boolean loadFromConversation(Conversation conv) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "loadFromConversation: " + conv);
		}

		long threadId = conv.getThreadId();
		if (threadId <= 0) {
			return false;
		}

		// Look for an SMS draft first.
		mText = readDraftSmsMessage(conv);
		if (!TextUtils.isEmpty(mText)) {
			return true;
		}

		// Then look for an MMS draft.
		StringBuilder sb = new StringBuilder();
		Uri uri = readDraftMmsMessage(mContext, threadId, sb);
		if (uri != null) {
			if (loadFromUri(uri)) {
				// If there was an MMS message, readDraftMmsMessage
				// will put the subject in our supplied StringBuilder.
				if (sb.length() > 0) {
					setSubject(sb.toString(), false);
				}
				return true;
			}
		}

		return false;
	}

	/**
	 * Sets the text of the message to the specified CharSequence.
	 */
	public void setText(CharSequence s) {
		mText = s;
	}

	/**
	 * Returns the current message text.
	 */
	public CharSequence getText() {
		return mText;
	}

	/**
	 * Returns true if the message has any text. A message with just whitespace is not considered
	 * to have text.
	 * 
	 * @return
	 */
	public boolean hasText() {
		return mText != null && TextUtils.getTrimmedLength(mText) > 0;
	}

	/**
	 * Adds an attachment to the message, replacing an old one if it existed.
	 * 
	 * @param type
	 *            Type of this attachment, such as {@link IMAGE}
	 * @param dataUri
	 *            Uri containing the attachment data (or null for {@link TEXT})
	 * @param append
	 *            true if we should add the attachment to a new slide
	 * @return An error code such as {@link UNKNOWN_ERROR} or {@link OK} if successful
	 */
	public int setAttachment(int type, Uri dataUri, boolean append) {
	    return setAttachments(type, new Uri[]{dataUri}, append);
	}
	
	public int setAttachments(int type, Uri[] dataUri, boolean append) {
		if (Logger.IS_DEBUG_ENABLED) {
			 Logger.debug(getClass(), "setAttachment type = " + type + ", uri = " + dataUri);
		}
		int result = OK;

		// Make sure mSlideshow is set up and has a slide.
		ensureSlideshow();

		// Change the attachment and translate the various underlying
		// exceptions into useful error codes.
		try {
			if (append) {
				appendMedia(type, dataUri);
			} else {
				changeMedia(type, dataUri);
			}
		} catch (MmsException e) {
			result = UNKNOWN_ERROR;
			Logger.error(getClass(), e);
		} catch (UnsupportContentTypeException e) {
			result = UNSUPPORTED_TYPE;
		} catch (ExceedMessageSizeException e) {
			result = MESSAGE_SIZE_EXCEEDED;
		} catch (ResolutionException e) {
			result = IMAGE_TOO_LARGE;
		} catch (Exception e) {
		    Logger.error(e);
		    result = UNKNOWN_ERROR;
		}

		// If we were successful, update mAttachmentType and notify
		// the listener than there was a change.
		if (result == OK) {
			mAttachmentType = type;
			mStatusListener.onAttachmentChanged();
		} else if (append) {
			// We may have added a new slide and what we attempted to insert on the slide failed.
			// Delete that slide, otherwise we could end up with a bunch of blank slides.
			final int size = mSlideshow.size();
			if (size > 1) {
				SlideshowEditor slideShowEditor = new SlideshowEditor(mContext, mSlideshow);
				slideShowEditor.removeSlide(size - 1);
			}
		}

		// set attachment state and update listeners
		correctAttachmentState(true);
		return result;
	}

	/**
	 * Returns true if this message contains anything worth saving.
	 */
	public boolean isWorthSaving() {
		// If it actually contains anything, it's of course not empty.
		if (hasText() || hasSubject() || hasAttachment() || hasSlideshow()) {
			return true;
		}

		// When saveAsMms() has been called, we set FORCE_MMS to represent
		// sort of an "invisible attachment" so that the message isn't thrown
		// away when we are shipping it off to other activities.
		if (isFakeMmsForDraft()) {
			return true;
		}

		return false;
	}

	/**
	 * Returns true if FORCE_MMS is set.
	 * When saveAsMms() has been called, we set FORCE_MMS to represent
	 * sort of an "invisible attachment" so that the message isn't thrown
	 * away when we are shipping it off to other activities.
	 */
	public boolean isFakeMmsForDraft() {
		return (mMmsState & FORCE_MMS) > 0;
	}

	/**
	 * Makes sure mSlideshow is set up.
	 */
	private void ensureSlideshow() {
		if (mSlideshow != null) {
			return;
		}

		SlideshowModel slideshow = SlideshowModel.createNew(mContext);
		SlideModel slide = new SlideModel(slideshow);
		slideshow.add(slide);

		mSlideshow = slideshow;
	}

	/**
	 * Change the message's attachment to the data in the specified Uri.
	 * Used only for single-slide ("attachment mode") messages.
	 */
	private void changeMedia(int type, Uri[] uri) throws MmsException {
		SlideModel slide = mSlideshow.get(0);
		if (slide == null) {
			Logger.warn(getClass(), "changeMedia: no slides");
			return;
		}

		// Remove any previous attachments.
		slide.removeImage();
		slide.removeVideo();
		slide.removeAudio();
		slide.removeVCard();

		// If we're changing to text, just bail out.
		if (type == TEXT) {
			return;
		}

		addMedia(type, uri, slide);
	}

	private void addMedia(int type, Uri[] uri, SlideModel slide) throws MmsException {
		// Make a correct MediaModel for the type of attachment.
		final MediaModel media;
		if (type == IMAGE) {
			// Handle picasa issue:
			// http://code.google.com/p/android/issues/detail?id=21234 
			Uri imageUri = uri[0];
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "addMedia: image uri: " + imageUri);
			}
			if (imageUri.toString().startsWith("content://com.android.gallery3d.provider")) {
				// use the com.google provider, not the com.android provider.
				imageUri = Uri.parse(imageUri.toString().replace(
						"com.android.gallery3d",
						"com.google.android.gallery3rd"));

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "addMedia: changed uri to: " + imageUri);
				}
			}

			media = new ImageModel(mContext, imageUri, mSlideshow.getLayout().getImageRegion());
			//if we are not able to fetch the bitmap initialization of ImageModel itself throws an error
			/*ImageModel imageModel = (ImageModel) media;
			Bitmap bitmap = imageModel.getBitmap();
			if (bitmap == null) {
			    throw new MmsException("Unable to fetch the bitmap");
			}*/
		} else if (type == VIDEO) {
			media = new VideoModel(mContext, uri[0], mSlideshow.getLayout().getImageRegion());
		} else if (type == AUDIO) {
			media = new AudioModel(mContext, uri[0]);
		} else if (type == VCARD) {
		    media = new VCardModel(mContext, uri[0],  mSlideshow.getLayout().getImageRegion());
		} else if (type == LOCATION) {
		    LocationModel locationModel = new LocationModel(mContext, uri[0], mSlideshow.getLayout().getImageRegion());
		    slide.add(locationModel);
		    
		    if (uri.length > 1 && uri[1] != null) {
		        media = new ImageModel(mContext, uri[1], mSlideshow.getLayout().getImageRegion());
		    } else {
		        media = null;
		    }
	    } else {
			throw new IllegalArgumentException("changeMedia type=" + type + ", uri=" + uri);
		}

		// Add it to the slide.
		slide.add(media);

		// For video and audio, set the duration of the slide to
		// that of the attachment.
		if (type == VIDEO || type == AUDIO) {
			slide.updateDuration(media.getDuration());
		}
	}

	/**
	 * Add the message's attachment to the data in the specified Uri to a new slide.
	 */
	private void appendMedia(int type, Uri[] uri) throws MmsException {
		// If we're changing to text, just bail out.
		if (type == TEXT) {
			return;
		}

		// The first time this method is called, mSlideshow.size() is going to be
		// one (a newly initialized slideshow has one empty slide). The first time we
		// attach the picture/video to that first empty slide. From then on when this
		// function is called, we've got to create a new slide and add the picture/video
		// to that new slide.
		final SlideshowModel slideshow = mSlideshow;
		int size = slideshow.size();
		if (size != 1 || !slideshow.get(0).isTextOnly()) {
			SlideshowEditor slideShowEditor = new SlideshowEditor(mContext, slideshow);
			if (!slideShowEditor.addNewSlide()) {
				return;
			}
			++size;
		}
		// add a correct MediaModel for the type of attachment.
		SlideModel slide = slideshow.get(size - 1);
		addMedia(type, uri, slide);
	}

	/**
	 * Remove attachment at given position.
	 */
	public void removeAttachment(int position) {
		// remove attachment if in range
		final SlideshowModel slideshow = mSlideshow;
		if (slideshow != null) {
			final int size = slideshow.size();
			if (position >= 0 && position < size) {
				SlideModel slide = slideshow.remove(position);
				
				freeScrapSpace(slide);
				
				// set attachment state and update listeners
				correctAttachmentState(true);
			}
		}
	}

	/**
     * This Method removes the temp files that were used to store the media
     * such as captured image, vcard and location
     * @param slide
     */
	private static void freeScrapSpace(SlideModel slide) {
	    Uri uri = null;

	    if (slide.hasVCard()) {
	        uri = slide.getVCard().getUri();
	        removeFile(uri);
	    } 
	    if (slide.hasLocation()) {
	        uri = slide.getLocation().getUri();
	        removeFile(uri);
	    }
	    if (slide.hasImage()) {
	        uri = slide.getImage().getUri();
	        removeFile(uri);
	    }
	}

    /**
     * This Method deletes the file pointed by the uri if it was created by our application
     * @param uri
     */
    public static void removeFile(Uri uri) {
        if (uri != null) {
            String path = uri.getPath();
            if (path != null && uri.getScheme().equals("file") && 
                    (path.startsWith(Mms.ScrapSpace.SCRAP_DIR_PATH) || 
                            path.startsWith(mFilesDirPath))) {
                File file = new File(path);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    /**
	 * Returns true if the message has an attachment (including slideshows).
	 */
	public boolean hasAttachment() {
		return (mAttachmentType > TEXT);
	}

	/**
	 * Returns the slideshow associated with this message.
	 */
	public SlideshowModel getSlideshow() {
		return mSlideshow;
	}

	/**
	 * Returns true if the message has a real slideshow, as opposed to just
	 * one image attachment, for example.
	 */
	public boolean hasSlideshow() {
		return (mAttachmentType == SLIDESHOW);
	}

	/**
	 * Sets the MMS subject of the message. Passing null indicates that there
	 * is no subject. Passing "" will result in an empty subject being added
	 * to the message, possibly triggering a conversion to MMS. This extra
	 * bit of state is needed to support ComposeMessageActivity converting to
	 * MMS when the user adds a subject. An empty subject will be removed
	 * before saving to disk or sending, however.
	 */
	public void setSubject(CharSequence s, boolean notify) {
		mSubject = s;
		updateState(HAS_SUBJECT, (s != null), notify);
	}

	/**
	 * Returns the MMS subject of the message.
	 */
	public CharSequence getSubject() {
		return mSubject;
	}

	/**
	 * Returns true if this message has an MMS subject. A subject has to be more than just
	 * whitespace.
	 * 
	 * @return
	 */
	public boolean hasSubject() {
		return mSubject != null && TextUtils.getTrimmedLength(mSubject) > 0;
	}

	/**
	 * Moves the message text into the slideshow. Should be called any time
	 * the message is about to be sent or written to disk.
	 */
	private void syncTextToSlideshow() {
		if (mSlideshow == null || mSlideshow.size() < 1) {
			// should never be allowed?
			throw new IllegalStateException("Trying to sync text with " + (mSlideshow == null ? "null" : "empty") + " slideshow");
		}

		SlideModel slide = mSlideshow.get(0);
		TextModel text;
		if (!slide.hasText()) {
			// Add a TextModel to slide 0 if one doesn't already exist
			text = new TextModel(mContext, ContentType.TEXT_PLAIN, SlideshowModel.FIRST_SLIDE_TEXT_SRC, mSlideshow.getLayout().getTextRegion());
			slide.add(text);
		} else {
			// Otherwise just reuse the existing one.
			text = slide.getText();
		}
		
		text.setText(mText);
	}

	/**
	 * Sets the message text out of the slideshow. Should be called any time
	 * a slideshow is loaded from disk.
	 */
	private void syncTextFromSlideshow() {
		// Don't sync text for real slideshows.
		if (mSlideshow.size() < 1) {
			return;
		}

		SlideModel slide = mSlideshow.get(0);
		if (slide == null || !slide.hasText()) {
			return;
		}

		mText = slide.getText().getText();
	}

	/**
	 * Removes the subject if it is empty, possibly converting back to SMS.
	 */
	private void removeSubjectIfEmpty(boolean notify) {
		if (!hasSubject()) {
			setSubject(null, notify);
		}
	}

	/**
	 * Gets internal message state ready for storage. Should be called any
	 * time the message is about to be sent or written to disk.
	 */
	private void prepareForSave(boolean notify) {
		// Make sure our working set of recipients is resolved
		// to first-class Contact objects before we save.
		syncWorkingRecipients();

		if (requiresMms()) {
			ensureSlideshow();
			syncTextToSlideshow();
			removeSubjectIfEmpty(notify);
		}
	}

	/**
	 * Resolve the temporary working set of recipients to a ContactList.
	 */
	public void syncWorkingRecipients() {
		if (mWorkingRecipients != null) {
			ContactList recipients = ContactList.getByNumbers(mWorkingRecipients, false);
			mConversation.setRecipients(recipients);
			mWorkingRecipients = null;
		}
	}

	// Call when we've returned from adding an attachment. We're no longer forcing the message
	// into a Mms message. At this point we either have the goods to make the message a Mms
	// or we don't. No longer fake it.
	public void removeFakeMmsForDraft() {
		updateState(FORCE_MMS, false, false);
	}

	/**
	 * Force the message to be saved as MMS and return the Uri of the message.
	 * Typically used when handing a message off to another activity.
	 */
	public Uri saveAsMms(boolean notify) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "saveAsMms: mConversation = " + mConversation);
		}

		if (mDiscarded) {
			throw new IllegalStateException("save() called after discard()");
		}

		// FORCE_MMS behaves as sort of an "invisible attachment", making
		// the message seem non-empty (and thus not discarded). This bit
		// is sticky until the last other MMS bit is removed, at which
		// point the message will fall back to SMS.
		updateState(FORCE_MMS, true, notify);

		// Collect our state to be written to disk.
		prepareForSave(true /* notify */);

		// Make sure we are saving to the correct thread ID.
		mConversation.ensureThreadId();
		mConversation.setDraftState(true);

		PduPersister persister = PduPersister.getPduPersister(mContext);
		SendReq sendReq = makeSendReq(mConversation, mSubject);

		// If we don't already have a Uri lying around, make a new one. If we do
		// have one already, make sure it is synced to disk.
		if (mMessageUri == null) {
			mMessageUri = createDraftMmsMessage(persister, sendReq, mSlideshow);
		} else {
			updateDraftMmsMessage(mMessageUri, persister, mSlideshow, sendReq);
		}

		return mMessageUri;
	}

	/**
	 * Save this message as a draft in the conversation previously specified
	 * to {@link setConversation}.
	 */
	public void saveDraft() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "saveDraft");
		}

		// If we have discarded the message, just bail out.
		if (mDiscarded) {
			return;
		}

		// Make sure setConversation was called.
		if (mConversation == null) {
			throw new IllegalStateException("saveDraft() called with no conversation");
		}

		// Get ready to write to disk. But don't notify message status when saving draft
		prepareForSave(false /* notify */);

		if (requiresMms()) {
			asyncUpdateDraftMmsMessage(mConversation);
		} else {
			String content = mText.toString();

			// bug 2169583: don't bother creating a thread id only to delete the thread
			// because the content is empty. When we delete the thread in updateDraftSmsMessage,
			// we didn't nullify conv.mThreadId, causing a temperary situation where conv
			// is holding onto a thread id that isn't in the database. If a new message arrives
			// and takes that thread id (because it's the next thread id to be assigned), the
			// new message will be merged with the draft message thread, causing confusion!
			if (!TextUtils.isEmpty(content)) {
				asyncUpdateDraftSmsMessage(mConversation, content);
				// Update state of the draft cache.
				mConversation.setDraftState(true);
			}
		}

	}

	synchronized public void discard() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "discard");
		}

		if (mDiscarded == true) {
			return;
		}

		//free up the created space
		if (mSlideshow != null) {
		    for (SlideModel sm : mSlideshow) {
		        freeScrapSpace(sm);
		    }
		}
		
		// Mark this message as discarded in order to make saveDraft() no-op.
		mDiscarded = true;

		// Delete our MMS message, if there is one.
		if (mMessageUri != null) {
			asyncDelete(mMessageUri, null, null);
			//Bug 985 set the mMessageUri to null on discarding the message
			mMessageUri = null;
		}

		clearConversation(mConversation);
	}

	public void unDiscard() {
		if (Logger.IS_DEBUG_ENABLED)
			Logger.debug(getClass(), "unDiscard");

		mDiscarded = false;
	}

	/**
	 * Returns true if discard() has been called on this message.
	 */
	public boolean isDiscarded() {
		return mDiscarded;
	}

	/**
	 * To be called from our Activity's onSaveInstanceState() to give us a chance
	 * to stow our state away for later retrieval.
	 * 
	 * @param bundle
	 *            The Bundle passed in to onSaveInstanceState
	 */
	public void writeStateToBundle(Bundle bundle) {
		if (hasSubject()) {
			bundle.putString("subject", mSubject.toString());
		}

		if (mMessageUri != null) {
			bundle.putParcelable("msg_uri", mMessageUri);
		} else if (hasText()) {
			bundle.putString("sms_body", mText.toString());
		}
	}

	/**
	 * To be called from our Activity's onCreate() if the activity manager
	 * has given it a Bundle to reinflate
	 * 
	 * @param bundle
	 *            The Bundle passed in to onCreate
	 */
	public void readStateFromBundle(Bundle bundle) {
		if (bundle == null) {
			return;
		}

		String subject = bundle.getString("subject");
		setSubject(subject, false);

		Uri uri = (Uri) bundle.getParcelable("msg_uri");
		if (uri != null) {
			loadFromUri(uri);
			return;
		} else {
			String body = bundle.getString("sms_body");
			mText = body;
		}
	}

	/**
	 * Update the temporary list of recipients, used when setting up a
	 * new conversation. Will be converted to a ContactList on any
	 * save event (send, save draft, etc.)
	 */
	public void setWorkingRecipients(List<String> numbers) {
		mWorkingRecipients = numbers;
	}

	/**
	 * Set the conversation associated with this message.
	 */
	public void setConversation(Conversation conv) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "setConversation: from " + mConversation + " to " + conv);
		}

		mConversation = conv;

		// Convert to MMS if there are any email addresses in the recipient list.
		setHasEmail(conv.getRecipients().containsEmail(), false);
	}

	/**
	 * Hint whether or not this message will be delivered to an
	 * an email address.
	 */
    public void setHasEmail(boolean hasEmail, boolean notify) {
    	if (MmsConfig.getEmailGateway() != null) {
			updateState(RECIPIENTS_REQUIRE_MMS, false, notify);
		} else {
			updateState(RECIPIENTS_REQUIRE_MMS, hasEmail, notify);
		}
    }

    /**
	 * Returns true if this message would require MMS to send.
	 */
	public boolean requiresMms() {
		return mMmsState != 0;
	}

	private boolean isGroupMms() {
		return (mMmsState & GROUP_MMS) != 0;
	}

	private static String stateString(int state) {
		if (state == 0)
			return "<none>";

		StringBuilder sb = new StringBuilder();
		if ((state & RECIPIENTS_REQUIRE_MMS) > 0)
			sb.append("RECIPIENTS_REQUIRE_MMS | ");
		if ((state & HAS_SUBJECT) > 0)
			sb.append("HAS_SUBJECT | ");
		if ((state & HAS_ATTACHMENT) > 0)
			sb.append("HAS_ATTACHMENT | ");
		if ((state & LENGTH_REQUIRES_MMS) > 0)
			sb.append("LENGTH_REQUIRES_MMS | ");
		if ((state & FORCE_MMS) > 0)
			sb.append("FORCE_MMS | ");
		if ((state & GROUP_MMS) > 0)
			sb.append("GROUP_MMS | ");

		sb.delete(sb.length() - 3, sb.length());
		return sb.toString();
	}

	/**
	 * Sets the current state of our various "MMS required" bits.
	 * 
	 * @param state
	 *            The bit to change, such as {@link HAS_ATTACHMENT}
	 * @param on
	 *            If true, set it; if false, clear it
	 * @param notify
	 *            Whether or not to notify the user
	 * @return True if the MMS required state was changed
	 */
	public boolean updateState(int state, boolean on, boolean notify) {
		if (!sMmsEnabled) {
			// If Mms isn't enabled, the rest of the Messaging UI should not be using any
			// feature that would cause us to to turn on any Mms flag and show the
			// "Converting to multimedia..." message.
			return false;
		}
		int oldState = mMmsState;
		if (on) {
			mMmsState |= state;
		} else {
			mMmsState &= ~state;
		}

		// If we are clearing the last bit that is not FORCE_MMS,
		// expire the FORCE_MMS bit.
		final int unforcedOldState = oldState & ~FORCE_MMS;  // ignore force bit
		if (mMmsState == FORCE_MMS && unforcedOldState != 0) {
			mMmsState = 0;
		}

		// Notify the listener if we are moving from SMS to MMS or vice versa and whether
		// the base message properties have changed, as opposed to the group flag.
		final int unforcedNewState = mMmsState & ~FORCE_MMS;  // ignore force bit
		final int unforcedMessageState = unforcedNewState & ~GROUP_MMS;  // ignore group bit
		final boolean protocolChanged = (unforcedOldState == 0) != (unforcedNewState == 0);
		final boolean messageChanged = ((unforcedOldState & ~GROUP_MMS) == 0) != (unforcedMessageState == 0);
		if (notify && (protocolChanged || messageChanged)) {
			mStatusListener.onMessageChanged(protocolChanged, unforcedNewState != 0, messageChanged, unforcedMessageState != 0);
		}

		if (oldState != mMmsState && Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "updateState: " + (on ? "+" : "-") + stateString(state) + " = " + stateString(mMmsState));
		}

		return protocolChanged;
	}

	/**
	 * Send this message over the network. Will call back with onMessageSent() once
	 * it has been dispatched to the telephony stack. This WorkingMessage object is
	 * no longer useful after this method has been called.
	 */
	public void send() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "send");
		}

		// Get ready to write to disk.
		prepareForSave(true /* notify */);

		// We need the recipient list for both SMS and MMS.
		final Conversation conv = mConversation;
		final String msgTxt = mText.toString();

		if (requiresMms() || addressContainsEmailToMms(conv, msgTxt)) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "WorkingMessage.send: sending via MMS");
			}
			// Make local copies of the bits we need for sending a message,
			// because we will be doing it off of the main thread, which will
			// immediately continue on to resetting some of this state.
			final Uri mmsUri = mMessageUri;
			final PduPersister persister = PduPersister.getPduPersister(mContext);

			final SlideshowModel slideshow = mSlideshow;
			final SendReq sendReq = makeSendReq(conv, mSubject);

			// Do the dirty work of sending the message off of the main UI thread.
			new Thread(new Runnable() {
				public void run() {
					// Make sure the text in slide 0 is no longer holding onto a reference to
					// the text in the message text box.
					slideshow.prepareForSend();
					sendMmsWorker(conv, mmsUri, persister, slideshow, sendReq, msgTxt);
				}
			},SEND_MMS_THREAD_NAME).start();
		} else {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "WorkingMessage.send: sending via SMS");
			}
			// Same rules apply as above.
			final String msgText = mText.toString();
			new Thread(new Runnable() {
				public void run() {
					sendSmsWorker(conv, msgText);
				}
			}, SEND_SMS_THREAD_NAME).start();
		}

		// update the Recipient cache with the new to address, if it's different
		RecipientIdCache.updateNumbers(conv.getThreadId(), conv.getRecipients());

		// Mark the message as discarded because it is "off the market" after being sent.
		mDiscarded = true;
	}

	private boolean addressContainsEmailToMms(Conversation conv, String text) {
		if (MmsConfig.getEmailGateway() != null) {
			String[] dests = conv.getRecipients().getNumbers();
			int length = dests.length;
			for (int i = 0; i < length; i++) {
				if (Mms.isEmailAddress(dests[i]) || MessageUtils.isAlias(dests[i])) {
					String mtext = dests[i] + " " + text;
					int[] params = SmsMessage.calculateLength(mtext, false);
					if (params[0] > 1) {
						updateState(RECIPIENTS_REQUIRE_MMS, true, true);
						ensureSlideshow();
						syncTextToSlideshow();
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean updateContainsEmailToMms(List<String> dests, CharSequence text) {
		if (MmsConfig.getEmailGateway() != null) {
			for (String number : dests) {
				if (Mms.isEmailAddress(number) || MessageUtils.isAlias(number)) {
					String mtext = number + " " + text;
					int[] params = SmsMessage.calculateLength(mtext, false);
					if (params[0] > 1) {
						updateState(RECIPIENTS_REQUIRE_MMS, true, true);
						return true;
					}
				}
			}
		}
		updateState(RECIPIENTS_REQUIRE_MMS, false, true);
		
		return false;
	}

	// Message sending stuff

	private void sendSmsWorker(Conversation conv, String msgText) {
		// get or create the thread for our recipient set
		long threadId = conv.ensureThreadId();

		// notify the listener
		mStatusListener.onPreMessageSent();
		final String[] dests = conv.getRecipients().getNumbers();
		final int num = dests.length;
		final List<SendingMessage> msgs = new ArrayList<SendingMessage>(num);
		for (int i = 0; i < num; ++i) {
			final long id = sendingId.addAndGet(1);
			msgs.add(new SendingMessage(id, msgText));
		}
		mStatusListener.onSendingMessages(msgs, false, -1);

		// send the message(s)
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "sendSmsWorker: sending message");
		}
		final MessageSender sender = new SmsMessageSender(mContext, dests, msgText, threadId);
		try {
			final Uri[] uris = sender.sendMessage(threadId);

			// notify listener
			if (uris.length == num) {
				// update sending messages with actual Uris
				for (int i = 0; i < num; ++i) {
					msgs.get(i).setUri(uris[i]);
				}
				mStatusListener.onMessagesSent(msgs);
			}
			else {
			    if(Logger.IS_ERROR_ENABLED){
			        Logger.error(getClass(), "sendSmsWorker: msgs = " + msgs + ", uris = " + Arrays.asList(uris));
			    }
			}

			// Make sure this thread isn't over the limits in message count
//			Recycler.getMessageRecycler().deleteOldMessagesByThreadId(mContext, threadId);
		} catch (Exception e) {
		    if(Logger.IS_ERROR_ENABLED){
		        Logger.error(getClass(),"Error sending SMS message, threadId = " + threadId, e);
		    }
			mStatusListener.onSendError();
		}

		// Be paranoid and clean any draft SMS up.
		deleteDraftSmsMessage(threadId);
	}

	private void sendMmsWorker(Conversation conv, Uri mmsUri, PduPersister persister, SlideshowModel slideshow,
			SendReq sendReq, String msgText) {

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "sendMmsWorker: uri = " + mmsUri + ", slideshow = " + slideshow);
		}

		// check that existing outgoing messages aren't already beyond the size limit
		Cursor cursor = null;
		try {
			cursor = SqliteWrapper.query(mContext, mContentResolver, VZUris.getMmsOutboxUri(), MMS_OUTBOX_PROJECTION, null, null, null);
			if (cursor != null) {
				long maxMessageSize = MmsConfig.getMaxSizeScaleForPendingMmsAllowed() * MmsConfig.getMaxMessageSize();
				long totalPendingSize = 0;
				while (cursor.moveToNext()) {
					totalPendingSize += cursor.getLong(MMS_MESSAGE_SIZE_INDEX);
				}
				if (totalPendingSize >= maxMessageSize) {
					unDiscard(); // it wasn't successfully sent. Allow it to be saved as a draft.
					mStatusListener.onMaxPendingMessagesReached();
					return;
				}
			}
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		// get ot create the thread for our recipient set
		long threadId = conv.ensureThreadId();

		// notify the listener
		mStatusListener.onPreMessageSent();

		// if we are sending multiple messages then body text gets moved to the last one,
		// which is the first to be sent since they appear in the attachment list in reverse
		// order of when they were added
		//
		TextModel text = null;
		final int numSlides = slideshow.size();
		final SlideModel firstSlide = slideshow.get(0);
		if (firstSlide.hasText()) {
			text = firstSlide.getText();
			if (numSlides > 1) {
				firstSlide.removeText();
			}
		}

		// parse the slideshow into separate messages
		final ArrayList<SlideshowModel> slideshows = new ArrayList<SlideshowModel>(numSlides);
		final List<SendingMessage> sendingMsgs = new ArrayList<SendingMessage>(numSlides);
		final String subject = mSubject == null ? null : mSubject.toString();
		final int lastSlide = numSlides - 1;
		for (int i = lastSlide; i >= 0; --i) {
			final SlideshowModel msgSlideshow;
			if (i > 0) {
				// remove this slide from original message and create new slideshow with it
				msgSlideshow = SlideshowModel.createNew(mContext);
				final SlideModel slide = slideshow.remove(i, false);
				if (text != null) {
					slide.add(text);
					text = null;
				}
				msgSlideshow.add(slide);
			}
			else {
				// original message now contains text body and/or no more than one attachment
				msgSlideshow = slideshow;
			}

			// add to list of messages to send
			slideshows.add(msgSlideshow);
			final long id = sendingId.addAndGet(1);
			sendingMsgs.add(new SendingMessage(id, subject, msgSlideshow));
		}

		// notify the listener
		// if we have a uri then use it to get the last message id
		final long lastId = mmsUri != null ? Long.parseLong(mmsUri.getLastPathSegment()) - 1 : -1;
		mStatusListener.onSendingMessages(sendingMsgs, true, lastId);

		// create an outgoing message for each slide in the slideshow and send them
		for (int i = 0; i < numSlides; ++i) {
			final SlideshowModel msgSlideshow = slideshows.get(i);
			final SendingMessage msg = sendingMsgs.get(i);
			final Uri msgUri;

			if (i < lastSlide || mmsUri == null) {
				// create a new draft message
				msgUri = createDraftMmsMessage(persister, sendReq, msgSlideshow);
			}
			else {
				// sync the existing draft to disk
				msgUri = mmsUri;
				updateDraftMmsMessage(msgUri, persister, msgSlideshow, sendReq);
			}


			// try to resize the attachment if needed
			int error = 0;
			// what to do if msg could not be created. 
			if (msgUri == null) {
				error = UNKNOWN_ERROR;
			} else {
				try {
					msgSlideshow.finalResize(msgUri);
				}
				catch (ExceedMessageSizeException e1) {
					error = MESSAGE_SIZE_EXCEEDED;
				}
				catch (MmsException e1) {
					error = UNKNOWN_ERROR;
				}
				catch (Exception e1) {
					error = UNKNOWN_ERROR;
				}
			}
			if (error == 0) {
				// send it
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "sendMmsWorker: sending " + msgUri +
						", threadId = " + threadId + ", slides = " + msgSlideshow);
				}
				try {
					final MessageSender sender = new MmsMessageSender(mContext, msgUri, msgSlideshow.getCurrentMessageSize());
					final Uri[] uris = sender.sendMessage(threadId);
					msg.setUri(uris[0]);
				}
				catch (Exception e) {
					Logger.error(getClass(), "sendMmsWorker: error sending " + msgUri + ", threadId = " + threadId, e);
					markMmsMessageWithError(msgUri);
					msg.setError(UNKNOWN_ERROR);
					mStatusListener.onSendError();
				}
			}
			else {
				MessageUtils.markMmsMessageWithError(mContext, mContentResolver, msgUri, false);
				msg.setError(error);
				mStatusListener.onAttachmentError(error);
			}
		}

		// notify the listener
		mStatusListener.onMessagesSent(sendingMsgs);

		// remove any draft SMS message for this thread
		deleteDraftSmsMessage(threadId);

		// Make sure this thread isn't over the limits in message count
//		Recycler.getMessageRecycler().deleteOldMessagesByThreadId(mContext, threadId);

		
	}

	private void markMmsMessageWithError(Uri mmsUri) {
		try {
			PduPersister p = PduPersister.getPduPersister(mContext);
			// Move the message into MMS Outbox. A trigger will create an entry in
			// the "pending_msgs" table.
			p.move(mmsUri, VZUris.getMmsOutboxUri());

			// Now update the pending_msgs table with an error for that new item.
			ContentValues values = new ContentValues(1);
			values.put(PendingMessages.ERROR_TYPE, MmsSms.ERR_TYPE_GENERIC_PERMANENT);
			final String where = PendingMessages.MSG_ID + "=" + ContentUris.parseId(mmsUri);
			SqliteWrapper.update(mContext, mContentResolver, VZUris.getMmsSmsPendingUri(), values, where, null);
		} catch (MmsException e) {
			// Not much we can do here. If the p.move throws an exception, we'll just
			// leave the message in the draft box.
			Logger.error(getClass(),"Failed to move message to outbox and mark as error: " + mmsUri, e);
		}
	}

	// Draft message stuff

	private static final String[] MMS_DRAFT_PROJECTION = { Mms._ID, // 0
			Mms.SUBJECT, // 1
			Mms.SUBJECT_CHARSET // 2
	};

	private static final int MMS_ID_INDEX = 0;
	private static final int MMS_SUBJECT_INDEX = 1;
	private static final int MMS_SUBJECT_CS_INDEX = 2;

	private Uri readDraftMmsMessage(Context context, long threadId, StringBuilder sb) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("readDraftMmsMessage tid = " + threadId);
		}
		Cursor cursor;
		ContentResolver cr = context.getContentResolver();

		final String selection = Mms.THREAD_ID + " = " + threadId;
		cursor = SqliteWrapper.query(context, cr, VZUris.getMmsDraftsUri(), MMS_DRAFT_PROJECTION, selection, null, null);

		Uri uri;
		if(cursor !=null){
		try {
			if (cursor.moveToFirst()) {
				uri = ContentUris.withAppendedId(VZUris.getMmsDraftsUri(), cursor.getLong(MMS_ID_INDEX));
				String subject = MessageUtils.extractEncStrFromCursor(cursor, MMS_SUBJECT_INDEX, MMS_SUBJECT_CS_INDEX);
				if (subject != null) {
					sb.append(subject);
				}
				mDraftThreadId = threadId;
				return uri;
			}
		} finally {
			cursor.close();
		}
		}
		return null;
	}

	/**
	 * makeSendReq should always return a non-null SendReq, whether the dest addresses are
	 * valid or not.
	 */
	private SendReq makeSendReq(Conversation conv, CharSequence subject) {
		String[] dests = conv.getRecipients().getNumbers(true /* scrub for MMS address */);

		SendReq req = new SendReq();
		EncodedStringValue[] encodedNumbers = EncodedStringValue.encodeStrings(dests);
		if (encodedNumbers != null) {
			// if there are multiple recipients and the group MMS flag isn't set then
			// send it as "mass text", putting the recipients in the Bcc: instead of To:
			if (encodedNumbers.length > 1 && !isGroupMms()) {
				req.setBcc(encodedNumbers);
			}
			else {
				req.setTo(encodedNumbers);
			}
		}

		if (!TextUtils.isEmpty(subject)) {
			req.setSubject(new EncodedStringValue(subject.toString()));
		}

		req.setDate(System.currentTimeMillis() / 1000L);

		return req;
	}

	private static Uri createDraftMmsMessage(PduPersister persister, SendReq sendReq, SlideshowModel slideshow) {
		try {
			PduBody pb = slideshow.toPduBody();
			sendReq.setBody(pb);
			Uri res = persister.persist(sendReq, VZUris.getMmsDraftsUri());
			
			slideshow.sync(pb);
			return res;
		} catch (MmsException e) {
			Logger.error(WorkingMessage.class, e);
			return null;
		}
	}

	private void asyncUpdateDraftMmsMessage(final Conversation conv) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "asyncUpdateDraftMmsMessage: conv = " + conv + ", uri = " + mMessageUri);
		}

		final PduPersister persister = PduPersister.getPduPersister(mContext);
		final SendReq sendReq = makeSendReq(conv, mSubject);

		new Thread(new Runnable() {
			public void run() {
				final long threadId = conv.ensureThreadId();
				conv.setDraftState(true);
				if (mMessageUri == null) {
					mMessageUri = createDraftMmsMessage(persister, sendReq, mSlideshow);
				} else {
					updateDraftMmsMessage(mMessageUri, persister, mSlideshow, sendReq);
				}

				// if the thread id changed (e.g. due to recipients changing) then delete old drafts
				final long draftThreadId = mDraftThreadId;
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "asyncUpdateDraftMmsMessage: draft/new thread id = " + draftThreadId + " / " + threadId);
				}
				if (draftThreadId != 0 && draftThreadId != threadId) {
					asyncDeleteDraftSmsMessage(draftThreadId);
					asyncDeleteDraftMmsMessage(draftThreadId);
				}

				// Be paranoid and delete any SMS drafts that might be lying around. Must do
				// this after ensureThreadId so conv has the correct thread id.
				asyncDeleteDraftSmsMessage(conv.getThreadId());
			}
		}).start();
	}

	private static void updateDraftMmsMessage(Uri uri, PduPersister persister, SlideshowModel slideshow, SendReq sendReq) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(WorkingMessage.class, "updateDraftMmsMessage: uri = " + uri);
		}
		if (uri == null) {
			Logger.error(WorkingMessage.class, "updateDraftMmsMessage: null uri");
			return;
		}
		try {
			// update the headers, overriding any previously saved recipients since we may now
			// be sending as Bcc instead of To or vice-versa
			persister.updateHeaders(uri, sendReq, false, true);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		final PduBody pb = slideshow.toPduBody();

		try {
			persister.updateParts(uri, pb);
		} catch (MmsException e) {
			Logger.error(WorkingMessage.class, "updateDraftMmsMessage: cannot update message " + uri, e);
		}

		slideshow.sync(pb);
	}

	private static final String SMS_DRAFT_WHERE = Sms.TYPE + "=" + Sms.MESSAGE_TYPE_DRAFT;
	private static final String[] SMS_BODY_PROJECTION = { Sms.BODY };
	private static final int SMS_BODY_INDEX = 0;

	/**
	 * Reads a draft message for the given thread ID from the database,
	 * if there is one, deletes it from the database, and returns it.
	 * 
	 * @return The draft message or an empty string.
	 */
	private String readDraftSmsMessage(Conversation conv) {
		long threadId = conv.getThreadId();
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "readDraftSmsMessage: tid = " + threadId);
		}
		// If it's an invalid thread or we know there's no draft, don't bother.
		if (threadId <= 0 || !conv.hasDraft()) {
			return "";
		}

		Uri threadUri = ContentUris.withAppendedId(VZUris.getSmsConversations(), threadId);
		String body = "";

		Cursor c = SqliteWrapper.query(mContext, mContentResolver, threadUri, SMS_BODY_PROJECTION, SMS_DRAFT_WHERE,
				null, null);
		boolean haveDraft = false;
		if (c != null) {
			try {
				if (c.moveToFirst()) {
					body = c.getString(SMS_BODY_INDEX);
					haveDraft = true;
				}
			} finally {
				c.close();
			}
		}

		// We found a draft, and if there are no messages in the conversation,
		// that means we deleted the thread, too. Must reset the thread id
		// so we'll eventually create a new thread.
		if (haveDraft) {
			mDraftThreadId = threadId;
			if (conv.getMessageCount() == 0) {
				// Clean out drafts for this thread -- if the recipient set changes,
				// we will lose track of the original draft and be unable to delete
				// it later. The message will be re-saved if necessary upon exit of
				// the activity.
				clearConversation(conv);
				conv.clearThreadId();
			}
		}

		return body;
	}

	private void clearConversation(final Conversation conv) {
		asyncDeleteDraftSmsMessage(conv.getThreadId());

		//the messagecount is initialized only when we 
		//create conversation from fillFromCursor in other cases
		//the mMessageCount will be 0 so it is wrong to clear the thread id
		//based on it
	    
	    //Update:12/06/2012
		// Reason for not putting here is that : on new compose screen when
		// message is the message count is not updated hence 0 so on discarding
		// message thread id is lost.
		// if (conv.getMessageCount() == 0) {
		// if (DEBUG)
		// Logger.debug("clearConversation calling clearThreadId");
		// conv.clearThreadId();
		// }

		conv.setDraftState(false);

	}

	private void asyncUpdateDraftSmsMessage(final Conversation conv, final String contents) {
		new Thread(new Runnable() {
			public void run() {
				long threadId = conv.ensureThreadId();
				conv.setDraftState(true);
				updateDraftSmsMessage(threadId, contents);

				// if the thread id changed (e.g. due to recipients changing) then delete old drafts
				final long draftThreadId = mDraftThreadId;
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "asyncUpdateDraftSmsMessage: draft/new thread id = " + draftThreadId + " / " + threadId);
				}
				if (draftThreadId != 0 && draftThreadId != threadId) {
					asyncDeleteDraftSmsMessage(draftThreadId);
					asyncDeleteDraftMmsMessage(draftThreadId);
				}
			}
		}).start();
	}

	private void updateDraftSmsMessage(long thread_id, String contents) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "updateDraftSmsMessage: tid = " + thread_id + ", contents = <" + contents + ">");
		}

		// If we don't have a valid thread, there's nothing to do.
		if (thread_id <= 0) {
			return;
		}

		ContentValues values = new ContentValues(3);
		values.put(Sms.THREAD_ID, thread_id);
		values.put(Sms.BODY, contents);
		values.put(Sms.TYPE, Sms.MESSAGE_TYPE_DRAFT);
		SqliteWrapper.insert(mContext, mContentResolver, VZUris.getSmsUri(), values);
		asyncDeleteDraftMmsMessage(thread_id);
	}

	private void asyncDelete(final Uri uri, final String selection, final String[] selectionArgs) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "asyncDelete: uri = " + uri + ", where = " + selection + ", args = " + Arrays.toString(selectionArgs));
		}
		new Thread(new Runnable() {
			public void run() {
				SqliteWrapper.delete(mContext, mContentResolver, uri, selection, selectionArgs);
			}
		}).start();
	}

	private void asyncDeleteDraftSmsMessage(long threadId) {
		if (threadId > 0) {
			asyncDelete(ContentUris.withAppendedId(VZUris.getSmsConversations(), threadId), SMS_DRAFT_WHERE, null);
		}
	}

	private void deleteDraftSmsMessage(long threadId) {
		SqliteWrapper.delete(mContext, mContentResolver,
				ContentUris.withAppendedId(VZUris.getSmsConversations(), threadId), SMS_DRAFT_WHERE, null);
	}

	private void asyncDeleteDraftMmsMessage(long threadId) {
		final String where = Mms.THREAD_ID + " = " + threadId;
		asyncDelete(VZUris.getMmsDraftsUri(), where, null);
	}

    /**
     * This Method deletes the temp files if it is not being used
     * @param uri
     * @param dataUri
     */
    public static void freeScrapSpace(Uri oldUri, Uri newUri) {
        String oldPath = oldUri != null? oldUri.getPath() : null;
        String newPath = newUri != null? newUri.getPath() : null;
        if (!TextUtils.isEmpty(oldPath) && !TextUtils.isEmpty(newPath)) {
            if (!oldPath.equals(newPath)) {
                removeFile(oldUri);
            }
        }
    }
    
   }
