package com.vzw.pdi.hackathon.server.controllers;

import net.sf.json.JSONObject;
import net.sf.serfj.RestController;
import net.sf.serfj.annotations.DoNotRenderPage;
import net.sf.serfj.annotations.GET;
import net.sf.serfj.annotations.POST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vzw.pdi.hackathon.server.ServerResponse;

public class GroupEvent extends RestController {
	Logger logger = LoggerFactory.getLogger(GroupEvent.class);
	
	@POST
	@DoNotRenderPage
	public ServerResponse create(){
		logger.debug("++++++++++ Starting create");
		
		try{
			String jsonString = this.getStringParam("groupEvent");
			
			logger.debug("++++++++++ jsonString: " + jsonString);
			
			JSONObject jsonObject = JSONObject.fromObject( jsonString );  
			logger.debug("+++++++++ JSONObject" + jsonObject.toString());
			
			com.vzw.hackathon.GroupEvent ge = (com.vzw.hackathon.GroupEvent) JSONObject.toBean( jsonObject, com.vzw.hackathon.GroupEvent.class ); 
			logger.debug("+++++++++++ deserialzed GroupEvent:" + ge);
			
		} catch (Exception e){
			logger.error("Error parsing JSON", e);
		}

		
		return ServerResponse.OK;
	}
	
	
	@GET
	public com.vzw.hackathon.GroupEvent show() {
		com.vzw.hackathon.GroupEvent ge = new com.vzw.hackathon.GroupEvent();
		ge.setId(1);
		
		return ge;
	}
}
