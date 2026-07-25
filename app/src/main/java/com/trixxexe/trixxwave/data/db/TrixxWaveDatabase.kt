package com.trixxexe.trixxwave.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Song::class,
        Album::class,
        Artist::class,
        Playlist::class,
        PlaylistSongCrossRef::class,
        Profile::class,
        PlayHistoryEntry::class,
        LyricsCache::class,
        LibrarySourceFolder::class,
        RecentSearch::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TrixxWaveDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun profileDao(): ProfileDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun librarySourceDao(): LibrarySourceDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        @Volatile
        private var INSTANCE: TrixxWaveDatabase? = null

        fun getDatabase(context: Context): TrixxWaveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrixxWaveDatabase::class.java,
                    "trixxwave_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        // Create default initial profile
                        database.profileDao().insertProfile(
                            Profile(
                                name = "Main Profile",
                                themePreset = "Frosted",
                                isActive = true
                            )
                        )
                    }
                }
            }
        }
    }
}
