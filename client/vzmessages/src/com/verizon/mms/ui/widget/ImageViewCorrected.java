package com.verizon.mms.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;


// fixes bugs in handling of measurements when adjustViewBounds is true

public class ImageViewCorrected extends ImageView {
	private boolean mAdjustViewBounds;
	private int mMaxWidth;
	private int mMaxHeight;

	// from View
	public static final int MEASURED_STATE_MASK = 0xff000000;
	public static final int MEASURED_STATE_TOO_SMALL = 0x01000000;


	public ImageViewCorrected(Context context) {
		super(context);
	}

	public ImageViewCorrected(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ImageViewCorrected(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setAdjustViewBounds(boolean adjustViewBounds) {
		mAdjustViewBounds = adjustViewBounds;
		if (adjustViewBounds) {
			setScaleType(ScaleType.FIT_CENTER);
		}
	}

	@Override
	public void setMaxWidth(int maxWidth) {
		super.setMaxWidth(maxWidth);
		mMaxWidth = maxWidth;
	}

	@Override
	public void setMaxHeight(int maxHeight) {
		super.setMaxHeight(maxHeight);
		mMaxHeight = maxHeight;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w;
		int h;

		// Desired aspect ratio of the view's contents (not including padding)
		float desiredAspect = 0.0f;

		// We are allowed to change the view's width
		boolean resizeWidth = false;

		// We are allowed to change the view's height
		boolean resizeHeight = false;

		final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
		final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

		final Drawable d = getDrawable();
		if (d == null) {
			// If no drawable, its intrinsic size is 0.
			w = h = 0;
		}
		else {
			w = d.getIntrinsicWidth();
			h = d.getIntrinsicHeight();
			if (w <= 0)
				w = 1;
			if (h <= 0)
				h = 1;

			// We are supposed to adjust view bounds to match the aspect
			// ratio of our drawable. See if that is possible.
			if (mAdjustViewBounds) {
				resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
				resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;

				desiredAspect = (float)w / (float)h;
			}
		}

		int pleft = getPaddingLeft();
		int pright = getPaddingRight();
		int ptop = getPaddingTop();
		int pbottom = getPaddingBottom();

		int widthSize;
		int heightSize;

		if (resizeWidth || resizeHeight) {
			/* If we get here, it means we want to resize to match the
			    drawables aspect ratio, and we have the freedom to change at
			    least one dimension. 
			*/

			// Get the max possible width given our constraints
			widthSize = resolveAdjustedSize(w + pleft + pright, mMaxWidth, widthMeasureSpec);

			// Get the max possible height given our constraints
			heightSize = resolveAdjustedSize(h + ptop + pbottom, mMaxHeight, heightMeasureSpec);

			if (desiredAspect != 0.0f) {
				// See what our actual aspect ratio is
				float actualAspect = (float)(widthSize - pleft - pright) / (heightSize - ptop - pbottom);

				if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {

					boolean done = false;

					// Try adjusting width to be proportional to height
					if (resizeWidth) {
						int newWidth = (int)(desiredAspect * (heightSize - ptop - pbottom)) + pleft + pright;
						final int newWidthSize = resolveAdjustedSize(newWidth, mMaxWidth, widthMeasureSpec);
						if (newWidthSize <= widthSize) {
							widthSize = Math.max(newWidth, getSuggestedMinimumWidth());
							done = true;
						}
					}

					// Try adjusting height to be proportional to width
					if (!done && resizeHeight) {
						int newHeight = (int)((widthSize - pleft - pright) / desiredAspect) + ptop + pbottom;
						heightSize = resolveAdjustedSize(newHeight, mMaxHeight, heightMeasureSpec);
						heightSize = Math.max(heightSize, getSuggestedMinimumHeight());
					}
				}
			}
		}
		else {
			/* We are either don't want to preserve the drawables aspect ratio,
			   or we are not allowed to change view dimensions. Just measure in
			   the normal way.
			*/
			w += pleft + pright;
			h += ptop + pbottom;

			w = Math.max(w, getSuggestedMinimumWidth());
			h = Math.max(h, getSuggestedMinimumHeight());

			widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
			heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);
		}

		setMeasuredDimension(widthSize, heightSize);
	}

	private int resolveAdjustedSize(int desiredSize, int maxSize, int measureSpec) {
		int result = desiredSize;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		switch (specMode) {
			case MeasureSpec.UNSPECIFIED:
				/* Parent says we can be as big as we want. Just don't be larger
				than max size imposed on ourselves.
				*/
				result = Math.min(desiredSize, maxSize);
				break;
			case MeasureSpec.AT_MOST:
				// Parent says we can be as big as we want, up to specSize. 
				// Don't be larger than specSize, and don't be larger than 
				// the max size imposed on ourselves.
				result = Math.min(Math.min(desiredSize, specSize), maxSize);
				break;
			case MeasureSpec.EXACTLY:
				// No choice. Do what we are told.
				result = specSize;
				break;
		}
		return result;
	}

	public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
		int result = size;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		switch (specMode) {
			case MeasureSpec.UNSPECIFIED:
				result = size;
				break;
			case MeasureSpec.AT_MOST:
				if (specSize < size) {
					result = specSize | MEASURED_STATE_TOO_SMALL;
				}
				else {
					result = size;
				}
				break;
			case MeasureSpec.EXACTLY:
				result = specSize;
				break;
		}
		return result | (childMeasuredState & MEASURED_STATE_MASK);
	}
}
