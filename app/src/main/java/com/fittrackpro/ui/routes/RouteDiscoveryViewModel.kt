package com.fittrackpro.ui.routes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrackpro.data.local.database.dao.RouteDao
import com.fittrackpro.data.local.database.entity.Route
import com.fittrackpro.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteDiscoveryViewModel @Inject constructor(
    private val routeDao: RouteDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _routes = MutableLiveData<List<Route>>()
    val routes: LiveData<List<Route>> = _routes

    private val _myRoutes = MutableLiveData<List<Route>>()
    val myRoutes: LiveData<List<Route>> = _myRoutes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentFilter = RouteFilter.ALL

    init {
        loadRoutes()
        loadMyRoutes()
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            _isLoading.value = true
            routeDao.getPopularPublicRoutes(50).collect { routeList ->
                _routes.value = routeList
                _isLoading.value = false
            }
        }
    }

    private fun loadMyRoutes() {
        viewModelScope.launch {
            val userId = userPreferences.userId ?: return@launch
            routeDao.getRoutesByUser(userId).collect { routeList ->
                _myRoutes.value = routeList
            }
        }
    }

    fun filterByActivity(activityType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            routeDao.getPublicRoutesByActivity(activityType).collect { routeList ->
                _routes.value = routeList
                _isLoading.value = false
            }
        }
    }

    fun filterByDifficulty(difficulty: String) {
        viewModelScope.launch {
            _isLoading.value = true
            routeDao.getPublicRoutesByDifficulty(difficulty).collect { routeList ->
                _routes.value = routeList
                _isLoading.value = false
            }
        }
    }

    fun searchNearby(latitude: Double, longitude: Double, radiusKm: Double = 10.0) {
        viewModelScope.launch {
            _isLoading.value = true
            val latDiff = radiusKm / 111.0 // Approximate km per degree latitude
            val lngDiff = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(latitude)))

            routeDao.getRoutesNearLocation(
                minLat = latitude - latDiff,
                maxLat = latitude + latDiff,
                minLng = longitude - lngDiff,
                maxLng = longitude + lngDiff
            ).collect { routeList ->
                _routes.value = routeList
                _isLoading.value = false
            }
        }
    }

    fun clearFilters() {
        currentFilter = RouteFilter.ALL
        loadRoutes()
    }

    fun toggleFavorite(route: Route) {
        viewModelScope.launch {
            routeDao.updateFavoriteStatus(
                routeId = route.id,
                isFavorite = !route.isFavorite,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

enum class RouteFilter {
    ALL, RUNNING, CYCLING, WALKING, HIKING, EASY, MODERATE, HARD, NEARBY
}
