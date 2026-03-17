package com.fittrackpro.ui.training

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.TrainingPlanDao
import com.fittrackpro.data.local.database.dao.ScheduledWorkoutDao
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
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _currentPlan = MutableLiveData<TrainingPlan?>()
    val currentPlan: LiveData<TrainingPlan?> = _currentPlan

    private val _upcomingWorkouts = MutableLiveData<List<ScheduledWorkout>>()
    val upcomingWorkouts: LiveData<List<ScheduledWorkout>> = _upcomingWorkouts

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
            }
        }
    }

    private fun loadUpcomingWorkouts() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            scheduledWorkoutDao.getPendingWorkouts(userId).collect { workouts ->
                _upcomingWorkouts.value = workouts.take(5)
            }
        }
    }
}
