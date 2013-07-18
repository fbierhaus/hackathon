package com.hackathon.tvnight.api;

import com.hackathon.tvnight.util.Util;
import com.vzw.hackathon.MemberStatus;
import com.vzw.hackathon.apihandler.RestClient2;


public class ReplyInvitation {
	private int mInvitationId;
	private String mMdn;
	
	public ReplyInvitation(int invitationId, String mdn) {
		mInvitationId = invitationId;
		mMdn = mdn;
	}
	
	public boolean send(boolean accept) {
		RestClient2.rsvp(mInvitationId, mMdn,
				MemberStatus.ACCEPTED, ApiConstant.VERIZON_SERVER);

		return false;
	}
}
