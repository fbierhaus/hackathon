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

import java.io.IOException;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.util.BitmapManager;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;

/**
 * This class provides an embedded editor/viewer of slide-show attachment.
 */
public class SlideshowAttachmentView extends LinearLayout implements
        SlideViewInterface {
    private ImageView mImageView;
    private TextView mTextView;

    public SlideshowAttachmentView(Context context) {
        super(context);
    }

    public SlideshowAttachmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        mImageView = (ImageView) findViewById(R.id.slideshow_image);
        mTextView = (TextView) findViewById(R.id.slideshow_text);
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
        if (null == bitmap) {
            try {
                bitmap = BitmapManager.INSTANCE.decodeResource(getResources(),
                        R.drawable.ic_missing_thumbnail_picture);
            } catch (java.lang.OutOfMemoryError e) {
                // We don't even have enough memory to load the "missing thumbnail" image
            }
        }
        if (bitmap != null) {
            mImageView.setImageBitmap(bitmap);      // implementation doesn't appear to be null-safe
        }
    }

    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
    }

    public void setImageVisibility(boolean visible) {
        mImageView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    public void setText(String name, String text) {
        mTextView.setText(text);
    }

    public void setTextVisibility(boolean visible) {
        mTextView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    public void setVideo(String name, Uri video) {
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(getContext(), video);
            //mImageView.setImageBitmap(mp.getFrameAt(1000));
        } catch (IOException e) {
            Logger.error(getClass(), "Unexpected IOException.", e);
        } finally {
            mp.release();
        }
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
        mImageView.setImageURI(null);
        mTextView.setText("");
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
    public void showVCard(ContactStruct contact) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setVCard(Uri uri, String name, Bitmap bitmap) {
        // TODO Auto-generated method stub
        
    }

    /* Overriding method 
     * (non-Javadoc)
     * @see com.verizon.mms.ui.SlideViewInterface#setLocation(android.net.Uri, java.lang.String)
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
