package com.verizon.mms;

import java.util.Map;

import android.content.Context;
import android.database.Cursor;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.util.SqliteWrapper;

/**
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * @Since Mar 28, 2012
 */
public class SmsScanner extends AbstractScanner {

    // Projection
    static final String[] projection = new String[] { "_id", "thread_id", "address", "date", "read", "type",
            "body" };
    static final String M_CT = "text/plain";

    // private static final Uri SMS_URI = Uri.parse("content://vzm-sms");
    private final Context ctx;

    public SmsScanner(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    int getNumberOfMessages(long thread) {
        int count = 0;
        // Query (sort by _id, that is process the latest ones first)
        Cursor cursor = SqliteWrapper.query(ctx, VZUris.getSmsUri(), new String[] { "_id" }, "thread_id = "
                + thread, null, null);
        if (null != cursor) {
            count = cursor.getCount();
            cursor.close();
        }

        return count;
    }

    @Override
    void delete(long id) {
        // First, delete this thread
        String where = MediaProvider.Helper.M_ID + " = " + id + " AND " + MediaProvider.Helper.M_TYPE
                + " <= 2";

        int count = SqliteWrapper.delete(ctx, MediaSyncService.CACHE_URI, where, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "==> delete, where=" + where + ", result=" + count);
        }
    }

    @Override
    void scan(String selection, Emitter emitter) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "SMS #==> Querying db=" + VZUris.getSmsUri() + " selection=" + selection);
        }
        // Query (sort by _id, that is process the latest ones first)
        Cursor cursor = SqliteWrapper.query(ctx, VZUris.getSmsUri(), projection, selection, null, "_id");

        if (cursor == null) {
            Logger.error(getClass(), "#==> Cursor null in scan(" + selection + ")");
            return;
        }
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "SMS #==> Querying count=" + cursor.getCount());
        }
        if (cursor.moveToFirst()) {
            do {
                int threadId = cursor.getInt(cursor.getColumnIndex("thread_id"));
                int sid = cursor.getInt(cursor.getColumnIndex("_id"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                int read = cursor.getInt(cursor.getColumnIndex("read"));
                String address = cursor.getString(cursor.getColumnIndex("address"));
                long date = cursor.getLong(cursor.getColumnIndex("date")); // date
                int type = cursor.getInt(cursor.getColumnIndex("type"));
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("#> _id=" + sid + ", thread=" + threadId + ", body=" + body);
                }
                Map<String, String> map = extractUris(body);
                int count = 0;
                for (String key : map.keySet()) {
                    String mPartUri = key;
                    String mPartCt = map.get(key);
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("\t> mPartUri=" + mPartUri + ", mPartCt=" + mPartCt);
                    }
                    Media media = new Media(threadId, sid, count++, address, M_CT, mPartCt, mPartUri, type,
                            read, date, 0, 0);
                    // emit
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug(getClass(), "#> _id=" + sid + " thread=" + threadId + " address="
                                + address);
                    }
                    emitter.emit(media);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
    }
}