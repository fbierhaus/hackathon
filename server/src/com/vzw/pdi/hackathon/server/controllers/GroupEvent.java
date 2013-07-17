package com.vzw.pdi.hackathon.server.controllers;

import net.sf.json.JSONObject;
import net.sf.serfj.RestController;
import net.sf.serfj.annotations.DoNotRenderPage;
import net.sf.serfj.annotations.GET;
import net.sf.serfj.annotations.POST;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vzw.hackathon.GroupEventManager;
import com.vzw.hackathon.Member;
import com.vzw.pdi.hackathon.server.ServerResponse;
import com.vzw.util.JSONUtil;

public class GroupEvent extends RestController {
	Logger logger = LoggerFactory.getLogger(GroupEvent.class);
	
	/**
	 * Request should look like:
	 * curl -i --data "groupEvent={'showId':'1','masterMdn':'9255551234', 'memberList':[{'mdn':'9258881234','name':'foo'},{'mdn':'9259991234','name':'bar'}]}" http://localhost:8080/server/groupEvents
	 * @return
	 */
	@POST
	@DoNotRenderPage
	public ServerResponse create(){
		logger.debug("++++++++++ Starting create");
		
		try{
			String jsonString = this.getStringParam("groupEvent");
			
			logger.debug("++++++++++ jsonString: " + jsonString);
			
			JSONObject jsonObject = JSONObject.fromObject( jsonString );  
			logger.debug("+++++++++ JSONObject" + jsonObject.toString());
			
//			com.vzw.hackathon.GroupEvent ge = (com.vzw.hackathon.GroupEvent) JSONObject.toBean( jsonObject, com.vzw.hackathon.GroupEvent.class );
			com.vzw.hackathon.GroupEvent ge = (com.vzw.hackathon.GroupEvent) JSONUtil.toJava(jsonObject, com.vzw.hackathon.GroupEvent.class, "memberList", Member.class);
			logger.debug("+++++++++++ deserialzed GroupEvent:" + ge);
			
			
			// save to db both groupevent and group_member
			GroupEventManager gem = new GroupEventManager();
			gem.createGroupEvent(ge);
			
			// call schedule GroupEventManager.schedulePlay(ge)
			gem.schedulePlay(ge.getMasterMdn(),ge);
			
		} catch (Exception e){
			logger.error("Error parsing JSON", e);
			return ServerResponse.ERROR;
		}

		
		return ServerResponse.OK;
	}
	
	
	// accept
	// need groupEventId to lookup in db, mdn
	// update db to set status to accepted/delcined (mdn, groupeventid, status)
	// GEM.schedulePlay(ge, mdn)
	
	@GET
	public com.vzw.hackathon.GroupEvent show() {
		com.vzw.hackathon.GroupEvent ge = new com.vzw.hackathon.GroupEvent();
		ge.setId(1);
		
		return ge;
	}
}
