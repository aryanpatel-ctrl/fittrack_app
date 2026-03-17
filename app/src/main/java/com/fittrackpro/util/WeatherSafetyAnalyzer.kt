package com.fittrackpro.util

import com.fittrackpro.data.remote.api.AirPollutionResponse
import com.fittrackpro.data.remote.api.WeatherResponse

/**
 * Weather Safety Analysis System
 *
 * Analyzes weather conditions and provides safety recommendations for outdoor workouts.
 * From the project specification:
 *
 * Temperature:
 * - > 35°C: UNSAFE (Extreme heat)
 * - > 30°C: MODIFY (Very hot)
 * - < -10°C: UNSAFE (Extreme cold)
 * - < 0°C: MODIFY (Very cold)
 *
 * Air Quality (AQI):
 * - > 150: UNSAFE (Unhealthy)
 * - > 100: MODIFY (Moderate)
 *
 * Conditions:
 * - Thunderstorm: UNSAFE
 * - Rain/Snow: MODIFY
 *
 * Wind:
 * - > 40 km/h: UNSAFE
 * - > 25 km/h: MODIFY
 *
 * UV Index:
 * - > 8: High warning
 */
object WeatherSafetyAnalyzer {

    /**
     * Safety recommendation status
     */
    enum class SafetyStatus {
        SAFE,       // Perfect conditions
        MODIFY,     // Can workout with modifications
        UNSAFE      // Should not workout outdoors
    }

    /**
     * Complete weather analysis result
     */
    data class WeatherAnalysis(
        val status: SafetyStatus,
        val temperature: Double,
        val feelsLike: Double,
        val condition: String,
        val description: String,
        val humidity: Int,
        val windSpeed: Double,          // km/h
        val uvIndex: Double,
        val airQualityIndex: Int?,
        val warnings: List<String>,
        val suggestions: List<String>,
        val indoorAlternatives: List<String>,
        val statusMessage: String,
        val statusColor: String,        // Hex color for UI
        val iconCode: String?           // Weather icon code
    )

    /**
     * Indoor workout alternatives
     */
    data class IndoorAlternative(
        val name: String,
        val type: String,
        val description: String,
        val estimatedCalories: Int
    )

    // Temperature thresholds (Celsius)
    private const val TEMP_EXTREME_HOT = 35.0
    private const val TEMP_VERY_HOT = 30.0
    private const val TEMP_EXTREME_COLD = -10.0
    private const val TEMP_COLD = 0.0

    // Wind thresholds (km/h)
    private const val WIND_DANGEROUS = 40.0
    private const val WIND_STRONG = 25.0

    // AQI thresholds
    private const val AQI_UNHEALTHY = 150
    private const val AQI_MODERATE = 100

    // UV Index threshold
    private const val UV_VERY_HIGH = 8.0
    private const val UV_HIGH = 6.0

    /**
     * Analyze weather conditions for workout safety
     */
    fun analyzeWeather(
        weatherResponse: WeatherResponse,
        airPollution: AirPollutionResponse? = null
    ): WeatherAnalysis {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var overallStatus = SafetyStatus.SAFE

        // Extract data from response
        val temp = weatherResponse.main?.temp ?: 20.0
        val feelsLike = weatherResponse.main?.feels_like ?: temp
        val humidity = weatherResponse.main?.humidity ?: 50
        val windSpeedMs = weatherResponse.wind?.speed ?: 0.0
        val windSpeedKmh = windSpeedMs * 3.6 // Convert m/s to km/h
        val condition = weatherResponse.weather?.firstOrNull()?.main ?: "Clear"
        val description = weatherResponse.weather?.firstOrNull()?.description ?: ""
        val iconCode = weatherResponse.weather?.firstOrNull()?.icon

        // Get AQI if available
        val aqi = airPollution?.list?.firstOrNull()?.main?.aqi

        // Analyze temperature
        val tempStatus = analyzeTemperature(temp, feelsLike)
        if (tempStatus.first != SafetyStatus.SAFE) {
            overallStatus = maxStatus(overallStatus, tempStatus.first)
            warnings.addAll(tempStatus.second)
            suggestions.addAll(tempStatus.third)
        }

        // Analyze weather condition (rain, storm, etc.)
        val conditionStatus = analyzeCondition(condition, description)
        if (conditionStatus.first != SafetyStatus.SAFE) {
            overallStatus = maxStatus(overallStatus, conditionStatus.first)
            warnings.addAll(conditionStatus.second)
            suggestions.addAll(conditionStatus.third)
        }

        // Analyze wind
        val windStatus = analyzeWind(windSpeedKmh)
        if (windStatus.first != SafetyStatus.SAFE) {
            overallStatus = maxStatus(overallStatus, windStatus.first)
            warnings.addAll(windStatus.second)
            suggestions.addAll(windStatus.third)
        }

        // Analyze air quality
        if (aqi != null) {
            val aqiStatus = analyzeAirQuality(aqi)
            if (aqiStatus.first != SafetyStatus.SAFE) {
                overallStatus = maxStatus(overallStatus, aqiStatus.first)
                warnings.addAll(aqiStatus.second)
                suggestions.addAll(aqiStatus.third)
            }
        }

        // Analyze humidity
        val humidityStatus = analyzeHumidity(humidity, temp)
        if (humidityStatus.first != SafetyStatus.SAFE) {
            // Humidity alone doesn't make it unsafe, just adds warnings
            warnings.addAll(humidityStatus.second)
            suggestions.addAll(humidityStatus.third)
        }

        // Generate status message
        val statusMessage = generateStatusMessage(overallStatus, warnings)
        val statusColor = getStatusColor(overallStatus)

        // Generate indoor alternatives if needed
        val indoorAlternatives = if (overallStatus != SafetyStatus.SAFE) {
            generateIndoorAlternatives()
        } else {
            emptyList()
        }

        return WeatherAnalysis(
            status = overallStatus,
            temperature = temp,
            feelsLike = feelsLike,
            condition = condition,
            description = description.replaceFirstChar { it.uppercase() },
            humidity = humidity,
            windSpeed = windSpeedKmh,
            uvIndex = 0.0, // UV not available in basic weather API
            airQualityIndex = aqi,
            warnings = warnings,
            suggestions = suggestions,
            indoorAlternatives = indoorAlternatives,
            statusMessage = statusMessage,
            statusColor = statusColor,
            iconCode = iconCode
        )
    }

    /**
     * Analyze temperature conditions
     */
    private fun analyzeTemperature(temp: Double, feelsLike: Double): Triple<SafetyStatus, List<String>, List<String>> {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var status = SafetyStatus.SAFE

        val effectiveTemp = maxOf(temp, feelsLike)

        when {
            effectiveTemp > TEMP_EXTREME_HOT -> {
                status = SafetyStatus.UNSAFE
                warnings.add("Extreme heat (${temp.toInt()}°C)")
                suggestions.add("Workout indoors with AC")
                suggestions.add("Reschedule to early morning (6-8 AM)")
            }
            effectiveTemp > TEMP_VERY_HOT -> {
                status = SafetyStatus.MODIFY
                warnings.add("Very hot (${temp.toInt()}°C)")
                suggestions.add("Reduce intensity by 20%")
                suggestions.add("Stay hydrated - drink every 15 minutes")
                suggestions.add("Wear light, breathable clothing")
            }
            temp < TEMP_EXTREME_COLD -> {
                status = SafetyStatus.UNSAFE
                warnings.add("Extreme cold (${temp.toInt()}°C)")
                suggestions.add("Indoor treadmill workout recommended")
            }
            temp < TEMP_COLD -> {
                status = SafetyStatus.MODIFY
                warnings.add("Cold conditions (${temp.toInt()}°C)")
                suggestions.add("Wear thermal layers")
                suggestions.add("Warm up indoors first")
                suggestions.add("Protect extremities (gloves, hat)")
            }
        }

        return Triple(status, warnings, suggestions)
    }

    /**
     * Analyze weather condition (rain, storm, etc.)
     */
    private fun analyzeCondition(condition: String, description: String): Triple<SafetyStatus, List<String>, List<String>> {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var status = SafetyStatus.SAFE

        when (condition.lowercase()) {
            "thunderstorm" -> {
                status = SafetyStatus.UNSAFE
                warnings.add("Thunderstorm - lightning risk")
                suggestions.add("Indoor workout strongly recommended")
                suggestions.add("Wait at least 30 minutes after last thunder")
            }
            "rain", "drizzle" -> {
                status = SafetyStatus.MODIFY
                warnings.add("Rain: ${description.replaceFirstChar { it.uppercase() }}")
                suggestions.add("Wear waterproof/reflective gear")
                suggestions.add("Reduce pace on slippery surfaces")
                suggestions.add("Avoid routes with poor drainage")
            }
            "snow" -> {
                status = SafetyStatus.MODIFY
                warnings.add("Snow: ${description.replaceFirstChar { it.uppercase() }}")
                suggestions.add("Wear trail shoes with good grip")
                suggestions.add("Reduce pace significantly")
                suggestions.add("Stay on cleared paths when possible")
            }
            "fog", "mist", "haze" -> {
                status = SafetyStatus.MODIFY
                warnings.add("Low visibility: ${description.replaceFirstChar { it.uppercase() }}")
                suggestions.add("Wear bright, reflective clothing")
                suggestions.add("Stay on familiar routes")
                suggestions.add("Be extra cautious of traffic")
            }
        }

        return Triple(status, warnings, suggestions)
    }

    /**
     * Analyze wind conditions
     */
    private fun analyzeWind(windSpeedKmh: Double): Triple<SafetyStatus, List<String>, List<String>> {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var status = SafetyStatus.SAFE

        when {
            windSpeedKmh > WIND_DANGEROUS -> {
                status = SafetyStatus.UNSAFE
                warnings.add("Strong winds (${windSpeedKmh.toInt()} km/h)")
                suggestions.add("Indoor workout recommended")
                suggestions.add("Risk of debris and difficult running conditions")
            }
            windSpeedKmh > WIND_STRONG -> {
                status = SafetyStatus.MODIFY
                warnings.add("Windy conditions (${windSpeedKmh.toInt()} km/h)")
                suggestions.add("Choose sheltered route")
                suggestions.add("Start into the wind, finish with it")
                suggestions.add("Expect slower pace on exposed sections")
            }
        }

        return Triple(status, warnings, suggestions)
    }

    /**
     * Analyze air quality
     */
    private fun analyzeAirQuality(aqi: Int): Triple<SafetyStatus, List<String>, List<String>> {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var status = SafetyStatus.SAFE

        // OpenWeatherMap AQI: 1=Good, 2=Fair, 3=Moderate, 4=Poor, 5=Very Poor
        // Convert to standard AQI scale for display
        val displayAqi = when (aqi) {
            1 -> 25
            2 -> 75
            3 -> 125
            4 -> 175
            5 -> 250
            else -> 50
        }

        when {
            aqi >= 4 -> { // Poor or Very Poor
                status = SafetyStatus.UNSAFE
                warnings.add("Unhealthy air quality (AQI: $displayAqi)")
                suggestions.add("Indoor workout only")
                suggestions.add("Avoid outdoor exposure")
            }
            aqi == 3 -> { // Moderate
                status = SafetyStatus.MODIFY
                warnings.add("Moderate air quality (AQI: $displayAqi)")
                suggestions.add("Reduce outdoor workout duration")
                suggestions.add("Avoid high-intensity outdoor exercise")
            }
        }

        return Triple(status, warnings, suggestions)
    }

    /**
     * Analyze humidity
     */
    private fun analyzeHumidity(humidity: Int, temp: Double): Triple<SafetyStatus, List<String>, List<String>> {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var status = SafetyStatus.SAFE

        // High humidity is more dangerous in hot weather
        if (humidity > 80 && temp > 25) {
            warnings.add("High humidity ($humidity%)")
            suggestions.add("Increase hydration frequency")
            suggestions.add("Watch for signs of heat exhaustion")
            suggestions.add("Take more frequent breaks")
        } else if (humidity < 30) {
            warnings.add("Low humidity ($humidity%)")
            suggestions.add("Stay extra hydrated")
            suggestions.add("Protect lips and skin")
        }

        return Triple(status, warnings, suggestions)
    }

    /**
     * Analyze UV index
     */
    fun analyzeUVIndex(uvIndex: Double): Triple<SafetyStatus, List<String>, List<String>> {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        var status = SafetyStatus.SAFE

        when {
            uvIndex > UV_VERY_HIGH -> {
                warnings.add("Very high UV index (${uvIndex.toInt()})")
                suggestions.add("Apply SPF 50+ sunscreen")
                suggestions.add("Wear sunglasses and hat")
                suggestions.add("Seek shade when possible")
                suggestions.add("Consider early morning or evening workout")
            }
            uvIndex > UV_HIGH -> {
                warnings.add("High UV index (${uvIndex.toInt()})")
                suggestions.add("Apply sunscreen SPF 30+")
                suggestions.add("Wear protective eyewear")
            }
        }

        return Triple(status, warnings, suggestions)
    }

    /**
     * Get indoor alternatives based on planned outdoor workout
     */
    private fun generateIndoorAlternatives(): List<String> {
        return listOf(
            "Treadmill run at 1% incline",
            "Indoor cycling / Stationary bike",
            "Elliptical trainer",
            "Swimming (if available)",
            "Bodyweight HIIT workout",
            "Yoga or stretching session"
        )
    }

    /**
     * Generate detailed indoor workout alternatives
     */
    fun getIndoorWorkoutAlternatives(originalWorkoutType: String): List<IndoorAlternative> {
        return when (originalWorkoutType.lowercase()) {
            "easy_run", "long_run" -> listOf(
                IndoorAlternative(
                    name = "Treadmill Run",
                    type = "treadmill_run",
                    description = "Set treadmill to 1% incline to simulate outdoor running",
                    estimatedCalories = 400
                ),
                IndoorAlternative(
                    name = "Indoor Cycling",
                    type = "indoor_cycling",
                    description = "Moderate effort cycling for similar cardiovascular benefit",
                    estimatedCalories = 350
                ),
                IndoorAlternative(
                    name = "Elliptical Trainer",
                    type = "elliptical",
                    description = "Low-impact alternative with similar muscle engagement",
                    estimatedCalories = 380
                )
            )
            "tempo_run", "interval" -> listOf(
                IndoorAlternative(
                    name = "Treadmill Intervals",
                    type = "treadmill_interval",
                    description = "Alternate between fast and recovery paces on treadmill",
                    estimatedCalories = 450
                ),
                IndoorAlternative(
                    name = "HIIT Workout",
                    type = "hiit",
                    description = "High-intensity interval training with bodyweight exercises",
                    estimatedCalories = 400
                ),
                IndoorAlternative(
                    name = "Spin Class",
                    type = "spin",
                    description = "High-intensity cycling workout",
                    estimatedCalories = 500
                )
            )
            else -> listOf(
                IndoorAlternative(
                    name = "General Cardio",
                    type = "cardio",
                    description = "30-45 minutes of moderate indoor cardio",
                    estimatedCalories = 350
                ),
                IndoorAlternative(
                    name = "Strength Training",
                    type = "strength",
                    description = "Full body strength workout with bodyweight or weights",
                    estimatedCalories = 300
                )
            )
        }
    }

    /**
     * Generate user-friendly status message
     */
    private fun generateStatusMessage(status: SafetyStatus, warnings: List<String>): String {
        return when (status) {
            SafetyStatus.SAFE -> "Perfect conditions for your workout!"
            SafetyStatus.MODIFY -> "Workout possible with modifications: ${warnings.firstOrNull() ?: ""}"
            SafetyStatus.UNSAFE -> "Unsafe conditions: ${warnings.firstOrNull() ?: ""}. Indoor workout recommended."
        }
    }

    /**
     * Get color for status display
     */
    private fun getStatusColor(status: SafetyStatus): String {
        return when (status) {
            SafetyStatus.SAFE -> "#4CAF50"    // Green
            SafetyStatus.MODIFY -> "#FF9800"  // Orange
            SafetyStatus.UNSAFE -> "#F44336"  // Red
        }
    }

    /**
     * Get maximum (worst) status
     */
    private fun maxStatus(current: SafetyStatus, new: SafetyStatus): SafetyStatus {
        return when {
            current == SafetyStatus.UNSAFE || new == SafetyStatus.UNSAFE -> SafetyStatus.UNSAFE
            current == SafetyStatus.MODIFY || new == SafetyStatus.MODIFY -> SafetyStatus.MODIFY
            else -> SafetyStatus.SAFE
        }
    }

    /**
     * Quick check if weather is safe for outdoor workout
     */
    fun isSafeForOutdoorWorkout(weatherResponse: WeatherResponse): Boolean {
        val analysis = analyzeWeather(weatherResponse)
        return analysis.status != SafetyStatus.UNSAFE
    }

    /**
     * Get weather emoji for UI
     */
    fun getWeatherEmoji(condition: String): String {
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

    /**
     * Get status emoji for UI
     */
    fun getStatusEmoji(status: SafetyStatus): String {
        return when (status) {
            SafetyStatus.SAFE -> "✅"
            SafetyStatus.MODIFY -> "⚠️"
            SafetyStatus.UNSAFE -> "🚫"
        }
    }
}
