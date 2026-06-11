package top.niunaijun.blackbox.core;

import android.os.Process;
import androidx.annotation.Keep;
import java.io.File;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;

public class RNative {
    
    public static final String TAG = "RNative";
    public static final int HOOK_BASE = 1;
    public static final int HOOK_UNIX_FILE_SYSTEM = 1 << 1;
    public static final int HOOK_VM_CLASS_LOADER = 1 << 2;
    public static final int HOOK_SYSTEM_PROPERTIES = 1 << 3;
    public static final int HOOK_RUNTIME_LOAD = 1 << 4;
    public static final int HOOK_LINUX_IO = 1 << 5;
    public static final int HOOK_BINDER = 1 << 6;
    public static final int HOOK_ALL = HOOK_BASE
            | HOOK_UNIX_FILE_SYSTEM
            | HOOK_VM_CLASS_LOADER
            | HOOK_SYSTEM_PROPERTIES
            | HOOK_RUNTIME_LOAD
            | HOOK_LINUX_IO
            | HOOK_BINDER;

    static {
        System.loadLibrary("RIYAZcore");
    }

    public static native void init(int apiLevel);
    public static native void enableIO();
    public static native void enableIO(int hookFlags);
    public static native void addIORule(String targetPath, String relocatePath);
    public static native void hideXposed();
    
    @Keep
    public static int getCallingUid(int origCallingUid) {
        if (origCallingUid > 0 && origCallingUid < Process.FIRST_APPLICATION_UID) return origCallingUid;
        if (origCallingUid > Process.LAST_APPLICATION_UID) return origCallingUid;
        if (origCallingUid == BlackBoxCore.getHostUid()) {
            if(BActivityThread.getAppPackageName().equals("com.google.android.gms")){
                return Process.ROOT_UID;
            }
            if(BActivityThread.getAppPackageName().equals("com.google.android.webview")){
                return Process.myUid();
            }
            return BActivityThread.getCallingBUid();
        }
        return origCallingUid;
    }

    @Keep
    public static String redirectPath(String path) {
        return RCore.get().redirectPath(path);
    }

    @Keep
    public static File redirectPath(File path) {
        return RCore.get().redirectPath(path);
    }

}
