package com.badal.batterypulse;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvVoltage, tvTemp, tvHealth;
    private TextView tvWatts, tvSpeed, tvAmpAvg, tvAmpMin, tvAmpMax, tvPowerSource, tvAmpStatus;
    private TextView tvInsight, tvInsightIcon, tvTimeEstimate, tvBatteryScore, tvRemainingCard;
    private TextView range5m, range30m, range1h;
    private TextView placeholderCurrent, placeholderVoltage, placeholderTemp, placeholderPct;
    private LinearLayout insightCard;
    private CircularBatteryView circularBattery;
    private LineGraphView graphCurrent, graphVoltage, graphTemp, graphPct;
    private Handler handler = new Handler();
    private BatteryManager batteryManager;
    private SharedPreferences prefs;
    private Vibrator vibrator;

    private int minAmp = Integer.MAX_VALUE;
    private int maxAmp = Integer.MIN_VALUE;
    private long ampSum = 0;
    private int ampCount = 0;
    private int lastAmp = 0;

    private boolean wasPlugged = false;
    private long chargeStartTime = 0;
    private int chargeStartPct = 0;

    private boolean notifiedFull = false;
    private boolean notifiedLow = false;
    private boolean notifiedHot = false;
    private Boolean lastPluggedState = null;

    private List<Long> timestamps = new ArrayList<>();
    private List<Float> currentHistory = new ArrayList<>();
    private List<Float> voltageHistory = new ArrayList<>();
    private List<Float> tempHistory = new ArrayList<>();
    private List<Float> pctHistory = new ArrayList<>();
    private static final int MAX_POINTS = 1800;
    private int selectedRangeMinutes = 5;

    private String cachedEstimate = "Estimating...";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CrashDialogHelper.showIfPresent(this);

        tvVoltage = findViewById(R.id.tvVoltage);
        tvTemp = findViewById(R.id.tvTemp);
        tvHealth = findViewById(R.id.tvHealth);
        tvWatts = findViewById(R.id.tvWatts);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvAmpAvg = findViewById(R.id.tvAmpAvg);
        tvAmpMin = findViewById(R.id.tvAmpMin);
        tvAmpMax = findViewById(R.id.tvAmpMax);
        tvPowerSource = findViewById(R.id.tvPowerSource);
        tvAmpStatus = findViewById(R.id.tvAmpStatus);
        tvInsight = findViewById(R.id.tvInsight);
        tvInsightIcon = findViewById(R.id.tvInsightIcon);
        tvTimeEstimate = findViewById(R.id.tvTimeEstimate);
        tvBatteryScore = findViewById(R.id.tvBatteryScore);
        tvRemainingCard = findViewById(R.id.tvRemainingCard);
        insightCard = findViewById(R.id.insightCard);
        circularBattery = findViewById(R.id.circularBattery);
        graphCurrent = findViewById(R.id.graphCurrent);
        graphVoltage = findViewById(R.id.graphVoltage);
        graphTemp = findViewById(R.id.graphTemp);
        graphPct = findViewById(R.id.graphPct);
        placeholderCurrent = findViewById(R.id.placeholderCurrent);
        placeholderVoltage = findViewById(R.id.placeholderVoltage);
        placeholderTemp = findViewById(R.id.placeholderTemp);
        placeholderPct = findViewById(R.id.placeholderPct);
        range5m = findViewById(R.id.range5m);
        range30m = findViewById(R.id.range30m);
        range1h = findViewById(R.id.range1h);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        range5m.setOnClickListener(v -> selectRange(5, range5m));
        range30m.setOnClickListener(v -> selectRange(30, range30m));
        range1h.setOnClickListener(v -> selectRange(60, range1h));

        findViewById(R.id.navHistory).setOnClickListener(v -> {
            haptic();
            startActivity(new Intent(this, HistoryActivity.class));
        });
        findViewById(R.id.navStats).setOnClickListener(v -> {
            haptic();
            startActivity(new Intent(this, StatsActivity.class));
        });
        findViewById(R.id.navSettings).setOnClickListener(v -> {
            haptic();
            startActivity(new Intent(this, SettingsActivity.class));
        });

        batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE);

        NotificationHelper.createChannel(this);

        if (Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        updateBatteryInfo();
    }

    private void haptic() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private void selectRange(int minutes, TextView selected) {
        haptic();
        selectedRangeMinutes = minutes;
        TextView[] all = {range5m, range30m, range1h};
        for (TextView t : all) {
            t.setBackgroundResource(R.drawable.card_bg);
            t.setTextColor(getColor(R.color.text_white));
        }
        selected.setBackgroundResource(R.drawable.card_bg_green);
        selected.setTextColor(getColor(R.color.bg_black));
        refreshGraphs();
    }

    private void updateBatteryInfo() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = registerReceiver(null, filter);

        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            int temp10 = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            int health = battery.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            int plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            String technology = battery.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

            boolean isPlugged = plugged != 0;
            double tempC = temp10 / 10.0;

            int pct = (int) ((level / (float) scale) * 100);

            int microAmp = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            int milliAmp = microAmp / 1000;

            if (milliAmp < minAmp) minAmp = milliAmp;
            if (milliAmp > maxAmp) maxAmp = milliAmp;
            ampSum += milliAmp;
            ampCount++;
            int ampAvg = (int) (ampSum / ampCount);

            double watts = 0;
            String speedLabel = "Unplugged";

            if (isPlugged) {
                watts = Math.abs((voltage / 1000.0) * (milliAmp / 1000.0));
                if (watts >= 18) speedLabel = "Fast";
                else if (watts >= 10) speedLabel = "Normal";
                else speedLabel = "Slow";
            }

            String statusStr;
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    statusStr = "Charging";
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    statusStr = "Discharging";
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    statusStr = "Full";
                    break;
                default:
                    statusStr = "Unknown";
            }

            String healthStr;
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_GOOD:
                    healthStr = "Good";
                    break;
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    healthStr = "Overheat";
                    break;
                case BatteryManager.BATTERY_HEALTH_DEAD:
                    healthStr = "Dead";
                    break;
                default:
                    healthStr = "Unknown";
            }

            String powerSourceStr;
            switch (plugged) {
                case BatteryManager.BATTERY_PLUGGED_AC:
                    powerSourceStr = "Strong (AC)";
                    break;
                case BatteryManager.BATTERY_PLUGGED_USB:
                    powerSourceStr = "USB";
                    break;
                case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                    powerSourceStr = "Wireless";
                    break;
                default:
                    powerSourceStr = "Unplugged";
            }

            String ampStatusStr;
            if (isPlugged) {
                if (milliAmp > lastAmp) ampStatusStr = "Gaining";
                else if (milliAmp < lastAmp) ampStatusStr = "Losing";
                else ampStatusStr = "Stable";
            } else {
                int absNow = Math.abs(milliAmp);
                int absLast = Math.abs(lastAmp);
                if (absNow > absLast) ampStatusStr = "Drain increasing";
                else if (absNow < absLast) ampStatusStr = "Drain decreasing";
                else ampStatusStr = "Stable";
            }
            lastAmp = milliAmp;

            int ringColor;
            String insightIcon;
            String insightText;
            int insightBg;

            if (tempC >= 45) {
                ringColor = Color.parseColor("#EF4444");
                insightIcon = "🔥";
                insightText = "High Temperature Detected";
                insightBg = Color.parseColor("#3A1414");
            } else if (tempC >= 40) {
                ringColor = Color.parseColor("#EAB308");
                insightIcon = "⚠️";
                insightText = "Battery Warm — Consider Removing Case";
                insightBg = Color.parseColor("#3A2E0A");
            } else if (isPlugged && watts > 0 && watts < 7) {
                ringColor = Color.parseColor("#EAB308");
                insightIcon = "🐢";
                insightText = "Charging is Slow";
                insightBg = Color.parseColor("#3A2E0A");
            } else if (!isPlugged && milliAmp < -1500) {
                ringColor = Color.parseColor("#EAB308");
                insightIcon = "🎮";
                insightText = "Avoid Gaming While Draining Fast";
                insightBg = Color.parseColor("#3A2E0A");
            } else {
                ringColor = Color.parseColor("#22C55E");
                insightIcon = "✅";
                insightText = "Battery is Healthy";
                insightBg = Color.parseColor("#0F2A1A");
            }

            circularBattery.setData(pct, ringColor, statusStr);
            insightCard.setBackgroundColor(insightBg);
            tvInsightIcon.setText(insightIcon);
            tvInsight.setText(insightText);

            tvVoltage.setText(voltage + " mV");
            tvTemp.setText(tempC + " °C");
            tvHealth.setText(healthStr);
            tvWatts.setText(String.format("%.1f watts", watts));
            tvSpeed.setText(speedLabel);
            tvAmpAvg.setText(ampAvg + " mA");
            tvAmpMin.setText("Min: " + minAmp + " mA");
            tvAmpMax.setText("Max: " + maxAmp + " mA");
            tvPowerSource.setText(powerSourceStr);
            tvAmpStatus.setText(ampStatusStr);

            long now = System.currentTimeMillis();
            timestamps.add(now);
            currentHistory.add((float) milliAmp);
            voltageHistory.add((float) voltage);
            tempHistory.add((float) tempC);
            pctHistory.add((float) pct);

            if (timestamps.size() > MAX_POINTS) {
                timestamps.remove(0);
                currentHistory.remove(0);
                voltageHistory.remove(0);
                tempHistory.remove(0);
                pctHistory.remove(0);
            }

            refreshGraphs();

            String estimateText = calculateTimeEstimate(isPlugged, pct);
            tvTimeEstimate.setText(estimateText);
            tvRemainingCard.setText(estimateText);

            int batteryScore = calculateBatteryScore(tempC, health, milliAmp, isPlugged);
            tvBatteryScore.setText(String.valueOf(batteryScore));

            handleNotifications(pct, isPlugged, tempC);
            handleSessionTracking(isPlugged, pct);
            updateWidget(pct, statusStr);

            if (lastPluggedState != null && lastPluggedState != isPlugged) {
                cachedEstimate = "Estimating...";
                boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
                if (notificationsEnabled) {
                    if (isPlugged) {
                        NotificationHelper.notify(this, 4, "Charger Connected", "Battery at " + pct + "%");
                    } else {
                        NotificationHelper.notify(this, 5, "Charger Disconnected", "Battery at " + pct + "%");
                    }
                }
            }
            lastPluggedState = isPlugged;
        }

        handler.postDelayed(this::updateBatteryInfo, 2000);
    }

    private void refreshGraphs() {
        long cutoff = System.currentTimeMillis() - (selectedRangeMinutes * 60000L);

        List<Float> curr = filterByTime(currentHistory, cutoff);
        List<Float> volt = filterByTime(voltageHistory, cutoff);
        List<Float> temp = filterByTime(tempHistory, cutoff);
        List<Float> pctL = filterByTime(pctHistory, cutoff);

        setGraphOrPlaceholder(graphCurrent, placeholderCurrent, curr, Color.parseColor("#22C55E"));
        setGraphOrPlaceholder(graphVoltage, placeholderVoltage, volt, Color.parseColor("#3B82F6"));
        setGraphOrPlaceholder(graphTemp, placeholderTemp, temp, Color.parseColor("#EF4444"));
        setGraphOrPlaceholder(graphPct, placeholderPct, pctL, Color.parseColor("#EAB308"));
    }

    private List<Float> filterByTime(List<Float> values, long cutoff) {
        List<Float> result = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            if (timestamps.get(i) >= cutoff) {
                result.add(values.get(i));
            }
        }
        return result;
    }

    private void setGraphOrPlaceholder(LineGraphView graph, TextView placeholder, List<Float> data, int color) {
        if (data.size() < 2) {
            graph.setVisibility(View.GONE);
            placeholder.setVisibility(View.VISIBLE);
        } else {
            graph.setVisibility(View.VISIBLE);
            placeholder.setVisibility(View.GONE);
            graph.setData(data, color);
        }
    }

    private String calculateTimeEstimate(boolean isPlugged, int pct) {
        int chargeCounterUah = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        int currentNowUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

        if (chargeCounterUah <= 0 || currentNowUa == Integer.MIN_VALUE || pct <= 0) {
            return cachedEstimate;
        }

        double currentAbsUa = Math.abs(currentNowUa);
        if (currentAbsUa < 1000) {
            return cachedEstimate;
        }

        if (isPlugged) {
            if (currentNowUa <= 0) return cachedEstimate;
            double estimatedFullCapacityUah = chargeCounterUah / (pct / 100.0);
            double remainingToFullUah = estimatedFullCapacityUah - chargeCounterUah;
            if (remainingToFullUah <= 0) {
                cachedEstimate = "Almost full";
                return cachedEstimate;
            }
            double hours = remainingToFullUah / currentAbsUa;
            int totalMinutes = (int) Math.round(hours * 60);
            cachedEstimate = "Est. " + formatMinutes(totalMinutes) + " to full (approx)";
        } else {
            double hours = chargeCounterUah / currentAbsUa;
            int totalMinutes = (int) Math.round(hours * 60);
            cachedEstimate = "Est. " + formatMinutes(totalMinutes) + " to empty (approx)";
        }

        return cachedEstimate;
    }



    private String formatMinutes(int totalMinutes) {
        if (totalMinutes < 1) return "< 1 min";
        if (totalMinutes < 60) return totalMinutes + " min";
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        return hours + "h " + mins + "m";
    }

    private int calculateBatteryScore(double tempC, int health, int milliAmp, boolean isPlugged) {
        int score = 100;

        if (tempC > 40) score -= (int) ((tempC - 40) * 4);
        else if (tempC > 35) score -= (int) ((tempC - 35) * 2);

        if (health == BatteryManager.BATTERY_HEALTH_OVERHEAT) score -= 30;
        else if (health == BatteryManager.BATTERY_HEALTH_DEAD) score -= 60;
        else if (health != BatteryManager.BATTERY_HEALTH_GOOD) score -= 10;

        if (isPlugged && Math.abs(milliAmp) > 5000) score -= 10;

        if (score < 0) score = 0;
        if (score > 100) score = 100;
        return score;
    }

    private void handleNotifications(int pct, boolean isPlugged, double tempC) {
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        if (!notificationsEnabled) return;

        if (isPlugged && pct >= 100 && !notifiedFull) {
            NotificationHelper.notify(this, 1, "Battery Full", "Unplug your charger to protect battery health");
            notifiedFull = true;
        }
        if (!isPlugged && pct <= 100) notifiedFull = false;

        if (!isPlugged && pct <= 15 && !notifiedLow) {
            NotificationHelper.notify(this, 2, "Battery Low", "Only " + pct + "% remaining");
            notifiedLow = true;
        }
        if (pct > 15 || isPlugged) notifiedLow = false;

        if (tempC >= 45 && !notifiedHot) {
            NotificationHelper.notify(this, 3, "High Temperature", "Battery is at " + tempC + "°C — consider cooling down");
            notifiedHot = true;
        }
        if (tempC < 42) notifiedHot = false;
    }

    private void handleSessionTracking(boolean isPlugged, int pct) {
        if (isPlugged && !wasPlugged) {
            chargeStartTime = System.currentTimeMillis();
            chargeStartPct = pct;
        }
        if (!isPlugged && wasPlugged && chargeStartTime > 0) {
            long durationMin = (System.currentTimeMillis() - chargeStartTime) / 60000;
            String timestamp = DateFormat.format("dd MMM, hh:mm a", chargeStartTime).toString();
            String entry = timestamp + "|" + chargeStartPct + "|" + pct + "|" + durationMin + ";";
            String existing = prefs.getString("sessions", "");
            prefs.edit().putString("sessions", existing + entry).apply();
            chargeStartTime = 0;
        }
        wasPlugged = isPlugged;
    }

    private void updateWidget(int pct, String statusStr) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName componentName = new ComponentName(this, BatteryWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(componentName);
        if (ids.length > 0) {
            Intent intent = new Intent(this, BatteryWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
        }
    }
}
