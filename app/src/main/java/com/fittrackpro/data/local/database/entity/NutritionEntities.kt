package com.fittrackpro.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Food items database
 */
@Entity(
    tableName = "food_items",
    indices = [Index("barcode"), Index("name")]
)
data class FoodItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val brand: String? = null,
    val calories: Int, // per serving
    val protein: Float, // grams
    val carbs: Float, // grams
    val fat: Float, // grams
    val fiber: Float? = null,
    val sugar: Float? = null,
    val sodium: Float? = null,
    val servingSize: Float, // grams
    val servingUnit: String = "g",
    val barcode: String? = null,
    val imageUrl: String? = null,
    val isCustom: Boolean = false,
    val createdBy: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Nutrition logs - user's meal entries
 */
@Entity(
    tableName = "nutrition_logs",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("foodItemId"), Index("date"), Index("mealType")]
)
data class NutritionLog(
    @PrimaryKey
    val id: String,
    val userId: String,
    val foodItemId: String,
    val date: Long, // Date only (midnight timestamp)
    val mealType: String, // breakfast, lunch, dinner, snack
    val quantity: Float, // number of servings
    val calories: Int, // calculated based on quantity
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Custom meals - user-created recipes
 */
@Entity(
    tableName = "custom_meals",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class CustomMeal(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val description: String? = null,
    val ingredients: String, // JSON array of {foodItemId, quantity}
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val servings: Int = 1,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Hydration logs
 */
@Entity(
    tableName = "hydration_logs",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("date")]
)
data class HydrationLog(
    @PrimaryKey
    val id: String,
    val userId: String,
    val date: Long, // Date only (midnight timestamp)
    val amountMl: Int,
    val drinkType: String = "water", // water, sports_drink, coffee, tea, other
    val time: Long, // Actual timestamp
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Daily nutrition summary (cached)
 */
@Entity(
    tableName = "daily_nutrition_summary",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index(value = ["userId", "date"], unique = true)]
)
data class DailyNutritionSummary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: Long,
    val totalCalories: Int = 0,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val caloriesBurned: Int = 0, // From activities
    val calorieBalance: Int = 0, // consumed - burned
    val hydrationMl: Int = 0,
    val mealsLogged: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
