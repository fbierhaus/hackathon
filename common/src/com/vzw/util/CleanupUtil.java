/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vzw.util;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.apache.log4j.Logger;

/**
 *
 * @author hud
 */
public class CleanupUtil {
	
	private static final Logger				logger = Logger.getLogger(CleanupUtil.class);

	public static void release(Object...objs) {
		for (Object obj : objs) {
			if (obj != null) {
				if (obj instanceof Reader) {
					try {
						((Reader)obj).close();
					}
					catch (IOException e) {
						LogUtil.error(logger, e, "Failed to close reader");
					}
				}
				else if (obj instanceof Writer) {
					try {
						((Writer)obj).close();
					}
					catch (IOException e) {
						LogUtil.error(logger, e, "Failed to close Writer");
					}
				}
				else if (obj instanceof InputStream) {
					try {
						((InputStream)obj).close();
					}
					catch (IOException e) {
						LogUtil.error(logger, e, "Failed to close input stream");
					}
				}
				else if (obj instanceof OutputStream) {
					try {
						((OutputStream)obj).close();
					}
					catch (IOException e) {
						LogUtil.error(logger, e, "Failed to close output stream");
					}
				}
				else if (obj instanceof Statement) {
					try {
						((Statement)obj).close();
					}
					catch (SQLException e) {
						LogUtil.error(logger, e, "Failed to close statement");
					}
				}
				else if (obj instanceof Connection) {
					try {
						((Connection)obj).close();
					}
					catch (SQLException e) {
						LogUtil.error(logger, e, "Failed to close connection");
					}
				}
				else if (obj instanceof ResultSet) {
					try {
						((ResultSet)obj).close();
					}
					catch (SQLException e) {
						LogUtil.error(logger, e, "Failed to close result set");
					}
				}
				else if (obj instanceof ImageInputStream) {
					try {
						((ImageInputStream)obj).close();
					}
					catch (IOException e) {
						LogUtil.error(logger, e, "Failed to close ImageInputStream");
					}
				}
				else if (obj instanceof ImageOutputStream) {
					try {
						((ImageOutputStream)obj).close();
					}
					catch (IOException e) {
						LogUtil.error(logger, e, "Failed to close ImageOutputStream");
					}
				}
				else if (obj instanceof ImageReader) {
					((ImageReader)obj).dispose();
				}
				else if (obj instanceof ImageWriter) {
					((ImageWriter)obj).dispose();
				}
				else {
					LogUtil.error(logger, "Unsupported object for cleanup: {0}", obj);
				}
			}
		}
	}

}
