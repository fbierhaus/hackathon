package com.verizon.mms.ui.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.RelativeLayout;

import com.verizon.messaging.vzmsgs.R;

public class EmojiGridView extends GridView {
	Context context;
	public EmojiGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
	}

	public EmojiGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
	}

	public EmojiGridView(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		setGridMargin();
		super.onConfigurationChanged(newConfig);
	}
	
	public void setGridMargin() {
		final WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay(); 
    	DisplayMetrics dm = new DisplayMetrics();
    	Resources res = context.getResources();
        display.getMetrics(dm);
        
        int width = 0;
        
        boolean portrait = res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT; 
        if (portrait) {
        	width = java.lang.Math.min(display.getHeight(), display.getWidth());
        } else {
        	width = java.lang.Math.max(display.getHeight(), display.getWidth());
        }
        int emojiWidth = res.getDimensionPixelSize(R.dimen.emoji_icon_width);
        
        int spacing = (width % emojiWidth) / 2 - 1;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)getLayoutParams();
        params.leftMargin = spacing;
        
        setLayoutParams(params);
    }
}