package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "charging_sessions")
data class ChargingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String,           // e.g. "June 24"
    val startPercentage: Int,
    val endPercentage: Int,
    val durationMinutes: Int,
    val averagePowerWatts: Double,
    val peakTemperatureCelsius: Double,
    val healthImpact: String          // "Low", "Medium", "High"
)

@Dao
interface ChargingDao {
    @Query("SELECT * FROM charging_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChargingSession>>

    @Insert
    suspend fun insertSession(session: ChargingSession)

    @Query("DELETE FROM charging_sessions")
    suspend fun clearHistory()

    @Query("DELETE FROM charging_sessions WHERE id = :id")
    suspend fun deleteSession(id: Int)
}
