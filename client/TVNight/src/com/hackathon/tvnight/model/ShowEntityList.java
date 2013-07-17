package com.hackathon.tvnight.model;

import java.util.ArrayList;

public class ShowEntityList {
	private int returned;
	private ArrayList<TVShow> entities;
	
	public int getReturned() {
		return returned;
	}
	
	public void setReturned(int returned) {
		this.returned = returned;
	}
	
	public ArrayList<TVShow> getEntities() {
		return entities;
	}
	
	public void setEntities(ArrayList<TVShow> entities) {
		this.entities = entities;
	}
}
