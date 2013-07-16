/**
 * SyncStatus.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.vzw.vma.sync;

/**
 * This class/interface   
 * @author Jegadeesan.M
 * @Since  Jun 18, 2012
 */
public class SyncStatus {
    public long minUid;
    public long maxUid;
    public long minModSeq;
    public long maxModSeq;
    public boolean syncCompleted;
    public long partialSyncModSeq;
    
    
    
    /*
     * Overriding method (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("MinUID=" + minUid);
        buffer.append(",MinModseq=" + minModSeq);
        buffer.append(",MaxUID=" + maxUid);
        buffer.append(",MaxModseq=" + maxModSeq);
        buffer.append(",FullSync=" + syncCompleted);
        buffer.append(",partialSyncModSeq=" + partialSyncModSeq+".");
        return buffer.toString();
    }
}
