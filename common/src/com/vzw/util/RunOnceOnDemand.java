/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

/**
 *
 * @author hud
 */
public class RunOnceOnDemand<T extends Throwable> {

	final private Object			syncMonitor = new Object();
	private boolean					done = false;
	private Task<T>					task = null;
	
	public static interface Task<T extends Throwable> {
		public void run() throws T;
	}

	public RunOnceOnDemand(Task<T> task) {
		this.task = task;
	}
	
	public void run() throws T  {
		boolean _done = done;
		
		if (!_done) {
			synchronized(syncMonitor) {
				_done = done;
				
				if (!_done) {
					task.run();
				}
				
				done = _done = true;
			}
		}
	}
}
