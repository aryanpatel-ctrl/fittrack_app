package com.fittrackpro.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Open Food Facts API interface for barcode-based food lookup
 * Base URL: https://world.openfoodfacts.org/
 * Free API - no API key required
 */
interface OpenFoodFactsApi {

    /**
     * Get product information by barcode
     */
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): OpenFoodFactsResponse
}

// Open Food Facts Response models

data class OpenFoodFactsResponse(
    val status: Int,
    @SerializedName("status_verbose")
    val statusVerbose: String?,
    val product: OpenFoodFactsProduct?
) {
    val isFound: Boolean get() = status == 1 && product != null
}

data class OpenFoodFactsProduct(
    @SerializedName("product_name")
    val productName: String?,

    val brands: String?,

    @SerializedName("serving_size")
    val servingSize: String?,

    @SerializedName("serving_quantity")
    val servingQuantity: Double?,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("image_small_url")
    val imageSmallUrl: String?,

    val nutriments: OpenFoodFactsNutriments?,

    val categories: String?,

    @SerializedName("categories_tags")
    val categoriesTags: List<String>?
)

data class OpenFoodFactsNutriments(
    // Per 100g values
    @SerializedName("energy-kcal_100g")
    val caloriesPer100g: Float?,

    @SerializedName("proteins_100g")
    val proteinPer100g: Float?,

    @SerializedName("carbohydrates_100g")
    val carbsPer100g: Float?,

    @SerializedName("fat_100g")
    val fatPer100g: Float?,

    @SerializedName("fiber_100g")
    val fiberPer100g: Float?,

    @SerializedName("sugars_100g")
    val sugarsPer100g: Float?,

    @SerializedName("sodium_100g")
    val sodiumPer100g: Float?,

    // Per serving values (if available)
    @SerializedName("energy-kcal_serving")
    val caloriesPerServing: Float?,

    @SerializedName("proteins_serving")
    val proteinPerServing: Float?,

    @SerializedName("carbohydrates_serving")
    val carbsPerServing: Float?,

    @SerializedName("fat_serving")
    val fatPerServing: Float?
)
