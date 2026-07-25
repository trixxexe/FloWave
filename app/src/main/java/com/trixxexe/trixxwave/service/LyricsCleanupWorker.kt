package com.trixxexe.trixxwave.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trixxexe.trixxwave.data.db.TrixxWaveDatabase

/**
 * Background WorkManager worker that periodically cleans up old (expired)
 * or orphaned cached lyrics from the Room database to optimize storage.
 */
class LyricsCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = TrixxWaveDatabase.getDatabase(applicationContext)
            val lyricsDao = database.lyricsDao()

            // Delete lyrics fetched more than 14 days ago
            val fourteenDaysMs = 14 * 24 * 60 * 60 * 1000L
            val expirationTimestamp = System.currentTimeMillis() - fourteenDaysMs

            val deletedExpired = lyricsDao.deleteExpiredLyrics(expirationTimestamp)
            val deletedOrphaned = lyricsDao.deleteOrphanedLyrics()

            Log.d(
                TAG,
                "Lyrics DB cleanup worker completed: $deletedExpired expired records and $deletedOrphaned orphaned records removed."
            )

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing lyrics DB cleanup: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "LyricsCleanupWorker"
        private const val TAG = "LyricsCleanupWorker"
    }
}
