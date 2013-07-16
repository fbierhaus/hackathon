/*
 * Copyright (C) 2008 SocialMuse, Inc.
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

package com.verizon.mms.util;

import java.util.HashMap;


// MRU memory cache

public class MemoryCacheMap<K, V> {
	private HashMap<K, MemoryCacheObject<V>> map;
	private int cacheSize;  // max entries in cache
	private K oldestKey;
	private String tag = "";


	public MemoryCacheMap(int cacheSize) {
		map = new HashMap<K, MemoryCacheObject<V>>(cacheSize);
		this.cacheSize = cacheSize;
	}

	public V put(K key, V value) {
//		if (MMSLogger.IS_DEBUG_ENABLED) {
//			if (tag == "") {
//				tag = value.getClass().getSimpleName();
//			}
//			log.debug("MemoryCacheMap<" + tag + ">.put(" + key + ", " + value + ")");
//		}
		synchronized (map) {
			if (oldestKey == null) {
				oldestKey = key;
			}
			else {
				trim();
			}
			MemoryCacheObject<V> co = new MemoryCacheObject<V>(value, System.currentTimeMillis());
			co = map.put(key, co);
			return co == null ? null : co.get();
		}
	}

	public V get(K key) {
		synchronized (map) {
			final MemoryCacheObject<V> co = map.get(key);
			if (co != null) {
				co.setTime(System.currentTimeMillis());
			}
//			if (MMSLogger.IS_DEBUG_ENABLED) {
//				log.debug("MemoryCacheMap<" + tag + ">.get(" + key + "): cache " + (co == null ? "miss" : "hit"));
//			}
			return co == null ? null : co.get();
		}
	}

	public void clear() {
		synchronized (map) {
			map.clear();
		}
	}

	public V remove(Object key) {
		synchronized (map) {
			final MemoryCacheObject<V> co = map.remove(key);
			return co == null ? null : co.get();
		}
	}

	// remove oldest member if necessary
	private void trim() {
		synchronized (map) {
			if (map.size() >= cacheSize && oldestKey != null) {
				// remove oldest
				final MemoryCacheObject<V> oldest = map.remove(oldestKey);
//				if (MMSLogger.IS_DEBUG_ENABLED) {
//					log.debug("MemoryCacheMap<" + tag + ">.trim: removed " + oldest.get());
//				}

				// set new oldest
				K oldestKey = null;
				long oldestTime = Long.MAX_VALUE;
				for (final K key : map.keySet()) {
					final MemoryCacheObject<V> co = map.get(key);
					long time = co.getTime();
					if (time < oldestTime) {
						oldestTime = time;
						oldestKey = key;
					}
				}
				this.oldestKey = oldestKey;
			}
		}
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public int size() {
		return map.size();
	}

	/**
	 * Add all the cache's objects to this cache.
	 */
	public void add(MemoryCacheMap<K, V> cache) {
		final HashMap<K, MemoryCacheObject<V>> map = cache.map;
		for (K key : map.keySet()) {
			put(key, map.get(key).get());
		}
	}
}
