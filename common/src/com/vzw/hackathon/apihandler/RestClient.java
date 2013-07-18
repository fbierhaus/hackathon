/**
 * 
 */
package com.vzw.hackathon.apihandler;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.vzw.hackathon.GroupEvent;
import com.vzw.hackathon.Member;
import com.vzw.hackathon.MemberStatus;
import com.vzw.util.HttpClientProperties;
import com.vzw.util.HttpClientUtil;
import com.vzw.util.JSONUtil;

/**
 * @author fred
 *
 */
public class RestClient {

	private static final Logger logger = Logger.getLogger(RestClient.class);
	
	private static HttpClientUtil.Client	hClient = null;
	
	
	static {
		HttpClientProperties props = HttpClientProperties.getInstance();
		hClient = HttpClientUtil.initClient(props, "", "http://192.168.1.13:8080");
	}
	
	
	public static int createGroupEvent(GroupEvent ge, String serverBaseURL){
		int id = -1;
		
        try {
        	String json = JSONUtil.toJsonString(ge);
        	
        	String url = serverBaseURL + "/server/groupEvents";
        	
	        HttpPost post = new HttpPost(url);
	        
			List<NameValuePair> formParams = new ArrayList<NameValuePair>(1);
			formParams.add(new BasicNameValuePair("groupEvent", json));

			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
			post.setEntity(entity);
			
	        String res = hClient.execute(post, new ResponseHandler<String>() {

	        	@Override
	        	public String handleResponse(HttpResponse resp) throws ClientProtocolException, IOException {
	        		// TODO Auto-generated method stub
	        		HttpEntity entity = resp.getEntity();

	        		return EntityUtils.toString(entity);
	        	}
	        });
	        
	        id = extractId(res);
	        
	    } catch (Exception e) {
	        logger.error("error", e);
	    }
        
		return id;
	}
	
	
	
	public static void rsvp(int groupEventId, String mdn, MemberStatus status, String serverBaseURL){
        try {
        	
        	String url = serverBaseURL + "/server/groupEvents/" + groupEventId + "/rsvp";
	        URI uri = new URIBuilder(url).addParameter("mdn", mdn).addParameter("status", status.toString()).build();
        	
	        HttpGet get = new HttpGet(uri);
			
	        String res = hClient.execute(get, new ResponseHandler<String>() {

	        	@Override
	        	public String handleResponse(HttpResponse resp) throws ClientProtocolException, IOException {
	        		// TODO Auto-generated method stub
	        		HttpEntity entity = resp.getEntity();

	        		return EntityUtils.toString(entity);
	        	}
	        });
	        
	    } catch (Exception e) {
	        logger.error("error", e);
	    }
	}
	
	
	public static void postMessage(String sender, String recipient, String message, String serverBaseURL){
        try {
        	
        	String url = serverBaseURL + "/server/messages";
        	
	        HttpPost post = new HttpPost(url);
	        
			List<NameValuePair> formParams = new ArrayList<NameValuePair>(3);
			formParams.add(new BasicNameValuePair("mdn", recipient));
			formParams.add(new BasicNameValuePair("from", sender));
			formParams.add(new BasicNameValuePair("message", message));

			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
			post.setEntity(entity);
			
	        String res = hClient.execute(post, new ResponseHandler<String>() {

	        	@Override
	        	public String handleResponse(HttpResponse resp) throws ClientProtocolException, IOException {
	        		// TODO Auto-generated method stub
	        		HttpEntity entity = resp.getEntity();

	        		return EntityUtils.toString(entity);
	        	}
	        });
	        
	        
	    } catch (Exception e) {
	        logger.error("error", e);
	    }
		
	}
	
	
	
	
	protected static int extractId(String response){
		JSONObject jsonObject = JSONObject.fromObject( response );
		ServerResponse sr = (ServerResponse) JSONUtil.toJava(jsonObject, ServerResponse.class);
		return sr.getId();
	}
	
	public static void main(String[] args) {
//		new RestClient().testGroupEvent();
//		new RestClient().testRsvp();
		new RestClient().testPostMessage();
	}
	
	protected void testGroupEvent(){
		GroupEvent ge = new GroupEvent();
		ge.setMasterMdn("123456789");
		ge.setChannelId("1");
		ge.setShowId("2");
		ge.setShowTime(new Date());
		ge.setShowName("Game of Thrones");
		Member m1 = new Member();
		m1.setMdn("223456789");
		Member m2 = new Member();
		m2.setMdn("323456789");
		List<Member> memberList = new ArrayList<Member>();
		memberList.add(m1);
		memberList.add(m2);
		ge.setMemberList(memberList);
		String serverBaseUrl = "http://localhost:8080/";
		int id = RestClient.createGroupEvent(ge, serverBaseUrl);
		
		System.out.println("************** id: " + id);
	}

	protected void testRsvp(){
		String serverBaseUrl = "http://localhost:8080/";
		RestClient.rsvp(3, "9259991234", MemberStatus.DECLINED, serverBaseUrl);
	}
	
	protected void testPostMessage(){
		String sender = "9255551234";
		String from = "9259991234";
		String message = "Hello World!";
		String serverBaseUrl = "http://localhost:8080/";

		RestClient.postMessage(sender, from, message, serverBaseUrl);
	}
}
