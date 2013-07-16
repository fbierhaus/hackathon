package com.verizon.mms.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

public class PopupWindows {
	protected Context mContext;
	protected PopupWindow mWindow;
	protected View mRootView;
	protected Drawable mBackground = null;
	protected WindowManager mWindowManager;
	protected int mTitleId;
	protected String mTitle;
	
	public PopupWindows(Context context) {
		mContext	= context;
		mWindow 	= new PopupWindow(context);
//		mWindow.setBackgroundDrawable(null);
		mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
	}
	
	protected void onDismiss() {		
	}
	
	protected void onShow() {		
	}

	protected void preShow() {
		if (mRootView == null) 
			throw new IllegalStateException("setContentView was not called with a view to display.");
	
		onShow();

		
		
		/*if (mBackground == null) { 
			mWindow.setBackgroundDrawable(new BitmapDrawable());
		} else { 
			mWindow.setBackgroundDrawable(mBackground);
		}*/
		mWindow.setBackgroundDrawable(null);

		//fix for the crash that happens on some
		//of the android OS in keyDispatchEvent
		if (mRootView instanceof QuickActionRelativeLayout) {
	        ((QuickActionRelativeLayout)mRootView).setDispatchKeyEventListener(new QuickActionRelativeLayout.OnDispatchKeyEventListener() {

	            @Override
	            public void onDispatchKeyEvent(KeyEvent event) {
	                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && mWindow.isShowing()) {
	                    dismiss();
	                }
	            }
	        });
	    }  
		
		mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		mWindow.setTouchable(true);
		mWindow.setFocusable(true);
		mWindow.setOutsideTouchable(true);

		mWindow.setContentView(mRootView);
	}

	public void setBackgroundDrawable(Drawable background) {
		mBackground = background;
	}
	
	public void setContentView(View root) {
		mRootView = root;
		
		mWindow.setContentView(root);
	}
	
	public void setContentView(int layoutResID) {
		LayoutInflater inflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		setContentView(inflator.inflate(layoutResID, null));
	}
	
	protected void setOnDismissListener(PopupWindow.OnDismissListener listener) {
		mWindow.setOnDismissListener(listener);  
	}

	public void dismiss() {
		mWindow.dismiss();
	}
	
	public void setTitle(int title) {
		mTitleId = title;
	}
	
	/**
	 * Sets the resource title id of the popupwindow
	 */
	public void setTitle(String title) {
		mTitle = title;
	}
	
	public boolean isShowing() {
		return mWindow.isShowing();
	}
}