/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rocketmobile.asimov;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpPostSender;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.telephony.TelephonyManager;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.AppConstants;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.messaging.vzmsgs.provider.dao.SyncItemDao;
import com.verizon.messaging.vzmsgs.provider.dao.VMAEventHandler;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsDebug;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.drm.DrmUtils;
import com.verizon.mms.layout.LayoutManager;
import com.verizon.mms.transaction.MessagingNotification;
import com.verizon.mms.transaction.TransactionService;
import com.verizon.mms.ui.ConversationResHelper;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.ui.MessagingPreferenceActivity;
import com.verizon.mms.ui.VZActivityHelper;
import com.verizon.mms.util.EmojiParser;
import com.verizon.mms.util.BitmapManager;
import com.verizon.mms.util.DownloadManager;
import com.verizon.mms.util.DraftCache;
import com.verizon.mms.util.FileMonitorThread;
import com.verizon.mms.util.RateController;
import com.verizon.mms.util.RecyclerScheduleReceiver;
import com.verizon.mms.util.SaveGetOrCreateThreadIDFailure;
import com.verizon.mms.util.SmileyParser;
import com.verizon.mms.util.SpamReportInitialization;
import com.verizon.report.ReportService;
import com.verizon.sync.SyncManager;
import com.verizon.sync.UiNotificationHandler;
import com.verizon.vzmsgs.saverestore.BackupManagerImpl;
import com.verizon.vzmsgs.saverestore.PopUpUtil;
import com.vzw.vma.sync.refactor.PDUDao;
import com.vzw.vma.sync.refactor.impl.MapperDaoImpl;
import com.vzw.vma.sync.refactor.impl.PDUDaoImpl;
import com.vzw.vma.sync.refactor.impl.SyncItemDaoImpl;
import com.vzw.vma.sync.refactor.impl.VMAEventHandlerImpl;

//@ReportsCrashes(formKey = "dHk4LUZaS3hDSHdQNGNqaGNTVHc0eXc6MQ", socketTimeout=30000)
//@ReportsCrashes(formKey = "dFZCdHpEX2NCWG1yYlFVV1VWWHVYaXc6MQ", socketTimeout=30000)
@ReportsCrashes(formKey = "", customReportContent = { org.acra.ReportField.APP_VERSION_NAME,
		org.acra.ReportField.APP_VERSION_CODE, 
		org.acra.ReportField.PACKAGE_NAME, 
		org.acra.ReportField.USER_CRASH_DATE, 
		org.acra.ReportField.BUILD, 
		org.acra.ReportField.DISPLAY,
		org.acra.ReportField.AVAILABLE_MEM_SIZE, 
		org.acra.ReportField.USER_APP_START_DATE, 
		org.acra.ReportField.BRAND, 
		org.acra.ReportField.TOTAL_MEM_SIZE,
		org.acra.ReportField.REPORT_ID, 
		org.acra.ReportField.PHONE_MODEL, 
		org.acra.ReportField.DEVICE_ID, 
		org.acra.ReportField.SHARED_PREFERENCES, 
		org.acra.ReportField.IS_SILENT,
		org.acra.ReportField.ANDROID_VERSION,
		org.acra.ReportField.LOGCAT,
		org.acra.ReportField.STACK_TRACE }, 
		logcatArguments = { "-t", "300" },
		formUri = "https://vzm.pdi.vzw.com/acra/acra/")
 

public class Asimov extends Application {
	
	private final String PROGRESS_KEY = "backupInProgress"; 
	public final String APP_VERSION_CODE = "appVersionCode";
	private SearchRecentSuggestions mRecentSuggestions;
	private TelephonyManager mTelephonyManager;
	private static Asimov sMmsApp = null;
	private Handler handler;
	private UncaughtExceptionHandler defaultUncaughtHandler;
	private boolean isMainProcess;
	private boolean isMediaSyncProcess;
	private boolean isWifiSyncProcess;
	private SharedPreferences prefs;
	private static int visibleActivityCount = 0;
   
	private static float mFontScale = 1.0f;
	private FileMonitorThread fileMonitorThread;
    
	@Override
	public void onCreate() {
		super.onCreate();
		// init process info
		initProcessInfo();
		// Initializing 
		
		addOrUpdateVersionCode();
		
		if (Logger.IS_INFO_ENABLED) {
			Logger.info(Asimov.class, "Process Info ==> main=" + isMainProcess
					+ ", medidaSync=" + isMediaSyncProcess + ", wifi="
					+ isWifiSyncProcess);
		}
		//starting contentobserver to listen setting changes.
		if (isMainProcess()) {
			mFontScale = android.provider.Settings.System.getFloat(getContentResolver(),
					android.provider.Settings.System.FONT_SCALE ,(float)1.0);
		}
		// Init ACRA the first thing! 
		if (Logger.IS_ACRA_ENABLED) {
			Logger.debug(Asimov.class, "#===>>>> ACRA is instantiated!");
			ACRA.init(this);
			String formUri = null;
			if (Logger.IS_DEBUG_ENABLED) {
				formUri = getResources().getString(R.string.acra_dev);
			} else {
				formUri = getResources().getString(R.string.acra_prod);
			}
			Logger.debug(Asimov.class, "#===>>> ACRA formUri=" + formUri);
			ACRA.getErrorReporter().setReportSender(new HttpPostSender(formUri, null));
			
			// Add an identifier
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			if (!prefs.contains("pref_key_acraInstall")) {
				String id = Acra.id(this);
				prefs.edit().putString("pref_key_acraInstall", id).commit();
			}
		}

		if (Logger.IS_STRICT_MODE_ENABLED && Build.VERSION.SDK_INT >= 9) {
			try {
				MmsDebug.enableStrictMode();
			} catch (Exception e) {
				Logger.error(e);
			}
		}

		// Dup shortcuts were getting created. So remove this.
		//createShortcut();
		
		handler = new Handler();
		sMmsApp = this;
		defaultUncaughtHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(uncaughtHandler);
		// Loading logger
		// need to move this logger init to application settings
		Logger.init(this);
		// Application settings
		ApplicationSettings as = ApplicationSettings.getInstance(this);

		if (isMainProcess()) {
			VZActivityHelper.init(this);
			// Load the default preference values
			PreferenceManager.setDefaultValues(this, R.xml.preferences, false); // Main
			MessagingPreferenceActivity.setLocale(getBaseContext()); // Main
			ConversationResHelper.init(this); // Main
			MessageUtils.init(this); // Main
		}
		
		MmsConfig.init(this, R.xml.device_config, R.xml.mms_config, R.bool.isTablet); // XXX perf xml parse
		VZUris.init(MmsConfig.isTabletDevice());
		
			MapperDaoImpl mapperDao = new MapperDaoImpl(this);
			SyncItemDao syncItemDao = new SyncItemDaoImpl(this);
			UiNotificationHandler uiNotification= new UiNotificationHandler(this);
			PDUDao pdu = new PDUDaoImpl(this , uiNotification);
			
			VMAEventHandler vmaEventHandler = new VMAEventHandlerImpl(mapperDao, syncItemDao, pdu);
	        as.setVMAEventHandler(vmaEventHandler);
	        as.setSyncItemDao(syncItemDao);
	        as.setPduDao(pdu);
	        as.setUiNotificationHandler(uiNotification);
		
		if (isMainProcess()) {
			Contact.init(this); // Main
			DraftCache.init(this); // Main
			Conversation.init(this);  // Main
			DownloadManager.init(this); // Main
			RateController.init(this); // Main
			LayoutManager.init(this); // Main
			SmileyParser.init(this); // Main
			MessagingNotification.init(this); // Main 
			SaveGetOrCreateThreadIDFailure.getInstance(getApplicationContext()).restoreFailedMessages(); // Main
            if(MmsConfig.enableEmojis) {
				EmojiParser.init(this); 
			} else {
				SmileyParser.init(this); // Main
			}
		}		
		// only call Spam Init from main process not both
		if (!MmsConfig.isTabletDevice() && isMainProcess()) {
			SpamReportInitialization.initialize(this);
		}

		// defer non-critical inits
		if (isMainProcess()) {
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					// cleanup storage in a separate thread
					new Thread(new Runnable() {
						@Override
						public void run() {
						    Looper.prepare();
							if (!MmsConfig.isTabletDevice()) {
								// Start MMS transaction service - in case there are
								// any MMS's pending to be sent
								sMmsApp.startService(new Intent(sMmsApp,
										TransactionService.class));
								prefs = PreferenceManager.getDefaultSharedPreferences(Asimov.this);
								
								RecyclerScheduleReceiver.scheduleOnLaunchAlarm(Asimov.this);
								
								int saveOrRestoreState = prefs.getInt(PROGRESS_KEY, 0);
								if (saveOrRestoreState != 0) {
									PopUpUtil util = new PopUpUtil(Asimov.this, false);
									util.clearSyncNotification();
									if (saveOrRestoreState == 1) {
										util.showNotification(BackupManagerImpl.BSYNC_SAVE_CANCELLED, 0, 0);	
									} else {
									   util.showNotification(BackupManagerImpl.BSYNC_RESTORE_CANCELLED, 0, 0);	
									}
									
									Editor edit = prefs.edit();
									edit.putInt(PROGRESS_KEY, 0);
										edit.commit();	
								}
							} else {
								if (Logger.IS_WARNING_ENABLED) {
									Logger.warn(getClass(),
											"Device is tablet : Ignoring the sending process. ");
								}
							}
	
							DrmUtils.cleanupStorage(sMmsApp); // Delayed start
							if (Logger.IS_DEBUG_ENABLED) {
								if (FileMonitorThread.ENABLE_FILE_MONITOR) {
									fileMonitorThread = new FileMonitorThread("/data/anr/traces.txt", 
											Thread.MIN_PRIORITY, Asimov.this);
									fileMonitorThread.start();
								}
							}
							Looper.loop();
						}
					}).start();
				}
			}, 10000);
			
			// schedule report service after 10 mins
			if (MmsConfig.isLTE()) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MINUTE, 10);
				ReportService.schedule(getApplicationContext(), cal);
			}
		}
	}

	private void addOrUpdateVersionCode() {
		PackageInfo info;
		try {
			info = getPackageManager().getPackageInfo(getPackageName(),0);
			int presentVersionCode = info.versionCode;
			//get version code from shared preference if any
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Asimov.this);
			int storedVersionCode = prefs.getInt(APP_VERSION_CODE, 0);
			
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(), " addOrUpdateVersionCode previous version code " + storedVersionCode
						+ " current version code " + presentVersionCode);
			}
			
			if(storedVersionCode != presentVersionCode){
				Editor edit = prefs.edit();
				edit.putInt(APP_VERSION_CODE, presentVersionCode);
			    edit.commit();
			}
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void removeShortcut(String name, boolean specifyPkg) { 
		if (Logger.IS_INFO_ENABLED) {
			Logger.info(Asimov.class, "removeShortcut - starting " + name);
		}		
		Intent shortcut = new Intent("com.android.launcher.action.UNINSTALL_SHORTCUT");

		// Shortcut name
		shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);  
		shortcut.putExtra("duplicate", true);  // delete all with old name

		// Setup current activity shoud be shortcut object 
		ComponentName comp = new ComponentName("com.verizon.messaging.vzmsgs", "com.verizon.mms.ui.activity.Provisioning");  
		Intent launchIntent = new Intent(Intent.ACTION_MAIN).setComponent(comp);
		if (specifyPkg) {
			launchIntent.setPackage("com.verizon.messaging.vzmsgs");
		}
		launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);  

		sendBroadcast(shortcut); 
		if (Logger.IS_INFO_ENABLED) {
			Logger.info(Asimov.class, "removeShortcut - end " + name);
		}
	}
	
	/**
	 * Creates the application shortcut if this is the first time we launched the app
	 */
	private void removeVZMShortcuts(){ 
		removeShortcut("VZMessages", true);
		removeShortcut("VZMessages", false);
		removeShortcut(getString(R.string.app_name), false);
	}
	
	/**
	 * Creates the application shortcut if this is the first time we launched the app
	 */
	private void createShortcut() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean appLaunched = prefs.getBoolean("app_launched", false);

		if (Logger.IS_INFO_ENABLED) {
			Logger.info(Asimov.class, "createShortcut - outside");
		}
		
		if (!appLaunched) {
			removeVZMShortcuts();
			
			if (Logger.IS_INFO_ENABLED) {
				Logger.info(Asimov.class, "createShortcut - creating");
			}
			Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");

			// Shortcut name
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));  
			shortcut.putExtra("duplicate", false);  // Just create once

			// Setup current activity shoud be shortcut object 
			ComponentName comp = new ComponentName("com.verizon.messaging.vzmsgs", "com.verizon.mms.ui.activity.Provisioning");  
			Intent launchIntent = new Intent(Intent.ACTION_MAIN).setComponent(comp);
			launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);  

			// Set shortcut icon
			ShortcutIconResource iconRes = Intent.ShortcutIconResource.fromContext(this, R.drawable.launcher_home_icon);  
			shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);  

			sendBroadcast(shortcut); 

			if (Logger.IS_INFO_ENABLED) {
				Logger.info(Asimov.class, "createShortcut - created");
			}

			SharedPreferences.Editor edit = prefs.edit();
			edit.putBoolean("app_launched", true);
			edit.commit();
		}
	}

	private UncaughtExceptionHandler uncaughtHandler = new UncaughtExceptionHandler() {
		@Override
		public void uncaughtException(Thread thread, Throwable t) {
			thread.setUncaughtExceptionHandler(null); // to avoid loops if we
														// fault
			Thread.setDefaultUncaughtExceptionHandler(null);
			// ACRA will itself handle uncaught exception reporting. Let's not do it twice. 
			Logger.error(false, "Uncaught exception in " + thread, t);
			String trace = Logger.postThreadStacksToLog();
			
			if (defaultUncaughtHandler != null) {
				defaultUncaughtHandler.uncaughtException(thread, t);
			} else {
				try {
					int pid = Process.myPid();
					Process.killProcess(pid);
				} catch (Throwable t2) {
				}
			}
		}
	};

	synchronized public static Asimov getApplication() {
		return sMmsApp;
	}

	@Override
	public void onTerminate() {
		DrmUtils.cleanupStorage(this);
		
//		if (fileMonitorThread != null) {
//			fileMonitorThread.stopMonitor();
//		}
		if (isMainProcess()) {
			BitmapManager.INSTANCE.close();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (isMainProcess()) {
			float fontScale = newConfig.fontScale;

			if (fontScale != mFontScale) {
				mFontScale = fontScale;
				MessageUtils.resetTheme();
			}

			MessagingPreferenceActivity.setLocale(getBaseContext());
			LayoutManager.getInstance().onConfigurationChanged(newConfig);
			BitmapManager.INSTANCE.onConfigurationChanged(newConfig);
		}
	}

	/**
	 * @return Returns the TelephonyManager.
	 */
	public TelephonyManager getTelephonyManager() {
		if (mTelephonyManager == null) {
			mTelephonyManager = (TelephonyManager) getApplicationContext()
					.getSystemService(Context.TELEPHONY_SERVICE);
		}
		return mTelephonyManager;
	}

	/**
	 * Returns the content provider wrapper that allows access to recent
	 * searches.
	 * 
	 * @return Returns the content provider wrapper that allows access to recent
	 *         searches.
	 */
	public SearchRecentSuggestions getRecentSuggestions() {
		/*
		 * if (mRecentSuggestions == null) { mRecentSuggestions = new
		 * SearchRecentSuggestions(this, SuggestionsProvider.AUTHORITY,
		 * SuggestionsProvider.MODE); }
		 */
		return mRecentSuggestions;
	}
	
	private void initProcessInfo() {
		ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		for (RunningAppProcessInfo p : am.getRunningAppProcesses()){
			// Is it me?
			if (p.pid != Process.myPid()) {
				continue;
			}
			
			if (Logger.IS_INFO_ENABLED) {
				Logger.info(Asimov.class, "Process name=" + p.processName + ", pid=" + p.pid);
			}
			
			if (p.processName.equalsIgnoreCase(AppConstants.MAIN_PROCESS_NAME)) {
				isMainProcess = true;
			}
			else if (p.processName.equalsIgnoreCase(AppConstants.MEDIA_SYNC_PROCESS_NAME)) {
				isMediaSyncProcess = true;
			}
		}
	}
	
	public boolean isMainProcess() {
		return isMainProcess;
	}

	public boolean isMediaSyncProcess() {
		return isMediaSyncProcess;
	}

	public boolean isWifiSyncProcess() {
		return isWifiSyncProcess;
	}
	
	public static boolean isActivityVisible() {
	    return visibleActivityCount > 0;
	}  

	public static void activityStarted() {
		Intent syncFailedIntent = new Intent(SyncManager.ACTION_SYNC_STATUS);
        syncFailedIntent.putExtra(SyncManager.EXTRA_STATUS, SyncManager.FINISH_POP_UP);
        sMmsApp.sendBroadcast(syncFailedIntent);
//		ApplicationSettings.getInstance().activityStarted();
		visibleActivityCount++;
		
		Asimov app = Asimov.sMmsApp;
//		if (app != null && app.fileMonitorThread != null) {
//			app.fileMonitorThread.resumeMonitor();
//		}
 	}

	public static void activityStoped() {
//	    ApplicationSettings.getInstance().activityStopped();
		visibleActivityCount--;
	}
	
	private static class Acra {
	    private static String sID = null;
	    private static final String NAME = "ACRA";

	    public synchronized static String id(Context context) {
	        if (sID == null) {  
	            File installation = new File(context.getFilesDir(), NAME);
	            try {
	                if (!installation.exists()) {
	                    writeInstallationFile(installation);
	                }
	                sID = readInstallationFile(installation);
	            } catch (Exception e) {
	                throw new RuntimeException(e);
	            }
	        }
	        return sID;
	    }

	    private static String readInstallationFile(File installation) throws IOException {
	        RandomAccessFile f = new RandomAccessFile(installation, "r");
	        byte[] bytes = new byte[(int) f.length()];
	        f.readFully(bytes);
	        f.close();
	        return new String(bytes);
	    }

	    private static void writeInstallationFile(File installation) throws IOException {
	        FileOutputStream out = new FileOutputStream(installation);
	        String id = UUID.randomUUID().toString();
	        out.write(id.getBytes());
	        out.close();
	    }
	}

}
