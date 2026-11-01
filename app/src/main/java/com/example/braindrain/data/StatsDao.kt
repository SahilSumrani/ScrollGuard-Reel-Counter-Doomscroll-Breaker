package com.example.braindrain.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StatsDao {
    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getDailyStats(date: String): DailyStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStats(stats: DailyStats)

    @Query("SELECT * FROM hourly_bucket WHERE date = :date")
    suspend fun getHourlyBuckets(date: String): List<HourlyBucket>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyBucket(bucket: HourlyBucket)
    
    @Query("SELECT * FROM friend_score ORDER BY rank ASC")
    suspend fun getFriends(): List<FriendScore>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendScore>)
}
