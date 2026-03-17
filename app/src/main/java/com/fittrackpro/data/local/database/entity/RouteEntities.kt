package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Routes - saved and shared routes
 */
@Entity(
    tableName = "routes",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["creatorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("creatorId"), Index("isPublic"), Index("difficulty")]
)
data class Route(
    @PrimaryKey
    val id: String,
    val creatorId: String,
    val name: String,
    val description: String? = null,
    val activityType: String, // running, cycling, walking, hiking
    val distance: Double, // meters
    val estimatedDuration: Long? = null, // milliseconds
    val difficulty: String, // easy, moderate, hard, expert
    val elevationGain: Double? = null,
    val surfaceType: String? = null, // paved, trail, mixed
    val polyline: String, // Encoded polyline string
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val city: String? = null,
    val country: String? = null,
    val isPublic: Boolean = false,
    val isFavorite: Boolean = false,
    val timesUsed: Int = 0,
    val avgRating: Float = 0f,
    val ratingCount: Int = 0,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Route waypoints
 */
@Entity(
    tableName = "route_waypoints",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId")]
)
data class RouteWaypoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routeId: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val orderIndex: Int,
    val name: String? = null,
    val description: String? = null,
    val waypointType: String? = null // start, end, water_station, viewpoint, hazard
)

/**
 * Route ratings and reviews
 */
@Entity(
    tableName = "route_ratings",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
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
        Index("routeId"),
        Index("userId"),
        Index(value = ["routeId", "userId"], unique = true)
    ]
)
data class RouteRating(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routeId: String,
    val userId: String,
    val rating: Float, // 1-5
    val review: String? = null,
    val safetyRating: Int? = null, // 1-5
    val sceneryRating: Int? = null, // 1-5
    val surfaceRating: Int? = null, // 1-5
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Route photos
 */
@Entity(
    tableName = "route_photos",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId"), Index("userId")]
)
data class RoutePhoto(
    @PrimaryKey
    val id: String,
    val routeId: String,
    val userId: String,
    val imageUrl: String,
    val caption: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Route categories/tags
 */
@Entity(tableName = "route_categories")
data class RouteCategory(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconName: String,
    val color: String
)
