package com.verizon.mms.ui;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ListView;

import com.rocketmobile.asimov.Asimov;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.MediaProvider;
import com.verizon.mms.MmsConfig;
import com.verizon.mms.ui.adapter.ContactInfoAdapter;
import com.verizon.mms.util.UtilClass;

public class ContactInfoActivity extends VZMActivity {
	public static final long INVALID = -1L;
    private ContactInfoAdapter cia;
    private Cursor             query;
    private ListView           rawContacts;
    private ContentObserver observer;
    
    private static final int    RE_QUERY = 1001;
    private Handler handler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
        setContentView(R.layout.contactinfoscreen);

        query();
    	cia = new ContactInfoAdapter(this, query);
        rawContacts = (ListView) findViewById(R.id.contactinfolist);
        rawContacts.setAdapter(cia);
        rawContacts.invalidate();
        
        // observe
        observer = new CacheObserver(new Handler());
        getContentResolver().registerContentObserver(Uri.parse(UtilClass.MEDIA_URI), false, observer);

        
        // Handler
        handler = new Handler() {
            public void handleMessage(Message msg) {
                switch(msg.what) {
                case RE_QUERY: {
                	// re-query
                	query();
                	cia.changeCursor(query);
                	cia.notifyDataSetChanged();
                	rawContacts.invalidate();
                }
                default:
                	break;
                }
            }
        };
    }
    
    private void query() {
    	long thread = getIntent().getLongExtra("threadid", INVALID);
    	String selectionForThread = (thread != INVALID ) ? MediaProvider.Helper.THREAD_ID + " = " + thread + " AND " : "";
    	query = getContentResolver().query(
                Uri.parse(UtilClass.MEDIA_URI),
                null,
                selectionForThread + " ("
                        + MediaProvider.Helper.M_PART_CT + "='text/mailto' OR "
                		+ MediaProvider.Helper.M_PART_CT + "='text/namecard' OR "
                        + MediaProvider.Helper.M_PART_CT + "=" + "'text/tel')", null, "date desc");
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        // observer
        if (null != observer) {
            getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }
        
        // close cursor
        if (null != query) {
            query.close();
            query = null;
        }
    }


    /**
     * Observer
     * 
     * @author "Animesh Kumar <animesh@strumsoft.com>"
     * @Since Apr 30, 2012
     */
    class CacheObserver extends ContentObserver {
        public CacheObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(getClass(), "=> Dataset Changed, updating view!" );
        	}
        	
			if (!handler.hasMessages(RE_QUERY)) {
				Message requery = handler.obtainMessage(RE_QUERY);
				handler.sendMessageDelayed(requery, MmsConfig.getFMSBInterval());
			}
			
        }
    }

}
