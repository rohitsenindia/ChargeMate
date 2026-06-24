package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.os.VibrationEffect
import android.speech.tts.TextToSpeech
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.BatteryRepository
import com.example.data.ChargingSession
import com.example.receiver.BatteryReceiver
import com.example.receiver.BatteryState
import com.example.receiver.BatteryStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChargingForegroundService : Service(), TextToSpeech.OnInitListener {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: BatteryRepository
    private var batteryReceiver: BatteryReceiver? = null

    // Session stats tracking
    private var startPercentage: Int = -1
    private var maxTemperature: Double = 0.0
    private var powerSum: Double = 0.0
    private var readingsCount: Int = 0
    private val startTimeMs = System.currentTimeMillis()

    // TTS & Alarms
    private var tts: TextToSpeech? = null
    private var ttsInitialized = false
    private val spokenMilestones = mutableSetOf<Int>()
    private var targetGoalReachedSpoken = false
    private var highTempAlertSpoken = false

    companion object {
        const val CHANNEL_ID = "chargemate_service_channel"
        const val NOTIFICATION_ID = 41295
    }

    override fun onCreate() {
        super.onCreate()
        repository = BatteryRepository(applicationContext)
        createNotificationChannel()

        // Setup TextToSpeech
        tts = TextToSpeech(applicationContext, this)

        // Register internal receiver to listen to battery changes during service lifecycle
        batteryReceiver = BatteryReceiver()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)

        // Start Foreground immediately with a placeholder notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(BatteryStateManager.state.value),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(BatteryStateManager.state.value))
        }

        // Monitor state changes to update the notification dynamically
        BatteryStateManager.state
            .onEach { state ->
                updateSessionStats(state)
                checkThresholdsAndAlert(state)
                updateNotification(state)
            }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            val voiceType = repository.prefs.voiceType
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
                
                try {
                    val voices = tts?.voices
                    val selectedVoice = voices?.firstOrNull { voice ->
                        val voiceNameLower = voice.name.lowercase()
                        when (voiceType) {
                            "Assistant" -> voiceNameLower.contains("assistant") || voiceNameLower.contains("neutral")
                            "Male" -> voiceNameLower.contains("male") && !voiceNameLower.contains("female")
                            "Female" -> voiceNameLower.contains("female")
                            else -> false
                        }
                    } ?: voices?.firstOrNull { voice -> voice.locale.language == "en" }
                    if (selectedVoice != null) {
                        tts?.voice = selectedVoice
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ttsInitialized = true
        }
    }

    private fun updateSessionStats(state: BatteryState) {
        if (startPercentage == -1) {
            startPercentage = state.percentage
        }
        if (state.temperatureCelsius > maxTemperature) {
            maxTemperature = state.temperatureCelsius
        }
        powerSum += state.powerWatts
        readingsCount++
    }

    private fun checkThresholdsAndAlert(state: BatteryState) {
        val prefs = repository.prefs
        val currentPercent = state.percentage

        // 1. Check Custom Milestone Notifications
        val customMilestones = prefs.customMilestones
        if (customMilestones.contains(currentPercent)) {
            if (!spokenMilestones.contains(currentPercent)) {
                spokenMilestones.add(currentPercent)
                if (prefs.isMilestoneEnabled(currentPercent)) {
                    if (currentPercent == 100) {
                        triggerFullChargeAlert()
                    } else {
                        triggerMilestoneAlert(currentPercent)
                    }
                }
            }
        }

        // 2. Check Charge Goal Reached
        val goal = prefs.chargeGoal
        if (goal < 100 && currentPercent >= goal && !targetGoalReachedSpoken) {
            targetGoalReachedSpoken = true
            if (prefs.isGoalAlertEnabled) {
                triggerGoalReachedAlert(goal)
            }
        } else if (currentPercent < goal) {
            targetGoalReachedSpoken = false // Reset if unplugged or discharged
        }

        // 3. Check Temperature Protection
        val tempThreshold = prefs.tempThreshold
        if (prefs.isTempProtectionEnabled && state.temperatureCelsius >= tempThreshold) {
            if (!highTempAlertSpoken) {
                highTempAlertSpoken = true
                triggerTemperatureWarning(state.temperatureCelsius.toInt())
            }
        } else if (state.temperatureCelsius < tempThreshold - 2) {
            highTempAlertSpoken = false // Reset threshold with a small hysteresis
        }
    }

    private fun playAlarmChime() {
        if (repository.prefs.soundMode == "Silent") return
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
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
                isLooping = false
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

    private fun triggerFullChargeAlert() {
        val message = "Battery is fully charged. Please unplug."
        playAlarmChime()
        if (ttsInitialized && repository.prefs.soundMode == "Voice") {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "chargemate_full_1")
            for (i in 2..3) {
                tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "chargemate_full_$i")
            }
        }
    }

    private fun speakText(text: String) {
        if (ttsInitialized && repository.prefs.soundMode == "Voice") {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chargemate_voice")
        }
    }

    private fun playVibe() {
        // Disabled based on user preference to prevent unwanted device vibration
    }

    private fun triggerMilestoneAlert(percentage: Int) {
        val message = "Battery reached $percentage percent."
        playAlarmChime()
        speakText(message)
    }

    private fun triggerGoalReachedAlert(goal: Int) {
        val message = "Charging complete. Target goal of $goal percent reached."
        playAlarmChime()
        speakText(message)
    }

    private fun triggerTemperatureWarning(temp: Int) {
        val message = "Battery temperature is high. Current temperature is $temp degrees Celsius. Consider unplugging."
        playAlarmChime()
        speakText(message)
    }

    private fun postUrgentNotification(title: String, message: String) {
        // Disabled heads-up notifications based on user request to keep status clean and fast
    }

    private fun updateNotification(state: BatteryState) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: BatteryState): Notification {
        val prefs = repository.prefs
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isCompact = prefs.isCompactNotification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        if (isCompact) {
            builder.setContentTitle("⚡ ${state.percentage}% • ${state.chargingType}")
            val details = StringBuilder()
            if (prefs.showPowerOnNotif) details.append("${state.powerWatts.toInt()}W  ")
            if (prefs.showRemainingOnNotif && state.timeRemainingMinutes > 0) details.append("${state.timeRemainingMinutes}m left")
            builder.setContentText(details.toString().trim())
        } else {
            builder.setContentTitle("ChargeMate AI • ${state.percentage}%")
            val details = StringBuilder()
            if (prefs.showPowerOnNotif) details.append("Power: ${state.powerWatts.toInt()}W  •  ")
            if (prefs.showTempOnNotif) details.append("Temp: ${state.temperatureCelsius}°C\n")
            if (prefs.showHealthOnNotif) details.append("Health Score: ${state.healthScore}/100  •  ")
            if (prefs.showRemainingOnNotif && state.timeRemainingMinutes > 0) {
                details.append("Remaining: ${state.timeRemainingMinutes} min")
            } else if (state.percentage == 100) {
                details.append("Fully Charged")
            } else {
                details.append("Status: ${state.source}")
            }
            builder.setContentText("Status: Charging • ${state.chargingType}")
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(details.toString()))
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Smart Charging Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time battery status in the notification drawer while charging"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        // Unregister local battery receiver
        batteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Save session stats to database before closing
        val endPercentage = BatteryStateManager.state.value.percentage
        val durationMs = System.currentTimeMillis() - startTimeMs
        val durationMin = (durationMs / 60000).toInt().coerceAtLeast(1)

        val avgPower = if (readingsCount > 0) (powerSum / readingsCount) else 15.0
        val peakTemp = if (maxTemperature > 0) maxTemperature else 33.0

        val healthImpact = when {
            peakTemp > 43.0 -> "High"
            peakTemp > 38.0 || endPercentage > 90 -> "Medium"
            else -> "Low"
        }

        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date())

        serviceScope.launch(Dispatchers.IO) {
            // Save to database only if we charged at least some amount or for at least 15 seconds (to avoid spamming tiny sessions)
            if (startPercentage != -1 && (endPercentage - startPercentage >= 1 || durationMs >= 15000)) {
                val session = ChargingSession(
                    dateString = dateStr,
                    startPercentage = startPercentage,
                    endPercentage = endPercentage,
                    durationMinutes = durationMin,
                    averagePowerWatts = avgPower,
                    peakTemperatureCelsius = peakTemp,
                    healthImpact = healthImpact
                )
                repository.addSession(session)
            }
        }

        // Clean up TTS
        tts?.stop()
        tts?.shutdown()

        serviceJob.cancel()
        super.onDestroy()
    }
}
