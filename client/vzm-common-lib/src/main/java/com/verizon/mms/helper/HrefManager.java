package com.verizon.mms.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.strumsoft.android.commons.logger.Logger;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Html.TagHandler;
import android.text.Spanned;

/**
 * @Since Jul 7, 2012
 */
public enum HrefManager {
	/** The INSTANCE. */
	INSTANCE;

	private static final boolean USE_FAVICON		= false;
	
	private static final int CACHE_SIZE				= 200;					// memory cache size
	private static final int BLOCKING_QUEUE_SIZE	= 100;					// might need a long					
	private static final int POOL_SIZE				= 2;
	
	private static final int DB_SIZE				= 50;					// database max number of records
	private static final long DB_KEEP_TIME			= (15*24*60*60*1000);	// 15 days
   
	//Using delayed post as in some cases when UI is not populated and handler was called. i.g server connection error.
	private static final int MESSAGE_DELAY = 5000;
	
	private static final int MAX_CONNECT_TIMEOUT = 20000;
	private static final int MAX_SOCKET_TIMEOUT = 20000;	
	
	//    public final static int                 MESSAGE_URL_TASK      = 100;
	//    final static String                     PROPERTY_URL          = "og:url";

	private static final String TAG_META = "meta";
	private static final String TAG_LINK = "link";
	private static final String TAG_TITLE = "title";
	
	private static final String ATTR_PROPERTY = "property";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_CONTENT = "content";
	private static final String ATTR_ITEMPROP = "itemprop";
	private static final String ATTR_HREF = "href";
	private static final String ATTR_REL = "rel";
	
	private static final int TYPE_TITLE = 0;
	private static final int TYPE_DESCRIPTION = 1;
	private static final int TYPE_IMAGE = 2;

	public static final int CODE_NOT_TO_CACHE = -1;			// link detail should not be cached because it might be temporary error
	public static final int CODE_CACHE_WITH_EXPIRY = -2;	// link detail should be refreshed later
	
	/**
	 * List of tags and properties to extract the information.
	 * 
	 * They are in the order of preference.
	 * So if the tag in the higher order is found in the HTML parsing, the new information from that tag will replace the previous extracted information.
	 */
	private static final HrefEntity[] ENTITY_LIST = {
		// title
		new HrefEntity(TAG_META, ATTR_PROPERTY, "og:title", ATTR_CONTENT, TYPE_TITLE),	// <meta property="og:title" content="a title"/>
		new HrefEntity(TAG_META, ATTR_NAME, "title", ATTR_CONTENT, TYPE_TITLE),			// <meta name="title" content="a title"/>
		new HrefEntity(TAG_META, ATTR_ITEMPROP, "name", ATTR_CONTENT, TYPE_TITLE),		// <meta itemprop="name" content="a title">
		new HrefEntity(TAG_TITLE, null, null, null, TYPE_TITLE),						// <title>a title</title>
		
		// description
		new HrefEntity(TAG_META, ATTR_PROPERTY, "og:description", ATTR_CONTENT, TYPE_DESCRIPTION),	// <meta property="og:description" content="a description"/>
		new HrefEntity(TAG_META, ATTR_NAME, "description", ATTR_CONTENT, TYPE_DESCRIPTION),			// <meta name="description" content="a description"/>
		new HrefEntity(TAG_META, ATTR_ITEMPROP, "description", ATTR_CONTENT, TYPE_DESCRIPTION),		// <meta itemprop="description" content="a description">
		
		// image
		new HrefEntity(TAG_META, ATTR_PROPERTY, "og:image", ATTR_CONTENT, TYPE_IMAGE),				// <meta property="og:image" content="an image"/>
		new HrefEntity(TAG_LINK, ATTR_REL, "image_src", ATTR_HREF, TYPE_IMAGE),						// <link rel="image_src" href="an image"/>
		new HrefEntity(TAG_META, ATTR_ITEMPROP, "image", ATTR_CONTENT, TYPE_IMAGE),					// <meta itemprop="image" content="an image"/>
		new HrefEntity(TAG_LINK, ATTR_REL, "apple-touch-icon-precomposed", ATTR_HREF, TYPE_IMAGE),	// <link rel="apple-touch-icon-precomposed" href="an image"/>
		new HrefEntity(TAG_LINK, ATTR_REL, "apple-touch-icon", ATTR_HREF, TYPE_IMAGE),				// <link rel="apple-touch-icon" href="an image"/>
		new HrefEntity(TAG_LINK, ATTR_REL, "icon", ATTR_HREF, TYPE_IMAGE),							// <link rel="icon" href="an image"/>
		new HrefEntity(TAG_LINK, ATTR_REL, "shortcut icon", ATTR_HREF, TYPE_IMAGE)					// <link rel="shortcut icon" href="an image"/>
	};

	/** 1st level database */
	private HrefDB					mCacheDB = null;

	/** 2nd level cache */
	private final SoftCache<String, LinkDetail> mCache;

	private final BlockingQueue<Runnable>   mQueue;

	/** The pool. */
	private final ThreadPoolExecutor        mPool;

	/**
	 * Create and open cache database.
	 * 
	 * @param context
	 * @return
	 */
	private boolean openCacheDB(Context context) {
		if (mCacheDB == null) {
			mCacheDB = new HrefDB(context.getApplicationContext());
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
	 *  Constructor 
	 */
	HrefManager() {
		mCache = new SoftCache<String, LinkDetail>(CACHE_SIZE);
		mQueue = new ArrayBlockingQueue<Runnable>(BLOCKING_QUEUE_SIZE);
		mPool = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 20, TimeUnit.SECONDS, mQueue);
	}

	/**
	 * Get LinkDetail from cache.
	 * 
	 * Returns null if not found, from cache only will not download the url.
	 * 
	 * @param context
	 * @param url
	 * @return
	 */
    public LinkDetail getFromCache(Context context, String url) {
    	url = LinkDetail.fixUrl(url);
    	
        // first check memory cache
    	LinkDetail detail = mCache.getFromCache(url);

		if (detail != null) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "LinkDetail " + url + " found in memory cache");
			}
			return detail;
		}

    	synchronized(this) {
			// if it's not found in memory, try persistent cache
    		if (openCacheDB(context)) {
    			try {
    				detail = mCacheDB.getLinkDetail(url);
    				if (detail != null) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "LinkDetail " + url + " found in db cache");
						}

    					if (detail.responseCode == CODE_CACHE_WITH_EXPIRY) {
    						// this should be refreshed if it's not found in memory cache
        					if (Logger.IS_DEBUG_ENABLED) {
        						Logger.debug(getClass(), "LinkDetail " + url + " error last time, try to reload it");
        					}
    						detail = null;
    					}
    					else {
    						// add to memory cache
    						mCache.putToCache(url, detail);
    					}
    				}    				
    			}
    			catch (Exception e) {
    				if (Logger.IS_WARNING_ENABLED) {
    					Logger.warn(getClass(), "Exception in getting linkdetail from cache DB", e);
    				}
    				detail = null;
    			}
    			closeCacheDB();
    		}
    	}
		return detail;
    }

    /**
     * Store LinkDetail and its Url to memory cache and persistent database.
     * 
     * @param context
     * @param url
     * @param detail
     */
    private void putToCache(Context context, String url, LinkDetail detail) {
    	// we first add it to memory cache
    	mCache.putToCache(url, detail);
    	
    	if (detail.responseCode != CODE_CACHE_WITH_EXPIRY && detail.responseCode != CODE_NOT_TO_CACHE) {
    		// add to the persistent database
    		synchronized(this) {
    			if (openCacheDB(context)) {
    				try {
    					if (Logger.IS_DEBUG_ENABLED) {
    						Logger.debug(getClass(), "Putting LinkDetail to database: " + url);
    					}

    					// insert a new record (or update the record if it exists
    					mCacheDB.insertLinkDetail(url, detail.contentType, detail.title, detail.description, detail.linkImage,
    							detail.responseCode, detail.error, System.currentTimeMillis());
    				}
    				catch (Exception e) {
    					if (Logger.IS_WARNING_ENABLED) {
    						Logger.warn(getClass(), "Failed to add " + url + " to cache DB", e);
    					}
    				}
    				closeCacheDB();
    			}
    		}
    	}
    }
	
	/**
	 * Get the LinkDetail of a link if it's already been cached.
	 * 
	 * It will not download.
	 * 
	 * @param ctx	Context
	 * @param link	Link to get
	 * @return		LinkDetail if it's in cache, null if otherwise.
	 */
	public LinkDetail getLink(final Context ctx, String url) {
		final String link = LinkDetail.fixUrl(url);
				
		// check in cache
		LinkDetail linkDetail = null;
		linkDetail = getFromCache(ctx, link);
		if (null != linkDetail) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "#=> link=" + link + " found in cache! thread=" + Thread.currentThread().getName());
				Logger.debug(getClass(), "#=> code=" + linkDetail.responseCode + " error=" + linkDetail.error);
			}
		}
		return linkDetail;                	
	}

	/**
	 * Load the link detials from cache, if not found download to get it.
	 * Return result asychronously.
	 * 
	 * @param ctx
	 * @param link
	 * @param handler
	 * @param msgCode
	 */
	public void loadLink(final Context ctx, String url, final Handler handler, final int msgCode) {
		loadLink(ctx, url, handler, msgCode, false);
	}

	/**
	 * Load the link detials from cache, if not found download to get it.
	 * Return result asychronously.
	 * 	 * If refetch is true, it will skip checking the database and download it regardless.
	 * 
	 * @param ctx
	 * @param link
	 * @param handler
	 * @param msgCode
	 * @param refetch
	 */
	public void loadLink(final Context ctx, String url, final Handler handler, final int msgCode,
			final boolean refetch) {
		final String link = LinkDetail.fixUrl(url);
		
		if (refetch == false) {
			// check in cache
			LinkDetail linkDetail = null;
			linkDetail = getFromCache(ctx, link);
			if (null != linkDetail) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "#=> link=" + link + " found in cache! thread=" + Thread.currentThread().getName());
					Logger.debug(getClass(), "#=> code=" + linkDetail.responseCode + " error=" + linkDetail.error);
				}
				sendMessage(handler, linkDetail, msgCode);
				return;
			}
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "#=> not cached, link=" + link);
			}
		}
		else {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "#=> refetch link=" + link + " thread=" + Thread.currentThread().getName());
			}
		}
		
		boolean newTask = true;
		
		// check if the link is being fetched or in the queue
		Iterator<Runnable> it = mQueue.iterator();
		while (it.hasNext()) {
			FetchTask task = (FetchTask)it.next();
			
			if (task.link.equals(link)) {				
				newTask = !task.addListener(handler, msgCode);
				
				if (newTask == false) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "#=> found task in queue link=" + link);
					}
					break;
				}
			}
		}
		
		// schedule a FetchTask
		if (newTask) {
			try {
				FetchTask task = new FetchTask(ctx, link, refetch, handler, msgCode);
				mPool.execute((Runnable)task);
			}
			catch (RejectedExecutionException reh) {
				if (Logger.IS_WARNING_ENABLED) {
					Logger.warn(getClass(), "#==> Queue=" + mQueue.size() + " threads=" + mPool.getActiveCount() + " err=" + reh.getMessage());
				}
			}
		}
	}

	/**
	 * Send message to the caller.
	 * 
	 * @param handler
	 * @param linkDetail
	 * @param msgCode
	 */
	private void sendMessage(final Handler handler, final LinkDetail linkDetail, int msgCode) {
		if (handler != null) {
			Message msg = handler.obtainMessage();
			msg.what = msgCode;	//MESSAGE_URL_TASK;
			msg.obj = linkDetail;
			handler.sendMessageDelayed(msg, MESSAGE_DELAY);
		}
	}

	/**
	 * Parse response from Http Head or Get request.
	 * Return true if to continue processing, false if no more processing is needed
	 * 
	 * @param linkDetail
	 * @param response
	 * @param request
	 * @return
	 */
	private boolean parseResponse(LinkDetail linkDetail, HttpResponse response, int request) {
		String link = linkDetail.link;
		int code;
		String reasonPhrase = null;
		StatusLine status = null;
		
		// get the response code
		code = response.getStatusLine().getStatusCode();
		linkDetail.responseCode = code;
		status = response.getStatusLine();
		if (status != null) {
			reasonPhrase = status.getReasonPhrase();
		}

		if (reasonPhrase == null) {
			reasonPhrase = "Unknown Error, code " + code;
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "#=> link=" + link + " code=" + code + " reason=" + reasonPhrase);
		}

		if (code != 200) {	// not 200 OK
			if (code == 301 && request == 0) {
				// need redirection for HEAD request
				if (Logger.IS_WARNING_ENABLED) {
					Logger.warn("#=> code=" + code + " link=" + link + " HEAD need redirection");
				}
				return true;
			}
			else {
				if (Logger.IS_WARNING_ENABLED) {
					Logger.warn("#=> code= " + code + " link=" + link + " " +
							(request == 0? "HEAD" : "GET") +
							" failed!");
				}
				linkDetail.error = reasonPhrase;
				return false;
			}
		}

		// get content-type before downloading the whole body
		Header header = response.getLastHeader("content-type");
		if (null == header) {
			Logger.warn(getClass(), "#=> link=" + link + " failed! because there are no headers!");
			linkDetail.error = "Invalid headers";
			return false;
		}
		
		String contentType = header.getValue();
		linkDetail.contentType = contentType;
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "#=> content-type=" + contentType);
		}

		if (contentType.startsWith("image/")) {
			// if it's an image, no content to parse, set the link and return
			linkDetail.linkImage = link;
			return false;	// no more processing needed
		} // image
		
		if (contentType.startsWith("text/html") == false) {
			// it is not image or html, we do not support preview
			// do not set the error string so it won't show an error on the preview
//			linkDetail.error = "Preview not supported";
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "#==> link=" + link + " type=" + contentType + " :Preview Not Supported");
			}
			return false;
		}
		
		return true;
	}
	
	/**
	 * Set maximum timeout values.
	 * 
	 * @param httpClient
	 */
	private void setTimeoutValues(DefaultHttpClient httpClient) {
		HttpParams params = httpClient.getParams();
		int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
		int soTimeout = HttpConnectionParams.getSoTimeout(params);
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(getClass(), "#=> Default connection timeout=" + connTimeout + " socket timeout=" + soTimeout);
//		}
		if (connTimeout <= 0 || connTimeout > MAX_CONNECT_TIMEOUT) {
			HttpConnectionParams.setConnectionTimeout(params, MAX_CONNECT_TIMEOUT);
		}
		if (connTimeout <= 0 || soTimeout > MAX_SOCKET_TIMEOUT) {
			HttpConnectionParams.setSoTimeout(params, MAX_SOCKET_TIMEOUT); 
		}
	}

	/**
	 * Download a link.
	 * 
	 * @param link
	 * @return
	 */
	private LinkDetail fetch(String link) {
		// must use DefaultHttpClient because AndroidHttpClient does not do redirection (301) automatically
		DefaultHttpClient httpClient = null;
//		AndroidHttpClient httpClient = null;
		
		LinkDetail linkDetail = new LinkDetail();
		linkDetail.link = link;

		try {			
			// Do a HEAD request first to make sure it's image or html			
			HttpHead head = new HttpHead(link);			
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "#=> HEAD fetching link=" + link);
			}
			
			httpClient = new DefaultHttpClient();
//			httpClient = AndroidHttpClient.newInstance("Android");			
			setTimeoutValues(httpClient);
			
			HttpResponse response = httpClient.execute(head);
//			httpClient.close();
			httpClient = null;

			if (parseResponse(linkDetail, response, 0) == false) {
				return linkDetail;
			}
			
			// continue with get request to get the body for html
			HttpGet get = new HttpGet(link);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "#=> GET fetching link=" + link);
			}
			
			httpClient = new DefaultHttpClient();
//			httpClient = AndroidHttpClient.newInstance("Android");
			setTimeoutValues(httpClient);

			response = httpClient.execute(get);
//			httpClient.close();
			httpClient = null;
			
			if (parseResponse(linkDetail, response, 1) == false) {
				return linkDetail;
			}

			// we have the html content, we need get the correct charset
			// get charset from content-type
			String contentType = linkDetail.contentType;
			int charsetIndex = contentType.indexOf("charset=");
			String charset = null;
			if (charsetIndex >= 0) {
				charset = contentType.substring(charsetIndex + (new String("charset=")).length());
				int end = charset.indexOf(';');
				if (end > 0) {
					charset = charset.substring(0, end-1);
				}

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), " link=" + link + " charset=" + charset);
				}
				
				if (charset.equalsIgnoreCase("utf-8")) {
					charset = null;
				}
			}

			if (contentType.startsWith("text/html")) {	// should be, already checked
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					// Read entity
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					BufferedOutputStream bos = new BufferedOutputStream(baos);
					byte[] buffer = new byte[1024];
					BufferedInputStream bis = new BufferedInputStream(entity.getContent());
					int read;
					int count = 0;
					do {
						read = bis.read(buffer, 0, 1024);
						if (read > 0) {
							count += read;
							bos.write(buffer, 0, read);
						}
					} while (read >= 0);
					bis.close();
					bos.close();

					String data;
					if (charset == null) {
						data = baos.toString();
					}
					else {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "#=> converting using charset=" + charset);
						}
						try {
							data = baos.toString(charset);
						}
						catch (UnsupportedEncodingException uee) {
							// do a regular conversion then
							data = baos.toString();
						}
					}
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "#=> link=" + link + " content-length=" + entity.getContentLength() + " byte-count=" + count);
					}
					
//					Logger.warn(getClass(), data);	// dump the received data
					parseHtml(linkDetail, link, data);
				}
			} // html

		}
		//        catch (UnknownHostException e) {
		//            log.error(" link={} error={}", link, e);
		//            linkDetail.responseCode = -1;
		//            linkDetail.error = e.toString();        	
		//        }
		catch (Exception e) {
//			if (httpClient != null) {
//				httpClient.close();
//			}
			
			if (Logger.IS_WARNING_ENABLED) {
				Logger.warn(getClass(), "#=> link=" + link + " error=", e);
			}
			linkDetail.responseCode = CODE_CACHE_WITH_EXPIRY;
			linkDetail.error = e.toString();
			if (linkDetail.error == null) {
				// if there is no error string, use the exception class name
				linkDetail.error = e.getClass().getSimpleName();
			}
		}

		return linkDetail;
	}
	
	class HrefContentHandler implements ContentHandler {
		private ContentHandler mContentHandler;
		private HrefTagHandler mTagHandler;
		private boolean done = false;
		
		public HrefContentHandler(ContentHandler contentHandler, HrefTagHandler tagHandler) {
			mContentHandler = contentHandler;
			mTagHandler = tagHandler;
		}
		
		@Override
		public void characters(char[] ch, int start,
				int length) throws SAXException {
			// ignore any character unless it's <title>
			mContentHandler.characters(ch,  start, length);
			mTagHandler.tagCharacters(ch, start, length);
		}

		@Override
		public void endDocument() throws SAXException {
			mContentHandler.endDocument();
		}

		@Override
		public void endElement(String uri,
				String localName, String qName)
						throws SAXException {
			mContentHandler.endElement(uri, localName, qName);
		}

		@Override
		public void endPrefixMapping(String prefix)
				throws SAXException {
			mContentHandler.endPrefixMapping(prefix);
		}

		@Override
		public void ignorableWhitespace(char[] ch,
				int start, int length) throws SAXException {
			mContentHandler.ignorableWhitespace(ch, start, length);
		}

		@Override
		public void processingInstruction(String target,
				String data) throws SAXException {
			mContentHandler.processingInstruction(target, data);
		}

		@Override
		public void setDocumentLocator(Locator locator) {
			mContentHandler.setDocumentLocator(locator);
		}

		@Override
		public void skippedEntity(String name)
				throws SAXException {
			mContentHandler.skippedEntity(name);
		}

		@Override
		public void startDocument() throws SAXException {
			mContentHandler.startDocument();
		}

		@Override
		public void startElement(String uri,
				String localName, String qName,
				Attributes atts) throws SAXException {
			mContentHandler.startElement(uri, localName, qName, atts);
			mTagHandler.tagAttributes(localName, atts);
		}

		@Override
		public void startPrefixMapping(String prefix,
				String uri) throws SAXException {
			mContentHandler.startPrefixMapping(prefix, uri);
		}
		
		public void stop() {
			done = true;
		}
	};

	
	class HrefTagHandler implements TagHandler {
		boolean headDone = false;						// if </head> has been passed, we ignore everything after
		boolean detailDone = false;						// if we have all title, description and image
		
		ContentHandler originalContentHandler = null;
		HrefContentHandler customContentHandler = null;
		LinkDetail linkDetail = null;
		
		// order of the information extracted
		int indexTitle = Integer.MAX_VALUE;
		int indexDescription = Integer.MAX_VALUE;
		int indexImage = Integer.MAX_VALUE;

		int indexEntity = Integer.MAX_VALUE;
		HrefEntity currentEntity = null;
		StringBuffer currentTagText = null;
		
		boolean ignoreContent = false;
		
		public HrefTagHandler(LinkDetail linkDetail) {
			this.linkDetail = linkDetail;
		}
		
		@Override
		public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
			//		log.info("TagHandler: {}", tag);

			if (tag.equalsIgnoreCase("html")) {
//				Logger.debug(getClass(), "TagHandler: <" + (opening? "" : "/") + tag + ">");

				if (opening) {
					ContentHandler contentHandler = xmlReader.getContentHandler();
					if (contentHandler == null) {
						Logger.debug(getClass(), "   ContentHandler is null");
						originalContentHandler = null;
					}
					else {
						originalContentHandler = contentHandler;

						// replace the ContentHandler
						customContentHandler = new HrefContentHandler(originalContentHandler, this);
						xmlReader.setContentHandler(customContentHandler);
					}
				}
				else {
					// restore the contenthandler
					if (originalContentHandler != null) {
						xmlReader.setContentHandler(originalContentHandler);
					}
				}
			}
			else if (tag.equalsIgnoreCase("head") && opening == false) {
				customContentHandler.stop();
				headDone = true;
			}
			else if (headDone == false) {
				if (opening) {
					tagStart(tag);
				}
				else {
					tagEnd(tag);
				}
			}
		}
		
		public void tagAttributes(String tag, Attributes atts) {
			if (headDone == false && detailDone == false && ignoreContent == false) {				
//				int len = atts.getLength();
//				for (int i=0; i<len; i++) {
//					String localName= atts.getLocalName(i);
//					String qName = atts.getQName(i);
//					String value = atts.getValue(i);
//					log.info("    tag = {}, localName={}, qName={}, value={}", new String[]{ tag, localName, qName, value });
//				}
				
				int index = 0;
				for (HrefEntity entity : ENTITY_LIST) {
					if (currentEntity == null) {
						// we're not inside any enclosing tag
						if (entity.attrPropertyValue != null && entity.tag.equalsIgnoreCase(tag)) {
//							log.info("---> check entity: tag={} name={} value={}",
//									new String[]{ entity.tag, entity.attrPropertyName, entity.attrPropertyValue});

							// this entity uses attributes in the tag
							String propertyValue = atts.getValue(entity.attrPropertyName);
							String value = atts.getValue(entity.attrValueName);

							if (propertyValue != null) {
//								log.info("Property Name {} found: {}", entity.attrPropertyName, propertyValue);
								if (propertyValue.equalsIgnoreCase(entity.attrPropertyValue) && value != null) {
//									log.info("Property Value {} found: {}", entity.attrValueName, value);

									// we have both and we now check what type it belongs to
									if (entity.type == TYPE_TITLE && index < indexTitle) {
										linkDetail.title = value; 
										indexTitle = index;
										break;
									}
									else if (entity.type == TYPE_DESCRIPTION && index < indexDescription) {
										linkDetail.description = value;
										indexDescription = index;
										break;
									}
									else if (entity.type == TYPE_IMAGE && index < indexImage) {
										linkDetail.linkImage = value;
										indexImage = index;
										break;
									}
								}								
							}
						}
						
						index++;
					}
				}
				
//				checkDone();
			}
		}

		public void tagStart(String tag) {
//			log.info("TagHandler: <{}>", tag);
			if (currentEntity == null) {
				if (tag.equalsIgnoreCase("script")) {
					// we ignore anything after until the end tag
					currentEntity = new HrefEntity("script", null, null, null, 0);
					ignoreContent = true;
				}
				else {
					// we're not until any matching entity yet, check it
					int index = 0;
					for (HrefEntity entity : ENTITY_LIST) {
						if (entity.attrPropertyValue == null) {
							if (tag.equalsIgnoreCase(entity.tag)) {
								// found it, we need everything between the start and close tag
								currentEntity = entity;
								currentTagText = new StringBuffer();
								indexEntity = index;
							}
						}
						index++;
					}
				}
			}
		}
		
		public void tagCharacters(char[] ch, int start, int len) {
			if (ignoreContent == false && currentTagText != null) {
				currentTagText.append(ch, start, len);
			}
		}
		
		public void tagEnd(String tag) {
//			log.info("TagHandler: </{}>", tag);
			if (currentEntity != null) {
				if (tag.equalsIgnoreCase(currentEntity.tag)) {
					if (ignoreContent == true) {
						// restore all processing
						ignoreContent = false;
					}
					else {
						// we now close the tag
						if (currentTagText != null) {
							String text = currentTagText.toString();
//							log.info("Tag text: {}", text);

							// we have both and we now check what type it belongs to
							if (currentEntity.type == TYPE_TITLE && indexEntity < indexTitle) {
								linkDetail.title = text;
								indexTitle = indexEntity;
							}
							else if (currentEntity.type == TYPE_DESCRIPTION && indexEntity < indexDescription) {
								linkDetail.description = text;
								indexDescription = indexEntity;
							}
							else if (currentEntity.type == TYPE_IMAGE && indexEntity < indexImage) {
								linkDetail.linkImage = text;
								indexImage = indexEntity;
							}
							//						checkDone();						
						}
					}
					currentEntity = null;
					currentTagText = null;
				}
			}
		}

//		private void checkDone() {
//			if (linkDetail.title != null && linkDetail.description != null && linkDetail.linkImage != null) {
//				// we have them all
//				log.info("TagHandler: we have all details needed");
//				detailDone = true;
//			}
//		}
	}

	/**
	 * Parse the HTML to extract the title, description and image.
	 * 
	 * @param linkDetail
	 * @param link
	 * @param data
	 */
	protected void parseHtml(LinkDetail linkDetail, String link, String data) {
		ImageGetter imageGetter = new ImageGetter() {
			@Override
			public Drawable getDrawable(String source) {
				// if we want to get the first image, do it here
				return null;
			}			
		};
		
		TagHandler tagHandler = new HrefTagHandler(linkDetail);
		
		@SuppressWarnings("unused")
		Spanned spanned = Html.fromHtml(data, imageGetter, tagHandler);	// spanned is dummy
		
		// complete the url
		// using the favicon.ico
		if (USE_FAVICON) {
			if (linkDetail.linkImage == null) {
				Uri uri = Uri.parse(link);
				linkDetail.linkImage = uri.getScheme() + "://" + uri.getAuthority() + "/favicon.ico";                    	
			}
		}
		linkDetail.linkImage = LinkDetail.relativeToAbsoluteUrl(linkDetail.linkImage, link);

		// trim the strings
		linkDetail.title = trimString(linkDetail.title);
		linkDetail.description = trimString(linkDetail.description);

		// log.debug(" title={}", linkDetail.title);
		// log.debug(" description={}", linkDetail.description);
		// log.debug(" image={}", linkDetail.linkImage);
	}

	private String trimString(String org) {
		if (org != null) {
			org = org.trim();
		}
		return org;
	}
	
	private class FetchTask implements Runnable {
		private String link;
		private boolean refetch;
		private Context context;
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
		
		public FetchTask(Context context, String url, boolean refetch, Handler handler, int msgCode) {
			this.context = context;
			this.refetch = refetch;
			this.link = url;
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
				if (done == false) {
					Listener listener = new Listener(handler, msgCode);
					listeners.add(listener);
					ret = true;
				}
			}
			return ret;
		}
		
		@Override
		public void run() {
			LinkDetail linkDetail = null;

			if (refetch == false) {
				// Check in cache again, some other thread might already have populated it
				linkDetail = getFromCache(context, link);
			}

			if (null != linkDetail) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "#=> link=" + link + " found in cache! thread=" + Thread.currentThread().getName());
					Logger.debug(getClass(), "#=> code=" + linkDetail.responseCode + " error=" + linkDetail.error);
				}
			}
			else {
				// not in the cache, fetch from the url
				linkDetail = fetch(link);

				// add the data to cache
				// do not cache if responseCode = -1
				if (linkDetail.responseCode != CODE_NOT_TO_CACHE) {
					putToCache(context, link, linkDetail);
				}
			}

			synchronized(listeners) {
				done = true;
				for (Listener listener : listeners) {
					sendMessage(listener.handler, linkDetail, listener.msgCode);
				}
				listeners.clear();
			}
		}
	}
}