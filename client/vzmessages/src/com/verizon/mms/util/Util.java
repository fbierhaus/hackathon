/*
 * Copyright (C) 2008 SocialMuse, Inc.
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

package com.verizon.mms.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Threads;
import android.support.v4.app.FragmentManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.nbi.map.data.Coordinates;
import com.rocketmobile.asimov.Asimov;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.messaging.vzmsgs.Settings;
import com.verizon.messaging.vzmsgs.provider.SyncItem;
import com.verizon.messaging.vzmsgs.provider.VMAMapping;
import com.verizon.messaging.vzmsgs.provider.Vma.SyncStatusTable;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.DeviceConfig.OEM;
import com.verizon.mms.MediaSyncService;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.GenericPdu;
import com.verizon.mms.pdu.NotificationInd;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.RetrieveConf;
import com.verizon.mms.transaction.TransactionService;



public class Util {
    private static Coordinates coordinates;
    private static final Runtime runtime = Runtime.getRuntime();
    private static final long maxNativeHeap = MmsConfig.getMaxNativeHeap();
    private static final int PAN_UI_MIN_WIDTH = 600;
    private static final double PAN_UI_MIN_SIZE = 8;
    private static boolean mIsMultiPane = false;
    private static int mScreenWidth;
    private static double mScreenSize;
    private static long lastMemDump = 0;
	private static final long MEM_DUMP_INTERVAL = 60 * 1000;  // min interval between mem dumps

	interface TaskListener {
        public void updateStatus(Uri uri , int progess, int total);
    }
    
    public static final class SaveDBTask extends AsyncTask<Void ,Object, String>{
        private Context context;
        private ProgressDialog dialog;

        public SaveDBTask(Context context) {
            this.context=context;
        }

		@Override
		protected String doInBackground(Void... params) {
			String fname = context.getExternalCacheDir() + "/mmsdb.txt.gz";
			try {
				final FileOutputStream fos = new FileOutputStream(fname);
				final BufferedOutputStream out = new BufferedOutputStream(new GZIPOutputStream(fos, 8192));
				if (writeMessageDb(context, out, "|", new TaskListener() {

					@Override
					public void updateStatus(Uri uri, int progess, int total) {
						publishProgress(uri, progess, total);
					}
				})) {
					email(context, null, "message DB", fname, true, true);
				}
				else {
					fname = null;
				}
			}
			catch (Exception e) {
				Logger.error(Util.class, "Utils.saveMessageDb:", e);
				fname = null;
			}
			return fname;
		}
        
		@Override
		protected void onProgressUpdate(Object... values) {
			super.onProgressUpdate(values);
			if (values != null && values.length == 3) {
				Uri uri = (Uri)values[0];
				int progress = (Integer)values[1];
				int count = (Integer)values[2];
				dialog.setMessage("Uri: " + uri + ": " + progress + " of " + count);
			}
		}
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = ProgressDialog.show(context, null, "dumping db");
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            dialog.dismiss();
            Toast.makeText(context,
                    result == null ? "Error saving DB" : "Saved DB to " + result,
                    Toast.LENGTH_LONG).show();
        }
    }
    
    public static void saveDb(Context context) {
        SaveDBTask task = new SaveDBTask(context);
        task.execute();
    }
    
    public static void saveMessageDb(Context context,String filename, ZipOutputStream zipOutputStream ) {
        try {
            ZipEntry entry = new ZipEntry(filename);
            zipOutputStream.putNextEntry(entry);
            writeMessageDb(context, zipOutputStream, "|",null,false);
        }catch (Exception e) {
            Logger.error(Util.class, "Utils.saveMessageDb:" , e);
        }
    }

	public static String saveMessageDb(Context context) {
		String fname = context.getExternalCacheDir() + "/mmsdb.txt.gz";
		try {
			final FileOutputStream fos = new FileOutputStream(fname);
			final BufferedOutputStream out = new BufferedOutputStream(new GZIPOutputStream(fos, 8192));
			if (writeMessageDb(context, out, "|",null)) {
				email(context, null, "message DB", fname, true, true);
			}
			else {
				fname = null;
			}
		}
		catch (Exception e) {
			Logger.error(Util.class, "Utils.saveMessageDb:" , e);
			fname = null;
		}
		return fname;
	}

	private static class DBUri {
		private Uri uri;
		private String sort;

		public DBUri(Uri uri, String sort) {
			this.uri = uri;
			this.sort = sort;
		}
	}

	private static boolean writeMessageDb(Context context, OutputStream os, String delim, TaskListener listener ) throws IOException {
	    return writeMessageDb(context, os, delim, listener, true);
	}
	private static boolean writeMessageDb(Context context, OutputStream os, String delim, TaskListener listener ,boolean closezipStream) throws IOException {
		boolean ret = false;
		final String escdelim = "\\" + delim;
		final Pattern pat = Pattern.compile(".*\\p{Cntrl}.*");
		BufferedWriter out = null;
		final String idSort = BaseColumns._ID + " DESC";
		final String threadIdSort = Sms.THREAD_ID + " DESC," + idSort;

		final DBUri[] uris = {
			new DBUri(VZUris.getTelephonyCarriers(), idSort),
			new DBUri(Uri.withAppendedPath(VZUris.getMmsSmsUri(), "canonical-addresses"), idSort),
			new DBUri(VZUris.getSmsUri(), idSort),
			new DBUri(VZUris.getSmsDraftUri(), idSort),
			new DBUri(VZUris.getMmsUri(), idSort),
			new DBUri(VZUris.getMmsDraftsUri(), idSort),
			new DBUri(Uri.withAppendedPath(VZUris.getMmsUri(), "part"), idSort),
			new DBUri(Uri.withAppendedPath(VZUris.getMmsSmsUri(), "pending"), idSort),
			new DBUri(VZUris.getMmsSmsConversationUri(), threadIdSort),
			new DBUri(VZUris.getMmsSmsConversationUri().buildUpon().appendQueryParameter("simple", "true").build(), Threads.DATE + " DESC"),
			// WiFi sync db
//			new DBUri(BaseMapping.SERVER_URI, BaseMapping.UID + " ASC "),
//			new DBUri(BaseMapping.CLIENT_URI, BaseMapping.UID + " ASC "),
//			new DBUri(MessageEvents.CONTENT_URI, MessageEvents.MOD_SEQ_NO + " ASC "),
			// VMA 
			new DBUri(SyncItem.CONTENT_URI, SyncItem._ID + " DESC "),
			new DBUri(VMAMapping.CONTENT_URI, VMAMapping._ID + " DESC "),
			new DBUri(AppSettings.SETTINGS_URI, Settings._ID + " ASC "),
			new DBUri(SyncStatusTable.CONTENT_URI, null),
			// Media sync 
			new DBUri(MediaSyncService.CACHE_URI, null)
		};

		final DBUri[] mmsUris = {
			new DBUri(VZUris.getMmsReportRequestUri(), idSort),
			new DBUri(VZUris.getMmsReportStatusUri(), idSort)
		};

		final ContentResolver resolver = context.getContentResolver();
		try {
			out = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
			final ArrayList<Long> mmsIds = new ArrayList<Long>(256);

			 // Writing build number
            out.write("\nLast    build number :" +ApplicationSettings.getInstance().getLastBuildNo());
            out.write("\nCurrent build number :" +ApplicationSettings.getInstance().getCurrentBuildNo());
            out.write("\n Current build number :" + new Date()+"\n\n");
			// print the non-id ones
			final Uri mmsUri = VZUris.getMmsUri();
			for (DBUri dburi : uris) {
				final Uri uri = dburi.uri;
				out.write(uri.toString());
				out.write("\n");
				final boolean saveIds = uri.equals(mmsUri);  // save mms ids
				writeTable(uri, dburi.sort, out, resolver, true, delim, escdelim, pat, saveIds, mmsIds, listener);
				out.write("\n");
			}

			// print the ones that take an mms id
			/*int num = mmsIds.size();
			if (num > 0) {
				// only dump the most recent since the queries are very inefficient
				if (num > 100) {
					num = 100;
				}
				for (DBUri dburi : mmsUris) {
					final Uri uri = dburi.uri;
					out.write(uri.toString());
					out.write("\n");
					for (int i = 0; i < num; ++i) {
						final Long id = mmsIds.get(i);
						final Uri muri = Uri.withAppendedPath(uri, id.toString());
						writeTable(muri, dburi.sort, out, resolver, i == 0, delim, escdelim, pat, false, null, listener);
					}
					out.write("\n");
				}
			}
*/
			
			// DUMP the MMS Address table
			out.write("\n");
			for (Long mmsId : mmsIds) {
			       Uri uri = VZUris.getMmsAddrUri(mmsId);
                   writeTable(uri, null, out, resolver, true, delim, escdelim, pat, false, mmsIds, listener);   
			}
			out.write("\n");
			ret = true;
		}
		catch (Exception e) {
			Logger.error(Util.class, e);
		}
		finally {
			if (out != null && closezipStream) {
				try {
					out.close();
				}
				catch (Exception e) {
					Logger.error(Util.class, e);
					ret = false;
				}
			}
		}
		return ret;
	}

	private static void writeTable(Uri uri, String sort, BufferedWriter out, ContentResolver resolver, boolean printColumns,
			String delim, String escdelim, Pattern pat, boolean saveIds, ArrayList<Long> ids, TaskListener listener) {
		try {
			final Cursor cursor = resolver.query(uri, null, null, null, sort);
			if (cursor != null) {
				int idCol = 0;
				if (saveIds) {
					idCol = cursor.getColumnIndex("_id");
				}

				final int num = cursor.getColumnCount();

				if (printColumns) {
					final String[] cols = cursor.getColumnNames();
					for (int i = 0; i < num; ++i) {
						if (i > 0) {
							out.write(delim);
						}
						out.write(cols[i]);
					}
					out.write("\n");
				}

				// get data
				int progress = 0;
				final int rowcount = cursor.getCount();
	            boolean isSms =(uri==VZUris.getSmsUri())?true:false;
	            int smsBodyColumnIndex=-1;

				while (cursor.moveToNext()) {
				    if(isSms && smsBodyColumnIndex  < 0){
				        smsBodyColumnIndex =cursor.getColumnIndex(Sms.BODY);
                    }
					for (int i = 0; i < num; ++i) {
						String data = cursor.getString(i);
						// handle binary or null data and escape delimiters
						if (data == null || pat.matcher(data).matches()) {
							data = "";
						}
						else {
							data = data.replace(delim, escdelim);
						}
						if (i > 0) {
							out.write(delim);
						}
						if(isSms && smsBodyColumnIndex== i){
						    data= getCheckSum(cursor)+"";
						}
						out.write(data);
					}
					out.write("\n");
					
					if (saveIds) {
						ids.add(cursor.getLong(idCol));
					}

					if (listener != null){
					    listener.updateStatus(uri, ++progress, rowcount);
					}
				}
				cursor.close();
			}
		}
		catch (Exception e) {
			Logger.error(Util.class, e);
		}
	}

	/**
     * This Method 
     * @param cursor
     * @return
	 * @throws IOException 
     */
    private static long getCheckSum(Cursor cursor) throws IOException {
        String address= ApplicationSettings.parseAdddressForChecksum(cursor.getString(cursor.getColumnIndex(Sms.ADDRESS)));
        String source = address+cursor.getString(cursor.getColumnIndex(Sms.BODY));
//        return ApplicationSettings.getInstance().computeCheckSum(new ByteArrayInputStream(source.getBytes()));
        return ApplicationSettings.computeCheckSum(source.getBytes());
    }

    public static void emailTraces(Context context) {
		final String fname = "/data/anr/traces.txt";
		InputStream in = null;
		try {
			final File file = new File(fname);
			if (file.exists()) {
				int size = (int)file.length();
				if (size > 0) {
					if (size > 1024 * 1024) {
						size = 1024 * 1024;
					}
					final byte buf[] = new byte[size];
					in = new BufferedInputStream(new FileInputStream(fname), size);
					in.read(buf);
					email(context, null, "traces.txt", new String(buf), false, false);
				}
			}
		}
		catch (Exception e) {
			Logger.error(Util.class,e);
		}
		finally {
			if (in != null) {
				try {
					in.close();
				}
				catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Invoke the mailer to email either a file or a string.
	 * @param context
	 * @param to Optional To: address, null otherwise
	 * @param subject Optional subject, null otherwise
	 * @param content Filename of content if attach is true, otherwise body of message
	 * @param attach True if content is filename to attach instead of body
	 * @param compressed True if content is filename of compressed content
	 */
	public static void email(Context context, String to, String subject, String content, boolean attach, boolean compressed) {
		final Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType(compressed ? "application/gzip" : "text/plain");
		if (attach) {
			intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + content));
		}
		else {
			intent.putExtra(Intent.EXTRA_TEXT, content == null ? "" : content);
		}
		if (to != null) {
			intent.putExtra(Intent.EXTRA_EMAIL, new String[] { to });
		}
		if (subject != null) {
			intent.putExtra(Intent.EXTRA_SUBJECT, subject);
		}
		context.startActivity(Intent.createChooser(intent, "Email using:"));
	}

	public static String dumpIntent(Intent intent, String prefix) {
		if (intent == null) {
			return "null";
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(intent.toString());
		final Bundle b = intent.getExtras();
		if (b != null) {
			sb.append(":\n");
			for (String key : b.keySet()) {
				if (prefix != null) {
					sb.append(prefix);
				}
				sb.append(key);
				sb.append(" = ");
				sb.append(b.get(key));
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	
	public static String dumpMessage(Message msg) {
		if (msg == null) {
			return "null";
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(msg.toString());
		final Bundle b = msg.getData();
		if (b != null) {
			sb.append(", {");
			for (String key : b.keySet()) {
				sb.append(key);
				sb.append("=");
				sb.append(b.get(key));
				sb.append(", ");
			}
			sb.append("}");
		}
		
		return sb.toString();
	}
	
	
	public static void saveLocalLog(String filename ,ZipOutputStream out) {
        try {
            ZipEntry zipEntry = new ZipEntry(filename);
            out.putNextEntry(zipEntry);
            Logger.writeLocalLog(out  , false);
        }
        catch (Exception e) {
            Logger.error(e);
        }
    }

	public static String saveLocalLog(Context context) {
		String fname = context.getExternalCacheDir() + "/mmslog.txt.gz";
		try {
			final FileOutputStream fos = new FileOutputStream(fname);
			final BufferedOutputStream out = new BufferedOutputStream(new GZIPOutputStream(fos, 8192));
			if (Logger.writeLocalLog(out)) {
				email(context, null, "local log", fname, true, true);
			}
			else {
				fname = null;
			}
		}
		catch (Exception e) {
			Logger.error(e);
			fname = null;
		}
		return fname;
	}
	
	public static boolean isThereVzwAppApnLib(Context context) {
		return false;
		
//		if (Build.VERSION.SDK_INT > 11) {
//			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
//			if (sp.contains(vzApnPreference)) {
//				if (Logger.IS_DEBUG_ENABLED)
//					Logger.debug("Found preference");
//				return true;
//			}
//			else {
//				PackageManager pm = context.getPackageManager();
//				String[] names = pm.getSystemSharedLibraryNames();
//				for (int i = 0; i<names.length; i++) {
//					if (names[i].startsWith("com.vzw.apnlib")) {
//						if (Logger.IS_DEBUG_ENABLED)
//							Logger.debug("Found vzw apnlib - " + names[i]);
//						Editor e = sp.edit();
//						e.putBoolean(vzApnPreference, true);
//						e.commit();
//						return true;
//					}
//				}
//			}
//		}
//		if (Logger.IS_DEBUG_ENABLED)
//			Logger.debug("Found vzw apnlib - NOT");
//		return false;
	}

	public static Bitmap getPhoto(Context context, long id) {
		Bitmap photo = null;

		if (id != -1) {
			Uri uri = ContentUris.withAppendedId(
					ContactsContract.Contacts.CONTENT_URI, id);
			try {
				InputStream photoDataStream = Contacts
						.openContactPhotoInputStream(
								context.getContentResolver(), uri);
				if (photoDataStream != null) {
					photo = BitmapManager.INSTANCE.decodeStream(photoDataStream);
					photoDataStream.close();
				}
			} catch (IOException e) {
				Logger.error(e);
			}
		}

		return photo;
	}
	
	static public void hideKeyboard(Activity activity, View view) {
	    if (view != null) {
	        InputMethodManager inputManager = (InputMethodManager) activity
	                .getSystemService(Context.INPUT_METHOD_SERVICE);
	        inputManager.hideSoftInputFromWindow(view.getWindowToken(),
	                InputMethodManager.HIDE_NOT_ALWAYS);
	    }
	}

	static public void forceHideKeyboard(Activity activity, View view) {
	    if (view != null) {
	        InputMethodManager inputManager = (InputMethodManager) activity
	                .getSystemService(Context.INPUT_METHOD_SERVICE);
	        inputManager.hideSoftInputFromWindow(view.getWindowToken(),
	                0);
	    }
	}

    static public void showKeyboard(Activity activity, View view) {
    	InputMethodManager inputManager = (InputMethodManager) activity
		.getSystemService(Context.INPUT_METHOD_SERVICE);
    	inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }
    
    static public void forceShowKeyboard(Activity activity, View view) {
        InputMethodManager inputManager = (InputMethodManager) activity
        .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }

	static public void setMapCenter (Coordinates cord) {
	    coordinates = cord;
	}
    
    static public Coordinates getMapCenter(){
        return coordinates;
    }

	/**
     * Compares two floating point numbers with a default margin of 0.0001
     *
     * @param v1 The first variable for comparison
     * @param v2 The second variable for comparison
     * @return true if they are equal to each within the margin
     */
    public static boolean compareFloat(float v1, float v2) {
    	final float margin = 0.0001f;
    	return compareFloat(v1, v2, margin);
    }
    
    /**
     * Compares two floating point numbers with a default margin of 0.0001
     * This function works as: abs(v1 - v2) > margin;
     *
     * @param v1 The first variable for comparison
     * @param v2 The second variable for comparison
     * @param margin The maximum difference before they are considered the same
     * @return true if they are equal to each within the margin
     */
    public static boolean compareFloat(float v1, float v2, float margin) {
    	return Math.abs(v1 - v2) <= margin;
    }
	
	/**
	 * 
	 * This Method returns the actual starting coordinates of the Bitmap in the imageview  
	 * @param img
	 * @param includeLayout
	 * @return
	 */
    public static Point getBitmapOffset(ImageView img, Boolean includeLayout) {
        Point offset = new Point();
        float[] values = new float[9];

        Matrix m = img.getImageMatrix();
        m.getValues(values);

        offset.x = (int) values[2];
        offset.y = (int) values[5];

        if (includeLayout) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) img.getLayoutParams();
            int paddingTop = (int) (img.getPaddingTop());
            int paddingLeft = (int) (img.getPaddingLeft());

            offset.x += paddingLeft + lp.leftMargin;
            offset.y += paddingTop + lp.topMargin;
        }
        return offset;
    }

    public static boolean onMainThread() {
		return Looper.myLooper() == Looper.getMainLooper();
	}

	public static String dumpPdu(GenericPdu pdu) {
		final StringBuilder sb = new StringBuilder("type = ");
		final int type = pdu.getMessageType();
		sb.append(type);
		if ((type == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) || (type == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND_X)) {
			final NotificationInd ni = (NotificationInd)pdu;
			sb.append(", size = ");
			sb.append(ni.getMessageSize());
			EncodedStringValue val = ni.getFrom();
			sb.append(", from = ");
			sb.append(val == null ? "null" : val.getString());
			sb.append(", subject = ");
			val = ni.getSubject();
			sb.append(val == null ? "null" : val.getString());
		}
		else if (type == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) {
			final RetrieveConf rc = (RetrieveConf)pdu;
			EncodedStringValue val = rc.getFrom();
			sb.append(", from = ");
			sb.append(val == null ? "null" : val.getString());
			EncodedStringValue[] vals = rc.getTo();
			sb.append(", to = ");
			sb.append(vals == null ? "null" : getEncodedStringArray(vals));
			vals = rc.getCc();
			sb.append(", cc = ");
			sb.append(vals == null ? "null" : getEncodedStringArray(vals));
			long date = rc.getDate();
			sb.append(", date = ");
			sb.append(date);
			sb.append(", subject = ");
			val = rc.getSubject();
			sb.append(val == null ? "null" : val.getString());
		}
		return sb.toString();
	}

	private static String getEncodedStringArray(EncodedStringValue[] vals) {
		final StringBuilder sb = new StringBuilder();
		String delim = "[";
		for (EncodedStringValue val : vals) {
			sb.append(delim);
			sb.append(val.getString());
			delim = ", ";
		}
		sb.append("]");
		return sb.toString();
	}

	public static String formatPhone(String phoneNumber) {
		if (phoneNumber.length() == 10) {
			StringBuilder sb = new StringBuilder(phoneNumber);
			sb.insert(0, "1-");
			sb.insert(5, "-");
			sb.insert(9, "-");
			return sb.toString();
		}
		return null;
	}

	public static String getStackTrace() {
		StringWriter w = new StringWriter();
		new Throwable().printStackTrace(new PrintWriter(w));
		return w.toString();
	}
	
	public static boolean isOnPhoneCall(Context context) {
	    TelephonyManager telephonymanager = (TelephonyManager)context.getSystemService("phone");
	    int i = telephonymanager.getNetworkType();
	    if(i == 14 || i == 13)
	        return false;
	    return telephonymanager.getCallState() != 0;
	}
	
	/**
	 * This function is used to determine if we have to load multipane UI
	 * @param act
	 * @return
	 */
    public static boolean isMultiPaneSupported(Activity act) {
        if (mScreenWidth == 0) {
            Display display = act.getWindowManager().getDefaultDisplay();
            mScreenWidth = java.lang.Math.min(display.getHeight(), display.getWidth());

            DisplayMetrics dm = new DisplayMetrics();
            display.getMetrics(dm);

            double width = Math.pow(dm.widthPixels/dm.xdpi, 2);
            double height = Math.pow(dm.heightPixels/dm.ydpi, 2);
			
			//calculate the screen size in inches
            mScreenSize = Math.sqrt(width + height);

            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(Util.class, "isMultiPaneSupported: minWidthOrHeight :" + mScreenWidth);
            }
            
            if (MmsConfig.isTabletDevice() && mScreenWidth > PAN_UI_MIN_WIDTH && mScreenSize >= PAN_UI_MIN_SIZE) {
                mIsMultiPane = true;
            }
        }
        return mIsMultiPane;
    }
    
    public static boolean isMultipane(Display display) {
    	int screenWidth = java.lang.Math.min(display.getHeight(), display.getWidth());

        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);

        double width = Math.pow(dm.widthPixels/dm.xdpi, 2);
        double height = Math.pow(dm.heightPixels/dm.ydpi, 2);
		
		//calculate the screen size in inches
        double mScreenSize = Math.sqrt(width + height);

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(Util.class, "isMultiPaneSupported: minWidthOrHeight :" + screenWidth);
        }
        
        if (MmsConfig.isTabletDevice() && screenWidth > PAN_UI_MIN_WIDTH && mScreenSize >= PAN_UI_MIN_SIZE) {
            return true;
        }
        return false;
    }
    
    public static boolean isMultipane(Context context) {
    	final WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		final Display display = wm.getDefaultDisplay();
    	
		return isMultipane(display);
    }

    public static Uri addPendingTableEntry(Context context, Uri uri, int mType) { 
    	ContentValues values = new ContentValues();

    	// Default values for pending messages
    	values.put(PendingMessages.MSG_ID, ContentUris.parseId(uri));
    	values.put(PendingMessages.MSG_TYPE, mType);
    	values.put(PendingMessages.PROTO_TYPE, MmsSms.MMS_PROTO);
    	values.put(PendingMessages.DUE_TIME, 0);
    	values.put(PendingMessages.ERROR_CODE, 0);
    	values.put(PendingMessages.RETRY_INDEX, 0);
    	values.put(PendingMessages.ERROR_TYPE, 0);
    	values.put(PendingMessages.LAST_TRY, 0);
    	Uri pendingEntryUri =  context.getContentResolver().insert(VZUris.getMmsSmsPendingUri(), values);
    
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(TransactionService.class, "addPendingTableEntry: uri: " + uri + ", mType: "
    				+ mType + ", pendingUri: " + pendingEntryUri);

    		dumpPendingTable(context);
    	}
    	return pendingEntryUri;
    }

    // dumping this was taking some time and causing an ANR as other threads get blocked on file system write
    public static void dumpPendingTable(Context context) {
//    	if (Logger.IS_DEBUG_ENABLED) {
//		    // Dumping the pending table
//	    	Cursor c = null;
//	    	try {
//	    		c = context.getContentResolver().query(VZUris.getMmsSmsPendingUri(), null,null,null,null);
//	    		if(c!=null) {
//	    			String[] cnames = c.getColumnNames();
//	    			final StringBuilder sb = new StringBuilder("pending table:");
//					final String delim = "\n ";
//	    			while(c.moveToNext()){
//	    				sb.append(delim);
//	    				for (String column : cnames) {
//	    					sb.append(' ');
//	    					sb.append(column);
//	    					sb.append('=');
//	    					sb.append(c.getString(c.getColumnIndex(column)));
//	    				}
//	    			}
//					Logger.debug(TransactionService.class, sb.toString());
//	    		}
//	    	} finally {
//	    		if (null != c) 
//	    			c.close();	            		
//	    	}   		
//    	}
    }

	public static void dumpView(View view, String prefix) {
		final StringBuilder sb = new StringBuilder(prefix);
		sb.append(" ");
		sb.append(view.toString());
		sb.append(" ");
		sb.append(Integer.toHexString(view.getId()));
		sb.append(" ");
		final int vis = view.getVisibility();
		sb.append(vis == View.VISIBLE ? "VISIBLE" : vis == View.GONE ? "GONE" : "INVISIBLE");
		Logger.debug(sb);

		if (view instanceof ViewGroup) {
			final ViewGroup vg = (ViewGroup)view;
			final int num = vg.getChildCount();
			final String pfx = prefix + "  ";
			for (int i = 0; i < num; ++i) {
				dumpView(vg.getChildAt(i), pfx);
			}
		}
	}
	
	public static BitmapDrawable loadAvatarImage(Context mContext, long id) {
		byte[] data = null;
		Cursor photoCursor = null;
		Bitmap bitmap = null;

		try {
			bitmap = Util.getPhoto(mContext, id);
							
			/*
			 * ... got null. This means there was no photo, or it was a Facebook contact, try the raw query to get the contact.
			 * this raw query is not supported on Ice Cream Sandwich, so skip if that OS is the case.
			 */
			if (bitmap == null && !OEM.isIceCreamSandwich) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug("loadAvatarData no picture found trying query to get fb contact image");
				}
				// Create a URI request from the content table
				Uri contentUri = Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, id + "/data");
				String unionSelect = " 1 ) union all select data15 from view_data where (contact_id=" + id +" AND "+
						ContactsContract.Contacts.Data.MIMETYPE+" == '"+ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "' )";

				photoCursor = SqliteWrapper.query(mContext, mContext.getContentResolver(), contentUri,
						new String[]{ "data15" }, unionSelect + "/*", null, "*/");

				if(photoCursor != null) {
					while(data == null && photoCursor.moveToNext()) {
						if (!photoCursor.isNull(0)) {
							data = photoCursor.getBlob(0);
							bitmap = BitmapManager.INSTANCE.decodeByteArray(data, 0, data.length);
							break;
						}
					}
				}
			}
		} catch (Exception ex) {
			if (Logger.IS_DEBUG_ENABLED) {
    			Logger.debug("loadAvatarData " + id, ex);
			}
		} finally {
			if (photoCursor != null) {
				photoCursor.close();
			}
		}
		
		return bitmap != null ? new BitmapDrawable(bitmap) : null;
	}

	public static void dumpFragmentState(FragmentManager mgr, String tag, String prefix) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintWriter writer = new PrintWriter(new BufferedOutputStream(baos));
		mgr.dump(prefix, null, writer, null);
		writer.flush();
		Logger.debug(tag, baos.toString());
	}

	public static void dumpMemory() {
		final String fname = Asimov.getApplication().getExternalFilesDir(null) + "/memdump.hprof";
		try {
			Debug.dumpHprofData(fname);
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("Util.dumpMemory: dumped to " + fname);
			}
		}
		catch (Exception e) {
			Logger.error(e);
		}
	}

	public static void dumpMemoryWrapper() {
		// dump memory periodically to enable diagnostics
		final long now = SystemClock.uptimeMillis();
		if (now > lastMemDump + MEM_DUMP_INTERVAL) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(Util.class, "dumping memory");
			}
			lastMemDump = now;
			new Thread() {
				public void run() {
					Util.dumpMemory();
				}
			}.start();
		}
	}

	/**
	 * Returns the amount of memory that should be available for allocation.
	 * @param bitmap true if this is for a bitmap allocation
	 * @return the number of bytes that should be available
	 */
	public static long getAvailableMemory(boolean bitmap) {
		long avail = getMaxMemory(true) - getUsedMemory(true);
		if (avail < 0) {
			avail = 0;
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(Util.class, "getAvailableMemory: bitmap = " + bitmap + ", returning " + avail);
		}

		return avail;
	}

	/**
	 * Returns the amount of memory that is currently used.
	 * @param bitmap true if this is for a bitmap allocation
	 * @return the number of bytes that are used
	 */
	public static long getUsedMemory(boolean bitmap) {
		long used;
		if (DeviceConfig.OEM.isHoneycomb || !bitmap) {
			// use the VM heap
			used = runtime.totalMemory() - runtime.freeMemory();
		}
		else {
			// pre-Honeycomb bitmaps are allocated from the native heap
			used = Debug.getNativeHeapAllocatedSize() - Debug.getNativeHeapFreeSize();
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(Util.class, "getUsedMemory: bitmap = " + bitmap + ", returning " + used +
				" with mem = " + getMemString());
		}

		return used;
	}

	public static long getMaxMemory(boolean bitmap) {
		// pre-Honeycomb bitmaps are allocated from the native heap, and unfortunately most
		// devices don't tell us the max heap limit so we max the current limit with our hard-coded limit
		//
		final long max = (DeviceConfig.OEM.isHoneycomb || !bitmap) ?
			runtime.maxMemory() :
			Math.max(Debug.getNativeHeapSize(), maxNativeHeap);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(Util.class, "getMaxMemory: bitmap = " + bitmap + ", returning " + max);
		}

		return max;
	}

	public static String getMemString() {
		return "vm: " + runtime.freeMemory() + " / " + runtime.totalMemory() + " / " + runtime.maxMemory() +
			", native: " + Debug.getNativeHeapFreeSize() + " / " + Debug.getNativeHeapAllocatedSize() +
			" / " + Debug.getNativeHeapSize();
	}

	public static String dumpSms(SmsMessage sms){
	    StringBuffer b = new StringBuffer();
	    b.append("Class =" + sms.getMessageClass().name()+"\n");
	    b.append("body=" + sms.getMessageBody()+"\n");
	    b.append("Address=" + sms.getOriginatingAddress()+"\n");
//	    b.append("UserData=" + new String(sms.getUserData())+"\n");
//	    b.append("Pdu=" + new String(sms.getPdu())+"\n");
	    return b.toString();
	}

	public static boolean isEmulator()
    {
        return Build.PRODUCT.contains("sdk");
    }
	
	public static boolean hasLocalMessaging(Context context)
    {
        if(Build.MODEL.equals("Eris"))
            return true;
        else
            return context.getPackageManager().hasSystemFeature("android.hardware.telephony") || isEmulator();
    }

}
