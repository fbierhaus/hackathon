package com.hackathon.tvnight.model;

public class TVShow {

	private String name;
	private String imageUrl;
	private String episodeTitle;
	
	public TVShow(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
