package top.niunaijun.blackbox.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.TrieTree;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.lsposed.lsparanoid.Obfuscate;

/**
 * Created by Milk on 4/9/21.
 * <p>
 * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
@Obfuscate
@SuppressLint("SdCardPath")
public class IOCore {
    public static final String TAG = "IOCore";

    private static final IOCore sIOCore = new IOCore();
    private static final TrieTree mTrieTree = new TrieTree();

    private final Map<String, String> mRedirectMap = new LinkedHashMap<>();

    public static IOCore get() {
        return sIOCore;
    }

    /**
     * Add a path redirect rule.
     *
     * @param origPath     original path
     * @param redirectPath redirected path
     */
    public void addRedirect(@NonNull String origPath, @NonNull String redirectPath) {
        if (TextUtils.isEmpty(origPath) || TextUtils.isEmpty(redirectPath)) {
            return;
        }
        if (mRedirectMap.containsKey(origPath)) {
            return;
        }
        // Add the key to TrieTree for fast prefix search
        mTrieTree.add(origPath);
        mRedirectMap.put(origPath, redirectPath);

        // Ensure the target directory exists
        File redirectFile = new File(redirectPath);
        if (!redirectFile.exists()) {
            FileUtils.mkdirs(redirectPath);
        }

        // Notify native layer
        NativeCore.addIORule(origPath, redirectPath);
        Log.d(TAG, "Added redirect: " + origPath + " -> " + redirectPath);
    }

    /**
     * Redirect a single path using the global rules.
     *
     * @param path original path
     * @return redirected path, or original if no rule matches
     */
    @Nullable
    public String redirectPath(@Nullable String path) {
        if (TextUtils.isEmpty(path)) {
            return path;
        }
        // Skip localhost paths (used for some internal communication)
        if (path.contains("/localhost/")) {
            return path;
        }

        // Find the longest matching prefix
        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key)) {
            String replacement = mRedirectMap.get(key);
            if (replacement != null) {
                String newPath = path.replace(key, replacement);
                Log.v(TAG, "Redirected: " + path + " -> " + newPath);
                return newPath;
            }
        }
        return path;
    }

    /**
     * Redirect a File using the global rules.
     *
     * @param path original File
     * @return redirected File, or the original if no change
     */
    @Nullable
    public File redirectPath(@Nullable File path) {
        if (path == null) return null;
        String original = path.getAbsolutePath();
        String redirected = redirectPath(original);
        if (original.equals(redirected)) {
            return path;
        }
        return new File(redirected);
    }

    /**
     * Redirect a path using a custom rule map (used for per‑process rules).
     *
     * @param path original path
     * @param rule custom rule map
     * @return redirected path
     */
    @Nullable
    public String redirectPath(@Nullable String path, @NonNull Map<String, String> rule) {
        if (TextUtils.isEmpty(path)) return path;
        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key)) {
            String replacement = rule.get(key);
            if (replacement != null) {
                return path.replace(key, replacement);
            }
        }
        return path;
    }

    /**
     * Redirect a File using a custom rule map.
     *
     * @param path original File
     * @param rule custom rule map
     * @return redirected File
     */
    @Nullable
    public File redirectPath(@Nullable File path, @NonNull Map<String, String> rule) {
        if (path == null) return null;
        return new File(redirectPath(path.getAbsolutePath(), rule));
    }

    /**
     * Enable I/O redirection for the given context (usually a virtual app process).
     *
     * @param context context of the virtual app
     */
    public void enableRedirect(@NonNull Context context) {
        Map<String, String> rule = new LinkedHashMap<>();
        String packageName = context.getPackageName();

        try {
            ApplicationInfo appInfo = BlackBoxCore.getBPackageManager()
                    .getApplicationInfo(packageName, PackageManager.GET_META_DATA, BActivityThread.getUserId());
            int hostUserId = BlackBoxCore.getHostUserId();

            // Redirect library paths
            String libPrefix1 = String.format("/data/data/%s/lib", packageName);
            String libPrefix2 = String.format("/data/user/%d/%s/lib", hostUserId, packageName);
            rule.put(libPrefix1, appInfo.nativeLibraryDir);
            rule.put(libPrefix2, appInfo.nativeLibraryDir);

            // Redirect data directories
            String dataPrefix1 = String.format("/data/data/%s", packageName);
            String dataPrefix2 = String.format("/data/user/%d/%s", hostUserId, packageName);
            rule.put(dataPrefix1, appInfo.dataDir);
            rule.put(dataPrefix2, appInfo.dataDir);

            // External storage (SD card) redirection
            File hostExternalCache = BlackBoxCore.getContext().getExternalCacheDir();
            File appExternalCache = context.getExternalCacheDir();
            if (hostExternalCache != null && appExternalCache != null) {
                File externalUserDir = BEnvironment.getExternalUserDir(BActivityThread.getUserId());
                File sdcardAndroid = new File(android.os.Environment.getExternalStorageDirectory(), "Android");
                String sdcardPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                String emulatedPath = String.format("/storage/emulated/%d/Android", hostUserId);

                // Use existing directory
                if (!sdcardAndroid.exists()) {
                    sdcardAndroid = new File(emulatedPath);
                }

                if (sdcardAndroid.exists()) {
                    File[] childDirs = sdcardAndroid.listFiles(File::isDirectory);
                    if (childDirs != null) {
                        for (File child : childDirs) {
                            String childName = child.getName();
                            String redirectBase = externalUserDir.getAbsolutePath() + "/Android/" + childName;
                            rule.put(sdcardPath + "/Android/" + childName, redirectBase);
                            rule.put(emulatedPath + "/" + childName, redirectBase);
                        }
                    } else {
                        // No subdirectories, redirect whole Android folder
                        String redirectBase = externalUserDir.getAbsolutePath() + "/Android";
                        rule.put(sdcardPath + "/Android", redirectBase);
                        rule.put(emulatedPath, redirectBase);
                    }
                } else {
                    // No Android folder, redirect root of external storage
                    String redirectBase = externalUserDir.getAbsolutePath();
                    rule.put(sdcardPath + "/Android", redirectBase);
                    rule.put(emulatedPath, redirectBase);
                }

                // Specific OBB and data folders
                rule.put(sdcardPath + "/Android/obb", externalUserDir.getAbsolutePath() + "/Android/obb");
                rule.put(emulatedPath + "/obb", externalUserDir.getAbsolutePath() + "/Android/obb");
                rule.put(sdcardPath + "/Android/data", externalUserDir.getAbsolutePath() + "/Android/data");
                rule.put(emulatedPath + "/data", externalUserDir.getAbsolutePath() + "/Android/data");
            }

            // Root hiding if enabled
            if (BlackBoxCore.get().isHideRoot()) {
                hideRoot(rule);
            }

            // Add /proc redirection for cmdline
            addProcRedirect(rule);

        } catch (Exception e) {
            Log.e(TAG, "Failed to setup I/O redirection", e);
        }

        // Apply all collected rules
        for (Map.Entry<String, String> entry : rule.entrySet()) {
            get().addRedirect(entry.getKey(), entry.getValue());
        }

        // Enable native I/O hooking
        NativeCore.enableIO();
    }

    /**
     * Add fake paths to hide root binaries.
     *
     * @param rule rule map to populate
     */
    private void hideRoot(@NonNull Map<String, String> rule) {
        String[] suPaths = {
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su"
        };
        for (String path : suPaths) {
            rule.put(path, path + "-fake");
        }
    }

    /**
     * Redirect /proc/self/cmdline and /proc/<pid>/cmdline to a virtual file
     * that contains the virtual app's process name.
     *
     * @param rule rule map to populate
     */
    private void addProcRedirect(@NonNull Map<String, String> rule) {
        int appPid = BActivityThread.getAppPid();
        int myPid = Process.myPid();
        String selfProc = "/proc/self/";
        String proc = "/proc/" + myPid + "/";

        File cmdlineFile = new File(BEnvironment.getProcDir(appPid), "cmdline");
        String cmdlinePath = cmdlineFile.getAbsolutePath();

        rule.put(proc + "cmdline", cmdlinePath);
        rule.put(selfProc + "cmdline", cmdlinePath);
    }
}