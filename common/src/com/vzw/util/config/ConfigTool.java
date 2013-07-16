/*
 * Created on Sep 10, 2003
 *
 */
package	com.vzw.util.config;


import java.util.Iterator;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map.Entry;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.io.File;



/**
 * @author hud
 *
 */
public class ConfigTool
{
	/**
	 * Constants
	 */
	public static final String	OPTION_DIR 								= "dir";
	public static final String	OPTION_FILE								= "file";
	public static final String	OPTION_BATCH							= "batch";
	public static final String	OPTION_wlcfgmerge						= "wlcfgmerge";
	
	public static final String	PROPKEY_master_component				= "master_component";
	public static final String 	PROPKEY_env_out_dir						= "env_out_dir";
	
	/**
	 * reference to a base properties file
	 * same and base.env, for backwards compatibility
	 */
	public static final String	PROPKEY_refer_to						= "properties.file.refer.to";
	public static final String	PROPKEY_base_env						= "base.env";

	public static final int		OPTION_DIR_INT							= 1;
	public static final int		OPTION_FILE_INT							= 2;
	public static final int		OPTION_BATCH_INT						= 3;
	public static final int		OPTION_WLCFGMERGE_INT					= 4;

	public static final String	DELIMITER1								= ";+";
	public static final String	DELIMITER2								= ",+";

	public static final String	SUCCEEDED								= "0";
	
	public static final String 	OVERRIDE_PREFIX							= "OVERRIDE.";
	
	/**
	 * predefined properties
	 */
	public static final String PROPKEY_LOCALHOST_IP						= "LOCALHOST_IP";
	public static final String PROPKEY_COMPONENT_RUNTIME_DIR			= "COMPONENT_RUNTIME_DIR";

	/**
	 * instance member
	 */
	private ArgParser		m_ap			= new ArgParser();
	private PropUtil		m_pu			= new PropUtil();
	Properties 				m_propDefault	= null, m_propOverride = null;
	boolean					m_bDebug		= false;

	/**
	 * Class for a command
	 */
	static private class ConfigCommand
	{
		public int			option;
		public String		src, dst;
		public String		serverDir = null;	// for wlmerge
	}


	/**
	 * Constructor
	 */
	public ConfigTool()
	throws ConfigException
	{
		try {

			///////////////////////////////////////////////
			// prepare system properties
			m_propDefault = System.getProperties();
	
			// add LOCALHOST_IP
			if (m_propDefault.getProperty(PROPKEY_LOCALHOST_IP) == null) {
				String sIP = InetAddress.getLocalHost().getHostAddress();
				m_propDefault.setProperty(PROPKEY_LOCALHOST_IP, sIP);
				m_propDefault.setProperty("reserved." + PROPKEY_LOCALHOST_IP, sIP);
			}			
			
			// add COMPONENT_RUNTIME_DIR
			// This property is based on the current
			// directory of the component
			// if it's build time, just pick a dummy value 
			// which is actually not used to avoid validation
			// failure
			// if it's runtime, it should be defined by
			// the startup script
			if (m_propDefault.getProperty(PROPKEY_COMPONENT_RUNTIME_DIR) == null) {
				m_propDefault.setProperty(PROPKEY_COMPONENT_RUNTIME_DIR, "DUMMY");
			}
			
			
			/////////////////////////////////////////////
			// get overridden properties
			buildOverrideProperties();
	
	
			//////////////////////////////////////////////
			// reset before initializing parser
			m_ap.reset();
	
			// register options
			m_ap.addOption(ArgParser.SINGLECHAROPTION, "d", null, "Turn on debug mode.");
			m_ap.addOption(ArgParser.SINGLECHAROPTION, "t", null, "Check if the token replacement in configuration files is complete.");
			m_ap.addOption(ArgParser.SINGLECHAROPTION, "e", null, "Validate the env properties for maste/slave web component settings.");
			
			m_ap.addOption(ArgParser.PARAMOPTION, "-tp", "--token-prefix", "Token prefix, default \"{T}\".");
			m_ap.addOption(ArgParser.PARAMOPTION, "-ts", "--token-suffix", "Token suffix, default \"{/T}\".");
			m_ap.addOption(ArgParser.PARAMOPTION, "-vp", "--var-in-token-prefix", "Token (variable) in token prefix, default \"${\".");
			m_ap.addOption(ArgParser.PARAMOPTION, "-vs", "--var-in-token-suffix", "Token (variable) in token suffix, default \"}\".");
			m_ap.addOption(ArgParser.NOPARAMOPTION, "-h", "--help", "Print this screen.");
	
			m_ap.addOption(ArgParser.PARAMOPTION, "-p", "--prop-file-list", "Properties file list.");
			m_ap.addOption(ArgParser.PARAMOPTION, "-c", "--command-list", "Command list.");
		}
		catch (Exception e) {
			throw new ConfigException(e);
		}
	}

	public void buildOverrideProperties() {
		Properties propSystem = System.getProperties();
		
		Set entrySet = propSystem.entrySet();
		
		Iterator it = entrySet.iterator();
		Entry entry;
		String key, key1, value;
		while (it.hasNext()) {
			
			entry = (Entry)it.next();
			
			key = (String)entry.getKey();
			if (key.startsWith(OVERRIDE_PREFIX)) {
				key1 = key.substring(OVERRIDE_PREFIX.length());
				
				if (m_propOverride == null) {
					m_propOverride = new Properties();
				}
				
				m_propOverride.put(key1, entry.getValue());
			}
		}
	}
	
	public boolean execute(String args[], Properties propExternal)
	throws ConfigException
	{
		try {
			m_ap.parse(args);
	
			///////////////////////////////////////////
			// Check if it's help
			if (m_ap.getSwitch("-h")) {
				return false;
				//throw new Exception("ConfigTool v1.1. Author: Dongliang Hu");
			}
	
			///////////////////////////////////////////
			// check "debug" flag (-d)
			if (m_ap.getSwitch("d"))  {
				m_bDebug = true;
			}
			else {
				m_bDebug = false;
			}
			m_pu.setDebug(m_bDebug);
	
			log("system", m_propDefault);
	
	
			///////////////////////////////////////////
			// Do configuration
			doConfig(propExternal);
		}
		catch (ConfigException ce) {
			throw ce;
		}
		catch (Exception e) {
			throw new ConfigException(e);
		}
		
		// done
		return true;
	}

	private void doConfig(Properties propExternal)
	throws ConfigException
	{
		String sPrefix, sSuffix;
		boolean bRet = true;

		// set token wrapper
		sPrefix = m_ap.getValue("-tp");
		sSuffix = m_ap.getValue("-ts");
		if (sPrefix == null)
			sPrefix = PropUtil.TOKEN_PREFIX;
		if (sSuffix == null)
			sSuffix = PropUtil.TOKEN_SUFFIX;
		m_pu.setTokenWrapper(sPrefix, sSuffix);

		// set var wrapper
		sPrefix = m_ap.getValue("-vp");
		sSuffix = m_ap.getValue("-vs");
		if (sPrefix == null)
			sPrefix = PropUtil.VAR_PREFIX;
		if (sSuffix == null)
			sSuffix = PropUtil.VAR_SUFFIX;
		m_pu.setVarWrapper(sPrefix, sSuffix);

		// get properties list
		String sPropList = m_ap.getValue("-p");
		if (sPropList == null && propExternal == null)
			throw new ConfigException("Missing properties file list.");
		log("proplist = " + (sPropList == null ? "null" : sPropList));
		String[] asProp;
		if (sPropList != null) {
			asProp = sPropList.split(DELIMITER1);
		}
		else {
			asProp = new String[0];
		}

		// get command list
		String sCmdList = m_ap.getValue("-c");
		if (sCmdList == null)
			throw new ConfigException("Missing command list.");
		log("cmdlist = " + sCmdList);
		ArrayList alCmd = getCommandsA(sCmdList);
		
		
		// check the options
		EnvFileValidator envFileValidator = null;
		if (m_ap.getSwitch("e")) {
			// let's use WebCompEnvFileValidation
			envFileValidator = new WebCompEnvFileValidator(m_pu.getVarWrapper());
		}
		else {
			envFileValidator = new DefaultEnvFileValidator(m_pu.getVarWrapper());
		}

		// expand properties
		Properties prop = buildProperties(asProp, envFileValidator, propExternal);
		log("properties", prop);

		// set properties
		m_pu.setProperties(prop);
		
		// for token config validation
		TokenConfigValidator tokenConfigValidator = null;
		if (m_ap.getSwitch("t")) {
			tokenConfigValidator = new DefaultTokenConfigValidator(m_pu.getTokenWrapper());
		}
		
		// go over the commands
		int nSize = alCmd.size();
		ConfigCommand cc;
		
		try {
			for (int i = 0; i < nSize; i ++)  {
	
				cc = (ConfigCommand)alCmd.get(i);
				switch (cc.option)  {
	
					case OPTION_DIR_INT:
						bRet = m_pu.replaceDirTokens(
								cc.src,
								cc.dst,
								tokenConfigValidator
						);
						break;
	
					case OPTION_FILE_INT:
						bRet = m_pu.replaceFileTokens(
								cc.src,
								cc.dst,
								tokenConfigValidator
						);
						break;
						
					case OPTION_WLCFGMERGE_INT:
						m_pu.mergeWLConfig(cc.src, cc.dst, cc.serverDir, tokenConfigValidator);
						break;
	
					default:
						break;
				}
	
				// check if succeeded
				if (!bRet)  {
					throw new ConfigException("Replacing token failed.");
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


	/**
	 * Logging
	 */
	public void log(String msg, Properties prop)
	{
		if (m_bDebug) {
			System.out.println(msg);
			prop.list(System.out);
		}
	}

	public void log(String msg)
	{
		if (m_bDebug) {
			System.out.println(msg);
		}
	}


	/**
	 * Show usage
	 */
	public static void showUsage()
	{
		//Usage:
		// java -jar ConfigTool.jar [options] [params] {-p | --prop-file-list} "<properties file list>" {-c | --command-list} "<command list>"
		// options: -dte
		//     -d
		//         Turn on debug mode
		//
		//     -t
		//		   Check if the token replacement in configuration files is complete.
		//
		//     -e
		//		   Validate the env properties for maste/slave web component settings.
		//
		// params:
		//     -tp "<string>"
		//     --token-prefix "<string>"
		//         Token prefix, default "{T}"
		//     -ts "<string>"
		//     --token-suffix "<string>"
		//         Token suffix, default "{/T}"
		//     -vp "<string>"
		//     --var-in-token-prefix "<string>"
		//         Token (variable) in token prefix, default "${"
		//     -vs "<string>"
		//     --var-in-token-suffix "<string>"
		//         Token (variable) in token suffix, default "}"
		//
		//     -h
		//     --help
		//         Print this screen
		//
		// <properties file list> ::= <properties file path>[; <properties file list>]
		// <command list> ::= <command>[; <command list>]
		// <command> ::= <file command> | <directory command> | <weblogic config merge command>
		// <file command> ::= <source file>, <destination file>
		// <directory command> ::= <source directory>, <destination directory>
		// <weblogic config merge command> ::= wlcfgmerge, <server directory>, <source file>, <destination file>
		//	where <server directory> stands for the the path to the weblogic server instance directory, e.g.
		//	"my-domain/my-server"
		//
		// Note: Any properties file inherits properties in properties files listed previously in
		//       <properties file list>. Any property in a properties file overlays same property
		//       in properties files listed previously in <properties file list>.
		//

		System.out.println("Usage: ");
		System.out.println("java -jar ConfigTool.jar [options] [params] {-p | --prop-file-list} \"<properties file list>\" {-c | --command-list} \"<command list>\"");
		
		System.out.println("options: -dte");
		System.out.println("\t-d");
		System.out.println("\t\tTurn on debug mode");
		System.out.println("\t-t");
		System.out.println("\t\tCheck if the token replacement in configuration files is complete");
		System.out.println("\t-e");
		System.out.println("\t\tValidate the env properties for maste/slave web component settings");
		System.out.println("");
		
		System.out.println("params:");
		System.out.println("\t-tp \"<string>\"");
		System.out.println("\t--token-prefix \"<string>\"");
		System.out.println("\t\tToken prefix, default \"{T}\"");
		System.out.println("\t-ts \"<string>\"");
		System.out.println("\t--token-suffix \"<string>\"");
		System.out.println("\t\tToken suffix, default \"{/T}\"");
		System.out.println("\t-vp \"<string>\"");
		System.out.println("\t--var-in-token-prefix \"<string>\"");
		System.out.println("\t\tToken (variable) in token prefix, default \"${\"");
		System.out.println("\t-vs \"<string>\"");
		System.out.println("\t--var-in-token-suffix \"<string>\"");
		System.out.println("\t\tToken (variable) in token suffix, default \"}\"");
		System.out.println("");
		System.out.println("\t-h");
		System.out.println("\t--help");
		System.out.println("\t\tPrint this screen");
		System.out.println("");
		System.out.println("<properties file list> ::= <properties file path>[; <properties file list>]");
		System.out.println("<command list> ::= <command>[; <command list>]");
		System.out.println("<command> ::= <file command> | < directory command> | <weblogic config merge command>");
		System.out.println("<file command> ::= <source file>, <destination file>");
		System.out.println("<directory command> ::= <source directory>, <destination directory>");
		System.out.println("<weblogic config merge command> ::= wlcfgmerge, <server directory>, <source file>, <destination file>");
		System.out.println("<where <server directory> stands for the the path to the weblogic server instance directory, e.g. \"my-domain/my-server\"");
		System.out.println("");
		System.out.println("Note: Any properties file inherits properties in properties files listed previously in");
		System.out.println("\t<properties file list>. Any property in a properties file overlays same property");
		System.out.println("\tin properties files listed previously in <properties file list>.");

	}


	/**
	 * Gets command list
	 *
	 * @param	command list	delimited by ";", and each option in a command
	 *							is delimited by ","
	 * @return	command list
	 */
	/*
	private ArrayList getCommands(String cmdlist)
	throws Exception
	{
		String[] asCmds = cmdlist.split(DELIMITER1);
		String[] asCmd;
		String sCmd;
		int nOption;
		ConfigCommand cc;
		ArrayList alCmd = new ArrayList();

		for (int i = 0; i < asCmds.length; i ++)  {

			asCmd = asCmds[i].split(DELIMITER2);

			// check argument number
			if (asCmd.length != 3) {
				throw new Exception("Invalid batch command list at: [" + asCmds[i] + "]");
			}

			// check option
			sCmd = asCmd[0].trim();
			log("cmd = " + sCmd);
			if (sCmd.equals(OPTION_DIR))  {
				nOption = OPTION_DIR_INT;
			}
			else if (sCmd.equals(OPTION_FILE))  {
				nOption = OPTION_FILE_INT;
			}
			else  {
				throw new Exception("Invalid command option in batch commands at: [" + asCmds[i] + "]");
			}

			// create a command object
			cc = new ConfigCommand();
			cc.option = nOption;

			// get source and destination
			cc.src = asCmd[1].trim();
			cc.dst = asCmd[2].trim();

			// add to list
			alCmd.add(cc);

		}


		return alCmd;
	}
	*/

	private ArrayList getCommandsA(String cmdlist)
	throws ConfigException
	{
		String[] asCmds = cmdlist.split(DELIMITER1);
		String[] asCmd;
		String	 sCmd;
		ConfigCommand cc;
		ArrayList alCmd = new ArrayList();
		File fSrc;

		// Add functionality of merging config.xml file
		// file, file	- token replacement for file
		// dir, dir		- token replacement for directory
		// wlcfgmerge, file, file 	- token replacement + weblogic config.xml merge
		for (int i = 0; i < asCmds.length; i ++)  {

			asCmd = asCmds[i].split(DELIMITER2);

			// check argument number
			if (asCmd.length == 4) {
				
				sCmd = asCmd[0].trim();
				if (OPTION_wlcfgmerge.equals(sCmd)) {
					cc = new ConfigCommand();
					cc.option = OPTION_WLCFGMERGE_INT;
					cc.serverDir = asCmd[1].trim();
					cc.src = asCmd[2].trim();
					cc.dst = asCmd[3].trim();
					
					
				}
				else {
					// invalid command specified
					throw new ConfigException("Invalid command [" + asCmds[i] + "].");
				}
			}
			else if (asCmd.length == 2) {

	
				// create a command object
				cc = new ConfigCommand();
	
				// get source and destination
				cc.src = asCmd[0].trim();
				cc.dst = asCmd[1].trim();
	
				// check if source file is valid
				fSrc = new File(cc.src);
				if (!fSrc.exists())
					throw new ConfigException("File or directory [" + cc.src + "] doesn't exist.");
	
				if (fSrc.isDirectory())
					cc.option = OPTION_DIR_INT;
				else
					cc.option = OPTION_FILE_INT;
	
			}
			else {
				throw new ConfigException("Invalid command list at: [" + asCmds[i] + "]");
			}

			// add to list
			alCmd.add(cc);

		}


		return alCmd;
	}

	/**
	 * Simple configuration
	 * @deprecated
	 */
	/*
	private void doConfig(int option, String[] args)
	throws Exception
	{
		// check parameter num
		if (args.length != 4 && args.length != 6 && args.length != 8)  {
			throw new Exception("Invalid parameter number: " + args.length);
		}

		boolean			bRet		= true;
		//String			sTokenPrefix, sTokenSuffix;
		//String			sVarPrefix, sVarSuffix;
		Properties 		propOrig	= new Properties();
		Properties		prop;
		FileInputStream	fiSdpEnv;


		// get token prefix/suffix
		if (args.length >= 6)  {
			m_pu.setTokenWrapper(args[4], args[5]);

			if (args.length == 8)  {
				m_pu.setVarWrapper(args[6], args[7]);
			}
		}

		// expand the properties file
		propOrig.putAll(m_propDefault);
		fiSdpEnv	= new FileInputStream(args[1]);
		propOrig.load(fiSdpEnv);
		prop	= m_pu.expandProperties(propOrig, null);
		log("main", prop);
		fiSdpEnv.close();

		// set properties
		m_pu.setProperties(prop);


		// replace tokens
		switch (option)  {
			case OPTION_DIR_INT:
				bRet = m_pu.replaceDirTokens(
						args[2],
						args[3],
						null
				);
				break;

			case OPTION_FILE_INT:
				bRet = m_pu.replaceFileTokens(
						args[2],
						args[3],
						null
				);
				break;

			default:
				break;
		}

		// check if succeeded
		if (!bRet)  {
			throw new Exception("Replacing token failed.");
		}
	}
	*/

	/***
	 * Builds properties including expanding and inheritance
	 */
	/*
	public Properties buildProperties(
			String[] 			asPropFile, 
			EnvFileValidator	envFileValidator,
			Properties			propExternal
	)
	throws ConfigException
	{
		
		if (asPropFile == null) 
			return null;

		FileInputStream[] fis = new FileInputStream[asPropFile.length];
		try {
			for (int i = 0; i < asPropFile.length; i ++)  {
				//fis[i] = new FileInputStream(asPropFile[i].trim());
				loadPropertiesFile
			}
		}
		catch (Exception e) {
			throw new ConfigException(e);
		}

		return buildProperties(fis, asPropFile, envFileValidator, propExternal);
	}
	*/
	
	/*
	public Properties buildProperties(
			File[] 				afPropFile, 
			EnvFileValidator	envFileValidator,
			Properties			propExternal
	)
	throws ConfigException
	{
		if (afPropFile == null) 
			return null;
		
		FileInputStream[] fis = new FileInputStream[afPropFile.length];
		
		String asPropFile[] = new String[afPropFile.length];
		try {
			for (int i = 0; i < afPropFile.length; i ++)  {
				fis[i] = new FileInputStream(afPropFile[i]);
				asPropFile[i] = afPropFile[i].getAbsolutePath();
			}
		}
		catch (Exception e) {
			throw new ConfigException(e);
		}

		return buildProperties(fis, asPropFile, envFileValidator, propExternal);
	}
	*/

	private String findBaseEnv(Properties prop, String propFilePath) throws ConfigException {
		String ret = prop.getProperty(PROPKEY_base_env);
		if (ret == null) {
			ret = prop.getProperty(PROPKEY_refer_to);
		}
		
		if (ret != null) {
			// check whether the file exists
			File envFile = new File(ret);
			if (!envFile.exists()) {
				// attach the path
				File propFile = new File(propFilePath);
				String parentDir = propFile.getParent();
				if (parentDir == null) {
					throw new ConfigException("Invalid base env: [" + ret + "] in [" + propFilePath + "]");
				}

				String ret1 = parentDir + File.separator + ret;
				File envFile1 = new File(ret1);
				if (!envFile1.exists()) {
					throw new ConfigException("Invalid base env: [" + ret1 + "] in [" + propFilePath + "]");
				}
				
				ret = ret1;
			}
		}
		return ret;
	}
	
	public Properties loadPropertiesFile(String propFilePath) throws ConfigException {
		
		Properties propRet = null;
		
		try {
			Properties prop1 = new Properties();
			
			FileInputStream fis = new FileInputStream(propFilePath);
			
			prop1.load(fis);
			
			// check base env
			String baseEnvFile = findBaseEnv(prop1, propFilePath);
			
			if (baseEnvFile != null) {
				// we need load this first (recursive call)
				propRet = loadPropertiesFile(baseEnvFile);
			}
			
			if (propRet != null) {
				propRet.putAll(prop1);
			}
			else {
				propRet = prop1;
			}
			
		}
		catch (ConfigException ce) {
			throw ce;
		}
		catch (Exception e) {
			throw new ConfigException(e);
		}
		
		return propRet;
		
	}
	
	public Properties buildProperties(
			String[]			afPropFile,
			EnvFileValidator	envFileValidator,
			Properties			propExternal
	)
	throws ConfigException
	{
		if (afPropFile == null) 
			return null;
		
		Properties prop0 = null;
		prop0 = new Properties();
		prop0.putAll(m_propDefault);
		log("prop0_start", prop0);
		
		Properties propRet = null;
		String baseEnvFile = null;
		
		Properties propTemp;
		try {
		
			for (int i = 0; i < afPropFile.length; i ++)  {
				
				propTemp = loadPropertiesFile(afPropFile[i]);
				prop0.putAll(propTemp);
				
			}
	
			if (propExternal != null) {
				prop0.putAll(propExternal);
			}
			log("prop0", prop0);
			log("prop0 size = " + prop0.size());
			
			propRet = m_pu.expandProperties(prop0, envFileValidator);
			outputProp(propRet);
			
		}
		catch (Exception e) {
			throw new ConfigException(e);
		}
		
		return propRet;
		
	}
	
	public Properties buildProperties(
			Properties 			prop,
			EnvFileValidator	envFileValidator
	)
	throws ConfigException
	{
		Properties propRet = null;
		try {
			
			Properties propEx = new Properties();
			propEx.putAll(m_propDefault);
			propEx.putAll(prop);
			propRet = m_pu.expandProperties(propEx, envFileValidator);
			outputProp(propRet);
		}
		catch (Exception e) {
			throw new ConfigException(e);
		}
		
		return propRet;
	}
	
	private void outputProp(Properties prop)
	throws ConfigException
	{
		String envOutDir = prop.getProperty(PROPKEY_env_out_dir);
		if (envOutDir != null) {
			// if the directory doesn't exist try to create it
			File fDir = new File(envOutDir);
			if (fDir.exists()) {
				if (fDir.isFile()) {
					// not good, we should throw exception
					throw new ConfigException("Unable to output env properties to " + envOutDir +
							" which is a file but not a directory.");
				}
			}
			else {
				// let's make it
				if (!fDir.mkdirs()) {
					throw new ConfigException("Unable to output env properties to " + envOutDir +
							" which can not be created.");
				}
			}
			
			try {
				// write it
				prop.list(new PrintStream(
							new FileOutputStream(
									fDir.getAbsolutePath() + File.separator + "env_ex.properties")));
			}
			catch (Exception e) {
				throw new ConfigException("Unable to output env properties to " + envOutDir, e);
			}
		}
	}

	/**
	 * Batch configuration
	 * @deprecated
	 */
	/*
	private void doBatchConfig(int option, String[] args)
	throws Exception
	{
		// batch option
		if (args.length != 3 && args.length != 5 && args.length != 7)  {
			throw new Exception("Invalid parameter number: " + args.length);
		}

		// get token prefix/suffix
		if (args.length >= 5)  {
			m_pu.setTokenWrapper(args[3], args[4]);

			if (args.length == 7)  {
				m_pu.setVarWrapper(args[5], args[6]);
			}
		}

		boolean bRet = true;

		// properties file list
		log("envlist = " + args[1]);
		String[] asProp = args[1].split(DELIMITER1);

		// command list
		log("cmdlist = " + args[2]);
		ArrayList alCmd = getCommands(args[2]);

		// expand properties
		Properties prop = buildProperties(asProp, null);
		log("batch", prop);

		// set properties
		m_pu.setProperties(prop);

		// go over the commands
		int nSize = alCmd.size();
		ConfigCommand cc;
		for (int i = 0; i < nSize; i ++)  {

			cc = (ConfigCommand)alCmd.get(i);
			switch (cc.option)  {

				case OPTION_DIR_INT:
					bRet = m_pu.replaceDirTokens(
							cc.src,
							cc.dst,
							null
					);
					break;

				case OPTION_FILE_INT:
					bRet = m_pu.replaceFileTokens(
							cc.src,
							cc.dst,
							null
					);
					break;

				default:
					break;
			}

			// check if succeeded
			if (!bRet)  {
				throw new Exception("Replacing token failed.");
			}
		}


	}
	*/
	
	/**
	 * Main procedure for command line usage
	 */
	public static void main(String[] args)
	{
		try {

			// Construct object
			ConfigTool ct = new ConfigTool();

			// Parse command
			if (!ct.execute(args, ct.m_propOverride)) {
				// print help message
				showUsage();
			}

		}
		catch (ConfigException e)  {
			//System.out.println(e.getMessage());
			e.printStackTrace();
			showUsage();
			System.exit(1);
		}

	}


}
