package com.fittrackpro.data.repository

import com.fittrackpro.data.local.database.dao.AchievementDao
import com.fittrackpro.data.local.database.entity.Achievement
import com.fittrackpro.data.local.database.entity.Streak
import com.fittrackpro.data.local.database.entity.UserAchievement
import com.fittrackpro.data.local.database.entity.XpTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDao: AchievementDao
) {
    fun getAllAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()
    fun getUserAchievements(userId: String): Flow<List<UserAchievement>> = achievementDao.getUserAchievements(userId)
    fun getStreaks(userId: String): Flow<List<Streak>> = achievementDao.getStreaks(userId)
    suspend fun unlockAchievement(userAchievement: UserAchievement) = achievementDao.insertUserAchievement(userAchievement)
    suspend fun addXp(transaction: XpTransaction) = achievementDao.insertXpTransaction(transaction)
    suspend fun updateStreak(userId: String, streakType: String, activityDate: Long) =
        achievementDao.incrementStreak(userId, streakType, activityDate, System.currentTimeMillis())
    suspend fun getTotalXp(userId: String): Int = achievementDao.getTotalXp(userId) ?: 0
}
