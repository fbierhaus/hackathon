package com.verizon.mms.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.strumsoft.android.commons.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * A subclass of Context to allow create database on external storage.
 * 
 * All methods calls the superclass's methods, except openOrCreateDDatabase().
 * openOrCreateDatabase() will call a local method getDatabaseFolder() to get the path to open or create database.
 * getDatabaseFolder() will check the Preferences' setting and return a path within the package or external storage.   
 * 
 * @author Samson W S Chan
 *
 */
public class DBContext extends ContextWrapper {
	public final static boolean USE_EXTERNAL = true;
	public final static String FOLDER_NAME = "VZMessages";		// folder name on external storage for cache
	
	private Context mContext;
	
	public DBContext(Context context) {
		super(context);
		this.mContext = context;
	}

	@Override
	public boolean bindService(Intent service, ServiceConnection conn, int flags) {
		return mContext.bindService(service, conn, flags);
	}

	@Override
	public int checkCallingOrSelfPermission(String permission) {
		return mContext.checkCallingOrSelfPermission(permission);
	}

	@Override
	public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
		return mContext.checkCallingOrSelfUriPermission(uri, modeFlags);
	}

	@Override
	public int checkCallingPermission(String permission) {
		return mContext.checkCallingPermission(permission);
	}

	@Override
	public int checkCallingUriPermission(Uri uri, int modeFlags) {
		return mContext.checkCallingUriPermission(uri, modeFlags);
	}

	@Override
	public int checkPermission(String permission, int pid, int uid) {
		return mContext.checkPermission(permission, pid, uid);
	}

	@Override
	public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
		return mContext.checkUriPermission(uri, pid, uid, modeFlags);
	}

	@Override
	public int checkUriPermission(Uri uri, String readPermission,
			String writePermission, int pid, int uid, int modeFlags) {
		return mContext.checkUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags);
	}

	@Override
	public void clearWallpaper() throws IOException {
		mContext.clearWallpaper();
	}

	@Override
	public Context createPackageContext(String packageName, int flags)
			throws NameNotFoundException {
		return mContext.createPackageContext(packageName, flags);
	}

	@Override
	public String[] databaseList() {		
		return mContext.databaseList();
	}

	@Override
	public boolean deleteDatabase(String name) {
		return mContext.deleteDatabase(name);
	}

	@Override
	public boolean deleteFile(String name) {
		return mContext.deleteFile(name);
	}

	@Override
	public void enforceCallingOrSelfPermission(String permission, String message) {
		mContext.enforceCallingOrSelfPermission(permission, message);
	}

	@Override
	public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {
		mContext.enforceCallingOrSelfUriPermission(uri, modeFlags, message);
	}

	@Override
	public void enforceCallingPermission(String permission, String message) {
		mContext.enforceCallingPermission(permission, message);
	}

	@Override
	public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {
		mContext.enforceCallingUriPermission(uri, modeFlags, message);
	}

	@Override
	public void enforcePermission(String permission, int pid, int uid,
			String message) {
		mContext.enforcePermission(permission, pid, uid, message);
	}

	@Override
	public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags,
			String message) {
		mContext.enforceUriPermission(uri,  pid, uid, modeFlags, message);
	}

	@Override
	public void enforceUriPermission(Uri uri, String readPermission,
			String writePermission, int pid, int uid, int modeFlags,
			String message) {
		mContext.enforceUriPermission(uri, readPermission, writePermission, pid, uid, modeFlags, message);
	}

	@Override
	public String[] fileList() {
		return mContext.fileList();
	}

	@Override
	public Context getApplicationContext() {
		return mContext.getApplicationContext();
	}

	@Override
	public ApplicationInfo getApplicationInfo() {
		return mContext.getApplicationInfo();
	}

	@Override
	public AssetManager getAssets() {
		return mContext.getAssets();
	}

	@Override
	public File getCacheDir() {
		return mContext.getCacheDir();
	}

	@Override
	public ClassLoader getClassLoader() {
		return mContext.getClassLoader();
	}

	@Override
	public ContentResolver getContentResolver() {
		return mContext.getContentResolver();
	}

	@Override
	public File getDatabasePath(String name) {		
		return mContext.getDatabasePath(name);
	}

	@Override
	public File getDir(String name, int mode) {
		return mContext.getDir(name, mode);
	}

	@Override
	public File getFileStreamPath(String name) {
		return mContext.getFileStreamPath(name);
	}

	@Override
	public File getFilesDir() {
		return mContext.getFilesDir();
	}

	@Override
	public Looper getMainLooper() {
		return mContext.getMainLooper();
	}

	@Override
	public PackageManager getPackageManager() {
		return mContext.getPackageManager();
	}

	@Override
	public String getPackageName() {
		return mContext.getPackageName();
	}

	@Override
	public Resources getResources() {
		return mContext.getResources();
	}

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return mContext.getSharedPreferences(name, mode);
	}

	@Override
	public Object getSystemService(String name) {
		return mContext.getSystemService(name);
	}

	@Override
	public Theme getTheme() {
		return mContext.getTheme();
	}

	@Override
	public Drawable getWallpaper() {
		return mContext.getWallpaper();
	}

	@Override
	public int getWallpaperDesiredMinimumHeight() {
		return mContext.getWallpaperDesiredMinimumHeight();
	}

	@Override
	public int getWallpaperDesiredMinimumWidth() {
		return mContext.getWallpaperDesiredMinimumWidth();
	}

	@Override
	public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {
		mContext.grantUriPermission(toPackage, uri, modeFlags);
	}

	@Override
	public FileInputStream openFileInput(String name)
			throws FileNotFoundException {
		return mContext.openFileInput(name);
	}

	@Override
	public FileOutputStream openFileOutput(String name, int mode)
			throws FileNotFoundException {
		return mContext.openFileOutput(name,  mode);
	}

	/**
	 * Get database folder based on the Preferences
	 * 
	 * @return path to the database or null using internal storage for application  
	 */
	private String getDatabaseFolder() {
//		String folder = pref.getAppConfig().db_folder;
		String dbPath = null;
		
		if (USE_EXTERNAL) {
			// no customized folder specified, get the default ext folder
//			File ext = Environment.getExternalStorageDirectory();
//			dbPath = ext.getAbsolutePath() + "/" + FOLDER_NAME;
			try {
				File ext = mContext.getExternalCacheDir();	// only API 8 or above
				dbPath = ext.getAbsolutePath();
			}
			catch (Exception e) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug("Failed to get external cache directory");
				}
				dbPath = null;
			}
		}
		else {
//			if (folder != null && folder.length() > 0) {
//				// use customized folder
//				dbPath = folder;
//			}						
		}
		return dbPath;
	}
	
	@Override
	public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
		SQLiteDatabase database = null;		
		String dbPath = getDatabaseFolder();

		if (dbPath != null) {
			// use external storage or customized folder 

			// create the path file
			File dir = new File(dbPath);
			if (dir.exists() == false) {
				if (dir.mkdir() == false) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug("Failed to create folder: " + dbPath);
					}
					dir = null;
				}
			}
			
			if (dir != null) {
				dbPath += "/" + name;
				// open database
				try {
					database = SQLiteDatabase.openDatabase(dbPath, factory, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY);
				}
				catch (Exception e) {
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug("Failed to open db " + dbPath + ":" + e.getMessage());
					}
					database = null;
				}
			}
		}
		
		if (database == null) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("Use internal storage for database");
			}
			// use internal storage or failed to use external storage
			try {
				database = mContext.openOrCreateDatabase(name, mode, factory);
			}
			catch (Exception e) {
				if (Logger.IS_WARNING_ENABLED) {
					Logger.warn("Failed to open internal db:", e);
				}
				database = null;
			}
		}
		return database;
	}

	@Override
	public Drawable peekWallpaper() {
		return mContext.peekWallpaper();
	}

	@Override
	public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
		return mContext.registerReceiver(receiver, filter);
	}

	@Override
	public Intent registerReceiver(BroadcastReceiver receiver,
			IntentFilter filter, String broadcastPermission, Handler scheduler) {
		return mContext.registerReceiver(receiver, filter, broadcastPermission, scheduler);
	}

	@Override
	public void removeStickyBroadcast(Intent intent) {
		mContext.removeStickyBroadcast(intent);
	}

	@Override
	public void revokeUriPermission(Uri uri, int modeFlags) {
		mContext.revokeUriPermission(uri, modeFlags);
	}

	@Override
	public void sendBroadcast(Intent intent) {
		mContext.sendBroadcast(intent);
	}

	@Override
	public void sendBroadcast(Intent intent, String receiverPermission) {
		mContext.sendBroadcast(intent, receiverPermission);
	}

	@Override
	public void sendOrderedBroadcast(Intent intent, String receiverPermission) {
		mContext.sendOrderedBroadcast(intent, receiverPermission);
	}

	@Override
	public void sendOrderedBroadcast(Intent intent, String receiverPermission,
			BroadcastReceiver resultReceiver, Handler scheduler,
			int initialCode, String initialData, Bundle initialExtras) {
		mContext.sendOrderedBroadcast(intent, receiverPermission, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	@Override
	public void sendStickyBroadcast(Intent intent) {
		mContext.sendStickyBroadcast(intent);
	}

	@Override
	public void sendStickyOrderedBroadcast(Intent intent,
			BroadcastReceiver resultReceiver, Handler scheduler,
			int initialCode, String initialData, Bundle initialExtras) {
		mContext.sendStickyOrderedBroadcast(intent, resultReceiver, scheduler, initialCode, initialData, initialExtras);
	}

	@Override
	public void setTheme(int resid) {
		mContext.setTheme(resid);
	}

	@Override
	public void setWallpaper(Bitmap bitmap) throws IOException {
		mContext.setWallpaper(bitmap);
	}

	@Override
	public void setWallpaper(InputStream data) throws IOException {
		mContext.setWallpaper(data);
	}

	@Override
	public void startActivity(Intent intent) {
		mContext.startActivity(intent);
	}

	@Override
	public boolean startInstrumentation(ComponentName className,
			String profileFile, Bundle arguments) {
		return mContext.startInstrumentation(className, profileFile, arguments);
	}

	@Override
	public void startIntentSender(IntentSender intent, Intent fillInIntent,
			int flagsMask, int flagsValues, int extraFlags)
			throws SendIntentException {
		mContext.startIntentSender(intent, fillInIntent, flagsMask, flagsValues, extraFlags);
	}

	@Override
	public ComponentName startService(Intent service) {
		return mContext.startService(service);
	}

	@Override
	public boolean stopService(Intent service) {
		return mContext.stopService(service);
	}

	@Override
	public void unbindService(ServiceConnection conn) {
		mContext.unbindService(conn);
	}

	@Override
	public void unregisterReceiver(BroadcastReceiver receiver) {
		mContext.unregisterReceiver(receiver);
	}

	@Override
	public File getExternalCacheDir() {
		return mContext.getExternalCacheDir();
	}

	@Override
	public File getExternalFilesDir(String arg0) {
		return mContext.getExternalFilesDir(arg0);
	}

	@Override
	public String getPackageCodePath() {
		return mContext.getPackageCodePath();
	}

	@Override
	public String getPackageResourcePath() {
		return mContext.getPackageResourcePath();
	}
}
