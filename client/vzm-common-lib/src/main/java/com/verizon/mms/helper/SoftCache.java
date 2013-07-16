package com.verizon.mms.helper;

import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.strumsoft.android.commons.logger.Logger;

/**
 * @author "Animesh Kumar <animesh@strumsoft.com>"
 * 
 * @param <K>
 * @param <V>
 */
public class SoftCache<K, V> {

    /** The cache. */
    private final Map<K, SoftReference<V>> cache;
    private static final int DEFAULT_CACHE_SIZE = 10;

    public SoftCache() {
        this(DEFAULT_CACHE_SIZE); // Default size is 10.
    }

    public SoftCache(int size) {
        cache = new LRUCache<K, SoftReference<V>>(size);
    }

    public V getFromCache(K key) {
    	synchronized(this) {
    		if (cache.containsKey(key)) {
    			return cache.get(key).get();
    		}
    		return null;
    	}
    }
    
    public Set<K> keySet() {
    	synchronized(this) {
    		return new HashSet<K>(cache.keySet());
    	}
    }

    public void putToCache(K key, V value) {
    	synchronized(this) {
    		cache.put(key, new SoftReference<V>(value));
    	}
    }
    
    public SoftReference<V> removeFromCache(K key) {
    	synchronized(this) {
    		return cache.remove(key);
    	}
    }
    
    public void clear() {
    	synchronized (this) {
    		cache.clear();
    	}
    }

	public Iterator<SoftReference<V>> valueIterator() {
		synchronized (this) {
			return cache.values().iterator();
		}
	}

    /**
     * Testing purpose only
     */
    public void dumpKeys() {
    	Set<K> keys;
    	synchronized(this) {
    		keys = cache.keySet();
    	}
    	int i=0;
    	for (K key : keys) {
    		Logger.debug(getClass(), "**** cache key " + i++ + "=" + key +  "***");
    	}
    }

    public class LRUCache<A, B> extends LinkedHashMap<A, B> {

        /**
         * Default value
         */
        private static final long serialVersionUID = 1L;

        private int               maxEntries;

        public LRUCache(int maxEntries) {
            // removeEldestEntry() is called after a put(). To allow maxEntries in
            // cache, capacity should be maxEntries + 1 (+1 for the entry which will
            // be removed). Load factor is taken as 1 because size is fixed. This is
            // less space efficient when very less entries are present, but there
            // will be no effect on time complexity for get(). The third parameter
            // in the base class constructor says that this map is access-order
            // oriented.
            super(maxEntries + 1, 1, true);
            this.maxEntries = maxEntries;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<A, B> eldest) {
            // After size exceeds max entries, this statement returns true and the
            // oldest value will be removed. Since this map is access oriented the
            // oldest value would be least recently used.
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(getClass(), "removeEldestEntry: size = " + size() + ", max = " + maxEntries);
        	}
            return size() > maxEntries;
        }
    }

}