package com.hackathon.tvnight.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsMessage;
import android.widget.Toast;
import android.net.Uri;
import android.database.Cursor;
import android.content.ContentResolver;
import android.util.Log;

import java.lang.Exception;

import com.hackathon.tvnight.ui.InviteProposeActivity;
import com.hackathon.tvnight.ui.ReminderActivity;

public class SMSReceiver extends BroadcastReceiver {
	private final static String TAG = SMSReceiver.class.getSimpleName();
	private final static String MSG_PREFIX = "TVN_";
	private final static String REMINDER_PREFIX = "RMD_";
		
	public final static String EXTRA_SHOW_ID = "show_id";
	public final static String EXTRA_SHOW_STARTTIME = "start_time";
	public final static String EXTRA_SHOW_DURATION = "duration";
	public final static String EXTRA_SENDER = "sender";
	public final static String EXTRA_PARTICIPANTS = "participaints";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
        //---get the SMS message passed in---
        Bundle bundle = intent.getExtras();

        if (bundle != null)
        {
        	SmsMessage[] msgs = getMessages(bundle);

        	if (msgs != null) {
        		for (int i=0; i<msgs.length; i++) {        			
        			processMessage(context, msgs[i]);
        		}
        	}
        }                 
	}

	private SmsMessage[] getMessages(Bundle bundle) {
		//---retrieve the SMS message received---
		Object[] pdus = (Object[])bundle.get("pdus");
		SmsMessage[] msgs = null;

		if (pdus != null && pdus.length > 0) {
			msgs = new SmsMessage[pdus.length];
			for (int i=0; i<msgs.length; i++){
				msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
			}
		}
		return msgs;
	}
	
	private void processMessage(Context context, SmsMessage msg) {
		String msgBody =  msg.getMessageBody().toString();
    	Log.e(TAG, "Message:" + msgBody);
    	
		String str = "SMS from " + msg.getOriginatingAddress();                     
		str += " :";
		str += msgBody;
		str += "\n";

		// display the new SMS messages
		Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
 	
		if (msgBody.startsWith(REMINDER_PREFIX)) {
			context.startActivity(createReminderIntent(context, msgBody));
		}
		
   		if (msgBody.startsWith(MSG_PREFIX)) {
   			
			context.startActivity(createInvitationIntent(context, msgBody));

//   			// app directed SMS
//   	   		String action = msgBody.substring(MSG_PREFIX.length()+1);
//
//   	   		if (action.equalsIgnoreCase("control")) {
//   			}
//   			else if (action.equalsIgnoreCase("start service")) {
////   				KidSafe.startService(context);
//   			}
//   			else if (action.equalsIgnoreCase("stop service")) {
////   				KidSafe.stopService(context);   				
//   			}
//   			
//   			abortBroadcast();
//	        
//	        scheduleDeleteMessage(context, msg);
   		}
	}
	
	private void scheduleDeleteMessage(Context context, SmsMessage msg) {
//		msg.get
	}
	
	private void deleteMessage(SmsMessage msg) {
//		Context context = this;

//		Uri uriSms = Uri.parse("content://sms/inbox");
//		ContentResolver cr = context.getContentResolver();
//		try {
//			Cursor c = cr.query(uriSms, null,null,null,null); 
//			int ccount = c.getColumnCount();
//			int rcount = c.getCount();        		
//			Log.e(TAG, "row:" + rcount + " col:" + ccount);
//
//			if (rcount > 0) {
//				c.moveToFirst();
//				int id = c.getInt(0);
//				int thread_id = c.getInt(1); //get the thread_id 
//				Log.e(TAG, "id:" +  id + " thread_id:" + thread_id);
//				//        		int deleted = cr.delete(Uri.parse("content://sms/conversations/" + thread_id), null, null);
//				//        		Log.e(TAG, "row deleted:" + deleted);
//			}
//			else {
//				Log.e(TAG, "no data in database");
//			}
//		}
//		catch (Exception e) {
//			Log.e(TAG, e.getMessage());
//		}
	}

	public static void dumpSMSInbox(Context context) {
    	Uri uriSms = Uri.parse("content://sms/inbox");
    	ContentResolver cr = context.getContentResolver();
    	try {
    		Cursor c = cr.query(uriSms, null,null,null,null); 
    		int ccount = c.getColumnCount();
    		int rcount = c.getCount();
    		
    		Log.e(TAG, "row:" + rcount + " col:" + ccount);
    		for (int i=0; i<rcount; i++) {
    			if (c.moveToPosition(i) == true) {
    				//    		int id = c.getInt(0);
    				//    		int thread_id = c.getInt(1); //get the thread_id 
    				//    		cr.delete(Uri.parse("content://sms/conversations/" + thread_id), null, null);
    			}
    		}
    	}
    	catch (Exception e) {
    		Log.e(TAG, e.getMessage());
    	}
	}
	
	// for coordinating the animation thread and TextView update
	protected Handler m_Handler = new Handler() {
//		
//    	public void handleMessage(Message msg) {
//    		if (msg.what == DELETE_MESSAGE) {
//    			deleteMessage(msg.gmsg.obj);
//    		}
//    	}
	};
	
	protected Intent createReminderIntent(Context context, String msgBody) {
		Intent intent = new Intent(context, ReminderActivity.class);
		intent.putExtra("recips", msgBody.split("_")[1]);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);			
		
		return intent;
}
	
	protected Intent createInvitationIntent(Context context, String msgBody) {
			String inviteSender = msgBody.split("_")[2];
			
			Intent intent = new Intent(context, InviteProposeActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);			
			
			intent.putExtra(EXTRA_SENDER, inviteSender);
//			intent.putExtra(EXTRA_SHOW_STARTTIME, startTime);
//			intent.putExtra(EXTRA_SHOW_DURATION, duration);
//			intent.putExtra(EXTRA_SENDER, sender);
//			intent.putEputExtra(EXTRA_PARTICIPANTS, );	// string array

			return intent;
	}
}
