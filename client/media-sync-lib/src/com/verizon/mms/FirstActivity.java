package com.verizon.mms;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;

/**
 * @author animeshkumar
 * 
 */
public class FirstActivity extends Activity {

    private Context ctx;
//    private MediaCacheApi api;
    private final ServiceConnection serviceConnection = new MediaServiceConnection();

    class MediaServiceConnection implements ServiceConnection {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "#==> onServiceDisconnected, name=" + name);
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (Logger.IS_DEBUG_ENABLED) {
                Logger.debug(getClass(), "#==> onServiceConnected, name=" + name + "service=" + service);
            }
            // api = MediaCacheApi.Stub.asInterface(service);
            // try {
            // api.cacheMms(1, new Callback.Stub() {
            // @Override
            // public void onComplete(long count) throws RemoteException {
            // log.info("#==> mms count={}", count);
            // }
            // });
            // api.cacheSms(1, new Callback.Stub() {
            // @Override
            // public void onComplete(long count) throws RemoteException {
            // log.info("#==> sms count={}", count);
            // }
            // });
            // api.removeMms(1, new Callback.Stub() {
            // @Override
            // public void onComplete(long count) throws RemoteException {
            // log.info("#==> remove mms count={}", count);
            // }
            // });

            // } catch (RemoteException e) {
            // // TODO Auto-generated catch block
            // e.printStackTrace();
            // }
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        String text = "Hey: http://animesh.org, smile.animesh@gmail.com, +91-99778899889";
        if (Logger.IS_DEBUG_ENABLED) {
            Logger.debug(getClass(), "#===============>" + (SmsScanner.extractUris(text)));
        }
        // start service
        final Intent service = new Intent(MediaSyncService.class.getName());
        startService(service);

        // final Intent receiver = new Intent(MediaSyncReceiver.CACHE_THREAD);
        // receiver.putExtra(MediaSyncReceiver.THREAD, 1);
        // sendBroadcast(receiver);

        // intent.putExtra(MediaSyncService.EAGER_CACHE, true);
        // startService(intent);

        //
        // // bind to service
        // bindService(service, serviceConnection, 0);

        // query
        // Cursor q = ctx.getContentResolver().query(Uri.parse("content://mms-sms/conversations"),
        // new String[] { "_id", "thread_id" }, null, null, null);
        // if (q.moveToFirst()) {
        // do {
        // log.info("#-------------------");
        // for (int i = 0; i < q.getColumnCount(); i++) {
        // log.info("===> {} ==> {}", q.getColumnName(i), q.getString(i));
        // }
        // } while (q.moveToNext());
        // }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first);
        ctx = this;
    }

}