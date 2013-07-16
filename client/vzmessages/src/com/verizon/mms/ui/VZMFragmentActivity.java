package com.verizon.mms.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.VZActivityHelper.ActivityState;
import com.verizon.mms.ui.widget.ActionItem;
import com.verizon.mms.ui.widget.QuickAction;
import com.verizon.mms.ui.widget.QuickAction.OnActionItemClickListener;

public abstract class VZMFragmentActivity extends FragmentActivity  implements ActivityState {
	int state = ActivityState.ACTIVITY_STATE_NONE;

    // Maximum menu options that can be displayed in android native menu
    private static final int MAX_DISPLAY_MENU = 6;

    protected ActionMenuBuilder mActionMenuBuilder = null;

    // For Tablet 

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        MessageUtils.setTheme(this);
		
        VZActivityHelper.activityCreated(this);
        
        mActionMenuBuilder = new ActionMenuBuilder(this, null, mActionItemClick);
//        if (ApplicationSettings.isVMASyncEnabled()) {
//            vmaFlowManager = new VMAProvisionFlowHelper(this);
//            if(MmsConfig.isTabletDevice()){
//                vmaFlowManager.start();
//            }
//        } else {
//            // Wifi Sync flow
//        }

    }
	
	@Override
	protected void onStart() {
		super.onStart();
		state = ActivityState.ACTIVITY_START;
		VZActivityHelper.activityStarted(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		state = ActivityState.ACTIVITY_STOP;
		VZActivityHelper.activityStoped(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		state = ActivityState.ACTIVITY_RESUMED;
		VZActivityHelper.activityStarted(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		state = ActivityState.ACTIVITY_PAUSED;
		VZActivityHelper.activityStoped(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		VZActivityHelper.activityDestroyed(this);
	}
	
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		
		if (hasFocus) {
			VZActivityHelper.activityStarted(this);
		}
	}

	@Override
	public int getActivityState() {
		return state;
	}

	private OnActionItemClickListener mActionItemClick = new OnActionItemClickListener() {
        @Override
        public void onItemClick(QuickAction source, int pos, int actionId) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Selected menu item " + actionId);
            }
            ActionItem item = source.getActionItem(pos);
            onActionItemSelected(item);
        }
    };

    abstract public boolean onPrepareOptionsMenu(Menu menu);

    abstract public boolean onOptionsItemSelected(MenuItem item);

    /**
     * This function is called when QuickAction menu is selected
     */
    abstract protected void onActionItemSelected(ActionItem item);

    // TODO move this class to different file
    /*
     * This class is used to build the Options menu after the MAC_DISPLAY_MENU size is reached it adds the
     * menu items to QuickAction menu
     */
    public class ActionMenuBuilder {
        private Menu mMenu;
        private QuickAction mActionMenu;
        private Context mContext;
        private OnActionItemClickListener mClicklistener;

        public ActionMenuBuilder(Context context, Menu menu, OnActionItemClickListener clickListener) {
            mContext = context;
            mMenu = menu;
            mClicklistener = clickListener;
        }

        public void setMenu(Menu menu) {
            mMenu = menu;
        }

        /**
         * This function is called to add MenuItem to menu if the number of MenuItems are more or equal to
         * MAX_DISPLAY_MENU the extra Items will be added to mActionMenu menu
         */
        public void add(int group, int item, int order, int title, int icon, Intent intent) {
            int size = mMenu.size();
            boolean addtoQuickMenu = true;

            if (size < MAX_DISPLAY_MENU) {
                MenuItem menuItem = mMenu.add(group, item, order, title);
                if (icon != 0) {
                    menuItem.setIcon(icon);
                }
                if (intent != null) {
                    menuItem.setIntent(intent);
                }
                addtoQuickMenu = false;
            } 
            if (size == MAX_DISPLAY_MENU - 1) {
            	addtoQuickMenu = true;
            }
            if (size == MAX_DISPLAY_MENU && mActionMenu.getSize() == 1) {
            	// add the more menu
            	mMenu.removeItem(mActionMenu.getActionItem(0).getActionId());
                MenuItem more = mMenu.add(0, -1, 0, R.string.more).setIcon(R.drawable.ico_more_menu);
                more.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mActionMenu.show(null, VZMFragmentActivity.this.getCurrentFocus(), true);
                        return true;
                    }
                });
            }

            // menu item will be added to the QuickAction menu
            if (addtoQuickMenu) {
                ActionItem actionItem = new ActionItem(item, title, 0);
                actionItem.setTag(intent);
                mActionMenu.addActionItem(actionItem);
            }
        }

        /**
         * This function is called to add MenuItem to menu if the number of MenuItems are more or equal to
         * MAX_DISPLAY_MENU the extra Items will be added to mActionMenu menu
         */
        public void add(int group, int item, int order, String title, int icon, Intent intent) {
            int size = mMenu.size();
            boolean addtoQuickMenu = true;

            if (size < MAX_DISPLAY_MENU) {
                MenuItem menuItem = mMenu.add(group, item, order, title);
                if (icon != 0) {
                    menuItem.setIcon(icon);
                }
                if (intent != null) {
                    menuItem.setIntent(intent);
                }
                addtoQuickMenu = false;
            }
            if (size == MAX_DISPLAY_MENU - 1) {
            	addtoQuickMenu = true;
            }
            if (size == MAX_DISPLAY_MENU && mActionMenu.getSize() == 1) {
            	// add the more menu
            	mMenu.removeItem(mActionMenu.getActionItem(0).getActionId());
                MenuItem menuItem = mMenu.add(0, -1, 0, R.string.more).setIcon(R.drawable.ico_more_menu);
                menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (mActionMenu != null) {
                            mActionMenu.show(null, ((Activity) mContext).getCurrentFocus(), true);
                            return true;
                        }
                        return false;
                    }
                });
                addtoQuickMenu = false;
            }

            // menu item will be added to the QuickAction menu
            if (addtoQuickMenu) {
                ActionItem actionItem = new ActionItem(item, title, 0);
                actionItem.setTag(intent);
                mActionMenu.addActionItem(actionItem);
            }
        }

        public void add(int itemId, String title, int icon) {
            add(0, itemId, 0, title, icon, null);
        }

        public void add(int itemId, int title, int icon) {
            add(0, itemId, 0, title, icon, null);
        }

        public void init() {
            mActionMenu = new QuickAction(mContext);
            mActionMenu.setOnActionItemClickListener(mClicklistener);
            mActionMenu.setTitle(R.string.more);

            if (mMenu != null) {
                mMenu.clear();
            }
        }
    }

    public void updateCurrentThreadID(long threadId, String calledFrom) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
	public void onBackPressed() {
		try {
			//this might crash in honeycomb and greater devices
			super.onBackPressed();
		} catch (Exception e) {
			try {
				finish();
			} catch (Exception ex) {
				
			}
		}
	}
}
