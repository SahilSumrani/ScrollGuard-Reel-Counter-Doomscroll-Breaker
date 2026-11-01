package com.example.braindrain

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class BrainDrainAccessibilityService : AccessibilityService() {

    private var currentForegroundApp: String? = null
    private var isInTrackedApp = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        try {
            handleAccessibilityEvent(event)
        } catch (exception: Exception) {
            Log.e("MindControl", "Failed to process accessibility event ${event.eventType}", exception)
        }
    }

    private fun handleAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val previousApp = currentForegroundApp
                currentForegroundApp = packageName
                val wasInTrackedApp = isInTrackedApp
                isInTrackedApp = ReelCounterManager.TRACKED_PACKAGES.contains(packageName)

                if (isInTrackedApp && !wasInTrackedApp) {
                    // User ENTERED a tracked app — notify counter and show pill
                    ReelCounterManager.onAppEntered(packageName)
                    val showIntent = Intent(this, BrainOverlayService::class.java)
                    showIntent.action = "SHOW_PILL"
                    startService(showIntent)
                }

                if (!isInTrackedApp && wasInTrackedApp) {
                    // User LEFT a tracked app — hide pill but keep service alive
                    Log.d("ReelCounter", "Left tracked app, hiding pill (service stays alive)")
                    val hideIntent = Intent(this, BrainOverlayService::class.java)
                    hideIntent.action = "HIDE_PILL"
                    startService(hideIntent)

                    // Reset the reel session for the app they left
                    // so next time they enter, counting starts fresh
                    if (previousApp != null) {
                        ReelCounterManager.resetAppSession(previousApp)
                    }
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (!ReelCounterManager.TRACKED_PACKAGES.contains(packageName)) return

                val rootNode = rootInActiveWindow
                try {
                    ReelCounterManager.analyzeAccessibilityEvent(this, event, rootNode)
                } finally {
                    @Suppress("DEPRECATION")
                    rootNode?.recycle()
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("MindControl", "Service Interrupted")
    }
}
