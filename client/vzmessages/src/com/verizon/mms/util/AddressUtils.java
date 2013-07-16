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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms.Addr;
import android.text.TextUtils;

import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPersister;

public class AddressUtils {
	private static final String[] COLS = { Addr.ADDRESS, Addr.CHARSET };
	private static final String WHERE = Addr.TYPE + " = " + PduHeaders.FROM + " AND " + Addr.ADDRESS + " != ?";
	private static final String[] ARGS = { PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR };

    private AddressUtils() {
        // Forbidden being instantiated.
    }

    public static String getFrom(Context context, Uri uri) {
        String msgId = uri.getLastPathSegment();
        Uri.Builder builder = VZUris.getMmsUri().buildUpon();

        builder.appendPath(msgId).appendPath("addr");

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(), builder.build(), COLS, WHERE, ARGS, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String from = cursor.getString(0);

                    if (!TextUtils.isEmpty(from)) {
                        byte[] bytes = PduPersister.getBytes(from);
                        int charset = cursor.getInt(1);
                        return new EncodedStringValue(charset, bytes).getString();
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return context.getString(R.string.hidden_sender_address);
    }
}
