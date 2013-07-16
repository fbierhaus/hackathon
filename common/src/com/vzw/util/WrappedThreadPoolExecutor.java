/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author hud
 */
public class WrappedThreadPoolExecutor extends ThreadPoolExecutor {
	
	public static interface EventListener {
		public void beforeExecute(Thread t, Runnable r);
		public void afterExecute(Runnable r, Throwable t);
	}
	
	abstract public static class AbstractEventListener implements EventListener {

		@Override
		public void beforeExecute(Thread t, Runnable r) {
		}

		@Override
		public void afterExecute(Runnable r, Throwable t) {
		}
	}
	
	protected EventListener				eventListener = null;

	public WrappedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public WrappedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	}

	public WrappedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	public WrappedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		
		if (eventListener != null) {
			eventListener.beforeExecute(t, r);
		}
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		
		if (eventListener != null) {
			eventListener.afterExecute(r, t);
		}
	}
	
	
	/**
	 * 
	 * @param listener 
	 */
	public void addEventListener(EventListener listener) {
		this.eventListener = listener;
	}
	
}
