package com.verizon.mms.util;

import java.util.Calendar;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.MmsConfig;

public class AsyncQueryHandlerWithMonitor extends AsyncQueryHandler {

	private Handler mHandler;
	
	public AsyncQueryHandlerWithMonitor(Handler mHandler, ContentResolver cr) {
		super(cr);
		this.mHandler = mHandler;
	}
	
	@Override
	public void startQuery(final int token, Object cookie, Uri uri,
			String[] projection, String selection, String[] selectionArgs,
			String orderBy) {

		if (MmsConfig.getMonitorAsyncQuery()) {
			final AsyncQueryHandler self = this;
			final Runnable callback = new Runnable() {
				@Override
				public void run() {
					Logger.debug(getClass(),
							"==> Still not finished token=" + token);
					try {
						// Cancel this operation!
						self.cancelOperation(token);
					} catch (Throwable th) {
					}
					// Exit the app
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							System.exit(0);
						}
					});

				}
			};
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.SECOND, MmsConfig.getMonitorAsyncQueryTimeout());
			// post delayed callback
			postAtTime(callback, token, cal.getTimeInMillis());
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(),
						"==> Posted monitor runnable for token=" + token
								+ ", to be executed at=" + cal.getTime());
			}
		}
		super.startQuery(token, cookie, uri, projection, selection,
				selectionArgs, orderBy);
	}
	
	@Override
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		if (MmsConfig.getMonitorAsyncQuery()) {
			// Remove monitor Runnable objects against this token
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(getClass(),
						"==> Removing callback/message with token=" + token);
			}
			removeCallbacksAndMessages(token);
		}
	}
}
