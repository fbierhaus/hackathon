package com.verizon.mms.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.rocketmobile.asimov.Asimov;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.ui.UriData;
import com.verizon.mms.ui.UriImage;

/**
 * Singleton BitmapManager. Helps load MMS images
 */
public enum BitmapManager {
	INSTANCE;

	private final Context context;
	private final BlockingQueue<Runnable> queue;
	private final int minWidth;    // smallest px to which bitmaps are scaled
	private final int dispWidth;   // display's width in px * DISP_SCALE_FACTOR
	private final int dispHeight;  // display's height in px * DISP_SCALE_FACTOR
	private int orientation;
	private final ThreadPoolExecutor pool;
	private ArrayList<BitmapUser> users;
	private static boolean hasCaptureFrame = true; // assume true until tried

	public static final float SCALE_DOWN_FACTOR = 0.75f;  // amount to scale down by on each attempt
	private static final int MIN_WIDTH = 50;  // the smallest reasonable dp to which bitmaps can scale

	// images without target dimensions are scaled to no more than this * display dimensions
	private static final float DISP_SCALE_FACTOR = 1.5f;

	private static final int MAX_MEM_TRIES = 2; // max number of times to retry on an OOM

	private static final int BUF_EXPAND_THRESHOLD = 512 * 1024;
	private static final int BUF_EXPAND_INCREMENT = 256 * 1024;

	private static final String CONTENT_PART = VZUris.getMmsUri() + "/part/";

	public static final int UNKNOWN = -1;  // indicates unknown image dimensions

	private static final float CACHE_PERCENT = 0.5f;        // percent of available memory to allocate to user cache
	private static final long CACHE_MAX = 16 * 1024 * 1024; // max user cache size

	private static final float HEAP_PERCENT = 0.75f;  // don't try to allocate more than this much of the heap


	public interface OnBitmapLoaded {
		void onBitmapLoaded(String url, Bitmap bmp, ImageView imageView);
	}


	public interface BitmapUser {
		void freeBitmapMemory();  // called when the user should release memory
		void debugDump();
	}


	ImageCacheParams mCacheParams;
	private static final float MIN_MESSAGE_WIDTH_RATIO = 295f / 614f;  // min message width
	private static final float IMAGE_WIDTH_HEIGHT_RATIO = 1.5f;        // ratio of min/max image height to width
	public final int cachedImageWidth;
	public final int cachedImageHeight;
	
	private BitmapManager() {
		context = Asimov.getApplication();
		queue = new ArrayBlockingQueue<Runnable>(15);
		pool = new ThreadPoolExecutor(4, 4, 20, TimeUnit.SECONDS, queue);
		users = new ArrayList<BitmapUser>(4);

		// calculate the display scaling limits and minimum bitmap width in pixels
		final WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		final DisplayMetrics metrics = new DisplayMetrics();
		final Display disp = wm.getDefaultDisplay();
		orientation = disp.getOrientation();
		disp.getMetrics(metrics);
		dispWidth = Math.round(metrics.widthPixels * DISP_SCALE_FACTOR);
		dispHeight = Math.round(metrics.heightPixels * DISP_SCALE_FACTOR);
		minWidth = Math.round(MIN_WIDTH * metrics.density);
		
		if (MmsConfig.allowConvDiskCache()) {
			mCacheParams = new ImageCacheParams(context, IMAGE_CACHE_DIR); 
			initDiskCache(mCacheParams);
		}
		
		int listWidth = (int)(Math.min(metrics.widthPixels, metrics.heightPixels)/* * 0.8*/);
		
		if (Util.isMultipane(disp)) {
			int contentWeight = context.getResources().getInteger(R.integer.tabletContentWeight);
			int listWeight = context.getResources().getInteger(R.integer.tabletListWeight);
			int maxWidth = (int)(Math.min(metrics.widthPixels, metrics.heightPixels)/* * 0.8*/);
			
			listWidth = (maxWidth * contentWeight)/(listWeight + contentWeight);
		}
		listWidth = Math.round(listWidth * MIN_MESSAGE_WIDTH_RATIO);
		
		cachedImageWidth = listWidth;
		cachedImageHeight = Math.round(listWidth / IMAGE_WIDTH_HEIGHT_RATIO);
	}

	public void onConfigurationChanged(Configuration config) {
		orientation = config.orientation;
	}

	private static class QueuedJob {
		private String url;
		private Bitmap bmp;
		private ImageView view;
		private OnBitmapLoaded listener;

		private QueuedJob(String url, Bitmap bmp, ImageView view, OnBitmapLoaded listener) {
			this.url = url;
			this.bmp = bmp;
			this.view = view;
			this.listener = listener;
		}
	}

	private void queueJob(final String url, final ImageView imageView, final int width, final int height,
			final boolean fill, final boolean readCache, final boolean writeCache, final OnBitmapLoaded listener) {
		try {
			pool.execute(new Runnable() {
				@Override
				public void run() {
					final Bitmap bmp = getBitmap(url, width, height, fill, readCache, writeCache);
			    	if (Logger.IS_DEBUG_ENABLED) {
			    		Logger.debug("BitmapManager: queueJob: got " + bmp + " for view " + imageView + ", url " + url);
			    	}
					final Message message = Message.obtain();
					message.obj = new QueuedJob(url, bmp, imageView, listener);
					handler.sendMessage(message);
				}
			});
		} catch (RejectedExecutionException reh) {
			// TODO: retry
		}
	}

	private final Handler handler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(Message msg) {
			final QueuedJob result = (QueuedJob)msg.obj;
			final ImageView imageView = result.view;
			final String url = result.url;
			final Bitmap bmp = result.bmp;
			if (bmp != null) {
				imageView.setImageBitmap(bmp);
		    	if (Logger.IS_DEBUG_ENABLED) {
		    		Logger.debug("BitmapManager: handleMessage: set " + imageView + " with " + bmp + " for url " + url);
		    	}
			} else {
				// TODO: accept default image to show
		    	if (Logger.IS_DEBUG_ENABLED) {
		    		Logger.debug("BitmapManager: handleMessage: failed to get bitmap for " + imageView);
		    	}
			}

			// notify the listener
			final OnBitmapLoaded listener = result.listener;
			if (listener != null) {
				listener.onBitmapLoaded(url, bmp, imageView);
			}
		}
	};

	/**
	 * Load an image, trying the cache first and loading in the background if not cached.
	 * 
	 * @param url Source uri of the image
	 * @param imageView ImageView to load with the bitmap when retrieved
	 * @param width Target width of the image
	 * @param height Target height of the image
	 * @param fill True to make shortest side fit, otherwise longest side will fit
	 * @param listener Listener to call back when the bitmap is loaded or null if none
	 */
	public void loadBitmap(final String url, final ImageView imageView, final int width, final int height,
			boolean fill, final OnBitmapLoaded listener) {
		//queueJob(url, imageView, width, height, fill, listener);
		loadBitmap(url, imageView, width, height, fill, false, false, listener);
	}
	
	/**
	 * Load an image, trying the cache first and loading in the background if not cached.
	 * 
	 * @param url Source uri of the image
	 * @param imageView ImageView to load with the bitmap when retrieved
	 * @param width Target width of the image
	 * @param height Target height of the image
	 * @param fill True to make shortest side fit, otherwise longest side will fit
	 * @param readCache true to read the image from the disk cache
	 * @param writeCache true to write the processed image into the cache
	 * @param listener Listener to call back when the bitmap is loaded or null if none
	 */
	public void loadBitmap(final String url, final ImageView imageView, final int width, final int height,
			boolean fill, boolean readCache, boolean writeCache, final OnBitmapLoaded listener) {
		queueJob(url, imageView, width, height, fill, readCache, writeCache, listener);
	}

	private InputStream openAndGetInputStream(final Context ctx, final String url, boolean isContent)
			throws MalformedURLException, IOException {
		final InputStream stream;
		if (isContent || url.startsWith(CONTENT_PART)) {
			Uri uri = Uri.parse(url);
			stream = ctx.getContentResolver().openInputStream(uri);
		} else {
			stream = (InputStream)new URL(url).getContent();
		}
		if (stream == null) {
			throw new IOException("Unable to open " + url);
		}
		return stream;
	}

	public Bitmap getBitmap(final String url, int targetWidth, int targetHeight, boolean fill) {
		return getBitmap(url, targetWidth, targetHeight, fill, false, false);
	}
	
	public Bitmap getBitmap(final String url, int targetWidth, int targetHeight, boolean fill, boolean readCache, boolean writeCache) {
		// use display dimensions if target size is unspecified
		if (targetWidth <= 0 || targetHeight <= 0) {
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				targetWidth = dispHeight;
				targetHeight = dispWidth;
			}
			else {
				targetWidth = dispWidth;
				targetHeight = dispHeight;
			}
		}

		return getBitmap(url, UNKNOWN, UNKNOWN, targetWidth, targetHeight, minWidth,
			fill, true, true, ExifInterface.ORIENTATION_UNDEFINED, false, readCache, writeCache);
	}

	public Bitmap getBitmap(String url, int width, int height, int dstWidth, int dstHeight, int minWidth,
			boolean fill, boolean scaleUp, boolean bits16, int exifOrientation, boolean isContent) {
		return getBitmap(url, width, height, dstWidth, dstHeight, minWidth, fill, scaleUp, 
				bits16, exifOrientation, isContent, false, false);
	}
	
	public Bitmap getBitmap(String url, int width, int height, int imageWidth, int imageHeight, int minWidth,
			boolean fill, boolean scaleUp, boolean bits16, int exifOrientation, boolean isContent, boolean readFromCache, boolean writeToCache) {
		int dstWidth = imageWidth;
		int dstHeight = imageHeight;
		boolean useDiskCache = isMMS(url) & MmsConfig.allowConvDiskCache();
		String urlKey = null;
		
		//dont write to cache if we are running on the main thread
		writeToCache = Looper.myLooper() != Looper.getMainLooper();
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "getBitmap: " + url + ": orientation = " + exifOrientation +
				", minWidth = " + minWidth + ", fill = " + fill + ", scaleUp = " + scaleUp +
				", isContent = " + isContent + ", target = " + dstWidth + "x" + dstHeight + "useDiskCache" + useDiskCache);
		}
		
		if (useDiskCache && readFromCache) {
			try {
				urlKey = getBitmapKey(Uri.parse(url));
				
				if (urlKey != null) {
					Bitmap bitmap = getBitmapFromDiskCache(urlKey, getBitmapScale(fill, dstWidth, dstHeight, bits16, scaleUp), true);

					if (bitmap != null) {
						return bitmap;
					}
				}
			} catch (OutOfMemoryError e) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "getBitmap: " + url + ": OOM on fetching image from the cache");
				}
				return null;
			}
			if (cachedImageHeight > 0 && cachedImageWidth > 0) {
				dstWidth = cachedImageWidth;
				dstHeight = cachedImageHeight;
			}
		}
		
		// use default min size if not specified
		if (minWidth <= 0) {
			minWidth = this.minWidth;
		}

		final Options options = new Options();
		options.inPurgeable = true;
		options.inPreferredConfig = bits16 ? Config.RGB_565 : Config.ARGB_8888;

		Bitmap src = null;
		Bitmap bmp = null;
		try {
			// get image bounds if needed
			if (width <= 0 || height <= 0) {
				options.inJustDecodeBounds = true;
				decodeStream(url, isContent, options, false);
				width = options.outWidth;
				height = options.outHeight;
				options.inJustDecodeBounds = false;
			}

			// swap width and height if image will be rotated below
			final int srcWidth;
			final int srcHeight;
			final boolean rotate = exifOrientation == ExifInterface.ORIENTATION_ROTATE_90
					|| exifOrientation == ExifInterface.ORIENTATION_ROTATE_270;
			if (rotate) {
				srcWidth = height;
				srcHeight = width;
			}
			else {
				srcWidth = width;
				srcHeight = height;
			}

			// calculate the proper scale to get to the target size
			float bmpScale;
			if (dstWidth <= 0 || dstHeight <= 0) {
				// unspecified target size means same as source
				dstWidth = srcWidth;
				dstHeight = srcHeight;
				bmpScale = 1;
			} else {
				// scale to either fill (shortest side fits) or fit (longest side fits)
				final float wratio = (float)dstWidth / srcWidth;
				final float hratio = (float)dstHeight / srcHeight;
				bmpScale = fill ? Math.max(wratio, hratio) : Math.min(wratio, hratio);

				// don't scale up unless allowed
				if (bmpScale > 1 && !scaleUp) {
					dstWidth = srcWidth;
					dstHeight = srcHeight;
					bmpScale = 1;
				}
			}

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "getBitmap: " + url + ": img = " + srcWidth + "x" + srcHeight +
					", target = " + dstWidth + "x" + dstHeight + ", bmpScale = " + bmpScale);
			}

			int scale = 1;
			int lastScale = 0;
			boolean failed = false;

			while (true) {
				// decrease target size if we would exceed available memory or we have failed
				while (failed || !enoughMemory(getBitmapSize(dstWidth, dstHeight, bits16))) {
					dstWidth = Math.round(dstWidth * SCALE_DOWN_FACTOR);
					dstHeight = Math.round(dstHeight * SCALE_DOWN_FACTOR);
					bmpScale *= SCALE_DOWN_FACTOR;
	
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "getBitmap: scaled down to " + dstWidth + "x" + dstHeight);
					}

					// bail if we reached our limit of scaling down
					if (dstWidth < minWidth) {
						return null;
					}

					failed = false;
				}

				// calculate the max input scaling that yields both sides larger than the target
				// in fill mode or the longer side larger than the target in fit mode
				//
				if (fill) {
					while (srcWidth / scale > dstWidth && srcHeight / scale > dstHeight) {
						scale *= 2;
					}
				}
				else {
					while (srcWidth / scale > dstWidth || srcHeight / scale > dstHeight) {
						scale *= 2;
					}
				}
				if (scale > 1) {
					scale /= 2;
				}

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "getBitmap: " + url + ": scale = " + scale + ", lastScale = " +
						lastScale + ", scaled = " + srcWidth / scale + "x" + srcHeight / scale +
						", target = " + dstWidth + "x" + dstHeight);
				}

				// read the bitmap from the stream if the scale has changed from the last attempt
				if (scale != lastScale) {
					lastScale = scale;
					if (src != null) {
						src.recycle();
					}
					options.inSampleSize = scale;
					try {
						src = decodeStream(url, isContent, options, true);
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "getBitmap: " + url + ": decoded = " +
								(src == null ? "null" : src.getWidth() + "x" + src.getHeight()));
						}
					}
					catch (OutOfMemoryError e) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "getBitmap: " + url + ": OOM on decode, needed " +
								getBitmapSize(srcWidth / scale, srcHeight / scale, bits16));
						}
						src = null;
					}
				}

				if (src != null) {
					// check if we need to scale further or otherwise transform
					final float scaledScale = bmpScale * scale;
					Matrix matrix = UriImage.getMatrix(exifOrientation);
					if (matrix == null && scaledScale == 1) {
						// no scaling or transforms needed
						bmp = src;
					}
					else {
						// scale to target size and apply any required transforms
						if (matrix == null) {
							matrix = new Matrix();
							matrix.setScale(scaledScale, scaledScale);
						}
						else {
							matrix.postScale(scaledScale, scaledScale);
						}
						try {
							bmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
						}
						catch (OutOfMemoryError e) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "getBitmap: " + url + ": OOM on scale, needed " +
									getBitmapSize(src.getWidth(), src.getHeight(), bits16));
							}
							recoverMemory();
						}
					}
					if (Logger.IS_DEBUG_ENABLED && bmp != null) {
						Logger.debug(getClass(), "getBitmap: " + url + ": after " +
							(bmp == src ? "not " : "") + "scaling by " + scaledScale + ": " +
							bmp.getWidth() + "x" + bmp.getHeight() + ", size = " + getBitmapSize(bmp) +
							", mem = " + Util.getMemString() + ", bmp = " + bmp);
					}
				}

				if (bmp != null) {
					if (useDiskCache && writeToCache && urlKey != null) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(" writing to cache ", "url " + url + "after scaling size " + getBitmapSize(bmp));
						}
						addBitmapToCache(urlKey, bmp);
					}
					return bmp;
				}

				failed = true;
			}
		}
		catch (Exception e) {
			Logger.error(getClass(), url, e);
			return null;
		}
		finally {
			if (src != null && src != bmp) {
				src.recycle();
			}
		}
	}

	private String getBitmapKey(Uri uri) {
		try {
			UriData ururiData = new UriData(context, uri);
			return ururiData.getSrc() + uri.getLastPathSegment();
		} catch (Exception e) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.error(" Got an exception in BitmapManager.getBitmapKey " + e);
			}
		}
		return null;
	}

	public Bitmap createVideoThumbnail(Context context, Uri uri) {
		Bitmap bitmap = null;
		//boolean useDiskCache = isMMS(uri.toString()) & MmsConfig.allowConvDiskCache();
		boolean useDiskCache = false;
		
		if (useDiskCache) {
			try {
				bitmap = getBitmapFromDiskCache(uri.toString(), null, false);

				if (bitmap != null) {
					return bitmap;
				} 
			} catch (OutOfMemoryError e) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "getBitmap: " + uri.toString() + ": OOM on fetching image from the cache");
				}
				return null;
			}
		}
		
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			if (hasCaptureFrame) {
				retriever.setDataSource(context, uri);
				bitmap = retriever.captureFrame();
				
				if (useDiskCache) {
					addBitmapToCache(uri.toString(), bitmap);
				}
			}
			else {
				bitmap = getVideoThumbnailAlternatively(context, retriever, uri);
				if (useDiskCache) {
					addBitmapToCache(uri.toString(), bitmap);
				}
			}
		}
		catch (Throwable t) {
			if (t instanceof NoSuchMethodError) {
				hasCaptureFrame = false;
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug("BitmapManager: device doesn't have captureFrame, trying alternative");
				}
			}
			else {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug("error getting thumbnail for " + uri + ", trying alternative", t);
				}
			}
			bitmap = getVideoThumbnailAlternatively(context, retriever, uri);
			if (useDiskCache) {
				addBitmapToCache(uri.toString(), bitmap);
			}
		}
		finally {
			try {
				retriever.release();
			} catch (Throwable t) {
				// Ignore failures while cleaning up.
			}
		}
		return bitmap;
	}
	
	public void loadVideoThumbnail(final String url, final ImageView imageView,
			final int width, final int height, OnBitmapLoaded listener) {
		final Context context = this.context;
		Uri uri = Uri.parse(url);
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		Bitmap bitmap = null;
		try {
			if (hasCaptureFrame) {
				retriever.setDataSource(context, uri);
				bitmap = retriever.captureFrame();
			} else {
				bitmap = getVideoThumbnailAlternatively(context, retriever, uri);
			}
		}
		// Load bitmap alternatively
		catch (Throwable t) {
			if (t instanceof NoSuchMethodError) {
				hasCaptureFrame = false;
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(),
							"device doesn't have captureFrame, trying alternative");
				}
			} else {
				Logger.error("error getting thumbnail for " + uri
						+ ", trying alternative", t);
			}
			bitmap = getVideoThumbnailAlternatively(context, retriever, uri);
		} finally {
			try {
				retriever.release();
			} catch (Throwable t) {
				// Ignore failures while cleaning up.
			}

			// missing thumbnail
			if (null == bitmap) {
				bitmap = BitmapManager.INSTANCE.decodeResource(
						context.getResources(),
						R.drawable.ic_missing_thumbnail_video);
			}
		}

		// Set the view
		if (null != bitmap) {

			if (height != UNKNOWN && width != UNKNOWN) {
				bitmap = createScaledBitmap(bitmap, width, height, true);
			}

			imageView.setImageBitmap(bitmap);
		}

		// notify the listener
		if (listener != null) {
			listener.onBitmapLoaded(url, bitmap, imageView);
		}
	}

	private Bitmap getVideoThumbnailAlternatively(Context context,
			MediaMetadataRetriever retriever, Uri uri) {
		Bitmap localBitmap = null;
		try {
			retriever.setDataSource(context, uri);
			localBitmap = retriever.getFrameAtTime();
		} catch (Throwable t) {
			Logger.error("error getting thumbnail for " + uri, t);
		}
		return localBitmap;
	}

	private interface RetryCall {
		public Object call();
	}

	private Object callWithRetry(RetryCall call, boolean throwOOMs) {
		int tries = 0;
		for (;;) {
			try {
				return call.call();
			} catch (Throwable t) {
				if (t instanceof OutOfMemoryError) {
					// bail if we're past the retry limit
					if (++tries >= MAX_MEM_TRIES) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "callWithRetry: failing with mem = " + Util.getMemString(), t);
						}
						if (throwOOMs) {
							throw (OutOfMemoryError)t;
						}
					} else {
						// otherwise try to get below the heap break by clearing
						// the cache and forcing a gc
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "callWithRetry: tries = " + tries, t);
						}

						recoverMemory();

						// try again
						continue;
					}
				} else {
					Logger.error(t);
				}
			}
			break;
		}

		return null;
	}

	/**
	 * Checks if there should be enough memory available for the given allocation and
	 * tries to free up memory if not.
	 *
	 * @param size required memory allocation
	 * @return true if there should be enough memory to satisfy the allocation
	 */
	private boolean enoughMemory(int size) {
		final long start;
		if (Logger.IS_DEBUG_ENABLED) {
			start = SystemClock.uptimeMillis();
		}

		boolean enough = size < getAvailableMemory();
		if (!enough) {
			recoverMemory();
			System.gc();
			System.gc();
			enough = size < getAvailableMemory();
		}

		if (!enough) {
			if (Logger.IS_MEMDUMP_ENABLED) {
				if (MmsConfig.dieOnOom()) {
					// ensure it gets dumped before we're killed
					Util.dumpMemory();
				}
				else {
					Util.dumpMemoryWrapper();
				}
			}

			final String msg = "not enough memory for " + size + " bitmap: " +
				Util.getMemString() + ", max = " + Util.getMaxMemory(true);

			if (Logger.IS_ERROR_ENABLED) {
				Logger.error(false, getClass(), msg);
			}
			Logger.postErrorToAcra(getClass(), "not enough memory");
			
			// if we're supposed to die rather than fail then kill ourselves
			if (MmsConfig.dieOnOom()) {
				Process.killProcess(Process.myPid());
			}
		}
		else if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "enoughMemory: returning " + enough + " for " + size +
				" in " + (SystemClock.uptimeMillis() - start) + "ms");
		}

		return enough;
	}

	private long getAvailableMemory() {
		final long max = (long)((double)Util.getMaxMemory(true) * HEAP_PERCENT);
		final long free = max - Util.getUsedMemory(true);
		return free >= 0 ? free : 0;
	}

	private void recoverMemory() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "recoverMemory: before: mem = " + Util.getMemString());
		}

		freeBitmapMemory();

		if (Logger.IS_DEBUG_ENABLED) {
			System.gc();
			System.gc();
			Logger.debug(getClass(), "recoverMemory: after: mem = " + Util.getMemString());
		}
	}

	@SuppressWarnings("unchecked")
	private void freeBitmapMemory() {
		final ArrayList<BitmapUser> list;
		synchronized (users) {
			list = (ArrayList<BitmapUser>)users.clone();
		}
		if (Logger.IS_DEBUG_ENABLED) {
			for (BitmapUser user : list) {
				user.debugDump();
			}			
		}
		for (BitmapUser user : list) {
			user.freeBitmapMemory();
		}
	}

	public void addBitmapUser(BitmapUser user) {
		synchronized (users) {
			users.add(user);
		}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "addBitmapUser: after: users = " + users);
		}
	}

	public void removeBitmapUser(BitmapUser user) {
		synchronized (users) {
			users.remove(user);
		}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "removeBitmapUser: after: users = " + users);
		}
	}

	public static int getBitmapSize(Bitmap bmp) {
		return bmp == null ? 0 : bmp.getRowBytes() * bmp.getHeight();
	}

	private int getBitmapSize(int width, int height, boolean bits16) {
		return width * height * (bits16 ? 2 : 4);
	}

	/**
	 * @return the max target image size for this device
	 */
	public int getMaxImageSize() {
		return dispWidth * dispHeight;
	}

	public Bitmap decodeStream(final InputStream stream) {
		// force to 16 bits
		final Options opts = new Options();
		opts.inPreferredConfig = Bitmap.Config.RGB_565;
		return decodeStream(stream, null, opts, false);
	}

	public Bitmap decodeStream(final InputStream stream, final Rect outPadding, final Options opts) {
		return decodeStream(stream, outPadding, opts, false);
	}

	private Bitmap decodeStream(final InputStream stream, final Rect outPadding, final Options opts, boolean throwOOMs) {
		try {
			// try native bitmap decode first
			return BitmapFactory.decodeStream(stream, outPadding, opts);
		}
		catch (OutOfMemoryError e) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "decodeStream: out of memory, trying local buffer: " + Util.getMemString());
			}

			// if this isn't just a decode call then try reading the stream into a byte
			// buffer that we can then attempt to decode with retries
			// we do our own buffer management since ByteArrayOutputStream reallocates by
			// doubling, which quickly leads to OOMs
			//
			if (opts == null || !opts.inJustDecodeBounds) {
				try {
					int bufsiz = 32 * 1024;
					byte[] buf = new byte[bufsiz];
					int toread = bufsiz;
					int i = 0;
					int num;
					while ((num = stream.read(buf, i, toread)) != -1) {
						i += num;
						toread -= num;
		
						if (toread <= 0) {
							// expand buffer, exponentially until the threshold and then linearly
							if (bufsiz < BUF_EXPAND_THRESHOLD) {
								bufsiz *= 2;
							}
							else {
								bufsiz += BUF_EXPAND_INCREMENT;
							}
							final byte[] temp = new byte[bufsiz];
							System.arraycopy(buf, 0, temp, 0, i);
							buf = temp;
							toread = bufsiz - i;

							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "decodeStream: expanded buf to " + bufsiz + ", i = " + i);
							}
						}
					}

					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "decodeStream: calling decodeByteArray with len = " + i);
					}

					final Bitmap bmp = decodeByteArray(buf, 0, i, opts);
					if (throwOOMs && bmp == null) {
						throw e;
					}
					return bmp;
				}
				catch (OutOfMemoryError oom) {
					if (throwOOMs) {
						throw oom;
					}
				}
				catch (Throwable t) {
					Logger.error(t);
				}
			}
		}
		catch (Throwable t) {
			Logger.error(t);
		}

		if (opts != null) {
			opts.outHeight = -1;
			opts.outWidth = -1;
		}

		return null;
	}

	public Bitmap decodeByteArray(final byte[] data, final int offset,
			final int length) {
		return decodeByteArray(data, offset, length, null);
	}

	private Bitmap decodeByteArray(final byte[] data, final int offset,
			final int length, final Options opts) {
		return (Bitmap) callWithRetry(new RetryCall() {
			public Object call() {
				return BitmapFactory.decodeByteArray(data, offset, length, opts);
			}
		}, false);
	}

	public Bitmap decodeStream(final String url, final boolean isContent, final Options opts, boolean throwOOMs) {
		return (Bitmap) callWithRetry(new RetryCall() {
			public Object call() {
				InputStream stream = null;
				Bitmap bmp = null;
				try {
					stream = openAndGetInputStream(context, url, isContent);
					bmp = BitmapFactory.decodeStream(stream, null, opts);
				}
				catch (Exception e) {
					Logger.error(getClass(), "decodeStream: " + url, e);
				}
				finally {
					if (stream != null) {
						try {
							stream.close();
						}
						catch (Exception e) {
						}
					}
				}
				return bmp;
			}
		}, throwOOMs);
	}

	public Bitmap decodeResource(final Resources res, final int id) {
		return (Bitmap) callWithRetry(new RetryCall() {
			public Object call() {
				return BitmapFactory.decodeResource(res, id);
			}
		}, false);
	}

	public Bitmap createScaledBitmap(final Bitmap source, final int width,
			final int height, final boolean filter) {
		return (Bitmap) callWithRetry(new RetryCall() {
			public Object call() {
				return Bitmap.createScaledBitmap(source, width, height, filter);
			}
		}, false);
	}

	public Bitmap viewToBitmap(View view, int dstWidth, int dstHeight, int minWidth, boolean bits16) {
		// use default min size if not specified
		if (minWidth <= 0) {
			minWidth = this.minWidth;
		}

		final Config config = bits16 ? Config.RGB_565 : Config.ARGB_8888;
		Bitmap bmp = null;
		boolean failed = false;
		try {
			while (true) {
				// decrease target size if we would exceed available memory or we have failed
				while (failed || !enoughMemory(getBitmapSize(dstWidth, dstHeight, bits16))) {
					dstWidth = Math.round(dstWidth * SCALE_DOWN_FACTOR);
					dstHeight = Math.round(dstHeight * SCALE_DOWN_FACTOR);

					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "viewToBitmap: scaled down to " + dstWidth + "x" + dstHeight);
					}

					// bail if we reached our limit of scaling down
					if (dstWidth < minWidth) {
						return null;
					}

					failed = false;
				}

				// decrease target size and try again
				try {
					bmp = Bitmap.createBitmap(dstWidth, dstHeight, config);
					final Canvas canvas = new Canvas(bmp);
					view.draw(canvas);
				}
				catch (OutOfMemoryError e) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "viewToBitmap: OOM, needed " +
							getBitmapSize(dstWidth, dstHeight, bits16));
					}
					recoverMemory();
				}

				if (bmp != null) {
					return bmp;
				}

				failed = true;
			}
		}
		catch (Exception e) {
			Logger.error(getClass(), e);
		}

		return null;
	}

	/**
	 * Returns a reasonable size for a user's cache based on the available memory.
	 * Note this assumes that only one cache of this size will be active at any time.
	 */
	public long getUserCacheSize(String logTag) {
		long cacheSize = (long)(Util.getAvailableMemory(true) * CACHE_PERCENT);
		if (cacheSize > CACHE_MAX) {
			cacheSize = CACHE_MAX;
		}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "getUserCacheSize: returning " + cacheSize + " for " + logTag);
		}
		return cacheSize;
	}

	private Options getBitmapScale (boolean fill, int dstWidth, int dstHeight, boolean bits16, boolean scaleUp) {
		// calculate the proper scale to get to the target size
		float bmpScale;
		if (dstWidth <= 0 || dstHeight <= 0) {
			// unspecified target size means same as source
			dstWidth = cachedImageWidth;
			dstHeight = cachedImageHeight;
			bmpScale = 1;
		}
		else {
			// scale to either fill (shortest side fits) or fit (longest side fits)
			final float wratio = (float)dstWidth / cachedImageWidth;
			final float hratio = (float)dstHeight / cachedImageHeight;
			bmpScale = fill ? Math.max(wratio, hratio) : Math.min(wratio, hratio);

			// don't scale up unless allowed
			if (bmpScale > 1 && !scaleUp) {
				dstWidth = cachedImageWidth;
				dstHeight = cachedImageHeight;
				bmpScale = 1;
			}
		}
		int scale = 1;
		// calculate the max input scaling that yields both sides larger than the target
		// in fill mode or the longer side larger than the target in fit mode
		//
		if (fill) {
			while (cachedImageWidth / scale > dstWidth && cachedImageHeight / scale > dstHeight) {
				scale *= 2;
			}
		}
		else {
			while (cachedImageWidth / scale > dstWidth || cachedImageHeight / scale > dstHeight) {
				scale *= 2;
			}
		}
		if (scale > 1) {
			scale /= 2;
		}

		final Options options = new Options();
		options.inPurgeable = true;
		options.inPreferredConfig = bits16 ? Config.RGB_565 : Config.ARGB_8888;
		options.inSampleSize = scale;
		options.inJustDecodeBounds = false;
		
		return options;
	}
	
	public String checkMemory() {
		final StringBuilder sb = new StringBuilder(Build.MANUFACTURER);
		sb.append(" ");
		sb.append(Build.MODEL);
		sb.append(" ");
		sb.append(Build.VERSION.SDK_INT);
		sb.append("\nbefore: ");
		sb.append(Util.getMemString());

		final int size = 256;
		ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>(512);
		int num = 0;
		while (true) {
			try {
				bitmaps.add(Bitmap.createBitmap(size, size, Config.RGB_565));
			}
			catch (OutOfMemoryError e) {
				sb.append("\nOOM at ");
				sb.append(Util.getMemString());
				break;
			}
			sb.append("\nallocated ");
			sb.append(++num);
			sb.append(", mem = ");
			sb.append(Util.getMemString());
		}

		for (Bitmap bitmap : bitmaps) {
			bitmap.recycle();
		}
		bitmaps = null;
		System.gc();

		return sb.toString();
	}
		
	
	private DiskLruCache mDiskLruCache;
    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;
	
	public void initDiskCache(ImageCacheParams mCacheParams) {
        // Set up disk cache
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache == null || mDiskLruCache.isClosed()) {
                File diskCacheDir = mCacheParams.diskCacheDir;
                if (mCacheParams.diskCacheEnabled && diskCacheDir != null) {
                    if (!diskCacheDir.exists()) {
                        diskCacheDir.mkdirs();
                    }
                    if (ImageCacheParams.getUsableSpace(diskCacheDir) > mCacheParams.diskCacheSize) {
                        try {
                            mDiskLruCache = DiskLruCache.open(
                                    diskCacheDir, 1, 1, mCacheParams.diskCacheSize);
                            
                            if (Logger.IS_DEBUG_ENABLED) {
                            	Logger.debug("Disk cache initialized");
                            }
                        } catch (final IOException e) {
                            mCacheParams.diskCacheDir = null;
                            Logger.error("initDiskCache - " + e);
                        }
                    }
                }
            }
            mDiskCacheStarting = false;
            mDiskCacheLock.notifyAll();
        }
    }
	
	private static final String IMAGE_CACHE_DIR = "images";


	/**
     * Adds a bitmap to both memory and disk cache.
     * @param data Unique identifier for the bitmap to store
     * @param bitmap The bitmap to store
     */
    public void addBitmapToCache(String data, Bitmap bitmap) {
        if (data == null || bitmap == null) {
            return;
        }


        synchronized (mDiskCacheLock) {
            // Add to disk cache
            if (mDiskLruCache != null) {
                final String key = hashKeyForDisk(data);
                OutputStream out = null;
                try {
                    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot == null) {
                        final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                        if (editor != null) {
                            out = editor.newOutputStream(ImageCacheParams.DISK_CACHE_INDEX);
                            bitmap.compress(
                                    mCacheParams.compressFormat, mCacheParams.compressQuality, out);
                            editor.commit();
                            out.close();
                        }
                    } else {
                        snapshot.getInputStream(ImageCacheParams.DISK_CACHE_INDEX).close();
                    }
                } catch (final IOException e) {
                	Logger.error("addBitmapToCache - " + e);
                } catch (Exception e) {
                	Logger.error("addBitmapToCache - " + e);
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {}
                }
            }
        }
    }

    /**
     * A hashing method that changes a string (like a URL) into a hash suitable for using as a
     * disk filename.
     */
    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] bytes) {
        // http://stackoverflow.com/questions/332079
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Get from disk cache.
     *
     * @param data Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    public Bitmap getBitmapFromDiskCache(String data, Options options, boolean isContent) {
        final String key = hashKeyForDisk(data);
        synchronized (mDiskCacheLock) {
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {}
            }
            if (mDiskLruCache != null) {
                InputStream inputStream = null;
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    if (snapshot != null) {
                    	if (Logger.IS_DEBUG_ENABLED) {
                    		Logger.debug("Disk cache hit");
                    	}
                        
                        inputStream = snapshot.getInputStream(ImageCacheParams.DISK_CACHE_INDEX);
                        if (inputStream != null) {
                        	final Bitmap bitmap = (options != null) ? BitmapFactory.decodeStream(inputStream, null, options) : BitmapFactory.decodeStream(inputStream);
                            return bitmap;
                        }
                    }
                } catch (final IOException e) {
                    Logger.error("getBitmapFromDiskCache - " + e);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {}
                }
            }
            return null;
        }
    }

    /**
     * Closes the disk cache associated with this ImageCache object. Note that this includes
     * disk access so this should not be executed on the main/UI thread.
     */
    public void close() {
        synchronized (mDiskCacheLock) {
            if (mDiskLruCache != null) {
                try {
                    if (!mDiskLruCache.isClosed()) {
                        mDiskLruCache.close();
                        mDiskLruCache = null;
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Disk cache closed");
                        }
                    }
                } catch (IOException e) {
                    Logger.error("close - " + e);
                }
            }
        }
    }
    
    /**
     * Check if the URL is an MMS.
     * 
     * @param url
     * @return
     */
    private boolean isMMS(String url) {
    	return url.startsWith("content://"+VZUris.getMmsUri().getAuthority()+"/part/");
    }
    
    public Bitmap createBitmap(final Bitmap source, final int x, final int y,
			final int width, final int height, final Matrix matrix,
			final boolean filter) {
		return Bitmap.createBitmap(source, x, y, width, height, matrix,
						filter);
	}
}
