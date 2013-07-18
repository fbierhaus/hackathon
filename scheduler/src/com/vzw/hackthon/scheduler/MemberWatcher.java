package com.vzw.hackthon.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.vzw.hackathon.Channel;
import com.vzw.hackathon.GroupEventManager;
import com.vzw.hackathon.User;
import com.vzw.hackathon.apihandler.SendVMAMessage;
import com.vzw.util.db.DBManager;
import com.vzw.util.db.DBPool;
import com.vzw.util.db.DBUtil;

public class MemberWatcher implements Runnable {
	private static final Logger		logger = Logger.getLogger(EventReminder.class);

	private static final DBPool 	dbPool = DBManager.getDBPool();
	
	private static final int CHECK_INTERVAL_SECONDS		= 10;

	
	private static final String SQL_SEL_GROUP0 =
			"select ge0.group_event_id from group_event ge0 " +
			" where ge0.create_time = ( " +
			" select max(create_time) from  (select ge.create_time as create_time from group_event ge, group_member gm" +
			" where gm.mdn = ? and gm.group_event_id = ge.group_event_id) a) ";
	
	private static final String SQL_SEL_MEMBERS0 = 
			"select u.mdn, u.name from "
			+ " GROUP_MEMBER g left outer join users u on g.mdn = u.mdn where g.group_event_id = ?";
		
	private ScheduledExecutorService		executor = null;
	

	
	public MemberWatcher() {
	}
	
	public void start() {
		
		// check the database every 10 seconds
		executor = Executors.newScheduledThreadPool(10);
		executor.scheduleAtFixedRate(this, 1, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
		
		
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				if (executor != null) {
					executor.shutdown();
				}
			}
			
		});
		
	}

	public void await() {
		
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}	
	
	
	private void watchMembers() {
		GroupEventManager gem = GroupEventManager.getInstance();
		List<User> userList = gem.getUsers();
		
		for (User user : userList) {
			if (!StringUtils.equals(user.getLastChannelId(), user.getChannelId())) {
				//need to get the latest group
				
				int geId = getLatestGroup(user.getMdn());
				
				if (geId < 0) {
					continue;
				}
				
				
				List<User> ul = getMemberUsers(geId);
				
				if (ul.isEmpty()) {
					continue;
				}
				
				
				String[] tos = new String[ul.size()];
				for (int i = 0; i < tos.length; ++ i) { 
					tos[i] = ul.get(i).getMdn();
				}
				String toStr = StringUtils.join(tos, ",");
				
				Channel channel = gem.getChannel(user.getChannelId());
				String msg = user.getName() + " just changed channel to " + channel.getName();
				logger.info("channel changed, to=" + toStr + ", msg = " + msg);
				
				// need to send the group message of this change
				SendVMAMessage.sendMMS(user.getMdn(), toStr, msg);
				
				// update last channel id
				gem.updateMemberLastChannelId(user.getMdn(), user.getChannelId());
				logger.info("updated lastChannelId to " + user.getChannelId() + " for mdn=" + user.getMdn());				
			}
		}
	}
	
	
	private List<User> getMemberUsers(int geId) {
		List<User> ul = new ArrayList<User>();
		
		try {
			ul = DBUtil.query(dbPool, SQL_SEL_MEMBERS0, new DBUtil.BeanListHandlerEx<User>(User.class), DBUtil.THROW_HANDLER, geId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("failed to get member users: geId=" + geId);
		}
		
		return ul;
	}
	
	
	private int getLatestGroup(String mdn) {
		int geId = -1;
		
		try {
			Number geIdObj = (Number)DBUtil.query(dbPool, SQL_SEL_GROUP0, new ScalarHandler<Number>(), DBUtil.THROW_HANDLER, mdn);
			if (geIdObj != null) {
				geId = geIdObj.intValue();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("Failed to get lastest group id for mdn=" + mdn);
		}
		
		
		return geId;
	}
	


	@Override
	public void run() {
		watchMembers();
		
	}
}
