package com.vzw.hackthon.scheduler.test;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vzw.hackthon.scheduler.MemberWatcher;

public class MemberWatcherTest {
	private static MemberWatcher		mw = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		mw = new MemberWatcher();
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
	public final void testRun() {
		mw.start();
		
		try {
			TimeUnit.SECONDS.sleep(300);
		}
		catch (InterruptedException e) {
			
		}		
	}

}
