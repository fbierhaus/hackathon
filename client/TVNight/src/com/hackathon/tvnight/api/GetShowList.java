package com.hackathon.tvnight.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.hackathon.tvnight.model.SearchResult;
import com.hackathon.tvnight.model.TVShow;
import com.hackathon.tvnight.util.JSONHelper;

/**
 */
public class GetShowList {

	public GetShowList() {		
	}
	
	public List<TVShow> getList(String searchTerm) {
		try {
			//		  URL url = new URL("http://www.vogella.com");
//			String query = ApiConstant.MASHERY_KEY + "&" + "q=\"movie\"";	// &filters=<filters>&<query parameter1>&<query parameter2>&<query parameterN>";
			
			if (searchTerm == null) {
				searchTerm = "zombie";
			}
			
			String query = ApiConstant.SERVER +
					"/CCcontent/search?search=" + searchTerm +
					"&" + "availability=%28%28tvlisting.location%3Amerlin%247444042768941558110%29OR%28vod.location%3Audb.persona%246099%29%29" +
					"&" + ApiConstant.MASHERY_KEY;				
			
			URL url = new URL(query);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();

			StringBuilder builder = new StringBuilder();			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			
			String response = builder.toString();			
			SearchResult result = JSONHelper.fromJson(response, SearchResult.class);
			List<TVShow> list = result.getEntities();		
			if (list == null) {
				list = new ArrayList<TVShow>();
			}
			return list;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
