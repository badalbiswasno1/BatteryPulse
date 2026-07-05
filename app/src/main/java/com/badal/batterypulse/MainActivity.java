package com.badal.batterypulse;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvVoltage, tvTemp, tvHealth;
    private TextView tvWatts, tvSpeed, tvAmpAvg, tvAmpMin, tvAmpMax, tvPowerSource, tvAmpStatus;
    private TextView tvInsight, tvInsightIcon;
    private LinearLayout insightCard;
    private CircularBatteryView circularBattery;
    private Handler handler = new Handler();
    private BatteryManager batteryManager;

    private int minAmp = Integer.MAX_VALUE;
    private int maxAmp = Integer.MIN_VALUE;
    private long ampSum = 0;
    private int ampCount = 0;
    private int lastAmp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        insightCard = findViewById(R.id.insightCard);
        circularBattery = findViewById(R.id.circularBattery);

        batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);

        updateBatteryInfo();
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
            if (milliAmp > lastAmp) ampStatusStr = "Gaining";
            else if (milliAmp < lastAmp) ampStatusStr = "Losing";
            else ampStatusStr = "Stable";
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
                insightText = "High Drain — Avoid Heavy Usage Now";
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
        }

        handler.postDelayed(this::updateBatteryInfo, 2000);
    }
}
