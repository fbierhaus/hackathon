package com.verizon.mms.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.DeviceConfig;
import com.verizon.mms.Media;
import com.verizon.mms.MediaProvider;
import com.verizon.mms.MmsException;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.model.LocationModel;
import com.verizon.mms.model.RegionModel;
import com.verizon.vcard.android.syncml.pim.vcard.ContactStruct;

public class SharedLocationAdapter extends CursorAdapter {

	public SharedLocationAdapter(Context context, Cursor c) {
		super(context, c);
	}
	
	private static class LocationRowViews {
		private TextView title;
		private TextView subtitle1;
		private TextView subtitle2;
		private TextView sender;
		private TextView date;
	}
	
	@Override
	public View newView(Context c, Cursor cur, ViewGroup group) {
		LayoutInflater vi = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = vi.inflate(R.layout.locationlistitem, null);
		LocationRowViews lrv = new LocationRowViews();
		lrv.date = ((TextView) view.findViewById(R.id.date));
		lrv.title = ((TextView) view.findViewById(R.id.addresstitle));
		lrv.subtitle1 = ((TextView) view.findViewById(R.id.subtitle1));
		lrv.subtitle2 = ((TextView) view.findViewById(R.id.subtitle2));
		lrv.sender = ((TextView) view.findViewById(R.id.lastmessagesender));
		view.setTag(lrv);
		return view;
	}

	@Override
	public void bindView(View convertView, final Context c, Cursor cur) {
		LocationRowViews lrv = (LocationRowViews) convertView.getTag();
		final Media mms = MediaProvider.Helper.fromCursorCurentPosition(cur);
		final LocationModel lm;
		try {
			lm = new LocationModel(c, Uri.parse(mms.getLocationUri()), new RegionModel(null, 0, 0, 0, 0));
			lrv.title.setText(R.string.loading);
			
			//TODO: cache the required data dont do a asynctask query
			new AsyncTask<LocationRowViews, Void, String>() {
				
				LocationRowViews mylrv;
				
				@Override
				protected String doInBackground(LocationRowViews... params) {
					mylrv = params[0];
					return lm.getFormattedMsg();
				}
				
				@Override
				protected void onPostExecute(String result) {
					String place = lm.getFormattedMsg();
					if (mms.isOutgoing()) {
						mylrv.sender.setText(c.getString(R.string.me));  
					}
					else {
						Contact contact = ContactList.getByNumbers(mms.getAddress(), false, false).get(0);
						mylrv.sender.setText(contact.getName());
					}
					mylrv.date.setText(MessageUtils.formatTimeStampString(mms.getDate(), false));
					String[] parts = place.split(", ");
					int partslen = parts.length;
					if (partslen == 1) {
						mylrv.title.setText(parts[0]);
						mylrv.subtitle1.setVisibility(View.GONE);
						mylrv.subtitle2.setVisibility(View.GONE);
					}
					if (partslen == 2) {
						mylrv.title.setText(parts[0]);
						mylrv.subtitle1.setText(parts[1]);
						mylrv.subtitle1.setVisibility(View.VISIBLE);
						mylrv.subtitle2.setVisibility(View.GONE);
					}
					if(partslen==3) {
					    mylrv.title.setText(parts[0]);
                        mylrv.subtitle1.setText(parts[1] + ", " + parts[2]);
                        mylrv.subtitle1.setVisibility(View.VISIBLE);
                        mylrv.subtitle2.setVisibility(View.GONE); 
					}
					
					if (partslen == 4) {
						mylrv.title.setText(parts[0]);
						mylrv.subtitle1.setText(parts[1] + ", " + parts[2] + ", " + parts[3]);
						mylrv.subtitle1.setVisibility(View.VISIBLE);
						mylrv.subtitle2.setVisibility(View.GONE);
					}
					if (partslen == 5) {
						mylrv.title.setText(parts[0]);
						mylrv.subtitle1.setText(parts[1]);
						mylrv.subtitle1.setVisibility(View.VISIBLE);
						mylrv.subtitle2.setText(parts[2] + ", " + parts[3]+ ", " + parts[4]);
						mylrv.subtitle2.setVisibility(View.VISIBLE);
					}
				}
			}.execute(lrv);
			convertView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
				    
				    if(DeviceConfig.OEM.isNbiLocationDisabled){
				        Intent goToGallery = new Intent(c, GalleryActivity.class);
				        goToGallery.putExtra("itemtogo", "content://" + VZUris.getMmsUri().getAuthority() + "/"
				                + mms.getMId());
				        goToGallery.putExtra("threadid", mms.getThreadId());
				        goToGallery.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				        c.startActivity(goToGallery);
				        return;
				    } 
					ContactStruct contactStruct = lm.getContactStruct();
					if (contactStruct != null) {
						String url = contactStruct.getURL();
						if (url != null) {
				            Intent intent = new Intent(c, AddLocationActivity.class);
				            intent.putExtra("mapURL", url);
				            c.startActivity(intent);
				        } 
						else {
				            Toast.makeText(c, R.string.url_not_present, Toast.LENGTH_LONG).show();
				        }
					}
					else {
			            Toast.makeText(c, R.string.url_not_present, Toast.LENGTH_LONG).show();
			        }
				}
			});
		} catch (MmsException e) {
			Logger.error(e);
			
			lrv.title.setText(R.string.location_parse_error);
			lrv.subtitle1.setVisibility(View.GONE);
			lrv.subtitle2.setVisibility(View.GONE);
			lrv.sender.setText("");
			lrv.date.setText("");
			convertView.setOnClickListener(null);
		}
	}
	
	public int getItemViewType(int position) {
	    return 0;
	}

	public int getViewTypeCount() {
	    return 1;
	}
	
}