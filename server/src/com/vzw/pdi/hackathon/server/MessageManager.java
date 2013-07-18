/**
 * 
 */
package com.vzw.pdi.hackathon.server;

import com.vzw.hackathon.ComcastOAuthApi;
import com.vzw.hackathon.GroupEventManager;
import com.vzw.hackathon.User;

/**
 * @author fred
 *
 */
public class MessageManager {

	public void postMessage(String from, String to, String message){
		// look up from name
		// TODO from Dongliang
		User user = GroupEventManager.getInstance().getUser(from);
		
		// call Jeff's API
//		ComcastAPIHandler.postMessage(to, user.getName() + ":" + message);
		ComcastOAuthApi.postMessage(user.getName() + ":" + message);
	}
}
