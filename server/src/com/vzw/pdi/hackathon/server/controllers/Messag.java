/**
 * 
 */
package com.vzw.pdi.hackathon.server.controllers;

import net.sf.serfj.RestController;
import net.sf.serfj.annotations.DoNotRenderPage;
import net.sf.serfj.annotations.POST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vzw.pdi.hackathon.server.MessageManager;

/**
 * @author fred
 *
 */
public class Messag extends RestController {

	Logger logger = LoggerFactory.getLogger(GroupEvent.class);
	
	@POST
	@DoNotRenderPage
	public void create(){
		logger.debug("Starting create");
		
		String mdn = this.getStringParam("mdn");
		String message = this.getStringParam("message");
		String from = this.getStringParam("message");
		
		MessageManager mm = new MessageManager();
		mm.postMessage(from, mdn, message);
	}
}
