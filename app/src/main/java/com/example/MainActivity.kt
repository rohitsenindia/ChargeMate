package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import com.example.receiver.BatteryReceiver
import com.example.receiver.BatteryStateManager
import com.example.ui.ChargeMateViewModel
import com.example.ui.screens.*
import com.example.ui.theme.ChargeMateTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ChargeMateViewModel by viewModels()
    private val batteryReceiver = BatteryReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle auto-launch extra on cold start
        checkAutoLaunchIntent(intent)

        // Register system battery change receiver
        BatteryReceiver.register(applicationContext, batteryReceiver)

        setContent {
            // Apply simple premium clean dark theme
            ChargeMateTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChargeMateApp(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkAutoLaunchIntent(intent)
    }

    private fun checkAutoLaunchIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("auto_launch_charging", false)) {
            viewModel.triggerAutoLaunchCharging()
        }
    }

    override fun onDestroy() {
        // Unregister to prevent system memory leaks
        BatteryReceiver.unregister(applicationContext, batteryReceiver)
        super.onDestroy()
    }
}

@Composable
fun ChargeMateApp(viewModel: ChargeMateViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(onSplashFinished = {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigate = { /* Not needed for single-screen view */ }
            )
        }
    }
}
