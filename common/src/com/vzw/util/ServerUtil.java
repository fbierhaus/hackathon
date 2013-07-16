/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vzw.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

/**
 *
 * @author hud
 * 
 * Facilitates server identification and operations
 */
public class ServerUtil {
	
	private static final Logger			logger = Logger.getLogger(ServerUtil.class);
	
	/**
	 * Check whether the passed host is on this machine
	 * @param host
	 * @return 
	 */
	public static boolean isThisServer(String host) {
		boolean ret = false;
		try {
			if (host == null) {
				LogUtil.error(logger, "The input host is null");
			}
			else {
				InetAddress ia = InetAddress.getByName(host);
				if (ia == null) {
					LogUtil.error(logger, "The found InetAddress is null for {0}", host);
				}
				else {
					NetworkInterface ni = NetworkInterface.getByInetAddress(ia);
					if (ni == null) {
						LogUtil.error(logger, "there is no network interface with the specified IP address for {0}", host);
					}
					
					ret = true;
				}
			}
		}
		catch (UnknownHostException uhe) {
			LogUtil.info(logger, "No InetAddress found for host {0}", host);
		}
		catch (SocketException se) {
			LogUtil.error(logger, "Socket error occurred while getting network interface for host {0}", host);
		}
		
		return ret;
	}
}
