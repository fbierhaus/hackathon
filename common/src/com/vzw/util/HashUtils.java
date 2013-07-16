/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vzw.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

/**
 *
 * @author hud
 */
public class HashUtils {

	private static final Logger			logger = Logger.getLogger(HashUtils.class);
	private static final Charset		CHARSET = Charset.forName("UTF-8");
	private static final short			MAX_OBJ_LEN = Short.MAX_VALUE;

	public static enum Method {
		method1 {
			@Override
			protected String doEncode(byte[] methodBytes, Object[] objs) {
				byte[] r = compact(objs);
				for (int i = 0; i < r.length; ++ i) {
					r[i] = (byte)~r[i];
				}

				return Base64.encodeBase64String(r);
			}

			@Override
			protected String[] doDecode(byte[] methodBytes, Object rObj) {
				byte[] r = Base64.decodeBase64(rObj.toString());

				for (int i = 0; i < r.length; ++ i) {
					r[i] = (byte)~r[i];
				}

				return extract(r);
			}
		}

,		method2 {
			@Override
			protected String doEncode(byte[] methodBytes, Object[] objs) {
				byte[] r = compact(objs);

				byte[] r1 = new byte[r.length];

				int i, j;
				for (i = j = 0; i < r.length; i += 2, ++ j) {
					r1[j] = (byte)~r[i];
				}

				for (i = 1; i < r.length; i += 2, ++ j) {
					r1[j] = (byte)~r[i];
				}

				return Base64.encodeBase64String(r1);
			}

			@Override
			protected String[] doDecode(byte[] methodBytes, Object rObj) {
				byte[] r = Base64.decodeBase64(rObj.toString());

				byte[] r1 = new byte[r.length];
				int n1 = (r.length - 1) / 2 + 1;

				int i;
				for (i = 0; i < n1; ++ i) {
					r1[i * 2] = (byte)~r[i];
				}

				for (i = n1; i < r.length; ++ i) {
					r1[(i - n1) * 2 + 1] = (byte)~r[i];
				}


				return extract(r1);
			}
		}

,		method3 {
			@Override
			protected String doEncode(byte[] methodBytes, Object[] objs) {
				byte[] r = compact(objs);

				byte[] r1 = new byte[r.length];
				for (int i = 0; i < r.length; ++ i) {
					r1[r.length - 1 - i] = (byte)~r[i];
				}

				return Base64.encodeBase64String(r1);
			}

			@Override
			protected String[] doDecode(byte[] methodBytes, Object rObj) {
				byte[] r = Base64.decodeBase64(rObj.toString());

				byte[] r1 = new byte[r.length];
				for (int i = 0; i < r.length; ++ i) {
					r1[r.length - 1 - i] = (byte)~r[i];
				}

				return extract(r1);
			}
		}



		;

		private byte[]	byteRep = null;

		private Method() {
			byteRep = name().getBytes(CHARSET);
		}

		abstract protected String doEncode(byte[] methodBytes, Object...objs);
		abstract protected String[] doDecode(byte[] methodBytes, Object r);
		
		
		String encode(Object...objs) {
			return doEncode(byteRep, objs);
		}

		String[] decode(Object r) {
			return doDecode(byteRep, r);
		}


	}
	
	private static class _Worker {
		private MessageDigest			messageDigest = null;
		
		private _Worker() throws Exception {
			// By default we use SHA-256
			messageDigest = MessageDigest.getInstance("SHA-256");
		}
		
		private byte[] digest(String text) {
			if (text == null) {
				return null;
			}
			
			messageDigest.update(text.getBytes(CHARSET));
			return messageDigest.digest();
		}
		
		
		private String digestAsString(String text) {
			byte[] bytes = digest(text);
			
			if (bytes == null) {
				return null;
			}
			
			return Base64.encodeBase64String(bytes);
		}
	}
	
	private static ThreadLocal<_Worker>		tlWorker;
	
	static {
		tlWorker = new ThreadLocal<_Worker>() {
			@Override
			protected _Worker initialValue() {
				_Worker ret = null;
				try {
					ret = new _Worker();
				}
				catch (Exception e) {
					LogUtil.error(logger, e, "Unable to initialize digest worker");
				}

				return ret;
			}
		};
		
	}
	
	
	public static byte[] digest(Object value) {
		if (value == null) {
			return null;
		}
		else {
			return tlWorker.get().digest(value.toString());
		}
	}
	
	
	public static String digestAsString(Object value) {
		if (value == null) {
			return null;
		}
		else {
			return tlWorker.get().digestAsString(value.toString());
		}
	}

	/**
	 *
	 * @param objs
	 * @return
	 */
	private static byte[] compact(Object...objs) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		
		for (Object obj : objs) {
			byte[] a;
		
			if (obj instanceof byte[]) {
				a = (byte[])obj;
			}
			else {
				a = obj.toString().getBytes(CHARSET);
			}
			
			
			byte[] nl = shortToByteArray((short)a.length);

			try {
				baos.write(nl);
				baos.write(a);
			}
			catch (IOException ioe) {
				LogUtil.error(logger, ioe, "Unable to encode");
			}
		}

		return baos.toByteArray();
	}


	/**
	 *
	 * @param a
	 * @return
	 */
	private static String[] extract(byte[] a) {

		String[] ret = null;
		try {

			if (a.length <= 2) {
				throw new SimpleException("Too short value: {0}", a.length);
			}

			int idx = 0;
			short len;

			List<String> retList = new ArrayList<String>();

			do {
				len = byteArrayToShort(a, idx);

				if (len > MAX_OBJ_LEN) {
					throw new SimpleException("Too long object: {0}", len);
				}

				idx += 2;
				if (idx + len > a.length) {
					throw new SimpleException("Unable to decode.");
				}


				retList.add(new String(a, idx, len, CHARSET));

				idx += len;

			} while (idx < a.length);

			ret = retList.toArray(new String[0]);
		}
		catch (Exception e) {
			LogUtil.error(logger, e, "Decoding failed");
		}

		return ret;

	}


	/**
	 *
	 * @param v
	 * @return
	 */
	private static byte[] shortToByteArray(short v) {
		return new byte[]{
			(byte)((v >>> 8) ^ 0x80),
			(byte)((v & 0xff) ^ 0x80)
		};
	}


	/**
	 *
	 * @param ba
	 * @return
	 */
	private static short byteArrayToShort(byte[] ba, int offset) {
		return (short)(((ba[offset] ^ 0x80) << 8) +
				((ba[offset + 1] ^ 0x80) & 0xff));
	}

	/**
	 *
	 * @param o1
	 * @param o2
	 * @param method
	 * @return
	 */
	public static String encode(Method method, Object...objs) {

		return method.encode(objs);
	}

	/**
	 * 
	 * @param obj
	 * @return
	 */
	public static String[] decode(Method method, Object r) {
		return method.decode(r);
	}
}
