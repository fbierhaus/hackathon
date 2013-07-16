package com.verizon.mms.util;

import android.content.Context;

import com.cloudmark.spamrep.MessageReporter;
import com.cloudmark.spamrep.MessageReporter.ErrVal;
import com.cloudmark.spamrep.SLog;
import com.cloudmark.spamrep.Settings.Option;
import com.strumsoft.android.commons.logger.Logger;

public class SpamReportInitialization {

	// for Strumsoft signed builds
    //private static final String app_auth = "a9f2f31c59eaabadbaceda6bcda0a3e1c0d07f026aa48ec512917196fe31ea73c32ca341f7bb1dcdd1e9a2ecbd12aff1d4862134d2f7b38870cd2ec0e41e97a0a95418d2423692ba566e05cc59558443cc52068a14d9a9ee08195b39dc3ccda998899ad1";

    // for vz signed builds
    private static final String app_auth = "2006f3b98aa73265dafdb3ad85e87e22dd2d6cbaf7435da8ebc6aa857c92431a06e4e862d78922ab84fbcffd1c658792e9dafb27748e941ce181e7202da45e26e9b55aed1f6e9a2256be351e11f59e5e6383054e4baa1c41e4659c96ad374941589dea4d";
    	
    public static void initialize(Context context) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(SpamReportInitialization.class, "inside intialize()");
        }
        try {
            MessageReporter reporter = MessageReporter.getInstance(context);
            if (Logger.IS_DEBUG_ENABLED) {
            	reporter.setConfigOption(Option.LOG_LEVEL, SLog.LogFilter.ALL);
                Logger.debug(SpamReportInitialization.class, "Message Reporter: " + reporter + "spamLib:" +
                		com.cloudmark.spamrep.util.Util.LIB_VERSION);
            } else {
            	reporter.setConfigOption(Option.LOG_LEVEL, SLog.LogFilter.NONE);
            }
            ErrVal errVal = reporter.authenticateApplication(com.cloudmark.spamrep.util.Util
                    .fromHexString(app_auth));
            if (errVal != ErrVal.NONE) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(SpamReportInitialization.class,
                            "Failed to authenticate the app: " + errVal.toString());
                }
            }
           
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(SpamReportInitialization.class,
                        "Is the device registered? " + reporter.isDeviceRegistered());
            }

            if (!reporter.isDeviceRegistered()) {
                errVal = reporter.requestDeviceRegistration();
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(SpamReportInitialization.class,
                            "Is Device Registered after sending request? " + reporter.isDeviceRegistered());
                }
            }
        } catch (UnsatisfiedLinkError e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error(SpamReportInitialization.class, "UnsatisfiedLinkError: " + e.getMessage());
            }
        }
    }

}
