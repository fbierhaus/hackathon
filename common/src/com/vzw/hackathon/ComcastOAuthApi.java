package com.vzw.hackathon;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;

public class ComcastOAuthApi {
	
	public static final String CONSUMER_KEY = "74d20869-5df3-4c79-b410-dd5635804449";
	public static final String CONSUMER_SECRET = "MXFOmP4YQoWHYEGWBeMpVg==";
	public static final String REQUEST_TOKEN_ENDPOINT_URL = "https://xip.comcast.net/xip/proxy/rtune/authtoken";
	public static final String CUSTOM_HEADER_KEY = "X-CIM-RT-Authorization";
	public static final String  DEVICE_KEY = "Ed8EoFlo0fKDkdfESrijdJC68-lg1k9TJvFkq75hSUkKV-IGkYTYa6dzAqwdQFR2.c.I9sTOWWoSO6y4nhKJOSAMXrf1iEFmhi7gLOfsTvl8HEwW8t0XDwc5vcYsGTOOJYu5WSc4aietpHA11oVi_t3FQ**.a.2";
	
	
	public static final Logger	logger = Logger.getLogger(ComcastOAuthApi.class);
	
	
	public static void tuneChannel(Channel channel) {
		String urlStr = "https://xip.comcast.net/xip/proxy/rtune/device/" + DEVICE_KEY + "/tune/tv/vcn/703";

		String chValue = null;
		
		
		logger.info("Start to tune channel");
		HttpURLConnection request = null, req2 = null;
        try {
	        
			
			// create a consumer object and configure it with the access
	        // token and token secret obtained from the service provider
	        OAuthConsumer consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
	        //? consumer.setTokenWithSecret(ACCESS_TOKEN, TOKEN_SECRET);
	
	        // create an HTTP request to a protected resource
	        URL url = new URL(REQUEST_TOKEN_ENDPOINT_URL);
	        request = (HttpURLConnection) url.openConnection();
	
	        // sign the request
	        consumer.sign(request);
	
	        // send the request
	        request.connect();		
	        
	        
	        
	        // create a new service provider object and configure it with
	        // the URLs which provide request tokens, access tokens, and
	        // the URL to which users are sent in order to grant permission
	        // to your application to access protected resources
	        /////OAuthProvider provider = new DefaultOAuthProvider(
	        /////        REQUEST_TOKEN_ENDPOINT_URL, REQUEST_TOKEN_ENDPOINT_URL, REQUEST_TOKEN_ENDPOINT_URL);

	        // fetches a request token from the service provider and builds
	        // a url based on AUTHORIZE_WEBSITE_URL and CALLBACK_URL to
	        // which your app must now send the user
	        /////String urlt = provider.retrieveRequestToken(consumer, null); 
	        
	        
	        
	        
	        /////provider.retrieveAccessToken(consumer, null);
	        
	        
			
			// Get Response
			InputStream is = request.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				//response.append('\r');
			}
			rd.close();
			
			
			chValue = response.toString();
			request.disconnect();
			request = null;
			
			
			/////////////////////////////////////////////////////////
			// now create request to tune
			URL url2 = new URL(urlStr);
			req2 = (HttpURLConnection)url2.openConnection();
			
			
			req2.setRequestProperty(CUSTOM_HEADER_KEY, chValue);
			req2.setRequestProperty("Content-Length","0");			
			req2.setRequestMethod("POST");
			req2.setDoOutput(true);
			
			
			req2.setReadTimeout(10000);
			req2.setUseCaches(false);
			req2.setDoInput(true);
			
			
			
			consumer.sign(req2);		
			
			DataOutputStream wr = new DataOutputStream(
					req2.getOutputStream());
			wr.writeBytes("");
			wr.flush();
			wr.close();
			
			
			is = req2.getInputStream();
			rd = new BufferedReader(new InputStreamReader(is));
			
			response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			
			
			String resp = response.toString();
			
			System.out.println(resp);
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
        finally {
        	if (request != null) {
        		request.disconnect();
        	}
        	
        	if (req2 != null) {
        		req2.disconnect();
        	}
        }
        
        
        System.out.println("done");
 
	}
	
	
	public static void postMessage(String msg) {
		
		logger.info("Start to post message");
		String chValue = null;
		
		HttpURLConnection request = null, req2 = null;
        try {
        	String urlStr = "https://xip.comcast.net/xip/proxy/rtune/device/" + DEVICE_KEY + "/tune/message/" + URLEncoder.encode(msg, "UTF-8");
			
			// create a consumer object and configure it with the access
	        // token and token secret obtained from the service provider
	        OAuthConsumer consumer = new DefaultOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
	        //? consumer.setTokenWithSecret(ACCESS_TOKEN, TOKEN_SECRET);
	
	        // create an HTTP request to a protected resource
	        URL url = new URL(REQUEST_TOKEN_ENDPOINT_URL);
	        request = (HttpURLConnection) url.openConnection();
	
	        // sign the request
	        consumer.sign(request);
	
	        // send the request
	        request.connect();		
	        
	        
	        
	        // create a new service provider object and configure it with
	        // the URLs which provide request tokens, access tokens, and
	        // the URL to which users are sent in order to grant permission
	        // to your application to access protected resources
	        /////OAuthProvider provider = new DefaultOAuthProvider(
	        /////        REQUEST_TOKEN_ENDPOINT_URL, REQUEST_TOKEN_ENDPOINT_URL, REQUEST_TOKEN_ENDPOINT_URL);

	        // fetches a request token from the service provider and builds
	        // a url based on AUTHORIZE_WEBSITE_URL and CALLBACK_URL to
	        // which your app must now send the user
	        /////String urlt = provider.retrieveRequestToken(consumer, null); 
	        
	        
	        
	        
	        /////provider.retrieveAccessToken(consumer, null);
	        
	        
			
			// Get Response
			InputStream is = request.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				//response.append('\r');
			}
			rd.close();
			
			
			chValue = response.toString();
			request.disconnect();
			request = null;
			
			
			/////////////////////////////////////////////////////////
			// now create request to tune
			URL url2 = new URL(urlStr);
			req2 = (HttpURLConnection)url2.openConnection();
			
			
			req2.setRequestProperty(CUSTOM_HEADER_KEY, chValue);
			req2.setRequestProperty("Content-Length","0");			
			req2.setRequestMethod("POST");
			req2.setDoOutput(true);
			
			
			req2.setReadTimeout(10000);
			req2.setUseCaches(false);
			req2.setDoInput(true);
			
			
			
			consumer.sign(req2);		
			
			DataOutputStream wr = new DataOutputStream(
					req2.getOutputStream());
			wr.writeBytes("");
			wr.flush();
			wr.close();
			
			
			is = req2.getInputStream();
			rd = new BufferedReader(new InputStreamReader(is));
			
			response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			
			
			String resp = response.toString();
			
			System.out.println(resp);
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
        finally {
        	if (request != null) {
        		request.disconnect();
        	}
        	
        	if (req2 != null) {
        		req2.disconnect();
        	}
        }
        
        
        System.out.println("done");		
	}
	
	public static void main(String[] args) throws Exception {
		//tuneChannel(null);
		postMessage("this is test 2");
	}
	
}
