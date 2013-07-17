package com.hackathon.tvnight.task;

import java.util.List;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.hackathon.tvnight.api.GetShowImage;
import com.hackathon.tvnight.model.ShowImage;

public class GetShowImageTask extends AsyncTask<String, Void, List<ShowImage>> {
	private Handler mHandler;
	private int mMsgCode;
	
	/**
	 * Specify the handler and code to notify the show list has been retrieved.
	 * Result returned in Message.obj (TVShow object) or null if failed.
	 * 
	 * @param handler
	 * @param msgCode
	 */
	public GetShowImageTask(Handler handler, int msgCode) {
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
	protected List<ShowImage> doInBackground(String... params) {
		List<ShowImage> list = null;
		if (params.length > 0) {
			list = (new GetShowImage()).getImageList(params[0]);	// params[0] is roci id
		}
		return list;
	}

	@Override
	protected void onPostExecute(List<ShowImage> result) {
		// end of task, notify the handler 
		Message msg = mHandler.obtainMessage(mMsgCode);
		msg.obj = result;
		msg.sendToTarget();
	}
	
}
