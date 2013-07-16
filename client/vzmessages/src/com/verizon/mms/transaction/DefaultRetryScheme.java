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

package com.verizon.mms.transaction;

import android.content.Context;

import com.strumsoft.android.commons.logger.Logger;

/**
 * Default retry scheme, based on specs.
 */
public class DefaultRetryScheme extends AbstractRetryScheme {
    private static final String TAG = "DefaultRetryScheme";

    // Verizon retry scheme needs to be 30 sec, 3 min, 5 min, and 8 min - added more retries
    private static final int[] sDefaultRetryScheme = {
        0, 1 * 20 * 1000, 1 * 20 * 1000, 1 * 30 * 1000, 1 * 60 * 1000, 1 * 60 * 1000, 3 * 60 * 1000, 5 * 60 * 1000, 8 * 60 * 1000};

    public DefaultRetryScheme(Context context, int retriedTimes) {
        super(retriedTimes);

        mRetriedTimes = mRetriedTimes < 0 ? 0 : mRetriedTimes;
        mRetriedTimes = mRetriedTimes >= sDefaultRetryScheme.length
                ? sDefaultRetryScheme.length - 1 : mRetriedTimes;

        // TODO Get retry scheme from preference.
    }

    @Override
    public int getRetryLimit() {
        return sDefaultRetryScheme.length;
    }

    @Override
    public long getWaitingInterval() {
        if(Logger.IS_DEBUG_ENABLED){
        	Logger.debug(getClass(), "Next int: " + sDefaultRetryScheme[mRetriedTimes]);
        }
        return sDefaultRetryScheme[mRetriedTimes];
    }
}
