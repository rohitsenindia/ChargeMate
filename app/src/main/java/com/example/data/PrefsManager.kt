package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chargemate_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_BATTERY_SAVING = "battery_saving"
        private const val KEY_PERSISTENT_NOTIF = "persistent_notif"
        private const val KEY_NOTIF_COMPACT = "notif_compact"
        private const val KEY_SHOW_TEMP = "show_temp"
        private const val KEY_SHOW_POWER = "show_power"
        private const val KEY_SHOW_HEALTH = "show_health"
        private const val KEY_SHOW_REMAINING = "show_remaining"
        
        private const val KEY_TEMP_PROTECT_ENABLED = "temp_protect_enabled"
        private const val KEY_TEMP_THRESHOLD = "temp_threshold"
        private const val KEY_TEMP_ALERT_TYPE = "temp_alert_type"
        
        private const val KEY_CHARGE_GOAL = "charge_goal"
        private const val KEY_GOAL_ALERT = "goal_alert"
        private const val KEY_GOAL_SOUND = "goal_sound"
        private const val KEY_GOAL_VIBE = "goal_vibe"
        
        private const val KEY_SOUND_MODE = "sound_mode"
        private const val KEY_VOICE_TYPE = "voice_type"
        private const val KEY_CUSTOM_AUDIO = "custom_audio"

        // Milestone alerts
        private val MILESTONES = listOf(20, 30, 40, 50, 60, 70, 80, 90, 100)
    }

    var theme: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var lockscreenTheme: String
        get() = prefs.getString("lockscreen_theme", "cyberpunk") ?: "cyberpunk"
        set(value) = prefs.edit().putString("lockscreen_theme", value).apply()

    var autoLaunchOnPlug: Boolean
        get() = prefs.getBoolean("auto_launch_on_plug", true)
        set(value) = prefs.edit().putBoolean("auto_launch_on_plug", value).apply()

    var isBatterySavingMode: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_SAVING, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_SAVING, value).apply()

    var isPersistentNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_PERSISTENT_NOTIF, true)
        set(value) = prefs.edit().putBoolean(KEY_PERSISTENT_NOTIF, value).apply()

    var isCompactNotification: Boolean
        get() = prefs.getBoolean(KEY_NOTIF_COMPACT, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIF_COMPACT, value).apply()

    var showTempOnNotif: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TEMP, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_TEMP, value).apply()

    var showPowerOnNotif: Boolean
        get() = prefs.getBoolean(KEY_SHOW_POWER, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_POWER, value).apply()

    var showHealthOnNotif: Boolean
        get() = prefs.getBoolean(KEY_SHOW_HEALTH, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_HEALTH, value).apply()

    var showRemainingOnNotif: Boolean
        get() = prefs.getBoolean(KEY_SHOW_REMAINING, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_REMAINING, value).apply()

    var isTempProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_TEMP_PROTECT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TEMP_PROTECT_ENABLED, value).apply()

    var tempThreshold: Int
        get() = prefs.getInt(KEY_TEMP_THRESHOLD, 40)
        set(value) = prefs.edit().putInt(KEY_TEMP_THRESHOLD, value).apply()

    var tempAlertType: String
        get() = prefs.getString(KEY_TEMP_ALERT_TYPE, "Voice") ?: "Voice"
        set(value) = prefs.edit().putString(KEY_TEMP_ALERT_TYPE, value).apply()

    var chargeGoal: Int
        get() = prefs.getInt(KEY_CHARGE_GOAL, 80)
        set(value) = prefs.edit().putInt(KEY_CHARGE_GOAL, value).apply()

    var isGoalAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_GOAL_ALERT, true)
        set(value) = prefs.edit().putBoolean(KEY_GOAL_ALERT, value).apply()

    var isGoalSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_GOAL_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_GOAL_SOUND, value).apply()

    var isGoalVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_GOAL_VIBE, true)
        set(value) = prefs.edit().putBoolean(KEY_GOAL_VIBE, value).apply()

    var soundMode: String
        get() = prefs.getString(KEY_SOUND_MODE, "Voice") ?: "Voice"
        set(value) = prefs.edit().putString(KEY_SOUND_MODE, value).apply()

    var voiceType: String
        get() = prefs.getString(KEY_VOICE_TYPE, "Female US") ?: "Female US"
        set(value) = prefs.edit().putString(KEY_VOICE_TYPE, value).apply()

    var customAudioPath: String?
        get() = prefs.getString(KEY_CUSTOM_AUDIO, null)
        set(value) = prefs.edit().putString(KEY_CUSTOM_AUDIO, value).apply()

    fun isMilestoneEnabled(percentage: Int): Boolean {
        // Default to true for requested milestones
        val defaultVal = percentage == 20 || percentage == 30 || percentage == 40 || percentage == 50 || 
                percentage == 60 || percentage == 70 || percentage == 80 || percentage == 90 || percentage == 100
        return prefs.getBoolean("milestone_$percentage", defaultVal)
    }

    fun setMilestoneEnabled(percentage: Int, enabled: Boolean) {
        prefs.edit().putBoolean("milestone_$percentage", enabled).apply()
    }

    var customMilestones: Set<Int>
        get() {
            val set = prefs.getStringSet("custom_milestones_set", null)
            if (set == null) {
                return setOf(20, 30, 40, 50, 60, 70, 80, 90, 100)
            }
            return set.mapNotNull { it.toIntOrNull() }.toSet()
        }
        set(value) {
            prefs.edit().putStringSet("custom_milestones_set", value.map { it.toString() }.toSet()).apply()
        }

    // Onboarding status
    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()
}
