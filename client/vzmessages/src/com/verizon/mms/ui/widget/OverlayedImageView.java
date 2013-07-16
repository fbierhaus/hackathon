/**
 * OverlayedImageView.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.verizon.mms.util.Util;

/**
 * This class adds an overlay on the Image displayed in the ImageView
 * 
 * @author Essack
 * @Since Apr 3, 2012
 */
public class OverlayedImageView extends ImageView {
    private BitmapDrawable     mOverlayBitmap        = null;
    private Rect               mOverlayRect          = null;
    private OnClickListener    mImageClickListener   = null;
    private OnClickListener    mOverlayClickListener = null;

    //variables used to identify the click on OverlayImage
    private static final float DELTA_X               = 5;
    private float              mInitXPos             = 0;


    public OverlayedImageView(Context context) {
        super(context);
    }

    public OverlayedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public OverlayedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * User different onClickListener, don't use the same and switch using the ViewId as the view returned will
     * be the same for both overlay and image click
     */
    public void init(BitmapDrawable bitmap, OnClickListener imageClickListener,
            OnClickListener overlayClickListener) {
        mOverlayBitmap = bitmap;
        mOverlayClickListener = overlayClickListener;
        mImageClickListener = imageClickListener;
        mOverlayRect = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mOverlayBitmap != null) {
            if (mOverlayRect == null) {
                Point point = Util.getBitmapOffset(this, true);
                
                if (point.y < 0) {
                    point.y = 0;
                }
                //hard coding the width to and height to 65 to increase the 
                //touch size of the overlay
                mOverlayRect = new Rect(point.x, point.y, point.x + 75, point.y + 75);
            }

            canvas.drawBitmap(mOverlayBitmap.getBitmap(), mOverlayRect.left, mOverlayRect.top, null);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mOverlayRect = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mOverlayClickListener == null) {
            return super.onTouchEvent(event);
        }

        int evnt = event.getAction() & MotionEvent.ACTION_MASK;

        if (evnt == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();

            if (x - mInitXPos > DELTA_X || x - mInitXPos < 5) {
                //if the point is within overlayrect then call
                //overlay's onClick function
                if (mOverlayRect.contains((int) x, (int) y)) {
                    mOverlayClickListener.onClick(this);
                } else {
                    mImageClickListener.onClick(this);
                }
            }
            mInitXPos = 0;
        } else if (evnt == MotionEvent.ACTION_DOWN) {
            mInitXPos = event.getX();
        }
        super.onTouchEvent(event);

        return true;
    }
}
