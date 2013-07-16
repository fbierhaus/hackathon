package com.verizon.mms.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;
import com.verizon.messaging.vzmsgs.R;
import com.verizon.mms.ui.adapter.ConversationSeachAdapter;
import com.verizon.mms.util.EmojiParser;

public class ComposeMessageSearchActivity extends VZMListActivity{
    private ListView mListView = null;
    private ConversationSeachAdapter mAdapter = null;
    private EditText mEditText = null;
    private SearchMessageTask mSearchMessageTask = null;
    private TextView mEmptyTextView = null;
    public static final String SELECTED_MSG = "select_id";
    public static final String SEARCHED_STRING = "highlight";
    private RelativeLayout mSearchHeader = null;
    private long threadId;
    public int colRowId = -1;
    public int colThreadId = -1;
    public int colBody = -1;
    public int colDate = -1;
    private String recipienNames;
    /*
     * Subclass of TextView which displays a snippet of text which matches the full text and highlights the
     * matches within the snippet.
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
         * We have to know our width before we can compute the snippet string. Do that here and then defer to
         * super for whatever work is normally done.
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
                textFieldWidth -= (2F * ellipsisWidth); // assume we'll need one
                                                        // on both ends

                int offset = -1;
                int start = -1;
                int end = -1;
                /*
                 * TODO: this code could be made more efficient by only measuring the additional characters as
                 * we widen the string rather than measuring the whole new string each time.
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

                    // pull the candidate string out of the full text rather
                    // than body
                    // because body has been toLower()'ed
                    String candidate = mFullText.substring(start, end);
                    if (tp.measureText(candidate) > textFieldWidth) {
                        // if the newly computed width would exceed our bounds
                        // then we're done
                        // do not use this "candidate"
                        break;
                    }

                    snippetString = String.format("%s%s%s", start == 0 ? "" : sEllipsis, candidate,
                            end == bodyLength ? "" : sEllipsis);
                }
            }
            if (snippetString == null) {
                // required for large search String
                if (searchStringLength > bodyLength) {
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
                // remove the spans if it crashes
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
        this.threadId = getIntent().getLongExtra("thread_id", 0L);
        recipienNames=getIntent().getStringExtra("recipienNames");
        setContentView(R.layout.comp_search_activity);
        initView();
    }

    private void initView() {
        mEditText = (EditText) findViewById(R.id.searchEdit);
        mEditText.addTextChangedListener(mSearchWatcher);
        mEditText.setOnKeyListener(OnKeyListener);
        mEditText.setHint(mEditText.getHint()+" "+recipienNames);
        mListView = getListView();
        mListView.setItemsCanFocus(true);
        mListView.setFocusable(true);
        mListView.setClickable(true);
        mEmptyTextView = (TextView) findViewById(R.id.empty);
        mSearchHeader = (RelativeLayout) findViewById(R.id.searchScreenHeader);
        mEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private OnKeyListener OnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (mSearchMessageTask != null) {
                        mSearchMessageTask.cancel(true);
                    }
                    CharSequence searchString = ((EditText) v).getText();
                    if (searchString.length() == 0) {
                        mListView.setVisibility(View.GONE);
                        mEmptyTextView.setVisibility(View.GONE);
                        mSearchHeader.invalidate();
                        return true;
                    }
                    return true;
                }
            }
            return false;
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter = null;
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
            if (mSearchMessageTask != null) {
                mSearchMessageTask.cancel(true);
            }
            if (searchString.length() > 0) {
                mSearchMessageTask = new SearchMessageTask(searchString.toString());
                mSearchMessageTask.execute();
            } else {
                mListView.setVisibility(View.GONE);
                mEmptyTextView.setVisibility(View.GONE);
                mSearchHeader.invalidate();
            }
        }
    };

    private class SearchMessageTask extends AsyncTask<Void, Void, String> {
        private String searchString;
        private List<ConversationSearchItem> searchItemList = null;

        public SearchMessageTask(String searchString) {
            this.searchString = searchString;
        }

        protected String doInBackground(Void... params) {
            String pattern = Uri.encode(searchString);
            StringBuilder sb = null;
            pattern = pattern.replace("%20", " ");
            Uri uri = VZUris.getMmsSmsSearch().buildUpon().appendQueryParameter("pattern", pattern).build();
            Cursor cursor = ComposeMessageSearchActivity.this.getContentResolver().query(uri, null, null,
                    null, null);
            if (cursor == null) {
                return null;
            }
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    searchItemList = new ArrayList<ConversationSearchItem>();
                    cursor.moveToFirst();
                    int cp = cursor.getPosition();
                    sb = new StringBuilder();
                    while (cursor.isAfterLast() == false) {
                        final long tid = cursor.getLong(cursor.getColumnIndex("thread_id"));
                        if (threadId == tid) {
                            long time = cursor.getLong(cursor.getColumnIndex("date"));
                            long mid = cursor.getLong(cursor.getColumnIndex("_id"));
                            
                            if (time < Integer.MAX_VALUE) {
                                time = time * 1000;
                                //if its mms msgid, pass it as -ve
                                mid = -mid;
                            } 
                            sb.append(mid);
                            sb.append(":");
                            String timsestamp = MessageUtils.formatTimeStampString(time, false);
                            // msgSource field will be updated in later version
                            String msgSource = " ";
                            ConversationSearchItem searchItem = new ConversationSearchItem(tid, mid,
                                    cursor.getString(cursor.getColumnIndex("body")), timsestamp, msgSource);
                            searchItemList.add(searchItem);
                        }
                        cursor.moveToNext();
                    }
                    cursor.moveToPosition(cp);
                    
                    if (Logger.IS_DEBUG_ENABLED) {
                        Logger.debug("ALL ids" + sb.toString());
                    }
                }
                
                cursor.close();
            }
            
            if(sb != null) {
                return sb.toString();
            }
            return null;
        }

        protected void onPostExecute(String msgIds) {
            if (msgIds != null) {
                if (Logger.IS_DEBUG_ENABLED) {
                    Logger.debug("ALL Msg Ids:" + msgIds);
                }
                ConversationSeachAdapter adapter = mAdapter;
                if (searchItemList.size() > 0) {
                    mListView.setVisibility(View.VISIBLE);
                    mEmptyTextView.setVisibility(View.GONE);
                }
                mAdapter = new ConversationSeachAdapter(ComposeMessageSearchActivity.this, searchItemList,
                        searchString, msgIds);
                setListAdapter(mAdapter);
                if (adapter != null) {
                    adapter.clear();
                }
                if (searchItemList.size() == 0) {
                    mListView.setVisibility(View.GONE);
                    mEmptyTextView.setVisibility(View.VISIBLE);
                }
            } else {
                mListView.setVisibility(View.GONE);
                mEmptyTextView.setVisibility(View.VISIBLE);
            }
        }
    }

}