package com.hackathon.tvnight.model;

import java.util.ArrayList;

public class ImageListResult {
	private String status;
	private int code;
//	private String messages;
//	private String build;
//	private ImageListView view;
	private ArrayList<ShowImage> images;
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;		
	}
	
	public ArrayList<ShowImage> getImages() {
		return images;		
	}
	
	public void setImages(ArrayList<ShowImage> images) {
		this.images = images;
	}
}
