/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since  Nov 22, 2012
 */
package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedSet;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MediaSyncHelper;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.transaction.MessagingNotification;
import com.verizon.mms.transaction.MessagingNotification.PopUpInfo;
import com.verizon.mms.ui.widget.ScrollViewGallery;
import com.verizon.mms.ui.widget.ScrollViewGallery.GalleryMoveListener;
import com.verizon.sync.ConversationDataObserver;
import com.verizon.sync.SyncManager;

/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since Nov 22, 2012
 */
@Deprecated
public class PopUpNotificationActivity extends Activity {

    // [JEGA] TODO[Nabajyothi/Ishaque] please rewrite the logic to update and
    // add a new notificaton instead of
    // sending all items in intent use the message type SMS =1 . MMS =2 ,
    // Conversation = 3 , 0 = UNKNOWN
    // extend the parcelabe in PopUpInfo or SMSMMSNotification object and remove
    // the ParcelablePopUpInfo pager

    private ScrollViewGallery gallery;
    private NotificationAdapter galleryAdapter;
    private ImageView closeButton;
    private TextView headerText;
    private static int itemsCount;
    private Context mContext;
    private static ArrayList<PopUpInfo> infos = null;
    private static ArrayList<PopUpInfo> tempInfos = null;

    private final static String MESSAGES_INFO = "messages_info";
    private int NOTIFICATION_ID = 123;
    private static final String FONT_ROBOTO_REGULAR = "fonts/roboto/Roboto-Regular.ttf";
    private NotificationManager nm;
    private Typeface robotoRegular;
    private Handler updateHandler;
    private ReadStatusReceiver statusReceiver;

    private int selectedGalleryIndex;
    private ImageView back;
    private ImageView next;
    private View mainView;
    private View header;

    protected int positionDeleted = 0;

    protected int galleryState = GalleryMoveListener.STATE_GALLERY_STOP;

    private boolean inEditMode = false; // variable that says if the user is composing a message
    private int popUpWidth;
    private int popUpHeight;
    private int pagerWidth;
    private int pagerHeight;
    private int POST_FLING_DONE = 1;
    private static boolean  isVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int theme = MessageUtils.getTheme(this);

        if (theme == R.style.Theme_Large) {
            setTheme(R.style.PopUpDialogLarge);
        } else {
            setTheme(R.style.PopUpDialogNormal);
        }

        super.onCreate(savedInstanceState);

        statusReceiver = new ReadStatusReceiver();
        IntentFilter readIntent = new IntentFilter(SyncManager.ACTION_SYNC_STATUS);
        readIntent.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("PopUp register receiver");
        }
        PopUpNotificationActivity.isVisible = true;
        registerReceiver(statusReceiver, readIntent);

        mContext = this;
        int flags = getIntent().getFlags();
        if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
            Intent intent = new Intent(mContext, ConversationListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("PopUp finishing activity - onCreate");
            }
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.popupnotification);

        // Initialize Views
        DisplayMetrics metrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        gallery = (ScrollViewGallery) findViewById(R.id.gallery);
        gallery.setGalleryMoveListener(mGalleryMoveListener);

        gallery.setCallbackDuringFling(false);

        closeButton = (ImageView) findViewById(R.id.popup_close_btn);
        headerText = (TextView) findViewById(R.id.titleText);

        back = (ImageView) findViewById(R.id.prvMsg);
        //Bug 4612 - PopUp notification Left arrow is not working.
        back.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Simulating Key Down event KEYCODE_DPAD_LEFT ");
                }
                if (mGalleryMoveListener != null) {
                    mGalleryMoveListener.onGalleryMoving(GalleryMoveListener.STATE_GALLERY_MOVE);
                }
                gallery.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
                mArrowsHandler.sendEmptyMessageDelayed(POST_FLING_DONE, 700);
            }
        });
        next = (ImageView) findViewById(R.id.nxtMsg);

        next.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Simulating Key Down event KEYCODE_DPAD_RIGHT");
                }
                if (mGalleryMoveListener != null) {
                    mGalleryMoveListener.onGalleryMoving(GalleryMoveListener.STATE_GALLERY_MOVE);
                }
                gallery.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
                mArrowsHandler.sendEmptyMessageDelayed(POST_FLING_DONE, 700);
            }
        });
        
        
        
        mainView = findViewById(R.id.id_main_view);
        header = findViewById(R.id.header);
        infos = getIntent().getParcelableArrayListExtra(MESSAGES_INFO);

        Resources res = PopUpNotificationActivity.this.getResources();
        popUpHeight = res.getDimensionPixelSize(R.dimen.popup_activity_height);
        popUpWidth = res.getDimensionPixelSize(R.dimen.popup_activity_width);
        pagerHeight = res.getDimensionPixelSize(R.dimen.popup_gallery_height);
        pagerWidth = res.getDimensionPixelSize(R.dimen.popoup_gallery_item_width);

        loadFonts();
        initFonts();

        nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        updateHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("PopUp - finish - handleMessage");
                }
                finish();
                if (nm != null) {
                    nm.cancel(NOTIFICATION_ID);
                }
                super.handleMessage(msg);
            }
        };
    }

    protected void setNotchButtonVisibility() {
        if (infos != null && galleryState == GalleryMoveListener.STATE_GALLERY_STOP) {
            if (infos.size() == 1) {
                next.setVisibility(View.GONE);
                back.setVisibility(View.GONE);
            } else if (infos.size() - 1 == selectedGalleryIndex && infos.size() - 1 > 0) {
                back.setVisibility(View.VISIBLE);
                next.setVisibility(View.GONE);
            } else if (selectedGalleryIndex > 0) {
                next.setVisibility(View.VISIBLE);
                back.setVisibility(View.VISIBLE);
            } else {
                next.setVisibility(View.VISIBLE);
                back.setVisibility(View.GONE);
            }
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("PopUp onDestroy - remove receiver");
        }
        
        unregisterReceiver(statusReceiver);

        if (galleryAdapter != null) {
            galleryAdapter.shutdown();
        }
        PopUpNotificationActivity.isVisible = false;
        super.onDestroy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onNewIntent(android.content.Intent)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        intent.putExtra("position", positionDeleted);
        setIntent(intent);
        super.onNewIntent(intent);
    }

    protected void setHeaderCount() {
        itemsCount = infos.size();
        if (itemsCount > 1) {
            headerText.setText(String.valueOf(itemsCount) + " "
                    + getString(R.string.notification_multiple_title));
            String values = headerText.getText().toString();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("itemCount" + values);
            }

        } else {
            headerText.setText(String.valueOf(itemsCount) + " " + getString(R.string.new_message));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("PopUp - resume");
        }
        
        Intent intent = getIntent();
        if (inEditMode) {
            // we are currently composing a message so keep the updated values and use it later once user
            // completes
            // composing the message
            tempInfos = intent.getParcelableArrayListExtra(MESSAGES_INFO);
        } else {
            int position = intent.getIntExtra("position", 0);
            infos = intent.getParcelableArrayListExtra(MESSAGES_INFO);
            updateView(position);
        }

        super.onResume();
    }

    protected void updateView(int position) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("PopUp.updateView");
        }
        if (infos.size() == 1) {
            back.setVisibility(View.GONE);
            next.setVisibility(View.VISIBLE);
        }

        if (position >= infos.size()) {
            position = 0;
        }

        PopUpInfo popUpInfo = infos.get(position);
        long msgId;
        if (popUpInfo != null) {
            msgId = popUpInfo.getMsgId();
            boolean isSMS = popUpInfo.isSms();
            int updateSeenCount = Conversation.markMsgAsSeen(this, Long.toString(msgId), isSMS);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("PopUp New msgId:: " + msgId + " :: seen updateCount " + updateSeenCount);
            }
            /*
             * MessagingNotification.nonBlockingUpdateNewMessageIndicator(PopUpNotificationActivity.this,
             * false, false, null, false);
             */
        }
        setHeaderCount();

        if (galleryAdapter == null || galleryAdapter.isClosed()) {
            galleryAdapter = new NotificationAdapter(PopUpNotificationActivity.this, infos, gallery);
            gallery.setAdapter(galleryAdapter);
        } else {
            galleryAdapter.updateData(infos);
        }
        gallery.setSelection(position);

        if (position == 0) {
            back.setVisibility(View.GONE);
            selectedGalleryIndex = 0;
        }

        gallery.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                PopUpInfo popUpInfo = galleryAdapter.getItemAtPosition(position);
                selectedGalleryIndex = position;

                if (galleryState == GalleryMoveListener.STATE_GALLERY_FLING) {
                    galleryState = GalleryMoveListener.STATE_GALLERY_STOP;
                }
                setNotchButtonVisibility();

                long msgId = popUpInfo.getMsgId();
                boolean isSMS = popUpInfo.isSms();

                // TODO: move to background thread
                Conversation.markMsgAsSeen(PopUpNotificationActivity.this, Long.toString(msgId), isSMS);

                /*
                 * MessagingNotification.nonBlockingUpdateNewMessageIndicator(PopUpNotificationActivity.this,
                 * false, false, null, false);
                 */
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (infos == null) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("PopUp - finish - onClick");
                    }
                    finish();
                } else {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
                    if (sp.getBoolean(MessagingPreferenceActivity.MARK_AS_READ, false)) {
                        markAsSeenAndRead();
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("PopUp - finish - onClick2");
                        }
                        finish();
                    }
                }
            }
        });

        setNotchButtonVisibility();
    }

    private void markAsSeenAndRead() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("PopUp - marAsSeenAndRead");
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateHandler.sendMessage(new Message());
                for (PopUpInfo info : infos) {
                    if (info != null) {
                        long msgID = info.getMsgId();
                        long threadID = info.getThreadId();
                        if (msgID > 0) {
                            Conversation.markMsgAsRead(mContext, threadID);
                        }
                    }

                }
            }

        };
        new Thread(runnable).start();
    }

    private void loadFonts() {
        try {
            final AssetManager mgr = this.getAssets();
            robotoRegular = Typeface.createFromAsset(mgr, FONT_ROBOTO_REGULAR);

        } catch (Exception e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error(e);
            }
        }
    }

    private void initFonts() {
        final Typeface robotoRegular = this.robotoRegular;
        if (robotoRegular != null) {
            headerText.setTypeface(robotoRegular);
        }
    }

    public static void showPopUp(Context context, SortedSet<PopUpInfo> accumulator) {
        Intent popUpIntent = createIntent(context, accumulator);
        context.startActivity(popUpIntent);
    }

    private static Intent createIntent(Context context, SortedSet<PopUpInfo> accumulator) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("PopUp.createIntent");
        }

        Intent popUpIntent = new Intent(context, PopUpNotificationActivity.class);
        ArrayList<PopUpInfo> values = new ArrayList<PopUpInfo>();
        Iterator<PopUpInfo> iterator = accumulator.iterator();
        while (iterator.hasNext()) {
            values.add(iterator.next());
        }

        popUpIntent.putParcelableArrayListExtra(MESSAGES_INFO, values);
        popUpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return popUpIntent;
    }
    
    
    public static void dismissPopup(Context context){
        
        if(PopUpNotificationActivity.isVisible()){
            if(Logger.IS_DEBUG_ENABLED){
                Logger.debug("dismissPopup(): popup is visible closing the popup");
            }
            Intent closeIntent = new  Intent(SyncManager.ACTION_SYNC_STATUS);
            closeIntent.putExtra(SyncManager.EXTRA_STATUS, SyncManager.FINISH_POP_UP);
            context.sendBroadcast(closeIntent);
            
        }else{
            if(Logger.IS_DEBUG_ENABLED){
                Logger.debug("dismissPopup(): popup is not visible. no need to close.");
            }
        }
        
    }

    class ReadStatusReceiver extends BroadcastReceiver {

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("PopUp:ReadStatusReceiver:SyncStatus Notification");
            }
            int status = intent.getIntExtra(SyncManager.EXTRA_STATUS, 0);
            if (status == SyncManager.CLEAR_SHOWN_NOTIFICATION) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("PopUp:Read clear notification - infos.size:" + infos.size());
                }
                if (intent.hasExtra(SyncManager.EXTRA_URI)) {
                    Uri uri = (Uri) intent.getParcelableExtra(SyncManager.EXTRA_URI);
                    galleryAdapter.clearReadMessage(uri);
                    setHeaderCount();
                }
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("PopUp: Done clear notification - new infos.size:" + infos.size());
                }
            } else if (status == SyncManager.FINISH_POP_UP) {
                if(Logger.IS_DEBUG_ENABLED){
                    Logger.debug("PopUp(): FINISH_POP_UP");
                }
                finish();
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("PopUp: a read clear status ignoring.status=" + status);
                }
            }
        }
    }

    public static void confirmDeleteMsgDialog(final PopUpNotificationActivity popupActivity,
            final int position) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("PopUp.confirmDeleteMsgDialog");
        }

        popupActivity.positionDeleted = position;

        final PopUpInfo info = infos.get(position);
        final boolean isSms = info.isSms();
        final long msgId = info.getMsgId();
        final Uri uri = ContentUris.withAppendedId((isSms) ? VZUris.getSmsUri() : VZUris.getMmsUri(), msgId);
        final long threadId = info.getThreadId();
        int text = R.string.confirm_dialog_title;
        int messageText = R.string.confirm_delete_message;

        final AppAlignedDialog d = new AppAlignedDialog(popupActivity, R.drawable.dialog_alert, text,
                messageText);
        Button deleteButton = (Button) d.findViewById(R.id.positive_button);
        deleteButton.setText(R.string.delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "Deleting message :" + uri);
                }

                infos.remove(info);
                ((PopUpNotificationActivity) popupActivity).galleryAdapter.updateData(infos);
                if (infos.size() == 0) {
                    popupActivity.finish();
                } else {
                    ((PopUpNotificationActivity) popupActivity).updateView(position);
                }
                d.dismiss();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        popupActivity.deleteMessage(uri, popupActivity, msgId, threadId, isSms);
                    }
                }).start();
            }
        });

        Button cancelButton = (Button) d.findViewById(R.id.negative_button);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setText(R.string.no);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "Cancel delete :" + uri);
                }

                popupActivity.positionDeleted = 0;
                d.dismiss();
            }
        });
        d.show();
    }

    protected void deleteMessage(Uri uri, Context context, long msgId, long threadId, boolean isSms) {
        context.getContentResolver().delete(uri, null, null);

        MessagingNotification.blockingUpdateNewMessageIndicator(context, false, false, null, false);

        Intent intent = new Intent(SyncManager.ACTION_MSG_DELETED);
        intent.putExtra(SyncManager.EXTRA_URI, uri);
        context.startService(intent);

        if (isSms) {
            // media cache
            MediaSyncHelper.onSMSDelete(context, msgId);
        } else {
            // media cache
            MediaSyncHelper.onMMSDelete(context, msgId);
        }
        ConversationDataObserver.onMessageDeleted(threadId, msgId,
                isSms ? ConversationDataObserver.MSG_TYPE_SMS : ConversationDataObserver.MSG_TYPE_MMS,
                ConversationDataObserver.MSG_SRC_TELEPHONY);
    }

    /*
     * The user is composing the message so hide the unnecessary views
     */
    public void startEditMode() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("PopUpNotification startEditMode");
        }
        inEditMode = true;

        header.setVisibility(View.GONE);
        back.setVisibility(View.GONE);
        next.setVisibility(View.GONE);

        LayoutParams params = mainView.getLayoutParams();
        params.height = pagerHeight;
        params.width = pagerWidth;
        mainView.setLayoutParams(params);
        mainView.setBackgroundDrawable(null);

        // ignore touch events to makesure that the user cant scroll the gallery
        gallery.ignoreTouchEvents(true);
    }

    /*
     * Show the complete popup once the user has exited the compose mode
     */
    public void endEditMode() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("PopUpNotification endEditMode");
        }

        inEditMode = false;

        header.setVisibility(View.VISIBLE);
        setNotchButtonVisibility();

        LayoutParams params = mainView.getLayoutParams();
        params.height = popUpHeight;
        params.width = popUpWidth;
        mainView.setLayoutParams(params);
        mainView.setBackgroundResource(R.drawable.popupbg_five);

        gallery.ignoreTouchEvents(false);

        // if there were any changes that occured while composing the message then update it
        if (tempInfos != null) {
            infos = tempInfos;
            tempInfos = null;
            if (infos.size() > 0) {
                updateView(0);
            }
        }
    }

    public void markMsgAsRead(int position) {
        PopUpInfo popUpInfo = infos.get(position);
        long threadId = popUpInfo.getThreadId();
        int size = infos.size();
        for (int i = 0, j = 0; i < size; i++) {
            PopUpInfo info = infos.get(j);
            if (info.getThreadId() == threadId) {
                infos.remove(j);
                continue;
            }
            j++;
        }
        galleryAdapter.updateData(infos);
        new MarkAsMsgReadTask(popUpInfo).execute();
        if (infos.size() == 0) {
            finish();
        } else {
            updateView(position);
        }
    }

    private class MarkAsMsgReadTask extends AsyncTask<Void, Void, Void> {
        PopUpInfo popUpInfo;

        public MarkAsMsgReadTask(PopUpInfo popUpInfo) {
            this.popUpInfo = popUpInfo;
        }

        protected Void doInBackground(Void... params) {
            Conversation.markMsgAsRead(mContext, popUpInfo.getThreadId());
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(popUpInfo.getMsgId() + " is marked as Read");
            }
            MessagingNotification.blockingUpdateNewMessageIndicator(PopUpNotificationActivity.this, false,
                    false, null, false);
            return null;
        }
    }

    /**
     * Returns the Value of the isVisible
     * @return the  {@link boolean}
     */
    public static boolean isVisible() {
        return isVisible;
    }

    GalleryMoveListener mGalleryMoveListener = new GalleryMoveListener() {
        @Override
        public void onGalleryMoving(int state) {
            if (state == GalleryMoveListener.STATE_GALLERY_MOVE
                    || state == GalleryMoveListener.STATE_GALLERY_FLING) {
                next.setVisibility(View.GONE);
                back.setVisibility(View.GONE);
            } else if (state == GalleryMoveListener.STATE_GALLERY_STOP
                    && galleryState == GalleryMoveListener.STATE_GALLERY_MOVE) {
                galleryState = state;
                setNotchButtonVisibility();
            }
            galleryState = state;
        }
    };

    Handler mArrowsHandler = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            if (what == POST_FLING_DONE) {
                if (mGalleryMoveListener != null) {
                    mGalleryMoveListener.onGalleryMoving(GalleryMoveListener.STATE_GALLERY_MOVE);
                    mGalleryMoveListener.onGalleryMoving(GalleryMoveListener.STATE_GALLERY_STOP);
                }
                return;
            }
            super.handleMessage(msg);
        }
    };

}