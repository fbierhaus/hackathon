package com.verizon.mms.ui.widget;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;


public class ResizeAnimation extends ScaleAnimation {
	private View view;
	private int fromWidth;
	private int fromHeight;
	private int toWidth;
	private int toHeight;


	public ResizeAnimation(View view, int fromWidth, int fromHeight) {
		super(
			fromHeight == 0 ? 1f : 0, 1f,
			fromWidth == 0 ? 1f : 0, 1f,
			Animation.RELATIVE_TO_SELF, 0f,
			Animation.RELATIVE_TO_SELF, 0f);
		this.view = view;
		this.fromWidth = fromWidth;
		this.fromHeight = fromHeight;
		toWidth = -1;
		toHeight = -1;
	}

	public ResizeAnimation(View view, int fromWidth, int fromHeight, int toWidth, int toHeight) {
		this(view, fromWidth, fromHeight);
		this.toWidth = toWidth;
		this.toHeight = toHeight;
	}

	@Override
	public void initialize(int width, int height, int parentWidth, int parentHeight) {
		super.initialize(width, height, parentWidth, parentHeight);
		if (fromWidth == -1) {
			fromWidth = width;
		}
		if (fromHeight == -1) {
			fromHeight = height;
		}
		if (toWidth == -1) {
			toWidth = width;
		}
		if (toHeight == -1) {
			toHeight = height;
		}
	}

	@Override
	protected void applyTransformation(float interpolatedTime, Transformation t) {
		final LayoutParams params = view.getLayoutParams();
		params.height = Math.round((toHeight - fromHeight) * interpolatedTime) + fromHeight;
		params.width = Math.round((toWidth - fromWidth) * interpolatedTime) + fromWidth;
		super.applyTransformation(interpolatedTime, t);
		view.requestLayout();
	}
}
