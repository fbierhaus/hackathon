package com.verizon.mms.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.Callback;
import com.verizon.mms.MediaCacheApi;
import com.verizon.mms.MediaSyncService;
import com.verizon.mms.util.UtilClass;

public class MediaShoeboxInterceptorActivity extends VZMActivity {

	private MediaServiceConnection serviceConnection;
	private Handler handler;
	private ProgressDialog pd;
	private long threadid;
	private Cursor isInDb;
	private Activity activity;
	private boolean alreadyWentToGallery = false; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		activity = this;
		setContentView(new View(this));
		pd = null;
		handler = new Handler();
		threadid = getIntent().getLongExtra("threadid", -1);
		long id = getIntent().getLongExtra("id", -1);
    	String item = getIntent().getStringExtra("itemtogo");
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(GalleryActivity.class, "MediaShoebox Interceptor - tid=" + threadid + " id=" + id + " itemToGo=" + item);
		}
		isInDb = getContentResolver().query(Uri.parse(UtilClass.MEDIA_URI), 
				new String[] {"count(*) AS count"}, 
				"thread_id=" + threadid + " AND m_id=" + id + " AND m_ct != 'plain/text'",
				null, null);
		isInDb.moveToFirst();
		if (isInDb.getInt(isInDb.getColumnIndex("count")) == 1) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(GalleryActivity.class, "MediaShoebox Interceptor going to Gallery");
			}
			goToGallery();
		}
		else {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(GalleryActivity.class, "MediaShoebox Interceptor going to MediaSyncService");
			}
			serviceConnection = new MediaServiceConnection(threadid, id);
	        startService(new Intent(MediaSyncService.class.getName()));
	        bindService(new Intent(MediaSyncService.class.getName()), serviceConnection, 0);
		}
	}
	
//	@Override
//	protected void onStop() {
//		if (Logger.IS_DEBUG_ENABLED) {
//			Logger.debug(GalleryActivity.class, "MediaShoebox Interceptor stopping");
//		}
//		unbindService(serviceConnection);
//		super.onStop();
//	}
		
	
	@Override
    public void onDestroy() {
        super.onDestroy();
        if (null != serviceConnection) {
            serviceConnection.unregisterCallbacks();
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "#====> Trying to unbind with MediaSyncService");
            }
            unbindService(serviceConnection); 
            serviceConnection = null;
        }
        
        stopService(new Intent(MediaSyncService.class.getName()));
    }

	
	void showLoadingIcon() {
        if (pd == null) {
        	pd = new ProgressDialog(this);
        	pd.setMessage(getString(R.string.loading));
        	pd.setCancelable(false);
        	pd.show();
        }
    }

    void hideLoadingIcon() {
    	if (pd != null && pd.isShowing()) {
    		pd.cancel();
    		pd = null;
    	}
    }
    
    void goToGallery() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MediaShoeboxInterceptorActivity.class, "==>> Go To Gallery!");
		}
		
    	if (alreadyWentToGallery) {
    		if (Logger.IS_DEBUG_ENABLED) {
    			Logger.debug(MediaShoeboxInterceptorActivity.class, "==>> Already Went To Gallery!");
    		}
    	}
    	alreadyWentToGallery = true;
		isInDb.close();
    	Intent goToGallery = new Intent(this, GalleryActivity.class);
    	goToGallery.putExtra("itemtogo", getIntent().getStringExtra("itemtogo"));
		goToGallery.putExtra("threadid", threadid);
		goToGallery.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(goToGallery);
		finish();
    }
	
	/**
     * ServiceConnection implementation
     * 
     * @author "Animesh Kumar <animesh@strumsoft.com>"
     * @Since May 4, 2012
     */
    class MediaServiceConnection implements ServiceConnection {
        private MediaCacheApi api;
        private long thread;
        private long mms;

        public MediaServiceConnection(long thread, long mms) {
            this.thread = thread;
            this.mms = mms;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "#====> onServiceConnected");
            }
            
            api = MediaCacheApi.Stub.asInterface(service);
            try {
                // Show Loading..
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showLoadingIcon();
                    }
                });
                // Haven't gone to Gallery Yet!
                
                alreadyWentToGallery = false;
                
                api.cacheMms(thread, mms, new Callback.Stub() {
					@Override
					public void onComplete(final long count)
							throws RemoteException {
						if (activity.isFinishing()) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(MediaShoeboxInterceptorActivity.class,
										"Activity is finishing...");
							}
							return;
						}

						// Hide loading
						handler.post(new Runnable() {
							@Override
							public void run() {
								if (count != -1) {
									hideLoadingIcon();
									// If re-cached, then update the view
									goToGallery();
								}
							}
						});
					}
				});


                api.cacheWithoutClear(thread, new Callback.Stub() {
                    @Override
                    public void onComplete(final long count) throws RemoteException {
                    	if (activity.isFinishing()) {
                    		if (Logger.IS_DEBUG_ENABLED) {
                    			Logger.debug(MediaShoeboxInterceptorActivity.class, "Activity is finishing...");
                    		}
                    		return;
                    	}
                        // Hide loading
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                hideLoadingIcon();
                                // If re-cached, then update the view
                                goToGallery();
                            }
                        });
                    }
                });
                
            } catch (Exception e) {
                Logger.error(MediaShoeboxInterceptorActivity.class, e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "#====> onServiceDisconnected");
            }

        	unregisterCallbacks();
            api = null;
        }

        public void unregisterCallbacks() {
            if (null != api) {
                try {
                    api.unregisterCallback(thread);
                	hideLoadingIcon();
                	stopService(new Intent(MediaSyncService.class.getName()));
                } catch (Exception e) {
                    Logger.error(MediaShoeboxInterceptorActivity.class, e);
                }
            }
        }
    }
	
}
