/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.rocketmobile.asimov.Asimov;
import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.ui.ComposeMessageFragment.ComposeMessageListener;
import com.verizon.mms.ui.widget.ActionItem;
import com.verizon.mms.util.Util;

/**
 * This is the main UI for:
 * 1. Composing a new message;
 * 2. Viewing/managing message history of a conversation.
 *
 * This activity can handle following parameters from the intent
 * by which it's launched.
 * thread_id long Identify the conversation to be viewed. When creating a
 *         new message, this parameter shouldn't be present.
 * msg_uri Uri The message which should be opened for editing in the editor.
 * address String The addresses of the recipients in current conversation.
 * exit_on_sent boolean Exit this activity after the message is sent.
 */
public class ComposeMessageActivity extends VZMFragmentActivity 
		implements ComposeMessageListener {
	private static final String TAG = "ComposeMessageActivity";
	
	private ComposeMessageFragment mComposeMessageFragment = null;
	 public static KeyboardListener keyboardListener;


	public static final String PREPOPULATED_ADDRESS = "prepopulated_addresses";
	public static final String SEND_RECIPIENT       = "send_recipient";
	public static final String EXIT_ON_SENT         = "exit_on_sent";
	static final String FORWARD_MESSAGE             = "forwarded_message";
	static final String GROUP_MODE                  = "group_mode";
	private Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Util.isMultiPaneSupported( this)) {
            Intent messageIntent = getIntent();
            messageIntent.setClass(this, ConversationListActivity.class);
            startActivity(messageIntent);
            this.finish();
            //the fragment was getting initialized hence causing call to on activity attach 2 times.
            return;
        }
        
        setContentView(R.layout.compose_message_activity);

        /*
		 * setContentView with the msgcompose_activity <fragment/> calls the default constructor in MessageComposeFragment
		 * We must init() the members that were set by the non-default constructor.
		 */
		mComposeMessageFragment = (ComposeMessageFragment) getSupportFragmentManager().findFragmentById(R.id.compose_fragment);

		mComposeMessageFragment.init(this);
		
        setProgressBarVisibility(false);
        //In case of external intent launch panUI

    }

    @Override
    protected void onRestart() {
        super.onRestart();

        mComposeMessageFragment.onRestart();
    }
    

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isHandledByFragment = mComposeMessageFragment.onKeyDown(keyCode, event);
        
        if (isHandledByFragment) {
        	return true;
        }

        return false;
    }

    public long getThreadId() {
    	return mComposeMessageFragment.getConversation().getThreadId();
    }

    public static Intent createIntent(Context context, long threadId) {
        Intent intent = new Intent(context, ComposeMessageActivity.class);

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
    
    public static Intent createIntent(Context context, long threadId ,boolean showKeyboard) {
        Intent intent = new Intent(context, ComposeMessageActivity.class);

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
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mComposeMessageFragment != null) {
        	mComposeMessageFragment.onNewIntent(intent);
        }
    }

	@Override
	public void finished() {
		ComposeMessageActivity.this.finish();
	}
	
//	@Override
//	public void newCompose(Intent intent, boolean newComposer)
//	{
//		intent.setClass(getApplicationContext(), ComposeMessageActivity.class);
//		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//
//		startActivity(intent);
//	}

	@Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2) {
		//TODO: there seems to be a bug in the fragment support jar file
		//due to which we are fragments onActivityResult is not called. Check using another version of jar
		mComposeMessageFragment.onActivityResult(arg0, arg1, arg2);
	}


    @Override
    public Intent getIntentFromParent(Context mActivity, long threadId) {
        // TODO Auto-generated method stub
        return ComposeMessageActivity.createIntent(mActivity, threadId);
    }

    @Override
    public void launchComposeView(Intent mIntent) {
        startActivity(mIntent);
        
    }

    @Override
    public void onDeleteGoToNext(long threadId) {
        // TODO no use here this will close the activity anyway ( if not tab)
    }

    @Override
    public void updateCurrentThreadID(long threadId, String calledFrom) {
        // TODO no use here this will close the activity anyway ( if not tab)
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mActionMenuBuilder.setMenu(menu);
        mActionMenuBuilder.init();
        
        if (mComposeMessageFragment != null) {
            mComposeMessageFragment.onPrepareActionMenu(mActionMenuBuilder);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = false;
        ActionItem actionItem = new ActionItem(item.getItemId(), 0, 0);
        actionItem.setTag(item.getIntent());
        
        if (mComposeMessageFragment != null) {
            MenuSelectedListener menuListener = (MenuSelectedListener)mComposeMessageFragment;
            handled = menuListener.onActionItemSelected(actionItem);
        }
        return handled;
    }

    @Override
    protected void onActionItemSelected(ActionItem item) {
        if (mComposeMessageFragment != null) {
            MenuSelectedListener menuListener = (MenuSelectedListener)mComposeMessageFragment;
            menuListener.onActionItemSelected(item);
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onConfigurationChanged: " + newConfig);
        }
        MessagingPreferenceActivity.setLocale(getBaseContext());
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void setGalleryLoaded(boolean enable) {

    }
    public interface KeyboardListener{
		abstract void onChange();
		abstract void onMessageSend();
	}
	
	public static void setKBListener(KeyboardListener kbListener){
		keyboardListener = kbListener;
	}
}
