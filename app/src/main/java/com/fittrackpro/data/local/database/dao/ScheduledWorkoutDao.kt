package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.ScheduledWorkout
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledWorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledWorkout(workout: ScheduledWorkout)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledWorkouts(workouts: List<ScheduledWorkout>)

    @Update
    suspend fun updateScheduledWorkout(workout: ScheduledWorkout)

    @Delete
    suspend fun deleteScheduledWorkout(workout: ScheduledWorkout)

    @Query("SELECT * FROM scheduled_workouts WHERE id = :workoutId")
    suspend fun getScheduledWorkoutById(workoutId: String): ScheduledWorkout?

    @Query("SELECT * FROM scheduled_workouts WHERE userId = :userId ORDER BY scheduledDate ASC")
    fun getScheduledWorkoutsByUserId(userId: String): Flow<List<ScheduledWorkout>>

    @Query("SELECT * FROM scheduled_workouts WHERE userId = :userId AND scheduledDate >= :startDate AND scheduledDate <= :endDate ORDER BY scheduledDate ASC")
    fun getScheduledWorkoutsByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<ScheduledWorkout>>

    @Query("SELECT * FROM scheduled_workouts WHERE userId = :userId AND status = 'pending' ORDER BY scheduledDate ASC")
    fun getPendingWorkouts(userId: String): Flow<List<ScheduledWorkout>>

    @Query("SELECT * FROM scheduled_workouts WHERE userId = :userId AND scheduledDate = :date")
    suspend fun getWorkoutsForDate(userId: String, date: Long): List<ScheduledWorkout>

    @Query("SELECT * FROM scheduled_workouts WHERE userId = :userId AND status = 'pending' ORDER BY scheduledDate ASC LIMIT 1")
    suspend fun getNextPendingWorkout(userId: String): ScheduledWorkout?

    @Query("UPDATE scheduled_workouts SET status = :status, completedTrackId = :trackId, completedAt = :completedAt WHERE id = :workoutId")
    suspend fun markWorkoutCompleted(workoutId: String, status: String, trackId: String?, completedAt: Long)

    @Query("UPDATE scheduled_workouts SET status = 'skipped' WHERE id = :workoutId")
    suspend fun markWorkoutSkipped(workoutId: String)

    @Query("SELECT COUNT(*) FROM scheduled_workouts WHERE userId = :userId AND status = 'completed'")
    suspend fun getCompletedWorkoutCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM scheduled_workouts WHERE userId = :userId AND status = 'pending' AND scheduledDate < :currentDate")
    suspend fun getMissedWorkoutCount(userId: String, currentDate: Long): Int

    @Query("DELETE FROM scheduled_workouts WHERE userId = :userId")
    suspend fun deleteAllWorkoutsForUser(userId: String)
}
