package com.verizon.mms.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.helper.BitmapDB.BitmapRecord;

/**
 * Singleton BitmapManager. Helps load/cache MMS images
 * 
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * 
 */
public enum BitmapManager {
    /** The INSTANCE. */
    INSTANCE;

    private static final int              CACHE_SIZE = 50;		// 50 Bitmap object in memory
    private static final int			  BLOCKING_QUEUE_SIZE = 30;
    private static final int			  POOL_SIZE = 1;
        
	private static final int 			  DB_SIZE = 30;							// database max number of records
	private static final long			  DB_KEEP_TIME = (15*24*60*60*1000);	// 15 days

    public static final int               ERROR = -1;
    public static final int               OK = 0;
    public static final int               NOT_USED = 1;			// found but not used, e.g. image is smaller than the min dimension specified
    public static final int               NOT_FOUND = 404;

    // flags for scaling
    public static final int				SCALE_XY = 0;			// scale to width and height specified
    public static final int				SCALE_MAX_XY = (1);		// use width and height as max, and maintain aspect ratio
    public static final int				SCALE_MIN_XY = (1<<1);	// use width and height as min, and maintain aspect ratio
    
    public static final int				SCALE_NO_ENLARGE = (1<<2);	// do not scale up
    private static boolean hasCaptureFrame = true;
    private static boolean checkFlag(int flag, int bit) {
    	int mask = ~bit;
    	int k = flag & mask;
    	return k != 0;
    }
    
    /** The INVALID. */
    private final int                     INVALID    = -1;

    /** The cache. */
	private final SoftCache<String, BitmapEntry> mCache;

    private final BlockingQueue<Runnable> mQueue;

    /** The pool. */
    private final ThreadPoolExecutor      mPool;

    private static final boolean USE_SOFT_CACHE = false;

    public class BitmapEntry {
    	public String url;
    	public int result;					// store the last retrieving result, we don't want to re-download if it's not found error
    	public Bitmap bitmap;
    };

    private BitmapDB mCacheDB = null;

    /**
     * Create and open cache database.
     * 
     * @param context
     * @return
     */
   	private boolean openCacheDB(Context context) {
   		if (mCacheDB == null) {
   			mCacheDB = new BitmapDB(context.getApplicationContext());
   			mCacheDB.setStorageCount(DB_SIZE, DB_SIZE, DB_KEEP_TIME);
   		}
   		return mCacheDB.openReadWrite();
   	}
   	
   	/**
   	 * Close cache database.
   	 */
   	private void closeCacheDB() {
   		if (mCacheDB != null) {
   			mCacheDB.close();
   			mCacheDB = null;
   		}
   	}
    	
    /**
     * Instantiates a new bitmap manager.
     */
    BitmapManager() {
    	if (USE_SOFT_CACHE) {
    		mCache = new SoftCache<String, BitmapEntry>(CACHE_SIZE);
    	}
    	else {
    		mCache = null;
    	}
        mQueue = new ArrayBlockingQueue<Runnable>(BLOCKING_QUEUE_SIZE);
        mPool = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 20, TimeUnit.SECONDS, mQueue);
    }

    public class BitmapTask implements Runnable {
    	private Context context;
    	private String url;
    	private int width;
    	private int height;
    	private boolean canceled = false;
    	
		private class Listener {
			Handler handler;
			int msgCode;			
			public Listener(Handler handler, int msgCode) {
				this.handler = handler;
				this.msgCode = msgCode;
			}
		}    	
    	private ArrayList<Listener> listeners = new ArrayList<Listener>();
    	
    	private boolean done = false;
    	
    	public BitmapTask(Context context, Handler handler, int msgCode, String url, int width, int height) {
    		this.context = context;
    		this.url = url;
    		this.width = width;
    		this.height = height;
    		
    		addListener(handler, msgCode);
    	}
    	
		/**
		 * Add a listener only if the task is not done.
		 * 
		 * Return true if it's added, otherwise false.
		 * 
		 * @param handler
		 * @param msgCode
		 */
		public boolean addListener(Handler handler, int msgCode) {
			boolean ret = false;
			synchronized(listeners) {
				if (done == false && !canceled && !done) {
					Listener listener = new Listener(handler, msgCode);
					listeners.add(listener);
					ret = true;
				}
			}
			return ret;
		}		

    	public boolean cancel(Handler handler) {
			boolean ret = false;
    		synchronized(listeners) {
    			final int size = listeners.size();
    			for (int i = size - 1; i >= 0; --i) {
    				final Listener listener = listeners.get(i);
    				if (listener.handler == handler) {
//    					if (Logger.IS_DEBUG_ENABLED) {
//    						Logger.debug("###### Removed a task for url=" + url);
//    					}
    					listeners.remove(i);
    					ret = true;
    				}
    			}

    			if (listeners.size() == 0) {
    				// remove itself from the queue
//    				if (Logger.IS_DEBUG_ENABLED) {
//    					Logger.debug(this.getClass(), "####### remove task url=" + url);
//    				}
    				// fix BZ#1966
//    				mQueue.remove(this);
    				canceled = true;	// mark it canceled and let it stay in the queue 
    			}
    		}    		
    		return ret;
    	}
    	
        @Override
        public void run() {
        	if (canceled) {
            	if (Logger.IS_DEBUG_ENABLED) {
            		Logger.debug(getClass(), Thread.currentThread().getId()+ "- Job already canceled, url=" + url + " width=" + width + " height=" + height);
            	}
            	return;
        	}
        	
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(getClass(), Thread.currentThread().getId()+ "- Job url=" + url + " width=" + width + " height=" + height);
        	}
        	
        	// check cache again as it might be fetched in previous job                    
        	//            String cacheKey = getCacheKey(url, width, height);
        	BitmapEntry bitmapEntry = getFromCache(context, url, width, height, true);
        	int flag = SCALE_MAX_XY | SCALE_NO_ENLARGE;	// always scale to smaller and maintains aspect ratio
        	int result = OK;

        	if (bitmapEntry == null) {
        		// it's not in the cache, download it
        		bitmapEntry = downloadBitmap(context, url, width, height, flag);
        		if (bitmapEntry == null || bitmapEntry.bitmap == null) {
        			if (Logger.IS_DEBUG_ENABLED) {
        				Logger.debug(getClass(), "url=" + url + " download failed.");
        			}
        			if (bitmapEntry != null) {
        				result = bitmapEntry.result;
        			}
        			else {
        				result = ERROR;
        			}
        		}
        		else {
        			if (Logger.IS_DEBUG_ENABLED) {
        				Logger.debug(getClass(), "url=" + url + " downloaded.");
        			}
        			result = bitmapEntry.result;
        		}
        	}
        	else {
        		if (Logger.IS_DEBUG_ENABLED) {
        			Logger.debug(getClass(), "Found image in cache before job, url=" + url);
        		}
        		result = bitmapEntry.result;                    	
        	}
        	
        	final BitmapEntry entry = bitmapEntry;
        	
			synchronized(listeners) {
				done = true;
				
				for (Listener listener : listeners) {
					Handler handler = listener.handler;
					if (handler != null) {
						// handler can be null if it's a request to prefetch
						Message message = handler.obtainMessage(listener.msgCode);
						message.arg1 = result;
						message.obj = entry;

						handler.sendMessage(message);
					}
				}
				// remove all
				listeners.clear();
			}
        }
    }
    
    /**
     * Queue job.
     * 
     * @param ctx
     *            the ctx
     * @param url
     *            the url
     * @param imageView
     *            the image view
     * @param width
     *            the width
     * @param height
     *            the height
     */
    public BitmapTask queueJob(final Context ctx, final String url, final int width, final int height,
    						   final int minDimension, final Handler listener, final int msgCode) {
		boolean newTask = true;
       	BitmapTask task = null;
		
		// check if the link is being fetched or in the queue
		Iterator<Runnable> it = mQueue.iterator();
		while (it.hasNext()) {
			BitmapTask old = (BitmapTask)it.next();
			
			if (old.url.equals(url)) {				
				newTask = !old.addListener(listener, msgCode);
				
				if (newTask == false) {
					task = old;
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "#=> found task in queue link=" + url);
					}
					break;
				}
			}
		}

       	// image not found in cache, queue the job to download       	
       	if (newTask) {
           	if (Logger.IS_DEBUG_ENABLED) {
           		Logger.debug(getClass(), "Queue Bitmap Job. Key=" + url, "w=" + width + " h=" + height);
           	}
    	
           	try {
           		task = new BitmapTask(ctx, listener, msgCode, url, width, height);
           		mPool.execute(task);
           	}
           	catch (RejectedExecutionException reh) {
           		// TODO: something interesting here?
           		Logger.warn(getClass(), "Queue=" + mQueue.size() + " threads=" + mPool.getActiveCount() + " err=" , reh );
           		task = null;
           	}
       	}

       	return task;
    }

    /**
     * Get cached Bitmap entry from either memory cache only.
     */
    public BitmapEntry getFromMemoryCache(Context context, String url, int width, int height) {
    	return getFromCache(context, url, width, height, false);
    }
    
//    /**
//     * Get cached Bitmap entry from either memory cache or persistent cache.
//     * 
//     * @param context
//     * @param url
//     * @param width
//     * @param height
//     * @return
//     */
//    public BitmapEntry getFromCache(Context context, String url, int width, int height) {
//    	return getFromCache(context, url, width, height, false);
//    }
    
    public BitmapEntry getFromCache(Context context, String url, int width, int height, boolean fromDB) {
        BitmapEntry bitmap = null;

        // generate cache from url, width and height
        String cacheKey = getCacheKey(url, width, height);
        
    	if (USE_SOFT_CACHE) {
	        // first check memory cache
	       	bitmap = mCache.getFromCache(cacheKey);
	       	
			if (bitmap != null) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "Bitmap " + cacheKey + " found in memory cache, result=" + bitmap.result);
				}
				return bitmap;
			}
    	}
//    	else {
//    		if (Logger.IS_DEBUG_ENABLED) {
//    			Logger.debug(getClass(), "Using memory cache is disabled");
//    		}
//    	}

		if (fromDB == true) {
//    		if (Logger.IS_DEBUG_ENABLED) {
//    			Logger.debug(getClass(), "Checking database cache");
//    		}
			// if it's not found in memory, try persistent cache
			synchronized(this) {
				if (openCacheDB(context)) {
					try {
						BitmapRecord record = mCacheDB.getBitmap(cacheKey);
						if (record != null) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "Bitmap " + cacheKey + " found in db cache, result="+ record.responseCode);
							}

							if (record.bitmap != null) {
								// we have image
								bitmap = new BitmapEntry();
								bitmap.result = record.responseCode;
								bitmap.url = url;
								ByteArrayInputStream bais = new ByteArrayInputStream(record.bitmap);
								bitmap.bitmap = BitmapFactory.decodeStream(bais, null, null);
								bais.close();

								if (bitmap.bitmap == null) {
									// failed to decode?
									if (Logger.IS_WARNING_ENABLED) {
										Logger.warn(getClass(), "  Failed to decode bitmap from cache DB");
									}
									bitmap = null;
								}
								else {
							    	if (USE_SOFT_CACHE) {
								        // add it to memory cache
								       	mCache.putToCache(cacheKey, bitmap);      	
							    	}
								}
							}
						}
					}
					catch (Throwable t) {
						if (Logger.IS_ERROR_ENABLED) {
							Logger.error(getClass(), "Exception getting bitmap from cache DB:", t);
						}
						bitmap = null;
					}
					closeCacheDB();
				}
			}
			
//	    	if (bitmap != null && bitmap.bitmap != null) {
//	    		if (Logger.IS_DEBUG_ENABLED) {
//	    			Logger.debug("======> Image size" + bitmap.bitmap.getWidth() + "x" + bitmap.bitmap.getHeight() + " <== " + url);
//	    		}
//	    	}
		}
    	
		return bitmap;
    }

    public void putToCache(Context context, String url, int width, int height, BitmapEntry entry) {
    	String cacheKey = getCacheKey(url, width, height);
	    	
    	if (USE_SOFT_CACHE) {
	    	// we first add it to memory cache
	    	mCache.putToCache(cacheKey, entry);
    	}
	    	
    	// add it to the database only if it has the bitmap, i.e. not 404 NOT FOUND error
    	if (isMMS(url) == false && entry.bitmap != null) {
    		try {
    			// convert bitmap into a byte array
    			ByteArrayOutputStream baos = new ByteArrayOutputStream();
    			entry.bitmap.compress(CompressFormat.JPEG, 80, baos);
    			byte[] bitmap = baos.toByteArray();
    			baos.close();

    			synchronized(this) {
    				if (openCacheDB(context)) {
    					try {
    						// insert a new record
    						mCacheDB.insertRecord(cacheKey, null, entry.result, bitmap, null, System.currentTimeMillis());
    					} catch (Exception e) {
    						Logger.error(e);
    					}

    					closeCacheDB();
    				}
    			}
    		}
    		catch (Exception e) {
    			if (Logger.IS_WARNING_ENABLED) {
    				Logger.warn("Failed to add " + cacheKey + " to cache DB: ", e);
    			}
    		}
    	}
    }

    /**
     * Load mms bitmap.
     * 
     * @param ctx
     *            the ctx
     * @param partId
     *            the part id
     * @param imageView
     *            the image view
     */
//    public void loadMmsBitmap(final Context ctx, final String url, final ImageView imageView) {
//        loadMmsBitmap(ctx, url, imageView, INVALID, INVALID);
//    }

    /**
     * Load mms bitmap.
     * 
     * @param ctx
     *            the ctx
     * @param partId
     *            the part id
     * @param imageView
     *            the image view
     * @param width
     *            the width
     * @param height
     *            the height
     */
//    public void loadMmsBitmap(final Context ctx, final String url, final ImageView imageView,
//            final int width, final int height) {
//        loadBitmap(ctx, url, imageView, width, height, INVALID, null, 0);
//    }

    /**
     * Load a bitmap and return the result by sending message to the listener.
     *  
     * @param ctx
     * @param url
     * @param imageView
     * @param listener
     * @param msgCode
     */
//    public void loadBitmap(final Context ctx, final String url, final ImageView imageView,
//    					   Handler listener, int msgCode) {
//    	loadBitmap(ctx, url, imageView, INVALID, INVALID, INVALID, listener, msgCode);
//    }

    /**
     * Load bitmap.
     * 
     * @param ctx
     *            the ctx
     * @param url
     *            the url
     * @param imageView
     *            the image view
     */
//    public void loadBitmap(final Context ctx, final String url, final ImageView imageView) {
//        loadBitmap(ctx, url, imageView, null, 0);
//    }

//    public class BitmapTaskResult {
//    	public int reqId;
//    	public BitmapTask task;
//    }

    /**
     * Load bitmap.
     * 
     * @param ctx
     *            the ctx
     * @param url
     *            the url
     * @param imageView
     *            the image view
     * @param width
     *            the width
     * @param height
     *            the height
     * @return
     */
    public boolean loadBitmap(final Context ctx, final String url,
    					   final int width, final int height, final int minDimension,
    					   Handler listener, int msgCode) {
       	BitmapTask task = queueJob(ctx, url, width, height, minDimension, listener, msgCode);
       	if (task != null) {
       		return true;
       	}
        
        return false;
    }

    /**
     * Open and get input stream.
     * 
     * @param ctx
     *            the ctx
     * @param url
     *            the url
     * @return the input stream
     * @throws MalformedURLException
     *             the malformed url exception
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private InputStream openAndGetInputStream(final Context ctx, final String url)
            throws MalformedURLException, IOException {
        if (isMMS(url)) {
            Uri uri = Uri.parse(url);
            return ctx.getContentResolver().openInputStream(uri);
        }
        else {
        	// if it's not from MMS storage, assumed it's from the network
            return (InputStream)new URL(url).getContent();
        }
    }

    /**
     * Download bitmap.
     * 
     * @param ctx
     *            the ctx
     * @param url
     *            the url
     * @param width
     *            the width
     * @param height
     *            the height
     * @return the bitmap
     */
    private BitmapEntry downloadBitmap(final Context ctx, final String url, final int width, final int height, int flag) {
        // stream
        InputStream stream = null;
        BitmapEntry entry = null;
        try {
            stream = openAndGetInputStream(ctx, url);

        	BitmapFactory.Options opts = new BitmapFactory.Options();
        	//    opts.inSampleSize = 2;		// we don't reduce its size because we don't even know if it might be too size
        	Bitmap bitmap = BitmapFactory.decodeStream(stream, null, opts);
        	if (null != bitmap) {
    			int bw = bitmap.getWidth();
    			int bh = bitmap.getHeight();
//        		Logger.debug("======> Image size" + bw + "x" + bh + " <== " + url);
        		
        		// for valid height/width, resize the bitmap
        		if (height != INVALID && width != INVALID) {
        			int sw = width;
        			int sh = height;
        			
        			// now we rescale the image
        			boolean rescale = true;
        			if (checkFlag(flag, SCALE_NO_ENLARGE)) {
        				// no enlarge
        				if (bw <= width && bh <= height) {
        					// no rescale
        					rescale = false;
        				}
        			}
        			
        			if (rescale) {
        				if (checkFlag(flag, SCALE_MAX_XY)) {
        					float bratio = (float)bw / (float)bh;
        					float ratio = (float)width / (float)height;
        					
        					if (bratio >= ratio) {
        						// use width
        						sh = (int)(((float)sw / bratio) + .5f);
        					}
        					else {
        						// use height
        						sw = (int)(((float)sh * bratio) + .5f);
        					}
        				}
        				else {
        					// SCALE_MIN_XY is not supported
        				}
        			}
        			
        			if (rescale) {
//        				Logger.debug("======> Rescale image from " + bw + "x" + bh + " to " + sw + "x" + sh);
        				bitmap = Bitmap.createScaledBitmap(bitmap, sw, sh, true);
        			}
        		}
        		entry = new BitmapEntry();
        		entry.url = url;
        		entry.bitmap = bitmap;
        		entry.result = OK;
        		putToCache(ctx, url, width, height, entry);
        		
        		if (Logger.IS_DEBUG_ENABLED) {
        			Logger.debug(" url=" + url + " downlaod and decoded succeeded");
        		}        		
        	}
        	else {
        		// we don't want it to be downloaded again
        		if (Logger.IS_DEBUG_ENABLED) {
        			Logger.debug(" url=" + url + " BitmapFactory.decodeStream returned null");
        		}
        		entry = new BitmapEntry();
        		entry.url = url;
        		entry.bitmap = null;
        		entry.result = NOT_FOUND;
        		putToCache(ctx, url, width, height, entry);
        	}        	
        }
        catch (MalformedURLException e) {
    		// we don't want to download it again until next session
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(getClass(), " MalformedURLException url="+ url);
        	}
        	entry = new BitmapEntry();
        	entry.url = url;
            entry.bitmap = null;
            entry.result = NOT_FOUND;            
            putToCache(ctx, url, INVALID, INVALID, entry);
//            e.printStackTrace();
        }
        catch (FileNotFoundException e) {
        	// we don't want to download it again until next session
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(getClass(), " FileNotFoundExeption url="+ url);
        	}
        	entry = new BitmapEntry();
        	entry.url = url;
            entry.bitmap = null;
            entry.result = NOT_FOUND;
            putToCache(ctx, url, INVALID, INVALID, entry);
//            e.printStackTrace();
        }
        catch (IOException e) {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(getClass(), " IOExeption url="+ url);
        	}
        	Logger.error(e);
        }
        catch (Throwable t) {
        	// other exception
        	Logger.error(getClass(), "error loading url " + url, t);
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException e) {
                }
            }
        }
        return entry;
    }

    /**
     * Gets the cache key.
     * 
     * @param url
     *            the url
     * @param width
     *            the width
     * @param height
     *            the height
     * @return the cache key
     */
    private String getCacheKey(final String url, final int width, final int height) {
        return MessageFormat.format("{0}?w={1}&h={2}", url, width, height);
    }

//    public int getBitmap(final Context ctx, final String url, final ImageView imageView) {
//    	return getBitmap(ctx, url, imageView, INVALID, INVALID);    	
//    }
    
    /**
     * Get the Bitmap if it's already in the cache.
     * 
     * @param ctx
     * @param url
     * @param imageView
     * @param width
     * @param height
     * @paran minDimension		Minimum dimension of the image to be used (either width or height needs to be equal or larger than this value)
     * @return	OK if image is in the cache and size is longer than the minDimension, NOT_USED if image size doesn't meet the requirement, FILE_NOT_FOUND if in cache but file is not on server, ERROR if not in cache
     */
//    public int getBitmap(final Context ctx, final String url, final ImageView imageView, int width, int height, int minDimension,
//    		Handler listener, int msgCode) {
//        mImageViews.put(imageView, url);
//        BitmapEntry bitmap = getFromCache(ctx, url, width, height);
//
//        // check in UI thread, so no concurrency issues
//        if (bitmap != null) {
//        	if (Logger.IS_DEBUG_ENABLED) {
//        		Logger.debug(getClass(), "Found in cache. Key=" + getCacheKey(url, width, height));
//        	}
//        	
//        	// return the result
//            int result = bitmap.result;
//            if (bitmap.bitmap != null) {
//            	// bitmap can be null, e.g. if result is 404 NOT FOUND
//            	if (setImageBitmap(imageView, bitmap.bitmap, minDimension) == false) {
//            		result = NOT_USED;
//            	}
//            	sendListener(listener, msgCode, bitmap.result, imageView, bitmap);
//            }
//            return result;
//        }
//        else {
//        	if (Logger.IS_DEBUG_ENABLED) {
//        		Logger.debug(getClass(), "Not Found in cache. Key=" + getCacheKey(url, width, height));
//        	}
//            return ERROR;
//        }
//    }

    /**
     * Get Bitmap from cache.
     * 
     * It's asynchronous call, result returend in callback.
     *
     * @param ctx
     * @param url
     * @param width
     * @param height
     * @param minDimension
     * @param listener
     * @param msgCode
     * @return
     */
    public BitmapEntry getBitmap(final Context ctx, final String url, int width, int height)
    {
    	BitmapEntry bitmap = getFromCache(ctx, url, width, height, false);

    	// check in UI thread, so no concurrency issues
    	if (bitmap != null) {
    		if (Logger.IS_DEBUG_ENABLED) {
    			Logger.debug(getClass(), "Found in cache. Key=" + getCacheKey(url, width, height));
    		}
    	}
    	else {
    		if (Logger.IS_DEBUG_ENABLED) {
    			Logger.debug(getClass(), "Not Found in cache. Key=" + getCacheKey(url, width, height));
    		}
    	}
		return bitmap;
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

    /**
     * Covert DIP to Pixel.
     * 
     * @param ctx
     * @param idDimen
     * @return
     */
    public static final int dipToPixel(final Context ctx, final int idDimen) {
    	int pixel = 0;
    	
    	Resources res = ctx.getResources();
    	if (res != null) {
    		float dp = res.getDimension(idDimen);
    		// convert dp to actual screen size
    		pixel = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    	}
    	return pixel;
    }

	/**
	 * Get the bitmap's dimensions.
	 * @return A rectangle of the bitmap's dimensions, or null if unable to open and/or decode it
	 */
	public Rect getBitmapSize(final Context ctx, final String url) {
		Rect rect = null;
		InputStream stream = null;
		try {
			stream = openAndGetInputStream(ctx, url);
			final BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(stream, null, opts);
			final int width = opts.outWidth;
			final int height = opts.outHeight;
			if (width != -1 && height != -1) {
				rect = new Rect(0, 0, width, height);
			}
		}
		catch (Throwable t) {
			Logger.error(getClass(), "getBitmapSize: error decoding url " + url, t);
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
		return rect;
	}

	public boolean cancelRequest(Handler handler, String url) {
		boolean ret = false;
		
		// check if the link is being fetched or in the queue
		Iterator<Runnable> it = mQueue.iterator();
		while (it.hasNext()) {
			BitmapTask task = (BitmapTask)it.next();
			if (task.url.equals(url) && task.canceled == false) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(this.getClass(), "####### task of url=" + url + " found");
				}
				task.cancel(handler);
				ret = true;
			}
		}
		return ret;
	}
	
	/**
	 * Get the video thumbnail's dimensions.
	 * @return A rectangle of the thumbnail's dimensions, or null if unable to get it
	 */
	public Rect getVideoThumbnailSize(final Context ctx, final String url) {
		Rect rect = null;
		Bitmap bitmap = null;
		final Uri uri = Uri.parse(url);
		final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			if (hasCaptureFrame) {
				retriever.setDataSource(ctx, uri);
				bitmap = retriever.captureFrame();
			}
			else {
				bitmap = getVideoThumbnailFrame(ctx, retriever, uri);
			}
		}
		catch (Throwable t) {
			if (t instanceof NoSuchMethodError || t instanceof UnsupportedOperationException ) {
				hasCaptureFrame = false;
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "getVideoThumbnailSize: device doesn't have captureFrame, trying getFrameAtTime");
				}
			}
			else {
				Logger.error(false, getClass(), "error getting thumbnail for " + uri + ", trying getFrameAtTime", t);
			}
			bitmap = getVideoThumbnailFrame(ctx, retriever, uri);
		}
		finally {
			try {
				retriever.release();
			}
			catch (Throwable t) {
			}
		}

		if (bitmap != null) {
			rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
			bitmap.recycle();
		}

		return rect;
	}

	private Bitmap getVideoThumbnailFrame(Context context, MediaMetadataRetriever retriever, Uri uri) {
		Bitmap bitmap = null;
		try {
			retriever.setDataSource(context, uri);
			bitmap = retriever.getFrameAtTime();
		}
		catch (Throwable t) {
			Logger.error(getClass(), "error getting thumbnail for " + uri, t);
		}
		return bitmap;
	}
}
