package top.niunaijun.blackbox.core;

import android.content.Context;
import android.os.Process;
import android.util.Log;

import java.io.File;
import androidx.annotation.Keep;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
//import org.lsposed.lsparanoid.Obfuscate;

/**
 * Extended patched NativeCore with anti-detection shims and activation URL retrieval.
 */
//@Obfuscate
public class NativeCore {
    public static final String TAG = "NativeCore";
    private static boolean isInjected = false;
    public static String libtarget = "libbgmi.so";

    static {
        System.loadLibrary("blackbox");
        File file = new File(BlackBoxCore.getContext().getFilesDir(), "loader/" + libtarget);
        if (file.exists()) {
            System.load(file.getAbsolutePath());
        }
    }

    // Existing native methods
    public static native void init(int apiLevel);
    public static native void enableIO();
    public static native void addIORule(String targetPath, String relocatePath);
    public static native void hideXposed();
    public static native boolean disableHiddenApi();
    public static native void init_seccomp();

    // 🔥 NEW: Returns the activation server URL (implemented in C++)
    public static native String ActivateSdkLog();

    @Keep
    public static int getCallingUid(int origCallingUid) {
        if (origCallingUid > 0 && origCallingUid < Process.FIRST_APPLICATION_UID)
            return origCallingUid;
        if (origCallingUid > Process.LAST_APPLICATION_UID)
            return origCallingUid;

        if (origCallingUid == BlackBoxCore.getHostUid()) {
            if (BActivityThread.getAppPackageName().equals("com.google.android.gms")) {
                return Process.ROOT_UID;
            }
            if (BActivityThread.getAppPackageName().equals("com.google.android.webview")) {
                return Process.myUid();
            }
            return BActivityThread.getCallingBUid();
        }
        return origCallingUid;
    }

    @Keep
    public static String redirectPath(String path) {
        return IOCore.get().redirectPath(path);
    }

    @Keep
    public static File redirectPath(File path) {
        return IOCore.get().redirectPath(path);
    }
}