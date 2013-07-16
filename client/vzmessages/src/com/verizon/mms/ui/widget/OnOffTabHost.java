package com.verizon.mms.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.verizon.messaging.vzmsgs.R;

public class OnOffTabHost extends LinearLayout {

	LayoutInflater inflater;
	private OnTabChangedListener tabChangedListener;
	
	public interface OnTabChangedListener {
		public void onTabChanged(int position);
	}
	
	public static class ViewHolder {
		ImageView icon;
		View selected;
		int position;
	}
	public OnOffTabHost(Context context) {
		super(context);
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public OnOffTabHost(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void setTabChangedListener(OnTabChangedListener listener) {
		tabChangedListener = listener;
	}
	
	public void addTab(int resId) {
		View view = getTab(resId);
		
		view.setOnClickListener(clickListener);
		addView(view);
	}
	
	public void setSelected(int position) {
		int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View view = getChildAt(i);
			ViewHolder holder = (ViewHolder)view.getTag();
			
			if (i == position) {
				holder.icon.setSelected(true);
				holder.selected.setVisibility(VISIBLE);
			} else {
				holder.icon.setSelected(false);
				holder.selected.setVisibility(GONE);
			}
		}
	}
	
	private View getTab(int imageResId) {
		View view = inflater.inflate(R.layout.tab_indicator, null);
		ViewHolder holder = new ViewHolder();
		
		view.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
		holder.icon = (ImageView)view.findViewById(R.id.icon);
		holder.selected = view.findViewById(R.id.selected);
		holder.icon.setImageResource(imageResId);
		holder.position = getChildCount();
		
		view.setTag(holder);
		
		return view;
	}
	
	private OnClickListener clickListener = new OnClickListener() {
		public void onClick(View v) {
			ViewHolder holder = (ViewHolder)v.getTag();
			setSelected(holder.position);
			
			if (tabChangedListener != null) {
				tabChangedListener.onTabChanged(holder.position);
			}
		}
	};
}
