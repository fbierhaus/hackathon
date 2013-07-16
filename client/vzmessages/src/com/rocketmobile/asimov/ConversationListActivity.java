package com.rocketmobile.asimov;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.widget.RelativeLayout;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.transaction.MessagingNotification;
import com.verizon.mms.transaction.SmsRejectedReceiver;
import com.verizon.mms.ui.ComposeMessageActivity;
import com.verizon.mms.ui.ComposeMessageFragment;
import com.verizon.mms.ui.ComposeMessageFragment.ComposeMessageListener;
import com.verizon.mms.ui.ConversationListFragment;
import com.verizon.mms.ui.MenuSelectedListener;
import com.verizon.mms.ui.MessagingPreferenceActivity;
import com.verizon.mms.ui.TabletSharedContentFragment;
import com.verizon.mms.ui.TabletSharedContentFragment.TabletGalleryListener;
import com.verizon.mms.ui.activity.Provisioning;
import com.verizon.mms.ui.VZMFragmentActivity;
import com.verizon.mms.ui.WarnOfStorageLimitsActivity;
import com.verizon.mms.ui.widget.ActionItem;
import com.verizon.mms.util.Recycler;
import com.verizon.mms.util.Util;
import com.verizon.vzmsgs.saverestore.SDCardStatus;

public class ConversationListActivity extends VZMFragmentActivity implements ComposeMessageListener, TabletGalleryListener  {
	private SharedPreferences mPrefs;
	private Editor editor;
	private Handler mHandler;
	static private final String CHECKED_MESSAGE_LIMITS = "checked_message_limits";
	public ConversationListFragment mConversationListFragment;
    public ComposeMessageFragment mComposeMessageFragment;
    public FragmentTransaction mFragmentTransaction;
    public static boolean mIsTablet;
    public int mOrientation;
    private RelativeLayout mDivider;
    private View mComposeContainer;
    public long mLastOpenThreadID = -3l;
    public long mLastDeletedThreadID = -5l;  
    private boolean mDisableComposeFragment;
    private boolean visible;
    private boolean isGalleryViewOn = false;
    TabletSharedContentFragment mTabletSharedContentFragment = null;
    
    public static final String EXTRA_FROM_NOTIFICATION = "conversation_from_notification";
    public static final String EXTRA_LAUNCH_COMPOSE = "conversation_launch_compose";
    
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
	
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
			
		mIsTablet = Util.isMultiPaneSupported(this);
	    mHandler = new Handler();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = mPrefs.edit();
     	
	    initializeViews();
		
		boolean checkedMessageLimits = mPrefs.getBoolean(
				CHECKED_MESSAGE_LIMITS, false);
	    if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "checkedMessageLimits: " + checkedMessageLimits);
			FragmentManager.enableDebugLogging(true);
	    }
		// http://50.17.243.155/bugzilla/show_bug.cgi?id=233
		// if (!checkedMessageLimits || DEBUG) {
		// runOneTimeStorageLimitCheckForLegacyMessages();
		// }

		// Start ReportService only if this is LTE device.
		//if (MmsConfig.isLTE()) {
			// TODO - Animesh
			//startService(new Intent(ReportService.class.getName()));
		//}

	}
	
	 
	private void initializeViews() {
        setContentView(R.layout.conversation_screen);
        mConversationListFragment = (ConversationListFragment) getSupportFragmentManager().findFragmentById(
                R.id.conversationlistfragment);
        mConversationListFragment.init(this);

        if (mIsTablet) {
        	final View composeFragmentStub = findViewById(R.id.compose_screen_stub);
    		if (composeFragmentStub != null) {
    			((ViewStub) composeFragmentStub).inflate().setVisibility(View.VISIBLE);
    		}
    		
        	mComposeMessageFragment = (ComposeMessageFragment) getSupportFragmentManager().findFragmentById(
                    R.id.compose_fragment_c);
        	
            mComposeMessageFragment.init(this);
        }

        mDivider = (RelativeLayout) findViewById(R.id.conversationlist_divider);
        mComposeContainer = findViewById(R.id.composeFragments);
        if (!mIsTablet) {
            /*mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            mFragmentTransaction.hide(mComposeMessageFragment);
            mFragmentTransaction.remove(mComposeMessageFragment);*/
            
            mComposeMessageFragment = null;
            
            mDivider.setVisibility(View.GONE);
            /*mComposeContainer.setVisibility(View.GONE);
            try {
            	mFragmentTransaction.commit();
            }
            catch (Exception e) {
            	Logger.error(e);
            }*/
        }
        
        Configuration configuration = getResources().getConfiguration();
        mOrientation = configuration.orientation;
       
        setProgressBarVisibility(false);
        getIntent().putExtra("showKeyboard", false);
    }
	
	
	@Override
	protected void onNewIntent(Intent intent) {
		// Handle intents that occur after the activity has already been
		// created.
        if (mIsTablet) {
            setGalleryLoaded(false);
            mComposeMessageFragment.onNewIntent(intent);
            mLastOpenThreadID = intent.getLongExtra("thread_id", 0L);
        }
        
        mConversationListFragment.onNewIntent(intent);
        // if we're already started then we need to re-query
        if (visible) {
            mConversationListFragment.startAsyncQuery();
        }
    }
	
	
	protected void onStart() {
		super.onStart();
		visible = true;
		MessagingNotification.cancelNotification(getApplicationContext(),
				SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);
	}

	@Override
	protected void onStop() {
		super.onStop();
		visible = false;
		Util.forceHideKeyboard(this, mConversationListFragment.getView());
		if (mIsTablet) {
            getSupportFragmentManager().saveFragmentInstanceState(mComposeMessageFragment);
        }
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// We override this method to avoid restarting the entire
		// activity when the keyboard is opened (declared in
		// AndroidManifest.xml). Because the only translatable text
		// in this activity is "New Message", which has the full width
		// of phone to work with, localization shouldn't be a problem:
		// no abbreviated alternate words should be needed even in
		// 'wide' languages like German or Russian.
		//newConfig.locale = MessagingPreferenceActivity.getLocale(getApplicationContext());

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onConfigurationChanged: " + newConfig);
        }
	    MessagingPreferenceActivity.setLocale(getBaseContext());
        super.onConfigurationChanged(newConfig);
		
		mOrientation = newConfig.orientation;
	}
	
	/**
	 * Checks to see if the number of MMS and SMS messages are under the limits
	 * for the recycler. If so, it will automatically turn on the recycler
	 * setting. If not, it will prompt the user with a message and point them to
	 * the setting to manually turn on the recycler.
	 */
	public synchronized void runOneTimeStorageLimitCheckForLegacyMessages() {
		if (Recycler.isAutoDeleteEnabled(this)) {
		    if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), "recycler is already turned on");
		    }
			// The recycler is already turned on. We don't need to check
			// anything or warn
			// the user, just remember that we've made the check.
			markCheckedMessageLimit();
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				if (Recycler.checkForThreadsOverLimit(ConversationListActivity.this)) {
				    if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "checkForThreadsOverLimit TRUE");
				    }
					// Dang, one or more of the threads are over the limit. Show
					// an activity
					// that'll encourage the user to manually turn on the
					// setting. Delay showing
					// this activity until a couple of seconds after the
					// conversation list appears.
					mHandler.postDelayed(new Runnable() {
						public void run() {
							Intent intent = new Intent(
									ConversationListActivity.this,
									WarnOfStorageLimitsActivity.class);
							startActivity(intent);
						}
					}, 2000);
				} else {
				    if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), 
								"checkForThreadsOverLimit silently turning on recycler");
				}
					// No threads were over the limit. Turn on the recycler by
					// default.
					runOnUiThread(new Runnable() {
						public void run() {
							SharedPreferences.Editor editor = mPrefs.edit();
							editor.putBoolean(
									MessagingPreferenceActivity.AUTO_DELETE,
									true);
							editor.commit();
						}
					});
				}
				// Remember that we don't have to do the check anymore when
				// starting MMS.
				runOnUiThread(new Runnable() {
					public void run() {
						markCheckedMessageLimit();
					}
				});
			}
		}).start();
	}

	/**
	 * Mark in preferences that we've checked the user's message limits. Once
	 * checked, we'll never check them again, unless the user wipe-data or
	 * resets the device.
	 */
	private void markCheckedMessageLimit() {
	    if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "markCheckedMessageLimit");
	    }
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putBoolean(CHECKED_MESSAGE_LIMITS, true);
		editor.commit();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean isHandledByFragment = mConversationListFragment.onKeyDown(
				keyCode, event);
        boolean isHandledByComposeFragment = false;
        boolean isHandledByGalleryFragment = false;
        if (mIsTablet) {
            isHandledByComposeFragment = mComposeMessageFragment.onKeyDown(keyCode, event);
            if (mTabletSharedContentFragment != null && isGalleryViewOn) {
                isHandledByGalleryFragment = mTabletSharedContentFragment.onKeyDown(keyCode, event);
            }
        }
		if (isHandledByFragment||isHandledByComposeFragment||isHandledByGalleryFragment) {
			return true;
		}
		
        if(keyCode== KeyEvent.KEYCODE_BACK)
        {
            if (mIsTablet) {
                mComposeMessageFragment.onBackPressed();
            }
        }

		return super.onKeyDown(keyCode, event);
	}

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mIsTablet) {
            mComposeMessageFragment.onRestart();
        }
    }


    public static Intent createIntent(Context context, long threadId) {
        Intent intent = new Intent(context, ConversationListActivity.class);

        if (threadId > 0) {
            intent.setData(Conversation.getUri(threadId));
            intent.putExtra("thread_id", threadId);
            intent.putExtra("showKeyboard", false);
			intent.putExtra("is_compose", false);
        } else {
            intent.putExtra("is_compose", true);
        }
        return intent;
    }

    public static Intent createIntent(Context context, long threadId, boolean showKeyboard) {
        Intent intent = new Intent(context, ConversationListActivity.class);

        if (threadId > 0) {
            intent.setData(Conversation.getUri(threadId));
            intent.putExtra("thread_id", threadId);
			intent.putExtra("is_compose", false);
        } else {
            intent.putExtra("is_compose", true);  
        }

            intent.putExtra("showKeyboard", showKeyboard);

        return intent;
    }
    
    public static Intent createNotificationIntent(Context context, long threadId ,boolean showKeyboard) {
        Intent intent = new Intent(context, ConversationListActivity.class);

        intent.setData(Conversation.getUri(threadId));
        intent.putExtra("thread_id", threadId);
		intent.putExtra("is_compose", false);
        intent.putExtra("showKeyboard", showKeyboard);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        
        intent.putExtra(EXTRA_LAUNCH_COMPOSE, true);

        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent arg2) {
        // TODO: there seems to be a bug in the fragment support jar file
        // due to which we are fragments onActivityResult is not called. Check using another version of jar
    	if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(),"onActivityResult called: resultCode"+ resultCode + "request code:"+requestCode + "\t (requestCode & 0xffff) :"+(requestCode & 0xffff) );
		}
    	if(requestCode == ConversationListFragment.REQUEST_CODE_FINISH_ACTIVITY
    			|| ((requestCode & 0xffff) == ConversationListFragment.REQUEST_CODE_FINISH_ACTIVITY)){
        	if(Logger.IS_DEBUG_ENABLED) {
    			Logger.debug(getClass(),"starting Provisioning activity");
    		}
    		Intent i = new Intent(this, Provisioning.class);
       	    i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        startActivity(i);
	        finish();
    	}
    	if(resultCode == SDCardStatus.SD_CARD_UNMOUNTED_CLOSE_ACTIVITY) {
    		if(mConversationListFragment != null)
    			mConversationListFragment.onActivityResult(requestCode, resultCode, arg2);
        }
    	if (MmsConfig.isTabletDevice()) {
			if (requestCode == MessagingPreferenceActivity.REQUEST_WIFI_SYNC_PAIRING){			
				mConversationListFragment.onActivityResult(requestCode, resultCode, arg2);
			} else {
                if (mIsTablet) { // Only if tab UI is loaded. ( in case of 7'tab its false.
                    mComposeMessageFragment.onActivityResult(requestCode, resultCode, arg2);
                }
    		}
        
        } 
    		
        Util.hideKeyboard(this, mDivider);
    }


    @Override
    public void finished() {
        if (!mIsTablet) {
            ConversationListActivity.this.finish();
        } else {
            try {
//                ft = getSupportFragmentManager().beginTransaction();
//                ft.hide(mComposeMessageFragment);
//                ft.commit();

            }
            catch (Exception e) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "Fragment error received");
                }
            }
        }
    }

    public void launchComposeView(Activity mActivity, long threadId) {

        if (mIsTablet) {
            launchComposeView(ConversationListActivity.createIntent(mActivity, threadId));
        } else {
            startActivity(ComposeMessageActivity.createIntent(mActivity, threadId));
        }
    }

    public void launchComposeView(Activity mActivity, long threadId, boolean showKeyboard) {
        if (mIsTablet) {
            launchComposeView(ConversationListActivity.createIntent(mActivity, threadId, showKeyboard));
        } else {
            startActivity(ComposeMessageActivity.createIntent(mActivity, threadId, showKeyboard));
        }

    }


    @Override
    public Intent getIntentFromParent(Context mActivity, long threadId) {
        if (mIsTablet) {
            return ConversationListActivity.createIntent(mActivity, threadId);
        } else {
            return ComposeMessageActivity.createIntent(mActivity, threadId);
        }
    }

    public static Intent getIntentFromParent(Context activity, long threadId, boolean keyboard) {
        if (mIsTablet) {
        	return ConversationListActivity.createIntent(activity, threadId, keyboard);
        } else {
            return ComposeMessageActivity.createIntent(activity, threadId, keyboard);
        }
    }
    
    public static Intent getNotificationIntentFromParent(Context activity, long threadId, boolean keyboard) {
        if (MmsConfig.isTabletDevice() && Util.isMultipane(activity)) {
        	return ConversationListActivity.createIntent(activity, threadId, keyboard);
        } else {
            return ConversationListActivity.createNotificationIntent(activity, threadId, keyboard);
        }
    }

    @Override
    public void launchComposeView(Intent mIntent) {
        long threadId;
        long newThreadId;
        if (mIsTablet) {

            // Check if Change is required or just needs to be displayed
            threadId = mIntent.getLongExtra("thread_id", 0L);
            newThreadId = getIntent().getLongExtra("thread_id", 0L);
            mLastOpenThreadID = threadId;
            if (isGalleryViewOn && newThreadId != 0l
                    && !mIntent.getBooleanExtra("is_compose", false)) {
                loadTabGalleryView(threadId, true);
            } else if ((threadId != newThreadId)
                    || mIntent.getBooleanExtra("is_compose", false)) {

                if (isGalleryViewOn) // we received a compose request
                {
                    loadTabGalleryView(threadId, false);
                    mComposeMessageFragment.onMessageChanged();
                    mComposeMessageFragment.onNewIntent(mIntent);
                } else {
                    mFragmentTransaction = getSupportFragmentManager().beginTransaction();
                    mFragmentTransaction.show(mComposeMessageFragment);
					try {
                    	mFragmentTransaction.commit();
					} catch (Exception e) {
	            		Logger.error(e);
	            	}
                    mComposeMessageFragment.onMessageChanged();
                    mComposeMessageFragment.onNewIntent(mIntent);
                }

                isGalleryViewOn = false;
                mLastOpenThreadID = threadId;
            }

        } else {
            startActivity(mIntent);
        }
    }

    @Override
    public void onDeleteGoToNext(long threadId) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ConversationListActivity.class,
                    "onDeleteGoToNext request came from compose fragment  " + threadId);
        }
        // we trust the value of compose fragment as on message sent the a new thread ID is assigned which is
        // not know to us.
        mLastDeletedThreadID = threadId;
        mLastOpenThreadID = threadId;
        mConversationListFragment.batchCount = 1;
        mConversationListFragment.onDeleteSelectAnother();

    }

    @Override
    public void updateCurrentThreadID(long threadId, String calledFrom) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ConversationListActivity.class, "Setting current open thread ID to " + threadId);
        }
        if (isGalleryViewOn && calledFrom.equals(mComposeMessageFragment.getClass().toString())) {
            return;
        }
        mLastOpenThreadID = threadId;
        mConversationListFragment.setSelected(threadId);
    }

    @Override
    public void onActionItemSelected(ActionItem item) {
        boolean handled = false;
        
        if (mConversationListFragment != null) {
            MenuSelectedListener menuListener = (MenuSelectedListener)mConversationListFragment;
            handled = menuListener.onActionItemSelected(item);
        }
        
        if (!handled) {
            if (mComposeMessageFragment != null) {
                MenuSelectedListener menuListener = (MenuSelectedListener)mComposeMessageFragment;
                handled = menuListener.onActionItemSelected(item);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = false;
        ActionItem actionItem = new ActionItem(item.getItemId(), 0, 0);
        actionItem.setTag(item.getIntent());
        
        if (mConversationListFragment != null) {
            MenuSelectedListener menuListener = (MenuSelectedListener)mConversationListFragment;
            handled = menuListener.onActionItemSelected(actionItem);
        }
        
        if (!handled) {
            if (mComposeMessageFragment != null) {
                MenuSelectedListener menuListener = (MenuSelectedListener)mComposeMessageFragment;
                handled = menuListener.onActionItemSelected(actionItem);
            }
        }
        return handled;
    }

    public void disableFragments() {
        mDisableComposeFragment = true;
        
        if (mIsTablet) {
        	findViewById(R.id.disable_compose).setVisibility(View.VISIBLE);
        }
    }
    
    public void enableFragments() {
        mDisableComposeFragment = false;
        
        if (mIsTablet) {
        	findViewById(R.id.disable_compose).setVisibility(View.GONE);
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mActionMenuBuilder.setMenu(menu);
        mActionMenuBuilder.init();
        if (mConversationListFragment != null) {
            mConversationListFragment.onPrepareActionMenu(mActionMenuBuilder);
        }
        if (mComposeMessageFragment != null && !mDisableComposeFragment) {
            mComposeMessageFragment.onPrepareActionMenu(mActionMenuBuilder);
        }
        return true;
    }

    public void loadTabGalleryView(long ThreadID, boolean enabled) {
        if (enabled) {
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            Intent i = getIntent();
            i.putExtra("thread_id", ThreadID);
            mComposeMessageFragment.onNewIntent(i);
            if (ThreadID != 0) {
                i.putExtra("members", Conversation.get(this, ThreadID, false).getRecipients().getNumbers());
            }
            if (mTabletSharedContentFragment == null) {
                mTabletSharedContentFragment = new TabletSharedContentFragment(this);
                // mFragmentTransaction.setCustomAnimations(android.R.anim.slide_in_left,
                // android.R.anim.slide_out_right);
                mFragmentTransaction.add(R.id.compose_fragment_c, mTabletSharedContentFragment);
                
            } else {
                if (mTabletSharedContentFragment.isAdded()) {
                    mTabletSharedContentFragment.onRestart(this);
                    mFragmentTransaction.show(mTabletSharedContentFragment);
                } else {
                    mTabletSharedContentFragment.onRestart(this);
                    mFragmentTransaction.remove(mTabletSharedContentFragment);
                    mFragmentTransaction.add(R.id.compose_fragment_c, mTabletSharedContentFragment)
                            .addToBackStack(null);
                }
            }
            mFragmentTransaction.setCustomAnimations(android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right);
            mFragmentTransaction.commit();
            isGalleryViewOn = true;
        } else {
            mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            if (mTabletSharedContentFragment != null && mTabletSharedContentFragment.isAdded()) {
                mFragmentTransaction.remove(mTabletSharedContentFragment);
                mTabletSharedContentFragment.onStopStub();// closing gallery being paranoid and closing all connections.
                mTabletSharedContentFragment = null;
            }
            mFragmentTransaction.show(mComposeMessageFragment);
            mFragmentTransaction.commitAllowingStateLoss();
            isGalleryViewOn = false;
        }
        
        if(isGalleryViewOn){
        	mComposeMessageFragment.hideQuickBrowseList();
        }
        else
        {
        	mComposeMessageFragment.showQuickBrowseList();
        }
    }

    @Override
    public void setGalleryLoaded(boolean enable) {

        boolean isStateChange = (enable != isGalleryViewOn);
        isGalleryViewOn = enable;
        if (!enable && isStateChange) {
            loadTabGalleryView(mLastOpenThreadID, enable);
        }

    }

    @Override
    public void launchShareMenu() {
        mComposeMessageFragment.getView().findViewById(R.id.btnAttach).performClick();
    }
}
