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

public class ComcastAPIHandler {

	private static final Logger logger = Logger.getLogger(ComcastAPIHandler.class);
	
	private static HttpClientUtil.Client	hClient = null;
	
	static {
		HttpClientProperties props = HttpClientProperties.getInstance();
		hClient = HttpClientUtil.initClient(props, "", "http://www.google.com");

	}
	
	public static boolean tuneChannel(ArrayList<String> deviceIdList, String channelId) {
		
		logger.info("tuneChannel - deviceIdList=" + deviceIdList + ", channelId=" + channelId);
		
		boolean success = true;
		
	    for (String deviceId : deviceIdList) {
	    	success = success && tuneChannel(deviceId, channelId);
	    }

		return success;
	}
	
	public static boolean tuneChannel(String deviceId, String channelId) {
		
		logger.info("tuneChannel - deviceId=" + deviceId + ", channelId=" + channelId);
		
		boolean success = false;

		HttpClientProperties props = null;
		//HttpClientUtil.Client hClient = null;
		props = HttpClientProperties.getInstance();

		String res;
        try {
        	
        	String url = "http://comcastmobilityteam.api.mashery.com/message/sms";
        	
	        // initialize http client
	        //hClient = HttpClientUtil.initClient(props, "", url);

	        HttpPost post = new HttpPost(url);
	        
	        post.addHeader("Authorization", "Basic Sk9FX0hhY2thdGhvbjU6RDNxfU0ycD0=");
	        post.addHeader("X-Param-Keys", "com.bea.wlcp.wlng.plugin.sms.RequestDeliveryReportFlag");
	        post.addHeader("X-Param-Values", "true");
	        post.addHeader("Content-Type", "application/json");
	        post.addHeader("X-Originating-Ip", "204.15.241.97");
	    
	        String request = "";
	        
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
	        logger.error("error", e);
        }
		return success;
	}
	
	public static String getDeviceId(String mdn) {
		
		logger.info("getDeviceId - mdn=" + mdn);
		
		String deviceId = null;

		HttpClientProperties props = null;
		//HttpClientUtil.Client hClient = null;
		props = HttpClientProperties.getInstance();

		String res;
        try {
        	
        	String url = "http://comcastmobilityteam.api.mashery.com/message/sms";
        	
	        // initialize http client
	        //hClient = HttpClientUtil.initClient(props, "", url);

	        HttpPost post = new HttpPost(url);
	        
	        post.addHeader("Authorization", "Basic Sk9FX0hhY2thdGhvbjU6RDNxfU0ycD0=");
	        post.addHeader("X-Param-Keys", "com.bea.wlcp.wlng.plugin.sms.RequestDeliveryReportFlag");
	        post.addHeader("X-Param-Values", "true");
	        post.addHeader("Content-Type", "application/json");
	        post.addHeader("X-Originating-Ip", "204.15.241.97");
	    
	        String request = "";
	        
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
			
	        deviceId = "";
        } catch (Exception e) {
	        logger.error("error", e);
        }
		return deviceId;
	}
	
	public static String getJID(String mdn) {
		
		logger.info("getJID - mdn=" + mdn);
		
		String JID = null;

		HttpClientProperties props = null;
		//HttpClientUtil.Client hClient = null;
		props = HttpClientProperties.getInstance();

		String res;
        try {
        	
        	String url = "http://comcastmobilityteam.api.mashery.com/message/sms";
        	
	        // initialize http client
	        //hClient = HttpClientUtil.initClient(props, "", url);

	        HttpPost post = new HttpPost(url);
	        
	        post.addHeader("Authorization", "Basic Sk9FX0hhY2thdGhvbjU6RDNxfU0ycD0=");
	        post.addHeader("X-Param-Keys", "com.bea.wlcp.wlng.plugin.sms.RequestDeliveryReportFlag");
	        post.addHeader("X-Param-Values", "true");
	        post.addHeader("Content-Type", "application/json");
	        post.addHeader("X-Originating-Ip", "204.15.241.97");
	    
	        String request = "";
	        
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
			
			JID = "";
        } catch (Exception e) {
	        logger.error("error", e);
        }
		return JID;
	}
	
	public static String getChannelId(String deviceId) {
		
		logger.info("getChannelId - deviceId=" + deviceId);
		
		String channelId = null;

		HttpClientProperties props = null;
		//HttpClientUtil.Client hClient = null;
		props = HttpClientProperties.getInstance();

		String res;
        try {
        	
        	String url = "http://comcastmobilityteam.api.mashery.com/message/sms";
        	
	        // initialize http client
	        //hClient = HttpClientUtil.initClient(props, "", url);

	        HttpPost post = new HttpPost(url);
	        
	        post.addHeader("Authorization", "Basic Sk9FX0hhY2thdGhvbjU6RDNxfU0ycD0=");
	        post.addHeader("X-Param-Keys", "com.bea.wlcp.wlng.plugin.sms.RequestDeliveryReportFlag");
	        post.addHeader("X-Param-Values", "true");
	        post.addHeader("Content-Type", "application/json");
	        post.addHeader("X-Originating-Ip", "204.15.241.97");
	    
	        String request = "";
	        
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
			
			channelId = "";
        } catch (Exception e) {
	        logger.error("error", e);
        }
		return channelId;
	}
	
	public static void main(String[] args) {
	}

}
