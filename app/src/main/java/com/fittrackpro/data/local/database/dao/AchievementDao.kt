package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    // Achievement definitions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Query("SELECT * FROM achievements ORDER BY sortOrder")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE id = :achievementId")
    suspend fun getAchievementById(achievementId: String): Achievement?

    @Query("SELECT * FROM achievements WHERE category = :category ORDER BY sortOrder")
    fun getAchievementsByCategory(category: String): Flow<List<Achievement>>

    // User Achievements
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAchievement(userAchievement: UserAchievement)

    @Update
    suspend fun updateUserAchievement(userAchievement: UserAchievement)

    @Query("SELECT * FROM user_achievements WHERE userId = :userId")
    fun getUserAchievements(userId: String): Flow<List<UserAchievement>>

    @Query("SELECT * FROM user_achievements WHERE userId = :userId AND isUnlocked = 1 ORDER BY earnedAt DESC")
    fun getUnlockedAchievements(userId: String): Flow<List<UserAchievement>>

    @Query("SELECT * FROM user_achievements WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun getUserAchievement(userId: String, achievementId: String): UserAchievement?

    @Query("SELECT COUNT(*) FROM user_achievements WHERE userId = :userId AND isUnlocked = 1")
    suspend fun getUnlockedCount(userId: String): Int

    @Query("UPDATE user_achievements SET progress = :progress, updatedAt = :timestamp WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun updateAchievementProgress(userId: String, achievementId: String, progress: Double, timestamp: Long)

    @Query("UPDATE user_achievements SET isUnlocked = 1, earnedAt = :earnedAt, updatedAt = :timestamp WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun unlockAchievement(userId: String, achievementId: String, earnedAt: Long, timestamp: Long)

    @Query("SELECT * FROM user_achievements WHERE userId = :userId AND isUnlocked = 1 AND notified = 0")
    suspend fun getUnnotifiedAchievements(userId: String): List<UserAchievement>

    @Query("UPDATE user_achievements SET notified = 1 WHERE id = :id")
    suspend fun markAchievementNotified(id: Long)

    // Streaks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: Streak)

    @Update
    suspend fun updateStreak(streak: Streak)

    @Query("SELECT * FROM streaks WHERE userId = :userId")
    fun getStreaks(userId: String): Flow<List<Streak>>

    @Query("SELECT * FROM streaks WHERE userId = :userId AND streakType = :streakType")
    suspend fun getStreakByType(userId: String, streakType: String): Streak?

    @Query("""
        UPDATE streaks
        SET currentCount = currentCount + 1,
            bestCount = MAX(bestCount, currentCount + 1),
            lastActivityDate = :activityDate,
            updatedAt = :timestamp
        WHERE userId = :userId AND streakType = :streakType
    """)
    suspend fun incrementStreak(userId: String, streakType: String, activityDate: Long, timestamp: Long)

    @Query("UPDATE streaks SET currentCount = 0, updatedAt = :timestamp WHERE userId = :userId AND streakType = :streakType")
    suspend fun resetStreak(userId: String, streakType: String, timestamp: Long)

    // XP Transactions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertXpTransaction(transaction: XpTransaction)

    @Query("SELECT * FROM xp_transactions WHERE userId = :userId ORDER BY earnedAt DESC LIMIT :limit")
    fun getRecentXpTransactions(userId: String, limit: Int): Flow<List<XpTransaction>>

    @Query("SELECT SUM(amount) FROM xp_transactions WHERE userId = :userId")
    suspend fun getTotalXp(userId: String): Int?

    @Query("SELECT SUM(amount) FROM xp_transactions WHERE userId = :userId AND earnedAt >= :startDate AND earnedAt <= :endDate")
    suspend fun getXpByDateRange(userId: String, startDate: Long, endDate: Long): Int?
}
