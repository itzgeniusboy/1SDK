package com.zoro.loader.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.zoro.loader.R;
import com.zoro.loader.BoxApplication;
import com.zoro.loader.libhelper.DownloadZip;
import com.zoro.loader.utils.FLog;
import com.zoro.loader.utils.Prefs;

import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class LoginActivity extends AppCompatActivity {

    private static final int REQUEST_MANAGE_STORAGE_PERMISSION = 100;
    private static final int REQUEST_MANAGE_UNKNOWN_APP_SOURCES = 200;
    private static final String PREFS_NAME = "com.zoro.loader.prefs";
    private static final String PREF_PERMISSIONS_GRANTED = "permissions_granted";

    public static String USERKEY;
    private Button btnSignIn;
    private Dialog loadingDialog;
    private TextView errorTextView;
    private LottieAnimationView backgroundAnimation;
    private TextView getKeyTextView; // ✅ Get Key TextView
    
    public static native String FixCrash();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // --- LOTTIE ANIMATION BACKGROUND ---
        backgroundAnimation = findViewById(R.id.backgroundAnimation);
        if (backgroundAnimation != null) {
            backgroundAnimation.setSpeed(0.8f);
            backgroundAnimation.setRepeatCount(LottieDrawable.INFINITE);
            backgroundAnimation.playAnimation();
        }
        
        // ✅ GET KEY TEXTVIEW CLICK - TELEGRAM DM
        getKeyTextView = findViewById(R.id.GetKey);
        getKeyTextView.setOnClickListener(v -> {
            try {
                // 🔥 अपना Telegram ID यहाँ डालो
                String telegramUsername = "@ZOROADMINSERVER";
                Intent intent = new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("https://t.me/ZOROADMINSERVER" + telegramUsername));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Telegram app नहीं मिला", Toast.LENGTH_SHORT).show();
            }
        });
            
        // --- तुम्हारा पुराना ओरिजिनल लॉजिक ---
        checkAndRequestPermissions();
        initDesign();
        
        if (!BoxApplication.isNativeLoaded()) {
            FLog.error("Native library MCoreEsp is unavailable; skipping FixCrash native call");
            Toast.makeText(this, "Native SDK load failed. Please share Loader log.", Toast.LENGTH_LONG).show();
        } else {
            try {
                String crashFixUrl = FixCrash();
                FLog.info("FixCrash URL obtained: " + (crashFixUrl == null ? "null" : "length=" + crashFixUrl.length()));
                if (crashFixUrl != null && !crashFixUrl.trim().isEmpty()) {
                    new DownloadZip(LoginActivity.this).startDownload(crashFixUrl);
                } else {
                    FLog.error("FixCrash returned an empty URL; download skipped");
                }
            } catch (Throwable throwable) {
                FLog.error("FixCrash native call failed", throwable);
                Toast.makeText(this, "Native startup failed. Please share Loader log.", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void checkAndRequestPermissions() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean permissionsGranted = prefs.getBoolean(PREF_PERMISSIONS_GRANTED, false);

        if (!isStoragePermissionGranted()) {
            requestStoragePermissionDirect();
        } else if (!canRequestPackageInstalls()) {
            requestUnknownAppPermissionsDirect();
        } else {
            prefs.edit().putBoolean(PREF_PERMISSIONS_GRANTED, true).apply();
        }
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermissionDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_MANAGE_STORAGE_PERMISSION);
        }
    }

    private boolean canRequestPackageInstalls() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    private void requestUnknownAppPermissionsDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_MANAGE_UNKNOWN_APP_SOURCES);
        }
    }

    private void initDesign() {
        Prefs prefs = new Prefs(this);
        TextView textUsername = findViewById(R.id.userkey);
        textUsername.setText(prefs.getSt("USER", ""));

        btnSignIn = findViewById(R.id.login);
        btnSignIn.setOnClickListener(v -> {
            String userKey = textUsername.getText().toString().trim();
            if (!userKey.isEmpty()) {
                prefs.setSt("USER", userKey);
                USERKEY = userKey;
                login(userKey);
            } else {
                textUsername.setError("Please Enter Your License");
            }
        });

        ImageView paste = findViewById(R.id.paste);
        paste.setOnClickListener(view -> {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboardManager != null && clipboardManager.getPrimaryClip() != null && clipboardManager.getPrimaryClip().getItemCount() > 0) {
                String pastedText = clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
                if (pastedText.length() > 5) {
                    textUsername.setText(pastedText);
                } else {
                    Toast.makeText(this, "Invalid key in clipboard.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No text in clipboard.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoadingDialog(String errorMessage) {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(this);
            loadingDialog.setContentView(R.layout.ios_loading);
            loadingDialog.setCancelable(false);
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            errorTextView = loadingDialog.findViewById(R.id.errorText);
        }

        ProgressBar progressBar = loadingDialog.findViewById(R.id.progressBar);
        ImageView errorIcon = loadingDialog.findViewById(R.id.errorIcon);
        Button okButton = loadingDialog.findViewById(R.id.okButton);

        if (errorMessage != null && !errorMessage.isEmpty()) {
            progressBar.setVisibility(ProgressBar.GONE);
            errorIcon.setVisibility(ImageView.VISIBLE);
            errorTextView.setVisibility(TextView.VISIBLE);
            errorTextView.setText("ERROR: " + errorMessage);

            okButton.setVisibility(Button.VISIBLE);
            okButton.setOnClickListener(v -> dismissLoadingDialog());
        } else {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            errorIcon.setVisibility(ImageView.GONE);
            errorTextView.setVisibility(TextView.GONE);
            okButton.setVisibility(Button.GONE);
        }

        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void login(String userKey) {
        showLoadingDialog(null);

        Handler loginHandler = new Handler(msg -> {
            dismissLoadingDialog();
            if (msg.what == 0) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                Toast.makeText(this, "Login Success✅", Toast.LENGTH_SHORT).show();
                finish();
            } else if (msg.what == 1) {
                String errorMessage = (String) msg.obj;
                showLoadingDialog(errorMessage);
            }
            return true;
        });

        new Thread(() -> {
            String result = Check(this, userKey);
            if ("OK".equals(result)) {
                loginHandler.sendEmptyMessage(0);
            } else {
                Message msg = Message.obtain();
                msg.what = 1;
                msg.obj = result;
                loginHandler.sendMessage(msg);
            }
        }).start();
    }

    private static native String Check(Context mContext, String userKey);
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION) {
            checkAndRequestPermissions();
        } else if (requestCode == REQUEST_MANAGE_UNKNOWN_APP_SOURCES) {
            if (canRequestPackageInstalls()) {
                // ✅ FIXED: android.os.Process use kiya
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (backgroundAnimation != null && !backgroundAnimation.isAnimating()) {
            backgroundAnimation.resumeAnimation();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundAnimation != null && backgroundAnimation.isAnimating()) {
            backgroundAnimation.pauseAnimation();
        }
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