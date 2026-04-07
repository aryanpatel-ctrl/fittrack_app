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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Combined data class for Track with its statistics
 */
data class TrackWithStats(
    val track: Track,
    val stats: TrackStatistics?
)

/**
 * Monthly statistics summary
 */
data class MonthlyStats(
    val workoutsCount: Int = 0,
    val totalDistance: Double = 0.0, // meters
    val totalDuration: Long = 0L // milliseconds
)

@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    private val trackDao: TrackDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _activities = MutableLiveData<List<TrackWithStats>>()
    val activities: LiveData<List<TrackWithStats>> = _activities

    private val _monthlyStats = MutableLiveData<MonthlyStats>()
    val monthlyStats: LiveData<MonthlyStats> = _monthlyStats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Store all activities for filtering
    private var allActivities: List<TrackWithStats> = emptyList()
    private var currentFilter: String? = null // null means "all"

    init {
        loadActivities()
    }

    private fun loadActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = userPreferences.userId
            if (userId != null) {
                trackDao.getTracksByUserIdFlow(userId).collect { tracks ->
                    // Load statistics for each track
                    val tracksWithStats = tracks.map { track ->
                        val stats = trackDao.getStatisticsByTrackId(track.id)
                        TrackWithStats(track, stats)
                    }
                    allActivities = tracksWithStats
                    applyFilter()
                    _isLoading.value = false

                    // Calculate monthly stats (always from all activities)
                    calculateMonthlyStats(tracksWithStats)
                }
            } else {
                allActivities = emptyList()
                _activities.value = emptyList()
                _monthlyStats.value = MonthlyStats()
                _isLoading.value = false
            }
        }
    }

    /**
     * Filter activities by type
     * @param activityType The type to filter by (running, cycling, walking) or null for all
     */
    fun filterByType(activityType: String?) {
        currentFilter = activityType
        applyFilter()
    }

    private fun applyFilter() {
        _activities.value = if (currentFilter == null) {
            allActivities
        } else {
            allActivities.filter { it.track.activityType == currentFilter }
        }
    }

    /**
     * Refresh activities list
     */
    fun refresh() {
        loadActivities()
    }

    private fun calculateMonthlyStats(tracksWithStats: List<TrackWithStats>) {
        // Get start of current month
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        // Filter activities from this month
        val thisMonthActivities = tracksWithStats.filter { it.track.startTime >= startOfMonth }

        // Calculate totals
        var totalDistance = 0.0
        var totalDuration = 0L

        thisMonthActivities.forEach { trackWithStats ->
            trackWithStats.stats?.let { stats ->
                totalDistance += stats.distance
                totalDuration += stats.duration
            }
        }

        _monthlyStats.value = MonthlyStats(
            workoutsCount = thisMonthActivities.size,
            totalDistance = totalDistance,
            totalDuration = totalDuration
        )
    }

    fun deleteActivity(track: Track) {
        viewModelScope.launch {
            trackDao.deleteTrack(track)
        }
    }
}
