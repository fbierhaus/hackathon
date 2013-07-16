package com.verizon.mms.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.verizon.messaging.vzmsgs.R;

public class DragNDropListView extends ListView {

	boolean mDragMode;

	int mStartPosition;
	int mEndPosition;
	int mDragPointOffset;		//Used to adjust drag view location
	int mDragWidth;
	int mDragHeight;
	private int mUpperBound;
    private int mLowerBound;
    private int mHeight;
    
	ImageView mDragView;
	GestureDetector mGestureDetector;
	
	DropListener mDropListener;
	RemoveListener mRemoveListener;
	DragListener mDragListener;

	private int mTouchSlop;
	
	public DragNDropListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mDragWidth = (int)context.getResources().getDimension(R.dimen.dragWidth);
		mDragHeight = (int)context.getResources().getDimension(R.dimen.dragHeigth);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
	}
	
	public void setDropListener(DropListener l) {
		mDropListener = l;
	}

	public void setRemoveListener(RemoveListener l) {
		mRemoveListener = l;
	}
	
	public void setDragListener(DragListener l) {
		mDragListener = l;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();	
		
		if (action == MotionEvent.ACTION_DOWN && x > getWidth() - mDragWidth) {
			int startPosition = pointToPosition(x,y);
			if (startPosition != INVALID_POSITION) {
				int itemPosition = startPosition - getFirstVisiblePosition();
	            int top = getChildAt(itemPosition).getTop();
	            
	            if (y < top + mDragHeight) {
	            	mDragMode = true;
	            }
			}
		}

		if (!mDragMode) {
			return super.onTouchEvent(ev);
		}

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mStartPosition = pointToPosition(x,y);
				if (mStartPosition != INVALID_POSITION) {
					int mItemPosition = mStartPosition - getFirstVisiblePosition();
                    mDragPointOffset = y - getChildAt(mItemPosition).getTop();
                    mDragPointOffset -= ((int)ev.getRawY()) - y;
                    mHeight = getHeight();
                    
                    int touchSlop = mTouchSlop;
                    mUpperBound = Math.min(y - touchSlop, mHeight / 3);
                    mLowerBound = Math.max(y + touchSlop, mHeight * 2 /3);
                    
					startDrag(mItemPosition,y);
					drag(0,y);// replace 0 with x if desired
				}	
				break;
			case MotionEvent.ACTION_MOVE:
				drag(0,y);// replace 0 with x if desired
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
			default:
				mDragMode = false;
				mEndPosition = pointToPosition(x,y);
				stopDrag(mStartPosition - getFirstVisiblePosition());
				if (mDropListener != null && mStartPosition != INVALID_POSITION && mEndPosition != INVALID_POSITION) { 
	        		 mDropListener.onDrop(mStartPosition, mEndPosition);
				}
				break;
		}
		return true;
	}	
	
	private void adjustScrollBounds(int y) {
        if (y >= mHeight / 3) {
            mUpperBound = mHeight / 3;
        }
        if (y <= mHeight * 2 / 3) {
            mLowerBound = mHeight * 2 / 3;
        }
    }
	
	// move the drag view
	private void drag(int x, int y) {
		if (mDragView != null) {
			WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mDragView.getLayoutParams();
			layoutParams.x = x;
			layoutParams.y = y - mDragPointOffset;
			WindowManager mWindowManager = (WindowManager) getContext()
					.getSystemService(Context.WINDOW_SERVICE);
			mWindowManager.updateViewLayout(mDragView, layoutParams);

			int speed = 0;
			adjustScrollBounds(y);
			if (y > mLowerBound) {
                // scroll the list up a bit
                speed = y > (mHeight + mLowerBound) / 2 ? 16 : 4;
            } else if (y < mUpperBound) {
                // scroll the list down a bit
                speed = y < mUpperBound / 2 ? -16 : -4;
            }
            if (speed != 0) {
                int ref = pointToPosition(0, mHeight / 2);
                if (ref == AdapterView.INVALID_POSITION) {
                    //we hit a divider or an invisible view, check somewhere else
                    ref = pointToPosition(0, mHeight / 2 + getDividerHeight() + 64);
                }
                View v = getChildAt(ref - getFirstVisiblePosition());
                if (v!= null) {
                    int pos = v.getTop();
                    setSelectionFromTop(ref, pos - speed);
                }
            }
			if (mDragListener != null)
				mDragListener.onDrag(x, y, null);// change null to "this" when ready to use
		}
	}

	// enable the drag view for dragging
	private void startDrag(int itemIndex, int y) {
		stopDrag(itemIndex);

		View item = getChildAt(itemIndex);
		if (item == null) return;
		item.setDrawingCacheEnabled(true);
		if (mDragListener != null) {
			mDragListener.onStartDrag(item, mStartPosition);
		}
		
        // Create a copy of the drawing cache so that it does not get recycled
        // by the framework when the list tries to clean up memory
        Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
        
        item.setDrawingCacheEnabled(false);
        WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPointOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;
        
        Context context = getContext();
        ImageView v = new ImageView(context);
        v.setImageBitmap(bitmap);      

        WindowManager mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
	}

	// destroy drag view
	private void stopDrag(int itemIndex) {
		if (mDragView != null) {
			if (mDragListener != null)
				mDragListener.onStopDrag(getChildAt(itemIndex));
            mDragView.setVisibility(GONE);
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
	}

//	private GestureDetector createFlingDetector() {
//		return new GestureDetector(getContext(), new SimpleOnGestureListener() {
//            @Override
//            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
//                    float velocityY) {         	
//                if (mDragView != null) {              	
//                	int deltaX = (int)Math.abs(e1.getX()-e2.getX());
//                	int deltaY = (int)Math.abs(e1.getY() - e2.getY());
//               
//                	if (deltaX > mDragView.getWidth()/2 && deltaY < mDragView.getHeight()) {
//                		mRemoveListener.onRemove(mStartPosition);
//                	}
//                	
//                	stopDrag(mStartPosition - getFirstVisiblePosition());
//
//                    return true;
//                }
//                return false;
//            }
//        });
//	}
}
