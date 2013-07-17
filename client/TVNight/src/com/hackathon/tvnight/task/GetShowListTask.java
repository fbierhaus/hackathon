package com.hackathon.tvnight.task;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.hackathon.tvnight.api.GetShowList;
import com.hackathon.tvnight.model.TVShow;

public class GetShowListTask extends AsyncTask<Void, Void, Void> {
	private Handler mHandler;
	private int mMsgCode;
	private List<TVShow> mShowList;
	
	/**
	 * Specify the handler and code to notify the show list has been retrieved.
	 * Call getShowList() to get the result.
	 * 
	 * @param handler
	 * @param msgCode
	 */
	public GetShowListTask(Handler handler, int msgCode) {
		super();

		mHandler = handler;
		mMsgCode = msgCode;
	}
	
	public List<TVShow> getShowList() {
		return mShowList;
	}
	
	public void cancelOperation() {
		if (getStatus() != Status.FINISHED) {
			cancel(false);
		}
	}
	
	@Override
	protected void onPreExecute() {
		// here we do the API thing
	}

	@Override
	protected Void doInBackground(Void... params) {
//		for (int i=0; i<10; i++) {
//			TVShow show = new TVShow("Show " + (i+1));
//			mShowList.add(show);
//		}
		GetShowList task = new GetShowList();
		mShowList = task.getList();
		return null;
	}

	@Override
	protected void onPostExecute(Void param) {
		// end of task, notify the handler 
		Message msg = mHandler.obtainMessage(mMsgCode);
		msg.sendToTarget();
	}
	
}
