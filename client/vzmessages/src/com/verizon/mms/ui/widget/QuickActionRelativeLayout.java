package com.verizon.mms.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.strumsoft.android.commons.logger.Logger;

public class QuickActionRelativeLayout extends RelativeLayout {

	public QuickActionRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public QuickActionRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public QuickActionRelativeLayout(Context context) {
		super(context);
	}

	private OnDispatchKeyEventListener mOnDispatchKeyEventListener;



	public void setDispatchKeyEventListener(OnDispatchKeyEventListener listener) {
		mOnDispatchKeyEventListener = listener;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		boolean flag = false;
		try {
			if (mOnDispatchKeyEventListener != null) {
				mOnDispatchKeyEventListener.onDispatchKeyEvent(event);
			}
			flag = super.dispatchKeyEvent(event);
		} catch (Exception e) {
			Logger.error("QuickActionRelativeLayout.dispatchKeyEvent", e);
		}
		return flag;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}   

	public static interface OnDispatchKeyEventListener {
		public void onDispatchKeyEvent(KeyEvent event);
	}
}
