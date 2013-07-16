package com.verizon.mms.ui;

import static com.verizon.mms.ui.GalleryItem.TYPE_CONTACT;
import static com.verizon.mms.ui.GalleryItem.TYPE_IMAGE;
import static com.verizon.mms.ui.GalleryItem.TYPE_LINK;
import static com.verizon.mms.ui.GalleryItem.TYPE_LOCATION;
import static com.verizon.mms.ui.GalleryItem.TYPE_VIDEO;
import static com.verizon.mms.ui.TabletSharedContentFragment.WEIGHT_TOTAL;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Threads;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ContentType;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.Media;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.data.RecipientIdCache;
import com.verizon.mms.helper.Cache;
import com.verizon.mms.helper.HrefManager;
import com.verizon.mms.helper.LinkDetail;
import com.verizon.mms.model.IModelChangedObserver;
import com.verizon.mms.model.LocationModel;
import com.verizon.mms.model.Model;
import com.verizon.mms.model.RegionModel;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.model.VCardModel;
import com.verizon.mms.pdu.PduBody;
import com.verizon.mms.pdu.PduPart;
import com.verizon.mms.util.BitmapManager;
import com.verizon.mms.util.BitmapManager.OnBitmapLoaded;
import com.verizon.mms.util.Rotate3dAnimation;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;

public class TabletSharedContentAdapter extends BaseAdapter {
    private int itemSize;
    private ArrayList<GalleryRowData> rows;
    private CacheMap viewCache;
    private Cache<String, TextView> linkTitles;
    private Cache<String, LinkDetail> linkDetails;
    private Context context;
    private long threadID;
    private static Handler handler;
    private static final int DETAIL_CACHE_SIZE = 30;
    private static final int MSG_URL_TASK = 1;
    private LayoutInflater inflater;
    private Resources res;
	private HashSet<Long> animated;
    private boolean animate = true;
	private BitmapManager bitmapMgr;
    private File imagesFolder;
    protected Handler serviceHandler;
	private static int cacheSize;
	private static int[] fixedChildren = { R.id.play, R.id.image_name, R.id.image_shared_time };

	private static final float CACHE_PERCENT = 0.3f;       // percent of threshold memory to allocate to cache
	private static final int CACHE_MAX = 16 * 1024 * 1024; // max cache size
    private static final int NUM_FIXED_CHILDREN = 2;       // number of fixed children in item layouts
    private static final int NUM_COLS = 3;                 // number of items in a row

    private static final String CACHED_LINK_PREFIX = "link-";  // prefix for cached link images


    public TabletSharedContentAdapter(long thread, Context ctx, ArrayList<GalleryRowData> rows) {
        this.context = ctx;
        this.rows = rows;
        this.threadID = thread;

        bitmapMgr = BitmapManager.INSTANCE;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        res = context.getResources();

        // calculate the item size based on the current width of the content area
        final int listWeight = res.getInteger(R.integer.tabletListWeight);
        final int contentWeight = res.getInteger(R.integer.tabletListWeight);
        final float contentPercent = (float)contentWeight / (listWeight + contentWeight);
        final DisplayMetrics metrics = res.getDisplayMetrics();
        final int totalWidth = metrics.widthPixels - res.getDimensionPixelSize(R.dimen.tabletDividerWidth);
        itemSize = Math.round((totalWidth * contentPercent) / 3f);
        //TODO: limit size of cached data
        imagesFolder = context.getCacheDir();
        mServiceThread.start();

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "TabletSharedContentAdapter.ctor: thread = " + thread + ", itemSize = " + itemSize);
		}

		createCaches();

        handler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_URL_TASK: {
                    LinkDetail linkDetail = (LinkDetail) msg.obj; // object
                    final String link = linkDetail.getLink().trim();
                    linkDetails.putToCache(link, linkDetail); // cache

                    // set view title
                    final TextView title = linkTitles.getFromCache(link);
            		if (Logger.IS_DEBUG_ENABLED) {
            			Logger.debug(getClass(), "TabletSharedContentAdapter.handleMessage: link = " + link + ", view = " + title);
            		}
                    if (title != null) {
                    	setLinkTitle(title, linkDetail);
                    }
                    else {
                    	Logger.error("TabletSharedContentAdapter.handleMessage: no title for link " + link);
                    }
                    break;
                }

                default:
                    break;
                }
            }
        };
    }

    private void createCaches() {
        linkDetails = new Cache<String, LinkDetail>(DETAIL_CACHE_SIZE);
        linkTitles = new Cache<String, TextView>(DETAIL_CACHE_SIZE);
        animated = new HashSet<Long>(32);

        if (cacheSize == 0) {
			final ActivityManager mgr = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
			final MemoryInfo info = new MemoryInfo();
			mgr.getMemoryInfo(info);
			cacheSize = (int)(info.threshold * CACHE_PERCENT);
			if (cacheSize > CACHE_MAX) {
				cacheSize = CACHE_MAX;
			}
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "createCaches: set cacheSize to " + cacheSize +
					", threshold = " + info.threshold);
			}
		}

        viewCache = new CacheMap(cacheSize);
    }

    /**
     * Notifies us that that adapter is no longer in use and we should free resources.
     */
	public void shutdown() {
		// shut down the service thread
		if (serviceHandler != null && mServiceThread.isAlive()) {
			final Looper looper = serviceHandler.getLooper();
			if (looper != null) {
				looper.quit();
			}
		}

		linkDetails.clear();
		linkTitles.clear();

		for (View view : viewCache.values()) {
			if (view instanceof ImageView) {
				((ImageView)view).setImageDrawable(null);
			}
			else if (view instanceof WebView) {
				final ViewParent parent = view.getParent();
				if (parent instanceof ViewGroup) {
					((ViewGroup)parent).removeView(view);
				}
				((WebView)view).destroy();
			}
		}
		viewCache.clear();

	}

	private static class ViewHolder {
    	private GalleryRowData grd;
        private ViewGroup[] items = new ViewGroup[NUM_COLS];
    }

	@Override
	public View getView(int index, View v, ViewGroup parent) {
		final GalleryRowData grd = rows.get(index);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "getView: grd = " + grd);
		}

		// TODO use actual number of rows
		if (index > 2) {
			animate = false;
		}

		final ViewHolder holder;
		if (v != null) {
			// recycled view: if it has the same row data then no need to do anything
			holder = (ViewHolder)v.getTag();
			if (holder.grd == grd) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "getView: recycled with same data");
				}
				return v;
			}

			// different data: remove views from wrappers
			for (int i = 0; i < NUM_COLS; ++i) {
				clearContent(holder.items[i]);
			}
		}
		else {
			// create new view and holder
			holder = new ViewHolder();
			v = inflater.inflate(R.layout.tabletgalleryrow, null);
			holder.items[0] = (ViewGroup)v.findViewById(R.id.first_item);
			holder.items[1] = (ViewGroup)v.findViewById(R.id.second_item);
			holder.items[2] = (ViewGroup)v.findViewById(R.id.third_item);
			v.setTag(holder);
		}
		holder.grd = grd;

		final int totalItems = grd.count;
		final int totalWeight = grd.totalWeight;

		// set the content for all items in the row
		int i = 0;
		while (i < totalItems) {
			final GalleryItem item = grd.getItem(i);
			final ViewGroup wrapper = holder.items[i++];
			setContent(item, wrapper, totalWeight);
			wrapper.setVisibility(View.VISIBLE);
		}

		// if the row is unfilled then blank any unused cells
		if (totalItems < NUM_COLS) {
			while (i < NUM_COLS - 1) {
				holder.items[i++].setVisibility(View.GONE);
			}

			// set the weight of the last cell to fill the remaining space
			final ViewGroup vg = holder.items[NUM_COLS - 1];
			clearContent(vg);
			for (int id : fixedChildren) {
				vg.findViewById(id).setVisibility(View.GONE);
			}
			final int lastWeight = WEIGHT_TOTAL - totalWeight;
			vg.setLayoutParams(new LinearLayout.LayoutParams(0, itemSize, lastWeight));
		}

		return v;
	}

	/**
	 * Removes the content views from the item layout.
	 */
	private void clearContent(ViewGroup item) {
		if (item != null) {
			int count = item.getChildCount();
			while (count-- > NUM_FIXED_CHILDREN) {
				item.removeViewAt(0);
			}
		}
		item.setOnClickListener(null);
	}

	/**
	 * 
	 * Sets the content of the items in the given row.
	 * 
	 * @param item
	 *            the item data
	 * @param wrapper
	 *            the layout that will contain the content
	 * @param totalWeight
	 *            the total weight of the views in the row
	 */
	private void setContent(final GalleryItem item, ViewGroup wrapper, int totalWeight) {
		// set wrapper view weight
		wrapper.setLayoutParams(new LinearLayout.LayoutParams(0, itemSize, item.weight));

		// check if we have a cached view for this item
		final long itemId = item.id;
		View cached = viewCache.get(itemId);
		if (cached != null && cached.getParent() != null) {
			cached = null;
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "setContent: item = " + item + ", cached = " + cached);
		}

		int playVis = View.GONE;
		final LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

		switch (item.type) {
			case TYPE_IMAGE: {
				final ImageView img;
				if (cached == null) {
					img = new ImageView(context);
					img.setScaleType(ScaleType.CENTER_CROP);
					viewCache.put(itemId, img);

					bitmapMgr.loadBitmap(item.media.getImageUri(), img,
						getViewWidth(item.weight), itemSize, true, bitmapListener);
				}
				else {
					img = (ImageView)cached;
				}

				// TODO play button should only be enabled for animated GIFs
				if ("image/gif".equalsIgnoreCase(item.media.getmPartCt())) {
					playVis = View.VISIBLE;
				}

				addView(wrapper, img, 0, params);
				wrapper.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onImageOrVideoClicked(item);
					}
				});

				break;
			}

			case TYPE_VIDEO: {
				final ImageView img;
				if (cached == null) {
					img = new ImageView(context);
					img.setScaleType(ScaleType.CENTER_CROP);
					viewCache.put(itemId, img);

					bitmapMgr.loadVideoThumbnail(item.media.getVideoUri(), img,
						getViewWidth(item.weight), itemSize, bitmapListener);
				}
				else {
					img = (ImageView)cached;
				}

				playVis = View.VISIBLE;

				addView(wrapper, img, 0, params);
				wrapper.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onImageOrVideoClicked(item);
					}
				});
				break;
			}

			case TYPE_LOCATION: {
				PduBody body = PduBodyCache.getPduBody(context,
						ContentUris.withAppendedId(VZUris.getMmsUri(), item.media.getMId()));
            if (body != null) {
    	
    				Uri URI = null;
    				int partNum = body.getPartsNum();
    				for (int i = 0; i < partNum; i++) {
    					PduPart part = body.getPart(i);
    					String type = new String(part.getContentType());
    					if (ContentType.isImageType(type) || ContentType.isVideoType(type) || ContentType.isAudioType(type)) {
    						URI = part.getDataUri();
    					}
    				}
    				
    				final ImageView img;
    				if (cached == null) {
    					img = new ImageView(context);
    					img.setScaleType(ScaleType.CENTER_CROP);
    					viewCache.put(itemId, img);
    
    					Uri path = Uri.parse("android.resource://"+context.getPackageName()+"/"+ R.drawable.loc_menu_place_media);
    					// TODO add a default image just in case.
    					bitmapMgr.loadBitmap(URI != null ? URI.toString() : path.toString(), img,
    						getViewWidth(item.weight), itemSize, true, bitmapListener);
    				}
    				else {
    					img = (ImageView)cached;
    				}
    
                    addView(wrapper, img, 0, params);
    
    				try {
    					final LocationModel loc = new LocationModel(context, Uri.parse(item.media.getLocationUri()), new RegionModel(
    							null, 0, 0, 0, 0));
    					TextView title = (TextView)wrapper.findViewById(R.id.image_name);
    					String place = loc.getFormattedMsg().replace(", ", "\n");
    					title.setText(place);
    
    					wrapper.setOnClickListener(new View.OnClickListener() {
    						@Override
    						public void onClick(View v) {
    						    if(DeviceConfig.OEM.isNbiLocationDisabled){
    						        onImageOrVideoClicked(item);
    						        return;
    						    }
    							ContactStruct contactStruct = loc.getContactStruct();
    							if (contactStruct != null) {
    								String url = contactStruct.getURL();
    								if (url != null) {
    									Intent intent = new Intent(context, AddLocationActivity.class);
    									intent.putExtra("mapURL", url);
    									intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    									context.startActivity(intent);
    								}
    								else {
    									Toast.makeText(context, R.string.url_not_present, Toast.LENGTH_LONG).show();
    								}
    							}
    							else {
    								Toast.makeText(context, R.string.url_not_present, Toast.LENGTH_LONG).show();
    							}
    						}
    					});
    				}
    				catch (MmsException e) {
                }
            }
				break;
			}

        case TYPE_LINK: {
            //variable used to avoid file operation once image is cashed and is in view
            boolean mImageIsSaved = false;
            
            if (cached == null) {
                File output = new File(imagesFolder, getLinkFileName(item.media.getText()));
                if (output.exists()) {
                    final ImageView img;

                    img = new ImageView(context);
                    img.setScaleType(ScaleType.FIT_XY);
                    viewCache.put(itemId, img);
                    bitmapMgr.loadBitmap("file:///" + output.getAbsolutePath(), img,
                            getViewWidth(item.weight), itemSize, false, bitmapListener);

                    addView(wrapper, img, 0, params);
                    wrapper.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String url = item.media.getText();
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "http://" + url;
                            }
                            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(myIntent);
                        }
                    });
                    mImageIsSaved = true;
                }
            } else {
                if (cached instanceof ImageView) {
                    ImageView img = (ImageView) cached;
                    addView(wrapper, img, 0, params);
                    wrapper.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String url = item.media.getText();
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "http://" + url;
                            }
                            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(myIntent);
                        }
                    });
                    mImageIsSaved = true;
                } else if (cached instanceof WebView) {
                    File output = new File(imagesFolder, getLinkFileName(item.media.getText()));
                    if (output.exists()) {

                        final ViewParent old = cached.getParent();
                        if (old instanceof ViewGroup) {
                            ((ViewGroup) old).removeView(cached);
                        }
                        final ImageView img;

                        img = new ImageView(context);
                        img.setScaleType(ScaleType.FIT_XY);
                        viewCache.put(itemId, img);
                        bitmapMgr.loadBitmap("file:///" + output.getAbsolutePath(), img,
                                getViewWidth(item.weight), itemSize, false, bitmapListener);
                        addView(wrapper, img, 0, params);
                        wrapper.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String url = item.media.getText();
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    url = "http://" + url;
                                }
                                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(myIntent);
                            }
                        });
                        mImageIsSaved = true;
                    }
                }
            }

            if (!mImageIsSaved) {

                final WebView wb;
                if (cached == null) {
                    wb = new WebView(context);

                    WebSettings webSettings = wb.getSettings();
                    webSettings.setLoadWithOverviewMode(true);
                    webSettings.setUseWideViewPort(true);
                    webSettings.setJavaScriptEnabled(true);

                    final Message message = Message.obtain();
                    message.obj = new QueuedJob(item.media.getText(), wb, QueuedJob.ACTION_CHECK_FILE);
                    serviceHandler.sendMessageDelayed(message, 200);

                    viewCache.put(itemId, wb);
                }

                else {
                    wb = (WebView) cached;

                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "Web preview RECASHED  " + wb.getUrl());
                    }
                    // if its not file then there is a chance we have stoped the redirect.
                    if (null != wb.getUrl() && !wb.getUrl().startsWith("file"))
                        wb.reload();
                }

                addView(wrapper, wb, 0, params);

                wb.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            String url = item.media.getText();
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "http://" + url;
                            }
                            Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(myIntent);
                        }
                        return true;
                    }
                });

            }
            break;
        }

			case TYPE_CONTACT: {
				final View contactLayout;
				if (cached == null) {
					contactLayout = getContactLayout(item.media);
					viewCache.put(itemId, contactLayout);
				}
				else {
					contactLayout = cached;
				}
				addView(wrapper, contactLayout, 0, params);
				break;
			}

			default:
				Logger.error(getClass(), "setContent: invalid item " + item);
				break;
		}

		wrapper.findViewById(R.id.play).setVisibility(playVis);

		updateTitleAndTime((TextView)wrapper.findViewById(R.id.image_name),
			(TextView)wrapper.findViewById(R.id.image_shared_time), item);

		if (animate && !animated.contains(itemId)) {
			animated.add(itemId);
			startAnimation(wrapper, item.weight, totalWeight);
		}

//		if (Logger.IS_DEBUG_ENABLED) {
//			Util.dumpView(wrapper, "TabletSharedContentAdapter.setContent: after:");
//		}
	}

	private void addView(ViewGroup parent, View child, int index, LayoutParams params) {
		final ViewParent old = child.getParent();
		if (old instanceof ViewGroup) {
			((ViewGroup)old).removeView(child);
		}
		parent.addView(child, index, params);
	}

	private OnBitmapLoaded bitmapListener = new OnBitmapLoaded() {
		@Override
		public void onBitmapLoaded(String url, Bitmap bmp, ImageView imageView) {
			viewCache.updateSize();
		}
	};

	private WebViewClient webViewClient = new WebViewClient() {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String urlNewString) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ": view " + view + ": loading: " + urlNewString);
			}
			if (view.isShown()) {
				return false;
			}
			else {
				view.stopLoading();
			}
			return true;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ": view " + view + ": page started: " + url);
			}
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ": view " + view + ": page finished: " + url);
			}
		}
	};

	private View getContactLayout(final Media mms) {
		//TODO create string elements for all static string in function
		View view = null;
		view = inflater.inflate(R.layout.tabletgallery_contact, null);
		TextView nameTextView = (TextView)view.findViewById(R.id.tablet_contact_name);
		Button primary = (Button)view.findViewById(R.id.tablet_contact_btn_primary);
		Button secondary_left = (Button)view.findViewById(R.id.tablet_contact_btn_sec_one);
		Button secondary_right = (Button)view.findViewById(R.id.tablet_contact_btn_sec_two);
		final ImageView icon = (ImageView)view.findViewById(R.id.tablet_contact_icon);
		icon.setVisibility(View.INVISIBLE);
		
		secondary_right.setText(R.string.go_to_message);
		secondary_left.setText(R.string.add_or_view);
		primary.setText(R.string.text);
		// not a name card case
		if (!mms.isNameCard() && (mms.isPhone() || mms.isEmail())) {
			nameTextView.setText(mms.getText());
			secondary_right.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					long retVal = getThreadId(mms.getText());
					Intent intent = ConversationListActivity.getIntentFromParent(context, retVal, true);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, mms.getText());
					context.startActivity(intent);
				}
			});
		
			if (mms.isPhone()) {
				primary.setText(R.string.text);
				primary.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent messageIntent = ConversationListActivity.getIntentFromParent(context, 0, true);
						messageIntent.putExtra(ComposeMessageActivity.SEND_RECIPIENT, true);
						messageIntent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, mms.getText());
						messageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(messageIntent);
					}
				});

				secondary_left.setText(R.string.add_or_view);
				secondary_left.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Contact con = Contact.get(mms.getText(), true);

						if (con.existsInDatabase()) {
							Uri contactUri = con.getUri();
							Intent viewContact = new Intent(Intent.ACTION_VIEW, contactUri);
							viewContact.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							con.markAsStale();
							context.startActivity(viewContact);
						} else {
							con.markAsStale();
							Intent conIntent = ConversationListFragment.createAddContactIntent(mms.getText());
							conIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(conIntent);
						}
					}
				});
			}

			if (mms.isEmail()) {
				primary.setText(R.string.email_text);
				primary.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {

						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + mms.getText()));
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(intent);
					}
				});

				
				secondary_left.setText(R.string.add_or_view);
				secondary_left.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						if (haveEmailContact(mms.getText())) {
							Intent viewContact = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI,
									Uri.encode(mms.getText())));
							viewContact.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							try {
								context.startActivity(viewContact);
							} catch (ActivityNotFoundException e) {
								if (Logger.IS_DEBUG_ENABLED) {
									Logger.debug(getClass(),"Could not open :"+ Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI,Uri.encode(mms.getText())));
								}
							}
						} else {
							Intent conIntent = ConversationListFragment.createAddContactIntent(mms.getText());
							conIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(conIntent);
						}
					}
				});
			}
		}
		else {
			VCardModel vcm = null;
			try {
				vcm = new VCardModel(context, Uri.parse(mms.getNameCardUri()), new RegionModel(null, 0, 0, 0, 0));
			}
			catch (MmsException e) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error(e);
                }
            } finally {
              //if we don't get the content = return blank view.
                if (vcm == null) {
                    primary.setEnabled(false);
                    secondary_left.setEnabled(false);
                    secondary_right.setEnabled(false);
                    return view;
                }
            }
			
			final VCardModel vCardModel = vcm;
			
			if (vcm.getContactPicture() != null) {
				icon.setImageBitmap(vcm.getContactPicture());
				icon.setVisibility(View.VISIBLE);
			}
			else {
				final IModelChangedObserver mVCardModelChangedObserver = new IModelChangedObserver() {
					public void onModelChanged(final Model model, final boolean dataChanged) {
						VCardModel vcm = (VCardModel)model;
						if (vcm.getContactPicture() != null) {
							icon.setImageBitmap(vcm.getContactPicture());
						}
					}
				};
				vcm.registerModelChangedObserver(mVCardModelChangedObserver);
			}
			String initiated = null;
			while (initiated == null) {
				initiated = vcm.getFormattedMsg();
			}
			ContactStruct vCardContact = vcm.getContactStruct();
			if(vCardContact == null){
				OnClickListener EmptyVCardListener = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, context.getString(R.string.name_card_parse_error), Toast.LENGTH_LONG).show();
                    }
                };
                primary.setOnClickListener(EmptyVCardListener);
                secondary_left.setOnClickListener(EmptyVCardListener);
                secondary_right.setOnClickListener(EmptyVCardListener);
			}
			else
			{	
			final String name = vCardContact.getName();
			final String number = vCardContact.getFirstNumber();
			final String email = vCardContact.getFirstEmail();
			if (name != null && name != "") {
				nameTextView.setText(name);
			}
			else if (number != null && number != "") {
				nameTextView.setText(number);
			}
			else if (email != null && email != "") {
				nameTextView.setText(email);
			}
			if(number == null && email == null)
			{
				secondary_left.setEnabled(false);
				secondary_right.setEnabled(false);
				primary.setEnabled(false);
			}
			
			if(number!=null)
			{
				secondary_right.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						long retVal = getThreadId(number);
						Intent intent = ConversationListActivity.getIntentFromParent(context, retVal, true);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, number);
						context.startActivity(intent);
					}
				});
				primary.setText(R.string.text);
				primary.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Intent messageIntent = ConversationListActivity.getIntentFromParent(context, 0, true);
						messageIntent.putExtra(ComposeMessageActivity.SEND_RECIPIENT, true);
						messageIntent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, number);
						messageIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(messageIntent);
					}
				});
			}
			else if(email != null)
			{
				secondary_right.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						long retVal = getThreadId(email);
						Intent intent = ConversationListActivity.getIntentFromParent(context, retVal, true);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, email);
						context.startActivity(intent);
					}
				});
				primary.setText(R.string.email_text);
				primary.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {

						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + mms.getText()));
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						context.startActivity(intent);
					}
				});
			}
				
			if (number != null || email != null) {
				secondary_left.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						MessageUtils.showVCard(context, vCardModel, true, Intent.FLAG_ACTIVITY_NEW_TASK);
					}
				});
			}
		}
		}	
		return view;
	}

	/**
	 * It will get the Recipient Id from Cache using Number and returns thread id for that recipient id 
	 * if exists, else return 0 to compose a new Message
	 * @param number
	 * @return
	 */
	protected long getThreadId(String number) {
		long retVal = 0;
		long recipientId = RecipientIdCache.getRecipientId(number);
		if(recipientId != -1){
			Uri sAllThreadsUri = VZUris.getMmsSmsConversationUri().buildUpon()  
		            .appendQueryParameter("simple", "true").build();
			
			Cursor cursor = context.getContentResolver().query(sAllThreadsUri, new String[] {Threads._ID}, 
					Threads.RECIPIENT_IDS +" = ?", new String[] {String.valueOf(recipientId)}, null);
			if (cursor!= null && cursor.getCount()> 0 &&  cursor.moveToFirst()) {
				do {
					
					retVal = cursor.getLong(cursor
							.getColumnIndex(Threads._ID));
					break;
				} while (cursor.moveToNext());
			}
		}
		if(Logger.IS_DEBUG_ENABLED){
			Logger.debug("returned thread id: "+retVal +" recipient Id: "+recipientId +" contact: "+number);
		}
		return retVal;		
	}

	private void updateTitleAndTime(TextView titleView, TextView nameAndDate, GalleryItem item) {
		ContactList cList = ContactList.getByNumbers(item.media.getAddress(), false, false);
        boolean checkOutGoing = true;
        try {
            checkOutGoing = item.media.isOutgoing();
        } catch (Exception e) {
            Logger.debug(this +" Getting exception" +e);
        }
		if (checkOutGoing) {
			nameAndDate.setText(Html.fromHtml("<b> Me </b><small><font color='#C0C0C0'>"
					+ MessageUtils.formatTimeStampString(item.media.getDate(), true) + "</font></small>"));
		}
		else if (cList.size() > 0) {
			Contact contact = cList.get(0);
			nameAndDate.setText(Html.fromHtml("<b>" + contact.getName() + " </b><small><font color='#C0C0C0'>"
					+ MessageUtils.formatTimeStampString(item.media.getDate(), true) + "</font></small>"));
		}
		else {
			nameAndDate.setText(Html.fromHtml("<b>" + item.media.getAddress() + " </b><small><font color='#C0C0C0'>"
					+ MessageUtils.formatTimeStampString(item.media.getDate(), true) + "</font></small>"));
		}
		nameAndDate.setVisibility(View.VISIBLE);

		if (item.type == TYPE_LINK) {
			String url = item.media.getText();

			LinkDetail linkDetail = linkDetails.getFromCache(url);
			if (null == linkDetail) {
				linkDetail = HrefManager.INSTANCE.getFromCache(context, url);
				if (null == linkDetail) {
					fetchUrl(url, titleView); // fetch URL in background
					titleView.setText(R.string.loading);
					return;
				}
				else {
					linkDetails.putToCache(url, linkDetail);
				}
			}

			// we have linkDetail now
			setLinkTitle(titleView, linkDetail);
		}
		else {
			titleView.setVisibility(View.GONE);
		}
	}

    private void setLinkTitle(TextView titleView, LinkDetail linkDetail) {
		if (linkDetail.getError() != null) {
            titleView.setText(linkDetail.getLink() + "\n" + res.getString(R.string.cannot_fetch_link_preview));
			titleView.setVisibility(View.VISIBLE);
		}
		else if (linkDetail.getTitle() != null) {
			titleView.setText(Html.fromHtml(linkDetail.getTitle()));
			titleView.setVisibility(View.VISIBLE);
		}
		else if (linkDetail.getDescription() != null) {
			titleView.setText(Html.fromHtml(linkDetail.getDescription()));
			titleView.setVisibility(View.VISIBLE);
		}
		else {
			titleView.setVisibility(View.GONE);
		}
	}

	private void fetchUrl(final String link, TextView titleView) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "fetchUrl: link = " + link + ", view = " + titleView);
		}
		linkTitles.putToCache(link, titleView);
        HrefManager.INSTANCE.loadLink(context, link, handler, MSG_URL_TASK, false);
    }

    // on Image Or video  clicked
    private void onImageOrVideoClicked(GalleryItem item) {
        Intent goToGallery = new Intent(context, GalleryActivity.class);
        goToGallery.putExtra("itemtogo", "content://" + VZUris.getMmsUri().getAuthority() + "/"
                + item.media.getMId());
        goToGallery.putExtra("threadid", threadID);
        goToGallery.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(goToGallery);
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return rows.get(position).rowId;
    }

    private int getViewWidth(int itemWeight) {
    	return Math.round(itemSize * ((float)itemWeight / ((float)WEIGHT_TOTAL / NUM_COLS)));
    }

    private void startAnimation(View v, int weight, int totalweight) {
        final float center = itemSize / 2.0f;

        AnimationSet animationSet = new AnimationSet(true);

        // adding Alpha animation
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(1500);
        alphaAnimation.setFillAfter(true);
        animationSet.addAnimation(alphaAnimation);

        // setting Rotation
        final Rotate3dAnimation rotation = new Rotate3dAnimation(80, 0, center, center, 310.0f, true);
        rotation.setDuration(1500);
        rotation.setFillAfter(true);
        rotation.setInterpolator(new AccelerateInterpolator());
        animationSet.addAnimation(rotation);

        v.startAnimation(animationSet);
    }

    private boolean haveEmailContact(String emailAddress) {
        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress)),
                new String[] { Contacts.DISPLAY_NAME }, null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    if (!TextUtils.isEmpty(name)) {
                        return true;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    private static class PduBodyCache {
        private static PduBody mLastPduBody;
        private static Uri mLastUri;

        static public PduBody getPduBody(Context context, Uri contentUri) {
            if (contentUri.equals(mLastUri)) {
                return mLastPduBody;
            }
            try {
                mLastPduBody = SlideshowModel.getPduBody(context, contentUri);
                mLastUri = contentUri;
            } catch (MmsException e) {
                Logger.error(ComposeMessageFragment.class, e.getMessage(), e);
                return null;
            }
            return mLastPduBody;
        }
    }

    /**
     * Maintains the sum of the cached view sizes below a given limit, removing in LRU order as needed,
     * and giving priority to smaller views.
     */
	@SuppressWarnings("serial")
	private static class CacheMap extends LinkedHashMap<Long, View> {
		private int cacheSize;
		private int maxCacheSize;

		private static final int INITIAL_CAPACITY = 40;
		private static final int VIEW_SIZE = 100;    // order-of-magnitude estimate of overhead for view objects
		private static final int LARGE_VIEW = 5000;  // limit between "small" and "large" views, e.g. images and contacts


		public CacheMap(int maxCacheSize) {
			super(INITIAL_CAPACITY, 1.0f, true);
			this.maxCacheSize = maxCacheSize;
		}

		/**
		 * Put the view into the cache, adding its size if it is loaded.
		 * NB assumes caller is holding the lock on this object
		 */
		@Override
		public View put(Long key, View view) {
			synchronized (this) {
				// if an view already exists for this key then subtract its size
				final View oldView = super.get(key);
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "put: old = " + oldView + ", new = " + view);
				}
				if (oldView != null) {
					removeView(oldView);
				}
	
				// add the view's size
				addSize(view);
	
				return super.put(key, view);
			}
		}

		@Override
		public View get(Object key) {
			synchronized (this) {
				final View view = super.get(key);
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "get: key = " + key + ", returning " + view);
				}
				return view;
			}
		}

		/**
		 * Increases the size of the cache by the view's size, trimming as needed to keep within the max.
		 */
		private void addSize(View view) {
			if (Logger.IS_DEBUG_ENABLED) {
				final int bytes = getCachedBytes();
				Logger.debug(getClass(), "addSize: before cacheSize = " + cacheSize + ", maxSize = " + maxCacheSize +
					", actual bytes = " + bytes + ", delta = " + (bytes - cacheSize) + ", size = " + size() +
					", adding: " + view);
			}
			cacheSize += getSize(view);
			if (cacheSize > maxCacheSize) {
				trim();
			}
		}

		private void trim() {
			// try removing large views first
			int limit = LARGE_VIEW;
			do {
				// remove loaded views in LRU order until we're below the max
				final Iterator<View> iter = values().iterator();
				while (iter.hasNext()) {
					final View view = iter.next();
					if (getSize(view) >= limit) {
						// remove view and return if that put us below the max
						iter.remove();
						if (cacheSize <= maxCacheSize) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "trim: new cacheSize = " + cacheSize);
							}
							return;
						}
					}
				}

				// now remove small views too
				limit -= LARGE_VIEW;

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "trim: after large views: cacheSize = " + cacheSize);
				}
			} while (limit >= 0);

			Logger.error(getClass(), "trim: cacheSize " + cacheSize + " still above " + maxCacheSize + ": " + super.toString());
		}

		private void removeView(View view) {
			if (Logger.IS_DEBUG_ENABLED) {
				if (!Thread.holdsLock(this)) {
					throw new IllegalStateException("Current thread not holding lock");
				}
			}

			cacheSize -= getSize(view);

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "removeView: cacheSize = " + cacheSize + " after removing " + view);
			}
		}

		private int getSize(View view) {
			int size = 0;
			if (view instanceof ImageView) {
				final Drawable d = ((ImageView)view).getDrawable();
				if (d instanceof BitmapDrawable) {
					size = BitmapManager.getBitmapSize(((BitmapDrawable)d).getBitmap());
				}
			}
//			if (Logger.IS_DEBUG_ENABLED) {
//				Logger.debug(getClass(), "getSize: computed size of " + view + " = " + size);
//			}
			return size + VIEW_SIZE;
		}

		private void updateSize() {
			synchronized (this) {
				cacheSize = getCachedBytes();
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "updateSize: set to " + cacheSize);
				}
				if (cacheSize > maxCacheSize) {
					trim();
				}
			}
		}

		@Override
		public View remove(Object key) {
			synchronized (this) {
				final View view = super.remove(key);
				if (view != null) {
					removeView(view);
				}
				return view;
			}
		}

		private int getCachedBytes() {
			int bytes = 0;
			for (View view : values()) {
				bytes += getSize(view);
			}
			return bytes;
		}
	}

    private static String getLinkFileName(String link) {
        return CACHED_LINK_PREFIX + link.replaceAll("[^a-zA-Z0-9]+", "").toLowerCase() + ".jpeg";
    }


    private static class QueuedJob {
        public static int ACTION_CHECK_FILE = 1;
        public static int ACTION_SAVE_FILE = 2;

        private String url;
        private WebView view;
        private int ACTION;

        private QueuedJob(String url, WebView view, int ACTION) {
            this.url = url;
            this.view = view;
            this.ACTION = ACTION;
        }
    }
    

    private Thread mServiceThread = new Thread(new Runnable() {

        @Override
        public void run() {

            Looper.prepare();
            serviceHandler = new Handler(Looper.myLooper()) {

                @Override
                public void handleMessage(Message msg) {
                    final QueuedJob result = (QueuedJob) msg.obj;
                    final WebView wb = result.view;
                    final String url = result.url;
                    final String filePath = getLinkFileName(url);

                    if (result.ACTION == QueuedJob.ACTION_SAVE_FILE) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(), "Web preview Trying to save file for : " + filePath);
                        }
                        // Immediately after loading the page the image comes blank
                        if (wb.getContentHeight() > 0) {
                            final File output = new File(imagesFolder, filePath);
                            if (output.exists() && output.length() > 2000) {
                                try {
                                    wb.loadUrl("file:///" + output.getAbsolutePath()); // if image is found
                                    wb.setWebViewClient(webViewClient);
                                    if (Logger.IS_DEBUG_ENABLED) {
                                        Logger.debug(getClass(), "Web preview File Already present: "
                                                + output);
                                    }
                                } catch (Exception e) {
                                    if (Logger.IS_DEBUG_ENABLED) {
                                        Logger.debug(getClass(),
                                                "Exception while loading file to web view : " + filePath);
                                        Logger.error(e);
                                    }
                                }
                                return;
                            }

                            boolean fileSaved = false;
                            OutputStream imageFileOS = null;
                            try {
                                // create bitmap from view
                                final int width = itemSize;
                                final Bitmap bmp = bitmapMgr.viewToBitmap(wb, width, width, width / 2, true);
                                if (bmp != null) {
	                                // test if picture is not full black
	                                final int bmpWidth = bmp.getWidth();
	                                bmpTest:
	                                for (int i = 1; i < bmpWidth; i += 5) {
	                                    for (int j = 1; j < bmpWidth; j += 5) {
	                                        final int pixel = bmp.getPixel(i, j);
	                                        if (pixel != 0 && pixel != -1 && pixel != Color.BLACK) {
	                                            // found a valid pixel: save the bitmap
	        	                                if (Logger.IS_DEBUG_ENABLED) {
	        	                                    Logger.debug(getClass(), "Saving Web preview file " + output);
	        	                                }
	                                            final Uri uriSavedImage = Uri.fromFile(output);
	                                            imageFileOS = context.getContentResolver().openOutputStream(uriSavedImage);
	                                            fileSaved = bmp.compress(Bitmap.CompressFormat.JPEG, 90, imageFileOS);
	                                        	break bmpTest;
	                                        }
	                                    }
	                                }
	                                if (Logger.IS_DEBUG_ENABLED) {
	                                    Logger.debug(getClass(), "Web preview file " + output + ": saved = " + fileSaved);
	                                }
                                }
                                else if (Logger.IS_DEBUG_ENABLED) {
                                    Logger.error(getClass(), "Web preview failed to save to bitmap for " + output);
                                }
		                    } catch (Exception e) {
		                        if (Logger.IS_DEBUG_ENABLED) {
		                            Logger.debug(getClass(), "Web preview error on " + output, e);
		                        }
		                    }
                            finally {
                            	if (imageFileOS != null) {
                            		try {
                            			imageFileOS.close();
                            		}
                            		catch (Exception e) {
                            			Logger.error(getClass(), output, e);
                            			output.delete();
                            		}
                            	}
                                if (!fileSaved) {
                                    output.delete();
                                }
                            }
                        } else if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(), filePath + " Web preview  Picture not good to save");
                        }
                    } else {
                        File output = new File(imagesFolder, filePath);
                        if (output.exists() && output.length() > 2000) {
                            // TODO: for now using same Webview so that the view is not regenerated again and
                            // againg later we can replace it with image view or keep same.
                            try {
                                wb.loadUrl("file:///" + output.getAbsolutePath());
                                wb.setWebViewClient(webViewClient);
                            } catch (Exception e) {
                                if (Logger.IS_DEBUG_ENABLED) {
                                    Logger.debug(getClass(),
                                            "Exception while loading file to web view : " + filePath);
                                    Logger.error(e);
                                }
                            }

                        } else {
                            // image not found so load the URL and then cash image.
                            wb.loadUrl(url);

                            // needed to create a new client as we need to save the image with the shared URL
                            // not with the redirected url
                            wb.setWebViewClient(new WebViewClient() {

                                // variable to check if a error occurred then don't save file.
                                boolean mSafeToSaveImage = true;

                                @Override
                                public boolean shouldOverrideUrlLoading(WebView view, String urlNewString) {
                                    if (Logger.IS_DEBUG_ENABLED) {
                                        Logger.debug(this + ": Web preview  view " + view + ": loading: "
                                                + urlNewString);
                                    }
                                    if (view.isShown()) {
                                        mSafeToSaveImage = true;
                                        return false;
                                    } else {
                                        view.stopLoading();
                                        mSafeToSaveImage = false;
                                    }
                                    return true;
                                }

                                @Override
                                public void onReceivedError(WebView view, int errorCode, String description,
                                        String failingUrl) {
                                    mSafeToSaveImage = false;
                                    if (Logger.IS_DEBUG_ENABLED) {
                                        Logger.debug(getClass(), " Web preview Error : " + description
                                                + "  URL:" + failingUrl);
                                    }

                                    super.onReceivedError(view, errorCode, description, failingUrl);
                                }

                                @Override
                                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                                    mSafeToSaveImage = true;
                                    if (Logger.IS_DEBUG_ENABLED) {
                                        Logger.debug(getClass() + " Web preview page started: " + url);
                                    }
                                }

                                @Override
                                public void onPageFinished(WebView view, final String url_loaded) {

                                    if (Logger.IS_DEBUG_ENABLED) {
                                        Logger.debug(getClass() + " Web preview page finished: " + url_loaded);
                                    }
                                    if (mSafeToSaveImage) {
                                        final Message message = Message.obtain();
                                        message.obj = new QueuedJob(url, view, QueuedJob.ACTION_SAVE_FILE);
                                        // using direct url to avoid redrirects file name
                                        serviceHandler.sendMessageDelayed(message, 5000);
                                    }
                                }
                            });
                        }

                    }

                }

            };

            Looper.loop();
        }
    });
}
