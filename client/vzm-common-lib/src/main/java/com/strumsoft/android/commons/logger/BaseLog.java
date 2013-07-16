/**
 * BaseLog.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.strumsoft.android.commons.logger;

/**
 * This class/interface   
 * @author Jegadeesan.M
 * @Since  Apr 4, 2012
 */
public interface BaseLog {



    public void warn(String message);
    public void debug(String message);
    public void info(String message);
    public void error(String message);

}