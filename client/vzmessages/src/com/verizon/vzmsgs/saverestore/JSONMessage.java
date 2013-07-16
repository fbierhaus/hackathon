/**
 * JSONMessage.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.vzmsgs.saverestore;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

/**
 * This class
 * 
 * @author Jegadeesan.M
 * @Since Apr 20, 2012
 */
public class JSONMessage {

    
    public static final String JKEY_PDU = "pdu";
    public static final String JKEY_ADR = "addr";
    public static final String JKEY_PARTS = "parts";
   
    // Sms and MMS PDU values
    private JSONObject pdu;
    // address of SMS/MMS - group
    private List<JSONObject> address;

    private List<JSONObject> parts;

    private String recipients;
   
    
   /**
	 * 
	 */
	public JSONMessage() {
		// TODO Auto-generated constructor stub
	}


/**
     * Returns the Value of the pdu
     * 
     * @return the {@link JSONObject}
     */
    public JSONObject getPdu() {
        if (pdu == null) {
            pdu = new JSONObject();
        }
        return pdu;
    }

    /**
     * Set the Value of the field pdu
     * 
     * @param pdu
     *            the pdu to set
     */
    public void setPdu(JSONObject pdu) {
        this.pdu = pdu;
    }

    /**
     * Returns the Value of the parts
     * 
     * @return the {@link List<JSONObject>}
     */
    public List<JSONObject> getParts() {
        if (parts == null) {
            parts = new ArrayList<JSONObject>();
        }
        return parts;
    }

    /**
     * Set the Value of the field parts
     * 
     * @param parts
     *            the parts to set
     */
    public void setParts(List<JSONObject> parts) {
        this.parts = parts;
    }

    /**
     * Returns the Value of the address
     * 
     * @return the {@link List<JSONObject>}
     */
    public List<JSONObject> getAddresses() {
        if (address == null) {
            address = new ArrayList<JSONObject>();
        }
        return address;
    }

    /**
     * Set the Value of the field address
     * 
     * @param address
     *            the address to set
     */
    public void setAddresses(List<JSONObject> addresses) {
        this.address = addresses;
    }

   
   
    /**
	 * @param recipients
	 */
	public void setRecipients(String recipients) {
		this.recipients = recipients;
		
	}
	
	public String getRecipients() {
		return recipients;
	}

   

}
