package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * User entity - stores user profile information
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val email: String,
    val name: String,
    val profileImage: String? = null,
    val role: String = "athlete", // athlete, coach, admin
    val dateOfBirth: Long? = null,
    val weight: Float? = null, // in kg
    val height: Float? = null, // in cm
    val gender: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * User settings entity
 */
@Entity(
    tableName = "user_settings",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class UserSettings(
    @PrimaryKey
    val userId: String,
    val useMetricUnits: Boolean = true,
    val enableNotifications: Boolean = true,
    val enableDarkMode: Boolean = false,
    val enableWorkoutReminders: Boolean = true,
    val enableChallengeAlerts: Boolean = true,
    val enableAchievementAlerts: Boolean = true,
    val shareActivities: Boolean = true,
    val showOnLeaderboard: Boolean = true,
    val dailyGoalDistance: Float = 5000f, // meters
    val dailyGoalCalories: Int = 500,
    val weeklyGoalActivities: Int = 5,
    val restDays: String = "0,6", // Sunday=0, Saturday=6
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * User statistics - aggregated stats
 */
@Entity(
    tableName = "user_stats",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class UserStats(
    @PrimaryKey
    val userId: String,
    val totalDistance: Double = 0.0, // meters
    val totalDuration: Long = 0L, // milliseconds
    val totalActivities: Int = 0,
    val totalCalories: Int = 0,
    val totalElevationGain: Double = 0.0, // meters
    val level: Int = 1,
    val totalXp: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
