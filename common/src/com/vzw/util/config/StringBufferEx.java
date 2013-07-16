package com.vzw.util.config;

import java.lang.StringBuilder;

/**
 * <p>Title: StringBufferEx</p>
 *
 * <p>Description: </p>
 *
 * This is a extended version of StringBuilder Object
 * It also implements most interfaces that StringBuilder provides
 * This implementation gives high performance
 *
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class StringBufferEx
{
	private static int BUFFER_SIZE = 4096;

	/**
	 * Internal StringBuilder
	 */
	private StringBuilder m_sb;

	/**
	 * Default constructor
	 */
	public StringBufferEx() {
		m_sb = new StringBuilder();
    }

	/**
	 * Constructor
	 * @param length
	 */
	public StringBufferEx(int length) {
		m_sb = new StringBuilder(length);
	}

	/**
	 * Constructor
	 * @param str
	 */
	public StringBufferEx(String str) {
		m_sb = new StringBuilder(str);
	}

	/**
	 * Constructors
	 * @param sb
	 */
	public StringBufferEx(StringBuilder sb)  {
		m_sb = sb;
	}

	/**
	 *
	 * @return
	 */
	public int length() {
		return m_sb.length();
	}

	/**
	 *
	 */
	public void clear() {
		m_sb.delete(0, m_sb.length());
	}


	public int indexOf(String str) {
		return m_sb.indexOf(str);
	}
	public int indexOf(String str, int fromIndex) {
		return m_sb.indexOf(str, fromIndex);
	}


	/**
	 * Appends
	 * @params
	 * @return
	 */
	public StringBufferEx append(boolean b) {
		m_sb.append(b);
		return this;
	}
	public StringBufferEx append(char c) {
		m_sb.append(c);
		return this;
	}
	public StringBufferEx append(char[] str) {
		m_sb.append(str);
		return this;
	}
	public StringBufferEx append(char[] str, int offset, int len) {
		m_sb.append(str, offset, len);
		return this;
	}
	public StringBufferEx append(double d) {
		m_sb.append(d);
		return this;
	}
	public StringBufferEx append(float f) {
		m_sb.append(f);
		return this;
	}
	public StringBufferEx append(int i)  {
		m_sb.append(i);
		return this;
	}
	public StringBufferEx append(long l)  {
		m_sb.append(l);
		return this;
	}
	public StringBufferEx append(Object obj) {
		m_sb.append(obj);
		return this;
	}
	public StringBufferEx append(String str) {
		m_sb.append(str);
		return this;
	}
	public StringBufferEx append(StringBufferEx sb) {
		m_sb.append(sb);
		return this;
	}
	public StringBufferEx append(StringBufferEx sb, int start, int end) {
		return append(sb, start, end, new char[BUFFER_SIZE]);
	}


	/**
	 * Append a string using external buffer
	 * @param sb
	 * @param start
	 * @param end
	 * @param buf
	 * @param bufsize
	 * @return
	 */
	public StringBufferEx append(StringBufferEx sb, int start, int end, char[] buf) {

		int idxEnd = sb.length();
		int bufsize = buf.length;

		if (idxEnd > end)
			idxEnd = end;

		if (bufsize >= idxEnd - start)  {
			sb.getChars(start, idxEnd, buf, 0);
			return append(buf, 0, idxEnd - start);
		}

		// loop
		int nCopyStart = start;
		int nCopyEnd;
		while (nCopyStart < idxEnd)  {
			nCopyEnd = nCopyStart + bufsize;
			if (nCopyEnd > idxEnd)   nCopyEnd = idxEnd;
			sb.getChars(nCopyStart,  nCopyEnd, buf, 0);
			append(buf, 0, nCopyEnd - nCopyStart);
			nCopyStart += bufsize;
		}

		return this;
	}


	public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
		m_sb.getChars(srcBegin, srcEnd, dst, dstBegin);
	}

	public int capacity() {
		return m_sb.capacity();
	}

	public char charAt(int index) {
		return m_sb.charAt(index);
	}

	/**
	 * Gets internal StringBuilder Object
	 * @return
	 */
	public StringBuilder getStringBuffer() {
		return m_sb;
	}

	/**
	 * extracts substring
	 * @param start
	 * @return String
	 */
	public String substring(int start) {
		return m_sb.substring(start);
	}

	/**
	 * extracts substring
	 * @param start
	 * @param end
	 * @return
	 */
	public String substring(int start, int end)  {
		return m_sb.substring(start, end);
	}

	/**
	 * converts to String object
	 * @return
	 */
	public String toString() {
		return m_sb.toString();
	}

	/**
	 * Copies string
	 * @param sbex
	 * @param start
	 * @param end
	 * @return
	 */
	public StringBufferEx assign(
		   StringBufferEx sbex,
		   int start,
		   int end)
	{
		return assign(sbex.getStringBuffer(), start, end);
	}

	/**
	 * Assigns string
	 * @param sb
	 * @param start
	 * @param end
	 * @return
	 */
	public StringBufferEx assign(
		   StringBuilder sb,
		   int start,
		   int end)
	{
		return this;
	}
}
