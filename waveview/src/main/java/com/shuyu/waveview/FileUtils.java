package com.shuyu.waveview;

import android.os.Environment;

import java.io.File;

/**
 * Created by shuyu on 2016/11/15.
 * 文件管理
 */

public class FileUtils {
    private static final String SD_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final String DATA_PATH = Environment.getDataDirectory().getPath();
    private static final String SD_STATE = Environment.getExternalStorageState();
    public static final String NAME = "audioWave";

    public static String getAppPath() {
        StringBuilder sb = new StringBuilder();
        if (SD_STATE.equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            sb.append(SD_PATH);
        } else {
            sb.append(DATA_PATH);
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
                for (String path : filePaths) {
                    deleteFile(filePath + File.separator + path);
                }
                file.delete();
            }
        }
    }
}
