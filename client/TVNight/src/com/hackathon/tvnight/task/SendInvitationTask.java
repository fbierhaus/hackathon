package com.hackathon.tvnight.task;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

/**
 * Result is retured in the Message sent to the Handler supplied by the caller.
 * Message.obj1 contains the unique invitation id, or 0 if the invitation failed to send.
 * 
 * @author mcdull
 *
 */
public class SendInvitationTask extends AsyncTask<Invitation, Void, Integer> {
	private Handler mHandler;
	private int mMsgCode;	
	
	private int mInvitationId = 0;
	
	public SendInvitationTask(Handler handler, int msgCode) {
		mHandler = handler;
		mMsgCode = msgCode;
	}
	
	public int getInvitationId() {
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
	protected Integer doInBackground(Invitation... params) {
		Invitation invite = params[0];

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	protected void onPostExecute(Integer param) {
		if (param == null) {
			mInvitationId = 0;
		}
		else {
			mInvitationId = param.intValue();
		}

		// end of task, notify the handler 
		Message msg = mHandler.obtainMessage(mMsgCode);
		msg.arg1 = mInvitationId;
		msg.sendToTarget();
	}
}
