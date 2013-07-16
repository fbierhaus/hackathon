package com.vzw.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class TimeUtil {

	/**
	 * The following timezones are defined in SMSC NPANXX table. The index to the
	 * array is the time zone ID for the SMSC.
	 */
	private static String [] SMSC_TZ_ID_MAPPING = {"Greenwich",
													 "Pacific/Guam",
													 "US/Hawaii",
													 "US/Alaska",
													 "US/Pacific",
													 "US/Mountain",
													 "US/Central",
													 "US/Eastern",
													 "Canada/Atlantic",
													 "Canada/Newfoundland" };
	
	private static ConcurrentHashMap<String, TimeZone> tzMap = new ConcurrentHashMap<String, TimeZone>();
	
	static {
		for(int i=0; i<SMSC_TZ_ID_MAPPING.length; i++) {
			TimeZone tz = TimeZone.getTimeZone(SMSC_TZ_ID_MAPPING[i]);
			tzMap.putIfAbsent(String.valueOf(i), tz);
		}
	}

	public static Date GMT2EST(Date date) {
		return GMT2TimeZone(date, TimeZone.getTimeZone("America/New_York"));
	}
	
	public static Date GMT2TimeZone(Date date, TimeZone tz) {
		if (date==null) {
			return null;
		}
		long time = date.getTime();
		int offset = tz.getOffset(time);
		return new Date(time+offset);
	}
	
	public static Date TimeZone2GMT(Date date, TimeZone tz) {
		if (date==null) {
			return null;
		}
		long time = date.getTime();
		int offset = tz.getOffset(time);
		return new Date(time-offset);
	}
	
	public static Calendar getTimeInEST() {
		return Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
	}
	
	public static boolean isEvenDay() {
		return getTimeInEST().get(Calendar.DAY_OF_YEAR) % 2 == 0;
	}
	
	public static TimeZone getSubrSMSCTZ(String subrTZid) {
		return tzMap.get(subrTZid);
	}
	
	
}
