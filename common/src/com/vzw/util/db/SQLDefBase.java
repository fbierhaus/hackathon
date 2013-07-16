/**
 * 
 */
package com.vzw.util.db;

/**
 * @author hud
 *
 */
public class SQLDefBase {
	
	public static class IntegerWrapper {
		private int value = -1;

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
		
	}
	
	public static class LongWrapper {
		private long value = -1;

		public long getValue() {
			return value;
		}

		public void setValue(long value) {
			this.value = value;
		}
		
	}
	
	
	public static class FloatWrapper {
		private float value = 0;

		public float getValue() {
			return value;
		}

		public void setValue(float value) {
			this.value = value;
		}
		
	}
	
	
	public static class DoubleWrapper {
		private double value = 0;

		public double getValue() {
			return value;
		}

		public void setValue(double value) {
			this.value = value;
		}
		
		
	}
	
	

	/**
	 * 
	 */
	public SQLDefBase() {
		// TODO Auto-generated constructor stub
	}
	
	
	//get object name
	// For class  AA$BB$CC$DD 
	// iLevel == 1 => DD
	// iLevel == 2 => CC
	// iLevel == 3 => BB
	// never get AA
	private static String findObjName(Class cls, int iLevel)
	{
		String fullName = cls.getName();
		
		// Split seems not working
		//String[] names = fullName.split("\\\\$");
		
		String nameB = null;
		int i, posA, posB;
		
		posB = fullName.length();
		for (i = 0; i < iLevel; ++ i) {
			posA = fullName.lastIndexOf('$', posB - 1);
			if (posA == -1) {
				return null;	// not possible
			}
			nameB = fullName.substring(posA + 1, posB);
			posB = posA;
		}
		
		
		return nameB;
	}
	
	///////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////
	
	//////////////////////////////////////////////////////////////////////////
	//																		//
	//$$$$$$$$$$$$$$$$$$$$$$$ utility class $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$//
	//																		//
	//////////////////////////////////////////////////////////////////////////
	static private class OBJROOT 
	{
		private String 				name = null;
		private int 				size = 0;		// size of the type
		private int					indexCounter = 1;
		
		
		
		// indexing
		protected boolean resetIndex()
		{
			indexCounter = 1;
			return true;
		}
		protected int incIndex()
		{
			return indexCounter ++;
		}
		
		
		void init()
		{
			name = findObjName(getClass(), 1);
			size = incIndex();
		}
		
		String getName() 
		{
			return name;
		}
		int getSize() 
		{
			return size;
		}
	}
	
	static abstract public class STMTROOT extends OBJROOT
	{
		// statement SQL
		private String sql = null;
		public String getSql()		{
			return sql;
		}
		
		public String getSql(Object...params) {
			return String.format(sql, params);
		}
		
		protected STMTROOT(String sqlBase) {
			super.init();
			sql = sqlBase;
		}
		
		static protected String P(String paramName)		{
			return ':' + paramName;
		}
		
		
	}
	
}
