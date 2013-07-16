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
 * Purpose of this class is to wrap the generic object Object
 */
public class DataWrapper<T> {
	private T				data = null;
	private Object[]		params = null;

	public DataWrapper() {
	}

	public DataWrapper(T data, Object...params) {
		this.data = data;
		this.params = params;
	}

	public T getData() {
		return data;
	}

	public void setData(T data, Object...params) {
		this.data = data;
		this.params = params;
	}

	@Override
	public String toString() {
		if (data == null) {
			return "";
		}
		else {
			if (params == null || params.length == 0) {
				return data.toString();
			}
			else {
				return MessageFormat.format(data.toString(), params);
			}
		}

		//if (params == null || params.length == 0) {
		//	return data.toString();
		//}

		// get resource if necessary
		/*
		ResourceBundle rb = VmaProperties.getInstance().getResourceBundle();
		String dataStr = data.toString();
		String val = null;
		try {
			val = rb.getString(dataStr);
		}
		catch (MissingResourceException mre) {
			// do nothing
		}

		if (val == null) {
			val = dataStr;
		}

		return MessageFormat.format(val, params);
		 *
		 */
	}
}
