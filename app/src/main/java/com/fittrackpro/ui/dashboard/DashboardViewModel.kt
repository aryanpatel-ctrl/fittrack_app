package com.fittrackpro.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.StepDao
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.DailySteps
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.data.repository.WeatherRepository
import com.fittrackpro.service.StepCounterService
import com.fittrackpro.util.WeatherSafetyAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val trackDao: TrackDao,
    private val userDao: UserDao,
    private val stepDao: StepDao,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?> = _userName

    private val _todayStats = MutableLiveData<TodayStats>()
    val todayStats: LiveData<TodayStats> = _todayStats

    private val _weeklyData = MutableLiveData<List<DailyData>>()
    val weeklyData: LiveData<List<DailyData>> = _weeklyData

    // Step counter data
    private val _todaySteps = MutableLiveData<StepData>()
    val todaySteps: LiveData<StepData> = _todaySteps

    private val _weeklyStepData = MutableLiveData<List<DailySteps>>()
    val weeklyStepData: LiveData<List<DailySteps>> = _weeklyStepData

    // Weather data
    private val _weatherData = MutableLiveData<WeatherUiState>()
    val weatherData: LiveData<WeatherUiState> = _weatherData

    private val _isWeatherLoading = MutableLiveData<Boolean>()
    val isWeatherLoading: LiveData<Boolean> = _isWeatherLoading

    init {
        loadUserData()
        loadTodayStats()
        loadWeeklyData()
        loadStepData()
    }

    private fun loadUserData() {
        _userName.value = userPreferences.userName
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch

            // Get today's date range
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis

            // Collect today's tracks and calculate stats
            trackDao.getTracksByDateRange(userId, startOfDay, endOfDay).collect { tracks ->
                var totalDistance = 0.0
                var totalDuration = 0L
                var totalCalories = 0

                for (track in tracks) {
                    // Get track statistics for each track
                    val stats = trackDao.getStatisticsByTrackId(track.id)
                    totalDistance += stats?.distance ?: 0.0
                    totalDuration += stats?.duration ?: 0L
                    totalCalories += stats?.calories ?: 0
                }

                _todayStats.value = TodayStats(
                    distance = totalDistance,
                    duration = totalDuration,
                    calories = totalCalories,
                    activities = tracks.size
                )
            }
        }
    }

    private fun loadWeeklyData() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch

            // Calculate date range for the past 7 days
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis

            // Set to start of today
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Go back 6 days (7 days total including today)
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val startTime = calendar.timeInMillis

            // Query tracks for the past 7 days
            trackDao.getTracksByDateRange(userId, startTime, endTime).collect { tracks ->
                // Group tracks by day and calculate totals
                val dailyDataList = mutableListOf<DailyData>()
                val tempCalendar = Calendar.getInstance()

                // Create a map to hold daily aggregations
                val dailyMap = mutableMapOf<Long, DailyData>()

                // Initialize all 7 days with zero values
                tempCalendar.timeInMillis = startTime
                for (i in 0 until 7) {
                    val dayStart = tempCalendar.timeInMillis
                    dailyMap[dayStart] = DailyData(
                        date = dayStart,
                        distance = 0.0,
                        duration = 0L,
                        calories = 0
                    )
                    tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                // Aggregate track data by day
                for (track in tracks) {
                    // Find which day this track belongs to
                    tempCalendar.timeInMillis = track.startTime
                    tempCalendar.set(Calendar.HOUR_OF_DAY, 0)
                    tempCalendar.set(Calendar.MINUTE, 0)
                    tempCalendar.set(Calendar.SECOND, 0)
                    tempCalendar.set(Calendar.MILLISECOND, 0)
                    val dayStart = tempCalendar.timeInMillis

                    // Get track statistics
                    val stats = trackDao.getStatisticsByTrackId(track.id)

                    dailyMap[dayStart]?.let { existing ->
                        dailyMap[dayStart] = existing.copy(
                            distance = existing.distance + (stats?.distance ?: 0.0),
                            duration = existing.duration + (stats?.duration ?: 0L),
                            calories = existing.calories + (stats?.calories ?: 0)
                        )
                    }
                }

                // Convert map to sorted list
                dailyDataList.addAll(dailyMap.values.sortedBy { it.date })
                _weeklyData.value = dailyDataList
            }
        }
    }

    /**
     * Load weather data for the given location
     */
    fun loadWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isWeatherLoading.value = true

            val result = weatherRepository.getCurrentWeather(latitude, longitude)

            result.onSuccess { data ->
                val analysis = data.analysis
                _weatherData.value = WeatherUiState(
                    isLoading = false,
                    temperature = analysis.temperature.toInt(),
                    feelsLike = analysis.feelsLike.toInt(),
                    condition = analysis.condition,
                    description = analysis.description,
                    humidity = analysis.humidity,
                    windSpeed = analysis.windSpeed,
                    safetyStatus = analysis.status,
                    statusMessage = analysis.statusMessage,
                    statusColor = analysis.statusColor,
                    warnings = analysis.warnings,
                    suggestions = analysis.suggestions,
                    indoorAlternatives = analysis.indoorAlternatives,
                    iconCode = analysis.iconCode,
                    isFromCache = data.isFromCache,
                    error = null
                )
            }.onFailure { error ->
                _weatherData.value = WeatherUiState(
                    isLoading = false,
                    error = error.message ?: "Failed to load weather"
                )
            }

            _isWeatherLoading.value = false
        }
    }

    /**
     * Get workout recommendation based on current weather
     */
    fun getWorkoutRecommendation(
        latitude: Double,
        longitude: Double,
        workoutType: String,
        onResult: (WeatherRepository.WorkoutRecommendation?) -> Unit
    ) {
        viewModelScope.launch {
            val result = weatherRepository.getWorkoutRecommendations(
                latitude, longitude, workoutType
            )
            onResult(result.getOrNull())
        }
    }

    private fun loadStepData() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Observe today's steps from database
            stepDao.getTodayStepsFlow(userId, today).collect { dailySteps ->
                val stepCount = dailySteps?.stepCount ?: 0
                val goal = dailySteps?.goal ?: 10000
                val distance = dailySteps?.distanceMeters ?: 0.0
                val calories = dailySteps?.caloriesBurned ?: 0

                _todaySteps.value = StepData(
                    steps = stepCount,
                    goal = goal,
                    distance = distance,
                    calories = calories,
                    progress = ((stepCount.toFloat() / goal) * 100).toInt().coerceIn(0, 100),
                    goalAchieved = stepCount >= goal
                )
            }
        }

        // Load weekly step history
        loadWeeklyStepHistory()
    }

    private fun loadWeeklyStepHistory() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            // Calculate date 7 days ago
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = dateFormat.format(calendar.time)
            val endDate = dateFormat.format(Date())

            stepDao.getStepsByDateRangeFlow(userId, startDate, endDate).collect { stepList ->
                _weeklyStepData.value = stepList
            }
        }
    }

    fun updateStepGoal(goal: Int) {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            stepDao.updateStepGoal(userId, today, goal)

            // Refresh step data
            loadStepData()
        }
    }

    fun getStepStatistics(
        startDate: String,
        endDate: String,
        onResult: (StepStatistics) -> Unit
    ) {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch

            val totalSteps = stepDao.getTotalSteps(userId, startDate, endDate)
            val averageSteps = stepDao.getAverageSteps(userId, startDate, endDate)
            val totalDistance = stepDao.getTotalDistance(userId, startDate, endDate)
            val totalCalories = stepDao.getTotalCalories(userId, startDate, endDate)
            val goalsAchieved = stepDao.getGoalsAchievedCount(userId, startDate, endDate)
            val bestDay = stepDao.getBestStepCount(userId) ?: 0

            onResult(
                StepStatistics(
                    totalSteps = totalSteps,
                    averageSteps = averageSteps,
                    totalDistance = totalDistance,
                    totalCalories = totalCalories,
                    goalsAchieved = goalsAchieved,
                    bestDay = bestDay
                )
            )
        }
    }

    fun refresh() {
        loadTodayStats()
        loadStepData()
    }

    fun refreshWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isWeatherLoading.value = true
            val result = weatherRepository.getCurrentWeather(latitude, longitude, forceRefresh = true)
            result.onSuccess { data ->
                val analysis = data.analysis
                _weatherData.value = WeatherUiState(
                    isLoading = false,
                    temperature = analysis.temperature.toInt(),
                    feelsLike = analysis.feelsLike.toInt(),
                    condition = analysis.condition,
                    description = analysis.description,
                    humidity = analysis.humidity,
                    windSpeed = analysis.windSpeed,
                    safetyStatus = analysis.status,
                    statusMessage = analysis.statusMessage,
                    statusColor = analysis.statusColor,
                    warnings = analysis.warnings,
                    suggestions = analysis.suggestions,
                    indoorAlternatives = analysis.indoorAlternatives,
                    iconCode = analysis.iconCode,
                    isFromCache = false,
                    error = null
                )
            }
            _isWeatherLoading.value = false
        }
    }
}

data class TodayStats(
    val distance: Double, // meters
    val duration: Long, // milliseconds
    val calories: Int,
    val activities: Int
)

data class DailyData(
    val date: Long,
    val distance: Double,
    val duration: Long,
    val calories: Int
)

/**
 * UI state for weather display
 */
data class WeatherUiState(
    val isLoading: Boolean = true,
    val temperature: Int = 0,
    val feelsLike: Int = 0,
    val condition: String = "",
    val description: String = "",
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val safetyStatus: WeatherSafetyAnalyzer.SafetyStatus = WeatherSafetyAnalyzer.SafetyStatus.SAFE,
    val statusMessage: String = "",
    val statusColor: String = "#4CAF50",
    val warnings: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val indoorAlternatives: List<String> = emptyList(),
    val iconCode: String? = null,
    val isFromCache: Boolean = false,
    val error: String? = null
) {
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
    val isSafe: Boolean get() = safetyStatus == WeatherSafetyAnalyzer.SafetyStatus.SAFE
    val isUnsafe: Boolean get() = safetyStatus == WeatherSafetyAnalyzer.SafetyStatus.UNSAFE

    fun getWeatherEmoji(): String {
        return when (condition.lowercase()) {
            "clear" -> "☀️"
            "clouds" -> "☁️"
            "rain", "drizzle" -> "🌧️"
            "thunderstorm" -> "⛈️"
            "snow" -> "🌨️"
            "mist", "fog", "haze" -> "🌫️"
            else -> "🌤️"
        }
    }

    fun getStatusEmoji(): String {
        return when (safetyStatus) {
            WeatherSafetyAnalyzer.SafetyStatus.SAFE -> "✅"
            WeatherSafetyAnalyzer.SafetyStatus.MODIFY -> "⚠️"
            WeatherSafetyAnalyzer.SafetyStatus.UNSAFE -> "🚫"
        }
    }
}

/**
 * Data class for step counter display
 */
data class StepData(
    val steps: Int = 0,
    val goal: Int = 10000,
    val distance: Double = 0.0,
    val calories: Int = 0,
    val progress: Int = 0,
    val goalAchieved: Boolean = false
)

/**
 * Statistics for step history analysis
 */
data class StepStatistics(
    val totalSteps: Int = 0,
    val averageSteps: Int = 0,
    val totalDistance: Double = 0.0,
    val totalCalories: Int = 0,
    val goalsAchieved: Int = 0,
    val bestDay: Int = 0
)
