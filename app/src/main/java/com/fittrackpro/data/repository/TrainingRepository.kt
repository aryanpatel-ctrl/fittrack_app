package com.fittrackpro.data.repository

import com.fittrackpro.data.local.database.dao.TrainingPlanDao
import com.fittrackpro.data.local.database.dao.ScheduledWorkoutDao
import com.fittrackpro.data.local.database.dao.WorkoutTemplateDao
import com.fittrackpro.data.local.database.entity.PlanProgress
import com.fittrackpro.data.local.database.entity.ScheduledWorkout
import com.fittrackpro.data.local.database.entity.TrainingPlan
import com.fittrackpro.data.local.database.entity.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainingRepository @Inject constructor(
    private val trainingPlanDao: TrainingPlanDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val scheduledWorkoutDao: ScheduledWorkoutDao
) {
    suspend fun getActivePlan(userId: String): TrainingPlan? {
        val progress = trainingPlanDao.getActivePlanProgress(userId) ?: return null
        return trainingPlanDao.getPlanById(progress.planId)
    }
    fun getAllPlans(): Flow<List<TrainingPlan>> = trainingPlanDao.getAllPlans()
    suspend fun insertPlan(plan: TrainingPlan) = trainingPlanDao.insertPlan(plan)
    fun getWorkoutTemplates(planId: String): Flow<List<WorkoutTemplate>> = workoutTemplateDao.getTemplatesByPlanId(planId)
    fun getScheduledWorkouts(userId: String): Flow<List<ScheduledWorkout>> = scheduledWorkoutDao.getPendingWorkouts(userId)
    suspend fun insertScheduledWorkout(workout: ScheduledWorkout) = scheduledWorkoutDao.insertScheduledWorkout(workout)
    suspend fun getPlanProgress(userId: String): PlanProgress? = trainingPlanDao.getActivePlanProgress(userId)
    suspend fun updatePlanProgress(progress: PlanProgress) = trainingPlanDao.insertProgress(progress)
}
