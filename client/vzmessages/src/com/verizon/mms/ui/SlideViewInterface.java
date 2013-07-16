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

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;

import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;

/**
 * Defines the interfaces of the view to show contents of a slide.
 */
public interface SlideViewInterface extends ViewInterface {
    void setImage(String name, Bitmap bitmap);
    void setImageRegionFit(String fit);
    void setImageVisibility(boolean visible);

    void setVideo(String name, Uri video);
    void setVideo(String name, Bitmap bitmap);
    void setVideoVisibility(boolean visible);
    void startVideo();
    void stopVideo();
    void pauseVideo();
    void seekVideo(int seekTo);

    void setAudio(Uri audio, String name, Map<String, ?> extras);
    void startAudio();
    void stopAudio();
    void pauseAudio();
    void seekAudio(int seekTo);
    
    void setVCard(Uri uri, String name, Bitmap bitmap);
    void showVCard(ContactStruct contact);

    void setLocation(Bitmap bitmap, String location);
    void setSlideShow();
    
    void setText(String name, String text);
    void setTextVisibility(boolean visible);

    Rect getImageDimensions();
}
