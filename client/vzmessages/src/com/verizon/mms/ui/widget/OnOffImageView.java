package com.verizon.mms.ui.widget;

import android.content.Context;
import android.graphics.LightingColorFilter;
import android.util.AttributeSet;
import android.widget.ImageView;

public class OnOffImageView extends ImageView {

	public OnOffImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public OnOffImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OnOffImageView(Context context) {
		super(context);
	}

	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		
		if (selected) {
			this.setColorFilter(new LightingColorFilter(0x00ff00, 0xff000000));
		} else {
			this.clearColorFilter();
		}
	}
}
