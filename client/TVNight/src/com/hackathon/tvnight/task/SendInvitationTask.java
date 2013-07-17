package com.hackathon.tvnight.task;

import com.hackathon.tvnight.api.SendInvitation;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

/**
 * Result is returned in the Message sent to the Handler supplied by the caller.
 * Message.obj contains the unique invitation id in Long, or Long of 0 if the invitation failed to send.
 */
public class SendInvitationTask extends AsyncTask<Invitation, Void, Long> {
	private Handler mHandler;
	private int mMsgCode;	
	
	private long mInvitationId = 0;
	
	public SendInvitationTask(Handler handler, int msgCode) {
		mHandler = handler;
		mMsgCode = msgCode;
	}
	
	public long getInvitationId() {
		return mInvitationId;
	}
	
	public void cancelOperation() {
		if (getStatus() != Status.FINISHED) {
			cancel(false);
		}
	}

	@Override
	protected void onPreExecute() {
		//nothing
	}

	@Override
	protected Long doInBackground(Invitation... params) {
		Long invitationId = null;
		Invitation invite = params[0];

		long id = (new SendInvitation(invite).send());
		invitationId = Long.valueOf(id);					
		return invitationId;
	}

	@Override
	protected void onPostExecute(Long param) {
		if (param == null) {
			mInvitationId = 0;
		}
		else {
			mInvitationId = param.intValue();
		}

		// end of task, notify the handler 
		Message msg = mHandler.obtainMessage(mMsgCode);
		msg.obj = Long.valueOf(mInvitationId);
		msg.sendToTarget();
	}
}
