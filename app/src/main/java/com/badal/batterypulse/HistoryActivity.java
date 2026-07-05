package com.badal.batterypulse;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends Activity {

    private LinearLayout container;
    private List<String> allEntries = new ArrayList<>();

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

        container = findViewById(R.id.historyContainer);
        EditText searchBox = findViewById(R.id.searchBox);
        TextView btnExport = findViewById(R.id.btnExportCsv);

        loadEntries();
        renderEntries(allEntries);

        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntries(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnExport.setOnClickListener(v -> exportCsv());
    }

    private void loadEntries() {
        SharedPreferences prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);
        String sessions = prefs.getString("sessions", "");
        allEntries.clear();
        for (String e : sessions.split(";")) {
            if (!e.trim().isEmpty()) allEntries.add(e);
        }
    }

    private void filterEntries(String query) {
        List<String> filtered = new ArrayList<>();
        for (String e : allEntries) {
            if (e.toLowerCase().contains(query.toLowerCase())) filtered.add(e);
        }
        renderEntries(filtered);
    }

    private void renderEntries(List<String> entries) {
        container.removeAllViews();

        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No charging sessions found");
            empty.setTextColor(0xFF9E9E9E);
            empty.setTextSize(15f);
            container.addView(empty);
            return;
        }

        for (int i = entries.size() - 1; i >= 0; i--) {
            String[] parts = entries.get(i).split("\\|");
            if (parts.length < 4) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 0, 0, 24);

            TextView tv = new TextView(this);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(15f);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tv.setText(parts[0] + "\n" + parts[1] + "% → " + parts[2] + "%   •   " + parts[3] + " min");

            TextView delBtn = new TextView(this);
            delBtn.setText("✕");
            delBtn.setTextColor(0xFFEF4444);
            delBtn.setTextSize(18f);
            delBtn.setPadding(20, 0, 0, 0);

            final String entryToDelete = entries.get(entries.size() - 1 - (entries.size() - 1 - i));
            final String targetEntry = entries.get(i);
            delBtn.setOnClickListener(v -> deleteEntry(targetEntry));

            row.addView(tv);
            row.addView(delBtn);
            container.addView(row);
        }
    }

    private void deleteEntry(String entry) {
        SharedPreferences prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);
        allEntries.remove(entry);

        StringBuilder sb = new StringBuilder();
        for (String e : allEntries) sb.append(e).append(";");

        prefs.edit().putString("sessions", sb.toString()).apply();
        renderEntries(allEntries);
        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
    }

    private void exportCsv() {
        try {
            File dir = new File(getExternalFilesDir(null), "exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "battery_history.csv");

            FileWriter writer = new FileWriter(file);
            writer.write("Start Time,Start %,End %,Duration (min)\n");
            for (String e : allEntries) {
                String[] parts = e.split("\\|");
                if (parts.length < 4) continue;
                writer.write(parts[0] + "," + parts[1] + "," + parts[2] + "," + parts[3] + "\n");
            }
            writer.close();

            Toast.makeText(this, "Exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("path", file.getAbsolutePath()));
        } catch (IOException e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
