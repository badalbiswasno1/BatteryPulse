package com.badal.batterypulse

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.badal.batterypulse.data.AppDatabase
import com.badal.batterypulse.data.BatteryReading

class BatteryMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val battery = context.registerReceiver(null, filter) ?: return Result.success()

        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val temp10 = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val microAmp = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val milliAmp = microAmp / 1000

        val pct = if (scale > 0) ((level.toFloat() / scale) * 100).toInt() else -1
        val isPlugged = plugged != 0
        val tempC = temp10 / 10.0

        val statusStr = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            else -> "Unknown"
        }

        val reading = BatteryReading(
            timestamp = System.currentTimeMillis(),
            percentage = pct,
            voltageMv = voltage,
            temperatureC = tempC,
            currentMa = milliAmp,
            isPlugged = isPlugged,
            status = statusStr
        )

        val db = AppDatabase.getInstance(context)
        db.batteryReadingDao().insert(reading)

        val cutoff = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        db.batteryReadingDao().deleteOlderThan(cutoff)

        return Result.success()
    }
}
