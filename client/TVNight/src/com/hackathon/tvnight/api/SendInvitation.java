package com.hackathon.tvnight.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.hackathon.tvnight.task.Invitation;
import com.hackathon.tvnight.util.JSONHelper;
import com.vzw.hackathon.GroupEvent;
import com.vzw.hackathon.Member;
import com.vzw.hackathon.apihandler.RestClient2;

public class SendInvitation {
	private Invitation mInvite;
	
	public SendInvitation(Invitation invite) {
		mInvite = invite;
	}
	
	public long send() {
		long invitationId = 0;
		
		try {
			// create the member list
			ArrayList<Member> members = new ArrayList<Member>();
			List<String> list = mInvite.getRecipientList();
			for (String mdn : list) {
				Member member = new Member();
				member.setMdn(mdn);
//				member.setName("Name " + mdn);
				members.add(member);
			}
			
			GroupEvent event = new GroupEvent();
//			event.setId((int)(System.currentTimeMillis()/1000L));
			event.setChannelId(mInvite.getChannelNumber() + "##" + mInvite.getRoviId());
			event.setShowId(mInvite.getShowId());
			event.setMemberList(members);
			event.setMasterMdn(mInvite.getSender());		// sender
			event.setShowTime(mInvite.getStartTime());
			
//			String json = JSONHelper.toJson(event);
//			json = "groupEvent={ " + json + " }";
			
			invitationId = RestClient2.createGroupEvent(event, ApiConstant.VERIZON_SERVER);
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return invitationId;
	}
}
