package com.vzw.hackthon.scheduler.test;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vzw.hackathon.Channel;
import com.vzw.hackathon.GroupEvent;
import com.vzw.hackathon.GroupEventManager;
import com.vzw.hackathon.Member;
import com.vzw.hackathon.MemberStatus;
import com.vzw.hackathon.User;
import com.vzw.hackthon.scheduler.SchedulerProperties;
import com.vzw.util.HttpClientUtil;
import com.vzw.util.JSONUtil;
import com.vzw.util.db.DBManager;

public class GroupEventManagerTest {
	
	private static GroupEventManager		gem = null;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		gem = GroupEventManager.getInstance();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public final void testLoadGroupEventFromDb() {
		GroupEvent ge = gem.loadGroupEventFromDb(1);
		System.out.println(ge);
	}

	@Test
	public final void testCreateGroupEvent() {
		String js = "{'masterMdn':'9251000001','showId':'1234455555555','channelId': '101##11111', 'showTime':'2830303000000','memberList': [{ 'mdn': '9251000002'},{ 'mdn': '9251000003'}]}";
		//String js = "{'masterMdn':'9251000001','showId':'2234455555555','channelId': '102##11111', 'showTime':'2830303000000','memberList': [{ 'mdn': '9251000002'},{ 'mdn': '9251000003'}]}";
		//String js = "{'masterMdn':'9252000001','showId':'3234455555555','channelId': '103##11111', 'showTime':'2830303000000','memberList': [{ 'mdn': '9252000002'},{ 'mdn': '9252000003'}]}";
		
		GroupEvent ge = null;
		try {
			ge = JSONUtil.toJava(js, GroupEvent.class, "memberList", Member.class);
			System.out.println(ge);
			
			gem.createGroupEvent(ge);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		

	}

	//@Test
	public final void testUpdateMemberStatus() {
		gem.updateMemberStatus(101, "9250000002", MemberStatus.ACCEPTED);
	}

	//@Test
	public final void testUpdateMemberLastChannelId() {
		gem.updateMemberLastChannelId(101, "9250000003", "002##10001");
	}

	@Test
	public final void testGetChannel() {
		Channel channel = gem.getChannel("002##10001");
		System.out.println(channel);
	}

	@Test
	public final void testGetUser() {
		User user = gem.getUser("9250000001");
		System.out.println(user);
	}

	//@Test
	public final void testSchedulePlay() {
		gem.schedulePlay("9250000001", 101);
	}

}
