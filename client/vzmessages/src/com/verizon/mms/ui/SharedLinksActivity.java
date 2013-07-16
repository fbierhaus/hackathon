package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.Media;
import com.verizon.mms.MediaProvider;
import com.verizon.mms.MediaSyncService;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.helper.BitmapManager;
import com.verizon.mms.helper.BitmapManager.BitmapEntry;
import com.verizon.mms.helper.Cache;
import com.verizon.mms.helper.HrefManager;
import com.verizon.mms.helper.LinkDetail;
import com.verizon.mms.ui.widget.ScaledImageView;
import com.verizon.mms.util.UtilClass;

/**
 * 
 * 
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * @Since Apr 10, 2012
 */
public class SharedLinksActivity extends VZMActivity {
	public static final long INVALID = -1L;
	
    private static final int    MSG_URL_TASK = 1;
    private static final int    MSG_BITMAP = 2;
    private static final int    DETAIL_CACHE_SIZE = 30;
    private static final int    BITMAP_CACHE_SIZE = 30;
    private static final int    RE_QUERY = 1001;
    
    private static final boolean SHOW_PREVIEW_ERROR = false;
    
    private List<Link>			mItemList = null;
    private ListView            mLinkListView;
    private ListLinkAdapter     mLinkListAdapter;
    
	private SharedPreferences mPreferences;
	private boolean mEnableUrlPreview; 
	
	private Bitmap defaultPreviewBitmap = null;
	
	private ContentObserver observer;
	private boolean queryInProgress = false;
	private Handler handler;
	private long thread;
	
	private int mMinPreviewImage;
	
	private final OnSharedPreferenceChangeListener mPreferencesChangeListener =
			new OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			// check if weblink preview preference is changed
			if (AdvancePreferenceActivity.WEBLINK_PREVIEW.equals(key)) {
				// get the new setting
				mEnableUrlPreview = prefs.getBoolean(AdvancePreferenceActivity.WEBLINK_PREVIEW,
					AdvancePreferenceActivity.WEBLINK_PREVIEW_DEFAULT);

				// refresh the list
				// seems not necessary as on return to the Activity, all list items will be re-populated
				// mListView.invalidate();
			}
		}
	};	

	@SuppressWarnings("unchecked")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.locationlayout);
        View header = (RelativeLayout) findViewById(R.id.headerlayout);
        ((TextView) header.findViewById(R.id.locationtitletext)).setText(R.string.shared_link_title);
        ((ImageView) header.findViewById(R.id.titleicon)).setImageResource(R.drawable.ico_shared_links);
        
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferences.registerOnSharedPreferenceChangeListener(mPreferencesChangeListener);
		mEnableUrlPreview = mPreferences.getBoolean(AdvancePreferenceActivity.WEBLINK_PREVIEW,
			AdvancePreferenceActivity.WEBLINK_PREVIEW_DEFAULT);  

        mItemList = (List<Link>) getLastNonConfigurationInstance();
        if (mItemList != null) {
        	createList();
        }        
        
        mMinPreviewImage = (int)getResources().getDimension(R.dimen.linkPreviewMinImageSizeSharedLinks);
        
		thread = getIntent().getLongExtra("threadid", INVALID);

        // Handler
        handler = new Handler() {
            public void handleMessage(Message msg) {
                switch(msg.what) {
                case RE_QUERY: {
                    if (!queryInProgress) {
                    	new AsyncQuery().execute(thread);
                    } else {
                    	if (!handler.hasMessages(RE_QUERY)) {
	                    	Message requery = handler.obtainMessage(RE_QUERY);
	                    	handler.sendMessageDelayed(requery, MmsConfig.getFMSBInterval());
                    	}
                    }
                }
                default:
                	break;
                }
            }
        };
        
        
        // observe
        observer = new CacheObserver(new Handler());
        getContentResolver().registerContentObserver(Uri.parse(UtilClass.MEDIA_URI), false, observer);
    }

    @Override
    protected void onResume() {
    	if (mItemList == null) {
    		// no item yet, retrieve it from database
			new AsyncQuery().execute(thread);
    	}
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
		mPreferences.unregisterOnSharedPreferenceChangeListener(mPreferencesChangeListener);
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
    	super.onPause();
    }
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        // observer
        if (null != observer) {
            getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        // store list for screen rotation or etc
        return mItemList;
    }

    private void createList() {
    	if (mItemList != null) {
    		// Adaptor
    		mLinkListAdapter = new ListLinkAdapter(SharedLinksActivity.this, mItemList);
    		mLinkListView = (ListView) findViewById(R.id.locationlist);
    		mLinkListView.setAdapter(mLinkListAdapter);
    	}
    }
    

    /**
     * Observer
     * 
     * @author "Animesh Kumar <animesh@strumsoft.com>"
     * @Since Apr 30, 2012
     */
    class CacheObserver extends ContentObserver {
        public CacheObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(getClass(), "=> Dataset Changed, updating view!" );
        	}
			if (!handler.hasMessages(RE_QUERY)) {
				Message requery = handler.obtainMessage(RE_QUERY);
				handler.sendMessageDelayed(requery, MmsConfig.getFMSBInterval());
			}
        }
    }
    
    /**
     * Holder class
     * 
     * @author "Animesh Kumar <animesh@strumsoft.com>"
     * @Since Apr 10, 2012
     */
    class Link {
        final String  url;
        final String  address;
        final boolean outgoing;
        final long date;

        public Link(String link, String address, boolean outgoing, long date) {
            this.url = link;
            this.address = address;
            this.outgoing = outgoing;
            this.date = date;
        }

        @Override
        public String toString() {
            return "Link [url=" + url + ", address=" + address + ", outgoing=" + outgoing +
            		", date=" + MessageUtils.formatTimeStampString(date, true) + "]";
        }
    }

    /**
     * Queries for all links relevant to this conversation in background
     * 
     * @author "Animesh Kumar <animesh@strumsoft.com>"
     * @Since Apr 10, 2012
     */
    class AsyncQuery extends AsyncTask<Long, Void, List<Link>> {
    	String unknown = null;
    	
    	@Override
    	protected void onPreExecute() { 
    		queryInProgress = true;
    		unknown = getString(R.string.unknown_sender);
    	} 
    	
        @Override
        protected List<Link> doInBackground(Long... params) {
            List<Link> items = new ArrayList<Link>();

            long thread = params[0];
            String ct = Media.M_LINK_CT;
            String selectionForThread = (thread != INVALID ) ? MediaProvider.Helper.THREAD_ID + " = " + thread + " AND " : "";
			String where = selectionForThread + MediaProvider.Helper.M_PART_CT
					+ " = " + "'" + ct + "'";
            String sort = MediaProvider.Helper.DATE + " DESC ";
            
            Cursor q = getContentResolver().query(MediaSyncService.CACHE_URI, null, where, null, sort);

            if (q.moveToFirst()) {
                do {
                    Media media = MediaProvider.Helper.fromCursorCurentPosition(q);
                    ContactList contactList = ContactList.getByNumbers(media.getAddress(), false, false);
                    String name = unknown;
                    
                    if (contactList.size() > 0) {
                    	name = contactList.get(0).getName();
                    }
                    Link link = new Link(media.getText(),
                    					name,
                    					media.isOutgoing(),
                    					media.getDate());
                    if (Logger.IS_DEBUG_ENABLED) {
                    	Logger.debug(SharedLinksActivity.class, "link=" + link);
                    }
                    items.add(link);
                } while (q.moveToNext());
            }

            if (null != q) {
                q.close();
            }

            return items;
        }

        @Override
        protected void onPostExecute(List<Link> items) {
            super.onPostExecute(items);
            
            mItemList = items;
            createList();
            
            queryInProgress = false;
        }
    }

    class ListLinkAdapter extends BaseAdapter {
        final Context                           ctx;
        final Handler                           handler;
        final private List<Link>                links;
        final private Cache<String, LinkDetail> linkDetails;
        final private Cache<String, BitmapEntry> bitmapCache;

        public ListLinkAdapter(final Context ctx, final List<Link> _links) {
            this.ctx = ctx;
            links = _links;
            linkDetails = new Cache<String, LinkDetail>(DETAIL_CACHE_SIZE);
            bitmapCache = new Cache<String, BitmapEntry>(BITMAP_CACHE_SIZE);

            // Handler
            handler = new Handler() {
                public void handleMessage(Message msg) {
                    switch(msg.what) {
                    case MSG_URL_TASK: {
                        LinkDetail linkDetail = (LinkDetail) msg.obj; // object
                        
                        String link = linkDetail.getLink();
                        linkDetails.putToCache(link.trim(), linkDetail); // cache
                        mLinkListAdapter.notifyDataSetChanged(); // notify adaptor
                        break;
                    }
                    case MSG_BITMAP: {                    	
                    	int result = msg.arg1;
                		BitmapEntry entry = (BitmapEntry) msg.obj;
                		
                		// add to local cache
                		if (entry != null && entry.url != null) {
                			if (result == BitmapManager.OK || result == BitmapManager.NOT_FOUND) {
                				bitmapCache.putToCache(entry.url, entry);
                			}
                		}
            			
                    	// look for the View that has the request Id
                    	if (mLinkListView != null) {
                    		int size = mLinkListView.getChildCount();
                    		ImageView imageView = null;
                    		for (int i=0; i<size && imageView == null; i++) {
                    			View view = (View)mLinkListView.getChildAt(i);
                    			if (view != null) {
                    				ViewHolder holder = (ViewHolder)view.getTag();
                    				if (holder != null) {
                    					if (holder.imageView != null) {
//                    						BitmapTaskResult task = (BitmapTaskResult)holder.imageView.getTag();
                    						LinkTag oldLinkTag = (LinkTag)holder.imageView.getTag();
                    						if (oldLinkTag != null && entry != null && entry.url != null) {
                    							if (oldLinkTag.url != null && oldLinkTag.url.equals(entry.url)) {                    								
                    								// it's the correct ImageView
                    								imageView = holder.imageView;
                    								
                    								// clear the tag
                    								imageView.setTag(null);
                    								
                    								// found the view of match task        			
                    								switch(result) {
                    								case BitmapManager.OK: {
                    									if (SharedLinksActivity.this.setImageBitmap(imageView, entry.bitmap, mMinPreviewImage)) {
                    										imageView.setVisibility(View.VISIBLE);
                    									}
                    									break;
                    								}
                    								case BitmapManager.ERROR:
                    								case BitmapManager.NOT_FOUND: {
                    									// keep the generic image
                    									// imageView.setVisibility(View.GONE);
                    									break;
                    								}
                    								default:
                    									break;
                    								}
                    							}
                    						}
                    					}
                    				}
                    			}
                    		}
                    	}                    	
                    }
                    default:
                    	break;
                    }
                }
            };
        }

        @Override
        public int getCount() {
            return links.size();
        }

        @Override
        public Link getItem(int index) {
            return links.get(index);
        }

        @Override
        public long getItemId(int index) {
            return index;
        }

        /**
         * ViewHolder
         * 
         * @author "Animesh Kumar <animesh@strumsoft.com>"
         * @Since Apr 10, 2012
         */
        class ViewHolder {
            View            loading;
            View            details;
            TextView        title;
            TextView        desc;
            TextView        urlInside;
            TextView        urlOutside;
            TextView        sender;
            TextView		time;		// if it's not appended to the end of desc
            ScaledImageView imageView;
            
            View			error;
            ImageView		refresh;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Link item = links.get(position);
            ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflator = (LayoutInflater) ctx
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflator.inflate(R.layout.sharedlinksitem, null);

                holder = new ViewHolder();
                holder.loading = convertView.findViewById(R.id.viewLoading);
                holder.details = convertView.findViewById(R.id.detailsview);
                holder.title = (TextView) convertView.findViewById(R.id.txtTitle);
                holder.desc = (TextView) convertView.findViewById(R.id.txtDesc);
                holder.urlInside = (TextView) convertView.findViewById(R.id.txtUrlInside);
                holder.urlOutside = (TextView) convertView.findViewById(R.id.txtUrl);
                holder.sender = (TextView) convertView.findViewById(R.id.lastmessagesender);
                holder.imageView = (ScaledImageView) convertView.findViewById(R.id.imgPreview);
                holder.error = (View) convertView.findViewById(R.id.viewFailed);
                holder.refresh = (ImageView) convertView.findViewById(R.id.btnRefresh);
                holder.time = (TextView) convertView.findViewById(R.id.txtTime);
                
                convertView.setTag(holder);
                
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // this has to be done for new or old convertView because View objects are reused
            // if not reset all the data, it will contains old data
//            TextView link = (TextView) convertView.findViewById(R.id.txtUrl);
            holder.urlOutside.setText(item.url);
            holder.urlOutside.setVisibility(View.VISIBLE);
//            holder.time = 
            String date = MessageUtils.formatTimeStampString(item.date, false);
            holder.time.setText(date);
            holder.loading.setVisibility(View.GONE);
            holder.details.setVisibility(View.GONE);
//            holder.imageView.setVisibility(View.GONE);

            if (holder.refresh != null) {
            	holder.refresh.setTag(convertView);
            	holder.refresh.setOnClickListener(new OnClickListener() {
            		@Override
            		public void onClick(View view) {
            			try {
            				View parent = (View) view.getTag();
            				ViewHolder holder = (ViewHolder) parent.getTag();
            				TextView link = (TextView) parent.findViewById(R.id.txtUrl);
            				String url = link.getText().toString();

            				if (url != null) {
            					holder.urlOutside.setVisibility(View.VISIBLE);
            					showLoading(holder);

            					// clear cache and refetch
            					fetchUrl(url, true);
            				}
            			}
            			catch (Exception e) {
                            Logger.error(SharedLinksActivity.class, e);
            			}
            		}            			
            	});
            }
            
            if (mEnableUrlPreview) {            
            	populateView(position, holder);
            }

            return convertView;
        }

        /**
         * This Method fetches URL in background
         * 
         * @param link
         */
        private void fetchUrl(final String link, boolean refetch) {
            HrefManager.INSTANCE.loadLink(ctx, link, handler, MSG_URL_TASK, refetch);
        }

        private void showLoading(ViewHolder holder) {
        	holder.loading.setVisibility(View.VISIBLE);
        	holder.details.setVisibility(View.GONE);
        }

        /**
         * This Method populates the view.
         * 
         * @param index
         * @param holder
         */
        private void populateView(int index, ViewHolder holder) {
            Link link = links.get(index);
            String url = link.url.trim();

            LinkDetail linkDetail = linkDetails.getFromCache(url);
//            if (linkDetail == null) {
//                linkDetails.dumpKeys();
//            }

            // fetch URL if unavailable
            if (null == linkDetail) {
            	linkDetail = HrefManager.INSTANCE.getFromCache(ctx, url);
            	
            	if (null == linkDetail) {           			
            		showLoading(holder);
            		fetchUrl(url, false); // fetch URL in background
            		return;
            	}
            	else {
            		linkDetails.putToCache(url, linkDetail);
            	}
            }
            
            // we have linkDetail now

            holder.loading.setVisibility(View.GONE);
            holder.details.setVisibility(View.VISIBLE);

            if (linkDetail.getError() != null) {
                holder.title.setText(Html.fromHtml(linkDetail.getError()));
                
                // 2012-06-04 show error message BZ#227
                // 2012-08-12 disable error BZ#1276
                holder.error.setVisibility(SHOW_PREVIEW_ERROR? View.VISIBLE : View.GONE);
            }
            else {
            	holder.error.setVisibility(View.GONE);
            }

            // title, this supercede the error text set above
            if (linkDetail.getTitle() != null) {
                holder.title.setText(Html.fromHtml(linkDetail.getTitle()));
                holder.title.setVisibility(View.VISIBLE);
            } else {
                holder.title.setVisibility(View.GONE);
            }

            // Description
            if (linkDetail.getDescription() != null) {
                holder.desc.setText(Html.fromHtml(linkDetail.getDescription()));
                holder.desc.setVisibility(View.VISIBLE);
            }
            else {
                holder.desc.setVisibility(View.GONE);
            }

            // Url
            url = linkDetail.getLink();	//linkDetail.getOgUrl() != null ? linkDetail.getOgUrl() : linkDetail.getLink();
            holder.urlInside.setText(url);
            holder.urlOutside.setVisibility(View.GONE);

            // Image
    		// Image and every object in View MUST BE set because row View objects are being recycled in Adapter.
    		// So any child View is not set, it will show data from previous item that used this child View.
    		// Set to GONE if we don't want to show it or set to a default image
//            BitmapTaskResult oldTask = (BitmapTaskResult)holder.imageView.getTag();
            LinkTag oldLinkTag = (LinkTag)holder.imageView.getTag();
        	holder.imageView.setTag(null);
            
            if (defaultPreviewBitmap == null) {
            	defaultPreviewBitmap = com.verizon.mms.util.BitmapManager.INSTANCE.decodeResource(ctx.getResources(), R.drawable.default_link1);
            }
            if (defaultPreviewBitmap != null) {
            	holder.imageView.setImageBitmap(defaultPreviewBitmap);
            }
            			
            String imageUrl = linkDetail.getLinkImage();
            if (imageUrl != null) {
            	// check local cache
            	BitmapEntry bitmapEntry = bitmapCache.getFromCache(imageUrl);
            	if (bitmapEntry != null) {
            		// we found it in local cache
           			setImageBitmap(holder.imageView, bitmapEntry.bitmap, mMinPreviewImage);
            	}
            	else {
            		// check global cache
            		// calculate the pixel size we want, use the same size as in Conversation so to use the same images
            		int pixel = BitmapManager.dipToPixel(ctx, R.dimen.linkPreviewImageSize);
            		BitmapManager bmMgr = BitmapManager.INSTANCE;
            		BitmapEntry result = bmMgr.getBitmap(ctx, imageUrl, pixel, pixel);//, minPreviewImage, handler, MSG_BITMAP);
            		if (result != null) {
            			// entry found in cache
            			if (result.bitmap != null) {
            				// bitmap can be null, e.g. if result is 404 NOT FOUND
            				if (setImageBitmap(holder.imageView, result.bitmap, mMinPreviewImage) == false) {
//            					result = NOT_USED;
            				}
            			}
            			else {
            				// result found but not bitmap, so previous download didn't get a bitmap
            				// TODO check response code to reload or not
            			}
            		}
            		else {
            			// not found in cache            			
            			boolean download = true;            			
            			if (oldLinkTag != null) {
            				if (oldLinkTag.url.equals(imageUrl)) {
            					// still is queue, no need to reschedule
            					holder.imageView.setTag(oldLinkTag);
            					download = false;
            					oldLinkTag = null;
            				}
            			}
            			
            			if (download) {
            				// schedule to download
            				boolean success = bmMgr.loadBitmap(ctx, linkDetail.getLinkImage(), pixel, pixel, mMinPreviewImage, handler, MSG_BITMAP);
            				if (success) {
            					LinkTag tag = new LinkTag();
            					tag.url = linkDetail.getLinkImage();
            					holder.imageView.setTag(tag);
            				}
            				else {
            					holder.imageView.setTag(null);
            				}
            			}
            		}
            	}
            }
            else {
            	// there is no url for image preview extracted
            }
            holder.imageView.setVisibility(View.VISIBLE);

            if (oldLinkTag != null) {
            	// cancel previous task and reset the tag
            	BitmapManager.INSTANCE.cancelRequest(handler, oldLinkTag.url);
            }
            
            // Sender
            if (link.outgoing) {
                holder.sender.setText(R.string.me);
            }
            else {
                holder.sender.setText(link.address);
            }
            
            // time
            
        }
    }
        
      /**
      * Set the bitmap to use in an ImageView.
      * 
      * Bitmap is set only if any side of the bitmap is larger than the minimum dimension required.
      * 
      * @param imageView
      * @param bitmap
      * @param minDimension
      * @return
      */
     private boolean setImageBitmap(ImageView imageView, Bitmap bitmap, int minDimension) {
     	if (minDimension > 0 && bitmap != null && 
         	(bitmap.getWidth() >= minDimension || bitmap.getHeight() >= minDimension)) {
         	imageView.setImageBitmap(bitmap);
         	imageView.setVisibility(View.VISIBLE);
         	return true;
         }    	
     	return false;
     }
     
 	/**
 	 * Object to be set as tag in the preivew ImageView
 	 */
 	private class LinkTag {
 		String url = null;;
 	};
}
