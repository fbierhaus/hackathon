/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import com.vzw.util.config.AbstractProperties;
import java.util.concurrent.*;
import org.apache.log4j.Logger;

/**
 *
 * @author hud
 * 
 * Wrappers usage of ExecuteServices
 */
public class ThreadPoolUtil {
	private static final Logger			logger = Logger.getLogger(ThreadPoolUtil.class);
	
	/**
	 * Wrapper to the thread pool for easy shutdown and process
	 */
	public static class Executor {
		private ThreadPoolExecutor			executor = null;
		private Future						future = null;
		
		public Executor(ThreadPoolExecutor e) {
			this.executor = e;
		}

		public ThreadPoolExecutor getExecutor() {
			return executor;
		}

		public void setExecutor(ThreadPoolExecutor executor) {
			this.executor = executor;
		}

		public Future getFuture() {
			return future;
		}

		public void setFuture(Future future) {
			this.future = future;
		}
		
		
		public void shutdown(long timeoutSec) {
			if (executor != null) {
				executor.shutdownNow();
				try {
					executor.awaitTermination(timeoutSec, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					LogUtil.info(logger, e, "Shutting down the executor was interrupted");
				}
				
				executor = null;
				future = null;
			}
		}
		
		
	}
	
	public static class Param {
		private static String makeKey(String scope, String name) {
			return scope + "." + name;
		}
		
		
		public static enum Name {
			CORE_POOL_SIZE("core_pool_size")
		,	MAX_POOL_SIZE("max_pool_size")
		,	KEEP_ALIVE_TIME_SECONDS("keep_alive_time_seconds")
		
			// if not supplied, 0 is default
			// 0:	will use SynchronousQueue 
			// > 0:	will use ArrayBlockingQueue (bounded)
			// < 0: will use LinkedBlockingQueue (unbounded)
		,	BLOCKING_QUEUE_SIZE("blocking_queue_size")
			;
		
			private String key = null;

			private Name(String key) {
				this.key = key;
			}

			public String key() {
				return key;
			}
		}
		
		
		private int		corePoolSize = -1;
		private int		maxPoolSize = -1;
		private int		keepAliveTimeSeconds = -1;
		private int		blockingQueueSize = 0;
		
		
		public Param() {
		}
		
		public void setParams(AbstractProperties props, String scope, boolean scheduled) throws Exception {
			if ((corePoolSize = props.getInt(makeKey(scope, Name.CORE_POOL_SIZE.key()), -1)) <= 0) {
				LogUtil.errorAndThrow(logger, new SimpleException("Invalid {0}: {1}", Name.CORE_POOL_SIZE.key(), corePoolSize));
			}
			
			if (!scheduled) {
				if ((maxPoolSize = props.getInt(makeKey(scope, Name.MAX_POOL_SIZE.key()), -1)) <= 0) {
					LogUtil.errorAndThrow(logger, new SimpleException("Invalid {0}: {1}", Name.MAX_POOL_SIZE.key(), maxPoolSize));
				}
				if ((keepAliveTimeSeconds = props.getInt(makeKey(scope, Name.KEEP_ALIVE_TIME_SECONDS.key()), -1)) < 0) {
					LogUtil.errorAndThrow(logger, new SimpleException("Invalid {0}: {1}", Name.KEEP_ALIVE_TIME_SECONDS.key(), keepAliveTimeSeconds));
				}
				
				// get the blocking queue size
				blockingQueueSize = props.getInt(makeKey(scope, Name.BLOCKING_QUEUE_SIZE.key()), 0);
			}
			
		}

		public int getCorePoolSize() {
			return corePoolSize;
		}

		public void setCorePoolSize(int corePoolSize) {
			this.corePoolSize = corePoolSize;
		}

		public int getKeepAliveTimeSeconds() {
			return keepAliveTimeSeconds;
		}

		public void setKeepAliveTimeSeconds(int keepAliveTimeSeconds) {
			this.keepAliveTimeSeconds = keepAliveTimeSeconds;
		}

		public int getMaxPoolSize() {
			return maxPoolSize;
		}

		public void setMaxPoolSize(int maxPoolSize) {
			this.maxPoolSize = maxPoolSize;
		}

		public int getBlockingQueueSize() {
			return blockingQueueSize;
		}

		public void setBlockingQueueSize(int blockingQueueSize) {
			this.blockingQueueSize = blockingQueueSize;
		}
		
		
	}
	

	/**
	 * 
	 * @param props
	 * @param scope
	 * @return
	 * @throws Exception 
	 */
	public static Param getParam(AbstractProperties props, String scope, boolean scheduled) throws Exception {
		Param param = new Param();
		
		param.setParams(props, scope, scheduled);
		
		return param;
	}	
	
	public static Executor initThreadPool2(AbstractProperties props, String scope) throws Exception {
		WrappedThreadPoolExecutor e = initThreadPool(props, scope);
		return new Executor(e);
	}
	
	public static WrappedThreadPoolExecutor initThreadPool(AbstractProperties props, String scope) throws Exception {
		return initThreadPool(getParam(props, scope, false));
	}
	
	public static WrappedThreadPoolExecutor initThreadPool(Param param) {
		//ThreadPoolExecutor exec = (ThreadPoolExecutor)Executors.newCachedThreadPool();
		// determine the blocking queue
		java.util.concurrent.BlockingQueue<Runnable> blockingQueue;
		if (param.getBlockingQueueSize() == 0) {
			blockingQueue = new SynchronousQueue<Runnable>();
		}
		else if (param.getBlockingQueueSize() < 0) {
			blockingQueue = new LinkedBlockingQueue<Runnable>();	// no capacity here
		}
		else {
			blockingQueue = new ArrayBlockingQueue<Runnable>(param.getBlockingQueueSize());
		}
		
		WrappedThreadPoolExecutor exec = new WrappedThreadPoolExecutor(
				param.getCorePoolSize(), 
				param.getMaxPoolSize(), 
				param.getKeepAliveTimeSeconds(), 
				TimeUnit.SECONDS, 
				blockingQueue);
		
		return exec;
	}
	
	public static Executor initScheduledThreadPool2(AbstractProperties props, String scope) throws Exception {
		ScheduledThreadPoolExecutor e = initScheduledThreadPool(props, scope);
		return new Executor(e);
	}
	
	public static ScheduledThreadPoolExecutor initScheduledThreadPool(AbstractProperties props, String scope) throws Exception {
		return initScheduledThreadPool(getParam(props, scope, true));
	}
	
	public static ScheduledThreadPoolExecutor initScheduledThreadPool(Param param) {
		return (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(param.getCorePoolSize());
	}
}
