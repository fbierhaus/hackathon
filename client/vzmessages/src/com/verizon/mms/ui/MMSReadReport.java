package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Telephony.Mms;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.rocketmobile.asimov.Asimov;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.InvalidHeaderValueException;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.pdu.EncodedStringValue;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.mms.pdu.PduPersister;
import com.verizon.mms.pdu.ReadRecInd;
import com.verizon.mms.transaction.MmsMessageSender;
import com.verizon.mms.transaction.TransactionService;
import com.verizon.mms.util.AddressUtils;
import com.verizon.mms.util.SqliteWrapper;
import com.verizon.mms.util.Util;
import com.verizon.sync.VMASyncHook;


/**
 * Handle the MMS read Report 
 * 
 * @author "Jerald Philip <jerald.philip@strumsoft.com>"
 * 
 */
public class MMSReadReport {
	private static SharedPreferences mPrefs;
	private static boolean isFirstReadReport;

	private static final String SELECTION = Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF + " AND " + Mms.READ
			+ " = 0 AND " + Mms.READ_REPORT + " = " + PduHeaders.VALUE_YES;
	
	
	private static final String THREAD_SELECTION = SELECTION + " AND " + Mms.THREAD_ID + " = ?";

	private static final String[] COLS = { Mms._ID, Mms.MESSAGE_ID };


	public MMSReadReport() {
	}

	/*
	 * Have multiple read report to be sent. Eg :  When user opens a conversation where more that one MMS
	 * with read report status to be sent 
	 * @param threads : conversations
	 * @param status : READ_STATUS__DELETED_WITHOUT_BEING_READ (or) READ_STATUS_READ
	 * @param showDialog = false : Don't notify the user with "A read report will be sent". dialog
	 * @param showDialog = true :  Notify the user with "A read report will be sent". dialog
	 */
	static public void handleReadReport(final Context context, ArrayList<Long> threads, final int status) {
		if (threads != null && threads.size() != 0) {
			for (Long threadId : threads) {
				handleReadReport(context,null, threadId, status);
			}
		}
	}
  
	/*
	 * Handle read report to be sent. Eg :  When user opens the conversation to view 
	 * with read report status to be sent 
	 * @param threadid : conversation
	 * @param status : READ_STATUS__DELETED_WITHOUT_BEING_READ (or) READ_STATUS_READ
	 * @param showDialog = false : Don't notify the user with "A read report will be sent". dialog
	 * @param showDialog = true :  Notify the user with "A read report will be sent". dialog
	 */
	static public void handleReadReport(final Context context, Conversation mConversation, final long threadId, final int status) {

		boolean autoReadReport = isAutoRespondReadReport();
	//	boolean noReadReport = isNeverRespondEnabled();
	//	boolean noDialog = isAutoRespondEnabled();

		if (!autoReadReport) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(MMSReadReport.class, "Our setting wont allow to send readreport (ignore and return)");
			}
			return;
		}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MMSReadReport.class, "Handle Read report for threadID: " + threadId);
		}

		final String selection;
		final String[] args;
		if (threadId != -1) {
			selection = THREAD_SELECTION;
			args = new String[] {
				Long.toString(threadId)
			};
		}
		else {
			selection = SELECTION;
			args = null;
		}

		Cursor c = null;
		try {
			c = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getMmsInboxUri(),
				COLS, selection, args, null);

			if (c != null && c.getCount() != 0) {
				final Map<String, String> readreportRecipList = new HashMap<String, String>();
				while (c.moveToNext()) {
					final Uri uri = ContentUris.withAppendedId(VZUris.getMmsUri(), c.getLong(0));
					final String addr = AddressUtils.getFrom(context, uri);
					final String mid = c.getString(1);
					readreportRecipList.put(mid, addr);

					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(MMSReadReport.class, "msg id = " + mid + ", addr = " + addr);
					}
				}

				if (readreportRecipList.size() != 0) {
					confirmReadReport(readreportRecipList, context, status, false);
					if(mConversation != null) {
						mConversation.markAsRead();
					}
				}
			}
		}
		catch (Exception e) {
			Logger.error(MMSReadReport.class, e);
		}
		finally {
			if (c != null) {
				c.close();
			}
		}
	}
	@Deprecated
	static public void handleReadReport(final Context context, long msgidWifi, final long threadId, final int status) {
		
		boolean autoReadReport = isAutoRespondReadReport();
	//	boolean noReadReport = isNeverRespondEnabled();
	//	boolean noDialog = isAutoRespondEnabled();

		if (!autoReadReport) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(MMSReadReport.class, "Our setting wont allow to send readreport (ignore and return)");
			}
			// Need to remove on 4.0  
			 Conversation conv = new Conversation(context, threadId, false);
             conv.blockMarkAsRead(false);
             conv.markAsRead();
			return;
			
		}
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MMSReadReport.class, "Handle Read report for threadID: " + threadId);
		}

		final String selection;
		final String[] args;
		if (threadId != -1) {
			selection = THREAD_SELECTION;
			args = new String[] {
				Long.toString(threadId)
			};
		}
		else {
			selection = SELECTION;
			args = null;
		}

		Cursor c = null;
		try {
			c = SqliteWrapper.query(context, context.getContentResolver(), VZUris.getMmsInboxUri(),
				COLS, selection, args, null);

			if (c != null && c.getCount() != 0) {
				final Map<String, String> readreportRecipList = new HashMap<String, String>();
				while (c.moveToNext()) {
					long msgid = c.getLong(0);
					if(msgidWifi == msgid) {
						final Uri uri = ContentUris.withAppendedId(VZUris.getMmsUri(), c.getLong(0));
						final String addr = AddressUtils.getFrom(context, uri);
						final String mid = c.getString(1);
						readreportRecipList.put(mid, addr);
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(MMSReadReport.class, "msg id = " + mid + ", addr = " + addr);
						}
					}
				}
       			if (readreportRecipList.size() != 0) {
       				if(status == PduHeaders.READ_STATUS_READ) {
       					confirmReadReport(readreportRecipList, context, status, false);
       					Conversation conv = new Conversation(context, threadId, false);
       					conv.blockMarkAsRead(false);
       					conv.markAsRead();
       				} else {
       					confirmReadReport(readreportRecipList, context, status, false);
       					String where = Mms._ID + "=" + msgidWifi;
       					Uri mmsUri = VZUris.getMmsUri();
       					if (Logger.IS_ACRA_ENABLED) {
       						Logger.debug("isTablet: " + MmsConfig.isTabletDevice() + "mms Uri is: " + mmsUri);
       					}
       					final int rows = SqliteWrapper.delete(context,context.getContentResolver(),
       							mmsUri, where, null);
       					if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(MMSReadReport.class, "msg id = " + msgidWifi + ", delete = " + rows);
						}
       				}
				}
			}
		}
		catch (Exception e) {
			Logger.error(MMSReadReport.class, e);
		}
		finally {
			if (c != null) {
				c.close();
			}
		}
	}

	/*
	 * @param showDialog = false : Don't notify the user with "A read report will be sent". dialog
	 * @param showDialog = true :  Notify the user with "A read report will be sent". dialog
	 * @param status = READ_STATUS__DELETED_WITHOUT_BEING_READ (or) READ_STATUS_READ
	 * @param readreportRecipList : List of recipients to whom status that has to be sent
	 */
	static void confirmReadReport(Map<String, String> readreportRecipList,
			Context context, int status, boolean showDialog) {
		if (readreportRecipList.size() > 0) {
			for (Map.Entry<String, String> readreportRecip : readreportRecipList.entrySet()) {
				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(MMSReadReport.class, "confirmReadReport for : "
						+ readreportRecip.getKey() + " : " + readreportRecip.getValue());
				}
				if (showDialog) {
					showReadReportDialog(readreportRecip.getKey(),
						readreportRecip.getValue(), status, context);
					showDialog = false;
				} else {
					sendReadRec(context, readreportRecip.getValue(), readreportRecip.getKey(), status);
				}
			}
		}
	}

	/*
	* Show the dialog to the user "A read report will be sent"
	* @param positiveListener :  Read report 
	* @param cancelListener   :  Don't send Read report
	* @param status : READ_STATUS__DELETED_WITHOUT_BEING_READ (or) READ_STATUS_READ
	* @param address : To address (Status will be sent to the address)
	* @param messageId : Message id
	*/
	static void showReadReportDialog(final String messageId, final String address, final int status, final Context context) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MMSReadReport.class, "Show Read report dialog for address : " + address +
				"Message Id : " + messageId + "Status : " + status);
		}
		ReadListener positiveListener = new ReadListener() {
			@Override
			public void onClick(View v) {
				sendReadRec(context, address, messageId, status);
				dialog.dismiss();
			}
		};
		ReadListener dontSendListener = new ReadListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		};

		OnCancelListener cancelListener = new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();

			}
		};
		confirmReadReportDialog(context, positiveListener, cancelListener, dontSendListener);
	}

	private static abstract class ReadListener implements View.OnClickListener {
		protected Dialog dialog;

		public void setDialog(Dialog d) {
			dialog = d;
		}

		abstract public void onClick(View v);
	}

	private static void confirmReadReportDialog(Context context, ReadListener positiveListener, OnCancelListener cancelListener,
			ReadListener dontSendListener) {
	
		isFirstReadReport = getSharedPref().getBoolean(MMSReadReportPref.FIRST_READ_REPORT_LAUNCH, true);
		final AppAlignedDialog d = new AppAlignedDialog(context, R.drawable.dialog_alert, R.string.confirm,
				R.string.message_send_read_report);
		d.setCancelable(true);
		positiveListener.setDialog(d);
		dontSendListener.setDialog(d);
		CheckBox checkBox = null;
		if (isFirstReadReport) {
			checkBox = (CheckBox)d.findViewById(R.id.do_not_ask_again);
			checkBox.setText(R.string.pref_summary_readreport_always);
			checkBox.setVisibility(View.VISIBLE);
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						SharedPreferences.Editor edit = mPrefs.edit();
						edit.putBoolean(MMSReadReportPref.MMS_READ_REPORT_AUTO_RESPOND, true);
						edit.putBoolean(MMSReadReportPref.FIRST_READ_REPORT_LAUNCH, false);
						edit.commit();
					}

				}
			});
		}

		Button sendButton = (Button)d.findViewById(R.id.positive_button);
		sendButton.setText(R.string.read_report_send);
		sendButton.setOnClickListener(positiveListener);

		Button dontSendButton = (Button)d.findViewById(R.id.negative_button);
		dontSendButton.setText(R.string.read_report_dont_send);
		dontSendButton.setVisibility(View.VISIBLE);
		dontSendButton.setOnClickListener(dontSendListener);

		cancelListener.onCancel(d);
		d.setOnCancelListener(cancelListener);
		if(!d.isShowing()) 		{
			d.show();
		}
		
	}

	/*  Send the read report .  
	 *  @param to : To address (Status will be sent to the address)
	 *  @param status : READ_STATUS__DELETED_WITHOUT_BEING_READ (or) READ_STATUS_READ
	 *  @param messageId : Message id
	 */
	private static void sendReadRec(Context context, String to, String messageId, int status) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MMSReadReport.class, "Read report final sent to: " + to + ", Message Id: " + messageId + ", Status: " + status);
		}
		EncodedStringValue[] sender = new EncodedStringValue[1];
		sender[0] = new EncodedStringValue(to);
		try {
			final ReadRecInd readRec = new ReadRecInd(new EncodedStringValue(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.getBytes()),
					messageId.getBytes(), PduHeaders.CURRENT_MMS_VERSION, status, sender);
			readRec.setDate(System.currentTimeMillis() / 1000);
			Uri outBoxuri = PduPersister.getPduPersister(context).persist(readRec, VZUris.getMmsOutboxUri());
			Util.addPendingTableEntry(context, outBoxuri, PduHeaders.MESSAGE_TYPE_READ_REC_IND);
			if(MmsConfig.isTabletDevice()){
				if(Logger.IS_DEBUG_ENABLED){
            		Logger.debug("WifiHook: sending read receipt:uri="+outBoxuri);
            	}
            	VMASyncHook.sendReadReceipt(context , ContentUris.parseId(outBoxuri));
			}else{
				context.startService(new Intent(context, TransactionService.class));
			}
			
		}
		catch (InvalidHeaderValueException e) {
			Logger.error(MmsMessageSender.class, "Invalid header value", e);
		}
		catch (MmsException e) {
			Logger.error(MmsMessageSender.class, "Persist message failed", e);
		}
	}

	/*
	 * Auto respond to read report 
	 * Default sent to false
	 */
	static private boolean isAutoRespondReadReport() {
		return getSharedPref().getBoolean(MMSReadReportPref.MMS_READ_REPORT_AUTO_RESPOND, false);
	}

	/*
	 * Auto respond to read report 
	 * Default sent to false 
	 * true = Show Readreport Dialog / false = Send read report response in background
	 */
	static private boolean isAutoRespondEnabled() {
		return getSharedPref().getBoolean(MMSReadReportPref.MMS_READ_REPORT_AUTO_RESPOND, false) ? false : true;
	}

	/*
	 * Never respond to MMS read reports
	 */
	private static boolean isNeverRespondEnabled() {
		return getSharedPref().getInt(MMSReadReportPref.MMS_READ_REPORT_OPTIONS,
			MMSReadReportPref.ALWAYS_RESPOND) == MMSReadReportPref.NEVER_RESPOND;

	}

	private static SharedPreferences getSharedPref() {
		if (mPrefs == null) {
			mPrefs = PreferenceManager.getDefaultSharedPreferences(Asimov.getApplication().getApplicationContext());
		}
		return mPrefs;
	}

	public interface MMSReadReportPref {
		public final String MMS_READ_REPORT = "pref_key_mms_read_reports";
		public final String MMS_READ_REPORT_AUTO_RESPOND = "pref_key_mms_auto_response_read_reports";
		public final int ALWAYS_RESPOND = 0;
		public final int NEVER_RESPOND = 1;
		public final String MMS_READ_REPORT_OPTIONS = "option_selected";
		String AUTO_RESPOND_OPTIONS[] = new String[] { "Always Respond", "Never Respond" };
		public final String FIRST_READ_REPORT_LAUNCH = "first_readreport_launch";
	}
}
