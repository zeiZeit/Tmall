package com.bitbeyond.tmall;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

/**
 * Created by yufa on 2018/1/18.
 */

public class BootReceiver extends BroadcastReceiver {


    private static final int UNLOCK = 0x101;
    private static final int START = 0x102;

    private String TAG = getClass().getSimpleName();

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UNLOCK: {

                    Context context = (Context) msg.obj;

                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                    PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "Start Screen");
                    KeyguardManager.KeyguardLock keyguardLock = km.newKeyguardLock("Start Key");
                    Log.i(TAG, "开始解锁");
                    wakeLock.acquire();
                    keyguardLock.disableKeyguard();

                    Message message = new Message();
                    message.what = START;
                    message.obj = context;
                    handler.sendMessageDelayed(message, 5 * 1000);
                    break;
                }
                case START: {
                    Context context = (Context) msg.obj;
                    Intent intent = new Intent();
                    intent.setClassName("com.bitbeyond.tmall", "com.bitbeyond.tmall.ConfigActivity");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    break;
                }
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "监测到开机广播");
        Message message = new Message();
        message.what = UNLOCK;
        message.obj = context;
        handler.sendMessageDelayed(message, 15 * 1000);
    }
}
