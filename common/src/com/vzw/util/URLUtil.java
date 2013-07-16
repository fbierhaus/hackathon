/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author hud
 */
public class URLUtil {
	
	public static class URLComponents {
		private String			scheme = null;
		private String			host = null;
		private int				port = 0;
		private String			uri = null;
		private boolean			secured = false;

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getScheme() {
			return scheme;
		}

		public void setScheme(String scheme) {
			this.scheme = scheme;
		}

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public boolean isSecured() {
			return secured;
		}

		public void setSecured(boolean secured) {
			this.secured = secured;
		}
		
		/**
		 * Construct the root url:
		 * 
		 * scheme://host:port
		 * @return 
		 */
		public String toRootUrl() {
			
			StringBuilder sb = new StringBuilder();
			sb.append(scheme).append("://").append(host);
			
			if (!((secured && port == 443) || (!secured && port == 80))) {
				sb.append(':').append(port);
			}
			
			return sb.toString();
		}

		@Override
		public String toString() {
			return toRootUrl() + uri;
		}
		
		
		
		
		
	}
	
	
	private static final Pattern		URL_PATTERN = Pattern.compile("^((http)|(https))://([\\.\\w\\-]+)(:(\\d+))?");
	
	public static URLComponents parse(String url) {
		Matcher m = URL_PATTERN.matcher(url);
		
		if (!m.find()) {
			return null;
		}
		
		URLComponents uc = new URLComponents();
		
		StringBuffer sbUri = new StringBuffer();
		m.appendReplacement(sbUri, m.group());
		sbUri.delete(0, sbUri.length());
		m.appendTail(sbUri);
		
		uc.setUri(sbUri.toString());
		uc.setScheme(m.group(1));
		
		String schemeHttp = m.group(2);
		uc.setHost(m.group(4));
		
		String portStr = m.group(6);
		boolean ssl = (schemeHttp == null);
		
		if (portStr == null) {
			uc.setPort(ssl ? 443 : 80);
		}
		else {
			uc.setPort(Integer.parseInt(portStr));
		}
		
		uc.setSecured(ssl);
		
		return uc;
	}
	
	
	public static String getTopLevelDomain(String host, boolean includingDot) {
		
		String ret = null;
		
		int idx1 = host.lastIndexOf(".");
		if (idx1 >= 0) {
			int idx2 = host.lastIndexOf(".", idx1 - 1);
			if (idx2 >= 0) {
				ret = includingDot ? host.substring(idx2) : host.substring(idx2 + 1);
			}
		}
		
		return ret;
	}
	
	
	/**
	 * Return a URL of srcUrl, in which the protocol part is adjusted to use refUrl's protocol
	 * e.g., srcUrl = http://www.vzw.com, refUrl = https://aaa.bbb.com
	 * always use default port
	 * then return https://www.vzw.com
	 * 
	 * @param srcUrl
	 * @param refUrl
	 * @return 
	 */
	public static String resetProtocol(String srcUrl, String refUrl) {
		
		URLComponents srcUc = parse(srcUrl);
		URLComponents refUc = parse(refUrl);
		
		// assert not null
		assert srcUc != null && refUc != null : String.format("Invalid srcUrl=%s or refUrl=%s", srcUrl, refUrl);
		
		
		srcUc.setSecured(refUc.isSecured());
		srcUc.setScheme(refUc.getScheme());
		
		// always use default port
		if (srcUc.isSecured()) {
			srcUc.setPort(443);
		}
		else {
			srcUc.setPort(80);
		}
		
		
		
		
		return srcUc.toString();
	}
}
