package com.vzw.hackathon.apihandler;

public class ComcastDevice {

	private String deviceId;
	private String consumerKey;
	private String aggregateKey;
	
	public ComcastDevice(String deviceId, String consumerKey, String aggregateKey) {
		this.deviceId = deviceId;
		this.consumerKey = consumerKey;
		this.aggregateKey = aggregateKey;
	}
	
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getConsumerKey() {
		return consumerKey;
	}
	public void setConsumerKey(String consumerKey) {
		this.consumerKey = consumerKey;
	}
	public String getAggregateKey() {
		return aggregateKey;
	}
	public void setAggregateKey(String aggregateKey) {
		this.aggregateKey = aggregateKey;
	}
	
	
}
