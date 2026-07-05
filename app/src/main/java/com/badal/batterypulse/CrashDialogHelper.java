package com.badal.batterypulse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.widget.Toast;

public class CrashDialogHelper {

    public static void showIfPresent(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("battery_pulse", Activity.MODE_PRIVATE);
        String crash = prefs.getString("last_crash", "");

        if (crash.isEmpty()) return;

        new AlertDialog.Builder(activity)
                .setTitle("App Crashed")
                .setMessage(crash)
                .setPositiveButton("Copy", (dialog, which) -> {
                    ClipboardManager cm = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("crash", crash));
                    Toast.makeText(activity, "Copied", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Dismiss", (dialog, which) ->
                        prefs.edit().remove("last_crash").apply())
                .setCancelable(false)
                .show();
    }
}
