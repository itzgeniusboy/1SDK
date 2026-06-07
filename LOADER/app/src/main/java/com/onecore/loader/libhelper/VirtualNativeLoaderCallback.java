package com.onecore.loader.libhelper;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import com.onecore.loader.BoxApplication;
import com.onecore.loader.utils.FLog;
import java.io.File;
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback;

public class VirtualNativeLoaderCallback extends AppLifecycleCallback {
    private static final String TARGET_PACKAGE = "com.pubg.imobile";
    private static final String LOADER_NAME = "libbgmi.so";
    private static volatile boolean loadedInCurrentProcess = false;

    private String getProcessName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName();
        }
        return "pid=" + Process.myPid();
    }

    private boolean shouldLoad(String packageName, String processName) {
        if (!TARGET_PACKAGE.equals(packageName)) {
            FLog.info("[VLoader] skip package=" + packageName + ", target=" + TARGET_PACKAGE);
            return false;
        }
        if (processName != null && !TARGET_PACKAGE.equals(processName)) {
            FLog.info("[VLoader] skip non-main process=" + processName);
            return false;
        }
        return true;
    }

    private void tryLoadFromContext(Context virtualContext, String stage, String packageName, String targetProcessName, int userId) {
        if (!shouldLoad(packageName, targetProcessName)) {
            return;
        }

        long ts = System.currentTimeMillis();
        String processName = getProcessName();

        File hostLoader = new File(BoxApplication.get().getFilesDir(), "loader/" + LOADER_NAME);
        File virtualLoader = virtualContext != null ? new File(virtualContext.getFilesDir(), "loader/" + LOADER_NAME) : null;

        FLog.info("[VLoader] ts=" + ts + ", stage=" + stage + ", process=" + processName + ", package=" + packageName + ", user=" + userId);
        FLog.info("[VLoader] hostPath=" + hostLoader.getAbsolutePath() + ", exists=" + hostLoader.exists() + ", size=" + (hostLoader.exists() ? hostLoader.length() : -1));
        if (virtualLoader != null) {
            FLog.info("[VLoader] virtualPath=" + virtualLoader.getAbsolutePath() + ", exists=" + virtualLoader.exists() + ", size=" + (virtualLoader.exists() ? virtualLoader.length() : -1));
        }

        if (loadedInCurrentProcess) {
            FLog.info("[VLoader] already loaded in process=" + processName + ", skip duplicate load");
            return;
        }

        File loadTarget = hostLoader.exists() ? hostLoader : virtualLoader;
        if (loadTarget == null || !loadTarget.exists()) {
            FLog.error("[VLoader] no loader file found in host/virtual files directories");
            return;
        }

        try {
            System.load(loadTarget.getAbsolutePath());
            loadedInCurrentProcess = true;
            FLog.info("[VLoader] System.load SUCCESS ts=" + System.currentTimeMillis() + ", process=" + processName + ", path=" + loadTarget.getAbsolutePath());
            FLog.info("[VLoader] NOTE: verify JNI_OnLoad via native logcat from loaded .so");
        } catch (Throwable t) {
            FLog.error("[VLoader] System.load FAILED ts=" + System.currentTimeMillis() + ", process=" + processName + ", path=" + loadTarget.getAbsolutePath() + ", error=" + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    @Override
    public void afterApplicationOnCreate(String packageName, String processName, Application application, int userId) {
        tryLoadFromContext(application, "afterApplicationOnCreate", packageName, processName, userId);
    }
}
