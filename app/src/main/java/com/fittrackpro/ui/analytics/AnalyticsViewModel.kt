package com.fittrackpro.ui.analytics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.TrackPointDao
import com.fittrackpro.data.local.database.entity.PersonalRecord
import com.fittrackpro.data.local.database.entity.TrackStatistics
import com.fittrackpro.data.local.preferences.UserPreferences
import com.fittrackpro.util.FatigueAnalyzer
import com.fittrackpro.util.RaceTimePredictor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class SummaryStats(
    val totalDistance: Double, val totalDuration: Long,
    val totalActivities: Int, val totalCalories: Int,
    val avgPace: Float, val avgDistance: Double
)

enum class TimeRange { WEEK, MONTH, YEAR }

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _weeklyDistance = MutableLiveData<List<Pair<String, Float>>>()
    val weeklyDistance: LiveData<List<Pair<String, Float>>> = _weeklyDistance

    private val _paceHistory = MutableLiveData<List<Pair<String, Float>>>()
    val paceHistory: LiveData<List<Pair<String, Float>>> = _paceHistory

    private val _activityTypeBreakdown = MutableLiveData<Map<String, Int>>()
    val activityTypeBreakdown: LiveData<Map<String, Int>> = _activityTypeBreakdown

    private val _caloriesHistory = MutableLiveData<List<Pair<String, Float>>>()
    val caloriesHistory: LiveData<List<Pair<String, Float>>> = _caloriesHistory

    private val _summaryStats = MutableLiveData<SummaryStats?>()
    val summaryStats: LiveData<SummaryStats?> = _summaryStats

    private val _personalRecords = MutableLiveData<List<PersonalRecord>>()
    val personalRecords: LiveData<List<PersonalRecord>> = _personalRecords

    // Fatigue Analysis
    private val _fatigueAnalysis = MutableLiveData<FatigueUiState?>()
    val fatigueAnalysis: LiveData<FatigueUiState?> = _fatigueAnalysis

    // Race Predictions
    private val _racePredictions = MutableLiveData<RacePredictionUiState?>()
    val racePredictions: LiveData<RacePredictionUiState?> = _racePredictions

    private var currentTimeRange = TimeRange.WEEK

    init { loadData() }

    fun setTimeRange(range: TimeRange) { currentTimeRange = range; loadData() }

    private fun loadData() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            val startTime = when (currentTimeRange) {
                TimeRange.WEEK -> { calendar.add(Calendar.DAY_OF_YEAR, -7); calendar.timeInMillis }
                TimeRange.MONTH -> { calendar.add(Calendar.MONTH, -1); calendar.timeInMillis }
                TimeRange.YEAR -> { calendar.add(Calendar.YEAR, -1); calendar.timeInMillis }
            }

            val tracks = trackDao.getTracksByDateRange(userId, startTime, endTime).first()
            val stats = tracks.mapNotNull { trackDao.getStatisticsByTrackId(it.id) }

            val totalDistance = stats.sumOf { it.distance }
            val totalDuration = stats.sumOf { it.duration }
            val totalCalories = stats.sumOf { it.calories }
            val avgPace = if (stats.isNotEmpty()) stats.map { it.avgPace }.average().toFloat() else 0f
            val avgDistance = if (tracks.isNotEmpty()) totalDistance / tracks.size else 0.0

            _summaryStats.value = SummaryStats(totalDistance, totalDuration, tracks.size, totalCalories, avgPace, avgDistance)

            val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val distanceByDay = mutableListOf<Pair<String, Float>>()
            val cal = Calendar.getInstance().apply { timeInMillis = startTime }
            while (cal.timeInMillis <= endTime) {
                val dayStart = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val dayEnd = cal.timeInMillis
                val dayDist = tracks.filter { it.startTime in dayStart until dayEnd }
                    .sumOf { t -> stats.find { it.trackId == t.id }?.distance ?: 0.0 }
                distanceByDay.add(Pair(dateFormat.format(dayStart), (dayDist / 1000).toFloat()))
            }
            _weeklyDistance.value = distanceByDay.takeLast(7)

            _paceHistory.value = tracks.sortedBy { it.startTime }.mapNotNull { track ->
                stats.find { it.trackId == track.id }?.takeIf { it.avgPace > 0 }?.let {
                    Pair(dateFormat.format(track.startTime), it.avgPace)
                }
            }.takeLast(10)

            _activityTypeBreakdown.value = tracks.groupBy { it.activityType }.mapValues { it.value.size }

            _caloriesHistory.value = tracks.sortedBy { it.startTime }.mapNotNull { track ->
                stats.find { it.trackId == track.id }?.let { Pair(dateFormat.format(track.startTime), it.calories.toFloat()) }
            }.takeLast(7)

            trackDao.getPersonalRecords(userId).collect { records ->
                _personalRecords.value = records
            }

            // Load fatigue analysis and race predictions
            loadFatigueAnalysis(userId)
            loadRacePredictions(userId, stats)
        }
    }

    /**
     * Load fatigue analysis using Acute:Chronic Workload Ratio
     *
     * From project specification:
     * - Acute Load = Average of last 7 days
     * - Chronic Load = Average of last 42 days
     * - Ratio = Acute / Chronic
     * - Safe: 0.8 - 1.3
     * - Warning: 1.3 - 1.5
     * - Danger: > 1.5
     */
    private suspend fun loadFatigueAnalysis(userId: String) {
        try {
            // Get last 42 days of data for chronic load calculation
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -42)
            val startTime = calendar.timeInMillis

            val tracks = trackDao.getTracksByDateRange(userId, startTime, endTime).first()
            val stats = tracks.mapNotNull { trackDao.getStatisticsByTrackId(it.id) }

            if (stats.isEmpty()) {
                _fatigueAnalysis.value = FatigueUiState(
                    hasData = false,
                    message = "Need more workout data to analyze fatigue"
                )
                return
            }

            // Create track date map
            val trackDates = tracks.associate { it.id to it.startTime }

            // Analyze fatigue
            val analysis = FatigueAnalyzer.analyzeFatigue(stats, trackDates)

            _fatigueAnalysis.value = FatigueUiState(
                hasData = true,
                acuteLoad = analysis.acuteLoad,
                chronicLoad = analysis.chronicLoad,
                ratio = analysis.acuteChronicRatio,
                status = analysis.status,
                statusMessage = analysis.statusMessage,
                recommendation = analysis.recommendation,
                riskPercentage = analysis.riskPercentage,
                fitnessScore = analysis.fitnessScore,
                fatigueScore = analysis.fatigueScore,
                formScore = analysis.formScore,
                trend = analysis.weeklyLoadTrend,
                daysUntilRecovery = analysis.daysUntilRecovery,
                statusColor = FatigueAnalyzer.getStatusColor(analysis.status)
            )
        } catch (e: Exception) {
            _fatigueAnalysis.value = FatigueUiState(
                hasData = false,
                message = "Unable to analyze fatigue: ${e.message}"
            )
        }
    }

    /**
     * Load race time predictions using Riegel formula
     *
     * From project specification:
     * T2 = T1 x (D2/D1)^1.06
     */
    private suspend fun loadRacePredictions(userId: String, recentStats: List<TrackStatistics>) {
        try {
            // Find the user's best recent 5K time (or closest distance)
            val runningStats = recentStats.filter { stat ->
                stat.distance >= 3000 && stat.distance <= 15000 // Between 3K and 15K
            }

            if (runningStats.isEmpty()) {
                _racePredictions.value = RacePredictionUiState(
                    hasData = false,
                    message = "Complete a run of 3-15km to get race predictions"
                )
                return
            }

            // Find best performance (fastest pace for distance category)
            val bestPerformance = runningStats.minByOrNull { it.avgPace }
            if (bestPerformance == null || bestPerformance.avgPace <= 0) {
                _racePredictions.value = RacePredictionUiState(
                    hasData = false,
                    message = "Need valid pace data for predictions"
                )
                return
            }

            // Get training data for adjustments
            val trainingData = getTrainingDataForPrediction(userId, recentStats)

            // Generate predictions
            val predictions = RaceTimePredictor.getAllPredictions(
                knownDistance = bestPerformance.distance,
                knownTime = bestPerformance.duration,
                trainingData = trainingData
            )

            _racePredictions.value = RacePredictionUiState(
                hasData = true,
                baseDistance = bestPerformance.distance,
                baseTime = bestPerformance.duration,
                basePace = bestPerformance.avgPace,
                predictions = predictions.predictions.map { pred ->
                    RacePredictionItem(
                        distanceName = pred.distanceName,
                        predictedTime = pred.predictedTimeFormatted,
                        predictedPace = String.format("%.2f min/km", pred.predictedPace),
                        confidence = pred.confidencePercentage,
                        confidenceLevel = pred.confidenceLevel.name
                    )
                },
                vdotScore = predictions.vdotScore,
                runnerLevel = predictions.runnerLevel,
                improvementTips = predictions.improvementPotential
            )
        } catch (e: Exception) {
            _racePredictions.value = RacePredictionUiState(
                hasData = false,
                message = "Unable to predict race times: ${e.message}"
            )
        }
    }

    /**
     * Get training data for race prediction adjustments
     */
    private suspend fun getTrainingDataForPrediction(
        userId: String,
        recentStats: List<TrackStatistics>
    ): RaceTimePredictor.TrainingData {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val tracks = trackDao.getTracksByDateRange(userId, thirtyDaysAgo, System.currentTimeMillis()).first()

        val longestRun = recentStats.maxOfOrNull { it.distance } ?: 0.0
        val avgWeeklyDistance = if (tracks.isNotEmpty()) {
            recentStats.sumOf { it.distance } / 4.3 // Approximate weeks in 30 days
        } else 0.0

        // Calculate trend (comparing this week to last week)
        val oneWeekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val twoWeeksAgo = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)

        val thisWeekTracks = tracks.filter { it.startTime >= oneWeekAgo }
        val lastWeekTracks = tracks.filter { it.startTime in twoWeeksAgo until oneWeekAgo }

        val thisWeekPace = thisWeekTracks.mapNotNull { track ->
            recentStats.find { it.trackId == track.id }?.avgPace
        }.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f

        val lastWeekPace = lastWeekTracks.mapNotNull { track ->
            recentStats.find { it.trackId == track.id }?.avgPace
        }.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f

        val trend = if (lastWeekPace > 0 && thisWeekPace > 0) {
            // Lower pace = better, so invert the comparison
            ((lastWeekPace - thisWeekPace) / lastWeekPace).toDouble().coerceIn(-1.0, 1.0)
        } else 0.0

        // Count training weeks
        val uniqueWeeks = tracks.map { it.startTime / (7 * 24 * 60 * 60 * 1000) }.distinct().size

        val avgPace = if (recentStats.isNotEmpty()) {
            recentStats.filter { it.avgPace > 0 }.map { it.avgPace }.average().toFloat()
        } else 0f

        return RaceTimePredictor.TrainingData(
            longestRunDistance = longestRun,
            averageWeeklyDistance = avgWeeklyDistance,
            recentTrend = trend,
            trainingWeeks = uniqueWeeks,
            avgPaceLast30Days = avgPace
        )
    }

    /**
     * Refresh all analytics data
     */
    fun refresh() {
        loadData()
    }
}

/**
 * UI State for fatigue analysis display
 */
data class FatigueUiState(
    val hasData: Boolean = false,
    val acuteLoad: Double = 0.0,
    val chronicLoad: Double = 0.0,
    val ratio: Double = 0.0,
    val status: FatigueAnalyzer.FatigueStatus = FatigueAnalyzer.FatigueStatus.OPTIMAL,
    val statusMessage: String = "",
    val recommendation: String = "",
    val riskPercentage: Int = 0,
    val fitnessScore: Int = 0,
    val fatigueScore: Int = 0,
    val formScore: Int = 0,
    val trend: FatigueAnalyzer.LoadTrend = FatigueAnalyzer.LoadTrend.STABLE,
    val daysUntilRecovery: Int? = null,
    val statusColor: String = "#4CAF50",
    val message: String = ""
) {
    fun getRatioFormatted(): String = String.format("%.2f", ratio)

    fun getStatusEmoji(): String = when (status) {
        FatigueAnalyzer.FatigueStatus.UNDERTRAINED -> "📉"
        FatigueAnalyzer.FatigueStatus.OPTIMAL -> "✅"
        FatigueAnalyzer.FatigueStatus.BUILDING -> "📈"
        FatigueAnalyzer.FatigueStatus.WARNING -> "⚠️"
        FatigueAnalyzer.FatigueStatus.DANGER -> "🚨"
    }

    fun getTrendEmoji(): String = when (trend) {
        FatigueAnalyzer.LoadTrend.INCREASING -> "📈"
        FatigueAnalyzer.LoadTrend.STABLE -> "➡️"
        FatigueAnalyzer.LoadTrend.DECREASING -> "📉"
    }
}

/**
 * UI State for race predictions display
 */
data class RacePredictionUiState(
    val hasData: Boolean = false,
    val baseDistance: Double = 0.0,
    val baseTime: Long = 0L,
    val basePace: Float = 0f,
    val predictions: List<RacePredictionItem> = emptyList(),
    val vdotScore: Double = 0.0,
    val runnerLevel: String = "",
    val improvementTips: String = "",
    val message: String = ""
) {
    fun getBaseDistanceFormatted(): String = String.format("%.1f km", baseDistance / 1000)

    fun getBaseTimeFormatted(): String {
        val totalSeconds = baseTime / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    fun getVdotFormatted(): String = String.format("%.1f", vdotScore)
}

/**
 * Individual race prediction item
 */
data class RacePredictionItem(
    val distanceName: String,
    val predictedTime: String,
    val predictedPace: String,
    val confidence: Int,
    val confidenceLevel: String
) {
    fun getConfidenceColor(): String = when {
        confidence >= 75 -> "#4CAF50"  // Green
        confidence >= 55 -> "#FF9800"  // Orange
        else -> "#F44336"              // Red
    }
}
