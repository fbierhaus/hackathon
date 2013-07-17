/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vzw.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * SimpleDateFormat is not thread-safe, let's make it thread-safe
 * @author hud
 */
public class ThreadSafeSimpleDateFormat {

	private ThreadLocal<SimpleDateFormat> sdf = null;

	public ThreadSafeSimpleDateFormat(final String pattern) {
		sdf = new ThreadLocal<SimpleDateFormat>() {

			@Override
			protected SimpleDateFormat initialValue() {
				return new SimpleDateFormat(pattern);
			}

		};
	}

	public Date parse(String input) throws ParseException {
		return sdf.get().parse(input);
	}

	public String format(Date date) {
		return sdf.get().format(date);
	}
	
	public void setTimeZone(TimeZone tz) {
		sdf.get().setTimeZone(tz);
	}
}
