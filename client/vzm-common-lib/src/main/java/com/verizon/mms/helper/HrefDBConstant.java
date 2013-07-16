package com.verizon.mms.helper;

public interface HrefDBConstant {
	// table names in the database
	static final String TABLE_DETAILS = "details_tbl";

	// Table and Schemas	
	static final String CREATE_DETAILS = "CREATE TABLE " + TABLE_DETAILS + " ("
//		+ "_id INTEGER, "
	    + "url VACHAR PRIMARY KEY, "
	    + "content_type VACHAR, "
	    + "title VACHAR, "
	    + "description VACHAR, "
//	    + "og_url VACHAR, "
	    + "image_url VACHAR, "
	    + "response_code INT, "
	    + "error VACHAR, "
	    + "last_update BIGINT, "
	    + "last_read BIGINT, "
	    + "image_cache VARCHAR "	// not used
		+ ")";

	// ============================== Insert =================================	

	static final String INSERT_DETAILS = "insert into " + TABLE_DETAILS
			+ "(url, content_type, title, description, image_url, response_code, error, last_update, last_read, image_cache) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	// ============================== Update ===============================

//	static final String UPDATE_DETAILS = "update " + TABLE_DETAILS
//			+ "(url, content_type, title, description, image_url, response_code, error, last_update, last_read) "
//			+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
//
//	static final String UPDATE_DETAILS_LAST_READ = "update " + TABLE_DETAILS
//			+ "(url, last_read) "
//			+ "values (?, ?)";
}
