package com.vzw.hackathon;

public class Channel {
	// id = channelNumber##roviId
	private String				id = null;
	private String				name = null;
	private String				desc = null;
	
	// for  tuning only
	private String				channelNumber = null;
	private String				roviId = null;
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	
	
	
	public String getChannelNumber() {
		return channelNumber;
	}
	public void setChannelNumber(String channelNumber) {
		this.channelNumber = channelNumber;
	}
	public String getRoviId() {
		return roviId;
	}
	public void setRoviId(String roviId) {
		this.roviId = roviId;
	}
	
	
	public static Channel fromChannelId(String channelId) {
		Channel channel = new Channel();
		channel.setId(channelId);
		
		String[] sa = channelId.split("##");
		channel.setChannelNumber(sa[0]);
		channel.setRoviId(sa[1]);
		
		return channel;
	}
}
