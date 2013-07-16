/**
 * RestoreConversationItem.java
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
 * @Since  Jun 12, 2012
 */
public class RestoreConversationItem {

    
    private long conversationID;
    private long mDate;
    private int mCount;
    private String snippet;
    private String address;
    private int type; //For Messages Restore 
    private int statusIndicator = NONE;
    
    private String mmsBody;
    
    private String attachmentData;
    private String contentType;
    private String mmsText;
 

	public static final int NONE  = 0;
    public static final int ERROR  = 1;
    public static final int PENDING  = 2;
   
    
    private boolean isSms;
    
    
    public boolean isSms() {
		return isSms;
	}
	public void setSms(boolean isSms) {
		this.isSms = isSms;
	}
	
	
	public String getAttachmentData() {
		return attachmentData;
	}
	
	public void setAttachmentData(String attachmentData) {
		this.attachmentData = attachmentData;
	}
	
	public String getMmsText() {
		return mmsText;
	}
	
	public void setMmsText(String mmsText) {
		this.mmsText = mmsText;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
    
    public String getMmsBody() {
		return mmsBody;
	}
	
    public void setMmsBody(String mmsBody) {
		this.mmsBody = mmsBody;
	}
    
   /**
     * Set the Value of the statusIndicator
     * @return the statusIndicator
     */
    public int getStatusIndicator() {
        return statusIndicator;
    }
    /**
     * Return the Value of the fielstatusIndicator
     *
     * @param statusIndicator the statusIndicator to set
     */
    public void setStatusIndicator(int statusIndicator) {
        this.statusIndicator = statusIndicator;
    }
    /**
     * Set the Value of the address
     * @return the address
     */
    public String getAddress() {
        return address;
    }
    /**
     * Return the Value of the fieladdress
     *
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }
    /**
     * Set the Value of the snippet
     * @return the snippet
     */
    public String getSnippet() {
        return snippet != null ? snippet : "";
    }
    /**
     * Return the Value of the fielsnippet
     *
     * @param snippet the snippet to set
     */
    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
    /**
     * Set the Value of the mCount
     * @return the mCount
     */
    public int getmCount() {
        return mCount;
    }
    /**
     * Return the Value of the fielmCount
     *
     * @param mCount the mCount to set
     */
    public void setmCount(int mCount) {
        this.mCount = mCount;
    }
    
    public long getDate() {
       
        return mDate;
    }
    
    public void setDate(long date) {
        mDate = date;
    }
    
 
    /**
     * Set the Value of the conversationID
     * @return the conversationID
     */
    public long getConversationID() {
        return conversationID;
    }
    /**
     * Return the Value of the fielconversationID
     *
     * @param conversationID the conversationID to set
     */
    public void setConversationID(long conversationID) {
        this.conversationID = conversationID;
    }
    /**
     * Set the Value of the smsType
     * @return the smsType
     */
    public int getType() {
        return type;
    }
    /**
     * Return the Value of the fielsmsType
     *
     * @param smsType the smsType to set
     */
    public void setType(int type) {
        this.type = type;
    }
}
