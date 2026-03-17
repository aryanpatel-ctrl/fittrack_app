package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Coach profiles
 */
@Entity(
    tableName = "coaches",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId", unique = true)]
)
data class Coach(
    @PrimaryKey
    val id: String,
    val userId: String,
    val credentials: String? = null, // Certifications
    val specialty: String? = null, // running, cycling, triathlon, weight_loss
    val bio: String? = null,
    val experienceYears: Int? = null,
    val isVerified: Boolean = false,
    val acceptingClients: Boolean = true,
    val maxClients: Int = 20,
    val currentClients: Int = 0,
    val rating: Float = 0f,
    val reviewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Coach-client relationships
 */
@Entity(
    tableName = "coach_client_relationships",
    foreignKeys = [
        ForeignKey(
            entity = Coach::class,
            parentColumns = ["id"],
            childColumns = ["coachId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("coachId"),
        Index("clientId"),
        Index(value = ["coachId", "clientId"], unique = true)
    ]
)
data class CoachClientRelationship(
    @PrimaryKey
    val id: String,
    val coachId: String,
    val clientId: String,
    val status: String = "pending", // pending, active, paused, ended
    val startDate: Long? = null,
    val endDate: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Coaching feedback on workouts
 */
@Entity(
    tableName = "coaching_feedback",
    foreignKeys = [
        ForeignKey(
            entity = CoachClientRelationship::class,
            parentColumns = ["id"],
            childColumns = ["relationshipId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScheduledWorkout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("relationshipId"), Index("workoutId")]
)
data class CoachingFeedback(
    @PrimaryKey
    val id: String,
    val relationshipId: String,
    val workoutId: String? = null,
    val feedback: String,
    val rating: Int? = null, // 1-5 performance rating
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Coach-client messages
 */
@Entity(
    tableName = "coach_messages",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["fromId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["toId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fromId"), Index("toId"), Index("createdAt")]
)
data class CoachMessage(
    @PrimaryKey
    val id: String,
    val fromId: String,
    val toId: String,
    val message: String,
    val messageType: String = "text", // text, voice, video, file
    val attachmentUrl: String? = null,
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Plan assignments from coach to client
 */
@Entity(
    tableName = "plan_assignments",
    foreignKeys = [
        ForeignKey(
            entity = Coach::class,
            parentColumns = ["id"],
            childColumns = ["coachId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrainingPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("coachId"), Index("clientId"), Index("planId")]
)
data class PlanAssignment(
    @PrimaryKey
    val id: String,
    val coachId: String,
    val clientId: String,
    val planId: String,
    val customizations: String? = null, // JSON for any customizations
    val startDate: Long,
    val status: String = "active", // active, paused, completed
    val notes: String? = null,
    val assignedAt: Long = System.currentTimeMillis()
)
