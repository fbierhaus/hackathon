package com.verizon.mms.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils.TruncateAt;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import com.verizon.mms.data.ContactList;
import com.verizon.mms.ui.ConversationListAdapter.ContactNameData;
import com.verizon.mms.ui.adapter.RestoreConversationAdapter.ContactData;

public class FromTextView extends TextView {
	private CharSequence baseText;
	private int plusSuffixLen;
	private int numNames;
	private int numTrimmed;
	private String suffix;
	private int lastOrientation;
	private boolean internalSet;
	private StringBuilder builder;
	private String spanString = null;
	private static final char SEPARATOR = ',';


	public FromTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public FromTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public FromTextView(Context context) {
		super(context);
		init();
	}

	private void init() {
		lastOrientation = getResources().getConfiguration().orientation;
		builder = new StringBuilder();
		setEllipsize(TruncateAt.END);
	}

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			requestLayout();
		}
	};

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		// if this text has multiple names and has been ellipsized then try to trim the last one and append "+N"
		if (numNames > 1) {
			final Layout layout = getLayout();
			if (layout != null) {
				final int lines = layout.getLineCount();
//				if (MMSLogger.IS_DEBUG_ENABLED) {
//					log.debug("lines = " + lines + ", start = " + getLayout().getEllipsisStart(lines - 1) + ", text = " + super.getText());
//				}
				if (lines > 0) {
					int end = layout.getEllipsisStart(lines - 1);
					if (end > 0) {
						// has been ellipsized: trim last name off and add +N
						// if this causes it to ellipsize again then we'll get called back and try again
						final CharSequence text = super.getText();

						// if we haven't trimmed any yet then we need to count the ones beyond the ellipsis point
						int numTrimmed = this.numTrimmed;
						if (numTrimmed == 0) {
							final int len = text.length();
							for (int i = end; i < len; ++i) {
								if (text.charAt(i) == SEPARATOR) {
									++numTrimmed;
								}
							}
						}
						this.numTrimmed = numTrimmed;

						// now find end of previous name if any
						end -= plusSuffixLen;  // start before +N and/or suffix if any
						while (--end > 0 && text.charAt(end) != SEPARATOR);
						if (end > 0) {
							// found one to trim
							if (++numTrimmed >= numNames) {
								numTrimmed = numNames - 1;
							}
							this.numTrimmed = numTrimmed;

							// create +N suffix, append it to the trimmed names, add suffix if any, and set it
							final StringBuilder sb = builder;
							sb.setLength(0);
							sb.append(text.subSequence(0, end));
							sb.append(" + ");
							sb.append(numTrimmed);
							if (suffix != null) {
								sb.append(suffix);
							}
							plusSuffixLen = sb.length() - end;
							internalSet = true;
							
							if (spanString == null) {
								setText(sb.toString());
							} else {
								setText(getSpannableString(sb.toString(), 0, spanString));
							}

							// queue a layout request for after this one is done
							handler.sendEmptyMessage(0);
						}
					}
				}
			}
		}
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		final int orientation = newConfig.orientation;
		if (orientation != lastOrientation) {
			// the view has changed width, so reset to the original text and re-layout
			lastOrientation = orientation;
			setText(getText());
			handler.sendEmptyMessage(0);
		}
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int before, int after) {
		if (internalSet) {
			// ignore this since it's ours
			internalSet = false;
		}
		else {
			// reset to base state
			numTrimmed = plusSuffixLen = 0;
		}
	}

	private void internalSetText(String text, int numNames) {
		// check if the data is the same
		if (numNames == this.numNames) {
			if (baseText != null) {
				if (baseText.equals(text)) {
					return;
				}
			}
			else if (text == null) {
				// both null
				return;
			}
		}
		this.numNames = numNames;
		baseText = text;
		setText(getText());
	}

	private void internalSetText(String text, int numNames, Spannable span) {
		// check if the data is the same
		if (numNames == this.numNames) {
			if (baseText != null) {
				if (baseText.equals(span)) {
					return;
				}
			}
			else if (text == null) {
				// both null
				return;
			}
		}
		this.numNames = numNames;
		baseText = span;
		setText(span);
	}

	/**
	 * Sets the text and multiple-name flag from the given data.
	 */
	public void setNames(ContactNameData names) {
		final String text;
		final int numNames;
		if (names != null) {
			text = new String(names.names);
			numNames = names.num;
		}
		else {
			text = "";
			numNames = 0;
		}
		internalSetText(text, numNames);
	}
	
	
	/**
     * Sets the text and multiple-name flag from the given data.
     */
    public void setContactNames(ContactData names) {
        final String text;
        final int numNames;
        if (names != null) {
            text = new String(names.names);
            numNames = names.num;
        }
        else {
            text = "";
            numNames = 0;
        }
        internalSetText(text, numNames);
    }
	

	/**
	 * Sets the text appropriately from the given contact list.
	 */
	public void setNames(ContactList list) {
		final String text;
		final int numNames;
		if (list != null) {
			text = list.formatNames();
			numNames = list.size();
		}
		else {
			text = "";
			numNames = 0;
		}
		internalSetText(text, numNames);
	}
	
	/**
	 * Sets the text appropriately from the given contact list.
	 */
	public void setNames(ContactList list, String span) {
		final String text;
		final int numNames;
		if (list != null) {
			text = list.formatNames(",", false);
			numNames = list.size();
		}
		else {
			text = "";
			numNames = 0;
		}
		this.spanString = span;
		internalSetText(text, numNames, getSpannableString(text, 0, span));
	}

    /**
     * 
     * This Method return bold spannable for sting match.
     * 
     * @param target
     *            string data.
     * @param startIndex
     *            index from where the search should begin.
     * @return
     */

    private Spannable getSpannableString(String target, int startIndex, String searchString) {
        Spannable spaned = new SpannableString(target);
        final String search = searchString.toLowerCase();
        final int len = search.length();
        if (len > 0) {
	        // TODO: add multiple support
        	target = target.toLowerCase();
	        int start = target.indexOf(search, startIndex);
	        while (start >= 0) {
	            int end = start + len;
	            spaned.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end,
	                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	            startIndex = end;
	            start = target.indexOf(search, startIndex);
	        }
        }

        return spaned;
    }
    
	/**
	 * Sets a suffix string that is always appended to the contact names, potentially after the "+ N".
	 * Must be called before calling setNames().
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	@Override
	public CharSequence getText() {
		return suffix != null ? baseText + suffix : baseText;
	}
	public void setText(String text) {
	    this.numNames = 0;
	    this.suffix = null;
        baseText = text;
        setText(getText());
      
    }

	public void resetBaseText() {
        baseText = null;
    }
}
