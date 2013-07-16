/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import org.apache.http.impl.auth.BasicScheme;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.FormattedHeader;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.params.AuthParams;
import org.apache.http.message.BufferedHeader;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EncodingUtils;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.protocol.HTTP;

/**
 *
 * @author hud
 */
public class CustomBasicScheme extends BasicScheme {
	
	public static final String	AUTH_HEADER_CREDENTIAL_KEY = "CUSTOM_AUTH_HEADER_CREDENTIAL";
	public static final String	AUTH_HEADER_CHALLENGE_KEY = "CUSTOM_AUTH_HEADER_CHALLENGE";
	
	private String authHeaderCredential = null;
	private String authHeaderChallenge = null;
	
	private boolean _proxy = false;

	public CustomBasicScheme(String authHeaderCredential, String authHeaderChallenge) {
		super();
		
		this.authHeaderCredential = authHeaderCredential;
		this.authHeaderChallenge = authHeaderChallenge;
	}

	@Override
	public boolean isProxy() {
		return _proxy;
	}
	
	
    /**
	 * 
	 * Have to copy from the source as the headers are hard-coded
	 * 
     * Processes the given challenge token. Some authentication schemes
     * may involve multiple challenge-response exchanges. Such schemes must be able
     * to maintain the state information when dealing with sequential challenges
     *
     * @param header the challenge header
     *
     * @throws MalformedChallengeException is thrown if the authentication challenge
     * is malformed
     */
	@Override
    public void processChallenge(final Header header) throws MalformedChallengeException {
		
		try {
			super.processChallenge(header);
			
			// we override the proxy private member, which is bad
			_proxy = isProxy();
		}
		catch (MalformedChallengeException e) {
			// do our head
			
			/*
			 * Codes copied but modified from the super method
			 */
			if (header == null) {
				throw new IllegalArgumentException("Header may not be null");
			}
			String authheader = header.getName();
			
			// modified here, only check authHeaderChallenge
			_proxy = false;
			if (!authheader.equalsIgnoreCase(authHeaderChallenge)) {
				throw new MalformedChallengeException("Unexpected header name: " + authheader + ", should be " + authHeaderChallenge);
			}

			//
			// copied codes following
			//
			CharArrayBuffer buffer;
			int pos;
			if (header instanceof FormattedHeader) {
				buffer = ((FormattedHeader) header).getBuffer();
				pos = ((FormattedHeader) header).getValuePos();
			} else {
				String s = header.getValue();
				if (s == null) {
					throw new MalformedChallengeException("Header value is null");
				}
				buffer = new CharArrayBuffer(s.length());
				buffer.append(s);
				pos = 0;
			}
			while (pos < buffer.length() && HTTP.isWhitespace(buffer.charAt(pos))) {
				pos++;
			}
			int beginIndex = pos;
			while (pos < buffer.length() && !HTTP.isWhitespace(buffer.charAt(pos))) {
				pos++;
			}
			int endIndex = pos;
			String s = buffer.substring(beginIndex, endIndex);
			if (!s.equalsIgnoreCase(getSchemeName())) {
				throw new MalformedChallengeException("Invalid scheme identifier: " + s);
			}

			parseChallenge(buffer, pos, buffer.length());
		}
		
		
    }
	
	
    /**
     * Produces basic authorization header for the given set of {@link Credentials}.
     *
     * @param credentials The set of credentials to be used for authentication
     * @param request The request being authenticated
     * @throws InvalidCredentialsException if authentication credentials are not
     *   valid or not applicable for this authentication scheme
     * @throws AuthenticationException if authorization string cannot
     *   be generated due to an authentication failure
     *
     * @return a basic authorization string
     */
	@Override
    public Header authenticate(
            final Credentials credentials,
            final HttpRequest request) throws AuthenticationException {

        if (credentials == null) {
            throw new IllegalArgumentException("Credentials may not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }

        String charset = AuthParams.getCredentialCharset(request.getParams());
        return doAuthenticate(credentials, charset, isProxy());
    }	
	
    /**
	 * Have to copy from source code as the header names are hard-coded
	 * 
     * Returns a basic <tt>Authorization</tt> header value for the given
     * {@link Credentials} and charset.
     *
     * @param credentials The credentials to encode.
     * @param charset The charset to use for encoding the credentials
     *
     * @return a basic authorization header
     */
    private Header doAuthenticate(
            final Credentials credentials,
            final String charset,
            boolean proxy) {
        if (credentials == null) {
            throw new IllegalArgumentException("Credentials may not be null");
        }
        if (charset == null) {
            throw new IllegalArgumentException("charset may not be null");
        }

        StringBuilder tmp = new StringBuilder();
        tmp.append(credentials.getUserPrincipal().getName());
        tmp.append(":");
        tmp.append((credentials.getPassword() == null) ? "null" : credentials.getPassword());

        byte[] base64password = Base64.encodeBase64(
                EncodingUtils.getBytes(tmp.toString(), charset));

        CharArrayBuffer buffer = new CharArrayBuffer(32);
		
        if (proxy) {
            buffer.append(AUTH.PROXY_AUTH_RESP);
        } else {
			if (authHeaderCredential == null) {
				buffer.append(AUTH.WWW_AUTH_RESP);
			}
			else {
				buffer.append(authHeaderCredential);
			}
        }
        buffer.append(": Basic ");
        buffer.append(base64password, 0, base64password.length);

        return new BufferedHeader(buffer);
    }
	
}
