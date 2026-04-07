package com.fittrackpro.ui.tracking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.data.local.database.entity.TrackStatistics
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompareActivitiesViewModel @Inject constructor(
    private val trackDao: TrackDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _activities = MutableLiveData<List<Track>>()
    val activities: LiveData<List<Track>> = _activities

    private val _activity1 = MutableLiveData<TrackWithStats?>()
    val activity1: LiveData<TrackWithStats?> = _activity1

    private val _activity2 = MutableLiveData<TrackWithStats?>()
    val activity2: LiveData<TrackWithStats?> = _activity2

    private val _comparison = MutableLiveData<ComparisonResult?>()
    val comparison: LiveData<ComparisonResult?> = _comparison

    init {
        loadActivities()
    }

    private fun loadActivities() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val tracks = trackDao.getCompletedTracksByUser(userId).first()
            _activities.value = tracks
        }
    }

    fun selectActivity1(trackId: String) {
        viewModelScope.launch {
            val track = trackDao.getTrackById(trackId)
            val stats = trackDao.getStatisticsByTrackId(trackId)
            if (track != null && stats != null) {
                _activity1.value = TrackWithStats(track, stats)
                calculateComparison()
            }
        }
    }

    fun selectActivity2(trackId: String) {
        viewModelScope.launch {
            val track = trackDao.getTrackById(trackId)
            val stats = trackDao.getStatisticsByTrackId(trackId)
            if (track != null && stats != null) {
                _activity2.value = TrackWithStats(track, stats)
                calculateComparison()
            }
        }
    }

    private fun calculateComparison() {
        val stats1 = _activity1.value?.stats ?: return
        val stats2 = _activity2.value?.stats ?: return

        val distanceDiff = stats2.distance - stats1.distance
        val durationDiff = stats2.duration - stats1.duration
        val paceDiff = stats2.avgPace - stats1.avgPace
        val speedDiff = stats2.avgSpeed - stats1.avgSpeed
        val caloriesDiff = stats2.calories - stats1.calories
        val elevationDiff = stats2.elevationGain - stats1.elevationGain

        _comparison.value = ComparisonResult(
            distanceDiff = distanceDiff,
            distancePercent = if (stats1.distance > 0) ((distanceDiff / stats1.distance) * 100).toFloat() else 0f,
            durationDiff = durationDiff,
            durationPercent = if (stats1.duration > 0) ((durationDiff.toDouble() / stats1.duration) * 100).toFloat() else 0f,
            paceDiff = paceDiff,
            pacePercent = if (stats1.avgPace > 0) ((paceDiff / stats1.avgPace) * 100) else 0f,
            speedDiff = speedDiff,
            speedPercent = if (stats1.avgSpeed > 0) ((speedDiff / stats1.avgSpeed) * 100) else 0f,
            caloriesDiff = caloriesDiff,
            caloriesPercent = if (stats1.calories > 0) ((caloriesDiff.toFloat() / stats1.calories) * 100) else 0f,
            elevationDiff = elevationDiff,
            elevationPercent = if (stats1.elevationGain > 0) ((elevationDiff / stats1.elevationGain) * 100).toFloat() else 0f
        )
    }
}

data class ComparisonResult(
    val distanceDiff: Double,
    val distancePercent: Float,
    val durationDiff: Long,
    val durationPercent: Float,
    val paceDiff: Float,
    val pacePercent: Float,
    val speedDiff: Float,
    val speedPercent: Float,
    val caloriesDiff: Int,
    val caloriesPercent: Float,
    val elevationDiff: Double,
    val elevationPercent: Float
)
