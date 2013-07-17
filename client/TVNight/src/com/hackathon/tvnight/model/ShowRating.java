package com.hackathon.tvnight.model;

import java.util.ArrayList;
import java.util.Map;

public class ShowRating {
	private ArrayList<Map<String,String>> audience;
	
	public ArrayList<Map<String,String>> getAudience() {
		return audience;
	}
	
	public void setAudience(ArrayList<Map<String,String>> audience) {
		this.audience = audience;
	}
	
}
