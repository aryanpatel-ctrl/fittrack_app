package com.fittrackpro.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.data.remote.firebase.FirestoreService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val trackDao: TrackDao,
    private val firestoreService: FirestoreService,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userId = userPreferences.userId ?: return Result.failure()

            // Get all tracks for the user and filter unsynced ones
            val allTracks = trackDao.getTracksByUserIdFlow(userId).first()
            val unsyncedTracks = allTracks.filter { it.syncedAt == null }

            for (track in unsyncedTracks) {
                val trackData = mapOf(
                    "id" to track.id, "userId" to track.userId,
                    "activityType" to track.activityType,
                    "startTime" to track.startTime,
                    "endTime" to (track.endTime ?: 0L),
                    "status" to track.status,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestoreService.syncTrack(userId, track.id, trackData)

                // Update the track with synced timestamp
                val syncedTrack = track.copy(syncedAt = System.currentTimeMillis())
                trackDao.updateTrack(syncedTrack)
            }

            userPreferences.lastSyncTime = System.currentTimeMillis()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "sync_worker"
    }
}
