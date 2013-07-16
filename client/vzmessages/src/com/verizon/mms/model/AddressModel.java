/**
 * AddressModel.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.model;

/**
 * This class/interface   
 * @author Imthiaz
 * @Since  Apr 12, 2012
 */
public class AddressModel {
    
    
    private int     location_Id;
    private String  addressText;
    private long    date;
    private String placeName;
    private boolean favoriteState;
    private double latitude;
    private double longitude;
    private String addContext;

   
    public AddressModel (int _id, String _txt, long cTime, boolean favState, double lat, double lon, String addContext, String placeName){
    	latitude = lat;
    	longitude = lon;
        this.location_Id = _id;
        this.addressText = _txt;
        this.date = cTime;
        this.favoriteState = favState;
        this.addContext = addContext;
        this.placeName = placeName;
    }
    
    public AddressModel (String _txt, long cTime, boolean favState, double lat, double lon, String addContext, String placeName) {
        latitude = lat;
        longitude = lon;
        this.addressText = _txt;
        this.date = cTime;
        this.favoriteState = favState;
        this.addContext = addContext;
        this.placeName = placeName;
    }
    
    
    public boolean isFavoriteState() {
        return favoriteState;
    }

    /**
     * Return the Value of the fielfavoriteState
     * 
     * @param favoriteState
     *            the favoriteState to set
     */
    public void setFavoriteState(boolean favoriteState) {
        this.favoriteState = favoriteState;
    }

    /**
     * Set the Value of the addressText
     * 
     * @return the addressText
     */
    public String getAddressText() {
        return addressText;
    }

    /**
     * Return the Value of the fieladdressText
     * 
     * @param addressText
     *            the addressText to set
     */
    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    /**
     * Set the Value of the currentTime
     * 
     * @return the currentTime
     */
    public long getCurrentTime() {
        return date;
    }

    /**
     * Return the Value of the fielcurrentTime
     * 
     * @param currentTime
     *            the currentTime to set
     */
    public void setCurrentTime(long currentTime) {
        this.date = currentTime;
    }

    /**
     * Set the Value of the location_Id
     * @return the location_Id
     */
    public int getLocation_Id() {
        return location_Id;
    }

    /**
     * Return the Value of the fiellocation_Id
     *
     * @param location_Id the location_Id to set
     */
    public void setLocation_Id(int location_Id) {
        this.location_Id = location_Id;
    }

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
    
    public String getAddressContext() {
    	return this.addContext;
    }

	public String getPlaceName() {
		return placeName;
	}

	public void setPlaceName(String placeName) {
		this.placeName = placeName;
	}

	public String getAddContext() {
		return addContext;
	}

	public void setAddContext(String addContext) {
		this.addContext = addContext;
	}
	
}
