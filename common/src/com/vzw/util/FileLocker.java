/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 *
 * @author hud
 * 
 * Note: this class CAN ONLY BE USED BY A SINGLE THREAD
 * 
 * If multiple threads want to create lock, create different object then
 */
public class FileLocker {
	private static final Logger	logger = Logger.getLogger(FileLocker.class);
	
	private File				lockFile = null;
	private RandomAccessFile	lockRaf = null;
	private FileLock			locker = null;
	
	
	public FileLocker(String dir, String name, boolean lock) {
		this(new File(dir), name, lock);
	}
	
	/**
	 * Construct the file locker names
	 * Actual file names are:
	 * 
	 * locker: .~#<name>.lck
	 * 
	 * @param dir
	 * @param name
	 * @param lock 
	 */
	public FileLocker(File dir, String name, boolean lock) {
		lockFile = new File(dir, String.format(".~#%1$s.lck", name));
		
		try {
		
			lockRaf = new RandomAccessFile(lockFile, "rw");
			if (lock) {
				locker = lockRaf.getChannel().tryLock();
			}
		}
		catch (FileNotFoundException e) {
			LogUtil.error(logger, e, "Failed to create file locker for {0}/{1}", dir, name);
		}
		catch (Exception ioe) {
			LogUtil.error(logger, ioe, "Failed to lock the channel for {0}/{1}", dir, name);
		}
	}
	
	/**
	 * wait infinitively
	 * @return 
	 */
	public boolean acquireLock() {
		return acquireLock(-1, null);
	}
	
	synchronized public boolean acquireLock(long wait, TimeUnit unit) {
		
		boolean ret = false;
		
		// if already locked, return right away
		if (locker != null && locker.isValid()) {
			ret = true;
		}
		else {
			long start = System.currentTimeMillis();
			
			
			// do a wait (1 second interval)
			try {
				while (true) {
					if (locker == null) {
						locker = lockRaf.getChannel().tryLock();
					}
					
					if (locker != null && locker.isValid()) {
						ret = true;
						break;
					}
					else {
						if (wait > 0 && unit != null && 
								System.currentTimeMillis() - start >= unit.toMillis(wait)) {
							break;		// done here
						}
						else {
							// wait
							LogUtil.info(logger, "Waiting for file lock: {0}", lockFile.getAbsolutePath());
							TimeUnit.SECONDS.sleep(1);
						}
					}
					
				}
			}
			catch (Exception e) {
				// failed to lock
				LogUtil.error(logger, e, "Failed to lock the channel: {0}", lockFile.getAbsolutePath());
			}
		}
		
		return ret;
	}
	
	
	
	
	synchronized public void releaseLock() {
		if (locker != null && locker.isValid()) {
			try {
				locker.release();
			}
			catch (IOException e) {
				// failed to lock
				LogUtil.error(logger, e, "Failed to release the lock: {0}", lockFile.getAbsolutePath());
			}
		}
	}
	
}
