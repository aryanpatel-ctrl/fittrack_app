package com.fittrackpro.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fittrackpro.data.local.database.entity.DailySteps
import com.fittrackpro.data.local.database.entity.WeeklyStepData
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    // ==================== Insert Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailySteps(dailySteps: DailySteps)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailySteps(dailySteps: DailySteps): Long

    @Update
    suspend fun updateDailySteps(dailySteps: DailySteps)

    // ==================== Query Operations ====================

    /**
     * Get steps for a specific date
     */
    @Query("SELECT * FROM daily_steps WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getStepsForDate(userId: String, date: String): DailySteps?

    /**
     * Get steps for a specific date as Flow for reactive updates
     */
    @Query("SELECT * FROM daily_steps WHERE userId = :userId AND date = :date LIMIT 1")
    fun getStepsForDateFlow(userId: String, date: String): Flow<DailySteps?>

    /**
     * Get steps for today
     */
    @Query("SELECT * FROM daily_steps WHERE userId = :userId AND date = :today LIMIT 1")
    suspend fun getTodaySteps(userId: String, today: String): DailySteps?

    /**
     * Get today's steps as Flow
     */
    @Query("SELECT * FROM daily_steps WHERE userId = :userId AND date = :today LIMIT 1")
    fun getTodayStepsFlow(userId: String, today: String): Flow<DailySteps?>

    /**
     * Get steps for the last N days
     */
    @Query("""
        SELECT * FROM daily_steps
        WHERE userId = :userId
        ORDER BY date DESC
        LIMIT :days
    """)
    suspend fun getRecentSteps(userId: String, days: Int): List<DailySteps>

    /**
     * Get steps for a date range
     */
    @Query("""
        SELECT * FROM daily_steps
        WHERE userId = :userId
        AND date >= :startDate
        AND date <= :endDate
        ORDER BY date ASC
    """)
    suspend fun getStepsByDateRange(userId: String, startDate: String, endDate: String): List<DailySteps>

    /**
     * Get steps for a date range as Flow
     */
    @Query("""
        SELECT * FROM daily_steps
        WHERE userId = :userId
        AND date >= :startDate
        AND date <= :endDate
        ORDER BY date ASC
    """)
    fun getStepsByDateRangeFlow(userId: String, startDate: String, endDate: String): Flow<List<DailySteps>>

    /**
     * Get weekly step data for chart display (last 7 days)
     */
    @Query("""
        SELECT date, stepCount, goal
        FROM daily_steps
        WHERE userId = :userId
        AND date >= :startDate
        ORDER BY date ASC
    """)
    suspend fun getWeeklyStepData(userId: String, startDate: String): List<WeeklyStepData>

    // ==================== Aggregate Queries ====================

    /**
     * Get total steps for a date range
     */
    @Query("""
        SELECT COALESCE(SUM(stepCount), 0)
        FROM daily_steps
        WHERE userId = :userId
        AND date >= :startDate
        AND date <= :endDate
    """)
    suspend fun getTotalSteps(userId: String, startDate: String, endDate: String): Int

    /**
     * Get average steps per day for a date range
     */
    @Query("""
        SELECT COALESCE(AVG(stepCount), 0)
        FROM daily_steps
        WHERE userId = :userId
        AND date >= :startDate
        AND date <= :endDate
    """)
    suspend fun getAverageSteps(userId: String, startDate: String, endDate: String): Int

    /**
     * Get total distance for a date range
     */
    @Query("""
        SELECT COALESCE(SUM(distanceMeters), 0)
        FROM daily_steps
        WHERE userId = :userId
        AND date >= :startDate
        AND date <= :endDate
    """)
    suspend fun getTotalDistance(userId: String, startDate: String, endDate: String): Double

    /**
     * Get total calories burned from steps for a date range
     */
    @Query("""
        SELECT COALESCE(SUM(caloriesBurned), 0)
        FROM daily_steps
        WHERE userId = :userId
        AND date >= :startDate
        AND date <= :endDate
    """)
    suspend fun getTotalCalories(userId: String, startDate: String, endDate: String): Int

    /**
     * Get count of days where goal was achieved
     */
    @Query("""
        SELECT COUNT(*)
        FROM daily_steps
        WHERE userId = :userId
        AND goalAchieved = 1
        AND date >= :startDate
        AND date <= :endDate
    """)
    suspend fun getGoalsAchievedCount(userId: String, startDate: String, endDate: String): Int

    /**
     * Get the best (highest) step count
     */
    @Query("""
        SELECT MAX(stepCount)
        FROM daily_steps
        WHERE userId = :userId
    """)
    suspend fun getBestStepCount(userId: String): Int?

    /**
     * Get all step records for a user
     */
    @Query("SELECT * FROM daily_steps WHERE userId = :userId ORDER BY date DESC")
    fun getAllStepsFlow(userId: String): Flow<List<DailySteps>>

    /**
     * Get count of tracked days
     */
    @Query("SELECT COUNT(*) FROM daily_steps WHERE userId = :userId")
    suspend fun getTrackedDaysCount(userId: String): Int

    // ==================== Update Operations ====================

    /**
     * Update step count for a specific date
     */
    @Query("""
        UPDATE daily_steps
        SET stepCount = :stepCount,
            goalAchieved = :stepCount >= goal,
            distanceMeters = :distanceMeters,
            caloriesBurned = :caloriesBurned,
            updatedAt = :updatedAt
        WHERE userId = :userId AND date = :date
    """)
    suspend fun updateStepCount(
        userId: String,
        date: String,
        stepCount: Int,
        distanceMeters: Double,
        caloriesBurned: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    /**
     * Update step goal for a specific date
     */
    @Query("""
        UPDATE daily_steps
        SET goal = :goal,
            goalAchieved = stepCount >= :goal,
            updatedAt = :updatedAt
        WHERE userId = :userId AND date = :date
    """)
    suspend fun updateStepGoal(
        userId: String,
        date: String,
        goal: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    // ==================== Delete Operations ====================

    /**
     * Delete step record for a specific date
     */
    @Query("DELETE FROM daily_steps WHERE userId = :userId AND date = :date")
    suspend fun deleteStepsForDate(userId: String, date: String)

    /**
     * Delete all step records for a user
     */
    @Query("DELETE FROM daily_steps WHERE userId = :userId")
    suspend fun deleteAllSteps(userId: String)

    /**
     * Delete old step records (older than specified date)
     */
    @Query("DELETE FROM daily_steps WHERE userId = :userId AND date < :cutoffDate")
    suspend fun deleteOldSteps(userId: String, cutoffDate: String)
}
