package com.onecore.loader.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Persistent diagnostics for the original Loader source. */
public final class DiagnosticLogger {
    private static final String TAG = "OneCoreDiagnostic";
    private static final String FILE_NAME = "onecore_loader.log";
    private static final Object LOCK = new Object();
    private static volatile File privateLogFile;
    private static volatile File publicLogFile;

    private DiagnosticLogger() {
    }

    public static void init(Context context) {
        if (context == null) {
            return;
        }
        synchronized (LOCK) {
            try {
                File dir = new File(context.getFilesDir(), "diagnostics");
                if (!dir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    dir.mkdirs();
                }
                privateLogFile = new File(dir, FILE_NAME);
                appendLocked("=== Loader start ===");
                appendLocked("SDK=" + Build.VERSION.SDK_INT + ", Android=" + Build.VERSION.RELEASE + ", Model=" + Build.MODEL);
                appendLocked("Package=" + context.getPackageName());
            } catch (Throwable error) {
                Log.e(TAG, "Private diagnostic initialization failed", error);
            }
            try {
                File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloads.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    downloads.mkdirs();
                }
                publicLogFile = new File(downloads, FILE_NAME);
                appendLocked("Public log path=" + publicLogFile.getAbsolutePath());
            } catch (Throwable error) {
                Log.e(TAG, "Public diagnostic initialization failed", error);
            }
        }
    }

    public static void log(String message) {
        synchronized (LOCK) {
            appendLocked(message == null ? "null" : message);
        }
    }

    public static void exception(String label, Throwable error) {
        StringWriter writer = new StringWriter();
        if (error != null) {
            error.printStackTrace(new PrintWriter(writer));
        }
        log(label + ": " + writer.toString());
    }

    public static File getLogFile(Context context) {
        if (privateLogFile == null) {
            init(context);
        }
        return privateLogFile;
    }

    public static File getPublicLogFile(Context context) {
        if (publicLogFile == null) {
            init(context);
        }
        return publicLogFile;
    }

    private static void appendLocked(String message) {
        String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date())
                + " [" + Thread.currentThread().getName() + "] " + message + "\n";
        Log.i(TAG, message);
        write(privateLogFile, line);
        if (publicLogFile != null && !publicLogFile.equals(privateLogFile)) {
            write(publicLogFile, line);
        }
    }

    private static void write(File file, String text) {
        if (file == null) {
            return;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            FileOutputStream output = new FileOutputStream(file, true);
            try {
                output.write(text.getBytes(Charset.forName("UTF-8")));
                output.flush();
            } finally {
                output.close();
            }
        } catch (Throwable error) {
            Log.e(TAG, "Unable to write diagnostic file: " + file, error);
        }
    }
}

