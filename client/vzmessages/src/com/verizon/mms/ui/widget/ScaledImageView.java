package com.verizon.mms.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * ImageView for Scaled Image.
 * We want to use the width of the ImageView set in the layout as the length of the longer side and
 * maintain the aspect ratio
 * That cannot be done by the ImageView, so we have to subclass it and calculate the image dimension
 * in onMeasure()
 * 
 * @author samson
 */
public class ScaledImageView extends ImageView {
	// private View mParentView = null;
	// private Drawable mLastDrawable = null;

	/**
	 * Constructor for ScaledImageView
	 * 
	 * @param context
	 */
	public ScaledImageView(Context context) {
		super(context);
	}

	public ScaledImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// public void setParent(View parent) {
	// mParentView = parent;
	// }

	@Override
	protected void drawableStateChanged() {
		// boolean changed = drawable != mLastDrawable;
		// mLastDrawable = drawable;
		//
		// if (changed == true) {
		// if (mParentView != null)
		// mParentView.requestLayout();
		// }

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// int currentWidth = this.getWidth();
		// int currentHeight = this.getHeight();

		// first calculate the original bound
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		Drawable drawable = this.getDrawable();
		if (drawable != null) {
			int height;
			int width = MeasureSpec.getSize(widthMeasureSpec);

			// get the image's actual width and height
			int imageWidth = drawable.getIntrinsicWidth();
			int imageHeight = drawable.getIntrinsicHeight();

			if (imageWidth > 0 && imageHeight > 0) { // check if width and height are available
				// scale the image and maintain the aspect ratio using the view's width set in the
				// layout as the maximum side
				if (imageWidth >= imageHeight)
					height = (int)((float)width * ((float)imageHeight / (float)imageWidth));
				else {
					height = width;
					width = (int)((float)height * ((float)imageWidth / (float)imageHeight));
				}
				setMeasuredDimension(width, height);
			}
		}
	}
}
