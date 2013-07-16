/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since  Sep 8, 2012
 */
package com.verizon.vzmsgs.saverestore;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.transaction.MessagingNotification;
import com.verizon.mms.ui.AdvancePreferenceActivity;
import com.verizon.mms.ui.AppAlignedDialog;
import com.verizon.mms.ui.ConversationListFragment;
import com.verizon.mms.ui.MessagingPreferenceActivity;
import com.verizon.mms.util.Util;
import com.verizon.sync.SyncManager;

/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since Sep 8, 2012
 */
public class PopUpUtil {

	private AppAlignedDialog pd;
	private Context mContext;
	private boolean finish;
	public final String PROGRESS_KEY = "backupInProgress"; 
	private SharedPreferences sp;
	//Notification variables
	public static final String BACKUP_STATUS_NOTIFY = "vzm.backup.progress";
	private static final int BACKUP_NOTIFICATION_ID = 133;
	private static final int BACKUP_INPROGRESS = 2;
	private static final int BACKUP_COMPLETED = 6;
	private NotificationManager manager;
	String statusMsg = "";
	String ticker = null;
	Intent clickIntent = null;
	Notification alert;
	long processCount;
	long total;
	int status;
	Intent statusIntent;
	PendingIntent pendingIntent;
    
	
	
	
	public PopUpUtil(Context context, boolean finish) {
		this.mContext = context;
		statusIntent = new Intent(BACKUP_STATUS_NOTIFY);
		sp = PreferenceManager.getDefaultSharedPreferences(context);
		manager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		clickIntent = new Intent(Intent.ACTION_MAIN);
		clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    this.finish  = finish;	
	   
	}
	
	
	
	public void showProgressDialog(String message) {
		pd = new AppAlignedDialog(mContext,
				mContext.getString(R.string.app_name), message, true, false);
		pd.setCancelable(true);
		pd.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();
				((Activity)mContext).finish();
				
			}
		});
        pd.show();
	}

	public void show(Context context) {
		Toast.makeText(context.getApplicationContext(), context.getString(R.string.file_restore_unable),1).show();
	}
	
	public void dismissProgressDialog() {
    	if (pd != null) {
			pd.dismiss();
			pd = null;
		}
	}
	
	 
    public void showNotification(int status, int progress, int count) {
		
		switch (status) {
		case BackupManagerImpl.BSYNC_SAVE_CANCELLED:
			ticker = null;
			statusMsg = mContext.getString(R.string.save_cancelled);
			break;
		case BackupManagerImpl.BSYNC_RESTORE_CANCELLED:
			ticker = null;
			statusMsg = mContext.getString(R.string.restore_cancelled);
			break;
		case BackupManagerImpl.BSYNC_SD_CARD_INSUFFICIENT_SPACE:
			statusMsg = mContext.getString(R.string.save_cancelled);
			ticker = mContext.getString(R.string.error_insufficient_memory);
			break;
		case BackupManagerImpl.BSYNC_RESTORING_CHANGES:
			statusMsg = mContext.getString(R.string.restore_progress_message);
			total = count;
			processCount = progress;
			if (total == -1) {
			  ticker = mContext.getString(R.string.backup_restore_prepare);
			} else {
				if (processCount != -1) {
					ticker = mContext.getString(R.string.backup_restoring,
							processCount, total);		
				}
				
			}
			
			break;
		case BackupManagerImpl.BSYNC_RESTORE_FAILED:
			ticker = null;
			statusMsg = mContext.getString(R.string.file_restore_unable);
			break;
		case BackupManagerImpl.BSYNC_SAVING_CHANGES:
			clickIntent.putExtra(ConversationListFragment.SAVE_RESTORE_NOTIFICATION, true);
			clickIntent.putExtra("inProgress", true);
			statusMsg = mContext.getString(R.string.save_progress_message);
			total = count;
			processCount = progress;
			if (total > 0) {
				ticker = mContext.getString(R.string.backup_saving,
						processCount, total);
			}
			break;
		case BackupManagerImpl.BSYNC_SAVED:
			clickIntent.putExtra(ConversationListFragment.SAVE_RESTORE_NOTIFICATION, true);
			clickIntent.putExtra("inProgress", false);
			statusMsg = mContext.getString(R.string.file_saved_successfully);
			ticker = null;
			break;
		case BackupManagerImpl.BSYNC_RESTORED:
			statusMsg = mContext.getString(R.string.file_restored_successfully);
			ticker = null;
			break;
		case BackupManagerImpl.BSYNC_SD_CARD_UNMOUNTED :
			statusMsg = mContext.getString(R.string.insert_sd_card);
			int onGoingtask = sp.getInt(PROGRESS_KEY, 0);
			if(onGoingtask == BackupManagerImpl.SAVE) {
				ticker = mContext.getString(R.string.save_cancelled);
			} else if(onGoingtask == BackupManagerImpl.RESTORE)	{
				ticker = mContext.getString(R.string.restore_cancelled);
			} else 	{
				ticker = null;
			}
			break;
	    default:
			return;

		}
		if (status == BackupManagerImpl.BSYNC_SAVING_CHANGES || status == BackupManagerImpl.BSYNC_RESTORING_CHANGES) {
			if (progress == 0) {
				publishStatus(BACKUP_INPROGRESS);	
			}
			
		} else {
			publishStatus(BACKUP_COMPLETED);
		}
		clickIntent.setClass(mContext, ConversationListActivity.class);
		if (status == BackupManagerImpl.BSYNC_SAVE_CANCELLED || status == BackupManagerImpl.BSYNC_RESTORE_CANCELLED || status == BackupManagerImpl.BSYNC_SD_CARD_INSUFFICIENT_SPACE) {
			clickIntent.putExtra(ConversationListFragment.SAVE_RESTORE_NOTIFICATION, false);
		}
		pendingIntent = PendingIntent.getActivity(mContext, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		if (alert == null) {
			alert = new Notification(R.drawable.ic_launcher_notification,
					ticker != null ? ticker : statusMsg, System.currentTimeMillis());
		}
		
		if (status == BackupManagerImpl.BSYNC_SAVED || status == BackupManagerImpl.BSYNC_RESTORED || status == BackupManagerImpl.BSYNC_RESTORE_FAILED || status == BackupManagerImpl.BSYNC_SAVE_CANCELLED || status == BackupManagerImpl.BSYNC_RESTORE_CANCELLED ||  status == BackupManagerImpl.BSYNC_SD_CARD_INSUFFICIENT_SPACE) {
			
			alert = new Notification(R.drawable.ic_launcher_notification,
					ticker != null ? ticker : statusMsg, System.currentTimeMillis());
			alert.flags |= Notification.FLAG_AUTO_CANCEL;
			setNotificationFlags(alert);
		} else if (status == BackupManagerImpl.BSYNC_SD_CARD_UNMOUNTED) {
			alert.flags |= Notification.FLAG_AUTO_CANCEL;
			setNotificationFlags(alert);
		} else {
			alert.flags |= Notification.FLAG_ONGOING_EVENT;
			alert.flags |= Notification.FLAG_NO_CLEAR;
		}
		
		if (ticker != null) {
			alert.setLatestEventInfo(mContext, statusMsg, ticker, pendingIntent);	
		} else {
			alert.setLatestEventInfo(mContext, statusMsg, "", pendingIntent);
		}
		manager.notify(BACKUP_NOTIFICATION_ID, alert);
	
	}
	
	public void clearSyncNotification() {
		NotificationManager nm = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(BACKUP_NOTIFICATION_ID);

	}
	
	public void showErrorDialog(final Context context, String title,
			String message) {
		final AppAlignedDialog dialog = new AppAlignedDialog(context,
				R.drawable.dialog_alert, title, message);
		Button okButton = (Button) dialog.findViewById(R.id.positive_button);
		okButton.setText(android.R.string.ok);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				((Activity) context).finish();
			}
		});
		dialog.show();
	}

	public void fileEmptyPopup(final Context context) {
		final Dialog d = new AppAlignedDialog(context, R.drawable.dialog_alert,
				R.string.error, R.string.file_empty);
		Button yesButton = (Button) d.findViewById(R.id.positive_button);
		yesButton.setText(R.string.yes);
		d.setCancelable(false);
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				d.dismiss();
				if (finish) {
					((Activity) context).finish();
				}
				
			}
		});

		
		d.show();
	}
	
	
	public void verNotSupportedPopup(final Context context) {
		final Dialog d = new AppAlignedDialog(context, R.drawable.dialog_alert,
				R.string.error, R.string.xml_version_not_supported);
		Button yesButton = (Button) d.findViewById(R.id.positive_button);
		yesButton.setText(R.string.yes);
		d.setCancelable(false);
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
 				d.dismiss();
 				if (finish) {
					((Activity) context).finish();
				}
			}
		});

		
		d.show();
	}
	
	private void setNotificationFlags(Notification notification) {
		boolean disableIfOnCall = sp
				.getBoolean(
						AdvancePreferenceActivity.NOTIFICATION_DISABLE_DURING_PHONE_CALL,
						true);
		boolean onCall = Util.isOnPhoneCall(mContext);

		/*
		 * The function isOnPhoneCall() will give false for EHRPD or LTE even if
		 * we are in a call and we actually need to block sound/vibrate if we're
		 * on a call and disableIfOnCall is true.
		 */
		TelephonyManager tm = (TelephonyManager) mContext
				.getSystemService(Context.TELEPHONY_SERVICE);
		int state = tm.getCallState();
		if (state == TelephonyManager.CALL_STATE_OFFHOOK
				|| state == TelephonyManager.CALL_STATE_RINGING) {
			onCall = true;
		}

		if (!onCall || !disableIfOnCall) {

			String vibrateWhen;
			if (sp.contains(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN)) {
				vibrateWhen = sp.getString(
						MessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN,
						null);
			} else {
				vibrateWhen = mContext
						.getString(R.string.prefDefault_vibrateWhen);
			}

			boolean vibrateAlways = vibrateWhen.equals("always");
			boolean vibrateSilent = vibrateWhen.equals("silent");
			AudioManager audioManager = (AudioManager) mContext
					.getSystemService(Context.AUDIO_SERVICE);
			boolean nowSilent = audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE;

			if (vibrateAlways || vibrateSilent && nowSilent) {
				notification.defaults |= Notification.DEFAULT_VIBRATE;
			}
		}

		boolean onCallStatus = onCall && disableIfOnCall;
		String ringtoneStr = sp.getString(
				MessagingPreferenceActivity.NOTIFICATION_RINGTONE, null);
		if (!onCallStatus) {
			notification.sound = TextUtils.isEmpty(ringtoneStr) ? null : Uri
					.parse(ringtoneStr);
		}

		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = MessagingNotification.getNotificationColour(sp);
		notification.ledOnMS = 500;
		notification.ledOffMS = 2000;
	}
	
	private void publishStatus(int status) {
		 if(Logger.IS_DEBUG_ENABLED) {
			 Logger.debug("Publishing sync status update to UI  - " + status);
		 }
		 statusIntent.putExtra(SyncManager.EXTRA_STATUS, status);
		 mContext.sendBroadcast(statusIntent);
	 }

	
}
