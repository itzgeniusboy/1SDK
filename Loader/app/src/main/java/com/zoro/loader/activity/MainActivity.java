package com.zoro.loader.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.zoro.loader.R;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import com.zoro.loader.libhelper.FileCopyTask;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Lottie Animation Import
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

public class MainActivity extends Activity {
    
    public static native String TimeExpired();

    private static final String BGMI_PACKAGE = "com.pubg.imobile";
    private static final int USER_ID = 0;

    private BlackBoxCore blackBoxCore;
    private Button starthack, stophack; 
    private FileCopyTask fileCopyTask;
    private LottieAnimationView backgroundAnimation; // Lottie Animation View

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- LOTTIE ANIMATION BACKGROUND ---
        backgroundAnimation = findViewById(R.id.backgroundAnimation);
        if (backgroundAnimation != null) {
            // Set animation speed (0.8 = 80% of original speed)
            backgroundAnimation.setSpeed(0.8f);
            
            // Infinite loop
            backgroundAnimation.setRepeatCount(LottieDrawable.INFINITE);
            
            // Optional: Adjust transparency if needed
            // backgroundAnimation.setAlpha(0.9f);
            
            // Optional: Start animation (already autoplay in XML)
            backgroundAnimation.playAnimation();
            
            // Optional: Animation listener for events
            /*
            backgroundAnimation.addAnimatorUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();
                // Do something with animation progress
            });
            */
        }

        // --- Your Original Logic Below ---
        blackBoxCore = BlackBoxCore.get();
        blackBoxCore.doCreate();
        
        fileCopyTask = new FileCopyTask(this);

        starthack = findViewById(R.id.starthack);
        stophack = findViewById(R.id.stophack);

        countDownStart();

        if (starthack != null) {
            starthack.setOnClickListener(view -> handleStart());
        }
        
        if (stophack != null) {
            stophack.setOnClickListener(view -> handleStop());
        }
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
                    handler.postDelayed(this, 1000);
                } catch (Exception e) { 
                    e.printStackTrace(); 
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    private void handleStart() {
        if (blackBoxCore.isInstalled(BGMI_PACKAGE, USER_ID)) {
            copyObbFilesAndLaunch();
        } else {
            installGame();
        }
    }

    private void installGame() {
        Toast.makeText(this, "Installing In Container...", Toast.LENGTH_SHORT).show();
        InstallResult res = blackBoxCore.installPackageAsUser(BGMI_PACKAGE, USER_ID);
        if (res.success) {
            copyObbFilesAndLaunch();
        } else {
            Toast.makeText(this, "Installation Failed: " + res.msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void copyObbFilesAndLaunch() {
        fileCopyTask.copyObbFolderAsync(BGMI_PACKAGE, success -> {
            blackBoxCore.launchApk(BGMI_PACKAGE, USER_ID);
        });
    }

    private void handleStop() {
        blackBoxCore.uninstallPackageAsUser(BGMI_PACKAGE, USER_ID);
        Toast.makeText(this, "Game Uninstalled From Container", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Resume animation when activity resumes
        if (backgroundAnimation != null && !backgroundAnimation.isAnimating()) {
            backgroundAnimation.resumeAnimation();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Pause animation when activity pauses (optional)
        if (backgroundAnimation != null && backgroundAnimation.isAnimating()) {
            backgroundAnimation.pauseAnimation();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up animation resources
        if (backgroundAnimation != null) {
            backgroundAnimation.cancelAnimation();
            backgroundAnimation.clearAnimation();
        }
    }
}