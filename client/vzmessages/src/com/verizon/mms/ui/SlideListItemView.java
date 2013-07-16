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
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
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
 * A simplified view of slide in the slides list.
 */
public class SlideListItemView extends LinearLayout implements SlideViewInterface {
    private TextView mTextPreview;
    private ImageView mImagePreview;
    private TextView mAttachmentName;
    private ImageView mAttachmentIcon;

    public SlideListItemView(Context context) {
        super(context);
    }

    public SlideListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        mTextPreview = (TextView) findViewById(R.id.text_preview);
        mTextPreview.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        mImagePreview = (ImageView) findViewById(R.id.image_preview);
        mAttachmentName = (TextView) findViewById(R.id.attachment_name);
        mAttachmentIcon = (ImageView) findViewById(R.id.attachment_icon);
    }

    public void startAudio() {
        // Playing audio is not needed in this view.
    }

    public void startVideo() {
        // Playing audio is not needed in this view.
    }

    public void setAudio(Uri audio, String name, Map<String, ?> extras) {
        if (name != null) {
            mAttachmentName.setText(name);
            mAttachmentIcon.setImageResource(R.drawable.ic_mms_music);
        } else {
            mAttachmentName.setText("");
            mAttachmentIcon.setImageDrawable(null);
        }
    }

    public void setImage(String name, Bitmap bitmap) {
        if (null == bitmap) {
            bitmap = BitmapManager.INSTANCE.decodeResource(getResources(),
                    R.drawable.ic_missing_thumbnail_picture);
        }
        mImagePreview.setImageBitmap(bitmap);
    }

    public void setImageRegionFit(String fit) {
        // TODO Auto-generated method stub
    }

    public void setImageVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void setText(String name, String text) {
        mTextPreview.setText(text);
        mTextPreview.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    public void setTextVisibility(boolean visible) {
        // TODO Auto-generated method stub
    }

    public void setVideo(String name, Uri video) {
        if (name != null) {
            mAttachmentName.setText(name);
            mAttachmentIcon.setImageResource(R.drawable.movie);
        } else {
            mAttachmentName.setText("");
            mAttachmentIcon.setImageDrawable(null);
        }

        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(getContext(), video);
            //mImagePreview.setImageBitmap(mp.getFrameAt(1000));
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
        // Stopping audio is not needed in this view.
    }

    public void stopVideo() {
        // Stopping video is not needed in this view.
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
