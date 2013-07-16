package com.verizon.mms.ui;

import static com.verizon.mms.ui.GalleryItem.TYPE_ALL;
import static com.verizon.mms.ui.GalleryItem.TYPE_CONTACT;
import static com.verizon.mms.ui.GalleryItem.TYPE_IMAGE;
import static com.verizon.mms.ui.GalleryItem.TYPE_LINK;
import static com.verizon.mms.ui.GalleryItem.TYPE_LOCATION;
import static com.verizon.mms.ui.GalleryItem.TYPE_VIDEO;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.AccCallback;
import com.verizon.mms.Callback;
import com.verizon.mms.ContentType;
import com.verizon.mms.Media;
import com.verizon.mms.MediaCacheApi;
import com.verizon.mms.MediaProvider;
import com.verizon.mms.MediaSyncService;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.Util;
import com.verizon.mms.util.UtilClass;

public class TabletSharedContentFragment extends Fragment implements OnClickListener {
	private View gallery;
	private View noContent;
	private View progressView;
	private View btnPanel;
	private View btnConv;
	private View updating;
	private ListView galleryList;
    private long thread;
    private String[] members;
    private MediaServiceConnection serviceConnection;
    private ContactList contacts;
    private int mediaTypes;
    private int mediaFilter;
    private Activity mActivity;
	private ContentResolver mResolver;
    private TabletGalleryListener mTabletGalleryListener;
    private TabletSharedContentAdapter tabletSharedContentAdapter;
	private ArrayList<GalleryItem> items;
	private Object dataLock = new Object();
	private ArrayList<GalleryRowData> rows;
	private boolean isRunningFirstTime = true;

	private static final int MSG_UPDATE = 1;
	private static final int MSG_UPDATE_CALLBACK = 2;
	private static final int MSG_FILTER = 3;

	private static final long UPDATE_DELAY = 1000;

	static final int WEIGHT_TOTAL = 6;  // total weight of all cells in a row
    static final int WEIGHT_ONE_THIRD = WEIGHT_TOTAL / 3;
    static final int WEIGHT_TWO_THIRDS = WEIGHT_TOTAL * 2 / 3;
    static final int WEIGHT_ONE_HALF = WEIGHT_TOTAL / 2;

    private static final int MIN_HD_RES = 1024 * 768;
    public static final long INVALID = -1L;

    private static String WHERE_ALL =
            MediaProvider.Helper.M_PART_CT + " NOT NULL AND " +
            MediaProvider.Helper.M_PART_CT + " != '" + ContentType.TEXT_PLAIN + "'";

    private static String WHERE_THREAD = WHERE_ALL + " AND " +  MediaProvider.Helper.THREAD_ID + " = ?";

    private static final int FILTER_IMAGE    = TYPE_IMAGE | TYPE_VIDEO;
    private static final int FILTER_LOCATION = TYPE_LOCATION;
    private static final int FILTER_LINK     = TYPE_LINK;
    private static final int FILTER_CONTACT  = TYPE_CONTACT;
    private static final int FILTER_ALL      = TYPE_ALL;


	private enum Filter {
		IMAGE(FILTER_IMAGE, R.id.btn_gallery_photos_videos),
		LOCATION(FILTER_LOCATION, R.id.btn_gallery_locations),
		LINK(FILTER_LINK, R.id.btn_gallery_links),
		CONTACT(FILTER_CONTACT, R.id.btn_gallery_contact_info),
		ALL(FILTER_ALL, R.id.btn_gallery_all);

		private int filter;
		private int btnId;
		private View button;

		private Filter(int filter, int btnId) {
			this.filter = filter;
			this.btnId = btnId;
		}
	}


	private static class ItemData {
		private ArrayList<GalleryItem> items;
		private int mediaTypes;

		private ItemData(ArrayList<GalleryItem> items, int mediaTypes) {
			this.items = items;
			this.mediaTypes = mediaTypes;
		}
	}


	public interface TabletGalleryListener {
        public void launchComposeView(Intent mIntent);
        public void onDeleteGoToNext(long threadId);
        public void updateCurrentThreadID(long threadId, String calledFrom);
        public void setGalleryLoaded(boolean enable);
        public void launchShareMenu();
    }


    public TabletSharedContentFragment() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "no-arg ctor");
		}
    }

    public TabletSharedContentFragment(TabletGalleryListener tabletGalleryListner) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "listener ctor");
		}
        mTabletGalleryListener = tabletGalleryListner;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        mResolver = activity.getContentResolver();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

	@Override
	public void onClick(View v) {
		final int id = v.getId();
		switch (id) {
			case R.id.imgConversation:
				v.setEnabled(false);
				mTabletGalleryListener.setGalleryLoaded(false);
				break;

			case R.id.startsharing:
				v.setEnabled(false);
				mTabletGalleryListener.setGalleryLoaded(false);
				mTabletGalleryListener.launchShareMenu();
				break;

			default:
				for (Filter filter : Filter.values()) {
					if (filter.btnId == id) {
						filter.button.setEnabled(false);

						// allow UI to refresh the button state before data is changed
						final Message msg = Message.obtain(handler, MSG_FILTER);
						msg.arg1 = filter.filter;
						handler.sendMessage(msg);
					}
					else {
						filter.button.setEnabled(true);
					}
				}
				break;
		}
	}

	/**
	 * Filter the item data by the currently set filter and organize it into a set
	 * of displayable rows based on the display criteria.
	 * NB assumes caller is holding the dataLock.
	 * @return Filtered display rows
	 */
	private ArrayList<GalleryRowData> getRows() {
		long start;
		if (Logger.IS_DEBUG_ENABLED) {
			start = SystemClock.uptimeMillis();
		}
		final ArrayList<GalleryRowData> rows = new ArrayList<GalleryRowData>();
		final int filter = mediaFilter;
		final boolean showingImages = (filter & FILTER_IMAGE) != 0;
		int totalWeight = 0;
		long rowId = 1;
		GalleryRowData grd = new GalleryRowData(1);
		final int lastItem = items.size() - 1;
		for (int i = 0; i <= lastItem; ++i) {
			final GalleryItem item = items.get(i);

			// filter by type
			final int type = item.type;
			if ((type & filter) != 0) {
				final Media media = item.media;
				int weight = WEIGHT_ONE_THIRD;  // all non-images take a one-third cell

				if (totalWeight < WEIGHT_TWO_THIRDS && showingImages) {
					// if we're left with a one-half cell then this item must be a high-res image
					if (totalWeight == WEIGHT_ONE_HALF) {
						weight = WEIGHT_ONE_HALF;
					}
					else if (isHighResImage(type, media)) {
						// high-res images take a half cell if the next is a high-res, otherwise two-thirds
						weight = WEIGHT_TWO_THIRDS;
						if (totalWeight == 0) {
							// check next matching item
							for (int j = i + 1; j < lastItem; ++j) {
								final GalleryItem next = items.get(j);
								final int nextType = next.type;
								if ((nextType & filter) != 0) {
									if (isHighResImage(nextType, next.media)) {
										weight = WEIGHT_ONE_HALF;
									}
									break;
								}
							}
						}
					}
				}

				item.weight = weight;
				totalWeight += weight;

				// add the item to the current row
				grd.add(item);

				// if we've filled up the current row then add it and create a new one
				if (totalWeight >= WEIGHT_TOTAL) {
					rows.add(grd);
					grd = new GalleryRowData(++rowId);
					totalWeight = 0;
				}
			}
		}

		if (grd.count > 0) {
			rows.add(grd);
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "getRows: filtered to " + rows.size() + " rows by filter " +
				Integer.toHexString(mediaFilter) + " in " + (SystemClock.uptimeMillis() - start) + "ms");
		}

		return rows;
	}

	private boolean isHighResImage(int type, Media media) {
		return (type & FILTER_IMAGE) != 0 && media.getWidth() * media.getHeight() >= MIN_HD_RES;
	}

	private void initView() {
        updateHeader();
        updateGalleryView(null);
        final Message msg = Message.obtain(handler, MSG_UPDATE);
        handler.sendMessage(msg);
    }


	private class FetchTask extends AsyncTask<Void, Void, ItemData> {
        @Override
        protected void onPreExecute() {
        };

        @Override
        protected ItemData doInBackground(Void... args) {
        	// get the data and display it if it's changed or we aren't showing any
   			final ItemData data = getItems(thread);
   			final boolean changed = itemsChanged(data.items);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("TabletSharedContentFragment: doInBackground: changed = " + changed);
            }
   			return changed || updating.getVisibility() == View.VISIBLE ? data : null;
        }

		@Override
		protected void onPostExecute(ItemData result) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("TabletSharedContentFragment: onPostExecute: result = " + result);
            }
			if (result != null) {
				// changed items to display
				synchronized (dataLock) {
					items = result.items;
					mediaTypes = result.mediaTypes;
					updateGalleryView(getRows());
				}
			}
		}
    }


    private boolean itemsChanged(ArrayList<GalleryItem> newItems) {
    	boolean changed = true;
    	final ArrayList<GalleryItem> oldItems = items;
    	if (oldItems != null) {
    		final int num = oldItems.size();
    		if (num == newItems.size()) {
    			for (int i = 0; i < num; ++i) {
    				if (!oldItems.get(i).equal(newItems.get(i))) {
    					return true;
    				}
    			}
    			changed = false;
    		}
    	}
		return changed;
	}

	private void updateHeader() {
        // the view has been destroyed and the ui is trying to get updated from bg.
        if (null == getView()) {
            return;
        }
		final FromTextView titleView = (FromTextView)getView().findViewById(R.id.sharedtitletext);
		if (thread == INVALID) {
			titleView.setText(getString(R.string.allandme));
		}
		else {
			final StringBuilder membersConcated = new StringBuilder();
			if (members != null) {
				for (int i = 0; i < members.length; i++) {
					if (i > 0) {
						membersConcated.append(";");
					}
					membersConcated.append(members[i]);
				}
				contacts = ContactList.getByNumbers(membersConcated.toString(), false, true);
				titleView.setSuffix(getString(R.string.andme));
				titleView.setNames(contacts);
			}
		}
		final TextView titlesubtext = (TextView)getView().findViewById(R.id.titlesubtext);
		titlesubtext.setText(R.string.sharedcontent);
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View frag = inflater.inflate(R.layout.shared_gallery_contentscreen, container, false);

        gallery = frag.findViewById(R.id.tabletgallery);
        noContent = frag.findViewById(R.id.tabletgallery_nocontent);
        btnPanel = frag.findViewById(R.id.tabletgallery_bottom_panel);
        updating = frag.findViewById(R.id.updating);

        btnConv = frag.findViewById(R.id.imgConversation);
        btnConv.setVisibility(View.VISIBLE);
        btnConv.setOnClickListener(this);
        btnConv.setEnabled(true);

        return frag;
    }

    @Override
    public void onStart() {
        Util.forceHideKeyboard(getActivity(), getView());
        onRestart(getActivity());
        
        // set text again to take effect of language change
		for (Filter filter : Filter.values()) {
			final Button button = (Button) getView().findViewById(filter.btnId);
			if (filter.filter == FILTER_IMAGE) {
				button.setText(R.string.gallery_photos_videos);
			} else if (filter.filter == FILTER_CONTACT) {
				button.setText(R.string.gallery_contact_info);
			} else if (filter.filter == FILTER_LINK) {
				button.setText(R.string.gallery_links);
			} else if (filter.filter == FILTER_LOCATION) {
				button.setText(R.string.gallery_locations);
			} else if (filter.filter == FILTER_ALL) {
				button.setText(R.string.gallery_all);
			}
		}
        super.onStart();
    }

    public void onRestart(Activity act) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onRestart");
            Util.dumpFragmentState(getFragmentManager(), "TabletSharedContentFragment: fragment state = ", "  ");
        }

        onStopStub();

        mActivity = act;
        final Intent intent = act.getIntent();
        long threadid = thread;
        thread = intent.getLongExtra("thread_id", -1L);
        members = intent.getStringArrayExtra("members");
        if (threadid != thread)
            mediaFilter = FILTER_ALL;
        // display loading screen and start fetching data
        initView();
        isRunningFirstTime = true;
        // bind to media service
        serviceConnection = new MediaServiceConnection(thread);
        mActivity.startService(new Intent(MediaSyncService.class.getName()));
        mActivity.bindService(new Intent(MediaSyncService.class.getName()), serviceConnection, 0);

        mTabletGalleryListener.updateCurrentThreadID(thread, this.getClass().toString());
    }

    public void onStopStub() {
        if (null != serviceConnection) {
            serviceConnection.unregisterCallbacks();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "onStopStub: unbinding MediaSyncService");
            }
            mActivity.unbindService(serviceConnection);
            serviceConnection = null;
        }
        final TabletSharedContentAdapter adapter = tabletSharedContentAdapter;
        if (adapter != null) {
        	adapter.shutdown();
        	final ListView list = galleryList;
        	if (list != null) {
        		list.setAdapter(null);
        	}
        }
    }

    @Override
    public void onStop() {
        onStopStub();
        super.onStop();
    }

    /**
     * Display the given data or the appropriate status.
     * @param rows Data rows to display or null to display "fetching" status;
     *             if rows is empty then "no data" status is displayed
     */
	private void updateGalleryView(ArrayList<GalleryRowData> rows) {
		this.rows = rows;
		View fragmentView = getView();
        // the view has been destroyed and the ui is trying to get updated from bg.
        if (null == fragmentView) {
            return;
        }
		final TextView tv = (TextView) fragmentView.findViewById(R.id.updating_text);
		tv.setText(R.string.updating_content);
		if (rows != null && rows.size() > 0) {
		    updating.setVisibility(View.GONE);
			// display data in list
			gallery.setVisibility(View.VISIBLE);
			noContent.setVisibility(View.GONE);

			initFilterButtons(fragmentView);

			// TODO update existing adapter instead of re-creating it
			if (tabletSharedContentAdapter != null) {
				tabletSharedContentAdapter.shutdown();
			}
			tabletSharedContentAdapter = new TabletSharedContentAdapter(thread, getActivity(), rows);
			galleryList = (ListView)fragmentView.findViewById(R.id.tabletgallery_list);
			galleryList.setAdapter(tabletSharedContentAdapter);
			galleryList.setFastScrollEnabled(true);
		}
		else {
			// display status screen
			gallery.setVisibility(View.GONE);
			noContent.setVisibility(View.VISIBLE);

			final TextView descView = (TextView)fragmentView.findViewById(R.id.screen_description);
			final Button startSharing = (Button)fragmentView.findViewById(R.id.startsharing);
			if (rows != null && !isRunningFirstTime) {
			    updating.setVisibility(View.GONE);
				// no data

				startSharing.setVisibility(View.VISIBLE);
				startSharing.setEnabled(true);
				startSharing.setOnClickListener(this);
				startSharing.setText(R.string.btn_start_sharing);
                if (thread == INVALID) {
                    descView.setText(getString(R.string.nosharedcontent, getString(R.string.fmsb_all)));
                } else {
                    descView.setText(getString(R.string.nosharedcontent, contacts.formatNames()));
                }
			}
			else {
                updating.setVisibility(View.VISIBLE);
                gallery.setVisibility(View.VISIBLE);
                galleryList = (ListView)fragmentView.findViewById(R.id.tabletgallery_list);
                galleryList.setAdapter(null);
                noContent.setVisibility(View.GONE);
			}
		}
	}

    private void initFilterButtons(View parent) {
    	final int mediaTypes = this.mediaTypes;
    	final int mediaFilter = this.mediaFilter;
    	int numButtons = 0;
		for (Filter filter : Filter.values()) {
			// if this filter's media is present then set up its button, otherwise disappear it
			final View button = filter.button = parent.findViewById(filter.btnId);
			if ((filter.filter & mediaTypes) != 0) {
				button.setVisibility(View.VISIBLE);
				button.setEnabled(filter.filter != mediaFilter);
				button.setOnClickListener(this);
				++numButtons;
			}
			else {
				button.setVisibility(View.GONE);
			}
		}

		// if there is only one media type then there's no need for the buttons
		btnPanel.setVisibility(numButtons <= 2 ? View.GONE : View.VISIBLE);
	}


	class MediaServiceConnection implements ServiceConnection {
        private MediaCacheApi api;
        private long thread;

        public MediaServiceConnection(long thread) {
            this.thread = thread;
        }

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			api = MediaCacheApi.Stub.asInterface(service);
			try {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug("TabletSharedContentFragment: onServiceConnected: thread = " + thread);
				}

				if (thread == INVALID) {
					api.accCache(new AccCallback.Stub() {
					    long counts =0;
						@Override
						public void onComplete(final long threadId, final long count) throws RemoteException {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug("TabletSharedContentFragment: onComplete: threadId = " + threadId + ", count = " + count+", counts = "+counts);
							}
							
                            // INVALID means the cache update is complete
                            if (threadId == INVALID) {
                                update(counts); // count of
                                counts = 0;
                            } else {
                                counts = counts + count;
                            }
						}
					});
				}
				else {
					api.cache(thread, new Callback.Stub() {
						@Override
						public void onComplete(final long count) throws RemoteException {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug("TabletSharedContentFragment: onComplete: count = " + count);
							}
							update(count);
						}
					});
				}
			}
			catch (Exception e) {
				Logger.error(TabletSharedContentFragment.class, e);
			}
		}

		private void update(long count) {
			// check for changed data if media service indicated there were updates
			if (count > 0 || isRunningFirstTime) {
			    isRunningFirstTime = false;
				final Message msg = Message.obtain(handler, MSG_UPDATE);
				msg.arg1 = (int)count;
				handler.sendMessage(msg);
			}
		}

		@Override
        public void onServiceDisconnected(ComponentName name) {
            api = null;
        }

        public void unregisterCallbacks() {
            if (null != api) {
                try {
                    api.unregisterCallback(thread);
                } catch (Exception e) {
                    Logger.error(TabletSharedContentFragment.class, e);
                }
            }
        }
    }


	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("TabletSharedContentFragment: handleMessage: msg = " + msg);
			}
			final int what = msg.what;
			if (what == MSG_UPDATE) {
				// display the updating message and call back in a bit to perform the update
				updating.setVisibility(View.VISIBLE);
				removeMessages(MSG_UPDATE_CALLBACK);
				sendEmptyMessageDelayed(MSG_UPDATE_CALLBACK, UPDATE_DELAY);
			}
			else if (what == MSG_UPDATE_CALLBACK) {
		        new FetchTask().execute();
			}
			else if (what == MSG_FILTER) {
				// set filter and update
				synchronized (dataLock) {
					mediaFilter = msg.arg1;
					updateGalleryView(getRows());
				}
			}
		}
	};

    private ItemData getItems(long thread) {
		ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();
		int mediaTypes = 0;

		final String where;
		final String[] args;
		if (thread != INVALID) {
			where = WHERE_THREAD;
			args = new String[] { Long.toString(thread) };
		}
		else {
			where = WHERE_ALL;
			args = null;
		}

		final Cursor query = SqliteWrapper.query(mActivity, mResolver, Uri.parse(UtilClass.MEDIA_URI),
			null, where, args, "date desc");

		if (query != null) {
			try {
				long itemId = 1;
				while (query.moveToNext()) {
                    isRunningFirstTime = false;
					final Media media = MediaProvider.Helper.fromCursorCurentPosition(query);
					final GalleryItem item = new GalleryItem(itemId++, media);
					items.add(item);
					mediaTypes |= item.type;
				}
			}
			catch (Exception e) {
				Logger.error(getClass(), e);
			}
			finally {
				query.close();
			}
		}

		return new ItemData(items, mediaTypes);
	}

    @Override
    public void onConfigurationChanged(Configuration newConfig) {       
    	initView();
        MessagingPreferenceActivity.setLocale(mActivity.getBaseContext());
        super.onConfigurationChanged(newConfig);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            mTabletGalleryListener.setGalleryLoaded(false);
            return true;

        }
        return false;
    }
}
