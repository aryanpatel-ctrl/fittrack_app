package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Training plan templates
 */
@Entity(tableName = "training_plans")
data class TrainingPlan(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String? = null,
    val goalType: String, // 5k, 10k, half_marathon, marathon, weight_loss, endurance
    val durationWeeks: Int,
    val difficulty: String, // beginner, intermediate, advanced
    val workoutsPerWeek: Int,
    val isCustom: Boolean = false,
    val creatorId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Workout templates within a training plan
 */
@Entity(
    tableName = "workout_templates",
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId")]
)
data class WorkoutTemplate(
    @PrimaryKey
    val id: String,
    val planId: String,
    val weekNumber: Int,
    val dayNumber: Int, // 1-7, Monday=1
    val name: String,
    val type: String, // easy_run, tempo_run, interval, long_run, rest, cross_training
    val description: String? = null,
    val instructions: String? = null,
    val targetDistance: Float? = null, // meters
    val targetDuration: Long? = null, // milliseconds
    val targetPace: Float? = null, // min/km
    val warmupDuration: Long? = null,
    val cooldownDuration: Long? = null,
    val intervals: String? = null // JSON string for interval workouts
)

/**
 * User fitness goals
 */
@Entity(
    tableName = "user_goals",
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
data class UserGoal(
    @PrimaryKey
    val id: String,
    val userId: String,
    val goalType: String, // race, distance, duration, weight_loss, frequency
    val targetValue: Float,
    val currentValue: Float = 0f,
    val unit: String, // km, minutes, kg, activities
    val deadline: Long? = null,
    val status: String = "active", // active, completed, abandoned
    val raceDate: Long? = null,
    val raceName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Scheduled workouts for a user
 */
@Entity(
    tableName = "scheduled_workouts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutTemplate::class,
            parentColumns = ["id"],
            childColumns = ["workoutTemplateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["completedTrackId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("workoutTemplateId"), Index("scheduledDate"), Index("completedTrackId")]
)
data class ScheduledWorkout(
    @PrimaryKey
    val id: String,
    val userId: String,
    val workoutTemplateId: String,
    val scheduledDate: Long,
    val status: String = "pending", // pending, completed, skipped
    val completedTrackId: String? = null,
    val completedAt: Long? = null,
    val notes: String? = null,
    val coachFeedback: String? = null
)

/**
 * User's progress on a training plan
 */
@Entity(
    tableName = "plan_progress",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrainingPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("planId"), Index(value = ["userId", "planId"], unique = true)]
)
data class PlanProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val planId: String,
    val startDate: Long,
    val currentWeek: Int = 1,
    val completedWorkouts: Int = 0,
    val totalWorkouts: Int,
    val completionPercentage: Float = 0f,
    val status: String = "active", // active, completed, paused, abandoned
    val updatedAt: Long = System.currentTimeMillis()
)
