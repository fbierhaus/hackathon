/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author hud
 * 
 * Utility class for jsp tag functions. No Java EE library needed here
 * but apache commons libraries are needed
 */
public class JspTagUtil {
	
	private static final Logger		logger = Logger.getLogger(JspTagUtil.class);
	
	/**
	 * 
	 * @param val
	 * @return 
	 */
	public static String escapeEcmaScript(String val) {
		return StringEscapeUtils.escapeEcmaScript(val);
	}
	
	/**
	 * 
	 * @param obj
	 * @return 
	 */
	public static String toJson(Object obj) {
		return JSONUtil.toJsonString(obj);
	}
	

	/**
	 * 
	 * @param val
	 * @return 
	 */
	public static String escapeHtml(String val) {
		return StringEscapeUtils.escapeHtml4(val);
	}

	/**
	 * escape XML entities
	 * @param val
	 * @return 
	 */
	public static String escapeXml(String val) {
		return StringEscapeUtils.escapeXml(val);
	}

	/**
	 * Remove leading and tailing white spaces, replace multiple spaces
	 * with one space
	 * @param val
	 * @return
	 */
	public static String normalizeSpace(String val) {
		return org.apache.commons.lang3.StringUtils.normalizeSpace(val);
	}
	
	
	/**
	 * 
	 * @param val
	 * @param maxLen
	 * @return 
	 */
	public static String truncate(String val, int maxLen) {
		if (val == null) {
			return "";
		}
		else if (val.length() < maxLen) {
			return val;
		}
		else {
			return val.substring(0, maxLen);
		}
	}	




	/**
	 * 
	 * @return 
	 */
	public static Date now() {
		return new Date();
	}

	/**
	 * 
	 * @param arg
	 * @return 
	 */
	public static String encodeUrlArg(String arg) {
		try {
			return URLEncoder.encode(arg, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			LogUtil.info(logger, "Unable to encode url arg: {0}", arg);
			return arg;
		}
	}

}
