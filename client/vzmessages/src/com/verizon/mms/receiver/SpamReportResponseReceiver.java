package com.verizon.mms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.strumsoft.android.commons.logger.Logger;

public class SpamReportResponseReceiver extends BroadcastReceiver {

    public static final String CUSTOM_INTENT = com.cloudmark.spamrep.util.Util.SPAM_REP_INTENT;
    int mReportId;
    boolean mResponseStatus;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(SpamReportResponseReceiver.class, "inside onReceive()");
        }

        if (intent.getExtras() != null) {
            mResponseStatus = intent.getBooleanExtra(com.cloudmark.spamrep.util.Util.REP_EXTRA_SUCCESS,
                    mResponseStatus);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(SpamReportResponseReceiver.class, "mResponseStatus: " + mResponseStatus);
            }
            mReportId = intent.getIntExtra(com.cloudmark.spamrep.util.Util.REP_EXTRA_ID, -1);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(SpamReportResponseReceiver.class, "mReportId: " + mReportId);
            }
        }
    }
}