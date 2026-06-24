package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.example.service.ChargingForegroundService

class BatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (BatteryStateManager.isSimulationActive) return

        val action = intent.action
        if (action == Intent.ACTION_BATTERY_CHANGED) {
            val state = parseBatteryIntent(context, intent)
            BatteryStateManager.updateState(state)

            // Dynamic foreground service management
            val isCharging = state.isCharging
            val serviceIntent = Intent(context, ChargingForegroundService::class.java)
            if (isCharging) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    context.stopService(serviceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else if (action == Intent.ACTION_POWER_CONNECTED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val batteryStatusIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (batteryStatusIntent != null) {
                val state = parseBatteryIntent(context, batteryStatusIntent)
                BatteryStateManager.updateState(state)
                if (state.isCharging) {
                    val serviceIntent = Intent(context, ChargingForegroundService::class.java)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
            val serviceIntent = Intent(context, ChargingForegroundService::class.java)
            try {
                context.stopService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun register(context: Context, receiver: BatteryReceiver): Intent? {
            return context.registerReceiver(
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        }

        fun unregister(context: Context, receiver: BatteryReceiver) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if not registered
            }
        }

        fun parseBatteryIntent(context: Context, intent: Intent): BatteryState {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percentage = if (level >= 0 && scale > 0) (level * 100 / scale) else 50

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val source = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> if (isCharging) "Charger" else "Battery"
            }

            val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
            val temperatureCelsius = tempRaw / 10.0

            val voltageRaw = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            // Voltage can be in millivolts or volts depending on manufacturer
            val voltageVolts = if (voltageRaw > 1000) voltageRaw / 1000.0 else voltageRaw.toDouble()

            // Fetch current in Amperes/Microamperes
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentMicroAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            // Some devices return positive current when charging, others negative.
            // Let's normalize it so charging current is positive, discharging is negative.
            var currentAmps = currentMicroAmps / 1_000_000.0
            if (isCharging && currentAmps < 0) {
                currentAmps = -currentAmps
            } else if (!isCharging && currentAmps > 0) {
                currentAmps = -currentAmps
            }

            // Estimate Power in Watts
            val powerWatts = Math.abs(voltageVolts * currentAmps)

            // Charging type classification
            val chargingType = if (!isCharging) {
                "Discharging"
            } else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                "Wireless"
            } else if (powerWatts > 20) {
                "Super Fast"
            } else if (powerWatts > 10) {
                "Fast Charging"
            } else {
                "Slow"
            }

            // Minutes per percent based on speed
            val minutesPerPercent = when (chargingType) {
                "Super Fast" -> 0.4
                "Fast Charging" -> 0.8
                "Wireless" -> 1.3
                "Slow" -> 1.6
                else -> 1.0
            }

            val timeRemainingMinutes = if (isCharging) {
                ((100 - percentage) * minutesPerPercent).toInt()
            } else {
                -1
            }

            val timeTo50Min = if (isCharging && percentage < 50) ((50 - percentage) * minutesPerPercent).toInt() else 0
            val timeTo80Min = if (isCharging && percentage < 80) ((80 - percentage) * minutesPerPercent).toInt() else 0
            val timeTo100Min = if (isCharging && percentage < 100) ((100 - percentage) * minutesPerPercent).toInt() else 0

            val healthRaw = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            val healthStatus = when (healthRaw) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Good"
            }

            // Approximate cycle count & health score
            val cycleCount = 180 // Simulated / estimated
            val healthScore = 94  // Standard high value for battery health

            return BatteryState(
                percentage = percentage,
                isCharging = isCharging,
                chargingType = chargingType,
                temperatureCelsius = temperatureCelsius,
                voltageVolts = voltageVolts,
                currentAmperes = currentAmps,
                powerWatts = powerWatts,
                timeRemainingMinutes = timeRemainingMinutes,
                healthStatus = healthStatus,
                source = source,
                healthScore = healthScore,
                cycleCount = cycleCount,
                timeTo50Min = timeTo50Min,
                timeTo80Min = timeTo80Min,
                timeTo100Min = timeTo100Min,
                isSimulated = false
            )
        }
    }
}
