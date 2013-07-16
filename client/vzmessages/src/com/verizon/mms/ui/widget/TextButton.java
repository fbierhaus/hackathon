package com.verizon.mms.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class TextButton extends Button {
	private TextButtonClickListener clickListener;
	
	public TextButton(Context context) {
		super(context);		
	}
	
	public TextButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setButtonClickListener(TextButtonClickListener clickListener) {
		this.clickListener = clickListener;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		Drawable d = this.getBackground();
		if (isEnabled()) {
		    if (action == MotionEvent.ACTION_DOWN) {
		        d.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
		        this.setBackgroundDrawable(d);
		    } else if (action == MotionEvent.ACTION_OUTSIDE || action == MotionEvent.ACTION_CANCEL) {
		        d.clearColorFilter();
		        this.setBackgroundDrawable(d);
		    } else if (action == MotionEvent.ACTION_UP) {
		    	d.clearColorFilter();
		    	this.setBackgroundDrawable(d);
		    	if (clickListener != null) {
		    		clickListener.OnClick(TextButton.this);
		    	}
		    }
		}
		return super.onTouchEvent(event);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		
		Drawable d = this.getBackground();
		
		d.clearColorFilter();
		if (!enabled) {
		    d.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
		}
		this.setBackgroundDrawable(d);
	}
	

	public interface TextButtonClickListener {
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
