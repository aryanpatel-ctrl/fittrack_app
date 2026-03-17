package com.fittrackpro.ui.tracking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.TrackDao
import com.fittrackpro.data.local.database.entity.Track
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    private val trackDao: TrackDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _activities = MutableLiveData<List<Track>>()
    val activities: LiveData<List<Track>> = _activities

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadActivities()
    }

    private fun loadActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = userPreferences.userId
            if (userId != null) {
                trackDao.getTracksByUserIdFlow(userId).collect { tracks ->
                    _activities.value = tracks
                    _isLoading.value = false
                }
            } else {
                _activities.value = emptyList()
                _isLoading.value = false
            }
        }
    }

    fun deleteActivity(track: Track) {
        viewModelScope.launch {
            trackDao.deleteTrack(track)
        }
    }
}
