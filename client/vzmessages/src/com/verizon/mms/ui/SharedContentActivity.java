package com.verizon.mms.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.AccCallback;
import com.verizon.mms.Callback;
import com.verizon.mms.ContentType;
import com.verizon.mms.Media;
import com.verizon.mms.MediaCacheApi;
import com.verizon.mms.MediaProvider;
import com.verizon.mms.MediaSyncService;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.util.UtilClass;

public class SharedContentActivity extends VZMActivity implements OnClickListener {
	public static final long INVALID = -1L;
	
    public static String photoVideoSelect = "(" + MediaProvider.Helper.M_PART_CT
            + " = " + "'"+ContentType.IMAGE_PNG+"'" + " OR " + MediaProvider.Helper.M_PART_CT + " = "
            + "'"+ContentType.IMAGE_JPEG+"'" + " OR " + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_JPG+"'"
            + " OR " + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_GIF+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_BMP+"'" + " OR "
          //bug#3433: added ms-bmp,wbmp,svg file content types as well.
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_MBMP+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_WBMP+"'" + " OR "
	        + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_SVG_XML+"'" + " OR "
//            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_SVG+"'" + " OR "
//            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_UNSPECIFIED+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_MP4ES+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_MP4+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_MPEG+"'" + " OR " 
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_QUICKTIME+"'" + " OR " 
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_3G2+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_H2632000+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_H264+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_H263+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_UNSPECIFIED+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_3GPP+"')";
    
    public static String photoVideoLocationSelect = "(" + MediaProvider.Helper.M_PART_CT
            + " = " + "'"+ContentType.IMAGE_PNG+"'" + " OR " + MediaProvider.Helper.M_PART_CT + " = "
            + "'"+ContentType.IMAGE_JPEG+"'" + " OR " + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_JPG+"'"
            + " OR " + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_GIF+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_BMP+"'" + " OR "
            //bug#3433: added ms-bmp,wbmp,svg file content types as well.
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_MBMP+"'" + " OR "
	        + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_SVG_XML+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_WBMP+"'" + " OR "
//            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_SVG+"'" + " OR "
//            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.IMAGE_UNSPECIFIED+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_MP4ES+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_MP4+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_MPEG+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_QUICKTIME+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_3G2+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_H2632000+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_H264+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_H263+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+Media.M_LOCATION_CT+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+Media.M_LOCATION_CT2+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_UNSPECIFIED+"'" + " OR "
            + MediaProvider.Helper.M_PART_CT + " = " + "'"+ContentType.VIDEO_3GPP+"')";
    
    private int numberOfPhotos = 0;
    private int numberOfLinks = 0;
    private int numberOfContacts = 0;
    private int numberOfLocations = 0;
    private long thread;
    private String[] members;
    private Handler handler;
    private MediaServiceConnection serviceConnection;
    boolean onFirstRunAnimation = false;
    boolean isLoadedOnce = false;
    boolean hasContentViewSet = false;
    boolean hasFetchingViewSet = false;
    boolean onFirstRunLoading = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
        handler = new Handler();
        thread = getIntent().getLongExtra("threadid", INVALID);
        members = getIntent().getStringArrayExtra("members");
        if (null == members) {
        	members = new String[] {getString(R.string.gallery_all)};
        }
        // FIXME: throw exception if thread == null //thread can't be null
        onFirstRunLoading = true;
        initData();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	
    	onFirstRunAnimation = true;
    	updateView();
    	// MediaSync Service
        serviceConnection = new MediaServiceConnection(thread);
        startService(new Intent(MediaSyncService.class.getName()));
        //boolean bindService = 
    	bindService(new Intent(MediaSyncService.class.getName()), serviceConnection, 0);
    }

    @Override
    public void onPause() {
    	super.onPause();
    	
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
    
    @Override
    public void onStop() {
    	super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.row1layout:
            if (numberOfPhotos != 0) {
                Intent i = new Intent(this, GalleryActivity.class).putExtra("threadid", thread);
                startActivity(i);
            }
            break;
        case R.id.row2layout:
            if (numberOfLinks != 0) {
                Intent i2 = new Intent(this, SharedLinksActivity.class).putExtra("threadid", thread);
                startActivity(i2);
            }
            break;
        case R.id.row3layout:
        	if (numberOfLocations != 0) {
                Intent i3 = new Intent(this, SharedLocationActivity.class).putExtra("threadid", thread);
                startActivity(i3);
        	}
            break;
        case R.id.row4layout:
            if (numberOfContacts != 0) {
                Intent i4 = new Intent(this, ContactInfoActivity.class).putExtra("threadid", thread);
                startActivity(i4);
            }
            break;
        case R.id.startsharing:
            setResult(RESULT_OK);
            finish();
            break;
        }
    }

    void initView() {
        initData();
        updateView();
    }

    private void updateView() {
    	String membersConcated = "";
        for (int i = 0; i < members.length; i++) {
        	membersConcated += members[i] + ";";
        }
        ContactList contacts = ContactList.getByNumbers(membersConcated, false, true);
        if (numberOfContacts > 0 || numberOfPhotos > 0 || numberOfLinks > 0 || numberOfLocations > 0) {
            // Shared Content Screen
        	if (!hasContentViewSet) {
        		setContentView(R.layout.sharedcontentscreen);
        	}
        	hasContentViewSet = true;
        	hasFetchingViewSet = false;
        	
            RelativeLayout row1 = (RelativeLayout) findViewById(R.id.row1layout);
            RelativeLayout row2 = (RelativeLayout) findViewById(R.id.row2layout);
            RelativeLayout row3 = (RelativeLayout) findViewById(R.id.row3layout);
            RelativeLayout row4 = (RelativeLayout) findViewById(R.id.row4layout);
            row4.setOnClickListener(this);
            row3.setOnClickListener(this);
            row2.setOnClickListener(this);
            row1.setOnClickListener(this);

            ImageView photosView = (ImageView) findViewById(R.id.row1icon);
            TextView photosTextView = (TextView) findViewById(R.id.photosandvideostext);
            TextView photosTextCount = (TextView) findViewById(R.id.photosandvideoscount);

            ImageView linksView = (ImageView) findViewById(R.id.row2icon);
            TextView linksTextView = (TextView) findViewById(R.id.linkstext);
            TextView linksTextCount = (TextView) findViewById(R.id.linkscount);

            ImageView locationView = (ImageView) findViewById(R.id.row3icon);
            TextView locationTextView = (TextView) findViewById(R.id.locationtext);
            TextView locationTextCount = (TextView) findViewById(R.id.locationcount);

            ImageView contactsView = (ImageView) findViewById(R.id.row4icon);
            TextView contactsTextView = (TextView) findViewById(R.id.contactinfotext);
            TextView contactsTextCount = (TextView) findViewById(R.id.contactinfocount);
           
           
            if (onFirstRunAnimation) {
                onFirstRunAnimation = false;
                TranslateAnimation anim = new TranslateAnimation(200f, 0f, -100f, 0f);
                anim.setDuration(250);
                photosView.startAnimation(anim);
                photosTextView.startAnimation(anim);
                photosTextCount.startAnimation(anim);

                TranslateAnimation anim1 = new TranslateAnimation(200f, 0f, -100f, 0f);
                anim1.setDuration(500);
                linksView.startAnimation(anim1);
                linksTextView.startAnimation(anim1);
                linksTextCount.startAnimation(anim1);

                TranslateAnimation anim2 = new TranslateAnimation(200f, 0f, -100f, 0f);
                anim2.setDuration(750);
                locationView.startAnimation(anim2);
                locationTextView.startAnimation(anim2);
                locationTextCount.startAnimation(anim2);

                TranslateAnimation anim3 = new TranslateAnimation(200f, 0f, -100f, 0f);
                anim3.setDuration(1000);
                contactsView.startAnimation(anim3);
                contactsTextView.startAnimation(anim3);
                contactsTextCount.startAnimation(anim3);
            }

           

            photosTextCount.setText("(" + numberOfPhotos + ")"); // set photo + video count
            linksTextCount.setText("(" + numberOfLinks + ")"); // set links count            
            locationTextCount.setText("(" + numberOfLocations + ")"); // set photo + video count
            contactsTextCount.setText("(" + numberOfContacts + ")"); // set contact info count      	
        } else {
            // No Shared data, show appropriate screen
            
            if (!hasFetchingViewSet) {
                setContentView(R.layout.sharedcontentscreen);
            }
            hasFetchingViewSet = true;
            hasContentViewSet = false;
            
            setContentView(R.layout.nosharedcontent);
            TextView descView = (TextView) findViewById(R.id.screen_description);
            Button startSharing = (Button) findViewById(R.id.startsharing);
            ProgressBar loadingProgressbar = ( ProgressBar) findViewById(R.id.progress_loading_gallery);
            
            if (onFirstRunLoading) {
                startSharing.setVisibility(View.GONE);
                loadingProgressbar.setVisibility(View.VISIBLE);
                descView.setText(R.string.updating_content);
            } else {
                startSharing.setVisibility(View.VISIBLE);
                loadingProgressbar.setVisibility(View.GONE);
                startSharing.setOnClickListener(this);
                startSharing.setEnabled(true);
                descView.setText(getString(R.string.nosharedcontent, contacts.formatNames()));
            }
            
        }

        // Update title
        FromTextView titleView = (FromTextView) findViewById(R.id.sharedtitletext);
        titleView.setSuffix(getString(R.string.andme));
        titleView.setNames(contacts);
    }

	void showLoadingIcon() {
        ProgressBar progressView = (ProgressBar) findViewById(R.id.progress_sync);
    	progressView.setVisibility(View.VISIBLE);
    }

    void hideLoadingIcon() {
        ProgressBar progressView = (ProgressBar) findViewById(R.id.progress_sync);
        progressView.setVisibility(View.GONE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        onFirstRunAnimation = false;
        super.onConfigurationChanged(newConfig);
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

        public MediaServiceConnection(long thread) {
            this.thread = thread;
        }

        @Override 
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (Logger.IS_DEBUG_ENABLED) {
            	Logger.debug(getClass(), "#====> onServiceConnected, thread=" + this.thread);
            }
            api = MediaCacheApi.Stub.asInterface(service);
            if(Logger.IS_DEBUG_ENABLED){
            Logger.debug(getClass(), "#====> onServiceConnected, api=" + api);
            }
            try {
                // Show Loading..
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showLoadingIcon();
                    }
                });
                if(Logger.IS_DEBUG_ENABLED){
                Logger.debug(getClass(), "#====> onServiceConnected, thread=" + this.thread);
                }
                
                if (this.thread == INVALID) {
	                api.accCache(new AccCallback.Stub() {
	                    @Override
	                    public void onComplete(final long threadId, final long count) throws RemoteException {
	                    	if(Logger.IS_DEBUG_ENABLED){
	                    	Logger.debug(getClass(), "#====> onComplete ==> threadId=" + threadId + ", count=" + count);
	                    	}
	                        // Hide loading
	                        handler.post(new Runnable() {
	                            @Override
	                            public void run() {
	                                // If re-cached, then update the view
                                    if (count > 0 || (onFirstRunLoading && threadId == INVALID)) {
                                        onFirstRunLoading = false;
                                        onFirstRunAnimation = false;
                                        initView();
                                    }
	                                
	                                // If INVALID, then the cache is complete  
	                                if (threadId == INVALID) {
	                                	hideLoadingIcon();
	                                } else {
	                                	showLoadingIcon();
	                                }
	                            }
	                        });
	                    }
	                });
                } else {
                    api.cache(this.thread, new Callback.Stub() {
                        @Override
                        public void onComplete(final long count) throws RemoteException {
                            // Hide loading
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    hideLoadingIcon();
                                    // If re-cached, then update the view
                                    if (count > 0 || onFirstRunLoading) {
                                        onFirstRunLoading = false;
                                        onFirstRunAnimation = false;
                                        initView();
                                    }
                                }
                            });
                        }
                    });
                }
                
            } catch (Exception e) {
                Logger.error(SharedContentActivity.class, e);
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
                } catch (Exception e) {
                    Logger.error(SharedContentActivity.class, e);
                }
            }
        }
    }

    private void initData() {
        // links
        Cursor query;
        String selectionForThread = (thread != INVALID ) ? MediaProvider.Helper.THREAD_ID + " = " + thread + " AND " : "";
        
        query = getContentResolver().query(
                Uri.parse(UtilClass.MEDIA_URI),
                null,
                selectionForThread + MediaProvider.Helper.M_PART_CT
                        + " = " + "'text/link'", null, null);
        if (null != query) {
            numberOfLinks = query.getCount();
            query.close();
        }

        // Photos/Videos
        query = getContentResolver().query(
                Uri.parse(UtilClass.MEDIA_URI),
                null,
                selectionForThread + photoVideoSelect, null, null);

        if (null != query) {
            numberOfPhotos = query.getCount(); 
            query.close();
        }
        
        // Locations
        query = getContentResolver().query(
                Uri.parse(UtilClass.MEDIA_URI),
                null,
                selectionForThread + " (" + MediaProvider.Helper.M_PART_CT
                        + " = 'text/x-vcard' OR " + MediaProvider.Helper.M_PART_CT +
                        " = 'text/x-vCard')", null, null);
        if (null != query) {
            numberOfLocations = query.getCount();
            query.close();
        }

        // Contacts
        query = getContentResolver().query(
                Uri.parse(UtilClass.MEDIA_URI),
                null,
                selectionForThread + " (" + MediaProvider.Helper.M_PART_CT
                        + " = " + "'text/mailto' OR " + MediaProvider.Helper.M_PART_CT + " = 'text/namecard'"
                        + " OR " + MediaProvider.Helper.M_PART_CT + " = "
                        + "'text/tel')", null, null);
        if (null != query) {
            numberOfContacts = query.getCount();
            query.close();
        }
    }
    
}
