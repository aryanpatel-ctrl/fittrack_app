package com.fittrackpro.data.repository

import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.TrackPointDao
import com.fittrackpro.data.local.database.entity.PersonalRecord
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.data.local.database.entity.TrackPoint
import com.fittrackpro.data.local.database.entity.TrackStatistics
import com.fittrackpro.data.remote.firebase.FirestoreService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao,
    private val firestoreService: FirestoreService
) {
    fun getTracksForUser(userId: String): Flow<List<Track>> = trackDao.getTracksByUserIdFlow(userId)
    suspend fun getTrackById(trackId: String): Track? = trackDao.getTrackById(trackId)
    suspend fun getTrackStatistics(trackId: String): TrackStatistics? = trackDao.getStatisticsByTrackId(trackId)
    suspend fun getTrackPoints(trackId: String): List<TrackPoint> = trackPointDao.getTrackPointsByTrackId(trackId)
    suspend fun insertTrack(track: Track) = trackDao.insertTrack(track)
    suspend fun updateTrack(track: Track) = trackDao.updateTrack(track)
    suspend fun insertTrackPoint(point: TrackPoint) = trackPointDao.insertTrackPoint(point)
    suspend fun insertTrackStatistics(stats: TrackStatistics) = trackDao.insertStatistics(stats)
    fun getTracksByDateRange(userId: String, startTime: Long, endTime: Long): Flow<List<Track>> = trackDao.getTracksByDateRange(userId, startTime, endTime)
    fun getPersonalRecords(userId: String): Flow<List<PersonalRecord>> = trackDao.getPersonalRecords(userId)
    suspend fun deleteTrack(track: Track) = trackDao.deleteTrack(track)
}
