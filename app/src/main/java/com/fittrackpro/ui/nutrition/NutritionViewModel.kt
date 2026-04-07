package com.fittrackpro.ui.nutrition

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.NutritionDao
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.DailyNutritionSummary
import com.fittrackpro.data.local.database.entity.HydrationLog
import com.fittrackpro.data.local.database.entity.NutritionLog
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionDao: NutritionDao,
    private val userDao: UserDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _selectedDate = MutableLiveData<Date>(Date())
    val selectedDate: LiveData<Date> = _selectedDate

    private val _dailySummary = MutableLiveData<DailyNutritionSummary?>()
    val dailySummary: LiveData<DailyNutritionSummary?> = _dailySummary

    private val _mealLogs = MutableLiveData<List<NutritionLog>>()
    val mealLogs: LiveData<List<NutritionLog>> = _mealLogs

    private val _hydration = MutableLiveData<List<HydrationLog>>()
    val hydration: LiveData<List<HydrationLog>> = _hydration

    // Dynamic nutrition goals based on user profile
    private val _calorieGoal = MutableLiveData(2000)
    val calorieGoal: LiveData<Int> = _calorieGoal

    private val _proteinGoal = MutableLiveData(150)
    val proteinGoal: LiveData<Int> = _proteinGoal

    private val _carbsGoal = MutableLiveData(250)
    val carbsGoal: LiveData<Int> = _carbsGoal

    private val _fatGoal = MutableLiveData(65)
    val fatGoal: LiveData<Int> = _fatGoal

    val waterGoal = 2500 // ml - standard recommendation

    init {
        loadDataForDate(Date())
        calculateNutritionGoals()
    }

    /**
     * Calculate nutrition goals based on user's weight, height, and gender
     * Uses Mifflin-St Jeor equation for BMR and moderate activity multiplier
     */
    private fun calculateNutritionGoals() {
        viewModelScope.launch {
            userPreferences.userId?.let { userId ->
                val user = userDao.getUserById(userId)
                user?.let {
                    val weight = it.weight ?: 70f // Default 70kg
                    val height = it.height ?: 170f // Default 170cm
                    val age = calculateAge(it.dateOfBirth) ?: 30 // Default 30 years
                    val isMale = it.gender?.lowercase() != "female"

                    // Mifflin-St Jeor Equation for BMR
                    val bmr = if (isMale) {
                        (10 * weight) + (6.25 * height) - (5 * age) + 5
                    } else {
                        (10 * weight) + (6.25 * height) - (5 * age) - 161
                    }

                    // TDEE with moderate activity level (1.55 multiplier)
                    val tdee = (bmr * 1.55).toInt()

                    // Macronutrient distribution: 30% protein, 40% carbs, 30% fat
                    // Protein: 4 cal/g, Carbs: 4 cal/g, Fat: 9 cal/g
                    val proteinCals = tdee * 0.30
                    val carbsCals = tdee * 0.40
                    val fatCals = tdee * 0.30

                    _calorieGoal.value = tdee
                    _proteinGoal.value = (proteinCals / 4).toInt()
                    _carbsGoal.value = (carbsCals / 4).toInt()
                    _fatGoal.value = (fatCals / 9).toInt()
                }
            }
        }
    }

    private fun calculateAge(dateOfBirth: Long?): Int? {
        if (dateOfBirth == null) return null
        val today = Calendar.getInstance()
        val birthDate = Calendar.getInstance().apply { timeInMillis = dateOfBirth }
        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    fun previousDay() {
        val calendar = Calendar.getInstance()
        calendar.time = _selectedDate.value ?: Date()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        _selectedDate.value = calendar.time
        loadDataForDate(calendar.time)
    }

    fun nextDay() {
        val calendar = Calendar.getInstance()
        calendar.time = _selectedDate.value ?: Date()
        calendar.add(Calendar.DAY_OF_MONTH, 1)

        // Don't go past today
        if (calendar.time.before(Date()) || isSameDay(calendar.time, Date())) {
            _selectedDate.value = calendar.time
            loadDataForDate(calendar.time)
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(date1) == fmt.format(date2)
    }

    private fun getDateTimestamp(date: Date): Long {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun loadDataForDate(date: Date) {
        viewModelScope.launch {
            userPreferences.userId?.let { userId ->
                val dateTimestamp = getDateTimestamp(date)

                // Load daily summary
                _dailySummary.value = nutritionDao.getDailySummary(userId, dateTimestamp)

                // Load meal logs
                nutritionDao.getNutritionLogsByDate(userId, dateTimestamp).collect { logs ->
                    _mealLogs.value = logs
                }
            }
        }
        viewModelScope.launch {
            userPreferences.userId?.let { userId ->
                val dateTimestamp = getDateTimestamp(date)
                // Load hydration
                nutritionDao.getHydrationLogsByDate(userId, dateTimestamp).collect { logs ->
                    _hydration.value = logs
                }
            }
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            userPreferences.userId?.let { userId ->
                val dateTimestamp = getDateTimestamp(_selectedDate.value ?: Date())

                val hydrationLog = HydrationLog(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    date = dateTimestamp,
                    amountMl = amountMl,
                    time = System.currentTimeMillis()
                )
                nutritionDao.insertHydrationLog(hydrationLog)

                // Reload data
                loadDataForDate(_selectedDate.value ?: Date())

                // Update daily summary
                updateDailySummary()
            }
        }
    }

    fun deleteLog(log: NutritionLog) {
        viewModelScope.launch {
            nutritionDao.deleteNutritionLog(log)
            loadDataForDate(_selectedDate.value ?: Date())
            updateDailySummary()
        }
    }

    private suspend fun updateDailySummary() {
        userPreferences.userId?.let { userId ->
            val dateTimestamp = getDateTimestamp(_selectedDate.value ?: Date())

            val macros = nutritionDao.getDailyMacros(userId, dateTimestamp)

            val totalCalories = macros?.calories ?: 0
            val totalProtein = macros?.protein ?: 0f
            val totalCarbs = macros?.carbs ?: 0f
            val totalFat = macros?.fat ?: 0f
            val hydrationTotal = nutritionDao.getTotalHydrationForDate(userId, dateTimestamp) ?: 0

            val summary = DailyNutritionSummary(
                userId = userId,
                date = dateTimestamp,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
                hydrationMl = hydrationTotal
            )

            nutritionDao.insertDailySummary(summary)
            _dailySummary.value = summary
        }
    }
}
