package com.hackathon.tvnight.api;


public class ReplyInvitation {
	private int mInvitationId;
	
	public ReplyInvitation(int invitationId) {
		mInvitationId = invitationId;
	}
	
	public boolean send(boolean accept) {
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
//			event.setId((int)(System.currentTimeMillis()/1000L));
//			event.setChannelId(mInvite.getChannelId());
//			event.setShowId(mInvite.getShowId());
//			event.setMemberList(members);
//			event.setMasterMdn(mInvite.getSender());
//			
//			String json = JSONHelper.toJson(event);
//			json = "groupEvent={ " + json + " }";
//			
//			String query = ApiConstant.VERIZON_SERVER + ApiConstant.SEND_INVITE;
//			
//			URL url;
//			try {
////				url = new URL(query);
////				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
////				conn.setRequestMethod("POST");
////
////				StringBuilder builder = new StringBuilder();			
////				BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
////				String line = null;
////				while ((line = reader.readLine()) != null) {
////					builder.append(line);
////				}
////
////				String response = builder.toString();

			Thread.sleep(5000);
			return true;
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
}
