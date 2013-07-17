/**
 * 
 */
package com.vzw.pdi.hackathon.server;

import com.vzw.hackathon.apihandler.ComcastAPIHandler;

/**
 * @author fred
 *
 */
public class MessageManager {

	public void postMessage(String from, String to, String message){
		// look up from name
		// TODO from Dongliang
		
		// call Jeff's API
		ComcastAPIHandler.postMessage(to, from + ":" + message);
	}
}
