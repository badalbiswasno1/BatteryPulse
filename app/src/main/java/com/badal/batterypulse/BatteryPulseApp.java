package com.badal.batterypulse;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

import java.io.PrintWriter;
import java.io.StringWriter;

public class BatteryPulseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));

            SharedPreferences prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);
            prefs.edit().putString("last_crash", sw.toString()).apply();

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                BatteryMonitorWorker.class, 15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "battery_monitor_work",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }
}
