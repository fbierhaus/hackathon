package com.vzw.util.config;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 *
 * There are several types of arguments
 *
 */

import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
//import java.util.ArrayList;

public class ArgParser
{
	public static final int SINGLECHAROPTION = 1;  // -abcdefg  (always first arg)
	public static final int PARAMOPTION = 2;  // -opt "some value" | -opt2 (no param)
	public static final int NOPARAMOPTION = 3; // start | stop | restart ... | -a | -b
	public static final int STICKYPARAMOPTION = 4; // -Xms128m | -Xloggc:file_path  (-Xms and -Xloggc: will be the key)

	public static final String SWITCHOPTIONVALUE = "t";

	// options in different category
	private HashMap m_hmSingleCharOption = new HashMap();

	private HashMap m_hmParamOption = new HashMap();
	private HashMap m_hmNoParamOption = new HashMap();
	private HashMap m_hmStickyParamOption = new HashMap();

	private HashMap m_hmParamLongOption = new HashMap();
	private HashMap m_hmNoParamLongOption = new HashMap();
	private HashMap m_hmStickyParamLongOption = new HashMap();

	// all options
	private HashMap m_hmOption = new HashMap();

	private class Option {
		int type;
		String option;
		String long_option; // unused by single-char option
		String desc;
		String value;

		/**
		 * Constructor
		 * @param nType
		 * @param sOption
		 * @param sLongOption
		 * @param sDesc
		 */
		public Option(int nType, String sOption, String sLongOption, String sDesc)
		{
			type = nType;
			option = new String(sOption);
			long_option = sLongOption != null ? new String(sLongOption) : null;
			desc = new String(sDesc);
			value = null;
		}

	}


	/**
	 * constructor
	 */
	public ArgParser()
	{
    }

	/**
	 *  Resets the parser for another parsing
	 */
	public void reset()
	{
		// clear the hash maps
		m_hmSingleCharOption.clear();
		m_hmParamOption.clear();
		m_hmNoParamOption.clear();
		m_hmStickyParamOption.clear();
		m_hmParamLongOption.clear();
		m_hmNoParamLongOption.clear();
		m_hmStickyParamLongOption.clear();
		m_hmOption.clear();
	}
	/**
	 * Registers single-char option
	 * @param sSingleCharOption
	 */
	public void addOption(
		   int nType,
		   String sOption,
		   String sLongOption,
		   String sDesc
	) throws Exception
	{
		if (nType == SINGLECHAROPTION) {
			if (sOption.length() != 1)
				throw new Exception(
					"Single-char option string has more than one character: " +
					sOption + "(" + sOption.length() + ")");
		}

		// check if sOption is null
		if (sOption == null)
			throw new Exception("option is null.");

		// Check if this option has already been registered
		if (sOption.equals(m_hmOption.get(sOption)))
			throw new Exception("The option \"" + sOption + "\" has already been registered");

		Option opt = new Option(nType, sOption, sLongOption, sDesc);

		switch (nType)  {
			case SINGLECHAROPTION:
				m_hmSingleCharOption.put(opt.option, opt);
				break;
			case PARAMOPTION:
				m_hmParamOption.put(opt.option, opt);
				if (sLongOption != null)
					m_hmParamLongOption.put(opt.long_option, opt);
				break;
			case NOPARAMOPTION:
				m_hmNoParamOption.put(opt.option, opt);
				if (sLongOption != null)
					m_hmNoParamLongOption.put(opt.long_option, opt);
				break;
			case STICKYPARAMOPTION:
				m_hmStickyParamOption.put(opt.option, opt);
				if (sLongOption != null)
					m_hmStickyParamLongOption.put(opt.long_option, opt);
				break;
		}

		// for getting purpose only
		m_hmOption.put(opt.option, opt);
	}

	/**
	 * Gets switch option (single char, noparam option)
	 * @param sOption
	 * @return boolean
	 */
	public boolean getSwitch(
		   String sOption
	)
	{
		Option opt = (Option)m_hmOption.get(sOption);
		if (opt == null)
			return false;
		else if (opt.value == null)
			return false;
		else if (!opt.value.equals(SWITCHOPTIONVALUE))
			return false;

		return true;
	}

	/**
	 * Gets parameter value
	 *
	 * @param sOption
	 * @return
	 */
	public String getValue(
		   String sOption
	)
	{
		Option opt = (Option)m_hmOption.get(sOption);
		if (opt == null)
			return null;
		else
			return opt.value;
	}

	/**
	 * Parses the input command-line
	 * @param args
	 * @throws java.lang.Exception
	 */
	public void parse(
		   String[] args
	) throws Exception
	{
		// return immediately if no args
		int nArgNum = args.length;
		if (nArgNum == 0)
			return;

		// Before parsing, reset the value with null
		resetValues();

		int i;
		//#Option opt;

		// generate a list of args
		String[] argsWork = new String[nArgNum];
		for (i = 0; i < nArgNum; i ++)  {
			argsWork[i] = args[i];
		}

		// find single char option, always the first param and starts with "-"
		findSingleCharOption(argsWork);

		// Go over the rest of the arguments
		i = argsWork[0] == null ? 1 : 0;
		for (; i < nArgNum; i ++)  {

			// Check Param Option
			if (i < nArgNum - 1) {
				if (checkParamOption(argsWork[i], argsWork[i + 1])) {
					i++;
					continue;
				}
			}

			// check sticky param option
			if (checkStickyParamOption(argsWork[i]))
				continue;

			// check no param options
			if (checkNoParamOption(argsWork[i]))
				continue;

			// no match, throw
			throw new ConfigException("Invalid option: " + argsWork[i]);
		}

	}

	/**
	 * Checks param option
	 *
	 * @param sOption
	 * @param sValue
	 * @return
	 * @throws java.lang.Exception
	 */
	private boolean checkParamOption(
		   String sOption,
		   String sValue
	) throws Exception
	{
		//now go over the argsWork and check non-null strings
		Option opt;

		// find the option
		opt = (Option)m_hmParamOption.get(sOption);
		if (opt == null) {
			// check long option
			opt = (Option)m_hmParamLongOption.get(sOption);
			if (opt == null)
				return false;
		}

		if (opt.value != null)
			new Exception("Duplicated option: \"" + sOption + "\".");

		opt.value = sValue;
		return true;
	}


	/**
	 * Checks sticky param option
	 *
	 * @param sOption
	 * @return
	 */
	private boolean checkStickyParamOption(
		   String sOption
	)
	{
		Collection cln = m_hmStickyParamOption.values();
		Iterator it = cln.iterator();
		Option opt;

		while (it.hasNext())  {
			opt = (Option)it.next();

			// check the option value
			if (sOption.startsWith(opt.option)) {
				opt.value = sOption.substring(opt.option.length());
				return true;
			}
			else if (opt.long_option != null && sOption.startsWith(opt.long_option)) {
				opt.value = sOption.substring(opt.long_option.length());
				return true;
			}
		}

		// didn't find
		return false;
	}

	/**
	 *
	 * @param sOption
	 * @return
	 * @throws java.lang.Exception
	 */
	private boolean checkNoParamOption(
		   String sOption
	) throws Exception
	{
		//now go over the argsWork and check non-null strings
		Option opt;

		// find the option
		opt = (Option)m_hmNoParamOption.get(sOption);
		if (opt == null) {
			opt = (Option)m_hmNoParamLongOption.get(sOption);
			if (opt == null)
				return false;
		}

		if (opt.value != null)
			new Exception("Duplicated option: \"" + sOption + "\".");

		opt.value = SWITCHOPTIONVALUE;
		return true;
	}

	/**
	 * Finds single char option
	 *
	 * @param argsWork
	 * @throws java.lang.Exception
	 */
	private void findSingleCharOption(
		   String[] argsWork
	) throws Exception
	{
		String arg = argsWork[0];
		int idx, nArgLen;
		Option opt;
		String sOption;

		if (m_hmSingleCharOption.isEmpty())
			return;

		nArgLen = arg.length();



		// a "-" is allowed at the beginning of the first parameter
		if (arg.charAt(0) == '-') {
			idx = 1;
		}
		else {
			idx = 0;
		}

		for (; idx < nArgLen; idx ++)  {
			sOption = arg.substring(idx, idx + 1);

			opt = (Option)m_hmSingleCharOption.get(sOption);
			//if (opt == null)
			//	throw new Exception("Invalid single-char option \"" + sOption + "\".");
			if (opt == null) {
				// meaning there isn't any single char options, just get out
				return;
			}

			if (opt.value != null)
				throw new Exception("Duplicated single-char option \"" + sOption + "\".");

			opt.value = SWITCHOPTIONVALUE;
		}

		argsWork[0] = null;

	}

	private void resetValues()
	{
		Collection cln = m_hmOption.values();
		Iterator it = cln.iterator();
		Option opt;

		while (it.hasNext())  {
			opt = (Option)it.next();
			opt.value = null;
		}

	}
}
