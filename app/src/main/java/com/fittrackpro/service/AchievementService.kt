package com.fittrackpro.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fittrackpro.R
import com.fittrackpro.data.local.database.dao.AchievementDao
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.Achievement
import com.fittrackpro.data.local.database.entity.UserAchievement
import com.fittrackpro.data.local.database.entity.XpTransaction
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.ui.main.MainActivity
import com.fittrackpro.util.Constants
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Achievement Service
 *
 * Handles achievement tracking, unlocking, and notifications.
 * Called after each workout completion to check for new achievements.
 *
 * Achievement Categories:
 * - DISTANCE: Total distance milestones
 * - STREAK: Consecutive day achievements
 * - CHALLENGE: Challenge participation/wins
 * - TRAINING: Training plan completions
 * - SOCIAL: Social engagement achievements
 * - SPECIAL: Time-based or condition-based achievements
 */
@Singleton
class AchievementService @Inject constructor(
    private val context: Context,
    private val achievementDao: AchievementDao,
    private val trackDao: TrackDao,
    private val userDao: UserDao,
    private val userPreferences: UserPreferences
) {
    companion object {
        private const val CHANNEL_ID = "achievement_notifications"
        private const val CHANNEL_NAME = "Achievement Notifications"
        private const val NOTIFICATION_ID_BASE = 2000
    }

    init {
        createNotificationChannel()
    }

    /**
     * Check for achievement unlocks after workout completion
     */
    suspend fun checkAchievementsAfterWorkout(userId: String, trackId: String) {
        val userStats = calculateUserStats(userId)

        // Get all achievements the user hasn't unlocked yet
        val allAchievements = achievementDao.getAllAchievements().first()
        val userAchievements = achievementDao.getUserAchievements(userId).first()
        val unlockedIds = userAchievements.map { it.achievementId }.toSet()

        val pendingAchievements = allAchievements.filter { it.id !in unlockedIds }

        // Check each pending achievement
        val newlyUnlocked = mutableListOf<Achievement>()

        for (achievement in pendingAchievements) {
            val unlocked = checkAchievementCriteria(achievement, userStats)
            if (unlocked) {
                // Unlock the achievement
                unlockAchievement(userId, achievement)
                newlyUnlocked.add(achievement)
            }
        }

        // Show notifications for newly unlocked achievements
        if (newlyUnlocked.isNotEmpty()) {
            for (achievement in newlyUnlocked) {
                sendAchievementNotification(achievement)
            }
        }
    }

    /**
     * Calculate user's current stats for achievement checking
     */
    private suspend fun calculateUserStats(userId: String): UserStats {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val thirtyDaysAgo = calendar.timeInMillis

        calendar.timeInMillis = now
        calendar.add(Calendar.YEAR, -1)
        val oneYearAgo = calendar.timeInMillis

        // Get all user tracks
        val allTracks = trackDao.getTracksByDateRange(userId, 0, now).first()
        val recentTracks = allTracks.filter { it.startTime >= thirtyDaysAgo }

        // Calculate statistics
        val allStats = allTracks.mapNotNull { trackDao.getStatisticsByTrackId(it.id) }
        val recentStats = recentTracks.mapNotNull { trackDao.getStatisticsByTrackId(it.id) }

        val totalDistance = allStats.sumOf { it.distance }
        val totalWorkouts = allTracks.size
        val totalCalories = allStats.sumOf { it.calories }

        // Calculate streak
        val currentStreak = calculateCurrentStreak(allTracks.map { it.startTime })

        // Get personal records
        val personalRecords = trackDao.getRecentPersonalRecords(userId, 10)

        // Check for special conditions
        val hasEarlyMorningWorkout = recentTracks.any {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.startTime
            cal.get(Calendar.HOUR_OF_DAY) < 6
        }

        val hasRainyWorkout = recentTracks.any {
            it.weatherCondition?.lowercase()?.contains("rain") == true
        }

        return UserStats(
            totalDistance = totalDistance,
            totalWorkouts = totalWorkouts,
            totalCalories = totalCalories,
            currentStreak = currentStreak,
            longestRun = allStats.maxOfOrNull { it.distance } ?: 0.0,
            fastestPace = allStats.filter { it.avgPace > 0 }.minOfOrNull { it.avgPace } ?: 0f,
            hasEarlyMorningWorkout = hasEarlyMorningWorkout,
            hasRainyWorkout = hasRainyWorkout,
            challengesCompleted = 0, // TODO: Get from challenge dao
            plansCompleted = 0 // TODO: Get from training plan dao
        )
    }

    /**
     * Calculate current activity streak (consecutive days)
     */
    private fun calculateCurrentStreak(activityDates: List<Long>): Int {
        if (activityDates.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val uniqueDays = activityDates.map { timestamp ->
            calendar.timeInMillis = timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }.distinct().sortedDescending()

        if (uniqueDays.isEmpty()) return 0

        var streak = 1
        val oneDayMs = 24 * 60 * 60 * 1000L

        // Check if there's activity today or yesterday
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterday = today - oneDayMs

        if (uniqueDays[0] < yesterday) {
            return 0 // Streak broken
        }

        // Count consecutive days
        for (i in 1 until uniqueDays.size) {
            val diff = uniqueDays[i - 1] - uniqueDays[i]
            if (diff == oneDayMs) {
                streak++
            } else if (diff > oneDayMs) {
                break
            }
        }

        return streak
    }

    /**
     * Check if achievement criteria is met
     */
    private fun checkAchievementCriteria(achievement: Achievement, stats: UserStats): Boolean {
        return when (achievement.criteriaType) {
            "total_distance" -> stats.totalDistance >= achievement.criteriaValue
            "total_activities" -> stats.totalWorkouts >= achievement.criteriaValue.toInt()
            "streak_days" -> stats.currentStreak >= achievement.criteriaValue.toInt()
            "single_distance" -> stats.longestRun >= achievement.criteriaValue
            "fastest_5k" -> {
                // Check if user has a 5K record under the threshold
                stats.fastestPace > 0 && stats.fastestPace <= achievement.criteriaValue.toFloat()
            }
            "challenge_wins" -> stats.challengesCompleted >= achievement.criteriaValue.toInt()
            "plans_completed" -> stats.plansCompleted >= achievement.criteriaValue.toInt()
            "early_bird" -> stats.hasEarlyMorningWorkout
            "weather_warrior" -> stats.hasRainyWorkout
            "total_calories" -> stats.totalCalories >= achievement.criteriaValue.toInt()
            else -> false
        }
    }

    /**
     * Unlock an achievement for a user
     */
    private suspend fun unlockAchievement(userId: String, achievement: Achievement) {
        // Create user achievement record
        val userAchievement = UserAchievement(
            userId = userId,
            achievementId = achievement.id,
            progress = achievement.criteriaValue,
            isUnlocked = true,
            earnedAt = System.currentTimeMillis(),
            notified = false
        )
        achievementDao.insertUserAchievement(userAchievement)

        // Award XP
        val xpTransaction = XpTransaction(
            userId = userId,
            amount = achievement.xpReward,
            source = "achievement",
            sourceId = achievement.id,
            description = "Unlocked: ${achievement.name}",
            earnedAt = System.currentTimeMillis()
        )
        achievementDao.insertXpTransaction(xpTransaction)

        // Update user's total XP and level
        updateUserLevel(userId, achievement.xpReward)
    }

    /**
     * Update user's XP and level
     */
    private suspend fun updateUserLevel(userId: String, xpGained: Int) {
        val userStats = userDao.getStatsByUserId(userId) ?: return
        val newTotalXp = userStats.totalXp + xpGained
        val newLevel = calculateLevel(newTotalXp)

        userDao.updateStats(
            userStats.copy(
                totalXp = newTotalXp,
                level = newLevel
            )
        )

        // Check if user leveled up
        if (newLevel > userStats.level) {
            sendLevelUpNotification(newLevel)
        }
    }

    /**
     * Calculate level from XP
     */
    private fun calculateLevel(totalXp: Int): Int {
        val thresholds = Constants.LEVEL_THRESHOLDS
        for (level in thresholds.size - 1 downTo 0) {
            if (totalXp >= thresholds[level]) {
                return level + 1
            }
        }
        return 1
    }

    /**
     * Create notification channel (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for achievement unlocks and level ups"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Send achievement unlock notification
     */
    private fun sendAchievementNotification(achievement: Achievement) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "achievements")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            achievement.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tierEmoji = when (achievement.tier) {
            "bronze" -> "🥉"
            "silver" -> "🥈"
            "gold" -> "🥇"
            "platinum" -> "💎"
            else -> "🏆"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$tierEmoji Achievement Unlocked!")
            .setContentText(achievement.name)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${achievement.name}\n${achievement.description}\n+${achievement.xpReward} XP")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BASE + achievement.id.hashCode(),
                notification
            )
        } catch (e: SecurityException) {
            // Handle missing notification permission
        }
    }

    /**
     * Send level up notification
     */
    private fun sendLevelUpNotification(newLevel: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "profile")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            newLevel,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎉 Level Up!")
            .setContentText("Congratulations! You've reached Level $newLevel!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BASE + 1000 + newLevel,
                notification
            )
        } catch (e: SecurityException) {
            // Handle missing notification permission
        }
    }

    /**
     * Send streak notification
     */
    fun sendStreakNotification(streakDays: Int) {
        if (streakDays <= 0) return

        val milestones = listOf(7, 14, 30, 60, 100, 365)
        if (streakDays !in milestones) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔥 Streak Milestone!")
            .setContentText("Amazing! You've maintained a $streakDays-day streak!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BASE + 2000 + streakDays,
                notification
            )
        } catch (e: SecurityException) {
            // Handle missing notification permission
        }
    }

    /**
     * User statistics for achievement checking
     */
    private data class UserStats(
        val totalDistance: Double,
        val totalWorkouts: Int,
        val totalCalories: Int,
        val currentStreak: Int,
        val longestRun: Double,
        val fastestPace: Float,
        val hasEarlyMorningWorkout: Boolean,
        val hasRainyWorkout: Boolean,
        val challengesCompleted: Int,
        val plansCompleted: Int
    )
}
