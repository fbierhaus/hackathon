package com.verizon.sync;

public abstract class DatasetChangeListener {
	private long registeredThreadId;

	public DatasetChangeListener (long threadId) {
		registeredThreadId = threadId;
	}
	
	public abstract void onMessageAdded(long threadId, long msgId, int msgType, long msgSource);

	public abstract void onMesssageDeleted(long threadId, long msgId, int msgType, long msgSource);

	public abstract void onMesssageStatusChanged(long threadId, long msgId, int msgType, long msgSource);
	
	public long getRegisteredThreadId() {
		return registeredThreadId;
	}
}