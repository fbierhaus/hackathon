/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.verizon.mms;

import java.util.HashSet;

public class ContentType {
    public static final String MMS_MESSAGE       = "application/vnd.wap.mms-message";
    // The phony content type for generic PDUs (e.g. ReadOrig.ind,
    // Notification.ind, Delivery.ind).
    public static final String MMS_GENERIC       = "application/vnd.wap.mms-generic";
    public static final String MULTIPART_MIXED   = "application/vnd.wap.multipart.mixed";
    public static final String MULTIPART_RELATED = "application/vnd.wap.multipart.related";
    public static final String MULTIPART_ALTERNATIVE = "application/vnd.wap.multipart.alternative";

    public static final String TEXT_PLAIN        = "text/plain";
    public static final String TEXT_HTML         = "text/html";
    public static final String TEXT_VCALENDAR    = "text/x-vCalendar";
    public static final String TEXT_VCARD        = "text/x-vCard";

    public static final String IMAGE_UNSPECIFIED = "image/*";
    public static final String IMAGE_JPEG        = "image/jpeg";
    public static final String IMAGE_JPG         = "image/jpg";
    public static final String IMAGE_GIF         = "image/gif";
    public static final String IMAGE_WBMP        = "image/vnd.wap.wbmp";
    public static final String IMAGE_BMP         = "image/bmp";
    public static final String IMAGE_SVG         = "image/sav+xml";
    public static final String IMAGE_PNG         = "image/png";
    public static final String IMAGE_MBMP        = "image/x-ms-bmp";
    public static final String IMAGE_SVG_XML     = "image/svg+xml";

    public static final String AUDIO_UNSPECIFIED = "audio/*";
    public static final String AUDIO_AAC         = "audio/aac";
    public static final String AUDIO_AMR         = "audio/amr";
    public static final String AUDIO_VNDQCP      = "audio/vnd.qcelp";
    public static final String AUDIO_QCELP       = "audio/qcelp";
    public static final String AUDIO_QCP         = "audio/qcp";
    public static final String AUDIO_EVRC        = "audio/evrc";
    public static final String AUDIO_EVRC_QCP    = "audio/evrc-qcp";
    public static final String AUDIO_IMELODY     = "audio/imelody";
    public static final String AUDIO_MID         = "audio/mid";
    public static final String AUDIO_MIDI        = "audio/midi";
    public static final String AUDIO_MP3         = "audio/mp3";
    public static final String AUDIO_MPEG3       = "audio/mpeg3";
    public static final String AUDIO_MPEG        = "audio/mpeg";
    public static final String AUDIO_MPG         = "audio/mpg";
    public static final String AUDIO_MP4         = "audio/mp4";
    public static final String AUDIO_M4A         = "audio/m4a";
    public static final String AUDIO_X_MID       = "audio/x-mid";
    public static final String AUDIO_X_MIDI      = "audio/x-midi";
    public static final String AUDIO_WAV         = "audio/wav";
    public static final String AUDIO_X_WAV       = "audio/x-wav";
    public static final String AUDIO_MP4A        = "audio/mp4a-latm";
    public static final String AUDIO_X_MP3       = "audio/x-mp3";
    public static final String AUDIO_X_MPEG3     = "audio/x-mpeg3";
    public static final String AUDIO_X_MPEG      = "audio/x-mpeg";
    public static final String AUDIO_X_MPG       = "audio/x-mpg";
    public static final String AUDIO_3GPP        = "audio/3gpp";
    public static final String AUDIO_3GPP2       = "audio/3gpp2";
    public static final String AUDIO_OGG         = "application/ogg";
    public static final String AUDIO_SP_MIDI     = "audio/sp-midi";

    public static final String VIDEO_UNSPECIFIED = "video/*";
    public static final String VIDEO_3GPP        = "video/3gpp";
    public static final String VIDEO_H2632000    = "video/h263-2000";
    public static final String VIDEO_MP4ES       = "video/mp4v-es";
    public static final String VIDEO_3G2         = "video/3gpp2";
    public static final String VIDEO_H263        = "video/h263";
    public static final String VIDEO_H264        = "video/h264";
    public static final String VIDEO_MP4         = "video/mp4";
    public static final String VIDEO_MPEG        = "video/mpeg";
    public static final String VIDEO_QUICKTIME   = "video/quicktime";

    public static final String APP_SMIL          = "application/smil";
    public static final String APP_WAP_XHTML     = "application/vnd.wap.xhtml+xml";
    public static final String APP_XHTML         = "application/xhtml+xml";

    public static final String APP_DRM_CONTENT   = "application/vnd.oma.drm.content";
    public static final String APP_DRM_MESSAGE   = "application/vnd.oma.drm.message";

    private static final HashSet<String> sSupportedTextTypes = new HashSet<String>();
    private static final HashSet<String> sSupportedImageTypes = new HashSet<String>();
    private static final HashSet<String> sSupportedAudioTypes = new HashSet<String>();
    private static final HashSet<String> sSupportedVideoTypes = new HashSet<String>();

    static {
        // add supported text types
        sSupportedTextTypes.add(TEXT_PLAIN);
        sSupportedTextTypes.add(TEXT_HTML);
        sSupportedTextTypes.add(TEXT_VCALENDAR);
        sSupportedTextTypes.add(TEXT_VCARD);

        // add supported image types
        sSupportedImageTypes.add(IMAGE_JPEG);
        sSupportedImageTypes.add(IMAGE_GIF);
        sSupportedImageTypes.add(IMAGE_SVG);
        sSupportedImageTypes.add(IMAGE_BMP);
        sSupportedImageTypes.add(IMAGE_WBMP);
        sSupportedImageTypes.add(IMAGE_MBMP);
        sSupportedImageTypes.add(IMAGE_PNG);
        sSupportedImageTypes.add(IMAGE_JPG);
        sSupportedImageTypes.add(IMAGE_SVG_XML);

        // add supported audio types
        sSupportedAudioTypes.add(AUDIO_AAC);
        sSupportedAudioTypes.add(AUDIO_QCP);
        sSupportedAudioTypes.add(AUDIO_QCELP);
        sSupportedAudioTypes.add(AUDIO_VNDQCP);
        sSupportedAudioTypes.add(AUDIO_WAV);
        sSupportedAudioTypes.add(AUDIO_X_WAV);
        sSupportedAudioTypes.add(AUDIO_MP4A);
        sSupportedAudioTypes.add(AUDIO_EVRC);
        sSupportedAudioTypes.add(AUDIO_EVRC_QCP);        
        sSupportedAudioTypes.add(AUDIO_AMR);
        sSupportedAudioTypes.add(AUDIO_IMELODY);
        sSupportedAudioTypes.add(AUDIO_MID);
        sSupportedAudioTypes.add(AUDIO_MIDI);
        sSupportedAudioTypes.add(AUDIO_MP3);
        sSupportedAudioTypes.add(AUDIO_MPEG3);
        sSupportedAudioTypes.add(AUDIO_MPEG);
        sSupportedAudioTypes.add(AUDIO_MPG);
        sSupportedAudioTypes.add(AUDIO_MP4);
        sSupportedAudioTypes.add(AUDIO_X_MID);
        sSupportedAudioTypes.add(AUDIO_X_MIDI);
        sSupportedAudioTypes.add(AUDIO_X_MP3);
        sSupportedAudioTypes.add(AUDIO_X_MPEG3);
        sSupportedAudioTypes.add(AUDIO_X_MPEG);
        sSupportedAudioTypes.add(AUDIO_X_MPG);
        sSupportedAudioTypes.add(AUDIO_3GPP);
        sSupportedAudioTypes.add(AUDIO_3GPP2);
        sSupportedAudioTypes.add(AUDIO_OGG);
        sSupportedAudioTypes.add(AUDIO_M4A);
        sSupportedAudioTypes.add(AUDIO_MP4A);
        sSupportedAudioTypes.add(AUDIO_SP_MIDI);
        
        // add supported video types
        sSupportedVideoTypes.add(VIDEO_3GPP);
        sSupportedVideoTypes.add(VIDEO_H2632000);
        sSupportedVideoTypes.add(VIDEO_H264);
        sSupportedVideoTypes.add(VIDEO_MP4ES);
        sSupportedVideoTypes.add(VIDEO_3G2);
        sSupportedVideoTypes.add(VIDEO_H263);
        sSupportedVideoTypes.add(VIDEO_MP4);
        sSupportedVideoTypes.add(VIDEO_MPEG);
        sSupportedVideoTypes.add(VIDEO_QUICKTIME);
    }

    // This class should never be instantiated.
    private ContentType() {
    }

    public static boolean isSupportedImageType(String contentType) {
        return sSupportedImageTypes.contains(contentType);
    }

    public static boolean isSupportedAudioType(String contentType) {
        return sSupportedAudioTypes.contains(contentType);
    }

    public static boolean isSupportedVideoType(String contentType) {
        return sSupportedVideoTypes.contains(contentType);
    }

    public static boolean isTextType(String contentType) {
        return sSupportedTextTypes.contains(contentType);
    }

    public static boolean isImageType(String contentType) {
        return isSupportedImageType(contentType);
    }

    public static boolean isAudioType(String contentType) {
        return isSupportedAudioType(contentType);
    }

    public static boolean isVideoType(String contentType) {
        return isSupportedVideoType(contentType);
    }

    public static boolean isDrmType(String contentType) {
        return (null != contentType)
                && (contentType.equals(APP_DRM_CONTENT)
                        || contentType.equals(APP_DRM_MESSAGE));
    }

    public static boolean isUnspecified(String contentType) {
        return (null != contentType) && contentType.endsWith("*");
    }
    
    public static boolean isPlainTextType(String contentType) {
        return (null != contentType) && contentType.equalsIgnoreCase(ContentType.TEXT_PLAIN);
    }
    
    public static boolean isHtmlTextType(String contentType) {
        return (null != contentType) && 
                (contentType.equalsIgnoreCase(ContentType.TEXT_HTML) ||
                        contentType.equalsIgnoreCase(ContentType.APP_WAP_XHTML));
    }
    
    public static boolean isVcardTextType(String contentType) {
        return (null != contentType) && contentType.equalsIgnoreCase(ContentType.TEXT_VCARD);
    }
}
