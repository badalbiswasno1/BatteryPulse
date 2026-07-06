package com.badal.batterypulse.overlay

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.*
import android.view.*
import androidx.core.app.NotificationCompat
import com.badal.batterypulse.R
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var pillView: View
    private lateinit var expandedView: View
    private lateinit var container: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var prefs: SharedPreferences

    private var isExpanded = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastSampleTime = 0L
    private var lastPingMs: Long = -1

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("battery_pulse", MODE_PRIVATE)
        startForegroundNotification()
        setupOverlay()
        startMetricsLoop()
    }

    private fun startForegroundNotification() {
        val channelId = "overlay_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Gaming Overlay", NotificationManager.IMPORTANCE_MIN)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("BatteryPulse Overlay Active")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        container = inflater.inflate(R.layout.overlay_container, null)
        pillView = container.findViewById(R.id.pillLayout)
        expandedView = container.findViewById(R.id.expandedLayout)
        expandedView.visibility = View.GONE

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = prefs.getInt("overlay_x", 20)
        params.y = prefs.getInt("overlay_y", 100)

        windowManager.addView(container, params)

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        prefs.edit().putInt("overlay_x", params.x).putInt("overlay_y", params.y).apply()
                    } else {
                        toggleExpand()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleExpand() {
        isExpanded = !isExpanded
        pillView.visibility = if (isExpanded) View.GONE else View.VISIBLE
        expandedView.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun startMetricsLoop() {
        lastSampleTime = System.currentTimeMillis()
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()

        val runnable = object : Runnable {
            override fun run() {
                updateMetrics()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)

        executor.execute(object : Runnable {
            override fun run() {
                measurePing()
                handler.postDelayed({ executor.execute(this) }, 3000)
            }
        })
    }

    private fun measurePing() {
        try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
            socket.close()
            lastPingMs = System.currentTimeMillis() - start
        } catch (e: Exception) {
            lastPingMs = -1
        }
    }

    private fun updateMetrics() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val battery = registerReceiver(null, filter) ?: return

        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val temp10 = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val health = battery.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val pct = if (scale > 0) ((level.toFloat() / scale) * 100).toInt() else -1
        val tempC = temp10 / 10.0
        val isPlugged = plugged != 0

        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val microAmp = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val milliAmp = microAmp / 1000
        val watts = if (isPlugged) Math.abs((voltage / 1000.0) * (milliAmp / 1000.0)) else 0.0

        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val usedRamPct = (100 - (memInfo.availMem * 100 / memInfo.totalMem)).toInt()

        val now = System.currentTimeMillis()
        val elapsedSec = (now - lastSampleTime) / 1000.0
        val rxNow = TrafficStats.getTotalRxBytes()
        val txNow = TrafficStats.getTotalTxBytes()
        val downKbps = if (elapsedSec > 0) ((rxNow - lastRxBytes) / 1024.0 / elapsedSec) else 0.0
        val upKbps = if (elapsedSec > 0) ((txNow - lastTxBytes) / 1024.0 / elapsedSec) else 0.0
        lastRxBytes = rxNow
        lastTxBytes = txNow
        lastSampleTime = now

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val networkType = when {
            caps == null -> "No connection"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            else -> "Unknown"
        }

        var thermalStatus = "--"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            thermalStatus = when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Normal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                else -> "Emergency"
            }
        }

        var refreshRate = "--"
        try {
            val display = if (Build.VERSION.SDK_INT >= 30) display else windowManager.defaultDisplay
            if (display != null) refreshRate = "${display.refreshRate.toInt()} Hz"
        } catch (e: Exception) {
        }

        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            else -> "Unknown"
        }

        val statusStr = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            else -> "Unknown"
        }

        renderPill(pct, tempC)
        renderExpanded(pct, tempC, voltage, milliAmp, watts, usedRamPct, downKbps, upKbps,
            networkType, thermalStatus, refreshRate, healthStr, statusStr)
    }

    private fun colorFor(value: Double, greenMax: Double, yellowMax: Double, inverse: Boolean = false): Int {
        return if (!inverse) {
            when {
                value <= greenMax -> 0xFF22C55E.toInt()
                value <= yellowMax -> 0xFFEAB308.toInt()
                else -> 0xFFEF4444.toInt()
            }
        } else {
            when {
                value >= greenMax -> 0xFF22C55E.toInt()
                value >= yellowMax -> 0xFFEAB308.toInt()
                else -> 0xFFEF4444.toInt()
            }
        }
    }

    private fun renderPill(pct: Int, tempC: Double) {
        val tvPillBattery = pillView.findViewById<android.widget.TextView>(R.id.tvPillBattery)
        val tvPillTemp = pillView.findViewById<android.widget.TextView>(R.id.tvPillTemp)
        tvPillBattery.text = "$pct%"
        tvPillTemp.text = "${tempC}°C"
        tvPillTemp.setTextColor(colorFor(tempC, 38.0, 42.0))
    }

    private fun renderExpanded(
        pct: Int, tempC: Double, voltage: Int, milliAmp: Int, watts: Double,
        ramPct: Int, downKbps: Double, upKbps: Double, networkType: String,
        thermalStatus: String, refreshRate: String, healthStr: String, statusStr: String
    ) {
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpBattery).text = "$pct% • $statusStr"
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpTemp).apply {
            text = "Temp: ${tempC}°C"
            setTextColor(colorFor(tempC, 38.0, 42.0))
        }
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpVoltage).text = "Voltage: $voltage mV"
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpCurrent).text = "Current: $milliAmp mA"
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpWatts).text =
            if (watts > 0) "Charging: ${"%.1f".format(watts)} W" else "Not charging"
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpHealth).text = "Health: $healthStr"
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpRam).apply {
            text = "RAM: $ramPct%"
            setTextColor(colorFor(ramPct.toDouble(), 60.0, 85.0))
        }
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpNetwork).text = networkType
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpSpeed).text =
            "↓ ${"%.0f".format(downKbps)} KB/s  ↑ ${"%.0f".format(upKbps)} KB/s"
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpPing).apply {
            text = if (lastPingMs >= 0) "Ping: ${lastPingMs}ms" else "Ping: --"
            if (lastPingMs >= 0) setTextColor(colorFor(lastPingMs.toDouble(), 60.0, 120.0))
        }
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpThermal).text = "Thermal: $thermalStatus"
        expandedView.findViewById<android.widget.TextView>(R.id.tvExpRefresh).text = "Refresh: $refreshRate"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        if (::container.isInitialized) {
            try {
                windowManager.removeView(container)
            } catch (e: Exception) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
