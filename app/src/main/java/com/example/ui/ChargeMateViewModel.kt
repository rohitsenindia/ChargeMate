package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BatteryRepository
import com.example.data.ChargingSession
import com.example.receiver.BatteryState
import com.example.receiver.BatteryStateManager
import com.example.service.ChargingForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.speech.tts.TextToSpeech
import android.media.RingtoneManager
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager

class ChargeMateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BatteryRepository(application.applicationContext)
    val prefs = repository.prefs

    // Observed Battery State (Real or Simulated)
    val batteryState: StateFlow<BatteryState> = BatteryStateManager.state

    // Charging History from DB
    val chargingHistory: StateFlow<List<ChargingSession>> = repository.chargingHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // History filtering
    private val _historyFilter = MutableStateFlow("Week") // "Today", "Week", "Month"
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    // Filtered History
    val filteredHistory: StateFlow<List<ChargingSession>> = combine(
        chargingHistory,
        _historyFilter
    ) { history, filter ->
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        when (filter) {
            "Today" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startOfToday = calendar.timeInMillis
                history.filter { it.timestamp >= startOfToday }
            }
            "Week" -> {
                val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
                history.filter { it.timestamp >= sevenDaysAgo }
            }
            "Month" -> {
                val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                history.filter { it.timestamp >= thirtyDaysAgo }
            }
            else -> history
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Simulation Controls (State for slider manipulation)
    var simPercentage by mutableStateOf(67)
    var simIsCharging by mutableStateOf(true)
    var simPowerWatts by mutableStateOf(58.0)
    var simTemperature by mutableStateOf(33.0)
    var simType by mutableStateOf("Fast Charging") // "Slow", "Fast Charging", "Super Fast", "Wireless"
    var simVoltage by mutableStateOf(4.1)

    // Observable App Preference States to trigger reactive recompositions
    var appTheme by mutableStateOf(prefs.theme)
    var lockscreenTheme by mutableStateOf(prefs.lockscreenTheme)
    var autoLaunchOnPlug by mutableStateOf(prefs.autoLaunchOnPlug)
    var shouldAutoRouteToLiveCharging by mutableStateOf(false)
    var isBatterySaving by mutableStateOf(prefs.isBatterySavingMode)
    var isPersistentNotification by mutableStateOf(prefs.isPersistentNotificationEnabled)
    var isCompactNotif by mutableStateOf(prefs.isCompactNotification)
    var showTempOnNotif by mutableStateOf(prefs.showTempOnNotif)
    var showPowerOnNotif by mutableStateOf(prefs.showPowerOnNotif)
    var showHealthOnNotif by mutableStateOf(prefs.showHealthOnNotif)
    var showRemainingOnNotif by mutableStateOf(prefs.showRemainingOnNotif)

    var isTempProtectEnabled by mutableStateOf(prefs.isTempProtectionEnabled)
    var tempThreshold by mutableStateOf(prefs.tempThreshold)
    var tempAlertType by mutableStateOf(prefs.tempAlertType)

    var chargeGoal by mutableStateOf(prefs.chargeGoal)
    var isGoalAlert by mutableStateOf(prefs.isGoalAlertEnabled)
    var isGoalSound by mutableStateOf(prefs.isGoalSoundEnabled)
    var isGoalVibrate by mutableStateOf(prefs.isGoalVibrationEnabled)

    var soundMode by mutableStateOf(prefs.soundMode)
    var voiceType by mutableStateOf(prefs.voiceType)

    // Real-time power readings list for rendering the live charging speed graph
    val powerHistory = androidx.compose.runtime.mutableStateListOf<Float>().apply {
        addAll(listOf(12f, 15f, 14f, 18f, 22f, 25f, 24f, 28f, 32f, 35f, 34f, 38f, 42f, 40f, 45f))
    }

    init {
        // Automatically sync system theme if user didn't set custom one
        viewModelScope.launch {
            batteryState.collect { state ->
                val currentPower = if (state.isCharging) state.powerWatts.toFloat() else 0f
                powerHistory.add(currentPower)
                if (powerHistory.size > 30) {
                    powerHistory.removeAt(0)
                }
            }
        }
    }

    // Preferences setters
    fun updateTheme(newTheme: String) {
        prefs.theme = newTheme
        appTheme = newTheme
    }

    fun updateLockscreenTheme(newTheme: String) {
        prefs.lockscreenTheme = newTheme
        lockscreenTheme = newTheme
    }

    fun updateAutoLaunchOnPlug(enabled: Boolean) {
        prefs.autoLaunchOnPlug = enabled
        autoLaunchOnPlug = enabled
    }

    fun triggerAutoLaunchCharging() {
        shouldAutoRouteToLiveCharging = true
    }

    fun clearAutoRouteTrigger() {
        shouldAutoRouteToLiveCharging = false
    }

    fun updateBatterySaving(enabled: Boolean) {
        prefs.isBatterySavingMode = enabled
        isBatterySaving = enabled
    }

    fun updatePersistentNotification(enabled: Boolean) {
        prefs.isPersistentNotificationEnabled = enabled
        isPersistentNotification = enabled
        toggleForegroundServiceIfNeeded()
    }

    fun updateCompactNotif(enabled: Boolean) {
        prefs.isCompactNotification = enabled
        isCompactNotif = enabled
    }

    fun updateNotifDetail(temp: Boolean, power: Boolean, health: Boolean, remaining: Boolean) {
        prefs.showTempOnNotif = temp
        prefs.showPowerOnNotif = power
        prefs.showHealthOnNotif = health
        prefs.showRemainingOnNotif = remaining

        showTempOnNotif = temp
        showPowerOnNotif = power
        showHealthOnNotif = health
        showRemainingOnNotif = remaining
    }

    fun updateTempProtection(enabled: Boolean, threshold: Int, alertType: String) {
        prefs.isTempProtectionEnabled = enabled
        prefs.tempThreshold = threshold
        prefs.tempAlertType = alertType

        isTempProtectEnabled = enabled
        tempThreshold = threshold
        tempAlertType = alertType
    }

    fun updateChargeGoal(goal: Int, alert: Boolean, sound: Boolean, vibrate: Boolean) {
        prefs.chargeGoal = goal
        prefs.isGoalAlertEnabled = alert
        prefs.isGoalSoundEnabled = sound
        prefs.isGoalVibrationEnabled = vibrate

        chargeGoal = goal
        isGoalAlert = alert
        isGoalSound = sound
        isGoalVibrate = vibrate
    }

    fun updateSoundSettings(mode: String, voice: String) {
        prefs.soundMode = mode
        prefs.voiceType = voice

        soundMode = mode
        voiceType = voice
    }

    var customMilestones by mutableStateOf(prefs.customMilestones)
        private set

    fun addCustomMilestone(pct: Int) {
        if (pct in 1..100) {
            val updated = prefs.customMilestones.toMutableSet().apply { add(pct) }
            prefs.customMilestones = updated
            customMilestones = updated
            prefs.setMilestoneEnabled(pct, true)
        }
    }

    fun removeCustomMilestone(pct: Int) {
        val updated = prefs.customMilestones.toMutableSet().apply { remove(pct) }
        prefs.customMilestones = updated
        customMilestones = updated
    }

    fun setMilestoneEnabled(percentage: Int, enabled: Boolean) {
        prefs.setMilestoneEnabled(percentage, enabled)
        // Refresh custom milestones state
        customMilestones = prefs.customMilestones
    }

    fun isMilestoneEnabled(percentage: Int): Boolean {
        return prefs.isMilestoneEnabled(percentage)
    }

    fun setFilter(filter: String) {
        _historyFilter.value = filter
    }

    // Simulation Trigger
    fun toggleSimulation(active: Boolean) {
        BatteryStateManager.setSimulationMode(active)
        if (active) {
            applySimulationState()
        } else {
            toggleForegroundServiceIfNeeded()
        }
    }

    fun updateSimulationParams(
        percent: Int = simPercentage,
        charging: Boolean = simIsCharging,
        power: Double = simPowerWatts,
        temp: Double = simTemperature,
        type: String = simType,
        voltage: Double = simVoltage
    ) {
        simPercentage = percent
        simIsCharging = charging
        simPowerWatts = power
        simTemperature = temp
        simType = type
        simVoltage = voltage

        if (BatteryStateManager.isSimulationActive) {
            applySimulationState()
        }
    }

    private fun applySimulationState() {
        val currentAmps = if (simIsCharging) simPowerWatts / simVoltage else -(simPowerWatts / simVoltage)
        val minPerPercent = when (simType) {
            "Super Fast" -> 0.4
            "Fast Charging" -> 0.8
            "Wireless" -> 1.3
            "Slow" -> 1.6
            else -> 1.0
        }
        val remaining = if (simIsCharging) ((100 - simPercentage) * minPerPercent).toInt() else -1

        val state = BatteryState(
            percentage = simPercentage,
            isCharging = simIsCharging,
            chargingType = if (simIsCharging) simType else "Discharging",
            temperatureCelsius = simTemperature,
            voltageVolts = simVoltage,
            currentAmperes = currentAmps,
            powerWatts = simPowerWatts,
            timeRemainingMinutes = remaining,
            healthStatus = if (simTemperature > 43.0) "Overheat" else "Good",
            source = if (simIsCharging) (if (simType == "Wireless") "Wireless" else "AC Charger") else "Battery",
            healthScore = 95,
            cycleCount = 142,
            timeTo50Min = if (simIsCharging && simPercentage < 50) ((50 - simPercentage) * minPerPercent).toInt() else 0,
            timeTo80Min = if (simIsCharging && simPercentage < 80) ((80 - simPercentage) * minPerPercent).toInt() else 0,
            timeTo100Min = if (simIsCharging && simPercentage < 100) ((100 - simPercentage) * minPerPercent).toInt() else 0,
            isSimulated = true
        )
        BatteryStateManager.updateState(state)

        // Ensure service triggers based on simulated plug-in
        toggleForegroundServiceIfNeeded()
    }

    private fun toggleForegroundServiceIfNeeded() {
        val context = getApplication<Application>().applicationContext
        val state = BatteryStateManager.state.value
        val serviceIntent = Intent(context, ChargingForegroundService::class.java)

        if (state.isCharging && isPersistentNotification) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                // Ignore background start exceptions
            }
        } else {
            context.stopService(serviceIntent)
        }
    }

    // Dynamic AI Insights Engine (Analyzes historical Room records)
    fun generateInsights(): List<String> {
        val history = chargingHistory.value
        val insights = mutableListOf<String>()

        if (history.isEmpty()) {
            insights.add("📊 No charging history recorded yet. Plug in your charger to start tracking battery intelligence.")
            insights.add("💡 Recommendation: Keep battery level between 20% and 80% to maximize lithium-ion life cycle.")
            insights.add("⚡ Charging Tip: Fast charging generates heat. Try charging in a cool room to keep temp below 35°C.")
            return insights
        }

        // 1. Analyze charging habits
        val avgStart = history.map { it.startPercentage }.average().toInt()
        val avgEnd = history.map { it.endPercentage }.average().toInt()
        insights.add("📉 You usually plug in around $avgStart% and unplug near $avgEnd%.")

        if (avgEnd > 90) {
            insights.add("💡 Longevity Alert: You consistently charge to $avgEnd%. Setting a Charge Goal of 80% could double your battery's cycle lifespan!")
        } else {
            insights.add("🔋 Healthy habit detected! By unplugging around $avgEnd%, you are preventing accelerated cell aging.")
        }

        // 2. Temp warning insights
        val hotSessions = history.count { it.peakTemperatureCelsius > 40 }
        if (hotSessions > 0) {
            insights.add("🔥 Heat Warning: $hotSessions session(s) exceeded 40°C. High temperature causes permanent capacity degradation.")
        } else {
            insights.add("❄️ Cold Core: Your battery temperature stayed below 35°C all week. Excellent thermal protection.")
        }

        // 3. Speed metrics
        val superFastCount = history.count { it.averagePowerWatts > 25 }
        if (superFastCount > 0) {
            insights.add("⚡ Supercharger detected! You charged 18% faster today by leveraging higher wattage feeds.")
        }

        return insights
    }

    // Seed mock history so user can easily test filters, analytics, graphs
    fun generateSampleHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

            // Seed 5 historical records
            val sessions = listOf(
                ChargingSession(
                    dateString = sdf.format(Date(System.currentTimeMillis() - 4 * 3600000)),
                    timestamp = System.currentTimeMillis() - 4 * 3600000,
                    startPercentage = 18,
                    endPercentage = 80,
                    durationMinutes = 23,
                    averagePowerWatts = 52.0,
                    peakTemperatureCelsius = 34.5,
                    healthImpact = "Low"
                ),
                ChargingSession(
                    dateString = sdf.format(Date(System.currentTimeMillis() - 24 * 3600000)),
                    timestamp = System.currentTimeMillis() - 24 * 3600000,
                    startPercentage = 35,
                    endPercentage = 95,
                    durationMinutes = 40,
                    averagePowerWatts = 18.0,
                    peakTemperatureCelsius = 38.0,
                    healthImpact = "Medium"
                ),
                ChargingSession(
                    dateString = sdf.format(Date(System.currentTimeMillis() - 3 * 24 * 3600000)),
                    timestamp = System.currentTimeMillis() - 3 * 24 * 3600000,
                    startPercentage = 12,
                    endPercentage = 100,
                    durationMinutes = 65,
                    averagePowerWatts = 15.0,
                    peakTemperatureCelsius = 44.2,
                    healthImpact = "High"
                ),
                ChargingSession(
                    dateString = sdf.format(Date(System.currentTimeMillis() - 6 * 24 * 3600000)),
                    timestamp = System.currentTimeMillis() - 6 * 24 * 3600000,
                    startPercentage = 22,
                    endPercentage = 78,
                    durationMinutes = 20,
                    averagePowerWatts = 45.0,
                    peakTemperatureCelsius = 33.1,
                    healthImpact = "Low"
                ),
                ChargingSession(
                    dateString = sdf.format(Date(System.currentTimeMillis() - 12 * 24 * 3600000)),
                    timestamp = System.currentTimeMillis() - 12 * 24 * 3600000,
                    startPercentage = 40,
                    endPercentage = 80,
                    durationMinutes = 15,
                    averagePowerWatts = 58.0,
                    peakTemperatureCelsius = 32.8,
                    healthImpact = "Low"
                )
            )

            for (session in sessions) {
                repository.addSession(session)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }
    
    fun deleteSession(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSession(id)
        }
    }

    private var testTts: TextToSpeech? = null
    private var isTestTtsInitialized = false

    fun testVoiceAlert(context: Context) {
        if (testTts == null) {
            testTts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    testTts?.language = Locale.US
                    isTestTtsInitialized = true
                    speakTestMessage()
                }
            }
        } else if (isTestTtsInitialized) {
            speakTestMessage()
        }
    }

    private fun speakTestMessage() {
        val voiceStr = if (soundMode == "Voice") {
            "Testing voice profile $voiceType. Current simulated battery is $simPercentage percent."
        } else {
            "Voice announcements are currently set to $soundMode mode. Change mode to Voice in delivery options to hear this during actual charging."
        }
        testTts?.speak(voiceStr, TextToSpeech.QUEUE_FLUSH, null, "chargemate_test_speak")
    }

    fun testSoundAlert(context: Context) {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alarmUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }
                prepare()
                start()
            }
            mediaPlayer.setOnCompletionListener {
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        testTts?.stop()
        testTts?.shutdown()
    }
}
