package com.verizon.mms.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;


public class ContentWrapper extends LinearLayout {
	private View image;
	private View text;

	public ContentWrapper(Context context) {
		super(context);
	}

	public ContentWrapper(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void init(View image, View text) {
		this.image = image;
		this.text = text;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (image != null && text != null) {
			// set the text width the same as the image
			image.measure(widthMeasureSpec, heightMeasureSpec);
			final LayoutParams params = (LayoutParams)text.getLayoutParams();
			params.width = image.getMeasuredWidth();
			requestLayout();
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
