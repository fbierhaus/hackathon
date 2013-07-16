package com.verizon.mms.util;

import java.util.LinkedHashMap;
import java.util.Map;


@SuppressWarnings("serial")
public abstract class AbstractCache<K, V> extends LinkedHashMap<K, V> {
	private int maxSize;

	protected AbstractCache(int initialSize, int maxSize) {
		// remove entries in LRU order
		super(initialSize, 1.0f, true);
		this.maxSize = maxSize;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > maxSize;
	}
}
