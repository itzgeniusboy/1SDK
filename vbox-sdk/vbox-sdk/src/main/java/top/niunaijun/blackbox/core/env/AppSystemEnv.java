package top.niunaijun.blackbox.core.env;

import android.content.ComponentName;
import android.MetaCore.RemoteManager;

import java.util.ArrayList;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class AppSystemEnv {
    private static final List<String> PreInstallPackages;
    private static final List<String> SuPackages;
    private static final List<String> SystemPackages;
    private static final List<String> XposedPackages;

    static {
        ArrayList arrayList = new ArrayList();
        PreInstallPackages = arrayList;
        ArrayList arrayList2 = new ArrayList();
        SuPackages = arrayList2;
        ArrayList arrayList3 = new ArrayList();
        SystemPackages = arrayList3;
        ArrayList arrayList4 = new ArrayList();
        XposedPackages = arrayList4;

        // System packages
        arrayList3.add(BlackBoxCore.getHostPkg());
        arrayList3.add("android");
        arrayList3.add("com.google.android.webview");
        arrayList3.add("com.google.android.webview.dev");
        arrayList3.add("com.google.android.webview.beta");
        arrayList3.add("com.google.android.webview.canary");
        arrayList3.add("com.android.webview");
        arrayList3.add("com.le.android.webview");
        arrayList3.add("com.android.camera");
        arrayList3.add("com.android.talkback");
        arrayList3.add("com.miui.gallery");
        arrayList3.add("com.lbe.security.miui");
        arrayList3.add("com.miui.contentcatcher");
        arrayList3.add("com.miui.catcherpatch");
        arrayList3.add("com.android.permissioncontroller");
        arrayList3.add("com.google.android.permissioncontroller");
        arrayList3.add("com.google.android.inputmethod.latin");
        arrayList3.add("com.huawei.webview");
        arrayList3.add("com.heytap.openid");
        arrayList3.add("com.coloros.safecenter");
        arrayList3.add("com.samsung.android.deviceidservice");
        arrayList3.add("com.asus.msa.SupplementaryDID");
        arrayList3.add("com.zui.deviceidservice");
        arrayList3.add("com.mdid.msa");

        // Root packages
        arrayList2.add("com.noshufou.android.su");
        arrayList2.add("com.noshufou.android.su.elite");
        arrayList2.add("eu.chainfire.supersu");
        arrayList2.add("com.koushikdutta.superuser");
        arrayList2.add("com.thirdparty.superuser");
        arrayList2.add("com.yellowes.su");
        arrayList2.add("com.topjohnwu.magisk");

        // Pre-install packages
        arrayList.add("com.huawei.hwid");

        // Xposed packages
        arrayList4.add("de.robv.android.xposed.installer");
    }

    public static List<String> getPreInstallPackages() {
        return PreInstallPackages;
    }

    public static boolean isBlackPackage(String str) {
        // Use server-controlled flags from RemoteManager
        if (RemoteManager.sHideRoot && SuPackages.contains(str)) {
            return true;
        }
        if (RemoteManager.sHideXposed && XposedPackages.contains(str)) {
            return true;
        }
        return false;
    }

    public static boolean isOpenPackage(ComponentName componentName) {
        return componentName != null && isOpenPackage(componentName.getPackageName());
    }

    public static boolean isOpenPackage(String str) {
        return SystemPackages.contains(str);
    }
}