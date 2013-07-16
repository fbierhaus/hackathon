/**
 * ISyncClient.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync;

import java.util.HashSet;
import java.util.Set;

import com.verizon.messaging.vzmsgs.provider.SyncItem.ItemPriority;
import com.vzw.vma.sync.refactor.imapconnection.impl.VMASync.VMAConnectionType;

/**
 * This interface is used to interact with upper layer(UI or Service Binder) to send or receive the changes to
 * VMA system.
 * 
 * @author Jegadeesan
 * @Since Feb 12, 2013
 */
public interface SyncClient extends Runnable, DataConnectivityListener {

    public boolean isConnected();

    public boolean isRunning();

    public boolean isShutdown();

    public void startOrWakeUp();

    public boolean isMine(ItemPriority itemPriority);

    public boolean isMine(Set<ItemPriority> itemPriority);
    
    public VMAConnectionType getConnectionType();
    
    public String getThreadName();
    
    public void shutdown();

}
