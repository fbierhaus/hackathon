/**
 * ConversationSeachAdapter.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.adapter;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.ComposeMessageSearchActivity.TextViewSnippet;
import com.verizon.mms.ui.ConversationSearchItem;
import com.verizon.mms.ui.SearchActivity;

public class ConversationSeachAdapter extends
		ArrayAdapter<ConversationSearchItem> {

	private List<ConversationSearchItem> searchItems;
	private Context mContext;
	String searchString;
	String matchMsgids;
	private static class SearchHolder {
		private TextView timeStampView;
		private TextView msgSource;
		private TextViewSnippet snippet;

	}

	public ConversationSeachAdapter(Context context,
			List<ConversationSearchItem> searchItemList, String searchString, String matchMsgids) {
		super(context, R.layout.comp_search_item, searchItemList);
		this.mContext = context;
		this.searchItems = searchItemList;
		this.searchString = searchString;
		this.matchMsgids=matchMsgids;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowItem = convertView;
		SearchHolder data = null;
		if (rowItem == null) {
			data = new SearchHolder();
			LayoutInflater inflater = LayoutInflater.from(mContext);
			rowItem = inflater
					.inflate(R.layout.comp_search_item, parent, false);
			data.snippet = (TextViewSnippet) rowItem
					.findViewById(R.id.subtitle);
			data.msgSource = (TextView) rowItem.findViewById(R.id.msgSource);
			data.timeStampView = (TextView) rowItem
					.findViewById(R.id.timeStamp);
			rowItem.setTag(data);
		} else {
			data = (SearchHolder) rowItem.getTag();
		}
		final ConversationSearchItem dataItem = searchItems.get(position);
		data.snippet.setText(dataItem.getSnippet(), searchString);
		data.timeStampView.setText(dataItem.getTimeStamp());
		data.msgSource.setText(dataItem.getMsgSource());
		rowItem.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent onClickIntent = new Intent();
				onClickIntent.putExtra("thread_id", dataItem.getThreadId());
				onClickIntent.putExtra(SearchActivity.SEARCHED_STRING, searchString);
				onClickIntent.putExtra("SELECTED_MSG", dataItem.getMsgId());
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("PIX:  SELECTED_MSG" + dataItem.getMsgId() + ": total:");
                }
				onClickIntent.putExtra(SearchActivity.SELECTED_ALL_MSG,matchMsgids);
				((Activity)mContext).setResult(Activity.RESULT_OK,onClickIntent);
				((Activity)mContext).finish();}
		});
		return rowItem;
	}

	public int getCount() {
		return searchItems.size();
	}

}
