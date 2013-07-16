/**
 * SearchCursorAdapter.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.mms.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.rocketmobile.asimov.ConversationListActivity;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.ui.ContactImage;
import com.verizon.mms.ui.FromTextView;
import com.verizon.mms.ui.MessageUtils;
import com.verizon.mms.ui.SearchActivity;
import com.verizon.mms.ui.SearchActivity.TextViewSnippet;
import com.verizon.mms.util.MemoryCacheMap;
import com.verizon.mms.util.Util;

public class SearchCursorAdapter extends CursorAdapter{
    Context context = null;

    public static final Long ROW_TYPE_SEARCH_LOADING = -1l;
    public static final Long ROW_TYPE_CONTACT_RES = -2l;
    
    private final int COL_ROWID;
    private final int COL_THREAD_ID;
    private final int COL_BODY;
    private final int COL_TIME;
    
    private String searchString;
    private final Drawable defaultImage;
    private ListView listView;
	
	private MemoryCacheMap<Long, Conversation> conversationCache = new MemoryCacheMap<Long, Conversation>(50);
   
	private static class ViewData {
        private FromTextView    title;
        private TextView        timeStampView;
        private ContactImage    avatarImage;
        private TextViewSnippet snippet;
        private View			searchdetail;
        private View 			contactLoading;
        private long threadId;
        private long rowid;
        private long tempMsgId;
    }
	
	
	
    public SearchCursorAdapter(Context context, Cursor c, boolean autoRequery, String searchStr) {
        super(context, c, autoRequery);
        this.context = context;
        this.searchString = searchStr;
        this.listView=listView;
        defaultImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
        COL_THREAD_ID = c.getColumnIndex("thread_id");
        COL_BODY = c.getColumnIndex("body");
        COL_ROWID = c.getColumnIndex("_id");
        COL_TIME = c.getColumnIndex("date");
        
       
    }
    
    
    public void gotoComposeMessageFragment( long threadId, long rowid,long tempMsgId){
    	if (rowid == ROW_TYPE_SEARCH_LOADING) {
            return;
        }
        final Intent onClickIntent = ConversationListActivity.getIntentFromParent(context, 0,
                false);

        if (rowid == ROW_TYPE_CONTACT_RES) {
            onClickIntent.putExtra("thread_id", threadId);
        } else {
            Cursor cursor = getCursor();
            int currentPos = cursor.getPosition();
            if (cursor != null) {
                cursor.moveToPosition(-1);
                StringBuilder sb = new StringBuilder();
                int cursorCount = cursor.getCount();
                if (cursorCount > 0) {
                    cursor.moveToFirst();
					while (cursor.isAfterLast() == false) {
						long rowidvalue = cursor.getLong(COL_ROWID);
						if (rowidvalue <= 0) {
							cursor.moveToNext();
							continue;
						}
						long tid = cursor.getLong(COL_THREAD_ID);
						if (threadId == tid) {
                            if (cursor.getLong(COL_TIME) < Integer.MAX_VALUE) {
                                //its an mms msgId, so passing as -ve 
                                sb.append(-rowidvalue);
                            } else {
                                sb.append(rowidvalue);
                            }
                            sb.append(":");
						}
						cursor.moveToNext();
					}
                    cursor.moveToPosition(currentPos);
                }
                onClickIntent.putExtra("thread_id", threadId);
                onClickIntent.putExtra(SearchActivity.SEARCHED_STRING, searchString);
                onClickIntent.putExtra("SELECTED_MSG", tempMsgId);
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("Search:  SELECTED_MSG" + rowid + ": total:" + sb.toString());
                }
                onClickIntent.putExtra(SearchActivity.SELECTED_ALL_MSG, sb.toString());
            }
        }
        if (Util.isMultiPaneSupported((Activity)context)) {
        	onClickIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        context.startActivity(onClickIntent);
    }
    
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final ViewData data = (ViewData)view.getTag();
        long id = cursor.getLong(COL_ROWID);
        
        if (id == -1) {
        	data.contactLoading.setVisibility(View.VISIBLE);
        	data.searchdetail.setVisibility(View.GONE);
        } else {
        	data.threadId = cursor.getLong(COL_THREAD_ID);
        	data.rowid = cursor.getLong(COL_ROWID);
        	boolean isMMS=false;
        	data.contactLoading.setVisibility(View.GONE);
        	data.searchdetail.setVisibility(View.VISIBLE);
        	
        	// Need to get the Conversation for Recipients Name and Image
        	Conversation conv = null;
        	conv = conversationCache.get(data.threadId);
        	if (conv == null) {
        		conv = Conversation.get(context, data.threadId, false);
        		conversationCache.put(data.threadId, conv);
        	}

        	ContactList list = conv.getRecipients();
        	
        	if (data.rowid == ROW_TYPE_CONTACT_RES) {
        		long recipId = cursor.getLong(COL_BODY);
        		int size = list.size();
        		
        		if (size > 1) {
        			Contact matchedContact = null;
        			for (int i = 0; i < size; i++) {
        				matchedContact = list.get(i); 
        				if (matchedContact.getRecipientId() == recipId) {
        					if (i != 0) {
        						list.remove(i);
        						list.add(0, matchedContact);
        					}
        					break;
        				}
        			}
        		}
        	}
        	updateTitle(list, data.title,data.rowid);
        	updateContactImage(list, data.avatarImage);

        	if (data.rowid > 0) {
        		String body = cursor.getString(COL_BODY);
        		
        		data.timeStampView.setVisibility(View.VISIBLE);
        		data.snippet.setVisibility(View.VISIBLE);
        		
        		long time = cursor.getLong(COL_TIME);
        		if (time < Integer.MAX_VALUE) {
        			time = time * 1000;
        			isMMS=true;
        		}
        		String timsestamp = MessageUtils.formatTimeStampString(time , false);
        		data.timeStampView.setText(timsestamp);
        		data.snippet.setText(body, searchString);
        	} else {
        		data.timeStampView.setVisibility(View.GONE);
        		data.snippet.setVisibility(View.GONE);
        	}
        	data.tempMsgId = isMMS ? -data.rowid :data.rowid;
        
        	view.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                	gotoComposeMessageFragment(data.threadId, data.rowid, data.tempMsgId);
                }
        	});
        }
    }
  
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.search_item, parent, false);
        final ViewData data1 = new ViewData();
        data1.title = (FromTextView) (view.findViewById(R.id.title));
        data1.timeStampView = (TextView) (view.findViewById(R.id.timeStamp));
        data1.avatarImage = (ContactImage)(view.findViewById(R.id.avatar));
        data1.snippet = (TextViewSnippet) (view.findViewById(R.id.subtitle));
        data1.contactLoading = view.findViewById(R.id.loadingContact);
        data1.searchdetail = view.findViewById(R.id.searchDetails);
        view.setTag(data1);
        if(Logger.IS_DEBUG_ENABLED){
        	Logger.debug("number of time newView() method called");
        }
        return view;
    }

    private void updateTitle(ContactList list, TextView title, long rowId) {
        if (list.size() == 0) {
            String recipient = "";
            title.setText(recipient);
        } else {
        	if (rowId > 0) {
        		((FromTextView) title).setNames(list);
        	} else {
        		((FromTextView) title).setNames(list, searchString);
        	}
        }
    }
    
    private void updateContactImage(ContactList contactList, ContactImage avatar) {
        if (contactList != null) {
			Drawable[] images = contactList.getImages(context, defaultImage, false);
			if (images == null) {
				images = new Drawable[] { defaultImage };
			}
			avatar.setImages(images, defaultImage);
       }
    }

	public void changeCursor(Cursor cursor, String searchString) {
		this.searchString = searchString;
		super.changeCursor(cursor);
	}
}
