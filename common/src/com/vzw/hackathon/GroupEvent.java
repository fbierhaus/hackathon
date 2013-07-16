package com.vzw.hackathon;

import java.util.Date;

public class GroupEvent {
	
	private int id = -1;
	private String showId = null;
	private String channelId = null;
	private Date showTime = null;
	private String showName = null;
	private Date createTime = null;
	private String masterMdn = null;
	

	public GroupEvent() {
		// TODO Auto-generated constructor stub
	}



	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public String getShowId() {
		return showId;
	}


	public void setShowId(String showId) {
		this.showId = showId;
	}


	public String getChannelId() {
		return channelId;
	}


	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}


	public Date getShowTime() {
		return showTime;
	}


	public void setShowTime(Date showTime) {
		this.showTime = showTime;
	}


	public Date getCreateTime() {
		return createTime;
	}


	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}


	public String getMasterMdn() {
		return masterMdn;
	}


	public void setMasterMdn(String masterMdn) {
		this.masterMdn = masterMdn;
	}


	public String getShowName() {
		return showName;
	}


	public void setShowName(String showName) {
		this.showName = showName;
	}

	
	
	
	
}
