package com.example.receiver

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BatteryState(
    val percentage: Int = 67,
    val isCharging: Boolean = false,
    val chargingType: String = "Fast Charging", // "Slow", "Fast", "Super Fast", "Wireless", "Discharging"
    val temperatureCelsius: Double = 33.0,
    val voltageVolts: Double = 4.12,
    val currentAmperes: Double = 2.4, // Amperes
    val powerWatts: Double = 15.0,     // Watts
    val timeRemainingMinutes: Int = 18,
    val healthStatus: String = "Good",
    val source: String = "AC Charger", // "Battery", "AC", "USB", "Wireless"
    val healthScore: Int = 95,
    val cycleCount: Int = 142,
    val timeTo50Min: Int = 2,
    val timeTo80Min: Int = 11,
    val timeTo100Min: Int = 24,
    // Simulated state flag
    val isSimulated: Boolean = false
)

object BatteryStateManager {
    private val _state = MutableStateFlow(BatteryState())
    val state: StateFlow<BatteryState> = _state

    // Tracks if simulation is active
    var isSimulationActive = false
        private set

    fun updateState(newState: BatteryState) {
        _state.value = newState
    }

    fun setSimulationMode(active: Boolean) {
        isSimulationActive = active
        if (!active) {
            // Revert to default state or wait for system broadcast
            _state.value = BatteryState(isSimulated = false)
        } else {
            _state.value = _state.value.copy(isSimulated = true)
        }
    }
}
