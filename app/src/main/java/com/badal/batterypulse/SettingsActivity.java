package com.badal.batterypulse;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.Toast;

import com.badal.batterypulse.overlay.OverlayService;

public class SettingsActivity extends Activity {

    private Switch swOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });
        findViewById(R.id.navStats).setOnClickListener(v -> {
            startActivity(new Intent(this, StatsActivity.class));
            finish();
        });

        findViewById(R.id.btnHelpCenter).setOnClickListener(v ->
                startActivity(new Intent(this, HelpCenterActivity.class)));

        SharedPreferences prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);

        Switch swNotifications = findViewById(R.id.swNotifications);
        Switch swFahrenheit = findViewById(R.id.swFahrenheit);
        swOverlay = findViewById(R.id.swOverlay);

        swNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));
        swFahrenheit.setChecked(prefs.getBoolean("use_fahrenheit", false));
        swOverlay.setChecked(prefs.getBoolean("overlay_enabled", false) && Settings.canDrawOverlays(this));

        swNotifications.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("notifications_enabled", checked).apply());

        swFahrenheit.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("use_fahrenheit", checked).apply());

        swOverlay.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Grant overlay permission next", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 200);
                } else {
                    startOverlay();
                }
            } else {
                stopOverlay();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
            if (Settings.canDrawOverlays(this)) {
                startOverlay();
                swOverlay.setChecked(true);
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
                swOverlay.setChecked(false);
            }
        }
    }

    private void startOverlay() {
        SharedPreferences prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);
        prefs.edit().putBoolean("overlay_enabled", true).apply();
        Intent serviceIntent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "Overlay started", Toast.LENGTH_SHORT).show();
    }

    private void stopOverlay() {
        SharedPreferences prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);
        prefs.edit().putBoolean("overlay_enabled", false).apply();
        stopService(new Intent(this, OverlayService.class));
        Toast.makeText(this, "Overlay stopped", Toast.LENGTH_SHORT).show();
    }
}
