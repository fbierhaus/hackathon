package com.hackathon.tvnight.task;

import com.hackathon.tvnight.api.ReplyInvitation;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

/**
 * Result is returned in the Message sent to the Handler supplied by the caller.
 * Message.arg1 = 1 if success, 0 if failed
 */
public class ReplyInvitationTask extends AsyncTask<Void, Void, Boolean> {
	private Handler mHandler;
	private int mMsgCode;
	private int mInvitationId;
	private boolean mAccept;
	
	public ReplyInvitationTask(Handler handler, int msgCode, int invitationId, boolean accept) {
		mHandler = handler;
		mMsgCode = msgCode;
		mInvitationId = invitationId;
		mAccept = accept;
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
	protected Boolean doInBackground(Void... params) {
		boolean success = (new ReplyInvitation(mInvitationId).send(mAccept));					
		return Boolean.valueOf(success);
	}

	@Override
	protected void onPostExecute(Boolean param) {
		// end of task, notify the handler 
		Message msg = mHandler.obtainMessage(mMsgCode);
		msg.arg1 = param.booleanValue()? 1 : 0;
		msg.sendToTarget();
	}
}
