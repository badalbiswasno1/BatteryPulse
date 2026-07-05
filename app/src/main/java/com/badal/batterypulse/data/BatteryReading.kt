package com.badal.batterypulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_readings")
data class BatteryReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val percentage: Int,
    val voltageMv: Int,
    val temperatureC: Double,
    val currentMa: Int,
    val isPlugged: Boolean,
    val status: String
)
