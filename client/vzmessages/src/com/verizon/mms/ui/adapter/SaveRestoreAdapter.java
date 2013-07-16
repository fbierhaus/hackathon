/**
 * SaveRestoreAdapter.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.model.SaveRestoreItem;

/**
 * This class/interface   
 * @author Imthiaz
 * @Since  May 29, 2012
 */
public class SaveRestoreAdapter extends BaseAdapter {

    private ArrayAdapter<SaveRestoreItem> items;
    
    
    public SaveRestoreAdapter(ArrayAdapter<SaveRestoreItem> items) {
        super();
        this.items = items;
    }
    
    @Override
    public int getCount() {
        
        return items.getCount();
    }

    @Override
    public Object getItem(int position) {
        
        return items.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater li = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = li.inflate(R.layout.save_list_item, parent, false);
        SaveRestoreItem item = items.getItem(position);
        TextView name = (TextView) convertView.findViewById(R.id.savedFileName);
        name.setText(item.getFilename());
        /*TextView size = (TextView) convertView.findViewById(R.id.size);
        size.setText(item.getSizeString(parent.getContext()));*/
        TextView date = (TextView) convertView.findViewById(R.id.date);
        ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
        if (item.getType() == SaveRestoreItem.TYPE_DIRECTORY)
        {
            icon.setImageResource(R.drawable.folder_icon);
            date.setVisibility(View.GONE);
        }
        else
        {
           icon.setImageResource(R.drawable.launcher_home_icon);
           String fileName = item.getStringFilename();
           if (fileName.endsWith(".vzm") || (fileName.endsWith(".xml"))) {
        	  String dateString  = null;
        	  dateString = (String)item.getDateString(parent.getContext());
        	  if (dateString != null) {
        		  date.setText(dateString);
    			  date.setVisibility(View.VISIBLE);  
        	  }
			  
		   }
        }
        return convertView;
    }

}
