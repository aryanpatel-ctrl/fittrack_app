package com.fittrackpro.data.repository

import com.fittrackpro.data.local.database.dao.NutritionDao
import com.fittrackpro.data.local.database.entity.DailyNutritionSummary
import com.fittrackpro.data.local.database.entity.FoodItem
import com.fittrackpro.data.local.database.entity.HydrationLog
import com.fittrackpro.data.local.database.entity.NutritionLog
import com.fittrackpro.data.remote.api.NutritionixApi
import com.fittrackpro.data.remote.api.UsdaFoodItem
import com.fittrackpro.data.remote.api.UsdaNutrient
import com.fittrackpro.util.Constants
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepository @Inject constructor(
    private val nutritionDao: NutritionDao,
    private val nutritionixApi: NutritionixApi
) {
    fun getNutritionLogs(userId: String, date: Long): Flow<List<NutritionLog>> =
        nutritionDao.getNutritionLogsByDate(userId, date)

    suspend fun getDailySummary(userId: String, date: Long): DailyNutritionSummary? =
        nutritionDao.getDailySummary(userId, date)

    fun getHydrationLogs(userId: String, date: Long): Flow<List<HydrationLog>> =
        nutritionDao.getHydrationLogsByDate(userId, date)

    suspend fun insertNutritionLog(log: NutritionLog) = nutritionDao.insertNutritionLog(log)
    suspend fun insertFoodItem(item: FoodItem) = nutritionDao.insertFoodItem(item)
    suspend fun insertHydrationLog(log: HydrationLog) = nutritionDao.insertHydrationLog(log)

    suspend fun searchFoodItems(query: String, limit: Int = 20): List<FoodItem> =
        nutritionDao.searchFoodItems(query, limit)

    suspend fun searchFoodOnline(query: String): List<FoodItem> {
        return try {
            val response = nutritionixApi.searchFood(query, Constants.USDA_API_KEY)
            response.foods?.map { usdaFood -> mapUsdaToFoodItem(usdaFood) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun mapUsdaToFoodItem(usdaFood: UsdaFoodItem): FoodItem {
        val nutrients = usdaFood.foodNutrients ?: emptyList()
        return FoodItem(
            id = UUID.randomUUID().toString(),
            name = usdaFood.description ?: "Unknown",
            brand = usdaFood.brandName ?: usdaFood.brandOwner,
            calories = getNutrientValue(nutrients, UsdaNutrient.ENERGY)?.toInt() ?: 0,
            protein = getNutrientValue(nutrients, UsdaNutrient.PROTEIN)?.toFloat() ?: 0f,
            carbs = getNutrientValue(nutrients, UsdaNutrient.CARBS)?.toFloat() ?: 0f,
            fat = getNutrientValue(nutrients, UsdaNutrient.FAT)?.toFloat() ?: 0f,
            fiber = getNutrientValue(nutrients, UsdaNutrient.FIBER)?.toFloat(),
            sugar = getNutrientValue(nutrients, UsdaNutrient.SUGAR)?.toFloat(),
            sodium = getNutrientValue(nutrients, UsdaNutrient.SODIUM)?.toFloat(),
            servingSize = usdaFood.servingSize?.toFloat() ?: 100f,
            servingUnit = usdaFood.servingSizeUnit ?: "g"
        )
    }

    private fun getNutrientValue(nutrients: List<UsdaNutrient>, nutrientId: Int): Double? {
        return nutrients.find { it.nutrientId == nutrientId }?.value
    }

    suspend fun deleteNutritionLog(log: NutritionLog) = nutritionDao.deleteNutritionLog(log)
    suspend fun insertDailySummary(summary: DailyNutritionSummary) = nutritionDao.insertDailySummary(summary)
}
