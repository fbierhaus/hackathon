package com.verizon.mms.util;

import java.util.Iterator;
import java.util.LinkedHashMap;

import android.os.SystemClock;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.mms.ui.MessageListAdapter;
import com.verizon.mms.ui.MessageListAdapter.SendingMessage;
import com.verizon.mms.util.BitmapManager.BitmapUser;


/**
 * Maintains the sum of the cached item sizes below a given limit, removing in LRU order as needed,
 * and optionally giving priority to retaining smaller messages.
 */
public class SizeCache<K, V extends MemoryItem> implements BitmapUser {
	private String tag;
	private long maxCacheSize;
	private CachePolicy policy;
	private int largeItem;
	private int itemSize;
	private final LinkedHashMap<K, V> map;
	private long cacheSize;


	public interface CachePolicy {
		/**
		 * Returns the maximum size to which the cache should grow.  Called to update the
		 * maxCacheSize before checking if a trim is needed.
		 */
		public long getMaxCacheSize();
	}


	/**
	 * @param tag identifying string for debugging
	 * @param initialCapacity initial size of map
	 * @param maxCacheSize max size in bytes to which cache should grow if policy
	 *        isn't specified
	 * @param itemSize order-of-magnitude estimate of overhead for items in cache
	 * @param largeItem limit between "small" and "large" items; if non-zero then small items
	 *        are preserved until all large items are freed when space is needed
	 */
	public SizeCache(String tag, int initialCapacity, long maxCacheSize, CachePolicy policy,
			int largeItem, int itemSize) {
		this.tag = tag;
		this.maxCacheSize = maxCacheSize;
		this.policy = policy;
		this.largeItem = largeItem;
		this.itemSize = itemSize;

		map = new LinkedHashMap<K, V>(initialCapacity, 1.0f, true);

		BitmapManager.INSTANCE.addBitmapUser(this);
	}

	/**
	 * Puts the item into the cache, adding its size and trimming if necessary.
	 */
	public V put(K key, V item) {
		synchronized (this) {
			// if an item already exists for this key then subtract its size
			final V oldItem = map.get(key);
			if (oldItem != null) {
				removeItem(oldItem);
			}

			map.put(key, item);

			// update the cache size and trim as needed
			updateSizeLocked();

			if (Logger.IS_DEBUG_ENABLED) {
				Logger.debug(this + ".put: num items = " + map.size() + " after adding: " + item);
			}

			return oldItem;
		}
	}

	/**
	 * Updates the size of the cache, presumably because the size of one or more of the
	 * items may have changed, trimming if necessary to keep within the max size.
	 */
	public void updateSize() {
		synchronized (this) {
			updateSizeLocked();
		}
	}

	private void updateSizeLocked() {
		if (policy != null) {
			maxCacheSize = policy.getMaxCacheSize();
		}
		cacheSize = getCachedSize();
		if (cacheSize > maxCacheSize) {
			trim();
		}
	}

	private void trim() {
		// try removing large items first (if defined)
		int limit = largeItem;
		long cacheSize = this.cacheSize;
		final long maxCacheSize = this.maxCacheSize;
		try {
			do {
				// remove items in LRU order until we're below the max
				final Iterator<V> iter = map.values().iterator();
				while (iter.hasNext()) {
					final V item = iter.next();
					final int size = getItemSize(item);
					if (size >= limit) {
						// remove item and return if that put us below the max
						iter.remove();
						removeItem(item, size);
						cacheSize -= size;
						if (cacheSize <= maxCacheSize) {
							if (Logger.IS_DEBUG_ENABLED) {
								Logger.debug(this + ".trim: new cacheSize = " + cacheSize);
							}
							return;
						}
					}
				}

				// now remove small items too
				limit -= largeItem;

				if (Logger.IS_DEBUG_ENABLED) {
					Logger.debug(this + ".trim: after large items: cacheSize = " + cacheSize);
				}
			}
			while (limit >= 0);
		}
		finally {
			this.cacheSize = cacheSize;
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.error(this + ".trim: cacheSize " + cacheSize + " still above " + maxCacheSize);
		}
	}

	private void removeItem(V item) {
		removeItem(item, getItemSize(item));
	}

	protected void removeItem(V item, int size) {
		if (Logger.IS_DEBUG_ENABLED) {
			if (!Thread.holdsLock(this)) {
				throw new IllegalStateException("Current thread not holding lock");
			}
		}

		cacheSize -= size;

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".removeItem: cacheSize = " + cacheSize + " after removing " + item);
		}
	}

	public V remove(K key) {
		synchronized (this) {
			final V item = map.remove(key);
			if (item != null) {
				removeItem(item);
			}
			return item;
		}
	}

	private long getCachedSize() {
		long start;
		if (Logger.IS_DEBUG_ENABLED) {
			start = SystemClock.uptimeMillis();
		}

		long size = 0;
		for (V item : map.values()) {
			size += getItemSize(item);
		}

		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".getCachedSize: in " + (SystemClock.uptimeMillis() - start) +
				"ms, cacheSize = " + cacheSize + ", maxSize = " + maxCacheSize);
		}

		return size;
	}

	private int getItemSize(V item) {
		return item.getMemorySize() + itemSize;
	}

	public V get(K key) {
		synchronized (this) {
			return map.get(key);
		}
	}

	public void clear() {
		synchronized (this) {
			// give the subclasses a chance to remove the items
			for (V item : map.values()) {
				removeItem(item, 0);
			}
			map.clear();
			cacheSize = 0;
		}
	}

	public void shutdown() {
		BitmapManager.INSTANCE.removeBitmapUser(this);
		clear();
	}

	@Override
	public void debugDump() {
		Logger.debug(MessageListAdapter.class, "------SizeCache--DebugDump------" + toString());
	}

	@Override
	public void freeBitmapMemory() {
		if (Logger.IS_DEBUG_ENABLED) {
			Logger.debug(this + ".freeBitmapMemory: cacheSize = " + cacheSize);
		}
		clear();
	}

	@Override
	public String toString() {
		return "SizeCache-" + tag + "@" + Integer.toHexString(hashCode());
	}
}