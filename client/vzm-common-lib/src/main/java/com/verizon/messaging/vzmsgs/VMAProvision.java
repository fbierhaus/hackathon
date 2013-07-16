/**
 * VMAProvision.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.messaging.vzmsgs;

import java.io.IOException;
import java.util.ArrayList;

import com.verizon.messaging.vzmsgs.provider.Vma.LinkedVMADevices;

/**
 * This class/interface
 * 
 * @author Jegadeesan M
 * @Since Nov 10, 2012
 */
public interface VMAProvision {
    
    public interface ProvisionStatusListener {
        public static final int STATE_GENERATE_PIN=1;
        public static final int STATE_VALIDATE_PIN=2;
        public static final int STATE_VERIFY_SUBSCRIPTION=3;
        public static final int STATE_CREATE_MAIL_BOX=4;
        public void onStatusChanged(int state , int status);
    }
    
    public static final String SENDER_ID = "135713301837";

//    public static final int RESPONSE_UNKNOWN = 0;
//    public static final int RESPONSE_OK = 1;
//    public static final int RESPONSE_NOTVZWMDN = 2;
//    public static final int RESPONSE_NOTELIGIBLE = 3;
//    public static final int RESPONSE_OVERLIMIT = 4;
//    public static final int RESPONSE_ERROR = 5;
//    public static final int RESPONSE_FAIL = 6;
//    public static final int RESPONSE_SUSPENDED = 7;
//    public static final int RESPONSE_VBLOCK = 8;
//    public static final int RESPONSE_EXCEEDDEVICELIMIT = 9;
//    public static final int RESPONSE_VMA_SUBSCRIBER = 10;
//    public static final int RESPONSE_NOT_VMA_SUBCRIBER = 11;
//    public static final int RESPONSE_INVALID_MDN = 12;
//    public static final int RESPONSE_DUPLICATE_DESTINATION = 13;
//    public static final int RESPONSE_ALREADY_AUTO_FORWARDED = 14;
//    public static final int RESPONSE_INVALID_END_DATE = 15;
//    public static final int RESPONSE_NETWORK_ERROR = 16;
//    public static final int RESPONSE_INTERNAL_SERVER_ERROR = 17;
//    public static final int RESPONSE_PIN_RETRIEVAL_FAILED = 18;
//    public static final int RESPONSE_LOGIN_FAIL = 19;

    public static final int ACTION_ASSISTANT_QUERY = 1000;
//    public static final int ACTION_REGISTER_GCM = 1002;
    public static final int ACTION_PUSH_GCMID_TO_VMASERVER = 1003;
    public static final int ACTION_AUTO_PROVISION_HANDSET = 1004;

    /**
     * This Method The VMA User Query API is used to query if a user (MDN) is registered as VMA user. URL
     * http://vmadevui.pdi.vzw.com/services/ngm_user_query (lab) Method GET Parameter mdn NOTE: need to
     * discuss the encryption of the mdn Result YES indicating the user is registered as a VMA user NO
     * indicating the user is registered as a VMA user
     * 
     * @param mdn
     * @return
     */
    public int isVMASubscriber(String mdn);

    /**
     * This API is used by tablet clients as the first step in PIN-based authentication. This method will
     * generate a PIN, which is sent to the device with the supplied MDN. The user is allowed a finite number
     * of PIN requests (default is 3) per hour. If this threshold is exceeded by one or more devices, VMA will
     * reject all further requests for a period of time (default is 1 hour).
     * 
     * @param mdn
     * @param deviceModel
     * @return
     */
    public int generatePIN(String mdn, String deviceModel, boolean isPrimary);

    public int isValidLoginPIN(String mdn, String password);

    /**
     * This API is used to update push notification (C2DM and APNS) Registration ID for an MDN in VMA. URL
     * http://vmadevui.pdi.vzw.com/services/pushId (lab) Method GET Parameter mdn loginToken type A APNS, G
     * C2DM oldRegistrationId Optional, need to be provided if old ID exists in order for VMA to clean old ID
     * to avoid unnecessary push notification to be sent RegistrationId Registration Id from CD2M or APNS
     * Result (JSON) status OK update succeeds, FAIL login fails, ERROR error statusInfo optional, detailed
     * info about status
     * 
     * @param registrationId
     * @return
     */
    public int registerGCMToken(String registrationId);

    /**
     * This API is used to provision a valid vzw.com account user to be a VMA user. The VZ Messages client in
     * a tablet should provision a user to be a VMA user upon successful login and accepting VMA T&Cs. URL
     * http://vmadevui.pdi.vzw.com/services/vmaProvisioning (lab) Method GET Parameter mdn loginToken Result
     * (JSON) status OK provisioning succeeds, FAIL login fails, VBLOCK user is blocked from using this
     * service, ERROR error statusInfo optional, detailed info about status.
     * 
     * @param mdn
     * @param loginToken
     * @return {@link Boolean}
     */
    public int doProvision();

    /**
     * This API is used to query VMA to get user details for Messaging Assistant features (Auto-Forward and
     * Auto-Reply). URL http://vmadevui.pdi.vzw.com/services/AssistantQuery (lab) 11 Verizon Wireless
     * Proprietary and Confidential Information Method GET Parameter mdn loginToken Result (JSON) status OK
     * query succeeds, FAIL login fails, ERROR error statusInfo optional, detailed info about status.
     * autoForwardStatus Auto-forward status: PENDING, ACTIVE, INACTIVE, EXPIRED autoForwardAddr Auto-forward
     * address autoForwardEndDate Auto-forward end date autoReplyStatus Auto-reply status: ACTIVE, INACTIVE,
     * EXPIRED autoReplyMsg Auto-reply message autoReplyEndDate Auto-reply end date autoReplyUsedMsgs Array of
     * last 5 auto-reply messages used by the users
     * 
     * @return {@link Integer}
     */
    public int syncMessagingAssistantsSettings() throws IOException;

    public int syncMessagingAssistantsSettings(boolean isReply) throws IOException;

    /**
     * This API is used to set Auto-Forward feature. URL http:// <host:port>/services/AssistantAutoFwd Method
     * GET Parameters mdn loginToken autoForwardAddr Auto-forward address autoForwardEndDate Auto-forward end
     * date Result (JSON) status OK – provisioning succeeds FAIL – login fails VBLOCK – user is blocked from
     * using this service ERROR – error statusInfo optional, detailed info about status: Invalid Address –
     * auto-forward address is invalid Invalid End Date – end date is invalid This Method
     * 
     * @param address
     * @param endDate
     * @return
     */
    public int enableAutoforward(String address, String endDate);

    /**
     * This API is used to set Auto-Reply feature. URL http:// <host:port>/services/ AssistantAutoReply Method
     * GET Parameters mdn loginToken autoReplyMsg Auto-reply message autoReplyEndDate Auto-reply end date
     * Result (JSON) status OK – provisioning succeeds FAIL – login fails VBLOCK – user is blocked from using
     * this service ERROR – error statusInfo optional, detailed info about status: Message Too Long – message
     * is longer than 160 characters Invalid End Date – end date is invalid This Method
     * 
     * @param message
     * @param endDate
     * @return
     */
    public int enableAutoReply(String message, String endDate);

    /**
     * 6.10 Disable Auto-Forward API This API is used to disable Auto-Forward feature. 16 Verizon Wireless –
     * Proprietary and Confidential Information URL http:// <host:port>/services/ AssistantAutoFwdDisable
     * Method GET Parameters mdn loginToken Result (JSON) status OK – disable auto-forward succeeds FAIL –
     * login fails ERROR – error statusInfo optional, detailed info about status This Method
     * 
     * @return
     */

    public int disableAutoForward();

    /**
     * This API is used to disable Auto-Reply feature. URL http:// <host:port>/services/
     * AssistantAutoReplyDisable Method GET Parameters mdn loginToken Result (JSON) status OK – disable
     * Auto-Reply succeeds FAIL – login fails ERROR – error statusInfo optional, detailed info about status
     * This Method
     * 
     * @return {@link Integer}
     */
    public int disableAutoReply();

    /**
     * This API is used to de-provision a valid VMA user (delete subscriber from VMA and delete messages from
     * message store). The VZ Messages client in a tablet should de-provision a user upon successful login and
     * accepting warning about deleting messages from store.
     * 
     * @return {@link Boolean}
     * @throws IOException
     */
    public int deleteVMAAccount();

    public ArrayList<LinkedVMADevices> syncLinkedDevices();

    public int deleteLinkedDevice(String deviceId);

    public int deleteAllLinkedDevices();

    public void registerGCMToken1();

    public int doHandsetProvisioning(String mdn, String deviceModel);
    
    public int registerHandset(String mdn, String deviceModel , ProvisionStatusListener listener);

    public int queryMessagingAssistantfeatures(long autoForwardSyncAnchor, long autoReplySyncAnchor);

}