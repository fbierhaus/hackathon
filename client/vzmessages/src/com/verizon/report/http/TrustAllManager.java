/**
 * TrustAllManager.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.report.http;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * This class/interface
 * 
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * @Since Jul 3, 2012
 */
public class TrustAllManager implements X509TrustManager {
    
    public void checkClientTrusted(X509Certificate[] cert, String authType) throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] cert, String authType) throws CertificateException {
    }

    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
