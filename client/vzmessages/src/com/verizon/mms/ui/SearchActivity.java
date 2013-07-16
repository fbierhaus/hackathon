/**
 * Copyright (c) 2009, Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.provider.Telephony.Threads;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rocketmobile.asimov.Asimov;
import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.ArrayListCursor;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.data.Contact;
import com.verizon.mms.data.ContactList;
import com.verizon.mms.data.Conversation;
import com.verizon.mms.ui.adapter.SearchCursorAdapter;
import com.verizon.mms.ui.widget.ImageViewButton;
import com.verizon.mms.util.EmojiParser;
import com.verizon.mms.util.SearchHash;

/***
 * Presents a List of search results. Each item in the list represents a thread which
 * matches. The item contains the contact (or phone number) as the "title" and a
 * snippet of what matches, below. The snippet is taken from the most recent part of
 * the conversation that has a match. Each match within the visible portion of the
 * snippet is highlighted.
 */

public class SearchActivity extends VZMListActivity {
    private ListView                   listView          = null;
    private SearchCursorAdapter        adapter           = null;
    private EditText                   editText          = null;
    private SearchMessageTask          searchMessageTask = null;
    private TextView                   emptyTextView     = null;
    public  static final String        SELECTED_MSG       = "select_id";
    public  static final String        SELECTED_ALL_MSG = "select_ids";
    public  static final String        SEARCHED_STRING    = "highlight"; 
    private RelativeLayout 			   searchHeader	  = null;
    
    private String 					   curSearchString	  = null;
    private Map<Long, List<Long>>      recipIdThreadsMap = null;
    private boolean					   conversationLoading = true; //we are still in the process of loading the conversations in background
    private ContactLoadTask			   contactLoadTask = null;
    private SearchHash				   searchHash = new SearchHash();

    private static final String ALL_THREADS_WHERE = Threads.RECIPIENT_IDS + " != '' and " + Threads._ID + " != -1";
    private static final String[] THREADS_PROJECTION = { Threads._ID, Threads.RECIPIENT_IDS};
    private static final int COL_THREAD_ID = 0;
    private static final int COL_RECIPIENT_ID = 1;

    public int colRowId = -1;
    public int colThreadId = -1;
    public int colBody = -1;
    public int colDate = -1;
    
	/*
	 * Subclass of TextView which displays a snippet of text which matches the full text and
	 * highlights the matches within the snippet.
	 */
	public static class TextViewSnippet extends TextView {
		private static String sEllipsis = "\u2026";

		private static int sTypefaceHighlight = Typeface.BOLD;

		private String mFullText;
		private String mTargetString;
		private Pattern mPattern;

		public TextViewSnippet(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public TextViewSnippet(Context context) {
			super(context);
		}

		public TextViewSnippet(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		/**
		 * We have to know our width before we can compute the snippet string. Do that
		 * here and then defer to super for whatever work is normally done.
		 */
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			String fullTextLower = mFullText.toLowerCase();
			String targetStringLower = mTargetString.toLowerCase();

			int startPos = 0;
			int searchStringLength = targetStringLower.length();
			int bodyLength = fullTextLower.length();

			Matcher m = mPattern.matcher(mFullText);
			if (m.find(0)) {
				startPos = m.start();
			}

			TextPaint tp = getPaint();

			float searchStringWidth = tp.measureText(mTargetString);
			float textFieldWidth = getWidth();

			String snippetString = null;
			if (searchStringWidth < textFieldWidth) {
				float ellipsisWidth = tp.measureText(sEllipsis);
				textFieldWidth -= (2F * ellipsisWidth); // assume we'll need one on both ends

				int offset = -1;
				int start = -1;
				int end = -1;
				/*
				 * TODO: this code could be made more efficient by only measuring the additional
				 * characters as we widen the string rather than measuring the whole new
				 * string each time.
				 */
				while (true) {
					offset += 1;

					int newstart = Math.max(0, startPos - offset);
					int newend = Math.min(bodyLength, startPos + searchStringLength + offset);

					if (newstart == start && newend == end) {
						// if we couldn't expand out any further then we're done
						break;
					}
					start = newstart;
					end = newend;

					// pull the candidate string out of the full text rather than body
					// because body has been toLower()'ed
					String candidate = mFullText.substring(start, end);
					if (tp.measureText(candidate) > textFieldWidth) {
						// if the newly computed width would exceed our bounds then we're done
						// do not use this "candidate"
					    break;
					}

					snippetString = String.format("%s%s%s", start == 0 ? "" : sEllipsis, candidate,
							end == bodyLength ? "" : sEllipsis);
				}
			}
			if(snippetString == null){
			    // required for large search String 
			    if(searchStringLength > bodyLength)
                {
                    searchStringLength = bodyLength - startPos;
                }
			    String candidate = mFullText.substring(startPos, startPos + searchStringLength);
			    snippetString = String.format("%s%s%s", startPos == 0 ? "" : sEllipsis, candidate,
			            searchStringLength == bodyLength ? "" : sEllipsis);
			} 
			    
            SpannableString spannable = new SpannableString(snippetString);
            int start = 0;
            m = mPattern.matcher(snippetString);
            while (m.find(start)) {
                 spannable.setSpan(new StyleSpan(sTypefaceHighlight), m.start(), m.end(), 0);
                 start = m.end();
            }
            CharSequence txt=EmojiParser.getInstance().addEmojiSpans(spannable, true);
            setText(txt);
			// do this after the call to setText() above
            super.onLayout(changed, left, top, right, bottom);
		}

		public void setText(String fullText, String target) {
			// Use a regular expression to locate the target string
			// within the full text. The target string must be
			// found as a word start so we use \b which matches
			// word boundaries.
			String patternString = "\\b" + Pattern.quote(target);
			mPattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);

			mFullText = fullText;
			mTargetString = target;
			requestLayout();
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			try {
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			} catch (IndexOutOfBoundsException e) {
				Logger.debug("SearchActivity.java ", "onMeasure found IndexOutOfBoundsException");
				//remove the spans if it crashes
				CharSequence cs = getText();
				CharSequence newSequence = cs;
	        	if (cs instanceof Spanned || cs instanceof Spannable) {
	        		newSequence = removeSpans(new SpannableStringBuilder(cs));
	        	}
	        	
	        	setText(newSequence);
	        	super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	        }
		}

		private CharSequence removeSpans(Spannable newsequence) {
			Object[] spans = newsequence.getSpans(0, newsequence.length(), StyleSpan.class);
			for (Object span : spans) {
				newsequence.removeSpan(span);
			}
			
			return newsequence;
		}
	}

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.search_activity);
        initView();
        
        
        contactLoadTask = new ContactLoadTask();
        contactLoadTask.execute();
    }

	private void initView() {
        editText = (EditText)findViewById(R.id.searchEdit);
        editText.addTextChangedListener(mSearchWatcher);
        listView = getListView();
        listView.setItemsCanFocus(true);
        listView.setFocusable(true);
        listView.setClickable(true);
        listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long arg3) {
				Cursor cursor = adapter.getCursor();
				cursor.moveToPosition(position);
				long threadId = cursor.getLong(colThreadId);
	        	long rowid = cursor.getLong(colRowId);
	        	long date = 0l;
	        	long tempMsgId = rowid;
	        	if (rowid > 0) {
	        		date = cursor.getLong(colDate);
	        		if (date < Integer.MAX_VALUE) {
	        			tempMsgId = -rowid;
	        		}
	        	}
	        	
	        	adapter.gotoComposeMessageFragment(threadId, rowid, tempMsgId);
			}
        });
        emptyTextView = (TextView)findViewById(R.id.empty);
        searchHeader = (RelativeLayout)findViewById(R.id.searchScreenHeader);
        
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        InputMethodManager imm = (InputMethodManager) this
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();

    	if (searchMessageTask != null) {
    		searchMessageTask.cancel(true);
    	}
    	
    	if (contactLoadTask != null) {
    		contactLoadTask.cancel(true);
    	}
    	
    	// close the cursor
    	final SearchCursorAdapter adapter = this.adapter;
    	if (adapter != null) {
    		final Cursor cursor = adapter.getCursor();
    		if (cursor != null) {
    			cursor.close();
    		}
    	}
    }

    
    
    @Override
	protected void onStop() {
    	// close the cursor
    	final SearchCursorAdapter adapter = this.adapter;
    	if (adapter != null) {
    		final Cursor cursor = adapter.getCursor();
    		if (cursor != null) {
    			cursor.close();
    		}
    	}
		super.onStop();
	}


    @Override
	protected void onStart() {
		super.onStart();
		
		if (editText != null) {
			CharSequence ch = editText.getText();
			if (ch.length() > 0) {
				editText.setText(ch);
			}
		}
	}




	private TextWatcher mSearchWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(final Editable searchString) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence searchString, int start, int before, int count) {
            if(searchMessageTask != null){
                searchMessageTask.cancel(true);
            }
            curSearchString = searchString.toString();
            if (searchString.length() > 0) {
                searchMessageTask = new SearchMessageTask(curSearchString);
                searchMessageTask.execute();
            }
            else
            {
                listView.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.GONE);
            	searchHeader.invalidate();
            }
        }
    };
    
    private class SearchMessageTask extends AsyncTask<Void, Void, Cursor>{
    	private String searchString = null;
    	
    	public SearchMessageTask(String search) {
    		searchString = search;
    	}

    	@Override
    	protected Cursor doInBackground(Void... params) {
    		String search = searchString.trim();
    		String pattern = Uri.encode(search);
    		pattern = pattern.replace("%20", " ");
    		Uri uri = VZUris.getMmsSmsSearch().buildUpon().appendQueryParameter("pattern", 
    				pattern).build();
    		Cursor cursor = SearchActivity.this.getContentResolver().query(uri, null, null, null, null);
    		
    		if (cursor == null) {
    			return null;
    		}
    		
    		
    		String[] columnNames = cursor.getColumnNames();
    		
    		if (colRowId == -1) {//columns not initialized initialize them
    			colRowId = cursor.getColumnIndex("_id");
    			colThreadId = cursor.getColumnIndex("thread_id");
    			colBody = cursor.getColumnIndex("body");
    			colDate = cursor.getColumnIndex("date");
    		}
    		
    		int columns = columnNames.length;
    		
    		if (!conversationLoading) {
    			List<Contact> matching = searchHash.getMatchingContactList(curSearchString, -1);
    			ArrayList<ArrayList<Object>> wrap = new ArrayList<ArrayList<Object>>();
    			for (Contact contact : matching) {
    				List<Long> ids = recipIdThreadsMap.get(contact.getRecipientId());
    				for (Long threadId : ids) {
    					ArrayList<Object> result = new ArrayList<Object>();
    					
    					for (int i = 0; i < columns; i++) {
    						if (i == colRowId) {
    							result.add(SearchCursorAdapter.ROW_TYPE_CONTACT_RES);
    						} else if (i == colThreadId) {
    							result.add(threadId);
    						} else if (i == colBody) {
    							result.add(contact.getRecipientId());
    						} else {
    							result.add("");
    						}
    			        }

    					wrap.add(result);
    				}
    			}
    			if (wrap.size() > 0) {
    				ArrayListCursor contactLoadingCursor = new ArrayListCursor(columnNames, wrap);
    				cursor = new MergeCursor(new Cursor[] {contactLoadingCursor, cursor});
    			}
    		}
    		return cursor;
    	}

    	@Override
    	protected void onPostExecute(Cursor cursor) {
    		if (cursor == null) {
    			return;
    		}
    		Cursor mergedCursor = cursor;
    		
    		if (conversationLoading) {
    			String[] columnNames = cursor.getColumnNames(); 
        		int columns = columnNames.length;

        		ArrayList<Object> result = new ArrayList<Object>();
				
				for (int i = 0; i < columns; i++) {
					if (i == colRowId) {
						result.add(SearchCursorAdapter.ROW_TYPE_SEARCH_LOADING);
					} else {
						result.add("");
					}
		        }
				
    	        ArrayList<ArrayList<Object>> wrap = new ArrayList<ArrayList<Object>>();
    	        wrap.add(result);

    	        ArrayListCursor contactLoadingCursor = new ArrayListCursor(columnNames, wrap);
    	        
    	        mergedCursor = new MergeCursor(new Cursor[] {contactLoadingCursor, cursor});
    		}
    		
    		int cursorCount = mergedCursor.getCount();
    		if (cursorCount > 0) {
    			emptyTextView.setVisibility(View.GONE);
    			listView.setVisibility(View.VISIBLE);
    			// Note that we're telling the CursorAdapter not to do auto-requeries. If we
    			// want to dynamically respond to changes in the search results,
    			// we'll have have to add a setOnDataSetChangedListener().
    			if (adapter == null) {
    				adapter = new SearchCursorAdapter(SearchActivity.this, mergedCursor, false , searchString);
    				setListAdapter(adapter);
    			} else {
    				adapter.changeCursor(mergedCursor, searchString);
    			}

    			// Remember the query if there are actual results
    			SearchRecentSuggestions recent = ((Asimov) getApplication()).getRecentSuggestions();
    			if (recent != null && cursor.getCount() > 0) {
    				recent.saveRecentQuery(searchString,
    						getString(R.string.search_history, cursor.getCount(), searchString));
    			}
    			listView.setSelection(0);
    			mergedCursor.moveToPosition(0);
    		}
    		else
    		{
    			cursor.close();
    			listView.setVisibility(View.GONE);
    			emptyTextView.setVisibility(View.VISIBLE);
    		}
    	}
    }

    
  	/**
  	 * This class is used to load the contacts using the recipientdId and map them to the threads which contains those recipient id
  	 */
  	private class ContactLoadTask extends AsyncTask<Void, Long, Void>{
    	@Override
    	protected Void doInBackground(Void... params) {
    		cacheAllThreads(SearchActivity.this);
    		    		
    		for (Long id : recipIdThreadsMap.keySet()) {
    			if (isCancelled()) {
    				return null;
    			}
    			ContactList list = ContactList.getByIds(id.toString(), true);
    			
    			if (isCancelled()) {
    				return null;
    			}
    			for (Contact contact : list) {
    				/*if (contact.existsInDatabase())*/ {
    					searchHash.addSearchItem(contact);
    				}
    			}
    		}
    		
    		return null;
    	}

		@Override
		protected void onProgressUpdate(Long... values) {
			
		}

		@Override
		protected void onPostExecute(Void result) {
			conversationLoading = false;

			if (TextUtils.isEmpty(curSearchString)) {
				return;
			}
			if(searchMessageTask != null){
				searchMessageTask.cancel(true);
			}
			searchMessageTask = new SearchMessageTask(curSearchString);
			searchMessageTask.execute();
		}
		
	    //load all the contacts present in the threads
		private void cacheAllThreads(Context context) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("[SEarchActivity] cacheAllThreads: begin");
			}

			recipIdThreadsMap = new HashMap<Long, List<Long>>();
			// Query for all conversations.
			Cursor c = null;
			c = context.getContentResolver().query(Conversation.sAllThreadsUri,
					THREADS_PROJECTION, ALL_THREADS_WHERE, null, null);
			if (c != null) {
				while (c.moveToNext()) {
					String recipId = c.getString(COL_RECIPIENT_ID);
					long   threadId = c.getLong(COL_THREAD_ID);
					final String[] ids = recipId.split(" ");
					for (String id : ids) {
						long recip = Long.parseLong(id);
						List<Long> threads = recipIdThreadsMap.get(recip);
						if (threads == null) {
							threads = new ArrayList<Long>();
							recipIdThreadsMap.put(recip, threads);
						}
						threads.add(threadId);
					}
				}
				c.close();
			}
			
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug("[SEarchActivity] cacheAllThreads: end");
			}
		}
  	}
}
