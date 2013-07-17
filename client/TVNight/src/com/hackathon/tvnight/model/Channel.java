package com.hackathon.tvnight.model;

public class Channel {
	private String stationid;
	private String source;
	private String name;
	private String callletters;
	private String number;
	
	public String getStationid() {
		return stationid;
	}
	
	public void setStationid(String id) {
		this.stationid = id;
	}
	
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getCallletters() {
		return callletters;
	}
	
	public void setCallletters(String callletters) {
		this.callletters = callletters;
	}

	public String getNumber() {
		return number;
	}
	
	public void setNumber(String number) {
		this.number = number;
	}
}
