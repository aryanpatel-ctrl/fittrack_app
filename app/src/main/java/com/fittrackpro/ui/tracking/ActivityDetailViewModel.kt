package com.fittrackpro.ui.tracking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.dao.TrackPointDao
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.data.local.database.entity.TrackPoint
import com.fittrackpro.data.local.database.entity.TrackStatistics
import com.fittrackpro.service.ExportService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    private val trackDao: TrackDao,
    private val trackPointDao: TrackPointDao,
    private val exportService: ExportService
) : ViewModel() {

    private val _track = MutableLiveData<Track?>()
    val track: LiveData<Track?> = _track

    private val _statistics = MutableLiveData<TrackStatistics?>()
    val statistics: LiveData<TrackStatistics?> = _statistics

    private val _trackPoints = MutableLiveData<List<TrackPoint>>()
    val trackPoints: LiveData<List<TrackPoint>> = _trackPoints

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    private val _exportResult = MutableLiveData<ExportService.ExportResult?>()
    val exportResult: LiveData<ExportService.ExportResult?> = _exportResult

    private var currentTrackId: String? = null
    private var currentTrack: Track? = null
    private var currentExportFormat: ExportService.ExportFormat? = null

    fun loadActivity(trackId: String) {
        currentTrackId = trackId
        viewModelScope.launch {
            val track = trackDao.getTrackById(trackId)
            currentTrack = track
            _track.value = track
            _statistics.value = trackDao.getStatisticsByTrackId(trackId)
            _trackPoints.value = trackPointDao.getTrackPointsByTrackId(trackId)
        }
    }

    fun deleteActivity() {
        currentTrack?.let { track ->
            viewModelScope.launch {
                trackDao.deleteTrack(track)
                _deleteSuccess.value = true
            }
        }
    }

    fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formatPace(speedMs: Float): String {
        if (speedMs <= 0) return "--:--"
        val paceMinPerKm = 1000 / (speedMs * 60)
        val minutes = paceMinPerKm.toInt()
        val seconds = ((paceMinPerKm - minutes) * 60).toInt()
        return String.format("%d:%02d /km", minutes, seconds)
    }

    fun exportActivity(format: ExportService.ExportFormat) {
        currentTrackId?.let { trackId ->
            currentExportFormat = format
            viewModelScope.launch {
                val result = exportService.exportTrack(trackId, format)
                _exportResult.value = result
            }
        }
    }

    fun createShareIntent(): android.content.Intent? {
        val result = _exportResult.value ?: return null
        val format = currentExportFormat ?: return null
        return exportService.createShareIntent(result, format)
    }

    fun clearExportResult() {
        _exportResult.value = null
    }
}
