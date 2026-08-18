package com.zoro.loader;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.zoro.loader.utils.DiagnosticLogger;
import com.zoro.loader.utils.FLog;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;
import top.niunaijun.blackbox.core.system.api.MetaActivationManager;

public class BoxApplication extends Application {
    public static final String STATUS_BY = "online";
    public static BoxApplication gApp;

    private native String BoxApp();

    public static BoxApplication get() {
        return gApp;
    }

    static {
        try {
            System.loadLibrary("MCoreEsp");
            Log.i("ZoroLoader", "Native library MCoreEsp loaded");
        } catch (UnsatisfiedLinkError error) {
            FLog.error("Native library MCoreEsp load failed", error);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        DiagnosticLogger.init(base.getApplicationContext());
        FLog.info("Application.attachBaseContext started");
        super.attachBaseContext(base);
        try {
            BlackBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                public boolean isHideRoot() {
                    return true;
                }

                public boolean isHideXposed() {
                    return true;
                }

                public boolean isEnableDaemonService() {
                    return true;
                }
            });
            FLog.info("BlackBoxCore.doAttachBaseContext completed");
        } catch (Throwable throwable) {
            FLog.error("BlackBoxCore.doAttachBaseContext failed", throwable);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gApp = this;
        FLog.info("Application.onCreate started");
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            FLog.error("Uncaught exception on thread " + thread.getName(), throwable);
            android.os.Process.killProcess(android.os.Process.myPid());
        });

        try {
            BlackBoxCore.get().doCreate();
            FLog.info("BlackBoxCore.doCreate completed");
        } catch (Throwable throwable) {
            FLog.error("BlackBoxCore.doCreate failed", throwable);
        }

        String processName = Application.getProcessName();
        boolean isHostProcess = getPackageName().equals(processName);
        FLog.info("Process check: name=" + processName + ", host=" + isHostProcess);
        if (!isHostProcess) {
            FLog.info("Skipping host-only SDK activation in virtual process");
            return;
        }

        try {
            String key = BoxApp();
            FLog.info("Activation key obtained: " + (key == null ? "null" : "length=" + key.length()));
            MetaActivationManager.activateSdk(key);
            FLog.info("MetaActivationManager.activateSdk completed");
        } catch (Throwable throwable) {
            FLog.error("License/SDK activation failed", throwable);
        }
    }
}
