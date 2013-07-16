/**
 * 
 */
package com.vzw.util.config;

/**
 * @author hudongl
 *
 */
public class DefaultTokenConfigValidator implements TokenConfigValidator 
{
	private static final PropUtil.TokenWrapper DEFAULTTOKENWRAPPER = new PropUtil.TokenWrapper(PropUtil.TOKEN_PREFIX, PropUtil.TOKEN_SUFFIX);
	private PropUtil.TokenWrapper m_trToken = DEFAULTTOKENWRAPPER;
	protected static final int DISPLAY_BAD_LEN = 100;
	
	
	public DefaultTokenConfigValidator()
	{
		// nothing
	}
	
	public DefaultTokenConfigValidator(PropUtil.TokenWrapper tokenWrapper)
	{
		init(tokenWrapper);
	}
	
	public void init(PropUtil.TokenWrapper tokenWrapper)
	{
		m_trToken = tokenWrapper;
	}

	/* (non-Javadoc)
	 * @see com.vzw.config.TokenConfigValidator#validate(java.lang.StringBuilder)
	 */
	public void validate(StringBuilder sb) throws ConfigException 
	{
		// try to find any matches of token prefixes and suffixes in the sb
		if (m_trToken.prefix == null || m_trToken.prefix.length() == 0 ||
			m_trToken.suffix == null || m_trToken.suffix.length() == 0)
		{
			return; // do nothing
		}
		
		// In order to display more information, print out the 100 chars before
		// and 100 chars after segment.
		int nBad;
		StringBuilder sbBad = null;
		if ((nBad = sb.indexOf(m_trToken.prefix)) >= 0 || (nBad = sb.indexOf(m_trToken.prefix)) >= 0) {
			sbBad = new StringBuilder("Token replacement is not complete: >>>");
			sbBad.append(sb.substring(nBad - DISPLAY_BAD_LEN >= 0 ? nBad - DISPLAY_BAD_LEN : 0, 
					nBad + DISPLAY_BAD_LEN > sb.length() ? sb.length() : nBad + DISPLAY_BAD_LEN))
				 .append("<<<");
		}
		
		if (sbBad != null) {
			throw new ConfigException(sbBad.toString());
		}

	}

}
