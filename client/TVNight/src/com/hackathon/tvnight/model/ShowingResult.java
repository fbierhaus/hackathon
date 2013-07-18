package com.hackathon.tvnight.model;

import java.util.Date;

import com.vzw.util.ThreadSafeSimpleDateFormat;

public class ShowingResult {
	///////////////////////////////////////////////
	// helper functions
	
	private static final ThreadSafeSimpleDateFormat sdf = new ThreadSafeSimpleDateFormat("MM/dd hh:mm aa");
	
	public static final String convertTime(long time) {
		return sdf.format(new Date(time));
	}
	
	public String getPriceType() {
		String type = null;
		if (price != null) {
			type = price.getType();
		}
		return type;
	}
	
	/**
	 * Get channel id
	 */
	public String getChannelId() {
		String id = null;
		if (channel != null) {
			id = channel.getNumber();
		}
		return id;
	}
	
	/**
	 * Get duration in seconds
	 * 
	 * @return
	 */
	public int getDurationSec() {
		int second = 0;
		if (duration != null) {
			String[] parts = duration.split(":");
			// support only up to 24 hours, not days
			for (int i=parts.length-1, p=1; i>=0; i--, p*=60) {
				int sec = Integer.parseInt(parts[i]);
				sec *= p;
				second += sec;
			}
		}
		return second;
	}
	
	public String getStarttimeString() {
		return getTimeString(starttime);
	}
	
	public String getEndtimeString() {
		return getTimeString(endtime);
	}
	
	public String getTimeString(long time) {
		String str = sdf.format(new Date(time));
		return str;
	}
	
	///////////////////////////////////////////////
	// beans

	private long endtime;
	private long starttime;
//	private ArrayList<Object> audio;
	private String catalog;
	private String seriesid;
	private String programid;
	private String duration;
	private boolean _new;
	private Price price;
	private String merlinprogramid;
//	private ShowRating rating;
	private Channel channel;
	private Video video;
	private boolean closecaptioned;
	
	public long getEndtime() {
		return endtime;
	}

	public void setEndtime(long endtime) {
		this.endtime = endtime;
	}
	
	public long getStarttime() {
		return starttime;
	}
	
	public void setStarttime(long starttime) {
		this.starttime = starttime;
	}
	
	public String getCatalog() {
		return catalog;
	}
	
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}
	
	public String getSeriesid() {
		return seriesid;
	}
	
	public void setSeriesid(String id) {
		this.seriesid = id;
	}
	
	public String getProgramid() {
		return programid;
	}
	
	public void setProgramid(String id) {
		this.programid = id;
	}

	public String getDuration() {
		return duration;
	}
	
	public void setDuration(String duration) {
		this.duration = duration;
	}

	public boolean getNew() {
		return _new;
	}
	
	public void setNew(boolean _new) {
		this._new = _new;
	}
	
	public Price getPrice() {
		return price;
	}
	
	public void setPrice(Price price) {
		this.price = price;
	}
	
	public String getMerlinprogramid() {
		return merlinprogramid;
	}
	
	public void setMerlinprogramid(String id) {
		this.merlinprogramid = id;
	}
	
//	public ShowRating getRating() {
//		return rating;
//	}
//	
//	public void setRating(ShowRating rating) {
//		this.rating = rating;
//	}
	
	public Channel getChannel() {
		return channel;
	}
	
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	
	public Video getVideo() {
		return video;
	}
	
	public void setVideo(Video video) {
		this.video = video;
	}
	
	public boolean getClosecaptioned() {
		return closecaptioned;
	}
	
	public void setClosecaptioned(boolean closecaptioned) {
		this.closecaptioned = closecaptioned;
	}
}
