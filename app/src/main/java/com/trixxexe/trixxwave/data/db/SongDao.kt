package com.trixxexe.trixxwave.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY dateAdded DESC")
    fun getLikedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE filePath IS NOT NULL ORDER BY dateAdded DESC")
    fun getDownloadedSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
    
    @Update
    suspend fun updateSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)
    
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): Song?

    @Query("SELECT * FROM songs WHERE originalUrl = :path OR filePath = :path LIMIT 1")
    suspend fun getSongByPath(path: String): Song?

    @Query("SELECT * FROM songs WHERE title = :title AND artist = :artist LIMIT 1")
    suspend fun getSongByTitleAndArtist(title: String, artist: String): Song?

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<Song>>
}

