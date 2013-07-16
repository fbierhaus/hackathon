package com.verizon.mms.ui;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ContentType;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.Media;
import com.verizon.mms.MediaProvider;
import com.verizon.mms.MmsException;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.pdu.PduBody;
import com.verizon.mms.pdu.PduPart;
import com.verizon.mms.util.BitmapCache;
import com.verizon.mms.util.BitmapManager;
import com.verizon.mms.util.BitmapManager.OnBitmapLoaded;
import com.verizon.mms.util.EnableDisableViewPager;
import com.verizon.mms.util.TouchImageView;

public class ViewPagerAdapter extends PagerAdapter implements OnBitmapLoaded {
	private final BitmapManager bitmapMgr;
	private final Context ctx;
	private final GestureDetector gestureDetector;
	private int count;
	private Cursor c;
	private Activity clickCallback;
	private int dp100;
	private int dp300;
	private long cacheSize;
	private BitmapCache bitmapCache;
	private EnableDisableViewPager mDisableViewPager;

	private static final int INITIAL_CAPACITY = 10;

	public ViewPagerAdapter(Context ctx, GestureDetector gestureDetector,
			Cursor c, Activity a, ViewPager mViewPager) {
		bitmapMgr = BitmapManager.INSTANCE;
		this.ctx = ctx;
		mDisableViewPager = (EnableDisableViewPager) mViewPager;
		this.gestureDetector = gestureDetector;
		changeCursor(c);
		clickCallback = a;

		dp100 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				100, ctx.getResources().getDisplayMetrics());
		dp300 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				300, ctx.getResources().getDisplayMetrics());

		initCache();
	}

	private void initCache() {
		if (cacheSize == 0) {
			// we split the user bitmap cache between the pager and gallery
			// adapters
			final long userCache = bitmapMgr.getUserCacheSize("GalleryPager");
			cacheSize = (long) ((float) userCache * GalleryActivity.PAGER_CACHE_PERCENT);
		}
		bitmapCache = new BitmapCache("GalleryPager", cacheSize,
				INITIAL_CAPACITY);
	}

	@Override
	public void onBitmapLoaded(String url, Bitmap bmp, ImageView imageView) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "onBitmapLoaded: url = " + url);
		}
		if (bitmapCache != null && bmp != null) {
			bitmapCache.putBitmap(url, bmp);
		}
	}

	public void changeCursor(Cursor c) {
		this.c = c;
		if (c == null) {
			if (bitmapCache != null) {
				// Fix: http://50.17.243.155/bugzilla/show_bug.cgi?id=1967
				bitmapCache.clear();
				// bitmapCache.shutdown();
				// bitmapCache = null;
			}
			count = 0;
		} else {
			count = c.getCount();
		}
	}

	@Override
	public int getCount() {
		return count;
	}

	@Override
	public Object instantiateItem(View collection, int position) {
		Media mms = get(position);
		if (null == mms) {
			return null;
		}

		View layout = null;

		// load video
		if (mms.isVideo()) {
			final BorderLayout borderLayout = new BorderLayout(ctx);
			layout = borderLayout;
			final Bitmap bmp = bitmapCache.getBitmap(mms.getVideoUri());
			if (bmp != null) {
				borderLayout.imageView.setImageBitmap(bmp);
			} else {
				BitmapManager.INSTANCE.loadVideoThumbnail(mms.getVideoUri(),
						borderLayout.imageView, BitmapManager.UNKNOWN,
						BitmapManager.UNKNOWN, this);
			}
			((ViewPager) collection).addView(borderLayout, 0);

			borderLayout.overlayView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					((GalleryActivity) clickCallback).clickCallback(v);
				}
			});
		} else if (mms.isImage()) {
			if ("image/gif".equalsIgnoreCase(mms.getmPartCt())) {
				final BorderLayout borderLayout = new BorderLayout(ctx);
				layout = borderLayout;
				final Bitmap bmp = bitmapCache.getBitmap(mms.getImageUri());
				if (bmp != null) {
					borderLayout.imageView.setImageBitmap(bmp);
				} else {
					BitmapManager.INSTANCE.loadBitmap(mms.getImageUri(),
							borderLayout.imageView, BitmapManager.UNKNOWN,
							BitmapManager.UNKNOWN, false, this);
				}
				((ViewPager) collection).addView(borderLayout, 0);

				final String uri = mms.getImageUri();
				borderLayout.overlayView
						.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								Intent startVideo = new Intent(
										Intent.ACTION_VIEW, Uri.parse(uri));
								startVideo
										.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
								try {
									clickCallback.startActivity(startVideo);
								} catch (Exception e) {
								}
							}
						});
			} else {
				// For 2.2 with onPich scaling
				TouchImageView img = new TouchImageView(ctx, gestureDetector,
						mDisableViewPager);
				layout = img;
				final Bitmap bmp = bitmapCache.getBitmap(mms.getImageUri());
				if (bmp != null) {
					img.setImageBitmap(bmp);
				} else {
					BitmapManager.INSTANCE.loadBitmap(mms.getImageUri(), img,
							BitmapManager.UNKNOWN, BitmapManager.UNKNOWN,
							false, this);
				}
				((ViewPager) collection).addView(img, 0);
			}
		} else if (mms.isLocation() && DeviceConfig.OEM.isNbiLocationDisabled) {

			PduBody body = PduBodyCache.getPduBody(ctx, ContentUris
					.withAppendedId(VZUris.getMmsUri(), mms.getMId()));
			if (body != null) {
				Uri URI = null;
				int partNum = body.getPartsNum();
				for (int i = 0; i < partNum; i++) {
					PduPart part = body.getPart(i);
					String type = new String(part.getContentType());
					if (ContentType.isImageType(type)
							|| ContentType.isVideoType(type)
							|| ContentType.isAudioType(type)) {
						URI = part.getDataUri();
					}
				}

				TouchImageView img = new TouchImageView(ctx, gestureDetector,
						mDisableViewPager);
				layout = img;
				
				if (URI != null) {
					final Bitmap bmp = bitmapCache.getBitmap(URI.toString());
					if (bmp != null) {
						img.setImageBitmap(bmp);
					} else {
						BitmapManager.INSTANCE.loadBitmap(URI.toString(), img,
								BitmapManager.UNKNOWN, BitmapManager.UNKNOWN,
								false, this);
					}
				} else {
					img.setImageResource(R.drawable.attach_location);
				}
				((ViewPager) collection).addView(img, 0);
			}
		}

		return layout;
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

	private class BorderLayout extends RelativeLayout {
		private ImageView imageView;
		private ImageView overlayView;

		public BorderLayout(Context context) {
			super(context);
			imageView = new ImageView(ctx);
			Drawable overlay = ctx.getResources().getDrawable(
					R.drawable.btn_play_gallery);
			overlayView = new ImageView(ctx);
			overlayView.setImageDrawable(overlay);
			LayoutParams centerParams = new LayoutParams(dp100, dp100);
			centerParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			LayoutParams imageParams = new LayoutParams(
					LayoutParams.FILL_PARENT, dp300);
			imageParams.addRule(RelativeLayout.CENTER_VERTICAL);
			imageView.setLayoutParams(imageParams);
			imageView.setScaleType(ScaleType.CENTER_CROP);
			overlayView.setLayoutParams(centerParams);
			addView(imageView);
			addView(overlayView);
		}
	}

	private Media get(int location) {
		Media mms;
		if (null != c && !c.isClosed() && c.moveToPosition(location)) {
			mms = MediaProvider.Helper.fromCursorCurentPosition(c);
			return mms;
		}

		return null;
	}

	public void onConfigurationChanged(Configuration newConfig) {
		// clear the cache to optimize image sizes for the current orientation
		if (bitmapCache != null) {
			bitmapCache.clear();
		}
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	@Override
	public void finishUpdate(View arg0) {
	}

	@Override
	public void restoreState(Parcelable arg0, ClassLoader arg1) {
	}

	@Override
	public Parcelable saveState() {
		return null;
	}

	@Override
	public void startUpdate(View arg0) {
	}
}