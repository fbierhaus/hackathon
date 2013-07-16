package com.verizon.mms.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class ImageViewButton extends ImageView {
	private ImageButtonClickListener clickListener;
	
	public ImageViewButton(Context context) {
		super(context);		
	}
	
	public ImageViewButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setButtonClickListener(ImageButtonClickListener clickListener) {
		this.clickListener = clickListener;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		
		if (isEnabled()) {
		    if (action == MotionEvent.ACTION_DOWN) {
		        this.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
		    } else if (action == MotionEvent.ACTION_OUTSIDE || action == MotionEvent.ACTION_CANCEL) {
		        this.clearColorFilter();
		    } else if (action == MotionEvent.ACTION_UP) {
		    	this.clearColorFilter();
		    	
		    	if (clickListener != null) {
		    		clickListener.OnClick(ImageViewButton.this);
		    	}
		    }
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		
		this.clearColorFilter();
		if (!enabled) {
		    this.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
		}
	}
	
	public interface ImageButtonClickListener {
		public void OnClick(View v);
	}
	
	@Override
	public void onRestoreInstanceState(Parcelable state) {
		try {
			super.onRestoreInstanceState(null);
		} catch (Exception e) {
			
		}
	}
}
