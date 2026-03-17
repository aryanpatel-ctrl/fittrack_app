package com.fittrackpro.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.UserDao
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.data.repository.WeatherRepository
import com.fittrackpro.util.WeatherSafetyAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val trackDao: TrackDao,
    private val userDao: UserDao,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?> = _userName

    private val _todayStats = MutableLiveData<TodayStats>()
    val todayStats: LiveData<TodayStats> = _todayStats

    private val _recentActivities = MutableLiveData<List<Track>>()
    val recentActivities: LiveData<List<Track>> = _recentActivities

    private val _weeklyData = MutableLiveData<List<DailyData>>()
    val weeklyData: LiveData<List<DailyData>> = _weeklyData

    // Weather data
    private val _weatherData = MutableLiveData<WeatherUiState>()
    val weatherData: LiveData<WeatherUiState> = _weatherData

    private val _isWeatherLoading = MutableLiveData<Boolean>()
    val isWeatherLoading: LiveData<Boolean> = _isWeatherLoading

    init {
        loadUserData()
        loadTodayStats()
        loadRecentActivities()
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

            // For now, return empty stats - will be populated with actual data
            _todayStats.value = TodayStats(
                distance = 0.0,
                duration = 0L,
                calories = 0,
                activities = 0
            )
        }
    }

    private fun loadRecentActivities() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val activities = trackDao.getRecentTracks(userId, 5)
            _recentActivities.value = activities
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

    fun refresh() {
        loadTodayStats()
        loadRecentActivities()
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
