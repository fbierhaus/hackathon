package com.vzw.vma.message;

import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;

import com.sun.mail.iap.ProtocolException;
import com.vzw.vma.common.message.VMAAttachment;
import com.vzw.vma.common.message.VMAMessage;

public interface VMAStore {

    public static final int ANY = -1;

    public static final int ALL_UID = ANY;
    
    public static final int IMAP_SERVER_API_VERSION = 0;
    public static final String IMAP_HANDSET_ACTION_TAG="C";
    public static final String IMAP_TABLET_ACTION_TAG="D";
    
    public static final  int IDLE_STATUS_UNKNOWN=0;
    public static final  int IDLE_STATUS_XUPDATE=1;
    public static final  int IDLE_STATUS_EXCEPTION=2;
    public static final  int IDLE_STATUS_LOGIN_FAILED=3;
    public static final  int IDLE_STATUS_ABORT=4;
    public static final  int IDLE_NO_VALID_SESSION=5;
    
    public static final  int MSA_MISSING_LOGIN_OR_PASSWORD=400;
    public static final  int MSA_SESSION_WRONG_STATE=402;
    public static final  int MSA_NOT_A_VMA_SUBSCRIBER=450;
    public static final  int MSA_HAS_SMS_BLOCKING=451;
    public static final  int MSA_ACCOUNT_SUSPENDED=452;
    public static final int MSA_FAILED_ANTISPAM_CHECK = 454;
    public static final int MSA_ISTD_NOT_SUPPORTED = 460;

    //NO [455] All destination MDNs blocked by Usage Control
    public static final int MSA_ALLDESTINATION_MDNS_BLOCKED_BY_USAGE_CONTROL = 455;
    //NO [456] UC limit reached
    public static final int MSA_UC_LIMIT_REACHED = 456;
    //NO [453] Subscriber has MMS blocking
    public static final int MSA_SUBSCRIBER_HAS_MMS_BLOCKING = 453;
    //NO [457] UC system error
    public static final int MSA_UC_SYSTEM_ERROR = 457;
    //NO [458] Subscriber has insufficient funds
    public static final int MSA_SUBSCRIBER_HAS_INSUFFICIENT_FUNDS = 458;
    // NO SELECT [408] Mailbox does not exist
    public static final int MSA_MAILBOX_DOES_NOT_EXIST = 408;
    // * NO [499] Other permanent failure, IDLE command not active

    public static final int MSA_OTHER_PERMANENT_FAILURE = 499;
    //NO IDLE [403] Another command already in progress
    public static final int MSA_ANOTHER_COMMAND_ALREADY_IN_PROGRESS = 403;
    
    public static final String LOGIN_FAILED = "LOGIN failed.";

    @Deprecated
    public List<VMAChangedSinceResponse> getChangedSince(long modSeq) throws ProtocolException, IOException;

    public VmaSelectResponse selectInbox() throws ProtocolException, IOException;

    public List<VMAXconvListResponse> getConversationLists(long maxUid, int numItems) throws ProtocolException, IOException;

    public List<VMAXconvFetchResponse> getConversation(String conversationId, long maxUid, long numUids)
            throws ProtocolException, IOException;

    public VMAMessageResponse getUid(long uid) throws ProtocolException,IOException, MessagingException;
    
    public List<VMAMessageResponse> getUids(long[] uid) throws ProtocolException,IOException, MessagingException;

    public List<VMAMessageResponse> getUidsNoThumbnail(long[] uidarr) throws ProtocolException, IOException, MessagingException;
    
    public List<VMAMessageResponse> getUidsHeaders(long[] uidarr) throws ProtocolException, IOException, MessagingException;
    	
    public List<VMAAttachment> getAttachments(long uid) throws ProtocolException, IOException, MessagingException;
    
    public String sendSMS(String data, String recipient) throws  ProtocolException, IOException, MessagingException;
    
    public String sendMMS(VMAMessage msg) throws  ProtocolException, IOException, MessagingException;
    
    public List<VMAMarkMessageResponse> markMessageDeleted(List<Long> uid) throws  ProtocolException, IOException;

    public List<VMAMarkMessageResponse> markMessageRead(List<Long> uid) throws  ProtocolException, IOException;
    
    public int startIdle() throws ProtocolException;
    
    public boolean abortIdle();
    
    public boolean setCredentials(String mdn, String pwd) throws  ProtocolException, IOException;

    /**
     * This Method is used to get the idle timeout of the MSA server
     * @return
     */
    public long getIdleTimeout();

    /**
     * This Method is used to verfiy the session is created or not. 
     * @return
     */
    public boolean isConnected();
    
    public boolean signout();
    
    public int getStatusCode(String result);
    
    public VmaSelectResponse reLogin() throws ProtocolException, IOException;
    
    public boolean isIdling();

    /**
     * This Method 
     */
    public VmaSelectResponse login() throws  ProtocolException, IOException;

    /**
     * This Method 
     * @param msg
     * @return
     */
    public String sendSMS(VMAMessage msg) throws ProtocolException, IOException, MessagingException;

    public void clearCache();
    
    
    public List<VMAChangedSinceResponse> getChangedSince(long maxPMCR , long maxSMCR) throws ProtocolException, IOException;
}
