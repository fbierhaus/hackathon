package com.hackathon.tvnight.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TVShow {
	
	private boolean simulatePaid;
	private String imageUrl;
	
	public final static String ID_KEY_MERLIN = "merlin";
	public final static String ID_KEY_REX = "rex";
	public final static String ID_KEY_ROVI = "rovi";

	// ========================================================
	// helper functions
	
	/**
	 * Get the "default" description from the description list.
	 */
	public String getDefaultDescription() {
		return getDefaultText(getDescription());
	}
	
	/**
	 * Get the "default" title from the title list.
	 * 
	 * @return
	 */
	public String getDefaultTitle() {
		return getDefaultText(getTitle());
	}
	
	/**
	 * Get the default text entry in List<TextEntry>
	 * If not found, it uses the id of this TVShow
	 * 
	 * @param list
	 * @return
	 */
	private String getDefaultText(List<TextEntry> list) {
		String text = null;
		if (list != null) {
			if (list.size() > 0) {
				TextEntry entry = list.get(0);
				text = entry.getDefault();
			}

			if (text == null) {
				// if nothing found, use id
				text = getArbitraryId();
			}
		}
		return text;
	}
		
	public String getId(String key) {
		for (Map<String, String> entry : id) 
		{
			if (entry.containsKey(key)) {
				return entry.get(key);
			}
        }
		return null;
	}

	/**
	 * Get one of the defined id from the order of key preference.
	 */
	public String getArbitraryId() {
		final String[] keys = {
			ID_KEY_MERLIN,
			ID_KEY_REX,
			ID_KEY_ROVI
		};
		
		for (String key : keys) {
			String id = getId(key);
			if (id != null) {
				return id;				
			}
		}
		
		return null;	
	}

	/**
	 * Get a list of show result of this show which includes
	 * time, price, channel id and etc
	 * 
	 * @return
	 */
	public List<ShowingResult> getShowingResultList() {
		// get a list of shows if detail exists
		if (subresults != null && subresults.getShowings() != null) {
			return subresults.getShowings().getShowingresults();
		}
		return null;
	}
	
	//========================================================
	// beans
	
	// these will be in SearchResult
	private List<TextEntry> description;
	private String entitytype;
	private ArrayList<Map<String,String>> id;
	private String language;
	private ArrayList<ShowRating> rating;
	private String ref;
	private int startyear;
	private List<TextEntry> title;
	
	// these will be in Entity detail
	private SubResults subresults;
	
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
	
	public SubResults getSubresults() {
		return subresults;
	}
	
	public void setSubresults(SubResults subresults) {
		this.subresults = subresults;
	}
	
	public void setSimulatePaid(boolean simulatePaid) {
		this.simulatePaid = simulatePaid;
	}
	
	public boolean getSimulatePaid() {
		return simulatePaid;
	}
	
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	public String getImageUrl() {
		return imageUrl;
	}
	
}
