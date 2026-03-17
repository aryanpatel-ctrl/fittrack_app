package com.fittrackpro.data.local.database.dao

import androidx.room.*
import com.fittrackpro.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {
    // Food Item operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(foodItems: List<FoodItem>)

    @Query("SELECT * FROM food_items WHERE id = :itemId")
    suspend fun getFoodItemById(itemId: String): FoodItem?

    @Query("SELECT * FROM food_items WHERE barcode = :barcode")
    suspend fun getFoodItemByBarcode(barcode: String): FoodItem?

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' LIMIT :limit")
    suspend fun searchFoodItems(query: String, limit: Int = 20): List<FoodItem>

    @Query("SELECT * FROM food_items WHERE isCustom = 1 AND createdBy = :userId")
    fun getCustomFoodItems(userId: String): Flow<List<FoodItem>>

    // Nutrition Log operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionLog(log: NutritionLog)

    @Update
    suspend fun updateNutritionLog(log: NutritionLog)

    @Delete
    suspend fun deleteNutritionLog(log: NutritionLog)

    @Query("SELECT * FROM nutrition_logs WHERE id = :logId")
    suspend fun getNutritionLogById(logId: String): NutritionLog?

    @Query("SELECT * FROM nutrition_logs WHERE userId = :userId AND date = :date ORDER BY mealType")
    fun getNutritionLogsByDate(userId: String, date: Long): Flow<List<NutritionLog>>

    @Query("SELECT * FROM nutrition_logs WHERE userId = :userId AND date = :date AND mealType = :mealType")
    fun getNutritionLogsByMeal(userId: String, date: Long, mealType: String): Flow<List<NutritionLog>>

    @Query("SELECT SUM(calories) FROM nutrition_logs WHERE userId = :userId AND date = :date")
    suspend fun getTotalCaloriesForDate(userId: String, date: Long): Int?

    @Query("""
        SELECT SUM(calories) as calories, SUM(protein) as protein, SUM(carbs) as carbs, SUM(fat) as fat
        FROM nutrition_logs
        WHERE userId = :userId AND date = :date
    """)
    suspend fun getDailyMacros(userId: String, date: Long): DailyMacros?

    // Custom Meals
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomMeal(meal: CustomMeal)

    @Update
    suspend fun updateCustomMeal(meal: CustomMeal)

    @Delete
    suspend fun deleteCustomMeal(meal: CustomMeal)

    @Query("SELECT * FROM custom_meals WHERE userId = :userId")
    fun getCustomMeals(userId: String): Flow<List<CustomMeal>>

    @Query("SELECT * FROM custom_meals WHERE id = :mealId")
    suspend fun getCustomMealById(mealId: String): CustomMeal?

    // Hydration
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHydrationLog(log: HydrationLog)

    @Query("SELECT * FROM hydration_logs WHERE userId = :userId AND date = :date ORDER BY time DESC")
    fun getHydrationLogsByDate(userId: String, date: Long): Flow<List<HydrationLog>>

    @Query("SELECT SUM(amountMl) FROM hydration_logs WHERE userId = :userId AND date = :date")
    suspend fun getTotalHydrationForDate(userId: String, date: Long): Int?

    // Daily Summary
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: DailyNutritionSummary)

    @Update
    suspend fun updateDailySummary(summary: DailyNutritionSummary)

    @Query("SELECT * FROM daily_nutrition_summary WHERE userId = :userId AND date = :date")
    suspend fun getDailySummary(userId: String, date: Long): DailyNutritionSummary?

    @Query("SELECT * FROM daily_nutrition_summary WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getDailySummariesByRange(userId: String, startDate: Long, endDate: Long): Flow<List<DailyNutritionSummary>>
}

data class DailyMacros(
    val calories: Int?,
    val protein: Float?,
    val carbs: Float?,
    val fat: Float?
)
