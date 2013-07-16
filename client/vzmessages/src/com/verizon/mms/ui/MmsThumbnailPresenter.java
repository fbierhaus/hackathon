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

import android.content.Context;
import android.drm.mobile1.DrmException;
import android.graphics.Bitmap;
import android.os.Handler;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.model.AudioModel;
import com.verizon.mms.model.ImageModel;
import com.verizon.mms.model.Model;
import com.verizon.mms.model.SlideModel;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.model.VCardModel;
import com.verizon.mms.model.VideoModel;
import com.verizon.mms.util.BitmapManager;

public class MmsThumbnailPresenter extends Presenter {
    protected final Handler mHandler = new Handler();
    public MmsThumbnailPresenter(Context context, ViewInterface view, Model model) {
        super(context, view, model);
    }

    @Override
    public void present() {
        SlideModel slide = ((SlideshowModel) mModel).get(0);
        if (slide != null) {
            if (slide.hasLocation()) {
                presentLocationThumbnail((SlideViewInterface)mView, slide);
            } else {
                presentFirstSlide((SlideViewInterface) mView, slide);
            }
        }
    }
    
    private void presentFirstSlide(SlideViewInterface view, SlideModel slide) {
        view.reset();

        if (slide.hasImage()) {
            presentImageThumbnail(view, slide.getImage());
        } else if (slide.hasVideo()) {
            presentVideoThumbnail(view, slide.getVideo());
        } else if (slide.hasAudio()) {
            presentAudioThumbnail(view, slide.getAudio());
        } else if (slide.hasVCard()) {
            presentVcardThumbnail(view, slide.getVCard());
        } else {
            if (((SlideshowModel) mModel).size() > 0) {
                presentBlankThumbnail(view);
            }
        }
    }

    private void presentBlankThumbnail(SlideViewInterface view) {
        view.setSlideShow();
    }

    private void presentLocationThumbnail(SlideViewInterface view, SlideModel slide) {
        view.reset();
        
        ImageModel im = slide.getImage();
        Bitmap bm = im != null ? im.getBitmap() : null;
        view.setLocation(bm, slide.getLocation().getFormattedMsg());
    }

    private void presentVideoThumbnail(SlideViewInterface view, VideoModel video) {
        if (video.isDrmProtected()) {
            showDrmIcon(view, video.getSrc());
        } else {
            view.setVideo(video.getSrc(), video.getBitmap());
        }
    }

    private void presentImageThumbnail(SlideViewInterface view, ImageModel image) {
        if (image.isDrmProtected()) {
            showDrmIcon(view, image.getSrc());
        } else {
            view.setImage(image.getSrc(), image.getBitmap(view.getImageDimensions()));
        }
    }
    
    protected void presentVcardThumbnail(SlideViewInterface view, VCardModel vcard) {
        view.setVCard(vcard.getUri(), vcard.getFormattedMsg(), vcard.getContactPicture());
    }

    protected void presentAudioThumbnail(SlideViewInterface view, AudioModel audio) {
        if (audio.isDrmProtected()) {
            showDrmIcon(view, audio.getSrc());
        } else {
            view.setAudio(audio.getUri(), audio.getSrc(), audio.getExtras());
        }
    }

    // Show an icon instead of real content in the thumbnail.
    private void showDrmIcon(SlideViewInterface view, String name) {
        Bitmap bitmap = BitmapManager.INSTANCE.decodeResource(
                mContext.getResources(), R.drawable.ic_mms_drm_protected);
        view.setImage(name, bitmap);
    }

    public void onModelChanged(final Model model,final boolean dataChanged) {
      final SlideViewInterface view = (SlideViewInterface) mView;
       if (model instanceof VCardModel) {
               mHandler.post(new Runnable() {
                   public void run() {
                           try {
                               presentVcard(view, (VCardModel) model, dataChanged);
                        } catch (DrmException e) {
                            // TODO Auto-generated catch block
                            Logger.error(e);
                        }
                   }
               });
           } 
    }
    
    protected void presentVcard(SlideViewInterface view, VCardModel vcard,
            boolean dataChanged) throws DrmException {
            view.setVCard(vcard.getUri(), vcard.getFormattedMsg(), vcard.getContactPicture()); 
    }}
