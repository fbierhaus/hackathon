/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Mainly for shorter executions
 * @author hud
 */
public class AsyncExecutor {
	
	private static ExecutorService			executorService = null;
	
	
	static {
		executorService = Executors.newCachedThreadPool();
		
		// need to add to shutdown hook
		Runtime r = Runtime.getRuntime();
		r.addShutdownHook(new Thread(new Runnable() {
			public void run() {
				if (executorService != null) {
					executorService.shutdownNow();
				}
			}
		}));
	}
	
	public static void execute(Runnable task) {
		executorService.submit(task);
	}
}
