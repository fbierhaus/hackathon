package com.verizon.mms.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import org.xmlpull.v1.XmlSerializer;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPersister;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Telephony.Mms.Addr;
import android.provider.Telephony.Sms.Inbox;
import android.util.Log;
import android.util.Xml;

public class SaveGetOrCreateThreadIDFailure {
	public static SaveGetOrCreateThreadIDFailure mSaveGetOrCreateThreadIDFailure = null;
	private final String SMS_RECEIVED = "SMS_RECEIVED";
	private final String MMS_RECEIVED = "MMS_RECEIVED";
	private final String SMS_RECEIVED_STATUS = "status";
	private final String MMS_RECEIVED_STATUS = "status";
	private final String STATUS_FAILURE = "Failure";
	private final String VERSION_NO = "3.0.1";
	private final String VERSION = "ver";
	private final String SMS = "SMS_";
	private final String MMS = "MMS_";
	private final String FILE_EXT = ".xml";
	int delay = (1*(1000*60)); // delay for 5 sec.
	int period = (1*(1000*60)); // repeat every sec.
	File cacheDir ;
	Timer timer ;
	static Object lock = new Object();
	private static Context mContext = null;
	public static boolean failedMessage = false;
	private static final int[] ADDRESS_FIELDS = new int[] { PduHeaders.BCC, PduHeaders.CC, PduHeaders.FROM,
        PduHeaders.TO };
	
	private SaveGetOrCreateThreadIDFailure(Context context) {
		cacheDir = new File(context.getCacheDir().toString()+"/FailedThreads");
		cacheDir.mkdir();
		timer = new Timer();
	}
	public static Object getLockObject() {
		return lock;
	}
	public static SaveGetOrCreateThreadIDFailure getInstance(Context context) {
		mContext = context;
		if (mSaveGetOrCreateThreadIDFailure == null) {
			mSaveGetOrCreateThreadIDFailure = new SaveGetOrCreateThreadIDFailure(context);
		}
		return mSaveGetOrCreateThreadIDFailure;
	}
	public void saveReceivedSMS(ContentValues values) throws IOException, Exception{
		String  fileNameCreated = createXML(values);
		if(fileNameCreated != null) {
			failedMessage = true;//Start a times task to retry to get the threadID
			RestoreGetOrCreateThreadIDFailure.retryCount = 0;
			cancelAlarm(mContext);
			setAlarm(mContext);
		}
	}
	public void saveReceivedMMSNotificationIND(ContentValues values, HashSet<String> recipients, HashMap<Integer, EncodedStringValue[]> addressMap) throws IOException, Exception{
		String  fileNameCreated = createXMLNotificationIND(values, recipients, addressMap);
		if(fileNameCreated != null) {
			failedMessage = true;//Start a times task to retry to get the threadID
			RestoreGetOrCreateThreadIDFailure.retryCount = 0;
			cancelAlarm(mContext);
			setAlarm(mContext);
		}
	}
	public void restoreFailedMessages() {
		setAlarm(mContext);
	}
	String createXML(ContentValues values) throws IOException {
		File saveFile = null;
		FileOutputStream fos = null;
		try {
			String smsFileName = SMS + System.currentTimeMillis() + FILE_EXT;
			saveFile = new File(cacheDir, smsFileName);
		    fos = new FileOutputStream(saveFile);
		    XmlSerializer serializer = Xml.newSerializer();
		    serializer.setOutput(fos, "UTF-8");
		    serializer.startDocument("UTF-8", true);
		    serializer.startTag("", SMS_RECEIVED);
		    serializer.attribute("", VERSION, VERSION_NO);
		    serializer.startTag("", "SMS");
		    serializer.attribute("", SMS_RECEIVED_STATUS, STATUS_FAILURE);
		    Set<Entry<String, Object>> s=values.valueSet();
		    Iterator<Entry<String, Object>> itr = s.iterator();
        
	        while (itr.hasNext()) {
	             Map.Entry me = (Map.Entry)itr.next(); 
	             String key = me.getKey().toString();
	             Object value =  me.getValue();
	              serializer.startTag("", key);
	             
	             if(value != null) {
	            	 if (key.equals(Inbox.BODY)) {
	            		 serializer.text(getCDataString(value.toString()));
	            	 } else {
	            		 serializer.text(value.toString());
	            	 }
	             }
	             serializer.endTag("", key);
	        }
	        serializer.endTag("", "SMS");
	        serializer.endTag("", SMS_RECEIVED);
	        serializer.endDocument();
	        return saveFile.getName();
	   }
	   catch(Exception e) {
		   Logger.error(SaveGetOrCreateThreadIDFailure.class,e);
		   return null;
	   }
		finally{
			if (fos != null) {
			 fos.flush();
		     fos.close();
			}
			
		}
   }
	String createXMLNotificationIND(ContentValues values, HashSet<String> recipients, HashMap<Integer, EncodedStringValue[]> addressMap) throws IOException {
		File saveFile = null;
		FileOutputStream fos = null;
		try {
			String mmsFileName = MMS + System.currentTimeMillis() + FILE_EXT;
			saveFile = new File(cacheDir, mmsFileName);
		    fos = new FileOutputStream(saveFile);
		    XmlSerializer serializer = Xml.newSerializer();
		    serializer.setOutput(fos, "UTF-8");
		    serializer.startDocument("UTF-8", true);
		    serializer.startTag("", MMS_RECEIVED);
		    serializer.attribute("", VERSION, VERSION_NO);
		    serializer.startTag("", "MMS");
		    serializer.attribute("", MMS_RECEIVED_STATUS, STATUS_FAILURE);
		    Set<Entry<String, Object>> s = values.valueSet();
		    Iterator<Entry<String, Object>> itr = s.iterator();
        
	        while (itr.hasNext()) {
	             Map.Entry me = (Map.Entry)itr.next(); 
	             String key = me.getKey().toString();
	             Object value =  me.getValue();
	              serializer.startTag("", key);
	             
	             if(value != null) {
	            	if(key.equals("ct_l")) {
	            		 serializer.text(getCDataString(value.toString()));
	                } else {
	                	 serializer.text(value.toString());
	                }
	             }
	            		
	             serializer.endTag("", key);
	        }
	        for (String recipient : recipients) {
	        	StringBuilder recipientBuilder = new StringBuilder();
	        	if (recipient != null) {
	        		recipientBuilder.append(recipient);
	        	}
	        	recipientBuilder.append("|");
	        	serializer.startTag("", "Cust_Receipients");
	        	serializer.text(recipientBuilder.toString());
	        	serializer.endTag("", "Cust_Receipients");
	        	 
	        }
	        for (int addrType : ADDRESS_FIELDS) {
	            EncodedStringValue[] array = addressMap.get(addrType);
	            if (array != null) {
	                persistAddress(serializer, addrType, array);
	            }
	        }
	        
	        serializer.endTag("", "MMS");
	        serializer.endTag("", MMS_RECEIVED);
	        serializer.endDocument();
	        return saveFile.getName();
	   }
	   catch(Exception e) {
		   Logger.error(SaveGetOrCreateThreadIDFailure.class,e);
		   return null;
	   }
		finally{
			if (fos != null) {
			 fos.flush();
		     fos.close();
			}
			
		}
   }
	private void persistAddress(XmlSerializer serializer, int type, EncodedStringValue[] array) throws IOException {
        ContentValues values = new ContentValues(3);
        for (EncodedStringValue addr : array) {
            values.clear(); // Clear all values first.
            serializer.startTag("", "Cust_Address");
            serializer.attribute("", Addr.ADDRESS, PduPersister.toIsoString(addr.getTextString()));
            serializer.attribute("", Addr.CHARSET, ""+addr.getCharacterSet());
            serializer.attribute("", Addr.TYPE, ""+type);   
	        serializer.endTag("", "Cust_Address");
       }
    }
	private String getCDataString(String value) {
		return "<![CDATA[" + value + "]]>";
	}
	
	public void setAlarm(Context context) {
		Log.d("Jerry1","*****************setAlarm");
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RestoreGetOrCreateThreadIDFailure.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.currentThreadTimeMillis(), delay, pi); 
	}

    public void cancelAlarm(Context context) {
    	Log.d("Jerry1","*****************setCancel");
        Intent intent = new Intent(context, RestoreGetOrCreateThreadIDFailure.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

}
