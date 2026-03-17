package com.fittrackpro.data.remote.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OpenWeatherMap API interface
 * Base URL: https://api.openweathermap.org/data/2.5/
 */
interface WeatherApi {

    /**
     * Get current weather by coordinates
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric" // metric or imperial
    ): WeatherResponse

    /**
     * Get weather forecast (5 days / 3 hours)
     */
    @GET("forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("cnt") count: Int = 8 // Number of 3-hour periods (8 = 24 hours)
    ): ForecastResponse

    /**
     * Get air pollution data
     */
    @GET("air_pollution")
    suspend fun getAirPollution(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String
    ): AirPollutionResponse
}

// Response models

data class WeatherResponse(
    val coord: Coordinates?,
    val weather: List<Weather>?,
    val main: MainWeather?,
    val visibility: Int?,
    val wind: Wind?,
    val clouds: Clouds?,
    val dt: Long?, // Unix timestamp
    val sys: Sys?,
    val timezone: Int?,
    val name: String?
)

data class Coordinates(
    val lon: Double?,
    val lat: Double?
)

data class Weather(
    val id: Int?,
    val main: String?, // e.g., "Clear", "Clouds", "Rain"
    val description: String?,
    val icon: String?
)

data class MainWeather(
    val temp: Double?, // Temperature
    val feels_like: Double?,
    val temp_min: Double?,
    val temp_max: Double?,
    val pressure: Int?, // hPa
    val humidity: Int?, // %
    val sea_level: Int?,
    val grnd_level: Int?
)

data class Wind(
    val speed: Double?, // m/s or mph depending on units
    val deg: Int?, // Wind direction in degrees
    val gust: Double?
)

data class Clouds(
    val all: Int? // Cloudiness %
)

data class Sys(
    val country: String?,
    val sunrise: Long?, // Unix timestamp
    val sunset: Long? // Unix timestamp
)

data class ForecastResponse(
    val cod: String?,
    val message: Int?,
    val cnt: Int?,
    val list: List<ForecastItem>?,
    val city: City?
)

data class ForecastItem(
    val dt: Long?,
    val main: MainWeather?,
    val weather: List<Weather>?,
    val clouds: Clouds?,
    val wind: Wind?,
    val visibility: Int?,
    val pop: Double?, // Probability of precipitation
    val dt_txt: String?
)

data class City(
    val id: Int?,
    val name: String?,
    val coord: Coordinates?,
    val country: String?,
    val population: Int?,
    val timezone: Int?,
    val sunrise: Long?,
    val sunset: Long?
)

data class AirPollutionResponse(
    val coord: Coordinates?,
    val list: List<AirPollutionData>?
)

data class AirPollutionData(
    val dt: Long?,
    val main: AirQualityIndex?,
    val components: AirComponents?
)

data class AirQualityIndex(
    val aqi: Int? // Air Quality Index: 1 = Good, 2 = Fair, 3 = Moderate, 4 = Poor, 5 = Very Poor
)

data class AirComponents(
    val co: Double?, // Carbon monoxide
    val no: Double?, // Nitrogen monoxide
    val no2: Double?, // Nitrogen dioxide
    val o3: Double?, // Ozone
    val so2: Double?, // Sulphur dioxide
    val pm2_5: Double?, // Fine particles
    val pm10: Double?, // Coarse particles
    val nh3: Double? // Ammonia
)
