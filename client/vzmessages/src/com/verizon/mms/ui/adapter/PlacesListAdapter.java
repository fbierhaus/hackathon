/**
 * AddressInfoAdapter.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.adapter;


import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CursorAdapter;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.nbi.location.Location;
import com.nbi.map.data.Coordinates;
import com.nbi.map.data.Place;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.AddressProvider.AddressHelper;
import com.verizon.mms.model.AddressModel;
import com.verizon.mms.ui.AddLocationActivity;
import com.verizon.mms.ui.MessagingPreferenceActivity;


/**
 * This class/interface   
 * @author Imthiaz
 * @Since  Apr 12, 2012
 */
public class PlacesListAdapter extends CursorAdapter implements OnClickListener {

	private static final float INFO_TITLE_TEXT_SIZE = 16.784f;
	private static final float INFO_TITLE_TEXT_SIZE_LARGE = 20.784f;
	private static final float INFO_SUBTITLE_TEXT_SIZE = 14f;
	private static final float INFO_SUBTITLE_TEXT_SIZE_LARGE = 18f;
	
	
    private static class ViewHolder  {
        
        private CheckBox favState;
        private TextView title;
        private TextView subtitle;
        private TextView addressContext;        
    }
    
    private static class PlaceData {
        
        private int position;
        private String placeName;
        private String street;
        private int id;
        private double lat;
        private double lon;
        private String citystatezip;
    }
    
    private LayoutInflater mInflater;
    private boolean fromAddLocation;
    private List<Boolean> mToggleState;
    private List<Integer> mIds;
    private Context mContext;
    private static final int REQUEST_ADD_LOCATION = 1;
    // URIs
    public static final Uri    CACHE_URI = Uri.parse("content://address-cache/address");
    
    /**
     * @param context
     * @param c
     *  Constructor 
     */
    public PlacesListAdapter(Context context, Cursor c, boolean fromadd) {
        super(context, c);
        fromAddLocation = fromadd;
        mInflater = LayoutInflater.from(context);
        mContext = context;     
        updateToggleState();
    }

    public void updateToggleState() {
    	getCursor().moveToFirst();
    	for(mToggleState = new ArrayList<Boolean>(),mIds = new ArrayList<Integer>(); !getCursor().isAfterLast(); getCursor().moveToNext()) {
            mToggleState.add(getCursor().getInt(getCursor().getColumnIndex(AddressHelper.FAVORITE)) != 0 ? true : false);
            mIds.add(getCursor().getInt(getCursor().getColumnIndex(AddressHelper._ID)));
        }
    }
    
    /* Overriding method 
     * (non-Javadoc)
     * @see android.widget.CursorAdapter#newView(android.content.Context, android.database.Cursor, android.view.ViewGroup)
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewgroup) {
        
        View view =  mInflater.inflate(R.layout.places_listrow, null);
        
        ViewHolder holder = new ViewHolder();
        holder.favState = (CheckBox) view.findViewById(R.id.favIcon);
        holder.title = (TextView) view.findViewById(R.id.addressText);
        holder.title.setTextSize(20f);
        holder.subtitle = (TextView) view.findViewById(R.id.street);
        holder.subtitle.setTextSize(20f);
        holder.addressContext = (TextView) view.findViewById(R.id.addressContext);
        holder.addressContext.setTextSize(20f);
        view.setOnClickListener(this);
		if (PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(MessagingPreferenceActivity.APP_FONT_SUPPORT,
						MessagingPreferenceActivity.APP_FONT_SUPPORT_DEFAULT)
				.equals(MessagingPreferenceActivity.APP_FONT_SUPPORT_DEFAULT)) {
			if (android.provider.Settings.System.getFloat(
					context.getContentResolver(),
					android.provider.Settings.System.FONT_SCALE, (float) 1.0) > 1.0) {
				holder.title.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_TITLE_TEXT_SIZE_LARGE);
				holder.subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_SUBTITLE_TEXT_SIZE_LARGE);
				holder.addressContext.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_SUBTITLE_TEXT_SIZE_LARGE);
			} else {
				holder.title.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_TITLE_TEXT_SIZE);
				holder.subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_SUBTITLE_TEXT_SIZE);
				holder.addressContext.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_SUBTITLE_TEXT_SIZE);
			}

		} else {
			if (PreferenceManager
					.getDefaultSharedPreferences(context)
					.getString(MessagingPreferenceActivity.APP_FONT_SUPPORT,
							MessagingPreferenceActivity.APP_FONT_SUPPORT_NORMAL)
					.equals(MessagingPreferenceActivity.APP_FONT_SUPPORT_LARGE)) {
				holder.title.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_TITLE_TEXT_SIZE_LARGE);
				holder.subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_SUBTITLE_TEXT_SIZE_LARGE);
				holder.addressContext.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_SUBTITLE_TEXT_SIZE_LARGE);
			} else {
				holder.title.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_TITLE_TEXT_SIZE);
				holder.subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_SUBTITLE_TEXT_SIZE);
				holder.addressContext.setTextSize(TypedValue.COMPLEX_UNIT_SP,
						INFO_SUBTITLE_TEXT_SIZE);
			}

		}
        holder.addressContext.setOnClickListener(this);
        holder.favState.setOnClickListener(this);
        holder.title.setOnClickListener(this);
        holder.subtitle.setOnClickListener(this);
        view.setTag(holder);
        return view;
    }

    /* Overriding method 
     * (non-Javadoc)
     * @see android.widget.CursorAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // TODO Auto-generated method stub
        if (view ==  null) {
            view = mInflater.inflate(R.layout.places_listrow, null);
        }
        AddressModel item = AddressHelper.fromCursor(cursor);
        PlaceData data = new PlaceData();
        data.position = cursor.getPosition();
        data.id = mIds.get(data.position);
        data.citystatezip = item.getAddressContext();
        data.placeName = item.getPlaceName();
        data.street = item.getAddressText();
        data.lat = item.getLatitude();
        data.lon = item.getLongitude();
        
        ViewHolder holder  = (ViewHolder) view.getTag();
        holder.favState.setChecked(mToggleState.get(data.position));
        if (data.placeName != null) {
        	holder.title.setText(data.placeName);
        	holder.subtitle.setText(data.street);
        	holder.subtitle.setVisibility(View.VISIBLE);
        	holder.subtitle.setTag(data);
        	holder.addressContext.setText(data.citystatezip);
        }
        else if (!data.street.startsWith(mContext.getString(R.string.latwithcolon))){
        	holder.title.setText(data.street);
            holder.addressContext.setText(data.citystatezip);
        }
        else {
        	holder.title.setText(data.street);
        	RelativeLayout.LayoutParams lp = (LayoutParams) holder.title.getLayoutParams();
        	lp.addRule(RelativeLayout.CENTER_VERTICAL);
        	holder.title.setLayoutParams(lp);
        }
        if(mToggleState.get(data.position)) {
           holder.favState.setButtonDrawable(R.drawable.list_favorites_star_on);
           holder.favState.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.list_favorites_star_on));
        } else  {
           holder.favState.setButtonDrawable(R.drawable.list_favorites_star_off);
           holder.favState.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.list_favorites_star_off));
        }
        holder.favState.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) {
					buttonView.setButtonDrawable(R.drawable.list_favorites_star_on);
					buttonView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.list_favorites_star_on));
				} else  {
					buttonView.setButtonDrawable(R.drawable.list_favorites_star_off);
					buttonView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.list_favorites_star_off));
				}
			}
		});
        holder.addressContext.setTag(data);
        holder.title.setTag(data);
        holder.favState.setTag(data);
        
    }

    /* Overriding method 
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        Object object = v.getTag();
        PlaceData data = null;
        
        if (object instanceof PlaceData) {
            data = (PlaceData)object;
        } else if (object instanceof ViewHolder) {
            ViewHolder vh = (ViewHolder)object;
            data = (PlaceData)vh.addressContext.getTag();
        }
        
        if (data == null) {
            return;
        }
        
        switch (v.getId()) {
        case R.id.favIcon:
        	CheckBox checkBox = (CheckBox)v;
        	updateDB(checkBox.isChecked(),data.id);
        	mToggleState.set(mIds.indexOf(data.id), checkBox.isChecked());
        	break;
        default:
        	String searchString  = null;
        	boolean isOnlyLatLong = false;
            if (data.placeName != null) {
                searchString =  data.placeName;
             	searchString = searchString + "," + data.street;
               	searchString = searchString + "," + data.citystatezip;
            } else {
             	searchString = data.street;
             	if (data.citystatezip != null) {
             		searchString = searchString + "," + data.citystatezip;	
             	} else {
             		isOnlyLatLong = true;
             		searchString = data.lat + ","+ data.lon;
             	}
               	
            }
            if (isOnlyLatLong) {
            	Location location = new Location();
            	Coordinates coor = new Coordinates(data.lat, data.lon);
            	location.setLatitude(data.lat);
            	location.setLongitude(data.lon);
            	Place place = new Place(location);
            	((AddLocationActivity) mContext).drawPin(place, coor);
            } else {
            	((AddLocationActivity) mContext).doSearch(searchString);	
            }
        	
        
        	break;
        }
    }

    
    
    /**
     * This Method 
     * @param cursor
     */
    public void updateDB(boolean updateBooleanValue,int _id) {
        ContentValues values  =  new ContentValues();
        values.put(AddressHelper.FAVORITE,updateBooleanValue?1:0);
        mContext.getContentResolver().update(CACHE_URI, values, AddressHelper._ID + " = " + _id, null);
        
    }

}
