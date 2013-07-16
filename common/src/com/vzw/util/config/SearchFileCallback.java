/*
 * Created on Nov. 17, 2003
 *
 */
package com.vzw.util.config;

import java.io.File;

/**
 * @author hud
 *
 */
public abstract class SearchFileCallback
{
	/** 
	 * member var 
	 */
	protected boolean			m_bSucceeded;
	
	/**
	 * constructor
	 */
	public SearchFileCallback()
	{
		init();
	}
	
	/** 
	 * init
	 */
	public void init()
	{
		m_bSucceeded		= true;
	}
	
	
	/**
	 * Checks if file search is succeeded
	 */
	public boolean succeeded()
	{
		return m_bSucceeded;
	}
	
	/**
	 * Initialize before searching
	 */
	public void initialize()
	{
		m_bSucceeded	= true;
	}
	
	
	/** 
	 * Interface for search file engine
	 * to set succeed/fail
	 */
	public void setSucceeded()
	{
		m_bSucceeded	= true;
	}
	
	public void setFailed()
	{
		m_bSucceeded	= false;
	}
	
	 
	/**
	 * process function
	 */
	public boolean execFileProc(File file)
	{
		return true;
	}
	
	public boolean execAfterSubdir(File file)
	{
		return true;
	}
}
