package com.fittrackpro.ui.tracking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.TrackPointDao
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.data.local.database.entity.TrackPoint
import com.fittrackpro.data.local.database.entity.TrackStatistics
import com.fittrackpro.data.local.database.entity.User
import com.fittrackpro.data.local.database.entity.UserStats
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.service.AchievementService
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
    private val userDao: UserDao,
    private val userPreferences: UserPreferences,
    private val achievementService: AchievementService
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

    private val _steps = MutableLiveData(0)
    val steps: LiveData<Int> = _steps

    private var startStepCount: Int = 0

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

            // Ensure user exists in database before creating track
            val existingUser = userDao.getUserById(userId)
            if (existingUser == null) {
                // Create user record if it doesn't exist
                val newUser = User(
                    id = userId,
                    email = userPreferences.userEmail ?: "user@fittrackpro.app",
                    name = userPreferences.userName ?: "FitTrack User",
                    role = userPreferences.userRole
                )
                userDao.insertUser(newUser)

                // Also create UserStats record
                val newStats = UserStats(
                    userId = userId,
                    totalActivities = 0,
                    totalDistance = 0.0,
                    totalDuration = 0L,
                    totalCalories = 0,
                    totalElevationGain = 0.0,
                    totalXp = 0,
                    level = 1
                )
                userDao.insertStats(newStats)
            } else {
                // Ensure stats exist for existing user
                val existingStats = userDao.getStatsByUserId(userId)
                if (existingStats == null) {
                    val newStats = UserStats(
                        userId = userId,
                        totalActivities = 0,
                        totalDistance = 0.0,
                        totalDuration = 0L,
                        totalCalories = 0,
                        totalElevationGain = 0.0,
                        totalXp = 0,
                        level = 1
                    )
                    userDao.insertStats(newStats)
                }
            }

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
                val userId = userPreferences.userId

                // Update track
                trackDao.updateTrackStatus(trackId, "completed", endTime)

                val finalDistance = _distance.value ?: 0.0
                val finalDuration = _duration.value ?: 0L
                val finalCalories = _calories.value ?: 0
                val finalElevation = _elevation.value ?: 0.0

                // Save statistics
                val statistics = TrackStatistics(
                    trackId = trackId,
                    distance = finalDistance,
                    duration = finalDuration,
                    totalTime = endTime - startTime,
                    avgSpeed = calculateAverageSpeed(),
                    maxSpeed = 0f, // Would be calculated from track points
                    calories = finalCalories,
                    elevationGain = finalElevation,
                    steps = _steps.value ?: 0
                )
                trackDao.insertStatistics(statistics)

                // Update user stats
                if (userId != null) {
                    // Ensure UserStats exists for the user
                    val existingStats = userDao.getStatsByUserId(userId)
                    if (existingStats == null) {
                        val newStats = UserStats(
                            userId = userId,
                            totalActivities = 0,
                            totalDistance = 0.0,
                            totalDuration = 0L,
                            totalCalories = 0,
                            totalElevationGain = 0.0,
                            totalXp = 0,
                            level = 1
                        )
                        userDao.insertStats(newStats)
                    }

                    // Update stats with this activity's data
                    userDao.updateStatsAfterActivity(
                        userId = userId,
                        distance = finalDistance,
                        duration = finalDuration,
                        calories = finalCalories,
                        elevation = finalElevation,
                        timestamp = endTime
                    )

                    // Check for achievement unlocks after workout completion
                    achievementService.checkAchievementsAfterWorkout(userId, trackId)
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

    fun setStartStepCount(count: Int) {
        startStepCount = count
    }

    fun updateSteps(currentStepCount: Int) {
        val stepsInWorkout = currentStepCount - startStepCount
        _steps.value = if (stepsInWorkout > 0) stepsInWorkout else 0
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
        _steps.value = 0
        startStepCount = 0
        currentTrackId = null
        startTime = 0
        pausedDuration = 0
    }
}
