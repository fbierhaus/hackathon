package com.hackathon.tvnight.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hackathon.tvnight.model.SearchResult;
import com.hackathon.tvnight.model.ShowEntityList;
import com.hackathon.tvnight.model.ShowImage;
import com.hackathon.tvnight.model.TVShow;
import com.hackathon.tvnight.util.JSONHelper;

/**
 */
public class GetShowImage {

	public GetShowImage() {		
	}
	
	public List<ShowImage> getImageList(String id) {
		ArrayList<ShowImage> list = null;
		
		try {
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	private String sendRequest(HttpURLConnection conn) throws IOException {
		StringBuilder builder = new StringBuilder();			
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		
		return builder.toString();		
	}
}
