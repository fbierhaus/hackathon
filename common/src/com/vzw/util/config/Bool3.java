/**
 * 
 */
package com.vzw.util.config;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hud
 * 
 * 3-state boolean 
 * the third state is undetermined
 *
 */
public enum Bool3 {
	/**
	 *
	 */
	UNKNOWN((byte)-1, "U", "u")
,
	/**
	 *
	 */
	TRUE((byte)0, "Y", "y", "T", "t")
,
	/**
	 *
	 */
	FALSE((byte)1, "N", "n", "F", "f")
	;
	

	final private static Map<String, Bool3>		valMap;
	
	private byte val;
	private String[] strVals = null;
	
	static {
		valMap = new HashMap<String, Bool3>();
		
		for (Bool3 v : values()) {
			for (String s : v.getStrVals()) {
				valMap.put(s, v);
			}
		}
	}
	
	
	private Bool3(byte val, String...strVals) {
		this.val = val;
		this.strVals = strVals;
	}
	
	/**
	 *
	 * @return
	 */
	public String[] getStrVals() {
		return strVals;
	}
	
	/**
	 *
	 * @return
	 */
	public boolean isTrue() {
		return val == 0;
	}
	
	/**
	 *
	 * @return
	 */
	public boolean isFalse() {
		return val == 1;
	}
	
	/**
	 *
	 * @return
	 */
	public boolean isUnknown() {
		return val == -1;
	}

	/**
	 *
	 * @param strVal
	 * @return
	 */
	public static Bool3 fromString(String strVal) {
		Bool3 b = valMap.get(strVal);
		if (b == null) {
			b = UNKNOWN;
		}
		
		return b;
	}
	
	/**
	 * 
	 * @param b
	 * @return 
	 */
	public static Bool3 fromBool(boolean b) {
		return b ? TRUE : FALSE;
	}
	
	/**
	 *
	 * @param defaultVal
	 * @return
	 */
	public boolean getValue(boolean defaultVal) {
		switch (val) {
		case -1:
			return defaultVal;
		case 0:
			return true;
		case 1:
			return false;
		}
		
		return defaultVal;
	}
	
	
	/**
	 * Only get the default string value (first one in array)
	 * @return 
	 */
	public String getStrVal() {
		return strVals[0];
	}

}
