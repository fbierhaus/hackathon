package com.verizon.mms.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.activity.Provisioning;
import com.verizon.sync.SyncController;

public class AppAlertDialog extends VZMActivity {

    public final static String EXTRA_DIALOG_TYPE = "dialog_type";
    public final static String EXTRA_TITLE = "dialog_title";
    public final static String EXTRA_MESSAGE = "dialog_message";

    public final static int DIALOG_OK_BUTTON = 0;
    public final static int DIALOG_BROWSE_OFFLINE_RECONNECT = 1;
    public final static int DIALOG_BROWSE_OFFLINE_RETRY = 2;

    private ApplicationSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        int theme = MessageUtils.getTheme(this);

        if (theme == R.style.Theme_Large) {
            setTheme(R.style.PopUpDialogLarge);
        } else {
            setTheme(R.style.PopUpDialogNormal);
        }
        super.onCreate(savedInstanceState , false);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Transparent background 
        FrameLayout f = new FrameLayout(this);
        f.setBackgroundColor(Color.TRANSPARENT);
        setContentView(f);
        
        settings = ApplicationSettings.getInstance();

        Intent intent = getIntent();
        int dialogCode = intent.getIntExtra(EXTRA_DIALOG_TYPE, -1);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String msg = intent.getStringExtra(EXTRA_MESSAGE);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "onCreate() error=" + dialogCode);
        }

        if (dialogCode == DIALOG_OK_BUTTON) {
            showOkButtonDialog(title, msg);
        } else if (dialogCode == DIALOG_BROWSE_OFFLINE_RECONNECT) {
            showBrowseOfflineAndReconnectDialog(title, msg);
        } else if (dialogCode == DIALOG_BROWSE_OFFLINE_RETRY) {
            showBrowseOfflineAndRetryDialog(title, msg);
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.warn(getClass(), "onCreate() default else block." + dialogCode);
            }
            finish();
        }

    }

    private void resetTablet() {
        new AsyncTask<Void, String, Void>() {
            ProgressDialog dialog;

            protected void onPreExecute() {

                dialog = ProgressDialog.show(AppAlertDialog.this, null,
                        getString(R.string.vma_erasing_disconnecting));
            }

            @Override
            protected Void doInBackground(Void... params) {
                settings.resetTablet();
                return null;
            };

            protected void onPostExecute(Void result) {
                dialog.dismiss();
            	if(Logger.IS_DEBUG_ENABLED) {
        			Logger.debug(getClass(),"starting Provisioning activity from AppAlertDialog");
        		}
                
                VZActivityHelper.closePausedActivityOnStack();
                Intent intent = new Intent(AppAlertDialog.this, Provisioning.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            };

        }.execute();
    }

    public void showConversationListScreen() {
//        startService(new Intent(SyncManager.ACTION_START_VMA_SYNC));
        Intent intent = new Intent(this, ConversationListActivity.class);
        if (getIntent() != null && getIntent().getBooleanExtra(ConversationListFragment.IS_WIDGET, false)) {
            intent.putExtra(ConversationListFragment.IS_WIDGET, true);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    // New methods

    // private void showAlert(int statuscode) {
    //
    // /**
    // * ACCOUNT_INVALID: - This state invalidates the user's login token.<br>
    // * A0 NO LOGIN failed.<br>
    // * MSA_MAILBOX_DOES_NOT_EXIST [408]:<br>
    // * MSA_NOT_A_VMA_SUBSCRIBER [450]:<br>
    // * MSA_MISSING_LOGIN_OR_PASSWORD[400]:<br>
    // * ACCOUNT_SUSPENDED - This state stop sync with VMA during this session, we attempt sync when user
    // * restarts the app<br>
    // * MSA_ACCOUNT_SUSPENDED [452]:<br>
    // * MSA_HAS_SMS_BLOCKING [451]:<br>
    // * MSA_SUBSCRIBER_HAS_INSUFFICIENT_FUNDS[458]:<br>
    // * RETRY_WITH_DELAY - Transient errors on either server or client, or recover from logic errors in
    // * code.<br>
    // * MSA_SESSION_WRONG_STATE[402]:<br>
    // * MSA_ANOTHER_COMMAND_ALREADY_IN_PROGRESS [403]<br>
    // * ANY other error code that we do not explicitly handle or a no response from server e.g. a
    // * disconnect<br>
    // * NOT APPLICABLEr TO LOGIN - If we get this or any other error, on login we will retry after a
    // delay<br>
    // * MSA_FAILED_ANTISPAM_CHECK[454]<br>
    // * MSA_SUBSCRIBER_HAS_MMS_BLOCKING[453]:<br>
    // * MSA_ISTD_NOT_SUPPORTED [460]<br>
    // */
    //
    // String msg = getString(R.string.unknown_error);
    // String title = getString(R.string.error);
    // switch (statuscode) {
    //
    // case VMAStore.MSA_NOT_A_VMA_SUBSCRIBER:
    // case VMAStore.MSA_MAILBOX_DOES_NOT_EXIST:
    // case VMAStore.MSA_MISSING_LOGIN_OR_PASSWORD:
    // // A0 NO LOGIN failed.
    // case VMAStore.MSA_LOGIN_FAILED:
    // title = getString(R.string.vma_disconnected);
    // if (MmsConfig.isTabletDevice()) {
    // msg = getString(R.string.vma_error_450);
    // showBrowseOfflineAndReconnectDialog(statuscode, title, msg);
    // } else {
    // // Reset the handset and show alert dialog.
    // msg = getString(R.string.vma_account_deactivated);
    // resetHandset(statuscode, title, msg);
    // }
    // break;
    //
    // case VMAStore.MSA_HAS_SMS_BLOCKING:
    // title = getString(R.string.vma_account_suspended);
    // msg = getString(R.string.vma_error_451);
    // showBrowseOfflineAndRetryDialog(statuscode, title, msg);
    // break;
    //
    // case VMAStore.MSA_ACCOUNT_SUSPENDED:
    // title = getString(R.string.vma_account_suspended);
    // msg = getString(R.string.vma_error_452);
    // showBrowseOfflineAndRetryDialog(statuscode, title, msg);
    // break;
    // case VMAStore.MSA_SUBSCRIBER_HAS_INSUFFICIENT_FUNDS:
    // title = getString(R.string.vma_account_suspended);
    // msg = getString(R.string.vma_error_458);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    // case VMAStore.MSA_SUBSCRIBER_HAS_MMS_BLOCKING:
    // title = getString(R.string.vma_account_suspended);
    // msg = getString(R.string.vma_error_453);
    // showBrowseOfflineAndRetryDialog(statuscode, title, msg);
    // break;
    // case VMAStore.MSA_FAILED_ANTISPAM_CHECK:
    // msg = getString(R.string.vma_error_454);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    // case VMAStore.MSA_ISTD_NOT_SUPPORTED:
    // msg = getString(R.string.vma_istd_not_supported);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case ProvisionManager.RESPONSE_NOTELIGIBLE:
    // msg = getString(R.string.vma_noteligible);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.RESPONSE_NOTVZWMDN:
    // msg = getString(R.string.vma_notvzwmdn);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    // case VMAProvision.RESPONSE_OVERLIMIT:
    // msg = getString(R.string.vma_overlimit);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.RESPONSE_VBLOCK:
    // msg = getString(R.string.vma_vblock);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.RESPONSE_SUSPENDED:
    // msg = getString(R.string.vma_suspended);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.HANDSET_AUTO_PROVISIONED:
    // title = getString(R.string.vma_welcome_screen_title);
    // msg = getString(R.string.vma_welcome_text);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.RESPONSE_FAIL:
    // msg = getString(R.string.vma_fails);
    // showOkButtonDialog(statuscode, title, msg);
    //
    // break;
    //
    // case VMAProvision.RESPONSE_EXCEEDDEVICELIMIT:
    // msg = getString(R.string.vma_exceed_device_limit);
    // showOkButtonDialog(statuscode, title, msg);
    //
    // break;
    //
    // case VMAProvision.RESPONSE_NETWORK_ERROR:
    // if (isFromProvision && !MmsConfig.isTabletDevice()) {
    // msg = getString(R.string.vma_provision_noNetwork);
    // } else {
    // msg = getString(R.string.cant_attach);
    // }
    // showOkButtonDialog(statuscode, title, msg);
    //
    // break;
    //
    // case VMAProvision.RESPONSE_NOT_VMA_SUBCRIBER:
    // msg = getString(R.string.vma_not_susbscriber);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.RESPONSE_PIN_RETRIEVAL_FAILED:
    // msg = getString(R.string.vma_pin_retrival_failed);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.RESPONSE_INVALID_MDN:
    // if (MmsConfig.isTabletDevice()) {
    // msg = getString(R.string.vma_invalid_address);
    // } else {
    // msg = getString(R.string.vma_account_deactivated);
    // }
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.RESPONSE_DUPLICATE_DESTINATION:
    // msg = getString(R.string.vma_duplicate);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.RESPONSE_ALREADY_AUTO_FORWARDED:
    // msg = getString(R.string.vma_already_autoforward);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.RESPONSE_INVALID_END_DATE:
    // msg = getString(R.string.vma_invalid_date);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    // case VMAProvision.RESPONSE_INTERNAL_SERVER_ERROR:
    // msg = getString(R.string.vma_internal);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    // case VMAProvision.UNSUBSCRIBE_ERROR:
    // msg = getString(R.string.vma_unable_toUnSubscribe);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    //
    // case VMAProvision.RESPONSE_LOGIN_FAIL:
    // msg = "Server Login Fail";
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    // case VMAProvision.RESPONSE_ERROR:
    // msg = getString(R.string.vma_provision_unknown);
    // showOkButtonDialog(statuscode, title, msg);
    // break;
    // default:
    // if (Logger.IS_DEBUG_ENABLED) {
    // Logger.debug("Unknown error :code=" + statuscode);
    // }
    // finish();
    // break;
    // }
    // }

    private void showBrowseOfflineAndReconnectDialog(String title, String message) {
        final Dialog dialog = new AppAlignedDialog(this, 0, title, message);
        dialog.setCancelable(false);
        Button offlineButton = (Button) dialog.findViewById(R.id.negative_button);
        Button retryButton = (Button) dialog.findViewById(R.id.positive_button);
        offlineButton.setVisibility(View.VISIBLE);
        offlineButton.setText(R.string.vma_browse_offline);

        // Sync retry
        retryButton.setText(R.string.vma_erase_reconnect);
        retryButton.setVisibility(View.VISIBLE);

        offlineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Browse offline button clicked");
                }
                dialog.dismiss();
                enableOfflineMode();
            }
        });
        //
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "Reconnect button clicked.");
                    Logger.debug(getClass(), "Resting the tablet.");
                }
                settings.setVMALoginFailed(false);
                resetTablet();
            }
        });
        dialog.show();
    }

    private void showBrowseOfflineAndRetryDialog(String title, String message) {
        final Dialog dialog = new AppAlignedDialog(this, 0, title, message);
        dialog.setCancelable(false);
        Button offlineButton = (Button) dialog.findViewById(R.id.negative_button);
        Button retryButton = (Button) dialog.findViewById(R.id.positive_button);
        offlineButton.setVisibility(View.VISIBLE);
        offlineButton.setText(R.string.vma_browse_offline);

        // Sync retry
        retryButton.setText(R.string.vma_retry_sync);
        retryButton.setVisibility(View.VISIBLE);

        offlineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("offline mode button clicked");
                }
                dialog.dismiss();
                enableOfflineMode();
            }
        });
        //
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "Retry button clicked");
                }
                finish();
                settings.setVMAAccountSuspended(false);
                SyncController.getInstance().startVMASync();

            }
        });
        dialog.show();
    }

    private void enableOfflineMode() {
        settings.setTabletInOfflineMode(true);
        settings.put(AppSettings.KEY_VMA_DONT_SHOW_DIALOG, false);
        showConversationListScreen();
    }

    private void showOkButtonDialog(String title, String message) {

        final Dialog d = new AppAlignedDialog(AppAlertDialog.this, R.drawable.launcher_home_icon,
                R.string.vma_server_error, message);
        d.setCancelable(false);
        Button okButton = (Button) d.findViewById(R.id.positive_button);
        okButton.setText(R.string.button_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("showOkButtonDialog(): ok button clicked.");
                }
                d.dismiss();
                finish();
            }
        });
        d.show();
    }

}
