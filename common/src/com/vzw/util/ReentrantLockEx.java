/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author hud
 * 
 * For debugging purpose
 */
public class ReentrantLockEx extends ReentrantLock {
	public String getOwnerName() {
		Thread t = getOwner();
		if (t == null) {
			return "none";
		}
		else {
			return t.getName();
		}
	}

	@Override
	public void lock() {
		LogUtil.tlog("REENTRANTLOCKEX::TO_LOCK {0}", this);
		super.lock();
	}

	@Override
	public void unlock() {
		super.unlock();
		LogUtil.tlog("REENTRANTLOCKEX::UNLOCKED {0}", this);
	}
	
	
}
