/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vzw.util;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.regex.Pattern;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author hud
 */
public class LogUtil {
	
	private static final int		FIRST_STACK_LEVEL = 3;
	
	/**
	 * Only log timestamps
	 */
	private static Logger			timestampLogger = null;
	
	/**
	 * only log diagnosis data, not additive
	 */
	private static Logger			diagnosisLogger = null;
	
	
	/**
	 * Purpose of this class is to pass some decoration information
	 * to the logger, currently, only MDN is passed
	 */
	public static class WorkLogger {
		private String			info = null;
		private Logger			logger = null;
		
		public WorkLogger(Logger logger, String info) {
			this.logger = logger;
			this.info = info;
		}

		public String getInfo() {
			return info;
		}

		public void setInfo(String info) {
			this.info = info;
		}

		public Logger getLogger() {
			return logger;
		}

		public void setLogger(Logger logger) {
			this.logger = logger;
		}
		
		public boolean isDebugEnabled() {
			return logger.isDebugEnabled();
		}
	}

	
	
	
	/**
	 * A thread local class that stores formatting data
	 * this is only for the root logger
	 * If there are other loggers, this may not be accurate
	 */
	private static class LogConfig {
		private boolean				initialized = false;
		
		
		
		///////////////////////////////////////////////////////
		private boolean				displayCallerInfo = false;
		
		/**
		 * This only needs to be called once
		 * @param logger 
		 */
		private LogConfig init(Logger logger) {
			if (!initialized) {
				Logger lg = logger;

				do {
					Enumeration<Appender> ae = lg.getAllAppenders();
					for (; ae.hasMoreElements(); ) {
						Appender a = ae.nextElement();
						Layout l = a.getLayout();
						String pstr = null;
						if (l instanceof PatternLayout) {
							pstr = ((PatternLayout)l).getConversionPattern();
						}

						if (pstr != null && 
							 (pstr.contains("%F") ||
									pstr.contains("%L") ||
									pstr.contains("%l"))) {

							displayCallerInfo = true;
						}
					}
					
					if (!lg.getAdditivity()) {
						break;
					}
					
					lg = (Logger)lg.getParent();
					if (lg == null) {
						break;
					}
					
				} while(true);
					
				initialized = true;
			}
			
			return this;
		}

		public boolean isDisplayCallerInfo() {
			return displayCallerInfo;
		}

	}
	
	
	/**
	 * Initialize the logger by reading the log4j.properties
	 */
	static {
		_initTimestampLogger();
		_initDiagnosisLogger();
	}
	
	private static ThreadLocal<LogConfig> logConfig = new ThreadLocal<LogConfig> () {

		@Override
		protected LogConfig initialValue() {
			return new LogConfig();
		}
		
	};
	
	/**
	 * 
	 */
	private static void _initTimestampLogger() {
		try {
			timestampLogger = Logger.getLogger("timestamp");
		}
		catch (Exception e) {
			System.out.println("Unable to initialize timestamp logger: " + e.getMessage());
		}
	}
	
	/**
	 * 
	 */
	private static void _initDiagnosisLogger() {
		try {
			diagnosisLogger = Logger.getLogger("diagnosis");
		}
		catch (Exception e) {
			System.out.println("Unable to initialize diagnosis logger: " + e.getMessage());
		}
		
	}
	
	/**
	 * Log the timestamp, do not print method information, which should be done by the caller only
	 * @param msg
	 * @param params 
	 */
	public static void tlog(String msg, Object...params) {
		if (timestampLogger != null) {
			info(timestampLogger, msg, params);
		}
	}
	
	public static void tdlog(String msg, Object...params) {
		if (timestampLogger != null) {
			debug(timestampLogger, msg, params);
		}
	}
	
	/**
	 * Log the diagnosis info. do not print method information for performance purpose.
	 * @param msg
	 * @param params 
	 */
	public static void dlog(String msg, Object...params) {
		if (diagnosisLogger != null) {
			debug(diagnosisLogger, msg, params);
		}
	}
	
	/**
	 * Shorten the given string to avoid too long message
	 * By default, the length will be cut to 16, call the other version to 
	 * specify length
	 * @param msg
	 * @return 
	 */
	public static String s(String msg) {
		return s(msg, 16);
	}
	
	/**
	 * 
	 * @param msg
	 * @param len
	 * @return 
	 */
	public static String s(String msg, int len) {
		return org.apache.commons.lang3.StringUtils.abbreviateMiddle(msg, "...", len);
	}
	
	
	///////////////////////////////////////////////////////////////////////
	/////////////////// Internal methods that need to /////////////////////
	/////////////////// print out methods and line numbers ////////////////
	private static <E extends Exception> void _logAndThrow(Logger logger, Level level, E e, int stackLevel)
			throws E {

		if (logger.getEffectiveLevel().toInt() <= level.toInt()) {
			
			if (logConfig.get().init(logger).isDisplayCallerInfo()) {
				StackTraceElement ste = Thread.currentThread().getStackTrace()[stackLevel];
				logger.log(level, MessageFormat.format("[{0}:{1}:{2}] - {3}", 
						ste.getClassName(),
						ste.getMethodName(),
						ste.getLineNumber(),
						e.getMessage() == null ? "" : e.getMessage()), e.getCause());
			}
			else {
				logger.log(level, e.getMessage() == null ? "" : e.getMessage(), e.getCause());
			}
		}
		throw e;
	}
	
	
	private static <E extends Exception> void _logAndThrowWithMdn(Logger logger, Level level, String mdn, E e, int stackLevel)
			throws E {

		if (logger.getEffectiveLevel().toInt() <= level.toInt()) {
			
			if (logConfig.get().init(logger).isDisplayCallerInfo()) {
				StackTraceElement ste = Thread.currentThread().getStackTrace()[stackLevel];
				
				if (mdn == null) {
					logger.log(level, MessageFormat.format("[{0}:{1}:{2}] - {3}", 
							ste.getClassName(),
							ste.getMethodName(),
							ste.getLineNumber(),
							e.getMessage() == null ? "" : e.getMessage()), e.getCause());
				}
				else {
					logger.log(level, MessageFormat.format("[MDN:{0}] [{1}:{2}:{3}] - {4}", 
							mdn,
							ste.getClassName(),
							ste.getMethodName(),
							ste.getLineNumber(),
							e.getMessage() == null ? "" : e.getMessage()), e.getCause());
				}
			}
			else {
				if (mdn == null) {
					logger.log(level,  
							e.getMessage() == null ? "" : e.getMessage(), 
							e.getCause());
				}
				else {
					logger.log(level,  
							MessageFormat.format("[MDN:{0}] {1}", mdn, e.getMessage() == null ? "" : e.getMessage()), 
							e.getCause());
				}
			}
		}
		throw e;
	}	
	
	/**
	 * Add mdn in front: [MDN:xxxxxxxxxx]
	 * @param logger
	 * @param level
	 * @param mdn
	 * @param t
	 * @param stackLevel
	 * @param msgPattern
	 * @param args 
	 */
	private static void _logWithMdn(Logger logger, Level level, String mdn, Throwable t, int stackLevel, String msgPattern, Object...args) {
		if (logger.getEffectiveLevel().toInt() <= level.toInt()) {
			if (logConfig.get().init(logger).isDisplayCallerInfo()) {
				StackTraceElement ste = Thread.currentThread().getStackTrace()[stackLevel];
				if (t == null) {
					logger.log(level, MessageFormat.format("[{0}:{1}:{2}] - {3}", 
						ste.getClassName(),
						ste.getMethodName(),
						ste.getLineNumber(),
						formatMessageWithMdn(mdn, msgPattern, args)));
				}
				else {
					logger.log(level, MessageFormat.format("[{0}:{1}:{2}] - {3}", 
						ste.getClassName(),
						ste.getMethodName(),
						ste.getLineNumber(),
						formatMessageWithMdn(mdn, msgPattern, args)), t);
				}
				
			}
			else {
				if (t == null) {
					logger.log(level, formatMessageWithMdn(mdn, msgPattern, args));
				}
				else {
					logger.log(level, formatMessageWithMdn(mdn, msgPattern, args), t);
				}
			}
		}
		
	}
	

	/**
	 *
	 * @param logger
	 * @param level
	 * @param msgPattern
	 * @param args
	 */
	private static void _log(Logger logger, Level level, Throwable t, int stackLevel, String msgPattern, Object...args) {
		if (logger.getEffectiveLevel().toInt() <= level.toInt()) {
			if (logConfig.get().init(logger).isDisplayCallerInfo()) {
				StackTraceElement ste = Thread.currentThread().getStackTrace()[stackLevel];
				if (t == null) {
					logger.log(level, MessageFormat.format("[{0}:{1}:{2}] - {3}", 
						ste.getClassName(),
						ste.getMethodName(),
						ste.getLineNumber(),
						formatMessage(msgPattern, args)));
				}
				else {
					logger.log(level, MessageFormat.format("[{0}:{1}:{2}] - {3}", 
						ste.getClassName(),
						ste.getMethodName(),
						ste.getLineNumber(),
						formatMessage(msgPattern, args)), t);
				}
				
			}
			else {
				if (t == null) {
					logger.log(level, formatMessage(msgPattern, args));
				}
				else {
					logger.log(level, formatMessage(msgPattern, args), t);
				}
			}
		}
	}
	
	/**
	 * if length of args is 0, try to escape the "{" and "}"
	 * because in this case, caller simply wants to print the text
	 * 
	 * otherwise, indicating caller knows what to do, it's caller's responsibility
	 * to escape {}
	 * 
	 * 
	 * @param msgPattern
	 * @param args
	 * @return 
	 */
	private static final Pattern			SINGLE_QUOTE_PATTERN = Pattern.compile("(')");
	private static String formatMessage(String msgPattern, Object...args) {
		String pStr;
		if (args.length == 0) {
			//Matcher m = SINGLE_QUOTE_PATTERN.matcher(msgPattern);
			//pStr = new StringBuilder("'").append(m.replaceAll("''")).append("'").toString();
			pStr = msgPattern;
		}
		else {
			pStr = MessageFormat.format(msgPattern, args);
		}
		
		return pStr;
	}
	
	/**
	 * 
	 * @param mdn
	 * @param msgPattern
	 * @param args
	 * @return 
	 */
	private static String formatMessageWithMdn(String mdn, String msgPattern, Object...args) {
		
		String pStr;
		if (args.length == 0) {
			//Matcher m = SINGLE_QUOTE_PATTERN.matcher(msgPattern);
			
			if (mdn == null) {
				pStr = msgPattern;//new StringBuilder("'").append(m.replaceAll("''")).append("'").toString();
			}
			else {
				pStr = new StringBuilder("[MDN:").append(mdn).append("] ")
						.append(msgPattern).toString();
			}
			
		}
		else {
			String pStr0 = MessageFormat.format(msgPattern, args);
			
			if (mdn == null) {
				pStr = pStr0;
			}
			else {
				pStr = MessageFormat.format("[MDN:{0}] {1}", mdn, pStr0);
			}
			
		}
		
		return pStr;
	}
	
	//////////////////////////////////////////////////////////////////////
	//////////////////////// Public Methods //////////////////////////////
	//////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * @param <E>
	 * @param logger
	 * @param level
	 * @param e
	 * @throws E
	 */
	public static <E extends Exception> void logAndThrow(Logger logger, Level level, E e)
			throws E {

		_logAndThrow(logger, level, e, FIRST_STACK_LEVEL);
	}

	
	public static <E extends Exception> void logAndThrowWithMdn(Logger logger, Level level, String mdn, E e) 
			throws E {
		_logAndThrowWithMdn(logger, level, mdn, e, FIRST_STACK_LEVEL);
	}


	public static void infoOrError(Logger logger, AbstractException e, Object...args) {
		if (e.getData() == null) {
			// error
			_log(logger, Level.ERROR, e, FIRST_STACK_LEVEL, e.getMessage(), args);
		}
		else {
			_log(logger, Level.INFO, e, FIRST_STACK_LEVEL, e.getMessage(), args);
		}
	}

	public static void infoOrErrorWithMdn(Logger logger, String mdn, AbstractException e, Object...args) {
		if (e.getData() == null) {
			// error
			_logWithMdn(logger, Level.ERROR, mdn, e, FIRST_STACK_LEVEL, e.getMessage(), args);
		}
		else {
			_logWithMdn(logger, Level.INFO, mdn, e, FIRST_STACK_LEVEL, e.getMessage(), args);
		}
	}



	public static void error(Logger logger, Throwable t, String msgPattern, Object...args) {
		_log(logger, Level.ERROR, t, FIRST_STACK_LEVEL, msgPattern, args);
	}

	public static void errorWithMdn(Logger logger, String mdn, Throwable t, String msgPattern, Object...args) {
		_logWithMdn(logger, Level.ERROR, mdn, t, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	public static void error(WorkLogger workLogger, Throwable t, String msgPattern, Object...args) {
		_logWithMdn(workLogger.getLogger(), Level.ERROR, workLogger.getInfo(), t, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	public static void error(Logger logger, String msgPattern, Object...args) {
		_log(logger, Level.ERROR, null, FIRST_STACK_LEVEL, msgPattern, args);
	}

	public static void errorWithMdn(Logger logger, String mdn, String msgPattern, Object...args) {
		_logWithMdn(logger, Level.ERROR, mdn, null, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	public static void error(WorkLogger workLogger, String msgPattern, Object...args) {
		_logWithMdn(workLogger.getLogger(), Level.ERROR, workLogger.getInfo(), null, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	public static <T extends Throwable> void errorAndThrow(Logger logger, Throwable cause, T toThrow)
			throws T {

		_log(logger, Level.ERROR, cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		//error(logger, cause, toThrow.getMessage());
		throw toThrow;
	}

	public static <T extends Throwable> void errorAndThrowWithMdn(Logger logger, String mdn, Throwable cause, T toThrow)
			throws T {

		_logWithMdn(logger, Level.ERROR, mdn, cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		//error(logger, cause, toThrow.getMessage());
		throw toThrow;
	}
	
	public static <T extends Throwable> void errorAndThrow(WorkLogger workLogger, Throwable cause, T toThrow)
		throws T {
		_logWithMdn(workLogger.getLogger(), Level.ERROR, workLogger.getInfo(), cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		//error(logger, cause, toThrow.getMessage());
		throw toThrow;
	}
	
	public static <T extends Throwable> void errorAndThrow(Logger logger, T toThrow)
			throws T {

		_log(logger, Level.ERROR, null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}

	public static <T extends Throwable> void errorAndThrowWithMdn(Logger logger, String mdn, T toThrow)
			throws T {

		_logWithMdn(logger, Level.ERROR, mdn, null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}
	
	public static <T extends Throwable> void errorAndThrow(WorkLogger workLogger, T toThrow)
			throws T {
		_logWithMdn(workLogger.getLogger(), Level.ERROR, workLogger.getInfo(), null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}	
	
	/**
	 *
	 * @param logger
	 * @param msgPattern
	 * @param args
	 */
	public static void info(Logger logger, Throwable t, String msgPattern, Object...args) {
		_log(logger, Level.INFO, t, FIRST_STACK_LEVEL, msgPattern, args);
	}

	public static void infoWithMdn(Logger logger, String mdn, Throwable t, String msgPattern, Object...args) {
		_logWithMdn(logger, Level.INFO, mdn, t, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	public static void info(WorkLogger workLogger, Throwable t, String msgPattern, Object...args) {
		_logWithMdn(workLogger.getLogger(), Level.INFO, workLogger.getInfo(), t, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	
	public static void info(Logger logger, String msgPattern, Object...args) {
		_log(logger, Level.INFO, null, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	/**
	 * Use common MDN format and put at the beginning of the message
	 * @param logger
	 * @param mdn
	 * @param msgPattern
	 * @param args 
	 */
	public static void infoWithMdn(Logger logger, String mdn, String msgPattern, Object...args) {
		_logWithMdn(logger, Level.INFO, mdn, null, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	public static void info(WorkLogger workLogger, String msgPattern, Object...args) {
		_logWithMdn(workLogger.getLogger(), Level.INFO, workLogger.getInfo(), null, FIRST_STACK_LEVEL, msgPattern, args);
	}	

	public static <T extends Throwable> void infoAndThrow(Logger logger, Throwable cause, T toThrow)
			throws T {

		_log(logger, Level.INFO, cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}
	
	public static <T extends Throwable> void infoAndThrow(Logger logger, T toThrow)
			throws T {

		_log(logger, Level.INFO, null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}
	
	public static <T extends Throwable> void infoAndThrowWithMdn(Logger logger, String mdn, Throwable cause, T toThrow)
			throws T {

		_logWithMdn(logger, Level.INFO, mdn, cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}
	
	public static <T extends Throwable> void infoAndThrow(WorkLogger workLogger, Throwable cause, T toThrow)
			throws T {
		_logWithMdn(workLogger.getLogger(), Level.INFO, workLogger.getInfo(), cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}	
	
	public static <T extends Throwable> void infoAndThrowWithMdn(Logger logger, String mdn, T toThrow)
			throws T {

		_logWithMdn(logger, Level.INFO, mdn, null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}	
	
	public static <T extends Throwable> void infoAndThrow(WorkLogger workLogger, T toThrow)
			throws T {
		_logWithMdn(workLogger.getLogger(), Level.INFO, workLogger.getInfo(), null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}		
	
	public static <T extends Throwable> void warnAndThrow(Logger logger, Throwable cause, T toThrow)
			throws T {

		_log(logger, Level.WARN, cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}
	
	public static <T extends Throwable> void warnAndThrowWithMdn(Logger logger, String mdn, Throwable cause, T toThrow)
			throws T {

		_logWithMdn(logger, Level.WARN, mdn, cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}

	public static <T extends Throwable> void warnAndThrow(Logger logger, T toThrow)
			throws T {

		_log(logger, Level.WARN, null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}	
	
	
	public static <T extends Throwable> void warnAndThrowWithMdn(Logger logger, String mdn, T toThrow)
			throws T {

		_logWithMdn(logger, Level.WARN, mdn, null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}	
	
	public static <T extends Throwable> void warnAndThrow(WorkLogger workLogger, Throwable cause, T toThrow)
			throws T {
		_logWithMdn(workLogger.getLogger(), Level.WARN, workLogger.getInfo(), cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}	
	
	public static <T extends Throwable> void warnAndThrow(WorkLogger workLogger, T toThrow)
			throws T {
		_logWithMdn(workLogger.getLogger(), Level.WARN, workLogger.getInfo(), null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}		
	
	/**
	 *
	 * @param logger
	 * @param msgPattern
	 * @param args
	 */
	public static void debug(Logger logger, Throwable t, String msgPattern, Object...args) {
		_log(logger, Level.DEBUG, t, FIRST_STACK_LEVEL, msgPattern, args);
	}

	public static void debugWithMdn(Logger logger, String mdn, Throwable t, String msgPattern, Object...args) {
		_logWithMdn(logger, Level.DEBUG, mdn, t, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	/**
	 * 
	 * @param logger
	 * @param msgPattern
	 * @param args 
	 */
	public static void debug(Logger logger, String msgPattern, Object...args) {
		_log(logger, Level.DEBUG, null, FIRST_STACK_LEVEL, msgPattern, args);
	}

	public static void debugWithMdn(Logger logger, String mdn, String msgPattern, Object...args) {
		_logWithMdn(logger, Level.DEBUG, mdn, null, FIRST_STACK_LEVEL, msgPattern, args);
	}

	public static void debug(WorkLogger workLogger, String msgPattern, Object...args) {
		_logWithMdn(workLogger.getLogger(), Level.DEBUG, workLogger.getInfo(), null, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	/**
	 *
	 * @param logger
	 * @param msgPattern
	 * @param args
	 */
	public static void warn(Logger logger, Throwable t, String msgPattern, Object...args) {
		_log(logger, Level.WARN, t, FIRST_STACK_LEVEL, msgPattern, args);
	}

	public static void warnWithMdn(Logger logger, String mdn, Throwable t, String msgPattern, Object...args) {
		_logWithMdn(logger, Level.WARN, mdn, t, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	public static void warn(WorkLogger workLogger, Throwable t, String msgPattern, Object...args) {
		_logWithMdn(workLogger.getLogger(), Level.WARN, workLogger.getInfo(), t, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	public static void warn(Logger logger, String msgPattern, Object...args) {
		_log(logger, Level.WARN, null, FIRST_STACK_LEVEL, msgPattern, args);
	}

	public static void warnWithMdn(Logger logger, String mdn, String msgPattern, Object...args) {
		_logWithMdn(logger, Level.WARN, mdn, null, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	public static void warn(WorkLogger workLogger, String msgPattern, Object...args) {
		_logWithMdn(workLogger.getLogger(), Level.WARN, workLogger.getInfo(), null, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	/**
	 *
	 * @param logger
	 * @param msgPattern
	 * @param args
	 */
	public static void fatal(Logger logger, Throwable t, String msgPattern, Object...args) {
		_log(logger, Level.FATAL, t, FIRST_STACK_LEVEL, msgPattern, args);
	}

	public static void fatalWithMdn(Logger logger, String mdn, Throwable t, String msgPattern, Object...args) {
		_logWithMdn(logger, Level.FATAL, mdn, t, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	
	public static void fatal(Logger logger, String msgPattern, Object...args) {
		_log(logger, Level.FATAL, null, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	public static void fatalWithMdn(Logger logger, String mdn, String msgPattern, Object...args) {
		_logWithMdn(logger, Level.FATAL, mdn, null, FIRST_STACK_LEVEL, msgPattern, args);
	}
	
	
	public static <T extends Throwable> void fatalAndThrow(Logger logger, Throwable cause, T toThrow)
			throws T {

		_log(logger, Level.FATAL, cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		//error(logger, cause, toThrow.getMessage());
		throw toThrow;
	}

	public static <T extends Throwable> void fatalAndThrowWithMdn(Logger logger, String mdn, Throwable cause, T toThrow)
			throws T {

		_logWithMdn(logger, Level.FATAL, mdn, cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		//error(logger, cause, toThrow.getMessage());
		throw toThrow;
	}
	
	public static <T extends Throwable> void fatalAndThrow(WorkLogger workLogger, Throwable cause, T toThrow)
			throws T {

		_logWithMdn(workLogger.getLogger(), Level.FATAL, workLogger.getInfo(), cause, FIRST_STACK_LEVEL, toThrow.getMessage());
		//error(logger, cause, toThrow.getMessage());
		throw toThrow;
	}
	
	public static <T extends Throwable> void fatalAndThrow(Logger logger, T toThrow)
			throws T {

		_log(logger, Level.FATAL, null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}	
	
	public static <T extends Throwable> void fatalAndThrowWithMdn(Logger logger, String mdn, T toThrow)
			throws T {

		_logWithMdn(logger, Level.FATAL, mdn, null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}	
	
	public static <T extends Throwable> void fatalAndThrow(WorkLogger workLogger, String mdn, T toThrow)
			throws T {

		_logWithMdn(workLogger.getLogger(), Level.FATAL, workLogger.getInfo(), null, FIRST_STACK_LEVEL, toThrow.getMessage());
		throw toThrow;
	}	
}
