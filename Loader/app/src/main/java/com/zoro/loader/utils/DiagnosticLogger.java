package com.zoro.loader.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Persistent diagnostics for Loader and BlackBox startup/launch failures. */
public final class DiagnosticLogger {
    private static final String TAG = "ZoroLoader";
    private static final long MAX_LOG_BYTES = 1024L * 1024L;
    private static final Object LOCK = new Object();
    private static volatile File logFile;

    private DiagnosticLogger() {
    }

    public static void init(Context context) {
        synchronized (LOCK) {
            File dir = new File(context.getFilesDir(), "diagnostics");
            if (!dir.exists() && !dir.mkdirs()) {
                Log.e(TAG, "Unable to create diagnostics directory: " + dir);
            }
            logFile = new File(dir, "loader.log");
        }
        log("INFO", "Logger initialized. package=" + context.getPackageName() + ", version=" + getVersion(context));
    }

    public static File getLogFile(Context context) {
        if (logFile == null) {
            init(context.getApplicationContext());
        }
        return logFile;
    }

    public static void log(String level, String message) {
        String safeMessage = message == null ? "null" : message;
        Log.println(toPriority(level), TAG, safeMessage);
        File file = logFile;
        if (file == null) {
            return;
        }
        synchronized (LOCK) {
            try {
                rotateIfNeeded(file);
                BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
                writer.write(timestamp + " [" + level + "] " + safeMessage);
                writer.newLine();
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to write diagnostic log", e);
            }
        }
    }

    public static void exception(String where, Throwable throwable) {
        if (throwable == null) {
            log("ERROR", where + ": null throwable");
            return;
        }
        StringWriter trace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(trace));
        log("ERROR", where + ":\n" + trace);
    }

    private static int toPriority(String level) {
        if ("ERROR".equals(level)) return Log.ERROR;
        if ("WARN".equals(level)) return Log.WARN;
        if ("DEBUG".equals(level)) return Log.DEBUG;
        return Log.INFO;
    }

    private static void rotateIfNeeded(File file) {
        if (file.exists() && file.length() > MAX_LOG_BYTES) {
            File backup = new File(file.getParentFile(), "loader.log.1");
            if (backup.exists()) {
                //noinspection ResultOfMethodCallIgnored
                backup.delete();
            }
            //noinspection ResultOfMethodCallIgnored
            file.renameTo(backup);
        }
    }

    private static String getVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            return "unknown";
        }
    }
}
