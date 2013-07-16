package com.verizon.mms.ui;

import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_ERROR_TYPE;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_RESPONSE_STATUS;
import static com.verizon.mms.ui.MessageListAdapter.COLUMN_MMS_STATUS;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.pdu.PduHeaders;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.view.View;
import android.widget.Button;

/**
 * This class will retrie ve message status information
 * 
 */
public class MessageDetails extends AsyncTask<Void, Void, Void> {
	private long threadId = 0;
	private long totalSms, totalMms,sentSms, sentMms, receivedSms, receivedMms, failedSms,
			failedMms;
	private Context context;
	private ProgressDialog dialog;

	public MessageDetails(Context context, long longExtra) {
		this.context = context;
		this.threadId = longExtra;
	}

	
	@Override
	protected void onPreExecute() {
		dialog = ProgressDialog.show(context, null,"Reading from DB");
		super.onPreExecute();
	}


	@Override
	protected Void doInBackground(Void... params) {
		sentSms = getSentSms();
		sentMms = getSentMms();
		receivedSms = getReceivedSms();
		receivedMms = getReceivedMms();
		failedSms = getFailedSms();
		failedMms = getFailedMms();
		return null;
	}

	
	@Override
	protected void onPostExecute(Void result) {
		dialog.dismiss();
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "MessageDetails:sentSms" + sentSms
					+ "\tsentMms" + sentMms + "\treceivedSms" + receivedSms
					+ "\treceivedMms" + receivedMms + "\tfailedSms" + failedSms
					+ "\tfailedMms" + failedMms);
		}
		String title = "MMS and SMS Message Status for thread: "+threadId;
		String description = "Sent Sms: "
				+ sentSms + "\nReceived Sms: " + receivedSms + "\nFailed Sms: "
				+ failedSms +"\n------------------------"+ "\n\nSent Mms: "
				+ sentMms + "\nReceived Mms: " + receivedMms + "\nFailed Mms: "
				+ failedMms;
		final Dialog d = new AppAlignedDialog(context,
				R.drawable.launcher_home_icon, title, description);
		d.setCancelable(true);
		Button okButton = (Button) d.findViewById(R.id.positive_button);
		okButton.setText(R.string.button_ok);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
			}
		});
		d.show();
		super.onPostExecute(result);
	}
	
	private long getTotalMms() {
		long count = 0;
        String[] projection = new String[] { "COUNT(*) as mmscount" };
        String where = Mms.MESSAGE_TYPE + "=" + 0x80 + " OR " + Mms.MESSAGE_TYPE + "=" + 0x84 
				+ " AND " + Mms.THREAD_ID + "=" + threadId;
        Cursor c = context.getContentResolver().query(VZUris.getMmsUri(), projection, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                count = c.getLong(0);
            }
            c.close();
        }
        return count;
	}

	private long getTotalSms() {
		long count = 0;
        String[] projection = new String[] { "COUNT(*) as smscount" };
        String where = Sms.TYPE + "!=" + Sms.MESSAGE_TYPE_DRAFT + " AND " + Sms.THREAD_ID + "=" + threadId;
        Cursor c = context.getContentResolver().query(VZUris.getSmsUri(), projection, where, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                count = c.getLong(0);
            }
            c.close();
        }
        return count;
	}


	private long getFailedMms() {
		long count = 0;
		Cursor c = context.getContentResolver().query(
				Conversation.getUri(threadId), MessageListAdapter.PROJECTION,
				null, null, null);
		if (c != null) {
			while (c.moveToNext()) {
				int mErrorType = c.getInt(COLUMN_MMS_ERROR_TYPE);
				int status = c.getInt(COLUMN_MMS_RESPONSE_STATUS);
				if ((status !=0 && status != PduHeaders.RESPONSE_STATUS_OK)
						|| mErrorType >= MmsSms.ERR_TYPE_GENERIC_PERMANENT) {
					count++;
				}
			}
			c.close();
		}
		return count;
	}

	private long getFailedSms() {
		long count = 0;
		String[] projection = new String[] { "COUNT(*) as smscount" };
		String where = Sms.TYPE + "=" + Sms.MESSAGE_TYPE_FAILED + " AND "
				+ Sms.THREAD_ID + "=" + threadId;
		Cursor c = context.getContentResolver().query(VZUris.getSmsUri(),
				projection, where, null, null);
		if (c != null) {
			while (c.moveToNext()) {
				count = c.getLong(0);
			}
			c.close();
		}
		return count;
	}
	
	private long getReceivedMms() {
		long count = 0;
		String[] projection = new String[] { "COUNT(*) as mmscount" };
		String where = Mms.MESSAGE_BOX + "=" + Mms.MESSAGE_BOX_INBOX + " AND "
				+ Mms.THREAD_ID + "=" + threadId;
		Cursor c = context.getContentResolver().query(VZUris.getMmsUri(),
				projection, where, null, null);
		if (c != null) {
			while (c.moveToNext()) {
				count = c.getLong(0);
			}
			c.close();
		}
		return count;
	}

	private long getReceivedSms() {
		long count = 0;
		String[] projection = new String[] { "COUNT(*) as smscount" };
		String where = Sms.TYPE + "=" + Sms.MESSAGE_TYPE_INBOX + " AND "
				+ Sms.THREAD_ID + "=" + threadId;
		Cursor c = context.getContentResolver().query(VZUris.getSmsUri(),
				projection, where, null, null);
		if (c != null) {
			while (c.moveToNext()) {
				count = c.getLong(0);
			}
			c.close();
		}
		return count;
	}

	private long getSentMms() {
		long count = 0;
		String[] projection = new String[] { "COUNT(*) as mmscount" };
		String where = Mms.MESSAGE_BOX + "=" + Mms.MESSAGE_BOX_SENT + " AND "
				+ Mms.THREAD_ID + "=" + threadId;
		Cursor c = context.getContentResolver().query(VZUris.getMmsUri(),
				projection, where, null, null);
		if (c != null) {
			while (c.moveToNext()) {
				count = c.getLong(0);
			}
			c.close();
		}
		return count;
	}

	private long getSentSms() {
		long count = 0;
		String[] projection = new String[] { "COUNT(*) as smscount" };
		String where = Sms.TYPE + "=" + Sms.MESSAGE_TYPE_SENT + " AND "
				+ Sms.THREAD_ID + "=" + threadId;
		Cursor c = context.getContentResolver().query(VZUris.getSmsUri(),
				projection, where, null, null);
		if (c != null) {
			while (c.moveToNext()) {
				count = c.getLong(0);
			}
			c.close();
		}
		return count;
	}
}
