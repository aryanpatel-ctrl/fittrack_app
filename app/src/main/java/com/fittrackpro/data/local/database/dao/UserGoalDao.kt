package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.UserGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: UserGoal)

    @Update
    suspend fun updateGoal(goal: UserGoal)

    @Delete
    suspend fun deleteGoal(goal: UserGoal)

    @Query("SELECT * FROM user_goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: String): UserGoal?

    @Query("SELECT * FROM user_goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGoalsByUserId(userId: String): Flow<List<UserGoal>>

    @Query("SELECT * FROM user_goals WHERE userId = :userId AND status = 'active' ORDER BY deadline ASC")
    fun getActiveGoals(userId: String): Flow<List<UserGoal>>

    @Query("SELECT * FROM user_goals WHERE userId = :userId AND status = 'completed' ORDER BY completedAt DESC")
    fun getCompletedGoals(userId: String): Flow<List<UserGoal>>

    @Query("SELECT * FROM user_goals WHERE userId = :userId AND goalType = :goalType AND status = 'active' LIMIT 1")
    suspend fun getActiveGoalByType(userId: String, goalType: String): UserGoal?

    @Query("UPDATE user_goals SET currentValue = :currentValue WHERE id = :goalId")
    suspend fun updateGoalProgress(goalId: String, currentValue: Float)

    @Query("UPDATE user_goals SET status = 'completed', completedAt = :completedAt WHERE id = :goalId")
    suspend fun markGoalCompleted(goalId: String, completedAt: Long)
}
