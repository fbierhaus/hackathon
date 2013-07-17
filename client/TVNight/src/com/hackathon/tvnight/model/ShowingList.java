package com.hackathon.tvnight.model;

import java.util.ArrayList;

public class ShowingList extends ResultList {
	private ArrayList<ShowingResult> showingresults;
	
	public ArrayList<ShowingResult> getShowingresults() {
		return showingresults;
	}
	
	public void setShowresults(ArrayList<ShowingResult> showingresults) {
		this.showingresults = showingresults;
	}
}
