package com.verizon.mms.ui.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;


public class UrlAdapter extends ArrayAdapter<String> {
	private Context context;

	public static final String TEL_PREFIX = "tel:";
	private static final int TEL_PREFIX_LEN = TEL_PREFIX.length();


	public UrlAdapter(Context context, ArrayList<String> urls) {
		super(context, android.R.layout.select_dialog_item, urls);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view;
		try {
			view = (TextView)super.getView(position, convertView, parent);
			String url = getItem(position).toString();
			final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			final Drawable d = context.getPackageManager().getActivityIcon(intent);
			if (d != null) {
				d.setBounds(0, 0, d.getIntrinsicHeight(), d.getIntrinsicHeight());
				view.setCompoundDrawablePadding(10);
				view.setCompoundDrawables(d, null, null, null);
			}
			if (url.startsWith(TEL_PREFIX)) {
				url = PhoneNumberUtils.formatNumber(url.substring(TEL_PREFIX_LEN));
			}
			view.setText(url);
		}
		catch (Exception e) {
			Logger.error(getClass(),e);
			view = null;
		}
		return view;
	}
}
