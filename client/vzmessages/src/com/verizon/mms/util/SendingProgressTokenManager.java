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

package com.verizon.mms.util;

import java.util.HashMap;

import com.strumsoft.android.commons.logger.Logger;

public class SendingProgressTokenManager {
    private static final String TAG = "SendingProgressTokenManager";
    private static final HashMap<Object, Long> TOKEN_POOL;

    public static final long NO_TOKEN = -1L;

    static {
        TOKEN_POOL = new HashMap<Object, Long>();
    }

    synchronized public static long get(Object key) {
        Long token = TOKEN_POOL.get(key);
        		if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(SendingProgressTokenManager.class, "TokenManager.get(" + key + ") -> " + token);
        }
        return token != null ? token : NO_TOKEN;
    }

    synchronized public static void put(Object key, long token) {
        		if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(SendingProgressTokenManager.class, "TokenManager.put(" + key + ", " + token + ")");
        }
        TOKEN_POOL.put(key, token);
    }

    synchronized public static void remove(Object key) {
        		if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(SendingProgressTokenManager.class, "TokenManager.remove(" + key + ")");
        }
        TOKEN_POOL.remove(key);
    }
}