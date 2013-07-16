package com.verizon.mms.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

public class ImageViewButton extends ImageView {
	public ImageViewButton(Context context) {
		super(context);		
	}
	
	public ImageViewButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction(); 
		if (action == MotionEvent.ACTION_DOWN) {
			this.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
		}
		else if (action == MotionEvent.ACTION_UP ||
				 action == MotionEvent.ACTION_OUTSIDE) {
			this.clearColorFilter();
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void setEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		super.setEnabled(enabled);
		
		if (enabled) {
			this.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
		} else {
			this.clearColorFilter();
		}
	}
	
	
}
