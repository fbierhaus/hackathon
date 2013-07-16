/**
 * SyncActionFactory.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.action;

import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.sync.UiNotification;
import com.vzw.vma.sync.refactor.PDUDao;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Feb 26, 2013
 */
public class SyncActionFactory {

    private static SyncActionFactory instance;
    private VMAEventHandler eventHandler;
    private PDUDao pduDao;
    private SyncItemDao syncItemDao;
    private UiNotification uiNotification;
    private AppSettings settings;

    /**
     * 
     * Constructor
     */
    public SyncActionFactory(VMAEventHandler eventHandler, PDUDao pduDao, SyncItemDao syncItemDao,
            UiNotification uiNotification, AppSettings settings) {
        this.uiNotification=uiNotification;
        this.pduDao=pduDao;
        this.syncItemDao=syncItemDao;
        
    }

}
