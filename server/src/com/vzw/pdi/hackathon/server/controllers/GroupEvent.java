package com.vzw.pdi.hackathon.server.controllers;

import net.sf.serfj.RestController;
import net.sf.serfj.annotations.GET;

public class GroupEvent extends RestController {
	@GET
	public com.vzw.hackathon.GroupEvent show() {
		com.vzw.hackathon.GroupEvent ge = new com.vzw.hackathon.GroupEvent();
		ge.setId(1);
		
		return ge;
	}
}
