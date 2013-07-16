package com.verizon.mms;

import static com.verizon.mms.MediaProvider.Helper.THREAD_ID;
import static com.verizon.mms.MediaSyncHelper.EVENT_ALL_DELETE;
import static com.verizon.mms.MediaSyncHelper.EVENT_MEDIA_SYNC;
import static com.verizon.mms.MediaSyncHelper.EVENT_MMS_DELETE;
import static com.verizon.mms.MediaSyncHelper.EVENT_SMS_DELETE;
import static com.verizon.mms.MediaSyncHelper.EVENT_THREAD_DELETE;
import static com.verizon.mms.MediaProvider.Helper.toContentValues;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.mms.util.SqliteWrapper;

/**
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * 
 */
public class MediaSyncService extends Service {

    public static final String THREAD_INFO_CT = "cache/thread-info";
    public static final String THREAD_INFO_FORMAT = "T%d-C%d-L%d-H%s";
    public static final long INVALID = -1L;

    // URIs
    public static final Uri CACHE_URI = Uri.parse("content://verizon-cache/media");
    // Sleep for 1 seconds
    static final int SLEEP_FOR = 5000;
    // Sleep after 100 elements
    static final int SLEEP_AFTER = 200;

    private Context ctx;
    private SmsScanner smsScanner;
    private MmsScanner mmsScanner;
    private ThreadPoolExecutor executor;
    private MediaCacheApi.Stub syncApiEndpoint;
    private final Map<Long, List<Callback>> callbackMap = new ConcurrentHashMap<Long, List<Callback>>(10, 1,
            2);
    // http://developer.android.com/reference/java/util/concurrent/CopyOnWriteArrayList.html
    // http://stackoverflow.com/questions/2950871/how-can-copyonwritearraylist-be-thread-safe
    private final List<AccCallback> accCallbackList = new CopyOnWriteArrayList<AccCallback>();// new
                                                                                              // ArrayList<AccCallback>();//Collections.synchronizedList(new
                                                                                              // ArrayList<AccCallback>());
    private Handler serviceHandler;
    private Handler updateHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        ctx = this;

        updateHandler = new Handler();
        serviceHandler = new ServiceHandler();

        // scanners
        smsScanner = new SmsScanner(ctx);
        mmsScanner = new MmsScanner(ctx);

        // executors
        executor = new ThreadPoolExecutor(1, // default pool size
                1, // max pool size (1 extra)
                10, // timeout (how long a thread should wait before getting
                    // expired
                TimeUnit.SECONDS, // in seconds
            	new LinkedBlockingQueue<Runnable>(), // Requests to be queued - unbounded
                new ThreadFactory() {
                    @Override
                    public Thread newThread(final Runnable runnable) {
                        Thread thread = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    Looper.prepare();
                                } catch (Throwable th) {
                                }
                                // Run at lowest
                                android.os.Process
                                        .setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                                runnable.run();

                                try {
                                    Looper.loop();
                                } catch (Throwable th) {
                                }
                            }
                        });
                        return thread;
                    }
                });

        syncApiEndpoint = new MediaCacheApi.Stub() {
            @Override
            public void cache(long thread, Callback cb) throws RemoteException {
                queueCacheRequest(thread, cb, true);
            }

            @Override
            public void cacheWithoutClear(long thread, Callback cb) throws RemoteException {
                queueCacheRequest(thread, cb, false);
            }

            @Override
            public void accCache(AccCallback cb) throws RemoteException {
                queueAccCacheRequest(cb);
            }

            @Override
            public void unregisterCallback(long thread) throws RemoteException {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(), "#==> Unregistering callbacks for thread=" + thread);
                }
                callbackMap.remove(thread);
            }

            @Override
            public void cacheMms(final long thread, final long mms, final Callback cb) throws RemoteException {

                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug("==>> cacheMms");
                        }

                        try {
                            // Operations
                            final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            // Emitter to collect all operations
                            AbstractScanner.Emitter emitter = new AbstractScanner.Emitter() {
                                @Override
                                public void emit(Media media) {
                                    ContentValues cv = toContentValues(media);
                                    operations.add(ContentProviderOperation.newInsert(CACHE_URI)
                                            .withValues(cv).build());
                                }
                            };

                            String selection = "thread_id = " + thread + " AND _id = " + mms;
                            mmsScanner.scan(selection, emitter);

                            // Apply operations
                            final long result = applyBatchOperations(operations);

                            updateHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        cb.onComplete(result);
                                    } catch (RemoteException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            });

                        } catch (Exception e) {
                            Logger.error(getClass(), "error in doCacheThread(). error=", e);

                            updateHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        cb.onComplete(-1);
                                    } catch (RemoteException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        };

    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "===>>> onStartCommand, intent=" + intent);
        }

        Bundle bundle = intent.getExtras();
        if (null != bundle && bundle.getString("event").equals(EVENT_MEDIA_SYNC)) {
            int what = bundle.getInt("type");
            Object payload = bundle.get("payload");

            Message msg = serviceHandler.obtainMessage(what);
            msg.obj = payload;
            serviceHandler.sendMessage(msg);
        }

        final int rtn = Service.START_NOT_STICKY;
        return rtn;
    }

    class ServiceHandler extends Handler {
        public void handleMessage(final Message msg) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(MediaSyncService.class, "==>> ServiceHandler ==>> handle message ==> " + msg);
            }
            switch (msg.what) {
            case EVENT_ALL_DELETE: {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(), "==>> Preparing to delete everyting");
                        }
                        deleteAll();
                    }
                });

                break;
            }
            case EVENT_THREAD_DELETE: {
                final List<Long> list = MediaSyncHelper.split((String) msg.obj);
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(), "==>> Preparing to delete THREAD, ids=" + list);
                        }
                        deleteThreads(list);
                    }
                });
                break;
            }
            case EVENT_SMS_DELETE: {
                final long id = (Long) msg.obj;
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(), "==>> Preparing to delete SMS, id=" + id);
                        }
                        smsScanner.delete(id);
                    }
                });
                break;
            }
            case EVENT_MMS_DELETE: {
                final long id = (Long) msg.obj;
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(getClass(), "==>> Preparing to delete MMS, id=" + id);
                        }
                        mmsScanner.delete(id);
                    }
                });
                break;
            }
            default:
                break;
            }
        }
    }

    private void deleteThreads(List<Long> threadIds) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "==> deleteThreads, threadIds=" + threadIds);
        }

        // Operations
        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        for (long thread : threadIds) {
            // First, delete this thread
            String where = THREAD_ID + " = " + thread;
            operations.add(ContentProviderOperation.newDelete(CACHE_URI).withSelection(where, null).build());
        }

        if (!operations.isEmpty()) {
            try {
                applyBatchOperations(operations);
            } catch (Exception e) {
                Logger.error(getClass(), "error in deleteThreads, e=", e);
            }
        }
    }

    void deleteAll() {
        int count = SqliteWrapper.delete(ctx, MediaSyncService.CACHE_URI, null, null);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "==> deleteAll, where=" + null + ", result=" + count);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(MediaSyncService.class, "#============>>> Going Down!");
        }
        if (null != executor) {
            executor.shutdownNow();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (MediaSyncService.class.getName().equals(intent.getAction())) {
            return syncApiEndpoint;
        } else {
            return null;
        }
    }

    private void queueCacheRequest(final long thread, final Callback callback, final boolean clearAll)
            throws RemoteException {
        List<Callback> callbacks = callbackMap.get(thread);
        if (null != callbacks) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "#==> Thread=" + thread
                        + "is already Queued for cache. Will only register your callback!");
            }
            callbacks.add(callback);
            callbackMap.put(thread, callbacks);
        } else {
            callbacks = new ArrayList<Callback>();
            callbacks.add(callback);
            callbackMap.put(thread, callbacks);
            doCacheThread(thread, clearAll);
        }
    }

    private void queueAccCacheRequest(final AccCallback callback) throws RemoteException {

        if (!accCallbackList.isEmpty()) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(),
                        "#==> Full sync is already in progres. Will only register your callback!");
            }
            accCallbackList.add(callback);
        } else {
            accCallbackList.add(callback);
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "#==> Adding callback class= " + callback + ", size="
                        + accCallbackList.size());
            }
            doCacheAccumulative();
        }
    }

    private void notifyCallback(final long thread, final long count) {
        // Callback
        updateHandler.post(new Runnable() {
            @Override
            public void run() {
                List<Callback> cbs = callbackMap.get(thread);
                if (null != cbs) {
                    callbackMap.remove(thread);
                    for (Callback cb : cbs) {
                        try {
                            cb.onComplete(count);
                        } catch (Exception e) {
                            if (Logger.IS_ERROR_ENABLED) {
                                Logger.error(getClass(), "error in notifyCallback(). error=", e);
                            }
                        }
                    }
                }
            }
        });
    }

    private void notifyAccCallback(final long thread, final long count, final Iterator<AccCallback> itr) {
        // Callback
        updateHandler.post(new Runnable() {

            @Override
            public void run() {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(getClass(),
                            "==>> notifyAccCallback, accCallbackList=" + accCallbackList.size() + ", itr="
                                    + itr);
                }
                // List<AccCallback> copy = new
                // ArrayList<AccCallback>(accCallbackList);
                // Collections.copy(copy, accCallbackList);
                // Iterator<AccCallback> itr = accCallbackList.iterator();
                if (null != itr) {
                    while (itr.hasNext()) {
                        try {
                            AccCallback cb = itr.next();
                            cb.onComplete(thread, count);
                        } catch (Throwable e) {
                            Logger.error(getClass(), "error in notifyAccCallback(). error=", e);
                        }
                    }
                }
                // for (AccCallback cb : accCallbackList) {
                // try {
                // cb.onComplete(thread, count);
                // } catch (Throwable e) {
                // Logger.error(getClass(),
                // "error in notifyAccCallback(). error=", e);
                // }
                // }

            }
        });

    }

    /**
     * This Method caches all message(s) from a given conversation atomically.
     * 
     * @param thread
     *            conversation id
     * @param cb
     *            {@link Callback} callback
     */
    private void doCacheThread(final Long thread, final boolean clearAll) {
        // assert
        isNotNull(thread);

        // execute
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("==>> doCacheThread");
                }

                try {
                    long count = performThreadCache(thread, clearAll);
                    notifyCallback(thread, count);
                } catch (Exception e) {
                    Logger.error(getClass(), "error in doCacheThread(). error=", e);
                }
            }
        });
    }

    private void doCacheAccumulative() {
        // execute
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final long now = new Date().getTime();
                long lastAccumulativeCacheTime = 0L;

                // Let's always evaluate each thread for re-caching
                // // get last sync info
                // String threadInfo = getThreadInfoFromCache(-1L);
                // if (null != threadInfo) {
                // lastAccumulativeCacheTime = Long.parseLong(threadInfo);
                // }

                // Get all threads
                // Projection
                final String[] projection = new String[] { "_id", "date", "message_count" };
                final Uri sAllThreadsUri = VZUris.getMmsSmsConversationUri().buildUpon()
                        .appendQueryParameter("simple", "true").build();
                final String where = "date > " + lastAccumulativeCacheTime;

                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug(MediaSyncService.class, "==> Last FMSB sync time = "
                            + lastAccumulativeCacheTime);
                }

                List<Long> threadIds = new ArrayList<Long>();
                Cursor cursor = null;
                try {
                    cursor = SqliteWrapper.query(ctx, sAllThreadsUri, projection, where, null, "date DESC");

                    if (null != cursor) {
                        if (Logger.IS_DEBUG_ENABLED) {
                            Logger.debug(MediaSyncService.class, "==> Found " + cursor.getCount()
                                    + " threads updated after time = " + lastAccumulativeCacheTime);
                        }

                        if (cursor.moveToFirst()) {
                            do {
                                long thread = cursor.getLong(cursor.getColumnIndex("_id"));
                                long date = cursor.getLong(cursor.getColumnIndex("date"));
                                String messageCount = cursor.getString(cursor.getColumnIndex("message_count"));
                                if (Logger.IS_DEBUG_ENABLED) {
                                    Logger.debug(getClass(), "===>>> Thread=" + thread + ", date=" + date
                                            + ", count=" + messageCount);
                                }
                                threadIds.add(thread);
                            } while (cursor.moveToNext());
                        }
                    }
                } finally {
                    if (null != cursor) {
                        cursor.close();
                    }
                }

                // Cache all threads
                for (Long thradId : threadIds) {
                    try {
                        long count = performThreadCache(thradId);
                        notifyAccCallback(thradId, count, accCallbackList.listIterator());
                        notifyCallback(thradId, count);
                    } catch (Exception e) {
                        Logger.error(getClass(), "error in doCacheThread(). error=", e);
                    }
                }

                // Let's always evaluate each thread for re-caching
                // set thread info
                // if (!threadIds.isEmpty()) {
                // threadInfo = now + "";
                // setThreadInfoToCache(INVALID, threadInfo);
                // }

                // End
                notifyAccCallback(INVALID, INVALID, accCallbackList.listIterator());
                accCallbackList.clear();
            }
        });
    }

    private Long performThreadCache(final Long thread) throws RemoteException, OperationApplicationException {
        return performThreadCache(thread, true);
    }

    private Long performThreadCache(final Long thread, final boolean clearAll) throws RemoteException,
            OperationApplicationException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "#==> Evaluating thread=" + thread + " for caching");
        }
        String threadInfo = compareAndGetLatestThreadInfo(thread);
        if (null == threadInfo) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "#==> Thread=" + thread + " is already UP-TO-DATE! why cache? :) ");
            }
            notifyCallback(thread, 0L);
            return 0L;
        }

        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "threadInfo=" + threadInfo);
        }
        // Operations
        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        // First, delete this thread
        if (clearAll) {
            String where = THREAD_ID + " = " + thread;
            operations.add(ContentProviderOperation.newDelete(CACHE_URI).withSelection(where, null).build());
        }
        // Add threadInfo
        Media threadInfoMedia = new Media(thread.intValue(), -1, -1, null, THREAD_INFO_CT, null, threadInfo,
                -1, -1, new Date().getTime(), 0, 0);
        operations.add(ContentProviderOperation.newInsert(CACHE_URI)
                .withValues(toContentValues(threadInfoMedia)).build());

        final AtomicLong count = new AtomicLong(0);
        // Emitter to collect all operations
        AbstractScanner.Emitter emitter = new AbstractScanner.Emitter() {
            @Override
            public void emit(Media media) {
                ContentValues cv = toContentValues(media);
                operations.add(ContentProviderOperation.newInsert(CACHE_URI).withValues(cv).build());

                if (operations.size() == 5) {
                    try {
                        count.set(applyBatchOperations(operations));
                        operations.clear();
                    } catch (Exception e) {
                    }
                }
            }
        };

        String selection = "thread_id = " + thread;
        mmsScanner.scan(selection, emitter);
        smsScanner.scan(selection, emitter);

        // Apply operations
        count.set(applyBatchOperations(operations));
        return count.get();
    }

    private void setThreadInfoToCache(Long thread, String threadInfo) {
        // Add threadInfo
        Media threadInfoMedia = new Media(thread.intValue(), -1, -1, null, THREAD_INFO_CT, null, threadInfo,
                -1, -1, new Date().getTime(), 0, 0);
        // Operations
        final ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        // First, delete this thread
        operations.add(ContentProviderOperation.newDelete(CACHE_URI)
                .withSelection(THREAD_ID + " = " + -1, null).build());
        // add
        operations.add(ContentProviderOperation.newInsert(CACHE_URI)
                .withValues(toContentValues(threadInfoMedia)).build());
        try {
            applyBatchOperations(operations);
        } catch (Exception e) {
            Logger.error(MediaSyncService.class, "error in storing thread info for thread = -1", e);
        }
    }

    private String getThreadInfoFromCache(long thread) {
        String threadInfo = null;
        Cursor cacheDb = SqliteWrapper.query(ctx, CACHE_URI, new String[] { MediaProvider.Helper.TEXT,
                MediaProvider.Helper.M_CT }, MediaProvider.Helper.THREAD_ID + " = " + thread + " AND "
                + MediaProvider.Helper.M_CT + " = '" + THREAD_INFO_CT + "'", null, null);
        if (null != cacheDb) {
            if (cacheDb.moveToFirst()) {
                do {
                    threadInfo = cacheDb.getString(0);
                } while (cacheDb.moveToNext());
            }
            cacheDb.close();
        }
        return threadInfo;
    }

    private boolean hasThreadChanged(String fromCache, long thread, Cursor query) {
        if (null != fromCache && query.moveToLast()) {
            // "T%d-C%d-L%d-H%s";
            String temp = String.format(THREAD_INFO_FORMAT, thread, query.getCount(),
                    query.getInt(query.getColumnIndex("_id")), "");
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "fromCache starts with=" + temp);
            }
            if (fromCache.startsWith(temp)) {
                return false;
            }
        }

        return true;
    }

    private String compareAndGetLatestThreadInfo(long thread) {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "#==> compareAndGetLatestThreadInfo, thread=" + thread);
        }
        // Is there anything info in cache?
        final String fromCache = getThreadInfoFromCache(thread);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "fromCache=" + fromCache);
        }
        Cursor query = null;
        try {
            // From conversation
            query = SqliteWrapper.query(ctx,
                    Uri.withAppendedPath(VZUris.getMmsSmsConversationUri(), Long.toString(thread)),
                    new String[] { "_id", "date" }, null, null, null);
            // If there is no info, chances are the thread is gone! So, go
            // re-cache
            // it, which will just
            // remove everything.
            if (null == query) {
                // "T%d-C%d-L%d-H%s";
                return String.format(THREAD_INFO_FORMAT, thread, -1, -1, "NA");
            }

            // If no change then go on live your life!
            if (!hasThreadChanged(fromCache, thread, query)) {
                return null;
            }

            int count = query.getCount();
            long hash = 0L;
            int id = 0;
            if (query.moveToFirst()) {
                do {
                    id = query.getInt(0);
                    long date = query.getLong(1);
                    if (Logger.IS_INFO_ENABLED) {
                        Logger.info(getClass(), "id=" + id + "date=" + date);
                    }
                    hash = hash ^ (id + date);
                } while (query.moveToNext());
            }
            // "T%d-C%d-L%d-H%s";
            String fromConv = String.format(THREAD_INFO_FORMAT, thread, count, id, Long.toString(hash));
            // If they match?
            if (fromConv.equals(fromCache)) {
                return null;
            }

            return fromConv;
        } catch (Exception e) {
            Logger.error(e);
        } finally {
            if (query != null) {
                query.close();
            }
        }

        return null;
    }

    private void isNotNull(Long obj) {
        if (obj == null || obj == INVALID) {
            throw new IllegalArgumentException("illegal null");
        }
    }

    private long applyBatchOperations(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException, RemoteException {
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(),
                    "#==> Performing " + CACHE_URI + " Batch DB Operations" + operations.size());
        }
        if (operations.isEmpty()) {
            return 0;
        }

        ContentProviderResult[] results = getContentResolver()
                .applyBatch(MediaProvider.AUTHORITY, operations);
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "Transaction Reuslts");
        }
        for (ContentProviderResult result : results) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "> uri=" + result.uri + " count=" + result.count);
            }
        }

        return results.length;

    }
}