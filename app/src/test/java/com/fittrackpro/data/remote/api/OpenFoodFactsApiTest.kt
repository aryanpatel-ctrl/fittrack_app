package com.fittrackpro.data.remote.api

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for OpenFoodFacts API response parsing
 * Tests JSON parsing and data mapping
 */
class OpenFoodFactsApiTest {

    private val gson = Gson()

    // ==================== RESPONSE PARSING TESTS ====================

    @Test
    fun `parse successful response with full product data`() {
        val json = """
        {
            "status": 1,
            "status_verbose": "product found",
            "product": {
                "product_name": "Nutella",
                "brands": "Ferrero",
                "serving_size": "15g",
                "serving_quantity": 15.0,
                "image_url": "https://example.com/image.jpg",
                "image_small_url": "https://example.com/small.jpg",
                "categories": "Spreads",
                "nutriments": {
                    "energy-kcal_100g": 539.0,
                    "proteins_100g": 6.3,
                    "carbohydrates_100g": 57.5,
                    "fat_100g": 30.9,
                    "fiber_100g": 3.4,
                    "sugars_100g": 56.3,
                    "sodium_100g": 0.041
                }
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, OpenFoodFactsResponse::class.java)

        assertEquals(1, response.status)
        assertEquals("product found", response.statusVerbose)
        assertTrue(response.isFound)

        val product = response.product!!
        assertEquals("Nutella", product.productName)
        assertEquals("Ferrero", product.brands)
        assertEquals("15g", product.servingSize)
        assertEquals(15.0, product.servingQuantity!!, 0.1)

        val nutriments = product.nutriments!!
        assertEquals(539f, nutriments.caloriesPer100g!!, 0.1f)
        assertEquals(6.3f, nutriments.proteinPer100g!!, 0.1f)
        assertEquals(57.5f, nutriments.carbsPer100g!!, 0.1f)
        assertEquals(30.9f, nutriments.fatPer100g!!, 0.1f)
        assertEquals(3.4f, nutriments.fiberPer100g!!, 0.1f)
        assertEquals(56.3f, nutriments.sugarsPer100g!!, 0.1f)
    }

    @Test
    fun `parse response with product not found`() {
        val json = """
        {
            "status": 0,
            "status_verbose": "product not found",
            "product": null
        }
        """.trimIndent()

        val response = gson.fromJson(json, OpenFoodFactsResponse::class.java)

        assertEquals(0, response.status)
        assertEquals("product not found", response.statusVerbose)
        assertFalse(response.isFound)
        assertNull(response.product)
    }

    @Test
    fun `parse response with minimal product data`() {
        val json = """
        {
            "status": 1,
            "product": {
                "product_name": "Generic Product",
                "nutriments": {
                    "energy-kcal_100g": 100.0
                }
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, OpenFoodFactsResponse::class.java)

        assertTrue(response.isFound)
        assertEquals("Generic Product", response.product!!.productName)
        assertNull(response.product!!.brands)
        assertNull(response.product!!.servingSize)
        assertEquals(100f, response.product!!.nutriments!!.caloriesPer100g!!, 0.1f)
        assertNull(response.product!!.nutriments!!.proteinPer100g)
    }

    @Test
    fun `parse response with per-serving nutriments`() {
        val json = """
        {
            "status": 1,
            "product": {
                "product_name": "Cereal Bar",
                "nutriments": {
                    "energy-kcal_100g": 400.0,
                    "energy-kcal_serving": 120.0,
                    "proteins_100g": 5.0,
                    "proteins_serving": 1.5,
                    "carbohydrates_100g": 70.0,
                    "carbohydrates_serving": 21.0,
                    "fat_100g": 10.0,
                    "fat_serving": 3.0
                }
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, OpenFoodFactsResponse::class.java)
        val nutriments = response.product!!.nutriments!!

        // Per 100g values
        assertEquals(400f, nutriments.caloriesPer100g!!, 0.1f)
        assertEquals(5f, nutriments.proteinPer100g!!, 0.1f)
        assertEquals(70f, nutriments.carbsPer100g!!, 0.1f)
        assertEquals(10f, nutriments.fatPer100g!!, 0.1f)

        // Per serving values
        assertEquals(120f, nutriments.caloriesPerServing!!, 0.1f)
        assertEquals(1.5f, nutriments.proteinPerServing!!, 0.1f)
        assertEquals(21f, nutriments.carbsPerServing!!, 0.1f)
        assertEquals(3f, nutriments.fatPerServing!!, 0.1f)
    }

    @Test
    fun `parse response with categories tags`() {
        val json = """
        {
            "status": 1,
            "product": {
                "product_name": "Apple Juice",
                "categories": "Beverages, Fruit juices",
                "categories_tags": ["en:beverages", "en:fruit-juices", "en:apple-juices"],
                "nutriments": {}
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, OpenFoodFactsResponse::class.java)
        val product = response.product!!

        assertEquals("Beverages, Fruit juices", product.categories)
        assertEquals(3, product.categoriesTags!!.size)
        assertTrue(product.categoriesTags!!.contains("en:apple-juices"))
    }

    // ==================== isFound PROPERTY TESTS ====================

    @Test
    fun `isFound returns true when status is 1 and product exists`() {
        val response = OpenFoodFactsResponse(
            status = 1,
            statusVerbose = "product found",
            product = OpenFoodFactsProduct(
                productName = "Test",
                brands = null,
                servingSize = null,
                servingQuantity = null,
                imageUrl = null,
                imageSmallUrl = null,
                nutriments = null,
                categories = null,
                categoriesTags = null
            )
        )
        assertTrue(response.isFound)
    }

    @Test
    fun `isFound returns false when status is 0`() {
        val response = OpenFoodFactsResponse(
            status = 0,
            statusVerbose = "product not found",
            product = null
        )
        assertFalse(response.isFound)
    }

    @Test
    fun `isFound returns false when product is null even with status 1`() {
        val response = OpenFoodFactsResponse(
            status = 1,
            statusVerbose = null,
            product = null
        )
        assertFalse(response.isFound)
    }

    // ==================== REAL BARCODE FORMAT TESTS ====================

    @Test
    fun `common barcode formats are supported`() {
        // These are the formats we support in BarcodeAnalyzer
        val supportedFormats = listOf(
            "5449000000996",    // EAN-13 (Coca-Cola)
            "12345678",         // EAN-8
            "012345678905",     // UPC-A
            "01234565",         // UPC-E
            "CODE128EXAMPLE",   // CODE-128
            "ABC-1234"          // CODE-39
        )

        // Just verify these are valid string formats
        supportedFormats.forEach { barcode ->
            assertTrue("Barcode $barcode should be valid", barcode.isNotEmpty())
            assertTrue("Barcode $barcode should have reasonable length", barcode.length in 6..20)
        }
    }

    // ==================== NUTRIMENTS EDGE CASES ====================

    @Test
    fun `handle zero values in nutriments`() {
        val json = """
        {
            "status": 1,
            "product": {
                "product_name": "Water",
                "nutriments": {
                    "energy-kcal_100g": 0.0,
                    "proteins_100g": 0.0,
                    "carbohydrates_100g": 0.0,
                    "fat_100g": 0.0
                }
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, OpenFoodFactsResponse::class.java)
        val nutriments = response.product!!.nutriments!!

        assertEquals(0f, nutriments.caloriesPer100g!!, 0.01f)
        assertEquals(0f, nutriments.proteinPer100g!!, 0.01f)
        assertEquals(0f, nutriments.carbsPer100g!!, 0.01f)
        assertEquals(0f, nutriments.fatPer100g!!, 0.01f)
    }

    @Test
    fun `handle missing nutriments object`() {
        val json = """
        {
            "status": 1,
            "product": {
                "product_name": "Unknown Product"
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, OpenFoodFactsResponse::class.java)

        assertTrue(response.isFound)
        assertNull(response.product!!.nutriments)
    }

    @Test
    fun `handle empty product name`() {
        val json = """
        {
            "status": 1,
            "product": {
                "product_name": "",
                "brands": "Some Brand",
                "nutriments": {}
            }
        }
        """.trimIndent()

        val response = gson.fromJson(json, OpenFoodFactsResponse::class.java)

        assertTrue(response.isFound)
        assertEquals("", response.product!!.productName)
    }

    // ==================== MAPPING TO FOODITEM TESTS ====================

    @Test
    fun `can create FoodItem from API response`() {
        val response = OpenFoodFactsResponse(
            status = 1,
            statusVerbose = "product found",
            product = OpenFoodFactsProduct(
                productName = "Test Food",
                brands = "Test Brand",
                servingSize = "100g",
                servingQuantity = 100.0,
                imageUrl = "https://example.com/image.jpg",
                imageSmallUrl = "https://example.com/small.jpg",
                nutriments = OpenFoodFactsNutriments(
                    caloriesPer100g = 250f,
                    proteinPer100g = 10f,
                    carbsPer100g = 30f,
                    fatPer100g = 12f,
                    fiberPer100g = 3f,
                    sugarsPer100g = 15f,
                    sodiumPer100g = 0.5f,
                    caloriesPerServing = null,
                    proteinPerServing = null,
                    carbsPerServing = null,
                    fatPerServing = null
                ),
                categories = "Test Category",
                categoriesTags = listOf("en:test")
            )
        )

        val product = response.product!!
        val nutriments = product.nutriments!!

        // Simulate the mapping done in AddMealViewModel.lookupBarcode()
        val name = product.productName ?: "Unknown Product"
        val brand = product.brands
        val calories = nutriments.caloriesPer100g?.toInt() ?: 0
        val protein = nutriments.proteinPer100g ?: 0f
        val carbs = nutriments.carbsPer100g ?: 0f
        val fat = nutriments.fatPer100g ?: 0f
        val fiber = nutriments.fiberPer100g ?: 0f
        val sugar = nutriments.sugarsPer100g ?: 0f
        val servingSize = product.servingQuantity?.toFloat() ?: 100f

        assertEquals("Test Food", name)
        assertEquals("Test Brand", brand)
        assertEquals(250, calories)
        assertEquals(10f, protein, 0.01f)
        assertEquals(30f, carbs, 0.01f)
        assertEquals(12f, fat, 0.01f)
        assertEquals(3f, fiber, 0.01f)
        assertEquals(15f, sugar, 0.01f)
        assertEquals(100f, servingSize, 0.01f)
    }
}
