package com.bitbeyond.tmall;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;

public class Util {
    private static String TAG = "Util";

    public static String getDeviceID(Context context) {

        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String DEVICE_ID = tm.getDeviceId();
        return DEVICE_ID;
    }

    public static String loadFileAsString(String fileName) throws Exception {
        FileReader reader = new FileReader(fileName);
        String text = loadReaderAsString(reader);
        reader.close();
        return text;
    }

    public static String loadReaderAsString(Reader reader) throws Exception {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int readLength = reader.read(buffer);
        while (readLength >= 0) {
            builder.append(buffer, 0, readLength);
            readLength = reader.read(buffer);
        }
        return builder.toString();
    }



    private static String[] sign(String str, int i) {
        int length = ((str.length() + i) - 1) / i;
        String[] strArr = new String[length];
        int i2 = 0;
        while (i2 < length) {
            strArr[i2] = str.substring(i2 * i, (i2 == length + -1 ? str.length() - (i2 * i) : i) + (i2 * i));
            i2++;
        }
        return strArr;
    }

    public static float getBattery(Context cx) {
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = cx.registerReceiver(null, batteryFilter);
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (float) scale;
    }

    /**
     * 获取当前电池温度
     **/
    public static int getTemperature(Context cx) {
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = cx.registerReceiver(null, batteryFilter);
        int temperature=intent.getIntExtra("temperature",0) / 10;
        return temperature;
    }


    public static String getcpu(Context cx) {
        String path = "/proc/stat";// 系统CPU信息文件
        long totalJiffies[]=new long[2];
        long totalIdle[]=new long[2];
        int firstCPUNum=0;//设置这个参数，这要是防止两次读取文件获知的CPU数量不同，导致不能计算。这里统一以第一次的CPU数量为基准
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        Pattern pattern= Pattern.compile(" [0-9]+");
        for(int i=0;i<2;i++) {
            totalJiffies[i]=0;
            totalIdle[i]=0;
            try {
                fileReader = new FileReader(path);
                bufferedReader = new BufferedReader(fileReader, 8192);
                int currentCPUNum=0;
                String str;
                while ((str = bufferedReader.readLine()) != null&&(i==0||currentCPUNum<firstCPUNum)) {
                    if (str.toLowerCase().startsWith("cpu")) {
                        currentCPUNum++;
                        int index = 0;
                        Matcher matcher = pattern.matcher(str);
                        while (matcher.find()) {
                            try {
                                long tempJiffies = Long.parseLong(matcher.group(0).trim());
                                totalJiffies[i] += tempJiffies;
                                if (index == 3) {//空闲时间为该行第4条栏目
                                    totalIdle[i] += tempJiffies;
                                }
                                index++;
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(i==0){
                        firstCPUNum=currentCPUNum;
                        try {//暂停50毫秒，等待系统更新信息。
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        double rate=-1;
        if (totalJiffies[0]>0&&totalJiffies[1]>0&&totalJiffies[0]!=totalJiffies[1]){
            rate=100.0*((totalJiffies[1]-totalIdle[1])-(totalJiffies[0]-totalIdle[0]))/(totalJiffies[1]-totalJiffies[0]);
        }

        return (int)rate + "";
    }

    /**
     * 获取android当前可用内存大小
     */
    public static String getAvailMemory(Context cx) {
        ActivityManager am = (ActivityManager) cx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        long availMem = mi.availMem / (1024 * 1024);
        Log.i(TAG, "Available_Memory is--- " + String.valueOf(availMem) + "MB");
        return String.valueOf(availMem) + "MB";
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

    private static void delete() {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File mWechatLog = new File(externalStorageDirectory, "tencent/MicroMsg/xlog");//Tencent    tencent
        deleteDir(mWechatLog);
    }

    private static void deleteDir(File dir) {
        if (dir.isDirectory() && dir.exists()) {
            File[] listFiles = dir.listFiles();
            if (listFiles != null) {
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

    public static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }
    public static String md5(String string) {
        if (string.isEmpty()) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String sign(String str, String str2) {
        try {
            RSAPublicKey rSAPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(str2, 2)));
            Log.i(TAG, "rSAPublicKey value is : " + KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(str2, 2))).toString());
            String[] a = sign(str, 39);
            StringBuilder stringBuilder = new StringBuilder();
            Cipher instance = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            instance.init(1, rSAPublicKey);
            for (String bytes : a) {
                stringBuilder.append(Base64.encodeToString(instance.doFinal(bytes.getBytes("UTF-8")), 2));

            }
            Log.i(TAG, "stringBuilder value is :" + stringBuilder.toString());
            return stringBuilder.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
