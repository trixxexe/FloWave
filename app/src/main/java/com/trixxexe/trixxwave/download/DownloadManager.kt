package com.trixxexe.trixxwave.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data class Downloading(val progress: Int) : DownloadStatus()
    data class Completed(val uri: String) : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
}

class DownloadManager(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)
    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    fun startDownload(
        videoId: String,
        url: String,
        title: String,
        artist: String,
        album: String,
        artworkUrl: String?,
        isWebm: Boolean
    ): UUID {
        val inputData = Data.Builder()
            .putString("videoId", videoId)
            .putString("url", url)
            .putString("title", title)
            .putString("artist", artist)
            .putString("album", album)
            .putString("artworkUrl", artworkUrl)
            .putBoolean("isWebm", isWebm)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        workManager.enqueue(request)
        
        // Track state
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(videoId, DownloadStatus.Downloading(0))
        }
        
        // Observe progress
        workManager.getWorkInfoByIdLiveData(request.id).observeForever { workInfo ->
            if (workInfo != null) {
                val stateMap = _downloadStates.value.toMutableMap()
                when (workInfo.state) {
                    androidx.work.WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt("progress", 0)
                        stateMap[videoId] = DownloadStatus.Downloading(progress)
                    }
                    androidx.work.WorkInfo.State.SUCCEEDED -> {
                        val uri = workInfo.outputData.getString("uri") ?: ""
                        stateMap[videoId] = DownloadStatus.Completed(uri)
                    }
                    androidx.work.WorkInfo.State.FAILED -> {
                        val error = workInfo.outputData.getString("error") ?: "Unknown error"
                        stateMap[videoId] = DownloadStatus.Failed(error)
                    }
                    else -> {}
                }
                _downloadStates.value = stateMap
            }
        }
        
        return request.id
    }
}
