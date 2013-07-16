/**
 * BatchModeController.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui;

import java.util.ArrayList;

/**
 * This class/interface   
 * @author Essack
 * @Since  May 29, 2012
 */
public abstract class BatchModeThreadsController {
    protected ArrayList<Long> mThreads = null;
    
    protected BatchModeThreadsController() {
        mThreads = new ArrayList<Long>();
    }
    
    public ArrayList<Long> getBatchedThreads() {
        return mThreads;
    }
    
    public void clearIds() {
        mThreads.clear();
    }
    
    abstract public void onThreadsUpdated();
}
