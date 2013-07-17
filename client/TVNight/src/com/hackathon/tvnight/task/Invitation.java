package com.hackathon.tvnight.task;

import java.util.ArrayList;
import java.util.List;

public class Invitation {
	private ArrayList<String> mRecipientList = new ArrayList<String>();
	private String mChannelId;
	private String mShowId;

	/**
	 * @param channelId		Use the "rovi" in the id list
	 * @param showId		Use the "rex" in the id list
	 */
	public Invitation(String channelId, String showId) {
		mChannelId = channelId;
		mShowId = showId;
	}
	
	public void addRecipient(String recipient) {
		if (mRecipientList.contains(recipient) == false) {
			mRecipientList.add(recipient);
		}
	}
	
	public List<String> getRecipientList() {
		return mRecipientList;
	}
	
	public String getChannelId() {
		return mChannelId;
	}
	
	public String getShowId() {
		return mShowId;
	}
}
