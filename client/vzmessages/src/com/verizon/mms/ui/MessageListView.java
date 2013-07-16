/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.verizon.mms.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.text.ClipboardManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ui.MessageListAdapter.ItemView;


public final class MessageListView extends ListView {
	private MessageListAdapter adapter;
	private boolean offBottom;
	private int topPos = INVALID_POSITION;
	private int topViewTop;
	private boolean touching;
	private boolean ignoreTouch;

	public MessageListView(Context context) {
		super(context);
		init();
	}

	public MessageListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// keep track of whether the user has scrolled off of the bottom view
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					final int count;
					final int numViews;
					offBottom = false;
					if ((count = getCount()) > 0 && (numViews = getChildCount()) > 0) {
						if (getFirstVisiblePosition() + numViews != count) {
							// last item isn't visible
							offBottom = true;
							setPositionInternal();
						}
						else {
							final int bottomViewBottom = getChildAt(numViews - 1).getBottom();
							final int listBottom = getBottom() - getListPaddingBottom();
							if (bottomViewBottom > listBottom) {
								// last item has been scrolled up
								offBottom = true;
								setPositionInternal();
							}
						}
					}
				}
				else {
					// user is scrolling: clear position
					offBottom = true;
					topPos = INVALID_POSITION;
				}

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(getClass(), "onScrollStateChanged: " + scrollState + ", offBottom = " + offBottom);
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			}
		});
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		this.adapter = (MessageListAdapter)adapter;
		super.setAdapter(adapter);
	}

	/**
	 * Attempts to preserve the current position of the items across the next layout.
	 * NB this won't set the position if we're scrolled to the bottom.
	 */
	public void setPosition() {
		// checkpoint the top view and position if we're not at the bottom
		topPos = INVALID_POSITION;
		final int numViews = getChildCount();
		if (numViews > 0) {
			final int count = getCount();
			if (count == 0 || getFirstVisiblePosition() + numViews != count) {
				// last item not visible
				setPositionInternal();
			}
			else {
				final int bottomViewBottom = getChildAt(numViews - 1).getBottom();
				final int listBottom = getBottom() - getListPaddingBottom();
				if (bottomViewBottom > listBottom) {
					// last item visible but scrolled
					setPositionInternal();
				}
			}
		}
	}

	private void setPositionInternal() {
		// NB assumes that we have at least one view
		topPos = getFirstVisiblePosition();
		final View view = getChildAt(0);
		topViewTop = view.getTop() - getTop() - getListPaddingTop();

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "setPosition: numViews = " + getChildCount() +
				", topPos = " + topPos + ", viewTop = " + topViewTop);
		}
	}

	/**
	 * Attempts to put the given item position at the top of the list on the next layout.
	 */
	public void setPosition(int position) {
		topPos = position;
		topViewTop = 0;
		offBottom = position != INVALID_POSITION;
	}

	@Override
	protected void layoutChildren() {
		// we maintain the current position if we have a checkpoint or we're showing the last item
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(getClass(), "layoutChildren: offBottom = " + offBottom +
				", topPos = " + topPos + ", touching = " + touching);
		}
		if (touching) {
			// assume the user is fast-scrolling
			if (!ignoreTouch) {
				topPos = INVALID_POSITION;
			}
			
			super.layoutChildren();
		}
		else {
			final int topPos = this.topPos;
			if (offBottom || topPos != INVALID_POSITION) {
				// we're off the bottom item or have a checkpoint: first do a normal layout
				super.layoutChildren();
	
				// if we have a valid checkpoint then restore its position
				final int count = getCount();
				if (count != 0) {
					if (topPos != INVALID_POSITION && topPos < count) {
						try {
							int numViews = getChildCount();
							if (numViews > 0) {
								// if the top item isn't the same then scroll to it
								final int curTop = getFirstVisiblePosition();
								if (Logger.IS_DEBUG_ENABLED) {
									Logger.debug(getClass(), "layoutChildren: after layout: numViews = " + numViews +
										", curTop = " + curTop + ", topPos = " + topPos);
								}
								if (curTop != topPos) {
									super.setSelection(topPos);
									super.layoutChildren();
									super.setSelection(INVALID_POSITION);
									numViews = getChildCount();
								}
	
								// offset views by the same amount from the top as when checkpointed
								final View view = getChildAt(0);
								final int top = view.getTop() - getTop() - getListPaddingTop();
								final int offset = topViewTop - top;
								if (Logger.IS_DEBUG_ENABLED) {
									Logger.debug(getClass(), "layoutChildren: after scroll: numViews = " + numViews +
										", curTop = " + getFirstVisiblePosition() + ", offset = " + offset);
								}
								if (offset != 0) {
									view.offsetTopAndBottom(offset);
									for (int i = 1; i < numViews; ++i) {
										getChildAt(i).offsetTopAndBottom(offset);
									}
									// layout again to fill in any space we may have opened up
									super.layoutChildren();
	
									if (Logger.IS_DEBUG_ENABLED) {
										Logger.debug(getClass(), "layoutChildren: after offset: numViews = " + getChildCount());
									}
								}
							}
						}
						catch (Throwable t) {
							Logger.error(t);
						}
					}
				}
			}
			else {
				// if we have an item at the bottom then keep it there
				final boolean checkBottom;
				boolean lastVisible = false;
				View beforeBottomView = null;
				final int count = getCount();
				if (count > 0) {
					final int numViews = getChildCount();
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(getClass(), "layoutChildren: before: numViews = " + numViews +
							", count = " + count + ", first = " + getFirstVisiblePosition());
					}
					if (numViews > 0) {
						// check if the last item in the adapter is visible
						if (lastVisible = (getFirstVisiblePosition() + numViews == count)) {
							beforeBottomView = getChildAt(numViews - 1);
							checkBottom = true;
	
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(getClass(), "layoutChildren: before: numViews = " + numViews +
									", bottom = " + ((ItemView)beforeBottomView.getTag()).msgItem);
							}
						}
						else {
							// last item not visible
							checkBottom = false;
						}
					}
					else {
						// no views before layout: check them after
						checkBottom = true;
					}
				}
				else {
					// no items in the adapter
					checkBottom = false;
				}
	
				// do the layout
				super.layoutChildren();
	
				if (checkBottom) {
					try {
						final int numViews = getChildCount();
						if (numViews > 0) {
							// if the bottom view is the same as it was then check if we need to scroll it up
							// due to its bottom edge being off the screen
							//
							final View afterBottomView = getChildAt(numViews - 1);
							if (lastVisible && adapter.sameItem(beforeBottomView, afterBottomView)) {
								final int childBottom = afterBottomView.getBottom();
								final int listBottom = getBottom() - getListPaddingBottom();
								final int offset = listBottom - childBottom;
								if (offset < 0) {
									// we don't scroll if the top of the view isn't visible, since that can lead to
									// its jumping up and down on subsequent layouts
									//
									final int childTop = afterBottomView.getTop();
									final int listTop = getTop() + getListPaddingTop();
									if (childTop > listTop) {
										afterBottomView.offsetTopAndBottom(offset);
										for (int i = numViews - 2; i >= 0; i--) {
											getChildAt(i).offsetTopAndBottom(offset);
										}
										if (Logger.IS_DEBUG_ENABLED) {
											Logger.debug(getClass(), "layoutChildren: after: numViews = " + numViews +
												", offset by = " + offset);
										}
									}
									else if (Logger.IS_DEBUG_ENABLED) {
										Logger.debug(getClass(), "layoutChildren: after: numViews = " + numViews +
											", childTop = " + childTop + ", listTop = " + listTop);
									}
								}
								else if (Logger.IS_DEBUG_ENABLED) {
									Logger.debug(getClass(), "layoutChildren: after: numViews = " + numViews + ", no offset");
								}
							}
							else {
								// new views or the last one has been shifted off the screen: re-layout to keep it on the screen
								if (Logger.IS_DEBUG_ENABLED) {
									final MessageItem item = adapter.getMessageItem(afterBottomView);
									Logger.debug(getClass(), "layoutChildren: after: numViews = " + numViews + ", " +
										(lastVisible ? "changed" : "new") + " bottom = " + item);
								}
	
								super.setSelection(count - 1);
								super.layoutChildren();
								super.setSelection(INVALID_POSITION);
	
								if (Logger.IS_DEBUG_ENABLED) {
									final int num = getChildCount();
									final MessageItem item = adapter.getMessageItem(getChildAt(num - 1));
									Logger.debug(getClass(), "layoutChildren: after: numViews = " + num +
										", new bottom after re-layout = " + item);
								}
							}
						}
					}
					catch (Throwable t) {
						Logger.error(t);
					}
				}
			}
		}
	}

	@Override
	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_C:
				final Adapter adapter = getAdapter();
				if (adapter instanceof MessageListAdapter) {
					final MessageItem item = ((MessageListAdapter)adapter).getSelectedMessageItem();
					if (item != null && item.isSms()) {
						ClipboardManager clip = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
						clip.setText(item.mBody);
						return true;
					}
				}
				break;
		}

		return super.onKeyShortcut(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			touching = true;
		}
		else if (ev.getAction() == MotionEvent.ACTION_UP) {
			touching = false;
		}
		return super.onTouchEvent(ev);
	}
	
	@Override
	protected void onDetachedFromWindow() {
		if (adapter != null) {
			adapter.cancelAllPreviewTask();
		}
		
		super.onDetachedFromWindow();
	}
	
	/*
	 * Sets the transcript mode to disabled in case there is a data change for which we dont want to 
	 * scroll the list to the end
	 */
	public void ignoreTouchOnCursorChange() {
		ignoreTouch = true;
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MessageListView.class, "ignoreTouch - setting the scroll mode to TRANSCRIPT_MODE_DISABLED");
		}
		/*if (touching)*/ {
			setTranscriptMode(TRANSCRIPT_MODE_DISABLED);
		}
	}

	public void cancelIgnoreTouchOnCursorChange() {
		ignoreTouch = false;
		
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(MessageListView.class, "cancel ignoreTouch - setting the scroll mode to TRANSCRIPT_MODE_ALWAYS_SCROLL");
		}
		setTranscriptMode(TRANSCRIPT_MODE_ALWAYS_SCROLL);
	}
	
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		
		if (ignoreTouch) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(MessageListView.class, ".draw setting the scroll mode to TRANSCRIPT_MODE_ALWAYS_SCROLL ");
			}
			ignoreTouch = false;
			setTranscriptMode(TRANSCRIPT_MODE_ALWAYS_SCROLL);
		}
	}
}
