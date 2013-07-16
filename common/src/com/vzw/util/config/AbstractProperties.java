/**
 * 
 */
package com.vzw.util.config;

import com.vzw.util.CleanupUtil;
import com.vzw.util.CollectionUtil;
import com.vzw.util.LogUtil;
import com.vzw.util.SimpleException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;



/**
 * @author hud
 *
 * This is a wrapper for configuration file with
 * java properties file format
 * 
 * IMPORTANT:
 * 	1. This root class does not force whether all the
 *  getters of the deriving class should be synchronized
 *  or not. It's developer's preference to determine
 *  whether some properties need to be updated and some
 *  other properties do not need to be hot-reloaded.
 *  
 *  On other words, in the "refresh" method, statements can
 *  be divided into two groups depending or "bFirstLoad". Usually
 *  refresh method MUST be synchronized. If some getters need to
 *  be hot-reloaded, add "synchronized" keyword to the getters method
 *  declaration and add them outside of "if (bFirstLoad)" block in
 *  "refresh()" method
 *  
 *  2. DO NOT initialize member variables during declaration. ALWAYS
 *  use "getter"s "defaultValue" parameter to initialize it. See
 *  AccessProperties for example.
 * 
 */
public abstract class AbstractProperties extends ResourceWatcher {
	
	private static final Logger			logger = Logger.getLogger(AbstractProperties.class);

	public static enum Type {
		STRING,
		STRING_ARRAY,
		STRING_LIST,
		DIR,
		DOUBLE,
		INT,
		LONG,
		BOOLEAN,
		BOOL3,
		SIZE,
		UNSET
	}
	
	
	public static enum SizeUnit {
		U(1L)
	,	K(1024L)
	,	M(K.getSize() * 1024L)
	,	G(M.getSize() * 1024L)
	
		;
	
		private long size = 1L;
		private static final Pattern	sizePattern = Pattern.compile("([0-9.]+)(k|K|m|M|g|G)?");
		
		private SizeUnit(long size) {
			this.size = size;
		}
		
		public long getSize() {
			return size;
		}
		
		public static SizeUnit fromString(String unitString) {
			SizeUnit sizeUnit;
			try {
				if (U.name().equals(unitString)) {
					sizeUnit = U;
				}
				else {
					sizeUnit = valueOf(unitString.toUpperCase());
				}
			}
			catch (Exception e) {
				sizeUnit = U;
			}
			
			return sizeUnit;
		}
		
		public static long parse(String value) throws Exception {
			Matcher m = sizePattern.matcher(value);
			SizeUnit sizeUnit;
			if (m.matches()) {
				int i = m.groupCount();
				String n = m.group(1);
				if (i == 1) {
					sizeUnit = U;
				}
				else {
					sizeUnit = fromString(m.group(2));
				}
				
				return (long)(sizeUnit.getSize() * Double.parseDouble(n));
			}
			else {
				throw new SimpleException("Invalid size format: {0}", value);
			}
		}
		
		
		
		
	}
	

	
	private static class FileInfo {
		private File			file = null;
		/**
		 * last modified
		 */
		private long			lastModified	= 0;
		
		public FileInfo(File file) {
			this.file = file;
		}
		
		public FileInfo(String filePath) {
			file = new File(filePath);
		}

		public File getFile() {
			return file;
		}

		public void setFile(File file) {
			this.file = file;
		}


		public long getLastModified() {
			return lastModified;
		}

		public void setLastModified(long lastModified) {
			this.lastModified = lastModified;
		}
		
		
	}
	
	/**
	 * property util, singleton
	 */
	private static PropUtil			pu = null;
	
	/**
	 * env file validator
	 */
	private static EnvFileValidator	validator = new DefaultEnvFileValidator();
	
	/**
	 * the internal properties object
	 */
	private Properties				prop = null;
	
	
	private boolean					searchRealPath = false;
	
	/**
	 * the file path
	 */
	//private String			fileName	= null;
	private List<FileInfo>			fileInfos = null;
	
	/**
	 * The fileNames passed to the constructor
	 */
	private List<String>			rootFileNames = null;
	
	
	public static final String	DEFAULT_LIST_SEP = "\\s*,\\s*";
	
	
	
	
	/**
	 * 
	 */
	public AbstractProperties(String...fileNames) throws Exception {
		// TODO Auto-generated constructor stub
		super();

		this.rootFileNames = Arrays.asList(fileNames);
		
		getPu();

		load(true, false, fileNames);
	}
	
	
	public AbstractProperties(boolean searchRealPath, String...fileNames) throws Exception {
		super();
		
		this.searchRealPath = searchRealPath;
		
		getPu();
		load(true, searchRealPath, fileNames);
	}
	

	synchronized private static PropUtil getPu() {
		if (pu == null) {
			pu = new PropUtil();
		}
		return pu;
	}
	

	/**
	 * Recursive method call
	 * @param cl
	 * @param searchRealPath
	 * @param fileNames
	 * @throws Exception 
	 */
	private Properties _load(boolean buildFileInfos, ClassLoader cl, boolean searchRealPath, String...fileNames) throws Exception {
		
		Properties propRet = new Properties();
		InputStream fis = null;
		
		for (String fn : fileNames) {
			URL url = cl.getResource(fn);
			
			File f = null;
			
			if (url != null) {
				f = new File(url.getFile());
			}
			else if (searchRealPath) {
				f = new File(fn);
				if (!(f.exists() && f.isFile())) {
					f = null;
				}
			}
			
			if (f != null) {
				// we found a file
				Properties propCur = new Properties();
				
				try {
					fis = new FileInputStream(f);
					propCur.load(fis);
				}
				finally {
					CleanupUtil.release(fis);
					fis = null;
				}
				
				if (buildFileInfos) {
					if (fileInfos == null) {
						fileInfos = new ArrayList<FileInfo>();
					}
					FileInfo fi = new FileInfo(f);
					fi.setLastModified(f.lastModified());
					fileInfos.add(fi);				
				}
				
				//
				// check imports
				//
				boolean srp;
				String srpStr = propCur.getProperty("import.search_real_path");
				srp = StringUtils.equalsIgnoreCase(srpStr, "true");
				
				String[] files = null;
				String flStr = propCur.getProperty("import.files");
				if (flStr != null) {
					files = flStr.split("\\s*,\\s*");
				}
				
				if (!CollectionUtil.isEmpty(files)) {
					Properties propImport = _load(buildFileInfos, cl, srp, files);
					// once done, 
					propRet.putAll(propImport);
				}
				
				propRet.putAll(propCur);
				
				//
				// now check sub dir
				//
				checkIncludeHome(propRet, f);
				// once done, the include_home_subdir will be removed
			}
		}
		
		return propRet;
	}
	
	private void load(boolean refresh, boolean searchRealPath, String...fileNames) throws Exception {
		
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Properties propLoad = _load(true, cl, searchRealPath, fileNames);
		
			// expand it
			prop = pu.expandProperties(propLoad, validator);

			// refresh if needed
			if (refresh) {
				refresh(true);
			}

			// set watcher
			initWatcher();
		}
		catch (Exception e) {
			LogUtil.errorAndThrow(logger, e);
			//throw e;
		}
		
		
	}
	
	/**
	 * 
	 * @param fileName
	 * @param refresh
	 * @throws Exception
	 * 
	 * @deprecated 
	 */
	private void load_old(boolean refresh, boolean searchRealPath, String...fileNames) throws Exception {
		
		InputStream fis = null;
		
		try {
		
			// get file input stream
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			
			Properties propOrig = new Properties();
			for (String fn : fileNames) {
				
				URL url = cl.getResource(fn);

				File f = null;
				if (url != null) {
					f = new File(url.getFile());
				}
				else if (searchRealPath) {
					f = new File(fn);
					if (!(f.exists() && f.isFile())) {
						f = null;
					}
				}
				
				if (f != null) {
				
					if (fileInfos == null) {
						fileInfos = new ArrayList<FileInfo>();
					}
					
					fileInfos.add(new FileInfo(f));

					try {
						//fis = url.openStream();
						fis = new FileInputStream(f);
						
						// load the property
						propOrig.load(fis);
						
						// check imports
						
						
						// check include_home
						checkIncludeHome(propOrig, f);
					}
					finally {
						// close the file input stream
						if (fis != null) {
							fis.close();
							fis = null;
						}
					}
				}
				else {
					// unable to find the file
					// simply create the properties 
					LogUtil.info(logger, "Unable to find file: {0}", fn);
					System.out.println(MessageFormat.format("Unable to find file {0}. Use empty properties.", fn));
					prop = new Properties();		// empty one

				}
				
				
				
			}
			
				
			// expand it
			prop = pu.expandProperties(propOrig, validator);

			// refresh if needed
			if (refresh) {
				refresh(true);
			}

			// set watcher
			initWatcher();
			
		}
		catch (Exception e) {
			LogUtil.errorAndThrow(logger, e);
			//throw e;
		}
	}
	
	/**
	 * 
	 * @param prop 
	 */
	private void checkIncludeHome(Properties prop, File origFile) {
		String homeSubDir = prop.getProperty("include_home_subdir");
		
		if (homeSubDir != null) {
			File homeSubDirFile = new File(System.getProperty("user.home"), homeSubDir);
			
			if (homeSubDirFile.exists() && homeSubDirFile.isDirectory()) {
			
				// find the same file in home
				File f = new File(homeSubDirFile, origFile.getName());
				if (f.exists() && f.isFile()) {
					FileInputStream fis = null;
					try {
						fis = new FileInputStream(f);
						prop.load(fis);
						prop.setProperty("include_home_subdir", null);
					}
					catch (Exception e) {
						// do nothing
						CleanupUtil.release(fis);
					}
				}
			}
		}
	}
	

	/**
	 * called by timer task
	 * already synchronized with time task
	 * 
	 * this method only gets called when requiredRefresh returns true, so 
	 * no need to check
	 */
	@Override
	final protected void reload() throws Exception {
		
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Properties propLoad = _load(false, cl, searchRealPath, rootFileNames.toArray(new String[0]));

		// expand it
		prop = pu.expandProperties(propLoad, validator);

		// refresh if needed
		refresh(false);
	}

	
	@Override
	final protected int getCheckInterval() {
		return getInt(KEY_HOTRELOAD_CHECKINTERVAL, -1);
	}
	
	@Override
	final protected int getForceInterval() {
		return getInt(KEY_HOTRELOAD_FORCEINTERVAL, -1);
	}
	
	@Override
	final protected int getCheckDelay() {
		return 0;	// use interval
	}
	
	@Override
	final protected int getForceDelay() {
		return 0;	// use interval
	}
	
	@Override
	final protected boolean requireRefresh() {
		boolean ret = false;
		
		if (fileInfos != null && fileInfos.size() > 0) {
			
			for (FileInfo fi : fileInfos) {
				long lModified = fi.getFile().lastModified();
				if (lModified != 0 && lModified != fi.getLastModified()) {
					ret = true;
					fi.setLastModified(lModified);
					
					// do not break, just update the modified time
					// for all the files
				}
			}
			
			
		}
		
		
		return ret;
	}
	
	/*
	final public long lastFileModified() {
		File file = new File(fileName);
		return file.lastModified();
		
	}
	
	 * 
	 */
	
	
	abstract protected void refresh(boolean bFirstLoad) throws Exception;
	
	
	/**
	 * get keyset
	 * @return
	 */
	final public Enumeration getPropertyNames() {
		return prop.propertyNames();
	}
	
	/**
	 * get internal properties
	 * @param key
	 * @return
	 */
	final public Properties getProperties() {
		return prop;
	}
	
	/////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////
	// utilities to get and set values
	final public <T> T getProperty(String key, Type type, T defaultValue, Object...params) {
		switch (type) {
			case STRING:
				return (T)getString(key, (String)defaultValue);
			case STRING_ARRAY:
				return (T)getStringArray(key, (String)params[0]);
			case STRING_LIST:
				return (T)getStringList(key, (String)params[0]);
			case DIR:
				return (T)getDir(key, (String)defaultValue);
			case DOUBLE:
				return (T)(Object)getDouble(key, (Double)defaultValue);
			case INT:
				return (T)(Object)getInt(key, (Integer)defaultValue);
			case LONG:
				return (T)(Object)getLong(key, (Long)defaultValue);
			case BOOLEAN:
				return (T)(Object)getBoolean(key, (Boolean)defaultValue);
			case BOOL3:
				return (T)(Object)getBool3(key);
			case SIZE:
				return (T)(Object)getSize(key, (Long)defaultValue);
			default:
				return null;
		}
	}

	final public <T> T getProperty(String key, Type type, Object...params) throws Exception {
		switch (type) {
			case STRING:
				return (T)getString(key);
			case STRING_ARRAY:
				return (T)getStringArray(key, (String)params[0]);
			case STRING_LIST:
				return (T)getStringList(key, (String)params[0]);
			case DIR:
				return (T)getDir(key);
			case DOUBLE:
				return (T)(Object)getDouble(key);
			case INT:
				return (T)(Object)getInt(key);
			case LONG:
				return (T)(Object)getLong(key);
			case BOOLEAN:
				return (T)(Object)getBoolean(key);
			case BOOL3:
				return (T)(Object)getBool3(key);
			case SIZE:
				return (T)(Object)getSize(key);
			default:
				return null;
		}

	}


	final public String getString(String key) {
		String sRet = prop.getProperty(key);
		return sRet != null ? sRet.trim() : sRet;
	}
	
	final public String getString(String key, String defaultValue)	{
		String sRet = prop.getProperty(key, defaultValue);
		return sRet != null ? sRet.trim() : sRet;
	}
	
	/**
	 * 
	 * @param key
	 * @return 
	 */
	final public String[] getStringArray(String key) {
		return getStringArray(key, DEFAULT_LIST_SEP);
	}
	
	final public String[] getStringArray(String key, String sepRegex) {
		String[] ret = null;
		
		if (sepRegex != null && key != null) {
		
			String val = getString(key);
			if (val != null) {
				ret = val.split(sepRegex);
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param key
	 * @return 
	 */
	final public List<String> getStringList(String key) {
		return getStringList(key, DEFAULT_LIST_SEP);
	}
	
	/**
	 * helper to get string array in another form
	 * @param key
	 * @param sepRegex
	 * @return
	 */
	final public List<String> getStringList(String key, String sepRegex) {
		
		List<String> ret = null;
		String[] arr = getStringArray(key, sepRegex);
		if (arr != null) {
			ret = Arrays.asList(arr);
		}
		
		return ret;
	}
	
	final public boolean getBoolean(String key) throws Exception	{
		String sTmp = prop.getProperty(key);
		if (sTmp == null) {
			throw new Exception("Property [" + key + "] was not found.");
		}
		else {
			return "true".equalsIgnoreCase(sTmp);
		}
	}
	
	final public boolean getBoolean(String key, boolean defaultValue)	{
		String sTmp = prop.getProperty(key);
		return sTmp == null ? defaultValue : "true".equalsIgnoreCase(sTmp);
	}
	
	
	final public Bool3 getBool3(String key) {
		Bool3 ret = Bool3.UNKNOWN;
		
		boolean bret;
		try {
			bret = getBoolean(key);
			
			if (bret) {
				ret = Bool3.TRUE;
			}
			else {
				ret = Bool3.FALSE;
			}
		}
		catch (Exception e) {
			// do nothing
		}
		
		
		return ret;
	}
	
	
	final public int getInt(String key, int defaultValue) {
		String sTmp = getString(key);
		if (sTmp == null) {
			return defaultValue;
		}
		else {
			return Integer.parseInt(sTmp);
		}
	}
	
	final public long getLong(String key, long defaultValue) {
		String sTmp = getString(key);
		if (sTmp == null) {
			return defaultValue;
		}
		else {
			return Long.parseLong(sTmp);
		}
	}	

	final public int getInt(String key) throws Exception {
		String sTmp = getString(key);
		if (sTmp == null) {
			throw new Exception("Property [" + key + "] was not found.");
		}
		else {
			return Integer.parseInt(sTmp);
		}
	}
	
	final public long getLong(String key) throws Exception {
		String sTmp = getString(key);
		if (sTmp == null) {
			throw new Exception("Property [" + key + "] was not found.");
		}
		else {
			return Long.parseLong(sTmp);
		}
	}
	
	final public long getSize(String key) throws Exception {
		String sTmp = getString(key);
		
		if (sTmp == null) {
			throw new Exception("Property [" + key + "] was not found.");
		}
		else {
			return SizeUnit.parse(sTmp);
		}
	}
	
	final public long getSize(String key, long defVal) {
		String sTmp = getString(key);
		
		if (sTmp == null) {
			return defVal;
		}
		else {
			try {
				return SizeUnit.parse(sTmp);
			}
			catch (Exception e)  {
				return defVal;
			}
			
		}
		
	}
	
	
	final public String getDir(String key, String defaultDir) {
		String dir = getString(key);
		if (dir == null) {
			dir = defaultDir;
		}
		else if (dir.charAt(dir.length() - 1) != File.separatorChar) {
			dir += File.separator;
		}
		
		return dir;
	}
	
	
	final public String getDir(String key) throws Exception {
		String dir = getDir(key, null);
		if (dir == null) {
			throw new Exception("Property [" + key + "] was not found.");
		}
		
		return dir;
	}
	
	
	final public double getDouble(String key, double defaultValue) {
		String sTmp = getString(key);
		if (sTmp == null) {
			return defaultValue;
		}
		else {
			return Double.parseDouble(sTmp);
		}
		
	}
	
	final public double getDouble(String key) throws Exception {
		String sTmp = getString(key);
		if (sTmp == null) {
			throw new Exception("Property [" + key + "] was not found.");
		}
		else {
			return Double.parseDouble(sTmp);
		}
	}

	
	final public void setProperty(Object key, Object value) throws Exception {
		prop.setProperty(key.toString(), value.toString());
	}
	
	
	/**
	 * testing
	 * @param args
	 */
	public static void main(String[] args) {
		class TestProperties extends AbstractProperties {
			
			private String val1;
			private String val2;
			
			public TestProperties(String fileName) throws Exception {
				super(fileName);
			}
			@Override
			synchronized public void refresh(boolean bFirstLoad) throws Exception {
				val1 = getString("key1");
				val2 = getString("key2");
			}
			
			
			public void print() {
				System.out.println("val1=" + val1);
				System.out.println("val2=" + val2);
				
			}
			
		}
		
		
		// main part
		try {
			TestProperties tp = new TestProperties("test.properties");
			tp.print();
		}
		catch (Exception e) {
			//done
		}
	}
}
