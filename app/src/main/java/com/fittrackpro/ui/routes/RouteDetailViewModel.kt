package com.fittrackpro.ui.routes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.RouteDao
import com.fittrackpro.data.local.database.entity.Route
import com.fittrackpro.data.local.database.entity.RouteRating
import com.fittrackpro.data.local.database.entity.RouteWaypoint
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    private val routeDao: RouteDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _route = MutableLiveData<Route?>()
    val route: LiveData<Route?> = _route

    private val _waypoints = MutableLiveData<List<RouteWaypoint>>()
    val waypoints: LiveData<List<RouteWaypoint>> = _waypoints

    private val _ratings = MutableLiveData<List<RouteRating>>()
    val ratings: LiveData<List<RouteRating>> = _ratings

    private val _userRating = MutableLiveData<RouteRating?>()
    val userRating: LiveData<RouteRating?> = _userRating

    private val _ratingSubmitted = MutableLiveData<Boolean>()
    val ratingSubmitted: LiveData<Boolean> = _ratingSubmitted

    private var currentRouteId: String? = null

    fun loadRoute(routeId: String) {
        currentRouteId = routeId
        viewModelScope.launch {
            _route.value = routeDao.getRouteById(routeId)
            _waypoints.value = routeDao.getWaypointsByRoute(routeId)

            val userId = userPreferences.userId
            if (userId != null) {
                _userRating.value = routeDao.getUserRating(routeId, userId)
            }

            routeDao.getRatingsByRoute(routeId).collect { ratingList ->
                _ratings.value = ratingList
            }
        }
    }

    fun submitRating(rating: Float, review: String?, safetyRating: Int?, sceneryRating: Int?, surfaceRating: Int?) {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            val routeId = currentRouteId ?: return@launch

            val existingRating = routeDao.getUserRating(routeId, userId)
            val routeRating = RouteRating(
                id = existingRating?.id ?: 0,
                routeId = routeId,
                userId = userId,
                rating = rating,
                review = review,
                safetyRating = safetyRating,
                sceneryRating = sceneryRating,
                surfaceRating = surfaceRating,
                updatedAt = System.currentTimeMillis()
            )

            if (existingRating != null) {
                routeDao.updateRating(routeRating)
            } else {
                routeDao.insertRating(routeRating)
            }

            // Update route average rating
            val avgRating = routeDao.getAverageRating(routeId) ?: rating
            val ratingCount = routeDao.getRatingCount(routeId)
            routeDao.updateRouteRating(routeId, avgRating, ratingCount, System.currentTimeMillis())

            _userRating.value = routeRating
            _ratingSubmitted.value = true
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val route = _route.value ?: return@launch
            routeDao.updateFavoriteStatus(
                routeId = route.id,
                isFavorite = !route.isFavorite,
                timestamp = System.currentTimeMillis()
            )
            _route.value = route.copy(isFavorite = !route.isFavorite)
        }
    }

    fun useRoute() {
        viewModelScope.launch {
            val routeId = currentRouteId ?: return@launch
            routeDao.incrementTimesUsed(routeId, System.currentTimeMillis())
        }
    }
}
