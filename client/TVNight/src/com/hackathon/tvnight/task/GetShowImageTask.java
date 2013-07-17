package com.hackathon.tvnight.task;

import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.hackathon.tvnight.api.GetShowImage;
import com.hackathon.tvnight.model.ShowImage;
import com.hackathon.tvnight.model.TVShow;

public class GetShowImageTask extends AsyncTask<String[], Void, Void> {
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
	protected Void doInBackground(String[]... params) {
		List<ShowImage> list = null;
		if (params.length > 0) {
			String[] idList = params[0];
			
			for (String id : idList) {
				list = (new GetShowImage()).getImageList(id);	// params[0] is roci id

				long starttime = System.currentTimeMillis();
				
				// end of task, notify the handler 
				Message msg = mHandler.obtainMessage(mMsgCode);
				Bundle data = new Bundle();
				data.putString(TVShow.ID_KEY_ROVI, id);
				msg.setData(data);
				msg.obj = list;
				msg.sendToTarget();
				
				long endtime = System.currentTimeMillis();
				long elapsed = endtime - starttime;
				
				if (elapsed < 500) {
					try {
						Thread.sleep(500-elapsed);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}				
			}
		}
		return null;
	}

}
