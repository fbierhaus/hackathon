package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Intents;
import android.provider.Telephony.Threads;
import android.support.v4.app.ListFragment;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.ExportLogs;
import com.verizon.common.VZUris;
import com.verizon.messaging.vma.provision.ProvisionManager;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.VMAProvision;
import com.verizon.messaging.vzmsgs.sync.AppErrorCodes;
import com.verizon.messaging.vzmsgs.sync.SyncStatusCode;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.transaction.MessagingNotification;
import com.verizon.mms.transaction.MessagingNotification.PopUpInfo;
import com.verizon.mms.transaction.MessagingNotification.PopUpInfoComparator;
import com.verizon.mms.transaction.SmsRejectedReceiver;
import com.verizon.mms.ui.VZMFragmentActivity.ActionMenuBuilder;
import com.verizon.mms.ui.activity.DebugPanelActivity;
import com.verizon.mms.ui.activity.ErrorHandler;
import com.verizon.mms.ui.activity.Provisioning;
import com.verizon.mms.ui.widget.ActionItem;
import com.verizon.mms.ui.widget.ImageViewButton;
import com.verizon.mms.ui.widget.QuickAction;
import com.verizon.mms.ui.widget.WelcomeScreen;
import com.verizon.mms.util.BitmapManager;
import com.verizon.mms.util.DraftCache;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.Util;
import com.verizon.sync.SyncManager;
import com.verizon.vzmsgs.saverestore.PopUpUtil;
import com.verizon.vzmsgs.saverestore.SDCardStatus;

public class ConversationListFragment extends ListFragment implements DraftCache.OnDraftChangedListener,
        android.view.View.OnClickListener, MenuSelectedListener, SyncStatusCode {
    public ConversationListActivity parentActivity;
    private ConversationListAdapter mAdapter;
    private static final int THREAD_LIST_QUERY_TOKEN = 1701;
    public static final int DELETE_CONVERSATION_TOKEN = 1801;
    public static final int HAVE_LOCKED_MESSAGES_TOKEN = 1802;
    private static final int DELETE_OBSOLETE_THREADS_TOKEN = 1803;
    // Changing all Menuitems to 2000 Serese
    // IDs of the context menu items for the list of conversations.
    private static final int MENU_DELETE = 2000;
    private static final int MENU_VIEW = 2001;
    private static final int MENU_VIEW_CONTACT = 2002;
    private static final int MENU_ADD_TO_CONTACTS = 2003;
    private static final int MENU_SEND_MESSAGE = 2004;
    private static final int MENU_CONVERSATION_DETAILS = 2005;
    private static final int MENU_CALL_BACK = 2006;
    private static final int MENU_SEND_EMAIL = 2007;

    // IDs of the main menu items.
    private static final int MENU_COMPOSE_NEW = 2100;
    private static final int MENU_RESTORE_MESSAGES = 2101;
    private static final int MENU_DELETE_CONVERSATIONS = 2103;
    private static final int MENU_PREFERENCES = 2104;
    private static final int MENU_DUMP_DB = 2105;
    private static final int MENU_DUMP_LOG = 2106;
    private static final int MENU_TIPS = 2107;
    private static final int MENU_ABOUT = 2108;
    private static final int MENU_CANCEL_BATCH = 2109;

    // private static final int MENU_VMA_RESET = 2111;
    private static final int MENU_SAVE_CONVERSATIONS = 2112;
    private static final int MENU_SAVE_CONV = 2113;
    private static final int MENU_EMAIL_TRACES = 2114;
    private static final int MENU_EMAIL_MEM = 2115;
    private static final int MENU_DEBUG_PANEL = 2116;
    private static final int MENU_UNPAIR_DEVICE = 2117;
    private static final int MENU_SYNC = 2118;

    private static final int MENU_GENERATE_SMS = 2119;
    private static final int MENU_SEND_COMMENTS = 2120;
    private static final int MENU_SEARCH = 2121;
    private static final int MENU_RECONNECT = 2122;
    private static final int SHOW_TERMS_AND_CONDITION = 2123;
    private static final int SHOW_PRIVACY_POLICY = 2124;
    private static final int REPORT_BUG = 2125;
    private static final int MENU_SHOW_STATUS_DETAILS = 2126;
    private static final int MENU_SHOW_POPUP = 2127;

    // private static final int MENU_SAVE_CSV = 2122;

    public static final String SAVE_RESTORE_NOTIFICATION = "save-restore";
    private static final String BACKUP_PROGRESS_STATE = "backupInProgress";
    private ThreadListQueryHandler mQueryHandler;
    private boolean mNeedToMarkAsSeen;
    private boolean mDeleteObsoleteThread;
    public static final int REQUEST_CODE_FINISH_ACTIVITY = 10109;
    private Activity mActivity;
    private SharedPreferences mPrefs;
    private ImageView composeNewMessageImg;
    private View idleStatus;
    // gallery
    private ImageView mediaShoebox;

    private long queryStart;
    private boolean mActive;
    private WelcomeScreen mWScreen;
    private boolean mIsLandscape;
    private Point p, p1;
    // when the fragment is launched for the first time select the first thread
    private boolean selectFirstConversation = false;
    public static final String IS_WIDGET = "isWidget";

    /**
     * Variables for batch mode
     */
    private static final int BATCH_OPERATION_DELETE = 1;
    private static final int BATCH_OPERATION_SAVE = 2;
    private static final int BATCH_OPERATION_NONE = 3;

    private static final int MSG_BATCH_QUERY_COMPLETE = 1;
    private static final int MSG_DEL_SYNC_COMPLETE = 2;

    private int mBatchOperation = BATCH_OPERATION_NONE;
    private BatchModeController mBatchModeController;

    public static final int REQUEST_CODE_CHECK_CONTACT = 444;

    private String mSelectedAddress;
    private Button mBtnSelectAll;
    private Button mBtnunSelectAll;
    // private View mAutoFwdStatus;
    private View mAutoReplyStatus;
    // private TextView mFwdPendingText ;
    private View mAutoReplyFwdStatusBar;
    private ImageView mAutoReplyStatusIcon;
    public int batchCount = 0;
    private boolean isMultipaneUI;
    private Handler mainHandler;
    protected View mSyncProgressView;
    ApplicationSettings settings;
    private Object lock;
    private boolean isPinReceived = false;
    private String pin;
    private ErrorHandler errorHandler;
    // currently selected threadId
    long selectedThreadId = 0;
    /*
     * Added Delete Conversation progress dialog in order to show conversation deletion progress
     */
    private ProgressDialog convDeletionDialog;

    private AsyncTask mAsyncTask;
    private SyncStatusReceiver syncStatusReceiver = null;
    private Dialog exitDialog = null;

    private boolean launchCompose = false;

    private final ConversationListAdapter.OnContentChangedListener mContentChangedListener = new ConversationListAdapter.OnContentChangedListener() {
        public void onContentChanged(ConversationListAdapter adapter) {
            startAsyncQuery();
        }
    };

    private final Handler mBatchQueryHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	if (mActive) {
        		switch (msg.what) {
        		case MSG_BATCH_QUERY_COMPLETE:
        			if (convDeletionDialog.isShowing()) {
        				convDeletionDialog.dismiss();
        				if (mBatchOperation != BATCH_OPERATION_SAVE) {
        					onDeleteSelectAnother();
        				}
        			}
        			setBatchMode(BATCH_OPERATION_NONE);
        			break;

        		case MSG_DEL_SYNC_COMPLETE:
        			if (MmsConfig.isTabletDevice()) {
        				convDeletionDialog.getWindow().setLayout(300, 200);
        			}
        			convDeletionDialog.setMessage(getString(R.string.deleting));
        		}
        	}
        }
    };

    private final OnKeyListener mThreadListKeyListener = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                case KeyEvent.KEYCODE_DEL: {
                    long id = getListView().getSelectedItemId();
                    if (id > 0) {
                        confirmDeleteThreadDialog(new DeleteThreadListener(id, mQueryHandler, mActivity),
                                id == -1, false, mActivity);
                    }
                    return true;
                }
                }
            }
            return false;
        }
    };

    private boolean isGCMregistered = false;

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onResume()
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = ApplicationSettings.getInstance();
        mActive = true;
        isMultipaneUI = Util.isMultiPaneSupported(getActivity());
        isGCMregistered = false;

        if (settings.isProvisioned() && MmsConfig.isTabletDevice()) {
            try {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("GCM: Registration");
                }
                GCMRegistrar.checkDevice(settings.getContext());
                GCMRegistrar.checkManifest(settings.getContext());
                isGCMregistered = true;
                final String regId = GCMRegistrar.getRegistrationId(settings.getContext());
                if (regId.equals("")) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("GCM: invoking GCMRegistrar.register()");
                    }
                    GCMRegistrar.register(settings.getContext(), VMAProvision.SENDER_ID);
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("GCM Token Already registered");
                    }
                }
            } catch (Exception e) {
                if (Logger.IS_ERROR_ENABLED) {
                    Logger.error("GCM: Registration failed.", e);
                }
            }

        }

    }

    @Override
    public void onResume() {

        boolean isTabletInOfflineMode = settings.getBooleanSetting(AppSettings.KEY_VMA_TAB_OFFLINE_MODE,
                false);
        if (isTabletInOfflineMode) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("Tablet is in offline mode.");
            }
            Toast.makeText(mActivity, R.string.vma_offline_text, Toast.LENGTH_LONG).show();
        }

        showOrHideAutoReplyAndFwdHeaders();
        super.onResume();
    }

    /**
     * Provision handset if it is not subscribed to VMA
     * 
     * @param dialog
     * 
     */
    private void provisionHandset(final Dialog dialog) {

        mAsyncTask = new AsyncTask<Void, String, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                ProvisionManager provisionManager = new ProvisionManager(mActivity);
                return provisionManager.doProvision();
            };

            protected void onPostExecute(Integer result) {
                dialog.dismiss();
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    showPendingProvisioningInfo(true);
                } else {
                    // Intent errorIntent = new Intent(mActivity, SingleDialogActivity.class);
                    // errorIntent.putExtra(SingleDialogActivity.ERROR_CODE, result.intValue());
                    // mActivity.startActivity(errorIntent);
                    errorHandler.showAlertDialog(result.intValue());
                }
            };

        }.execute();

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mActivity = getActivity();
        errorHandler = new ErrorHandler(mActivity);
        settings = ApplicationSettings.getInstance(mActivity);
        boolean isTabletNotProvisioned =(!settings.isProvisioned() && VZUris.isTabletDevice());
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(),"isTabletNotProvisioned="+isTabletNotProvisioned);
        }
   
        if (isTabletNotProvisioned || !settings.getBooleanSetting(AppSettings.KEY_VMA_ACCEPT_TERMS, false)) {
        	if(Logger.IS_DEBUG_ENABLED) {
    			Logger.debug(getClass(),"starting Provisioning activity from ConversationListFragment - T&C false");
    		}
            Intent i = new Intent(mActivity, Provisioning.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            // finish this activity if we are still under T&C screen
            mActivity.finish();
        }
        Configuration configuration = getResources().getConfiguration();
        mIsLandscape = configuration.orientation == configuration.ORIENTATION_LANDSCAPE;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        composeNewMessageImg = (ImageView) mActivity.findViewById(R.id.composebutton);
        composeNewMessageImg.setOnClickListener(this);

        // shoebox
        mediaShoebox = (ImageView) mActivity.findViewById(R.id.imgGallery);
        idleStatus = mActivity.findViewById(R.id.sync_idle);
        idleStatus.setOnClickListener(this);

        if (MmsConfig.isFMSBEnabled()) {
            mediaShoebox.setOnClickListener(this);
            mediaShoebox.setVisibility(View.VISIBLE);
        } else {
            mediaShoebox.setVisibility(View.GONE);
        }

        // mAutoFwdStatus = mActivity.findViewById(R.id.fwdStatus);
        mAutoReplyStatus = mActivity.findViewById(R.id.replyStatus);
        mAutoReplyFwdStatusBar = mActivity.findViewById(R.id.replyfwdStatus);
        mAutoReplyStatusIcon = (ImageView) mActivity.findViewById(R.id.imgAutoFwdReply);
        // mFwdPendingText = (TextView)mActivity.findViewById(R.id.forwardPendingText);
        mSyncProgressView = mActivity.findViewById(R.id.sync_progressBar);

        initActivityState(savedInstanceState);

        initListAdapter();
        setBatchMode(BATCH_OPERATION_NONE);
        mQueryHandler = new ThreadListQueryHandler(mainHandler, getActivity().getContentResolver());
        mNeedToMarkAsSeen = true;
        mDeleteObsoleteThread = true;
        ListView listView = getListView();
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> list, View view, int pos, long id) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "onItemLongClick: pos = " + pos + ", id = " + id);
                }
                setConversationMenu(pos);
                return true;
            }
        });
        listView.setOnKeyListener(mThreadListKeyListener);
        listView.setFastScrollEnabled(true);

        mainHandler = new Handler();

        // register the syncstatus receiver
        syncStatusReceiver = new SyncStatusReceiver();
        mActivity.registerReceiver(syncStatusReceiver, new IntentFilter(SyncManager.ACTION_SYNC_STATUS));
        mActivity.registerReceiver(syncStatusReceiver, new IntentFilter(PopUpUtil.BACKUP_STATUS_NOTIFY));

        super.onActivityCreated(savedInstanceState);
    }

    private OnGlobalLayoutListener onGlobalLayoutListener = new OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (mWScreen == null)
                return;

            if ((mWScreen != null && !mWScreen.isShown())) {
                mWScreen = null;
                return;
            }
            ImageViewButton composeButton = (ImageViewButton) mActivity.findViewById(R.id.composebutton);
            ImageViewButton imgGallery = (ImageViewButton) mActivity.findViewById(R.id.imgGallery);
            int[] location = new int[2];
            int[] location1 = new int[2];
            composeButton.getLocationOnScreen(location);
            imgGallery.getLocationOnScreen(location1);
            p = new Point();
            p.x = location[0];
            p.y = location[1];
            p1 = new Point();
            p1.x = location1[0];
            p1.y = location1[1];
            if (p.equals(mWScreen.getP().x, mWScreen.getP().y)
                    || p1.equals(mWScreen.getP1().x, mWScreen.getP1().y)) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Both modes are same:" + mIsLandscape + "," + mWScreen.ismIsLandscape());
                }
            } else {
                mWScreen.dismiss();
                mWScreen = null;
                mWScreen = new WelcomeScreen(mActivity, isMultipaneUI, mIsLandscape, p, p1);
                mWScreen.show();
            }

        }
    };

    private void initActivityState(Bundle savedInstanceState) {
        Intent intent = mActivity.getIntent();

        if (intent.getBooleanExtra(ConversationListActivity.EXTRA_LAUNCH_COMPOSE, false)) {
            launchComposeWithIntent(intent);
            return;
        }

        if (settings.getBooleanSetting(AppSettings.KEY_VMA_SHOW_PROVISION_SERVICE, false)) {
            settings.put(AppSettings.KEY_VMA_SHOW_PROVISION_SERVICE, false);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Triggering foreground provisiong from onActivity Created");
            }
            showProgressStatus();
        }

        boolean fromSaveRestoreNotification = intent.getBooleanExtra(SAVE_RESTORE_NOTIFICATION, false);
        if (fromSaveRestoreNotification) {
            boolean isSaveRestoreInProgress = intent.getBooleanExtra("inProgress", false);
            showSaveInfoDialog(isSaveRestoreInProgress);
        }
        // Temp fix for build 16
        // if (intent.getIntExtra(SyncManager.EXTRA_ACTION, 0) == SyncStatusListener.SYNC_PROVISIONING) {
        // mSyncProgressView.setVisibility(View.VISIBLE);
        // }

        long threadId = intent.getLongExtra("thread_id", 0L);
        // select the first conversation when the activity is started
        // dont select the first conversation if the activity was launched from
        // a notification with a thread id specified in it
        if (isMultipaneUI && threadId <= 0) {
            String action = intent.getAction();
            if (null != action
                    && !(action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE) || action
                            .equals(Intent.ACTION_SENDTO)))
                selectFirstConversation = true;
        }

        if (isMultipaneUI && intent.getBooleanExtra(IS_WIDGET, false)) {
            selectFirstConversation = true;
        }
    }

    private void launchComposeWithIntent(Intent intent) {
        Intent newIntent = new Intent(mActivity, ComposeMessageActivity.class);

        newIntent.setData(intent.getData());
        newIntent.putExtra("thread_id", intent.getLongExtra("thread_id", 0));
        newIntent.putExtra("is_compose", intent.getBooleanExtra("is_compose", false));
        newIntent.putExtra("showKeyboard", intent.getBooleanExtra("showKeyboard", false));
        newIntent.putExtra("undelivered_flag", intent.getBooleanExtra("undelivered_flag", false));
        newIntent.putExtra("failed_download_flag", intent.getBooleanExtra("failed_download_flag", false));

        newIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // remove the launch compose flag so tat if this activity was restarted it wont go to compose screen
        // again
        intent.putExtra(ConversationListActivity.EXTRA_LAUNCH_COMPOSE, false);

        startActivity(newIntent);

        launchCompose = true;
    }

    private void showProgressStatus() {
        final AppAlignedDialog dialog = new AppAlignedDialog(mActivity, R.layout.launcher_view);
        dialog.setCancelable(false);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                switch (keyCode) {
                case KeyEvent.KEYCODE_SEARCH:
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "Handling search key so that dialog dont get removed");
                    }
                    return true;
                }
                return false;
            }
        });
        TextView progressStatus = (TextView) dialog.findViewById(R.id.progressStatus);
        ProgressBar progressBar = (ProgressBar) dialog.findViewById(R.id.progressBar);
        activateVMA(dialog, progressStatus, progressBar);
        dialog.show();
    }

    /**
     * provisioning in handset will be done in foreground. it will first call generatePin,If response is
     * RESPONSE_OK then with the pin from Broadcast Receiver , it will call isValidLogin, if response is
     * RESPONSE_OK, then last call goes to isValidSubscriber, If an error response comes at any call, it will
     * be notified to user and will be redirected to Conversation List, if response is
     * RESPONSE_NOT_VMA_SUBCRIBER , a dialog will be shown to accept Terms and Condition
     **/

    private void activateVMA(final Dialog dialog, final TextView progressStatus, final ProgressBar progressBar) {

        mAsyncTask = new AsyncTask<Void, String, Integer>() {
            String mdn;

            protected void onPreExecute() {
                /*
                 * During a full sync, we do not need to sync the old messages back to the handset device. So
                 * for that we should keep track of the starting time of the first install of the application
                 * and not insert any sent messages into the Handset that have a timestamp prior to the when
                 * VZW was installed.
                 */
                settings.put(AppSettings.KEY_APP_INSTALL_TIME, new Date().getTime());
                updateText("Validating account...", 20);
                lock = new Object();
            };

            protected void onProgressUpdate(String[] values) {
                updateText(values[0], 20);
            }

            @Override
            protected Integer doInBackground(Void... params) {
                mdn = ApplicationSettings.parseAdddressForChecksum(MessageUtils.getLocalNumber());
                if (Logger.IS_INFO_ENABLED) {
                    Logger.info(getClass(), " Local mdn =" + mdn);
                }
                String devicemodel = Build.MANUFACTURER + "-" + Build.MODEL;
                ProvisionManager provisionManager = new ProvisionManager(mActivity);
                PINIntercepters intercepters = new PINIntercepters();
                IntentFilter filter = new IntentFilter(Intents.SMS_RECEIVED_ACTION);
                // filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
                // GOSMS app is using Integer.MAX_VALUE as the priority, we hve to set more
                filter.setPriority(Integer.MAX_VALUE);
                int result = 0;
                try {
                    mActivity.registerReceiver(intercepters, filter);
                    result = provisionManager.generatePIN(mdn, devicemodel, true);
                    if (result == AppErrorCodes.VMA_PROVISION_OK) {
                        updateText("Authorizing...", 40);

                        // wait for 2 minutes
                        synchronized (lock) {
                            try {
                                if (Logger.IS_DEBUG_ENABLED) {
                                    Logger.debug("Waiting for Silent SMS PIN" + 9999999999999L);
                                }
                                // two minutes time out
                                lock.wait(120000);
                                // To abort the broadcast.
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                            }

                        }
                        if (Logger.IS_DEBUG_ENABLED) {
                            if (!isPinReceived) {
                                return AppErrorCodes.VMA_PROVISION_PIN_RETRIEVAL_FAILED;
                            }
                            updateText("Authorizing...pin=" + pin, 60);
                        }
                        updateText("Verifying your phone", 80);
                        result = provisionManager.isValidLoginPIN(mdn, pin);
                        if (!(isPinReceived && (result == AppErrorCodes.VMA_PROVISION_OK))) {
                            result = AppErrorCodes.VMA_PROVISION_PIN_RETRIEVAL_FAILED;
                        }
                    } else {
                        synchronized (lock) {
                            try {
                                // let conv list Activity to start
                                lock.wait(2000);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                    mActivity.unregisterReceiver(intercepters);
                } catch (Exception e) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Exception in Intent receiver", e);
                    }
                }
                return result;
            }

            private void updateText(final String text, final int progress) {
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        progressBar.setProgress(progress);
                        // progressStatus.setText(text);
                    }
                });
            }

            protected void onPostExecute(Integer result) {

                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    checkifVMASubscriber(mdn, dialog, progressStatus, progressBar);
                } else {
                    dialog.dismiss();
                    errorHandler.showAlertDialog(result.intValue());
                }
            }
        }.execute();
    }

    private void checkifVMASubscriber(final String mdn, final Dialog dialog, final TextView progressStatus,
            final ProgressBar progressBar) {

        mAsyncTask = new AsyncTask<Void, String, Integer>() {
            ProvisionManager provisionManager = new ProvisionManager(mActivity);

            protected void onPreExecute() {
                progressBar.setProgress(90);
                // progressStatus.setText("Checking subscription details...");
            };

            @Override
            protected Integer doInBackground(Void... params) {
                return provisionManager.isVMASubscriber(mdn);
            }

            protected void onPostExecute(Integer result) {
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_VMA_SUBSCRIBER) {
                    if (!settings.getBooleanSetting(AppSettings.KEY_SMS_CHECKSUM_COMPLETED)) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Handset was already a VMA sub, starting thread to do indexing old messages from isVmaSubscriber");
                        }
                        provisionManager.startSMSChecksumBuilder();
                    }
                    dialog.dismiss();
                    showPendingProvisioningInfo(true);
                } else if (result.intValue() == AppErrorCodes.VMA_PROVISION_NOT_VMA_SUBCRIBER) {
                    progressBar.setProgress(99);
                    provisionHandset(dialog);
                } else {
                    dialog.dismiss();
                    errorHandler.showAlertDialog(result.intValue());
                }
            }
        }.execute();
    }

    class PINIntercepters extends BroadcastReceiver {

        /**
	         * 
	         */
        private static final String SILENT_PIN_SENDER_ADDRESS = "900080004102";

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            // SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(PINIntercepters.class,
                        "PINIntercepters.onReceive(Context, Intent) in Conversation List Fragment");
            }
            if (Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                SmsMessage[] msgs = Intents.getMessagesFromIntent(intent);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(PINIntercepters.class, "body :" + msgs[0].getDisplayMessageBody());
                    Logger.debug(PINIntercepters.class, "sender :" + msgs[0].getDisplayOriginatingAddress());
                }
                String sender = msgs[0].getDisplayOriginatingAddress();
                String message = msgs[0].getDisplayMessageBody();
                if (SILENT_PIN_SENDER_ADDRESS.equalsIgnoreCase(sender)) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(PINIntercepters.class, "Service Messaage ");
                    }
                    if (message.startsWith("VZ Messages PIN:") || message.startsWith(":")) {
                        String[] pins = message.split(":", 2);
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(PINIntercepters.class, "found silent pin=" + pins[1].trim());
                        }
                        pin = pins[1].trim();
                        isPinReceived = true;
                        if (isOrderedBroadcast()) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(PINIntercepters.class, "aborting Order Broadcast.");
                            }
                            abortBroadcast();
                        } else {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(PINIntercepters.class, "aborting non-Order Broadcast.");
                            }
                            setResult(Activity.RESULT_CANCELED, null, null);
                            clearAbortBroadcast();
                        }
                        // Need notify after unblock.
                        synchronized (lock) {
                            lock.notify();
                        }

                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(PINIntercepters.class, "Not a service messaage. no need to process");
                        }
                    }
                }
            }

        }

    }

    /**
     * Start Sync for both handset and tablet and show Welcome screen for New Handset User
     */

    private void showPendingProvisioningInfo(boolean showWelcomeDialog) {
        if (settings.isProvisioned() && settings.getBooleanSetting(AppSettings.SHOW_WELCOME_SCREEN, false)
                && !settings.getBooleanSetting(AppSettings.KEY_VMA_HANDSET_PROVISIONING_IN_BACKROUND, false)) {
            settings.put(AppSettings.SHOW_WELCOME_SCREEN, false);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Welcome screen is called from showPendingProvisoningInfo"
                        + showWelcomeDialog);
            }
            if (showWelcomeDialog) {
                showWelcomeScreen();
            }
            if (settings.getBooleanSetting(AppSettings.KEY_VMA_ACCEPT_TERMS, false)) {
             //   startSync();
            }
        }
    }

    /**
     * This Method
     */
    private void showOrHideAutoReplyAndFwdHeaders() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("showOrHideAutoReplyAndFwdHeaders:reply=" + settings.isAutoReplyEnabled());
        }
        if (settings.isAutoReplyEnabled()) {
            mAutoReplyStatusIcon.setVisibility(View.VISIBLE);
            mAutoReplyStatusIcon.setImageResource(R.drawable.ico_setting_auto_reply);
            mAutoReplyStatus.setVisibility(View.VISIBLE);
        } else {
            mAutoReplyStatusIcon.setVisibility(View.GONE);
            mAutoReplyStatus.setVisibility(View.GONE);
            mAutoReplyStatus.setVisibility(View.GONE);
        }

        // String autoFwdStatus = settings.getSettings(KEY_VMA_AUTOFORWARDSTATUS);
        // mAutoFwdStatus.setVisibility(View.GONE);
        /*
         * if(isAutoReplyEnabled && isAutoFwdEnabled){ if("0".equals(autoFwdStatus)) {
         * mFwdPendingText.setText(R.string.vma_forward_pending_text); } else {
         * mFwdPendingText.setText(R.string.is_forward); } mAutoFwdStatus.setVisibility(View.VISIBLE);
         * mAutoReplyStatus.setVisibility(View.VISIBLE); mAutoReplyFwdStatusIcon.setVisibility(View.VISIBLE);
         * mAutoReplyFwdStatusIcon.setImageResource(R.drawable.ico_setting_auto_reply_forward);
         * 
         * } else { if(isAutoFwdEnabled){ if("0".equals(autoFwdStatus)) {
         * mFwdPendingText.setText(R.string.vma_forward_pending_text); } else {
         * mFwdPendingText.setText(R.string.is_forward); }
         * 
         * mAutoReplyFwdStatusIcon.setVisibility(View.VISIBLE);
         * mAutoReplyFwdStatusIcon.setImageResource(R.drawable.ico_setting_auto_forward);
         * mAutoFwdStatus.setVisibility(View.VISIBLE); } else { mAutoFwdStatus.setVisibility(View.GONE); }
         * if(isAutoReplyEnabled){
         * 
         * mAutoReplyFwdStatusIcon.setVisibility(View.VISIBLE);
         * mAutoReplyFwdStatusIcon.setImageResource(R.drawable.ico_setting_auto_reply);
         * mAutoReplyStatus.setVisibility(View.VISIBLE); } else { mAutoReplyStatus.setVisibility(View.GONE); }
         * }
         */
    }

    public void onNewIntent(Intent intent) {
        if (intent.getBooleanExtra(ConversationListActivity.EXTRA_LAUNCH_COMPOSE, false)) {
            launchComposeWithIntent(intent);
            return;
        }

        if (intent.getBooleanExtra(MessagingPreferenceActivity.EXTRA_FONT_CHANGED, false)) {
            // font is changed within the Application
            // so restart the Activity to reflect the font change
            mActivity.finish();

            Intent i = new Intent(mActivity, ConversationListActivity.class);
            startActivity(i);

            return;
        }
        boolean fromSaveRestoreNotification = intent.getBooleanExtra(SAVE_RESTORE_NOTIFICATION, false);
        if (fromSaveRestoreNotification) {
            boolean isSaveRestoreInProgress = intent.getBooleanExtra("inProgress", false);
            showSaveInfoDialog(isSaveRestoreInProgress);
        }
        // if (intent.getIntExtra(SyncManager.EXTRA_ACTION, 0) == SyncStatusListener.SYNC_PROVISIONING) {
        // mSyncProgressView.setVisibility(View.VISIBLE);
        // }
        if (settings.getBooleanSetting(AppSettings.KEY_VMA_SHOW_PROVISION_SERVICE, false)) {
            settings.put(AppSettings.KEY_VMA_SHOW_PROVISION_SERVICE, false);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Triggering foreground provisiong from onNew Intent");
            }
            showProgressStatus();
        }

        boolean fromNotification = intent.getBooleanExtra(ConversationListActivity.EXTRA_FROM_NOTIFICATION,
                false);
        if (fromNotification) {
            // mark conversation as seen if we are launched from the notification
            mNeedToMarkAsSeen = true;
        }
    }

    private void showSaveInfoDialog(boolean state) {
        final Dialog d = new AppAlignedDialog(mActivity, R.drawable.launcher_home_icon, R.string.app_label,
                state ? R.string.popup_saving_text : R.string.popup_saved_text);
        d.setCancelable(true);
        Button continueButton = (Button) d.findViewById(R.id.positive_button);
        continueButton.setText(R.string.yes);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        Button cancelButton = (Button) d.findViewById(R.id.negative_button);
        if (state) {
            cancelButton.setVisibility(View.VISIBLE);
        }
        cancelButton.setText(R.string.cancel_save);
        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                d.dismiss();
                showCancelAlertDialog();
            }

        });
        d.show();
    }

    private void showCancelAlertDialog() {
        final Dialog d = new AppAlignedDialog(mActivity, R.drawable.launcher_home_icon, R.string.app_label,
                R.string.save_cancel_alert);
        d.setCancelable(true);
        Button okButton = (Button) d.findViewById(R.id.positive_button);
        okButton.setText(R.string.yes_string);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
                SharedPreferences.Editor edit = mPrefs.edit();
                edit.putInt(BACKUP_PROGRESS_STATE, 0);
                edit.commit();
                if (!SaveRestoreActivity.cancelSavingTask()) {
                    final Dialog innerDialog = new AppAlignedDialog(mActivity, R.drawable.launcher_home_icon,
                            R.string.app_label, R.string.file_already_saved);
                    innerDialog.setCancelable(true);
                    Button okButton = (Button) innerDialog.findViewById(R.id.positive_button);
                    okButton.setText(R.string.yes);
                    okButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            innerDialog.dismiss();
                        }
                    });
                    innerDialog.show();
                }
            }
        });
        Button cancelButton = (Button) d.findViewById(R.id.negative_button);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setText(R.string.no_string);
        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();

    }

    private void setBatchMode(int batchOperation) {
        mBatchOperation = batchOperation;
        // reset selection buttons
        if (batchOperation == BATCH_OPERATION_SAVE) {
            mBtnunSelectAll.setVisibility(View.VISIBLE);
            mBtnSelectAll.setVisibility(View.GONE);
        } else {
            mBtnunSelectAll.setVisibility(View.GONE);
            mBtnSelectAll.setVisibility(View.VISIBLE);
        }
        boolean batchMode = batchOperation != BATCH_OPERATION_NONE;
        if (batchMode) {
            mActivity.findViewById(R.id.bottom_panel).setVisibility(View.VISIBLE);
            composeNewMessageImg.setVisibility(View.GONE);
            Button action = (Button) mActivity.findViewById(R.id.performAction);
            // Need to update string of these views to handle Language Change
            mBtnSelectAll.setText(R.string.select_all);
            mBtnunSelectAll.setText(R.string.unselect_all);
            if (batchOperation == BATCH_OPERATION_DELETE) {
                action.setText(R.string.delete);
            } else {
                action.setText(R.string.save);
            }

            if (mBatchModeController.getBatchedThreads().size() == 0) {
                mActivity.findViewById(R.id.performAction).setEnabled(false);
            }
            parentActivity.disableFragments();
        } else {
            mBatchModeController.clearIds();
            mActivity.findViewById(R.id.bottom_panel).setVisibility(View.GONE);
            composeNewMessageImg.setVisibility(View.VISIBLE);
            parentActivity.enableFragments();
        }
        mAdapter.setBatchMode(batchMode);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == SDCardStatus.SD_CARD_UNMOUNTED_CLOSE_ACTIVITY) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "SD_CARD_UNMOUNTED_CLOSE_ACTIVITY");
            }
            sdCardNotConnectedPopUp();
        }
        // super.onActivityResult(requestCode, resultCode, data);
        // super.onActivityResult(requestCode, resultCode, data);
    }

    void sdCardNotConnectedPopUp() {
        final Dialog d = new AppAlignedDialog(mActivity, R.drawable.dialog_alert, R.string.error,
                R.string.error_sd_card_not_connected);
        d.setCancelable(false);
        Button yesButton = (Button) d.findViewById(R.id.positive_button);
        yesButton.setText(R.string.yes);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActive = false;
        if (mAdapter != null) {
            mAdapter.shutdown();
            mAdapter = null;
        }
    }

    @Override
    public void onDestroyView() {
        // all the listeners have to be removed here and
        // the adapter has to be set to null
        // because there are chances of onCreateView getting called after
        // onDestroyView
        setListAdapter(null);

        // remove the sync status broadcast receiver
        mActivity.unregisterReceiver(syncStatusReceiver);

        if (convDeletionDialog != null && convDeletionDialog.isShowing()) {
            convDeletionDialog.dismiss();
        }
        if (mAsyncTask != null) {
            mAsyncTask.cancel(true);
        }
        mQueryHandler.cancelOperation(THREAD_LIST_QUERY_TOKEN);
        mQueryHandler.cancelOperation(HAVE_LOCKED_MESSAGES_TOKEN);

        super.onDestroyView();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onDestroy()");
        }
        if (isGCMregistered) {
            try {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("GCM: Destorying");
                }
                GCMRegistrar.onDestroy(settings.getContext());
            } catch (Exception e) {
                if (Logger.IS_ERROR_ENABLED) {
                    Logger.error("GCM", e);
                }
            }
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("GCM was not registered");
            }
        }

        isGCMregistered = false;

        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.conversation_list_screen, container, false);
    }

    @Override
    public void onStart() {
        MessagingNotification.cancelNotification(mActivity.getApplicationContext(),
                SmsRejectedReceiver.SMS_REJECTED_NOTIFICATION_ID);
        DraftCache.getInstance().addOnDraftChangedListener(this);
        startAsyncQuery();

        // invalidate cache if it is not in the process
        // of loading the contacts
        /*
         * if (!Contact.isLoadingContacts()) { Contact.invalidateCache(); }
         */
        if (MmsConfig.isTabletDevice()) {
            showPendingProvisioningInfo(true);
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        DraftCache.getInstance().removeOnDraftChangedListener(this);
        mAdapter.changeCursor(null);

        super.onStop();
    }

    
    @Override
	public void onPause() {
    	launchCompose = false;
    	
		super.onPause();
	}

	private void initListAdapter() {
        mBatchModeController = new BatchModeController();

        mAdapter = new ConversationListAdapter(getActivity(), getListView(), null, mBatchModeController);
        mAdapter.setOnContentChangedListener(mContentChangedListener);
        setListAdapter(mAdapter);

        mBtnSelectAll = (Button) mActivity.findViewById(R.id.selectAll);
        mBtnSelectAll.setOnClickListener(this);
        mBtnunSelectAll = (Button) mActivity.findViewById(R.id.unSelectAll);
        mBtnunSelectAll.setOnClickListener(this);
        mBtnunSelectAll.setVisibility(View.GONE);
        mActivity.findViewById(R.id.performAction).setOnClickListener(this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onListItemClick: position=" + position + ", id=" + id);
        }

        // Note: don't read the thread id data from the ConversationListItem view passed in.
        // It's unreliable to read the cached data stored in the view because the ListItem
        // can be recycled, and the same view could be assigned to a different position
        // if you click the list item fast enough. Instead, get the cursor at the position
        // clicked and load the data from the cursor.
        // (ConversationListAdapter extends CursorAdapter, so getItemAtPosition() should
        // return the cursor object, which is moved to the position passed in)
        if (mBatchOperation != BATCH_OPERATION_NONE) {
            mAdapter.toggleCheckBox(v);
        } else {
            Cursor cursor = (Cursor) getListView().getItemAtPosition(position);
            Conversation conv = Conversation.from(mActivity, cursor);
            selectedThreadId = conv.getThreadId();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "onListItemClick: pos=" + position + ", view=" + v + ", tid="
                        + selectedThreadId);
            }
            openThread(selectedThreadId);
            Util.hideKeyboard(mActivity, getView());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.composebutton:
            createNewMessage();
            break;
        case R.id.imgGallery:
            goToMediaShoebox();
            break;
        case R.id.selectAll:
            setBatchAllSelection(true);
            mBtnunSelectAll.setVisibility(View.VISIBLE);
            mBtnSelectAll.setVisibility(View.GONE);
            break;
        case R.id.unSelectAll:
            setBatchAllSelection(false);
            mBtnunSelectAll.setVisibility(View.GONE);
            mBtnSelectAll.setVisibility(View.VISIBLE);
            break;
        case R.id.performAction:
            ArrayList<Long> batchThreads = mBatchModeController.getBatchedThreads();
            if (mBatchOperation == BATCH_OPERATION_DELETE) {
                deleteSelectedThread(batchThreads);
            } else if (mBatchOperation == BATCH_OPERATION_SAVE) {

                startSaveRestoreActivity(batchThreads);
                setBatchMode(BATCH_OPERATION_NONE);
            }
            break;

        case R.id.sync_idle:
            showRetryIdle();
            break;
        }
    }

    private void showRetryIdle() {
        final Dialog retryIdle = new AppAlignedDialog(mActivity, R.drawable.launcher_home_icon,
                R.string.vma_server_error, R.string.vma_retry_sync_text);
        retryIdle.setCancelable(true);
        Button continueButton = (Button) retryIdle.findViewById(R.id.positive_button);
        continueButton.setText(R.string.vma_retry_sync);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent syncIntent = new Intent(SyncManager.ACTION_START_VMA_SYNC);
                syncIntent.putExtra(SyncManager.EXTRA_SYNC_TYPE, SyncManager.SYNC_MANUAL);
                mActivity.startService(syncIntent);
                idleStatus.setVisibility(View.GONE);
                retryIdle.dismiss();
            }
        });
        Button cancelButton = (Button) retryIdle.findViewById(R.id.negative_button);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setText(R.string.wait);
        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                retryIdle.dismiss();
            }

        });
        retryIdle.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                retryIdle.dismiss();
            }
        });
        retryIdle.show();
    }

    /**
     * This Method
     */
    private void showSaveNotSupported(final ArrayList<Long> threads, String msgString, final boolean doSave) {
        final Dialog d = new AppAlignedDialog(mActivity, R.drawable.dialog_alert, R.string.menu_save,
                msgString);
        Button positiveButton = (Button) d.findViewById(R.id.positive_button);
        positiveButton.setText(R.string.yes);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (doSave) {
                    startSaveRestoreActivity(threads);
                    d.dismiss();
                } else {
                    d.dismiss();
                    return;
                }
            }
        });

        Button negativeButton = (Button) d.findViewById(R.id.negative_button);
        negativeButton.setVisibility(View.VISIBLE);
        negativeButton.setText(R.string.cancel);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    private void deleteSelectedThread(final ArrayList<Long> threads) {
        // commented out as there are chances of
        // new incoming conversation to be deleted
        /*
         * if (threads.size() == mAdapter.getCount()) { confirmDeleteThreadDialog(new
         * DeleteThreadListener(-1l, mQueryHandler, mActivity), true, false, mActivity); } else
         */{
            int msgId = threads.size() == 1 ? R.string.confirm_delete_conversation
                    : R.string.confirm_del_sel_conversation;
            confirmDeleteThreadDialog(msgId, mActivity, threads);
        }
    }

    private boolean canSaveProcessOccur(ArrayList<Long> threads) {
        // Check if threads have sms to save

        for (Long threadID : threads) {
            if (Conversation.hasSMSMessages(mActivity, threadID)) {
                return true;
            }
        }

        return false;

    }

    /**
     * This Method
     * 
     * @param threads
     */
    private void startSaveRestoreActivity(ArrayList<Long> threads) {
        Intent sdIntent = new Intent(mActivity, SaveRestoreActivity.class);
        sdIntent.putExtra(SaveRestoreActivity.EXTRA_SAVE_TYPE, SaveRestoreActivity.SAVE_THREADS_TO_XML);
        sdIntent.putExtra(SaveRestoreActivity.EXTRA_BATCH_THREADS, threads);
        startActivityForResult(sdIntent, SDCardStatus.SD_CARD_EJECTED);
    }

    private void openThread(long threadId) {
        if (mAdapter != null) {
            mAdapter.resetUnreadCount(threadId);
        }
        parentActivity.launchComposeView(mActivity, threadId);
    }

    private void openThread(long threadId, boolean showKeyboard) {
        parentActivity.launchComposeView(mActivity, threadId, showKeyboard);
    }

    public void startAsyncQuery() {
        try {
            mActivity.setTitle(getString(R.string.refreshing));
            mActivity.setProgressBarIndeterminateVisibility(true);

            if (!launchCompose) {
                mQueryHandler.startThreadListQuery();
            }
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(mActivity, e);
        }
    }

    @Override
    public void onDraftChanged(final long threadId, final boolean hasDraft) {
        // Run notifyDataSetChanged() on the main thread.
        mQueryHandler.post(new Runnable() {
            public void run() {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "onDraftChanged: threadId=" + threadId + ", hasDraft="
                            + hasDraft);
                }
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    public void createNewMessage() {
        if (isMultipaneUI) {
            parentActivity.launchComposeView(mActivity, 0L, true);
        } else {
            Intent intent = ComposeMessageActivity.createIntent(mActivity, 0L, true);
            intent.putExtra("compose_new", true);
            startActivity(intent);
        }

    }

    public void goToMediaShoebox() {

        if (!isMultipaneUI) {
            Intent i = new Intent(getActivity(), SharedContentActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } else {
            parentActivity.setGalleryLoaded(true);
            Intent i = getActivity().getIntent();
            i.putExtra("thread_id", -1L);
            i.putExtra("members", "");
            i.putExtra("is_compose", false);
            parentActivity.launchComposeView(i);
        }

    }

    private void setConversationMenu(int pos) {
        Cursor cursor = mAdapter.getCursor();
        if (cursor == null) {
            Logger.error(getClass(), "setConversationMenu: called with null cursor");
            return;
        }
        // make sure the cursor is positioned at the selected item
        if (!cursor.moveToPosition(pos)) {
            Logger.error(getClass(), "setConversationMenu: failed to move to pos " + pos + ", count = "
                    + cursor.getCount());
            return;
        }
        Conversation conv = Conversation.from(mActivity, cursor);
        selectedThreadId = conv.getThreadId();
        ContactList recipients = conv.getRecipients();
        QuickAction mConvMenu = new QuickAction(mActivity);
        mConvMenu.setTitle(recipients.formatNames());
        ActionItem actionItem = new ActionItem(MENU_SEND_MESSAGE, R.string.send_message, 0);
        mConvMenu.addActionItem(actionItem);
        actionItem = new ActionItem(MENU_VIEW, R.string.menu_view, 0);
        mConvMenu.addActionItem(actionItem);

        // Only show if there's a single recipient
        if (recipients.size() == 1) {
            // do we have this recipient in contacts?
            if (recipients.get(0).existsInDatabase()) {
                actionItem = new ActionItem(MENU_VIEW_CONTACT, R.string.menu_view_contact, 0);
                mConvMenu.addActionItem(actionItem);
            } else {
                actionItem = new ActionItem(MENU_ADD_TO_CONTACTS, R.string.menu_add_to_contacts, 0);
                mConvMenu.addActionItem(actionItem);
            }
            String address = recipients.get(0).getNumber();
            if (Mms.isEmailAddress(address)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + address));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                actionItem = new ActionItem(MENU_SEND_EMAIL, R.string.send_email, 0);
                actionItem.setTag(intent);
                mConvMenu.addActionItem(actionItem);
            } else {
                if (!MmsConfig.isTabletDevice()) {
                    String callBackString = getString(R.string.menu_call_back).replace("%s", "");
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + address));
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    actionItem = new ActionItem(MENU_CALL_BACK, callBackString, 0);
                    actionItem.setTag(intent);
                    mConvMenu.addActionItem(actionItem);
                }
            }

        } else {
            actionItem = new ActionItem(MENU_CONVERSATION_DETAILS, R.string.menu_conversation_detail, 0);
            mConvMenu.addActionItem(actionItem);
        }
        actionItem = new ActionItem(MENU_DELETE, R.string.menu_delete_conv, 0);
        mConvMenu.addActionItem(actionItem);
        if (conv.hasDraft() && conv.getMessageCount() == 0) {
            // ignore
        } else {
            if (!VZUris.isTabletDevice()) {
                actionItem = new ActionItem(MENU_SAVE_CONV, R.string.save_messages_text, 0);
                mConvMenu.addActionItem(actionItem);
                /*
                 * actionItem = new ActionItem(MENU_SAVE_CSV, R.string.save_csv_text, 0);
                 * mConvMenu.addActionItem(actionItem);
                 */
            }

        }

        mConvMenu.setOnActionItemClickListener(mConvClickListener);
        mConvMenu.show(null, getView(), true);

    }

    QuickAction.OnActionItemClickListener mConvClickListener = new QuickAction.OnActionItemClickListener() {
        @Override
        public void onItemClick(QuickAction source, int pos, int actionId) {
            Cursor cursor = mAdapter.getCursor();
            if (cursor != null && cursor.getPosition() >= 0) {
                Conversation conv = Conversation.get(mActivity, selectedThreadId, false);
                switch (actionId) {
                case MENU_DELETE: {
                    /*
                     * confirmDeleteThreadDialog(new DeleteThreadListener(selectedThreadId, mQueryHandler,
                     * mActivity), false, false, mActivity);
                     */
                    ArrayList<Long> list = new ArrayList<Long>();
                    list.add(selectedThreadId);
                    deleteSelectedThread(list);
                    break;
                }
                case MENU_SEND_MESSAGE: {
                    openThread(selectedThreadId, true);
                    break;
                }
                case MENU_VIEW: {
                    openThread(selectedThreadId);
                    break;
                }
                case MENU_VIEW_CONTACT: {
                    ContactList contactList = conv.getRecipients();

                    if (contactList.size() > 0) {
                        Contact c = contactList.get(0);
                        c.markAsStale();
                        if (c.existsInDatabase()) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, c.getUri());
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                            startActivity(intent);
                        }
                    }
                    break;
                }
                case MENU_ADD_TO_CONTACTS: {
                    ContactList contactList = conv.getRecipients();

                    if (contactList.size() > 0) {
                        Contact c = contactList.get(0);
                        c.markAsStale();
                        startActivity(createAddContactIntent(c.getNumber()));
                    }
                    break;
                }
                case MENU_CONVERSATION_DETAILS: {
                    showAllRecipients(selectedThreadId);
                    break;
                }
                // case MENU_SAVE_CSV:
                case MENU_SAVE_CONV: {
                    ArrayList<Long> threadsList = new ArrayList<Long>();
                    threadsList.add(selectedThreadId);
                    startSaveRestoreActivity(threadsList);
                    break;
                }

                case MENU_CALL_BACK: {
                    startActivity((Intent) source.getActionItem(pos).getTag());
                    break;
                }
                case MENU_SEND_EMAIL:
                    startActivity((Intent) source.getActionItem(pos).getTag());
                    break;
                default:
                    break;
                }
            }
        }
    };

    protected void resetTablet() {
        mAsyncTask = new AsyncTask<Void, String, Void>() {
            ProgressDialog dialog;

            protected void onPreExecute() {

                dialog = ProgressDialog.show(mActivity, null, getString(R.string.vma_erasing_reconnecting));
            }

            @Override
            protected Void doInBackground(Void... params) {
                settings.resetTablet();
                return null;
            };

            protected void onPostExecute(Void result) {
                settings.put(AppSettings.KEY_VMA_TAB_OFFLINE_MODE, false);
                settings.put(AppSettings.KEY_CANT_SEND_MESSAGE, false);
                dialog.dismiss();
            	if(Logger.IS_DEBUG_ENABLED) {
        			Logger.debug(getClass(),"starting Provisioning activity from resetTablet");
        		}
                Intent i = new Intent(mActivity, Provisioning.class);
                i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                mActivity.finish();
            };
        }.execute();
    }

    /**
     * MENU_CONVERSATION_DETAILS will display the recipient(s) in the conversation in Recipients List and the
     * option to "Create Message" at the bottom of the screen in case of Group Message
     * 
     * @param threadId
     */
    private void showAllRecipients(long threadId) {
        Intent intent = new Intent(mActivity, RecipientListActivity.class);
        if (threadId != 0) {
            intent.putExtra(RecipientListActivity.THREAD_ID, threadId);
        }
        startActivity(intent);
    }

    public void showMenuTipsAndReminders() {
        // Tips and Reminders option
        final Dialog d = new AppAlignedDialog((Context) mActivity, R.drawable.launcher_home_icon,
                R.string.menu_tips, MmsConfig.isTabletDevice());
        Button cancelButton = (Button) d.findViewById(R.id.positive_button);
        cancelButton.setText(R.string.button_ok);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();

    }

    public void showTermsAndCondition() {
        final AppAlignedDialog d = new AppAlignedDialog(mActivity, R.drawable.launcher_home_icon,
                R.string.tnc_title, getString(R.string.menu_terms_of_service));
        TextView message = (TextView) d.findViewById(R.id.dialog_message);
        message.setText(R.string.menu_vma_terms_of_service);
        message.setMovementMethod(LinkMovementMethod.getInstance());
        Button okButton = (Button) d.findViewById(R.id.positive_button);
        okButton.setText(R.string.button_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();

            }
        });
        d.dismiss();
        d.show();
    }

    public void showMenuAbout() {
        String title = getString(R.string.about_app);
        try {
            if (settings.isReleaseBuild()) {
                title = getString(R.string.app_name) + ": 4.0.5";
            } else {
                PackageInfo info = getActivity().getPackageManager().getPackageInfo(
                        getActivity().getPackageName(), 0);
                title = String.format(getString(R.string.about_app), info.versionName, info.versionCode);
            }
        } catch (Exception e) {

        }
        final Dialog d = new AppAlignedDialog((Context) mActivity, R.drawable.launcher_home_icon,
                R.string.menu_about, title);
        d.setCancelable(false);
        Button okButton = (Button) d.findViewById(R.id.positive_button);
        okButton.setText(R.string.button_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();

    }

    public final class ThreadListQueryHandler extends AsyncQueryHandler {
        private int lastQuery;

        public ThreadListQueryHandler(Handler mHandler, ContentResolver contentResolver) {
            super(contentResolver);
        }

        public void startThreadListQuery() {
            // start the query with a unique cookie
            synchronized (this) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "ConversationListFragment.startThreadListQuery: active = "
                            + mActive + ", cookie = " + (lastQuery + 1));
                    queryStart = SystemClock.uptimeMillis();
                }
                if (mActive) {
                    Conversation.startQueryForAll(mQueryHandler, THREAD_LIST_QUERY_TOKEN, ++lastQuery);
                }
            }
        }

        @Override
        public void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "ConversationListFragment.onQueryComplete: token = " + token
                        + ", cookie = " + cookie + ", count = " + (cursor == null ? -1 : cursor.getCount())
                        + ", active = " + mActive + ", time = " + (SystemClock.uptimeMillis() - queryStart)
                        + " ms");
            }
            switch (token) {
            case THREAD_LIST_QUERY_TOKEN:
                // ignore stale query results
                synchronized (this) {
                    if (!mActive || (Integer) cookie != lastQuery) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(),
                                    "ConversationListFragment.onQueryComplete: ignoring stale query");
                        }
                        if (cursor != null) {
                            cursor.close();
                        }
                        return;
                    }
                }

                if (cursor == null) {
                    showErrorAndExit();
                    if (mAdapter != null) {
                        mAdapter.changeCursor(null);
                    }
                    return;
                }

                if (mAdapter != null) {
                    long threadId = 0;
                    if (cursor.getCount() > 0 && selectFirstConversation) {
                        int pos = cursor.getPosition();
                        if (cursor.moveToFirst()) {
                            threadId = cursor.getLong(Conversation.ID);
                        }
                        cursor.moveToPosition(pos);
                    }
                    selectFirstConversation = false;

                    mAdapter.changeCursor(cursor);

                    // Activity created so select the first conversation
                    if (threadId != 0) {
                        ((ConversationListActivity) mActivity).updateCurrentThreadID(threadId, this
                                .getClass().toString());
                        openThread(threadId);
                    }

                    mActivity.setTitle(mActivity.getString(R.string.app_label));
                    mActivity.setProgressBarIndeterminateVisibility(false);

                    if (mNeedToMarkAsSeen) {
                        mNeedToMarkAsSeen = false;
                        Conversation.markAllConversationsAsSeen(mActivity.getApplicationContext());
                    }

                    // Delete obsolete threads only once when the conversationlist is loaded for the first
                    // time
                    if (mDeleteObsoleteThread) {
                        // Delete any obsolete threads. Obsolete threads are threads that aren't
                        // referenced by at least one message in the pdu or sms tables.
                        mDeleteObsoleteThread = false;
                        Conversation.asyncDeleteObsoleteThreads(mQueryHandler, DELETE_OBSOLETE_THREADS_TOKEN);
                    }
                }
                break;

            case HAVE_LOCKED_MESSAGES_TOKEN:
                long threadId = (Long) cookie;
                confirmDeleteThreadDialog(new DeleteThreadListener(threadId, mQueryHandler, mActivity),
                        threadId == -1, cursor != null && cursor.getCount() > 0, mActivity);
                break;

            default:
                Logger.error(getClass(), "ListFragment", "onQueryComplete called with unknown token " + token);
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            if (mAdapter != null) {
                mAdapter.setDeleteInProgress(false);
            }
            switch (token) {
            case DELETE_CONVERSATION_TOKEN:
                // Dismiss the progress dialog
                if (convDeletionDialog.isShowing()) {
                    convDeletionDialog.dismiss();
                }
                // If batch mode is set remove it
                setBatchMode(BATCH_OPERATION_NONE);
                onDeleteSelectAnother();
                break;

            case DELETE_OBSOLETE_THREADS_TOKEN:
                // Nothing to do here.
                batchCount = 1;
                onDeleteSelectAnother();
                break;
            }
        }
    }

    public void onDeleteSelectAnother() {
        if (!isMultipaneUI) {
            return;
        }

        // Implies that message deleted was open.
        if (parentActivity.mLastOpenThreadID == parentActivity.mLastDeletedThreadID) {
            boolean foundThreadID = false;
            Cursor cursor = mAdapter.getCursor();
            if (cursor == null)
                return;
            cursor.moveToNext();

            while (cursor.moveToPrevious()) {
                Conversation conv = Conversation.from(mActivity, cursor);
                if (parentActivity.mLastOpenThreadID == conv.getThreadId()) {// need to move two time up one
                                                                             // for the null and last row
                                                                             // each.

                    if (!cursor.moveToPrevious()) {

                        if (cursor.moveToNext() && cursor.moveToNext()) {

                            foundThreadID = true;
                            // successfully moved back.
                        }
                    } else {
                        foundThreadID = true; // Successfully move forward.
                    }
                    break;
                }

            }

            if (foundThreadID && batchCount == 1) {
                Conversation conv = Conversation.from(mActivity, cursor);
                long tid = conv.getThreadId();
                openThread(tid);
            } else {
                createNewMessage();
            }
        } else if (parentActivity.mLastDeletedThreadID == -1l) {
            // - 1 if all conversation were deleted
            createNewMessage();
        }
        // reset parameters.
        batchCount = 0;
        parentActivity.mLastDeletedThreadID = -5l;// -1l is reserved for deleting all threads.

    }

    /**
     * Build and show the proper delete thread dialog. The UI is slightly different depending on whether there
     * are locked messages in the thread(s) and whether we're deleting a single thread or all threads.
     * 
     * @param listener
     *            gets called when the delete button is pressed
     * @param deleteAll
     *            whether to show a single thread or all threads UI
     * @param hasLockedMessages
     *            whether the thread(s) contain locked messages
     * @param context
     *            used to load the various UI elements
     */
    public static void confirmDeleteThreadDialog(final DeleteThreadClickListener listener, boolean deleteAll,
            boolean hasLockedMessages, Context context) {
        View contents = View.inflate(context, R.layout.delete_thread_dialog_view, null);
        TextView msg = (TextView) contents.findViewById(R.id.message);
        msg.setText(deleteAll ? R.string.confirm_delete_all_conversations
                : R.string.confirm_delete_conversation);
        final CheckBox checkbox = (CheckBox) contents.findViewById(R.id.delete_locked);
        if (!hasLockedMessages) {
            checkbox.setVisibility(View.GONE);
        } else {
            listener.setDeleteLockedMessage(checkbox.isChecked());
            checkbox.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    listener.setDeleteLockedMessage(checkbox.isChecked());
                }
            });
        }

        final Dialog d = new AppAlignedDialog(context, R.drawable.dialog_alert,
                R.string.confirm_dialog_title, 0);
        LinearLayout ll = (LinearLayout) d.findViewById(R.id.dialog_info_layout);
        ll.addView(contents);
        Button deleteButton = (Button) d.findViewById(R.id.positive_button);
        deleteButton.setText(R.string.delete);
        listener.setDialog(d);
        deleteButton.setOnClickListener(listener);
        Button noButton = (Button) d.findViewById(R.id.negative_button);
        noButton.setVisibility(View.VISIBLE);
        noButton.setText(R.string.no);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    /**
     * Build and show the proper delete thread dialog.
     * 
     */
    public void confirmDeleteThreadDialog(int messageId, final Context context, final ArrayList<Long> threads) {
        final Dialog d = new AppAlignedDialog(context, R.drawable.dialog_alert,
                R.string.confirm_dialog_title, messageId);
        Button deleteButton = (Button) d.findViewById(R.id.positive_button);
        deleteButton.setText(R.string.delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convDeletionDialog = new ProgressDialog(context);
                convDeletionDialog.setMessage(context.getString(R.string.sync_delete_progress));
                convDeletionDialog.setCancelable(false);
                convDeletionDialog.show();

                // delay the cursor update only if we are trying to delete more than 3 conversation
                if (mAdapter != null && threads.size() > 3) {
                    mAdapter.setDeleteInProgress(true);
                }

                Conversation.asyncDeleteSelected(context, mBatchQueryHandler, MSG_BATCH_QUERY_COMPLETE,
                        MSG_DEL_SYNC_COMPLETE, threads);
                // check if batch contains the thread that is open
                if (threads.contains(parentActivity.mLastOpenThreadID)) {
                    parentActivity.mLastDeletedThreadID = parentActivity.mLastOpenThreadID;
                    batchCount = threads.size();// IF delete batch is more then one then its hard to find as
                                                // to which what is next message hence will load blank
                                                // compose.
                }

                d.dismiss();
            }
        });
        Button noButton = (Button) d.findViewById(R.id.negative_button);
        noButton.setVisibility(View.VISIBLE);
        noButton.setText(R.string.no);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    public static Intent createAddContactIntent(String address) {
        // address must be a single recipient
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        if (Mms.isEmailAddress(address)) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, address);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, address);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        return intent;
    }

    public void showErrorAndExit() {
        if (exitDialog != null) {

            exitDialog.dismiss();
        }

        exitDialog = new AppAlignedDialog(mActivity, R.drawable.dialog_alert, R.string.error,
                R.string.cursor_error);
        exitDialog.setCancelable(false);
        Button yesButton = (Button) exitDialog.findViewById(R.id.positive_button);
        yesButton.setText(R.string.ok);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitDialog.dismiss();
                mActivity.finish();
            }
        });
        exitDialog.show();
    }

    public static abstract class DeleteThreadClickListener implements View.OnClickListener {
        protected final long mThreadId;
        protected final AsyncQueryHandler mHandler;
        protected final Context mContext;
        protected boolean mDeleteLockedMessages;
        protected Dialog dialog;

        public DeleteThreadClickListener(long threadId, AsyncQueryHandler handler, Context context) {
            mThreadId = threadId;
            mHandler = handler;
            mContext = context;
        }

        public void setDeleteLockedMessage(boolean deleteLockedMessages) {
            mDeleteLockedMessages = deleteLockedMessages;
        }

        public void setDialog(Dialog d) {
            dialog = d;
        }

        abstract public void onClick(View v);
    }

    public class DeleteThreadListener extends DeleteThreadClickListener {
        public DeleteThreadListener(long threadId, AsyncQueryHandler handler, Context context) {
            super(threadId, handler, context);
        }

        @Override
        public void onClick(View v) {
            dialog.dismiss();

            parentActivity.mLastDeletedThreadID = mThreadId;
            batchCount = 1;
            int token = DELETE_CONVERSATION_TOKEN;

            convDeletionDialog.setMessage(mContext.getString(R.string.deleting));
            convDeletionDialog.setCancelable(false);
            convDeletionDialog.show();
            /* Wifi Sync HookUps */
            if (mThreadId == -1) {
                Conversation.startDeleteAll(mContext, mHandler, token, mDeleteLockedMessages);
                DraftCache.getInstance().refresh();

            } else {
                Conversation.startDelete(mContext, mHandler, token, mDeleteLockedMessages, mThreadId);
                DraftCache.getInstance().setDraftState(mThreadId, false);

            }
            // notify adapter
            if (mAdapter != null) {
                mAdapter.setCursorChanging(true);
            }
        }
    }

    /**
     * public void startWifiSync(){ if(Logger.IS_DEBUG_ENABLED){ Logger.debug("Starting wifi sync service"); }
     * mActivity.startService(new Intent(SyncManager.ACTION_START_WIFI_SYNC)); }
     * 
     * public void showWifiPairingDialog(){ if(Logger.IS_DEBUG_ENABLED){
     * Logger.debug("staring pairing or unpairing service."); }
     * 
     * WifiSyncPairingDlg pairingDlg = new WifiSyncPairingDlg(mActivity); pairingDlg.show(); }
     **/

    class BatchModeController extends BatchModeThreadsController {

        public BatchModeController() {
            super();
        }

        @Override
        public void onThreadsUpdated() {
            if (mThreads.size() == 0) {
                mBtnunSelectAll.setVisibility(View.GONE);
                mBtnSelectAll.setVisibility(View.VISIBLE);
                mActivity.findViewById(R.id.performAction).setEnabled(false);
            } else {
                mBtnunSelectAll.setVisibility(View.VISIBLE);
                mBtnSelectAll.setVisibility(View.GONE);
                mActivity.findViewById(R.id.performAction).setEnabled(true);
            }
        }

    }

    private void setBatchAllSelection(boolean selected) {
        ArrayList<Long> batchThreads = mBatchModeController.getBatchedThreads();
        if (selected) {
            Cursor c = mAdapter.getCursor();
            if (c.moveToFirst()) {
                do {
                    long id = c.getLong(0);
                    if (!batchThreads.contains(id)) {
                        batchThreads.add(id);
                    }
                } while (c.moveToNext());
            }
            mActivity.findViewById(R.id.performAction).setEnabled(true);
        } else {
            batchThreads.clear();
            mActivity.findViewById(R.id.performAction).setEnabled(false);
        }

        mAdapter.notifyDataSetChanged();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            // check if we are in batchmode if so then remove it
            if (mBatchOperation != BATCH_OPERATION_NONE) {
                setBatchMode(BATCH_OPERATION_NONE);
                return true;
            }
            break;
        case KeyEvent.KEYCODE_SEARCH:
            Intent searchIntent = new Intent(getActivity(), SearchActivity.class);
            getActivity().startActivity(searchIntent);
            return true;
        }

        return false;
    }

    public void init(ConversationListActivity conversationListActivity) {
        parentActivity = conversationListActivity;
    }

    @Override
    public boolean onActionItemSelected(ActionItem item) {
        int actionId = item.getActionId();
        switch (actionId) {

        /*
         * case MENU_VMA_RESET: settings.resetTablet(); return true;
         */
        case MENU_UNPAIR_DEVICE:
            // showWifiPairingDialog();
            return true;
        case MENU_TIPS:
            showMenuTipsAndReminders();
            return true;
        case MENU_SEND_COMMENTS:
            startActivity(new Intent(mActivity, SendCommentsToDeveloper.class));
            return true;
        case MENU_SEARCH:
            Intent searchIntent = new Intent(getActivity(), SearchActivity.class);
            getActivity().startActivity(searchIntent);
            return true;
        case MENU_ABOUT:
            showMenuAbout();
            return true;
        case MENU_RECONNECT:
            addReconnect();
            return true;
        case MENU_DUMP_DB: {
            // final String fname = Util.saveMessageDb(mActivity);
            // Toast.makeText(mActivity,
            // fname == null ? "Error saving DB" : "Saved DB to " + fname,
            // Toast.LENGTH_LONG).show();
            Util.saveDb(mActivity);
            return true;
        }
        case MENU_DUMP_LOG: {
            final String fname = Util.saveLocalLog(mActivity);
            Toast.makeText(mActivity, fname == null ? "Error saving log" : "Saved log to " + fname,
                    Toast.LENGTH_LONG).show();
            return true;
        }
        case REPORT_BUG:
            reportABug();
            return true;
        case MENU_EMAIL_TRACES: {
            Util.emailTraces(mActivity);
            return true;
        }
        case MENU_EMAIL_MEM:
            Util.email(mActivity, "pjeffe@gmail.com", "memory info", BitmapManager.INSTANCE.checkMemory(),
                    false, false);
            return true;
        case MENU_DEBUG_PANEL:
            startActivity(new Intent(mActivity, DebugPanelActivity.class));
            return true;
        case MENU_SYNC:
            Intent syncIntent = new Intent(SyncManager.ACTION_START_VMA_SYNC);
            syncIntent.putExtra(SyncManager.EXTRA_SYNC_TYPE, SyncManager.SYNC_MANUAL);
            mActivity.startService(syncIntent);
            // startWifiSync();
            return true;
        case MENU_COMPOSE_NEW:
            createNewMessage();
            return true;
        case MENU_RESTORE_MESSAGES:
            Intent restoreIntent = new Intent(getActivity(), SaveRestoreActivity.class);
            restoreIntent.putExtra("showRestoreFiles", true);
            startActivityForResult(restoreIntent, SDCardStatus.SD_CARD_EJECTED);
            return true;
        case SHOW_TERMS_AND_CONDITION:
            showTermsAndCondition();
            return true;
        case SHOW_PRIVACY_POLICY:
            Intent intentUrl = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www22.verizon.com/about/privacy/"));
            startActivity(intentUrl);
            return true;
        case MENU_DELETE_CONVERSATIONS:
            setBatchMode(BATCH_OPERATION_DELETE);
            return true;
        case MENU_SAVE_CONVERSATIONS:
            setBatchMode(BATCH_OPERATION_SAVE);
            setBatchAllSelection(true);
            return true;
        case MENU_PREFERENCES: {

            Intent intent = new Intent(getActivity(), MessagingPreferenceActivity.class);
            getActivity().startActivityIfNeeded(intent, -1);

            return true;
        }
        case MENU_CANCEL_BATCH:
            setBatchMode(BATCH_OPERATION_NONE);
            return true;
        case MENU_SHOW_STATUS_DETAILS:
            showMessageDetails();
            return true;
        case MENU_SHOW_POPUP:
            simulatePopup();
            return true;
        case MENU_GENERATE_SMS:
            // GenerateSMS genSms = new GenerateSMS(2000, mActivity);
            // genSms.execute();
            return true;
        }
        return false;
    }

    private void showMessageDetails() {
        final Dialog d = new AppAlignedDialog(mActivity, 0, "Message details",
                "Please enter a conversation thread number");

        final EditText input = new EditText(mActivity);
        LinearLayout ll = (LinearLayout) d.findViewById(R.id.dialog_info_layout);
        ll.addView(input);
        Button yesButton = (Button) d.findViewById(R.id.positive_button);
        yesButton.setText(R.string.yes);
        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager mgr = (InputMethodManager) mActivity
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(input.getWindowToken(), 0);
                Editable etText = input.getText();
                String text = etText.toString();
                if (etText != null && !text.trim().equals("")) {
                    long threadId = 0;
                    try {
                        threadId = Long.parseLong(text);
                        new MessageDetails(mActivity, threadId).execute();
                    } catch (NumberFormatException e) {
                        input.setError("thread is always a long value");
                    }
                }
                d.dismiss();
            }
        });
        Button noButton = (Button) d.findViewById(R.id.negative_button);
        noButton.setVisibility(View.VISIBLE);
        noButton.setText(R.string.cancel);
        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });

        d.show();
    }

    private void reportABug() {

        final Dialog dialog = new AppAlignedDialog(mActivity, 0, getString(R.string.menu_report_a_bug),
                getString(R.string.vma_bug_report_lable));
        final EditText input = (EditText) dialog.findViewById(R.id.app_aligned_edit);
        input.setVisibility(View.VISIBLE);
        Button button = (Button) dialog.findViewById(R.id.positive_button);
        button.setText(R.string.send);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String subject = input.getText().toString();
                if (!TextUtils.isEmpty(input.getText())) {
                    new ExportLogs(mActivity, subject).execute();
                    dialog.dismiss();
                } else {
                    input.setError(getString(R.string.vma_describe_error));
                }
            }

        });

        Button cancelButton = (Button) dialog.findViewById(R.id.negative_button);
        cancelButton.setText(R.string.cancel);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void addReconnect() {
        final Dialog dialog = new AppAlignedDialog(mActivity, R.drawable.dialog_alert,
                R.string.vma_erase_reconnect, R.string.reconnect);
        Button okButton = (Button) dialog.findViewById(R.id.positive_button);
        okButton.setText(R.string.button_ok);
        okButton.setVisibility(View.VISIBLE);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (VZUris.isTabletDevice()) {
                    resetTablet();
                }
            }
        });

        Button negativeButton = (Button) dialog.findViewById(R.id.negative_button);
        negativeButton.setText(R.string.cancel);
        negativeButton.setVisibility(View.VISIBLE);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    public void onPrepareActionMenu(ActionMenuBuilder menu) {
        if (mBatchOperation != BATCH_OPERATION_NONE) {
            menu.add(0, MENU_CANCEL_BATCH, 0, R.string.no, 0, null); // .setIcon(R.drawable.ic_menu_batch_mode);
        } else {
    //        menu.add(0, MENU_COMPOSE_NEW, 0, R.string.menu_compose_new, R.drawable.ic_menu_compose, null);
            menu.add(MENU_SEARCH, R.string.menu_search, R.drawable.ic_menu_search);
             int count = mAdapter.getCount();
			  if (count > 0) {
                menu.add(0, MENU_DELETE_CONVERSATIONS, 0, R.string.menu_delete_all,
                        R.drawable.ic_menu_delete, null);
            }
			menu.add(0, MENU_PREFERENCES, 0, R.string.menu_preferences, R.drawable.setting, null);
			 
			 
            if (settings.getBooleanSetting(AppSettings.KEY_VMA_TAB_OFFLINE_MODE, false)) {
                menu.add(MENU_RECONNECT, R.string.vma_reconnect, 0);
            }
           
            if (count > 0 && !VZUris.isTabletDevice()) {
                menu.add(0, MENU_SAVE_CONVERSATIONS, 0, R.string.menu_save_conversations,
                        android.R.drawable.ic_menu_save, null);
            }
            

           
            if (!VZUris.isTabletDevice()) {
                menu.add(0, MENU_RESTORE_MESSAGES, 0, R.string.menu_restore_messages,
                        android.R.drawable.ic_menu_save, null);

            }
            // try to accumulate this function
            // .setAlphabeticShortcut(android.app.SearchManager.MENU_KEY);

            // Login/log out
            // sync
            if (settings.isProvisioned()) {
                menu.add(MENU_SYNC, R.string.sync, R.drawable.ico_sync);
                // menu.add(0, MENU_SUBSCRIBE_VMA, 0, R.string.vma_subscribe, 0, null);
            }

            menu.add(MENU_TIPS, R.string.menu_tips, 0);

            // menu.add(MENU_SEND_COMMENTS, R.string.menu_send_comments, 0);

            /* menu.add(MENU_SEND_COMMENTS, R.string.menu_send_comments, 0); */

            menu.add(MENU_ABOUT, R.string.menu_about, 0);

            menu.add(SHOW_TERMS_AND_CONDITION, R.string.vma_terms_conditions, 0);

            menu.add(SHOW_PRIVACY_POLICY, R.string.privacy_and_policy, 0);

            if (Logger.IS_DEBUG_ENABLED) {
                menu.add(REPORT_BUG, R.string.menu_report_a_bug, 0);

                menu.add(MENU_DUMP_DB, "Dump DB", 0);
                // menu.add(MENU_DUMP_LOG, "Dump Log", 0);
                // menu.add(MENU_EMAIL_TRACES, "Email traces.txt", 0);

                menu.add(MENU_EMAIL_MEM, "Email Memory Info", 0);
                menu.add(MENU_DEBUG_PANEL, "Debugpanel", 0);
                menu.add(MENU_GENERATE_SMS, "Generate SMS", 0);
                menu.add(MENU_SHOW_STATUS_DETAILS, "SMS and MMS Count of thread", 0);
                menu.add(MENU_SHOW_POPUP, "Show POPUP", 0);
            }

        }
    }

    private void showWelcomeScreen() {
        if (DeviceConfig.OEM.isSamsungGalaxyCamera) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Dont show welcome dialog for Samsung Galaxy Camera device");
            }
            return;
        }
        mActivity.findViewById(R.id.parentViewConv).postDelayed(new Runnable() {
            public void run() {
                mWScreen = new WelcomeScreen(mActivity, isMultipaneUI, mIsLandscape, p, p1);
                boolean isActivityFinishing = mActivity.isFinishing();
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("isActivityFinising" + isActivityFinishing);
                }
                if (!isActivityFinishing) {
                    mWScreen.show();
                }
                getView().getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
            }
        }, 500);

    }

    public void setSelected(long threadID) {
        if (mAdapter != null) {
            mAdapter.setSelected(threadID);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * This class is used to update the sync status in the Screens header
     */
    public class SyncStatusReceiver extends BroadcastReceiver {

        int errorCode = AppErrorCodes.VMA_PROVISION_UNKNOWN;

        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(SyncManager.EXTRA_STATUS, 0);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this.getClass(), "onReceiver received status: " + status);
            }

            if (status >= AppErrorCodes.VMA_PROVISION_UNKNOWN) {
                errorHandler.showAlertDialog(status);
                mSyncProgressView.setVisibility(View.GONE);
                idleStatus.setVisibility(View.GONE);
            } else {
                String title = null;
                String msg = null;
                switch (status) {

                case SyncManager.SYNC_STATUS_GCM_TOKEN_FAILED:
                    Toast.makeText(mActivity, getString(R.string.vma_gcm_failed_alert), Toast.LENGTH_LONG)
                            .show();
                    break;
                case VMA_PROVISION_GENERATE_PIN:
                case VMA_PROVISION_VALIDATE_PIN:
                case VMA_PROVISION_USER_QUERY:
                case VMA_PROVISION_CREATE_MAILBOX:
                case VMA_PROVISION_SYNC_ASSISTANT_SETTINGS:
                case VMA_PROVISION_SUCCESS:
                case VMA_PROVISION_FAILED:
                case VMA_UNSUBSCRIBE:
                    /**
                     * Notification updates
                     */
                case VMA_SYNC_IDLE_LOGIN:
                case VMA_SYNC_FULLSYNC_LOGIN:
                case VMA_SYNC_IDLE_RENEW:
                case VMA_SYNC_IDLE_RELEASE:
                case VMA_SYNC_FETCH_LOGIN:
                case VMA_SYNC_FETCH_MSG:
                case VMA_SYNC_FETCH_ATTACHEMENT:
                case VMA_SYNC_FETCH_RELEASE:
                case VMA_SYNC_SEND_LOGIN:
                case VMA_SYNC_SEND_SMS:
                case VMA_SYNC_SEND_MMS:
                case VMA_SYNC_SEND_READ:
                case VMA_SYNC_FETCH_CONVERSATION:
                case VMA_SYNC_FETCH_UIDS:
                case VMA_SYNC_FETCHING_ATTACHEMENTS:
                case VMA_SYNC_FETCH_CHANGES:
                case VMA_SYNC_NEW_MESSAGE:
                case VMA_SYNC_CHECKSUM_BUILDER_START:    
                    mSyncProgressView.setVisibility(View.VISIBLE);
                    idleStatus.setVisibility(View.GONE);
                    break;
                    
                case VMA_SYNC_IDLE_LOGOUT:
                case VMA_SYNC_SEND_LOGOUT:                    
                case VMA_SYNC_FETCH_LOGOUT:
//                case VMA_SYNC_FULLSYNC_LOGOUT:
                    mSyncProgressView.setVisibility(View.GONE);
                    break;
                case VMA_SYNC_FETCH_NO_NETWORK:
                case VMA_SYNC_SEND_NO_NETWORK:
                case VMA_SYNC_IDLE_NO_NETWORK:
                    // case VMA_SYNC_IDLING:
                    mSyncProgressView.setVisibility(View.GONE);
                    idleStatus.setVisibility(View.VISIBLE);
                    break;
                case UPDATE_AUTO_REPLY_OR_FORWARD:
                    showOrHideAutoReplyAndFwdHeaders();
                    break;
                case VMA_UNSUBSCRIBE_FAILED:
                    title = getString(R.string.vma_unsubscribe);
                    msg = getString(R.string.vma_unable_toUnSubscribe);
                    showAlert(context, title, msg);
                    break;
                case VMA_AUTO_PROVISION_SUCCESS:
                    title = getString(R.string.vma_welcome_screen_title);
                    msg = getString(R.string.vma_welcome_text);
                    showAlert(context, title, msg);
                    break;
                case VMA_AUTO_PROVISION_FAILED:
                    title = getString(R.string.vma_provision_title);
                    msg = getString(R.string.vma_provision_noNetwork);
                    showAlert(context, title, msg);
                    break;
                default:
                    idleStatus.setVisibility(View.GONE);
                    mSyncProgressView.setVisibility(View.GONE);
                    break;
                }
            }
        }

        /**
         * This Method
         * 
         * @param context
         * @param title
         * @param message
         */

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onConfigurationChanged: " + newConfig);
        }
        MessagingPreferenceActivity.setLocale(getActivity().getBaseContext());
        mIsLandscape = newConfig.orientation == newConfig.ORIENTATION_LANDSCAPE;
        super.onConfigurationChanged(newConfig);

    }

    private void showAlert(Context context, String title, String message) {
        Intent alertDialog = new Intent(context, AppAlertDialog.class);
        alertDialog.putExtra(AppAlertDialog.EXTRA_DIALOG_TYPE, AppAlertDialog.DIALOG_OK_BUTTON);
        alertDialog.putExtra(AppAlertDialog.EXTRA_TITLE, title);
        alertDialog.putExtra(AppAlertDialog.EXTRA_MESSAGE, message);
        context.startActivity(alertDialog);
    }

//    public void startSync() {
//        mAsyncTask = new AsyncTask<Void, String, Void>() {
//            @Override
//            protected Void doInBackground(Void... params) {
//                ProvisionManager provMgr = new ProvisionManager(mActivity);
//                provMgr.startSync();
//                return null;
//            };
//
//            protected void onPostExecute(Void result) {
//                showOrHideAutoReplyAndFwdHeaders();
//            };
//        }.execute();
//
//    }
    
    
    

    public void simulatePopup() {

        new Thread(new Runnable() {

            @Override
            public void run() {
                loadPopUp();
            }
        }, "Popup Reader").start();
    }

    /**
     * This Method
     */
    protected void loadPopUp() {

        SortedSet<PopUpInfo> accumulator = new TreeSet<PopUpInfo>(new PopUpInfoComparator());

        String[] projection = new String[] { Sms._ID, Sms.THREAD_ID, Sms.DATE };
        String selection = Sms.READ + "<= 0 or " + Sms.READ + "=1";
        Cursor c = mActivity.getContentResolver().query(VZUris.getSmsUri(), projection, selection, null,
                Sms.DATE + " desc LIMIT 5");
        HashMap<Long, String> recipIdMapping = new HashMap<Long, String>();
        if (c != null) {
            while (c.moveToNext()) {
                long msgId = c.getLong(0);
                long threadId = c.getLong(1);
                long timeMillis = c.getLong(2);
                ContentValues values = new ContentValues(1);
                values.put(Sms.READ, 1);
                mActivity.getContentResolver().update(ContentUris.withAppendedId(VZUris.getSmsUri(), msgId),
                        values, null, null);

                String recipId = recipIdMapping.get(threadId);
                if (recipId == null) {
                    recipId = getRecipIds(threadId, mActivity);
                    recipIdMapping.put(threadId, recipId);
                }

                accumulator.add(new com.verizon.mms.transaction.MessagingNotification.PopUpInfo(threadId,
                        msgId, true, timeMillis, recipId));
            }
            c.close();
        }
        if (!accumulator.isEmpty()) {
            PopUpNotificationActivity.showPopUp(mActivity, accumulator);
            mActivity.finish();
        } else {
            Toast.makeText(mActivity, "No unread message found", Toast.LENGTH_LONG).show();
        }

    }

    private static String getRecipIds(long threadId, Context context) {
        String recipId = null;
        String RECIP_ID_PROJECTION[] = new String[] { Threads.RECIPIENT_IDS };
        Cursor cursor = SqliteWrapper.query(context, Conversation.sAllThreadsUri, RECIP_ID_PROJECTION,
                Threads._ID + "=" + threadId, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                recipId = cursor.getString(0);
            }
        }

        return recipId;
    }

}
