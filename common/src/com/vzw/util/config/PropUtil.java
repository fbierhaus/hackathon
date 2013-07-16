/*
 * Created on Nov. 17, 2003
 *
 */
package	com.vzw.util.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.FileFilter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//import java.util.Hashtable;
//import javax.naming.Context;
//import javax.naming.InitialContext;

//import weblogic.common.T3StartupDef;
//import weblogic.common.T3ServicesDef;
//import weblogic.management.Helper;

//import java.io.FileInputStream;




/**
 * @author hud
 *
 */
public class PropUtil
{
	/**
	 * constants
	 */
	private	static final int		SB_INIT_SIZE		= 1024;
	private	static final int		MAX_EXPAND_LEVEL	= 20;
	private	static final int		FILE_BUF_SIZE		= 2048;
	private static final int        INTERNAL_BUF_SIZE   = 4069;

	/**
	 * status codes
	 */
//	# private	static final int		SELECT_ACCEPT		= 0;
//	# private	static final int		SELECT_REFUSE		= 1;
//	# private	static final int		SELECT_ERROR		= 2;


	/**
	 * var key decorator
	 */
	public static final	String					VAR_PREFIX			= "${";
	public static final	String					VAR_SUFFIX			= "}";
	public static final	String					KEY_FIELD_SEP		= ".";
	public static final	String					TOKEN_PREFIX		= "{T}";
	public static final	String					TOKEN_SUFFIX		= "{/T}";
	public static final	String					SELECT_BEGIN_TAG0	= "{SELECT";
	public static final	String					SELECT_BEGIN_TAG1	= "}";
	public static final	String					SELECT_EQUAL_TAG	= "=";
	public static final	String					SELECT_NOTEQUAL_TAG	= "!=";
	public static final	String					SELECT_GT_TAG		= ">";
	public static final	String					SELECT_LT_TAG		= "<";
	public static final	String					SELECT_GE_TAG		= ">=";
	public static final	String					SELECT_LE_TAG		= "<=";
	public static final	String					SELECT_IN_TAG		= "in";
	public static final	String					SELECT_END_TAG		= "{/SELECT}";
	public static final	String					SELECT_AND_TAG		= "&&";
	public static final	String					SELECT_OR_TAG		= "||";
	public static final	String					SELECT_NOT_TAG		= "!";
	public static final	String					WHITE_CHARSET		= "\t\n\u000B\f\r";

	public static final String					LEFT_PAREN			= "(";
	public static final String					RIGHT_PAREN			= ")";
	public static final String					NEWLINE0			= "\r\n";//System.getProperty("line.separator");
	public static final char					NEWLINE1			= '\n';//System.getProperty("line.separator");
	public static final char					NEWLINE2			= '\r';//System.getProperty("line.separator");
	public static final String					NEWLINE 			= "\n";//System.getProperty("line.separator");

	public static final String					EMPTY_STR			= "";
	public static final String					WHITE_NO_NEWLINE;


	static {

		// construct WHITE_NO_NEWLINE
		int i, len;
		char ch;
		len = WHITE_CHARSET.length();
		StringBufferEx sb = new StringBufferEx();

		for (i = 0; i < len; i ++) {
			ch = WHITE_CHARSET.charAt(i);
			if (NEWLINE.indexOf(ch) < 0)  {
				sb.append(ch);
			}
		}
		WHITE_NO_NEWLINE = sb.toString();

	}


	/**
	 * instance	member
	 *
	 */
	//private	String	m_sVarPrefix		= VAR_PREFIX;
	//private	String	m_sVarSuffix		= VAR_SUFFIX;
	private TokenWrapper m_trVar = new TokenWrapper(VAR_PREFIX, VAR_SUFFIX);
	
	
	//private	String	m_sTokenPrefix		= TOKEN_PREFIX;
	//private	String	m_sTokenSuffix		= TOKEN_SUFFIX;
	private TokenWrapper m_trToken = new TokenWrapper(TOKEN_PREFIX, TOKEN_SUFFIX);
	
	private	String	m_sSelectBeginTag0	= SELECT_BEGIN_TAG0;
	private	String	m_sSelectBeginTag1	= SELECT_BEGIN_TAG1;
	private	String	m_sSelectEqualTag	= SELECT_EQUAL_TAG;
	private	String	m_sSelectNotEqualTag	= SELECT_NOTEQUAL_TAG;
	private	String	m_sSelectGTTag		= SELECT_GT_TAG;
	private	String	m_sSelectLTTag		= SELECT_LT_TAG;
	private	String	m_sSelectGETag		= SELECT_GE_TAG;
	private	String	m_sSelectLETag		= SELECT_LE_TAG;
	private	String	m_sSelectINTag		= SELECT_IN_TAG;
	private	String	m_sSelectAndTag		= SELECT_AND_TAG;
	private	String	m_sSelectOrTag		= SELECT_OR_TAG;
	private	String	m_sSelectEndTag		= SELECT_END_TAG;
	private	String	m_sSelectNotTag		= SELECT_NOT_TAG;

	private String 	m_sLeftParen		= LEFT_PAREN;
	private String 	m_sRightParen		= RIGHT_PAREN;
	private	int		m_nMaxExpandLevel	= MAX_EXPAND_LEVEL;
	private	Properties m_prop			= null;
	private	int	m_nMaxKeyLen			= 0;
	private	boolean	m_bDebug			= false;

	/**
	 * Temporary vars
	 */
	private	int	mt_nBeginTag0Len;
	private	int	mt_nBeginTag1Len;
	private	int	mt_nEqualTagLen;
	private	int	mt_nNotEqualTagLen;
	private	int	mt_nGTTagLen;
	private	int	mt_nLTTagLen;
	private	int	mt_nGETagLen;
	private	int	mt_nLETagLen;
	private	int	mt_nINTagLen;
	private	int	mt_nEndTagLen;
	private	int	mt_nNotTagLen;
	private	int	mt_nAndTagLen;
	private	int	mt_nOrTagLen;
	private int	mt_nLeftParenLen;
	private int mt_nRightParenLen;
	private String mt_sTokenPrefix;
	private String mt_sTokenSuffix;
	private int mt_nTokenPrefixLen;
	private int mt_nTokenSuffixLen;
	private	Properties mt_prop;
	private	int	mt_nMaxKeyLen;
	private StringBufferEx mt_sbGetValue = new StringBufferEx(SB_INIT_SIZE);
	private char[] mt_chTemp;
	//# private int mt_nNewLineLen;
	
	
	/**
	 * for proprties expanding
	 */
	private static class EXPPROPSTATUS {
		String			result = null;
		int				idxNext = -1;
	}
	
	
	/**
	 * Token wrapper
	 */
	public static class TokenWrapper
	{
		public String				prefix;
		public String				suffix;
		
		public TokenWrapper(String _prefix, String _suffix)
		{
			set(_prefix, _suffix);
		}
		
		public void set(String _prefix, String _suffix)
		{
			prefix = _prefix;
			suffix = _suffix;
		}
	}

	/**
	 * inner class to reprensent a key-value pair
	 */
	public static class	Pair
	{
		boolean				good;
		public String		key;
		public String		value;
		public String		keystr;

		public Pair()
		{
			key		= null;
			value	= null;
			keystr	= null;
			good	= false;
		}

		public Pair(String _key, String	_value,	String _keystr)
		{
			key			= _key;
			value		= _value;
			keystr		= _keystr;
			good		= false;
		}

		boolean	hasLoopReference()
		{
			return value.indexOf(keystr) >=	0;
		}


	}

	/**
	 * inner class to represent	Value in select	expression
	 */
	private	static final int			EXPRVALTYPE_KEY		= 1;
	private	static final int			EXPRVALTYPE_STRING	= 2;
	private	static final int			EXPRVALTYPE_NUMBER	= 8;
	private	static final int			EXPRVALTYPE_BOOL	= 3;
	private	static final int			EXPRVALTYPE_NONE	= 4;
	private	static final int			EXPRVALTYPE_OR		= 5;
	private	static final int			EXPRVALTYPE_AND		= 6;
	private	static final int			EXPRVALTYPE_NOT		= 7;
	private	static final int			EXPRVALTYPE_EQUAL	= 100;
	private	static final int			EXPRVALTYPE_NOTEQUAL	= 101;
	private	static final int			EXPRVALTYPE_GT	= 102;
	private	static final int			EXPRVALTYPE_LT	= 103;
	private	static final int			EXPRVALTYPE_GE	= 104;
	private	static final int			EXPRVALTYPE_LE	= 105;
	private	static final int			EXPRVALTYPE_IN	= 106;
	private	static final int			EXPRVALTYPE_LEFTPAREN	= 9;
	private	static final int			EXPRVALTYPE_RIGHTPAREN	= 10;

//<expression> ::=
//				<normal	expression>
//			|	<unary expression>
//
//<normal expression> ::=
//				<term> {<add op> <term>}
//
//<unary expression> ::=
//				<not op> [<term> | <unary expression>]
//
//<term> ::=
//				'('	<expression> ')'
//			|	<unary expression>
//			|	<factor> {<mul op> <factor>}
//
//<factor> ::=
//				<key>
//			|	<value>
//			| 	<number>
//
//<key>	::=
//				{chars excluding whitespace	and	'"'}
//
//<value> ::=
//				'"'	{<chars	excluding '"'> | '\"'} '"'
//
//<number ::=
//				{<signed long value>}
//
//<add op> ::=
//				'&&'
//			|	'||'
//
//<mul op> ::=
//				'=' | '>' | '<' | '>=' | '<=' | '!=' | in
//		(note: >, <, >=, <= only works for number
//
//<not op> ::=
//				'!'


	public static class	ExprToken
	{
		int			type;
		String		strvalue;
		boolean		boolvalue;
		long		longvalue;

		public void	set(ExprToken et)
		{
			type = et.type;
			strvalue = et.strvalue;
			boolvalue =	et.boolvalue;
		}

	}


	/**
	 * inner class for recursive token replacement
	 */
	public static class	ReplaceTokenSearchFileCallback
			extends	SearchFileCallback
	{
		/**
		 * members
		 */
		private	PropUtil		m_propUtil;
		private	Properties		m_propEnv;
		private	String			m_sTokenPrefix;
		private	String			m_sTokenSuffix;
		private	StringBufferEx	m_sbSrc, m_sbDst;
		private	int				m_nMaxKeyLen;
		private TokenConfigValidator	m_tokenConfigValidator = null;
		//# private	boolean			m_bSucceeded;

		/**
		 * constructor
		 */
		public ReplaceTokenSearchFileCallback(
				PropUtil		proputil,
				Properties		prop,
				int				maxkeylen,
				String			tokenprefix,
				String			tokensuffix,
				TokenConfigValidator	tokenConfigValidator
		)
		{
			super();

			m_propUtil		= proputil;
			m_sbSrc			= new StringBufferEx(FILE_BUF_SIZE);
			m_sbDst			= new StringBufferEx(FILE_BUF_SIZE);
			m_nMaxKeyLen	= maxkeylen;
			m_tokenConfigValidator = tokenConfigValidator;

			init(prop, tokenprefix,	tokensuffix);
		}

		/**
		 * init	before searching
		 */
		public void	init(
				Properties		prop,
				String			tokenprefix,
				String			tokensuffix
		)
		{
			super.init();

			m_propEnv		= prop;
			m_sTokenPrefix	= tokenprefix;
			m_sTokenSuffix	= tokensuffix;


		}



		/**
		 * process function
		 */
		public boolean execFileProc(File srcfile)
		{

			if (! srcfile.isDirectory())  {
				try	{
					m_propUtil.readFile(m_sbSrc, srcfile.getPath());
					m_propUtil.log("replacetoken: srcfile =	" +	srcfile.getPath());
					m_sbDst.clear();

					m_propUtil.processSelect(
							m_sbSrc,		// src
							m_sbDst,		// dst (reuse)
							m_propEnv,
							m_nMaxKeyLen
					);

					m_sbSrc.clear();
					m_propUtil.replaceTokens(
							m_sbDst,
							m_sbSrc,		// reuse
							m_propEnv,
							m_nMaxKeyLen,
							m_sTokenPrefix,
							m_sTokenSuffix
					);
					
					m_propUtil.writeFile(m_sbSrc, srcfile.getPath());
					
					// check it if necessary
					if (m_tokenConfigValidator != null) {
						m_tokenConfigValidator.validate(m_sbSrc.getStringBuffer());
					}
				}
				catch (Exception e)	 {
					System.out.println("Error occured in [src file: " + srcfile.getPath() + "]");
					e.printStackTrace(System.err);
					setFailed();
				}
			}

			return true;
		}


	}

	/**
	 * inner class for copy	directory
	 */
	public static class	CopyDirSearchFileCallback
			extends	SearchFileCallback
	{
		/**
		 * members
		 */
		PropUtil	m_propUtil;
		File		m_fAbsSrcDir;
		File		m_fAbsDstDir;

		/**
		 * Constructor
		 */
		public CopyDirSearchFileCallback(
				PropUtil	proputil,
				File		fAbsSrcDir,
				File		fAbsDstDir
		)
		{
			super();

			m_propUtil			= proputil;
			m_fAbsSrcDir		= fAbsSrcDir;
			m_fAbsDstDir		= fAbsDstDir;
		}

		/**
		 * process function
		 */
		public boolean execFileProc(File srcfile)
		{
			try	{
				// get the dst file	path
				String		sDstPath = m_fAbsDstDir	+ srcfile.getCanonicalPath().substring(m_fAbsSrcDir.getPath().length());
				File		fDstPath	= new File(sDstPath);

				m_propUtil.log("copy: srcfile =	" +	srcfile.getPath());
				m_propUtil.log("copy: dstfile =	" +	sDstPath);

				if (srcfile.isDirectory())	{

					// create directory
					if (!fDstPath.exists()) {
						fDstPath.mkdirs();
					}
				}
				else  {
					
					File fPar = fDstPath.getParentFile();
					if (!fPar.exists()) {
						fPar.mkdirs();
					}
					
					if (!m_propUtil.copyFile(srcfile.getPath(),	fDstPath.getPath()))  {
						setFailed();
					}

				}
			}
			catch (Exception e)	{
				e.printStackTrace(System.err);
				setFailed();
			}



			return true;
		}

	}


	/**
	 * inner class for delete directory
	 */
	public static class	DeleteDirSearchFileCallback
			extends	SearchFileCallback
	{

		/**
		 * process function
		 */
		public boolean execFileProc(File filetodelete)
		{

			try	{
				if (!filetodelete.isDirectory())  {
					filetodelete.delete();
				}

			}
			catch (Exception e)	{
				e.printStackTrace(System.err);
				setFailed();
			}
			return true;

		}

		public boolean execAfterSubdir(File	filetodelete)
		{
			try	{
				filetodelete.delete();
			}
			catch (Exception e)	{
				e.printStackTrace(System.err);
				setFailed();
			}

			return true;
		}

	}



	/**
	 * default constructor,	manditory
	 */
	public PropUtil()
	{
		//@	TODO
		mt_nBeginTag0Len		= m_sSelectBeginTag0.length();
		mt_nBeginTag1Len		= m_sSelectBeginTag1.length();
		mt_nEqualTagLen			= m_sSelectEqualTag.length();
		mt_nNotEqualTagLen		= m_sSelectNotEqualTag.length();
		mt_nGTTagLen			= m_sSelectGTTag.length();
		mt_nLTTagLen			= m_sSelectLTTag.length();
		mt_nGETagLen			= m_sSelectGETag.length();
		mt_nLETagLen			= m_sSelectLETag.length();
		mt_nINTagLen			= m_sSelectINTag.length();
		mt_nEndTagLen			= m_sSelectEndTag.length();
		mt_nNotTagLen			= m_sSelectNotTag.length();
		mt_nAndTagLen			= m_sSelectAndTag.length();
		mt_nOrTagLen			= m_sSelectOrTag.length();
		mt_nLeftParenLen		= m_sLeftParen.length();
		mt_nRightParenLen		= m_sRightParen.length();
		//# mt_nNewLineLen          = 1;//NEWLINE.length();

		// internal buffer
		mt_chTemp	= new char[INTERNAL_BUF_SIZE];

	}


	public void	setVarWrapper(
			String			varprefix,
			String			varsuffix
	)
	{
		m_trVar.set(varprefix, varsuffix);
	}
	
	public TokenWrapper getVarWrapper()
	{
		return m_trVar;
	}

	public void	setMaxExpandLevel(
			int				maxexpandlevel
	)
	{
		m_nMaxExpandLevel =	maxexpandlevel;
	}

	public void	setDebug(boolean debug)
	{
		m_bDebug = debug;
	}
	public boolean getDebug()
	{
		return m_bDebug;
	}

	public void	log(String msg)
	{
		if (m_bDebug)  {
			System.out.println(msg);
		}
	}


	public Properties expandProperties(
			Properties			prop,
			EnvFileValidator	envFileValidator
	) throws Exception
	{
		return expandProperties(prop, envFileValidator, m_trVar.prefix,	m_trVar.suffix);
	}

	public Properties expandProperties(
			Properties		prop,
			EnvFileValidator	envFileValidator,
			String			varprefix,
			String			varsuffix
	) throws Exception
	{
		// local vars
		boolean		    bSamePS;
		//# ArrayList		alPair		= new ArrayList();
		Properties		propEx0		= new Properties();
		Properties		propEx1		= new Properties();
		Properties		propExA, propExB, propExTmp;
		String			sKey, sVal,	sVal1;
		Pair			aPair[];
		//Enumeration		eProp;
		int				i, num,	nLevel,	idxPrefix, idxSuffix;
		int				nPrefixLen,	nSuffixLen,	nSearchStart;
		Pair			pair;
		StringBuilder	sbTemp = new StringBuilder(SB_INIT_SIZE);
		StringBuilder	sbKeyStr = new StringBuilder(SB_INIT_SIZE);
		boolean		   	bReplaced, bNotInit,	bHasVar;
		Set				propEntrySet = null;
		Map.Entry		propEntry = null;
		Iterator		itProp = null;
		String			sExpandedValue = null;

		// check if	suffix is same as prefix
		bSamePS		= varprefix.equals(varsuffix);

		// Prepare the prefix/suffix length
		nPrefixLen	= varprefix.length();
		nSuffixLen	= varsuffix.length();

		// Prepare the array list for iteration
		num			= prop.size();
		aPair		= new Pair[num];
		//eProp		= prop.keys();
		propEntrySet = prop.entrySet();
		i			= 0;
		//while (eProp.hasMoreElements())	 {
		itProp = propEntrySet.iterator();
		while (itProp.hasNext()) {
			
			propEntry = (Map.Entry)itProp.next();
			
			sKey		= (String)propEntry.getKey();
			sbKeyStr.delete(0, sbKeyStr.length());
			sbKeyStr.append(varprefix).append(sKey).append(varsuffix);
			aPair[i]	= new Pair(sKey, (String)propEntry.getValue(), sbKeyStr.toString());
			i ++;
		}


		// Loop	through	alPair
		propExA		= prop;
		propExB		= propEx0;


		bHasVar		= true;
		bNotInit	= true;
		nLevel		= 0;
		
		EXPPROPSTATUS epStatus;
		while (bHasVar && nLevel < m_nMaxExpandLevel)  {

			bHasVar	= false;
			for	(i = 0;	i <	num; i ++)	{

				pair = aPair[i];

				// check if	it's expanded completely
				if (pair.good)	{
					propExB.setProperty(pair.key, pair.value);
					continue;
				}

				// check if	loop reference exists
				if (pair.hasLoopReference())  {
					throw new Exception("The properties	file contains loop references.");
				}


				// get value
				sVal = pair.value;
				

				
				// loop	to find	var	keys
				idxPrefix =	sVal.indexOf(varprefix);
				if (idxPrefix >= 0)	 {
					idxSuffix =	sVal.indexOf(varsuffix,	idxPrefix +	nPrefixLen);
				}
				else  {
					idxSuffix =	-1;
				}

				if (idxPrefix >= 0 && idxSuffix	>= 0)  {

					epStatus = _expandStringRec(sVal, propExA, varprefix, varsuffix, 0);
					sExpandedValue = epStatus.result;
					
					bReplaced = ! sExpandedValue.equals(sVal);
					sbTemp.delete(0, sbTemp.length());
					sbTemp.append(sExpandedValue);
					
					
					// extract the key between idxPrefix and idxSuffix
					/*
					sbTemp.delete(0, sbTemp.length());
					sbTemp.append(sVal);

					bReplaced =	false;

					do	{
						sKey = sbTemp.substring(idxPrefix +	nPrefixLen,	idxSuffix);

						// check if	the	key	exists
						sVal1		= propExA.getProperty(sKey,	null);
						if (sVal1 != null)	{
							// replace it
							sbTemp.replace(idxPrefix, idxSuffix	+ nSuffixLen, sVal1);
							nSearchStart = idxPrefix + sVal1.length();

							bReplaced =	true;

							if (bSamePS)  {
								idxPrefix =	sbTemp.indexOf(varprefix, nSearchStart);
							}
						}
						else  {
							nSearchStart = idxSuffix + nSuffixLen;

							if (bSamePS)  {
								idxPrefix =	idxSuffix;
							}
						}


						// search next
						if (!bSamePS)  {
							idxPrefix =	sbTemp.indexOf(varprefix, nSearchStart);
						}

						if (idxPrefix >= 0)	 {
							idxSuffix =	sbTemp.indexOf(varsuffix, idxPrefix	+ nPrefixLen);
						}


					} while	(idxPrefix >= 0	&& idxSuffix >=	0);
					
					*/
					if (bReplaced)	{
						// if there	is no prefix/suffix, set good
						idxPrefix =	sbTemp.indexOf(varprefix);
						if (idxPrefix >= 0)	 {
							idxSuffix =	sbTemp.indexOf(varsuffix, idxPrefix	+ nPrefixLen);
						}
						else  {
							idxSuffix =	-1;
						}


						if (idxPrefix == -1	|| idxSuffix ==	-1)	 {
							pair.good =	true;
						}
						else  {
							bHasVar	= true;
						}


						// add to propExB and pair
						sVal1 =	sbTemp.toString();
						propExB.setProperty(pair.key, sVal1);
						pair.value = sVal1;
					}
					else  {

						// pair	is good	anyway
						pair.good =	true;


						if (bNotInit)  {
							// add it
							propExB.setProperty(pair.key, pair.value);
						}
					}

				}
				else  {
					// set good
					pair.good =	true;
					if (bNotInit)  {
						// add it
						propExB.setProperty(pair.key, pair.value);
					}
				}
			}

			// set states
			if (nLevel >= 1)  {
				bNotInit = false;
			}

			// swap
			if (nLevel == 0)  {
				propExB		= propEx1;
				propExA		= propEx0;
			}
			else  {
				propExTmp	= propExA;
				propExA		= propExB;
				propExB		= propExTmp;
			}



			nLevel ++;

		}

		if (bHasVar)  {
			throw new Exception("Too many recursive	references.");
		}
		
		// validate it
		if (envFileValidator != null) {
			envFileValidator.validate(propExA);
		}

		return propExA;

	}

	/**
	 * Recursive expanding the properties
	 * @param sVal
	 * @param prop
	 * @return
	 */
	private EXPPROPSTATUS _expandStringRec(
		String 			sVal, 
		Properties 		prop, 
		String 			varprefix, 
		String 			varsuffix,
		int				idxStart
	)
	{
		EXPPROPSTATUS status = new EXPPROPSTATUS();
		EXPPROPSTATUS statusNext;
		
		int prefixLen = varprefix.length();
		int suffixLen = varsuffix.length();
		
		int idxPrefix, idxSuffix, idxNext = idxStart;
		
		String res;
		
		idxPrefix = sVal.indexOf(varprefix, idxNext);
		idxSuffix = sVal.indexOf(varsuffix, idxNext);
		StringBuilder sbRes = new StringBuilder();
		StringBuilder sbRes1 = null;
		
		// always ignore the first one
		while (idxPrefix >= 0 && (idxPrefix < idxSuffix || idxStart == 0)) {
		
			sbRes.append(sVal.substring(idxNext, idxPrefix));
			statusNext = _expandStringRec(sVal, prop, varprefix, varsuffix, idxPrefix + prefixLen);
			sbRes.append(statusNext.result);
			
			idxNext = statusNext.idxNext;
			if (idxNext < 0) {
				break;	// done
			}
			idxPrefix = sVal.indexOf(varprefix, idxNext);
			
			if (idxNext > idxSuffix) {
				idxSuffix = sVal.indexOf(varsuffix, idxNext);
			}
			
		}
		
		if (idxStart == 0) {
			if (idxNext >= 0) {
				sbRes.append(sVal.substring(idxNext));
			}
			status.result = sbRes.toString();
			status.idxNext = -1;	// done
		}
		else if (idxSuffix >= 0) {
			sbRes.append(sVal.substring(idxNext, idxSuffix));
			status.result = prop.getProperty(sbRes.toString());
			if (status.result == null) {
				status.result = new StringBuilder(varprefix).append(sbRes).append(varsuffix).toString();
			}
			status.idxNext = idxSuffix + 1;
			if (status.idxNext >= sVal.length()) {
				status.idxNext = -1;
			}
		}
		else {
			// append until the end
			if (idxNext >= 0) {
				sbRes.append(sVal.substring(idxNext));
			}
			sbRes1 = new StringBuilder(varprefix).append(sbRes);
			
			status.result = sbRes1.toString();
			status.idxNext = -1;
		}
		
		
		
		
		return status;
		
	}

	/**
	 * Search keys containing a	field
	 */
	public Properties searchKeysContainField(
			Properties		prop,
			String			field
	)
	{
		StringBuilder	sb = new StringBuilder(64);

		sb.append("[\\w\\.]*\\.").append(field).append("\\.[\\w\\.]*");

		return searchKeys(prop,	sb.toString());
	}


	/**
	 * Search keys containing a	string
	 */
	public Properties searchKeysContain(
			Properties		prop,
			String			str
	)
	{
		StringBuilder	sb = new StringBuilder(64);

		sb.append("[\\w\\.]*").append(str).append("[\\w\\.]*");

		return searchKeys(prop,	sb.toString());
	}

	/**
	 * Searches	keys based on the regex
	 */
	public Properties searchKeys(
			Properties		prop,
			String			regex
	)
	{
		String			sKey;
		Enumeration		eKeys	= prop.keys();
		Pattern			pattern		= Pattern.compile(regex);
		Matcher			matcher;
		Properties		propRet	= new Properties();

		while (eKeys.hasMoreElements())	 {
			sKey	= (String)eKeys.nextElement();

			matcher	= pattern.matcher(sKey);

			if (matcher.matches())	{
				propRet.setProperty(sKey, prop.getProperty(sKey));
			}

		}

		return propRet;

	}

	/**
	 * Selects distinct	key	and	return linked list
	 *
	 * @param	prop		the	properties object
	 *			regex		reg	ex
	 *			startfield	index of starting field	to be picked
	 *						for	distinct selection
	 *			endfield	endfield - 1 is	index of the ending	field
	 *						to be picked for distinct selection
	 *
	 * @return				Set	of strings composed	with
	 *						fields from	<startfield> to	<endfield> - 1
	 */
	public LinkedHashSet selectKeyDistinct(
			Properties		prop,
			String			regex,
			int				startfield,
			int				endfield		// excluded
	)
	{
		Enumeration		eKeys	= prop.keys();
		return selectKeyDistinct(eKeys,	regex, startfield, endfield);
	}

	public LinkedHashSet selectKeyDistinct(
			Enumeration		enumData,
			String			regex,
			int				startfield,
			int				endfield		// excluded
	)
	{
		LinkedHashSet	lhRet	= new LinkedHashSet();
		String			sKey, sKeyRet;
		Pattern			pattern		= Pattern.compile(regex);
		Matcher			matcher;

		while (enumData.hasMoreElements())	{

			sKey	= (String)enumData.nextElement();
			matcher	= pattern.matcher(sKey);

			if (matcher.matches())	{
				sKeyRet	= extractKeyFields(sKey, startfield, endfield);
				if (sKeyRet	!= null)  {
					lhRet.add(sKeyRet);
				}
			}
		}

		return lhRet;

	}

	public String extractKeyFields(
			String			key,
			int				startfield,
			int				endfield
	)
	{
		int	nStart,	nEnd;
		int	i, endfield1;
		int	nLen;

		// validate	param
		if (startfield >= endfield)	 {
			return null;
		}

		nLen = key.length();

		// find	start
		if (startfield == 0)  {
			nStart = 0;
			i =	0;
		}
		else  {
			nStart = 0;
			for	(i = 0;	i <	startfield;	i ++)  {
				nStart = key.indexOf(KEY_FIELD_SEP,	nStart);
				if (nStart == -1) {
					return null;
				}

				nStart ++;

				if (nStart == nLen)	 {
					return null;
				}
			}
		}

		// find	end
		nEnd		= nStart - 1;
		endfield1	= endfield - 1;
		for	(; i < endfield; i ++)	{
			nEnd = key.indexOf(KEY_FIELD_SEP, nEnd + 1);
			if (nEnd ==	-1)	{
				if (i <	endfield1)	{
					return null;
				}
				else  {
					nEnd = nLen;
					break;
				}
			}

			if (nStart == nLen)	 {
				return null;
			}
		}

		return key.substring(nStart, nEnd);

	}


	/**
	 * Gets	next key field
	 */
	public String getNextKeyField(
			String		key,
			int			start
	)
	{
		int	n1,	n2;

		n1		= key.indexOf(KEY_FIELD_SEP, start);
		if (n1 == -1)  {
			return null;
		}
		n1 ++;

		n2		= key.indexOf(KEY_FIELD_SEP, n1);
		if (n2 == -1)  {
			return key.substring(n1);
		}
		else  {
			return key.substring(n1, n2);
		}

	}

	/**
	 * maximum length of key
	 */
	public int maxKeyLength(
			Properties prop
	)
	{
		Enumeration	eKeys =	prop.keys();
		return maxStringLength(eKeys);
	}

	public int maxStringLength(
			Enumeration	enumData
	)
	{
		String	sVal;
		int		nLen, nMaxLen =	0;

		while (enumData.hasMoreElements())	{

			sVal	= (String)enumData.nextElement();
			nLen	= sVal.length();

			if (nLen > nMaxLen)	 {
				nMaxLen	= nLen;
			}

		}

		return nMaxLen;

	}

	/**
	 * Copies file
	 */
	public boolean copyFile(
			String			srcfile,
			String			dstfile
	)
	{

		byte[]				byteBuf;
		FileInputStream		fis	= null;
		FileOutputStream	fos	= null;
		int					nRead;
		boolean				bRet = true;

		try	 {

			byteBuf		= new byte[FILE_BUF_SIZE];
			fis			= new FileInputStream(srcfile);
			fos			= new FileOutputStream(dstfile);


			nRead	= fis.read(byteBuf,	0, FILE_BUF_SIZE);
			while (nRead !=	-1)	 {

				// append to string
				fos.write(byteBuf, 0, nRead);

				// read	next
				nRead	= fis.read(byteBuf,	0, FILE_BUF_SIZE);
			}

			byteBuf	= null;

		}
		catch (Exception e)	{

			e.printStackTrace(System.err);
			bRet = false;
		}

		try	{
			if (fis	!= null)  {
				fis.close();
			}
		}
		catch (Exception e)	 {
			e.printStackTrace(System.err);
			bRet = false;
		}

		try	{
			if (fos	!= null)  {
				fos.close();
			}
		}
		catch (Exception e)	 {
			e.printStackTrace(System.err);
			bRet = false;
		}
		return bRet;

	}


	/**
	 * read	text file contents to a	string buffer
	 * provided	by caller
	 */
	public StringBufferEx	readFile(
			StringBufferEx	sb,
			String			path
	) throws Exception
	{

		char[]			chBuf;
		BufferedReader	br = null;
		int				nRead;

		try	 {

			chBuf =	new	char[FILE_BUF_SIZE];
			br	= new BufferedReader(new FileReader(path));


			nRead	= br.read(chBuf, 0,	FILE_BUF_SIZE);
			sb.clear();
			while (nRead !=	-1)	 {

				// append to string
				sb.append(chBuf, 0,	nRead);

				// read	next
				nRead	= br.read(chBuf, 0,	FILE_BUF_SIZE);
			}

			br.close();
			chBuf =	null;

			return sb;
		}
		catch (Exception e)	{

			if (br != null)	 {
				br.close();
			}

			throw e;
		}
	}

	/**
	 * read	text file contents to a	string buffer
	 * provided	by caller
	 */
	public void	writeFile(
			StringBufferEx	sb,
			String			path
	) throws Exception
	{

		char[]			chBuf;
		BufferedWriter	bw = null;
		int				nLen, nToWrite,	nLeft, nRead;

		try	 {
			
			// check if the dst exists
			File fileOut = new File(path), fileWork;
			if (fileOut.exists()) {
				fileWork = fileOut;
			}
			else {
				// we need to create one
				fileWork = fileOut.getCanonicalFile();
				File fileParent = fileWork.getParentFile();
				if (fileParent != null && !fileParent.exists()) {
					fileParent.mkdirs();
				}
				if (!fileWork.createNewFile()) {
					throw new Exception("Unable to create file: " + fileWork.getPath());
				}
			}
			
			bw	= new BufferedWriter(new FileWriter(fileWork));
			nLen = sb.length();

			if (nLen < FILE_BUF_SIZE)  {
				chBuf =	new	char[nLen];
				nToWrite = nLen;
			}
			else {
				chBuf =	new	char[FILE_BUF_SIZE];
				nToWrite = FILE_BUF_SIZE;
			}

			sb.getChars(0, nToWrite, chBuf,	0);
			bw.write(chBuf,	0, nToWrite);

			nLeft	= nLen - nToWrite;
			nRead	= nToWrite;

			while (nLeft > 0)  {

				if (nLeft <	nToWrite)  {
					nToWrite = nLeft;
				}

				sb.getChars(nRead, nRead + nToWrite, chBuf,	0);
				bw.write(chBuf,	0, nToWrite);
				nRead += nToWrite;

				nLeft =	nLeft -	nToWrite;
			}


			bw.close();
			chBuf =	null;

		}
		catch (Exception e)	{

			if (bw != null)	 {
				bw.close();
			}

			throw e;
		}
	}


	/**
	 * Replaces	tokens
	 */
	public StringBufferEx	replaceTokens(
			StringBufferEx		sbsrc,
			StringBufferEx		sbdst,
			Properties			prop,
			String				tokenprefix,
			String				tokensuffix
	)
	{
		return replaceTokens(
					sbsrc,
					sbdst,
					prop,
					maxKeyLength(prop),
					tokenprefix,
					tokensuffix);
	}

	public StringBufferEx	replaceTokens(
			StringBufferEx		sbsrc,
			StringBufferEx		sbdst,
			Properties			prop,
			int					maxkeylen,
			String				tokenprefix,
			String				tokensuffix
	)
	{
		boolean			bSamePS;
		int				nPrefixLen,	nSuffixLen;
		int				idxPrefix, idxSuffix;
		//char[]			chTemp;
		String			sKey, sVal;
		int				nPos;//, nTemp;

		// check if	suffix is same as prefix
		bSamePS			= tokenprefix.equals(tokensuffix);

		// get length of prefix/suffix
		nPrefixLen		= tokenprefix.length();
		nSuffixLen		= tokensuffix.length();

		// find	prefix
		idxPrefix		= sbsrc.indexOf(tokenprefix);
		if (idxPrefix >= 0)	 {
			idxSuffix	= sbsrc.indexOf(tokensuffix, idxPrefix + nPrefixLen);
		}
		else  {
			idxSuffix	= -1;
		}

		if (idxPrefix >= 0 && idxSuffix	>= 0)  {

			//chTemp			= new char[sbsrc.length()];
			nPos			= 0;

			do	{
				// check if	the	token is too long
				if (idxSuffix -	idxPrefix -	nPrefixLen <= maxkeylen)  {

					// find	the	key
					sKey	= sbsrc.substring(idxPrefix	+ nPrefixLen, idxSuffix);
					sVal	= prop.getProperty(sKey);

					if (sVal !=	null)  {
						sbdst.append(sbsrc, nPos, idxPrefix, mt_chTemp);
						//sbsrc.getChars(nPos, idxPrefix,	chTemp,	0);
						//sbdst.append(chTemp, 0,	idxPrefix -	nPos);
						sbdst.append(sVal);

						nPos			= idxSuffix	+ nSuffixLen;

						// search next
						idxPrefix		= sbsrc.indexOf(tokenprefix, nPos);
						if (idxPrefix >= 0)	 {
							idxSuffix	= sbsrc.indexOf(tokensuffix, idxPrefix + nPrefixLen);
						}
						else  {
							idxSuffix	= -1;
						}

					}
					else {

						if (bSamePS)  {
							idxPrefix	= idxSuffix;
							idxSuffix	= sbsrc.indexOf(tokensuffix, idxPrefix + nPrefixLen);
						}
						else  {
							idxPrefix	= sbsrc.indexOf(tokenprefix, idxSuffix + nSuffixLen);
							if (idxPrefix >= 0)	 {
								idxSuffix	= sbsrc.indexOf(tokensuffix, idxPrefix + nPrefixLen);
							}
							else  {
								idxSuffix	= -1;
							}
						}
					}

				}
				else  {

					if (bSamePS)  {
						idxPrefix	= idxSuffix;
						idxSuffix	= sbsrc.indexOf(tokensuffix, idxPrefix + nPrefixLen);
					}
					else  {
						idxPrefix	= sbsrc.indexOf(tokenprefix, idxSuffix + nSuffixLen);
						if (idxPrefix >= 0)	 {
							idxSuffix	= sbsrc.indexOf(tokensuffix, idxPrefix + nPrefixLen);
						}
						else  {
							idxSuffix	= -1;
						}
					}
				}


			} while	(idxPrefix >= 0	&& idxSuffix >=	0);

			// fill	sbdst
			if (nPos ==	0)	{
				// no keys
				// don't do	copy (SUN's	stringbuffer is	a stupid crap!)
				sbdst.append(sbsrc);
			}
			else  {
				sbdst.append(sbsrc, nPos, sbsrc.length(), mt_chTemp);
				//sbsrc.getChars(nPos, sbsrc.length(), chTemp, 0);
				//sbdst.append(chTemp, 0,	sbsrc.length() - nPos);
			}

		}
		else  {
			sbdst.append(sbsrc);
		}

		return sbdst;

	}


	/**
	 * Gets	absolute path
	 *
	 * @param	path		file / directory path
	 *
	 * @return				absolute path
	 */
	public File	getAbsolutePath(
			File				path
	)
	{
		try	 {
			return path.getCanonicalFile();
		}
		catch (Exception e)	 {
			e.printStackTrace(System.err);
			return null;
		}

	}


	/**
	 * Deletes a directory
	 *
	 * @param	dir			path of	the	directory
	 *
	 * @return				true if	succeeded, or false
	 */
	public boolean deleteDir(
			String				dir
	)
	{
		return deleteDir(new File(dir));
	}

	/**
	 * Deletes a directory
	 *
	 * @param	dir			path of	the	directory
	 *
	 * @return				true if	succeeded, or false
	 */
	public boolean deleteDir(
			File				dir
	)
	{
		boolean		bRet = true;
		DeleteDirSearchFileCallback		cb = null;

		try	 {

			cb = new DeleteDirSearchFileCallback();

			searchFiles(dir, null, cb);
		}
		catch (Exception e)	 {
			bRet	= false;
			e.printStackTrace(System.err);
		}

		// delete cur dir
		dir.delete();

		return bRet	&& cb != null && cb.succeeded();
	}

	/**
	 * Copies directory	recursively
	 *
	 * @param	srcdir			source directory
	 *			dstdir			destination	directory
	 *
	 * @return					true if	succeeded, or false
	 */
	public boolean copyDir(
			String				srcdir,
			String				dstdir
	)
	{
		return copyDir(new File(srcdir), new File(dstdir));
	}


	/**
	 * Copy	directory recursively
	 *
	 * @param	srcdir			source directory
	 *			dstdir			destination	directory
	 *
	 * @return					true if	succeeded, or false
	 */
	public boolean copyDir(
			File				srcdir,
			String				dstdir
	)
	{
		return copyDir(srcdir, new File(dstdir));
	}

	/**
	 * Copy	directory recursively
	 *
	 * @param	srcdir			source directory
	 *			dstdir			destination	directory
	 *
	 * @return					true if	succeeded, or false
	 */
	public boolean copyDir(
			String				srcdir,
			File				dstdir
	)
	{
		return copyDir(new File(srcdir), dstdir);
	}

	/**
	 * Copy	directory recursively
	 *
	 * @param	srcdir			source directory
	 *			dstdir			destination	directory
	 *
	 * @return					true if	succeeded, or false
	 */
	public boolean copyDir(
			File				srcdir,
			File				dstdir
	)
	{
		boolean		bRet = true;
		CopyDirSearchFileCallback	cb = null;

		try	 {
			// first, get absolute path
			// not using File.getAbsolutePath as on	UNIX it
			// will	use	current	user's home	directory
			File	fAbsSrcDir		= getAbsolutePath(srcdir);
			File	fAbsDstDir		= getAbsolutePath(dstdir);

			// create the root dir
			//File	fDstRootDir		= new File(fAbsDstDir.getPath()	+ File.separator + fAbsSrcDir.getName());
			//fDstRootDir.mkdir();

			// for callback
			cb = new CopyDirSearchFileCallback(this, fAbsSrcDir, fAbsDstDir);


			// call	search files
			// filefileter is null for all files
			searchFiles(fAbsSrcDir,	null, cb);

		}
		catch (Exception e)	 {

			bRet	= false;
			e.printStackTrace(System.err);
		}


		return bRet	&& cb != null && cb.succeeded();


	}

	/**
	 * Search files	in a given root	directory recursively
	 *
	 * @param	srcfile			source file
	 *			dstfile			destination	file
	 *			prop			properties
	 *			tokenprefix		prefix of token
	 *			tokensuffix		suffix of token
	 *
	 * @return					true if	succeeded, otherwise, false
	 */
	public void	setTokenWrapper(
			String				tokenprefix,
			String				tokensuffix
	)
	{
		m_trToken.set(tokenprefix, tokensuffix);
	}
	
	public TokenWrapper getTokenWrapper()
	{
		return m_trToken;
	}

	public void	setProperties(
			Properties			prop
	)
	{
		m_prop = prop;
		m_nMaxKeyLen = maxKeyLength(prop);
	}

	public boolean replaceFileTokens(
			String				srcfile,
			String				dstfile,
			TokenConfigValidator	tokenConfigValidator
	) throws ConfigException
	{
		return replaceFileTokens(
					srcfile,
					dstfile,
					m_trToken.prefix,
					m_trToken.suffix,
					tokenConfigValidator
				);
	}

	public boolean replaceFileTokens(
			String				srcfile,
			String				dstfile,
			String				tokenprefix,
			String				tokensuffix,
			TokenConfigValidator	tokenConfigValidator
	) throws ConfigException
	{
		return replaceFileTokens(
					srcfile,
					dstfile,
					m_prop,
					m_nMaxKeyLen,
					tokenprefix,
					tokensuffix,
					tokenConfigValidator
				);
	}

	public boolean replaceFileTokens(
			String				srcfile,
			String				dstfile,
			Properties			prop,
			String				tokenprefix,
			String				tokensuffix,
			TokenConfigValidator	tokenConfigValidator
	) throws ConfigException
	{
		return replaceFileTokens(
					srcfile,
					dstfile,
					prop,
					maxKeyLength(prop),
					tokenprefix,
					tokensuffix,
					tokenConfigValidator
				);
	}

	public boolean replaceFileTokens(
			String				srcfile,
			String				dstfile,
			Properties			prop,
			int				   maxkeylen,
			String				tokenprefix,
			String				tokensuffix,
			TokenConfigValidator	tokenConfigValidator
	) throws ConfigException
	{
		try {
			StringBufferEx sbRes = transformFileTokens(srcfile, prop, maxkeylen, tokenprefix, tokensuffix, tokenConfigValidator);
			writeFile(sbRes, dstfile);
			return true;
		}
		catch (Exception e) {
			throw new ConfigException("Token replacement failed on src [" + srcfile +
					"] - dst[" + dstfile + "] -- " + e.getMessage(), e);
		}
	}
	
	
	public StringBufferEx transformFileTokens(
			String				srcfile,
			Properties			prop,
			int				   maxkeylen,
			String				tokenprefix,
			String				tokensuffix,
			TokenConfigValidator	tokenConfigValidator
	) throws ConfigException
	{
		try	 {
			StringBufferEx sbsrc		= new StringBufferEx(SB_INIT_SIZE);
			StringBufferEx sbdst		= new StringBufferEx(SB_INIT_SIZE);

			readFile(sbsrc,	srcfile);

			// process select blocks
			initProcessSelect(prop,	maxkeylen, tokenprefix, tokensuffix);
			processSelect(
					sbsrc,		// src
					sbdst,		// dst (reuse)
					prop,
					maxkeylen
			);

			// @TEST
			//writeFile(sbdst, "D:\\Harry\\firm\\ConfigTool\\testout.txt");


			// reuse sbsrc
			sbsrc.clear();
			replaceTokens(
					sbdst,
					sbsrc,
					prop,
					maxkeylen,
					tokenprefix,
					tokensuffix
			);
			
			if (tokenConfigValidator != null) {
				tokenConfigValidator.validate(sbsrc.getStringBuffer());
			}

			return sbsrc;

			//writeFile(sbsrc, dstfile);
			//return true;
		}
		catch (ConfigException ce) {
			throw ce;
		}
		catch (Exception e)	{
			//System.out.println("Error occured in [src file: " + srcfile + "] [dst file: " + dstfile + "]");
			//e.printStackTrace(System.err);
			throw new ConfigException(e);
		}




	}

	private int getLineNum(
			StringBufferEx	sbsrc,
			int				atindex
	)
	{
		// try to find the line number at index.
		// Basically search number of '\n'
		int idx = 0;
		int linenum = 0;

		do  {
			idx = sbsrc.indexOf(NEWLINE, idx);
			linenum ++;
			idx ++;

		} while (idx >= 0 && idx < atindex);

		return linenum;
	}


	private	void initProcessSelect(
			Properties		prop,
			int				maxkeylen,
			String			tokenprefix,
			String			tokensuffix
	)
	{
		mt_prop					= prop;
		mt_nMaxKeyLen			= maxkeylen;

		mt_sTokenPrefix			= tokenprefix;
		mt_sTokenSuffix			= tokensuffix;
		mt_nTokenPrefixLen		= tokenprefix.length();
		mt_nTokenSuffixLen		= tokensuffix.length();
	}


	public void	processSelect(
			StringBufferEx		sbsrc,		// src
			StringBufferEx		sbdst,		// dst (reuse)
			Properties			prop,
			int					maxkeylen
	) throws Exception
	{


		int idxEnd = sbsrc.length();
		int idxNext;

		// call	recursively
		idxNext = processSelectBlock(
				sbsrc,
				0,
				idxEnd ,
				sbdst);

		// append the left
		sbdst.append(sbsrc, idxNext, idxEnd, mt_chTemp);
	}

	private	int processSelectBlock(
			StringBufferEx		sbsrc,
			int					start,
			int					end,		// not included
			StringBufferEx		sbdst
	) throws Exception
	{
		int	idxBeginTag0, idxEndTag;
		int	idxNext, idx1, idx2;
		int	idxSearchStart = start;
		ExprToken retval = new ExprToken();

		do	{
			// search the destination
			idxBeginTag0		= sbsrc.indexOf(m_sSelectBeginTag0,	idxSearchStart);

			// if not found, quit
			if (idxBeginTag0 < 0 || idxBeginTag0 + mt_nBeginTag0Len >= end)  {
				// set the return
				idxNext = idxSearchStart;
				break;
			}


			// check if an endtag is before the begintag0
			idxEndTag = sbsrc.indexOf(m_sSelectEndTag,	idxSearchStart);
			if (idxEndTag >= 0 && idxEndTag + mt_nEndTagLen < end)  {
				if (idxBeginTag0 > idxEndTag) {
					// indicates end of the block

					//if (idxEndTag + mt_nEndTagLen <= end)  {
					// this is an syntax error
					//	throw new Exception("Unexpected endtag found at line: " + getLineNum(sbsrc, idxEndTag));
					//}

					idxNext = idxSearchStart;
					break;
				}
			}
			else  {
				// missing endtag, throw
				throw new Exception("Missing end tag for begin tag0 at line: " + getLineNum(sbsrc, idxBeginTag0 + 1));
			}




			// go to next char
			idxNext				= idxBeginTag0 + mt_nBeginTag0Len;

			// check if	the	next char is space
			if (!Character.isWhitespace(sbsrc.charAt(idxNext)))  {
				// a wront syntax, throw exception
				throw new Exception("No	white space	after begin	tag0 at line: " + getLineNum(sbsrc, idxNext));
			}


			// good, go	to the next	char
			idxNext	++;
			if (idxNext	>= end)	 {
				throw new Exception("Invalid begin tag0 at line: " + getLineNum(sbsrc, idxNext));
			}

			// Now try to find the begin tag1 so we	can	get	a parsing string
			// The begin tag1 may be escaped in	value string by	"\"
			// so we also need to verify

			// this	is the expression start	point
			// find	the	begin tag1
			idxNext = processSelectExpression(sbsrc, idxNext, end, retval);

			// skip tailing whitespace
			idxNext = skipTailingWhitespace(sbsrc, idxNext, end);

			//idxBeginTag1 = sbsrc.indexOf(m_sSelectBeginTag1, idxNext);

			// if accepted,	process the internal block
			// append beforehand
			// Check if there is a newline before begin tag0,
			// If yes, then remove it before appending
			idx1 = testNewLineBackward(sbsrc, idxSearchStart, idxBeginTag0);
			if (idx1 < 0)  {
				// not found
				idx1 = idxBeginTag0;
			}

			// However, idxSearchStart may have already done white skipping
			// so check it again
			if (idx1 > idxSearchStart)  {
				sbdst.append(sbsrc, idxSearchStart, idx1, mt_chTemp);
			}
			//sbsrc.getChars(idxSearchStart, idxBeginTag0, mt_chTemp,	0);
			//sbdst.append(mt_chTemp,	0, idxBeginTag0	- idxSearchStart);

			if (retval.boolvalue)  {

				// call	recursively
				idxNext = processSelectBlock(
						sbsrc,
						idxNext,
						end,
						sbdst);

				idxEndTag =	sbsrc.indexOf(m_sSelectEndTag, idxNext);
				if (idxEndTag <	0 || idxEndTag + mt_nEndTagLen >= end)	{
					throw new Exception("Cannot	find end tag for begin tag at line: " + getLineNum(sbsrc, idxBeginTag0 + 1));
				}

				// append
				idx1 = testNewLineBackward(sbsrc, idxSearchStart, idxEndTag);
				if (idx1 < 0 || skipTailingWhitespace(sbsrc, idxEndTag + mt_nEndTagLen, end) == idxEndTag + mt_nEndTagLen)  {
					idx1 = idxEndTag;
				}
				if (idx1 > idxNext)  {
					sbdst.append(sbsrc, idxNext, idx1, mt_chTemp);
				}

				/*
				idx1 = idxEndTag - mt_nNewLineLen;
				if (idx1 >= start)  {
					// 1. check if it's a NEWLINE
					// 2. search until NEWLINE to see if there is non-white chars exist
					// if yes, then do not skip the NEWLINE
					if ((!sbsrc.substring(idx1, idxEndTag).equals(NEWLINE)) ||
						skipTailingWhitespace(sbsrc, idxEndTag + mt_nEndTagLen, end) == idxEndTag + mt_nEndTagLen)
					{
						idx1 = idxEndTag;
					}
				}
				if (idx1 > idxNext)  {
					sbdst.append(sbsrc, idxNext, idx1, mt_chTemp);
				}
			*/
				//sbsrc.getChars(idxNext, idxEndTag, mt_chTemp, 0);
				//sbdst.append(mt_chTemp,	0, idxEndTag - idxNext);
			}
			else  {

				// search then matched endtag
				// to avoid embedded select blocks, we need
				// to find pairs of blocks
				idxEndTag = psFindMatchedEndTag(sbsrc, idxNext, end);
				if (idxEndTag <	0 || idxEndTag + mt_nEndTagLen >= end)	{
					throw new Exception("Cannot	find end tag for begin tag at line: " + getLineNum(sbsrc, idxBeginTag0));
				}
			}

			// set next	search start
			idxSearchStart = idxEndTag + mt_nEndTagLen;

			// skip tailing whitespace as needed, but not the newline
			idx1 = skipTailingWhitespace(sbsrc, idxSearchStart, end);
			if (idx1 > idxSearchStart) {
				// meaning skipped, now move back a little
				idx2 = testNewLineBackward(sbsrc, idxSearchStart, idx1);
				if (idx2 > 0)
					idxSearchStart = idx2;
				//idxSearchStart = idx1 - mt_nNewLineLen;
			}
			else  {
				idxSearchStart = idx1;
			}


		} while	(true);


		// append the left
		//sbsrc.getChars(idxSearchStart, end,	mt_chTemp, 0);
		//sbdst.append(mt_chTemp,	0, end - idxSearchStart);
		return idxNext;
	}


	private int psFindMatchedEndTag(
			StringBufferEx		sbsrc,
			int					start,
			int					end
	) throws Exception
	{
		int idxBeginTag0, idxEndTag;
		int idxNext = start;
		do {

			idxBeginTag0 = sbsrc.indexOf(m_sSelectBeginTag0, idxNext);
			idxEndTag =	sbsrc.indexOf(m_sSelectEndTag, idxNext);

			if (idxBeginTag0 < 0 || idxBeginTag0 + mt_nBeginTag0Len >= end)  {
				if (idxEndTag < 0 || idxEndTag + mt_nEndTagLen >= end) {
					throw new Exception("Cannot find end tag for begin tag0 at line: " + getLineNum(sbsrc, start));
				}
				else  {
					return idxEndTag;
				}
			}
			else {
				if (idxEndTag < 0 || idxEndTag + mt_nEndTagLen >= end) {
					throw new Exception("Cannot find end tag for begin tag0 at line: " + getLineNum(sbsrc, start));
				}
				else if (idxEndTag < idxBeginTag0) {
					return idxEndTag;
				}
			}

			idxNext = idxEndTag + mt_nEndTagLen;

		} while (true);

	}



	/**
	 * Finds the begin tag1	and	get	the	expression
	 */
	private	int	processSelectExpression(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		// This	is part	of the expression parsing

		// just	call the root expression
		// If an expression	is thrown, an syntax error
		// occurs or end has been passed
		int	idxEnd, idx;
		idxEnd = psGetExpression(sbsrc,	start, end,	retval);

		if (retval.type == EXPRVALTYPE_NONE)  {
			// not found any expression, wrong
			throw new Exception("Got empty expression before begin tag1 at line: " + getLineNum(sbsrc, start));

		}
		else  {
			switch (retval.type) {
				case EXPRVALTYPE_BOOL:
				case EXPRVALTYPE_KEY:
					// nothing
					break;

				default:
					throw new Exception("Got wrong expression before begin tag1 at line: " + getLineNum(sbsrc, idxEnd));
			}

		}

		// now find the begin tag1, must start at a non-whitespace char
		idx = psGetNextNonWhiteChar(sbsrc, idxEnd, end);
		if (idx < 0 || idx + mt_nBeginTag1Len > end)  {
			throw new Exception("Cannot find begin tag1 at line: " + getLineNum(sbsrc, idxEnd));
		}

		if (!sbsrc.substring(idx, idx + mt_nBeginTag1Len).equals(m_sSelectBeginTag1))  {
			throw new Exception("Cannot find begin tag1 at line: " + getLineNum(sbsrc, idxEnd));
		}

		// to avoid inserting a newline after begin tag1,
		idx += mt_nBeginTag1Len;
		idx = skipTailingWhitespace(sbsrc, idx, end);

		return idx;
	}

	private int testNewLineForward(
		   StringBufferEx        sbsrc,
		   int                   start,
		   int                   end
	)
	{

		int idx1;

		idx1 = start + NEWLINE0.length();
		if (idx1 <= end)  {
			if (NEWLINE0.equals(sbsrc.substring(start, idx1)))  {
				return idx1;
			}
		}

		if (sbsrc.charAt(start) == NEWLINE1) {
			return start + 1;
		}

		if (sbsrc.charAt(start) == NEWLINE2) {
			return start + 1;
		}


		return -1;
	}

	private int testNewLineBackward(
		   StringBufferEx        sbsrc,
		   int                   start,
		   int                   end
	)
	{
		// search from end-1

		int idx1;

		idx1 = end - NEWLINE0.length();
		if (idx1 >= start)  {
			if (NEWLINE0.equals(sbsrc.substring(idx1, end)))  {
				return idx1;
			}
		}

		idx1 = end - 1;
		if (sbsrc.charAt(idx1) == NEWLINE1) {
			return idx1;
		}

		if (sbsrc.charAt(idx1) == NEWLINE2) {
			return idx1;
		}


		return -1;
	}

	private int skipTailingWhitespace(
		   StringBufferEx		sbsrc,
		   int					start,
		   int					end
	)
	{
		int idx, idx1;
		char ch;
//		int nNewlineLen = mt_nNewLineLen;

		for (idx = start; idx < end; idx ++) {
			ch = sbsrc.charAt(idx);
			if (WHITE_CHARSET.indexOf(ch) < 0) {
				// there is a non-whitespace char, return original start
				// and keep the space
				idx = start;
				break;
			}

			idx1 = testNewLineForward(sbsrc, idx, end);
			if (idx1 > 0)  {
				idx = idx1;
				break;
			}
		}

		return idx;
	}

	private int psGetNextNonWhiteChar(
			StringBufferEx		sbsrc,
			int					start,
			int					end
	)
	{
		int idx;

		for (idx = start; idx < end; idx ++)  {
			if (!Character.isWhitespace(sbsrc.charAt(idx))) {
				break;
			}
		}

		if (idx < end) {
			return idx;
		}
		else  {
			return -1;
		}


	}


	private	int	psGetExpression(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{

		// find	normal expression or unary expression

		// 1. try to find unary
		int idxEnd = psGetNormalExpression(sbsrc, start, end, retval);
		if (retval.type == EXPRVALTYPE_NONE)	{
			// 2. try to get a normal expression
			idxEnd = psGetUnaryExpression(sbsrc, start, end, retval);
		}

		return idxEnd;
	}


	private	int	psGetUnaryExpression(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		int idxEnd, idxEndPrev;

		idxEnd = psGetNotOp(sbsrc, start, end, retval);

		if (retval.type == EXPRVALTYPE_NOT)	{
			// so it's a unary expr

			// two options
			// A. term
			idxEndPrev = idxEnd;
			idxEnd = psGetTerm(sbsrc, idxEndPrev, end, retval);
			if (retval.type == EXPRVALTYPE_NONE) {
				// B unary
				idxEnd = psGetUnaryExpression(sbsrc, idxEndPrev, end, retval);
				if (retval.type == EXPRVALTYPE_NONE)  {
					// failed
					throw new Exception("Falied getting unary expression at line: " + getLineNum(sbsrc, idxEndPrev));
				}
				// set value and return
				retval.type = EXPRVALTYPE_BOOL;
				retval.boolvalue = ! retval.boolvalue;
				retval.strvalue = null;
				return idxEnd;

			}

			// it's a term, make sure it's a bool or key
			switch (retval.type)  {
				case EXPRVALTYPE_BOOL:
				case EXPRVALTYPE_KEY:
					retval.type = EXPRVALTYPE_BOOL;
					retval.boolvalue = ! retval.boolvalue;
					retval.strvalue = null;
					return idxEnd;

				default:
					throw new Exception("Not a bool or key on unary operation at line: " + getLineNum(sbsrc, idxEndPrev));
			}

		}

		// set none
		retval.type = EXPRVALTYPE_NONE;
		return -1;
	}

	private	int	psGetNormalExpression(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		ExprToken token;
		ExprToken op;
		int idxEnd, idxEndPrev;

		// get a term
		idxEnd = psGetTerm(sbsrc, start, end, retval);
		if (retval.type == EXPRVALTYPE_NONE)	 {
			// not found, this is an empty expression, throw an	error
			//throw new Exception("GetTerm for normal	expression failed.");
			return -1;
		}

		// try to get an	add-op for normal expression
		token = new ExprToken();
		op = new ExprToken();
		idxEndPrev = idxEnd;
		idxEnd = psGetAddOp(sbsrc, idxEndPrev, end,	op);
		while (op.type != EXPRVALTYPE_NONE)	 {

			// get next	term
			idxEndPrev = idxEnd;
			idxEnd = psGetTerm(sbsrc, idxEndPrev, end, token);

			// if no term, throw
			if (token.type == EXPRVALTYPE_NONE)	 {
				throw new Exception("Missing next term at line: " + getLineNum(sbsrc, idxEndPrev));
			}

			// calculate, result stored	in retval
			try  {
				psAddOp(op,	retval, token);
			}
			catch (Exception e) {
				throw new Exception(e.getMessage() + " (line: " + getLineNum(sbsrc, idxEndPrev) + ")");
			}


			// get add-op
			idxEndPrev = idxEnd;
			idxEnd = psGetAddOp(sbsrc, idxEnd, end,	op);
		}

		return idxEndPrev;

	}

	private	void psAddOp(
			ExprToken			op,
			ExprToken			term0,
			ExprToken			term1
	) throws Exception
	{
		// check the term1 -> operation
		switch (op.type) {

			case EXPRVALTYPE_OR:
			case EXPRVALTYPE_AND:

				// check term1's type
				switch (term1.type)	 {
					case EXPRVALTYPE_KEY:
					case EXPRVALTYPE_BOOL:

						// check if	term0 is key or	bool
						if (term0.type != EXPRVALTYPE_KEY &&
							term0.type != EXPRVALTYPE_BOOL)
						{
							// if this is Key, the term0 must be key or	bool too
							throw new Exception("The next term is key but the previous term	is not");
						}

						// AddOp
						term0.boolvalue	= psBoolOp(op, term0, term1);
						term0.type = EXPRVALTYPE_BOOL;
						term0.strvalue = null;	// it's	bool now, no need string value
						break;

					default:
						// wrong term1 type
						throw new Exception("Wrong term1 type.");
				}
				break;

			default:
				throw new Exception("Invalid OP	type: "	+ op.type);
		}
	}

	private	boolean	psBoolOp(
			ExprToken		op,
			ExprToken		term0,
			ExprToken		term1
	) throws Exception
	{
		// calculate
		switch (op.type)  {
			case EXPRVALTYPE_OR:
				return (term0.boolvalue	|| term1.boolvalue);
			case EXPRVALTYPE_AND:
				return (term0.boolvalue	&& term1.boolvalue);
			default:
				throw new Exception("Invalid AddOp.");
		}
	}

	private	int	psGetNotOp(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		// just	get	the	first non-whitespace char
		int	idx	= psGetNextNonWhiteChar(sbsrc, start, end);

		if (idx	>= start &&	idx	+ mt_nNotTagLen	<= end)	{
			if (sbsrc.substring(idx, idx + mt_nNotTagLen).equals(m_sSelectNotTag))	{
				retval.type	= EXPRVALTYPE_NOT;
				return idx + mt_nNotTagLen;
			}
		}

		retval.type	= EXPRVALTYPE_NONE;
		return -1;
	}

	private int psGetMulOp(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		// just	get	the	first non-whitespace char
		int	idx	= psGetNextNonWhiteChar(sbsrc, start, end);

		if (idx	>= start)  {

			if (idx	+ mt_nEqualTagLen <= end)	{
				if (sbsrc.substring(idx, idx + mt_nEqualTagLen).equals(m_sSelectEqualTag))	{
					retval.type	= EXPRVALTYPE_EQUAL;
					return idx + mt_nEqualTagLen;
				}
			}

			if (idx + mt_nNotEqualTagLen <= end)  {
				if (sbsrc.substring(idx, idx + mt_nNotEqualTagLen).equals(m_sSelectNotEqualTag))	{
					retval.type	= EXPRVALTYPE_NOTEQUAL;
					return idx + mt_nNotEqualTagLen;
				}
			}

			if (idx + mt_nGTTagLen <= end)  {
				if (sbsrc.substring(idx, idx + mt_nGTTagLen).equals(m_sSelectGTTag))	{
					retval.type	= EXPRVALTYPE_GT;
					return idx + mt_nGTTagLen;
				}
			}

			if (idx + mt_nLTTagLen <= end)  {
				if (sbsrc.substring(idx, idx + mt_nLTTagLen).equals(m_sSelectLTTag))	{
					retval.type	= EXPRVALTYPE_LT;
					return idx + mt_nLTTagLen;
				}
			}

			if (idx + mt_nGETagLen <= end)  {
				if (sbsrc.substring(idx, idx + mt_nGETagLen).equals(m_sSelectGETag))	{
					retval.type	= EXPRVALTYPE_GE;
					return idx + mt_nGETagLen;
				}
			}

			if (idx + mt_nLETagLen <= end)  {
				if (sbsrc.substring(idx, idx + mt_nLETagLen).equals(m_sSelectLETag))	{
					retval.type	= EXPRVALTYPE_LE;
					return idx + mt_nLETagLen;
				}
			}

			if (idx + mt_nINTagLen <= end)  {
				if (sbsrc.substring(idx, idx + mt_nINTagLen).equals(m_sSelectINTag))	{
					retval.type	= EXPRVALTYPE_IN;
					return idx + mt_nINTagLen;
				}
			}
		}
		retval.type	= EXPRVALTYPE_NONE;
		return -1;

	}


	private	int	psGetAddOp(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		// just	get	the	first non-whitespace char
		int	idx	= psGetNextNonWhiteChar(sbsrc, start, end);

		if (idx	>= start)  {

			if (idx	+ mt_nAndTagLen	<= end)	{
				if (sbsrc.substring(idx, idx + mt_nAndTagLen).equals(m_sSelectAndTag))	{
					retval.type	= EXPRVALTYPE_AND;
					return idx + mt_nAndTagLen;
				}
			}

			if (idx + mt_nOrTagLen <= end)  {
				if (sbsrc.substring(idx, idx + mt_nOrTagLen).equals(m_sSelectOrTag))	{
					retval.type	= EXPRVALTYPE_OR;
					return idx + mt_nOrTagLen;
				}
			}

		}

		retval.type	= EXPRVALTYPE_NONE;
		return -1;
	}

	private	int	psGetLeftParen(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		int	idx	= psGetNextNonWhiteChar(sbsrc, start, end);

		if (idx	>= start &&	idx	+ mt_nLeftParenLen <= end)	{
			if (sbsrc.substring(idx, idx + mt_nLeftParenLen).equals(m_sLeftParen))	{
				retval.type	= EXPRVALTYPE_LEFTPAREN;
				return idx + mt_nLeftParenLen;
			}
		}

		retval.type = EXPRVALTYPE_NONE;
		return -1;
	}

	private	int	psGetRightParen(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		int	idx	= psGetNextNonWhiteChar(sbsrc, start, end);

		if (idx	>= start &&	idx	+ mt_nRightParenLen <= end)	{
			if (sbsrc.substring(idx, idx + mt_nRightParenLen).equals(m_sRightParen))	{
				retval.type	= EXPRVALTYPE_RIGHTPAREN;
				return idx + mt_nRightParenLen;
			}
		}

		retval.type = EXPRVALTYPE_NONE;
		return -1;
	}

	private	int	psGetFactor(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		int idxEnd;

		// get key
		idxEnd = psGetKey(sbsrc, start, end, retval);
		if (retval.type == EXPRVALTYPE_NONE)  {
			// try get value
			idxEnd = psGetValue(sbsrc, start, end, retval);
			if (retval.type == EXPRVALTYPE_NONE) {

				// try number
				idxEnd = psGetNumber(sbsrc, start, end, retval);

			}

		}

		// return it
		return idxEnd;
	}


	private	int	psGetKey(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		// now start low-level parsing
		int idxStart, idx;
		char ch;

		// skip whitespaces
		idxStart	= psGetNextNonWhiteChar(sbsrc, start, end);
		if (idxStart < 0)  {
			retval.type = EXPRVALTYPE_NONE;
			return -1;
		}

		// start iterating
		// the first must not be +,-,.,digit, '"', '}'
		ch = sbsrc.charAt(idxStart);
		if (Character.isDigit(ch) || ch == '.' || ch == '+' || ch == '-' || ch == '\"'
			|| m_sSelectBeginTag1.charAt(0) == ch)  {
			// not a key name
			retval.type = EXPRVALTYPE_NONE;
			return -1;
		}

		for (idx = idxStart + 1; idx < end; idx ++)  {
			ch = sbsrc.charAt(idx);

			//check if it's a valid key
			if (Character.isLetterOrDigit(ch) ||
				ch == '.' ||
				ch == '_' ||
				ch == '+' ||
				ch == '-')
			{
				continue;
			}
			else {
				// stop here
				break;
			}


		}

		if (idx >= end)  {
			// error occured
			retval.type = EXPRVALTYPE_NONE;
			return -1;
		}
		else  {

			// search property
			String sKey = sbsrc.substring(idxStart, idx);
			String sValue = mt_prop.getProperty(sKey);

			retval.type = EXPRVALTYPE_KEY;
			retval.strvalue = sValue;
			retval.boolvalue = (sValue != null);
			return idx;
		}

	}

	private	int	psGetValue(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		// now start low-level parsing
		int idxStart, idx, idx1;
		char ch, ch1;
		String sKey, sValue;

		// skip whitespaces
		idxStart	= psGetNextNonWhiteChar(sbsrc, start, end);
		if (idxStart < 0)  {
			retval.type = EXPRVALTYPE_NONE;
			return -1;
		}

		// start iterating

		// first char must be '"'
		ch = sbsrc.charAt(idxStart);
		if (ch != '\"') {
			retval.type = EXPRVALTYPE_NONE;
			return -1;
		}

		boolean bEnd = false;
		//mt_sbGetValue.delete(0, mt_sbGetValue.length());
		mt_sbGetValue.clear();
		for (idx = idxStart + 1; idx < end; idx ++)  {
			ch = sbsrc.charAt(idx);

			//check if it's '"' for end
			if (ch == '\"')  {
				// end of the string value
				bEnd = true;
				idx ++;
				break;
			}

			//check if it's a '\\' for an escaped '"'
			if (ch == '\\') {
				if (idx + 1 < end)  {
					ch1 = sbsrc.charAt(idx + 1);
					if (ch1 == '\"')  {
						idx ++;
						mt_sbGetValue.append('\"');
						continue;
					}
				}

			}

			//check if it's a token
			if (ch == mt_sTokenPrefix.charAt(0))  {
				if (idx + mt_nTokenPrefixLen < end)  {
					if (sbsrc.substring(idx, idx + mt_nTokenPrefixLen).equals(mt_sTokenPrefix))  {

						// find tokensuffix
						idx1 = sbsrc.indexOf(mt_sTokenSuffix, idx + mt_nTokenPrefixLen);
						if (idx1 > 0 && idx1 + mt_nTokenSuffixLen < end &&
							idx1 - (idx + mt_nTokenPrefixLen) <= mt_nMaxKeyLen)
						{
							// good to use, now check if it's a property
							sKey = sbsrc.substring(idx + mt_nTokenPrefixLen, idx1);
							sValue = mt_prop.getProperty(sKey);
							if (sValue != null)  {
								mt_sbGetValue.append(sValue);
								idx = idx1 + mt_nTokenSuffixLen - 1;
								continue;
							}

						}

					}

				}
			}

			// for any other chars, just append it
			mt_sbGetValue.append(ch);

		}

		if (bEnd)  {
			// a good string value
			retval.type = EXPRVALTYPE_STRING;
			retval.strvalue = mt_sbGetValue.toString();
			retval.boolvalue = false;
			return idx;
		}
		else  {
			retval.type = EXPRVALTYPE_NONE;
			return -1;
		}
	}

	private	int	psGetNumber(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		// now start low-level parsing
		int idxStart, idx;
		char ch;

		// skip whitespaces
		idxStart	= psGetNextNonWhiteChar(sbsrc, start, end);
		if (idxStart < 0)  {
			retval.type = EXPRVALTYPE_NONE;
			return -1;
		}

		// start iterating
		// the first must be +,-, or digit
		ch = sbsrc.charAt(idxStart);
		if (! (Character.isDigit(ch) || ch == '+' || ch == '-'))  {
			// not a key name
			retval.type = EXPRVALTYPE_NONE;
			return -1;
		}


		for (idx = idxStart + 1; idx < end; idx ++)  {
			ch = sbsrc.charAt(idx);

			//check if it's a valid key
			if (Character.isDigit(ch))	{
				continue;
			}
			else {
				// stop here
				break;
			}


		}

		if (idx >= end)  {
			// error occured
			retval.type = EXPRVALTYPE_NONE;
			return -1;
		}
		else  {

			// set the long value
			retval.type = EXPRVALTYPE_NUMBER;
			retval.longvalue = Long.parseLong(sbsrc.substring(idxStart, idx));
			retval.boolvalue = false;
			retval.strvalue = null;

			return idx;
		}
	}



	private	int	psGetTerm(
			StringBufferEx		sbsrc,
			int					start,
			int					end,
			ExprToken			retval
	) throws Exception
	{
		int	idxEnd,	idxEndPrev;
		ExprToken token	= new ExprToken();
		//int	nNotNum;

		// get left paren (<expression>)
		idxEnd = psGetLeftParen(sbsrc, start, end, token);
		if (token.type == EXPRVALTYPE_LEFTPAREN)  {

			// get expression
			idxEndPrev = idxEnd;
			idxEnd = psGetExpression(sbsrc, idxEndPrev, end, retval);

			if (retval.type == EXPRVALTYPE_NONE)  {
				// return, trace back
				throw new Exception("No expression after left parenthesis (line: " + getLineNum(sbsrc, idxEndPrev) + ")");
			}

			// find right paren
			idxEndPrev = idxEnd;
			idxEnd = psGetRightParen(sbsrc, idxEndPrev, end, token);

			if (token.type == EXPRVALTYPE_NONE)  {

				// return, trace back
				throw new Exception("No right parenthesis after expression for a term (line: " + getLineNum(sbsrc, idxEndPrev) + ")");
			}

			return idxEnd;

		}

		// unary expression
		idxEnd = psGetUnaryExpression(sbsrc, start, end, retval);
		if (retval.type != EXPRVALTYPE_NONE)  {
			// good,
			return idxEnd;
		}

		// factor
		ExprToken op = new ExprToken();

		// factor mul-op factor
		idxEnd = psGetFactor(sbsrc, start, end, retval);

		if (retval.type == EXPRVALTYPE_NONE)  {
			return -1;
		}

		// get mul-op
		idxEndPrev = idxEnd;
		idxEnd = psGetMulOp(sbsrc, idxEndPrev, end, op);
		if (op.type == EXPRVALTYPE_NONE) {
			// just return this retvals
			retval.type = EXPRVALTYPE_BOOL;
			return idxEndPrev;
		}

		// get right factor
		idxEndPrev = idxEnd;
		idxEnd = psGetFactor(sbsrc, idxEndPrev, end, token);
		if (token.type == EXPRVALTYPE_NONE) {
			throw new Exception("No right factor after a MulOp.");
		}

		// calculate
		try  {
			psMulOp(op, retval, token);
		}
		catch (Exception e) {
			throw new Exception(e.getMessage() + " (line: " +  getLineNum(sbsrc, idxEndPrev) + ")");
		}

		return idxEnd;
	}


	private void psMulOp(
			ExprToken		op,
			ExprToken		factor0,
			ExprToken		factor1
	) throws Exception
	{
		// compute

		// 1. numeric computation
		switch (op.type) {

			case EXPRVALTYPE_IN:

				if (factor0.type == EXPRVALTYPE_STRING)  {
					if (factor1.type != EXPRVALTYPE_STRING && factor1.type != EXPRVALTYPE_KEY)  {
						throw new Exception("In operation failed.");
					}
					else if (factor1.type == EXPRVALTYPE_KEY)  {
						if (factor1.strvalue == null)  {
							factor0.boolvalue = false;
						}
						else {
							factor0.boolvalue = (factor1.strvalue.indexOf(factor0.strvalue) > -1);
						}
					}
					else {
						// string
						factor0.boolvalue = (factor1.strvalue.indexOf(factor0.strvalue) > -1);
					}
				}
				else if (factor0.type == EXPRVALTYPE_KEY)  {
					if (factor1.type == EXPRVALTYPE_KEY)  {
						// only compare string
						if (factor0.strvalue != null && factor1.strvalue != null) {
							factor0.boolvalue = (factor1.strvalue.indexOf(factor0.strvalue) > -1);
						}
						else if (factor0.strvalue == null && factor1.strvalue != null) {
							factor0.boolvalue = false;  // a null always belongs to nothing
						}
						else if (factor0.strvalue != null && factor1.strvalue == null) {
							factor0.boolvalue = false;
						}
						else {
							factor0.boolvalue = (factor1.strvalue.indexOf(factor0.strvalue) > -1);
						}
					}
					else if (factor1.type == EXPRVALTYPE_STRING)  {
						if (factor0.strvalue == null)  {
							factor0.boolvalue = false;
						}
						else {
							factor0.boolvalue = (factor1.strvalue.indexOf(factor0.strvalue) > -1);
						}
					}
					else  {
						throw new Exception("Invalid factor1 type for \"in\" operation: " + factor1.type);
					}

				}
				else   {
					throw new Exception("Invalid factor0 type for \"in\" operation: " + factor0.type);
				}
				break;

			case EXPRVALTYPE_EQUAL:
			case EXPRVALTYPE_NOTEQUAL:

				if (factor0.type == EXPRVALTYPE_STRING)  {
					if (factor1.type != EXPRVALTYPE_STRING && factor1.type != EXPRVALTYPE_KEY)  {
						throw new Exception("MulOp failed.");
					}
					else if (factor1.type == EXPRVALTYPE_KEY)  {
						if (factor1.strvalue == null)  {
							factor0.boolvalue = (op.type == EXPRVALTYPE_NOTEQUAL);
						}
						else {
							factor0.boolvalue =
								op.type == EXPRVALTYPE_EQUAL ?
									factor0.strvalue.equals(factor1.strvalue) :
									! factor0.strvalue.equals(factor1.strvalue);
						}

					}
					else {
						// string
						factor0.boolvalue =
							op.type == EXPRVALTYPE_EQUAL ?
								factor0.strvalue.equals(factor1.strvalue) :
								! factor0.strvalue.equals(factor1.strvalue);
					}
				}
				else if (factor0.type == EXPRVALTYPE_KEY)  {
					if (factor1.type == EXPRVALTYPE_KEY)  {
						// only compare string
						if (factor0.strvalue != null && factor1.strvalue != null) {
							factor0.boolvalue =
								op.type == EXPRVALTYPE_EQUAL ?
									factor0.strvalue.equals(factor1.strvalue) :
									! factor0.strvalue.equals(factor1.strvalue);
						}
						else if ((factor0.strvalue == null && factor1.strvalue != null) ||
								 (factor0.strvalue != null && factor1.strvalue == null))
						{
							factor0.boolvalue = (op.type == EXPRVALTYPE_NOTEQUAL);
						}
						else {
							factor0.boolvalue = (op.type == EXPRVALTYPE_EQUAL);
						}
					}
					else if (factor1.type == EXPRVALTYPE_STRING)  {
						if (factor0.strvalue == null)  {
							factor0.boolvalue = (op.type == EXPRVALTYPE_NOTEQUAL);
						}
						else {
							factor0.boolvalue =
								op.type == EXPRVALTYPE_EQUAL ?
									factor0.strvalue.equals(factor1.strvalue) :
									! factor0.strvalue.equals(factor1.strvalue);
						}
					}
					else if (factor1.type == EXPRVALTYPE_NUMBER)  {
						if (factor0.strvalue == null) {
							factor0.boolvalue = (op.type == EXPRVALTYPE_NOTEQUAL);
						}
						else  {
							try  {
								factor0.longvalue = Long.parseLong(factor0.strvalue);
								factor0.boolvalue =
									op.type == EXPRVALTYPE_EQUAL ?
										(factor0.longvalue == factor1.longvalue) : (factor0.longvalue != factor1.longvalue);
							}
							catch (NumberFormatException e)  {
								factor0.boolvalue = (op.type == EXPRVALTYPE_NOTEQUAL);
							}
						}
					}
					else  {
						throw new Exception("Invalid factor1 type: " + factor1.type);
					}

				}
				else if (factor0.type == EXPRVALTYPE_NUMBER)  {
					// factor1 must key or number
					if (factor1.type == EXPRVALTYPE_KEY)  {
						if (factor1.strvalue == null)  {
							factor0.boolvalue = (op.type == EXPRVALTYPE_NOTEQUAL);
						}
						else {
							try  {
								factor1.longvalue = Long.parseLong(factor1.strvalue);
								factor0.boolvalue =
									op.type == EXPRVALTYPE_EQUAL ?
										(factor1.longvalue == factor0.longvalue) : (factor1.longvalue != factor0.longvalue);
							}
							catch (NumberFormatException e)  {
								factor0.boolvalue = (op.type == EXPRVALTYPE_NOTEQUAL);
							}
						}

					}
					else if (factor1.type == EXPRVALTYPE_NUMBER)  {
						factor0.boolvalue =
							op.type == EXPRVALTYPE_EQUAL ?
								(factor0.longvalue == factor1.longvalue) : (factor0.longvalue != factor1.longvalue);
					}
					else {
						throw new Exception("Invalid factor1 type: " + factor1.type);
					}

				}
				else  {
					throw new Exception("Invalid factor0 type: " + factor0.type);
				}
				break;


			case EXPRVALTYPE_GT:
			case EXPRVALTYPE_LT:
			case EXPRVALTYPE_GE:
			case EXPRVALTYPE_LE:

				// must be key-key, key-num, num-key, num-num
				if (factor0.type == EXPRVALTYPE_KEY )  {
					if (factor1.type == EXPRVALTYPE_KEY)  {
						// must be numberable
						if (factor0.strvalue == null)  {
							throw new Exception("Invalid mulop values.");
						}
						else {
							if (factor1.strvalue == null)  {
								throw new Exception("Invalid mulop values.");
							}
							else  {
								try  {
									factor0.longvalue = Long.parseLong(factor0.strvalue);
								}
								catch (NumberFormatException e)  {
									throw new Exception("Invalid mulop values.");
								}

								try  {
									factor1.longvalue = Long.parseLong(factor1.strvalue);
								}
								catch (NumberFormatException e)  {
									throw new Exception("Invalid mulop values.");
								}

								switch (op.type)  {
									case EXPRVALTYPE_GT:
										factor0.boolvalue = (factor0.longvalue > factor1.longvalue);
										break;

									case EXPRVALTYPE_LT:
										factor0.boolvalue = (factor0.longvalue < factor1.longvalue);
										break;

									case EXPRVALTYPE_GE:
										factor0.boolvalue = (factor0.longvalue >= factor1.longvalue);
										break;

									case EXPRVALTYPE_LE:
										factor0.boolvalue = (factor0.longvalue <= factor1.longvalue);
										break;
								}

							}

						}


					}
					else if (factor1.type == EXPRVALTYPE_NUMBER) {
						if (factor0.strvalue == null)  {
							throw new Exception("Invalid mulop values.");
						}
						else  {
							try  {
								factor0.longvalue = Long.parseLong(factor0.strvalue);
							}
							catch (NumberFormatException e)  {
								throw new Exception("Invalid mulop values.");
							}

							switch (op.type)  {
								case EXPRVALTYPE_GT:
									factor0.boolvalue = (factor0.longvalue > factor1.longvalue);
									break;

								case EXPRVALTYPE_LT:
									factor0.boolvalue = (factor0.longvalue < factor1.longvalue);
									break;

								case EXPRVALTYPE_GE:
									factor0.boolvalue = (factor0.longvalue >= factor1.longvalue);
									break;

								case EXPRVALTYPE_LE:
									factor0.boolvalue = (factor0.longvalue <= factor1.longvalue);
									break;
							}
						}


					}
					else  {
						throw new Exception("Invalid factor1 type (must be key or number): " + factor0.type);
					}
				}
				else if (factor0.type == EXPRVALTYPE_NUMBER)  {
					if (factor1.type == EXPRVALTYPE_KEY)  {
						// must be numberable
						if (factor1.strvalue == null) {
							throw new Exception("Invalid mulop values.");
						}
						else  {
							try  {
								factor1.longvalue = Long.parseLong(factor1.strvalue);
							}
							catch (NumberFormatException e)  {
								throw new Exception("Invalid mulop values.");
							}

							switch (op.type)  {
								case EXPRVALTYPE_GT:
									factor0.boolvalue = (factor0.longvalue > factor1.longvalue);
									break;

								case EXPRVALTYPE_LT:
									factor0.boolvalue = (factor0.longvalue < factor1.longvalue);
									break;

								case EXPRVALTYPE_GE:
									factor0.boolvalue = (factor0.longvalue >= factor1.longvalue);
									break;

								case EXPRVALTYPE_LE:
									factor0.boolvalue = (factor0.longvalue <= factor1.longvalue);
									break;
							}
						}

					}
					else if (factor1.type == EXPRVALTYPE_NUMBER) {
						switch (op.type)  {
							case EXPRVALTYPE_GT:
								factor0.boolvalue = (factor0.longvalue > factor1.longvalue);
								break;

							case EXPRVALTYPE_LT:
								factor0.boolvalue = (factor0.longvalue < factor1.longvalue);
								break;

							case EXPRVALTYPE_GE:
								factor0.boolvalue = (factor0.longvalue >= factor1.longvalue);
								break;

							case EXPRVALTYPE_LE:
								factor0.boolvalue = (factor0.longvalue <= factor1.longvalue);
								break;
						}
					}
					else  {
						throw new Exception("Invalid factor1 type (must be key or number): " + factor0.type);
					}
				}
				else  {
					throw new Exception("Invalid factor0 type (must be key or number): " + factor0.type);
				}

				break;


			default:
				throw new Exception("Invalid MulOp type: " + op.type);
		}


		factor0.type = EXPRVALTYPE_BOOL;
		factor0.strvalue = null;


	}



	/**
	 * Search files	in a given root	directory recursively
	 *
	 * @param	srcdir			source root	directory
	 *			dstdir			destination	root directory
	 *			prop			properties
	 *			tokenprefix		prefix of token
	 *			tokensuffix		suffix of token
	 *
	 * @return					true if	succeeded, otherwise, false
	 */
	public boolean replaceDirTokens(
			String				srcdir,
			String				dstdir,
			TokenConfigValidator	tokenConfigValidator
	)
	{
		return replaceDirTokens(
					srcdir,
					dstdir,
					m_trToken.prefix,
					m_trToken.suffix,
					tokenConfigValidator
		);
	}

	public boolean replaceDirTokens(
			String				srcdir,
			String				dstdir,
			String				tokenprefix,
			String				tokensuffix,
			TokenConfigValidator	tokenConfigValidator
	)
	{
		return replaceDirTokens(
					srcdir,
					dstdir,
					m_prop,
					m_nMaxKeyLen,
					tokenprefix,
					tokensuffix,
					tokenConfigValidator);
	}

	public boolean replaceDirTokens(
			String				srcdir,
			String				dstdir,
			Properties			prop,
			String				tokenprefix,
			String				tokensuffix,
			TokenConfigValidator	tokenConfigValidator
	)
	{
		return replaceDirTokens(
					srcdir,
					dstdir,
					prop,
					maxKeyLength(prop),
					tokenprefix,
					tokensuffix,
					tokenConfigValidator);
	}

	public boolean replaceDirTokens(
			String				srcdir,
			String				dstdir,
			Properties			prop,
			int					maxkeylen,
			String				tokenprefix,
			String				tokensuffix,
			TokenConfigValidator	tokenConfigValidator
	)
	{
		String		sTmpDir;
		File		fileCfgTmpDir;
		boolean		bRet = true;

		try	 {

			// check if	the	srcdir and dstdir exists
			File fileSrcDir	= new File(srcdir);
			if (!fileSrcDir.isDirectory())	{
				throw new Exception("Source	directory \"" +	srcdir + "\" doesn't exist.");
			}

			File fileDstDir	= new File(dstdir);
			// the dest dir doesn't have to exist
			if (fileDstDir.exists()) {
				if (!fileDstDir.isDirectory())	{
					throw new Exception("Destination directory \"" + dstdir	+ "\" doesn't exist.");
				}
			}


			// create a	temporary directory	and	copy src dir to	temp dir
			sTmpDir		= System.getProperty("java.io.tmpdir");

			if (sTmpDir	== null)  {
				return false;
			}

			fileCfgTmpDir	= new File(sTmpDir + File.separator	+ "_tmp_" +	System.currentTimeMillis());
			fileCfgTmpDir.mkdirs();

			// copy	srcdir to tmp dir
			if (!copyDir(fileSrcDir, fileCfgTmpDir))  {
				throw new Exception("Failed	in copying to temporary	directory: " + fileCfgTmpDir.getPath());
			}

			// replace tokens in the tmpdir
			initProcessSelect(prop,	maxkeylen, tokenprefix, tokensuffix);
			replaceDirTokens(fileCfgTmpDir,	prop, maxkeylen, tokenprefix, tokensuffix, tokenConfigValidator);

			// copy	the	file to	the	dst	dir
			if (!copyDir(fileCfgTmpDir,	fileDstDir))  {
				throw new Exception("Failed in copying to destination directory: " + dstdir);
			}


			//delete temp dir
			if (!m_bDebug)	{
				if (!deleteDir(fileCfgTmpDir))	{
					throw new Exception("Failed	in deleting	to temporary directory:	" +	fileCfgTmpDir.getPath());
				}
			}

		}
		catch (Exception e)	 {

			e.printStackTrace(System.err);
			bRet = false;
		}

		return bRet;

	}


	/**
	 * Search files	in a given root	directory recursively
	 *
	 * @param	rootdir			root directory
	 *			prop			properties
	 *			tokenprefix		prefix of token
	 *			tokensuffix		suffix of token
	 *
	 * @return					true if	succeeded, otherwise, false
	 */
	public boolean replaceDirTokens(
			String				rootdir,
			Properties			prop,
			int					maxkeylen,
			String				tokenprefix,
			String				tokensuffix,
			TokenConfigValidator	tokenConfigValidator
	) throws ConfigException
	{
		return replaceDirTokens(new	File(rootdir), prop, maxkeylen,	tokenprefix, tokensuffix, tokenConfigValidator);
	}

	/**
	 * Search files	in a given root	directory recursively
	 *
	 * @param	rootdir			root directory
	 *			prop			properties
	 *			tokenprefix		prefix of token
	 *			tokensuffix		suffix of token
	 *
	 * @return					true if	succeeded, otherwise, false
	 */
	public boolean replaceDirTokens(
			File				rootdir,
			Properties			prop,
			int					maxkeylen,
			String				tokenprefix,
			String				tokensuffix,
			TokenConfigValidator	tokenConfigValidator
	) throws ConfigException
	{
		boolean		bRet = true;

		// for callback
		ReplaceTokenSearchFileCallback		cb = new ReplaceTokenSearchFileCallback(
															this,
															prop,
															maxkeylen,
															tokenprefix,
															tokensuffix,
															tokenConfigValidator);

		try	 {

			// call	search files
			// filefileter is null for all files
			searchFiles(rootdir, null, cb);
		}
		catch (ConfigException ce) {
			throw ce;
		}
		catch (Exception e)	 {
			//bRet	= false;
			//e.printStackTrace(System.err);
			throw new ConfigException(e);
		}


		return bRet	&& cb.succeeded();

	}

	/**
	 * Search files	recursively
	 *
	 * @param	rootdir		root directory
	 *			filefilter	file name filter, null of filter is	not	needed
	 *			callback	ISearchFileCallback
	 * @return				true if	succeeded, otherwise false.
	 */
	public boolean searchFiles(
			File				rootdir,
			FileFilter			filefilter,
			SearchFileCallback	callback
	) throws ConfigException
	{
		File[]		files		= rootdir.listFiles(filefilter);
		File		curFile;

		// root
		//if (filefilter ==	null ||	filefilter.accept(rootdir))	 {
		//	callback.execFileProc(rootdir);
		//}

		for	(int i = 0;	i <	files.length; i	++)	 {

			try	 {

				curFile		= files[i];

				// check if	it's acceptable
				if (filefilter == null || filefilter.accept(curFile))  {
					callback.execFileProc(curFile);
					if (!callback.succeeded()) {
						throw new ConfigException("Token replacement failed.");
					}
				}

				// check if	it's a directory
				if (curFile.isDirectory())	{
					searchFiles(curFile, filefilter, callback);
					callback.execAfterSubdir(curFile);
					if (!callback.succeeded()) {
						throw new ConfigException("Token replacement failed.");
					}
				}
			}
			catch (ConfigException ce) {
				throw ce;
			}
			catch (Exception e)	 {
				callback.setFailed();
				//e.printStackTrace(System.err);
				throw new ConfigException(e);
			}
		}

		return callback.succeeded();
	}

	
	/**
	 * 
	 * @param srcFile
	 * @param dstFile
	 * @param tokenConfigValidator
	 * @throws ConfigException
	 */
	public void mergeWLConfig(
			String 		srcfile,
			String		dstfile,
			String		serverDir,
			TokenConfigValidator	tokenConfigValidator
	) throws ConfigException
	{
		// 1. check if the files exsit
		File fileDst = new File(dstfile);
		if (!fileDst.exists() || !fileDst.isFile() || !fileDst.canWrite()) {
			// throw it
			throw new ConfigException("The destination file doesn't exist or is invalid: [" + dstfile + "].");
		}
		
		
		log("wlcfgmerge: srcfile =	" +	srcfile);
		log("wlcfgmerge: dstfile =	" +	dstfile);
		

		try {
			// 2. token replacement on the source file	
			StringBufferEx sbRes = transformFileTokens(srcfile, m_prop, m_nMaxKeyLen, m_trToken.prefix, m_trToken.suffix, tokenConfigValidator);
			
			
			//========================================================
			// 3. dom
			DocumentBuilderFactory fac;
			DocumentBuilder docBuilder;
			Node nodeImport;
			String sVal;
			int i;
			
			
			// create dom builder
			fac = DocumentBuilderFactory.newInstance(); 
			docBuilder = fac.newDocumentBuilder();
			
			
			// create source dom and check validity
			ByteArrayInputStream isSrc = new ByteArrayInputStream(sbRes.getStringBuffer().toString().getBytes());
			Document docSrc = docBuilder.parse(isSrc);
			NodeList nlAppSrc = docSrc.getElementsByTagName("Application");
			int lenSrc = nlAppSrc.getLength();
			if (lenSrc == 0) {
				// do nothing as nothing to merge
				throw new ConfigException("No appliation exists in the source config [" + srcfile + "]");
			}
			
			// create the destination dom
			Document docDst = docBuilder.parse(fileDst);
			NodeList nlAppDst = docDst.getElementsByTagName("Application");
			
			HashSet hsSrc = wlcfgGetApplicationNameSet(nlAppSrc);
			if (hsSrc.size() == 0) {
				throw new ConfigException("No appliation name exists in the source config [" + srcfile + "]");
			}
			
			HashSet hsDst = wlcfgGetApplicationNameSet(nlAppDst);
			// there could be no app exists in dst
			
			// now validate
			Iterator itSrc = hsSrc.iterator();
			while (itSrc.hasNext()) {
				sVal = (String)itSrc.next();
				if (hsDst.contains(sVal)) {
					throw new ConfigException("The application [" + sVal + "] exists in the destination file. Unable to merge");
				}
			}
			
			// we need to modify the "Path" attribute for the Application element
			// so get the dir of dest first, -> serverDir,
			String serverDirA = null;
			if (serverDir == null) {
				throw new ConfigException("Missing serverDir");
			}
			char lastChar = serverDir.charAt(serverDir.length() - 1); 
			if (lastChar == '/' && lastChar == '\\') {
				serverDirA = serverDir.substring(0, serverDir.length() - 1);
			}
			else {
				serverDirA = serverDir;
			}
			
			
			// do merge
			Element eltRootDst = docDst.getDocumentElement();
			Element eltAppSrc;
			for (i = 0; i < lenSrc; ++ i) {
				eltAppSrc = (Element)nlAppSrc.item(i);
				eltAppSrc.setAttribute("Path", serverDirA);
				
				nodeImport = docDst.importNode(eltAppSrc, true);
				eltRootDst.appendChild(nodeImport);
			}
			
			// finally write to dest file
			Result resDst = new StreamResult(fileDst);
			Transformer tran = TransformerFactory.newInstance().newTransformer();
			tran.setOutputProperty(OutputKeys.INDENT, "yes");
			Source domOut = new DOMSource(docDst);
			tran.transform(domOut, resDst);
			// done
			
		}
		catch (Exception e) {
			throw new ConfigException("Unable to merge WebLogic config file - src[" + srcfile +
					"] dst[" + dstfile + "]", e);
		}
		
		
	}
			
	
	private HashSet wlcfgGetApplicationNameSet(NodeList nodeList)
	{
		HashSet hsRet = null;
		NamedNodeMap attMap;
		Node node, nodeAtt;
		String sVal;
		int i;
		
		// we need to make sure an application in src doesn't exist in 
		// destination file, otherwise, it's an invalid merge
		int len = nodeList.getLength();
		for (i = 0; i < len; ++ i) {
			node = nodeList.item(i);
			attMap = node.getAttributes();
			nodeAtt = attMap.getNamedItem("Name");
			if (nodeAtt != null) {
				sVal = nodeAtt.getNodeValue();
				if (sVal != null && !sVal.equals("")) {
					if (hsRet == null) {
						hsRet = new HashSet();
					}
					hsRet.add(sVal);
				}
			}
		}
		
		
		return hsRet;
	}
	
	
	
	
	
	
	
	//////////////////////////////////////////////////////////////////
	// testing methods


	/**
	 * test1
	 */
	private	static void	test1(
			String[]	args
	)
	{
		try	 {
			FileInputStream	fi	 = new FileInputStream(args[0]);

			Properties propOrig	= new Properties();

			// read	file
			propOrig.load(fi);
			fi.close();

			// display
			propOrig.list(System.out);


			// expand
			PropUtil pu	= new PropUtil();
			Properties propEx =	pu.expandProperties(propOrig, null);
			propEx.list(System.out);
			//System.out.println(propEx.getProperty("cfg.path.dserver"));
		}
		catch (Exception e)	{
			System.out.println(e);
		}

	}

	/**
	 * test2
	 */
	private	static void	test2(
			String[]	args
	)
	{
		try	 {
			FileInputStream	fi	 = new FileInputStream(args[0]);

			Properties propOrig	= new Properties();

			// read	file
			propOrig.load(fi);
			fi.close();

			// display
			propOrig.list(System.out);


			// expand
			PropUtil pu	= new PropUtil();
			Properties propEx =	pu.expandProperties(propOrig, null);
			propEx.list(System.out);
			//System.out.println(propEx.getProperty("cfg.path.dserver"));

			// get connection pool
			//Properties prop1 = searchKeysContainField(propEx,	"JDBCConnectionPool");
			//Properties prop1 = searchKeys(propEx,	"cae\\.wl\\.JDBCConnectionPool\\.[\\w]+\\.[\\w]+");
			//prop1.list(System.out);

			// GET DISTINCT
			LinkedHashSet	lhSet =	pu.selectKeyDistinct(
						propEx,
						"cae\\.wl\\.JDBCConnectionPool\\.[\\w]+\\.[\\w]+",
						3,
						4);
			Iterator itSet = lhSet.iterator();
			while (itSet.hasNext())	 {
				System.out.println(itSet.next());
			}



		}
		catch (Exception e)	{
			System.out.println(e);
		}
	}


	/**
	 * test3
	 */
	private	static void	test3(
			String[]	args
	)
	{
		try	 {

			StringBufferEx sb	= new StringBufferEx(1024);

			PropUtil pu	= new PropUtil();

			pu.readFile(sb,	args[0]);
			pu.writeFile(sb, args[0] + ".ORIG");

			pu.readFile(sb,	args[1]);
			pu.writeFile(sb, args[1] + ".ORIG");

			//System.out.println("file size	= "	+ sb.length());
			//System.out.println("file :");
			//System.out.println(sb.toString());
			System.out.println("done");
		}
		catch (Exception e)	 {
			System.out.println(e.toString());//
		}
	}


	/**
	 * test4
	 */
	private	static void	test4(
			String[]	args
	)
	{
		try	 {

			FileInputStream	fi	 = new FileInputStream(args[0]);
			Properties prop	= new Properties();


			// read	file
			prop.load(fi);
			fi.close();

			PropUtil pu	= new PropUtil();
			Properties	prop1	= pu.expandProperties(prop, null);


			StringBufferEx sbsrc		= new StringBufferEx(1024);
			StringBufferEx sbdst		= new StringBufferEx(1024);

			pu.readFile(sbsrc, args[1]);

			pu.replaceTokens(
					sbsrc,
					sbdst,
					prop1,
					"@",
					"@"
			);

			pu.writeFile(sbdst,	args[1]	+ ".result");

			System.out.println("done");

		}
		catch (Exception e)	 {
			System.out.println(e.toString());//
		}

	}


	/**
	 * test5, copy directory
	 */
	private	static void	test5(
			String[]	args
	)
	{
		//copyDir(args[0], args[1]);
		PropUtil pu	= new PropUtil();
		pu.copyDir("C:\\sdpwork\\dev\\cae_test\\prepare\\appserver\\APPSERVER_cae_us\\config\\cae-domain\\environment\\dev",
				"C:\\sdpwork\\tmp\\cfgtest");
	};

	/**
	 * test6, delete directory
	 */
	private	static void	test6(
			String[]	args
	)
	{
		//copyDir(args[0], args[1]);
		PropUtil pu	= new PropUtil();
		pu.deleteDir("C:\\sdpwork\\tmp\\cfgtest");
	};

	/**
	 * test7, replace tokens in	a directory
	 */
	private	static void	test7(
			String[]	args
	)
	{
		//copyDir(args[0], args[1]);
		PropUtil pu	= new PropUtil();
		pu.deleteDir("C:\\sdpwork\\tmp\\cfgtest");
	};
	/**
	 * main	method for standalone usage
	 * for now,	it's only a	testing
	 */
	public static void main(
			String[] args
	)
	{
		//test1(args);
		try	{
			//System.out.println(InetAddress.getLocalHost().getHostAddress());

			//System.out.println("finished.");

			//String ss = System.getProperty("line.separator");
			boolean bb = NEWLINE.equals("\r\n");
			System.out.print(bb);
			
			String dd = "dd";
			System.out.print(dd);
		}
		catch (Exception e)	{
			e.printStackTrace(System.err);
		}
	}


}

