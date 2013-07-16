package com.verizon.mms.ui.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ui.widget.CursorAdapter;


/**
 * A CursorAdapter with some performance optimizations.
 */
public abstract class FastCursorAdapter extends CursorAdapter {
	protected boolean cursorChanging;
	private ChangeListener changeListener;
	protected boolean closed;
	private long maxDelayTime;
	protected boolean closeOnChange = true;
	private static ArrayList<Cursor> openCursors;


	// we buffer content change notifications to reduce thrashing, but need to be
	// responsive to status changes when too long a delay might be objectionable
	protected static final long MINIMUM_CONTENT_CHANGE_DELAY = 100;
	protected static final long DEFAULT_CONTENT_CHANGE_DELAY = 1000;
	private static final long MAX_DELAY = 5000;  // update at least this often
	private static final long CONTENT_CHANGE_CHECK_DELAY = 300;  // check back after this period

	private static final int MSG_CONTENT_CHANGED = 1;
	private static final int MSG_CONTENT_CHANGE_CHECK = 2;


	static {
		if (Logger.IS_DEBUG_ENABLED) {
			openCursors = new ArrayList<Cursor>();
		}
	}


	protected interface ChangeListener {
		/**
		 * Called when the content of the cursor may have changed.
		 */
		public void onContentChanged();

		/**
		 * Called when the cursor is about to be changed.  The return value is used
		 * as the cursor to change, so the callee can substitute a new cursor if desired.
		 * @param cursor Cursor about to be changed to
		 * @return Cursor to change to; return the cursor param if no change is desired
		 */
		public Cursor onCursorChanging(Cursor cursor);
	}


	public FastCursorAdapter(Context context, Cursor c, boolean autoRequery) {
		super(context, c, autoRequery);
	}

	@Override
	public void changeCursor(Cursor newCursor) {
		changeCursor(newCursor, false);

		if (Logger.IS_DEBUG_ENABLED) {
			// remove any closed cursors from the open list
			for (int i = openCursors.size() - 1; i >= 0; --i) {
				final Cursor cursor = openCursors.get(i);
				if (cursor.isClosed()) {
					openCursors.remove(i);
				}
			}

			// add the new one
			if (newCursor != null) {
				openCursors.add(newCursor);
			}

			Logger.debug(this + ".changeCursor: after change: open cursors = " + openCursors);
		}
	}

	/**
     * Change the underlying cursor to a new cursor.
	 * NB we only close the old cursor if the closeOnChsnge flag is set.
	 * @param force Always perform the change even if the cursor contents haven't changed
	 * @return The old cursor or null if none
	 */
	protected void changeCursor(Cursor newCursor, boolean force) {
		final Cursor oldCursor = getCursor();

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".changeCursor: force = " + force + ", closeOnChange = " + closeOnChange +
				", closed = " + closed + ", cursorChanging = " + cursorChanging + ", oldCursor = " + oldCursor +
				", newCursor = " + newCursor + ", old / new size = " +
				(oldCursor == null ? -1 : oldCursor.getCount()) + " / " +
				(newCursor == null ? -1 : newCursor.getCount()));
		}

		if (!closed) {
			// if the cursors are both non-null then check if their counts are different
			boolean changed = true;
			if (!force) {
				if (oldCursor != null && newCursor != null) {
					if (oldCursor.getCount() == newCursor.getCount()) {
						// give the subclass a chance to check if the contents are different
						changed = isCursorChanged(oldCursor, newCursor);
					}
					else {
						// assume this is the insert or delete we've been told to expect and reset the changing flag
						cursorChanging = false;
					}
				}
				// otherwise it's unchanged if they're both null
				else if (oldCursor == null && newCursor == null) {
					changed = false;
				}
				else if (newCursor != null && newCursor.getCount() != 0) {
					// reset changing flag on assumption that this is the change we've been told to expect
					cursorChanging = false;
				}
			}

			if (changed) {
				// clear any outstanding requests if the cursor is being cleared
				if (newCursor == null) {
					handler.removeCallbacksAndMessages(null);
				}
	
				// notify listener and let it give us a new cursor if desired
				final ChangeListener changeListener = this.changeListener;
				if (changeListener != null) {
					newCursor = changeListener.onCursorChanging(newCursor);
				}
	
				// change the cursor and conditionally close the old one
				super.swapCursor(newCursor);
	
				if (closeOnChange && oldCursor != null && oldCursor != newCursor) {
					oldCursor.close();
				}
			}
		}
	}

	/**
	 * Returns true if the cursors are different.  The default implementation always returns true;
	 * a subclass may want to check the cursor data if it's efficient to do so and its display is
	 * only derived from the cursor data or other easily obtainable data.
	 *
	 * @return True if the two cursors are different as far as the adapter is concerned
	 */
	protected boolean isCursorChanged(Cursor oldCursor, Cursor newCursor) {
		return true;
	}

	@Override
	public void onContentChanged() {
		onContentChanged(false);
	}

	public void onContentChanged(boolean ignoreDelay) {
		// notification messages can get buffered up in the message queue and delivered after we're
		// closed or invalidated, so make sure we're valid before notifying the listener
		if (isValid()) {
			synchronized (handler) {
				// clear all messages
				handler.removeCallbacksAndMessages(null);

				// check if we should buffer notifications to avoid unnecessary thrashing
				long delay = getContentChangeDelay();
				if (delay == -1) {
					// adapter is busy, so check again after a short delay
					handler.sendEmptyMessageDelayed(MSG_CONTENT_CHANGE_CHECK, CONTENT_CHANGE_CHECK_DELAY);
					if (Logger.IS_DEBUG_ENABLED) {
						Logger.debug(this + ".onContentChanged: adapter busy, deferring");
					}
				}
				else {
					if (ignoreDelay) {
						delay = 0;
					}
					else {
						final long curTime = SystemClock.uptimeMillis();
						if (maxDelayTime != 0) {
							// make sure we don't go past the max delay time
							final long delta = maxDelayTime - curTime;
		
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(this + ".onContentChanged: cursorChanging = " + cursorChanging +
									", curTime = " + curTime + ", delta = " + delta + ", delay = " + delay);
							}
		
							if (delay >= delta) {
								delay = delta;
							}
						}
						else {
							// set the max delay time from now
							maxDelayTime = curTime + MAX_DELAY;
		
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(this + ".onContentChanged: cursorChanging = " + cursorChanging +
									", curTime = " + curTime + ", delay = " + delay);
							}
						}
					}

					// notify the listener now or schedule it if delayed
					if (delay <= 0) {
						notifyListener();
					}
					else {
						handler.sendEmptyMessageDelayed(MSG_CONTENT_CHANGED, delay);
					}
				}
			}
		}
		else if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".onContentChanged: invalid");
		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(FastCursorAdapter.this + "(FastCursorAdapter).handleMessage: msg = " + msg);
			}
			final int what = msg.what;
			if (what == MSG_CONTENT_CHANGE_CHECK) {
				// call back after a deferred content change
				onContentChanged(true);
			}
			else {  // MSG_CONTENT_CHANGED
				notifyListener();
			}
		}
	};

	private void notifyListener() {
		synchronized (handler) {
			maxDelayTime = 0;
			if (changeListener != null) {
				changeListener.onContentChanged();
			}
		}
	}

	/**
	 * Returns the minimum amount of time desired between cursor content change notifications,
	 * or -1 if we should check again after a short delay.
	 * Default implementation returns a minimal delay if it has been told that the cursor is
	 * expected to change soon and a long delay otherwise, so subclasses should handle other cases.
	 * 
	 * @return Delay time in milliseconds
	 */
	protected long getContentChangeDelay() {
		// if the cursor is expected to change then return a minimal delay so that we update the display quickly
		return cursorChanging ? MINIMUM_CONTENT_CHANGE_DELAY : DEFAULT_CONTENT_CHANGE_DELAY;
	}

	/**
	 * Tells us whether the cursor is expected to change in size due to an insert or delete
	 * (e.g. a message being sent or deleted) so that we can be more responsive to updates.
	 *
	 * @param cursorChanging True if cursor count is expected to change soon
	 */
	public void setCursorChanging(boolean cursorChanging) {
		this.cursorChanging = cursorChanging;
	}

	public boolean getCursorChanging() {
		return cursorChanging;
	}

	public void setChangeListener(ChangeListener listener) {
		synchronized (handler) {
			changeListener = listener;
		}
	}

	private boolean isValid() {
		final Cursor cursor;
		return !closed && (cursor = getCursor()) != null && !cursor.isClosed();
	}

	protected void shutdown() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".shutdown");
		}
		synchronized (handler) {
			handler.removeCallbacksAndMessages(null);
			super.changeCursor(null);
			changeListener = null;
			closed = true;
		}
	}
}
