package com.fittrackpro.util

import kotlin.math.pow

/**
 * Race Time Prediction using the Riegel Formula
 *
 * From the project specification:
 * T2 = T1 × (D2/D1)^1.06
 *
 * Where:
 * - T1 = Known race time
 * - D1 = Known race distance
 * - T2 = Predicted race time
 * - D2 = Target race distance
 *
 * Additional adjustments based on:
 * - User's endurance factor (from long run data)
 * - Training consistency
 * - Recent performance trends
 */
object RaceTimePredictor {

    // Riegel exponent (1.06 is the standard, can be adjusted for individuals)
    private const val RIEGEL_EXPONENT = 1.06

    // Standard race distances in meters
    object RaceDistance {
        const val ONE_KM = 1000.0
        const val ONE_MILE = 1609.34
        const val FIVE_KM = 5000.0
        const val TEN_KM = 10000.0
        const val HALF_MARATHON = 21097.5
        const val MARATHON = 42195.0
    }

    /**
     * Race prediction result
     */
    data class RacePrediction(
        val distance: Double,                  // Target distance in meters
        val distanceName: String,              // e.g., "5K", "10K"
        val predictedTime: Long,               // Predicted time in milliseconds
        val predictedTimeFormatted: String,    // e.g., "25:30"
        val predictedPace: Float,              // Predicted pace in min/km
        val confidenceLevel: ConfidenceLevel,  // How reliable is this prediction
        val confidencePercentage: Int,         // 0-100 confidence
        val adjustments: List<String>          // Applied adjustments
    )

    /**
     * Complete prediction analysis
     */
    data class PredictionAnalysis(
        val baseDistance: Double,              // Distance used for prediction
        val baseTime: Long,                    // Time used for prediction
        val predictions: List<RacePrediction>, // All race predictions
        val vdotScore: Double,                 // Equivalent VO2max estimate
        val runnerLevel: String,               // e.g., "Intermediate Runner"
        val improvementPotential: String       // Suggestions for improvement
    )

    /**
     * Confidence levels for predictions
     */
    enum class ConfidenceLevel {
        HIGH,       // Prediction for similar distance (±50%)
        MEDIUM,     // Prediction for 2-3x distance difference
        LOW         // Prediction for 4x+ distance difference
    }

    /**
     * User's training data for adjustments
     */
    data class TrainingData(
        val longestRunDistance: Double = 0.0,      // Longest run in last 30 days
        val averageWeeklyDistance: Double = 0.0,   // Average weekly mileage
        val recentTrend: Double = 0.0,             // -1 to 1 (declining to improving)
        val trainingWeeks: Int = 0,                // Weeks of consistent training
        val avgPaceLast30Days: Float = 0f          // Average training pace
    )

    /**
     * Predict race time using Riegel formula with adjustments
     *
     * @param knownDistance Distance of known performance (meters)
     * @param knownTime Time of known performance (milliseconds)
     * @param targetDistance Target race distance (meters)
     * @param trainingData Optional training data for adjustments
     */
    fun predictRaceTime(
        knownDistance: Double,
        knownTime: Long,
        targetDistance: Double,
        trainingData: TrainingData? = null
    ): RacePrediction {
        // Apply Riegel formula: T2 = T1 × (D2/D1)^1.06
        val distanceRatio = targetDistance / knownDistance
        val baselinePrediction = knownTime * distanceRatio.pow(RIEGEL_EXPONENT)

        // Apply adjustments based on training data
        val (adjustedTime, adjustments) = applyAdjustments(
            baselinePrediction,
            knownDistance,
            targetDistance,
            trainingData
        )

        // Calculate confidence
        val confidence = calculateConfidence(knownDistance, targetDistance, trainingData)

        // Calculate predicted pace
        val predictedPace = (adjustedTime / 1000.0 / 60.0) / (targetDistance / 1000.0)

        return RacePrediction(
            distance = targetDistance,
            distanceName = getDistanceName(targetDistance),
            predictedTime = adjustedTime.toLong(),
            predictedTimeFormatted = formatTime(adjustedTime.toLong()),
            predictedPace = predictedPace.toFloat(),
            confidenceLevel = confidence.first,
            confidencePercentage = confidence.second,
            adjustments = adjustments
        )
    }

    /**
     * Get predictions for all standard race distances
     */
    fun getAllPredictions(
        knownDistance: Double,
        knownTime: Long,
        trainingData: TrainingData? = null
    ): PredictionAnalysis {
        val raceDistances = listOf(
            RaceDistance.ONE_KM,
            RaceDistance.FIVE_KM,
            RaceDistance.TEN_KM,
            RaceDistance.HALF_MARATHON,
            RaceDistance.MARATHON
        )

        val predictions = raceDistances
            .filter { it != knownDistance } // Exclude the known distance
            .map { distance ->
                predictRaceTime(knownDistance, knownTime, distance, trainingData)
            }

        // Calculate VDOT score (VO2max equivalent)
        val vdot = calculateVDOT(knownDistance, knownTime)

        // Determine runner level
        val runnerLevel = getRunnerLevel(vdot)

        // Generate improvement suggestions
        val improvementPotential = generateImprovementSuggestions(vdot, trainingData)

        return PredictionAnalysis(
            baseDistance = knownDistance,
            baseTime = knownTime,
            predictions = predictions,
            vdotScore = vdot,
            runnerLevel = runnerLevel,
            improvementPotential = improvementPotential
        )
    }

    /**
     * Apply training-based adjustments to the prediction
     */
    private fun applyAdjustments(
        baselinePrediction: Double,
        knownDistance: Double,
        targetDistance: Double,
        trainingData: TrainingData?
    ): Pair<Double, List<String>> {
        var adjustedTime = baselinePrediction
        val adjustments = mutableListOf<String>()

        if (trainingData == null) {
            adjustments.add("No training data - using standard formula")
            return Pair(adjustedTime, adjustments)
        }

        // Adjustment 1: Endurance factor based on long run distance
        if (targetDistance > knownDistance * 2) {
            val enduranceFactor = when {
                trainingData.longestRunDistance >= targetDistance * 0.7 -> {
                    adjustments.add("Good endurance base: -3%")
                    0.97
                }
                trainingData.longestRunDistance >= targetDistance * 0.5 -> {
                    adjustments.add("Moderate endurance: no adjustment")
                    1.0
                }
                else -> {
                    adjustments.add("Limited long runs: +5%")
                    1.05
                }
            }
            adjustedTime *= enduranceFactor
        }

        // Adjustment 2: Weekly mileage adjustment
        val weeklyDistanceKm = trainingData.averageWeeklyDistance / 1000
        val targetDistanceKm = targetDistance / 1000
        val mileageRatio = weeklyDistanceKm / targetDistanceKm

        val mileageFactor = when {
            mileageRatio >= 3.0 -> {
                adjustments.add("High weekly mileage: -2%")
                0.98
            }
            mileageRatio >= 2.0 -> {
                adjustments.add("Good weekly mileage: no adjustment")
                1.0
            }
            mileageRatio >= 1.0 -> {
                adjustments.add("Low weekly mileage: +3%")
                1.03
            }
            else -> {
                adjustments.add("Very low mileage: +5%")
                1.05
            }
        }
        adjustedTime *= mileageFactor

        // Adjustment 3: Recent performance trend
        if (trainingData.recentTrend != 0.0) {
            val trendFactor = 1.0 - (trainingData.recentTrend * 0.03) // ±3% based on trend
            if (trainingData.recentTrend > 0) {
                adjustments.add("Improving trend: -${(trainingData.recentTrend * 3).toInt()}%")
            } else if (trainingData.recentTrend < 0) {
                adjustments.add("Declining trend: +${(-trainingData.recentTrend * 3).toInt()}%")
            }
            adjustedTime *= trendFactor
        }

        // Adjustment 4: Training consistency
        val consistencyFactor = when {
            trainingData.trainingWeeks >= 12 -> {
                adjustments.add("12+ weeks training: -2%")
                0.98
            }
            trainingData.trainingWeeks >= 8 -> {
                adjustments.add("8+ weeks training: no adjustment")
                1.0
            }
            trainingData.trainingWeeks >= 4 -> {
                adjustments.add("4-8 weeks training: +2%")
                1.02
            }
            else -> {
                adjustments.add("Limited training: +4%")
                1.04
            }
        }
        adjustedTime *= consistencyFactor

        return Pair(adjustedTime, adjustments)
    }

    /**
     * Calculate prediction confidence
     */
    private fun calculateConfidence(
        knownDistance: Double,
        targetDistance: Double,
        trainingData: TrainingData?
    ): Pair<ConfidenceLevel, Int> {
        val distanceRatio = maxOf(knownDistance, targetDistance) / minOf(knownDistance, targetDistance)

        var baseConfidence = when {
            distanceRatio <= 2.0 -> 85  // Similar distances
            distanceRatio <= 4.0 -> 70  // Moderate extrapolation
            distanceRatio <= 8.0 -> 55  // Large extrapolation
            else -> 40                   // Very large extrapolation
        }

        // Adjust confidence based on training data availability
        if (trainingData != null) {
            if (trainingData.trainingWeeks >= 8) baseConfidence += 5
            if (trainingData.longestRunDistance >= targetDistance * 0.5) baseConfidence += 5
        } else {
            baseConfidence -= 10
        }

        val level = when {
            baseConfidence >= 75 -> ConfidenceLevel.HIGH
            baseConfidence >= 55 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        return Pair(level, minOf(95, maxOf(30, baseConfidence)))
    }

    /**
     * Calculate VDOT score (VO2max equivalent)
     * Based on Jack Daniels' running formula
     */
    private fun calculateVDOT(distanceMeters: Double, timeMs: Long): Double {
        val timeMinutes = timeMs / 60000.0
        val distanceKm = distanceMeters / 1000.0

        // Simplified VDOT calculation
        val velocity = distanceKm / timeMinutes * 60 // km/hour

        // Approximate VO2max based on velocity
        // This is a simplified formula; actual VDOT uses more complex calculations
        return when {
            distanceKm <= 1.5 -> velocity * 0.9
            distanceKm <= 5.0 -> velocity * 0.85
            distanceKm <= 10.0 -> velocity * 0.82
            distanceKm <= 21.1 -> velocity * 0.78
            else -> velocity * 0.75
        }
    }

    /**
     * Determine runner level based on VDOT
     */
    private fun getRunnerLevel(vdot: Double): String {
        return when {
            vdot >= 70 -> "Elite Runner"
            vdot >= 60 -> "Advanced Runner"
            vdot >= 50 -> "Intermediate Runner"
            vdot >= 40 -> "Recreational Runner"
            vdot >= 30 -> "Beginner Runner"
            else -> "New Runner"
        }
    }

    /**
     * Generate improvement suggestions
     */
    private fun generateImprovementSuggestions(vdot: Double, trainingData: TrainingData?): String {
        val suggestions = mutableListOf<String>()

        if (trainingData == null) {
            return "Track more workouts to get personalized improvement suggestions."
        }

        // Suggest based on weekly mileage
        if (trainingData.averageWeeklyDistance < 30000) { // Less than 30km/week
            suggestions.add("Increase weekly mileage gradually to build aerobic base")
        }

        // Suggest based on long runs
        if (trainingData.longestRunDistance < 15000) { // Less than 15km long run
            suggestions.add("Extend your long run to improve endurance")
        }

        // Suggest based on training consistency
        if (trainingData.trainingWeeks < 8) {
            suggestions.add("Maintain consistent training for at least 8-12 weeks")
        }

        // Suggest based on current level
        when {
            vdot < 35 -> suggestions.add("Focus on building consistent running habit")
            vdot < 45 -> suggestions.add("Add one tempo run per week to improve threshold")
            vdot < 55 -> suggestions.add("Include interval training for speed development")
            else -> suggestions.add("Consider working with a coach for advanced training")
        }

        return suggestions.joinToString(". ")
    }

    /**
     * Get readable distance name
     */
    private fun getDistanceName(distanceMeters: Double): String {
        return when {
            distanceMeters <= 1001 -> "1K"
            distanceMeters <= 1610 -> "1 Mile"
            distanceMeters <= 5001 -> "5K"
            distanceMeters <= 10001 -> "10K"
            distanceMeters <= 21098 -> "Half Marathon"
            distanceMeters <= 42196 -> "Marathon"
            else -> "${(distanceMeters / 1000).toInt()}K"
        }
    }

    /**
     * Format time in milliseconds to readable string
     */
    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * Get pace zones based on race prediction
     * Useful for training
     */
    fun getPaceZones(fiveKmPrediction: RacePrediction): Map<String, Pair<Float, Float>> {
        val racePace = fiveKmPrediction.predictedPace

        return mapOf(
            "Easy" to Pair(racePace * 1.25f, racePace * 1.40f),
            "Aerobic" to Pair(racePace * 1.15f, racePace * 1.25f),
            "Tempo" to Pair(racePace * 1.05f, racePace * 1.15f),
            "Threshold" to Pair(racePace * 0.97f, racePace * 1.05f),
            "Interval" to Pair(racePace * 0.90f, racePace * 0.97f),
            "Repetition" to Pair(racePace * 0.85f, racePace * 0.90f)
        )
    }
}
