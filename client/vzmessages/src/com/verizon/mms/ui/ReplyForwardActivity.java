package com.verizon.mms.ui;

import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOFORWARDADDR;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOFORWARDENDDATE;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOFORWARDSTATUS;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOREPLYENDDATE;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOREPLYMSG;
import static com.verizon.messaging.vzmsgs.AppSettings.KEY_VMA_AUTOREPLYSTATUS;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vma.provision.ProvisionManager;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.provider.Vma.RecentlyUsedFwdAddr;
import com.verizon.messaging.vzmsgs.provider.Vma.RecentlyUsedReplyMsg;
import com.verizon.messaging.vzmsgs.sync.AppErrorCodes;
import com.verizon.mms.DeviceConfig.OEM;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.ui.widget.ImageViewButton;

public class ReplyForwardActivity extends VZMActivity implements View.OnClickListener {
    public static final String REPLY_INTENT = "reply";
    public static final String FWD_INTENT = "forward";
    public static final int REQUEST_CODE_PICK_CONTACT = 4323;
    TextView mTitleText;
    TextView mEnterDesc;
    ImageView mHeader;
    LinearLayout mForwardedMsgList;
    private boolean isReply;
    Button mPositiveButton;
    Button mNegativeButton;
    DatePicker mLastSavedDate;
    Spinner mSelectedSetting;
    TextView mLastFiveHint;
    TextView mDateDesc;
    String autoFwdStatus;
    String autoFwdAddr;
    String autoFwdEndDate;
    String autoReplyStatus;
    String autoReplyMsg;
    String autoReplyDate;
    private boolean isTablet;
    private ApplicationSettings settings;
    private ProvisionManager provMng;
    private Context context;
    String selectedContact;
    private ArrayList<String> messageList;
    String clickedView;
    private ImageViewButton refreshView;
    private AsyncTask mValidateTask;
	private boolean mIsLandscape;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	isTablet = MmsConfig.isTabletDevice();
    	Configuration config = getResources().getConfiguration();
		mIsLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
    	if (isTablet) {
    		setContentView(R.layout.reply_forward_dialog);
		}
    	else
    	{
    		if(mIsLandscape){
    			setContentView(R.layout.reply_forward_handset_land_dialog);
    		}
    		else{
    			setContentView(R.layout.reply_forward_handset_dialog);
    		}
    	}
        context = this;
        settings = ApplicationSettings.getInstance();
        provMng = new ProvisionManager(context);
        isReply = getIntent().getBooleanExtra(REPLY_INTENT, false);
        initView();
        //mValidateTask = new CheckIfReplyForwardStatusChanged().execute();
        messageList = getMessage();
        if (isReply) {
            updateViewForReply();
        } else {
            updateViewForForward();
        }
        super.onCreate(savedInstanceState);
    }

 

    @Override
	protected void onDestroy() {
		if(mValidateTask != null){
			mValidateTask.cancel(true);
		}
		super.onDestroy();
	}



	private class CheckIfReplyForwardStatusChanged extends AsyncTask<Void, Void, Integer> {
        private void checkReplyForwardActivation() {
            if (isReply) {
                String reply = settings.getStringSettings(KEY_VMA_AUTOREPLYSTATUS);
                if (reply != null) {
                    if (reply.equals(autoReplyStatus)) {
                        return;
                    }
                }
                if (autoReplyStatus != null) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(),
                                "Auto Reply view is refreshing as data has been changed in Server");
                    }
                    messageList = getMessage();
                    updateViewForReply();
                }
            } else {
                String forward = settings.getStringSettings(KEY_VMA_AUTOFORWARDSTATUS);
                if (forward != null) {
                    if (forward.equals(autoFwdStatus)) {
                        return;
                    }
                }
                if (autoFwdStatus != null) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(),
                                "Auto Forward view is refreshing as data has been changed in Server");
                    }
                    messageList = getMessage();
                    updateViewForForward();
                }
            }
        }
        
        /* Overriding method 
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            refreshView.setImageResource(R.xml.progress_bar_style);
            refreshView.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                return provMng.syncMessagingAssistantsSettings(isReply);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return -1;
        };

        protected void onPostExecute(Integer result) {
            refreshView.setImageResource(R.drawable.refresh);
            refreshView.setEnabled(true);
            if (AppErrorCodes.VMA_PROVISION_OK == result.intValue()) {
                checkReplyForwardActivation();
            }
            
        };
    }
    
    
    
    private void updateViewForForward() {
        //mLastFiveHint.setText(R.string.vma_forward_lastFive);
        mDateDesc.setText(R.string.vma_until);
        autoFwdStatus = settings.getStringSettings(KEY_VMA_AUTOFORWARDSTATUS);
        autoFwdAddr = settings.getStringSettings(KEY_VMA_AUTOFORWARDADDR);
        autoFwdEndDate = settings.getStringSettings(KEY_VMA_AUTOFORWARDENDDATE);
      
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "Auto Forward Status :" + getStatus(autoFwdStatus) + "\t autoFwdAddr" + autoFwdAddr
            		+ "\t autoFwdEndDate"+autoFwdEndDate);
        }
        mHeader.setImageResource(R.drawable.auto_forward_header);
        mForwardedMsgList.removeAllViews();
        LayoutInflater mInflater = LayoutInflater.from(context);
        for(int i =0; i < messageList.size(); i++){
            final View container = mInflater.inflate(R.layout.reply_forward_item, null);
            TextView text = (TextView) container.findViewById(R.id.tv_title);
            final String result = messageList.get(i);
            final ImageView img = (ImageView) container.findViewById(R.id.iv_icon);
            if (result.equals(getString(R.string.vma_forward_desc))) {
                text.setText(Html.fromHtml("<b>" + result + "</b>"));
                img.setImageResource(R.drawable.arrow_add);
            } else {
                text.setText(result);
                img.setImageResource(R.drawable.radiobutton_off);
            }
            if(result.equals(clickedView) || result.equals(autoFwdAddr)){
                img.setImageResource(R.drawable.radiobutton_on);
            }
            container.setOnClickListener(new ReplyForwardListener(result));
            mForwardedMsgList.addView(container, i);
        }
        clickedView = null;
        mEnterDesc.setText(R.string.vma_forward_select);
        if (!TextUtils.isEmpty(autoFwdEndDate)) {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            try {
                Date date = format.parse(autoFwdEndDate);
                final Calendar c = Calendar.getInstance();
                c.setTime(date);
                mLastSavedDate.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), null);
            } catch (Exception e) {

            }
        }
        else
        {
            final Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, 14);
            mLastSavedDate.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), null);
        }
        // Show Pending State
        if("0".equals(autoFwdStatus)){
            mPositiveButton.setText(getString(R.string.stop)+ " " + getString(R.string.vma_auto_forward));
            mTitleText.setText(Html.fromHtml(getString(R.string.vma_auto_forward )+" "+getString(R.string.vma_is_currently)+" " + getString(R.string.vma_pending)+"."));
            disableResources();
            return;
        }

        if (settings.isAutoForwardEnabled()){
            mPositiveButton.setText(getString(R.string.stop)+ " " + getString(R.string.vma_auto_forward));
            mTitleText.setText(Html.fromHtml(getString(R.string.vma_auto_forward )+" "+getString(R.string.vma_is_currently)+" " +  getString(R.string.active)+"."));
            disableResources();
        }
        else {
            mPositiveButton.setText(getString(R.string.start)+ " " + getString(R.string.vma_auto_forward));
            mTitleText.setText(Html.fromHtml(getString(R.string.vma_auto_forward )+" "+getString(R.string.vma_is_currently)+" " +  getString(R.string.inactive)+"."));
            enableResources();
        }
    }

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
    	MessagingPreferenceActivity.setLocale(context);
    	if(isTablet){
    		super.onConfigurationChanged(newConfig);
    		return;
    	}
    	mIsLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
    	if(mIsLandscape){
			setContentView(R.layout.reply_forward_handset_land_dialog);
		}
		else{
			setContentView(R.layout.reply_forward_handset_dialog);
		}
    	initView();
    	if(Logger.IS_DEBUG_ENABLED){
    		Logger.debug("messagelist in Reply Forward"+messageList.toString());
    	}
    	if (isReply) {
            updateViewForReply();
        } else {
            updateViewForForward();
        }
        super.onConfigurationChanged(newConfig);
	
    }
    

    private void showComposeDialog(String result) {
        final AppAlignedDialog dialog = new AppAlignedDialog(context, R.layout.reply_forward_entry_dialog);
        final EditText enterdata = (EditText) dialog.findViewById(R.id.enterData);
        
        ImageViewButton composeButton = (ImageViewButton) dialog.findViewById(R.id.composebutton);
        TextView dialogheader = (TextView) dialog.findViewById(R.id.dialog_header);
        final TextView desciption = (TextView) dialog.findViewById(R.id.enter_desc);
		final TextView maxLimitTxt = (TextView) dialog.findViewById(R.id.maxlimitcount);
		final int SMS_MAX_LIMIT=Integer.parseInt(context.getResources().getString(R.string.auto_reply_msg_limit));
		maxLimitTxt.setText("0"+"/"+SMS_MAX_LIMIT);
		Logger.debug("SMS_MAX_LIMIT123:"+SMS_MAX_LIMIT);
        enterdata.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		enterdata.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				}

			@Override
			public void afterTextChanged(Editable s) {
				int count = s.toString().length();
				Logger.debug(getClass(), "COUNT:::" + count);
				maxLimitTxt.setText("" + count + "/"+SMS_MAX_LIMIT);
				Logger.debug(getClass(), "COUNT:::" + count);
				if (count == SMS_MAX_LIMIT) {
					desciption.setText(R.string.auto_reply_limit_exceeded_msg);
					desciption.setTextColor(Color.RED);
				} else {
					desciption.setText(R.string.vma_auto_reply_message);
					desciption.setTextColor(Color.BLACK);
				}
			}
		});
		
        if(isReply){
            dialogheader.setText(R.string.vma_auto_reply);
            composeButton.setVisibility(View.GONE);
            desciption.setText(R.string.vma_auto_reply_message);
            enterdata.setMinLines(3);
            if(result.equals(getString(R.string.vma_reply_default_text))){
                enterdata.setHint(result);    
            }
            else
            {
                enterdata.setText(result);    
            }
        }
        else
        {
            dialogheader.setText(R.string.vma_forward_to);
            composeButton.setVisibility(View.VISIBLE);
            desciption.setText(R.string.vma_auto_forward_destination);
            composeButton.setImageResource(R.drawable.address_book);
            composeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    addNativeContact();
                }
            });
            if(result.equals(getString(R.string.vma_forward_desc))){
                enterdata.setHint(result);    
            }
            else
            {
                enterdata.setText(result);    
            }
        }
        Button saveButton = (Button) dialog.findViewById(R.id.positive_button);
        saveButton.setText(R.string.save);
        saveButton.setTextColor(getResources().getColor(R.color.black));
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredText = enterdata.getText().toString();
                if (TextUtils.isEmpty(enteredText)) {
                    if (isReply) {
                        enterdata.setError(getString(R.string.vma_auto_reply_add_msg));
                    } else {
                        enterdata.setError("Please add an address");
                    }
                } else {
                    addView(enteredText);
                    dialog.dismiss();
                }
            }
        });
        Button cancelButton = (Button) dialog.findViewById(R.id.negative_button);
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setText(R.string.cancel);
        cancelButton.setTextColor(getResources().getColor(R.color.black));
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

	/**
     * This Method will add a View to the message / number selection 
     * @param string selectedString
     */
    protected void addView(final String selectedData) {
        if (!isReply) {
            if (!MessageUtils.isValidMmsAddress(selectedData)) {
                Toast.makeText(context, getString(R.string.vma_invalid_address_msg, selectedData),Toast.LENGTH_LONG).show();
                return;
            }
        }
        for(String checkString: messageList){            
            if(checkString.equals(selectedData)){
               setSelected(checkString);
              //  Toast.makeText(context, R.string.vma_duplicate_address,Toast.LENGTH_LONG).show();
                return;
            }
        }
        messageList.add(0, selectedData);
        if (isReply) {
            ContentValues values = new ContentValues(1);
            values.put(AppSettings.KEY_VMA_AUTOREPLYUSEDMSGS, selectedData);
            context.getContentResolver().insert(RecentlyUsedReplyMsg.CONTENT_URI, values);
        } else {
            ContentValues values = new ContentValues(1);
            values.put(AppSettings.KEY_VMA_AUTOFORWARDUSEDADDR, selectedData);
            context.getContentResolver().insert(RecentlyUsedFwdAddr.CONTENT_URI, values);
        }
        setSelected(selectedData);
    }

    private void updateViewForReply() {
       // mLastFiveHint.setText(R.string.vma_reply_lastFive);
        mDateDesc.setText(R.string.vma_sent_until);
        autoReplyStatus = settings.getStringSettings(KEY_VMA_AUTOREPLYSTATUS);
        autoReplyMsg = settings.getStringSettings(KEY_VMA_AUTOREPLYMSG);
        autoReplyDate = settings.getStringSettings(KEY_VMA_AUTOREPLYENDDATE);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "Auto Reply Status :" + getStatus(autoReplyStatus) + "\t autoReplyMsg" + autoReplyMsg
            		+ "\t autoReplyDate"+autoReplyDate);
        }
        mHeader.setImageResource(R.drawable.auto_reply_header);
        mForwardedMsgList.removeAllViews();
        LayoutInflater mInflater = LayoutInflater.from(context);
        
        for(int i =0; i < messageList.size(); i++){
            final View container = mInflater.inflate(R.layout.reply_forward_item, null);
            final ImageView img = (ImageView) container.findViewById(R.id.iv_icon);
            TextView text = (TextView) container.findViewById(R.id.tv_title);
            final String result = messageList.get(i);
            if (result.equals(getString(R.string.vma_reply_default_text))) {
                text.setText(Html.fromHtml("<b>" + result + "</b>"));
                img.setImageResource(R.drawable.arrow_add);
            } else {
                text.setText(result);
                img.setImageResource(R.drawable.radiobutton_off);
            }
            if(result.equals(clickedView) || result.equals(autoReplyMsg)){
                img.setImageResource(R.drawable.radiobutton_on);
            }
            container.setOnClickListener(new ReplyForwardListener(result));
            mForwardedMsgList.addView(container, i);
        }
        clickedView = null;
        if (!TextUtils.isEmpty(autoReplyDate)) {
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
            try {
                Date date = format.parse(autoReplyDate);
                final Calendar c = Calendar.getInstance();
                c.setTime(date);
                mLastSavedDate.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), null);
            } catch (Exception e) {

            }
        }
        else
        {
            final Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, 14);
            mLastSavedDate.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), null);
        }

        if (settings.isAutoReplyEnabled()){
            mPositiveButton.setText(getString(R.string.stop)+ " " + getString(R.string.vma_auto_reply));
            mTitleText.setText(Html.fromHtml(getString(R.string.vma_auto_reply )+" "+ getString(R.string.vma_is_currently)+" " + getString(R.string.active)+"." ));
            disableResources();
        }
        else {
            mPositiveButton.setText(getString(R.string.start)+ " " + getString(R.string.vma_auto_reply));
            mTitleText.setText(Html.fromHtml(getString(R.string.vma_auto_reply )+" "+getString(R.string.vma_is_currently)+" " + getString(R.string.inactive) +"."));
            enableResources();
        }
        mEnterDesc.setText(R.string.vma_reply_options);
        
    }

    /**
     * This Method will remove all views and redraws again
     * so that we dont get more than one checkView in any way
     * @param result
     */
    protected void setSelected(String data) {
        mForwardedMsgList.removeAllViews();
        LayoutInflater mInflater = LayoutInflater.from(context);
        
        for(int i =0; i < messageList.size(); i++){
            final View container = mInflater.inflate(R.layout.reply_forward_item, null);
            final ImageView img = (ImageView) container.findViewById(R.id.iv_icon);
            TextView text = (TextView) container.findViewById(R.id.tv_title);
            final String result = messageList.get(i);
            if(isReply){
                if (result.equals(getString(R.string.vma_reply_default_text))) {
                    text.setText(Html.fromHtml("<b>" + result + "</b>"));
                } else {
                    text.setText(result);
                    img.setImageResource(R.drawable.radiobutton_off);
                }
            }
            else
            {
                if (result.equals(getString(R.string.vma_forward_desc))) {
                    text.setText(Html.fromHtml("<b>" + result + "</b>"));
                    img.setImageResource(R.drawable.arrow_add);
                } else {
                    text.setText(result);
                    img.setImageResource(R.drawable.radiobutton_off);
                }
            }
            if(result.equals(data)){
                clickedView = data;
                img.setImageResource(R.drawable.radiobutton_on);
            }
            container.setOnClickListener(new ReplyForwardListener(result));
            mForwardedMsgList.addView(container, i);
        }
    }

    private void initView() {
        mTitleText = (TextView) findViewById(R.id.titleText);
        mHeader = (ImageView) findViewById(R.id.header);
        mEnterDesc = (TextView) findViewById(R.id.enterDesc);
        mForwardedMsgList = (LinearLayout) findViewById(R.id.forwardedEmails);
        mPositiveButton = (Button) findViewById(R.id.positive_button);
        mNegativeButton = (Button) findViewById(R.id.negative_button);
        mLastSavedDate = (DatePicker) findViewById(R.id.datePicker);
        mPositiveButton.setOnClickListener(this);
        mNegativeButton.setOnClickListener(this);
        //mLastFiveHint = (TextView) findViewById(R.id.lastFiveHint);
        mDateDesc = (TextView) findViewById(R.id.dateDesc);
        refreshView = (ImageViewButton)findViewById(R.id.refreshView);
        refreshView.setOnClickListener(this);
    }


    private void enableResources() {
        mLastSavedDate.setEnabled(true);
        mForwardedMsgList.setEnabled(true);
        for(int i =0; i <mForwardedMsgList.getChildCount(); i++){
            View v = mForwardedMsgList.getChildAt(i);
            v.setEnabled(true);
            v.setClickable(true);
            v.setBackgroundColor(Color.WHITE);
        }
    }

    private void disableResources() {
        mLastSavedDate.setEnabled(false);
        mForwardedMsgList.setEnabled(false);
        for(int i =0; i <mForwardedMsgList.getChildCount(); i++){
            View v = mForwardedMsgList.getChildAt(i);
            v.setEnabled(false);
            v.setClickable(false);
            v.setBackgroundColor(Color.DKGRAY);
        }
    }
   
    public String getStatus(String forward) {
        String status = "";
        if (forward != null) {
            int forwardStatus = -1;
            try {
                forwardStatus = Integer.valueOf(forward);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (forwardStatus == 0) {
                status = "PENDING";
            } else if (forwardStatus == 1) {
                status = "ACTIVE";
            } else if (forwardStatus == 2) {
                status = "INACTIVE";
            } else if (forwardStatus == 3) {
                status = "EXPIRED";
            } else if (forwardStatus == 4) {
                status = "STOPPED";
            }
        }
        return status;
    }

    
    @Override
    public void onClick(View v) {
        int id = v.getId();
        final String date = (mLastSavedDate.getMonth() + 1) + "/" + mLastSavedDate.getDayOfMonth() + "/"
                + mLastSavedDate.getYear();
        if (id == R.id.positive_button) {
            if (isReply) {
                if (settings.isAutoReplyEnabled()) {
                	if(mValidateTask!=null){
                		mValidateTask.cancel(true);
                	}
                    disableAutoReply();
              
                } else {

                    if (!TextUtils.isEmpty(clickedView)) {
                    	enableAutoReply(clickedView, date);
                    } else {
                        Toast.makeText(
                                context,
                                (messageList.size() > 1) ?R.string.vma_select_message
                                        : R.string.vma_create_message, Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                if (settings.isAutoForwardEnabled()) {
                    disableAutoForward();
                } else {
                    if (!TextUtils.isEmpty(clickedView)) {
                        final String extractValidAddress = MessageUtils.parseMmsAddress(clickedView, false);
                        if (extractValidAddress == null) {
                            Toast.makeText(context, R.string.vma_select_valid_address, Toast.LENGTH_LONG)
                                    .show();
                        } else {
                            final Dialog d = new AppAlignedDialog(ReplyForwardActivity.this,
                                    R.drawable.dialog_alert,R.string.vma_auto_copy_forward,
                                    R.string.vma_automatically_send_copies);
                            Button positiveButton = (Button) d.findViewById(R.id.positive_button);
                            positiveButton.setText(R.string.yes);
                            positiveButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                	if(mValidateTask!=null){
                                		mValidateTask.cancel(true);
                                	}
                                    enableAutoForward(extractValidAddress, date);
                                    d.dismiss();
                                }
                            });

                            Button negativeButton = (Button) d.findViewById(R.id.negative_button);
                            negativeButton.setVisibility(View.VISIBLE);
                            negativeButton.setText(R.string.no);
                            negativeButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    d.dismiss();
                                }
                            });
                            d.show();
                        }
                    } else {
                        Toast.makeText(context,R.string.vma_select_valid_address, Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
        else if(id == R.id.negative_button){
            finish();
        }
        else if(id == R.id.refreshView){
            new CheckIfReplyForwardStatusChanged().execute();
        }
    }


    /**
     * 
     * This Method start the contact picker activity and process its result
     */
    protected void addNativeContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_CONTACT);
    }

    private void enableAutoReply(final String message, final String date) {
        new AsyncTask<Void, String, Integer>() {
            ProgressDialog dialog;
            String errorMsg = null;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(context, null, getString(R.string.vma_enabling_auto_reply));
            }

            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    int result = provMng.enableAutoReply(message, date);
                    return result;
                } catch (RuntimeException e) {
                    errorMsg = e.getMessage();
                    return -2;
                }
            };

            protected void onPostExecute(Integer result) {
                dialog.dismiss();
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    finish();
                } else if (result.intValue() == -2) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context,R.string.vma_not_enable_reply, Toast.LENGTH_LONG).show();
                }
            };

        }.execute();
    }

    private void enableAutoForward(final String message, final String date) {
        new AsyncTask<Void, String, Integer>() {
            ProgressDialog dialog;
            String errorMsg = null;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(context, null,getString(R.string.vma_enabling_auto_forward));
            }

            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    int result = provMng.enableAutoforward(message, date);
                    if(result == AppErrorCodes.VMA_PROVISION_OK){
                        try {
                            result = provMng.syncMessagingAssistantsSettings(isReply);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return result;
                } catch (RuntimeException e) {
                    errorMsg = e.getMessage();
                    return -2;
                }

            };

            protected void onPostExecute(Integer result) {
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    updateViewForForward();
                } else if (result.intValue() == -2) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, R.string.vma_not_enable_forward, Toast.LENGTH_LONG).show();
                }
                dialog.dismiss();
            };

        }.execute();
    }

    private void disableAutoReply() {
        new AsyncTask<Void, String, Integer>() {
            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(context, null, getString(R.string.vma_disabling_auto_reply));
            };

            @Override
            protected Integer doInBackground(Void... params) {
                int result = provMng.disableAutoReply();
                return result;
            };

            protected void onPostExecute(Integer result) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("result in disable Auto Reply" + result);
                }
                dialog.dismiss();
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    finish();
                } else {
                    Toast.makeText(context, R.string.vma_not_disable_reply, Toast.LENGTH_LONG).show();
                }
                
            };

        }.execute();
    }

    private void disableAutoForward() {
        new AsyncTask<Void, String, Integer>() {
            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(context, null,getString(R.string.vma_disabling_auto_forward));
            }

            @Override
            protected Integer doInBackground(Void... params) {
                int result = provMng.disableAutoForward();
                if(result == AppErrorCodes.VMA_PROVISION_OK){
                    try {
                        result = provMng.syncMessagingAssistantsSettings(isReply);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return result;
            };

            protected void onPostExecute(Integer result) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("result" + result);
                }
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    updateViewForForward();
                }else {
                    Toast.makeText(context,R.string.vma_not_disable_forward, Toast.LENGTH_LONG).show();
                }
                dialog.dismiss();
            };

        }.execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        requestCode &= 0xffff; // XXX it's sometimes ORed with 0x10000, not sure
        if (requestCode == REQUEST_CODE_PICK_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                Uri result = data.getData();

                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(this + ": Got a contact result: " + result.toString());
                }
                getPhoneContactInfo(data);
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getPhoneContactInfo(Intent data) {
        ContentResolver cr = getContentResolver();
        Uri uri = data.getData();
        Cursor cur = managedQuery(uri, null, null, null, null);
        ArrayList<String> phoneNumbersAndEmails = new ArrayList<String>();
        String contactName;
        String id;
        boolean found = false;

        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {

                id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));

                contactName = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                if (Integer.parseInt(cur.getString(cur
                        .getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    String phoneNo = "";

                    Cursor cursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id },
                            null);
                    while (cursor.moveToNext()) {
                        found = true;
                        phoneNo = cursor.getString(cursor
                                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phoneNumbersAndEmails.add(PhoneNumberUtils.stripSeparators(phoneNo));
                    }
                    cursor.close();

                } 

				String emailIdOfContact = "";
				Cursor emails = cr.query(Email.CONTENT_URI, null,
						Email.CONTACT_ID + " = ?", new String[] { id }, null);
				while (emails.moveToNext()) {
					found = true;
					emailIdOfContact = emails.getString(emails
							.getColumnIndex(Email.DATA));
					phoneNumbersAndEmails.add(emailIdOfContact);
				}
				emails.close();

                if (found) {
                    setAvailableContactInfo(new ArrayList<String>(phoneNumbersAndEmails), contactName, id);
                }
            }
        }
        // cursor returned zero count, could be a facebook contact
        if (!found && !OEM.isIceCreamSandwich) {
            String contactId = uri.getLastPathSegment();
            String name = null;
            Uri contentUri = Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI,
                    contactId + "/data");
            ArrayList<String> phoneNumber = new ArrayList<String>();
            ArrayList<String> emailAddress = new ArrayList<String>();
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
                        } else {
                            emailAddress.add(dataCursor.getString(0));
                        }
                    } while (dataCursor.moveToNext());
                }
                if (phoneNumber.size() > 0) {
                    setAvailableContactInfo(phoneNumber, name, contactId);
                } else {
                    setAvailableContactInfo(phoneNumber, name, contactId);
                }
            } catch (Exception e) {
                Logger.error("getPhoneContactInfo " + data, e);
            }
            if (dataCursor != null) {
                dataCursor.close();
            }
        }
    }

    private void setAvailableContactInfo(ArrayList<String> phoneDetails, String contactName, String id) {

        if (phoneDetails.size() == 0) {
            final AppAlignedDialog build = new AppAlignedDialog(context, R.drawable.dialog_alert,
                    R.string.no_recepients, R.string.vma_no_valid_address);
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
			if (checkContact(phoneDetails.get(0))) {
				selectedContact = phoneDetails.get(0);
				showComposeDialog(phoneDetails.get(0));
	//			Util.showKeyboard(ReplyForwardActivity.this, mEnterData);
			} else {
				notifyInvalidAddr(contactName);
        	}
        } else {
            displayAvailableContactInfo(phoneDetails, contactName, id);
        }
    }

    private boolean checkContact(String number) {
		return MessageUtils.isValidMmsAddress(number);
	}

    public void notifyInvalidAddr(String contactName) {
		String msg = getString(R.string.vma_invalid_address_msg,
				contactName);
		final AppAlignedDialog d = new AppAlignedDialog(context,
				R.drawable.dialog_alert, R.string.vma_invalid_address, msg);
		Button saveButton = (Button) d.findViewById(R.id.positive_button);
		saveButton.setText(R.string.yes);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!TextUtils.isEmpty(selectedContact)){
					selectedContact = null;
				}	
				d.dismiss();
			}
		});
		d.show();
	}

	private void displayAvailableContactInfo(ArrayList<String> phoneList, final String contactName,
            final String contactId) {

        final String[] items = phoneList.toArray(new String[phoneList.size()]);
        final AppAlignedDialog d = new AppAlignedDialog(R.drawable.dialog_alert, contactName,
                context);
        
        ListView mListView = (ListView) d.findViewById(R.id.dialog_msg);
        mListView.setAdapter(new ArrayAdapter<String>(context, R.layout.single_choice_list, items));
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String attendeeDetail = items[arg2];
				if (checkContact(attendeeDetail)) {
					selectedContact = attendeeDetail;
					showComposeDialog(attendeeDetail);
				} else {
					notifyInvalidAddr(contactName);
				}
				d.dismiss();

            }
        });
        d.show();
    }
    
  

		protected ArrayList<String> getMessage(){
			if (isReply) {
				ArrayList<String> autoReplyUsedMsg = new ArrayList<String>();
				Cursor cur = managedQuery(RecentlyUsedReplyMsg.CONTENT_URI,
						null, null, null, null);
				if (cur.getCount() > 0) {
					if (cur.moveToFirst()) {
						do {
							autoReplyUsedMsg.add(cur.getString(cur
											.getColumnIndex(AppSettings.KEY_VMA_AUTOREPLYUSEDMSGS)));
						} while (cur.moveToNext());
					}
				}

				autoReplyUsedMsg.add(getString(R.string.vma_reply_default_text));
				return autoReplyUsedMsg;
			} else {
				ArrayList<String> autoFwdUsedAddr = new ArrayList<String>();
				Cursor cur = managedQuery(RecentlyUsedFwdAddr.CONTENT_URI,
						null, null, null, null);
				if (cur.getCount() > 0) {
					if (cur.moveToFirst()) {
						do {
							autoFwdUsedAddr.add(cur.getString(cur
											.getColumnIndex(AppSettings.KEY_VMA_AUTOFORWARDUSEDADDR)));
						} while (cur.moveToNext());
					}
				}
				autoFwdUsedAddr.add(getString(R.string.vma_forward_desc));
				return autoFwdUsedAddr;
			}
		}
		
		private class ReplyForwardListener implements View.OnClickListener {
		    String result;
		    ReplyForwardListener(String data){
		        result = data;
		    }
            @Override
            public void onClick(View v) {
                if(isReply){
                    if(!result.equals(getString(R.string.vma_reply_default_text))){
                        //setSelected(result);
                    }
                }
                else
                {
                    if(!result.equals(getString(R.string.vma_forward_desc))){
                        setSelected(result);
                    }
                }
               
                showComposeDialog(result);
            }
		    
		}

}
