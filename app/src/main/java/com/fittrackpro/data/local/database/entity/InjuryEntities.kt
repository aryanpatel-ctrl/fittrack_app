package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Body parts reference
 */
@Entity(tableName = "body_parts")
data class BodyPart(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String, // upper_body, lower_body, core
    val iconName: String
)

/**
 * Injury records
 */
@Entity(
    tableName = "injuries",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("injuryDate"), Index("status")]
)
data class Injury(
    @PrimaryKey
    val id: String,
    val userId: String,
    val bodyPart: String,
    val injuryType: String, // strain, sprain, fracture, inflammation, other
    val severity: String, // mild, moderate, severe
    val description: String? = null,
    val injuryDate: Long,
    val recoveryDate: Long? = null,
    val status: String = "active", // active, recovering, recovered
    val doctorNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Pain tracking entries
 */
@Entity(
    tableName = "pain_tracking",
    foreignKeys = [
        ForeignKey(
            entity = Injury::class,
            parentColumns = ["id"],
            childColumns = ["injuryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("injuryId"), Index("date")]
)
data class PainTracking(
    @PrimaryKey
    val id: String,
    val injuryId: String,
    val date: Long,
    val painLevel: Int, // 1-10
    val painType: String? = null, // sharp, dull, throbbing, burning
    val triggers: String? = null, // What caused the pain
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Recovery activities
 */
@Entity(
    tableName = "recovery_activities",
    foreignKeys = [
        ForeignKey(
            entity = Injury::class,
            parentColumns = ["id"],
            childColumns = ["injuryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("injuryId"), Index("date")]
)
data class RecoveryActivity(
    @PrimaryKey
    val id: String,
    val injuryId: String,
    val activityType: String, // physical_therapy, stretching, ice, heat, rest, medication
    val duration: Long? = null, // milliseconds
    val description: String? = null,
    val date: Long,
    val effectiveness: Int? = null, // 1-5 rating
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
