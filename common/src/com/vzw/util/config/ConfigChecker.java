/*
 * $Author: linj $
 * $Date: 2007/08/09 17:13:17 $
 * $Revision: 1.1 $
 * $Source: /cvs_repository/shared/src/ConfigTool/src/com/vzw/config/ConfigChecker.java,v $
 * $Name:  $
 */
package com.vzw.util.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This class is used to compare existing runtime configuration files and params
 * with the tokenized configuration files and params.
 * 
 */
public class ConfigChecker {


    public static void main(String[] args) {
        if (args.length<2) {
            System.out.println("Usage: ConfigChecker TOKENIZED_CONFIG_DIR RUNTIME_CONFIG_DIR");
            System.exit(0);
        }

        boolean success = true;
        
        try {
            success = compareConfigDir(args[0], args[1]);
        } catch (Exception e) {
            System.err.println("===== Couldn't check runtime config against tokenized config.");
            e.printStackTrace();
            success = false;
        }
        
        if (success) {
            System.out.print("success");
            System.exit(0);
        } else {
            System.out.print("failure");
            System.exit(1);
        }
    }
    
    public static boolean compareConfigDir(String dir1, String dir2) throws Exception {
        boolean success = true;
        File tokenizedDir = new File(dir1);
        File[] tokenizedFiles = tokenizedDir.listFiles();
        for (int i=0; i<tokenizedFiles.length; i++) {
            File runtimeFile = new File(dir2+File.separator+tokenizedFiles[i].getName());
            if (!runtimeFile.exists()) {
                System.err.println("===== File " + tokenizedFiles[i].getName() + " doesn't exist in folder " + dir2);
                success = false;
            }
            success = success && compareConfigFile(tokenizedFiles[i], runtimeFile);           
        }
        return success;
    }
    
    public static boolean compareConfigFile(File file1, File file2) throws Exception {
        boolean success = true;
        Properties tokenizedProp = new Properties();
        tokenizedProp.load(new FileInputStream(file1));
        Properties runtimeProp = new Properties();
        runtimeProp.load(new FileInputStream(file2));
        Enumeration propNames = tokenizedProp.propertyNames();
        while(propNames.hasMoreElements()) {
            String key = (String)propNames.nextElement();
            String prop = runtimeProp.getProperty(key);
            if (prop==null && key.indexOf("{T}")==-1) {
                System.err.println("===== Property " + key + " doesn't exist in file " + file2.getAbsoluteFile());
                success = false;
            }
        }
        return success;
    }

}
