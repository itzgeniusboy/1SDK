package com.zoro.loader.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.zoro.loader.R;
import com.zoro.loader.libhelper.FileCopyTask;
import com.zoro.loader.utils.FLog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

public class MainActivity extends Activity {
    public static native String TimeExpired();

    private static final String BGMI_PACKAGE = "com.pubg.imobile";
    private static final int USER_ID = 0;
    private BlackBoxCore blackBoxCore;
    private Button starthack;
    private Button stophack;
    private Button shareLogs;
    private FileCopyTask fileCopyTask;
    private LottieAnimationView backgroundAnimation;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FLog.info("MainActivity.onCreate started");

        backgroundAnimation = findViewById(R.id.backgroundAnimation);
        if (backgroundAnimation != null) {
            backgroundAnimation.setSpeed(0.8f);
            backgroundAnimation.setRepeatCount(LottieDrawable.INFINITE);
            backgroundAnimation.playAnimation();
        }

        try {
            blackBoxCore = BlackBoxCore.get();
            FLog.info("BlackBoxCore instance obtained");
            fileCopyTask = new FileCopyTask(this);
        } catch (Throwable throwable) {
            FLog.error("MainActivity SDK setup failed", throwable);
            Toast.makeText(this, "SDK setup failed. Share logs.", Toast.LENGTH_LONG).show();
        }

        starthack = findViewById(R.id.starthack);
        stophack = findViewById(R.id.stophack);
        shareLogs = findViewById(R.id.shareLogs);
        countDownStart();

        if (starthack != null) starthack.setOnClickListener(view -> handleStart());
        if (stophack != null) stophack.setOnClickListener(view -> handleStop());
        if (shareLogs != null) shareLogs.setOnClickListener(view -> shareLogs());
    }

    private void countDownStart() {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    String expiryStr = TimeExpired();
                    if (expiryStr != null && !expiryStr.isEmpty()) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        Date expiryDate = dateFormat.parse(expiryStr);
                        if (expiryDate != null) {
                            long distance = expiryDate.getTime() - System.currentTimeMillis();
                            if (distance > 0) {
                                long days = distance / (24 * 60 * 60 * 1000);
                                long hours = (distance / (60 * 60 * 1000)) % 24;
                                long minutes = (distance / (60 * 1000)) % 60;
                                long seconds = (distance / 1000) % 60;
                                runOnUiThread(() -> {
                                    TextView tvD = findViewById(R.id.tv_d);
                                    TextView tvH = findViewById(R.id.tv_h);
                                    TextView tvM = findViewById(R.id.tv_m);
                                    TextView tvS = findViewById(R.id.tv_s);
                                    if (tvD != null) tvD.setText(String.format(Locale.getDefault(), "%02d", days));
                                    if (tvH != null) tvH.setText(String.format(Locale.getDefault(), "%02d", hours));
                                    if (tvM != null) tvM.setText(String.format(Locale.getDefault(), "%02d", minutes));
                                    if (tvS != null) tvS.setText(String.format(Locale.getDefault(), "%02d", seconds));
                                });
                            }
                        }
                    }
                    handler.postDelayed(this, 1000);
                } catch (Throwable throwable) {
                    FLog.error("Subscription countdown failed", throwable);
                }
            }
        };
        handler.post(runnable);
    }

    private void handleStart() {
        FLog.info("Start game clicked. package=" + BGMI_PACKAGE + ", user=" + USER_ID);
        try {
            if (blackBoxCore == null) {
                FLog.error("Start aborted: BlackBoxCore is null");
                Toast.makeText(this, "SDK not ready. Share logs.", Toast.LENGTH_LONG).show();
                return;
            }
            boolean installed = blackBoxCore.isInstalled(BGMI_PACKAGE, USER_ID);
            FLog.info("Virtual install state=" + installed);
            if (installed) {
                copyObbFilesAndLaunch();
            } else {
                installGame();
            }
        } catch (Throwable throwable) {
            FLog.error("Start game failed before install/copy", throwable);
            Toast.makeText(this, "Game start failed. Share logs.", Toast.LENGTH_LONG).show();
        }
    }

    private void installGame() {
        Toast.makeText(this, "Installing In Container...", Toast.LENGTH_SHORT).show();
        FLog.info("Installing package into virtual user");
        try {
            InstallResult result = blackBoxCore.installPackageAsUser(BGMI_PACKAGE, USER_ID);
            FLog.info("Install result success=" + result.success + ", message=" + result.msg);
            if (result.success) {
                copyObbFilesAndLaunch();
            } else {
                FLog.error("Installation failed: " + result.msg);
                Toast.makeText(this, "Installation Failed: " + result.msg, Toast.LENGTH_LONG).show();
            }
        } catch (Throwable throwable) {
            FLog.error("installPackageAsUser threw", throwable);
            Toast.makeText(this, "Installation crashed. Share logs.", Toast.LENGTH_LONG).show();
        }
    }

    private void copyObbFilesAndLaunch() {
        FLog.info("Checking/copying OBB files");
        try {
            fileCopyTask.copyObbFolderAsync(BGMI_PACKAGE, success -> {
                FLog.info("OBB copy callback success=" + success);
                if (!success) {
                    FLog.error("Game launch blocked because OBB copy failed");
                    Toast.makeText(this, "OBB copy failed. Share logs.", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    FLog.info("Calling BlackBoxCore.launchApk");
                    blackBoxCore.launchApk(BGMI_PACKAGE, USER_ID);
                    FLog.info("BlackBoxCore.launchApk returned");
                } catch (Throwable throwable) {
                    FLog.error("BlackBoxCore.launchApk threw", throwable);
                    Toast.makeText(this, "Game launch failed. Share logs.", Toast.LENGTH_LONG).show();
                }
            });
        } catch (Throwable throwable) {
            FLog.error("OBB copy task could not start", throwable);
            Toast.makeText(this, "OBB preparation failed. Share logs.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleStop() {
        FLog.info("Uninstall game clicked");
        try {
            blackBoxCore.uninstallPackageAsUser(BGMI_PACKAGE, USER_ID);
            FLog.info("Game uninstalled from virtual user");
            Toast.makeText(this, "Game Uninstalled From Container", Toast.LENGTH_SHORT).show();
        } catch (Throwable throwable) {
            FLog.error("Game uninstall failed", throwable);
            Toast.makeText(this, "Uninstall failed. Share logs.", Toast.LENGTH_LONG).show();
        }
    }

    private void shareLogs() {
        try {
            File logFile = com.zoro.loader.utils.DiagnosticLogger.getPublicLogFile(getApplicationContext());
            if (!logFile.exists()) {
                logFile = com.zoro.loader.utils.DiagnosticLogger.getLogFile(getApplicationContext());
            }
            FLog.info("Preparing log share: " + logFile.getAbsolutePath());
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".diagnostics", logFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Zoro Loader diagnostic log");
            intent.putExtra(Intent.EXTRA_TEXT, "Loader log attached. Reproduce the issue once before sharing.");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Send Loader Log"));
        } catch (Throwable throwable) {
            FLog.error("Unable to share diagnostic log", throwable);
            Toast.makeText(this, "Log share failed: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundAnimation != null && !backgroundAnimation.isAnimating()) backgroundAnimation.resumeAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundAnimation != null && backgroundAnimation.isAnimating()) backgroundAnimation.pauseAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundAnimation != null) {
            backgroundAnimation.cancelAnimation();
            backgroundAnimation.clearAnimation();
        }
    }
}
