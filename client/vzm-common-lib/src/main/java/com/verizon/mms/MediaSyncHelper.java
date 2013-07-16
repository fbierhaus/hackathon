package com.verizon.mms;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;

public class MediaSyncHelper {
    public static final String ACTION_MEDIA_SYNC="com.verizon.mms.MediaSyncService";
    public static final String EVENT_MEDIA_SYNC = "com.verizon.mms.EVENT_MEDIA_SYNC";
    public static final int EVENT_MMS_DELETE = 1001;
    public static final int EVENT_SMS_DELETE = 1002;
    public static final int EVENT_THREAD_DELETE = 1003;
    public static final int EVENT_ALL_DELETE = 1004;

	static final String separator = ",";
	
	public static void onThreadDelete(Context context, Long... threadIds) {
		Intent intent = new Intent();
		intent.setAction(ACTION_MEDIA_SYNC);
		intent.putExtra("event", EVENT_MEDIA_SYNC);
		intent.putExtra("type", EVENT_THREAD_DELETE);
		intent.putExtra("payload", join(threadIds));
		context.startService(intent);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MediaSyncHelper.class, "===>> onThreadDelete, intent = " + intent);
		}
	}

	public static void onAllThreadsDelete(Context context) {
		Intent intent = new Intent();
		intent.setAction(ACTION_MEDIA_SYNC);
		intent.putExtra("event", EVENT_MEDIA_SYNC);
		intent.putExtra("type", EVENT_ALL_DELETE);
		context.startService(intent);
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MediaSyncHelper.class, "===>> onAllThreadsDelete, intent = " + intent);
		}
	}

	public static void onSMSDelete(Context context, long messageId) {
		Intent intent = new Intent();
		intent.setAction(ACTION_MEDIA_SYNC);
		intent.putExtra("event", EVENT_MEDIA_SYNC);
		intent.putExtra("type", EVENT_SMS_DELETE);
		intent.putExtra("payload", messageId);
		context.startService(intent);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MediaSyncHelper.class, "===>> onSMSDelete, intent = " + intent);
		}
	}

	public static void onMMSDelete(Context context, long messageId) {
		Intent intent = new Intent();
		intent.setAction(ACTION_MEDIA_SYNC);
		intent.putExtra("event", EVENT_MEDIA_SYNC);
		intent.putExtra("type", EVENT_MMS_DELETE);
		intent.putExtra("payload", messageId);
		context.startService(intent);

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MediaSyncHelper.class, "===>> onMMSDelete, intent = " + intent);
		}
	}
	
	public static String join(Long... list) {
		StringBuilder b = new StringBuilder();
		for (Long item : list) {
			b.append(separator).append(item);
		}
		if (b.length() > 0) {
			return b.toString().substring(separator.length());
		}

		return "";
	}
	
	public static List<Long> split(String content) {
		List<Long> list = new ArrayList<Long>();
		if (null == content || content.length() == 0) {
			return list;
		}
		try {
			String[] arr = content.split(separator);
			for (String a : arr) {
				list.add(Long.parseLong(a.trim()));
			}
		} catch (Exception e) {
			Logger.error(MediaSyncHelper.class, "Error", e);
		}
		return list;
	}

    /**
     * This Method 
     * @param msgUri
     */
    public static void onMessageDelete(Context context ,Uri msgUri) {
        if(VZUris.isSmsUri(msgUri)){
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(MediaSyncHelper.class, "update media cache:onSMSDelete=uri"+msgUri);
            }
            onSMSDelete(context, ContentUris.parseId(msgUri));
        }else if(VZUris.isMmsUri(msgUri)){
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(MediaSyncHelper.class, "update media cache:onMMSDelete=uri"+msgUri);
            }
            onMMSDelete(context, ContentUris.parseId(msgUri));
        }
    }

}
