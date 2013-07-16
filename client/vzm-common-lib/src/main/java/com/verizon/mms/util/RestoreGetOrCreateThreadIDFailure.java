package com.verizon.mms.util;

import java.io.File;
import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Inbox;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.SyncConstants;
import com.verizon.common.VZUris;
import com.verizon.mms.MmsException;
import com.verizon.mms.pdu.PduHeaders;
import com.verizon.sync.SyncManager;

public class RestoreGetOrCreateThreadIDFailure extends BroadcastReceiver {
	File cacheDir = null;
	boolean dirPresent = false;
	boolean filesPresent = false;
	boolean goRetry = true;
	String[] fileNames = null;
	public static int retryCount = 0;
	private static final String GETORCREATE_ACTION = "com.verizon.mms.transaction.GETORCREATE_THREADID";

	public RestoreGetOrCreateThreadIDFailure(Context context) {
		try {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("RestoreGetOrCreateThreadIDFailure","*****************stared restore");
			}
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Retry Count : " + retryCount);
			}
			cacheDir = new File(context.getCacheDir().toString()+ "/FailedThreads");
			dirPresent = cacheDir.isDirectory();
			if (dirPresent) {
				fileNames = cacheDir.list();
				if (fileNames != null && fileNames.length > 0) {
					filesPresent = true;
					if (retryCount > 5) {
						if (Logger.IS_DEBUG_ENABLED) {
							Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Retry Count >  5 : " + retryCount);
						}
						goRetry = SaveGetOrCreateThreadIDFailure.failedMessage = false;
						retryCount = 0;
					}
				}
			}
		} catch (Exception e) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.error(RestoreGetOrCreateThreadIDFailure.class,"Exception in GetOrCreateThreadIDTimerTask()" + e);
			}
		}
		retryCount++;
	}
   
	public RestoreGetOrCreateThreadIDFailure() {
		//EmptyConstructor
	}

	@Override
	public void onReceive(final Context context, Intent intent) { 

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
		wl.acquire();
		
		try {
			new Thread(new Runnable() {
	
				@Override
				public void run() {
					Context mContext = context;
					ContentResolver mResolver = context.getContentResolver();	
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Retry to save  run()" + retryCount);
					}
					if (dirPresent && filesPresent && goRetry) {
						try {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Retry inside run()" + retryCount);
							}
							synchronized (SaveGetOrCreateThreadIDFailure.getLockObject()) {
								
								for (String fileName : fileNames) {
									if (fileName.startsWith("SMS")) {
										if (Logger.IS_DEBUG_ENABLED) {
											Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Restoring SMS");
										}
										try {
											ContentValues values = ParseGetOrCreateThreadIDFailure.getInstance(mContext).parseSMS(fileName);
											String address = (String) values.get(Inbox.ADDRESS);
											
											if (values != null && address != null && values.size() > 1) {
												long threadId = VZTelephony.getOrCreateThreadId(mContext, address);
												values.put(Inbox.DATE, Long.valueOf(System.currentTimeMillis()));
												values.put(Sms.THREAD_ID, threadId);
												Uri insertedUri = SqliteWrapper.insert(mContext, mResolver, VZUris.getSmsInboxUri(), values);
												if (insertedUri != null) {
													deletePendingFiles(fileName);
													Intent intent = new Intent(SyncManager.ACTION_SYNC_STATUS);
													intent.putExtra(SyncManager.EXTRA_STATUS, SyncManager.SYNC_SHOW_NONBLOCKING_NOTIFICATION);
													intent.putExtra(SyncManager.EXTRA_INSERT_URI, insertedUri.toString());
													context.sendBroadcast(intent);
												}
												
										     	//Wifi Related Code Commented out
												/*	if (AndroidUtil.isPaired()) {
													Intent intent = new Intent(SyncManager.ACTION_SYNC_CHANGES);
												    intent.putExtra(SyncManager.EXTRA_LUID, msgID);
												    intent.putExtra(SyncManager.EXTRA_MOD_TYPE, RECEIVED_SMS);
												    context.startService(intent);	
											  	}*/
												
											
//												Recycler.getMessageRecycler().deleteOldMessagesByThreadId(mContext,threadId);
												
											} else {
												if (Logger.IS_DEBUG_ENABLED) {
													Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"(Delete)Cannot restore Values : Address "+ values + " : "+ address);
												}
												deletePendingFiles(fileName);
											}
										}
										catch(Exception e) {
											if (Logger.IS_DEBUG_ENABLED) {
												Logger.error(RestoreGetOrCreateThreadIDFailure.class,"Exception in restore SMS"+ e);
											}
										}
									} else if (fileName.startsWith("MMS")) {
										if (Logger.IS_DEBUG_ENABLED) {
											Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Restoring MMS");
										}
										try {
											RestoreMMS restoreMMS = ParseGetOrCreateThreadIDFailure.getInstance(context).parseMMS(fileName);
											if (Logger.IS_DEBUG_ENABLED) {
												Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Restoring MMS content"+ restoreMMS );
											}
											if (restoreMMS != null) {
												if (restoreMMS.recipients != null && restoreMMS.recipients.size() > 0) {
													long threadId = VZTelephony.getOrCreateThreadId(mContext, restoreMMS.recipients);
													
													if (Logger.IS_DEBUG_ENABLED) {
														Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Restoring MMS ThreadID"+ threadId );
													}
													
													restoreMMS.values.put(Mms.THREAD_ID, threadId);
													final Uri msgUri = SqliteWrapper.insert(mContext, mResolver, VZUris.getMmsInboxUri(), restoreMMS.values);
													
													if (Logger.IS_DEBUG_ENABLED) {
														Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Restoring MMS uri"+ msgUri );
													}
													
													if (msgUri == null) {
														throw new MmsException("persist failed to insert the message");
													} else {
														deletePendingFiles(fileName);
														if (Logger.IS_DEBUG_ENABLED) {
															Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Restoring And file Deleted"+fileName);
														}
													}
													final long msgId = ContentUris.parseId(msgUri);
													persistAddress(context, msgId,restoreMMS.addressValues);
													mResolver.notifyChange(VZUris.getMmsSmsUri(), null);
													final Uri retUri = Uri.parse(VZUris.getMmsInboxUri()+ "/"+ msgId);
													ContentValues values = new ContentValues(1);
													values.put(Mms.MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
													int i = SqliteWrapper.update(mContext, mResolver, retUri, values, null, null);
													addPendingTableEntry(mContext, retUri, PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
													Intent svc = new Intent(GETORCREATE_ACTION);
													svc.putExtra(SyncConstants.URI,	retUri.toString());
													svc.putExtra(SyncConstants.TRANSACTION_TYPE, SyncConstants.NOTIFICATION_TRANSACTION);
													mContext.startService(svc);
		
												} else {
													deletePendingFiles(fileName);
													if (Logger.IS_DEBUG_ENABLED) {
														Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Failed to restore so deleted file"+fileName);
													}
												}
											}
										} catch(Exception e) {
											if (Logger.IS_DEBUG_ENABLED) {
												Logger.error(RestoreGetOrCreateThreadIDFailure.class,"Exception in restore MMS"+ e);
											}
										}
									}
	
								}
							}
							wl.release();
	
						} catch (Exception e) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.error(RestoreGetOrCreateThreadIDFailure.class,"Excption in GetOrCreateThreadIDTimerTask()-->run()"+ e);
							}
						}
	
					} else {
						try {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(RestoreGetOrCreateThreadIDFailure.class,"Timer cancelled" + retryCount);
							}
							cancelAlarm(mContext);
							wl.release();
							retryCount = 0;
						} catch (Exception e) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.error(RestoreGetOrCreateThreadIDFailure.class,"Excption in onCancel" + e);
							}
						}
					}
	
				}
			}).start();
		} catch(Exception e) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.error(RestoreGetOrCreateThreadIDFailure.class,"Exception trying to restore failed messages" + e);
			}
		}

	}

	private Uri addPendingTableEntry (Context context, Uri uri, int mType) {
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
        //Since takes long time we neglect this process
    	/*if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(TransactionService.class, "addPendingTableEntry: uri: " + uri + ", mType: "
    				+ mType + ", pendingUri: " + pendingEntryUri);

    		dumpPendingTable(context);
    	}*/
    	return pendingEntryUri;
	}
	
	
	public void cancelAlarm(Context context) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("RestoreGetOrCreateThreadIDFailure","*****************setCancel");
		}
		Intent intent = new Intent(context,	RestoreGetOrCreateThreadIDFailure.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}

	void persistAddress(Context context, long msgid, ArrayList<ContentValues> addressValues) {
		final Uri uri = VZUris.getMmsAddrUri(msgid);
		for (ContentValues values : addressValues) {
			SqliteWrapper.insert(context, context.getContentResolver(), uri, values);
		}
	}
	void deletePendingFiles(String fileName) {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug("RestoreGetOrCreateThreadIDFailure","*****************Deleteing " +fileName);
		}
		File deleteFile = new File(cacheDir, fileName);
		if (deleteFile != null) {
			deleteFile.delete();
		}
	}

}
