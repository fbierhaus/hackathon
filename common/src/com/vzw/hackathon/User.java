package com.vzw.hackathon;

/**
 * Works as an addressbook as well as current channel
 * the user is watching (tuned)
 * @author hud
 *
 */

public class User {
	private String				mdn = null;
	private String				channelId = null;	// the channel he's watching, may be null in the very beginning
	private String				name = null;
	
	
	public String getMdn() {
		return mdn;
	}
	public void setMdn(String mdn) {
		this.mdn = mdn;
	}
	public String getChannelId() {
		return channelId;
	}
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
}
