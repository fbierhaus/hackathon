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

package com.verizon.mms.layout;

import com.strumsoft.android.commons.logger.Logger;

public class HVGALayoutParameters implements LayoutParameters {
    private static final String TAG = "HVGALayoutParameters";

    private int mType = -1;

    private static final int IMAGE_HEIGHT_LANDSCAPE = 240;
    private static final int TEXT_HEIGHT_LANDSCAPE = 80;
    private static final int IMAGE_HEIGHT_PORTRAIT = 320;
    private static final int TEXT_HEIGHT_PORTRAIT = 160;

    public HVGALayoutParameters(int type) {
        if ((type != HVGA_LANDSCAPE) && (type != HVGA_PORTRAIT)) {
            throw new IllegalArgumentException("Bad layout type detected: " + type);
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "HVGALayoutParameters.<init>(" + type + ").");
        }
        mType = type;
    }

    public int getWidth() {
        return mType == HVGA_LANDSCAPE ? HVGA_LANDSCAPE_WIDTH : HVGA_PORTRAIT_WIDTH;
    }

    public int getHeight() {
        return mType == HVGA_LANDSCAPE ? HVGA_LANDSCAPE_HEIGHT : HVGA_PORTRAIT_HEIGHT;
    }

    public int getImageHeight() {
        return mType == HVGA_LANDSCAPE ? IMAGE_HEIGHT_LANDSCAPE : IMAGE_HEIGHT_PORTRAIT;
    }

    public int getTextHeight() {
        return mType == HVGA_LANDSCAPE ? TEXT_HEIGHT_LANDSCAPE : TEXT_HEIGHT_PORTRAIT;
    }

    public int getType() {
        return mType;
    }

    public String getTypeDescription() {
        return mType == HVGA_LANDSCAPE ? "HVGA-L" : "HVGA-P";
    }
}
