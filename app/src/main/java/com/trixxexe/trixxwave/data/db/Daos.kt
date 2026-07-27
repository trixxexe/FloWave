package com.trixxexe.trixxwave.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: Long): Song?

    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY dateAdded DESC")
    fun getLikedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC LIMIT 30")
    fun getMostPlayedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT 30")
    fun getRecentlyAddedSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isLiked = 1 AND id NOT IN (SELECT songId FROM play_history WHERE timestamp > :sinceTimestamp) ORDER BY RANDOM() LIMIT 20")
    fun getForgottenFavorites(sinceTimestamp: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchSongs(query: String): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Query("SELECT filePath FROM songs")
    suspend fun getAllSongPaths(): List<String>

    @Query("SELECT * FROM songs WHERE filePath = :path LIMIT 1")
    suspend fun getSongByPath(path: String): Song?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongs(songs: List<Song>): List<Long>

    @Update
    suspend fun updateSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: Long)

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun setLiked(songId: Long, isLiked: Boolean)

    @Query("UPDATE songs SET moodTags = :tags WHERE id = :songId")
    suspend fun updateMoodTags(songId: Long, tags: String?)

    @Query("UPDATE songs SET waveformPoints = :waveform WHERE id = :songId")
    suspend fun updateWaveform(songId: Long, waveform: String)

    @Query("UPDATE songs SET trimStartMs = :trimStartMs, trimEndMs = :trimEndMs, isGaplessAnalyzed = 1 WHERE id = :songId")
    suspend fun updateGaplessTrim(songId: Long, trimStartMs: Long, trimEndMs: Long)

    @Query("UPDATE songs SET trimStartMs = 0, trimEndMs = 0, isGaplessAnalyzed = 0 WHERE id = :songId")
    suspend fun resetGaplessTrim(songId: Long)

    @Query("UPDATE songs SET trimStartMs = 0, trimEndMs = 0, isGaplessAnalyzed = 0")
    suspend fun resetAllGaplessTrim()

    @Query("SELECT * FROM songs WHERE isGaplessAnalyzed = 0")
    suspend fun getUnanalyzedSongs(): List<Song>
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_song_cross_ref ref ON s.id = ref.songId
        WHERE ref.playlistId = :playlistId
        ORDER BY ref.position ASC
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>>
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<Profile?>

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfileSync(): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun clearActiveProfiles()

    @Transaction
    suspend fun setActiveProfile(profileId: Long) {
        clearActiveProfiles()
        setActiveById(profileId)
    }

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :profileId")
    suspend fun setActiveById(profileId: Long)

    @Delete
    suspend fun deleteProfile(profile: Profile)
}

@Dao
interface PlayHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PlayHistoryEntry)

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN play_history ph ON s.id = ph.songId
        ORDER BY ph.timestamp DESC LIMIT 30
    """)
    fun getRecentlyPlayedSongs(): Flow<List<Song>>
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId LIMIT 1")
    suspend fun getLyricsForSong(songId: Long): LyricsCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsCache)

    @Query("DELETE FROM lyrics_cache WHERE fetchedAt < :expirationTimestamp")
    suspend fun deleteExpiredLyrics(expirationTimestamp: Long): Int

    @Query("DELETE FROM lyrics_cache WHERE songId NOT IN (SELECT id FROM songs)")
    suspend fun deleteOrphanedLyrics(): Int

    @Query("DELETE FROM lyrics_cache")
    suspend fun clearAllLyrics(): Int
}

@Dao
interface LibrarySourceDao {
    @Query("SELECT * FROM library_source_folders ORDER BY id ASC")
    fun getAllFolders(): Flow<List<LibrarySourceFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: LibrarySourceFolder): Long

    @Delete
    suspend fun deleteFolder(folder: LibrarySourceFolder)
}

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 15")
    fun getRecentSearches(): Flow<List<RecentSearch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: RecentSearch)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}
