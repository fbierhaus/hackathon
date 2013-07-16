/**
 * 
 */
package com.vzw.pdi.hackathon.server;

/**
 * @author fred
 *
 */
public enum ServerResponse {

	OK("OK"),
	FAIL("FAIL"),
	ERROR("ERROR");
	
	private final String status;
	
	private ServerResponse(String status){
		this.status = status;
	}
	
	public String getStatus(){
		return status;
	}

}
