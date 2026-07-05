package com.badal.batterypulse;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HistoryActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navStats).setOnClickListener(v -> {
            startActivity(new Intent(this, StatsActivity.class));
            finish();
        });
        findViewById(R.id.navSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        });

        LinearLayout container = findViewById(R.id.historyContainer);
        SharedPreferences prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);
        String sessions = prefs.getString("sessions", "");

        if (sessions.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No charging sessions recorded yet");
            empty.setTextColor(0xFF9E9E9E);
            empty.setTextSize(15f);
            container.addView(empty);
            return;
        }

        String[] entries = sessions.split(";");
        for (int i = entries.length - 1; i >= 0; i--) {
            if (entries[i].trim().isEmpty()) continue;
            String[] parts = entries[i].split("\\|");
            if (parts.length < 4) continue;

            TextView tv = new TextView(this);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(15f);
            tv.setPadding(0, 0, 0, 24);
            tv.setText(parts[0] + "\n" + parts[1] + "% → " + parts[2] + "%   •   " + parts[3] + " min");
            container.addView(tv);
        }
    }
}
