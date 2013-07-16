package com.vzw.vzmessages;

public class Preferences {
	
	public static final String WIFI_NAME = "com.vzm.wifi.pref";
	
	public interface Keys {
		public static final String DEVICE_ID = "com.vzw.device.id";
		public static final String DEVICE_DESCRIPTION = "com.vzw.device.desc";
		public static final String PAIRED_DEVICES = "com.vzw.pair.paired";
		public static final String SYNC_RECV_DATA = "com.vzw.sync.recvdata";
		public static final String SYNC_TOSEND_DATA = "com.vzw.sync.tosenddata";
		public static final String CONNECTED_DEVICES = "com.vzw.connection.connections";
		public static final String PAIRED_DEVICE_MDN = "paired.mdn";
		public static final String SELF_MAC_ADDRESS = "com.vzw.wifi.self.mac_address";
		
		// boolean 
        public static final String IS_MSG_INDEXED = "vzm.wifi.intial_events_populated"; // common
		public static final String IS_PAIRED = "vzm.wifi.is_paired"; // common
		public static final String SYNC_ENABLED = "vzm.wifi.sync_enabled";
		public static final String IS_INITIAL_SYNC = "vzm.wifi.is_initial_sync"; // common
		
		// Intial sync data 
		public static final String FULLSYNC_SMS_COUNT = "vzm.wifi.fullsync_sms_count"; // common
		public static final String FULLSYNC_MMS_COUNT = "vzm.wifi.fullsync_mms_count"; // common
		public static final String FULLSYNC_SENT_COUNT = "vzm.wifi.lastSentCount";
    
        
        
        public static final String INDEXING_LAST_SMS_LUID = "wifi.indexing.last_sms_luid"; // common
        public static final String INDEXING_LAST_MMS_LUID = "wifi.indexing.last_mms_luid"; // common
        
        public static final String INDEXING_MAX_MMS_ID = "wifi.indexing.max_mms_id"; // common
        public static final String INDEXING_MAX_SMS_ID = "wifi.indexing.max_sms_id";
        
        public static final String INDEXING_THREAD_IDS = "wifi.indexing.threadIds"; // common
		
		
	}
}
