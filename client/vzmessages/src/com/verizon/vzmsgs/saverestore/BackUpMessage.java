package com.verizon.vzmsgs.saverestore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since  Sep 5, 2012
 */
public class BackUpMessage implements Serializable {
	
	private static final long   serialVersionUID    = -3103056551554521995L;
	private ArrayList<HashMap<String, String>> addressData;
	private ArrayList<HashMap<String, String>> partsData;
	private HashMap<String, String> pduData;
	private String recipients;
	private boolean isSms;
	private int msg_index = -1;
	private int totalCount = 0;
	
	public ArrayList<HashMap<String, String>> getAddressData() {
		return addressData;
	}
	public void setAddressData(ArrayList<HashMap<String, String>> addressData) {
		this.addressData = addressData;
	}
	public ArrayList<HashMap<String, String>> getPartsData() {
		return partsData;
	}
	public void setPartsData(ArrayList<HashMap<String, String>> partsData) {
		this.partsData = partsData;
	}
	public HashMap<String, String> getPduData() {
		return pduData;
	}
	
	public void setPduData(HashMap<String, String> pduData) {
		this.pduData = pduData;
	}
	
	
    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }
    
    public boolean isSms() {
		return isSms;
	}
    
	public void setSms(boolean isSms) {
		this.isSms = isSms;
	}
	
	public int getMsgIndex() {
		return msg_index;
	}

	public void setMsgIndex(int msgIndex) {
		this.msg_index = msgIndex;
	}
	
	public int getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}
}
