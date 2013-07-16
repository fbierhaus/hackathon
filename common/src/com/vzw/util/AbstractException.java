/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vzw.util;

import java.text.MessageFormat;

/**
 *
 * @author hud
 *
 * Base class for exceptions
 *
 * May be moved to ConfigTool or any common module as needed
 */
abstract public class AbstractException extends Exception {

	protected DataWrapper<?> dataWrapper = null;

	public AbstractException(Throwable cause) {
		super(cause);
	}

	public AbstractException(String message, Object...args) {
		super(MessageFormat.format(message, args));
	}

	public AbstractException(Throwable cause, String message, Object...args) {
		super(MessageFormat.format(message, args), cause);
	}
	public AbstractException() {
	}



	public <T> AbstractException(DataWrapper<T> data, Throwable cause) {
		super(cause);
		this.dataWrapper = data;
	}

	public <T> AbstractException(DataWrapper<T> data, String message, Object...args) {
		super(MessageFormat.format(message, args));
		this.dataWrapper = data;
	}

	public <T> AbstractException(DataWrapper<T> data, Throwable cause, String message, Object...args) {
		super(MessageFormat.format(message, args), cause);
		this.dataWrapper = data;
	}
	public <T> AbstractException(DataWrapper<T> data) {
		this.dataWrapper = data;
	}


	public <T> DataWrapper<T> getDataWrapper() {
		return (DataWrapper<T>)dataWrapper;
	}

	public <T> void setDataWrapper(DataWrapper<T> data) {
		this.dataWrapper = data;
	}

	public <T> T getData() {
		return dataWrapper == null ? null : (T)dataWrapper.getData();
	}

	public String getDataString() {
		return dataWrapper == null ? "" : dataWrapper.toString();
	}

	
}
