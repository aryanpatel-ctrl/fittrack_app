package com.fittrackpro.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.AchievementDao
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.UserStats
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userDao: UserDao,
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

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _userName.value = userPreferences.userName

            val userId = userPreferences.userId
            if (userId != null) {
                // Load user stats
                val stats = userDao.getStatsByUserId(userId)
                _userStats.value = stats ?: UserStats(userId = userId)

                // Load streak
                val streak = achievementDao.getStreakByType(userId, "daily_activity")
                _currentStreak.value = streak?.currentCount ?: 0
            } else {
                _userStats.value = UserStats(userId = "")
                _currentStreak.value = 0
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
