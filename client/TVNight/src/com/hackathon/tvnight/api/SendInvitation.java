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
				member.setName("Name " + mdn);
				members.add(member);
			}
			
			GroupEvent event = new GroupEvent();
			event.setId((int)(System.currentTimeMillis()/1000L));
			event.setChannelId(mInvite.getChannelId());
			event.setShowId(mInvite.getShowId());
			event.setMemberList(members);
			event.setMasterMdn(mInvite.getSender());
			
			String json = JSONHelper.toJson(event);
			json = "groupEvent={ " + json + " }";
			
			String query = ApiConstant.VERIZON_SERVER + ApiConstant.SEND_INVITE;
			
			URL url;
			try {
//				url = new URL(query);
//				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//				conn.setRequestMethod("POST");
//
//				StringBuilder builder = new StringBuilder();			
//				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//				String line = null;
//				while ((line = reader.readLine()) != null) {
//					builder.append(line);
//				}
//
//				String response = builder.toString();

				Thread.sleep(5000);
				
				invitationId = event.getId();
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return invitationId;
	}
}
