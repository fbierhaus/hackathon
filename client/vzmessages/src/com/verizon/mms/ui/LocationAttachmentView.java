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
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.data.Contact;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;

/**
 * This class provides an embedded editor/viewer of audio attachment.
 */
public class LocationAttachmentView extends LinearLayout implements
        SlideViewInterface {
    private static final String TAG = "LocationAttachmentView";

    private final Resources mRes;
    
    private TextView mLocationText;
    private ImageView mLocationPicture;
    private Context mContext;

    public LocationAttachmentView(Context context) {
        super(context);
        mRes = context.getResources();
        mContext = context;
    }

    public LocationAttachmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRes = context.getResources();
    }

    @Override
    protected void onFinishInflate() {
        /*mLocationText = (TextView) findViewById(R.id.locationText);
        mLocationPicture = (ImageView) findViewById(R.id.locationPicture);*/
    }

    public void startAudio() {
    }

    public void startVideo() {
        // TODO Auto-generated method stub

    }

    public void setAudio(Uri audio, String name, Map<String, ?> extras) {
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
        // TODO Auto-generated method stub

    }

    public void setVideoVisibility(boolean visible) {
        // TODO Auto-generated method stub

    }

    synchronized public void stopAudio() {
    }

    public void stopVideo() {
        // TODO Auto-generated method stub

    }

    public void reset() {
    }

    public void setVisibility(boolean visible) {
        // TODO Auto-generated method stub

    }

    private void showErrorMessage(String msg) {
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
        if (name != null) {
            mLocationText.setVisibility(View.VISIBLE);
            mLocationText.setText(name);
        }
        if (bitmap != null) {
            mLocationPicture.setImageBitmap(bitmap);
        } else {
            mLocationPicture.setImageResource(R.drawable.attach_location);
        }
    }

    @Override
    public void showVCard(ContactStruct contactStruct) {
        String email = contactStruct.getFirstEmail();
        String number = contactStruct.getFirstNumber();
        
        Contact contact = null;
        if(!TextUtils.isEmpty(number)) {
            contact = Contact.get(number, true);
        } else if(!TextUtils.isEmpty(email)) {
            contact = Contact.get(email, true);
        }
        if(contact != null && contact.existsInDatabase())
        {
            Uri uri = contact.getUri();
            Intent intent = new Intent("android.intent.action.VIEW", uri);
            mContext.startActivity(intent);
        } else {
            Toast.makeText(mContext, R.string.contact_has_no_address, Toast.LENGTH_LONG).show();
        }
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
