package com.vzw.pdi.hackathon.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.vzw.util.db.DBManager;

/**
 * Application Lifecycle Listener implementation class ServerContextListener
 *
 */
@WebListener
public class ServerContextListener implements ServletContextListener {

    /**
     * Default constructor. 
     */
    public ServerContextListener() {
        // TODO Auto-generated constructor stub
    }

	/**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent arg0) {
        // TODO Auto-generated method stub
    }

	/**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent arg0) {
        DBManager.destroyAll();
    }
	
}
