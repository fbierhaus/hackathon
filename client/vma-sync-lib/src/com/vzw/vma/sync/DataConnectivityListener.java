/**
 * DataConnectivityListener.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync;

/**
 * This class/interface   
 * @author Jegadeesan M
 * @Since  Nov 13, 2012
 */
public interface DataConnectivityListener {
    public void onConnectionChanged(boolean isConnected , int type);
}
