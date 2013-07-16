/**
 * HandlerTask.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.common.handler;

import android.content.Intent;
import android.os.Message;

/**
 * This class/interface   
 * @author Jegadeesan
 * @Since  Feb 20, 2013
 */
public class HandlerTask {
    public int taskActionOrId; 
    public int serviceStartId; 
    public String  name; 
    public Intent intent;

    /**
     * @param msg
     *  Constructor 
     */
    public HandlerTask(Message msg) {
        taskActionOrId = msg.what;
        serviceStartId = msg.arg1;
        intent=(Intent)msg.obj;
    }

}
