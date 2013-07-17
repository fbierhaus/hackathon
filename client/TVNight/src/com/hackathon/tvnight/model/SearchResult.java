package com.hackathon.tvnight.model;

import java.io.Serializable;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class SearchResult extends ResultList { // implements Parcelable, Serializable {
	private List<TVShow> entities;	
	
	public List<TVShow> getEntities() {
		return this.entities;
	}
	
	public void setEntities(List<TVShow> list) {
		this.entities = list;
	}
	
}
