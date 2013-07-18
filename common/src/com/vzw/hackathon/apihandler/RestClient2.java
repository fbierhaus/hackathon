package com.vzw.hackathon.apihandler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.vzw.hackathon.GroupEvent;
import com.vzw.hackathon.MemberStatus;
import com.vzw.util.JSONUtil;

public class RestClient2 {

	public static final String CHARSET = "UTF-8";
	private static final Logger logger = Logger.getLogger(RestClient2.class);

	public static String encodeParam(Map<String, String> paramMap)
			throws Exception {
		String urlParameters = null;

		for (Entry<String, String> entry : paramMap.entrySet()) {
			String pp = entry.getKey() + "="
					+ URLEncoder.encode(entry.getValue(), CHARSET);
			if (urlParameters == null) {
				urlParameters = pp;
			} else {
				urlParameters += "&" + pp;
			}
		}

		return urlParameters;
	}

	public static String execute(boolean doPost, String targetURL,
			Map<String, String> paramMap) throws Exception {

		String urlParameters = encodeParam(paramMap);
		URL url;
		HttpURLConnection connection = null;
		try {
			// Create connection
			url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			
			if (doPost) {
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
				connection.setDoInput(true);
			}
			else {
				connection.setRequestMethod("GET");
			}

			connection.setRequestProperty("Content-Length",
					"" + Integer.toString(urlParameters.getBytes().length));
			//connection.setRequestProperty("Content-Language", "en-US");

			connection.setReadTimeout(10000);
			connection.setUseCaches(false);
			connection.setDoOutput(true);

			// Send request
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			// Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();

		} catch (Exception e) {

			e.printStackTrace();
			return null;

		} finally {

			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	
	protected static int extractId(String response){
		JSONObject jsonObject = JSONObject.fromObject( response );
		ServerResponse sr = (ServerResponse) JSONUtil.toJava(jsonObject, ServerResponse.class);
		return sr.getId();
	}
	
	public static int createGroupEvent(GroupEvent ge, String serverBaseURL) {
		int id = -1;
		
		try {
			Map<String, String> pm = new HashMap<String, String>();
			pm.put("groupEvent", JSONUtil.toJsonString(ge));
			
			String res = execute(true, serverBaseURL + "/server/groupEvents", pm);
			
			logger.info("got group event id: " + res);
			id = extractId(res);
		}
		catch (Exception e) {
			logger.error("Failed to create group event: " + ge, e);
		}

		return id;
	}
	
	public static void rsvp(int groupEventId, String mdn, MemberStatus status, String serverBaseURL){
        try {
        	
        	String url = serverBaseURL + "/server/groupEvents/" + groupEventId + "/rsvp";
        	logger.info("url = " + url);
        	
			Map<String, String> pm = new HashMap<String, String>();
			pm.put("mdn", mdn);
			pm.put("status", status.toString());
        	String res = execute(false, url, pm);

        	logger.info("rsvp response: " + res);
	    } catch (Exception e) {
	        logger.error("error", e);
	    }
	}
	
	public static void postMessage(String sender, String recipient, String message, String serverBaseURL){
        try {
        	
        	String url = serverBaseURL + "/server/messages";
        	Map<String, String> pm = new HashMap<String, String>();
        	pm.put("mdn", recipient);
        	pm.put("from", sender);
        	pm.put("message", message);
        	
	        String res = execute(true, url, pm);
	        logger.info("postMessage response: " + res);
	        
	        
	    } catch (Exception e) {
	        logger.error("error", e);
	    }
		
	}

}
