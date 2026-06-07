package top.niunaijun.blackbox.fake.frameworks;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;
import android.util.Log;

import org.lsposed.lsparanoid.Obfuscate;

import java.util.Collections;
import java.util.List;

import android.MetaCore.nk;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.system.ServiceManager;
import top.niunaijun.blackbox.core.system.pm.IBPackageManagerService;
import top.niunaijun.blackbox.entity.pm.InstallOption;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import top.niunaijun.blackbox.entity.pm.InstalledPackage;
import top.niunaijun.blackbox.utils.Slog;

/**
 * BPackageManager – proxy to the virtual package manager service.
 * Now integrated with activation system (nk) to enforce license checks.
 */
@Obfuscate
public class BPackageManager extends BlackManager<IBPackageManagerService> {

    private static final String TAG = "BPackageManager";
    private static final BPackageManager sPackageManager = new BPackageManager();

    public static BPackageManager get() {
        return sPackageManager;
    }

    @Override
    protected String getServiceName() {
        return ServiceManager.PACKAGE_MANAGER;
    }

    // ----------------------------------------------------------------------
    // Activation check helper
    // ----------------------------------------------------------------------

    /**
     * Checks if the SDK is activated. If not, logs a warning and returns false.
     * Sensitive operations should call this and fail gracefully.
     */
    private boolean isActivated() {
        boolean activated = nk.isSystemApp(); // nk.isSystemApp() checks activation
        if (!activated) {
            Slog.w(TAG, "Operation blocked: SDK not activated");
        }
        return activated;
    }

    // ----------------------------------------------------------------------
    // Public API – all critical methods now check activation
    // ----------------------------------------------------------------------

    public Intent getLaunchIntentForPackage(String packageName, int userId) {
        if (!isActivated()) return null; // Block if not activated

        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
        intentToResolve.setPackage(packageName);
        String resolvedType = intentToResolve.resolveTypeIfNeeded(BlackBoxCore.getContext().getContentResolver());
        List<ResolveInfo> ris = queryIntentActivities(intentToResolve, 0, resolvedType, userId);

        if (ris == null || ris.isEmpty()) {
            intentToResolve.removeCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.addCategory(Intent.CATEGORY_INFO);
            intentToResolve.setPackage(packageName);
            ris = queryIntentActivities(intentToResolve, 0, resolvedType, userId);
        }

        if (ris == null || ris.isEmpty()) {
            return null;
        }

        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName, ris.get(0).activityInfo.name);
        return intent;
    }

    public ResolveInfo resolveService(Intent intent, int flags, String resolvedType, int userId) {
        if (!isActivated()) return null;
        try {
            return getService().resolveService(intent, flags, resolvedType, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public ResolveInfo resolveActivity(Intent intent, int flags, String resolvedType, int userId) {
        if (!isActivated()) return null;
        try {
            return getService().resolveActivity(intent, flags, resolvedType, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public ProviderInfo resolveContentProvider(String authority, int flags, int userId) {
        if (!isActivated()) return null;
        try {
            return getService().resolveContentProvider(authority, flags, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) {
        if (!isActivated()) return null;
        try {
            return getService().resolveIntent(intent, resolvedType, flags, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        if (!isActivated()) return null;
        try {
            return getService().getApplicationInfo(packageName, flags, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        if (!isActivated()) return null;
        try {
            return getService().getPackageInfo(packageName, flags, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) {
        if (!isActivated()) return null;
        try {
            return getService().getServiceInfo(component, flags, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public ActivityInfo getReceiverInfo(ComponentName componentName, int flags, int userId) {
        if (!isActivated()) return null;
        try {
            return getService().getReceiverInfo(componentName, flags, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
        if (!isActivated()) return null;
        try {
            return getService().getActivityInfo(component, flags, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public ProviderInfo getProviderInfo(ComponentName component, int flags, int userId) {
        if (!isActivated()) return null;
        try {
            return getService().getProviderInfo(component, flags, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags, String resolvedType, int userId) {
        if (!isActivated()) return Collections.emptyList();
        try {
            return getService().queryIntentActivities(intent, flags, resolvedType, userId);
        } catch (RemoteException e) {
            crash(e);
            return Collections.emptyList();
        }
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags, String resolvedType, int userId) {
        if (!isActivated()) return Collections.emptyList();
        try {
            return getService().queryBroadcastReceivers(intent, flags, resolvedType, userId);
        } catch (RemoteException e) {
            crash(e);
            return Collections.emptyList();
        }
    }

    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags, int userId) {
        if (!isActivated()) return Collections.emptyList();
        try {
            return getService().queryContentProviders(processName, uid, flags, userId);
        } catch (RemoteException e) {
            crash(e);
            return Collections.emptyList();
        }
    }

    public InstallResult installPackageAsUser(String file, InstallOption option, int userId) {
        if (!isActivated()) {
            // Return a failed InstallResult
            InstallResult result = new InstallResult();
            result.installError("SDK not activated. Contact " + nk.getServerMessage());
            return result;
        }
        try {
            return getService().installPackageAsUser(file, option, userId);
        } catch (RemoteException e) {
            crash(e);
            return null;
        }
    }

    public List<ApplicationInfo> getInstalledApplications(int flags, int userId) {
        if (!isActivated()) return Collections.emptyList();
        try {
            return getService().getInstalledApplications(flags, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "getInstalledApplications failed", e);
            return Collections.emptyList();
        }
    }

    public List<PackageInfo> getInstalledPackages(int flags, int userId) {
        if (!isActivated()) return Collections.emptyList();
        try {
            return getService().getInstalledPackages(flags, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "getInstalledPackages failed", e);
            return Collections.emptyList();
        }
    }

    public void clearPackage(String packageName, int userId) {
        if (!isActivated()) return;
        try {
            getService().clearPackage(packageName, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "clearPackage failed", e);
        }
    }

    public void stopPackage(String packageName, int userId) {
        if (!isActivated()) return;
        try {
            getService().stopPackage(packageName, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "stopPackage failed", e);
        }
    }

    public boolean isAppRunning(String packageName, int userId) {
        if (!isActivated()) return false;
        try {
            return getService().isAppRunning(packageName, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "isAppRunning failed", e);
            return false;
        }
    }

    public void uninstallPackageAsUser(String packageName, int userId) {
        if (!isActivated()) return;
        try {
            getService().uninstallPackageAsUser(packageName, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "uninstallPackageAsUser failed", e);
        }
    }

    public void uninstallPackage(String packageName) {
        if (!isActivated()) return;
        try {
            getService().uninstallPackage(packageName);
        } catch (RemoteException e) {
            Log.e(TAG, "uninstallPackage failed", e);
        }
    }

    public boolean isInstalled(String packageName, int userId) {
        if (!isActivated()) return false;
        try {
            return getService().isInstalled(packageName, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "isInstalled failed", e);
            return false;
        }
    }

    public List<InstalledPackage> getInstalledPackagesAsUser(int userId) {
        if (!isActivated()) return Collections.emptyList();
        try {
            return getService().getInstalledPackagesAsUser(userId);
        } catch (RemoteException e) {
            Log.e(TAG, "getInstalledPackagesAsUser failed", e);
            return Collections.emptyList();
        }
    }

    public String[] getPackagesForUid(int uid) {
        if (!isActivated()) return new String[0];
        try {
            return getService().getPackagesForUid(uid, BActivityThread.getUserId());
        } catch (RemoteException e) {
            Log.e(TAG, "getPackagesForUid failed", e);
            return new String[0];
        }
    }

    private void crash(Throwable e) {
        Slog.e(TAG, "Remote exception", e);
    }
}