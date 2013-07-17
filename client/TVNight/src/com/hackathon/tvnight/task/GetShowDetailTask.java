package com.hackathon.tvnight.task;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.hackathon.tvnight.api.GetShowDetail;
import com.hackathon.tvnight.model.TVShow;

public class GetShowDetailTask extends AsyncTask<String, Void, TVShow> {
	private Handler mHandler;
	private int mMsgCode;
	
	/**
	 * Specify the handler and code to notify the show list has been retrieved.
	 * Result returned in Message.obj (TVShow object) or null if failed.
	 * 
	 * @param handler
	 * @param msgCode
	 */
	public GetShowDetailTask(Handler handler, int msgCode) {
		super();

		mHandler = handler;
		mMsgCode = msgCode;
	}
	
	public void cancelOperation() {
		if (getStatus() != Status.FINISHED) {
			cancel(false);
		}
	}
	
	@Override
	protected void onPreExecute() {
	}

	@Override
	protected TVShow doInBackground(String... params) {
		TVShow tvShow = null;
		if (params.length > 0) {
			tvShow = (new GetShowDetail()).getDetail(params[0]);
		}
		return tvShow;
	}

	@Override
	protected void onPostExecute(TVShow result) {
		// end of task, notify the handler 
		Message msg = mHandler.obtainMessage(mMsgCode);
		msg.obj = result;
		msg.sendToTarget();
	}
	
}
