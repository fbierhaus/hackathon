/**
 * RestoreConversationAdapter.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.ui.ContactImage;
import com.verizon.mms.ui.FromTextView;
import com.verizon.mms.ui.MessageUtils;

/**
 * This class/interface
 * 
 * @author Imthiaz
 * @Since Jun 12, 2012
 */
public class RestoreConversationAdapter extends BaseAdapter {

	private ArrayList<Object> items;

 	private ArrayList<String> selectedList;
	private HashMap<String, String> dateList;
	private Context mContext;
	private Button restoreButton;
	private Button unSelectAllButton;
	private Button selectAllButton;
	// Image Variables
	private Drawable defaultImage;
	private Drawable[] defaultImages;
	
	
	private static class ViewData {
		private FromTextView from;
		private TextView date;
		private ContactImage avatar;
		private CheckBox checkBox;
	}

	

	public static class ContactData {
		public String names;
		public int num;

		/**
		 * Constructor
		 */
		public ContactData(String names, int num) {
			this.names = names;
			this.num = num;
		}
	}

	/**
	 * 
	 * Constructor
	 */
	public RestoreConversationAdapter(Context context, ArrayList<Object> items,
			HashMap<String, String> dateList, Button restoreButton, Button unSelectAllButton, Button selectAllButton,
			boolean hasMultipleConversations) {
		super();
		this.mContext = context;
		this.items = items;
		this.restoreButton = restoreButton;
		this.unSelectAllButton = unSelectAllButton;
		this.selectAllButton = selectAllButton;
		this.selectedList = new ArrayList<String>();
		this.dateList = dateList;
		defaultImage = mContext.getResources().getDrawable(
				R.drawable.ic_contact_picture);
		
		
	
	}
	
	public  Handler handler = new Handler() {
        public void handleMessage(Message msg) {
               try {
                    notifyDataSetChanged();
                }
                catch (Exception e) {
                    Logger.error(getClass(),e);
                }
            
      }
   };
	public Handler getHandler() {
		return handler;
	}
		    
	
	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return items.size();	
	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int pos) {
		// TODO Auto-generated method stub
		return items.get(pos);
	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	/*
	 * Overriding method (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getView(int, android.view.View,
	 * android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewData data;
		LayoutInflater inflator = (LayoutInflater) parent.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
            if (convertView == null) {
				convertView = inflator.inflate(R.layout.restore_conversation_list_item,
						parent, false);
            }
			data = new ViewData();
			data.from = (FromTextView) convertView.findViewById(R.id.from);
			data.date = (TextView) convertView.findViewById(R.id.date);
			data.avatar = (ContactImage) convertView.findViewById(R.id.avatar);
			data.checkBox = (CheckBox) convertView.findViewById(R.id.checkBox);
			convertView.setTag(data);
			String recipients = (String) items.get(position);
			ContactList contactList = ContactList.getByNumbers(recipients.trim(),
					false, false);
			data.from.setNames(contactList);
			updateTitle(contactList, data.from);
			updateContactImage(contactList, data.avatar);
			String time = dateList.get(recipients); //Need to trim if required
			if (time != null) {
				long dateInMilliSec = (time.length() > 10) ? Long.parseLong(time)
						: Long.parseLong(time) * 1000;
				String date = MessageUtils.formatTimeStampString(dateInMilliSec,
						true);
				data.date.setText(String.valueOf(date));	
			}
			
			data.checkBox.setVisibility(View.VISIBLE);
			data.checkBox.setTag(recipients);

			if (selectedList.contains(recipients)) {
				data.checkBox.setChecked(true);
			} else {
				data.checkBox.setChecked(false);
			}

			data.checkBox.setOnCheckedChangeListener(checkedChangeListener);

		return convertView;
	}

	
	private void updateTitle(ContactList list, TextView title) {
		if (list.size() == 0) {
			String recipient = "";
			title.setText(recipient);

		} else {
			((FromTextView) title).setNames(list);
		}
	}

	public void updateContactImage(ContactList contactList,
			ContactImage mAvatarView) {
		if (contactList != null) {
			defaultImages = contactList
					.getImages(mContext, defaultImage, false);
			if (defaultImages == null) {
				defaultImages = new Drawable[] { defaultImage };
			}
			mAvatarView.setImages(defaultImages, defaultImage);

		}
	}

	


	private OnCheckedChangeListener checkedChangeListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {

			String recipient = (String) buttonView.getTag();
			if (isChecked) {
				restoreButton.setEnabled(true);
				if (!selectedList.contains(recipient))
					selectedList.add(recipient);
			} else {
				if (selectedList.contains(recipient))
					selectedList.remove(recipient);
			}

			if (selectedList.size() == 0) {
				restoreButton.setEnabled(false);
				selectAllButton.setVisibility(View.VISIBLE);
				unSelectAllButton.setVisibility(View.GONE);	
				
			} else {
				restoreButton.setEnabled(true);
				unSelectAllButton.setEnabled(true);
				selectAllButton.setVisibility(View.GONE);
				unSelectAllButton.setVisibility(View.VISIBLE);
				
			}

		}
	};

	public ArrayList<String> getSelectedThreadIDs() {
		
	      return selectedList;
		
	}

	public void setAllItemsChecked(ArrayList<String> recipients) {
		for (String recipient : recipients) {
			if (!selectedList.contains(recipient)) {
				selectedList.add(recipient);
			}
			restoreButton.setEnabled(true);
			unSelectAllButton.setEnabled(true);	
			
			
		}
		notifyDataSetChanged();

	}

	public void setAllItemsUnChecked() {
		selectedList.clear();
		restoreButton.setEnabled(false);
		unSelectAllButton.setEnabled(false);	
		notifyDataSetChanged();
	}



	

	

	
	

    
    

}
