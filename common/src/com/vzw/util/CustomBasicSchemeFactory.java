/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.params.HttpParams;

/**
 *
 * @author hud
 */
public class CustomBasicSchemeFactory implements AuthSchemeFactory {
	@Override
	public AuthScheme newInstance(HttpParams params) {
		return new CustomBasicScheme(
			(String)params.getParameter(CustomBasicScheme.AUTH_HEADER_CREDENTIAL_KEY),
			(String)params.getParameter(CustomBasicScheme.AUTH_HEADER_CHALLENGE_KEY)
		);
	}
	
}
