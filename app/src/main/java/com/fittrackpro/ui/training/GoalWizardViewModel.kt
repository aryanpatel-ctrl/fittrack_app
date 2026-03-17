package com.fittrackpro.ui.training

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.ScheduledWorkoutDao
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.TrainingPlanDao
import com.fittrackpro.data.local.database.dao.WorkoutTemplateDao
import com.fittrackpro.data.local.database.entity.PlanProgress
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.util.TrainingPlanGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalWizardViewModel @Inject constructor(
    private val trainingPlanDao: TrainingPlanDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val scheduledWorkoutDao: ScheduledWorkoutDao,
    private val trackDao: TrackDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _planCreated = MutableLiveData<Boolean>()
    val planCreated: LiveData<Boolean> = _planCreated

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _planSummary = MutableLiveData<PlanSummary?>()
    val planSummary: LiveData<PlanSummary?> = _planSummary

    /**
     * Generate training plan using the progressive overload algorithm
     *
     * This implements the formula from the project specification:
     * - Week N Distance = Week N-1 x 1.10 (10% increase)
     * - Recovery Week (every 4th week) = -25% volume
     */
    fun generatePlan(goalType: String, difficulty: String, durationStr: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val userId = userPreferences.userId
                if (userId == null) {
                    _errorMessage.value = "User not logged in"
                    _isLoading.value = false
                    return@launch
                }

                val weeks = durationStr.split(" ")[0].toIntOrNull() ?: 8
                val daysPerWeek = when (difficulty.lowercase()) {
                    "beginner" -> 3
                    "intermediate" -> 4
                    "advanced" -> 5
                    else -> 3
                }

                // Fetch user's historical fitness data for personalization
                val userFitnessData = fetchUserFitnessData(userId)

                // Generate plan using the progressive overload algorithm
                val generatedPlan = TrainingPlanGenerator.generatePlan(
                    userId = userId,
                    goalType = goalType,
                    difficulty = difficulty,
                    durationWeeks = weeks,
                    daysPerWeek = daysPerWeek,
                    userFitnessData = userFitnessData,
                    startDate = System.currentTimeMillis()
                )

                // Save the training plan
                trainingPlanDao.insertPlan(generatedPlan.plan)

                // Save all workout templates
                generatedPlan.workoutTemplates.forEach { template ->
                    workoutTemplateDao.insertTemplate(template)
                }

                // Save all scheduled workouts
                scheduledWorkoutDao.insertScheduledWorkouts(generatedPlan.scheduledWorkouts)

                // Create plan progress record
                val progress = PlanProgress(
                    userId = userId,
                    planId = generatedPlan.plan.id,
                    currentWeek = 1,
                    completionPercentage = 0f,
                    startDate = System.currentTimeMillis(),
                    totalWorkouts = generatedPlan.totalWorkouts,
                    completedWorkouts = 0,
                    status = "active"
                )
                trainingPlanDao.insertProgress(progress)

                // Create plan summary for UI
                _planSummary.value = PlanSummary(
                    planName = generatedPlan.plan.name,
                    totalWeeks = weeks,
                    totalWorkouts = generatedPlan.totalWorkouts,
                    workoutsPerWeek = daysPerWeek,
                    difficulty = difficulty,
                    goalType = goalType,
                    startDate = System.currentTimeMillis(),
                    estimatedEndDate = calculateEndDate(weeks),
                    hasProgressiveOverload = true,
                    recoveryWeeks = weeks / 4
                )

                _isLoading.value = false
                _planCreated.value = true

            } catch (e: Exception) {
                _errorMessage.value = "Failed to create plan: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetch user's historical fitness data for plan personalization
     */
    private suspend fun fetchUserFitnessData(userId: String): TrainingPlanGenerator.UserFitnessData? {
        return try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val now = System.currentTimeMillis()

            // Get tracks from last 30 days
            val recentTracks = trackDao.getTracksByDateRange(userId, thirtyDaysAgo, now).first()

            if (recentTracks.isEmpty()) {
                return null // No historical data, will use defaults
            }

            // Calculate statistics
            val statistics = recentTracks.mapNotNull { track ->
                trackDao.getStatisticsByTrackId(track.id)
            }

            val totalDistance = statistics.sumOf { it.distance }
            val avgDistance = if (statistics.isNotEmpty()) totalDistance / statistics.size else 0.0
            val avgPace = if (statistics.isNotEmpty()) {
                statistics.filter { it.avgPace > 0 }.map { it.avgPace }.average().toFloat()
            } else 0f
            val longestDistance = statistics.maxOfOrNull { it.distance } ?: 0.0

            // Calculate workout frequency (workouts per week)
            val uniqueDays = recentTracks.map { it.startTime / (24 * 60 * 60 * 1000) }.distinct().size
            val workoutFrequency = (uniqueDays / 4.3).toInt() // Approximate weeks in 30 days

            TrainingPlanGenerator.UserFitnessData(
                avgDistanceLast30Days = avgDistance,
                avgPaceLast30Days = avgPace,
                workoutFrequency = workoutFrequency,
                longestDistance = longestDistance,
                totalDistanceLast30Days = totalDistance
            )
        } catch (e: Exception) {
            null // Return null on error, will use defaults
        }
    }

    /**
     * Calculate estimated end date
     */
    private fun calculateEndDate(weeks: Int): Long {
        return System.currentTimeMillis() + (weeks * 7L * 24 * 60 * 60 * 1000)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * Summary of generated training plan
 */
data class PlanSummary(
    val planName: String,
    val totalWeeks: Int,
    val totalWorkouts: Int,
    val workoutsPerWeek: Int,
    val difficulty: String,
    val goalType: String,
    val startDate: Long,
    val estimatedEndDate: Long,
    val hasProgressiveOverload: Boolean,
    val recoveryWeeks: Int
)
