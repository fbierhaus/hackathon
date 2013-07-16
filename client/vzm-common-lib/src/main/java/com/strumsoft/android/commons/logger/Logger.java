/**
 * Logger.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2012 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.strumsoft.android.commons.logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.CRC32;

import org.acra.ErrorReporter;
import org.apache.log4j.Level;

import com.verizon.messaging.vzmsgs.AcraReports;
import com.verizon.mms.pdu.RetrieveConf;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import de.mindpipe.android.logging.log4j.LogConfigurator;

/**
 * This class is used to control the application logging level.
 * 
 * @author Jegadeesan.M
 * @Since Mar 20, 2012
 */
public class Logger implements FileFilter {

    private static final String TAG = "VZMLogger";
    // ALL will be true for Debug build version
    public static final boolean IS_DEBUG_ENABLED = true;
    public static final boolean IS_WARNING_ENABLED = true;
    public static final boolean IS_INFO_ENABLED = true;
    public static final boolean IS_ERROR_ENABLED = true;
    public static final boolean IS_ACRA_ENABLED = true;
    public static final boolean TRACE_LONG_QUERIES = false;
    
    public static final boolean IS_ACRA_ERROR_REPORT_ENABLED = true; // to be used for Logger.postError to
                                                                     // post error to Acra
    public static final boolean IS_MEMDUMP_ENABLED = false;
    public static final boolean IS_STRICT_MODE_ENABLED = false;
    public static final boolean STRICT_MODE_FATAL = false;
    public static final boolean IGNORE_SQL_CONFLICT = true;

    // private static final long MIN_FREE_SPACE = 8 * 1024 * 1024; // don't do local logging if < this free
    // private static final long MAX_LOCAL_LOG_SIZE = MIN_FREE_SPACE / 2; // max size of local log
    // private static final int MAX_LOCAL_LOG_FILES = 8; // number of log files to keep
    // private static final float PERCENT_FREE_SPACE = 0.2f; // use no more than this much of free space

    // XXX: SANDEEP INCREASE LOG SIZE FOR VMA BUILDS
    private static final long MIN_FREE_SPACE = 8 * 1024 * 1024; // don't do local logging if < this free
    private static final long MAX_LOCAL_LOG_SIZE = 9 * 1024 * 1024; // max size of local log
    private static final int MAX_LOCAL_LOG_FILES = 9; // number of log files to keep
    private static final float PERCENT_FREE_SPACE = 0.6f; // use no more than this much of free space

    private static final String LOCAL_HEADER_FMT = "%tm-%<td %<tH:%<tM:%<tS.%<tL %5d %5d %s";
    private String LOG_FILE_NAME = "vzm-debug.log";

    private static org.apache.log4j.Logger localLogger;
    private static String localLogFname;
    private static Formatter header;
    private static StringBuilder headersb;

    private static Logger instance;
    private Context context;

    /**
     * Set the Value of the instance
     * 
     * @return the instance
     */
    public static Logger init(Context context) {
        if (instance == null) {
            instance = new Logger(context);
        }
        return instance;
    }

    public static Logger getInstance() {
        return instance;
    }

    private Logger(Context context) {
        this.context = context;
        if (IS_DEBUG_ENABLED) {
            openLocalLog();
        }
    }

    public static void debug(Object... objs) {
        if (IS_DEBUG_ENABLED) {
            final String message = formatMessage(objs);
            Log.d(TAG, getHeader(Level.DEBUG) + message);
            logLocal(Level.DEBUG, message);
        }
    }

    public static void info(Object... objs) {
        if (IS_INFO_ENABLED) {
            final String message = formatMessage(objs);
            Log.i(TAG, getHeader(Level.INFO) + message);
            logLocal(Level.INFO, message);
        }
    }

    public static void warn(Object... objs) {
        if (IS_WARNING_ENABLED) {
            final String message = formatMessage(objs);
            Log.w(TAG, getHeader(Level.WARN) + message);
            logLocal(Level.WARN, message);
        }
    }

    public static String postThreadStacksToLog() {
    	StringBuffer buf = new StringBuffer();
    	buf.append(" Thread stacks on crash\n");
    	try {
    		Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();

    		for(Thread t : stacks.keySet()) {
    			buf.append(t.toString() + "\n");
    			StackTraceElement[] stackElems = stacks.get(t);
    			for(StackTraceElement elem : stackElems) {
    				buf.append("\t" + elem.toString() + "\n");
    			}
    			buf.append("\n");
    		}
    	} catch (Exception e) {
    		buf.append("Got exception while getting stack traces: " + e.getMessage());
    	}
    	Logger.debug(buf.toString());
    	return buf.toString();
    }
 
    public static void postErrorToAcraIfDebug(Object... objs) {
    	if (Logger.IS_DEBUG_ENABLED) {
			String trace = postThreadStacksToLog();

    		postErrorToAcra(objs);
    	}
    }
    
    public static void postErrorToAcra(Object... objs) {
        final String message = formatMessage(objs);
        if (Logger.IS_ERROR_ENABLED) {
        	String trace = postThreadStacksToLog();
            Log.e(TAG, getHeader(Level.ERROR) + message);
            logLocal(Level.ERROR, message);
        }

        if (Logger.IS_ACRA_ERROR_REPORT_ENABLED) {
            Context ctx = getInstance().context;
            CRC32 chksum = new CRC32();
            chksum.update(message.getBytes());
            long chksumVal = chksum.getValue();
            long crashtime = System.currentTimeMillis();
            boolean alreadyReported = instance.isAlreadyReportedCrash(chksumVal, message);
            boolean doReport = true;
            if (((message.startsWith("DeviceConfig: Device not found") || message.contains("BitmapManager: not enough memory")) & alreadyReported)) {
            	// report one out of 16384 (2^14)
            	if (!(Math.round(Math.random() * 16384) == 777)) {
            		doReport = false;
            	}
            }
            	
            if (doReport) {
                report(message, objs);
                // to detect the dupe on next time.
                instance.addReportedCrash(chksumVal, message, crashtime);
            } else {
                if (IS_DEBUG_ENABLED) {
                    Logger.debug("Duplicate!!! already reported - " + message);
                }
            }

            // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
            // Boolean msgVal = preferences.getBoolean("errep."+Long.toString(chksumVal), false);
            // if (!msgVal) {
            // preferences.edit().putBoolean(Long.toString(chksumVal), true).commit();
            // report(message, objs);
            // }
        }
    }

    /**
     * This Method
     * 
     * @param chksum
     * @param message
     * @return
     */
    private boolean isAlreadyReportedCrash(long checksum, String message) {
        String[] projection = new String[] { "COUNT (*) AS error" };
        boolean result =false;
        String where = AcraReports._CHECK_SUM + "=" + checksum;
        if (IS_DEBUG_ENABLED) {
            Logger.debug("isAlreadyReportedCrash():Checksum="+checksum+",msg="+message);
        }
        Cursor c = context.getContentResolver().query(AcraReports.CONTENT_URI, projection, where, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                if (c.getLong(0) > 0) {
                    result = true;
                }
            }
            c.close();
        }
        if (IS_DEBUG_ENABLED) {
            Logger.debug("isAlreadyReportedCrash() return "+ result);
        }
        return result;
    }

    private Uri addReportedCrash(long checksum, String message, long reportTime) {
        ContentValues values = new ContentValues();
        values.put(AcraReports._CHECK_SUM, checksum);
        values.put(AcraReports._ERROR_MSG, message);
        values.put(AcraReports._REPORT_STATUS, AcraReports.REPORT_SENT);
        values.put(AcraReports._REPORT_DATETIME, reportTime);
        Uri u = context.getContentResolver().insert(AcraReports.CONTENT_URI, values);
        if (IS_DEBUG_ENABLED) {
            Logger.debug("Added the crash report in db.uri=" + u + ",msg=" + message);
        }
        return u;
    }

    // in rare situation where we want error reports to go to ACRA in production we should use postErrorToAcra
    // method
    // that method does not require IS_ERROR_ENABLED flag - only the IS_ACRA_ERROR_REPORT_ENABLED flag
    public static void error(Object... objs) {
        if (Logger.IS_ERROR_ENABLED) {
            boolean report = false;

            // if first arg is boolean then interpret it as the report flag
            // Even though we do not use report flag anymore here keeping it as some callers are using it
            if (objs != null && objs.length > 0 && objs[0] instanceof Boolean) {
                report = (Boolean) objs[0];
                final int len = objs.length - 1;
                final Object[] newobjs = new Object[len];
                System.arraycopy(objs, 1, newobjs, 0, len);
                objs = newobjs;
            }

            final String message = formatMessage(objs);
            Log.e(TAG, getHeader(Level.ERROR) + message);
            logLocal(Level.ERROR, message);

            if (Logger.IS_ACRA_ERROR_REPORT_ENABLED && report) {
                report(message, objs);
            }
        }
    }

    private static String getHeader(Level level) {
        return level.toString() + " [" + Thread.currentThread().getName() + "]: ";
    }

    private static String formatMessage(Object... objs) {
        if (objs == null || objs.length == 0) {
            return "";
        }
        final String msg;
        if (objs.length > 1 || objs[0] instanceof Throwable) {
            StringBuilder sb = new StringBuilder();
            boolean delim = false;
            for (Object o : objs) {
                if (delim) {
                    sb.append("\n");
                } else {
                    delim = true;
                }
                if (o instanceof Throwable) {
                    Throwable e = (Throwable) o;
                    do {
                        String emsg = e.getMessage();
                        sb.append(e.getClass().getName());
                        if (emsg != null) {
                            sb.append(": ");
                            sb.append(emsg);
                        }
                        sb.append("\n");
                        for (StackTraceElement ste : e.getStackTrace()) {
                            sb.append("  ");
                            sb.append(ste.toString());
                            sb.append("\n");
                        }
                        e = e.getCause();
                        if (e != null) {
                            sb.append("caused by:\n");
                        }
                    } while (e != null);
                } else if (o instanceof Class) { // prefix message with class name
                    Class<?> cls = (Class<?>) o;
                    String name = cls.getSimpleName();
                    if (name == null || name.length() == 0) {
                        name = cls.getName();
                    }
                    sb.append(name);
                    sb.append(": ");
                    delim = false;
                } else if (o == null) {
                    sb.append("<null>");
                } else {
                    sb.append(o.toString());
                }
            }
            msg = sb.toString();
        } else {
            msg = objs[0].toString();
        }
        return msg;
    }

    private static void report(Class<?> claz, Throwable th) {
        report(claz, th, null);
    }

    private static void report(String message, Object... objs) {
        final Class<?> claz = objs != null && objs.length > 0 && objs[0] instanceof Class ? (Class<?>) objs[0]
                : null;
        // dont need to add version as that is reported separately by ACRA anyway
        // String vers = "";
        // try {
        // Context ctx = getInstance().context;
        // if (ctx != null) {
        // PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
        // vers = info.versionName + "-" + info.versionCode;
        // } else {
        // vers = "ctxNull";
        // }
        // } catch (Exception e) {
        // Log.e(TAG, "Logger.error: error getting package info: " + e);
        // }

        // final String messageReport = vers + " " + Build.MODEL + " " + Build.DEVICE + " " +
        // Build.VERSION.RELEASE + " " + message;
        report(claz, new Exception(message));
    }

    public static void report(Class<?> claz, Throwable th, String msg) {
        if (!IS_ACRA_ERROR_REPORT_ENABLED) {
            return;
        }
        ErrorReporter err = ErrorReporter.getInstance();
        err.putCustomData("logger", "true"); // That the report is from logger
        if (claz != null) {
            err.putCustomData("class", claz.getSimpleName()); // Pertinent class
        }
        if (null != msg) {
            err.putCustomData("message", msg); // Message
        }
        err.handleSilentException(th);
    }

    // use log4j to write rolling log files to internal cache dir
    private void openLocalLog() {
        final LogConfigurator cfg = new LogConfigurator();
        final String dir = context.getCacheDir().getAbsolutePath();
        long free;
        try {
            final StatFs stat = new StatFs(dir);
            final long bs = stat.getBlockSize();
            final long avail = stat.getAvailableBlocks();
            free = avail * bs;
            if (IS_DEBUG_ENABLED) {
                final long total = (long) stat.getBlockCount() * bs;
                Log.i(TAG, "Logger.openLocalLog: " + dir + ": " + free + " / " + total);
            }
        } catch (Exception e) {
            Log.e(TAG, "Logger.openLocalLog: " + e);
            free = MIN_FREE_SPACE + 1; // assume OK
        }
        if (free > MIN_FREE_SPACE) {
            long max = (long) (free * PERCENT_FREE_SPACE);
            if (max > MAX_LOCAL_LOG_SIZE) {
                max = MAX_LOCAL_LOG_SIZE;
            }
            cfg.setMaxFileSize(max / MAX_LOCAL_LOG_FILES);
            cfg.setMaxBackupSize(MAX_LOCAL_LOG_FILES);
            localLogFname = dir + File.separator + LOG_FILE_NAME;
            cfg.setFileName(localLogFname);
            cfg.setFilePattern("%m");
            cfg.setUseLogCatAppender(false);
            cfg.configure();
            localLogger = org.apache.log4j.Logger.getRootLogger();

            headersb = new StringBuilder();
            header = new Formatter(headersb, Locale.US);
        } else {
            Log.e(TAG, "Logger.openLocalLog: " + dir + " has only " + free + " bytes");
        }
    }

    // write to local log file
    private static void logLocal(Level level, String msg) {
        final org.apache.log4j.Logger logger = localLogger;
        if (logger != null) {
            final int pid = Process.myPid();
            final int tid = Process.myTid();
            synchronized (logger) {
                final Calendar cal = Calendar.getInstance();
                final String tag = getHeader(level);
                headersb.setLength(0);
                header.format(LOCAL_HEADER_FMT, cal, pid, tid, tag);
                final String hdr = headersb.toString();
                final StringBuilder sb = new StringBuilder();
                final String[] lines = msg.split("\n");
                for (final String line : lines) {
                    sb.append(hdr);
                    sb.append(line);
                    sb.append("\n");
                }
                logger.log(level, sb.toString());
            }
        }
    }

    /**
     * Concatenate any local log files to the output stream.
     * 
     * @return true if succeeded
     */
    public static boolean writeLocalLog(OutputStream out) {
        return writeLocalLog(out, true);
    }

    public static boolean writeLocalLog(OutputStream out, boolean closeZipStream) {
        boolean ret = true;

        final org.apache.log4j.Logger logger = localLogger;
        if (logger != null) {
            synchronized (logger) {
                InputStream last = null;
                for (int i = MAX_LOCAL_LOG_FILES - 1; i >= 0; --i) {
                    final String fname = i > 0 ? localLogFname + "." + i : localLogFname;
                    InputStream in = null;
                    try {
                        in = new BufferedInputStream(new FileInputStream(fname), 16 * 1024);
                        last = in;
                    } catch (FileNotFoundException e) {
                        if (last != null) {
                            Log.e(TAG, "Logger.writeLocalLog: missing log file " + fname);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Logger.writeLocalLog: error opening " + fname + ": " + e);
                        ret = false;
                    }

                    if (in != null) {
                        try {
                            final byte[] buf = new byte[16 * 1024];
                            int num;
                            while ((num = in.read(buf)) >= 0) {
                                out.write(buf, 0, num);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Logger.writeLocalLog: error writing " + fname + ": " + e);
                            ret = false;
                        } finally {
                            try {
                                in.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        }

        try {
            if (closeZipStream) {
                out.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Logger.writeLocalLog: error closing: " + e);
            ret = false;
        }

        return ret;
    }

    public void exportLog(String dest) {
        try {
            final String src = context.getCacheDir().getAbsolutePath();
            zipDirectory(new File(src), new File(dest));
        } catch (Exception e) {
            Log.e(TAG, "Unable to export the logs");
            Toast.makeText(context, "Error while exporting logs", Toast.LENGTH_LONG).show();
        }

    }

    public final void zipDirectory(File directory, File zip) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
        zip(directory, directory, zos);
        zos.close();
    }

    private final void zip(File directory, File base, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                if (filename.endsWith("log")) {
                    return true;
                }
                return false;
            }
        });
        byte[] buffer = new byte[8192];
        int read = 0;
        for (int i = 0, n = files.length; i < n; i++) {
            if (files[i].isDirectory()) {
                zip(files[i], base, zos);
            } else {
                FileInputStream in = new FileInputStream(files[i]);
                ZipEntry entry = new ZipEntry(files[i].getPath().substring(base.getPath().length() + 1));
                zos.putNextEntry(entry);
                while (-1 != (read = in.read(buffer))) {
                    zos.write(buffer, 0, read);
                }
                in.close();
            }
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see java.io.FileFilter#accept(java.io.File)
     */
    @Override
    public boolean accept(File pathname) {
        if (pathname.getName().endsWith("log")) {
            return true;
        }
        return false;
    }
    
    
    public static String dumpIntent(Intent intent, String prefix) {
        if (intent == null) {
            return "null";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(intent.toString());
        final Bundle b = intent.getExtras();
        if (b != null) {
            sb.append(":\n");
            for (String key : b.keySet()) {
                if (prefix != null) {
                    sb.append(prefix);
                }
                sb.append(key);
                sb.append(" = ");
                sb.append(b.get(key));
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
