package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.User
import com.fittrackpro.data.local.database.entity.UserSettings
import com.fittrackpro.data.local.database.entity.UserStats
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // User operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    // User Settings operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: UserSettings)

    @Update
    suspend fun updateSettings(settings: UserSettings)

    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    suspend fun getSettingsByUserId(userId: String): UserSettings?

    @Query("SELECT * FROM user_settings WHERE userId = :userId")
    fun getSettingsByUserIdFlow(userId: String): Flow<UserSettings?>

    // User Stats operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: UserStats)

    @Update
    suspend fun updateStats(stats: UserStats)

    @Query("SELECT * FROM user_stats WHERE userId = :userId")
    suspend fun getStatsByUserId(userId: String): UserStats?

    @Query("SELECT * FROM user_stats WHERE userId = :userId")
    fun getStatsByUserIdFlow(userId: String): Flow<UserStats?>

    @Query("""
        UPDATE user_stats
        SET totalDistance = totalDistance + :distance,
            totalDuration = totalDuration + :duration,
            totalActivities = totalActivities + 1,
            totalCalories = totalCalories + :calories,
            totalElevationGain = totalElevationGain + :elevation,
            updatedAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun updateStatsAfterActivity(
        userId: String,
        distance: Double,
        duration: Long,
        calories: Int,
        elevation: Double,
        timestamp: Long
    )

    @Query("""
        UPDATE user_stats
        SET totalXp = totalXp + :xp,
            level = :level,
            updatedAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun updateXpAndLevel(userId: String, xp: Int, level: Int, timestamp: Long)
}
