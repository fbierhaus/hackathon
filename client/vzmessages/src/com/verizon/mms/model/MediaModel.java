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

package com.verizon.mms.model;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.events.EventListener;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.drm.mobile1.DrmException;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsException;
import com.verizon.mms.drm.DrmUtils;
import com.verizon.mms.drm.DrmWrapper;

public abstract class MediaModel extends Model implements EventListener {
	private final static String MUSIC_SERVICE_ACTION = "com.android.music.musicservicecommand";

	protected Context mContext;
	protected int mBegin;
	protected int mDuration;
	private MediaType mType;
	protected String mSrc;
	protected String mContentType;
	private Uri mUri;
	private byte[] mData;
	protected short mFill;
	protected int mSize;
	protected int mMemorySize;
	protected int mSeekTo;
	protected DrmWrapper mDrmObjectWrapper;
	protected boolean mMediaResizeable;
	protected String mContentDisposition;

	private final ArrayList<MediaAction> mMediaActions;


	private static enum MediaType {
		TEXT(SmilHelper.ELEMENT_TAG_TEXT),
		IMAGE(SmilHelper.ELEMENT_TAG_IMAGE),
		VIDEO(SmilHelper.ELEMENT_TAG_VIDEO),
		AUDIO(SmilHelper.ELEMENT_TAG_AUDIO),
		VCARD(SmilHelper.ELEMENT_TAG_VCARD),
		LOCATION(SmilHelper.ELEMENT_TAG_LOCATION),
		GENERIC(SmilHelper.ELEMENT_TAG_REF);

		private String tag;
		private static HashMap<String, MediaType> map;

		static {
			map = new HashMap<String, MediaType>(values().length);
			for (MediaType type : values()) {
				map.put(type.tag, type);
			}
		}

		private MediaType(String tag) {
			this.tag = tag;
		}

		private static MediaType get(String tag) {
			return map.get(tag);
		}
	}


	public static enum MediaAction {
		NO_ACTIVE_ACTION, START, STOP, PAUSE, SEEK,
	}

	public MediaModel(Context context, String tag, String contentType,
			String src, Uri uri) throws MmsException {
		mContext = context;
		mType = MediaType.get(tag);
		mContentType = contentType;
		mSrc = src;
		mUri = uri;
		initMediaSize();
		mMediaActions = new ArrayList<MediaAction>();
	}

	public MediaModel(Context context, String tag, String contentType,
			String src, byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException("data may not be null.");
		}

		mContext = context;
		mType = MediaType.get(tag);
		mContentType = contentType;
		mSrc = src;
		mData = data;
		mSize = data.length;
		mMediaActions = new ArrayList<MediaAction>();
	}

	public MediaModel(Context context, String tag, String contentType,
			String src, DrmWrapper wrapper) throws IOException {
		mContext = context;
		mType = MediaType.get(tag);
		mContentType = contentType;
		mSrc = src;
		mDrmObjectWrapper = wrapper;
		mUri = DrmUtils.insert(context, wrapper);
		mSize = wrapper.getOriginalData().length;
		mMediaActions = new ArrayList<MediaAction>();
	}

	public int getBegin() {
		return mBegin;
	}

	public void setBegin(int begin) {
		mBegin = begin;
		notifyModelChanged(true);
	}
	
	/**
	 * 
	 * This Method will be used for checking Content disposition for thubmail
	 */
	public void setContentDisposition(String contentDispo) {
        mContentDisposition = contentDispo;
    }

	public String getContentDisposition() {
        return mContentDisposition;
    }
	
	public int getDuration() {
		return mDuration;
	}

	public void setDuration(int duration) {
		if (isPlayable() && (duration < 0)) {
			// 'indefinite' duration, we should try to find its exact value;
			try {
				initMediaDuration();
			} catch (MmsException e) {
				// On error, keep default duration.
				Logger.error(getClass(), e.getMessage(), e);
				return;
			}
		} else {
			mDuration = duration;
		}
		notifyModelChanged(true);
	}

	public String getContentType() {
		return mContentType;
	}

	/**
	 * Get the URI of the media without checking DRM rights. Use this method
	 * only if the media is NOT DRM protected.
	 * 
	 * @return The URI of the media.
	 */
	public Uri getUri() {
		return mUri;
	}

	/**
	 * Get the URI of the media with checking DRM rights. Use this method if the
	 * media is probably DRM protected.
	 * 
	 * @return The URI of the media.
	 * @throws DrmException
	 *             Insufficient DRM rights detected.
	 */
	public Uri getUriWithDrmCheck() throws DrmException {
		if (mUri != null) {
			if (isDrmProtected() && !mDrmObjectWrapper.consumeRights()) {
				throw new DrmException("Insufficient DRM rights.");
			}
		}
		return mUri;
	}

	public byte[] getData() throws DrmException {
		if (mData != null) {
			if (isDrmProtected() && !mDrmObjectWrapper.consumeRights()) {
				throw new DrmException(
						mContext.getString(R.string.insufficient_drm_rights));
			}

			byte[] data = new byte[mData.length];
			System.arraycopy(mData, 0, data, 0, mData.length);
			return data;
		}
		return null;
	}

	/**
	 * @param uri
	 *            the mUri to set
	 */
	void setUri(Uri uri) {
		mUri = uri;
	}

	/**
	 * @return the mSrc
	 */
	public String getSrc() {
		return mSrc;
	}

	/**
	 * @return the mFill
	 */
	public short getFill() {
		return mFill;
	}

	/**
	 * @param fill
	 *            the mFill to set
	 */
	public void setFill(short fill) {
		mFill = fill;
		notifyModelChanged(true);
	}

	/**
	 * @return whether the media is resizable or not. For instance, a picture
	 *         can be resized to smaller dimensions or lower resolution. Other
	 *         media, such as video and sounds, aren't currently able to be
	 *         resized.
	 */
	public boolean getMediaResizable() {
		return mMediaResizeable;
	}

	/**
	 * @return the size of the attached media
	 */
	public int getMediaSize() {
		return mSize;
	}

	/**
	 * @return the amount of memory in bytes currently used by the media content
	 */
	public int getMemorySize() {
		return mMemorySize;
	}

	public boolean isText() {
		return mType == MediaType.TEXT;
	}

	public boolean isImage() {
		return mType == MediaType.IMAGE;
	}

	public boolean isVideo() {
		return mType == MediaType.VIDEO;
	}

	public boolean isAudio() {
		return mType == MediaType.AUDIO;
	}

	public boolean isVcard() {
		return mType == MediaType.VCARD;
	}

	public boolean isLocation() {
		return mType == MediaType.LOCATION;
	}

	public boolean isDrmProtected() {
		return mDrmObjectWrapper != null;
	}

	public boolean isAllowedToForward() {
		return mDrmObjectWrapper.isAllowedToForward();
	}

	protected void initMediaDuration() throws MmsException {
		if (mUri == null) {
			throw new IllegalArgumentException("Uri may not be null.");
		}

		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		// Fix for ==> http://50.17.243.155/bugzilla/show_bug.cgi?id=25
		// Not public, don't use it.
		// try {
		// retriever.setMode(MediaMetadataRetriever.MODE_GET_METADATA_ONLY);
		// } catch (NoSuchMethodError localNoSuchMethodError) {
		// } catch (UnsupportedOperationException
		// localUnsupportedOperationException) {
		// } catch (UnsatisfiedLinkError localUnsatisfiedLinkError) {
		// }

		int duration = 0;
		try {
			retriever.setDataSource(mContext, mUri);
			String dur = retriever
					.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			if (dur != null) {
				duration = Integer.parseInt(dur);
			}
			mDuration = duration;
		} catch (Exception ex) {
			Logger.error(getClass(), "MediaMetadataRetriever failed to get duration for "
					+ mUri.getPath(), ex);
			//duration is not mandatory as we are not supporting slideshow
			//throw new MmsException(ex);
		} finally {
			retriever.release();
		}
	}

	private void initMediaSize() throws MmsException {
		ContentResolver cr = mContext.getContentResolver();
		InputStream input = null;
		try {
			input = cr.openInputStream(mUri);
			if (input instanceof FileInputStream) {
				// avoid reading the whole stream to get its length
				FileInputStream f = (FileInputStream) input;
				mSize = (int) f.getChannel().size();
			} else {
				while (-1 != input.read()) {
					mSize++;
				}
			}

		} catch (IOException e) {
			// Ignore
			Logger.error(getClass(), "IOException caught while opening or reading stream", e);
			if (e instanceof FileNotFoundException) {
				throw new MmsException(e.getMessage());
			}
		} finally {
			if (null != input) {
				try {
					input.close();
				} catch (IOException e) {
					// Ignore
					Logger.error(getClass(), "IOException caught while closing stream", e);
				}
			}
		}
	}

	public static boolean isMmsUri(Uri uri) {
	    String auth = uri.getAuthority();
	    
	    if (auth == null) {
	        return false;
	    }
		return auth.startsWith(VZUris.getMmsUri().getAuthority());
	}

	public int getSeekTo() {
		return mSeekTo;
	}

	public void appendAction(MediaAction action) {
		mMediaActions.add(action);
	}

	public MediaAction getCurrentAction() {
		if (0 == mMediaActions.size()) {
			return MediaAction.NO_ACTIVE_ACTION;
		}
		return mMediaActions.remove(0);
	}

	protected boolean isPlayable() {
		return false;
	}

	public DrmWrapper getDrmObject() {
		return mDrmObjectWrapper;
	}

	protected void pauseMusicPlayer() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "pauseMusicPlayer");
		}

		Intent i = new Intent(MUSIC_SERVICE_ACTION);
		i.putExtra("command", "pause");
		mContext.sendBroadcast(i);
	}

	/**
	 * If the attached media is resizeable, resize it to fit within the
	 * byteLimit. Save the new part in the pdu.
	 * 
	 * @param byteLimit
	 *            the max size of the media attachment
	 * @throws MmsException
	 */
	protected void resizeMedia(int byteLimit, long messageId)
			throws MmsException {
	}

	@Override
	public String toString() {
		return super.toString() + ": type = " + mType + ", uri = " + mUri + ", size = " + mSize +
			", memSize = " + mMemorySize + ", begin = " + mBegin + ", duration = " + mDuration;
	}
	
	/*
	 * Checks if the media pointed by the uri is loaded from the disk
	 */
	public boolean isLoaded(Rect dims) {
		return true;
	}
	
	public void resetIfLoadRequired() {
		
	}
}
