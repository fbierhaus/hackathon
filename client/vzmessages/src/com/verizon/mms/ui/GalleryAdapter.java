package com.verizon.mms.ui;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;

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


public class GalleryAdapter extends SimpleCursorAdapter implements OnBitmapLoaded {
	private Context ctx;
	private final BitmapManager bitmapMgr;
	private int lastClicked;
	private int dp104;
	private int dp34;
	private long cacheSize;
	private BitmapCache bitmapCache;

	private static final int INITIAL_CAPACITY = 40;


	public GalleryAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
		super(context, layout, c, from, to);
		this.ctx = context;
		bitmapMgr = BitmapManager.INSTANCE;
		lastClicked = 0;
		dp104 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				104, ctx.getResources().getDisplayMetrics());
		dp34 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 34,
				ctx.getResources().getDisplayMetrics());
		initCache();
	}

	private void initCache() {
		if (cacheSize == 0) {
			// we split the user bitmap cache between the pager and gallery adapters
			final long userCache = bitmapMgr.getUserCacheSize("Gallery");
			cacheSize = (long)((float)userCache * (1f - GalleryActivity.PAGER_CACHE_PERCENT));
		}
		bitmapCache = new BitmapCache("Gallery", cacheSize, INITIAL_CAPACITY);
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

	@Override
	public void changeCursor(Cursor cursor) {
		if (cursor == null && bitmapCache != null) {
			bitmapCache.shutdown();
			bitmapCache = null;
		}
		super.changeCursor(cursor);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = new RelativeLayout(ctx);

		// Thumbnail
		ImageView imgView = new ImageView(ctx);
		imgView.setBackgroundColor(Color.DKGRAY);
		imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		imgView.setLayoutParams(new Gallery.LayoutParams(dp104, dp104));
		view.setTag(imgView);

		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// If clicked?
		if (cursor.getPosition() == lastClicked) {
			view.setPadding(1, 1, 1, 1);
			view.setBackgroundColor(Color.WHITE);
		}

		// Get imageView
		ImageView imgView = (ImageView) view.getTag();

		// Get Media
		Media media = MediaProvider.Helper.fromCursorCurentPosition(cursor);
        Bitmap bmp = null;
		if (media.isImage()) {
			// check cache for bitmap
			final String uri = media.getImageUri();
            if (bitmapCache != null) {
                bmp = bitmapCache.getBitmap(uri);
            }
			if (bmp != null) {
				imgView.setImageBitmap(bmp);
			}
			else {
				// Load
				bitmapMgr.loadBitmap(uri, imgView, dp104, dp104, true, true, false, this);
			}
			((RelativeLayout) view).addView(imgView);
			
			if ("image/gif".equalsIgnoreCase(media.getmPartCt())) {
				// Overlay
				// TODO: Can we reuse this overlayView somehow?
				ImageView overlayView = new ImageView(ctx);
				overlayView.setImageDrawable(context.getResources().getDrawable(
						R.drawable.btn_play_gallery));
				RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams(
						dp34, dp34);
				centerParams.addRule(RelativeLayout.CENTER_IN_PARENT);
				overlayView.setLayoutParams(centerParams);
				((RelativeLayout) view).addView(overlayView);
			}
		}
		else if (media.isVideo()) {
			final String uri = media.getVideoUri();
            if (bitmapCache != null) {
                bmp = bitmapCache.getBitmap(uri);
            }
			if (bmp != null) {
				imgView.setImageBitmap(bmp);
			}
			else {
				// Load thumbnail
				bitmapMgr.loadVideoThumbnail(uri, imgView, dp104, dp104, this);
			}
			((RelativeLayout) view).addView(imgView);

			// Overlay
			// TODO: Can we reuse this overlayView somehow?
			ImageView overlayView = new ImageView(ctx);
			overlayView.setImageDrawable(context.getResources().getDrawable(
					R.drawable.btn_play_gallery));
			RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams(
					dp34, dp34);
			centerParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			overlayView.setLayoutParams(centerParams);
			((RelativeLayout) view).addView(overlayView);
        } else if (media.isLocation() && DeviceConfig.OEM.isNbiLocationDisabled) {

            PduBody body = PduBodyCache.getPduBody(context,
                    ContentUris.withAppendedId(VZUris.getMmsUri(), media.getMId()));
            if (body != null) {

                Uri URI = null;
                int partNum = body.getPartsNum();
                for (int i = 0; i < partNum; i++) {
                    PduPart part = body.getPart(i);
                    String type = new String(part.getContentType());
                    if (ContentType.isImageType(type) || ContentType.isVideoType(type)
                            || ContentType.isAudioType(type)) {
                        URI = part.getDataUri();
                    }
                }
                // Location coming in two part wont hv any image,
                // so show a placeholder location image for it
                if(URI == null){
                	imgView.setImageResource(R.drawable.loc_menu_place_media);
                	return;
                }
                if (bitmapCache != null) {
                    bmp = bitmapCache.getBitmap(URI.toString());
                }
                if (bmp != null) {
                    imgView.setImageBitmap(bmp);
                } else {
                    // Load
                    bitmapMgr.loadBitmap(URI.toString(), imgView, dp104, dp104, true, this);
                }
                ((RelativeLayout) view).addView(imgView);

            }
        }
	}

	public int getMmsPosition(String id) {
		int intid = Integer.parseInt(id);
		Cursor c = getCursor();
		c.moveToPosition(-1);
		while (c.moveToNext()) {
			if (c.getInt(c.getColumnIndex(MediaProvider.Helper.M_ID)) == intid) {
				return c.getPosition();
			}
		}
		return 0;
	}

	public void setLastClickedPosition(int position) {
		this.lastClicked = position;
	}

	public int getLastClickedPosition() {
		return this.lastClicked;
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
}