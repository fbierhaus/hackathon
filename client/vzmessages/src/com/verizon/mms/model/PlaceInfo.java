/**
 * PlaceInfo.java
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
 * @Since  Apr 22, 2012
 */
import android.content.Context;

import com.nbi.map.data.MapLocation;
import com.nbi.map.data.POI;
import com.nbi.map.data.Place;
import com.verizon.messaging.vzmsgs.R;

//Holds each result from a search query
public class PlaceInfo {
    private POI poi;
	private double       distance;
    private Place        place;
    private int          pinID;
    private final double METERSINMILE = 1609.344;

    public PlaceInfo(Place place, double distance) {
        this.place = place;
        this.distance = distance;
    }
    
    PlaceInfo(Place place) {
		this.place = place;
		// if there is no name for this Place, then create one
		if (place != null && (place.getName() == null || place.getName().length() == 0))
			place.setName(createName(place.getLocation()));
	}
    
    private String createName(MapLocation mapLocation) {
		String locationAddress=mapLocation.getAddress();
		String address = locationAddress +
                         ( (locationAddress.length() == 0)? "" : " " ) +
                         mapLocation.getStreet();
		// if the address is blank, then use areaName if it exists, else city, state instead
		if (address.length() == 0) {
			if (mapLocation.getAreaName().length() > 0)
				address = mapLocation.getAreaName();
			else
			{
				if(mapLocation.getCity().length() > 0)
				{
					address = mapLocation.getCity() + ", ";
				}
				address += mapLocation.getState();
			}
		}
		return address;
	}
    
    public PlaceInfo(POI poi) {
		this(poi.getPlace());
		this.poi = poi;
	}
   
    String getPOIDistance(Context context) {
		if (poi == null) {
			return "";
		}

		double distance = poi.getDistance();
		if (distance == 0) {
			return "";
		}
		/* The distance is stored in meters and convert to miles.
		 It is better to use the system defined values (English v Metric) */
		double miles = distance / METERSINMILE;
		String formatString = context.getString(R.string.miles);
		String mileString = String.format(formatString, miles);
		return mileString;
	}
    
    public PlaceInfo(MapLocation mapLocation) {
        // convert the MapLocation object into a Place object
        String locationAddress = mapLocation.getAddress();
        String address = locationAddress + ((locationAddress.length() == 0) ? "" : " ")
                + mapLocation.getStreet();
        // if the address is blank, then use areaName if it exists, else city, state instead
        if (address.length() == 0) {
            if (mapLocation.getAreaName().length() > 0)
                address = mapLocation.getAreaName();
            else {
                if (mapLocation.getCity().length() > 0) {
                    address = mapLocation.getCity() + ", ";
                }
                address += mapLocation.getState();
            }
        }
        this.place = new Place(address, mapLocation);
    }

    public void setPinID(int pinID) {
        this.pinID = pinID;
    }

    public int getPinID() {
        return pinID;
    }

    public Place getPlace() {
        return place;
    }

    public String getName() {
        if (place.getName() == null || place.getName().equals(getAddress()))
            return "Address";
        else
            return place.getName();
    }
    
    public String getPlaceName() {
    	return place.getName();
    }

    public String getAreaName() {
        return place.getLocation().getAreaName();
    }

    public String getAddress() {
        String address = place.getLocation().getAddress();
        return address + ((address.length() == 0) ? "" : " ") + place.getLocation().getStreet();
    }

    public String getCity() {
        return place.getLocation().getCity();
    }

    public String getState() {
        return place.getLocation().getState();
    }

    public String getZip() {
        return place.getLocation().getPostalCode();
    }

    public String getCountry() {
        return place.getLocation().getCountry();
    }

    public double getLatitude() {
        return place.getLocation().getLatitude();
    }

    public double getLongitude() {
        return place.getLocation().getLongitude();
    }

    public String getPhoneNumber() {
        String phoneNumber = null;
        if (place.getPhoneNumberCount() > 0)
            phoneNumber = place.getPhoneNumber(0).getArea() + place.getPhoneNumber(0).getNumber();
        return phoneNumber;
    }

    public String getDistance(Context context) {
        if (distance == 0)
            return "";
        /*
         * The distance is stored in meters and convert to miles. It is better to use the system defined
         * values (English v Metric)
         */
        double miles = distance / METERSINMILE;
        String formatString = context.getString(R.string.miles);
        String mileString = String.format(formatString, miles);
        return mileString;
    }

    // this returns the address in a single line format
    public String getOneLineAddress() {
        String ret = "";
        if (getAddress().length() > 0) {
            ret += getAddress() + ", ";
        }
        if (getCity().length() > 0) {
            ret += getCity() + ", ";
        }
        if (getState().length() > 0) {
            ret += getState();
        }
        return ret;
    }

    // this returns the city, state, and zip
    public String getCityStateZip() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCity());
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(getState()).append(", ").append(getZip());
        return sb.toString();
    }

    // this returns the city and state
    public String getCityState() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCity());
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(getState());
        return sb.toString();
    }

    public boolean hasPhoneNumber() {
        return (place != null && place.getPhoneNumberCount() > 0);
    }
}
