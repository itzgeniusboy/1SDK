package com.onecore.loader.utils;

import android.util.Log;

public final class FLog {
    public static final String TAG = "OneCoreLoader";

    private FLog() {
    }

    public static void debug(String msg) {
        Log.d(TAG, msg == null ? "null" : msg);
        DiagnosticLogger.log("DEBUG " + msg);
    }

    public static void info(String msg) {
        Log.i(TAG, msg == null ? "null" : msg);
        DiagnosticLogger.log("INFO " + msg);
    }

    public static void warning(String msg) {
        Log.w(TAG, msg == null ? "null" : msg);
        DiagnosticLogger.log("WARN " + msg);
    }

    public static void error(String msg) {
        Log.e(TAG, msg == null ? "null" : msg);
        DiagnosticLogger.log("ERROR " + msg);
    }

    public static void error(String message, Throwable error) {
        Log.e(TAG, message == null ? "null" : message, error);
        DiagnosticLogger.exception("ERROR " + message, error);
    }
}
