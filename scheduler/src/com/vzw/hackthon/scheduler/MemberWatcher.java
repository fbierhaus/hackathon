package com.vzw.hackthon.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.vzw.hackathon.GroupEvent;
import com.vzw.hackathon.GroupEventManager;
import com.vzw.hackathon.Member;
import com.vzw.hackathon.apihandler.ComcastAPIHandler;
import com.vzw.hackathon.apihandler.SendVMAMessage;
import com.vzw.hackathon.apihandler.VZWAPIHandler;
import com.vzw.util.db.DBManager;
import com.vzw.util.db.DBPool;
import com.vzw.util.db.DBUtil;

public class MemberWatcher implements Runnable {
	private static final Logger		logger = Logger.getLogger(EventReminder.class);

	private static final DBPool 	dbPool = DBManager.getDBPool();
	
	private static final int CHECK_INTERVAL_SECONDS		= 10;
	
	
	private static final String SQL_SEL_MEMBERS = 
			"select group_event_id, mdn, device_id, member_name"
			+ " from GROUP_MEMBER"
			+ " where member_status = 'MASTER' or member_status = 'ACCEPTED"
			+ " order by GROUP_EVENT_ID, mdn";
	
		
	private ScheduledExecutorService		executor = null;
	

	
	private MemberWatcher() {
		
		// check the database every 10 seconds
		executor = Executors.newScheduledThreadPool(10);
		executor.scheduleAtFixedRate(this, 10, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
		
	}

	/**
	 * Get group events (list of group events)
	 * @return
	 */
	public List<GroupEvent> getGroupEventsForReminder() {
		
		List<GroupEvent> geList = null;
		
		try {
			
			// go over the events the show time of which is not REMINDER_MINUTES more minutes than now.
			geList = DBUtil.query(dbPool, SQL_SEL_EVENTS_FOR_REMINDER, 
					new DBUtil.BeanListHandlerEx<GroupEvent>(GroupEvent.class), DBUtil.THROW_HANDLER, -REMINDER_MINUTES);
			
		}
		catch (Exception e) {
			logger.error("Failed to get group events reminder", e);
		}
		finally {
		}
		
		
		return geList;
		
	}
	
	/**
	 * 
	 * @param groupEventId
	 * @return
	 */
	public List<String> getMemberMdnForReminder(int groupEventId) {
		List<String> mdnList = null;
		try {
			
			// go over the events the show time of which is not REMINDER_MINUTES more minutes than now.
			List<Object[]> l1 = DBUtil.query(dbPool, SQL_SEL_MEMBER_FOR_REMINDER, 
					new ArrayListHandler(), DBUtil.THROW_HANDLER, groupEventId);
			
			if (! CollectionUtils.isEmpty(l1)) {
				mdnList = new ArrayList<String>();
				for (Object[] oa : l1) {
					mdnList.add((String)oa[0]);
				}
				
			}
			
		}
		catch (Exception e) {
			logger.error("Failed to get group events members", e);
		}
		finally {
		}
		
		
		return mdnList;	
	}
	
	/**
	 * 
	 */
	public void sendReminders() {
		
		List<GroupEvent> geList = getGroupEventsForReminder();
		
		if (!CollectionUtils.isEmpty(geList)) {
			for (GroupEvent ge : geList) {
				List<String> mdnList = getMemberMdnForReminder(ge.getId());
				if (!CollectionUtils.isEmpty(mdnList)) {
					
					String msg = buildReminderString(ge);
					
					// send to the client
					//MessagingAPIHandler.sendSMS(mdnList, msg);
					VZWAPIHandler.sendSMS(mdnList, msg);
					
					flagReminderSent(ge.getId());
				}
			}
		}
	}
	
	private void flagReminderSent(int geId) {
		try {
			DBUtil.update(dbPool, SQL_FLAG_REMINDER_SENT, DBUtil.THROW_HANDLER, geId);
		}
		catch (Exception e) {
			logger.error("Unable flag reminder sent for geId=" + geId, e);
		}
	}
	
	/**
	 * 
	 * @param ge
	 * @return
	 */
	public String buildReminderString(GroupEvent ge) {
		return MessageFormat.format("MNREMINDER##{0}##{1,time,yyyy-MM-dd HH:mm}##{2}", 
				ge.getChannelId(), ge.getShowTime(), ge.getShowName());
	}
	
	
	private void watchMembers() {
		// get all the events first
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			ps = conn.prepareStatement(SQL_SEL_MEMBERS);
			rs = ps.executeQuery();
			
			
			
			int lastGeId = -1;
			int curGeId = -1;
			GroupEvent ge = null;
			List<Member> memberList = null;
			while (rs.next()) {
				curGeId = rs.getInt("group_event_id");
				
				if (curGeId != lastGeId) {
					// new group event
					if (ge != null) {
						processWatch(ge);
					}
					
					ge = GroupEventManager.getInstance().loadGroupEventFromDb(curGeId);
					memberList = new ArrayList<Member>();
					ge.setMemberList(memberList);
					
					
				}
				
				Member member = new Member();
				member.setDeviceId(rs.getString("device_id"));
				member.setMdn(rs.getString("mdn"));
				member.setName(rs.getString("member_name"));
				memberList.add(member);
					
			}
		}
		catch (Exception e) {
			logger.error("Failed to watch members", e);
		}
		finally {
			DBManager.release(rs, ps, conn);
		}
	}
	
	private void processWatch(GroupEvent ge) {
		String[] tos = ge.getMemberMdns();
		String toStr = StringUtils.join(tos, ",");
		for (Member m : ge.getMemberList()) {
			String channelId = ComcastAPIHandler.getChannelId(m.getDeviceId());
			if (!StringUtils.equals(channelId, ge.getChannelId())) {
				// need to send the group message of this change
				SendVMAMessage.sendMMS(m.getMdn(), toStr, m.getName() + " changed channel");
			}
		}
	}

	@Override
	public void run() {
		watchMembers();
		
	}
}
