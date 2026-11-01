package com.example.braindrain.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey val date: String,
    val totalReels: Int = 0,
    val instagramReels: Int = 0,
    val youtubeReels: Int = 0,
    val facebookReels: Int = 0,
    val snapchatReels: Int = 0,
    val tiktokReels: Int = 0,
    val timeSpentSeconds: Long = 0L
)

@Entity(tableName = "hourly_bucket")
data class HourlyBucket(
    @PrimaryKey val id: String,
    val date: String,
    val hour: Int,
    val count: Int = 0
)

@Entity(tableName = "friend_score")
data class FriendScore(
    @PrimaryKey val userId: String,
    val displayName: String,
    val avatarUrl: String,
    val todayCount: Int,
    val rank: Int
)
