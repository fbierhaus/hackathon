package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony.Sms;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rocketmobile.asimov.Asimov;
import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.data.WorkingMessage;
import com.verizon.mms.data.WorkingMessage.MessageStatusListener;
import com.verizon.mms.model.AudioModel;
import com.verizon.mms.model.ImageModel;
import com.verizon.mms.model.LocationModel;
import com.verizon.mms.model.MediaModel;
import com.verizon.mms.model.SlideModel;
import com.verizon.mms.model.SlideshowModel;
import com.verizon.mms.model.TextModel;
import com.verizon.mms.model.VCardModel;
import com.verizon.mms.model.VideoModel;
import com.verizon.mms.pdu.MultimediaMessagePdu;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.RetrieveConf;
import com.verizon.mms.transaction.MessagingNotification.PopUpInfo;
import com.verizon.mms.ui.ConversationListAdapter.ContactNameData;
import com.verizon.mms.ui.ListDataWorker.ListDataJob;
import com.verizon.mms.ui.MessageListAdapter.SendingMessage;
import com.verizon.mms.ui.widget.ImageViewButton;
import com.verizon.mms.ui.widget.ImageViewButton.ImageButtonClickListener;
import com.verizon.mms.ui.widget.ScrollViewGallery;
import com.verizon.mms.ui.widget.TextButton;
import com.verizon.mms.ui.widget.TextButton.TextButtonClickListener;
import com.verizon.mms.util.EmojiParser;
import com.verizon.mms.util.MemoryCacheMap;
import com.verizon.mms.util.SmileyParser;
import com.verizon.mms.util.Util;

public class NotificationAdapter extends BaseAdapter implements MessageStatusListener {

    protected LayoutInflater mInflater;
    private ArrayList<PopUpInfo> msgInfoList;
    // Image Variables
    private Drawable defaultContactImage;
    private Drawable[] defaultContactImages;
    private Drawable missingPicture;

    private static final String FONT_ROBOTO_REGULAR = "fonts/roboto/Roboto-Regular.ttf";
    private static final String FONT_ROBOTO_BOLD = "fonts/roboto/Roboto-Bold.ttf";
    private Typeface robotoRegular;
    private Typeface robotoBold;
    private PopUpNotificationActivity activity;
    private DisplayMetrics metrics;
    private ScrollViewGallery gallery;

    float popUpWidth;
    float popUpPadding;
    float innerCardWidth;

    private ListDataWorker popUpDetailWorker;
    private MemoryCacheMap<Long, PopUpItem> popUpItemCache;
    private MemoryCacheMap<Long, PopUpItem> oldPopUpItemCache;
    private MemoryCacheMap<String, ContactDetails> contactDetailsCache;
    private MemoryCacheMap<String, ContactDetails> oldContactDetailsCache;

    private static final int MIN_VIEWS = 10;
    private static final int MIN_QUEUE_SIZE = 10;
    private static final int QUEUE_POPUP_DETAILS = 0;
    private static final int QUEUE_CONTACT_DETAILS = 1;

    private static final int MSG_POPUP_DETAILS = 1;
    private static final int MSG_CONTACT_DETAILS = 2;

    private final Rect rect = new Rect(0, 0, 150, 150);

    private static class PopUpItem {
        SlideshowModel slideShowModel;
        String subject;
        String senderName;
    }

    static class ContactDetails {
        ContactNameData contactNameData;
        Drawable[] images;
        ContactList contactList;
        int pos;

        public ContactDetails(String names, int num, Drawable[] images, ContactList contactList, int pos) {
            contactNameData = new ContactNameData(names, num);
            this.images = images;
            this.contactList = contactList;
            this.pos = pos;
        }
    }

    private static class ViewData {
        private View popUpMmsView;
        private View progress;
        private ContactImage popUpContactImage;
        private FromTextView popUpContact;
        private TextView popUpContactType;
        private ImageView popUpImageView;
        private ImageView popUpVideoView;
        private TextView popUpTxt;
        private TextView popUpSubjectTxt;
        private TextView popUpAttText;
        private ImageView popUpVcardView;
        private TextView popUpDateView;
        private TextView popUpLocationTextView;
        private RelativeLayout popUpBodyView;

        private TextButton openButton;
        private ImageViewButton deleteButton;
        private TextButton sendButton;
        private TextButton cancelButton;
        private int position;
        private PopUpInfo popUpInfo;
    }

    /**
	 * 
	 */
    public NotificationAdapter(PopUpNotificationActivity context, ArrayList<PopUpInfo> items,
            ScrollViewGallery gallery) {
        Resources res = context.getResources();
        activity = context;
        msgInfoList = items;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        metrics = new DisplayMetrics();
        activity.getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        defaultContactImage = res.getDrawable(R.drawable.ic_contact_picture);
        defaultContactImages = new Drawable[] { defaultContactImage };
        missingPicture = res.getDrawable(R.drawable.ic_missing_thumbnail_picture);

        popUpWidth = res.getDimensionPixelSize(R.dimen.popup_activity_width);
        popUpPadding = res.getDimensionPixelSize(R.dimen.popup_gallery_padding);
        innerCardWidth = popUpWidth - popUpPadding;

        createCache();
        createQueues();

        this.gallery = gallery;
    }

    private void createCache() {
        popUpItemCache = new MemoryCacheMap<Long, PopUpItem>(10);
        contactDetailsCache = new MemoryCacheMap<String, NotificationAdapter.ContactDetails>(20);

        oldPopUpItemCache = new MemoryCacheMap<Long, PopUpItem>(10);
        oldContactDetailsCache = new MemoryCacheMap<String, NotificationAdapter.ContactDetails>(20);
    }

    private void createQueues() {
        popUpDetailWorker = new ListDataWorker();
        popUpDetailWorker.addQueue(handler, QUEUE_POPUP_DETAILS, MSG_POPUP_DETAILS, MIN_QUEUE_SIZE, null);
        popUpDetailWorker.addQueue(handler, QUEUE_CONTACT_DETAILS, MSG_CONTACT_DETAILS, MIN_QUEUE_SIZE, null);
        popUpDetailWorker.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.view.PagerAdapter#getCount()
     */
    @Override
    public int getCount() {
        return msgInfoList.size();
    }

    public PopUpInfo getItemAtPosition(int index) {
        return msgInfoList.get(index);
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        ViewData data = null;

        if (view == null) {
            view = mInflater.inflate(R.layout.popup_pager_item, null);

            view.setLayoutParams(new Gallery.LayoutParams((int) innerCardWidth,
                    Gallery.LayoutParams.FILL_PARENT));

            view.setPadding((int) popUpPadding, 0, (int) popUpPadding, 0);
        }
        data = getViewData(view);

        PopUpInfo info = msgInfoList.get(position);
        updatePagerView(view, data, info, position);
        return view;
    }

    private ViewData getViewData(View view) {
        ViewData data = (ViewData) view.getTag();
        if (data == null) {
            data = new ViewData();
            data.popUpContactImage = (ContactImage) view.findViewById(R.id.popUpAvatar);
            data.popUpMmsView = (RelativeLayout) view.findViewById(R.id.popUpMmsView);
            data.progress = view.findViewById(R.id.progress);
            data.popUpContact = (FromTextView) view.findViewById(R.id.popUpContact);
            data.popUpContactType = (TextView) view.findViewById(R.id.popUpDesType);
            data.popUpTxt = (TextView) view.findViewById(R.id.popUpTxt);
            data.popUpSubjectTxt = (TextView) view.findViewById(R.id.popUpSubjectTxt);
            data.popUpImageView = (ImageView) view.findViewById(R.id.pop_image_content);
            data.popUpVideoView = (ImageView) view.findViewById(R.id.pop_play_video);
            data.popUpVcardView = (ImageView) view.findViewById(R.id.pop_vcard);
            data.popUpAttText = (TextView) view.findViewById(R.id.popUp_AttachmentText);
            data.popUpDateView = (TextView) view.findViewById(R.id.popUpDate);
            data.popUpLocationTextView = (TextView) view.findViewById(R.id.popUp_LocationText);
            data.popUpBodyView = (RelativeLayout) view.findViewById(R.id.popup_body);
            data.openButton = (TextButton) view.findViewById(R.id.openButton);
            data.sendButton = (TextButton) view.findViewById(R.id.sendButton);
            data.cancelButton = (TextButton) view.findViewById(R.id.cancelButton);

            data.deleteButton = (ImageViewButton) view.findViewById(R.id.deleteButton);
            final EditText text_Editor = (EditText) view.findViewById(R.id.embedded_text_editor);

            final View relativeDeleteOpenButton = view.findViewById(R.id.deleteOpenButton);
            final View relativeCancelSend = view.findViewById(R.id.cancelSendButton);
            text_Editor.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() > 0 && relativeCancelSend.getVisibility() == View.GONE) {
                        relativeCancelSend.setVisibility(View.VISIBLE);
                        activity.startEditMode();
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void afterTextChanged(Editable s) {
                    // TODO Auto-generated method stub
                }
            });

            text_Editor.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (relativeCancelSend.getVisibility() == View.GONE) {
                        relativeCancelSend.setVisibility(View.VISIBLE);
                        activity.startEditMode();

                        text_Editor.setOnFocusChangeListener(null);
                    }
                }
            });

            data.deleteButton.setButtonClickListener(new ImageButtonClickListener() {
                public void OnClick(View v) {
                    ViewData data = (ViewData) v.getTag();
                    PopUpNotificationActivity.confirmDeleteMsgDialog(activity, data.position);
                }
            });

            data.sendButton.setButtonClickListener(new TextButtonClickListener() {

                @Override
                public void OnClick(View v) {
                    final String msgTxt = text_Editor.getText().toString();

                    if (msgTxt.trim().length() > 0) {
                        text_Editor.setText("");
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Sending the message msgTxt" + msgTxt);
                        }
                        ViewData data = (ViewData) v.getTag();
                        sendMessage(msgTxt, data.position);
                        activity.markMsgAsRead(data.position);
                        activity.endEditMode();
                        Util.forceHideKeyboard(activity, v);
                        relativeCancelSend.setVisibility(View.GONE);
                        relativeDeleteOpenButton.setVisibility(View.VISIBLE);
                        text_Editor.setText("");
                    }
                }
            });

            data.cancelButton.setButtonClickListener(new TextButtonClickListener() {

                @Override
                public void OnClick(View v) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("NotificationAdapter cancelling the compose ");
                    }
                    Util.forceHideKeyboard(activity, v);
                    activity.endEditMode();
                    relativeCancelSend.setVisibility(View.GONE);
                    relativeDeleteOpenButton.setVisibility(View.VISIBLE);
                    text_Editor.setText("");
                }
            });

            data.openButton.setButtonClickListener(new TextButtonClickListener() {
                @Override
                public void OnClick(View v) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("NotificationAdapter opening the conversation");
                    }
                    ViewData data = (ViewData) v.getTag();
                    showConversation(data.position);
                }
            });
            view.setTag(data);

            if (robotoRegular == null || robotoBold == null) {
                loadFonts();
            }
            initFonts(data);
        }
        return data;
    }

    private ListDataJob loadJob = new ListDataJob() {
        public Object run(int pos, Object obj) {
            PopUpInfo info = (PopUpInfo) obj;
            PopUpItem item = new PopUpItem();
            PduPersister pdu = PduPersister.getPduPersister(activity);
            Uri uri = VZUris.getMmsUri().buildUpon().appendPath(Long.toString(info.getMsgId())).build();

            try {
                MultimediaMessagePdu msg = (MultimediaMessagePdu) pdu.load(uri);

                if (msg.getSubject() != null) {
                    String subject = msg.getSubject().toString();
                    if (subject != null && subject.length() > 0) {
                        item.subject = subject;
                    }
                }
                item.slideShowModel = SlideshowModel.createFromPduBody(activity, msg.getBody());
                item.senderName = Contact.get(((RetrieveConf) msg).getFrom().getString(), false).getName();
                synchronized (popUpItemCache) {
                    long id = getIdKey(info);
                    popUpItemCache.put(id, item);
                    oldPopUpItemCache.put(id, item);
                }
            } catch (MmsException e) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error("NotificationAdapter loadJob could not load mms for " + uri);
                }
            } catch (Exception e) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error("NotificationAdapter loadJob could not load mms for " + e);
                }
            }

            return item;
        }
    };
    private boolean closed;

    public void updatePagerView(View view, ViewData data, PopUpInfo info, final int position) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "NotificationAdapter.updatePagerView");
        }

        data.position = position;
        data.deleteButton.setTag(data);
        data.sendButton.setTag(data);
        data.openButton.setTag(data);
        data.popUpImageView.setImageBitmap(null);
        data.popUpVcardView.setBackgroundDrawable(null);
        data.popUpTxt.setText(null);
        data.popUpInfo = info;

        data.deleteButton.setEnabled(true);
        data.openButton.setEnabled(true);
        data.sendButton.setEnabled(true);
        data.cancelButton.setEnabled(true);

        setMessageContent(info, data);
    }

    public void sendMessage(String messageText, int position) {
        if(Logger.IS_DEBUG_ENABLED){
            Logger.debug("Popup:sendMessage:messageText="+messageText+" position="+position);
        }
        
        
        WorkingMessage workingMessage = WorkingMessage.createEmpty(this);
        PopUpInfo info = msgInfoList.get(position);
        Conversation conversation = Conversation.get(activity, info.getThreadId(), false);
        ContactList contactList = conversation.getRecipients();
        List<String> list = (conversation.getRecipients().getNumbersList());

       String txtWithSignature= MessagingPreferenceActivity.getSignature(activity ,messageText);
        if(Logger.IS_DEBUG_ENABLED){
            Logger.debug("Popup:sendMessage: txtWithSignature=" + txtWithSignature+" Address=" + list);
        }

        workingMessage.setText(txtWithSignature);
        workingMessage.setWorkingRecipients(list);
        workingMessage.setHasEmail(contactList.containsEmail(), true);
        workingMessage.updateContainsEmailToMms(list, workingMessage.getText());

        if (list.size() > 1) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("NotificationAdapter.sendMessage more than one recipient send group message");
            }
            workingMessage.updateState(WorkingMessage.GROUP_MMS, true, false);
        }

        workingMessage.setConversation(conversation);
        workingMessage.send();

    }

    private void setContact(ViewData data) {
        PopUpInfo popUpInfo = data.popUpInfo;

        ContactDetails contactDetail = getContactDetails(popUpInfo.getRecipientId(), data.position);
        data.popUpContact.setNames(contactDetail.contactNameData);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("popUpContactList" + contactDetail.contactNameData);
        }
        setContactType(contactDetail.contactList, data, popUpInfo);
        data.popUpContactImage.setImages(getContactImages(data.position, popUpInfo.getRecipientId()),
                defaultContactImage);
    }

    private void setContactType(ContactList contactList, ViewData data, PopUpInfo info) {
        if (contactList.size() <= 1) {
            String prefix = null;

            if (contactList != null && contactList.size() == 1) {
                Contact contact = contactList.get(0);
                prefix = contact.getPrefix();
            }
            data.popUpContactType.setText(prefix);
        } else {
            if (data.popUpInfo.isSms()) {
                data.popUpContactType.setText(R.string.channel_group_text);
            } else {
                updateMMSSenderName(data, info);
            }
        }
    }

    /**
   	 * 
   	 */
    private void setMessageContent(PopUpInfo info, ViewData data) {
        if (info == null) {
            return;
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("NotificationAdapter.setMessageContent");
        }

        long date = info.getDate();
        data.popUpDateView.setText(MessageUtils.formatTimeStampString(date, true));

        setContact(data);

        boolean isSMS = info.isSms();
        if (isSMS) {
            data.progress.setVisibility(View.GONE);
            data.popUpMmsView.setVisibility(View.GONE);
            long msgId = info.getMsgId();
            Cursor cursor = activity.getContentResolver()
                    .query(VZUris.getSmsUri(), new String[] { Sms.ADDRESS, Sms.BODY },
                            Sms._ID + " = " + msgId, null, Sms.DATE + " DESC");
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String body = cursor.getString(cursor.getColumnIndex(Sms.BODY));
                    if (body != null && body.length() > 0) {
                        data.popUpTxt.setVisibility(View.VISIBLE);
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("SAM:" + body + "\t :" + getSpannedMsgBody(body));
                        }
                        data.popUpTxt.setText(getSpannedMsgBody(body));
                    }

                }
            }
            if (cursor != null) {
                cursor.close();
            }
        } else {
            PopUpItem item = null;
            PopUpItem oldItem = null;
            synchronized (popUpItemCache) {
                item = popUpItemCache.get(getIdKey(info));

                if (oldPopUpItemCache != null) {
                    oldItem = popUpItemCache.get(getIdKey(info));
                }
            }

            if (item == null) {
                popUpDetailWorker.request(data.position, loadJob, info);
                data.popUpMmsView.setVisibility(View.GONE);
                data.progress.setVisibility(View.VISIBLE);
            }
            if (item == null) {
                item = oldItem;
            }
            if (item != null) {
                data.progress.setVisibility(View.GONE);
                if (item.subject != null && item.subject.length() > 0) {
                    data.popUpSubjectTxt.setVisibility(View.VISIBLE);
                    data.popUpSubjectTxt.setText(getSpannedMsgBody(item.subject));
                }

                SlideModel slide = loadSlideShow(item.slideShowModel, data);
                if (!slide.isTextOnly()) {
                    showMMSPreview(slide, data);
                }
            }
        }
    }

    private SpannableStringBuilder getSpannedMsgBody(String msg) {
        SpannableStringBuilder buf = new SpannableStringBuilder();
        CharSequence parsedMsg;
        if (MmsConfig.enableEmojis) {
            parsedMsg = EmojiParser.getInstance().addEmojiSpans(msg, false);
        } else {
            parsedMsg = SmileyParser.getInstance().addSmileySpans(msg, false);
        }
        buf.append(parsedMsg);
        return buf;
    }

    /**
     * @param slide
     */
    private void showMMSPreview(SlideModel slide, ViewData data) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "NotificationAdapter.showMMSPreview :");
        }
        MediaModel media = null;
        for (MediaModel model : slide.getMedia()) {
            if (!model.isText()) {
                media = model;
                break;
            }
        }

        if (media == null) {
            data.popUpMmsView.setVisibility(View.GONE);
            data.popUpTxt.setVisibility(View.VISIBLE);
            TextModel model = slide.getText();
            String txtBody = model.getText();
            if (txtBody != null && txtBody.length() > 0) {
                data.popUpTxt.setText((getSpannedMsgBody(txtBody))); // Add Group message text
            }

        } else {
            data.popUpMmsView.setVisibility(View.VISIBLE);
            if (media.isImage()) {
                data.popUpImageView.setVisibility(View.VISIBLE);
                data.popUpVideoView.setVisibility(View.GONE);
                data.popUpVideoView.setImageDrawable(null);
                ImageModel imageModel = (ImageModel) media;
                Bitmap bmp = imageModel.getBitmap(rect);
                if (bmp != null) {
                    data.popUpImageView.setImageBitmap(bmp);
                } else {
                    data.popUpImageView.setImageDrawable(missingPicture);
                }
            } else if (media.isVideo()) {
                data.popUpImageView.setVisibility(View.VISIBLE);
                data.popUpVideoView.setVisibility(View.VISIBLE);
                VideoModel videoModel = (VideoModel) media;
                Bitmap bmp = videoModel.getBitmap();
                data.popUpImageView.setImageBitmap(bmp);

            } else if (media.isAudio()) {
                AudioModel audioModel = (AudioModel) media;
                Map<String, ?> extras = audioModel.getExtras();
                String audioText = audioModel.getSrc();
                String album = (String) extras.get("album");
                if (album != null) {
                    audioText = audioText + "\n" + album;
                }
                String artist = (String) extras.get("artist");
                if (artist != null) {
                    audioText = audioText + "\n" + artist;
                }
                data.popUpImageView.setVisibility(View.GONE);
                data.popUpVideoView.setVisibility(View.GONE);
                data.popUpVideoView.setImageDrawable(null);
                data.popUpVcardView.setVisibility(View.VISIBLE);
                data.popUpVcardView.setImageResource(R.drawable.audio);
                data.popUpAttText.setVisibility(View.VISIBLE);
                data.popUpAttText.setText(audioText);
            } else if (media.isVcard()) {
                VCardModel vcardModel = (VCardModel) media;
                Bitmap contactImage = vcardModel.getContactPicture();
                data.popUpImageView.setVisibility(View.GONE);
                data.popUpVideoView.setVisibility(View.GONE);
                data.popUpVideoView.setImageDrawable(null);
                data.popUpVcardView.setVisibility(View.VISIBLE);
                if (contactImage == null) {
                    data.popUpVcardView.setImageResource(R.drawable.list_namecard);
                } else {
                    data.popUpVcardView.setImageBitmap(contactImage);
                }
                data.popUpAttText.setVisibility(View.VISIBLE);
                data.popUpAttText.setText(vcardModel.getFormattedMsg());
            } else if (media.isLocation()) {
                if (slide.hasLocation()) {
                    ImageModel im = slide.getImage();
                    Bitmap bmp = im != null ? im.getBitmap() : null;
                    LocationModel locationModel = (LocationModel) media;
                    if (bmp != null) {
                        data.popUpImageView.setVisibility(View.VISIBLE);
                        data.popUpLocationTextView.setVisibility(View.VISIBLE);
                        data.popUpVcardView.setVisibility(View.GONE);
                        data.popUpAttText.setVisibility(View.GONE);
                        data.popUpImageView.setImageBitmap(bmp);
                        data.popUpLocationTextView.setText(locationModel.getFormattedMsg());
                    } else {
                        data.popUpImageView.setVisibility(View.GONE);
                        data.popUpLocationTextView.setVisibility(View.GONE);
                        data.popUpVcardView.setVisibility(View.VISIBLE);
                        data.popUpVcardView.setImageResource(R.drawable.attach_location);
                        data.popUpAttText.setVisibility(View.VISIBLE);
                        data.popUpAttText.setText(locationModel.getFormattedMsg());
                    }
                }

                data.popUpVideoView.setVisibility(View.GONE);
                data.popUpVideoView.setImageDrawable(null);

            }
        }
    }

    /**
     * @param createFromPduBody
     */
    private SlideModel loadSlideShow(SlideshowModel slideShow, ViewData data) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "loadSlideshow: slideshow = " + slideShow);
        }
        String body;
        final SlideModel slide = slideShow.get(0);
        if ((slide != null) && slide.hasText()) {
            TextModel tm = slide.getText();
            if (tm.isDrmProtected()) {
                body = activity.getString(R.string.drm_protected_text);
            } else {
                body = tm.getText();
                // when there is no text in the text part the cursor tends to
                // return
                // a new line character which makes the UI look unpleasant
                if (body.length() == 1) {
                    body = body.trim();
                }
            }
            // mTextContentType = tm.getContentType();
        } else {
            body = "";
        }

        if (body != null && body.length() > 0) {
            data.popUpTxt.setVisibility(View.VISIBLE);
            data.popUpTxt.setText(getSpannedMsgBody(body));
        }
        return slide;

    }

    private void loadFonts() {
        try {
            final AssetManager mgr = activity.getAssets();
            robotoRegular = Typeface.createFromAsset(mgr, FONT_ROBOTO_REGULAR);
            robotoBold = Typeface.createFromAsset(mgr, FONT_ROBOTO_BOLD);
        } catch (Exception e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error(e);
            }
        }
    }

    private void initFonts(ViewData data) {
        final Typeface robotoRegular = this.robotoRegular;
        final Typeface robotoBold = this.robotoBold;
        if (robotoRegular != null && robotoBold != null) {
            data.popUpTxt.setTypeface(robotoRegular);
            data.popUpDateView.setTypeface(robotoRegular);
            data.popUpSubjectTxt.setTypeface(robotoBold);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(int pos) {
        // TODO Auto-generated method stub
        return msgInfoList.get(pos);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    /**
     * This Method
     * 
     * @param uri
     */
    public void clearReadMessage(Uri uri) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("POPUP: clearReadMessage()");
        }
        boolean msgType = isSms(uri);
        long id = ContentUris.parseId(uri);
        int removeIndex = -1;
        int size = msgInfoList.size();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("POPUP: Message to read  -  msgType:" + msgType + " id:" + id);
        }
        for (int i = 0; i < size; i++) {
            PopUpInfo info = msgInfoList.get(i);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("POPUP: Message in popup -  issms:" + info.isSms() + " id:" + info.getMsgId());
            }
            if (info.isSms() == msgType && info.getMsgId() == id) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("POPUP: Message read." + info);
                }
                removeIndex = i;
                break;
            }
        }

        if (removeIndex >= 0) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("POPUP: Read message removed ." + msgInfoList.get(removeIndex));
            }
            msgInfoList.remove(removeIndex);
            updateData(msgInfoList);
            if (msgInfoList.isEmpty()) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("POPUP: No Read message. closing pop dialog.");
                }
                activity.finish();
            }

        }

    }

    /**
     * This Method
     * 
     * @param uri
     * @return
     */
    private boolean isSms(Uri uri) {
        if (VZUris.getSmsAuthority().equalsIgnoreCase(uri.getAuthority())) {
            return true;
        }
        return false;
    }

    private void showConversation(int position) {
        activity.finish();

        PopUpInfo popUpInfo = msgInfoList.get(position);
        long threadId = popUpInfo.getThreadId();
        Intent clickIntent = ConversationListActivity.getNotificationIntentFromParent(activity, threadId,
                false);
        clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        clickIntent.putExtra(ComposeMessageFragment.INTENT_FROM_NOTIFICATION, true);
        Asimov.getApplication().startActivity(clickIntent);
    }

    @Override
    public Context getContext() {
        // TODO Auto-generated method stub
        return activity;
    }

    @Override
    public void onAttachmentChanged() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAttachmentError(int error) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMaxPendingMessagesReached() {
        // TODO Auto-generated method stub

    }

    public void onMessageChanged(boolean protocolChanged, boolean mms, boolean messageChanged,
            boolean messageMms) {
    }

    @Override
    public void onMessagesSent(List<SendingMessage> msgs) {
    }

    @Override
    public void onPreMessageSent() {
    }

    public void onSendError() {
    };

    @Override
    public void onSendingMessages(List<SendingMessage> msgs, boolean mms, long lastId) {
    }

    public void shutdown() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("NotificationAdapter.shutdown");
        }
        closed = true;
        if (popUpDetailWorker != null) {
            popUpDetailWorker.exit();
            popUpDetailWorker = null;
        }
        if (popUpItemCache != null) {
            popUpItemCache.clear();
        }

        if (oldPopUpItemCache != null) {
            oldPopUpItemCache.clear();
        }

        if (contactDetailsCache != null) {
            contactDetailsCache.clear();
        }
    }

    public boolean isClosed() {
        return closed;
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            // update the item if it is visible
            final ScrollViewGallery galleryView = NotificationAdapter.this.gallery;
            final int first = galleryView.getFirstVisiblePosition();
            final int last = galleryView.getLastVisiblePosition();
            final int pos = msg.arg1;

            if (pos >= first && pos <= last) {
                try {
                    final View view = galleryView.getChildAt(pos - first);
                    final ViewData data = (ViewData) view.getTag();

                    switch (msg.what) {
                    case MSG_POPUP_DETAILS:
                        setMessageContent(data.popUpInfo, data);
                        break;
                    case MSG_CONTACT_DETAILS:
                        setContact(data);
                        break;
                    }
                } catch (Exception e) {
                    Logger.error(e);
                }
            }
        }
    };

    public void updateData(ArrayList<PopUpInfo> info) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("NotificationAdapter.updateData");
        }
        if (popUpDetailWorker != null) {
            popUpDetailWorker.clear();
        }
        synchronized (popUpItemCache) {
            popUpItemCache.clear();
        }
        synchronized (contactDetailsCache) {
            contactDetailsCache.clear();
        }
        msgInfoList = info;
        notifyDataSetChanged();
    }

    public long getIdKey(PopUpInfo info) {
        return info.isSms() ? info.getDate() : -info.getDate();
    }

    private ContactDetails getContactDetails(String id, int pos) {
        ContactDetails contactDetails = null;
        synchronized (contactDetailsCache) {
            contactDetails = contactDetailsCache.get(id);

            if (contactDetails == null) {
                contactDetails = oldContactDetailsCache.get(id);

                if (contactDetails != null) {
                    contactDetails.images = null;
                    contactDetailsCache.put(id, contactDetails);
                }
            }
        }

        if (contactDetails == null) {
            ContactList contacts = ContactList.getByIds(id, false);
            int num = contacts.size();
            String names;

            if (num == 1) {
                names = contacts.get(0).getName();
            } else if (num > 1) {
                names = contacts.formatNames();
            } else {
                names = "";
                Logger.error(this + ".fetchContactNames: no contacts for " + id);
            }

            contactDetails = new ContactDetails(names, num, null, contacts, pos);
            synchronized (contactDetailsCache) {
                contactDetailsCache.put(id, contactDetails);
            }
        }
        return contactDetails;
    }

    private Drawable[] getContactImages(int pos, String id) {
        ContactDetails contactDetails = null;
        Drawable[] images = defaultContactImages;
        synchronized (contactDetailsCache) {
            contactDetails = contactDetailsCache.get(id);
        }

        if (contactDetails != null) {
            // try the local cache
            images = contactDetails.images;
            if (images == null) {
                popUpDetailWorker.request(QUEUE_CONTACT_DETAILS, pos, contactDetailsJob, id);

                images = defaultContactImages;
            }
        }
        return images;
    }

    private ListDataJob contactDetailsJob = new ListDataJob() {
        public Object run(int pos, Object data) {
            ContactDetails contactDetails = null;
            Drawable[] images = defaultContactImages;
            String ids = (String) data;

            synchronized (contactDetailsCache) {
                contactDetails = contactDetailsCache.get(ids);
                oldContactDetailsCache.put(ids, contactDetails);
            }

            if (contactDetails != null) {
                StringBuilder builder = new StringBuilder();
                ContactList contacts = contactDetails.contactList;
                int size = contacts.size();
                for (int i = 0; i < size; i++) {
                    builder.append(contacts.get(i).getRecipientId());

                    if (i != size) {
                        builder.append(" ");
                    }
                }

                contacts = ContactList.getByIds(builder.toString(), true);

                int num = contacts.size();
                String names;

                if (num == 1) {
                    names = contacts.get(0).getName();
                } else if (num > 1) {
                    names = contacts.formatNames();
                } else {
                    names = "";
                    Logger.error(this + ".fetchContactNames: no contacts for " + ids);
                }

                contactDetails.contactNameData = new ContactNameData(names, num);
                contactDetails.contactList = contacts;

                images = contactDetails.contactList.getImages(activity, defaultContactImage, false);
                if (images == null) {
                    images = defaultContactImages;
                }
                contactDetails.images = images;
            }
            return contactDetails;
        }
    };

    public String getRecipId(ContactList contacts) {
        StringBuilder builder = new StringBuilder();
        int size = contacts.size();
        for (int i = 0; i < size; i++) {
            builder.append(contacts.get(i).getRecipientId());

            if (i != size) {
                builder.append(" ");
            }
        }

        return builder.toString();
    }

    private void updateMMSSenderName(ViewData data, PopUpInfo info) {
        synchronized (popUpItemCache) {
            PopUpItem item = null;
            item = popUpItemCache.get(getIdKey(info));
            if (item == null) {
                if (oldPopUpItemCache != null) {
                    item = oldPopUpItemCache.get(getIdKey(info));
                }
            }
            if (item != null) {
                data.popUpContactType.setText(activity.getString(R.string.from) + ": " + item.senderName);
            } else {
                data.popUpContactType.setText(R.string.channel_group_mms);
            }
        }
    }
}
