package com.example.braindrain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.braindrain.data.AppDatabase
import com.example.braindrain.data.DailyStats
import com.example.braindrain.data.HourlyBucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindControlTheme {
                RootAppNavigation()
            }
        }
    }
}

@Composable
private fun OnResumeEffect(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

// ----------------------------------------------------
// Dribbble / Figma Dark Luxury Palette
// ----------------------------------------------------
val DarkBackground = Color(0xFF090A10)
val DarkSurfaceCard = Color(0xFF141724)
val DarkSurfaceCardElevated = Color(0xFF1B2032)
val DarkBorderSubtle = Color(0x1AFFFFFF)
val DarkBorderGlow = Color(0x338B5CF6)

val NeonViolet = Color(0xFF8B5CF6)
val ElectricIndigo = Color(0xFF6366F1)
val HyperCyan = Color(0xFF06B6D4)
val ZenEmerald = Color(0xFF10B981)
val WarmAmber = Color(0xFFF59E0B)
val AlertRose = Color(0xFFF43F5E)

val TextPrimaryDark = Color(0xFFF9FAFB)
val TextSecondaryDark = Color(0xFF9CA3AF)
val TextMutedDark = Color(0xFF6B7280)

@Composable
fun MindControlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            surface = DarkSurfaceCard,
            primary = NeonViolet,
            secondary = HyperCyan,
            tertiary = WarmAmber
        ),
        content = content
    )
}

// ----------------------------------------------------
// Navigation Root
// ----------------------------------------------------
@Composable
fun RootAppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BrainPalPrefs", Context.MODE_PRIVATE) }
    val hasCompletedOnboarding = remember { prefs.getBoolean("has_completed_onboarding", false) }

    NavHost(
        navController = navController,
        startDestination = if (hasCompletedOnboarding) "main" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(onComplete = {
                prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                navController.navigate("main") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("main") {
            MainAppScreen()
        }
    }
}

// ----------------------------------------------------
// Modern Figma Onboarding Screen
// ----------------------------------------------------
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val isAccessibilityGranted = remember { mutableStateOf(false) }
    val isOverlayGranted = remember { mutableStateOf(false) }

    OnResumeEffect {
        isOverlayGranted.value = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        isAccessibilityGranted.value = enabledServices.contains(BrainDrainAccessibilityService::class.java.name)
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 28.dp, vertical = 20.dp)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(800)) + scaleIn(initialScale = 0.8f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        ) {
            // Glowing Hero Icon
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(NeonViolet.copy(alpha = 0.2f), Color.Transparent)))
                    .border(2.dp, DarkBorderGlow, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brain_control),
                    contentDescription = "Mind Control Logo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(800, delayMillis = 300)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(800, delayMillis = 300))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MIND CONTROL",
                    color = NeonViolet,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Reclaim Your Dopamine",
                    color = TextPrimaryDark,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Stop mindless scrolling with gentle mindful resets. Zero personal data tracked. Pure focus.",
                    color = TextSecondaryDark,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(800, delayMillis = 500)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(800, delayMillis = 500))
        ) {
            // Feature Highlight Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureChip(icon = "🛡️", title = "100% Private")
                FeatureChip(icon = "⚡", title = "Reel Counter")
                FeatureChip(icon = "🔒", title = "100% Safe")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(800, delayMillis = 700)) + slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(800, delayMillis = 700))
        ) {
            Column {
                // Permission Card 1: Accessibility
                PermissionCard(
                    title = "Reel Detection Service",
                    description = "Detects short-form scrolling in Instagram & YouTube. Never reads private chats.",
                    isGranted = isAccessibilityGranted.value,
                    onActionClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Permission Card 2: Overlay
                PermissionCard(
                    title = "Mindful Shield Overlay",
                    description = "Displays the subtle Dynamic Island pill and calming breathing reset screens.",
                    isGranted = isOverlayGranted.value,
                    onActionClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(800, delayMillis = 900)) + scaleIn(initialScale = 0.9f, animationSpec = tween(800, delayMillis = 900))
        ) {
            // Primary Continue Button
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(29.dp)
            ) {
                Text(
                    text = "Enter Mind Control",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FeatureChip(icon: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, DarkBorderSubtle, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(icon, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title, 
            color = TextPrimaryDark, 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onActionClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isGranted) ZenEmerald.copy(alpha = 0.5f) else DarkBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimaryDark, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = TextSecondaryDark, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) ZenEmerald.copy(alpha = 0.2f) else DarkSurfaceCardElevated
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isGranted) ZenEmerald else DarkBorderGlow),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isGranted) "Active" else "Enable",
                    color = if (isGranted) ZenEmerald else NeonViolet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ----------------------------------------------------
// Main App Screen with Dribbble-Grade Bottom Bar
// ----------------------------------------------------
@Composable
fun MainAppScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = DarkBackground,
                contentColor = TextPrimaryDark,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(BorderStroke(1.dp, DarkBorderSubtle))
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Focus Insights") },
                    label = { Text("Insights", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonViolet,
                        selectedTextColor = NeonViolet,
                        indicatorColor = DarkSurfaceCardElevated,
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Social Arena") },
                    label = { Text("Arena", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HyperCyan,
                        selectedTextColor = HyperCyan,
                        indicatorColor = DarkSurfaceCardElevated,
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.List, contentDescription = "Shield Settings") },
                    label = { Text("Shield", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = WarmAmber,
                        selectedTextColor = WarmAmber,
                        indicatorColor = DarkSurfaceCardElevated,
                        unselectedIconColor = TextMutedDark,
                        unselectedTextColor = TextMutedDark
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(DarkBackground)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            when (selectedTab) {
                0 -> InsightsDashboardTab()
                1 -> SocialArenaTab()
                2 -> ShieldSettingsTab()
            }
        }
    }
}

// ----------------------------------------------------
// TAB 1: Insights Dashboard (Dribbble Hero Experience)
// ----------------------------------------------------
@Composable
fun InsightsDashboardTab() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BrainPalPrefs", Context.MODE_PRIVATE) }
    var dailyLimit by remember { mutableStateOf(prefs.getInt("daily_limit", 100)) }
    
    var totalReels by remember { mutableStateOf(0) }
    var instaReels by remember { mutableStateOf(0) }
    var ytReels by remember { mutableStateOf(0) }
    var fbReels by remember { mutableStateOf(0) }
    var snapReels by remember { mutableStateOf(0) }
    var tiktokReels by remember { mutableStateOf(0) }
    var hourlyBuckets by remember { mutableStateOf<List<HourlyBucket>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = AppDatabase.getDatabase(context)
        withContext(Dispatchers.IO) {
            val stats = db.statsDao().getDailyStats(dateStr)
            if (stats != null) {
                totalReels = stats.totalReels
                instaReels = stats.instagramReels
                ytReels = stats.youtubeReels
                fbReels = stats.facebookReels
                snapReels = stats.snapchatReels
                tiktokReels = stats.tiktokReels
            }
            hourlyBuckets = db.statsDao().getHourlyBuckets(dateStr)
        }
            kotlinx.coroutines.delay(1500)
        }
    }

    val progressRatio = if (dailyLimit > 0) (totalReels.toFloat() / dailyLimit).coerceIn(0f, 1.2f) else 0f
    val focusReserve = (100 - (progressRatio * 100).toInt()).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Top Brand & Live Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MIND CONTROL",
                    color = NeonViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Daily Focus",
                    color = TextPrimaryDark,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Neural State Status Chip (live brain state)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, BrainState.fromScrollCount(totalReels).primaryColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                MiniNeuralIndicator(scrollCount = totalReels, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    BrainState.fromScrollCount(totalReels).pillEmoji + " " + BrainState.fromScrollCount(totalReels).label,
                    color = TextPrimaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Hero Component: Neural Consciousness Visualizer (6-stage brain degradation)
        NeuralConsciousnessVisualizer(
            scrollCount = totalReels,
            dailyLimit = dailyLimit
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Metrics Row (3 Glass Cards)
        val currentBrainState = BrainState.fromScrollCount(totalReels)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickMetricCard(
                title = "Neural",
                value = "${currentBrainState.emoji}",
                subtitle = currentBrainState.label,
                accentColor = currentBrainState.primaryColor,
                modifier = Modifier.weight(1f)
            )
            QuickMetricCard(
                title = "Time Lost",
                value = "~${(totalReels * 0.5).toInt()}m",
                subtitle = "In reels today",
                accentColor = HyperCyan,
                modifier = Modifier.weight(1f)
            )
            QuickMetricCard(
                title = "Budget",
                value = "$focusReserve%",
                subtitle = "$totalReels / $dailyLimit",
                accentColor = if (focusReserve > 50) ZenEmerald else if (focusReserve > 20) WarmAmber else AlertRose,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Platform Distribution Section
        Text(
            text = "PLATFORM BREAKDOWN",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Instagram Card
        PlatformStatCard(
            platformName = "Instagram Reels",
            iconEmoji = "📸",
            count = instaReels,
            total = totalReels,
            gradient = Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))),
            badgeColor = Color(0xFFE1306C)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // YouTube Shorts Card
        PlatformStatCard(
            platformName = "YouTube Shorts",
            iconEmoji = "▶️",
            count = ytReels,
            total = totalReels,
            gradient = Brush.linearGradient(listOf(Color(0xFFFF0033), Color(0xFFCC0000))),
            badgeColor = Color(0xFFFF0033)
        )

        // Facebook Reels (only show if user has activity)
        if (fbReels > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            PlatformStatCard(
                platformName = "Facebook Reels",
                iconEmoji = "📘",
                count = fbReels,
                total = totalReels,
                gradient = Brush.linearGradient(listOf(Color(0xFF1877F2), Color(0xFF42A5F5))),
                badgeColor = Color(0xFF1877F2)
            )
        }

        // Snapchat Spotlight
        if (snapReels > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            PlatformStatCard(
                platformName = "Snapchat Spotlight",
                iconEmoji = "👻",
                count = snapReels,
                total = totalReels,
                gradient = Brush.linearGradient(listOf(Color(0xFFFFFC00), Color(0xFFFFEB3B))),
                badgeColor = Color(0xFFFFFC00)
            )
        }

        // TikTok
        if (tiktokReels > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            PlatformStatCard(
                platformName = "TikTok",
                iconEmoji = "🎵",
                count = tiktokReels,
                total = totalReels,
                gradient = Brush.linearGradient(listOf(Color(0xFF00F2EA), Color(0xFFFF0050))),
                badgeColor = Color(0xFF00F2EA)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Hourly Activity Visualizer (Figma Analytics Chart)
        HourlyActivityHeatmap(hourlyBuckets)

        Spacer(modifier = Modifier.height(26.dp))

        // Mindful Reset Quick Action
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, DarkBorderGlow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mindful Pause", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Test the gentle breathing reset screen right now.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        val breakIntent = Intent(context, BrainOverlayService::class.java).apply {
                            action = "SHOW_LOCK"
                        }
                        context.startService(breakIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Breathe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ----------------------------------------------------
// Hero Component: Radial Gauge with Gradient Arc
// ----------------------------------------------------
@Composable
fun HeroRadialFocusGauge(
    scrollCount: Int,
    dailyLimit: Int,
    progressRatio: Float,
    focusReserve: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "radialProgress"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, DarkBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Background Track & Gradient Arc
                Canvas(modifier = Modifier.size(190.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    // Track
                    drawArc(
                        color = Color(0x1AFFFFFF),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // Active Gradient Arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(HyperCyan, NeonViolet, WarmAmber, AlertRose)
                        ),
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Inner Content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$scrollCount",
                        color = TextPrimaryDark,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "reels today",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Status Pill
                    val statusText = if (scrollCount < dailyLimit * 0.6f) "Optimal Focus"
                                     else if (scrollCount <= dailyLimit) "Budget Normal"
                                     else "Limit Exceeded"
                    val statusColor = if (scrollCount < dailyLimit * 0.6f) ZenEmerald
                                      else if (scrollCount <= dailyLimit) WarmAmber
                                      else AlertRose

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-bar showing daily limit progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Goal: $dailyLimit reels daily",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}% used",
                    color = if (animatedProgress > 0.9f) AlertRose else NeonViolet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ----------------------------------------------------
// Quick Metric Glass Card
// ----------------------------------------------------
@Composable
fun QuickMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, DarkBorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title, color = TextMutedDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = TextSecondaryDark, fontSize = 10.sp)
        }
    }
}

// ----------------------------------------------------
// Platform Stat Card
// ----------------------------------------------------
@Composable
fun PlatformStatCard(
    platformName: String,
    iconEmoji: String,
    count: Int,
    total: Int,
    gradient: Brush,
    badgeColor: Color
) {
    val share = if (total > 0) (count.toFloat() / total) else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, DarkBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(iconEmoji, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(platformName, color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${(share * 100).toInt()}% of scroll time", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$count", color = badgeColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("reels", color = TextMutedDark, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Smooth progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(DarkSurfaceCardElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(share.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(gradient)
                )
            }
        }
    }
}

// ----------------------------------------------------
// Hourly Activity Heatmap Visualizer (Figma Widget)
// ----------------------------------------------------
@Composable
fun HourlyActivityHeatmap(hourlyBuckets: List<HourlyBucket>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, DarkBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOURLY SCROLL RHYTHM",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Today",
                    color = NeonViolet,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6 Time Slots (4 AM to 12 AM)
            val slots = listOf("6a", "9a", "12p", "3p", "6p", "9p")
            val maxCount = (hourlyBuckets.maxOfOrNull { it.count } ?: 10).coerceAtLeast(5)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                slots.forEachIndexed { index, label ->
                    val hourTarget = 6 + (index * 3)
                    val bucket = hourlyBuckets.find { it.hour == hourTarget || it.hour == hourTarget - 1 }
                    val count = bucket?.count ?: 0
                    val heightRatio = if (maxCount > 0) (count.toFloat() / maxCount).coerceIn(0.1f, 1f) else 0.1f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((65 * heightRatio).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (heightRatio > 0.6f) Brush.verticalGradient(listOf(AlertRose, NeonViolet))
                                    else Brush.verticalGradient(listOf(NeonViolet, HyperCyan))
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(label, color = TextMutedDark, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 2: Social Arena (Dribbble Podium Leaderboard)
// ----------------------------------------------------
@Composable
fun SocialArenaTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "MINDFUL LEAGUE",
            color = HyperCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = "Social Arena",
            color = TextPrimaryDark,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Dribbble Podium Display for Top 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // #2 Silver (Left)
            PodiumColumn(
                rank = 2,
                name = "You",
                reels = 18,
                badgeColor = Color(0xFFC0C0C0),
                crown = "🥈",
                isMe = true,
                podiumHeight = 90.dp
            )

            // #1 Gold (Center, Elevated)
            PodiumColumn(
                rank = 1,
                name = "Alex M.",
                reels = 8,
                badgeColor = Color(0xFFFFD700),
                crown = "👑",
                isMe = false,
                podiumHeight = 120.dp
            )

            // #3 Bronze (Right)
            PodiumColumn(
                rank = 3,
                name = "Sarah K.",
                reels = 32,
                badgeColor = Color(0xFFCD7F32),
                crown = "🥉",
                isMe = false,
                podiumHeight = 70.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ranked List
        Text(
            text = "ALL PARTICIPANTS",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, DarkBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                LeaderboardItem(rank = 1, name = "Alex M. (Pro)", count = 8, badge = "Zen Master", isMe = false)
                Divider(color = DarkBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))
                LeaderboardItem(rank = 2, name = "You", count = 18, badge = "Mind Guard", isMe = true)
                Divider(color = DarkBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))
                LeaderboardItem(rank = 3, name = "Sarah K.", count = 32, badge = "Balanced", isMe = false)
                Divider(color = DarkBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))
                LeaderboardItem(rank = 4, name = "David_99", count = 74, badge = "Scroller", isMe = false)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Share Card Button
        Button(
            onClick = { /* Share score intent */ },
            colors = ButtonDefaults.buttonColors(containerColor = HyperCyan),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text("Invite Friends to Arena", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PodiumColumn(
    rank: Int,
    name: String,
    reels: Int,
    badgeColor: Color,
    crown: String,
    isMe: Boolean,
    podiumHeight: androidx.compose.ui.unit.Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        Text(crown, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (isMe) NeonViolet else DarkSurfaceCardElevated)
                .border(2.dp, badgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            color = TextPrimaryDark,
            fontSize = 12.sp,
            fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            text = "$reels reels",
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Podium Block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(DarkSurfaceCard)
                .border(1.dp, DarkBorderSubtle, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("#$rank", color = badgeColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun LeaderboardItem(rank: Int, name: String, count: Int, badge: String, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#$rank",
                color = if (isMe) NeonViolet else TextMutedDark,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.width(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = name,
                    color = if (isMe) NeonViolet else TextPrimaryDark,
                    fontWeight = if (isMe) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(badge, color = TextSecondaryDark, fontSize = 11.sp)
            }
        }
        Text(
            text = "$count reels",
            color = if (isMe) NeonViolet else TextPrimaryDark,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

// ----------------------------------------------------
// TAB 3: Shield & Settings
// ----------------------------------------------------
@Composable
fun ShieldSettingsTab() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("BrainPalPrefs", Context.MODE_PRIVATE) }
    var dailyLimit by remember { mutableStateOf(prefs.getInt("daily_limit", 100)) }
    var shieldActive by remember { mutableStateOf(prefs.getBoolean("shield_active", true)) }
    var customLimitText by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    // Per-app limits
    var instaLimit by remember { mutableStateOf(prefs.getInt("insta_limit", 0)) }
    var ytLimit by remember { mutableStateOf(prefs.getInt("yt_limit", 0)) }
    var fbLimit by remember { mutableStateOf(prefs.getInt("fb_limit", 0)) }
    var snapLimit by remember { mutableStateOf(prefs.getInt("snap_limit", 0)) }
    var tiktokLimit by remember { mutableStateOf(prefs.getInt("tiktok_limit", 0)) }

    // Current counts from DB
    var totalReels by remember { mutableStateOf(0) }
    var instaReels by remember { mutableStateOf(0) }
    var ytReels by remember { mutableStateOf(0) }
    var fbReels by remember { mutableStateOf(0) }
    var snapReels by remember { mutableStateOf(0) }
    var tiktokReels by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = AppDatabase.getDatabase(context)
        withContext(Dispatchers.IO) {
            val stats = db.statsDao().getDailyStats(dateStr)
            if (stats != null) {
                totalReels = stats.totalReels
                instaReels = stats.instagramReels
                ytReels = stats.youtubeReels
                fbReels = stats.facebookReels
                snapReels = stats.snapchatReels
                tiktokReels = stats.tiktokReels
            }
        }
    }

    val totalPercentage = if (dailyLimit > 0) ((totalReels.toFloat() / dailyLimit) * 100).toInt().coerceAtMost(999) else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "CONTROL CENTER",
            color = WarmAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Text(
            text = "Shield & Goals",
            color = TextPrimaryDark,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Live Status Card — Total % Used ──
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp,
                when {
                    totalPercentage >= 100 -> AlertRose.copy(alpha = 0.6f)
                    totalPercentage >= 80 -> WarmAmber.copy(alpha = 0.5f)
                    else -> ZenEmerald.copy(alpha = 0.4f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today's Usage", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$totalReels / $dailyLimit reels",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    }
                    // Big percentage number
                    Text(
                        text = "$totalPercentage%",
                        color = when {
                            totalPercentage >= 100 -> AlertRose
                            totalPercentage >= 80 -> WarmAmber
                            totalPercentage >= 50 -> NeonViolet
                            else -> ZenEmerald
                        },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurfaceCardElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((totalReels.toFloat() / dailyLimit.coerceAtLeast(1)).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        if (totalPercentage >= 80) WarmAmber else ZenEmerald,
                                        if (totalPercentage >= 100) AlertRose else NeonViolet
                                    )
                                )
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Shield Master Switch ──
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (shieldActive) ZenEmerald.copy(alpha = 0.5f) else DarkBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Focus Shield", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (shieldActive) "Protection active. Auto-blocks when limit reached."
                        else "Shield paused. Scroll alerts muted.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = shieldActive,
                    onCheckedChange = {
                        shieldActive = it
                        prefs.edit().putBoolean("shield_active", it).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ZenEmerald
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Daily Total Limit Selector ──
        Text(
            text = "DAILY SCROLL BUDGET",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preset chips row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("50", "Strict", 50),
                Triple("100", "Normal", 100),
                Triple("200", "Relaxed", 200),
                Triple("300", "Chill", 300)
            ).forEach { (label, caption, value) ->
                LimitOptionChip(
                    label = label,
                    caption = caption,
                    isSelected = dailyLimit == value && !showCustomInput,
                    onClick = {
                        dailyLimit = value
                        showCustomInput = false
                        prefs.edit().putInt("daily_limit", value).apply()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom input toggle + field
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, if (showCustomInput) NeonViolet.copy(alpha = 0.5f) else DarkBorderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCustomInput = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Custom Limit",
                        color = if (showCustomInput) NeonViolet else TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Set your own daily scroll budget",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
                if (showCustomInput) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.OutlinedTextField(
                            value = customLimitText,
                            onValueChange = { newValue ->
                                // Only allow digits
                                val filtered = newValue.filter { it.isDigit() }
                                if (filtered.length <= 4) {
                                    customLimitText = filtered
                                    val num = filtered.toIntOrNull()
                                    if (num != null && num > 0) {
                                        dailyLimit = num
                                        prefs.edit().putInt("daily_limit", num).apply()
                                    }
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = NeonViolet,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            placeholder = {
                                Text(
                                    "$dailyLimit",
                                    color = TextMutedDark,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonViolet,
                                unfocusedBorderColor = DarkBorderSubtle,
                                cursorColor = NeonViolet
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                } else {
                    Text(
                        text = "✏️",
                        fontSize = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Per-App Limits Section ──
        Text(
            text = "PER-APP SCROLL LIMITS",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Set individual limits per app. 0 = no limit (uses total budget only).",
            color = TextMutedDark,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Instagram
        PerAppLimitCard(
            appName = "Instagram",
            emoji = "📸",
            currentCount = instaReels,
            limit = instaLimit,
            accentColor = Color(0xFFE1306C),
            onLimitChange = { newLimit ->
                instaLimit = newLimit
                prefs.edit().putInt("insta_limit", newLimit).apply()
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // YouTube
        PerAppLimitCard(
            appName = "YouTube",
            emoji = "▶️",
            currentCount = ytReels,
            limit = ytLimit,
            accentColor = Color(0xFFFF0033),
            onLimitChange = { newLimit ->
                ytLimit = newLimit
                prefs.edit().putInt("yt_limit", newLimit).apply()
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Facebook
        PerAppLimitCard(
            appName = "Facebook",
            emoji = "📘",
            currentCount = fbReels,
            limit = fbLimit,
            accentColor = Color(0xFF1877F2),
            onLimitChange = { newLimit ->
                fbLimit = newLimit
                prefs.edit().putInt("fb_limit", newLimit).apply()
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Snapchat
        PerAppLimitCard(
            appName = "Snapchat",
            emoji = "👻",
            currentCount = snapReels,
            limit = snapLimit,
            accentColor = Color(0xFFFFFC00),
            onLimitChange = { newLimit ->
                snapLimit = newLimit
                prefs.edit().putInt("snap_limit", newLimit).apply()
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // TikTok
        PerAppLimitCard(
            appName = "TikTok",
            emoji = "🎵",
            currentCount = tiktokReels,
            limit = tiktokLimit,
            accentColor = Color(0xFF00F2EA),
            onLimitChange = { newLimit ->
                tiktokLimit = newLimit
                prefs.edit().putInt("tiktok_limit", newLimit).apply()
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Permissions Status Overview
        Text(
            text = "SYSTEM PERMISSIONS",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, DarkBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SystemPermissionRow(
                    name = "Accessibility Service",
                    status = "Configured",
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                )
                Divider(color = DarkBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))
                SystemPermissionRow(
                    name = "Display Over Other Apps",
                    status = "Granted",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Emergency Break Test
        Button(
            onClick = {
                val blockIntent = Intent(context, BlockOverlayService::class.java).apply {
                    action = "SHOW_BLOCK"
                }
                context.startService(blockIntent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCardElevated),
            border = BorderStroke(1.dp, AlertRose.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(27.dp)
        ) {
            Text("Test Limit Boundary Screen", color = AlertRose, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Per-App Limit Card Component ──
@Composable
fun PerAppLimitCard(
    appName: String,
    emoji: String,
    currentCount: Int,
    limit: Int,
    accentColor: Color,
    onLimitChange: (Int) -> Unit
) {
    var limitText by remember(limit) { mutableStateOf(if (limit > 0) "$limit" else "") }
    val percentage = if (limit > 0) ((currentCount.toFloat() / limit) * 100).toInt().coerceAtMost(999) else 0

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (limit > 0 && currentCount >= limit) AlertRose.copy(alpha = 0.5f) else DarkBorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(appName, color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "$currentCount reels today" + if (limit > 0) " · $percentage%" else "",
                            color = if (limit > 0 && currentCount >= limit) AlertRose else TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }

                // Limit input
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.OutlinedTextField(
                        value = limitText,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() }
                            if (filtered.length <= 4) {
                                limitText = filtered
                                val num = filtered.toIntOrNull() ?: 0
                                onLimitChange(num)
                            }
                        },
                        modifier = Modifier.width(72.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = accentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "∞",
                                color = TextMutedDark,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = DarkBorderSubtle,
                            cursorColor = accentColor
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Per-app progress bar (only shown when limit is set)
            if (limit > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DarkSurfaceCardElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((currentCount.toFloat() / limit).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (currentCount >= limit) AlertRose else accentColor
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun LimitOptionChip(
    label: String,
    caption: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NeonViolet.copy(alpha = 0.2f) else DarkSurfaceCard
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isSelected) NeonViolet else DarkBorderSubtle),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = if (isSelected) NeonViolet else TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = caption,
                color = if (isSelected) NeonViolet else TextSecondaryDark,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SystemPermissionRow(name: String, status: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(status, color = ZenEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("›", color = TextSecondaryDark, fontSize = 16.sp)
        }
    }
}
