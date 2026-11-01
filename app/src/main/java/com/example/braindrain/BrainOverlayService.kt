package com.example.braindrain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class ComposeOverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

class BrainOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var pillView: ComposeView? = null
    private var lockScreenView: ComposeView? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private var currentCountState = mutableStateOf(0)
    private var shouldShowPill = false

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
            "SHOW_PILL" -> {
                shouldShowPill = true
                if (pillView == null && lockScreenView == null) {
                    showPillView()
                }
            }
            "UPDATE_COUNT" -> {
                val count = intent.getIntExtra("count", 0)
                currentCountState.value = count
                if (shouldShowPill && pillView == null && lockScreenView == null) {
                    showPillView()
                }
            }
            "SHOW_LOCK" -> {
                shouldShowPill = false
                showLockScreen()
            }
            "HIDE_PILL" -> {
                shouldShowPill = false
                removePillOnly()
            }
            "HIDE_OVERLAY" -> {
                shouldShowPill = false
                removeOverlays()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "brain_drain_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Focus Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("BrainPal Monitor Active")
            .setContentText("Watching your reels")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
    }

    @Suppress("DEPRECATION")
    private fun showPillView() {
        if (pillView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 50

        pillView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            
            setContent {
                val count = currentCountState.value
                val brainState = BrainState.fromScrollCount(count)

                // Simple Pill View matching BrainPal style
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(2.dp, brainState.primaryColor, RoundedCornerShape(32.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .animateContentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = brainState.pillEmoji,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "$count",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                    Text(
                        text = " reels",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
        }
        windowManager.addView(pillView, params)
    }

    @Suppress("DEPRECATION")
    private fun showLockScreen() {
        if (lockScreenView != null) return
        removeOverlays() // Hide pill

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        lockScreenView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            
            setContent {
                val count = currentCountState.value
                val brainState = BrainState.fromScrollCount(count)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "STOP SCROLLING",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Simple Brain Display
                        Text(
                            text = brainState.emoji,
                            fontSize = 120.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "You've watched $count reels today.",
                            color = brainState.primaryColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Your brain is ${brainState.label.lowercase()}.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        TextButton(
                            onClick = { removeOverlays() }
                        ) {
                            Text(
                                text = "Close",
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

    private fun removeOverlays() {
        pillView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
            pillView = null
        }
        lockScreenView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
            lockScreenView = null
        }
    }

    private fun removePillOnly() {
        pillView?.let {
            if (it.isAttachedToWindow) {
                windowManager.removeView(it)
            }
            pillView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlays()
        lifecycleOwner.onDestroy()
    }
}
