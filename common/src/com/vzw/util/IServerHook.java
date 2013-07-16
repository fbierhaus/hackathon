package com.vzw.util;

public interface IServerHook {

	/**
	 * Server shutdown hook that implements the cleanup gracefully.
	 */
	public void shutdown();
}
