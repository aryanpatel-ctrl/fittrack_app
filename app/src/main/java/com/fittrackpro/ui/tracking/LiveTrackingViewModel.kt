package com.fittrackpro.ui.tracking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.TrackPointDao
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.data.local.database.entity.TrackPoint
import com.fittrackpro.data.local.database.entity.TrackStatistics
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LiveTrackingViewModel @Inject constructor(
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _trackingState = MutableLiveData(TrackingState.IDLE)
    val trackingState: LiveData<TrackingState> = _trackingState

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<Double> = _distance

    private val _duration = MutableLiveData(0L)
    val duration: LiveData<Long> = _duration

    private val _pace = MutableLiveData(0.0)
    val pace: LiveData<Double> = _pace

    private val _calories = MutableLiveData(0)
    val calories: LiveData<Int> = _calories

    private val _elevation = MutableLiveData(0.0)
    val elevation: LiveData<Double> = _elevation

    private val _currentSpeed = MutableLiveData(0f)
    val currentSpeed: LiveData<Float> = _currentSpeed

    private var currentTrackId: String? = null
    private var startTime: Long = 0
    private var pausedDuration: Long = 0
    private var lastPauseTime: Long = 0
    private var timerJob: Job? = null

    private var activityType: String = Constants.ActivityType.RUNNING

    fun setActivityType(type: String) {
        activityType = type
    }

    fun startTracking() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch

            // Create new track
            val trackId = UUID.randomUUID().toString()
            currentTrackId = trackId
            startTime = System.currentTimeMillis()

            val track = Track(
                id = trackId,
                userId = userId,
                activityType = activityType,
                startTime = startTime,
                status = "in_progress"
            )
            trackDao.insertTrack(track)

            _trackingState.value = TrackingState.TRACKING
            startTimer()
        }
    }

    fun pauseTracking() {
        _trackingState.value = TrackingState.PAUSED
        lastPauseTime = System.currentTimeMillis()
        timerJob?.cancel()
    }

    fun resumeTracking() {
        pausedDuration += System.currentTimeMillis() - lastPauseTime
        _trackingState.value = TrackingState.TRACKING
        startTimer()
    }

    fun stopTracking() {
        viewModelScope.launch {
            timerJob?.cancel()

            currentTrackId?.let { trackId ->
                val endTime = System.currentTimeMillis()

                // Update track
                trackDao.updateTrackStatus(trackId, "completed", endTime)

                // Save statistics
                val statistics = TrackStatistics(
                    trackId = trackId,
                    distance = _distance.value ?: 0.0,
                    duration = _duration.value ?: 0L,
                    totalTime = endTime - startTime,
                    avgSpeed = calculateAverageSpeed(),
                    maxSpeed = 0f, // Would be calculated from track points
                    calories = _calories.value ?: 0,
                    elevationGain = _elevation.value ?: 0.0
                )
                trackDao.insertStatistics(statistics)

                // Update user stats
                val userId = userPreferences.userId
                if (userId != null) {
                    // This would update total user stats
                }
            }

            _trackingState.value = TrackingState.STOPPED
            resetValues()
        }
    }

    fun addTrackPoint(latitude: Double, longitude: Double, altitude: Double?, speed: Float?, accuracy: Float?) {
        viewModelScope.launch {
            currentTrackId?.let { trackId ->
                val trackPoint = TrackPoint(
                    trackId = trackId,
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude,
                    speed = speed,
                    accuracy = accuracy,
                    timestamp = System.currentTimeMillis()
                )
                trackPointDao.insertTrackPoint(trackPoint)

                // Update current speed
                speed?.let { _currentSpeed.value = it }

                // Calculate and update distance (would need previous point)
                // Update calories based on distance and activity type
                updateCalories()
            }
        }
    }

    fun updateDistance(newDistance: Double) {
        _distance.value = newDistance
        updateCalories()
        updatePace()
    }

    fun updateElevation(elevationGain: Double) {
        _elevation.value = elevationGain
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime - pausedDuration
                _duration.value = elapsed
                delay(1000)
            }
        }
    }

    private fun updateCalories() {
        val distanceKm = (_distance.value ?: 0.0) / 1000
        val caloriesPerKm = when (activityType) {
            Constants.ActivityType.RUNNING -> Constants.CALORIES_PER_KM_RUNNING
            Constants.ActivityType.CYCLING -> Constants.CALORIES_PER_KM_CYCLING
            Constants.ActivityType.WALKING -> Constants.CALORIES_PER_KM_WALKING
            else -> Constants.CALORIES_PER_KM_RUNNING
        }
        _calories.value = (distanceKm * caloriesPerKm).toInt()
    }

    private fun updatePace() {
        val distance = _distance.value ?: 0.0
        val duration = _duration.value ?: 0L

        if (distance > 0 && duration > 0) {
            // Pace in minutes per km
            val paceMinPerKm = (duration / 60000.0) / (distance / 1000.0)
            _pace.value = paceMinPerKm
        }
    }

    private fun calculateAverageSpeed(): Float {
        val distance = _distance.value ?: 0.0
        val duration = _duration.value ?: 0L

        return if (duration > 0) {
            (distance / (duration / 1000.0)).toFloat() // m/s
        } else {
            0f
        }
    }

    private fun resetValues() {
        _distance.value = 0.0
        _duration.value = 0L
        _pace.value = 0.0
        _calories.value = 0
        _elevation.value = 0.0
        _currentSpeed.value = 0f
        currentTrackId = null
        startTime = 0
        pausedDuration = 0
    }
}
