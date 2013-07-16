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

import android.content.Context;
import android.content.res.Configuration;

import com.strumsoft.android.commons.logger.Logger;

/**
 * MMS presentation layout management.
 */
public class LayoutManager {
    private static final String TAG = "LayoutManager";

    private final Context mContext;
    private LayoutParameters mLayoutParams;

    private static LayoutManager sInstance;

    private LayoutManager(Context context) {
        mContext = context;
        initLayoutParameters(context.getResources().getConfiguration());
    }

    private void initLayoutParameters(Configuration configuration) {
        mLayoutParams = getLayoutParameters(
                configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                ? LayoutParameters.HVGA_PORTRAIT
                : LayoutParameters.HVGA_LANDSCAPE);

        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "LayoutParameters: " + mLayoutParams.getTypeDescription()
                    + ": " + mLayoutParams.getWidth() + "x" + mLayoutParams.getHeight());
        }
    }

    private static LayoutParameters getLayoutParameters(int displayType) {
        switch (displayType) {
            case LayoutParameters.HVGA_LANDSCAPE:
                return new HVGALayoutParameters(LayoutParameters.HVGA_LANDSCAPE);
            case LayoutParameters.HVGA_PORTRAIT:
                return new HVGALayoutParameters(LayoutParameters.HVGA_PORTRAIT);
        }

        throw new IllegalArgumentException(
                "Unsupported display type: " + displayType);
    }

    public static void init(Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(LayoutManager.class, "DefaultLayoutManager.init()");
        }

        if (sInstance != null) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(LayoutManager.class, "Already initialized.");
            }
        }
        sInstance = new LayoutManager(context);
    }

    public static LayoutManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("Uninitialized.");
        }
        return sInstance;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "-> LayoutManager.onConfigurationChanged().");
        }
        initLayoutParameters(newConfig);
    }

    public int getLayoutType() {
        return mLayoutParams.getType();
    }

    public int getLayoutWidth() {
        return mLayoutParams.getWidth();
    }

    public int getLayoutHeight() {
        return mLayoutParams.getHeight();
    }

    public LayoutParameters getLayoutParameters() {
        return mLayoutParams;
    }
}
