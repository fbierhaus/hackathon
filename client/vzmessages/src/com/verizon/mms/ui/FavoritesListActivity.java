/**
 * FavouriteslistActivity.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.rocketmobile.asimov.Asimov;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.AddressProvider.AddressHelper;
import com.verizon.mms.ui.adapter.PlacesListAdapter;

/**
 * This class/interface
 * 
 * @author Imthiaz
 * @Since Apr 12, 2012
 */
public class FavoritesListActivity extends VZMListActivity {

    private PlacesListAdapter pLA;
    private Cursor            cursor;
    // URIs
    public static final Uri   CACHE_URI = Uri.parse("content://address-cache/address");

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.places_list);

    }

    @Override
    public void onResume() {
        super.onResume();
        
        if (cursor != null) {
        	cursor.close();
        }
        cursor = getContentResolver().query(CACHE_URI, null, AddressHelper.FAVORITE + " = " + 1, null,
                AddressHelper.DATE + " desc limit 50"); // Maximum list size is 50 as per VzM doc

        if (cursor == null || cursor.getCount() == 0) {
            getListView().setAdapter(null);
            getListView().invalidate();
            Toast.makeText(FavoritesListActivity.this, getString(R.string.no_favorites_dlg_msg),
                    Toast.LENGTH_SHORT).show();
            if (cursor != null) {
            	cursor.close();
            	cursor = null;
            }
        } else {
            cursor.moveToFirst();
            pLA = new PlacesListAdapter(this, cursor, false);
            setListAdapter(pLA);
        }
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

    	// close the cursor
    	if (pLA != null) {
    		final Cursor cursor = pLA.getCursor();
    		if (cursor != null) {
    			cursor.close();
    		}
    	}
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);

       
//        if (resultCode == Activity.RESULT_OK) {
//            Intent returnIntent = new Intent();  
//            returnIntent.putExtra(AddLocationActivity.IMAGE_FILE_PATH, data.getStringExtra(AddLocationActivity.IMAGE_FILE_PATH));
//            returnIntent.putExtra(AddLocationActivity.MAP_URL, data.getStringExtra(AddLocationActivity.MAP_URL));
//            returnIntent.putExtra(AddLocationActivity.PLACE_LAT, data.getDoubleExtra(AddLocationActivity.PLACE_LAT,0));
//            returnIntent.putExtra(AddLocationActivity.PLACE_LONG, data.getDoubleExtra(AddLocationActivity.PLACE_LONG,0));
//            returnIntent.putExtra(AddLocationActivity.ADDRESS_TITLE, data.getStringExtra(AddLocationActivity.ADDRESS_TITLE));
//            if (data.getStringExtra(AddLocationActivity.STATE) != null && data.getStringExtra(AddLocationActivity.STATE).length() > 0) {
//                returnIntent.putExtra(AddLocationActivity.STATE, data.getStringExtra(AddLocationActivity.STATE));
//                returnIntent.putExtra(AddLocationActivity.CITY, data.getStringExtra(AddLocationActivity.CITY));
//                returnIntent.putExtra(AddLocationActivity.ZIP, data.getStringExtra(AddLocationActivity.ZIP));
//            }
//            else {
//                returnIntent.putExtra(PlacesActivity.PLACESNAME, data.getStringExtra(PlacesActivity.PLACESNAME));
//            	returnIntent.putExtra(AddLocationActivity.CITYSTATEZIP, data.getStringExtra(AddLocationActivity.CITYSTATEZIP));
//            }
//            getParent().setResult(Activity.RESULT_OK, returnIntent);
//            getParent().finish();
//        }
        
    }

}
