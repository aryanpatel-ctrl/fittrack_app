package com.fittrackpro.ui.achievements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.AchievementDao
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.Achievement
import com.fittrackpro.data.local.database.entity.Streak
import com.fittrackpro.data.local.database.entity.UserAchievement
import com.fittrackpro.data.local.database.entity.UserStats
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementWithStatus(
    val achievement: Achievement,
    val userAchievement: UserAchievement?
)

enum class AchievementFilter { ALL, EARNED, IN_PROGRESS, LOCKED }

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementDao: AchievementDao,
    private val userDao: UserDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _userStats = MutableLiveData<UserStats?>()
    val userStats: LiveData<UserStats?> = _userStats

    private val _achievements = MutableLiveData<List<AchievementWithStatus>>()
    val achievements: LiveData<List<AchievementWithStatus>> = _achievements

    private val _streaks = MutableLiveData<List<Streak>>()
    val streaks: LiveData<List<Streak>> = _streaks

    private var allAchievements: List<AchievementWithStatus> = emptyList()
    private var currentFilter = AchievementFilter.ALL

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            userPreferences.userId?.let { userId ->
                // Load user stats
                _userStats.value = userDao.getStatsByUserId(userId)

                // Load all achievements with user progress
                val achievements = achievementDao.getAllAchievements().first()
                val userAchievements = achievementDao.getUserAchievements(userId).first()
                val userAchievementsMap = userAchievements.associateBy { it.achievementId }

                allAchievements = achievements.map { achievement ->
                    AchievementWithStatus(
                        achievement = achievement,
                        userAchievement = userAchievementsMap[achievement.id]
                    )
                }
                applyFilter()

                // Load streaks
                achievementDao.getStreaks(userId).collect { streakList ->
                    _streaks.value = streakList
                }
            }
        }
    }

    fun filterAchievements(filter: AchievementFilter) {
        currentFilter = filter
        applyFilter()
    }

    private fun applyFilter() {
        _achievements.value = when (currentFilter) {
            AchievementFilter.ALL -> allAchievements
            AchievementFilter.EARNED -> allAchievements.filter {
                it.userAchievement?.earnedAt != null
            }
            AchievementFilter.IN_PROGRESS -> allAchievements.filter {
                val ua = it.userAchievement
                ua != null && ua.earnedAt == null && ua.progress > 0
            }
            AchievementFilter.LOCKED -> allAchievements.filter {
                it.userAchievement == null || it.userAchievement.progress == 0.0
            }
        }
    }

    fun calculateLevelProgress(totalXp: Int, level: Int): Int {
        val xpForCurrentLevel = xpRequiredForLevel(level)
        val xpForNextLevel = xpRequiredForLevel(level + 1)
        val xpInCurrentLevel = totalXp - xpForCurrentLevel
        val xpNeededForNextLevel = xpForNextLevel - xpForCurrentLevel
        return ((xpInCurrentLevel.toFloat() / xpNeededForNextLevel) * 100).toInt()
    }

    fun xpToNextLevel(totalXp: Int, level: Int): Int {
        val xpForNextLevel = xpRequiredForLevel(level + 1)
        return xpForNextLevel - totalXp
    }

    private fun xpRequiredForLevel(level: Int): Int {
        // XP required follows a curve: 100 * level^1.5
        return (100 * Math.pow(level.toDouble(), 1.5)).toInt()
    }
}
