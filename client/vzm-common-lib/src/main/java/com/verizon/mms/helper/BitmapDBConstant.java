package com.verizon.mms.helper;

public interface BitmapDBConstant {
	// table names in the database
	static final String TABLE_BITMAP = "bitmap_tbl";

	// Table and Schemas	
	static final String CREATE_EVENT = "CREATE TABLE " + TABLE_BITMAP + " ("
	    + "url VACHAR PRIMARY KEY, "
	    + "content_type VACHAR, "
	    + "response_code INT, "
	    + "error VACHAR, "
	    + "bitmap BLOB, "
	    + "bitmap_size INT,"
	    + "last_update BIGINT, "
	    + "last_read BIGINT "
		+ ")";

	// ============================== Insert =================================	

	static final String INSERT_BITMAP = "INSERT INTO " + TABLE_BITMAP
			+ "(url, content_type, response_code, bitmap, bitmap_size, error, last_update, last_read) "
			+ "values (?, ?, ?, ?, ?, ?, ?, ?)";

}
