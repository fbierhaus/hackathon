package com.hackathon.tvnight.task;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.hackathon.tvnight.api.GetShowList;
import com.hackathon.tvnight.model.TVShow;

public class GetShowListTask extends AsyncTask<Integer, Void, List<TVShow>> {
	private Handler mHandler;
	private int mMsgCode;
	private String mKeyword;
//	private List<TVShow> mShowList = null;
	private int mIndex = 0;
	private String searchTerm;
	
	/**
	 * Specify the handler and code to notify the show list has been retrieved.
	 * Result returned in Message.obj as List<TVShow>
	 * 
	 * @param handler
	 * @param msgCode
	 * @param keyword	keyword to search or null for getting top watched
	 */
	public GetShowListTask(Handler handler, int msgCode, String keyword) {
		super();
		
		this.searchTerm = searchTerm;
		mHandler = handler;
		mMsgCode = msgCode;
		mKeyword = keyword; 
	}
	
//	/**
//	 * null if there is an error in request
//	 * Empty if there is no more entry matching the search criteria.
//	 *  
//	 * @return
//	 */
//	public List<TVShow> getShowList() {
//		return mShowList;
//	}
	
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
	protected List<TVShow> doInBackground(Integer... params) {
		// clear previous list		
		List<TVShow> showList = null;

		if (params.length > 0) {
			int limit = params[0].intValue();
			GetShowList task = new GetShowList();
			showList = task.getList(mKeyword, mIndex, limit);
			if (showList != null) {
				mIndex += showList.size();
			}
		}
		else {
			// create an empty list
			showList = new ArrayList<TVShow>();
		}
		return showList;
	}

	@Override
	protected void onPostExecute(List<TVShow> param) {
		// end of task, notify the handler 
		Message msg = mHandler.obtainMessage(mMsgCode);
		msg.obj = param;
		msg.sendToTarget();		
	}
	
}
