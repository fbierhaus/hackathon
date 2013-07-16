package com.verizon.common;

import android.net.Uri;

public class VZUris {
    private static boolean isTablet;
    private static Uri smsUri;
    private static Uri smsStatusUri;
    private static Uri smsQueuedUri;
    private static Uri smsInboxUri;
    private static Uri smsSentUri;
    private static Uri smsDraftUri;
    private static Uri smsOutboxUri;
    private static Uri smsConversations;
    private static Uri threadIdUri;
    private static Uri mmsSmsConversationUri;
    private static Uri mmsUri;
    private static Uri mmsInboxUri;
    private static Uri mmsSentUri;
    private static Uri mmsDraftsUri;
    private static Uri mmsReportStatusUri;
    private static Uri mmsReportRequestUri;
    private static Uri mmsRateUri;
    private static Uri mmsOutboxUri;
    private static Uri scrapSpaceUri;
    private static Uri mmsSmsUri;
    private static Uri byPhoneUri;
    private static Uri undelivered;
    private static Uri mmsSmsDraftUri;
    private static Uri mmsSmsLocked;
    private static Uri mmsSmsSearch;
    private static Uri mmsSmsCanonical;
    private static Uri telephonyCarriers;
    private static Uri mmsSmsPendingUri;
    private static Uri nativeMmsSmsPendingUri;
    private static String mmsSmsAuthority;
    private static String mmsAuthority;
    private static String smsAuthority;
    private static Uri mmsSmsCanonicalAddresses;
    private static Uri threadsUri;

    private static final String VZM_PREFIX = "vzm-";

    private VZUris() {
    }

    public static void init(boolean isTablet) {
        VZUris.isTablet = isTablet;

        final String prefix = (isTablet ? VZM_PREFIX : "");
        final String contentPrefix = "content://" + prefix;

        mmsSmsAuthority = prefix + "mms-sms";
        mmsAuthority = prefix + "mms";
        smsAuthority = prefix + "sms";
        smsUri = Uri.parse(contentPrefix + "sms");
        smsStatusUri = Uri.parse(contentPrefix + "sms/status");
        smsQueuedUri = Uri.parse(contentPrefix + "sms/queued");
        smsInboxUri = Uri.parse(contentPrefix + "sms/inbox");
        smsSentUri = Uri.parse(contentPrefix + "sms/sent");
        smsDraftUri = Uri.parse(contentPrefix + "sms/draft");
        smsOutboxUri = Uri.parse(contentPrefix + "sms/outbox");
        smsConversations = Uri.parse(contentPrefix + "sms/conversations");
        threadIdUri = Uri.parse(contentPrefix + "mms-sms/threadID");
        mmsSmsConversationUri = Uri.parse(contentPrefix + "mms-sms/conversations");
        mmsUri = Uri.parse(contentPrefix + "mms");
        mmsInboxUri = Uri.parse(contentPrefix + "mms/inbox");
        mmsSentUri = Uri.parse(contentPrefix + "mms/sent");
        mmsDraftsUri = Uri.parse(contentPrefix + "mms/drafts");
        mmsReportStatusUri = Uri.parse(contentPrefix + "mms/report-status");
        mmsReportRequestUri = Uri.parse(contentPrefix + "mms/report-request");
        mmsRateUri = Uri.parse(contentPrefix + "mms/rate");
        mmsOutboxUri = Uri.parse(contentPrefix + "mms/outbox");
        scrapSpaceUri = Uri.parse(contentPrefix + "verizon/scrapSpace");
        mmsSmsUri = Uri.parse(contentPrefix + "mms-sms/");
        byPhoneUri = Uri.parse(contentPrefix + "mms-sms/messages/byphone");
        undelivered = Uri.parse(contentPrefix + "mms-sms/undelivered");
        mmsSmsDraftUri = Uri.parse(contentPrefix + "mms-sms/draft");
        mmsSmsLocked = Uri.parse(contentPrefix + "mms-sms/locked");
        mmsSmsSearch = Uri.parse(contentPrefix + "mms-sms/search");
        mmsSmsCanonical = Uri.parse(contentPrefix + "mms-sms/canonical-address");
        mmsSmsCanonicalAddresses = Uri.parse(contentPrefix + "mms-sms/canonical-addresses");
        telephonyCarriers = Uri.parse(contentPrefix + "telephony/carriers");
        mmsSmsPendingUri = Uri.parse("content://" + VZM_PREFIX + "mms-sms/pending");
        nativeMmsSmsPendingUri = Uri.parse(contentPrefix + "mms-sms/pending");
        threadsUri = mmsSmsConversationUri.buildUpon().appendQueryParameter("simple", "true").build();
    }

    public static boolean isTabletDevice() {
        return isTablet;
    }

    public static Uri getSmsUri() {
        return smsUri;
    }

    public static Uri getSmsStatusUri() {
        return smsStatusUri;
    }

    public static Uri getSmsQueuedUri() {
        return smsQueuedUri;
    }

    public static Uri getSmsInboxUri() {
        return smsInboxUri;
    }

    public static Uri getSmsSentUri() {
        return smsSentUri;
    }

    public static Uri getSmsDraftUri() {
        return smsDraftUri;
    }

    public static Uri getSmsOutboxUri() {
        return smsOutboxUri;
    }

    public static Uri getSmsConversations() {
        return smsConversations;
    }

    public static Uri getThreadIdUri() {
        return threadIdUri;
    }

    public static Uri getMmsSmsConversationUri() {
        return mmsSmsConversationUri;
    }

    public static Uri getMmsUri() {
        return mmsUri;
    }

    public static Uri getMmsInboxUri() {
        return mmsInboxUri;
    }

    public static Uri getMmsSentUri() {
        return mmsSentUri;
    }

    public static Uri getMmsDraftsUri() {
        return mmsDraftsUri;
    }

    public static Uri getMmsReportStatusUri() {
        return mmsReportStatusUri;
    }

    public static Uri getMmsReportRequestUri() {
        return mmsReportRequestUri;
    }

    public static Uri getMmsRateUri() {
        return mmsRateUri;
    }

    public static Uri getMmsOutboxUri() {
        return mmsOutboxUri;
    }

    public static Uri getScrapSpaceUri() {
        return scrapSpaceUri;
    }

    public static Uri getMmsSmsUri() {
        return mmsSmsUri;
    }

    public static Uri getByPhoneUri() {
        return byPhoneUri;
    }

    public static Uri getUndelivered() {
        return undelivered;
    }

    public static Uri getMmsSmsDraftUri() {
        return mmsSmsDraftUri;
    }

    public static Uri getMmsSmsLocked() {
        return mmsSmsLocked;
    }

    public static Uri getMmsSmsSearch() {
        return mmsSmsSearch;
    }

    public static Uri getMmsSmsCanonical() {
        return mmsSmsCanonical;
    }

    public static Uri getMmsSmsCanonicalAddresses() {
        return mmsSmsCanonicalAddresses;
    }

    public static Uri getTelephonyCarriers() {
        return telephonyCarriers;
    }

    public static Uri getMmsSmsPendingUri() {
        return mmsSmsPendingUri;
    }

    public static Uri getNativeMmsSmsPendingUri() {
        return nativeMmsSmsPendingUri;
    }

    public static String getMmsSmsAuthority() {
        return mmsSmsAuthority;
    }

    public static String getMmsAuthority() {
        return mmsAuthority;
    }

    public static String getSmsAuthority() {
        return smsAuthority;
    }

    public static Uri getMmsPartUri() {
        return Uri.parse(mmsUri + "/part/");
    }

    public static Uri getMmsPartsUri(long msgId) {
        return Uri.parse(mmsUri + "/" + msgId + "/part");
    }

    public static Uri getMmsAddrUri(long msgId) {
        return Uri.parse(mmsUri + "/" + msgId + "/addr");
    }

    public static Uri getThreadsUri() {
        return threadsUri;
    }

    /**
     * This Method
     * 
     * @return
     */
    public static Uri getObsoleteThreadsUri() {
        return Uri.withAppendedPath(VZUris.getMmsSmsConversationUri(), "obsolete");
    }

    public static boolean isMmsUri(Uri uri) {
        if (VZUris.getMmsAuthority().equalsIgnoreCase(uri.getAuthority())) {
            return true;
        }
        return false;
    }

    public static boolean isSmsUri(Uri uri) {
        if (VZUris.getSmsAuthority().equalsIgnoreCase(uri.getAuthority())) {
            return true;
        }
        return false;
    }

    public static boolean isConversationUri(Uri uri) {
        if (VZUris.getMmsSmsAuthority().equalsIgnoreCase(uri.getAuthority())) {
            return true;
        }
        return false;
    }

}
