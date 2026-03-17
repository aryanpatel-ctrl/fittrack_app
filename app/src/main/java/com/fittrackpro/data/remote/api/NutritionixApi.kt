package com.fittrackpro.data.remote.api

import retrofit2.http.*

/**
 * USDA FoodData Central API interface for food/nutrition data
 * Base URL: https://api.nal.usda.gov/fdc/v1/
 * Free API - sign up at https://fdc.nal.usda.gov/api-key-signup
 */
interface NutritionixApi {

    /**
     * Search for food items
     */
    @GET("foods/search")
    suspend fun searchFood(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("pageSize") pageSize: Int = 10
    ): UsdaSearchResponse

    /**
     * Get detailed food item by FDC ID
     */
    @GET("food/{fdcId}")
    suspend fun getFoodDetails(
        @Path("fdcId") fdcId: Int,
        @Query("api_key") apiKey: String
    ): UsdaFoodItem
}

// USDA Response models

data class UsdaSearchResponse(
    val totalHits: Int?,
    val currentPage: Int?,
    val foods: List<UsdaFoodItem>?
)

data class UsdaFoodItem(
    val fdcId: Int?,
    val description: String?,
    val brandName: String?,
    val brandOwner: String?,
    val servingSize: Double?,
    val servingSizeUnit: String?,
    val foodNutrients: List<UsdaNutrient>?
)

data class UsdaNutrient(
    val nutrientId: Int?,
    val nutrientName: String?,
    val nutrientNumber: String?,
    val unitName: String?,
    val value: Double?
) {
    companion object {
        const val ENERGY = 1008      // Energy (kcal)
        const val PROTEIN = 1003     // Protein (g)
        const val FAT = 1004         // Total fat (g)
        const val CARBS = 1005       // Carbohydrate (g)
        const val FIBER = 1079       // Fiber (g)
        const val SUGAR = 2000       // Sugars (g)
        const val SODIUM = 1093      // Sodium (mg)
    }
}
