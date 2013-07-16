package com.verizon.mms.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.Html;
import android.text.util.Linkify;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.verizon.messaging.vzmsgs.R;

/**
 * This class is used to construct the default dialog for reporting error and features of this application.   
 * 
 * 
 */
//[JEGA] TODO ishaque/Naba ,Instead of using one layout we can create header and footer and use it different layout.

public class AppAlignedDialog extends Dialog {

    public AppAlignedDialog(Context context, int iconResource, int titleText, int messageText) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_aligned_dialog);
        setCancelable(true);

        ImageView icon = (ImageView) findViewById(R.id.title_icon);
        if (iconResource != 0) {
            icon.setImageResource(iconResource);
        } else {
            icon.setVisibility(View.GONE);
        }
        TextView title = (TextView) findViewById(R.id.dialog_title);
        title.setText(titleText);

        TextView message = (TextView) findViewById(R.id.dialog_message);
        if (messageText != 0) {
            message.setText(messageText);
        } else {
            message.setVisibility(View.GONE);
        }
    }

    public AppAlignedDialog(Context context, int iconResource, int titleText, String messageText) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_aligned_dialog);
        setCancelable(true);

        ImageView icon = (ImageView) findViewById(R.id.title_icon);
        if (iconResource != 0) {
            icon.setImageResource(iconResource);
        } else {
            icon.setVisibility(View.GONE);
        }
        TextView title = (TextView) findViewById(R.id.dialog_title);
        if (titleText != 0) {
            title.setText(titleText);
        } else {
            title.setVisibility(View.GONE);
        }

        TextView message = (TextView) findViewById(R.id.dialog_message);
        message.setText(messageText);
    }

    public AppAlignedDialog(Activity activity, int iconResource, String titleText, int messageText) {
        super(activity, android.R.style.Theme_Translucent_NoTitleBar);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_aligned_dialog);
        setCancelable(true);

        ImageView icon = (ImageView) findViewById(R.id.title_icon);
        if (iconResource != 0) {
            icon.setImageResource(iconResource);
        } else {
            icon.setVisibility(View.GONE);
        }
        TextView title = (TextView) findViewById(R.id.dialog_title);
        title.setText(titleText);

        TextView message = (TextView) findViewById(R.id.dialog_message);
        message.setText(messageText);
    }

    public AppAlignedDialog(Context context, int iconResource, String titleText, String messageText) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_aligned_dialog);
        setCancelable(true);

        ImageView icon = (ImageView) findViewById(R.id.title_icon);
        if (iconResource != 0) {
            icon.setImageResource(iconResource);
        } else {
            icon.setVisibility(View.GONE);
        }
        TextView title = (TextView) findViewById(R.id.dialog_title);
        if (titleText != null) {
            title.setText(titleText);
        } else {
            findViewById(R.id.divider).setVisibility(View.GONE);
            title.setVisibility(View.GONE);
        }
        TextView message = (TextView) findViewById(R.id.dialog_message);
        message.setText(messageText);
    }

    public AppAlignedDialog(int dialogAlert, String selectphonenumber, Context context) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_algnd_dialog);
        setCancelable(true);

        ImageView icon = (ImageView) findViewById(R.id.title_icon);
        if (dialogAlert != 0) {
            icon.setImageResource(dialogAlert);
        } else {
            icon.setVisibility(View.GONE);
        }
        TextView title = (TextView) findViewById(R.id.dialog_title);
        title.setText(selectphonenumber);
    }

    public AppAlignedDialog(Context context, int iconResource, int titleText , boolean isTablet) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_aligned_dialog_listview);
        setCancelable(true);
        // TODO[NABA/ISHAQUE] using arraylist adapter , we don't have load all the views in memory
        // order and mapping changed for 4.0.1 per vz request - tip1 translation is needed
        TextView tip1 = (TextView) findViewById(R.id.tip2);
        tip1.setText(context.getString(R.string.tip1));
        TextView tip2 = (TextView) findViewById(R.id.tip3);
        tip2.setText(context.getString(R.string.tip2));
        TextView tip3 = (TextView) findViewById(R.id.tip4);
        tip3.setText(context.getString(R.string.tip3));
        TextView tip4 = (TextView) findViewById(R.id.tip5);
        tip4.setText(context.getString(R.string.tip4));
        TextView tip5 = (TextView) findViewById(R.id.tip6);
        tip5.setText(context.getString(R.string.tip5));
        TextView tip6 = (TextView) findViewById(R.id.tip7);
        tip6.setText(context.getString(R.string.tip6));
        
        TextView tip7 = (TextView) findViewById(R.id.tip1);
        if(isTablet){
            tip7.setText(Html.fromHtml(context.getString(R.string.tip_vma_tablet)));
        }else{
            tip7.setText(Html.fromHtml(context.getString(R.string.tip_vma_smartphone)));
        }
        Linkify.addLinks(tip7, Linkify.ALL);
        

        ImageView icon = (ImageView) findViewById(R.id.title_icon);
        if (iconResource != 0) {
            icon.setImageResource(iconResource);
        } else {
            icon.setVisibility(View.GONE);
        }
        TextView title = (TextView) findViewById(R.id.dialog_title);
        title.setText(titleText);
    }

    public AppAlignedDialog(Context context, String titleText, String subTitleText) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_signature_dialog);
        setCancelable(true);
        TextView title = (TextView) findViewById(R.id.dialog_title);
        TextView subtitle = (TextView) findViewById(R.id.signature_heading_title);
        if (titleText != null) {
            title.setText(titleText);
        }
        if (subTitleText != null) {
            subtitle.setText(subTitleText);
        }
    }

    public AppAlignedDialog(Context context, CharSequence title, CharSequence message, boolean indeterminate,
            boolean cancelable) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.app_aligned_progress);
        ImageView icon = (ImageView) findViewById(R.id.title_icon);
        icon.setVisibility(View.GONE);
        TextView titleView = (TextView) findViewById(R.id.dialog_title);
        titleView.setText(title);
        setCancelable(cancelable);
        TextView messageView = (TextView) findViewById(R.id.dialog_message);
        if (message != null) {
            messageView.setText(message);
        } else {
            messageView.setVisibility(View.GONE);
        }

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setIndeterminate(indeterminate);
    }

    public AppAlignedDialog(Context context, int layoutId) {
        super(context, R.style.ThemeDialog);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(layoutId);
        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    }

    public AppAlignedDialog(Context context) {
        super(context, R.style.ThemeDialog);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.accept_decline_layout);

    }
}
