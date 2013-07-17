package com.vzw.hackathon;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.vzw.hackathon.apihandler.ComcastAPIHandler;
import com.vzw.util.db.DBManager;
import com.vzw.util.db.DBPool;
import com.vzw.util.db.DBUtil;

public class GroupEventManager {
	private static final Logger	logger = Logger.getLogger(GroupEventManager.class);
	private static boolean TEST = true;
	
	
	private  DBPool dbPool = null;
	
	private static final String SQL_SEL_GROUP_EVENT = 
			"select group_event_id as id, show_id as showId, channel_id as channelId, show_time as showTime, "
			+ " show_name as showName, master_mdn as masterMdn, create_time as createTime"
			+ " from GROUP_EVENT where group_event_id = ?";	
	
	private static final String SQL_GET_NEXT_GROUP_EVENT_ID = 
			"VALUES (NEXT VALUE FOR GROUP_EVENT_PK_SEQ)";
	
	private static final String SQL_CREATE_GROUP_EVENT = 
			"INSERT INTO GROUP_EVENT (GROUP_EVENT_ID, SHOW_ID, CHANNEL_ID, SHOW_TIME, SHOW_NAME, MASTER_MDN"
			+ ") VALUES ("
			+ "	?, ?, ?, ?, ?, ?)";

	private static final String SQL_ADD_GROUP_MEMBER =
			"INSERT INTO GROUP_MEMBER ("
			+ "	GROUP_EVENT_ID,	MDN, MEMBER_STATUS) VALUES (?, ?, ?)";
	
	
	private static final String SQL_UPDATE_MEMBER_STATUS =
			"update GROUP_MEMBER set MEMBER_STATUS = ? where GROUP_EVENT_ID = ? and MDN = ?";
	
	
	private static final String SQL_UPDATE_MEMBER_LAST_CHANNEL_ID = 
			"update GROUP_MEMBER set LAST_CHANNEL_ID = ? where GROUP_EVENT_ID = ? and MDN = ?";
	
	private static final String SQL_GET_CHANNEL = 
			"select channel_id as \"id\", channel_name as \"name\", channel_desc as \"desc\" "
			+ " from channels where channel_id = ?";
	
	private static final String SQL_GET_USER = 
			"select mdn, channel_id as channelId, name from users"
			+ " where mdn = ?";
	
	
	private ScheduledExecutorService		scheduler = null;
	
	// key: groupEventId + mdn
	private Set<String>						scheduledEvent = new HashSet<String>();
	
	
	private static GroupEventManager instance = null;
	
	static {
		instance = new GroupEventManager();
	}
	
	public static GroupEventManager getInstance() {
		return instance;
	}
	private GroupEventManager() {
		dbPool = DBManager.getDBPool();
		scheduler = Executors.newScheduledThreadPool(20);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				GroupEventManager.getInstance().destroy();
			}
		});
	}
	
	public void destroy() {
		if (scheduler != null) {
			scheduler.shutdown();
			scheduler = null;
		}
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 * 
	 * This method does not load members
	 */
	public GroupEvent loadGroupEventFromDb(int id) {
		GroupEvent ge = null;
		try {
			ge = DBUtil.query(dbPool, SQL_SEL_GROUP_EVENT, new DBUtil.BeanHandlerEx<GroupEvent>(GroupEvent.class), DBUtil.THROW_HANDLER, id);
		}
		catch (Exception e) {
			logger.error("Failed to load group event from db", e);
		}
		
		return ge;
	}
	
	/**
	 * 
	 * @param ge
	 */
	public int createGroupEvent(GroupEvent ge) {
		int id = -1;
		try {
			// get group event id
			int geId = DBUtil.query(
					dbPool, 
					SQL_GET_NEXT_GROUP_EVENT_ID,
					new ScalarHandler<Integer>(),
					DBUtil.THROW_HANDLER
					);
			
			// insert group event
			DBUtil.update(dbPool, SQL_CREATE_GROUP_EVENT, DBUtil.THROW_HANDLER, 
					geId, ge.getShowId(), ge.getChannelId(), 
					new Timestamp(ge.getShowTime().getTime()), ge.getShowName(), ge.getMasterMdn());
			
			// insert members (including master mdn)
			for (Member m : ge.getMemberList()) {
				DBUtil.update(dbPool, SQL_ADD_GROUP_MEMBER, DBUtil.THROW_HANDLER, 
						geId, m.getMdn(), MemberStatus.INVITED.name());
			}
			
			DBUtil.update(dbPool, SQL_ADD_GROUP_MEMBER, DBUtil.THROW_HANDLER, 
					geId, ge.getMasterMdn(), MemberStatus.MASTER.name());
	
			
			id = geId;
		}
		catch (Exception e) {
			logger.error("Failed to create group event.", e);
		}
		return id;
	}

	/**
	 * 
	 * @param groupEventId
	 * @param mdn
	 * @param status
	 */
	public void updateMemberStatus(int groupEventId, String mdn, MemberStatus status) {
		try {
			
			DBUtil.update(dbPool, SQL_UPDATE_MEMBER_STATUS, DBUtil.THROW_HANDLER, 
					status.name(), groupEventId, mdn);
			
			if (status == MemberStatus.ACCEPTED) {
				// schedule a play
				schedulePlay(mdn, groupEventId);
			}
		}
		catch (Exception e) {
			logger.error("Failed to update member status");
		}
	}
	
	/**
	 * 
	 * @param groupEventId
	 * @param mdn
	 * @param lastChannelId
	 */
	public void updateMemberLastChannelId(int groupEventId, String mdn, String lastChannelId) {
		try {
			DBUtil.update(dbPool, SQL_UPDATE_MEMBER_LAST_CHANNEL_ID, DBUtil.THROW_HANDLER,
					lastChannelId, groupEventId, mdn);
			
			logger.debug("updated last channel id to " + lastChannelId);
		}
		catch (Exception e) {
			logger.error("Failed to update last channel id");
		}
	}

	
	/**
	 * 
	 * @param channelId
	 * @return
	 */
	public Channel getChannel(String channelId) {
		Channel channel = null;
		try {
			channel = DBUtil.query(dbPool, SQL_GET_CHANNEL, 
					new DBUtil.BeanHandlerEx<Channel>(Channel.class), DBUtil.THROW_HANDLER, channelId);
			
			if (channel == null) {
				logger.debug("Unknown channel created: " + channelId);
				channel = new Channel();
				channel.setName("Unknown");
				channel.setId(channelId);
			}
		}
		catch (Exception e) {
			logger.error("Failed to get channel by channel id: " + channelId);
		}
		
		return channel;
	}
	
	/**
	 * 
	 * @param mdn
	 * @return
	 */
	public User getUser(String mdn) {
		User user = null;
		try {
			user = DBUtil.query(dbPool, SQL_GET_USER, 
					new DBUtil.BeanHandlerEx<User>(User.class), DBUtil.THROW_HANDLER, mdn);
			
			if (user == null) {
				logger.debug("User not found in address book, create an  unknown user: mdn=" + mdn);
				user = new User();
				user.setMdn(mdn);
				user.setName("Unnamed");
				
			}
		}
		catch (Exception e) {
			logger.error("Failed to get user by mdn = " + mdn, e);
		}
		
		return user;
	}
	
	

	
	/**
	 * schedule tuning a show
	 * @param mdn
	 * 
	 */
	public void schedulePlay(final String mdn, int groupEventId) {
		final GroupEvent ge = loadGroupEventFromDb(groupEventId);
		if (ge != null) {
			String key = ge.getId() + mdn;
			if (!scheduledEvent.contains(key)) {
			
				Date now = new Date();
				long delay = ge.getShowTime().getTime() - now.getTime();
				
				if (delay < 0) {
					// tune right away
					if (!TEST) {
						ComcastAPIHandler.tuneChannel(mdn, ge.getChannelId());
					}
				}
				else {
					//schedule it
					scheduler.schedule(new Runnable() {
						
						@Override
						public void run() {
							if (!TEST) {
								ComcastAPIHandler.tuneChannel(mdn, ge.getChannelId());
							}
							
						}
					}, delay, TimeUnit.MILLISECONDS);
				}				
				scheduledEvent.add(key);
			}
		}
		
	}
		
}
