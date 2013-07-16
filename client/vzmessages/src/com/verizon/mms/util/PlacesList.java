/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since  Sep 17, 2012
 */
package com.verizon.mms.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.nbi.common.NBIContext;
import com.nbi.common.NBIException;
import com.nbi.common.NBIRequest;
import com.nbi.map.data.Coordinates;
import com.nbi.map.data.POI;
import com.nbi.map.data.SearchRegion;
import com.nbi.search.SearchRequest;
import com.nbi.search.singlesearch.SingleSearchInformation;
import com.nbi.search.singlesearch.SingleSearchListener;
import com.nbi.search.singlesearch.SingleSearchRequest;
import com.nbi.search.singlesearch.SuggestionSearchInformation;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.model.ErrorInformation;
import com.verizon.mms.model.PlaceInfo;
import com.verizon.mms.ui.AddLocationActivity;

public class PlacesList implements SingleSearchListener {
	private NBIContext nbiContext;
	private ArrayList<PlaceInfo> currentList;
	private ArrayList<PlaceInfo> placesList = new ArrayList<PlaceInfo>();
	private ArrayList<PlaceInfo> couponsList = new ArrayList<PlaceInfo>();
	private PlaceInfo extraPinPlace;
	private String searchString = "";
	private Coordinates coords = new Coordinates(0, 0);
	private AddLocationActivity mContext;
	private int currentPlace;
	private List<NBIRequest> nbiReqs = Collections.synchronizedList(new ArrayList<NBIRequest>());
	
	private int placesListNumber;
	
	private ArrayAdapter<PlaceInfo> adapter;
	private Activity currentActivity;
	private boolean selectLastResult;
	private int searchType;
	
	private Hashtable<String, Object> backup = new Hashtable<String, Object>();

	
	private SuggestionSearchInformation singleSubSearchInfo;
	private int singleSubSearchIndex;

	public static final int COUPON_SEARCH = 3;
	public static final int STORE_SEARCH = 4;
	public static final int SINGLE_SEARCH = 5;
	public static final int SINGLE_SUB_SEARCH = 6;

	private static final String BACKUP_SEARCH_STRING = "search_string";
	private static final String BACKUP_PLACES_LIST_NUMBER = "places_list_number";
	private static final String BACKUP_COORDINATES = "coordinates";
	private static final String BACKUP_CURRENT_ACTIVITY = "current_activity";
	private static final String BACKUP_ADAPTER = "adapter";
	

	public PlacesList(AddLocationActivity context, NBIContext nbiContext) {
		this.mContext = context;
		this.nbiContext = nbiContext;
		currentActivity = mContext;
		setSearchType(SINGLE_SEARCH);
	}

	int getSearchType() {
		return searchType;
	}

	void setSearchType(int searchType) {
		this.searchType = searchType;
		chooseCurrentList();
	}

	//Fires off a search request
	public void doSearch(int type, String search, Coordinates coords, Activity activity) {
		// backup previous search state
		backupSearchState();
		// save these parameters
		this.searchString = search;
		this.coords = coords;
		this.currentActivity = activity;
		// this is the first search
		placesListNumber = 0;
		// there is no array adapter
		adapter = null;
		selectLastResult = false;

		NBIRequest request = null;
		if (type == SINGLE_SEARCH) {
			// create a single search request
			request = new SingleSearchRequest(nbiContext, searchString, new SearchRegion(this.coords), null, NBIRequest.SLICE_RESULTS_DEFAULT, SearchRequest.POI_ENHANCED, this);
		} else if (type == SINGLE_SUB_SEARCH) {
			// create a single sub search request
			request = new SingleSearchRequest(nbiContext, singleSubSearchInfo, singleSubSearchIndex, new SearchRegion(this.coords), NBIRequest.SLICE_RESULTS_DEFAULT, SearchRequest.POI_ENHANCED, this);
		}
		if (request != null) {
			nbiReqs.add(request);
			request.startRequest();
			mContext.displaySearchingDialog(request, searchString);
		}
	
	}

	public void doSingleSubSearch(String search, Coordinates coords, Activity activity, SuggestionSearchInformation info, int searchIndex ) {
		setSearchType(SINGLE_SUB_SEARCH);
		singleSubSearchInfo = info;
		singleSubSearchIndex = searchIndex;
		//isSuggestionMatchCategory = (info.getSuggestMatch(searchIndex).getMatchType() == SuggestMatch.NBI_MATCH_TYPE_CATEGORY);
		doSearch(SINGLE_SUB_SEARCH,search, coords, activity);
	}
	
	public void setAdapter(ArrayAdapter<PlaceInfo> adapter) {
		// set the array adapter so we will update the list with the results
		this.adapter = adapter;
	}
	
	public void destroy() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "destroy: nbiReqs = " + nbiReqs);
		}
		nbiContext = null;
		for (NBIRequest req : nbiReqs) {
			try {
				req.cancelRequest();
			}
			catch (Throwable t) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.error(getClass(), t);
				}
			}
		}
		nbiReqs.clear();
	}

	
	//Returns places list number
	int getPlacesListNumber() {
		return placesListNumber;
	}
	
	//Returns places list
	ArrayList<PlaceInfo> getList() {
		return currentList;
	}

	//Checks whether places list is filled 
	boolean hasResults() {
		return (currentList != null) && (currentList.size() != 0);
	}

	boolean hasCoupons() {
		return couponsList.size() > 0;
	}

	//Returns number of results
	int numResults() {
		return (currentList != null) ? currentList.size() : 0;
	}

	// Returns slice size. By default is 10 items.
	int numResultsRequested() {
		return NBIRequest.SLICE_RESULTS_DEFAULT;
	}

	// Clears all search results and removes all pins from map.
	void clearList(boolean clearPlacesList) {
		backup.clear();
		if (clearPlacesList) {
			placesList.clear();
			
		}
		couponsList.clear();
		//mContext.removeAllPinsFromMap();
	}

	

	//Returns place info by specified place index in list
	PlaceInfo getPlaceInfo(int index) {
		if(currentList == null || index >= currentList.size())
		{
			return null;
		}
		if (index != -1)
			return (currentList != null) ? currentList.get(index) : null;
		else
			return extraPinPlace;
	}

	//Returns formated place string by specified place index in list
	String getPlaceString(int index) {
		String formatString = mContext.getString(R.string.x_of_x);
		// index is zero based, so add 1
		if (searchType != SINGLE_SEARCH)
			return String.format(formatString, index + 1 + placesListNumber, currentList.size() + placesListNumber);
		else
			return String.format(formatString, index + 1, currentList.size());
	}

	//Returns formated search result string 
	String getResultsString() {
		String formatString = mContext.getString(R.string.results);
		if (searchType != SINGLE_SEARCH)
			return String.format(formatString, 1 + placesListNumber, currentList.size() + placesListNumber);
		else
			return String.format(formatString, 1, currentList.size());
	}

	//Returns search string
	String getSearchString() {
		return searchString;
	}

	//Checks whether it is still possible to select previous pin
	boolean canPreviousPin() {
		return (currentPlace > 0);
	}

	//Checks whether it is possible to load previous pin
	boolean canPreviousLoadPin() {
		boolean canLoad = false;

		switch (searchType) {
			case COUPON_SEARCH: {
				canLoad = (placesListNumber > 0);
				break;
			}

			case STORE_SEARCH: {
				canLoad = false;
				break;
			}

			default: {
				canLoad = (currentPlace > 0) && ((currentPlace % NBIRequest.SLICE_RESULTS_DEFAULT) == 0);
				break;
			}
		}

		return canLoad;
	}

	//Checks whether it is still possible to select next pin
	boolean canNextPin() {
		return (currentPlace > -1) && (currentPlace < currentList.size() - 1);
	}

	//Checks whether it is possible to load next pin
	boolean canNextLoadPin() {
		// can load if the last search was complete
		if (!canLoadMore())
			return false;

		boolean canLoad = false;

		switch (searchType) {
			case COUPON_SEARCH: {
				// can load the next set if we are not at 100
				canLoad = (placesListNumber < 90);
				break;
			}

			case STORE_SEARCH: {
				canLoad = false;
				break;
			}

			default: {
				canLoad = ((currentPlace + 1) % NBIRequest.SLICE_RESULTS_DEFAULT == 0);
				break;
			}
		}

		return canLoad;
	}

	boolean canLoadMore() {
		// was the last search result less than the default size?
		int listSize = currentList.size();
		int lastResultSize = listSize % NBIRequest.SLICE_RESULTS_DEFAULT;
		return !(listSize == 100 || lastResultSize > 0);
	}
	


	//Sets pointer to the current place index
	void setCurrentPlaceIndex(int index) {
		currentPlace = index;
		// make sure the pins are displayed on the map
		//putPinsOnMap();
	}

	/* Find the PlaceInfo object with the specified pin ID that matches the incoming parameter
	   and will return its position within the list */
	int setCurrentPlace(int pinID) {
		Iterator<PlaceInfo> iterator = currentList.iterator();
		currentPlace = 0;
		while (iterator.hasNext()) {
			PlaceInfo placeInfo = iterator.next();
			if (placeInfo.getPinID() == pinID)
				break;
			currentPlace++;
		}
		// if we didn't find the pinID in the list, then return -1
		if (currentPlace == currentList.size())
			currentPlace = -1;
		// return the current position
		return currentPlace;
	}

	
	public void restoreRecentSubSearchPlace(POI poi){
		clearList(placesListNumber == 0);
		// create our PlaceInfo object
		PlaceInfo placeInfo = new PlaceInfo(poi);
		// add it to the list
		if (adapter == null)
			placesList.add(placeInfo);
		else {
			adapter.add(placeInfo);
		}
		PlaceInfo placeData = new PlaceInfo(poi);
		final Coordinates coordinate = new Coordinates(placeData
				.getLatitude(), placeData.getLongitude());
        mContext.drawPin(placeData.getPlace(), coordinate);
		
	}
	
	////////////////////////////////////////////////////////////////////
	// generic NBIRequest callbacks
	@Override
	public void onRequestCancelled(NBIRequest request) {
		mContext.dismissProgressDialag();
		restoreSearchState();
		nbiReqs.remove(request);
	}

	@Override
	public void onRequestComplete(NBIRequest request) {
		/* Do not dismiss the searching dialog here - 
		   it is dismissed in the onLocalSearch method.
		   The reason for this is the onLocalSearch method is called first
		   and that method adds pins to the map view.
		   Common recommendation: it is better to add pins after the dialog is dismissed. */
		nbiReqs.remove(request);
	}

	@Override
	public void onRequestError(NBIException e, NBIRequest request) {
		mContext.dismissProgressDialag();
		restoreSearchState();
		//Show error
		String displayStr = ErrorInformation.getErrorMessage(e.getErrorCode());
		showToast(displayStr);
		nbiReqs.remove(request);
	}

	@Override
	public void onRequestProgress(int percentage, NBIRequest request) {
	}

	@Override
	public void onRequestStart(NBIRequest request) {
	}

	// Dismisses progress dialog and clear result list
	@Override
	public void onRequestTimeOut(NBIRequest request) {
		mContext.dismissProgressDialag();
		restoreSearchState();
		// Show error
		showToast("Request timed out");
		nbiReqs.remove(request);
	}

	

	

	////////////////////////////////////////////////////////////////////
	// SingleSearchRequest callback methods
	@Override
	public void onSingleSearch(SingleSearchInformation info, SingleSearchRequest request) {
		// dismiss the progress dialog
		mContext.dismissProgressDialag();
		// check for "no results" (only display this for the initial search)
		if (info.getResultCount() == 0) {
			mContext.displayNoResultsDialog(searchString);
			mContext.clearSearchField(false);
			restoreSearchState();
			return;
		}
		// clear the list
		// if this is not the initial search, then don't clear the places list
		clearList(placesListNumber == 0);
		// save the results in the array
		for (int index = 0; index < info.getResultCount(); index++) {
			// create our PlaceInfo object
			POI poi=info.getPOI(index);
			PlaceInfo placeInfo = new PlaceInfo(poi);
			// add it to the list
			if (adapter == null)
				placesList.add(placeInfo);
			else
				adapter.add(placeInfo);
			
			mContext.addPlaceToRecentItem(poi);
		}
		// calculate the new current place
		if (!selectLastResult)
			currentPlace = placesListNumber;
		else
			currentPlace = placesList.size() - 1;
		
		
		POI poi = info.getPOI(currentPlace);
		PlaceInfo placeData = new PlaceInfo(poi);
		
		final Coordinates coordinate = new Coordinates(placeData
				.getLatitude(), placeData.getLongitude());
        mContext.drawPin(placeData.getPlace(), coordinate);
		setSearchType(SINGLE_SEARCH);

		
	}
	
	private void chooseCurrentList() {
		switch (searchType) {
			case COUPON_SEARCH:
				currentList = couponsList;
				break;
			case STORE_SEARCH:
			case SINGLE_SEARCH:
			case SINGLE_SUB_SEARCH:
			default:
				currentList = placesList;
				break;
		}
	}

	private void backupSearchState() {
		backup.put(BACKUP_SEARCH_STRING, searchString);
		backup.put(BACKUP_PLACES_LIST_NUMBER, new Integer(placesListNumber));
		backup.put(BACKUP_COORDINATES, coords);
		backup.put(BACKUP_CURRENT_ACTIVITY, currentActivity);

		if (adapter != null) {
			backup.put(BACKUP_ADAPTER, adapter);
		}

		
	}

	@SuppressWarnings("unchecked")
	private void restoreSearchState() {
		searchString = (String)backup.get(BACKUP_SEARCH_STRING);
		placesListNumber = ((Integer)backup.get(BACKUP_PLACES_LIST_NUMBER)).intValue();
		coords = (Coordinates)backup.get(BACKUP_COORDINATES);
		currentActivity = (Activity)backup.get(BACKUP_CURRENT_ACTIVITY);
		adapter = (ArrayAdapter<PlaceInfo>)backup.get(BACKUP_ADAPTER);
		

		backup.clear();
	}

	private void showToast(String text) {
		Toast t = Toast.makeText(currentActivity.getApplicationContext(), text, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.BOTTOM, 0, 0);
		t.show();
	}
}