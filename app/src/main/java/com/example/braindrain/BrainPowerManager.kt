package com.example.braindrain

import android.content.Context
import android.content.SharedPreferences

object BrainPowerManager {
    private const val PREFS_NAME = "BrainPowerPrefs"
    private const val KEY_BRAIN_POWER = "brain_power"
    private const val KEY_LAST_UPDATE = "last_update"
    private const val KEY_TOTAL_SCROLLS = "total_scrolls"
    private const val MAX_POWER = 100f

    val SUPPORTED_APPS = mapOf(
        "com.instagram.android" to "Instagram",
        "com.facebook.katana" to "Facebook",
        "com.google.android.youtube" to "YouTube",
        "com.snapchat.android" to "Snapchat",
        "com.zhiliaoapp.musically" to "TikTok"
    )

    // We drain 0.5% per scroll event by default
    private var drainPerScroll = 0.5f 

    fun getPower(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var currentPower = prefs.getFloat(KEY_BRAIN_POWER, MAX_POWER)
        val lastUpdate = prefs.getLong(KEY_LAST_UPDATE, System.currentTimeMillis())
        
        // Passive Recharge: 1% per minute away from the app
        val timeElapsedMillis = System.currentTimeMillis() - lastUpdate
        if (timeElapsedMillis > 0) {
            val minutesElapsed = timeElapsedMillis / (1000 * 60).toFloat()
            val rechargeAmount = minutesElapsed * 1.0f // 1% per minute
            
            if (rechargeAmount > 0.1f) {
                currentPower += rechargeAmount
                if (currentPower > MAX_POWER) currentPower = MAX_POWER
                
                // Save the recharged power
                prefs.edit()
                    .putFloat(KEY_BRAIN_POWER, currentPower)
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .apply()
            }
        }
        
        return currentPower
    }

    fun getTotalScrolls(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TOTAL_SCROLLS, 0)
    }

    fun getAppScrolls(context: Context, packageName: String): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("scrolls_$packageName", 0)
    }

    fun getTopDrainer(context: Context): Pair<String, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var maxApp = "None"
        var maxScrolls = 0
        for ((pkg, name) in SUPPORTED_APPS) {
            val scrolls = prefs.getInt("scrolls_$pkg", 0)
            if (scrolls >= maxScrolls && scrolls > 0) {
                maxScrolls = scrolls
                maxApp = name
            }
        }
        return Pair(maxApp, maxScrolls)
    }

    fun setDrainRate(rate: Float) {
        drainPerScroll = rate
    }

    fun drainPower(context: Context, packageName: String): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var currentPower = getPower(context) // Apply passive recharge first
        
        currentPower -= drainPerScroll
        if (currentPower < 0f) currentPower = 0f
        
        val totalScrolls = prefs.getInt(KEY_TOTAL_SCROLLS, 0) + 1
        val appScrolls = prefs.getInt("scrolls_$packageName", 0) + 1

        prefs.edit()
            .putFloat(KEY_BRAIN_POWER, currentPower)
            .putInt(KEY_TOTAL_SCROLLS, totalScrolls)
            .putInt("scrolls_$packageName", appScrolls)
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
            
        return currentPower
    }

    fun resetPower(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_BRAIN_POWER, MAX_POWER)
            .putInt(KEY_TOTAL_SCROLLS, 0)
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }
}
