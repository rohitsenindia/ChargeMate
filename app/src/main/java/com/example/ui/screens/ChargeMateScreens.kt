package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChargingSession
import com.example.receiver.BatteryState
import com.example.ui.ChargeMateViewModel
import kotlinx.coroutines.delay

// ==========================================
// 1. EXTRAORDINARY CYBERPUNK COLOR PALETTE
// ==========================================
val ElectricCyan = Color(0xFF00F0FF)
val NeonGreen = Color(0xFF39FF14)
val WarningAmber = Color(0xFFFFB800)
val CriticalRed = Color(0xFFFF3B3B)
val CyberDarkBg = Color(0xFF0D1515)
val CyberGlassCard = Color(0xFF161A20).copy(alpha = 0.5f)
val GlassStroke = Color.White.copy(alpha = 0.12f)
val CyberMutedText = Color(0xFFB9CACB)

// ==========================================
// 2. SPLASH SCREEN (FAST, MODERN, DARK)
// ==========================================
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.05f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "Scale"
    )
    val opacity by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "Opacity"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(1200) // Beautiful 1.2s introduction
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(24.dp, CircleShape, spotColor = ElectricCyan)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(ElectricCyan.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            radius = size.width / 1.1f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Bolt Logo",
                    tint = ElectricCyan,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ChargeMate AI",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.testTag("splash_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "BATTERY PROTECTION • SOUND ALARMS",
                color = ElectricCyan.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// 3. MAIN REDESIGNED 4-TAB DASHBOARD SCREEN
// ==========================================
@SuppressLint("BatteryLife")
@Composable
fun HomeScreen(
    viewModel: ChargeMateViewModel,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.batteryState.collectAsState()
    val history by viewModel.filteredHistory.collectAsState()
    val rawHistory by viewModel.chargingHistory.collectAsState()
    val activeFilter by viewModel.historyFilter.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Health, 2: History, 3: Settings

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberDarkBg,
        topBar = {
            // Elegant top bar with logo, title and settings switch
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElectricCyan.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f)), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Logo",
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "ChargeMate AI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricCyan,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Premium Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(ElectricCyan.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f)), RoundedCornerShape(50.dp))
                                .clickable {
                                    Toast.makeText(context, "Premium active for account", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Premium",
                                color = ElectricCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Settings Top Right Button
                        IconButton(
                            onClick = { selectedTab = 3 },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selectedTab == 3) ElectricCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .testTag("top_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (selectedTab == 3) ElectricCyan else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // High fidelity styled navigation bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xFF192122).copy(alpha = 0.9f),
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, GlassStroke),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavBarItem(
                        icon = Icons.Default.Home,
                        label = "Home",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavBarItem(
                        icon = Icons.Default.Favorite,
                        label = "Health",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavBarItem(
                        icon = Icons.Default.History,
                        label = "History",
                        isSelected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavBarItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        isSelected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabContent"
            ) { targetTab ->
                when (targetTab) {
                    0 -> HomeTabContent(state = state, viewModel = viewModel)
                    1 -> HealthTabContent(state = state)
                    2 -> HistoryTabContent(
                        history = history,
                        rawHistory = rawHistory,
                        activeFilter = activeFilter,
                        viewModel = viewModel
                    )
                    3 -> SettingsTabContent(viewModel = viewModel)
                }
            }
        }
    }
}

// ==========================================
// 4. BOTTOM NAVBAR COMPONENT
// ==========================================
@Composable
fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF39FF14).copy(alpha = 0.15f)) // Beautiful soft green background container
                .border(BorderStroke(1.dp, Color(0xFF39FF14).copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NeonGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = NeonGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Column(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onClick() }
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CyberMutedText.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = CyberMutedText.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================
// 5. TAB 0: HOME SCREEN CONTENT (CHARGING RING)
// ==========================================
@Composable
fun HomeTabContent(state: BatteryState, viewModel: ChargeMateViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_halo")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Charging Ring Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CyberGlassCard)
                    .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(24.dp))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Radial pulse backing
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(ElectricCyan.copy(alpha = pulseAlpha), Color.Transparent)
                                ),
                                radius = (size.width / 2f) * pulseRadiusScale
                            )
                        }
                )

                // SVG Circular track simulation
                Canvas(modifier = Modifier.size(210.dp)) {
                    // Dim background path
                    drawCircle(
                        color = Color.White.copy(alpha = 0.03f),
                        radius = size.width / 2.1f,
                        style = Stroke(width = 12.dp.toPx())
                    )

                    // Electric foreground track
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = listOf(ElectricCyan, NeonGreen)
                        ),
                        startAngle = -90f,
                        sweepAngle = (state.percentage * 3.6f),
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center metrics
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${state.percentage}",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 64.sp
                        )
                        Text(
                            text = "%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberMutedText,
                            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Charging status indicator pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (state.isCharging) ElectricCyan.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (state.isCharging) ElectricCyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)
                                ),
                                RoundedCornerShape(50.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isCharging) Icons.Default.FlashOn else Icons.Default.BatteryStd,
                                contentDescription = null,
                                tint = if (state.isCharging) ElectricCyan else CyberMutedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (state.isCharging) "CHARGING" else "DISCHARGING",
                                color = if (state.isCharging) ElectricCyan else CyberMutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Stats Bento Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Current Power (Full Width Span)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CyberGlassCard)
                        .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    // Subtle cyber gradient overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(ElectricCyan.copy(alpha = 0.03f), Color.Transparent)
                                )
                            )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT POWER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberMutedText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%.1f", state.powerWatts),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "W",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }

                        // Beautiful live charging columns graph
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(44.dp)
                        ) {
                            val mockHeights = listOf(0.2f, 0.4f, 0.35f, 0.6f, 0.8f, 1.0f)
                            mockHeights.forEachIndexed { index, heightMultiplier ->
                                val isHigh = index >= 4
                                val barColor = if (isHigh) ElectricCyan else Color.White.copy(alpha = 0.15f)
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .fillMaxHeight(heightMultiplier)
                                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                        .background(barColor)
                                        .shadow(
                                            elevation = if (isHigh) 6.dp else 0.dp,
                                            shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                                            ambientColor = ElectricCyan,
                                            spotColor = ElectricCyan
                                        )
                                )
                            }
                        }
                    }
                }

                // Row with Temp & Time Left
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Temperature Bento Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(CyberGlassCard)
                            .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Thermostat,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "TEMP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberMutedText,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${state.temperatureCelsius.toInt()}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "°C",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CyberMutedText,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }

                    // Time Left Bento Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(CyberGlassCard)
                            .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = CyberMutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "TIME LEFT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberMutedText,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                val minutes = state.timeRemainingMinutes
                                val displayValue = if (minutes > 0) "$minutes" else "On Bat"
                                val displayUnit = if (minutes > 0) "min" else ""
                                Text(
                                    text = displayValue,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (displayUnit.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = displayUnit,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = CyberMutedText,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Smart Insights Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF161A20))
                    .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                // Glow corner
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(NeonGreen.copy(alpha = 0.06f), Color.Transparent)
                                ),
                                radius = size.width / 2.5f,
                                center = Offset(x = 0f, y = size.height / 2f)
                            )
                        }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.12f))
                            .border(BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Smart Insights",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You charged 18% faster today compared to your weekly average.",
                            fontSize = 12.sp,
                            color = CyberMutedText,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. TAB 1: BATTERY HEALTH SCREEN CONTENT
// ==========================================
@Composable
fun HealthTabContent(state: BatteryState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Circular Score
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161A20).copy(alpha = 0.3f))
                        .border(BorderStroke(1.dp, GlassStroke), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Back glow
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(NeonGreen.copy(alpha = 0.1f), Color.Transparent)
                                )
                            )
                    )

                    // Arc ring path
                    Canvas(modifier = Modifier.size(180.dp)) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = size.width / 2.1f,
                            style = Stroke(width = 10.dp.toPx())
                        )
                        drawArc(
                            color = NeonGreen,
                            startAngle = -90f,
                            sweepAngle = 342f, // 95% of 360
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "95",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonGreen,
                            lineHeight = 56.sp
                        )
                        Text(
                            text = "HEALTH SCORE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberMutedText,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your battery health is excellent.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Capacity is significantly higher than average for this device age.",
                    fontSize = 12.sp,
                    color = CyberMutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    lineHeight = 18.sp
                )
            }
        }

        // Bento Grid Charts Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Temperature Trend
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CyberGlassCard)
                        .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Thermostat,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "TEMP TREND",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberMutedText,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "28°C",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Custom curved line drawing to match HTML SVG path exactly!
                        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                            val path = Path().apply {
                                moveTo(0f, size.height * 0.75f)
                                cubicTo(
                                    size.width * 0.25f, size.height * 0.7f,
                                    size.width * 0.15f, size.height * 0.85f,
                                    size.width * 0.3f, size.height * 0.88f
                                )
                                cubicTo(
                                    size.width * 0.45f, size.height * 0.9f,
                                    size.width * 0.5f, size.height * 0.5f,
                                    size.width * 0.6f, size.height * 0.62f
                                )
                                cubicTo(
                                    size.width * 0.75f, size.height * 0.8f,
                                    size.width * 0.85f, size.height * 0.38f,
                                    size.width * 1.0f, size.height * 0.25f
                                )
                            }

                            // Fill translucent gradient under the curve
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(WarningAmber.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )

                            drawPath(
                                path = path,
                                color = WarningAmber,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                // Cycle Estimate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CyberGlassCard)
                        .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight()) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "CYCLES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberMutedText,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "242",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "/ 500",
                                    fontSize = 11.sp,
                                    color = CyberMutedText,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom progress indicator
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Good", fontSize = 9.sp, color = CyberMutedText)
                                Text("Degraded", fontSize = 9.sp, color = CyberMutedText)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.06f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.484f)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(ElectricCyan)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Healthy Habits Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CyberGlassCard)
                    .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "HEALTHY HABITS DETECTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberMutedText,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Habit 1
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(BorderStroke(1.dp, GlassStroke), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Optimized Charging",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "You consistently unplug around 85%, reducing micro-cycle stress.",
                                fontSize = 11.sp,
                                color = CyberMutedText,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Habit 2
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .border(BorderStroke(1.dp, GlassStroke), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Cool Environment",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Minimal exposure to high temperatures during fast charging sessions.",
                                fontSize = 11.sp,
                                color = CyberMutedText,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. TAB 2: CHARGING HISTORY SCREEN CONTENT
// ==========================================
@Composable
fun HistoryTabContent(
    history: List<ChargingSession>,
    rawHistory: List<ChargingSession>,
    activeFilter: String,
    viewModel: ChargeMateViewModel
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Filters & Actions Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today / Week / Month selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val filterOptions = listOf("Today", "Week", "Month")
                    filterOptions.forEach { filter ->
                        val isSelected = activeFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isSelected) ElectricCyan else Color(0xFF192122))
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) Color.Transparent else GlassStroke
                                    ),
                                    RoundedCornerShape(50.dp)
                                )
                                .clickable { viewModel.setFilter(filter) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.Black else CyberMutedText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Export CSV button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF192122))
                        .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(8.dp))
                        .clickable {
                            Toast.makeText(context, "Exported History to CSV", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = CyberMutedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Export CSV",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Demo data seeder if history is empty
        if (rawHistory.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CyberGlassCard)
                        .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(18.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyberMutedText,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Charging History",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generate premium smart sample history to test filters & insights.",
                            color = CyberMutedText,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.generateSampleHistory()
                                Toast.makeText(context, "Sample data seeded", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            Text("Generate Demo Data", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(history) { session ->
                HistoryCardItem(session = session)
            }
        }
    }
}

// ==========================================
// 8. PAST SESSION CARD ITEM
// ==========================================
@Composable
fun HistoryCardItem(session: ChargingSession) {
    // Determine badge and color based on heat / start state
    val isOptimal = session.peakTemperatureCelsius <= 35.0
    val isModerate = session.peakTemperatureCelsius > 35.0 && session.peakTemperatureCelsius <= 40.0

    val badgeText = when {
        isOptimal -> "Optimal"
        isModerate -> "Moderate"
        else -> "High Heat"
    }

    val badgeColor = when {
        isOptimal -> NeonGreen
        isModerate -> WarningAmber
        else -> CriticalRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberGlassCard)
            .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            // Header: Date & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = session.dateString,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (session.averagePowerWatts > 25) "Public Fast Charger" else "Home Charger",
                        fontSize = 11.sp,
                        color = CyberMutedText
                    )
                }

                // Custom badge matching HTML exactly
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)), RoundedCornerShape(50.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isOptimal -> Icons.Default.CheckCircle
                                isModerate -> Icons.Default.Warning
                                else -> Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Data Density Grid matching HTML exactly
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Range
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "RANGE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberMutedText,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${session.startPercentage}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = CyberMutedText,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${session.endPercentage}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan
                        )
                    }
                }

                // Duration
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                        .border(
                            BorderStroke(0.dp, Color.Transparent)
                        ) // For layout consistency
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                // Draw left separator line to match border-l border-glass-stroke in HTML
                                drawLine(
                                    color = GlassStroke,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                            .padding(start = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = "DURATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberMutedText,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${session.durationMinutes} MIN",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. TAB 3: SETTINGS SCREEN CONTENT (HTML STYLE)
// ==========================================
@Composable
fun SettingsTabContent(viewModel: ChargeMateViewModel) {
    val context = LocalContext.current

    // Alert Volume
    var alarmVolume by remember { mutableFloatStateOf(0.7f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Milestone Alerts Section
        item {
            Column {
                Text(
                    text = "Milestone Alerts",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberGlassCard)
                        .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(20.dp))
                ) {
                    Column {
                        // 20% Alert
                        MilestoneRow(
                            title = "Low Battery",
                            percentage = 20,
                            color = WarningAmber,
                            icon = Icons.Default.BatteryChargingFull,
                            viewModel = viewModel
                        )
                        Divider(color = Color.White.copy(alpha = 0.05f))

                        // 50% Alert
                        MilestoneRow(
                            title = "Half Charge",
                            percentage = 50,
                            color = ElectricCyan,
                            icon = Icons.Default.BatteryChargingFull,
                            viewModel = viewModel
                        )
                        Divider(color = Color.White.copy(alpha = 0.05f))

                        // 80% Alert
                        MilestoneRow(
                            title = "Optimal Limit",
                            percentage = 80,
                            color = ElectricCyan,
                            icon = Icons.Default.BatteryChargingFull,
                            viewModel = viewModel,
                            highlighted = true
                        )
                        Divider(color = Color.White.copy(alpha = 0.05f))

                        // 100% Alert
                        MilestoneRow(
                            title = "Full Charge",
                            percentage = 100,
                            color = NeonGreen,
                            icon = Icons.Default.BatteryChargingFull,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Delivery Method & Preferences Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delivery Method
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberGlassCard)
                        .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "DELIVERY METHOD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberMutedText,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val deliveryModes = listOf(
                            Triple("Voice", "Voice Announcement", Icons.Default.RecordVoiceOver),
                            Triple("Vibe", "Vibration Only", Icons.Default.Vibration),
                            Triple("Silent", "Silent Notification", Icons.Default.NotificationsOff)
                        )

                        deliveryModes.forEachIndexed { idx, pair ->
                            val (key, label, icon) = pair
                            val isSelected = viewModel.soundMode == key

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.05f)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.updateSoundSettings(key, viewModel.voiceType)
                                    }
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) ElectricCyan else CyberMutedText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ElectricCyan else Color.White
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.updateSoundSettings(key, viewModel.voiceType) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = ElectricCyan,
                                            unselectedColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Voice Accent Card (As requested: Assistant, Female, Male)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberGlassCard)
                        .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "VOICE PROFILE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberMutedText,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val voiceOptions = listOf("Assistant", "Female", "Male")
                        voiceOptions.forEach { voice ->
                            val isSelected = viewModel.voiceType == voice
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.1f) else Color.Transparent)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.05f)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.updateSoundSettings(viewModel.soundMode, voice)
                                    }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = voice,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Alert Sound & Volume slider
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CyberGlassCard)
                    .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .border(BorderStroke(1.dp, GlassStroke), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Digital Pulse",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Default Alert Sound",
                                    fontSize = 10.sp,
                                    color = CyberMutedText
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = CyberMutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Volume", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${(alarmVolume * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeDown,
                            contentDescription = null,
                            tint = CyberMutedText,
                            modifier = Modifier.size(14.dp)
                        )
                        Slider(
                            value = alarmVolume,
                            onValueChange = { alarmVolume = it },
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Temperature Protection Section
        item {
            Column {
                Text(
                    text = "Temperature Protection",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberGlassCard)
                        .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(WarningAmber.copy(alpha = 0.12f))
                                        .border(BorderStroke(1.dp, WarningAmber.copy(alpha = 0.2f)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Thermostat,
                                        contentDescription = null,
                                        tint = WarningAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Overheat Alert",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Notify and pause charging if exceeded",
                                        fontSize = 10.sp,
                                        color = CyberMutedText
                                    )
                                }
                            }

                            Switch(
                                checked = viewModel.isTempProtectEnabled,
                                onCheckedChange = {
                                    viewModel.updateTempProtection(it, viewModel.tempThreshold, viewModel.tempAlertType)
                                    Toast.makeText(context, "Overheat protection ${if (it) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = WarningAmber,
                                    checkedTrackColor = WarningAmber.copy(alpha = 0.4f),
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }

                        if (viewModel.isTempProtectEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Limit Threshold", fontSize = 12.sp, color = Color.White)
                                Text(
                                    text = "${viewModel.tempThreshold}°C",
                                    fontSize = 14.sp,
                                    color = if (viewModel.tempThreshold >= 45) CriticalRed else WarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Slider(
                                value = viewModel.tempThreshold.toFloat(),
                                onValueChange = {
                                    viewModel.updateTempProtection(true, it.toInt(), viewModel.tempAlertType)
                                },
                                valueRange = 40f..45f,
                                steps = 4,
                                colors = SliderDefaults.colors(
                                    thumbColor = WarningAmber,
                                    activeTrackColor = WarningAmber,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("40°C", fontSize = 10.sp, color = CyberMutedText)
                                Text("45°C", fontSize = 10.sp, color = CyberMutedText)
                            }
                        }
                    }
                }
            }
        }

        // Demo & Testing Suite Section
        item {
            Column {
                Text(
                    text = "Demo & Testing Suite",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp, top = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CyberGlassCard)
                        .border(BorderStroke(1.dp, GlassStroke), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan.copy(alpha = 0.12f))
                                        .border(BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.2f)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Simulation Mode",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Test app features without physical charging",
                                        fontSize = 10.sp,
                                        color = CyberMutedText
                                    )
                                }
                            }

                            Switch(
                                checked = com.example.receiver.BatteryStateManager.isSimulationActive,
                                onCheckedChange = { active ->
                                    viewModel.toggleSimulation(active)
                                    Toast.makeText(
                                        context,
                                        if (active) "Simulation Mode active" else "Simulation Mode inactive",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ElectricCyan,
                                    checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }

                        if (com.example.receiver.BatteryStateManager.isSimulationActive) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Simulated Battery Level
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Simulated Battery Level", fontSize = 12.sp, color = Color.White)
                                Text(
                                    text = "${viewModel.simPercentage}%",
                                    fontSize = 14.sp,
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Slider(
                                value = viewModel.simPercentage.toFloat(),
                                onValueChange = {
                                    viewModel.updateSimulationParams(percent = it.toInt())
                                },
                                valueRange = 0f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Simulated Charging State
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Simulated Power Connection", fontSize = 12.sp, color = Color.White)
                                    Text("Simulate charger plugged in", fontSize = 10.sp, color = CyberMutedText)
                                }
                                Switch(
                                    checked = viewModel.simIsCharging,
                                    onCheckedChange = { isCharging ->
                                        viewModel.updateSimulationParams(charging = isCharging)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElectricCyan,
                                        checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Simulated Temperature
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Simulated Temperature", fontSize = 12.sp, color = Color.White)
                                Text(
                                    text = "${viewModel.simTemperature.toInt()}°C",
                                    fontSize = 14.sp,
                                    color = if (viewModel.simTemperature >= 40) CriticalRed else WarningAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Slider(
                                value = viewModel.simTemperature.toFloat(),
                                onValueChange = {
                                    viewModel.updateSimulationParams(temp = it.toDouble())
                                },
                                valueRange = 30f..50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = WarningAmber,
                                    activeTrackColor = WarningAmber,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Diagnostic actions
                            Text("Direct Alert Testing", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberMutedText)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Testing Voice Alert...", Toast.LENGTH_SHORT).show()
                                        viewModel.testVoiceAlert(context)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Speech", color = ElectricCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Testing Alarm Chime...", Toast.LENGTH_SHORT).show()
                                        viewModel.testSoundAlert(context)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Chime", color = WarningAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. MILESTONE ALERTS ROW COMPONENT
// ==========================================
@Composable
fun MilestoneRow(
    title: String,
    percentage: Int,
    color: Color,
    icon: ImageVector,
    viewModel: ChargeMateViewModel,
    highlighted: Boolean = false
) {
    val isEnabled = viewModel.isMilestoneEnabled(percentage)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlighted) ElectricCyan.copy(alpha = 0.03f) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (highlighted) ElectricCyan.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.04f))
                    .border(
                        BorderStroke(
                            1.dp,
                            if (highlighted) ElectricCyan.copy(alpha = 0.2f) else GlassStroke
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$percentage%",
                    fontSize = 11.sp,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = { checked ->
                if (checked) {
                    viewModel.addCustomMilestone(percentage)
                    viewModel.setMilestoneEnabled(percentage, true)
                } else {
                    viewModel.setMilestoneEnabled(percentage, false)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = color,
                checkedTrackColor = color.copy(alpha = 0.4f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}
