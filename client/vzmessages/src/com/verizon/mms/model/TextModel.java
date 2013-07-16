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

import java.io.IOException;

import org.w3c.dom.events.Event;
import org.w3c.dom.smil.ElementTime;

import android.content.Context;
import android.drm.mobile1.DrmException;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.dom.smil.SmilMediaElementImpl;
import com.verizon.mms.drm.DrmWrapper;
import com.verizon.mms.pdu.CharacterSets;
import com.verizon.mms.ui.MessageUtils;

public class TextModel extends RegionMediaModel {
    private CharSequence mText;
    private final int mCharset;

    public TextModel(Context context, String contentType, String src, RegionModel region) {
        this(context, contentType, src, CharacterSets.UTF_8, new byte[0], region);
    }

    public TextModel(Context context, String contentType, String src,
            int charset, byte[] data, RegionModel region) {
        super(context, SmilHelper.ELEMENT_TAG_TEXT, contentType, src,
                data != null ? data : new byte[0], region);

        if (charset == CharacterSets.ANY_CHARSET) {
            // By default, we use ISO_8859_1 to decode the data
            // which character set wasn't set.
            charset = CharacterSets.ISO_8859_1;
        }
        mCharset = charset;
        mText = MessageUtils.extractTextFromData(data, charset);
       	mMemorySize = mText.length() * 2;
    }

    public TextModel(Context context, String contentType, String src, int charset,
            DrmWrapper wrapper, RegionModel regionModel) throws IOException {
        super(context, SmilHelper.ELEMENT_TAG_TEXT, contentType, src, wrapper, regionModel);

        if (charset == CharacterSets.ANY_CHARSET) {
            // By default, we use ISO_8859_1 to decode the data
            // which character set wasn't set.
            charset = CharacterSets.ISO_8859_1;
        }
        mCharset = charset;
    }

    public String getText() {
        if (mText == null) {
            try {
                mText = MessageUtils.extractTextFromData(getData(), mCharset);
            } catch (DrmException e) {
            	Logger.error(getClass(), e.getMessage(), e);
                // Display DRM error message in place.
                mText = e.getMessage();
            }
           	mMemorySize = mText.length() * 2;
        }

        // If our internal CharSequence is not already a String,
        // re-save it as a String so subsequent calls to getText will
        // be less expensive.
        if (!(mText instanceof String)) {
            mText = mText.toString();
        }

        return mText.toString();
    }

    public void setText(CharSequence text) {
        mText = text;
       	mMemorySize = text == null ? 0 : text.length() * 2;
        notifyModelChanged(true);
    }

    public void cloneText() {
        mText = new String(mText.toString());
    }

    public int getCharset() {
        return mCharset;
    }

    // EventListener Interface
    public void handleEvent(Event evt) {
        if (evt.getType().equals(SmilMediaElementImpl.SMIL_MEDIA_START_EVENT)) {
            mVisible = true;
        } else if (mFill != ElementTime.FILL_FREEZE) {
            mVisible = false;
        }

        notifyModelChanged(false);
    }
}
