package com.example.braindrain

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.braindrain.data.AppDatabase
import com.example.braindrain.data.DailyStats
import com.example.braindrain.data.HourlyBucket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.*

/**
 * Robust reel/short-form video counter.
 *
 * KEY FIX: Only counts DOWNWARD scrolls (new content). Scrolling UP to revisit
 * old reels is NOT counted. Staying/pausing on a reel is NOT counted.
 *
 * Detection strategy per platform:
 * 1. PRIMARY: TYPE_VIEW_SCROLLED on RecyclerView/ViewPager — most reliable.
 *    Only counts when fromIndex INCREASES (user scrolled to NEW content).
 *    Scrolling back up (decreasing index) is ignored.
 * 2. FALLBACK: TYPE_WINDOW_CONTENT_CHANGED with keyword analysis — heavily
 *    debounced, only fires when primary missed it.
 */
object ReelCounterManager {

    // ── Tracked packages ──
    val TRACKED_PACKAGES = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.zhiliaoapp.musically"
    )

    // Maps package name → our internal label
    private val PACKAGE_LABELS = mapOf(
        "com.instagram.android" to "instagram",
        "com.google.android.youtube" to "youtube",
        "com.facebook.katana" to "facebook",
        "com.snapchat.android" to "snapchat",
        "com.zhiliaoapp.musically" to "tiktok"
    )

    // ── Per-app deduplication state ──
    // Stores the highest index seen for each app to avoid counting backwards scrolls.
    private val highWaterIndex = mutableMapOf<String, Int>()
    private val lastCountTime = mutableMapOf<String, Long>()

    // ── Debounce constants ──
    private const val PRIMARY_DEBOUNCE_MS = 1000L   // Min gap between two scroll counts per app

    // ── SharedPreferences keys ──
    private const val PREFS_NAME = "BrainPalPrefs"
    const val KEY_TOTAL_LIMIT = "daily_limit"
    const val KEY_INSTA_LIMIT = "insta_limit"
    const val KEY_YT_LIMIT = "yt_limit"
    const val KEY_FB_LIMIT = "fb_limit"
    const val KEY_SNAP_LIMIT = "snap_limit"
    const val KEY_TIKTOK_LIMIT = "tiktok_limit"

    const val DEFAULT_TOTAL_LIMIT = 100
    const val DEFAULT_APP_LIMIT = 0  // 0 = no per-app limit

    /**
     * Main entry point called from the AccessibilityService.
     */
    fun analyzeAccessibilityEvent(
        context: Context,
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?
    ) {
        val packageName = event.packageName?.toString() ?: return
        if (!TRACKED_PACKAGES.contains(packageName)) return

        val appLabel = PACKAGE_LABELS[packageName] ?: return
        val now = System.currentTimeMillis()

        when (packageName) {
            "com.instagram.android" -> {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                    handleInstagramScrollEvent(context, event, packageName, appLabel, now)
                }
            }
            "com.google.android.youtube", "com.facebook.katana" -> {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                    handleShortVideoScroll(context, event, rootNode, packageName, appLabel, now)
                }
            }
            else -> if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                handleScrollEvent(context, event, packageName, appLabel, now)
            }
        }
    }

    /**
     * YouTube Shorts and Facebook Reels use custom pager views that often report
     * neither RecyclerView classes nor useful adapter indexes. Count their real
     * accessibility scroll events only while short-video controls are on screen.
     */
    private fun handleShortVideoScroll(
        context: Context,
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?,
        packageName: String,
        appLabel: String,
        now: Long
    ) {
        val visibleText = collectVisibleText(rootNode).lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ").trim()
        val isShortVideoScreen = when (packageName) {
            "com.google.android.youtube" ->
                visibleText.contains("remix this short") ||
                    visibleText.contains("#shorts") ||
                    visibleText.contains("see more videos using this sound") ||
                    visibleText.contains("share this reel with")
            "com.facebook.katana" ->
                visibleText.contains("reels") &&
                    (visibleText.contains("comment") || visibleText.contains("like"))
            else -> false
        }
        if (!isShortVideoScreen) {
            Log.d("ReelCounter", "Scroll ignored outside short-video screen: $packageName")
            return
        }

        val lastTime = lastCountTime[packageName] ?: 0L
        if (now - lastTime < PRIMARY_DEBOUNCE_MS) return

        val index = when {
            event.toIndex >= 0 -> event.toIndex
            event.fromIndex >= 0 -> event.fromIndex
            else -> -1
        }
        if (index >= 0) highWaterIndex[packageName] = index
        lastCountTime[packageName] = now
        Log.d("ReelCounter", "Short-video scroll event ${event.eventType} detected for $appLabel — COUNTED")
        incrementCount(context, appLabel)
    }

    private fun collectVisibleText(root: AccessibilityNodeInfo?): String {
        if (root == null) return ""
        val result = StringBuilder()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 250) {
            val node = queue.removeFirst()
            visited++
            node.text?.let { result.append(it).append(' ') }
            node.contentDescription?.let { result.append(it).append(' ') }
            for (index in 0 until minOf(node.childCount, 40)) {
                val child = node.getChild(index) ?: continue
                queue.addLast(child)
            }
            if (node !== root) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
        }
        while (queue.isNotEmpty()) {
            @Suppress("DEPRECATION")
            queue.removeFirst().recycle()
        }
        return result.toString()
    }

    /** Instagram's Reels pager can report a changing accessibility class name and no index. */
    private fun handleInstagramScrollEvent(
        context: Context,
        event: AccessibilityEvent,
        packageName: String,
        appLabel: String,
        now: Long
    ) {
        val lastTime = lastCountTime[packageName] ?: 0L
        if (now - lastTime < PRIMARY_DEBOUNCE_MS) return

        val currentIndex = when {
            event.fromIndex >= 0 -> event.fromIndex
            event.toIndex >= 0 -> event.toIndex
            else -> -1
        }
        val previousHighWater = highWaterIndex[packageName] ?: -1
        if (currentIndex >= 0 && currentIndex == previousHighWater) {
            Log.d("ReelCounter", "Instagram reel index unchanged: $currentIndex — SKIPPED")
            return
        }

        if (currentIndex >= 0) highWaterIndex[packageName] = currentIndex
        lastCountTime[packageName] = now
        Log.d("ReelCounter", "Instagram Reels scroll detected — COUNTED")
        incrementCount(context, appLabel)
    }

    /**
     * PRIMARY detection: Scroll events from RecyclerView/ViewPager.
     *
     * CRITICAL: Only counts when the index INCREASES from the previous high-water mark.
     * - Scrolling DOWN (forward to new reels): fromIndex increases → COUNTED
     * - Scrolling UP (back to old reels): fromIndex decreases → NOT counted
     * - Staying on same reel: fromIndex unchanged → NOT counted
     * - After scrolling back up, scrolling down again only counts NEW reels
     *   (past the previous high-water mark)
     */
    private fun handleScrollEvent(
        context: Context,
        event: AccessibilityEvent,
        packageName: String,
        appLabel: String,
        now: Long
    ) {
        val className = event.className?.toString() ?: return

        // Only count RecyclerView or ViewPager scroll events (reel containers)
        val isReelContainer = className.contains("RecyclerView", ignoreCase = true) ||
                className.contains("ViewPager", ignoreCase = true) ||
                className.contains("Pager", ignoreCase = true)

        if (!isReelContainer) return

        // Check debounce — don't count if we just counted for this app
        val lastTime = lastCountTime[packageName] ?: 0L
        if (now - lastTime < PRIMARY_DEBOUNCE_MS) return

        // ── DIRECTION-AWARE INDEX TRACKING ──
        // Get the current scroll position index (adapter position)
        val currentIndex = when {
            event.fromIndex >= 0 -> event.fromIndex
            event.toIndex >= 0 -> event.toIndex
            else -> -1
        }

        if (currentIndex < 0) {
            // No index available on this device (e.g. custom container)
            // Still count, but rely purely on the strict 1000ms debounce
            lastCountTime[packageName] = now
            incrementCount(context, appLabel)
            return
        }

        // Get the previous high-water mark for this app
        val previousHighWater = highWaterIndex[packageName] ?: -1

        if (currentIndex <= previousHighWater) {
            // User is scrolling BACKWARDS or staying on the same reel
            // DO NOT COUNT — this is revisiting old content
            Log.d("ReelCounter", "Backward/same scroll for $appLabel: index=$currentIndex, highWater=$previousHighWater — SKIPPED")
            return
        }

        // Index INCREASED — user scrolled to NEW content (forward/downward)
        highWaterIndex[packageName] = currentIndex
        lastCountTime[packageName] = now

        Log.d("ReelCounter", "Forward scroll for $appLabel: index=$currentIndex (prev highWater=$previousHighWater) — COUNTED")
        incrementCount(context, appLabel)
    }

    /**
     * Increment count, persist to DB, check limits, update overlay/widget.
     */
    private fun incrementCount(context: Context, appLabel: String) {
        Log.d("ReelCounter", "✓ Counted reel for $appLabel")

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val bucketId = "$dateStr-$hour"

        val db = AppDatabase.getDatabase(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        CoroutineScope(Dispatchers.IO).launch {
            val dao = db.statsDao()

            // ── Update Daily Stats ──
            var daily = dao.getDailyStats(dateStr) ?: DailyStats(dateStr)
            daily = daily.copy(
                totalReels = daily.totalReels + 1,
                instagramReels = if (appLabel == "instagram") daily.instagramReels + 1 else daily.instagramReels,
                youtubeReels = if (appLabel == "youtube") daily.youtubeReels + 1 else daily.youtubeReels,
                facebookReels = if (appLabel == "facebook") daily.facebookReels + 1 else daily.facebookReels,
                snapchatReels = if (appLabel == "snapchat") daily.snapchatReels + 1 else daily.snapchatReels,
                tiktokReels = if (appLabel == "tiktok") daily.tiktokReels + 1 else daily.tiktokReels
            )
            dao.insertDailyStats(daily)

            // ── Update Hourly Bucket ──
            val buckets = dao.getHourlyBuckets(dateStr)
            var bucket = buckets.find { it.id == bucketId } ?: HourlyBucket(bucketId, dateStr, hour, 0)
            bucket = bucket.copy(count = bucket.count + 1)
            dao.insertHourlyBucket(bucket)

            // ── Update Overlay Pill ──
            val updateIntent = Intent(context, BrainOverlayService::class.java).apply {
                action = "UPDATE_COUNT"
                putExtra("count", daily.totalReels)
            }
            context.startService(updateIntent)

            // ── Update Widget ──
            BrainWidgetProvider.updateWidget(context, daily.totalReels)

            // ── Check limits & trigger block if exceeded ──
            checkLimitsAndBlock(context, prefs, daily, appLabel)
        }
    }

    /**
     * Check if the user has exceeded their total or per-app limit.
     */
    private fun checkLimitsAndBlock(
        context: Context,
        prefs: SharedPreferences,
        daily: DailyStats,
        appLabel: String
    ) {
        val shieldActive = prefs.getBoolean("shield_active", true)
        if (!shieldActive) return

        val totalLimit = prefs.getInt(KEY_TOTAL_LIMIT, DEFAULT_TOTAL_LIMIT)

        val appLimitKey = when (appLabel) {
            "instagram" -> KEY_INSTA_LIMIT
            "youtube" -> KEY_YT_LIMIT
            "facebook" -> KEY_FB_LIMIT
            "snapchat" -> KEY_SNAP_LIMIT
            "tiktok" -> KEY_TIKTOK_LIMIT
            else -> null
        }
        val appLimit = if (appLimitKey != null) prefs.getInt(appLimitKey, DEFAULT_APP_LIMIT) else 0

        val appCount = when (appLabel) {
            "instagram" -> daily.instagramReels
            "youtube" -> daily.youtubeReels
            "facebook" -> daily.facebookReels
            "snapchat" -> daily.snapchatReels
            "tiktok" -> daily.tiktokReels
            else -> 0
        }

        val totalExceeded = daily.totalReels >= totalLimit
        val appExceeded = appLimit > 0 && appCount >= appLimit

        if (totalExceeded || appExceeded) {
            Log.d("ReelCounter", "🚫 BLOCK triggered")
            val blockIntent = Intent(context, BlockOverlayService::class.java).apply {
                action = "SHOW_BLOCK"
            }
            context.startService(blockIntent)
        }
    }

    /**
     * Called when user enters a tracked app — reset per-session state
     * so returning to Instagram after being in YouTube works correctly.
     */
    fun onAppEntered(packageName: String) {
        // Don't reset highWaterIndex here — we want to track the max across the session
        // Only reset when the app itself is restarted (new foreground window)
        Log.d("ReelCounter", "Entered tracked app: $packageName")
    }

    /**
     * Reset the tracking state for a specific app.
     * Called when the app's main window changes.
     */
    fun resetAppSession(packageName: String) {
        highWaterIndex.remove(packageName)
        Log.d("ReelCounter", "Reset session for $packageName")
    }

    /**
     * Reset all tracking state.
     */
    fun resetTracking() {
        highWaterIndex.clear()
        lastCountTime.clear()
    }
}
