package com.verizon.vzmsgs.saverestore;


import java.util.HashMap;

public abstract class SDCardStatus {
	public static int SD_CARD_EJECTED = 1001;
	public static int SD_CARD_UNMOUNTED_CLOSE_ACTIVITY = 1002;
	public static String SD_CARD_STATUS = "SD_CARD_STATUS";
	public abstract void status(String status) ;
	private static HashMap<String, SDCardStatusContainer> runningTask  = new HashMap<String, SDCardStatusContainer>();
	
	public static void  addRunningTask(SDCardStatusContainer runningProcess, String key) {
		synchronized (runningTask) {
			runningTask.put(key, runningProcess);
		}
		
	}
	
	public static void removeRunningTask(String taskName) {
		synchronized (runningTask) {
			runningTask.remove(taskName);
		}
	}
	public static HashMap<String, SDCardStatusContainer> getRunningTasks() {
		return runningTask;
	}
}
