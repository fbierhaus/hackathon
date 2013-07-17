/**
 * 
 */
package com.vzw.pdi.hackathon.server;

import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.log4j.Logger;

import com.vzw.util.db.DBManager;
import com.vzw.util.db.DBPool;
import com.vzw.util.db.DBUtil;

/**
 * @author fred
 *
 */
public class FlingManager {
	
	private static final Logger	logger = Logger.getLogger(FlingManager.class);
	private  DBPool dbPool = null;
		
	private static final String SQL_GET_NEXT_FLING_ID = "VALUES (NEXT VALUE FOR FLING_PK_SEQ)";
	
	private static final String SQL_CREATE_FLING = "INSERT INTO FLING (FLING_ID, CONTENT_TYPE, FILE_PATH) VALUES (?, ?, ?)";
	
	private static FlingManager instance = null;
	
	static {
		instance = new FlingManager();
	}
	
	public static FlingManager getInstance() {
		return instance;
	}
	
	private FlingManager() {
		dbPool = DBManager.getDBPool();
	}
	
	public void save(int flingId, String contentType, String path){
		
		try {
			
			// insert into db
			DBUtil.update(dbPool, SQL_CREATE_FLING, DBUtil.THROW_HANDLER, flingId, contentType, path);
			
		} catch (Exception e) {
			logger.error("Failed to create Fling", e);
		}
		
	}
	
	public int getId() throws Exception{
		return DBUtil.query(
				dbPool, 
				SQL_GET_NEXT_FLING_ID,
				new ScalarHandler<Integer>(),
				DBUtil.THROW_HANDLER
				);
	}
}
