package com.verizon.mms.util;

import com.strumsoft.android.commons.logger.Logger;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class EnableDisableViewPager extends ViewPager {

    private boolean enabled = true;

    public EnableDisableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
    	if(Logger.IS_DEBUG_ENABLED){
    		Log.v("Neelesh sahu", ">>>>>>>onInterceptTouchEvent>>>>" + enabled);
    	}
        if (enabled) {

            return super.onInterceptTouchEvent(arg0);
        }
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
    	if(Logger.IS_DEBUG_ENABLED){
    		Log.v("Neelesh sahu", ">>>>>>>setEnabled>>>>" + enabled);
    	}
        this.enabled = enabled;
    }
}