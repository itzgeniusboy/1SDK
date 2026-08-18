package com.onecore.loader;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.Jagdish.tastytoast.TastyToast;
import com.google.android.material.color.DynamicColors;
import com.onecore.loader.libhelper.VirtualNativeLoaderCallback;
import com.onecore.loader.utils.CrashHandler;
import com.onecore.loader.utils.DiagnosticLogger;
import com.onecore.loader.utils.FLog;
import com.onecore.loader.utils.NetworkConnection;
import com.topjohnwu.superuser.Shell;

import java.io.IOException;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;

public class BoxApplication extends Application {
    public static final String STATUS_BY = "online";
    public static BoxApplication gApp;
    private static volatile boolean nativeLoaded;
    private boolean isNetworkConnected = false;

    public static BoxApplication get() {
        return gApp;
    }

    public static boolean isNativeLoaded() {
        return nativeLoaded;
    }

    public boolean isInternetAvailable() {
        return isNetworkConnected;
    }

    public void setInternetAvailable(boolean value) {
        isNetworkConnected = value;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        DiagnosticLogger.init(base);
        DiagnosticLogger.log("Application.attachBaseContext");
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(base));

        try {
            System.loadLibrary("MCoreEsp");
            nativeLoaded = true;
            DiagnosticLogger.log("Native MCoreEsp loaded");
        } catch (Throwable error) {
            nativeLoaded = false;
            DiagnosticLogger.exception("Native MCoreEsp load failed", error);
        }

        try {
            BlackBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean isEnableDaemonService() {
                    return true;
                }
            });
            DiagnosticLogger.log("BlackBoxCore.attachBaseContext completed");
        } catch (Throwable error) {
            DiagnosticLogger.exception("BlackBoxCore.attachBaseContext failed", error);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gApp = this;
        DiagnosticLogger.log("Application.onCreate");
        try {
            BlackBoxCore.get().doCreate();
            BlackBoxCore.get().addAppLifecycleCallback(new VirtualNativeLoaderCallback());
            DiagnosticLogger.log("BlackBoxCore.doCreate completed");
        } catch (Throwable error) {
            DiagnosticLogger.exception("BlackBoxCore.doCreate failed", error);
        }
        DynamicColors.applyToActivitiesIfAvailable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        NetworkConnection.CheckInternet network = new NetworkConnection.CheckInternet(this);
        network.registerNetworkCallback();
    }

    public void showToastWithImage(String msg, int type) {
        TastyToast.makeText(BoxApplication.get(), msg, TastyToast.LENGTH_LONG, type).show();
    }

    public static boolean checkRootAccess() {
        if (Shell.rootAccess()) {
            FLog.info("Root granted");
            return true;
        }
        FLog.info("Root not granted");
        return false;
    }

    public static void doExe(String shell) {
        if (checkRootAccess()) {
            Shell.su(shell).exec();
        } else {
            try {
                Runtime.getRuntime().exec(shell);
                FLog.info("Shell: " + shell);
            } catch (IOException error) {
                FLog.error("Shell execution failed", error);
            }
        }
    }

    public void doExecute(String shell) {
        doChmod(shell, 777);
        doExe(shell);
    }

    public static void doChmod(String shell, int mask) {
        doExe("chmod " + mask + " " + shell);
    }
}
