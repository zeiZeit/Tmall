package com.bitbeyond.tmall;

import android.os.Parcelable;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Created by Sean on 2017/3/9.
 * RootShell
 */

public final class RootShell {
    private static final String TAG = RootShell.class.getSimpleName();
    private static final String HOOK_ROOT_CMD = "echo \"rootOK\"\n";
    private static RootShell mInstance;
    private OutputStream mOutput;
    private Process mProcess;


    private RootShell() {
        this.mOutput = null;
        this.mProcess = null;
        requestRoot();
    }

    /**
     * get a instance
     */
    public static RootShell open() {
        if (mInstance == null) {
            mInstance = new RootShell();
        }
        return mInstance;
    }


    /**
     * execute shell cmd
     *
     * @param command shell command
     */
    public void execute(String command) {
        try {
            this.mOutput.write(command.getBytes());
            this.mOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * request root permission
     */
    private void requestRoot() {
        try {
            this.mProcess = Runtime.getRuntime().exec("su \n");
            this.mOutput = this.mProcess.getOutputStream();
            this.mOutput.write(HOOK_ROOT_CMD.getBytes());
            this.mOutput.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
