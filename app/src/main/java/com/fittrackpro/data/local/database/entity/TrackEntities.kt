package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Track entity - stores GPS activity recordings
 */
@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("startTime")]
)
data class Track(
    @PrimaryKey
    val id: String,
    val userId: String,
    val activityType: String, // running, cycling, walking, hiking, swimming
    val name: String? = null,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val status: String = "in_progress", // in_progress, paused, completed
    val isOutdoor: Boolean = true,
    val weatherCondition: String? = null,
    val temperature: Float? = null,
    val syncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Track point entity - individual GPS coordinates
 */
@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId"), Index("timestamp")]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val speed: Float? = null, // m/s
    val accuracy: Float? = null,
    val heartRate: Int? = null,
    val timestamp: Long
)

/**
 * Track statistics - calculated metrics per track
 */
@Entity(
    tableName = "track_statistics",
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("trackId")]
)
data class TrackStatistics(
    @PrimaryKey
    val trackId: String,
    val distance: Double = 0.0, // meters
    val duration: Long = 0L, // milliseconds (active time)
    val totalTime: Long = 0L, // milliseconds (including pauses)
    val avgSpeed: Float = 0f, // m/s
    val maxSpeed: Float = 0f, // m/s
    val avgPace: Float = 0f, // min/km
    val calories: Int = 0,
    val elevationGain: Double = 0.0, // meters
    val elevationLoss: Double = 0.0, // meters
    val maxAltitude: Double? = null,
    val minAltitude: Double? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val steps: Int? = null
)

/**
 * Personal records
 */
@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("trackId"), Index(value = ["userId", "recordType"], unique = true)]
)
data class PersonalRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val recordType: String, // fastest_1k, fastest_5k, longest_distance, etc.
    val value: Double, // time in ms for speed records, distance in meters for distance records
    val activityType: String,
    val trackId: String? = null,
    val achievedAt: Long = System.currentTimeMillis(),
    val previousValue: Double? = null
)
