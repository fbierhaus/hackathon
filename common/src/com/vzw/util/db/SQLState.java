/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util.db;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author hud
 * 
 * Work with DBDriver
 */
public enum SQLState {
	// ORACLE, JAVADB
	UNIQUE_INDEX_VIOLATION("23000", "23505")
	
	
	;
	
	private String[] codes = null;
	
	private SQLState(String...codes) {
		this.codes = codes;
	}
	
	public String getCode(DBDriver driver) {
		return codes[driver.ordinal()];
	}
	
	public boolean isCode(String code) {
		boolean ret = false;
		for (String c : codes) {
			if (StringUtils.equals(c, code)) {
				ret = true;
				break;
			}
		}
		
		return ret;
	}
	
	public boolean isCode(String code, DBDriver driver) {
		return StringUtils.equals(code, this.codes[driver.ordinal()]);
	}
	
}
