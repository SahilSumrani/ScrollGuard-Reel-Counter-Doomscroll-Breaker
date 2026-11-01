package com.example.braindrain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay

class BlockOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var lockScreenView: ComposeView? = null
    private lateinit var lifecycleOwner: ComposeOverlayLifecycleOwner

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner = ComposeOverlayLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()
        startForegroundService()
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "SHOW_BLOCK" -> {
                showLockScreen()
            }
            "HIDE_BLOCK" -> {
                removeOverlay()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val channelId = "brain_block_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Focus Enforcer", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Focus Limit Reached")
            .setContentText("Enforcing screen block.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()

        startForeground(2, notification)
    }

    @Suppress("DEPRECATION")
    private fun showLockScreen() {
        if (lockScreenView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        val prefs = getSharedPreferences("BrainPalPrefs", MODE_PRIVATE)
        val dailyLimit = prefs.getInt("daily_limit", 50)

        val focusMessages = listOf(
            "Daily Focus Boundary Reached",
            "Give Your Dopamine System a Rest",
            "Step Away. Protect Your Energy",
            "Mind Shield Activated"
        )
        val selectedMessage = focusMessages.random()

        lockScreenView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            
            setContent {
                val infiniteTransition = rememberInfiniteTransition(label = "shieldPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.92f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                var progress by remember { mutableStateOf(0f) }
                
                LaunchedEffect(Unit) {
                    val totalDuration = 5000L
                    val interval = 50L
                    var elapsed = 0L
                    while (elapsed < totalDuration) {
                        delay(interval)
                        elapsed += interval
                        progress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                    }
                    // Time's up! Automatically close the app and go to home screen
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    this@BlockOverlayService.startActivity(homeIntent)
                    
                    removeOverlay()
                    stopSelf()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF1E1128), Color(0xFF0D0A14), Color(0xFF050408)),
                                radius = 1200f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth()
                    ) {
                        // Shield Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x33F43F5E))
                                .border(1.dp, Color(0x66FB7185), RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FOCUS ENFORCER",
                                color = Color(0xFFFECDD3),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        // Glowing Shield Emblem Visualizer (NO raw countdown numbers!)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(200.dp)
                        ) {
                            // Ambient glow aura
                            Box(
                                modifier = Modifier
                                    .size((180f * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E).copy(alpha = 0.2f))
                            )

                            // Glowing Core Shield Circle
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFE11D48), Color(0xFF881337))
                                        )
                                    )
                                    .border(2.dp, Color(0x66FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🛡️",
                                    fontSize = 48.sp
                                )
                            }

                            // Smooth circular progress ring (fills gently over 5s without ticking digits)
                            Canvas(modifier = Modifier.size(190.dp)) {
                                drawArc(
                                    color = Color(0x22FFFFFF),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(Color(0xFFF43F5E), Color(0xFFFB7185), Color(0xFFF43F5E))
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    style = Stroke(
                                        width = 4.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                            text = selectedMessage,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "You've scrolled past your planned limit. App will close shortly to protect your focus.",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        // Professional Exit Button: takes user safely to Home Launcher immediately
                        Button(
                            onClick = {
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                this@BlockOverlayService.startActivity(homeIntent)
                                removeOverlay()
                                stopSelf()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF43F5E)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Exit App & Return Home",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        windowManager.addView(lockScreenView, params)
    }

    private fun removeOverlay() {
        lockScreenView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
            lockScreenView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        lifecycleOwner.onDestroy()
    }
}
