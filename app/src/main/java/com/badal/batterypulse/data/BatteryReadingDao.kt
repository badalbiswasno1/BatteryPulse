package com.badal.batterypulse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BatteryReadingDao {

    @Insert
    suspend fun insert(reading: BatteryReading)

    @Query("SELECT * FROM battery_readings WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getReadingsSince(sinceTimestamp: Long): List<BatteryReading>

    @Query("SELECT * FROM battery_readings ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): BatteryReading?

    @Query("DELETE FROM battery_readings WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM battery_readings")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM battery_readings")
    suspend fun count(): Int

    @Query("SELECT MAX(temperatureC) FROM battery_readings WHERE timestamp >= :sinceTimestamp")
    suspend fun getMaxTemp(sinceTimestamp: Long): Double?

    @Query("SELECT AVG(currentMa) FROM battery_readings WHERE timestamp >= :sinceTimestamp")
    suspend fun getAvgCurrent(sinceTimestamp: Long): Double?
}
