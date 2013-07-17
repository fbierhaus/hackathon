package com.hackathon.tvnight.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.hackathon.tvnight.model.ImageListResult;
import com.hackathon.tvnight.model.ShowImage;
import com.hackathon.tvnight.util.JSONHelper;
import com.hackathon.tvnight.util.Util;

/**
 */
public class GetShowImage {

	public GetShowImage() {		
	}
	
	public List<ShowImage> getImageList(String id) {
		ArrayList<ShowImage> list = null;
		
		try {
			String query = ApiConstant.ROVI_SERVER + String.format(ApiConstant.ROVI_IMAGE, id);
			query += "&imagesize=80-120x80-120";	// "&formatid=51"; // "&imagesize=100x100";
			query += "&apikey=" + ApiConstant.ROVI_KEY;
			String md5 = ApiConstant.ROVI_KEY + ApiConstant.ROVI_SECRET + (System.currentTimeMillis()/1000);
			md5 = Util.md5(md5);
			query += "&sig=" + md5;

			URL url = new URL(query);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			String response = sendRequest(conn);
			ImageListResult result = JSONHelper.fromJson(response, ImageListResult.class);
			if (result != null) {
				list = result.getImages();
			}
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
