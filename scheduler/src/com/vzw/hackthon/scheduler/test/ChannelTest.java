package com.vzw.hackthon.scheduler.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.vzw.hackathon.Channel;

public class ChannelTest {

	@Test
	public void testFromChannelId() {
		Channel channel = Channel.fromChannelId("256##10934");
		
		System.out.println(channel);
	}

}
