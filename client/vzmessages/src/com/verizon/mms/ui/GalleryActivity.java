package com.verizon.mms.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.Media;
import com.verizon.mms.MediaProvider;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.ui.widget.ActionItem;
import com.verizon.mms.ui.widget.QuickAction;
import com.verizon.mms.util.UtilClass;

/**
 * @author animeshkumar
 * 
 */
public class GalleryActivity extends VZMActivity {
	public static final long INVALID = -1L;
	private final static int ACTION_MENU_SAVE_TO_GALLERY = 0;
	private final static int ACTION_MENU_FORWARD_MEDIA = 1;
//	private final static int ACTION_MENU_GO_TO_CONVERSATION = 2;

	private int prevIndex;

	private ImageView mAttachButton;

	// Header
	private RelativeLayout headerView;
	private TextView titleView;
	private TextView dateView;
	// pager
	private ViewPager pagerView;
	private ViewPagerAdapter pagerAdapter;
	private Media currentPagerViewMedia;
	// gallery
	private Gallery galleryView;
	private GalleryAdapter galleryAdapter;
	// gesture
	private GestureDetector gestureDetector;

	NewMMSReceiver myReceiever;
	private static final int slideTime = 1200;
	private Context ctx;
	private Cursor query;

	private ContentObserver observer;

	private static final int RE_QUERY = 1001;
	private Handler handler;
	int pagerPosition = 0;
	static final float PAGER_CACHE_PERCENT = 0.65f; // percent of cache to
													// allocate to pager

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prevIndex = 0;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.gallery);
		ctx = this;
		myReceiever = new NewMMSReceiver();

		mAttachButton = (ImageView) findViewById(R.id.id_option);
		mAttachButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				showGalleryActionMenu();
				return false;
			}
		});
		// view
		headerView = (RelativeLayout) findViewById(R.id.id_bg_header);
		titleView = (TextView) findViewById(R.id.id_image_name);
		dateView = (TextView) findViewById(R.id.id_image_shared_time);
		galleryView = (Gallery) findViewById(R.id.id_gallery);
		pagerView = (ViewPager) findViewById(R.id.awesomepager);

		query();

		// Handler
		handler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case RE_QUERY: {
					// re-query
					query();
					galleryAdapter.changeCursor(query);
					galleryAdapter.notifyDataSetChanged();
					galleryView.invalidate();

					pagerAdapter.changeCursor(query);
					pagerAdapter.notifyDataSetChanged();
					pagerView.invalidate();
				}
				default:
					break;
				}
			}
		};

		// observe
		observer = new CacheObserver(new Handler());
		getContentResolver().registerContentObserver(
				Uri.parse(UtilClass.MEDIA_URI), false, observer);

		// Gallery
		galleryAdapter = new GalleryAdapter(ctx, R.layout.gallery, query,
				query.getColumnNames(), null);

		galleryView.setAdapter(galleryAdapter);

		galleryView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adaptorView, View view,
					int index, long arg3) {
				if (galleryAdapter.getLastClickedPosition() != index) {
					// Move pager view here!
					// Note: don't want smoothScroll. Wont work fine on slower
					// devices.
					pagerView.setCurrentItem(index, false);
					// Set gallery here!
					changeGalleryIndex(index);
				} else {
					Media mms = MediaProvider.Helper
							.fromCursorCurentPosition(galleryAdapter
									.getCursor());
					if (mms.isVideo()) {
						Intent startVideo = new Intent(Intent.ACTION_VIEW, Uri
								.parse(mms.getVideoUri()));
						startVideo
								.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
						try {
							startActivity(startVideo);
						} catch (Exception e) {
							Toast.makeText(GalleryActivity.this,
									getString(R.string.no_media_player),
									Toast.LENGTH_SHORT).show();
						}
					}
				}
			}
		});

		// Gesture
		gestureDetector = new GestureDetector(ctx, new GestureListener());

		// Pager
		pagerAdapter = new ViewPagerAdapter(ctx, gestureDetector, query, this,
				pagerView);

		pagerView.setAdapter(pagerAdapter);

		pagerView.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int index) {
				pagerPosition = index;
				loadCurrentPagerViewMedia(pagerPosition);
				updateTitleAndTime(index);
				// Set gallery here!
				changeGalleryIndex(index);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int index) {
			}
		});

		pagerView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				// log.info("<< onTouch");
				// ((TouchImageView)view).onTouchEvent(e);
				return gestureDetector.onTouchEvent(event);
			}
		});

		// Update title/time for first element
		loadCurrentPagerViewMedia(0);
		updateTitleAndTime(0);
	}

	private void loadCurrentPagerViewMedia(int position) {
		if (query.moveToPosition(position)) {
			currentPagerViewMedia = MediaProvider.Helper
					.fromCursorCurentPosition(query);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(GalleryActivity.class, "==>> pagerPosition = "
						+ pagerPosition + ", currentPagerViewMedia = "
						+ currentPagerViewMedia);
			}
		} else {
			currentPagerViewMedia = null;
		}
	}

	private void query() {
		long threadId = getIntent().getLongExtra("threadid", INVALID);
		String selectionForThread = (threadId != INVALID) ? MediaProvider.Helper.THREAD_ID
				+ " = " + threadId + " AND "
				: "";
		if (DeviceConfig.OEM.isNbiLocationDisabled) {
			query = getContentResolver().query(
					Uri.parse(UtilClass.MEDIA_URI),
					null,
					selectionForThread
							+ SharedContentActivity.photoVideoLocationSelect,
					null, "date desc"); // descending
		} else {
			query = getContentResolver()
					.query(Uri.parse(UtilClass.MEDIA_URI),
							null,
							selectionForThread
									+ SharedContentActivity.photoVideoSelect,
							null, "date desc"); // descending
		}
	}

	@Override
	protected void onResume() {
		init();
		String uri = getIntent().getStringExtra("itemtogo");

		if (uri != null) {
			Cursor q = null;
			try {
				q = getContentResolver().query(Uri.parse(uri), null, null,
						null, null);
				if (q.moveToFirst()) {
					String id = q.getString(q.getColumnIndex("_id"));
					int index = galleryAdapter.getMmsPosition(id);
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(GalleryActivity.class, "itemtogo=" + uri
								+ " _id=" + id);
					}
					changeGalleryIndex(index);
					pagerView.setCurrentItem(index);
					updateTitleAndTime(index);
				}
			} catch (Exception e) {
			} finally {
				if (null != q) {
					q.close();
				}
			}
		}
		if (prevIndex != 0) {
			pagerView.setCurrentItem(prevIndex, false);
			changeGalleryIndex(prevIndex);
			updateTitleAndTime(prevIndex);

		}
		IntentFilter filter = new IntentFilter();
		registerReceiver(myReceiever, filter);
		super.onResume();
	}


	private void showGalleryActionMenu() {
		if (null == currentPagerViewMedia) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(GalleryActivity.class,
						"==>> currentPagerViewMedia is null.");
			}
			return;
		}

		final QuickAction galleryActionMenu = new QuickAction(GalleryActivity.this);
				
		galleryActionMenu.setTitle(getString(R.string.gallery_menu_title));
		ActionItem actionItem = new ActionItem(ACTION_MENU_SAVE_TO_GALLERY,R.string.gallery_save_to_gallery, R.drawable.save_media);		
		galleryActionMenu.addActionItem(actionItem);
		actionItem = new ActionItem(ACTION_MENU_FORWARD_MEDIA,R.string.gallery_forward_media, R.drawable.forward_media);				
		galleryActionMenu.addActionItem(actionItem);

//		 actionItem = new ActionItem(ACTION_MENU_GO_TO_CONVERSATION,
//		 R.string.gallery_go_to_conversation,
//		 R.drawable.ic_menu_friendslist_mod);
//		 galleryActionMenu.addActionItem(actionItem);

		galleryActionMenu
				.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {

						final Media media = currentPagerViewMedia;
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(GalleryActivity.class, "==>> Media = "
									+ media);
						}

						switch (actionId) {
						case ACTION_MENU_SAVE_TO_GALLERY:
							if (media.isImage()) {
								try {
									AssetFileDescriptor videoAsset = getContentResolver()
											.openAssetFileDescriptor(
													Uri.parse(media
															.getImageUri()),
													"r");
									FileInputStream fis = videoAsset
											.createInputStream();
									File root = new File(Environment
											.getExternalStorageDirectory(),
											"VZPics");
									if (!root.exists()) {
										root.mkdirs();
									}
									File file = null;
									if (media.getmPartCt().endsWith("jpeg")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".jpeg");
									} else if (media.getmPartCt().endsWith(
											"bmp")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".bmp");
									} else if (media.getmPartCt().endsWith(
											"png")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".png");
									} else if (media.getmPartCt().endsWith(
											"jpg")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".jpg");
									} else if (media.getmPartCt().endsWith(
											"gif")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".gif");
									}

									FileOutputStream fos = new FileOutputStream(
											file);
									Uri uri = Uri.fromFile(file);

									byte[] buf = new byte[1024];
									int len;
									while ((len = fis.read(buf)) > 0) {
										fos.write(buf, 0, len);
									}
									fis.close();
									fos.close();
									sendBroadcast(new Intent(
											Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
											uri));
									showSavedToast();
								} catch (Exception e) {
									showFailedToast();
								}
							} else {
								try {
									AssetFileDescriptor videoAsset = getContentResolver()
											.openAssetFileDescriptor(
													Uri.parse(media
															.getVideoUri()),
													"r");
									FileInputStream fis = videoAsset
											.createInputStream();
									File root = new File(Environment
											.getExternalStorageDirectory(),
											"VZVideos");
									if (!root.exists()) {
										root.mkdirs();
									}
									File file = null;
									if (media.getmPartCt().endsWith("3gpp")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".3gp");
									} else if (media.getmPartCt().endsWith(
											"h263-2000")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".mp4");
									} else if (media.getmPartCt().endsWith(
											"mp4v-es")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".mp4");
									} else if (media.getmPartCt().endsWith(
											"h263")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".mp4");
									} else if (media.getmPartCt().endsWith(
											"h264")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".mp4");
									} else if (media.getmPartCt().endsWith(
											"3gpp2")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".3g2");
									} else if (media.getmPartCt().endsWith(
											"mp4")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".mp4");
									} else if (media.getmPartCt().endsWith(
											"mpeg")) {
										file = new File(root, media.getMId()
												+ media.getmPartId() + ".aac");
									}

									FileOutputStream fos = new FileOutputStream(
											file);
									Uri uri = Uri.fromFile(file);

									byte[] buf = new byte[1024];
									int len;
									while ((len = fis.read(buf)) > 0) {
										fos.write(buf, 0, len);
									}
									fis.close();
									fos.close();
									sendBroadcast(new Intent(
											Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
											uri));
									showSavedToast();
								} catch (Exception e) {
									showFailedToast();
								}
							}
							break;

						case ACTION_MENU_FORWARD_MEDIA:
							forwardMessage(media);
							break;

//						case ACTION_MENU_GO_TO_CONVERSATION:
//							 Media mms1 =
//							 MediaProvider.Helper.fromCursorCurentPosition(galleryAdapter.getCursor());
//							 finish();
//							 Intent intnet =
//							 ComposeMessageActivity.createIntent(ctx,
//							 mms1.getThreadId());
//							 intnet.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//							 startActivity(intnet);
//							break;
						}
					}
				});

		galleryActionMenu.show(mAttachButton, findViewById(R.id.id_main_view),
				false);
	}

	private void showSavedToast() {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.savedtoast,
				(ViewGroup) findViewById(R.id.toast_layout_root));

		Toast toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.setView(layout);
		toast.show();
	}

	private void showFailedToast() {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.failedtoast,
				(ViewGroup) findViewById(R.id.toast_layout_root));

		Toast toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		toast.setDuration(Toast.LENGTH_SHORT);
		toast.setView(layout);
		toast.show();
	}

	private void forwardMessage(Media mms) {
		Intent i = ComposeMessageActivity.createIntent(this, 0, false);
		i.putExtra(ComposeMessageActivity.FORWARD_MESSAGE, true);
		if (mms.isVideo()) {
			i.putExtra("forwarduri", mms.getVideoUri());
			i.putExtra("isVideo", true);
		} else {
			if (mms.isLocation() && DeviceConfig.OEM.isNbiLocationDisabled) {
				i.putExtra("forwarduri", mms.getLocationUri());
			} else {
				i.putExtra("forwarduri", mms.getImageUri());
			}
			i.putExtra("isVideo", false);
		}
		i.putExtra("subject", getString(R.string.forward_prefix));
		finish();
		startActivity(i);
	}

	private void init() {
		if (query == null || query.isClosed()) {
			query();
			// query = getContentResolver().query(
			// Uri.parse(UtilClass.MEDIA_URI),
			// null,
			// SharedContentActivity.photoVideoSelect,
			// new String[] {Long.toString(getIntent().getLongExtra("threadid",
			// 0))}, "date desc"); // descending

			galleryAdapter = new GalleryAdapter(ctx, R.layout.gallery, query,
					query.getColumnNames(), null);

			pagerAdapter = new ViewPagerAdapter(ctx, gestureDetector, query,
					this, pagerView);

			galleryView.setAdapter(galleryAdapter);

			pagerView.setAdapter(pagerAdapter);

			// Update title/time for first element
			updateTitleAndTime(0);
		}
	}

	public void clickCallback(View v) {
		if (galleryAdapter.getLastClickedPosition() == pagerView
				.getCurrentItem()) {
			galleryAdapter.getCursor().moveToPosition(
					galleryAdapter.getLastClickedPosition());
			Media mms = MediaProvider.Helper
					.fromCursorCurentPosition(galleryAdapter.getCursor());
			if (mms.isVideo()) {
				Intent startVideo = new Intent(Intent.ACTION_VIEW,
						Uri.parse(mms.getVideoUri()));
				startVideo.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				try {
					startActivity(startVideo);
				} catch (Exception e) {
					Toast.makeText(this, getString(R.string.no_media_player),
							Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	protected void onPause() {
		prevIndex = pagerView.getCurrentItem();
		unregisterReceiver(myReceiever);
		super.onPause();
	}

	@Override
	protected void onStop() {
		galleryAdapter.changeCursor(null);
		pagerAdapter.changeCursor(null);
		query = null;
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// log.info("### Destroying ###");
		galleryView.setAdapter(null);
		pagerView.setAdapter(null);

		// observer
		if (null != observer) {
			getContentResolver().unregisterContentObserver(observer);
			observer = null;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// clear the cache and redraw to optimize image sizes for the current
		// orientation
		if (pagerAdapter != null) {
			pagerAdapter.onConfigurationChanged(newConfig);
			final int pos = pagerView.getCurrentItem();
			pagerView.setAdapter(pagerAdapter);
			if (pos >= 0) {
				pagerView.setCurrentItem(pos, false);
			}
		}
	}

	private void updateAdaptors() {
		galleryAdapter.notifyDataSetChanged();
		pagerAdapter.notifyDataSetChanged();
	}

	private void changeGalleryIndex(int position) {
		// galleryView.invalidate(); No need to invalidate the view. Wondering
		// why we did this in the first place?
		galleryAdapter.setLastClickedPosition(position);
		galleryView.setAdapter(galleryAdapter);
		galleryView.setSelection(position);
	}

	private void updateTitleAndTime(int index) {
		titleView.setText(R.string.loading);
		dateView.setText("");
		// if (query.moveToPosition(index)) {
		// Media mms = MediaProvider.Helper.fromCursorCurentPosition(query);
		if (currentPagerViewMedia != null) {
			final Media mms = currentPagerViewMedia;
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(GalleryActivity.class,
						"===>> updateTitleAndTime ==> index=" + index
								+ ", media=" + mms);
			}
			// At times, cList might be empty. bug: 1158
			ContactList cList = ContactList.getByNumbers(mms.getAddress(),
					false, false);
			if (cList.size() > 0) {
				Contact contact = cList.get(0);
				String sharedName = contact.getName();
				if (sharedName.equals(getString(R.string.me))
						|| sharedName.equals("Me")) {
					sharedName = getString(R.string.me);
				}
				titleView
						.setText(getString(R.string.sharedPerson) + sharedName);
			} else {
				titleView.setText(getString(R.string.sharedPerson)
						+ mms.getAddress());
			}
			dateView.setText(MessageUtils.formatTimeStampString(mms.getDate(),
					true));
		}
	}

	private void toggleGalleryVisibility() {
		int visibility = galleryView.getVisibility();
		int dp104 = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 104, ctx.getResources()
						.getDisplayMetrics());
		if (visibility == View.VISIBLE) {
			TranslateAnimation slide = new TranslateAnimation(0, 0, 0, dp104);// translate
																				// gallery
																				// down
			slide.setDuration(slideTime);
			slide.setFillAfter(true);
			galleryView.startAnimation(slide);
			galleryView.setVisibility(View.INVISIBLE);
		} else {
			TranslateAnimation slide = new TranslateAnimation(0, 0, dp104, 0);// translate
																				// it
																				// back
																				// up
			slide.setDuration(slideTime);
			slide.setFillAfter(true);
			galleryView.startAnimation(slide);
			galleryView.setVisibility(View.VISIBLE);
		}
	}

	private void toggleHeaderVisibility() {
		int dp104 = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 104, ctx.getResources()
						.getDisplayMetrics());
		int visibility = headerView.getVisibility();
		if (visibility == View.VISIBLE) {
			TranslateAnimation slide = new TranslateAnimation(0, 0, 0, -dp104);// translate
																				// gallery
																				// down
			slide.setDuration(slideTime);
			slide.setFillAfter(true);
			headerView.startAnimation(slide);
			headerView.setVisibility(View.INVISIBLE);
		} else {
			TranslateAnimation slide = new TranslateAnimation(0, 0, -dp104, 0);// translate
																				// it
																				// back
																				// up
			slide.setDuration(slideTime);
			slide.setFillAfter(true);
			headerView.startAnimation(slide);
			headerView.setVisibility(View.VISIBLE);
		}
	}

	// @Override
	// public void onBackPressed() {
	// Intent setIntent = new Intent(Intent.ACTION_MAIN);
	// setIntent.addCategory(Intent.CATEGORY_HOME);
	// setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// startActivity(setIntent);
	// }

	class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			toggleGalleryVisibility();
			toggleHeaderVisibility();
			return true;
		}
	}

	class NewMMSReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(),
						">>>>>>>> RECEIVED BROADCAST >>>>>>>>>>");
			}
			updateAdaptors();
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
				Logger.debug(getClass(), "=> Dataset Changed, updating view!");
			}

			if (!handler.hasMessages(RE_QUERY)) {
				Message requery = handler.obtainMessage(RE_QUERY);
				handler.sendMessageDelayed(requery, MmsConfig.getFMSBInterval());
			}

		}
	}

}