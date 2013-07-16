/**
 * ErrorHandler.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vma.provision.ProvisionManager;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.sync.AppErrorCodes;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.ui.AppAlertDialog;

/**
 * This class is used to handle all the errors on Ui.
 * 
 * @author Jegadeesan M
 * @Since Apr 1, 2013
 */
public class ErrorHandler implements AppErrorCodes {

    private Context context;
    private AppSettings settings;

    /**
     * 
     * Constructor
     */
    public ErrorHandler(Context context) {
        this.context = context;
        this.settings = ApplicationSettings.getInstance(context);
    }

    /**
     * This Method
     * 
     * @param i
     */
    public void showAlertDialog(int statuscode) {
        /**
         * ACCOUNT_INVALID: - This state invalidates the user's login token.<br>
         * A0 NO LOGIN failed.<br>
         * MSA_MAILBOX_DOES_NOT_EXIST [408]:<br>
         * MSA_NOT_A_VMA_SUBSCRIBER [450]:<br>
         * MSA_MISSING_LOGIN_OR_PASSWORD[400]:<br>
         * ACCOUNT_SUSPENDED - This state stop sync with VMA during this session, we attempt sync when user
         * restarts the app<br>
         * MSA_ACCOUNT_SUSPENDED [452]:<br>
         * MSA_HAS_SMS_BLOCKING [451]:<br>
         * MSA_SUBSCRIBER_HAS_INSUFFICIENT_FUNDS[458]:<br>
         * RETRY_WITH_DELAY - Transient errors on either server or client, or recover from logic errors in
         * code.<br>
         * MSA_SESSION_WRONG_STATE[402]:<br>
         * MSA_ANOTHER_COMMAND_ALREADY_IN_PROGRESS [403]<br>
         * ANY other error code that we do not explicitly handle or a no response from server e.g. a
         * disconnect<br>
         * NOT APPLICABLEr TO LOGIN - If we get this or any other error, on login we will retry after a delay<br>
         * MSA_FAILED_ANTISPAM_CHECK[454]<br>
         * MSA_SUBSCRIBER_HAS_MMS_BLOCKING[453]:<br>
         * MSA_ISTD_NOT_SUPPORTED [460]<br>
         */
        String msg = context.getString(R.string.unknown_error);
        String title = context.getString(R.string.error);

        switch (statuscode) {

        case VMA_SYNC_NOT_A_VMA_SUBSCRIBER:
        case VMA_SYNC_MAILBOX_DOES_NOT_EXIST:
        case VMA_SYNC_MISSING_LOGIN_OR_PASSWORD:
        case VMA_SYNC_OTHER_PERMANENT_FAILURE:
            // A0 NO LOGIN failed.
        case VMA_SYNC_LOGIN_FAILED:
            title = context.getString(R.string.vma_disconnected);
            if (!settings.isVMALoginFailed()) {
                settings.setVMALoginFailed(true);
                if (MmsConfig.isTabletDevice()) {
                    msg = context.getString(R.string.vma_error_450);
                    showBrowseOfflineAndReconnectDialog(title, msg);

                } else {
                    resetHandset();
                }
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "ignoring the multiple Broadcast.");
                }
            }
            break;

        case VMA_PROVISION_NOTEXT:
            title = context.getString(R.string.vma_account_suspended);
            msg = context.getString(R.string.vma_error_451);
            showOkButtonDialog(title, msg);
            break;
        case VMA_SYNC_HAS_SMS_BLOCKING:
            title = context.getString(R.string.vma_account_suspended);
            msg = context.getString(R.string.vma_error_451);
            showBrowseOfflineAndRetryDialog(title, msg);
            break;

        case VMA_SYNC_ACCOUNT_SUSPENDED:
            title = context.getString(R.string.vma_account_suspended);
            msg = context.getString(R.string.vma_error_452);
            showBrowseOfflineAndRetryDialog(title, msg);
            break;

        case VMA_SYNC_SUBSCRIBER_HAS_INSUFFICIENT_FUNDS:
            title = context.getString(R.string.vma_account_suspended);
            msg = context.getString(R.string.vma_error_458);
            showOkButtonDialog(title, msg);
            break;
        case VMA_PROVISION_NOMMS:
            title = context.getString(R.string.vma_account_suspended);
            msg = context.getString(R.string.vma_error_453);
            showOkButtonDialog(title, msg);
            break;
        case VMA_SYNC_SUBSCRIBER_HAS_MMS_BLOCKING:
            title = context.getString(R.string.vma_account_suspended);
            msg = context.getString(R.string.vma_error_453);
            showBrowseOfflineAndRetryDialog(title, msg);
            break;
        case VMA_SYNC_FAILED_ANTISPAM_CHECK:
            msg = context.getString(R.string.vma_error_454);
            showOkButtonDialog(title, msg);
            break;
        case VMA_SYNC_ISTD_NOT_SUPPORTED:
            msg = context.getString(R.string.vma_istd_not_supported);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_NOTELIGIBLE:
            msg = context.getString(R.string.vma_noteligible);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_NOTVZWMDN:
            msg = context.getString(R.string.vma_notvzwmdn);
            showOkButtonDialog(title, msg);
            break;
        case VMA_PROVISION_OVERLIMIT:
            msg = context.getString(R.string.vma_overlimit);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_VBLOCK:
            msg = context.getString(R.string.vma_vblock);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_SUSPENDED:
            msg = context.getString(R.string.vma_suspended);
            showOkButtonDialog(title, msg);
            break;

        // case VMAProvision.HANDSET_AUTO_PROVISIONED:
        // title = context.getString(R.string.vma_welcome_screen_title);
        // msg = context.getString(R.string.vma_welcome_text);
        // showOkButtonDialog( title, msg);
        // break;

        case VMA_PROVISION_FAIL:
            msg = context.getString(R.string.vma_fails);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_EXCEEDDEVICELIMIT:
            msg = context.getString(R.string.vma_exceed_device_limit);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_NETWORK_ERROR:
            if (!MmsConfig.isTabletDevice()) {
                msg = context.getString(R.string.vma_provision_noNetwork);
            } else {
                msg = context.getString(R.string.cant_attach);
            }
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_NOT_VMA_SUBCRIBER:
            msg = context.getString(R.string.vma_not_susbscriber);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_PIN_RETRIEVAL_FAILED:
            msg = context.getString(R.string.vma_pin_retrival_failed);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_INVALID_MDN:
            if (MmsConfig.isTabletDevice()) {
                msg = context.getString(R.string.vma_invalid_address);
            } else {
                msg = context.getString(R.string.vma_account_deactivated);
            }
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_DUPLICATE_DESTINATION:
            msg = context.getString(R.string.vma_duplicate);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_ALREADY_AUTO_FORWARDED:
            msg = context.getString(R.string.vma_already_autoforward);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_INVALID_END_DATE:
            msg = context.getString(R.string.vma_invalid_date);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_INTERNAL_SERVER_ERROR:
            msg = context.getString(R.string.vma_internal);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_UNSUBSCRIBE_ERROR:
            msg = context.getString(R.string.vma_unable_toUnSubscribe);
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_LOGIN_FAIL:
            msg = "Server Login Fail";
            showOkButtonDialog(title, msg);
            break;

        case VMA_PROVISION_ERROR:
            msg = context.getString(R.string.vma_provision_unknown);
            showOkButtonDialog(title, msg);
            break;

        case VMA_REMOVE_DEVICE_ERROR:
            msg = context.getString(R.string.vma_remove_device_error);
            showOkButtonDialog(title, msg);
            break;
        case VMA_LOW_MEMORY:
            msg = context.getString(R.string.vma_low_memory);
            showOkButtonDialog(title, msg);
            break;

        default:
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Unknown error :code=" + statuscode);
            }
            break;
        }

    }

    private void showDialog(int type, String title, String msg) {
        Intent alertDialog = new Intent(context, AppAlertDialog.class);
        alertDialog.putExtra(AppAlertDialog.EXTRA_DIALOG_TYPE, type);
        alertDialog.putExtra(AppAlertDialog.EXTRA_TITLE, title);
        alertDialog.putExtra(AppAlertDialog.EXTRA_MESSAGE, msg);
        context.startActivity(alertDialog);
    }

    /**
     * This Method
     * 
     * @param title
     * @param msg
     */
    private void showOkButtonDialog(String title, String msg) {
        showDialog(AppAlertDialog.DIALOG_OK_BUTTON, title, msg);
    }

    /**
     * This Method
     * 
     * @param title
     * @param msg
     */
    private void showBrowseOfflineAndRetryDialog(String title, String msg) {
        if (!settings.isVMAAccountSuspended()) {
            settings.setVMAAccountSuspended(true);
            showDialog(AppAlertDialog.DIALOG_BROWSE_OFFLINE_RETRY, title, msg);
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "Already shown ignoring the multiple broadcast");
            }
        }
    }

    /**
     * This Method
     * 
     * @param title
     * @param msg
     */
    private void showBrowseOfflineAndReconnectDialog(String title, String msg) {
        showDialog(AppAlertDialog.DIALOG_BROWSE_OFFLINE_RECONNECT, title, msg);
    }

    private void resetHandset() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "ResetHandset()");
        }
        new AsyncTask<Void, String, Void>() {
            ProgressDialog dialog;

            protected void onPreExecute() {

                dialog = ProgressDialog.show(context, null,
                        context.getString(R.string.vma_erasing_disconnecting_handset));
            }

            @Override
            protected Void doInBackground(Void... params) {
                ApplicationSettings.getInstance().resetVMASettings();
                return null;
            };

            protected void onPostExecute(Void result) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "ResetHandset() - postExecute");
                }
                ApplicationSettings.getInstance().put(AppSettings.KEY_VMA_ACCEPT_TERMS, true);
                dialog.dismiss();
                String title = context.getString(R.string.warning);
                String msg = context.getString(R.string.vma_account_deactivated);
                showOkButtonDialog(title, msg);
            };

        }.execute();
    }

}
