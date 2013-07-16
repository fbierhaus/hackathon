/**
 * VZDialog.java
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

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.Window;

import com.verizon.messaging.vzmsgs.R;


/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jul 14, 2012
 */
public class DialogFactory implements OnClickListener {
    public static final int TOS_DIALOG = 1;
    public static final int LOGIN_DIALOG = 2;
    public static final int ERROR_DIALOG = 3;
    public static final int POSITIVE_BUTTON=-1;
    public static final int NEGATIVE_BUTTON=-2;

    public static Dialog getDialog(Context context, int dialogId, OnClickListener listener) {
        Builder builder = new Builder(context);
        switch (dialogId) {
        case TOS_DIALOG:
            builder.setMessage(readTextFile(context, R.raw.termsandconditions));
            builder.setCancelable(false);
            builder.setTitle(R.string.tnc_title);
            builder.setPositiveButton(R.string.tnc_agree, listener);
            builder.setNegativeButton(R.string.tnc_disagree, listener);
            break;
        case ERROR_DIALOG:
            builder.setMessage(readTextFile(context, R.raw.termsandconditions));
            builder.setCancelable(false);
            builder.setTitle(R.string.tnc_title);
            builder.setPositiveButton(R.string.tnc_agree, listener);
            builder.setNegativeButton(R.string.tnc_disagree, listener);
            break;
         case LOGIN_DIALOG:
             
             break;


        default:
            break;
        }

        return builder.create();

    }
    
    public class CustomDialog extends Dialog{

        /**
         * @param context
         *  Constructor 
         */
        public CustomDialog(Context context) {
            super(context, android.R.style.Theme_Translucent_NoTitleBar);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.message_details);
            
        }
        
    }
    

    public static String readTextFile(Context context, int resId) {
        InputStream inputStream = context.getResources().openRawResource(resId);
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

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
    }

}
