package com.vzw.hackthon.scheduler.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vzw.hackathon.GroupEvent;
import com.vzw.hackathon.HackathonUtil;
import com.vzw.hackthon.scheduler.EventReminder;

public class EventReminderTest {
	
	private static EventReminder		eventReminder = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		eventReminder = new EventReminder();
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

	@Test
	public void testGetGroupEventsForReminder() {
		List<GroupEvent> geList = eventReminder.getGroupEventsForReminder();
		
		System.out.println("done " + geList);
	}
	
	@Test
	public void testBuildReminderString() {
		GroupEvent ge = new HackathonUtil().loadGroupEventFromDb(1);
		String s = eventReminder.buildReminderString(ge);
		System.out.println(s);
	}

}
