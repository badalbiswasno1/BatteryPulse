package com.badal.batterypulse;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);

        Switch swNotifications = findViewById(R.id.swNotifications);
        Switch swFahrenheit = findViewById(R.id.swFahrenheit);

        swNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));
        swFahrenheit.setChecked(prefs.getBoolean("use_fahrenheit", false));

        swNotifications.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("notifications_enabled", checked).apply());

        swFahrenheit.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean("use_fahrenheit", checked).apply());
    }
}
