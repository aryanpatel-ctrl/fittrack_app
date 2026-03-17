package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.data.local.database.entity.TrackStatistics
import com.fittrackpro.data.local.database.entity.PersonalRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    // Track operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: Track)

    @Update
    suspend fun updateTrack(track: Track)

    @Delete
    suspend fun deleteTrack(track: Track)

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: String): Track?

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    fun getTrackByIdFlow(trackId: String): Flow<Track?>

    @Query("SELECT * FROM tracks WHERE userId = :userId ORDER BY startTime DESC")
    fun getTracksByUserIdFlow(userId: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentTracks(userId: String, limit: Int): List<Track>

    @Query("SELECT * FROM tracks WHERE userId = :userId AND activityType = :activityType ORDER BY startTime DESC")
    fun getTracksByActivityType(userId: String, activityType: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE userId = :userId AND startTime >= :startDate AND startTime <= :endDate ORDER BY startTime DESC")
    fun getTracksByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE userId = :userId AND status = 'in_progress' LIMIT 1")
    suspend fun getActiveTrack(userId: String): Track?

    @Query("UPDATE tracks SET status = :status, endTime = :endTime WHERE id = :trackId")
    suspend fun updateTrackStatus(trackId: String, status: String, endTime: Long?)

    @Query("SELECT COUNT(*) FROM tracks WHERE userId = :userId")
    suspend fun getTrackCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM tracks WHERE userId = :userId AND startTime >= :startDate AND startTime <= :endDate")
    suspend fun getTrackCountByDateRange(userId: String, startDate: Long, endDate: Long): Int

    // Track Statistics operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatistics(statistics: TrackStatistics)

    @Update
    suspend fun updateStatistics(statistics: TrackStatistics)

    @Query("SELECT * FROM track_statistics WHERE trackId = :trackId")
    suspend fun getStatisticsByTrackId(trackId: String): TrackStatistics?

    @Query("SELECT * FROM track_statistics WHERE trackId = :trackId")
    fun getStatisticsByTrackIdFlow(trackId: String): Flow<TrackStatistics?>

    // Personal Records operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRecord(record: PersonalRecord)

    @Query("SELECT * FROM personal_records WHERE userId = :userId")
    fun getPersonalRecords(userId: String): Flow<List<PersonalRecord>>

    @Query("SELECT * FROM personal_records WHERE userId = :userId AND recordType = :recordType")
    suspend fun getPersonalRecordByType(userId: String, recordType: String): PersonalRecord?

    @Query("SELECT * FROM personal_records WHERE userId = :userId ORDER BY achievedAt DESC LIMIT :limit")
    suspend fun getRecentPersonalRecords(userId: String, limit: Int): List<PersonalRecord>
}
