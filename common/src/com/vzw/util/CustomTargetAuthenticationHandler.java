/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.impl.client.DefaultTargetAuthenticationHandler;
import org.apache.http.protocol.HttpContext;

/**
 *
 * @author hud
 */
public class CustomTargetAuthenticationHandler extends DefaultTargetAuthenticationHandler {
	
	private String		challengeHeaderName = null;
	
	public CustomTargetAuthenticationHandler(String challengeHeaderName) {
		super();
		this.challengeHeaderName = challengeHeaderName;
	}

	@Override
	public Map<String, Header> getChallenges(HttpResponse response, HttpContext context) throws MalformedChallengeException {
        if (response == null) {
            throw new IllegalArgumentException("HTTP response may not be null");
        }
        Header[] headers = response.getHeaders(challengeHeaderName == null ? AUTH.WWW_AUTH : challengeHeaderName);
        return parseChallenges(headers);
	}
	
	
}
