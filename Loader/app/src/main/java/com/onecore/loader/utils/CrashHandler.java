package com.onecore.loader.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.onecore.loader.activity.CrashActivity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private final Context context;

    public CrashHandler(Context context) {
        this.context = context.getApplicationContext() == null ? context : context.getApplicationContext();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        String software = "SDK: " + Build.VERSION.SDK_INT + "\n"
                + "Android: " + Build.VERSION.RELEASE + "\n"
                + "Model: " + Build.MODEL + "\n"
                + "Manufacturer: " + Build.MANUFACTURER + "\n";
        String date = Calendar.getInstance().getTime().toString();

        DiagnosticLogger.exception("UNCAUGHT EXCEPTION thread=" + thread.getName(), exception);
        DiagnosticLogger.log("Software=" + software.replace('\n', ' ') + " Date=" + date);
        Log.e("OneCoreCrash", stackTrace.toString());

        try {
            Intent intent = new Intent(context, CrashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("Error", stackTrace.toString());
            intent.putExtra("Software", software);
            intent.putExtra("Date", date);
            context.startActivity(intent);
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        } catch (Throwable displayError) {
            DiagnosticLogger.exception("Crash screen could not be displayed", displayError);
        }

        Process.killProcess(Process.myPid());
        System.exit(2);
    }
}
