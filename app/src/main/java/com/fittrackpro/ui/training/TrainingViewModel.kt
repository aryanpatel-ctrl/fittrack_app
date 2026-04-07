package com.fittrackpro.ui.training

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.TrainingPlanDao
import com.fittrackpro.data.local.database.dao.ScheduledWorkoutDao
import com.fittrackpro.data.local.database.dao.WorkoutTemplateDao
import com.fittrackpro.data.local.database.entity.TrainingPlan
import com.fittrackpro.data.local.database.entity.ScheduledWorkout
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val trainingPlanDao: TrainingPlanDao,
    private val scheduledWorkoutDao: ScheduledWorkoutDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _currentPlan = MutableLiveData<TrainingPlan?>()
    val currentPlan: LiveData<TrainingPlan?> = _currentPlan

    private val _planProgress = MutableLiveData<PlanProgressData?>()
    val planProgress: LiveData<PlanProgressData?> = _planProgress

    private val _upcomingWorkouts = MutableLiveData<List<ScheduledWorkoutWithName>>()
    val upcomingWorkouts: LiveData<List<ScheduledWorkoutWithName>> = _upcomingWorkouts

    init {
        loadCurrentPlan()
        loadUpcomingWorkouts()
    }

    private fun loadCurrentPlan() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val progress = trainingPlanDao.getActivePlanProgress(userId)
            if (progress != null) {
                _currentPlan.value = trainingPlanDao.getPlanById(progress.planId)
                _planProgress.value = PlanProgressData(
                    currentWeek = progress.currentWeek,
                    totalWeeks = (_currentPlan.value?.durationWeeks ?: progress.currentWeek),
                    completedWorkouts = progress.completedWorkouts,
                    totalWorkouts = progress.totalWorkouts,
                    completionPercentage = progress.completionPercentage
                )
            } else {
                _currentPlan.value = null
                _planProgress.value = null
            }
        }
    }

    private fun loadUpcomingWorkouts() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            // Show all workouts (pending, completed, skipped) so status changes are visible
            scheduledWorkoutDao.getScheduledWorkoutsByUserId(userId).collect { workouts ->
                // Load workout template names for each scheduled workout
                val workoutsWithNames = workouts.take(10).map { scheduled ->
                    val template = workoutTemplateDao.getTemplateById(scheduled.workoutTemplateId)
                    ScheduledWorkoutWithName(
                        scheduledWorkout = scheduled,
                        workoutName = template?.name ?: "Workout"
                    )
                }
                _upcomingWorkouts.value = workoutsWithNames
            }
        }
    }

    fun refresh() {
        loadCurrentPlan()
        loadUpcomingWorkouts()
    }

    fun deletePlan() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val progress = trainingPlanDao.getActivePlanProgress(userId)
            if (progress != null) {
                // Delete scheduled workouts for this user
                scheduledWorkoutDao.deleteAllWorkoutsForUser(userId)
                // Delete plan progress
                trainingPlanDao.deleteProgress(progress)
                // Delete the plan
                _currentPlan.value?.let { trainingPlanDao.deletePlan(it) }
                // Clear state
                _currentPlan.value = null
                _planProgress.value = null
                _upcomingWorkouts.value = emptyList()
            }
        }
    }
}

data class PlanProgressData(
    val currentWeek: Int,
    val totalWeeks: Int,
    val completedWorkouts: Int,
    val totalWorkouts: Int,
    val completionPercentage: Float
)

data class ScheduledWorkoutWithName(
    val scheduledWorkout: ScheduledWorkout,
    val workoutName: String
)
