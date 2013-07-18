package com.vzw.hackthon.scheduler.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vzw.hackathon.apihandler.ComcastAPIHandler;
import com.vzw.hackthon.scheduler.SchedulerProperties;
import com.vzw.util.HttpClientUtil;
import com.vzw.util.config.AbstractProperties;
import com.vzw.util.db.DBManager;
import com.vzw.util.db.DBPool;
import com.vzw.util.db.DBUtil;

public class CommonTest {
	
	private static DBPool				dbPool = null;
	private static SchedulerProperties 	props = null;
	private static HttpClientUtil.Client	hClient = null;	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		dbPool = DBManager.getDBPool();
		props = SchedulerProperties.getInstance();
		
		// initialize http client
		hClient = HttpClientUtil.initClient(props, "", "http://www.yahoo.com");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}


	
	//@Test
	public void testDb1() throws Exception {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			conn = dbPool.getConnection();
			ps = conn.prepareStatement("select count(*) as cnt from group_event");
			rs = ps.executeQuery();
			
			rs.next();
			int n = rs.getInt(1);
			
			System.out.println(n);
		}
		finally {
			DBManager.release(rs, ps, conn);
		}

	}
	
	//@Test
	public void testProps() throws Exception {
		System.out.println(props.getString("test.property1"));
	}
	
	//@Test
	public void testHttpClient() throws Exception {
		HttpGet hg = new HttpGet("http://www.yahoo.com");
		
		String res = hClient.execute(hg, new ResponseHandler<String>() {

			@Override
			public String handleResponse(HttpResponse resp) throws ClientProtocolException,
					IOException {
				// TODO Auto-generated method stub
				HttpEntity entity = resp.getEntity();
				
				return EntityUtils.toString(entity);
			}
		});
		
		System.out.println(res);
		
	}
	
	//@Test
	public void testTimestamp() {
		long t = new Date().getTime();
		System.out.println("t=" + t);
		
		long t1 = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES) + t;
		System.out.println(t1);
		System.out.println(MessageFormat.format("t1={0,time,yyyyMMddHHmmss}", t1));
	}
	
	@Test
	public void demoTuneChannel() {
		ComcastAPIHandler.tuneChannel("9253248967", "703##6718065");
	}

}
