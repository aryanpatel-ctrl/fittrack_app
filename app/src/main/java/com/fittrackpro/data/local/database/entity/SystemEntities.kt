package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Sync queue for offline-first architecture
 */
@Entity(
    tableName = "sync_queue",
    indices = [Index("tableName"), Index("operation"), Index("createdAt")]
)
data class SyncQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tableName: String,
    val recordId: String,
    val operation: String, // insert, update, delete
    val data: String? = null, // JSON of the record data
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)

/**
 * Device registry for multi-device sync
 */
@Entity(
    tableName = "device_registry",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("deviceId", unique = true)]
)
data class DeviceRegistry(
    @PrimaryKey
    val id: String,
    val userId: String,
    val deviceId: String,
    val deviceName: String,
    val deviceType: String, // phone, tablet, watch
    val osVersion: String? = null,
    val appVersion: String? = null,
    val fcmToken: String? = null,
    val lastSync: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Sync logs for debugging
 */
@Entity(
    tableName = "sync_logs",
    indices = [Index("timestamp"), Index("status")]
)
data class SyncLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncType: String, // full, incremental, push, pull
    val status: String, // started, completed, failed
    val recordsProcessed: Int = 0,
    val recordsFailed: Int = 0,
    val errorMessage: String? = null,
    val duration: Long? = null, // milliseconds
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Weather cache
 */
@Entity(
    tableName = "weather_cache",
    indices = [Index(value = ["latitude", "longitude"], unique = true)]
)
data class WeatherCache(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val data: String, // JSON weather data
    val temperature: Float? = null,
    val condition: String? = null,
    val humidity: Int? = null,
    val windSpeed: Float? = null,
    val fetchedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 1800000 // 30 minutes
)

/**
 * Notifications history
 */
@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("createdAt"), Index("isRead")]
)
data class Notification(
    @PrimaryKey
    val id: String,
    val userId: String,
    val type: String, // achievement, challenge, workout_reminder, coach_message, system
    val title: String,
    val body: String,
    val data: String? = null, // JSON for additional data
    val actionType: String? = null, // navigate_to, open_url
    val actionData: String? = null,
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Analytics cache for performance insights
 */
@Entity(
    tableName = "analytics_cache",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index(value = ["userId", "metricType", "period"], unique = true)]
)
data class AnalyticsCache(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val metricType: String, // distance, duration, pace, calories, activities
    val period: String, // daily, weekly, monthly, yearly
    val periodStart: Long,
    val periodEnd: Long,
    val value: Double,
    val previousValue: Double? = null,
    val changePercentage: Float? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Performance metrics history
 */
@Entity(
    tableName = "performance_metrics",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("date"), Index("metricType")]
)
data class PerformanceMetric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: Long,
    val metricType: String, // fitness_level, fatigue_score, form_score, training_load
    val value: Double,
    val activityType: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
