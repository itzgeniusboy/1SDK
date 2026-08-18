package com.onecore.loader.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.onecore.loader.floating.FloatAim;
import com.onecore.loader.floating.FloatLogo;
import com.onecore.loader.floating.Overlay;
import com.onecore.loader.libhelper.DownloadZip;
import com.onecore.loader.utils.CrashHandler;
import com.onecore.loader.utils.DiagnosticLogger;
import com.onecore.loader.utils.Prefs;
import androidx.core.content.FileProvider;
import com.Jagdish.tastytoast.TastyToast;
import com.onecore.loader.BoxApplication;
import com.onecore.loader.libhelper.ApkEnv;
import com.onecore.loader.libhelper.FileCopyTask;
import com.onecore.loader.utils.Constants;
import com.onecore.loader.utils.FLog;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import static com.onecore.loader.Config.GAME_LIST_PKG;
import com.onecore.loader.R;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class MainActivity extends Activity {

    public static MainActivity instance;
    private BlackBoxCore blackBoxCore;
    private InstallResult installResult;
    private SharedPreferences sharedPreferences;
    public static native String TimeExpired();
    public static native String FixCrash();
    public String CURRENT_PACKAGE;
    private TextView installIndia, btnStartGame;
    private View rootView;
    private Prefs prefs;
    private static final String PREF_THEME = "loader_theme";
    
    public static int gameType = 0;
    private boolean isGameLaunched = false;
    private String selectedGamePkg = "";
    private boolean isIndiaSelected = false;
    
    public static MainActivity get() {
        return instance;
    }
    
    public static void goMain(Context context) {
        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DiagnosticLogger.log("MainActivity.onCreate begin");
        setContentView(R.layout.activity_main);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        instance = this;
        blackBoxCore = BlackBoxCore.get();
        blackBoxCore.doCreate();
        countDownStart();
        GameJsonMods();
        sharedPreferences = getSharedPreferences(getPackageName(), Activity.MODE_PRIVATE);
        prefs = new Prefs(this);
        CheckFloatViewPermission();
        
        rootView = findViewById(R.id.main_root);

        View settingsButton = findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(v -> showThemePicker());

        View shareLogsButton = findViewById(R.id.btn_share_logs);
        if (shareLogsButton != null) {
            shareLogsButton.setOnClickListener(v -> shareDiagnosticLog());
        }

        selectedGamePkg = GAME_LIST_PKG[0];
        gameType = 5;
        isIndiaSelected = true;
        
        // Find Views
        installIndia = findViewById(R.id.installIndia);
        btnStartGame = findViewById(R.id.btn_start_game);
        applySelectedTheme();
        
        // Update Install Button State
        updateButtonState(0, installIndia);
        
        // Install button click listener
        installIndia.setOnClickListener(view -> handleInstallUninstall(0, installIndia));

        // Start Game button click listener
        btnStartGame.setOnClickListener(v -> {
            DiagnosticLogger.log("START GAME pressed package=" + selectedGamePkg);
            try {
                if (!ApkEnv.getInstance().isInstalled(selectedGamePkg)) {
                    DiagnosticLogger.log("START GAME blocked: package not installed=" + selectedGamePkg);
                    BoxApplication.get().showToastWithImage(Constants.GAME_NOT_INSTALL, TastyToast.ERROR);
                    return;
                }

                do_Lib_And_Run(selectedGamePkg);
            } catch (Throwable error) {
                DiagnosticLogger.exception("START GAME host-side failure", error);
                BoxApplication.get().showToastWithImage("Game launch failed; check logs", TastyToast.ERROR);
            }
        });
        
        // Start download - DownloadZip will show its own animation and dialog
        // No need to show any toast here as DownloadZip handles it
        DiagnosticLogger.log("Starting native ZIP download");
        String nativeZipUrl = null;
        try {
            nativeZipUrl = FixCrash();
            DiagnosticLogger.log("Native ZIP URL obtained: " + (nativeZipUrl == null ? "null" : nativeZipUrl));
        } catch (Throwable error) {
            DiagnosticLogger.exception("FixCrash native URL failed", error);
        }
        if (nativeZipUrl != null && nativeZipUrl.length() > 0) {
            new DownloadZip(MainActivity.get()).startDownload(nativeZipUrl, new DownloadZip.DownloadCallback() {
            @Override
            public void onStart() {
                // DownloadZip shows its own animation
            }
            @Override
            public void onProgress(int progress) {
                // Progress is handled in DownloadZip animation
            }
            @Override
            public void onSuccess() {
                // Don't show toast - DownloadZip already shows success dialog
                // You can add any additional logic here if needed
            }
            @Override
            public void onError(String error) {
                DiagnosticLogger.log("Native ZIP download error: " + error);
            }
        });
        } else {
            DiagnosticLogger.log("Native ZIP URL unavailable; download skipped");
        }
        DiagnosticLogger.log("MainActivity.onCreate completed");
    }

    private void shareDiagnosticLog() {
        try {
            File file = DiagnosticLogger.getPublicLogFile(this);
            if (file == null || !file.exists() || file.length() == 0) {
                file = DiagnosticLogger.getLogFile(this);
            }
            if (file == null || !file.exists()) {
                TastyToast.makeText(this, "Log file not created yet", TastyToast.LENGTH_LONG, TastyToast.ERROR).show();
                return;
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".diagnostics", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_TEXT, "OneCore Loader diagnostic log");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            share.setClipData(ClipData.newRawUri("diagnostic-log", uri));
            startActivity(Intent.createChooser(share, "Share Loader Log"));
        } catch (Throwable error) {
            DiagnosticLogger.exception("Unable to share diagnostic log", error);
            TastyToast.makeText(this, "Unable to share log", TastyToast.LENGTH_LONG, TastyToast.ERROR).show();
        }
    }

    private void showThemePicker() {
        final String[] themeNames = new String[]{"Neon Blue", "Cyber Purple", "Emerald Tech", "Amber Elite"};
        int selected = prefs.getInt(PREF_THEME, 0);
        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setSingleChoiceItems(themeNames, selected, (dialog, which) -> {
                    prefs.setInt(PREF_THEME, which);
                    applySelectedTheme();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applySelectedTheme() {
        int themeIndex = prefs.getInt(PREF_THEME, 0);
        int accent;
        int accentSoft;
        switch (themeIndex) {
            case 1:
                accent = Color.parseColor("#9D4DFF");
                accentSoft = Color.parseColor("#CEB2FF");
                break;
            case 2:
                accent = Color.parseColor("#14E6A3");
                accentSoft = Color.parseColor("#9FF8DD");
                break;
            case 3:
                accent = Color.parseColor("#FFB347");
                accentSoft = Color.parseColor("#FFD79A");
                break;
            case 0:
            default:
                accent = Color.parseColor("#4DB8FF");
                accentSoft = Color.parseColor("#9AB4FF");
                break;
        }

        if (rootView != null) rootView.setBackgroundColor(Color.parseColor("#000000"));
        tintText(R.id.PremiumFileManager, accent);
        tintText(R.id.tv_d, accent);
        tintText(R.id.tv_h, accent);
        tintText(R.id.tv_m, accent);
        tintText(R.id.tv_s, accent);
        tintText(R.id.IndiaVersion, accentSoft);
        tintText(R.id.tv_welcome, accentSoft);
        tintText(R.id.tv_announcement, accent);
        tintText(R.id.tv_announcement2, accentSoft);
        tintButtonBackground(R.id.installIndia, accent);
        tintButtonBackground(R.id.btn_start_game, accent);
    }

    private void tintText(int id, int color) {
        TextView view = findViewById(id);
        if (view != null) view.setTextColor(color);
    }

    private void tintButtonBackground(int id, int color) {
        View view = findViewById(id);
        if (view == null || view.getBackground() == null) return;
        view.getBackground().mutate().setTint(color);
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(Color.parseColor("#EAF7FF"));
        }
    }
    
    public void do_Lib_And_Run(String packageName) {
        CURRENT_PACKAGE = packageName;
        DiagnosticLogger.log("Preparing game launch package=" + packageName);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            try {
                File loaderFile = new File(getFilesDir(), "loader/libbgmi.so");
                DiagnosticLogger.log("Launch loader path=" + loaderFile.getAbsolutePath() + ", exists=" + loaderFile.exists() + ", size=" + (loaderFile.exists() ? loaderFile.length() : -1));
                if (!loaderFile.exists()) {
                    DiagnosticLogger.log("Launch blocked: native loader missing");
                    BoxApplication.get().showToastWithImage("Loader missing: files/loader/libbgmi.so (wait for Zoro1.zip extraction)", TastyToast.ERROR);
                    return;
                }

                boolean loaderReady = ApkEnv.getInstance().tryAddLoader(packageName);
                DiagnosticLogger.log("tryAddLoader result=" + loaderReady + ", package=" + packageName);
                if (!loaderReady) {
                    BoxApplication.get().showToastWithImage("Loader setup failed, check logs", TastyToast.ERROR);
                    return;
                }

                boolean launched = BlackBoxCore.get().launchApk(packageName, 0);
                DiagnosticLogger.log("BlackBox launchApk result=" + launched + ", package=" + packageName);
                if (!launched) {
                    BoxApplication.get().showToastWithImage("Game launch failed; check logs", TastyToast.ERROR);
                    return;
                }

                // Start the overlay only after the virtual activity launch has been requested.
                handler.postDelayed(() -> {
                    try {
                        DiagnosticLogger.log("Starting overlay after virtual launch");
                        startPatcher();
                    } catch (Throwable overlayError) {
                        DiagnosticLogger.exception("Overlay startup failed after game launch", overlayError);
                    }
                }, 500L);
            } catch (Throwable error) {
                DiagnosticLogger.exception("Virtual game launch failed", error);
                BoxApplication.get().showToastWithImage("Game launch failed; check logs", TastyToast.ERROR);
            }
        });
    }
    
    private void handleInstallUninstall(final int gameIndex, final TextView installButton) {
        final String packageName = GAME_LIST_PKG[gameIndex];
        final FileCopyTask fileCopyTask = new FileCopyTask(MainActivity.get());

        boolean isInstalled = getInstallationStatus(packageName);
        android.util.Log.d("OBBCopy", "handleInstallUninstall: pkg=" + packageName + ", isInstalled=" + isInstalled);

        if (isInstalled) {
            ApkEnv.getInstance().unInstallApp(packageName);
            installButton.setText("INSTALL");
            saveInstallationStatus(packageName, false);
            BoxApplication.get().showToastWithImage(Constants.UNINSTALL_SUCCESS, TastyToast.SUCCESS);
        } else {
            // FileCopyTask will show its own animation and dialog
            if (fileCopyTask.isObbCopied(packageName)) {
                if (ApkEnv.getInstance().installByPackage(packageName)) {
                    installButton.setText("UNINSTALL");
                    saveInstallationStatus(packageName, true);
                    BoxApplication.get().showToastWithImage(Constants.INSTALL_SUCCESS, TastyToast.SUCCESS);
                } else {
                    BoxApplication.get().showToastWithImage(Constants.MSG_ERROR, TastyToast.WARNING);
                }
            } else {
                fileCopyTask.copyObbFolderAsync(packageName, new FileCopyTask.CopyCallback() {
                    @Override
                    public void onCopyCompleted(boolean copySuccess) {
                        if (copySuccess) {
                            if (ApkEnv.getInstance().installByPackage(packageName)) {
                                installButton.setText("UNINSTALL");
                                saveInstallationStatus(packageName, true);
                                BoxApplication.get().showToastWithImage(Constants.INSTALL_SUCCESS, TastyToast.SUCCESS);
                            } else {
                                BoxApplication.get().showToastWithImage(Constants.MSG_ERROR, TastyToast.WARNING);
                            }
                        } else {
                            BoxApplication.get().showToastWithImage(Constants.COPY_FAILED, TastyToast.ERROR);
                        }
                    }
                });
            }
        }
    }
    
    private void saveInstallationStatus(String packageName, boolean installed) {
        SharedPreferences preferences = MainActivity.get().getSharedPreferences("install_status", Context.MODE_PRIVATE);
        preferences.edit().putBoolean(packageName, installed).apply();
    }

    private boolean getInstallationStatus(String packageName) {
        SharedPreferences preferences = MainActivity.get().getSharedPreferences("install_status", Context.MODE_PRIVATE);
        return preferences.getBoolean(packageName, false);
    }
    
    private void updateButtonState(int gameIndex, TextView installButton) {
        String packageName = GAME_LIST_PKG[gameIndex];
        boolean installed = getInstallationStatus(packageName);
        if(installed) {
            installButton.setText("UNINSTALL");
        } else {
            installButton.setText("INSTALL");
        }
    }
    
    private void countDownStart() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    handler.postDelayed(this, 1000);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date expiryDate = dateFormat.parse(TimeExpired());
                    long now = System.currentTimeMillis();
                    long distance = expiryDate.getTime() - now;
                    long days = distance / (24 * 60 * 60 * 1000);
                    long hours = distance / (60 * 60 * 1000) % 24;
                    long minutes = distance / (60 * 1000) % 60;
                    long seconds = distance / 1000 % 60;
                    
                    TextView Hari = findViewById(R.id.tv_d);
                    TextView Jam = findViewById(R.id.tv_h);
                    TextView Menit = findViewById(R.id.tv_m);
                    TextView Detik = findViewById(R.id.tv_s);
                    
                    Hari.setText(String.format("%02d", Math.max(0, days)));
                    Jam.setText(String.format("%02d", Math.max(0, hours)));
                    Menit.setText(String.format("%02d", Math.max(0, minutes)));
                    Detik.setText(String.format("%02d", Math.max(0, seconds)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }
    
    private void GameJsonMods() {
        try {
            JSONArray games = new JSONObject(loadJSONFromAssets()).getJSONArray("games");
            TextView indiaName = findViewById(R.id.IndiaName);
            TextView indiaVersion = findViewById(R.id.IndiaVersion);
            if (indiaName != null) {
                indiaName.setText(games.getJSONObject(1).getString("name"));
            }
            if (indiaVersion != null) {
                indiaVersion.setText("Version: " + games.getJSONObject(1).getString("version"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String loadJSONFromAssets() {
        try {
            InputStream is = getAssets().open("games.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void CheckFloatViewPermission() {
        if (!Settings.canDrawOverlays(MainActivity.get())) {
            BoxApplication.get().showToastWithImage(Constants.MSG_FLOATING, TastyToast.INFO);
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (FloatLogo.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startPatcher() {
        if (!Settings.canDrawOverlays(MainActivity.get())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 123);
        } else {
            startFloater();
        }
    }

    private void startFloater() {
        if (!isServiceRunning()) {
            startService(new Intent(MainActivity.get(), FloatLogo.class));
        } else {
            BoxApplication.get().showToastWithImage(Constants.MSG_RUNNING, TastyToast.WARNING);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        countDownStart();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(MainActivity.get(), FloatLogo.class));
        stopService(new Intent(MainActivity.get(), Overlay.class));
        stopService(new Intent(MainActivity.get(), FloatAim.class));
    }
}
