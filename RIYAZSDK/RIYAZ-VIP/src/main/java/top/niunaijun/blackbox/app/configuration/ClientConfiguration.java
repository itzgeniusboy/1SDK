package top.niunaijun.blackbox.app.configuration;

import java.io.File;

public abstract class ClientConfiguration {
    
    public abstract String getHostPackageName();
    
    public boolean setHideRoot() {
        return false;
    }
    
    public boolean isEnableDaemonService() {
        return true;
    }

    public boolean isEnableLauncherActivity() {
        return true;
    }

    /**
     * Controls the Java Binder proxy for android.media.IAudioService.
     * High-performance native games (UE4/BGMI) are safer with the platform audio service left untouched.
     */
    public boolean isAudioManagerProxyEnabled() {
        return true;
    }

    /**
     * Controls installation of native/JNI I/O redirection hooks.
     * Keep this disabled for games that abort inside bionic/UE4 after native startup.
     */
    public boolean isNativeIORedirectEnabled() {
        return true;
    }

    /**
     * Bit mask consumed by RNative.enableIO(int). 0 keeps redirect rules in Java only.
     */
    public int getNativeHookFlags() {
        return -1;
    }

    public boolean requestInstallPackage(File file) {
        return false;
    }
}
