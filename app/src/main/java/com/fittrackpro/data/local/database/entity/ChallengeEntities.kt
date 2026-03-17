package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Challenge entity
 */
@Entity(
    tableName = "challenges",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("creatorId"), Index("startDate"), Index("endDate")]
)
data class Challenge(
    @PrimaryKey
    val id: String,
    val creatorId: String,
    val name: String,
    val description: String? = null,
    val type: String, // distance, duration, calories, activities
    val activityType: String? = null, // null = any activity
    val goalValue: Double,
    val goalUnit: String, // km, minutes, kcal, count
    val startDate: Long,
    val endDate: Long,
    val visibility: String = "public", // public, private, invite_only
    val maxParticipants: Int? = null,
    val imageUrl: String? = null,
    val isTeamChallenge: Boolean = false,
    val status: String = "active", // upcoming, active, completed, cancelled
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Challenge participants
 */
@Entity(
    tableName = "challenge_participants",
    foreignKeys = [
        ForeignKey(
            entity = Challenge::class,
            parentColumns = ["id"],
            childColumns = ["challengeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("challengeId"),
        Index("userId"),
        Index(value = ["challengeId", "userId"], unique = true)
    ]
)
data class ChallengeParticipant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val challengeId: String,
    val userId: String,
    val teamId: String? = null,
    val progress: Double = 0.0,
    val rank: Int? = null,
    val joinedAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Challenge leaderboard (for faster queries)
 */
@Entity(
    tableName = "challenge_leaderboard",
    foreignKeys = [
        ForeignKey(
            entity = Challenge::class,
            parentColumns = ["id"],
            childColumns = ["challengeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("challengeId"), Index("userId"), Index("rank")]
)
data class ChallengeLeaderboard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val challengeId: String,
    val userId: String,
    val userName: String,
    val userImage: String? = null,
    val score: Double,
    val rank: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Challenge messages/comments
 */
@Entity(
    tableName = "challenge_messages",
    foreignKeys = [
        ForeignKey(
            entity = Challenge::class,
            parentColumns = ["id"],
            childColumns = ["challengeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("challengeId"), Index("userId"), Index("timestamp")]
)
data class ChallengeMessage(
    @PrimaryKey
    val id: String,
    val challengeId: String,
    val userId: String,
    val userName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Team challenges
 */
@Entity(
    tableName = "team_challenges",
    foreignKeys = [
        ForeignKey(
            entity = Challenge::class,
            parentColumns = ["id"],
            childColumns = ["challengeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("challengeId")]
)
data class TeamChallenge(
    @PrimaryKey
    val id: String,
    val challengeId: String,
    val teamName: String,
    val teamImage: String? = null,
    val captainId: String,
    val totalProgress: Double = 0.0,
    val memberCount: Int = 0,
    val rank: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
