package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InjuryDao {
    // Body Parts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyParts(bodyParts: List<BodyPart>)

    @Query("SELECT * FROM body_parts ORDER BY category, name")
    fun getAllBodyParts(): Flow<List<BodyPart>>

    // Injury operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInjury(injury: Injury)

    @Update
    suspend fun updateInjury(injury: Injury)

    @Delete
    suspend fun deleteInjury(injury: Injury)

    @Query("SELECT * FROM injuries WHERE id = :injuryId")
    suspend fun getInjuryById(injuryId: String): Injury?

    @Query("SELECT * FROM injuries WHERE userId = :userId ORDER BY injuryDate DESC")
    fun getInjuriesByUser(userId: String): Flow<List<Injury>>

    @Query("SELECT * FROM injuries WHERE userId = :userId AND status = 'active' ORDER BY injuryDate DESC")
    fun getActiveInjuries(userId: String): Flow<List<Injury>>

    @Query("SELECT * FROM injuries WHERE userId = :userId AND status = 'recovered' ORDER BY recoveryDate DESC")
    fun getRecoveredInjuries(userId: String): Flow<List<Injury>>

    @Query("SELECT COUNT(*) FROM injuries WHERE userId = :userId AND status = 'active'")
    suspend fun getActiveInjuryCount(userId: String): Int

    @Query("UPDATE injuries SET status = :status, recoveryDate = :recoveryDate, updatedAt = :timestamp WHERE id = :injuryId")
    suspend fun updateInjuryStatus(injuryId: String, status: String, recoveryDate: Long?, timestamp: Long)

    // Pain Tracking
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPainTracking(painTracking: PainTracking)

    @Query("SELECT * FROM pain_tracking WHERE injuryId = :injuryId ORDER BY date DESC")
    fun getPainTrackingByInjury(injuryId: String): Flow<List<PainTracking>>

    @Query("SELECT * FROM pain_tracking WHERE injuryId = :injuryId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestPainTracking(injuryId: String): PainTracking?

    @Query("SELECT AVG(painLevel) FROM pain_tracking WHERE injuryId = :injuryId AND date >= :startDate AND date <= :endDate")
    suspend fun getAveragePainLevel(injuryId: String, startDate: Long, endDate: Long): Float?

    // Recovery Activities
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecoveryActivity(activity: RecoveryActivity)

    @Query("SELECT * FROM recovery_activities WHERE injuryId = :injuryId ORDER BY date DESC")
    fun getRecoveryActivitiesByInjury(injuryId: String): Flow<List<RecoveryActivity>>

    @Query("SELECT * FROM recovery_activities WHERE injuryId = :injuryId AND date = :date")
    suspend fun getRecoveryActivitiesForDate(injuryId: String, date: Long): List<RecoveryActivity>

    @Query("SELECT SUM(duration) FROM recovery_activities WHERE injuryId = :injuryId")
    suspend fun getTotalRecoveryTime(injuryId: String): Long?
}
