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

package com.verizon.mms.model;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.events.Event;
import org.w3c.dom.smil.ElementTime;

import android.content.Context;
import android.drm.mobile1.DrmException;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.text.TextUtils;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ContentRestrictionException;
import com.verizon.mms.ExceedMessageSizeException;
import com.verizon.mms.MmsException;
import com.verizon.mms.dom.smil.SmilMediaElementImpl;
import com.verizon.mms.drm.DrmWrapper;
import com.verizon.mms.pdu.PduPart;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.ui.UriImage;
import com.verizon.mms.util.BitmapManager;


public class ImageModel extends RegionMediaModel {
	private static final String PICASA = "content://com.google.android.gallery3d.provider";
	private static final String PICASA2 = "content://com.sec.google.android.gallery3d.provider";

    private static final Rect THUMBNAIL_BOUNDS_LIMIT = new Rect(0, 0, 480, 320);

    private int mWidth;
    private int mHeight;
    private Bitmap mBitmap;
    private boolean mBitmapLoaded = false;
	
    private boolean mImageBoundsDecoded = false; // flag used to load the imagebounds at the later stage only when displaying the image
    
    // flag used to notify whether to load the exif information or not 
    // this flag will be set to false when displaying the bitmaps in messagebubble as there is no need of 
    // getting the orientation at this stage
    private boolean mLoadExif = true;
    
    private Rect mLastDims;
    private int mExifOrientation = -1;		// not set
    private BitmapManager bitmapMgr = BitmapManager.INSTANCE;


    public ImageModel(Context context, Uri uri, RegionModel region)
            throws MmsException {
        super(context, SmilHelper.ELEMENT_TAG_IMAGE, uri, region);
        initModelFromUri(uri);
        checkContentRestriction();
    }

    public ImageModel(Context context, String contentType, String src,
            Uri uri, RegionModel region) throws DrmException, MmsException {
        this(context, contentType, src, uri, region, true);
    }
    
    public ImageModel(Context context, String contentType, String src,
            Uri uri, RegionModel region, boolean decodeBounds) throws DrmException, MmsException {
        super(context, SmilHelper.ELEMENT_TAG_IMAGE,
                contentType, src, uri, region);
        if (decodeBounds) {
        	decodeImageBounds();
        } else {
        	mLoadExif = false;
        }
    }

    public ImageModel(Context context, String contentType, String src,
            DrmWrapper wrapper, RegionModel regionModel) throws IOException {
        super(context, SmilHelper.ELEMENT_TAG_IMAGE, contentType, src,
                wrapper, regionModel);
        // image orientation will be retrieved later
    }

    public ImageModel(Context context, String contentType, String src,
            Uri uri, byte[] data, RegionModel region) throws DrmException, MmsException {
        super(context, SmilHelper.ELEMENT_TAG_IMAGE,
                contentType, src, uri, region);
        decodeImageBounds();
    }

    private void initModelFromUri(Uri uri) throws MmsException {
        UriImage uriImage = new UriImage(mContext, uri);

        mContentType = uriImage.getContentType();
        if (TextUtils.isEmpty(mContentType)) {
            throw new MmsException("Type of media is unknown.");
        }
        mSrc = uriImage.getSrc();
        mWidth = uriImage.getWidth();
        mHeight = uriImage.getHeight();
        if (mLoadExif) {
        	mExifOrientation = getImageOrientation(mContext, uriImage.getPath(), uri, ExifInterface.ORIENTATION_NORMAL);
        }
        mImageBoundsDecoded = true;
        
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "initModelFromUri: " + this + ", mSrc = " + mSrc +
            	", mContentType = " + mContentType);
        }
    }

    private void decodeImageBounds() throws DrmException {
    	mImageBoundsDecoded = true;
    	Uri uri = getUriWithDrmCheck();
        UriImage uriImage = new UriImage(mContext, uri);
        mWidth = uriImage.getWidth();
        mHeight = uriImage.getHeight();
        
        if (mLoadExif && mExifOrientation == -1) {
        	mExifOrientation = getImageOrientation(mContext, uriImage.getPath(), uri, ExifInterface.ORIENTATION_NORMAL);
        } else {
        	mExifOrientation = ExifInterface.ORIENTATION_NORMAL;
        }
        
        if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), "Image bounds: " + mWidth + "x" + mHeight + " loadExifOrientation " + mExifOrientation);
        }
    }

    // EventListener Interface
    public void handleEvent(Event evt) {
        if (evt.getType().equals(SmilMediaElementImpl.SMIL_MEDIA_START_EVENT)) {
            mVisible = true;
        } else if (mFill != ElementTime.FILL_FREEZE) {
            mVisible = false;
        }

        notifyModelChanged(false);
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    protected void checkContentRestriction() throws ContentRestrictionException {
        ContentRestriction cr = ContentRestrictionFactory.getContentRestriction();
        cr.checkImageContentType(mContentType);
    }

    public Bitmap getBitmap() {
        return internalGetBitmap(getUri(), null);
    }

    public Bitmap getBitmapWithDrmCheck() throws DrmException {
        return internalGetBitmap(getUriWithDrmCheck(), null);
    }

	public Bitmap getBitmap(Rect dims) {
        return internalGetBitmap(getUri(), dims);
	}

    private Bitmap internalGetBitmap(Uri uri, Rect dims) {
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "internalGetBitmap: uri = " + uri + ", dims = " + dims + ", lastDims = " + mLastDims);
    	}
    	if (!mImageBoundsDecoded) {
    		try {
    			decodeImageBounds();
    		} catch (DrmException e) {
    			
    		}
    	}
    	if (mBitmapLoaded && mBitmap == null) {
    		Logger.debug(getClass(), "There was a failure in loading bitmap in prev attempt due to an OOM so avoid fetching it again");
    		return null;
    	}
    	if (dims == null) {
    		dims = THUMBNAIL_BOUNDS_LIMIT;
    	}
    	// try the cache if the dimensions haven't changed
        Bitmap bm = null;
        if (mLastDims != null && (dims.equals(mLastDims) || mLastDims.contains(dims))) {
        	bm = mBitmap;
        }
        else {
        	// clear cache
        	mBitmap = null;
        	mMemorySize = 0;
        }
        
        if (bm == null) {
            try {
            	mBitmapLoaded = true;
                bm = createThumbnailBitmap(dims, uri);
                if (bm != null) {
                    mBitmap = bm;
                    mLastDims = dims;
                }
            } catch (OutOfMemoryError ex) {
                // fall through and return a null bitmap. The callers can handle a null
                // result and show R.drawable.ic_missing_thumbnail_picture
            }

            mMemorySize = bm == null ? 0 : BitmapManager.getBitmapSize(bm);
        }
        return bm;
    }

	public void freeBitmapMemory() {
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "freeBitmapMemory: bitmap = " + mBitmap + ", this = " + this);
    	}
		mBitmap = null;
		mBitmapLoaded = false;
		mMemorySize = 0;
	}

	private Bitmap createThumbnailBitmap(Rect thumbnailBoundsLimit, Uri uri) {
		if (mExifOrientation == -1) {
			// we don't have the orientation yet probably because it's DRM
			// try to get it now
			mExifOrientation = getImageOrientationFromUri(mContext, uri, ExifInterface.ORIENTATION_NORMAL);
		}

		final int limitWidth = thumbnailBoundsLimit.width();
		final int limitHeight = thumbnailBoundsLimit.height();

		try {
			return bitmapMgr.getBitmap(uri.toString(), mWidth, mHeight,
				limitWidth, limitHeight, 0, true, false, true, mExifOrientation, true, true, true);
		}
		catch (OutOfMemoryError ex) {
			//MessageUtils.writeHprofDataToFile();
			throw ex;
		}
		catch (Exception e) {
			Logger.error(getClass(), e);
			return null;
		}
	}

    @Override
    public boolean getMediaResizable() {
        return true;
    }

    @Override
    protected void resizeMedia(int byteLimit, long messageId) throws MmsException {
        UriImage image = new UriImage(mContext, getUri());
        // 2012-05-05 correct the image orientation
        if (mExifOrientation == -1) {
        	// we don't have the orientation yet probably because it's DRM
        	// try to get it now
        	mExifOrientation = getImageOrientationFromUri(mContext, getUri(), ExifInterface.ORIENTATION_NORMAL);
        }
        
        int widthLimit;
        int heightLimit;
        // use original image dimension to try the best quality if it's less than byte limit
//        widthLimit = MmsConfig.getMaxImageWidth();
//        heightLimit = MmsConfig.getMaxImageHeight();
        widthLimit = mWidth;
        heightLimit = mHeight;
        PduPart part = image.getResizedImageAsPart(
                widthLimit,
                heightLimit,
                byteLimit, mExifOrientation);

        // we don't need the mExifOrientation anymore
        // it's very important so the old rotation information will not be saved
        // with the new rotated image
        mExifOrientation = ExifInterface.ORIENTATION_UNDEFINED;
        
        if (part == null) {
        	throw new ExceedMessageSizeException("Failed to resize image");
        }
        
        PduPersister persister = PduPersister.getPduPersister(mContext);
        this.mSize = part.getData().length;
        Uri newUri = persister.persistPart(part, messageId);
        setUri(newUri);
    }

    public static boolean isPicasaUri(Uri uri) {
    	if (uri.toString().startsWith("content://com.android.gallery3d.provider"))  {
 		   // use the com.google provider, not the com.android provider.
    		uri = Uri.parse(uri.toString().replace("com.android.gallery3d","com.google.android.gallery3d"));
    	}
    	if (uri.toString().startsWith("content://com.sec.android.gallery3d.provider"))  {
    		uri = Uri.parse(uri.toString().replace("com.sec.android.gallery3d","com.google.android.gallery3d"));
    	}
    	boolean isPicasa = uri.toString().startsWith(PICASA) || uri.toString().startsWith(PICASA2);
    	return isPicasa;
	}
   
    public int getExifOrientation() {
    	return mExifOrientation;
    }

    /**
     * Get orientation of the image.
     * 
     * It first tries using filepath with ExifInterface and then inputstream.
     * 
     */
    private static int getImageOrientation(Context context, String filepath, Uri uri, int defaultOrientation) {
    	int orientation = ExifInterface.ORIENTATION_UNDEFINED;
    	
    	// we try to use the filepath if it's not from mms database
    	// because filepath from mms database will return permission error anyway
    	boolean tryFile = false;    	
    	if (uri != null) {
    		if (isMmsUri(uri) == false) {
    			tryFile = true;
    		}
    	}
    	
    	if (tryFile && filepath != null) {
    		// first try using filepath, i.e. ExifInterface, which is faster
    		orientation = getImageOrientationFromFile(context, filepath, ExifInterface.ORIENTATION_UNDEFINED);
    	}
    	
    	if (orientation == ExifInterface.ORIENTATION_UNDEFINED && uri != null) {
    		// if we don't have an orientation yet, try again using uri
    		orientation = getImageOrientationFromUri(context, uri, ExifInterface.ORIENTATION_UNDEFINED);
    	}
    	
    	if (orientation <= ExifInterface.ORIENTATION_UNDEFINED || orientation > ExifInterface.ORIENTATION_ROTATE_270) {
    		orientation = defaultOrientation;
    	}
    	return orientation;
    }
    
    /**
     * Get orientation of the image.
     * 
     * MUST use the absolute path of the image, not path of through the MediaStorage content provider.
     * It seems like in some Android devices, there is some cached data in the MediaStorage that doesn't
     * have the same EXIF information as the actual image. It always returns orientation as 0.  
     *  
     * @param uriImage	UriImage to get orientation
     * @return 
     */
    private static int getImageOrientationFromFile(Context context, String path, int defaultOrientation) {
    	int orientation = ExifInterface.ORIENTATION_UNDEFINED;

    	if (path != null) {
            try {
            	ExifInterface exifReader = new ExifInterface(path); 
            	
            	// it will return 0 if it fails to read the exif if permission error
            	orientation = exifReader.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            	if (Logger.IS_DEBUG_ENABLED) {
            		Logger.debug(ImageModel.class, "getImageOrientationFromFile: got " + orientation + " for " + path);
            	}
            }
            catch (Exception e) {
            	if (Logger.IS_DEBUG_ENABLED) {
            		Logger.debug(ImageModel.class, "Exception caught while using ExifInterface:", e);
            	}
            }
        }

    	if (orientation <= ExifInterface.ORIENTATION_UNDEFINED || orientation > ExifInterface.ORIENTATION_ROTATE_270) {
    		orientation = defaultOrientation;
    	}
        return orientation;
    }

    /**
     * Get orientation of the image from input stream.
     * 
     * We use Metadata Extrator there as Android's ExifInterface doesn't support reading Exif from input stream.
     * 
     * @param context
     * @param uri
     * @return
     */
    private static int getImageOrientationFromUri(Context context, Uri uri, int defaultOrientation) {
    	int orientation = ExifInterface.ORIENTATION_UNDEFINED;
    	InputStream is = null;

    	if (uri != null) {
            try {
            	// create an input stream from the path
                is = context.getContentResolver().openInputStream(uri);
                Metadata meta = JpegMetadataReader.readMetadata(is);
                for (Directory directory : meta.getDirectories()) {
                	if (directory instanceof ExifIFD0Directory) {
                		if (directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                			orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                        	if (Logger.IS_DEBUG_ENABLED) {
                        		Logger.debug(ImageModel.class, "getImageOrientationFromUri: got " + orientation + " for " + uri);
                        	}
                		}
                		break;
                	}
                }
            }
            catch (Exception e) {
            	if (Logger.IS_DEBUG_ENABLED) {
            		Logger.debug(ImageModel.class, "Exception caught while getting image orientation:", e);
            	}
            }
            finally {
            	if (is != null) {
            		try {
            			is.close();
            		}
            		catch (Exception e) {
            		}
            	}
            }
        }

    	if (orientation <= ExifInterface.ORIENTATION_UNDEFINED || orientation > ExifInterface.ORIENTATION_ROTATE_270) {
    		orientation = defaultOrientation;
    	}        
        return orientation;
    }

    @Override
	public String toString() {
		return super.toString() + ", imgSize = " + mWidth + "x" + mHeight;
    }

	@Override
	public boolean isLoaded(Rect dims) {
		if (mBitmapLoaded && dims != null) {
			mBitmapLoaded = mLastDims.equals(dims) || dims.contains(mLastDims);
		}
		return mBitmapLoaded;
	}
	
	@Override
	public void resetIfLoadRequired() {
		if (mBitmapLoaded && mBitmap == null) {
			mBitmapLoaded = false;
		}
	}
}
