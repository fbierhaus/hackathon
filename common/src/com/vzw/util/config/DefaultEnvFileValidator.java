/**
 * 
 */
package com.vzw.util.config;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author hudongl
 *
 */
public class DefaultEnvFileValidator implements EnvFileValidator 
{
	public static final String NAME = "default";

	
	private static final PropUtil.TokenWrapper DEFAULTVARWRAPPER = new PropUtil.TokenWrapper(PropUtil.VAR_PREFIX, PropUtil.VAR_SUFFIX);
	private PropUtil.TokenWrapper m_trVar = DEFAULTVARWRAPPER;
	
	
	
	public DefaultEnvFileValidator()
	{
		// nothing
	}
	
	public DefaultEnvFileValidator(PropUtil.TokenWrapper varWrapper)
	{
		init(varWrapper);
	}
	
	public void init(PropUtil.TokenWrapper varWrapper)
	{
		m_trVar = varWrapper;
	}

	/* (non-Javadoc)
	 * @see com.vzw.config.EnvFileValidator#validate(java.util.Properties)
	 */
	public void validate(Properties prop)
	throws ConfigException
	{
		
		// perform the basic validation
		// just check if any property value has ${ and $}
		if (prop == null || m_trVar.prefix == null || m_trVar.suffix == null) {
			return; // do nothing
		}
		
		try {
			Collection varCol = prop.values();
			Iterator it = varCol.iterator();
			String sVal;
			while (it.hasNext()) {
				sVal = (String)it.next();
				if (sVal != null) {
					//if (sVal.indexOf(m_trVar.prefix) >= 0 || sVal.indexOf(m_trVar.suffix) >= 0) {
					if (sVal.indexOf(m_trVar.prefix) >= 0) {
						throw new ConfigException("Property value: \"" + sVal + "\" is not expanded");
					}
				}
			}
		}
		catch (ConfigException ce) {
			throw ce;
		}
		catch (Exception e) {
			throw new ConfigException(e);
		}

	}

}
