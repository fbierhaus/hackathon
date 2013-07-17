package com.vzw.hackathon;

import java.util.Date;
import java.util.List;

import com.vzw.util.ThreadSafeSimpleDateFormat;

public class GroupEvent {
	
	private static final ThreadSafeSimpleDateFormat sdf = new ThreadSafeSimpleDateFormat("yyyy-MM-dd HH:mm");
	
	private int id = -1;
	private String showId = null;
	private String channelId = null;
	private Date showTime = null;
	private String showName = null;
	private Date createTime = null;
	private String masterMdn = null;
	
	private List<Member> memberList = null;
	

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
	
	public void setShowTime(String showTimeStr) throws Exception {
		this.showTime = sdf.parse(showTimeStr);
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



	public List<Member> getMemberList() {
		return memberList;
	}



	public void setMemberList(List<Member> memberList) {
		this.memberList = memberList;
	}
	
	
	
	public String[] getMemberMdns() {
		String[] ret = null;
		
		if (memberList != null) {
			ret = new String[memberList.size()];
			int i = 0;
			for (Member m : memberList) {
				ret[i] = m.getMdn();
				++ i;
			}
		}
		
		return ret;
	}

	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("id:").append(getId()).append(",showId:").append(getShowId()).append(",channelId:").append(getChannelId()).append(",showTime:")
		  .append(getShowTime()).append(",showName:").append(getShowName()).append(",createTime:").append(getCreateTime()).append(",masterMdn:").append(getMasterMdn());
		
		return sb.toString();
	}
}
