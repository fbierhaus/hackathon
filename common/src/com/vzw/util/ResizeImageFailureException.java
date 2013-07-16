/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

/**
 *
 * @author hud
 */
public class ResizeImageFailureException extends SimpleException {
	public ResizeImageFailureException() {
		super();
	}

	public ResizeImageFailureException(Throwable cause, String message, Object...args) {
		super(cause, message, args);
	}

	public ResizeImageFailureException(String message, Object...args) {
		super(message, args);
	}

	public ResizeImageFailureException(Throwable cause) {
		super(cause);
	}

	public <T> ResizeImageFailureException(DataWrapper<T>  data) {
		super(data);
	}

	public <T> ResizeImageFailureException(DataWrapper<T> data, Throwable cause, String message, Object...args) {
		super(data, cause, message, args);
	}

	public <T> ResizeImageFailureException(DataWrapper<T> data, String message, Object...args) {
		super(data, message, args);
	}

	public <T> ResizeImageFailureException(DataWrapper<T> data, Throwable cause) {
		super(data, cause);
	}
	
}
