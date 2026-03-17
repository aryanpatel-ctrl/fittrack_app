package com.fittrackpro.data.repository

import android.util.Log
import com.fittrackpro.data.remote.api.AirPollutionResponse
import com.fittrackpro.data.remote.api.WeatherApi
import com.fittrackpro.data.remote.api.WeatherResponse
import com.fittrackpro.util.Constants
import com.fittrackpro.util.WeatherSafetyAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for weather data management
 *
 * Handles:
 * - Fetching current weather from OpenWeatherMap API
 * - Fetching air quality data
 * - Caching weather data
 * - Providing weather analysis for workout safety
 */
@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi
) {
    companion object {
        private const val TAG = "WeatherRepository"
        private const val CACHE_DURATION_MS = 15 * 60 * 1000L // 15 minutes
    }

    // In-memory cache
    private var cachedWeather: WeatherResponse? = null
    private var cachedAirPollution: AirPollutionResponse? = null
    private var cachedLatitude: Double = 0.0
    private var cachedLongitude: Double = 0.0
    private var cacheTimestamp: Long = 0L

    /**
     * Weather data with analysis
     */
    data class WeatherData(
        val weather: WeatherResponse,
        val airPollution: AirPollutionResponse?,
        val analysis: WeatherSafetyAnalyzer.WeatherAnalysis,
        val isFromCache: Boolean
    )

    /**
     * Get current weather with safety analysis
     */
    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            if (!forceRefresh && isCacheValid(latitude, longitude)) {
                val cached = cachedWeather
                if (cached != null) {
                    val analysis = WeatherSafetyAnalyzer.analyzeWeather(cached, cachedAirPollution)
                    return@withContext Result.success(
                        WeatherData(
                            weather = cached,
                            airPollution = cachedAirPollution,
                            analysis = analysis,
                            isFromCache = true
                        )
                    )
                }
            }

            // Fetch fresh data
            val weatherResponse = weatherApi.getCurrentWeather(
                latitude = latitude,
                longitude = longitude,
                apiKey = Constants.WEATHER_API_KEY
            )

            // Try to fetch air pollution data (optional, don't fail if it errors)
            val airPollution = try {
                weatherApi.getAirPollution(
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = Constants.WEATHER_API_KEY
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch air pollution data: ${e.message}")
                null
            }

            // Update cache
            updateCache(latitude, longitude, weatherResponse, airPollution)

            // Analyze weather
            val analysis = WeatherSafetyAnalyzer.analyzeWeather(weatherResponse, airPollution)

            Result.success(
                WeatherData(
                    weather = weatherResponse,
                    airPollution = airPollution,
                    analysis = analysis,
                    isFromCache = false
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch weather: ${e.message}", e)

            // Return cached data if available
            val cached = cachedWeather
            if (cached != null) {
                val analysis = WeatherSafetyAnalyzer.analyzeWeather(cached, cachedAirPollution)
                return@withContext Result.success(
                    WeatherData(
                        weather = cached,
                        airPollution = cachedAirPollution,
                        analysis = analysis,
                        isFromCache = true
                    )
                )
            }

            Result.failure(e)
        }
    }

    /**
     * Get weather forecast
     */
    suspend fun getWeatherForecast(
        latitude: Double,
        longitude: Double,
        hours: Int = 24
    ): Result<List<WeatherResponse>> = withContext(Dispatchers.IO) {
        try {
            val count = (hours / 3).coerceIn(1, 40) // 3-hour intervals
            val forecast = weatherApi.getForecast(
                latitude = latitude,
                longitude = longitude,
                apiKey = Constants.WEATHER_API_KEY,
                count = count
            )

            // Convert forecast items to WeatherResponse format for consistency
            val weatherList = forecast.list?.map { item ->
                WeatherResponse(
                    coord = forecast.city?.coord,
                    weather = item.weather,
                    main = item.main,
                    visibility = item.visibility,
                    wind = item.wind,
                    clouds = item.clouds,
                    dt = item.dt,
                    sys = null,
                    timezone = forecast.city?.timezone,
                    name = forecast.city?.name
                )
            } ?: emptyList()

            Result.success(weatherList)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch forecast: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if there's a good time to workout in the next 24 hours
     */
    suspend fun findBestWorkoutTime(
        latitude: Double,
        longitude: Double
    ): Result<BestWorkoutTime?> = withContext(Dispatchers.IO) {
        try {
            val forecastResult = getWeatherForecast(latitude, longitude, 24)
            if (forecastResult.isFailure) {
                return@withContext Result.failure(forecastResult.exceptionOrNull()!!)
            }

            val forecasts = forecastResult.getOrNull() ?: return@withContext Result.success(null)

            // Analyze each forecast period
            val goodTimes = forecasts.mapNotNull { weather ->
                val analysis = WeatherSafetyAnalyzer.analyzeWeather(weather, null)
                if (analysis.status == WeatherSafetyAnalyzer.SafetyStatus.SAFE) {
                    BestWorkoutTime(
                        timestamp = (weather.dt ?: 0) * 1000L,
                        temperature = analysis.temperature,
                        condition = analysis.condition,
                        description = analysis.description
                    )
                } else null
            }

            Result.success(goodTimes.firstOrNull())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if workout conditions are safe
     */
    suspend fun isWorkoutSafe(latitude: Double, longitude: Double): Boolean {
        val result = getCurrentWeather(latitude, longitude)
        return result.getOrNull()?.analysis?.status != WeatherSafetyAnalyzer.SafetyStatus.UNSAFE
    }

    /**
     * Get workout recommendations based on weather
     */
    suspend fun getWorkoutRecommendations(
        latitude: Double,
        longitude: Double,
        plannedWorkoutType: String
    ): Result<WorkoutRecommendation> = withContext(Dispatchers.IO) {
        val weatherResult = getCurrentWeather(latitude, longitude)
        if (weatherResult.isFailure) {
            return@withContext Result.failure(weatherResult.exceptionOrNull()!!)
        }

        val data = weatherResult.getOrNull()!!
        val analysis = data.analysis

        val recommendation = when (analysis.status) {
            WeatherSafetyAnalyzer.SafetyStatus.SAFE -> {
                WorkoutRecommendation(
                    canProceed = true,
                    message = "Great conditions for your ${formatWorkoutType(plannedWorkoutType)}!",
                    modifications = emptyList(),
                    alternatives = emptyList(),
                    tips = generateWeatherTips(analysis)
                )
            }
            WeatherSafetyAnalyzer.SafetyStatus.MODIFY -> {
                WorkoutRecommendation(
                    canProceed = true,
                    message = "You can proceed with modifications",
                    modifications = analysis.suggestions,
                    alternatives = WeatherSafetyAnalyzer.getIndoorWorkoutAlternatives(plannedWorkoutType)
                        .map { it.name },
                    tips = analysis.suggestions
                )
            }
            WeatherSafetyAnalyzer.SafetyStatus.UNSAFE -> {
                WorkoutRecommendation(
                    canProceed = false,
                    message = "Outdoor workout not recommended. ${analysis.warnings.firstOrNull() ?: ""}",
                    modifications = emptyList(),
                    alternatives = WeatherSafetyAnalyzer.getIndoorWorkoutAlternatives(plannedWorkoutType)
                        .map { "${it.name}: ${it.description}" },
                    tips = listOf("Try again when conditions improve")
                )
            }
        }

        Result.success(recommendation)
    }

    /**
     * Check cache validity
     */
    private fun isCacheValid(latitude: Double, longitude: Double): Boolean {
        val now = System.currentTimeMillis()
        val isTimeValid = (now - cacheTimestamp) < CACHE_DURATION_MS
        val isLocationValid = kotlin.math.abs(latitude - cachedLatitude) < 0.01 &&
                kotlin.math.abs(longitude - cachedLongitude) < 0.01
        return isTimeValid && isLocationValid
    }

    /**
     * Update cache
     */
    private fun updateCache(
        latitude: Double,
        longitude: Double,
        weather: WeatherResponse,
        airPollution: AirPollutionResponse?
    ) {
        cachedWeather = weather
        cachedAirPollution = airPollution
        cachedLatitude = latitude
        cachedLongitude = longitude
        cacheTimestamp = System.currentTimeMillis()
    }

    /**
     * Clear cache
     */
    fun clearCache() {
        cachedWeather = null
        cachedAirPollution = null
        cacheTimestamp = 0L
    }

    /**
     * Generate weather-specific workout tips
     */
    private fun generateWeatherTips(analysis: WeatherSafetyAnalyzer.WeatherAnalysis): List<String> {
        val tips = mutableListOf<String>()

        // Temperature-based tips
        when {
            analysis.temperature > 25 -> {
                tips.add("Wear light, breathable clothing")
                tips.add("Carry water or plan water stops")
            }
            analysis.temperature < 10 -> {
                tips.add("Dress in layers")
                tips.add("Do a longer warm-up indoors")
            }
        }

        // Humidity tips
        if (analysis.humidity > 70) {
            tips.add("Expect to sweat more - stay hydrated")
        }

        // General tip
        if (tips.isEmpty()) {
            tips.add("Enjoy your workout!")
        }

        return tips
    }

    /**
     * Format workout type for display
     */
    private fun formatWorkoutType(type: String): String {
        return type.replace("_", " ").split(" ").joinToString(" ") {
            it.replaceFirstChar { char -> char.uppercase() }
        }
    }

    /**
     * Data class for best workout time
     */
    data class BestWorkoutTime(
        val timestamp: Long,
        val temperature: Double,
        val condition: String,
        val description: String
    )

    /**
     * Data class for workout recommendation
     */
    data class WorkoutRecommendation(
        val canProceed: Boolean,
        val message: String,
        val modifications: List<String>,
        val alternatives: List<String>,
        val tips: List<String>
    )
}
