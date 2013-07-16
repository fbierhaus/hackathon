package com.verizon.mms.ui;

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;
import static com.verizon.mms.transaction.ProgressCallbackEntity.PROGRESS_ABORT;
import static com.verizon.mms.transaction.ProgressCallbackEntity.PROGRESS_COMPLETE;
import static com.verizon.mms.transaction.ProgressCallbackEntity.PROGRESS_START;
import static com.verizon.mms.transaction.ProgressCallbackEntity.PROGRESS_STATUS_ACTION;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_ID;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MSG_TYPE;
import static com.verizon.mms.ui.MessageListAdapter.PROJECTION;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteException;
import android.drm.mobile1.DrmException;
import android.drm.mobile1.DrmRawContent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.DrmStore;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Video;
import android.provider.Settings;
import android.provider.Telephony.Mms;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.Time;
import android.text.method.TextKeyListener;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.cloudmark.spamrep.MessageReport;
import com.cloudmark.spamrep.MessageReport.MsgType;
import com.cloudmark.spamrep.MessageReporter;
import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.internal.telephony.TelephonyIntents;
import com.verizon.internal.telephony.TelephonyProperties;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ContentType;
import com.verizon.mms.DeviceConfig.OEM;
import com.verizon.mms.ExceedMessageSizeException;
import com.verizon.mms.MediaSyncHelper;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.data.RecipContact;
import com.verizon.mms.data.WorkingMessage;
import com.verizon.mms.data.WorkingMessage.MessageStatusListener;
import com.verizon.mms.model.MediaModel;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.model.SmilHelper;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduBody;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPart;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.SendReq;
import com.verizon.mms.receiver.SpamReportResponseReceiver;
import com.verizon.mms.transaction.MessagingNotification;
import com.verizon.mms.ui.ContactSearchTask.ContactSearchListener;
import com.verizon.mms.ui.ConversationListFragment.DeleteThreadClickListener;
import com.verizon.mms.ui.GroupModeChooser.OnGroupModeChangedListener;
import com.verizon.mms.ui.MessageItem.GroupMode;
import com.verizon.mms.ui.MessageListAdapter.OnContentChangedListener;
import com.verizon.mms.ui.MessageListAdapter.SendingMessage;
import com.verizon.mms.ui.MessageListAdapter.TextEntryStateProvider;
import com.verizon.mms.ui.MessageUtils.ResizeImageResultCallback;
import com.verizon.mms.ui.VZMFragmentActivity.ActionMenuBuilder;
import com.verizon.mms.ui.activity.Provisioning;
import com.verizon.mms.ui.adapter.ContactsCursorAdapter;
import com.verizon.mms.ui.adapter.EmojiPagerAdapter;
import com.verizon.mms.ui.adapter.MultiAttachmentAdapter;
import com.verizon.mms.ui.widget.ActionItem;
import com.verizon.mms.ui.widget.ImageViewButton;
import com.verizon.mms.ui.widget.OnOffTabHost;
import com.verizon.mms.ui.widget.QuickAction;
import com.verizon.mms.ui.widget.RecipientEditor;
import com.verizon.mms.ui.widget.RecipientEditor.RecipientsStateListener;
import com.verizon.mms.ui.widget.TextButton;
import com.verizon.mms.util.DraftCache;
import com.verizon.mms.util.EmojiParser;
import com.verizon.mms.util.SendingProgressTokenManager;
import com.verizon.mms.util.SmileyParser;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.Util;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.SyncManager;
import com.verizon.vcard.android.provider.VCardContacts;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;
import com.verizon.vcard.android.syncml.pim.vcard.VCardComposer;
import com.verizon.vcard.android.syncml.pim.vcard.VCardException;

public class ComposeMessageFragment extends Fragment implements View.OnClickListener,
        TextView.OnEditorActionListener, MessageStatusListener, Contact.UpdateListener,
        RecipientsStateListener, Conversation.ThreadChangeListener, MenuSelectedListener {

    // Changing all request codes to four thousand serees 4100.
    public static final int REQUEST_CODE_ATTACH_IMAGE = 4110;
    public static final int REQUEST_CODE_TAKE_PICTURE = 4111;
    public static final int REQUEST_CODE_ATTACH_VIDEO = 4112;
    public static final int REQUEST_CODE_TAKE_VIDEO = 4113;
    public static final int REQUEST_CODE_ATTACH_SOUND = 4114;
    public static final int REQUEST_CODE_RECORD_SOUND = 4115;
    public static final int REQUEST_CODE_CREATE_SLIDESHOW = 4116;
    public static final int REQUEST_CODE_ECM_EXIT_DIALOG = 4117;
    public static final int REQUEST_CODE_ADD_CONTACT = 4118;
    public static final int REQUEST_CODE_PICK_CONTACT = 4119;
    public static final int REQUEST_CODE_ADD_LOCATION = 4120;
    public static final int REQUEST_CODE_FROM_MEDIA_LOCATION = 4121;
    public static final int REQUEST_CODE_SHOW_ATTACH = 4125;
    public static final int REQUEST_CODE_ATTACH_VCARD = 4126;
    public static final int REQUEST_CODE_ATTACH_RINGTONE = 4127;
    public static final int REQUEST_CODE_RECIPIENT_LIST = 4128;
    public static final int REQUEST_CODE_CHECK_CONTACT = 41111;
    public static final int REQUEST_CODE_ADD_CONTACT_FROM_RECIPIENT_EDITOR = 41428;
    public static final int REQUEST_CODE_FONT_CHANGE = 41429;
    public static final int REQUEST_CODE_EDIT_IMAGE_DONE = 41422;
    public static final int REQUEST_CODE_SEARCH_MENU_DONE = 41423;
    private static final String SpamReportTAG = "Cloudmarking_Spam_Report";
    private static final String VZM_EDIT_IMAGE_INTENT = "com.verizon.messaging.vzmsgs.image.ImageEditor";
    private static final boolean TRACE = false;

    // Menu ID
    private static final int MENU_ADD_SUBJECT = 4000;
    private static final int MENU_DELETE_CONVERSATION = 4001;
    private static final int MENU_ADD_ATTACHMENT = 4002;
    private static final int MENU_DISCARD = 4003;
    private static final int MENU_SEND = 4004;
    private static final int MENU_CALL_RECIPIENT = 4005;
    private static final int MENU_CONVERSATION_LIST = 4006;
    private static final int MENU_ADD_RECIPIENT = 4007;
    private static final int MENU_VIEW_RECIPIENT = 4008;
    private static final int MENU_EDIT_RECIPIENT = 4009;
    private static final int MENU_REMOVE_SUBJECT = 4010;
    private static final int MENU_SHOW_STATUS_DETAILS = 4011;
    // Context menu ID
    private static final int MENU_VIEW_CONTACT = 4012;

    private static final int MENU_VIEW_MESSAGE_DETAILS = 4017;
    private static final int MENU_DELETE_MESSAGE = 4018;
    private static final int MENU_SEARCH = 4019;
    private static final int MENU_FORWARD_MESSAGE = 4021;
    private static final int MENU_CALL_BACK = 4022;
    private static final int MENU_SEND_EMAIL = 4023;
    private static final int MENU_COPY_MESSAGE_TEXT = 4024;
    private static final int MENU_COPY_TO_SDCARD = 4025;
    private static final int MENU_INSERT_SMILEY = 4026;
    private static final int MENU_ADD_ADDRESS_TO_CONTACTS = 4027;
    private static final int MENU_COPY_TO_DRM_PROVIDER = 4030;
    private static final int MENU_MESSAGE_RECIPIENT = 4031;
    private static final int MENU_REPLY_TO_SENDER = 4032;
    private static final int MENU_CONTACT_DETAILS = 4033;
    private static final int MENU_REPORT_MESSAGE_SPAM = 4034;
    private static final int MENU_CUSTOMIZE_CONV = 4035;
    // QuickActionMenu
    private static final int MENU_ATTACH_PLACES = 4041;
    private static final int MENU_ATTACH_AUDIO = 4042;
    private static final int MENU_ATTACH_VIDEO = 4043;
    private static final int MENU_ATTACH_PICT = 4044;
    private static final int MENU_PREFERENCES = 4045;
    private static final int MENU_SAVE_MSG_TO_SD_CARD = 4046;
    private static final int MENU_ATTACH_MORE = 4047;
    // private static final int MENU_INSERT_EMOJI = 4048;
    private static final int MENU_ATTACH_EMOJI = 4049;
    private static final int MESSAGE_LIST_QUERY_TOKEN = 9527;
    private static final int DELETE_MESSAGE_TOKEN = 9700;
    private static final int DELETE_CONVERSATION_TOKEN = 9701;
    private static final int DELETE_SPAM_REPORT_TOKEN = 9702;
    private static final int CHARS_REMAINING_BEFORE_COUNTER_SHOWN = 10;

    private static final int SMS_TO_MMS_MAX_CHAR_LENGTH = 1000;
    private static final long NO_DATE_FOR_DIALOG = -1L;
    public static final int MULTI_ATTACHMENT_MAX_LIMIT = 7;

    private static final String EXIT_ECM_RESULT = "exit_ecm_result";

    private ContentResolver mContentResolver;
    private BackgroundQueryHandler mBackgroundQueryHandler;
    private Conversation mConversation; // Conversation we are working in
    private boolean mExitOnSent; // Should we finish() after sending a message?
    private ComposeMessageListener mComposeMsgListener;
    private TextView mMmsAttachmentCount;
    private LinearLayout mMmsCountLayout;
    private GroupModeChooser mGroupChooser; // contains group message mode
                                            // buttons
    private boolean mGroupMms; // true if group messages are always to be sent
                               // via MMS
    private boolean mResetAdapterGroupMode;// reset the groupmode set in adapter
    private boolean mGroupModeChanged; // true if user has set the group mode
    private boolean mQueryCompleted; // true after the first query is complete
    private TextView mChannel; // channel to be used for message
    private TextView mChannel2; // channel to be used for message
    private View mBottomPanel; // View containing the text editor, send button,
    // etc.

    private View mSearchNavigatePanel;
    private ImageView mUpArrow;
    private ImageView mDownArrow;
    private TextView searchResultTxt;
    private TextButton mSearchDone;
    private int currentPos = 0;
    private int msgIdsCcount;
    private String searchString;
    private List<Long> msgIdsList;
    private List<Integer> rowIdsList;

    private EmojiEditText mTextEditor; // Text editor to type your message into
    private TextView mTextCounter; // Shows the number of characters used in
    // text editor
    private TextButton mSendButton; // Press to detonate
    // private View mMessageProgress; // progress indicator for message being
    // sent
    private ImageViewButton mAttachButton;
    private ImageViewButton mGalleryButton;
    private View fragmentView;
    private ViewPager mViewPager;
    private OnOffTabHost mTabHost;
    private LinearLayout emojiLayout;
    private boolean isEmojiPanelVisible;
    private EmojiEditText mSubjectTextEditor; // Text editor for MMS subject
    private MessageListView mMsgListView; // ListView for messages in this
    // conversation
    public MessageListAdapter mMsgListAdapter; // and its corresponding
    // ListAdapter
    private View mRecipSubjectPanel; // View containing the recipient and
    // subject editors
    private RecipientEditor mRecipientEditor; // UI control for editing
    // recipients
    private View mRecipientLayout; // Contains the recipients editor
    private ListView mRecipListView = null;
    private ContactsCursorAdapter mRecipAdapter = null;
    private Cursor mRecipCursor = null;
    private Object mCursorLock = new Object();
    private AsyncTask<String, Void, Cursor> mSearchContTask = null;
    private View mMsgHeaderPanel;
    private View mShrinkMsgHeaderPanel;
    private TextView mDisplayNameView;
    private TextView mDisplayNameView2;
    private ContactImage mAvatarView;
    private boolean mToastForDraftSave; // Whether to notify the user that a
    // draft is being saved

    // true if the user has sent a message while in this
    // activity. On a new compose message case, when the first
    // message is sent is a MMS w/ attachment, the list blanks
    // for a second before showing the sent message. But we'd
    // think the message list is empty, thus show the recipients
    // editor thinking it's a draft message. This flag should
    // help clarify the situation.
    private boolean mSentMessage;
    private boolean mFirstMessage; // true if this is the first message sent on
    // a new thread
    private WorkingMessage mWorkingMessage; // The message currently being
    // composed.
    private boolean mWaitingForSubActivity;
    private int mLastRecipientCount; // number of recipients as of last change
    private boolean mSendingMessage; // Indicates the current message is
    // sending, and shouldn't send again.
    private Intent mAddContactIntent; // Intent used to add a new contact
    private boolean mIsKeyboardOpen; // Whether a keyboard is available
    private boolean mIsHardKeyboardOpen; // Whether the hardware keyboard is
                                         // visible
    private boolean mIsLandscape; // Whether we're in landscape mode
    private Activity mActivity;
    private boolean mActive;
    private boolean mPaused;
    private boolean mPendingMarkAsread;
    private long mHighLightedMsgId = 0;
    private boolean mFromNotification; // true when we were launched from a
    // notification
    private String mSelectedAddress;

    /**
     * This variable will be set while calling native Intent from RecipientEditor Menu and Add Contact. If
     * true, it will load this activity without any WorthSaving item in onRestart
     */
    private static boolean mIsNativeIntentCalled = false;

    // path where the captured picture is saved
    private String mCapturedImgFileName = null;
    private boolean showKeyboard = false;
    private static String langcode;
    private static ContactList sEmptyContactList;

    private AlertDialog mEmojiDialog;
    private View mEmojiView;
    /*
     * Added Delete Conversation progress dialog in order to show conversation deletion progress
     */
    private ProgressDialog convDeletionDialog;

    private LinearLayout mAttachlayout;
    private int mAttachLayoutMaxHeight;
    private ListView mAttachmentListView;
    private View groupDialog;
    private MultiAttachmentAdapter mAttachmentAdapter;
    private MessageItem mSelectedMsgItem;
    private long queryStart;
    private RelativeLayout attendeesHintListLayout;
    public boolean isMultipaneUI;
    private long lastDeletedThreadID = -5l;
    private Handler mainHandler;
    private SpamReportResponseReceiver responseReceiver;
    private int rootHeight;
    private boolean showingHeader;
    private boolean showingFullHeader;
    private int softKeyboardLimit;
    private long lastTextEntryTime;

    private View rootView;

    private static final int SOFT_KEYBOARD_LIMIT = 96;
    private static final long ANIM_HEADER_DURATION = 1000;

    static final boolean GROUP_MMS_DEFAULT = true;
    // to serese 4300
    static final int MSG_EDIT_SLIDESHOW = 4301;
    static final int MSG_SEND_SLIDESHOW = 4302;
    static final int MSG_PLAY_SLIDESHOW = 4303;
    static final int MSG_REPLACE_IMAGE = 4304;
    static final int MSG_REPLACE_VIDEO = 4305;
    static final int MSG_REPLACE_AUDIO = 4306;
    static final int MSG_PLAY_VIDEO = 4307;
    static final int MSG_PLAY_AUDIO = 4308;
    static final int MSG_VIEW_IMAGE = 4309;
    static final int MSG_REMOVE_ATTACHMENT = 4310;
    static final int MSG_VIEW_VCARD = 4311;
    static final int MSG_REPLACE_VCARD = 4312;
    public static final int MSG_REMOVE_SLIDE = 4313;
    public static final int MSG_PLAY_SLIDE = 4314;

    public final static int MODE_NONE = -1;
    public final static int MODE_AUDIO = 0;
    public final static int MODE_VIDEO = 1;
    public final static int MODE_ALL = 2;
    public final static int MODE_IMAGE = 3;
    // to serese 4400
    public final static int ADD_IMAGE = 4400;
    public final static int TAKE_PICTURE = 4401;
    public final static int ADD_VIDEO = 4402;
    public final static int RECORD_VIDEO = 4403;
    public final static int ADD_SOUND = 4404;
    public final static int RECORD_SOUND = 4405;
    public final static int ADD_SLIDESHOW = 4406;
    public final static int NEW_LOCATION = 4407;
    public final static int FROM_RECENTLYUSED = 4408;
    public final static int FROM_FAVORITE = 4409;
    public final static int FROM_MEDIA = 4410;
    public final static int FROM_ADDRESS = 4411;
    public final static int FROM_PLACES = 4412;
    public final static int LOCATION = 4413;
    public final static int ADD_VCARD = 4414;
    public final static int ADD_RINGTONE = 4415;
    public static final long INVALID = -1L;
    public static final String INTENT_FROM_NOTIFICATION = "notification";

    public EmojiParser mParser = EmojiParser.getInstance();

    // mVideoUri will look like this: content://media/external/video/media
    private static final String mVideoUri = Video.Media.getContentUri("external").toString();
    // mImageUri will look like this: content://media/external/images/media
    private static final String mImageUri = Images.Media.getContentUri("external").toString();
    // mAudioUri will look like this: content://media/external/audio/media/
    private static final String mAudioUir = Audio.Media.getContentUri("external").toString();

    private static final String mVcardUri = ContactsContract.Contacts.CONTENT_VCARD_URI.toString();
    private static final String motoralVcardUri = "content://com.android.contacts/contacts/as_partial_field_vcard";
    private static final String mLookUpUri = ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString();
    private static final String PREFERENCE_FILE_PATH = "filePath";

    private AppSettings settings;
    
    private static void log(String logMsg) {
        Thread current = Thread.currentThread();
        long tid = current.getId();
        StackTraceElement[] stack = current.getStackTrace();
        String methodName = stack[3].getMethodName();
        // Prepend current thread ID and name of calling method to the message.
        logMsg = "[" + tid + "] [" + methodName + "] " + logMsg;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(ComposeMessageFragment.class, logMsg);
        }
    }

    // listener interface
    public interface ComposeMessageListener {
        // tells the listener that the fragment should be removed
        public void finished();

        public Intent getIntentFromParent(Context mActivity, long threadId);

        public void launchComposeView(Intent mIntent);

        // after sending if delete from conversation list view.
        public void onDeleteGoToNext(long threadId);

        // Update current open threadID, need to be updated in case message
        // changes its threadID ie on
        // sending.
        public void updateCurrentThreadID(long threadId, String calledFrom);

        public void setGalleryLoaded(boolean enable);

    }

    private void editSlideshow() {
        Uri dataUri = mWorkingMessage.saveAsMms(false);
        Intent intent = new Intent(mActivity, SlideshowEditActivity.class);
        intent.setData(dataUri);
        mActivity.startActivityForResult(intent, REQUEST_CODE_CREATE_SLIDESHOW);
    }

    private final Handler mAttachmentEditorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_EDIT_SLIDESHOW: {
                editSlideshow();
                break;
            }
            case MSG_SEND_SLIDESHOW: {
                if (isPreparedForSending()) {
                    ComposeMessageFragment.this.confirmSendMessageIfNeeded();
                }
                break;
            }
            case MSG_VIEW_IMAGE:
            case MSG_PLAY_VIDEO:
            case MSG_PLAY_AUDIO:
            case MSG_PLAY_SLIDESHOW:
            case MSG_VIEW_VCARD:
                MessageUtils.viewMmsMessageAttachment(mActivity, mWorkingMessage);
                break;

            case MSG_REPLACE_IMAGE:
            case MSG_REPLACE_VIDEO:
            case MSG_REPLACE_AUDIO:
            case MSG_REPLACE_VCARD:
                showAddAttachmentDialog(true, MODE_ALL);
                break;

            case MSG_REMOVE_ATTACHMENT:
                mWorkingMessage.setAttachment(WorkingMessage.TEXT, null, false);
                break;

            case MSG_REMOVE_SLIDE: {
                final WorkingMessage message = mWorkingMessage;
                int position = msg.arg1;
                message.removeAttachment(position);

                // Removes the item from the AttachmentListView
                final MultiAttachmentAdapter adapter = mAttachmentAdapter;
                adapter.setItems(message.getSlideshow());
                mAttachmentListView.setAdapter(adapter);
                mMmsAttachmentCount.setText("(" + mAttachmentAdapter.getCount() + ")");
                if (adapter.getCount() == 0) {
                    adapter.clearItems();
                    mAttachmentListView.setVisibility(View.GONE);
                    mMmsCountLayout.setVisibility(View.GONE);
                    mAttachlayout.setVisibility(View.GONE);
                    if (!message.requiresMms()
                            && (mSubjectTextEditor == null || mSubjectTextEditor.getVisibility() != View.VISIBLE))
                        toastConvertInfo(false);
                }
                adjustAttachmentListViewHeight();
                updateSendButtonState();
                break;
            }

            case MSG_PLAY_SLIDE: {
                MediaModel model = null;
                for (MediaModel media : mAttachmentAdapter.getItem(msg.arg1).getMedia()) {
                    if (!media.isText()) {
                        model = media;
                        break;
                    }
                }
                // Hide keyboard if shown in order to avoid the keyboard overlap
                // on model preview
                Configuration configuration = getResources().getConfiguration();
                boolean isKeyBoardOpen = configuration.keyboardHidden == KEYBOARDHIDDEN_NO;
                if (isKeyBoardOpen) {
                    Util.forceHideKeyboard(mActivity, mTextEditor);
                }
                MessageUtils.viewMediaModel(mActivity, model);
                break;
            }

            default:
                break;
            }
        }
    };

    private final Handler mMessageListItemHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MessageListAdapter.MSG_LIST_ERROR) {
                final MessageItem msgItem = (MessageItem) msg.obj;
                if (msgItem != null) {
                    mSelectedMsgItem = msgItem;
                    showDeliveryReport(msgItem);
                }
            } else if (msg.what == MessageListAdapter.MSG_LIST_MENU) {
                final MessageItem msgItem = (MessageItem) msg.obj;
                if (msgItem != null) {
                    setMsgMenu(msgItem);
                }
            }

        }
    };

    private final OnKeyListener mSubjectKeyListener = new OnKeyListener() {
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            // When the subject editor is empty, press
            // "DEL" to hide the input
            // field.
            if ((keyCode == KeyEvent.KEYCODE_DEL) && (mSubjectTextEditor.length() == 0)) {
                showSubjectEditor(false);
                mWorkingMessage.setSubject(null, true);
                return true;
            }
            return false;
        }
    };

    private boolean isCursorValid() {
        // Check whether the cursor is valid or not.
        Cursor cursor = mMsgListAdapter.getCursor();
        if (cursor == null || cursor.isClosed() || cursor.isBeforeFirst() || cursor.isAfterLast()) {
            Logger.error(getClass(), "Bad cursor.", new RuntimeException());
            return false;
        }
        return true;
    }

    private void resetCounter() {
        mTextCounter.setText("");
        mTextCounter.setVisibility(View.GONE);
    }

    private void updateCounter(CharSequence text, int start, int before, int count) {
        WorkingMessage workingMessage = mWorkingMessage;
        if (workingMessage.requiresMms()) {
            // If we're not removing text (i.e. no chance of converting back to
            // SMS
            // because of this change) and we're in MMS mode, just bail out
            // since we
            // then won't have to calculate the length unnecessarily.
            final boolean textRemoved = (before > count);
            if (!textRemoved) {
                return;
            }
        }

        int[] params = SmsMessage.calculateLength(MessagingPreferenceActivity.getSignature(mActivity, text),
                false);
        /*
         * SmsMessage.calculateLength returns an int[4] with: int[0] being the number of SMS's required,
         * int[1] the number of code units used, int[2] is the number of code units remaining until the next
         * message. int[3] is the encoding type that should be used for the message.
         */
        int msgCount = params[0];
        int remainingInCurrentMessage = params[2];

        // Show the counter only if:
        // - We are not in MMS mode
        // - We are going to send more than one message OR we are getting close
        boolean showCounter = false;
        if (!workingMessage.requiresMms()
                && (msgCount > 1 || remainingInCurrentMessage <= CHARS_REMAINING_BEFORE_COUNTER_SHOWN)) {
            showCounter = true;
        }

        if (showCounter) {
            // Update the remaining characters and number of messages required.
            String counterText = msgCount > 1 ? remainingInCurrentMessage + " / " + msgCount : String
                    .valueOf(remainingInCurrentMessage);
            mTextCounter.setText(counterText);
            mTextCounter.setVisibility(View.VISIBLE);
        } else {
            mTextCounter.setVisibility(View.GONE);
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        // requestCode >= 0 means the activity in question is a sub-activity.
        if (requestCode >= 0) {
            mWaitingForSubActivity = true;
        }

        super.startActivityForResult(intent, requestCode);
    }

    private void toastConvertInfo(boolean toMms) {
        final int resId = toMms ? R.string.converting_to_picture_message
                : R.string.converting_to_text_message;
        Toast.makeText(mActivity, resId, Toast.LENGTH_SHORT).show();
    }

    private void deleteMessageListener(int token, Uri deleteUri, int msgCount) {
        // [JEGA] TODO Imthiaz/Ishaque , If we delete the mms using this uri
        // content://mms/#. it will
        // not update the time stamp on the conversation table. so please use
        // the below uri to delete the
        // conversation with message type content://mms-sms/conversations or
        // content://mms-sms/conversations/#

        switch (token) {
        case DELETE_MESSAGE_TOKEN:
            if (msgCount == 1) {
                convDeletionDialog.setMessage(mActivity.getString(R.string.deleting));
                convDeletionDialog.setCancelable(false);
                convDeletionDialog.show();
            }
            break;
        case DELETE_SPAM_REPORT_TOKEN:
            break;
        }
        mBackgroundQueryHandler.startDelete(token, deleteUri, deleteUri, null, null);
        /* Wifi Sync HookUps */
        if (deleteUri.getAuthority().equalsIgnoreCase(VZUris.getMmsUri().getAuthority())) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Deleting MMS :" + deleteUri);
            }
            // WifiSyncHelper.syncMMSDelete(mActivity,
            // ContentUris.parseId(mDeleteUri));

            // media cache
            MediaSyncHelper.onMMSDelete(mActivity, ContentUris.parseId(deleteUri));
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Deleting SMS :" + deleteUri);
            }
            // WifiSyncHelper.syncSMSDelete(mActivity,
            // ContentUris.parseId(mDeleteUri));

            // media cache
            MediaSyncHelper.onSMSDelete(mActivity, ContentUris.parseId(deleteUri));
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "VMA-Hook:Delete message uri=" + deleteUri);
        }
        Intent intent = new Intent(SyncManager.ACTION_MSG_DELETED);
        intent.putExtra(SyncManager.EXTRA_URI, deleteUri);
        mActivity.startService(intent);

        // notify adapter
        if (mMsgListAdapter != null) {
            mMsgListAdapter.setCursorChanging(true);
        }
    }

    public class DiscardDraftListener implements View.OnClickListener {
        protected Dialog dialog;

        public void setDialog(Dialog d) {
            dialog = d;
        }

        public void onClick(View v) {
            dialog.dismiss();
            mWorkingMessage.discard();

            if (mComposeMsgListener != null) {
                mComposeMsgListener.finished();
                if (isMultipaneUI) {
                    // if message discarded reset the message.
                    resetMessage();
                    hideMessageHeader();
                    showRecipientEditor();
                    mComposeMsgListener.onDeleteGoToNext(0);
                }
            }
        }
    }

    private void sendListener() {
        sendMessage(true);

    }

    private void cancelDialog() {
        if (isRecipientsEditorVisible()) {
            mRecipientEditor.requestFocus();
        }
    }

    private void confirmSendMessageIfNeeded() {
        if (!isRecipientsEditorVisible()) {
            String number = mConversation.getRecipients().get(0).getNumber();
            boolean flag = MessageUtils.isValidMmsAddress(number);
            if (flag) {
                sendMessage(true);
            } else {
                final AppAlignedDialog d = new AppAlignedDialog(mActivity, R.drawable.dialog_alert,
                        R.string.cannot_send_message, R.string.cannot_send_message_reason);
                Button saveButton = (Button) d.findViewById(R.id.positive_button);
                saveButton.setText(R.string.yes);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelDialog();
                        d.dismiss();
                    }
                });
                d.show();
            }
            return;
        }

        boolean isMms = mWorkingMessage.requiresMms();
        int status = mRecipientEditor.flush(isMms);
        if (status == RecipientEditor.CONTACT_ADDED) {
            updateRecipientsState();
        } else if (status == RecipientEditor.BLANK_CONTACT_ONLY) { // return if
            // we are
            // not able
            // to add
            // the
            // contact
            // return;
            return;
        }

        if (mRecipientEditor.hasInvalidRecipient(isMms)) {
            if (mRecipientEditor.hasValidRecipient(isMms)) {
                final AppAlignedDialog build = new AppAlignedDialog(mActivity, R.drawable.dialog_alert,
                        R.string.accept, R.string.invalid_recipient_message);
                build.setCancelable(true);
                Button saveButton = (Button) build.findViewById(R.id.positive_button);
                saveButton.setText(R.string.try_to_send);
                saveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendListener();
                        build.dismiss();
                    }
                });
                Button cancelButton = (Button) build.findViewById(R.id.negative_button);
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setText(R.string.no);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelDialog();
                        build.dismiss();
                    }
                });
                build.show();
            } else {
                final AppAlignedDialog build = new AppAlignedDialog(mActivity, R.drawable.dialog_alert,
                        R.string.cannot_send_message, R.string.cannot_send_message_reason);
                build.setCancelable(true);
                Button cancelButton = (Button) build.findViewById(R.id.positive_button);
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setText(R.string.yes);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelDialog();
                        build.dismiss();
                    }
                });
                build.show();
            }
        } else {
            sendMessage(true);
        }
    }

    private void checkRecipientCount() {
        final int recipientCount = recipientCount();
        final int lastRecipientCount = mLastRecipientCount;
        final int recipientLimit = MmsConfig.getRecipientLimit();

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".checkRecipientCount: count = " + recipientCount + ", last = "
                    + lastRecipientCount);
        }

        if (recipientLimit != Integer.MAX_VALUE) {
            boolean tooMany = recipientCount > recipientLimit;

            if (recipientCount != lastRecipientCount) {
                // Don't warn the user on every character they type when they're
                // over the limit,
                // only when the actual # of recipients changes.
                if (tooMany) {
                    String tooManyMsg = getString(R.string.too_many_recipients, recipientCount,
                            recipientLimit);
                    Toast.makeText(mActivity, tooManyMsg, Toast.LENGTH_LONG).show();
                }
            }
        }

        // check if the message state needs to be changed from/to single/group
        if ((recipientCount > 1 && lastRecipientCount <= 1)
                || (recipientCount <= 1 && lastRecipientCount > 1)) {
            // update the group delivery mode and notify user if mode changed
            setGroupMode(true, true);
        }

        mLastRecipientCount = recipientCount;
    }

    private void initGroupMode(boolean fromThread) {
        // check if the caller has specified it
        final Intent intent = mActivity.getIntent();
        if (fromThread && mMsgListAdapter != null) {
            final GroupMode mode = mMsgListAdapter.getGroupMode();
            if (mode != null) {
                mGroupMms = (mode == GroupMode.GROUP);
            } else {
                // Initially on first query Mass Text case is handled (mGroupMms will be false for Mass Text)
            }
        } else if (intent != null && intent.hasExtra(ComposeMessageActivity.GROUP_MODE)) {
            mGroupMms = intent.getBooleanExtra(ComposeMessageActivity.GROUP_MODE, GROUP_MMS_DEFAULT);
        } else {
            // reset to the default
            mGroupMms = GROUP_MMS_DEFAULT;
        }
        setGroupMode(false, true);
    }

    private void setGroupMode(boolean showToast, boolean setChooser) {
        // update message state based on mode value and number of recipients
        final boolean groupMms = mGroupMms;
        final int recipientCount = mLastRecipientCount = recipientCount();
        final boolean groupMessage = recipientCount > 1;
        final boolean changed = mWorkingMessage.updateState(WorkingMessage.GROUP_MMS, groupMms
                && groupMessage, false);

        if (mActive) {
            if (setChooser) {
                mGroupChooser.setGroupMode(groupMms);
            }

            // show the group choice buttons if this is a new group message
            final boolean show = groupMessage && isRecipientsEditorVisible();
            mGroupChooser.setVisibility(show ? View.VISIBLE : View.GONE);
            if (mGroupChooser.findViewById(R.id.group_dialog).getVisibility() == View.VISIBLE) {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mGroupChooser.getLayoutParams();
                lp.height = LayoutParams.FILL_PARENT;
                mGroupChooser.setLayoutParams(lp);
            } else {
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mGroupChooser.getLayoutParams();
                lp.height = LayoutParams.WRAP_CONTENT;
                mGroupChooser.setLayoutParams(lp);
            }

            updateChannel();
            updateComposeHint();

            if (changed && showToast) {
                toastConvertInfo(groupMms && groupMessage);
            }
        }
        if (mGroupModeChanged) {
            mResetAdapterGroupMode = true;
        }
        // rested the flag as all changes are done.
        mGroupModeChanged = false;
    }

    public boolean getGroupMms() {
        return mGroupMms;
    }

    // this listens for changes to the group delivery mode from GroupModeChooser
    private OnGroupModeChangedListener groupModeListener = new OnGroupModeChangedListener() {
        public void groupModeChanged(boolean groupMms) {
            mGroupModeChanged = true;
            mGroupMms = groupMms;
            setGroupMode(false, false);
        }
    };

    private final void addCallAndContactMenuItems(QuickAction menu, MessageItem msgItem) {
        ActionItem actionItem;

        // Add all possible links in the address & message
        StringBuilder textToSpannify = new StringBuilder();
        if (msgItem.mBoxId == Mms.MESSAGE_BOX_INBOX) {
            textToSpannify.append(msgItem.mAddress + ": ");
        }
        textToSpannify.append(msgItem.mBody);

        SpannableString msg = new SpannableString(textToSpannify.toString());
        Linkify.addLinks(msg, Linkify.ALL);
        ArrayList<String> uris = MessageUtils.extractUris(msg.getSpans(0, msg.length(), URLSpan.class));

        while (uris.size() > 0) {
            String uriString = uris.remove(0);
            // Remove any dupes so they don't get added to the menu multiple
            // times
            while (uris.contains(uriString)) {
                uris.remove(uriString);
            }

            int sep = uriString.indexOf(":");
            String prefix = null;
            if (sep >= 0) {
                prefix = uriString.substring(0, sep);
                uriString = uriString.substring(sep + 1);
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < uriString.length(); i++) {
                char c = uriString.charAt(i);
                if (Character.isDigit(c)) {
                    stringBuilder.append(c);
                }
            }
            boolean addToContacts = false;
            if ("mailto".equalsIgnoreCase(prefix)) {
                String sendEmailString = getString(R.string.menu_send_email).replace("%s", uriString);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + uriString));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                actionItem = new ActionItem(MENU_SEND_EMAIL, sendEmailString, 0);
                actionItem.setTag(intent);
                menu.addActionItem(actionItem);
                addToContacts = !haveEmailContact(uriString);
            } else if ("tel".equalsIgnoreCase(prefix)) {
                String callBackString = null;
                String msgBackString;

                addToContacts = !isNumberInContacts(uriString);
                // call menu
                if (PhoneNumberUtils.compare(msgItem.mAddress, uriString) && (msgItem.mContact != null)) {
                    msgItem.mContact = Contact.get(msgItem.mAddress, false).getName();

                    if (stringBuilder.toString().length() >= 10) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("builder.append(c)" + stringBuilder.toString());
                        }
                        callBackString = getString(R.string.menu_call_back).replace("%s", msgItem.mContact);
                        msgBackString = getString(R.string.menu_msg) + " " + msgItem.mContact;
                    }

                } else {
                    if (stringBuilder.toString().length() >= 10) {
                        callBackString = getString(R.string.menu_call_back).replace("%s", uriString);
                        msgBackString = getString(R.string.menu_msg) + " " + uriString;
                    }
                }

                if (!MmsConfig.isTabletDevice() && callBackString != null) {
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + uriString));
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    actionItem = new ActionItem(MENU_CALL_BACK, callBackString, 0);
                    actionItem.setTag(intent);
                    menu.addActionItem(actionItem);
                }

                // Message<Name> and Message<Number>
                /*
                 * actionItem = new ActionItem(MENU_MESSAGE_RECIPIENT, msgBackString, 0);
                 * menu.addActionItem(actionItem);
                 */
            }
            if (addToContacts) {

                if (stringBuilder.toString().length() >= 10) {
                    Intent intent = ConversationListFragment.createAddContactIntent(uriString);
                    String addContactString = getString(R.string.menu_add_address_to_contacts).replace("%s",
                            uriString);
                    actionItem = new ActionItem(MENU_ADD_ADDRESS_TO_CONTACTS, addContactString, 0);
                    actionItem.setTag(intent);
                    menu.addActionItem(actionItem);
                }
            }
        }

        // Reply to Sender for Group Message
        // Replay to Sender Menu
        if (getRecipients().size() > 1) {
            if (msgItem.mContact != null) {
                String msgBackString = getString(R.string.reply_to_sender);
                actionItem = new ActionItem(MENU_REPLY_TO_SENDER, msgBackString, 0);
                menu.addActionItem(actionItem);
            }
        }

    }

    private boolean haveEmailContact(String emailAddress) {
        Cursor cursor = SqliteWrapper.query(mActivity, mActivity.getContentResolver(),
                Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, Uri.encode(emailAddress)),
                new String[] { Contacts.DISPLAY_NAME }, null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    if (!TextUtils.isEmpty(name)) {
                        return true;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }

    private boolean isNumberInContacts(String phoneNumber) {

        return Contact.get(phoneNumber, false).existsInDatabase();
    }

    private void setMsgMenu(final MessageItem msgItem) {
        ActionItem actionItem;
        mSelectedMsgItem = msgItem;
        if (msgItem != null) {

            QuickAction mMsgmenu = new QuickAction(mActivity);
            mMsgmenu.setTitle(R.string.message_options);

            if (msgItem.mBody != null) {
                if (msgItem.mBody.length() > 0) {
                    actionItem = new ActionItem(MENU_COPY_MESSAGE_TEXT, R.string.copy_message_text, 0);
                    mMsgmenu.addActionItem(actionItem);
                }
            }

            if (msgItem.isDownloaded()) {
                actionItem = new ActionItem(MENU_FORWARD_MESSAGE, R.string.menu_forward, 0);
                mMsgmenu.addActionItem(actionItem);
                // Save Message text Menu
                /*
                 * if (!VZUris.isTabletDevice()) { actionItem = new ActionItem(MENU_SAVE_MSG_TO_SD_CARD,
                 * R.string.save_message_text, 0); mMsgmenu.addActionItem(actionItem); }
                 */
            }

            actionItem = new ActionItem(MENU_VIEW_MESSAGE_DETAILS, R.string.view_message_details, 0);
            mMsgmenu.addActionItem(actionItem);

            actionItem = new ActionItem(MENU_DELETE_MESSAGE, R.string.delete_message, 0);
            mMsgmenu.addActionItem(actionItem);

            boolean isIncoming = msgItem.mContact != null;
            if (isIncoming && !MmsConfig.isTabletDevice()) {
                actionItem = new ActionItem(MENU_REPORT_MESSAGE_SPAM, R.string.report_spam, 0);
                mMsgmenu.addActionItem(actionItem);
            }
            // contact details menu
            /*
             * if (msgItem.mContact != null) { if (isNumberInContacts(msgItem.mAddress)) { actionItem = new
             * ActionItem(MENU_CONTACT_DETAILS, R.string.menu_view_contact, 0);
             * mMsgmenu.addActionItem(actionItem); } }
             */

            if (msgItem.isMms()) {
                switch (msgItem.mAttachmentType) {
                case WorkingMessage.TEXT:
                    break;
                case WorkingMessage.VIDEO:
                case WorkingMessage.IMAGE:
                    if (haveSomethingToCopyToSDCard(msgItem.mMsgId)) {
                        actionItem = new ActionItem(MENU_COPY_TO_SDCARD, R.string.copy_to_sdcard, 0);
                        mMsgmenu.addActionItem(actionItem);
                    }
                    break;
                case WorkingMessage.SLIDESHOW:
                default:
                    if (haveSomethingToCopyToSDCard(msgItem.mMsgId)) {
                        actionItem = new ActionItem(MENU_COPY_TO_SDCARD, R.string.copy_to_sdcard, 0);
                        mMsgmenu.addActionItem(actionItem);
                    }
                    if (haveSomethingToCopyToDrmProvider(msgItem.mMsgId)) {
                        actionItem = new ActionItem(MENU_COPY_TO_DRM_PROVIDER,
                                getDrmMimeMenuStringRsrc(msgItem.mMsgId), 0);
                        mMsgmenu.addActionItem(actionItem);
                    }
                    break;
                }
            }
            // Forward is not available for undownloaded messages
            mMsgmenu.setOnActionItemClickListener(mMsgClickListener);
            mMsgmenu.show(null, getView(), mIsKeyboardOpen);
            addCallAndContactMenuItems(mMsgmenu, msgItem);
        }
    }

    private void copyToClipboard(String str) throws NullPointerException {
        ClipboardManager clip = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (OEM.deviceModel.equalsIgnoreCase("GT-P7500")) {
            str = str.trim();
        }
        clip.setText(str);
    }

    private void forwardMessage(MessageItem msgItem, boolean addRecipients, int action, String recipients) {
        Intent intent = mComposeMsgListener.getIntentFromParent(mActivity, 0);
        intent.putExtra(ComposeMessageActivity.EXIT_ON_SENT, false);
        intent.putExtra(ComposeMessageActivity.FORWARD_MESSAGE, true);
        String updateBodyOrSubject = "";
        if (action == 1) {
            updateBodyOrSubject = getString(R.string.forward_prefix);

        }
        if (msgItem.mType.equals("sms")) {
            updateBodyOrSubject += msgItem.mBody;
            intent.putExtra("sms_body", updateBodyOrSubject);
        } else {
            try {
                SendReq sendReq = new SendReq();

                if (msgItem.mSubject != null) {
                    updateBodyOrSubject += msgItem.mSubject;
                }
                sendReq.setSubject(new EncodedStringValue(updateBodyOrSubject));
                sendReq.setBody(msgItem.mSlideshow.makeCopy(mActivity));

                Uri uri = null;
                try {
                    PduPersister persister = PduPersister.getPduPersister(mActivity);
                    // Copy the parts of the message here.
                    uri = persister.persist(sendReq, VZUris.getMmsDraftsUri());
                } catch (MmsException e) {
                    Logger.error(getClass(), "Failed to copy message: " + msgItem.mMessageUri, e);
                    Toast.makeText(ComposeMessageFragment.this.mActivity, R.string.cannot_save_message,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                intent.putExtra("msg_uri", uri);
                intent.putExtra("subject", updateBodyOrSubject);
            } catch (ExceedMessageSizeException e) {
                String title = getString(R.string.exceed_message_size_limitation);
                String msg = getString(R.string.forward_exceedsize);
                MessageUtils.showErrorDialog(mActivity, title, msg);
                return;
            }
        }

        if (addRecipients) {
            if (recipients == null) {
                // use the conversation's recipients
                recipients = ContactList.getDelimSeperatedNumbers(getRecipients(), ";");
            }
            intent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, recipients);
        }

        // ForwardMessageActivity is simply an alias in the manifest for
        // ComposeMessageActivity.
        // We have to make an alias because ComposeMessageActivity launch flags
        // specify
        // singleTop. When we forward a message, we want to start a separate
        // ComposeMessageActivity.
        // The only way to do that is to override the singleTop flag, which is
        // impossible to do
        // in code. By creating an alias to the activity, without the singleTop
        // flag, we can
        // launch a separate ComposeMessageActivity to edit the forward message.
        intent.setClassName(ComposeMessageFragment.this.mActivity,
                "com.verizon.mms.ui.ForwardMessageActivity");

        mComposeMsgListener.finished();
        mComposeMsgListener.launchComposeView(intent);

        mTextCounter.setVisibility(View.VISIBLE);
    }

    /**
     * Context menu handlers for the message list view.
     */
    QuickAction.OnActionItemClickListener mMsgClickListener = new QuickAction.OnActionItemClickListener() {
        @Override
        public void onItemClick(QuickAction source, int pos, int actionId) {
            if (!isCursorValid()) {
                return;
            }
            final MessageItem msgItem = mSelectedMsgItem;
            if (msgItem == null) {
                return;
            }
            final long msgId = msgItem.mMsgId;
            ActionItem item = source.getActionItem(pos);

            switch (actionId) {
            case MENU_COPY_MESSAGE_TEXT:
                try {
                    copyToClipboard(msgItem.mBody);
                } catch (NullPointerException e) {
                    Toast.makeText(mActivity, getString(R.string.copy_txt_toast), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }

                break;

            case MENU_FORWARD_MESSAGE:
                forwardMessage(msgItem, false, 1, null);
                break;

            case MENU_VIEW_MESSAGE_DETAILS: {
                showDeliveryReport(msgItem);
                break;
            }

            case MENU_REPORT_MESSAGE_SPAM: {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.info(SpamReportTAG, "MENU_REPORT_MESSAGE_SPAM is selected");
                }
                confirmMsgForSpamDialog(msgItem.mType, msgId, msgItem.mMessageUri);
                break;
            }
            case MENU_DELETE_MESSAGE: {
                lastDeletedThreadID = mConversation.getThreadId();
                confirmDeleteMsgDialog(msgItem.mMessageUri, msgItem.mLocked);
                break;
            }

            case MENU_COPY_TO_SDCARD: {
                int resId = copyMedia(msgId) ? R.string.copy_to_sdcard_success : R.string.copy_to_sdcard_fail;
                Toast.makeText(mActivity, resId, Toast.LENGTH_SHORT).show();
                break;
            }

            case MENU_COPY_TO_DRM_PROVIDER: {
                int resId = getDrmMimeSavedStringRsrc(msgId, copyToDrmProvider(msgId));
                Toast.makeText(mActivity, resId, Toast.LENGTH_SHORT).show();
                break;
            }

            case MENU_SAVE_MSG_TO_SD_CARD: {
                Intent sdIntent = new Intent(mActivity, SaveRestoreActivity.class);
                sdIntent.putExtra(SaveRestoreActivity.EXTRA_SAVE_TYPE, SaveRestoreActivity.SAVE_MSG_TO_XML);
                sdIntent.putExtra(SaveRestoreActivity.EXTRA_MESSAGE_ID, msgId);
                sdIntent.putExtra(SaveRestoreActivity.EXTRA_THREAD_ID, mConversation.getThreadId());
                sdIntent.putExtra(SaveRestoreActivity.EXTRA_IS_MMS, msgItem.isMms());
                startActivity(sdIntent);
                break;
            }

            case MENU_REPLY_TO_SENDER:
                messageRecipient(msgItem.mAddress);
                break;

            case MENU_MESSAGE_RECIPIENT: {
                // Get the Sender phone number from the title
                String title = (String) source.getActionItem(pos).getActionTitle();
                String recipient;
                int index = getString(R.string.menu_msg).length();
                String number = title.substring(index).trim();
                if (number.equals(msgItem.mContact)) {
                    recipient = msgItem.mAddress;
                } else {
                    recipient = number;
                }
                messageRecipient(recipient);
                break;
            }

            case MENU_CONTACT_DETAILS: {
                mSelectedAddress = msgItem.mAddress;
                Contact c = Contact.get(mSelectedAddress, false);
                if (c.existsInDatabase()) {
                    Uri contactUri = c.getUri();
                    Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    mActivity.startActivityForResult(intent, REQUEST_CODE_CHECK_CONTACT);
                }
                break;
            }
            case MENU_CALL_BACK:
                startActivity((Intent) item.getTag());
                break;
            case MENU_SEND_EMAIL:
                startActivity((Intent) item.getTag());
                break;
            case MENU_ADD_ADDRESS_TO_CONTACTS:
                mAddContactIntent = (Intent) item.getTag();
                startActivityForResult(mAddContactIntent, REQUEST_CODE_ADD_CONTACT);
                break;

            }
        }
    };

    private void reportMsgAsSpam(String msgType, long msgId, Uri uri) {
        MessageReporter reporter = MessageReporter.getInstance(getContext());
        ArrayList<Integer> mmsMessages = new ArrayList<Integer>();
        ArrayList<Integer> smsMessages = new ArrayList<Integer>();
        int msgCount = mMsgListAdapter.getCount();
        int token = DELETE_SPAM_REPORT_TOKEN;

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(SpamReportTAG, "Selected Message Type: " + msgType);
            Logger.debug(SpamReportTAG, "Slected Message Id: " + msgId);
        }
        Integer id = new Integer((int) msgId);

        if (msgType.equalsIgnoreCase(MsgType.SMS.toString())) {
            smsMessages.add(id);
            reporter.reportMessagesById(smsMessages, MsgType.SMS, MessageReport.AbuseType.SPAM);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(SpamReportTAG,
                        "Cloudmark Spam Report Library has been called to report msg as spam");
            }
            for (int i = 0; i < smsMessages.size(); i++) {
                deleteMessageListener(token, uri, msgCount);
                Toast.makeText(mActivity, R.string.report_spam_msg_after_delete, Toast.LENGTH_LONG).show();
            }
        } else if (msgType.equalsIgnoreCase(MsgType.MMS.toString())) {
            mmsMessages.add(id);
            reporter.reportMessagesById(mmsMessages, MsgType.MMS, MessageReport.AbuseType.SPAM);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(SpamReportTAG,
                        "Cloudmark Spam Report Library has been called to report msg as spam");
            }
            for (int i = 0; i < mmsMessages.size(); i++) {
                deleteMessageListener(token, uri, msgCount);
                Toast.makeText(mActivity, R.string.report_spam_msg_after_delete, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * This Method will launch Unified Composer with the name prepopulated in address field with keyboard
     * displayed. If there is existing conversation, it will be added to that otherwise new Conversation will
     * start
     * 
     * @param phoneNumber
     *            Phone Number of The Message Recipient
     */
    public void messageRecipient(String phoneNumber) {
        Intent intent = mComposeMsgListener.getIntentFromParent(mActivity, 0);
        intent.putExtra("showKeyboard", true);
        intent.putExtra(ComposeMessageActivity.SEND_RECIPIENT, true);
        intent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, phoneNumber);
        mComposeMsgListener.finished();
        mComposeMsgListener.launchComposeView(intent);
    }

    /**
     * Looks to see if there are any valid parts of the attachment that can be copied to a SD card.
     * 
     * @param msgId
     */
    private boolean haveSomethingToCopyToSDCard(long msgId) {
        PduBody body = PduBodyCache.getPduBody(mActivity,
                ContentUris.withAppendedId(VZUris.getMmsUri(), msgId));
        if (body == null) {
            return false;
        }

        boolean result = false;
        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this + ".haveSomethingToCopyToSDCard: part[" + i + "] contentType=" + type);
            }

            if (ContentType.isImageType(type) || ContentType.isVideoType(type)
                    || ContentType.isAudioType(type)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * Looks to see if there are any drm'd parts of the attachment that can be copied to the DrmProvider.
     * Right now we only support saving audio (e.g. ringtones).
     * 
     * @param msgId
     */
    private boolean haveSomethingToCopyToDrmProvider(long msgId) {
        String mimeType = getDrmMimeType(msgId);
        return isAudioMimeType(mimeType);
    }

    /**
     * Simple cache to prevent having to load the same PduBody again and again for the same uri.
     */
    private static class PduBodyCache {
        private static PduBody mLastPduBody;
        private static Uri mLastUri;

        static public PduBody getPduBody(Context context, Uri contentUri) {
            if (contentUri.equals(mLastUri)) {
                return mLastPduBody;
            }
            try {
                mLastPduBody = SlideshowModel.getPduBody(context, contentUri);
                mLastUri = contentUri;
            } catch (MmsException e) {
                Logger.error(ComposeMessageFragment.class, e.getMessage(), e);
                return null;
            }
            return mLastPduBody;
        }
    };

    /**
     * Copies media from an Mms to the DrmProvider
     * 
     * @param msgId
     */
    private boolean copyToDrmProvider(long msgId) {
        boolean result = true;
        PduBody body = PduBodyCache.getPduBody(mActivity,
                ContentUris.withAppendedId(VZUris.getMmsUri(), msgId));
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (ContentType.isDrmType(type)) {
                // All parts (but there's probably only a single one) have to be
                // successful
                // for a valid result.
                result &= copyPartToDrmProvider(part);
            }
        }
        return result;
    }

    private String mimeTypeOfDrmPart(PduPart part) {
        Uri uri = part.getDataUri();
        InputStream input = null;
        try {
            input = mContentResolver.openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;

                DrmRawContent content = new DrmRawContent(fin, fin.available(),
                        DrmRawContent.DRM_MIMETYPE_MESSAGE_STRING);
                String mimeType = content.getContentType();
                return mimeType;
            }
        } catch (IOException e) {
            // Ignore
            Logger.error(getClass(), "IOException caught while opening or reading stream", e);
        } catch (DrmException e) {
            Logger.error(getClass(), "DrmException caught ", e);
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Logger.error(getClass(), "IOException caught while closing stream", e);
                }
            }
        }
        return null;
    }

    /**
     * Returns the type of the first drm'd pdu part.
     * 
     * @param msgId
     */
    private String getDrmMimeType(long msgId) {
        PduBody body = PduBodyCache.getPduBody(mActivity,
                ContentUris.withAppendedId(VZUris.getMmsUri(), msgId));
        if (body == null) {
            return null;
        }

        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (ContentType.isDrmType(type)) {
                return mimeTypeOfDrmPart(part);
            }
        }
        return null;
    }

    private int getDrmMimeMenuStringRsrc(long msgId) {
        String mimeType = getDrmMimeType(msgId);
        if (isAudioMimeType(mimeType)) {
            return R.string.save_ringtone;
        }
        return 0;
    }

    private int getDrmMimeSavedStringRsrc(long msgId, boolean success) {
        String mimeType = getDrmMimeType(msgId);
        if (isAudioMimeType(mimeType)) {
            return success ? R.string.saved_ringtone : R.string.saved_ringtone_fail;
        }
        return 0;
    }

    private boolean isAudioMimeType(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/");
    }

    private boolean copyPartToDrmProvider(PduPart part) {
        Uri uri = part.getDataUri();

        InputStream input = null;
        try {
            input = mContentResolver.openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;

                // Build a nice title
                byte[] location = part.getName();
                if (location == null) {
                    location = part.getFilename();
                }
                if (location == null) {
                    location = part.getContentLocation();
                }

                // Depending on the location, there may be an
                // extension already on the name or not
                String title = new String(location);
                final int index = title.indexOf(".");
                if (index != -1) {
                    title = title.substring(0, index);
                }

                // transfer the file to the DRM content provider
                Intent item = DrmStore.addDrmFile(mContentResolver, fin, title);
                if (item == null) {
                    Logger.warn(getClass(), "unable to add file " + uri + " to DrmProvider");
                    return false;
                }
            }
        } catch (IOException e) {
            // Ignore
            Logger.error(getClass(), "IOException caught while opening or reading stream", e);
            return false;
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Logger.error(getClass(), "IOException caught while closing stream", e);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Copies media from an Mms to the "download" directory on the SD card
     * 
     * @param msgId
     */
    private boolean copyMedia(long msgId) {
        boolean result = true;
        PduBody body = PduBodyCache.getPduBody(mActivity,
                ContentUris.withAppendedId(VZUris.getMmsUri(), msgId));
        if (body == null) {
            return false;
        }

        int partNum = body.getPartsNum();
        for (int i = 0; i < partNum; i++) {
            PduPart part = body.getPart(i);
            String type = new String(part.getContentType());

            if (ContentType.isImageType(type) || ContentType.isVideoType(type)
                    || ContentType.isAudioType(type)) {
                result &= copyPart(part, Long.toHexString(msgId)); // all parts
                // have to
                // be
                // successful
                // for a
                // valid
                // result.
            }
        }
        return result;
    }

    private boolean copyPart(PduPart part, String fallback) {
        Uri uri = part.getDataUri();

        InputStream input = null;
        FileOutputStream fout = null;
        try {
            input = mContentResolver.openInputStream(uri);
            if (input instanceof FileInputStream) {
                FileInputStream fin = (FileInputStream) input;

                byte[] location = part.getName();
                if (location == null) {
                    location = part.getFilename();
                }
                if (location == null) {
                    location = part.getContentLocation();
                }

                String fileName;
                if (location == null) {
                    // Use fallback name.
                    fileName = fallback;
                } else {
                    fileName = new String(location);
                }
                // Depending on the location, there may be an
                // extension already on the name or not
                String dir = Environment.getExternalStorageDirectory() + "/"
                        + Environment.DIRECTORY_DOWNLOADS + "/";
                String extension;
                int index;
                if ((index = fileName.indexOf(".")) == -1) {
                    String type = new String(part.getContentType());
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
                } else {
                    extension = fileName.substring(index + 1, fileName.length());
                    fileName = fileName.substring(0, index);
                }

                File file = getUniqueDestination(dir + fileName, extension);

                // make sure the path is valid and directories created for this
                // file.
                File parentFile = file.getParentFile();
                if (!parentFile.exists() && !parentFile.mkdirs()) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.error(getClass(), "[MMS] copyPart: mkdirs for " + parentFile.getPath()
                                + " failed!");
                    }
                    return false;
                }

                fout = new FileOutputStream(file);

                byte[] buffer = new byte[8000];
                int size = 0;
                while ((size = fin.read(buffer)) != -1) {
                    fout.write(buffer, 0, size);
                }

                // Notify other applications listening to scanner events
                // that a media file has been added to the sd card
                mActivity
                        .sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }
        } catch (IOException e) {
            // Ignore
            Logger.error(getClass(), "IOException caught while opening or reading stream", e);
            return false;
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    // Ignore
                    Logger.error(getClass(), "IOException caught while closing stream", e);
                    return false;
                }
            }
            if (null != fout) {
                try {
                    fout.close();
                } catch (IOException e) {
                    // Ignore
                    Logger.error(getClass(), "IOException caught while closing stream", e);
                    return false;
                }
            }
        }
        return true;
    }

    private File getUniqueDestination(String base, String extension) {
        File file = new File(base + "." + extension);

        for (int i = 2; file.exists(); i++) {
            file = new File(base + "_" + i + "." + extension);
        }
        return file;
    }

    private void showDeliveryReport(MessageItem msgItem) {
        // Commented out the fix for bug 366 which doesnt happen anymore
        // mTextEditor.setFocusable(false);
        new DeliveryReport(getActivity(), msgItem, mConversation, resendHandler).show();
    }

    private Handler resendHandler = new Handler() {
        public void handleMessage(Message msg) {
            forwardMessage(mSelectedMsgItem, true, msg.what, (String) msg.obj);
        }
    };

    private final IntentFilter mHttpProgressFilter = new IntentFilter(PROGRESS_STATUS_ACTION);

    private final BroadcastReceiver mHttpProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PROGRESS_STATUS_ACTION.equals(intent.getAction())) {
                long token = intent.getLongExtra("token", SendingProgressTokenManager.NO_TOKEN);
                if (token != mConversation.getThreadId()) {
                    return;
                }

                int progress = intent.getIntExtra("progress", 0);
                switch (progress) {
                case PROGRESS_START:
                    mActivity.setProgressBarVisibility(true);
                    break;
                case PROGRESS_ABORT:
                case PROGRESS_COMPLETE:
                    mActivity.setProgressBarVisibility(false);
                    break;
                default:
                    mActivity.setProgress(100 * progress);
                }
            }
        }
    };

    private ContactList getRecipients() {
        // If the recipients editor is visible, the conversation has
        // not really officially 'started' yet. Recipients will be set
        // on the conversation once it has been saved or sent. In the
        // meantime, let anyone who needs the recipient list think it
        // is empty rather than giving them a stale one.
        if (isRecipientsEditorVisible()) {
            synchronized (ComposeMessageFragment.class) {
                if (sEmptyContactList == null) {
                    sEmptyContactList = new ContactList();
                }
                return sEmptyContactList;
            }
        }
        return mConversation.getRecipients();
    }

    /*
     * returns the list of recipients in recipientseditor if visible else from the conversation
     */
    private List<String> getCurRecipients() {
        if (isRecipientsEditorVisible()) {
            return mRecipientEditor.getNumbers();
        }
        return mConversation.getRecipients().getNumbersList();
    }

    private void updateTitle(ContactList list) {
        if (list.size() == 0) {
            String recipient = "";
            if (mRecipientEditor != null) {
                recipient = mRecipientEditor.getText();
            }
            mDisplayNameView.setText(recipient);
            mDisplayNameView2.setText(recipient);
        } else {
            if (isMultipaneUI) {
                ((FromTextView) mDisplayNameView).resetBaseText();
                ((FromTextView) mDisplayNameView2).resetBaseText();
            }
            ((FromTextView) mDisplayNameView).setNames(list);
            ((FromTextView) mDisplayNameView2).setNames(list);
        }
        if (mConversation != null && mComposeMsgListener != null) {
            mComposeMsgListener
                    .updateCurrentThreadID(mConversation.getThreadId(), this.getClass().toString());
        }
    }

    // Get the recipients editor ready to be displayed onscreen.
    private void initRecipientsEditor() {
        if (isRecipientsEditorVisible()) {
            return;
        }

        // Must grab the recipients before the view is made visible because
        // getRecipients() returns empty recipients when the editor is visible.
        ContactList recipients;

        recipients = getRecipients();

        final View view = getView();
        if (view == null) {
            return;
        }
        final View recipientStub = view.findViewById(R.id.recipient_editor_stub);
        if (recipientStub != null) {
            ((ViewStub) recipientStub).inflate().setVisibility(View.VISIBLE);
        }

        mRecipientEditor = (RecipientEditor) mActivity.findViewById(R.id.recipEditor);
        mRecipientEditor.init(mActivity, this);
        mRecipientEditor.setFocusListener(mOnFocusChangeListener);
        mRecipientEditor.registerStateListener(this);

        mRecipientEditor.requestFocus();
        mRecipientEditor.populate(recipients, mActivity);
        mRecipientLayout = mActivity.findViewById(R.id.newmsg_header_layout);

        // For adding contact button
        ImageViewButton mBtnBrowseContacts = (ImageViewButton) mActivity.findViewById(R.id.addContact);
        mBtnBrowseContacts.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addNativeContact();
            }
        });

        if (mRecipListView == null) {
            mRecipListView = (ListView) getView().findViewById(R.id.attendeeshintslistview);
            // Putting recipient hint list, on top of all others views.
            ((ViewGroup) mRecipListView.getParent()).removeView(mRecipListView);

            android.widget.RelativeLayout.LayoutParams innerParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            innerParams.setMargins(0, 0, 0, 0);
            attendeesHintListLayout = new RelativeLayout(mActivity);
            attendeesHintListLayout.setLayoutParams(innerParams);

            Context context = mActivity.getBaseContext();
            if (isMultipaneUI) {
                fixRecipientEditor();
            } else {
                attendeesHintListLayout
                        .setPadding(
                                context.getResources().getDimensionPixelSize(
                                        R.dimen.contact_hint_list_padding_left),
                                context.getResources().getDimensionPixelSize(
                                        R.dimen.contact_hint_list_padding_top),
                                context.getResources().getDimensionPixelSize(
                                        R.dimen.contact_hint_list_padding_right), 0);
            }
            attendeesHintListLayout.addView(mRecipListView);
            FrameLayout.LayoutParams outerParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            mActivity.addContentView(attendeesHintListLayout, outerParams);
        }
        mRecipListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String contactName = mRecipCursor.getString(ContactSearchTask.CONTACT_DISPLAY_NAME);
                long contactId = Long.valueOf(mRecipCursor.getString(ContactSearchTask.CONTACT_ID));
                String keyType = null;
                String key = mRecipCursor.getString(ContactSearchTask.CONTACT_DATA);

                if (contactId != -1) {
                    int TypeValue = mRecipCursor.getInt(ContactSearchTask.PHONE_TYPE);
                    String customLabel = mRecipCursor.getString(ContactSearchTask.PHONE_LABEL);
                    keyType = Phone.getTypeLabel(mActivity.getResources(), TypeValue, customLabel).toString();
                }

                RecipContact contact = new RecipContact(contactName, key, contactId, keyType);
                mRecipientEditor.addContact(contact, mActivity, true);
                Util.showKeyboard(mActivity, mRecipientEditor.getEditControl());

                updateRecipientsState();
            }
        });

        mRecipListView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // hide the keyboard if the MsgListView is in foreground and
                // user moves the list
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    Util.forceHideKeyboard(mActivity, mRecipientEditor.getEditControl());
                }
                return false;
            }
        });

    }

    // ==========================================================
    // Activity methods
    // ==========================================================

    public static boolean cancelFailedToDeliverNotification(Intent intent, Context context) {
        if (MessagingNotification.isFailedToDeliver(intent)) {
            // Cancel any failed message notifications
            MessagingNotification.cancelNotification(context,
                    MessagingNotification.MESSAGE_FAILED_NOTIFICATION_ID);
            return true;
        }
        return false;
    }

    public static boolean cancelFailedDownloadNotification(Intent intent, Context context) {
        if (MessagingNotification.isFailedToDownload(intent)) {
            // Cancel any failed download notifications
            MessagingNotification.cancelNotification(context,
                    MessagingNotification.DOWNLOAD_FAILED_NOTIFICATION_ID);
            return true;
        }
        return false;
    }

    // constructor
    public ComposeMessageFragment() {
        mComposeMsgListener = null;
    }

    public void init(ComposeMessageListener composeMessageListener) {
        mComposeMsgListener = composeMessageListener;
    }

    @Override
    public void onAttach(Activity activity) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onAttach");
        }
        super.onAttach(activity);

        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(
                    this.getClass(),
                    "onCreate: state = " + savedInstanceState + ", intent = "
                            + Util.dumpIntent(getActivity().getIntent(), "  "));
        }
        super.onCreate(savedInstanceState);
        mActive = true;
        isMultipaneUI = Util.isMultiPaneSupported(getActivity());

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainHandler = new Handler();
        Util.hideKeyboard(getActivity(), getView());
        settings = ApplicationSettings.getInstance(mActivity);
        boolean isTabletNotProvisioned =(!settings.isProvisioned() && VZUris.isTabletDevice());

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(),"isTabletNotProvisioned="+isTabletNotProvisioned);
        }
        if (isTabletNotProvisioned || !ApplicationSettings.getInstance().getBooleanSetting(AppSettings.KEY_VMA_ACCEPT_TERMS, false)) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(),
                        "starting Provisioning activity from onActivityCreated because T&C is false");
            }
            Intent i = new Intent(mActivity, Provisioning.class);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            // finish this activity if we are still under T&C screen
            mActivity.finish();
        }
        // Initialize members for UI elements.
        initResourceRefs();
        convDeletionDialog = new ProgressDialog(mActivity);
        mContentResolver = mActivity.getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(mainHandler, mContentResolver);

        initialize(savedInstanceState);

        // init group delivery mode
        initGroupMode(false);

        final Intent intent = mActivity.getIntent();

        // TODO: why was this blocked for tablets?
        // if (MmsConfig.isTabletDevice()) {
        // mGalleryButton.setVisibility(View.GONE);
        // }

        mFromNotification = intent.getBooleanExtra(INTENT_FROM_NOTIFICATION, false);

        
        PopUpNotificationActivity.dismissPopup(mActivity);

        if(Logger.IS_DEBUG_ENABLED){
            Logger.debug(" Activity started from some other activity: mFromNotification="+mFromNotification);
        }
        
        if (TRACE) {
            android.os.Debug.startMethodTracing("compose");
        }

        IntentFilter intentFilter = new IntentFilter(SpamReportResponseReceiver.CUSTOM_INTENT);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        responseReceiver = new SpamReportResponseReceiver();
        this.getActivity().registerReceiver(responseReceiver, intentFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onCreateView");
        }

        return inflater.inflate(R.layout.compose_message_screen, container, false);
    }

    @Override
    public void onDetach() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onDetach");
        }
        super.onDetach();
        mActive = false;

        if (convDeletionDialog != null && convDeletionDialog.isShowing()) {
            convDeletionDialog.dismiss();
        }
        if (mMsgListAdapter != null) {
            mMsgListAdapter.shutdown();
        }
    }

    private void showSubjectEditor(boolean show) {
        showSubjectEditor(show, true);
    }

    private void showSubjectEditor(boolean show, boolean focus) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".showSubjectEditor: " + show);
        }

        if (mSubjectTextEditor == null) {
            // Don't bother to initialize the subject editor if
            // we're just going to hide it.
            if (show == false) {
                return;
            }
            mSubjectTextEditor = (EmojiEditText) getView().findViewById(R.id.subject);
            mSubjectTextEditor.setOnEditorActionListener(new OnEditorActionListener() {
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        gainTextEditorFocus();
                        return true;
                    }
                    return false;
                }
            });
        }

        mSubjectTextEditor.setHint(R.string.subject_hint);
        mSubjectTextEditor.setOnKeyListener(show ? mSubjectKeyListener : null);

        if (show) {
            mSubjectTextEditor.addTextChangedListener(mSubjectEditorWatcher);
        } else {
            mSubjectTextEditor.removeTextChangedListener(mSubjectEditorWatcher);
            gainTextEditorFocus();
        }

        if (mWorkingMessage.getSubject() != null && MmsConfig.enableEmojis) {
            mSubjectTextEditor.setText(EmojiParser.getInstance().addEmojiSpans(mWorkingMessage.getSubject(),
                    false));
        } else {
            mSubjectTextEditor.setText(mWorkingMessage.getSubject());
        }

        mSubjectTextEditor.setVisibility(show ? View.VISIBLE : View.GONE);

        hideOrShowTopPanel();

        if (show && focus) {
            mSubjectTextEditor.requestFocus();
            Util.showKeyboard(mActivity, mSubjectTextEditor);
        }
    }

    private void hideOrShowTopPanel() {
        boolean anySubViewsVisible = (isSubjectEditorVisible() || isRecipientsEditorVisible());
        mRecipSubjectPanel.setVisibility(anySubViewsVisible ? View.VISIBLE : View.GONE);
    }

    public void initialize(Bundle savedInstanceState) {
        final Intent intent = mActivity.getIntent();

        SharedPreferences myPreference = PreferenceManager.getDefaultSharedPreferences(mActivity);
        langcode = myPreference.getString(MessagingPreferenceActivity.LANGUAGE_CODE, "");
        // show keyboard if specified in Intent
        showKeyboard = intent.getBooleanExtra("showKeyboard", false);
        boolean showTextEditor = intent.getBooleanExtra("showTextEditor", false);

        // reset the list view's saved position
        mMsgListView.setPosition(AdapterView.INVALID_POSITION);

        if (showTextEditor) {
            mTextEditor.requestFocus();
        }

        if (!showKeyboard) {
            mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }

        // Create a new empty working message.
        mWorkingMessage = WorkingMessage.createEmpty(this);

        // Read parameters or previously saved state of this activity.
        initActivityState(savedInstanceState, intent);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".initialize: savedInstanceState = " + savedInstanceState + " intent = "
                    + intent + " mConversation = " + mConversation);
        }

        if (cancelFailedToDeliverNotification(intent, mActivity)) {
            // Show a pop-up dialog to inform user the message was
            // failed to deliver.
            undeliveredMessageDialog(getMessageDate(null));
        }
        cancelFailedDownloadNotification(intent, mActivity);

        // Set up the message history ListAdapter
        initMessageList();

        // Load the draft for this thread, if we aren't already handling
        // existing data, such as a shared picture or forwarded message.
        boolean isForwardedMessage = false;
        if (!handleSendIntent(intent)) {
            isForwardedMessage = handleForwardedMessage();
            if (!isForwardedMessage) {
                loadDraft();
            }
        }

        // Let the working message know what conversation it belongs to
        mWorkingMessage.setConversation(mConversation);

        // Show the recipients editor if we don't have a valid thread. Hide it
        // otherwise.
        if (mConversation.getThreadId() <= 0) {
            // Hide the recipients editor so the call to initRecipientsEditor
            // won't get
            // short-circuited.
            hideRecipientEditor();
            initRecipientsEditor();
            showRecipientEditor();

            // hide the message header
            hideMessageHeader();

            // Bring up the softkeyboard so the user can immediately enter
            // recipients. This
            // call won't do anything on devices with a hard keyboard.
            // Removing as it conflicts the implementation of showKeyboard

            if (intent.getBooleanExtra("is_compose", false)) {

                // Util.showKeyboard(mActivity,
                // mRecipientEditor.getEditControl());

            }

            boolean isNewMsg = intent.getBooleanExtra("compose_new", false);
            if (isNewMsg) {
                mActivity.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                Util.showKeyboard(mActivity, mRecipientEditor.getEditControl());
            }

        } else {
            hideRecipientEditor();
            showMessageHeader();

        }

        updateSendButtonState();
        setGroupMode(false, false);

        drawTopPanel(false);
        drawBottomPanel();
        if (mWorkingMessage.hasAttachment()) {
            showAttachment(mWorkingMessage);
        } else {
            mAttachmentListView.setVisibility(View.GONE);
            mMmsCountLayout.setVisibility(View.GONE);
            mAttachlayout.setVisibility(View.GONE);
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".initialize: mConversation=" + mConversation.toString());
        }
        // for Bug 23 - Clicking on New Message button on Conversation List
        // should open keyboard
        // mActivity.getWindow().setSoftInputMode(
        // WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        // | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        Configuration config = getResources().getConfiguration();
        mIsKeyboardOpen = config.keyboardHidden == KEYBOARDHIDDEN_NO;
        mIsHardKeyboardOpen = config.hardKeyboardHidden == KEYBOARDHIDDEN_NO;
        mIsLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;

        updateTitle(mConversation.getRecipients());

        if (isForwardedMessage && isRecipientsEditorVisible()) {
            // The user is forwarding the message to someone. Put the focus on
            // the
            // recipient editor rather than in the message editor.
            mRecipientEditor.requestFocus();
        }

        // Added Group Messaging functionality when Compose Message Stared from
        // Message< >,
        // Edit Recipient and Create Message in Recipient List
        if (intent.getBooleanExtra(ComposeMessageActivity.SEND_RECIPIENT, false)
                && isRecipientsEditorVisible()) {
            updateRecipientsState();
        }
        updateComposeHint();
    }

    private void showMessageHeader(boolean full, boolean animate, boolean force) {
        if (showingFullHeader != full || force) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "showMessageHeader: full = " + full + ", animate = " + animate
                        + ", force = " + force + ", showing = " + showingHeader + ", showingFull = "
                        + showingFullHeader);
            }
            showingFullHeader = full;
            final int vis;
            if (showingHeader) {
                vis = full ? View.VISIBLE : View.GONE;
                if (animate) {
                    final float fromY;
                    final float toY;
                    if (full) { // slide down
                        fromY = -1;
                        toY = 0;
                    } else { // slide up
                        fromY = 0;
                        toY = -1;
                    }
                    final TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                            Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, fromY,
                            Animation.RELATIVE_TO_SELF, toY);
                    anim.setDuration(ANIM_HEADER_DURATION);
                    mMsgHeaderPanel.startAnimation(anim);
                }
            } else {
                vis = View.GONE;
            }
            mMsgHeaderPanel.setVisibility(vis);
        }
    }

    private void showMessageHeader() {
        // use the large header in tablet layouts and when the keyboards aren't
        // open in portrait orientation
        //
        mMsgHeaderPanel.setVisibility(isMultipaneUI || showingFullHeader ? View.VISIBLE : View.GONE);
        mShrinkMsgHeaderPanel.setVisibility(View.VISIBLE);
        ContactList list = mConversation.getRecipients();
        updateTitle(list);
        updateChannel(list);
        updateAvatarView(list);
        showingHeader = true;
    }

    private void hideMessageHeader() {
        mMsgHeaderPanel.setVisibility(View.GONE);
        mShrinkMsgHeaderPanel.setVisibility(View.GONE);
        showingHeader = false;
    }

    public void loadMessageContent() {
        startMsgListQuery();
        updateSendFailedNotification();
        drawBottomPanel();
    }

    private void updateSendFailedNotification() {
        final long threadId = mConversation.getThreadId();
        if (threadId <= 0)
            return;

        // updateSendFailedNotificationForThread makes a database call, so do
        // the work off of the ui thread.
        new Thread(new Runnable() {
            public void run() {
                MessagingNotification.updateSendFailedNotificationForThread(mActivity, threadId);
            }
        }).start();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onSaveInstanceState");
        }
        super.onSaveInstanceState(outState);

        outState.putString("recipients", getRecipients().serialize());

        mWorkingMessage.writeStateToBundle(outState);

        if (mExitOnSent) {
            outState.putBoolean(ComposeMessageActivity.EXIT_ON_SENT, mExitOnSent);
        }
    }

    @Override
    public void onResume() {
        updateSendButtonState();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onResume: mConversation=" + mConversation.toString());
        }
        super.onResume();
        mPaused = false;

        mPaused = false;

        if (!isRecipientsEditorVisible() && mPendingMarkAsread) {
            MMSReadReport.handleReadReport(mActivity, mConversation, mConversation.getThreadId(),
                    PduHeaders.READ_STATUS_READ);
            mConversation.markAsRead();
        }
        mPendingMarkAsread = false;

        // OLD: get notified of presence updates to update the titlebar.
        // NEW: we are using ContactHeaderWidget which displays presence, but
        // updating presence
        // there is out of our control.
        // Contact.startPresenceObserver();

        addRecipientsListeners();

        // There seems to be a bug in the framework such that setting the title
        // here gets overwritten to the original title. Do this delayed as a
        // workaround.
        mMessageListItemHandler.postDelayed(new Runnable() {
            public void run() {
                if (!showKeyboard) {
                    Util.forceHideKeyboard(getActivity(), getView());
                }
                ContactList recipients = isRecipientsEditorVisible() ? mRecipientEditor
                        .constructContactsFromInput() : getRecipients();
                updateTitle(recipients);

            }
        }, 100);

        int color = ConversationResHelper.getBGColor();
        if (mMsgListAdapter != null) {
            mMsgListAdapter.updateBackGroundColor();
        }
        if (mMsgListView != null) {
            mMsgListView.setBackgroundColor(color);
        }

        if (rootView != null) {
            rootView.setBackgroundColor(color);
        }
    }

    @Override
    public void onPause() {
        Util.forceHideKeyboard(getActivity(), getView());
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onPause");
        }
        super.onPause();
        mPaused = true;
        // OLD: stop getting notified of presence updates to update the
        // titlebar.
        // NEW: we are using ContactHeaderWidget which displays presence, but
        // updating presence
        // there is out of our control.
        // Contact.stopPresenceObserver();

        removeRecipientsListeners();
    }

    @Override
    public void onStart() {

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onStart: mConversation = " + mConversation);
        }
        super.onStart();

        // Commenting it out as it leads to ANR and dont see a reason in making this call
        // mConversation.blockMarkAsRead(true);

        /*
         * if (mMsgListAdapter != null) { mMsgListAdapter.registerContentChangeObserver(); }
         */
        // if the list's adapter isn't set or has been cleared then (re)set it
        if (mMsgListView.getAdapter() != mMsgListAdapter) {
            mMsgListView.setAdapter(mMsgListAdapter);
        }

        // Register a BroadcastReceiver to listen on HTTP I/O process.
        mActivity.registerReceiver(mHttpProgressReceiver, mHttpProgressFilter);
        mConversation.registerThreadChangeListener(this);

        // If Recipient Editor Visible, dont load earlier contents of the
        // Conversation of the Recipient(s)
        if (!isRecipientsEditorVisible()) {
            loadMessageContent();
        }

        long threadId = mConversation.getThreadId();
        // Update the fasttrack info in case any of the recipients' contact info
        // changed
        // while we were paused. This can happen, for example, if a user changes
        // or adds
        // an avatar associated with a contact.
        mWorkingMessage.syncWorkingRecipients();

        // check if the above function set the thread id to zero and ensure it
        // has a valid thread id if so
        if (mConversation.getThreadId() != threadId) {
            mConversation.ensureThreadId();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onStart: after sync, mConversation = " + mConversation);
        }

        ContactList list = mConversation.getRecipients();
        updateTitle(list);

        SharedPreferences myPreference = PreferenceManager.getDefaultSharedPreferences(mActivity);
        String changedLanguage = myPreference.getString(MessagingPreferenceActivity.LANGUAGE_CODE, "");
        if (!changedLanguage.equals(langcode)) {
            langcode = changedLanguage;
            // Need to update string of these views again if there is a language
            // Change
            mSendButton.setText(R.string.send);
            updateComposeHint();
            updateChannel();
            if (isRecipientsEditorVisible()) {
                mRecipientEditor.refresh();
            }

            if (langcode.equals("fr")) {
                mGroupChooser.setMassTextPadding();
            } else {
                mGroupChooser.changeMassTextPadding();
            }
            mGroupChooser.refresh();
            if (mSubjectTextEditor != null) {
                mSubjectTextEditor.setHint(R.string.subject_hint);
            }
        }
    }

    @Override
    public void onStop() {

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onStop");
        }
        super.onStop();

        // Commenting it out as it leads to ANR and dont see a reason in making this call
        // Allow any blocked calls to update the thread's read status.
        // mConversation.blockMarkAsRead(false);

        if (mMsgListAdapter != null) {
            changeCursor(null, true);
            // mMsgListAdapter.unregisterContentChangeObserver();
        }

        // clear the list's adapter so that the views can be freed
        mMsgListView.setAdapter(null);

        saveDraft();

        mConversation.unregisterThreadChangeListener(this);
        // Cleanup the BroadcastReceiver.
        mActivity.unregisterReceiver(mHttpProgressReceiver);

        lastTextEntryTime = 0;
        hideEmojisPanel();
    }

    @Override
    public void onDestroyView() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onDestroyView");
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (TRACE) {
            android.os.Debug.stopMethodTracing();
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onDestroy");
        }

        // close the adapter to ensure that
        // the worker thread in the adapter is stopped
        if (mRecipAdapter != null) {
            mRecipAdapter.closeAdapter();
            mRecipAdapter = null;
        }
        if (mAttachmentAdapter != null) {
            mAttachmentAdapter.closeAdapter();
            mAttachmentAdapter = null;
        }

        // close the cursor
        synchronized (mCursorLock) {
            if (mRecipCursor != null) {
                mRecipCursor.close();
            }
        }
        this.mActivity.unregisterReceiver(responseReceiver);

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onConfigurationChanged: " + newConfig);
        }

        MessagingPreferenceActivity.setLocale(getActivity().getBaseContext());
        super.onConfigurationChanged(newConfig);

        mIsKeyboardOpen = newConfig.keyboardHidden == KEYBOARDHIDDEN_NO;
        mIsHardKeyboardOpen = newConfig.hardKeyboardHidden == KEYBOARDHIDDEN_NO;
        mIsLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;

        adjustAttachmentListViewHeight();

        if (mWorkingMessage.hasAttachment()) {
            mAttachmentListView.setAdapter(mAttachmentAdapter);
        }

        onKeyboardStateChanged(mIsKeyboardOpen);

        fixRecipientEditor();

        mMsgListAdapter.onConfigChanged(newConfig);
        mMsgListView.setAdapter(mMsgListAdapter);
        updateComposeHint();
    }

    /*
     * This Method: Since the recipient editor is placed on top of all other views and fragments. The list
     * need to be placed at appropriate place by calibrating the compose fragment width.
     */

    private void fixRecipientEditor() {
        if (isMultipaneUI && attendeesHintListLayout != null && mActivity != null) {
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            Context context = mActivity.getBaseContext();
            Resources resource = getResources();
            attendeesHintListLayout
                    .setPadding(
                            (int) ((display.getWidth() * resource.getInteger(R.integer.tabletListWeight)) / (resource
                                    .getInteger(R.integer.tabletListWeight) + resource
                                    .getInteger(R.integer.tabletContentWeight)))
                                    + context.getResources().getDimensionPixelSize(
                                            R.dimen.contact_hint_list_padding_left),
                            context.getResources().getDimensionPixelSize(
                                    R.dimen.contact_hint_list_padding_top),
                            context.getResources().getDimensionPixelSize(
                                    R.dimen.contact_hint_list_padding_right), 0);
        }
    }

    private void onKeyboardStateChanged(boolean isKeyboardOpen) {
        // If the keyboard is hidden, don't show focus highlights for
        // things that cannot receive input.
        if (isKeyboardOpen) {
            if (mRecipientEditor != null) {
                mRecipientEditor.setFocusableInTouchMode(true);
            }
            if (mSubjectTextEditor != null) {
                mSubjectTextEditor.setFocusableInTouchMode(true);
            }
            mTextEditor.setFocusableInTouchMode(true);
            mTextEditor.setHint(R.string.type_to_compose_text_enter_to_send);
        } else {
            if (mRecipientEditor != null) {
                mRecipientEditor.setFocusable(false);
            }
            if (mSubjectTextEditor != null) {
                mSubjectTextEditor.setFocusable(false);
            }
            mTextEditor.setFocusable(false);
            mTextEditor.setHint(R.string.open_keyboard_to_compose_message);
        }
    }

    public void onRestart() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onRestart");
        }
        if (mWorkingMessage.isDiscarded()) {
            // If the message isn't worth saving, don't resurrect it. Doing so
            // can lead to
            // a situation where a new incoming message gets the old thread id
            // of the discarded
            // draft. This activity can end up displaying the recipients of the
            // old message with
            // the contents of the new message. Recognize that dangerous
            // situation and bail out
            // to the ConversationList where the user can enter this in a clean
            // manner.

            if (mWorkingMessage.isWorthSaving() || mIsNativeIntentCalled) {
                mWorkingMessage.unDiscard(); // it was discarded in onStop().
                mIsNativeIntentCalled = false;
            }
            // else if (isRecipientsEditorVisible()) {
            // goToConversationList();
            // }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_DEL:
            if ((mMsgListAdapter != null) && mMsgListView.isFocused()) {
                final MessageItem msgItem = mMsgListAdapter.getSelectedMessageItem();
                if (msgItem != null) {
                    confirmDeleteMsgDialog(msgItem.mMessageUri, msgItem.mLocked);
                    return true;
                }
            }
            break;
        case KeyEvent.KEYCODE_SEARCH:
            startSearchActivity();
            return true;
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DPAD_UP:
            if (isRecipientsEditorVisible()) {
                if (mRecipListView.getVisibility() == View.VISIBLE) {
                    if (!mRecipListView.hasFocus()) {
                        // mRecipientEditor.setOnFocusChangeListener(null);
                        mRecipListView.requestFocus();
                    }
                    mRecipListView.onKeyDown(keyCode, event);
                    return true;
                }
            }
            break;
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_ENTER:
            if (isPreparedForSending()) {
                confirmSendMessageIfNeeded();
                return true;
            }
            break;
        case KeyEvent.KEYCODE_BACK:
            // check if the recipients hint list is displayed
            // if it is then hide it
            if (groupDialog.getVisibility() == View.VISIBLE) {
                groupDialog.findViewById(R.id.group_dialog_close).performClick();
                return true;
            } else if (isEmojiPanelVisible) {
                hideEmojisPanel();
                return true;
            } else {
                if (isRecipientsEditorVisible()) {
                    if (mRecipListView.getVisibility() == View.VISIBLE) {
                        mRecipListView.setSelection(ListView.INVALID_POSITION);
                        mRecipListView.setVisibility(View.GONE);
                        return true;
                    }
                }
                if (!isMultipaneUI) {
                    onBackPressed();
                    return true;
                }

            }

        }

        return false;
    }

    private void startSearchActivity() {
        Intent searchIntent = new Intent(getActivity(), ComposeMessageSearchActivity.class);
        searchIntent.putExtra("thread_id", mConversation.getThreadId());
        searchIntent.putExtra("recipienNames", mDisplayNameView.getText());
        getActivity().startActivityForResult(searchIntent, REQUEST_CODE_SEARCH_MENU_DONE);
    }

    /**
     * Function to save draft on message changed my conversation list click
     */
    public void onMessageChanged() {
        exitComposeMessageActivity(null);
        saveDraft();
    }

    // closing the topview
    public void onBackPressed() {
        exitComposeMessageActivity(new Runnable() {
            public void run() {
                if (mFromNotification) {
                    final Intent intent = new Intent(mActivity, ConversationListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                mComposeMsgListener.finished();
            }
        });
    }

    private void exitComposeMessageActivity(final Runnable exit) {
        // If the message is empty, just quit -- finishing the
        // activity will cause an empty draft to be deleted.
        mToastForDraftSave = false;// reset the save draft
        if (!mWorkingMessage.isWorthSaving()) {
            if (exit != null) {
                exit.run();
            }
            return;
        }

        final boolean isMms = mWorkingMessage.requiresMms();
        if (isRecipientsEditorVisible()) {
            int status = mRecipientEditor.flush(isMms);
            if (status == RecipientEditor.CONTACT_ADDED) {
                updateRecipientsState();
            } else if (status == RecipientEditor.BLANK_CONTACT_ONLY) {
                return;
            }
            if (!mRecipientEditor.hasValidRecipient(isMms)) {

                // TODO in case of tab UI if message is change then it will
                // create a problem and as of now we
                // just trash old message. need to implement if use
                // user press OK then only trash and load the next message
                // pressed from conversation list.
                if (isMultipaneUI && null == exit) {
                    // if message discarded reset the message.
                    resetMessage();
                    hideMessageHeader();
                    showRecipientEditor();
                    mComposeMsgListener.onDeleteGoToNext(0);
                } else
                    MessageUtils.showDiscardDraftConfirmDialog(mActivity, new DiscardDraftListener());

                return;
            }
        }

        mToastForDraftSave = true;
        if (exit != null) {
            exit.run();
        }
    }

    private void goToConversationList() {
        mComposeMsgListener.finished();
        startActivity(new Intent(mActivity, ConversationListActivity.class));
    }

    private void hideRecipientEditor() {
        if (mRecipientEditor != null) {
            mRecipientLayout.setVisibility(View.GONE);
            mRecipientEditor.removeTextWatcher(mContactWatcher);
            hideOrShowTopPanel();
        }
        mGroupChooser.setVisibility(View.GONE);
    }

    private void showRecipientEditor() {
        if (mRecipientEditor != null) {
            if (isMultipaneUI) {
                mRecipientEditor.refresh();
            }
            mRecipientEditor.addTextWatcher(mContactWatcher);
            mRecipientLayout.setVisibility(View.VISIBLE);
            hideOrShowTopPanel();
            if (recipientCount() > 1) {
                mGroupChooser.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean isRecipientsEditorVisible() {
        return (null != mRecipientLayout) && (View.VISIBLE == mRecipientLayout.getVisibility());
    }

    private boolean isSubjectEditorVisible() {
        return (null != mSubjectTextEditor) && (View.VISIBLE == mSubjectTextEditor.getVisibility());
    }

    public void onAttachmentChanged() {
        // Have to make sure we're on the UI thread. This function can be called
        // off of the UI
        // thread when we're adding multi-attachments
        if (mActive) {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    drawBottomPanel();
                    updateSendButtonState();
                    showAttachment(mWorkingMessage);
                    if (mAttachmentAdapter.getCount() == MULTI_ATTACHMENT_MAX_LIMIT) {
                        Toast.makeText(
                                mActivity,
                                getString(R.string.too_many_attachments, MULTI_ATTACHMENT_MAX_LIMIT,
                                        MULTI_ATTACHMENT_MAX_LIMIT), Toast.LENGTH_LONG).show();
                    }

                }
            });
        }
    }

    public Context getContext() {
        return mActivity;
    }

    public void onMessageChanged(final boolean protocolChanged, final boolean mms,
            final boolean messageChanged, final boolean messageMms) {
        // Have to make sure we're on the UI thread. This function can be called
        // off of the UI
        // thread when we're adding multi-attachments
        if (mActive) {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if (protocolChanged) {
                        toastConvertInfo(mms);
                    }
                }
            });
        }
    }

    Runnable mResetMessageRunnable = new Runnable() {
        public void run() {
            resetMessage();
            updateTitle(mConversation.getRecipients());
        }
    };

    public void onPreMessageSent() {
        if (mActive) {
            mActivity.runOnUiThread(mResetMessageRunnable);
            if (mMsgListAdapter != null && mFirstMessage) {
                mMsgListAdapter.updateThreadMode();
            }
        }
    }

    public void onSendingMessages(List<SendingMessage> msgs, boolean mms, long lastId) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onSendingMessages: mms = " + mms + ", lastId = " + lastId + ", count = "
                    + (mMsgListAdapter == null ? -1 : mMsgListAdapter.getCount()) + ", msgs = " + msgs);
        }
        if (mActive) {
            final MessageListAdapter adapter = mMsgListAdapter;
            if (adapter != null) {
                adapter.onSendingMessages(msgs, mms, lastId);
            }
        }
    }

    public void onMessagesSent(List<SendingMessage> msgs) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onMessagesSent: first = " + mFirstMessage + ", count = "
                    + mMsgListAdapter.getCount() + ", msgs = " + msgs);
        }
        final MessageListAdapter adapter = mMsgListAdapter;
        if (msgs != null && adapter != null) {
            adapter.onMessagesSent(msgs);
        }
        if (mActive) {
            // If we already have messages in the list adapter, it
            // will be auto-requerying; don't thrash another query in.
            if (mFirstMessage || adapter == null || adapter.getCount() == 0) {
                mFirstMessage = false;
                startMsgListQuery();
            }
        }
    }

    public void onMaxPendingMessagesReached() {
        if (mActive) {
            saveDraft();

            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(mActivity, R.string.too_many_unsent_mms, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void onAttachmentError(final int error) {
        if (mActive) {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    handleAddAttachmentError(error, R.string.type_picture);
                    onMessagesSent(null); // now requery the list of messages
                }
            });
        }
    }

    public void onSendError() {
        if (mActive) {
            mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(mActivity, R.string.unknown_sending_error, Toast.LENGTH_LONG).show();
                    onMessagesSent(null); // requery the list of messages
                }
            });
        }
    }

    private void dialRecipient() {
        String number = getRecipients().get(0).getNumber();
        Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
        startActivity(dialIntent);
    }

    /**
     * Menu for Text Editor will have following Options 1. Batch Mode 2. Add subject 3. Attach 4. Insert
     * smiley 5. Delete conversation 6. More 6.1 All Conversations 6.2 View Recipients 6.3 Edit Recipients
     */
    private void addMenuForMessage(ActionMenuBuilder menu) {

        /* if (!(isMultipaneUI)) {
           menu.add(0, MENU_CONVERSATION_LIST, 0, R.string.all_threads, R.drawable.ic_menu_friendslist, null);
        }*/

        if (MmsConfig.getMmsEnabled()) {
            if (!isSubjectEditorVisible()) {
                menu.add(0, MENU_ADD_SUBJECT, 0, R.string.add_subject, R.drawable.ic_menu_edit, null);
            } else {
                menu.add(0, MENU_REMOVE_SUBJECT, 0, R.string.remove_subject, R.drawable.ic_menu_edit, null);
            }

            /*
             * if(MmsConfig.enableEmojis) { menu.add(0, MENU_INSERT_EMOJI, 0, R.string.menu_insert_emoji,
             * R.drawable.ic_menu_emoticons, null); } else{ menu.add(0, MENU_INSERT_SMILEY, 0,
             * R.string.menu_insert_smiley, R.drawable.ic_menu_emoticons, null); }
             */
            menu.add(0, MENU_INSERT_SMILEY, 0,R.string.menu_insert_smiley, R.drawable.ic_menu_emoticons, null);
        }
        if (mMsgListAdapter.getCount() > 0) {
            // Removed search as part of b/1205708
            menu.add(0, MENU_SEARCH, 0, R.string.search_within_conv, R.drawable.ic_menu_search, null);
            Cursor cursor = mMsgListAdapter.getCursor();
            if ((null != cursor) && (cursor.getCount() > 0)) {
                menu.add(0, MENU_DELETE_CONVERSATION, 0, R.string.delete_thread, R.drawable.ic_menu_delete,
                        null);
            }
        } else {
            if (!isRecipientsEditorVisible()) {
                menu.add(0, MENU_DISCARD, 0, R.string.discard, R.drawable.ic_menu_delete, null);
            }
        }

        menu.add(MENU_CUSTOMIZE_CONV, R.string.custom_conv, R.drawable.ic_menu_custom_conversation);

        /*
         * menu.add(0, MENU_ADD_ATTACHMENT, 0, R.string.add_attachment, R.drawable.ic_menu_attachment, null);
         */

        if (!isRecipientsEditorVisible()) {
            if (mConversation.getThreadId() != 0) {
                /*
                 * menu.add(MENU_EDIT_RECIPIENT, R.string.edit_recipient, 0); menu.add(MENU_VIEW_RECIPIENT,
                 * R.string.view_recipients, 0);
                 */
                if (!isMultipaneUI) {
                    menu.add(MENU_PREFERENCES, R.string.menu_preferences, R.drawable.setting);
                }
                if (Logger.IS_DEBUG_ENABLED) {
                    menu.add(MENU_SHOW_STATUS_DETAILS, "SMS and MMS count", 0);
                }
            }
        } else {
            if (!isMultipaneUI) {
                menu.add(0, MENU_PREFERENCES, 0, R.string.menu_preferences, R.drawable.setting, null);
            }
        }

        // buildAddAddressToContactMenuItem(menu)
    }

    /**
     * Menu for Recipients Editor will have following Options 1. Add Recipients 2. Discard 3. All
     * Conversations
     */
    private void addMenuForRecipient(ActionMenuBuilder menu) {
        menu.add(0, MENU_ADD_RECIPIENT, 0, R.string.add_recipient, R.drawable.menu_add_contact, null);
        menu.add(0, MENU_DISCARD, 0, R.string.discard, R.drawable.ic_menu_delete, null);
       /* if (!(isMultipaneUI)) {
           menu.add(0, MENU_CONVERSATION_LIST, 0, R.string.all_threads, R.drawable.ic_menu_friendslist, null);
        }*/
        menu.add(0, MENU_PREFERENCES, 0, R.string.menu_preferences, R.drawable.setting, null);
    }

    /**
     * "Edit Recipient(s)" will display a new message screen with the cursor in the address field to be
     * edited.
     */
    private void editRecipients() {
        String allRecipients = ContactList.getDelimSeperatedNumbers(getRecipients(), ";");

        Intent intent = mComposeMsgListener.getIntentFromParent(mActivity, 0);
        intent.putExtra("showKeyboard", true);
        intent.putExtra(ComposeMessageActivity.SEND_RECIPIENT, true);
        intent.putExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS, allRecipients);
        mComposeMsgListener.finished();
        mComposeMsgListener.launchComposeView(intent);
    }

    /**
     * "View Recipient(s)" will display the recipient(s) in the conversation in Recipients List and the option
     * to "Create Message" at the bottom of the screen.
     * 
     * @param Numbers
     *            List
     */
    private void showAllRecipients(String[] members) {
        String numberList = "";
        for (int i = 0; i < members.length; i++) {
            numberList += members[i] + ";";
        }
        Intent intent = new Intent(mActivity, RecipientListActivity.class);
        intent.putExtra(RecipientListActivity.THREAD_ID, 0L);
        intent.putExtra(RecipientListActivity.RECIPIENTS, numberList);
        intent.putExtra(RecipientListActivity.GROUP_MODE, mGroupMms);
        startActivityForResult(intent, REQUEST_CODE_RECIPIENT_LIST);
    }

    // static class SystemProperties { // TODO, temp class to get unbundling
    // working
    // static int getInt(String s, int value) {
    // return value; // just return the default value or now
    // }
    // }

    private int getVideoCaptureDurationLimit() {
        // return 60 by default since the message size is now increased to 1.2MB
        /*
         * CamcorderProfile cm = CamcorderProfile .get(CamcorderProfile.QUALITY_LOW); if (cm != null) { return
         * cm.duration; }
         */
        return 60;
    }

    private void showEmojisPanel() {
        isEmojiPanelVisible = true;

        initEmojisPanel();
        emojiLayout.setVisibility(View.VISIBLE);
    }

    private void hideEmojisPanel() {
        isEmojiPanelVisible = false;
        if (emojiLayout != null) {
            emojiLayout.setVisibility(View.GONE);
            mViewPager.setAdapter(null);
        }
        mAttachButton.setImageResource(R.drawable.btn_attach);
    }

    private void showAddAttachmentDialog(final boolean replace, int mode) {
        QuickAction attachOptionMenu = new QuickAction(mActivity);
        attachOptionMenu.setTitle(R.string.add_attachment);

        ActionItem actionItem;

        if (mode == MODE_IMAGE || mode == MODE_ALL) {
            if (mode == MODE_ALL) {
                actionItem = new ActionItem(MENU_ATTACH_EMOJI, R.string.menu_insert_emoji,
                        R.drawable.attach_emoji);
                attachOptionMenu.addActionItem(actionItem);
            }

            actionItem = new ActionItem(ADD_IMAGE, R.string.attach_image, R.drawable.picture);
            attachOptionMenu.addActionItem(actionItem);

            actionItem = new ActionItem(TAKE_PICTURE, R.string.attach_take_photo, R.drawable.camera);
            attachOptionMenu.addActionItem(actionItem);
            // has a bug in it and also it is not mentioned in the VZ spec
            /*
             * addItem(data, context.getString(R.string.attach_slideshow),
             * R.drawable.ic_launcher_slideshow_add_sms, ADD_SLIDESHOW);
             */
        }
        if (mode == MODE_VIDEO || mode == MODE_ALL) {
            actionItem = new ActionItem(ADD_VIDEO, R.string.attach_video, R.drawable.video);
            attachOptionMenu.addActionItem(actionItem);

            if (!OEM.isMOTOROLAMZ617) {
                actionItem = new ActionItem(RECORD_VIDEO, R.string.attach_record_video,
                        R.drawable.capture_video);
                attachOptionMenu.addActionItem(actionItem);
            }
        }

        if (mode == MODE_ALL) {
            if (!OEM.isNbiLocationDisabled) {
                actionItem = new ActionItem(LOCATION, R.string.location, R.drawable.set_pin);
                attachOptionMenu.addActionItem(actionItem);
            }
            actionItem = new ActionItem(ADD_VCARD, R.string.name_card, R.drawable.namecard);
            attachOptionMenu.addActionItem(actionItem);
        }

        if (mode == MODE_AUDIO || mode == MODE_ALL) {
            if (MmsConfig.getAllowAttachAudio()) {
                actionItem = new ActionItem(ADD_SOUND, R.string.attach_sound, R.drawable.audio);
                attachOptionMenu.addActionItem(actionItem);

                // actionItem = new ActionItem(ADD_RINGTONE,
                // R.string.attach_ringtone, R.drawable.attach_sound_with_clip);
                // attachOptionMenu.addActionItem(actionItem);
            }

            actionItem = new ActionItem(RECORD_SOUND, R.string.attach_record_sound, R.drawable.attach_sound);
            attachOptionMenu.addActionItem(actionItem);
        }

        attachOptionMenu.setOnActionItemClickListener(mAttachmentClickListener);

        attachOptionMenu.show(null, getView(), mIsKeyboardOpen);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onActivityResult: requestCode=" + requestCode + ", resultCode="
                    + resultCode + ", data=" + data);
        }
        mWaitingForSubActivity = false; // We're back!
        if (mWorkingMessage.isFakeMmsForDraft()) {
            // We no longer have to fake the fact we're an Mms. At this point we
            // are or we aren't,
            // based on attachments and other Mms attrs.
            mWorkingMessage.removeFakeMmsForDraft();
        }

        requestCode &= 0xffff; // XXX it's sometimes ORed with 0x10000, not sure
        // why

        // If there's no data (because the user didn't select a picture and
        // just hit BACK, for example), there's nothing to do.
        if (requestCode != REQUEST_CODE_RECIPIENT_LIST) {
            if (requestCode != REQUEST_CODE_TAKE_PICTURE && requestCode != REQUEST_CODE_SHOW_ATTACH
                    && requestCode != REQUEST_CODE_FONT_CHANGE) {
                if (data == null) {
                    if (requestCode != REQUEST_CODE_CHECK_CONTACT) {
                        return;
                    }
                }
            } else if (resultCode != Activity.RESULT_OK) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(this + ".onActivityResult: bail due to resultCode=" + resultCode);
                }
                return;
            }
        }

        switch (requestCode) {
        case REQUEST_CODE_CREATE_SLIDESHOW:
            if (data != null) {
                WorkingMessage newMessage = WorkingMessage.load(this, data.getData());
                if (newMessage != null) {
                    mWorkingMessage = newMessage;
                    mWorkingMessage.setConversation(mConversation);
                    // Since we use multi attachment we need to setup all
                    // attachment associate with it
                    if (mWorkingMessage.hasAttachment()) {
                        showAttachment(mWorkingMessage);
                    }
                    drawTopPanel();
                    updateSendButtonState();
                }
            }
            break;
        case REQUEST_CODE_ATTACH_IMAGE:
        case REQUEST_CODE_TAKE_PICTURE: {
            if (data != null && data.getData() != null) {
                if (MmsConfig.getVzmImageEditorEnabled()) {
                    try {
                        startActivityForResult(
                                new Intent(mActivity, Class.forName(VZM_EDIT_IMAGE_INTENT)).setData(data
                                        .getData()), REQUEST_CODE_EDIT_IMAGE_DONE);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    addImage(data.getData(), true);
                }
            } else {
                try {
                    if (mCapturedImgFileName == null) {
                        mCapturedImgFileName = PreferenceManager.getDefaultSharedPreferences(mActivity)
                                .getString(PREFERENCE_FILE_PATH, null);
                    }
                    File file = new File(mCapturedImgFileName);
                    Uri uri = Uri.fromFile(file);
                    if (MmsConfig.getVzmImageEditorEnabled()) {
                        try {
                            startActivityForResult(
                                    new Intent(mActivity, Class.forName(VZM_EDIT_IMAGE_INTENT)).setData(uri),
                                    REQUEST_CODE_EDIT_IMAGE_DONE);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        addImage(uri, true);
                    }
                } catch (NullPointerException e) {
                    handleAddAttachmentError(WorkingMessage.UNKNOWN_ERROR, R.string.type_picture);
                }
            }

            break;
        }

        case REQUEST_CODE_EDIT_IMAGE_DONE: {
            try {
                File file = new File(data.getStringExtra(PREFERENCE_FILE_PATH));
                Uri uri = Uri.fromFile(file);
                addImage(uri, true);
            } catch (NullPointerException e) {
                handleAddAttachmentError(WorkingMessage.UNKNOWN_ERROR, R.string.type_picture);
            }

            break;
        }

        case REQUEST_CODE_ATTACH_VCARD: {
            addVcard(data.getData(), true);
            break;
        }

        case REQUEST_CODE_TAKE_VIDEO:
        case REQUEST_CODE_ATTACH_VIDEO:
            addVideo(data.getData(), true);
            break;

        case REQUEST_CODE_ATTACH_SOUND: {
            // BUG_id_51 new Intent to fetch all audio
            addAudio(data.getData());
            break;
        }

        case REQUEST_CODE_ATTACH_RINGTONE: {
            Uri uri = (Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (Settings.System.DEFAULT_RINGTONE_URI.equals(uri)) {
                break;
            }
            addAudio(uri);
            break;
        }

        case REQUEST_CODE_RECORD_SOUND:
            addAudio(data.getData());
            break;

        case REQUEST_CODE_ECM_EXIT_DIALOG:
            boolean outOfEmergencyMode = data.getBooleanExtra(EXIT_ECM_RESULT, false);
            if (outOfEmergencyMode) {
                sendMessage(false);

            }
            break;

        case REQUEST_CODE_ADD_CONTACT:
            // The user just added a new contact. We saved the contact info in
            // mAddContactIntent. Get the contact and force our cached contact
            // to
            // get reloaded with the new info (such as contact name). After the
            // contact is reloaded, the function onUpdate() in this file will
            // get called
            // and it will update the title bar, etc.
            if (mAddContactIntent != null) {
                String address = mAddContactIntent.getStringExtra(ContactsContract.Intents.Insert.EMAIL);
                if (address == null) {
                    address = mAddContactIntent.getStringExtra(ContactsContract.Intents.Insert.PHONE);
                }
                if (address != null) {
                    Contact contact = Contact.get(address, false);
                    if (contact != null) {
                        contact.reload();
                    }
                }
            }
            break;

        case REQUEST_CODE_ADD_CONTACT_FROM_RECIPIENT_EDITOR:
            if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                String address = mRecipientEditor.getContactKey();
                if (address != null) {
                    Contact contact = Contact.get(address, false);
                    if (contact != null) {
                        contact.reload();
                    }
                }
            }
            break;

        case REQUEST_CODE_PICK_CONTACT:
            if (resultCode == Activity.RESULT_OK) {
                Uri result = data.getData();

                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(this + ": Got a contact result: " + result.toString());
                }
                getPhoneContactInfo(data);
            }

            break;

        case REQUEST_CODE_ADD_LOCATION:
            addLocation(data, true);
            break;

        case REQUEST_CODE_SHOW_ATTACH:
            mAttachButton.performClick();
            break;

        case REQUEST_CODE_RECIPIENT_LIST:
            // update the group mode setting if the user changed it
            if (resultCode != RecipientListActivity.GROUP_UNCHANGED) {
                mGroupModeChanged = true;
                mGroupMms = resultCode == RecipientListActivity.GROUP_GROUP;
                setGroupMode(false, true);
            }
            break;

        case REQUEST_CODE_CHECK_CONTACT:
            if (mSelectedAddress != null) {
                Contact contact = Contact.get(mSelectedAddress, false);
                if (contact != null) {
                    contact.reload();
                }
            }
            break;

        case REQUEST_CODE_FONT_CHANGE:
            // the font has changed so kill this activity
            if (!isMultipaneUI) {
                mComposeMsgListener.finished();
            }
            break;

        case REQUEST_CODE_SEARCH_MENU_DONE:
            enableSearchMode(data);
            break;

        default:
            // TODO
            break;
        }
    }

    private void addLocation(final Intent data, boolean append) {
        if (mAttachmentAdapter.getCount() < MULTI_ATTACHMENT_MAX_LIMIT) {
            String filePath = data.getStringExtra(AddLocationActivity.IMAGE_FILE_PATH);
            Uri imageUri = null;
            if (!TextUtils.isEmpty(filePath)) {
                File file = new File(filePath);
                String path = filePath.substring(0, filePath.lastIndexOf(File.separator));
                path = path + File.separator + getLocPictureName();

                File renamedFile = new File(path);
                if (renamedFile.exists()) {
                    renamedFile.delete();
                }
                // each location has to be unique since
                // we support multi attachment
                if (file.renameTo(renamedFile)) {
                    imageUri = Uri.fromFile(renamedFile);
                } else {
                    Logger.error(getClass(), "ComposeMessageFragment.addLocation: could not rename the file");
                }
            }
            Uri vcardUri = createLocationVcard(data, getLocationFileName());
            addLocation(imageUri, vcardUri, append);
        } else {
            Toast.makeText(
                    mActivity,
                    getString(R.string.too_many_attachments, MULTI_ATTACHMENT_MAX_LIMIT,
                            MULTI_ATTACHMENT_MAX_LIMIT), Toast.LENGTH_LONG).show();
        }
    }

    private void displayAvailableContactInfo(final ArrayList<String> phoneList,
            final ArrayList<String> phoneType, final String contactName, final String contactId) {
        final String[] items = new String[phoneList.size()];
        for (int i = 0; i < phoneList.size(); i++) {
            items[i] = ((phoneType.get(i) != null) ? (phoneType.get(i) + " : " + phoneList.get(i))
                    : phoneList.get(i));
        }
        final AppAlignedDialog d = new AppAlignedDialog(0, contactName, mActivity);
        ListView mListView = (ListView) d.findViewById(R.id.dialog_msg);
        mListView.setAdapter(new ArrayAdapter<String>(mActivity, R.layout.single_choice_list, items));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                RecipContact contact = new RecipContact(contactName, phoneList.get(arg2), Long
                        .valueOf(contactId), phoneType.get(arg2));

                mRecipientEditor.addContact(contact, mActivity, true);
                Util.showKeyboard(mActivity, mRecipientEditor.getEditControl());

                updateRecipientsState();
                d.dismiss();

            }
        });
        d.show();
    }

    private void getPhoneContactInfo(Intent data) {
        ContentResolver cr = mActivity.getContentResolver();
        Uri uri = data.getData();
        Cursor cur = cr.query(uri, null, null, null, null);
        ArrayList<String> phoneNumbersAndEmails = new ArrayList<String>();
        ArrayList<String> phoneNumberTypes = new ArrayList<String>();
        String contactName;
        String id;
        boolean found = false;

        if (cur != null && cur.getCount() > 0) {
            while (cur.moveToNext()) {

                id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));

                contactName = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                if (Integer.parseInt(cur.getString(cur
                        .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    String phoneNo = "";

                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id },
                            null);
                    while (pCur.moveToNext()) {
                        found = true;
                        phoneNo = pCur.getString(pCur
                                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        String label = "";
                        String mimeType = pCur.getString(pCur.getColumnIndex(ContactsContract.Data.MIMETYPE));
                        if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                            int type = pCur.getInt(pCur
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                            String customLabel = pCur.getString(pCur
                                    .getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));

                            label = Phone.getTypeLabel(mActivity.getResources(), type, customLabel)
                                    .toString();
                        }
                        phoneNumbersAndEmails.add(phoneNo);
                        phoneNumberTypes.add(label);
                    }
                    pCur.close();

                } else {
                    String emailIdOfContact = "";
                    Cursor emails = cr.query(Email.CONTENT_URI, null, Email.CONTACT_ID + " = ?",
                            new String[] { id }, null);
                    while (emails.moveToNext()) {
                        found = true;
                        emailIdOfContact = emails.getString(emails.getColumnIndex(Email.DATA));
                        phoneNumbersAndEmails.add(emailIdOfContact);
                        phoneNumberTypes.add("E");
                    }
                    emails.close();

                }

                if (found) {
                    setAvailableContactInfo(phoneNumbersAndEmails, phoneNumberTypes, contactName, id);
                }
            }
        }
        if (cur != null) {
            cur.close();
        }
        // cursor returned zero count, could be a facebook contact
        if (!found && !OEM.isIceCreamSandwich) {
            String contactId = uri.getLastPathSegment();
            String name = null;
            Uri contentUri = Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI,
                    contactId + "/data");
            ArrayList<String> phoneNumber = new ArrayList<String>();
            ArrayList<String> emailAddress = new ArrayList<String>();
            ArrayList<String> emailType = new ArrayList<String>();
            ArrayList<String> phoneType = new ArrayList<String>();
            Cursor dataCursor = null;

            try {
                String unionSelect = " 1 ) union all select data1, mimetype from view_data where (contact_id="
                        + contactId
                        + " AND ("
                        + ContactsContract.Contacts.Data.MIMETYPE
                        + " == '"
                        + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        + "' "
                        + "OR "
                        + ContactsContract.Contacts.Data.MIMETYPE
                        + " == '"
                        + ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                        + "' "
                        + "OR "
                        + ContactsContract.Contacts.Data.MIMETYPE
                        + " == '"
                        + CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE + "'))";

                dataCursor = cr.query(contentUri, new String[] { "data1", "mimetype" }, unionSelect + "/*",
                        null, "*/");

                if (dataCursor != null && dataCursor.moveToFirst()) {
                    do {
                        String mimeType = dataCursor.getString(1);
                        if (mimeType.equals(CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
                            name = dataCursor.getString(0);
                        } else if (mimeType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                            phoneNumber.add(dataCursor.getString(0));
                            phoneType.add("M");
                        } else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                            emailAddress.add(dataCursor.getString(0));
                            emailType.add("E");
                        }
                    } while (dataCursor.moveToNext());
                }
                if (phoneNumber.size() > 0) {
                    setAvailableContactInfo(phoneNumber, phoneType, name, contactId);
                } else {
                    setAvailableContactInfo(emailAddress, emailType, name, contactId);
                }
            } catch (Exception e) {
                Logger.error("getPhoneContactInfo " + data, e);
            }
            if (dataCursor != null) {
                dataCursor.close();
            }
        }
    }

    private void setAvailableContactInfo(ArrayList<String> phoneDetails, ArrayList<String> phoneType,
            String contactName, String id) {

        if (phoneDetails.size() == 0) {
            final AppAlignedDialog build = new AppAlignedDialog(mActivity, R.drawable.dialog_alert,
                    R.string.no_recepients, R.string.no_valid_address);
            build.setCancelable(true);
            Button saveButton = (Button) build.findViewById(R.id.positive_button);
            saveButton.setText(R.string.yes);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addNativeContact();
                    build.dismiss();
                }
            });
            build.show();
        } else if (phoneDetails.size() == 1) {
            RecipContact contact = new RecipContact(contactName, phoneDetails.get(0), Long.valueOf(id),
                    phoneType.get(0));
            mRecipientEditor.addContact(contact, mActivity, true);
            Util.showKeyboard(mActivity, mRecipientEditor.getEditControl());

            updateRecipientsState();
        } else {
            displayAvailableContactInfo(phoneDetails, phoneType, contactName, id);
        }
    }

    private final ResizeImageResultCallback mResizeImageCallback = new ResizeImageResultCallback() {
        // TODO: make this produce a Uri, that's
        // what we want anyway
        public void onResizeResult(PduPart part, boolean append) {
            if (part == null) {
                handleAddAttachmentError(WorkingMessage.UNKNOWN_ERROR, R.string.type_picture);
                return;
            }

            Context context = mActivity;
            PduPersister persister = PduPersister.getPduPersister(context);
            int result;

            Uri messageUri = mWorkingMessage.saveAsMms(true);
            try {
                Uri dataUri = persister.persistPart(part, ContentUris.parseId(messageUri));
                result = mWorkingMessage.setAttachment(WorkingMessage.IMAGE, dataUri, append);
                if (Logger.IS_DEBUG_ENABLED) {
                    log("ResizeImageResultCallback: dataUri=" + dataUri);
                }
            } catch (MmsException e) {
                result = WorkingMessage.UNKNOWN_ERROR;
            }

            handleAddAttachmentError(result, R.string.type_picture);
        }
    };

    private void handleAddAttachmentError(final int error, final int mediaTypeStringId) {
        if (error == WorkingMessage.OK) {
            return;
        }

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Resources res = getResources();
                String mediaType = null;
                String title;
                String message;

                if (mediaTypeStringId > 0) {
                    mediaType = res.getString(mediaTypeStringId);
                }

                switch (error) {
                case WorkingMessage.UNKNOWN_ERROR:
                    message = res.getString(R.string.failed_to_add_media, mediaType);
                    Toast.makeText(mActivity, message, Toast.LENGTH_SHORT).show();
                    return;
                case WorkingMessage.UNSUPPORTED_TYPE:
                    title = res.getString(R.string.unsupported_media_format, mediaType);
                    message = res.getString(R.string.select_different_media, mediaType);
                    break;
                case WorkingMessage.MESSAGE_SIZE_EXCEEDED:
                    title = res.getString(R.string.exceed_message_size_limitation, mediaType);
                    message = res.getString(R.string.failed_to_add_media, mediaType);
                    break;
                case WorkingMessage.IMAGE_TOO_LARGE:
                    title = res.getString(R.string.failed_to_resize_image);
                    message = res.getString(R.string.resize_image_error_information);
                    break;
                case WorkingMessage.SECURITY_EXCEPTION:
                    title = res.getString(R.string.error);
                    message = res.getString(R.string.security_exception_error);
                    break;
                default:
                    throw new IllegalArgumentException("unknown error " + error);
                }

                MessageUtils.showErrorDialog(mActivity, title, message);
            }
        });
    }

    private void addImage(Uri uri, boolean append) {
        if (mAttachmentAdapter.getCount() < MULTI_ATTACHMENT_MAX_LIMIT) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this + ".addImage: append=" + append + ", uri=" + uri);
            }
            int result = mWorkingMessage.setAttachment(WorkingMessage.IMAGE, uri, append);

            if (result == WorkingMessage.IMAGE_TOO_LARGE || result == WorkingMessage.MESSAGE_SIZE_EXCEEDED) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(this + ".addImage: resize image " + uri);
                }
                MessageUtils.resizeImageAsync(mActivity, uri, mAttachmentEditorHandler, mResizeImageCallback,
                        append);
                return;
            }
            handleAddAttachmentError(result, R.string.type_picture);
        } else {
            Toast.makeText(
                    mActivity,
                    getString(R.string.too_many_attachments, MULTI_ATTACHMENT_MAX_LIMIT,
                            MULTI_ATTACHMENT_MAX_LIMIT), Toast.LENGTH_LONG).show();
        }

    }

    private void addLocation(Uri imageUri, Uri vcardUri, boolean append) {
        if (mAttachmentAdapter.getCount() < MULTI_ATTACHMENT_MAX_LIMIT) {
            int result = WorkingMessage.UNKNOWN_ERROR;

            if (vcardUri != null) {
                result = mWorkingMessage.setAttachments(WorkingMessage.LOCATION, new Uri[] { vcardUri,
                        imageUri }, append);
            }

            handleAddAttachmentError(result, R.string.type_location);
        } else {
            Toast.makeText(
                    mActivity,
                    getString(R.string.too_many_attachments, MULTI_ATTACHMENT_MAX_LIMIT,
                            MULTI_ATTACHMENT_MAX_LIMIT), Toast.LENGTH_LONG).show();
        }
    }

    public Uri createLocationVcard(Intent data, String location) {
        Uri vcardUri = null;
        Double geoLat = data.getDoubleExtra(AddLocationActivity.PLACE_LAT, -1);
        Double geoLon = data.getDoubleExtra(AddLocationActivity.PLACE_LONG, -1);
        String mapUrl = data.getStringExtra(AddLocationActivity.MAP_URL);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "mapUrl in ConvView is: " + mapUrl);
        }

        String title = data.getStringExtra(AddLocationActivity.ADDRESS_TITLE);
        String street = data.getStringExtra(AddLocationActivity.STREET);
        String cityName = data.getStringExtra(AddLocationActivity.CITY);
        String stateName = data.getStringExtra(AddLocationActivity.STATE);
        String postalCode = data.getStringExtra(AddLocationActivity.ZIP);
        String locationName = mActivity.getString(R.string.location_address);
        StringBuilder address = new StringBuilder();
        if (title != null) {
            address.append(title);
            address.append(ContactStruct.ADDRESS_SEPERATOR);
        }
        if (street != null) {
            address.append(street);
            address.append(ContactStruct.ADDRESS_SEPERATOR);
        } else {
            if (data.getStringExtra(AddLocationActivity.ADDRESS_LAT) != null) {
                address.append(data.getStringExtra(AddLocationActivity.ADDRESS_LAT));
                address.append(ContactStruct.ADDRESS_SEPERATOR);
                address.append(data.getStringExtra(AddLocationActivity.ADDRESS_LON));
            } else if (geoLat != -1) {
                double roundedLat = geoLat * 100;
                double roundedLon = geoLon * 100;
                roundedLat = Math.round(roundedLat);
                roundedLon = Math.round(roundedLon);
                roundedLat = roundedLat / 100;
                roundedLon = roundedLon / 100;
                address.append(getString(R.string.latitude) + roundedLat);
                address.append(ContactStruct.ADDRESS_SEPERATOR);
                address.append(getString(R.string.longitude) + roundedLon);
            }
        }
        if (cityName == null && stateName == null && postalCode == null) {
            String citystatezip = data.getStringExtra(AddLocationActivity.CITYSTATEZIP);
            if (citystatezip != null) {
                address.append(citystatezip);
            }
        } else {
            if (cityName != null) {
                if (address.length() > 0) {
                    address.append(ContactStruct.ADDRESS_SEPERATOR);
                }
                address.append(cityName);
            }
            if (stateName != null) {
                if (address.length() > 0) {
                    address.append(ContactStruct.ADDRESS_SEPERATOR);
                }
                address.append(stateName);
            }
            if (postalCode != null) {
                if (address.length() > 0) {
                    address.append(ContactStruct.ADDRESS_SEPERATOR);
                }
                address.append(postalCode);
            }
        }

        ContactStruct mContactStruct = new ContactStruct();
        mContactStruct.setURL(mapUrl);
        mContactStruct.setGeoCoord(String.valueOf(geoLat) + "," + String.valueOf(geoLon));
        mContactStruct.setName(locationName);
        mContactStruct.addContactmethod(VCardContacts.KIND_POSTAL,
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK, address.toString(), "", true);

        // name of the image
        mContactStruct.setVZWLocation(cityName + ".jpeg");

        OutputStreamWriter toStream = null;

        VCardComposer composer = new VCardComposer();

        try {
            // create vCard representation using ContacStruct
            String vcardString = composer.createVCard(mContactStruct, VCardComposer.VERSION_VCARD21_INT);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this + ".createLocationVcard: vcard=" + vcardString);
            }

            toStream = new OutputStreamWriter(
                    mActivity.openFileOutput(location, Context.MODE_WORLD_READABLE), "UTF-8");
            toStream.write(vcardString);

            vcardUri = Uri.fromFile(new File(mActivity.getFilesDir() + "/" + location));
        } catch (FileNotFoundException e) {
            Logger.error(e);
        } catch (IOException e) {
            Logger.error(e);
        } catch (VCardException e) {
            Logger.error(e);
        } finally {
            try {
                if (toStream != null) {
                    toStream.close();
                }
            } catch (IOException e) {
                Logger.error(e);
            }
        }

        return vcardUri;
    }

    private void addVcard(Uri uri, boolean append) {
        if (mAttachmentAdapter.getCount() < MULTI_ATTACHMENT_MAX_LIMIT) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this + ".addVcard: append=" + append + ", uri=" + uri);
            }

            int result = WorkingMessage.UNKNOWN_ERROR;

            Uri vcardUri = null;

            try {
                vcardUri = getVcardUri(uri);
            } catch (Exception e) {
                Logger.error(e);
            }

            if (vcardUri != null) {
                result = mWorkingMessage.setAttachment(WorkingMessage.VCARD, vcardUri, append);
            }

            handleAddAttachmentError(result, R.string.type_vcard);
        } else {
            Toast.makeText(
                    mActivity,
                    getString(R.string.too_many_attachments, MULTI_ATTACHMENT_MAX_LIMIT,
                            MULTI_ATTACHMENT_MAX_LIMIT), Toast.LENGTH_LONG).show();
        }
    }

    private Uri getVcardUri(Uri uri) {
        String fileName = null;
        String lookupKey = null;
        Uri lookupUri = null;
        Uri vcardUri = null;

        if (uri.getScheme().startsWith("file")) {
            // we already have the vcf file copied to the disk use it
            return uri;
        }
        // Getting the Contact Display name so that contact_display.vcf file is
        // sent instead of temp.vcf
        ContentResolver cr = mActivity.getContentResolver();

        // called by send intent when we select a Contact and share it via
        // VZMessages
        if (uri.toString().startsWith(mVcardUri)) {
            lookupUri = uri;

            uri = Uri.parse(uri.toString().replace(mVcardUri, mLookUpUri));
        }

        Cursor cur = cr.query(uri, new String[] { ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.LOOKUP_KEY }, null, null, ContactsContract.Contacts.DISPLAY_NAME);

        lookupKey = null;
        if (cur.moveToNext()) {
            String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            lookupKey = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));

            if (name != null) {
                // remove special characters in name
                name = name.replaceAll("[^a-zA-Z0-9]+", "");
                if (name.length() == 0) {
                    fileName = getLocationFileName();
                } else {
                    fileName = name + ".vcf";
                }
            }
        }

        if (cur != null) {
            cur.close();
        }

        if (lookupKey != null || lookupUri != null) {
            if (null == fileName) {
                fileName = getLocationFileName();
            }

            if (lookupUri == null) {
                lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
            }

            vcardUri = copyToTemp(lookupUri, fileName);
        }
        return vcardUri;
    }

    /**
     * This Method creates a vcard file in the data folder
     * 
     * @param uri
     * @param string
     */
    private Uri copyToTemp(Uri uri, String name) {
        FileInputStream fis = null;
        FileOutputStream toStream = null;
        Uri copiedFileUri = null;

        try {
            AssetFileDescriptor fd = mActivity.getContentResolver().openAssetFileDescriptor(uri, "r");
            fis = fd.createInputStream();

            toStream = mActivity.openFileOutput(name, Context.MODE_WORLD_READABLE);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                toStream.write(buffer, 0, bytesRead);
            }

            toStream.close();
            fis.close();
            buffer = null;

            copiedFileUri = Uri.fromFile(new File(mActivity.getFilesDir() + "/" + name));
        } catch (FileNotFoundException e) {
            Logger.error(e);
        } catch (IOException e) {
            Logger.error(e);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (toStream != null) {
                    toStream.close();
                }
            } catch (IOException e) {
                Logger.error(e);
            }
        }

        return copiedFileUri;
    }

    private void addVideo(Uri uri, boolean append) {
        if (mAttachmentAdapter.getCount() < MULTI_ATTACHMENT_MAX_LIMIT) {
            if (uri != null) {
                int result = mWorkingMessage.setAttachment(WorkingMessage.VIDEO, uri, append);
                handleAddAttachmentError(result, R.string.type_video);
            }
        } else {
            Toast.makeText(
                    mActivity,
                    getString(R.string.too_many_attachments, MULTI_ATTACHMENT_MAX_LIMIT,
                            MULTI_ATTACHMENT_MAX_LIMIT), Toast.LENGTH_LONG).show();
        }
    }

    private void addAudio(Uri uri) {
        if (mAttachmentAdapter.getCount() < MULTI_ATTACHMENT_MAX_LIMIT) {
            int result = mWorkingMessage.setAttachment(WorkingMessage.AUDIO, uri, true);
            handleAddAttachmentError(result, R.string.type_audio);
        } else {
            Toast.makeText(
                    mActivity,
                    getString(R.string.too_many_attachments, MULTI_ATTACHMENT_MAX_LIMIT,
                            MULTI_ATTACHMENT_MAX_LIMIT), Toast.LENGTH_LONG).show();
        }

    }

    private boolean handleForwardedMessage() {
        final Intent intent = mActivity.getIntent();

        // If this is a forwarded message, it will have an Intent extra
        // indicating so. If not, bail out.
        if (!intent.getBooleanExtra(ComposeMessageActivity.FORWARD_MESSAGE, false)) {
            return false;
        }

        Uri uri = intent.getParcelableExtra("msg_uri");

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".handleForwardedMessage: uri = " + uri);
        }

        if (uri != null) {
            WorkingMessage workingMessage = WorkingMessage.load(this, uri);

            if (workingMessage == null) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(this + ".handleForwardedMessage: uri = " + uri + " Could not load the "
                            + "forwarded message ");
                }
                return false;
            }
            mWorkingMessage = workingMessage;
            mWorkingMessage.setSubject(intent.getStringExtra("subject"), false);
        } else {
            mWorkingMessage.setText(intent.getStringExtra("sms_body"));
        }

        String forwarding = intent.getStringExtra("forwarduri");
        boolean isVideo = intent.getBooleanExtra("isVideo", false);
        if (forwarding != null) {
            if (isVideo) {
                addVideo(Uri.parse(forwarding), false);
            } else {
                addImage(Uri.parse(forwarding), false);
            }
            mWorkingMessage.setSubject(getString(R.string.forward_prefix), false);
        }
        // let's clear the message thread for forwarded messages
        changeCursor(null, false);

        return true;
    }

    private boolean handleSendIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return false;
        }

        final String mimeType = intent.getType();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            if (extras.containsKey(Intent.EXTRA_STREAM)) {
                Uri uri = (Uri) extras.getParcelable(Intent.EXTRA_STREAM);
                addAttachment(mimeType, uri, false);
                return true;
            } else if (extras.containsKey(Intent.EXTRA_TEXT)) {
                mWorkingMessage.setText(extras.getString(Intent.EXTRA_TEXT));
                return true;
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && extras.containsKey(Intent.EXTRA_STREAM)) {
            SlideshowModel slideShow = mWorkingMessage.getSlideshow();
            final ArrayList<Parcelable> uris = extras.getParcelableArrayList(Intent.EXTRA_STREAM);
            int currentSlideCount = slideShow != null ? slideShow.size() : 0;
            int importCount = uris.size();
            if (importCount + currentSlideCount > SlideshowEditor.MAX_SLIDE_NUM) {
                importCount = Math.min(SlideshowEditor.MAX_SLIDE_NUM - currentSlideCount, importCount);
                Toast.makeText(mActivity,
                        getString(R.string.too_many_attachments, SlideshowEditor.MAX_SLIDE_NUM, importCount),
                        Toast.LENGTH_LONG).show();
            }

            // Attach all the pictures/videos off of the UI thread.
            // Show a progress alert if adding all the slides hasn't finished
            // within one second.
            // Stash the runnable for showing it away so we can cancel
            // it later if adding completes ahead of the deadline.
            final AppAlignedDialog build = new AppAlignedDialog(mActivity, R.drawable.dialog_alert,
                    R.string.adding_attachments_title, R.string.adding_attachments);
            final Runnable showProgress = new Runnable() {
                public void run() {
                    build.show();
                }
            };
            // Schedule it for one second from now.
            mAttachmentEditorHandler.postDelayed(showProgress, 1000);

            final int numberToImport = importCount;
            new Thread(new Runnable() {
                public void run() {
                    Looper.prepare();
                    for (int i = 0; i < numberToImport; i++) {
                        Parcelable uri = uris.get(i);
                        addAttachment(mimeType, (Uri) uri, true);
                    }
                    // Cancel pending show of the progress alert if necessary.
                    mAttachmentEditorHandler.removeCallbacks(showProgress);
                    build.dismiss();
                    Looper.loop();
                }
            }).start();
            return true;
        }

        if (Logger.IS_DEBUG_ENABLED) {
            // log error if we are not handling send intent
            Logger.debug(ComposeMessageFragment.class,
                    " Send Intent not handled " + Util.dumpIntent(intent, null));
        }
        return false;
    }

    private void addAttachment(String type, Uri uri, boolean append) {
        if (uri != null) {
            // When we're handling Intent.ACTION_SEND_MULTIPLE, the passed in
            // items can be
            // videos, and/or images, and/or some other unknown types we don't
            // handle. When
            // a single attachment is "shared" the type will specify an image or
            // video. When
            // there are multiple types, the type passed in is "*/*". In that
            // case, we've got
            // to look at the uri to figure out if it is an image or video.
            try {
                boolean wildcard = "*/*".equals(type);
                if (type.startsWith("image/") || (wildcard && uri.toString().startsWith(mImageUri))) {
                    addImage(uri, append);
                } else if (type.startsWith("video/") || (wildcard && uri.toString().startsWith(mVideoUri))) {
                    addVideo(uri, append);
                } else if (type.startsWith("audio/") || (wildcard && uri.toString().startsWith(mAudioUir))) {
                    addAudio(uri);
                } else if (ContentType.isVcardTextType(type)) {
                    if (uri.toString().startsWith(mVcardUri) || uri.getScheme().equals("file")) {
                        addVcard(uri, append);
                    } else if (uri.toString().startsWith(motoralVcardUri)) {
                        Uri vcardUri = getModifiedVcardUri(uri);

                        if (vcardUri != null) {
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug("motorola modified vcard uri " + uri.toString());
                            }
                            addVcard(vcardUri, append);
                        }
                    }
                }
            } catch (SecurityException e) {
                if (Logger.IS_ERROR_ENABLED) {
                    Logger.error("Could not handle the send intent due to security exception " + e);
                }
                handleAddAttachmentError(WorkingMessage.SECURITY_EXCEPTION, 0);
            }
        }
    }

    /**
     * Modify the motorola uri to actual contacts uri
     * 
     * @param uri
     * @return
     */
    private Uri getModifiedVcardUri(Uri uri) {
        String lookupKey = uri.getLastPathSegment();

        if (lookupKey.contains(".")) {
            // on some devices it returns a lookupkey along with some other
            // values so fetch only the lookupkey
            int lastIndex = lookupKey.lastIndexOf('.') + 1;
            if (lastIndex < lookupKey.length()) {
                lookupKey = lookupKey.substring(lastIndex);
            } else {
                return null;
            }
        }
        Uri lookUpUri = Uri.withAppendedPath(Uri.parse(mLookUpUri), lookupKey);

        return lookUpUri;
    }

    private String getResourcesString(int id, String mediaName) {
        Resources r = getResources();
        return r.getString(id, mediaName);
    }

    private void drawBottomPanel() {
        // Reset the counter for text editor.
        resetCounter();
        if (searchString == null)
            mBottomPanel.setVisibility(View.VISIBLE);

        CharSequence text = mWorkingMessage.getText();

        // TextView.setTextKeepState() doesn't like null input.
        if (text != null && text.length() != 0) {
            if (MmsConfig.enableEmojis) {
                mTextEditor.setTextKeepState(EmojiParser.getInstance().addEmojiSpans(text, false));
            } else {
                mTextEditor.setTextKeepState(SmileyParser.getInstance().addSmileySpans(text, false));
            }
        } else {
            mTextEditor.setText("");
        }
    }

    private void drawTopPanel() {
        showSubjectEditor(mWorkingMessage.hasSubject());
    }

    private void drawTopPanel(boolean focus) {
        showSubjectEditor(mWorkingMessage.hasSubject(), focus);
    }

    // ==========================================================
    // Interface methods
    // ==========================================================

    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.btnSend) {
            if (isPreparedForSending()) {
                confirmSendMessageIfNeeded();
            }
        } else if (id == R.id.recipientInfo || id == R.id.imgNext || id == R.id.shrinktxtName) {
            Intent intent = getActivity().getIntent();
            long threadId = intent.getLongExtra("thread_id", -2L);
            if (threadId != INVALID) {
                showAllRecipients(getConversation().getRecipients().getNumbers());
            }
        } else if (id == R.id.imgGallery || id == R.id.shrinkGallery) {
            Util.forceHideKeyboard(getActivity(), getView());
            if (!isMultipaneUI) {
                Intent i = new Intent(getActivity(), SharedContentActivity.class);
                i.putExtra("threadid", getConversation().ensureThreadId());
                i.putExtra("members", getConversation().getRecipients().getNumbers());
                getActivity().startActivityForResult(i, REQUEST_CODE_SHOW_ATTACH);
            } else {

                Intent i = getActivity().getIntent();
                i.putExtra("thread_id", getConversation().ensureThreadId());
                i.putExtra("members", getConversation().getRecipients().getNumbers());
                // bug:3199
                if (getConversation().ensureThreadId() > 0) {
                    i.putExtra("is_compose", false);
                }
                mComposeMsgListener.setGalleryLoaded(true);
                mComposeMsgListener.launchComposeView(i);
            }
        } else if (id == R.id.btnAttach) {
            // showAttachMenu();
            if (isEmojiPanelVisible) {
                hideEmojisPanel();
                if (mTextEditor.hasFocus())
                    Util.forceShowKeyboard(mActivity, mTextEditor);
                else
                    Util.forceShowKeyboard(mActivity, mSubjectTextEditor);
            } else {
                showAttachMenu();
            }
        }

    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event != null) {
            // if shift key is down, then we want to insert the '\n' char in the
            // TextView;
            // otherwise, the default action is to send the message.
            if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                return false;
            }
            if (!event.isShiftPressed()) {
                if (isPreparedForSending()) {
                    confirmSendMessageIfNeeded();
                }
                return true;
            }
            return false;
        }

        if (isPreparedForSending()) {
            confirmSendMessageIfNeeded();
        }
        return false;
    }

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {

        boolean isMMS = false;

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            lastTextEntryTime = SystemClock.uptimeMillis();
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mWorkingMessage.setText(s);
            mWorkingMessage.updateContainsEmailToMms(getCurRecipients(), s);
            updateSendButtonState();
            updateCounter(s, start, before, count);
            ensureCorrectButtonHeight();
            int totalLength = MessagingPreferenceActivity.getSignature(mActivity, s).length();

            if (totalLength > SMS_TO_MMS_MAX_CHAR_LENGTH) {
                if (mWorkingMessage.updateState(WorkingMessage.LENGTH_REQUIRES_MMS, true, true)) {
                    isMMS = true;
                    toastConvertInfo(true);
                }

            } else if (totalLength == SMS_TO_MMS_MAX_CHAR_LENGTH) {
                if (mWorkingMessage.updateState(WorkingMessage.LENGTH_REQUIRES_MMS, false, false)) {
                    isMMS = false;
                    toastConvertInfo(false);
                    mTextCounter.setVisibility(View.GONE);
                }
            } else if (totalLength < SMS_TO_MMS_MAX_CHAR_LENGTH) {
                if (mWorkingMessage.updateState(WorkingMessage.LENGTH_REQUIRES_MMS, false, false)) {
                    isMMS = false;
                    toastConvertInfo(false);
                    mTextCounter.setVisibility(View.GONE);
                }
            }

            if (mWorkingMessage.requiresMms()) {
                mTextCounter.setVisibility(View.GONE);
            }
        }

        public void afterTextChanged(Editable s) {

        }
    };

    private final TextEntryStateProvider textEntryStateProvider = new TextEntryStateProvider() {
        public long getLastTextEntryTime() {
            return lastTextEntryTime;
        }
    };

    /**
     * Ensures that if the text edit box extends past two lines then the button will be shifted up to allow
     * enough space for the character counter string to be placed beneath it.
     */
    private void ensureCorrectButtonHeight() {
        int currentTextLines = mTextEditor.getLineCount();
        if (currentTextLines <= 2) {
            mTextCounter.setVisibility(View.GONE);
        } else if (currentTextLines > 2 && mTextCounter.getVisibility() == View.GONE) {
            // Making the counter invisible ensures that it is used to correctly
            // calculate the position of the send button even if we choose not
            // to
            // display the text.
            mTextCounter.setVisibility(View.INVISIBLE);
        }
    }

    private final TextWatcher mSubjectEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            mWorkingMessage.setSubject("", true);
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mWorkingMessage.setSubject(s, true);
        }

        public void afterTextChanged(Editable s) {

        }
    };

    // ==========================================================
    // Private methods
    // ==========================================================

    /**
     * Initialize all UI elements from resources.
     */
    private void initResourceRefs() {
        fragmentView = getView();
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        softKeyboardLimit = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                SOFT_KEYBOARD_LIMIT, metrics));
        mMsgListView = (MessageListView) fragmentView.findViewById(R.id.history);
        mMsgListView.setDivider(null); // no divider so we look like IM
                                       // conversation.
        mMsgListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        mGroupChooser = (GroupModeChooser) fragmentView.findViewById(R.id.group_chooser);
        mGroupChooser.setListener(groupModeListener);
        groupDialog = fragmentView.findViewById(R.id.group_dialog);
        mChannel = (TextView) fragmentView.findViewById(R.id.txtDestType);
        mChannel2 = (TextView) fragmentView.findViewById(R.id.shrinktxtDestType);
        // mMessageProgress = fragmentView.findViewById(R.id.messageProgress);
        mBottomPanel = fragmentView.findViewById(R.id.bottom_panel);
        mTextEditor = (EmojiEditText) fragmentView.findViewById(R.id.embedded_text_editor);
        mTextEditor.setOnEditorActionListener(this);
        mTextEditor.addTextChangedListener(mTextEditorWatcher);
        mTextCounter = (TextView) fragmentView.findViewById(R.id.text_counter);
        mSendButton = (TextButton) fragmentView.findViewById(R.id.btnSend);
        mSendButton.setOnClickListener(this);
        fragmentView.findViewById(R.id.imgNext).setOnClickListener(this);
        fragmentView.findViewById(R.id.recipientInfo).setOnClickListener(this);

        mAttachButton = (ImageViewButton) fragmentView.findViewById(R.id.btnAttach);
        mAttachButton.setOnClickListener(this);
        // initEmojiResources();
        mRecipSubjectPanel = fragmentView.findViewById(R.id.recipients_subject_linear);
        mRecipSubjectPanel.setFocusable(false);
        mDisplayNameView = (TextView) fragmentView.findViewById(R.id.txtName);
        mMsgHeaderPanel = fragmentView.findViewById(R.id.title_header_layout);
        mShrinkMsgHeaderPanel = fragmentView.findViewById(R.id.shrink_title_header_layout);
        mDisplayNameView2 = (TextView) fragmentView.findViewById(R.id.shrinktxtName);
        mDisplayNameView2.setOnClickListener(this);
        View landscapeGallery = fragmentView.findViewById(R.id.shrinkGallery);
        if (MmsConfig.isTabletDevice()) {
            landscapeGallery.setOnClickListener(this);
        } else {
            landscapeGallery.setVisibility(View.GONE);
        }
        mGalleryButton = (ImageViewButton) mMsgHeaderPanel.findViewById(R.id.imgGallery);
        mGalleryButton.setOnClickListener(this);
        mGalleryButton.setEnabled(true);// avoiding double click
        mAvatarView = (ContactImage) fragmentView.findViewById(R.id.imgAvatar);
        mMmsCountLayout = (LinearLayout) fragmentView.findViewById(R.id.mmsCountLayout);
        mMmsAttachmentCount = (TextView) fragmentView.findViewById(R.id.mmsCountText);
        mAttachlayout = (LinearLayout) fragmentView.findViewById(R.id.attachLayout);
        mAttachmentListView = (ListView) fragmentView.findViewById(R.id.attachment_listView);
        mAttachmentAdapter = new MultiAttachmentAdapter(mActivity, mAttachmentEditorHandler,
                mAttachmentListView);
        mAttachLayoutMaxHeight = Math.round(mActivity.getResources().getDimension(
                R.dimen.attachmentLayoutMaxHeight));

        mSearchNavigatePanel = fragmentView.findViewById(R.id.searchViewPanel);
        mSearchNavigatePanel.setVisibility(View.GONE);
        mUpArrow = (ImageView) fragmentView.findViewById(R.id.searchUp);
        mDownArrow = (ImageView) fragmentView.findViewById(R.id.searchDown);
        mSearchDone = (TextButton) fragmentView.findViewById(R.id.btnDone);
        searchResultTxt = (TextView) fragmentView.findViewById(R.id.searchResultTxt);

        // Added TextEditor Touch listener to gain its focus on touch
        mTextEditor.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionevent) {
                view.requestFocus();
                return false;
            }
        });

        // we always use the large header for tablet layouts, otherwise we use a
        // layout
        // listener that makes the header size dependent on the presence of the
        // soft keyboard
        //
        showMessageHeader(true, false, true); // default for both cases
        /* if (!isMultipaneUI) */{
            fragmentView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }

        rootView = fragmentView.findViewById(R.id.composeFragment);
    }

    private void initEmojisPanel() {
        if (emojiLayout == null) {
            emojiLayout = (LinearLayout) fragmentView.findViewById(R.id.emojiTabLayout);
            mViewPager = (ViewPager) fragmentView.findViewById(R.id.pager);
            mTabHost = (OnOffTabHost) fragmentView.findViewById(R.id.tabhost);

            mTabHost.addTab(R.drawable.emoji_drawable_smile);
            mTabHost.addTab(R.drawable.emoji_drawable_flower);
            mTabHost.addTab(R.drawable.emoji_drawable_bell);
            mTabHost.addTab(R.drawable.emoji_drawable_car);
            mTabHost.addTab(R.drawable.emoji_drawable_symbol);

            View backButton = fragmentView.findViewById(R.id.del);
            backButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText insertTextEditor = mTextEditor;

                    if (mSubjectTextEditor != null && mSubjectTextEditor.hasFocus()) {
                        insertTextEditor = mSubjectTextEditor;
                    }
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL);
                    insertTextEditor.dispatchKeyEvent(event);
                }
            });

            backButton.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View arg0) {
                    EditText insertTextEditor = mTextEditor;
                    if (mSubjectTextEditor != null && mSubjectTextEditor.hasFocus()) {
                        insertTextEditor = mSubjectTextEditor;
                    }
                    KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL);
                    insertTextEditor.dispatchKeyEvent(event);
                    return true;
                }
            });
        }
        mTabHost.setSelected(0);

        EmojiPagerAdapter pagerAdapter = new EmojiPagerAdapter(mActivity, mViewPager,
                new EmojiPagerAdapter.OnEmojiSelectedListener() {
                    public void onEmojiSelected(CharSequence emoji) {
                        EditText insertTextEditor = mTextEditor;

                        if (mSubjectTextEditor != null && mSubjectTextEditor.hasFocus()) {
                            insertTextEditor = mSubjectTextEditor;
                        }
                        int start = insertTextEditor.getSelectionStart();
                        int end = insertTextEditor.getSelectionEnd();
                        insertTextEditor.getText().replace(Math.min(start, end), Math.max(start, end), emoji);

                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("replacing EMOJI :" + emoji);
                        }
                    }
                });

        this.mViewPager.setAdapter(pagerAdapter);
        EmojiPageChangeListener em = new EmojiPageChangeListener(mTabHost, mViewPager);
        this.mViewPager.setOnPageChangeListener(em);
        this.mTabHost.setTabChangedListener(em);
    }

    private OnGlobalLayoutListener onGlobalLayoutListener = new OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            rootHeight = fragmentView.getRootView().getHeight();
            final int contentHeight = fragmentView.getHeight();
            final boolean full = rootHeight - contentHeight <= softKeyboardLimit;
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(ComposeMessageFragment.class, "onGlobalLayout: rootHeight = " + rootHeight
                        + ", contentHeight = " + contentHeight + ", diff = " + (rootHeight - contentHeight));
            }

            if (!isMultipaneUI) {
                if (mIsLandscape) {
                    // landscape: use the small header
                    showMessageHeader(false, false, false);
                } else if (mIsHardKeyboardOpen) {
                    // hard keyboard is open: use the large header
                    showMessageHeader(true, false, false);
                } else {
                    // use the small header if the soft keyboard is showing
                    // unfortunately the best way to check if the soft keyboard is
                    // showing is to
                    // see if our height is significantly less than the root
                    // window's height
                    //
                    showMessageHeader(full, true, false);
                }
            }

            if (!full) {
                hideEmojisPanel();
            }
        }
    };

    private void confirmDeleteMsgDialog(final Uri uri, final boolean locked) {
        int text = locked ? R.string.confirm_dialog_locked_title : R.string.confirm_dialog_title;
        int messageText = locked ? R.string.confirm_delete_locked_message : R.string.confirm_delete_message;
        final AppAlignedDialog d = new AppAlignedDialog(mActivity, R.drawable.dialog_alert, text, messageText);
        final int msgCount = mMsgListAdapter.getCount();
        final int token = DELETE_MESSAGE_TOKEN;
        Button deleteButton = (Button) d.findViewById(R.id.positive_button);
        deleteButton.setText(R.string.delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteMessageListener(token, uri, msgCount);
                d.dismiss();
            }
        });
        Button cancelButton = (Button) d.findViewById(R.id.negative_button);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setText(R.string.no);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    private void confirmMsgForSpamDialog(final String msgType, final long msgId, final Uri uri) {
        int title = R.string.confirm_dialog_report_spam_title;
        int messageText = R.string.confirm_dialog_report_spam_message;
        final AppAlignedDialog d = new AppAlignedDialog(mActivity, R.drawable.dialog_alert, title,
                messageText);

        Button spamButton = (Button) d.findViewById(R.id.positive_button);
        spamButton.setText(R.string.report_spam);
        spamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportMsgAsSpam(msgType, msgId, uri);
                d.dismiss();
            }
        });
        Button cancelButton = (Button) d.findViewById(R.id.negative_button);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setText(R.string.no);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    void undeliveredMessageDialog(long date) {
        String body;
        LinearLayout dialog = (LinearLayout) LayoutInflater.from(mActivity).inflate(
                R.layout.retry_sending_dialog, null);

        if (date >= 0) {
            body = getString(R.string.undelivered_msg_dialog_body,
                    MessageUtils.formatTimeStampString(date, true));
        } else {
            // FIXME: we can not get sms retry time.
            body = getString(R.string.undelivered_sms_dialog_body);
        }

        ((TextView) dialog.findViewById(R.id.body_text_view)).setText(body);

        Toast undeliveredDialog = new Toast(mActivity);
        undeliveredDialog.setView(dialog);
        undeliveredDialog.setDuration(Toast.LENGTH_LONG);
        undeliveredDialog.show();
    }

    private void startMsgListQuery() {
        Uri conversationUri = mConversation.getUri();
        if (conversationUri == null) {
            return;
        }

        try {
            // Kick off the new query
            mBackgroundQueryHandler.startMessageListQuery(conversationUri);
        } catch (SQLiteException e) {
            SqliteWrapper.checkSQLiteException(mActivity, e);
        }
    }

    private void initMessageList() {
        enableSearchMode(mActivity.getIntent());
    }

    private void enableSearchMode(Intent searchIntent) {
        if (searchIntent != null) {
            searchString = searchIntent.getStringExtra(SearchActivity.SEARCHED_STRING);
            msgIdsList = new ArrayList<Long>();
            String allMsgIds = searchIntent.getStringExtra(SearchActivity.SELECTED_ALL_MSG);
            if (allMsgIds != null) {
                String[] am = allMsgIds.split(":");
                for (String string : am) {
                    msgIdsList.add(Long.parseLong(string));
                }
                msgIdsCcount = msgIdsList.size();
                // initSearchMode();
                mHighLightedMsgId = searchIntent.getLongExtra("SELECTED_MSG", 0);
            } else {
                if (mSearchNavigatePanel != null && (mSearchNavigatePanel.getVisibility() == View.VISIBLE)) {
                    searchString = null;
                    mHighLightedMsgId = 0;
                    rowIdsList = null;
                    msgIdsCcount = 0;
                    mSearchNavigatePanel.setVisibility(View.GONE);
                    drawBottomPanel();
                }
            }
        }
        refreshListView(mHighLightedMsgId);
    }

    private void initSearchMode() {
        mBottomPanel.setVisibility(View.GONE);
        mSearchNavigatePanel.setVisibility(View.VISIBLE);
        int result = msgIdsCcount == 1 ? R.string.search_result_text_singular
                : R.string.search_result_text_plural;
        searchResultTxt.setText("" + msgIdsCcount + " " + getString(result) + " \"" + searchString + "\"");
        searchResultTxt.setTextColor(Color.BLACK);

        mSearchDone.setEnabled(true);
        mSearchDone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSearchPanel();
            }
        });

        mUpArrow.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (--currentPos >= 0) {
                    initSearchNavigation();
                    long msgId = msgIdsList.get(currentPos);
                    int pos = rowIdsList.get(currentPos);

                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Search: moving to pos DOwn:index:" + currentPos + " rowid:" + pos
                                + " msgId:" + msgId);
                    }

                    if (pos == mMsgListAdapter.getCount() - 1) {
                        pos = ListView.INVALID_POSITION;
                    }
                    mMsgListView.setPosition(pos);
                    mMsgListAdapter.highLightMsg(msgId);
                    mMsgListAdapter.notifyDataSetChanged();
                    initSearchNavigation();
                } else
                    currentPos = 0;
            }
        });

        mDownArrow.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (++currentPos <= (msgIdsCcount - 1)) {
                    long msgId = msgIdsList.get(currentPos);
                    int pos = rowIdsList.get(currentPos);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Search: moving to pos UP:index:" + currentPos + " rowid:" + pos
                                + " msgId:" + msgId);
                    }

                    if (pos == mMsgListAdapter.getCount() - 1) {
                        pos = ListView.INVALID_POSITION;
                    }
                    mMsgListView.setPosition(pos);
                    mMsgListAdapter.highLightMsg(msgId);
                    mMsgListAdapter.notifyDataSetChanged();
                    initSearchNavigation();
                } else {
                    currentPos = 0;
                }
            }
        });
    }

    private void closeSearchPanel() {
        mMsgListAdapter.highLightMsg(0);
        mMsgListAdapter.notifyDataSetChanged();
        searchString = null;
        msgIdsList = null;
        currentPos = 0;
        msgIdsCcount = 0;
        mHighLightedMsgId = 0;
        rowIdsList = null;
        searchResultTxt.setText("");
        mSearchNavigatePanel.setVisibility(View.GONE);
        drawBottomPanel();
    }

    private void initSearchNavigation() {
        if (msgIdsCcount == 1) {
            mUpArrow.setImageResource(R.drawable.search_up_arrow_inactive);
            mUpArrow.setEnabled(false);
            mDownArrow.setImageResource(R.drawable.search_down_arrow_inactive);
            mDownArrow.setEnabled(false);
        } else {
            if (currentPos == 0) {
                mUpArrow.setImageResource(R.drawable.search_up_arrow_inactive);
                mUpArrow.setEnabled(false);
                mDownArrow.setImageResource(R.drawable.search_down_arrow_active);
                mDownArrow.setEnabled(true);
            } else if (currentPos == (msgIdsCcount - 1)) {
                mUpArrow.setEnabled(true);
                mUpArrow.setImageResource(R.drawable.search_up_arrow_active);
                mDownArrow.setImageResource(R.drawable.search_down_arrow_inactive);
                mDownArrow.setEnabled(false);
            } else {
                mUpArrow.setEnabled(true);
                mUpArrow.setImageResource(R.drawable.search_up_arrow_active);
                mDownArrow.setEnabled(true);
                mDownArrow.setImageResource(R.drawable.search_down_arrow_active);
            }

        }
    }

    /**
     * This Method
     * 
     * @param mHighLightedMsgId2
     */
    private void refreshListView(long msgId) {
        Pattern highlight = null;
        highlight = searchString == null ? null : Pattern.compile("\\b" + Pattern.quote(searchString),
                Pattern.CASE_INSENSITIVE);
        MessageListAdapter oldAdapter = mMsgListAdapter;
        // Initialize the list adapter with a null cursor.
        mMsgListAdapter = new MessageListAdapter(mActivity, null, mMsgListView, highlight, mConversation,
                msgId, !isMultipaneUI, textEntryStateProvider);
        mMsgListAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        mMsgListAdapter.setMsgListItemHandler(mMessageListItemHandler);
        mMsgListView.setAdapter(mMsgListAdapter);
        mMsgListView.setItemsCanFocus(false);
        mMsgListView.setVisibility(View.VISIBLE);
        mMsgListView.setFastScrollEnabled(true);
        // on new item added, it will scroll to bottom
        mMsgListView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        if (mBackgroundQueryHandler != null) {
            // change the last message count for new adapter
            mBackgroundQueryHandler.lastMessageCount = 0;
        }

        if (oldAdapter != null) {
            oldAdapter.shutdown();
        }

    }

    private void loadDraft() {
        if (mWorkingMessage.isWorthSaving()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.warn(getClass(), "loadDraft() called with non-empty working message");
            }
            return;
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".loadDraft: call WorkingMessage.loadDraft");
        }

        mWorkingMessage = WorkingMessage.loadDraft(this, mConversation);
    }

    private void saveDraft() {
        if (recipientCount() == 0 || mWorkingMessage.isDiscarded()) {
            return;
        }

        if (!mWaitingForSubActivity && !mWorkingMessage.isWorthSaving()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(this + ".saveDraft: not worth saving, discard WorkingMessage and bail");
            }
            mWorkingMessage.discard();
            return;
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".saveDraft: call WorkingMessage.saveDraft");
        }

        mWorkingMessage.saveDraft();

        if (mToastForDraftSave) {
            Toast.makeText(mActivity, R.string.message_saved_as_draft, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPreparedForSending() {
        int recipientCount = recipientCount();

        return recipientCount > 0 && recipientCount <= MmsConfig.getRecipientLimit()
                && (mWorkingMessage.hasAttachment() || mWorkingMessage.hasText());
    }

    private int recipientCount() {
        int recipientCount;

        // To avoid creating a bunch of invalid Contacts when the recipients
        // editor is in flux, we keep the recipients list empty. So if the
        // recipients editor is showing, see if there is anything in it rather
        // than consulting the empty recipient list.
        if (isRecipientsEditorVisible()) {
            recipientCount = mRecipientEditor.getRecipientCount();
        } else {
            recipientCount = getRecipients().size();
        }
        return recipientCount;
    }

    public void showOfflineDialog() {
        final Dialog dialog = new AppAlignedDialog(mActivity, 0, R.string.backup_alert_title,
                R.string.vma_offline_text);
        dialog.setCancelable(true);
        Button offlineButton = (Button) dialog.findViewById(R.id.positive_button);
        offlineButton.setText(R.string.yes);
        offlineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        Button eraseButton = (Button) dialog.findViewById(R.id.negative_button);
        eraseButton.setVisibility(View.GONE);

        dialog.show();
    }

    private void sendMessage(boolean bCheckEcmMode) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".sendMessage");
        }

        lastTextEntryTime = 0;

        if (VZUris.isTabletDevice()) {
            boolean isTabletInOfflineMode = ApplicationSettings.getInstance().getBooleanSetting(
                    AppSettings.KEY_VMA_TAB_OFFLINE_MODE, false);

            if (isTabletInOfflineMode) {
                ApplicationSettings.getInstance().put(AppSettings.KEY_CANT_SEND_MESSAGE, true);
                showOfflineDialog();
                return;
            }
        }

        DraftCache.getInstance().refresh();
        if (bCheckEcmMode) {
            // TODO: expose this in telephony layer for SDK build
            String inEcm = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE);
            if (Boolean.parseBoolean(inEcm)) {
                try {
                    startActivityForResult(new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS,
                            null), REQUEST_CODE_ECM_EXIT_DIALOG);
                    return;
                } catch (ActivityNotFoundException e) {
                    // continue to send message
                    Logger.error(getClass(), "Cannot find EmergencyCallbackModeExitDialog", e);
                }
            }
        }

        // notify adapter
        final MessageListAdapter adapter = mMsgListAdapter;
        if (adapter != null) {
            adapter.setCursorChanging(true);

            // reset the group mode
            if (mResetAdapterGroupMode) {
                adapter.setGroupMode(null);
                mResetAdapterGroupMode = false;
            }
        }

        // check if this is the first message sent on a new session
        if (isRecipientsEditorVisible() && !mSentMessage) {
            mFirstMessage = true;
        }

        if (!mSendingMessage) {
            // send can change the recipients. Make sure we remove the listeners
            // first and then add
            // them back once the recipient list has settled.
            removeRecipientsListeners();
            mWorkingMessage.setText(MessagingPreferenceActivity.getSignature(mActivity,
                    mWorkingMessage.getText()));
            mWorkingMessage.send();
            mSentMessage = true;
            mSendingMessage = true;
            addRecipientsListeners();
            mMsgListView.setPosition(AdapterView.INVALID_POSITION);
        }
        // But bail out if we are supposed to exit after the message is sent.
        if (mExitOnSent) {
            mComposeMsgListener.finished();
        }
    }

    private void resetMessage() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".resetMessage");
        }

        // Make the attachment editor hide its view.
        // mAttachmentEditor.hideView();
        // Now we need to hide the list view and clear the model list
        mAttachmentAdapter.clearItems();
        mAttachmentListView.setAdapter(mAttachmentAdapter);
        mAttachlayout.setVisibility(View.GONE);
        // Hide the subject editor.
        showSubjectEditor(false);

        // Focus to the text editor.
        mTextEditor.requestFocus();

        // We have to remove the text change listener while the text editor gets
        // cleared and
        // we subsequently turn the message back into SMS. When the listener is
        // listening while
        // doing the clearing, it's fighting to update its counts and itself try
        // and turn
        // the message one way or the other.
        mTextEditor.removeTextChangedListener(mTextEditorWatcher);

        // Clear the text box.
        TextKeyListener.clear(mTextEditor.getText());

        mWorkingMessage = WorkingMessage.createEmpty(this);
        mWorkingMessage.setConversation(mConversation);
        setGroupMode(false, false);

        hideRecipientEditor();
        showMessageHeader();
        drawBottomPanel();

        updateSendButtonState();

        // Our changes are done. Let the listener respond to text changes once
        // again.
        mTextEditor.addTextChangedListener(mTextEditorWatcher);

        // Close the soft on-screen keyboard if we're in landscape mode so the
        // user can see the conversation.
        if (mIsLandscape && !MmsConfig.isTabletDevice()) {
            InputMethodManager inputMethodManager = (InputMethodManager) mActivity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            inputMethodManager.hideSoftInputFromWindow(mTextEditor.getWindowToken(), 0);
        }

        mLastRecipientCount = 0;
        mSendingMessage = false;
    }

    private void updateSendButtonState() {
        final boolean enable = isPreparedForSending();

        mSendButton.setEnabled(enable);
        mSendButton.setFocusable(enable);

        if (mSearchNavigatePanel.getVisibility() == View.VISIBLE) {
            mSearchDone.setEnabled(true);
            mSendButton.setFocusable(true);
        }
    }

    private long getMessageDate(Uri uri) {
        if (uri != null) {
            Cursor cursor = SqliteWrapper.query(mActivity, mContentResolver, uri, new String[] { Mms.DATE },
                    null, null, null);
            if (cursor != null) {
                try {
                    if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                        return cursor.getLong(0) * 1000L;
                    }
                } finally {
                    cursor.close();
                }
            }
        }
        return NO_DATE_FOR_DIALOG;
    }

    private void initActivityState(Bundle bundle, Intent intent) {
        if (bundle != null) {
            String recipients = bundle.getString("recipients");
            mConversation = Conversation.get(mActivity,
                    ContactList.getByNumbers(recipients, false /* don't block */, true /* replace number */),
                    false);
            addRecipientsListeners();
            mExitOnSent = bundle.getBoolean(ComposeMessageActivity.EXIT_ON_SENT, false);
            mWorkingMessage.readStateFromBundle(bundle);
            return;
        }

        // If a prepopulated address sent then use it
        String prePopulatedAddresses = intent.getStringExtra(ComposeMessageActivity.PREPOPULATED_ADDRESS);
        // If we have been passed a thread_id, use that to find our
        // conversation.
        long threadId = intent.getLongExtra("thread_id", 0L);
        if (threadId > 0) {
            mConversation = Conversation.get(mActivity, threadId, false);
        }
        // Handle pre population of the recipient editor
        else if (prePopulatedAddresses != null) {
            ContactList list = ContactList.getByNumbers(prePopulatedAddresses, false, true);
            mConversation = Conversation.createNew(mActivity);
            mConversation.setRecipients(list);
            mTextEditor.requestFocus();
        } else {
            Uri intentData = intent.getData();
            boolean loadAddressData = true;

            if (intentData != null) {
                // try to get a conversation based on the data URI passed to our
                // intent.
                mConversation = Conversation.get(mActivity, intentData, false);
                if (mConversation.getThreadId() != 0 || mConversation.getRecipients().size() > 0) {
                    loadAddressData = false;
                }
            }

            if (loadAddressData) {
                // special intent extra parameter to specify the address
                String address = intent.getStringExtra("address");
                if (!TextUtils.isEmpty(address)) {
                    mConversation = Conversation.get(mActivity,
                            ContactList.getByNumbers(address, false /* don't block */, true /*
                                                                                             * replace number
                                                                                             */), false);
                } else {
                    mConversation = Conversation.createNew(mActivity);
                }
            }
        }
        addRecipientsListeners();

        mExitOnSent = intent.getBooleanExtra(ComposeMessageActivity.EXIT_ON_SENT, false);
        mWorkingMessage.setText(intent.getStringExtra("sms_body"));
        mWorkingMessage.setSubject(intent.getStringExtra("subject"), false);
    }

    private final OnContentChangedListener mDataSetChangedListener = new OnContentChangedListener() {
        public void onContentChanged(MessageListAdapter adapter) {
            startMsgListQuery();
        }

        public long getThreadId() {
            long threadId = 0l;

            if (mConversation != null) {
                threadId = mConversation.getThreadId();
            }

            return threadId;
        }
    };

    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        private int lastMessageCount;
        private int lastQuery;

        public BackgroundQueryHandler(Handler mHandler, ContentResolver contentResolver) {
            super(contentResolver);
        }

        public void startMessageListQuery(Uri uri) {
            synchronized (this) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(ComposeMessageFragment.this + ".startMessageListQuery: uri = " + uri
                            + ", active = " + mActive + ", cookie = " + (lastQuery + 1));
                    queryStart = SystemClock.uptimeMillis();
                }
                // cancel any pending requests and start the query with a unique
                // cookie
                cancelOperation(MESSAGE_LIST_QUERY_TOKEN);

                if (mActive) {
                    startQuery(MESSAGE_LIST_QUERY_TOKEN, new Integer(++lastQuery), uri, PROJECTION, null,
                            null, null);
                }
            }
        }

        @Override
        public void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(ComposeMessageFragment.this + ".onQueryComplete: token = " + token
                        + ", cookie = " + cookie + ", count = " + (cursor == null ? -1 : cursor.getCount())
                        + ", active = " + mActive + ", time = " + (SystemClock.uptimeMillis() - queryStart)
                        + " ms");
            }
            switch (token) {
            case MESSAGE_LIST_QUERY_TOKEN:
                if (cursor == null) {
                    Logger.error(getClass(), "Null cursor");
                    return;
                }

                final boolean firstQuery = !mQueryCompleted;
                if (firstQuery) {
                    mQueryCompleted = true;
                }

                final int count = cursor.getCount();

                if (lastMessageCount > 0 && count == 0) {
                    // the message was deleted in the paired device so discard
                    // it
                    mWorkingMessage.discard();
                    if (mConversation != null) {
                        DraftCache.getInstance().setDraftState(mConversation.getThreadId(), false);
                        mConversation.clearThreadId();
                    }

                    if (isMultipaneUI) {
                        onDeleteSelectAnother();
                    } else {
                        mComposeMsgListener.finished();
                    }
                    cursor.close();
                    return;
                }

                // ignore stale query results
                synchronized (this) {
                    if (!mActive || (Integer) cookie != lastQuery) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(ComposeMessageFragment.this
                                    + ".onQueryComplete: ignoring stale query");
                        }
                        cursor.close();
                        return;
                    }
                }

                // if a message to display has been specified then try to find
                // it in the cursor
                final long targetMsgId = mHighLightedMsgId;
                if (targetMsgId != 0) {
                    // mHighLightedMsgId = 0;
                    currentPos = 0;
                    rowIdsList = null;

                    if (msgIdsList != null) {
                        rowIdsList = new ArrayList<Integer>(msgIdsList.size());
                        ArrayList<Long> updatedMsgIdList = new ArrayList<Long>(msgIdsList.size());

                        cursor.moveToPosition(-1);
                        while (cursor.moveToNext()) {
                            long msgId = cursor.getLong(COLUMN_ID);
                            if (cursor.getString(COLUMN_MSG_TYPE).charAt(0) == 'm') {
                                msgId = -msgId;
                            }
                            int i = 0;
                            boolean remove = false;
                            for (long id : msgIdsList) {
                                if (msgId == id) {
                                    int pos = cursor.getPosition();
                                    updatedMsgIdList.add(id);
                                    rowIdsList.add(pos);

                                    if (targetMsgId == msgId) {
                                        currentPos = updatedMsgIdList.size() - 1;
                                    }
                                    remove = true;
                                    break;
                                }
                                i++;
                            }
                            if (remove) {
                                msgIdsList.remove(i);
                            }
                        }
                        msgIdsList = updatedMsgIdList;
                        msgIdsCcount = updatedMsgIdList.size();
                    }

                    if (msgIdsCcount > 0) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("PIX: msgIdsList::" + msgIdsList.toString());
                            Logger.debug("PIX: rowIdsList::" + rowIdsList.toString());
                        }

                        int pos = rowIdsList.get(currentPos);
                        if (pos == count - 1) {
                            pos = ListView.INVALID_POSITION;
                        }
                        mMsgListAdapter.highLightMsg(msgIdsList.get(currentPos));
                        mMsgListView.setPosition(pos);

                        initSearchMode();
                        initSearchNavigation();
                    } else {
                        closeSearchPanel();
                    }
                }

                // if we receive an new message scroll to the end of the list
                if (/* count > lastMessageCount && */lastMessageCount != 0) {
                    if (Conversation.hasUnreadMessage(cursor)) {
                        mMsgListView.setPosition(AdapterView.INVALID_POSITION);
                    }
                }
                changeCursor(cursor, false);

                // Once we have completed the query for the message history, if
                // there is nothing in the cursor and we are not composing a new
                // message, we must be editing a draft in a new conversation
                // (unless mSentMessage is true).
                // Show the recipients editor to give the user a chance to add
                // more people before the conversation begins.

                if (count == 0 && !isRecipientsEditorVisible() && !mSentMessage) {
                    hideMessageHeader();
                    initRecipientsEditor();
                    showRecipientEditor();
                    setGroupMode(false, true);
                } else if (firstQuery && !mGroupModeChanged) {
                    // first query and user hasn't set group mode: init it from
                    // thread
                    initGroupMode(true);
                }

                // FIXME: freshing layout changes the focused view to an
                // unexpected
                // one, set it back to TextEditor forcely.
                mTextEditor.requestFocus();
                mConversation.blockMarkAsRead(false);
                // ReadReport 3.1
                if (!mPaused) {
                    MMSReadReport.handleReadReport(mActivity, mConversation, mConversation.getThreadId(),
                            PduHeaders.READ_STATUS_READ);
                }
                // if the message count has increased then mark the conversation
                // as read
                // if (count > lastMessageCount) {
                // commented out the check for the lastMessageCount as for incoming MMS after adding the mms
                // we
                // delete the old notification indicator so the count remains same
                if (mPaused) {
                    mPendingMarkAsread = true;
                } else {
                    mConversation.markAsRead(true);
                }
                // }
                lastMessageCount = count;
                mQueryCompleted = true;
                return;

            case ConversationListFragment.HAVE_LOCKED_MESSAGES_TOKEN:
                long threadId = (Long) cookie;
                ConversationListFragment.confirmDeleteThreadDialog(

                new DeleteThreadListener(threadId, mBackgroundQueryHandler, mActivity), threadId == -1,
                        cursor != null && cursor.getCount() > 0, mActivity);
                break;
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            Uri uri = null;
            switch (token) {
            case DELETE_SPAM_REPORT_TOKEN:
            case DELETE_MESSAGE_TOKEN:
                if (cookie != null) {
                    uri = (Uri) cookie;
                }
            case DELETE_CONVERSATION_TOKEN:
                // Update the notification for new messages since they
                // may be deleted.
                if (convDeletionDialog.isShowing() && mActive) {
                    convDeletionDialog.dismiss();
                }
                MessagingNotification.nonBlockingUpdateNewMessageIndicator(mActivity, false, false, null);
                // Update the notification for failed messages since they
                // may be deleted.
                updateSendFailedNotification();
                break;
            }

            if (uri != null) {
                long id = ContentUris.parseId(uri);
                int msgType = uri.getAuthority().equalsIgnoreCase(VZUris.getMmsUri().getAuthority()) ? ConversationDataObserver.MSG_TYPE_MMS
                        : ConversationDataObserver.MSG_TYPE_SMS;

                ConversationDataObserver.onMessageDeleted(mConversation.getThreadId(), id, msgType,
                        ConversationDataObserver.MSG_SRC_TELEPHONY);
            }

            if (!mActive) {
                return;
            }
            // If we're deleting the whole conversation or if we are deleting
            // the only message in
            // conversation, throw away our current working message and bail.
            if (token == DELETE_CONVERSATION_TOKEN
                    || (token == DELETE_MESSAGE_TOKEN && lastMessageCount == 1)) {
                mWorkingMessage.discard();
                if (isMultipaneUI) {
                    onDeleteSelectAnother();
                } else {
                    mComposeMsgListener.finished();
                }
            }
        }
    }

    void onDeleteSelectAnother() {
        if (lastDeletedThreadID > 0) {
            mComposeMsgListener.onDeleteGoToNext(lastDeletedThreadID);
            lastDeletedThreadID = -5l;
        } else {
            mComposeMsgListener.launchComposeView(ConversationListActivity.getIntentFromParent(mActivity, 0,
                    false));
        }
    }

    public class DeleteThreadListener extends DeleteThreadClickListener {
        public DeleteThreadListener(long threadId, AsyncQueryHandler handler, Context context) {
            super(threadId, handler, context);
        }

        public void onClick(View v) {
            dialog.dismiss();

            convDeletionDialog.setMessage(mContext.getString(R.string.deleting));
            convDeletionDialog.setCancelable(false);
            convDeletionDialog.show();
            // broadcastMessageSyncIntent(null, mThreadId);
            lastDeletedThreadID = mThreadId;
            Conversation.startDelete(mContext, mHandler, DELETE_CONVERSATION_TOKEN, mDeleteLockedMessages,
                    mThreadId);
            DraftCache.getInstance().setDraftState(mThreadId, false);

        }
    }

    private void showSmileyDialog() {
        int[] icons = SmileyParser.DEFAULT_SMILEY_RES_IDS;
        String[] names = getResources().getStringArray(SmileyParser.DEFAULT_SMILEY_NAMES);
        final String[] texts = getResources().getStringArray(SmileyParser.DEFAULT_SMILEY_TEXTS);

        final int N = names.length;

        final List<Map<String, ?>> entries = new ArrayList<Map<String, ?>>();
        for (int i = 0; i < N; i++) {
            // We might have different ASCII for the same icon, skip it if
            // the icon is already added.
            boolean added = false;
            for (int j = 0; j < i; j++) {
                if (icons[i] == icons[j]) {
                    added = true;
                    break;
                }
            }
            if (!added) {
                HashMap<String, Object> entry = new HashMap<String, Object>();

                entry.put("icon", icons[i]);
                entry.put("name", names[i]);
                entry.put("text", texts[i]);

                entries.add(entry);
            }
        }

        QuickAction smileyAction = new QuickAction(mActivity);
        smileyAction.setTitle(getString(R.string.menu_insert_smiley));
        for (int i = 0; i < entries.size(); i++) {
            HashMap<String, ?> entry = (HashMap<String, ?>) entries.get(i);
            smileyAction.addActionItem(
                    new ActionItem(0, (String) entry.get("name"), (Integer) entry.get("icon"), (String) entry
                            .get("text")), true);
        }

        smileyAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                HashMap<String, ?> item = (HashMap<String, ?>) entries.get(pos);
                // get the text and insert the item at the current
                // cursor position
                int start = mTextEditor.getSelectionStart();
                mTextEditor.getText().insert(start, (String) item.get("text"));
            }
        });

        smileyAction.show(null, getView(), mIsKeyboardOpen);
    }

    private void changeCursor(Cursor cursor, boolean setPosition) {
        if (setPosition) {
            mMsgListView.setPosition();
        }
        mMsgListAdapter.changeCursor(cursor);
    }

    public Conversation getConversation() {
        return mConversation;
    }

    @Override
    public void onUpdate(final Contact updated, final Object cookie) {
        final ContactList recipients = isRecipientsEditorVisible() ? mRecipientEditor
                .constructContactsFromInput(false) : getRecipients();

        // update only when relavent contact has changed
        if (!containsContact(recipients, updated)) {
            return;
        }

        // Using an existing handler for the post, rather than conjuring up a
        // new one.
        mMessageListItemHandler.post(new Runnable() {
            public void run() {
                ContactList recipients = isRecipientsEditorVisible() ? mRecipientEditor
                        .constructContactsFromInput(false) : getRecipients();

                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(ComposeMessageFragment.this + ".onUpdate contact updated: " + updated
                            + ", recipients: " + recipients);
                }

                updateTitle(recipients);

                // The contact information for one (or more) of the recipients
                // has changed.
                // Rebuild the message list only when we are in groupmode so
                // each MessageItem will get the
                // last contact info.
                if (recipients.size() > 1) {
                    ComposeMessageFragment.this.mMsgListView.setPosition();
                    ComposeMessageFragment.this.mMsgListAdapter.notifyDataSetChanged();
                }

                if (mRecipientEditor != null) {
                    mRecipientEditor.populate(recipients, mActivity);
                    updateRecipientsState();
                }
            }
        });
    }

    private boolean containsContact(ContactList recipients, Contact updated) {
        for (Contact contact : recipients) {
            if (updated == contact) {
                return true;
            }
        }
        return false;
    }

    private void addRecipientsListeners() {
        Contact.addListener(this);
    }

    private void removeRecipientsListeners() {
        Contact.removeListener(this);
    }

    public void onNewIntent(Intent intent) {
        Conversation conversation = null;
        mSentMessage = false;
        mActivity.setIntent(intent);
        // If we have been passed a thread_id, use that to find our
        // conversation.
        long threadId = intent.getLongExtra("thread_id", 0L);
        Uri intentUri = intent.getData();

        boolean sameThread = false;
        if (threadId > 0) {
            conversation = Conversation.get(mActivity, threadId, false);
        } else {
            if (mConversation.getThreadId() == 0) {
                // We've got a draft. See if the new intent's recipient is the
                // same as
                // the draft's recipient. First make sure the working recipients
                // are synched
                // to the conversation.
                mWorkingMessage.syncWorkingRecipients();
                sameThread = mConversation.sameRecipient(intentUri);
            }
            if (!sameThread) {
                // Otherwise, try to get a conversation based on the
                // data URI passed to our intent.
                conversation = Conversation.get(mActivity, intentUri, false);
            }
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onNewIntent: new conversation = " + conversation + ", mConversation = "
                    + mConversation + ", intent = " + Util.dumpIntent(intent, "  "));
        }

        if (conversation != null) {
            // Don't let any markAsRead DB updates occur before we've loaded the
            // messages for
            // the thread.
            conversation.blockMarkAsRead(true);

            // this is probably paranoia to compare both thread_ids and
            // recipient lists,
            // but we want to make double sure because this is a last minute fix
            // for Froyo
            // and the previous code checked thread ids only.
            // (we cannot just compare thread ids because there is a case where
            // mConversation
            // has a stale/obsolete thread id (=1) that could collide against
            // the new thread_id(=1),
            // even though the recipient lists are different)
            sameThread = (conversation.getThreadId() == mConversation.getThreadId() && conversation
                    .equals(mConversation));

        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(this + ".onNewIntent: sameThread = " + sameThread);
        }
        mQueryCompleted = false;
        mGroupModeChanged = false;

        if (!sameThread) {
            saveDraft(); // if we've got a draft, save it first
            initialize(null);
            loadMessageContent();
        }

        // init group delivery mode
        initGroupMode(false);

    }

    private void showAttachMenu() {

        mAttachButton.setEnabled(false);
        final QuickAction attachMenu = new QuickAction(mActivity);
        attachMenu.setTitle(R.string.attach);

        ActionItem actionItem = new ActionItem(MENU_ATTACH_EMOJI, R.string.menu_insert_emoji,
                R.drawable.attach_emoji_icon);
        attachMenu.addActionItem(actionItem);

        actionItem = new ActionItem(MENU_ATTACH_PICT, R.string.photo, R.drawable.ico_dlg_photo);
        attachMenu.addActionItem(actionItem);

        actionItem = new ActionItem(MENU_ATTACH_VIDEO, R.string.video, R.drawable.ico_dlg_video);
        attachMenu.addActionItem(actionItem);

        // place holder for location
        if (!OEM.isNbiLocationDisabled) {
            actionItem = new ActionItem(MENU_ATTACH_PLACES, R.string.location, R.drawable.ico_dlg_location);
            attachMenu.addActionItem(actionItem);
        }

        // actionItem = new ActionItem(MENU_ATTACH_AUDIO, R.string.audio,
        // R.drawable.ico_dlg_audio);
        // mAttachMenu.addActionItem(actionItem);

        actionItem = new ActionItem(MENU_ATTACH_MORE, getString(R.string.more) + "\u2026",
                R.drawable.ico_dlg_more);
        attachMenu.addActionItem(actionItem);

        attachMenu.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                mActivity.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                switch (actionId) {
                case MENU_ATTACH_AUDIO:
                    showAddAttachmentDialog(false, MODE_AUDIO);
                    break;

                case MENU_ATTACH_PICT:
                    showAddAttachmentDialog(false, MODE_IMAGE);
                    break;

                case MENU_ATTACH_EMOJI:
                    mAttachButton.setImageResource(R.drawable.android_attach_keyboard);
                    showEmojisPanel();
                    break;

                case MENU_ATTACH_PLACES:
                    checkWifiProbePreferences();
                    break;

                case MENU_ATTACH_VIDEO:
                    showAddAttachmentDialog(false, MODE_VIDEO);
                    break;
                case MENU_ATTACH_MORE:
                    showAddAttachmentDialog(false, MODE_ALL);
                    break;
                }
            }
        });
        attachMenu.setOnDismissListener(new QuickAction.OnDismissListener() {
            @Override
            public void onDismiss() {
                mAttachButton.setEnabled(true);
                // TODO Auto-generated method stub
            }
        });

        attachMenu.show(mAttachButton, getView(), mIsKeyboardOpen);
    }

    private void checkWifiProbePreferences() {
        SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        AppAlignedDialog buildOld = null;
        Button saveButton = null;
        Button cancelButton = null;

        if (!myPrefs.contains(AdvancePreferenceActivity.WIFI_PROBE)) {
            buildOld = new AppAlignedDialog(mActivity, R.drawable.dialog_alert, R.string.wifi_probe_title,
                    R.string.wifi_probe_dialog);
            saveButton = (Button) buildOld.findViewById(R.id.positive_button);
            cancelButton = (Button) buildOld.findViewById(R.id.negative_button);
        }
        final AppAlignedDialog temp = buildOld;
        if (temp != null) {
            saveButton.setText(R.string.wifi_probe_accept);
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences.Editor myEdit = PreferenceManager.getDefaultSharedPreferences(
                            getContext()).edit();
                    myEdit.putBoolean(AdvancePreferenceActivity.WIFI_PROBE, true);
                    myEdit.commit();
                    temp.cancel();

                    Intent i = new Intent(getActivity(), AddLocationActivity.class);
                    mActivity.startActivityForResult(i, REQUEST_CODE_ADD_LOCATION);

                }
            });
            cancelButton.setText(R.string.wifi_probe_decline);
            cancelButton.setVisibility(View.VISIBLE);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences.Editor myEdit = PreferenceManager.getDefaultSharedPreferences(
                            getContext()).edit();
                    myEdit.putBoolean(AdvancePreferenceActivity.WIFI_PROBE, false);
                    myEdit.commit();
                    temp.cancel();

                    Intent i = new Intent(getActivity(), AddLocationActivity.class);
                    mActivity.startActivityForResult(i, REQUEST_CODE_ADD_LOCATION);

                }
            });
            temp.show();
        } else {

            Intent i = new Intent(getActivity(), AddLocationActivity.class);
            mActivity.startActivityForResult(i, REQUEST_CODE_ADD_LOCATION);

        }
    }

    /*
     * Added a focus change listener to update the contact if the user changes its focus from recipient Editor
     */
    private OnFocusChangeListener mOnFocusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus && !mRecipListView.hasFocus()) {
                EditText mRecipent = (EditText) mRecipientEditor.getEditControl();
                String searchString = mRecipent.getText().toString().trim();
                if (searchString.length() == 0) {
                    mRecipent.setText("");
                    return;
                }

                if (mRecipent != null && mRecipent.length() != 0 && searchString.length() > 0) {
                    autoSelect(searchString);
                }
            }
        }
    };

    private TextWatcher mContactWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(final Editable searchString) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mSearchContTask != null) {
                mSearchContTask.cancel(true);

            }

            // leading and trailing spaces can be useful in searching so we
            // don't
            // trim them from the actual search string
            boolean isEmptyContact = false;
            final String search = s.toString();
            if (search.indexOf(ContactSearchTask.CONTACT_SEPARATOR) != -1) {
                String searchStr = (search.substring(0, search.indexOf(ContactSearchTask.CONTACT_SEPARATOR)))
                        .trim();
                if (searchStr.length() == 0) {
                    isEmptyContact = true;
                    EditText mRecipent = (EditText) mRecipientEditor.getEditControl();
                    mRecipent.setText("");
                }
            }

            if (search.trim().length() > 0 && !isEmptyContact) {
                mSearchContTask = new ContactSearchTask(mActivity, mContactSearchHandler);
                mSearchContTask.execute(search);
            } else {
                mRecipListView.setSelection(ListView.INVALID_POSITION);
                mRecipListView.setVisibility(View.GONE);
            }

            updateSendButtonState();
        }
    };

    private final ContactSearchListener mContactSearchHandler = new ContactSearchListener() {
        public void updateContactList(Cursor cur, String searchString, boolean isAutoTerminate) {
            int count = cur.getCount();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "updateContactList: cur = " + cur + ", count = " + count);
            }
            if (count > 0) {
                updateContactHintsList(cur, searchString, isAutoTerminate);
            } else {
                cur.close();
                mRecipListView.setSelection(ListView.INVALID_POSITION);
                mRecipListView.setVisibility(View.GONE);
            }
        }
    };

    private void updateContactHintsList(Cursor cur, String searchString, boolean isTerminated) {
        synchronized (mCursorLock) {
            // close the previous cursor
            if (mRecipCursor != null) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "updateContactHintsList: closing " + mRecipCursor);
                }
                mRecipCursor.close();
            }
            mRecipCursor = cur;

            if (mRecipAdapter == null) {
                mRecipAdapter = new ContactsCursorAdapter(mActivity, R.layout.recip_contacts_item, cur,
                        new String[] {}, new int[] {}, mRecipListView);
                mRecipAdapter.setSearchString(searchString);
                mRecipListView.setAdapter(mRecipAdapter);
            } else {
                mRecipAdapter.setSearchString(searchString);
                mRecipAdapter.changeCursor(cur);
                mRecipListView.setSelection(ListView.INVALID_POSITION);
                mRecipAdapter.notifyDataSetChanged();
            }
        }

        // If isTerminated = true. The user has enter a Contact separator "," to
        // add multiple recipient
        if (isTerminated) {
            autoSelect(searchString);
            mRecipListView.setSelection(ListView.INVALID_POSITION);
            mRecipListView.setVisibility(View.GONE);
        } else {
            mRecipListView.setVisibility(View.VISIBLE);
        }
    }

    public void bringFocustoTextEditor() {
        mTextEditor.requestFocus();
    }

    public void autoSelect(String searchString) {
        String displayName = searchString;
        String key = searchString;
        long contactID = -1;
        String label = null;
        synchronized (mCursorLock) {
            if (mRecipCursor != null) {
                try {
                    displayName = mRecipCursor.getString(ContactSearchTask.CONTACT_DISPLAY_NAME);
                    key = mRecipCursor.getString(ContactSearchTask.CONTACT_DATA);
                    contactID = Long.valueOf(mRecipCursor.getString(ContactSearchTask.CONTACT_ID));

                    if (contactID != -1) {
                        int TypeValue = mRecipCursor.getInt(ContactSearchTask.PHONE_TYPE);
                        String customLabel = mRecipCursor.getString(ContactSearchTask.PHONE_LABEL);
                        label = Phone.getTypeLabel(mActivity.getResources(), TypeValue, customLabel)
                                .toString();
                    }
                } catch (CursorIndexOutOfBoundsException cio) {
                    displayName = searchString;
                    key = searchString;
                } catch (Exception e) {
                    ;
                }
            }
        }
        // if the display name is same as Contact suggested go with the first
        // suggestion else go with the contact typed.
        String name = searchString;
        String contKey = searchString;

        if (PhoneNumberUtils.compare(searchString, key)) {
            name = displayName;
            contKey = key;
        }

        RecipContact contact = new RecipContact(name, contKey, contactID, label);
        mRecipientEditor.addContact(contact, mActivity, true);
        // mRecipientEditor.requestFocus();
        Util.showKeyboard(mActivity, mRecipientEditor.getEditControl());
        updateRecipientsState();

    }

    public boolean isEnterKeyHandled(int keyCode, KeyEvent event) {
        if (isRecipientsEditorVisible()) {
            if (mRecipListView.getVisibility() == View.VISIBLE) {
                if (mRecipListView.getSelectedItemPosition() != ListView.INVALID_POSITION) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        String contactName = mRecipCursor.getString(ContactSearchTask.CONTACT_DISPLAY_NAME);
                        long contactId = Long.valueOf(mRecipCursor.getString(ContactSearchTask.CONTACT_ID));
                        String keyType = null;
                        String key = mRecipCursor.getString(ContactSearchTask.CONTACT_DATA);

                        if (contactId != -1) {
                            int TypeValue = mRecipCursor.getInt(ContactSearchTask.PHONE_TYPE);
                            String customLabel = mRecipCursor.getString(ContactSearchTask.PHONE_LABEL);
                            keyType = Phone.getTypeLabel(mActivity.getResources(), TypeValue, customLabel)
                                    .toString();
                        }

                        RecipContact contact = new RecipContact(contactName, key, contactId, keyType);
                        mRecipientEditor.addContact(contact, mActivity, true);
                        Util.showKeyboard(mActivity, mRecipientEditor.getEditControl());

                        updateRecipientsState();
                        mRecipListView.setSelection(ListView.INVALID_POSITION);
                        mRecipAdapter.changeCursor(null);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void updateState() {
        updateRecipientsState();
    }

    @Override
    public boolean isMMS() {
        if (mWorkingMessage != null) {
            return mWorkingMessage.requiresMms();
        }

        return true;
    }

    @Override
    public void nativeIntentCalled() {
        mIsNativeIntentCalled = true;
    }

    @Override
    public void notifyInvalidRecip(RecipContact recip) {
        Util.forceHideKeyboard(mActivity, mRecipientEditor);
        String msg = getResourcesString(R.string.invalid_recipient_msg, recip.getName());
        final AppAlignedDialog d = new AppAlignedDialog(mActivity, R.drawable.dialog_alert,
                R.string.invalid_recipient, msg);
        Button saveButton = (Button) d.findViewById(R.id.positive_button);
        saveButton.setText(R.string.yes);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDialog();
                d.dismiss();
            }
        });
        d.show();
    }

    private void updateRecipientsState() {
        List<String> numbers = mRecipientEditor.getNumbers();
        mWorkingMessage.setWorkingRecipients(mRecipientEditor.getNumbers());
        mWorkingMessage.setHasEmail(mRecipientEditor.containsEmail(), true);
        mWorkingMessage.updateContainsEmailToMms(numbers, mWorkingMessage.getText());

        checkRecipientCount();

        // If we have gone to zero recipients, disable send button.
        updateSendButtonState();

        updateComposeHint();
    }

    private void updateComposeHint() {
        if (recipientCount() <= 1) {
            mTextEditor.setHint(R.string.type_to_compose_text_enter_to_send);
        } else {
            mTextEditor.setHint(mGroupMms ? R.string.type_to_compose_text_group
                    : R.string.type_to_compose_text_massText);
        }
    }

    private void updateChannel() {
        final Conversation conv = mConversation;
        updateChannel(conv == null ? null : conv.getRecipients());
    }

    private void updateChannel(ContactList contactList) {
        if (recipientCount() <= 1) {
            String prefix = null;

            if (contactList != null && contactList.size() == 1) {
                Contact contact = contactList.get(0);
                prefix = contact.getPrefix();
            }
            mChannel2.setText(prefix);
            mChannel.setText(prefix);
        }
        // wait until query is complete or user has set group mode to set group
        // channel
        else if (mQueryCompleted || mGroupModeChanged) {
            mChannel.setText(mGroupMms ? R.string.channel_group_mms : R.string.channel_group_text);
            mChannel2.setText(mGroupMms ? R.string.channel_group_mms : R.string.channel_group_text);
        }
    }

    private void updateAvatarView(ContactList contactList) {
        Drawable avatarDrawable = null;

        // only single-recipient messages get an avatar shown
        if (contactList != null && contactList.size() == 1) {
            Contact contact = contactList.get(0);
            avatarDrawable = contact.getAvatar(mActivity, null);
        }
        if (avatarDrawable != null) {
            mAvatarView.setImage(avatarDrawable);
            avatarDrawable.setCallback(null);
            mAvatarView.setVisibility(View.VISIBLE);
            mAvatarView.setPadding(0, 0, 0, 0);
            avatarDrawable = null;
        } else {
            mAvatarView.setVisibility(View.GONE);
        }
    }

    /**
     * 
     * This Method start the contact picker activity and process its result
     */
    protected void addNativeContact() {
        Util.forceHideKeyboard(mActivity, mRecipientEditor.getEditControl());
        mIsNativeIntentCalled = true;
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        mActivity.startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
    }

    private void showAttachment(WorkingMessage msg) {
        if (mAttachmentListView.getVisibility() == View.GONE) {
            mAttachmentListView.setVisibility(View.VISIBLE);
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "Current Attachment Count ::", String.valueOf(msg.getSlideshow().size()));
        }
        mAttachmentAdapter.setItems(msg.getSlideshow());

        adjustAttachmentListViewHeight();

        mAttachmentListView.setAdapter(mAttachmentAdapter);
        if (mAttachmentAdapter.getCount() != 0) {
            mAttachlayout.setVisibility(View.VISIBLE);
            mMmsCountLayout.setVisibility(View.VISIBLE);
            mMmsAttachmentCount.setText("(" + mAttachmentAdapter.getCount() + ")");
        }
    }

    private void adjustAttachmentListViewHeight() {
        // set Height of attachment list view
        LinearLayout.LayoutParams params = null;
        if (mAttachmentAdapter.getCount() > 1) {
            // Bug:3417:
            if (View.VISIBLE == mGroupChooser.getVisibility() && mIsLandscape) {
                params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        (mAttachLayoutMaxHeight - mGroupChooser.getHeight() - 8));
            } else {
                params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        mAttachLayoutMaxHeight);
            }
            mAttachlayout.setLayoutParams(params);
        } else {
            params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            mAttachlayout.setLayoutParams(params);
        }
    }

    /**
     * What is the current orientation?
     */
    private boolean inPortraitMode() {
        final Configuration configuration = mActivity.getResources().getConfiguration();
        return configuration.orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * If the scrap space isn't available this function creates the directories.
     */
    private void verifyScrapSpace() {
        try {
            File dir = new File(Mms.ScrapSpace.SCRAP_DIR_PATH);
            if (!dir.exists())
                dir.mkdirs();
        } catch (SecurityException se) {
            Logger.error(se);
        }
    }

    private void setCapturedImgFileName(String path, Time now) {
        String date = String.format("%04d%02d%02d", now.year, (now.month + 1), now.monthDay);
        String time = String.format("%02d%02d%02d", now.hour, now.minute, now.second);
        mCapturedImgFileName = path + "/" + ".IMG_" + date + "_" + time + ".jpg";

        PreferenceManager.getDefaultSharedPreferences(mActivity).edit()
                .putString(PREFERENCE_FILE_PATH, mCapturedImgFileName).commit();
    }

    private String getLocationFileName() {
        return SmilHelper.LOCATION_VCARD_PREFIX + getUniqueName() + ".vcf";
    }

    private String getLocPictureName() {
        return SmilHelper.LOCATION_IMG_PREFIX + getUniqueName() + ".jpeg";
    }

    /**
     * This Method returns unique file name based on date and time
     * 
     * @return
     */
    private String getUniqueName() {
        Time now = new Time();
        now.setToNow();

        String date = String.format("%04d%02d%02d", now.year, (now.month + 1), now.monthDay);
        String time = String.format("%02d%02d%02d", now.hour, now.minute, now.second);

        return date + "_" + time;
    }

    QuickAction.OnActionItemClickListener mAttachmentClickListener = new QuickAction.OnActionItemClickListener() {
        @Override
        public void onItemClick(QuickAction source, int pos, int actionId) {

            switch (actionId) {
            case ADD_IMAGE:
                MessageUtils.selectImage(mActivity, REQUEST_CODE_ATTACH_IMAGE);
                break;

            case TAKE_PICTURE: {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                final String path = Mms.ScrapSpace.SCRAP_DIR_PATH;
                Time now = new Time();
                now.setToNow();
                setCapturedImgFileName(path, now);

                if (supportsCameraOutput()) {
                    if (OEM.isNotSupportingSaveCapturedImg) {
                        File dir = null;
                        try {
                            dir = new File(path);
                            if (dir != null && !dir.exists()) {
                                dir.mkdirs();
                            }
                        } catch (SecurityException se) {
                            Logger.error(se);
                        }

                        if (dir != null) {
                            ContentValues values = new ContentValues();
                            values.put(Media.DATE_TAKEN, now.toMillis(false));
                            values.put(Media.DATA, mCapturedImgFileName);
                            Uri uri = getActivity().getApplicationContext().getContentResolver()
                                    .insert(Media.EXTERNAL_CONTENT_URI, values);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                        } else {
                            intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    Uri.parse("file:" + mCapturedImgFileName));
                        }
                    } else {
                        verifyScrapSpace();
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse("file:" + mCapturedImgFileName));
                    }
                }

                mActivity.startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
                break;
            }

            case MENU_ATTACH_EMOJI:
                mAttachButton.setImageResource(R.drawable.android_attach_keyboard);
                showEmojisPanel();
                break;

            case ADD_VCARD: {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, REQUEST_CODE_ATTACH_VCARD);
                break;
            }

            case ADD_VIDEO:
                MessageUtils.selectVideo(mActivity, REQUEST_CODE_ATTACH_VIDEO);
                break;

            case LOCATION:
                checkWifiProbePreferences();
                break;

            case RECORD_VIDEO: {
                // Set video size limit. Subtract 1K for some text.
                long sizeLimit = MmsConfig.getMaxMessageSize() - SlideshowModel.SLIDESHOW_SLOP;
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if (OEM.isPantech910L) {
                    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 11);
                    // we had seen a problem with one tablet device which could
                    // not handle this parameter.
                    // we will need to find that and do it specifically for that
                    // device.
                } else if (!OEM.isMOTOROLAMZ617) {
                    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "Adding VIDEO_QUALITY - 0 and Duration limit");
                    }

                    // even though MMS Spec calls for 60 sec it seems that on
                    // some devices the time limit
                    // may conflict with size limit so best to avoid it. If max
                    // MMS size is not exceeded
                    // what is the problem in recording up to the size limit -
                    // saw that on LG Rev the size limit is
                    // not enforced so bringing the time limit back!
                    int durationLimit = getVideoCaptureDurationLimit();
                    intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, durationLimit);

                }
                intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, sizeLimit);

                // Extra parameter to make the Samsung devices record in Low
                // quality
                if (OEM.isSAMSUNG) {
                    intent.putExtra("mms", true);
                } else if (OEM.isLG) {
                    intent.putExtra("MMSAttach", 1);
                } else if (OEM.isHTC) {
                    intent.putExtra("showfilesize", true);
                    intent.putExtra("maxfilesize", (int) sizeLimit);
                    intent.putExtra(MediaStore.Audio.Media.EXTRA_MAX_BYTES, (int) sizeLimit);
                    intent.putExtra("RequestedFrom", "mms");
                    intent.putExtra("no3d_contents", true);
                } else if (OEM.isPantechApache) {
                    Intent pantechIntent = new Intent("pantech.SKYCamera.action.ATTACH_CONTENTS_CAMCORDER");
                    pantechIntent.putExtra("size_from_composer", sizeLimit);

                    try {
                        mActivity.startActivityForResult(pantechIntent, REQUEST_CODE_TAKE_VIDEO);
                        return;
                    } catch (ActivityNotFoundException e) {
                        Logger.error("record video activity not found try with default intent now");
                    }
                }

                mActivity.startActivityForResult(intent, REQUEST_CODE_TAKE_VIDEO);
            }
                break;

            case ADD_SOUND:
                MessageUtils.selectAudio(mActivity, REQUEST_CODE_ATTACH_SOUND);
                break;
            case ADD_RINGTONE:
                MessageUtils.selectRingtone(mActivity, REQUEST_CODE_ATTACH_RINGTONE);
                break;
            case RECORD_SOUND:
                // Bug_id 101_Attaching recorded audio. OEM specific Intent
                // calls made.
                int sizeUpTo = MmsConfig.getMaxMessageSize() - SlideshowModel.SLIDESHOW_SLOP;
                if (null != mWorkingMessage.getSlideshow()) {
                    sizeUpTo -= mWorkingMessage.getSlideshow().getCurrentMessageSize();
                }
                if (sizeUpTo > 0) {
                    MessageUtils.recordSound(mActivity, REQUEST_CODE_RECORD_SOUND, sizeUpTo);
                    // Intent intent = new Intent(mActivity, RecordAudio.class);
                    // intent.putExtra(MediaStore.Audio.Media.EXTRA_MAX_BYTES,
                    // sizeUpTo);
                    // ((Activity) mActivity).startActivityForResult(intent,
                    // REQUEST_CODE_RECORD_SOUND);
                } else {
                    Toast.makeText(getActivity(),
                            getActivity().getString(R.string.exceed_message_size_limitation),
                            Toast.LENGTH_SHORT).show();
                }
                // MessageUtils.selectAudio(mActivity,
                // REQUEST_CODE_ATTACH_SOUND);
                break;

            case ADD_SLIDESHOW:
                editSlideshow();
                break;

            default:
                break;
            }
        }
    };

    /**
     * there is no way to determine if the implementation of a gallery supports output directories. this
     * function returns true if the gallery is known to work with out extras.
     * 
     * @return True if the gallery supports output directories
     */
    private boolean supportsCameraOutput() {
        return !(OEM.isHTCEris || OEM.isHTCIncredible);
    }

    @Override
    public void onThreadChanged(long oldThreadId, long newThreadId) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("ComposeMessageFragment.onThreadChanged: oldThreadId " + oldThreadId
                    + " new threadId " + newThreadId);
        }

        if (mFirstMessage) {
            if (mMsgListAdapter != null) {
                mMsgListAdapter.updateThreadId(newThreadId);
            }
        }

        if (oldThreadId != 0) {
            if (!mFirstMessage) {
                mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        initMessageList();
                    }
                });
            }
        }
    }

    @Override
    public boolean onActionItemSelected(ActionItem item) {
        int actionId = item.getActionId();
        switch (actionId) {

        case MENU_ADD_SUBJECT:
            // Keyboard needs to be populated explicitly
            InputMethodManager imm = (InputMethodManager) mActivity
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            showSubjectEditor(true);

            return true;
        case MENU_REMOVE_SUBJECT:
            showSubjectEditor(false);
            mWorkingMessage.setSubject(null, true);
            return true;

        case MENU_ADD_ATTACHMENT:
            // Launch the add-attachment list dialog
            Util.forceHideKeyboard(getActivity(), getView());
            showAddAttachmentDialog(false, MODE_ALL);
            return true;
        case MENU_DISCARD:
            mWorkingMessage.discard();
            mComposeMsgListener.finished();
            if (isMultipaneUI) {
                // if message discarded reset the message.
                resetMessage();
                hideMessageHeader();
                showRecipientEditor();
                mComposeMsgListener.onDeleteGoToNext(0);
            }
            return true;
        case MENU_SEND:
            if (isPreparedForSending()) {
                confirmSendMessageIfNeeded();
            }
            return true;
        case MENU_SEARCH:
            startSearchActivity();
            return true;
        case MENU_DELETE_CONVERSATION:
            long threadId = mConversation.getThreadId();
            ConversationListFragment.confirmDeleteThreadDialog(new DeleteThreadListener(threadId,
                    mBackgroundQueryHandler, mActivity), threadId == -1, false, mActivity);
            return true;
        case MENU_CONVERSATION_LIST:
            exitComposeMessageActivity(new Runnable() {
                public void run() {
                    goToConversationList();
                }
            });
            return true;
        case MENU_CALL_RECIPIENT:
            dialRecipient();
            return true;
        case MENU_INSERT_SMILEY:
            showSmileyDialog();
            return true;
        case MENU_VIEW_CONTACT: {
            // View the contact for the first (and only) recipient.
            ContactList list = getRecipients();
            if (list.size() == 1 && list.get(0).existsInDatabase()) {
                Uri contactUri = list.get(0).getUri();
                Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(intent);
            }
            return true;
        }

        case MENU_ADD_ADDRESS_TO_CONTACTS:
            mAddContactIntent = (Intent) item.getTag();
            startActivityForResult(mAddContactIntent, REQUEST_CODE_ADD_CONTACT);
            return true;
        case MENU_ADD_RECIPIENT:
            addNativeContact();
            return true;
        case MENU_PREFERENCES:
            final Intent intent = new Intent(mActivity, MessagingPreferenceActivity.class);
            mIsNativeIntentCalled = true;
            mActivity.startActivityForResult(intent, REQUEST_CODE_FONT_CHANGE);
            return true;

        case MENU_VIEW_RECIPIENT:
            showAllRecipients(getConversation().getRecipients().getNumbers());
            return true;

        case MENU_EDIT_RECIPIENT:
            editRecipients();
            return true;

        case MENU_CUSTOMIZE_CONV:
            Intent itent = new Intent(getActivity(), CustomizeConActivity.class);
            getActivity().startActivity(itent);
            return true;
        case MENU_SHOW_STATUS_DETAILS:
            new MessageDetails(mActivity, mActivity.getIntent().getLongExtra("thread_id", 0L)).execute();
            return true;
        default:

            break;
        }
        return false;
    }

    @Override
    public void onPrepareActionMenu(ActionMenuBuilder menu) {
        if (isRecipientsEditorVisible() && mRecipientEditor.getEditControl().hasFocus()) {
            addMenuForRecipient(menu);
        } else {
            addMenuForMessage(menu);
        }
        if (mIsKeyboardOpen) {
            Util.hideKeyboard(mActivity, getView());
        }
    }

    public void hideQuickBrowseList() {
        if (attendeesHintListLayout != null) {
            attendeesHintListLayout.setVisibility(View.GONE);
        }
    }

    public void showQuickBrowseList() {
        if (attendeesHintListLayout != null) {
            attendeesHintListLayout.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * method will be called when user clicks on "Next" Button when user composing recipients list in
     * RecipientEditor..
     */
    public void gainNextFieldFocus() {
        if (isSubjectEditorVisible()) {
            // cursor should point at the end for "Fwd" messages.
            mSubjectTextEditor.setSelection(mSubjectTextEditor.getText().length());
            mSubjectTextEditor.requestFocus();
        } else {
            gainTextEditorFocus();
        }
    }

    private void gainTextEditorFocus() {
        mTextEditor.setSelection(mTextEditor.getText().length());
        mTextEditor.requestFocus();
    }
}
