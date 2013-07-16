package com.verizon.mms.ui.widget;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Checkable;
import android.widget.TextView;

import com.verizon.messaging.vzmsgs.R;


public class MultilineCheckedTextView extends TextView implements Checkable {
	private boolean mChecked;
	private int mCheckMarkResource;
	private Drawable mCheckMarkDrawable;
	private int mCheckMarkWidth;

	private int subTextSize;
	private int subTextColor;
	private boolean hasSubText;
	private boolean selfChange;

	private static final int[] CHECKED_STATE_SET = {
		android.R.attr.state_checked
	};

	public MultilineCheckedTextView(Context context) {
		this(context, null);
	}

	public MultilineCheckedTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MultilineCheckedTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MultilineCheckedTextView, defStyle, 0);
		subTextSize = a.getDimensionPixelSize(R.styleable.MultilineCheckedTextView_subTextSize, 0);
		subTextColor = a.getColor(R.styleable.MultilineCheckedTextView_subTextColor, 0);
		hasSubText = subTextSize != 0 || subTextColor != 0;

		if (hasSubText) {
			setSpannable(getText());
		}

		Drawable d = a.getDrawable(R.styleable.MultilineCheckedTextView_checkMark);
		if (d != null) {
			setCheckMarkDrawable(d);
		}

		boolean checked = a.getBoolean(R.styleable.MultilineCheckedTextView_checked, false);
		setChecked(checked);

		a.recycle();
	}

	@Override
	protected void onTextChanged(CharSequence text, int start, int before, int after) {
		if (selfChange) {
			selfChange = false;
		}
		else if (hasSubText) {
			setSpannable(text);
		}
	}

	private void setSpannable(CharSequence text) {
		// if this text has multiple lines then format the lines after the first
		final String s = text.toString();
		final int i = s.indexOf('\n');
		if (i >= 0) {
			final int len = s.length();
			final SpannableString ss = new SpannableString(text);
			if (subTextSize != 0) {
				final AbsoluteSizeSpan sizeSpan = new AbsoluteSizeSpan(subTextSize, false);
				ss.setSpan(sizeSpan, i, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (subTextColor != 0) {
				final ForegroundColorSpan colorSpan = new ForegroundColorSpan(subTextColor);
				ss.setSpan(colorSpan, i, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			selfChange = true;
			setText(ss, BufferType.SPANNABLE);
		}
	}

	public void toggle() {
		setChecked(!mChecked);
	}

	@ViewDebug.ExportedProperty
	public boolean isChecked() {
		return mChecked;
	}

	/**
	 * <p>Changes the checked state of this text view.</p>
	 *
	 * @param checked true to check the text, false to uncheck it
	 */
	public void setChecked(boolean checked) {
		if (mChecked != checked) {
			mChecked = checked;
			refreshDrawableState();
		}
	}

	/**
	 * Set the checkmark to a given Drawable, identified by its resourece id. This will be drawn
	 * when {@link #isChecked()} is true.
	 * 
	 * @param resid The Drawable to use for the checkmark.
	 */
	public void setCheckMarkDrawable(int resid) {
		if (resid != 0 && resid == mCheckMarkResource) {
			return;
		}

		mCheckMarkResource = resid;

		Drawable d = null;
		if (mCheckMarkResource != 0) {
			d = getResources().getDrawable(mCheckMarkResource);
		}
		setCheckMarkDrawable(d);
	}

	/**
	 * Set the checkmark to a given Drawable. This will be drawn when {@link #isChecked()} is true.
	 *
	 * @param d The Drawable to use for the checkmark.
	 */
	public void setCheckMarkDrawable(Drawable d) {
		if (mCheckMarkDrawable != null) {
			mCheckMarkDrawable.setCallback(null);
			unscheduleDrawable(mCheckMarkDrawable);
		}
		if (d != null) {
			d.setCallback(this);
			d.setVisible(getVisibility() == VISIBLE, false);
			d.setState(CHECKED_STATE_SET);
			setMinHeight(d.getIntrinsicHeight());
			mCheckMarkWidth = d.getIntrinsicWidth();
			d.setState(getDrawableState());
		}
		mCheckMarkDrawable = d;
		requestLayout();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		final Drawable checkMarkDrawable = mCheckMarkDrawable;
		if (checkMarkDrawable != null) {
			final int verticalGravity = getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
			final int height = checkMarkDrawable.getIntrinsicHeight();

			int y = 0;

			switch (verticalGravity) {
				case Gravity.BOTTOM:
					y = getHeight() - height;
					break;
				case Gravity.CENTER_VERTICAL:
					y = (getHeight() - height) / 2;
					break;
			}

			final int width = mCheckMarkWidth;
			int left = (getPaddingLeft() - width) / 2;
			if (left < 0) {
				left = 0;
			}
			checkMarkDrawable.setBounds(left, y, left + width, y + height);
			checkMarkDrawable.draw(canvas);
		}
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();

		if (mCheckMarkDrawable != null) {
			int[] myDrawableState = getDrawableState();

			// Set the state of the Drawable
			mCheckMarkDrawable.setState(myDrawableState);

			invalidate();
		}
	}

	@Override
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		boolean populated = super.dispatchPopulateAccessibilityEvent(event);
		if (!populated) {
			event.setChecked(mChecked);
		}
		return populated;
	}
}
