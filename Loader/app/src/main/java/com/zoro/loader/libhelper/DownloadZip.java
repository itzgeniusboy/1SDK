package com.zoro.loader.libhelper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.zoro.loader.utils.FLog;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadZip {

    private final Context context;
    private final ProgressDialog progressDialog;
    private final ExecutorService executor;
    private final Handler handler;
    private String ZIP_FILE_NAME = "Moranix.zip";

    private native String PASSJKPAPA();

    public DownloadZip(Context context) {
        this.context = context;
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);

        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
    }

    public void startDownload(String downloadUrl) {
        FLog.info("Native library download started. url=" + downloadUrl);
        File zipFile = new File(context.getFilesDir(), ZIP_FILE_NAME);
        if (zipFile.exists()) {
            progressDialog.setTitle("Updating");
        } else {
            progressDialog.setTitle("⚡Online Lib downloading⚡");
        }
        progressDialog.setMessage("Starting download...");
        progressDialog.show();

        executor.execute(() -> {
            boolean success = downloadFile(downloadUrl);

            handler.post(() -> {
                progressDialog.setMessage("Finishing...");
                FLog.info("Native library download completed success=" + success);
                if (success) {
                    String zipPath = zipFile.getAbsolutePath();
                    String outputDir = context.getFilesDir().getAbsolutePath();
                    String password = PASSJKPAPA();
                    FLog.info("Extracting native library archive: " + zipPath);

                    if (unzipEncrypted(zipPath, outputDir, password)) {
                        moveSoFiles(new File(outputDir, "loader"));
                        zipFile.delete();
                        Toast.makeText(context, "Online Lib download successful!✅", Toast.LENGTH_LONG).show();
                    } else {
                        FLog.error("Native library archive extraction failed");
                        Toast.makeText(context, "Failed to extract ZIP. Check ZIP and password.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    FLog.error("Native library download failed");
                    Toast.makeText(context, "Download failed. Check internet connection.❌", Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
            });
        });
    }

    private boolean downloadFile(String downloadUrl) {
        File outputZip = new File(context.getFilesDir(), ZIP_FILE_NAME);
        try (InputStream input = new URL(downloadUrl).openStream();
             OutputStream output = new FileOutputStream(outputZip)) {

            HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
            connection.connect();
            int lengthOfFile = connection.getContentLength();

            byte[] data = new byte[4096];
            int total = 0, count;
            while ((count = input.read(data)) != -1) {
                total += count;
                int progress = (total * 100) / lengthOfFile;
                handler.post(() -> progressDialog.setMessage("Download: " + progress + "%"));
                output.write(data, 0, count);
            }

            boolean exists = outputZip.exists();
            FLog.info("Downloaded archive exists=" + exists + ", bytes=" + (exists ? outputZip.length() : 0));
            return exists;

        } catch (Exception e) {
            FLog.error("Native library download exception", e);
            return false;
        }
    }

    private boolean unzipEncrypted(String zipPath, String outputDir, String password) {
        try {
            ZipFile zipFile = new ZipFile(zipPath, password.toCharArray());
            zipFile.extractAll(outputDir);
            setPermissions(new File(outputDir));
            FLog.info("Native library archive extracted successfully");
            return true;
        } catch (Exception e) {
            FLog.error("Native library extraction exception", e);
            return false;
        }
    }

    private void moveSoFiles(File loaderFolder) {
        File outputDir = context.getFilesDir();
        if (!loaderFolder.exists()) loaderFolder.mkdirs();

        File[] files = outputDir.listFiles((dir, name) -> name.endsWith(".so"));
        if (files != null) {
            for (File soFile : files) {
                try {
                    Files.move(soFile.toPath(), new File(loaderFolder, soFile.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    FLog.info("Moved native library: " + soFile.getName());
                } catch (Exception e) {
                    FLog.error("Failed moving native library " + soFile.getName(), e);
                }
            }
        }
    }

    private void setPermissions(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            for (File file : fileOrDir.listFiles()) {
                setPermissions(file);
            }
        }
        fileOrDir.setExecutable(true, false);
        fileOrDir.setReadable(true, false);
        fileOrDir.setWritable(true, false);
    }
}
