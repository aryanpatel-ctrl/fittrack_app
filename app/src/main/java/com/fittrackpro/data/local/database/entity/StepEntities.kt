package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity to store daily step count history.
 * Each record represents steps for a single day for a user.
 */
@Entity(
    tableName = "daily_steps",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["date"]),
        Index(value = ["userId", "date"], unique = true)
    ]
)
data class DailySteps(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,

    /** Date in format "yyyy-MM-dd" */
    val date: String,

    /** Total steps for the day */
    val stepCount: Int,

    /** User's step goal for that day */
    val goal: Int = 10000,

    /** Whether the user achieved their goal */
    val goalAchieved: Boolean = false,

    /** Distance walked in meters (estimated from steps) */
    val distanceMeters: Double = 0.0,

    /** Calories burned (estimated from steps) */
    val caloriesBurned: Int = 0,

    /** Timestamp when the record was created */
    val createdAt: Long = System.currentTimeMillis(),

    /** Timestamp when the record was last updated */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Summary of step statistics over a period
 */
data class StepSummary(
    val totalSteps: Int,
    val averageSteps: Int,
    val totalDistance: Double,
    val totalCalories: Int,
    val daysTracked: Int,
    val goalsAchieved: Int
)

/**
 * Weekly step data for chart display
 */
data class WeeklyStepData(
    val date: String,
    val stepCount: Int,
    val goal: Int
)
