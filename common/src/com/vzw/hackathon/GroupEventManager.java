package com.vzw.hackathon;

import org.apache.log4j.Logger;

import com.vzw.util.db.DBManager;
import com.vzw.util.db.DBPool;
import com.vzw.util.db.DBUtil;

public class GroupEventManager {
	private static final Logger	logger = Logger.getLogger(GroupEventManager.class);
	private static final DBPool dbPool = DBManager.getDBPool();
	
	private static final String SEL_GROUP_EVENT = 
			"select group_event_id as id, show_id as showId, channel_id as channelId, show_time as showTime, "
			+ " show_name as showName, master_mdn as masterMdn, create_time as createTime"
			+ " from GROUP_EVENT where group_event_id = ?";	
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	public GroupEvent loadGroupEventFromDb(int id) {
		GroupEvent ge = null;
		try {
			ge = DBUtil.query(dbPool, SEL_GROUP_EVENT, new DBUtil.BeanHandlerEx<GroupEvent>(GroupEvent.class), DBUtil.THROW_HANDLER, id);
		}
		catch (Exception e) {
			logger.error("Failed to load group event from db", e);
		}
		
		return ge;
	}

	

	/**
	 * 
	 */
	public void schedulePlay() {
		schedulePlay("");
	}
	
	/**
	 * schedule tuning a show
	 * @param mdn
	 * 
	 */
	public void schedulePlay(String mdn) {
		
	}
		
}
