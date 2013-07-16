package com.verizon.vzmsgs.saverestore;

import android.os.AsyncTask;

public class SDCardStatusContainer {
	private AsyncTask  mRunningTask = null;
	private BackupManagerImpl mBackManager = null;
	
	public SDCardStatusContainer(AsyncTask runningTask, BackupManagerImpl backManager){
		this.mBackManager = backManager;
		this.mRunningTask = runningTask;
	}

	public AsyncTask<Void, Integer, Void> getRunningTask() {
		return mRunningTask;
	}

	public BackupManagerImpl getBackManager() {
		return mBackManager;
	}
	

}
