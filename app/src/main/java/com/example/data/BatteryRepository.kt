package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class BatteryRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.chargingDao()
    val prefs = PrefsManager(context)

    val chargingHistory: Flow<List<ChargingSession>> = dao.getAllSessions()

    suspend fun addSession(session: ChargingSession) {
        dao.insertSession(session)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun deleteSession(id: Int) {
        dao.deleteSession(id)
    }
}
