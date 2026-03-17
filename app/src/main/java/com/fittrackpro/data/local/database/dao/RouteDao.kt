package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    // Route operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: Route)

    @Update
    suspend fun updateRoute(route: Route)

    @Delete
    suspend fun deleteRoute(route: Route)

    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: String): Route?

    @Query("SELECT * FROM routes WHERE id = :routeId")
    fun getRouteByIdFlow(routeId: String): Flow<Route?>

    @Query("SELECT * FROM routes WHERE creatorId = :userId ORDER BY createdAt DESC")
    fun getRoutesByUser(userId: String): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE creatorId = :userId AND isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteRoutes(userId: String): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE isPublic = 1 ORDER BY avgRating DESC, timesUsed DESC LIMIT :limit")
    fun getPopularPublicRoutes(limit: Int): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE isPublic = 1 AND activityType = :activityType ORDER BY avgRating DESC")
    fun getPublicRoutesByActivity(activityType: String): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE isPublic = 1 AND difficulty = :difficulty ORDER BY avgRating DESC")
    fun getPublicRoutesByDifficulty(difficulty: String): Flow<List<Route>>

    @Query("""
        SELECT * FROM routes
        WHERE isPublic = 1
        AND startLat BETWEEN :minLat AND :maxLat
        AND startLng BETWEEN :minLng AND :maxLng
        ORDER BY avgRating DESC
    """)
    fun getRoutesNearLocation(minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Flow<List<Route>>

    @Query("UPDATE routes SET isFavorite = :isFavorite, updatedAt = :timestamp WHERE id = :routeId")
    suspend fun updateFavoriteStatus(routeId: String, isFavorite: Boolean, timestamp: Long)

    @Query("UPDATE routes SET timesUsed = timesUsed + 1, updatedAt = :timestamp WHERE id = :routeId")
    suspend fun incrementTimesUsed(routeId: String, timestamp: Long)

    @Query("UPDATE routes SET avgRating = :avgRating, ratingCount = :ratingCount, updatedAt = :timestamp WHERE id = :routeId")
    suspend fun updateRouteRating(routeId: String, avgRating: Float, ratingCount: Int, timestamp: Long)

    // Route Waypoints
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaypoints(waypoints: List<RouteWaypoint>)

    @Query("SELECT * FROM route_waypoints WHERE routeId = :routeId ORDER BY orderIndex")
    suspend fun getWaypointsByRoute(routeId: String): List<RouteWaypoint>

    @Query("DELETE FROM route_waypoints WHERE routeId = :routeId")
    suspend fun deleteWaypointsByRoute(routeId: String)

    // Route Ratings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: RouteRating)

    @Update
    suspend fun updateRating(rating: RouteRating)

    @Query("SELECT * FROM route_ratings WHERE routeId = :routeId ORDER BY createdAt DESC")
    fun getRatingsByRoute(routeId: String): Flow<List<RouteRating>>

    @Query("SELECT * FROM route_ratings WHERE routeId = :routeId AND userId = :userId")
    suspend fun getUserRating(routeId: String, userId: String): RouteRating?

    @Query("SELECT AVG(rating) FROM route_ratings WHERE routeId = :routeId")
    suspend fun getAverageRating(routeId: String): Float?

    @Query("SELECT COUNT(*) FROM route_ratings WHERE routeId = :routeId")
    suspend fun getRatingCount(routeId: String): Int

    // Route Photos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: RoutePhoto)

    @Delete
    suspend fun deletePhoto(photo: RoutePhoto)

    @Query("SELECT * FROM route_photos WHERE routeId = :routeId ORDER BY createdAt DESC")
    fun getPhotosByRoute(routeId: String): Flow<List<RoutePhoto>>

    // Route Categories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<RouteCategory>)

    @Query("SELECT * FROM route_categories")
    fun getAllCategories(): Flow<List<RouteCategory>>
}
