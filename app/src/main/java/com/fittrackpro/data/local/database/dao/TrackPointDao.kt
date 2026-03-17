package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.TrackPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoint(trackPoint: TrackPoint)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoints(trackPoints: List<TrackPoint>)

    @Delete
    suspend fun deleteTrackPoint(trackPoint: TrackPoint)

    @Query("DELETE FROM track_points WHERE trackId = :trackId")
    suspend fun deleteTrackPointsByTrackId(trackId: String)

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getTrackPointsByTrackId(trackId: String): List<TrackPoint>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC")
    fun getTrackPointsByTrackIdFlow(trackId: String): Flow<List<TrackPoint>>

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTrackPoint(trackId: String): TrackPoint?

    @Query("SELECT * FROM track_points WHERE trackId = :trackId ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstTrackPoint(trackId: String): TrackPoint?

    @Query("SELECT COUNT(*) FROM track_points WHERE trackId = :trackId")
    suspend fun getTrackPointCount(trackId: String): Int

    @Query("SELECT AVG(speed) FROM track_points WHERE trackId = :trackId AND speed > 0")
    suspend fun getAverageSpeed(trackId: String): Float?

    @Query("SELECT MAX(speed) FROM track_points WHERE trackId = :trackId")
    suspend fun getMaxSpeed(trackId: String): Float?

    @Query("SELECT MAX(altitude) FROM track_points WHERE trackId = :trackId")
    suspend fun getMaxAltitude(trackId: String): Double?

    @Query("SELECT MIN(altitude) FROM track_points WHERE trackId = :trackId")
    suspend fun getMinAltitude(trackId: String): Double?

    @Query("SELECT AVG(heartRate) FROM track_points WHERE trackId = :trackId AND heartRate > 0")
    suspend fun getAverageHeartRate(trackId: String): Int?

    @Query("SELECT MAX(heartRate) FROM track_points WHERE trackId = :trackId")
    suspend fun getMaxHeartRate(trackId: String): Int?
}
