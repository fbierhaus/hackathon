package com.vzw.hackthon.scheduler.test;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vzw.hackathon.Channel;
import com.vzw.hackathon.GroupEvent;
import com.vzw.hackathon.GroupEventManager;
import com.vzw.hackathon.Member;
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
		String js = "{masterMdn:'9250000001',showId:'1234455555555',showTime:1830303000000,memberList: [{ mdn: '9250000002'},{ mdn: '9250000003'}]}";
		GroupEvent ge = JSONUtil.toJava(js, GroupEvent.class, "memberList", Member.class);
		
		System.out.println(ge);
		
		gem.createGroupEvent(ge);
	}

	@Test
	public final void testUpdateMemberStatus() {
		//gem.updateMemberStatus(groupEventId, mdn, status);
	}

	@Test
	public final void testUpdateMemberLastChannelId() {
		fail("Not yet implemented"); // TODO
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

	@Test
	public final void testSchedulePlay() {
		fail("Not yet implemented"); // TODO
	}

}
