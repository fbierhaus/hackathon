package com.vzw.vma.message.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Flags;
import javax.mail.MessagingException;

import android.content.Context;
import android.text.TextUtils;

import com.strumsoft.android.commons.logger.Logger;
import com.sun.mail.iap.Argument;
import com.sun.mail.iap.Literal;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.INTERNALDATE;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.sync.AppErrorCodes;
import com.vzw.vma.common.message.MSAMessage;
import com.vzw.vma.common.message.MessageSourceEnum;
import com.vzw.vma.common.message.MessageTypeEnum;
import com.vzw.vma.common.message.StringUtils;
import com.vzw.vma.common.message.VMAAttachment;
import com.vzw.vma.common.message.VMAMessage;
import com.vzw.vma.message.VMAChangedSinceResponse;
import com.vzw.vma.message.VMAFlags;
import com.vzw.vma.message.VMAMarkMessageResponse;
import com.vzw.vma.message.VMAMessageResponse;
import com.vzw.vma.message.VMAStore;
import com.vzw.vma.message.VMAXconvFetchResponse;
import com.vzw.vma.message.VMAXconvListResponse;
import com.vzw.vma.message.VmaSelectResponse;

public class VMAStoreJavaMailImpl implements VMAStore {

    // public static final String VMA_SERVER = "vmaqa.pdi.vzw.com";
    // private static final int VMA_PORT = 8143;,
    // private static final boolean USE_SSL = false;
    // private static final boolean DEBUG_IMAP;

    private String host;
    private int port;
    private boolean useSSL;
    private boolean debugImap;
    // private PrintStream OUT_PRINT_STREAM = System.out;
    private final PrintStream OUT_PRINT_STREAM;;
    public final static String INBOX = "INBOX";

    private VZWImapProtocol imap;

    protected Object connectionLock = new Object();

    private static final boolean ERROR_SIMULATOR = false;
    private static final int ERROR_SIMULATOR_RANDOM_FAIL_INTERVAL = 5;
    private static final int ERROR_SIMULATOR_CONNECT_FAIL_INTERVAL = 4;
    protected static int error_simulator_connectNumber = 0;
    protected static int error_simulator_request_number = 0;

    private static final String imapStore = "MSA";
    protected static final String KEY_CONNECT_TIMEOUT = "mail." + imapStore + ".connectiontimeout";
    protected static final String KEY_SOCKET_TIMEOUT = "mail." + imapStore + ".timeout";
    protected static final String KEY_APPEND_BUF_SZ = "mail." + imapStore + ".appendbuffersize";

    protected IMAPLogWrapperOutputStream logWriter;
    protected static final int CONNECT_TIMEOUT = 30 * 1000;
    protected static final int SOCKET_TIMEOUT = 110 * 1000;
    protected static final int APPEND_BUF_SZ = 100 * 1000;
    
   

    protected long idleTimeout = 0;
    Properties prop = new Properties();
    
    protected String mdn;
    protected String loginMdn;
    protected String pwd;

    protected Context context;
    protected long lastCommandTime = 0;

    protected static Pattern p = Pattern.compile(".*\\[(\\d\\d\\d)\\].*");
    protected static VMAStoreJavaMailImpl instance;
    
    protected boolean isSendOnly = false;
    
//    private static HashSet<String> SEND_VMA_MSG_ID_CACHE= new HashSet<String>();
    private AppSettings settings;
    private String actionTagPrefix;
    /**
     * @param context
     * @param settings
     *            Constructor
     */
    public VMAStoreJavaMailImpl(Context context, ApplicationSettings settings ,String actionTagPrefix ) {
        this.context = context;
        this.settings=settings;
        this.host = settings.getImapHost();
        this.port = settings.getImapPort();
        this.useSSL = settings.isSSLEnabled();
        this.mdn = settings.getMDN();
        this.loginMdn = settings.getMDN();
        this.pwd = settings.getDecryptedLoginToken();
        this.actionTagPrefix=actionTagPrefix; 
        if (Logger.IS_DEBUG_ENABLED && settings.isIMAPLogEnabled()) {
            logWriter = new IMAPLogWrapperOutputStream();
            OUT_PRINT_STREAM = new PrintStream(logWriter);
            this.debugImap = Logger.IS_DEBUG_ENABLED;
        } else {
            this.debugImap = false;
            OUT_PRINT_STREAM = null;
        }
    }

    public VMAStoreJavaMailImpl(Context context, String host, int port, boolean useSSL, String mdn,String loginMDN,
            String loginToken, boolean debugImap,String actionTagPrefix) {
        this.settings=ApplicationSettings.getInstance(context);
        this.loginMdn=loginMDN;
        this.context = context;
        this.host = host;
        this.port = port;
        this.useSSL = useSSL;
        this.mdn = mdn;
        this.pwd = loginToken;
        this.debugImap = debugImap;
        this.actionTagPrefix=actionTagPrefix; 
        if (debugImap) {
            OUT_PRINT_STREAM = new PrintStream(new IMAPLogWrapperOutputStream());
        } else {
            OUT_PRINT_STREAM = null;
        }
        prop.put(KEY_CONNECT_TIMEOUT, String.valueOf(CONNECT_TIMEOUT));
        prop.put(KEY_SOCKET_TIMEOUT, String.valueOf(SOCKET_TIMEOUT));
        
        if(loginMDN.endsWith("_SEND-ONLY")) {
        	isSendOnly = true;
        } 
        
        if(Logger.IS_DEBUG_ENABLED) {
        	Logger.debug("Initializing connection sendonly=" + isSendOnly + " loginMDN=" + loginMDN);
        }
        
        // TODO play with APPEND_BUF_SZ 
        // prop.put(KEY_SOCKET_TIMEOUT, SOCKET_TIMEOUT);
    }


    public class IMAPLogWrapperOutputStream extends OutputStream {

        /** The internal memory for the written bytes. */
        private String mem;

        public boolean logOperation = true;

        /**
         * 
         * Constructor
         */
        public IMAPLogWrapperOutputStream() {
            mem = "";
        }

        /*
         * Overriding method (non-Javadoc)
         * 
         * @see java.io.OutputStream#write(int)
         */
        @Override
        public void write(int oneByte) throws IOException {
            if (logOperation) {
                byte[] bytes = new byte[1];
                bytes[0] = (byte) (oneByte & 0xff);
                mem = mem + new String(bytes);
                // Preventing OOM for attachment downloads
                if (mem.endsWith("\n") || mem.length() >= 2048) {
                    mem = mem.substring(0, mem.length() - 1);
                    flush();
                }
            }
        }

        /**
         * Flushes the output stream.
         */
        public void flush() {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info(mem);
            }
            mem = "";
        }
    }



    // private VMAStoreJavaMailImpl(Context context, String host, int port, boolean useSSL, String mdn,
    // String pwd, boolean debugImap) {
    // this.context = context;
    // this.host = host;
    // this.port = port;
    // this.useSSL = useSSL;
    // this.debugImap = debugImap;
    // if (mdn != null) {
    // this.mdn = mdn;
    // }
    // if (pwd != null) {
    // this.pwd = pwd;
    // }
    // }

    /**
     * @param context
     * @param mdn
     * @param pwd
     *            Constructor
     */
    // public VMAStoreJavaMailImpl(Context context, String mdn, String pwd) {
    // this.context = context;
    // this.host = "vmaqa.pdi.vzw.com";
    // this.port = 8433;
    // this.useSSL = false;
    // this.debugImap = true;
    // if (mdn != null) {
    // this.mdn = mdn;
    // }
    // if (pwd != null) {
    // this.pwd = pwd;
    // }
    // }

    public VmaSelectResponse reLogin() throws ProtocolException, IOException {
    	
    	VmaSelectResponse selResp = null;

    	if(Logger.IS_DEBUG_ENABLED) {
    		Logger.debug("Doing reLogin getting connection lock");
    	}
    	
        synchronized (connectionLock) {
        	if(Logger.IS_DEBUG_ENABLED) {
        		Logger.debug("Doing reLogin got connection lock");
        	}
            if (imap != null) {
                /*
                 * LOGOUT
                 */
                try {
                    imap.logout();
                } catch (Exception e) {

                }
                /*
                 * DISCONNECT
                 */
                try {
                    imap.disconnect();
                } catch (Exception e) {

                }
                /*
                 * CLOSE
                 */
                try {
                    imap.close();
                } catch (Exception e) {

                }
                imap = null;
            }
            // Now login again
            selResp = login();
        }
        
        lastCommandTime = System.currentTimeMillis();
        
        return selResp;
        
    }

    public int getStatusCode(String result) {
        int code = 200;

        try {
            // Pattern p = Pattern.compile(".*\\[(\\d\\d\\d)\\].*");
            Matcher m = p.matcher(result);
            if (m.find()) {
                code = Integer.parseInt(m.group(1));
            }
        } catch (Exception e) {

        }

        return code;
    }

    protected Response[] issueCommand(String cmd, boolean retry) throws IOException, ProtocolException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "issueCommand: cmd=" + cmd + " RETRY=" + retry);
        }
        if (imap == null || !imap.isAuthenticated()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.warn("Authenticated=false. Relogging");
            }
            reLogin();
        } else {
            if (imap.isIdling()) {
                imap.idleAbort();
            }
        }
        
        Response[] resp = null;
        synchronized (connectionLock) {
            long start = System.currentTimeMillis();
            resp = imap.command(cmd, null);
            if (Logger.IS_DEBUG_ENABLED) {
                long dur = System.currentTimeMillis() - start;
                Logger.debug("IMAP call took : " + dur);
            }
        }
        if (resp != null && resp.length > 0) {
            Response r = resp[resp.length - 1];
            String result = new String(r.getByte());
            // * BYE JavaMail Exception: java.net.SocketException: Operation timed out
            // * BYE Imap .... connection timed out
            // * BAD [400] Command not understood, invalid message sequence set
            // NO UID FETCH [402] Session in wrong state
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("issueCommand: Res=" + r.getRest());
            }
            if (r.isBYE()) {
                if (retry) {
                    reLogin();
                    resp = issueCommand(cmd, false);
                } else {
                    throw new ProtocolException("Unable to connect: " + result);
                }
            } else if (r.isNO()) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error("NO response from server." + r.getRest());
                }
                throw new ProtocolException(r);
            } else if (r.isBAD()) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error("BAD response from server." + r.getRest());
                }
                throw new ProtocolException(r);
            } else {
                lastCommandTime = System.currentTimeMillis();
            }
            // * BYE JavaMail Exception: java.net.SocketException: Operation timed out
            // * BYE Imap .... connection timed out
            // * BAD [400] Command not understood, invalid message sequence set
            // if (result.startsWith("* BYE ")) {
            // if (retry) {
            // reLogin();
            // resp = issueCommand(cmd, false);
            // } else {
            // throw new ProtocolException("Unable to connect: " + result);
            // }
            // } else if (result.startsWith("* BAD")) {
            // throw new ProtocolException("Unable to connect: " + result);
            // } else {
            // lastCommandTime = System.currentTimeMillis();
            // }
        } else {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Unable to get response : either null or zero length ");
            }
            throw new ProtocolException("Unable to get response : either null or zero length response="
                    + resp + " | cmd=" + cmd);
        }
        return resp;
    }

    protected Response[] issueCommand(String cmd) throws IOException, ProtocolException {
        if (ERROR_SIMULATOR
                && (++error_simulator_request_number % ERROR_SIMULATOR_RANDOM_FAIL_INTERVAL == 0 || error_simulator_request_number
                        % (ERROR_SIMULATOR_RANDOM_FAIL_INTERVAL + 1) == 0)) {
            throw new IOException("SimulateError: Randomly failing command to simulate error ...");
        }
        return issueCommand(cmd, true);
    }

    @Override
    public VmaSelectResponse selectInbox() {
        VmaSelectResponse select = null;

        try {
            Response[] resp = issueCommand("SELECT INBOX");
            /*
             * A2 SELECT INBOX FLAGS (\Deleted \Seen $Sent $Thumbnail) OK [PERMANENTFLAGS \Deleted \Seen $Sent
             * $Thumbnail] OK [UIDVALIDITY 1346867501] UIDs valid OK [IDLETIMEOUT 300] OK [LASTUID 0] OK
             * [HIGHESTMCR (0 0)] OK [X-UNREAD 0]
             */
            long uidval = 0, lastuid = 0, modseq = 0, modseq2 = 0, unread = 0, idle = 0, ar=0, af=0;
            if (resp != null) {
                boolean found = false;
                for (Response res : resp) {
                    if (!(res instanceof IMAPResponse))
                        continue;
                    IMAPResponse ir = (IMAPResponse) res;

                    if (ir.isUnTagged() && ir.isOK()) {

                        ir.skipSpaces();

                        if (ir.readByte() != '[') { // huh ???
                            ir.reset();
                            continue;
                        }

                        /*
                         * FLAGS (\Deleted \Seen $Sent $Thumbnail) OK [PERMANENTFLAGS \Deleted \Seen $Sent
                         * $Thumbnail] OK [UIDVALIDITY 1355333366] UIDs valid OK [IDLETIMEOUT 120] OK [LASTUID
                         * 133018510] OK [HIGHESTMCR (102 0)] OK [X-UNREAD 1]
                         * OK [AutoForward 0]
                         * OK [AutoReply 0]
                         */

                        boolean handled = true;
                        String s = ir.readAtom();
                        if (s.equalsIgnoreCase("X-UNREAD")) {
                            unread = ir.readNumber();
                            found = true;
                        } else if (s.equalsIgnoreCase("AutoForward")) {
                            af = ir.readLong();
                            found = true;
                        } else if (s.equalsIgnoreCase("AutoReply")) {
                            ar = ir.readLong();
                            found = true;
                        } else if (s.equalsIgnoreCase("UIDVALIDITY")) {
                            uidval = ir.readNumber();
                            found = true;
                        } else if (s.equalsIgnoreCase("HIGHESTMCR")) { // HIGHESTMCR (357 0)
                            String[] mcrs = ir.readSimpleList();
                            if (mcrs != null) {
                                modseq = Long.valueOf(mcrs[0]);
                                modseq2 = Long.valueOf(mcrs[1]);
                            }
                            found = true;
                        } else if (s.equalsIgnoreCase("LASTUID")) {
                            lastuid = ir.readNumber();
                            found = true;
                        } else if (s.equalsIgnoreCase("IDLETIMEOUT")) {
                            idle = ir.readNumber();
                            if (idle > 0) {
                                idle = idle * 1000;
                                if (idleTimeout != idle) {
                                    if (Logger.IS_DEBUG_ENABLED) {
                                        Logger.debug("Found idletimeout : " + idleTimeout);
                                    }
                                    idleTimeout = idle;
                                }
                            }
                            found = true;
                        } else if (s.equalsIgnoreCase("PERMANENTFLAGS")) {
                            // idle = ir.readNumber();
                            // found = true;
                        } else
                            handled = false; // possibly an ALERT

                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("Error:1 =" + ir.getTag() + "-" + ir.getType() + "=" + ir.getRest()
                                    + "=" + ir.getNumber() + "=" + ir.isUnTagged() + "=ta" + ir.isTagged());
                        }
                    }
                }

                if (found) {
                    return new VMASelectResponseImpl(uidval, lastuid, modseq, modseq2,  unread, idle, ar, af);
                }
            }
        } catch (Exception e) {
            if (Logger.IS_ERROR_ENABLED) {
                Logger.error(false, e);
            }
        }
        return select;
    }

    @Override
    public List<VMAXconvListResponse> getConversationLists(long maxUid, int numItems) throws IOException, ProtocolException {
        String uid = (maxUid == VMAStore.ANY ? "*" : (maxUid + ""));
        String cmd = "XCONV LIST UID " + uid + " NUMGROUPS " + numItems;
        // System.out.println("Cmd=" + cmd);
        ArrayList<VMAXconvListResponse> list = new ArrayList<VMAXconvListResponse>();

        try {

            Response[] reps = issueCommand(cmd);

            for (Response rep : reps) {
                String result = new String(rep.getByte());
                if (result.startsWith("*")) {
                    StringTokenizer st = new StringTokenizer(result, " ");
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if ("(PARTICIPANTID".equals(token)) {
                            try {

                                String conversationThreadId = st.nextToken();
                                st.nextToken();
                                String lastUid = st.nextToken();
                                int highestUid = Integer.valueOf(lastUid);
                                st.nextToken();
                                String unreadStr = st.nextToken();
                                int unreadCount = Integer.valueOf(unreadStr.substring(0,
                                        unreadStr.indexOf(")")));
                                VMAXconvListResponse rx = new VMAXconvListResponseImpl(conversationThreadId,
                                        highestUid, unreadCount);
                                if (Logger.IS_DEBUG_ENABLED) {
                                    Logger.debug(VMAStoreJavaMailImpl.class, rx.toString());
                                }
                                list.add(rx);
                            }finally{
//                            } catch (Exception ex) {
//                                Logger.error(false, ex);
                            }
                            break;
                        }
                    }
                }
            }
        }finally{
//        } catch (Exception e) {
//            if (Logger.IS_ERROR_ENABLED) {
//                Logger.error(false, e);
//            }
        }
        return list;
    }

    private VZWImapProtocol getImapSession(String mdn, String pwd) throws IOException, ProtocolException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "issueCommand: cmd=LOGIN Username="+loginMdn+", Pin="+ pwd);
        }
        
        VZWImapProtocol imap1 = new VZWImapProtocol(imapStore, host, port, debugImap, OUT_PRINT_STREAM, prop,
                useSSL ,actionTagPrefix);
        // imap.login(mdn, StringUtils.getMsaPassword(mdn));
        imap1.login(mdn, pwd);
        imap1.capability();
        imap = imap1;
        return imap1;
    }

    @Override
    public boolean setCredentials(String mdna, String pwda) throws IOException, ProtocolException {
        boolean loggedIn = false;
        this.loginMdn = mdna;
        this.pwd = pwda;
        // try {
        synchronized (connectionLock) {
            imap = getImapSession(loginMdn, pwd);
        }
        loggedIn = true;
        // } catch (IOException e) {
        // Logger.error(false, e);
        // }
        return loggedIn;
    }

    // public static VMAStore getInstance(Context context) {
    // return getInstance(context, null, null);
    // }

    // public static VMAStore getInstance(Context context, String mdn, String pwd) {
    // synchronized (VMAStoreJavaMailImpl.class) {
    // if (instance == null) {
    // instance = new VMAStoreJavaMailImpl(context, mdn, pwd);
    // }
    // }
    // return instance;
    // }

    // public static VMAStore getInstance(Context context, String host, int port, boolean useSSL, String mdn,
    // String pwd, boolean debugImap) {
    // synchronized (VMAStoreJavaMailImpl.class) {
    // if (instance == null) {
    // instance = new VMAStoreJavaMailImpl(context, host, port, useSSL, mdn, pwd, debugImap);
    // }
    // }
    // return instance;
    // }

    @Override
    public List<VMAXconvFetchResponse> getConversation(String conversationId, long maxUid, long numUids) throws IOException, ProtocolException {
        String uid = (maxUid == VMAStore.ANY ? "*" : (maxUid + ""));
        String cmd = "XCONV FETCH PARTICIPANTID " + conversationId + " UID " + uid + " NUMMSGS " + numUids;
        ArrayList<VMAXconvFetchResponse> list = new ArrayList<VMAXconvFetchResponse>();

        try {

            Response[] reps = issueCommand(cmd);

            for (Response rep : reps) {
                String result = new String(rep.getByte());
                if (result.startsWith("*")) {
                    StringTokenizer st = new StringTokenizer(result, " ");
                    String conversationThreadId = null;
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if ("(PARTICIPANTID".equals(token)) {
                            conversationThreadId = st.nextToken();
                        } else if ("UID".equals(token)) {
                            try {
                                String uidStr = st.nextToken();
                                String thisUid = uidStr.substring(0, uidStr.indexOf(")"));
                                long highestUid = Long.valueOf(thisUid);
                                VMAXconvFetchResponse rx = new VMAXconvFetchResponseImpl(
                                        conversationThreadId, highestUid, 0);
                                // if (Logger.IS_DEBUG_ENABLED) {
                                // Logger.debug(VMAStoreJavaMailImpl.class, rx.toString());
                                // }
                                list.add(rx);
                            }finally{
//                            } catch (Exception ex) {
//                                if (Logger.IS_ERROR_ENABLED) {
//                                    Logger.error(false, ex);
//                                }
                            }
                            break;
                        }
                    }
                }
            }
        }finally{
//        } catch (Exception e) {
//            if (Logger.IS_DEBUG_ENABLED) {
//                Logger.error(VMAStoreJavaMailImpl.class, e);
//            }
        }

        return list;
    }

    @Override
    public List<VMAChangedSinceResponse> getChangedSince(long modSeq) throws ProtocolException {
    	return getChangedSince(modSeq ,0 , VMAStore.ANY);
    }

    public List<VMAChangedSinceResponse> getChangedSince(long maxPMCR, long maxSMCR) throws ProtocolException {
    	return getChangedSince(maxPMCR ,maxSMCR, VMAStore.ANY);
    }
    
    
    
    private List<VMAChangedSinceResponse> getChangedSince(long ourMaxPMcr,long ourMaxSMcr, long maxUid) throws ProtocolException {
        String uid = (maxUid == VMAStore.ANY ? "*" : (maxUid + ""));
        String cmd = "UID FETCH 1:" + uid + " Message-ID (CHANGEDSINCEMCR (" + ourMaxPMcr + " "+ourMaxSMcr+"))";
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(VMAStoreJavaMailImpl.class, cmd);
        }
        ArrayList<VMAChangedSinceResponse> list = new ArrayList<VMAChangedSinceResponse>();
        try {
            Response[] reps = issueCommand(cmd);

            /*
             * A5 UID FETCH 1:* Message-ID (CHANGEDSINCE 90) 68009502 FETCH (UID 68009502 MODSEQ (92)
             * MESSAGE-ID (20120715185252-1818891918)) 67316214 FETCH (UID 67316214 MODSEQ (91) MESSAGE-ID
             * (20120713184412-1928886958)) A5 OK UID FETCH [200] Command successful
             * 
             * * 141948855 FETCH (UID 141948855 XMCR (4885 0) MESSAGE-ID (02FBE61CE4FA00002A300003))
             */
            for (Response rep : reps) {
                String result = new String(rep.getByte());
                if (result.startsWith("*")) {
                    StringTokenizer st = new StringTokenizer(result, " ");
                    long uidx = 0;
//                    long mods = 0;
                    long pMcr = 0;
                    long sMcr = 0;
                    String vmaId = null;
                    while (st.hasMoreTokens()) {
                        String token = st.nextToken();
                        if ("(UID".equals(token)) {
                            String uids = st.nextToken();
                            uidx = Long.valueOf(uids);
                        } else if ("XMCR".equals(token)) {
//                         * 174953943 FETCH (UID 174953943 XMCR (6 0) MESSAGE-ID (vmaqa_27022001))
// MODSeq parse logic 
//                            String modx = st.nextToken();
//                        	mods = Long.valueOf(modx.substring(1));

                        	// The below changes are MSA 2.0 (failover)   
                        	String xMCRPrefix=st.nextToken();
                        	pMcr= Long.valueOf(xMCRPrefix.substring(1));
                           
                            String xMCRSuffix=st.nextToken();
                            sMcr= Long.valueOf(xMCRSuffix.substring(0,xMCRSuffix.length()-1));
                            if(Logger.IS_DEBUG_ENABLED){
                            	Logger.debug("MCR: xMCR Prefix="+xMCRPrefix+" xMCR Suffix="+xMCRSuffix+" pMCR="+pMcr+" sMCR="+sMcr);
                            }
                        } else if ("MESSAGE-ID".equals(token)) {
                            try {
                                String uidStr = st.nextToken();
                                vmaId = uidStr.substring(1, uidStr.indexOf(")"));
//                                VMAChangedSinceResponse rx = new VMAChangedSinceResponseImpl(uidx, mods,vmaId);
                                VMAChangedSinceResponse rx = new VMAChangedSinceResponseImpl(uidx, pMcr,sMcr,vmaId);
                                if (Logger.IS_DEBUG_ENABLED) {
                                    Logger.debug(VMAStoreJavaMailImpl.class, rx.toString());
                                }
                                list.add(rx);
                            } catch (Exception ex) {
                                Logger.error(false, ex);
                            }
                            break;
                        }
                    }
                }
            }

        } catch (IOException e) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.error(VMAStoreJavaMailImpl.class, e);
            }
        }
        return list;
    }

    protected void enableImapLog(boolean enable) {
        if (logWriter != null) {
            logWriter.logOperation = enable;
        }
    }

    @Override
    public List<VMAAttachment> getAttachments(long uid) throws ProtocolException, IOException,
            MessagingException {

        try {
            enableImapLog(false); // do not log ATTACHMENT fetches
            List<VMAAttachment> empty = new ArrayList<VMAAttachment>();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "getAttachments:" + uid);
            }
            long[] uidarr = new long[1];
            uidarr[0] = uid;

            List<VMAMessageResponse> resplist = getUidInternal(uidarr, true, false);
            VMAMessageResponse resp;
            if (resplist.size() == 1) {
                resp = resplist.get(0);
            } else if (resplist.size() > 1) {
                Logger.error(false,"VMAStoreJavamailImpl: ****** Why did we get more than one response object from VMA ?****");
                return empty;
            } else {
                Logger.error(false,"VMAStoreJavamailImpl: ****** Why did we get zero response object from VMA ?****");
                return empty;
            }

            return resp.getVmaMessage().getAttachments();
        } finally {
            enableImapLog(true);
        }
    }

    // private void debugAttachments(long uid) throws ProtocolException, IOException, MessagingException {
    // try {
    // Logger.debug("\n\n\n\n Fetching attacvhment");
    // List<VMAAttachment> attachments = getAttachments(uid);
    // Logger.debug("\n\n\n\n Fetched attacvhment");
    // if (attachments != null) {
    // for (VMAAttachment attachment : attachments) {
    // byte[] data = attachment.getData();
    // File f = new File("/mnt/sdcard/Download/" + System.currentTimeMillis() + ".jpg");
    // FileOutputStream fos = new FileOutputStream(f);
    // fos.write(data);
    // fos.close();
    // }
    // } else {
    // Logger.debug("\n\n\n Got no attachments");
    // }
    // } catch (IOException e) {
    // if (Logger.IS_ERROR_ENABLED) {
    // Logger.error(false, "\n\n\n", e);
    // }
    // }
    // }

    @Override
    public VMAMessageResponse getUid(long uid) throws ProtocolException, IOException, MessagingException {
        long[] uidarr = new long[1];
        uidarr[0] = uid;
        List<VMAMessageResponse> resplist = getUidInternal(uidarr, false, true);
        if (resplist.size() == 1) {
            return resplist.get(0);
        } else if (resplist.size() > 1) {
            Logger.error("****** why did we get multiple returned values *****" + resplist);
        }
        if(Logger.IS_DEBUG_ENABLED){
            Logger.debug("getUid() : empty response");
        }
        return null;
    }

    public List<VMAMessageResponse> getUidsNoThumbnail(long[] uidarr) throws ProtocolException, IOException,
            MessagingException {
        return getUidInternal(uidarr, false, false);
    }
    
    
    public List<VMAMessageResponse> getUidsHeaders(long[] uidarr) throws ProtocolException, IOException,
    MessagingException {
        return getUidInternal(uidarr, false, false ,true);
    }

    public List<VMAMessageResponse> getUids(long[] uidarr) throws ProtocolException, IOException,
            MessagingException {
        return getUidInternal(uidarr, false, true);
    }
    
    protected List<VMAMessageResponse> getUidInternal(long[] uids, boolean fetchAttachment,
            boolean wantThumbnails) throws ProtocolException, IOException, MessagingException {
        return getUidInternal(uids, fetchAttachment, wantThumbnails ,false);
    }

    protected List<VMAMessageResponse> getUidInternal(long[] uids, boolean fetchAttachment,
            boolean wantThumbnails ,boolean onlyHeaders) throws ProtocolException, IOException, MessagingException {
        if(Logger.IS_DEBUG_ENABLED){
            simulateError();
        }
        ArrayList<VMAMessageResponse> resplist = new ArrayList<VMAMessageResponse>();
        String uidstr = "";
        long uid = uids[0];
        ;
        if (uids.length > 1) {
            boolean first = true;
            for (long u : uids) {
                if (first) {
                    first = false;
                    uidstr += u;
                } else {
                    uidstr += "," + u;
                }
            }
        } else {
            uidstr = "" + uid;
        }
        String cmd;
        if (fetchAttachment) {
            cmd = "UID FETCH " + uidstr + " (FLAGS BODY.PEEK[ATTACHMENTS])";
        } else if(onlyHeaders){
            cmd = "UID FETCH " + uidstr + " (FLAGS XRECIPSTATUS BODY.PEEK[HEADER])";
        } else {
            if (wantThumbnails) {
                cmd = "UID FETCH " + uidstr + " (FLAGS XRECIPSTATUS BODY.PEEK[HEADER+TEXT+THUMBNAILS])";
            } else {
                cmd = "UID FETCH " + uidstr + " (FLAGS XRECIPSTATUS BODY.PEEK[HEADER+TEXT])";
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(VMAStoreJavaMailImpl.class, cmd);
        }

        try {
            Response[] reps = issueCommand(cmd);

            for (Response rep : reps) {
                IMAPResponse ir = (IMAPResponse) rep;
                if (ir.isUnTagged()) {
                    MSAMessage msg = new MSAMessage();

                    byte[] reply = ir.getByte();
                    ByteArrayInputStream is = new ByteArrayInputStream(reply);
                    msg.parse(is, fetchAttachment, context, AppSettings.SEND_VMA_MSG_ID_CACHE);
                    ArrayList<VMAFlags> flags = new ArrayList<VMAFlags>();
                    if (msg.isDeleted()) {
                        flags.add(VMAFlags.DELETED);
                    }
                    if (msg.isSeen()) {
                        flags.add(VMAFlags.SEEN);
                    }
                    if (msg.isSent()) {
                        flags.add(VMAFlags.SENT);
                    }
                    VMAMessageResponse msgResp = null;
//                    msgResp = new VMAMessageResponseImpl(msg.toVMAMessage(fetchAttachment), flags, uid,
//                            msg.getModSeq(), msg.getDeliveryReports());
                    //MSA Fail over changes 
                    msgResp = new VMAMessageResponseImpl(msg.toVMAMessage(fetchAttachment), flags, uid,
                    		msg.getPrimaryMCR(),msg.getSecondaryMCR(), msg.getDeliveryReports());
                    resplist.add(msgResp);
                } else {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Tagged Response:" + ir.readString());
                    }

                }
            }

        } finally {

        }
        if(Logger.IS_DEBUG_ENABLED){
            simulateError();
        }
        return resplist;
    }

    /*
    public boolean markMessageRead(long uid) throws IOException, ProtocolException {
    	ArrayList<Long> list = new ArrayList<Long>();
    	list.add(uid);
    	return markMessageRead(list);
    }

    public boolean markMessageDeleted(long uid) throws IOException, ProtocolException {
    	ArrayList<Long> list = new ArrayList<Long>();
    	list.add(uid);
    	return markMessageDeleted(list);
    }
    */

    @Override
    public List<VMAMarkMessageResponse> markMessageRead(List<Long> uidl) throws IOException {
    	// XXX: Hack return for now
    	return  markMessageOnServer(uidl,MSAMessage.FLAG_SEEN);
    }



    @Override
    public List<VMAMarkMessageResponse> markMessageDeleted(List<Long> uidl) throws IOException, ProtocolException {
    	// XXX: Hack return for now 
    	return markMessageOnServer(uidl,MSAMessage.FLAG_DELETED);
    }
    
    /*
     * 
     * 136154094 FETCH (UID 136154094 XMCR (46 0) FLAGS (\Deleted \Seen $Sent))
     * 136154078 FETCH (UID 136154078 XMCR (47 0) FLAGS (\Deleted \Seen $Sent))
     * 136154062 FETCH (UID 136154062 XMCR (48 0) FLAGS (\Deleted \Seen $Sent))
     * 136147478 FETCH (UID 136147478 XMCR (49 0) FLAGS (\Deleted \Seen $Sent))
     * 136146774 FETCH (UID 136146774 XMCR (50 0) FLAGS (\Deleted \Seen $Sent))
     * 136146766 FETCH (UID 136146766 XMCR (51 0) FLAGS (\Deleted \Seen $Sent))
     * 136144278 FETCH (UID 136144278 XMCR (52 0) FLAGS (\Deleted \Seen $Sent))
     * 136144262 FETCH (UID 136144262 XMCR (53 0) FLAGS (\Deleted \Seen $Sent))
     * 
     * 
     */
    protected VMAMarkMessageResponse getMessageResponse(String line) {
    	
		long uid = 0;
		long modSeq = 0;
		boolean isSeen = false;
		boolean isSent = false;
		boolean isDeleted = false;
    	int uidSeqIdx = line.indexOf("UID");
		if (uidSeqIdx != -1) {
			int endOfUidSeq = line.indexOf(")");
			String uidSeqString = line.substring(uidSeqIdx, endOfUidSeq);
			uidSeqString = uidSeqString.replace("UID ", "");
			String[] uids =  uidSeqString.split(" ");
			uid = Long.valueOf(uids[0]);

		}
		
		int modSeqIdx = line.indexOf("XMCR");
		if (modSeqIdx != -1) {
			int endOfModSeq = line.indexOf(")");
			String modSeqString = line.substring(modSeqIdx, endOfModSeq);
			modSeqString = modSeqString.replace("XMCR (", "");
			modSeqString = modSeqString.replace(")", "");
			String[] xmcrs =  modSeqString.split(" ");
			modSeq = Long.valueOf(xmcrs[0]);
		}

		int flagsIdx = line.indexOf("FLAGS");
		String flagsLine = null;
		if (flagsIdx != -1) {
			flagsLine = line.substring(flagsIdx+5);
			int endOfFlagsIdx = flagsLine.indexOf(")");
			if (endOfFlagsIdx != -1) {
				flagsLine = flagsLine.substring(0, endOfFlagsIdx).replace("(", "");
			}
			if(Logger.IS_DEBUG_ENABLED) {
				Logger.debug("Found flags = " + flagsLine);
			}
		}

		if (flagsLine!=null) {
			StringTokenizer st = new StringTokenizer(flagsLine);
			while (st.hasMoreTokens()) {
				String flag = st.nextToken();
				if (MSAMessage.FLAG_SEEN.equalsIgnoreCase(flag)) {
					isSeen = (true);
				} else if (MSAMessage.FLAG_DELETED.equalsIgnoreCase(flag)) {
					isDeleted = (true);
				} else if (MSAMessage.FLAG_SENT.equalsIgnoreCase(flag)) {
					isSent = (true);
				} else if (MSAMessage.FLAG_THUMBNAIL.equalsIgnoreCase(flag)) {
				}
			}
		}
		
		return new VMAMarkMessageResponseImpl(uid, modSeq, isSeen, isDeleted, isSent);
    }
    protected static final Long ZERO_LONG = new Long(0);
    

    public List<VMAMarkMessageResponse> markMessageOnServer(List<Long> uidi, String flag) throws IOException {
    	ArrayList<VMAMarkMessageResponse> resplist =  new ArrayList<VMAMarkMessageResponse>();
    	boolean success = false;
    	if(uidi.contains(ZERO_LONG)) {
    		ArrayList<Long> uidl = new ArrayList<Long>();
    		for(Long uid : uidi) {
    			if(uid > 0) {
    				uidl.add(uid);
    			}
    		}
    		uidi = uidl;
    	}
    	boolean isOK = false;
    	if(uidi.size() > 0) {
    		String uid = uidi.toString().replace("[", "").replace("]", "").replaceAll(" ", "");
    		try {
    			Response[] reps = issueCommand("UID STORE " + uid + " +FLAGS (" + flag + ")");
    			for (Response rep : reps) {
    				if (rep.isTagged()) {
    					success = rep.isOK();
    					isOK = true;
    					if(Logger.IS_DEBUG_ENABLED) {
    						Logger.debug("UID STORE Response = " + rep.toString());
    					}
    					break;
    				} else {
    					String line = rep.toString();
    					if(Logger.IS_DEBUG_ENABLED) {
    						Logger.debug("UID STORE ResponseLine = " + rep.toString());
    					}
    					if(line.startsWith("*")) {
    						VMAMarkMessageResponse mmr = getMessageResponse(line);
    						if(Logger.IS_DEBUG_ENABLED) {
    							Logger.debug(mmr);
    						}
    						resplist.add(mmr);
    					}
    				}
    			}

    		} catch (Exception e) {
    			if (Logger.IS_DEBUG_ENABLED) {
    				Logger.error(VMAStoreJavaMailImpl.class, e);
    			}
    			throw new IOException(e.getMessage());
    		}
    	}
    	
    	if(!isOK) {
    		resplist = null;
    	}
    	
        return resplist;
    }

    protected String appendMessage(VMAMessage message, boolean retry) throws ProtocolException, IOException,
            MessagingException {
        String msgId = null;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "appendMessage called for " + message.getMessageText() + " for type "
                    + message.getMessageType());
        }
        try {
            if (imap == null || !imap.isAuthenticated()) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.warn("Authenticated=false. Relogging");
                }
                reLogin();
            } else {
                if (imap.isIdling()) {
                    imap.idleAbort();
                }
            }
            Flags f = new Flags();
            if (message.getMdn().equals(message.getSourceAddr())) {
                f.add(MSAMessage.FLAG_SENT);
                f.add(MSAMessage.FLAG_SEEN);
            }
            f = f.getUserFlags().length > 0 ? f : null;
            MSAMessage mm = new MSAMessage(message);
            try {
                synchronized (connectionLock) {
                    msgId = imap.appendVMAuid(INBOX, f, null, mm);
                }
                message.setMessageId(msgId);
            } catch (ProtocolException e) {
                if (Logger.IS_ERROR_ENABLED) {
                    Logger.error("appendMessage.error=" + e.getMessage(), e);
                }

                if (e.getResponse().isNO()) {
                    // International destination numbers are not supported: <mdn>,<mdn>…
                    throw e;
                }
                if (retry) {
                    reLogin();
                    msgId = appendMessage(message, false);
                    message.setMessageId(msgId);
                }
            }
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("MsgID=" + msgId + " f=" + f);
            }
        } finally {
        }
        if(Logger.IS_DEBUG_ENABLED){
            simulateError();
        }
        return msgId;
    }

    private VMAMessage getSampleSMS(String mdn, String from, String to, String msg) {
        VMAMessage message = new VMAMessage();
        message.setMdn(mdn);
        message.setMessageTime(new Date());
        message.setSourceAddr(from);
        ArrayList<String> toList = new ArrayList<String>();
        toList.add(to);
        message.setToAddrs(toList);
        message.setMessageId(StringUtils.getRandomString(15));
        message.setMessageType(MessageTypeEnum.SMS);
        message.setMessageSource(MessageSourceEnum.IMAP);
        message.setMessageText(msg);
        return message;
    }
    
    

    /* Overriding method 
     * (non-Javadoc)
     * @see com.vzw.vma.message.VMAStore#sendSMS(com.vzw.vma.common.message.VMAMessage)
     */
    @Override
    public String sendSMS(VMAMessage msg) throws ProtocolException, IOException, MessagingException {
        if(Logger.IS_DEBUG_ENABLED){
            Logger.debug("store:sendSMS luid="+msg.getLuid() +",tempMsgId="+msg.getMessageId());
        }
        // account MDN 
        msg.setMdn(mdn);
        // Android Source 
        msg.setSourceAddr(mdn);

        return appendMessage(msg, true);
    }
    

    public String sendSMS(String data, String recipient) throws ProtocolException, IOException,
            MessagingException {
        VMAMessage mesg = getSampleSMS(mdn, mdn, recipient, data);
        return appendMessage(mesg, true);
    }

    @Override
    public String sendMMS(VMAMessage msg) throws ProtocolException, IOException, MessagingException {
        try {
            enableImapLog(false); // disable logs of binary attachment data
            msg.setMdn(mdn);
            msg.setSourceAddr(mdn);
           String messageId= appendMessage(msg, true);
           if(msg!=null){
               AppSettings.SEND_VMA_MSG_ID_CACHE.add(messageId);
           }
           return messageId;
        } finally {
            enableImapLog(true);
        }
    }

    @Override
    public int startIdle() throws ProtocolException {
        int ret = IDLE_STATUS_UNKNOWN;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(),"Starting idle");
        }
        synchronized (connectionLock) {
            if (imap != null) {
                imap.idleStart();
                ret = imap.status;
            } else {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(),"imap is null");
                }
                ret = IDLE_NO_VALID_SESSION;
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(),"Returning from start idle: " + ret);
        }
        return ret;
    }

    @Override
    public boolean abortIdle() {
        boolean ret = false;
        try {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Aborting idle imap=" + imap);
            }
            if (imap != null) {
                imap.idleAbort();
                ret = true;
            }
        } catch (Exception e) {
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Returning from abort idle: " + ret);
        }
        return ret;
    }

    // the DONE command to break out of IDLE
    private static final byte[] DONE = { 'D', 'O', 'N', 'E', '\r', '\n' };

    protected class VZWImapProtocol extends IMAPProtocol {

        protected int status;

        protected boolean idling;

        protected Object idleLock = new Object();

        protected boolean isIdling() {
            return idling;
        }
        
        

        protected boolean isUpdate(Response r) {
            return "* XUPDATE".equalsIgnoreCase(r.toString());
        }

        @Override
        public void idleStart() throws ProtocolException {
            idling = true;
            status = IDLE_STATUS_UNKNOWN;
            try {
                if (!isAuthenticated()) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.error("Idle failed. Session expiered. please relogin");
                    }
                    return;
                }
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.error("issueCommand: IDLE START");
                }
                super.idleStart();
                boolean done = false;
                Response r = null;
                while (!done) {
                    try {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(),"Invoking Idle Read call.");
                        }
                        r = readResponse();
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(),"Idle read released. response="+ ((r!=null)?r.toString():null));
                        }
                    } catch (IOException ioex) {
                        status = IDLE_STATUS_EXCEPTION;
                        // convert this into a BYE response
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.error(getClass(),"Idle got io exception ", ioex);
                        }
                        r = Response.byeResponse(ioex);
                    }
                    boolean wasUpdate = false;
                    if (r.isContinuation() || r.isBYE() || (wasUpdate = isUpdate(r)) || r.isNO() || r.isBAD()) {
                        done = true;
                        if (wasUpdate) {
                            status = IDLE_STATUS_XUPDATE;
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(getClass(),"* XUPDATE : Sending Idle Abort command.");
                            }
                            abortIdle();
                        }else{
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(getClass(),"Not * XUPDATE r=" +  ((r!=null)?r.toString():null));
                            }
                        }
                    } else {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(),"Idle got response that is not a update, continuation or bye == "
                                    + r.toString());
                        }
                        status = IDLE_STATUS_ABORT;
                    }
                    synchronized (idleLock) {
                        if (!idling) {
                            done = true;
                            if (Logger.IS_DEBUG_ENABLED) {
                                Logger.debug(getClass(),"Someone set idle to false, so should we exit");
                            }
                            if (wasUpdate) {
                                Logger.debug(getClass(),"Last message receieved was an update. But we should also get idle done response");
                                try {
                                    if (Logger.IS_DEBUG_ENABLED) {
                                        Logger.error(getClass(),false,"Someone set idle to false, so we should read update before we exit");
                                    }
                                    r = readResponse();

                                } catch (Exception e) {
                                    if (Logger.IS_DEBUG_ENABLED) {
                                        Logger.error(getClass(),"Idle got exception ", e);
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                idling = false;
            }
        }

        @Override
        public void idleAbort() throws ProtocolException {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "issueCommand: cmd=DONE (idleAbort)");
            }
            synchronized (idleLock) {
                try {
                    OutputStream os = getOutputStream();
                    os.write(DONE);
                    os.flush();
                } catch (Exception e) {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("Got an exception from the write to idle done." + e);
                    }
                } finally {
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "idleAbort: done");
                    }
                    idling = false;
                }
            }
        }

        public VZWImapProtocol(String name, String host, int port, boolean debug, PrintStream out,
                Properties props, boolean isSSL , String actionTagPrefix) throws IOException, ProtocolException {
            super(name, host, port, debug, out, props, isSSL,actionTagPrefix);
            if (ERROR_SIMULATOR
                    && (++VMAStoreJavaMailImpl.error_simulator_connectNumber
                            % ERROR_SIMULATOR_CONNECT_FAIL_INTERVAL == 0)) {
                try {
                    super.close();
                } catch (Exception e) {
                }
                throw new IOException("SimulateError: server error - deliberatly aborting login");
            }
        }

        /**
         * APPEND Command, return uid from APPENDUID response code.
         * 
         * @see "RFC2060, section 6.3.11"
         */
        public String appendVMAuid(String mbox, Flags f, Date d, com.sun.mail.iap.Literal data)
                throws ProtocolException {
            return appendVMAuid(mbox, f, d, data, true);
        }

        /**
         * Creates an IMAP flag_list from the given Flags object.
         */
        private String createFlagList(Flags flags) {
            StringBuffer sb = new StringBuffer();
            sb.append("("); // start of flag_list

            Flags.Flag[] sf = flags.getSystemFlags(); // get the system flags
            boolean first = true;
            for (int i = 0; i < sf.length; i++) {
                String s;
                Flags.Flag f = sf[i];
                if (f == Flags.Flag.ANSWERED)
                    s = "\\Answered";
                else if (f == Flags.Flag.DELETED)
                    s = "\\Deleted";
                else if (f == Flags.Flag.DRAFT)
                    s = "\\Draft";
                else if (f == Flags.Flag.FLAGGED)
                    s = "\\Flagged";
                else if (f == Flags.Flag.RECENT)
                    s = "\\Recent";
                else if (f == Flags.Flag.SEEN)
                    s = "\\Seen";
                else
                    continue; // skip it
                if (first)
                    first = false;
                else
                    sb.append(' ');
                sb.append(s);
            }

            String[] uf = flags.getUserFlags(); // get the user flag strings
            for (int i = 0; i < uf.length; i++) {
                if (first)
                    first = false;
                else
                    sb.append(' ');
                sb.append(uf[i]);
            }

            sb.append(")"); // terminate flag_list
            return sb.toString();
        }

        public String appendVMAuid(String mbox, Flags f, Date d, Literal data, boolean uid)
                throws ProtocolException {
            // encode the mbox as per RFC2060
            mbox = BASE64MailboxEncoder.encode(mbox);

            Argument args = new Argument();
            args.writeString(mbox);

            if (f != null) { // set Flags in appended message
                // can't set the \Recent flag in APPEND
                if (f.contains(Flags.Flag.RECENT)) {
                    f = new Flags(f); // copy, don't modify orig
                    f.remove(Flags.Flag.RECENT); // remove RECENT from copy
                }

                /*
                 * HACK ALERT: We want the flag_list to be written out without any checking/processing of the
                 * bytes in it. If I use writeString(), the flag_list will end up being quoted since it
                 * contains "illegal" characters. So I am depending on implementation knowledge that
                 * writeAtom() does not do any checking/processing - it just writes out the bytes. What we
                 * really need is a writeFoo() that just dumps out its argument.
                 */
                args.writeAtom(createFlagList(f));
            }
            if (d != null) // set INTERNALDATE in appended message
                args.writeString(INTERNALDATE.format(d));

            args.writeBytes(data);

            Response[] r = command("APPEND", args);

            // dispatch untagged responses
            notifyResponseHandlers(r);

            // Handle result of this command
            handleResult(r[r.length - 1]);

            if (uid)
                return getAppendVMAMsgid(r[r.length - 1]);
            else
                return null;
        }

        /**
         * If the response contains an Message-ID response code, extract it and return an String object with
         * the information.
         */
        private String getAppendVMAMsgid(Response r) {
            if (!r.isOK())
                return null;

            String temp = r.toString();
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("Response :" + temp);
            }
            temp = temp.substring(temp.lastIndexOf("Message-ID"));
            String msgId = temp.substring(temp.indexOf("D") + 1, temp.indexOf("]")).trim();
            if (msgId != null) {
                msgId = msgId.trim();
            }
            return msgId;
        }

    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.message.VMAStore#getIdleTimeout()
     */
    @Override
    public long getIdleTimeout() {
        long idle = idleTimeout > 0 ? (idleTimeout > SOCKET_TIMEOUT) ? SOCKET_TIMEOUT : idleTimeout
                : SOCKET_TIMEOUT;
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("Returning idle timeout as : " + idle);
        }
        return idle;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.message.VMAStore#isConnected()
     */
    @Override
    public boolean isConnected() {
        return (imap != null && imap.isAuthenticated());
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.message.VMAStore#shutdown()
     */
    @Override
    public boolean signout() {
        synchronized (connectionLock) {
            if (imap != null) {
                /*
                 * LOGOUT
                 */
                try {
                    imap.logout();
                } catch (Exception e) {

                }
                /*
                 * DISCONNECT
                 */
                try {
                    imap.disconnect();
                } catch (Exception e) {

                }
                /*
                 * CLOSE
                 */
                try {
                    imap.close();
                } catch (Exception e) {

                }
                imap = null;
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "signout() done");
        }
        return true;
    }

    public boolean isIdling() {
        return (imap != null) ? imap.idling : false;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see com.vzw.vma.message.VMAStore#login()
     */
    @Override
    public VmaSelectResponse login() throws ProtocolException, IOException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "doing login");
            simulateError();

        }
        VmaSelectResponse response = null;
        
        if(TextUtils.isEmpty(mdn)||TextUtils.isEmpty(pwd)){
            // Temporary fix for this release
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "NO LOGIN [400] Missing login/password.Username="+loginMdn+", Pin="+ pwd);
            }
            Response r = new Response("A0 NO LOGIN [400] Missing login/password");
            throw new ProtocolException(r);
        }
        synchronized (connectionLock) {
            imap = getImapSession(loginMdn, pwd);
            if(!isSendOnly) {
            	response = selectInbox();
            }
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "login mailbox="+response);
        }
        return response;
    }

    /* Overriding method 
     * (non-Javadoc)
     * @see com.vzw.vma.message.VMAStore#clearCache()
     */
    @Override
    public void clearCache() {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "clearCache(). SEND_VMA_MSG_ID_CACHE="+AppSettings.SEND_VMA_MSG_ID_CACHE.size());
        }
        
        AppSettings.SEND_VMA_MSG_ID_CACHE.clear();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "cleared the cached send vma msg ids");
        }
    }
    
    
    /**
     * This Method
     * 
     * @param error
     * @return
     * @throws ProtocolException 
     */
    private void simulateError() throws ProtocolException {
        if (!settings.isVMAErrorSimulationEnabled()) {
            return;
        }
        int errorCode = ApplicationSettings.getInstance().getIntSetting(AppSettings.KEY_VMA_SIMULATE_ERROR);
        Logger.debug("Simulate error code : " + errorCode);
        String errorMsg = null;
        switch (errorCode) {
        case AppErrorCodes.VMA_SYNC_MISSING_LOGIN_OR_PASSWORD:
            errorMsg = "A0 NO LOGIN [400] Missing login/password";
            break;
        case AppErrorCodes.VMA_SYNC_LOGIN_FAILED:
            errorMsg = "A0 NO LOGIN failed.";
            break;
        case AppErrorCodes.VMA_SYNC_MAILBOX_DOES_NOT_EXIST:
            errorMsg = "A0 NO SELECT [408] Mailbox does not exist";
            break;
        case AppErrorCodes.VMA_SYNC_NOT_A_VMA_SUBSCRIBER:
            errorMsg = "A0 NO [450]Subscriber not found with that MDN";
            break;
        case AppErrorCodes.VMA_SYNC_SESSION_WRONG_STATE:
            errorMsg = "A0 NO LOGIN [402] Session in wrong state";
            break;
        case AppErrorCodes.VMA_SYNC_ANOTHER_COMMAND_ALREADY_IN_PROGRESS:
            errorMsg = "A0 NO IDLE [403] Another command already in progress";
            break;
        case AppErrorCodes.VMA_SYNC_HAS_SMS_BLOCKING:
            errorMsg = "A0 NO [451] Subscriber has SMS blocking";
            break;
        case AppErrorCodes.VMA_SYNC_ACCOUNT_SUSPENDED:
            errorMsg = "A0 NO [452] Subscriber is suspended";
            break;
        case AppErrorCodes.VMA_SYNC_SUBSCRIBER_HAS_MMS_BLOCKING:
            errorMsg = "A0 NO [453] Subscriber has MMS blocking";
            break;
        case AppErrorCodes.VMA_SYNC_FAILED_ANTISPAM_CHECK:
            errorMsg = "A0  NO [454] Failed anti-spam check";
            break;
        case AppErrorCodes.VMA_SYNC_ISTD_NOT_SUPPORTED:
            errorMsg = "A0 NO [460] International destination numbers are not supported";
            break;
        case AppErrorCodes.VMA_SYNC_ALLDESTINATION_MDNS_BLOCKED_BY_USAGE_CONTROL:
            errorMsg = "A0 NO [455] All destination MDNs blocked by Usage Control";
            break;
        case AppErrorCodes.VMA_SYNC_UC_LIMIT_REACHED:
            errorMsg = "A0 NO [456] UC limit reached";
            break;
        case AppErrorCodes.VMA_SYNC_UC_SYSTEM_ERROR:
            errorMsg = "A0 NO [457] UC system error";
            break;
        case AppErrorCodes.VMA_SYNC_SUBSCRIBER_HAS_INSUFFICIENT_FUNDS:
            errorMsg = "A0 NO [458] Subscriber has insufficient funds";
            break;

        default:
            Logger.debug("Simulating sync errors. unknown error code:" + errorCode);
            break;
        }
        if (errorMsg != null) {
            Logger.debug("Simulating sync errors.");
            throw new ProtocolException(new Response(errorMsg));
        }
    }


}
