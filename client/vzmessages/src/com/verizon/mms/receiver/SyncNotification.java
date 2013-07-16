package com.verizon.mms.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.transaction.MessagingNotification;
import com.verizon.mms.util.Util;
import com.verizon.sync.SyncManager;

/**
 * This class/interface
 * 
 * @author Md Imthiaz
 * @Since Jun 21, 2012
 */
public class SyncNotification extends BroadcastReceiver {
    private static final int SYNC_NOTIFICATION_ID = 122;
    private NotificationManager manager;
    // Need to use a static Notification object so that on every call to sendBroadcast a new Object don't get
    // created: Bug 1758
    private static Notification alert;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                if (manager == null) {
                    manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                }

                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("SyncNotification : onReceive " + Util.dumpIntent(intent, null));
                }
                
                String statusMsg;
                String ticker = " ";
                Intent clickIntent = null;
//                long sendingCount;
//                long receivedCount;
//                long total;
                int status;
//                int errorCode = 0;
                PendingIntent pendingIntent;

                boolean isUsedOldNotification = false;
                status = intent.getIntExtra(SyncManager.EXTRA_STATUS, 0);

                switch (status) {

                case SyncManager.NO_WIFI_CONNECTION:
                    statusMsg = context.getString(R.string.vma_sync_failed);
                    ticker = context.getString(R.string.vma_no_wifi_connection);
                    break;
                case SyncManager.NO_DATA_CONNECTION:
                    statusMsg = context.getString(R.string.vma_sync_failed);
                    ticker = context.getString(R.string.vma_no_data_connection);
                    break;
                case SyncManager.SYNC_STATUS_FAILED:
                	statusMsg = context.getString(R.string.vma_sync_failed);
                    break;
                case SyncManager.SYNC_STATUS_LOGIN_FAILED:
                    statusMsg = context.getString(R.string.vma_fails);
                    break;
//                case SyncManager.SYNC_STATUS_SYNC_COMPLETED:
//                    //statusMsg = context.getString(R.string.vma_sync_completed);
//                    break;
//                case SyncManager.SYNC_SENDING_CHANGES:
//                    statusMsg = context.getString(R.string.sync_connected);
//                    sendingCount = intent.getLongExtra(SyncManager.EXTRA_SENDING_COUNT, 0);
//                    total = intent.getLongExtra(SyncManager.EXTRA_TOTAL_COUNT, 0);
//                    if (sendingCount > 0) {
//                        ticker = context.getString(R.string.sync_status_sending, sendingCount, total);
//                    }
//                    isUsedOldNotification = true;
//                    break;
//                case SyncManager.SYNC_FETCHING_MESSAGE:
//                    int count = intent.getIntExtra(SyncManager.EXTRA_TOTAL_COUNT, 0);
//                    receivedCount = intent.getIntExtra(SyncManager.EXTRA_RECEIVING_COUNT, 0);
//                    statusMsg = context.getString(R.string.sync_receiving_messages, receivedCount,count);
//                    isUsedOldNotification = true;
//                    break;
//                case SyncManager.SYNC_FETCH_ATTACHMENTS:
//                    int acount = intent.getIntExtra(SyncManager.EXTRA_TOTAL_COUNT, 1);
//                    receivedCount = intent.getIntExtra(SyncManager.EXTRA_RECEIVING_COUNT, 1);
//                    statusMsg = context.getString(R.string.vma_download_attachements,receivedCount , acount);
//                    isUsedOldNotification = true;
//                    break;
//                    
//                case SyncManager.SYNC_RECEIVING_CHANGES:
//                    int changesCount = intent.getIntExtra(SyncManager.EXTRA_TOTAL_COUNT, 1);
//                    receivedCount = intent.getIntExtra(SyncManager.EXTRA_RECEIVING_COUNT, 1);
//                    if (intent.hasExtra(SyncManager.EXTRA_XCONV_COUNT)) {
//                        // full sync
//                        int xConvReceivedCount = intent
//                                .getIntExtra(SyncManager.EXTRA_XCONV_RECEIVED_COUNT, 1);
//                        int xConvCount = intent.getIntExtra(SyncManager.EXTRA_XCONV_COUNT, 1);
//                        if (xConvCount > 0) {
//                            statusMsg = context.getString(R.string.sync_conversations, xConvReceivedCount,
//                                    xConvCount);
//                            statusMsg = context.getString(R.string.sync_receiving_messages, receivedCount,
//                                    changesCount);
//
//                        } else {
//                            statusMsg = context.getString(R.string.sync_receiving_changes, receivedCount,
//                                    changesCount);
//                        }
//                    } else {
//                        // fast sync
//                        statusMsg = context.getString(R.string.sync_receiving_changes);
//
//                    }
//
//                    isUsedOldNotification = true;
//                    break;
                case SyncManager.SYNC_SHOW_NEW_NOTIFICATION:
                    MessagingNotification.blockingUpdateAllNotificationsWifiSync(context, true, false);
                    return;
                case SyncManager.CLEAR_SHOWN_NOTIFICATION:
                    MessagingNotification.blockingUpdateAllNotifications(context, false);
                    return;
                case SyncManager.SYNC_SHOW_NONBLOCKING_NOTIFICATION:
                	String uriString = intent.getStringExtra(SyncManager.EXTRA_INSERT_URI);
                	if (uriString != null) {
                		Uri uri = Uri.parse(uriString);
                    	MessagingNotification.nonBlockingUpdateNewMessageIndicator(context, true, false, uri);	
                	}
                	return;
                default:
                    return;
                }

                if (alert == null) {
                    alert = new Notification(R.drawable.ic_launcher_notification, ticker != null ? ticker
                            : statusMsg, System.currentTimeMillis());
                }
             
                clickIntent = new Intent(Intent.ACTION_MAIN);
                clickIntent.setClass(context, ConversationListActivity.class);	
                clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                pendingIntent = PendingIntent.getActivity(context, 0, clickIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                if (isUsedOldNotification) {
                    alert.flags |= Notification.FLAG_ONGOING_EVENT;
                    alert.flags |= Notification.FLAG_NO_CLEAR;
                } else {
                    alert = new Notification(R.drawable.ic_launcher_notification, ticker != null ? ticker
                            : statusMsg, System.currentTimeMillis());
                    alert.flags |= Notification.FLAG_AUTO_CANCEL;
                }

                if (ticker != null) {
                    alert.setLatestEventInfo(context, statusMsg, ticker, pendingIntent);
                } else {
                    alert.setLatestEventInfo(context, statusMsg, "", pendingIntent);
                }

                try {
                    manager.notify(SYNC_NOTIFICATION_ID, alert);
                } catch (IllegalArgumentException e) {
                    if (Logger.IS_ERROR_ENABLED) {
                        Logger.error(e);
                    }
                }
            }
        }).start();

    }

    public static void clearSyncNotification(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(SYNC_NOTIFICATION_ID);
    }
}
