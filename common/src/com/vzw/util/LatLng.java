/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;


/**
 *
 * @author hud
 */
public class LatLng {
	public static final double LAT_MIN = -90;
	public static final double LAT_MAX = 90;
	public static final double LNG_MIN = -180;
	public static final double LNG_MAX = 180;
	
	private double			lat = -200;
	private double			lng = -200;
	
	public LatLng() {
		
	}
	
	public LatLng(double lat, double lng) {
		this.lat = lat;
		this.lng = lng;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getLng() {
		return lng;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}
	
	
	public boolean isValid() {
		return lat >= LAT_MIN && lat < LAT_MAX &&
					lng >= LNG_MIN && lng < LNG_MAX;
	}
}
