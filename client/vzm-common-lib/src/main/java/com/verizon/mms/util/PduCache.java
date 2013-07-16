/*
 * Copyright (C) 2008 Esmertec AG.
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

package com.verizon.mms.util;

import java.util.HashMap;
import java.util.HashSet;

import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;
import android.provider.Telephony.Mms;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.common.VZUris;

@SuppressWarnings("serial")
public final class PduCache extends AbstractCache<Uri, PduCacheEntry> {

	private static final int INITIAL_SIZE = 20;
	private static final int MAX_SIZE = 100;

    private static final int MMS_ALL             = 0;
    private static final int MMS_ALL_ID          = 1;
    private static final int MMS_INBOX           = 2;
    private static final int MMS_INBOX_ID        = 3;
    private static final int MMS_SENT            = 4;
    private static final int MMS_SENT_ID         = 5;
    private static final int MMS_DRAFTS          = 6;
    private static final int MMS_DRAFTS_ID       = 7;
    private static final int MMS_OUTBOX          = 8;
    private static final int MMS_OUTBOX_ID       = 9;
    private static final int MMS_CONVERSATION    = 10;
    private static final int MMS_CONVERSATION_ID = 11;

    private static final UriMatcher URI_MATCHER;
    private static final HashMap<Integer, Integer> MATCH_TO_MSGBOX_ID_MAP;

    private static PduCache sInstance;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        final String mmsAuth = VZUris.getMmsAuthority();
        final String mmsSmsAuth = VZUris.getMmsSmsAuthority();
        URI_MATCHER.addURI(mmsAuth, null,         MMS_ALL);
        URI_MATCHER.addURI(mmsAuth, "#",          MMS_ALL_ID);
        URI_MATCHER.addURI(mmsAuth, "inbox",      MMS_INBOX);
        URI_MATCHER.addURI(mmsAuth, "inbox/#",    MMS_INBOX_ID);
        URI_MATCHER.addURI(mmsAuth, "sent",       MMS_SENT);
        URI_MATCHER.addURI(mmsAuth, "sent/#",     MMS_SENT_ID);
        URI_MATCHER.addURI(mmsAuth, "drafts",     MMS_DRAFTS);
        URI_MATCHER.addURI(mmsAuth, "drafts/#",   MMS_DRAFTS_ID);
        URI_MATCHER.addURI(mmsAuth, "outbox",     MMS_OUTBOX);
        URI_MATCHER.addURI(mmsAuth, "outbox/#",   MMS_OUTBOX_ID);
        URI_MATCHER.addURI(mmsSmsAuth, "conversations",   MMS_CONVERSATION);
        URI_MATCHER.addURI(mmsSmsAuth, "conversations/#", MMS_CONVERSATION_ID);

        MATCH_TO_MSGBOX_ID_MAP = new HashMap<Integer, Integer>();
        MATCH_TO_MSGBOX_ID_MAP.put(MMS_INBOX,  Mms.MESSAGE_BOX_INBOX);
        MATCH_TO_MSGBOX_ID_MAP.put(MMS_SENT,   Mms.MESSAGE_BOX_SENT);
        MATCH_TO_MSGBOX_ID_MAP.put(MMS_DRAFTS, Mms.MESSAGE_BOX_DRAFTS);
        MATCH_TO_MSGBOX_ID_MAP.put(MMS_OUTBOX, Mms.MESSAGE_BOX_OUTBOX);
    }

    private final HashMap<Integer, HashSet<Uri>> mMessageBoxes;
    private final HashMap<Long, HashSet<Uri>> mThreads;

    private PduCache() {
    	super(INITIAL_SIZE, MAX_SIZE);
        mMessageBoxes = new HashMap<Integer, HashSet<Uri>>();
        mThreads = new HashMap<Long, HashSet<Uri>>();
    }

    synchronized public static final PduCache getInstance() {
        if (sInstance == null) {
            sInstance = new PduCache();
        }
        return sInstance;
    }

	@Override
	synchronized public PduCacheEntry get(Object key) {
		final PduCacheEntry ret = super.get(key);
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), (ret == null ? "missed " : "hit ") + key);
    	}
		return ret;
	}

    synchronized public boolean add(Uri uri, PduCacheEntry entry) {
        int msgBoxId = entry.getMessageBox();
        HashSet<Uri> msgBox = mMessageBoxes.get(msgBoxId);
        if (msgBox == null) {
            msgBox = new HashSet<Uri>();
            mMessageBoxes.put(msgBoxId, msgBox);
        }

        long threadId = entry.getThreadId();
        HashSet<Uri> thread = mThreads.get(threadId);
        if (thread == null) {
            thread = new HashSet<Uri>();
            mThreads.put(threadId, thread);
        }

        boolean result = false;
        Uri finalKey = normalizeKey(uri);
        if (finalKey != null) {
	        result = super.put(finalKey, entry) != null;
	        if (result) {
	            msgBox.add(finalKey);
	            thread.add(finalKey);
	        }
        }
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "added uri " + uri + ", size = " + size());
    	}
        return result;
    }

    synchronized public PduCacheEntry purge(Uri uri) {
        int match = URI_MATCHER.match(uri);
        switch (match) {
            case MMS_ALL_ID:
                return purgeSingleEntry(uri);
            case MMS_INBOX_ID:
            case MMS_SENT_ID:
            case MMS_DRAFTS_ID:
            case MMS_OUTBOX_ID:
                String msgId = uri.getLastPathSegment();
                return purgeSingleEntry(Uri.withAppendedPath(VZUris.getMmsUri(), msgId));
            // Implicit batch of purge, return null.
            case MMS_ALL:
            case MMS_CONVERSATION:
                purgeAll();
                return null;
            case MMS_INBOX:
            case MMS_SENT:
            case MMS_DRAFTS:
            case MMS_OUTBOX:
                purgeByMessageBox(MATCH_TO_MSGBOX_ID_MAP.get(match));
                return null;
            case MMS_CONVERSATION_ID:
                purgeByThreadId(ContentUris.parseId(uri));
                return null;
            default:
                return null;
        }
    }

    private PduCacheEntry purgeSingleEntry(Uri key) {
        PduCacheEntry entry = super.remove(key);
        if (entry != null) {
            removeFromThreads(key, entry);
            removeFromMessageBoxes(key, entry);
        }
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "purgeSingleEntry: size = " + size() +
    			" after " + (entry == null ? "not " : "") + "purging " + key);
    	}
        return entry;
    }

    synchronized public void purgeAll() {
        super.clear();
        mMessageBoxes.clear();
        mThreads.clear();
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "purgeAll");
    	}
    }

    /**
     * @param uri The Uri to be normalized.
     * @return Uri The normalized key of cached entry.
     */
    private Uri normalizeKey(Uri uri) {
        int match = URI_MATCHER.match(uri);
        Uri normalizedKey = null;

        switch (match) {
            case MMS_ALL_ID:
                normalizedKey = uri;
                break;
            case MMS_INBOX_ID:
            case MMS_SENT_ID:
            case MMS_DRAFTS_ID:
            case MMS_OUTBOX_ID:
                String msgId = uri.getLastPathSegment();
                normalizedKey = Uri.withAppendedPath(VZUris.getMmsUri(), msgId);
                break;
        }

   	    if (Logger.IS_DEBUG_ENABLED) {
        	Logger.debug(getClass(), uri + " normalized to " + normalizedKey);
        }
        return normalizedKey;
    }

    private void purgeByMessageBox(Integer msgBoxId) {
        if (msgBoxId != null) {
            HashSet<Uri> msgBox = mMessageBoxes.remove(msgBoxId);
            if (msgBox != null) {
                for (Uri key : msgBox) {
                    PduCacheEntry entry = super.remove(key);
                    if (entry != null) {
                        removeFromThreads(key, entry);
                    	if (Logger.IS_DEBUG_ENABLED) {
                    		Logger.debug(getClass(), "purgeByMessageBox: removed " + key);
                    	}
                    }
                }
            }
        	if (Logger.IS_DEBUG_ENABLED) {
        		Logger.debug(getClass(), "purgeByMessageBox: size = " + size() +
        			" after " + (msgBox == null ? "not " : "") + "purging msg box " + msgBoxId);
        	}
        }
    }

    private void removeFromThreads(Uri key, PduCacheEntry entry) {
        HashSet<Uri> thread = mThreads.get(entry.getThreadId());
        if (thread != null) {
            thread.remove(key);
        }
    }

    private void purgeByThreadId(long threadId) {
        HashSet<Uri> thread = mThreads.remove(threadId);
        if (thread != null) {
            for (Uri key : thread) {
                PduCacheEntry entry = super.remove(key);
                if (entry != null) {
                    removeFromMessageBoxes(key, entry);
                	if (Logger.IS_DEBUG_ENABLED) {
                		Logger.debug(getClass(), "purgeByThreadId: removed " + key);
                	}
                }
            }
        }
    	if (Logger.IS_DEBUG_ENABLED) {
    		Logger.debug(getClass(), "purgeByThreadId: size = " + size() +
    			" after " + (thread == null ? "not " : "") + "purging thread " + threadId);
    	}
    }

    private void removeFromMessageBoxes(Uri key, PduCacheEntry entry) {
        HashSet<Uri> msgBox = mThreads.get(entry.getMessageBox());
        if (msgBox != null) {
            msgBox.remove(key);
        }
    }
}
