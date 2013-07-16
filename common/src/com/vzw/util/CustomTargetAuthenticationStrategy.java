/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.http.protocol.HttpContext;

/**
 *
 * @author hud
 */
public class CustomTargetAuthenticationStrategy extends TargetAuthenticationStrategy {
	private String		challengeHeaderName = null;
	
	public CustomTargetAuthenticationStrategy(String challengeHeaderName) {
		super();
		this.challengeHeaderName = challengeHeaderName;
	}

//	@Override
//	public Map<String, Header> getChallenges(HttpResponse response, HttpContext context) throws MalformedChallengeException {
//        if (response == null) {
//            throw new IllegalArgumentException("HTTP response may not be null");
//        }
//        Header[] headers = response.getHeaders(challengeHeaderName == null ? AUTH.WWW_AUTH : challengeHeaderName);
//        return parseChallenges(headers);
//	}	

	@Override
	public Map<String, Header> getChallenges(HttpHost authhost, HttpResponse response, HttpContext context) throws MalformedChallengeException {
		return super.getChallenges(authhost, response, context);
	}
	
	
	
}
