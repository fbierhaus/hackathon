package com.hackathon.tvnight.api;

//import java.util.ArrayList;
//import java.util.List;

import com.hackathon.tvnight.task.Invitation;
import com.hackathon.tvnight.util.JSONHelper;
//import com.vzw.hackathon.GroupEvent;
//import com.vzw.hackathon.Member;

public class SendInvitation {
	private Invitation mInvite;
	
	public SendInvitation(Invitation invite) {
		mInvite = invite;
	}
	
	public long send() {
		try {
//			// create the member list
//			ArrayList<Member> members = new ArrayList<Member>();
//			List<String> list = mInvite.getRecipientList();
//			for (String mdn : list) {
//				Member member = new Member();
//				member.setMdn(mdn);
//				member.setName("Name " + mdn);
//				members.add(member);
//			}
//			
//			GroupEvent event = new GroupEvent();
//			event.setChannelId(mInvite.getChannelId());
//			event.setShowId(mInvite.getShowId());
//			event.setMemberList(members);
//			
//			String json = JSONHelper.toJson(event);
//			json = "groupEvent={ " + json + " }";
//			
//
//			event.setMasterMdn()
			
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return System.currentTimeMillis();
	}
}
