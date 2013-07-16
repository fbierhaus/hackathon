package com.verizon.mms.ui.widget;

import com.strumsoft.android.commons.logger.Logger;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.widget.TextView;

public class JellyBeanSpanFixTextView extends TextView {
	private static final String TAG = JellyBeanSpanFixTextView.class.getSimpleName();

	public JellyBeanSpanFixTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public JellyBeanSpanFixTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public JellyBeanSpanFixTextView(Context context) {
		super(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		try {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		} catch (IndexOutOfBoundsException e) {
			Logger.debug(TAG, "onMeasure found IndexOutOfBoundsException");
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
		
		spans = newsequence.getSpans(0, newsequence.length(), URLSpan.class);
		for (Object span : spans) {
			newsequence.removeSpan(span);
		}
		
		spans = newsequence.getSpans(0, newsequence.length(), ImageSpan.class);
		for (Object span : spans) {
			newsequence.removeSpan(span);
		}
		
		return newsequence;
	}
}
