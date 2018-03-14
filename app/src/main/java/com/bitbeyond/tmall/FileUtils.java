package com.bitbeyond.tmall;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import static android.content.ContentValues.TAG;

public class FileUtils {
    private static String mPath = Environment.getExternalStorageDirectory().toString() + "/WeChatHook";


    public FileUtils() {
        File file = new File(mPath);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    public File createFile(String saveneme) {
        File file = new File(mPath, saveneme);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    /***
     * 获取手机内存剩余百分比
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static double getMennoryRate()

    {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        double memoryRate = (availableBlocks * blockSize) / (double) (totalBlocks * blockSize);
        if (memoryRate < 0.50) {
            delete();
        }
        Log.i(TAG, "Mennory_Left_Rate is:---" + memoryRate * 100.0 + "%");
        return memoryRate;
    }

    public static void delete() {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File mWechatLog = new File(externalStorageDirectory, "tencent/MicroMsg/xlog");//Tencent    tencent
        deleteDir(mWechatLog);
    }

    public static void deleteDir(File dir) {
        if (dir.isDirectory() && dir.exists()) {
            File[] listFiles = dir.listFiles();
            for (int i = 0; i < listFiles.length; i++) {
                if (listFiles[i].isFile() && listFiles[i].exists()) {
                    listFiles[i].delete();
                }
                if (listFiles[i].isDirectory() && listFiles[i].exists()) {
                    deleteDir(listFiles[i]);
                }
            }
        }
    }
}
