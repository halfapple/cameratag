package com.app.cameramerge.util;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;

public class CrashLogHandler extends BaseLog implements
        UncaughtExceptionHandler {
    private static final String TAG = CrashLogHandler.class.getName();
    private static String CRASH_PATH;

    private static final String FILE_NAME_FORMATTER = "%s-%s-pid(%s).txt";

    private static Context ctx;
    private static CrashLogHandler INSTANCE = new CrashLogHandler();

    private UncaughtExceptionHandler mDefaultHandler;

    private CrashLogHandler() {
    }

    public static CrashLogHandler getInstance() {
        return INSTANCE;
    }

    public void init(Context context) {
        ctx = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        CRASH_PATH = "/sdcard/" + context.getPackageName() + "/crash/";
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        boolean isHandle = false;
        try {
            isHandle = handleException(ex);

            // 如果用户没有处理则让系统默认的异常处理器来处理
            if (!isHandle && mDefaultHandler != null) {
                mDefaultHandler.uncaughtException(thread, ex);
            }
        } finally {
            // 处理未捕获异常后 退出
            if (isHandle) {
                // todo App.exit
                // mx_android.exit(1);
            }
        }
    }

    public boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }

        try {
            saveCrashInfo2File(ex);
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Log.e(TAG, "error : ", e);
        }
        return true;
    }

    private void saveCrashInfo2File(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(getDeviceInfo(ctx, TAG));
        sb.append("\n");

        String excpetionMsg = getExceptionCauseMsg(ex);
        String formatterMsg = getFormatterMsg(TAG, "", excpetionMsg);
        sb.append(formatterMsg);

        int pid = Process.myPid();
        long timestamp = System.currentTimeMillis();
        String time = TIME_FORMATTER.format(new Date());
        String fileName = String.format(FILE_NAME_FORMATTER, time, timestamp,
                pid);

        writeToFile(CRASH_PATH, fileName, TAG, sb.toString());
        keepLogCount(CRASH_PATH, MAX_LOG_COUNT);
    }
}
