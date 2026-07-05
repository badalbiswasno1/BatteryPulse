package com.badal.batterypulse;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class StatsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            finish();
        });
        findViewById(R.id.navSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        });

        TextView tvTotalSessions = findViewById(R.id.tvTotalSessions);
        TextView tvAvgDuration = findViewById(R.id.tvAvgDuration);
        TextView tvAvgGain = findViewById(R.id.tvAvgGain);

        SharedPreferences prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);
        String sessions = prefs.getString("sessions", "");

        if (sessions.isEmpty()) {
            tvTotalSessions.setText("0");
            tvAvgDuration.setText("-- min");
            tvAvgGain.setText("--%");
            return;
        }

        String[] entries = sessions.split(";");
        int count = 0;
        long totalDuration = 0;
        int totalGain = 0;

        for (String e : entries) {
            if (e.trim().isEmpty()) continue;
            String[] parts = e.split("\\|");
            if (parts.length < 4) continue;
            try {
                int startPct = Integer.parseInt(parts[1]);
                int endPct = Integer.parseInt(parts[2]);
                int duration = Integer.parseInt(parts[3]);
                totalGain += (endPct - startPct);
                totalDuration += duration;
                count++;
            } catch (NumberFormatException ignored) {
            }
        }

        tvTotalSessions.setText(String.valueOf(count));
        tvAvgDuration.setText(count > 0 ? (totalDuration / count) + " min" : "-- min");
        tvAvgGain.setText(count > 0 ? (totalGain / count) + "%" : "--%");
    }
}
