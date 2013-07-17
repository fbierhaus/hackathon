package com.vzw.pdi.hackathon.server.controllers;

import java.io.IOException;

import net.sf.json.JSONObject;
import net.sf.serfj.RestController;
import net.sf.serfj.annotations.DoNotRenderPage;
import net.sf.serfj.annotations.GET;
import net.sf.serfj.annotations.POST;
import net.sf.serfj.annotations.PUT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vzw.hackathon.GroupEvent;
import com.vzw.hackathon.GroupEventManager;
import com.vzw.hackathon.Member;
import com.vzw.hackathon.MemberStatus;
import com.vzw.util.JSONUtil;

public class GroupEventController extends RestController {
	Logger logger = LoggerFactory.getLogger(GroupEventController.class);
	
	/**
	 * Request should look like:
	 * ## curl -i --data "groupEvent={'showId':'1','masterMdn':'9255551234', 'memberList':[{'mdn':'9258881234','name':'foo'},{'mdn':'9259991234','name':'bar'}]}" http://localhost:8080/server/groupEvents
	 * curl -i --data "groupEvent={'masterMdn':'9250000001','showId':'1234455555555','showTime':'1830303000000','channelId':'1','memberList': [{ 'mdn': '9250000002'},{ 'mdn': '9250000003'}]}" http://localhost:8080/server/groupEvents
	 * 
	 * {
			masterMdn:	'9250000001',
			showId:		'1234455555555',
			showTime:	1830303000000,
			memberList: [
				{ mdn: '9250000002'},
				{ mdn: '9250000003'}
			]
		}
	 * @return
	 * @throws IOException 
	 */
	@POST
	public void create() throws IOException{
		logger.debug("++++++++++ Starting create");
		
		try{
			String jsonString = this.getStringParam("groupEvent");
			
			logger.debug("++++++++++ jsonString: " + jsonString);
			
			JSONObject jsonObject = JSONObject.fromObject( jsonString );  
			logger.debug("+++++++++ JSONObject" + jsonObject.toString());
			
			GroupEvent ge = (GroupEvent) JSONUtil.toJava(jsonObject, GroupEvent.class, "memberList", Member.class);
			logger.debug("+++++++++++ deserialzed GroupEventController:" + ge);
			
			
			// save to db both groupevent and group_member
			GroupEventManager gem = GroupEventManager.getInstance();
			int id = gem.createGroupEvent(ge);
			logger.debug("Created GroupEventController with id: " + id);
			
			// set for jsp
			this.putParam("id", id);
		} catch (Exception e){
			logger.error("Error parsing JSON", e);
			this.renderPage("error.jsp");
		}
	}
	
	
	
	/**
	 * curl -X PUT -i  "http://localhost:8080/server/groupEvents/1/rsvp?status=ACCEPTED&mdn=9255551234"
	 */
	@PUT
	@DoNotRenderPage
	public void rsvp(){
		logger.debug("++++++++++ Starting RSVP");

		int id = Integer.parseInt(this.getId());
		String statusString = this.getStringParam("status");
		String mdn = this.getStringParam("mdn");
		MemberStatus status =  MemberStatus.valueOf(statusString);

		logger.debug("++++++++++ RSVP mdn:" + mdn + " status: " + status );
		
		
		GroupEventManager gem = GroupEventManager.getInstance();
		
		// update db to set status to accepted/delcined (mdn, groupeventid, status)
		gem.updateMemberStatus(id, mdn, status);
	}
	
	
	
	@GET
	public com.vzw.hackathon.GroupEvent show() {
		int id = Integer.parseInt(this.getId());
		
		GroupEventManager gem = GroupEventManager.getInstance();
		GroupEvent ge = gem.loadGroupEventFromDb(id);
		
		return ge;
	}
}
