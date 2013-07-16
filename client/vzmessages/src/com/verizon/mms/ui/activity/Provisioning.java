/**
 * Provisioning.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.activity;

import static android.content.res.Configuration.KEYBOARDHIDDEN_NO;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vma.provision.ProvisionManager;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.sync.AppErrorCodes;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.DeviceConfig.OEM;
import com.verizon.mms.transaction.MessagingNotification;
import com.verizon.mms.ui.AppAlignedDialog;
import com.verizon.mms.ui.ConversationListFragment;
import com.verizon.mms.ui.VZMActivity;
import com.verizon.mms.ui.widget.VZDialog;
import com.verizon.mms.util.BitmapManager;
import com.verizon.mms.util.Util;
import com.verizon.sync.SyncManager;
import com.vzw.vma.common.message.StringUtils;

/**
 * This class is used to start the VMA provisioning flow.
 * 
 * @author Jegadeesan M
 * @Since Oct 12, 2012
 */
public class Provisioning extends VZMActivity {

    private boolean isTablet;
    private ProvisionManager provisionManager;
    private String mdn;
    private String devicemodel;
    private String pin;
    private Context context = this;
    private boolean simulateMdn = false;
    private AppSettings settings;
    private static final int MENU_DUMP_DB = 2105;
    private static final int MENU_DUMP_LOG = 2106;
    private static final int MENU_EMAIL_TRACES = 2114;
    private static final int MENU_EMAIL_MEM = 2115;
    private AsyncTask tabletAsyncTask;
    private boolean mIsLandscape; // Whether we're in landscape mode
    private boolean mIsKeyboardOpen;
    private int dialogId = 0;
    private AppAlignedDialog dialog;
    private ErrorHandler errorHandler;

    /**
     * Dont prepare the dialog if we are doing configuration change only
     */
    boolean prepareDialog = true;

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = ApplicationSettings.getInstance();
        isTablet = MmsConfig.isTabletDevice();
        errorHandler = new ErrorHandler(context);
        View v = new View(this);
        v.setBackgroundColor(Color.WHITE);
        setContentView(v);
        Configuration config = getResources().getConfiguration();
        mIsLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE;
        mIsKeyboardOpen = config.keyboardHidden == KEYBOARDHIDDEN_NO;
        provisionManager = new ProvisionManager(this);

        // Validating the MDN on Handset
        if (!isTablet && settings.isProvisioned() && settings.isMdnChanged()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("MDN changed : reseting the VMA setting.");
            }
            settings.resetVMASettings();
        }

        if (settings.isProvisioned()
                || settings.getBooleanSetting(AppSettings.KEY_VMA_TAB_OFFLINE_MODE, false)) {
            settings.put(AppSettings.KEY_VMA_ACCEPT_TERMS, true);

            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "Provisiong thinks we are prov or in offline mode");
            }
            showConversationListScreen();
        } else {
            if (isTablet) {
                getProvisioningDialog(getProvioningDialogLayout()).show();
                return;
            }
            if (settings.getBooleanSetting(AppSettings.KEY_VMA_ACCEPT_TERMS, false)) {
                showConversationListScreen();
                return;
            }
            getProvisioningDialog(getProvioningDialogLayout()).show();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mIsLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        mIsKeyboardOpen = newConfig.keyboardHidden == KEYBOARDHIDDEN_NO;
        if (dialogId == 0) {
            return;
        }
        if (dialogId == 1) {
            if (dialog != null) {
                dialog.dismiss();
            }
            getProvisioningDialog(getProvioningDialogLayout()).show();
        } else if (dialogId == 2) {
            prepareDialog = false;
            dismissDialog(VZDialog.VMA_REQUEST_PRIMARY_MDN);
            showDialog(VZDialog.VMA_REQUEST_PRIMARY_MDN);
        } else if (dialogId == 3) {
            prepareDialog = false;
            dismissDialog(VZDialog.VMA_GET_PASSWORD);
            showDialog(VZDialog.VMA_GET_PASSWORD);
        }
    }

    /**
     * This Method
     */
    public void showConversationListScreen() {
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "call showConversationListScreen");
    	}
        Intent intent = new Intent(this, ConversationListActivity.class);
        if (getIntent() != null && getIntent().getBooleanExtra(ConversationListFragment.IS_WIDGET, false)) {
            intent.putExtra(ConversationListFragment.IS_WIDGET, true);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    	finish();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Provisioning Activity onDestroy called");
        }
        if (tabletAsyncTask != null) {
            tabletAsyncTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        dialogId = 0;
        if (id == VZDialog.VMA_GET_PASSWORD) {
            dialogId = 3;
            TextView middleText = (TextView) dialog.findViewById(R.id.middleText);
            if (mIsKeyboardOpen && mIsLandscape) {
                middleText.setWidth(750);
            } else {
                middleText.setWidth(500);
            }
            if (prepareDialog) {
                TextView tv = (TextView) dialog.findViewById(R.id.security_code);
                tv.setVisibility(View.VISIBLE);
                String formatedMdn;
                if (mdn == null) {
                    String lastEnterdMdn = settings.getStringSettings(AppSettings.KEY_VMA_ENTERD_MDN);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(),
                                "mdn is null, so taking MDN last entered mdn from Setting in onPrepareDialog:"
                                        + lastEnterdMdn);
                    }
                    mdn = lastEnterdMdn;
                }
                if (mdn.length() == 10) {
                    formatedMdn = String.format("(%s) %s-%s", mdn.substring(0, 3), mdn.substring(3, 6),
                            mdn.substring(6, 10));
                } else {
                    formatedMdn = String.format("(%s) %s-%s", mdn, "", "");
                }
                tv.setText(getString(R.string.vma_security_text, formatedMdn));
                final EditText passwordEntry = (EditText) dialog.findViewById(R.id.vzm_dialog_username);
                passwordEntry.setWidth(250);
                passwordEntry.setText("");
                passwordEntry.setError(null);
            }
            prepareDialog = true;
        } else if (id == VZDialog.VMA_REQUEST_PRIMARY_MDN) {
            dialogId = 2;
            if (prepareDialog) {
                final EditText mdnEntry = (EditText) dialog.findViewById(R.id.vzm_dialog_mdn);
                mdnEntry.setText("");
            }
            prepareDialog = true;
            TextView middleText = (TextView) dialog.findViewById(R.id.middleText);
            if (mIsKeyboardOpen && mIsLandscape) {
                middleText.setWidth(750);
            } else {
                middleText.setWidth(500);
            }
        }

        super.onPrepareDialog(id, dialog);
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        dialogId = 0;
        if (id == VZDialog.VMA_REQUEST_PRIMARY_MDN) {
            dialogId = 2;
            int layoutId = 0;
            if (OEM.isSmallScreen) {
                layoutId = R.layout.handsetlogin;
            } else {
                layoutId = R.layout.tabletlogin;
            }
            final Provisioningdialog dialog = new Provisioningdialog(context, layoutId);
            final EditText mdnEntry = (EditText) dialog.findViewById(R.id.vzm_dialog_mdn);
            final EditText deviceEntry = (EditText) dialog.findViewById(R.id.vzm_dialog_username);
            final View progressBar = dialog.findViewById(R.id.mdn_progressBar);
            final Button sendtext = (Button) dialog.findViewById(R.id.send_mdn);
            sendtext.setEnabled(false);
            deviceEntry.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

            TextView middleText = (TextView) dialog.findViewById(R.id.middleText);
            if (mIsKeyboardOpen && mIsLandscape) {
                middleText.setWidth(750);
            } else {
                middleText.setWidth(500);
            }
            if (OEM.isSmallScreen) {
                dialog.findViewById(R.id.third_row).setVisibility(View.GONE);
                dialog.findViewById(R.id.fourth_row).setVisibility(View.VISIBLE);
            }
            mdnEntry.addTextChangedListener(new EditTextWatcher(9, sendtext));
            sendtext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mdn = mdnEntry.getText().toString();
                    devicemodel = deviceEntry.getText().toString();
                    if (!StringUtils.isEmpty(mdn)) {
                        if (simulateMdn) {
                            ApplicationSettings settings = ApplicationSettings.getInstance();
                            settings.put(ApplicationSettings.KEY_VMA_TOKEN, "000000");
                            settings.put(ApplicationSettings.KEY_VMA_MDN, mdn);
                        } else {
                            if (mdn.length() == 10) {
                                settings.put(AppSettings.KEY_VMA_ENTERD_MDN, mdn);
                                progressBar.setVisibility(View.VISIBLE);
                                sendtext.setEnabled(false);
                                requestSMSPin(sendtext, progressBar);
                            } else {
                                mdnEntry.setError("Invalid Mobile Number");
                            }
                        }

                    } else {
                        mdnEntry.selectAll();
                        mdnEntry.setError(getText(R.string.enter_credentials));
                    }
                }
            });
            dialog.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    if (tabletAsyncTask != null) {
                        tabletAsyncTask.cancel(true);
                    }
                    finish();
                    moveTaskToBack(true);
                }
            });
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                    finish();
                    moveTaskToBack(true);
                }
            });
            String mdn = null;
            // mdn = "9253243014";// "JEGA";
            // mdn = "9738000890";// "JEGA Local";
            // mdn = "9253248960";// "SANDY";
            // mdn = "9253248951";// "SANDY";
            // mdn = "9253248963";// "JEGA";
            // mdn = "9258081895";// "JEGA";
            // mdn = "9258178127";// "JEGA";
            // mdn = "9253247930";// " ASHA";
            if (simulateMdn) {
                mdnEntry.setText(mdn);
            }
            deviceEntry.setText(Build.MANUFACTURER + "-" + Build.MODEL);
            mdnEntry.setFocusable(true);
            mdnEntry.requestFocus();
            return dialog;

        } else if (id == VZDialog.VMA_GET_PASSWORD) {
            dialogId = 3;
            int layoutId = 0;
            if (OEM.isSmallScreen) {
                layoutId = R.layout.handsetlogin;
            } else {
                layoutId = R.layout.tabletlogin;
            }
            
            if (settings.getBooleanSetting(AppSettings.KEY_VMA_NOTVZWMDN)) {
                // Temp fix: cleanup old flag 
                settings.put(AppSettings.KEY_VMA_NOTVZWMDN ,false);
            }
            
            final Provisioningdialog password = new Provisioningdialog(context, layoutId);

            TextView tv = (TextView) password.findViewById(R.id.security_code);
            tv.setVisibility(View.VISIBLE);
            final View progressBar = password.findViewById(R.id.mdn_progressBar);
            String formatedMdn;
            if (mdn == null) {
                String lastEnterdMdn = settings.getStringSettings(AppSettings.KEY_VMA_ENTERD_MDN);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "mdn is null, so taking MDN last entered mdn from Setting:"
                            + lastEnterdMdn);
                }
                mdn = lastEnterdMdn;
            }
            if (mdn.length() == 10) {
                formatedMdn = String.format("(%s) %s-%s", mdn.substring(0, 3), mdn.substring(3, 6),
                        mdn.substring(6, 10));
            } else {
                formatedMdn = String.format("(%s) %s-%s", mdn, "", "");
            }
            tv.setText(getString(R.string.vma_security_text, formatedMdn));

            password.findViewById(R.id.second_row).setVisibility(View.GONE);
            password.findViewById(R.id.third_row).setVisibility(View.GONE);

            final Button done = (Button) password.findViewById(R.id.send_mdn);
            done.setText("Done");
            TextView middleText = (TextView) password.findViewById(R.id.middleText);
            middleText.setText(getString(R.string.vma_security_code));
            if (mIsKeyboardOpen && mIsLandscape) {
                middleText.setWidth(750);
            } else {
                middleText.setWidth(500);
            }
            TextView firstColumn = (TextView) password.findViewById(R.id.first_column);
            firstColumn.setText(getString(R.string.vma_security_code) + "\u003A");
            final EditText passwordEntry = (EditText) password.findViewById(R.id.vzm_dialog_username);
            passwordEntry.addTextChangedListener(new EditTextWatcher(5, done));
            passwordEntry.setWidth(250);
            passwordEntry.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            passwordEntry.setTransformationMethod(PasswordTransformationMethod.getInstance());
            done.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    done.setEnabled(false);
                    String password = passwordEntry.getText().toString();
                    if (!StringUtils.isEmpty(password) && password.length() == 6) {
                        validatePin(password, passwordEntry, progressBar, done);
                    } else {
                        passwordEntry.selectAll();
                        passwordEntry.setError(getString(R.string.vma_enter_valid_pin));
                        done.setEnabled(true);
                    }
                }
            });
            password.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    password.dismiss();
                    if (tabletAsyncTask != null) {
                        tabletAsyncTask.cancel(true);
                    }
                    showDialog(VZDialog.VMA_REQUEST_PRIMARY_MDN);
                }
            });
            password.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                    showDialog(VZDialog.VMA_REQUEST_PRIMARY_MDN);
                }
            });
            return password;

        } else if (id == VZDialog.VMA_TABLET_TOS) {
            final Provisioningdialog dialog = new Provisioningdialog(context, R.layout.termsandconditions);
            TextView message = (TextView) dialog.findViewById(R.id.dialog_message);
            message.setText(R.string.menu_vma_terms_of_service);
            message.setMovementMethod(LinkMovementMethod.getInstance());
            Button positiveButton = (Button) dialog.findViewById(R.id.positive_button);
            positiveButton.setText(getString(R.string.yes));
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getProvisioningDialog(getProvioningDialogLayout()).show();
                    dialog.dismiss();
                }
            });
            dialog.findViewById(R.id.negative_button).setVisibility(View.GONE);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    getProvisioningDialog(getProvioningDialogLayout()).show();
                    dialog.dismiss();
                }
            });
            dialog.findViewById(R.id.notice).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent("android.intent.action.VIEW", Uri
                            .parse("http://www.apache.org/licenses/"));
                    startActivity(viewIntent);

                }
            });

            return dialog;
        } else if (id == VZDialog.VMA_REQUEST_VMA_SUBSCRIPTION) {
            final AppAlignedDialog dialog = new AppAlignedDialog(this, 0, R.string.confirm,
                    R.string.vma_request_subscription);
            Button pButton = (Button) dialog.findViewById(R.id.positive_button);
            pButton.setText(R.string.yes);
            pButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    provisionTablet();
                    dialog.dismiss();
                }
            });

            Button nButton = (Button) dialog.findViewById(R.id.negative_button);
            nButton.setVisibility(View.VISIBLE);
            nButton.setText(R.string.no);
            nButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "R.string.vma_request_subscription");
                    }
                    deProvisioning();
                    dialog.dismiss();
                }
            });
            return dialog;
        } else if (id == VZDialog.VMA_HANDSET_TOS) {
            final AppAlignedDialog dialog = new AppAlignedDialog(context, R.layout.handset_termsandcondition);
            TextView message = (TextView) dialog.findViewById(R.id.dialog_message);
            message.setText(R.string.menu_vma_terms_of_service);
            message.setMovementMethod(LinkMovementMethod.getInstance());
            Button positiveButton = (Button) dialog.findViewById(R.id.positive_button);
            positiveButton.setText(getString(R.string.yes));
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getProvisioningDialog(getProvioningDialogLayout()).show();
                    dialog.dismiss();
                }
            });
            dialog.findViewById(R.id.negative_button).setVisibility(View.GONE);
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    getProvisioningDialog(getProvioningDialogLayout()).show();
                    dialog.dismiss();
                }
            });
            dialog.findViewById(R.id.notice).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent viewIntent = new Intent("android.intent.action.VIEW", Uri
                            .parse("http://www.apache.org/licenses/"));
                    startActivity(viewIntent);

                }
            });

            return dialog;

        }
        return super.onCreateDialog(id);

    }

    private int getProvioningDialogLayout() {
        int id = 0;
        if (mIsLandscape) {
            if (isTablet && !DeviceConfig.OEM.isSamsungGalaxyCamera
                    && !DeviceConfig.OEM.deviceModel.equalsIgnoreCase("SCH-I800") && !OEM.isSmallScreen) {
                id = R.layout.accept_decline_layout_land_tab;
            } else {
                id = R.layout.accept_decline_layout_land;
            }

        } else {
            if (isTablet && !DeviceConfig.OEM.isSamsungGalaxyCamera
                    && !DeviceConfig.OEM.deviceModel.equalsIgnoreCase("SCH-I800") && !OEM.isSmallScreen) {
                id = R.layout.accept_decline_layout_port_tab;
            } else {
                id = R.layout.accept_decline_layout;
            }

        }
        return id;
    }

    private class EditTextWatcher implements TextWatcher {

        int enableCount;
        Button view;

        public EditTextWatcher(int eCount, Button view) {
            this.enableCount = eCount;
            this.view = view;
        }

        @Override
        public void afterTextChanged(Editable s) {
            int count = s.length();
            if (count > enableCount) {
                view.setEnabled(true);
            } else {
                if (view.isEnabled()) {
                    view.setEnabled(false);
                }
            }

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

    }

    private AppAlignedDialog getProvisioningDialog(int layoutID) {
        dialogId = 1;
        dialog = new AppAlignedDialog(context, layoutID);

        TextView tcText = (TextView) dialog.findViewById(R.id.terms_message);
        Button acceptButton = (Button) dialog.findViewById(R.id.prov_acceptButton);
        Button declineButton = (Button) dialog.findViewById(R.id.prov_declineButton);
        clickify(dialog, tcText, getString(R.string.vma_terms_conditions));
        dialog.setCancelable(false);
        acceptButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                settings.put(AppSettings.KEY_VMA_ACCEPT_TERMS, true);
                if (isTablet) {
                    settings.put(AppSettings.KEY_VMA_TAB_OFFLINE_MODE, false);
                    showDialog(VZDialog.VMA_REQUEST_PRIMARY_MDN);
                } else {
                    settings.put(AppSettings.KEY_VMA_SHOW_PROVISION_SERVICE, true);
                    Intent intent = new Intent(context, ConversationListActivity.class);
                    if (getIntent() != null
                            && getIntent().getBooleanExtra(ConversationListFragment.IS_WIDGET, false)) {
                        intent.putExtra(ConversationListFragment.IS_WIDGET, true);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            }
        });
        declineButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                settings.put(ApplicationSettings.KEY_VMA_PROVISIONED, String.valueOf(0));
                settings.put(AppSettings.KEY_VMA_ACCEPT_TERMS, false);
                finish();
                /**
                 * Move the task containing this activity to the back of the activity stack. This is done so
                 * that after Canceling the first screen, it dont show Conversation list which will be on
                 * Activity Stack if launched from Setting
                 */
                moveTaskToBack(true);
            }
        });
        return dialog;
    }

    /**
     * This Method
     */
    protected void deProvisioning() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "deProvisioning()");
        }
        new AsyncTask<Void, String, Integer>() {
            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(context, null, getString(R.string.vma_deprovisioning_device));
            }

            @Override
            protected Integer doInBackground(Void... params) {
                // return provisionManager.deleteVMAAccount();
                ApplicationSettings.getInstance().resetVMASettings();
                return AppErrorCodes.VMA_PROVISION_OK;
            };

            protected void onPostExecute(Integer result) {
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    dialog.dismiss();
                    if (isTablet) {
                        finish();
                    } else {
                        showConversationListScreen();
                    }
                } else {
                    handleError(result);
                }
            };

        }.execute();

    }

    /**
     * This Method
     * 
     * @param password
     * @param done
     * @param dialog
     */
    protected void validatePin(String password, final EditText passwordEntry, final View progressBar,
            final Button done) {
        pin = password;
        tabletAsyncTask = new AsyncTask<Void, String, Integer>() {
            protected void onPreExecute() {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Integer doInBackground(Void... params) {
                return provisionManager.isValidLoginPIN(mdn, pin);
            };

            protected void onPostExecute(Integer result) {
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    isVMASubscriber(progressBar, done);
                } else if (result.intValue() == AppErrorCodes.VMA_PROVISION_FAIL) {
                    passwordEntry.setError(getString(R.string.vma_enter_valid_pin));
                    progressBar.setVisibility(View.GONE);
                    done.setEnabled(true);
                } else {
                    progressBar.setVisibility(View.GONE);
                    done.setEnabled(true);
                    handleError(result);
                }
            };

        }.execute();
    }

    /**
     * This Method
     * 
     * @param button
     * @param progressBar
     */
    private void requestSMSPin(final Button button, final View progressBar) {
        tabletAsyncTask = new AsyncTask<Void, String, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return provisionManager.generatePIN(mdn, devicemodel, false);
            }

            protected void onPostExecute(Integer result) {

                progressBar.setVisibility(View.GONE);
                button.setEnabled(true);
                if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                    showDialog(VZDialog.VMA_GET_PASSWORD);
                    dismissDialog(VZDialog.VMA_REQUEST_PRIMARY_MDN);
                } else {
                    handleError(result);
                }

            }
        }.execute();
    }

    /**
     * This Method
     * 
     * @param string
     * @param string2
     */
    protected void showConfirmationMessage(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        builder.setTitle(title);
        builder.setPositiveButton("Yes", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showDialog(VZDialog.VMA_GET_PASSWORD);
            }
        });
        builder.setNegativeButton("No", new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (isTablet) {
                    finish();
                } else {
                    showConversationListScreen();
                }
            }
        });

        builder.create().show();

    }

    private void isVMASubscriber(final View progressBar, final Button done) {
        tabletAsyncTask = new AsyncTask<Void, String, Integer>() {

            protected void onPreExecute() {
                progressBar.setVisibility(View.VISIBLE);
            };

            @Override
            protected Integer doInBackground(Void... params) {
                return provisionManager.isVMASubscriber(mdn);
            }

            protected void onPostExecute(Integer result) {
                progressBar.setVisibility(View.GONE);

                if (result.intValue() == AppErrorCodes.VMA_PROVISION_VMA_SUBSCRIBER) {
                    dismissDialog(VZDialog.VMA_GET_PASSWORD);
                    showConversationListScreen();
                } else if (result.intValue() == AppErrorCodes.VMA_PROVISION_NOT_VMA_SUBCRIBER) {
                    dismissDialog(VZDialog.VMA_GET_PASSWORD);
                    provisionTablet();
                } else {
                    done.setEnabled(true);
                    handleError(result);
                }
            }
        }.execute();
    }

    private void handleError(Integer result) {
        ErrorHandler handler = new ErrorHandler(context);
        handler.showAlertDialog(result.intValue());
    }

    private void provisionTablet() {
        tabletAsyncTask = new AsyncTask<Void, String, Integer>() {
            ProgressDialog dialog;

            protected void onPreExecute() {
                dialog = ProgressDialog.show(Provisioning.this, null, getString(R.string.vma_register_txt));
            }

            @Override
            protected Integer doInBackground(Void... params) {
                return provisionManager.doProvision();
            };

            protected void onPostExecute(Integer result) {
                if (simulateMdn) {
                    showConversationListScreen();
                } else {
                    if (result.intValue() == AppErrorCodes.VMA_PROVISION_OK) {
                        showConversationListScreen();
                    } else {
                        finish();
                        handleError(result);
                    }
                }
                dialog.dismiss();
            };

        }.execute();

    }

    private class Provisioningdialog extends Dialog {

        public Provisioningdialog(Context context, int layoutId) {
            super(context, R.style.ThemeDialog);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(layoutId);
            getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        }

        public boolean onCreateOptionsMenu(Menu menu) {

            if (MmsConfig.isTabletDevice()) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(Provisioning.class.getSimpleName(), "its Provisioning activity...");
                    menu.add(0, MENU_DUMP_DB, 0, "Dump DB");
                    menu.add(0, MENU_DUMP_LOG, 0, "Dump Log");
                    menu.add(0, MENU_EMAIL_TRACES, 0, "Email traces.txt");
                    menu.add(0, MENU_EMAIL_MEM, 0, "Email Memory Info");
                }
            }
            return super.onCreateOptionsMenu(menu);
        }

        public boolean onMenuItemSelected(int featureId, MenuItem item) {

            switch (item.getItemId()) {
            case MENU_DUMP_DB:
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Provisioning", "dumpDB is running...");
                }
                Util.saveDb(context);
                return true;

            case MENU_DUMP_LOG:
                final String fname = Util.saveLocalLog(context);
                Toast.makeText(context, fname == null ? "Error saving log" : "Saved log to " + fname,
                        Toast.LENGTH_LONG).show();
                return true;

            case MENU_EMAIL_TRACES:
                Util.emailTraces(context);
                return true;

            case MENU_EMAIL_MEM:
                Util.email(context, "pjeffe@gmail.com", "memory info", BitmapManager.INSTANCE.checkMemory(),
                        false, false);
                return true;

            default:
                break;
            }
            return super.onOptionsItemSelected(item);
        }

    }

    public class ClickSpan extends ClickableSpan {
        final AppAlignedDialog provDialog;

        @Override
        public void onClick(View view) {
            provDialog.dismiss();
            dialogId = 0;
            if (isTablet && !OEM.isSmallScreen) {
                showDialog(VZDialog.VMA_TABLET_TOS);
            } else {
                showDialog(VZDialog.VMA_HANDSET_TOS);
            }
        }

        public ClickSpan(AppAlignedDialog provDialog2) {
            this.provDialog = provDialog2;
        }

    }

    private void clickify(AppAlignedDialog provDialog2, TextView view, final String clickableText) {

        CharSequence text = view.getText();
        String string = text.toString();
        ClickSpan span = new ClickSpan(provDialog2);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
        int start = string.indexOf(clickableText);
        int end = start + clickableText.length();
        if (start == -1)
            return;

        if (text instanceof Spannable) {
            ((Spannable) text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((Spannable) text).setSpan(colorSpan, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        } else {
            SpannableString s = SpannableString.valueOf(text);
            s.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(colorSpan, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            view.setText(s);
        }

        MovementMethod m = view.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            view.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
}
