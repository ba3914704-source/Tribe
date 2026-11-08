package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.database.dao.TripDao
import com.example.core.database.entity.TripEntity

@Database(entities = [TripEntity::class], version = 1, exportSchema = false)
abstract class TribeDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: TribeDatabase? = null

        fun getDatabase(context: Context): TribeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TribeDatabase::class.java,
                    "tribe_travel_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
