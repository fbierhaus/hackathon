package com.verizon.mms;

import com.verizon.mms.Callback;
import com.verizon.mms.AccCallback;

interface MediaCacheApi {

	void accCache(AccCallback acb);

	void cache (long thread, Callback cb);
	
	void cacheWithoutClear (long thread, Callback cb);

	void cacheMms (long thread, long mms, Callback cb);
	
	void unregisterCallback (long thread);
}