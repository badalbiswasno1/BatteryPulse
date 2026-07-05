package com.badal.batterypulse;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class HelpCenterActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_center);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvEmail = findViewById(R.id.tvEmail);
        tvEmail.setText("badalbiswas0045@gmail.com");

        findViewById(R.id.btnCopyEmail).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("email", "badalbiswas0045@gmail.com"));
            Toast.makeText(this, "Email copied", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnEmailUs).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse("mailto:badalbiswas0045@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "BatteryPulse Support");
            startActivity(Intent.createChooser(intent, "Send email"));
        });

        SharedPreferences prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);
        String crash = prefs.getString("last_crash", "");

        TextView tvCrashLog = findViewById(R.id.tvCrashLog);
        if (crash.isEmpty()) {
            tvCrashLog.setText("No recent crash detected.");
        } else {
            tvCrashLog.setText(crash);
        }

        findViewById(R.id.btnCopyCrash).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("crash", crash.isEmpty() ? "No crash log" : crash));
            Toast.makeText(this, "Crash log copied", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnClearCrash).setOnClickListener(v -> {
            prefs.edit().remove("last_crash").apply();
            tvCrashLog.setText("No recent crash detected.");
            Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show();
        });
    }
}
