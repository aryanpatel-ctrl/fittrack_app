package com.fittrackpro.ui.nutrition

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.NutritionDao
import com.fittrackpro.data.local.database.entity.FoodItem
import com.fittrackpro.data.local.database.entity.NutritionLog
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.data.remote.api.NutritionixApi
import com.fittrackpro.data.remote.api.UsdaNutrient
import com.fittrackpro.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddMealViewModel @Inject constructor(
    private val nutritionDao: NutritionDao,
    private val nutritionixApi: NutritionixApi,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<FoodItem>>()
    val searchResults: LiveData<List<FoodItem>> = _searchResults

    private val _selectedFood = MutableLiveData<FoodItem?>()
    val selectedFood: LiveData<FoodItem?> = _selectedFood

    private val _mealAdded = MutableLiveData<Boolean>()
    val mealAdded: LiveData<Boolean> = _mealAdded

    private var currentMealType = "breakfast"

    fun setMealType(mealType: String) {
        currentMealType = mealType
    }

    fun searchFood(query: String) {
        viewModelScope.launch {
            try {
                // Search local database first
                val localResults = nutritionDao.searchFoodItems(query, 20)
                if (localResults.isNotEmpty()) {
                    _searchResults.value = localResults
                } else {
                    // Search USDA API
                    try {
                        val response = nutritionixApi.searchFood(query, Constants.USDA_API_KEY)
                        val foodItems = response.foods?.map { usdaFood ->
                            val nutrients = usdaFood.foodNutrients ?: emptyList()
                            FoodItem(
                                id = UUID.randomUUID().toString(),
                                name = usdaFood.description ?: "Unknown",
                                brand = usdaFood.brandName,
                                calories = nutrients.find { it.nutrientId == UsdaNutrient.ENERGY }?.value?.toInt() ?: 0,
                                protein = nutrients.find { it.nutrientId == UsdaNutrient.PROTEIN }?.value?.toFloat() ?: 0f,
                                carbs = nutrients.find { it.nutrientId == UsdaNutrient.CARBS }?.value?.toFloat() ?: 0f,
                                fat = nutrients.find { it.nutrientId == UsdaNutrient.FAT }?.value?.toFloat() ?: 0f,
                                servingSize = usdaFood.servingSize?.toFloat() ?: 100f,
                                servingUnit = usdaFood.servingSizeUnit ?: "g"
                            )
                        } ?: emptyList()
                        _searchResults.value = foodItems
                    } catch (e: Exception) {
                        _searchResults.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }

    fun selectFood(food: FoodItem) {
        _selectedFood.value = food
    }

    private fun getDateTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun addMealEntry(name: String, calories: Int, protein: Float, carbs: Float, fat: Float, quantity: Float, mealType: String) {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val foodItemId = UUID.randomUUID().toString()
            val foodItem = FoodItem(
                id = foodItemId, name = name, calories = calories,
                protein = protein, carbs = carbs, fat = fat,
                servingSize = 100f, servingUnit = "g",
                isCustom = true, createdBy = userId
            )
            nutritionDao.insertFoodItem(foodItem)

            val log = NutritionLog(
                id = UUID.randomUUID().toString(), userId = userId,
                foodItemId = foodItemId, date = getDateTimestamp(),
                mealType = mealType, quantity = quantity,
                calories = (calories * quantity).toInt(),
                protein = protein * quantity, carbs = carbs * quantity, fat = fat * quantity
            )
            nutritionDao.insertNutritionLog(log)
            _mealAdded.value = true
        }
    }
}
