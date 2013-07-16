package com.verizon.mms.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

public class TouchImageView extends ImageView {

    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    private EnableDisableViewPager mEnableDisableViewPager;
    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;
    float oldDist;
    // Remember some things for zooming
    PointF last = new PointF();
    PointF mid = new PointF();
    PointF start = new PointF();
    float minScale = 1f;
    float maxScale = 3f;
    float[] m;

    float redundantXSpace, redundantYSpace;

    float width, height;
    static final int CLICK = 3;
    float saveScale = 1f;
    float right, bottom, origWidth, origHeight, bmWidth, bmHeight;

    ScaleGestureDetector mScaleDetector;
    final GestureDetector activityGestureDetector;
    final GestureDetector imageGestureDetector;
    Context context;

    public TouchImageView(Context context, GestureDetector _activityGestureDetector,
            EnableDisableViewPager mDisableViewPager) {
        super(context);
        super.setClickable(true);
       
        this.context = context;
        mEnableDisableViewPager = mDisableViewPager;
        this.activityGestureDetector = _activityGestureDetector;

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        matrix.setTranslate(1f, 1f);
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);
        imageGestureDetector = new GestureDetector(context, new GestureListener());

        setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                if (isZoomed) {
//                    imageGestureDetector.onTouchEvent(event);
//                } else {
//                    gestureDetector.onTouchEvent(event);
//                }
                if (mEnableDisableViewPager.isEnabled()) {
                    activityGestureDetector.onTouchEvent(event);
                } else {
                    imageGestureDetector.onTouchEvent(event);
                }
                
                mScaleDetector.onTouchEvent(event);

                matrix.getValues(m);
                float x = m[Matrix.MTRANS_X];
                float y = m[Matrix.MTRANS_Y];
                PointF curr = new PointF(event.getX(), event.getY());

                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    last.set(event.getX(), event.getY());
                    start.set(last);
                    if (!mEnableDisableViewPager.isEnabled()) {
                        setMode(DRAG);
                    }
                    savedMatrix.set(matrix);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        logImageViewMatrixInfo(matrix, TouchImageView.this);
                        float deltaX = curr.x - last.x;
                        float deltaY = curr.y - last.y;
                        float scaleWidth = Math.round(origWidth * saveScale);
                        float scaleHeight = Math.round(origHeight * saveScale);
                        if (scaleWidth < width) {
                            deltaX = 0;
                            if (y + deltaY > 0)
                                deltaY = -y;
                            else if (y + deltaY < -bottom)
                                deltaY = -(y + bottom);
                        } else if (scaleHeight < height) {
                            deltaY = 0;
                            if (x + deltaX > 0)
                                deltaX = -x;
                            else if (x + deltaX < -right)
                                deltaX = -(x + right);
                        } else {
                            if (x + deltaX > 0)
                                deltaX = -x;
                            else if (x + deltaX < -right)
                                deltaX = -(x + right);

                            if (y + deltaY > 0)
                                deltaY = -y;
                            else if (y + deltaY < -bottom)
                                deltaY = -(y + bottom);
                        }
//                        matrix.postTranslate(deltaX, deltaY);
                        last.set(curr.x, curr.y);
                        matrix.set(savedMatrix);
                        matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                    }
                    if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            float scaleX = (float) width / (float) bmWidth;
                            float scaleY = (float) height / (float) bmHeight;
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                            if (isZoomedOut(logImageViewMatrixInfos(matrix, TouchImageView.this))) {
                                scale = Math.min(scaleX, scaleY);
                                matrix.setScale(scale, scale);
                                matrix.postTranslate(redundantXSpace, redundantYSpace);
                            }

                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    setMode(NONE);
                    int xDiff = (int) Math.abs(curr.x - start.x);
                    int yDiff = (int) Math.abs(curr.y - start.y);
                    if (xDiff < CLICK && yDiff < CLICK)
                        performClick();
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        setMode(ZOOM);
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    setMode(NONE);
                    break;
                }
                setImageMatrix(matrix);
                invalidate();
                return true; // indicate event was handled
            }

        });
    }

    private void setMode(int m) {
        mode = m;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        bmWidth = bm.getWidth();
        bmHeight = bm.getHeight();
    }

    public void setMaxZoom(float x) {
        maxScale = x;
    }

	private float spacing(MotionEvent event) {
		if (event.getPointerCount() >= 2) {
			try{
			 float x = event.getX(0) - event.getX(1);
			 float y = event.getY(0) - event.getY(1);
			return FloatMath.sqrt(x * x + y * y);
			}
			catch(IllegalArgumentException e)
			{
				return 0;
			}
			}
		return 0;

	}

	private void midPoint(PointF point, MotionEvent event) {
		if (event.getPointerCount() >= 2) {
			float x = event.getX(0) + event.getX(1);
			float y = event.getY(0) + event.getY(1);
			point.set(x / 2, y / 2);
		} else if (event.getPointerCount() == 1) {
			point.set(event.getX(0), event.getY(0));
		} else {
			point.set(0, 0);
		}
	}

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            setMode(ZOOM);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = (float) Math.min(Math.max(.95f, detector.getScaleFactor()), 1.05);
            float origScale = saveScale;
            saveScale *= mScaleFactor;
            if (saveScale > maxScale) {
                saveScale = maxScale;
                mScaleFactor = maxScale / origScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                mScaleFactor = minScale / origScale;
            }
            right = width * saveScale - width - (2 * redundantXSpace * saveScale);
            bottom = height * saveScale - height - (2 * redundantYSpace * saveScale);
            if (origWidth * saveScale <= width || origHeight * saveScale <= height) {
                matrix.postScale(mScaleFactor, mScaleFactor, width / 2, height / 2);
                if (mScaleFactor < 1) {
                    matrix.getValues(m);
                    float x = m[Matrix.MTRANS_X];
                    float y = m[Matrix.MTRANS_Y];
                    if (mScaleFactor < 1) {
                        if (Math.round(origWidth * saveScale) < width) {
                            if (y < -bottom)
                                matrix.postTranslate(0, -(y + bottom));
                            else if (y > 0)
                                matrix.postTranslate(0, -y);
                        } else {
                            if (x < -right)
                                matrix.postTranslate(-(x + right), 0);
                            else if (x > 0)
                                matrix.postTranslate(-x, 0);
                        }
                    }
                }
            } else {
                matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
                matrix.getValues(m);
                float x = m[Matrix.MTRANS_X];
                float y = m[Matrix.MTRANS_Y];
                if (mScaleFactor < 1) {
                    if (x < -right)
                        matrix.postTranslate(-(x + right), 0);
                    else if (x > 0)
                        matrix.postTranslate(-x, 0);
                    if (y < -bottom)
                        matrix.postTranslate(0, -(y + bottom));
                    else if (y > 0)
                        matrix.postTranslate(0, -y);
                }
            }
            return true;

        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
     
        // Fit to screen.
        float scale;
        float scaleX = (float) width / (float) bmWidth;
        float scaleY = (float) height / (float) bmHeight;
        scale = Math.min(scaleX, scaleY);
        matrix.setScale(scale, scale);
        setImageMatrix(matrix);
        saveScale = 1f;

        // Center the image
        redundantYSpace = (float) height - (scale * (float) bmHeight);
        redundantXSpace = (float) width - (scale * (float) bmWidth);
        redundantYSpace /= (float) 2;
        redundantXSpace /= (float) 2;

        matrix.postTranslate(redundantXSpace, redundantYSpace);

        origWidth = width - 2 * redundantXSpace;
        origHeight = height - 2 * redundantYSpace;
        right = width * saveScale - width - (2 * redundantXSpace * saveScale);
        bottom = height * saveScale - height - (2 * redundantYSpace * saveScale);
        setImageMatrix(matrix);
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float scaleX = (float) width / (float) bmWidth;
            float scaleY = (float) height / (float) bmHeight;
            float scale = Math.min(scaleX, scaleY);
            matrix.setScale(scale, scale);
            matrix.postTranslate(redundantXSpace, redundantYSpace);
            mEnableDisableViewPager.setEnabled(true);
            return true;
        }
    }

    private float[] logImageViewMatrixInfos(Matrix matrix, ImageView imageView) {
        float[] values = new float[9];
        matrix.getValues(values);
        float[] imagedata = new float[2];
        imagedata[0] = values[0] * imageView.getWidth();
        imagedata[1] = values[4] * imageView.getHeight();
        return imagedata;
    }
    
    
    private void logImageViewMatrixInfo(Matrix matrix, ImageView imageView) {
        float[] values = new float[9];
        matrix.getValues(values);
        float[] imagedata = new float[2];
        imagedata[0] = values[0] * imageView.getWidth();
        imagedata[1] = values[4] * imageView.getHeight();
    }


    private boolean isZoomedOut(float[] imageInfo) {
        if ((imageInfo[0] + 100) < Math.min(width, bmWidth)
                || (imageInfo[1] + 100) < Math.min(height, bmHeight)) {
            mEnableDisableViewPager.setEnabled(true);
            return true;
        } else {
            mEnableDisableViewPager.setEnabled(false);
            return false;
        }
    }

}