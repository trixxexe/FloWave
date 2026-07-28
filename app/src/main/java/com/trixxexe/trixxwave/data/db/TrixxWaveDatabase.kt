package com.trixxexe.trixxwave.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Database(entities = [Song::class, Playlist::class, Profile::class], version = 1, exportSchema = false)
abstract class TrixxWaveDatabase : RoomDatabase() {
    
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: TrixxWaveDatabase? = null

        fun getDatabase(context: Context): TrixxWaveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrixxWaveDatabase::class.java,
                    "trixxwave_database"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
