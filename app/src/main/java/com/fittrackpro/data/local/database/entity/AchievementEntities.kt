package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Achievement definitions
 */
@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val category: String, // distance, activities, streaks, challenges, social
    val criteriaType: String, // total_distance, total_activities, streak_days, etc.
    val criteriaValue: Double, // The target value to unlock
    val badgeIcon: String, // drawable resource name or URL
    val badgeColor: String = "#4CAF50", // hex color
    val xpReward: Int = 100,
    val tier: String = "bronze", // bronze, silver, gold, platinum
    val isHidden: Boolean = false, // Hidden until unlocked
    val sortOrder: Int = 0
)

/**
 * User achievements - earned badges
 */
@Entity(
    tableName = "user_achievements",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Achievement::class,
            parentColumns = ["id"],
            childColumns = ["achievementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("achievementId"),
        Index(value = ["userId", "achievementId"], unique = true)
    ]
)
data class UserAchievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val achievementId: String,
    val progress: Double = 0.0, // Current progress towards the achievement
    val isUnlocked: Boolean = false,
    val earnedAt: Long? = null,
    val notified: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Streak tracking
 */
@Entity(
    tableName = "streaks",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index(value = ["userId", "streakType"], unique = true)]
)
data class Streak(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val streakType: String, // daily_activity, weekly_goal, nutrition_logging
    val currentCount: Int = 0,
    val bestCount: Int = 0,
    val lastActivityDate: Long? = null,
    val startDate: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * XP transaction log
 */
@Entity(
    tableName = "xp_transactions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("earnedAt")]
)
data class XpTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val amount: Int,
    val source: String, // activity, achievement, challenge, streak
    val sourceId: String? = null, // ID of the activity/achievement/etc
    val description: String? = null,
    val earnedAt: Long = System.currentTimeMillis()
)
