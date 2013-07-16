/**
 * CustomDialog.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.widget;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.verizon.messaging.vzmsgs.R;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jul 14, 2012
 */
public class VZDialog extends Dialog implements android.view.View.OnClickListener {

    public static final int DEFAULT = 1;
    public static final int YES_NO = 2;
    public static final int LOGIN = 3;
    public static final int TOS = 4;
    public static final int VMA_REQUEST_PRIMARY_MDN = 5;
    public static final int VMA_GET_PASSWORD = 6;
    public static final int VMA_HANDSET_TOS = 7;
    public static final int VMA_TABLET_TOS = 8;
    public static final int VMA_REQUEST_VMA_SUBSCRIPTION = 9;
    public static final int VZW_HANDSET_TOS = 10;
    public static final int VMA_HANDSET_ACCEPT_DECLINE = 11;
    public static final int VMA_TABLET_ACCEPT_DECLINE = 12;

    public static final int POSITIVE_BUTTON = -1;
    public static final int NEGATIVE_BUTTON = -2;

    private Button rightBtn;
    private Button leftBtn;

    private EditText editText1;
    private EditText editText2;

    private TextView title;
    private TextView text1;


    private OnClickListener listener;

    private int dialogType;

    /**
     * @param context
     *            Constructor
     */
    public VZDialog(Activity context, OnClickListener listener) {
        super(context);
        this.dialogType = DEFAULT;
        this.listener = listener;
        init();

    }

    /**
     * @param context
     *            Constructor
     */
    public VZDialog(Context context) {
        super(context);
        this.dialogType = DEFAULT;
        init();
    }

    public VZDialog(Context context, int dialogType) {
        super(context);
        this.dialogType = dialogType;
        init();
    }

    public String readTextFile(int resId) {
        InputStream inputStream = getContext().getResources().openRawResource(resId);
        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader bufferedreader = new BufferedReader(inputreader);
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            while ((line = bufferedreader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return stringBuilder.toString();
    }

    public VZDialog(Context context, int dialogType, OnClickListener listener) {
        super(context);
        this.dialogType = dialogType;
        this.listener = listener;
        init();
    }

    /**
     * This Method
     * 
     * @param dialogType
     */
    private void init() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        setContentView(R.layout.custom_dialog);
        // Title
        title = (TextView) findViewById(R.id.vzm_dialog_header_title);

        // Message Text
        text1 = (TextView) findViewById(R.id.vzm_dialog_text_1);



        // OnClick Listener
        rightBtn = (Button) findViewById(R.id.vzm_dialog_footer_positive_button);
        leftBtn = (Button) findViewById(R.id.vzm_dialog_footer_negative_button);

        if (dialogType == LOGIN) {
            ((LinearLayout) findViewById(R.id.vzm_dialog_login)).setVisibility(View.VISIBLE);
            ((View) findViewById(R.id.footerseperator)).setVisibility(View.INVISIBLE);
            // Login Dialog
            editText1 = (EditText) findViewById(R.id.vzm_dialog_username);
            editText2 = (EditText) findViewById(R.id.vzm_dialog_password);
        } else if(dialogType ==TOS || dialogType ==VMA_TABLET_TOS|| dialogType ==VMA_HANDSET_TOS){
            ((LinearLayout) findViewById(R.id.vzm_t_c)).setVisibility(View.VISIBLE);
            ((View) findViewById(R.id.footerseperator)).setVisibility(View.INVISIBLE);
            
        } else if(dialogType == VMA_REQUEST_PRIMARY_MDN){
            ((View) findViewById(R.id.footerseperator)).setVisibility(View.INVISIBLE);
            ((LinearLayout) findViewById(R.id.vzm_dialog_request_mdn_devicename)).setVisibility(View.VISIBLE);
            // Login Dialog
            editText1 = (EditText) findViewById(R.id.vzm_dialog_entermdn);
            editText2 = (EditText) findViewById(R.id.vzm_dialog_devicename);
        } else if(dialogType ==VMA_GET_PASSWORD){
            ((View) findViewById(R.id.footerseperator)).setVisibility(View.INVISIBLE);
            ((LinearLayout) findViewById(R.id.vzm_dialog_passwordBox)).setVisibility(View.VISIBLE);
            editText2 = (EditText) findViewById(R.id.vzm_pwd);
        } else {
//            LinearLayout.LayoutParams relativeParams = new LinearLayout.LayoutParams(
//                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
//            relativeParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.footerseperator);
//            ((LinearLayout) findViewById(R.id.vzm_dialog_footer)).setLayoutParams(relativeParams);
            ((LinearLayout) findViewById(R.id.vzm_t_c)).setVisibility(View.VISIBLE);
            ((View) findViewById(R.id.footerseperator)).setVisibility(View.INVISIBLE);
        }
        rightBtn.setOnClickListener(this);
        leftBtn.setOnClickListener(this);
        // Setting hr
        if (dialogType == TOS || dialogType ==VMA_TABLET_TOS|| dialogType ==VMA_HANDSET_TOS ) {
            title.setText(R.string.tnc_title);
            rightBtn.setText(R.string.tnc_agree);
            leftBtn.setText(R.string.tnc_disagree);
        } else if (dialogType == LOGIN) {
            title.setText(R.string.enter_credentials);
            rightBtn.setText(R.string.login);
            leftBtn.setText(R.string.cancel);
        } else if (dialogType == VMA_REQUEST_PRIMARY_MDN) {
            title.setText(R.string.enter_credentials);
            rightBtn.setText(R.string.login);
            leftBtn.setText(R.string.cancel);
        } else if (dialogType == YES_NO || dialogType== VMA_REQUEST_VMA_SUBSCRIPTION) {
            title.setText(R.string.warning);
            rightBtn.setText(R.string.yes_string);
            leftBtn.setText(R.string.no_string);
        } else {
            rightBtn.setText(R.string.ok);
            leftBtn.setText(R.string.cancel);
        }
    }

    /**
     * Returns the Value of the username
     * 
     * @return the {@link EditText}
     */
    public EditText getEditText1() {
        return editText1;
    }

    /**
     * Returns the Value of the password
     * 
     * @return the {@link EditText}
     */
    public EditText getEditText2() {
        return editText2;
    }

    /**
     * Returns the Value of the postiveBtn
     * 
     * @return the {@link Button}
     */
    public Button getRightButton() {
        return rightBtn;
    }

    /**
     * Returns the Value of the negativeBtn
     * 
     * @return the {@link Button}
     */
    public Button getLeftButton() {
        return leftBtn;
    }

    public void setOnClickListener(OnClickListener listener) {
        this.listener = listener;
    }

    /**
     * Set the Value of the field text1
     * 
     * @param msg
     *            the text1 to set
     */
    public void setMessage(String msg) {
        if (dialogType != LOGIN) {
            text1.setText(msg);
        }
    }

    public void setMessage(int resourceId) {
        if (dialogType != LOGIN) {
            text1.setText(resourceId);
        }
    }

    public void setTitle(int resourceId) {
        if (dialogType != LOGIN) {
            title.setText(resourceId);
        }
    }

    public void setTitle(String titleMsg) {
        if (dialogType != LOGIN) {
            title.setText(titleMsg);
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        if (listener != null) {
            int which = 0;
            if (v.getId() == R.id.vzm_dialog_footer_positive_button) {
                which = POSITIVE_BUTTON;
            } else if (v.getId() == R.id.vzm_dialog_footer_negative_button) {
                which = NEGATIVE_BUTTON;
            }
            listener.onClick(this, which);
        }
    }

}
