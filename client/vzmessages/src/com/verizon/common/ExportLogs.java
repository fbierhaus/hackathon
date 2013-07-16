/**
 * ExportLogs.java
 *
 * Version 1.0 
 *
 * Copyright (c) 2008-2013 Strumsoft. All rights reserved.
 * Strumsoft company confidential. This source code is an unpublished work.
 */
package com.verizon.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nbisdk.su;



import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import com.strumsoft.android.commons.logger.Logger;
import com.verizon.messaging.vzmsgs.AppSettings;
import com.verizon.messaging.vzmsgs.ApplicationSettings;
import com.verizon.mms.ContentType;
import com.verizon.mms.util.Util;

/**
 * This class/interface
 * 
 * @author Jegadeesan
 * @Since Feb 18, 2013
 */
public class ExportLogs extends AsyncTask<Void, String, Exception> {
    private static final String VMA_SUPPORT_EMAIL_ID = "vmaandroid_support@strumsoft.com";
    private Context context;
    private String desDirectory;
    private ProgressDialog dialog;
    String mdn;
    String subject;
    String deviceType;
    AppSettings settings;

    /**
     * 
     * Constructor
     * 
     * @param subject
     */
    public ExportLogs(Context context, String subject) {
        this.context = context;
        this.settings = ApplicationSettings.getInstance(context);
        mdn = settings.getMDN();
        if (mdn == null) {
            mdn = VZUris.isTabletDevice() ? "nomdn-" : ApplicationSettings.parseAdddressForChecksum(settings
                    .getLocalPhoneNumber()) + "-";
        }
        settings = ApplicationSettings.getInstance();
        deviceType = VZUris.isTabletDevice() ? "tablet" : "handset";
        this.subject = subject;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.os.AsyncTask#onPreExecute()
     */
    @Override
    protected void onPreExecute() {
    	if(Logger.IS_DEBUG_ENABLED){
        Logger.debug("onPreExecute() ");
    	}
        dialog = ProgressDialog.show(context, null, "Exporting logs...");
        String path = null;
        if (context.getExternalCacheDir() != null) {
            path = context.getExternalCacheDir().getAbsolutePath();
        } else {
            path = context.getCacheDir().getAbsolutePath();
            Toast.makeText(context, "External directory not found. saving on phone memory.path=:" + path,
                    Toast.LENGTH_SHORT).show();
        }
        desDirectory = path + "/" + mdn + "-" + deviceType + "vzm-debug-logs.zip";
        super.onPreExecute();
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(Exception result) {
        super.onPostExecute(result);
        dialog.dismiss();
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(ContentType.TEXT_HTML);
        i.putExtra(Intent.EXTRA_EMAIL, new String[] { VMA_SUPPORT_EMAIL_ID });
        i.putExtra(Intent.EXTRA_SUBJECT, (subject!=null && subject.length() > 80)?subject.substring(0,80):subject);
        StringBuilder builder = new StringBuilder();
        builder.append("<P>");
        builder.append("<hr width=60%><Br>");
        builder.append("Issue        :" + subject + "<Br>");
        builder.append("MDN          :" + ApplicationSettings.getInstance(context).getMDN() + "<Br>");
        builder.append("Login Token  :" + ApplicationSettings.getInstance(context).getDecryptedLoginToken()
                + "<Br>");
        builder.append("<hr width=60%><Br>");
        builder.append("Model        :" + android.os.Build.MODEL + "<Br>");
        builder.append("MANUFACTURER :" + android.os.Build.MANUFACTURER + "<Br>");
        builder.append("PRODUCT      :" + android.os.Build.PRODUCT + "<Br>");
        builder.append("SDK Version  :" + Build.VERSION.SDK_INT + "<Br>");
        builder.append("RAM          :" + getTotalRAM() + "<Br>");

        builder.append("SDK          :" + Build.VERSION.SDK + "<Br>");
        builder.append("CODE NAME    :" + Build.VERSION.CODENAME + "<Br>");
        builder.append("OS VERSION   :" + Build.VERSION.INCREMENTAL + "<Br>");
        builder.append("OS RELEASE   :" + Build.VERSION.RELEASE + "<Br>");
        builder.append("OS USER      :" + Build.USER + "<Br>");
        builder.append("Log file     :" + desDirectory + "<Br>");
        builder.append("<hr>");
        builder.append("Current Build No:" + ApplicationSettings.getInstance(context).getCurrentBuildNo() + "<Br>");
        builder.append("Last Build    No:" + ApplicationSettings.getInstance(context).getLastBuildNo()+ "<Br>");
        builder.append("<hr>");
        builder.append("<hr width=60%><Br>");
        i.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(builder.toString()));
        i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + desDirectory));
        try {
            context.startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(context, "Exporting logs", Toast.LENGTH_LONG).show();
    }

    public String getTotalRAM() {
        RandomAccessFile reader = null;
        String load = null;
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            load = reader.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return load;
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.os.AsyncTask#onProgressUpdate(Progress[])
     */
    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        String msg = values[0];
        dialog.setMessage(msg);
    }

    private final void export(File zipFileName) throws IOException {
        ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipFileName));
        try {

            // dump logcat
            publishProgress("Copying logcat logs.");
            exportLogcat(mdn + "-" + deviceType + "-logcat.txt", zipStream);

            // dump the logs
            publishProgress("Copying logs ...");
            Util.saveLocalLog(mdn + "-logs.txt", zipStream);

            // dump db
            publishProgress("Copying encrypted dumpdb.");
            Util.saveMessageDb(context, mdn + "-" + deviceType + "-dump-db.txt", zipStream);

            // email trace
            publishProgress("Copying crash trash...");
            final String fname = "/data/anr/traces.txt";
            File anrTrace = new File(fname);
            if (anrTrace.exists()) {
                if(Logger.IS_DEBUG_ENABLED){
            	Logger.debug("Traces found adding to zip file");
                }
                zip(anrTrace, zipStream);
            } else {
                publishProgress("No crash trash found.");
                if(Logger.IS_DEBUG_ENABLED){
                Log.e("EXPORTLOGS", "No trace file found");
                }
            }

        } catch (Exception e) {
            Log.e("EXPORTLOGS", "Unable to export the logs");
            publishProgress("Unable to dump logs.error=" + e.getMessage());
        } finally {
            zipStream.close();
        }

    }

    private final void zip(File fileOrDirectory, ZipOutputStream zos) throws IOException {
        byte[] buffer = new byte[8192];
        int read = 0;
        if (fileOrDirectory.isFile()) {
            FileInputStream in = new FileInputStream(fileOrDirectory);
            ZipEntry entry = new ZipEntry(fileOrDirectory.getName());
            zos.putNextEntry(entry);
            while (-1 != (read = in.read(buffer))) {
                zos.write(buffer, 0, read);
            }
            in.close();
        } else {
            File[] files = fileOrDirectory.listFiles();
            for (int i = 0, n = files.length; i < n; i++) {
                if (files[i].isDirectory()) {
                    zip(files[i], zos);
                } else {
                    FileInputStream in = new FileInputStream(files[i]);
                    ZipEntry entry = new ZipEntry(files[i].getName());
                    zos.putNextEntry(entry);
                    while (-1 != (read = in.read(buffer))) {
                        zos.write(buffer, 0, read);
                    }
                    in.close();
                }
            }
        }
    }

    private void exportLogcat(String filename, ZipOutputStream out) {
    	if(Logger.IS_DEBUG_ENABLED){
        Logger.debug("exportLogCatLogs" + filename);
    	}
        Process process = null;
        try {
            ZipEntry entry = new ZipEntry(filename);
            // process = Runtime.getRuntime().exec(new String[] { "logcat", "-d" });
            process = Runtime.getRuntime().exec(
                    new String[] { "logcat", "-d", "-v", "thread", "-v", "threadtime" });
            InputStream in = process.getInputStream();
            out.putNextEntry(entry);
            byte[] buffer = new byte[8192];
            int read = 0;
            while (-1 != (read = in.read(buffer))) {
                out.write(buffer, 0, read);
            }
            in.close();

        } catch (IOException e) {
            Log.e("EXPORTLOGS", "ERROR", e);
        } finally {
            Logger.debug("exportLogCatLogs done.");
        }
    }

    /*
     * Overriding method (non-Javadoc)
     * 
     * @see android.os.AsyncTask#doInBackground(Params[])
     */
    @Override
    protected Exception doInBackground(Void... params) {
        try {
            export(new File(desDirectory));
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("EXPORTLOGS", "ERROR", e);
        }
        return null;
    }

}
