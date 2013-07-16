package com.verizon.mms.util;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

/**
 * A holder class that contains cache parameters.
 */
public class ImageCacheParams {
	//Default disk cache size
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 8; // 10MB

    // Compression settings when writing images to disk cache
    private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 90;
    public static final int DISK_CACHE_INDEX = 0;
	private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;
    private static final boolean DEFAULT_CLEAR_DISK_CACHE_ON_START = false;
    private static final boolean DEFAULT_INIT_DISK_CACHE_ON_CREATE = false;
    
    
    
    public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
    public File diskCacheDir;
    public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
    public int compressQuality = DEFAULT_COMPRESS_QUALITY;
    public boolean diskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;
    public boolean clearDiskCacheOnStart = DEFAULT_CLEAR_DISK_CACHE_ON_START;
    public boolean initDiskCacheOnCreate = DEFAULT_INIT_DISK_CACHE_ON_CREATE;

    public ImageCacheParams(Context context, String uniqueName) {
        diskCacheDir = getDiskCacheDir(context, uniqueName);
    }

    public ImageCacheParams(File diskCacheDir) {
        this.diskCacheDir = diskCacheDir;
    }

    

    public static int getMemoryClass(Context context) {
        return ((ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE)).getMemoryClass();
    }
    

/**
 * Get a usable cache directory (external if available, internal otherwise).
 *
 * @param context The context to use
 * @param uniqueName A unique directory name to append to the cache dir
 * @return The cache dir
 */
public static File getDiskCacheDir(Context context, String uniqueName) {
    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
    // otherwise use internal cache dir
    final String cachePath =
            Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) &&
                    !isExternalStorageRemovable() ? getExternalCacheDir(context).getPath() :
                            context.getCacheDir().getPath();

    return new File(cachePath + File.separator + uniqueName);
}

/**
 * A hashing method that changes a string (like a URL) into a hash suitable for using as a
 * disk filename.
 */
public static String hashKeyForDisk(String key) {
    String cacheKey;
    try {
        final MessageDigest mDigest = MessageDigest.getInstance("MD5");
        mDigest.update(key.getBytes());
        cacheKey = bytesToHexString(mDigest.digest());
    } catch (NoSuchAlgorithmException e) {
        cacheKey = String.valueOf(key.hashCode());
    }
    return cacheKey;
}

private static String bytesToHexString(byte[] bytes) {
    // http://stackoverflow.com/questions/332079
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bytes.length; i++) {
        String hex = Integer.toHexString(0xFF & bytes[i]);
        if (hex.length() == 1) {
            sb.append('0');
        }
        sb.append(hex);
    }
    return sb.toString();
}

/**
 * Get the size in bytes of a bitmap.
 * @param bitmap
 * @return size in bytes
 */
@TargetApi(12)
public static int getBitmapSize(Bitmap bitmap) {
    /*if (Utils.hasHoneycombMR1()) {
        return bitmap.getByteCount();
    }*/
    // Pre HC-MR1
    return bitmap.getRowBytes() * bitmap.getHeight();
}

/**
 * Check if external storage is built-in or removable.
 *
 * @return True if external storage is removable (like an SD card), false
 *         otherwise.
 */
@TargetApi(9)
public static boolean isExternalStorageRemovable() {
    /*if (Utils.hasGingerbread()) {
        return Environment.isExternalStorageRemovable();
    }*/
    return true;
}

/**
 * Get the external app cache directory.
 *
 * @param context The context to use
 * @return The external cache dir
 */
public static File getExternalCacheDir(Context context) {
    return context.getExternalCacheDir();
}

/**
 * Check how much usable space is available at a given path.
 *
 * @param path The path to check
 * @return The space available in bytes
 */
@TargetApi(9)
public static long getUsableSpace(File path) {
    /*if (Build.VERSION.SDK_INT >= 9) {
        return path.getUsableSpace();
    }*/
    final StatFs stats = new StatFs(path.getPath());
    return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
}
}

