/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vzw.util;


/**
 *
 * @author hud
 */
public class SimpleException extends AbstractException {

	public SimpleException() {
		super();
	}

	public SimpleException(Throwable cause, String message, Object...args) {
		super(cause, message, args);
	}

	public SimpleException(String message, Object...args) {
		super(message, args);
	}

	public SimpleException(Throwable cause) {
		super(cause);
	}

	public <T> SimpleException(DataWrapper<T>  data) {
		super(data);
	}

	public <T> SimpleException(DataWrapper<T> data, Throwable cause, String message, Object...args) {
		super(data, cause, message, args);
	}

	public <T> SimpleException(DataWrapper<T> data, String message, Object...args) {
		super(data, message, args);
	}

	public <T> SimpleException(DataWrapper<T> data, Throwable cause) {
		super(data, cause);
	}

}
