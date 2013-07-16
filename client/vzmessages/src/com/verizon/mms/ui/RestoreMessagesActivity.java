package com.verizon.mms.ui;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.verizon.mms.ui.FromTextView;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.ui.RestoreConversationListActivity.ParsePreviewListener;
import com.verizon.mms.ui.adapter.RestoreMessagesAdapter;
import com.verizon.mms.util.RestoreMessagesTask;
import com.verizon.vzmsgs.saverestore.BackUpMessage;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl;
import com.verizon.vzmsgs.saverestore.PopUpUtil;
import com.verizon.vzmsgs.saverestore.SDCardStatus;
import com.verizon.vzmsgs.saverestore.SDCardStatusListener;

public class RestoreMessagesActivity extends VZMActivity {
  
	private View mSyncProgressView;
	private ListView msgRestoreList;
	private Button restoreButton;
	private Button cancelButton;
	private Context mContext;
	private ImageView headerView;
	private ContactImage contactImage;
	private FromTextView recipientName;
	private RestoreMessagesAdapter mListAdapter;
	private ArrayList<BackUpMessage> backUpMessages;
	private BackupManagerImpl mgr;
	private PopUpUtil util;
	private String filePath;
	private String recipients;
	private boolean isTaskCancelled;
	private SharedPreferences prefs;
	private OpenConversationTask task;
	// Image Variables
	private Drawable defaultImage;
	private Drawable[] defaultImages;
	SDCardStatusListener mCardStatusListener = null;
	private boolean isSDCardMounted = true;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.restore_message_item);
		msgRestoreList = (ListView) findViewById(R.id.messageRestoreList);
		restoreButton = (Button) findViewById(R.id.msgrestoreButton);
		cancelButton = (Button) findViewById(R.id.rCancelButton);
		headerView = (ImageView) findViewById(R.id.headerView);
		contactImage = (ContactImage) findViewById(R.id.restorecontact);
		recipientName = (FromTextView) findViewById(R.id.contactname);
		mSyncProgressView = findViewById(R.id.sync_progressBar);
		mContext = RestoreMessagesActivity.this;
		mgr = new BackupManagerImpl(this);
		util = new PopUpUtil(this, true);
		Intent passedIntent = getIntent();
		filePath = passedIntent.getStringExtra("filePath");
		recipients = passedIntent.getStringExtra("recipients");
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		backUpMessages = new ArrayList<BackUpMessage>();
		msgRestoreList.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				msgRestoreList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
				
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				
				
			}
		});
		task = new OpenConversationTask();
		task.execute();
		mCardStatusListener = new SDCardStatusListener(this, new SDCardStatus() {
			@Override
			public void status(String status) {
				isSDCardMounted = false;
				if(Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(),"SDCard Unmounted 3...");
				}
				isTaskCancelled = task.cancel(true);
				msgRestoreList.setAdapter(null);
				setResult(SDCardStatus.SD_CARD_UNMOUNTED_CLOSE_ACTIVITY);
				finish();
			}
		});
		
	}
	@Override
	protected void onResume() {
		super.onResume();
		mCardStatusListener.registerSDCardStastusListener();
	}
	@Override
	protected void onPause() {
		super.onPause();
		mCardStatusListener.unRegisterSDCardStatusListener();
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (mListAdapter != null) {
			mListAdapter.onConfigChanged(newConfig);	
		}
		super.onConfigurationChanged(newConfig);
	}
	
	public void onViewClick(View view) {
		int viewID = view.getId();
		if (backUpMessages == null) {
			util.fileEmptyPopup(mContext);
		} else if (viewID == R.id.msgrestoreButton) {
			if (mListAdapter != null) {
				int progressState = prefs.getInt(util.PROGRESS_KEY, 0);
				if (progressState == 1) {
					util.showErrorDialog(mContext,
							getString(R.string.backup_alert_title),
							getString(R.string.backup_save_alert_text));
				} else if (progressState == 2) {
					util.showErrorDialog(mContext,
							getString(R.string.backup_alert_title),
							getString(R.string.backup_restore_alert_text));
				} else {
					new RestoreMessagesTask(mContext, restoreButton,
							backUpMessages, mgr).execute();
					
				}
			}
		} else if (viewID == R.id.rCancelButton) {
			finish();
		}
	}
	
	@Override
	protected void onDestroy() {
	
		if (mListAdapter != null) {
			mListAdapter.closeAdapter();
		}
		super.onDestroy();
	}
	
	
	/*@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (task != null) {
				isTaskCancelled = task.cancel(true);
			}
	}
		return super.onKeyDown(keyCode, event);
	}
	*/
	/**
	 * This Method creates messages of single conversation
	 */
	private void setUpMessagesList(ArrayList<BackUpMessage> messages) {
		if (mListAdapter == null) {

			cancelButton.setVisibility(View.VISIBLE);
			cancelButton.setText(getString(R.string.cancel));
			restoreButton.setEnabled(false);
			cancelButton.setEnabled(true);
			headerView.setVisibility(View.GONE);
			recipientName.setVisibility(View.VISIBLE);
			contactImage.setVisibility(View.VISIBLE);
			ContactList contactList = ContactList.getByNumbers(recipients.trim(), false, false);
			recipientName.setNames(contactList);
			updateTitle(contactList, recipientName);
			defaultImage = mContext.getResources().getDrawable(
					R.drawable.ic_contact_picture);
			updateContactImage(contactList, contactImage);
			boolean isGroup = contactList.size() > 1 ? true : false;
			mListAdapter = new RestoreMessagesAdapter(RestoreMessagesActivity.this, messages, msgRestoreList, filePath, isGroup);
			msgRestoreList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
			msgRestoreList.setAdapter(mListAdapter);
			
		}

	}
    
	public void updateContactImage(ContactList contactList,
			ContactImage mAvatarView) {
		if (contactList != null) {
			defaultImages = contactList
					.getImages(mContext, defaultImage, false);
			if (defaultImages == null) {
				defaultImages = new Drawable[] { defaultImage };
			}
			mAvatarView.setImages(defaultImages, defaultImage);

		}
	}
	
	public final ParsePreviewListener mParsePreviewListener = new ParsePreviewListener() {
		public void updatePreviewListArrayList(BackUpMessage message) {
            //Not needed in case of messages
        }

		@Override
		public void updatePreviewMessageList(BackUpMessage message) {
			 
			util.dismissProgressDialog();
			getMessagesFromRecipient(message);
		}

		@Override
		public boolean isCancelled() {
			// TODO Auto-generated method stub
			return isTaskCancelled;
		}

		
	};
	
	
	private void getMessagesFromRecipient(final BackUpMessage message) {
		if (isSDCardMounted) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (backUpMessages != null) {
						backUpMessages.add(message);
						setUpMessagesList(backUpMessages);
						mListAdapter.notifyDataSetChanged();		
					} 
				}
			});
		} else {
			msgRestoreList.setAdapter(null);
		}
	}
	
	class OpenConversationTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			cancelButton.setVisibility(View.VISIBLE);
			cancelButton.setEnabled(false);
			cancelButton.setText(getString(R.string.cancel));
			util.showProgressDialog(getString(R.string.load_progress_message));
			mSyncProgressView.setVisibility(View.VISIBLE);
		}

		@Override
		protected Void doInBackground(Void... params) {
			    
			try {
				mgr.getConversations(filePath, mParsePreviewListener, recipients, false);
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				mListAdapter = null ;backUpMessages = null;
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				mListAdapter = null ;backUpMessages = null;
			} catch (SAXException e) {
				e.printStackTrace();
				//mListAdapter = null ;backUpMessages = null;
			} catch (IOException e) {
				e.printStackTrace();
				mListAdapter = null ;backUpMessages = null;
			} catch (Exception e) {
				e.printStackTrace();
				mListAdapter = null ;backUpMessages = null;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void params) {
			if (mListAdapter != null && backUpMessages != null) {
				restoreButton.setEnabled(true);	
			}
			mSyncProgressView.setVisibility(View.GONE);
			if (backUpMessages !=  null && backUpMessages.size() == 0) {
				util.fileEmptyPopup(mContext);
			}
			super.onPostExecute(params);
			
		}
		@Override
		protected void onCancelled() {
			isTaskCancelled = true;
			super.onCancelled();
		}
	}
	
	@Override
	public void onBackPressed() {
		if (task != null) {
			isTaskCancelled = task.cancel(true);
			finish();
		}
		super.onBackPressed();
	}
	
	private void updateTitle(ContactList list, TextView title) {
		if (list.size() == 0) {
			String recipient = "";
			title.setText(recipient);

		} else {
			((FromTextView) title).setNames(list);
		}
	}
	
}
