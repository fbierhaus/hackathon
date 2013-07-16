/**
 * SaveRestoreMessage.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.data;

import java.io.Serializable;

/**
 * This class/interface   
 * @author Imthiaz
 * @Since  May 30, 2012
 */
public class SaveRestoreMessage implements MessageFields, Serializable {

    private static final long   serialVersionUID    = -3103056551554521995L;

    private String address;
    private String body;
    private int locked;
    private long date;
    private int read;
    private int type;
    private int status;
    
    //MMS
    private String mmsBody = "";
    private String attachmentData;
    private String contentType;
    private String mmsText;
    
    private boolean isSms;
    
    
    public boolean isSms() {
		return isSms;
	}
	public void setSms(boolean isSms) {
		this.isSms = isSms;
	}
	
	public String getMmsBody() {
		return (mmsBody == "") ? "" : mmsBody;
	}
	public void setMmsBody(String mmsBody) {
		this.mmsBody = mmsBody;
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
     * Set the Value of the body
     * @return the body
     */
    public String getBody() {
        return body;
    }
    /**
     * Return the Value of the fielbody
     *
     * @param body the body to set
     */
    public void setBody(String body) {
        this.body = body;
    }
    /**
     * Set the Value of the locked
     * @return the locked
     */
    public int getLocked() {
        return locked;
    }
    /**
     * Return the Value of the fiellocked
     *
     * @param locked the locked to set
     */
    public void setLocked(String locked) {
        this.locked =  Integer.parseInt(locked);
    }
    /**
     * Set the Value of the date
     * @return the date
     */
    public long getDate() {
        return date;
    }
    /**
     * Return the Value of the fieldate
     *
     * @param date the date to set
     */
    public void setDate(String date) {
        this.date = Long.parseLong(date);
    }
    /**
     * Set the Value of the read
     * @return the read
     */
    public int getRead() {
        return read;
    }
    /**
     * Return the Value of the fielread
     *
     * @param read the read to set
     */
    public void setRead(String read) {
        this.read = Integer.parseInt(read);
    }
    /**
     * Set the Value of the type
     * @return the type
     */
    public int getType() {
        return type;
    }
    /**
     * Return the Value of the fieltype
     *
     * @param type the type to set
     */
    public void setType(int type) {
        this.type =  type;
    }
    /**
     * Set the Value of the status
     * @return the status
     */
    public int getStatus() {
        return status;
    }
    /**
     * Return the Value of the fielstatus
     *
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status =  Integer.parseInt(status);
    }
    
    
   
}
