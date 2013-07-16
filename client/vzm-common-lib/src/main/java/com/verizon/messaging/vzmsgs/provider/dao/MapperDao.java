package com.verizon.messaging.vzmsgs.provider.dao;

import java.util.List;

import com.verizon.messaging.vzmsgs.provider.VMAMapping;

public interface MapperDao {

	public static final int SMS = 1;
	public static final int MMS = 2;
	
	public enum CallerSource {
		VMA,
		TELEPHONY
	}
	public VMAMapping findMappingByMessageIdAndBoxType(String messageId, int msgtype , int boxType);
	
	public VMAMapping findMappingByMessageId(String messageId, int msgtype);

	public VMAMapping findMappingByPduLuid(long luid, int typeSms);

	public VMAMapping findMappingByChecksum(long checksum, long timestamp, CallerSource src);
	
	public List<VMAMapping> findAllMappingsByChecksum(long checksum, CallerSource src, int messageBox);

	public VMAMapping findMappingByUid(long uid);
	
	public VMAMapping createMapping(VMAMapping inputmap);
	
	public int updateMappingWithluid(long id, long luid, long threadId);
	
	public int updateMessageIdAndFlags(long id, String msgId, int vmaflags);

	public int addPendingUiEvent(long id, int code);
	
	public int addPendingUiEvent(long id,long tempLuid,long oldLuid, int code);

	public int updateUidMessageIdSrcAndFlags(long id, long uid, String msgId, int vmaflags, int srcCode);

    public int updateUidTimestampAndFlags(long id, long uid, int code, long timestamp, int vmaflags);   

    public int deleteMapping(long id);

    /**
     * This Method 
     * @param threadId
     * @return
     */
    public List<VMAMapping> findMappingByPduThreadId(long threadId);
}
