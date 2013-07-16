package com.vzw.vma.sync.action;

import java.util.LinkedHashMap;

import com.strumsoft.android.commons.logger.Logger;

public class UpdatedItemsCache {

	public static final int MAX_CACHE_SIZE = 1000;
	private LinkedHashMap<Long, Long> updateUidsCache;
	private int maxSize;
	
	private static UpdatedItemsCache instance;
	public static UpdatedItemsCache getInstance() {
		if(instance == null) {
			synchronized(UpdatedItemsCache.class) {
				if(instance == null) {
					instance = new UpdatedItemsCache();
				}
			}
		}
		return instance;
	}
	
	private UpdatedItemsCache() {
		if(Logger.IS_DEBUG_ENABLED) {
			Logger.debug("UpdatedItemsCache: init");
		}
		updateUidsCache = new LinkedHashMap<Long, Long>() {
			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<Long, Long> eldest) {
				return size() > MAX_CACHE_SIZE;
			}
			
		};
	}
	
	public synchronized long getMCR(long uid) {
		Long mcr = updateUidsCache.get(uid);
		if(mcr == null) {
			return 0;
		} else {
			return mcr;
		}
	}
	
	public synchronized  void addMCR(long uid, long mcr) {
		
		if(Logger.IS_DEBUG_ENABLED) {
			if(updateUidsCache == null)
			Logger.debug("UpdatedItemsCache: updateUidsCache=" + updateUidsCache + " uid=" + uid + " mcr=" + mcr);
		}
		 updateUidsCache.put(uid, mcr);
	}

	public synchronized long removeUid(long uid) {
		return updateUidsCache.remove(uid);
	}
}
