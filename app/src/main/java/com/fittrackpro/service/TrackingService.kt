package com.fittrackpro.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.fittrackpro.FitTrackApplication
import com.fittrackpro.R
import com.fittrackpro.ui.tracking.LiveTrackingActivity
import com.fittrackpro.util.Constants
import com.fittrackpro.util.formatDuration
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : Service() {

    private val binder = TrackingBinder()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var isTracking = false
    private var isPaused = false

    // Live data for UI updates
    val currentLocation = MutableLiveData<Location?>()
    val pathPoints = MutableLiveData<MutableList<Location>>(mutableListOf())
    val totalDistance = MutableLiveData(0.0)
    val elapsedTime = MutableLiveData(0L)

    private var startTime = 0L
    private var pausedDuration = 0L
    private var lastPauseTime = 0L

    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    inner class TrackingBinder : Binder() {
        fun getService(): TrackingService = this@TrackingService
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                Constants.TRACKING_SERVICE_ACTION_START -> startTracking()
                Constants.TRACKING_SERVICE_ACTION_PAUSE -> pauseTracking()
                Constants.TRACKING_SERVICE_ACTION_RESUME -> resumeTracking()
                Constants.TRACKING_SERVICE_ACTION_STOP -> stopTracking()
            }
        }
        return START_STICKY
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                if (isTracking && !isPaused) {
                    result.lastLocation?.let { location ->
                        addLocationPoint(location)
                    }
                }
            }
        }
    }

    private fun startTracking() {
        isTracking = true
        isPaused = false
        startTime = System.currentTimeMillis()

        startForeground(Constants.TRACKING_NOTIFICATION_ID, createNotification())
        requestLocationUpdates()
        startTimer()
    }

    private fun pauseTracking() {
        isPaused = true
        lastPauseTime = System.currentTimeMillis()
        timerJob?.cancel()
        updateNotification("Tracking Paused")
    }

    private fun resumeTracking() {
        isPaused = false
        pausedDuration += System.currentTimeMillis() - lastPauseTime
        startTimer()
        updateNotification("Tracking Active")
    }

    private fun stopTracking() {
        isTracking = false
        isPaused = false
        timerJob?.cancel()
        removeLocationUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(Constants.LOCATION_FASTEST_INTERVAL)
            setMinUpdateDistanceMeters(Constants.LOCATION_MIN_DISTANCE)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission not granted
        }
    }

    private fun removeLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun addLocationPoint(location: Location) {
        currentLocation.postValue(location)

        val points = pathPoints.value ?: mutableListOf()

        // Calculate distance from last point
        if (points.isNotEmpty()) {
            val lastPoint = points.last()
            val distance = lastPoint.distanceTo(location)
            val currentTotal = totalDistance.value ?: 0.0
            totalDistance.postValue(currentTotal + distance)
        }

        points.add(location)
        pathPoints.postValue(points)

        // Update notification with current stats
        updateNotificationWithStats()
    }

    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (isTracking && !isPaused) {
                val elapsed = System.currentTimeMillis() - startTime - pausedDuration
                elapsedTime.postValue(elapsed)
                delay(1000)
            }
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, LiveTrackingActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FitTrackApplication.CHANNEL_TRACKING)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(R.drawable.ic_activities)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(status: String) {
        val notification = NotificationCompat.Builder(this, FitTrackApplication.CHANNEL_TRACKING)
            .setContentTitle(status)
            .setContentText(getString(R.string.tracking_notification_text))
            .setSmallIcon(R.drawable.ic_activities)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(Constants.TRACKING_NOTIFICATION_ID, notification)
    }

    private fun updateNotificationWithStats() {
        val distance = String.format("%.2f km", (totalDistance.value ?: 0.0) / 1000)
        val duration = (elapsedTime.value ?: 0L).formatDuration()

        val notification = NotificationCompat.Builder(this, FitTrackApplication.CHANNEL_TRACKING)
            .setContentTitle("$distance • $duration")
            .setContentText("Tap to return to tracking")
            .setSmallIcon(R.drawable.ic_activities)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(Constants.TRACKING_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        removeLocationUpdates()
    }
}
