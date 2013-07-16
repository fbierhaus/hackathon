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

import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.util.BitmapManager;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;

/**
 * This class provides an embedded editor/viewer of video attachment.
 */
public class VideoAttachmentView extends LinearLayout implements
		SlideViewInterface {
	private ImageView mThumbnailView;

	private static boolean hasCaptureFrame = true;  // assume true until tried


	public VideoAttachmentView(Context context) {
		super(context);
	}

	public VideoAttachmentView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		mThumbnailView = (ImageView) findViewById(R.id.video_thumbnail);
	}

	public void startAudio() {
		// TODO Auto-generated method stub
	}

	public void startVideo() {
		// TODO Auto-generated method stub
	}

	public void setAudio(Uri audio, String name, Map<String, ?> extras) {
		// TODO Auto-generated method stub
	}

	public void setImage(String name, Bitmap bitmap) {
		// TODO Auto-generated method stub
	}

	public void setImageRegionFit(String fit) {
		// TODO Auto-generated method stub
	}

	public void setImageVisibility(boolean visible) {
		// TODO Auto-generated method stub
	}

	public void setText(String name, String text) {
		// TODO Auto-generated method stub
	}

	public void setTextVisibility(boolean visible) {
		// TODO Auto-generated method stub
	}

	public void setVideo(String name, Uri video) {
		Bitmap bitmap = createVideoThumbnail(getContext(), video);
		if (null == bitmap) {
			bitmap = BitmapManager.INSTANCE.decodeResource(getResources(),
					R.drawable.ic_missing_thumbnail_video);
		}
		mThumbnailView.setImageBitmap(bitmap);
	}

	public static Bitmap createVideoThumbnail(Context context, Uri uri) {
		Bitmap bitmap = null;
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			if (hasCaptureFrame) {
				retriever.setDataSource(context, uri);
				bitmap = retriever.captureFrame();
			}
			else {
				bitmap = getThumbnailAlternatively(context, retriever, uri);
			}
		}
		catch (Throwable t) {
			if (t instanceof NoSuchMethodError) {
				hasCaptureFrame = false;
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug("VideoAttachmentView: device doesn't have captureFrame, trying alternative");
				}
			}
			else {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug("error getting thumbnail for " + uri + ", trying alternative", t);
				}
			}
			bitmap = getThumbnailAlternatively(context, retriever, uri);
		}
		finally {
			try {
				retriever.release();
			} catch (Throwable t) {
				// Ignore failures while cleaning up.
			}
		}
		return bitmap;
	}

	// Fix for Bug 25
	private static Bitmap getThumbnailAlternatively(Context context,
			MediaMetadataRetriever retriever, Uri uri) {
		Bitmap localBitmap = null;
		try {
			retriever.setDataSource(context, uri);
			localBitmap = retriever.getFrameAtTime();
		}
		catch (Throwable t) {
			Logger.error("error getting thumbnail for " + uri, t);
		}

		return localBitmap;
	}

	public void setVideoVisibility(boolean visible) {
		// TODO Auto-generated method stub
	}

	public void stopAudio() {
		// TODO Auto-generated method stub
	}

	public void stopVideo() {
		// TODO Auto-generated method stub
	}

	public void reset() {
		// TODO Auto-generated method stub
	}

	public void setVisibility(boolean visible) {
		// TODO Auto-generated method stub
	}

	public void pauseAudio() {
		// TODO Auto-generated method stub

	}

	public void pauseVideo() {
		// TODO Auto-generated method stub

	}

	public void seekAudio(int seekTo) {
		// TODO Auto-generated method stub

	}

	public void seekVideo(int seekTo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVCard(Uri uri, String name, Bitmap bitmap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void showVCard(ContactStruct contact) {
		// TODO Auto-generated method stub

	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see com.verizon.mms.ui.SlideViewInterface#setLocation(android.net.Uri,
	 * java.lang.String)
	 */
	@Override
	public void setLocation(Bitmap bitmap, String location) {
		// TODO Auto-generated method stub

	}

    @Override
    public void setSlideShow() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setVideo(String name, Bitmap bitmap) {
        // TODO Auto-generated method stub
        
    }

	@Override
	public Rect getImageDimensions() {
		return null;
	}
}
