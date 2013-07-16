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

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms.Part;
import com.verizon.mms.util.SqliteWrapper;

/**
 * Class used to get the path of the file in _data field the assumption is that the 
 * Uri passed to it is always an MMS Uri
 * @author admin
 *
 */
public class UriData {
	private final Context mContext;
	private final Uri mUri;
	private String mContentType;
	private String mPath;
	private String mSrc;

	public UriData(Context context, Uri uri) {
		if ((null == context) || (null == uri)) {
			throw new IllegalArgumentException();
		}

		initFromContentUri(context, uri);
		mSrc = mPath.substring(mPath.lastIndexOf('/') + 1);

		// Some MMSCs appear to have problems with filenames
		// containing a space.  So just replace them with
		// underscores in the name, which is typically not
		// visible to the user anyway.
		mSrc = mSrc.replace(' ', '_');

		mContext = context;
		mUri = uri;
	}

	private void initFromContentUri(Context context, Uri uri) {
		Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
				uri, null, null, null, null);

		if (c == null) {
			throw new IllegalArgumentException(
					"Query on " + uri + " returns null result.");
		}

		try {
			if ((c.getCount() != 1) || !c.moveToFirst()) {
				throw new IllegalArgumentException(
						"Query on " + uri + " returns 0 or multiple rows.");
			}

			String filePath = null;

			final String path = c.getString(c.getColumnIndexOrThrow(Part._DATA));
			// try the filename field if the data field is invalid or the file doesn't exist
			if (path != null && path.length() != 0) {
				try {
					final File file = new File(path);
					if (file.exists()) {
						filePath = path;
					}
				}
				catch (Exception e) {
				}
			}
			if (filePath == null) {
				filePath = c.getString(c.getColumnIndexOrThrow(Part.FILENAME));
			}
			mContentType = c.getString(
					c.getColumnIndexOrThrow(Part.CONTENT_TYPE));
			mPath = filePath;
		} finally {
			c.close();
		}
	}

	public String getContentType() {
		return mContentType;
	}

	public String getSrc() {
		return mSrc;
	}

	public String getPath() {
		return mPath;
	}
}
