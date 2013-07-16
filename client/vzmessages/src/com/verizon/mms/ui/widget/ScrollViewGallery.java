package com.verizon.mms.ui.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Gallery;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;

public class ScrollViewGallery extends Gallery {

	/**
	 * The distance the user has to move their finger, in density independent
	 * pixels, before we count the motion as A) intended for the ScrollView if
	 * the motion is in the vertical direction or B) intended for ourselfs, if
	 * the motion is in the horizontal direction - after the user has moved this
	 * amount they are "locked" into this direction until the next ACTION_DOWN
	 * event
	 */
	private static final int DRAG_BOUNDS_IN_DP = 15;

	/**
	 * A value representing the "unlocked" state - we test all MotionEvents when
	 * in this state to see whether a lock should be make
	 */
	private static final int SCROLL_LOCK_NONE = 0;

	/**
	 * A value representing a lock in the vertical direction - once in this
	 * state we will never redirect MotionEvents from the ScrollView to ourself
	 */
	private static final int SCROLL_LOCK_VERTICAL = 1;

	/**
	 * A value representing a lock in the horizontal direction - once in this
	 * state we will not deliver any more MotionEvents to the ScrollView, and
	 * will deliver them to ourselves instead.
	 */
	private static final int SCROLL_LOCK_HORIZONTAL = 2;

	/**
	 * The drag bounds in density independent pixels converted to actual pixels
	 */
	private int mDragBoundsInPx = 0;

	
	private float popGalleryWidth = 0;
	/**
	 * The coordinates of the intercepted ACTION_DOWN event
	 */
	private float mTouchStartX;
	private float mTouchStartY;

	/**
	 * The current scroll lock state
	 */
	private int mScrollLock = SCROLL_LOCK_NONE;

	private GalleryMoveListener moveListener;
	
	private boolean ignoreTouch = false;

	private int POST_FLING_DONE = 1;
	
	/**
	 * Interface used to notify if this Gallery is moving or has stopped moving 
	 *
	 */
	public interface GalleryMoveListener {
		int STATE_GALLERY_MOVE = 1;
		int STATE_GALLERY_FLING = 2;
		int STATE_GALLERY_STOP = 3;
		
		public void onGalleryMoving(int state);
	}
	
	public ScrollViewGallery(Context context) {
		super(context);
		initCustomGallery(context);
	}

	public ScrollViewGallery(Context context, AttributeSet attrs) {
		super(context, attrs);
		initCustomGallery(context);
	}

	public ScrollViewGallery(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initCustomGallery(context);
	}

	private void initCustomGallery(Context context) {
		final float scale = context.getResources().getDisplayMetrics().density;
		mDragBoundsInPx = (int) (scale * DRAG_BOUNDS_IN_DP + 0.5f);
		popGalleryWidth = (float)(context.getResources().getDimensionPixelSize(R.dimen.popup_activity_width) * 2.7);
	}

	public void setGalleryMoveListener(GalleryMoveListener listener) {
		moveListener = listener;
	}

	  /**
     * This will be called before the intercepted views onTouchEvent is called
     * Return false to keep intercepting and passing the event on to the target view
     * Return true and the target view will recieve ACTION_CANCEL, and the rest of the
     * events will be delivered to our onTouchEvent
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ignoreTouch) {
        	return false;
        }
        
        final int action = ev.getAction();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mTouchStartX = ev.getX();
            mTouchStartY = ev.getY();
            mScrollLock = SCROLL_LOCK_NONE;

            /**
             * Deliver the down event to the Gallery to avoid jerky scrolling
             * if we decide to redirect the ScrollView events to ourself
             */
            super.onTouchEvent(ev);
            break;

        case MotionEvent.ACTION_MOVE:
            if (mScrollLock == SCROLL_LOCK_VERTICAL) {
                // keep returning false to pass the events
                // onto the ScrollView
                return false;
            }

            final float touchDistanceX = (ev.getX() - mTouchStartX);
            final float touchDistanceY = (ev.getY() - mTouchStartY);
            if (Math.abs(touchDistanceX) > mDragBoundsInPx) {
                mScrollLock = SCROLL_LOCK_HORIZONTAL; // gallery action
                return true; // redirect MotionEvents to ourself
            }
            
            if (Math.abs(touchDistanceY) > mDragBoundsInPx) {
                mScrollLock = SCROLL_LOCK_VERTICAL;
                return false;
            }
            
            break;

        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
        	// if we're still intercepting at this stage, make sure the gallery
            // also recieves the up/cancel event as we gave it the down event earlier
        	super.onTouchEvent(ev);
            break;
        }

        return false;
    }
		
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (ignoreTouch) {
        	return false;
        }
		int action = event.getAction();
		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			if (moveListener != null) {
        		moveListener.onGalleryMoving(GalleryMoveListener.STATE_GALLERY_STOP);
        	}
		} else if (action == MotionEvent.ACTION_MOVE) {
			if (moveListener != null) {
        		moveListener.onGalleryMoving(GalleryMoveListener.STATE_GALLERY_MOVE);
        	}
		}
		return super.onTouchEvent(event);
	}
/*
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}*/

/*	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if (moveListener != null) {
    		moveListener.onGalleryMoving(GalleryMoveListener.STATE_GALLERY_FLING);
    	}
		
		float xstart = e1.getX();
		float xend = e2.getX();
		
		float diff = xstart - xend;
		
		if (diff < 0) {
			diff = -diff;
		}
		float velx = velocityX;
		if (diff > drag) {
			velx = velx / 1.5f;
		}
		
		return super.onFling(e1, e2,velx , velocityY);
		
	}*/

	private boolean isScrollingLeft(MotionEvent e1, MotionEvent e2) {
		return e2.getX() > e1.getX();
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (moveListener != null) {
			moveListener.onGalleryMoving(GalleryMoveListener.STATE_GALLERY_FLING);
		}
		if (isScrollingLeft(e1, e2)) {
			int velocity = (int)(popGalleryWidth - (e2.getX() - e1.getX()));
			super.onFling(e1, e2, velocity, 0);
		} else {
			int velocity = (int)-(popGalleryWidth - (e1.getX() - e2.getX()));
			super.onFling(e1, e2, velocity, 0);
		}
		
		flingHandler.removeMessages(POST_FLING_DONE);
		flingHandler.sendEmptyMessageDelayed(POST_FLING_DONE, 600);
		return true;
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		boolean handled = false;

		if (getFocusedChild() != null) {
			handled = getFocusedChild().dispatchKeyEvent(event);
		}

		if (!handled) {
			handled = event.dispatch(this, null, null);
		}

		return handled;
	}
	
	public void ignoreTouchEvents(boolean ignore) {
		ignoreTouch = ignore;
	}
	
	Handler flingHandler = new Handler () {
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			Logger.error("ishaque stop fling");
			if (what == POST_FLING_DONE) {
				if (moveListener != null) {
					moveListener.onGalleryMoving(GalleryMoveListener.STATE_GALLERY_MOVE);
					moveListener.onGalleryMoving(GalleryMoveListener.STATE_GALLERY_STOP);
				}
				return;
			}
			super.handleMessage(msg);
		}
		
	};
}
