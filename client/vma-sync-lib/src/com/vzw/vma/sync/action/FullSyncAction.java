/**
 * FullSyncAction.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync.action;

import java.io.IOException;

import android.content.Context;

import com.strumsoft.android.commons.logger.Logger;
import com.sun.mail.iap.ProtocolException;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.vzw.vma.message.VMAStore;
import com.vzw.vma.sync.SyncClient;
import com.vzw.vma.sync.refactor.imapconnection.impl.FullSync;
import com.vzw.vma.sync.refactor.imapconnection.impl.VMASync;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Feb 22, 2013
 */
public class FullSyncAction {
    private static FullSyncAction instance;
    private VMAStore store;
    private AppSettings settings;
    private SyncItemDao syncItemDao;
    private Context context;

    public synchronized static FullSyncAction getInstance() {
        return instance;
    }

    public synchronized static FullSyncAction getInstance(AppSettings settings, VMAStore store, SyncItemDao syncItemDao,
            Context context) {
        if (instance == null) {
            instance = new FullSyncAction(settings, store, syncItemDao, context);
        }else{
            instance.updateOldReference(settings, store, syncItemDao, context);
        }
        return instance;
    }

    /**
     * This Method 
     * @param settings
     * @param store
     * @param syncItemDao
     * @param context
     */
    private void updateOldReference(AppSettings settings, VMAStore store, SyncItemDao syncItemDao, Context context) {
        this.context = context;
        this.store = store;
        this.settings = settings;
        this.syncItemDao = syncItemDao;
    }

    /**
     * 
     * Constructor
     */
    private FullSyncAction(AppSettings settings, VMAStore store, SyncItemDao syncItemDao, Context context) {
        this.context = context;
        this.store = store;
        this.settings = settings;
        this.syncItemDao = syncItemDao;

    }

    public boolean doXConvFetch(SyncClient syncClient) throws IOException, ProtocolException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("doXConvFetch().");
        }
        FullSync fetchUids = new FullSync(settings, store, syncItemDao, context , syncClient);
        fetchUids.fetchUids();
        int messageTotalCount = fetchUids.getMsgChangesCount();
        settings.put(AppSettings.KEY_FULLSYNC_COMPLETED, true);
        settings.put(AppSettings.KEY_FULLSYNC_MSG_COUNT, messageTotalCount);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug("doXConvFetch(). done");
        }
        return true;
    }

}
