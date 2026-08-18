package com.zoro.loader.libhelper;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

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
                if (success) {
                    String zipPath = zipFile.getAbsolutePath();
                    String outputDir = context.getFilesDir().getAbsolutePath();
                    String password = PASSJKPAPA();

                    if (unzipEncrypted(zipPath, outputDir, password)) {
                        moveSoFiles(new File(outputDir, "loader"));
                        zipFile.delete();
                        Toast.makeText(context, "Online Lib download successful!✅", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, "Failed to extract ZIP. Check ZIP and password.", Toast.LENGTH_LONG).show();
                    }
                } else {
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

            return outputZip.exists();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean unzipEncrypted(String zipPath, String outputDir, String password) {
        try {
            ZipFile zipFile = new ZipFile(zipPath, password.toCharArray());
            zipFile.extractAll(outputDir);
            setPermissions(new File(outputDir));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
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
                } catch (Exception e) {
                    e.printStackTrace();
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
