package com.app.cameratag.util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BaseLog {
    // 用于格式化日期,作为日志文件名的一部分
    protected static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss");

    protected static int MAX_LOG_COUNT = 10;

    private static final String MSG_FORMATTER = "Time:%s  TimeStamp:%s  tid:%s  Tag:%s  Msg:%s \n%s\n\n";

    protected static String getExceptionCauseMsg(Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        return writer.toString();
    }

    protected String getDeviceInfo(Context ctx, String tag) {
        StringBuffer sb = new StringBuffer();
        try {
            String versionName = "temp";
            sb.append("versionName = " + versionName + "\n");
        } catch (Exception e) {

        }

        // 获得环境信息
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                sb.append(field.getName() + " = " + field.get(null).toString() + "\n");
            } catch (Exception e) {
                Log.e(tag, "an error occured when collect crash info", e);
            }
        }
        return sb.toString();
    }

    protected static String getFormatterMsg(String tag, String extraMsg,
                                            String exceptionMsg) {
        long timestamp = System.currentTimeMillis();
        String time = TIME_FORMATTER.format(new Date());

        long threadId = Thread.currentThread().getId();

        return String.format(MSG_FORMATTER, time, timestamp, threadId, tag, extraMsg, exceptionMsg);
    }

    protected static void writeToFile(String logDir, String fileName, String tag, String exceptionMsg) {
        try {
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                File dir = new File(logDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                FileOutputStream fos = new FileOutputStream(logDir + fileName,
                        true);
                fos.write(exceptionMsg.getBytes());
                fos.flush();
                fos.close();
            }
        } catch (Exception e) {
            Log.e(tag, "an error occured while writing file...", e);
        }
    }

    protected static void keepLogCount(String dirPath, int count) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files != null && files.length > count) {
            int index = 0;
            long min = files[0].lastModified();
            for (int i = 0; i < files.length; ++i) {
                long lastModified = files[i].lastModified();
                if (lastModified < min) {
                    min = lastModified;
                    index = i;
                }
            }
            files[index].delete();
        }
    }
}
