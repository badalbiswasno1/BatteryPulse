package com.badal.batterypulse;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tvLevel, tvStatus, tvVoltage, tvTemp, tvHealth;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLevel = findViewById(R.id.tvLevel);
        tvStatus = findViewById(R.id.tvStatus);
        tvVoltage = findViewById(R.id.tvVoltage);
        tvTemp = findViewById(R.id.tvTemp);
        tvHealth = findViewById(R.id.tvHealth);

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
            int temp = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            int health = battery.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);

            int pct = (int) ((level / (float) scale) * 100);

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

            tvLevel.setText("Battery Level: " + pct + "%");
            tvStatus.setText("Status: " + statusStr);
            tvVoltage.setText("Voltage: " + voltage + " mV");
            tvTemp.setText("Temperature: " + (temp / 10.0) + " °C");
            tvHealth.setText("Health: " + healthStr);
        }

        handler.postDelayed(this::updateBatteryInfo, 2000);
    }
}
