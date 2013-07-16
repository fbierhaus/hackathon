/**
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Stack;
import org.json.JSONException;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.model.SaveRestoreItem;
import com.verizon.mms.ui.adapter.SaveRestoreAdapter;
import com.verizon.mms.util.RestoreMessagesTask;
import com.verizon.mms.util.Util;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl.BackUpStatusListener;
import com.verizon.vzmsgs.saverestore.MessageInfo;
import com.verizon.vzmsgs.saverestore.MessagesDaoImpl;
import com.verizon.vzmsgs.saverestore.PopUpUtil;
import com.verizon.vzmsgs.saverestore.SDCardStatus;
import com.verizon.vzmsgs.saverestore.SDCardStatusContainer;
import com.verizon.vzmsgs.saverestore.SDCardStatusListener;
import com.verizon.vzmsgs.saverestore.SDCardUnmountException;

/**
 * This class/interface
 * 
 * @author Imthiaz
 * @Since May 28, 2012
 */
public class SaveRestoreActivity extends VZMActivity implements
		OnItemClickListener {

	private ListView mList;
	private EditText fileNameText;
	private Button saveButton;
	private Button cancelButton;
	private CheckBox checkBox;
	private ImageView upArrowView = null;
	private ImageView titleImage;
	private LinearLayout saveLayout;
	private LinearLayout buttonLayout;
	private int mcontextMenuPosition = -1;

	// Identifiers for controlling save and restore behavior
	public static final int SAVE_THREAD_TO_XML = 0;
	public static final int SAVE_THREADS_TO_XML = 1;
	public static final int SAVE_ALL_THREADS_TO_XML = 2;
	public static final int RESTORE_FROM_XML = 3;
	public static final int SAVE_MSGS_TO_XML = 4;
	public static final int SAVE_MSG_TO_XML = 5;

	public static final String EXTRA_SAVE_TYPE = "savetype";
	public static final String EXTRA_THREAD_ID = "thread";
	public static final String EXTRA_MESSAGE_ID = "msgId";
	public static final String EXTRA_IS_MMS = "ismms";
	public static final String EXTRA_BATCH_THREADS = "batch_threads";
    private static final String BACKUP_ROOTPATH = "backup_root_path";
	private static final int MENU_OPEN = 0;
	private static final int MENU_RESTORE = 1;
	private static final int MENU_DELETE = 2;
	private static final int MENU_RENAME = 3;
	private static final int HANDLER_SAVE_SUCCEEDED = 5;
	private static final int HANDLER_INVALID_NAME = 6;
	private static final int HANDLER_SAVE_FILE_NOT_FOUND = 7;
	private static final int HANDLER_SAVE_FILE_NO_SD_CARD = 8;
	private static final int FILE_ALREADY_EXISTS = 9;
	private static final int UPDATE_COUNT = 10;
	private static final int FINISH_ACTIVITY = 11;
	private static final int SMS_DOES_NOT_EXISTS = 12;
	private static final int SAVE_FAILED = 13;
	private static final int SDCARD_INSUFFICIENT_SPACE = 14;
	private ArrayList<Long> batchThreadIds = null;
	private boolean canOverride;
	private boolean supportMMS;
	private String fileNameToSaveAs = null;
	private static GenerateXmlTask asyncTask;
	public static File root;
	public static File rootFile;
	public ProgressDialog pd;
	public SaveRestoreItem selectedItem;
	private SharedPreferences prefs;
	private Editor edit;
	private BackupManagerImpl manager;
	private PopUpUtil util;
	LoadConversationTask mConversationTask;
	SDCardStatusListener mCardStatusListener = null;

	FileFilter filter = new FileFilter() {

		@Override
		public boolean accept(File file) {
			if (file.isDirectory() && !file.isHidden()) {
				String data = file.getName();
				if (data.length() > 5) {
					data = data.substring(data.length() - 5);
					if (data.equalsIgnoreCase("parts")) {
						return false;
					} else {
						return true;
					}
				} else {
					return true;
				}

			} else if (file.getName().contains(".vzm")
					|| file.getName().contains(".xml")) {
				if (file.canWrite() && !file.isHidden() && file.canRead()) {
					return true;
				} else {
					return false;
				}

			}

			return false;
		}
	};

	private long messageId = -1;
	private long threadId = -1;
	private int saveType;
	private String sDate;
	private int seqNum = 1;
	private boolean isMms;
	private String defaultFileName;
	protected boolean countFlag = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.save_msgs_layout);
		saveLayout = (LinearLayout) findViewById(R.id.saveLayout);
		buttonLayout = (LinearLayout) findViewById(R.id.buttonLayout);
		mList = (ListView) findViewById(R.id.saveList);
		fileNameText = (EditText) findViewById(R.id.fileEdit);
		titleImage = (ImageView) findViewById(R.id.titleImage);
		saveButton = (Button) findViewById(R.id.msgSaveButton);
		cancelButton = (Button) findViewById(R.id.msgCancelButton);
		checkBox = (CheckBox) findViewById(R.id.sr_checkbox);
		pd = new ProgressDialog(this);
		manager = new BackupManagerImpl(this);
		util = new PopUpUtil(this, false);
		Intent intent = getIntent();
		supportMMS = true; // Default we save MMS
		batchThreadIds = (ArrayList<Long>) intent
				.getSerializableExtra(EXTRA_BATCH_THREADS);
		saveType = intent.getIntExtra(EXTRA_SAVE_TYPE, -1);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		edit = prefs.edit();
		if (saveType == SAVE_THREAD_TO_XML) {
			intent.getLongExtra(EXTRA_THREAD_ID, -1);
		} else if (saveType == SAVE_MSG_TO_XML) {
			messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1);
			threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1);
			isMms = intent.getBooleanExtra(EXTRA_IS_MMS, false);
		} else if (saveType == RESTORE_FROM_XML) {
			saveLayout.setVisibility(LinearLayout.GONE);
		}
		boolean isRestoreScreen = getIntent().getBooleanExtra(
				"showRestoreFiles", false);
		if (isRestoreScreen) {
			titleImage.setImageResource(R.drawable.saved_conversations);
			saveLayout.setVisibility(View.GONE);
			buttonLayout.setVisibility(View.GONE);
		} else {
			titleImage.setImageResource(R.drawable.save_conversations);
		}
		String storageState = Environment.getExternalStorageState();
		if (!(storageState.equals(Environment.MEDIA_MOUNTED))) {
			sdCardNotConnectedPopUp();
		}
		mCardStatusListener = new SDCardStatusListener(this,
				new SDCardStatus() {
					@Override
					public void status(String status) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), "SDCard Unmounted 1...");
						}
						if (mList != null && mConversationTask != null) {
							mList.setAdapter(null);
							mConversationTask.cancel(true);
							setResult(SDCardStatus.SD_CARD_UNMOUNTED_CLOSE_ACTIVITY);
							finish();
						}
					}
				});
		
		root = new File(Environment.getExternalStorageDirectory(),
				getString(R.string.save_folder_name));
		root.mkdir();
		root  = new File(prefs.getString(BACKUP_ROOTPATH, root.getPath()));
		mList.setOnItemClickListener(this);
		mList.setOnCreateContextMenuListener(this);
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton b, boolean isChecked) {
				if (isChecked) {
					supportMMS = false;
				} else {
					supportMMS = true;
				}
			}
		});

		// enable the up Arrow here since we know we're not at the top initially
		upArrowView = (ImageView) findViewById(R.id.up_arrow);
		upArrowView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				File path = root.getParentFile();
				if (path != null) {
					root = path;
					executeTask();

				} else {
					if (root.getAbsolutePath().equalsIgnoreCase("/mnt")) {
						root = Environment.getRootDirectory();
					} else {
						root = new File(Environment.getRootDirectory(), "mnt");	
					}
					
					executeTask();

				}
				if (root.getParentFile() == null) {
					upArrowView.setVisibility(View.GONE);
				} else {
					upArrowView.setVisibility(View.VISIBLE);
				}
				edit.putString(BACKUP_ROOTPATH, root.getPath());
			    edit.commit();
			}
		});
		// Check if file exist and act accordingly
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MMddyy");
		sDate = sdf.format(now);

		doSetUpButtons();
		executeTask();
	}

	private void setFileName() {
		for (int i = 0; i < mList.getCount(); i++) {
			SaveRestoreItem savedFiles = (SaveRestoreItem) mList
					.getItemAtPosition(i);
			String savedFile = savedFiles.getStringFilename();
			if (savedFile.contains(sDate)) {
				seqNum = seqNum + 1;
			}
		}
		fileNameToSaveAs = "msg-" + sDate + "-" + seqNum + ".vzm";
		checkFileExist(fileNameToSaveAs);
	}

	private void checkFileExist(String fileNameToSaveAs) {
		File file = new File(root, fileNameToSaveAs);
		while (file.exists()) {
			seqNum = seqNum + 1;
			fileNameToSaveAs = "msg-" + sDate + "-" + seqNum + ".vzm";
			file = new File(root, fileNameToSaveAs);
			continue;
		}
		defaultFileName = "msg-" + sDate + "-" + seqNum + ".vzm";
		fileNameText.setText(defaultFileName);
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

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		if (info.position < 0) {
			return;
		}

		mcontextMenuPosition = info.position;
		SaveRestoreItem item = (SaveRestoreItem) mList
				.getItemAtPosition(mcontextMenuPosition);
		menu.setHeaderTitle(item.getFilename());

		menu.add(0, MENU_RESTORE, 0, R.string.menu_restore);
		menu.add(0, MENU_OPEN, 0, R.string.menu_open);
		menu.add(0, MENU_DELETE, 0, R.string.menu_delete);
		menu.add(0, MENU_RENAME, 0, R.string.menu_rename);

	}

	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		selectedItem = (SaveRestoreItem) mList
				.getItemAtPosition(mcontextMenuPosition);
		int progressState = prefs.getInt(util.PROGRESS_KEY, 0);
		if (progressState == 1) {
			util.showErrorDialog(SaveRestoreActivity.this,
					getString(R.string.backup_alert_title),
					getString(R.string.backup_save_alert_text));
		} else if (progressState == 2) {
			util.showErrorDialog(SaveRestoreActivity.this,
					getString(R.string.backup_alert_title),
					getString(R.string.backup_restore_alert_text));

		} else {
			switch (menuItem.getItemId()) {
			case MENU_RESTORE:
				rootFile = root;
				String filePath = root.getAbsolutePath() + "/"
						+ selectedItem.getFilename().toString();
				RestoreMessagesTask restoreTask = new RestoreMessagesTask(
						SaveRestoreActivity.this, filePath);
				restoreTask.execute();
				break;
			case MENU_OPEN:
				startRestoreListActivity();
				break;
			case MENU_RENAME:
				renameFile();
				break;
			case MENU_DELETE:
				deleteFile();
				break;

			}
		}
		return super.onContextItemSelected(menuItem);
	}

	/**
	 * 
	 */
	private void startRestoreListActivity() {
		rootFile = root;
		Intent restoreIntent = new Intent(SaveRestoreActivity.this,
				RestoreConversationListActivity.class);
		restoreIntent.putExtra("filePath", root.getAbsolutePath() + "/"
				+ selectedItem.getFilename().toString());
		startActivityForResult(restoreIntent, SDCardStatus.SD_CARD_EJECTED);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "SD_CARD_UNMOUNTED_CLOSE_ACTIVITY");
		}
		if (requestCode == SDCardStatus.SD_CARD_EJECTED
				&& resultCode == SDCardStatus.SD_CARD_UNMOUNTED_CLOSE_ACTIVITY) {
			if (mList != null) {
				mList.setAdapter(null);
				mConversationTask.cancel(true);
				setResult(SDCardStatus.SD_CARD_UNMOUNTED_CLOSE_ACTIVITY);
				finish();
			}
		}
	}

	/**
	 * This Method
	 */
	private void doSetUpButtons() {
		saveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				int progressState = prefs.getInt(util.PROGRESS_KEY, 0);
				if (progressState == 1) {
					util.showErrorDialog(SaveRestoreActivity.this,
							getString(R.string.backup_alert_title),
							getString(R.string.backup_save_alert_text));
				} else if (progressState == 2) {
					util.showErrorDialog(SaveRestoreActivity.this,
							getString(R.string.backup_alert_title),
							getString(R.string.backup_restore_alert_text));
				} else {
					rootFile = root;
					asyncTask = new GenerateXmlTask();
					asyncTask.execute();
				}
				countFlag = false;
			}
		});
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.hideKeyboard(SaveRestoreActivity.this, fileNameText);
				finish();
			}
		});
	}

	class GenerateXmlTask extends AsyncTask<Void, Integer, Void> {
		SDCardStatusListener mSdCardStatusListener;
		private boolean isSDCardMounted = true;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(GenerateXmlTask.class,
						"GenerateXmlTask PreExecute()");
			}
			edit.putInt(util.PROGRESS_KEY, 1);
			edit.commit();

			SDCardStatusContainer runningProcess = new SDCardStatusContainer(
					this, manager);
			SDCardStatus.addRunningTask(runningProcess,
					GenerateXmlTask.class.toString());

			saveButton.setEnabled(false);
			util.showNotification(BackupManagerImpl.BSYNC_SAVING_CHANGES, 0, 0);
			super.onPreExecute();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(GenerateXmlTask.class, "onCancelled");
			}
			if (util != null && isSDCardMounted) {
				util.showNotification(BackupManagerImpl.BSYNC_SAVE_CANCELLED,
						0, 0);
			} else if (util != null && !isSDCardMounted) {
				util.showNotification(
						BackupManagerImpl.BSYNC_SD_CARD_UNMOUNTED, 0, 0);
			}
			if (edit != null) {
				edit.putInt(util.PROGRESS_KEY, 0);
				edit.commit();
			} else {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(GenerateXmlTask.class, "edit null");
				}
			}
			SDCardStatus.removeRunningTask(GenerateXmlTask.class.toString());
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (checkForValidFileName()) {
				String filePath = rootFile + "/" + fileNameToSaveAs;
				if (saveType == SAVE_THREADS_TO_XML) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(GenerateXmlTask.class,
								"SAVE_THREADS_TO_XML started");
					}
					ArrayList<Long[]> messageIDs = new ArrayList<Long[]>();
					for (Long threadID : batchThreadIds) {

						if (threadID != -1) { // check if it is a valid threadId
							for (MessageInfo msg : MessagesDaoImpl
									.getMessageIDs(SaveRestoreActivity.this,
											threadID)) {
								Long[] ids = new Long[] { msg.getId(),
										(long) (msg.isSMS() ? 1 : 0) };
								if (supportMMS) {
									messageIDs.add(ids);
								} else if (msg.isSMS()) {
									messageIDs.add(ids);
								}
							}
						}

					}
					if (messageIDs.size() == 0) {
						publishProgress(SMS_DOES_NOT_EXISTS);
						asyncTask.cancel(true);
						;
					} else {
						publishProgress(FINISH_ACTIVITY);
						try {
							manager.saveConversations(messageIDs, filePath,
									new BackUpStatusListener() {
										@Override
										public void updateStatus(int progress,
												int count) {
											publishProgress(UPDATE_COUNT,
													progress, count);
										}

										@Override
										public boolean isTaskCancelled() {
											if (isCancelled()) {
												return true;
											} else {
												return false;
											}

										}
									}, supportMMS);
							asyncTask.doProgress(HANDLER_SAVE_SUCCEEDED);
						}

						catch (FileNotFoundException e) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(GenerateXmlTask.class,
										"FileNotFoundException" + e);
							}
							asyncTask.cancel(true);
							;
							publishProgress(SDCARD_INSUFFICIENT_SPACE);
						} catch (IOException e) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(GenerateXmlTask.class,
										"IOException" + e);
							}
							asyncTask.cancel(true);
							;
							publishProgress(SDCARD_INSUFFICIENT_SPACE);
						} catch (JSONException e) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(GenerateXmlTask.class,
										"JSONException" + e);
							}
							publishProgress(SAVE_FAILED);
							asyncTask.cancel(true);
							;
						} catch (SDCardUnmountException e) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(GenerateXmlTask.class,
										"SDCardUnmountException" + e);
							}
							isSDCardMounted = false;
							asyncTask.cancel(true);
							;
						} catch (Exception e) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(GenerateXmlTask.class, "Exception"
										+ e);
							}
							publishProgress(SAVE_FAILED);
							asyncTask.cancel(true);
							;
						} finally {
							SDCardStatus
									.removeRunningTask(GenerateXmlTask.class
											.toString());
						}
					}
				} else if (saveType == SAVE_MSG_TO_XML) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(GenerateXmlTask.class,
								"SAVE_MSG_TO_XML started");
					}
					try {

						if (supportMMS) {
							saveMessage(filePath, !isMms);
						} else if (!isMms) {
							saveMessage(filePath, !isMms);
						} else {
							publishProgress(SMS_DOES_NOT_EXISTS);
							asyncTask.cancel(true);
							;
						}
						asyncTask.doProgress(HANDLER_SAVE_SUCCEEDED);
					} catch (FileNotFoundException e) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(GenerateXmlTask.class,
									"FileNotFoundException" + e);
						}
						publishProgress(SDCARD_INSUFFICIENT_SPACE);
						asyncTask.cancel(true);
						;
					} catch (IOException e) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(GenerateXmlTask.class, "IOException"
									+ e);
						}
						publishProgress(SDCARD_INSUFFICIENT_SPACE);
						asyncTask.cancel(true);
						;
					} catch (JSONException e) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(GenerateXmlTask.class, "JSONException"
									+ e);
						}
						publishProgress(SAVE_FAILED);
						asyncTask.cancel(true);
						;
					} catch (SDCardUnmountException e) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(GenerateXmlTask.class,
									"SDCardUnmountException" + e);
						}
						isSDCardMounted = false;
						asyncTask.cancel(true);
						;
					} catch (Exception e) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(GenerateXmlTask.class, "Exception" + e);
						}
						publishProgress(SAVE_FAILED);
						asyncTask.cancel(true);
						;
					} finally {
						SDCardStatus.removeRunningTask(GenerateXmlTask.class
								.toString());
					}

				}

				// asyncTask.doProgress(HANDLER_SAVE_DLG_DISMISS);
			}

			return null;
		}

		private void saveMessage(String filePath, boolean isSMS)
				throws FileNotFoundException, IOException, JSONException,
				SDCardUnmountException, Exception {
			publishProgress(FINISH_ACTIVITY);
			try {
				manager.saveMessage(messageId, threadId, isSMS, filePath,
						new BackUpStatusListener() {

							@Override
							public void updateStatus(int progress, int count) {
								publishProgress(UPDATE_COUNT, progress, count);
							}

							@Override
							public boolean isTaskCancelled() {
								if (isCancelled()) {
									return true;
								} else {
									return false;
								}
							}
						});

			} catch (FileNotFoundException e) {
				throw e;
			} catch (IOException e) {
				throw e;
			} catch (JSONException e) {
				throw e;
			} catch (SDCardUnmountException e) {
				throw e;
			} catch (Exception e) {
				throw e;
			} finally {
				SDCardStatus
						.removeRunningTask(GenerateXmlTask.class.toString());
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (values[0] != UPDATE_COUNT && values[0] != FINISH_ACTIVITY) {
				edit.putInt(util.PROGRESS_KEY, 0);
				edit.commit();
			} else {
				// Remove layouts respect to save actions
				if (saveLayout.getVisibility() == View.VISIBLE) {
					saveLayout.setVisibility(View.GONE);
					buttonLayout.setVisibility(View.GONE);
				}

			}
			util.dismissProgressDialog();
			executeTask();
			switch (values[0]) {
			case FINISH_ACTIVITY:
				Intent intent = new Intent(SaveRestoreActivity.this,
						ConversationListActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				break;
			case SMS_DOES_NOT_EXISTS:
				saveButton.setEnabled(true);
				util.clearSyncNotification();
				smsDoesNotExists();
				break;
			case UPDATE_COUNT:
				util.showNotification(BackupManagerImpl.BSYNC_SAVING_CHANGES,
						values[1], values[2]);
				break;
			case SDCARD_INSUFFICIENT_SPACE:
				util.clearSyncNotification();
				util.showNotification(
						BackupManagerImpl.BSYNC_SD_CARD_INSUFFICIENT_SPACE, 0,
						0);
				break;
			case SAVE_FAILED:
				util.showNotification(BackupManagerImpl.BSYNC_SAVE_CANCELLED,
						0, 0);
				break;
			case HANDLER_SAVE_SUCCEEDED:
				if (saveLayout.getVisibility() == View.VISIBLE) {
					saveLayout.setVisibility(View.GONE);
					buttonLayout.setVisibility(View.GONE);
				}
				if (!isCancelled()) {
					util.showNotification(BackupManagerImpl.BSYNC_SAVED, 0, 0);
				}

				break;

			case FILE_ALREADY_EXISTS:
				saveButton.setEnabled(true);
				util.clearSyncNotification();
				fileAlreadyExistsPopup(fileNameToSaveAs);
				break;
			case HANDLER_INVALID_NAME:
				saveButton.setEnabled(true);
				util.clearSyncNotification();
				fileNameNotSpecifiedPopup();
				break;
			case HANDLER_SAVE_FILE_NOT_FOUND:
				saveButton.setEnabled(true);
				util.clearSyncNotification();
				errorWritingFilePopUp();
				break;

			case HANDLER_SAVE_FILE_NO_SD_CARD:
				saveButton.setEnabled(true);
				util.clearSyncNotification();
				sdCardNotConnectedPopUp();
				break;
			}
			super.onProgressUpdate(values);
		}

		void doProgress(Integer value) {
			publishProgress(value);
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			edit.putInt(util.PROGRESS_KEY, 0);
			edit.commit();

			SDCardStatus.removeRunningTask(GenerateXmlTask.class.toString());

		}

	}

	private void smsDoesNotExists() {

		final Dialog d = new AppAlignedDialog(this, R.drawable.dialog_alert,
				R.string.backup_alert_title, R.string.sms_does_not_exists);
		Button yesButton = (Button) d.findViewById(R.id.positive_button);
		yesButton.setText(R.string.yes);
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
				finish();
			}
		});

		setDialogKeyDown(d);
		d.show();

	}

	private boolean checkForValidFileName() {

		Editable eText = fileNameText.getText();
		if (fileNameText.getText().toString().length() == 0) {
			asyncTask.doProgress(HANDLER_INVALID_NAME);
			return false;
		} else {
			fileNameToSaveAs = eText.toString().trim();

			if (!fileNameToSaveAs.endsWith(".xml")
					&& !fileNameToSaveAs.endsWith(".vzm")) {
				fileNameToSaveAs = fileNameToSaveAs + ".vzm";
			}

			// Check if file exist and act accordingly
			File file = new File(root, fileNameToSaveAs);
			if (file.exists() && !canOverride) {
				asyncTask.doProgress(FILE_ALREADY_EXISTS);
				return false;
			} else {
				return true;
			}

		}

	}

	/**
	 * This Method sets the adapter items
	 * 
	 * @param listFiles
	 * @return
	 */
	private SaveRestoreAdapter getFiles(File[] files) {
		ArrayList<SaveRestoreItem> items = new ArrayList<SaveRestoreItem>();
		if (files == null) {
			return null;
		} else {
			for (File file : files) {
				SaveRestoreItem sre = new SaveRestoreItem();
				sre.setFilename(file.getName());
				long date = file.lastModified();
				if (date > 0) {
					sre.setDate(date);	
				} else {
					sre.setDate(-1);
				}
				
				// sre.setFilesize((int) file.length());

				if (file.isDirectory()) {
					// sre.setFilesize(dirSize(file));
					sre.setType(SaveRestoreItem.TYPE_DIRECTORY);
				} else if (file.isFile()
						&& (file.getName().contains(".xml") || file.getName()
								.contains(".vzm"))) {
					// sre.setFilesize((int) file.length());
					sre.setType(SaveRestoreItem.TYPE_XML_FILE);
				} else {
					sre.setType(SaveRestoreItem.TYPE_OTHER_FILE);
				}
				items.add(sre);
			}
		}

		Collections.sort(items, new Comparator<SaveRestoreItem>() {

			@Override
			public int compare(SaveRestoreItem s1, SaveRestoreItem s2) {
				if (s1.getDate() < s2.getDate()) {
					return 1;
				}
				return -1;
			}

		});

		return new SaveRestoreAdapter(new ArrayAdapter<SaveRestoreItem>(this,
				R.layout.save_list_item, items));
	}

	private static int dirSize(File dir) {
		int result = 0;

		Stack<File> dirlist = new Stack<File>();
		dirlist.clear();

		dirlist.push(dir);

		while (!dirlist.isEmpty()) {
			File dirCurrent = dirlist.pop();

			File[] fileList = dirCurrent.listFiles();
			if (fileList == null) {
				break;
			}
			for (int i = 0; i < fileList.length; i++) {

				if (fileList[i].isDirectory())
					dirlist.push(fileList[i]);
				else
					result += fileList[i].length();
			}
		}

		return result;
	}

	/*
	 * This method execute LoadConversationTask AsyncTask class.
	 */
	private void executeTask() {
		mConversationTask = new LoadConversationTask();
		mConversationTask.execute();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		SaveRestoreItem item = (SaveRestoreItem) mList
				.getItemAtPosition(position);

		if (item.getType() == SaveRestoreItem.TYPE_XML_FILE) {
			selectedItem = item;
			int progressState = prefs.getInt(util.PROGRESS_KEY, 0);
			if (progressState == 1) {
				util.showErrorDialog(SaveRestoreActivity.this,
						getString(R.string.backup_alert_title),
						getString(R.string.backup_save_alert_text));
			} else if (progressState == 2) {
				util.showErrorDialog(SaveRestoreActivity.this,
						getString(R.string.backup_alert_title),
						getString(R.string.backup_restore_alert_text));

			} else {
				startRestoreListActivity();
			}
		} else if (item.getType() == SaveRestoreItem.TYPE_DIRECTORY) {
			// navigating into a directory
			root = new File(root.getAbsoluteFile(), item.getStringFilename());
		  	edit.putString(BACKUP_ROOTPATH, root.getPath());
		    edit.commit();
			executeTask();
			upArrowView.setVisibility(View.VISIBLE);
		}

	}

	private void fileNameNotSpecifiedPopup() {
		final Dialog d = new AppAlignedDialog(this, R.drawable.dialog_alert,
				R.string.error, R.string.error_filename_not_specified);
		Button yesButton = (Button) d.findViewById(R.id.positive_button);
		yesButton.setText(R.string.yes);
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				renameFile();
				d.dismiss();
			}
		});

		setDialogKeyDown(d);
		d.show();
	}

	private void renameFile() {
		if (selectedItem == null)
			return;
		final File file = new File(root, selectedItem.getFilename().toString());

		final Dialog d = new AppAlignedDialog(this, 0, R.string.menu_rename,
				R.string.enter_new_file_name);

		final EditText input = new EditText(this);
		LinearLayout ll = (LinearLayout) d
				.findViewById(R.id.dialog_info_layout);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = 12;
		params.rightMargin = 12;
		params.bottomMargin = 15;
		input.setLayoutParams(params);
		ll.addView(input);
		Button yesButton = (Button) d.findViewById(R.id.positive_button);
		yesButton.setText(R.string.yes);
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(input.getWindowToken(), 0);
				Editable etText = input.getText();
				if (etText != null && !etText.toString().trim().equals("")) {
					String newFilename = etText.toString().trim();
					if (!newFilename.endsWith(".xml")
							&& !newFilename.endsWith(".vzm")) {
						newFilename = newFilename + ".vzm";
					}
					File newFile = new File(root, newFilename);
					if (newFile.exists()) {
						fileAlreadyExistsPopup(file, newFilename);
					} else {
						boolean renamed = file.renameTo(newFile);
						if (renamed) {
							fileRenamedSuccessfullyPopup();
							executeTask();

						} else {
							fileRenamedUnsuccessfullyPopup();
						}
					}
				} else {
					fileNameNotSpecifiedPopup();
				}
				// Do something with value!
				d.dismiss();
			}
		});
		Button noButton = (Button) d.findViewById(R.id.negative_button);
		noButton.setVisibility(View.VISIBLE);
		noButton.setText(R.string.cancel);
		noButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				countFlag = false;
				d.dismiss();
			}
		});
		setDialogKeyDown(d);

		d.show();
	}

	private void fileAlreadyExistsPopup(final String filename) {
		final Dialog d = new AppAlignedDialog(this, 0, R.string.menu_rename,
				R.string.file_name_already_exists);
		Button positiveButton = (Button) d.findViewById(R.id.positive_button);
		positiveButton.setText(R.string.yes_string);
		positiveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				canOverride = true;
				asyncTask = new GenerateXmlTask();
				asyncTask.execute();
				d.dismiss();
			}
		});
		Button negButton = (Button) d.findViewById(R.id.negative_button);
		negButton.setVisibility(View.VISIBLE);
		negButton.setText(R.string.no_string);
		negButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				canOverride = false;
				changeNamePopup(filename);
				d.dismiss();
			}
		});
		setDialogKeyDown(d);

		d.show();
	}

	private void fileAlreadyExistsPopup(final File file, final String filename) {
		final Dialog d = new AppAlignedDialog(this, 0, R.string.menu_rename,
				R.string.file_name_already_exists);
		Button positiveButton = (Button) d.findViewById(R.id.positive_button);
		positiveButton.setText(R.string.yes_string);
		positiveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				File newFile = new File(root, filename);
				boolean renamed = file.renameTo(newFile);
				if (renamed) {
					fileRenamedSuccessfullyPopup();
					executeTask();
				} else {
					fileRenamedUnsuccessfullyPopup();
				}
				d.dismiss();
			}
		});
		Button negButton = (Button) d.findViewById(R.id.negative_button);
		negButton.setVisibility(View.VISIBLE);
		negButton.setText(R.string.no_string);
		negButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				changeNamePopup(file, filename);
				d.dismiss();
			}
		});
		setDialogKeyDown(d);

		d.show();
	}

	private void fileRenamedSuccessfullyPopup() {
		final Dialog d = new AppAlignedDialog(this, R.drawable.ic_dialog_info,
				R.string.success, R.string.file_renamed_successfully);
		Button yesButton = (Button) d.findViewById(R.id.positive_button);
		yesButton.setText(R.string.yes);
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
			}
		});

		setDialogKeyDown(d);
		d.show();
	}

	private void changeNamePopup(final File file, final String filename) {

		final Dialog d = new AppAlignedDialog(this, 0, R.string.menu_rename,
				R.string.enter_new_file_name);
		final EditText input = new EditText(this);
		LinearLayout ll = (LinearLayout) d
				.findViewById(R.id.dialog_info_layout);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = 12;
		params.rightMargin = 12;
		input.setLayoutParams(params);
		ll.addView(input);
		Button positiveButton = (Button) d.findViewById(R.id.positive_button);
		positiveButton.setText(R.string.yes);
		positiveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(input.getWindowToken(), 0);
				Editable etText = input.getText();
				if (etText != null && !etText.toString().trim().equals("")) {
					String newFilename = etText.toString().trim();
					if (!newFilename.endsWith(".xml")
							&& !newFilename.endsWith(".vzm")) {
						newFilename = newFilename + ".vzm";
					}
					File newFile = new File(root, newFilename);
					if (!newFile.exists()) {
						boolean renamed = file.renameTo(newFile);
						if (renamed) {
							fileRenamedSuccessfullyPopup();
							executeTask();
						} else {
							fileRenamedUnsuccessfullyPopup();
						}
					} else {
						fileAlreadyExistsPopup(newFile, newFilename);
					}
				} else {
					fileNameNotSpecifiedPopup();
				}
				d.dismiss();
			}
		});
		Button negButton = (Button) d.findViewById(R.id.negative_button);
		negButton.setVisibility(View.VISIBLE);
		negButton.setText(R.string.cancel);
		negButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				;
				d.dismiss();
			}
		});
		setDialogKeyDown(d);

		d.show();
	}

	private void fileRenamedUnsuccessfullyPopup() {
		final Dialog d = new AppAlignedDialog(this, R.drawable.dialog_alert,
				R.string.error, R.string.error_renaming_file);
		Button yesButton = (Button) d.findViewById(R.id.positive_button);
		yesButton.setText(R.string.yes);
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				renameFile();
				d.dismiss();
			}
		});

		setDialogKeyDown(d);
		d.show();
	}

	private void changeNamePopup(final String filename) {
		final Dialog d = new AppAlignedDialog(this, 0, R.string.menu_rename,
				R.string.enter_new_file_name);
		final EditText input = new EditText(this);
		LinearLayout ll = (LinearLayout) d
				.findViewById(R.id.dialog_info_layout);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		params.leftMargin = 12;
		params.rightMargin = 12;
		input.setLayoutParams(params);
		ll.addView(input);
		Button positiveButton = (Button) d.findViewById(R.id.positive_button);
		positiveButton.setText(R.string.yes);
		positiveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(input.getWindowToken(), 0);
				Editable etText = input.getText();
				if (etText != null && !etText.toString().trim().equals("")) {
					String newFilename = etText.toString().trim();
					if (!newFilename.endsWith(".xml")
							&& !newFilename.endsWith(".vzm")) {
						newFilename = newFilename + ".vzm";
					}

					File file = new File(root, newFilename);
					if (file.exists()) {
						fileAlreadyExistsPopup(newFilename);
					} else {
						fileNameText.setText(newFilename);
						asyncTask = new GenerateXmlTask();
						asyncTask.execute();
						dismissKeyboard(input);
					}

				} else {
					fileNameNotSpecifiedPopup();
				}
				d.dismiss();
			}
		});
		Button negButton = (Button) d.findViewById(R.id.negative_button);
		negButton.setVisibility(View.VISIBLE);
		negButton.setText(R.string.cancel);
		negButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
			}
		});
		setDialogKeyDown(d);

		d.show();
	}

	private void errorWritingFilePopUp() {
		final Dialog d = new AppAlignedDialog(this, R.drawable.dialog_alert,
				R.string.error, R.string.error_writing_file);
		Button yesButton = (Button) d.findViewById(R.id.positive_button);
		yesButton.setText(R.string.yes);
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
			}
		});

		setDialogKeyDown(d);
		d.show();
	}

	private void sdCardNotConnectedPopUp() {
		final Dialog d = new AppAlignedDialog(this, R.drawable.dialog_alert,
				R.string.error, R.string.error_sd_card_not_connected);
		d.setCancelable(false);
		Button yesButton = (Button) d.findViewById(R.id.positive_button);
		yesButton.setText(R.string.yes);
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
				finish();
			}
		});

		setDialogKeyDown(d);
		d.show();
	}

	private void dismissKeyboard(View v) {

		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}

	private static void setDialogKeyDown(Dialog builder) {
		builder.setOnKeyListener(new DialogInterface.OnKeyListener() {

			@Override
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_SEARCH
						&& event.getRepeatCount() == 0) {
					return true; // Pretend we processed it
				}
				return false; // Any other keys are still processed as normal
			}
		});
	}

	/**
	 * This Method deletes respective file from SD card
	 */
	public void deleteFile() {

		File file = new File(root, selectedItem.getFilename().toString());
		boolean deleted = file.delete();

		final Dialog dialog;
		if (deleted) {
			dialog = new AppAlignedDialog(this, R.drawable.ic_dialog_info,
					R.string.success, R.string.file_deleted_successfully);
		} else {
			dialog = new AppAlignedDialog(this, R.drawable.dialog_alert,
					R.string.error, R.string.error_deleting_file);
		}
		Button yesButton = (Button) dialog.findViewById(R.id.positive_button);
		yesButton.setText(R.string.yes);
		yesButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		setDialogKeyDown(dialog);

		dialog.show();

		SaveRestoreAdapter srla = getFiles(root.listFiles(filter));
		if (srla != null) {
			mList.setAdapter(srla);
		}

	}

	/**
	 * This class loads the conversation form the xml file.
	 * 
	 */
	private class LoadConversationTask extends
			AsyncTask<Void, Integer, SaveRestoreAdapter> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(SaveRestoreAdapter sRA) {

			if (sRA != null) {
				mList.setAdapter(sRA);
				if (countFlag)
					setFileName();
			} else {
				mList.setAdapter(null);
			}
			Util.forceHideKeyboard(SaveRestoreActivity.this, fileNameText);
			if (isHardwareKeyboardAvailable()) {
				(SaveRestoreActivity.this).getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
			}
		}

		@Override
		protected SaveRestoreAdapter doInBackground(Void... params) {
			if (root != null) {
				return getFiles(root.listFiles(filter));
			} else {
				return null;
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			mList.setAdapter(null);
		}
	}

	public static boolean cancelSavingTask() {
		boolean cancelled = false;
		if (asyncTask != null
				&& asyncTask.getStatus() != AsyncTask.Status.FINISHED) {
			cancelled = asyncTask.cancel(true);
		}
		return cancelled;
	}
	
	private boolean isHardwareKeyboardAvailable() {
		 return getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS; 
    }

}
