package com.zoro.loader.utils;

public final class FLog {
    public static final String TAG = "ZoroLoader";

    private FLog() {
    }

    public static void debug(String msg) {
        DiagnosticLogger.log("DEBUG", msg);
    }

    public static void info(String msg) {
        DiagnosticLogger.log("INFO", msg);
    }

    public static void warning(String msg) {
        DiagnosticLogger.log("WARN", msg);
    }

    public static void error(String msg) {
        DiagnosticLogger.log("ERROR", msg);
    }

    public static void error(String where, Throwable throwable) {
        DiagnosticLogger.exception(where, throwable);
    }
}
