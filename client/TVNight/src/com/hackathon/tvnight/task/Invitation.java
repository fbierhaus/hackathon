package com.hackathon.tvnight.task;

import java.util.ArrayList;
import java.util.List;

public class Invitation {
	private String mSender;
	private ArrayList<String> mRecipientList = new ArrayList<String>();
	private String mChannelNumber;
	private String mRoviId;
	private String mShowId;
	private long mStartTime;
	
	// below only when after it's created
	private long mId;

	/**
	 * @param sender		Phone's MDN
	 * @param channelId		Use the "rovi" in the id list
	 * @param showId		Use the "rex" in the id list
	 */
	public Invitation(String sender, String channelNumber, String roviId, String showId, long startTime) {
		mSender = sender;
		mChannelNumber = channelNumber;
		mRoviId = roviId;
		mShowId = showId;
		mStartTime = startTime;
	}
	
	public void addRecipient(String recipient) {
		if (mRecipientList.contains(recipient) == false) {
			mRecipientList.add(recipient);
		}
	}
	
	public List<String> getRecipientList() {
		return mRecipientList;
	}
	
	public String getChannelNumber() {
		return mChannelNumber;
	}
	
	public String getRoviId() {
		return mRoviId;
	}
	
	public String getShowId() {
		return mShowId;
	}
	
	public String getSender() {
		return mSender;
	}
	
	public long getStartTime() {
		return mStartTime;
	}
}
