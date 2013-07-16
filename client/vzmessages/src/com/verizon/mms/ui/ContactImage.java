package com.verizon.mms.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;


public class ContactImage extends RelativeLayout {
	private Context context;
	private ImageView imageView;
	private ImageView[] imageViews;
	private int multiImageSize;
	private int blankContactColor;
	public static final int NUM_VIEWS = 4;


	public ContactImage(Context context) {
		super(context);
		init(context, null);
	}

	public ContactImage(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public ContactImage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		this.context = context;
		Resources res = context.getResources();
		// get image size from layout
		final TypedArray a = res.obtainAttributes(attrs, new int[] { android.R.attr.layout_width });
		int width = a.getDimensionPixelSize(0, 0);
		a.recycle();
		if (width != 0) {
			width -= (getPaddingLeft() + getPaddingRight());
		}
		else {
			Logger.error(getClass(), "no layout_width");
			width = res.getDimensionPixelSize(R.dimen.convListItemAvatarSize);
			if (width == 0) {
				throw new RuntimeException("No layout width or avatar size");
			}
		}
		// allow for 1-pixel border between images
		multiImageSize = (width - 1) / 2;
		
		
		blankContactColor = res.getColor(R.color.blank_contact);
	}

	/**
	 * Sets the drawable for a single contact image.
	 */
	public void setImage(Drawable image) {
		ImageView imageView = this.imageView;
		if (imageView == null) {
			// create new view and add it
			imageView = this.imageView = new ImageView(context);
			imageView.setScaleType(ScaleType.CENTER_CROP);
			final LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			addView(imageView, params);
			imageView.setImageDrawable(image);
		}
		else if (image != imageView.getDrawable()) {
			imageView.setImageDrawable(image);
		}

		setVisibility(true);
	}

	private void setVisibility(boolean single) {
		ImageView imageView = this.imageView;
		if (imageView != null) {
			imageView.setVisibility(single ? VISIBLE : GONE);
		}

		final ImageView[] imageViews = this.imageViews;
		if (imageViews != null) {
			final int vis = single ? GONE : VISIBLE;
			for (int i = 0; i < NUM_VIEWS; ++i) {
				imageViews[i].setVisibility(vis);
			}
		}
	}

	/**
	 * Sets the drawable for one or more contact images.  Null and missing drawables are left blank.
	 * 
	 * @param images Array of image drawables to use
	 */
	public void setImages(Drawable images[]) {
		setImages(images, null);
	}

	/**
	 * Sets the drawable for one or more contact images.  Null and missing drawables use the defaultImage.
	 * 
	 * @param images Array of image drawables to use
	 * @param defaultImage Default drawable to use for null or missing images in the array
	 */
	public void setImages(Drawable images[], Drawable defaultImage) {
		if (images != null) {
			int num = images.length;
			if (num == 1) {
				// single image
				setImage(images[0]);
			}
			else {
				// multiple images
				setVisibility(false);

				if (num > NUM_VIEWS) {
					num = NUM_VIEWS;
				}

				// if the image views already exist then check if their images are the same
				ImageView[] imageViews = this.imageViews;
				if (imageViews != null) {
					//keep other images as blank
					int j = num;
					for (; j < NUM_VIEWS; ++j) {
						imageViews[j].setImageDrawable(null);
					}
					
					int i = 0;
					for (; i < num; ++i) {
						final Drawable image = images[i];
						if (imageViews[i].getDrawable() != (image != null ? image : defaultImage)) {
							break;
						}
					}
					if (i >= num) {
						for (; i < NUM_VIEWS; ++i) {
							if (imageViews[i].getDrawable() != defaultImage) {
								break;
							}
						}
						if (i >= NUM_VIEWS) {
							// all images are the same, nothing to do
							return;
						}
					}
				}
				else {
					// create new views and add them to the layout
					imageViews = this.imageViews = new ImageView[NUM_VIEWS];
					for (int i = 0; i < NUM_VIEWS; ++i) {
						final ImageView imageView = new ImageView(context);
						imageView.setScaleType(ScaleType.CENTER_CROP);
						final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(multiImageSize, multiImageSize);
						params.addRule(i < 2 ? RelativeLayout.ALIGN_PARENT_TOP : RelativeLayout.ALIGN_PARENT_BOTTOM);
						params.addRule(i % 2 == 0 ? RelativeLayout.ALIGN_PARENT_LEFT : RelativeLayout.ALIGN_PARENT_RIGHT);
						imageView.setBackgroundColor(blankContactColor);
						addView(imageView, params);
						imageViews[i] = imageView;
					}
				}

				// set image drawables
				int i = 0;
				for (; i < num; ++i) {
					final Drawable image = images[i];
					imageViews[i].setImageDrawable(image != null ? image : defaultImage);
				}
				
				//keep other images as blank
				for (; i < NUM_VIEWS; ++i) {
					imageViews[i].setImageDrawable(null);
				}
			}
		}
	}
}
