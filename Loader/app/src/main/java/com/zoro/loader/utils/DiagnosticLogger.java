package com.zoro.loader.utils;

import android.content.Context;
import android.os.Environment;
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
    private static volatile File publicLogFile;

    private DiagnosticLogger() {
    }

    public static void init(Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            File dir = new File(appContext.getFilesDir(), "diagnostics");
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
            logFile = new File(dir, "loader.log");

            // The Loader targets an Android version where this public Downloads path
            // remains writable after WRITE_EXTERNAL_STORAGE is granted. Keeping a
            // second copy makes the log available even when a virtual process dies.
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloads.exists()) {
                //noinspection ResultOfMethodCallIgnored
                downloads.mkdirs();
            }
            publicLogFile = new File(downloads, "com.zoro.loader_diagnostic.log");
        }
        log("INFO", "Logger initialized. package=" + appContext.getPackageName()
                + ", private=" + logFile.getAbsolutePath()
                + ", public=" + publicLogFile.getAbsolutePath()
                + ", version=" + getVersion(appContext));
    }

    public static File getLogFile(Context context) {
        if (logFile == null) {
            init(context.getApplicationContext());
        }
        return logFile;
    }

    public static File getPublicLogFile(Context context) {
        if (publicLogFile == null) {
            init(context.getApplicationContext());
        }
        return publicLogFile;
    }

    public static void log(String level, String message) {
        String safeMessage = message == null ? "null" : message;
        Log.println(toPriority(level), TAG, safeMessage);
        synchronized (LOCK) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            String line = timestamp + " [" + level + "] " + safeMessage;
            append(logFile, line);
            append(publicLogFile, line);
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

    private static void append(File file, String line) {
        if (file == null) {
            return;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            rotateIfNeeded(file);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(line);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to write diagnostic log: " + file, e);
        }
    }

    private static int toPriority(String level) {
        if ("ERROR".equals(level)) return Log.ERROR;
        if ("WARN".equals(level)) return Log.WARN;
        if ("DEBUG".equals(level)) return Log.DEBUG;
        return Log.INFO;
    }

    private static void rotateIfNeeded(File file) {
        if (file.exists() && file.length() > MAX_LOG_BYTES) {
            File backup = new File(file.getParentFile(), file.getName() + ".1");
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

/* EOF */
