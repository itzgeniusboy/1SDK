package com.zoro.loader;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.zoro.loader.utils.FLog;
import com.zoro.loader.utils.FPrefs;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;
import top.niunaijun.blackbox.core.system.api.MetaActivationManager;

public class BoxApplication extends Application {
    public static final String STATUS_BY = "online";
    public static BoxApplication gApp;

    // 🔐 Native method declaration
    private native String BoxApp();

    public static BoxApplication get() {
        return gApp;
    }

    static {
        try {
            // 🔥 Library load ho rahi hai
            System.loadLibrary("MCoreEsp");
        } catch (UnsatisfiedLinkError w) {
            FLog.error(w.getMessage() != null ? w.getMessage() : "Load lib error");
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            BlackBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                // @Override HATAYA - ab compiler error nahi dega
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                // @Override HATAYA
                public boolean isHideRoot() {
                    return true;
                }

                // @Override HATAYA
                public boolean isHideXposed() {
                    return true;
                }

                // @Override HATAYA
                public boolean isEnableDaemonService() {
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gApp = this;
        BlackBoxCore.get().doCreate();

        try {
            String key = BoxApp();
            Log.d("LICENSE_DEBUG", "KEY: " + key);

            MetaActivationManager.activateSdk(BoxApp());    
            //   MetaActivationManager.verifyLicense(Context, String)
            //   MetaActivationManager.verifyLicense(this, key);

        } catch (Exception exception) {
            exception.printStackTrace();
            FLog.error("License Verification Failed: " + exception.getMessage());
        }
    }
}