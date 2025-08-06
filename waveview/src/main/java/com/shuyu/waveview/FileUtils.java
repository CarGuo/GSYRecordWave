package com.shuyu.waveview;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by shuyu on 2016/11/15.
 * 文件管理
 */

public class FileUtils {
    public static final String NAME = "audioWave";

    /**
     * Get app-specific external storage directory (no permissions required for API 19+)
     * Falls back to internal storage if external storage is unavailable
     */
    public static String getAppPath(Context context) {
        StringBuilder sb = new StringBuilder();
        
        // Use app-specific external storage (no permissions required on API 19+)
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir != null && Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            sb.append(externalFilesDir.getPath());
        } else {
            // Fall back to internal storage
            sb.append(context.getFilesDir().getPath());
        }
        
        sb.append(File.separator);
        sb.append(NAME);
        sb.append(File.separator);
        
        // Create directory if it doesn't exist
        File dir = new File(sb.toString());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        return sb.toString();
    }

    /**
     * @deprecated Use getAppPath(Context) instead
     */
    @Deprecated
    public static String getAppPath() {
        // Legacy method - tries to use external storage
        StringBuilder sb = new StringBuilder();
        String sdState = Environment.getExternalStorageState();
        if (sdState.equals(Environment.MEDIA_MOUNTED)) {
            // On API 30+, this will only work with proper permissions or for app-specific directories
            sb.append(Environment.getExternalStorageDirectory().getPath());
        } else {
            sb.append(Environment.getDataDirectory().getPath());
        }
        sb.append(File.separator);
        sb.append(NAME);
        sb.append(File.separator);
        return sb.toString();
    }

    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
            } else {
                String[] filePaths = file.list();
                if (filePaths != null) {
                    for (String path : filePaths) {
                        deleteFile(filePath + File.separator + path);
                    }
                }
                file.delete();
            }
        }
    }
}
