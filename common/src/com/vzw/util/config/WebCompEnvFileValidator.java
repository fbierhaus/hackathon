/**
 * 
 */
package com.vzw.util.config;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;

/**
 * @author hudongl
 *
 * This class also creates deriveing properties with prefix "this.", which
 * will only be used by the application startup scripts and work only with the
 * corresponding ANT scripts
 */
public class WebCompEnvFileValidator extends DefaultEnvFileValidator 
{
	public static final String NAME = "webcomp";
	
	// different types of web components
	public static final int	   WC_WEBLOGIC								= 1;
	public static final int	   WC_JBOSS									= 2;
	
	
	public static final String ENVPROP_web_component__master			= ".web_component.master";
	public static final String ENVPROP_web_component__slave_list		= ".web_component.slave_list"; 
	public static final String ENVPROP_global__web_components			= "global.web_components";
	public static final String ENVPROP_weblogic							= ".weblogic";
	public static final String ENVPROP_jboss							= ".jboss";
	
	//public static final String ENVPOP_this_DOMAIN_HAME					= "this.DOMAIN_HAME";
	//public static final String ENVPOP_this_SERVER_HAME					= "this.SERVER_HAME";
	//public static final String ENVPOP_this_MASTER_COMPONENT_NAME		= "this.MASTER_COMPONENT_NAME";
	public static final String ENVPOP_this_APP_JAVA_HOME				= "this.APP_JAVA_HOME";
	public static final String ENVPOP_this_APP_JVM_OPTIONS				= "this.APP_JVM_OPTIONS";
	
	// for WebLogic
	public static final String ENVPOP_this_WL_HOME						= "this.WL_HOME";
	public static final String ENVPOP_this_WL_VERSION					= "this.WL_VERSION";
	public static final String ENVPOP_this_WL_HOST						= "this.WL_HOST";
	public static final String ENVPOP_this_WL_PORT						= "this.WL_PORT";
	
	// for JBoss
	public static final String ENVPOP_this_JBOSS_HOME						= "this.JBOSS_HOME";
	public static final String ENVPOP_this_JBOSS_VERSION					= "this.JBOSS_VERSION";
	public static final String ENVPOP_this_JBOSS_HOST						= "this.JBOSS_HOST";
	public static final String ENVPOP_this_JBOSS_PORT						= "this.JBOSS_PORT";
	
	
	public static final String ENV_COMPONENT_NAME						= "COMPONENT_NAME";
	
	// this is a hidden property
	// tells which master owns this slave component
	protected static final String ENVPROP_web_component__owner_master	= ".web_component.owner_master";
	
	class WebComponent
	{
		int					wcType = -1;
		
		String				webCompName = null;
		String				ownerMaster = null;
		
		boolean				isMaster = true;
		HashSet				hsSlave = null;
		
		// confirmed. 
		boolean				confirmed = false;
		
		public WebComponent(String _webCompName)
		{
			webCompName = _webCompName;
		}
		
		public boolean equals(Object _webComp) 
		{
			WebComponent wc = (WebComponent)_webComp;
			return wc != null && webCompName.equals(wc.webCompName);
		}
		
	}
	
	private Hashtable m_htWebComponent = null;
	private HashSet m_hsMasterWebComponent = null;

	/**
	 * 
	 */
	public WebCompEnvFileValidator() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param varPrefix
	 * @param varSuffix
	 */
	public WebCompEnvFileValidator(PropUtil.TokenWrapper varWrapper) {
		super(varWrapper);
		// TODO Auto-generated constructor stub
	}
	

	/**
	 * 
	 */
	public void validate(Properties prop)
	throws ConfigException
	{
		super.validate(prop);
		
		// now check web component's master/slave settings
		// and add normalized properties as necessary
		// Added properties are
		// global.webcomponents = <comma separated web component list>
		m_htWebComponent = new Hashtable();
		
		// collec the web components
		collectWebComponents(prop);
		
		// validate
		doValidate(prop);
		
		// add current properties if necessary
		createEnvProperties(prop);
		
	}
	
	private void createEnvProperties(Properties prop)
	{
		// check if COMPONENT_NAME is defined
		String sComponentName = prop.getProperty(ENV_COMPONENT_NAME);
		if (sComponentName != null) {
			String sVal;
			
			// set java_home
			sVal = prop.getProperty(sComponentName + ".java_home");
			if (sVal == null) {
				sVal = prop.getProperty("global.java_home");
				if (sVal == null) {
					sVal = "";
				}
			}
			prop.setProperty(ENVPOP_this_APP_JAVA_HOME, sVal);
			
			// jvm_options
			sVal = prop.getProperty(sComponentName + ".jvm_options");
			if (sVal == null) {
				sVal = "";
			}
			prop.setProperty(ENVPOP_this_APP_JVM_OPTIONS, sVal);
			
			
			// generate weblogic environments
			createWebLogicEnvProperties(prop, sComponentName);
			
			// generate JBOSS environments
			createJBossEnvProperties(prop, sComponentName);
			
		}
		                          
	}

	private void createWebLogicEnvProperties(Properties prop, String sComponentName) {
		
		String sVal;
		
		// wl_home
		sVal = prop.getProperty(sComponentName + ".weblogic.wl_home");
		if (sVal == null) {
			sVal = "";
		}
		prop.setProperty(ENVPOP_this_WL_HOME, sVal);

		// wl_version
		sVal = prop.getProperty(sComponentName + ".weblogic.version");
		if (sVal == null) {
			sVal = "";
		}
		prop.setProperty(ENVPOP_this_WL_VERSION, sVal);
		
		// host
		sVal = prop.getProperty(sComponentName + ".weblogic.host");
		if (sVal == null) {
			sVal = "";
		}
		prop.setProperty(ENVPOP_this_WL_HOST, sVal);
		
		// port
		sVal = prop.getProperty(sComponentName + ".weblogic.port");
		if (sVal == null) {
			sVal = "";
		}
		prop.setProperty(ENVPOP_this_WL_PORT, sVal);
		
		
	}
	
	private void createJBossEnvProperties(Properties prop, String sComponentName) {
		String sVal;
		
		// wl_home
		sVal = prop.getProperty(sComponentName + ".jboss.home");
		if (sVal == null) {
			sVal = "";
		}
		prop.setProperty(ENVPOP_this_JBOSS_HOME, sVal);

		// wl_version
		sVal = prop.getProperty(sComponentName + ".jboss.version");
		if (sVal == null) {
			sVal = "";
		}
		prop.setProperty(ENVPOP_this_JBOSS_VERSION, sVal);
		
		// host
		sVal = prop.getProperty(sComponentName + ".jboss.host");
		if (sVal == null) {
			sVal = "";
		}
		prop.setProperty(ENVPOP_this_JBOSS_HOST, sVal);
		
		// port
		sVal = prop.getProperty(sComponentName + ".jboss.port");
		if (sVal == null) {
			sVal = "";
		}
		prop.setProperty(ENVPOP_this_JBOSS_PORT, sVal);
		
		
	}
	
	private int checkWebComponentKey(String sKey)
	{
		int wcType = 0;
		if (sKey != null) {
			int i2;
			int i1 = sKey.indexOf(ENVPROP_weblogic);
			if (i1 > 0) {
				// check if this is the second element
				i2 = sKey.indexOf('.');
				if (i2 == i1) {
					wcType = WC_WEBLOGIC;
				}
			}
			
			if (wcType <= 0) {
				// check JBOSS
				i1 = sKey.indexOf(ENVPROP_jboss);
				
				if (i1 > 0) {
					i2 = sKey.indexOf('.');
					if (i2 == i1) {
						wcType = WC_JBOSS;
					}
				}
			}
		}
		return wcType;
	}
	
	private void collectWebComponents(Properties prop)
	throws ConfigException
	{
		Enumeration enumKey = prop.keys();
		String sKey = null;
		int i1;
		WebComponent wcTmp = new WebComponent(null), wcWork;
		Entry entry;
		Iterator itwc;
		StringBuilder sbWebComponents = new StringBuilder();
		
		int wcType;
		
		try {
			while (enumKey.hasMoreElements()) {
				sKey = (String)enumKey.nextElement();
				
				// get the first part of the key
				i1 = sKey.indexOf('.');
				if (i1 > 0) {
					
					
					// this is a potential web component's property
					wcTmp.webCompName  = sKey.substring(0, i1);
					
					wcWork = (WebComponent)m_htWebComponent.get(wcTmp.webCompName);
					if (wcWork != null) {
						if (!wcWork.confirmed) {
							wcType = checkWebComponentKey(sKey); 
							if (wcType > 0) {
								wcWork.confirmed = true;	// confirmed it is a web component.
								wcWork.wcType = wcType;
							}
						}
					}
					else {
						// add a new one
						wcWork = createWebComponent(prop, wcTmp.webCompName);
						m_htWebComponent.put(wcTmp.webCompName, wcWork);
					}
				}
			}
			
			// remove non-web components
			itwc = m_htWebComponent.entrySet().iterator();
			while (itwc.hasNext()) {
				entry = (Entry)itwc.next();
				
				wcWork = (WebComponent)entry.getValue();
				if (!wcWork.confirmed) {
					itwc.remove();
				}
				else {
					// prepare property
					if (sbWebComponents.length() > 0) {
						sbWebComponents.append(',');
					}
					sbWebComponents.append(wcWork.webCompName);
					
					// for later computation
					if (wcWork.isMaster) {
						if (m_hsMasterWebComponent == null) {
							m_hsMasterWebComponent = new HashSet();
						}
						m_hsMasterWebComponent.add(wcWork.webCompName);
						
						prop.put(wcWork.webCompName + ENVPROP_web_component__master, "true");
					}
					else {
						prop.put(wcWork.webCompName + ENVPROP_web_component__master, "false");
					}
				}
			}
			
			// set property
			prop.put(ENVPROP_global__web_components, sbWebComponents.toString());
			
		}
		catch (Exception e) {
			throw new ConfigException(sKey + e.getMessage(), e);
		}
		
	}
	
	private WebComponent createWebComponent(Properties prop, String sComp)
	throws ConfigException
	{
		WebComponent wcRet = null;
		String keyMaster = sComp + ENVPROP_web_component__master;
		String keySlave = sComp + ENVPROP_web_component__slave_list;
		String sSlaveList, sSlave, sTmp;
		String[] asSlave;
		int i;
		
		
		wcRet = new WebComponent(sComp);

		// check master and slave keys
		sTmp = prop.getProperty(keyMaster);
		wcRet.isMaster = (boolean)(sTmp == null || "true".equalsIgnoreCase(sTmp));
		
		sSlaveList = prop.getProperty(keySlave);
		if (sSlaveList != null) {
			
			if (!wcRet.isMaster) {
				throw new ConfigException("Non-master web component must not have slaves: [" + keyMaster + "]");
			}
			
			// split it
			asSlave = sSlaveList.split("\\s*,\\s*");
			
			if (asSlave.length > 0) {
				wcRet.hsSlave = new HashSet();
				for (i = 0; i < asSlave.length; ++ i) {
					
					if (i == asSlave.length - 1 || i == 0) {
						// the split uses regex which causes the last and first to 
						// have heading and tailing spaces
						sSlave = asSlave[i].trim();
					}
					else {
						sSlave = asSlave[i];
					}
					
					// check if there is any component exists
					if (sSlave.length() > 0) {
						if (wcRet.hsSlave.contains(sSlave)) {
							// invalid, just throw it
							throw new ConfigException("Invalid slave list: " + sSlaveList);
						}
						else if (sSlave.equals(sComp)) {
							throw new ConfigException("Invalid slave list, cannot point slave to itself: " + sSlaveList);
						}
						wcRet.hsSlave.add(sSlave);
					}
				}
			}
		}
		
		
		return wcRet;
	}
	
	/**
	 * 
	 *
	 */
	private void doValidate(Properties prop)
	throws ConfigException
	{
		if (!m_htWebComponent.isEmpty()) {
			
			Iterator itwc = m_htWebComponent.entrySet().iterator();
			Iterator itSlave;
			Entry entry;
			WebComponent wc = null, wcSlave;
			String sSlave;
			while (itwc.hasNext()) {
				entry = (Entry)itwc.next();
				wc = (WebComponent)entry.getValue();
				
				if (wc.hsSlave != null) {
					itSlave = wc.hsSlave.iterator();
					
					while (itSlave.hasNext()) {
						sSlave = (String)itSlave.next();
						
						// check if it's in the master web component set
						if (m_hsMasterWebComponent != null && m_hsMasterWebComponent.contains(sSlave)) {
							// bad
							throw new ConfigException("Master web component [" + 
									sSlave + 
									"] is in the slave list of [" + 
									wc.webCompName + "]");
						}
						else {
							wcSlave = (WebComponent)m_htWebComponent.get(sSlave);
							if (wcSlave.ownerMaster != null) {
								// duplicate slaves
								throw new ConfigException("Duplicate slave [" + sSlave + "]");
							}
							else {
								wcSlave.ownerMaster = wc.webCompName;
								prop.put(sSlave + ENVPROP_web_component__owner_master, wc.webCompName);
							}
						}
					}
				}
			}
			
		}
	}
}
