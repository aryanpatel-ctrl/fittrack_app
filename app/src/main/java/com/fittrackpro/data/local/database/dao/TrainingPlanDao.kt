package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.TrainingPlan
import com.fittrackpro.data.local.database.entity.PlanProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: TrainingPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<TrainingPlan>)

    @Update
    suspend fun updatePlan(plan: TrainingPlan)

    @Delete
    suspend fun deletePlan(plan: TrainingPlan)

    @Query("SELECT * FROM training_plans WHERE id = :planId")
    suspend fun getPlanById(planId: String): TrainingPlan?

    @Query("SELECT * FROM training_plans")
    fun getAllPlans(): Flow<List<TrainingPlan>>

    @Query("SELECT * FROM training_plans WHERE goalType = :goalType")
    fun getPlansByGoalType(goalType: String): Flow<List<TrainingPlan>>

    @Query("SELECT * FROM training_plans WHERE difficulty = :difficulty")
    fun getPlansByDifficulty(difficulty: String): Flow<List<TrainingPlan>>

    @Query("SELECT * FROM training_plans WHERE isCustom = 0")
    fun getStandardPlans(): Flow<List<TrainingPlan>>

    @Query("SELECT * FROM training_plans WHERE creatorId = :creatorId")
    fun getCustomPlansByCreator(creatorId: String): Flow<List<TrainingPlan>>

    // Plan Progress
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: PlanProgress)

    @Update
    suspend fun updateProgress(progress: PlanProgress)

    @Query("SELECT * FROM plan_progress WHERE userId = :userId AND status = 'active'")
    suspend fun getActivePlanProgress(userId: String): PlanProgress?

    @Query("SELECT * FROM plan_progress WHERE userId = :userId AND status = 'active'")
    fun getActivePlanProgressFlow(userId: String): Flow<PlanProgress?>

    @Query("SELECT * FROM plan_progress WHERE userId = :userId")
    fun getAllProgressByUser(userId: String): Flow<List<PlanProgress>>

    @Query("SELECT * FROM plan_progress WHERE userId = :userId AND planId = :planId")
    suspend fun getProgressByUserAndPlan(userId: String, planId: String): PlanProgress?

    @Query("""
        UPDATE plan_progress
        SET completedWorkouts = completedWorkouts + 1,
            completionPercentage = CAST(completedWorkouts + 1 AS REAL) / totalWorkouts * 100,
            updatedAt = :timestamp
        WHERE userId = :userId AND planId = :planId
    """)
    suspend fun incrementCompletedWorkouts(userId: String, planId: String, timestamp: Long)
}
