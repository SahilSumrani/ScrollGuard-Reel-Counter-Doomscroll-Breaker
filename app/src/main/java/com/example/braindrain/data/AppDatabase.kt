package com.example.braindrain.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DailyStats::class, HourlyBucket::class, FriendScore::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "brain_drain_db"
                )
                    .fallbackToDestructiveMigration()  // Safe for dev; resets DB on schema change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
