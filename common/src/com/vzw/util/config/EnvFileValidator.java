/**
 * 
 */
package com.vzw.util.config;

import java.util.Properties;

/**
 * @author hudongl
 *
 */
public interface EnvFileValidator {
	public void validate(Properties prop) throws ConfigException;
}
