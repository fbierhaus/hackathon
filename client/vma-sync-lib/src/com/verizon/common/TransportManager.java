/**
 * TransportManager.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.net.ConnectivityManager;
import android.provider.Settings.System;

import com.strumsoft.android.commons.logger.Logger;

/**
 * This class/interface
 * 
 * @author Jegadeesan.M
 * @Since Jul 17, 2012
 */
public class TransportManager {

    private Context context;
    private ConnectivityManager dataConnectivity;
    private DefaultHttpClient httpclient;

    /**
     * 
     * Constructor
     */
    public TransportManager(Context context) {
        this.context = context;
        dataConnectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        httpclient= new DefaultHttpClient();
    }

//    public TransportManager(Context context, boolean isVZWSSLEnabled) {
//        this.context = context;
//        dataConnectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (isVZWSSLEnabled) {
//            httpclient = getSSLClient();
//        } else {
//            httpclient = new DefaultHttpClient();
//        }
//
//    }

    public boolean hasDataConnectivity() {
        // test for connection
        if (dataConnectivity.getActiveNetworkInfo() != null
                && dataConnectivity.getActiveNetworkInfo().isAvailable()
                && dataConnectivity.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            if (Logger.IS_WARNING_ENABLED) {
                Logger.warn(TransportManager.class, "No Internet Connection found.");
            }
            return false;
        }
    }

//    private DefaultHttpClient getSSLClient() {
//        DefaultHttpClient client = new DefaultHttpClient();
//        try {
//            X509TrustManager tm = new X509TrustManager() {
//                public void checkClientTrusted(X509Certificate[] xcs, String string)
//                        throws CertificateException {
//                }
//
//                public void checkServerTrusted(X509Certificate[] xcs, String string)
//                        throws CertificateException {
//                }
//
//                public X509Certificate[] getAcceptedIssuers() {
//                    return null;
//                }
//            };
//            SSLContext ctx = SSLContext.getInstance("TLS");
//            ctx.init(null, new TrustManager[] { tm }, null);
//            SSLSocketFactory ssf = new MySSLSocketFactory(ctx);
//            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//            ClientConnectionManager ccm = client.getConnectionManager();
//            SchemeRegistry sr = ccm.getSchemeRegistry();
//            sr.register(new Scheme("https", ssf, 443));
//            return new DefaultHttpClient(ccm, client.getParams());
//        } catch (Exception ex) {
//
//        }
//        return client;
//    }
//
//    public class MySSLSocketFactory extends SSLSocketFactory {
//        SSLContext sslContext = SSLContext.getInstance("TLS");
//
//        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
//                KeyManagementException, KeyStoreException, UnrecoverableKeyException {
//            super(truststore);
//
//            TrustManager tm = new X509TrustManager() {
//                public void checkClientTrusted(X509Certificate[] chain, String authType)
//                        throws CertificateException {
//                }
//
//                public void checkServerTrusted(X509Certificate[] chain, String authType)
//                        throws CertificateException {
//                }
//
//                public X509Certificate[] getAcceptedIssuers() {
//                    return null;
//                }
//            };
//
//            sslContext.init(null, new TrustManager[] { tm }, null);
//        }
//
//        public MySSLSocketFactory(SSLContext context) throws KeyManagementException,
//                NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
//            super(null);
//            sslContext = context;
//        }
//
//        @Override
//        public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
//                throws IOException, UnknownHostException {
//            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
//        }
//
//        @Override
//        public Socket createSocket() throws IOException {
//            return sslContext.getSocketFactory().createSocket();
//        }
//    }

    public ByteArrayOutputStream makePostRequest(String url) throws IOException {
        if (!hasDataConnectivity()) {
            throw new IOException("No Internet Connection found.");
        }

        HttpPost post = new HttpPost(url);
        HttpResponse response;
        try {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("TransportManager: POST Request: url=" + url+ " starting time"+ java.lang.System.currentTimeMillis());
            }
            HttpParams httpParameters = new BasicHttpParams(); 
            int timeoutConnection = 60000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutConnection);
            post.setParams(httpParameters);
            response = httpclient.execute(post);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("TransportManager: response code=" + response.getStatusLine().getStatusCode()
                        + "response=", response.getStatusLine()+ " ending time for connect"+ java.lang.System.currentTimeMillis());
            }

            HttpEntity entity = response.getEntity();

            if (entity != null) {
                InputStream instream = entity.getContent();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int b;
                while ((b = instream.read()) != -1) {
                    out.write(b);
                }
                out.flush();
                out.close();
                instream.close();
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("TransportManager: message=", new String(out.toByteArray())+ " ending time for read"+ java.lang.System.currentTimeMillis());;
                }
                return out;
            }
        } catch (IOException e) {
            Logger.error(true, "TransportManager:error=" + e.getMessage(), e);
            throw e;
        }
        return null;
    }

    public ByteArrayOutputStream makeGetRequest(String url) throws IOException {
        if (!hasDataConnectivity()) {
            throw new IOException("No Internet Connection found.");
        }
        HttpGet httpget = new HttpGet(url);
        HttpResponse response;
        try {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.info("TransportManager: Get Request: url=" + url);
            }
            response = httpclient.execute(httpget);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug("TransportManager: response code=" + response.getStatusLine().getStatusCode()
                        + "response=", response.getStatusLine());
            }

            HttpEntity entity = response.getEntity();

            if (entity != null) {
                InputStream instream = entity.getContent();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int b;
                while ((b = instream.read()) != -1) {
                    out.write(b);
                }
                out.flush();
                out.close();
                instream.close();
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("TransportManager: message=", new String(out.toByteArray()));
                }
                return out;
            }
        } catch (IOException e) {
            Logger.error("TransportManager:error=" + e.getMessage(), e);
            throw e;
        }
        return null;

    }

}
