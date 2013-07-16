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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.smil.ElementTime;

import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ContentRestrictionException;
import com.verizon.mms.ContentType;
import com.verizon.mms.dom.smil.SmilParElementImpl;

public class SlideModel extends Model implements EventListener {
    private static final int DEFAULT_SLIDE_DURATION = 5000;

    private final ArrayList<MediaModel> mMedia = new ArrayList<MediaModel>();

    private MediaModel mText;
    private MediaModel mImage;
    private MediaModel mAudio;
    private MediaModel mVideo;
    private MediaModel mVcard;
    private MediaModel mLocation;

    private boolean mCanAddImage = true;
    private boolean mCanAddAudio = true;
    private boolean mCanAddVideo = true;
    private boolean mCanAddVcard = true;
    private boolean mCanAddLocation = true;

    private int mDuration;
    private boolean mVisible = true;
    private short mFill;
    private int mSlideSize;
    private SlideshowModel mParent;

    public SlideModel(SlideshowModel slideshow) {
        this(DEFAULT_SLIDE_DURATION, slideshow);
    }

    public SlideModel(int duration, SlideshowModel slideshow) {
        mDuration = duration;
        mParent = slideshow;
    }

    /**
     * Create a SlideModel with exist media collection.
     *
     * @param duration The duration of the slide.
     * @param mediaList The exist media collection.
     *
     * @throws IllegalStateException One or more media in the mediaList cannot
     *         be added into the slide due to a slide cannot contain image
     *         and video or audio and video at the same time.
     */
    public SlideModel(int duration, ArrayList<MediaModel> mediaList) {
        mDuration = duration;

        int maxDur = 0;
        for (MediaModel media : mediaList) {
            internalAdd(media);

            int mediaDur = media.getDuration();
            if (mediaDur > maxDur) {
                maxDur = mediaDur;
            }
        }

        updateDuration(maxDur);
    }

    private void internalAdd(MediaModel media) throws IllegalStateException {
        if (media == null) {
            // Don't add null value into the list.
            return;
        }

        if (media.isText()) {
            String contentType = media.getContentType();
            if (TextUtils.isEmpty(contentType) || ContentType.TEXT_PLAIN.equals(contentType)
                    || ContentType.TEXT_HTML.equals(contentType)) {
                internalAddOrReplace(mText, media);
                mText = media;
            } else {
            	Logger.warn(getClass(), "[SlideModel] content type " + media.getContentType() +
                        " isn't supported (as text)");
			}
        } else if (media.isImage()) {
            if (mCanAddImage) {
                internalAddOrReplace(mImage, media);
                mImage = media;
                mCanAddVideo = false;
                mCanAddVcard = false;
            } else {
                throw new IllegalStateException();
            }
        } else if (media.isAudio()) {
            if (mCanAddAudio) {
                internalAddOrReplace(mAudio, media);
                mAudio = media;
                mCanAddVideo = false;
                mCanAddVcard = false;
                mCanAddLocation = false;
            } else {
                throw new IllegalStateException();
            }
        } else if (media.isVideo()) {
            if (mCanAddVideo) {
                internalAddOrReplace(mVideo, media);
                mVideo = media;
                mCanAddImage = false;
                mCanAddAudio = false;
                mCanAddVcard = false;
                mCanAddLocation = false;
            } else {
                throw new IllegalStateException();
            }
        } else if (media.isVcard()) {
            if (mCanAddVcard) {
                internalAddOrReplace(mVcard, media);
                mVcard = media;
                mCanAddLocation = false;
                mCanAddAudio = false;
                mCanAddVideo = false;
                mCanAddImage = false;
            }
        } else if (media.isLocation()) {
            if (mCanAddLocation) {
                internalAddOrReplace(mLocation, media);
                mLocation = media;
                mCanAddVcard = false;
                mCanAddAudio = false;
                mCanAddVideo = false;
            }
        }
    }

    private void internalAddOrReplace(MediaModel old, MediaModel media) {
        // If the media is resizable, at this point consider it to be zero length.
        // Just before we send the slideshow, we take the remaining space in the
        // slideshow and equally allocate it to all the resizeable media items and resize them.
        int addSize = media.getMediaResizable() ? 0 : media.getMediaSize();
        int removeSize;
		synchronized (mMedia) {
	        if (old == null) {
	            if (null != mParent) {
	                mParent.checkMessageSize(addSize);
	            }
	            mMedia.add(media);
	            increaseSlideSize(addSize);
	        } else {
	            removeSize = old.getMediaSize();
	            if (addSize > removeSize) {
	                if (null != mParent) {
	                    mParent.checkMessageSize(addSize - removeSize);
	                }
	                increaseSlideSize(addSize - removeSize);
	            } else {
	                decreaseSlideSize(removeSize - addSize);
	            }
	            mMedia.set(mMedia.indexOf(old), media);
	            old.unregisterAllModelChangedObservers();
	        }
		}

        for (IModelChangedObserver observer : getModelChangedObservers()) {
            media.registerModelChangedObserver(observer);
        }
    }

    private boolean internalRemove(Object object) {
    	final boolean removed;
		synchronized (mMedia) {
	        removed = mMedia.remove(object);
		}
        if (removed) {
            if (object instanceof TextModel) {
                mText = null;
            } else if (object instanceof ImageModel) {
                mImage = null;
                mCanAddVideo = true;
            } else if (object instanceof AudioModel) {
                mAudio = null;
                mCanAddVideo = true;
            } else if (object instanceof VideoModel) {
                mVideo = null;
                mCanAddImage = true;
                mCanAddAudio = true;
            } else if (object instanceof VCardModel) {
                mVcard = null;
                mCanAddVcard = true;
            } else if (object instanceof LocationModel) {
                mLocation = null;
                mCanAddLocation = true;
            }
            // If the media is resizable, at this point consider it to be zero length.
            // Just before we send the slideshow, we take the remaining space in the
            // slideshow and equally allocate it to all the resizeable media items and resize them.
            int decreaseSize = ((MediaModel) object).getMediaResizable() ? 0
                                        : ((MediaModel) object).getMediaSize();
            decreaseSlideSize(decreaseSize);

            ((Model) object).unregisterAllModelChangedObservers();
        }

        return removed;
    }

    /**
     * @return the mDuration
     */
    public int getDuration() {
        return mDuration;
    }

    /**
     * @param duration the mDuration to set
     */
    public void setDuration(int duration) {
        mDuration = duration;
        notifyModelChanged(true);
    }

    public int getSlideSize() {
        return mSlideSize;
    }

	/**
	 * @return the amount of memory in bytes currently used by the slide's media content
	 */
	public int getMemorySize() {
		int size = 0;
		synchronized (mMedia) {
			for (MediaModel media : mMedia) {
				size += media.getMemorySize();
			}
		}
		return size;
	}

    public void increaseSlideSize(int increaseSize) {
        if (increaseSize > 0) {
            mSlideSize += increaseSize;
        }
    }

    public void decreaseSlideSize(int decreaseSize) {
        if (decreaseSize > 0) {
            mSlideSize -= decreaseSize;
        }
    }

    public void setParent(SlideshowModel parent) {
        mParent = parent;
    }

    //
    // Implement List<E> interface.
    //

    /**
     * Add a MediaModel to the slide. If the slide has already contained
     * a media object in the same type, the media object will be replaced by
     * the new one.
     *
     * @param object A media object to be added into the slide.
     * @return true
     * @throws IllegalStateException One or more media in the mediaList cannot
     *         be added into the slide due to a slide cannot contain image
     *         and video or audio and video at the same time.
     * @throws ContentRestrictionException when can not add this object.
     *
     */
    public boolean add(MediaModel object) {
        internalAdd(object);
        notifyModelChanged(true);
        return true;
    }

    private boolean remove(Object object) {
        if ((object != null) && (object instanceof MediaModel)
                && internalRemove(object)) {
            notifyModelChanged(true);
            return true;
        }
        return false;
    }

	@SuppressWarnings("unchecked")
	public List<MediaModel> getMedia() {
		synchronized (mMedia) {
			return (List<MediaModel>)mMedia.clone();
		}
	}

    /**
     * @return the mVisible
     */
    public boolean isVisible() {
        return mVisible;
    }

    /**
     * @param visible the mVisible to set
     */
    public void setVisible(boolean visible) {
        mVisible = visible;
        notifyModelChanged(true);
    }

    /**
     * @return the mFill
     */
    public short getFill() {
        return mFill;
    }

    /**
     * @param fill the mFill to set
     */
    public void setFill(short fill) {
        mFill = fill;
        notifyModelChanged(true);
    }

    @Override
    protected void registerModelChangedObserverInDescendants(IModelChangedObserver observer) {
		synchronized (mMedia) {
	        for (MediaModel media : mMedia) {
	            media.registerModelChangedObserver(observer);
	        }
		}
    }

    @Override
    protected void unregisterModelChangedObserverInDescendants(IModelChangedObserver observer) {
		synchronized (mMedia) {
	        for (MediaModel media : mMedia) {
	            media.unregisterModelChangedObserver(observer);
	        }
		}
    }

    @Override
    protected void unregisterAllModelChangedObserversInDescendants() {
		synchronized (mMedia) {
	        for (MediaModel media : mMedia) {
	            media.unregisterAllModelChangedObservers();
	        }
		}
    }

    // EventListener Interface
    public void handleEvent(Event evt) {
        if (evt.getType().equals(SmilParElementImpl.SMIL_SLIDE_START_EVENT)) {
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "Start to play slide: " + this);
            }
            mVisible = true;
        } else if (mFill != ElementTime.FILL_FREEZE) {
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "Stop playing slide: " + this);
            }
            mVisible = false;
        }

        notifyModelChanged(false);
    }

    public boolean isTextOnly() {
        return mImage == null && mAudio == null && 
                mVideo == null && mVcard == null &&
                mLocation == null;
    }

    public boolean hasText() {
        return mText != null;
    }

    public boolean hasImage() {
        return mImage != null;
    }

    public boolean hasAudio() {
        return mAudio != null;
    }

    public boolean hasVideo() {
        return mVideo != null;
    }

    public boolean hasVCard() {
        return mVcard != null;
    }
    
    public boolean removeText() {
        return remove(mText);
    }
    
    public boolean hasLocation() {
        return mLocation != null;
    }

    public boolean removeImage() {
        return remove(mImage);
    }

    public boolean removeAudio() {
        return remove(mAudio);
    }
    
    public boolean removeVCard() {
        return remove(mVcard);
    }
    
    public boolean removeLocation() {
        return remove(mLocation);
    }

    public boolean removeVideo() {
        return remove(mVideo);
    }

    public TextModel getText() {
        return (TextModel) mText;
    }

    public ImageModel getImage() {
        return (ImageModel) mImage;
    }

    public AudioModel getAudio() {
        return (AudioModel) mAudio;
    }

    public VideoModel getVideo() {
        return (VideoModel) mVideo;
    }
    
    public VCardModel getVCard() {
        return (VCardModel) mVcard;
    }
    
    public LocationModel getLocation() {
        return (LocationModel) mLocation;
    }

    public void updateDuration(int duration) {
        if (duration <= 0) {
            return;
        }

        if ((duration > mDuration)
                || (mDuration == DEFAULT_SLIDE_DURATION)) {
            mDuration = duration;
        }
    }

    public String toString() {
		synchronized (mMedia) {
			return mMedia.toString();
		}
    }
    
    public boolean hasMedia() {
        return mImage != null || mAudio != null || mVideo != null ||
                mVcard != null || mLocation != null;
    }
}
