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

import static android.media.ExifInterface.ORIENTATION_ROTATE_270;
import static android.media.ExifInterface.ORIENTATION_ROTATE_90;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.Telephony.Mms.Part;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.model.ImageModel;
import com.verizon.mms.pdu.PduPart;
import com.verizon.mms.util.BitmapManager;
import com.verizon.mms.util.SqliteWrapper;

public class UriImage {
    private final Context mContext;
    private final Uri mUri;
    private String mContentType;
    private String mPath;
    private String mSrc;
    private int mWidth;
    private int mHeight;

    // the min image size we'll scale down to: based on the given min width
    // in portrait orientation with a 3:2 aspect ratio
    private static final float ASPECT = 3f / 2f;
    private static final int MIN_WIDTH = 320;
    private static final int MIN_SIZE = (int)(MIN_WIDTH * (MIN_WIDTH * ASPECT));

    // the max image side length that we'll try to scale to
    private static final int MAX_LENGTH = 1600;


    public UriImage(Context context, Uri uri) {
        if ((null == context) || (null == uri)) {
            throw new IllegalArgumentException();
        }

        String scheme = uri.getScheme();
        if (scheme.equals("content")) {
            initFromContentUri(context, uri);
        } else if (uri.getScheme().equals("file")) {
            initFromFile(context, uri);
        } else {
        	throw new UnsupportedOperationException("Unsupported scheme: " + uri);
        }

        mSrc = mPath.substring(mPath.lastIndexOf('/') + 1);

        // Some MMSCs appear to have problems with filenames
        // containing a space.  So just replace them with
        // underscores in the name, which is typically not
        // visible to the user anyway.
        mSrc = mSrc.replace(' ', '_');

        mContext = context;
        mUri = uri;

        decodeBoundsInfo();
    }

    private void initFromFile(Context context, Uri uri) {
        mPath = uri.getPath();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String extension = MimeTypeMap.getFileExtensionFromUrl(mPath);
        if (TextUtils.isEmpty(extension)) {
            // getMimeTypeFromExtension() doesn't handle spaces in filenames nor can it handle
            // urlEncoded strings. Let's try one last time at finding the extension.
            int dotPos = mPath.lastIndexOf('.');
            if (0 <= dotPos) {
                extension = mPath.substring(dotPos + 1);
            }
        }
        mContentType = mimeTypeMap.getMimeTypeFromExtension(extension);
        // It's ok if mContentType is null. Eventually we'll show a toast telling the
        // user the picture couldn't be attached.
    }
    
    /**
     * Get the actual file path of the image from Uri and it's ContentProvider.
     * 
     * @param context
     * @param uri
     * @return	file path of the actual file of the uri or null if erro.
     */
    public static String getContentPath(Context context, Uri uri) {
    	String filePath = null;
    	try {
    		Cursor c = SqliteWrapper.query(context, context.getContentResolver(),
    				uri, null, null, null, null);

    		if (c != null) {
    			if ((c.getCount() == 1) && c.moveToFirst()) {
    				filePath = c.getString(c.getColumnIndexOrThrow(Images.Media.DATA));
    			}
    		}
    	}
    	catch (Exception e) {
    		// ignore any exception    			
    	}		
    	return filePath;
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
            if (ImageModel.isMmsUri(uri)) {
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
            } 
            else if (ImageModel.isPicasaUri(uri)) {
                filePath = uri.toString();
                mContentType = c.getString(
                        c.getColumnIndexOrThrow(Images.Media.MIME_TYPE));
            }
            else {
            	filePath = c.getString(
                        c.getColumnIndexOrThrow(Images.Media.DATA));
                mContentType = c.getString(
                        c.getColumnIndexOrThrow(Images.Media.MIME_TYPE));
            }
            mPath = filePath;
        } finally {
            c.close();
        }
    }

    private void decodeBoundsInfo() {
        InputStream input = null;
        try {
            input = mContext.getContentResolver().openInputStream(mUri);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapManager.INSTANCE.decodeStream(input, null, opt);
            mWidth = opt.outWidth;
            mHeight = opt.outHeight;
        } catch (FileNotFoundException e) {
            // Ignore
        	Logger.error(getClass(), "IOException caught while opening stream", e);
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                	Logger.error(getClass(), "IOException caught while closing stream", e);
                }
            }
        }
    }
    
    public String getContentType() {
        return mContentType;
    }

    public String getSrc() {
        return mSrc;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
    
    public String getPath() {
    	return mPath;
    }

    public PduPart getResizedImageAsPart(int widthLimit, int heightLimit, int byteLimit) {
    	return getResizedImageAsPart(widthLimit, heightLimit, byteLimit, ExifInterface.ORIENTATION_NORMAL);
    }
    
    public PduPart getResizedImageAsPart(int widthLimit, int heightLimit, int byteLimit, int exifOrientation) {
        PduPart part = new PduPart();

        byte[] data = getResizedImageData(widthLimit, heightLimit, byteLimit, exifOrientation);
        if (data == null) {
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "Resize image failed.");
            }
            return null;
        }

        part.setData(data);
        part.setContentType(getContentType().getBytes());
        String src = getSrc();
        byte[] srcBytes = src.getBytes();
        part.setContentLocation(srcBytes);
        part.setFilename(srcBytes);
        int period = src.lastIndexOf(".");
        byte[] contentId = period != -1 ? src.substring(0, period).getBytes() : srcBytes;
        part.setContentId(contentId);

        return part;
    }

	private byte[] getResizedImageData(int widthLimit, int heightLimit, int byteLimit, int exifOrientation) {
		final int srcWidth = mWidth;
		final int srcHeight = mHeight;
		byte[] data = null;
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		final String url = mUri.toString();
		final BitmapManager bitmapMgr = BitmapManager.INSTANCE;
		Bitmap src = null;
		boolean success = false;

		// handle if the image is rotated
		int dstWidth;
		int dstHeight;
		if (exifOrientation == ORIENTATION_ROTATE_90 || exifOrientation == ORIENTATION_ROTATE_270) {
			dstWidth = mHeight;
			dstHeight = mWidth;
		}
		else {
			dstWidth = mWidth;
			dstHeight = mHeight;
		}

		// start with the max target image size
		final float scale = (float)MAX_LENGTH / (dstWidth > dstHeight ? dstWidth : dstHeight);
		if (scale < 1f) {
			dstWidth = Math.round((float)dstWidth * scale);
			dstHeight = Math.round((float)dstHeight * scale);
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "getResizedImageData: " + url + ": byteLimit = " + byteLimit +
				", limit = " + widthLimit + "x" + heightLimit + ", img = " + mWidth + "x" + mHeight +
				", scale = " + scale + ", target = " + dstWidth + "x" + dstHeight);
		}

		do {
			// try it with the current target size
			src = bitmapMgr.getBitmap(url, srcWidth, srcHeight, dstWidth, dstHeight, dstWidth,
				true, false, true, exifOrientation, true);

			if (src != null) {
				// Compress the image into a JPG. Start with MessageUtils.IMAGE_COMPRESSION_QUALITY.
				// In case that the image byte size is still too large reduce the quality in
				// proportion to the desired byte size. Should the quality fall below
				// MINIMUM_IMAGE_COMPRESSION_QUALITY skip a compression attempt and we will enter
				// the next round with a smaller image to start with.
				try {
					int quality = MessageUtils.IMAGE_COMPRESSION_QUALITY;
					success = src.compress(CompressFormat.JPEG, quality, os);
					if (success) {
						// check that compressed size is within the limit
						final int jpgFileSize = os.size();
						if (byteLimit > 0 && jpgFileSize > byteLimit) {
							success = false;
							quality = Math.round((float)quality * byteLimit / jpgFileSize);
							if (quality >= MessageUtils.MINIMUM_IMAGE_COMPRESSION_QUALITY) {
								os.reset();
								success = src.compress(CompressFormat.JPEG, quality, os);
								if (Logger.IS_DEBUG_ENABLED) {
									Logger.debug(getClass(),
										"getResizedImageData: retried compress with quality = " + quality);
								}
							}
						}
					}
				}
				catch (Throwable t) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.error(getClass(), "getResizedImageData: error compressing " + url, t);
					}
				}

				src.recycle();
			}

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "getResizedImageData: success = " + success +
					", size = " + dstWidth + "x" + dstHeight + ", bytes = " + os.size());
			}

			if (!success) {
				os.reset();

				// scale down the target size
				dstWidth = Math.round(dstWidth * BitmapManager.SCALE_DOWN_FACTOR);
				dstHeight = Math.round(dstHeight * BitmapManager.SCALE_DOWN_FACTOR);
			}
		} while (!success && dstWidth * dstHeight > MIN_SIZE);

		if (success) {
			data = os.toByteArray();
			try {
				os.close();
			}
			catch (Exception e) {
			}
		}

		return data;
	}
    
    /**
     * Return a matrix that will rotate the source Bitmap according to the EXIF Orientation
     * or null if no rotation is required.
     * 
     * @param exifOrientation	EXIF Orientation of the source Bitmap 
     * @return					A matrix that will correctly rotate the Bitmap or null if no rotation is needed 
     */
    public static Matrix getMatrix(int exifOrientation) {
        Matrix matrix = new Matrix(); 
        switch (exifOrientation) {
        case ExifInterface.ORIENTATION_TRANSPOSE: {			// 5
        	matrix.postRotate(90);
        	// continue to flip horizontally
        }
        case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:	{	// 2
        	float[] mirrorY = {
        		-1, 0, 0,
        		0, 1, 0,
        		0, 0, 1
        	};
           	Matrix matrixMirrorY = new Matrix();
           	matrixMirrorY.setValues(mirrorY);
           	
           	matrix.postConcat(matrixMirrorY);
        	break;
        }
        case ExifInterface.ORIENTATION_ROTATE_180: {		// 3
        	matrix.postRotate(180);
        	break;
        }
        case ExifInterface.ORIENTATION_TRANSVERSE: {		// 7
        	matrix.postRotate(90);
        	// continue to flip vertically
        }
        case ExifInterface.ORIENTATION_FLIP_VERTICAL: {		// 4
        	float[] mirrorX = {
            	1, 0, 0,
            	0, -1, 0,
            	0, 0, 1
            };
           	Matrix matrixMirrorX = new Matrix();
           	
           	matrixMirrorX.setValues(mirrorX);
           	matrix.postConcat(matrixMirrorX);
           	break;
        }
        case ExifInterface.ORIENTATION_ROTATE_90: {			// 6
        	matrix.postRotate(90);
        	break;
        }
        case ExifInterface.ORIENTATION_ROTATE_270:	{		// 8
        	matrix.postRotate(270);
        	break;
        }
        default:
        	matrix = null;
        	break;                        	
        }

        return matrix;
    }
}
