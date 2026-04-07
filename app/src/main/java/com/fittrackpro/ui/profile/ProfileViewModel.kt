package com.fittrackpro.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.AchievementDao
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.User
import com.fittrackpro.data.local.database.entity.UserStats
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.util.BmiCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userDao: UserDao,
    private val trackDao: TrackDao,
    private val achievementDao: AchievementDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?> = _userName

    private val _userStats = MutableLiveData<UserStats>()
    val userStats: LiveData<UserStats> = _userStats

    private val _currentStreak = MutableLiveData<Int>()
    val currentStreak: LiveData<Int> = _currentStreak

    private val _loggedOut = MutableLiveData<Boolean>()
    val loggedOut: LiveData<Boolean> = _loggedOut

    // BMI Data
    private val _bmiValue = MutableLiveData<Float?>()
    val bmiValue: LiveData<Float?> = _bmiValue

    private val _bmiCategory = MutableLiveData<BmiCalculator.BmiCategory>()
    val bmiCategory: LiveData<BmiCalculator.BmiCategory> = _bmiCategory

    private val _userWeight = MutableLiveData<Float?>()
    val userWeight: LiveData<Float?> = _userWeight

    private val _userHeight = MutableLiveData<Float?>()
    val userHeight: LiveData<Float?> = _userHeight

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _userName.value = userPreferences.userName

            val userId = userPreferences.userId
            if (userId != null) {
                // Sync stats from existing activities first
                syncStatsFromActivities(userId)

                // Load user stats
                val stats = userDao.getStatsByUserId(userId)
                _userStats.value = stats ?: UserStats(userId = userId)

                // Load streak
                val streak = achievementDao.getStreakByType(userId, "daily_activity")
                _currentStreak.value = streak?.currentCount ?: 0

                // Load user profile for BMI
                val user = userDao.getUserById(userId)
                user?.let {
                    _userWeight.value = it.weight
                    _userHeight.value = it.height
                    calculateBmi(it.weight, it.height)
                }
            } else {
                _userStats.value = UserStats(userId = "")
                _currentStreak.value = 0
                _bmiCategory.value = BmiCalculator.BmiCategory.UNKNOWN
            }
        }
    }

    private suspend fun syncStatsFromActivities(userId: String) {
        try {
            // Get aggregated stats from all completed activities
            val aggregated = trackDao.getAggregatedStats(userId)

            // Ensure UserStats exists
            var existingStats = userDao.getStatsByUserId(userId)
            if (existingStats == null) {
                existingStats = UserStats(
                    userId = userId,
                    totalActivities = 0,
                    totalDistance = 0.0,
                    totalDuration = 0L,
                    totalCalories = 0,
                    totalElevationGain = 0.0,
                    totalXp = 0,
                    level = 1
                )
                userDao.insertStats(existingStats)
            }

            // Update stats with aggregated values from activities
            val updatedStats = existingStats.copy(
                totalActivities = aggregated.totalActivities,
                totalDistance = aggregated.totalDistance,
                totalDuration = aggregated.totalDuration,
                totalCalories = aggregated.totalCalories,
                totalElevationGain = aggregated.totalElevation,
                updatedAt = System.currentTimeMillis()
            )
            userDao.updateStats(updatedStats)
        } catch (e: Exception) {
            // Silently fail - stats will show current DB values
        }
    }

    private fun calculateBmi(weight: Float?, height: Float?) {
        if (weight != null && height != null && weight > 0 && height > 0) {
            val bmi = BmiCalculator.calculate(weight, height)
            _bmiValue.value = bmi
            _bmiCategory.value = BmiCalculator.getCategory(bmi)
        } else {
            _bmiValue.value = null
            _bmiCategory.value = BmiCalculator.BmiCategory.UNKNOWN
        }
    }

    fun updateUserBodyMetrics(weight: Float, height: Float) {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch

            // Update user in database
            val user = userDao.getUserById(userId)
            user?.let {
                val updatedUser = it.copy(
                    weight = weight,
                    height = height,
                    updatedAt = System.currentTimeMillis()
                )
                userDao.updateUser(updatedUser)

                // Update LiveData
                _userWeight.value = weight
                _userHeight.value = height
                calculateBmi(weight, height)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearUserData()
            _loggedOut.value = true
        }
    }
}
