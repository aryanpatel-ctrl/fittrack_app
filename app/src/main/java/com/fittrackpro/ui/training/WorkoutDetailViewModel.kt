package com.fittrackpro.ui.training

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.ScheduledWorkoutDao
import com.fittrackpro.data.local.database.dao.WorkoutTemplateDao
import com.fittrackpro.data.local.database.entity.WorkoutTemplate
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val scheduledWorkoutDao: ScheduledWorkoutDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _workout = MutableLiveData<WorkoutTemplate?>()
    val workout: LiveData<WorkoutTemplate?> = _workout

    private var scheduledWorkoutId: String? = null

    fun loadWorkout(workoutId: String, scheduledId: String?) {
        scheduledWorkoutId = scheduledId
        viewModelScope.launch {
            _workout.value = workoutTemplateDao.getTemplateById(workoutId)
        }
    }

    fun completeWorkout() {
        viewModelScope.launch {
            scheduledWorkoutId?.let { id ->
                scheduledWorkoutDao.markWorkoutCompleted(id, "completed", null, System.currentTimeMillis())
            }
        }
    }

    fun skipWorkout() {
        viewModelScope.launch {
            scheduledWorkoutId?.let { id ->
                scheduledWorkoutDao.markWorkoutSkipped(id)
            }
        }
    }
}
