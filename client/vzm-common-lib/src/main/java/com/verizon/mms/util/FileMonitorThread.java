package com.verizon.mms.util;
import java.io.BufferedInputStream;
import java.io.File;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.AppConstants;
import com.verizon.messaging.vzmsgs.ApplicationSettings;


public class FileMonitorThread extends Thread{
	private static final int MAX_PARSING_LENGTH = 50 * 1024;
	public static final boolean ENABLE_FILE_MONITOR = true;
	//public static final int RETRY_DELAY = 5000;
	public static final int START_DELAY = 120000;
			
	private String filePath;
	//private int 	retryDelay = RETRY_DELAY;
	private boolean stopMonitor = false;
	private Context context = null;
	private static final String KEY_LAST_MOD_TIME = "lastmodifiedtime";

	private Object lock = new Object();
	
	public FileMonitorThread(String path, int priority, Context context) {
		setName("FileMonitorThread");
		setPriority(priority);
		filePath = path;
		this.context = context;
	}
	
//	public void setRetryDelay(int retryDelay) {
//		this.retryDelay = retryDelay;
//	}
	
	public void run() {
		StringBuilder builder = null;
		int len = MAX_PARSING_LENGTH;

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "Started file monitoring");
		}
		//do {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

			try {
				Thread.sleep(START_DELAY);

				ApplicationSettings appSettings = ApplicationSettings.getInstance();
				
//				if (appSettings != null && appSettings.isApplicationInBackground()) {
//					if (Logger.IS_DEBUG_ENABLED) {
//						Logger.debug(getClass(), "Waiting for applciation to come to foreground");
//					}
//					synchronized (lock) {
//						lock.wait();
//					}
//				}
				
				File file = new File(filePath);
				if (file.exists()) {
					long modifiedTime = file.lastModified();
					long lastSentTime = prefs.getLong(KEY_LAST_MOD_TIME, 0);

					if (lastSentTime == 0 || lastSentTime != modifiedTime) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(getClass(), " found new traces file ");
						}
						if (file.length() < MAX_PARSING_LENGTH) {
							len = (int)file.length();
						} else {
							len = MAX_PARSING_LENGTH;
						}
						builder = new StringBuilder(len);
						BufferedInputStream is = null;
						Uri uri = Uri.fromFile(file);

						if (!stopMonitor) {
							final ContentResolver res = context.getContentResolver();
							is = new BufferedInputStream(res.openInputStream(uri), 2048);
							int totalRead = 0;
							final byte[] buf = new byte[2048];
							do {
								int readNext = MAX_PARSING_LENGTH - totalRead;
								if (readNext > 2048) {
									readNext = 2048;
								}
								final int read = is.read(buf);
								if (read < 0) {
									break;
								}
								totalRead += read;
								builder.append(new String(buf, 0, read));
							} while (totalRead < MAX_PARSING_LENGTH);

							if (builder.indexOf(AppConstants.MAIN_PROCESS_NAME) != -1) {
								Logger.postErrorToAcra(builder);
							}

							builder = null;
							Editor editor = prefs.edit();
							editor.putLong(KEY_LAST_MOD_TIME, modifiedTime);
							editor.commit();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
//		} while (!stopMonitor);
	}
	
//	public void stopMonitor() {
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(getClass(), " stopMonitor called");
//		}
//		stopMonitor = true;
//	}
	
//	public void resumeMonitor() {
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(getClass(), " Notifying the monitor ");
//		}
//		synchronized (lock) {
//			lock.notifyAll();
//		}
//	}
}
