package com.verizon.mms.ui.widget;

import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.ui.AppAlignedDialog;

public class VMAServerType extends ListPreference {
    Context mContext;
    String mClickedDialogEntry;
    ApplicationSettings settings;

    public VMAServerType(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        settings = ApplicationSettings.getInstance();
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        boolean prevServerType = settings.getBooleanSetting(AppSettings.KEY_VMA_SERVER_TYPE, false);
        CharSequence[] values = getEntryValues();
        int selectedentry = 0;
        final String prevSelected = prevServerType ? mContext.getString(R.string.vma_server_prod) : mContext
                .getString(R.string.vma_server_qa);
        for (; selectedentry < values.length; selectedentry++) {
            if (prevSelected.equals(values[selectedentry]))
                break;
        }
        mClickedDialogEntry = prevSelected;

        builder.setSingleChoiceItems(getEntries(), selectedentry, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                CharSequence[] values = getEntryValues();
                mClickedDialogEntry = (String) values[which];
            }
        });
        builder.setPositiveButton(mContext.getString(android.R.string.yes), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (prevSelected.equals(mClickedDialogEntry)) {
                    return;
                }
                final AppAlignedDialog switchServerdialog = new AppAlignedDialog(mContext,
                        R.drawable.dialog_alert, "Switching Server",
                        "Are you sure you want to swich the server. It will remove all your setting");
                Button okButton = (Button) switchServerdialog.findViewById(R.id.positive_button);
                okButton.setText(R.string.yes);
                okButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchServer(mClickedDialogEntry);
                        switchServerdialog.dismiss();
                    }
                });
                Button cancelButton = (Button) switchServerdialog.findViewById(R.id.negative_button);
                cancelButton.setText(R.string.no);
                cancelButton.setVisibility(View.VISIBLE);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchServerdialog.dismiss();
                    }
                });
                switchServerdialog.show();
            }
        });
        builder.setNegativeButton(mContext.getString(android.R.string.no), null);
    }

    private void switchServer(final String dialogEntry) {
        new AsyncTask<Void, String, Void>() {
            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(mContext, null, "Switching Server...");
            }

            @Override
            protected Void doInBackground(Void... params) {
                boolean useProduction;
                if (dialogEntry.equals(mContext.getString(R.string.vma_server_prod))) {
                    useProduction = true;
                } else {
                    useProduction = false;
                }
                if (MmsConfig.isTabletDevice()) {
                    settings.resetTablet();
                }
                settings.switchServer(useProduction);
                return null;
            };

            protected void onPostExecute(Void result) {
                dialog.dismiss();
            };

        }.execute();
    }
}
