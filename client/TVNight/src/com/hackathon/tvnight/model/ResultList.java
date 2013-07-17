package com.hackathon.tvnight.model;

public class ResultList {
	private int start;
	private int returned;
	private int count;

	public int getStart() {
		return start;		
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public int getReturned() {
		return returned;
	}
	
	public void setReturned(int returned) {
		this.returned = returned;
	}
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
}
