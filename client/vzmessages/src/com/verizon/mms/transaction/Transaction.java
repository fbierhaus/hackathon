/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.transaction;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.ContentUris;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.Telephony.MmsSms.PendingMessages;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.util.SendingProgressTokenManager;

/**
 * Transaction is an abstract class for notification transaction, send transaction
 * and other transactions described in MMS spec.
 * It provides the interfaces of them and some common methods for them.
 */
public abstract class Transaction extends Observable {
    private final int mServiceId;

    protected Context mContext;
    protected String mId;
    protected TransactionState mTransactionState;
    protected TransactionSettings mTransactionSettings;
    protected TransactionBundle mArgs;
    private static Map<String, InetAddress> inetAddCache = new HashMap<String, InetAddress>(5);
    private static InetAddress lastUsedAddress = null;

    /**
     * Identifies push requests.
     */
    public static final int NOTIFICATION_TRANSACTION = 0;
    /**
     * Identifies deferred retrieve requests.
     */
    public static final int RETRIEVE_TRANSACTION     = 1;
    /**
     * Identifies send multimedia message requests.
     */
    public static final int SEND_TRANSACTION         = 2;
    /**
     * Identifies send read report requests.
     */
    public static final int READREC_TRANSACTION      = 3;

    public Transaction(Context context, int serviceId,
            TransactionSettings settings, TransactionBundle args) {
        mContext = context;
        mTransactionState = new TransactionState();
        mServiceId = serviceId;
        mTransactionSettings = settings;
        mArgs = args;
    }

    public Transaction(Context context, int serviceId, TransactionSettings connectionSettings) {
    	this(context, serviceId, connectionSettings, null);
	}

    public static void deletePendingTableEntry(Context context, Uri uri) {
		//Uri uri = mTransactionState.getContentUri();

    	if(Logger.IS_DEBUG_ENABLED){
    		Logger.debug(Transaction.class, "BYPASS: MMS xn complete now deleting uri = " + uri);
    	}
    	// delete the pending table entry explicitly
    	String val = "" + ContentUris.parseId(uri);
    	// Default values for pending messages
   
    	if(Logger.IS_DEBUG_ENABLED){
    		Logger.debug(Transaction.class, "BYPASS: MMS xn complete now deleting uri = " + uri + "val = " + val);
    	}
    	int rows = context.getContentResolver().delete(VZUris.getMmsSmsPendingUri(), PendingMessages.MSG_ID + "=?", new String[]{val});
    	if(Logger.IS_DEBUG_ENABLED){
    		Logger.debug(Transaction.class, "BYPASS: MMS xn complete deleted rows: " + rows);
    	}
    }    

	/**
     * Returns the transaction state of this transaction.
     *
     * @return Current state of the Transaction.
     */
    @Override
    public TransactionState getState() {
        return mTransactionState;
    }

    /**
     * An instance of Transaction encapsulates the actions required
     * during a MMS Client transaction.
     */
    public abstract void process();
    
    
    /**
     * Execute the process in the supplied Executor
     * 
     * @param executor
     */
    public abstract void process(ThreadPoolExecutor executor);


    /**
     * Used to determine whether a transaction is equivalent to this instance.
     *
     * @param transaction the transaction which is compared to this instance.
     * @return true if transaction is equivalent to this instance, false otherwise.
     */
    public boolean isEquivalent(Transaction transaction) {
        return getClass().equals(transaction.getClass())
                && mId.equals(transaction.mId);
    }

    /**
     * Get the service-id of this transaction which was assigned by the framework.
     * @return the service-id of the transaction
     */
    public int getServiceId() {
        return mServiceId;
    }

    public TransactionSettings getConnectionSettings() {
        return mTransactionSettings;
    }

    public void setConnectionSettings(TransactionSettings settings) {
        mTransactionSettings = settings;
    }

    public TransactionBundle getArgs() {
    	return mArgs;
    }

    /**
     * A common method to send a PDU to MMSC.
     *
     * @param pdu A byte array which contains the data of the PDU.
     * @return A byte array which contains the response data.
     *         If an HTTP error code is returned, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] sendPdu(byte[] pdu) throws IOException {
        return sendPdu(SendingProgressTokenManager.NO_TOKEN, pdu,
                mTransactionSettings.getMmscUrl());
    }

    /**
     * A common method to send a PDU to MMSC.
     *
     * @param pdu A byte array which contains the data of the PDU.
     * @param mmscUrl Url of the recipient MMSC.
     * @return A byte array which contains the response data.
     *         If an HTTP error code is returned, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] sendPdu(byte[] pdu, String mmscUrl) throws IOException {
        return sendPdu(SendingProgressTokenManager.NO_TOKEN, pdu, mmscUrl);
    }

    /**
     * A common method to send a PDU to MMSC.
     *
     * @param token The token to identify the sending progress.
     * @param pdu A byte array which contains the data of the PDU.
     * @return A byte array which contains the response data.
     *         If an HTTP error code is returned, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] sendPdu(long token, byte[] pdu) throws IOException {
        return sendPdu(token, pdu, mTransactionSettings.getMmscUrl());
    }

    /**
     * A common method to send a PDU to MMSC.
     *
     * @param token The token to identify the sending progress.
     * @param pdu A byte array which contains the data of the PDU.
     * @param mmscUrl Url of the recipient MMSC.
     * @return A byte array which contains the response data.
     *         If an HTTP error code is returned, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] sendPdu(long token, byte[] pdu, String mmscUrl) throws IOException {
        String nUrl = ensureRouteToHost(mmscUrl, mTransactionSettings);
        return HttpUtils.httpConnection(
                mContext, token,
                nUrl,
                pdu, HttpUtils.HTTP_POST_METHOD,
                mTransactionSettings.isProxySet(),
                mTransactionSettings.getProxyAddress(),
                mTransactionSettings.getProxyPort());
    }

    /**
     * A common method to retrieve a PDU from MMSC.
     *
     * @param url The URL of the message which we are going to retrieve.
     * @return A byte array which contains the data of the PDU.
     *         If the status code is not correct, an IOException will be thrown.
     * @throws IOException if any error occurred on network interface or
     *         an HTTP error code(>=400) returned from the server.
     */
    protected byte[] getPdu(String url) throws IOException {
        String nUrl = ensureRouteToHost(url, mTransactionSettings);
        return HttpUtils.httpConnection(
                mContext, SendingProgressTokenManager.NO_TOKEN,
                nUrl, null, HttpUtils.HTTP_GET_METHOD,
                mTransactionSettings.isProxySet(),
                mTransactionSettings.getProxyAddress(),
                mTransactionSettings.getProxyPort());
    }

    /**
     * Make sure that a network route exists to allow us to reach the host in the
     * supplied URL, and to the MMS proxy host as well, if a proxy is used.
     * @param url The URL of the MMSC to which we need a route
     * @param settings Specifies the address of the proxy host, if any
     * @throws IOException if the host doesn't exist, or adding the route fails.
     */
    private String ensureRouteToHost(String url, TransactionSettings settings) throws IOException {
    	if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(Transaction.class, "ensureRouteToHost ==> url=" + url);
		}

    	if (url == null) {
    		throw new IOException("Null URL");
    	}

    	ConnectivityManager connMgr =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		
        // Debug
        boolean foundConnectedMmsNetwork = connMgr.getNetworkInfo(MmsConfig.getNetworkConnectivityMode()).isConnected();
		if (!foundConnectedMmsNetwork) {
			Logger.error(Transaction.class, "In ensureRouteToHost but network " + MmsConfig.getNetworkConnectivityMode() + 
					" is not connected");
		}
	
		String returnUrl = url;
        // Proxy
		String ipAddress;
        if (settings.isProxySet()) {
            String proxyAddr = settings.getProxyAddress();
            InetAddress inetAddress = getInetAddress(proxyAddr);
            if (null == inetAddress) {
            	// special case for VZ - if caching did not work either 
            	if (proxyAddr.equalsIgnoreCase("mms.vtext.com")) {           		
            		ipAddress = "69.78.81.38";
                    if (Logger.IS_DEBUG_ENABLED) {
                    	Logger.debug(TransactionService.class, "=> getInetAddress: Using hardcoded start address!");
                    }
            	} else {
            		throw new IOException("Cannot establish route for " + url + ": Unknown host1");
            	}
            } else {
            	int inetAddr = lookupHost(inetAddress);
            	if (inetAddr == -1) {
                    throw new IOException("Cannot establish route for " + url + ": Unknown host");
                } else {
                    if (!connMgr.requestRouteToHost(
                            MmsConfig.getNetworkConnectivityMode(), inetAddr)) {
                        throw new IOException("Cannot establish route to proxy " + inetAddr);
                    }
                }
                ipAddress = inetAddress.getHostAddress();
            }
        } else {
            Uri uri = Uri.parse(url);
            InetAddress inetAddress = getInetAddress(uri.getHost());
            if (null == inetAddress) {
            	// special case for VZ - if caching did not work either 
            	if (uri.getHost().equalsIgnoreCase("mms.vtext.com") && uri.getQuery() == null) {
            		ipAddress = "69.78.81.38";
                    if (Logger.IS_DEBUG_ENABLED) {
                    	Logger.debug(TransactionService.class, "=> getInetAddress: Using hardcoded start address!");
                    }

            	} else {
            		throw new IOException("Cannot establish route for " + url + ": Unknown host1");
            	}
            } else {
                int inetAddr = lookupHost(inetAddress);
                if (inetAddr == -1) {
                    throw new IOException("Cannot establish route for " + url + ": Unknown host2");
                } else {
                    if (!connMgr.requestRouteToHost(
                            MmsConfig.getNetworkConnectivityMode(), inetAddr)) {
                        throw new IOException("Cannot establish route to " + inetAddr + " for " + url);
                    }
                }
                ipAddress = inetAddress.getHostAddress();
            }
            
            // Since this is not Proxy (i.e. it's an end-point), let's use the IP
			returnUrl = uri.getScheme() + "://" + ipAddress
					+ ((uri.getPort() == -1) ? "" : ":" + uri.getPort()) // Port
					+ uri.getPath() // Path
					+ ((uri.getQuery() != null && uri.getQuery().length() > 0) ? "?" + uri.getQuery() : "") // Query
					+ ((uri.getFragment() != null && uri.getFragment().length() > 0) ? "#" + uri.getFragment() : "") // Fragment
					; 
        }
        
    	if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(Transaction.class, "ensureRouteToHost ==> returnUrl=" + returnUrl);
		}
        
        return returnUrl;
    }
    
    private static InetAddress getInetAddress(String hostname) {
    	return getInetAddress(hostname, 0);
    }
    
    private static InetAddress getInetAddress(String hostname, int count) {
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByName(hostname);
            inetAddCache.put(hostname, inetAddress);
            lastUsedAddress = inetAddress;
            
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(TransactionService.class, "=> getInetAddress: Found - hostname=" + hostname + ", inetAddress=" + inetAddress);
            }
        } catch (UnknownHostException e) {
        	Logger.error(Transaction.class, "getInetAddress failed for hostname=" + hostname + ", count=" + count);
        	if (inetAddCache.containsKey(hostname)) {
        		inetAddress = inetAddCache.get(hostname);
                if (Logger.IS_DEBUG_ENABLED) {
                	Logger.debug(TransactionService.class, "=> getInetAddress: Using cached - hostname=" + hostname + ", inetAddress=" + inetAddress);
                }
            } else if (count < 2) {
            	count++;
            	inetAddress = getInetAddress(hostname, count);
            } else {
            	inetAddress = lastUsedAddress;
            }
        }
        return inetAddress;
    }
    
    /**
     * Look up a host name and return the result as an int. Works if the argument
     * is an IP address in dot notation. Obviously, this can only be used for IPv4
     * addresses.
     * @param hostname the name of the host (or the IP address)
     * @return the IP address as an {@code int} in network byte order
     */
    // TODO: move this to android-common
    public static int lookupHost(String hostname) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            return -1;
        }
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24)
                | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8)
                |  (addrBytes[0] & 0xff);
        return addr;
    }

    public static int lookupHost(InetAddress inetAddress) {
        byte[] addrBytes;
        int addr;
        addrBytes = inetAddress.getAddress();
        addr = ((addrBytes[3] & 0xff) << 24)
                | ((addrBytes[2] & 0xff) << 16)
                | ((addrBytes[1] & 0xff) << 8)
                |  (addrBytes[0] & 0xff);
        return addr;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": type = " + getType() + ", serviceId = " + mServiceId + ", id = " + mId;
    }

    /**
     * Get the type of the transaction.
     *
     * @return Transaction type in integer.
     */
    abstract public int getType();
}
