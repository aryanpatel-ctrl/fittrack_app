package com.fittrackpro.util

import com.fittrackpro.data.local.database.entity.TrackStatistics

/**
 * Fatigue Detection and Training Load Analysis
 *
 * Implements the Acute:Chronic Workload Ratio (ACWR) from the project specification:
 * - Acute Load = Average of last 7 days
 * - Chronic Load = Average of last 42 days (6 weeks)
 * - Ratio = Acute / Chronic
 *
 * Safety Zones:
 * - Safe: 0.8 - 1.3 (optimal training zone)
 * - Warning: 1.3 - 1.5 (risk of overtraining)
 * - Danger: > 1.5 (high injury risk)
 * - Undertrained: < 0.8 (not training enough)
 */
object FatigueAnalyzer {

    // ACWR Constants
    private const val ACUTE_PERIOD_DAYS = 7
    private const val CHRONIC_PERIOD_DAYS = 42

    // Threshold values from project specification
    private const val SAFE_ZONE_MIN = 0.8
    private const val SAFE_ZONE_MAX = 1.3
    private const val WARNING_ZONE_MAX = 1.5
    private const val OPTIMAL_RATIO = 1.0

    // Training load calculation factors
    private const val INTENSITY_EASY = 0.6
    private const val INTENSITY_MODERATE = 0.8
    private const val INTENSITY_HARD = 1.0
    private const val INTENSITY_VERY_HARD = 1.2

    /**
     * Training load status levels
     */
    enum class FatigueStatus {
        UNDERTRAINED,   // Ratio < 0.8
        OPTIMAL,        // Ratio 0.8 - 1.0
        BUILDING,       // Ratio 1.0 - 1.3
        WARNING,        // Ratio 1.3 - 1.5
        DANGER          // Ratio > 1.5
    }

    /**
     * Complete fatigue analysis result
     */
    data class FatigueAnalysis(
        val acuteLoad: Double,              // 7-day training load
        val chronicLoad: Double,            // 42-day training load
        val acuteChronicRatio: Double,      // ACWR ratio
        val status: FatigueStatus,          // Current status
        val statusMessage: String,          // User-friendly message
        val recommendation: String,         // Action recommendation
        val riskPercentage: Int,            // 0-100 injury risk
        val fitnessScore: Int,              // 0-100 fitness level
        val fatigueScore: Int,              // 0-100 fatigue level
        val formScore: Int,                 // Fitness - Fatigue
        val weeklyLoadTrend: LoadTrend,     // Trending up/down/stable
        val daysUntilRecovery: Int?         // Estimated days to optimal zone
    )

    /**
     * Training load trend direction
     */
    enum class LoadTrend {
        INCREASING,
        STABLE,
        DECREASING
    }

    /**
     * Daily training load data point
     */
    data class DailyLoad(
        val date: Long,
        val load: Double,
        val distance: Double,
        val duration: Long,
        val intensity: Double
    )

    /**
     * Analyze user's fatigue based on recent training data
     *
     * @param recentStats List of track statistics from last 42+ days, sorted by date ascending
     * @param tracks List of tracks with their timestamps
     */
    fun analyzeFatigue(
        recentStats: List<TrackStatistics>,
        trackDates: Map<String, Long>  // trackId -> startTime
    ): FatigueAnalysis {
        val now = System.currentTimeMillis()
        val acuteCutoff = now - (ACUTE_PERIOD_DAYS * 24 * 60 * 60 * 1000L)
        val chronicCutoff = now - (CHRONIC_PERIOD_DAYS * 24 * 60 * 60 * 1000L)

        // Calculate daily training loads
        val dailyLoads = calculateDailyLoads(recentStats, trackDates)

        // Calculate acute load (last 7 days)
        val acuteLoads = dailyLoads.filter { it.date >= acuteCutoff }
        val acuteLoad = if (acuteLoads.isNotEmpty()) {
            acuteLoads.sumOf { it.load } / ACUTE_PERIOD_DAYS
        } else 0.0

        // Calculate chronic load (last 42 days)
        val chronicLoads = dailyLoads.filter { it.date >= chronicCutoff }
        val chronicLoad = if (chronicLoads.isNotEmpty()) {
            chronicLoads.sumOf { it.load } / CHRONIC_PERIOD_DAYS
        } else 0.0

        // Calculate ACWR ratio
        val ratio = if (chronicLoad > 0) acuteLoad / chronicLoad else 0.0

        // Determine status and generate recommendations
        val status = determineStatus(ratio)
        val statusMessage = generateStatusMessage(status, ratio)
        val recommendation = generateRecommendation(status, ratio, acuteLoad, chronicLoad)
        val riskPercentage = calculateRiskPercentage(ratio)

        // Calculate fitness and fatigue scores
        val fitnessScore = calculateFitnessScore(chronicLoad, dailyLoads)
        val fatigueScore = calculateFatigueScore(acuteLoad, ratio)
        val formScore = fitnessScore - fatigueScore

        // Determine load trend
        val trend = calculateLoadTrend(dailyLoads)

        // Estimate recovery days if needed
        val daysUntilRecovery = estimateRecoveryDays(ratio, status)

        return FatigueAnalysis(
            acuteLoad = acuteLoad,
            chronicLoad = chronicLoad,
            acuteChronicRatio = ratio,
            status = status,
            statusMessage = statusMessage,
            recommendation = recommendation,
            riskPercentage = riskPercentage,
            fitnessScore = fitnessScore,
            fatigueScore = fatigueScore,
            formScore = formScore,
            weeklyLoadTrend = trend,
            daysUntilRecovery = daysUntilRecovery
        )
    }

    /**
     * Calculate daily training loads from track statistics
     */
    private fun calculateDailyLoads(
        stats: List<TrackStatistics>,
        trackDates: Map<String, Long>
    ): List<DailyLoad> {
        return stats.mapNotNull { stat ->
            val date = trackDates[stat.trackId] ?: return@mapNotNull null
            val intensity = calculateIntensity(stat)
            val load = calculateTrainingLoad(stat.distance, stat.duration, intensity)

            DailyLoad(
                date = date,
                load = load,
                distance = stat.distance,
                duration = stat.duration,
                intensity = intensity
            )
        }.sortedBy { it.date }
    }

    /**
     * Calculate training load for a single workout
     * Load = Distance (km) × Duration (hours) × Intensity Factor
     */
    private fun calculateTrainingLoad(distance: Double, duration: Long, intensity: Double): Double {
        val distanceKm = distance / 1000.0
        val durationHours = duration / (1000.0 * 60 * 60)
        return distanceKm * durationHours * intensity * 100 // Scale factor
    }

    /**
     * Calculate intensity factor based on pace and heart rate
     */
    private fun calculateIntensity(stat: TrackStatistics): Double {
        // Use pace-based intensity calculation
        // Faster pace = higher intensity
        val paceMinPerKm = stat.avgPace

        return when {
            paceMinPerKm <= 4.5 -> INTENSITY_VERY_HARD
            paceMinPerKm <= 5.5 -> INTENSITY_HARD
            paceMinPerKm <= 6.5 -> INTENSITY_MODERATE
            else -> INTENSITY_EASY
        }
    }

    /**
     * Determine fatigue status based on ACWR ratio
     */
    private fun determineStatus(ratio: Double): FatigueStatus {
        return when {
            ratio < SAFE_ZONE_MIN -> FatigueStatus.UNDERTRAINED
            ratio <= OPTIMAL_RATIO -> FatigueStatus.OPTIMAL
            ratio <= SAFE_ZONE_MAX -> FatigueStatus.BUILDING
            ratio <= WARNING_ZONE_MAX -> FatigueStatus.WARNING
            else -> FatigueStatus.DANGER
        }
    }

    /**
     * Generate user-friendly status message
     */
    private fun generateStatusMessage(status: FatigueStatus, ratio: Double): String {
        val ratioFormatted = String.format("%.2f", ratio)
        return when (status) {
            FatigueStatus.UNDERTRAINED ->
                "Training load is low (ACWR: $ratioFormatted). You could train more to build fitness."

            FatigueStatus.OPTIMAL ->
                "Perfect balance! (ACWR: $ratioFormatted). Your training load is optimal for performance."

            FatigueStatus.BUILDING ->
                "Building fitness (ACWR: $ratioFormatted). Training is increasing safely."

            FatigueStatus.WARNING ->
                "Training hard! (ACWR: $ratioFormatted). Consider reducing volume to prevent overtraining."

            FatigueStatus.DANGER ->
                "High injury risk! (ACWR: $ratioFormatted). Reduce training immediately."
        }
    }

    /**
     * Generate actionable recommendation
     */
    private fun generateRecommendation(
        status: FatigueStatus,
        ratio: Double,
        acuteLoad: Double,
        chronicLoad: Double
    ): String {
        return when (status) {
            FatigueStatus.UNDERTRAINED -> {
                val targetIncrease = ((SAFE_ZONE_MIN * chronicLoad - acuteLoad) / acuteLoad * 100).toInt()
                "Gradually increase training volume by ${maxOf(10, targetIncrease)}% this week. " +
                        "Add an extra workout or extend your long run."
            }

            FatigueStatus.OPTIMAL ->
                "Maintain current training load. This is the sweet spot for performance gains. " +
                        "Focus on quality over quantity."

            FatigueStatus.BUILDING ->
                "Good progress! Continue current plan but monitor how you feel. " +
                        "Ensure adequate sleep and nutrition for recovery."

            FatigueStatus.WARNING -> {
                val reduction = ((ratio - SAFE_ZONE_MAX) / ratio * 100).toInt()
                "Reduce training volume by ${maxOf(15, reduction)}% this week. " +
                        "Replace a hard workout with easy running or cross-training."
            }

            FatigueStatus.DANGER -> {
                val reduction = ((ratio - SAFE_ZONE_MAX) / ratio * 100).toInt()
                "Take 2-3 rest days immediately. Then reduce volume by ${maxOf(25, reduction)}%. " +
                        "Consider a recovery week with only easy activities."
            }
        }
    }

    /**
     * Calculate injury risk percentage (0-100)
     */
    private fun calculateRiskPercentage(ratio: Double): Int {
        return when {
            ratio < SAFE_ZONE_MIN -> 10
            ratio <= OPTIMAL_RATIO -> 5
            ratio <= SAFE_ZONE_MAX -> 15
            ratio <= WARNING_ZONE_MAX -> ((ratio - SAFE_ZONE_MAX) / (WARNING_ZONE_MAX - SAFE_ZONE_MAX) * 40 + 30).toInt()
            else -> minOf(95, (70 + (ratio - WARNING_ZONE_MAX) * 50).toInt())
        }
    }

    /**
     * Calculate fitness score (0-100) based on chronic load and consistency
     */
    private fun calculateFitnessScore(chronicLoad: Double, dailyLoads: List<DailyLoad>): Int {
        // Base score from chronic load (higher load = more fit)
        val loadScore = minOf(50.0, chronicLoad / 2)

        // Consistency bonus (regular training = bonus points)
        val uniqueTrainingDays = dailyLoads.map { it.date / (24 * 60 * 60 * 1000L) }.distinct().size
        val consistencyScore = minOf(30.0, uniqueTrainingDays.toDouble())

        // Recent activity bonus
        val recentActivityScore = if (dailyLoads.any {
                it.date > System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
            }) 20.0 else 10.0

        return minOf(100, (loadScore + consistencyScore + recentActivityScore).toInt())
    }

    /**
     * Calculate fatigue score (0-100) based on acute load and ACWR
     */
    private fun calculateFatigueScore(acuteLoad: Double, ratio: Double): Int {
        val loadFatigue = minOf(40.0, acuteLoad / 2)
        val ratioFatigue = when {
            ratio > WARNING_ZONE_MAX -> 60.0
            ratio > SAFE_ZONE_MAX -> 40.0
            ratio > OPTIMAL_RATIO -> 20.0
            else -> 10.0
        }
        return minOf(100, (loadFatigue + ratioFatigue).toInt())
    }

    /**
     * Calculate weekly load trend
     */
    private fun calculateLoadTrend(dailyLoads: List<DailyLoad>): LoadTrend {
        if (dailyLoads.size < 14) return LoadTrend.STABLE

        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val twoWeeksAgo = now - (14 * 24 * 60 * 60 * 1000L)

        val thisWeekLoad = dailyLoads.filter { it.date >= oneWeekAgo }.sumOf { it.load }
        val lastWeekLoad = dailyLoads.filter { it.date in twoWeeksAgo until oneWeekAgo }.sumOf { it.load }

        val changePercent = if (lastWeekLoad > 0) {
            (thisWeekLoad - lastWeekLoad) / lastWeekLoad
        } else 0.0

        return when {
            changePercent > 0.1 -> LoadTrend.INCREASING
            changePercent < -0.1 -> LoadTrend.DECREASING
            else -> LoadTrend.STABLE
        }
    }

    /**
     * Estimate days until returning to optimal zone
     */
    private fun estimateRecoveryDays(ratio: Double, status: FatigueStatus): Int? {
        return when (status) {
            FatigueStatus.WARNING -> 3
            FatigueStatus.DANGER -> 5
            FatigueStatus.UNDERTRAINED -> null // Not applicable
            else -> null // Already in good zone
        }
    }

    /**
     * Quick check if user should rest today
     */
    fun shouldRestToday(ratio: Double): Boolean {
        return ratio > WARNING_ZONE_MAX
    }

    /**
     * Get color code for UI display
     */
    fun getStatusColor(status: FatigueStatus): String {
        return when (status) {
            FatigueStatus.UNDERTRAINED -> "#FFA500"  // Orange
            FatigueStatus.OPTIMAL -> "#4CAF50"      // Green
            FatigueStatus.BUILDING -> "#8BC34A"     // Light Green
            FatigueStatus.WARNING -> "#FF9800"      // Orange
            FatigueStatus.DANGER -> "#F44336"       // Red
        }
    }
}
