/**
 * 
 */
package com.vzw.hackathon.apihandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
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
	
	
	public static int sendFling(byte[] data, String fileName, String serverBaseURL){
		int id = -1;
		
		String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
		String CRLF = "\r\n"; // Line separator required by multipart/form-data.
		
		String url = serverBaseURL + "/server/flings";
		PrintWriter writer = null;
		URLConnection connection = null;
		String charset = "UTF-8";
		try {
			connection = new URL(url).openConnection();
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			
		    OutputStream output = connection.getOutputStream();
		    writer = new PrintWriter(new OutputStreamWriter(output, charset), true); // true = autoFlush, important!

		    // Send binary file.
		    writer.append("--" + boundary).append(CRLF);
		    writer.append("Content-Disposition: form-data; name=\"fling\"; filename=\"" + fileName + "\"").append(CRLF);
		    writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName)).append(CRLF);
		    writer.append("Content-Transfer-Encoding: binary").append(CRLF);
		    writer.append(CRLF).flush();
		    InputStream input = null;
		    try {
		        input = new ByteArrayInputStream(data);
		        byte[] buffer = new byte[1024];
		        for (int length = 0; (length = input.read(buffer)) > 0;) {
		            output.write(buffer, 0, length);
		        }
		        output.flush(); // Important! Output cannot be closed. Close of writer will close output as well.
		    } finally {
		        if (input != null) try { input.close(); } catch (IOException logOrIgnore) {}
		    }
		    writer.append(CRLF).flush(); // CRLF is important! It indicates end of binary boundary.

		    // End of multipart/form-data.
		    writer.append("--" + boundary + "--").append(CRLF);
		    
		} catch (Exception e) {
			System.out.println("Error uploading fling" + e);
		} finally {
		    if (writer != null) writer.close();
		}
		
		// read response
		try {
			InputStream response = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(response, charset));
			StringBuilder sb = new StringBuilder();
		    for (String line; (line = reader.readLine()) != null;) {
		            sb.append(line);
		    }
			if (sb.length() > 0) {
				id = extractId(sb.toString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return id;
	}
	
	
	public static File downloadFling(int flingId, String serverBaseURL, String folderPath){
		String url = serverBaseURL + "/server/flings/" + flingId + ".file";

		
		URLConnection connection;
		try {
			connection = new URL(url).openConnection();
			InputStream response = connection.getInputStream();
			
			String contentType = connection.getHeaderField("Content-Type");
			String contentDisposition = connection.getHeaderField("Content-Disposition");
			logger.debug("***** contentDisposition: " + contentDisposition);
			String filename = null;
			if (contentDisposition == null) {
				filename = "defaultFilename";
			} else {
				filename = contentDisposition.split(";")[1].split("=")[1];
			}
			
			File tempFile = new File(folderPath, filename);

			// read bytes
			response = connection.getInputStream();
			byte[] buffer = new byte[4096];
			int n = - 1;

			OutputStream output = new FileOutputStream(tempFile);
			while ( (n = response.read(buffer)) != -1)
			{
			    if (n > 0)
			    {
			        output.write(buffer, 0, n);
			    }
			}
			output.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	
	
	protected static int extractId(String response){
		logger.debug("Extracting id from: " + response);
		
		JSONObject jsonObject = JSONObject.fromObject( response );
		ServerResponse sr = (ServerResponse) JSONUtil.toJava(jsonObject, ServerResponse.class);
		return sr.getId();
	}
	
	public static void main(String[] args) {
//		new RestClient().testGroupEvent();
//		new RestClient().testRsvp();
//		new RestClient().testPostMessage();
//		new RestClient().testFileUpload();
		new RestClient().testFlingDownload();
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
	
	
	protected void testFileUpload(){
		File file = new File("/tmp/flings/photo.jpg");
		
	    byte []buffer = new byte[(int) file.length()];
	    InputStream ios = null;
	    try {
	        ios = new FileInputStream(file);
	        if ( ios.read(buffer) == -1 ) {
	            throw new IOException("EOF reached while trying to read the whole file");
	        }    
	    } catch (Exception e){
	    	e.printStackTrace();
	    } finally { 
	        try {
	             if ( ios != null ) 
	                  ios.close();
	        } catch ( IOException e) {
	        }
	    }
	    
		String serverBaseUrl = "http://localhost:8080/";
	    int id = RestClient.sendFling(buffer, "photo.jpg", serverBaseUrl);
	    
	    System.out.println("Fling id: " + id);
	}
	
	protected void testFlingDownload(){
		int flingId = 3;
		String folderPath = "/tmp/android";
		
		String serverBaseUrl = "http://localhost:8080/";
		
		RestClient.downloadFling(flingId, serverBaseUrl, folderPath);
	}
}
