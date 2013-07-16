package com.vzw.hackathon.apihandler;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.vzw.util.HttpClientProperties;
import com.vzw.util.HttpClientUtil;

public class VZWAPIHandler {

	private static final Logger logger = Logger.getLogger(VZWAPIHandler.class);
	
	public static boolean sendSMS(String from, ArrayList<String> toList, String message) {
		
		logger.info("sendSMS - from=" + from + ", toList=" + toList + ", message=" + message);
		
		boolean success = true;
		
	    for (String to : toList) {
	    	success = success && sendSMS(from, to, message);
	    }

		return success;
	}
	
	public static boolean sendSMS(String from, String to, String message) {
		
		logger.info("sendSMS - from=" + from + ", to=" + to + ", message=" + message);
		
		boolean success = false;

		HttpClientProperties props = null;
		HttpClientUtil.Client hClient = null;
		props = HttpClientProperties.getInstance();

		String res;
        try {
        	
        	String url = "http://comcastmobilityteam.api.mashery.com/message/sms";
        	
	        // initialize http client
	        hClient = HttpClientUtil.initClient(props, "", url);

	        HttpPost post = new HttpPost(url);
	        
	        post.addHeader("Authorization", "Basic Sk9FX0hhY2thdGhvbjU6RDNxfU0ycD0=");
	        post.addHeader("X-Param-Keys", "com.bea.wlcp.wlng.plugin.sms.RequestDeliveryReportFlag");
	        post.addHeader("X-Param-Values", "true");
	        post.addHeader("Content-Type", "application/json");
	        post.addHeader("X-Originating-Ip", "204.15.241.97");
	    
	        String request = "{\"addresses\":[\"tel:" + to + "\"],\"message\": \"" + message + "\", \"senderName\": \"tel:" + from + "\"}";
	        
	        StringEntity entity = new StringEntity(request,"UTF-8");
	        
	        post.setEntity(entity);
	        res = hClient.execute(post, new ResponseHandler<String>() {

	        	@Override
	        	public String handleResponse(HttpResponse resp) throws ClientProtocolException, IOException {
	        		// TODO Auto-generated method stub
	        		HttpEntity entity = resp.getEntity();

	        		return EntityUtils.toString(entity);
	        	}
	        });
	        
			System.out.println(res);
			
	        success = true;
        } catch (Exception e) {
	        logger.error("error sending SMS", e);
        }



		return success;
	}
	
	public static void main(String[] args) {
		String mdn = "9084426933";
		VZWAPIHandler.sendSMS("650066007047", mdn, "what the hack?");
	}

}
