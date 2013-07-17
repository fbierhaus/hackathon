package com.hackathon.tvnight.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TVShow {	
	List<TextEntry> description;
	String entitytype;
	ArrayList<Map<String,String>> id;
	String language;
	ArrayList<ShowRating> rating;
	String ref;
	int startyear;
	List<TextEntry> title;
	
	public List<TextEntry> getDescription() {
		return description;
	}
	
	public void setDescription(List<TextEntry> description) {
		this.description = description;
	}
	
	public String getEntitytype() {
		return entitytype;
	}
	
	public void setEntitytype(String entitytype) {
		this.entitytype = entitytype;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public void setLanguage(String language) {
		this.language = language;
	}
	
	public String getRef() {
		return ref;
	}
	
	public void setRef(String ref) {
		this.ref = ref;
	}
	
	public List<TextEntry> getTitle() {
		return title;
	}
	
	public void setTtile(List<TextEntry> title) {
		this.title = title;
	}

	public int getStartyear() {
		return startyear;
	}
	
	public void setStartyear(int year) {
		this.startyear = year;
	}
	
	public ArrayList<Map<String,String>> getId() {
		return id;
	}
	
	public void setId(ArrayList<Map<String,String>> id) {
		this.id = id;
	}

	public ArrayList<ShowRating> getRating() {
		return rating;
	}
	
	public void setRating(ArrayList<ShowRating> rating) {
		this.rating = rating;
	}
}
