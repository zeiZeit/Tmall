package com.bitbeyond.tmall;

import android.content.Context;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

/**
 * Created by luyufa on 2017/9/27.
 */

public class MobileSwitch {


    public static void open(MainActivity context){
        setMobileDataState(context,true);

    }

    public static void close(MainActivity context){
        setMobileDataState(context,false);

    }


    private static void setMobileDataState(Context cxt, boolean mobileDataEnabled) {
        TelephonyManager telephonyService = (TelephonyManager) cxt.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method setMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
            if (null != setMobileDataEnabledMethod)
            {
                setMobileDataEnabledMethod.invoke(telephonyService, mobileDataEnabled);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean getMobileDataState(Context cxt) {
        TelephonyManager telephonyService = (TelephonyManager) cxt.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method getMobileDataEnabledMethod = telephonyService.getClass().getDeclaredMethod("getDataEnabled");
            if (null != getMobileDataEnabledMethod)
            {
                boolean mobileDataEnabled = (Boolean) getMobileDataEnabledMethod.invoke(telephonyService);
                return mobileDataEnabled;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
