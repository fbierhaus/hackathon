package com.hackathon.tvnight.model;

import java.io.Serializable;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchResult { // implements Parcelable, Serializable {
	private int start;
	private int returned;
	private int count;
	private List<TVShow> entities;	
	
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
	
	public List<TVShow> getEntities() {
		return this.entities;
	}
	
	public void setEntities(List<TVShow> list) {
		this.entities = list;
	}
	
}
