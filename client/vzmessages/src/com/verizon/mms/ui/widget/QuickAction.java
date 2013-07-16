package com.verizon.mms.ui.widget;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.method.KeyListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager.BadTokenException;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.verizon.mms.ui.MessagingPreferenceActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.DeviceConfig.OEM;
import com.verizon.mms.ui.SendCommentsToDeveloper;
import com.verizon.mms.util.Util;

public class QuickAction extends PopupWindows implements OnDismissListener {
    private View mRootView;
    private View mQuickMenuView;
    private View mBackground;
    private ImageView mArrowUp;
    private ImageView mArrowDown;
    private LayoutInflater mInflater;
    private ViewGroup mTrack;
    private View mScroller;
    private LinearLayout mMenuLayout;
    private OnActionItemClickListener mItemClickListener;
    private OnDismissListener mDismissListener;

    private List<ActionItem> mActionItems = new ArrayList<ActionItem>();

    private boolean mDidAction;

    private int mChildPos;
    private int mInsertPos;
    private int mAnimStyle;
    private int rootWidth = 0;
	//This View is used in PopUpWindow listener as Menu Item listener 
    // wont receive All Screen events
    protected View touchedActionItem = null;
    
    public static final int ANIM_GROW_FROM_LEFT = 1;
    public static final int ANIM_GROW_FROM_RIGHT = 2;
    public static final int ANIM_GROW_FROM_CENTER = 3;
    public static final int ANIM_REFLECT = 4;
    public static final int ANIM_AUTO = 5;
    public static final int ANIM_NONE = 6;

    boolean isSendCommentFlag = false;
    private GlobalLayoutListener mLayoutListener = null;

    public QuickAction(Context context) {
        super(context);
                
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setRootViewId(R.layout.popup_vertical);

        mAnimStyle = ANIM_AUTO;
        mChildPos = 0;
    }

    public ActionItem getActionItem(int index) {
    	return mActionItems.get(index);
    }
    
    public int getSize() {
    	return mActionItems.size();
    }

    public void setRootViewId(int id) {
    	
        mRootView = (ViewGroup) mInflater.inflate(id, null);
        mTrack = (ViewGroup) mRootView.findViewById(R.id.tracks);
        mScroller = mRootView.findViewById(R.id.scroller);
        mQuickMenuView = mRootView.findViewById(R.id.quickMenu);

        mArrowDown = (ImageView) mRootView.findViewById(R.id.arrow_down);
        mArrowUp = (ImageView) mRootView.findViewById(R.id.arrow_up);

        mMenuLayout = (LinearLayout) mRootView.findViewById(R.id.menuLayout);
        
        mBackground = mRootView.findViewById(R.id.background);
        mBackground.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (mContext instanceof SendCommentsToDeveloper) {
        			((SendCommentsToDeveloper) mContext).isPopupClosed(true);
        		}

            }
        });
        
        setContentView(mRootView);
        
        //fill entire screen
        mWindow.setWindowLayoutMode(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
    }
    
    
    

    public void setAnimStyle(int mAnimStyle) {
    	
        this.mAnimStyle = mAnimStyle;
    }

    public void setOnActionItemClickListener(OnActionItemClickListener listener) {
    	
        mItemClickListener = listener;
    }
  
    public void addActionItem(ActionItem action) {
    	
    	addActionItem(action, false);
    }
    public void addActionItem(ActionItem action, boolean isSmiley) {
    	
        mActionItems.add(action);

        int title = action.getTitle();
        int icon = action.getIcon();
        final View container = mInflater.inflate(R.layout.action_item_vertical, null);
        ImageView img = (ImageView) container.findViewById(R.id.iv_icon);
        TextView text = (TextView) container.findViewById(R.id.tv_title);
        if (icon != 0) {
            img.setImageResource(icon);
            img.setVisibility(View.VISIBLE);
        } else {
            img.setVisibility(View.GONE);
        }

        if (title != 0) {
            text.setText(title);
        } else {
        	
        	text.setText(action.getActionTitle());
        }
        
        if(isSmiley){
        	TextView tv_smileyCharacter = (TextView) container.findViewById(R.id.tv_smileyCharacter);
            tv_smileyCharacter.setVisibility(View.VISIBLE);
            tv_smileyCharacter.setText(action.getSmileyCharacter());
        }
        
        final int pos = mChildPos;
        final int actionId = action.getActionId();

        container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(QuickAction.this, pos, actionId);
                }

                if (!getActionItem(pos).isSticky()) {
                    mDidAction = true;
                    
                    //remove the selection in case we will use the same menu again
                    v.setBackgroundResource(android.R.color.transparent);
                    
                    dismiss();
                    if (mContext instanceof SendCommentsToDeveloper) {
            			((SendCommentsToDeveloper) mContext).isPopupClosed(true);
            		}
                    
                }
            }
        });
        
        container.setOnTouchListener(new OnTouchListener() {
        	
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                   v.setBackgroundResource(R.drawable.action_item_selected);
                   touchedActionItem = v;
                }
                else if (action == MotionEvent.ACTION_CANCEL) {
                    v.setBackgroundResource(android.R.color.transparent);
                }
                else if (action == MotionEvent.ACTION_UP) {
                    if(OEM.isNexusTab){
                    v.setBackgroundResource(android.R.color.transparent);
                    }
                }
                return false;
            }
        });
       

        mWindow.setTouchInterceptor(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                
                int action = event.getAction();
                if (action == MotionEvent.ACTION_OUTSIDE) {
                    mWindow.dismiss();
                 
                    if (touchedActionItem != null) {
                        touchedActionItem.setBackgroundResource(android.R.color.transparent);
                        touchedActionItem = null;
                    }
                    return true;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    if (touchedActionItem != null) {
                        touchedActionItem.setBackgroundResource(R.drawable.action_item_selected);
                    }
                } else {
                    if (touchedActionItem != null) {
                        touchedActionItem.setBackgroundResource(android.R.color.transparent);
                        touchedActionItem = null;
                    }
                }

                return false;
            }
        });
        mTrack.addView(container, mInsertPos);

        mChildPos++;
        mInsertPos++;
    }
	
    public void show(final View anchor, final View windowTokenView, boolean softKeyVisible) {
    	
        if (softKeyVisible && windowTokenView != null) {
            // Hide the soft keyboard first to allow the activity to be resized
            // then show the QuickAction menu to display it at proper position
            Util.forceHideKeyboard((Activity) mContext, windowTokenView);
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(200);
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            	try {
                            		show(anchor, windowTokenView);
                            	} catch (BadTokenException e) {
                            		if (Logger.IS_ERROR_ENABLED) {
                            			Logger.error("Window toke not valid ", e);
                            		}
                            	}
                            }

                        });
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
        } else {
            show(anchor, windowTokenView);
        }
    }

    
    private void show(View anchor, View editText) {
    	if (mInsertPos > 0) {
            View v = mTrack.getChildAt(mInsertPos - 1);

            View divider = v.findViewById(R.id.divider);
            if (divider != null) {
                divider.setVisibility(View.GONE);
            }
        }

        TextView v = (TextView) mMenuLayout.findViewById(R.id.txtTitle);
        if (mTitleId > 0) {
            v.setText(mTitleId);
            v.setVisibility(View.VISIBLE);
        } else if (mTitle != null) {
            v.setText(mTitle);
            v.setVisibility(View.VISIBLE);
        }
        v.setOnClickListener(null);
       
        preShow();
        v.setKeyListener(new KeyListener() {
		
		@Override
		public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public boolean onKeyOther(View view, Editable text, KeyEvent event) {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public boolean onKeyDown(View view, Editable text, int keyCode,
				KeyEvent event) {
			
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public int getInputType() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public void clearMetaKeyState(View view, Editable content, int states) {
			// TODO Auto-generated method stub
			
		}
	});
        mRootView.getViewTreeObserver().removeGlobalOnLayoutListener(mLayoutListener);
        /*if (isShowing()) {
            dismiss();
        }*/
        
        mDidAction = false;
        if (anchor != null) {
            showAt(anchor);
        } else {
            showAtCenter();
        }
        
        if (mLayoutListener == null) {
            mLayoutListener = new GlobalLayoutListener(anchor);
        }
        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
        
		if (mRootView instanceof QuickActionRelativeLayout) {
	        ((QuickActionRelativeLayout)mRootView).setDispatchKeyEventListener(new QuickActionRelativeLayout.OnDispatchKeyEventListener() {

	            @Override
	            public void onDispatchKeyEvent(KeyEvent event) {
	                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && mWindow.isShowing()) {
	                    dismiss();
	                    if (mContext instanceof SendCommentsToDeveloper) {
	            			((SendCommentsToDeveloper) mContext).isPopupClosed(true);
	            		}

	                }
	            }
	        });
	    }  
    }
    
	private void showAt(View anchor) {
			    int xPos, yPos, arrowPos;
	    int[] location = new int[2];
        final View anc = anchor;
        anchor.getLocationOnScreen(location);
        
        int leftPadding = anchor.getPaddingLeft();
        int rightPadding = anchor.getPaddingRight();

        Rect anchorRect = new Rect(location[0] + leftPadding, location[1],
                location[0] + (anchor.getWidth() - leftPadding * 2),
                location[1] + anchor.getHeight());

        mQuickMenuView.measure(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);

        int rootHeight = mQuickMenuView.getMeasuredHeight();

        if (rootWidth == 0) {
            rootWidth = mQuickMenuView.getMeasuredWidth();
        }

        int screenWidth = mWindowManager.getDefaultDisplay().getWidth();
        int screenHeight = mWindowManager.getDefaultDisplay().getHeight();

        
        
        if ((anchorRect.left + rootWidth) > screenWidth) {
            xPos = anchorRect.left - (rootWidth - anchor.getWidth());
            xPos = (xPos + rootWidth > screenWidth ) ? screenWidth - rootWidth : xPos;

            arrowPos = anchorRect.centerX() - xPos;

        } 
        else {
            if (anchor.getWidth() > rootWidth) {
                xPos = anchorRect.centerX() - (rootWidth / 2);
            } 
            else {
                xPos = anchorRect.left;
            }
            xPos = (xPos < 0) ? 0 : xPos;
            arrowPos = anchorRect.centerX() - xPos;
        }

        if (arrowPos <= 0) {
            arrowPos = 8;
        }

        /*
         * if (xPos == 0) { xPos = 4; }
         */

        int dyTop = anchorRect.top;
        int dyBottom = screenHeight - anchorRect.bottom;

        boolean onTop = (dyTop > dyBottom) ? true : false;

        if (onTop) {
            if (rootHeight > dyTop) {
                yPos = 15;
                LayoutParams l = mMenuLayout.getLayoutParams();
                l.height = dyTop - anchor.getHeight();
            } else {
                yPos = anchorRect.top - rootHeight;
            }
        } else {
            yPos = anchorRect.bottom;

            if (rootHeight > dyBottom) {
                LayoutParams l = mMenuLayout.getLayoutParams();
                l.height = dyBottom;
            }
        }

        showArrow(((onTop) ? R.id.arrow_down : R.id.arrow_up), arrowPos + 8);

        setAnimationStyle(screenWidth, anchorRect.centerX(), onTop);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)mQuickMenuView.getLayoutParams();
        lp.leftMargin = xPos;
        lp.topMargin = yPos;
        
        mQuickMenuView.requestLayout();
        if (!isShowing()) {
            mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0);
            
            mWindow.setFocusable(true);
        }
    }

    private void showAtCenter() {
	    hideArrow();
        //if no anchor is provided we display the popup at the center
        //filling entire parents width
        int padding = mContext.getResources().getDimensionPixelSize(R.dimen.quickActionPadding);
        LayoutParams lp = mMenuLayout.getLayoutParams();
        lp.width = LayoutParams.FILL_PARENT;
        
        lp = mTrack.getLayoutParams();
        lp.width = LayoutParams.FILL_PARENT;
        
        lp = mScroller.getLayoutParams();
        lp.width = LayoutParams.FILL_PARENT;
        
        Rect rectgle = new Rect();
        Window window = ((Activity)mContext).getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
        //there seems to be a bug in the way the PopUpWindow is vertically centered especially in
        //landscape mode so add padding at the top and bottom also
        mQuickMenuView.setPadding(padding, padding, padding, padding);
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams)mQuickMenuView.getLayoutParams();
        rlp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        rlp.addRule(RelativeLayout.CENTER_VERTICAL);
        
        mWindow.setFocusable(true);
        
        mWindow.showAtLocation(mRootView, Gravity.NO_GRAVITY, 0, 0);
    }

    private void hideArrow() {
    	
       mArrowUp.setVisibility(View.GONE);
       mArrowDown.setVisibility(View.GONE);
    }
	
    private void setAnimationStyle(int screenWidth, int requestedX, boolean onTop) {
    	
        int arrowPos = requestedX - mArrowUp.getMeasuredWidth() / 2;

        switch (mAnimStyle) {
        case ANIM_GROW_FROM_LEFT:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left
                    : R.style.Animations_PopDownMenu_Left);
            break;

        case ANIM_GROW_FROM_RIGHT:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right
                    : R.style.Animations_PopDownMenu_Right);
            break;

        case ANIM_GROW_FROM_CENTER:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center
                    : R.style.Animations_PopDownMenu_Center);
            break;

        case ANIM_REFLECT:
            mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Reflect
                    : R.style.Animations_PopDownMenu_Reflect);
            break;

        case ANIM_AUTO:
            if (arrowPos <= screenWidth / 4) {
                mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Left
                        : R.style.Animations_PopDownMenu_Left);
            } else if (arrowPos > screenWidth / 4 && arrowPos < 3 * (screenWidth / 4)) {
                mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Center
                        : R.style.Animations_PopDownMenu_Center);
            } else {
                mWindow.setAnimationStyle((onTop) ? R.style.Animations_PopUpMenu_Right
                        : R.style.Animations_PopDownMenu_Right);
            }

            break;
        }
    }

    private void showArrow(int whichArrow, int requestedX) {
    	
        final ImageView showArrow = (whichArrow == R.id.arrow_up) ? mArrowUp : mArrowDown;
        final ImageView hideArrow = (whichArrow == R.id.arrow_up) ? mArrowDown : mArrowUp;

        final int arrowWidth = mArrowUp.getMeasuredWidth();
        int resId;

        showArrow.setVisibility(View.VISIBLE);

        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams) showArrow.getLayoutParams();

        int arrowX = requestedX - arrowWidth - 1;

        if (arrowX <= 0) {
            arrowX = 1;
        }

        if (whichArrow == R.id.arrow_up) {
            resId = R.drawable.arrow_up;
        } else {
            resId = R.drawable.arrow_down;
        }
        showArrow.setImageResource(resId);

        param.leftMargin = arrowX;

        hideArrow.setVisibility(View.INVISIBLE);
    }

    public void setOnDismissListener(QuickAction.OnDismissListener listener) {
    	
        setOnDismissListener(this);

        mDismissListener = listener;
    }

    
    @Override
    public void onDismiss() {
    	if (mDismissListener != null) {
            mDismissListener.onDismiss();
        }
    }

    public interface OnActionItemClickListener {
    	
        public abstract void onItemClick(QuickAction source, int pos, int actionId);
    }

    public interface OnDismissListener {
        public abstract void onDismiss();
    }
    
    /**
     * This class/interface is used to redraw the QuickAction menu whenever there is a change in the
     * postion of the anchor view or the orientaion of the screen is changed   
     * @author Essack
     * @Since  Jun 21, 2012
     */
    class GlobalLayoutListener implements OnGlobalLayoutListener {
        View mAnchor;
        int  mPrevYPos;
        int  mPrevXPos;
        int  mPrevWidth;
        
        public GlobalLayoutListener(View anchor) {
        	
            mAnchor = anchor;
            
            if (mAnchor != null) {
                int[] location = new int[2];
                mAnchor.getLocationOnScreen(location);
           
                mPrevYPos = location[1];
            }
            
            mPrevWidth = mWindowManager.getDefaultDisplay().getWidth();
        }
        
        @Override
        public void onGlobalLayout() {
            if (mWindow.isShowing()) {
                if (mAnchor != null) {
                    int[] location = new int[2];
                    mAnchor.getLocationOnScreen(location);

                    if (mPrevYPos != location[1] || mPrevXPos != location[0]) {
                        show(mAnchor, null);
                    }
                    mPrevYPos = location[1];
                    mPrevXPos = location[0];
                } else {
                    int width = mWindowManager.getDefaultDisplay().getWidth();
                    
                    if (mPrevWidth != width) {
                        show(null, null);
                        mPrevWidth = width;
                    }
                }
                
            }
        }
        
        public void setAnchor(View anchor) {
            mAnchor = anchor;
        }
        
        public void setPrevYPos(int y) {
            mPrevYPos = y;
        }
        
        public void setPrevWidth(int width) {
            mPrevWidth = width;
        }
    }
   
}