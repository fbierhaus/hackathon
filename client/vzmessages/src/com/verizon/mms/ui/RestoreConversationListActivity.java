/**
 * MessageRestoreListActivity.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.rocketmobile.asimov.Asimov;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.adapter.RestoreConversationAdapter;
import com.verizon.mms.util.RestoreConversationTask;
import com.verizon.vzmsgs.saverestore.BackUpMessage;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl;
import com.verizon.vzmsgs.saverestore.PopUpUtil;
import com.verizon.vzmsgs.saverestore.SDCardStatus;
import com.verizon.vzmsgs.saverestore.SDCardStatusListener;

/**
 * 
 * This class/interface
 * 
 * @author Imthiaz
 * @Since Sep 10, 2012
 */
public class RestoreConversationListActivity extends VZMActivity implements
		OnItemClickListener {

    private View mSyncProgressView;
	private ListView msgRestoreList;
	private Button restoreButton;
	private Button selectAllButton;
	private Button unSelectAllButton;
	private ImageView headerView;
	private ContactImage contactImage;
	private FromTextView recipientName;
	private RestoreConversationAdapter mListAdapter;
	private ArrayList<Object> msgThreads;
	private ArrayList<Object> cacheRecipients;
	private HashMap<String,String> dateList;
	private PopUpUtil util;
	private String filePath;
	private String recipients;
	private ArrayList<String> rList;
	private BackupManagerImpl mgr;
	private SharedPreferences prefs;
	private TextView footer;
	private OpenConversationTask task;
	private boolean isTaskCancelled;
	private String previousRecipient;
	private HashMap<String, String> recipientList;
	private static boolean isTaskCompleted = false;
	SDCardStatusListener mCardStatusListener =  null;
	boolean isSDCardUnMounted = true;
	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.message_restore);
		msgRestoreList = (ListView) findViewById(R.id.messageRestoreList);
		headerView = (ImageView) findViewById(R.id.headerView);
		recipientName = (FromTextView) findViewById(R.id.contactname);
		restoreButton = (Button) findViewById(R.id.restoreButton);
		selectAllButton = (Button) findViewById(R.id.rSelectAllButton);
		selectAllButton.setVisibility(View.GONE);
		unSelectAllButton = (Button) findViewById(R.id.rUnselectAllButton);
		contactImage = (ContactImage) findViewById(R.id.restorecontact);
		mSyncProgressView = findViewById(R.id.sync_progressBar);
		mgr = new BackupManagerImpl(this);
		util = new PopUpUtil(this, true);
		Intent passedIntent = getIntent();
		filePath = passedIntent.getStringExtra("filePath");
		recipients = passedIntent.getStringExtra("recipients");
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		dateList = new HashMap<String, String>();
		msgThreads = new ArrayList<Object>();
		LayoutInflater inflator = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view =  inflator.inflate(R.layout.loading_layout, null);
		footer = (TextView) view.findViewById(R.id.loading_text);
		msgRestoreList.addFooterView(footer);
		recipientList = new HashMap<String, String>();
		
		task = new OpenConversationTask();
		task.execute();
		
		mCardStatusListener = new SDCardStatusListener(this, new SDCardStatus() {
			@Override
			public void status(String status) {
				if(Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(),"SDCard Unmounted 2...");
				}
				if(msgRestoreList != null && task != null) {
					msgRestoreList.setAdapter(null); 
					isTaskCancelled = task.cancel(true);
				}
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

	
	/**
	 * This Method creates conversations list
	 */
	private void setUpConversationsList(ArrayList<Object> threads) {
		if (mListAdapter == null) {
			selectAllButton.setVisibility(View.VISIBLE);
			unSelectAllButton.setText(getString(R.string.unselect_all));
			headerView.setVisibility(View.VISIBLE);
			contactImage.setVisibility(View.GONE);
			recipientName.setVisibility(View.GONE);
			unSelectAllButton.setVisibility(View.VISIBLE);
			unSelectAllButton.setEnabled(false);
			selectAllButton.setVisibility(View.GONE);
			msgRestoreList.setOnItemClickListener(this);
			mListAdapter = new RestoreConversationAdapter(
					RestoreConversationListActivity.this, threads, dateList, restoreButton, unSelectAllButton, selectAllButton, true);
			msgRestoreList.setStackFromBottom(false);
			msgRestoreList.setAdapter(mListAdapter);
			
		}
	}

	
	public void onViewClick(View view) {

		int viewID = view.getId();
        if (msgThreads == null && cacheRecipients == null) {
        	util.fileEmptyPopup(RestoreConversationListActivity.this);
        } else if (viewID == R.id.restoreButton) {
        	
        	if (mListAdapter != null) {
        		int progressState = prefs.getInt(util.PROGRESS_KEY, 0);
				if (progressState == 1) {
					util.showErrorDialog(RestoreConversationListActivity.this,
							getString(R.string.backup_alert_title),
							getString(R.string.backup_save_alert_text));
				} else if (progressState == 2) {
					util.showErrorDialog(RestoreConversationListActivity.this,
							getString(R.string.backup_alert_title),
							getString(R.string.backup_restore_alert_text));
				} else {
					if (task != null) {
						task.cancel(true);
					}
					new RestoreConversationTask(RestoreConversationListActivity.this, restoreButton,
							mListAdapter.getSelectedThreadIDs(), filePath, mgr)
							.execute();
				}
			}
		} else if (viewID == R.id.rSelectAllButton) {
			unSelectAllButton.setVisibility(View.VISIBLE);
			selectAllButton.setVisibility(View.GONE);
			if (mListAdapter != null && rList != null) {
				mListAdapter.setAllItemsChecked(rList);
			}
			
		} else if (viewID == R.id.rUnselectAllButton) {
			if (mListAdapter != null) {
				mListAdapter.setAllItemsUnChecked();
				unSelectAllButton.setVisibility(View.GONE);
				selectAllButton.setVisibility(View.VISIBLE);
			}
		}
	}

	class OpenConversationTask extends AsyncTask<Void, Void, Integer> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mSyncProgressView.setVisibility(View.VISIBLE);

		}
		@Override
		protected void onCancelled() {
			super.onCancelled();
			msgRestoreList.setAdapter(null);
		}

		@Override
		protected Integer doInBackground(Void... params) {
			
			try {
				mgr.getConversations(filePath, mParsePreviewListener, null, true);
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				mListAdapter = null ; 
				recipientList = null;
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				mListAdapter = null ; 
				recipientList = null;
				return 3; //Invalid XML Version
			} catch (SAXException e) {
				e.printStackTrace();
				/*mListAdapter = null ; 
				recipientList = null;
				return 3; //Invalid XML Version*/			
			} catch (IOException e) {
				e.printStackTrace();
				mListAdapter = null ; 
				recipientList = null;
			} catch (Exception e) {
				e.printStackTrace();
				mListAdapter = null ; 
				recipientList = null;
			}
				
			return mgr.getRestoreStatus();
		}

		@Override
		protected void onPostExecute(Integer result) {
			isTaskCompleted = true;
			if (mListAdapter != null && recipientList != null) {
				Set<String> entries = recipientList.keySet();
				Iterator<String> iterator = entries.iterator();
				rList = new ArrayList<String>();
				while (iterator.hasNext()) { 
					String key = iterator.next();
					rList.add(recipientList.get(key));
				}
				mListAdapter.setAllItemsChecked(rList);
				
			}
			unSelectAllButton.setVisibility(View.VISIBLE);
			mSyncProgressView.setVisibility(View.GONE);
			unSelectAllButton.setEnabled(true);
			restoreButton.setEnabled(true);
			selectAllButton.setVisibility(View.GONE);
			if (footer != null && msgRestoreList != null) {
				if (msgRestoreList.getAdapter() != null) {
					msgRestoreList.removeFooterView(footer);	
				}
			}
			switch (result) {
			case 2:
				if (msgRestoreList != null) {
					msgRestoreList.setVisibility(View.GONE);
				}
				util.fileEmptyPopup(RestoreConversationListActivity.this);
				break;

			case 3:
				if (msgRestoreList != null) {
					msgRestoreList.setVisibility(View.GONE);	
				}
				util.verNotSupportedPopup(RestoreConversationListActivity.this);
				break;
			default:
				break;
			}

			super.onPostExecute(result);
		}
	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see
	 * android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget
	 * .AdapterView, android.view.View, int, long)
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		
		int progressState = prefs.getInt(util.PROGRESS_KEY, 0);
		if (progressState == 1) {
			util.showErrorDialog(RestoreConversationListActivity.this,
					getString(R.string.backup_alert_title),
					getString(R.string.backup_save_alert_text));
		} else if (progressState == 2) {
			util.showErrorDialog(RestoreConversationListActivity.this,
					getString(R.string.backup_alert_title),
					getString(R.string.backup_restore_alert_text));
		} else  {
			if (isTaskCompleted && msgRestoreList.getFooterViewsCount() == 0) {
				recipients = (String) msgRestoreList.getItemAtPosition(pos);
				Intent msgItemsIntent = new Intent(RestoreConversationListActivity.this, RestoreMessagesActivity.class);
				msgItemsIntent.putExtra("filePath", filePath);
				msgItemsIntent.putExtra("recipients", recipients);
				startActivityForResult(msgItemsIntent,SDCardStatus.SD_CARD_EJECTED);
			}
			
		}

	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(),"SD_CARD_UNMOUNTED_CLOSE_ACTIVITY");
		}
		if(requestCode == SDCardStatus.SD_CARD_EJECTED && resultCode == SDCardStatus.SD_CARD_UNMOUNTED_CLOSE_ACTIVITY) {
			setResult(resultCode);
			finish();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (task != null) {
					isTaskCancelled = task.cancel(true);
				}
		}
		
		return super.onKeyDown(keyCode, event);
	}

	
	public interface ParsePreviewListener {
		public void updatePreviewListArrayList(BackUpMessage msgThreads);
		public void updatePreviewMessageList(BackUpMessage messages);
		public boolean isCancelled();
	}

	public final ParsePreviewListener mParsePreviewListener = new ParsePreviewListener() {
		
		public void updatePreviewListArrayList(BackUpMessage message) {
            getRecipientFromMessage(message);
              
		}

		@Override
		public void updatePreviewMessageList(BackUpMessage messages) {
			  //not needed in case of conversation list view
		}

		@Override
		public boolean isCancelled() {
			
			return isTaskCancelled;
		}

		
	};
	
	
	
	
	
	private HashMap<String, String> getRecipientFromMessage(final BackUpMessage message) {
	 
		if(isSDCardUnMounted) {
	    String currentRecipient = message.getRecipients();
		if (!recipientList.containsValue(currentRecipient)) {

			recipientList.put(message.getPduData().get("date"),
					message.getRecipients());
			previousRecipient = currentRecipient;
			runOnUiThread(new Runnable() {
				public void run() {
					dateList.put(message.getRecipients(), message.getPduData()
							.get("date"));
					msgThreads.add(message.getRecipients());
					setUpConversationsList(msgThreads);
					mListAdapter.notifyDataSetChanged();
				}
			});

		} else {
			dateList.put(message.getRecipients(),
					message.getPduData().get("date"));
			if (!currentRecipient.equalsIgnoreCase(previousRecipient)) {
				runOnUiThread(new Runnable() {
					public void run() {
						mListAdapter.notifyDataSetChanged();
					}
				});
			}
		}
	
		return recipientList;
		}
		else
		{
			msgThreads.clear();
			msgRestoreList.setAdapter(null);
			return null;
			
		}
	}

	}
