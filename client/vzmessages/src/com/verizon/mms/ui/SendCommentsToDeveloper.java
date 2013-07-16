package com.verizon.mms.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.widget.ActionItem;
import com.verizon.mms.ui.widget.QuickAction;
import com.verizon.mms.ui.widget.QuickAction.OnActionItemClickListener;
import com.verizon.mms.util.Util;
import com.verizon.network.SuggestionService;

public class SendCommentsToDeveloper extends VZMActivity {
	private EditText commentsTextField;
	private Button sendCommentsButton;
	private Button cancelCommentsButton;
	private TextView subjecTextView;
	private SharedPreferences mPreferences;
	private static final int MENU_APP_IS_WORKING_GREAT = 1001;
	private static final int MENU_APP_CRASHING_FOR_ME_WHEN = 1002;
	private static final int MENU_APP_SOMETIMES_STOPS_WORKING = 1003;
	private static final int MENU_I_WOULD_LIKE_YOU_TO_ADD = 1004;
	private static final int MENU_IT_IS_NOT_WORKING_WHEN_I_DO = 1005;
	private SharedPreferences.Editor editor;
	public static boolean popupStatus = false;
	private static final int NOTIFICATION_CANCEL_FLAG = 1;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.send_comments_to_developer);
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_CANCEL_FLAG);
		commentsTextField = (EditText) findViewById(R.id.sendCommentsEditText);
		subjecTextView = (TextView) findViewById(R.id.subjectTextView);
		subjecTextView
				.setText(R.string.send_comments_menu_app_is_working_great);
		subjecTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setConversationMenu();
			}
		});
		mPreferences = getPreferences(MODE_PRIVATE);
		editor = mPreferences.edit();
		subjecTextView.setFocusable(true);
		subjecTextView.requestFocus();

		if (!checkForPopUpWindow()) {
			subjecTextView.performClick();
			editor.putBoolean("isActivityLaunched", true);
			editor.commit();
		} else {
			commentsTextField.requestFocus();
			getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
		sendCommentsButton = (Button) findViewById(R.id.send_button);
		sendCommentsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				requestToSendComments();
				Util.hideKeyboard(SendCommentsToDeveloper.this, v);
				finish();
			}
		});
		cancelCommentsButton = (Button) findViewById(R.id.cancel_button);
		cancelCommentsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	public void requestToSendComments() {

		Bundle bundle = new Bundle();
		bundle.putString("subject", subjecTextView.getText().toString());
		bundle.putString("comment", commentsTextField.getText().toString());
		bundle.putInt("retry_counter", 0);
		if(Logger.IS_DEBUG_ENABLED){
			Logger.debug(SendCommentsToDeveloper.class, "feedback data ==>" + bundle);
		}
		Intent feedbackData = new Intent(SendCommentsToDeveloper.this,
				SuggestionService.class);
		feedbackData.putExtras(bundle);

		startService(feedbackData);

	}

	public void isPopupClosed(boolean closed) {
		popupStatus = closed;
		if(Logger.IS_DEBUG_ENABLED){
			Logger.debug(SendCommentsToDeveloper.class, "pop up window status ==>" + closed);
		}
		if (closed) {
			getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		}
	}

	private void setConversationMenu() {
		QuickAction mConvMenu = new QuickAction(SendCommentsToDeveloper.this);
		mConvMenu.setTitle(R.string.send_comments_menu_title);
		ActionItem actionItem;
		actionItem = new ActionItem(MENU_APP_IS_WORKING_GREAT,
				R.string.send_comments_menu_app_is_working_great, 0);
		mConvMenu.addActionItem(actionItem);

		actionItem = new ActionItem(MENU_APP_CRASHING_FOR_ME_WHEN,
				R.string.send_comments_menu_app_crashing_for_me_when, 0);
		mConvMenu.addActionItem(actionItem);

		actionItem = new ActionItem(MENU_APP_SOMETIMES_STOPS_WORKING,
				R.string.send_comments_menu_app_sometimes_stops_working, 0);
		mConvMenu.addActionItem(actionItem);

		actionItem = new ActionItem(MENU_I_WOULD_LIKE_YOU_TO_ADD,
				R.string.send_comments_menu_i_would_like_you_to_add, 0);
		mConvMenu.addActionItem(actionItem);

		actionItem = new ActionItem(MENU_IT_IS_NOT_WORKING_WHEN_I_DO,
				R.string.send_comments_menu_it_is_not_working_when_i_do, 0);
		mConvMenu.addActionItem(actionItem);

		mConvMenu.show(null, subjecTextView, true);
		mConvMenu.setOnActionItemClickListener(new OnActionItemClickListener() {

			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				// TODO Auto-generated method stub
				switch (actionId) {
				case MENU_APP_IS_WORKING_GREAT: {
					subjecTextView
							.setText(R.string.send_comments_menu_app_is_working_great);
					break;
				}
				case MENU_APP_CRASHING_FOR_ME_WHEN: {
					subjecTextView
							.setText(R.string.send_comments_menu_app_crashing_for_me_when);
					break;
				}
				case MENU_APP_SOMETIMES_STOPS_WORKING: {
					subjecTextView
							.setText(R.string.send_comments_menu_app_sometimes_stops_working);
					break;
				}
				case MENU_I_WOULD_LIKE_YOU_TO_ADD: {
					subjecTextView
							.setText(R.string.send_comments_menu_i_would_like_you_to_add);
					break;
				}
				case MENU_IT_IS_NOT_WORKING_WHEN_I_DO: {
					subjecTextView
							.setText(R.string.send_comments_menu_it_is_not_working_when_i_do);
					break;
				}

				}

			}
		});

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (!commentsTextField.getText().toString().equals("")) {
				sendCommentsToDeveloperDialog();
				return true;
			} else {
				finish();
				return true;
			}
		}
		return false;
	}

	private void sendCommentsToDeveloperDialog() {
		int title = R.string.menu_send_comments;
		int messageText = R.string.would_you_like_to_send_the_message;
		final AppAlignedDialog d = new AppAlignedDialog(
				SendCommentsToDeveloper.this, R.drawable.dialog_alert, title,
				messageText);

		Button sendButton = (Button) d.findViewById(R.id.positive_button);
		sendButton.setText(R.string.send_dialog_button);
		sendButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				requestToSendComments();
				d.dismiss();
				finish();
			}
		});
		Button deletelButton = (Button) d.findViewById(R.id.negative_button);
		deletelButton.setVisibility(View.VISIBLE);
		deletelButton.setText(R.string.delete_dialog_button);
		deletelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
				finish();
				
			}
		});
		d.show();
	}

	private boolean checkForPopUpWindow() {
//		if(Logger.IS_DEBUG_ENABLED){
//			Logger.debug(SendCommentsToDeveloper.class, "preference to check pop up ==>" + mPreferences.contains("isActivityLaunched"));
//		}
		return mPreferences.contains("isActivityLaunched");
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		editor.remove("isActivityLaunched");
		editor.commit();
	}
	
	

}
